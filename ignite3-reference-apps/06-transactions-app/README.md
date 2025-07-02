# Module 06: Transactions App

This module demonstrates Apache Ignite 3 Transaction API patterns using music store sample data.

📖 **Related Documentation**: [Transactions](../../docs/04-distributed-operations/01-transaction-fundamentals.md)

## Overview

The Transaction API provides ACID guarantees for operations across multiple tables and nodes. This module shows practical patterns for transaction management including synchronous and asynchronous operations, error handling, and performance optimization.

## Applications

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

Production-ready transaction patterns for complex business scenarios:

- Multi-table purchase workflow coordination
- Transaction timeout configuration for different operation types
- Explicit transaction lifecycle management
- Resilient transaction processing with retry logic
- Circuit breaker patterns for system protection
- Customer purchase workflows with validation

### 5. TransactionAPIDemo

Main demonstration application that runs all examples in sequence.

## Key Concepts Demonstrated

- **Transaction Interface**: Begin, commit, rollback operations
- **IgniteTransactions**: Transaction manager and factory
- **TransactionOptions**: Timeout, read-only configuration
- **Closure-based Transactions**: Automatic commit/rollback management
- **API Integration**: Using transactions with Table API and SQL API
- **Error Handling**: Exception types and rollback scenarios
- **Async Patterns**: CompletableFuture composition with transactions
- **Performance**: Read-only transactions and batch operations

## Prerequisites

1. **Ignite Cluster**: Start the cluster using the docker module:

   ```bash
   cd ../00-docker
   docker-compose up -d
   ```

2. **Sample Data**: Set up the music store schema and data:

   ```bash
   cd ../01-sample-data-setup
   mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.setup.app.ProjectInitializationApp"
   ```

## Running the Examples

### Run All Examples

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.transactions.TransactionAPIDemo"
```

### Run Individual Examples

```bash
# Basic transaction patterns
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.transactions.BasicTransactionDemo"

# Async transaction patterns
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.transactions.AsyncTransactionDemo"

# Advanced transaction patterns
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.transactions.TransactionPatterns"

# Production business workflow patterns
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.transactions.BusinessWorkflowPatterns"
```

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

## Learning Outcomes

After running these examples, you will understand:

1. **Transaction Basics**: How to begin, commit, and rollback transactions
2. **API Integration**: Using transactions with both Table API and SQL API
3. **Async Patterns**: Managing asynchronous transaction workflows
4. **Error Handling**: Proper exception handling and rollback scenarios
5. **Performance**: When to use read-only transactions and batch operations
6. **Real-world Patterns**: Complex business workflows with multiple entities
