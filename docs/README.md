# Apache Ignite 3 Java API Primer

Apache Ignite 3 enables applications that scale horizontally while maintaining ACID transactions and executing distributed computing workloads. This primer teaches practical Java API patterns through hands-on implementation using consistent music store data.

## Learning Approach

This primer uses progressive complexity within consistent context. You'll work with Artist, Album, and Track entities throughout all modules, building from basic connections to production-ready distributed applications. This approach reduces cognitive load while demonstrating how distributed data patterns apply to real applications.

## Implementation Path

**Module 01 - Foundation**: Connection patterns, basic table operations, and data distribution understanding

**Module 02 - Schema Design**: Annotation-driven table creation, colocation strategies, and data relationship modeling

**Module 03 - Data Access APIs**: Table API and SQL API operations optimized for distributed schemas

**Module 04 - Distributed Operations**: Transaction management and distributed computing patterns for business workflows

**Module 05 - Performance and Scalability**: High-throughput streaming, caching strategies, and query optimization for production workloads

## Prerequisites

- Java 17 or later
- Basic Java programming experience
- Docker for running Ignite 3 cluster

## Getting Started

**New to Ignite 3?** Begin with [Foundation](./01-foundation/) to establish connection patterns and basic operations.

**Need architecture context?** Review [Reference Materials](./00-reference/) for platform design overview before implementation.

**Working on specific patterns?** Jump to the relevant module, though Foundation concepts enable advanced patterns.

---

**Start Implementation:** [Foundation - Introduction and Architecture](./01-foundation/01-introduction-and-architecture.md)
