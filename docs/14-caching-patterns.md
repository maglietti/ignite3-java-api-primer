# 14. Caching Patterns

This section provides comprehensive guidance on implementing caching patterns with Apache Ignite 3, including migration strategies from Ignite 2 and integration with popular caching frameworks.

## 14.1 Caching Fundamentals in Ignite 3

### Understanding Caching in Ignite 3

Apache Ignite 3 represents a paradigm shift from Ignite 2's cache-centric API to a table-centric approach. While Ignite 2 provided dedicated `IgniteCache` APIs, Ignite 3 achieves caching through its unified Table API, offering more flexibility and better SQL integration.

#### Key Differences from Ignite 2

```java
// Ignite 2 Approach
IgniteCache<String, Customer> cache = ignite.cache("customerCache");
Customer customer = cache.get("customer123");
cache.put("customer123", updatedCustomer);

// Ignite 3 Approach
RecordView<Customer> customerView = client.tables().table("Customer").recordView(Customer.class);
Customer key = new Customer(); key.setId("customer123");
Customer customer = customerView.get(null, key);
customerView.upsert(null, updatedCustomer);
```

#### When to Use Ignite 3 for Caching

**Ideal Use Cases:**

- **Distributed Caching**: Shared cache across multiple application instances
- **Session Storage**: Web session management in clustered environments
- **Reference Data**: Configuration, lookup tables, and reference data caching
- **Computed Results**: Expensive calculation results with TTL requirements
- **Database Off-loading**: Reducing database load through intelligent caching

**Performance Characteristics:**

- **Latency**: Sub-millisecond access for local data, low-millisecond for remote
- **Throughput**: Hundreds of thousands of operations per second per node
- **Scalability**: Linear scalability with cluster size
- **Consistency**: Strong consistency with ACID transactions
- **Durability**: Optional persistence with configurable backup strategies

### Cache Architecture Patterns

#### Pattern 1: Simple Key-Value Cache

```java
@Table(zone = @Zone(value = "cache_zone", storageProfiles = "default"))
public class CacheEntry {
    @Id
    @Column(value = "cache_key", nullable = false, length = 255)
    private String key;
    
    @Column(value = "cache_value", nullable = true)
    private byte[] value;  // Serialized object data
    
    @Column(value = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(value = "expires_at", nullable = true)
    private LocalDateTime expiresAt;
    
    @Column(value = "access_count", nullable = false)
    private Long accessCount = 0L;
    
    @Column(value = "last_accessed", nullable = false)
    private LocalDateTime lastAccessed;
    
    // Constructors, getters, setters...
    public CacheEntry() {}
    
    public CacheEntry(String key, byte[] value, LocalDateTime expiresAt) {
        this.key = key;
        this.value = value;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.lastAccessed = LocalDateTime.now();
        this.accessCount = 0L;
    }
    
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    // Getters and setters...
}
```

#### Pattern 2: Typed Domain Cache

```java
@Table(
    zone = @Zone(value = "customer_cache_zone", storageProfiles = "default"),
    indexes = {
        @Index(value = "idx_customer_email", columns = { @ColumnRef("email") }),
        @Index(value = "idx_customer_last_accessed", columns = { @ColumnRef("lastAccessed") })
    }
)
public class CustomerCache {
    @Id
    @Column(value = "customer_id", nullable = false)
    private String customerId;
    
    @Column(value = "first_name", nullable = false, length = 50)
    private String firstName;
    
    @Column(value = "last_name", nullable = false, length = 50)
    private String lastName;
    
    @Column(value = "email", nullable = false, length = 100)
    private String email;
    
    @Column(value = "phone", nullable = true, length = 20)
    private String phone;
    
    @Column(value = "cached_at", nullable = false)
    private LocalDateTime cachedAt;
    
    @Column(value = "expires_at", nullable = true)
    private LocalDateTime expiresAt;
    
    @Column(value = "last_accessed", nullable = false)
    private LocalDateTime lastAccessed;
    
    @Column(value = "access_count", nullable = false)
    private Long accessCount = 0L;
    
    // Constructors, getters, setters...
    public CustomerCache() {}
    
    public CustomerCache(String customerId, String firstName, String lastName, 
                        String email, String phone, Duration ttl) {
        this.customerId = customerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.cachedAt = LocalDateTime.now();
        this.lastAccessed = LocalDateTime.now();
        this.accessCount = 0L;
        
        if (ttl != null) {
            this.expiresAt = LocalDateTime.now().plus(ttl);
        }
    }
    
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    public void recordAccess() {
        this.lastAccessed = LocalDateTime.now();
        this.accessCount++;
    }
    
    // Getters and setters...
}
```

## 14.2 Cache Implementation Patterns

### Cache-Aside Pattern (Lazy Loading)

The cache-aside pattern is the most common caching strategy where the application manages the cache directly.

```java
public class CacheAsideService<K, V> {
    private final IgniteClient igniteClient;
    private final RecordView<CacheEntry> cacheView;
    private final Function<K, String> keyMapper;
    private final Function<V, byte[]> valueSerializer;
    private final Function<byte[], V> valueDeserializer;
    private final Function<K, V> dataLoader; // Function to load from primary data source
    
    public CacheAsideService(IgniteClient igniteClient,
                            Function<K, String> keyMapper,
                            Function<V, byte[]> valueSerializer,
                            Function<byte[], V> valueDeserializer,
                            Function<K, V> dataLoader) {
        this.igniteClient = igniteClient;
        this.cacheView = igniteClient.tables().table("CacheEntry").recordView(CacheEntry.class);
        this.keyMapper = keyMapper;
        this.valueSerializer = valueSerializer;
        this.valueDeserializer = valueDeserializer;
        this.dataLoader = dataLoader;
    }
    
    // Cache-aside read operation
    public V get(K key) {
        String cacheKey = keyMapper.apply(key);
        
        // 1. Try to get from cache
        CacheEntry keyEntry = new CacheEntry();
        keyEntry.setKey(cacheKey);
        CacheEntry cached = cacheView.get(null, keyEntry);
        
        if (cached != null && !cached.isExpired()) {
            // Cache hit - update access statistics and return
            cached.recordAccess();
            cacheView.upsert(null, cached);
            return valueDeserializer.apply(cached.getValue());
        }
        
        // 2. Cache miss - load from primary data source
        V value = dataLoader.apply(key);
        
        if (value != null) {
            // 3. Store in cache for future access
            put(key, value, Duration.ofHours(1)); // 1 hour TTL
        }
        
        return value;
    }
    
    // Cache-aside write operation
    public void put(K key, V value, Duration ttl) {
        String cacheKey = keyMapper.apply(key);
        byte[] serializedValue = valueSerializer.apply(value);
        LocalDateTime expiresAt = ttl != null ? LocalDateTime.now().plus(ttl) : null;
        
        CacheEntry entry = new CacheEntry(cacheKey, serializedValue, expiresAt);
        cacheView.upsert(null, entry);
    }
    
    // Invalidate cache entry
    public void evict(K key) {
        String cacheKey = keyMapper.apply(key);
        CacheEntry keyEntry = new CacheEntry();
        keyEntry.setKey(cacheKey);
        cacheView.delete(null, keyEntry);
    }
    
    // Bulk operations for better performance
    public Map<K, V> getAll(Collection<K> keys) {
        List<String> cacheKeys = keys.stream()
            .map(keyMapper)
            .collect(Collectors.toList());
        
        // Build SQL query for bulk retrieval
        String placeholders = cacheKeys.stream()
            .map(k -> "?")
            .collect(Collectors.joining(","));
        
        String sql = "SELECT cache_key, cache_value FROM CacheEntry WHERE cache_key IN (" + placeholders + ") AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)";
        
        Map<String, V> results = new HashMap<>();
        
        try (ResultSet<SqlRow> resultSet = igniteClient.sql().execute(null, sql, cacheKeys.toArray())) {
            while (resultSet.hasNext()) {
                SqlRow row = resultSet.next();
                String cacheKey = row.stringValue("cache_key");
                byte[] value = row.binaryValue("cache_value");
                results.put(cacheKey, valueDeserializer.apply(value));
            }
        }
        
        // Convert back to original key type
        Map<K, V> finalResults = new HashMap<>();
        for (K key : keys) {
            String cacheKey = keyMapper.apply(key);
            if (results.containsKey(cacheKey)) {
                finalResults.put(key, results.get(cacheKey));
            }
        }
        
        return finalResults;
    }
}
```

### Write-Through Pattern

In write-through caching, data is written to both the cache and the underlying data store synchronously.

```java
public class WriteThroughCacheService<K, V> {
    private final IgniteClient igniteClient;
    private final RecordView<CacheEntry> cacheView;
    private final Function<K, String> keyMapper;
    private final Function<V, byte[]> valueSerializer;
    private final Function<byte[], V> valueDeserializer;
    private final BiConsumer<K, V> dataWriter; // Function to write to primary data source
    private final Function<K, V> dataLoader;
    
    public WriteThroughCacheService(IgniteClient igniteClient,
                                   Function<K, String> keyMapper,
                                   Function<V, byte[]> valueSerializer,
                                   Function<byte[], V> valueDeserializer,
                                   BiConsumer<K, V> dataWriter,
                                   Function<K, V> dataLoader) {
        this.igniteClient = igniteClient;
        this.cacheView = igniteClient.tables().table("CacheEntry").recordView(CacheEntry.class);
        this.keyMapper = keyMapper;
        this.valueSerializer = valueSerializer;
        this.valueDeserializer = valueDeserializer;
        this.dataWriter = dataWriter;
        this.dataLoader = dataLoader;
    }
    
    // Write-through operation
    public void put(K key, V value, Duration ttl) {
        try {
            igniteClient.transactions().runInTransaction(tx -> {
                // 1. Write to primary data source first
                dataWriter.accept(key, value);
                
                // 2. Write to cache
                String cacheKey = keyMapper.apply(key);
                byte[] serializedValue = valueSerializer.apply(value);
                LocalDateTime expiresAt = ttl != null ? LocalDateTime.now().plus(ttl) : null;
                
                CacheEntry entry = new CacheEntry(cacheKey, serializedValue, expiresAt);
                cacheView.upsert(tx, entry);
            });
        } catch (Exception e) {
            throw new RuntimeException("Write-through operation failed", e);
        }
    }
    
    // Read operation with cache-aside fallback
    public V get(K key) {
        String cacheKey = keyMapper.apply(key);
        
        // Try cache first
        CacheEntry keyEntry = new CacheEntry();
        keyEntry.setKey(cacheKey);
        CacheEntry cached = cacheView.get(null, keyEntry);
        
        if (cached != null && !cached.isExpired()) {
            cached.recordAccess();
            cacheView.upsert(null, cached);
            return valueDeserializer.apply(cached.getValue());
        }
        
        // Cache miss - load from primary source and cache
        V value = dataLoader.apply(key);
        if (value != null) {
            // Cache the loaded value
            String serializedKey = keyMapper.apply(key);
            byte[] serializedValue = valueSerializer.apply(value);
            CacheEntry entry = new CacheEntry(serializedKey, serializedValue, LocalDateTime.now().plusHours(1));
            cacheView.upsert(null, entry);
        }
        
        return value;
    }
    
    // Delete operation
    public void delete(K key) {
        try {
            igniteClient.transactions().runInTransaction(tx -> {
                // 1. Delete from primary data source
                // dataDeleter.accept(key); // Implement based on your data source
                
                // 2. Remove from cache
                String cacheKey = keyMapper.apply(key);
                CacheEntry keyEntry = new CacheEntry();
                keyEntry.setKey(cacheKey);
                cacheView.delete(tx, keyEntry);
            });
        } catch (Exception e) {
            throw new RuntimeException("Write-through delete operation failed", e);
        }
    }
}
```

### Write-Behind Pattern (Write-Back)

Write-behind caching improves performance by writing to cache immediately and to the data store asynchronously.

```java
public class WriteBehindCacheService<K, V> {
    private final IgniteClient igniteClient;
    private final RecordView<CacheEntry> cacheView;
    private final RecordView<PendingWrite> pendingWritesView;
    private final Function<K, String> keyMapper;
    private final Function<V, byte[]> valueSerializer;
    private final Function<byte[], V> valueDeserializer;
    private final BiConsumer<K, V> dataWriter;
    private final ScheduledExecutorService writeScheduler;
    
    // Table for tracking pending writes
    @Table(zone = @Zone(value = "cache_zone", storageProfiles = "default"))
    public static class PendingWrite {
        @Id
        @Column(value = "cache_key", nullable = false)
        private String cacheKey;
        
        @Column(value = "operation_type", nullable = false)
        private String operationType; // PUT, DELETE
        
        @Column(value = "pending_since", nullable = false)
        private LocalDateTime pendingSince;
        
        @Column(value = "retry_count", nullable = false)
        private Integer retryCount = 0;
        
        // Constructors, getters, setters...
    }
    
    public WriteBehindCacheService(IgniteClient igniteClient,
                                  Function<K, String> keyMapper,
                                  Function<V, byte[]> valueSerializer,
                                  Function<byte[], V> valueDeserializer,
                                  BiConsumer<K, V> dataWriter) {
        this.igniteClient = igniteClient;
        this.cacheView = igniteClient.tables().table("CacheEntry").recordView(CacheEntry.class);
        this.pendingWritesView = igniteClient.tables().table("PendingWrite").recordView(PendingWrite.class);
        this.keyMapper = keyMapper;
        this.valueSerializer = valueSerializer;
        this.valueDeserializer = valueDeserializer;
        this.dataWriter = dataWriter;
        this.writeScheduler = Executors.newScheduledThreadPool(2);
        
        // Start background write processor
        startWriteBehindProcessor();
    }
    
    // Immediate cache write, async data store write
    public void put(K key, V value, Duration ttl) {
        String cacheKey = keyMapper.apply(key);
        
        try {
            igniteClient.transactions().runInTransaction(tx -> {
                // 1. Write to cache immediately
                byte[] serializedValue = valueSerializer.apply(value);
                LocalDateTime expiresAt = ttl != null ? LocalDateTime.now().plus(ttl) : null;
                CacheEntry entry = new CacheEntry(cacheKey, serializedValue, expiresAt);
                cacheView.upsert(tx, entry);
                
                // 2. Queue for background write to data store
                PendingWrite pendingWrite = new PendingWrite();
                pendingWrite.setCacheKey(cacheKey);
                pendingWrite.setOperationType("PUT");
                pendingWrite.setPendingSince(LocalDateTime.now());
                pendingWritesView.upsert(tx, pendingWrite);
            });
        } catch (Exception e) {
            throw new RuntimeException("Write-behind cache operation failed", e);
        }
    }
    
    // Read from cache (same as cache-aside)
    public V get(K key) {
        String cacheKey = keyMapper.apply(key);
        
        CacheEntry keyEntry = new CacheEntry();
        keyEntry.setKey(cacheKey);
        CacheEntry cached = cacheView.get(null, keyEntry);
        
        if (cached != null && !cached.isExpired()) {
            cached.recordAccess();
            cacheView.upsert(null, cached);
            return valueDeserializer.apply(cached.getValue());
        }
        
        return null; // For write-behind, we typically don't load from data store on miss
    }
    
    // Background processor for pending writes
    private void startWriteBehindProcessor() {
        writeScheduler.scheduleAtFixedRate(this::processPendingWrites, 5, 5, TimeUnit.SECONDS);
    }
    
    private void processPendingWrites() {
        try {
            // Get pending writes
            String sql = "SELECT cache_key, operation_type FROM PendingWrite WHERE retry_count < 3 ORDER BY pending_since LIMIT 100";
            
            try (ResultSet<SqlRow> resultSet = igniteClient.sql().execute(null, sql)) {
                while (resultSet.hasNext()) {
                    SqlRow row = resultSet.next();
                    String cacheKey = row.stringValue("cache_key");
                    String operationType = row.stringValue("operation_type");
                    
                    try {
                        if ("PUT".equals(operationType)) {
                            processPendingPut(cacheKey);
                        } else if ("DELETE".equals(operationType)) {
                            processPendingDelete(cacheKey);
                        }
                        
                        // Remove from pending writes on success
                        PendingWrite keyEntry = new PendingWrite();
                        keyEntry.setCacheKey(cacheKey);
                        pendingWritesView.delete(null, keyEntry);
                        
                    } catch (Exception e) {
                        // Increment retry count
                        System.err.println("Failed to process pending write for key " + cacheKey + ": " + e.getMessage());
                        incrementRetryCount(cacheKey);
                    }
                }\n            }\n        } catch (Exception e) {\n            System.err.println("Error processing pending writes: " + e.getMessage());\n        }\n    }\n    \n    private void processPendingPut(String cacheKey) {\n        // Get current value from cache\n        CacheEntry keyEntry = new CacheEntry();\n        keyEntry.setKey(cacheKey);\n        CacheEntry cached = cacheView.get(null, keyEntry);\n        \n        if (cached != null && cached.getValue() != null) {\n            // Deserialize and write to data store\n            V value = valueDeserializer.apply(cached.getValue());\n            // Convert cache key back to original key type for data writer\n            // This is a simplified approach - in practice you'd need proper key mapping\n            K originalKey = (K) cacheKey; // Simplified - implement proper reverse mapping\n            dataWriter.accept(originalKey, value);\n        }\n    }\n    \n    private void processPendingDelete(String cacheKey) {\n        // Implement delete operation to data store\n        // dataDeleter.accept(cacheKey);\n    }\n    \n    private void incrementRetryCount(String cacheKey) {\n        try {\n            igniteClient.sql().execute(null, \n                "UPDATE PendingWrite SET retry_count = retry_count + 1 WHERE cache_key = ?", \n                cacheKey);\n        } catch (Exception e) {\n            System.err.println("Failed to increment retry count: " + e.getMessage());\n        }\n    }\n}\n```\n\n### Read-Through Pattern\n\nRead-through caching automatically loads data from the backing store when a cache miss occurs.\n\n```java\npublic class ReadThroughCacheService<K, V> {\n    private final IgniteClient igniteClient;\n    private final RecordView<CacheEntry> cacheView;\n    private final Function<K, String> keyMapper;\n    private final Function<V, byte[]> valueSerializer;\n    private final Function<byte[], V> valueDeserializer;\n    private final Function<K, V> dataLoader;\n    private final Duration defaultTtl;\n    \n    public ReadThroughCacheService(IgniteClient igniteClient,\n                                  Function<K, String> keyMapper,\n                                  Function<V, byte[]> valueSerializer,\n                                  Function<byte[], V> valueDeserializer,\n                                  Function<K, V> dataLoader,\n                                  Duration defaultTtl) {\n        this.igniteClient = igniteClient;\n        this.cacheView = igniteClient.tables().table("CacheEntry").recordView(CacheEntry.class);\n        this.keyMapper = keyMapper;\n        this.valueSerializer = valueSerializer;\n        this.valueDeserializer = valueDeserializer;\n        this.dataLoader = dataLoader;\n        this.defaultTtl = defaultTtl;\n    }\n    \n    // Read-through get operation\n    public V get(K key) {\n        String cacheKey = keyMapper.apply(key);\n        \n        // Try cache first\n        CacheEntry keyEntry = new CacheEntry();\n        keyEntry.setKey(cacheKey);\n        CacheEntry cached = cacheView.get(null, keyEntry);\n        \n        if (cached != null && !cached.isExpired()) {\n            // Cache hit\n            cached.recordAccess();\n            cacheView.upsert(null, cached);\n            return valueDeserializer.apply(cached.getValue());\n        }\n        \n        // Cache miss - load through the cache\n        try {\n            return igniteClient.transactions().runInTransaction(tx -> {\n                // Double-check locking pattern in case another thread loaded it\n                CacheEntry doubleCheck = cacheView.get(tx, keyEntry);\n                if (doubleCheck != null && !doubleCheck.isExpired()) {\n                    doubleCheck.recordAccess();\n                    cacheView.upsert(tx, doubleCheck);\n                    return valueDeserializer.apply(doubleCheck.getValue());\n                }\n                \n                // Load from data source\n                V value = dataLoader.apply(key);\n                \n                if (value != null) {\n                    // Store in cache\n                    byte[] serializedValue = valueSerializer.apply(value);\n                    LocalDateTime expiresAt = defaultTtl != null ? LocalDateTime.now().plus(defaultTtl) : null;\n                    CacheEntry newEntry = new CacheEntry(cacheKey, serializedValue, expiresAt);\n                    cacheView.upsert(tx, newEntry);\n                }\n                \n                return value;\n            });\n        } catch (Exception e) {\n            throw new RuntimeException("Read-through operation failed for key: " + key, e);\n        }\n    }\n    \n    // Bulk read-through operation\n    public Map<K, V> getAll(Collection<K> keys) {\n        Map<K, V> result = new HashMap<>();\n        List<K> cacheMisses = new ArrayList<>();\n        \n        // First, try to get all from cache\n        for (K key : keys) {\n            String cacheKey = keyMapper.apply(key);\n            CacheEntry keyEntry = new CacheEntry();\n            keyEntry.setKey(cacheKey);\n            CacheEntry cached = cacheView.get(null, keyEntry);\n            \n            if (cached != null && !cached.isExpired()) {\n                result.put(key, valueDeserializer.apply(cached.getValue()));\n                \n                // Update access statistics\n                cached.recordAccess();\n                cacheView.upsert(null, cached);\n            } else {\n                cacheMisses.add(key);\n            }\n        }\n        \n        // Load cache misses\n        if (!cacheMisses.isEmpty()) {\n            Map<K, V> loaded = loadAndCacheMultiple(cacheMisses);\n            result.putAll(loaded);\n        }\n        \n        return result;\n    }\n    \n    private Map<K, V> loadAndCacheMultiple(List<K> keys) {\n        Map<K, V> loaded = new HashMap<>();\n        \n        try {\n            igniteClient.transactions().runInTransaction(tx -> {\n                for (K key : keys) {\n                    try {\n                        V value = dataLoader.apply(key);\n                        if (value != null) {\n                            loaded.put(key, value);\n                            \n                            // Cache the loaded value\n                            String cacheKey = keyMapper.apply(key);\n                            byte[] serializedValue = valueSerializer.apply(value);\n                            LocalDateTime expiresAt = defaultTtl != null ? LocalDateTime.now().plus(defaultTtl) : null;\n                            CacheEntry entry = new CacheEntry(cacheKey, serializedValue, expiresAt);\n                            cacheView.upsert(tx, entry);\n                        }\n                    } catch (Exception e) {\n                        System.err.println("Failed to load key " + key + ": " + e.getMessage());\n                    }\n                }\n            });\n        } catch (Exception e) {\n            throw new RuntimeException("Bulk read-through operation failed", e);\n        }\n        \n        return loaded;\n    }\n}\n```\n\n## 14.3 Cache Abstraction Layer\n\n### Generic Cache Interface\n\n```java\npublic interface DistributedCache<K, V> {\n    \n    // Basic operations\n    V get(K key);\n    void put(K key, V value);\n    void put(K key, V value, Duration ttl);\n    boolean putIfAbsent(K key, V value);\n    boolean putIfAbsent(K key, V value, Duration ttl);\n    void evict(K key);\n    void clear();\n    \n    // Bulk operations\n    Map<K, V> getAll(Collection<K> keys);\n    void putAll(Map<K, V> entries);\n    void putAll(Map<K, V> entries, Duration ttl);\n    void evictAll(Collection<K> keys);\n    \n    // Conditional operations\n    boolean replace(K key, V value);\n    boolean replace(K key, V oldValue, V newValue);\n    V getAndReplace(K key, V value);\n    boolean remove(K key, V value);\n    V getAndRemove(K key);\n    \n    // Async operations\n    CompletableFuture<V> getAsync(K key);\n    CompletableFuture<Void> putAsync(K key, V value);\n    CompletableFuture<Void> putAsync(K key, V value, Duration ttl);\n    CompletableFuture<Void> evictAsync(K key);\n    \n    // Statistics and management\n    CacheStats getStats();\n    void refresh(K key);\n    void refresh(Collection<K> keys);\n    boolean containsKey(K key);\n    long size();\n    \n    // Cache-specific operations\n    void expire(K key, Duration ttl);\n    Duration getTimeToLive(K key);\n    LocalDateTime getCreationTime(K key);\n    LocalDateTime getLastAccessTime(K key);\n    long getAccessCount(K key);\n}\n\n// Statistics interface\npublic interface CacheStats {\n    long getHitCount();\n    long getMissCount();\n    double getHitRate();\n    long getEvictionCount();\n    long getSize();\n    Duration getAverageLoadTime();\n    long getTotalLoadTime();\n}\n```\n\n### Ignite 3 Cache Implementation\n\n```java\npublic class IgniteDistributedCache<K, V> implements DistributedCache<K, V> {\n    private final IgniteClient igniteClient;\n    private final RecordView<CacheEntry> cacheView;\n    private final Function<K, String> keySerializer;\n    private final Function<String, K> keyDeserializer;\n    private final Function<V, byte[]> valueSerializer;\n    private final Function<byte[], V> valueDeserializer;\n    private final String cacheName;\n    private final Duration defaultTtl;\n    private final CacheStatsImpl stats;\n    \n    public IgniteDistributedCache(IgniteClient igniteClient,\n                                 String cacheName,\n                                 Function<K, String> keySerializer,\n                                 Function<String, K> keyDeserializer,\n                                 Function<V, byte[]> valueSerializer,\n                                 Function<byte[], V> valueDeserializer,\n                                 Duration defaultTtl) {\n        this.igniteClient = igniteClient;\n        this.cacheView = igniteClient.tables().table("CacheEntry").recordView(CacheEntry.class);\n        this.keySerializer = keySerializer;\n        this.keyDeserializer = keyDeserializer;\n        this.valueSerializer = valueSerializer;\n        this.valueDeserializer = valueDeserializer;\n        this.cacheName = cacheName;\n        this.defaultTtl = defaultTtl;\n        this.stats = new CacheStatsImpl();\n    }\n    \n    @Override\n    public V get(K key) {\n        long startTime = System.nanoTime();\n        \n        try {\n            String cacheKey = buildCacheKey(key);\n            CacheEntry keyEntry = new CacheEntry();\n            keyEntry.setKey(cacheKey);\n            CacheEntry cached = cacheView.get(null, keyEntry);\n            \n            if (cached != null && !cached.isExpired()) {\n                // Update access statistics\n                cached.recordAccess();\n                cacheView.upsert(null, cached);\n                \n                stats.recordHit();\n                return valueDeserializer.apply(cached.getValue());\n            } else {\n                stats.recordMiss();\n                return null;\n            }\n        } finally {\n            stats.recordLoadTime(System.nanoTime() - startTime);\n        }\n    }\n    \n    @Override\n    public void put(K key, V value) {\n        put(key, value, defaultTtl);\n    }\n    \n    @Override\n    public void put(K key, V value, Duration ttl) {\n        String cacheKey = buildCacheKey(key);\n        byte[] serializedValue = valueSerializer.apply(value);\n        LocalDateTime expiresAt = ttl != null ? LocalDateTime.now().plus(ttl) : null;\n        \n        CacheEntry entry = new CacheEntry(cacheKey, serializedValue, expiresAt);\n        cacheView.upsert(null, entry);\n    }\n    \n    @Override\n    public boolean putIfAbsent(K key, V value, Duration ttl) {\n        try {\n            return igniteClient.transactions().runInTransaction(tx -> {\n                String cacheKey = buildCacheKey(key);\n                CacheEntry keyEntry = new CacheEntry();\n                keyEntry.setKey(cacheKey);\n                CacheEntry existing = cacheView.get(tx, keyEntry);\n                \n                if (existing != null && !existing.isExpired()) {\n                    return false; // Key already exists\n                }\n                \n                // Insert new entry\n                byte[] serializedValue = valueSerializer.apply(value);\n                LocalDateTime expiresAt = ttl != null ? LocalDateTime.now().plus(ttl) : null;\n                CacheEntry entry = new CacheEntry(cacheKey, serializedValue, expiresAt);\n                cacheView.upsert(tx, entry);\n                \n                return true;\n            });\n        } catch (Exception e) {\n            throw new RuntimeException("putIfAbsent operation failed", e);\n        }\n    }\n    \n    @Override\n    public void evict(K key) {\n        String cacheKey = buildCacheKey(key);\n        CacheEntry keyEntry = new CacheEntry();\n        keyEntry.setKey(cacheKey);\n        boolean deleted = cacheView.delete(null, keyEntry);\n        \n        if (deleted) {\n            stats.recordEviction();\n        }\n    }\n    \n    @Override\n    public Map<K, V> getAll(Collection<K> keys) {\n        if (keys.isEmpty()) {\n            return Collections.emptyMap();\n        }\n        \n        List<String> cacheKeys = keys.stream()\n            .map(this::buildCacheKey)\n            .collect(Collectors.toList());\n        \n        String placeholders = cacheKeys.stream()\n            .map(k -> "?")\n            .collect(Collectors.joining(","));\n        \n        String sql = "SELECT cache_key, cache_value FROM CacheEntry WHERE cache_key IN (" + placeholders + \") AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)";\n        \n        Map<K, V> result = new HashMap<>();\n        \n        try (ResultSet<SqlRow> resultSet = igniteClient.sql().execute(null, sql, cacheKeys.toArray())) {\n            while (resultSet.hasNext()) {\n                SqlRow row = resultSet.next();\n                String cacheKey = row.stringValue("cache_key");\n                byte[] value = row.binaryValue("cache_value");\n                \n                K originalKey = extractOriginalKey(cacheKey);\n                V deserializedValue = valueDeserializer.apply(value);\n                result.put(originalKey, deserializedValue);\n                \n                stats.recordHit();\n            }\n        }\n        \n        // Record misses\n        int misses = keys.size() - result.size();\n        for (int i = 0; i < misses; i++) {\n            stats.recordMiss();\n        }\n        \n        return result;\n    }\n    \n    @Override\n    public CompletableFuture<V> getAsync(K key) {\n        return CompletableFuture.supplyAsync(() -> get(key));\n    }\n    \n    @Override\n    public CompletableFuture<Void> putAsync(K key, V value, Duration ttl) {\n        return CompletableFuture.runAsync(() -> put(key, value, ttl));\n    }\n    \n    @Override\n    public long size() {\n        String sql = "SELECT COUNT(*) as count FROM CacheEntry WHERE cache_key LIKE ? AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)";\n        String pattern = cacheName + ":%";\n        \n        try (ResultSet<SqlRow> resultSet = igniteClient.sql().execute(null, sql, pattern)) {\n            if (resultSet.hasNext()) {\n                return resultSet.next().longValue("count");\n            }\n        }\n        \n        return 0;\n    }\n    \n    @Override\n    public void clear() {\n        String sql = "DELETE FROM CacheEntry WHERE cache_key LIKE ?";\n        String pattern = cacheName + ":%";\n        igniteClient.sql().execute(null, sql, pattern);\n    }\n    \n    @Override\n    public boolean containsKey(K key) {\n        String cacheKey = buildCacheKey(key);\n        String sql = "SELECT 1 FROM CacheEntry WHERE cache_key = ? AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)";\n        \n        try (ResultSet<SqlRow> resultSet = igniteClient.sql().execute(null, sql, cacheKey)) {\n            return resultSet.hasNext();\n        }\n    }\n    \n    @Override\n    public CacheStats getStats() {\n        return stats;\n    }\n    \n    // Helper methods\n    private String buildCacheKey(K key) {\n        return cacheName + ":" + keySerializer.apply(key);\n    }\n    \n    private K extractOriginalKey(String cacheKey) {\n        String originalKey = cacheKey.substring(cacheName.length() + 1);\n        return keyDeserializer.apply(originalKey);\n    }\n    \n    // TTL and expiration management\n    @Override\n    public void expire(K key, Duration ttl) {\n        try {\n            igniteClient.transactions().runInTransaction(tx -> {\n                String cacheKey = buildCacheKey(key);\n                CacheEntry keyEntry = new CacheEntry();\n                keyEntry.setKey(cacheKey);\n                CacheEntry existing = cacheView.get(tx, keyEntry);\n                \n                if (existing != null) {\n                    existing.setExpiresAt(LocalDateTime.now().plus(ttl));\n                    cacheView.upsert(tx, existing);\n                }\n            });\n        } catch (Exception e) {\n            throw new RuntimeException("Failed to set expiration", e);\n        }\n    }\n    \n    @Override\n    public Duration getTimeToLive(K key) {\n        String cacheKey = buildCacheKey(key);\n        CacheEntry keyEntry = new CacheEntry();\n        keyEntry.setKey(cacheKey);\n        CacheEntry cached = cacheView.get(null, keyEntry);\n        \n        if (cached != null && cached.getExpiresAt() != null) {\n            return Duration.between(LocalDateTime.now(), cached.getExpiresAt());\n        }\n        \n        return null; // No expiration set\n    }\n    \n    // Statistics implementation\n    private static class CacheStatsImpl implements CacheStats {\n        private final AtomicLong hitCount = new AtomicLong(0);\n        private final AtomicLong missCount = new AtomicLong(0);\n        private final AtomicLong evictionCount = new AtomicLong(0);\n        private final AtomicLong totalLoadTime = new AtomicLong(0);\n        private final AtomicLong loadCount = new AtomicLong(0);\n        \n        public void recordHit() {\n            hitCount.incrementAndGet();\n        }\n        \n        public void recordMiss() {\n            missCount.incrementAndGet();\n        }\n        \n        public void recordEviction() {\n            evictionCount.incrementAndGet();\n        }\n        \n        public void recordLoadTime(long nanos) {\n            totalLoadTime.addAndGet(nanos);\n            loadCount.incrementAndGet();\n        }\n        \n        @Override\n        public long getHitCount() {\n            return hitCount.get();\n        }\n        \n        @Override\n        public long getMissCount() {\n            return missCount.get();\n        }\n        \n        @Override\n        public double getHitRate() {\n            long hits = getHitCount();\n            long total = hits + getMissCount();\n            return total == 0 ? 0.0 : (double) hits / total;\n        }\n        \n        @Override\n        public long getEvictionCount() {\n            return evictionCount.get();\n        }\n        \n        @Override\n        public Duration getAverageLoadTime() {\n            long loads = loadCount.get();\n            return loads == 0 ? Duration.ZERO : Duration.ofNanos(totalLoadTime.get() / loads);\n        }\n        \n        @Override\n        public long getTotalLoadTime() {\n            return totalLoadTime.get();\n        }\n        \n        @Override\n        public long getSize() {\n            return 0; // Would need to be calculated separately\n        }\n    }\n}\n```\n\n### Cache Factory and Builder Pattern

```java
public class CacheFactory {
    
    public static <K, V> DistributedCache<K, V> createStringKeyCache(
            IgniteClient igniteClient,
            String cacheName,
            Class<V> valueType,
            Duration defaultTtl) {
        
        Function<K, String> keySerializer = Object::toString;
        Function<String, K> keyDeserializer = key -> (K) key; // Simplified
        Function<V, byte[]> valueSerializer = createValueSerializer(valueType);
        Function<byte[], V> valueDeserializer = createValueDeserializer(valueType);
        
        return new IgniteDistributedCache<>(
            igniteClient, cacheName, keySerializer, keyDeserializer,
            valueSerializer, valueDeserializer, defaultTtl
        );
    }
    
    public static <K, V> CacheBuilder<K, V> builder(IgniteClient igniteClient) {
        return new CacheBuilder<>(igniteClient);
    }
    
    public static class CacheBuilder<K, V> {
        private final IgniteClient igniteClient;
        private String cacheName;
        private Duration defaultTtl;
        private Function<K, String> keySerializer;
        private Function<String, K> keyDeserializer;
        private Function<V, byte[]> valueSerializer;
        private Function<byte[], V> valueDeserializer;
        
        public CacheBuilder(IgniteClient igniteClient) {
            this.igniteClient = igniteClient;
        }
        
        public CacheBuilder<K, V> cacheName(String cacheName) {
            this.cacheName = cacheName;
            return this;
        }
        
        public CacheBuilder<K, V> defaultTtl(Duration ttl) {
            this.defaultTtl = ttl;
            return this;
        }
        
        public CacheBuilder<K, V> keySerializer(Function<K, String> serializer, Function<String, K> deserializer) {
            this.keySerializer = serializer;
            this.keyDeserializer = deserializer;
            return this;
        }
        
        public CacheBuilder<K, V> valueSerializer(Function<V, byte[]> serializer, Function<byte[], V> deserializer) {
            this.valueSerializer = serializer;
            this.valueDeserializer = deserializer;
            return this;
        }
        
        public DistributedCache<K, V> build() {
            Objects.requireNonNull(cacheName, "Cache name is required");
            Objects.requireNonNull(keySerializer, "Key serializer is required");
            Objects.requireNonNull(keyDeserializer, "Key deserializer is required");
            Objects.requireNonNull(valueSerializer, "Value serializer is required");
            Objects.requireNonNull(valueDeserializer, "Value deserializer is required");
            
            return new IgniteDistributedCache<>(
                igniteClient, cacheName, keySerializer, keyDeserializer,
                valueSerializer, valueDeserializer, defaultTtl
            );
        }
    }
    
    // Helper methods for common serialization patterns
    private static <V> Function<V, byte[]> createValueSerializer(Class<V> valueType) {
        if (String.class.equals(valueType)) {
            return value -> ((String) value).getBytes(StandardCharsets.UTF_8);
        } else {
            // Use Java serialization as fallback
            return value -> {
                try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                     ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                    oos.writeObject(value);
                    return bos.toByteArray();
                } catch (IOException e) {
                    throw new RuntimeException("Serialization failed", e);
                }
            };
        }
    }
    
    private static <V> Function<byte[], V> createValueDeserializer(Class<V> valueType) {
        if (String.class.equals(valueType)) {
            return bytes -> (V) new String(bytes, StandardCharsets.UTF_8);
        } else {
            // Use Java deserialization as fallback
            return bytes -> {
                try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                     ObjectInputStream ois = new ObjectInputStream(bis)) {
                    return (V) ois.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException("Deserialization failed", e);
                }
            };
        }
    }
}
```

## 14.4 Integration Patterns

### Spring Cache Integration

Ignite 3 can be integrated with Spring's caching abstraction for seamless application caching.

```java
// Spring Cache Configuration
@Configuration
@EnableCaching
public class IgniteCacheConfiguration {
    
    @Bean
    public CacheManager igniteCacheManager(IgniteClient igniteClient) {
        return new IgniteCacheManager(igniteClient);
    }
    
    // Custom cache manager implementation
    public static class IgniteCacheManager implements CacheManager {
        private final IgniteClient igniteClient;
        private final Map<String, DistributedCache<Object, Object>> caches = new ConcurrentHashMap<>();
        
        public IgniteCacheManager(IgniteClient igniteClient) {
            this.igniteClient = igniteClient;
        }
        
        @Override
        public Cache getCache(String name) {
            return caches.computeIfAbsent(name, this::createCache);
        }
        
        @Override
        public Collection<String> getCacheNames() {
            return caches.keySet();
        }
        
        private DistributedCache<Object, Object> createCache(String name) {
            return CacheFactory.<Object, Object>builder(igniteClient)
                .cacheName(name)
                .defaultTtl(Duration.ofHours(1))
                .keySerializer(Object::toString, key -> key)
                .valueSerializer(this::serialize, this::deserialize)
                .build();
        }
        
        private byte[] serialize(Object value) {
            // Implement serialization logic
            return new byte[0];
        }
        
        private Object deserialize(byte[] bytes) {
            // Implement deserialization logic
            return new Object();
        }
    }
}

// Usage in Spring service
@Service
public class UserService {
    
    @Cacheable(value = "users", key = "#userId")
    public User getUserById(String userId) {
        // This method result will be cached
        return loadUserFromDatabase(userId);
    }
    
    @CacheEvict(value = "users", key = "#user.id")
    public void updateUser(User user) {
        // This will evict the user from cache
        saveUserToDatabase(user);
    }
    
    @CachePut(value = "users", key = "#user.id")
    public User saveUser(User user) {
        // This will update the cache with the new value
        return saveUserToDatabase(user);
    }
    
    private User loadUserFromDatabase(String userId) {
        // Database loading logic
        return new User();
    }
    
    private User saveUserToDatabase(User user) {
        // Database saving logic
        return user;
    }
}
```

### Redis Migration Strategy

For applications migrating from Redis to Ignite 3, here's a compatibility layer:

```java
public class RedisToIgniteMigration {
    
    // Redis-like interface using Ignite 3
    public static class IgniteRedisAdapter {
        private final DistributedCache<String, String> stringCache;
        private final DistributedCache<String, byte[]> binaryCache;
        
        public IgniteRedisAdapter(IgniteClient igniteClient) {
            this.stringCache = CacheFactory.createStringKeyCache(
                igniteClient, "redis_strings", String.class, null);
                
            this.binaryCache = CacheFactory.<String, byte[]>builder(igniteClient)
                .cacheName("redis_binary")
                .keySerializer(key -> key, key -> key)
                .valueSerializer(bytes -> bytes, bytes -> bytes)
                .build();
        }
        
        // Redis SET command equivalent
        public void set(String key, String value) {
            stringCache.put(key, value);
        }
        
        public void set(String key, String value, Duration expiration) {
            stringCache.put(key, value, expiration);
        }
        
        // Redis GET command equivalent
        public String get(String key) {
            return stringCache.get(key);
        }
        
        // Redis DEL command equivalent
        public void del(String key) {
            stringCache.evict(key);
        }
        
        // Redis EXISTS command equivalent
        public boolean exists(String key) {
            return stringCache.containsKey(key);
        }
        
        // Redis EXPIRE command equivalent
        public void expire(String key, Duration ttl) {
            stringCache.expire(key, ttl);
        }
        
        // Redis MGET command equivalent
        public List<String> mget(String... keys) {
            Map<String, String> results = stringCache.getAll(Arrays.asList(keys));
            return Arrays.stream(keys)
                .map(results::get)
                .collect(Collectors.toList());
        }
        
        // Redis MSET command equivalent
        public void mset(Map<String, String> keyValues) {
            stringCache.putAll(keyValues);
        }
        
        // Redis INCR command equivalent
        public long incr(String key) {
            return incrBy(key, 1);
        }
        
        public long incrBy(String key, long increment) {
            // This requires atomic operations - simplified implementation
            String current = stringCache.get(key);
            long currentValue = current != null ? Long.parseLong(current) : 0;
            long newValue = currentValue + increment;
            stringCache.put(key, String.valueOf(newValue));
            return newValue;
        }
        
        // Redis Hash operations
        public void hset(String hash, String field, String value) {
            String hashKey = hash + ":" + field;
            stringCache.put(hashKey, value);
        }
        
        public String hget(String hash, String field) {
            String hashKey = hash + ":" + field;
            return stringCache.get(hashKey);
        }
        
        // Redis List operations (simplified)
        public void lpush(String list, String value) {
            // Simplified implementation - in practice you'd need more sophisticated list handling
            String listKey = list + ":list";\n            String current = stringCache.get(listKey);\n            String newValue = value + "," + (current != null ? current : "");\n            stringCache.put(listKey, newValue);\n        }\n        \n        public String lpop(String list) {\n            // Simplified implementation\n            String listKey = list + ":list";\n            String current = stringCache.get(listKey);\n            if (current != null && !current.isEmpty()) {\n                String[] parts = current.split(",", 2);\n                String value = parts[0];\n                String remaining = parts.length > 1 ? parts[1] : "";\n                \n                if (remaining.isEmpty()) {\n                    stringCache.evict(listKey);\n                } else {\n                    stringCache.put(listKey, remaining);\n                }\n                \n                return value;\n            }\n            return null;\n        }\n    }\n}\n```\n\n### Session Storage Pattern\n\n```java\n// HTTP Session storage using Ignite 3\n@Component\npublic class IgniteSessionStore {\n    private final DistributedCache<String, SessionData> sessionCache;\n    \n    public IgniteSessionStore(IgniteClient igniteClient) {\n        this.sessionCache = CacheFactory.<String, SessionData>builder(igniteClient)\n            .cacheName("http_sessions")\n            .defaultTtl(Duration.ofMinutes(30)) // 30-minute session timeout\n            .keySerializer(key -> key, key -> key)\n            .valueSerializer(this::serializeSession, this::deserializeSession)\n            .build();\n    }\n    \n    public static class SessionData implements Serializable {\n        private final Map<String, Object> attributes = new ConcurrentHashMap<>();\n        private LocalDateTime creationTime;\n        private LocalDateTime lastAccessTime;\n        private boolean isNew = true;\n        \n        public SessionData() {\n            this.creationTime = LocalDateTime.now();\n            this.lastAccessTime = LocalDateTime.now();\n        }\n        \n        public void setAttribute(String name, Object value) {\n            attributes.put(name, value);\n            touch();\n        }\n        \n        public Object getAttribute(String name) {\n            touch();\n            return attributes.get(name);\n        }\n        \n        public void removeAttribute(String name) {\n            attributes.remove(name);\n            touch();\n        }\n        \n        public Set<String> getAttributeNames() {\n            return attributes.keySet();\n        }\n        \n        private void touch() {\n            this.lastAccessTime = LocalDateTime.now();\n            this.isNew = false;\n        }\n        \n        // Getters and setters...\n    }\n    \n    public SessionData createSession(String sessionId) {\n        SessionData session = new SessionData();\n        sessionCache.put(sessionId, session);\n        return session;\n    }\n    \n    public SessionData getSession(String sessionId) {\n        return sessionCache.get(sessionId);\n    }\n    \n    public void saveSession(String sessionId, SessionData session) {\n        sessionCache.put(sessionId, session, Duration.ofMinutes(30));\n    }\n    \n    public void deleteSession(String sessionId) {\n        sessionCache.evict(sessionId);\n    }\n    \n    public void extendSession(String sessionId, Duration additionalTime) {\n        SessionData session = sessionCache.get(sessionId);\n        if (session != null) {\n            sessionCache.put(sessionId, session, additionalTime);\n        }\n    }\n    \n    private byte[] serializeSession(SessionData session) {\n        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();\n             ObjectOutputStream oos = new ObjectOutputStream(bos)) {\n            oos.writeObject(session);\n            return bos.toByteArray();\n        } catch (IOException e) {\n            throw new RuntimeException("Failed to serialize session", e);\n        }\n    }\n    \n    private SessionData deserializeSession(byte[] data) {\n        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);\n             ObjectInputStream ois = new ObjectInputStream(bis)) {\n            return (SessionData) ois.readObject();\n        } catch (IOException | ClassNotFoundException e) {\n            throw new RuntimeException("Failed to deserialize session", e);\n        }\n    }\n}\n\n// Spring Session integration\n@Component\npublic class IgniteSessionRepository implements SessionRepository<IgniteSession> {\n    private final IgniteSessionStore sessionStore;\n    \n    public IgniteSessionRepository(IgniteSessionStore sessionStore) {\n        this.sessionStore = sessionStore;\n    }\n    \n    @Override\n    public IgniteSession createSession() {\n        String sessionId = UUID.randomUUID().toString();\n        SessionData sessionData = sessionStore.createSession(sessionId);\n        return new IgniteSession(sessionId, sessionData, sessionStore);\n    }\n    \n    @Override\n    public IgniteSession findById(String id) {\n        SessionData sessionData = sessionStore.getSession(id);\n        return sessionData != null ? new IgniteSession(id, sessionData, sessionStore) : null;\n    }\n    \n    @Override\n    public void deleteById(String id) {\n        sessionStore.deleteSession(id);\n    }\n    \n    @Override\n    public void save(IgniteSession session) {\n        sessionStore.saveSession(session.getId(), session.getSessionData());\n    }\n    \n    // IgniteSession implementation\n    public static class IgniteSession implements Session {\n        private final String id;\n        private final SessionData sessionData;\n        private final IgniteSessionStore sessionStore;\n        \n        public IgniteSession(String id, SessionData sessionData, IgniteSessionStore sessionStore) {\n            this.id = id;\n            this.sessionData = sessionData;\n            this.sessionStore = sessionStore;\n        }\n        \n        @Override\n        public String getId() {\n            return id;\n        }\n        \n        @Override\n        public String changeSessionId() {\n            String newId = UUID.randomUUID().toString();\n            sessionStore.saveSession(newId, sessionData);\n            sessionStore.deleteSession(id);\n            return newId;\n        }\n        \n        @Override\n        public <T> T getAttribute(String attributeName) {\n            return (T) sessionData.getAttribute(attributeName);\n        }\n        \n        @Override\n        public void setAttribute(String attributeName, Object attributeValue) {\n            sessionData.setAttribute(attributeName, attributeValue);\n        }\n        \n        @Override\n        public void removeAttribute(String attributeName) {\n            sessionData.removeAttribute(attributeName);\n        }\n        \n        @Override\n        public Set<String> getAttributeNames() {\n            return sessionData.getAttributeNames();\n        }\n        \n        @Override\n        public Instant getCreationTime() {\n            return sessionData.getCreationTime().atZone(ZoneId.systemDefault()).toInstant();\n        }\n        \n        @Override\n        public Instant getLastAccessedTime() {\n            return sessionData.getLastAccessTime().atZone(ZoneId.systemDefault()).toInstant();\n        }\n        \n        @Override\n        public void setMaxInactiveInterval(Duration interval) {\n            sessionStore.extendSession(id, interval);\n        }\n        \n        @Override\n        public Duration getMaxInactiveInterval() {\n            return Duration.ofMinutes(30); // Default\n        }\n        \n        @Override\n        public boolean isExpired() {\n            return false; // Ignite handles expiration\n        }\n        \n        public SessionData getSessionData() {\n            return sessionData;\n        }\n    }\n}\n```\n\n## 14.5 Performance Optimization\n\n### Cache Hit Ratio Optimization\n\n```java\npublic class CacheOptimization {\n    \n    // Cache warming strategies\n    public static class CacheWarmer<K, V> {\n        private final DistributedCache<K, V> cache;\n        private final Function<Collection<K>, Map<K, V>> bulkLoader;\n        private final ScheduledExecutorService scheduler;\n        \n        public CacheWarmer(DistributedCache<K, V> cache, \n                          Function<Collection<K>, Map<K, V>> bulkLoader) {\n            this.cache = cache;\n            this.bulkLoader = bulkLoader;\n            this.scheduler = Executors.newScheduledThreadPool(2);\n        }\n        \n        // Proactive cache warming\n        public void warmCache(Collection<K> keys) {\n            System.out.println("Starting cache warming for \" + keys.size() + \" keys\");\n            \n            Map<K, V> data = bulkLoader.apply(keys);\n            cache.putAll(data);\n            \n            System.out.println(\"Cache warming completed. Loaded \" + data.size() + \" entries\");\n        }\n        \n        // Scheduled cache refresh\n        public void scheduleRefresh(Collection<K> keys, Duration interval) {\n            scheduler.scheduleAtFixedRate(\n                () -> {\n                    try {\n                        refreshKeys(keys);\n                    } catch (Exception e) {\n                        System.err.println(\"Cache refresh failed: \" + e.getMessage());\n                    }\n                },\n                interval.toSeconds(), interval.toSeconds(), TimeUnit.SECONDS\n            );\n        }\n        \n        private void refreshKeys(Collection<K> keys) {\n            System.out.println(\"Refreshing cache for \" + keys.size() + \" keys\");\n            \n            // Load fresh data\n            Map<K, V> freshData = bulkLoader.apply(keys);\n            \n            // Update cache\n            cache.putAll(freshData);\n            \n            System.out.println(\"Cache refresh completed\");\n        }\n        \n        // Intelligent warming based on access patterns\n        public void intelligentWarm(int topN) {\n            // This would require access statistics from the cache\n            // Get most accessed keys and pre-warm them\n            \n            // Simplified implementation - in practice you'd analyze access logs\n            List<K> popularKeys = getPopularKeys(topN);\n            warmCache(popularKeys);\n        }\n        \n        private List<K> getPopularKeys(int limit) {\n            // Implementation would analyze cache statistics or access logs\n            return new ArrayList<>(); // Simplified\n        }\n        \n        public void shutdown() {\n            scheduler.shutdown();\n        }\n    }\n    \n    // Cache monitoring and optimization\n    public static class CacheMonitor {\n        private final DistributedCache<?, ?> cache;\n        private final ScheduledExecutorService scheduler;\n        \n        public CacheMonitor(DistributedCache<?, ?> cache) {\n            this.cache = cache;\n            this.scheduler = Executors.newScheduledThreadPool(1);\n        }\n        \n        public void startMonitoring() {\n            scheduler.scheduleAtFixedRate(this::reportStats, 60, 60, TimeUnit.SECONDS);\n        }\n        \n        private void reportStats() {\n            CacheStats stats = cache.getStats();\n            \n            System.out.println(\"=== Cache Statistics ===\");\n            System.out.println(\"Hit Rate: \" + String.format(\"%.2f%%\", stats.getHitRate() * 100));\n            System.out.println(\"Hits: \" + stats.getHitCount());\n            System.out.println(\"Misses: \" + stats.getMissCount());\n            System.out.println(\"Evictions: \" + stats.getEvictionCount());\n            System.out.println(\"Size: \" + stats.getSize());\n            System.out.println(\"Avg Load Time: \" + stats.getAverageLoadTime().toMillis() + \"ms\");\n            \n            // Optimization recommendations\n            if (stats.getHitRate() < 0.8) {\n                System.out.println(\"⚠️ Low hit rate detected! Consider:\");\n                System.out.println(\"  - Increasing cache size\");\n                System.out.println(\"  - Extending TTL values\");\n                System.out.println(\"  - Implementing cache warming\");\n            }\n            \n            if (stats.getAverageLoadTime().toMillis() > 100) {\n                System.out.println(\"⚠️ High load time detected! Consider:\");\n                System.out.println(\"  - Optimizing data source queries\");\n                System.out.println(\"  - Implementing bulk loading\");\n                System.out.println(\"  - Using async loading patterns\");\n            }\n        }\n        \n        public void shutdown() {\n            scheduler.shutdown();\n        }\n    }\n}\n```\n\n### Memory Management for Caches\n\n```java\npublic class CacheMemoryManagement {\n    \n    // LRU eviction policy implementation\n    public static class LRUCacheWrapper<K, V> implements DistributedCache<K, V> {\n        private final DistributedCache<K, V> delegate;\n        private final int maxSize;\n        private final Map<K, LocalDateTime> accessTimes = new ConcurrentHashMap<>();\n        \n        public LRUCacheWrapper(DistributedCache<K, V> delegate, int maxSize) {\n            this.delegate = delegate;\n            this.maxSize = maxSize;\n            \n            // Start periodic cleanup\n            ScheduledExecutorService cleanup = Executors.newScheduledThreadPool(1);\n            cleanup.scheduleAtFixedRate(this::evictLRU, 60, 60, TimeUnit.SECONDS);\n        }\n        \n        @Override\n        public V get(K key) {\n            V value = delegate.get(key);\n            if (value != null) {\n                accessTimes.put(key, LocalDateTime.now());\n            }\n            return value;\n        }\n        \n        @Override\n        public void put(K key, V value, Duration ttl) {\n            // Check if we need to evict before adding\n            if (delegate.size() >= maxSize) {\n                evictLRU();\n            }\n            \n            delegate.put(key, value, ttl);\n            accessTimes.put(key, LocalDateTime.now());\n        }\n        \n        private void evictLRU() {\n            if (accessTimes.size() <= maxSize) {\n                return;\n            }\n            \n            // Find least recently used keys\n            int toEvict = (int) (accessTimes.size() - maxSize * 0.8); // Evict to 80% of max\n            \n            List<K> lruKeys = accessTimes.entrySet().stream()\n                .sorted(Map.Entry.comparingByValue())\n                .limit(toEvict)\n                .map(Map.Entry::getKey)\n                .collect(Collectors.toList());\n            \n            for (K key : lruKeys) {\n                delegate.evict(key);\n                accessTimes.remove(key);\n            }\n            \n            System.out.println(\"Evicted \" + lruKeys.size() + \" LRU entries\");\n        }\n        \n        // Delegate all other methods\n        @Override\n        public void put(K key, V value) {\n            put(key, value, null);\n        }\n        \n        @Override\n        public void evict(K key) {\n            delegate.evict(key);\n            accessTimes.remove(key);\n        }\n        \n        // ... other delegated methods\n    }\n    \n    // Memory-aware cache sizing\n    public static class MemoryAwareCacheManager {\n        private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();\n        private final Map<String, DistributedCache<?, ?>> managedCaches = new ConcurrentHashMap<>();\n        \n        public void registerCache(String name, DistributedCache<?, ?> cache) {\n            managedCaches.put(name, cache);\n        }\n        \n        public void checkMemoryPressure() {\n            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();\n            double usageRatio = (double) heapUsage.getUsed() / heapUsage.getMax();\n            \n            if (usageRatio > 0.8) {\n                System.out.println(\"⚠️ High memory usage detected: \" + \n                    String.format(\"%.1f%%\", usageRatio * 100));\n                \n                // Trigger cache cleanup\n                triggerCacheCleanup();\n            }\n        }\n        \n        private void triggerCacheCleanup() {\n            for (Map.Entry<String, DistributedCache<?, ?>> entry : managedCaches.entrySet()) {\n                String cacheName = entry.getKey();\n                DistributedCache<?, ?> cache = entry.getValue();\n                \n                long sizeBefore = cache.size();\n                \n                // Reduce cache size by 25%\n                reduceCacheSize(cache, 0.25);\n                \n                long sizeAfter = cache.size();\n                System.out.println(\"Cleaned cache \" + cacheName + \": \" + \n                    sizeBefore + \" -> \" + sizeAfter + \" entries\");\n            }\n            \n            // Suggest garbage collection\n            System.gc();\n        }\n        \n        private void reduceCacheSize(DistributedCache<?, ?> cache, double reductionRatio) {\n            // This is a simplified approach - in practice you'd implement\n            // more sophisticated eviction strategies\n            long targetReduction = (long) (cache.size() * reductionRatio);\n            \n            // Would need access to cache internals for proper LRU eviction\n            // This is a placeholder for the actual implementation\n        }\n        \n        public void startMemoryMonitoring() {\n            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);\n            scheduler.scheduleAtFixedRate(this::checkMemoryPressure, 30, 30, TimeUnit.SECONDS);\n        }\n    }\n}\n```\n\n### Bulk Operations for Cache Loading\n\n```java\npublic class BulkCacheOperations {\n    \n    // Efficient bulk loading\n    public static class BulkCacheLoader<K, V> {\n        private final DistributedCache<K, V> cache;\n        private final Function<Collection<K>, Map<K, V>> bulkDataLoader;\n        private final int batchSize;\n        \n        public BulkCacheLoader(DistributedCache<K, V> cache,\n                              Function<Collection<K>, Map<K, V>> bulkDataLoader,\n                              int batchSize) {\n            this.cache = cache;\n            this.bulkDataLoader = bulkDataLoader;\n            this.batchSize = batchSize;\n        }\n        \n        public void loadAll(Collection<K> keys) {\n            List<K> keyList = new ArrayList<>(keys);\n            \n            for (int i = 0; i < keyList.size(); i += batchSize) {\n                int endIndex = Math.min(i + batchSize, keyList.size());\n                List<K> batch = keyList.subList(i, endIndex);\n                \n                try {\n                    loadBatch(batch);\n                } catch (Exception e) {\n                    System.err.println(\"Failed to load batch: \" + e.getMessage());\n                    // Continue with next batch\n                }\n            }\n        }\n        \n        private void loadBatch(Collection<K> keys) {\n            Map<K, V> data = bulkDataLoader.apply(keys);\n            cache.putAll(data);\n        }\n        \n        // Parallel bulk loading\n        public CompletableFuture<Void> loadAllAsync(Collection<K> keys) {\n            List<K> keyList = new ArrayList<>(keys);\n            List<CompletableFuture<Void>> futures = new ArrayList<>();\n            \n            for (int i = 0; i < keyList.size(); i += batchSize) {\n                int endIndex = Math.min(i + batchSize, keyList.size());\n                List<K> batch = keyList.subList(i, endIndex);\n                \n                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {\n                    try {\n                        loadBatch(batch);\n                    } catch (Exception e) {\n                        System.err.println(\"Async batch load failed: \" + e.getMessage());\n                    }\n                });\n                \n                futures.add(future);\n            }\n            \n            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));\n        }\n    }\n}\n```\n\n## Best Practices Summary\n\n### 1. Cache Design Principles\n\n- **Single Responsibility**: Each cache should have a clear, single purpose\n- **Consistent Naming**: Use consistent naming conventions for cache keys\n- **TTL Strategy**: Always define appropriate TTL values\n- **Size Limits**: Implement size limits to prevent memory issues\n- **Monitoring**: Always implement cache statistics and monitoring\n\n### 2. Performance Guidelines\n\n- **Bulk Operations**: Use bulk operations for better performance\n- **Async Patterns**: Leverage async operations for non-blocking performance\n- **Connection Reuse**: Reuse Ignite client connections\n- **Batch Size Optimization**: Tune batch sizes based on data size and network latency\n- **Memory Management**: Monitor and manage cache memory usage\n\n### 3. Operational Considerations\n\n- **Cache Warming**: Implement cache warming strategies for critical data\n- **Graceful Degradation**: Handle cache failures gracefully\n- **Monitoring and Alerting**: Set up monitoring for cache hit rates and performance\n- **Backup Strategies**: Consider backup strategies for critical cached data\n- **Testing**: Thoroughly test cache behavior under various failure scenarios\n\n### 4. Migration from Ignite 2\n\n- **API Mapping**: Map Ignite 2 IgniteCache operations to Ignite 3 Table API\n- **Configuration Migration**: Migrate cache configurations to table definitions\n- **Near Cache**: Replace Ignite 2 near caches with application-level caching\n- **Event Handling**: Migrate cache event listeners to SQL-based monitoring\n- **Gradual Migration**: Implement gradual migration strategies for large applications\n\nThis comprehensive caching guide provides the foundation for implementing robust, scalable caching solutions with Apache Ignite 3, whether you're building new applications or migrating from existing caching solutions.\n
