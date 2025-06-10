# Apache Ignite 3 Developer's Guide: Building Distributed Database Applications

## Introduction

Apache Ignite 3 is a distributed database platform designed for high-performance computing with in-memory speed and scalable persistence. This guide provides a comprehensive walkthrough of key concepts in Ignite 3 using the Chinook database model as a practical example. 

By the end of this guide, you'll understand:

- Fundamental architecture of Ignite 3
- How to create and map tables using Java POJOs
- The concept of Distribution Zones for data management
- Storage Profile options for performance optimization
- Table co-location strategies for efficient joins
- How to perform CRUD operations
- Transaction handling in a distributed environment
- Bulk loading data efficiently

## Core Concepts

### Distribution Zones

Distribution zones in Apache Ignite 3 define how data is distributed and replicated across the cluster:

1. **Partitioning**: How data is split across nodes
2. **Replication**: How many copies of each partition exist
3. **Storage Profiles**: Which storage methods are used

For example, our Chinook application defines two zones:

```java
// Primary entity zone with 2 replicas
ZoneDefinition zoneChinook = ZoneDefinition.builder("Chinook")
        .ifNotExists()
        .replicas(2)
        .storageProfiles("default")
        .build();

// Reference data zone with 3 replicas and fewer partitions
ZoneDefinition zoneChinookReplicated = ZoneDefinition.builder("ChinookReplicated")
        .ifNotExists()
        .replicas(3)
        .partitions(25)
        .storageProfiles("default")
        .build();
```

> [!NOTE]
> Choose distribution zones wisely: primary entities with frequent updates often use fewer replicas, while reference data can benefit from more replicas for better read performance.

### Storage Profiles

Storage profiles define how data is physically stored:

1. **Apache Ignite Page Memory (B+ tree)** - Available in both persistent and volatile (in-memory) variants
2. **RocksDB** - A persistent storage based on LSM-tree optimized for write-heavy workloads

The default storage profile in Ignite 3 uses the persistent Apache Ignite Page Memory engine, which works well for most use cases with a balance of read and write performance.

### POJO to Table Mapping

Ignite 3 provides a powerful annotation-based system for mapping Java classes to database tables:

```java
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default"),
    colocateBy = @ColumnRef("ArtistId"),
    indexes = {
        @Index(value = "IFK_AlbumArtistId", columns = { @ColumnRef("ArtistId") })
    }
)
public class Album {
    @Id
    @Column(value = "AlbumId", nullable = false)
    private Integer albumId;

    @Column(value = "Title", nullable = false)
    private String title;

    @Id
    @Column(value = "ArtistId", nullable = false)
    private Integer artistId;
    
    // Constructors, getters, setters...
}
```

Key annotations:
- `@Table`: Marks a class as a table with zone and co-location properties
- `@Column`: Maps fields to columns with specific properties
- `@Id`: Marks fields that form the primary key
- `@Zone`: Specifies which distribution zone the table belongs to
- `@ColumnRef`: References columns for co-location or indexing

## Setup

### Prerequisites

Before you begin, ensure you have:

- Java 17 or higher
- Apache Maven 3.6+
- Docker and Docker Compose

### Starting the Cluster

1. **Start the Ignite nodes**:

```bash
docker-compose up -d
```

2. **Initialize the cluster**:

```bash
docker run --rm -it --network=host -e LANG=C.UTF-8 -e LC_ALL=C.UTF-8 \
  apacheignite/ignite:3.0.0 cli
connect http://localhost:10300
cluster init --name=ignite3 --metastorage-group=node1,node2,node3
exit
```

This creates a 3-node Ignite cluster ready for use.

## Building Your Database

### Approach 1: Step-by-Step POJO-based Setup

#### 1. Creating Distribution Zones

```java
public static boolean createDistributionZones(IgniteClient client) {
    // Create the Chinook zone with 2 replicas
    ZoneDefinition zoneChinook = ZoneDefinition.builder("Chinook")
            .ifNotExists()
            .replicas(2)
            .storageProfiles("default")
            .build();
    client.catalog().createZone(zoneChinook);

    // Create the ChinookReplicated zone with 3 replicas
    ZoneDefinition zoneChinookReplicated = ZoneDefinition.builder("ChinookReplicated")
            .ifNotExists()
            .replicas(3)
            .partitions(25)
            .storageProfiles("default")
            .build();
    client.catalog().createZone(zoneChinookReplicated);
    
    return true;
}
```

#### 2. Creating Tables from POJOs

```java
// Create tables from annotated classes
client.catalog().createTable(Artist.class);
client.catalog().createTable(Genre.class);
client.catalog().createTable(Album.class);
client.catalog().createTable(Track.class);
// Additional tables...
```

#### 3. Loading Sample Data

```java
// Add a new artist
Artist queen = new Artist(6, "Queen");
Table artistTable = client.tables().table("Artist");
RecordView<Artist> artistView = artistTable.recordView(Artist.class);
artistView.upsert(null, queen);

// Add a new album for this artist
Album newAlbum = new Album(6, "A Night at the Opera", 6);
Table albumTable = client.tables().table("Album");
RecordView<Album> albumView = albumTable.recordView(Album.class);
albumView.upsert(null, newAlbum);
```

### Approach 2: Bulk SQL-based Setup

For faster setup, Ignite 3 supports SQL-based table creation and data loading:

```sql
-- Create a distribution zone
CREATE ZONE IF NOT EXISTS Chinook 
WITH STORAGE_PROFILES='default', REPLICAS=2;

-- Create a table
CREATE TABLE Artist (
    ArtistId INT PRIMARY KEY,
    Name VARCHAR
) ZONE Chinook STORAGE PROFILE 'default';

-- Insert data
INSERT INTO Artist (ArtistId, Name) VALUES (1, 'AC/DC');
```

The bulk loading approach is ideal for initial database setup or complete refreshes.

## Data Co-location Strategy

Data co-location is a key performance optimization in distributed systems. Ignite 3 allows you to co-locate related data on the same cluster nodes, minimizing network transfers.

The Chinook model demonstrates a hierarchical co-location strategy:

1. `Artist` is the root entity
2. `Album` is co-located with the corresponding `Artist`
3. `Track` is co-located with the corresponding `Album`

This ensures that when you query data across these entities (e.g., all tracks by a specific artist), the data is already located on the same physical node.

```java
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default"),
    colocateBy = @ColumnRef("ArtistId")
)
public class Album { ... }

@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default"),
    colocateBy = @ColumnRef("AlbumId")
)
public class Track { ... }
```

## Working with Data

### Basic CRUD Operations

#### Creating Records

```java
// Create a new artist
Artist queen = new Artist(6, "Queen");
Table artistTable = client.tables().table("Artist");
RecordView<Artist> artistView = artistTable.recordView(Artist.class);
artistView.upsert(null, queen);
```

#### Reading Records

```java
// Get artist by ID
Table artistTable = client.tables().table("Artist");
KeyValueView<Integer, Artist> keyValueView = 
    artistTable.keyValueView(Integer.class, Artist.class);
Artist artist = keyValueView.get(null, 6);
```

#### Updating Records

```java
// Update an artist
artist.setName("Queen (Updated)");
RecordView<Artist> artistView = artistTable.recordView(Artist.class);
artistView.upsert(null, artist);
```

#### Deleting Records

```java
// Delete an artist
KeyValueView<Integer, Artist> keyValueView = 
    artistTable.keyValueView(Integer.class, Artist.class);
keyValueView.delete(null, 6);
```

### SQL Queries

Ignite 3 supports standard SQL for querying data:

```java
// Find albums by artist
client.sql().execute(null,
    "SELECT a.Title, ar.Name as ArtistName " +
    "FROM Album a JOIN Artist ar ON a.ArtistId = ar.ArtistId " +
    "WHERE ar.Name = ?", "Queen")
.forEachRemaining(row ->
    System.out.println("Album: " + row.stringValue("Title") +
        " by " + row.stringValue("ArtistName")));
```

### Batch Operations

For better performance with multiple records:

```java
// Add multiple tracks in batch
List<Track> tracks = new ArrayList<>();
tracks.add(track1);
tracks.add(track2);
// ...
RecordView<Track> trackView = trackTable.recordView(Track.class);
trackView.upsertAll(null, tracks);
```

### Transactions

Ignite 3 provides transaction support for atomic operations:

```java
client.transactions().runInTransaction(tx -> {
    // Create a new artist
    Artist newArtist = new Artist(7, "Pink Floyd");
    artistView.upsert(tx, newArtist);

    // Create a new album for this artist
    Album newAlbum = new Album(8, "The Dark Side of the Moon", 7);
    albumView.upsert(tx, newAlbum);

    // Create tracks for this album
    List<Track> newTracks = ChinookUtils.createSampleTracks(8, 10);
    trackView.upsertAll(tx, newTracks);
    
    return true;
});
```

## Performance Best Practices

1. **Use Batch Operations**: When inserting or updating multiple records, use batch operations like `upsertAll()` instead of individual operations.

2. **Leverage Co-location**: Design your schema to co-locate related data to minimize network transfers during joins.

3. **Choose Appropriate Storage Profiles**:
   - Write-heavy workloads: Consider RocksDB storage engine
   - Read-heavy workloads: Use Apache Ignite Page Memory engine

4. **Transaction Management**:
   - Keep transactions short to avoid contention
   - Use transactions only when necessary for data consistency

5. **Distribution Zones**:
   - Use higher replica counts for critical data (3+)
   - Use lower replica counts for less critical data to save storage space

6. **Use Indexes Wisely**: Create indexes for frequently queried columns but avoid over-indexing.

## Monitoring and Maintenance

### Checking Cluster Status

```bash
# Start the CLI
docker run --rm -it --network=host -e LANG=C.UTF-8 -e LC_ALL=C.UTF-8 \
  apacheignite/ignite:3.0.0 cli

# Connect to the cluster
connect http://localhost:10300

# Start SQL CLI
sql

# Check cluster status
SELECT * FROM system.nodes;

# List all zones
SELECT * FROM system.zones;

# Check table assignments
SELECT schema_name, table_name, zone_name 
FROM system.tables 
ORDER BY zone_name, table_name;
```

## Conclusion

Apache Ignite 3 offers a powerful platform for building distributed database applications with high performance and scalability. By leveraging concepts like distribution zones, storage profiles, and data co-location, you can optimize your application for various workloads and use cases.

The Chinook example demonstrates how to apply these concepts in a practical scenario, showing the flexibility of both POJO-based and SQL-based approaches to database design and management.
