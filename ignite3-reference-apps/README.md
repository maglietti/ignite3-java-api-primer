# Apache Ignite 3 Reference Applications

A comprehensive collection of reference applications demonstrating Apache Ignite 3 Java API usage patterns using a consistent music store sample dataset.

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
- Apache Ignite 3 cluster running (see [Ignite 3 Documentation](https://ignite.apache.org/docs/3.0.0/))
- Maven 3.8+

### Setup Sample Data

1. **Start with complete initialization** (recommended for first-time users):

   ```bash
   cd sample-data-setup
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
├── sample-data-setup/          # Foundation module with sample data
├── getting-started-app/        # Basic Ignite 3 operations
├── schema-annotations-app/     # Schema-as-code examples
├── table-api-app/             # Object-oriented data access
├── sql-api-app/               # SQL operations and queries
├── transactions-app/          # Transaction patterns
├── compute-api-app/           # Distributed computing
├── data-streaming-app/        # High-throughput data loading
├── caching-patterns-app/      # Caching strategies
├── catalog-management-app/    # Schema and zone management
├── advanced-topics-app/       # Error handling, monitoring
├── integration-patterns-app/  # Spring Boot, JPA integration
├── best-practices-app/        # Performance and testing
└── troubleshooting-app/       # Diagnostics and debugging
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

### 8. Data Streaming

**Purpose**: High-throughput data loading and processing  
**Features**: Data streamers, backpressure handling, error recovery

### 9. Integration Patterns

**Purpose**: Integration with popular Java frameworks  
**Features**: Spring Boot auto-configuration, JPA integration, microservices

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

1. **Connection Failed**
   - Verify Ignite 3 cluster is running
   - Check cluster address and port
   - Confirm network connectivity

2. **Table Not Found**
   - Run `sample-data-setup` module first
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
9. **integration-patterns-app** - Framework integration
10. **advanced-topics-app** - Production patterns
11. **best-practices-app** - Performance optimization
12. **troubleshooting-app** - Monitoring and debugging

## Documentation

- [Apache Ignite 3 Documentation](https://ignite.apache.org/docs/ignite3/latest/)
- [Java API Reference](https://ignite.apache.org/docs/ignite3/latest/developers-guide/clients/java)
- [SQL Reference](https://ignite.apache.org/docs/3.0.0/sql-reference/ddl)

## Contributing

This is a learning-focused project. Contributions that improve clarity, add educational value, or demonstrate additional Ignite 3 patterns are welcome.

## License

Licensed under the Apache License 2.0 - see the [LICENSE](../LICENSE) file for details.
