# 8. Data Streaming

## Data Streamer Setup

### Configuration Options

```java
DataStreamerOptions options = DataStreamerOptions.builder()
    .pageSize(1000)
    .perPartitionParallelOperations(1)
    .autoFlushInterval(1000)
    .retryLimit(16)
    .build();
```

### Performance Tuning

#### Optimal Configuration Settings

```java
// High-throughput configuration
DataStreamerOptions highThroughputOptions = DataStreamerOptions.builder()
    .pageSize(10000)                    // Larger batches for throughput
    .perPartitionParallelOperations(4)  // Parallel processing per partition
    .autoFlushInterval(5000)            // 5 second flush interval
    .retryLimit(5)                      // Retry failed operations
    .build();

// Low-latency configuration
DataStreamerOptions lowLatencyOptions = DataStreamerOptions.builder()
    .pageSize(100)                      // Smaller batches for low latency
    .perPartitionParallelOperations(1)  // Sequential processing
    .autoFlushInterval(100)             // 100ms flush interval
    .retryLimit(3)
    .build();

// Balanced configuration
DataStreamerOptions balancedOptions = DataStreamerOptions.builder()
    .pageSize(1000)                     // Moderate batch size
    .perPartitionParallelOperations(2)  // Some parallelism
    .autoFlushInterval(1000)            // 1 second flush
    .retryLimit(3)
    .build();
```

#### Adaptive Performance Tuning

```java
public class AdaptiveStreaming {
    
    // Dynamically adjust based on system performance
    public static DataStreamerOptions adaptiveOptions(int recordCount, boolean prioritizeThroughput) {
        DataStreamerOptions.Builder builder = DataStreamerOptions.builder();
        
        if (prioritizeThroughput) {
            // Optimize for throughput
            builder.pageSize(Math.min(recordCount / 10, 10000))
                   .perPartitionParallelOperations(Runtime.getRuntime().availableProcessors())
                   .autoFlushInterval(5000);
        } else {
            // Optimize for latency
            builder.pageSize(Math.min(recordCount / 100, 1000))
                   .perPartitionParallelOperations(1)
                   .autoFlushInterval(500);
        }
        
        return builder.retryLimit(3).build();
    }
    
    // Monitor and adjust streaming performance for Artist data
    public static void monitoredStreaming(IgniteClient client, List<Entry<Tuple, Tuple>> artistData) {
        KeyValueView<Tuple, Tuple> view = client.tables().table("Artist").keyValueView();
        
        long startTime = System.currentTimeMillis();
        
        try (var publisher = new SubmissionPublisher<DataStreamerItem<Entry<Tuple, Tuple>>>()) {
            DataStreamerOptions options = adaptiveOptions(artistData.size(), artistData.size() > 10000);
            
            CompletableFuture<Void> streamerFut = view.streamData(publisher, options);
            
            // Submit artist data with progress monitoring
            int batchSize = 1000;
            for (int i = 0; i < artistData.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, artistData.size());
                
                for (int j = i; j < endIndex; j++) {
                    publisher.submit(DataStreamerItem.of(artistData.get(j)));
                }
                
                // Log progress
                if (i % 10000 == 0) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double rate = (double) i / elapsed * 1000; // artists per second
                    System.out.printf("Streamed %d artists, rate: %.2f artists/sec%n", i, rate);
                }
            }
            
            publisher.close();
            streamerFut.join();
            
            long totalTime = System.currentTimeMillis() - startTime;
            double finalRate = (double) artistData.size() / totalTime * 1000;
            System.out.printf("Artist streaming completed: %d artists in %d ms (%.2f artists/sec)%n", 
                artistData.size(), totalTime, finalRate);
        }
    }
}
```

#### Memory Management for Large Datasets

```java
// Efficient memory usage for large track streaming operations
public static void memoryEfficientTrackStreaming(IgniteClient client, int totalTracks) {
    KeyValueView<Tuple, Tuple> view = client.tables().table("Track").keyValueView();
    
    DataStreamerOptions options = DataStreamerOptions.builder()
        .pageSize(5000)
        .perPartitionParallelOperations(2)
        .autoFlushInterval(2000)
        .build();
    
    try (var publisher = new SubmissionPublisher<DataStreamerItem<Entry<Tuple, Tuple>>>(
            ForkJoinPool.commonPool(), // Use common pool
            1000)) { // Limit buffer size to control memory
        
        CompletableFuture<Void> streamerFut = view.streamData(publisher, options);
        
        // Generate and stream track data without holding everything in memory
        String[] sampleTrackNames = {"Bohemian Rhapsody", "Stairway to Heaven", "Hotel California", 
                                     "Sweet Child O' Mine", "Smoke on the Water", "Imagine"};
        
        for (int i = 0; i < totalTracks; i++) {
            Tuple key = Tuple.create().set("TrackId", i);
            Tuple value = Tuple.create()
                .set("Name", sampleTrackNames[i % sampleTrackNames.length] + " " + i)
                .set("AlbumId", (i / 10) + 1)
                .set("Milliseconds", 180000 + (i * 1000));
            
            // Create track entry and submit immediately
            Entry<Tuple, Tuple> entry = Map.entry(key, value);
            publisher.submit(DataStreamerItem.of(entry));
            
            // Periodically check memory usage and pause if needed
            if (i % 1000 == 0) {
                Runtime runtime = Runtime.getRuntime();
                long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                long maxMemory = runtime.maxMemory();
                
                if (usedMemory > maxMemory * 0.8) {
                    System.out.println("High memory usage detected during track streaming, pausing...");
                    try { Thread.sleep(100); } catch (InterruptedException e) {}
                }
            }
        }
        
        publisher.close();
        streamerFut.join();
    }
}
```

## Streaming Patterns

### High-Throughput Ingestion

```java
// High-throughput streaming of customer data
KeyValueView<Tuple, Tuple> view = client.tables().table("Customer").keyValueView();

try (var publisher = new SubmissionPublisher<DataStreamerItem<Entry<Tuple, Tuple>>>()) {
    CompletableFuture<Void> streamerFut = view.streamData(publisher, options);
    
    // Submit customer data
    for (int i = 1; i <= CUSTOMER_COUNT; i++) {
        Tuple key = Tuple.create().set("CustomerId", i);
        Tuple value = Tuple.create()
            .set("FirstName", "Customer" + i)
            .set("LastName", "LastName" + i)
            .set("Email", "customer" + i + "@example.com")
            .set("Country", i % 3 == 0 ? "USA" : i % 3 == 1 ? "Canada" : "Brazil");
        
        publisher.submit(DataStreamerItem.of(Map.entry(key, value)));
    }
}
```

### Error Handling Strategies

#### Robust Error Handling with Retry

```java
public class StreamingErrorHandling {
    
    // Custom error handler for streaming operations
    public static class StreamingErrorHandler {
        private final AtomicInteger errorCount = new AtomicInteger(0);
        private final AtomicInteger retryCount = new AtomicInteger(0);
        
        public void handleError(DataStreamerItem<?> item, Throwable error) {
            errorCount.incrementAndGet();
            
            System.err.println("Streaming error for item: " + item + ", error: " + error.getMessage());
            
            // Categorize and handle different error types
            if (error instanceof TimeoutException) {
                handleTimeoutError(item, error);
            } else if (error instanceof IgniteClientConnectionException) {
                handleConnectionError(item, error);
            } else {
                handleGeneralError(item, error);
            }
        }
        
        private void handleTimeoutError(DataStreamerItem<?> item, Throwable error) {
            // Retry timeout errors with exponential backoff
            int retries = retryCount.incrementAndGet();
            if (retries < 5) {
                System.out.println("Retrying after timeout, attempt: " + retries);
                // Implement retry logic here
            } else {
                System.err.println("Max retries exceeded for timeout error");
            }
        }
        
        private void handleConnectionError(DataStreamerItem<?> item, Throwable error) {
            System.err.println("Connection error - check cluster status");
            // Could pause streaming and retry connection
        }
        
        private void handleGeneralError(DataStreamerItem<?> item, Throwable error) {
            System.err.println("General error - logging for investigation");
            // Log to file or monitoring system
        }
        
        public int getErrorCount() { return errorCount.get(); }
        public int getRetryCount() { return retryCount.get(); }
    }
    
    // Streaming with error handling for Album data
    public static void robustStreaming(IgniteClient client, List<Entry<Tuple, Tuple>> albumData) {
        KeyValueView<Tuple, Tuple> view = client.tables().table("Album").keyValueView();
        StreamingErrorHandler errorHandler = new StreamingErrorHandler();
        
        DataStreamerOptions options = DataStreamerOptions.builder()
            .pageSize(1000)
            .autoFlushInterval(2000)
            .retryLimit(3)
            .build();
        
        try (var publisher = new SubmissionPublisher<DataStreamerItem<Entry<Tuple, Tuple>>>()) {
            CompletableFuture<Void> streamerFut = view.streamData(publisher, options)
                .exceptionally(throwable -> {
                    System.err.println("Streaming failed: " + throwable.getMessage());
                    errorHandler.handleError(null, throwable);
                    return null;
                });
            
            // Submit album data with individual error handling
            for (Entry<Tuple, Tuple> entry : albumData) {
                try {
                    DataStreamerItem<Entry<Tuple, Tuple>> item = DataStreamerItem.of(entry);
                    publisher.submit(item);
                } catch (Exception e) {
                    errorHandler.handleError(DataStreamerItem.of(entry), e);
                }
            }
            
            publisher.close();
            
            try {
                streamerFut.get(30, TimeUnit.SECONDS);
                System.out.println("Streaming completed successfully");
            } catch (TimeoutException e) {
                System.err.println("Streaming timed out");
                errorHandler.handleError(null, e);
            } catch (Exception e) {
                System.err.println("Streaming failed: " + e.getMessage());
                errorHandler.handleError(null, e);
            }
            
            // Report error statistics
            System.out.printf("Streaming summary - Errors: %d, Retries: %d%n", 
                errorHandler.getErrorCount(), errorHandler.getRetryCount());
        }
    }
}
```

#### Dead Letter Queue Pattern

```java
public class DeadLetterQueueStreaming {
    
    // Failed items storage
    private static final Queue<DataStreamerItem<Entry<Tuple, Tuple>>> deadLetterQueue = 
        new ConcurrentLinkedQueue<>();
    
    // Stream Track data with dead letter queue for failed items
    public static void streamingWithDLQ(IgniteClient client, List<Entry<Tuple, Tuple>> trackData) {
        KeyValueView<Tuple, Tuple> view = client.tables().table("Track").keyValueView();
        
        DataStreamerOptions options = DataStreamerOptions.builder()
            .pageSize(1000)
            .retryLimit(2)
            .build();
        
        try (var publisher = new SubmissionPublisher<DataStreamerItem<Entry<Tuple, Tuple>>>()) {
            CompletableFuture<Void> streamerFut = view.streamData(publisher, options)
                .exceptionally(throwable -> {
                    System.err.println("Streaming operation failed: " + throwable.getMessage());
                    return null;
                });
            
            // Submit track items with error tracking
            for (Entry<Tuple, Tuple> entry : trackData) {
                DataStreamerItem<Entry<Tuple, Tuple>> item = DataStreamerItem.of(entry);
                
                try {
                    publisher.submit(item);
                } catch (Exception e) {
                    // Add failed items to dead letter queue
                    deadLetterQueue.offer(item);
                    System.err.println("Item added to DLQ: " + e.getMessage());
                }
            }
            
            publisher.close();
            streamerFut.join();
            
            // Process dead letter queue
            processDeadLetterQueue(client);
        }
    }
    
    // Reprocess failed items from dead letter queue
    private static void processDeadLetterQueue(IgniteClient client) {
        if (deadLetterQueue.isEmpty()) {
            System.out.println("No failed items to reprocess");
            return;
        }
        
        System.out.println("Reprocessing " + deadLetterQueue.size() + " failed track items");
        
        KeyValueView<Tuple, Tuple> view = client.tables().table("Track").keyValueView();
        
        // Reprocess with more conservative settings
        DataStreamerOptions retryOptions = DataStreamerOptions.builder()
            .pageSize(100)              // Smaller batches
            .autoFlushInterval(500)     // Faster flushing
            .retryLimit(1)              // Fewer retries
            .build();
        
        try (var publisher = new SubmissionPublisher<DataStreamerItem<Entry<Tuple, Tuple>>>()) {
            CompletableFuture<Void> reprocessFut = view.streamData(publisher, retryOptions);
            
            // Resubmit failed items
            DataStreamerItem<Entry<Tuple, Tuple>> item;
            int reprocessed = 0;
            int failed = 0;
            
            while ((item = deadLetterQueue.poll()) != null) {
                try {
                    publisher.submit(item);
                    reprocessed++;
                } catch (Exception e) {
                    failed++;
                    System.err.println("Item failed again: " + e.getMessage());
                }
            }
            
            publisher.close();
            reprocessFut.join();
            
            System.out.printf("Track DLQ processing complete - Reprocessed: %d, Failed: %d%n", 
                reprocessed, failed);
        }
    }
}
```

#### Circuit Breaker for Streaming

```java
public class CircuitBreakerStreaming {
    
    // Simple circuit breaker implementation
    public static class StreamingCircuitBreaker {
        private volatile boolean isOpen = false;
        private volatile long lastFailureTime = 0;
        private volatile int consecutiveFailures = 0;
        
        private final int failureThreshold;
        private final long timeoutMs;
        
        public StreamingCircuitBreaker(int failureThreshold, long timeoutMs) {
            this.failureThreshold = failureThreshold;
            this.timeoutMs = timeoutMs;
        }
        
        public boolean allowRequest() {
            if (!isOpen) {
                return true;
            }
            
            // Check if timeout has elapsed
            if (System.currentTimeMillis() - lastFailureTime > timeoutMs) {
                isOpen = false;
                consecutiveFailures = 0;
                return true;
            }
            
            return false;
        }
        
        public void recordSuccess() {
            consecutiveFailures = 0;
            isOpen = false;
        }
        
        public void recordFailure() {
            consecutiveFailures++;
            lastFailureTime = System.currentTimeMillis();
            
            if (consecutiveFailures >= failureThreshold) {
                isOpen = true;
                System.err.println("Circuit breaker opened after " + consecutiveFailures + " failures");
            }
        }
        
        public boolean isOpen() { return isOpen; }
    }
    
    // Streaming with circuit breaker protection for Playlist data
    public static void protectedPlaylistStreaming(IgniteClient client, List<Entry<Tuple, Tuple>> playlistData) {
        KeyValueView<Tuple, Tuple> view = client.tables().table("Playlist").keyValueView();
        StreamingCircuitBreaker circuitBreaker = new StreamingCircuitBreaker(5, 30000); // 5 failures, 30s timeout
        
        DataStreamerOptions options = DataStreamerOptions.builder()
            .pageSize(1000)
            .retryLimit(2)
            .build();
        
        int processed = 0;
        int skipped = 0;
        
        for (int i = 0; i < playlistData.size(); i += 1000) {
            if (!circuitBreaker.allowRequest()) {
                skipped += Math.min(1000, playlistData.size() - i);
                System.out.println("Circuit breaker open - skipping playlist batch");
                continue;
            }
            
            try (var publisher = new SubmissionPublisher<DataStreamerItem<Entry<Tuple, Tuple>>>()) {
                CompletableFuture<Void> streamerFut = view.streamData(publisher, options);
                
                // Process playlist batch
                int endIndex = Math.min(i + 1000, playlistData.size());
                for (int j = i; j < endIndex; j++) {
                    publisher.submit(DataStreamerItem.of(playlistData.get(j)));
                }
                
                publisher.close();
                
                // Wait for completion with timeout
                streamerFut.get(10, TimeUnit.SECONDS);
                
                circuitBreaker.recordSuccess();
                processed += (endIndex - i);
                
            } catch (Exception e) {
                System.err.println("Batch failed: " + e.getMessage());
                circuitBreaker.recordFailure();
            }
        }
        
        System.out.printf("Playlist streaming completed - Processed: %d, Skipped: %d%n", processed, skipped);
    }
}
```

### Backpressure Management

#### Understanding Backpressure in Streaming

Backpressure occurs when the data producer generates data faster than the consumer can process it. Ignite 3's streaming API handles this through several mechanisms:

```java
public class BackpressureManagement {
    
    // Monitor and handle backpressure for Invoice data
    public static void adaptiveBackpressureStreaming(IgniteClient client, int totalInvoices) {
        KeyValueView<Tuple, Tuple> view = client.tables().table("Invoice").keyValueView();
        
        // Start with moderate settings
        DataStreamerOptions initialOptions = DataStreamerOptions.builder()
            .pageSize(1000)
            .perPartitionParallelOperations(2)
            .autoFlushInterval(1000)
            .build();
        
        // Use SubmissionPublisher with capacity control
        try (var publisher = new SubmissionPublisher<DataStreamerItem<Entry<Tuple, Tuple>>>(
                ForkJoinPool.commonPool(),
                Flow.defaultBufferSize())) { // Control buffer size
            
            CompletableFuture<Void> streamerFut = view.streamData(publisher, initialOptions);
            
            AtomicLong backpressureEvents = new AtomicLong(0);
            
            // Monitor subscription for backpressure
            publisher.consume(subscriber -> {
                subscriber.onSubscribe(new Flow.Subscription() {
                    private volatile boolean cancelled = false;
                    
                    @Override
                    public void request(long n) {
                        // Monitoring demand signals
                        if (n < Flow.defaultBufferSize() / 2) {
                            backpressureEvents.incrementAndGet();
                            System.out.println("Backpressure detected - demand: " + n);
                        }
                    }
                    
                    @Override
                    public void cancel() {
                        cancelled = true;
                    }
                });
            });
            
            // Submit invoice data with backpressure awareness
            for (int i = 1; i <= totalInvoices; i++) {
                Tuple key = Tuple.create().set("InvoiceId", i);
                Tuple value = Tuple.create()
                    .set("CustomerId", (i % 100) + 1)
                    .set("InvoiceDate", "2023-01-" + String.format("%02d", (i % 28) + 1))
                    .set("Total", 9.99 + (i * 0.50));
                Entry<Tuple, Tuple> entry = Map.entry(key, value);
                
                try {
                    // Check estimated lag (queue size)
                    int lag = publisher.estimateMaximumLag();
                    
                    if (lag > Flow.defaultBufferSize() * 0.8) {
                        // High backpressure - slow down
                        System.out.println("High backpressure (lag: " + lag + ") - slowing down");
                        Thread.sleep(10);
                    }
                    
                    publisher.submit(DataStreamerItem.of(entry));
                    
                } catch (IllegalStateException e) {
                    // Publisher rejected submission due to backpressure
                    System.err.println("Submission rejected - backpressure: " + e.getMessage());
                    
                    // Wait and retry
                    try {
                        Thread.sleep(100);
                        publisher.submit(DataStreamerItem.of(entry));
                    } catch (Exception retryException) {
                        System.err.println("Retry failed: " + retryException.getMessage());
                    }
                }
                
                // Periodic progress report
                if (i % 10000 == 0) {
                    System.out.printf("Invoice Progress: %d/%d, Backpressure events: %d, Lag: %d%n", 
                        i, totalInvoices, backpressureEvents.get(), publisher.estimateMaximumLag());
                }
            }
            
            publisher.close();
            streamerFut.join();
            
            System.out.println("Invoice streaming completed. Total backpressure events: " + backpressureEvents.get());
        }
    }
}
```

#### Flow Control Strategies

```java
public class FlowControlStrategies {
    
    // Strategy 1: Rate limiting for Artist data
    public static void rateLimitedArtistStreaming(IgniteClient client, List<Entry<Tuple, Tuple>> artistData) {
        KeyValueView<Tuple, Tuple> view = client.tables().table("Artist").keyValueView();
        
        // Create rate limiter (e.g., Google Guava RateLimiter could be used here)
        SimpleRateLimiter rateLimiter = new SimpleRateLimiter(1000); // 1000 artists per second
        
        DataStreamerOptions options = DataStreamerOptions.builder()
            .pageSize(500)
            .autoFlushInterval(1000)
            .build();
        
        try (var publisher = new SubmissionPublisher<DataStreamerItem<Entry<Tuple, Tuple>>>()) {
            CompletableFuture<Void> streamerFut = view.streamData(publisher, options);
            
            for (Entry<Tuple, Tuple> entry : artistData) {
                // Wait for rate limiter permission
                rateLimiter.acquire();
                
                publisher.submit(DataStreamerItem.of(entry));
            }
            
            publisher.close();
            streamerFut.join();
        }
    }
    
    // Strategy 2: Adaptive batch sizing for Album data
    public static void adaptiveBatchAlbumStreaming(IgniteClient client, List<Entry<Tuple, Tuple>> albumData) {
        KeyValueView<Tuple, Tuple> view = client.tables().table("Album").keyValueView();
        
        int currentBatchSize = 1000;
        int minBatchSize = 100;
        int maxBatchSize = 5000;
        
        for (int i = 0; i < albumData.size(); i += currentBatchSize) {
            long batchStart = System.currentTimeMillis();
            
            DataStreamerOptions options = DataStreamerOptions.builder()
                .pageSize(currentBatchSize)
                .autoFlushInterval(500)
                .build();
            
            try (var publisher = new SubmissionPublisher<DataStreamerItem<Entry<Tuple, Tuple>>>()) {
                CompletableFuture<Void> streamerFut = view.streamData(publisher, options);
                
                // Submit current album batch
                int endIndex = Math.min(i + currentBatchSize, albumData.size());
                for (int j = i; j < endIndex; j++) {
                    publisher.submit(DataStreamerItem.of(albumData.get(j)));
                }
                
                publisher.close();
                streamerFut.join();
                
                long batchDuration = System.currentTimeMillis() - batchStart;
                
                // Adapt batch size based on performance
                if (batchDuration < 1000) {
                    // Fast processing - increase batch size
                    currentBatchSize = Math.min(currentBatchSize * 2, maxBatchSize);
                } else if (batchDuration > 5000) {
                    // Slow processing - decrease batch size
                    currentBatchSize = Math.max(currentBatchSize / 2, minBatchSize);
                }
                
                System.out.printf("Album batch %d-%d processed in %d ms, next batch size: %d%n", 
                    i, endIndex - 1, batchDuration, currentBatchSize);
                
            } catch (Exception e) {
                System.err.println("Album batch failed: " + e.getMessage());
                // Reduce batch size on failure
                currentBatchSize = Math.max(currentBatchSize / 2, minBatchSize);
            }
        }
    }
    
    // Strategy 3: Producer-consumer pattern with bounded queue for Invoice data
    public static void producerConsumerStreaming(IgniteClient client, List<Entry<Tuple, Tuple>> invoiceData) {
        KeyValueView<Tuple, Tuple> view = client.tables().table("Invoice").keyValueView();
        
        // Bounded queue to control memory usage
        BlockingQueue<Entry<Tuple, Tuple>> queue = new ArrayBlockingQueue<>(10000);
        
        // Producer thread
        Thread producer = new Thread(() -> {
            try {
                for (Entry<Tuple, Tuple> entry : invoiceData) {
                    queue.put(entry); // Blocks when queue is full
                }
                // Signal end of data
                queue.put(createPoisonPill());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Consumer thread
        Thread consumer = new Thread(() -> {
            DataStreamerOptions options = DataStreamerOptions.builder()
                .pageSize(1000)
                .autoFlushInterval(1000)
                .build();
            
            try (var publisher = new SubmissionPublisher<DataStreamerItem<Entry<Tuple, Tuple>>>()) {
                CompletableFuture<Void> streamerFut = view.streamData(publisher, options);
                
                Entry<Tuple, Tuple> entry;
                while ((entry = queue.take()) != isPoisonPill(entry)) {
                    publisher.submit(DataStreamerItem.of(entry));
                }
                
                publisher.close();
                streamerFut.join();
                
            } catch (Exception e) {
                System.err.println("Consumer failed: " + e.getMessage());
            }
        });
        
        producer.start();
        consumer.start();
        
        try {
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // Helper methods
    private static Entry<Tuple, Tuple> createPoisonPill() {
        return Map.entry(
            Tuple.create().set("poison", true),
            Tuple.create().set("pill", true)
        );
    }
    
    private static boolean isPoisonPill(Entry<Tuple, Tuple> entry) {
        return entry.getKey().booleanValue("poison") != null;
    }
    
    // Simple rate limiter implementation
    private static class SimpleRateLimiter {
        private final long intervalNanos;
        private volatile long nextAllowedTime = 0;
        
        public SimpleRateLimiter(double permitsPerSecond) {
            this.intervalNanos = (long) (1_000_000_000.0 / permitsPerSecond);
        }
        
        public void acquire() {
            long now = System.nanoTime();
            if (now < nextAllowedTime) {
                try {
                    Thread.sleep((nextAllowedTime - now) / 1_000_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            nextAllowedTime = Math.max(nextAllowedTime + intervalNanos, now + intervalNanos);
        }
    }
}
```

#### Best Practices for Backpressure Management

1. **Monitor Queue Sizes**: Keep track of internal buffer sizes
2. **Adaptive Sizing**: Adjust batch sizes based on performance
3. **Rate Limiting**: Control data ingestion rate
4. **Circuit Breakers**: Fail fast when system is overloaded
5. **Bounded Queues**: Prevent memory exhaustion
6. **Graceful Degradation**: Slow down rather than fail

```java
// Complete example combining all backpressure strategies for streaming media data
public static void backpressureHandling(IgniteClient client, 
                                                   Iterator<Entry<Tuple, Tuple>> mediaDataIterator) {
    KeyValueView<Tuple, Tuple> view = client.tables().table("MediaType").keyValueView();
    
    SimpleRateLimiter rateLimiter = new SimpleRateLimiter(5000); // 5000 records/sec
    int adaptiveBatchSize = 1000;
    
    while (mediaDataIterator.hasNext()) {
        List<Entry<Tuple, Tuple>> batch = new ArrayList<>();
        
        // Collect media data batch
        for (int i = 0; i < adaptiveBatchSize && mediaDataIterator.hasNext(); i++) {
            batch.add(mediaDataIterator.next());
        }
        
        long batchStart = System.currentTimeMillis();
        
        try {
            // Process batch with rate limiting
            processBatchWithBackpressure(view, batch, rateLimiter);
            
            long duration = System.currentTimeMillis() - batchStart;
            
            // Adapt batch size based on performance
            if (duration < 500) {
                adaptiveBatchSize = Math.min(adaptiveBatchSize + 100, 5000);
            } else if (duration > 2000) {
                adaptiveBatchSize = Math.max(adaptiveBatchSize - 100, 100);
            }
            
        } catch (Exception e) {
            System.err.println("Batch processing failed: " + e.getMessage());
            adaptiveBatchSize = Math.max(adaptiveBatchSize / 2, 100);
        }
    }
}

private static void processBatchWithBackpressure(KeyValueView<Tuple, Tuple> view, 
                                               List<Entry<Tuple, Tuple>> batch,
                                               SimpleRateLimiter rateLimiter) {
    DataStreamerOptions options = DataStreamerOptions.builder()
        .pageSize(batch.size())
        .autoFlushInterval(1000)
        .build();
    
    try (var publisher = new SubmissionPublisher<DataStreamerItem<Entry<Tuple, Tuple>>>(
            ForkJoinPool.commonPool(), Math.min(batch.size() * 2, 1000))) {
        
        CompletableFuture<Void> streamerFut = view.streamData(publisher, options);
        
        for (Entry<Tuple, Tuple> entry : batch) {
            rateLimiter.acquire();
            
            // Check for backpressure
            if (publisher.estimateMaximumLag() > 500) {
                Thread.sleep(10); // Brief pause
            }
            
            publisher.submit(DataStreamerItem.of(entry));
        }
        
        publisher.close();
        streamerFut.join();
    }
}
```
