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

**Required**: Complete [sample-data-setup](../01-sample-data-setup/) for data and understanding of colocation.

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

## Deployment Script Reference

This module includes `deploy-jar.sh`, a reference script for deploying JAR files to Ignite clusters via REST API.

### Script Features

- **REST API Integration**: Automatically deploys JARs using Ignite 3 management API
- **Error Handling**: Graceful handling of connection and deployment errors
- **Status Validation**: Check deployment progress and final status
- **Fallback Options**: Provides CLI alternatives when REST API fails
- **Cluster Configuration**: Support for different hosts, ports, and deployment modes
- **User-Friendly Output**: Colored output with clear progress indication

### Usage Examples

```bash
# Basic deployment
./deploy-jar.sh my-jobs 1.0.0 target/my-jobs.jar

# Deploy with all options
./deploy-jar.sh \
  --host 192.168.1.100 \
  --port 10300 \
  --mode ALL \
  --check \
  --remove \
  --verbose \
  data-processors 2.1.0 processors.jar

# Status check only
./deploy-jar.sh --check my-jobs 1.0.0 ""
```

### Script Options

| Option | Description | Default |
|--------|-------------|---------|
| `-h, --host` | Cluster hostname | localhost |
| `-p, --port` | REST API port | 10300 |
| `-m, --mode` | Deploy mode (MAJORITY/ALL) | MAJORITY |
| `-c, --check` | Check deployment status | false |
| `-r, --remove` | Remove existing before deploy | false |
| `-v, --verbose` | Enable detailed output | false |
| `--validate` | Validate cluster state pre-deployment | false |
| `--monitor` | Monitor compute jobs post-deployment | false |
| `--metrics` | Enable compute metrics | false |
| `--help` | Show complete usage | - |

### Advanced Features

**Cluster Validation (`--validate`):**

- Verifies cluster initialization status
- Checks CMG (Cluster Management Group) nodes
- Validates cluster topology and node availability
- Prevents deployment to unhealthy clusters

**Enhanced Status Checking (`--check`):**

- Cluster-wide deployment status verification
- Per-node deployment verification
- Version-specific status tracking
- Deployment consistency validation

**Compute Job Monitoring (`--monitor`):**

- Lists all active compute jobs
- Shows job status, ID, and creation time
- Monitors job execution after deployment
- Helps troubleshoot job execution issues

**Metrics Enablement (`--metrics`):**

- Enables compute-specific cluster metrics
- Supports performance monitoring and diagnostics
- Prepares cluster for job execution monitoring
- Facilitates troubleshooting and optimization

**Complete Workflow Example:**

```bash
# Full deployment with all features
./deploy-jar.sh \
  --validate \
  --metrics \
  --check \
  --monitor \
  --verbose \
  compute-jobs 1.0.0 target/app.jar

# Development workflow with validation
./deploy-jar.sh --validate --check my-jobs 1.0.0 target/jobs.jar && mvn exec:java

# Production deployment with monitoring
./deploy-jar.sh -h prod-cluster.company.com --validate --metrics --monitor production-jobs 2.0.0 app.jar
```

### Integration Patterns

The script can be integrated into various workflows:

**Development Workflow:**

```bash
mvn package && ./deploy-jar.sh compute-jobs 1.0.0 target/app.jar
```

**CI/CD Pipeline:**

```bash
./deploy-jar.sh -h $CLUSTER_HOST -c production-jobs $BUILD_VERSION $JAR_PATH
```

**Multi-Environment Deployment:**

```bash
for env in dev staging prod; do
  ./deploy-jar.sh -h ${env}-cluster.company.com my-jobs 1.0.0 app.jar
done
```

## Quick Start

**Build and run all demos:**

```bash
# Build JAR and deploy to cluster
mvn package
./deploy-jar.sh compute-jobs 1.0.0 target/07-compute-api-app-1.0.0.jar

# Run demos
mvn compile exec:java
```

**Alternative - automatic deployment:**

```bash
# Build and run (application attempts automatic deployment)
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

**Using Deployment Script (Recommended):**

This module includes a deployment script that handles REST API deployment with fallback options:

```bash
# Deploy compute jobs JAR (automatic REST API deployment)
./deploy-jar.sh compute-jobs 1.0.0 target/07-compute-api-app-1.0.0.jar

# Deploy with status check
./deploy-jar.sh -c compute-jobs 1.0.0 target/07-compute-api-app-1.0.0.jar

# Deploy to specific cluster
./deploy-jar.sh -h 192.168.1.100 -p 10300 compute-jobs 1.0.0 target/07-compute-api-app-1.0.0.jar

# Replace existing deployment
./deploy-jar.sh -r compute-jobs 1.0.0 target/07-compute-api-app-1.0.0.jar

# Check deployment status only
./deploy-jar.sh -c compute-jobs 1.0.0 ""

# Show all options
./deploy-jar.sh --help
```

**Script Features:**

- **Automatic REST API deployment** with comprehensive error handling
- **Cluster validation** with topology verification and health checks
- **Enhanced status checking** with per-node deployment verification
- **Compute job monitoring** to track job execution after deployment
- **Metrics enablement** for performance monitoring and diagnostics
- **Colored output** with clear progress indication and feedback
- **Fallback instructions** when deployment fails or API is unavailable
- **Multi-cluster support** for different hosts, ports, and configurations
- **Development-friendly** verbose mode for troubleshooting

**Manual REST API (Alternative):**

```bash
# Deploy via HTTP REST API (port 10300)
curl -X POST \
  "http://localhost:10300/management/v1/deployment/units/compute-jobs/1.0.0?deployMode=MAJORITY" \
  -H "Content-Type: multipart/form-data" \
  -F "unitContent=@target/07-compute-api-app-1.0.0.jar"
```

**Using Docker CLI (Fallback):**

```bash
# Start containerized Ignite CLI
docker run --rm -it --network=host -e LANG=C.UTF-8 -e LC_ALL=C.UTF-8 apacheignite/ignite:3.0.0 cli

# Deploy job classes (inside container)
cluster unit deploy compute-jobs /path/to/target/07-compute-api-app-1.0.0.jar
```

**Using Local CLI (Fallback):**

```bash
# Deploy using local Ignite CLI installation
ignite cluster unit deploy compute-jobs target/07-compute-api-app-1.0.0.jar
```

### 3. Run Application

```bash
mvn compile exec:java
```

**Note**: The application automatically builds deployment instructions when job deployment is required. Development environments may work with empty deployment units if the cluster has classpath access to job classes.

## Java Version Compatibility

If you encounter **"unsupported class file version"** errors:

```text
!!! Job execution failed: com/apache/ignite/examples/compute/BasicComputeOperations$NodeInfoJob 
    has been compiled by a more recent version of the Java Runtime 
    (class file version 61.0), this version of the Java Runtime only recognizes 
    class file versions up to 55.0
```

This indicates a Java version mismatch between your application and cluster:

### Quick Solutions

**Option 1: Upgrade Cluster to Java 17** (Recommended)

```bash
# Ensure cluster runs on Java 17
export JAVA_HOME=/path/to/java-17
./ignite node start

# Or use Java 17 Docker image
docker run -p 10800:10800 -p 10300:10300 apacheignite/ignite:3.0.0-jdk17
```

**Option 2: Compile Application for Java 11**

```xml
<!-- In pom.xml -->
<properties>
    <maven.compiler.source>11</maven.compiler.source>
    <maven.compiler.target>11</maven.compiler.target>
</properties>
```

**Option 3: Use Maven Release Flag**

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <release>11</release>
    </configuration>
</plugin>
```

### Automatic Detection

The deployment script can detect version mismatches:

```bash
# Use --validate to check Java compatibility
./deploy-jar.sh --validate compute-jobs 1.0.0 target/07-compute-api-app-1.0.0.jar
```

**Run individual demos:**

```bash
# Basic patterns
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.compute.BasicComputeOperations"

# Advanced patterns with data locality
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.compute.AdvancedComputeOperations"

# Multi-step workflows
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.compute.ComputeJobWorkflows"
```

## Related Modules

- **Prerequisites**: [sample-data-setup](../sample-data-setup/) - Understanding colocation
- **Foundation**: [table-api-app](../table-api-app/) - Data access patterns
- **Integration**: [transactions-app](../transactions-app/) - Transactional compute
- **Advanced**: [data-streaming-app](../data-streaming-app/) - Stream processing
