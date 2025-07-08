<!--
Licensed under Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International (CC BY-NC-SA 4.0)
SPDX-License-Identifier: CC-BY-NC-SA-4.0
For full license text, see LICENSE-CC-BY-NC-SA-4.0
-->

# Chapter 4.1: Transaction Fundamentals and Applied Patterns

A customer clicks "Buy Album" for a $12.99 purchase. Your application must create an invoice, add line items for each track, update inventory counts, and add tracks to the customer's library. Without proper transaction management, a failure between any of these steps leaves your data in an inconsistent state.

This chapter covers transaction fundamentals, what transactions are, why they're essential, and how to apply them correctly in distributed systems through practical business patterns.

## What Are Transactions?

A transaction is a unit of work that either completes entirely or fails entirely. In database systems, transactions group multiple operations together to ensure data consistency.

Consider this music store purchase scenario without transactions:

```
1. Create invoice record → SUCCESS
2. Add track line items → SUCCESS  
3. Update inventory count → NETWORK FAILURE
4. Add tracks to customer library → NEVER EXECUTED
```

Result: Customer charged, inventory not updated, library not updated. Data is inconsistent.

With transactions, either all operations succeed or all fail:

```
TRANSACTION BEGIN
1. Create invoice record
2. Add track line items  
3. Update inventory count
4. Add tracks to customer library
TRANSACTION COMMIT → All changes saved together
```

If any step fails, the entire transaction rolls back and no changes are saved.

## ACID Properties Explained

Transactions provide four critical guarantees, known as ACID properties:

### Atomicity

**Definition**: All operations in a transaction either complete successfully or none do.

**Example**: When transferring $50 between customer accounts:

- Debit $50 from Account A
- Credit $50 to Account B

Without atomicity, a failure after the debit but before the credit leaves $50 missing from the system.

### Consistency  

**Definition**: Transactions maintain database integrity rules and business constraints.

**Example**: Business rule states "Album total price must equal sum of track prices."

- Insert Album with TotalPrice = $12.99
- Insert 10 tracks with UnitPrice = $1.29 each (total $12.90)

The transaction fails because it violates the consistency rule (prices don't match).

### Isolation

**Definition**: Concurrent transactions don't interfere with each other.

**Example**: Two customers simultaneously buying the last copy of an album:

- Customer A reads inventory: 1 available
- Customer B reads inventory: 1 available  
- Customer A decrements inventory: 0 remaining
- Customer B decrements inventory: -1 remaining (PROBLEM)

Proper isolation prevents both customers from seeing the same inventory count.

### Durability

**Definition**: Once committed, transaction changes survive system failures.

**Example**: After confirming "Purchase Complete" to a customer, the data persists even if the server crashes immediately afterward.

## Distributed Transaction Challenges

Single-node databases have simpler transaction requirements. Distributed systems face additional complexity:

### Network Partitions

Nodes may lose communication during transaction processing. The system must decide whether to continue with partial node participation or halt until connectivity restores.

### Node Failures  

Individual nodes may crash during multi-step operations. The system needs mechanisms to detect failures and coordinate recovery across remaining nodes.

### Data Distribution

Related data may reside on different nodes, requiring coordination protocols to maintain consistency across the cluster.

**Example**: Customer data on Node 1, invoice data on Node 2, inventory data on Node 3. A purchase transaction must coordinate changes across all three nodes atomically.

## How Ignite 3 Handles Distributed Transactions

Ignite 3 uses a **two-phase commit protocol** to coordinate transactions across multiple nodes:

### Phase 1: Prepare

1. Transaction coordinator sends "prepare" message to all participating nodes
2. Each node validates its portion of the transaction
3. Each node responds with "ready" or "abort"
4. If any node responds "abort," the entire transaction fails

### Phase 2: Commit  

1. If all nodes respond "ready," coordinator sends "commit" message
2. Each node applies its changes permanently
3. Each node responds with "committed"
4. Transaction completes successfully

This protocol ensures atomicity across distributed nodes—either all nodes commit their changes or none do.

## Basic Transaction Usage

Ignite 3 provides two approaches for transaction management:

### Explicit Transaction Management

```java
Transaction tx = client.transactions().begin();
try {
    // Perform operations
    RecordView<Artist> artists = client.tables().table("Artist").recordView(Artist.class);
    artists.upsert(tx, new Artist(1, "Arctic Monkeys"));
    
    tx.commit();
} catch (Exception e) {
    tx.rollback();
}
```

### Automatic Transaction Management (Recommended)

```java
client.transactions().runInTransaction(tx -> {
    RecordView<Artist> artists = client.tables().table("Artist").recordView(Artist.class);
    artists.upsert(tx, new Artist(1, "Arctic Monkeys"));
    // Automatic commit on success, rollback on exception
});
```

The automatic approach eliminates common errors like forgetting to rollback on exceptions.

## Practical Transaction Examples

### Creating Related Records

When creating related data like an artist and their album, both records must be created together or not at all:

```java
public void createArtistAndAlbum(IgniteClient client) {
    Transaction tx = client.transactions().begin();
    
    try {
        RecordView<Artist> artists = client.tables().table("Artist").recordView(Artist.class);
        RecordView<Album> albums = client.tables().table("Album").recordView(Album.class);
        
        Artist artist = new Artist(100, "Arctic Monkeys");
        artists.upsert(tx, artist);
        
        Album album = new Album(200, 100, "AM");  // Links to artist ID 100
        albums.upsert(tx, album);
        
        tx.commit();  // Both records created atomically
        
    } catch (Exception e) {
        tx.rollback();  // Neither record created
        System.err.println("Failed to create artist and album: " + e.getMessage());
    }
}
```

### Inventory Management

Updating inventory requires consistency to prevent overselling:

```java
public boolean purchaseTrack(IgniteClient client, int trackId, int customerId) {
    return client.transactions().runInTransaction(tx -> {
        RecordView<Track> tracks = client.tables().table("Track").recordView(Track.class);
        RecordView<Purchase> purchases = client.tables().table("Purchase").recordView(Purchase.class);
        
        Track track = tracks.get(tx, Tuple.create(Map.of("TrackId", trackId)));
        if (track == null || track.getInventory() <= 0) {
            throw new RuntimeException("Track not available");
        }
        
        // Decrease inventory
        track.setInventory(track.getInventory() - 1);
        tracks.upsert(tx, track);
        
        // Record purchase
        Purchase purchase = new Purchase(customerId, trackId, LocalDate.now());
        purchases.upsert(tx, purchase);
        
        return true;  // Both operations succeed or both fail
    });
}
```

## Transaction Isolation Levels

Transactions can operate with different isolation levels that balance consistency with performance:

### Read-Write Transactions

Default mode for transactions that modify data. Provides full ACID guarantees but requires distributed coordination:

```java
public void updateTrackPrice(IgniteClient client, int trackId, BigDecimal newPrice) {
    client.transactions().runInTransaction(tx -> {
        RecordView<Track> tracks = client.tables().table("Track").recordView(Track.class);
        
        Track track = tracks.get(tx, Tuple.create(Map.of("TrackId", trackId)));
        if (track != null) {
            track.setUnitPrice(newPrice);
            tracks.upsert(tx, track);
        }
    });
}
```

### Read-Only Transactions

Optimized for queries that don't modify data. Provides snapshot consistency without locking:

```java
import org.apache.ignite.sql.Statement;

public List<Track> getExpensiveTracks(IgniteClient client) {
    TransactionOptions options = new TransactionOptions().readOnly(true);
    
    return client.transactions().runInTransaction(options, tx -> {
        IgniteSql sql = client.sql();
        Statement stmt = client.sql().statementBuilder()
            .query("SELECT * FROM Track WHERE UnitPrice > ?")
            .build();
        ResultSet<SqlRow> result = sql.execute(tx, stmt, new BigDecimal("1.50"));
        
        List<Track> tracks = new ArrayList<>();
        while (result.hasNext()) {
            SqlRow row = result.next();
            // Convert row to Track object
            tracks.add(convertToTrack(row));
        }
        return tracks;
    });
}
```

## Important Notes

### Transaction Interface Lifecycle

The `Transaction` interface is **not AutoCloseable**. For safety, use the automatic transaction management pattern:

```java
// Recommended: Automatic management
client.transactions().runInTransaction(tx -> {
    // Operations here
});

// Manual management requires careful exception handling
Transaction tx = client.transactions().begin();
try {
    // Operations here
    tx.commit();
} finally {
    if (!tx.isCommitted()) {
        tx.rollback();
    }
}
```

### Transaction Options

Configure transaction behavior with `TransactionOptions`:

```java
TransactionOptions options = new TransactionOptions()
    .readOnly(true)        // Optimize for queries
    .timeoutMillis(30000); // Set timeout

client.transactions().runInTransaction(options, tx -> {
    // Operations with custom options
});
```

## Applied Transaction Patterns

Building on the fundamentals, these patterns address common business scenarios that require coordinated operations across multiple tables and cluster nodes.

### Customer Purchase Workflow

E-commerce workflows require coordinating updates across invoice, line item, and inventory tables atomically:

```java
public Integer processPurchase(IgniteClient client, Integer customerId, List<Integer> trackIds) {
    return client.transactions().runInTransaction(tx -> {
        RecordView<Invoice> invoiceTable = client.tables().table("Invoice").recordView(Invoice.class);
        RecordView<InvoiceLine> invoiceLineTable = client.tables().table("InvoiceLine").recordView(InvoiceLine.class);
        IgniteSql sql = client.sql();
        
        // Step 1: Create the invoice
        Integer invoiceId = generateInvoiceId();
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setCustomerId(customerId);
        invoice.setInvoiceDate(LocalDate.now());
        invoiceTable.upsert(tx, invoice);
        
        // Step 2: Add line items and calculate total
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Integer trackId : trackIds) {
            // Get track price using SQL in the same transaction
            Statement trackStmt = sql.statementBuilder()
                .query("SELECT UnitPrice FROM Track WHERE TrackId = ?")
                .build();
            ResultSet<SqlRow> trackResult = sql.execute(tx, trackStmt, trackId);
            
            if (!trackResult.hasNext()) {
                throw new IllegalArgumentException("Track not found: " + trackId);
            }
            
            BigDecimal unitPrice = trackResult.next().decimalValue("UnitPrice");
            
            // Create line item
            InvoiceLine lineItem = new InvoiceLine();
            lineItem.setInvoiceId(invoiceId);
            lineItem.setTrackId(trackId);
            lineItem.setUnitPrice(unitPrice);
            lineItem.setQuantity(1);
            
            invoiceLineTable.upsert(tx, lineItem);
            totalAmount = totalAmount.add(unitPrice);
        }
        
        // Step 3: Update invoice with calculated total
        invoice.setTotal(totalAmount);
        invoiceTable.upsert(tx, invoice);
        
        return invoiceId;
    });
}
```

This pattern ensures that invoice creation, line item insertion, and total calculation execute atomically. If any step fails, the entire transaction rolls back, preventing orphaned invoices and billing inconsistencies.

### Transaction Timeout Configuration

Different business operations require different timeout configurations:

```java
// Quick operations - short timeout for fast failure
TransactionOptions quickOptions = TransactionOptions.readWrite()
    .timeout(5, TimeUnit.SECONDS);

client.transactions().runInTransaction(quickOptions, tx -> {
    // Simple customer update
    RecordView<Customer> customers = client.tables().table("Customer").recordView(Customer.class);
    Customer customer = customers.get(tx, customerKey);
    customer.setLastLogin(LocalDateTime.now());
    customers.upsert(tx, customer);
    return true;
});

// Complex analytics - longer timeout for completion
TransactionOptions analyticsOptions = TransactionOptions.readOnly()
    .timeout(60, TimeUnit.SECONDS);

client.transactions().runInTransaction(analyticsOptions, tx -> {
    // Complex multi-table analysis
    IgniteSql sql = client.sql();
    // Perform comprehensive analytics queries
    return generateAnalyticsReport(sql, tx);
});
```

### Asynchronous Transaction Patterns

High-throughput applications benefit from non-blocking transaction patterns:

```java
// Create multiple artists asynchronously
public CompletableFuture<String> createMultipleArtistsAsync(
        IgniteClient client, List<String> artistNames) {
    
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
            .thenApply(ignored -> "Success: " + artistNames.size() + " artists created");
    });
}
```

### Error Handling and Resilience

Production applications require robust error handling with retry logic:

```java
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
```

## Working with the Reference Application

The **`06-transactions-app`** demonstrates these patterns with complete working implementations:

```bash
cd ignite3-reference-apps/06-transactions-app
mvn compile exec:java
```

The reference application includes business workflow examples, async patterns, error handling strategies, and performance optimization techniques.

## Best Practices

1. **Use automatic transaction management** - Prevents resource leaks and ensures proper cleanup
2. **Keep transactions short** - Minimizes lock contention and improves performance  
3. **Use read-only transactions for queries** - Better performance when no modifications needed
4. **Handle specific exceptions** - Retry on conflicts, fail fast on business rule violations
5. **Prepare data outside transactions** - Minimize time spent holding locks
6. **Configure appropriate timeouts** - Match timeout duration to operation complexity
7. **Use async patterns for high throughput** - Prevent thread blocking during distributed operations

These transaction patterns provide the foundation for reliable distributed operations in music streaming applications. They ensure data consistency during complex business workflows while maintaining performance under concurrent load.

---

**Next**: [Distributed Computing](03-compute-api-processing.md) - Execute code directly on cluster nodes where data resides to eliminate network bottlenecks and enable massive parallel processing for analytics and recommendation algorithms.
