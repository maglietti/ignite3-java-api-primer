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

import org.apache.ignite.table.DataStreamerItem;
import org.apache.ignite.table.Tuple;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ForkJoinPool;

/**
 * Custom Flow.Publisher that reads CSV files line-by-line with true backpressure control.
 * 
 * This publisher demonstrates end-to-end backpressure propagation from file I/O to
 * cluster ingestion. It only reads lines from the CSV file when the DataStreamer
 * requests them, preventing memory bloat and controlling upstream data production
 * based on downstream consumption capacity.
 * 
 * Key backpressure features:
 * - Demand-driven file reading: only reads when subscriber requests data
 * - No speculative reading ahead to prevent memory accumulation
 * - Automatic rate adaptation based on cluster processing speed
 * - Comprehensive metrics tracking for backpressure analysis
 * - Resource cleanup with proper file handle management
 * 
 * The implementation follows the Reactive Streams specification for Flow.Publisher,
 * ensuring compatibility with Ignite 3's DataStreamer reactive streaming API.
 */
public class FileStreamingPublisher implements Flow.Publisher<DataStreamerItem<Tuple>> {
    
    private final Path csvFilePath;
    private final StreamingMetrics metrics;
    private final AtomicBoolean subscribed = new AtomicBoolean(false);
    
    /**
     * Creates a new file streaming publisher for the specified CSV file.
     * 
     * @param csvFilePath path to the CSV file to stream
     * @param metrics metrics tracker for monitoring performance
     */
    public FileStreamingPublisher(Path csvFilePath, StreamingMetrics metrics) {
        this.csvFilePath = csvFilePath;
        this.metrics = metrics;
    }
    
    @Override
    public void subscribe(Flow.Subscriber<? super DataStreamerItem<Tuple>> subscriber) {
        if (subscriber == null) {
            throw new NullPointerException("Subscriber cannot be null");
        }
        
        if (subscribed.compareAndSet(false, true)) {
            try {
                BufferedReader reader = Files.newBufferedReader(csvFilePath);
                FileSubscription subscription = new FileSubscription(subscriber, reader);
                subscriber.onSubscribe(subscription);
            } catch (IOException e) {
                subscriber.onError(new RuntimeException("Failed to open CSV file: " + csvFilePath, e));
            }
        } else {
            subscriber.onError(new IllegalStateException("Publisher already has a subscriber"));
        }
    }
    
    /**
     * Subscription implementation that provides demand-driven file reading.
     * Only reads lines from the CSV file when explicitly requested by the subscriber.
     */
    private class FileSubscription implements Flow.Subscription {
        private final Flow.Subscriber<? super DataStreamerItem<Tuple>> subscriber;
        private final BufferedReader reader;
        private final AtomicLong demand = new AtomicLong(0);
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicBoolean headerSkipped = new AtomicBoolean(false);
        private final AtomicBoolean delivering = new AtomicBoolean(false);
        
        public FileSubscription(Flow.Subscriber<? super DataStreamerItem<Tuple>> subscriber, 
                               BufferedReader reader) {
            this.subscriber = subscriber;
            this.reader = reader;
        }
        
        @Override
        public void request(long n) {
            if (n <= 0) {
                subscriber.onError(new IllegalArgumentException("Request count must be positive"));
                return;
            }
            
            if (cancelled.get()) {
                return;
            }
            
            // Record the demand for metrics  
            metrics.recordEventsRequested(n);
            
            // Add to demand and trigger delivery
            long currentDemand = demand.addAndGet(n);
            
            // Debug logging for significant requests
            //if (currentDemand <= 10000 && (n > 1000 || currentDemand % 1000 == 0)) {
            //    System.out.printf("!!! Request received: %d items, total demand: %d%n", n, currentDemand);
            //}
            
            deliverItems();
        }
        
        @Override
        public void cancel() {
            cancelled.set(true);
            closeReader();
        }
        
        /**
         * Delivers items based on current demand, reading lines only as needed.
         * Uses a guard to prevent concurrent deliveries from multiple request calls.
         */
        private void deliverItems() {
            // Only start delivery if not already delivering
            if (delivering.compareAndSet(false, true)) {
                deliverItemsAsync()
                    .whenComplete((result, error) -> {
                        delivering.set(false); // Allow future deliveries
                        if (error != null) {
                            subscriber.onError(error);
                            closeReader();
                        }
                    });
            }
        }
        
        /**
         * Delivers items synchronously in batches to satisfy demand.
         * This simpler approach avoids async complications and delivers items immediately.
         */
        private CompletableFuture<Void> deliverItemsAsync() {
            return CompletableFuture.runAsync(() -> {
                deliverItemsSynchronously();
            });
        }
        
        /**
         * Delivers items synchronously to satisfy current demand.
         * This approach is simpler and more reliable than complex async chains.
         */
        private void deliverItemsSynchronously() {
            long startingDemand = demand.get();
            //if (startingDemand <= 1000) {
            //    System.out.printf("!!! Starting delivery with demand: %d%n", startingDemand);
            //}
            
            while (demand.get() > 0 && !cancelled.get()) {
                try {
                    String line = readNextLine();
                    
                    if (line == null) {
                        // End of file reached
                        subscriber.onComplete();
                        closeReader();
                        return;
                    }
                    
                    // Skip header line
                    if (!headerSkipped.getAndSet(true)) {
                        continue;
                    }
                    
                    // Parse and deliver item, skip malformed lines
                    try {
                        DataStreamerItem<Tuple> item = parseLineToDataStreamerItem(line);
                        subscriber.onNext(item);
                        metrics.recordEventPublished();
                        demand.decrementAndGet();
                        
                        // Debug logging for first few items
                        //if (metrics.getEventsPublished() <= 5) {
                        //    System.out.printf(">>> Published item %d: %s%n", 
                        //       metrics.getEventsPublished(), line);
                        //}
                        
                    } catch (IllegalArgumentException e) {
                        // Skip malformed lines and continue processing
                        System.out.printf("!!! Skipping malformed line: %s (error: %s)%n", line, e.getMessage());
                        continue;
                    }
                    
                } catch (IOException e) {
                    subscriber.onError(new RuntimeException("Failed to read CSV line", e));
                    closeReader();
                    return;
                } catch (Exception e) {
                    subscriber.onError(new RuntimeException("Failed to process CSV line", e));
                    closeReader();
                    return;
                }
            }
            
            // Record backpressure if we stopped due to lack of demand
            if (demand.get() == 0 && !cancelled.get()) {
                metrics.recordBackpressureEvent();
            }
        }
        
        
        /**
         * Reads the next line from the CSV file and tracks metrics.
         * This method represents the controlled file I/O that respects backpressure.
         */
        private String readNextLine() throws IOException {
            String line = reader.readLine();
            if (line != null) {
                metrics.recordLineRead(line.length());
            }
            return line;
        }
        
        /**
         * Parses a CSV line into a DataStreamerItem for the music events table.
         * 
         * CSV format: EventId,UserId,TrackId,EventType,EventTime,Duration,PlaylistId
         */
        private DataStreamerItem<Tuple> parseLineToDataStreamerItem(String csvLine) {
            // Skip empty or malformed lines
            if (csvLine == null || csvLine.trim().isEmpty()) {
                throw new IllegalArgumentException("Empty CSV line encountered");
            }
            
            String[] fields = csvLine.split(",", -1); // -1 to include empty trailing fields
            
            if (fields.length < 6) {
                throw new IllegalArgumentException("Invalid CSV line - expected at least 6 fields, got " + 
                    fields.length + " fields: " + csvLine);
            }
            
            try {
                Tuple eventTuple = Tuple.create()
                    .set("EventId", Long.parseLong(fields[0]))
                    .set("UserId", Integer.parseInt(fields[1]))
                    .set("TrackId", Integer.parseInt(fields[2]))
                    .set("EventType", fields[3])
                    .set("EventTime", Long.parseLong(fields[4]))
                    .set("Duration", Long.parseLong(fields[5]))
                    .set("PlaylistId", fields.length > 6 && !fields[6].isEmpty() ? 
                        Integer.parseInt(fields[6]) : null);
                
                return DataStreamerItem.of(eventTuple);
                
            } catch (NumberFormatException e) {
                throw new RuntimeException("Failed to parse numeric fields in CSV line: " + csvLine, e);
            }
        }
        
        /**
         * Closes the file reader and releases resources.
         */
        private void closeReader() {
            try {
                reader.close();
            } catch (IOException e) {
                // Log but don't propagate errors during cleanup
                System.err.println("Warning: Failed to close CSV reader: " + e.getMessage());
            }
        }
    }
    
    /**
     * Gets the CSV file path being streamed.
     */
    public Path getFilePath() {
        return csvFilePath;
    }
    
    /**
     * Gets the metrics tracker for this publisher.
     */
    public StreamingMetrics getMetrics() {
        return metrics;
    }
    
    /**
     * Checks if this publisher has been subscribed to.
     */
    public boolean isSubscribed() {
        return subscribed.get();
    }
}