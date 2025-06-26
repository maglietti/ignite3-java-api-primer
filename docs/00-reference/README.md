# Module 00: Technical Reference

*Understanding the architectural foundation of Apache Ignite 3*

## About This Module

This module provides the technical foundation for understanding Apache Ignite 3's architecture and Java API design patterns. While other modules focus on hands-on learning, this module serves as reference for architectural understanding.

**When to read this module:**

- Before starting if you prefer understanding architecture first
- As reference material while working through other modules
- When you need to understand design decisions behind the APIs

## Module Contents

### [Ignite 3 Architecture](./ignite3-arch.md)

Core platform architecture and distributed systems concepts that underpin all Ignite 3 operations.

**What you'll learn:**

- Distributed data storage patterns
- Consistency and replication models
- Node coordination and cluster management
- Performance characteristics

### [Java API Architecture](./java-api-arch.md)

API design patterns and client programming model for Java development.

**What you'll learn:**

- Client connection patterns
- Synchronous vs asynchronous programming models
- Resource management strategies
- Error handling patterns

### [Storage System Architecture](./storage-system-arch.md)

Distributed storage foundation that powers Apache Ignite 3's data management capabilities.

**What you'll learn:**

- Distribution zones and data placement control
- Data partitioning algorithms and rebalancing
- Storage profiles and engine configuration
- Physical storage implementations and tuning

### [SQL Engine Architecture](./sql-engine-arch.md)

Apache Calcite integration and SQL processing architecture in Ignite 3.

**What you'll learn:**

- Apache Calcite integration and query processing pipeline
- Cost-based optimization and distributed query planning
- SQL standard compliance and advanced features
- Performance considerations and troubleshooting techniques

### [Technical Features](./technical-features.md)

Feature matrix and capability overview for understanding what's possible with Ignite 3.

**What you'll learn:**

- Complete feature inventory
- Performance characteristics
- Operational capabilities
- Integration options

## How This Connects to Your Learning

This module provides context for the practical patterns you'll implement in modules 01-05:

- **Module 01 foundations** build on the architectural concepts presented here
- **Module 02 schema design** applies the data distribution principles explained here
- **Module 03 data access** leverages the API patterns documented here
- **Module 04 operations** implements the consistency models described here
- **Module 05 performance** applies the characteristics outlined here

## Reading Strategy

**Reference Approach**: Keep this module open while working through practical modules for architectural context

**Study Approach**: Read through completely before starting practical work to build mental models

**Just-in-Time**: Jump to specific sections when you encounter concepts in other modules

---

**Next Steps:**

- **Ready to start hands-on learning?** → [**Module 01: Foundation**](../01-foundation/)
- **Want to understand fundamentals first?** → [**Ignite 3 Architecture**](./ignite3-arch.md)
- **Curious about API design?** → [**Java API Architecture**](./java-api-arch.md)
