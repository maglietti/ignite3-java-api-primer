# 6. Transactions

## Transaction Basics (`IgniteTransactions`)

- ACID properties in Ignite
- Transaction isolation levels

## Synchronous Transactions

### Explicit Transaction Management

```java
Transaction tx = client.transactions().begin(new TransactionOptions().readOnly(false));
try {
    // Perform operations
    client.sql().execute(tx, stmt, 1, "Forest Hill");
    tx.commit();
} catch (Exception e) {
    tx.rollback();
    throw e;
}
```

### `runInTransaction()` Pattern

```java
client.transactions().runInTransaction(tx -> {
    Account account = accounts.get(tx, key);
    account.balance += 200.0d;
    accounts.put(tx, key, account);
});
```

## Asynchronous Transactions

### Async Transaction Handling

```java
CompletableFuture<Void> fut = client.transactions().beginAsync().thenCompose(tx ->
    accounts.getAsync(tx, key)
        .thenCompose(account -> {
            account.balance += 300.0d;
            return accounts.putAsync(tx, key, account);
        })
        .thenCompose(ignored -> tx.commitAsync())
);
fut.join();
```

### Combining with Async Operations

*[To be completed with async combination patterns]*

## Transaction Options

- Read-only transactions
- Timeout configuration
- Error handling and rollback