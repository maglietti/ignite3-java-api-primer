# Data Streaming - Apache Ignite 3 Reference

**High-throughput data loading and processing**

📖 **Related Documentation**: [Data Streaming](../../docs/05-performance-scalability/01-data-streaming.md)

## Overview

Master high-throughput data operations with Ignite 3's streaming capabilities. Learn efficient bulk loading, backpressure handling, and real-time data processing patterns using reactive streams and optimized batching.

## What You'll Learn

- **Data Streamers**: Efficient bulk data loading with DataStreamerItem operations
- **Backpressure Handling**: Flow control and adaptive rate limiting
- **Stream Processing**: Real-time data transformation with custom receivers
- **Error Recovery**: Graceful failure handling during streaming operations
- **Performance Tuning**: Optimize throughput with batch sizing and parallelism
- **Flow API Integration**: Reactive streams with natural backpressure

## Prerequisites

**Required**: Complete [sample-data-setup](../01-sample-data-setup/) to understand the data model and ensure tables exist.

## Reference Applications

This module contains streaming demonstrations:

### 1. BasicDataStreamerDemo
**Basic streaming patterns and configuration**

```java
// Simple track event streaming
RecordView<Tuple> trackEventsView = ignite.tables()
    .table("TrackEvents")
    .recordView();

DataStreamerOptions options = DataStreamerOptions.builder()
    .pageSize(1000)
    .perPartitionParallelOperations(2)
    .autoFlushInterval(1000)
    .retryLimit(16)
    .build();

try (SubmissionPublisher<DataStreamerItem<Tuple>> publisher = 
        new SubmissionPublisher<>()) {
    
    CompletableFuture<Void> future = trackEventsView.streamData(publisher, options);
    
    // Stream music events
    publisher.submit(DataStreamerItem.of(trackEvent)); // PUT operation
    publisher.submit(DataStreamerItem.removed(oldEvent)); // REMOVE operation
}
```

**Demonstrates:**
- DataStreamerItem operations (PUT/REMOVE)
- Performance tuning with configuration options
- Mixed operation streaming
- Resource management patterns

### 2. BulkDataIngestion
**High-volume data loading optimization**

```java
// Bulk loading configuration
DataStreamerOptions bulkOptions = DataStreamerOptions.builder()
    .pageSize(5000)                     // Large batches for throughput
    .perPartitionParallelOperations(4)  // High parallelism
    .autoFlushInterval(200)             // Fast flushing
    .retryLimit(32)                     // High retry limit
    .build();

// File-based bulk loading
Files.lines(Paths.get(csvFile))
    .skip(1) // Skip header
    .forEach(line -> {
        Tuple record = parseCSVLine(line);
        publisher.submit(DataStreamerItem.of(record));
    });
```

**Demonstrates:**
- High-throughput streaming configuration
- Memory-efficient file processing
- Adaptive batch sizing optimization
- Progress monitoring and performance metrics

### 3. BackpressureHandling
**Flow control and adaptive rate limiting**

```java
// Custom publisher with backpressure support
public class AdaptiveMusicEventPublisher implements Flow.Publisher<DataStreamerItem<Tuple>> {
    
    @Override
    public void subscribe(Flow.Subscriber<? super DataStreamerItem<Tuple>> subscriber) {
        subscriber.onSubscribe(new BackpressureSubscription(subscriber));
    }
    
    private class BackpressureSubscription implements Flow.Subscription {
        @Override
        public void request(long n) {
            // Deliver items with adaptive rate limiting
            deliverItems(n);
        }
    }
}
```

**Demonstrates:**
- Custom Flow.Publisher implementation
- Adaptive rate limiting based on system feedback
- Buffer management and overflow handling
- Producer-consumer coordination patterns

### 4. DataStreamingAPIDemo
**Complete demonstration orchestrator**

Runs all streaming demonstrations in sequence with progress reporting and key concept explanations.

## Music Store Scenarios

The reference applications demonstrate real-world streaming scenarios:

- **Track Event Ingestion**: Stream millions of play/pause/skip events
- **Historical Data Migration**: Bulk load listening history from CSV files
- **Real-time Analytics**: Process streaming user interactions
- **Catalog Import**: Efficiently load large music catalogs
- **Sales Data Processing**: Handle transaction streams with backpressure

## Performance Optimization

Key optimization techniques demonstrated:

- **Batch Sizing**: Testing different page sizes for optimal throughput
- **Parallelism Control**: Per-partition parallel operations tuning
- **Auto-flush Intervals**: Balancing latency vs throughput
- **Retry Logic**: Resilient streaming with configurable retry limits
- **Memory Management**: Buffer sizing and overflow prevention

## Running the Examples

### Quick Start
```bash
# Run complete demonstration
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.streaming.DataStreamingAPIDemo"

# Run individual demos
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.streaming.BasicDataStreamerDemo"
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.streaming.BulkDataIngestion"
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.streaming.BackpressureHandling"
```

### Performance Testing
The demonstrations include built-in performance monitoring showing:
- Events per second throughput
- Batch processing efficiency
- Memory usage patterns
- Error rates and retry statistics

## Related Modules

- **Prerequisites**: [sample-data-setup](../sample-data-setup/) - Data model understanding
- **Foundation**: [table-api-app](../table-api-app/) - Basic data operations
- **Integration**: [compute-api-app](../compute-api-app/) - Stream processing
- **Performance**: [best-practices-app](../best-practices-app/) - Optimization techniques