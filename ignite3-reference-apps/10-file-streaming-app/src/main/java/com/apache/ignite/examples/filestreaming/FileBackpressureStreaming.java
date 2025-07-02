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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Future;

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
        
        // Create dedicated executors with proper resource management
        try (ManagedExecutorService fileDeliveryExecutor = createFileDeliveryExecutor();
             ManagedScheduledExecutorService monitoringExecutor = createMonitoringExecutor();
             IgniteClient client = IgniteClient.builder()
                     .addresses(clusterAddress)
                     .build()) {
            
            // Setup test environment
            createTestTable(client);
            
            try {
                // Demonstrate file streaming with different scenarios
                demonstrateNormalFileStreaming(client, fileDeliveryExecutor, monitoringExecutor);
                demonstrateSlowClusterScenario(client, fileDeliveryExecutor, monitoringExecutor);
                demonstrateHighVelocityStreaming(client, fileDeliveryExecutor, monitoringExecutor);
                
                System.out.println("\n<<< File-based backpressure streaming demonstrations completed");
                
            } finally {
                // Cleanup
                cleanupTestTable(client);
            }
            
        } catch (Exception e) {
            System.err.println("File streaming demo failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("<<< Application shutdown complete");
    }
    
    /**
     * Creates the test table for file streaming demonstrations using SQL DDL.
     */
    private static void createTestTable(IgniteClient client) {
        System.out.println("\n>>> Creating test table for file streaming");
        
        // Drop existing table if present
        try {
            String dropSql = "DROP TABLE IF EXISTS " + TABLE_NAME;
            client.sql().execute(null, dropSql);
            Thread.sleep(2000); // Allow cleanup to complete
        } catch (Exception e) {
            // Table doesn't exist, continue
        }
        
        // Create table with columns matching CSV structure using SQL DDL
        String createSql = """
            CREATE TABLE %s (
                EventId BIGINT PRIMARY KEY,
                UserId INTEGER,
                TrackId INTEGER,
                EventType VARCHAR,
                EventTime BIGINT,
                Duration BIGINT,
                PlaylistId INTEGER
            )
            """.formatted(TABLE_NAME);
        
        try {
            client.sql().execute(null, createSql);
            System.out.println("<<< Test table created: " + TABLE_NAME);
            Thread.sleep(2000); // Allow table to become available
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test table", e);
        }
    }
    
    /**
     * Creates a dedicated executor for file delivery operations with proper shutdown.
     */
    private static ManagedExecutorService createFileDeliveryExecutor() {
        return new ManagedExecutorService(Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "FileDelivery-Worker");
            t.setDaemon(false);
            return t;
        }));
    }
    
    /**
     * Creates a dedicated executor for monitoring operations with proper shutdown.
     */
    private static ManagedScheduledExecutorService createMonitoringExecutor() {
        return new ManagedScheduledExecutorService(Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "Monitoring-Worker");
            t.setDaemon(false);
            return t;
        }));
    }
    
    /**
     * Wrapper for ExecutorService that implements AutoCloseable with graceful shutdown.
     */
    private static class ManagedExecutorService implements ExecutorService, AutoCloseable {
        private final ExecutorService delegate;
        
        public ManagedExecutorService(ExecutorService delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public void close() {
            System.out.println(">>> Shutting down file delivery executor...");
            delegate.shutdown();
            try {
                if (!delegate.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.out.println(">>> Forcing file delivery executor shutdown...");
                    delegate.shutdownNow();
                    if (!delegate.awaitTermination(2, TimeUnit.SECONDS)) {
                        System.err.println("!!! File delivery executor did not terminate cleanly");
                    }
                }
                System.out.println("<<< File delivery executor shutdown completed");
            } catch (InterruptedException e) {
                delegate.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Delegate all ExecutorService methods
        @Override public void shutdown() { delegate.shutdown(); }
        @Override public java.util.List<Runnable> shutdownNow() { return delegate.shutdownNow(); }
        @Override public boolean isShutdown() { return delegate.isShutdown(); }
        @Override public boolean isTerminated() { return delegate.isTerminated(); }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException { return delegate.awaitTermination(timeout, unit); }
        @Override public <T> java.util.concurrent.Future<T> submit(java.util.concurrent.Callable<T> task) { return delegate.submit(task); }
        @Override public <T> java.util.concurrent.Future<T> submit(Runnable task, T result) { return delegate.submit(task, result); }
        @Override public java.util.concurrent.Future<?> submit(Runnable task) { return delegate.submit(task); }
        @Override public <T> java.util.List<java.util.concurrent.Future<T>> invokeAll(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks) throws InterruptedException { return delegate.invokeAll(tasks); }
        @Override public <T> java.util.List<java.util.concurrent.Future<T>> invokeAll(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException { return delegate.invokeAll(tasks, timeout, unit); }
        @Override public <T> T invokeAny(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks) throws InterruptedException, java.util.concurrent.ExecutionException { return delegate.invokeAny(tasks); }
        @Override public <T> T invokeAny(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException { return delegate.invokeAny(tasks, timeout, unit); }
        @Override public void execute(Runnable command) { delegate.execute(command); }
    }
    
    /**
     * Wrapper for ScheduledExecutorService that implements AutoCloseable with graceful shutdown.
     */
    private static class ManagedScheduledExecutorService implements ScheduledExecutorService, AutoCloseable {
        private final ScheduledExecutorService delegate;
        
        public ManagedScheduledExecutorService(ScheduledExecutorService delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public void close() {
            System.out.println(">>> Shutting down monitoring executor...");
            delegate.shutdown();
            try {
                if (!delegate.awaitTermination(3, TimeUnit.SECONDS)) {
                    System.out.println(">>> Forcing monitoring executor shutdown...");
                    delegate.shutdownNow();
                    if (!delegate.awaitTermination(1, TimeUnit.SECONDS)) {
                        System.err.println("!!! Monitoring executor did not terminate cleanly");
                    }
                }
                System.out.println("<<< Monitoring executor shutdown completed");
            } catch (InterruptedException e) {
                delegate.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Delegate all ScheduledExecutorService methods
        @Override public java.util.concurrent.ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) { return delegate.schedule(command, delay, unit); }
        @Override public <V> java.util.concurrent.ScheduledFuture<V> schedule(java.util.concurrent.Callable<V> callable, long delay, TimeUnit unit) { return delegate.schedule(callable, delay, unit); }
        @Override public java.util.concurrent.ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) { return delegate.scheduleAtFixedRate(command, initialDelay, period, unit); }
        @Override public java.util.concurrent.ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) { return delegate.scheduleWithFixedDelay(command, initialDelay, delay, unit); }
        @Override public void shutdown() { delegate.shutdown(); }
        @Override public java.util.List<Runnable> shutdownNow() { return delegate.shutdownNow(); }
        @Override public boolean isShutdown() { return delegate.isShutdown(); }
        @Override public boolean isTerminated() { return delegate.isTerminated(); }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException { return delegate.awaitTermination(timeout, unit); }
        @Override public <T> java.util.concurrent.Future<T> submit(java.util.concurrent.Callable<T> task) { return delegate.submit(task); }
        @Override public <T> java.util.concurrent.Future<T> submit(Runnable task, T result) { return delegate.submit(task, result); }
        @Override public java.util.concurrent.Future<?> submit(Runnable task) { return delegate.submit(task); }
        @Override public <T> java.util.List<java.util.concurrent.Future<T>> invokeAll(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks) throws InterruptedException { return delegate.invokeAll(tasks); }
        @Override public <T> java.util.List<java.util.concurrent.Future<T>> invokeAll(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException { return delegate.invokeAll(tasks, timeout, unit); }
        @Override public <T> T invokeAny(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks) throws InterruptedException, java.util.concurrent.ExecutionException { return delegate.invokeAny(tasks); }
        @Override public <T> T invokeAny(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException { return delegate.invokeAny(tasks, timeout, unit); }
        @Override public void execute(Runnable command) { delegate.execute(command); }
    }
    
    /**
     * Demonstrates normal file streaming where cluster keeps up with file reading.
     */
    private static void demonstrateNormalFileStreaming(IgniteClient client, 
                                                       ExecutorService fileExecutor,
                                                       ScheduledExecutorService monitoringExecutor) throws Exception {
        System.out.println("\n--- Scenario 1: Normal File Streaming ---");
        
        // Generate large sample file to demonstrate meaningful backpressure
        Path sampleFile = Paths.get(TEMP_DIR, "music_events_normal.csv");
        int eventCount = 1000000;
        
        System.out.printf(">>> Generating sample data file (%,d events)%n", eventCount);
        SampleDataGenerator.generateMusicEventFile(sampleFile.toString(), eventCount);
        
        double fileSizeMB = SampleDataGenerator.getFileSizeMB(sampleFile);
        System.out.printf(">>> Created: %s (%.2f MB)%n", sampleFile, fileSizeMB);
        
        // Setup streaming with explicit executor injection
        StreamingMetrics metrics = new StreamingMetrics();
        FileStreamingPublisher publisher = new FileStreamingPublisher(sampleFile, metrics, fileExecutor);
        
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
        
        // Monitor progress during streaming (more frequent updates)
        Future<?> monitoringFuture = startProgressMonitoring(metrics, 100, monitoringExecutor);
        
        try {
            // Execute streaming
            CompletableFuture<Void> streamingFuture = recordView.streamData(publisher, options);
            
            // Wait for completion
            streamingFuture.get();
        } finally {
            metrics.stopStreaming();
            monitoringFuture.cancel(true);
        }
        
        // Report results
        System.out.println("<<< Normal streaming completed");
        System.out.println(metrics.getDetailedReport());
        
        // Cleanup
        SampleDataGenerator.deleteFile(sampleFile);
    }
    
    /**
     * Demonstrates slow cluster scenario where backpressure controls file reading.
     */
    private static void demonstrateSlowClusterScenario(IgniteClient client,
                                                        ExecutorService fileExecutor,
                                                        ScheduledExecutorService monitoringExecutor) throws Exception {
        System.out.println("\n--- Scenario 2: Slow Cluster with Backpressure ---");
        
        // Generate substantial sample file to show backpressure effects
        Path sampleFile = Paths.get(TEMP_DIR, "music_events_backpressure.csv");
        int eventCount = 5000000;
        
        System.out.printf(">>> Generating sample data file (%,d events)%n", eventCount);
        SampleDataGenerator.generateMusicEventFile(sampleFile.toString(), eventCount);
        
        double fileSizeMB = SampleDataGenerator.getFileSizeMB(sampleFile);
        System.out.printf(">>> Created: %s (%.2f MB)%n", sampleFile, fileSizeMB);
        
        // Setup streaming with explicit executor injection
        StreamingMetrics metrics = new StreamingMetrics();
        FileStreamingPublisher publisher = new FileStreamingPublisher(sampleFile, metrics, fileExecutor);
        
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
        
        // Monitor progress during streaming (frequent updates to see backpressure)
        Future<?> monitoringFuture = startProgressMonitoring(metrics, 100, monitoringExecutor);
        
        try {
            // Execute streaming
            CompletableFuture<Void> streamingFuture = recordView.streamData(publisher, options);
            
            // Wait for completion
            streamingFuture.get();
        } finally {
            metrics.stopStreaming();
            monitoringFuture.cancel(true);
        }
        
        // Report results
        System.out.println("<<< Slow cluster streaming completed");
        System.out.println(metrics.getDetailedReport());
        
        // Cleanup
        SampleDataGenerator.deleteFile(sampleFile);
    }
    
    /**
     * Demonstrates high-velocity file streaming with rapid event generation.
     */
    private static void demonstrateHighVelocityStreaming(IgniteClient client,
                                                         ExecutorService fileExecutor,
                                                         ScheduledExecutorService monitoringExecutor) throws Exception {
        System.out.println("\n--- Scenario 3: High-Velocity Event Streaming ---");
        
        // Generate large high-velocity sample file (1M records)
        Path sampleFile = Paths.get(TEMP_DIR, "music_events_velocity.csv");
        int eventCount = 10000000;
        
        System.out.printf(">>> Generating high-velocity sample data file (%,d events)%n", eventCount);
        SampleDataGenerator.generateHighVelocityEventFile(sampleFile.toString(), eventCount);
        
        double fileSizeMB = SampleDataGenerator.getFileSizeMB(sampleFile);
        System.out.printf(">>> Created: %s (%.2f MB)%n", sampleFile, fileSizeMB);
        
        // Setup streaming with explicit executor injection
        StreamingMetrics metrics = new StreamingMetrics();
        FileStreamingPublisher publisher = new FileStreamingPublisher(sampleFile, metrics, fileExecutor);
        
        Table table = client.tables().table(TABLE_NAME);
        RecordView<Tuple> recordView = table.recordView();
        
        // Configure DataStreamer for high throughput
        DataStreamerOptions options = DataStreamerOptions.builder()
            .pageSize(2000)                     // Large batches for efficiency
            .perPartitionParallelOperations(3)  // High parallelism
            .autoFlushInterval(30)              // Fast flushing
            .retryLimit(16)                     // Higher retry tolerance
            .build();
        
        // Start streaming with monitoring
        System.out.println(">>> Starting high-velocity streaming...");
        metrics.startStreaming();
        metrics.setPhase("HIGH_VELOCITY");
        
        // Monitor progress during streaming (frequent updates for high throughput)
        Future<?> monitoringFuture = startProgressMonitoring(metrics, 100, monitoringExecutor);
        
        try {
            // Execute streaming
            CompletableFuture<Void> streamingFuture = recordView.streamData(publisher, options);
            
            // Wait for completion
            streamingFuture.get();
        } finally {
            metrics.stopStreaming();
            monitoringFuture.cancel(true);
        }
        
        // Report results
        System.out.println("<<< High-velocity streaming completed");
        System.out.println(metrics.getDetailedReport());
        
        // Cleanup
        SampleDataGenerator.deleteFile(sampleFile);
    }
    
    /**
     * Starts background monitoring of streaming progress with periodic updates.
     * Uses dedicated ScheduledExecutorService for proper thread management.
     */
    private static Future<?> startProgressMonitoring(StreamingMetrics metrics, 
                                                     long intervalMs,
                                                     ScheduledExecutorService executor) {
        return executor.scheduleWithFixedDelay(
            () -> {
                if (metrics.isActive()) {
                    System.out.println("+++ " + metrics.getFormattedSummary());
                }
            },
            0, // initial delay
            intervalMs,
            TimeUnit.MILLISECONDS
        );
    }
    
    /**
     * Cleans up the test table after demonstrations using SQL DDL.
     */
    private static void cleanupTestTable(IgniteClient client) {
        System.out.println("\n>>> Cleaning up test table");
        
        try {
            Thread.sleep(1000); // Allow operations to complete
            
            String dropSql = "DROP TABLE IF EXISTS " + TABLE_NAME;
            client.sql().execute(null, dropSql);
            System.out.println("<<< Test table cleanup completed");
            
        } catch (Exception e) {
            System.out.println("<<< Table cleanup failed or table not found: " + e.getMessage());
        }
    }
}