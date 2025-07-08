# Apache Ignite 3 Data Streaming Application

High-throughput data ingestion patterns using Apache Ignite 3's DataStreamer API.

**Related Documentation**: [Data Streaming](../../docs/05-performance-scalability/01-data-streaming.md)

## Overview

Demonstrates reactive streams-based data streaming for music platform event ingestion. Shows fundamental patterns, bulk operations, and flow control mechanisms for handling millions of events efficiently.

## Key Concepts

- **DataStreamer API**: Reactive streaming with intelligent batching
- **Backpressure Handling**: Flow control using Java Flow API
- **Performance Tuning**: Batch sizing, parallelism, and flush intervals
- **Mixed Operations**: PUT and REMOVE operations in streaming context

## Prerequisites

- Apache Ignite 3 cluster running (see [00-docker setup](../00-docker/README.md))
- Sample data setup completed ([01-sample-data-setup](../01-sample-data-setup/))
- Java 17 or higher
- Maven 3.8+

## Demonstrations

### Basic Data Streaming (`BasicDataStreamerDemo`)

- Default streaming configuration
- Performance-tuned options with large batches
- Mixed PUT/REMOVE operations for listening sessions

### Bulk Data Ingestion (`BulkDataIngestion`)

- High-throughput streaming with large datasets
- File-based loading with memory efficiency
- Adaptive batch sizing for optimal performance

### Backpressure Handling (`BackpressureHandling`)

- Custom Flow.Publisher with backpressure support
- Adaptive rate limiting based on system load
- Buffer overflow scenarios and management

## Usage

Run the orchestrator to see all patterns:

```bash
mvn compile exec:java
```

Run individual demonstrations:

```bash
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.streaming.BasicDataStreamerDemo"
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.streaming.BulkDataIngestion"  
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.streaming.BackpressureHandling"
```

## Performance Characteristics

- **Basic Streaming**: 150K-200K events/sec
- **Bulk Ingestion**: 200K-300K records/sec  
- **File Loading**: 250K-350K records/sec
- **Adaptive Batching**: Variable based on batch size optimization

## Data Model

Uses music streaming event data:

- **TrackEvents**: Event logging with user interactions
- **BulkLoadTest**: Historical listening records
- **FileLoadTest**: File-based data migration
- **AdaptiveLoadTest**: Performance optimization data
- **BackpressureTest**: Flow control demonstrations
- **RateLimitTest**: Adaptive rate limiting data
- **OverflowTest**: Buffer overflow scenarios

Tables are created and cleaned up automatically for each demonstration.

## Configuration Options

### DataStreamerOptions Parameters

- `pageSize`: Batch size for operations (500-5000)
- `perPartitionParallelOperations`: Concurrency level (1-4)
- `autoFlushInterval`: Flush timing in milliseconds (200-2000)
- `retryLimit`: Retry attempts for failed operations (8-32)

### Performance Tuning

- Small batches (500): Lower latency, higher overhead
- Large batches (5000): Higher throughput, more memory usage
- High parallelism: Better throughput with cluster resources
- Fast flushing: Lower latency at cost of throughput

## Error Handling

Demonstrates production patterns:

- Resource management with try-with-resources
- Retry logic for transient failures
- Graceful degradation under load
- Memory pressure management
