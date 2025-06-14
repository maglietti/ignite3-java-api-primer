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

package com.apache.ignite.examples.streaming;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.DataStreamerItem;
import org.apache.ignite.table.DataStreamerOptions;
import org.apache.ignite.table.Tuple;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.CompletableFuture;

/**
 * Demonstrates basic data streaming operations using Ignite 3's DataStreamer API.
 * 
 * This example shows how to stream music track listening events into an Ignite table
 * using the reactive streams-based data streaming interface. The streaming API provides
 * high-throughput data ingestion with intelligent batching and backpressure handling.
 * 
 * Key concepts demonstrated:
 * - Basic data streaming with DataStreamerItem
 * - Configuration options for performance tuning
 * - PUT and REMOVE operations in streaming context
 * - Resource management with try-with-resources
 * 
 * Business scenario:
 * A music streaming service needs to ingest millions of track listening events
 * in real-time. Each event represents when a user starts, pauses, or completes
 * playing a track. Traditional INSERT statements would create bottlenecks,
 * but the streaming API can handle thousands of events per second.
 */
public class BasicDataStreamerDemo {
    
    /**
     * Main demonstration method showing basic streaming patterns.
     * Creates a table for track events and demonstrates streaming operations.
     */
    public static void main(String[] args) {
        // Connect to Ignite cluster using client connector
        try (IgniteClient ignite = IgniteClient.builder()
                .addresses("localhost:10800")
                .build()) {
            
            System.out.println("=== Basic Data Streaming Demo ===");
            
            // Get table view for streaming operations
            RecordView<Tuple> trackEventsView = ignite.tables()
                .table("TrackEvents")
                .recordView();
            
            // Demonstrate basic streaming
            demonstrateBasicStreaming(trackEventsView);
            
            // Demonstrate streaming with performance tuning
            demonstratePerformanceTuning(trackEventsView);
            
            // Demonstrate mixed operations (PUT/REMOVE)
            demonstrateMixedOperations(trackEventsView);
            
            System.out.println("Basic data streaming demonstration completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Demonstrates the most basic streaming pattern with default options.
     * Streams a series of track listening events using PUT operations.
     */
    private static void demonstrateBasicStreaming(RecordView<Tuple> trackEventsView) {
        System.out.println("\n--- Basic Streaming with Default Options    ---");
        
        try {
            // Create publisher for streaming track events
            try (SubmissionPublisher<DataStreamerItem<Tuple>> publisher = 
                    new SubmissionPublisher<>()) {
                
                // Start streaming with default options
                CompletableFuture<Void> streamingFuture = trackEventsView
                    .streamData(publisher, DataStreamerOptions.DEFAULT);
                
                // Generate and stream sample track events
                System.out.println("Streaming 1,000 track events...");
                
                for (int i = 1; i <= 1000; i++) {
                    Tuple trackEvent = Tuple.create()
                        .set("EventId", i)
                        .set("UserId", 1000 + (i % 50))           // 50 different users
                        .set("TrackId", 1 + (i % 25))             // 25 different tracks
                        .set("EventType", getEventType(i))        // Realistic event types
                        .set("Timestamp", System.currentTimeMillis() + i * 1000)
                        .set("Duration", getDuration(i));         // Track duration in ms
                    
                    // Stream as PUT operation (insert/update)
                    publisher.submit(DataStreamerItem.of(trackEvent));
                }
                
                // Close publisher to signal completion
                publisher.close();
                
                // Wait for streaming to complete
                streamingFuture.get();
                
                System.out.println("✓ Successfully streamed 1,000 events with default settings");
            }
            
        } catch (Exception e) {
            System.err.println("Basic streaming failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates streaming with customized performance options.
     * Shows how to tune batch sizes, parallelism, and timing for optimal throughput.
     */
    private static void demonstratePerformanceTuning(RecordView<Tuple> trackEventsView) {
        System.out.println("\n--- Performance-Tuned Streaming    ---");
        
        // Configure options for high-throughput streaming
        DataStreamerOptions highThroughputOptions = DataStreamerOptions.builder()
            .pageSize(2000)                    // Large batches for better throughput
            .perPartitionParallelOperations(3) // Higher parallelism
            .autoFlushInterval(500)            // Faster flushing for responsiveness
            .retryLimit(16)                    // Reasonable retry limit
            .build();
        
        try {
            try (SubmissionPublisher<DataStreamerItem<Tuple>> publisher = 
                    new SubmissionPublisher<>()) {
                
                System.out.println("Streaming 5,000 events with performance tuning...");
                System.out.println("Configuration: pageSize=2000, parallelOperations=3, autoFlush=500ms");
                
                CompletableFuture<Void> streamingFuture = trackEventsView
                    .streamData(publisher, highThroughputOptions);
                
                long startTime = System.currentTimeMillis();
                
                // Stream larger dataset with performance monitoring
                for (int i = 2001; i <= 7000; i++) {
                    Tuple trackEvent = Tuple.create()
                        .set("EventId", i)
                        .set("UserId", 2000 + (i % 100))          // 100 different users
                        .set("TrackId", 1 + (i % 50))             // 50 different tracks
                        .set("EventType", getEventType(i))
                        .set("Timestamp", System.currentTimeMillis() + i * 500)
                        .set("Duration", getDuration(i));
                    
                    publisher.submit(DataStreamerItem.of(trackEvent));
                    
                    // Progress indicator every 1000 events
                    if (i % 1000 == 0) {
                        System.out.println("  Submitted " + (i - 2000) + " events...");
                    }
                }
                
                publisher.close();
                streamingFuture.get();
                
                long duration = System.currentTimeMillis() - startTime;
                double throughput = 5000.0 / duration * 1000; // events per second
                
                System.out.printf("✓ Streamed 5,000 events in %d ms (%.2f events/sec)%n", 
                    duration, throughput);
            }
            
        } catch (Exception e) {
            System.err.println("Performance tuning demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates mixed streaming operations including both PUT and REMOVE.
     * Shows how to handle different operation types in a single streaming session.
     */
    private static void demonstrateMixedOperations(RecordView<Tuple> trackEventsView) {
        System.out.println("\n--- Mixed Operations (PUT/REMOVE) Streaming    ---");
        
        // Configure for mixed operations with balanced settings
        DataStreamerOptions mixedOptions = DataStreamerOptions.builder()
            .pageSize(1000)                    // Moderate batch size
            .perPartitionParallelOperations(2) // Balanced parallelism
            .autoFlushInterval(1000)           // Standard flush interval
            .retryLimit(8)                     // Lower retry for faster failure detection
            .build();
        
        try {
            try (SubmissionPublisher<DataStreamerItem<Tuple>> publisher = 
                    new SubmissionPublisher<>()) {
                
                System.out.println("Streaming mixed operations (inserts, updates, deletes)...");
                
                CompletableFuture<Void> streamingFuture = trackEventsView
                    .streamData(publisher, mixedOptions);
                
                // Simulate realistic streaming lifecycle
                for (int sessionId = 1; sessionId <= 500; sessionId++) {
                    int userId = 3000 + (sessionId % 25);
                    int trackId = 1 + (sessionId % 20);
                    long baseEventId = 10000 + sessionId * 3;
                    
                    // 1. Track started event (PUT operation)
                    Tuple startEvent = Tuple.create()
                        .set("EventId", baseEventId)
                        .set("UserId", userId)
                        .set("TrackId", trackId)
                        .set("EventType", "TRACK_STARTED")
                        .set("Timestamp", System.currentTimeMillis() + sessionId * 1000)
                        .set("Duration", 0);
                    
                    publisher.submit(DataStreamerItem.of(startEvent));
                    
                    // 2. Track completed event (PUT operation - update with duration)
                    Tuple completeEvent = Tuple.create()
                        .set("EventId", baseEventId + 1)
                        .set("UserId", userId)
                        .set("TrackId", trackId)
                        .set("EventType", "TRACK_COMPLETED")
                        .set("Timestamp", System.currentTimeMillis() + sessionId * 1000 + 180000)
                        .set("Duration", 180000 + (sessionId % 60000)); // 3-4 minutes
                    
                    publisher.submit(DataStreamerItem.of(completeEvent));
                    
                    // 3. Occasionally remove old/invalid events (REMOVE operation)
                    if (sessionId % 20 == 0) {
                        Tuple obsoleteEvent = Tuple.create()
                            .set("EventId", baseEventId - 60); // Remove older event
                        
                        publisher.submit(DataStreamerItem.removed(obsoleteEvent));
                    }
                    
                    // Progress indication
                    if (sessionId % 100 == 0) {
                        System.out.println("  Processed " + sessionId + " listening sessions...");
                    }
                }
                
                publisher.close();
                streamingFuture.get();
                
                System.out.println("✓ Successfully processed 500 listening sessions with mixed operations");
                System.out.println("  Operations included: TRACK_STARTED, TRACK_COMPLETED, and cleanup deletions");
            }
            
        } catch (Exception e) {
            System.err.println("Mixed operations demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to generate realistic event types based on sequence.
     */
    private static String getEventType(int sequence) {
        String[] eventTypes = {
            "TRACK_STARTED", "TRACK_PAUSED", "TRACK_RESUMED", 
            "TRACK_COMPLETED", "TRACK_SKIPPED", "TRACK_LIKED"
        };
        return eventTypes[sequence % eventTypes.length];
    }
    
    /**
     * Helper method to generate realistic track durations.
     */
    private static int getDuration(int sequence) {
        // Generate durations between 30 seconds and 8 minutes
        int baseDuration = 30000; // 30 seconds
        int variation = sequence % 450000; // Up to 7.5 minutes variation
        return baseDuration + variation;
    }
}