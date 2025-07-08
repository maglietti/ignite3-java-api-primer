# Apache Ignite 3 Java API Primer

Apache Ignite 3 is a distributed database that combines in-memory storage, distributed SQL processing, and compute capabilities in a single platform. It provides ACID transactions across multiple nodes, automatic data partitioning, and the ability to execute code where data resides, eliminating the complexity of managing separate database, cache, and compute systems.

This primer provides practical guidance for learning Ignite 3's Java API through working code examples and realistic distributed data management patterns. Using a consistent music store dataset, it teaches distributed systems concepts with a progression from basic connections to production-scale implementations.

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker 20.10+

## Quick Start

1. Start the 3-node Ignite cluster:

   ```bash
   cd ignite3-reference-apps/00-docker
   ./init-cluster.sh
   ```

2. Load sample music store data:

   ```bash
   cd ../01-sample-data-setup
   mvn compile exec:java
   ```

3. Run any reference application:

   ```bash
   cd ../[app-directory]
   mvn compile exec:java
   ```

## Learning Path

### Documentation Modules

1. **[Foundation](./docs/01-foundation/)** - Distributed systems concepts and architecture
2. **[Schema Design](./docs/02-schema-design/)** - Data modeling for distributed systems
3. **[Data Access APIs](./docs/03-data-access-apis/)** - Table API, SQL API, and Key-Value operations
4. **[Distributed Operations](./docs/04-distributed-operations/)** - Transactions and compute operations
5. **[Performance & Scalability](./docs/05-performance-scalability/)** - Streaming, caching, and optimization

### Reference Applications

Each module includes working code examples in [`ignite3-reference-apps/`](./ignite3-reference-apps/):

- **Getting Started** ([`02-getting-started-app`](./ignite3-reference-apps/02-getting-started-app/)) - Basic connections and operations
- **Schema Annotations** ([`03-schema-annotations-app`](./ignite3-reference-apps/03-schema-annotations-app/)) - Schema-as-code patterns
- **Table API** ([`04-table-api-app`](./ignite3-reference-apps/04-table-api-app/)) - Object-oriented data access
- **SQL API** ([`05-sql-api-app`](./ignite3-reference-apps/05-sql-api-app/)) - SQL operations and analytics
- **Transactions** ([`06-transactions-app`](./ignite3-reference-apps/06-transactions-app/)) - ACID transaction patterns
- **Compute API** ([`07-compute-api-app`](./ignite3-reference-apps/07-compute-api-app/)) - Distributed job execution
- **Data Streaming** ([`08-data-streaming-app`](./ignite3-reference-apps/08-data-streaming-app/)) - High-throughput ingestion
- **Caching Patterns** ([`09-caching-patterns-app`](./ignite3-reference-apps/09-caching-patterns-app/)) - Cache strategies
- **File Streaming** ([`10-file-streaming-app`](./ignite3-reference-apps/10-file-streaming-app/)) - Reactive file processing
- **Performance Optimization** ([`11-performance-optimization-app`](./ignite3-reference-apps/11-performance-optimization-app/)) - SQL performance optimization

## Key Concepts Covered

- **Distributed Architecture** - Multi-node clusters with automatic failover and partition awareness
- **Schema Design** - Data colocation strategies for optimal join performance
- **Multiple APIs** - Table API for objects, SQL API for analytics, Key-Value API for performance
- **Distributed Transactions** - ACID guarantees across multiple nodes
- **Compute Colocation** - Execute code where data resides
- **High-Throughput Streaming** - Process millions of events per second
- **Caching Patterns** - Cache-aside, write-through, and write-behind strategies

## Project Structure

```text
ignite3-java-api-primer/
├── docs/                                   # Learning modules and documentation
│   ├── 01-foundation/                      # Distributed systems concepts
│   ├── 02-schema-design/                   # Data modeling patterns
│   ├── 03-data-access-apis/                # API usage and selection
│   ├── 04-distributed-operations/          # Transactions and compute
│   └── 05-performance-scalability/         # Performance optimization
└── ignite3-reference-apps/                 # Working code examples
    ├── 00-docker/                          # 3-node cluster setup
    ├── 01-sample-data-setup/               # Music store dataset
    ├── 02-getting-started-app/             # Basic connections and operations
    ├── 03-schema-annotations-app/          # Schema-as-code patterns
    ├── 04-table-api-app/                   # Object-oriented data access
    ├── 05-sql-api-app/                     # SQL operations and analytics
    ├── 06-transactions-app/                # ACID transaction patterns
    ├── 07-compute-api-app/                 # Distributed job execution
    ├── 08-data-streaming-app/              # High-throughput ingestion
    ├── 09-caching-patterns-app/            # Cache strategies
    ├── 10-file-streaming-app/              # Reactive file processing
    └── 11-performance-optimization-app/    # SQL optimization
```

## Sample Dataset

All examples use a consistent music store dataset with realistic data relationships:

- **Core Entities**: Artist → Album → Track hierarchy colocated for optimal joins
- **Business Data**: Customer → Invoice → InvoiceLine workflows
- **Reference Data**: Genre and MediaType lookup tables
- **11 Tables Total**: ~200 artists, ~500 albums, ~3000 tracks

Data is distributed across two zones:

- **MusicStore** (2 replicas): Primary business data
- **MusicStoreReplicated** (3 replicas): Reference data

## Additional Resources

- [IDE Setup Guide](./IDE-SETUP.md) - IntelliJ IDEA and VS Code configuration
- [Apache Ignite 3 Documentation](https://ignite.apache.org/docs/ignite3/latest/)
- [Reference Applications README](./ignite3-reference-apps/README.md) - Detailed application descriptions
- [Technical Reference](./docs/00-reference/) - Ignite 3 architecture deep dive

## License

Apache License 2.0
