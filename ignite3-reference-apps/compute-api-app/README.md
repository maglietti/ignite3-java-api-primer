# Compute API - Apache Ignite 3 Reference

**Distributed processing and job execution**

📖 **Related Documentation**: [Compute API - Distributed Processing](../../docs/07-compute-api-distributed-processing.md)

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

## Coming Soon

This reference application is in development. It will demonstrate:

### Basic Job Execution
```java
// Simple compute job
JobDescriptor<String, String> job = JobDescriptor.builder(MusicAnalysisJob.class)
    .units(DeploymentUnit.builder("music-jobs", "1.0.0").build())
    .build();

String result = client.compute().execute(
    JobTarget.anyNode(), 
    job, 
    "AC/DC"
);
```

### Data-Colocated Processing
```java
// Process data where it lives
JobTarget target = JobTarget.colocated("Artist", artistKey);
ArtistStats stats = client.compute().execute(
    target,
    artistAnalysisJob,
    artistKey
);
```

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

## Development Status

🚧 **In Development** - This module will be implemented as part of Phase 2B of the reference applications project.

## Related Modules

- **Prerequisites**: [sample-data-setup](../sample-data-setup/) - Understanding colocation
- **Foundation**: [table-api-app](../table-api-app/) - Data access patterns
- **Integration**: [transactions-app](../transactions-app/) - Transactional compute
- **Advanced**: [data-streaming-app](../data-streaming-app/) - Stream processing