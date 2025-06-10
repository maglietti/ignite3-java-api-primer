# Apache Ignite 3 Code Examples and Patterns

This document provides practical code examples and patterns for working with Apache Ignite 3, using the Chinook database model.

## Connecting to an Ignite Cluster

```java
public static IgniteClient connectToCluster() {
    try {
        // Define node addresses
        String[] nodeAddresses = {
                "localhost:10800", "localhost:10801", "localhost:10802"
        };

        // Build the client and connect
        IgniteClient client = IgniteClient.builder()
                .addresses(nodeAddresses)
                .build();

        System.out.println(">>> Connected to the cluster: " + client.connections());
        return client;
    } catch (Exception e) {
        System.err.println("An error occurred: " + e.getMessage());
        return null;
    }
}
```

Usage:

```java
try (IgniteClient client = ChinookUtils.connectToCluster()) {
    if (client == null) {
        System.err.println("Failed to connect to the cluster. Exiting.");
        return;
    }

    // Use the client...
}
```

## Creating Tables: POJO-based vs. SQL-based Approaches

### POJO-based Table Creation

```java
public static boolean createTables(IgniteClient client) {
    try {
        System.out.println("\n=== Creating tables");

        // Use IgniteCatalog.createTable to create tables from annotated classes
        System.out.println("--- Creating Artist table");
        client.catalog().createTable(Artist.class);

        System.out.println("--- Creating Genre table");
        client.catalog().createTable(Genre.class);

        System.out.println("--- Creating MediaType table");
        client.catalog().createTable(MediaType.class);

        System.out.println("--- Creating Album table");
        client.catalog().createTable(Album.class);

        System.out.println("--- Creating Track table");
        client.catalog().createTable(Track.class);

        System.out.println("--- Creating Employee table");
        client.catalog().createTable(Employee.class);

        System.out.println("--- Creating Customer table");
        client.catalog().createTable(Customer.class);

        System.out.println("--- Creating Invoice table");
        client.catalog().createTable(Invoice.class);

        System.out.println("--- Creating InvoiceLine table");
        client.catalog().createTable(InvoiceLine.class);

        System.out.println("--- Creating Playlist table");
        client.catalog().createTable(Playlist.class);

        System.out.println("--- Creating PlaylistTrack table");
        client.catalog().createTable(PlaylistTrack.class);

        System.out.println("=== All tables created successfully!");
        return true;
    } catch (Exception e) {
        System.err.println("Error creating tables: " + e.getMessage());
        e.printStackTrace();
        return false;
    }
}
```

### SQL-based Table Creation

```java
public static void createTablesWithSql(IgniteClient client) {
    try {
        // Create a distribution zone
        client.sql().execute(null,
                "CREATE ZONE IF NOT EXISTS Chinook " +
                        "WITH STORAGE_PROFILES='default', REPLICAS=2");

        // Create Artist table
        client.sql().execute(null,
                "CREATE TABLE IF NOT EXISTS Artist (" +
                        "  ArtistId INT PRIMARY KEY, " +
                        "  Name VARCHAR" +
                        ") ZONE Chinook");

        // Create Album table with co-location
        client.sql().execute(null,
                "CREATE TABLE IF NOT EXISTS Album (" +
                        "  AlbumId INT, " +
                        "  Title VARCHAR NOT NULL, " +
                        "  ArtistId INT, " +
                        "  PRIMARY KEY (AlbumId, ArtistId)" +
                        ") ZONE Chinook COLOCATE BY (ArtistId)");

        System.out.println("Tables created successfully.");
    } catch (Exception e) {
        System.err.println("Error creating tables with SQL: " + e.getMessage());
    }
}
```

This method is used by the `BulkLoadApp` to create tables from SQL statements in a file.

## Basic CRUD Operations

### SQL-to-POJO Pattern

From `ReportingUtils.java`:

```java
// Retrieve artists using SQL and convert to POJOs
public static void listArtists(IgniteClient client) {
    try {
        List<Artist> artists = new ArrayList<>();

        // Use SQL to get all artists
        client.sql().execute(null, "SELECT ArtistId, Name FROM Artist")
                .forEachRemaining(row -> {
                    Artist artist = new Artist();
                    artist.setArtistId(row.intValue("ArtistId"));
                    artist.setName(row.stringValue("Name"));
                    artists.add(artist);
                });

        System.out.println("\nRetrieved " + artists.size() + " artists using SQL");
        System.out.println("First 5 artists:");
        artists.stream().limit(5)
                .forEach(artist -> System.out.println("  - " + artist.getArtistId() + ": " + artist.getName()));
    } catch (Exception e) {
        System.err.println("Error listing artists: " + e.getMessage());
    }
}

// Get a specific artist by ID
public static void findArtistAndAlbums(IgniteClient client, int targetArtistId) {
    try {
        // Get the artist using SQL
        Artist targetArtist = null;
        var artistResult = client.sql().execute(null,
                "SELECT ArtistId, Name FROM Artist WHERE ArtistId = ?",
                targetArtistId);

        if (artistResult.hasNext()) {
            var row = artistResult.next();
            targetArtist = new Artist();
            targetArtist.setArtistId(row.intValue("ArtistId"));
            targetArtist.setName(row.stringValue("Name"));
        }

        if (targetArtist != null) {
            System.out.println("\nFound artist by ID: " + targetArtist.getName());

            // Get their albums using SQL
            List<Album> artistAlbums = new ArrayList<>();
            client.sql().execute(null,
                            "SELECT AlbumId, Title, ArtistId FROM Album WHERE ArtistId = ?",
                            targetArtistId)
                    .forEachRemaining(row -> {
                        Album album = new Album();
                        album.setAlbumId(row.intValue("AlbumId"));
                        album.setTitle(row.stringValue("Title"));
                        album.setArtistId(row.intValue("ArtistId"));
                        artistAlbums.add(album);
                    });

            System.out.println("Albums by " + targetArtist.getName() + ":");
            artistAlbums
                    .forEach(album -> System.out.println("  - " + album.getAlbumId() + ": " + album.getTitle()));

            // Get tracks for the first album
            if (!artistAlbums.isEmpty()) {
                Album firstAlbum = artistAlbums.get(0);
                List<Track> albumTracks = new ArrayList<>();
                // Additional code for getting tracks...
            }
        }
    } catch (Exception e) {
        System.err.println("Error finding artist and albums: " + e.getMessage());
    }
}
```

### Direct POJO-to-Table Operations

```java
public static boolean addArtist(IgniteClient client, Artist artist) {
    try {
        // Get the Artist table
        Table artistTable = client.tables().table("Artist");
        // Create a record view for Artist class
        RecordView<Artist> artistView = artistTable.recordView(Artist.class);
        // Insert the artist
        artistView.upsert(null, artist);
        System.out.println("Added artist: " + artist.getName());
        return true;
    } catch (Exception e) {
        System.err.println("Error adding artist: " + e.getMessage());
        return false;
    }
}
```

Usage:

```java
Artist queen = new Artist(6, "Queen");
ChinookUtils.addArtist(client, queen);
```

### Retrieving a Record by Key

```java
public static Artist getArtistById(IgniteClient client, int artistId) {
    try {
        Table artistTable = client.tables().table("Artist");
        KeyValueView<Integer, Artist> keyValueView = artistTable.keyValueView(Integer.class, Artist.class);
        
        return keyValueView.get(null, artistId);
    } catch (Exception e) {
        System.err.println("Error getting artist: " + e.getMessage());
        return null;
    }
}
```

Usage:

```java
Artist artist = ChinookUtils.getArtistById(client, 6);
if (artist != null) {
    System.out.println("Found artist: " + artist.getName());
}
```

### Updating a Record

```java
public static boolean updateArtist(IgniteClient client, Artist artist) {
    try {
        Table artistTable = client.tables().table("Artist");
        RecordView<Artist> artistView = artistTable.recordView(Artist.class);
        // Upsert can be used for both insert and update
        artistView.upsert(null, artist);
        System.out.println("Updated artist: " + artist.getName());
        return true;
    } catch (Exception e) {
        System.err.println("Error updating artist: " + e.getMessage());
        return false;
    }
}
```

From `testPojo.java`:

```java
// Fetch current version from database
personPojo originalPerson = personView.get(null, keyPerson);
System.out.println("Before: " + originalPerson);

// Update the person
originalPerson.setFirstName("Daniel");
personView.upsert(null, originalPerson);

// Fetch again to verify changes
personPojo updatedPerson = personView.get(null, keyPerson);
System.out.println("After : " + updatedPerson);
```

### Deleting a Record

```java
public static boolean deleteArtist(IgniteClient client, int artistId) {
    try {
        Table artistTable = client.tables().table("Artist");
        KeyValueView<Integer, Artist> keyValueView = artistTable.keyValueView(Integer.class, Artist.class);
        
        keyValueView.delete(null, artistId);
        System.out.println("Deleted artist with ID: " + artistId);
        return true;
    } catch (Exception e) {
        System.err.println("Error deleting artist: " + e.getMessage());
        return false;
    }
}
```

For composite primary keys:

```java
// Create a person with just the ID fields (composite primary key)
personPojo personToDelete = new personPojo();
personToDelete.setId(10);
personToDelete.setIdStr("P010");

// Delete the person using the POJO with primary key fields
personView.delete(null, personToDelete);
```

### Batch Insert Operations

```java
public static boolean addTracksInBatch(IgniteClient client, List<Track> tracks) {
    try {
        // Get the Track table
        Table trackTable = client.tables().table("Track");
        // Create a record view for Track class
        RecordView<Track> trackView = trackTable.recordView(Track.class);
        // Insert all tracks in batch
        trackView.upsertAll(null, tracks);
        System.out.println("Added " + tracks.size() + " tracks in batch");
        return true;
    } catch (Exception e) {
        System.err.println("Error adding tracks in batch: " + e.getMessage());
        return false;
    }
}
```

From `DataUtils.java`:

```java
// Create a list of artists
List<Artist> artists = new ArrayList<>();
artists.add(new Artist(1, "AC/DC"));
artists.add(new Artist(2, "Accept"));
artists.add(new Artist(3, "Aerosmith"));
artists.add(new Artist(4, "Alanis Morissette"));
artists.add(new Artist(5, "Alice In Chains"));

// Insert all artists in batch
artistView.upsertAll(tx, artists);
System.out.println("Added " + artists.size() + " artists");
```

## Executing SQL Queries

### Basic SQL Query

```java
public static void findAlbumsByArtist(IgniteClient client, String artistName) {
    try {
        System.out.println("\n--- Finding albums by artist: " + artistName + " ---");
        client.sql().execute(null,
                        "SELECT a.Title, ar.Name as ArtistName " +
                                "FROM Album a JOIN Artist ar ON a.ArtistId = ar.ArtistId " +
                                "WHERE ar.Name = ?", artistName)
                .forEachRemaining(row ->
                        System.out.println("Album: " + row.stringValue("Title") +
                                " by " + row.stringValue("ArtistName")));
    } catch (Exception e) {
        System.err.println("Error finding albums by artist: " + e.getMessage());
    }
}
```

### Query with Multiple Joins

```java
public static void findTracksByArtist(IgniteClient client, String artistName) {
    try {
        System.out.println("\n--- Finding tracks by artist: " + artistName + " ---");
        client.sql().execute(null,
                        "SELECT t.Name as Track, t.Composer, a.Title as Album, ar.Name as Artist " +
                                "FROM Track t " +
                                "JOIN Album a ON t.AlbumId = a.AlbumId " +
                                "JOIN Artist ar ON a.ArtistId = ar.ArtistId " +
                                "WHERE ar.Name = ?", artistName)
                .forEachRemaining(row ->
                        System.out.println("Track: " + row.stringValue("Track") +
                                ", Composer: " + row.stringValue("Composer") +
                                ", Album: " + row.stringValue("Album") +
                                ", Artist: " + row.stringValue("Artist")));
    } catch (Exception e) {
        System.err.println("Error finding tracks by artist: " + e.getMessage());
    }
}
```

### Using SQL with Parameters

```java
// From ReportingUtils.java - find an artist by ID
var artistResult = client.sql().execute(null,
        "SELECT ArtistId, Name FROM Artist WHERE ArtistId = ?",
        targetArtistId);
```

### Query with Aggregation

From `ReportingUtils.java`:

```java
public static void analyzeTrackLengths(IgniteClient client) {
    System.out.println("===== TRACK LENGTH ANALYSIS =====");
    try {
        System.out.println("Track Length Distribution:");

        // Define length categories in milliseconds
        int shortLength = 180000; // 3 minutes
        int mediumLength = 300000; // 5 minutes
        int longLength = 480000; // 8 minutes

        var result = client.sql().execute(null,
                "SELECT " +
                        "SUM(CASE WHEN Milliseconds < ? THEN 1 ELSE 0 END) as ShortTracks, " +
                        "SUM(CASE WHEN Milliseconds >= ? AND Milliseconds < ? THEN 1 ELSE 0 END) as MediumTracks, " +
                        "SUM(CASE WHEN Milliseconds >= ? AND Milliseconds < ? THEN 1 ELSE 0 END) as LongTracks, " +
                        "SUM(CASE WHEN Milliseconds >= ? THEN 1 ELSE 0 END) as VeryLongTracks, " +
                        "CAST(AVG(Milliseconds/60000.0) AS DECIMAL(10,2)) as AvgMinutes, " +
                        "CAST(MIN(Milliseconds/60000.0) AS DECIMAL(10,2)) as MinMinutes, " +
                        "CAST(MAX(Milliseconds/60000.0) AS DECIMAL(10,2)) as MaxMinutes " +
                        "FROM Track",
                shortLength, shortLength, mediumLength, mediumLength, longLength, longLength);

        if (result.hasNext()) {
            var row = result.next();

            System.out.println("Short Tracks (< 3 min): " + row.longValue("ShortTracks"));
            System.out.println("Medium Tracks (3-5 min): " + row.longValue("MediumTracks"));
            System.out.println("Long Tracks (5-8 min): " + row.longValue("LongTracks"));
            System.out.println("Very Long Tracks (> 8 min): " + row.longValue("VeryLongTracks"));
            System.out.println("Average Length: " + row.decimalValue("AvgMinutes") + " minutes");
            System.out.println("Shortest Track: " + row.decimalValue("MinMinutes") + " minutes");
            System.out.println("Longest Track: " + row.decimalValue("MaxMinutes") + " minutes");
        }
        
        // Additional code for longest tracks...
    } catch (Exception e) {
        System.err.println("Error analyzing track lengths: " + e.getMessage());
    }
}
```

## Working with Transactions

From `DataUtils.java`:

```java
public static boolean createQueenExample(IgniteClient client) {
    System.out.println("\n--- Creating Queen example ---");

    try {
        return client.transactions().runInTransaction(tx -> {
            // Add Queen artist
            Artist queen = new Artist(6, "Queen");
            Table artistTable = client.tables().table("Artist");
            RecordView<Artist> artistView = artistTable.recordView(Artist.class);
            artistView.upsert(tx, queen);

            // Add two Queen albums
            List<Album> queenAlbums = new ArrayList<>();
            queenAlbums.add(new Album(6, "A Night at the Opera", 6));
            queenAlbums.add(new Album(7, "News of the World", 6));

            Table albumTable = client.tables().table("Album");
            RecordView<Album> albumView = albumTable.recordView(Album.class);
            albumView.upsertAll(tx, queenAlbums);

            // Add tracks for the first album
            List<Track> operaTracks = new ArrayList<>();
            operaTracks.add(new Track(
                    6,
                    "Bohemian Rhapsody",
                    6,
                    1,
                    1,
                    "Freddie Mercury",
                    354947,
                    5733664,
                    new BigDecimal("0.99")
            ));

            operaTracks.add(new Track(
                    7,
                    "You're My Best Friend",
                    6,
                    1,
                    1,
                    "John Deacon",
                    175733,
                    2875239,
                    new BigDecimal("0.99")
            ));

            // Add tracks for the second album
            List<Track> newsTracks = new ArrayList<>();
            newsTracks.add(new Track(
                    8,
                    "We Will Rock You",
                    7,
                    1,
                    1,
                    "Brian May",
                    120000,
                    1947610,
                    new BigDecimal("0.99")
            ));

            newsTracks.add(new Track(
                    9,
                    "We Are The Champions",
                    7,
                    1,
                    1,
                    "Freddie Mercury",
                    180000,
                    2871563,
                    new BigDecimal("0.99")
            ));

            // Combine all tracks
            List<Track> allTracks = new ArrayList<>();
            allTracks.addAll(operaTracks);
            allTracks.addAll(newsTracks);

            Table trackTable = client.tables().table("Track");
            RecordView<Track> trackView = trackTable.recordView(Track.class);
            trackView.upsertAll(tx, allTracks);

            System.out.println("Created artist: " + queen.getName());
            System.out.println("Created " + queenAlbums.size() + " albums");
            System.out.println("Created " + allTracks.size() + " tracks");

            return true;
        });
    } catch (Exception e) {
        System.err.println("Transaction failed: " + e.getMessage());
        return false;
    }
}
```

## Checking If a Table Exists

```java
public static boolean tableExists(IgniteClient client, String tableName) {
    try {
        return client.tables().table(tableName) != null;
    } catch (Exception e) {
        return false;
    }
}
```

Usage:

```java
if (TableUtils.tableExists(client, "Artist")) {
    System.out.println("The Artist table exists!");
} else {
    System.out.println("The Artist table does not exist!");
}
```

## Error Handling Patterns

### Try-with-resources for Client Connection

```java
try (IgniteClient client = ChinookUtils.connectToCluster()) {
    if (client == null) {
        System.err.println("Failed to connect to the cluster. Exiting.");
        return;
    }
    
    // Use the client...
    
} catch (IgniteClientConnectionException e) {
    System.err.println("Connection error: " + e.getMessage());
    System.err.println("Affected endpoint: " + e.endpoint());
} catch (IgniteClientFeatureNotSupportedByServerException e) {
    System.err.println("Feature not supported: " + e.getMessage());
} catch (Exception e) {
    System.err.println("Error: " + e.getMessage());
    e.printStackTrace();
}
```

### Checking Preconditions

```java
// First, check if tables exist
if (!TableUtils.tableExists(client, "Artist")) {
    System.err.println("Tables do not exist. Please run CreateTablesApp first. Exiting.");
    return;
}
```

### Prompting for User Confirmation

```java
// Check if data already exists
long artistCount = 0;
var result = client.sql().execute(null, "SELECT COUNT(*) as cnt FROM Artist");
if (result.hasNext()) {
    artistCount = result.next().longValue("cnt");
}

if (artistCount > 0) {
    System.out.println("Database already contains data (" + artistCount + " artists found).");
    System.out.println("Do you want to load additional sample data? (Y/N)");

    // Simple way to get user input
    java.util.Scanner scanner = new java.util.Scanner(System.in);
    String input = scanner.nextLine().trim().toUpperCase();

    if (!input.equals("Y")) {
        System.out.println("Exiting without loading additional data.");
        return;
    }
}
```

## Working with Custom POJOs

From `testPojo.java`:

```java
// Define a custom POJO class
@Table(
    zone = @Zone(value = "test_zone", storageProfiles = "default")
)
public static class personPojo {
    @Id
    Integer id;

    @Id
    @Column(value = "id_str", length = 20)
    String idStr;

    @Column("f_name")
    String firstName;

    @Column("l_name")
    String lastName;

    String str;

    // Getters and setters...
}

// Create a custom test zone
ZoneDefinition zoneTestZone = ZoneDefinition.builder("test_zone")
        .ifNotExists()
        .partitions(2)
        .storageProfiles("default")
        .build();
client.catalog().createZone(zoneTestZone);

// Create table from personPojo
client.catalog().createTable(personPojo.class);

// Create a list of people
List<personPojo> people = new ArrayList<>();
people.add(createPerson(1, "P001", "John", "Doe", "Software Engineer"));
people.add(createPerson(2, "P002", "Jane", "Smith", "Data Scientist"));
// More people...

// Insert one person
System.out.println("\n--- Inserting: " + people.get(0));
personView.upsert(null, people.get(0));

// Retrieve using composite primary key
personPojo keyPerson = new personPojo();
keyPerson.setId(5);
keyPerson.setIdStr("P005");
personPojo originalPerson = personView.get(null, keyPerson);
```

## Handling NULL Values

Both patterns (POJO-to-Table and SQL-to-POJO) need to handle NULL values carefully:

```java
// From MainSQL.java - defensive programming for NULL values
try { 
    track.setGenreId(row.intValue("GenreId")); 
} catch (Exception e) { 
    /* Value is null */ 
}

try { 
    track.setComposer(row.stringValue("Composer")); 
} catch (Exception e) { 
    /* Value is null */ 
}

try { 
    track.setBytes(row.intValue("Bytes")); 
} catch (Exception e) { 
    /* Value is null */ 
}

try { 
    track.setUnitPrice(row.decimalValue("UnitPrice")); 
} catch (Exception e) { 
    /* Value is null */ 
}
```

## Performance Best Practices

1. **Use Batch Operations**: When inserting or updating multiple records, use batch operations like `upsertAll()` instead of individual operations.

```java
// From DataUtils.java - batch insert example
List<Artist> artists = new ArrayList<>();
artists.add(new Artist(1, "AC/DC"));
artists.add(new Artist(2, "Accept"));
artists.add(new Artist(3, "Aerosmith"));
// More artists...

// Insert all artists in batch
artistView.upsertAll(tx, artists);
```

2. **Leverage Co-location**: Design your schema to co-locate related data to minimize network transfers during joins.

```java
// From Album.java - co-location with Artist table
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default"),
    colocateBy = @ColumnRef("ArtistId"),
    indexes = {
        @Index(value = "IFK_AlbumArtistId", columns = { @ColumnRef("ArtistId") })
    }
)
public class Album { ... }
```

3. **Choose Appropriate Storage Profiles**:
   - Write-heavy workloads: Consider RocksDB storage engine
   - Read-heavy workloads: Use Apache Ignite Page Memory engine

4. **Use SQL for Complex Operations**: For complex aggregations and joins, SQL often provides better performance than client-side operations.

```java
// From ReportingUtils.java - complex aggregation
var result = client.sql().execute(null,
    "SELECT g.Name as Genre, COUNT(t.TrackId) as TrackCount " +
    "FROM Genre g " +
    "JOIN Track t ON g.GenreId = t.GenreId " +
    "GROUP BY g.Name " +
    "ORDER BY TrackCount DESC");
```

5. **Keep Transactions Short**: Long-running transactions can cause contention; keep them as short as possible.

6. **Distribution Zones**:
   - Use higher replica counts for critical data (3+)
   - Use lower replica counts for less critical data to save storage space
   - Choose partition counts based on your data size and cluster size

```java
// From TableUtils.java - different zones for different data
ZoneDefinition zoneChinook = ZoneDefinition.builder("Chinook")
        .ifNotExists()
        .replicas(2)
        .storageProfiles("default")
        .build();

ZoneDefinition zoneChinookReplicated = ZoneDefinition.builder("ChinookReplicated")
        .ifNotExists()
        .replicas(3)
        .partitions(25)
        .storageProfiles("default")
        .build();
```

7. **Use Indexes Wisely**: Create indexes for frequently queried columns but avoid over-indexing.

```java
// From Track.java - multiple indexes for common queries
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default"),
    colocateBy = @ColumnRef("AlbumId"),
    indexes = {
        @Index(value = "IFK_TrackAlbumId", columns = { @ColumnRef("AlbumId") }),
        @Index(value = "IFK_TrackGenreId", columns = { @ColumnRef("GenreId") }),
        @Index(value = "IFK_TrackMediaTypeId", columns = { @ColumnRef("MediaTypeId") })
    }
)
public class Track { ... }
```

## Handling Case Sensitivity in Column Names

When working with POJOs and Ignite tables, case sensitivity can sometimes cause issues. Here are strategies to handle this:

1. **Use SQL-to-POJO Pattern**: Avoid direct POJO mapping issues by using SQL to retrieve data and manually convert to POJOs.

```java
// From testPojo.java - dealing with case sensitivity
var result = client.sql().execute(null,
        "SELECT * FROM personPojo WHERE id = ? AND id_str = ?", 1, "P001");

if (result.hasNext()) {
    var row = result.next();
    System.out.println("Person found: ID=" + row.intValue("ID") +
            ", ID_STR=" + row.stringValue("ID_STR") +
            ", F_NAME=" + row.stringValue("F_NAME") +
            ", L_NAME=" + row.stringValue("L_NAME") +
            ", STR=" + row.stringValue("STR"));
}
```

2. **Match Case Exactly in Annotations**: Ensure your Java field annotations match the database column names exactly.

```java
// Correct annotation matching database column exactly
@Column(value = "ArtistId", nullable = false)
private Integer ArtistId;

// Note that field name in Java also matches the column name case
```

3. **Handle Nullable Fields Safely**: Use try-catch blocks when retrieving potentially null values from SQL results as shown in the NULL handling section.

## Display Table Columns

You can inspect the table structure as shown in `testPojo.java`:

```java
// From TableUtils.java - displaying table columns
public static void displayTableColumns(IgniteClient client, String tableName) {
    try {
        var dResult = client.sql().execute(null, "SELECT * FROM " + tableName + " LIMIT 1");

        if (dResult.hasNext()) {
            var row = dResult.next();

            System.out.println("\nColumns in " + tableName + " table:");
            for (int i = 0; i < row.columnCount(); i++) {
                String columnName = row.columnName(i);
                Object value = row.value(i);

                System.out.println("  - Column: " + columnName +
                        ", Value: " + value);
            }
        } else {
            System.out.println("No rows found in " + tableName + " table");
        }
    } catch (Exception e) {
        System.err.println("Error querying " + tableName + " table: " + e.getMessage());
    }
}
```
