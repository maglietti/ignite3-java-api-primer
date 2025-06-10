# 12. Best Practices & Common Patterns

This section provides best practices for production Apache Ignite 3 deployments, covering resource management, error handling, performance optimization, and testing strategies.

## Resource Management

### Connection Lifecycle Management

#### Proper Connection Handling

```java
public class ConnectionLifecycleExample {
    
    // ✅ GOOD: Use try-with-resources for automatic cleanup
    public void goodConnectionHandling() {
        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800")
                .build()) {
            
            // Perform operations
            performOperations(client);
            
        } // Connection automatically closed
    }
    
    // ❌ BAD: Manual connection management (prone to leaks)
    public void badConnectionHandling() {
        IgniteClient client = IgniteClient.builder()
            .addresses("localhost:10800")
            .build();
        
        try {
            performOperations(client);
        } finally {
            // Easy to forget or skip in error conditions
            client.close();
        }
    }
    
    // ✅ BEST: Connection pool for high-throughput applications
    public static class ConnectionManager {
        private final Queue<IgniteClient> connectionPool = new ConcurrentLinkedQueue<>();
        private final AtomicInteger activeConnections = new AtomicInteger(0);
        private final int maxConnections;
        private final String[] addresses;
        
        public ConnectionManager(int maxConnections, String... addresses) {
            this.maxConnections = maxConnections;
            this.addresses = addresses;
        }
        
        public IgniteClient borrowConnection() {
            IgniteClient client = connectionPool.poll();
            if (client == null && activeConnections.get() < maxConnections) {
                client = IgniteClient.builder().addresses(addresses).build();
                activeConnections.incrementAndGet();
            }
            return client;
        }
        
        public void returnConnection(IgniteClient client) {
            if (client != null) {
                connectionPool.offer(client);
            }
        }
        
        public void shutdown() {
            IgniteClient client;
            while ((client = connectionPool.poll()) != null) {
                try {
                    client.close();
                } catch (Exception e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }
    
    private void performOperations(IgniteClient client) {
        // Implementation...
    }
}
```

#### Resource Cleanup Patterns

```java
public class ResourceCleanupPatterns {
    
    // Pattern 1: Nested resource management
    public void nestedResourceExample(IgniteClient client) {
        try (ResultSet<SqlRow> resultSet = client.sql().execute(null, "SELECT * FROM Customer")) {
            
            while (resultSet.hasNext()) {
                SqlRow row = resultSet.next();
                processRow(row);
            }
            
        } // ResultSet automatically closed
    }
    
    // Pattern 2: Exception-safe resource handling
    public void exceptionSafeResourceHandling() {
        IgniteClient client = null;
        try {
            client = createClient();
            performCriticalOperations(client);
            
        } catch (Exception e) {
            System.err.println("Operation failed: " + e.getMessage());
            // Handle specific exceptions
            
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (Exception e) {
                    System.err.println("Failed to close client: " + e.getMessage());
                }
            }
        }
    }
    
    // Pattern 3: Async resource management
    public CompletableFuture<Void> asyncResourceManagement() {
        return CompletableFuture.supplyAsync(() -> {
            try (IgniteClient client = createClient()) {
                return performAsyncOperations(client);
            }
        }).thenCompose(Function.identity())
         .exceptionally(throwable -> {
             System.err.println("Async operation failed: " + throwable.getMessage());
             return null;
         });
    }
    
    private IgniteClient createClient() {
        return IgniteClient.builder().addresses("localhost:10800").build();
    }
    
    private void processRow(SqlRow row) {
        // Implementation...
    }
    
    private void performCriticalOperations(IgniteClient client) {
        // Implementation...
    }
    
    private CompletableFuture<Void> performAsyncOperations(IgniteClient client) {
        return CompletableFuture.completedFuture(null);
    }
}
```

### Transaction Boundaries

#### Transaction Scope Management

```java
public class TransactionBoundaryPatterns {
    
    // ✅ GOOD: Explicit transaction boundaries
    public void explicitTransactionBoundaries(IgniteClient client) {
        try (Transaction tx = client.transactions().begin()) {
            
            // All operations within this block are transactional
            RecordView<Customer> customerView = client.tables().table("Customer").recordView(Customer.class);
            RecordView<Order> orderView = client.tables().table("Order").recordView(Order.class);
            
            // Create customer
            Customer customer = new Customer(1L, "John", "Doe", "john.doe@example.com");
            customerView.upsert(tx, customer);
            
            // Create order for customer
            Order order = new Order(1L, 1L, LocalDateTime.now(), "PENDING");
            orderView.upsert(tx, order);
            
            // Explicitly commit
            tx.commit();
            
        } catch (Exception e) {
            // Transaction automatically rolled back if not committed
            System.err.println("Transaction failed: " + e.getMessage());
        }
    }
    
    // ✅ BEST: Functional transaction API
    public void functionalTransactionAPI(IgniteClient client) {
        try {
            client.transactions().runInTransaction(tx -> {
                RecordView<Customer> customerView = client.tables().table("Customer").recordView(Customer.class);
                RecordView<Order> orderView = client.tables().table("Order").recordView(Order.class);
                
                Customer customer = new Customer(2L, "Jane", "Smith", "jane.smith@example.com");
                customerView.upsert(tx, customer);
                
                Order order = new Order(2L, 2L, LocalDateTime.now(), "PROCESSING");
                orderView.upsert(tx, order);
                
                // Automatic commit on successful completion
            });
            
        } catch (Exception e) {
            System.err.println("Transaction failed and was rolled back: " + e.getMessage());
        }
    }
    
    // ✅ ADVANCED: Async transaction with proper error handling
    public CompletableFuture<Void> asyncTransactionPattern(IgniteClient client) {
        return client.transactions().runInTransactionAsync(tx -> {
            RecordView<Customer> customerView = client.tables().table("Customer").recordView(Customer.class);
            
            Customer customer = new Customer(3L, "Bob", "Johnson", "bob.johnson@example.com");
            
            return customerView.upsertAsync(tx, customer)
                .thenCompose(v -> {
                    // Additional operations...
                    return CompletableFuture.completedFuture(null);
                });
        }).exceptionally(throwable -> {
            System.err.println("Async transaction failed: " + throwable.getMessage());
            return null;
        });
    }
    
    // ❌ BAD: Transaction leaks
    public void badTransactionHandling(IgniteClient client) {
        Transaction tx = client.transactions().begin();
        
        try {
            // Operations...
            // Forgot to commit or rollback - transaction leak!
        } catch (Exception e) {
            // Exception path also doesn't clean up transaction
        }
        // Transaction never closed
    }
    
    // ✅ GOOD: Retry logic with transaction boundaries
    public void transactionWithRetry(IgniteClient client) {
        int maxRetries = 3;
        int attempt = 0;
        
        while (attempt < maxRetries) {
            try {
                client.transactions().runInTransaction(tx -> {
                    // Transactional operations
                    performTransactionalOperations(client, tx);
                });
                
                // Success - break out of retry loop
                break;
                
            } catch (TransactionException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    throw new RuntimeException("Transaction failed after " + maxRetries + " attempts", e);
                }
                
                // Wait before retry
                try {
                    Thread.sleep(100 * attempt); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
    }
    
    private void performTransactionalOperations(IgniteClient client, Transaction tx) {
        // Implementation...
    }
}
```

## Error Handling Strategies

### Comprehensive Error Classification

```java
public class ErrorHandlingStrategies {
    
    // Error classification and handling
    public <T> T handleIgniteOperation(Supplier<T> operation) {
        try {
            return operation.get();
            
        } catch (IgniteClientConnectionException e) {
            // Connection issues - usually transient
            System.err.println("Connection error (retryable): " + e.getMessage());
            throw new RetryableException("Connection failed", e);
            
        } catch (TableNotFoundException e) {
            // Schema issue - usually permanent
            System.err.println("Schema error (non-retryable): " + e.getMessage());
            throw new NonRetryableException("Table not found", e);
            
        } catch (TransactionException e) {
            // Transaction conflict - potentially retryable
            System.err.println("Transaction error (potentially retryable): " + e.getMessage());
            if (isRetryableTransactionError(e)) {
                throw new RetryableException("Transaction conflict", e);
            } else {
                throw new NonRetryableException("Transaction failed permanently", e);
            }
            
        } catch (SqlException e) {
            // SQL error - usually non-retryable
            System.err.println("SQL error (non-retryable): " + e.getMessage());
            throw new NonRetryableException("SQL execution failed", e);
            
        } catch (IgniteException e) {
            // General Ignite error
            System.err.println("General Ignite error: " + e.getMessage());
            analyzeAndClassifyError(e);
            throw new RuntimeException("Ignite operation failed", e);
            
        } catch (TimeoutException e) {
            // Timeout - usually retryable
            System.err.println("Timeout error (retryable): " + e.getMessage());
            throw new RetryableException("Operation timed out", e);
            
        } catch (Exception e) {
            // Unexpected error
            System.err.println("Unexpected error: " + e.getMessage());
            throw new RuntimeException("Unexpected failure", e);
        }
    }
    
    // Smart error classification
    private boolean isRetryableTransactionError(TransactionException e) {
        String message = e.getMessage().toLowerCase();
        return message.contains("conflict") || 
               message.contains("deadlock") ||
               message.contains("timeout");
    }
    
    private void analyzeAndClassifyError(IgniteException e) {
        // Analyze error details to determine if retryable
        String message = e.getMessage();
        
        if (message.contains("connection") || message.contains("network")) {
            System.err.println("Classified as network error - retryable");
        } else if (message.contains("permission") || message.contains("auth")) {
            System.err.println("Classified as authentication error - non-retryable");
        } else {
            System.err.println("Classified as unknown error - non-retryable by default");
        }
    }
    
    // Custom exception types for classification
    public static class RetryableException extends RuntimeException {
        public RetryableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class NonRetryableException extends RuntimeException {
        public NonRetryableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
```

### Graceful Degradation Patterns

```java
public class GracefulDegradationPatterns {
    
    // Fallback to cache when Ignite is unavailable
    public class CustomerServiceWithFallback {
        private final IgniteClient igniteClient;
        private final Map<Long, Customer> localCache = new ConcurrentHashMap<>();
        private final AtomicBoolean igniteAvailable = new AtomicBoolean(true);
        
        public CustomerServiceWithFallback(IgniteClient igniteClient) {
            this.igniteClient = igniteClient;
        }
        
        public Customer getCustomer(Long id) {
            if (igniteAvailable.get()) {
                try {
                    Customer customer = getFromIgnite(id);
                    localCache.put(id, customer); // Update cache on success
                    return customer;
                    
                } catch (Exception e) {
                    System.err.println("Ignite unavailable, falling back to cache: " + e.getMessage());
                    igniteAvailable.set(false);
                    scheduleHealthCheck();
                }
            }
            
            // Fallback to local cache
            Customer cached = localCache.get(id);
            if (cached != null) {
                System.out.println("Serving from fallback cache for customer: " + id);
                return cached;
            }
            
            throw new RuntimeException("Customer not found and Ignite unavailable");
        }
        
        private Customer getFromIgnite(Long id) {
            RecordView<Customer> view = igniteClient.tables().table("Customer").recordView(Customer.class);
            Customer key = new Customer();
            key.setId(id);
            return view.get(null, key);
        }
        
        private void scheduleHealthCheck() {
            CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS).execute(() -> {
                try {
                    // Simple health check
                    igniteClient.sql().execute(null, "SELECT 1").next();
                    igniteAvailable.set(true);
                    System.out.println("Ignite health restored");
                } catch (Exception e) {
                    System.out.println("Ignite still unavailable, will retry later");
                    scheduleHealthCheck();
                }
            });
        }
    }
    
    // Circuit breaker with fallback
    public class CircuitBreakerWithFallback {
        private final IgniteClient igniteClient;
        private volatile boolean circuitOpen = false;
        private volatile long lastFailureTime = 0;
        private volatile int consecutiveFailures = 0;
        
        private final int failureThreshold = 5;
        private final long timeoutMs = 60000; // 1 minute
        
        public CircuitBreakerWithFallback(IgniteClient igniteClient) {
            this.igniteClient = igniteClient;
        }
        
        public List<Customer> getCustomers(String country) {
            if (circuitOpen && (System.currentTimeMillis() - lastFailureTime) < timeoutMs) {
                return getFallbackCustomers(country);
            }
            
            try {
                List<Customer> customers = getCustomersFromIgnite(country);
                onSuccess();
                return customers;
                
            } catch (Exception e) {
                onFailure();
                return getFallbackCustomers(country);
            }
        }
        
        private List<Customer> getCustomersFromIgnite(String country) {
            try (ResultSet<Customer> resultSet = igniteClient.sql().execute(
                    null, Mapper.of(Customer.class), 
                    "SELECT * FROM Customer WHERE country = ?", country)) {
                
                List<Customer> customers = new ArrayList<>();
                while (resultSet.hasNext()) {
                    customers.add(resultSet.next());
                }
                return customers;
            }
        }
        
        private List<Customer> getFallbackCustomers(String country) {
            System.out.println("Using fallback data for country: " + country);
            // Return cached or default data
            return Arrays.asList(
                new Customer(0L, "Fallback", "Customer", "fallback@example.com")
            );
        }
        
        private void onSuccess() {
            consecutiveFailures = 0;
            circuitOpen = false;
        }
        
        private void onFailure() {
            consecutiveFailures++;
            lastFailureTime = System.currentTimeMillis();
            
            if (consecutiveFailures >= failureThreshold) {
                circuitOpen = true;
                System.out.println("Circuit breaker opened after " + consecutiveFailures + " failures");
            }
        }
    }
}
```

## Performance Guidelines

### Optimal Query Patterns

```java
public class PerformanceGuidelines {
    
    // ✅ GOOD: Use prepared statements for repeated queries
    public class OptimalQueryPatterns {
        private final IgniteClient client;
        private final Statement customerByCountryStmt;
        
        public OptimalQueryPatterns(IgniteClient client) {
            this.client = client;
            // Prepare statement once
            this.customerByCountryStmt = client.sql().statementBuilder()
                .query("SELECT * FROM Customer WHERE country = ?")
                .build();
        }
        
        public List<Customer> getCustomersByCountry(String country) {
            // Reuse prepared statement
            try (ResultSet<Customer> resultSet = client.sql().execute(
                    null, customerByCountryStmt, Mapper.of(Customer.class), country)) {
                
                List<Customer> customers = new ArrayList<>();
                while (resultSet.hasNext()) {
                    customers.add(resultSet.next());
                }
                return customers;
            }
        }
    }
    
    // ✅ GOOD: Batch operations for bulk data
    public void efficientBatchOperations(IgniteClient client, List<Customer> customers) {
        RecordView<Customer> view = client.tables().table("Customer").recordView(Customer.class);
        
        // Process in optimal batch sizes
        int batchSize = 1000;
        for (int i = 0; i < customers.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, customers.size());
            List<Customer> batch = customers.subList(i, endIndex);
            
            // Single batch operation is much faster than individual operations
            view.upsertAll(null, batch);
        }
    }
    
    // ✅ GOOD: Async operations for non-blocking performance
    public CompletableFuture<Map<String, List<Customer>>> getCustomersByCountriesAsync(IgniteClient client, 
                                                                                        List<String> countries) {
        List<CompletableFuture<Map.Entry<String, List<Customer>>>> futures = countries.stream()
            .map(country -> 
                client.sql().executeAsync(null, Mapper.of(Customer.class), 
                    "SELECT * FROM Customer WHERE country = ?", country)
                .thenApply(resultSet -> {
                    List<Customer> customers = new ArrayList<>();
                    while (resultSet.hasNext()) {
                        customers.add(resultSet.next());
                    }
                    return Map.entry(country, customers);
                })
            )
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }
    
    // ❌ BAD: N+1 query problem
    public void badQueryPattern(IgniteClient client, List<Long> customerIds) {
        RecordView<Customer> view = client.tables().table("Customer").recordView(Customer.class);
        
        for (Long id : customerIds) {
            Customer key = new Customer();
            key.setId(id);
            Customer customer = view.get(null, key); // Individual query for each ID
            processCustomer(customer);
        }
    }
    
    // ✅ GOOD: Single query to get all data
    public void goodQueryPattern(IgniteClient client, List<Long> customerIds) {
        String placeholders = customerIds.stream()
            .map(id -> "?")
            .collect(Collectors.joining(","));
        
        String sql = "SELECT * FROM Customer WHERE id IN (" + placeholders + ")";
        
        try (ResultSet<Customer> resultSet = client.sql().execute(
                null, Mapper.of(Customer.class), sql, customerIds.toArray())) {
            
            while (resultSet.hasNext()) {
                Customer customer = resultSet.next();
                processCustomer(customer);
            }
        }
    }
    
    // ✅ GOOD: Connection reuse pattern
    public class ConnectionReusePattern {
        private final IgniteClient client;
        
        public ConnectionReusePattern() {
            // Create client once and reuse
            this.client = IgniteClient.builder()
                .addresses("localhost:10800")
                .build();
        }
        
        public void performMultipleOperations() {
            // Reuse the same client for multiple operations
            RecordView<Customer> customerView = client.tables().table("Customer").recordView(Customer.class);
            RecordView<Order> orderView = client.tables().table("Order").recordView(Order.class);
            
            // Multiple operations with same client
            List<Customer> customers = getCustomers();
            List<Order> orders = getOrders();
            
            // Process data...
        }
        
        private List<Customer> getCustomers() {
            try (ResultSet<Customer> resultSet = client.sql().execute(
                    null, Mapper.of(Customer.class), "SELECT * FROM Customer LIMIT 100")) {
                
                List<Customer> customers = new ArrayList<>();
                while (resultSet.hasNext()) {
                    customers.add(resultSet.next());
                }
                return customers;
            }
        }
        
        private List<Order> getOrders() {
            try (ResultSet<Order> resultSet = client.sql().execute(
                    null, Mapper.of(Order.class), "SELECT * FROM Order LIMIT 100")) {
                
                List<Order> orders = new ArrayList<>();
                while (resultSet.hasNext()) {
                    orders.add(resultSet.next());
                }
                return orders;
            }
        }
        
        public void close() {
            try {
                client.close();
            } catch (Exception e) {
                System.err.println("Error closing client: " + e.getMessage());
            }
        }
    }
    
    private void processCustomer(Customer customer) {
        // Implementation...
    }
}
```

### Memory Management Best Practices

```java
public class MemoryManagementBestPractices {
    
    // ✅ GOOD: Stream large result sets
    public void streamLargeResultSets(IgniteClient client) {
        try (ResultSet<Customer> resultSet = client.sql().execute(
                null, Mapper.of(Customer.class), "SELECT * FROM Customer")) {
            
            // Process results as stream to avoid loading all into memory
            int batchSize = 0;
            while (resultSet.hasNext()) {
                Customer customer = resultSet.next();
                processCustomer(customer);
                
                batchSize++;
                if (batchSize % 1000 == 0) {
                    System.out.println("Processed " + batchSize + " customers");
                    // Optional: trigger GC for large datasets
                    System.gc();
                }
            }
        }
    }
    
    // ✅ GOOD: Limit query results
    public List<Customer> getRecentCustomers(IgniteClient client, int limit) {
        try (ResultSet<Customer> resultSet = client.sql().execute(
                null, Mapper.of(Customer.class), 
                "SELECT * FROM Customer ORDER BY createdAt DESC LIMIT ?", limit)) {
            
            List<Customer> customers = new ArrayList<>(limit);
            while (resultSet.hasNext()) {
                customers.add(resultSet.next());
            }
            return customers;
        }
    }
    
    // ✅ GOOD: Use pagination for large datasets
    public List<Customer> getCustomersPage(IgniteClient client, int page, int pageSize) {
        int offset = page * pageSize;
        
        try (ResultSet<Customer> resultSet = client.sql().execute(
                null, Mapper.of(Customer.class), 
                "SELECT * FROM Customer ORDER BY id LIMIT ? OFFSET ?", pageSize, offset)) {
            
            List<Customer> customers = new ArrayList<>(pageSize);
            while (resultSet.hasNext()) {
                customers.add(resultSet.next());
            }
            return customers;
        }
    }
    
    private void processCustomer(Customer customer) {
        // Implementation...
    }
}
```

## Testing Strategies

### Unit Testing Patterns

```java
public class TestingStrategies {
    
    // Test configuration
    @TestConfiguration
    public static class TestIgniteConfiguration {
        
        @Bean
        @Primary
        public IgniteClient testIgniteClient() {
            // Use embedded or test cluster
            return IgniteClient.builder()
                .addresses("localhost:10800")
                .connectTimeout(1000)
                .operationTimeout(5000)
                .build();
        }
    }
    
    // Unit test example
    @ExtendWith(MockitoExtension.class)
    public class CustomerServiceTest {
        
        @Mock
        private IgniteClient mockClient;
        
        @Mock
        private Tables mockTables;
        
        @Mock
        private Table mockTable;
        
        @Mock
        private RecordView<Customer> mockRecordView;
        
        @InjectMocks
        private CustomerService customerService;
        
        @BeforeEach
        void setUp() {
            when(mockClient.tables()).thenReturn(mockTables);
            when(mockTables.table("Customer")).thenReturn(mockTable);
            when(mockTable.recordView(Customer.class)).thenReturn(mockRecordView);
        }
        
        @Test
        void testGetCustomer_Success() {
            // Given
            Long customerId = 1L;
            Customer expectedCustomer = new Customer(customerId, "John", "Doe", "john.doe@example.com");
            
            Customer keyMatcher = argThat(key -> customerId.equals(key.getId()));
            when(mockRecordView.get(eq(null), keyMatcher)).thenReturn(expectedCustomer);
            
            // When
            Customer result = customerService.getCustomer(customerId);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(customerId);
            assertThat(result.getFirstName()).isEqualTo("John");
        }
        
        @Test
        void testGetCustomer_NotFound() {
            // Given
            Long customerId = 999L;
            when(mockRecordView.get(any(), any())).thenReturn(null);
            
            // When & Then
            assertThrows(EntityNotFoundException.class, () -> {
                customerService.getCustomer(customerId);
            });
        }
        
        @Test
        void testCreateCustomer_EmailAlreadyExists() {
            // Given
            Customer newCustomer = new Customer(null, "Jane", "Smith", "existing@example.com");
            
            // Mock SQL query for email check
            IgniteSql mockSql = mock(IgniteSql.class);
            ResultSet<Customer> mockResultSet = mock(ResultSet.class);
            
            when(mockClient.sql()).thenReturn(mockSql);
            when(mockSql.execute(any(), any(Mapper.class), anyString(), any()))
                .thenReturn(mockResultSet);
            when(mockResultSet.hasNext()).thenReturn(true);
            
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                customerService.createCustomer(newCustomer);
            });
        }
    }
    
    // Integration test example
    @SpringBootTest
    @Testcontainers
    public class CustomerServiceIntegrationTest {
        
        @Container
        static IgniteContainer igniteContainer = new IgniteContainer("apacheignite/ignite:3.0.0")
            .withExposedPorts(10300, 10800)
            .withEnv("IGNITE_CLUSTER_NAME", "test-cluster");
        
        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {
            registry.add("ignite.addresses", () -> 
                "localhost:" + igniteContainer.getMappedPort(10800));
        }
        
        @Autowired
        private CustomerService customerService;
        
        @Autowired
        private IgniteClient igniteClient;
        
        @BeforeEach
        void setUp() {
            // Initialize schema
            initializeTestSchema();
        }
        
        @Test
        void testCustomerCRUD() {
            // Create
            Customer customer = new Customer(null, "Test", "User", "test@example.com");
            Customer created = customerService.createCustomer(customer);
            
            assertThat(created.getId()).isNotNull();
            assertThat(created.getFirstName()).isEqualTo("Test");
            
            // Read
            Optional<Customer> retrieved = customerService.getCustomer(created.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getEmail()).isEqualTo("test@example.com");
            
            // Update
            retrieved.get().setEmail("updated@example.com");
            Customer updated = customerService.updateCustomer(created.getId(), retrieved.get());
            assertThat(updated.getEmail()).isEqualTo("updated@example.com");
            
            // Delete
            customerService.deleteCustomer(created.getId());
            Optional<Customer> deleted = customerService.getCustomer(created.getId());
            assertThat(deleted).isEmpty();
        }
        
        private void initializeTestSchema() {
            try {
                // Create test tables
                igniteClient.catalog().createTable(Customer.class);
            } catch (Exception e) {
                // Table might already exist
                System.out.println("Test schema initialization: " + e.getMessage());
            }
        }
    }
    
    // Performance test example
    public class PerformanceTest {
        
        @Test
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void testBulkInsertPerformance() {
            // Given
            List<Customer> customers = generateTestCustomers(1000);
            
            // When
            long startTime = System.currentTimeMillis();
            customerService.importCustomers(customers);
            long duration = System.currentTimeMillis() - startTime;
            
            // Then
            assertThat(duration).isLessThan(5000); // Should complete in less than 5 seconds
            
            // Verify all customers were inserted
            long count = customerService.countCustomers();
            assertThat(count).isGreaterThanOrEqualTo(1000);
        }
        
        private List<Customer> generateTestCustomers(int count) {
            List<Customer> customers = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                customers.add(new Customer(
                    null,
                    "First" + i,
                    "Last" + i,
                    "test" + i + "@example.com"
                ));
            }
            return customers;
        }
    }
    
    // Testcontainers custom configuration
    public static class IgniteContainer extends GenericContainer<IgniteContainer> {
        public IgniteContainer(String dockerImageName) {
            super(dockerImageName);
        }
        
        @Override
        protected void configure() {
            super.configure();
            
            // Wait for Ignite to be ready
            waitingFor(Wait.forLogMessage(".*Ignite node started successfully.*", 1)
                .withStartupTimeout(Duration.ofMinutes(2)));
        }
        
        public void initializeCluster() {
            try {
                ExecResult result = execInContainer(
                    "./bin/ignite", "cluster", "init", 
                    "--name=test-cluster",
                    "--metastorage-group=default"
                );
                
                System.out.println("Cluster initialization: " + result.getStdout());
            } catch (Exception e) {
                System.err.println("Failed to initialize cluster: " + e.getMessage());
            }
        }
    }
    
    // Mock data builders for testing
    public static class CustomerTestDataBuilder {
        private Long id;
        private String firstName = "Test";
        private String lastName = "Customer";
        private String email = "test@example.com";
        
        public static CustomerTestDataBuilder aCustomer() {
            return new CustomerTestDataBuilder();
        }
        
        public CustomerTestDataBuilder withId(Long id) {
            this.id = id;
            return this;
        }
        
        public CustomerTestDataBuilder withName(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
            return this;
        }
        
        public CustomerTestDataBuilder withEmail(String email) {
            this.email = email;
            return this;
        }
        
        public Customer build() {
            return new Customer(id, firstName, lastName, email);
        }
    }
}
```

### Test Data Management

```java
public class TestDataManagement {
    
    // Test data cleanup
    @Component
    public static class TestDataCleanup {
        private final IgniteClient igniteClient;
        
        public TestDataCleanup(IgniteClient igniteClient) {
            this.igniteClient = igniteClient;
        }
        
        public void cleanupTestData() {
            try {
                // Clean up test tables
                igniteClient.sql().execute(null, "DELETE FROM Customer WHERE email LIKE 'test%'");
                igniteClient.sql().execute(null, "DELETE FROM Order WHERE id < 0"); // Negative IDs for test data
                
                System.out.println("Test data cleanup completed");
            } catch (Exception e) {
                System.err.println("Failed to cleanup test data: " + e.getMessage());
            }
        }
    }
    
    // Test data factory
    public static class TestDataFactory {
        private static final Random random = new Random();
        
        public static Customer createRandomCustomer() {
            return new Customer(
                null,
                "FirstName" + random.nextInt(1000),
                "LastName" + random.nextInt(1000),
                "test" + random.nextInt(10000) + "@example.com"
            );
        }
        
        public static List<Customer> createCustomers(int count) {
            return IntStream.range(0, count)
                .mapToObj(i -> createRandomCustomer())
                .collect(Collectors.toList());
        }
        
        public static Customer createCustomerWithCountry(String country) {
            Customer customer = createRandomCustomer();
            customer.setCountry(country);
            return customer;
        }
    }
}
```

## Caching Best Practices

### Cache Key Design Patterns

```java
public class CacheKeyDesignPatterns {
    
    // ✅ GOOD: Consistent key naming convention
    public static class KeyGenerator {
        private static final String SEPARATOR = ":";
        
        public static String userKey(Long userId) {
            return "user" + SEPARATOR + userId;
        }
        
        public static String sessionKey(String sessionId) {
            return "session" + SEPARATOR + sessionId;
        }
        
        public static String productKey(Long productId) {
            return "product" + SEPARATOR + productId;
        }
        
        // Pattern for composite keys
        public static String orderItemKey(Long orderId, Long itemId) {
            return "order" + SEPARATOR + orderId + SEPARATOR + "item" + SEPARATOR + itemId;
        }
        
        // Pattern for filtered data
        public static String customersByCountryKey(String country) {
            return "customers" + SEPARATOR + "country" + SEPARATOR + country.toLowerCase();
        }
    }
    
    // ✅ GOOD: Key validation and sanitization
    public static class KeyValidator {
        private static final int MAX_KEY_LENGTH = 250;
        private static final Pattern INVALID_CHARS = Pattern.compile("[\\s\\n\\r\\t]");
        
        public static String validateAndSanitizeKey(String key) {
            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException("Cache key cannot be null or empty");
            }
            
            // Remove invalid characters
            String sanitized = INVALID_CHARS.matcher(key).replaceAll("_");
            
            if (sanitized.length() > MAX_KEY_LENGTH) {
                // Hash long keys to keep them manageable
                return "long_key_" + Integer.toHexString(sanitized.hashCode());
            }
            
            return sanitized;
        }
    }
}
```

### TTL and Expiration Strategies

```java
public class TTLExpirationStrategies {
    
    // ✅ GOOD: TTL based on data characteristics
    public enum CacheTTLStrategy {
        STATIC_DATA(Duration.ofHours(24)),        // Configuration, reference data
        USER_DATA(Duration.ofHours(6)),           // User profiles, preferences
        SESSION_DATA(Duration.ofMinutes(30)),     // Session information
        COMPUTED_DATA(Duration.ofHours(2)),       // Expensive calculations
        REAL_TIME_DATA(Duration.ofMinutes(5)),    // Frequently changing data
        TEMPORARY_DATA(Duration.ofMinutes(1));    // Short-lived cache
        
        private final Duration ttl;
        
        CacheTTLStrategy(Duration ttl) {
            this.ttl = ttl;
        }
        
        public Duration getTtl() {
            return ttl;
        }
    }
    
    // Smart TTL calculation based on access patterns
    public static class SmartTTLCalculator {
        
        public Duration calculateTTL(String dataType, long accessFrequency, LocalDateTime lastUpdate) {
            Duration baseTTL = getBaseTTL(dataType);
            
            // Adjust TTL based on access frequency
            if (accessFrequency > 100) { // High frequency
                return baseTTL.multipliedBy(2); // Keep longer
            } else if (accessFrequency < 10) { // Low frequency
                return baseTTL.dividedBy(2); // Shorter TTL
            }
            
            // Adjust based on data age
            Duration dataAge = Duration.between(lastUpdate, LocalDateTime.now());
            if (dataAge.compareTo(Duration.ofDays(1)) > 0) {
                return baseTTL.dividedBy(3); // Old data expires faster
            }
            
            return baseTTL;
        }
        
        private Duration getBaseTTL(String dataType) {
            return switch (dataType.toLowerCase()) {
                case "user" -> CacheTTLStrategy.USER_DATA.getTtl();
                case "session" -> CacheTTLStrategy.SESSION_DATA.getTtl();
                case "config" -> CacheTTLStrategy.STATIC_DATA.getTtl();
                case "computed" -> CacheTTLStrategy.COMPUTED_DATA.getTtl();
                default -> CacheTTLStrategy.TEMPORARY_DATA.getTtl();
            };
        }
    }
}
```

### Cache Monitoring and Error Handling

```java
public class CacheMonitoringAndErrorHandling {
    
    // Comprehensive cache monitoring
    public static class CacheHealthMonitor {
        private final DistributedCache<?, ?> cache;
        private final String cacheName;
        
        public CacheHealthMonitor(DistributedCache<?, ?> cache, String cacheName) {
            this.cache = cache;
            this.cacheName = cacheName;
        }
        
        public void generateHealthReport() {
            CacheStats stats = cache.getStats();
            
            System.out.println("=== Cache Health Report: " + cacheName + " ===");
            System.out.printf("Hit Rate: %.2f%%\n", stats.getHitRate() * 100);
            System.out.printf("Total Operations: %d\n", stats.getHitCount() + stats.getMissCount());
            System.out.printf("Cache Size: %d\n", stats.getSize());
            
            // Health recommendations
            if (stats.getHitRate() < 0.7) {
                System.out.println("⚠️  Low hit rate detected! Consider:");
                System.out.println("   - Reviewing TTL settings");
                System.out.println("   - Implementing cache warming");
                System.out.println("   - Analyzing access patterns");
            }
        }
    }
    
    // Resilient cache operations
    public static class ResilientCacheOperations {
        private final DistributedCache<String, Object> cache;
        private final Map<String, Object> fallbackCache = new ConcurrentHashMap<>();
        
        public ResilientCacheOperations(DistributedCache<String, Object> cache) {
            this.cache = cache;
        }
        
        public <T> T safeGet(String key, Class<T> type, Supplier<T> fallbackLoader) {
            try {
                Object cached = cache.get(key);
                if (cached != null) {
                    fallbackCache.put(key, cached);
                    return type.cast(cached);
                }
            } catch (Exception e) {
                System.err.println("Primary cache read failed for key " + key + ": " + e.getMessage());
            }
            
            // Try fallback cache
            Object fallbackValue = fallbackCache.get(key);
            if (fallbackValue != null) {
                return type.cast(fallbackValue);
            }
            
            // Load fresh data as last resort
            if (fallbackLoader != null) {
                try {
                    T freshValue = fallbackLoader.get();
                    safePut(key, freshValue, Duration.ofMinutes(5));
                    return freshValue;
                } catch (Exception e) {
                    System.err.println("Fallback loader failed: " + e.getMessage());
                }
            }
            
            return null;
        }
        
        public void safePut(String key, Object value, Duration ttl) {
            try {
                cache.put(key, value, ttl);
                fallbackCache.put(key, value);
            } catch (Exception e) {
                System.err.println("Cache put failed for key " + key + ": " + e.getMessage());
                fallbackCache.put(key, value);
            }
        }
    }
}
```
