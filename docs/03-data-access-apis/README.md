# Module 03: Data Access APIs

## What You'll Accomplish

By completing this module, you will:

- Implement object-oriented data access using Table API for type-safe operations
- Execute distributed queries using SQL API for analytical workloads
- Apply async patterns for high-performance non-blocking operations
- Choose optimal API patterns based on specific data access requirements

## Building on Previous Knowledge

This module builds directly on Schema Design concepts, using the annotated Artist, Album, and Track entities you've designed. You'll implement efficient data access patterns that leverage the colocation strategies and distribution zones established in Module 02.

## Module Overview

Data Access APIs transform schema definitions into working data operations. Through Table API and SQL API patterns, you'll implement efficient data access that takes advantage of distributed architecture while maintaining familiar programming models.

## Implementation Pattern

### Chapter 1: [Table API Operations](./01-table-api-operations.md)

**What You'll Build:** Type-safe CRUD operations using object-oriented patterns for direct data access

**Implementation Focus:** RecordView and KeyValueView patterns that leverage schema colocation for optimal performance
- Batch operations for high-throughput scenarios

**Key concepts:** Type safety, async programming, performance optimization

**Perfect for:** Single-record operations, cache scenarios, high-frequency lookups

### Chapter 2: [SQL API Analytics](./02-sql-api-analytics.md)

*Execute complex queries and analytical workloads*

**What you'll master:**

- Complex joins across distributed tables
- Aggregation queries with grouping and filtering
- Window functions for analytical processing
- Parameter binding and prepared statements

**Key concepts:** Distributed SQL, query optimization, analytics patterns

**Perfect for:** Complex analytics, reporting, multi-table operations

### Chapter 3: [SQL API Selection Guide](./03-sql-api-selection-guide.md)

*Choose optimal patterns for different scenarios*

**What you'll decide:**

- When to use Table API vs SQL API
- How to combine both APIs effectively
- Performance trade-offs between approaches
- Architecture patterns for complex applications

**Key concepts:** API selection criteria, performance characteristics, hybrid approaches

## Real-world Application

The music store data demonstrates data access patterns through progressive complexity: fast Artist lookups establish Table API performance patterns, Album analytics show SQL API capabilities, and Track relationship queries demonstrate combined API approaches.

This practical progression builds from simple key-value operations to complex analytical queries while maintaining consistent music store context.

## Reference Application

**[`04-table-api-app/`](../../ignite3-reference-apps/04-table-api-app/)**

Working implementation of Table API patterns with synchronous and asynchronous operations, performance optimization techniques, and type-safe data access using the music store schema.

**[`05-sql-api-app/`](../../ignite3-reference-apps/05-sql-api-app/)**

Advanced SQL query patterns with analytical workload examples, cross-table relationship queries, and optimization techniques for complex music store analytics.

## What You've Learned → Next Steps

Data Access APIs module establishes type-safe data operations through Table API patterns and complex analytics through SQL API patterns. This knowledge enables distributed transaction handling in Module 04, where you'll learn to maintain data consistency across multiple operations and tables.

---

**Module Navigation:**
← [Schema Design](../02-schema-design/) | **Data Access APIs** | [Distributed Operations](../04-distributed-operations/) →

**Start Implementation:** [Table API Operations](./01-table-api-operations.md)
