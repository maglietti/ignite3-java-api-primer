# Module 09: Caching Patterns - Java Implementations

## Overview

Music streaming platforms process millions of requests daily, from catalog browsing to real-time play tracking. The difference between fast and slow responses often determines user satisfaction. While traditional caching solutions address individual use cases, distributed systems require coordinated data management patterns that ensure consistency across multiple data stores.

Ignite 3's Table and SQL APIs provide the foundation for implementing three essential caching patterns: cache-aside for read-heavy workloads, write-through for data consistency, and write-behind for high-throughput scenarios. These patterns leverage Ignite's distributed architecture while maintaining standard Java interfaces.

This module demonstrates how music streaming services implement these patterns using Ignite 3 Java APIs, progressing from simple catalog caching to complex data synchronization workflows.

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
            return cached;
        }
        
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
            return cachedResults;
        }
        
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
        
        // Invalidate cache entry
        artistCache.remove(null, artist.getArtistId());
        
        // Alternative: Update cache with new values
        // artistCache.put(null, artist.getArtistId(), artist);
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
                
                // Transaction automatically commits if no exceptions
                
            } catch (Exception e) {
                // Transaction automatically rolls back on exception
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
                    return false; // Customer already exists
                }
                
                // Create in external data store first
                boolean created = externalDataSource.createCustomer(customer);
                if (!created) {
                    throw new RuntimeException("Failed to create customer in external store");
                }
                
                // Add to cache
                customerCache.insert(tx, customer);
                
                return true;
            });
            
        } catch (Exception e) {
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
                
            } catch (Exception e) {
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
                .thenCompose(ignored -> 
                    externalDataSource.updateCustomerAsync(customer)
                )
                .thenApply(ignored -> null);
        });
    }
    
    /**
     * Read operations with write-through cache.
     * 
     * Reads from cache first, falling back to external source
     * while maintaining data consistency.
     */
    public Customer getCustomer(int customerId) {
        Customer keyRecord = Customer.builder()
            .customerId(customerId)
            .build();
        
        // Try cache first
        Customer customer = customerCache.get(null, keyRecord);
        if (customer != null) {
            return customer;
        }
        
        // Load from external source and cache result
        customer = externalDataSource.loadCustomer(customerId);
        if (customer != null) {
            customerCache.upsert(null, customer);
        }
        
        return customer;
    }
}
```

### Write-Through Pattern Characteristics

**Immediate Consistency**: All updates happen synchronously
**Transaction Support**: ACID guarantees across cache and external store
**Higher Latency**: Writes are slower due to synchronous updates
**Data Integrity**: Strong consistency prevents data corruption

## Write-Behind Pattern Implementation

### Analytics Data Buffering

Write-behind patterns optimize for high-throughput scenarios by updating cache immediately while deferring external data store writes. Music streaming analytics require this pattern to handle millions of play events without blocking user experience.

```java
/**
 * Demonstrates write-behind pattern implementation using Ignite 3 async operations.
 * 
 * Music streaming services use write-behind for analytics data where:
 * - High write throughput is required
 * - Immediate consistency is not critical
 * - Performance is prioritized over immediate durability
 */
public class WriteBehindPatternDemo {
    
    private final KeyValueView<String, PlayEvent> playEventCache;
    private final ScheduledExecutorService scheduler;
    private final ExternalDataSource externalDataSource;
    private final Queue<PlayEvent> writeBuffer;
    private final int batchSize = 1000;
    private final long flushIntervalMs = 5000; // 5 seconds
    
    public WriteBehindPatternDemo(KeyValueView<String, PlayEvent> playEventCache,
                                 ExternalDataSource externalDataSource) {
        this.playEventCache = playEventCache;
        this.externalDataSource = externalDataSource;
        this.writeBuffer = new ConcurrentLinkedQueue<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        // Start background flush process
        startBackgroundFlush();
    }
    
    /**
     * Records play event using write-behind pattern.
     * 
     * Pattern implementation:
     * 1. Update cache immediately (fast response)
     * 2. Queue for background write to external store
     * 3. Return immediately without waiting for external write
     */
    public CompletableFuture<Void> recordPlayEvent(PlayEvent playEvent) {
        String eventId = generateEventId(playEvent);
        
        // Step 1: Update cache immediately
        return playEventCache.putAsync(null, eventId, playEvent)
            .thenAccept(ignored -> {
                // Step 2: Queue for background processing
                writeBuffer.offer(playEvent);
            });
    }
    
    /**
     * Batch play event recording for high-throughput scenarios.
     * 
     * Optimizes performance by batching cache operations
     * while maintaining write-behind semantics.
     */
    public CompletableFuture<Void> recordPlayEvents(List<PlayEvent> playEvents) {
        // Prepare cache updates
        Map<String, PlayEvent> cacheUpdates = playEvents.stream()
            .collect(Collectors.toMap(
                this::generateEventId,
                Function.identity()
            ));
        
        // Update cache in batch
        return playEventCache.putAllAsync(null, cacheUpdates)
            .thenAccept(ignored -> {
                // Queue all events for background processing
                writeBuffer.addAll(playEvents);
            });
    }
    
    /**
     * Background flush process for write-behind operations.
     * 
     * Periodically flushes buffered writes to external data store
     * with configurable batch size and flush intervals.
     */
    private void startBackgroundFlush() {
        // Time-based flush
        scheduler.scheduleAtFixedRate(
            this::flushPendingWrites,
            flushIntervalMs,
            flushIntervalMs,
            TimeUnit.MILLISECONDS
        );
        
        // Size-based flush monitoring
        scheduler.scheduleAtFixedRate(
            this::checkBufferSize,
            1000, // Check every second
            1000,
            TimeUnit.MILLISECONDS
        );
    }
    
    /**
     * Flushes pending writes to external data store.
     * 
     * Processes buffered events in batches for optimal
     * external data store performance.
     */
    private void flushPendingWrites() {
        List<PlayEvent> eventsToFlush = new ArrayList<>();
        
        // Drain up to batchSize events from buffer
        PlayEvent event;
        while (eventsToFlush.size() < batchSize && (event = writeBuffer.poll()) != null) {
            eventsToFlush.add(event);
        }
        
        if (eventsToFlush.isEmpty()) {
            return;
        }
        
        try {
            // Write to external data store
            externalDataSource.batchInsertPlayEvents(eventsToFlush);
            
        } catch (Exception e) {
            // On failure, consider re-queuing events or logging for manual intervention
            writeBuffer.addAll(eventsToFlush);
            throw new RuntimeException("Failed to flush play events to external store", e);
        }
    }
    
    /**
     * Checks buffer size and triggers immediate flush if needed.
     * 
     * Prevents memory overflow by flushing when buffer
     * reaches configured batch size.
     */
    private void checkBufferSize() {
        if (writeBuffer.size() >= batchSize) {
            flushPendingWrites();
        }
    }
    
    /**
     * User activity tracking with write-behind pattern.
     * 
     * Records user interactions immediately in cache
     * while deferring analytics processing.
     */
    public CompletableFuture<Void> trackUserActivity(int customerId, String activityType, Map<String, Object> metadata) {
        UserActivity activity = UserActivity.builder()
            .customerId(customerId)
            .activityType(activityType)
            .timestamp(LocalDateTime.now())
            .metadata(metadata)
            .build();
        
        String activityId = generateActivityId(activity);
        
        // Update cache immediately
        return playEventCache.putAsync(null, activityId, 
            convertToPlayEvent(activity)) // Convert for storage
            .thenAccept(ignored -> {
                // Queue for background analytics processing
                scheduleAnalyticsProcessing(activity);
            });
    }
    
    /**
     * Graceful shutdown with pending write flush.
     * 
     * Ensures all buffered writes are processed before
     * application shutdown.
     */
    public void shutdown() {
        try {
            // Flush all pending writes
            while (!writeBuffer.isEmpty()) {
                flushPendingWrites();
            }
            
            // Shutdown scheduler
            scheduler.shutdown();
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Manual flush trigger for testing or immediate consistency requirements.
     * 
     * Allows applications to force immediate write-behind
     * flush when necessary.
     */
    public void forceFlush() {
        flushPendingWrites();
    }
    
    private String generateEventId(PlayEvent playEvent) {
        return String.format("%d_%d_%d", 
            playEvent.getCustomerId(),
            playEvent.getTrackId(),
            playEvent.getTimestamp().toEpochSecond(ZoneOffset.UTC)
        );
    }
    
    private void scheduleAnalyticsProcessing(UserActivity activity) {
        // Schedule background analytics processing
        scheduler.schedule(() -> {
            try {
                externalDataSource.processUserActivityAnalytics(activity);
            } catch (Exception e) {
                // Log error but don't fail the user request
                System.err.println("Analytics processing failed for activity: " + activity);
            }
        }, 1, TimeUnit.SECONDS);
    }
}
```

### Write-Behind Pattern Characteristics

**High Throughput**: Immediate cache updates, deferred external writes
**Eventual Consistency**: External data store updates happen asynchronously  
**Data Risk**: Potential data loss if cache fails before flush
**Performance**: Optimized for write-heavy workloads

## Advanced Caching Pattern Combinations

### Multi-Tier Caching Strategy

Real-world applications combine multiple caching patterns based on data characteristics and access patterns.

```java
/**
 * Demonstrates combined caching patterns for comprehensive data management.
 * 
 * Music streaming platforms use different patterns for different data types:
 * - Cache-aside for catalog data (read-heavy)
 * - Write-through for customer data (consistency-critical)  
 * - Write-behind for analytics data (write-heavy)
 */
public class MultiTierCachingDemo {
    
    private final CacheAsidePatternDemo catalogCache;
    private final WriteThroughPatternDemo customerCache;
    private final WriteBehindPatternDemo analyticsCache;
    
    /**
     * Processes music streaming request using appropriate caching pattern.
     * 
     * Demonstrates how different patterns work together in a
     * single application request flow.
     */
    public StreamingResponse processStreamingRequest(int customerId, int trackId) {
        try {
            // Step 1: Get customer data (write-through pattern)
            Customer customer = customerCache.getCustomer(customerId);
            if (customer == null) {
                return StreamingResponse.customerNotFound();
            }
            
            // Step 2: Get track information (cache-aside pattern)
            Track track = getTrackWithCaching(trackId);
            if (track == null) {
                return StreamingResponse.trackNotFound();
            }
            
            // Step 3: Check subscription status (write-through cached data)
            if (!customer.hasActiveSubscription()) {
                return StreamingResponse.subscriptionRequired();
            }
            
            // Step 4: Record play event (write-behind pattern)
            PlayEvent playEvent = PlayEvent.builder()
                .customerId(customerId)
                .trackId(trackId)
                .timestamp(LocalDateTime.now())
                .build();
            
            analyticsCache.recordPlayEvent(playEvent);
            
            // Step 5: Return streaming response
            return StreamingResponse.success(track.getStreamingUrl());
            
        } catch (Exception e) {
            return StreamingResponse.error("Processing failed: " + e.getMessage());
        }
    }
    
    /**
     * Composite data loading using multiple caching patterns.
     * 
     * Loads related data efficiently by leveraging different
     * caching strategies for different entity types.
     */
    public PlaylistDetails getPlaylistDetails(int playlistId) {
        // Load playlist metadata (cache-aside)
        Playlist playlist = catalogCache.getPlaylist(playlistId);
        if (playlist == null) {
            return null;
        }
        
        // Load track information (cache-aside, batch optimized)
        Map<Integer, Track> tracks = catalogCache.getTracksForPlaylist(playlist.getTrackIds());
        
        // Load owner information (write-through)
        Customer owner = customerCache.getCustomer(playlist.getOwnerId());
        
        // Record playlist access (write-behind)
        PlaylistAccessEvent accessEvent = PlaylistAccessEvent.builder()
            .playlistId(playlistId)
            .timestamp(LocalDateTime.now())
            .build();
        
        analyticsCache.recordPlaylistAccess(accessEvent);
        
        return PlaylistDetails.builder()
            .playlist(playlist)
            .tracks(tracks)
            .owner(owner)
            .build();
    }
    
    private Track getTrackWithCaching(int trackId) {
        // Delegate to cache-aside pattern implementation
        return catalogCache.getTrack(trackId);
    }
}
```

## Performance Optimization Techniques

### Async Pattern Orchestration

Combining async operations across different caching patterns improves overall application performance.

```java
/**
 * Demonstrates async optimization techniques across caching patterns.
 * 
 * Orchestrates multiple caching operations concurrently to
 * minimize total request processing time.
 */
public class AsyncCachingOrchestration {
    
    private final CacheAsidePatternDemo catalogCache;
    private final WriteThroughPatternDemo customerCache;
    private final WriteBehindPatternDemo analyticsCache;
    
    /**
     * Async streaming request processing with parallel caching operations.
     * 
     * Executes independent caching operations concurrently
     * to minimize total response time.
     */
    public CompletableFuture<StreamingResponse> processStreamingRequestAsync(int customerId, int trackId) {
        // Start async operations concurrently
        CompletableFuture<Customer> customerFuture = 
            CompletableFuture.supplyAsync(() -> customerCache.getCustomer(customerId));
        
        CompletableFuture<Track> trackFuture = 
            catalogCache.getTrackAsync(trackId);
        
        // Combine results when both complete
        return customerFuture.thenCombine(trackFuture, (customer, track) -> {
            if (customer == null) {
                return StreamingResponse.customerNotFound();
            }
            
            if (track == null) {
                return StreamingResponse.trackNotFound();
            }
            
            if (!customer.hasActiveSubscription()) {
                return StreamingResponse.subscriptionRequired();
            }
            
            // Record play event asynchronously (fire-and-forget)
            PlayEvent playEvent = PlayEvent.builder()
                .customerId(customerId)
                .trackId(trackId)
                .timestamp(LocalDateTime.now())
                .build();
            
            analyticsCache.recordPlayEvent(playEvent)
                .exceptionally(ex -> {
                    System.err.println("Failed to record play event: " + ex.getMessage());
                    return null;
                });
            
            return StreamingResponse.success(track.getStreamingUrl());
        });
    }
    
    /**
     * Batch processing optimization with async caching patterns.
     * 
     * Processes multiple requests concurrently while maintaining
     * appropriate caching semantics for each data type.
     */
    public CompletableFuture<List<RecommendationResult>> generateRecommendationsAsync(
            int customerId, List<Integer> seedTrackIds) {
        
        // Load customer profile (write-through)
        CompletableFuture<Customer> customerFuture = 
            CompletableFuture.supplyAsync(() -> customerCache.getCustomer(customerId));
        
        // Load seed tracks (cache-aside, batch optimized)
        CompletableFuture<Map<Integer, Track>> tracksFuture = 
            CompletableFuture.supplyAsync(() -> catalogCache.getTracksById(seedTrackIds));
        
        // Combine and generate recommendations
        return customerFuture.thenCombine(tracksFuture, (customer, tracks) -> {
            if (customer == null || tracks.isEmpty()) {
                return Collections.emptyList();
            }
            
            // Generate recommendations based on customer preferences and seed tracks
            List<RecommendationResult> recommendations = 
                generateRecommendations(customer, tracks.values());
            
            // Record recommendation generation (write-behind)
            RecommendationEvent event = RecommendationEvent.builder()
                .customerId(customerId)
                .seedTrackIds(seedTrackIds)
                .recommendationCount(recommendations.size())
                .timestamp(LocalDateTime.now())
                .build();
            
            analyticsCache.recordRecommendationEvent(event)
                .exceptionally(ex -> {
                    System.err.println("Failed to record recommendation event: " + ex.getMessage());
                    return null;
                });
            
            return recommendations;
        });
    }
}
```

## Error Handling and Fault Tolerance

### Graceful Degradation Strategies

Robust caching implementations handle failures gracefully while maintaining application functionality.

```java
/**
 * Demonstrates error handling and fault tolerance across caching patterns.
 * 
 * Implements graceful degradation strategies that maintain
 * application functionality when caching operations fail.
 */
public class FaultTolerantCaching {
    
    private final CacheAsidePatternDemo catalogCache;
    private final WriteThroughPatternDemo customerCache;
    private final WriteBehindPatternDemo analyticsCache;
    private final CircuitBreaker circuitBreaker;
    
    /**
     * Fault-tolerant streaming request processing.
     * 
     * Implements fallback strategies for each caching pattern
     * to ensure application continues working during failures.
     */
    public StreamingResponse processStreamingRequestWithFallback(int customerId, int trackId) {
        try {
            // Customer data with fallback to external source
            Customer customer = getCustomerWithFallback(customerId);
            if (customer == null) {
                return StreamingResponse.customerNotFound();
            }
            
            // Track data with fallback to external source
            Track track = getTrackWithFallback(trackId);
            if (track == null) {
                return StreamingResponse.trackNotFound();
            }
            
            // Subscription check
            if (!customer.hasActiveSubscription()) {
                return StreamingResponse.subscriptionRequired();
            }
            
            // Analytics recording with error tolerance
            recordPlayEventWithFallback(customerId, trackId);
            
            return StreamingResponse.success(track.getStreamingUrl());
            
        } catch (Exception e) {
            return StreamingResponse.error("Processing failed: " + e.getMessage());
        }
    }
    
    /**
     * Customer data retrieval with write-through fallback.
     * 
     * Falls back to external data source if cache operations fail
     * while maintaining data consistency requirements.
     */
    private Customer getCustomerWithFallback(int customerId) {
        try {
            return customerCache.getCustomer(customerId);
            
        } catch (Exception e) {
            // Fallback to external source for critical customer data
            return externalDataSource.loadCustomer(customerId);
        }
    }
    
    /**
     * Track data retrieval with cache-aside fallback.
     * 
     * Implements multiple fallback levels for catalog data
     * to maintain service availability.
     */
    private Track getTrackWithFallback(int trackId) {
        try {
            // Try cache-aside pattern first
            return catalogCache.getTrack(trackId);
            
        } catch (Exception e) {
            try {
                // Fallback 1: Direct external source access
                return externalDataSource.loadTrack(trackId);
                
            } catch (Exception fallbackException) {
                // Fallback 2: Minimal track information for continued service
                return createMinimalTrack(trackId);
            }
        }
    }
    
    /**
     * Analytics recording with write-behind error tolerance.
     * 
     * Implements multiple fallback strategies for analytics data
     * to prevent user experience impact from analytics failures.
     */
    private void recordPlayEventWithFallback(int customerId, int trackId) {
        try {
            PlayEvent playEvent = PlayEvent.builder()
                .customerId(customerId)
                .trackId(trackId)
                .timestamp(LocalDateTime.now())
                .build();
            
            analyticsCache.recordPlayEvent(playEvent);
            
        } catch (Exception e) {
            try {
                // Fallback 1: Direct external analytics service
                externalDataSource.recordPlayEventDirect(customerId, trackId);
                
            } catch (Exception fallbackException) {
                // Fallback 2: Log for offline processing
                logPlayEventForOfflineProcessing(customerId, trackId);
            }
        }
    }
    
    /**
     * Circuit breaker pattern for cache operations.
     * 
     * Prevents cascading failures by temporarily bypassing
     * failing cache operations while monitoring recovery.
     */
    public <T> T executeWithCircuitBreaker(Supplier<T> cacheOperation, Supplier<T> fallbackOperation) {
        if (circuitBreaker.isOpen()) {
            // Circuit is open - execute fallback directly
            return fallbackOperation.get();
        }
        
        try {
            T result = cacheOperation.get();
            circuitBreaker.recordSuccess();
            return result;
            
        } catch (Exception e) {
            circuitBreaker.recordFailure();
            
            if (circuitBreaker.shouldTrip()) {
                circuitBreaker.trip();
            }
            
            // Execute fallback operation
            return fallbackOperation.get();
        }
    }
    
    private Track createMinimalTrack(int trackId) {
        return Track.builder()
            .trackId(trackId)
            .name("Track " + trackId)
            .streamingUrl("fallback://track/" + trackId)
            .build();
    }
    
    private void logPlayEventForOfflineProcessing(int customerId, int trackId) {
        String logEntry = String.format("PLAY_EVENT: customer=%d, track=%d, timestamp=%s", 
            customerId, trackId, Instant.now());
        System.out.println(logEntry);
    }
}
```

## Testing Strategies

### Unit Testing Caching Patterns

Testing caching patterns requires validating both cache operations and external data store interactions.

```java
/**
 * Demonstrates testing strategies for caching pattern implementations.
 * 
 * Covers unit testing approaches for cache-aside, write-through,
 * and write-behind patterns with proper mocking and verification.
 */
@ExtendWith(MockitoExtension.class)
public class CachingPatternsTest {
    
    @Mock
    private KeyValueView<Integer, Artist> artistCache;
    
    @Mock
    private RecordView<Customer> customerCache;
    
    @Mock
    private IgniteTransactions transactions;
    
    @Mock
    private ExternalDataSource externalDataSource;
    
    private CacheAsidePatternDemo cacheAsideDemo;
    private WriteThroughPatternDemo writeThroughDemo;
    
    @BeforeEach
    void setUp() {
        cacheAsideDemo = new CacheAsidePatternDemo(artistCache, externalDataSource);
        writeThroughDemo = new WriteThroughPatternDemo(customerCache, transactions, externalDataSource);
    }
    
    /**
     * Tests cache-aside pattern cache hit scenario.
     */
    @Test
    void testCacheAsidePatternCacheHit() {
        // Given
        int artistId = 1;
        Artist cachedArtist = Artist.builder()
            .artistId(artistId)
            .name("Test Artist")
            .build();
        
        when(artistCache.get(null, artistId)).thenReturn(cachedArtist);
        
        // When
        Artist result = cacheAsideDemo.getArtist(artistId);
        
        // Then
        assertThat(result).isEqualTo(cachedArtist);
        verify(artistCache).get(null, artistId);
        verifyNoInteractions(externalDataSource);
    }
    
    /**
     * Tests cache-aside pattern cache miss scenario.
     */
    @Test
    void testCacheAsidePatternCacheMiss() {
        // Given
        int artistId = 1;
        Artist loadedArtist = Artist.builder()
            .artistId(artistId)
            .name("Loaded Artist")
            .build();
        
        when(artistCache.get(null, artistId)).thenReturn(null);
        when(externalDataSource.loadArtist(artistId)).thenReturn(loadedArtist);
        
        // When
        Artist result = cacheAsideDemo.getArtist(artistId);
        
        // Then
        assertThat(result).isEqualTo(loadedArtist);
        verify(artistCache).get(null, artistId);
        verify(externalDataSource).loadArtist(artistId);
        verify(artistCache).put(null, artistId, loadedArtist);
    }
    
    /**
     * Tests write-through pattern transaction success.
     */
    @Test
    void testWriteThroughPatternSuccess() {
        // Given
        Customer customer = Customer.builder()
            .customerId(1)
            .name("Test Customer")
            .build();
        
        doAnswer(invocation -> {
            Consumer<Transaction> transactionConsumer = invocation.getArgument(0);
            Transaction mockTransaction = mock(Transaction.class);
            transactionConsumer.accept(mockTransaction);
            return null;
        }).when(transactions).runInTransaction(any(Consumer.class));
        
        // When
        assertDoesNotThrow(() -> writeThroughDemo.updateCustomer(customer));
        
        // Then
        verify(transactions).runInTransaction(any(Consumer.class));
    }
    
    /**
     * Tests write-behind pattern async operations.
     */
    @Test
    void testWriteBehindPatternAsync() {
        // Given
        KeyValueView<String, PlayEvent> playEventCache = mock(KeyValueView.class);
        ExternalDataSource externalDataSource = mock(ExternalDataSource.class);
        
        PlayEvent playEvent = PlayEvent.builder()
            .customerId(1)
            .trackId(100)
            .timestamp(LocalDateTime.now())
            .build();
        
        when(playEventCache.putAsync(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));
        
        WriteBehindPatternDemo writeBehindDemo = 
            new WriteBehindPatternDemo(playEventCache, externalDataSource);
        
        // When
        CompletableFuture<Void> result = writeBehindDemo.recordPlayEvent(playEvent);
        
        // Then
        assertThat(result).succeedsWithin(Duration.ofSeconds(1));
        verify(playEventCache).putAsync(any(), any(), eq(playEvent));
    }
    
    /**
     * Tests error handling in caching patterns.
     */
    @Test
    void testCacheAsidePatternErrorHandling() {
        // Given
        int artistId = 1;
        when(artistCache.get(null, artistId)).thenThrow(new RuntimeException("Cache failure"));
        
        // When & Then
        assertThrows(RuntimeException.class, () -> cacheAsideDemo.getArtist(artistId));
        verify(artistCache).get(null, artistId);
        verifyNoInteractions(externalDataSource);
    }
}
```

## Best Practices and Recommendations

### Pattern Selection Guidelines

**Use Cache-Aside When**:
- Read operations significantly outnumber writes
- Application can tolerate eventual consistency
- Cache misses are acceptable for performance
- Applications need control over cache management

**Use Write-Through When**:
- Data consistency is critical
- Writes require immediate visibility
- Transaction guarantees are needed
- Application can accept higher write latency

**Use Write-Behind When**:
- High write throughput is required
- Application can tolerate eventual consistency
- Performance is more important than immediate durability
- Batch processing is acceptable for external stores

### Performance Optimization

**Batch Operations**: Use bulk operations for improved performance
**Async Processing**: Leverage CompletableFuture for non-blocking operations
**Circuit Breakers**: Prevent cascading failures with circuit breaker patterns
**Resource Management**: Proper cleanup and resource management
**Monitoring**: Comprehensive metrics for cache hit rates and performance

### Production Considerations

**Error Handling**: Implement graceful degradation and fallback strategies
**Testing**: Comprehensive unit and integration testing
**Monitoring**: Cache performance metrics and alerting
**Capacity Planning**: Cache sizing and eviction policies
**Security**: Proper authentication and authorization for cache access

## Summary

Ignite 3's Table and SQL APIs provide the foundation for implementing standard caching patterns with distributed system benefits. Cache-aside patterns optimize read-heavy workloads, write-through patterns ensure data consistency, and write-behind patterns handle high-throughput scenarios.

The key advantage of using Ignite 3 for caching patterns lies in its native support for transactions, async operations, and distributed consistency. Applications can implement these patterns using familiar Java interfaces while gaining the benefits of distributed caching, automatic failover, and horizontal scalability.

Music streaming services demonstrate how different caching patterns work together: catalog data uses cache-aside for read optimization, customer data uses write-through for consistency, and analytics data uses write-behind for performance. This multi-pattern approach provides the right balance of performance, consistency, and scalability for complex applications.