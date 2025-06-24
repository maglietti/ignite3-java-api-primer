# File Streaming App

File-based backpressure streaming using Apache Ignite 3's reactive DataStreamer API.

## Overview

Demonstrates end-to-end backpressure propagation from file I/O to cluster ingestion. Shows how reactive streams control upstream data production to match downstream consumption capacity, preventing memory bloat during high-volume file processing.

## Key Concepts

- **File-based Streaming**: Line-by-line CSV processing with demand-driven reading
- **Backpressure Propagation**: Flow control from cluster through to file I/O operations
- **Memory Efficiency**: Reactive streams prevent large buffer accumulation
- **Resource Monitoring**: Real-time tracking of memory usage, CPU utilization, and system pressure
- **Performance Analysis**: Correlation between system resources and backpressure events

## Prerequisites

- Apache Ignite 3 cluster running on localhost:10800
- Java 17+
- Maven 3.6+
- Sample data setup completed (see `01-sample-data-setup`)

## Demonstrations

### File Backpressure Streaming (`FileBackpressureStreaming`)

- **Scenario 1**: Normal processing where cluster keeps up with file reading
- **Scenario 2**: Slow cluster processing demonstrating backpressure effects
- **Scenario 3**: High-velocity streaming with maximum throughput patterns

### Key Components

- **`SampleDataGenerator`**: Creates realistic music event CSV files for testing
- **`FileStreamingPublisher`**: Custom Flow.Publisher with demand-driven file reading
- **`StreamingMetrics`**: Comprehensive performance monitoring and rate tracking
- **`FileStreamingAPIDemo`**: Orchestrator demonstrating all file streaming patterns

## Educational Value

This module builds on the concepts from `08-data-streaming-app` by showing how backpressure naturally propagates to actual file I/O operations. Key learning outcomes:

- **Real-world scenarios**: File processing is a common high-volume use case
- **System coordination**: Multiple components working together via reactive streams
- **Memory management**: How Flow API prevents out-of-memory conditions
- **Performance adaptation**: Automatic rate adjustment based on cluster capacity

## Usage

Run the complete demonstration:

```bash
mvn compile exec:java
```

Run individual components:

```bash
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.filestreaming.FileBackpressureStreaming"
```

## Expected Output

The demonstration creates temporary CSV files (~150MB total) and shows:

1. **File Generation**: Creating realistic music event data
2. **Streaming Progress**: Real-time metrics showing file reading vs cluster ingestion rates
3. **Backpressure Events**: When and how file reading pauses due to cluster capacity
4. **Performance Reports**: Detailed analysis of throughput, memory usage, and flow control

Example metrics output:

```
Lines: 500,000 | Published: 485,000 | File Rate: 125,000 lines/sec | 
Publish Rate: 118,000 events/sec | Memory: 245.6 MB | CPU: 45.2% | 
Phase: NORMAL | Backpressure Events: 15
```

## Integration with Other Modules

- **Builds on**: `08-data-streaming-app` (basic streaming patterns)
- **Complements**: `05-sql-api-app` (data querying), `06-transactions-app` (data consistency)
- **Prepares for**: Advanced performance optimization and monitoring patterns

## File Management

- Temporary CSV files are created in system temp directory
- Files are automatically cleaned up after demonstrations
- Peak disk usage: ~150MB during largest scenario (1M records)
- Generated data follows music store schema for consistency
