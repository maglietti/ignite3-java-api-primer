# 5. SQL API - Java Interface for Relational Data Access

The Ignite 3 SQL API provides a comprehensive Java interface for relational database operations in distributed environments. This module focuses on teaching developers how to effectively use the `IgniteSql` interface and related classes to perform database operations, handle results, and integrate with transactions.

## Overview: The SQL API Architecture

The SQL API is designed around a few core interfaces that provide both synchronous and asynchronous access to SQL operations:

```mermaid
graph TB
    subgraph "Core SQL API Interfaces"
        A[IgniteSql] --> B[Statement]
        A --> C[ResultSet&lt;SqlRow&gt;]
        A --> D[AsyncResultSet&lt;SqlRow&gt;]
        B --> E[StatementBuilder]
        C --> F[SqlRow]
        F --> G[ResultSetMetadata]
    end
    
    subgraph "Supporting Interfaces"
        H[BatchedArguments] --> A
        I[Mapper&lt;T&gt;] --> A
        J[Transaction] --> A
    end
    
    subgraph "Client Access"
        K[IgniteClient] --> L[client.sql()]
        L --> A
    end
```

**Key Design Principles:**

- **Type Safety**: Strongly typed interfaces with generic support
- **Resource Management**: All operations support try-with-resources patterns
- **Transaction Integration**: Seamless integration with Ignite transactions
- **Async Support**: CompletableFuture-based asynchronous operations
- **Result Streaming**: Efficient handling of large result sets

## Getting Started with the IgniteSql Interface

The `IgniteSql` interface is your primary entry point for all SQL operations. You obtain it from the main client interface and use it for both DDL and DML operations.

### Obtaining the SQL Interface

```java
/**
 * Demonstrates basic SQL API access and simple query execution.
 * The IgniteSql interface is obtained from the main client and provides
 * access to all SQL operations in Ignite 3.
 */
public class BasicSQLAccess {
    
    public void demonstrateBasicAccess() {
        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800")
                .build()) {
            
            // Obtain the SQL interface - this is your gateway to all SQL operations
            IgniteSql sql = client.sql();
            
            // Simple query execution - returns ResultSet<SqlRow>
            ResultSet<SqlRow> artists = sql.execute(null, "SELECT ArtistId, Name FROM Artist LIMIT 5");
            
            // Process results using iterator pattern
            while (artists.hasNext()) {
                SqlRow row = artists.next();
                int artistId = row.intValue("ArtistId");
                String name = row.stringValue("Name");
                System.out.println("Artist: " + artistId + " - " + name);
            }
        }
    }
}
```

### Core Execute Methods

The `IgniteSql` interface provides several execute methods for different use cases:

```java
/**
 * Demonstrates the primary execute methods available in the IgniteSql interface.
 * Understanding when to use each method is crucial for effective SQL API usage.
 */
public class SQLExecuteMethods {
    
    private IgniteSql sql;
    
    public void demonstrateExecuteMethods() {
        // 1. Basic string query execution
        ResultSet<SqlRow> result1 = sql.execute(null, "SELECT * FROM Artist WHERE ArtistId = ?", 1);
        
        // 2. Statement-based execution (better for repeated queries)
        Statement stmt = sql.statementBuilder()
            .query("SELECT * FROM Artist WHERE ArtistId = ?")
            .build();
        ResultSet<SqlRow> result2 = sql.execute(null, stmt, 1);
        
        // 3. Execute with object mapping (returns typed objects instead of SqlRow)
        ResultSet<Artist> result3 = sql.execute(null, Mapper.of(Artist.class), 
            "SELECT ArtistId, Name FROM Artist WHERE ArtistId = ?", 1);
        
        // 4. Batch execution for bulk operations
        BatchedArguments batch = BatchedArguments.create()
            .add("Rock Artist")
            .add("Jazz Artist")
            .add("Pop Artist");
        long[] insertCounts = sql.executeBatch(null, 
            "INSERT INTO Artist (Name) VALUES (?)", batch);
        
        // 5. Script execution for multiple statements
        sql.executeScript(
            "CREATE ZONE IF NOT EXISTS MusicZone WITH REPLICAS=2;" +
            "CREATE TABLE IF NOT EXISTS NewTable (id INT PRIMARY KEY) WITH PRIMARY_ZONE='MusicZone';"
        );
    }
}
```

### Parameter Binding and Safety

Always use parameter binding to prevent SQL injection and ensure type safety:

```java
/**
 * Demonstrates proper parameter binding techniques for secure SQL execution.
 * Parameter binding is essential for both security and performance.
 */
public class ParameterBinding {
    
    public void demonstrateParameterBinding(IgniteSql sql) {
        // ✅ CORRECT: Use parameter placeholders
        String artistName = "Led Zeppelin";
        ResultSet<SqlRow> result = sql.execute(null, 
            "SELECT * FROM Artist WHERE Name = ?", artistName);
        
        // ✅ CORRECT: Multiple parameters in order
        ResultSet<SqlRow> albums = sql.execute(null,
            "SELECT * FROM Album WHERE ArtistId = ? AND Title LIKE ?", 
            1, "%Rock%");
        
        // ✅ CORRECT: Working with different data types
        LocalDate date = LocalDate.of(2023, 1, 1);
        BigDecimal minPrice = new BigDecimal("9.99");
        ResultSet<SqlRow> tracks = sql.execute(null,
            "SELECT * FROM Track WHERE UnitPrice >= ? AND LastModified > ?",
            minPrice, date);
        
        // ❌ WRONG: String concatenation (vulnerable to SQL injection)
        // String query = "SELECT * FROM Artist WHERE Name = '" + artistName + "'";
        // sql.execute(null, query); // DON'T DO THIS!
    }
}
```

## Statement Management and Configuration

The `Statement` interface allows you to configure query execution parameters and reuse prepared queries for better performance.

### Creating and Configuring Statements

```java
/**
 * Demonstrates advanced statement configuration for optimized query execution.
 * Statement builders provide fine-grained control over query behavior.
 */
public class StatementConfiguration {
    
    public void demonstrateStatementBuilder(IgniteSql sql) {
        // Basic statement creation
        Statement basicStmt = sql.statementBuilder()
            .query("SELECT * FROM Artist WHERE Name LIKE ?")
            .build();
        
        // Advanced statement configuration
        Statement advancedStmt = sql.statementBuilder()
            .query("SELECT * FROM Track ORDER BY Milliseconds DESC")
            .defaultSchema("MUSIC_STORE")           // Set default schema
            .queryTimeout(30, TimeUnit.SECONDS)     // Set timeout
            .pageSize(1000)                         // Configure result paging
            .timeZoneId(ZoneId.of("UTC"))          // Set timezone for temporal operations
            .build();
        
        // Execute configured statement
        ResultSet<SqlRow> longTracks = sql.execute(null, advancedStmt);
        
        // Statement reuse for performance
        Statement artistLookup = sql.statementBuilder()
            .query("SELECT ArtistId, Name FROM Artist WHERE Name = ?")
            .build();
        
        // Reuse the same statement with different parameters
        ResultSet<SqlRow> beatles = sql.execute(null, artistLookup, "The Beatles");
        ResultSet<SqlRow> stones = sql.execute(null, artistLookup, "The Rolling Stones");
    }
    
    /**
     * Demonstrates pagination configuration for large result sets.
     */
    public void demonstratePagination(IgniteSql sql) {
        // Configure statement for paged results
        Statement pagedQuery = sql.statementBuilder()
            .query("SELECT * FROM Track ORDER BY TrackId")
            .pageSize(100)  // Fetch 100 rows at a time
            .build();
        
        ResultSet<SqlRow> tracks = sql.execute(null, pagedQuery);
        
        int pageCount = 0;
        while (tracks.hasNext()) {
            int rowsInPage = 0;
            while (tracks.hasNext() && rowsInPage < 100) {
                SqlRow track = tracks.next();
                rowsInPage++;
                // Process track
            }
            pageCount++;
            System.out.println("Processed page " + pageCount + " with " + rowsInPage + " rows");
        }
    }
}
```

## ResultSet Processing and Data Access

Understanding how to work with `ResultSet<SqlRow>` and extract typed data is fundamental to effective SQL API usage.

### Basic ResultSet Operations

```java
/**
 * Demonstrates comprehensive ResultSet processing patterns.
 * Understanding these patterns is essential for effective data retrieval.
 */
public class ResultSetProcessing {
    
    public void demonstrateBasicProcessing(IgniteSql sql) {
        ResultSet<SqlRow> result = sql.execute(null, 
            "SELECT ArtistId, Name, Country FROM Artist ORDER BY Name");
        
        // Check if result contains rows (vs. just affected row count)
        if (result.hasRowSet()) {
            System.out.println("Query returned data rows");
            
            // Iterate through results
            while (result.hasNext()) {
                SqlRow row = result.next();
                
                // Type-safe data extraction by column name
                int artistId = row.intValue("ArtistId");
                String name = row.stringValue("Name");
                String country = row.stringValue("Country");  // May be null
                
                System.out.printf("Artist %d: %s from %s%n", 
                    artistId, name, country != null ? country : "Unknown");
            }
        } else {
            // DML operations return affected row count instead of data
            long affectedRows = result.affectedRows();
            System.out.println("Operation affected " + affectedRows + " rows");
        }
    }
    
    /**
     * Demonstrates handling different data types from SQL results.
     */
    public void demonstrateDataTypes(IgniteSql sql) {
        ResultSet<SqlRow> result = sql.execute(null,
            "SELECT TrackId, Name, Milliseconds, UnitPrice, LastModified " +
            "FROM Track WHERE TrackId = ?", 1);
        
        if (result.hasNext()) {
            SqlRow row = result.next();
            
            // Numeric types
            int trackId = row.intValue("TrackId");
            long duration = row.longValue("Milliseconds");
            BigDecimal price = row.decimalValue("UnitPrice");
            
            // String types
            String trackName = row.stringValue("Name");
            
            // Temporal types
            LocalDate modifiedDate = row.dateValue("LastModified");
            
            // Handle nullable values
            Integer genreId = row.intValue("GenreId");  // Returns null if column is NULL
            
            System.out.printf("Track: %s (%d ms) - $%s%n", 
                trackName, duration, price);
        }
    }
    
    /**
     * Demonstrates metadata access for dynamic result processing.
     */
    public void demonstrateMetadata(IgniteSql sql) {
        ResultSet<SqlRow> result = sql.execute(null, "SELECT * FROM Artist LIMIT 1");
        
        // Access result metadata
        ResultSetMetadata metadata = result.metadata();
        
        System.out.println("Columns in Artist table:");
        for (ColumnMetadata column : metadata.columns()) {
            System.out.printf("  %s: %s (%s)%s%n",
                column.name(),
                column.type(),
                column.valueClass().getSimpleName(),
                column.nullable() ? " [NULL]" : " [NOT NULL]");
        }
        
        // Dynamic row processing using metadata
        if (result.hasNext()) {
            SqlRow row = result.next();
            for (ColumnMetadata column : metadata.columns()) {
                Object value = row.value(column.name());
                System.out.printf("%s: %s%n", column.name(), value);
            }
        }
    }
}
```

### Advanced ResultSet Patterns

```java
/**
 * Demonstrates advanced patterns for processing complex query results.
 */
public class AdvancedResultProcessing {
    
    /**
     * Process JOIN query results with hierarchical data.
     */
    public void processJoinResults(IgniteSql sql) {
        String joinQuery = """
            SELECT a.Name as ArtistName, al.Title as AlbumTitle, 
                   t.Name as TrackName, t.UnitPrice
            FROM Artist a
            JOIN Album al ON a.ArtistId = al.ArtistId  
            JOIN Track t ON al.AlbumId = t.AlbumId
            WHERE a.ArtistId = ?
            ORDER BY al.Title, t.TrackId
            """;
        
        ResultSet<SqlRow> result = sql.execute(null, joinQuery, 1);
        
        String currentAlbum = null;
        BigDecimal albumTotal = BigDecimal.ZERO;
        
        while (result.hasNext()) {
            SqlRow row = result.next();
            
            String artist = row.stringValue("ArtistName");
            String album = row.stringValue("AlbumTitle");
            String track = row.stringValue("TrackName");
            BigDecimal price = row.decimalValue("UnitPrice");
            
            // Detect album changes in grouped results
            if (!album.equals(currentAlbum)) {
                if (currentAlbum != null) {
                    System.out.printf("  Album Total: $%s%n", albumTotal);
                }
                System.out.printf("%s - %s:%n", artist, album);
                currentAlbum = album;
                albumTotal = BigDecimal.ZERO;
            }
            
            System.out.printf("  %s - $%s%n", track, price);
            albumTotal = albumTotal.add(price);
        }
        
        if (currentAlbum != null) {
            System.out.printf("  Album Total: $%s%n", albumTotal);
        }
    }
    
    /**
     * Process aggregate query results.
     */
    public void processAggregateResults(IgniteSql sql) {
        String aggregateQuery = """
            SELECT a.Name as ArtistName,
                   COUNT(t.TrackId) as TrackCount,
                   AVG(t.Milliseconds) as AvgDuration,
                   SUM(t.UnitPrice) as TotalValue
            FROM Artist a
            JOIN Album al ON a.ArtistId = al.ArtistId
            JOIN Track t ON al.AlbumId = t.AlbumId  
            GROUP BY a.ArtistId, a.Name
            HAVING COUNT(t.TrackId) > 10
            ORDER BY TotalValue DESC
            """;
        
        ResultSet<SqlRow> result = sql.execute(null, aggregateQuery);
        
        System.out.println("Top Artists by Total Track Value:");
        while (result.hasNext()) {
            SqlRow row = result.next();
            
            String artist = row.stringValue("ArtistName");
            long trackCount = row.longValue("TrackCount");
            long avgDuration = row.longValue("AvgDuration");
            BigDecimal totalValue = row.decimalValue("TotalValue");
            
            System.out.printf("%s: %d tracks, avg %d ms, total value $%s%n",
                artist, trackCount, avgDuration, totalValue);
        }
    }
}
```

## Transaction Integration

The SQL API seamlessly integrates with Ignite's transaction system, allowing you to combine SQL operations with Table API operations in ACID-compliant transactions.

### Basic Transaction Usage

```java
/**
 * Demonstrates transaction integration with SQL operations.
 * Transactions ensure ACID compliance across distributed operations.
 */
public class SQLTransactions {
    
    public void demonstrateBasicTransaction(IgniteClient client) {
        IgniteSql sql = client.sql();
        
        // Execute SQL operations within a transaction
        try (Transaction tx = client.transactions().begin()) {
            // Insert new artist
            sql.execute(tx, "INSERT INTO Artist (Name, Country) VALUES (?, ?)", 
                "New Artist", "USA");
            
            // Get the artist ID (in real scenario, you'd use RETURNING clause or sequence)
            ResultSet<SqlRow> artistResult = sql.execute(tx,
                "SELECT ArtistId FROM Artist WHERE Name = ?", "New Artist");
            
            int artistId = artistResult.hasNext() ? 
                artistResult.next().intValue("ArtistId") : 0;
            
            // Insert album for the artist
            sql.execute(tx, "INSERT INTO Album (ArtistId, Title) VALUES (?, ?)",
                artistId, "Debut Album");
            
            // Commit the transaction
            tx.commit();
            System.out.println("Artist and album created successfully");
            
        } catch (Exception e) {
            System.err.println("Transaction failed: " + e.getMessage());
            // Transaction automatically rolled back when tx is closed
        }
    }
    
    /**
     * Demonstrates mixing Table API and SQL operations in the same transaction.
     */
    public void demonstrateMixedOperations(IgniteClient client) {
        IgniteSql sql = client.sql();
        
        // Get table references for Table API operations
        Table<Tuple, Tuple> artistTable = client.tables().table("Artist");
        
        try (Transaction tx = client.transactions().begin()) {
            // Use Table API for key-value operations
            artistTable.upsert(tx, Tuple.create().set("ArtistId", 999).set("Name", "API Artist"));
            
            // Use SQL API for complex operations in the same transaction
            sql.execute(tx, 
                "UPDATE Artist SET Country = ? WHERE ArtistId = ?", 
                "Canada", 999);
            
            // Query to verify both operations
            ResultSet<SqlRow> result = sql.execute(tx,
                "SELECT ArtistId, Name, Country FROM Artist WHERE ArtistId = ?", 999);
            
            if (result.hasNext()) {
                SqlRow row = result.next();
                System.out.printf("Mixed operation result: %d - %s from %s%n",
                    row.intValue("ArtistId"),
                    row.stringValue("Name"), 
                    row.stringValue("Country"));
            }
            
            tx.commit();
        }
    }
    
    /**
     * Demonstrates transaction rollback and error handling.
     */
    public void demonstrateTransactionRollback(IgniteClient client) {
        IgniteSql sql = client.sql();
        
        try (Transaction tx = client.transactions().begin()) {
            // This operation should succeed
            sql.execute(tx, "INSERT INTO Artist (Name) VALUES (?)", "Test Artist");
            
            // This operation will fail (duplicate key or constraint violation)
            sql.execute(tx, "INSERT INTO Artist (ArtistId, Name) VALUES (?, ?)", 
                1, "Duplicate Artist");  // Assuming ArtistId 1 already exists
            
            tx.commit();  // This line won't be reached if exception occurs
            
        } catch (Exception e) {
            System.err.println("Transaction rolled back due to error: " + e.getMessage());
            // Transaction is automatically rolled back when tx.close() is called
        }
        
        // Verify rollback - "Test Artist" should not exist
        ResultSet<SqlRow> result = sql.execute(null, 
            "SELECT COUNT(*) as count FROM Artist WHERE Name = ?", "Test Artist");
        
        if (result.hasNext()) {
            long count = result.next().longValue("count");
            System.out.println("Test Artist count after rollback: " + count); // Should be 0
        }
    }
}
```

## Asynchronous SQL Operations

For high-performance applications, the SQL API provides asynchronous operations that return `CompletableFuture` objects for non-blocking execution.

### Basic Async Operations

```java
/**
 * Demonstrates asynchronous SQL operations for non-blocking database access.
 * Async operations are essential for high-throughput applications.
 */
public class AsyncSQLOperations {
    
    public void demonstrateBasicAsync(IgniteSql sql) {
        // Async query execution
        CompletableFuture<AsyncResultSet<SqlRow>> future = sql.executeAsync(null,
            "SELECT ArtistId, Name FROM Artist ORDER BY Name");
        
        // Non-blocking result processing
        future.thenAccept(resultSet -> {
            System.out.println("Artists received asynchronously:");
            
            // Process current page
            for (SqlRow row : resultSet.currentPage()) {
                System.out.printf("  %d: %s%n", 
                    row.intValue("ArtistId"), 
                    row.stringValue("Name"));
            }
            
            // Close the result set
            resultSet.closeAsync();
        }).exceptionally(throwable -> {
            System.err.println("Async query failed: " + throwable.getMessage());
            return null;
        });
        
        // Continue with other work while query executes...
        System.out.println("Query submitted, continuing with other work...");
    }
    
    /**
     * Demonstrates async pagination for large result sets.
     */
    public void demonstrateAsyncPagination(IgniteSql sql) {
        Statement pagedStmt = sql.statementBuilder()
            .query("SELECT * FROM Track ORDER BY TrackId")
            .pageSize(100)
            .build();
        
        CompletableFuture<AsyncResultSet<SqlRow>> future = 
            sql.executeAsync(null, pagedStmt);
        
        // Chain operations to process all pages
        processAllPages(future, 1);
    }
    
    private void processAllPages(CompletableFuture<AsyncResultSet<SqlRow>> future, int pageNum) {
        future.thenCompose(resultSet -> {
            System.out.printf("Processing page %d with %d rows%n", 
                pageNum, resultSet.currentPage().size());
            
            // Process current page
            for (SqlRow row : resultSet.currentPage()) {
                int trackId = row.intValue("TrackId");
                String name = row.stringValue("Name");
                // Process track data
            }
            
            // Check for more pages
            if (resultSet.hasMorePages()) {
                return resultSet.fetchNextPage()
                    .thenCompose(nextResultSet -> {
                        processAllPages(CompletableFuture.completedFuture(nextResultSet), pageNum + 1);
                        return CompletableFuture.completedFuture(null);
                    });
            } else {
                return resultSet.closeAsync();
            }
        }).exceptionally(throwable -> {
            System.err.println("Error processing page " + pageNum + ": " + throwable.getMessage());
            return null;
        });
    }
    
    /**
     * Demonstrates parallel async operations for improved performance.
     */
    public void demonstrateParallelOperations(IgniteSql sql) {
        // Execute multiple independent queries in parallel
        CompletableFuture<AsyncResultSet<SqlRow>> artistsFuture = 
            sql.executeAsync(null, "SELECT COUNT(*) as count FROM Artist");
        
        CompletableFuture<AsyncResultSet<SqlRow>> albumsFuture = 
            sql.executeAsync(null, "SELECT COUNT(*) as count FROM Album");
        
        CompletableFuture<AsyncResultSet<SqlRow>> tracksFuture = 
            sql.executeAsync(null, "SELECT COUNT(*) as count FROM Track");
        
        // Combine results when all queries complete
        CompletableFuture.allOf(artistsFuture, albumsFuture, tracksFuture)
            .thenRun(() -> {
                try {
                    long artistCount = getCountFromResult(artistsFuture.get());
                    long albumCount = getCountFromResult(albumsFuture.get());
                    long trackCount = getCountFromResult(tracksFuture.get());
                    
                    System.out.printf("Database Statistics:%n");
                    System.out.printf("  Artists: %d%n", artistCount);
                    System.out.printf("  Albums: %d%n", albumCount);
                    System.out.printf("  Tracks: %d%n", trackCount);
                    
                } catch (Exception e) {
                    System.err.println("Error processing parallel results: " + e.getMessage());
                }
            });
    }
    
    private long getCountFromResult(AsyncResultSet<SqlRow> resultSet) {
        return resultSet.currentPage().iterator().next().longValue("count");
    }
}
```

## Batch Operations for Bulk Data

Batch operations provide efficient bulk data processing capabilities, essential for high-performance data loading and updates.

### Creating and Executing Batches

```java
/**
 * Demonstrates batch operations for efficient bulk data processing.
 * Batch operations significantly improve performance for bulk operations.
 */
public class BatchOperations {
    
    public void demonstrateBasicBatch(IgniteSql sql) {
        // Create batch arguments for multiple inserts
        BatchedArguments artistBatch = BatchedArguments.create()
            .add("The Beatles", "UK")
            .add("Led Zeppelin", "UK")  
            .add("Pink Floyd", "UK")
            .add("Queen", "UK")
            .add("The Rolling Stones", "UK");
        
        // Execute batch insert
        long[] insertCounts = sql.executeBatch(null,
            "INSERT INTO Artist (Name, Country) VALUES (?, ?)", 
            artistBatch);
        
        System.out.printf("Batch insert completed: %d artists inserted%n", insertCounts.length);
        for (int i = 0; i < insertCounts.length; i++) {
            System.out.printf("  Insert %d: %d rows affected%n", i + 1, insertCounts[i]);
        }
    }
    
    /**
     * Demonstrates batch operations from collections.
     */
    public void demonstrateBatchFromCollection(IgniteSql sql) {
        // Prepare data from existing collections
        List<Artist> artists = Arrays.asList(
            new Artist("Elvis Presley", "USA"),
            new Artist("Bob Dylan", "USA"),
            new Artist("Johnny Cash", "USA")
        );
        
        // Build batch from collection
        BatchedArguments.Builder batchBuilder = BatchedArguments.create();
        for (Artist artist : artists) {
            batchBuilder.add(artist.getName(), artist.getCountry());
        }
        BatchedArguments batch = batchBuilder.build();
        
        // Execute batch with transaction
        try (Transaction tx = sql.statementBuilder().build().transaction()) {
            long[] results = sql.executeBatch(tx,
                "INSERT INTO Artist (Name, Country) VALUES (?, ?)", batch);
            
            System.out.printf("Inserted %d artists in transaction%n", results.length);
            tx.commit();
        }
    }
    
    /**
     * Demonstrates async batch operations.
     */
    public void demonstrateAsyncBatch(IgniteSql sql) {
        BatchedArguments albumBatch = BatchedArguments.create()
            .add(1, "Please Please Me")
            .add(1, "With The Beatles")
            .add(1, "A Hard Day's Night")
            .add(1, "Beatles for Sale");
        
        // Execute batch asynchronously
        CompletableFuture<long[]> batchFuture = sql.executeBatchAsync(null,
            "INSERT INTO Album (ArtistId, Title) VALUES (?, ?)", albumBatch);
        
        batchFuture.thenAccept(results -> {
            System.out.printf("Async batch completed: %d albums inserted%n", results.length);
            long totalInserted = Arrays.stream(results).sum();
            System.out.printf("Total rows affected: %d%n", totalInserted);
        }).exceptionally(throwable -> {
            System.err.println("Batch operation failed: " + throwable.getMessage());
            return null;
        });
    }
    
    /**
     * Demonstrates batch error handling and partial success scenarios.
     */
    public void demonstrateBatchErrorHandling(IgniteSql sql) {
        // Create batch with some invalid data
        BatchedArguments mixedBatch = BatchedArguments.create()
            .add("Valid Artist 1", "USA")
            .add("Valid Artist 2", "Canada")
            .add(null, "Invalid")  // This will cause constraint violation
            .add("Valid Artist 3", "Mexico");
        
        try {
            long[] results = sql.executeBatch(null,
                "INSERT INTO Artist (Name, Country) VALUES (?, ?)", mixedBatch);
            
            System.out.println("Batch succeeded with results:");
            for (int i = 0; i < results.length; i++) {
                System.out.printf("  Statement %d: %d rows%n", i, results[i]);
            }
            
        } catch (SqlBatchException e) {
            System.err.println("Batch failed at statement: " + e.updateCount());
            System.err.println("Error: " + e.getMessage());
            
            // Process partial results if available
            long[] partialResults = e.results();
            if (partialResults != null) {
                System.out.println("Partial results before failure:");
                for (int i = 0; i < partialResults.length; i++) {
                    System.out.printf("  Statement %d: %d rows%n", i, partialResults[i]);
                }
            }
        }
    }
    
    // Helper class for batch examples
    private static class Artist {
        private final String name;
        private final String country;
        
        public Artist(String name, String country) {
            this.name = name;
            this.country = country;
        }
        
        public String getName() { return name; }
        public String getCountry() { return country; }
    }
}
```

## Object Mapping with SQL Results

The `Mapper` interface allows you to automatically convert SQL query results into Java objects, providing a more object-oriented approach to data access.

### Basic Object Mapping

```java
/**
 * Demonstrates object mapping for type-safe result processing.
 * Object mapping eliminates boilerplate code and provides compile-time safety.
 */
public class ObjectMapping {
    
    /**
     * Simple POJO for mapping Artist results.
     */
    public static class Artist {
        @Column("ArtistId")
        private Integer artistId;
        
        @Column("Name") 
        private String name;
        
        @Column("Country")
        private String country;
        
        // Constructors, getters, and setters
        public Artist() {}
        
        public Artist(Integer artistId, String name, String country) {
            this.artistId = artistId;
            this.name = name;
            this.country = country;
        }
        
        // Getters and setters...
        public Integer getArtistId() { return artistId; }
        public void setArtistId(Integer artistId) { this.artistId = artistId; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        
        @Override
        public String toString() {
            return String.format("Artist{id=%d, name='%s', country='%s'}", 
                artistId, name, country);
        }
    }
    
    public void demonstrateBasicMapping(IgniteSql sql) {
        // Execute query with automatic object mapping
        ResultSet<Artist> artists = sql.execute(null, 
            Mapper.of(Artist.class),
            "SELECT ArtistId, Name, Country FROM Artist WHERE Country = ?", 
            "UK");
        
        System.out.println("UK Artists:");
        while (artists.hasNext()) {
            Artist artist = artists.next();
            System.out.println("  " + artist);
        }
    }
    
    /**
     * Demonstrates custom mapping configuration.
     */
    public void demonstrateCustomMapping(IgniteSql sql) {
        // Create custom mapper with field mappings
        Mapper<Artist> customMapper = Mapper.builder(Artist.class)
            .map("artistId", "ArtistId")     // Java field -> SQL column
            .map("name", "Name")
            .map("country", "Country")
            .build();
        
        ResultSet<Artist> artists = sql.execute(null, customMapper,
            "SELECT ArtistId, Name, Country FROM Artist LIMIT 5");
        
        artists.forEachRemaining(artist -> 
            System.out.println("Custom mapped: " + artist));
    }
    
    /**
     * Demonstrates single-column mapping for simple types.
     */
    public void demonstrateSingleColumnMapping(IgniteSql sql) {
        // Map to single column for simple aggregates
        ResultSet<String> artistNames = sql.execute(null,
            Mapper.of(String.class, "Name"),
            "SELECT Name FROM Artist ORDER BY Name");
        
        System.out.println("All artist names:");
        artistNames.forEachRemaining(name -> System.out.println("  " + name));
        
        // Map to single column for numeric results
        ResultSet<Long> trackCounts = sql.execute(null,
            Mapper.of(Long.class, "track_count"),
            "SELECT COUNT(*) as track_count FROM Track GROUP BY AlbumId");
        
        System.out.println("Track counts per album:");
        trackCounts.forEachRemaining(count -> System.out.println("  " + count));
    }
}
```

## Error Handling and Resource Management

Proper error handling and resource management are crucial for robust SQL API usage in production applications.

### Exception Handling Patterns

```java
/**
 * Demonstrates comprehensive error handling for SQL operations.
 * Proper error handling is essential for production applications.
 */
public class SQLErrorHandling {
    
    public void demonstrateBasicErrorHandling(IgniteSql sql) {
        try {
            // Potentially problematic operation
            ResultSet<SqlRow> result = sql.execute(null,
                "SELECT * FROM NonExistentTable");
            
            // Process results if successful
            while (result.hasNext()) {
                SqlRow row = result.next();
                // Process row
            }
            
        } catch (SqlException e) {
            // Handle SQL-specific errors
            System.err.println("SQL Error: " + e.getMessage());
            System.err.println("Error Code: " + e.code());
            
            // Log additional context
            System.err.println("Query failed, check table existence and permissions");
            
        } catch (Exception e) {
            // Handle unexpected errors
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Demonstrates transaction error handling and rollback.
     */
    public void demonstrateTransactionErrorHandling(IgniteClient client) {
        IgniteSql sql = client.sql();
        
        try (Transaction tx = client.transactions().begin()) {
            try {
                // Series of operations that might fail
                sql.execute(tx, "INSERT INTO Artist (Name) VALUES (?)", "New Artist");
                
                // This might fail due to constraint violation
                sql.execute(tx, "INSERT INTO Album (ArtistId, Title) VALUES (?, ?)", 
                    999999, "Album by Non-existent Artist");
                
                // If we get here, commit the transaction
                tx.commit();
                System.out.println("Transaction completed successfully");
                
            } catch (SqlException e) {
                System.err.println("SQL error in transaction: " + e.getMessage());
                // Transaction will be rolled back automatically when tx is closed
                
            } catch (Exception e) {
                System.err.println("Unexpected error in transaction: " + e.getMessage());
                // Transaction will be rolled back automatically when tx is closed
            }
            
        } catch (Exception e) {
            System.err.println("Failed to create transaction: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates retry patterns for transient failures.
     */
    public void demonstrateRetryPattern(IgniteSql sql) {
        int maxRetries = 3;
        int retryCount = 0;
        boolean success = false;
        
        while (!success && retryCount < maxRetries) {
            try {
                ResultSet<SqlRow> result = sql.execute(null,
                    "SELECT COUNT(*) as count FROM Artist");
                
                if (result.hasNext()) {
                    long count = result.next().longValue("count");
                    System.out.println("Artist count: " + count);
                    success = true;
                }
                
            } catch (SqlException e) {
                retryCount++;
                System.err.printf("Attempt %d failed: %s%n", retryCount, e.getMessage());
                
                if (retryCount < maxRetries) {
                    try {
                        // Wait before retry with exponential backoff
                        Thread.sleep(1000 * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    System.err.println("All retry attempts exhausted");
                    throw e; // Re-throw after all retries failed
                }
            }
        }
    }
    
    /**
     * Demonstrates comprehensive resource cleanup.
     */
    public void demonstrateResourceManagement() {
        // Proper resource management with try-with-resources
        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800")
                .build()) {
            
            IgniteSql sql = client.sql();
            
            // Statement resources are handled automatically
            try (ResultSet<SqlRow> result = sql.execute(null, "SELECT * FROM Artist")) {
                
                while (result.hasNext()) {
                    SqlRow row = result.next();
                    // Process row
                }
                
                // ResultSet is automatically closed here
            }
            
            // Client connection is automatically closed here
            
        } catch (Exception e) {
            System.err.println("Error with automatic resource cleanup: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates async error handling.
     */
    public void demonstrateAsyncErrorHandling(IgniteSql sql) {
        CompletableFuture<AsyncResultSet<SqlRow>> future = sql.executeAsync(null,
            "SELECT * FROM PotentiallyMissingTable");
        
        future.thenCompose(resultSet -> {
            // Process results
            System.out.println("Processing async results...");
            for (SqlRow row : resultSet.currentPage()) {
                // Process row
            }
            return resultSet.closeAsync();
            
        }).exceptionally(throwable -> {
            if (throwable instanceof SqlException) {
                SqlException sqlEx = (SqlException) throwable;
                System.err.println("Async SQL error: " + sqlEx.getMessage());
                System.err.println("Error code: " + sqlEx.code());
            } else {
                System.err.println("Async operation failed: " + throwable.getMessage());
            }
            return null;
        });
    }
}
```

## Performance Optimization and Best Practices

Understanding performance characteristics and optimization techniques is essential for production SQL API usage.

### Query Optimization Patterns

```java
/**
 * Demonstrates performance optimization techniques for SQL API usage.
 * These patterns are essential for high-performance applications.
 */
public class SQLPerformanceOptimization {
    
    /**
     * Demonstrates prepared statement reuse for better performance.
     */
    public void demonstratePreparedStatementReuse(IgniteSql sql) {
        // Create reusable prepared statement
        Statement artistLookup = sql.statementBuilder()
            .query("SELECT ArtistId, Name FROM Artist WHERE Country = ?")
            .build();
        
        // Reuse statement with different parameters
        String[] countries = {"UK", "USA", "Canada", "Germany", "France"};
        
        long startTime = System.currentTimeMillis();
        
        for (String country : countries) {
            ResultSet<SqlRow> result = sql.execute(null, artistLookup, country);
            
            System.out.printf("Artists from %s:%n", country);
            while (result.hasNext()) {
                SqlRow row = result.next();
                System.out.printf("  %d: %s%n", 
                    row.intValue("ArtistId"), 
                    row.stringValue("Name"));
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        System.out.printf("Prepared statement reuse completed in %d ms%n", duration);
    }
    
    /**
     * Demonstrates optimal pagination for large result sets.
     */
    public void demonstrateOptimalPagination(IgniteSql sql) {
        // Configure statement for optimal paging
        Statement pagedQuery = sql.statementBuilder()
            .query("SELECT TrackId, Name, Milliseconds FROM Track ORDER BY TrackId")
            .pageSize(1000)  // Optimize page size based on network and memory
            .build();
        
        long totalRows = 0;
        long startTime = System.currentTimeMillis();
        
        ResultSet<SqlRow> result = sql.execute(null, pagedQuery);
        
        while (result.hasNext()) {
            SqlRow row = result.next();
            totalRows++;
            
            // Process row efficiently
            int trackId = row.intValue("TrackId");
            String name = row.stringValue("Name");
            long duration = row.longValue("Milliseconds");
            
            // Avoid expensive operations in tight loops
            if (totalRows % 1000 == 0) {
                System.out.printf("Processed %d rows...%n", totalRows);
            }
        }
        
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.printf("Processed %d total rows in %d ms (%.2f rows/sec)%n", 
            totalRows, elapsed, (double) totalRows / elapsed * 1000);
    }
    
    /**
     * Demonstrates batch optimization for bulk operations.
     */
    public void demonstrateBatchOptimization(IgniteSql sql) {
        // Optimal batch size balances memory usage and network efficiency
        int optimalBatchSize = 1000;
        
        // Simulate large dataset
        List<String> artistNames = generateArtistNames(10000);
        
        long startTime = System.currentTimeMillis();
        int totalInserted = 0;
        
        // Process in optimal batches
        for (int i = 0; i < artistNames.size(); i += optimalBatchSize) {
            int endIndex = Math.min(i + optimalBatchSize, artistNames.size());
            List<String> batch = artistNames.subList(i, endIndex);
            
            // Create batch arguments
            BatchedArguments batchArgs = BatchedArguments.create();
            for (String name : batch) {
                batchArgs.add(name);
            }
            
            // Execute batch
            long[] results = sql.executeBatch(null,
                "INSERT INTO Artist (Name) VALUES (?)", batchArgs);
            
            totalInserted += results.length;
            System.out.printf("Batch %d: inserted %d artists%n", 
                (i / optimalBatchSize) + 1, results.length);
        }
        
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.printf("Batch optimization: %d artists in %d ms (%.2f/sec)%n",
            totalInserted, elapsed, (double) totalInserted / elapsed * 1000);
    }
    
    /**
     * Demonstrates async operations for improved throughput.
     */
    public void demonstrateAsyncThroughputOptimization(IgniteSql sql) {
        List<String> queries = Arrays.asList(
            "SELECT COUNT(*) as count FROM Artist",
            "SELECT COUNT(*) as count FROM Album", 
            "SELECT COUNT(*) as count FROM Track",
            "SELECT AVG(UnitPrice) as avg_price FROM Track",
            "SELECT MAX(Milliseconds) as max_duration FROM Track"
        );
        
        long startTime = System.currentTimeMillis();
        
        // Execute all queries in parallel
        List<CompletableFuture<AsyncResultSet<SqlRow>>> futures = queries.stream()
            .map(query -> sql.executeAsync(null, query))
            .collect(Collectors.toList());
        
        // Wait for all to complete and process results
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                System.out.println("Parallel query results:");
                
                for (int i = 0; i < futures.size(); i++) {
                    try {
                        AsyncResultSet<SqlRow> resultSet = futures.get(i).get();
                        if (!resultSet.currentPage().isEmpty()) {
                            SqlRow row = resultSet.currentPage().iterator().next();
                            System.out.printf("Query %d result: %s%n", i + 1, 
                                row.value(0)); // First column value
                        }
                        resultSet.closeAsync();
                        
                    } catch (Exception e) {
                        System.err.printf("Query %d failed: %s%n", i + 1, e.getMessage());
                    }
                }
                
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.printf("Parallel execution completed in %d ms%n", elapsed);
            });
    }
    
    private List<String> generateArtistNames(int count) {
        List<String> names = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            names.add("Generated Artist " + i);
        }
        return names;
    }
}
```

## Summary

The Ignite 3 SQL API provides a comprehensive, type-safe interface for relational database operations in distributed environments. Key takeaways:

### Core Patterns

1. **Interface Access**: Always obtain `IgniteSql` via `client.sql()`
2. **Parameter Binding**: Use `?` placeholders for secure, typed parameters  
3. **Result Processing**: Leverage `ResultSet<SqlRow>` with typed extraction methods
4. **Transaction Integration**: Pass `Transaction` objects for ACID compliance
5. **Resource Management**: Use try-with-resources for automatic cleanup

### Performance Considerations

- **Statement Reuse**: Create `Statement` objects for repeated queries
- **Batch Operations**: Use `BatchedArguments` for bulk data operations
- **Async Operations**: Leverage `CompletableFuture` for high throughput
- **Pagination**: Configure appropriate page sizes for large result sets
- **Object Mapping**: Use `Mapper<T>` for type-safe result conversion

### Advanced Features

- **Async Processing**: Non-blocking operations with `AsyncResultSet`
- **Object Mapping**: Automatic POJO conversion with custom field mapping
- **Batch Processing**: Efficient bulk operations with error handling
- **Metadata Access**: Runtime introspection of result structure
- **Transaction Support**: Full ACID compliance with manual transaction management

The SQL API integrates seamlessly with other Ignite 3 APIs while providing familiar SQL semantics optimized for distributed computing environments. Focus on understanding the Java interface patterns rather than SQL syntax details, as the API abstracts distributed complexity while maintaining type safety and performance.

## Reference Application

The complete reference application demonstrating all these patterns is available in the `ignite3-reference-apps/05-sql-api-app` module, with practical examples of each concept covered in this documentation.