# Transactions - Apache Ignite 3 Reference

**ACID transaction patterns and consistency**

📖 **Related Documentation**: [Transactions](../../docs/06-transactions.md)

## Overview

Learn Ignite 3's transaction capabilities for ensuring data consistency in distributed systems. Master ACID properties, isolation levels, and transactional patterns.

## What You'll Learn

- **ACID Transactions**: Atomicity, Consistency, Isolation, Durability
- **Transaction APIs**: Explicit and implicit transaction patterns
- **Isolation Levels**: Read committed and serializable isolation
- **Rollback Handling**: Error recovery and transaction rollback
- **Cross-Partition Transactions**: Distributed transaction coordination
- **Performance Optimization**: Transaction best practices

## Prerequisites

**Required**: Complete [sample-data-setup](../sample-data-setup/) for tables and sample data.

## Coming Soon

This reference application is in development. It will demonstrate:

### Explicit Transactions
```java
// Manual transaction control
Transaction tx = client.transactions().begin();
try {
    artists.upsert(tx, artist);
    albums.upsert(tx, album);
    tracks.upsert(tx, track);
    tx.commit();
} catch (Exception e) {
    tx.rollback();
    throw e;
}
```

### Functional Transactions
```java
// Lambda-based transactions (recommended)
client.transactions().runInTransaction(tx -> {
    // All operations are automatically committed or rolled back
    artists.upsert(tx, artist);
    albums.upsert(tx, album);
    return "Success";
});
```

### Async Transactions
```java
// Non-blocking transaction patterns
CompletableFuture<Void> future = client.transactions().beginAsync()
    .thenCompose(tx -> 
        artists.upsertAsync(tx, artist)
            .thenCompose(ignored -> albums.upsertAsync(tx, album))
            .thenCompose(ignored -> tx.commitAsync())
    );
```

### Music Store Scenarios
- **Album Creation**: Create artist, album, and tracks atomically
- **Purchase Processing**: Customer orders with invoice and line items
- **Playlist Management**: Add/remove tracks from playlists consistently
- **Inventory Updates**: Track sales and inventory changes
- **Error Recovery**: Handle conflicts and retry patterns

### Isolation Levels
- Read Committed transactions
- Serializable isolation for critical operations
- Conflict detection and resolution
- Deadlock prevention patterns

## Development Status

🚧 **In Development** - This module will be implemented as part of Phase 2B of the reference applications project.

## Related Modules

- **Prerequisites**: [sample-data-setup](../sample-data-setup/) - Sample data required
- **Foundation**: [table-api-app](../table-api-app/) - Basic operations
- **Integration**: [sql-api-app](../sql-api-app/) - Transactional SQL
- **Advanced**: [compute-api-app](../compute-api-app/) - Distributed transactions