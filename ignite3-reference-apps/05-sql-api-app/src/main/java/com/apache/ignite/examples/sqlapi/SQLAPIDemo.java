package com.apache.ignite.examples.sqlapi;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.sql.Statement;
import org.apache.ignite.sql.BatchedArguments;
import org.apache.ignite.table.mapper.Mapper;
import org.apache.ignite.tx.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * Demonstration of the Ignite 3 SQL API for Java developers.
 * 
 * This application demonstrates how to use the IgniteSql interface and related
 * classes
 * for relational database operations in distributed environments. It covers:
 * 
 * - Basic SQL interface access and query execution
 * - Statement management and configuration
 * - ResultSet processing and data extraction
 * - Transaction integration
 * - Batch operations for bulk data
 * - Error handling and resource management
 * - Performance optimization patterns
 * 
 * The focus is on teaching Java API usage patterns rather than SQL syntax,
 * showing how to work with the Ignite 3 distributed SQL engine.
 * 
 * Prerequisites:
 * - Ignite 3 cluster running on localhost:10800
 * - Sample music store data loaded (run sample-data-setup first)
 * 
 * @see org.apache.ignite.sql.IgniteSql
 * @see org.apache.ignite.sql.ResultSet
 * @see org.apache.ignite.sql.Statement
 */
public class SQLAPIDemo {

    private static final Logger logger = LoggerFactory.getLogger(SQLAPIDemo.class);

    public static void main(String[] args) {
        logger.info("Starting Ignite 3 SQL API Demo");

        // Connect to Ignite cluster with proper resource management
        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800")
                .build()) {

            SQLAPIDemo demo = new SQLAPIDemo();

            // Demonstrate core SQL API concepts
            demo.demonstrateBasicSQLAccess(client);
            demo.demonstrateStatementConfiguration(client);
            demo.demonstrateResultSetProcessing(client);
            demo.demonstrateTransactionIntegration(client);
            demo.demonstrateBatchOperations(client);
            demo.demonstrateObjectMapping(client);
            demo.demonstrateErrorHandling(client);
            demo.demonstratePerformancePatterns(client);

            logger.info("SQL API Demo completed successfully");

        } catch (Exception e) {
            logger.error("SQL API Demo failed", e);
            System.exit(1);
        }
    }

    /**
     * Demonstrates basic SQL API access and simple query execution.
     * Shows how to obtain the IgniteSql interface and perform basic operations.
     */
    private void demonstrateBasicSQLAccess(IgniteClient client) {
        logger.info("=== Basic SQL API Access ===");

        // Obtain the SQL interface - this is your gateway to all SQL operations
        IgniteSql sql = client.sql();

        // Simple query execution - returns ResultSet<SqlRow>
        ResultSet<SqlRow> artists = sql.execute(null, "SELECT ArtistId, Name FROM Artist LIMIT 5");

        logger.info("First 5 artists in the database:");
        while (artists.hasNext()) {
            SqlRow row = artists.next();
            int artistId = row.intValue("ArtistId");
            String name = row.stringValue("Name");
            logger.info("  Artist {}: {}", artistId, name);
        }

        // Demonstrate parameter binding for security and type safety
        String searchPattern = "%Rock%";
        ResultSet<SqlRow> rockArtists = sql.execute(null,
                "SELECT ArtistId, Name FROM Artist WHERE Name LIKE ? ORDER BY Name",
                searchPattern);

        logger.info("Artists with 'Rock' in their name:");
        while (rockArtists.hasNext()) {
            SqlRow row = rockArtists.next();
            logger.info("  {}: {}", row.intValue("ArtistId"), row.stringValue("Name"));
        }
    }

    /**
     * Demonstrates advanced statement configuration for optimized query execution.
     * Shows how to use StatementBuilder for better performance and control.
     */
    private void demonstrateStatementConfiguration(IgniteClient client) {
        logger.info("=== Statement Configuration and Reuse ===");

        IgniteSql sql = client.sql();

        // Create reusable statement with configuration
        Statement artistLookup = sql.statementBuilder()
                .query("SELECT ArtistId, Name FROM Artist WHERE Name = ?")
                .queryTimeout(30, TimeUnit.SECONDS) // Set timeout
                .pageSize(100) // Configure paging
                .build();

        // Reuse the same statement with different parameters for better performance
        String[] searchArtists = { "AC/DC", "Metallica", "Led Zeppelin" };

        for (String artistName : searchArtists) {
            ResultSet<SqlRow> result = sql.execute(null, artistLookup, artistName);

            if (result.hasNext()) {
                SqlRow row = result.next();
                logger.info("Found artist: {} (ID: {})",
                        row.stringValue("Name"), row.intValue("ArtistId"));
            } else {
                logger.info("Artist '{}' not found", artistName);
            }
        }

        // Demonstrate pagination configuration for large result sets
        Statement pagedQuery = sql.statementBuilder()
                .query("SELECT TrackId, Name FROM Track ORDER BY TrackId")
                .pageSize(50) // Smaller page size for demo
                .build();

        ResultSet<SqlRow> tracks = sql.execute(null, pagedQuery);

        int count = 0;
        logger.info("First 10 tracks (demonstrating pagination):");
        while (tracks.hasNext() && count < 10) {
            SqlRow track = tracks.next();
            logger.info("  Track {}: {}", track.intValue("TrackId"), track.stringValue("Name"));
            count++;
        }
    }

    /**
     * Demonstrates comprehensive ResultSet processing patterns and data type
     * handling.
     * Shows how to extract different data types and handle metadata.
     */
    private void demonstrateResultSetProcessing(IgniteClient client) {
        logger.info("=== ResultSet Processing and Data Types ===");

        IgniteSql sql = client.sql();

        // Query with multiple data types
        ResultSet<SqlRow> result = sql.execute(null,
                "SELECT TrackId, Name, Milliseconds, UnitPrice FROM Track WHERE TrackId <= 3");

        logger.info("Track details with various data types:");
        while (result.hasNext()) {
            SqlRow row = result.next();

            // Extract different data types safely
            int trackId = row.intValue("TrackId");
            String trackName = row.stringValue("Name");
            long duration = row.intValue("Milliseconds");
            BigDecimal price = row.decimalValue("UnitPrice");

            logger.info("  Track {}: '{}' - {}ms - ${}",
                    trackId, trackName, duration, price);
        }

        // Demonstrate JOIN query processing with hierarchical data
        String joinQuery = """
                SELECT a.Name as ArtistName, al.Title as AlbumTitle,
                       COUNT(t.TrackId) as TrackCount
                FROM Artist a
                JOIN Album al ON a.ArtistId = al.ArtistId
                JOIN Track t ON al.AlbumId = t.AlbumId
                GROUP BY a.ArtistId, a.Name, al.AlbumId, al.Title
                HAVING COUNT(t.TrackId) > 10
                ORDER BY TrackCount DESC
                LIMIT 5
                """;

        ResultSet<SqlRow> albumStats = sql.execute(null, joinQuery);

        logger.info("Albums with most tracks:");
        while (albumStats.hasNext()) {
            SqlRow row = albumStats.next();

            String artist = row.stringValue("ArtistName");
            String album = row.stringValue("AlbumTitle");
            long trackCount = row.longValue("TrackCount");

            logger.info("  {}: '{}' ({} tracks)", artist, album, trackCount);
        }

        // Demonstrate checking result type (rows vs affected count)
        ResultSet<SqlRow> updateResult = sql.execute(null,
                "UPDATE Artist SET Name = Name WHERE ArtistId = 999999"); // Non-existent ID

        if (updateResult.hasRowSet()) {
            logger.info("Update returned data rows");
        } else {
            long affectedRows = updateResult.affectedRows();
            logger.info("Update affected {} rows", affectedRows);
        }
    }

    /**
     * Demonstrates transaction integration with SQL operations.
     * Shows how SQL operations integrate with Ignite's ACID transaction system.
     */
    private void demonstrateTransactionIntegration(IgniteClient client) {
        logger.info("=== Transaction Integration ===");

        IgniteSql sql = client.sql();

        // Demonstrate successful transaction
        Transaction tx = client.transactions().begin();
        try {
            logger.info("Starting transaction for new artist and album");

            // Insert new artist
            sql.execute(tx, "INSERT INTO Artist (Name, Country) VALUES (?, ?)",
                    "Demo Artist", "Demo Country");

            // Find the artist ID (in production, use RETURNING clause or sequences)
            ResultSet<SqlRow> artistResult = sql.execute(tx,
                    "SELECT ArtistId FROM Artist WHERE Name = ?", "Demo Artist");

            if (artistResult.hasNext()) {
                int artistId = artistResult.next().intValue("ArtistId");
                logger.info("Created artist with ID: {}", artistId);

                // Insert album for the artist
                sql.execute(tx, "INSERT INTO Album (ArtistId, Title) VALUES (?, ?)",
                        artistId, "Demo Album");

                logger.info("Created album for artist ID: {}", artistId);
            }

            // Commit the transaction
            tx.commit();
            logger.info("Transaction committed successfully");

        } catch (Exception e) {
            logger.error("Transaction failed and was rolled back: {}", e.getMessage());
        }

        // Verify the transaction results
        ResultSet<SqlRow> verification = sql.execute(null,
                "SELECT COUNT(*) as aCount FROM Artist WHERE Name = ?", "Demo Artist");

        if (verification.hasNext()) {
            long count = verification.next().longValue("aCount");
            logger.info("Verification: {} demo artists in database", count);
        }

        // Clean up demo data
        tx = client.transactions().begin();
        try {
            sql.execute(tx, "DELETE FROM Album WHERE Title = ?", "Demo Album");
            sql.execute(tx, "DELETE FROM Artist WHERE Name = ?", "Demo Artist");
            tx.commit();
            logger.info("Demo data cleaned up");
        } catch (Exception e) {
            tx.rollback();
            logger.error("Failed to clean up demo data, transaction rolled back", e);
        }
    }

    /**
     * Demonstrates batch operations for efficient bulk data processing.
     * Shows how to use BatchedArguments for high-performance bulk operations.
     */
    private void demonstrateBatchOperations(IgniteClient client) {
        logger.info("=== Batch Operations ===");

        IgniteSql sql = client.sql();

        // Create batch arguments for multiple inserts
        BatchedArguments artistBatch = BatchedArguments.create()
                .add("Batch Artist 1", "Country A")
                .add("Batch Artist 2", "Country B")
                .add("Batch Artist 3", "Country C")
                .add("Batch Artist 4", "Country A")
                .add("Batch Artist 5", "Country D");

        try {
            // Execute batch insert
            long[] insertCounts = sql.executeBatch(null,
                    "INSERT INTO Artist (Name, Country) VALUES (?, ?)",
                    artistBatch);

            logger.info("Batch insert completed: {} artists inserted", insertCounts.length);
            for (int i = 0; i < insertCounts.length; i++) {
                logger.info("  Insert {}: {} rows affected", i + 1, insertCounts[i]);
            }

            // Verify batch results
            ResultSet<SqlRow> batchResult = sql.execute(null,
                    "SELECT COUNT(*) as aCount FROM Artist WHERE Name LIKE 'Batch Artist%'");

            if (batchResult.hasNext()) {
                long count = batchResult.next().longValue("aCount");
                logger.info("Verification: {} batch artists in database", count);
            }

        } catch (Exception e) {
            logger.error("Batch operation failed: {}", e.getMessage());
        } finally {
            // Clean up batch test data
            sql.execute(null, "DELETE FROM Artist WHERE Name LIKE 'Batch Artist%'");
            logger.info("Batch test data cleaned up");
        }
    }

    /**
     * Demonstrates object mapping for type-safe result processing.
     * Shows how to use Mapper interface for automatic POJO conversion.
     */
    private void demonstrateObjectMapping(IgniteClient client) {
        logger.info("=== Object Mapping ===");

        IgniteSql sql = client.sql();

        // Simple single-column mapping for artist names
        ResultSet<String> artistNames = sql.execute(null,
                Mapper.of(String.class, "Name"),
                "SELECT Name FROM Artist WHERE ArtistId <= 5 ORDER BY Name");

        logger.info("Artist names using single-column mapping:");
        artistNames.forEachRemaining(name -> logger.info("  {}", name));

        // Numeric aggregation mapping
        ResultSet<Long> trackCounts = sql.execute(null,
                Mapper.of(Long.class, "track_count"),
                "SELECT COUNT(*) as track_count FROM Track");

        if (trackCounts.hasNext()) {
            Long totalTracks = trackCounts.next();
            logger.info("Total tracks in database: {}", totalTracks);
        }

        // For more complex POJO mapping, you would create custom classes with @Column
        // annotations
        // and use Mapper.of(YourClass.class) - see the documentation for detailed
        // examples
    }

    /**
     * Demonstrates comprehensive error handling for SQL operations.
     * Shows how to handle different types of SQL exceptions and implement retry
     * patterns.
     */
    private void demonstrateErrorHandling(IgniteClient client) {
        logger.info("=== Error Handling ===");

        IgniteSql sql = client.sql();

        // Demonstrate handling non-existent table error
        try {
            ResultSet<SqlRow> result = sql.execute(null, "SELECT * FROM NonExistentTable");
            logger.info("Query succeeded unexpectedly");
        } catch (Exception e) {
            logger.warn("Expected error accessing non-existent table: {}", e.getMessage());
        }

        // Demonstrate constraint violation handling
        try {
            // Try to insert artist with duplicate name (assuming unique constraint)
            sql.execute(null, "INSERT INTO Artist (ArtistId, Name) VALUES (?, ?)",
                    1, "Duplicate Test"); // ArtistId 1 likely exists
        } catch (Exception e) {
            logger.warn("Expected constraint violation: {}", e.getMessage());
        }

        // Demonstrate retry pattern for transient failures
        boolean success = executeWithRetry(sql,
                "SELECT COUNT(*) as aCount FROM Artist", 3);

        logger.info("Retry pattern demonstration: {}", success ? "SUCCESS" : "FAILED");
    }

    /**
     * Helper method demonstrating retry pattern for transient failures.
     */
    private boolean executeWithRetry(IgniteSql sql, String query, int maxRetries) {
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                ResultSet<SqlRow> result = sql.execute(null, query);
                if (result.hasNext()) {
                    long count = result.next().longValue("count");
                    logger.info("Retry attempt {}: Query succeeded, count = {}",
                            retryCount + 1, count);
                    return true;
                }
            } catch (Exception e) {
                retryCount++;
                logger.warn("Retry attempt {} failed: {}", retryCount, e.getMessage());

                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(1000 * retryCount); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Demonstrates performance optimization patterns for SQL API usage.
     * Shows statement reuse, optimal pagination, and efficient query patterns.
     */
    private void demonstratePerformancePatterns(IgniteClient client) {
        logger.info("=== Performance Optimization Patterns ===");

        IgniteSql sql = client.sql();

        // Demonstrate prepared statement reuse for better performance
        Statement countryLookup = sql.statementBuilder()
                .query("SELECT COUNT(*) as aCount FROM Artist WHERE Country = ?")
                .build();

        String[] countries = { "UK", "USA", "Canada" };
        long startTime = System.currentTimeMillis();

        for (String country : countries) {
            ResultSet<SqlRow> result = sql.execute(null, countryLookup, country);

            if (result.hasNext()) {
                long count = result.next().longValue("count");
                logger.info("Artists from {}: {}", country, count);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Statement reuse pattern completed in {} ms", duration);

        // Demonstrate efficient pagination
        Statement pagedQuery = sql.statementBuilder()
                .query("SELECT TrackId, Name FROM Track ORDER BY TrackId")
                .pageSize(100) // Optimal page size for performance
                .build();

        ResultSet<SqlRow> tracks = sql.execute(null, pagedQuery);

        int processedRows = 0;
        startTime = System.currentTimeMillis();

        while (tracks.hasNext() && processedRows < 500) { // Process first 500 rows
            SqlRow track = tracks.next();
            processedRows++;

            // Simulate minimal processing
            int trackId = track.intValue("TrackId");
            String name = track.stringValue("Name");
        }

        duration = System.currentTimeMillis() - startTime;
        logger.info("Processed {} rows in {} ms ({} rows/sec)",
                processedRows, duration,
                duration > 0 ? (processedRows * 1000.0 / duration) : 0);
    }
}