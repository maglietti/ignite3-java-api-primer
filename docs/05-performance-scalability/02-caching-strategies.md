<!--
Licensed under Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)
SPDX-License-Identifier: CC-BY-SA-4.0
For full license text, see LICENSE-CC-BY-SA-4.0
-->

# Chapter 5.2: Caching Strategies and Performance Optimization

Your popular tracks are being loaded from disk storage repeatedly because cache misses are creating database bottlenecks during peak traffic. Users experience response delays of 500ms+ when browsing the music catalog, while your database servers struggle under read pressure from the same data requests hitting storage systems dozens of times per second.

This happens because traditional application architectures treat caching as an afterthought rather than designing for distributed data access from the start. Your music streaming platform processes millions of catalog requests daily, but without intelligent caching strategies, each popular artist lookup becomes a costly database operation multiplied across your entire user base.

## Working with the Reference Application

The **`09-caching-patterns-app`** demonstrates intelligent caching patterns that reduce database load while ensuring data consistency:

```bash
cd ignite3-reference-apps/09-caching-patterns-app
mvn compile exec:java
```

The reference application showcases:

- **CacheAsidePatterns** - Application-controlled caching for read-heavy workloads
- **WriteThroughPatterns** - Immediate consistency for critical data updates
- **WriteBehindPatterns** - High-volume event processing with eventual consistency
- **ExternalDataSource** - Simulated external database for realistic scenarios

These patterns demonstrate production-ready caching strategies for music streaming platforms, handling catalog browsing, user profiles, and real-time events with appropriate consistency guarantees.

## Ignite 3's Table-Based Caching Architecture

Ignite 3 approaches caching differently from traditional cache-aside layers by treating cache as native distributed tables rather than external storage. This architecture eliminates the complexity of managing separate cache and database systems while providing the performance benefits of in-memory data access.

### Unified Cache and Storage

Instead of maintaining separate cache infrastructure, Ignite 3 tables function as both cache and storage, with configurable persistence layers that determine whether data resides in memory, on disk, or both. This unified approach simplifies cache management while providing ACID transaction guarantees across cached data.

### Multiple Access Patterns

The table-based architecture supports different access methods:

- **KeyValueView** - Direct key-value access for simple cache operations
- **RecordView** - Object-oriented access with full POJO support  
- **SQL API** - Query-based access for complex cache operations

This design enables caching strategies that leverage Ignite's distributed computing capabilities, transaction support, and query optimization while maintaining the simplicity of traditional cache operations.

## Caching Fundamentals

Effective caching strategies must address three core challenges in distributed environments: consistency, availability, and partition tolerance. These challenges directly impact how music streaming platforms handle catalog data, user sessions, and event processing.

### Cache Consistency Patterns

Different data types require different consistency guarantees:

**Strong Consistency** - Critical for customer profiles, subscription status, and billing information where stale data creates business problems.

**Eventual Consistency** - Suitable for catalog data, play counts, and recommendation systems where slight delays don't impact user experience.

**Weak Consistency** - Appropriate for analytics data and non-critical metrics where approximate values are acceptable for performance gains.

### Cache Invalidation Strategies

**Time-based Expiration** - Simple but wasteful for data with unpredictable change frequency.

**Event-based Invalidation** - Reactive approach that invalidates cache entries when source data changes.

**Version-based Invalidation** - Uses data versioning to determine cache freshness.

### Performance vs. Consistency Trade-offs

Music streaming platforms face different trade-offs across functional areas:

- **Catalog Browsing** - Favors performance over consistency
- **User Authentication** - Requires strong consistency  
- **Analytics Processing** - Balances both concerns

## Cache-Aside Pattern

Cache-aside operations place your application in control of data access decisions. When users browse artists, the application first checks Ignite's distributed cache. Cache hits return instantly from memory, while cache misses trigger database loads that populate the cache for subsequent requests.

### Implementation Approach

```java
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.KeyValueView;

public class CacheAsidePattern {
    
    private final KeyValueView<Integer, Artist> artistCache;
    private final ExternalDataSource externalDataSource;
    
    public CacheAsidePattern(IgniteClient client, ExternalDataSource dataSource) {
        this.artistCache = client.tables().table("ArtistCache")
            .keyValueView(Integer.class, Artist.class);
        this.externalDataSource = dataSource;
    }
    
    /**
     * Cache-aside pattern implementation:
     * 1. Check cache first
     * 2. On cache miss, load from external source
     * 3. Populate cache with loaded data
     * 4. Return data to caller
     */
    public CompletableFuture<Artist> getArtist(Integer artistId) {
        return artistCache.getAsync(artistId)
            .thenCompose(cachedArtist -> {
                if (cachedArtist != null) {
                    return CompletableFuture.completedFuture(cachedArtist);
                }
                
                // Cache miss - load from external source
                return externalDataSource.loadArtistAsync(artistId)
                    .thenCompose(artist -> {
                        if (artist != null) {
                            // Populate cache for future requests
                            return artistCache.putAsync(artistId, artist)
                                .thenApply(ignored -> artist);
                        }
                        return CompletableFuture.completedFuture(null);
                    });
            });
    }
}
```

### Performance Benefits

Cache-aside reduces database load by 60-80% for typical music catalog access characteristics while maintaining complete application control over caching behavior. This approach works best for read-heavy workloads where cache hit rates justify the implementation complexity.

## Write-Through Pattern

Customer profile updates require data consistency across all systems immediately after changes occur. When users update subscription status, changes must be visible instantly in both cache and persistent storage to prevent billing errors or access control issues.

### Implementation Approach

Write-through operations coordinate updates across multiple data stores within the same transaction:

```java
import org.apache.ignite.table.RecordView;
import org.apache.ignite.transactions.IgniteTransactions;

public class WriteThroughPattern {
    
    private final RecordView<Customer> customerCache;
    private final IgniteTransactions transactions;
    private final ExternalDataSource externalDataSource;
    
    /**
     * Write-through pattern implementation:
     * 1. Begin transaction
     * 2. Update cache
     * 3. Update external database
     * 4. Commit transaction (or rollback on failure)
     */
    public CompletableFuture<Void> updateCustomer(Customer customer) {
        return transactions.runInTransactionAsync(tx -> {
            // Update cache first
            return customerCache.upsertAsync(tx, customer)
                .thenCompose(ignored -> {
                    // Update external database
                    return externalDataSource.updateCustomerAsync(customer);
                });
        });
    }
}
```

### Consistency Benefits

Write-through eliminates consistency gaps between cache and database updates. All customer-critical data maintains immediate consistency across all systems, preventing access control issues and billing errors.

## Write-Behind Pattern

Play event tracking generates millions of events per hour that must be persisted without blocking real-time user interactions. Write-behind operations solve this by accepting writes immediately to cache while asynchronously persisting data to external systems.

### Implementation Approach

```java
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class WriteBehindPattern {
    
    private final RecordView<PlayEvent> eventCache;
    private final ExternalDataSource externalDataSource;
    private final BlockingQueue<PlayEvent> writeQueue;
    
    public WriteBehindPattern(IgniteClient client, ExternalDataSource dataSource) {
        this.eventCache = client.tables().table("PlayEventCache")
            .recordView(PlayEvent.class);
        this.externalDataSource = dataSource;
        this.writeQueue = new LinkedBlockingQueue<>();
        
        // Start background writer
        startBackgroundWriter();
    }
    
    /**
     * Write-behind pattern implementation:
     * 1. Write to cache immediately (fast response)
     * 2. Queue for background processing
     * 3. Async write to external database
     */
    public CompletableFuture<Void> recordPlayEvent(PlayEvent event) {
        return eventCache.upsertAsync(null, event)
            .thenRun(() -> {
                // Queue for background persistence
                writeQueue.offer(event);
            });
    }
    
    private void startBackgroundWriter() {
        CompletableFuture.runAsync(() -> {
            while (true) {
                try {
                    PlayEvent event = writeQueue.take();
                    externalDataSource.persistPlayEventAsync(event);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
}
```

### Performance Benefits

Write-behind provides immediate response times for high-volume events while ensuring eventual persistence. This pattern handles millions of play events per hour without impacting user experience.

## Advanced Caching Patterns

### Cache Preloading

Warm up caches with popular content during system startup or low-traffic periods:

```java
public class CacheWarmup {
    
    public CompletableFuture<Void> preloadPopularArtists(List<Integer> popularArtistIds) {
        List<CompletableFuture<Artist>> loadFutures = popularArtistIds.stream()
            .map(this::loadAndCacheArtist)
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(loadFutures.toArray(new CompletableFuture[0]));
    }
    
    private CompletableFuture<Artist> loadAndCacheArtist(Integer artistId) {
        return externalDataSource.loadArtistAsync(artistId)
            .thenCompose(artist -> {
                if (artist != null) {
                    return artistCache.putAsync(artistId, artist)
                        .thenApply(ignored -> artist);
                }
                return CompletableFuture.completedFuture(null);
            });
    }
}
```

### Cache Invalidation

Maintain cache freshness through coordinated invalidation:

```java
public class CacheInvalidation {
    
    /**
     * Invalidate related cache entries when artist data changes
     */
    public CompletableFuture<Void> invalidateArtistData(Integer artistId) {
        List<CompletableFuture<Boolean>> invalidationTasks = Arrays.asList(
            artistCache.removeAsync(artistId),
            albumCache.removeAllAsync(getAlbumKeysForArtist(artistId)),
            trackCache.removeAllAsync(getTrackKeysForArtist(artistId))
        );
        
        return CompletableFuture.allOf(invalidationTasks.toArray(new CompletableFuture[0]));
    }
    
    /**
     * Time-based cache expiration
     */
    public void schedulePeriodicInvalidation() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        scheduler.scheduleAtFixedRate(() -> {
            invalidateExpiredEntries();
        }, 1, 1, TimeUnit.HOURS);
    }
}
```

## Performance Optimization

### Batch Operations

Optimize cache performance through batch operations:

```java
// Batch loading for multiple artists
Map<Integer, Artist> artists = artistCache.getAll(artistIds);

// Batch updates for user activity
Map<Integer, UserActivity> activities = // prepare activities
userActivityCache.putAll(activities);
```

### Memory Management

Monitor and control cache memory usage:

```java
// Configure cache size limits
DataStorageConfiguration dataStorage = new DataStorageConfiguration()
    .setDefaultDataRegionConfiguration(
        new DataRegionConfiguration()
            .setMaxSize(1024 * 1024 * 1024) // 1GB limit
            .setEvictionPolicy(new RandomEvictionPolicy())
    );
```

### Partitioning Strategies

Optimize data distribution for cache performance:

```java
// Colocate related data by artist
@Table(name = "ArtistData")
@Zone(value = "MusicStore", storageProfiles = "default") 
public class Artist {
    @Id
    private Integer artistId;  // Natural colocation key
    // Other fields...
}
```

## Production Considerations

### Monitoring and Metrics

Track cache performance in production:

- **Hit Ratio** - Percentage of requests served from cache
- **Response Time** - Cache vs. database access latency
- **Memory Usage** - Cache memory consumption patterns
- **Eviction Rate** - Frequency of cache entry removal

### Failure Handling

Design for cache failures:

- **Graceful Degradation** - Fall back to database when cache is unavailable
- **Circuit Breaker** - Prevent cascading failures during cache outages
- **Retry Logic** - Handle transient cache access failures
- **Health Checks** - Monitor cache cluster health

### Capacity Planning

Plan cache resources based on usage patterns:

- **Working Set Size** - Amount of frequently accessed data
- **Peak Load Multiplier** - Additional capacity for traffic spikes
- **Growth Projections** - Scale cache with business growth
- **Cost Optimization** - Balance memory costs with performance gains

---

Intelligent caching strategies transform database performance bottlenecks into responsive user experiences. By implementing appropriate cache patterns for different data types and access patterns, music streaming platforms can handle massive concurrent loads while maintaining data consistency and optimal resource utilization.

The reference application provides production-ready implementations of all major caching patterns, demonstrating how to build scalable caching solutions that adapt to real-world usage characteristics and business requirements.

**Next**: Continue exploring performance optimization techniques or advance to operational topics like monitoring, deployment, and production readiness.
