# Chapter 1.1: Introduction and Architecture Overview

Your music streaming platform just crashed. Again. This time it was 50,000 concurrent users trying to browse the latest album release. Your PostgreSQL instance maxed out at 100% CPU, connection pools exhausted, and users got timeout errors instead of music.

Your monitoring dashboard shows the grim reality: average query time spiked to 8 seconds, memory usage hit 95%, and your read replicas are 45 seconds behind master writes. The DBA is talking about "horizontal sharding with application-level routing logic" - which sounds expensive and complicated.

## Production Platform Scaling Requirements

Meanwhile, Spotify serves 400 million active users. Netflix streams globally without crashes. They're not running single PostgreSQL instances with read replicas. They use distributed architecture that scales horizontally.

```mermaid
graph TB
    subgraph "Growing Challenges"
        U1["1K Users<br/>Simple Queries"] --> U2["100K Users<br/>Complex Analytics"]
        U2 --> U3["1M Users<br/>Real-time Recommendations"]
        U3 --> U4["10M Users<br/>Global Distribution"]
    end
    
    subgraph "Technical Requirements"
        U4 --> R1["Microsecond Response Times"]
        U4 --> R2["ACID Transactions"]
        U4 --> R3["Massive Throughput"]
        U4 --> R4["Global Availability"]
    end
    
    subgraph "Traditional Database Limits"
        R1 --> L1["Single Point Bottleneck"]
        R2 --> L2["Limited Scaling"]
        R3 --> L3["I/O Constraints"]
        R4 --> L4["Complex Sharding"]
    end
```

Your application now needs to:

- **Store catalogs**: 50M+ tracks across multiple regions, accessible in milliseconds
- **Process purchases**: Handle 10K concurrent transactions with full ACID guarantees  
- **Generate recommendations**: Analyze patterns across petabytes of listening data in real-time
- **Ingest events**: Process millions of play events per hour without dropping data
- **Scale operations**: Handle traffic spikes during album releases and viral content

Traditional databases hit walls. You need a distributed computing platform built for these demands.

## Distributed-First Architecture Implementation

Traditional scaling approaches create operational complexity: read replicas with replication lag, sharded databases that break cross-shard joins, cache layers with invalidation race conditions, and message queues with delivery semantics.

Ignite 3 implements distributed-first architecture:

```mermaid
graph TB
    subgraph "Ignite 3 Architecture"
        subgraph "Application Layer"
            APP["Your Java Application"]
        end
        
        subgraph "API Layer"
            TABLE["Table API<br/>(Objects)"]
            SQL["SQL API<br/>(Relations)"]
            KV["Key-Value API<br/>(Cache)"]
        end
        
        subgraph "Compute & Storage"
            NODE1["Node 1<br/>Data + Compute"]
            NODE2["Node 2<br/>Data + Compute"] 
            NODE3["Node 3<br/>Data + Compute"]
        end
        
        APP --> TABLE
        APP --> SQL
        APP --> KV
        
        TABLE --> NODE1
        SQL --> NODE2
        KV --> NODE3
        
        NODE1 <--> NODE2
        NODE2 <--> NODE3
        NODE1 <--> NODE3
    end
```

### Unified API Architecture

Instead of managing separate database connections, cache clusters, and compute frameworks:

```java
// Traditional approach - multiple systems to manage
Database db = connectToDatabase();
Cache cache = connectToCache(); 
ComputeCluster compute = connectToCompute();

// Ignite 3 - unified platform
IgniteClient ignite = IgniteClient.builder()
    .addresses("node1:10800", "node2:10800", "node3:10800")
    .build();

// All capabilities through one connection
Table<Artist> artists = ignite.tables().table("Artist").recordView(Artist.class);
SqlStatement analytics = ignite.sql().statementBuilder().query("SELECT...");
JobExecution<String> recommendation = ignite.compute().submit(nodes, job, args);
```

### Core Platform Capabilities

**In-memory distributed storage** - Your catalog resides in cluster memory across multiple nodes with microsecond access latencies and optional disk persistence for durability.

**Distributed SQL execution** - Standard SQL queries execute across the entire cluster topology automatically, processing data where it resides without network transfer overhead.

**Type-safe object operations** - Direct key-value access through Java APIs eliminates object-relational mapping complexity for known-key operations.

**Distributed compute execution** - Business logic executes directly on nodes containing relevant data, eliminating network serialization penalties.

**High-throughput streaming** - Event ingestion handles millions of events per second with automatic backpressure and flow control.

## Deployment Architecture Patterns

### Remote Client Pattern (Recommended)

```mermaid
graph TB
    subgraph "Remote Client Pattern (Recommended)"
        subgraph "Your Applications"
            APP1["Music API Service"]
            APP2["Recommendation Service"] 
            APP3["Analytics Dashboard"]
        end
        
        subgraph "Ignite Cluster"
            NODE1["Storage Node 1"]
            NODE2["Storage Node 2"]
            NODE3["Storage Node 3"]
        end
        
        APP1 -.->|"IgniteClient"| NODE1
        APP2 -.->|"IgniteClient"| NODE2
        APP3 -.->|"IgniteClient"| NODE3
        
        NODE1 <--> NODE2
        NODE2 <--> NODE3
        NODE1 <--> NODE3
    end
```

```mermaid
graph TB
    subgraph "Embedded Node Pattern"
        subgraph "Hybrid App-Storage Nodes"
            HYBRID1["App + Storage 1"]
            HYBRID2["App + Storage 2"]
            HYBRID3["App + Storage 3"]
        end
        
        HYBRID1 <--> HYBRID2
        HYBRID2 <--> HYBRID3
        HYBRID1 <--> HYBRID3
    end
```

### Remote Client Implementation

Your applications connect to the cluster but stay separate from it:

```java
// Clean separation - app and storage independent
IgniteClient client = IgniteClient.builder()
    .addresses("storage1:10800", "storage2:10800", "storage3:10800")
    .build();
```

**Optimal for:**

- Microservices (each service connects independently)
- Containers and Kubernetes deployments
- Development and testing
- When you want to scale apps and storage separately

**Implementation benefits:**

- Deploy new app versions without touching storage
- Scale applications based on traffic, storage based on data
- Simple operational model

### Embedded Node Implementation

Your application becomes part of the storage cluster:

```java
// App and storage lifecycle are joined
IgniteServer server = IgniteServer.start("myApp", configPath, workDir);
```

**Apply when:**

- Data locality is critical (compute runs where data lives)
- Legacy systems that can't be easily separated
- Single-deployment scenarios

**Implementation trade-offs:**

- App restarts affect cluster membership
- More complex deployment coordination
- Higher memory requirements

### Unified Programming Model

Both connection strategies implement the same Ignite interface, enabling consistent programming patterns regardless of deployment choice. This means you can develop with one pattern and deploy with another based on operational requirements.

## Multi-API Data Access Strategy

Ignite 3 provides three APIs to work with the same distributed data. Pick the right tool for each job:

```mermaid
graph LR
    subgraph "Your Java Application"
        LOGIC["Business Logic"]
    end
    
    subgraph "API Choice"
        TABLE["Table API<br/>Objects & Types"]
        SQL["SQL API<br/>Queries & Analytics"] 
        KV["Key-Value API<br/>Key-Value Operations"]
    end
    
    subgraph "Same Distributed Data"
        DATA["Artist, Album, Track<br/>Tables Across Cluster"]
    end
    
    LOGIC --> TABLE
    LOGIC --> SQL
    LOGIC --> KV
    
    TABLE --> DATA
    SQL --> DATA
    KV --> DATA
```

### API Selection Patterns

```java
// Table API - when you know the structure and want type safety
Artist artist = artists.get(null, artistKey);
artist.setName("Updated Name");
artists.upsert(null, artist);

// SQL API - when you need complex queries or analytics
var topTracks = client.sql().execute(null,
    "SELECT t.Name, COUNT(*) FROM Track t " +
    "JOIN InvoiceLine il ON t.TrackId = il.TrackId " + 
    "GROUP BY t.TrackId ORDER BY COUNT(*) DESC LIMIT 10");

// Key-Value API - when you want simple key-value operations
Tuple trackKey = Tuple.create().set("TrackId", 123);
Tuple trackData = tracks.get(null, trackKey); // Fast key lookup
```

### Asynchronous Operation Support

Every API supports both sync and async operations:

```java
// Synchronous - blocks until complete
Artist artist = artists.get(null, key);

// Asynchronous - returns immediately  
CompletableFuture<Artist> future = artists.getAsync(null, key);
future.thenApply(this::updateArtist)
      .thenCompose(updated -> artists.upsertAsync(null, updated))
      .thenRun(() -> System.out.println("Update complete"));
```

### Cross-API Type Safety

Your Java classes work consistently across all APIs:

```java
@Table(zone = @Zone("MusicStore"))
public class Artist {
    @Id Integer artistId;
    @Column String name;
    // ... constructors, getters, setters
}

// Same class works everywhere:
RecordView<Artist> tableView = ignite.tables().table("Artist").recordView(Artist.class);
ResultSet<Artist> sqlResults = ignite.sql().execute(null, "SELECT * FROM Artist", Artist.class);
```

## Distribution Zone Configuration

Distribution zones control how your data spreads across cluster nodes. Ignite 3 provides a default zone that works out of the box:

### Default Zone Implementation

When you create a table without specifying a zone, Ignite uses the default zone:

```java
@Table  // Uses default zone automatically
public class Artist {
    @Id Integer artistId;
    @Column String name;
}
```

**Configuration details:**

- **1 replica** (no backups)
- **25 partitions** (good for small to medium datasets)
- **All nodes included** (uses entire cluster)
- **Ready immediately** (no setup required)

**Optimal for:**

- Development and learning
- Proof-of-concept projects
- Getting started quickly

**Production consideration:** The default zone provides no fault tolerance. If a node fails, you lose data. For production workloads, you'll want custom zones with multiple replicas.

> **Need production-grade storage?** See [Storage System Architecture](../00-reference/storage-system-arch.md) for complete details on custom zones, partitioning strategies, and fault-tolerant configurations.

## Cluster Connection Strategies

How you connect to the cluster affects performance. Connecting to all nodes gives you the best experience:

```mermaid
graph TB
    subgraph "Single-Node Connection (Poor Performance)"
        APP1["Your Application"] 
        SINGLE["Single Connection<br/>to Node 1 only"]
        N1["Node 1"]
        N2["Node 2"] 
        N3["Node 3"]
        
        APP1 --> SINGLE
        SINGLE --> N1
        N1 -.->|"Extra hops"| N2
        N1 -.->|"Extra hops"| N3
        
        LIMIT["Limitations:<br/>• No partition awareness<br/>• All traffic through one node<br/>• Poor performance"]
    end
```

```mermaid
graph TB
    subgraph "Multi-Node Connection (Best Performance)"
        APP2["Your Application"]
        MULTI["Multi-Node Connection<br/>to All Cluster Nodes"]
        NN1["Node 1"]
        NN2["Node 2"] 
        NN3["Node 3"]
        
        APP2 --> MULTI
        MULTI --> NN1
        MULTI --> NN2
        MULTI --> NN3
        
        BENEFIT["Benefits:<br/>• Direct partition access<br/>• Automatic failover<br/>• Maximum performance"]
    end
```

### Multi-Node Connection Implementation

Always specify all cluster node addresses:

```java
// Good: Connect to all nodes for best performance
IgniteClient client = IgniteClient.builder()
    .addresses("node1:10800", "node2:10800", "node3:10800")
    .build();

// Poor: Single node creates bottlenecks  
IgniteClient client = IgniteClient.builder()
    .addresses("node1:10800")  // Only one node - bad performance
    .build();
```

**Implementation benefits:**

- **Direct access**: Your app connects directly to the node that holds the data
- **Automatic failover**: If one node goes down, your app keeps working
- **Load distribution**: Requests spread across all available nodes

## Implementation Pattern Summary

### Multi-Node Connection Pattern

```java
// Production pattern: specify all cluster nodes
IgniteClient client = IgniteClient.builder()
    .addresses("node1:10800", "node2:10800", "node3:10800")
    .build();
```

### Default Zone Pattern

```java
// Development pattern: use default zone for simplicity
@Table
public class Artist {
    @Id Integer artistId;
    @Column String name;
}
```

### API Selection Strategy

```java
// Table API for direct record access
Artist artist = artists.get(null, artistKey);

// SQL API for complex queries
var results = client.sql().execute(null, "SELECT * FROM Artist WHERE...");
```

## Technology Requirements

**Required software stack:**

- **Java 17+**: Modern JDK
- **Maven 3.8+**: Build and dependency management
- **Docker**: Version 20.10.0 or newer (12GB RAM recommended)
- **Docker Compose**: Version 2.23.1 or newer

**Installation approach:**

> [!NOTE]
> Docker installation is preferred but not required.

**For Unix-based systems (Linux, macOS)**: Use the Docker setup instructions at [Apache Ignite 3 Docker Installation Guide](https://ignite.apache.org/docs/ignite3/latest/installation/installing-using-docker).

**For Windows and other systems**: Follow the installation instructions at [https://ignite.apache.org/docs/ignite3/latest/installation/](https://ignite.apache.org/docs/ignite3/latest/installation/) which covers all supported platforms and installation methods.

Alternative installation methods are available for environments where Docker is not suitable.

**Required knowledge base:**

- Java fundamentals (collections, generics, streams)
- Basic SQL concepts (SELECT, JOIN, GROUP BY)
- General understanding of web application architecture

## Implementation Path

These architectural concepts and decision frameworks provide the foundation for hands-on development.

Continue with:

- **[Chapter 1.2: Your First Implementation](02-getting-started.md)** - Put these concepts into practice with a working Ignite 3 application using the default zone pattern

- **[Chapter 1.3: Distributed Data Fundamentals](03-distributed-data-fundamentals.md)** - Core concepts of distributed data management and advanced zone configuration patterns
