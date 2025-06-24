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

package com.apache.ignite.examples.filestreaming;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.DataStreamerOptions;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.catalog.IgniteCatalog;
import org.apache.ignite.catalog.definitions.ColumnDefinition;
import org.apache.ignite.catalog.ColumnType;
import org.apache.ignite.catalog.definitions.TableDefinition;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates file-to-cluster streaming with end-to-end backpressure propagation.
 * 
 * This class shows how Ignite 3's DataStreamer API naturally controls upstream
 * data production when reading from files. When cluster processing is slower than
 * file reading, the reactive streams implementation automatically pauses file I/O
 * to prevent memory bloat and system overload.
 * 
 * Key demonstrations:
 * - Line-by-line file reading controlled by DataStreamer demand
 * - Memory-efficient streaming without large buffers
 * - Automatic rate adaptation based on cluster capacity
 * - Performance monitoring with detailed metrics
 * - Recovery patterns when cluster processing speeds up
 * 
 * Business scenario:
 * Music streaming platforms often need to process large log files containing
 * user interaction events. These files can be several gigabytes and contain
 * millions of events. Processing them efficiently requires balancing file I/O
 * speed with database ingestion capacity to maintain system stability.
 * 
 * This demo uses realistic music event data to show how backpressure prevents
 * system overload when local file reading significantly outpaces distributed
 * cluster processing.
 */
public class FileBackpressureStreaming {
    
    private static final String TABLE_NAME = "FileStreamingEvents";
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    
    /**
     * Main demonstration method for file-based backpressure streaming.
     */
    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== File-Based Backpressure Streaming Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            // Setup test environment
            createTestTable(client);
            
            try {
                // Demonstrate file streaming with different scenarios
                demonstrateNormalFileStreaming(client);
                demonstrateSlowClusterScenario(client);
                demonstrateHighVelocityStreaming(client);
                
                System.out.println("\n<<< File-based backpressure streaming demonstrations completed");
                
            } finally {
                // Cleanup
                cleanupTestTable(client);
            }
            
        } catch (Exception e) {
            System.err.println("File streaming demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates the test table for file streaming demonstrations.
     */
    private static void createTestTable(IgniteClient client) {
        System.out.println("\n>>> Creating test table for file streaming");
        
        IgniteCatalog catalog = client.catalog();
        
        // Drop existing table if present
        try {
            catalog.dropTable(TABLE_NAME);
            Thread.sleep(2000); // Allow cleanup to complete
        } catch (Exception e) {
            // Table doesn't exist, continue
        }
        
        // Create table with columns matching CSV structure
        TableDefinition tableDefinition = TableDefinition.builder(TABLE_NAME)
            .columns(
                ColumnDefinition.column("EventId", ColumnType.BIGINT),
                ColumnDefinition.column("UserId", ColumnType.INTEGER),
                ColumnDefinition.column("TrackId", ColumnType.INTEGER),
                ColumnDefinition.column("EventType", ColumnType.VARCHAR),
                ColumnDefinition.column("EventTime", ColumnType.BIGINT),
                ColumnDefinition.column("Duration", ColumnType.BIGINT),
                ColumnDefinition.column("PlaylistId", ColumnType.INTEGER)
            )
            .primaryKey("EventId")
            .zone("MusicStore")
            .build();
        
        try {
            catalog.createTable(tableDefinition);
            System.out.println("<<< Test table created: " + TABLE_NAME);
            Thread.sleep(2000); // Allow table to become available
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test table", e);
        }
    }
    
    /**
     * Demonstrates normal file streaming where cluster keeps up with file reading.
     */
    private static void demonstrateNormalFileStreaming(IgniteClient client) throws Exception {
        System.out.println("\n--- Scenario 1: Normal File Streaming ---");
        
        // Generate moderate-sized sample file
        Path sampleFile = Paths.get(TEMP_DIR, "music_events_normal.csv");
        int eventCount = 50000;
        
        System.out.printf(">>> Generating sample data file (%,d events)%n", eventCount);
        SampleDataGenerator.generateMusicEventFile(sampleFile.toString(), eventCount);
        
        double fileSizeMB = SampleDataGenerator.getFileSizeMB(sampleFile);
        System.out.printf(">>> Created: %s (%.2f MB)%n", sampleFile, fileSizeMB);
        
        // Setup streaming
        StreamingMetrics metrics = new StreamingMetrics();
        FileStreamingPublisher publisher = new FileStreamingPublisher(sampleFile, metrics);
        
        Table table = client.tables().table(TABLE_NAME);
        RecordView<Tuple> recordView = table.recordView();
        
        // Configure DataStreamer for normal processing
        DataStreamerOptions options = DataStreamerOptions.builder()
            .pageSize(1000)                     // Moderate batch size
            .perPartitionParallelOperations(2)  // Standard parallelism
            .autoFlushInterval(500)             // Regular flushing
            .retryLimit(8)                      // Standard retry limit
            .build();
        
        // Start streaming with monitoring
        System.out.println(">>> Starting file streaming...");
        metrics.startStreaming();
        
        // Monitor progress during streaming
        CompletableFuture<Void> monitoringFuture = startProgressMonitoring(metrics, 2000);
        
        // Execute streaming
        CompletableFuture<Void> streamingFuture = recordView.streamData(publisher, options);
        
        // Wait for completion
        streamingFuture.get();
        metrics.stopStreaming();
        monitoringFuture.cancel(true);
        
        // Report results
        System.out.println("<<< Normal streaming completed");
        System.out.println(metrics.getDetailedReport());
        
        // Cleanup
        SampleDataGenerator.deleteFile(sampleFile);
    }
    
    /**
     * Demonstrates slow cluster scenario where backpressure controls file reading.
     */
    private static void demonstrateSlowClusterScenario(IgniteClient client) throws Exception {
        System.out.println("\n--- Scenario 2: Slow Cluster with Backpressure ---");
        
        // Generate sample file
        Path sampleFile = Paths.get(TEMP_DIR, "music_events_backpressure.csv");
        int eventCount = 30000;
        
        System.out.printf(">>> Generating sample data file (%,d events)%n", eventCount);
        SampleDataGenerator.generateMusicEventFile(sampleFile.toString(), eventCount);
        
        double fileSizeMB = SampleDataGenerator.getFileSizeMB(sampleFile);
        System.out.printf(">>> Created: %s (%.2f MB)%n", sampleFile, fileSizeMB);
        
        // Setup streaming
        StreamingMetrics metrics = new StreamingMetrics();
        FileStreamingPublisher publisher = new FileStreamingPublisher(sampleFile, metrics);
        
        Table table = client.tables().table(TABLE_NAME);
        RecordView<Tuple> recordView = table.recordView();
        
        // Configure DataStreamer for slower processing to create backpressure
        DataStreamerOptions options = DataStreamerOptions.builder()
            .pageSize(200)                      // Small batches to slow processing
            .perPartitionParallelOperations(1)  // Single-threaded processing
            .autoFlushInterval(2000)            // Slower flushing to create backpressure
            .retryLimit(8)                      // Standard retry limit
            .build();
        
        // Start streaming with monitoring
        System.out.println(">>> Starting slow cluster streaming (observe backpressure effects)...");
        metrics.startStreaming();
        metrics.setPhase("SLOW_CLUSTER");
        
        // Monitor progress during streaming
        CompletableFuture<Void> monitoringFuture = startProgressMonitoring(metrics, 3000);
        
        // Execute streaming
        CompletableFuture<Void> streamingFuture = recordView.streamData(publisher, options);
        
        // Wait for completion
        streamingFuture.get();
        metrics.stopStreaming();
        monitoringFuture.cancel(true);
        
        // Report results
        System.out.println("<<< Slow cluster streaming completed");
        System.out.println(metrics.getDetailedReport());
        
        // Cleanup
        SampleDataGenerator.deleteFile(sampleFile);
    }
    
    /**
     * Demonstrates high-velocity file streaming with rapid event generation.
     */
    private static void demonstrateHighVelocityStreaming(IgniteClient client) throws Exception {
        System.out.println("\n--- Scenario 3: High-Velocity Event Streaming ---");
        
        // Generate high-velocity sample file
        Path sampleFile = Paths.get(TEMP_DIR, "music_events_velocity.csv");
        int eventCount = 75000;
        
        System.out.printf(">>> Generating high-velocity sample data file (%,d events)%n", eventCount);
        SampleDataGenerator.generateHighVelocityEventFile(sampleFile.toString(), eventCount);
        
        double fileSizeMB = SampleDataGenerator.getFileSizeMB(sampleFile);
        System.out.printf(">>> Created: %s (%.2f MB)%n", sampleFile, fileSizeMB);
        
        // Setup streaming
        StreamingMetrics metrics = new StreamingMetrics();
        FileStreamingPublisher publisher = new FileStreamingPublisher(sampleFile, metrics);
        
        Table table = client.tables().table(TABLE_NAME);
        RecordView<Tuple> recordView = table.recordView();
        
        // Configure DataStreamer for high throughput
        DataStreamerOptions options = DataStreamerOptions.builder()
            .pageSize(2000)                     // Large batches for efficiency
            .perPartitionParallelOperations(3)  // High parallelism
            .autoFlushInterval(300)             // Fast flushing
            .retryLimit(16)                     // Higher retry tolerance
            .build();
        
        // Start streaming with monitoring
        System.out.println(">>> Starting high-velocity streaming...");
        metrics.startStreaming();
        metrics.setPhase("HIGH_VELOCITY");
        
        // Monitor progress during streaming
        CompletableFuture<Void> monitoringFuture = startProgressMonitoring(metrics, 1500);
        
        // Execute streaming
        CompletableFuture<Void> streamingFuture = recordView.streamData(publisher, options);
        
        // Wait for completion
        streamingFuture.get();
        metrics.stopStreaming();
        monitoringFuture.cancel(true);
        
        // Report results
        System.out.println("<<< High-velocity streaming completed");
        System.out.println(metrics.getDetailedReport());
        
        // Cleanup
        SampleDataGenerator.deleteFile(sampleFile);
    }
    
    /**
     * Starts background monitoring of streaming progress with periodic updates.
     */
    private static CompletableFuture<Void> startProgressMonitoring(StreamingMetrics metrics, 
                                                                  long intervalMs) {
        return CompletableFuture.runAsync(() -> {
            try {
                while (metrics.isActive() && !Thread.currentThread().isInterrupted()) {
                    Thread.sleep(intervalMs);
                    System.out.println("  " + metrics.getFormattedSummary());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    /**
     * Cleans up the test table after demonstrations.
     */
    private static void cleanupTestTable(IgniteClient client) {
        System.out.println("\n>>> Cleaning up test table");
        
        try {
            Thread.sleep(1000); // Allow operations to complete
            
            IgniteCatalog catalog = client.catalog();
            catalog.dropTable(TABLE_NAME);
            System.out.println("<<< Test table cleanup completed");
            
        } catch (Exception e) {
            System.out.println("<<< Table cleanup failed or table not found: " + e.getMessage());
        }
    }
}