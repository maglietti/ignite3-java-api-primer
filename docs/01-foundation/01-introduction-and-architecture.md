# Chapter 1.1: Introduction and Architecture Overview

## Learning Objectives

By completing this chapter, you will:

- Understand Apache Ignite 3's distributed computing capabilities and use cases
- Recognize the Java API architecture and multi-modal access patterns
- Distinguish between connection strategies and their appropriate applications
- Identify the evolution from Ignite 2 and key architectural improvements

## The Challenge: Building at Scale

Modern applications handle data at scales that traditional databases struggle to support. Consider a music streaming platform serving millions of users. Your application needs to:

- Store and retrieve artist catalogs instantaneously across global regions
- Process customer purchases with ACID guarantees while maintaining high throughput
- Generate real-time recommendations by analyzing listening patterns across distributed data
- Handle massive data ingestion as new tracks and user interactions flow continuously
- Scale seamlessly from thousands to millions of concurrent users

These requirements push beyond what traditional databases can deliver. You need a distributed computing platform designed for modern application demands.

## Apache Ignite 3: Distributed Computing Platform

Apache Ignite 3 addresses these challenges through a unified platform that combines:

- **In-Memory Data Grid**: Data resides in memory across cluster nodes, delivering microsecond access times with optional persistence for durability.

- **Distributed SQL Engine**: Execute ANSI-compliant SQL queries across distributed datasets with automatic join optimization and parallel execution.

- **NoSQL Key-Value Store**: Access data through type-safe object-oriented APIs optimized for high-performance single-record and batch operations.

- **Compute Engine**: Execute business logic directly on data nodes, eliminating network overhead and maximizing performance through colocation.

- **Streaming Engine**: Ingest and process high-velocity data streams with built-in backpressure handling and flow control.

### Evolution from Ignite 2

If you have worked with version 2 of Ignite in the past, Ignite 3 represents a fundamental redesign focused on modern developer experience and operational simplicity:

- **Cleaner Architecture**: Simplified codebase with clear separation between storage, compute, and API layers
- **Enhanced Schema Management**: Annotation-driven table creation eliminates configuration complexity
- **Improved Transaction Semantics**: Stronger consistency guarantees with intuitive programming models
- **Modern Java Integration**: Built-in support for CompletableFuture, type safety, and contemporary frameworks

## Java API Architecture

The Ignite 3 Java API follows three core design principles that simplify distributed programming:

### 1. Multi-modal Access Paradigms

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

Key-value API for cache-like operations:

```java
KeyValueView<Integer, String> artistCache = artistTable.keyValueView(Integer.class, String.class);
```

### 2. Async-First Design

Every operation supports both synchronous and asynchronous execution. Use synchronous operations for simplicity:

```java
Artist artist = artists.get(null, artistKey);
```

Use asynchronous operations for high-throughput scenarios:

```java
CompletableFuture<Artist> artistFuture = artists.getAsync(null, artistKey);
```

### 3. Strong Type Safety

Generic APIs ensure compile-time error detection and eliminate runtime type casting:

```java
RecordView<Artist> artists = table.recordView(Artist.class);
KeyValueView<Integer, String> artistNames = table.keyValueView(Integer.class, String.class);
```

## Connection Strategies

### IgniteClient - Remote Thin Client (Recommended)

The thin client provides optimal separation between application and storage tiers. Applications connect to cluster nodes without becoming part of the cluster topology:

```java
IgniteClient client = IgniteClient.builder()
    .addresses("127.0.0.1:10800", "127.0.0.1:10801", "127.0.0.1:10802")
    .build();
```

**Best Practice**: Specify all cluster node addresses for optimal performance. This enables direct partition mapping and eliminates extra network hops for data operations.

This approach scales applications independently from storage and simplifies deployment in containerized environments.

### IgniteServer - Embedded Node

Embedded nodes integrate applications directly into the cluster topology. Use this pattern when application logic requires maximum data locality:

```java
IgniteServer server = IgniteServer.start("node1", workDir, log4jConfig);
Ignite ignite = server.api();
```

### Ignite Interface - Unified API

Both connection types implement the same `Ignite` interface, enabling consistent programming patterns:

```java
// Works with both IgniteClient and server.api()
Ignite ignite = client; // or server.api()
Table artistTable = ignite.tables().table("Artist");
```

## Key Patterns You'll Master

### Data Colocation for Performance

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

### Asynchronous Programming

Handle high-performance scenarios through non-blocking operations:

```java
// Chain operations efficiently without blocking threads
artists.getAsync(null, artistKey)
    .thenCompose(artist -> albums.getAllAsync(null, artist.getAlbums()))
    .thenApply(this::calculateTotalDuration)
    .thenAccept(duration -> System.out.println("Total: " + duration));
```

### API Integration

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

- **Java 17+**: Modern JDK
- **Maven 3.8+**: Build and dependency management
- **Docker**: Version 20.10.0 or newer (12GB RAM recommended)
- **Docker Compose**: Version 2.23.1 or newer

**Installation**:

> [!NOTE]
> Docker installation is preferred but not required.

**For Unix-based systems (Linux, macOS)**: Use the Docker setup instructions at [Apache Ignite 3 Docker Installation Guide](https://ignite.apache.org/docs/ignite3/latest/installation/installing-using-docker).

**For Windows and other systems**: Follow the comprehensive installation instructions at [https://ignite.apache.org/docs/ignite3/latest/installation/](https://ignite.apache.org/docs/ignite3/latest/installation/) which covers all supported platforms and installation methods.

Alternative installation methods are available for environments where Docker is not suitable.

**Knowledge Assumptions**:

- Java fundamentals (collections, generics, streams)
- Basic SQL concepts (SELECT, JOIN, GROUP BY)
- General understanding of web application architecture

## Next Steps

Understanding the architecture provides the foundation for hands-on development.

Continue with:

- **[Chapter 1.2: Getting Started](02-getting-started.md)** - Connect to clusters and perform your first operations with a working example

- **[Chapter 1.3: Distributed Data Fundamentals](03-distributed-data-fundamentals.md)** - Learn the core concepts of distributed data management and zone configuration
