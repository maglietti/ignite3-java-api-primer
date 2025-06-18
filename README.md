# Apache Ignite 3 - Java API Primer

Learn to build distributed applications with Apache Ignite 3's Java API through structured modules and working reference applications. This primer guides Java developers from basic concepts to advanced distributed programming patterns using a consistent music store dataset.

## Who This Is For

Java developers new to Ignite 3 who want to master distributed data management through practical, runnable examples.

## Quick Start

**Prerequisites**: Java 17+, Maven 3.8+, Docker

- **Start Ignite 3 Cluster**:

```bash
cd ignite3-reference-apps/00-docker
./init-cluster.sh
```

- **Initialize Sample Data**:

```bash
cd ../01-sample-data-setup
mvn compile exec:java
```

- **Start Learning**: Follow the structured learning path below or jump to specific topics.

---

## Self-Paced Learning

**Recommended Path**: Follow this sequence for optimal learning progression

**Phase 1: Foundations** *(Start here for systematic learning)*

- **[Module 01: Foundation](./docs/01-foundation/)** - Essential distributed systems concepts
   - Introduction and Architecture → Getting Started → Data Fundamentals
   - **Reference App**: [`02-getting-started-app/`](./ignite3-reference-apps/02-getting-started-app/)

- **[Module 02: Schema Design](./docs/02-schema-design/)** - Schema-as-code mastery  
   - Basic Annotations → Relationships → Advanced Patterns → Evolution
   - **Reference App**: [`03-schema-annotations-app/`](./ignite3-reference-apps/03-schema-annotations-app/)

**Phase 2: Core APIs** *(Build on foundations)*
-. **[Module 03: Data Access APIs](./docs/03-data-access-apis/)** - Data manipulation patterns
   - Table API → SQL API → Selection Guide
   - **Reference Apps**: [`04-table-api-app/`](./ignite3-reference-apps/04-table-api-app/), [`05-sql-api-app/`](./ignite3-reference-apps/05-sql-api-app/)

- **[Module 04: Distributed Operations](./docs/04-distributed-operations/)** - Advanced operations
   - Transaction Fundamentals → Advanced Patterns → Compute API
   - **Reference Apps**: [`06-transactions-app/`](./ignite3-reference-apps/06-transactions-app/), [`07-compute-api-app/`](./ignite3-reference-apps/07-compute-api-app/)

**Phase 3: Performance** *(Optimize and scale)*

- **[Module 05: Performance & Scalability](./docs/05-performance-scalability/)** - Production patterns
   - Data Streaming → Caching Strategies → Query Performance
   - **Reference Apps**: [`08-data-streaming-app/`](./ignite3-reference-apps/08-data-streaming-app/), [`09-caching-patterns-app/`](./ignite3-reference-apps/09-caching-patterns-app/)

### Alternative Paths

**Problem-Focused** *(Jump to your immediate need)*

- **Need to connect and store data?** → [Module 01: Foundation](./docs/01-foundation/)
- **Building data models?** → [Module 02: Schema Design](./docs/02-schema-design/)  
- **Querying data efficiently?** → [Module 03: Data Access APIs](./docs/03-data-access-apis/)
- **Managing consistency?** → [Module 04: Distributed Operations](./docs/04-distributed-operations/)
- **Optimizing performance?** → [Module 05: Performance & Scalability](./docs/05-performance-scalability/)

**Reference Materials** *(Architectural context)*

- **[Technical Reference](./docs/00-reference/)** - Architecture patterns and API design principles

## Reference Applications

All reference applications are located in [`ignite3-reference-apps/`](./ignite3-reference-apps/) and use a consistent music store dataset. Each application demonstrates the concepts from its corresponding documentation module through working, runnable code.

- **Docker Setup**: [`00-docker/`](./ignite3-reference-apps/00-docker/) - 3-node cluster with initialization scripts  
- **Foundation Data**: [`01-sample-data-setup/`](./ignite3-reference-apps/01-sample-data-setup/) - Sample data and schema setup

## Key Concepts Demonstrated

### Data Colocation for Performance

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

### Asynchronous Programming Patterns

```java
// Chain operations efficiently without blocking threads
artists.getAsync(null, artistKey)
    .thenCompose(artist -> albums.getAllAsync(null, artist.getAlbums()))
    .thenApply(this::calculateTotalDuration)
    .thenAccept(duration -> System.out.println("Total: " + duration));
```

### Multi-Modal API Integration

```java
// Fast single-record operations through Table API
Artist artist = artists.get(null, artistKey);

// Complex analytics through SQL API
var topTracks = client.sql().execute(null,
    "SELECT t.Name, COUNT(il.Quantity) as Purchases " +
    "FROM Track t JOIN InvoiceLine il ON t.TrackId = il.TrackId " +
    "GROUP BY t.TrackId ORDER BY Purchases DESC LIMIT 10");
```

---

## Key Ignite 3 Java API Patterns

1. **Dual Mode Support**: All APIs support both synchronous and asynchronous operations
2. **Builder Pattern**: Extensive use of builders for configuration (Client.Builder, Statement.Builder)
3. **Resource Management**: AutoCloseable interfaces for proper resource cleanup
4. **Type Safety**: Generic types for compile-time type safety
5. **Fluent APIs**: Method chaining for improved developer experience

## Main Entry Points Summary

1. **Thin Client**: `IgniteClient.builder().addresses("127.0.0.1:10800").build()`
2. **Embedded Node**: `IgniteServer.start(nodeName, configPath, workDir)`
3. **JDBC**: `DriverManager.getConnection("jdbc:ignite:thin://127.0.0.1:10800/")`
4. **Spring Boot**: Auto-configured `IgniteClient` bean

## API Capabilities

- **Table Operations**: Key-value and record-based data access
- **SQL Operations**: Full SQL support with DDL/DML/DQL
- **Compute**: Distributed job execution and MapReduce
- **Transactions**: ACID transactions with configurable isolation
- **Streaming**: High-performance data ingestion
- **Catalog Management**: Dynamic schema operations
- **Caching**: Distributed caching with multiple patterns
- **Security**: Authentication and SSL/TLS support

## Sample Dataset

All examples use a music store dataset with:

### Core Music Entities

- **Artist** → **Album** → **Track** (hierarchical with colocation)
- **Genre**, **MediaType** (reference data)

### Business Entities  

- **Customer** → **Invoice** → **InvoiceLine** (business workflow)
- **Employee** (organizational hierarchy)

### Playlist Entities

- **Playlist** → **PlaylistTrack** (many-to-many relationships)

This consistent dataset reduces cognitive load and demonstrates real-world distributed application patterns.

## Getting Help

- **Learning Path Questions**: Start with [`modules/README.md`](./docs/README.md) for guidance
- **Module-Specific Issues**: Check individual module documentation in [`modules/`](./docs/)
- **Reference App Problems**: See app-specific READMEs in [`ignite3-reference-apps/`](./ignite3-reference-apps/)
- **Cluster Setup**: Troubleshooting guide in [`00-docker/README.md`](./ignite3-reference-apps/00-docker/README.md)

## Next Steps

**New to Ignite 3?** Start with the recommended learning path:

- **[Begin with Module 01: Foundation](./docs/01-foundation/)** - Essential concepts and first connection
- **[Continue with Module 02: Schema Design](./docs/02-schema-design/)** - Build your data models
- **[Follow the structured path](./docs/README.md)** - Progress through all modules systematically

**Need immediate results?** Use problem-focused learning to jump directly to your area of interest.
