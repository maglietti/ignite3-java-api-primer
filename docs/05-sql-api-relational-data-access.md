# 5. SQL API - Relational Data Access

## Basic SQL Operations (`IgniteSql`)

### DDL Operations (CREATE, ALTER, DROP)

```java
// Create tables using SQL DDL (alternative to annotation-based creation)
client.sql().executeScript(
    "CREATE TABLE Artist ("
    + "ArtistId INT PRIMARY KEY,"
    + "Name VARCHAR(120));"
);

// Create related tables with foreign keys
client.sql().executeScript(
    "CREATE TABLE Album ("
    + "AlbumId INT,"
    + "Title VARCHAR(160) NOT NULL,"
    + "ArtistId INT NOT NULL,"
    + "PRIMARY KEY (AlbumId, ArtistId)) "
    + "COLOCATE BY (ArtistId);"
);

// Add indexes for performance
client.sql().executeScript(
    "CREATE INDEX idx_artist_name ON Artist (Name);"
);
```

### DML Operations (INSERT, UPDATE, DELETE)

#### INSERT Operations

```java
// Basic INSERT
client.sql().execute(null,
    "INSERT INTO Artist (ArtistId, Name) VALUES (?, ?)",
    1, "AC/DC");

// INSERT with multiple rows
client.sql().execute(null,
    "INSERT INTO Artist (ArtistId, Name) VALUES " +
    "(?, ?), (?, ?), (?, ?)",
    2, "Accept", 
    3, "Aerosmith", 
    4, "Alanis Morissette");

// INSERT with SELECT (copy data)
client.sql().execute(null,
    "INSERT INTO ArtistBackup (ArtistId, Name) " +
    "SELECT ArtistId, Name FROM Artist WHERE ArtistId < ?",
    10);
```

#### UPDATE Operations

```java
// Basic UPDATE
long updatedRows = client.sql().execute(null,
    "UPDATE Artist SET Name = ? WHERE ArtistId = ?",
    "AC/DC (Updated)", 1).affectedRows();
System.out.println("Updated " + updatedRows + " rows");

// Conditional UPDATE
client.sql().execute(null,
    "UPDATE Artist SET Name = UPPER(Name) WHERE Name LIKE ?",
    "A%");

// UPDATE with JOIN
client.sql().execute(null,
    "UPDATE Album SET Title = ? " +
    "WHERE ArtistId IN (SELECT ArtistId FROM Artist WHERE Name = ?)",
    "Greatest Hits", "AC/DC");
```

#### DELETE Operations

```java
// Basic DELETE
long deletedRows = client.sql().execute(null,
    "DELETE FROM Artist WHERE ArtistId = ?", 1).affectedRows();
System.out.println("Deleted " + deletedRows + " rows");

// Conditional DELETE
client.sql().execute(null,
    "DELETE FROM Artist WHERE Name IS NULL OR Name = ''");

// DELETE with subquery
client.sql().execute(null,
    "DELETE FROM Album WHERE ArtistId NOT IN " +
    "(SELECT ArtistId FROM Artist)");
```

#### MERGE/UPSERT Operations

```java
// MERGE operation (upsert)
client.sql().execute(null,
    "MERGE INTO Artist (ArtistId, Name) VALUES (?, ?) " +
    "ON CONFLICT (ArtistId) DO UPDATE SET Name = EXCLUDED.Name",
    5, "Queen");

// Alternative upsert syntax
client.sql().execute(null,
    "INSERT INTO Artist (ArtistId, Name) VALUES (?, ?) " +
    "ON CONFLICT (ArtistId) DO UPDATE SET Name = EXCLUDED.Name",
    6, "The Beatles");
```

### Query Operations (SELECT)

#### Basic Queries

```java
// Simple SELECT with WHERE clause
try (ResultSet<SqlRow> rs = client.sql().execute(null,
        "SELECT ArtistId, Name FROM Artist WHERE Name LIKE ? ORDER BY Name", "A%")) {
    while (rs.hasNext()) {
        SqlRow row = rs.next();
        System.out.printf("ID: %d, Artist: %s%n", 
            row.intValue("ArtistId"), row.stringValue("Name"));
    }
}
```

#### Complex Joins

```java
// Join artists with their albums and tracks
try (ResultSet<SqlRow> rs = client.sql().execute(null,
        "SELECT ar.Name as ArtistName, al.Title as AlbumTitle, t.Name as TrackName " +
        "FROM Artist ar " +
        "JOIN Album al ON ar.ArtistId = al.ArtistId " +
        "JOIN Track t ON al.AlbumId = t.AlbumId " +
        "WHERE ar.ArtistId = ? " +
        "ORDER BY al.Title, t.TrackId", 1)) {
    
    while (rs.hasNext()) {
        SqlRow row = rs.next();
        System.out.printf("%s - %s - %s%n",
            row.stringValue("ArtistName"),
            row.stringValue("AlbumTitle"), 
            row.stringValue("TrackName"));
    }
}
```

## Prepared Statements

### Statement Builders

```java
Statement stmt = client.sql().createStatement("INSERT INTO CITIES (ID, NAME) VALUES (?, ?)");
```

### Parameter Binding

#### Positional Parameters

```java
// Basic positional parameter binding
Statement stmt = client.sql().createStatement(
    "SELECT * FROM Artist WHERE ArtistId = ? AND Name LIKE ?");

// Execute with parameters
try (ResultSet<SqlRow> result = client.sql().execute(null, stmt, 1, "A%")) {
    while (result.hasNext()) {
        SqlRow row = result.next();
        System.out.println("Artist: " + row.stringValue("Name"));
    }
}
```

#### Type-Safe Parameter Binding

```java
// Using StatementBuilder for more control
Statement complexStmt = client.sql().statementBuilder()
    .query("SELECT a.Name as ArtistName, al.Title as AlbumTitle " +
           "FROM Artist a " +
           "JOIN Album al ON a.ArtistId = al.ArtistId " +
           "WHERE a.ArtistId BETWEEN ? AND ? " +
           "ORDER BY a.Name, al.Title")
    .build();

// Execute with type-safe parameters
try (ResultSet<SqlRow> result = client.sql().execute(null, complexStmt, 1, 10)) {
    while (result.hasNext()) {
        SqlRow row = result.next();
        System.out.printf("%s - %s%n", 
            row.stringValue("ArtistName"), 
            row.stringValue("AlbumTitle"));
    }
}
```

#### Handling NULL Parameters

```java
// Properly handle NULL values in parameters
Integer artistId = null;  // This could be null
String namePattern = "Queen%";

String sql = "SELECT * FROM Artist WHERE (? IS NULL OR ArtistId = ?) AND Name LIKE ?";
try (ResultSet<SqlRow> result = client.sql().execute(null, sql, artistId, artistId, namePattern)) {
    while (result.hasNext()) {
        SqlRow row = result.next();
        System.out.println("Found: " + row.stringValue("Name"));
    }
}
```

#### Dynamic Parameter Lists

```java
// Handle variable number of parameters
public static void findArtistsByIds(IgniteClient client, List<Integer> artistIds) {
    if (artistIds.isEmpty()) return;
    
    // Create placeholder string: ?, ?, ?
    String placeholders = artistIds.stream()
        .map(id -> "?")
        .collect(Collectors.joining(", "));
    
    String sql = "SELECT * FROM Artist WHERE ArtistId IN (" + placeholders + ")";
    
    try (ResultSet<SqlRow> result = client.sql().execute(null, sql, artistIds.toArray())) {
        while (result.hasNext()) {
            SqlRow row = result.next();
            System.out.println("Artist: " + row.stringValue("Name"));
        }
    }
}

// Usage
List<Integer> ids = Arrays.asList(1, 2, 3, 4, 5);
findArtistsByIds(client, ids);
```

### Reusable Statements

#### Statement Caching Pattern

```java
public class StatementCache {
    private final IgniteClient client;
    private final Map<String, Statement> statementCache = new ConcurrentHashMap<>();
    
    public StatementCache(IgniteClient client) {
        this.client = client;
    }
    
    public Statement getStatement(String key, String sql) {
        return statementCache.computeIfAbsent(key, k -> 
            client.sql().createStatement(sql));
    }
    
    // Commonly used statements
    public Statement getArtistByIdStatement() {
        return getStatement("artist_by_id", 
            "SELECT * FROM Artist WHERE ArtistId = ?");
    }
    
    public Statement getAlbumsByArtistStatement() {
        return getStatement("albums_by_artist", 
            "SELECT * FROM Album WHERE ArtistId = ? ORDER BY Title");
    }
    
    public Statement getTracksByAlbumStatement() {
        return getStatement("tracks_by_album", 
            "SELECT * FROM Track WHERE AlbumId = ? ORDER BY TrackId");
    }
}

// Usage
StatementCache cache = new StatementCache(client);

// Reuse statements efficiently
for (int artistId = 1; artistId <= 10; artistId++) {
    try (ResultSet<SqlRow> result = client.sql().execute(
            null, cache.getArtistByIdStatement(), artistId)) {
        
        if (result.hasNext()) {
            SqlRow row = result.next();
            System.out.println("Artist: " + row.stringValue("Name"));
        }
    }
}
```

#### Prepared Statement Utility Class

```java
public class DatabaseQueries {
    private final IgniteClient client;
    private final Statement insertArtistStmt;
    private final Statement updateArtistStmt;
    private final Statement deleteArtistStmt;
    private final Statement findArtistStmt;
    
    public DatabaseQueries(IgniteClient client) {
        this.client = client;
        
        // Prepare all statements at initialization
        this.insertArtistStmt = client.sql().createStatement(
            "INSERT INTO Artist (ArtistId, Name) VALUES (?, ?)");
        
        this.updateArtistStmt = client.sql().createStatement(
            "UPDATE Artist SET Name = ? WHERE ArtistId = ?");
        
        this.deleteArtistStmt = client.sql().createStatement(
            "DELETE FROM Artist WHERE ArtistId = ?");
        
        this.findArtistStmt = client.sql().createStatement(
            "SELECT * FROM Artist WHERE ArtistId = ?");
    }
    
    public void insertArtist(Integer id, String name) {
        client.sql().execute(null, insertArtistStmt, id, name);
    }
    
    public void updateArtist(Integer id, String name) {
        client.sql().execute(null, updateArtistStmt, name, id);
    }
    
    public void deleteArtist(Integer id) {
        client.sql().execute(null, deleteArtistStmt, id);
    }
    
    public Optional<String> findArtistName(Integer id) {
        try (ResultSet<SqlRow> result = client.sql().execute(null, findArtistStmt, id)) {
            if (result.hasNext()) {
                return Optional.of(result.next().stringValue("Name"));
            }
            return Optional.empty();
        }
    }
}

// Usage
DatabaseQueries queries = new DatabaseQueries(client);
queries.insertArtist(100, "New Artist");
queries.updateArtist(100, "Updated Artist");
Optional<String> name = queries.findArtistName(100);
name.ifPresent(System.out::println);
```

#### Complex Query Builder

```java
public class QueryBuilder {
    private final IgniteClient client;
    private String baseQuery;
    private List<String> conditions = new ArrayList<>();
    private List<Object> parameters = new ArrayList<>();
    private String orderBy;
    private Integer limit;
    
    public QueryBuilder(IgniteClient client, String baseQuery) {
        this.client = client;
        this.baseQuery = baseQuery;
    }
    
    public QueryBuilder where(String condition, Object... params) {
        conditions.add(condition);
        parameters.addAll(Arrays.asList(params));
        return this;
    }
    
    public QueryBuilder orderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }
    
    public QueryBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }
    
    public ResultSet<SqlRow> execute() {
        StringBuilder sql = new StringBuilder(baseQuery);
        
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        
        if (orderBy != null) {
            sql.append(" ORDER BY ").append(orderBy);
        }
        
        if (limit != null) {
            sql.append(" LIMIT ").append(limit);
        }
        
        return client.sql().execute(null, sql.toString(), parameters.toArray());
    }
}

// Usage
try (ResultSet<SqlRow> result = new QueryBuilder(client, "SELECT * FROM Artist")
        .where("Name LIKE ?", "A%")
        .where("ArtistId > ?", 5)
        .orderBy("Name")
        .limit(10)
        .execute()) {
    
    while (result.hasNext()) {
        SqlRow row = result.next();
        System.out.println(row.stringValue("Name"));
    }
}
```

## Result Processing

### Working with `ResultSet<SqlRow>`

#### Basic Result Processing

```java
// Basic result iteration
try (ResultSet<SqlRow> result = client.sql().execute(null, 
        "SELECT ArtistId, Name FROM Artist ORDER BY Name")) {
    
    while (result.hasNext()) {
        SqlRow row = result.next();
        
        // Access columns by name
        Integer id = row.intValue("ArtistId");
        String name = row.stringValue("Name");
        
        // Access columns by index (0-based)
        Integer idByIndex = row.intValue(0);
        String nameByIndex = row.stringValue(1);
        
        System.out.println("ID: " + id + ", Name: " + name);
    }
}
```

#### Handling Different Data Types

```java
// Working with various SQL data types
try (ResultSet<SqlRow> result = client.sql().execute(null,
        "SELECT t.TrackId, t.Name, t.Milliseconds, t.Bytes, t.UnitPrice " +
        "FROM Track t WHERE t.AlbumId = ?", 1)) {
    
    while (result.hasNext()) {
        SqlRow row = result.next();
        
        // Integer values
        Integer trackId = row.intValue("TrackId");
        Integer milliseconds = row.intValue("Milliseconds");
        
        // String values
        String name = row.stringValue("Name");
        
        // Long values (for large numbers)
        Long bytes = row.longValue("Bytes");
        
        // BigDecimal for precise decimals
        BigDecimal price = row.decimalValue("UnitPrice");
        
        // Handle nullable values
        Long bytesNullSafe = row.value("Bytes") != null ? row.longValue("Bytes") : 0L;
        
        System.out.printf("Track: %s, Duration: %d ms, Size: %d bytes, Price: $%.2f%n",
            name, milliseconds, bytes, price);
    }
}
```

#### Advanced Result Processing

```java
// Process results into collections
public static List<Map<String, Object>> resultToMapList(ResultSet<SqlRow> result) {
    List<Map<String, Object>> results = new ArrayList<>();
    
    while (result.hasNext()) {
        SqlRow row = result.next();
        Map<String, Object> rowMap = new HashMap<>();
        
        // Get all column names and values
        for (int i = 0; i < row.columnCount(); i++) {
            String columnName = row.columnName(i);
            Object value = row.value(i);
            rowMap.put(columnName, value);
        }
        
        results.add(rowMap);
    }
    
    return results;
}

// Usage
try (ResultSet<SqlRow> result = client.sql().execute(null, "SELECT * FROM Artist LIMIT 5")) {
    List<Map<String, Object>> artists = resultToMapList(result);
    artists.forEach(artist -> {
        System.out.println("Artist: " + artist);
    });
}
```

#### Streaming Large Result Sets

```java
// Process large result sets efficiently
public static void processLargeResultSet(IgniteClient client) {
    // Use streaming to handle large results
    try (ResultSet<SqlRow> result = client.sql().execute(null,
            "SELECT * FROM Track ORDER BY TrackId")) {
        
        int count = 0;
        int batchSize = 100;
        List<String> batch = new ArrayList<>();
        
        while (result.hasNext()) {
            SqlRow row = result.next();
            batch.add(row.stringValue("Name"));
            count++;
            
            // Process in batches to manage memory
            if (count % batchSize == 0) {
                processBatch(batch);
                batch.clear();
                System.out.println("Processed " + count + " tracks");
            }
        }
        
        // Process remaining items
        if (!batch.isEmpty()) {
            processBatch(batch);
        }
        
        System.out.println("Total tracks processed: " + count);
    }
}

private static void processBatch(List<String> trackNames) {
    // Process batch of track names
    trackNames.forEach(name -> {
        // Do something with each track name
    });
}
```

#### Result Set Metadata

```java
// Examine result set structure
try (ResultSet<SqlRow> result = client.sql().execute(null, "SELECT * FROM Artist LIMIT 1")) {
    if (result.hasNext()) {
        SqlRow row = result.next();
        
        System.out.println("Column count: " + row.columnCount());
        
        for (int i = 0; i < row.columnCount(); i++) {
            String columnName = row.columnName(i);
            Object value = row.value(i);
            String type = value != null ? value.getClass().getSimpleName() : "NULL";
            
            System.out.printf("Column %d: %s (%s) = %s%n", i, columnName, type, value);
        }
    }
}
```

### POJO Mapping with `Mapper<T>`

#### Auto-Mapping to POJOs

```java
// Define a POJO for query results
public static class ArtistInfo {
    private Integer artistId;
    private String name;
    private Integer albumCount;
    
    // Default constructor
    public ArtistInfo() {}
    
    // Getters and setters
    public Integer getArtistId() { return artistId; }
    public void setArtistId(Integer artistId) { this.artistId = artistId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAlbumCount() { return albumCount; }
    public void setAlbumCount(Integer albumCount) { this.albumCount = albumCount; }
    
    @Override
    public String toString() {
        return "ArtistInfo{artistId=" + artistId + ", name='" + name + 
               "', albumCount=" + albumCount + "}";
    }
}

// Use auto-mapping with complex queries
String sql = "SELECT ar.ArtistId, ar.Name, COUNT(al.AlbumId) as albumCount " +
            "FROM Artist ar " +
            "LEFT JOIN Album al ON ar.ArtistId = al.ArtistId " +
            "GROUP BY ar.ArtistId, ar.Name " +
            "ORDER BY albumCount DESC, ar.Name";

try (ResultSet<ArtistInfo> rs = client.sql().execute(null, Mapper.of(ArtistInfo.class), sql)) {
    while (rs.hasNext()) {
        ArtistInfo artist = rs.next();
        System.out.println(artist);
    }
}
```

#### Custom Field Mapping

```java
// POJO with different field names
public static class TrackSummary {
    private String trackTitle;      // Maps to "Name" column
    private String artistName;      // Maps to computed field
    private String albumTitle;      // Maps to "Title" column
    private Double priceInDollars;  // Maps to "UnitPrice" column
    
    // Constructors, getters, setters...
}

// Custom mapper for different field names
Mapper<TrackSummary> customMapper = Mapper.<TrackSummary>builder()
    .map("trackTitle", "TrackName")
    .map("artistName", "ArtistName")
    .map("albumTitle", "AlbumTitle")
    .map("priceInDollars", "UnitPrice")
    .build();

String complexSql = "SELECT " +
    "t.Name as TrackName, " +
    "ar.Name as ArtistName, " +
    "al.Title as AlbumTitle, " +
    "t.UnitPrice " +
    "FROM Track t " +
    "JOIN Album al ON t.AlbumId = al.AlbumId " +
    "JOIN Artist ar ON al.ArtistId = ar.ArtistId " +
    "WHERE t.UnitPrice > ? " +
    "ORDER BY t.UnitPrice DESC";

try (ResultSet<TrackSummary> rs = client.sql().execute(
        null, customMapper, complexSql, new BigDecimal("1.00"))) {
    
    while (rs.hasNext()) {
        TrackSummary track = rs.next();
        System.out.println(track);
    }
}
```

### Iterating Through Results

#### Standard Iteration Patterns

```java
// Basic while loop iteration
try (ResultSet<SqlRow> result = client.sql().execute(null, 
        "SELECT * FROM Artist ORDER BY Name")) {
    
    while (result.hasNext()) {
        SqlRow row = result.next();
        System.out.println("Artist: " + row.stringValue("Name"));
    }
}

// For-each style iteration (requires conversion)
try (ResultSet<SqlRow> result = client.sql().execute(null, 
        "SELECT * FROM Artist ORDER BY Name")) {
    
    result.forEachRemaining(row -> {
        System.out.println("Artist: " + row.stringValue("Name"));
    });
}
```

#### Stream-Based Processing

```java
// Convert ResultSet to Stream for functional processing
public static Stream<SqlRow> resultSetToStream(ResultSet<SqlRow> resultSet) {
    Spliterator<SqlRow> spliterator = Spliterators.spliteratorUnknownSize(
        resultSet, Spliterator.ORDERED);
    return StreamSupport.stream(spliterator, false);
}

// Usage with streams
try (ResultSet<SqlRow> result = client.sql().execute(null,
        "SELECT ArtistId, Name FROM Artist")) {
    
    List<String> artistNames = resultSetToStream(result)
        .map(row -> row.stringValue("Name"))
        .filter(name -> name.startsWith("A"))
        .sorted()
        .collect(Collectors.toList());
    
    artistNames.forEach(System.out::println);
}
```

#### Pagination Patterns

```java
// Implement pagination for large result sets
public static void paginateResults(IgniteClient client, int pageSize) {
    int offset = 0;
    int pageNumber = 1;
    boolean hasMore = true;
    
    while (hasMore) {
        String sql = "SELECT ArtistId, Name FROM Artist " +
                    "ORDER BY ArtistId LIMIT ? OFFSET ?";
        
        try (ResultSet<SqlRow> result = client.sql().execute(null, sql, pageSize, offset)) {
            int count = 0;
            
            System.out.println("--- Page " + pageNumber + " ---");
            while (result.hasNext()) {
                SqlRow row = result.next();
                System.out.printf("%d: %s%n", 
                    row.intValue("ArtistId"), 
                    row.stringValue("Name"));
                count++;
            }
            
            hasMore = count == pageSize;
            offset += pageSize;
            pageNumber++;
            
            if (hasMore) {
                System.out.println("Press Enter for next page...");
                System.console().readLine();
            }
        }
    }
}

// Usage
paginateResults(client, 10);
```

#### Grouped Processing

```java
// Group results by a field
public static Map<String, List<SqlRow>> groupResultsByField(
        ResultSet<SqlRow> resultSet, String groupField) {
    
    Map<String, List<SqlRow>> groups = new HashMap<>();
    
    while (resultSet.hasNext()) {
        SqlRow row = resultSet.next();
        String groupKey = row.stringValue(groupField);
        
        groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(row);
    }
    
    return groups;
}

// Usage
try (ResultSet<SqlRow> result = client.sql().execute(null,
        "SELECT ar.Name as ArtistName, al.Title as AlbumTitle " +
        "FROM Artist ar JOIN Album al ON ar.ArtistId = al.ArtistId " +
        "ORDER BY ar.Name, al.Title")) {
    
    Map<String, List<SqlRow>> albumsByArtist = groupResultsByField(result, "ArtistName");
    
    albumsByArtist.forEach((artist, albums) -> {
        System.out.println("Artist: " + artist);
        albums.forEach(album -> {
            System.out.println("  - " + album.stringValue("AlbumTitle"));
        });
    });
}
```

#### Error-Safe Iteration

```java
// Safe iteration with error handling
public static void safeIteration(IgniteClient client) {
    try (ResultSet<SqlRow> result = client.sql().execute(null, 
            "SELECT * FROM Artist")) {
        
        int processedCount = 0;
        int errorCount = 0;
        
        while (result.hasNext()) {
            try {
                SqlRow row = result.next();
                
                // Process row - might throw exceptions
                processRow(row);
                processedCount++;
                
            } catch (Exception e) {
                errorCount++;
                System.err.println("Error processing row: " + e.getMessage());
                // Continue with next row
            }
        }
        
        System.out.println("Processed: " + processedCount + ", Errors: " + errorCount);
        
    } catch (Exception e) {
        System.err.println("Query execution failed: " + e.getMessage());
    }
}

private static void processRow(SqlRow row) {
    // Simulate row processing that might fail
    String name = row.stringValue("Name");
    if (name == null || name.trim().isEmpty()) {
        throw new IllegalArgumentException("Invalid artist name");
    }
    System.out.println("Processed: " + name);
}
```

#### Memory-Efficient Iteration

```java
// Process large result sets with minimal memory usage
public static void memoryEfficientProcessing(IgniteClient client) {
    String sql = "SELECT ArtistId, Name FROM Artist ORDER BY ArtistId";
    
    try (ResultSet<SqlRow> result = client.sql().execute(null, sql)) {
        // Don't collect all results - process one by one
        while (result.hasNext()) {
            SqlRow row = result.next();
            
            // Process immediately and don't store
            processArtistRow(row);
            
            // Optional: periodically report progress
            if (row.intValue("ArtistId") % 100 == 0) {
                System.out.println("Processed up to ID: " + row.intValue("ArtistId"));
            }
        }
    }
}

private static void processArtistRow(SqlRow row) {
    // Do something with the row without storing it
    Integer id = row.intValue("ArtistId");
    String name = row.stringValue("Name");
    
    // Example: write to file, send to another system, etc.
    System.out.println(id + ": " + name);
}
```

## Batch Operations

### Batch Inserts/Updates

#### Basic Batch Operations

```java
// Batch insert artists
long rowsAdded = Arrays.stream(client.sql().executeBatch(null,
    "INSERT INTO Artist (ArtistId, Name) VALUES (?, ?)",
    BatchedArguments.of(1, "AC/DC")
        .add(2, "Accept")
        .add(3, "Aerosmith")
        .add(4, "Alanis Morissette")
        .add(5, "Alice In Chains")))
    .sum();

System.out.println("Inserted " + rowsAdded + " artists");
```

#### Batch Updates with Related Data

```java
// Batch insert albums for multiple artists
BatchedArguments albumArgs = BatchedArguments
    .of(1, "For Those About To Rock We Salute You", 1)  // AC/DC
    .add(2, "Balls to the Wall", 2)                     // Accept
    .add(3, "Restless and Wild", 2)                     // Accept
    .add(4, "Let There Be Rock", 1)                     // AC/DC
    .add(5, "Big Ones", 3);                             // Aerosmith

long albumsAdded = Arrays.stream(client.sql().executeBatch(null,
    "INSERT INTO Album (AlbumId, Title, ArtistId) VALUES (?, ?, ?)",
    albumArgs)).sum();

System.out.println("Inserted " + albumsAdded + " albums");
```

#### Batch Updates

```java
// Batch update artist names
BatchedArguments updateArgs = BatchedArguments
    .of("AC/DC (Remastered)", 1)
    .add("Accept (Special Edition)", 2)
    .add("Aerosmith (Greatest Hits)", 3);

long rowsUpdated = Arrays.stream(client.sql().executeBatch(null,
    "UPDATE Artist SET Name = ? WHERE ArtistId = ?",
    updateArgs)).sum();

System.out.println("Updated " + rowsUpdated + " artist names");
```

### Performance Considerations

#### Batch Size Optimization

```java
// Optimal batch sizes for different operations
public class BatchOptimization {
    
    // Small batches for frequent operations
    public static void frequentSmallBatches(IgniteClient client) {
        List<Object[]> batch = new ArrayList<>();
        String sql = "INSERT INTO Artist (ArtistId, Name) VALUES (?, ?)";
        
        for (int i = 1; i <= 100; i++) {
            batch.add(new Object[]{i, "Artist " + i});
            
            // Process every 10 records for quick feedback
            if (i % 10 == 0) {
                executeBatch(client, sql, batch);
                batch.clear();
            }
        }
        
        // Process remaining
        if (!batch.isEmpty()) {
            executeBatch(client, sql, batch);
        }
    }
    
    // Large batches for bulk operations
    public static void bulkLargeBatches(IgniteClient client) {
        List<Object[]> batch = new ArrayList<>();
        String sql = "INSERT INTO Track (TrackId, Name, AlbumId, MediaTypeId, GenreId, UnitPrice) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
        
        for (int i = 1; i <= 10000; i++) {
            batch.add(new Object[]{i, "Track " + i, 1, 1, 1, new BigDecimal("0.99")});
            
            // Process every 1000 records for bulk efficiency
            if (i % 1000 == 0) {
                executeBatch(client, sql, batch);
                batch.clear();
                System.out.println("Processed " + i + " tracks");
            }
        }
    }
    
    private static void executeBatch(IgniteClient client, String sql, List<Object[]> batch) {
        BatchedArguments batchedArgs = BatchedArguments.of(batch.get(0));
        for (int i = 1; i < batch.size(); i++) {
            batchedArgs.add(batch.get(i));
        }
        client.sql().executeBatch(null, sql, batchedArgs);
    }
}
```

#### Query Optimization Techniques

```java
// Use indexes effectively
public static void optimizedQueries(IgniteClient client) {
    // GOOD: Use indexed columns in WHERE clauses
    client.sql().execute(null,
        "SELECT * FROM Artist WHERE ArtistId = ?", 1);
    
    // GOOD: Use co-location for joins
    client.sql().execute(null,
        "SELECT ar.Name, al.Title " +
        "FROM Artist ar " +
        "JOIN Album al ON ar.ArtistId = al.ArtistId " +  // Co-located join
        "WHERE ar.ArtistId = ?", 1);
    
    // AVOID: Functions on indexed columns
    // BAD: "SELECT * FROM Artist WHERE UPPER(Name) = 'AC/DC'"
    // GOOD: Store data in consistent case or use functional indexes
    
    // GOOD: Limit result sets
    client.sql().execute(null,
        "SELECT * FROM Track ORDER BY TrackId LIMIT 100");
    
    // GOOD: Use EXISTS instead of IN for large subqueries
    client.sql().execute(null,
        "SELECT * FROM Artist ar " +
        "WHERE EXISTS (SELECT 1 FROM Album al WHERE al.ArtistId = ar.ArtistId)");
}
```

#### Connection and Statement Pooling

```java
public class PerformanceOptimizedClient {
    private final IgniteClient client;
    private final Map<String, Statement> statementCache;
    private final ExecutorService executor;
    
    public PerformanceOptimizedClient(IgniteClient client) {
        this.client = client;
        this.statementCache = new ConcurrentHashMap<>();
        this.executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());
    }
    
    // Reuse prepared statements
    public Statement getCachedStatement(String key, String sql) {
        return statementCache.computeIfAbsent(key, k -> client.sql().createStatement(sql));
    }
    
    // Parallel query execution
    public List<CompletableFuture<ResultSet<SqlRow>>> executeQueriesInParallel(
            List<String> queries) {
        
        return queries.stream()
            .map(query -> CompletableFuture.supplyAsync(() -> 
                client.sql().execute(null, query), executor))
            .collect(Collectors.toList());
    }
    
    // Bulk operations with optimal batching
    public void bulkInsertOptimized(String table, List<Object[]> data) {
        int optimalBatchSize = calculateOptimalBatchSize(data.size());
        
        for (int i = 0; i < data.size(); i += optimalBatchSize) {
            int endIndex = Math.min(i + optimalBatchSize, data.size());
            List<Object[]> batch = data.subList(i, endIndex);
            
            // Execute batch asynchronously
            CompletableFuture.runAsync(() -> processBatch(table, batch), executor);
        }
    }
    
    private int calculateOptimalBatchSize(int totalSize) {
        // Adaptive batch sizing based on data volume
        if (totalSize < 1000) return 100;
        else if (totalSize < 10000) return 500;
        else return 1000;
    }
    
    private void processBatch(String table, List<Object[]> batch) {
        // Implementation depends on table structure
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}
```

#### Memory Management

```java
// Efficient memory usage patterns
public static void memoryEfficientProcessing(IgniteClient client) {
    // Use streaming for large result sets
    String sql = "SELECT * FROM Track";
    
    try (ResultSet<SqlRow> result = client.sql().execute(null, sql)) {
        // Process records immediately without storing in memory
        result.forEachRemaining(row -> {
            // Process row immediately
            processTrack(row);
            
            // Don't accumulate in collections
        });
    }
}

// Avoid memory leaks with proper resource management
public static void properResourceManagement(IgniteClient client) {
    // GOOD: Use try-with-resources
    try (ResultSet<SqlRow> result = client.sql().execute(null, "SELECT * FROM Artist")) {
        // Process results
    } // Automatically closed
    
    // AVOID: Manual resource management
    // ResultSet<SqlRow> result = client.sql().execute(null, "SELECT * FROM Artist");
    // // Process results
    // result.close(); // Easy to forget!
}

private static void processTrack(SqlRow row) {
    // Minimal processing to avoid memory accumulation
    String name = row.stringValue("Name");
    System.out.println(name);
}
```

#### Performance Monitoring

```java
public class QueryPerformanceMonitor {
    
    public static <T> T timeQuery(String description, Supplier<T> querySupplier) {
        long start = System.currentTimeMillis();
        try {
            T result = querySupplier.get();
            long duration = System.currentTimeMillis() - start;
            System.out.printf("%s completed in %d ms%n", description, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            System.err.printf("%s failed after %d ms: %s%n", 
                description, duration, e.getMessage());
            throw e;
        }
    }
    
    // Usage
    public static void monitoredQueries(IgniteClient client) {
        // Monitor single query
        timeQuery("Artist lookup", () -> 
            client.sql().execute(null, "SELECT * FROM Artist WHERE ArtistId = ?", 1));
        
        // Monitor batch operation
        timeQuery("Bulk insert", () -> {
            BatchedArguments args = BatchedArguments.of(100, "Test Artist");
            return client.sql().executeBatch(null, 
                "INSERT INTO Artist (ArtistId, Name) VALUES (?, ?)", args);
        });
    }
}
```

#### Best Practices Summary

1. **Use Prepared Statements**: Cache and reuse prepared statements
2. **Optimal Batch Sizes**: 100-1000 records per batch depending on operation
3. **Limit Result Sets**: Always use LIMIT for large queries
4. **Leverage Indexes**: Use indexed columns in WHERE clauses
5. **Co-location Aware**: Design queries to take advantage of co-location
6. **Stream Processing**: Process large result sets without accumulating in memory
7. **Resource Management**: Always use try-with-resources for automatic cleanup
8. **Parallel Execution**: Use async operations for independent queries
9. **Monitor Performance**: Track query execution times
10. **Connection Pooling**: Reuse connections and statements when possible

## Async SQL Operations

#### Basic Async Queries

```java
// Async query execution
CompletableFuture<ResultSet<SqlRow>> futureResult = 
    client.sql().executeAsync(null, "SELECT * FROM Artist ORDER BY Name");

futureResult.thenAccept(resultSet -> {
    System.out.println("Query completed asynchronously");
    try (ResultSet<SqlRow> rs = resultSet) {
        while (rs.hasNext()) {
            SqlRow row = rs.next();
            System.out.println("Artist: " + row.stringValue("Name"));
        }
    }
}).exceptionally(throwable -> {
    System.err.println("Query failed: " + throwable.getMessage());
    return null;
});
```

#### Async with POJO Mapping

```java
// Async query with auto-mapping
CompletableFuture<ResultSet<ArtistInfo>> asyncMappedResult = 
    client.sql().executeAsync(null, Mapper.of(ArtistInfo.class),
        "SELECT ArtistId, Name, 0 as albumCount FROM Artist WHERE ArtistId BETWEEN ? AND ?", 
        1, 10);

asyncMappedResult.thenAccept(resultSet -> {
    try (ResultSet<ArtistInfo> rs = resultSet) {
        rs.forEachRemaining(artist -> {
            System.out.println("Async result: " + artist);
        });
    }
});
```

#### Chaining Async Operations

```java
// Chain multiple async SQL operations
CompletableFuture<Void> chainedOperations = client.sql()
    .executeAsync(null, "SELECT ArtistId FROM Artist WHERE Name = ?", "AC/DC")
    .thenCompose(artistResult -> {
        try (ResultSet<SqlRow> rs = artistResult) {
            if (rs.hasNext()) {
                Integer artistId = rs.next().intValue("ArtistId");
                // Chain with albums query
                return client.sql().executeAsync(null, 
                    "SELECT * FROM Album WHERE ArtistId = ?", artistId);
            } else {
                return CompletableFuture.completedFuture(null);
            }
        }
    })
    .thenAccept(albumResult -> {
        if (albumResult != null) {
            try (ResultSet<SqlRow> rs = albumResult) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.println("Album: " + row.stringValue("Title"));
                }
            }
        }
    });

// Wait for completion or handle async
chainedOperations.join();
```

#### Parallel Async Queries

```java
// Execute multiple queries in parallel
List<CompletableFuture<Integer>> parallelCounts = Arrays.asList(
    client.sql().executeAsync(null, "SELECT COUNT(*) as count FROM Artist")
        .thenApply(rs -> { try (ResultSet<SqlRow> r = rs) { 
            return r.hasNext() ? r.next().intValue("count") : 0; } }),
    
    client.sql().executeAsync(null, "SELECT COUNT(*) as count FROM Album")
        .thenApply(rs -> { try (ResultSet<SqlRow> r = rs) { 
            return r.hasNext() ? r.next().intValue("count") : 0; } }),
    
    client.sql().executeAsync(null, "SELECT COUNT(*) as count FROM Track")
        .thenApply(rs -> { try (ResultSet<SqlRow> r = rs) { 
            return r.hasNext() ? r.next().intValue("count") : 0; } })
);

// Wait for all to complete
CompletableFuture.allOf(parallelCounts.toArray(new CompletableFuture[0]))
    .thenRun(() -> {
        System.out.println("Artists: " + parallelCounts.get(0).join());
        System.out.println("Albums: " + parallelCounts.get(1).join());
        System.out.println("Tracks: " + parallelCounts.get(2).join());
    });
```
