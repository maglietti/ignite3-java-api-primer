# Apache Ignite 3 Table API Application

Object-oriented data access patterns using Apache Ignite 3's Table API.

**Related Documentation**: [Table API Operations](../../docs/03-data-access-apis/01-table-api-operations.md)

## Overview

Demonstrates Apache Ignite 3's Table API for type-safe data access. Shows RecordView for object operations, KeyValueView for cache patterns, and async programming for non-blocking execution.

## Prerequisites

- Apache Ignite 3 cluster running (see [00-docker setup](../00-docker/README.md))
- Sample data setup completed ([01-sample-data-setup](../01-sample-data-setup/))
- Java 17 or higher
- Maven 3.8+ or Gradle (via wrapper)

## Quick Start

### Run Complete Demo

**Maven:**
```bash
mvn compile exec:java
```

**Gradle:**
```bash
../gradlew run
```

### Run Individual Demonstrations

**RecordView Operations** (Object-oriented CRUD):

**Maven:**
```bash
mvn compile exec:java@recordview
```

**Gradle:**
```bash
../gradlew runRecordViewExamples
```

**KeyValueView Operations** (Cache-like patterns):

**Maven:**
```bash
mvn compile exec:java@keyvalue
```

**Gradle:**
```bash
../gradlew runKeyValueExamples
```

**Advanced Async Operations** (Production patterns):

**Maven:**
```bash
mvn compile exec:java@async
```

**Gradle:**
```bash
../gradlew runAsyncBasicOperations
```

## Applications

| Application | Description | Run Command |
|-------------|-------------|-------------|
| TableAPIDemo | Main orchestrator that demonstrates all Table API capabilities including CRUD operations with Tuples, type-safe POJO operations, key-value access patterns, and asynchronous programming patterns. | `../gradlew runTableAPIDemo` |
| BasicTableOperations | Shows fundamental CRUD operations using the Tuple API for type-flexible database operations on the Artist table. | `../gradlew runBasicTableOperations` |
| RecordViewExamples | Demonstrates type-safe POJO-based operations using a custom Artist class for strongly-typed object operations. | `../gradlew runRecordViewExamples` |
| KeyValueExamples | Shows high-performance cache-like operations using KeyValueView including bulk operations and working with non-existent keys. | `../gradlew runKeyValueExamples` |
| AsyncBasicOperations | Demonstrates asynchronous patterns using CompletableFuture for non-blocking operations, error handling, and recovery strategies. | `../gradlew runAsyncBasicOperations` |

### Running the Applications

From this directory, use Gradle to run each application:

```bash
# Run complete Table API demo (all examples)
../gradlew runTableAPIDemo

# Run individual demonstrations
../gradlew runBasicTableOperations
../gradlew runRecordViewExamples  
../gradlew runKeyValueExamples
../gradlew runAsyncBasicOperations

# Custom cluster address
../gradlew runTableAPIDemo --args="192.168.1.100:10800"
```

## Application Details

### 1. RecordViewOperations

Object-oriented CRUD operations with POJOs:

- Basic CRUD operations
- Composite key handling
- Bulk operations for performance
- Transaction integration
- Async operation patterns

### 2. KeyValueOperations

Cache-like access patterns:

- Direct key-value operations
- Explicit null value handling
- Conditional updates (putIfAbsent, replace)
- Atomic operations
- Tuple-based flexible access

### 3. AsyncTableOperations

Async programming patterns:

- CompletableFuture chains
- Parallel execution
- Error handling strategies
- Circuit breaker implementation
- Retry with exponential backoff
- Performance monitoring

### 4. TableAPIDemo

Main orchestrator that runs all demonstrations:

- Complete Table API tour
- Individual demo execution
- Cluster connectivity checks


## Key Decision Points

### When to Use Table API

**Table API (RecordView)**: Known primary keys, POJO operations, type safety

**KeyValueView**: Cache patterns, partial data, explicit null handling

**SQL API**: Complex queries, aggregations, analytics, dynamic queries

**RecordView**: Complete entities, POJO mapping, object relationships

**KeyValueView**: Cache patterns, simple operations, partial data

## Performance Patterns

- Use batch operations (`upsertAll`, `getAll`) for multiple records
- Leverage async operations for concurrent workloads
- Ensure data colocation for related entities
- Implement circuit breakers for resilience

## Common Patterns

### Bulk Loading

```java
List<Artist> artists = loadArtists();
artistView.upsertAll(null, artists);
```

### Async Chain

```java
artists.getAsync(null, key)
    .thenCompose(artist -> updateArtist(artist))
    .thenAccept(result -> handleResult(result));
```

### Transaction

```java
client.transactions().runInTransaction(tx -> {
    artistView.upsert(tx, artist);
    albumView.upsertAll(tx, albums);
    return true;
});
```

### Error Handling

```java
try {
    artist = artistView.get(null, key);
} catch (MarshallerException e) {
    // Handle schema mismatch
} catch (UnexpectedNullValueException e) {
    // Handle null value error
}
```

## Troubleshooting

### Common Issues

1. **Connection Failed**: Ensure cluster is running at 127.0.0.1:10800
2. **Table Not Found**: Run sample-data-setup to create schema
3. **Schema Mismatch**: Verify POJO annotations match table schema
4. **Null Value Errors**: Use `getNullable()` for nullable columns
