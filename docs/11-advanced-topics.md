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
        // Example operation that might fail
        var table = client.tables().table("NonExistentTable");
        table.recordView().get(null, new Object());
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
                // Potentially failing operation
                return client.sql().execute(null, "SELECT 'Hello' as greeting").next().stringValue("greeting");
            },
            IgniteException.class,
            3
        );
        
        System.out.println("Result with retry: " + result);
        
        // Asynchronous retry
        CompletableFuture<String> asyncResult = executeWithRetryAsync(
            () -> {
                return client.sql().executeAsync(null, "SELECT 'Hello Async' as greeting")
                    .thenApply(resultSet -> resultSet.next().stringValue("greeting"));
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
                    // Potentially failing operation
                    if (Math.random() > 0.7) {
                        throw new RuntimeException("Simulated failure");
                    }
                    return client.sql().execute(null, "SELECT " + i + " as value").next().stringValue("value");
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
                // Perform operations
                var result = client.sql().execute(null, "SELECT 'Hello' as greeting");
                System.out.println("Result: " + result.next().stringValue("greeting"));
                
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
    
    // Optimized batch insertion
    public static void optimizedBatchInsert(IgniteClient client, List<Customer> customers) {
        RecordView<Customer> customerView = client.tables().table("Customer").recordView(Customer.class);
        
        // Process in optimal batch sizes
        int batchSize = 1000;
        int totalBatches = (customers.size() + batchSize - 1) / batchSize;
        
        System.out.println("Processing " + customers.size() + " customers in " + totalBatches + " batches");
        
        for (int i = 0; i < customers.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, customers.size());
            List<Customer> batch = customers.subList(i, endIndex);
            
            long startTime = System.currentTimeMillis();
            
            try {
                // Use upsertAll for best performance
                customerView.upsertAll(null, batch);
                
                long duration = System.currentTimeMillis() - startTime;
                double rate = (double) batch.size() / duration * 1000;
                
                System.out.printf("Batch %d/%d: %d records in %d ms (%.2f records/sec)%n", 
                    (i / batchSize) + 1, totalBatches, batch.size(), duration, rate);
                
            } catch (Exception e) {
                System.err.println("Batch failed: " + e.getMessage());
                // Could implement individual record fallback
                fallbackIndividualInserts(customerView, batch);
            }
        }
    }
    
    // Fallback for failed batches
    private static void fallbackIndividualInserts(RecordView<Customer> view, List<Customer> batch) {
        System.out.println("Falling back to individual inserts for " + batch.size() + " records");
        
        for (Customer customer : batch) {
            try {
                view.upsert(null, customer);
            } catch (Exception e) {
                System.err.println("Failed to insert customer " + customer.getId() + ": " + e.getMessage());
            }
        }
    }
    
    // Parallel batch processing
    public static void parallelBatchProcessing(IgniteClient client, List<Customer> customers) {
        RecordView<Customer> customerView = client.tables().table("Customer").recordView(Customer.class);
        
        int batchSize = 500;
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        System.out.println("Processing with " + numThreads + " threads");
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < customers.size(); i += batchSize) {
            final int startIndex = i;
            final int endIndex = Math.min(i + batchSize, customers.size());
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                List<Customer> batch = customers.subList(startIndex, endIndex);
                
                try {
                    long startTime = System.currentTimeMillis();
                    customerView.upsertAll(null, batch);
                    long duration = System.currentTimeMillis() - startTime;
                    
                    System.out.printf("Thread %s processed %d records in %d ms%n", 
                        Thread.currentThread().getName(), batch.size(), duration);
                        
                } catch (Exception e) {
                    System.err.println("Parallel batch failed: " + e.getMessage());
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
    
    // Reactive-style async processing
    public static CompletableFuture<List<String>> processCustomersAsync(IgniteClient client, List<Integer> customerIds) {
        RecordView<Customer> customerView = client.tables().table("Customer").recordView(Customer.class);
        
        // Create async operations for each customer
        List<CompletableFuture<String>> customerFutures = customerIds.stream()
            .map(id -> {
                Customer key = new Customer();
                key.setId(id);
                
                return customerView.getAsync(null, key)
                    .thenApply(customer -> {
                        if (customer != null) {
                            return customer.getFirstName() + " " + customer.getLastName();
                        } else {
                            return "Customer " + id + " not found";
                        }
                    })
                    .exceptionally(throwable -> {
                        System.err.println("Failed to get customer " + id + ": " + throwable.getMessage());
                        return "Error: " + id;
                    });
            })
            .collect(Collectors.toList());
        
        // Combine all results
        return CompletableFuture.allOf(customerFutures.toArray(new CompletableFuture[0]))
            .thenApply(v -> customerFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }
    
    // Async pipeline with transformation
    public static CompletableFuture<Map<String, Integer>> analyzeCustomerDataAsync(IgniteClient client) {
        return client.sql().executeAsync(null, "SELECT country, COUNT(*) as count FROM Customer GROUP BY country")
            .thenApply(resultSet -> {
                Map<String, Integer> countryStats = new HashMap<>();
                
                while (resultSet.hasNext()) {
                    var row = resultSet.next();
                    String country = row.stringValue("country");
                    Integer count = row.intValue("count");
                    countryStats.put(country, count);
                }
                
                return countryStats;
            })
            .thenApply(stats -> {
                // Additional processing
                System.out.println("Processed customer statistics for " + stats.size() + " countries");
                return stats;
            })
            .exceptionally(throwable -> {
                System.err.println("Failed to analyze customer data: " + throwable.getMessage());
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
        // Async customer processing with timeout
        List<Integer> customerIds = Arrays.asList(1, 2, 3, 4, 5);
        
        CompletableFuture<List<String>> customerFuture = withTimeout(
            processCustomersAsync(client, customerIds), 
            5000  // 5 second timeout
        );
        
        customerFuture.whenComplete((customers, throwable) -> {
            if (throwable != null) {
                System.err.println("Customer processing failed: " + throwable.getMessage());
            } else {
                System.out.println("Processed customers: " + customers);
            }
        });
        
        // Async data analysis
        CompletableFuture<Map<String, Integer>> analysisFuture = analyzeCustomerDataAsync(client);
        
        analysisFuture.whenComplete((stats, throwable) -> {
            if (throwable != null) {
                System.err.println("Analysis failed: " + throwable.getMessage());
            } else {
                stats.forEach((country, count) -> 
                    System.out.println(country + ": " + count + " customers"));
            }
        });
        
        // Wait for both to complete
        CompletableFuture.allOf(customerFuture, analysisFuture).join();
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
                // Simple health check query
                long startTime = System.currentTimeMillis();
                client.sql().execute(null, "SELECT 1 as health_check").next();
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
