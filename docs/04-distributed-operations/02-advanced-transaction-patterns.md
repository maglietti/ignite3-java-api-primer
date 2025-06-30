# Chapter 4.2: Transaction Use Cases and Applied Patterns

Your complex purchase workflow deadlocks when multiple users simultaneously buy tracks from the same album because of improper transaction ordering across distributed nodes. Customer sessions timeout while waiting for nested transaction chains to complete. Your application crashes during peak sales when distributed transaction failures cascade through the system without proper recovery patterns.

These problems require applied transaction patterns that handle real-world business scenarios, non-blocking operations, and resilient error recovery. Building on the [transaction fundamentals](01-transaction-fundamentals.md), this chapter demonstrates how to implement distributed transaction workflows for common business use cases that scale under concurrent load.

## Applied Transaction Patterns

This chapter focuses on practical transaction patterns for common business scenarios in distributed music store operations. These patterns demonstrate how to apply the distributed transaction fundamentals to real-world use cases that require coordinated operations across multiple tables and cluster nodes.

## Explicit Transaction Lifecycle Control

Business workflows with complex decision trees require precise control over transaction boundaries to prevent resource leaks and ensure proper rollback sequences across distributed operations. When purchase validation involves multiple steps with different rollback strategies, explicit transaction management provides the necessary control.

```java
/**
 * Explicit transaction patterns for business workflow control.
 * Manages transaction lifecycle manually when purchase validation
 * requires different rollback strategies across distributed operations.
 */
public class BusinessWorkflowTransactions {
    
    public void basicExplicitPattern(IgniteClient client) {
        Transaction tx = null;
        try {
            // 1. Begin transaction
            tx = client.transactions().begin();
            
            // 2. Perform operations
            performBusinessOperations(client, tx);
            
            // 3. Commit if all operations succeed
            tx.commit();
            System.out.println("Transaction completed successfully");
            
        } catch (Throwable e) {
            System.err.println("Transaction failed: " + e.getMessage());
            
            // 4. Rollback on any error
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Throwable rollbackError) {
                    System.err.println("Rollback failed: " + rollbackError.getMessage());
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
            System.out.println("Transaction completed successfully");
            
        } catch (Throwable e) {
            System.err.println("Transaction failed: " + e.getMessage());
        } finally {
            // Ensure rollback if transaction wasn't committed
            if (tx != null && !committed) {
                try {
                    tx.rollback();
                } catch (Throwable rollbackError) {
                    System.err.println("Rollback failed: " + rollbackError.getMessage());
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

Explicit transaction management becomes necessary when business workflow logic requires different rollback strategies based on validation results, payment processing outcomes, or inventory availability checks. The pattern provides fine-grained control but requires careful resource management to prevent transaction leaks and data inconsistency in distributed environments.

## Business Use Case: Customer Purchase Workflow

Customer purchase workflows fail when partial updates leave orphaned invoice records without corresponding line items, or when concurrent purchases create race conditions that double-charge customers. Distributed nodes must coordinate multiple table updates atomically to maintain data consistency.

This common e-commerce use case demonstrates how to coordinate updates across invoice, line item, and inventory tables. Any failure must rollback all changes to prevent data corruption and customer billing errors.

```java
/**
 * Multi-table purchase workflow with coordinated distributed updates.
 * Prevents partial invoice creation and billing inconsistencies
 * by ensuring all operations complete atomically across nodes.
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
                System.out.println("Created invoice " + invoiceId);
                
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
                    System.out.println("Added track " + trackId + " ($" + unitPrice + ")");
                }
                
                // Step 3: Update invoice with calculated total
                invoice.setTotal(totalAmount);
                invoiceTable.upsert(tx, invoice);
                
                System.out.println("Invoice total: $" + totalAmount);
                System.out.println("Purchase completed successfully");
                
                return invoiceId;
                
            } catch (Exception e) {
                System.err.println("Purchase failed: " + e.getMessage());
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

This business use case demonstrates coordinated distributed updates where invoice creation, line item insertion, and total calculation execute atomically. If track price lookup fails or line item creation encounters errors, the entire transaction rolls back, preventing orphaned invoices and billing inconsistencies. This pattern is essential for any multi-step business process that spans multiple tables.

## Business Use Case: Operational vs. Analytical Transactions

Different business operations have different performance and timeout requirements. Quick customer updates need short timeouts to fail fast, while complex monthly reports require extended timeouts to complete across multiple nodes. TransactionOptions provides precise control over transaction behavior for different business workflow requirements.

```java
/**
 * Transaction timeout patterns for different business operations.
 * Prevents resource blocking while ensuring sufficient time
 * for operational updates vs. analytical reports to complete.
 */
public class BusinessOperationTimeouts {
    
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
                System.out.printf("Music Store Statistics:%n");
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

### Business Use Case: Consistent Reporting

Read-only transactions avoid write locks and conflict detection overhead, enabling consistent multi-table reports without blocking concurrent customer purchases and updates. This optimization significantly improves performance for business intelligence and analytics workloads.

```java
/**
 * Read-only transaction patterns for business reporting and analytics.
 * Eliminates write lock overhead while maintaining snapshot consistency
 * across distributed music store reporting operations.
 */
public class BusinessReportingTransactions {
    
    public void generateMonthlySalesReport(IgniteClient client) {
        TransactionOptions readOnlyOptions = TransactionOptions.readOnly()
            .timeout(120, TimeUnit.SECONDS);
        
        client.transactions().runInTransaction(readOnlyOptions, tx -> {
            IgniteSql sql = client.sql();
            
            // All queries in the same transaction see consistent data
            System.out.println("Generating Monthly Sales Report...");
            
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
            
            System.out.println("Top Selling Tracks:");
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
            
            System.out.println("Revenue by Genre:");
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

## Business Use Case: High-Throughput Order Processing

Thread pool exhaustion occurs when synchronous transactions block waiting for distributed operations to complete during peak sales periods. High-throughput music store applications require non-blocking patterns that release threads while purchase operations execute across cluster nodes. Proper async implementation uses native transaction APIs rather than wrapping synchronous calls.

### Non-Blocking Order Processing

Native async transaction APIs prevent thread blocking while distributed purchase operations complete. Using `transactions.beginAsync()` directly avoids the overhead and resource contention of thread pool wrapping patterns during high-volume sales periods.

```java
/**
 * Non-blocking transaction patterns for high-throughput order processing.
 * Prevents thread pool exhaustion during peak sales by using native async APIs
 * instead of wrapping synchronous calls with CompletableFuture.
 */
public class HighThroughputOrderProcessing {
    
    public CompletableFuture<Void> createArtistAsync(IgniteClient client, String artistName) {
        return client.transactions().beginAsync()
            .thenCompose(tx -> {
                System.out.println("Starting async transaction for: " + artistName);
                
                RecordView<Artist> artistTable = client.tables().table("Artist").recordView(Artist.class);
                Artist artist = new Artist(generateArtistId(), artistName);
                
                // Chain async operations without blocking
                return artistTable.upsertAsync(tx, artist)
                    .thenCompose(ignored -> {
                        System.out.println("Artist created: " + artistName);
                        return tx.commitAsync();
                    })
                    .exceptionally(throwable -> {
                        System.err.println("Failed to create artist: " + throwable.getMessage());
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
                    System.out.println("Created " + artistNames.size() + " artists");
                    return "Success: " + artistNames.size() + " artists created";
                });
        });
    }
    
    private Integer generateArtistId() {
        return (int) (System.currentTimeMillis() % 100000);
    }
}
```

### Complex Business Workflow Coordination

```java
/**
 * Multi-stage async transaction workflows for complete purchase processes.
 * Coordinates customer validation, invoice creation, payment processing, and inventory updates
 * without blocking threads during distributed execution.
 */
public class AsyncPurchaseWorkflow {
    
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

## Business Use Case: Resilient Transaction Processing

Transaction failures cascade through distributed music store systems when temporary network partitions, node failures, or high-concurrency deadlock situations occur during peak sales events. Resilient patterns implement retry strategies with exponential backoff and circuit breakers to handle transient failures while maintaining customer experience during system stress.

```java
/**
 * Retry strategies for transient business transaction failures.
 * Implements exponential backoff with jitter to prevent thundering herd
 * problems when multiple customers retry purchases simultaneously.
 */
public class ResilientBusinessTransactions {
    
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

### Circuit Breaker for Customer Protection

```java
/**
 * Circuit breaker pattern prevents cascading failures when business
 * transaction errors exceed threshold during system stress. Fails fast during outages to
 * protect system resources and provide predictable customer experience.
 */
public class BusinessTransactionCircuitBreaker {
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

These applied transaction patterns solve the real-world business challenges that cause customer-facing failures under concurrent load. Explicit lifecycle control prevents resource leaks during complex purchase flows, async patterns eliminate thread pool exhaustion during peak sales, and resilience patterns maintain customer experience during system stress.

The patterns demonstrated in this chapter provide the foundation for implementing reliable business workflows in distributed environments. By focusing on practical use cases like purchase processing, reporting, and high-throughput operations, these patterns bridge the gap between distributed transaction theory and production business applications.

The next chapter demonstrates how these transaction coordination patterns enable reliable distributed computing operations across cluster nodes for advanced data processing workloads.
