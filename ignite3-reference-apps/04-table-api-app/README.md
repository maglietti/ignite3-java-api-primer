# Table API - Object-Oriented Data Access

This module demonstrates Apache Ignite 3's Table API for object-oriented data access patterns using the music store dataset.

📖 **Related Documentation**: [Table API - Object-Oriented Data Access](../../docs/03-data-access-apis/01-table-api-operations.md)

## Overview

The Table API provides a native object-oriented interface to Ignite data, eliminating the impedance mismatch between Java objects and database operations. This module showcases:

- **RecordView**: Complete object operations with POJOs
- **KeyValueView**: Cache-like key-value operations
- **Async Programming**: High-performance non-blocking patterns
- **Error Handling**: Production-ready resilience patterns

## Prerequisites

1. **Ignite Cluster**: Ensure a 3-node cluster is running
   ```bash
   cd ../00-docker
   ./init-cluster.sh
   ```

2. **Sample Data**: Load the music store schema and data
   ```bash
   cd ../01-sample-data-setup
   mvn compile exec:java
   ```

## Quick Start

### Run Complete Demo
```bash
mvn compile exec:java
```

### Run Individual Demonstrations

**RecordView Operations** (Object-oriented CRUD):
```bash
mvn compile exec:java@recordview
```

**KeyValueView Operations** (Cache-like patterns):
```bash
mvn compile exec:java@keyvalue
```

**Advanced Async Operations** (Production patterns):
```bash
mvn compile exec:java@async
```

## Reference Applications

### 1. RecordViewOperations.java
Demonstrates complete object-oriented data access patterns:

- **Basic CRUD**: Create, read, update, delete with POJOs
- **Complex Entities**: Composite keys and relationships
- **Bulk Operations**: High-performance batch processing
- **Transactions**: ACID guarantees across operations
- **Async Operations**: Non-blocking execution patterns

**Key Concepts**:
- Automatic POJO-to-tuple mapping
- Type safety and compile-time validation
- Colocation benefits with related data
- Transaction integration

### 2. KeyValueOperations.java
Showcases cache-like key-value access patterns:

- **Simple Operations**: Direct key-value access
- **Null Handling**: Explicit null value management
- **Conditional Operations**: putIfAbsent, replace patterns
- **Atomic Operations**: get-and-modify transactions
- **Tuple Operations**: Flexible schema handling

**Key Concepts**:
- Separation of keys and values
- NullableValue for explicit null handling
- Conditional and atomic updates
- Bulk operations for performance

### 3. AsyncTableOperations.java
Advanced async programming patterns for production:

- **Operation Chaining**: Complex async workflows
- **Parallel Execution**: Maximum resource utilization
- **Error Handling**: Comprehensive recovery strategies
- **Circuit Breaker**: Prevent cascade failures
- **Retry Logic**: Exponential backoff patterns
- **Performance Optimization**: Monitoring and tuning

**Key Concepts**:
- CompletableFuture composition
- Error classification and recovery
- Resilience patterns
- Performance monitoring

### 4. TableAPIDemo.java
Main demonstration application that orchestrates all patterns:

- **Guided Tour**: Step-by-step demonstration
- **Individual Demos**: Run specific demonstrations
- **Connectivity Verification**: Cluster health checks
- **Learning Summary**: Key takeaways and next steps

## Learning Path

### Beginner
1. Start with **RecordViewOperations** to understand object-oriented data access
2. Practice basic CRUD operations with the Artist entity
3. Explore composite keys with Album and Track entities

### Intermediate
1. Move to **KeyValueOperations** for cache-like patterns
2. Learn null value handling strategies
3. Practice conditional and atomic operations

### Advanced
1. Master **AsyncTableOperations** for production patterns
2. Implement error handling and resilience
3. Optimize performance with monitoring

## Key Decision Points

### When to Use Table API

✅ **Table API Excels For**:
- Known primary keys
- Single record operations
- Type safety requirements
- Object-oriented domain models
- High-performance point operations

⚠️ **Consider SQL API For**:
- Complex JOINs across tables
- Aggregate functions (COUNT, SUM, AVG)
- Range queries with WHERE clauses
- Dynamic queries at runtime
- Analytical operations

### RecordView vs KeyValueView

**Use RecordView When**:
- Working with complete entities
- Need full object mapping
- POJO-based development
- Complex entity relationships

**Use KeyValueView When**:
- Cache-like access patterns
- Simple key-value operations
- Working with partial data
- Need explicit null handling

## Performance Tips

1. **Batch Operations**: Use `upsertAll()`, `getAll()`, `deleteAll()` for multiple records
2. **Async Patterns**: Leverage async operations for concurrent workloads
3. **Colocation**: Ensure related data is colocated for optimal performance
4. **Connection Pooling**: Configure appropriate connection pool sizes
5. **Error Handling**: Implement circuit breakers and retry logic

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

## Testing

Run the test suite:
```bash
mvn test
```

The tests demonstrate:
- Unit testing patterns for Table API operations
- Integration testing with embedded Ignite
- Performance testing and benchmarking
- Error condition testing

## Troubleshooting

### Common Issues

1. **Connection Failed**: Ensure cluster is running at 127.0.0.1:10800
2. **Table Not Found**: Run sample-data-setup to create schema
3. **Schema Mismatch**: Verify POJO annotations match table schema
4. **Null Value Errors**: Use `getNullable()` for nullable columns

### Debug Mode
Enable debug logging:
```bash
mvn compile exec:java -Dlog4j.configurationFile=debug-log4j2.xml
```

## Next Steps

1. **Explore SQL API**: Learn when SQL excels over Table API
   - File: `../05-sql-api-app/`
   - Documentation: `../../docs/03-data-access-apis/02-sql-api-analytics.md`

2. **Master Transactions**: Understand distributed ACID guarantees
   - File: `../06-transactions-app/`
   - Documentation: `../../docs/04-distributed-operations/01-transaction-fundamentals.md`

3. **Build Your Application**: 
   - Use this module as a template
   - Apply patterns to your domain model
   - Combine with SQL API for complex queries

## Resources

- **Documentation**: `../../docs/03-data-access-apis/01-table-api-operations.md`
- **Model Classes**: `../01-sample-data-setup/src/main/java/.../model/`
- **Apache Ignite 3 Docs**: [Official Documentation](https://ignite.apache.org/docs/latest/)

The Table API provides the foundation for building robust, high-performance distributed applications with Apache Ignite 3. Master these patterns and you'll be well-equipped for most data access scenarios!