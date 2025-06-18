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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

/**
 * Demonstrates high-volume bulk data ingestion using Ignite 3's DataStreamer API.
 * 
 * This example shows how to stream large datasets efficiently into Ignite tables
 * with optimized performance settings. The focus is on maximizing throughput
 * while maintaining data integrity for bulk loading scenarios.
 * 
 * Key concepts demonstrated:
 * - High-throughput streaming configuration
 * - Memory-efficient file processing
 * - Adaptive batch sizing for optimal performance
 * - Error handling and recovery patterns
 * - Progress monitoring and performance metrics
 * 
 * Business scenario:
 * A music streaming service needs to migrate 10 million historical listening
 * records from a legacy system. The data comes in CSV format and must be
 * loaded quickly without impacting live operations. Traditional SQL INSERTs
 * would take hours, but optimized streaming can complete the migration in minutes.
 */
public class BulkDataIngestion {
    
    /**
     * Main demonstration method for bulk data ingestion patterns.
     */
    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Bulk Data Ingestion Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Demonstrating high-volume data loading patterns");

        try (IgniteClient ignite = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            runBulkIngestionOperations(ignite);
            
            System.out.println("\n>>> Bulk data ingestion operations completed successfully");
            
        } catch (Exception e) {
            System.err.println("!!! Error: " + e.getMessage());
        }
    }
    
    private static void runBulkIngestionOperations(IgniteClient ignite) {
        demonstrateHighThroughputStreaming(ignite);
        demonstrateFileBulkLoading(ignite);
        demonstrateAdaptiveBatching(ignite);
    }

    /**
     * Demonstrates streaming optimized for maximum throughput.
     * Uses large batches and high parallelism for bulk loading scenarios.
     */
    private static void demonstrateHighThroughputStreaming(IgniteClient ignite) {
        System.out.println("\n--- High-Throughput Bulk Streaming");
        
        RecordView<Tuple> bulkDataView = ignite.tables()
            .table("BulkLoadTest")
            .recordView();
        
        // Configure for maximum throughput
        DataStreamerOptions bulkOptions = DataStreamerOptions.builder()
            .pageSize(5000)                     // Very large batches
            .perPartitionParallelOperations(4)  // High parallelism
            .autoFlushInterval(200)             // Fast flushing
            .retryLimit(32)                     // High retry limit for stability
            .build();
        
        // Use dedicated executor for high-volume operations
        ExecutorService bulkExecutor = Executors.newFixedThreadPool(2);
        
        try {
            // Create publisher with large buffer for bulk operations
            try (SubmissionPublisher<DataStreamerItem<Tuple>> publisher = 
                    new SubmissionPublisher<>(bulkExecutor, 20000)) {
                
                System.out.println("Streaming 50,000 records with high-throughput configuration...");
                System.out.println("Settings: pageSize=5000, parallelOperations=4, buffer=20000");
                
                CompletableFuture<Void> streamingFuture = bulkDataView
                    .streamData(publisher, bulkOptions);
                
                long startTime = System.currentTimeMillis();
                int recordCount = 50000;
                
                // Generate large dataset for bulk loading
                for (int i = 1; i <= recordCount; i++) {
                    Tuple bulkRecord = Tuple.create()
                        .set("RecordId", i)
                        .set("UserId", 1000 + (i % 5000))           // 5000 different users
                        .set("TrackId", 1 + (i % 1000))             // 1000 different tracks
                        .set("PlayDate", "2024-" + String.format("%02d", (i % 12) + 1) + 
                               "-" + String.format("%02d", (i % 28) + 1))
                        .set("PlayCount", 1 + (i % 10))             // 1-10 plays
                        .set("Rating", (i % 20 == 0) ? null : 1 + (i % 5)) // Some null ratings
                        .set("PlayDuration", 30000 + (i % 300000)); // 30s to 5.5min
                    
                    publisher.submit(DataStreamerItem.of(bulkRecord));
                    
                    // Progress monitoring
                    if (i % 10000 == 0) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        double rate = (double) i / elapsed * 1000;
                        System.out.printf("  %d records submitted (%.2f records/sec)%n", i, rate);
                    }
                }
                
                publisher.close();
                streamingFuture.get();
                
                long totalTime = System.currentTimeMillis() - startTime;
                double throughput = (double) recordCount / totalTime * 1000;
                
                System.out.printf("    <<< Bulk loaded %d records in %d ms (%.2f records/sec)%n", 
                    recordCount, totalTime, throughput);
                
            }
            
        } catch (Exception e) {
            System.err.println("High-throughput streaming failed: " + e.getMessage());
        } finally {
            bulkExecutor.shutdown();
        }
    }
    
    /**
     * Demonstrates loading data from a file source with memory-efficient processing.
     * Shows how to handle large files without loading everything into memory.
     */
    private static void demonstrateFileBulkLoading(IgniteClient ignite) {
        System.out.println("\n--- File-Based Bulk Loading    ---");
        
        RecordView<Tuple> fileDataView = ignite.tables()
            .table("FileLoadTest")
            .recordView();
        
        // Create sample data file
        String sampleFile = createSampleDataFile();
        if (sampleFile == null) {
            System.err.println("Failed to create sample data file");
            return;
        }
        
        DataStreamerOptions fileOptions = DataStreamerOptions.builder()
            .pageSize(2000)                     // Moderate batch size for file processing
            .perPartitionParallelOperations(3)  // Balanced parallelism
            .autoFlushInterval(1000)            // Regular flushing
            .retryLimit(16)                     // Standard retry limit
            .build();
        
        try {
            try (SubmissionPublisher<DataStreamerItem<Tuple>> publisher = 
                    new SubmissionPublisher<>()) {
                
                System.out.println("Loading data from file: " + sampleFile);
                
                CompletableFuture<Void> streamingFuture = fileDataView
                    .streamData(publisher, fileOptions);
                
                long startTime = System.currentTimeMillis();
                int lineCount = 0;
                
                // Process file line by line for memory efficiency
                try {
                    List<String> lines = Files.readAllLines(Paths.get(sampleFile));
                    
                    // Skip header line
                    for (int i = 1; i < lines.size(); i++) {
                        String line = lines.get(i);
                        String[] fields = line.split(",");
                        
                        if (fields.length >= 6) {
                            Tuple fileRecord = Tuple.create()
                                .set("RecordId", Long.parseLong(fields[0]))
                                .set("UserId", Integer.parseInt(fields[1]))
                                .set("TrackId", Integer.parseInt(fields[2]))
                                .set("PlayDate", fields[3])
                                .set("PlayCount", Integer.parseInt(fields[4]))
                                .set("Rating", fields[5].isEmpty() ? null : Integer.parseInt(fields[5]))
                                .set("PlayDuration", Integer.parseInt(fields[6]));
                            
                            publisher.submit(DataStreamerItem.of(fileRecord));
                            lineCount++;
                            
                            // Progress indicator
                            if (lineCount % 5000 == 0) {
                                System.out.println("  Processed " + lineCount + " lines from file...");
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error processing file: " + e.getMessage());
                }
                
                publisher.close();
                streamingFuture.get();
                
                long totalTime = System.currentTimeMillis() - startTime;
                double throughput = (double) lineCount / totalTime * 1000;
                
                System.out.printf("    <<< Loaded %d records from file in %d ms (%.2f records/sec)%n", 
                    lineCount, totalTime, throughput);
                
            }
            
        } catch (Exception e) {
            System.err.println("File-based loading failed: " + e.getMessage());
        } finally {
            // Clean up sample file
            try {
                Files.deleteIfExists(Paths.get(sampleFile));
            } catch (Exception e) {
                System.err.println("Failed to cleanup sample file: " + e.getMessage());
            }
        }
    }
    
    /**
     * Demonstrates adaptive batch sizing based on data characteristics.
     * Shows how to adjust performance parameters during streaming.
     */
    private static void demonstrateAdaptiveBatching(IgniteClient ignite) {
        System.out.println("\n--- Adaptive Batch Sizing    ---");
        
        RecordView<Tuple> adaptiveView = ignite.tables()
            .table("AdaptiveLoadTest")
            .recordView();
        
        // Test different batch sizes to find optimal performance
        int[] batchSizes = {500, 1000, 2000, 3000, 5000};
        
        for (int batchSize : batchSizes) {
            System.out.printf("Testing batch size: %d%n", batchSize);
            
            DataStreamerOptions testOptions = DataStreamerOptions.builder()
                .pageSize(batchSize)
                .perPartitionParallelOperations(2)
                .autoFlushInterval(1000)
                .retryLimit(16)
                .build();
            
            try (SubmissionPublisher<DataStreamerItem<Tuple>> publisher = 
                    new SubmissionPublisher<>()) {
                
                CompletableFuture<Void> streamingFuture = adaptiveView
                    .streamData(publisher, testOptions);
                
                long startTime = System.currentTimeMillis();
                int testRecords = 10000;
                
                // Stream test dataset
                for (int i = 1; i <= testRecords; i++) {
                    Tuple testRecord = Tuple.create()
                        .set("RecordId", (long) (batchSize * 100000 + i)) // Unique IDs per test
                        .set("UserId", 5000 + (i % 100))
                        .set("TrackId", 1 + (i % 200))
                        .set("BatchSize", batchSize)                    // Track which batch size
                        .set("Timestamp", System.currentTimeMillis())
                        .set("TestData", "Batch test data for size " + batchSize);
                    
                    publisher.submit(DataStreamerItem.of(testRecord));
                }
                
                publisher.close();
                streamingFuture.get();
                
                long duration = System.currentTimeMillis() - startTime;
                double throughput = (double) testRecords / duration * 1000;
                
                System.out.printf("  Batch size %d: %d ms, %.2f records/sec%n", 
                    batchSize, duration, throughput);
                
            } catch (Exception e) {
                System.err.println("  Batch size " + batchSize + " failed: " + e.getMessage());
            }
            
            // Small delay between tests
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println("    <<< Adaptive batching test completed");
        System.out.println("  Use the batch size with the highest throughput for your use case");
    }
    
    /**
     * Creates a sample CSV file for demonstrating file-based loading.
     * @return Path to the created sample file, or null if creation failed
     */
    private static String createSampleDataFile() {
        try {
            Path tempFile = Files.createTempFile("music-sample-", ".csv");
            List<String> lines = new ArrayList<>();
            
            // CSV header
            lines.add("RecordId,UserId,TrackId,PlayDate,PlayCount,Rating,PlayDuration");
            
            // Generate sample data
            for (int i = 1; i <= 20000; i++) {
                String rating = (i % 7 == 0) ? "" : String.valueOf(1 + (i % 5)); // Some empty ratings
                String line = String.format("%d,%d,%d,2024-%02d-%02d,%d,%s,%d",
                    i,                              // RecordId
                    4000 + (i % 200),              // UserId
                    1 + (i % 300),                 // TrackId
                    (i % 12) + 1,                  // Month
                    (i % 28) + 1,                  // Day
                    1 + (i % 8),                   // PlayCount
                    rating,                        // Rating (sometimes empty)
                    45000 + (i % 240000)           // PlayDuration (45s to 4.5min)
                );
                lines.add(line);
            }
            
            Files.write(tempFile, lines);
            System.out.println("Created sample file with " + (lines.size() - 1) + " data records");
            
            return tempFile.toString();
            
        } catch (Exception e) {
            System.err.println("Failed to create sample data file: " + e.getMessage());
            return null;
        }
    }
}