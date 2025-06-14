package com.apache.ignite.examples.caching;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.client.IgniteClientBuilder;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;
import org.apache.ignite.tx.IgniteTransactions;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive demonstration of caching patterns using Ignite 3 Java APIs.
 * 
 * This orchestrator application demonstrates how music streaming services
 * implement different caching patterns based on data characteristics:
 * 
 * - Cache-aside for catalog data (read-heavy workloads)
 * - Write-through for customer data (consistency-critical)
 * - Write-behind for analytics data (high-throughput writes)
 * 
 * The demonstration shows realistic music streaming scenarios with
 * proper error handling, performance optimization, and production patterns.
 * 
 * Key concepts demonstrated:
 * - Multi-pattern caching architecture
 * - Performance optimization techniques
 * - Error handling and graceful degradation
 * - Monitoring and operational visibility
 * 
 * @see CacheAsidePatternDemo for read-heavy catalog operations
 * @see WriteThroughPatternDemo for consistency-critical customer data
 * @see WriteBehindPatternDemo for high-throughput analytics
 */
public class CachingPatternsDemo {
    
    private static final String CLUSTER_URL = "http://localhost:10800";
    private static final String ARTIST_TABLE = "Artist";
    private static final String CUSTOMER_TABLE = "Customer";
    private static final String PLAY_EVENT_TABLE = "PlayEvent";
    
    private IgniteClient client;
    private CacheAsidePatternDemo cacheAsideDemo;
    private WriteThroughPatternDemo writeThroughDemo;
    private WriteBehindPatternDemo writeBehindDemo;
    private ExternalDataSource externalDataSource;
    
    public static void main(String[] args) {
        CachingPatternsDemo demo = new CachingPatternsDemo();
        
        try {
            demo.initialize();
            demo.runAllDemonstrations();
            
        } catch (Exception e) {
            System.err.println("Demo execution failed: " + e.getMessage());
            e.printStackTrace();
            
        } finally {
            demo.cleanup();
        }
    }
    
    /**
     * Initializes the caching patterns demonstration.
     * 
     * Sets up Ignite client connections and initializes all
     * pattern demonstration components with proper configuration.
     */
    private void initialize() {
        System.out.println("=== Initializing Caching Patterns Demo ===");
        
        // Initialize Ignite client
        client = IgniteClientBuilder.builder()
            .addresses(CLUSTER_URL)
            .build();
        
        System.out.println("Connected to Ignite cluster at " + CLUSTER_URL);
        
        // Initialize external data source simulation
        externalDataSource = new ExternalDataSource();
        
        // Initialize pattern demonstrations
        initializeCacheAsideDemo();
        initializeWriteThroughDemo();
        initializeWriteBehindDemo();
        
        System.out.println("All caching pattern demos initialized successfully");
        System.out.println();
    }
    
    /**
     * Initializes cache-aside pattern demonstration.
     * 
     * Sets up KeyValueView for artist caching and demonstrates
     * read-heavy catalog operations.
     */
    private void initializeCacheAsideDemo() {
        try {
            Table artistTable = client.tables().table(ARTIST_TABLE);
            KeyValueView<Integer, Artist> artistCache = artistTable.keyValueView();
            
            cacheAsideDemo = new CacheAsidePatternDemo(artistCache, externalDataSource);
            System.out.println("Cache-aside demo initialized for artist catalog");
            
        } catch (Exception e) {
            System.err.println("Failed to initialize cache-aside demo: " + e.getMessage());
            // In production, consider fallback strategies
            throw new RuntimeException("Cache-aside initialization failed", e);
        }
    }
    
    /**
     * Initializes write-through pattern demonstration.
     * 
     * Sets up RecordView and transaction management for customer data
     * operations requiring consistency guarantees.
     */
    private void initializeWriteThroughDemo() {
        try {
            Table customerTable = client.tables().table(CUSTOMER_TABLE);
            RecordView<Customer> customerCache = customerTable.recordView();
            IgniteTransactions transactions = client.transactions();
            
            writeThroughDemo = new WriteThroughPatternDemo(customerCache, transactions, externalDataSource);
            System.out.println("Write-through demo initialized for customer data");
            
        } catch (Exception e) {
            System.err.println("Failed to initialize write-through demo: " + e.getMessage());
            throw new RuntimeException("Write-through initialization failed", e);
        }
    }
    
    /**
     * Initializes write-behind pattern demonstration.
     * 
     * Sets up KeyValueView for high-throughput event recording
     * with background processing capabilities.
     */
    private void initializeWriteBehindDemo() {
        try {
            Table playEventTable = client.tables().table(PLAY_EVENT_TABLE);
            KeyValueView<String, PlayEvent> playEventCache = playEventTable.keyValueView();
            
            writeBehindDemo = new WriteBehindPatternDemo(playEventCache, externalDataSource);
            System.out.println("Write-behind demo initialized for analytics events");
            
        } catch (Exception e) {
            System.err.println("Failed to initialize write-behind demo: " + e.getMessage());
            throw new RuntimeException("Write-behind initialization failed", e);
        }
    }
    
    /**
     * Runs all caching pattern demonstrations sequentially.
     * 
     * Demonstrates realistic music streaming scenarios using
     * appropriate caching patterns for different data types.
     */
    private void runAllDemonstrations() {
        System.out.println("=== Running Caching Patterns Demonstrations ===");
        
        // Demonstrate cache-aside pattern
        demonstrateCacheAsidePattern();
        
        // Demonstrate write-through pattern
        demonstrateWriteThroughPattern();
        
        // Demonstrate write-behind pattern
        demonstrateWriteBehindPattern();
        
        // Demonstrate combined patterns
        demonstrateCombinedPatterns();
        
        // Demonstrate performance optimization
        demonstratePerformanceOptimization();
        
        // Demonstrate error handling
        demonstrateErrorHandling();
        
        System.out.println("=== All demonstrations completed successfully ===");
    }
    
    /**
     * Demonstrates cache-aside pattern for music catalog operations.
     * 
     * Shows how music streaming services handle artist and track
     * catalog browsing with cache-aside pattern.
     */
    private void demonstrateCacheAsidePattern() {
        System.out.println("\n--- Cache-Aside Pattern Demonstration ---");
        
        try {
            // Simulate popular artist lookups
            System.out.println("Simulating popular artist lookups:");
            
            // Single artist retrieval (cache miss then hit)
            Artist artist1 = cacheAsideDemo.getArtist(1);
            System.out.printf("Retrieved artist: %s%n", artist1 != null ? artist1.getName() : "Not found");
            
            // Second retrieval should hit cache
            Artist artist1Again = cacheAsideDemo.getArtist(1);
            System.out.printf("Retrieved artist again: %s%n", artist1Again != null ? artist1Again.getName() : "Not found");
            
            // Batch artist retrieval
            List<Integer> popularArtistIds = Arrays.asList(1, 2, 3, 4, 5);
            Map<Integer, Artist> popularArtists = cacheAsideDemo.getPopularArtists(popularArtistIds);
            System.out.printf("Retrieved %d popular artists%n", popularArtists.size());
            
            // Async operations
            System.out.println("Demonstrating async cache-aside operations:");
            CompletableFuture<Artist> asyncArtist = cacheAsideDemo.getArtistAsync(10);
            Artist asyncResult = asyncArtist.get(5, TimeUnit.SECONDS);
            System.out.printf("Async retrieved artist: %s%n", asyncResult != null ? asyncResult.getName() : "Not found");
            
            // Cache warming simulation
            System.out.println("Simulating cache warming for application startup:");
            List<Integer> warmupArtists = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            cacheAsideDemo.warmCache(warmupArtists);
            
            // Display cache statistics
            cacheAsideDemo.getCacheStatistics();
            
        } catch (Exception e) {
            System.err.println("Cache-aside demonstration failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates write-through pattern for customer data operations.
     * 
     * Shows how customer profile updates maintain consistency
     * across cache and external data stores.
     */
    private void demonstrateWriteThroughPattern() {
        System.out.println("\n--- Write-Through Pattern Demonstration ---");
        
        try {
            // Create new customer
            Customer newCustomer = Customer.builder()
                .customerId(1001)
                .name("John Streaming")
                .email("john@example.com")
                .subscriptionTier(SubscriptionTier.PREMIUM)
                .build();
            
            System.out.println("Creating new customer with write-through pattern:");
            boolean created = writeThroughDemo.createCustomer(newCustomer);
            System.out.printf("Customer creation result: %s%n", created ? "Success" : "Already exists");
            
            // Update customer information
            Customer updatedCustomer = newCustomer.toBuilder()
                .name("John Premium Streaming")
                .subscriptionTier(SubscriptionTier.FAMILY)
                .build();
            
            System.out.println("Updating customer with write-through pattern:");
            writeThroughDemo.updateCustomer(updatedCustomer);
            
            // Demonstrate subscription upgrade
            System.out.println("Demonstrating subscription upgrade:");
            boolean upgraded = writeThroughDemo.upgradeSubscription(1001, SubscriptionTier.PREMIUM_PLUS);
            System.out.printf("Subscription upgrade result: %s%n", upgraded ? "Success" : "Failed");
            
            // Batch customer operations
            List<Customer> batchCustomers = Arrays.asList(
                Customer.builder().customerId(1002).name("Alice Music").subscriptionTier(SubscriptionTier.BASIC).build(),
                Customer.builder().customerId(1003).name("Bob Audio").subscriptionTier(SubscriptionTier.PREMIUM).build(),
                Customer.builder().customerId(1004).name("Carol Sound").subscriptionTier(SubscriptionTier.FAMILY).build()
            );
            
            System.out.println("Batch updating customers with write-through pattern:");
            writeThroughDemo.updateSubscriptions(batchCustomers);
            
            // Async operations
            System.out.println("Demonstrating async write-through operations:");
            CompletableFuture<Void> asyncUpdate = writeThroughDemo.updateCustomerAsync(updatedCustomer);
            asyncUpdate.get(10, TimeUnit.SECONDS);
            System.out.println("Async customer update completed");
            
        } catch (Exception e) {
            System.err.println("Write-through demonstration failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates write-behind pattern for analytics data operations.
     * 
     * Shows how high-throughput play events are recorded immediately
     * in cache with background processing to external analytics systems.
     */
    private void demonstrateWriteBehindPattern() {
        System.out.println("\n--- Write-Behind Pattern Demonstration ---");
        
        try {
            // Single play event recording
            PlayEvent playEvent = PlayEvent.builder()
                .customerId(1001)
                .trackId(5001)
                .timestamp(LocalDateTime.now())
                .build();
            
            System.out.println("Recording single play event with write-behind pattern:");
            CompletableFuture<Void> recordResult = writeBehindDemo.recordPlayEvent(playEvent);
            recordResult.get(2, TimeUnit.SECONDS);
            
            // Batch play event recording
            List<PlayEvent> batchEvents = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                batchEvents.add(PlayEvent.builder()
                    .customerId(1001 + (i % 10))
                    .trackId(5001 + i)
                    .timestamp(LocalDateTime.now().minusSeconds(i))
                    .build());
            }
            
            System.out.println("Recording batch play events with write-behind pattern:");
            CompletableFuture<Void> batchResult = writeBehindDemo.recordPlayEvents(batchEvents);
            batchResult.get(5, TimeUnit.SECONDS);
            
            // User activity tracking
            System.out.println("Tracking user activities with write-behind pattern:");
            Map<String, Object> activityMetadata = new HashMap<>();
            activityMetadata.put("playlist_id", 2001);
            activityMetadata.put("device_type", "mobile");
            
            CompletableFuture<Void> activityResult = writeBehindDemo.trackUserActivity(
                1001, "playlist_create", activityMetadata);
            activityResult.get(2, TimeUnit.SECONDS);
            
            // High-throughput streaming simulation
            System.out.println("Simulating high-throughput event stream:");
            List<PlayEvent> eventStream = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                eventStream.add(PlayEvent.builder()
                    .customerId(1001 + (i % 20))
                    .trackId(5001 + (i % 50))
                    .timestamp(LocalDateTime.now().minusSeconds(i))
                    .build());
            }
            
            CompletableFuture<Void> streamResult = writeBehindDemo.ingestEventStream(eventStream);
            streamResult.get(10, TimeUnit.SECONDS);
            
            // Display buffer statistics
            writeBehindDemo.getBufferStatistics();
            
            // Allow some time for background processing
            System.out.println("Waiting for background processing...");
            Thread.sleep(3000);
            
        } catch (Exception e) {
            System.err.println("Write-behind demonstration failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates combined caching patterns in realistic scenarios.
     * 
     * Shows how music streaming services use different patterns
     * together for comprehensive data management.
     */
    private void demonstrateCombinedPatterns() {
        System.out.println("\n--- Combined Caching Patterns Demonstration ---");
        
        try {
            System.out.println("Simulating music streaming request processing:");
            
            // Simulate a user streaming a track (combines all patterns)
            int customerId = 1001;
            int trackId = 5001;
            
            // 1. Get customer data (write-through pattern)
            Customer customer = writeThroughDemo.getCustomer(customerId);
            if (customer != null) {
                System.out.printf("Customer %s has %s subscription%n", 
                    customer.getName(), customer.getSubscriptionTier());
            }
            
            // 2. Get track information (cache-aside pattern)
            Track track = cacheAsideDemo.getTrack(trackId);
            if (track != null) {
                System.out.printf("Track: %s%n", track.getName());
            }
            
            // 3. Record play event (write-behind pattern)
            if (customer != null && track != null) {
                PlayEvent streamingEvent = PlayEvent.builder()
                    .customerId(customerId)
                    .trackId(trackId)
                    .timestamp(LocalDateTime.now())
                    .build();
                
                writeBehindDemo.recordPlayEvent(streamingEvent);
                System.out.println("Streaming event recorded successfully");
            }
            
            // Simulate playlist loading (multiple patterns)
            System.out.println("Simulating playlist loading with multiple patterns:");
            
            List<Integer> playlistTrackIds = Arrays.asList(5001, 5002, 5003, 5004, 5005);
            Map<Integer, Track> playlistTracks = cacheAsideDemo.getTracksForPlaylist(playlistTrackIds);
            System.out.printf("Loaded %d tracks for playlist%n", playlistTracks.size());
            
            // Record playlist access
            Map<String, Object> playlistMetadata = new HashMap<>();
            playlistMetadata.put("track_count", playlistTracks.size());
            playlistMetadata.put("access_type", "full_load");
            
            writeBehindDemo.trackUserActivity(customerId, "playlist_access", playlistMetadata);
            
        } catch (Exception e) {
            System.err.println("Combined patterns demonstration failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates performance optimization techniques across patterns.
     * 
     * Shows how to optimize caching patterns for maximum throughput
     * and minimum latency in production environments.
     */
    private void demonstratePerformanceOptimization() {
        System.out.println("\n--- Performance Optimization Demonstration ---");
        
        try {
            System.out.println("Demonstrating async pattern orchestration:");
            
            // Parallel operations using different patterns
            List<CompletableFuture<Void>> parallelOperations = new ArrayList<>();
            
            // Parallel artist lookups (cache-aside)
            for (int i = 1; i <= 10; i++) {
                final int artistId = i;
                CompletableFuture<Void> artistLookup = cacheAsideDemo.getArtistAsync(artistId)
                    .thenAccept(artist -> {
                        if (artist != null) {
                            System.out.printf("Async loaded artist %d: %s%n", artistId, artist.getName());
                        }
                    });
                parallelOperations.add(artistLookup);
            }
            
            // Parallel customer updates (write-through)
            for (int i = 1001; i <= 1005; i++) {
                final int customerId = i;
                Customer customer = Customer.builder()
                    .customerId(customerId)
                    .name("Customer " + customerId)
                    .subscriptionTier(SubscriptionTier.PREMIUM)
                    .build();
                
                CompletableFuture<Void> customerUpdate = writeThroughDemo.updateCustomerAsync(customer);
                parallelOperations.add(customerUpdate);
            }
            
            // Parallel event recording (write-behind)
            for (int i = 0; i < 20; i++) {
                PlayEvent event = PlayEvent.builder()
                    .customerId(1001 + (i % 5))
                    .trackId(5001 + i)
                    .timestamp(LocalDateTime.now())
                    .build();
                
                CompletableFuture<Void> eventRecord = writeBehindDemo.recordPlayEvent(event);
                parallelOperations.add(eventRecord);
            }
            
            // Wait for all operations to complete
            CompletableFuture<Void> allOperations = CompletableFuture.allOf(
                parallelOperations.toArray(new CompletableFuture[0])
            );
            
            long startTime = System.currentTimeMillis();
            allOperations.get(15, TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();
            
            System.out.printf("Completed %d parallel operations in %d ms%n", 
                parallelOperations.size(), (endTime - startTime));
            
        } catch (Exception e) {
            System.err.println("Performance optimization demonstration failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates error handling and graceful degradation.
     * 
     * Shows how applications handle caching failures while
     * maintaining service availability.
     */
    private void demonstrateErrorHandling() {
        System.out.println("\n--- Error Handling Demonstration ---");
        
        try {
            System.out.println("Simulating cache failure scenarios:");
            
            // Test cache-aside fallback
            System.out.println("Testing cache-aside error handling:");
            try {
                // This would normally trigger error handling in a real failure scenario
                Artist artist = cacheAsideDemo.getArtist(999);
                System.out.printf("Artist retrieval result: %s%n", 
                    artist != null ? artist.getName() : "Not found");
            } catch (Exception e) {
                System.out.println("Cache-aside error handled gracefully: " + e.getMessage());
            }
            
            // Test write-through transaction rollback
            System.out.println("Testing write-through error handling:");
            try {
                Customer invalidCustomer = Customer.builder()
                    .customerId(-1) // Invalid ID to trigger error
                    .name("Invalid Customer")
                    .build();
                
                writeThroughDemo.updateCustomer(invalidCustomer);
            } catch (Exception e) {
                System.out.println("Write-through error handled gracefully: " + e.getMessage());
            }
            
            // Test write-behind buffer management
            System.out.println("Testing write-behind buffer management:");
            writeBehindDemo.getBufferStatistics();
            
            System.out.println("Error handling demonstrations completed");
            
        } catch (Exception e) {
            System.err.println("Error handling demonstration failed: " + e.getMessage());
        }
    }
    
    /**
     * Cleanup resources and graceful shutdown.
     * 
     * Ensures all background processes complete and resources
     * are properly released.
     */
    private void cleanup() {
        System.out.println("\n=== Cleaning up resources ===");
        
        try {
            // Force flush any pending write-behind operations
            if (writeBehindDemo != null) {
                writeBehindDemo.forceFlush();
                writeBehindDemo.shutdown();
            }
            
            // Close Ignite client
            if (client != null) {
                client.close();
                System.out.println("Ignite client closed");
            }
            
            // Cleanup external data source
            if (externalDataSource != null) {
                externalDataSource.cleanup();
            }
            
            System.out.println("Cleanup completed successfully");
            
        } catch (Exception e) {
            System.err.println("Cleanup failed: " + e.getMessage());
        }
    }
}