# Chapter 4.2: Advanced Transaction Patterns

## Learning Objectives

By completing this chapter, you will:

- Master functional transaction patterns for cleaner code organization
- Implement asynchronous transactions for high-throughput applications
- Configure transaction options for different performance requirements
- Handle complex multi-step business workflows with proper error recovery

## Explicit Transaction Management (Advanced)

For scenarios requiring fine-grained control over transaction lifecycle, you can manage transactions explicitly. **Note: This approach is generally not recommended** due to increased complexity and error potential.

```java
/**
 * Demonstrates explicit transaction management patterns.
 * USE WITH CAUTION: This approach requires careful error handling
 * and is prone to resource leaks if not implemented correctly.
 */
public class ExplicitTransactionManagement {
    
    public void basicExplicitPattern(IgniteClient client) {
        Transaction tx = null;
        try {
            // 1. Begin transaction
            tx = client.transactions().begin();
            
            // 2. Perform operations
            performBusinessOperations(client, tx);
            
            // 3. Commit if all operations succeed
            tx.commit();
            System.out.println("✓ Transaction completed successfully");
            
        } catch (Throwable e) {
            System.err.println("✗ Transaction failed: " + e.getMessage());
            
            // 4. Rollback on any error
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Throwable rollbackError) {
                    System.err.println("✗ Rollback failed: " + rollbackError.getMessage());
                }
            }
        }
    }
    
    public void saferExplicitPattern(IgniteClient client) {
        // Safer explicit pattern: ensure rollback in finally block
        Transaction tx = null;
        boolean committed = false;
        try {
            tx = client.transactions().begin();
            performBusinessOperations(client, tx);
            tx.commit();
            committed = true;
            System.out.println("✓ Transaction completed successfully");
            
        } catch (Throwable e) {
            System.err.println("✗ Transaction failed: " + e.getMessage());
        } finally {
            // Ensure rollback if transaction wasn't committed
            if (tx != null && !committed) {
                try {
                    tx.rollback();
                } catch (Throwable rollbackError) {
                    System.err.println("✗ Rollback failed: " + rollbackError.getMessage());
                }
            }
        }
    }
    
    private void performBusinessOperations(IgniteClient client, Transaction tx) {
        RecordView<Customer> customers = client.tables().table("Customer").recordView(Customer.class);
        IgniteSql sql = client.sql();
        
        // Mix Table API and SQL operations in same transaction
        Customer customer = new Customer(100, "Jane", "Smith", "jane@example.com");
        customers.upsert(tx, customer);
        
        sql.execute(tx, "UPDATE Customer SET Country = ? WHERE CustomerId = ?", "USA", 100);
    }
}
```

**When explicit management might be necessary:**

- **Complex conditional logic**: Multiple decision points requiring different rollback strategies
- **Performance optimization**: Reusing transactions across multiple operations
- **Integration scenarios**: Coordinating with external transaction managers
- **Debugging purposes**: Fine-grained control for troubleshooting

**Risks of explicit management:**

- **Resource leaks**: Forgetting to close transactions
- **Inconsistent state**: Improper error handling leaving data corrupted  
- **Code complexity**: More boilerplate and error-prone patterns
- **Maintenance burden**: Harder to understand and modify

## Real-World Scenario: Customer Purchase Workflow

Let's implement a complete customer purchase transaction that demonstrates why transactions matter in distributed systems.

### The Purchase Challenge

When a customer buys tracks, multiple things must happen atomically:

1. **Create invoice** with customer information
2. **Add line items** for each purchased track
3. **Calculate and set** the total amount
4. **Update track sales counters** (if maintaining statistics)

If any step fails, the entire purchase should be cancelled. No partial invoices, no orphaned line items.

### Multi-Table Transaction Implementation

```java
/**
 * Demonstrates a realistic business workflow requiring transactions.
 * This example shows why ACID properties are crucial for maintaining
 * data consistency in multi-table operations.
 */
public class CustomerPurchaseWorkflow {
    
    public Integer processPurchase(IgniteClient client, Integer customerId, List<Integer> trackIds) {
        return client.transactions().runInTransaction(tx -> {
            try {
                // Get table access
                RecordView<Invoice> invoiceTable = client.tables().table("Invoice").recordView(Invoice.class);
                RecordView<InvoiceLine> invoiceLineTable = client.tables().table("InvoiceLine").recordView(InvoiceLine.class);
                IgniteSql sql = client.sql();
                
                // Step 1: Create the invoice
                Integer invoiceId = generateInvoiceId();
                Invoice invoice = new Invoice();
                invoice.setInvoiceId(invoiceId);
                invoice.setCustomerId(customerId);
                invoice.setInvoiceDate(LocalDate.now());
                invoice.setBillingAddress("123 Music Street");
                invoice.setBillingCity("Harmony");
                invoice.setBillingCountry("USA");
                invoice.setTotal(BigDecimal.ZERO);  // Will calculate later
                
                invoiceTable.upsert(tx, invoice);
                System.out.println("📝 Created invoice " + invoiceId);
                
                // Step 2: Add line items and calculate total
                BigDecimal totalAmount = BigDecimal.ZERO;
                for (int i = 0; i < trackIds.size(); i++) {
                    Integer trackId = trackIds.get(i);
                    
                    // Get track price using SQL in the same transaction
                    ResultSet<SqlRow> trackResult = sql.execute(tx,
                        "SELECT UnitPrice FROM Track WHERE TrackId = ?", trackId);
                    
                    if (!trackResult.hasNext()) {
                        throw new IllegalArgumentException("Track not found: " + trackId);
                    }
                    
                    BigDecimal unitPrice = trackResult.next().decimalValue("UnitPrice");
                    
                    // Create line item
                    InvoiceLine lineItem = new InvoiceLine();
                    lineItem.setInvoiceLineId(generateLineItemId(i));
                    lineItem.setInvoiceId(invoiceId);
                    lineItem.setTrackId(trackId);
                    lineItem.setUnitPrice(unitPrice);
                    lineItem.setQuantity(1);
                    
                    invoiceLineTable.upsert(tx, lineItem);
                    totalAmount = totalAmount.add(unitPrice);
                    System.out.println("🎵 Added track " + trackId + " ($" + unitPrice + ")");
                }
                
                // Step 3: Update invoice with calculated total
                invoice.setTotal(totalAmount);
                invoiceTable.upsert(tx, invoice);
                
                System.out.println("💰 Invoice total: $" + totalAmount);
                System.out.println("✓ Purchase completed successfully");
                
                return invoiceId;
                
            } catch (Exception e) {
                System.err.println("✗ Purchase failed: " + e.getMessage());
                throw e;  // This will trigger rollback
            }
        });
    }
    
    private Integer generateInvoiceId() {
        return (int) (System.currentTimeMillis() % 100000);
    }
    
    private Integer generateLineItemId(int index) {
        return (int) ((System.currentTimeMillis() + index) % 100000);
    }
}
```

**Transaction benefits demonstrated:**

- **Atomicity**: All changes happen together or not at all
- **Consistency**: Foreign key relationships are maintained
- **Isolation**: Other transactions don't see partial state
- **Durability**: Committed changes survive system failures

## TransactionOptions: Controlling Transaction Behavior

Different business scenarios require different transaction configurations. The `TransactionOptions` class provides fine-grained control over transaction behavior.

### Timeout Configuration

```java
/**
 * Demonstrates transaction timeout configuration for different scenarios.
 * Proper timeout setting prevents long-running transactions from blocking
 * system resources while allowing sufficient time for legitimate operations.
 */
public class TransactionTimeouts {
    
    public void quickUpdate(IgniteClient client) {
        // Short timeout for simple operations
        TransactionOptions quickOptions = TransactionOptions.readWrite()
            .timeout(5, TimeUnit.SECONDS);
        
        client.transactions().runInTransaction(quickOptions, tx -> {
            RecordView<Artist> artistTable = client.tables().table("Artist").recordView(Artist.class);
            
            Artist key = new Artist();
            key.setArtistId(1);
            Artist artist = artistTable.get(tx, key);
            
            if (artist != null) {
                artist.setName(artist.getName() + " (Updated)");
                artistTable.upsert(tx, artist);
                System.out.println("Quick update completed");
            }
            return true;
        });
    }
    
    public void complexReport(IgniteClient client) {
        // Longer timeout for complex operations
        TransactionOptions reportOptions = TransactionOptions.readOnly()
            .timeout(60, TimeUnit.SECONDS);
        
        client.transactions().runInTransaction(reportOptions, tx -> {
            IgniteSql sql = client.sql();
            
            // Complex multi-table analysis
            ResultSet<SqlRow> stats = sql.execute(tx, """
                SELECT 
                    COUNT(DISTINCT a.ArtistId) as artist_count,
                    COUNT(DISTINCT al.AlbumId) as album_count,
                    COUNT(DISTINCT t.TrackId) as track_count,
                    AVG(t.UnitPrice) as avg_price
                FROM Artist a
                JOIN Album al ON a.ArtistId = al.ArtistId
                JOIN Track t ON al.AlbumId = t.AlbumId
                """);
            
            if (stats.hasNext()) {
                SqlRow row = stats.next();
                System.out.printf("📊 Music Store Statistics:%n");
                System.out.printf("   Artists: %d%n", row.longValue("artist_count"));
                System.out.printf("   Albums: %d%n", row.longValue("album_count"));
                System.out.printf("   Tracks: %d%n", row.longValue("track_count"));
                System.out.printf("   Average Price: $%.2f%n", row.decimalValue("avg_price"));
            }
            
            return true;
        });
    }
}
```

### Read-Only Transactions

Read-only transactions provide performance benefits for queries that don't modify data:

```java
/**
 * Read-only transactions optimize performance for reporting and analytics.
 * They can access multiple tables consistently without the overhead
 * of write locks or conflict detection.
 */
public class ReadOnlyTransactions {
    
    public void generateMonthlySalesReport(IgniteClient client) {
        TransactionOptions readOnlyOptions = TransactionOptions.readOnly()
            .timeout(120, TimeUnit.SECONDS);
        
        client.transactions().runInTransaction(readOnlyOptions, tx -> {
            IgniteSql sql = client.sql();
            
            // All queries in the same transaction see consistent data
            System.out.println("📈 Generating Monthly Sales Report...");
            
            // Top selling tracks
            ResultSet<SqlRow> topTracks = sql.execute(tx, """
                SELECT t.Name, COUNT(*) as sales_count
                FROM Track t
                JOIN InvoiceLine il ON t.TrackId = il.TrackId
                JOIN Invoice i ON il.InvoiceId = i.InvoiceId
                WHERE i.InvoiceDate >= ?
                GROUP BY t.TrackId, t.Name
                ORDER BY sales_count DESC
                LIMIT 10
                """, LocalDate.now().minusMonths(1));
            
            System.out.println("🎵 Top Selling Tracks:");
            while (topTracks.hasNext()) {
                SqlRow track = topTracks.next();
                System.out.printf("   %s: %d sales%n", 
                    track.stringValue("Name"), 
                    track.longValue("sales_count"));
            }
            
            // Revenue by genre
            ResultSet<SqlRow> genreRevenue = sql.execute(tx, """
                SELECT g.Name, SUM(il.UnitPrice * il.Quantity) as revenue
                FROM Genre g
                JOIN Track t ON g.GenreId = t.GenreId
                JOIN InvoiceLine il ON t.TrackId = il.TrackId
                JOIN Invoice i ON il.InvoiceId = i.InvoiceId
                WHERE i.InvoiceDate >= ?
                GROUP BY g.GenreId, g.Name
                ORDER BY revenue DESC
                """, LocalDate.now().minusMonths(1));
            
            System.out.println("🎭 Revenue by Genre:");
            while (genreRevenue.hasNext()) {
                SqlRow genre = genreRevenue.next();
                System.out.printf("   %s: $%.2f%n", 
                    genre.stringValue("Name"), 
                    genre.decimalValue("revenue"));
            }
            
            return true;
        });
    }
}
```

## Asynchronous Transactions: Non-Blocking Operations

For high-throughput applications, asynchronous transactions prevent blocking threads while operations complete across the distributed cluster. Proper async patterns use `transactions.beginAsync()` directly instead of wrapping synchronous operations in `CompletableFuture.supplyAsync()`.

### Basic Async Pattern

> [!IMPORTANT]
> **Async Best Practices**: Use `transactions.beginAsync()` directly instead of `CompletableFuture.supplyAsync(() -> transactions.begin())`. Avoid `.get()` calls that defeat the async purpose. Use proper rollback handling in error scenarios.

```java
/**
 * Demonstrates proper asynchronous transaction patterns for non-blocking operations.
 * Uses transactions.beginAsync() without blocking calls for optimal performance.
 * Includes proper error handling with rollback on failures.
 */
public class AsyncTransactionPatterns {
    
    public CompletableFuture<Void> createArtistAsync(IgniteClient client, String artistName) {
        return client.transactions().beginAsync()
            .thenCompose(tx -> {
                System.out.println("🚀 Starting async transaction for: " + artistName);
                
                RecordView<Artist> artistTable = client.tables().table("Artist").recordView(Artist.class);
                Artist artist = new Artist(generateArtistId(), artistName);
                
                // Chain async operations without blocking
                return artistTable.upsertAsync(tx, artist)
                    .thenCompose(ignored -> {
                        System.out.println("✓ Artist created: " + artistName);
                        return tx.commitAsync();
                    })
                    .exceptionally(throwable -> {
                        System.err.println("✗ Failed to create artist: " + throwable.getMessage());
                        try {
                            tx.rollback(); // Synchronous rollback in error path
                        } catch (Exception rollbackError) {
                            System.err.println("Rollback failed: " + rollbackError.getMessage());
                        }
                        throw new RuntimeException(throwable);
                    });
            });
    }
    
    public CompletableFuture<String> createMultipleArtistsAsync(IgniteClient client, List<String> artistNames) {
        return client.transactions().runInTransactionAsync(tx -> {
            RecordView<Artist> artistTable = client.tables().table("Artist").recordView(Artist.class);
            
            // Create all artists in parallel within the same transaction
            List<CompletableFuture<Void>> operations = artistNames.stream()
                .map(name -> {
                    Artist artist = new Artist(generateArtistId(), name);
                    return artistTable.upsertAsync(tx, artist);
                })
                .collect(Collectors.toList());
            
            // Wait for all operations to complete
            return CompletableFuture.allOf(operations.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> {
                    System.out.println("✓ Created " + artistNames.size() + " artists");
                    return "Success: " + artistNames.size() + " artists created";
                });
        });
    }
    
    private Integer generateArtistId() {
        return (int) (System.currentTimeMillis() % 100000);
    }
}
```

### Advanced Async Patterns

```java
/**
 * Advanced asynchronous transaction patterns for complex workflows.
 */
public class AdvancedAsyncPatterns {
    
    public CompletableFuture<PurchaseResult> processAsyncPurchase(
            IgniteClient client, Integer customerId, List<PurchaseItem> items) {
        
        return client.transactions().runInTransactionAsync(tx -> {
            return validateCustomerAsync(client, tx, customerId)
                .thenCompose(customer -> createInvoiceAsync(client, tx, customer, items))
                .thenCompose(invoice -> processPaymentAsync(client, tx, invoice))
                .thenCompose(payment -> updateInventoryAsync(client, tx, items))
                .thenApply(inventory -> new PurchaseResult(true, "Purchase completed successfully"))
                .exceptionally(throwable -> {
                    System.err.println("Purchase failed: " + throwable.getMessage());
                    return new PurchaseResult(false, "Purchase failed: " + throwable.getMessage());
                });
        });
    }
    
    private CompletableFuture<Customer> validateCustomerAsync(
            IgniteClient client, Transaction tx, Integer customerId) {
        RecordView<Customer> customers = client.tables().table("Customer").recordView(Customer.class);
        
        Customer key = new Customer();
        key.setCustomerId(customerId);
        
        return customers.getAsync(tx, key)
            .thenApply(customer -> {
                if (customer == null) {
                    throw new IllegalArgumentException("Customer not found: " + customerId);
                }
                return customer;
            });
    }
    
    private CompletableFuture<Invoice> createInvoiceAsync(
            IgniteClient client, Transaction tx, Customer customer, List<PurchaseItem> items) {
        RecordView<Invoice> invoices = client.tables().table("Invoice").recordView(Invoice.class);
        
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(generateInvoiceId());
        invoice.setCustomerId(customer.getCustomerId());
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setTotal(calculateTotal(items));
        
        return invoices.upsertAsync(tx, invoice)
            .thenApply(ignored -> invoice);
    }
    
    private CompletableFuture<Payment> processPaymentAsync(
            IgniteClient client, Transaction tx, Invoice invoice) {
        // Simulate async payment processing without blocking thread pools
        Payment payment = new Payment(invoice.getInvoiceId(), invoice.getTotal(), "SUCCESS");
        
        // In real implementation, this would use actual async payment gateway APIs
        // Avoid CompletableFuture.supplyAsync() with blocking operations
        return CompletableFuture.completedFuture(payment);
    }
    
    private CompletableFuture<Void> updateInventoryAsync(
            IgniteClient client, Transaction tx, List<PurchaseItem> items) {
        IgniteSql sql = client.sql();
        
        // Use async SQL APIs directly instead of wrapping sync operations
        List<CompletableFuture<Void>> updates = items.stream()
            .map(item -> sql.executeAsync(tx, 
                "UPDATE Inventory SET QuantityAvailable = QuantityAvailable - ? WHERE ProductId = ?",
                item.getQuantity(), item.getProductId())
                .thenApply(result -> (Void) null))
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(updates.toArray(new CompletableFuture[0]));
    }
    
    private Integer generateInvoiceId() {
        return (int) (System.currentTimeMillis() % 100000);
    }
    
    private BigDecimal calculateTotal(List<PurchaseItem> items) {
        return items.stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

// Helper classes
class PurchaseResult {
    private final boolean success;
    private final String message;
    
    public PurchaseResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    // Getters...
}

class PurchaseItem {
    private String productId;
    private int quantity;
    private BigDecimal price;
    
    // Constructors, getters, setters...
}

class Payment {
    private Integer invoiceId;
    private BigDecimal amount;
    private String status;
    
    public Payment(Integer invoiceId, BigDecimal amount, String status) {
        this.invoiceId = invoiceId;
        this.amount = amount;
        this.status = status;
    }
    
    // Getters...
}
```

## Error Handling and Recovery Patterns

### Retry Strategies

```java
/**
 * Demonstrates retry strategies for handling transient transaction failures.
 */
public class TransactionRetryPatterns {
    
    public <T> CompletableFuture<T> executeWithRetry(
            IgniteClient client, 
            Function<Transaction, T> operation,
            int maxRetries) {
        
        return executeWithRetryInternal(client, operation, maxRetries, 0);
    }
    
    private <T> CompletableFuture<T> executeWithRetryInternal(
            IgniteClient client,
            Function<Transaction, T> operation,
            int maxRetries,
            int currentAttempt) {
        
        return client.transactions().runInTransactionAsync(operation)
            .exceptionallyCompose(throwable -> {
                if (currentAttempt >= maxRetries) {
                    return CompletableFuture.failedFuture(throwable);
                }
                
                if (isRetryableException(throwable)) {
                    long delay = calculateBackoffDelay(currentAttempt);
                    
                    return CompletableFuture
                        .delayedExecutor(delay, TimeUnit.MILLISECONDS)
                        .thenCompose(ignored -> 
                            executeWithRetryInternal(client, operation, maxRetries, currentAttempt + 1));
                } else {
                    return CompletableFuture.failedFuture(throwable);
                }
            });
    }
    
    private boolean isRetryableException(Throwable throwable) {
        // Retry on transaction conflicts and temporary network issues
        return throwable instanceof TransactionException ||
               throwable.getMessage().contains("timeout") ||
               throwable.getMessage().contains("connection");
    }
    
    private long calculateBackoffDelay(int attempt) {
        // Exponential backoff with jitter
        long baseDelay = 100; // 100ms
        long exponentialDelay = baseDelay * (long) Math.pow(2, attempt);
        long jitter = (long) (Math.random() * 50); // 0-50ms jitter
        return Math.min(exponentialDelay + jitter, 5000); // Max 5 seconds
    }
}
```

### Circuit Breaker Pattern

```java
/**
 * Circuit breaker pattern for transaction operations.
 */
public class TransactionCircuitBreaker {
    private enum State { CLOSED, OPEN, HALF_OPEN }
    
    private State state = State.CLOSED;
    private int failureCount = 0;
    private long lastFailureTime = 0;
    private final int failureThreshold = 5;
    private final long timeoutDuration = 10000; // 10 seconds
    
    public <T> CompletableFuture<T> executeTransaction(
            IgniteClient client,
            Function<Transaction, T> operation) {
        
        if (state == State.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime > timeoutDuration) {
                state = State.HALF_OPEN;
            } else {
                return CompletableFuture.failedFuture(
                    new RuntimeException("Circuit breaker is OPEN"));
            }
        }
        
        return client.transactions().runInTransactionAsync(operation)
            .thenApply(result -> {
                onSuccess();
                return result;
            })
            .exceptionally(throwable -> {
                onFailure();
                throw new RuntimeException(throwable);
            });
    }
    
    private synchronized void onSuccess() {
        failureCount = 0;
        state = State.CLOSED;
    }
    
    private synchronized void onFailure() {
        failureCount++;
        lastFailureTime = System.currentTimeMillis();
        
        if (failureCount >= failureThreshold) {
            state = State.OPEN;
        }
    }
}
```

Advanced transaction patterns enable robust, high-performance distributed applications that handle complex business workflows while maintaining data consistency and providing excellent user experiences.

## Next Steps

Understanding advanced transaction patterns prepares you for distributed processing and compute operations:

- **[Chapter 4.3: Compute API for Distributed Processing](03-compute-api-processing.md)** - Learn how transaction consistency enables reliable distributed job execution, data processing, and analytics across your cluster
