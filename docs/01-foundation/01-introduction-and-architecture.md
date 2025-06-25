# Chapter 1.1: Introduction and Architecture Overview

## Learning Objectives

By completing this chapter, you will:

- Understand Apache Ignite 3's distributed computing capabilities and core use cases
- Distinguish between connection strategies and their appropriate applications
- Recognize when to use default zones versus custom zones for different scenarios
- Understand the Java API architecture and multi-modal access patterns
- Make informed decisions about deployment patterns for development and production

## The Challenge: Building at Scale

Modern applications handle data at scales that traditional databases struggle to support. Consider a music streaming platform serving millions of users. Your application needs to:

- Store and retrieve artist catalogs across global regions
- Process customer purchases with ACID guarantees while maintaining high throughput
- Generate real-time recommendations by analyzing listening patterns across distributed data
- Handle massive data ingestion as new tracks and user interactions flow continuously
- Scale from thousands to millions of concurrent users

These requirements push beyond what traditional databases can deliver. You need a distributed computing platform designed for modern application demands.

## Apache Ignite 3: Distributed Computing Platform

Apache Ignite 3 addresses these challenges through a unified platform that combines:

### Core Capabilities

- **In-Memory Data Grid**: Data resides in memory across cluster nodes, providing microsecond access times with persistence for durability.

- **Distributed SQL Engine**: Execute ANSI-compliant SQL queries across distributed datasets with join optimization and parallel execution.

- **NoSQL Key-Value Store**: Access data through type-safe object-oriented APIs for single-record and batch operations.

- **Compute Engine**: Execute business logic directly on data nodes, eliminating network overhead through colocation.

- **Streaming Engine**: Ingest and process high-velocity data streams with built-in backpressure handling and flow control.

### Evolution from Ignite 2

If you have worked with version 2 of Ignite in the past, Ignite 3 represents a redesign focused on developer experience and operational management:

- **Cleaner Architecture**: Simplified codebase with clear separation between storage, compute, and API layers
- **Schema Management**: Annotation-driven table creation reduces configuration complexity
- **Transaction Semantics**: Consistency guarantees with programming models
- **Modern Java Integration**: Built-in support for CompletableFuture, type safety, and contemporary frameworks

## Connection Strategy Framework

Ignite 3 provides two primary connection approaches, each suited for different architectural patterns:

### Remote Client Pattern (Recommended)

The **IgniteClient** provides optimal separation between application and storage tiers. Applications connect to cluster nodes without becoming part of the cluster topology.

**When to Use:**

- Microservices architectures
- Containerized deployments
- Independent application scaling
- Cloud-native environments
- Development and testing scenarios

**Benefits:**

- Applications scale independently from storage
- Simplified deployment and management
- No cluster membership overhead
- Automatic failover across cluster nodes

### Embedded Node Pattern

The **IgniteServer** integrates applications directly into the cluster topology, making them both consumers and providers of cluster services.

**When to Use:**

- Maximum data locality requirements
- Legacy application integration
- Specialized compute-heavy workloads
- Single-deployment scenarios

**Trade-offs:**

- Application lifecycle tied to cluster membership
- More complex deployment coordination
- Higher resource requirements per application instance

### Unified Programming Model

Both connection strategies implement the same Ignite interface, enabling consistent programming patterns regardless of deployment choice. This means you can develop with one pattern and deploy with another based on operational requirements.

## Java API Architecture

The Ignite 3 Java API follows three core design principles that simplify distributed programming:

### Multi-modal Access Paradigms

Choose the appropriate API for each use case:

- **Table API**: Object-oriented access through strongly-typed RecordView interfaces handles structured data operations efficiently with compile-time type safety.

- **SQL API**: Relational access handles complex queries, joins, and analytics using standard ANSI SQL syntax with automatic query optimization.

- **Key-Value API**: Cache-like operations for basic get/put scenarios with minimal overhead and maximum performance.

### Async-First Design

Every operation supports both synchronous and asynchronous execution patterns:

- **Synchronous Operations**: Use for straightforward scenarios where blocking is acceptable and code readability is prioritized.

- **Asynchronous Operations**: Use CompletableFuture-based operations for high-throughput scenarios, reactive programming, and when you need to compose multiple operations efficiently.

### Strong Type Safety

Generic APIs ensure compile-time error detection and eliminate runtime type casting. The same Java classes work across Table API and SQL API, providing consistent data models throughout your application.

## Distribution Zone Decision Framework

Distribution zones control how your data is distributed, replicated, and managed across cluster nodes. Understanding when to use each approach is crucial for both development efficiency and production success.

### Default Zone Strategy

**What It Is**: Ignite 3 automatically creates a "Default" zone during cluster initialization with conservative settings optimized for getting started quickly.

**Configuration**:

- 1 replica (no fault tolerance)
- 25 partitions (good for small to medium datasets)
- Includes all cluster nodes
- Immediate availability (no setup required)

**When to Use**:

- Development and testing scenarios
- Learning and experimentation
- Proof-of-concept implementations
- Single-node or non-critical deployments
- Rapid prototyping where setup speed matters more than fault tolerance

**Benefits**:

- Zero configuration required
- Immediate availability
- Reduced resource overhead
- Simplified mental model for learning

**Limitations**:

- No fault tolerance (data loss if node fails)
- Not suitable for production workloads
- Limited performance optimization options

### Custom Zone Strategy

**What It Is**: Explicitly defined zones with specific replica counts, partition configurations, and node selection criteria tailored to your application requirements.

**Configuration Options**:

- 2+ replicas (fault tolerance)
- Optimized partition counts for your data size
- Specific storage profiles
- Node filtering and placement policies
- Performance-tuned settings

**When to Use**:

- Production deployments
- Mission-critical data requiring fault tolerance
- Performance-sensitive applications
- Multi-tenant scenarios requiring data isolation
- Applications with specific compliance requirements

**Benefits**:

- Fault tolerance through multiple replicas
- Performance optimization for specific workloads
- Data isolation and security
- Operational control and predictability

**Trade-offs**:

- Requires explicit configuration
- Higher resource consumption
- More complex operational management
- Increased setup complexity

### Decision Matrix

| Scenario | Zone Strategy | Reasoning |
|----------|---------------|-----------|
| Local development | Default | Speed of setup, no fault tolerance needed |
| Integration testing | Default | Simplified configuration, reproducible environments |
| Proof of concept | Default | Focus on functionality, not operations |
| Production API backend | Custom (2-3 replicas) | Fault tolerance, predictable performance |
| Analytics workload | Custom (optimized partitions) | Performance tuning for large datasets |
| Multi-tenant SaaS | Custom (per tenant) | Data isolation and security |

## Connection Framework

For optimal performance, Ignite 3 remote clients should connect to all cluster nodes to enable direct partition mapping and eliminate unnecessary network hops.

### Single-Node Connection Limitations

Connecting to only one cluster node results in:

- No automatic discovery of other nodes
- All operations routed through single connection point
- Poor performance due to extra network hops
- No direct partition awareness

### Multi-Node Connection Benefits

Specifying all cluster node addresses enables:

- Direct partition mapping for optimal performance
- Automatic failover to healthy nodes
- Load distribution across cluster
- Maximum throughput for data operations

### Topology Change Considerations

Current limitations require explicit address management:

- No automatic node discovery beyond specified addresses
- Adding/removing nodes requires application updates
- DNS-based addressing recommended for dynamic environments

## Key Patterns for Production Success

### Data Colocation Strategy

Store related data together to optimize query performance and reduce network overhead. Colocation keys ensure that related records reside on the same cluster nodes, enabling efficient local operations.

### Performance-First Connection Management

Always specify all cluster node addresses in production deployments. This enables partition awareness and eliminates performance bottlenecks caused by single-point-of-connection architectures.

### Zone-Aware Application Design

Design applications to work with both default and custom zones using the same programming patterns. This enables smooth transitions from development to production without code changes.

### API Integration Patterns

Combine Table API and SQL API based on operation characteristics:

- Use Table API for known-key operations requiring type safety
- Use SQL API for complex queries, joins, and analytical operations
- Both APIs access the same underlying distributed data seamlessly

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

**For Windows and other systems**: Follow the installation instructions at [https://ignite.apache.org/docs/ignite3/latest/installation/](https://ignite.apache.org/docs/ignite3/latest/installation/) which covers all supported platforms and installation methods.

Alternative installation methods are available for environments where Docker is not suitable.

**Knowledge Assumptions**:

- Java fundamentals (collections, generics, streams)
- Basic SQL concepts (SELECT, JOIN, GROUP BY)
- General understanding of web application architecture

## Next Steps

Understanding these architectural concepts and decision frameworks provides the foundation for hands-on development.

Continue with:

- **[Chapter 1.2: Your First Implementation](02-getting-started.md)** - Put these concepts into practice with a working Ignite 3 application using the default zone pattern

- **[Chapter 1.3: Distributed Data Fundamentals](03-distributed-data-fundamentals.md)** - Learn the core concepts of distributed data management and advanced zone configuration patterns
