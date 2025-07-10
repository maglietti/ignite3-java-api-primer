# Apache Ignite 3 Transactions Application

Transaction patterns and ACID guarantees using Apache Ignite 3's Transaction API.

**Related Documentation**: [Transaction Fundamentals](../../docs/04-distributed-operations/01-transaction-fundamentals.md)

## Overview

Demonstrates Apache Ignite 3's Transaction API for ACID operations across distributed data. Shows transaction lifecycle management, API integration, async patterns, and error handling strategies.

## Applications

| Application | Description | Run Command |
|-------------|-------------|-------------|
| TransactionAPIDemo | Orchestrator that runs all transaction demonstrations in educational progression from basic ACID operations through advanced business workflow patterns. | `../gradlew runTransactionAPIDemo` |
| BasicTransactions | Demonstrates fundamental transaction lifecycle including explicit and functional transaction patterns with proper error handling and rollback scenarios. | `../gradlew runBasicTransactions` |
| AsyncTransactions | Shows asynchronous transaction patterns using CompletableFuture for non-blocking operations with proper error handling and recovery. | `../gradlew runAsyncTransactions` |
| BatchTransactions | Focuses on bulk operations within transactions including batch inserts, updates, and error handling for partial failures. | `../gradlew runBatchTransactions` |
| TransactionIsolation | Demonstrates transaction isolation concepts, concurrent behavior, and write conflict scenarios between concurrent transactions. | `../gradlew runTransactionIsolation` |
| BusinessWorkflowPatterns | Implements production-ready patterns including customer purchase workflows, timeout patterns, retry logic, and circuit breaker implementations. | `../gradlew runBusinessWorkflowPatterns` |

### Running the Applications

From this directory, use Gradle to run each application:

```bash
# Run complete Transaction API demo (all examples)
../gradlew runTransactionAPIDemo

# Run individual demonstrations
../gradlew runBasicTransactions
../gradlew runAsyncTransactions
../gradlew runBatchTransactions
../gradlew runTransactionIsolation
../gradlew runBusinessWorkflowPatterns

# Custom cluster address
../gradlew runTransactionAPIDemo --args="192.168.1.100:10800"
```

## Application Details

### 1. BasicTransactionDemo

Demonstrates fundamental transaction patterns:

- Explicit transaction management (begin/commit/rollback)
- Closure-based transactions with `runInTransaction()`
- TransactionOptions configuration
- Integration with Table API and SQL API
- Exception handling patterns

### 2. AsyncTransactionDemo  

Shows asynchronous transaction patterns:

- Async transaction beginning and completion
- Chaining async operations within transactions
- Parallel operations in transactions
- Async functional transactions with `runInTransactionAsync()`
- Error handling and timeout patterns

### 3. TransactionPatterns

Advanced real-world scenarios:

- Mixing Table API and SQL API within transactions
- Batch operations with transaction management
- Complex business workflows with multiple entities
- Transaction state management and monitoring
- Performance optimization patterns

### 4. BusinessWorkflowPatterns

Production transaction patterns:

- Multi-table purchase workflows
- Transaction timeout configuration
- Lifecycle management
- Retry logic implementation
- Circuit breaker patterns
- Customer purchase validation

### 5. TransactionAPIDemo

Main demonstration application that runs all examples in sequence.

## Key Concepts Demonstrated

- Transaction lifecycle: begin, commit, rollback
- IgniteTransactions manager interface
- TransactionOptions configuration
- Closure-based transaction patterns
- Table API and SQL API integration
- Exception handling and rollback
- Async transaction patterns
- Performance optimization

## Prerequisites

- Apache Ignite 3 cluster running (see [00-docker setup](../00-docker/README.md))
- Sample data setup completed ([01-sample-data-setup](../01-sample-data-setup/))
- Java 17 or higher
- Maven 3.8+ or Gradle (via wrapper)


## API Patterns Covered

### Transaction Lifecycle

```java
// Explicit management
Transaction tx = client.transactions().begin();
try {
    // operations
    tx.commit();
} catch (Exception e) {
    tx.rollback();
}

// Functional pattern
client.transactions().runInTransaction(tx -> {
    // operations
    return true; // commit
});
```

### Async Transactions

```java
CompletableFuture<Void> future = client.transactions().beginAsync()
    .thenCompose(tx -> {
        // async operations
        return tx.commitAsync();
    });
```

### Transaction Options

```java
TransactionOptions options = new TransactionOptions()
    .timeoutMillis(30000)
    .readOnly(false);

Transaction tx = client.transactions().begin(options);
```

## Music Store Examples

### Album Creation Workflow

```java
// Create artist, album, and tracks atomically
client.transactions().runInTransaction(tx -> {
    RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
    RecordView<Album> albumView = client.tables().table("Album").recordView(Album.class);
    
    Artist artist = new Artist(100, "Led Zeppelin");
    artistView.upsert(tx, artist);
    
    Album album = new Album(200, "Led Zeppelin IV", 100);
    albumView.upsert(tx, album);
    
    return true;
});
```

### Customer Order Processing

```java
// Process customer order with invoice and line items
client.transactions().runInTransaction(tx -> {
    // Create invoice
    Invoice invoice = new Invoice();
    invoice.setCustomerId(customerId);
    invoice.setTotal(BigDecimal.ZERO);
    invoiceView.upsert(tx, invoice);
    
    // Add line items
    BigDecimal total = BigDecimal.ZERO;
    for (Track track : selectedTracks) {
        InvoiceLine line = new InvoiceLine();
        line.setInvoiceId(invoice.getInvoiceId());
        line.setTrackId(track.getTrackId());
        line.setUnitPrice(track.getUnitPrice());
        invoiceLineView.upsert(tx, line);
        
        total = total.add(track.getUnitPrice());
    }
    
    // Update total
    invoice.setTotal(total);
    invoiceView.upsert(tx, invoice);
    
    return true;
});
```
