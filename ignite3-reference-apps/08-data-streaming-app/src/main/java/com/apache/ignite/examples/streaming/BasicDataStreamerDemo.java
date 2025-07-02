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
import org.apache.ignite.catalog.IgniteCatalog;
import org.apache.ignite.catalog.definitions.ColumnDefinition;
import org.apache.ignite.catalog.ColumnType;
import org.apache.ignite.catalog.definitions.TableDefinition;

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
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Basic Data Streaming Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Demonstrating fundamental streaming patterns");

        try (IgniteClient ignite = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            // Create test table
            createTestTable(ignite);
            
            try {
                // Get table view for streaming operations
                RecordView<Tuple> trackEventsView = ignite.tables()
                    .table("TrackEvents")
                    .recordView();
                
                runBasicStreamingOperations(trackEventsView);
                System.out.println("\n>>> Basic data streaming operations completed");
                
            } finally {
                // Clean up test table
                cleanupTestTable(ignite);
            }
            
        } catch (Exception e) {
            System.err.println("!!! Error: " + e.getMessage());
        }
    }
    
    /**
     * Creates test table for streaming demonstrations.
     */
    private static void createTestTable(IgniteClient ignite) {
        System.out.println(">>> Creating TrackEvents table for streaming demonstrations");
        
        IgniteCatalog catalog = ignite.catalog();
        
        // Table recreation ensures consistent schema and data isolation
        try {
            catalog.dropTable("TrackEvents");
            System.out.println("<<< Dropped existing TrackEvents table");
            Thread.sleep(2000); // Distributed cluster operations require coordination time
        } catch (Exception e) {
            // Table doesn't exist, continue
        }
        
        TableDefinition trackEventsTable = TableDefinition.builder("TrackEvents")
            .columns(
                ColumnDefinition.column("EventId", ColumnType.BIGINT),
                ColumnDefinition.column("UserId", ColumnType.INTEGER),
                ColumnDefinition.column("TrackId", ColumnType.INTEGER),
                ColumnDefinition.column("EventType", ColumnType.VARCHAR),
                ColumnDefinition.column("EventTime", ColumnType.BIGINT),
                ColumnDefinition.column("Duration", ColumnType.INTEGER)
            )
            .primaryKey("EventId")
            .zone("MusicStore")
            .build();
        
        try {
            catalog.createTable(trackEventsTable);
            System.out.println("<<< TrackEvents table created");
            Thread.sleep(2000); // Schema propagation across cluster nodes
        } catch (Exception e) {
            System.out.println("<<< Failed to create TrackEvents table: " + e.getMessage());
            throw new RuntimeException("Table creation failed", e);
        }
    }
    
    /**
     * Cleans up test table after demonstration.
     */
    private static void cleanupTestTable(IgniteClient ignite) {
        System.out.println(">>> Cleaning up TrackEvents table");
        
        try {
            // Streaming operations complete asynchronously before table operations
            Thread.sleep(1000);
            
            IgniteCatalog catalog = ignite.catalog();
            catalog.dropTable("TrackEvents");
            System.out.println("<<< TrackEvents table dropped");
        } catch (Exception e) {
            System.out.println("<<< TrackEvents table not found or already dropped");
        }
    }
    
    /**
     * Demonstrates the most basic streaming pattern with default options.
     * Streams a series of track listening events using PUT operations.
     */
    private static void runBasicStreamingOperations(RecordView<Tuple> trackEventsView) {
        demonstrateBasicStreaming(trackEventsView);
        demonstratePerformanceTuning(trackEventsView);
        demonstrateMixedOperations(trackEventsView);
    }

    private static void demonstrateBasicStreaming(RecordView<Tuple> trackEventsView) {
        System.out.println("\n--- Basic Streaming with Default Options");
        
        try {
            // Create publisher for streaming track events
            try (SubmissionPublisher<DataStreamerItem<Tuple>> publisher = 
                    new SubmissionPublisher<>()) {
                
                // Start streaming with default options
                CompletableFuture<Void> streamingFuture = trackEventsView
                    .streamData(publisher, DataStreamerOptions.DEFAULT);
                
                // Generate and stream sample track events
                System.out.println(">>> Streaming 1,000 track events");
                
                for (int i = 1; i <= 1000; i++) {
                    Tuple trackEvent = Tuple.create()
                        .set("EventId", (long) i)
                        .set("UserId", 1000 + (i % 50))           // 50 different users
                        .set("TrackId", 1 + (i % 25))             // 25 different tracks
                        .set("EventType", getEventType(i))        // Realistic event types
                        .set("EventTime", System.currentTimeMillis() + i * 1000)
                        .set("Duration", getDuration(i));         // Track duration in ms
                    
                    // Stream as PUT operation (insert/update)
                    publisher.submit(DataStreamerItem.of(trackEvent));
                }
                
                // Close publisher to signal completion
                publisher.close();
                
                // Wait for streaming to complete
                streamingFuture.get();
                
                System.out.println("<<< Streamed 1,000 events with default settings");
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
                        .set("EventId", (long) i)
                        .set("UserId", 2000 + (i % 100))          // 100 different users
                        .set("TrackId", 1 + (i % 50))             // 50 different tracks
                        .set("EventType", getEventType(i))
                        .set("EventTime", System.currentTimeMillis() + i * 500)
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
                
                System.out.printf("<<< Streamed 5,000 events in %d ms (%.2f events/sec)%n", 
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
        System.out.println("\n--- Mixed Operations (PUT/REMOVE) Streaming");
        
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
                        .set("EventTime", System.currentTimeMillis() + sessionId * 1000)
                        .set("Duration", 0);
                    
                    publisher.submit(DataStreamerItem.of(startEvent));
                    
                    // 2. Track completed event (PUT operation - update with duration)
                    Tuple completeEvent = Tuple.create()
                        .set("EventId", baseEventId + 1)
                        .set("UserId", userId)
                        .set("TrackId", trackId)
                        .set("EventType", "TRACK_COMPLETED")
                        .set("EventTime", System.currentTimeMillis() + sessionId * 1000 + 180000)
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
                
                System.out.println("<<< Processed 500 listening sessions with mixed operations");
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