# Chapter 5.2: Caching Strategies and Performance Optimization

## Learning Objectives

By completing this chapter, you will:

- Implement cache-aside patterns for read-heavy workloads
- Master write-through and write-behind caching strategies
- Tune application performance with data access patterns
- Handle cache invalidation and consistency requirements

## Working with the Reference Application

The **`09-caching-patterns-app`** demonstrates caching patterns covered in this chapter with music platform examples. Run it alongside your learning to see cache-aside, write-through, and write-behind patterns in action.

**Quick Start**: After reading this chapter, explore the reference application:

```bash
cd ignite3-reference-apps/09-caching-patterns-app
mvn compile exec:java
```

The reference app shows how the streaming patterns from [Chapter 5.1](01-data-streaming.md) integrate with caching strategies, building on the data access and consistency patterns from previous modules.

## The Performance Challenge

Music streaming platforms process millions of requests daily, from catalog browsing to real-time play tracking. The difference between fast and slow responses often determines user satisfaction. While traditional caching solutions address individual use cases, distributed systems require coordinated data management patterns that ensure consistency across multiple data stores.

Ignite 3's Table and SQL APIs provide the foundation for implementing three essential caching patterns: cache-aside for read-heavy workloads, write-through for data consistency, and write-behind for high-throughput scenarios. These patterns leverage Ignite's distributed architecture while maintaining standard Java interfaces.

## Understanding Caching Patterns Through Music Store Operations

### The Data Access Challenge

A music streaming service manages multiple types of data with different access patterns:

**Catalog Data**: Frequently read artist and album information with occasional updates
**Customer Data**: User profiles requiring consistent updates across systems  
**Analytics Data**: High-volume play events and usage metrics

Each data type benefits from different caching strategies that balance performance with consistency requirements.

### Pattern Selection Criteria

**Cache-Aside Pattern**: Applications manage cache explicitly

- Read-heavy workloads with infrequent updates
- Catalog browsing, search results, popular playlists
- Application controls caching logic

**Write-Through Pattern**: Updates happen synchronously to cache and data store

- Data consistency requirements
- Customer profiles, payment information, subscription status
- Transaction guarantees across systems

**Write-Behind Pattern**: Cache updates immediately, data store updates asynchronously

- High-throughput write scenarios
- Play events, user activity tracking, metrics collection
- Performance over immediate consistency

## Cache-Aside Pattern Implementation

### Music Catalog Caching

The cache-aside pattern puts applications in control of cache management. When users browse artists, the application first checks Ignite for cached data, then loads from the primary data store on cache misses.

```java
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.KeyValueView;
import java.util.concurrent.CompletableFuture;

/**
 * Demonstrates cache-aside pattern implementation using Ignite 3 KeyValueView API.
 * 
 * Music streaming services use cache-aside for catalog data where:
 * - Reads significantly outnumber writes
 * - Applications control cache population and invalidation
 * - Cache misses are acceptable for infrequently accessed data
 */
public class CacheAsidePatternDemo {
    
    private final KeyValueView<Integer, Artist> artistCache;
    private final ExternalDataSource externalDataSource;
    
    public CacheAsidePatternDemo(IgniteClient client, ExternalDataSource dataSource) {
        this.artistCache = client.tables().table("ArtistCache").keyValueView(Integer.class, Artist.class);
        this.externalDataSource = dataSource;
    }
    
    /**
     * Retrieves artist information using cache-aside pattern.
     * 
     * Pattern implementation:
     * 1. Check cache first (Ignite)
     * 2. On cache miss, load from external source
     * 3. Populate cache with loaded data
     * 4. Return data to caller
     */
    public Artist getArtist(int artistId) {
        // Step 1: Check cache first
        Artist cached = artistCache.get(null, artistId);
        if (cached != null) {
            System.out.println("Cache hit for artist: " + artistId);
            return cached;
        }
        
        System.out.println("Cache miss for artist: " + artistId);
        
        // Step 2: Cache miss - load from external data source
        Artist artist = externalDataSource.loadArtist(artistId);
        if (artist == null) {
            return null;
        }
        
        // Step 3: Populate cache for future requests
        artistCache.put(null, artistId, artist);
        
        return artist;
    }
    
    /**
     * Batch loading optimization for popular artists.
     * 
     * Reduces external data source load by batching requests
     * and using Ignite's bulk operations for efficiency.
     */
    public Map<Integer, Artist> getPopularArtists(Collection<Integer> artistIds) {
        // Check cache for all requested artists
        Map<Integer, Artist> cachedResults = artistCache.getAll(null, artistIds);
        
        // Identify cache misses
        Set<Integer> missedIds = artistIds.stream()
            .filter(id -> !cachedResults.containsKey(id))
            .collect(Collectors.toSet());
        
        if (missedIds.isEmpty()) {
            System.out.println("All artists found in cache");
            return cachedResults;
        }
        
        System.out.println("Loading " + missedIds.size() + " artists from external source");
        
        // Load missing artists from external source
        Map<Integer, Artist> loadedArtists = externalDataSource.loadArtists(missedIds);
        
        // Cache the loaded artists
        if (!loadedArtists.isEmpty()) {
            artistCache.putAll(null, loadedArtists);
        }
        
        // Combine cached and loaded results
        Map<Integer, Artist> results = new HashMap<>(cachedResults);
        results.putAll(loadedArtists);
        
        return results;
    }
    
    /**
     * Cache invalidation for updated artist information.
     * 
     * Applications must handle cache invalidation when
     * external data changes to prevent stale data.
     */
    public void updateArtist(Artist artist) {
        // Update external data source first
        externalDataSource.updateArtist(artist);
        
        // Invalidate cache entry to force reload on next access
        artistCache.remove(null, artist.getArtistId());
        
        System.out.println("Artist " + artist.getArtistId() + " updated and cache invalidated");
    }
    
    /**
     * Asynchronous cache-aside for non-blocking operations.
     * 
     * Improves response times for concurrent requests by
     * using Ignite's async API capabilities.
     */
    public CompletableFuture<Artist> getArtistAsync(int artistId) {
        return artistCache.getAsync(null, artistId)
            .thenCompose(cached -> {
                if (cached != null) {
                    return CompletableFuture.completedFuture(cached);
                }
                
                // Load from external source asynchronously
                return externalDataSource.loadArtistAsync(artistId)
                    .thenCompose(artist -> {
                        if (artist == null) {
                            return CompletableFuture.completedFuture(null);
                        }
                        
                        // Cache the loaded artist
                        return artistCache.putAsync(null, artistId, artist)
                            .thenApply(ignored -> artist);
                    });
            });
    }
}
```

### Cache-Aside Pattern Characteristics

**Application Control**: The application manages all cache interactions explicitly
**Performance**: Fast reads for cached data, slower for cache misses
**Consistency**: Eventual consistency between cache and external data store
**Fault Tolerance**: Application continues working if cache is unavailable

## Write-Through Pattern Implementation

### Customer Data Synchronization

Write-through patterns ensure data consistency by updating both cache and external data store in the same transaction. Customer profile updates require this consistency to prevent data corruption across systems.

```java
import org.apache.ignite.transactions.IgniteTransactions;
import org.apache.ignite.table.RecordView;

/**
 * Demonstrates write-through pattern implementation using Ignite 3 transactions.
 * 
 * Music streaming services use write-through for customer data where:
 * - Data consistency is critical
 * - Updates must be immediately visible across systems
 * - Transaction guarantees are required
 */
public class WriteThroughPatternDemo {
    
    private final RecordView<Customer> customerCache;
    private final IgniteTransactions transactions;
    private final ExternalDataSource externalDataSource;
    
    public WriteThroughPatternDemo(IgniteClient client, ExternalDataSource dataSource) {
        this.customerCache = client.tables().table("CustomerCache").recordView(Customer.class);
        this.transactions = client.transactions();
        this.externalDataSource = dataSource;
    }
    
    /**
     * Updates customer information using write-through pattern.
     * 
     * Pattern implementation:
     * 1. Begin transaction
     * 2. Update cache (Ignite)
     * 3. Update external data store
     * 4. Commit transaction (both succeed or both fail)
     */
    public void updateCustomer(Customer customer) {
        transactions.runInTransaction(tx -> {
            try {
                // Step 1: Update cache within transaction
                customerCache.upsert(tx, customer);
                
                // Step 2: Update external data store
                externalDataSource.updateCustomer(customer);
                
                System.out.println("Customer " + customer.getCustomerId() + " updated in both cache and external store");
                
                // Transaction automatically commits if no exceptions
                return true;
                
            } catch (Exception e) {
                // Transaction automatically rolls back on exception
                System.err.println("Failed to update customer " + customer.getCustomerId() + ": " + e.getMessage());
                throw new RuntimeException("Failed to update customer: " + customer.getCustomerId(), e);
            }
        });
    }
    
    /**
     * Customer profile creation with write-through consistency.
     * 
     * Ensures new customer data is immediately available
     * in both cache and external systems.
     */
    public boolean createCustomer(Customer customer) {
        try {
            return transactions.runInTransaction(tx -> {
                // Check if customer already exists
                Customer existingCustomer = customerCache.get(tx, customer);
                if (existingCustomer != null) {
                    System.out.println("Customer " + customer.getCustomerId() + " already exists");
                    return false; // Customer already exists
                }
                
                // Create in external data store first
                boolean created = externalDataSource.createCustomer(customer);
                if (!created) {
                    throw new RuntimeException("Failed to create customer in external store");
                }
                
                // Add to cache
                customerCache.insert(tx, customer);
                
                System.out.println("Customer " + customer.getCustomerId() + " created successfully");
                return true;
            });
            
        } catch (Exception e) {
            System.err.println("Failed to create customer " + customer.getCustomerId() + ": " + e.getMessage());
            throw new RuntimeException("Failed to create customer: " + customer.getCustomerId(), e);
        }
    }
    
    /**
     * Batch customer updates for subscription changes.
     * 
     * Processes multiple customer updates while maintaining
     * consistency across all data stores.
     */
    public void updateSubscriptions(List<Customer> customers) {
        transactions.runInTransaction(tx -> {
            try {
                for (Customer customer : customers) {
                    // Update cache
                    customerCache.upsert(tx, customer);
                    
                    // Update external data store
                    externalDataSource.updateCustomer(customer);
                }
                
                System.out.println("Updated " + customers.size() + " customer subscriptions");
                return true;
                
            } catch (Exception e) {
                System.err.println("Failed to update customer subscriptions: " + e.getMessage());
                throw new RuntimeException("Failed to update customer subscriptions", e);
            }
        });
    }
    
    /**
     * Asynchronous write-through for high-concurrency scenarios.
     * 
     * Maintains consistency while improving throughput
     * for concurrent customer updates.
     */
    public CompletableFuture<Void> updateCustomerAsync(Customer customer) {
        return transactions.runInTransactionAsync(tx -> {
            return customerCache.upsertAsync(tx, customer)
                .thenCompose(ignored -> externalDataSource.updateCustomerAsync(customer))
                .thenApply(ignored -> null);
        });
    }
}
```

## Write-Behind Pattern Implementation

### High-Volume Event Processing

Write-behind patterns prioritize performance by updating the cache immediately and synchronizing with external data stores asynchronously. This approach handles high-volume music play events efficiently.

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates write-behind pattern implementation for high-throughput scenarios.
 * 
 * Music streaming services use write-behind for event data where:
 * - High write volume requires immediate response
 * - Eventual consistency is acceptable
 * - Batch processing optimizes external store writes
 */
public class WriteBehindPatternDemo {
    
    private final RecordView<PlayEvent> eventCache;
    private final ExternalDataSource externalDataSource;
    private final ScheduledExecutorService scheduler;
    private final AtomicLong pendingWrites = new AtomicLong(0);
    
    public WriteBehindPatternDemo(IgniteClient client, ExternalDataSource dataSource) {
        this.eventCache = client.tables().table("PlayEventCache").recordView(PlayEvent.class);
        this.externalDataSource = dataSource;
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        // Start background sync process
        startBackgroundSync();
    }
    
    /**
     * Records play event using write-behind pattern.
     * 
     * Pattern implementation:
     * 1. Update cache immediately
     * 2. Queue for background write to external store
     * 3. Return success to caller
     * 4. Background process handles external writes
     */
    public void recordPlayEvent(PlayEvent event) {
        try {
            // Step 1: Update cache immediately for fast response
            eventCache.upsert(null, event);
            
            // Step 2: Mark as pending write
            pendingWrites.incrementAndGet();
            
            System.out.println("Play event " + event.getEventId() + " cached, pending sync");
            
        } catch (Exception e) {
            System.err.println("Failed to cache play event " + event.getEventId() + ": " + e.getMessage());
            throw new RuntimeException("Failed to record play event", e);
        }
    }
    
    /**
     * Batch play event recording for high-throughput scenarios.
     * 
     * Optimizes performance by batching cache operations
     * while maintaining write-behind semantics.
     */
    public void recordPlayEvents(List<PlayEvent> events) {
        try {
            // Batch insert into cache for performance
            Map<PlayEvent, PlayEvent> eventMap = events.stream()
                .collect(Collectors.toMap(e -> e, e -> e));
            
            eventCache.upsertAll(null, eventMap);
            
            // Track pending writes
            pendingWrites.addAndGet(events.size());
            
            System.out.println("Cached " + events.size() + " play events, pending sync");
            
        } catch (Exception e) {
            System.err.println("Failed to cache play events: " + e.getMessage());
            throw new RuntimeException("Failed to record play events", e);
        }
    }
    
    /**
     * Background synchronization process.
     * 
     * Periodically syncs cached data to external store
     * in batches for optimal performance.
     */
    private void startBackgroundSync() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                syncPendingEvents();
            } catch (Exception e) {
                System.err.println("Background sync error: " + e.getMessage());
            }
        }, 5, 5, TimeUnit.SECONDS); // Sync every 5 seconds
    }
    
    private void syncPendingEvents() {
        if (pendingWrites.get() == 0) {
            return; // No pending writes
        }
        
        try {
            // Query events that need syncing (simplified - would use timestamp in practice)
            IgniteSql sql = eventCache.table().ignite().sql();
            ResultSet<SqlRow> pendingEvents = sql.execute(null,
                "SELECT * FROM PlayEventCache WHERE syncStatus = 'PENDING' LIMIT 1000");
            
            List<PlayEvent> eventsToSync = new ArrayList<>();
            while (pendingEvents.hasNext()) {
                SqlRow row = pendingEvents.next();
                PlayEvent event = mapRowToPlayEvent(row);
                eventsToSync.add(event);
            }
            
            if (eventsToSync.isEmpty()) {
                return;
            }
            
            // Batch write to external store
            boolean success = externalDataSource.writePlayEvents(eventsToSync);
            
            if (success) {
                // Mark events as synced in cache
                for (PlayEvent event : eventsToSync) {
                    event.setSyncStatus("SYNCED");
                    eventCache.upsert(null, event);
                }
                
                pendingWrites.addAndGet(-eventsToSync.size());
                System.out.println("Synced " + eventsToSync.size() + " events to external store");
                
            } else {
                System.err.println("Failed to sync events to external store, will retry");
            }
            
        } catch (Exception e) {
            System.err.println("Error during background sync: " + e.getMessage());
        }
    }
    
    /**
     * Force synchronization for critical events.
     * 
     * Provides mechanism to ensure important events
     * are written to external store immediately.
     */
    public CompletableFuture<Void> forceSyncEvent(PlayEvent event) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Write directly to external store
                boolean success = externalDataSource.writePlayEvent(event);
                
                if (success) {
                    // Update cache to mark as synced
                    event.setSyncStatus("SYNCED");
                    eventCache.upsert(null, event);
                    
                    System.out.println("Force synced event " + event.getEventId());
                } else {
                    throw new RuntimeException("Failed to force sync event " + event.getEventId());
                }
                
            } catch (Exception e) {
                System.err.println("Force sync failed for event " + event.getEventId() + ": " + e.getMessage());
                throw new RuntimeException("Force sync failed", e);
            }
        }, scheduler);
    }
    
    /**
     * Get current sync status and pending write count.
     */
    public SyncStatus getSyncStatus() {
        return new SyncStatus(pendingWrites.get(), scheduler.isShutdown());
    }
    
    /**
     * Shutdown with graceful sync completion.
     */
    public void shutdown() {
        try {
            // Complete pending syncs
            syncPendingEvents();
            
            scheduler.shutdown();
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            
            System.out.println("Write-behind service shutdown completed");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }
    }
    
    private PlayEvent mapRowToPlayEvent(SqlRow row) {
        // Map SQL row to PlayEvent object
        PlayEvent event = new PlayEvent();
        event.setEventId(row.longValue("EventId"));
        event.setUserId(row.intValue("UserId"));
        event.setTrackId(row.intValue("TrackId"));
        event.setTimestamp(row.longValue("Timestamp"));
        event.setSyncStatus(row.stringValue("syncStatus"));
        return event;
    }
}

// Helper classes
class SyncStatus {
    private final long pendingWrites;
    private final boolean shutdown;
    
    public SyncStatus(long pendingWrites, boolean shutdown) {
        this.pendingWrites = pendingWrites;
        this.shutdown = shutdown;
    }
    
    // Getters...
}

class PlayEvent {
    private long eventId;
    private int userId;
    private int trackId;
    private long timestamp;
    private String syncStatus = "PENDING";
    
    // Constructors, getters, setters...
}
```

## Advanced Caching Strategies

### Cache Warm-Up and Preloading

```java
/**
 * Cache warm-up strategies for optimal performance from application startup.
 */
public class CacheWarmUpDemo {
    
    private final KeyValueView<Integer, Artist> artistCache;
    private final KeyValueView<Integer, Album> albumCache;
    private final ExternalDataSource externalDataSource;
    
    /**
     * Preload popular artists and albums into cache during startup.
     */
    public CompletableFuture<Void> warmUpCache() {
        return CompletableFuture.allOf(
            warmUpPopularArtists(),
            warmUpPopularAlbums(),
            warmUpRecentlyPlayed()
        );
    }
    
    private CompletableFuture<Void> warmUpPopularArtists() {
        return CompletableFuture.runAsync(() -> {
            try {
                List<Artist> popularArtists = externalDataSource.getPopularArtists(100);
                
                Map<Integer, Artist> artistMap = popularArtists.stream()
                    .collect(Collectors.toMap(Artist::getArtistId, artist -> artist));
                
                artistCache.putAll(null, artistMap);
                
                System.out.println("Warmed up " + popularArtists.size() + " popular artists");
                
            } catch (Exception e) {
                System.err.println("Failed to warm up popular artists: " + e.getMessage());
            }
        });
    }
    
    private CompletableFuture<Void> warmUpPopularAlbums() {
        return CompletableFuture.runAsync(() -> {
            try {
                List<Album> popularAlbums = externalDataSource.getPopularAlbums(200);
                
                Map<Integer, Album> albumMap = popularAlbums.stream()
                    .collect(Collectors.toMap(Album::getAlbumId, album -> album));
                
                albumCache.putAll(null, albumMap);
                
                System.out.println("Warmed up " + popularAlbums.size() + " popular albums");
                
            } catch (Exception e) {
                System.err.println("Failed to warm up popular albums: " + e.getMessage());
            }
        });
    }
    
    private CompletableFuture<Void> warmUpRecentlyPlayed() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Load recently played tracks for active users
                List<Integer> activeUsers = externalDataSource.getActiveUsers(1000);
                
                for (Integer userId : activeUsers) {
                    List<Integer> recentTracks = externalDataSource.getRecentlyPlayedTracks(userId, 10);
                    // Cache recent tracks for quick access
                    preloadTracksForUser(userId, recentTracks);
                }
                
                System.out.println("Warmed up recently played tracks for " + activeUsers.size() + " users");
                
            } catch (Exception e) {
                System.err.println("Failed to warm up recently played tracks: " + e.getMessage());
            }
        });
    }
    
    private void preloadTracksForUser(Integer userId, List<Integer> trackIds) {
        // Implementation would cache user's recent tracks
        // for immediate access when they return to the app
    }
}
```

### Cache Invalidation Strategies

```java
/**
 * Comprehensive cache invalidation strategies for data consistency.
 */
public class CacheInvalidationDemo {
    
    private final KeyValueView<Integer, Artist> artistCache;
    private final KeyValueView<Integer, Album> albumCache;
    private final KeyValueView<String, Object> metadataCache;
    
    /**
     * Invalidate related cache entries when artist information changes.
     */
    public void invalidateArtistData(Integer artistId) {
        try {
            // Remove artist from cache
            artistCache.remove(null, artistId);
            
            // Find and remove related albums
            List<Integer> relatedAlbums = findAlbumsByArtist(artistId);
            for (Integer albumId : relatedAlbums) {
                albumCache.remove(null, albumId);
            }
            
            // Remove artist-related metadata
            metadataCache.remove(null, "artist_" + artistId + "_stats");
            metadataCache.remove(null, "artist_" + artistId + "_recommendations");
            
            System.out.println("Invalidated cache entries for artist " + artistId + 
                " and " + relatedAlbums.size() + " related albums");
            
        } catch (Exception e) {
            System.err.println("Failed to invalidate artist data: " + e.getMessage());
        }
    }
    
    /**
     * Pattern-based cache invalidation for related data.
     */
    public void invalidateUserData(Integer userId) {
        try {
            // Use SQL to find and remove user-related cache entries
            IgniteSql sql = artistCache.table().ignite().sql();
            
            // Remove user's playlist cache
            sql.execute(null, "DELETE FROM PlaylistCache WHERE UserId = ?", userId);
            
            // Remove user's recommendation cache  
            sql.execute(null, "DELETE FROM RecommendationCache WHERE UserId = ?", userId);
            
            // Remove user's listening history cache
            sql.execute(null, "DELETE FROM ListeningHistoryCache WHERE UserId = ?", userId);
            
            System.out.println("Invalidated all cache entries for user " + userId);
            
        } catch (Exception e) {
            System.err.println("Failed to invalidate user data: " + e.getMessage());
        }
    }
    
    private List<Integer> findAlbumsByArtist(Integer artistId) {
        // Query to find albums by artist
        try {
            IgniteSql sql = albumCache.table().ignite().sql();
            ResultSet<SqlRow> results = sql.execute(null, 
                "SELECT AlbumId FROM Album WHERE ArtistId = ?", artistId);
            
            List<Integer> albumIds = new ArrayList<>();
            while (results.hasNext()) {
                albumIds.add(results.next().intValue("AlbumId"));
            }
            
            return albumIds;
            
        } catch (Exception e) {
            System.err.println("Failed to find albums for artist " + artistId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
```

Caching strategies transform how music applications handle data access patterns. By implementing appropriate caching patterns for different data types and access patterns, applications can deliver consistent performance while maintaining data consistency across distributed systems.

## Next Steps

Understanding caching strategies prepares you for comprehensive query optimization and performance tuning:

- **[Chapter 5.3: Query Performance and Index Optimization](03-query-performance.md)** - Master SQL performance tuning and indexing strategies that work with your caching patterns

- **[Chapter 6.1: Production Deployment Patterns](../06-production-concerns/01-deployment-patterns.md)** - Learn how to deploy and manage these performance optimizations in production environments
