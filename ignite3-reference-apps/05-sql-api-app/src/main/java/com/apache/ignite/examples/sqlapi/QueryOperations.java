package com.apache.ignite.examples.sqlapi;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.sql.Statement;
import org.apache.ignite.sql.ColumnMetadata;
import org.apache.ignite.sql.async.AsyncResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates complex query operations using the Ignite 3 SQL API.
 * 
 * This class focuses on advanced query patterns including:
 * - Complex JOIN operations across multiple tables
 * - Aggregate functions with GROUP BY and HAVING
 * - Subqueries and correlated subqueries
 * - Window functions and analytical queries
 * - Performance-optimized query patterns
 * - Asynchronous query execution
 * - Result set pagination and streaming
 * - Query metadata introspection
 * 
 * The emphasis is on demonstrating how to use the Java SQL API for complex
 * analytical and reporting queries in distributed environments, showcasing
 * patterns that work efficiently with Ignite's distributed query engine.
 * 
 * Prerequisites:
 * - Ignite 3 cluster running on localhost:10800
 * - Sample music store data loaded (run sample-data-setup first)
 * 
 * @see org.apache.ignite.sql.IgniteSql#execute
 * @see org.apache.ignite.sql.Statement
 * @see org.apache.ignite.sql.async.AsyncResultSet
 */
public class QueryOperations {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryOperations.class);
    
    public static void main(String[] args) {
        logger.info("Starting Query Operations Demo");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800")
                .build()) {
            
            QueryOperations demo = new QueryOperations();
            
            // Demonstrate various query patterns
            demo.demonstrateSimpleQueries(client);
            demo.demonstrateJoinOperations(client);
            demo.demonstrateAggregateQueries(client);
            demo.demonstrateSubqueries(client);
            demo.demonstrateAnalyticalQueries(client);
            demo.demonstrateAsyncQueries(client);
            demo.demonstratePaginatedQueries(client);
            demo.demonstrateMetadataIntrospection(client);
            demo.demonstratePerformancePatterns(client);
            
            logger.info("Query Operations Demo completed successfully");
            
        } catch (Exception e) {
            logger.error("Query Operations Demo failed", e);
            System.exit(1);
        }
    }
    
    /**
     * Demonstrates basic query patterns and filtering operations.
     * Shows fundamental SQL API query usage patterns.
     */
    private void demonstrateSimpleQueries(IgniteClient client) {
        logger.info("=== Simple Query Operations ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // Basic SELECT with filtering
            logger.info("Basic artist query with filtering:");
            ResultSet<SqlRow> artists = sql.execute(null,
                "SELECT ArtistId, Name FROM Artist WHERE Name LIKE ? ORDER BY Name LIMIT 5",
                "%A%");
            
            while (artists.hasNext()) {
                SqlRow row = artists.next();
                logger.info("  Artist {}: {}", row.intValue("ArtistId"), row.stringValue("Name"));
            }
            
            // Query with multiple conditions
            logger.info("Tracks with price and duration filters:");
            ResultSet<SqlRow> tracks = sql.execute(null,
                "SELECT Name, UnitPrice, Milliseconds " +
                "FROM Track " +
                "WHERE UnitPrice >= ? AND Milliseconds > ? " +
                "ORDER BY UnitPrice DESC, Milliseconds DESC " +
                "LIMIT 5",
                new BigDecimal("1.00"), 300000);
            
            while (tracks.hasNext()) {
                SqlRow row = tracks.next();
                logger.info("  Track: '{}' - ${} ({} ms)", 
                    row.stringValue("Name"),
                    row.decimalValue("UnitPrice"),
                    row.longValue("Milliseconds"));
            }
            
            // LIKE pattern matching with wildcards
            logger.info("Albums with 'Greatest' in title:");
            ResultSet<SqlRow> albums = sql.execute(null,
                "SELECT Title FROM Album WHERE Title LIKE ? ORDER BY Title",
                "%Greatest%");
            
            while (albums.hasNext()) {
                SqlRow row = albums.next();
                logger.info("  Album: '{}'", row.stringValue("Title"));
            }
            
            // IN clause with multiple values
            logger.info("Tracks in specific genres:");
            ResultSet<SqlRow> genreTracks = sql.execute(null,
                "SELECT t.Name, g.Name as GenreName " +
                "FROM Track t " +
                "LEFT JOIN Genre g ON t.GenreId = g.GenreId " +
                "WHERE t.GenreId IN (?, ?, ?) " +
                "ORDER BY g.Name, t.Name " +
                "LIMIT 10",
                1, 2, 3); // Rock, Jazz, Metal genre IDs
            
            while (genreTracks.hasNext()) {
                SqlRow row = genreTracks.next();
                logger.info("  Track: '{}' (Genre: {})", 
                    row.stringValue("Name"),
                    row.stringValue("GenreName"));
            }
            
        } catch (Exception e) {
            logger.error("Failed to execute simple queries: {}", e.getMessage());
        }
    }
    
    /**
     * Demonstrates JOIN operations across multiple tables.
     * Shows how to efficiently query related data in distributed tables.
     */
    private void demonstrateJoinOperations(IgniteClient client) {
        logger.info("=== JOIN Operations ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // INNER JOIN across artist, album, and track
            logger.info("Artist-Album-Track hierarchy (INNER JOIN):");
            ResultSet<SqlRow> hierarchy = sql.execute(null,
                "SELECT a.Name as ArtistName, al.Title as AlbumTitle, t.Name as TrackName " +
                "FROM Artist a " +
                "INNER JOIN Album al ON a.ArtistId = al.ArtistId " +
                "INNER JOIN Track t ON al.AlbumId = t.AlbumId " +
                "WHERE a.ArtistId <= ? " +
                "ORDER BY a.Name, al.Title, t.TrackId " +
                "LIMIT 10", 3);
            
            while (hierarchy.hasNext()) {
                SqlRow row = hierarchy.next();
                logger.info("  {} -> '{}' -> '{}'", 
                    row.stringValue("ArtistName"),
                    row.stringValue("AlbumTitle"),
                    row.stringValue("TrackName"));
            }
            
            // LEFT JOIN to find artists without albums
            logger.info("Artists without albums (LEFT JOIN):");
            ResultSet<SqlRow> artistsWithoutAlbums = sql.execute(null,
                "SELECT a.ArtistId, a.Name " +
                "FROM Artist a " +
                "LEFT JOIN Album al ON a.ArtistId = al.ArtistId " +
                "WHERE al.ArtistId IS NULL " +
                "ORDER BY a.Name " +
                "LIMIT 5");
            
            while (artistsWithoutAlbums.hasNext()) {
                SqlRow row = artistsWithoutAlbums.next();
                logger.info("  Artist without albums: {} (ID: {})", 
                    row.stringValue("Name"),
                    row.intValue("ArtistId"));
            }
            
            // Complex JOIN with multiple conditions
            logger.info("Tracks with artist and genre information:");
            ResultSet<SqlRow> trackDetails = sql.execute(null,
                "SELECT t.Name as TrackName, " +
                "       a.Name as ArtistName, " +
                "       al.Title as AlbumTitle, " +
                "       g.Name as GenreName, " +
                "       t.UnitPrice " +
                "FROM Track t " +
                "INNER JOIN Album al ON t.AlbumId = al.AlbumId " +
                "INNER JOIN Artist a ON al.ArtistId = a.ArtistId " +
                "LEFT JOIN Genre g ON t.GenreId = g.GenreId " +
                "WHERE t.UnitPrice > ? " +
                "ORDER BY t.UnitPrice DESC, a.Name " +
                "LIMIT 8",
                new BigDecimal("1.50"));
            
            while (trackDetails.hasNext()) {
                SqlRow row = trackDetails.next();
                logger.info("  '{}' by {} from '{}' ({}): ${}", 
                    row.stringValue("TrackName"),
                    row.stringValue("ArtistName"),
                    row.stringValue("AlbumTitle"),
                    row.stringValue("GenreName"),
                    row.decimalValue("UnitPrice"));
            }
            
            // Self-join example (if employee hierarchy exists)
            logger.info("Employee management hierarchy (Self-JOIN):");
            ResultSet<SqlRow> employeeHierarchy = sql.execute(null,
                "SELECT e.FirstName + ' ' + e.LastName as EmployeeName, " +
                "       m.FirstName + ' ' + m.LastName as ManagerName " +
                "FROM Employee e " +
                "LEFT JOIN Employee m ON e.ReportsTo = m.EmployeeId " +
                "ORDER BY m.LastName, e.LastName " +
                "LIMIT 5");
            
            while (employeeHierarchy.hasNext()) {
                SqlRow row = employeeHierarchy.next();
                String managerName = row.stringValue("ManagerName");
                logger.info("  Employee: {} reports to {}", 
                    row.stringValue("EmployeeName"),
                    managerName != null ? managerName : "No manager");
            }
            
        } catch (Exception e) {
            logger.error("Failed to execute JOIN operations: {}", e.getMessage());
        }
    }
    
    /**
     * Demonstrates aggregate functions and GROUP BY operations.
     * Shows how to perform analytical queries with aggregations.
     */
    private void demonstrateAggregateQueries(IgniteClient client) {
        logger.info("=== Aggregate Query Operations ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // Basic aggregations
            logger.info("Database summary statistics:");
            ResultSet<SqlRow> summary = sql.execute(null,
                "SELECT " +
                "    (SELECT COUNT(*) FROM Artist) as ArtistCount, " +
                "    (SELECT COUNT(*) FROM Album) as AlbumCount, " +
                "    (SELECT COUNT(*) FROM Track) as TrackCount, " +
                "    (SELECT AVG(UnitPrice) FROM Track) as AvgTrackPrice, " +
                "    (SELECT SUM(UnitPrice) FROM Track) as TotalValue");
            
            if (summary.hasNext()) {
                SqlRow row = summary.next();
                logger.info("  Artists: {}, Albums: {}, Tracks: {}", 
                    row.longValue("ArtistCount"),
                    row.longValue("AlbumCount"),
                    row.longValue("TrackCount"));
                logger.info("  Avg Track Price: ${}, Total Value: ${}", 
                    row.decimalValue("AvgTrackPrice"),
                    row.decimalValue("TotalValue"));
            }
            
            // GROUP BY with multiple aggregations
            logger.info("Track statistics by genre:");
            ResultSet<SqlRow> genreStats = sql.execute(null,
                "SELECT g.Name as GenreName, " +
                "       COUNT(t.TrackId) as TrackCount, " +
                "       AVG(t.Milliseconds) as AvgDuration, " +
                "       AVG(t.UnitPrice) as AvgPrice, " +
                "       MIN(t.UnitPrice) as MinPrice, " +
                "       MAX(t.UnitPrice) as MaxPrice " +
                "FROM Track t " +
                "LEFT JOIN Genre g ON t.GenreId = g.GenreId " +
                "GROUP BY g.GenreId, g.Name " +
                "HAVING COUNT(t.TrackId) > ? " +
                "ORDER BY TrackCount DESC " +
                "LIMIT 10", 50);
            
            while (genreStats.hasNext()) {
                SqlRow row = genreStats.next();
                logger.info("  Genre '{}': {} tracks, avg {}ms, avg price ${} (${}-${})", 
                    row.stringValue("GenreName"),
                    row.longValue("TrackCount"),
                    row.longValue("AvgDuration"),
                    row.decimalValue("AvgPrice"),
                    row.decimalValue("MinPrice"),
                    row.decimalValue("MaxPrice"));
            }
            
            // Artist productivity analysis
            logger.info("Most productive artists:");
            ResultSet<SqlRow> artistStats = sql.execute(null,
                "SELECT a.Name as ArtistName, " +
                "       COUNT(DISTINCT al.AlbumId) as AlbumCount, " +
                "       COUNT(t.TrackId) as TrackCount, " +
                "       SUM(t.Milliseconds) as TotalDuration, " +
                "       SUM(t.UnitPrice) as TotalValue " +
                "FROM Artist a " +
                "LEFT JOIN Album al ON a.ArtistId = al.ArtistId " +
                "LEFT JOIN Track t ON al.AlbumId = t.AlbumId " +
                "GROUP BY a.ArtistId, a.Name " +
                "HAVING COUNT(t.TrackId) > ? " +
                "ORDER BY TotalDuration DESC " +
                "LIMIT 8", 20);
            
            while (artistStats.hasNext()) {
                SqlRow row = artistStats.next();
                long durationMinutes = row.longValue("TotalDuration") / 60000;
                logger.info("  Artist '{}': {} albums, {} tracks, {} minutes, ${} total value", 
                    row.stringValue("ArtistName"),
                    row.longValue("AlbumCount"),
                    row.longValue("TrackCount"),
                    durationMinutes,
                    row.decimalValue("TotalValue"));
            }
            
            // Complex aggregation with CASE
            logger.info("Price distribution analysis:");
            ResultSet<SqlRow> priceDistribution = sql.execute(null,
                "SELECT " +
                "    COUNT(CASE WHEN UnitPrice < 1.0 THEN 1 END) as Cheap, " +
                "    COUNT(CASE WHEN UnitPrice >= 1.0 AND UnitPrice < 2.0 THEN 1 END) as Medium, " +
                "    COUNT(CASE WHEN UnitPrice >= 2.0 THEN 1 END) as Expensive, " +
                "    COUNT(*) as Total " +
                "FROM Track");
            
            if (priceDistribution.hasNext()) {
                SqlRow row = priceDistribution.next();
                logger.info("  Price distribution - Cheap: {}, Medium: {}, Expensive: {}, Total: {}", 
                    row.longValue("Cheap"),
                    row.longValue("Medium"),
                    row.longValue("Expensive"),
                    row.longValue("Total"));
            }
            
        } catch (Exception e) {
            logger.error("Failed to execute aggregate queries: {}", e.getMessage());
        }
    }
    
    /**
     * Demonstrates subqueries and correlated subqueries.
     * Shows advanced query patterns for complex business logic.
     */
    private void demonstrateSubqueries(IgniteClient client) {
        logger.info("=== Subquery Operations ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // Simple subquery in WHERE clause
            logger.info("Artists with above-average track counts:");
            ResultSet<SqlRow> aboveAverage = sql.execute(null,
                "SELECT a.Name as ArtistName, " +
                "       (SELECT COUNT(*) FROM Album al " +
                "        JOIN Track t ON al.AlbumId = t.AlbumId " +
                "        WHERE al.ArtistId = a.ArtistId) as TrackCount " +
                "FROM Artist a " +
                "WHERE (SELECT COUNT(*) FROM Album al " +
                "       JOIN Track t ON al.AlbumId = t.AlbumId " +
                "       WHERE al.ArtistId = a.ArtistId) > " +
                "      (SELECT AVG(track_count) FROM (" +
                "           SELECT COUNT(*) as track_count " +
                "           FROM Album al2 " +
                "           JOIN Track t2 ON al2.AlbumId = t2.AlbumId " +
                "           GROUP BY al2.ArtistId" +
                "       ) avg_calc) " +
                "ORDER BY TrackCount DESC " +
                "LIMIT 5");
            
            while (aboveAverage.hasNext()) {
                SqlRow row = aboveAverage.next();
                logger.info("  Artist '{}': {} tracks", 
                    row.stringValue("ArtistName"),
                    row.longValue("TrackCount"));
            }
            
            // EXISTS subquery
            logger.info("Artists with expensive tracks (using EXISTS):");
            ResultSet<SqlRow> expensiveArtists = sql.execute(null,
                "SELECT a.Name as ArtistName " +
                "FROM Artist a " +
                "WHERE EXISTS (" +
                "    SELECT 1 FROM Album al " +
                "    JOIN Track t ON al.AlbumId = t.AlbumId " +
                "    WHERE al.ArtistId = a.ArtistId " +
                "    AND t.UnitPrice > ?" +
                ") " +
                "ORDER BY a.Name " +
                "LIMIT 8",
                new BigDecimal("1.50"));
            
            while (expensiveArtists.hasNext()) {
                SqlRow row = expensiveArtists.next();
                logger.info("  Artist with expensive tracks: '{}'", 
                    row.stringValue("ArtistName"));
            }
            
            // NOT EXISTS subquery
            logger.info("Genres with no tracks (using NOT EXISTS):");
            ResultSet<SqlRow> unusedGenres = sql.execute(null,
                "SELECT g.Name as GenreName " +
                "FROM Genre g " +
                "WHERE NOT EXISTS (" +
                "    SELECT 1 FROM Track t WHERE t.GenreId = g.GenreId" +
                ") " +
                "ORDER BY g.Name");
            
            while (unusedGenres.hasNext()) {
                SqlRow row = unusedGenres.next();
                logger.info("  Unused genre: '{}'", row.stringValue("GenreName"));
            }
            
            // Subquery in SELECT clause (correlated)
            logger.info("Albums with track count and total duration:");
            ResultSet<SqlRow> albumDetails = sql.execute(null,
                "SELECT al.Title as AlbumTitle, " +
                "       a.Name as ArtistName, " +
                "       (SELECT COUNT(*) FROM Track t WHERE t.AlbumId = al.AlbumId) as TrackCount, " +
                "       (SELECT SUM(t.Milliseconds) FROM Track t WHERE t.AlbumId = al.AlbumId) as TotalDuration " +
                "FROM Album al " +
                "JOIN Artist a ON al.ArtistId = a.ArtistId " +
                "WHERE (SELECT COUNT(*) FROM Track t WHERE t.AlbumId = al.AlbumId) > ? " +
                "ORDER BY TrackCount DESC " +
                "LIMIT 8", 15);
            
            while (albumDetails.hasNext()) {
                SqlRow row = albumDetails.next();
                Long duration = row.longValue("TotalDuration");
                long durationMinutes = duration != null ? duration / 60000 : 0;
                logger.info("  Album '{}' by {}: {} tracks, {} minutes", 
                    row.stringValue("AlbumTitle"),
                    row.stringValue("ArtistName"),
                    row.longValue("TrackCount"),
                    durationMinutes);
            }
            
            // IN subquery with complex conditions
            logger.info("Tracks from top-selling genres:");
            ResultSet<SqlRow> topGenreTracks = sql.execute(null,
                "SELECT t.Name as TrackName, g.Name as GenreName " +
                "FROM Track t " +
                "LEFT JOIN Genre g ON t.GenreId = g.GenreId " +
                "WHERE t.GenreId IN (" +
                "    SELECT t2.GenreId " +
                "    FROM Track t2 " +
                "    GROUP BY t2.GenreId " +
                "    HAVING COUNT(*) > ?" +
                ") " +
                "ORDER BY g.Name, t.Name " +
                "LIMIT 10", 100);
            
            while (topGenreTracks.hasNext()) {
                SqlRow row = topGenreTracks.next();
                logger.info("  Track '{}' in popular genre '{}'", 
                    row.stringValue("TrackName"),
                    row.stringValue("GenreName"));
            }
            
        } catch (Exception e) {
            logger.error("Failed to execute subqueries: {}", e.getMessage());
        }
    }
    
    /**
     * Demonstrates analytical queries and window functions.
     * Shows advanced analytical patterns for business intelligence.
     */
    private void demonstrateAnalyticalQueries(IgniteClient client) {
        logger.info("=== Analytical Query Operations ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // Ranking and top-N queries
            logger.info("Top 5 longest tracks per genre:");
            // Note: Window functions may not be fully supported in all Ignite versions
            // Using alternative approach with subqueries
            ResultSet<SqlRow> topTracks = sql.execute(null,
                "SELECT t.Name as TrackName, " +
                "       g.Name as GenreName, " +
                "       t.Milliseconds, " +
                "       (SELECT COUNT(*) + 1 FROM Track t2 " +
                "        WHERE t2.GenreId = t.GenreId " +
                "        AND t2.Milliseconds > t.Milliseconds) as Rank " +
                "FROM Track t " +
                "LEFT JOIN Genre g ON t.GenreId = g.GenreId " +
                "WHERE (SELECT COUNT(*) FROM Track t2 " +
                "       WHERE t2.GenreId = t.GenreId " +
                "       AND t2.Milliseconds > t.Milliseconds) < 3 " +
                "AND t.GenreId IS NOT NULL " +
                "ORDER BY g.Name, t.Milliseconds DESC " +
                "LIMIT 15");
            
            String currentGenre = "";
            while (topTracks.hasNext()) {
                SqlRow row = topTracks.next();
                String genre = row.stringValue("GenreName");
                
                if (!genre.equals(currentGenre)) {
                    logger.info("  Genre: {}", genre);
                    currentGenre = genre;
                }
                
                long minutes = row.longValue("Milliseconds") / 60000;
                logger.info("    #{}: '{}' - {} minutes", 
                    row.longValue("Rank"),
                    row.stringValue("TrackName"),
                    minutes);
            }
            
            // Percentage calculations
            logger.info("Genre market share by track count:");
            ResultSet<SqlRow> marketShare = sql.execute(null,
                "SELECT g.Name as GenreName, " +
                "       COUNT(t.TrackId) as TrackCount, " +
                "       ROUND(COUNT(t.TrackId) * 100.0 / " +
                "           (SELECT COUNT(*) FROM Track), 2) as MarketShare " +
                "FROM Genre g " +
                "LEFT JOIN Track t ON g.GenreId = t.GenreId " +
                "GROUP BY g.GenreId, g.Name " +
                "HAVING COUNT(t.TrackId) > 0 " +
                "ORDER BY TrackCount DESC " +
                "LIMIT 10");
            
            while (marketShare.hasNext()) {
                SqlRow row = marketShare.next();
                logger.info("  Genre '{}': {} tracks ({}% market share)", 
                    row.stringValue("GenreName"),
                    row.longValue("TrackCount"),
                    row.decimalValue("MarketShare"));
            }
            
            // Running totals and cumulative calculations
            logger.info("Cumulative album count by artist (top 5):");
            ResultSet<SqlRow> cumulativeData = sql.execute(null,
                "SELECT a.Name as ArtistName, " +
                "       COUNT(al.AlbumId) as AlbumCount, " +
                "       (SELECT SUM(al2_count) FROM (" +
                "           SELECT COUNT(al2.AlbumId) as al2_count " +
                "           FROM Artist a2 " +
                "           LEFT JOIN Album al2 ON a2.ArtistId = al2.ArtistId " +
                "           WHERE a2.Name <= a.Name " +
                "           GROUP BY a2.ArtistId" +
                "       ) cumulative) as CumulativeTotal " +
                "FROM Artist a " +
                "LEFT JOIN Album al ON a.ArtistId = al.ArtistId " +
                "GROUP BY a.ArtistId, a.Name " +
                "HAVING COUNT(al.AlbumId) > 0 " +
                "ORDER BY a.Name " +
                "LIMIT 5");
            
            while (cumulativeData.hasNext()) {
                SqlRow row = cumulativeData.next();
                logger.info("  Artist '{}': {} albums (cumulative: {})", 
                    row.stringValue("ArtistName"),
                    row.longValue("AlbumCount"),
                    row.longValue("CumulativeTotal"));
            }
            
            // Complex analytical query with multiple dimensions
            logger.info("Revenue analysis by artist and genre:");
            ResultSet<SqlRow> revenueAnalysis = sql.execute(null,
                "SELECT a.Name as ArtistName, " +
                "       g.Name as GenreName, " +
                "       COUNT(t.TrackId) as TrackCount, " +
                "       SUM(t.UnitPrice) as TotalRevenue, " +
                "       AVG(t.UnitPrice) as AvgPrice " +
                "FROM Artist a " +
                "JOIN Album al ON a.ArtistId = al.ArtistId " +
                "JOIN Track t ON al.AlbumId = t.AlbumId " +
                "LEFT JOIN Genre g ON t.GenreId = g.GenreId " +
                "GROUP BY a.ArtistId, a.Name, g.GenreId, g.Name " +
                "HAVING SUM(t.UnitPrice) > ? " +
                "ORDER BY TotalRevenue DESC " +
                "LIMIT 8",
                new BigDecimal("20.00"));
            
            while (revenueAnalysis.hasNext()) {
                SqlRow row = revenueAnalysis.next();
                logger.info("  Artist '{}' in genre '{}': {} tracks, ${} revenue, ${} avg", 
                    row.stringValue("ArtistName"),
                    row.stringValue("GenreName"),
                    row.longValue("TrackCount"),
                    row.decimalValue("TotalRevenue"),
                    row.decimalValue("AvgPrice"));
            }
            
        } catch (Exception e) {
            logger.error("Failed to execute analytical queries: {}", e.getMessage());
        }
    }
    
    /**
     * Demonstrates asynchronous query execution for improved performance.
     * Shows how to use CompletableFuture for non-blocking query operations.
     */
    private void demonstrateAsyncQueries(IgniteClient client) {
        logger.info("=== Asynchronous Query Operations ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // Simple async query
            logger.info("Executing async query for artist count...");
            
            CompletableFuture<AsyncResultSet<SqlRow>> asyncFuture = sql.executeAsync(null,
                "SELECT COUNT(*) as count FROM Artist");
            
            asyncFuture.thenAccept(resultSet -> {
                logger.info("Async query completed");
                for (SqlRow row : resultSet.currentPage()) {
                    logger.info("  Total artists: {}", row.longValue("count"));
                }
                resultSet.closeAsync();
            }).exceptionally(throwable -> {
                logger.error("Async query failed: {}", throwable.getMessage());
                return null;
            });
            
            // Wait for async operation to complete
            Thread.sleep(1000);
            
            // Multiple parallel async queries
            logger.info("Executing multiple parallel queries...");
            
            CompletableFuture<AsyncResultSet<SqlRow>> artistCount = sql.executeAsync(null,
                "SELECT COUNT(*) as count FROM Artist");
            
            CompletableFuture<AsyncResultSet<SqlRow>> albumCount = sql.executeAsync(null,
                "SELECT COUNT(*) as count FROM Album");
            
            CompletableFuture<AsyncResultSet<SqlRow>> trackCount = sql.executeAsync(null,
                "SELECT COUNT(*) as count FROM Track");
            
            // Combine all results
            CompletableFuture.allOf(artistCount, albumCount, trackCount)
                .thenRun(() -> {
                    try {
                        AsyncResultSet<SqlRow> artists = artistCount.get();
                        AsyncResultSet<SqlRow> albums = albumCount.get();
                        AsyncResultSet<SqlRow> tracks = trackCount.get();
                        
                        long artistTotal = artists.currentPage().iterator().next().longValue("count");
                        long albumTotal = albums.currentPage().iterator().next().longValue("count");
                        long trackTotal = tracks.currentPage().iterator().next().longValue("count");
                        
                        logger.info("Parallel query results - Artists: {}, Albums: {}, Tracks: {}", 
                            artistTotal, albumTotal, trackTotal);
                        
                        // Clean up resources
                        artists.closeAsync();
                        albums.closeAsync();
                        tracks.closeAsync();
                        
                    } catch (Exception e) {
                        logger.error("Error processing parallel results: {}", e.getMessage());
                    }
                })
                .get(5, TimeUnit.SECONDS); // Wait up to 5 seconds
            
            logger.info("Parallel async queries completed");
            
        } catch (Exception e) {
            logger.error("Failed to execute async queries: {}", e.getMessage());
        }
    }
    
    /**
     * Demonstrates paginated queries for handling large result sets.
     * Shows how to efficiently process large amounts of data.
     */
    private void demonstratePaginatedQueries(IgniteClient client) {
        logger.info("=== Paginated Query Operations ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // Configure statement for pagination
            Statement pagedQuery = sql.statementBuilder()
                .query("SELECT TrackId, Name, UnitPrice FROM Track ORDER BY TrackId")
                .pageSize(50)  // Process 50 rows at a time
                .build();
            
            logger.info("Processing tracks with pagination (page size: 50):");
            
            ResultSet<SqlRow> tracks = sql.execute(null, pagedQuery);
            
            int totalProcessed = 0;
            int pageCount = 0;
            
            while (tracks.hasNext() && totalProcessed < 200) { // Limit to first 200 for demo
                int pageSize = 0;
                
                while (tracks.hasNext() && pageSize < 50 && totalProcessed < 200) {
                    SqlRow track = tracks.next();
                    
                    // Process each track (minimal logging to avoid spam)
                    if (totalProcessed % 25 == 0) { // Log every 25th track
                        logger.info("  Track {}: '{}' - ${}", 
                            track.intValue("TrackId"),
                            track.stringValue("Name"),
                            track.decimalValue("UnitPrice"));
                    }
                    
                    pageSize++;
                    totalProcessed++;
                }
                
                pageCount++;
                logger.info("  Processed page {} with {} tracks (total: {})", 
                    pageCount, pageSize, totalProcessed);
            }
            
            logger.info("Pagination demo completed: {} pages, {} total tracks processed", 
                pageCount, totalProcessed);
            
            // Demonstrate offset-based pagination alternative
            logger.info("Alternative: Offset-based pagination:");
            
            int pageSize = 10;
            for (int page = 0; page < 3; page++) { // Show first 3 pages
                int offset = page * pageSize;
                
                ResultSet<SqlRow> pageResults = sql.execute(null,
                    "SELECT ArtistId, Name FROM Artist ORDER BY Name LIMIT ? OFFSET ?",
                    pageSize, offset);
                
                logger.info("  Page {} (offset {}):", page + 1, offset);
                while (pageResults.hasNext()) {
                    SqlRow row = pageResults.next();
                    logger.info("    Artist {}: {}", 
                        row.intValue("ArtistId"),
                        row.stringValue("Name"));
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to execute paginated queries: {}", e.getMessage());
        }
    }
    
    /**
     * Demonstrates result set metadata introspection.
     * Shows how to analyze query results dynamically.
     */
    private void demonstrateMetadataIntrospection(IgniteClient client) {
        logger.info("=== Metadata Introspection ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // Execute query and examine metadata
            logger.info("Analyzing query result metadata:");
            
            ResultSet<SqlRow> result = sql.execute(null,
                "SELECT a.ArtistId, a.Name, COUNT(al.AlbumId) as AlbumCount, " +
                "       AVG(t.UnitPrice) as AvgTrackPrice " +
                "FROM Artist a " +
                "LEFT JOIN Album al ON a.ArtistId = al.ArtistId " +
                "LEFT JOIN Track t ON al.AlbumId = t.AlbumId " +
                "GROUP BY a.ArtistId, a.Name " +
                "HAVING COUNT(al.AlbumId) > 0 " +
                "ORDER BY AlbumCount DESC " +
                "LIMIT 5");
            
            // Examine result metadata
            var metadata = result.metadata();
            logger.info("Query returned {} columns:", metadata.columns().size());
            
            for (ColumnMetadata column : metadata.columns()) {
                logger.info("  Column '{}': {} ({}){}", 
                    column.name(),
                    column.type(),
                    column.valueClass().getSimpleName(),
                    column.nullable() ? " [nullable]" : " [not null]");
            }
            
            // Process results using metadata
            logger.info("Query results with dynamic processing:");
            while (result.hasNext()) {
                SqlRow row = result.next();
                
                StringBuilder rowData = new StringBuilder();
                for (ColumnMetadata column : metadata.columns()) {
                    Object value = row.value(column.name());
                    rowData.append(column.name()).append("=").append(value).append(" ");
                }
                
                logger.info("  Row: {}", rowData.toString().trim());
            }
            
            // Examine system table metadata
            logger.info("System table column information:");
            
            ResultSet<SqlRow> systemInfo = sql.execute(null,
                "SELECT column_name, data_type, is_nullable " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE table_name = ? " +
                "ORDER BY ordinal_position",
                "Artist");
            
            while (systemInfo.hasNext()) {
                SqlRow row = systemInfo.next();
                logger.info("  Artist.{}: {} ({})", 
                    row.stringValue("column_name"),
                    row.stringValue("data_type"),
                    row.stringValue("is_nullable").equals("YES") ? "nullable" : "not null");
            }
            
        } catch (Exception e) {
            logger.error("Failed to demonstrate metadata introspection: {}", e.getMessage());
        }
    }
    
    /**
     * Demonstrates performance optimization patterns for complex queries.
     * Shows techniques for efficient query execution in distributed environments.
     */
    private void demonstratePerformancePatterns(IgniteClient client) {
        logger.info("=== Performance Optimization Patterns ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // Prepared statement reuse for similar queries
            logger.info("Demonstrating prepared statement reuse:");
            
            Statement genreQuery = sql.statementBuilder()
                .query("SELECT COUNT(*) as count FROM Track WHERE GenreId = ?")
                .build();
            
            long startTime = System.currentTimeMillis();
            
            for (int genreId = 1; genreId <= 5; genreId++) {
                ResultSet<SqlRow> result = sql.execute(null, genreQuery, genreId);
                
                if (result.hasNext()) {
                    long count = result.next().longValue("count");
                    logger.info("  Genre {}: {} tracks", genreId, count);
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Prepared statement reuse completed in {} ms", duration);
            
            // Optimized JOIN with proper colocation
            logger.info("Demonstrating colocation-optimized query:");
            
            startTime = System.currentTimeMillis();
            
            ResultSet<SqlRow> colocatedJoin = sql.execute(null,
                "SELECT a.Name, COUNT(t.TrackId) as TrackCount " +
                "FROM Artist a " +
                "JOIN Album al ON a.ArtistId = al.ArtistId " +
                "JOIN Track t ON al.AlbumId = t.AlbumId " +
                "WHERE a.ArtistId <= ? " +
                "GROUP BY a.ArtistId, a.Name " +
                "ORDER BY TrackCount DESC", 10);
            
            int resultCount = 0;
            while (colocatedJoin.hasNext()) {
                SqlRow row = colocatedJoin.next();
                if (resultCount < 5) { // Only log first 5 for brevity
                    logger.info("  Artist '{}': {} tracks", 
                        row.stringValue("Name"),
                        row.longValue("TrackCount"));
                }
                resultCount++;
            }
            
            duration = System.currentTimeMillis() - startTime;
            logger.info("Colocation-optimized query completed in {} ms ({} results)", 
                duration, resultCount);
            
            // Batch-friendly query patterns
            logger.info("Demonstrating batch-friendly aggregation:");
            
            startTime = System.currentTimeMillis();
            
            Statement batchAggregation = sql.statementBuilder()
                .query("SELECT g.Name, COUNT(*) as count, AVG(t.UnitPrice) as avg_price " +
                       "FROM Genre g " +
                       "LEFT JOIN Track t ON g.GenreId = t.GenreId " +
                       "GROUP BY g.GenreId, g.Name " +
                       "HAVING COUNT(*) > 0 " +
                       "ORDER BY count DESC")
                .pageSize(100)  // Appropriate page size
                .build();
            
            ResultSet<SqlRow> batchResults = sql.execute(null, batchAggregation);
            
            int genreCount = 0;
            while (batchResults.hasNext()) {
                SqlRow row = batchResults.next();
                genreCount++;
                // Minimal processing for performance demo
            }
            
            duration = System.currentTimeMillis() - startTime;
            logger.info("Batch aggregation processed {} genres in {} ms", genreCount, duration);
            
            // Memory-efficient large result processing
            logger.info("Demonstrating memory-efficient processing:");
            
            Statement memoryEfficient = sql.statementBuilder()
                .query("SELECT TrackId, Name, Milliseconds FROM Track ORDER BY TrackId")
                .pageSize(100)  // Small page size for memory efficiency
                .build();
            
            startTime = System.currentTimeMillis();
            ResultSet<SqlRow> largeResults = sql.execute(null, memoryEfficient);
            
            int processedCount = 0;
            long totalDuration = 0;
            
            while (largeResults.hasNext() && processedCount < 500) { // Limit for demo
                SqlRow row = largeResults.next();
                
                // Simulate processing
                totalDuration += row.longValue("Milliseconds");
                processedCount++;
                
                // Log progress periodically
                if (processedCount % 100 == 0) {
                    logger.info("  Processed {} tracks...", processedCount);
                }
            }
            
            duration = System.currentTimeMillis() - startTime;
            logger.info("Memory-efficient processing: {} tracks in {} ms (avg duration: {} ms)", 
                processedCount, duration, processedCount > 0 ? totalDuration / processedCount : 0);
            
        } catch (Exception e) {
            logger.error("Failed to demonstrate performance patterns: {}", e.getMessage());
        }
    }
}