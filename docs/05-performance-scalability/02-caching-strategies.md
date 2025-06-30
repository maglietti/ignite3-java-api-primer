# Chapter 5.2: Caching Strategies and Performance Optimization

Your popular tracks are being loaded from disk storage repeatedly because cache misses are creating database bottlenecks during peak traffic. Users experience response delays of 500ms+ when browsing the music catalog, while your database servers struggle under read pressure from the same data requests hitting storage systems dozens of times per second.

This happens because traditional application architectures treat caching as an afterthought rather than designing for distributed data access patterns from the start. Your music streaming platform processes millions of catalog requests daily, but without intelligent caching strategies, each popular artist lookup becomes a costly database operation multiplied across your entire user base.

The solution involves implementing coordinated caching patterns that eliminate redundant data access while maintaining consistency across distributed systems. Ignite 3's distributed caching capabilities provide the foundation for cache-aside patterns that serve popular content instantly, write-through patterns that maintain data consistency across updates, and write-behind patterns that handle high-volume event processing without database contention.

## How Cache-Aside Patterns Eliminate Database Pressure

Your music catalog contains 50,000+ artists, but analysis shows that just 500 popular artists account for 80% of browse requests. These repetitive lookups create unnecessary database load because each user session independently queries the same artist information that was already loaded by previous sessions.

Cache-aside patterns solve this by placing your application in control of data access decisions. When a user browses artists, your application first checks Ignite's distributed cache for the requested data. Cache hits return instantly from memory across your cluster, while cache misses trigger one database load that populates the cache for all subsequent requests.

This pattern works because music catalog data has predictable access characteristics: popular content gets accessed frequently while deep catalog items see sporadic requests. Your application can implement intelligent cache population strategies that load popular artists proactively while allowing less popular content to load on-demand.

The cache-aside implementation gives you complete control over cache behavior. You decide what data to cache, when to load it, and how to handle invalidation when catalog information changes. This control becomes critical when balancing cache memory usage against database load reduction.

### Cache-Aside Implementation

The implementation centers on the KeyValueView API, which provides direct access to Ignite's distributed key-value storage. Your application becomes responsible for cache management decisions, checking the cache first and falling back to database loads only when necessary.

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

The cache-aside pattern transforms catalog browsing performance by eliminating redundant database queries. Popular artists load once and serve thousands of requests from distributed memory, while your database handles only the unique lookups that haven't been cached yet.

This approach reduces database load by 60-80% for typical music catalog access patterns while maintaining complete application control over caching behavior. The pattern works best for read-heavy workloads where cache hit rates justify the implementation complexity.

## How Write-Through Patterns Maintain Data Consistency

Customer profile updates create a different challenge: data must remain consistent across all systems immediately after changes occur. When a user updates their subscription status, that change needs to be visible instantly in both your cache and persistent storage to prevent billing errors or access control issues.

Write-through patterns solve this by coordinating updates across multiple data stores within the same transaction. Unlike cache-aside patterns that handle reads independently, write-through operations must succeed in both the cache and external database or fail completely.

This pattern eliminates the consistency gaps that occur when cache and database updates happen independently. User profile changes, payment information updates, and subscription modifications all require this level of consistency to maintain system integrity.

### Write-Through Implementation

The implementation relies on Ignite's transaction capabilities to coordinate updates across cache and external systems. The RecordView API provides the interface for managing customer data with full ACID transaction support.

```java
import org.apache.ignite.transactions.IgniteTransactions;
import org.apache.ignite.table.RecordView;

/**
 * Write-through pattern implementation using Ignite 3 transactions.
 * 
 * Coordinates customer data updates across cache and external systems
 * within the same transaction boundary for immediate consistency.
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

Write-through patterns provide the strongest consistency guarantees for critical customer data updates. All systems see changes immediately after transactions commit, eliminating the data synchronization issues that cause billing errors and user access problems.

The pattern trades some write performance for consistency guarantees, making it ideal for customer profiles, subscription changes, and payment information where data accuracy takes priority over raw throughput.

## How Write-Behind Patterns Handle High-Volume Events

Music streaming generates massive event volumes that would overwhelm traditional write-through approaches. Play events, skip tracking, and user interaction data arrive at rates of 10,000+ events per second during peak usage, creating database bottlenecks if every event requires immediate persistence.

Write-behind patterns solve this by accepting events into cache immediately while batching database writes asynchronously. Your application responds to event submissions instantly, while background processes handle the database synchronization efficiently.

This approach prevents event processing delays from impacting user experience. Users don't wait for play event logging to complete before their music starts, and high event volumes don't create database contention that affects other application features.

### Write-Behind Implementation

The implementation uses scheduled background processing to sync cached events to external storage in batches. The RecordView API handles immediate event caching while executor services manage the asynchronous database writes.

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Write-behind pattern implementation for high-throughput event processing.
 * 
 * Provides immediate response to event submissions while batching
 * database writes for optimal performance and reduced contention.
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

Write-behind patterns enable high-throughput event processing by decoupling immediate response from database persistence. Events cache instantly while background processes handle database synchronization efficiently, preventing event volumes from creating system bottlenecks.

The pattern provides eventual consistency for scenarios where immediate persistence isn't critical, allowing systems to maintain responsiveness under high load while ensuring all events eventually reach persistent storage.

## Optimizing Cache Performance Through Preloading

Cold cache startup creates performance problems when applications restart because popular data needs to reload from database storage. Users experience slow responses during the initial minutes until cache hit rates improve, creating poor experiences during deployment cycles or system maintenance.

Cache warm-up strategies solve this by preloading popular data during application startup. Instead of waiting for user requests to populate the cache organically, applications can identify and load high-traffic data proactively based on usage analytics and historical access patterns.

### Cache Warm-Up Implementation

The implementation uses CompletableFuture for parallel cache loading during startup. Applications can load popular artists, trending albums, and user-specific data concurrently to minimize warm-up time.

```java
/**
 * Cache warm-up implementation for eliminating cold start performance issues.
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

Cache warm-up eliminates the performance degradation that occurs during application restarts by proactively loading popular content before user requests arrive. This approach reduces cold start impact from minutes to seconds while ensuring consistent response times from application launch.

## Managing Cache Consistency Through Invalidation

Cache invalidation becomes critical when data changes in external systems need to be reflected immediately in your distributed cache. Stale artist information, outdated album metadata, or incorrect user profile data create user experience problems and potential system errors.

Cache invalidation strategies must handle the cascading relationships between different data types. When artist information changes, related albums, tracks, and recommendation data also become stale and need coordinated invalidation to maintain system consistency.

### Cache Invalidation Implementation

The implementation uses SQL queries to identify and remove related cache entries when data changes. This approach handles the cascading invalidation required for maintaining consistency across interconnected data.

```java
/**
 * Cache invalidation implementation for maintaining data consistency.
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

Cache invalidation ensures data consistency by coordinating updates across all related cache entries when source data changes. This prevents stale data from causing user experience issues while maintaining the performance benefits of distributed caching.

The combination of cache-aside patterns for popular content, write-through patterns for critical data, and write-behind patterns for high-volume events creates a complete caching architecture that eliminates database bottlenecks while maintaining appropriate consistency guarantees for different data types.

These caching strategies reduce database load by 60-80% while providing sub-50ms response times for cached data access. The patterns work together to handle the full spectrum of data access requirements in distributed music streaming applications, from catalog browsing to real-time event processing.

## Next Steps

Caching optimization sets the foundation for comprehensive performance tuning that includes query optimization and indexing strategies:

**[Chapter 5.3: Query Performance and Index Optimization](03-query-performance.md)** details SQL performance tuning techniques that work with your caching patterns to eliminate remaining database bottlenecks.

**[Chapter 6.1: Production Deployment Patterns](../06-production-concerns/01-deployment-patterns.md)** covers deploying and managing these performance optimizations in production environments with proper monitoring and scaling strategies.
