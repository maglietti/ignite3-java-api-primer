# Compute API - Apache Ignite 3 Reference

**Distributed processing and job execution**

> [!WARNING]
> This module is known to be incomplete, does not compile, and has known bugs.
>
> ❌ DO NOT reference code from this module until this warning is removed.

📖 **Related Documentation**: [Compute API - Distributed Processing](../../docs/04-distributed-operations/03-compute-api-processing.md)

## Overview

Harness Ignite 3's distributed computing capabilities to process data where it lives. Learn job execution, data colocation, and distributed algorithms using the music store dataset.

## What You'll Learn

- **Compute Jobs**: Execute code on cluster nodes
- **Data Colocation**: Process data where it's stored for performance
- **Job Targeting**: Control where jobs execute
- **Distributed Algorithms**: Implement MapReduce patterns
- **Error Handling**: Deal with node failures and retries
- **Performance Optimization**: Maximize throughput with parallel processing

## Prerequisites

**Required**: Complete [sample-data-setup](../sample-data-setup/) for data and understanding of colocation.

## Reference Applications

This module provides compute demonstrations:

### 1. BasicComputeDemo - Job Submission Fundamentals

- **TrackDurationJob**: Calculate total duration for track collections
- **NameProcessingJob**: CPU-intensive string processing  
- **AlbumStatsJob**: Multi-table analysis with SQL integration
- **Key Concepts**: Job creation, targeting strategies, result handling

### 2. ColocationComputeDemo - Data-Local Execution

- **ArtistAnalysisJob**: Colocated artist sales analysis
- **LocalTrackStatsJob**: Broadcast statistics gathering
- **CustomerPurchaseAnalysisJob**: Customer data processing
- **GenreAnalysisMapJob**: MapReduce pattern implementation

### 3. AsyncComputePatterns - Non-blocking Execution  

- **TrackAnalysisJob**: Async track data processing
- **ArtistSalesJob**: Parallel artist analysis
- **LongRunningAnalysisJob**: Job monitoring and control
- **GenreCountJob + DetailedGenreAnalysisJob**: Workflow orchestration

### 4. MusicStoreJobs - Business Intelligence

- **CustomerRecommendationJob**: Personalized music recommendations
- **SalesAnalysisMapJob**: Distributed sales performance analysis
- **ContentPopularityJob**: Track and genre popularity trends
- **RevenueOptimizationJob**: Revenue analysis and optimization

### 5. ComputeAPIDemo - Complete Demonstration

Main application that runs all compute patterns in sequence with detailed output formatting.

### Music Store Use Cases

- **Artist Analysis**: Calculate statistics for artist catalogs
- **Genre Analytics**: Process tracks by genre across the cluster
- **Sales Reporting**: Distributed revenue calculations
- **Recommendation Engine**: Collaborative filtering algorithms
- **Data Aggregation**: Roll up statistics from partitioned data

### MapReduce Patterns

- **Map Phase**: Distribute analysis across partitions
- **Reduce Phase**: Aggregate results from multiple nodes
- **Custom Jobs**: Implement business-specific algorithms
- **Parallel Processing**: Maximize CPU utilization

### Job Management

- Job deployment and versioning
- Error handling and retry logic
- Progress monitoring and cancellation
- Resource management

## Quick Start

**Run all demos:**

```bash
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.compute.ComputeAPIDemo"
```

**Run individual demos:**

```bash
# Basic patterns
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.compute.BasicComputeDemo"

# Data locality patterns  
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.compute.ColocationComputeDemo"

# Async execution patterns
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.compute.AsyncComputePatterns"

# Business intelligence jobs
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.compute.MusicStoreJobs"
```

## Development Status

✅ **COMPLETED** - Full implementation with compute patterns and real-world analytics jobs.

## Related Modules

- **Prerequisites**: [sample-data-setup](../sample-data-setup/) - Understanding colocation
- **Foundation**: [table-api-app](../table-api-app/) - Data access patterns
- **Integration**: [transactions-app](../transactions-app/) - Transactional compute
- **Advanced**: [data-streaming-app](../data-streaming-app/) - Stream processing
