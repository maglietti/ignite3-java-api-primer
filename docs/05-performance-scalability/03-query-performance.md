# Chapter 5.3: Query Performance and Index Optimization

## Learning Objectives

By completing this chapter, you will:

- Optimize SQL query performance with proper indexing strategies
- Understand query execution plans and performance bottlenecks
- Implement efficient data retrieval patterns for music applications
- Configure distribution zones for optimal query performance

## Working with the Reference Application

The **`10-catalog-management-app`** demonstrates all query optimization patterns covered in this chapter with comprehensive music platform performance examples. Run it alongside your learning to see index optimization, query tuning, and distribution strategies in action.

**Quick Start**: After reading this chapter, explore the reference application:

```bash
cd ignite3-reference-apps/10-catalog-management-app
mvn compile exec:java
```

The reference app shows how the caching optimizations from [Chapter 5.2](02-caching-strategies.md) integrate with query performance patterns, demonstrating the complete performance optimization lifecycle for music platforms.

## The Query Performance Challenge

As music streaming platforms grow from thousands to millions of tracks, query performance becomes critical. A user searching for "love songs from the 80s" expects instant results across millions of tracks, while recommendation engines analyze listening patterns in real-time. The difference between sub-second and multi-second response times determines user satisfaction and platform success.

Query optimization requires understanding both the data patterns and access requirements specific to music platforms. Catalog searches require different optimization strategies than analytics queries, and recommendation algorithms have unique performance characteristics.

## Understanding Query Execution in Distributed Systems

### Query Analysis Fundamentals

Before optimizing queries, understanding how Ignite 3 processes distributed queries helps identify performance bottlenecks:

```java
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.sql.Statement;

/**
 * Query analysis and performance monitoring for music platform operations.
 * Demonstrates techniques for understanding query execution characteristics.
 */
public class QueryAnalysisDemo {
    
    private final IgniteSql sql;
    
    public QueryAnalysisDemo(IgniteClient client) {
        this.sql = client.sql();
    }
    
    /**
     * Analyze track search query performance.
     * Demonstrates measuring execution time and result characteristics.
     */
    public void analyzeTrackSearchQuery(String searchTerm) {
        String query = """
            SELECT t.TrackId, t.Name, ar.Name as ArtistName, al.Title as AlbumTitle
            FROM Track t
            JOIN Album al ON t.AlbumId = al.AlbumId  
            JOIN Artist ar ON al.ArtistId = ar.ArtistId
            WHERE LOWER(t.Name) LIKE LOWER(?)
            ORDER BY ar.Name, al.Title, t.Name
            LIMIT 50
            """;
        
        long startTime = System.nanoTime();
        
        try (ResultSet<SqlRow> results = sql.execute(null, query, "%" + searchTerm + "%")) {
            int resultCount = 0;
            long firstResultTime = 0;
            
            while (results.hasNext()) {
                SqlRow row = results.next();
                
                if (resultCount == 0) {
                    firstResultTime = System.nanoTime();
                }
                
                resultCount++;
                
                // Process result (would display in real application)
                String trackInfo = String.format("%s by %s from %s", 
                    row.stringValue("Name"),
                    row.stringValue("ArtistName"), 
                    row.stringValue("AlbumTitle"));
            }
            
            long totalTime = System.nanoTime() - startTime;
            long timeToFirstResult = firstResultTime > 0 ? firstResultTime - startTime : 0;
            
            System.out.printf("Track search analysis:%n");
            System.out.printf("  Search term: '%s'%n", searchTerm);
            System.out.printf("  Results found: %d%n", resultCount);
            System.out.printf("  Total execution time: %.2f ms%n", totalTime / 1_000_000.0);
            System.out.printf("  Time to first result: %.2f ms%n", timeToFirstResult / 1_000_000.0);
            
        } catch (Exception e) {
            System.err.println("Query analysis failed: " + e.getMessage());
        }
    }
    
    /**
     * Analyze genre-based analytics query performance.
     * Shows how complex aggregation queries perform across distributed data.
     */
    public void analyzeGenreAnalyticsQuery() {
        String query = """
            SELECT 
                g.Name as Genre,
                COUNT(t.TrackId) as TrackCount,
                COUNT(DISTINCT ar.ArtistId) as ArtistCount,
                AVG(t.UnitPrice) as AvgPrice,
                SUM(COALESCE(il.Quantity, 0)) as TotalSales
            FROM Genre g
            LEFT JOIN Track t ON g.GenreId = t.GenreId
            LEFT JOIN Album al ON t.AlbumId = al.AlbumId
            LEFT JOIN Artist ar ON al.ArtistId = ar.ArtistId
            LEFT JOIN InvoiceLine il ON t.TrackId = il.TrackId
            GROUP BY g.GenreId, g.Name
            HAVING COUNT(t.TrackId) > 0
            ORDER BY TotalSales DESC, TrackCount DESC
            """;
        
        long startTime = System.nanoTime();
        
        try (ResultSet<SqlRow> results = sql.execute(null, query)) {
            int genreCount = 0;
            long aggregationData = 0;
            
            while (results.hasNext()) {
                SqlRow row = results.next();
                genreCount++;
                
                // Track data volume for analysis
                aggregationData += row.longValue("TrackCount");
                
                if (genreCount <= 10) { // Show top 10 genres
                    System.out.printf("  %s: %d tracks, %d artists, %.2f avg price, %d sales%n",
                        row.stringValue("Genre"),
                        row.longValue("TrackCount"),
                        row.longValue("ArtistCount"), 
                        row.doubleValue("AvgPrice"),
                        row.longValue("TotalSales"));
                }
            }
            
            long totalTime = System.nanoTime() - startTime;
            
            System.out.printf("Genre analytics analysis:%n");
            System.out.printf("  Genres processed: %d%n", genreCount);
            System.out.printf("  Total tracks aggregated: %d%n", aggregationData);
            System.out.printf("  Execution time: %.2f ms%n", totalTime / 1_000_000.0);
            
        } catch (Exception e) {
            System.err.println("Analytics query analysis failed: " + e.getMessage());
        }
    }
    
    /**
     * Compare query performance with different WHERE clause strategies.
     * Shows impact of different filtering approaches on execution time.
     */
    public void compareFilteringStrategies(int artistId) {
        // Strategy 1: Direct filter on indexed column
        String indexedQuery = """
            SELECT COUNT(*) as TrackCount
            FROM Track t
            JOIN Album al ON t.AlbumId = al.AlbumId
            WHERE al.ArtistId = ?
            """;
        
        // Strategy 2: Filter with function call (less efficient)
        String functionQuery = """
            SELECT COUNT(*) as TrackCount  
            FROM Track t
            JOIN Album al ON t.AlbumId = al.AlbumId
            WHERE CAST(al.ArtistId AS VARCHAR) = CAST(? AS VARCHAR)
            """;
        
        // Strategy 3: Subquery approach
        String subqueryQuery = """
            SELECT COUNT(*) as TrackCount
            FROM Track t
            WHERE t.AlbumId IN (
                SELECT AlbumId FROM Album WHERE ArtistId = ?
            )
            """;
        
        System.out.printf("Comparing filtering strategies for artist %d:%n", artistId);
        
        long time1 = measureQueryTime(indexedQuery, artistId);
        long time2 = measureQueryTime(functionQuery, artistId);  
        long time3 = measureQueryTime(subqueryQuery, artistId);
        
        System.out.printf("  Direct indexed filter: %.2f ms%n", time1 / 1_000_000.0);
        System.out.printf("  Function-based filter: %.2f ms%n", time2 / 1_000_000.0);
        System.out.printf("  Subquery approach: %.2f ms%n", time3 / 1_000_000.0);
        
        // Identify best approach
        long fastest = Math.min(time1, Math.min(time2, time3));
        String recommendation = "unknown";
        if (fastest == time1) recommendation = "Direct indexed filter (optimal)";
        else if (fastest == time2) recommendation = "Function-based filter"; 
        else if (fastest == time3) recommendation = "Subquery approach";
        
        System.out.printf("  Recommended approach: %s%n", recommendation);
    }
    
    private long measureQueryTime(String query, Object... params) {
        long startTime = System.nanoTime();
        
        try (ResultSet<SqlRow> results = sql.execute(null, query, params)) {
            // Process all results to ensure complete execution
            while (results.hasNext()) {
                results.next();
            }
        } catch (Exception e) {
            System.err.println("Query measurement failed: " + e.getMessage());
            return Long.MAX_VALUE;
        }
        
        return System.nanoTime() - startTime;
    }
}
```

## Index Design and Creation

### Strategic Index Planning

Effective indexing requires understanding your application's query patterns. Music platforms typically have distinct access patterns that benefit from specific indexing strategies:

```java
import org.apache.ignite.catalog.IgniteCatalog;
import org.apache.ignite.catalog.IndexType;
import org.apache.ignite.catalog.definitions.IndexDefinition;

/**
 * Strategic index design for music platform query optimization.
 * Demonstrates creating indexes that align with common access patterns.
 */
public class MusicPlatformIndexStrategy {
    
    private final IgniteCatalog catalog;
    private final IgniteSql sql;
    
    public MusicPlatformIndexStrategy(IgniteClient client) {
        this.catalog = client.catalog();
        this.sql = client.sql();
    }
    
    /**
     * Create catalog search indexes for fast text-based queries.
     * Optimizes artist, album, and track name searches.
     */
    public void createCatalogSearchIndexes() {
        try {
            // Artist name index for artist search
            IndexDefinition artistNameIndex = IndexDefinition.builder("idx_artist_name")
                .tableName("Artist")
                .type(IndexType.SORTED)
                .columns("Name")
                .build();
            
            catalog.createIndexAsync(artistNameIndex)
                .thenRun(() -> System.out.println("Created artist name index"))
                .exceptionally(throwable -> {
                    if (!isIndexExists(throwable)) {
                        System.err.println("Failed to create artist name index: " + throwable.getMessage());
                    }
                    return null;
                });
            
            // Album title and artist composite index
            IndexDefinition albumSearchIndex = IndexDefinition.builder("idx_album_search")
                .tableName("Album")
                .type(IndexType.SORTED)
                .columns("ArtistId", "Title") // Composite key for efficient filtering
                .build();
            
            catalog.createIndexAsync(albumSearchIndex)
                .thenRun(() -> System.out.println("Created album search index"))
                .exceptionally(throwable -> {
                    if (!isIndexExists(throwable)) {
                        System.err.println("Failed to create album search index: " + throwable.getMessage());
                    }
                    return null;
                });
            
            // Track search with multiple access patterns
            IndexDefinition trackNameIndex = IndexDefinition.builder("idx_track_name")
                .tableName("Track")
                .type(IndexType.SORTED)
                .columns("Name")
                .build();
            
            IndexDefinition trackGenreIndex = IndexDefinition.builder("idx_track_genre")
                .tableName("Track")
                .type(IndexType.SORTED)
                .columns("GenreId", "UnitPrice") // Genre browsing with price sorting
                .build();
            
            CompletableFuture.allOf(
                catalog.createIndexAsync(trackNameIndex),
                catalog.createIndexAsync(trackGenreIndex)
            ).thenRun(() -> System.out.println("Created track search indexes"))
            .exceptionally(throwable -> {
                System.err.println("Some track indexes may have failed: " + throwable.getMessage());
                return null;
            });
            
        } catch (Exception e) {
            System.err.println("Error creating catalog search indexes: " + e.getMessage());
        }
    }
    
    /**
     * Create analytics indexes for business intelligence queries.
     * Optimizes reporting and dashboard queries.
     */
    public void createAnalyticsIndexes() {
        try {
            // Customer purchase analysis
            IndexDefinition customerPurchaseIndex = IndexDefinition.builder("idx_customer_purchases")
                .tableName("Invoice")
                .type(IndexType.SORTED)
                .columns("CustomerId", "InvoiceDate") // Customer purchase history
                .build();
            
            // Sales analysis by date and total
            IndexDefinition salesAnalysisIndex = IndexDefinition.builder("idx_sales_analysis")
                .tableName("Invoice") 
                .type(IndexType.SORTED)
                .columns("InvoiceDate", "Total") // Time-series sales analysis
                .build();
            
            // Track popularity analysis
            IndexDefinition trackPopularityIndex = IndexDefinition.builder("idx_track_popularity")
                .tableName("InvoiceLine")
                .type(IndexType.SORTED)
                .columns("TrackId", "Quantity") // Track sales performance
                .build();
            
            CompletableFuture.allOf(
                catalog.createIndexAsync(customerPurchaseIndex),
                catalog.createIndexAsync(salesAnalysisIndex),
                catalog.createIndexAsync(trackPopularityIndex)
            ).thenRun(() -> System.out.println("Created analytics indexes"))
            .exceptionally(throwable -> {
                System.err.println("Some analytics indexes may have failed: " + throwable.getMessage());
                return null;
            });
            
        } catch (Exception e) {
            System.err.println("Error creating analytics indexes: " + e.getMessage());
        }
    }
    
    /**
     * Create specialized indexes for recommendation engines.
     * Optimizes queries that power music recommendations.
     */
    public void createRecommendationIndexes() {
        try {
            // User listening patterns - what customers bought together
            IndexDefinition userPatternIndex = IndexDefinition.builder("idx_user_listening_patterns")
                .tableName("InvoiceLine")
                .type(IndexType.SORTED)
                .columns("TrackId", "InvoiceId") // Track co-purchase analysis
                .build();
            
            // Genre preferences for collaborative filtering  
            IndexDefinition genrePreferenceIndex = IndexDefinition.builder("idx_genre_preferences")
                .tableName("Track")
                .type(IndexType.SORTED)
                .columns("GenreId", "AlbumId", "TrackId") // Genre-based recommendations
                .build();
            
            // Artist discovery patterns
            IndexDefinition artistDiscoveryIndex = IndexDefinition.builder("idx_artist_discovery")
                .tableName("Album")
                .type(IndexType.SORTED)
                .columns("ArtistId", "AlbumId") // Artist discography navigation
                .build();
            
            CompletableFuture.allOf(
                catalog.createIndexAsync(userPatternIndex),
                catalog.createIndexAsync(genrePreferenceIndex),
                catalog.createIndexAsync(artistDiscoveryIndex)
            ).thenRun(() -> System.out.println("Created recommendation indexes"))
            .exceptionally(throwable -> {
                System.err.println("Some recommendation indexes may have failed: " + throwable.getMessage());
                return null;
            });
            
        } catch (Exception e) {
            System.err.println("Error creating recommendation indexes: " + e.getMessage());
        }
    }
    
    /**
     * Validate index effectiveness with sample queries.
     * Tests whether created indexes improve query performance.
     */
    public void validateIndexEffectiveness() {
        System.out.println("Validating index effectiveness...");
        
        // Test artist search performance
        long artistSearchTime = measureQueryTime(
            "SELECT * FROM Artist WHERE Name LIKE 'A%' ORDER BY Name LIMIT 10"
        );
        System.out.printf("Artist search with index: %.2f ms%n", artistSearchTime / 1_000_000.0);
        
        // Test album lookup performance  
        long albumLookupTime = measureQueryTime(
            "SELECT * FROM Album WHERE ArtistId = ? ORDER BY Title",
            1
        );
        System.out.printf("Album lookup with index: %.2f ms%n", albumLookupTime / 1_000_000.0);
        
        // Test genre browsing performance
        long genreBrowseTime = measureQueryTime(
            "SELECT * FROM Track WHERE GenreId = ? ORDER BY UnitPrice LIMIT 20",
            1
        );
        System.out.printf("Genre browse with index: %.2f ms%n", genreBrowseTime / 1_000_000.0);
        
        // Test analytics query performance
        long analyticsTime = measureQueryTime(
            "SELECT COUNT(*), AVG(Total) FROM Invoice WHERE InvoiceDate >= ? AND InvoiceDate < ?",
            java.time.LocalDate.now().minusMonths(1),
            java.time.LocalDate.now()
        );
        System.out.printf("Analytics query with index: %.2f ms%n", analyticsTime / 1_000_000.0);
    }
    
    private long measureQueryTime(String query, Object... params) {
        long startTime = System.nanoTime();
        
        try (ResultSet<SqlRow> results = sql.execute(null, query, params)) {
            int count = 0;
            while (results.hasNext()) {
                results.next();
                count++;
            }
            // Include result count in timing to ensure full execution
        } catch (Exception e) {
            System.err.println("Query validation failed: " + e.getMessage());
            return Long.MAX_VALUE;
        }
        
        return System.nanoTime() - startTime;
    }
    
    private boolean isIndexExists(Throwable throwable) {
        return throwable != null && 
               throwable.getMessage() != null && 
               throwable.getMessage().toLowerCase().contains("already exists");
    }
}
```

## Query Optimization Patterns

### Efficient Join Strategies

Music platforms frequently join multiple tables for comprehensive results. Understanding join optimization helps create responsive queries:

```java
/**
 * Optimized join patterns for music platform queries.
 * Demonstrates efficient strategies for multi-table operations.
 */
public class OptimizedJoinPatterns {
    
    private final IgniteSql sql;
    
    public OptimizedJoinPatterns(IgniteClient client) {
        this.sql = client.sql();
    }
    
    /**
     * Optimized artist catalog query with efficient join ordering.
     * Shows how join order affects performance in distributed queries.
     */
    public List<ArtistCatalogInfo> getArtistCatalogOptimized(String artistNamePattern) {
        // Optimized query: filter artists first (smallest result set)
        // then join with albums and tracks
        String optimizedQuery = """
            SELECT 
                ar.ArtistId,
                ar.Name as ArtistName,
                COUNT(DISTINCT al.AlbumId) as AlbumCount,
                COUNT(t.TrackId) as TrackCount,
                AVG(t.UnitPrice) as AvgTrackPrice
            FROM Artist ar
            LEFT JOIN Album al ON ar.ArtistId = al.ArtistId
            LEFT JOIN Track t ON al.AlbumId = t.AlbumId  
            WHERE ar.Name LIKE ?
            GROUP BY ar.ArtistId, ar.Name
            ORDER BY ar.Name
            """;
        
        List<ArtistCatalogInfo> results = new ArrayList<>();
        
        try (ResultSet<SqlRow> rs = sql.execute(null, optimizedQuery, artistNamePattern)) {
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                
                ArtistCatalogInfo info = new ArtistCatalogInfo(
                    row.intValue("ArtistId"),
                    row.stringValue("ArtistName"),
                    row.intValue("AlbumCount"),
                    row.intValue("TrackCount"),
                    row.doubleValue("AvgTrackPrice")
                );
                
                results.add(info);
            }
        }
        
        return results;
    }
    
    /**
     * Customer purchase history with optimized join strategy.
     * Uses colocation to minimize network traffic.
     */
    public List<CustomerPurchaseInfo> getCustomerPurchaseHistory(int customerId, int limit) {
        // Query optimized for customer data colocation
        String colocatedQuery = """
            SELECT 
                i.InvoiceId,
                i.InvoiceDate,
                i.Total,
                COUNT(il.InvoiceLineId) as ItemCount,
                STRING_AGG(t.Name, ', ') as TrackNames
            FROM Invoice i
            JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId
            JOIN Track t ON il.TrackId = t.TrackId
            WHERE i.CustomerId = ?
            GROUP BY i.InvoiceId, i.InvoiceDate, i.Total
            ORDER BY i.InvoiceDate DESC
            LIMIT ?
            """;
        
        List<CustomerPurchaseInfo> results = new ArrayList<>();
        
        try (ResultSet<SqlRow> rs = sql.execute(null, colocatedQuery, customerId, limit)) {
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                
                CustomerPurchaseInfo info = new CustomerPurchaseInfo(
                    row.intValue("InvoiceId"),
                    row.value("InvoiceDate"),
                    row.decimalValue("Total"),
                    row.intValue("ItemCount"),
                    row.stringValue("TrackNames")
                );
                
                results.add(info);
            }
        }
        
        return results;
    }
    
    /**
     * Genre popularity analysis with optimized aggregation.
     * Demonstrates efficient aggregation patterns for analytics.
     */
    public List<GenrePopularityInfo> getGenrePopularityAnalysis() {
        // Query optimized for aggregation performance
        String aggregationQuery = """
            WITH genre_stats AS (
                SELECT 
                    g.GenreId,
                    g.Name as GenreName,
                    COUNT(DISTINCT t.TrackId) as UniqueTrackCount,
                    COUNT(DISTINCT al.AlbumId) as UniqueAlbumCount,
                    COUNT(DISTINCT ar.ArtistId) as UniqueArtistCount
                FROM Genre g
                LEFT JOIN Track t ON g.GenreId = t.GenreId
                LEFT JOIN Album al ON t.AlbumId = al.AlbumId
                LEFT JOIN Artist ar ON al.ArtistId = ar.ArtistId
                GROUP BY g.GenreId, g.Name
            ),
            sales_stats AS (
                SELECT 
                    t.GenreId,
                    COUNT(il.InvoiceLineId) as TotalSales,
                    SUM(il.Quantity) as TotalQuantity,
                    AVG(il.UnitPrice) as AvgSalePrice
                FROM Track t
                JOIN InvoiceLine il ON t.TrackId = il.TrackId
                GROUP BY t.GenreId
            )
            SELECT 
                gs.GenreId,
                gs.GenreName,
                gs.UniqueTrackCount,
                gs.UniqueAlbumCount, 
                gs.UniqueArtistCount,
                COALESCE(ss.TotalSales, 0) as TotalSales,
                COALESCE(ss.TotalQuantity, 0) as TotalQuantity,
                COALESCE(ss.AvgSalePrice, 0) as AvgSalePrice
            FROM genre_stats gs
            LEFT JOIN sales_stats ss ON gs.GenreId = ss.GenreId
            ORDER BY COALESCE(ss.TotalSales, 0) DESC, gs.UniqueTrackCount DESC
            """;
        
        List<GenrePopularityInfo> results = new ArrayList<>();
        
        try (ResultSet<SqlRow> rs = sql.execute(null, aggregationQuery)) {
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                
                GenrePopularityInfo info = new GenrePopularityInfo(
                    row.intValue("GenreId"),
                    row.stringValue("GenreName"),
                    row.intValue("UniqueTrackCount"),
                    row.intValue("UniqueAlbumCount"),
                    row.intValue("UniqueArtistCount"),
                    row.intValue("TotalSales"),
                    row.intValue("TotalQuantity"),
                    row.doubleValue("AvgSalePrice")
                );
                
                results.add(info);
            }
        }
        
        return results;
    }
    
    /**
     * Recommendation query optimized for real-time performance.
     * Uses efficient filtering and limiting for responsive recommendations.
     */
    public List<TrackRecommendation> getTrackRecommendationsOptimized(int customerId, int limit) {
        // Multi-step optimization: find similar customers, then their preferences
        String recommendationQuery = """
            WITH customer_genres AS (
                SELECT t.GenreId, COUNT(*) as GenreCount
                FROM Invoice i
                JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId
                JOIN Track t ON il.TrackId = t.TrackId
                WHERE i.CustomerId = ?
                GROUP BY t.GenreId
                ORDER BY GenreCount DESC
                LIMIT 5
            ),
            similar_customers AS (
                SELECT DISTINCT i2.CustomerId
                FROM Invoice i2
                JOIN InvoiceLine il2 ON i2.InvoiceId = il2.InvoiceId
                JOIN Track t2 ON il2.TrackId = t2.TrackId
                JOIN customer_genres cg ON t2.GenreId = cg.GenreId
                WHERE i2.CustomerId != ?
                LIMIT 100
            ),
            recommendations AS (
                SELECT 
                    t.TrackId,
                    t.Name as TrackName,
                    ar.Name as ArtistName,
                    al.Title as AlbumTitle,
                    COUNT(*) as PopularityScore
                FROM similar_customers sc
                JOIN Invoice i ON sc.CustomerId = i.CustomerId
                JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId
                JOIN Track t ON il.TrackId = t.TrackId
                JOIN Album al ON t.AlbumId = al.AlbumId
                JOIN Artist ar ON al.ArtistId = ar.ArtistId
                WHERE t.TrackId NOT IN (
                    SELECT DISTINCT t2.TrackId
                    FROM Invoice i2
                    JOIN InvoiceLine il2 ON i2.InvoiceId = il2.InvoiceId
                    JOIN Track t2 ON il2.TrackId = t2.TrackId
                    WHERE i2.CustomerId = ?
                )
                GROUP BY t.TrackId, t.Name, ar.Name, al.Title
                ORDER BY PopularityScore DESC
                LIMIT ?
            )
            SELECT * FROM recommendations
            """;
        
        List<TrackRecommendation> results = new ArrayList<>();
        
        try (ResultSet<SqlRow> rs = sql.execute(null, recommendationQuery, 
                customerId, customerId, customerId, limit)) {
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                
                TrackRecommendation recommendation = new TrackRecommendation(
                    row.intValue("TrackId"),
                    row.stringValue("TrackName"),
                    row.stringValue("ArtistName"),
                    row.stringValue("AlbumTitle"),
                    row.intValue("PopularityScore")
                );
                
                results.add(recommendation);
            }
        }
        
        return results;
    }
}

// Helper classes for query results
class ArtistCatalogInfo {
    private final int artistId;
    private final String artistName;
    private final int albumCount;
    private final int trackCount;
    private final double avgTrackPrice;
    
    // Constructor and getters...
}

class CustomerPurchaseInfo {
    private final int invoiceId;
    private final Object invoiceDate;
    private final java.math.BigDecimal total;
    private final int itemCount;
    private final String trackNames;
    
    // Constructor and getters...
}

class GenrePopularityInfo {
    private final int genreId;
    private final String genreName;
    private final int uniqueTrackCount;
    private final int uniqueAlbumCount;
    private final int uniqueArtistCount;
    private final int totalSales;
    private final int totalQuantity;
    private final double avgSalePrice;
    
    // Constructor and getters...
}

class TrackRecommendation {
    private final int trackId;
    private final String trackName;
    private final String artistName;
    private final String albumTitle;
    private final int popularityScore;
    
    // Constructor and getters...
}
```

## Distribution Zone Optimization

### Zone Configuration for Query Performance

Distribution zones affect query performance by determining data placement and replication strategies:

```java
import org.apache.ignite.catalog.definitions.ZoneDefinition;

/**
 * Distribution zone optimization for music platform performance.
 * Shows how zone configuration affects query execution efficiency.
 */
public class DistributionZoneOptimization {
    
    private final IgniteCatalog catalog;
    
    public DistributionZoneOptimization(IgniteClient client) {
        this.catalog = client.catalog();
    }
    
    /**
     * Create performance-optimized zones for different data types.
     * Configures zones based on access patterns and consistency requirements.
     */
    public void createOptimizedZones() {
        try {
            // Catalog zone: optimized for read-heavy workloads
            ZoneDefinition catalogZone = ZoneDefinition.builder("MusicCatalogZone")
                .replicas(3)           // High availability for catalog data
                .partitions(32)        // Good parallelism for searches
                .dataNodesAutoAdjust(5)    // Auto-adjust with cluster growth
                .build();
            
            // Customer zone: optimized for transactional consistency
            ZoneDefinition customerZone = ZoneDefinition.builder("CustomerDataZone")
                .replicas(2)           // Balanced consistency and performance
                .partitions(16)        // Fewer partitions for transactional data
                .dataNodesAutoAdjust(3)    // Conservative scaling
                .build();
            
            // Analytics zone: optimized for large-scale queries
            ZoneDefinition analyticsZone = ZoneDefinition.builder("AnalyticsZone")
                .replicas(2)           // Performance over availability
                .partitions(64)        // High parallelism for analytics
                .dataNodesAutoAdjust(10)   // Aggressive scaling for analytics
                .build();
            
            // Events zone: optimized for high-throughput writes
            ZoneDefinition eventsZone = ZoneDefinition.builder("EventsZone")
                .replicas(1)           // Minimal replication for throughput
                .partitions(128)       // Maximum parallelism
                .dataNodesAutoAdjust(8)    // Scale with write load
                .build();
            
            CompletableFuture.allOf(
                catalog.createZoneAsync(catalogZone),
                catalog.createZoneAsync(customerZone),
                catalog.createZoneAsync(analyticsZone),
                catalog.createZoneAsync(eventsZone)
            ).thenRun(() -> System.out.println("Created performance-optimized zones"))
            .exceptionally(throwable -> {
                System.err.println("Some zones may have failed to create: " + throwable.getMessage());
                return null;
            });
            
        } catch (Exception e) {
            System.err.println("Error creating optimized zones: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrate zone-aware query patterns.
     * Shows how to write queries that leverage zone characteristics.
     */
    public void demonstrateZoneAwareQueries() {
        // Example: Catalog browsing leverages high-replica catalog zone
        System.out.println("Catalog zone queries benefit from:");
        System.out.println("- High replica count for read scalability");
        System.out.println("- Optimized partitioning for search performance");
        System.out.println("- Auto-adjustment for growing catalog data");
        
        // Example: Customer transactions leverage consistent customer zone
        System.out.println("\nCustomer zone queries benefit from:");
        System.out.println("- Balanced replication for consistency");
        System.out.println("- Conservative partitioning for transaction performance");
        System.out.println("- Stable configuration for predictable behavior");
        
        // Example: Analytics queries leverage high-partition analytics zone
        System.out.println("\nAnalytics zone queries benefit from:");
        System.out.println("- High partition count for parallel processing");
        System.out.println("- Aggressive scaling for large datasets");
        System.out.println("- Performance-focused replication strategy");
    }
}
```

Query performance optimization transforms how music platforms handle user interactions and business operations. By implementing strategic indexing, efficient join patterns, and zone-aware distribution, applications can deliver sub-second responses even as data grows to millions of records.

## Module Conclusion

You've now mastered the complete performance and scalability foundation in Ignite 3, from high-throughput data streaming through intelligent caching to query optimization. This prepares you for the production concerns and operational patterns covered in the final module.

- **Continue Learning**: **[Module 6: Production Concerns](../06-production-concerns/01-deployment-patterns.md)** - Apply your performance optimization knowledge to master production deployment, monitoring, and operational management patterns
