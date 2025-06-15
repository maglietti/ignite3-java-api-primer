# 1. Setting Up the Infrastructure - Foundation for a Distributed Music Platform

**Apache Ignite 3 Java API Primer**

Modern applications handle data at scales that traditional databases struggle to support. Users expect instant responses when browsing music catalogs, immediate updates when purchasing tracks, and real-time analytics that power recommendation engines. This primer guides you through building such a system using Apache Ignite 3's Java API.

## The Challenge: Building at Scale

Consider a music streaming platform serving millions of users. Your application needs to:

- Store and retrieve artist catalogs instantaneously across global regions
- Process customer purchases with ACID guarantees while maintaining high throughput
- Generate real-time recommendations by analyzing listening patterns across distributed data
- Handle massive data ingestion as new tracks and user interactions flow continuously
- Scale seamlessly from thousands to millions of concurrent users

These requirements push beyond what traditional databases can deliver. You need a distributed computing platform designed for modern application demands.

## Apache Ignite 3: Distributed Computing Platform

Apache Ignite 3 addresses these challenges through a unified platform that combines:

**In-Memory Data Grid**: Data resides in memory across cluster nodes, delivering microsecond access times with optional persistence for durability.

**Distributed SQL Engine**: Execute ANSI-compliant SQL queries across distributed datasets with automatic join optimization and parallel execution.

**NoSQL Key-Value Store**: Access data through type-safe object-oriented APIs optimized for high-performance single-record and batch operations.

**Compute Engine**: Execute business logic directly on data nodes, eliminating network overhead and maximizing performance through colocation.

**Streaming Engine**: Ingest and process high-velocity data streams with built-in backpressure handling and flow control.

### Evolution from Ignite 2

Ignite 3 represents a fundamental redesign focused on developer experience and operational simplicity:

- **Cleaner Architecture**: Simplified codebase with clear separation between storage, compute, and API layers
- **Enhanced Schema Management**: Annotation-driven table creation eliminates configuration complexity
- **Improved Transaction Semantics**: Stronger consistency guarantees with intuitive programming models
- **Modern Java Integration**: Built-in support for CompletableFuture, type safety, and contemporary frameworks

## Java API Architecture

The Ignite 3 Java API follows three core design principles that simplify distributed programming:

### 1. **Dual Access Paradigms**

Choose the appropriate API for each use case. Object-oriented access through the Table API handles structured data operations efficiently:

```java
RecordView<Artist> artists = client.tables()
    .table("Artist").recordView(Artist.class);
Artist artist = new Artist(1, "AC/DC");
artists.upsert(null, artist);
```

Relational access through the SQL API handles complex queries and analytics:

```java
var resultSet = client.sql().execute(null,
    "SELECT Name FROM Artist WHERE ArtistId = ?", 1);
```

### 2. **Async-First Design**

Every operation supports both synchronous and asynchronous execution. Use synchronous operations for simplicity:

```java
Artist artist = artists.get(null, artistKey);
```

Use asynchronous operations for high-throughput scenarios:

```java
CompletableFuture<Artist> artistFuture = artists.getAsync(null, artistKey);
```

### 3. **Strong Type Safety**

Generic APIs ensure compile-time error detection and eliminate runtime type casting:

```java
RecordView<Artist> artists = table.recordView(Artist.class);
KeyValueView<Integer, String> artistNames = table.keyValueView(Integer.class, String.class);
```

## Connection Strategies

### **IgniteClient** - Remote Thin Client (Recommended)

The thin client provides optimal separation between application and storage tiers. Applications connect to cluster nodes without becoming part of the cluster topology:

```java
IgniteClient client = IgniteClient.builder()
    .addresses("127.0.0.1:10800")
    .build();
```

This approach scales applications independently from storage and simplifies deployment in containerized environments.

### **IgniteServer** - Embedded Node

Embedded nodes integrate applications directly into the cluster topology. Use this pattern when application logic requires maximum data locality:

```java
IgniteServer server = IgniteServer.start("node1", workDir, log4jConfig);
Ignite ignite = server.api();
```

### **Ignite Interface** - Unified API

Both connection types implement the same `Ignite` interface, enabling consistent programming patterns:

```java
// Works with both IgniteClient and server.api()
Ignite ignite = client; // or server.api()
Table artistTable = ignite.tables().table("Artist");
```

## Learning Through Practice: Music Store Implementation

This primer uses a music store dataset to demonstrate concepts through realistic scenarios. You'll implement features that actual streaming platforms require:

**Phase 1 - Foundation**: Connect to clusters, define schemas, and perform basic data operations using Artist and Album entities.

**Phase 2 - Core Operations**: Master object-oriented and relational data access patterns while building customer purchase workflows.

**Phase 3 - Advanced Features**: Implement recommendation engines through distributed computing and handle real-time data ingestion.

Each concept builds upon previous work while remaining independently valuable. You can focus on specific areas (such as transactions or streaming) without completing every section sequentially.

## Development Environment Setup

### Quick Start (5 minutes)

**1. Start your Ignite 3 cluster**:

```bash
cd ignite3-reference-apps/00-docker
./init-cluster.sh
```

**2. Initialize the music store dataset**:

```bash
cd ../01-sample-data-setup
mvn compile exec:java
```

**3. Verify cluster functionality**:

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.setup.app.SampleAnalyticsApp"
```

You should see music store data and analytics reports confirming proper cluster operation.

### What You Built

- **3-Node Cluster**: Production-ready distributed setup with automatic failover
- **Music Store Schema**: 11 tables modeling realistic music business relationships
  - **Core Entities**: Artists, Albums, Tracks with hierarchical relationships
  - **Business Data**: Customers, Invoices, Purchase history
  - **Reference Data**: Genres, Media Types, Playlists
- **Optimized Distribution**: Data colocation strategies that minimize network overhead
- **Sample Applications**: Working examples for each API component

## Learning Path Structure

### **Phase 1: Foundation** (Modules 1-3)

Build core understanding and development confidence:

**[Module 1: Setting Up Infrastructure](01-introduction-overview.md)** ← *You are here*
- Understand platform capabilities and connection patterns
- Configure development environment with music store dataset

**[Module 2: Your First Success](02-getting-started.md)** - Immediate hands-on experience
- Connect to clusters and perform basic operations
- Works standalone for developers seeking quick success

**[Module 3: Designing for Scale](03-schema-as-code-with-annotations.md)** - Distributed schema patterns
- Define tables using annotations and colocation strategies
- Build foundation for high-performance data operations

### **Phase 2: Core Operations** (Modules 4-6)

Master the fundamental data access and consistency patterns:

**[Module 4: Managing Your Data](04-table-api-object-oriented-data-access.md)** - Object-oriented operations
- Handle structured data through type-safe APIs

**[Module 5: Analyzing Your Business](05-sql-api-relational-data-access.md)** - SQL analytics and reporting
- Execute complex queries and handle result processing

**[Module 6: Ensuring Consistency](06-transactions.md)** - ACID guarantees
- Implement reliable business processes with distributed transactions

### **Phase 3: Advanced Capabilities** (Modules 7-10)

Scale beyond basic operations with distributed intelligence:

**[Module 7: Intelligence at Scale](07-compute-api-distributed-processing.md)** - Distributed processing
- Build recommendation engines and analytics pipelines

**[Module 8: Real-Time Responsiveness](08-data-streaming-high-throughput-ingestion.md)** - Streaming data
- Handle high-velocity data ingestion and processing

**[Module 9: Performance Optimization](09-caching-patterns-java-implementations.md)** - Caching strategies
- Implement patterns that optimize user experience

**[Module 10: Operational Excellence](10-schema-and-catalog-management.md)** - Schema management
- Handle schema evolution and catalog operations in production

## Key Patterns You'll Master

### **Data Colocation for Performance**

Optimize query performance by storing related data together:

```java
// Album data colocates with Artist data for efficient joins
@Table(zone = @Zone(value = "MusicStore"), 
       colocateBy = @ColumnRef("ArtistId"))
public class Album {
    @Id Integer AlbumId;
    @Id Integer ArtistId;  // Colocation key
    String Title;
}
```

### **Asynchronous Programming**

Handle high-performance scenarios through non-blocking operations:

```java
// Chain operations efficiently without blocking threads
artists.getAsync(null, artistKey)
    .thenCompose(artist -> albums.getAllAsync(null, artist.getAlbums()))
    .thenApply(this::calculateTotalDuration)
    .thenAccept(duration -> System.out.println("Total: " + duration));
```

### **API Integration**

Combine Table and SQL APIs for optimal solutions:

```java
// Fast single-record operations through Table API
Artist artist = artists.get(null, artistKey);

// Complex analytics through SQL API
var topTracks = client.sql().execute(null,
    "SELECT t.Name, COUNT(il.Quantity) as Purchases " +
    "FROM Track t JOIN InvoiceLine il ON t.TrackId = il.TrackId " +
    "GROUP BY t.TrackId ORDER BY Purchases DESC LIMIT 10");
```

## Prerequisites

**Required Technology**:
- **Java 17+**: Modern JDK with Ignite 3 compatibility
- **Maven 3.8+**: Build and dependency management
- **Docker**: Cluster infrastructure (12GB RAM recommended)

**Knowledge Assumptions**:
- Java fundamentals (collections, generics, streams)
- Basic SQL concepts (SELECT, JOIN, GROUP BY)
- General understanding of web application architecture

## Module Navigation

Each module corresponds to a reference application that demonstrates concepts through working code:

| Documentation Module | Reference Application | Implementation Focus |
|----------------------|----------------------|----------|
| [Getting Started](02-getting-started.md) | `02-getting-started-app/` | Connection and basic operations |
| [Schema Design](03-schema-as-code-with-annotations.md) | `03-schema-annotations-app/` | Annotation-driven table creation |
| [Table API](04-table-api-object-oriented-data-access.md) | `04-table-api-app/` | Object-oriented data access |
| [SQL API](05-sql-api-relational-data-access.md) | `05-sql-api-app/` | Relational queries and analytics |
| [Transactions](06-transactions.md) | `06-transactions-app/` | ACID consistency patterns |

Each module includes concepts, working examples, and reference applications you can run immediately.

## Next Steps

**Start with practical foundations**:

1. **[Module 2: Getting Started](02-getting-started.md)** - Connect to clusters and perform basic operations
2. **Practice immediately**: Run the `02-getting-started-app` to see concepts in action
3. **Explore the dataset**: Use `SampleAnalyticsApp` to understand music store relationships

**Need help?** Check the [Troubleshooting Guide](14-troubleshooting-guide.md) or verify cluster setup in `00-docker/`.

---

**The foundation is ready.** Let's build distributed applications that scale with demand.