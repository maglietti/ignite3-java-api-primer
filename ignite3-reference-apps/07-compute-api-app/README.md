# Apache Ignite 3 Compute API Application

Distributed job execution patterns for Apache Ignite 3, demonstrating how to build scalable compute workflows in a music streaming platform context.

## Overview

This application demonstrates Apache Ignite 3's Compute API through practical examples ranging from basic job submission to production-scale distributed processing patterns. Each module focuses on specific distributed computing concepts using real music store data.

## Prerequisites

- Apache Ignite 3 cluster running (see [00-docker setup](../00-docker/README.md))
- Sample data setup completed ([01-sample-data-setup](../01-sample-data-setup/))
- Java 17 or higher
- Maven 3.8+ or Gradle (via wrapper)

## Quick Start

**Maven:**
```bash
# Build the application
mvn clean package

# Run the complete demo
mvn exec:java

# Or run with custom cluster address
mvn exec:java -Dexec.args="192.168.1.100:10800"
```

**Gradle:**
```bash
# Build the application
./gradlew :07-compute-api-app:build

# Run the complete demo
../gradlew runComputeAPIDemo

# Run individual demonstrations
../gradlew runBasicComputeOperations
../gradlew runAdvancedComputeOperations
../gradlew runComputeJobWorkflows
../gradlew runProductionComputePatterns
../gradlew runMusicPlatformIntelligence

# Or run with custom cluster address
../gradlew runComputeAPIDemo --args="192.168.1.100:10800"
```

## Applications

| Application | Description | Run Command |
|-------------|-------------|-------------|
| ComputeAPIDemo | Main orchestrator that runs all compute examples in educational progression from basic job execution to production-scale distributed processing patterns. | `../gradlew runComputeAPIDemo` |
| BasicComputeOperations | Demonstrates fundamental job execution including simple jobs, parameterized jobs, SQL-based processing, and asynchronous execution patterns. | `../gradlew runBasicComputeOperations` |
| AdvancedComputeOperations | Shows advanced patterns including data colocation optimization, broadcast execution, MapReduce patterns, and performance comparisons. | `../gradlew runAdvancedComputeOperations` |
| ComputeJobWorkflows | Demonstrates complex job coordination and business workflow patterns including customer analytics and music recommendation workflows. | `../gradlew runComputeJobWorkflows` |
| ProductionComputePatterns | Implements production-scale patterns including large-scale recommendation engines, circuit breaker patterns, and performance monitoring. | `../gradlew runProductionComputePatterns` |
| MusicPlatformIntelligence | Shows documentation-aligned compute patterns for music streaming platforms with artist analytics and MapReduce trend analysis. | `../gradlew runMusicPlatformIntelligence` |

## Architecture

The application consists of six modules:

### 1. Basic Compute Operations (`BasicComputeOperations.java`)

- Simple job execution patterns
- Parameterized job inputs
- SQL-based distributed queries
- Asynchronous job execution

### 2. Advanced Compute Operations (`AdvancedComputeOperations.java`)

- Data-colocated job execution
- Broadcast patterns across all nodes
- Performance comparison of colocation strategies
- MapReduce coordination patterns

### 3. Compute Job Workflows (`ComputeJobWorkflows.java`)

- Multi-step business process automation
- Customer analytics pipelines
- Music recommendation workflows
- Revenue optimization analysis

### 4. Production Compute Patterns (`ProductionComputePatterns.java`)

- Large-scale recommendation engine processing
- Hierarchical data colocation strategies
- Circuit breaker patterns for resilience
- Performance monitoring and metrics

### 5. Music Platform Intelligence (`MusicPlatformIntelligence.java`)

- Artist popularity analysis
- User recommendation processing with data locality
- Concurrent user processing patterns
- MapReduce trend detection

## Key Concepts Demonstrated

### Data Locality and Colocation

Jobs execute on nodes where data resides, minimizing network overhead:

```java
JobTarget colocatedTarget = JobTarget.colocated("Artist", 
    Tuple.create(Map.of("ArtistId", 1)));
```

### Distributed MapReduce

Parallel processing across cluster nodes with result aggregation:

```java
// Map phase executes on each node
// Reduce phase aggregates results
```

### Resilient Job Execution

Circuit breaker patterns and retry logic for production reliability:

```java
// Exponential backoff retry
// Failure isolation and recovery
```

### Performance Optimization

- JSON serialization for cross-node compatibility
- Batch processing for large datasets
- Async execution patterns

## Data Model

The application uses music store sample data:

- **Artists** → Albums → Tracks (hierarchical relationship)
- **Customers** → Invoices → Invoice Lines (transactional data)
- **Genres** and **Media Types** (reference data)

All data is colocated by primary keys (ArtistId, CustomerId) for optimal performance.

## Technical Implementation

### Job Deployment

Jobs are packaged and deployed to the cluster via REST API. Deployment happens automatically on startup or can be done manually via the management API.

### Serialization Strategy

Complex types are serialized as JSON strings to ensure cross-node compatibility:

- POJOs → JSON strings
- Maps → JSON objects
- Lists → JSON arrays

### SQL Integration

All queries use Ignite's SQL API with prepared statements:

```java
Statement stmt = sql.statementBuilder()
    .query("SELECT ... WHERE ArtistId = ?")
    .build();
```

## Running Individual Modules

**BasicComputeOperations:**

**Maven:**
```bash
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.compute.BasicComputeOperations"
```

**Gradle:**
```bash
# Use Maven command above (custom Gradle task not configured)
```

**AdvancedComputeOperations:**

**Maven:**
```bash
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.compute.AdvancedComputeOperations"
```

**Gradle:**
```bash
# Use Maven command above (custom Gradle task not configured)
```

**Other modules:**
- ComputeJobWorkflows
- ProductionComputePatterns
- MusicPlatformIntelligence

Follow the same pattern with the appropriate class name.

## Performance Considerations

1. **Data Colocation**: Jobs execute where data resides
2. **Batch Processing**: Process multiple records per job
3. **Connection Pooling**: Reuse SQL connections within jobs
4. **Result Aggregation**: Minimize data transfer between nodes

## Troubleshooting

### ClassNotFoundException

Ensure JAR is deployed to cluster:

```bash
mvn clean package
# Check deployment status in logs
```

### Column Not Found

Ignite 3 normalizes all SQL metadata to uppercase:

- Use `row.stringValue("NAME")` not `row.stringValue("name")`
- Column aliases are also uppercased

### Marshalling Exceptions

Complex types use JSON serialization:

- Jobs return String instead of POJOs
- Parse JSON in result processors

**Related Documentation**: [Compute API Processing](../../docs/04-distributed-operations/03-compute-api-processing.md)
