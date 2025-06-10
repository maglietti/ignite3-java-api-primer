# 11. Advanced Topics

This section covers enterprise-grade patterns and advanced techniques for production Apache Ignite 3 deployments.

## Error Handling

### Exception Hierarchy Understanding

Ignite 3 provides an exception hierarchy for precise error handling:

```java
public class IgniteExceptionHandling {
    
    // Exception handling patterns
    public static void demonstrateExceptionHandling(IgniteClient client) {
        try {
            // Operation that might fail
            performRiskyOperation(client);
            
        } catch (IgniteClientConnectionException e) {
            // Connection-related issues
            System.err.println("Connection failed: " + e.getMessage());
            handleConnectionFailure(e);
            
        } catch (TableNotFoundException e) {
            // Table doesn't exist
            System.err.println("Table not found: " + e.getMessage());
            handleMissingTable(e);
            
        } catch (TransactionException e) {
            // Transaction-related errors
            System.err.println("Transaction failed: " + e.getMessage());
            handleTransactionFailure(e);
            
        } catch (IgniteException e) {
            // General Ignite exceptions
            System.err.println("Ignite operation failed: " + e.getMessage());
            handleGeneralIgniteFailure(e);
            
        } catch (Exception e) {
            // Unexpected errors
            System.err.println("Unexpected error: " + e.getMessage());
            handleUnexpectedFailure(e);
        }
    }
    
    // Specific error handling strategies
    private static void handleConnectionFailure(IgniteClientConnectionException e) {
        // Implement reconnection logic
        System.out.println("Attempting to reconnect...");
        // Could trigger circuit breaker or failover
    }
    
    private static void handleMissingTable(TableNotFoundException e) {
        // Could auto-create table or fail gracefully
        System.out.println("Consider creating missing table: " + e.getMessage());
    }
    
    private static void handleTransactionFailure(TransactionException e) {
        // Implement transaction retry logic
        System.out.println("Transaction will be retried: " + e.getMessage());
    }
    
    private static void handleGeneralIgniteFailure(IgniteException e) {
        // General Ignite error handling
        System.out.println("Logging Ignite error for investigation: " + e.getMessage());
    }
    
    private static void handleUnexpectedFailure(Exception e) {
        // Unexpected error handling
        System.out.println("Critical error - alerting operations team: " + e.getMessage());
    }
    
    private static void performRiskyOperation(IgniteClient client) {
        // Example operation that might fail - trying to access non-existent Artist
        var artistTable = client.tables().table("Artist");
        Artist key = new Artist();
        key.setArtistId(99999); // Non-existent artist ID
        artistTable.recordView(Artist.class).get(null, key);
    }
}
```

### Retry Strategies

#### Exponential Backoff Retry

```java
public class RetryStrategies {
    
    // Exponential backoff retry pattern
    public static <T> T executeWithRetry(Supplier<T> operation, 
                                       Class<? extends Exception> retryableException,
                                       int maxRetries) {
        int attempt = 0;
        long delay = 100; // Start with 100ms
        
        while (attempt < maxRetries) {
            try {
                return operation.get();
                
            } catch (Exception e) {
                attempt++;
                
                if (!retryableException.isInstance(e) || attempt >= maxRetries) {
                    throw new RuntimeException("Operation failed after " + attempt + " attempts", e);
                }
                
                System.out.println("Attempt " + attempt + " failed, retrying in " + delay + "ms: " + e.getMessage());
                
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
                
                delay = Math.min(delay * 2, 5000); // Cap at 5 seconds
            }
        }
        
        throw new RuntimeException("Operation failed after " + maxRetries + " attempts");
    }
    
    // Async retry with CompletableFuture
    public static <T> CompletableFuture<T> executeWithRetryAsync(Supplier<CompletableFuture<T>> operation,
                                                               int maxRetries) {
        return executeWithRetryAsync(operation, maxRetries, 100, 1);
    }
    
    private static <T> CompletableFuture<T> executeWithRetryAsync(Supplier<CompletableFuture<T>> operation,
                                                                int remainingRetries,
                                                                long delay,
                                                                int attempt) {
        return operation.get()
            .exceptionallyCompose(throwable -> {
                if (remainingRetries <= 0) {
                    return CompletableFuture.failedFuture(
                        new RuntimeException("Operation failed after " + attempt + " attempts", throwable));
                }
                
                System.out.println("Attempt " + attempt + " failed, retrying in " + delay + "ms: " + throwable.getMessage());
                
                return CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS)
                    .execute(() -> {})
                    .thenCompose(v -> executeWithRetryAsync(operation, remainingRetries - 1, Math.min(delay * 2, 5000), attempt + 1));
            });
    }
    
    // Example usage
    public static void demonstrateRetry(IgniteClient client) {
        // Synchronous retry
        String result = executeWithRetry(
            () -> {
                // Potentially failing operation - get random artist name
                return client.sql().execute(null, "SELECT Name FROM Artist LIMIT 1").next().stringValue("Name");
            },
            IgniteException.class,
            3
        );
        
        System.out.println("Artist result with retry: " + result);
        
        // Asynchronous retry for album count
        CompletableFuture<Integer> asyncResult = executeWithRetryAsync(
            () -> {
                return client.sql().executeAsync(null, "SELECT COUNT(*) as album_count FROM Album")
                    .thenApply(resultSet -> resultSet.next().intValue("album_count"));
            },
            3
        );
        
        asyncResult.whenComplete((res, throwable) -> {
            if (throwable != null) {
                System.err.println("Async operation failed: " + throwable.getMessage());
            } else {
                System.out.println("Async result with retry: " + res);
            }
        });
    }
}
```

### Circuit Breaker Patterns

#### Enterprise Circuit Breaker Implementation

```java
public class CircuitBreakerPattern {
    
    public enum CircuitState {
        CLOSED,    // Normal operation
        OPEN,      // Failing fast
        HALF_OPEN  // Testing if service recovered
    }
    
    public static class CircuitBreaker {
        private volatile CircuitState state = CircuitState.CLOSED;
        private volatile int failureCount = 0;
        private volatile long lastFailureTime = 0;
        private volatile long lastSuccessTime = System.currentTimeMillis();
        
        private final int failureThreshold;
        private final long timeout;
        private final long monitoringPeriod;
        
        public CircuitBreaker(int failureThreshold, long timeoutMs, long monitoringPeriodMs) {
            this.failureThreshold = failureThreshold;
            this.timeout = timeoutMs;
            this.monitoringPeriod = monitoringPeriodMs;
        }
        
        public <T> T execute(Supplier<T> operation) throws Exception {
            if (state == CircuitState.OPEN) {
                if (System.currentTimeMillis() - lastFailureTime < timeout) {
                    throw new RuntimeException("Circuit breaker is OPEN");
                } else {
                    state = CircuitState.HALF_OPEN;
                }
            }
            
            try {
                T result = operation.get();
                onSuccess();
                return result;
                
            } catch (Exception e) {
                onFailure();
                throw e;
            }
        }
        
        private void onSuccess() {
            failureCount = 0;
            lastSuccessTime = System.currentTimeMillis();
            state = CircuitState.CLOSED;
        }
        
        private void onFailure() {
            failureCount++;
            lastFailureTime = System.currentTimeMillis();
            
            if (failureCount >= failureThreshold) {
                state = CircuitState.OPEN;
                System.out.println("Circuit breaker opened due to " + failureCount + " consecutive failures");
            }
        }
        
        public CircuitState getState() {
            return state;
        }
        
        public int getFailureCount() {
            return failureCount;
        }
        
        // Health monitoring
        public boolean isHealthy() {
            long timeSinceLastSuccess = System.currentTimeMillis() - lastSuccessTime;
            return state == CircuitState.CLOSED && timeSinceLastSuccess < monitoringPeriod;
        }
    }
    
    // Usage example with Ignite operations
    public static void demonstrateCircuitBreaker(IgniteClient client) {
        CircuitBreaker circuitBreaker = new CircuitBreaker(
            5,      // 5 failures
            30000,  // 30 second timeout
            60000   // 1 minute monitoring period
        );
        
        // Simulate multiple operations
        for (int i = 0; i < 10; i++) {
            try {
                String result = circuitBreaker.execute(() -> {
                    // Potentially failing operation - get artist by ID
                    if (Math.random() > 0.7) {
                        throw new RuntimeException("Simulated artist lookup failure");
                    }
                    return client.sql().execute(null, "SELECT Name FROM Artist WHERE ArtistId = " + (i + 1))
                        .next().stringValue("Name");
                });
                
                System.out.println("Success: " + result + ", Circuit state: " + circuitBreaker.getState());
                
            } catch (Exception e) {
                System.err.println("Failure: " + e.getMessage() + ", Circuit state: " + circuitBreaker.getState());
            }
            
            // Brief pause between operations
            try { Thread.sleep(1000); } catch (InterruptedException ie) {}
        }
    }
}
```

## Performance Optimization

### Connection Pooling and Management

#### Enterprise Connection Pool

```java
public class ConnectionPoolManagement {
    
    public static class IgniteConnectionPool {
        private final Queue<IgniteClient> pool = new ConcurrentLinkedQueue<>();
        private final AtomicInteger activeConnections = new AtomicInteger(0);
        private final AtomicInteger totalConnections = new AtomicInteger(0);
        
        private final int maxPoolSize;
        private final int minPoolSize;
        private final String[] addresses;
        private final IgniteClientConfiguration config;
        
        public IgniteConnectionPool(int minPoolSize, int maxPoolSize, 
                                  String[] addresses, IgniteClientConfiguration config) {
            this.minPoolSize = minPoolSize;
            this.maxPoolSize = maxPoolSize;
            this.addresses = addresses;
            this.config = config;
            
            // Initialize minimum connections
            initializePool();
        }
        
        private void initializePool() {
            for (int i = 0; i < minPoolSize; i++) {
                try {
                    IgniteClient client = createClient();
                    pool.offer(client);
                    totalConnections.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Failed to initialize connection " + i + ": " + e.getMessage());
                }
            }
            System.out.println("Connection pool initialized with " + pool.size() + " connections");
        }
        
        public IgniteClient borrowConnection() throws Exception {
            IgniteClient client = pool.poll();
            
            if (client == null) {
                if (totalConnections.get() < maxPoolSize) {
                    client = createClient();
                    totalConnections.incrementAndGet();
                } else {
                    throw new RuntimeException("Connection pool exhausted");
                }
            }
            
            activeConnections.incrementAndGet();
            return client;
        }
        
        public void returnConnection(IgniteClient client) {
            if (client != null) {
                pool.offer(client);
                activeConnections.decrementAndGet();
            }
        }
        
        private IgniteClient createClient() {
            return IgniteClient.builder()
                .addresses(addresses)
                .connectTimeout(config.connectTimeout())
                .operationTimeout(config.operationTimeout())
                .heartbeatInterval(config.heartbeatInterval())
                .build();
        }
        
        public void shutdown() {
            IgniteClient client;
            while ((client = pool.poll()) != null) {
                try {
                    client.close();
                } catch (Exception e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
            System.out.println("Connection pool shutdown complete");
        }
        
        // Monitoring methods
        public int getActiveConnections() { return activeConnections.get(); }
        public int getTotalConnections() { return totalConnections.get(); }
        public int getAvailableConnections() { return pool.size(); }
    }
    
    // Connection pool usage pattern
    public static void demonstrateConnectionPool() {
        IgniteConnectionPool pool = new IgniteConnectionPool(
            5,  // min connections
            20, // max connections
            new String[]{"localhost:10800"},
            IgniteClientConfiguration.builder().build()
        );
        
        // Use connection from pool
        try {
            IgniteClient client = pool.borrowConnection();
            
            try {
                // Perform operations - get random artist
                var result = client.sql().execute(null, "SELECT Name FROM Artist ORDER BY RANDOM() LIMIT 1");
                System.out.println("Random Artist: " + result.next().stringValue("Name"));
                
            } finally {
                // Always return connection to pool
                pool.returnConnection(client);
            }
            
        } catch (Exception e) {
            System.err.println("Operation failed: " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }
}
```

### Batch Operations Optimization

#### High-Performance Batch Processing

```java
public class BatchOptimization {
    
    // Optimized batch insertion for Artists
    public static void optimizedBatchInsert(IgniteClient client, List<Artist> artists) {
        RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
        
        // Process in optimal batch sizes
        int batchSize = 1000;
        int totalBatches = (artists.size() + batchSize - 1) / batchSize;
        
        System.out.println("Processing " + artists.size() + " artists in " + totalBatches + " batches");
        
        for (int i = 0; i < artists.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, artists.size());
            List<Artist> batch = artists.subList(i, endIndex);
            
            long startTime = System.currentTimeMillis();
            
            try {
                // Use upsertAll for best performance
                artistView.upsertAll(null, batch);
                
                long duration = System.currentTimeMillis() - startTime;
                double rate = (double) batch.size() / duration * 1000;
                
                System.out.printf("Batch %d/%d: %d artists in %d ms (%.2f artists/sec)%n", 
                    (i / batchSize) + 1, totalBatches, batch.size(), duration, rate);
                
            } catch (Exception e) {
                System.err.println("Artist batch failed: " + e.getMessage());
                // Could implement individual record fallback
                fallbackIndividualInserts(artistView, batch);
            }
        }
    }
    
    // Fallback for failed batches
    private static void fallbackIndividualInserts(RecordView<Artist> view, List<Artist> batch) {
        System.out.println("Falling back to individual inserts for " + batch.size() + " artists");
        
        for (Artist artist : batch) {
            try {
                view.upsert(null, artist);
            } catch (Exception e) {
                System.err.println("Failed to insert artist " + artist.getArtistId() + ": " + e.getMessage());
            }
        }
    }
    
    // Parallel batch processing for Albums
    public static void parallelBatchProcessing(IgniteClient client, List<Album> albums) {
        RecordView<Album> albumView = client.tables().table("Album").recordView(Album.class);
        
        int batchSize = 500;
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        System.out.println("Processing " + albums.size() + " albums with " + numThreads + " threads");
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < albums.size(); i += batchSize) {
            final int startIndex = i;
            final int endIndex = Math.min(i + batchSize, albums.size());
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                List<Album> batch = albums.subList(startIndex, endIndex);
                
                try {
                    long startTime = System.currentTimeMillis();
                    albumView.upsertAll(null, batch);
                    long duration = System.currentTimeMillis() - startTime;
                    
                    System.out.printf("Thread %s processed %d albums in %d ms%n", 
                        Thread.currentThread().getName(), batch.size(), duration);
                        
                } catch (Exception e) {
                    System.err.println("Parallel album batch failed: " + e.getMessage());
                }
            }, executor);
            
            futures.add(future);
        }
        
        // Wait for all batches to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    System.err.println("Parallel processing failed: " + throwable.getMessage());
                } else {
                    System.out.println("All parallel batches completed successfully");
                }
                executor.shutdown();
            })
            .join();
    }
}
```

### Async Patterns and Performance

#### Advanced Async Programming

```java
public class AsyncPatterns {
    
    // Reactive-style async processing for Artists
    public static CompletableFuture<List<String>> processArtistsAsync(IgniteClient client, List<Integer> artistIds) {
        RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
        
        // Create async operations for each artist
        List<CompletableFuture<String>> artistFutures = artistIds.stream()
            .map(id -> {
                Artist key = new Artist();
                key.setArtistId(id);
                
                return artistView.getAsync(null, key)
                    .thenApply(artist -> {
                        if (artist != null) {
                            return artist.getName();
                        } else {
                            return "Artist " + id + " not found";
                        }
                    })
                    .exceptionally(throwable -> {
                        System.err.println("Failed to get artist " + id + ": " + throwable.getMessage());
                        return "Error: " + id;
                    });
            })
            .collect(Collectors.toList());
        
        // Combine all results
        return CompletableFuture.allOf(artistFutures.toArray(new CompletableFuture[0]))
            .thenApply(v -> artistFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }
    
    // Async pipeline with transformation for music analytics
    public static CompletableFuture<Map<String, Integer>> analyzeMusicDataAsync(IgniteClient client) {
        return client.sql().executeAsync(null, 
                "SELECT g.Name as genre, COUNT(*) as track_count " +
                "FROM Track t JOIN Genre g ON t.GenreId = g.GenreId " +
                "GROUP BY g.Name ORDER BY track_count DESC")
            .thenApply(resultSet -> {
                Map<String, Integer> genreStats = new HashMap<>();
                
                while (resultSet.hasNext()) {
                    var row = resultSet.next();
                    String genre = row.stringValue("genre");
                    Integer count = row.intValue("track_count");
                    genreStats.put(genre, count);
                }
                
                return genreStats;
            })
            .thenApply(stats -> {
                // Additional processing
                System.out.println("Processed music statistics for " + stats.size() + " genres");
                return stats;
            })
            .exceptionally(throwable -> {
                System.err.println("Failed to analyze music data: " + throwable.getMessage());
                return Collections.emptyMap();
            });
    }
    
    // Timeout handling for async operations
    public static <T> CompletableFuture<T> withTimeout(CompletableFuture<T> future, long timeoutMs) {
        CompletableFuture<T> timeoutFuture = new CompletableFuture<>();
        
        // Create timeout
        CompletableFuture.delayedExecutor(timeoutMs, TimeUnit.MILLISECONDS)
            .execute(() -> timeoutFuture.completeExceptionally(
                new TimeoutException("Operation timed out after " + timeoutMs + "ms")));
        
        // Race between operation and timeout
        return future.applyToEither(timeoutFuture, Function.identity());
    }
    
    // Example usage
    public static void demonstrateAsyncPatterns(IgniteClient client) {
        // Async artist processing with timeout
        List<Integer> artistIds = Arrays.asList(1, 2, 3, 4, 5);
        
        CompletableFuture<List<String>> artistFuture = withTimeout(
            processArtistsAsync(client, artistIds), 
            5000  // 5 second timeout
        );
        
        artistFuture.whenComplete((artists, throwable) -> {
            if (throwable != null) {
                System.err.println("Artist processing failed: " + throwable.getMessage());
            } else {
                System.out.println("Processed artists: " + artists);
            }
        });
        
        // Async music data analysis
        CompletableFuture<Map<String, Integer>> analysisFuture = analyzeMusicDataAsync(client);
        
        analysisFuture.whenComplete((stats, throwable) -> {
            if (throwable != null) {
                System.err.println("Music analysis failed: " + throwable.getMessage());
            } else {
                stats.forEach((genre, count) -> 
                    System.out.println(genre + ": " + count + " tracks"));
            }
        });
        
        // Wait for both to complete
        CompletableFuture.allOf(artistFuture, analysisFuture).join();
    }
}
```

## Monitoring and Metrics

### JMX Metrics Integration

```java
public class JMXMetrics {
    
    // Custom MBean for Ignite metrics
    public interface IgniteOperationMetricsMBean {
        long getTotalOperations();
        long getSuccessfulOperations();
        long getFailedOperations();
        double getAverageResponseTime();
        void reset();
    }
    
    public static class IgniteOperationMetrics implements IgniteOperationMetricsMBean {
        private final AtomicLong totalOperations = new AtomicLong(0);
        private final AtomicLong successfulOperations = new AtomicLong(0);
        private final AtomicLong failedOperations = new AtomicLong(0);
        private final AtomicLong totalResponseTime = new AtomicLong(0);
        
        public void recordOperation(long responseTimeMs, boolean success) {
            totalOperations.incrementAndGet();
            totalResponseTime.addAndGet(responseTimeMs);
            
            if (success) {
                successfulOperations.incrementAndGet();
            } else {
                failedOperations.incrementAndGet();
            }
        }
        
        @Override
        public long getTotalOperations() {
            return totalOperations.get();
        }
        
        @Override
        public long getSuccessfulOperations() {
            return successfulOperations.get();
        }
        
        @Override
        public long getFailedOperations() {
            return failedOperations.get();
        }
        
        @Override
        public double getAverageResponseTime() {
            long total = totalOperations.get();
            return total > 0 ? (double) totalResponseTime.get() / total : 0.0;
        }
        
        @Override
        public void reset() {
            totalOperations.set(0);
            successfulOperations.set(0);
            failedOperations.set(0);
            totalResponseTime.set(0);
        }
    }
    
    // Register metrics with JMX
    public static void registerMetrics() {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            IgniteOperationMetrics metrics = new IgniteOperationMetrics();
            
            ObjectName objectName = new ObjectName("com.example.ignite:type=Operations");
            server.registerMBean(metrics, objectName);
            
            System.out.println("JMX metrics registered successfully");
            
        } catch (Exception e) {
            System.err.println("Failed to register JMX metrics: " + e.getMessage());
        }
    }
}
```

### Connection Monitoring

```java
public class ConnectionMonitoring {
    
    // Connection health checker
    public static class ConnectionHealthChecker {
        private final IgniteClient client;
        private final ScheduledExecutorService scheduler;
        private volatile boolean isHealthy = true;
        private volatile long lastHealthCheck = System.currentTimeMillis();
        
        public ConnectionHealthChecker(IgniteClient client) {
            this.client = client;
            this.scheduler = Executors.newScheduledThreadPool(1);
            startHealthChecking();
        }
        
        private void startHealthChecking() {
            scheduler.scheduleAtFixedRate(this::performHealthCheck, 30, 30, TimeUnit.SECONDS);
        }
        
        private void performHealthCheck() {
            try {
                // Simple health check query using Artist table
                long startTime = System.currentTimeMillis();
                client.sql().execute(null, "SELECT COUNT(*) as artist_count FROM Artist").next();
                long duration = System.currentTimeMillis() - startTime;
                
                isHealthy = true;
                lastHealthCheck = System.currentTimeMillis();
                
                System.out.println("Health check passed in " + duration + "ms");
                
            } catch (Exception e) {
                isHealthy = false;
                System.err.println("Health check failed: " + e.getMessage());
            }
        }
        
        public boolean isHealthy() {
            return isHealthy && (System.currentTimeMillis() - lastHealthCheck < 60000); // 1 minute threshold
        }
        
        public void shutdown() {
            scheduler.shutdown();
        }
    }
    
    // Demonstrate connection monitoring
    public static void demonstrateConnectionMonitoring(IgniteClient client) {
        ConnectionHealthChecker healthChecker = new ConnectionHealthChecker(client);
        
        // Monitor for a period
        for (int i = 0; i < 10; i++) {
            System.out.println("Connection healthy: " + healthChecker.isHealthy());
            
            try {
                Thread.sleep(5000); // Check every 5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        healthChecker.shutdown();
    }
}
```

## Chinook Music Store Advanced Operations

### High-Performance Music Analytics

```java
public class ChinookAdvancedOperations {
    
    // Advanced music recommendation engine with async processing
    public static CompletableFuture<List<TrackRecommendation>> generateRecommendationsAsync(
            IgniteClient client, Integer customerId, int limit) {
        
        return CompletableFuture.supplyAsync(() -> {
            // Get customer's purchase history
            String customerHistoryQuery = """
                SELECT DISTINCT t.GenreId, g.Name as genre_name, COUNT(*) as purchase_count
                FROM InvoiceLine il
                JOIN Invoice i ON il.InvoiceId = i.InvoiceId
                JOIN Track t ON il.TrackId = t.TrackId
                JOIN Genre g ON t.GenreId = g.GenreId
                WHERE i.CustomerId = ?
                GROUP BY t.GenreId, g.Name
                ORDER BY purchase_count DESC
                LIMIT 3
                """;
            
            List<Integer> preferredGenres = new ArrayList<>();
            
            try (var resultSet = client.sql().execute(null, customerHistoryQuery, customerId)) {
                while (resultSet.hasNext()) {
                    var row = resultSet.next();
                    preferredGenres.add(row.intValue("GenreId"));
                }
            }
            
            if (preferredGenres.isEmpty()) {
                // Fallback to popular tracks
                return getPopularTracks(client, limit);
            }
            
            // Find similar tracks in preferred genres
            String recommendationQuery = """
                SELECT t.TrackId, t.Name, a.Name as artist_name, al.Title as album_title,
                       t.UnitPrice, AVG(il.Quantity) as popularity_score
                FROM Track t
                JOIN Album al ON t.AlbumId = al.AlbumId
                JOIN Artist a ON al.ArtistId = a.ArtistId
                LEFT JOIN InvoiceLine il ON t.TrackId = il.TrackId
                WHERE t.GenreId IN (""" + 
                preferredGenres.stream().map(String::valueOf).collect(Collectors.joining(",")) + """)
                AND t.TrackId NOT IN (
                    SELECT DISTINCT il2.TrackId 
                    FROM InvoiceLine il2 
                    JOIN Invoice i2 ON il2.InvoiceId = i2.InvoiceId 
                    WHERE i2.CustomerId = ?
                )
                GROUP BY t.TrackId, t.Name, a.Name, al.Title, t.UnitPrice
                ORDER BY popularity_score DESC NULLS LAST, t.UnitPrice ASC
                LIMIT ?
                """;
            
            List<TrackRecommendation> recommendations = new ArrayList<>();
            
            try (var resultSet = client.sql().execute(null, recommendationQuery, customerId, limit)) {
                while (resultSet.hasNext()) {
                    var row = resultSet.next();
                    recommendations.add(new TrackRecommendation(
                        row.intValue("TrackId"),
                        row.stringValue("Name"),
                        row.stringValue("artist_name"),
                        row.stringValue("album_title"),
                        row.decimalValue("UnitPrice"),
                        row.doubleValue("popularity_score")
                    ));
                }
            }
            
            return recommendations;
        })
        .exceptionally(throwable -> {
            System.err.println("Failed to generate recommendations: " + throwable.getMessage());
            return Collections.emptyList();
        });
    }
    
    private static List<TrackRecommendation> getPopularTracks(IgniteClient client, int limit) {
        String popularTracksQuery = """
            SELECT t.TrackId, t.Name, a.Name as artist_name, al.Title as album_title,
                   t.UnitPrice, COUNT(il.TrackId) as sales_count
            FROM Track t
            JOIN Album al ON t.AlbumId = al.AlbumId
            JOIN Artist a ON al.ArtistId = a.ArtistId
            JOIN InvoiceLine il ON t.TrackId = il.TrackId
            GROUP BY t.TrackId, t.Name, a.Name, al.Title, t.UnitPrice
            ORDER BY sales_count DESC
            LIMIT ?
            """;
        
        List<TrackRecommendation> popular = new ArrayList<>();
        
        try (var resultSet = client.sql().execute(null, popularTracksQuery, limit)) {
            while (resultSet.hasNext()) {
                var row = resultSet.next();
                popular.add(new TrackRecommendation(
                    row.intValue("TrackId"),
                    row.stringValue("Name"),
                    row.stringValue("artist_name"),
                    row.stringValue("album_title"),
                    row.decimalValue("UnitPrice"),
                    row.doubleValue("sales_count")
                ));
            }
        }
        
        return popular;
    }
    
    // Advanced analytics with circuit breaker pattern
    public static class MusicAnalyticsService {
        private final IgniteClient client;
        private final CircuitBreakerPattern.CircuitBreaker circuitBreaker;
        
        public MusicAnalyticsService(IgniteClient client) {
            this.client = client;
            this.circuitBreaker = new CircuitBreakerPattern.CircuitBreaker(3, 30000, 60000);
        }
        
        public CompletableFuture<SalesReport> generateSalesReportAsync(LocalDate startDate, LocalDate endDate) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return circuitBreaker.execute(() -> generateSalesReport(startDate, endDate));
                } catch (Exception e) {
                    throw new RuntimeException("Sales report generation failed", e);
                }
            });
        }
        
        private SalesReport generateSalesReport(LocalDate startDate, LocalDate endDate) {
            String salesQuery = """
                SELECT 
                    COUNT(DISTINCT i.InvoiceId) as total_orders,
                    SUM(i.Total) as total_revenue,
                    COUNT(DISTINCT i.CustomerId) as unique_customers,
                    AVG(i.Total) as avg_order_value,
                    COUNT(il.InvoiceLineId) as total_items_sold
                FROM Invoice i
                JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId
                WHERE i.InvoiceDate BETWEEN ? AND ?
                """;
            
            try (var resultSet = client.sql().execute(null, salesQuery, startDate, endDate)) {
                if (resultSet.hasNext()) {
                    var row = resultSet.next();
                    return new SalesReport(
                        row.intValue("total_orders"),
                        row.decimalValue("total_revenue"),
                        row.intValue("unique_customers"),
                        row.decimalValue("avg_order_value"),
                        row.intValue("total_items_sold"),
                        getTopSellingTracks(startDate, endDate),
                        getTopSpendingCustomers(startDate, endDate)
                    );
                }
            }
            
            return new SalesReport();
        }
        
        private List<TopTrack> getTopSellingTracks(LocalDate startDate, LocalDate endDate) {
            String topTracksQuery = """
                SELECT t.Name, a.Name as artist_name, SUM(il.Quantity) as total_sold,
                       SUM(il.UnitPrice * il.Quantity) as revenue
                FROM InvoiceLine il
                JOIN Invoice i ON il.InvoiceId = i.InvoiceId
                JOIN Track t ON il.TrackId = t.TrackId
                JOIN Album al ON t.AlbumId = al.AlbumId
                JOIN Artist a ON al.ArtistId = a.ArtistId
                WHERE i.InvoiceDate BETWEEN ? AND ?
                GROUP BY t.TrackId, t.Name, a.Name
                ORDER BY total_sold DESC
                LIMIT 10
                """;
            
            List<TopTrack> topTracks = new ArrayList<>();
            
            try (var resultSet = client.sql().execute(null, topTracksQuery, startDate, endDate)) {
                while (resultSet.hasNext()) {
                    var row = resultSet.next();
                    topTracks.add(new TopTrack(
                        row.stringValue("Name"),
                        row.stringValue("artist_name"),
                        row.intValue("total_sold"),
                        row.decimalValue("revenue")
                    ));
                }
            }
            
            return topTracks;
        }
        
        private List<TopCustomer> getTopSpendingCustomers(LocalDate startDate, LocalDate endDate) {
            String topCustomersQuery = """
                SELECT c.FirstName, c.LastName, c.Email, SUM(i.Total) as total_spent,
                       COUNT(i.InvoiceId) as order_count
                FROM Customer c
                JOIN Invoice i ON c.CustomerId = i.CustomerId
                WHERE i.InvoiceDate BETWEEN ? AND ?
                GROUP BY c.CustomerId, c.FirstName, c.LastName, c.Email
                ORDER BY total_spent DESC
                LIMIT 10
                """;
            
            List<TopCustomer> topCustomers = new ArrayList<>();
            
            try (var resultSet = client.sql().execute(null, topCustomersQuery, startDate, endDate)) {
                while (resultSet.hasNext()) {
                    var row = resultSet.next();
                    topCustomers.add(new TopCustomer(
                        row.stringValue("FirstName") + " " + row.stringValue("LastName"),
                        row.stringValue("Email"),
                        row.decimalValue("total_spent"),
                        row.intValue("order_count")
                    ));
                }
            }
            
            return topCustomers;
        }
    }
    
    // Data transfer objects
    public static class TrackRecommendation {
        private final Integer trackId;
        private final String name;
        private final String artistName;
        private final String albumTitle;
        private final BigDecimal unitPrice;
        private final Double popularityScore;
        
        public TrackRecommendation(Integer trackId, String name, String artistName, 
                                 String albumTitle, BigDecimal unitPrice, Double popularityScore) {
            this.trackId = trackId;
            this.name = name;
            this.artistName = artistName;
            this.albumTitle = albumTitle;
            this.unitPrice = unitPrice;
            this.popularityScore = popularityScore;
        }
        
        // Getters...
    }
    
    public static class SalesReport {
        private final Integer totalOrders;
        private final BigDecimal totalRevenue;
        private final Integer uniqueCustomers;
        private final BigDecimal avgOrderValue;
        private final Integer totalItemsSold;
        private final List<TopTrack> topTracks;
        private final List<TopCustomer> topCustomers;
        
        public SalesReport() {
            this(0, BigDecimal.ZERO, 0, BigDecimal.ZERO, 0, new ArrayList<>(), new ArrayList<>());
        }
        
        public SalesReport(Integer totalOrders, BigDecimal totalRevenue, Integer uniqueCustomers,
                          BigDecimal avgOrderValue, Integer totalItemsSold, List<TopTrack> topTracks,
                          List<TopCustomer> topCustomers) {
            this.totalOrders = totalOrders;
            this.totalRevenue = totalRevenue;
            this.uniqueCustomers = uniqueCustomers;
            this.avgOrderValue = avgOrderValue;
            this.totalItemsSold = totalItemsSold;
            this.topTracks = topTracks;
            this.topCustomers = topCustomers;
        }
        
        // Getters...
    }
    
    public static class TopTrack {
        private final String name;
        private final String artistName;
        private final Integer totalSold;
        private final BigDecimal revenue;
        
        public TopTrack(String name, String artistName, Integer totalSold, BigDecimal revenue) {
            this.name = name;
            this.artistName = artistName;
            this.totalSold = totalSold;
            this.revenue = revenue;
        }
        
        // Getters...
    }
    
    public static class TopCustomer {
        private final String name;
        private final String email;
        private final BigDecimal totalSpent;
        private final Integer orderCount;
        
        public TopCustomer(String name, String email, BigDecimal totalSpent, Integer orderCount) {
            this.name = name;
            this.email = email;
            this.totalSpent = totalSpent;
            this.orderCount = orderCount;
        }
        
        // Getters...
    }
}
```

## Security

### Authentication Setup

```java
public class AuthenticationSetup {
    
    // Basic authentication
    public static IgniteClient createAuthenticatedClient() {
        return IgniteClient.builder()
            .addresses("localhost:10800")
            .authenticator(BasicAuthenticator.builder()
                .username("ignite_user")
                .password("secure_password")
                .build())
            .build();
    }
    
    // Environment-based authentication
    public static IgniteClient createClientFromEnvironment() {
        String username = System.getenv("IGNITE_USERNAME");
        String password = System.getenv("IGNITE_PASSWORD");
        String addresses = System.getenv("IGNITE_ADDRESSES");
        
        if (username == null || password == null || addresses == null) {
            throw new IllegalStateException("Required environment variables not set");
        }
        
        return IgniteClient.builder()
            .addresses(addresses.split(","))
            .authenticator(BasicAuthenticator.builder()
                .username(username)
                .password(password)
                .build())
            .build();
    }
}
```

### SSL/TLS Configuration

```java
public class SSLConfiguration {
    
    // Production SSL setup
    public static IgniteClient createSecureClient() {
        SslConfiguration sslConfig = SslConfiguration.builder()
            .enabled(true)
            .keyStorePath("/path/to/client-keystore.jks")
            .keyStorePassword("keystore-password")
            .trustStorePath("/path/to/client-truststore.jks")
            .trustStorePassword("truststore-password")
            .build();
        
        return IgniteClient.builder()
            .addresses("secure-ignite-cluster:10800")
            .ssl(sslConfig)
            .authenticator(BasicAuthenticator.builder()
                .username("secure_user")
                .password("secure_password")
                .build())
            .build();
    }
    
    // Development SSL setup (self-signed certificates)
    public static IgniteClient createDevSecureClient() {
        SslConfiguration sslConfig = SslConfiguration.builder()
            .enabled(true)
            .trustAll(true)  // Only for development!
            .build();
        
        return IgniteClient.builder()
            .addresses("localhost:10800")
            .ssl(sslConfig)
            .build();
    }
}
```
