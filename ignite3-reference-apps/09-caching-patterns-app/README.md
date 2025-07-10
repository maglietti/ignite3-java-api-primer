# Apache Ignite 3 Caching Patterns Application

Cache-aside, write-through, and write-behind patterns using Apache Ignite 3.

**Related Documentation**: [Caching Strategies](../../docs/05-performance-scalability/02-caching-strategies.md)

## Overview

Demonstrates caching patterns for performance optimization in distributed systems. Shows cache-aside for read-heavy workloads, write-through for consistency, and write-behind for high-throughput scenarios.

## What You'll Learn

- **Cache-Aside Pattern**: Application-controlled caching for read-heavy workloads
- **Write-Through Pattern**: Synchronous updates ensuring data consistency
- **Write-Behind Pattern**: High-throughput asynchronous updates for performance
- **Pattern Selection**: Choosing the right caching strategy for different data types
- **Consistency Management**: Maintaining data integrity across caching layers
- **Performance Optimization**: Maximizing throughput while ensuring reliability

## Prerequisites

- Apache Ignite 3 cluster running (see [00-docker setup](../00-docker/README.md))
- Sample data setup completed ([01-sample-data-setup](../01-sample-data-setup/))
- Java 17 or higher
- Maven 3.8+ or Gradle (via wrapper)

## Applications

**CachingAPIDemo** - Orchestrator that runs all caching pattern demonstrations in sequence, showing cache-aside, write-through, and write-behind patterns with different use cases.

**CacheAsidePatterns** - Demonstrates the cache-aside pattern for read-heavy catalog operations including cache miss handling, batch loading optimization, and async operations.

**WriteThroughPatterns** - Shows the write-through pattern for consistency-critical customer data operations with synchronous updates and ACID guarantees.

**WriteBehindPatterns** - Implements the write-behind pattern for high-throughput analytics event recording with immediate cache writes and background processing.

### Running the Applications

From this directory, use Gradle to run each application:

```bash
# Run complete Caching API demo (all patterns)
../gradlew runCachingAPIDemo

# Run individual demonstrations
../gradlew runCacheAsidePatterns
../gradlew runWriteThroughPatterns
../gradlew runWriteBehindPatterns

# Custom cluster address
../gradlew runCachingAPIDemo --args="192.168.1.100:10800"
```

## Application Details

### 1. CacheAsidePatterns

Application-controlled catalog caching:

- Manual cache management for catalog browsing
- Batch operations for data loading
- Cache warming strategies
- Async patterns for non-blocking operations

### 2. WriteThroughPatterns

Synchronous customer data updates:

- Profile synchronization across cache and data store
- Transaction management
- Error handling with rollback
- Consistency guarantees

### 3. WriteBehindPatterns

High-throughput analytics event recording:

- Analytics data buffering
- Background processing with flush intervals
- High-throughput event recording
- Buffer management and overflow handling

### 4. CachingAPIDemo

Complete orchestrator demonstrating all patterns:

- Combined pattern usage
- Progress reporting
- Performance monitoring
- Integration of all caching patterns


## Key Implementation Patterns

### Cache-Aside Implementation

```java
// Application manages cache explicitly
Artist artist = artistCache.get(artistId);
if (artist == null) {
    artist = loadFromDataStore(artistId);
    artistCache.put(artistId, artist);
}
return artist;
```

### Write-Through Implementation

```java
// Synchronous updates to both cache and data store
client.transactions().runInTransaction(tx -> {
    customerTable.put(tx, customerKey, customer);
    customerCache.put(customerKey, customer);
});
```

### Write-Behind Implementation

```java
// Immediate cache update, asynchronous data store update
eventBuffer.add(playEvent);
if (eventBuffer.size() >= batchSize) {
    asyncFlushToDataStore(eventBuffer);
}
```

## Performance Characteristics

**Cache-Aside**: Read-heavy workloads, eventual consistency, high read performance

**Write-Through**: Consistency-critical data, strong consistency, synchronous writes

**Write-Behind**: High-throughput writes, eventual consistency, minimal write latency

## Common Issues

**Cache misses**: Implement cache warming for frequently accessed data

**Write-behind overflow**: Monitor buffer sizes and adjust flush intervals

**Consistency issues**: Choose appropriate pattern based on data requirements
