package com.apache.ignite.examples.caching;

import org.apache.ignite.table.KeyValueView;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Demonstrates write-behind pattern implementation using Ignite 3 async operations.
 * 
 * Music streaming services use write-behind for analytics data where:
 * - High write throughput is required
 * - Immediate consistency is not critical
 * - Performance is prioritized over immediate durability
 * 
 * Write-behind patterns optimize for high-throughput scenarios by updating cache
 * immediately while deferring external data store writes. Music streaming analytics
 * require this pattern to handle millions of play events without blocking user experience.
 * 
 * Key operations demonstrated:
 * - Immediate cache updates with deferred external writes
 * - Background batch processing for external store synchronization
 * - High-throughput event recording patterns
 * - Graceful shutdown with pending write flush
 * 
 * @see KeyValueView for cache operations
 * @see CompletableFuture for async operations
 * @see ScheduledExecutorService for background processing
 */
public class WriteBehindPatternDemo {
    
    private final KeyValueView<String, PlayEvent> playEventCache;
    private final ScheduledExecutorService scheduler;
    private final ExternalDataSource externalDataSource;
    private final Queue<PlayEvent> writeBuffer;
    private final Queue<UserActivity> activityBuffer;
    private final int batchSize;
    private final long flushIntervalMs;
    
    /**
     * Constructs write-behind demo with configurable parameters.
     * 
     * @param playEventCache Ignite KeyValueView for event caching
     * @param externalDataSource Primary data store for deferred writes
     */
    public WriteBehindPatternDemo(KeyValueView<String, PlayEvent> playEventCache,
                                 ExternalDataSource externalDataSource) {
        this(playEventCache, externalDataSource, 1000, 5000);
    }
    
    /**
     * Constructs write-behind demo with custom batch size and flush interval.
     * 
     * @param playEventCache Ignite KeyValueView for event caching
     * @param externalDataSource Primary data store for deferred writes
     * @param batchSize Number of events to batch before flushing
     * @param flushIntervalMs Maximum time between flushes in milliseconds
     */
    public WriteBehindPatternDemo(KeyValueView<String, PlayEvent> playEventCache,
                                 ExternalDataSource externalDataSource,
                                 int batchSize,
                                 long flushIntervalMs) {
        this.playEventCache = playEventCache;
        this.externalDataSource = externalDataSource;
        this.batchSize = batchSize;
        this.flushIntervalMs = flushIntervalMs;
        this.writeBuffer = new ConcurrentLinkedQueue<>();
        this.activityBuffer = new ConcurrentLinkedQueue<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        // Start background flush processes
        startBackgroundFlush();
        
        System.out.printf("Write-behind demo initialized with batch size %d and flush interval %d ms%n", 
            batchSize, flushIntervalMs);
    }
    
    /**
     * Records play event using write-behind pattern.
     * 
     * Pattern implementation:
     * 1. Update cache immediately (fast response)
     * 2. Queue for background write to external store
     * 3. Return immediately without waiting for external write
     * 
     * This approach ensures fast user experience while ensuring eventual
     * consistency with the external analytics system.
     * 
     * @param playEvent Play event to record
     * @return CompletableFuture that completes when cache update is done
     */
    public CompletableFuture<Void> recordPlayEvent(PlayEvent playEvent) {
        String eventId = generateEventId(playEvent);
        System.out.printf("Recording play event %s using write-behind pattern%n", eventId);
        
        // Step 1: Update cache immediately for fast response
        return playEventCache.putAsync(null, eventId, playEvent)
            .thenAccept(ignored -> {
                System.out.printf("Cached play event %s immediately%n", eventId);
                
                // Step 2: Queue for background processing
                writeBuffer.offer(playEvent);
                System.out.printf("Queued play event %s for background write (queue size: %d)%n", 
                    eventId, writeBuffer.size());
            });
    }
    
    /**
     * Batch play event recording for high-throughput scenarios.
     * 
     * Optimizes performance by batching cache operations
     * while maintaining write-behind semantics.
     * 
     * This approach is particularly effective for:
     * - Bulk event ingestion from streaming services
     * - Batch processing of historical events
     * - High-concurrency real-time analytics
     * 
     * @param playEvents List of play events to record
     * @return CompletableFuture that completes when cache updates are done
     */
    public CompletableFuture<Void> recordPlayEvents(List<PlayEvent> playEvents) {
        System.out.printf("Batch recording %d play events using write-behind pattern%n", playEvents.size());
        
        // Prepare cache updates
        Map<String, PlayEvent> cacheUpdates = playEvents.stream()
            .collect(Collectors.toMap(
                this::generateEventId,
                Function.identity()
            ));
        
        // Update cache in batch for performance
        return playEventCache.putAllAsync(null, cacheUpdates)
            .thenAccept(ignored -> {
                System.out.printf("Cached %d play events immediately%n", playEvents.size());
                
                // Queue all events for background processing
                writeBuffer.addAll(playEvents);
                System.out.printf("Queued %d play events for background write (queue size: %d)%n", 
                    playEvents.size(), writeBuffer.size());
                
                // Check if immediate flush is needed due to buffer size
                if (writeBuffer.size() >= batchSize) {
                    System.out.printf("Buffer size (%d) reached batch threshold, triggering immediate flush%n", 
                        writeBuffer.size());
                    scheduler.submit(this::flushPendingWrites);
                }
            });
    }
    
    /**
     * User activity tracking with write-behind pattern.
     * 
     * Records user interactions immediately in cache
     * while deferring analytics processing.
     * 
     * This pattern handles diverse user activity types:
     * - Track plays, skips, and pauses
     * - Playlist creation and modification
     * - Search queries and results
     * - Social interactions (likes, shares)
     * 
     * @param customerId Customer performing the activity
     * @param activityType Type of activity performed
     * @param metadata Additional activity metadata
     * @return CompletableFuture that completes when cache update is done
     */
    public CompletableFuture<Void> trackUserActivity(int customerId, String activityType, Map<String, Object> metadata) {
        UserActivity activity = UserActivity.builder()
            .customerId(customerId)
            .activityType(activityType)
            .timestamp(LocalDateTime.now())
            .metadata(metadata)
            .build();
        
        String activityId = generateActivityId(activity);
        System.out.printf("Tracking user activity %s for customer %d%n", activityId, customerId);
        
        // Convert to PlayEvent for storage (in real implementation, you'd have separate caches)
        PlayEvent eventForStorage = convertToPlayEvent(activity);
        
        // Update cache immediately
        return playEventCache.putAsync(null, activityId, eventForStorage)
            .thenAccept(ignored -> {
                System.out.printf("Cached user activity %s immediately%n", activityId);
                
                // Queue for background analytics processing
                activityBuffer.offer(activity);
                System.out.printf("Queued activity %s for background analytics%n", activityId);
                
                // Schedule analytics processing (separate from main data flush)
                scheduleAnalyticsProcessing(activity);
            });
    }
    
    /**
     * High-throughput streaming event ingestion.
     * 
     * Handles continuous streams of events with optimal batching
     * and buffer management for maximum throughput.
     * 
     * @param eventStream Stream of events to process
     * @return CompletableFuture that completes when all events are cached
     */
    public CompletableFuture<Void> ingestEventStream(List<PlayEvent> eventStream) {
        System.out.printf("Ingesting event stream with %d events%n", eventStream.size());
        
        // Process events in optimal batch sizes
        List<CompletableFuture<Void>> batchFutures = new ArrayList<>();
        
        for (int i = 0; i < eventStream.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, eventStream.size());
            List<PlayEvent> batch = eventStream.subList(i, endIndex);
            
            System.out.printf("Processing event batch %d-%d%n", i + 1, endIndex);
            batchFutures.add(recordPlayEvents(batch));
        }
        
        // Wait for all batches to complete
        return CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]))
            .thenAccept(ignored -> {
                System.out.printf("Completed ingestion of %d events%n", eventStream.size());
            });
    }
    
    /**
     * Background flush process for write-behind operations.
     * 
     * Periodically flushes buffered writes to external data store
     * with configurable batch size and flush intervals.
     */
    private void startBackgroundFlush() {
        // Time-based flush for guaranteed maximum latency
        scheduler.scheduleAtFixedRate(
            this::flushPendingWrites,
            flushIntervalMs,
            flushIntervalMs,
            TimeUnit.MILLISECONDS
        );
        
        // Size-based flush monitoring for throughput optimization
        scheduler.scheduleAtFixedRate(
            this::checkBufferSize,
            1000, // Check every second
            1000,
            TimeUnit.MILLISECONDS
        );
        
        System.out.println("Started background flush processes");
    }
    
    /**
     * Flushes pending writes to external data store.
     * 
     * Processes buffered events in batches for optimal
     * external data store performance while handling failures gracefully.
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
        
        System.out.printf("Flushing %d events to external data store%n", eventsToFlush.size());
        
        try {
            // Write to external data store in batch
            boolean success = externalDataSource.batchInsertPlayEvents(eventsToFlush);
            if (success) {
                System.out.printf("Successfully flushed %d events to external store%n", eventsToFlush.size());
            } else {
                throw new RuntimeException("External data store rejected batch");
            }
            
        } catch (Exception e) {
            System.err.printf("Failed to flush %d events to external store: %s%n", 
                eventsToFlush.size(), e.getMessage());
            
            // Re-queue events for retry (in production, consider dead letter queue)
            writeBuffer.addAll(eventsToFlush);
            System.out.printf("Re-queued %d events for retry%n", eventsToFlush.size());
            
            // In production, you might want to:
            // 1. Use exponential backoff for retries
            // 2. Move failed events to dead letter queue after max retries
            // 3. Alert monitoring systems about persistent failures
        }
    }
    
    /**
     * Checks buffer size and triggers immediate flush if needed.
     * 
     * Prevents memory overflow by flushing when buffer
     * reaches configured batch size threshold.
     */
    private void checkBufferSize() {
        int currentSize = writeBuffer.size();
        if (currentSize >= batchSize) {
            System.out.printf("Buffer size (%d) reached threshold, triggering flush%n", currentSize);
            flushPendingWrites();
        }
    }
    
    /**
     * Manual flush trigger for testing or immediate consistency requirements.
     * 
     * Allows applications to force immediate write-behind
     * flush when necessary, such as before application shutdown
     * or when immediate consistency is temporarily required.
     */
    public void forceFlush() {
        System.out.println("Force flushing all pending writes");
        
        // Flush main event buffer
        while (!writeBuffer.isEmpty()) {
            flushPendingWrites();
        }
        
        // Flush activity buffer
        flushPendingActivities();
        
        System.out.println("Force flush completed");
    }
    
    /**
     * Graceful shutdown with pending write flush.
     * 
     * Ensures all buffered writes are processed before
     * application shutdown to prevent data loss.
     */
    public void shutdown() {
        System.out.println("Shutting down write-behind demo with graceful flush");
        
        try {
            // Stop accepting new background tasks
            scheduler.shutdown();
            
            // Flush all pending writes
            System.out.println("Flushing pending writes before shutdown");
            forceFlush();
            
            // Wait for background tasks to complete
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                System.err.println("Background tasks did not complete within timeout, forcing shutdown");
                scheduler.shutdownNow();
            }
            
            System.out.println("Write-behind demo shutdown completed");
            
        } catch (InterruptedException e) {
            System.err.println("Shutdown interrupted, forcing immediate shutdown");
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Retrieves buffered write statistics for monitoring.
     * 
     * Provides visibility into write-behind performance and
     * buffer utilization for operational monitoring.
     * 
     * @return Map containing buffer statistics
     */
    public Map<String, Object> getBufferStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pending_writes", writeBuffer.size());
        stats.put("pending_activities", activityBuffer.size());
        stats.put("batch_size", batchSize);
        stats.put("flush_interval_ms", flushIntervalMs);
        stats.put("scheduler_active", !scheduler.isShutdown());
        
        System.out.println("Write-behind Buffer Statistics:");
        stats.forEach((key, value) -> System.out.printf("  %s: %s%n", key, value));
        
        return stats;
    }
    
    /**
     * Schedules background analytics processing for user activities.
     * 
     * Processes user activity data for insights and recommendations
     * without blocking the main event recording flow.
     * 
     * @param activity User activity to process
     */
    private void scheduleAnalyticsProcessing(UserActivity activity) {
        scheduler.schedule(() -> {
            try {
                System.out.printf("Processing analytics for activity: %s%n", activity.getActivityType());
                boolean processed = externalDataSource.processUserActivityAnalytics(activity);
                
                if (processed) {
                    System.out.printf("Successfully processed analytics for customer %d%n", activity.getCustomerId());
                } else {
                    System.err.printf("Failed to process analytics for customer %d%n", activity.getCustomerId());
                    // In production, consider re-queuing or alerting
                }
                
            } catch (Exception e) {
                System.err.printf("Analytics processing failed for customer %d: %s%n", 
                    activity.getCustomerId(), e.getMessage());
                // Log error but don't fail the user request
            }
        }, 1, TimeUnit.SECONDS);
    }
    
    /**
     * Flushes pending user activity data to analytics systems.
     */
    private void flushPendingActivities() {
        List<UserActivity> activitiesToFlush = new ArrayList<>();
        
        UserActivity activity;
        while (activitiesToFlush.size() < batchSize && (activity = activityBuffer.poll()) != null) {
            activitiesToFlush.add(activity);
        }
        
        if (activitiesToFlush.isEmpty()) {
            return;
        }
        
        System.out.printf("Flushing %d activities to analytics system%n", activitiesToFlush.size());
        
        try {
            boolean success = externalDataSource.batchProcessActivities(activitiesToFlush);
            if (success) {
                System.out.printf("Successfully processed %d activities%n", activitiesToFlush.size());
            } else {
                // Re-queue for retry
                activityBuffer.addAll(activitiesToFlush);
            }
            
        } catch (Exception e) {
            System.err.printf("Failed to process activities: %s%n", e.getMessage());
            activityBuffer.addAll(activitiesToFlush);
        }
    }
    
    private String generateEventId(PlayEvent playEvent) {
        return String.format("%d_%d_%d", 
            playEvent.getCustomerId(),
            playEvent.getTrackId(),
            playEvent.getTimestamp().toEpochSecond(ZoneOffset.UTC)
        );
    }
    
    private String generateActivityId(UserActivity activity) {
        return String.format("activity_%d_%d", 
            activity.getCustomerId(),
            activity.getTimestamp().toEpochSecond(ZoneOffset.UTC)
        );
    }
    
    private PlayEvent convertToPlayEvent(UserActivity activity) {
        // Convert UserActivity to PlayEvent for storage
        // In a real implementation, you'd have separate caches for different entity types
        return PlayEvent.builder()
            .customerId(activity.getCustomerId())
            .trackId(0) // Not applicable for non-play activities
            .timestamp(activity.getTimestamp())
            .activityType(activity.getActivityType())
            .metadata(activity.getMetadata())
            .build();
    }
}