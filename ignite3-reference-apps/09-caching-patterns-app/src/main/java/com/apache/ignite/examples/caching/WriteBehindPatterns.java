/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apache.ignite.examples.caching;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Write-Behind Patterns - High-throughput analytics data operations with Apache Ignite 3.
 * 
 * This class demonstrates write-behind pattern for analytics event recording:
 * - Immediate cache writes with background external system updates
 * - High-throughput event ingestion with buffering
 * - Background batch processing to external analytics systems
 * - Flow control and backpressure management
 * - Buffer monitoring and performance optimization
 * 
 * Learning Focus:
 * - When to use write-behind (high-throughput writes, can tolerate eventual consistency)
 * - Background processing patterns with scheduled executors
 * - Buffer management and overflow protection
 * - Performance optimization for high-volume scenarios
 */
public class WriteBehindPatterns {

    private static final Logger logger = LoggerFactory.getLogger(WriteBehindPatterns.class);
    
    private final ScheduledExecutorService backgroundProcessor = Executors.newScheduledThreadPool(2);
    private final List<Tuple> writeBuffer = new ArrayList<>();
    private final AtomicInteger eventCounter = new AtomicInteger(0);
    private final AtomicInteger bufferedEvents = new AtomicInteger(0);
    private volatile boolean isShutdown = false;
    
    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Write-Behind Patterns Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        
        WriteBehindPatterns demo = new WriteBehindPatterns();
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            demo.runWriteBehindPatterns(client);
            
        } catch (Exception e) {
            logger.error("Failed to run write-behind patterns", e);
            System.err.println("Error: " + e.getMessage());
        } finally {
            demo.shutdown();
        }
    }

    private void runWriteBehindPatterns(IgniteClient client) {
        // Use existing Track table for high-throughput operations demonstration
        Table eventTable = client.tables().table("Track");
        if (eventTable == null) {
            throw new RuntimeException("Track table not found. Please run sample data setup first.");
        }
        
        KeyValueView<Tuple, Tuple> eventCache = eventTable.keyValueView();
        
        System.out.println("\n--- Write-Behind Pattern Demonstrations ---");
        
        // Start background processing
        startBackgroundProcessing();
        
        // Demonstrate core write-behind patterns
        demonstrateSingleEventRecording(eventCache);
        demonstrateHighThroughputIngestion(eventCache);
        demonstrateBatchEventProcessing(eventCache);
        demonstrateBufferManagement(eventCache);
        
        // Allow background processing to complete
        System.out.println("\n   Waiting for background processing to complete...");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Display final statistics
        displayFinalStatistics();
        
        System.out.println("\n>>> Write-behind patterns completed successfully");
    }

    /**
     * Demonstrates single event recording with write-behind pattern.
     * 
     * Shows basic write-behind flow:
     * 1. Immediate write to cache (low latency)
     * 2. Add to background processing buffer
     * 3. Continue without waiting for external system
     */
    private void demonstrateSingleEventRecording(KeyValueView<Tuple, Tuple> eventCache) {
        System.out.println("\n1. Single Event Recording Pattern:");
        
        // Record play event immediately to cache
        String eventId = generateEventId();
        Tuple playEvent = createPlayEvent(eventId, 1001, 5001);
        
        System.out.println("   Recording play event with write-behind pattern");
        long startTime = System.currentTimeMillis();
        
        // Phase 1: Immediate cache write (low latency) - using Track table structure
        Tuple key = Tuple.create().set("TrackId", Integer.parseInt(eventId.split("_")[2]) % 1000).set("AlbumId", 1);
        eventCache.put(null, key, playEvent);
        
        long cacheTime = System.currentTimeMillis() - startTime;
        System.out.printf("   >>> Event written to cache in %d ms%n", cacheTime);
        
        // Phase 2: Add to background processing buffer
        addToWriteBuffer(playEvent);
        System.out.println("   >>> Event queued for background processing");
        
        // Phase 3: Return immediately (client doesn't wait)
        System.out.printf("   >>> Total response time: %d ms (immediate return)%n", cacheTime);
    }

    /**
     * Demonstrates high-throughput event ingestion.
     * 
     * Shows how write-behind handles burst traffic:
     * - Accept high-volume writes with minimal latency
     * - Buffer events for efficient batch processing
     * - Monitor buffer levels and processing rates
     */
    private void demonstrateHighThroughputIngestion(KeyValueView<Tuple, Tuple> eventCache) {
        System.out.println("\n2. High-Throughput Ingestion Pattern:");
        
        int eventCount = 100;
        System.out.printf("   Ingesting %d events at high throughput%n", eventCount);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < eventCount; i++) {
            String eventId = generateEventId();
            Tuple playEvent = createPlayEvent(eventId, 1001 + (i % 10), 5001 + i);
            
            // Immediate cache write using Track table structure
            Tuple key = Tuple.create().set("TrackId", Integer.parseInt(eventId.split("_")[2]) % 1000).set("AlbumId", 1);
            eventCache.put(null, key, playEvent);
            
            // Add to background processing
            addToWriteBuffer(playEvent);
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double eventsPerSecond = (eventCount * 1000.0) / totalTime;
        
        System.out.printf("   >>> Ingested %d events in %d ms%n", eventCount, totalTime);
        System.out.printf("   >>> Throughput: %.1f events/second%n", eventsPerSecond);
        System.out.printf("   >>> Average latency: %.2f ms/event%n", (double) totalTime / eventCount);
    }

    /**
     * Demonstrates batch event processing with flow control.
     * 
     * Shows how to process events in batches:
     * - Collect events into efficient batch sizes
     * - Process batches to external systems
     * - Handle processing failures gracefully
     */
    private void demonstrateBatchEventProcessing(KeyValueView<Tuple, Tuple> eventCache) {
        System.out.println("\n3. Batch Event Processing Pattern:");
        
        // Generate a batch of related events
        List<Tuple> eventBatch = new ArrayList<>();
        String playlistId = "playlist_" + System.currentTimeMillis();
        
        for (int i = 0; i < 25; i++) {
            String eventId = generateEventId();
            Tuple event = createPlayEvent(eventId, 1001, 5001 + i);
            // Note: PlaylistId and SequenceNumber would be stored in external analytics system
            eventBatch.add(event);
        }
        
        System.out.printf("   Processing batch of %d related events%n", eventBatch.size());
        
        // Phase 1: Write all events to cache immediately
        long cacheStartTime = System.currentTimeMillis();
        for (int i = 0; i < eventBatch.size(); i++) {
            Tuple event = eventBatch.get(i);
            Tuple key = Tuple.create().set("TrackId", 6000 + i).set("AlbumId", 1);
            eventCache.put(null, key, event);
        }
        long cacheTime = System.currentTimeMillis() - cacheStartTime;
        
        System.out.printf("   >>> Batch written to cache in %d ms%n", cacheTime);
        
        // Phase 2: Add to background processing buffer
        synchronized (writeBuffer) {
            writeBuffer.addAll(eventBatch);
            bufferedEvents.addAndGet(eventBatch.size());
        }
        
        System.out.printf("   >>> Batch queued for background processing (%d events buffered)%n", 
            bufferedEvents.get());
    }

    /**
     * Demonstrates buffer management and overflow protection.
     * 
     * Shows production-ready buffer management:
     * - Monitor buffer levels
     * - Handle buffer overflow scenarios
     * - Provide backpressure when needed
     */
    private void demonstrateBufferManagement(KeyValueView<Tuple, Tuple> eventCache) {
        System.out.println("\n4. Buffer Management Pattern:");
        
        System.out.println("   Demonstrating buffer management and overflow protection");
        
        // Simulate high load that might cause buffer pressure
        int loadTestEvents = 200;
        int batchSize = 50;
        
        for (int batch = 0; batch < loadTestEvents / batchSize; batch++) {
            System.out.printf("   Processing batch %d/%d%n", batch + 1, loadTestEvents / batchSize);
            
            // Check buffer level before processing
            int currentBufferSize = bufferedEvents.get();
            if (currentBufferSize > 150) {
                System.out.printf("   !!! High buffer level detected: %d events%n", currentBufferSize);
                System.out.println("   Applying backpressure (simulated slow-down)");
                
                try {
                    Thread.sleep(100); // Simulate backpressure
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            // Process batch
            for (int i = 0; i < batchSize; i++) {
                String eventId = generateEventId();
                Tuple event = createPlayEvent(eventId, 1001 + (i % 5), 5001 + i);
                
                Tuple key = Tuple.create().set("TrackId", 7000 + (batch * batchSize + i)).set("AlbumId", 1);
                eventCache.put(null, key, event);
                addToWriteBuffer(event);
            }
        }
        
        System.out.printf("   >>> Load test completed: %d events processed%n", loadTestEvents);
        System.out.printf("   >>> Current buffer level: %d events%n", bufferedEvents.get());
    }

    // Background processing methods

    private void startBackgroundProcessing() {
        System.out.println("   Starting background processing threads");
        
        // Background processor for write-behind operations
        backgroundProcessor.scheduleWithFixedDelay(this::processWriteBuffer, 1, 1, TimeUnit.SECONDS);
        
        // Buffer monitoring task
        backgroundProcessor.scheduleWithFixedDelay(this::monitorBuffer, 5, 5, TimeUnit.SECONDS);
    }

    private void processWriteBuffer() {
        if (isShutdown) return;
        
        List<Tuple> eventsToProcess = new ArrayList<>();
        
        synchronized (writeBuffer) {
            if (!writeBuffer.isEmpty()) {
                // Process up to 20 events per batch for efficiency
                int batchSize = Math.min(20, writeBuffer.size());
                eventsToProcess.addAll(writeBuffer.subList(0, batchSize));
                writeBuffer.subList(0, batchSize).clear();
                bufferedEvents.addAndGet(-batchSize);
            }
        }
        
        if (!eventsToProcess.isEmpty()) {
            try {
                // Simulate writing to external analytics system
                simulateExternalAnalyticsWrite(eventsToProcess);
                logger.debug("Processed {} events to external system", eventsToProcess.size());
            } catch (Exception e) {
                logger.warn("Failed to process events to external system", e);
                // In production, implement retry logic or dead letter queue
            }
        }
    }

    private void monitorBuffer() {
        if (isShutdown) return;
        
        int currentBufferSize = bufferedEvents.get();
        int totalEvents = eventCounter.get();
        
        if (currentBufferSize > 0) {
            System.out.printf("   Buffer status: %d events pending, %d total processed%n", 
                currentBufferSize, totalEvents);
        }
    }

    // Helper methods

    // Note: Using existing Track table to demonstrate write-behind patterns
    // In production, you would have dedicated event tables for analytics data

    private String generateEventId() {
        return "event_" + System.currentTimeMillis() + "_" + eventCounter.incrementAndGet();
    }

    private Tuple createPlayEvent(String eventId, int customerId, int trackId) {
        // Create event using Track table schema (non-key fields only for KeyValue view)
        return Tuple.create()
            .set("Name", "Play Event " + eventId)
            .set("MediaTypeId", 1)
            .set("GenreId", 1)
            .set("Composer", "System Generated")
            .set("Milliseconds", 180000)
            .set("Bytes", 5000000)
            .set("UnitPrice", new BigDecimal("0.99"));
    }

    private void addToWriteBuffer(Tuple event) {
        synchronized (writeBuffer) {
            writeBuffer.add(event);
            bufferedEvents.incrementAndGet();
        }
    }

    private void simulateExternalAnalyticsWrite(List<Tuple> events) {
        // Simulate external analytics system processing
        try {
            // Simulate network latency and processing time
            Thread.sleep(50 + events.size() * 2);
            
            // Simulate occasional failures (5% failure rate)
            if (Math.random() < 0.05) {
                throw new RuntimeException("Simulated external system failure");
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("External analytics write interrupted", e);
        }
    }

    private void displayFinalStatistics() {
        System.out.println("\n--- Final Statistics ---");
        System.out.printf("Total events processed: %d%n", eventCounter.get());
        System.out.printf("Events still buffered: %d%n", bufferedEvents.get());
        
        double bufferUtilization = bufferedEvents.get() * 100.0 / eventCounter.get();
        System.out.printf("Buffer utilization: %.1f%%%n", bufferUtilization);
    }

    public void shutdown() {
        System.out.println("\n   Shutting down background processing");
        isShutdown = true;
        
        // Process remaining buffered events
        if (bufferedEvents.get() > 0) {
            System.out.printf("   Processing %d remaining buffered events%n", bufferedEvents.get());
            processWriteBuffer();
        }
        
        backgroundProcessor.shutdown();
        try {
            if (!backgroundProcessor.awaitTermination(5, TimeUnit.SECONDS)) {
                backgroundProcessor.shutdownNow();
            }
        } catch (InterruptedException e) {
            backgroundProcessor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("   >>> Background processing stopped");
    }
}