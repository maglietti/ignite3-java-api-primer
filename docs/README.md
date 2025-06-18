# Apache Ignite 3 Java API Learning Modules

This directory contains the structured learning modules for mastering Apache Ignite 3's Java API. Each module builds upon previous concepts while demonstrating practical patterns through working reference applications.

## Module Structure

### [00 - Reference](./00-reference/)

Technical architecture and API design patterns that guide the entire learning journey.

- [Ignite 3 Architecture](./00-reference/IGNITE3-ARCH.md) - Platform architecture and distributed systems concepts
- [Java API Architecture](./00-reference/JAVA-API-ARCH.md) - API design patterns and client programming model
- [Technical Features](./00-reference/TECHNICAL_FEATURES.md) - Feature matrix and capability overview

### [01 - Foundation](./01-foundation/)

Essential concepts for building distributed applications with Ignite 3.

- [Introduction and Architecture](./01-foundation/01-introduction-and-architecture.md) - Platform overview and core concepts
- [Getting Started](./01-foundation/02-getting-started.md) - First connection and basic operations
- [Distributed Data Fundamentals](./01-foundation/03-distributed-data-fundamentals.md) - Data distribution and consistency patterns

### [02 - Schema Design](./02-schema-design/)

Schema-as-code patterns for distributed data modeling.

- [Basic Annotations](./02-schema-design/01-basic-annotations.md) - Core annotations for table creation
- [Relationships and Colocation](./02-schema-design/02-relationships-and-colocation.md) - Entity relationships and performance optimization
- [Advanced Annotations](./02-schema-design/03-advanced-annotations.md) - Complex patterns and multi-zone architectures
- [Schema Evolution](./02-schema-design/04-schema-evolution.md) - DDL generation and production deployment patterns

### [03 - Data Access APIs](./03-data-access-apis/)

Object-oriented and relational data access patterns.

- [Table API Operations](./03-data-access-apis/01-table-api-operations.md) - Type-safe record and key-value operations
- [SQL API Analytics](./03-data-access-apis/02-sql-api-analytics.md) - Complex queries and analytical workloads
- [SQL API Selection Guide](./03-data-access-apis/03-sql-api-selection-guide.md) - Choosing optimal API patterns for different use cases

### [04 - Distributed Operations](./04-distributed-operations/)

Transaction management and distributed computing patterns.

- [Transaction Fundamentals](./04-distributed-operations/01-transaction-fundamentals.md) - ACID properties and isolation levels
- [Advanced Transaction Patterns](./04-distributed-operations/02-advanced-transaction-patterns.md) - Complex workflows and error handling
- [Compute API Processing](./04-distributed-operations/03-compute-api-processing.md) - Distributed job execution and MapReduce patterns

### [05 - Performance and Scalability](./05-performance-scalability/)

High-performance patterns for production applications.

- [Data Streaming](./05-performance-scalability/01-data-streaming.md) - High-throughput data ingestion patterns
- [Caching Strategies](./05-performance-scalability/02-caching-strategies.md) - Performance optimization through caching
- [Query Performance](./05-performance-scalability/03-query-performance.md) - Query optimization and monitoring

## Learning Orchestration

### Self-Paced Learning Framework

**🎯 Start Here**: [**Module 01: Foundation**](./01-foundation/01-introduction-and-architecture.md)

This primer uses a progressive learning model where each module builds upon previous concepts:

**Foundation → Schema → APIs → Operations → Performance**

### Learning Checkpoints

After completing each module, verify your understanding before progressing:

#### Module 01 Complete

When you can:

- Connect to an Ignite 3 cluster
- Understand distributed data fundamentals  
- Run basic operations

#### Ready for Module 02: Schema Design

#### Module 02 Complete

When you can:

- Design schema using annotations
- Implement colocation strategies
- Deploy schemas programmatically

#### Ready for Module 03: Data Access APIs

#### Module 03 Complete

When you can:

- Choose optimal API patterns
- Execute efficient queries
- Handle asynchronous operations

#### Ready for Module 04: Distributed Operations

#### Module 04 Complete

When you can:

- Manage ACID transactions
- Handle distributed computing workloads
- Implement error recovery patterns

#### Ready for Module 05: Performance & Scalability

#### Module 05 Complete

When you can:

- Optimize data ingestion performance
- Implement caching strategies
- Monitor and tune query performance

### Alternative Learning Paths

**🚀 Quick Start**: Need immediate results?

- **I need to connect and store data** → [Module 01](./01-foundation/)
- **I'm building data models** → [Module 02](./02-schema-design/)  
- **I need efficient queries** → [Module 03](./03-data-access-apis/)
- **I need transaction handling** → [Module 04](./04-distributed-operations/)
- **I need performance optimization** → [Module 05](./05-performance-scalability/)

**📚 Reference Study**: Understanding architecture first?

- Start with [Module 00: Reference](./00-reference/) for architectural context
- Then follow the standard progression from Module 01

## Reference Applications

Each module connects to working reference applications in [`../ignite3-reference-apps/`](../ignite3-reference-apps/) that demonstrate the concepts through executable code using a consistent music store dataset.

## Technical Standards

All modules follow established patterns:

- **Progressive Complexity**: Simple concepts build to advanced patterns
- **Real-World Context**: Music streaming platform scenarios throughout
- **Production Readiness**: Proper error handling, resource management, and best practices
- **API Focus**: Emphasis on Java API mastery rather than operational concerns

## Dataset Consistency

Examples use a unified music store dataset:

- **Core Entities**: Artist → Album → Track (hierarchical relationships)
- **Business Entities**: Customer → Invoice workflows
- **Reference Data**: Genre, MediaType tables
- **Colocation Strategy**: Related data distributed for optimal performance

This consistent dataset reduces cognitive load and demonstrates real-world distributed application patterns.
