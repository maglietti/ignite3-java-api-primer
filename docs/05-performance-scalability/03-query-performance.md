# Chapter 5.3: Query Performance and Index Optimization

Your music streaming platform handles 10 million users, but your marketing team can't get basic analytics because genre popularity reports timeout after 30 seconds. Customer searches for "rock albums" return results after 5-10 seconds while Spotify delivers instant responses. Your recommendation engine fails to generate suggestions during peak traffic because cross-table joins overwhelm the cluster with excessive data movement.

The problem isn't your data volume - it's how queries execute across distributed nodes. Without proper indexes, your "most popular jazz tracks" query scans 50 million records instead of using optimized access paths. Without partition pruning, artist lookup queries hit every cluster node instead of routing directly to data locations. Without proper zone configuration, analytics joins force massive datasets across network boundaries.

Your business depends on real-time insights: which genres drive revenue, which artists generate engagement, which tracks predict subscription renewals. But your current query execution creates a choice between fast responses and accurate analytics. This chapter eliminates that trade-off through systematic query optimization that delivers sub-second analytics across petabytes of distributed music data.

## Query Execution Analysis

Your recommendation algorithm queries timeout because they don't understand how Ignite 3 executes SQL across distributed data. When you run `SELECT COUNT(*) FROM Track WHERE GenreId = 1`, the query might scan every node in your cluster or execute efficiently on a single partition - the difference depends on indexing strategies and data distribution decisions you made during schema design.

Distributed query performance depends on three critical factors: data locality (can the query execute without network data movement), index utilization (does filtering happen early or late in execution), and partition pruning (does the query coordinator know which nodes contain relevant data). Understanding these factors transforms 30-second analytical queries into sub-second insights that power real-time business decisions.

### Basic Performance Measurement

Track query execution characteristics to identify performance bottlenecks:

```java
/**
 * Basic query performance measurement for optimization analysis.
 */
public class QueryTimingAnalysis {
    
    private final IgniteSql sql;
    
    public QueryTimingAnalysis(IgniteClient client) {
        this.sql = client.sql();
    }
    
    /**
     * Measure query execution time and result characteristics using Statement API.
     */
    public QueryMetrics analyzeStatement(Statement statement, Object... params) {
        long startTime = System.nanoTime();
        int resultCount = 0;
        long firstResultTime = 0;
        
        try (ResultSet<SqlRow> results = sql.execute(null, statement, params)) {
            while (results.hasNext()) {
                SqlRow row = results.next();
                
                if (resultCount == 0) {
                    firstResultTime = System.nanoTime();
                }
                resultCount++;
            }
        }
        
        long totalTime = System.nanoTime() - startTime;
        long timeToFirstResult = firstResultTime > 0 ? firstResultTime - startTime : 0;
        
        return new QueryMetrics(resultCount, totalTime, timeToFirstResult);
    }
    
    /**
     * Legacy method for simple query strings - use analyzeStatement() for new code.
     */
    public QueryMetrics analyzeQuery(String query, Object... params) {
        Statement statement = client.sql().statementBuilder().query(query).build();
        return analyzeStatement(statement, params);
    }
}

class QueryMetrics {
    public final int resultCount;
    public final long totalTimeNanos;
    public final long timeToFirstResultNanos;
    
    // Constructor and helper methods for displaying metrics...
}
```

### Understanding Query Execution Bottlenecks

Your platform's search performance varies wildly because different query approaches create dramatically different execution costs. When users search for tracks by a specific artist, the database might execute efficiently in 10 milliseconds or struggle for 10 seconds - the difference depends on whether the query optimizer can leverage indexes or must scan entire tables.

Distributed systems amplify these performance differences because inefficient queries force data movement across network boundaries, while optimized queries execute locally on single nodes. Understanding how filtering strategies affect execution plans enables you to design queries that scale predictably as your music catalog grows from thousands to millions of tracks.

### Filter Strategy Performance Analysis

The same business logic implemented through different SQL approaches creates vastly different performance characteristics:

```java
/**
 * Compare filtering strategies to identify optimal query approaches.
 */
public void compareFilteringStrategies(int artistId) {
    QueryTimingAnalysis analyzer = new QueryTimingAnalysis(client);
    
    // Strategy 1: Direct indexed filter (optimal)
    Statement indexedQuery = client.sql().statementBuilder()
        .query("SELECT COUNT(*) FROM Track t JOIN Album al ON t.AlbumId = al.AlbumId WHERE al.ArtistId = ?")
        .build();
    
    // Strategy 2: Function-based filter (suboptimal)
    Statement functionQuery = client.sql().statementBuilder()
        .query("SELECT COUNT(*) FROM Track t JOIN Album al ON t.AlbumId = al.AlbumId WHERE CAST(al.ArtistId AS VARCHAR) = CAST(? AS VARCHAR)")
        .build();
    
    QueryMetrics indexed = analyzer.analyzeStatement(indexedQuery, artistId);
    QueryMetrics function = analyzer.analyzeStatement(functionQuery, artistId);
    
    System.out.printf("Direct indexed filter: %.2f ms (optimal for production)%n", 
        indexed.totalTimeNanos / 1_000_000.0);
    System.out.printf("Function-based filter: %.2f ms (avoid in distributed systems)%n", 
        function.totalTimeNanos / 1_000_000.0);
    
    // Direct filters typically execute 10-100x faster than function-based filters
    if (function.totalTimeNanos > indexed.totalTimeNanos * 5) {
        System.out.printf("⚠️  Function-based filter is %.1fx slower than direct filter%n", 
            (double) function.totalTimeNanos / indexed.totalTimeNanos);
    }
}
```

### Analytics Query Performance

Complex aggregation queries require special attention for distributed execution:

```java
/**
 * Analyze performance of analytics queries across distributed data.
 */
public void analyzeGenreAnalytics() {
    Statement analyticsQuery = client.sql().statementBuilder()
        .query("SELECT g.Name as Genre, COUNT(t.TrackId) as TrackCount, AVG(t.UnitPrice) as AvgPrice FROM Genre g LEFT JOIN Track t ON g.GenreId = t.GenreId GROUP BY g.GenreId, g.Name ORDER BY TrackCount DESC")
        .build();
    
    QueryMetrics metrics = analyzeStatement(analyticsQuery);
    
    // Analytics queries should complete in under 1 second for good UX
    if (metrics.totalTimeNanos > 1_000_000_000) {
        System.out.println("Analytics query exceeds 1s - consider optimization");
    }
}

## Strategic Index Design for Music Analytics

Your music platform's search functionality embarrasses you during investor demos. Typing "Rock" into the artist search takes 8 seconds to return results, while typing the same query into Spotify returns instantly. Your genre analytics dashboard shows "Loading..." for 45 seconds before displaying basic statistics. Customer browsing sessions abandon after 3 seconds of waiting for album listings.

The root cause isn't your server capacity - it's missing indexes that transform table scans into direct data access. When a user searches for artists containing "Beatles," your query scans 2 million artist records sequentially instead of using a name-based index that finds matches in microseconds. When analytics queries aggregate tracks by genre, they process 50 million records instead of using composite indexes that pre-organize data for grouping operations.

Strategic index design transforms your platform from unresponsive to instantaneous by creating data access paths that match your application's query characteristics. Every search becomes a direct lookup, every analytics query becomes an optimized aggregation, and every user interaction delivers the real-time experience that modern applications require.

### Catalog Search Optimization

Create indexes that support common search operations:

```java
/**
 * Create indexes optimized for catalog browsing and search.
 */
public void createSearchIndexes() {
    IgniteCatalog catalog = client.catalog();
    
    // Artist name search index
    IndexDefinition artistNameIndex = IndexDefinition.builder("idx_artist_name")
        .tableName("Artist")
        .type(IndexType.SORTED)
        .columns("Name")
        .build();
    
    // Composite album search index (artist + title)
    IndexDefinition albumSearchIndex = IndexDefinition.builder("idx_album_search")
        .tableName("Album")
        .type(IndexType.SORTED)
        .columns("ArtistId", "Title") // Supports artist-specific album queries
        .build();
    
    // Track genre browsing index
    IndexDefinition trackGenreIndex = IndexDefinition.builder("idx_track_genre")
        .tableName("Track")
        .type(IndexType.SORTED)
        .columns("GenreId", "UnitPrice") // Genre filtering with price sorting
        .build();
    
    // Create indexes asynchronously
    CompletableFuture.allOf(
        catalog.createIndexAsync(artistNameIndex),
        catalog.createIndexAsync(albumSearchIndex),
        catalog.createIndexAsync(trackGenreIndex)
    ).thenRun(() -> System.out.println("Search indexes created successfully"));
}
```

### Analytics Indexes

Optimize business intelligence and reporting queries:

```java
/**
 * Create indexes for analytics and reporting queries.
 */
public void createAnalyticsIndexes() {
    // Customer purchase history index
    IndexDefinition customerHistoryIndex = IndexDefinition.builder("idx_customer_history")
        .tableName("Invoice")
        .type(IndexType.SORTED)
        .columns("CustomerId", "InvoiceDate") // Customer activity over time
        .build();
    
    // Sales analysis index
    IndexDefinition salesIndex = IndexDefinition.builder("idx_sales_analysis")
        .tableName("Invoice")
        .type(IndexType.SORTED)
        .columns("InvoiceDate", "Total") // Time-series sales reporting
        .build();
    
    // Track popularity index
    IndexDefinition popularityIndex = IndexDefinition.builder("idx_track_popularity")
        .tableName("InvoiceLine")
        .type(IndexType.SORTED)
        .columns("TrackId", "Quantity") // Track sales performance
        .build();
    
    catalog.createIndexAsync(customerHistoryIndex);
    catalog.createIndexAsync(salesIndex);
    catalog.createIndexAsync(popularityIndex);
}
```

### Index Effectiveness Validation

Test index performance improvements:

```java
/**
 * Validate that indexes improve query performance.
 */
public void validateIndexes() {
    QueryTimingAnalysis analyzer = new QueryTimingAnalysis(client);
    
    // Test artist search performance
    QueryMetrics artistSearch = analyzer.analyzeQuery(
        "SELECT * FROM Artist WHERE Name LIKE ? ORDER BY Name LIMIT 10", 
        "A%"
    );
    
    // Test album lookup performance
    QueryMetrics albumLookup = analyzer.analyzeQuery(
        "SELECT * FROM Album WHERE ArtistId = ? ORDER BY Title", 
        1
    );
    
    System.out.printf("Artist search: %.2f ms%n", 
        artistSearch.totalTimeNanos / 1_000_000.0);
    System.out.printf("Album lookup: %.2f ms%n", 
        albumLookup.totalTimeNanos / 1_000_000.0);
    
    // Good performance targets: < 100ms for search, < 50ms for lookups
    if (artistSearch.totalTimeNanos > 100_000_000) {
        System.out.println("Artist search may need additional optimization");
    }
}
```

### Index Design Guidelines

Choose index strategies based on query characteristics:

- **Single-column indexes**: Use for simple equality and range queries
- **Composite indexes**: Use for multi-column WHERE clauses and sorting
- **Order matters**: Place most selective columns first in composite indexes
- **Covering indexes**: Include additional columns to avoid table lookups

## Distributed Join Optimization

Your artist catalog displays load for 45 seconds because your multi-table joins process data in the worst possible order. Instead of filtering artists first and then joining with their albums, your query joins all artists with all albums before applying filters - forcing millions of unnecessary records across network boundaries before eliminating 99% of the data.

This happens because distributed join optimization requires understanding both SQL query planning and cluster data placement. When you query for "artists with albums released after 2020," the execution order determines whether you process 50 records or 50 million records. Poor join order creates exponential performance degradation as your catalog grows.

The solution involves designing queries that exploit data colocation and filter selectivity. By placing the most restrictive filters first and leveraging your schema's colocation strategies, multi-table analytics queries execute on single nodes instead of coordinating massive datasets across cluster boundaries.

### Real-World Join Performance Analysis

The difference between optimized and unoptimized joins becomes dramatic at music streaming scale:

```java
/**
 * Optimized join strategies for music platform queries.
 * Demonstrates efficient strategies for multi-table operations.
 */
public class OptimizedJoinStrategies {
    
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
     * Demonstrates efficient aggregation techniques for analytics.
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

## Query Execution Plan Analysis

Before optimizing queries through indexes or zone configuration, understanding how Ignite 3 executes SQL operations provides insight into performance bottlenecks. The `EXPLAIN PLAN FOR` functionality reveals query execution paths, join strategies, and resource usage characteristics that identify optimization opportunities.

Query execution plans show whether operations scan entire tables or use indexes, how joins execute across distributed data, and where filtering occurs in the execution pipeline. This analysis guides decisions about index creation, query restructuring, and zone configuration adjustments.

```java
/**
 * Query execution plan analysis for music platform optimization.
 * Demonstrates using EXPLAIN PLAN FOR to understand query performance characteristics.
 */
public class QueryExecutionPlanAnalysis {
    
    private final IgniteSql sql;
    
    public QueryExecutionPlanAnalysis(IgniteClient client) {
        this.sql = client.sql();
    }
    
    /**
     * Analyze execution plan for artist search query.
     * Shows how text search operations execute across distributed data.
     */
    public void analyzeArtistSearchPlan(String searchTerm) {
        String query = """
            SELECT ar.ArtistId, ar.Name, COUNT(al.AlbumId) as AlbumCount
            FROM Artist ar
            LEFT JOIN Album al ON ar.ArtistId = al.ArtistId
            WHERE LOWER(ar.Name) LIKE LOWER(?)
            GROUP BY ar.ArtistId, ar.Name
            ORDER BY ar.Name
            """;
        
        System.out.println("=== Artist Search Query Execution Plan ===");
        analyzeQueryExecutionPlan("EXPLAIN PLAN FOR " + query, "%" + searchTerm + "%");
        
        // Compare with indexed vs non-indexed scenarios
        System.out.println("\n--- Execution Plan Analysis ---");
        System.out.println("Key observations to look for:");
        System.out.println("- Table scan vs index scan operations");
        System.out.println("- Join algorithm selection (nested loop, hash, merge)");
        System.out.println("- Filter pushdown effectiveness");
        System.out.println("- Sort operation placement and cost");
        System.out.println("- Network data movement characteristics");
    }
    
    /**
     * Analyze execution plan for customer purchase analytics.
     * Demonstrates how complex aggregation queries execute.
     */
    public void analyzeCustomerAnalyticsPlan(int customerId) {
        String query = """
            SELECT 
                c.CustomerId,
                c.FirstName,
                c.LastName,
                COUNT(i.InvoiceId) as PurchaseCount,
                SUM(i.Total) as TotalSpent,
                AVG(i.Total) as AvgPurchase,
                COUNT(DISTINCT t.GenreId) as GenresDiversit
            FROM Customer c
            JOIN Invoice i ON c.CustomerId = i.CustomerId
            JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId
            JOIN Track t ON il.TrackId = t.TrackId
            WHERE c.CustomerId = ?
            GROUP BY c.CustomerId, c.FirstName, c.LastName
            """;
        
        System.out.println("=== Customer Analytics Query Execution Plan ===");
        analyzeQueryExecutionPlan("EXPLAIN PLAN FOR " + query, customerId);
        
        System.out.println("\n--- Aggregation Plan Analysis ---");
        System.out.println("Optimization indicators:");
        System.out.println("- Hash aggregation vs sort-based aggregation");
        System.out.println("- Join order optimization based on selectivity");
        System.out.println("- Predicate pushdown to reduce intermediate results");
        System.out.println("- Parallel execution across partitions");
    }
    
    /**
     * Compare execution plans for different query formulations.
     * Shows how query structure affects execution strategy.
     */
    public void compareQueryFormulations() {
        System.out.println("=== Query Formulation Comparison ===");
        
        // Approach 1: Subquery with EXISTS
        String existsQuery = """
            SELECT ar.ArtistId, ar.Name
            FROM Artist ar
            WHERE EXISTS (
                SELECT 1 FROM Album al 
                JOIN Track t ON al.AlbumId = t.AlbumId
                WHERE al.ArtistId = ar.ArtistId 
                AND t.UnitPrice > 1.0
            )
            ORDER BY ar.Name
            """;
        
        // Approach 2: JOIN with DISTINCT
        String joinQuery = """
            SELECT DISTINCT ar.ArtistId, ar.Name
            FROM Artist ar
            JOIN Album al ON ar.ArtistId = al.ArtistId
            JOIN Track t ON al.AlbumId = t.AlbumId
            WHERE t.UnitPrice > 1.0
            ORDER BY ar.Name
            """;
        
        // Approach 3: Window function approach
        String windowQuery = """
            SELECT ArtistId, Name
            FROM (
                SELECT 
                    ar.ArtistId, 
                    ar.Name,
                    ROW_NUMBER() OVER (PARTITION BY ar.ArtistId ORDER BY ar.ArtistId) as rn
                FROM Artist ar
                JOIN Album al ON ar.ArtistId = al.ArtistId
                JOIN Track t ON al.AlbumId = t.AlbumId
                WHERE t.UnitPrice > 1.0
            ) ranked
            WHERE rn = 1
            ORDER BY Name
            """;
        
        System.out.println("\n--- EXISTS Subquery Approach ---");
        analyzeQueryExecutionPlan("EXPLAIN PLAN FOR " + existsQuery);
        
        System.out.println("\n--- JOIN with DISTINCT Approach ---");
        analyzeQueryExecutionPlan("EXPLAIN PLAN FOR " + joinQuery);
        
        System.out.println("\n--- Window Function Approach ---");
        analyzeQueryExecutionPlan("EXPLAIN PLAN FOR " + windowQuery);
        
        System.out.println("\n--- Comparison Guidelines ---");
        System.out.println("Evaluate plans based on:");
        System.out.println("- Estimated row counts at each step");
        System.out.println("- Join algorithm efficiency");
        System.out.println("- Sort and aggregation placement");
        System.out.println("- Overall execution cost estimates");
    }
    
    /**
     * Analyze execution plan for genre popularity query.
     * Demonstrates plan analysis for complex analytical workloads.
     */
    public void analyzeGenrePopularityPlan() {
        String complexAnalyticsQuery = """
            WITH genre_sales AS (
                SELECT 
                    g.GenreId,
                    g.Name as GenreName,
                    COUNT(il.InvoiceLineId) as SalesCount,
                    SUM(il.Quantity * il.UnitPrice) as Revenue,
                    COUNT(DISTINCT i.CustomerId) as UniqueCustomers
                FROM Genre g
                JOIN Track t ON g.GenreId = t.GenreId
                JOIN InvoiceLine il ON t.TrackId = il.TrackId
                JOIN Invoice i ON il.InvoiceId = i.InvoiceId
                GROUP BY g.GenreId, g.Name
            ),
            genre_rankings AS (
                SELECT 
                    *,
                    RANK() OVER (ORDER BY Revenue DESC) as RevenueRank,
                    RANK() OVER (ORDER BY SalesCount DESC) as SalesRank
                FROM genre_sales
            )
            SELECT 
                GenreId,
                GenreName,
                SalesCount,
                Revenue,
                UniqueCustomers,
                RevenueRank,
                SalesRank,
                (RevenueRank + SalesRank) / 2.0 as CompositeScore
            FROM genre_rankings
            WHERE RevenueRank <= 10 OR SalesRank <= 10
            ORDER BY CompositeScore
            """;
        
        System.out.println("=== Complex Analytics Query Execution Plan ===");
        analyzeQueryExecutionPlan("EXPLAIN PLAN FOR " + complexAnalyticsQuery);
        
        System.out.println("\n--- Complex Query Plan Analysis ---");
        System.out.println("Advanced optimization patterns:");
        System.out.println("- CTE materialization vs inline expansion");
        System.out.println("- Window function execution strategy");
        System.out.println("- Hash vs sort-based operations");
        System.out.println("- Partition pruning effectiveness");
        System.out.println("- Memory usage for intermediate results");
    }
    
    /**
     * Execute and analyze query execution plan.
     * Helper method that displays execution plan details.
     */
    private void analyzeQueryExecutionPlan(String explainQuery, Object... params) {
        try (ResultSet<SqlRow> planResults = sql.execute(null, explainQuery, params)) {
            
            System.out.println("Query Execution Plan:");
            int stepNumber = 1;
            
            while (planResults.hasNext()) {
                SqlRow planRow = planResults.next();
                
                // The exact column names may vary based on Ignite 3 implementation
                // Common execution plan information typically includes:
                String planStep = planRow.stringValue(0); // Plan step description
                
                System.out.printf("%2d. %s%n", stepNumber++, planStep);
            }
            
        } catch (Exception e) {
            System.err.println("Failed to analyze execution plan: " + e.getMessage());
            System.err.println("Note: EXPLAIN PLAN FOR syntax may vary by Ignite 3 version");
        }
    }
    
    /**
     * Practical query optimization workflow using execution plans.
     * Demonstrates step-by-step optimization process.
     */
    public void demonstrateOptimizationWorkflow() {
        System.out.println("=== Query Optimization Workflow ===");
        
        System.out.println("\n1. Baseline Query Analysis");
        System.out.println("   - Run EXPLAIN PLAN FOR on current query");
        System.out.println("   - Identify table scans and expensive operations");
        System.out.println("   - Note join algorithms and sort operations");
        
        System.out.println("\n2. Index Impact Assessment");
        System.out.println("   - Create candidate indexes");
        System.out.println("   - Re-run EXPLAIN PLAN FOR with indexes");
        System.out.println("   - Compare execution plans before/after");
        
        System.out.println("\n3. Query Restructuring Evaluation");
        System.out.println("   - Test alternative query formulations");
        System.out.println("   - Analyze plans for each approach");
        System.out.println("   - Select approach with optimal plan");
        
        System.out.println("\n4. Zone Configuration Validation");
        System.out.println("   - Verify partition pruning effectiveness");
        System.out.println("   - Check parallel execution utilization");
        System.out.println("   - Validate data locality optimizations");
        
        System.out.println("\n5. Performance Verification");
        System.out.println("   - Execute optimized query with timing");
        System.out.println("   - Compare actual vs estimated costs");
        System.out.println("   - Monitor resource utilization patterns");
        
        System.out.println("\nExecution Plan Key Indicators:");
        System.out.println("✓ Index scans instead of table scans");
        System.out.println("✓ Hash joins for large datasets");
        System.out.println("✓ Merge joins for sorted inputs");
        System.out.println("✓ Early filtering (predicate pushdown)");
        System.out.println("✓ Parallel execution across partitions");
        System.out.println("✓ Minimal data movement between nodes");
    }
}
```

## Distribution Zone Configuration

Query performance degrades when data distribution doesn't match access patterns. Catalog queries execute slowly because data spreads across too many partitions without sufficient replicas for read scaling. Analytics operations fail to leverage parallelism because partition counts are too low for large-scale aggregations. Customer transaction queries suffer from inconsistent performance due to inappropriate replication strategies.

Zone configuration optimizes query execution by aligning data placement with operational requirements:

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

## Production Query Performance Success

Your music platform's transformation from 30-second analytics queries to sub-second insights represents the difference between a platform that struggles with growth and one that scales effortlessly with business success. The techniques in this chapter - execution plan analysis, strategic indexing, distributed join optimization, and zone-aware configuration - work together to deliver the real-time analytics that modern music platforms require.

**Before Optimization**: Genre popularity reports timeout, artist searches frustrate users, recommendation algorithms can't process enough data to generate meaningful suggestions.

**After Optimization**: Marketing dashboards update in real-time, customer searches return instantly, recommendation engines analyze massive datasets to deliver personalized experiences that drive engagement and revenue.

The systematic approach - measuring execution characteristics, designing indexes that match query access characteristics, optimizing join strategies for distributed data locality, and configuring zones for workload requirements - transforms your platform from a system that fights against its own success to one that leverages scale as a competitive advantage.

Your music streaming platform now delivers the responsive, insight-driven experience that both users and business stakeholders expect. Query optimization doesn't just improve performance - it enables the real-time analytics and personalization features that differentiate successful platforms in competitive markets.

## Next Steps

With high-performance query optimization mastered, you've completed the technical foundation for building production-scale distributed systems with Apache Ignite 3. The journey from traditional database limitations to distributed system capabilities transforms how applications handle data at any scale.

**[Chapter 6: From Learning to Production](../06-primer-conclusion.md)** - Reflect on your complete learning journey through the five core areas of Ignite 3 development and learn how to apply these concepts to your own production systems.
