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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;

/**
 * Demonstrates backpressure handling and flow control in Ignite 3's DataStreamer API.
 * 
 * This example shows how to implement adaptive flow control when data producers
 * generate data faster than consumers can process it. The streaming API uses
 * Java Flow API for natural backpressure handling, but applications must still
 * manage producer rates and buffer sizes appropriately.
 * 
 * Key concepts demonstrated:
 * - Custom Flow.Publisher with backpressure support
 * - Adaptive rate limiting based on system feedback
 * - Buffer management and overflow handling
 * - Monitoring streaming performance metrics
 * - Producer-consumer coordination patterns
 * 
 * Business scenario:
 * During peak hours (new album releases, viral tracks), a music streaming service
 * receives millions of events per minute. The system must adapt streaming rates
 * to prevent overwhelming the database while maintaining near real-time processing.
 * When the cluster is under load, the system should slow producers rather than
 * dropping events or causing out-of-memory errors.
 */
public class BackpressureHandling {
    
    /**
     * Main demonstration method for backpressure and flow control patterns.
     */
    public static void main(String[] args) {
        try (IgniteClient ignite = IgniteClient.builder()
                .addresses("localhost:10800")
                .build()) {
            
            System.out.println("=== Backpressure Handling Demo ===");
            
            // Demonstrate custom publisher with backpressure
            demonstrateCustomPublisher(ignite);
            
            // Demonstrate adaptive rate limiting
            demonstrateAdaptiveRateLimiting(ignite);
            
            // Demonstrate buffer overflow handling
            demonstrateBufferOverflowHandling(ignite);
            
            System.out.println("Backpressure handling demonstration completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Backpressure demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Demonstrates a custom Flow.Publisher that implements proper backpressure handling.
     * Shows how to coordinate between data production and consumption rates.
     */
    private static void demonstrateCustomPublisher(IgniteClient ignite) {
        System.out.println("\n--- Custom Publisher with Backpressure");
        
        RecordView<Tuple> backpressureView = ignite.tables()
            .table("BackpressureTest")
            .recordView();
        
        // Generate large dataset for backpressure testing
        List<MusicEvent> events = generateMusicEvents(100000);
        System.out.println("Generated " + events.size() + " events for backpressure testing");
        
        // Create custom publisher with backpressure support
        AdaptiveMusicEventPublisher publisher = new AdaptiveMusicEventPublisher(events);
        
        DataStreamerOptions backpressureOptions = DataStreamerOptions.builder()
            .pageSize(1000)                     // Moderate batch size
            .perPartitionParallelOperations(2)  // Limited parallelism
            .autoFlushInterval(500)             // Fast flushing
            .retryLimit(8)                      // Quick failure detection
            .build();
        
        try {
            System.out.println("Starting streaming with custom backpressure-aware publisher...");
            
            long startTime = System.currentTimeMillis();
            
            CompletableFuture<Void> streamingFuture = backpressureView
                .streamData(publisher, backpressureOptions);
            
            // Monitor progress while streaming
            CompletableFuture.runAsync(() -> {
                while (!streamingFuture.isDone()) {
                    try {
                        Thread.sleep(2000);
                        System.out.printf("  Published: %d, Requested: %d, Rate: %.2f events/sec%n",
                            publisher.getPublishedCount(),
                            publisher.getRequestedCount(),
                            publisher.getCurrentRate());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            
            streamingFuture.get();
            
            long duration = System.currentTimeMillis() - startTime;
            double avgThroughput = (double) events.size() / duration * 1000;
            
            System.out.printf("    <<< Completed streaming with backpressure in %d ms (%.2f events/sec avg)%n", 
                duration, avgThroughput);
            System.out.printf("  Final stats - Published: %d, Max rate: %.2f events/sec%n",
                publisher.getPublishedCount(), publisher.getMaxRate());
            
        } catch (Exception e) {
            System.err.println("Custom publisher demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates adaptive rate limiting that responds to system performance.
     * Shows how to slow down producers when the system is under load.
     */
    private static void demonstrateAdaptiveRateLimiting(IgniteClient ignite) {
        System.out.println("\n--- Adaptive Rate Limiting");
        
        RecordView<Tuple> rateLimitView = ignite.tables()
            .table("RateLimitTest")
            .recordView();
        
        DataStreamerOptions adaptiveOptions = DataStreamerOptions.builder()
            .pageSize(2000)                     // Larger batches for efficiency
            .perPartitionParallelOperations(3)  // Higher parallelism
            .autoFlushInterval(300)             // Fast flushing
            .retryLimit(16)                     // Higher retry tolerance
            .build();
        
        try {
            try (SubmissionPublisher<DataStreamerItem<Tuple>> publisher = 
                    new SubmissionPublisher<>()) {
                
                System.out.println("Starting adaptive rate limiting demonstration...");
                
                CompletableFuture<Void> streamingFuture = rateLimitView
                    .streamData(publisher, adaptiveOptions);
                
                // Simulate variable load with adaptive rate control
                AdaptiveRateController rateController = new AdaptiveRateController();
                
                CompletableFuture.runAsync(() -> {
                    try {
                        for (int i = 1; i <= 50000; i++) {
                            Tuple event = Tuple.create()
                                .set("EventId", i)
                                .set("UserId", 6000 + (i % 300))
                                .set("TrackId", 1 + (i % 150))
                                .set("EventType", "ADAPTIVE_TEST")
                                .set("Timestamp", System.currentTimeMillis())
                                .set("LoadPhase", rateController.getCurrentPhase());
                            
                            publisher.submit(DataStreamerItem.of(event));
                            
                            // Apply adaptive rate limiting
                            long delay = rateController.getAdaptiveDelay(i);
                            if (delay > 0) {
                                Thread.sleep(delay);
                            }
                            
                            // Progress and rate monitoring
                            if (i % 5000 == 0) {
                                System.out.printf("  Submitted %d events, Current rate: %.2f events/sec, Phase: %s%n",
                                    i, rateController.getCurrentRate(), rateController.getCurrentPhase());
                            }
                        }
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        publisher.close();
                    }
                });
                
                streamingFuture.get();
                
                System.out.println("    <<< Adaptive rate limiting completed");
                System.out.printf("  Peak rate: %.2f events/sec, Min rate: %.2f events/sec%n",
                    rateController.getPeakRate(), rateController.getMinRate());
                
            }
            
        } catch (Exception e) {
            System.err.println("Adaptive rate limiting failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates handling buffer overflow situations gracefully.
     * Shows strategies for dealing with memory pressure during high-volume streaming.
     */
    private static void demonstrateBufferOverflowHandling(IgniteClient ignite) {
        System.out.println("\n--- Buffer Overflow Handling");
        
        RecordView<Tuple> overflowView = ignite.tables()
            .table("OverflowTest")
            .recordView();
        
        // Create publisher with limited buffer to force overflow scenarios
        BufferOverflowPublisher overflowPublisher = new BufferOverflowPublisher(10000); // Small buffer
        
        DataStreamerOptions overflowOptions = DataStreamerOptions.builder()
            .pageSize(500)                      // Small batches to increase pressure
            .perPartitionParallelOperations(1)  // Single-threaded processing
            .autoFlushInterval(2000)            // Slow flushing to create backpressure
            .retryLimit(8)                      // Standard retry limit
            .build();
        
        try {
            System.out.println("Testing buffer overflow handling with limited buffer size...");
            
            CompletableFuture<Void> streamingFuture = overflowView
                .streamData(overflowPublisher, overflowOptions);
            
            // Monitor overflow statistics
            CompletableFuture.runAsync(() -> {
                while (!streamingFuture.isDone()) {
                    try {
                        Thread.sleep(3000);
                        System.out.printf("  Buffer status - Size: %d, Overflows: %d, Dropped: %d%n",
                            overflowPublisher.getBufferSize(),
                            overflowPublisher.getOverflowCount(),
                            overflowPublisher.getDroppedCount());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            
            streamingFuture.get();
            
            System.out.println("    <<< Buffer overflow handling completed");
            System.out.printf("  Final stats - Processed: %d, Dropped: %d, Overflow events: %d%n",
                overflowPublisher.getProcessedCount(),
                overflowPublisher.getDroppedCount(),
                overflowPublisher.getOverflowCount());
            
        } catch (Exception e) {
            System.err.println("Buffer overflow demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Custom Flow.Publisher that implements proper backpressure handling
     * for music event streaming with adaptive rate control.
     */
    private static class AdaptiveMusicEventPublisher implements Flow.Publisher<DataStreamerItem<Tuple>> {
        private final List<MusicEvent> events;
        private final AtomicLong publishedCount = new AtomicLong(0);
        private final AtomicLong requestedCount = new AtomicLong(0);
        private volatile boolean subscribed = false;
        private volatile double currentRate = 0.0;
        private volatile double maxRate = 0.0;
        private long lastRateUpdate = System.currentTimeMillis();
        
        public AdaptiveMusicEventPublisher(List<MusicEvent> events) {
            this.events = events;
        }
        
        @Override
        public void subscribe(Flow.Subscriber<? super DataStreamerItem<Tuple>> subscriber) {
            if (subscribed) {
                subscriber.onError(new IllegalStateException("Publisher already subscribed"));
                return;
            }
            subscribed = true;
            subscriber.onSubscribe(new BackpressureSubscription(subscriber));
        }
        
        public long getPublishedCount() { return publishedCount.get(); }
        public long getRequestedCount() { return requestedCount.get(); }
        public double getCurrentRate() { return currentRate; }
        public double getMaxRate() { return maxRate; }
        
        private class BackpressureSubscription implements Flow.Subscription {
            private final Flow.Subscriber<? super DataStreamerItem<Tuple>> subscriber;
            private final AtomicLong demand = new AtomicLong(0);
            private final AtomicInteger currentIndex = new AtomicInteger(0);
            private volatile boolean cancelled = false;
            
            public BackpressureSubscription(Flow.Subscriber<? super DataStreamerItem<Tuple>> subscriber) {
                this.subscriber = subscriber;
            }
            
            @Override
            public void request(long n) {
                if (n <= 0) {
                    subscriber.onError(new IllegalArgumentException("Request must be positive"));
                    return;
                }
                
                requestedCount.addAndGet(n);
                long newDemand = demand.addAndGet(n);
                deliverItems(newDemand);
            }
            
            @Override
            public void cancel() {
                cancelled = true;
            }
            
            private void deliverItems(long requestedItems) {
                CompletableFuture.runAsync(() -> {
                    long delivered = 0;
                    long startTime = System.currentTimeMillis();
                    
                    while (delivered < requestedItems && !cancelled) {
                        int index = currentIndex.getAndIncrement();
                        
                        if (index >= events.size()) {
                            subscriber.onComplete();
                            return;
                        }
                        
                        MusicEvent event = events.get(index);
                        Tuple eventTuple = Tuple.create()
                            .set("EventId", event.getId())
                            .set("UserId", event.getUserId())
                            .set("TrackId", event.getTrackId())
                            .set("EventType", event.getEventType())
                            .set("Timestamp", event.getTimestamp())
                            .set("BackpressureTest", true);
                        
                        try {
                            subscriber.onNext(DataStreamerItem.of(eventTuple));
                            publishedCount.incrementAndGet();
                            delivered++;
                            
                            // Adaptive rate limiting based on backpressure
                            if (delivered % 100 == 0) {
                                // Calculate current rate and apply adaptive delay
                                long elapsed = System.currentTimeMillis() - startTime;
                                if (elapsed > 0) {
                                    currentRate = (double) delivered / elapsed * 1000;
                                    maxRate = Math.max(maxRate, currentRate);
                                }
                                
                                // Apply backpressure-based delay
                                long delay = calculateAdaptiveDelay();
                                if (delay > 0) {
                                    Thread.sleep(delay);
                                }
                            }
                            
                        } catch (Exception e) {
                            subscriber.onError(e);
                            return;
                        }
                    }
                    
                    demand.addAndGet(-delivered);
                });
            }
            
            private long calculateAdaptiveDelay() {
                // Adaptive delay based on current rate and system feedback
                if (currentRate > 10000) {
                    return 10; // High rate, add significant delay
                } else if (currentRate > 5000) {
                    return 5;  // Medium rate, add moderate delay
                } else if (currentRate > 1000) {
                    return 1;  // Normal rate, minimal delay
                }
                return 0;      // Low rate, no delay needed
            }
        }
    }
    
    /**
     * Rate controller that adapts streaming speed based on simulated system load.
     */
    private static class AdaptiveRateController {
        private volatile double currentRate = 0.0;
        private volatile double peakRate = 0.0;
        private volatile double minRate = Double.MAX_VALUE;
        private volatile String currentPhase = "WARMUP";
        private long phaseStartTime = System.currentTimeMillis();
        private int eventCount = 0;
        
        public long getAdaptiveDelay(int eventNumber) {
            eventCount++;
            long elapsed = System.currentTimeMillis() - phaseStartTime;
            
            // Update phase based on event progress
            updatePhase(eventNumber);
            
            // Calculate current rate
            if (elapsed > 0) {
                currentRate = (double) eventCount / elapsed * 1000;
                peakRate = Math.max(peakRate, currentRate);
                minRate = Math.min(minRate, currentRate);
            }
            
            // Return adaptive delay based on current phase
            switch (currentPhase) {
                case "WARMUP":
                    return 10; // Slow start
                case "NORMAL":
                    return 2;  // Normal processing
                case "PEAK_LOAD":
                    return 20; // High load, slow down significantly
                case "RECOVERY":
                    return 5;  // Moderate recovery speed
                default:
                    return 1;
            }
        }
        
        private void updatePhase(int eventNumber) {
            if (eventNumber < 5000) {
                setPhase("WARMUP");
            } else if (eventNumber < 20000) {
                setPhase("NORMAL");
            } else if (eventNumber < 35000) {
                setPhase("PEAK_LOAD");
            } else if (eventNumber < 45000) {
                setPhase("RECOVERY");
            } else {
                setPhase("NORMAL");
            }
        }
        
        private void setPhase(String newPhase) {
            if (!newPhase.equals(currentPhase)) {
                currentPhase = newPhase;
                phaseStartTime = System.currentTimeMillis();
                eventCount = 0;
            }
        }
        
        public double getCurrentRate() { return currentRate; }
        public double getPeakRate() { return peakRate; }
        public double getMinRate() { return minRate == Double.MAX_VALUE ? 0.0 : minRate; }
        public String getCurrentPhase() { return currentPhase; }
    }
    
    /**
     * Publisher that simulates buffer overflow scenarios and handles them gracefully.
     */
    private static class BufferOverflowPublisher implements Flow.Publisher<DataStreamerItem<Tuple>> {
        private final BlockingQueue<DataStreamerItem<Tuple>> buffer;
        private final AtomicLong processedCount = new AtomicLong(0);
        private final AtomicLong droppedCount = new AtomicLong(0);
        private final AtomicLong overflowCount = new AtomicLong(0);
        private final int maxBufferSize;
        private volatile boolean producing = true;
        
        public BufferOverflowPublisher(int bufferSize) {
            this.maxBufferSize = bufferSize;
            this.buffer = new LinkedBlockingQueue<>(bufferSize);
            startProducing();
        }
        
        @Override
        public void subscribe(Flow.Subscriber<? super DataStreamerItem<Tuple>> subscriber) {
            subscriber.onSubscribe(new OverflowSubscription(subscriber));
        }
        
        private void startProducing() {
            CompletableFuture.runAsync(() -> {
                int eventId = 1;
                while (producing && eventId <= 30000) {
                    Tuple event = Tuple.create()
                        .set("EventId", eventId++)
                        .set("UserId", 7000 + (eventId % 100))
                        .set("TrackId", 1 + (eventId % 50))
                        .set("EventType", "OVERFLOW_TEST")
                        .set("Timestamp", System.currentTimeMillis())
                        .set("BufferTest", true);
                    
                    DataStreamerItem<Tuple> item = DataStreamerItem.of(event);
                    
                    if (!buffer.offer(item)) {
                        // Buffer is full, handle overflow
                        overflowCount.incrementAndGet();
                        
                        // Try to make space by removing oldest item
                        if (buffer.poll() != null) {
                            droppedCount.incrementAndGet();
                        }
                        
                        // Try to add the new item again
                        if (!buffer.offer(item)) {
                            droppedCount.incrementAndGet();
                        }
                    }
                    
                    // Produce at high rate to create pressure
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                producing = false;
            });
        }
        
        public int getBufferSize() { return buffer.size(); }
        public long getProcessedCount() { return processedCount.get(); }
        public long getDroppedCount() { return droppedCount.get(); }
        public long getOverflowCount() { return overflowCount.get(); }
        
        private class OverflowSubscription implements Flow.Subscription {
            private final Flow.Subscriber<? super DataStreamerItem<Tuple>> subscriber;
            private volatile boolean cancelled = false;
            
            public OverflowSubscription(Flow.Subscriber<? super DataStreamerItem<Tuple>> subscriber) {
                this.subscriber = subscriber;
                startDelivery();
            }
            
            @Override
            public void request(long n) {
                // Flow control is handled by buffer capacity
            }
            
            @Override
            public void cancel() {
                cancelled = true;
                producing = false;
            }
            
            private void startDelivery() {
                CompletableFuture.runAsync(() -> {
                    while (!cancelled && (producing || !buffer.isEmpty())) {
                        try {
                            DataStreamerItem<Tuple> item = buffer.poll(100, TimeUnit.MILLISECONDS);
                            if (item != null) {
                                subscriber.onNext(item);
                                processedCount.incrementAndGet();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    subscriber.onComplete();
                });
            }
        }
    }
    
    /**
     * Simple music event data structure for testing.
     */
    private static class MusicEvent {
        private final long id;
        private final int userId;
        private final int trackId;
        private final String eventType;
        private final long timestamp;
        
        public MusicEvent(long id, int userId, int trackId, String eventType, long timestamp) {
            this.id = id;
            this.userId = userId;
            this.trackId = trackId;
            this.eventType = eventType;
            this.timestamp = timestamp;
        }
        
        public long getId() { return id; }
        public int getUserId() { return userId; }
        public int getTrackId() { return trackId; }
        public String getEventType() { return eventType; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Generates a list of sample music events for testing.
     */
    private static List<MusicEvent> generateMusicEvents(int count) {
        List<MusicEvent> events = new ArrayList<>();
        String[] eventTypes = {"PLAY_START", "PLAY_END", "PAUSE", "SKIP", "LIKE", "SHARE"};
        
        for (int i = 1; i <= count; i++) {
            events.add(new MusicEvent(
                i,
                5000 + (i % 500),                           // 500 different users
                1 + (i % 200),                              // 200 different tracks
                eventTypes[i % eventTypes.length],          // Cycle through event types
                System.currentTimeMillis() + i * 1000       // Spread timestamps
            ));
        }
        
        return events;
    }
}