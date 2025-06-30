# Chapter 3.3: Data Access API Decision Guide

Your mobile app response times degraded after implementing user session management through SQL queries instead of direct key lookups. Meanwhile, your analytics dashboard fails to aggregate sales data efficiently because you're using RecordView operations to load thousands of individual records instead of executing SQL aggregations. These performance problems stem from API selection mismatches that compound under production load.

Modern distributed applications require different data access patterns for different operations. Session lookups need sub-millisecond key-value performance, business logic operations need type-safe object manipulation, and analytics require SQL aggregation capabilities. Ignite 3 provides three distinct APIs optimized for these different patterns, but selecting the wrong API for each operation creates performance bottlenecks that scale poorly.

This chapter demonstrates how to analyze operation characteristics, select optimal APIs, and combine multiple approaches within single applications.

## API Decision Framework

Your operation characteristics determine optimal API choice. Analyze data access patterns, performance requirements, and object complexity to match operations with appropriate APIs.

**KeyValueView Selection Criteria:**
- Primary key available for direct access
- Performance requirements exceed 1000 operations per second
- Simple value operations without complex object relationships
- Caching patterns and session management scenarios

**RecordView Selection Criteria:**
- Complete object context required for business logic
- Type safety and compile-time validation important
- CRUD operations with business rule enforcement
- Development productivity matters more than raw performance

**SQL API Selection Criteria:**
- Complex queries spanning multiple entities
- Analytics and aggregation requirements
- Dynamic query construction needed
- Cross-partition operations unavoidable

**Multi-API Strategy:**
- Combine approaches within single applications for optimal performance
- Use each API where it provides maximum advantage
- Design data access layers that leverage API strengths strategically

Modern distributed applications achieve optimal performance by matching operation characteristics with appropriate API capabilities. The most effective implementations combine all three approaches, selecting APIs based on specific operation requirements rather than architectural uniformity.

## Operation Analysis Framework

Your API selection determines both development efficiency and runtime performance. Each operation type requires different optimization strategies based on data access patterns, object complexity, and performance requirements.

**KeyValueView Operations**: Direct partition access with minimal serialization overhead. Optimal for high-frequency lookups, caching patterns, and simple value operations where you have primary keys.

**RecordView Operations**: Full object serialization with type safety and business logic integration. Optimal for entity management, CRUD operations, and scenarios requiring complete object context.

**SQL Operations**: Query planning and optimization with support for complex joins and aggregations. Optimal for analytics, reporting, and operations spanning multiple entities or requiring aggregation logic.

The wrong API choice creates cascading performance problems. SQL queries for simple key lookups add query planning overhead and network roundtrips. RecordView operations for analytics load excessive data into memory and perform client-side aggregations instead of server-side optimizations.

### KeyValueView: Maximum Performance Access

KeyValueView provides direct partition access with minimal serialization overhead. The API bypasses query planning and object mapping, delivering sub-millisecond operations for primary key lookups. This approach works best when you need maximum performance for simple operations and have primary keys available.

Session management, feature flags, and cache-aside patterns benefit from KeyValueView's direct access model. The API eliminates object instantiation overhead and transmits only key-value pairs, making it ideal for high-frequency operations where performance matters more than object richness.

```java
public class HighPerformanceCache {
    private final KeyValueView<String, String> sessionCache;
    
    public HighPerformanceCache(IgniteClient client) {
        this.sessionCache = client.tables().table("UserSession")
            .keyValueView(String.class, String.class);
    }
    
    // Direct session access - no query planning or object instantiation
    public void storeSession(String sessionId, String userId) {
        sessionCache.put(null, sessionId, userId);  // Sub-millisecond operation
    }
    
    public String getUserSession(String sessionId) {
        return sessionCache.get(null, sessionId);   // Fastest possible lookup
    }
    
    // Feature flag evaluation with minimal overhead
    public boolean isFeatureEnabled(String featureKey) {
        String value = sessionCache.get(null, featureKey);
        return "true".equals(value);
    }
}
```

KeyValueView operations eliminate serialization overhead by transmitting only key-value pairs. Direct partition access bypasses query planning, making these operations optimal for cache-aside patterns and high-frequency lookups where performance requirements exceed object complexity needs.

### RecordView: Type-Safe Entity Operations

RecordView provides full object serialization with compile-time type safety and IDE integration support. The API handles complete object graphs and enables business logic integration, making it optimal for entity management scenarios where object richness matters more than raw performance.

Business entity operations, CRUD workflows, and scenarios requiring complete object context benefit from RecordView's type-safe approach. The API provides compile-time error detection and object validation while maintaining reasonable performance for complex entity operations.

```java
public class BusinessEntityOperations {
    private final RecordView<Customer> customers;
    private final RecordView<Invoice> invoices;
    
    public BusinessEntityOperations(IgniteClient client) {
        this.customers = client.tables().table("Customer").recordView(Customer.class);
        this.invoices = client.tables().table("Invoice").recordView(Invoice.class);
    }
    
    // Full object retrieval with business context
    public Customer getCustomerProfile(Integer customerId) {
        Customer key = new Customer();
        key.setCustomerId(customerId);
        return customers.get(null, key);  // Type-safe, complete object
    }
    
    public void updateCustomerProfile(Customer customer) {
        // Business logic validation with compile-time safety
        if (customer.getEmail() == null || !customer.getEmail().contains("@")) {
            throw new IllegalArgumentException("Invalid email address");
        }
        
        customers.upsert(null, customer);  // Full object update
    }
    
    // Complex business workflows with multiple entities
    public void processOrder(Customer customer, Invoice invoice) {
        // Type-safe operations with business context
        customer.setLastOrderDate(LocalDate.now());
        customers.upsert(null, customer);
        
        invoice.setCustomerId(customer.getCustomerId());
        invoices.upsert(null, invoice);
    }
}
```

RecordView operations provide compile-time type safety and IDE support for refactoring and development efficiency. Full object context enables business logic integration and validation while maintaining performance suitable for entity management operations.

### SQL API: Complex Query Processing

SQL API provides query planning and optimization with support for complex joins, aggregations, and analytical operations. The API handles cross-partition operations and server-side processing, making it optimal for scenarios requiring flexible query capabilities and data aggregation.

Analytics, reporting, and operations spanning multiple entities benefit from SQL's query optimization and aggregation capabilities. The API performs server-side processing to minimize data transfer and supports complex business logic that requires multiple table interactions.

```java
public class AnalyticsOperations {
    private final IgniteSql sql;
    
    public AnalyticsOperations(IgniteClient client) {
        this.sql = client.sql();
    }
    
    // Server-side aggregation with colocation optimization
    public List<ArtistSales> getTopSellingArtists(int limit) {
        String query = """
            SELECT a.Name, COUNT(il.TrackId) as TracksSold, 
                   SUM(il.UnitPrice * il.Quantity) as Revenue
            FROM Artist a
            JOIN Album al ON a.ArtistId = al.ArtistId     -- Colocated join
            JOIN Track t ON al.AlbumId = t.AlbumId        -- Colocated join
            JOIN InvoiceLine il ON t.TrackId = il.TrackId -- Distributed join
            GROUP BY a.ArtistId, a.Name
            ORDER BY Revenue DESC
            LIMIT ?
            """;
        
        ResultSet<SqlRow> result = sql.execute(null, query, limit);
        
        List<ArtistSales> sales = new ArrayList<>();
        while (result.hasNext()) {
            SqlRow row = result.next();
            sales.add(new ArtistSales(
                row.stringValue("Name"),
                row.longValue("TracksSold"),
                row.decimalValue("Revenue")
            ));
        }
        return sales;
    }
    
    // Flexible reporting with dynamic parameters
    public BigDecimal calculateRevenue(LocalDate startDate, LocalDate endDate, String genre) {
        String query = """
            SELECT SUM(il.UnitPrice * il.Quantity) as Revenue
            FROM InvoiceLine il
            JOIN Track t ON il.TrackId = t.TrackId
            JOIN Genre g ON t.GenreId = g.GenreId
            JOIN Invoice i ON il.InvoiceId = i.InvoiceId
            WHERE i.InvoiceDate BETWEEN ? AND ?
            AND (? IS NULL OR g.Name = ?)
            """;
        
        ResultSet<SqlRow> result = sql.execute(null, query, startDate, endDate, genre, genre);
        
        return result.hasNext() ? result.next().decimalValue("Revenue") : BigDecimal.ZERO;
    }
}
```

SQL operations leverage query optimization and colocation awareness to minimize data movement during complex operations. Server-side aggregation and join processing reduce network traffic while providing flexible query capabilities for analytics and reporting scenarios.

Modern applications require multiple API approaches within single workflows. Order processing combines entity management, high-performance lookups, and complex calculations. Dashboard systems need cached metrics, real-time analytics, and session management. These scenarios demonstrate how to combine APIs strategically for optimal performance.

### E-commerce Order Processing

```java
public class OrderProcessingService {
    private final RecordView<Customer> customers;
    private final RecordView<Order> orders;
    private final KeyValueView<String, Boolean> inventory;
    private final IgniteSql sql;
    
    public OrderProcessingService(IgniteClient client) {
        this.customers = client.tables().table("Customer").recordView(Customer.class);
        this.orders = client.tables().table("Order").recordView(Order.class);
        this.inventory = client.tables().table("Inventory")
            .keyValueView(String.class, Boolean.class);
        this.sql = client.sql();
    }
    
    public OrderResult processOrder(Integer customerId, List<OrderItem> items) {
        // RecordView: Customer entity with business logic validation
        Customer customer = getCustomer(customerId);
        if (customer == null) {
            return OrderResult.customerNotFound();
        }
        
        // KeyValueView: High-performance inventory checks
        for (OrderItem item : items) {
            Boolean available = inventory.get(null, item.getProductId());
            if (!Boolean.TRUE.equals(available)) {
                return OrderResult.outOfStock(item.getProductId());
            }
        }
        
        // SQL API: Complex pricing with customer-specific discounts
        BigDecimal totalPrice = calculateOrderTotal(customerId, items);
        
        // RecordView: Order creation with business context
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setTotalPrice(totalPrice);
        order.setOrderDate(LocalDate.now());
        orders.upsert(null, order);
        
        return OrderResult.success(order);
    }
    
    private Customer getCustomer(Integer customerId) {
        Customer key = new Customer();
        key.setCustomerId(customerId);
        return customers.get(null, key);
    }
    
    private BigDecimal calculateOrderTotal(Integer customerId, List<OrderItem> items) {
        // Server-side calculation with customer-specific discounts
        String query = """
            SELECT SUM(p.Price * ? * (1 - COALESCE(cd.DiscountPercent, 0) / 100)) as Total
            FROM Product p
            LEFT JOIN CustomerDiscount cd ON cd.CustomerId = ? AND cd.ProductId = p.ProductId
            WHERE p.ProductId IN (?)
            """;
        
        List<String> productIds = items.stream()
            .map(OrderItem::getProductId)
            .collect(Collectors.toList());
        
        ResultSet<SqlRow> result = sql.execute(null, query, 
            items.size(), customerId, productIds);
        
        return result.hasNext() ? result.next().decimalValue("Total") : BigDecimal.ZERO;
    }
}
```

Order processing combines three API approaches strategically. RecordView handles customer entities with business logic validation. KeyValueView provides high-performance inventory lookups. SQL API calculates complex pricing with customer-specific discounts. Each API handles operations where it provides optimal performance characteristics.

```java
// SQL API: Analytical operations that complement entity management
public List<CustomerMetrics> getTopCustomersByGenre(String genre) {
    String query = sql.statementBuilder()
        .query("SELECT c.CustomerId, c.FirstName, c.LastName, SUM(il.UnitPrice * il.Quantity) as TotalSpent " +
               "FROM Customer c JOIN Invoice i ON c.CustomerId = i.CustomerId " +
               "JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId " +
               "JOIN Track t ON il.TrackId = t.TrackId " +
               "JOIN Genre g ON t.GenreId = g.GenreId " +
               "WHERE g.Name = ? GROUP BY c.CustomerId, c.FirstName, c.LastName " +
               "ORDER BY TotalSpent DESC LIMIT 10")
        .build();
    
    try (ResultSet<SqlRow> rs = sql.execute(null, query, genre)) {
        return extractCustomerMetrics(rs);
    }
}
```

This SQL example demonstrates analytical operations that KeyValueView and RecordView cannot handle efficiently. Complex joins, aggregations, and filtering across multiple tables require SQL's query optimization and server-side processing capabilities.

### Real-Time Dashboard Implementation

```java
public class DashboardService {
    private final KeyValueView<String, String> metricsCache;
    private final IgniteSql sql;
    
    public DashboardService(IgniteClient client) {
        this.metricsCache = client.tables().table("MetricsCache")
            .keyValueView(String.class, String.class);
        this.sql = client.sql();
    }
    
    public DashboardData getDashboardData() {
        DashboardData dashboard = new DashboardData();
        
        // KeyValueView: High-frequency cached metrics for instant access
        dashboard.setActiveUsers(getCachedMetric("active_users"));
        dashboard.setSystemStatus(getCachedMetric("system_status"));
        
        // Hybrid approach: Check cache first, calculate if needed
        String recentSalesKey = "recent_sales_" + LocalDate.now();
        String cachedSales = metricsCache.get(null, recentSalesKey);
        
        if (cachedSales == null) {
            // SQL API: Complex analytics calculation
            BigDecimal sales = calculateRecentSales();
            metricsCache.put(null, recentSalesKey, sales.toString());
            dashboard.setRecentSales(sales);
        } else {
            dashboard.setRecentSales(new BigDecimal(cachedSales));
        }
        
        return dashboard;
    }
    
    private String getCachedMetric(String metricKey) {
        return metricsCache.get(null, metricKey);
    }
    
    private BigDecimal calculateRecentSales() {
        String query = """
            SELECT SUM(Total) as RecentSales
            FROM Invoice
            WHERE InvoiceDate >= ?
            """;
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        ResultSet<SqlRow> result = sql.execute(null, query, yesterday);
        
        return result.hasNext() ? result.next().decimalValue("RecentSales") : BigDecimal.ZERO;
    }
}
```

Dashboard implementation combines caching with analytics. KeyValueView provides instant access to high-frequency metrics like active users and system status. SQL API handles complex calculations when cache misses occur. This hybrid approach delivers consistent performance while maintaining data freshness.

### User Profile Management System

```java
public class UserProfileService {
    private final RecordView<UserProfile> profiles;
    private final KeyValueView<Integer, String> preferencesCache;
    private final IgniteSql sql;
    
    public UserProfileService(IgniteClient client) {
        this.profiles = client.tables().table("UserProfile").recordView(UserProfile.class);
        this.preferencesCache = client.tables().table("UserPreferences")
            .keyValueView(Integer.class, String.class);
        this.sql = client.sql();
    }
    
    public CompleteUserProfile getUserProfile(Integer userId) {
        // RecordView: Complete profile object with business context
        UserProfile profile = getProfile(userId);
        if (profile == null) {
            return null;
        }
        
        // KeyValueView: High-frequency preference access
        String preferences = preferencesCache.get(null, userId);
        
        // SQL API: Analytical activity summary
        UserActivity activity = getUserActivity(userId);
        
        return new CompleteUserProfile(profile, preferences, activity);
    }
    
    public void updateUserPreferences(Integer userId, String preferences) {
        // KeyValueView: Instant preference updates
        preferencesCache.put(null, userId, preferences);
    }
    
    public void updateUserProfile(UserProfile profile) {
        // RecordView: Full object updates with business validation
        if (profile.getEmail() == null || !isValidEmail(profile.getEmail())) {
            throw new IllegalArgumentException("Invalid email");
        }
        
        profiles.upsert(null, profile);
    }
    
    private UserProfile getProfile(Integer userId) {
        UserProfile key = new UserProfile();
        key.setUserId(userId);
        return profiles.get(null, key);
    }
    
    private UserActivity getUserActivity(Integer userId) {
        String query = """
            SELECT COUNT(*) as LoginCount,
                   MAX(LastLogin) as LastLogin,
                   COUNT(DISTINCT DATE(ActivityDate)) as ActiveDays
            FROM UserActivity
            WHERE UserId = ? AND ActivityDate >= ?
            """;
        
        LocalDate monthAgo = LocalDate.now().minusMonths(1);
        ResultSet<SqlRow> result = sql.execute(null, query, userId, monthAgo);
        
        if (result.hasNext()) {
            SqlRow row = result.next();
            return new UserActivity(
                row.longValue("LoginCount"),
                row.dateValue("LastLogin"),
                row.longValue("ActiveDays")
            );
        }
        
        return new UserActivity(0, null, 0);
    }
    
    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }
}
```

User profile management separates concerns by data access pattern. RecordView handles complete profile objects with business validation. KeyValueView provides instant preference access for high-frequency operations. SQL API generates activity summaries with complex aggregations. This separation optimizes each operation type while maintaining consistent user experience.

## Advanced Multi-API Patterns

### Search and Metadata Enrichment

```java
public class OptimizedMusicService {
    private final KeyValueView<Integer, String> artistNameCache;
    private final RecordView<Track> tracks;
    private final IgniteSql sql;
    
    public OptimizedMusicService(IgniteClient client) {
        this.artistNameCache = client.tables().table("Artist")
            .keyValueView(Integer.class, String.class);
        this.tracks = client.tables().table("Track").recordView(Track.class);
        this.sql = client.sql();
    }
    
    public SearchResults searchTracks(String searchTerm, int limit) {
        // SQL API: Complex search with relevance ranking
        String searchQuery = """
            SELECT t.TrackId, t.Name as TrackName, t.ArtistId,
                   RANK() OVER (ORDER BY 
                     CASE WHEN t.Name LIKE ? THEN 1 ELSE 2 END,
                     t.Name
                   ) as Relevance
            FROM Track t
            WHERE t.Name LIKE ?
            ORDER BY Relevance, t.Name
            LIMIT ?
            """;
        
        String pattern = "%" + searchTerm + "%";
        ResultSet<SqlRow> searchResult = sql.execute(null, searchQuery, pattern, pattern, limit);
        
        List<TrackResult> results = new ArrayList<>();
        Set<Integer> artistIds = new HashSet<>();
        
        // Collect search results and extract artist IDs
        while (searchResult.hasNext()) {
            SqlRow row = searchResult.next();
            
            int trackId = row.intValue("TrackId");
            String trackName = row.stringValue("TrackName");
            int artistId = row.intValue("ArtistId");
            
            results.add(new TrackResult(trackId, trackName, artistId, null));
            artistIds.add(artistId);
        }
        
        // KeyValueView: Batch lookup for artist names
        Map<Integer, String> artistNames = artistNameCache.getAll(null, artistIds);
        
        // Enrich results with cached artist names
        for (TrackResult result : results) {
            result.setArtistName(artistNames.get(result.getArtistId()));
        }
        
        return new SearchResults(results);
    }
    
    public PlaylistDetails getPlaylistWithMetadata(Integer playlistId) {
        // SQL API: Playlist structure with join optimization
        String playlistQuery = """
            SELECT p.Name as PlaylistName, t.TrackId, t.Name as TrackName,
                   t.Milliseconds, a.Name as ArtistName
            FROM Playlist p
            JOIN PlaylistTrack pt ON p.PlaylistId = pt.PlaylistId
            JOIN Track t ON pt.TrackId = t.TrackId
            JOIN Album al ON t.AlbumId = al.AlbumId
            JOIN Artist a ON al.ArtistId = a.ArtistId
            WHERE p.PlaylistId = ?
            ORDER BY pt.TrackOrder
            """;
        
        ResultSet<SqlRow> playlistResult = sql.execute(null, playlistQuery, playlistId);
        
        String playlistName = null;
        List<TrackDetails> trackDetails = new ArrayList<>();
        Set<Integer> trackIds = new HashSet<>();
        
        while (playlistResult.hasNext()) {
            SqlRow row = playlistResult.next();
            
            if (playlistName == null) {
                playlistName = row.stringValue("PlaylistName");
            }
            
            int trackId = row.intValue("TrackId");
            trackIds.add(trackId);
            
            trackDetails.add(new TrackDetails(
                trackId,
                row.stringValue("TrackName"),
                row.stringValue("ArtistName"),
                row.intValue("Milliseconds")
            ));
        }
        
        // RecordView: Additional metadata for complete track objects
        Collection<Track> trackKeys = trackIds.stream()
            .map(id -> {
                Track key = new Track();
                key.setTrackId(id);
                return key;
            })
            .collect(Collectors.toList());
        
        Collection<Track> fullTracks = tracks.getAll(null, trackKeys);
        
        // Combine SQL results with RecordView metadata
        Map<Integer, Track> trackMap = fullTracks.stream()
            .collect(Collectors.toMap(Track::getTrackId, t -> t));
        
        for (TrackDetails detail : trackDetails) {
            Track fullTrack = trackMap.get(detail.getTrackId());
            if (fullTrack != null) {
                detail.setUnitPrice(fullTrack.getUnitPrice());
                detail.setGenreId(fullTrack.getGenreId());
            }
        }
        
        return new PlaylistDetails(playlistName, trackDetails);
    }
}
```

Advanced patterns combine multiple APIs within single operations for optimal performance. Search operations use SQL for complex ranking while leveraging KeyValueView for metadata enrichment. Playlist operations combine SQL joins with RecordView batch lookups to minimize network roundtrips while maximizing data completeness.

## Performance Anti-Patterns

Your API selection mistakes create performance bottlenecks that compound under load. Using SQL for simple key lookups adds unnecessary query planning overhead. Using RecordView for analytics forces client-side aggregation of massive datasets. These anti-patterns demonstrate common mismatches between operation requirements and API capabilities.

```java
public class APISelectionAntiPatterns {
    
    // ANTI-PATTERN: SQL queries for simple key lookups
    public String getArtistNameInefficient(IgniteSql sql, Integer artistId) {
        // Adds query planning overhead for simple operation
        ResultSet<SqlRow> result = sql.execute(null, 
            "SELECT Name FROM Artist WHERE ArtistId = ?", artistId);
        return result.hasNext() ? result.next().stringValue("Name") : null;
    }
    
    // OPTIMIZED: Direct key access for maximum performance
    public String getArtistNameOptimized(KeyValueView<Integer, String> artistNames, Integer artistId) {
        return artistNames.get(null, artistId);  // Sub-millisecond access
    }
    
    // ANTI-PATTERN: RecordView for large-scale analytics
    public BigDecimal calculateTotalRevenueInefficient(RecordView<InvoiceLine> invoiceLines) {
        // Forces massive data transfer and client-side aggregation
        Collection<InvoiceLine> allLines = invoiceLines.getAll(null, getAllKeys());
        return allLines.stream()
            .map(line -> line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    // OPTIMIZED: Server-side aggregation with SQL
    public BigDecimal calculateTotalRevenueOptimized(IgniteSql sql) {
        ResultSet<SqlRow> result = sql.execute(null,
            "SELECT SUM(UnitPrice * Quantity) as Total FROM InvoiceLine");
        return result.hasNext() ? result.next().decimalValue("Total") : BigDecimal.ZERO;
    }
}
```

These anti-patterns create performance problems that scale poorly under production load. SQL queries for simple lookups waste processing cycles on unnecessary query planning. RecordView analytics operations transfer excessive data across the network and perform aggregations that should execute server-side.

## Implementation Guidelines

Your API selection directly impacts application performance, development productivity, and operational complexity. Choose APIs that match operation characteristics while considering long-term maintenance and scaling requirements.

**Next Steps**: **[Module 4: Distributed Operations](../04-distributed-operations/01-transaction-fundamentals.md)** demonstrates how data access patterns integrate with transaction management, distributed processing, and consistency guarantees across multiple nodes.
