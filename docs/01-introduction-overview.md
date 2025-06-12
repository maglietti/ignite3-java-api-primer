# 1. Introduction & Overview

**Apache Ignite 3 Java API Primer**

Welcome to the most comprehensive, hands-on guide for mastering Apache Ignite 3's Java API. This primer combines theoretical understanding with practical, runnable examples using a music store dataset that you'll work with throughout your learning journey.

## What You'll Accomplish

By the end of this primer, you'll confidently:

- **Connect and interact** with Ignite 3 clusters using the Java client API
- **Design distributed schemas** using schema-as-code patterns with annotations
- **Optimize data placement** with colocation strategies and distribution zones
- **Execute complex queries** combining SQL and object-oriented data access
- **Handle transactions** with ACID guarantees across distributed nodes
- **Process data at scale** using distributed computing and streaming APIs
- **Integrate Ignite 3** with Spring Boot, JPA, and microservice architectures
- **Troubleshoot and optimize** production Ignite 3 applications

## What is Apache Ignite 3

Apache Ignite 3 is a next-generation distributed computing platform that combines:

- **In-Memory Data Grid**: Lightning-fast data access with optional persistence
- **Distributed SQL Engine**: ANSI-compliant SQL with distributed joins and aggregations
- **NoSQL Key-Value Store**: High-performance object-oriented data access
- **Compute Engine**: Co-locate processing with data for maximum performance
- **Streaming Engine**: Handle high-velocity data ingestion and processing

**Key Improvements in Ignite 3 from Ignite 2**:

- Simplified architecture with cleaner separation of concerns
- Enhanced schema management with annotation-driven table creation
- Improved transaction semantics and consistency guarantees
- Better integration with modern Java frameworks and patterns

## Java API Architecture

The Ignite 3 Java API provides a clean, modern interface designed around three core principles:

### 1. **Dual Access Paradigms**

**Table API** (Object-Oriented):

```java
RecordView<Artist> artists = client.tables()
    .table("Artist").recordView(Artist.class);
Artist artist = new Artist(1, "AC/DC");
artists.upsert(null, artist);
```

**SQL API** (Relational):

```java
var resultSet = client.sql().execute(null,
    "SELECT Name FROM Artist WHERE ArtistId = ?", 1);
```

### 2. **Async-First Design**

Every operation supports both synchronous and asynchronous execution:

```java
// Synchronous
Artist artist = artists.get(null, artistKey);

// Asynchronous
CompletableFuture<Artist> artistFuture = artists.getAsync(null, artistKey);
```

### 3. **Strong Type Safety**

Generic APIs ensure compile-time type checking:

```java
RecordView<Artist> artists = table.recordView(Artist.class);
KeyValueView<Integer, String> artistNames = table.keyValueView(Integer.class, String.class);
```

## API Entry Points

### **IgniteClient** - Remote Thin Client (Recommended)

Perfect for application development and microservices:

```java
IgniteClient client = IgniteClient.builder()
    .addresses("127.0.0.1:10800")
    .build();
```

### **IgniteServer** - Embedded Node

For compute-intensive applications requiring data locality:

```java
IgniteServer server = IgniteServer.start("node1", workDir, log4jConfig);
Ignite ignite = server.api();
```

### **Ignite Interface** - Unified API

Common interface for both client and server modes:

```java
// Works with both IgniteClient and server.api()
Ignite ignite = client; // or server.api()
Table artistTable = ignite.tables().table("Artist");
```

## Learning Approach: Theory + Practice

This primer uses a **dual-approach learning methodology**:

1. **Conceptual Understanding** (like this documentation)
2. **Hands-On Practice** (Reference applications with runnable code)

Each section pairs theoretical concepts with practical, executable examples using a consistent music store dataset. You'll work with the same Artists, Albums, Tracks, and Customers throughout your learning journey, building familiarity while mastering different aspects of the Ignite 3 API.

## Getting Started: Your Learning Environment

**⚠️ Important**: Before diving into the concepts, set up your development environment using the reference applications. This ensures you can immediately practice what you learn.

### Quick Start (5 minutes)

1. **Start your Ignite 3 cluster**:

   ```bash
   cd ignite3-reference-apps/00-docker
   ./init-cluster.sh
   ```

2. **Setup the sample music store dataset**:

   ```bash
   cd ../01-sample-data-setup
   mvn compile exec:java
   ```

3. **Verify everything works**:

   ```bash
   mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.setup.app.SampleAnalyticsApp"
   ```

You should see music store data and analytics reports, confirming your environment is ready.

### What You Just Created

- **3-Node Ignite 3 Cluster**: Production-like distributed setup
- **Music Store Dataset**: 11 tables with realistic relationships
  - **Artists** (AC/DC, Led Zeppelin, Black Sabbath...)
  - **Albums** and **Tracks** with metadata
  - **Customers**, **Invoices**, and **Purchase Data**
  - **Playlists** and **Reference Data** (Genres, Media Types)
- **Distribution Strategy**: Optimized data placement with colocation
- **Sample Applications**: Ready-to-run examples for each API area

### Navigation Between Docs and Code

Each documentation section corresponds to a reference application:

| Documentation Section | Reference Application | Purpose |
|----------------------|----------------------|----------|
| [Getting Started](02-getting-started.md) | `02-getting-started-app/` | Basic operations |
| [Schema as Code](03-schema-as-code-with-annotations.md) | `03-schema-annotations-app/` | Table creation |
| [Table API](04-table-api-object-oriented-data-access.md) | `04-table-api-app/` | Object-oriented access |
| [SQL API](05-sql-api-relational-data-access.md) | `05-sql-api-app/` | Relational operations |
| [Transactions](06-transactions.md) | `06-transactions-app/` | ACID guarantees |
| [Compute API](07-compute-api-distributed-processing.md) | `07-compute-api-app/` | Distributed processing |

Each section includes:

- **Concepts**: Core principles and theory
- **Code Examples**: Inline code snippets  
- **Reference App**: Link to runnable application
- **Practice Exercises**: Suggested modifications and experiments

### 📋 Prerequisites

**Required**:

- **Java 17+**: Modern JDK for Ignite 3 compatibility
- **Maven 3.8+**: For building and running reference applications
- **Docker**: For the 3-node Ignite cluster (12GB RAM recommended)

**Recommended Experience**:

- Basic Java knowledge (collections, generics, CompletableFuture)
- SQL fundamentals (SELECT, JOIN, GROUP BY)
- Understanding of distributed systems concepts (helpful but not required)

## Your Learning Journey Roadmap

### **Phase 1: Foundation** (Sections 1-3)

**Goal**: Understand Ignite 3 basics and get comfortable with the development environment

1. **[Introduction & Overview](01-introduction-overview.md)** ← *You are here*
2. **[Getting Started](02-getting-started.md)** - First connections and basic operations
3. **[Schema as Code](03-schema-as-code-with-annotations.md)** - Define distributed tables with annotations

**Practice**: Run `02-getting-started-app` and `03-schema-annotations-app`

### **Phase 2: Core APIs** (Sections 4-6)

**Goal**: Master the two primary data access patterns and transaction handling

4. **[Table API](04-table-api-object-oriented-data-access.md)** - Object-oriented data access
5. **[SQL API](05-sql-api-relational-data-access.md)** - Relational queries and operations
6. **[Transactions](06-transactions.md)** - ACID guarantees and consistency

**Practice**: Build a complete music store management system using both APIs

### **Phase 3: Advanced Features** (Sections 7-9)

**Goal**: Scale beyond basic operations with distributed computing and caching

7. **[Compute API](07-compute-api-distributed-processing.md)** - Distributed processing jobs
8. **[Data Streaming](08-data-streaming.md)** - High-throughput data loading
9. **[Caching Patterns](09-caching-patterns.md)** - Performance optimization strategies

**Practice**: Implement music recommendation engine with distributed analytics

### **Phase 4: Production Readiness** (Sections 10-14)

**Goal**: Production deployment, integration, and operational excellence

10. **[Schema & Catalog Management](10-schema-and-catalog-management.md)** - Schema evolution
11. **[Advanced Topics](11-advanced-topics.md)** - Error handling and monitoring
12. **[Integration Patterns](12-integration-patterns.md)** - Spring Boot, JPA, microservices
13. **[Best Practices](13-best-practices-common-patterns.md)** - Performance and testing
14. **[Troubleshooting Guide](14-troubleshooting-guide.md)** - Diagnostics and debugging

**Practice**: Deploy a production-ready music streaming service

## Key Concepts You'll Master

### **Distribution & Colocation**

Learn how to optimize performance by co-locating related data:

```java
// Artist data becomes the "anchor" for related Album and Track data
@Table(zone = @Zone(value = "MusicStore"), 
       colocateBy = @ColumnRef("ArtistId"))
public class Album {
    @Id Integer AlbumId;
    @Id Integer ArtistId;  // Colocation key
    String Title;
}
```

### **Async Programming Patterns**

Handle high-performance scenarios with non-blocking operations:

```java
// Chain async operations efficiently
artists.getAsync(null, artistKey)
    .thenCompose(artist -> albums.getAllAsync(null, artist.getAlbums()))
    .thenApply(this::calculateTotalDuration)
    .thenAccept(duration -> System.out.println("Total: " + duration));
```

### **Cross-API Integration**

Combine Table API and SQL for optimal solutions:

```java
// Use Table API for fast single-record operations
Artist artist = artists.get(null, artistKey);

// Use SQL for complex analytical queries
var topTracks = client.sql().execute(null,
    "SELECT t.Name, COUNT(il.Quantity) as Purchases " +
    "FROM Track t JOIN InvoiceLine il ON t.TrackId = il.TrackId " +
    "GROUP BY t.TrackId ORDER BY Purchases DESC LIMIT 10");
```

## Success Metrics

**By Section 6**, you should be able to:

- Connect to Ignite clusters and handle connection lifecycle
- Create and manage distributed tables with proper colocation
- Perform CRUD operations using both Table and SQL APIs
- Handle transactions with proper error handling and rollbacks

**By Section 10**, you should be able to:

- Design and implement distributed processing jobs
- Handle high-velocity data streams efficiently
- Implement caching strategies for performance optimization
- Manage schema evolution in production environments

**By Section 14**, you should be able to:

- Integrate Ignite 3 with Spring Boot and microservice architectures
- Implement comprehensive monitoring and alerting
- Troubleshoot performance issues and optimize queries
- Deploy and operate Ignite 3 in production environments

## Next Steps

**Ready to begin?** Start with practical foundations:

1. **[Getting Started](02-getting-started.md)** - Learn connection patterns and basic operations
2. **Practice immediately**: Run `02-getting-started-app` to see concepts in action
3. **Explore the dataset**: Use `SampleAnalyticsApp` to understand the music store data model

**Having issues?** Check the [Troubleshooting Guide](14-troubleshooting-guide.md) or verify your cluster setup in `00-docker/`.

---

**Ready to rock with Apache Ignite 3?** Let's start building distributed applications that scale!
