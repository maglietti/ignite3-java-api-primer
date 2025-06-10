# 14. Troubleshooting Guide

This troubleshooting guide covers common issues, configuration problems, performance bottlenecks, and debugging techniques for Apache Ignite 3 Java applications.

## Common Issues

### Connection Problems

#### Issue: "Connection refused" or "Unable to connect"

**Symptoms:**

```text
IgniteClientConnectionException: Connection refused: localhost/127.0.0.1:10800
```

**Common Causes & Solutions:**

1. **Ignite cluster not running**

   ```bash
   # Check if Ignite is running
   docker ps | grep ignite
   
   # Start Ignite cluster
   docker run -d --name ignite-node \
     -p 10300:10300 -p 10800:10800 \
     apacheignite/ignite:3.0.0
   ```

2. **Wrong port configuration**

   ```java
   // ❌ Wrong port
   IgniteClient client = IgniteClient.builder()
       .addresses("localhost:47500")  // This is Ignite 2.x port
       .build();
   
   // ✅ Correct port for Ignite 3
   IgniteClient client = IgniteClient.builder()
       .addresses("localhost:10800")  // Ignite 3 thin client port
       .build();
   ```

3. **Firewall blocking connection**

   ```bash
   # Test connectivity
   telnet localhost 10800
   
   # Check if port is listening
   netstat -an | grep 10800
   ```

4. **Network interface binding**

   ```bash
   # Check Ignite network configuration
   # Ensure Ignite is listening on the correct interface
   # Update ignite-config.xml if needed
   ```

#### Issue: "Authentication failed"

**Symptoms:**

```
IgniteClientAuthenticationException: Authentication failed
```

**Solutions:**

1. **Check credentials**

   ```java
   // Ensure correct username/password
   IgniteClient client = IgniteClient.builder()
       .addresses("localhost:10800")
       .authenticator(BasicAuthenticator.builder()
           .username("ignite")     // Check actual username
           .password("ignite")     // Check actual password
           .build())
       .build();
   ```

2. **Authentication not enabled on server**

   ```bash
   # Check if authentication is enabled in Ignite server configuration
   # If authentication is disabled, don't provide authenticator
   ```

#### Issue: "SSL/TLS connection errors"

**Symptoms:**

```
SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed
```

**Solutions:**

1. **Certificate validation issues**

   ```java
   // For development only - disable certificate validation
   SslConfiguration sslConfig = SslConfiguration.builder()
       .enabled(true)
       .trustAll(true)  // ⚠️ Only for development!
       .build();
   ```

2. **Proper certificate configuration**

   ```java
   // Production SSL configuration
   SslConfiguration sslConfig = SslConfiguration.builder()
       .enabled(true)
       .keyStorePath("/path/to/client-keystore.jks")
       .keyStorePassword("keystore-password")
       .trustStorePath("/path/to/truststore.jks")
       .trustStorePassword("truststore-password")
       .build();
   ```

### Schema-Related Issues

#### Issue: "Table does not exist"

**Symptoms:**

```
TableNotFoundException: Table 'Customer' does not exist
```

**Diagnostic Steps:**

```java
public class SchemaDiagnostics {
    
    public static void diagnoseSchemaIssues(IgniteClient client) {
        System.out.println("=== Schema Diagnostics ===");
        
        // List all available tables
        Collection<String> tables = client.catalog().tables();
        System.out.println("Available tables: " + tables);
        
        // List all zones
        Collection<String> zones = client.catalog().zones();
        System.out.println("Available zones: " + zones);
        
        // Check specific table
        String tableName = "Customer";
        if (tables.contains(tableName)) {
            try {
                TableDefinition tableDef = client.catalog().tableDefinition(tableName);
                System.out.println("Table " + tableName + " details:");
                System.out.println("  Zone: " + tableDef.zone());
                System.out.println("  Columns: " + tableDef.columns().size());
                System.out.println("  Primary Key: " + tableDef.primaryKey());
            } catch (Exception e) {
                System.err.println("Error getting table definition: " + e.getMessage());
            }
        } else {
            System.out.println("Table " + tableName + " not found!");
            System.out.println("Available tables: " + String.join(", ", tables));
        }
    }
    
    // Create table if it doesn't exist
    public static void ensureTableExists(IgniteClient client, Class<?> tableClass) {
        String tableName = extractTableName(tableClass);
        
        try {
            if (!client.catalog().tables().contains(tableName)) {
                System.out.println("Creating missing table: " + tableName);
                client.catalog().createTable(tableClass);
                System.out.println("Table created successfully");
            } else {
                System.out.println("Table " + tableName + " already exists");
            }
        } catch (Exception e) {
            System.err.println("Failed to create table: " + e.getMessage());
        }
    }
    
    private static String extractTableName(Class<?> tableClass) {
        Table tableAnnotation = tableClass.getAnnotation(Table.class);
        if (tableAnnotation != null && !tableAnnotation.value().isEmpty()) {
            return tableAnnotation.value();
        }
        return tableClass.getSimpleName();
    }
}
```

#### Issue: "Zone does not exist"

**Symptoms:**

```
ZoneNotFoundException: Zone 'my_zone' does not exist
```

**Solutions:**

```java
public class ZoneTroubleshooting {
    
    public static void createZoneIfMissing(IgniteClient client, String zoneName) {
        try {
            Collection<String> zones = client.catalog().zones();
            
            if (!zones.contains(zoneName)) {
                System.out.println("Creating missing zone: " + zoneName);
                
                ZoneDefinition zoneDef = ZoneDefinition.builder(zoneName)
                    .ifNotExists()
                    .partitions(4)
                    .replicas(1)
                    .storageProfiles("default")
                    .build();
                
                client.catalog().createZone(zoneDef);
                System.out.println("Zone created successfully");
            } else {
                System.out.println("Zone " + zoneName + " already exists");
            }
        } catch (Exception e) {
            System.err.println("Failed to create zone: " + e.getMessage());
        }
    }
}
```

### Transaction Issues

#### Issue: "Transaction timeout"

**Symptoms:**

```
TransactionTimeoutException: Transaction timed out
```

**Solutions:**

```java
public class TransactionTroubleshooting {
    
    // Configure transaction timeout
    public static void configureTransactionTimeout(IgniteClient client) {
        TransactionOptions options = TransactionOptions.builder()
            .timeoutMs(30000)  // 30 second timeout
            .build();
        
        try (Transaction tx = client.transactions().begin(options)) {
            // Perform operations
            performTransactionalOperations(client, tx);
            tx.commit();
        }
    }
    
    // Break large transactions into smaller ones
    public static void handleLargeDatasetTransactions(IgniteClient client, List<Customer> customers) {
        int batchSize = 100;  // Smaller batches to avoid timeout
        
        for (int i = 0; i < customers.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, customers.size());
            List<Customer> batch = customers.subList(i, endIndex);
            
            try {
                client.transactions().runInTransaction(tx -> {
                    RecordView<Customer> view = client.tables().table("Customer").recordView(Customer.class);
                    view.upsertAll(tx, batch);
                });
                
                System.out.println("Processed batch " + (i / batchSize + 1));
                
            } catch (Exception e) {
                System.err.println("Batch failed: " + e.getMessage());
                // Could implement retry logic here
            }
        }
    }
    
    private static void performTransactionalOperations(IgniteClient client, Transaction tx) {
        // Implementation...
    }
}
```

#### Issue: "Transaction deadlock"

**Symptoms:**

```
TransactionException: Deadlock detected
```

**Solutions:**

```java
public class DeadlockPrevention {
    
    // Consistent ordering to prevent deadlocks
    public static void transferFunds(IgniteClient client, Long fromAccountId, Long toAccountId, BigDecimal amount) {
        // Always acquire locks in the same order (by ID) to prevent deadlocks
        Long firstId = Math.min(fromAccountId, toAccountId);
        Long secondId = Math.max(fromAccountId, toAccountId);
        
        try {
            client.transactions().runInTransaction(tx -> {
                RecordView<Account> accountView = client.tables().table("Account").recordView(Account.class);
                
                // Get accounts in consistent order
                Account firstAccount = getAccount(accountView, tx, firstId);
                Account secondAccount = getAccount(accountView, tx, secondId);
                
                // Determine which is from/to
                Account fromAccount = fromAccountId.equals(firstId) ? firstAccount : secondAccount;
                Account toAccount = fromAccountId.equals(firstId) ? secondAccount : firstAccount;
                
                // Perform transfer
                fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
                toAccount.setBalance(toAccount.getBalance().add(amount));
                
                accountView.upsert(tx, fromAccount);
                accountView.upsert(tx, toAccount);
            });
        } catch (Exception e) {
            System.err.println("Transfer failed: " + e.getMessage());
        }
    }
    
    private static Account getAccount(RecordView<Account> view, Transaction tx, Long id) {
        Account key = new Account();
        key.setId(id);
        return view.get(tx, key);
    }
}
```

## Configuration Problems

### Client Configuration Issues

#### Issue: "Connection timeout"

**Symptoms:**

```
IgniteClientConnectionException: Connection timeout
```

**Solutions:**

```java
public class ConnectionConfiguration {
    
    // Optimize connection timeouts
    public static IgniteClient createOptimizedClient() {
        return IgniteClient.builder()
            .addresses("localhost:10800")
            .connectTimeout(10000)         // 10 second connect timeout
            .operationTimeout(60000)       // 60 second operation timeout
            .heartbeatInterval(5000)       // 5 second heartbeat
            .heartbeatTimeout(3000)        // 3 second heartbeat timeout
            .retryPolicy(RetryLimitPolicy.builder()
                .retryLimit(3)
                .build())
            .build();
    }
    
    // Multiple addresses for failover
    public static IgniteClient createResilientClient() {
        return IgniteClient.builder()
            .addresses(
                "ignite-node1:10800",
                "ignite-node2:10800",
                "ignite-node3:10800"
            )
            .connectTimeout(5000)
            .build();
    }
}
```

#### Issue: "Memory configuration problems"

**Symptoms:**

```
OutOfMemoryError: Java heap space
```

**Solutions:**

```java
public class MemoryConfiguration {
    
    // Configure JVM for Ignite client
    public static void configureJVMForIgnite() {
        System.out.println("Recommended JVM settings for Ignite client:");
        System.out.println("-Xms1g -Xmx4g");  // Adjust based on your needs
        System.out.println("-XX:+UseG1GC");   // G1 garbage collector
        System.out.println("-XX:MaxGCPauseMillis=200");
        System.out.println("-XX:+PrintGCDetails -XX:+PrintGCTimeStamps");
    }
    
    // Process large result sets efficiently
    public static void processLargeResultSet(IgniteClient client) {
        try (ResultSet<Customer> resultSet = client.sql().execute(
                null, Mapper.of(Customer.class), "SELECT * FROM Customer")) {
            
            int batchSize = 0;
            List<Customer> batch = new ArrayList<>(1000);
            
            while (resultSet.hasNext()) {
                batch.add(resultSet.next());
                batchSize++;
                
                if (batchSize % 1000 == 0) {
                    processBatch(batch);
                    batch.clear();  // Clear to free memory
                    
                    // Optional: suggest GC
                    if (batchSize % 10000 == 0) {
                        System.gc();
                    }
                }
            }
            
            // Process remaining items
            if (!batch.isEmpty()) {
                processBatch(batch);
            }
        }
    }
    
    private static void processBatch(List<Customer> batch) {
        // Process batch
        System.out.println("Processed batch of " + batch.size() + " customers");
    }
}
```

### Network Configuration

#### Issue: "Network partitioning"

**Symptoms:**

```
IgniteClientConnectionException: Failed to connect to any of the provided addresses
```

**Diagnostic Tools:**

```java
public class NetworkDiagnostics {
    
    public static void diagnoseNetworkIssues(String[] addresses) {
        System.out.println("=== Network Diagnostics ===");
        
        for (String address : addresses) {
            try {
                String[] parts = address.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                
                // Test connectivity
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(host, port), 5000);
                    System.out.println("✅ " + address + " - Connection successful");
                } catch (IOException e) {
                    System.out.println("❌ " + address + " - Connection failed: " + e.getMessage());
                }
                
            } catch (Exception e) {
                System.out.println("❌ " + address + " - Invalid address format");
            }
        }
    }
    
    public static void testClientConnection() {
        String[] addresses = {"localhost:10800", "localhost:10801", "localhost:10802"};
        
        diagnoseNetworkIssues(addresses);
        
        // Test actual Ignite connection
        for (String address : addresses) {
            try (IgniteClient client = IgniteClient.builder()
                    .addresses(address)
                    .connectTimeout(2000)
                    .build()) {
                
                // Simple health check
                client.sql().execute(null, "SELECT 1").next();
                System.out.println("✅ Ignite connection successful to " + address);
                break;
                
            } catch (Exception e) {
                System.out.println("❌ Ignite connection failed to " + address + ": " + e.getMessage());
            }
        }
    }
}
```

## Performance Issues

### Query Performance Problems

#### Issue: "Slow query execution"

**Diagnostic Steps:**

```java
public class QueryPerformanceDiagnostics {
    
    // Query performance monitoring
    public static void monitorQueryPerformance(IgniteClient client, String sql, Object... params) {
        long startTime = System.currentTimeMillis();
        
        try (ResultSet<SqlRow> resultSet = client.sql().execute(null, sql, params)) {
            long queryTime = System.currentTimeMillis() - startTime;
            
            int rowCount = 0;
            while (resultSet.hasNext()) {
                resultSet.next();
                rowCount++;
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            
            System.out.println("=== Query Performance Report ===");
            System.out.println("SQL: " + sql);
            System.out.println("Query execution time: " + queryTime + "ms");
            System.out.println("Result processing time: " + (totalTime - queryTime) + "ms");
            System.out.println("Total time: " + totalTime + "ms");
            System.out.println("Rows returned: " + rowCount);
            System.out.println("Rows per second: " + (rowCount * 1000.0 / totalTime));
            
            if (totalTime > 1000) {
                System.out.println("⚠️ Slow query detected!");
                suggestOptimizations(sql, totalTime, rowCount);
            }
        }
    }
    
    private static void suggestOptimizations(String sql, long totalTime, int rowCount) {
        System.out.println("\n=== Performance Optimization Suggestions ===");
        
        if (sql.toLowerCase().contains("select *")) {
            System.out.println("- Consider selecting only needed columns instead of SELECT *");
        }
        
        if (!sql.toLowerCase().contains("limit") && rowCount > 1000) {
            System.out.println("- Consider adding LIMIT clause for large result sets");
        }
        
        if (sql.toLowerCase().contains("where")) {
            System.out.println("- Ensure indexes exist on WHERE clause columns");
        }
        
        if (sql.toLowerCase().contains("order by")) {
            System.out.println("- Consider creating composite index for ORDER BY columns");
        }
        
        if (totalTime > 5000) {
            System.out.println("- Consider breaking query into smaller batches");
            System.out.println("- Check if data can be pre-aggregated");
        }
    }
    
    // Index analysis
    public static void analyzeTableIndexes(IgniteClient client, String tableName) {
        try {
            TableDefinition tableDef = client.catalog().tableDefinition(tableName);
            
            System.out.println("=== Index Analysis for " + tableName + " ===");
            System.out.println("Indexes:");
            
            if (tableDef.indexes().isEmpty()) {
                System.out.println("⚠️ No indexes found! Consider adding indexes for frequently queried columns.");
            } else {
                for (IndexDefinition index : tableDef.indexes()) {
                    System.out.println("- " + index.name() + ": " + index.columns());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Failed to analyze indexes: " + e.getMessage());
        }
    }
}
```

### Memory and Resource Issues

#### Issue: "Memory leaks"

**Diagnostic Tools:**

```java
public class MemoryDiagnostics {
    
    // Memory monitoring
    public static void monitorMemoryUsage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        System.out.println("=== Memory Usage Report ===");
        System.out.println("Heap Memory:");
        System.out.println("  Used: " + formatBytes(heapUsage.getUsed()));
        System.out.println("  Max: " + formatBytes(heapUsage.getMax()));
        System.out.println("  Usage: " + (heapUsage.getUsed() * 100.0 / heapUsage.getMax()) + "%");
        
        System.out.println("Non-Heap Memory:");
        System.out.println("  Used: " + formatBytes(nonHeapUsage.getUsed()));
        System.out.println("  Max: " + formatBytes(nonHeapUsage.getMax()));
        
        if (heapUsage.getUsed() * 100.0 / heapUsage.getMax() > 80) {
            System.out.println("⚠️ High memory usage detected! Consider:");
            System.out.println("  - Increasing heap size (-Xmx)");
            System.out.println("  - Processing data in smaller batches");
            System.out.println("  - Checking for resource leaks");
        }
    }
    
    // Resource leak detection
    public static void detectResourceLeaks() {
        System.out.println("=== Resource Leak Detection ===");
        
        // Monitor thread count
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        int threadCount = threadBean.getThreadCount();
        System.out.println("Active threads: " + threadCount);
        
        if (threadCount > 100) {
            System.out.println("⚠️ High thread count! Check for:");
            System.out.println("  - Unclosed IgniteClient instances");
            System.out.println("  - Runaway async operations");
            System.out.println("  - Thread pool leaks");
        }
        
        // Monitor open file descriptors (Unix/Linux)
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                long openFileDescriptors = sunOsBean.getOpenFileDescriptorCount();
                long maxFileDescriptors = sunOsBean.getMaxFileDescriptorCount();
                
                System.out.println("File descriptors: " + openFileDescriptors + "/" + maxFileDescriptors);
                
                if (openFileDescriptors > maxFileDescriptors * 0.8) {
                    System.out.println("⚠️ High file descriptor usage! Check for:");
                    System.out.println("  - Unclosed connections");
                    System.out.println("  - File handle leaks");
                }
            }
        } catch (Exception e) {
            System.out.println("Could not check file descriptors: " + e.getMessage());
        }
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
```

## Debugging Techniques

### Logging Configuration

#### Enable Debug Logging

```java
public class LoggingConfiguration {
    
    // Configure logging for debugging
    public static void configureDebugLogging() {
        // For Logback (logback.xml)
        System.out.println("Add to logback.xml:");
        System.out.println("""
            <logger name="org.apache.ignite" level="DEBUG"/>
            <logger name="org.apache.ignite.client" level="TRACE"/>
            <logger name="org.apache.ignite.sql" level="DEBUG"/>
            """);
        
        // For Log4j2 (log4j2.xml)
        System.out.println("Add to log4j2.xml:");
        System.out.println("""
            <Logger name="org.apache.ignite" level="DEBUG"/>
            <Logger name="org.apache.ignite.client" level="TRACE"/>
            <Logger name="org.apache.ignite.sql" level="DEBUG"/>
            """);
    }
    
    // Programmatic logging setup
    public static void enableDetailedLogging() {
        // Set system properties for more detailed logging
        System.setProperty("java.util.logging.config.file", "logging.properties");
        
        // Enable SQL query logging
        System.setProperty("IGNITE_QUIET", "false");
    }
}
```

### Request/Response Debugging

```java
public class RequestResponseDebugging {
    
    // Debug wrapper for client operations
    public static class DebuggingIgniteClient {
        private final IgniteClient delegate;
        private final AtomicLong requestCounter = new AtomicLong(0);
        
        public DebuggingIgniteClient(IgniteClient delegate) {
            this.delegate = delegate;
        }
        
        public <T> T executeWithDebug(String operation, Supplier<T> supplier) {
            long requestId = requestCounter.incrementAndGet();
            long startTime = System.currentTimeMillis();
            
            System.out.println("[" + requestId + "] Starting " + operation);
            
            try {
                T result = supplier.get();
                long duration = System.currentTimeMillis() - startTime;
                System.out.println("[" + requestId + "] Completed " + operation + " in " + duration + "ms");
                return result;
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                System.err.println("[" + requestId + "] Failed " + operation + " after " + duration + "ms: " + e.getMessage());
                throw e;
            }
        }
        
        public Customer getCustomerWithDebug(Long id) {
            return executeWithDebug("getCustomer(" + id + ")", () -> {
                RecordView<Customer> view = delegate.tables().table("Customer").recordView(Customer.class);
                Customer key = new Customer();
                key.setId(id);
                return view.get(null, key);
            });
        }
        
        public List<Customer> queryWithDebug(String sql, Object... params) {
            return executeWithDebug("query(" + sql + ")", () -> {
                try (ResultSet<Customer> resultSet = delegate.sql().execute(
                        null, Mapper.of(Customer.class), sql, params)) {
                    
                    List<Customer> results = new ArrayList<>();
                    while (resultSet.hasNext()) {
                        results.add(resultSet.next());
                    }
                    return results;
                }
            });
        }
    }
}
```

### Connection State Monitoring

```java
public class ConnectionStateMonitoring {
    
    // Monitor client connection state
    public static void monitorConnectionHealth(IgniteClient client) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Check connection count
                int connectionCount = client.connections().size();
                System.out.println("Active connections: " + connectionCount);
                
                // Perform health check
                long startTime = System.currentTimeMillis();
                client.sql().execute(null, "SELECT 1 as health_check").next();
                long responseTime = System.currentTimeMillis() - startTime;
                
                System.out.println("Health check response time: " + responseTime + "ms");
                
                if (responseTime > 1000) {
                    System.out.println("⚠️ Slow response detected!");
                }
                
            } catch (Exception e) {
                System.err.println("❌ Health check failed: " + e.getMessage());
            }
        }, 0, 30, TimeUnit.SECONDS);
        
        // Shutdown hook to clean up
        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::shutdown));
    }
    
    // Comprehensive connection diagnostics
    public static void diagnoseConnection(IgniteClient client) {
        System.out.println("=== Connection Diagnostics ===");
        
        try {
            // Test basic connectivity
            long startTime = System.currentTimeMillis();
            client.sql().execute(null, "SELECT CURRENT_TIMESTAMP as current_time").next();
            long responseTime = System.currentTimeMillis() - startTime;
            
            System.out.println("✅ Basic connectivity: OK (" + responseTime + "ms)");
            
            // Test schema access
            Collection<String> tables = client.catalog().tables();
            System.out.println("✅ Schema access: OK (" + tables.size() + " tables)");
            
            // Test transaction capability
            try (Transaction tx = client.transactions().begin()) {
                tx.rollback();  // Immediately rollback
                System.out.println("✅ Transaction support: OK");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Connection diagnostic failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### Performance Profiling

```java
public class PerformanceProfiling {
    
    // Simple performance profiler
    public static class SimpleProfiler {
        private final Map<String, List<Long>> timings = new ConcurrentHashMap<>();
        
        public <T> T profile(String operation, Supplier<T> supplier) {
            long startTime = System.nanoTime();
            try {
                return supplier.get();
            } finally {
                long duration = System.nanoTime() - startTime;
                timings.computeIfAbsent(operation, k -> new ArrayList<>()).add(duration);
            }
        }
        
        public void printReport() {
            System.out.println("=== Performance Report ===");
            
            timings.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String operation = entry.getKey();
                    List<Long> times = entry.getValue();
                    
                    long avgNanos = (long) times.stream().mapToLong(Long::longValue).average().orElse(0);
                    long minNanos = times.stream().mapToLong(Long::longValue).min().orElse(0);
                    long maxNanos = times.stream().mapToLong(Long::longValue).max().orElse(0);
                    
                    System.out.printf("%s: avg=%.2fms, min=%.2fms, max=%.2fms, count=%d%n",
                        operation,
                        avgNanos / 1_000_000.0,
                        minNanos / 1_000_000.0,
                        maxNanos / 1_000_000.0,
                        times.size());
                });
        }
        
        public void reset() {
            timings.clear();
        }
    }
    
    // Usage example
    public static void demonstrateProfiling(IgniteClient client) {
        SimpleProfiler profiler = new SimpleProfiler();
        
        // Profile various operations
        for (int i = 0; i < 10; i++) {
            profiler.profile("simple-query", () -> {
                return client.sql().execute(null, "SELECT COUNT(*) as count FROM Customer").next();
            });
            
            profiler.profile("table-access", () -> {
                RecordView<Customer> view = client.tables().table("Customer").recordView(Customer.class);
                Customer key = new Customer();
                key.setId(1L);
                return view.get(null, key);
            });
        }
        
        profiler.printReport();
    }
}
```

### Emergency Debugging Checklist

```java
public class EmergencyDebugging {
    
    // Complete system health check
    public static void emergencyHealthCheck(IgniteClient client) {
        System.out.println("=== EMERGENCY HEALTH CHECK ===");
        
        // 1. Basic connectivity
        System.out.println("\n1. Testing basic connectivity...");
        try {
            client.sql().execute(null, "SELECT 1").next();
            System.out.println("✅ Basic connectivity: OK");
        } catch (Exception e) {
            System.err.println("❌ Basic connectivity: FAILED - " + e.getMessage());
            return; // Can't proceed without basic connectivity
        }
        
        // 2. Schema access
        System.out.println("\n2. Testing schema access...");
        try {
            Collection<String> tables = client.catalog().tables();
            System.out.println("✅ Schema access: OK (" + tables.size() + " tables)");
            System.out.println("   Available tables: " + String.join(", ", tables));
        } catch (Exception e) {
            System.err.println("❌ Schema access: FAILED - " + e.getMessage());
        }
        
        // 3. Transaction capability
        System.out.println("\n3. Testing transaction capability...");
        try {
            try (Transaction tx = client.transactions().begin()) {
                tx.rollback();
            }
            System.out.println("✅ Transactions: OK");
        } catch (Exception e) {
            System.err.println("❌ Transactions: FAILED - " + e.getMessage());
        }
        
        // 4. Performance check
        System.out.println("\n4. Testing performance...");
        try {
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 10; i++) {
                client.sql().execute(null, "SELECT " + i).next();
            }
            long avgTime = (System.currentTimeMillis() - startTime) / 10;
            
            if (avgTime < 100) {
                System.out.println("✅ Performance: GOOD (" + avgTime + "ms avg)");
            } else {
                System.out.println("⚠️ Performance: SLOW (" + avgTime + "ms avg)");
            }
        } catch (Exception e) {
            System.err.println("❌ Performance test: FAILED - " + e.getMessage());
        }
        
        // 5. Memory status
        System.out.println("\n5. Checking memory status...");
        MemoryDiagnostics.monitorMemoryUsage();
        
        System.out.println("\n=== HEALTH CHECK COMPLETE ===");
    }
    
    // Quick troubleshooting guide
    public static void printTroubleshootingGuide() {
        System.out.println("""
            === QUICK TROUBLESHOOTING GUIDE ===
            
            Connection Issues:
            1. Check if Ignite cluster is running
            2. Verify correct port (10800 for thin client)
            3. Check firewall settings
            4. Test network connectivity
            
            Performance Issues:
            1. Check query execution plans
            2. Verify indexes exist for WHERE clauses
            3. Consider query optimization
            4. Monitor memory usage
            
            Schema Issues:
            1. Verify table exists in catalog
            2. Check zone configuration
            3. Validate annotations
            4. Ensure proper DDL execution
            
            Transaction Issues:
            1. Check transaction timeout settings
            2. Verify lock ordering
            3. Consider batch size reduction
            4. Monitor for deadlocks
            
            For more help, check the full documentation.
            """);
    }
}
```
