# Compute API - Apache Ignite 3 Reference

**Distributed processing and job execution**

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

### 1. BasicComputeOperations - Job Submission Fundamentals

- **HelloWorldJob**: Simple distributed job execution
- **ArtistSearchJob**: Parameterized jobs with SQL queries
- **TrackCountJob**: Data aggregation across cluster
- **GenreAnalysisJob**: Complex multi-table analysis
- **Key Concepts**: Job creation, targeting strategies, async execution

### 2. AdvancedComputeOperations - Data Locality and Complex Patterns

- **ArtistAnalysisJob**: Colocated artist sales analysis using `JobTarget.colocated()`
- **CustomerAnalysisJob**: Customer data processing with data locality
- **ArtistSalesAnalysisJob**: Performance comparison between colocated and any-node execution
- **GenreMapJob**: Distributed MapReduce pattern implementation
- **ClusterHealthJob**: Broadcast execution across all nodes

### 3. ComputeJobWorkflows - Multi-step Business Process Automation

- **CustomerAnalyticsWorkflow**: Multi-step customer segmentation analysis
- **MusicRecommendationWorkflow**: Personalized recommendation pipeline
- **RevenueOptimizationWorkflow**: Business intelligence with metric calculation
- **Key Concepts**: Job coordination, workflow orchestration, result aggregation

### 4. ComputeAPIDemo - Complete Demonstration

Main orchestrator that runs all compute patterns in educational progression with detailed output formatting.

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

**Build and run all demos:**

```bash
mvn package
mvn compile exec:java
```

## Job Deployment Requirements

For standalone Ignite clusters, job classes must be deployed before execution. If you encounter "Deployment units list is empty" errors:

### 1. Build JAR with Job Classes

```bash
mvn package
```

This creates `target/07-compute-api-app-1.0.0.jar` containing all job implementations.

### 2. Deploy JAR to Cluster

**Using REST API (Automated - Recommended):**

The application automatically attempts REST API deployment. You can also deploy manually:

```bash
# Deploy via HTTP REST API (port 10300)
curl -X POST \
  "http://localhost:10300/management/v1/deployment/units/compute-jobs/1.0.0?deployMode=MAJORITY" \
  -H "Content-Type: multipart/form-data" \
  -F "unitContent=@target/07-compute-api-app-1.0.0.jar"
```

**Using Docker CLI:**

```bash
# Start containerized Ignite CLI
docker run --rm -it --network=host -e LANG=C.UTF-8 -e LC_ALL=C.UTF-8 apacheignite/ignite:3.0.0 cli

# Deploy job classes (inside container)
deployment deploy compute-jobs /path/to/target/07-compute-api-app-1.0.0.jar
```

**Using Local CLI:**

```bash
# Deploy using local Ignite CLI installation
ignite deployment deploy compute-jobs target/07-compute-api-app-1.0.0.jar
```

### 3. Run Application

```bash
mvn compile exec:java
```

**Note**: The application automatically builds deployment instructions when job deployment is required. Development environments may work with empty deployment units if the cluster has classpath access to job classes.

**Run individual demos:**

```bash
# Basic patterns
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.compute.BasicComputeOperations"

# Advanced patterns with data locality
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.compute.AdvancedComputeOperations"

# Multi-step workflows
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.compute.ComputeJobWorkflows"
```

## Development Status

✅ **ALIGNED WITH CHAPTER 4.3** - Fully functional implementation demonstrating:
- ✅ Correct Apache Ignite 3 Compute API usage (`ComputeJob<T,R>`, `executeAsync`)
- ✅ Data colocation patterns with `JobTarget.colocated()` 
- ✅ Broadcast execution with `BroadcastJobTarget.nodes()`
- ✅ MapReduce patterns with proper map/reduce phases
- ✅ Job coordination and workflow orchestration
- ✅ Performance optimization through data locality

## Related Modules

- **Prerequisites**: [sample-data-setup](../sample-data-setup/) - Understanding colocation
- **Foundation**: [table-api-app](../table-api-app/) - Data access patterns
- **Integration**: [transactions-app](../transactions-app/) - Transactional compute
- **Advanced**: [data-streaming-app](../data-streaming-app/) - Stream processing
