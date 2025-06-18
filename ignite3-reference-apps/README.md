# Apache Ignite 3 Reference Applications

A collection of reference applications demonstrating Apache Ignite 3 Java API usage patterns using a consistent music store sample dataset.

## Project Overview

This multi-module Maven project provides practical, runnable examples for all major Apache Ignite 3 features:

- **Schema-as-Code** with annotations
- **Table API** for object-oriented data access
- **SQL API** for relational data operations
- **Transactions** and consistency patterns
- **Compute API** for distributed processing
- **Data Streaming** for high-throughput operations
- **Integration Patterns** with popular frameworks

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose

### 1. Start Ignite 3 Cluster

**⚠️ Required First Step**: Start and initialize the 3-node Docker cluster before running any applications.

**Quick setup** (recommended):
```bash
cd 00-docker
./init-cluster.sh
```

**Manual setup**:
```bash
cd 00-docker
docker-compose up -d
```

Wait for containers to be healthy, then initialize the cluster:
```bash
# Wait 30-60 seconds for startup
docker-compose ps

# Initialize cluster (required before first use)
curl -X POST http://localhost:10300/management/v1/cluster/init \
  -H "Content-Type: application/json" \
  -d '{
    "metaStorageNodes": ["node1", "node2", "node3"],
    "cmgNodes": ["node1", "node2", "node3"],
    "clusterName": "ignite3-reference-cluster"
  }'
```

Verify cluster is initialized and ready:
```bash
curl http://localhost:10300/management/v1/cluster/state
# {"cmgNodes":["node1","node2","node3"],"msNodes":["node1","node2","node3"],"igniteVersion":"3.0.0","clusterTag":{"clusterName":"ignite3-reference-cluster","clusterId":"b30643d3-34b0-4c2a-b8f7-e74c5f8ca316"}}
```

### 2. Setup Sample Data

1. **Start with complete initialization** (recommended for first-time users):

   ```bash
   cd 01-sample-data-setup
   mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.setup.app.ProjectInitializationApp"
   ```

2. **Or run components individually**:

   ```bash
   # Create schema only
   mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.setup.app.SchemaCreationApp"
   
   # Load data only (requires existing schema)
   mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.setup.app.DataLoadingApp"
   
   # Run analytics on existing data
   mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.setup.app.SampleAnalyticsApp"
   ```

### Project Structure

```text
ignite3-reference-apps/
├── 00-docker/                      # Docker cluster setup (start here!)
├── 01-sample-data-setup/           # Foundation module with sample data
├── 02-getting-started-app/         # QUICK START - just the key concepts
├── 03-schema-annotations-app/      # Schema-as-code examples
├── 04-table-api-app/               # Object-oriented data access
├── 05-sql-api-app/                 # SQL operations and queries
├── 06-transactions-app/            # Transaction patterns
├── 07-compute-api-app/             # Distributed computing
├── 08-data-streaming-app/          # High-throughput data loading
├── 09-caching-patterns-app/        # Caching strategies
└── 10-catalog-management-app/      # Schema and zone management
```

## Sample Dataset

All reference applications use a consistent **music store dataset** with 11 entities:

### Core Music Entities

- **Artist** → **Album** → **Track** (hierarchical with colocation)
- **Genre**, **MediaType** (reference data)

### Business Entities  

- **Customer** → **Invoice** → **InvoiceLine** (business workflow)
- **Employee** (organizational hierarchy)

### Playlist Entities

- **Playlist** → **PlaylistTrack** (many-to-many relationships)

### Distribution Strategy

- **MusicStore Zone** (2 replicas): Primary business data with colocation
- **MusicStoreReplicated Zone** (3 replicas): Reference/lookup data

## Key Features Demonstrated

### Schema-as-Code

```java
@Table(zone = @Zone(value = "MusicStore", storageProfiles = "default"),
       colocateBy = @ColumnRef("ArtistId"))
public class Album {
    @Id Integer AlbumId;
    @Id Integer ArtistId;
    @Column(value = "Title", nullable = false) String Title;
}
```

### Table API Operations

```java
RecordView<Artist> artists = client.tables()
    .table("Artist").recordView(Artist.class);
Artist artist = new Artist(1, "AC/DC");
artists.upsert(null, artist);
```

### SQL Operations

```java
var resultSet = client.sql().execute(null,
    "SELECT a.Name, COUNT(al.AlbumId) as AlbumCount " +
    "FROM Artist a LEFT JOIN Album al ON a.ArtistId = al.ArtistId " +
    "GROUP BY a.ArtistId, a.Name ORDER BY AlbumCount DESC");
```

### Transactional Operations

```java
client.transactions().runInTransaction(tx -> {
    Artist artist = artists.get(tx, artistKey);
    artist.setName("Updated Name");
    artists.put(tx, artistKey, artist);
});
```

## Module Usage

### 1. Sample Data Setup

**Purpose**: Foundation module providing sample data and utilities  
**Run**: `mvn exec:java` (uses ProjectInitializationApp by default)  
**Features**: Schema creation, data loading, analytics utilities

### 2. Getting Started

**Purpose**: Basic Ignite 3 operations and connection patterns  
**Prerequisites**: Completed sample-data-setup  
**Features**: Client connections, basic CRUD, simple queries

### 3. Schema Annotations

**Purpose**: Schema-as-code with annotation-driven table creation  
**Features**: POJO mapping, zone configuration, colocation strategies

### 4. Table API

**Purpose**: Object-oriented data access patterns  
**Features**: RecordView, KeyValueView, async operations, bulk operations

### 5. SQL API

**Purpose**: Relational data access and complex queries  
**Features**: DDL/DML operations, parameterized queries, result mapping

### 6. Transactions

**Purpose**: ACID transaction patterns and consistency  
**Features**: Explicit transactions, isolation levels, rollback handling

### 7. Compute API

**Purpose**: Distributed processing and job execution  
**Features**: Compute jobs, data colocation, job targeting

### 8. Data Streaming ✅

**Purpose**: High-throughput data loading and processing  
**Features**: DataStreamer API, Flow control, bulk ingestion, backpressure handling

### 9. Caching Patterns

**Purpose**: Caching pattern implementations with Ignite 3  
**Features**: Cache-aside, write-through, write-behind patterns, high-performance data access

### 10. Catalog Management

**Purpose**: Schema and zone management operations  
**Features**: Dynamic schema evolution, zone configuration, table administration

## Building and Running

### Build All Modules

```bash
mvn clean compile
```

### Run Specific Application

```bash
cd [module-name]
mvn exec:java -Dexec.mainClass="[MainClass]" -Dexec.args="[cluster-address]"
```

### Run with Custom Cluster Address

```bash
mvn exec:java -Dexec.args="192.168.1.100:10800"
```

## Configuration

### Default Settings

- **Cluster Address**: `127.0.0.1:10800`
- **Connection Timeout**: 30 seconds
- **Zone Replicas**: 2 (MusicStore), 3 (MusicStoreReplicated)
- **Partitions**: 25 per zone

### Customization

Edit configuration in:

- `sample-data-setup/src/main/java/com/apache/ignite/examples/setup/config/`
- Individual module application properties

## Troubleshooting

### Common Issues

1. **Connection Failed / Cluster Not Initialized**
   - Verify Ignite 3 cluster is running: `docker-compose ps`
   - **Most Common**: Cluster not initialized - run initialization:
     ```bash
     curl -X POST http://localhost:10300/management/v1/cluster/init \
       -H "Content-Type: application/json" \
       -d '{"metaStorageNodes": ["node1", "node2", "node3"], "cmgNodes": ["node1", "node2", "node3"], "clusterName": "ignite3-reference-cluster"}'
     ```
   - Check cluster address and port
   - Confirm network connectivity

2. **Table Not Found**
   - Run `01-sample-data-setup` module first
   - Verify schema creation completed successfully
   - Check zone configuration

3. **Data Not Found**
   - Ensure data loading completed
   - Verify using `SampleAnalyticsApp`
   - Check transaction commits

### Diagnostics

```bash
# Check cluster status
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.setup.app.SampleAnalyticsApp" -Dexec.args="127.0.0.1:10800 info"

# Verify schema
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.setup.app.SchemaCreationApp" -Dexec.args="127.0.0.1:10800 info"
```

## Learning Path

**Recommended order for learning Ignite 3:**

1. **sample-data-setup** - Understanding the dataset and basic setup
2. **getting-started-app** - Basic operations and connections  
3. **schema-annotations-app** - Schema definition and table creation
4. **table-api-app** - Object-oriented data access
5. **sql-api-app** - SQL operations and queries
6. **transactions-app** - ACID transactions and consistency
7. **compute-api-app** - Distributed processing
8. **data-streaming-app** - High-throughput operations
9. **caching-patterns-app** - Caching strategies and patterns
10. **catalog-management-app** - Schema and zone management

## Documentation

- [Apache Ignite 3 Documentation](https://ignite.apache.org/docs/ignite3/latest/)
- [Java API Reference](https://ignite.apache.org/docs/ignite3/latest/developers-guide/clients/java)
- [SQL Reference](https://ignite.apache.org/docs/3.0.0/sql-reference/ddl)

## Contributing

This is a learning-focused project. Contributions that improve clarity, add educational value, or demonstrate additional Ignite 3 patterns are welcome.

## License

Licensed under the Apache License 2.0 - see the [LICENSE](../LICENSE) file for details.
