# Module 03: Data Access APIs

*Object-oriented and relational data access patterns*

## About This Module

This module teaches you to efficiently access and manipulate data in your distributed Ignite 3 cluster. You'll master both object-oriented (Table API) and relational (SQL API) approaches, learning when to use each pattern for optimal performance.

**Critical for application development** - these APIs are how your applications interact with distributed data every day.

## Learning Objectives

By completing this module, you will:

- Choose the optimal API pattern for different data access scenarios
- Execute efficient queries across distributed data
- Handle asynchronous operations for high-performance applications
- Integrate multiple API patterns within the same application

## Module Journey

### Chapter 1: [Table API Operations](./01-table-api-operations.md)

*Master type-safe, object-oriented data access*

**What you'll implement:**

- Key-value operations for cache-like performance
- Record operations for full object handling
- Asynchronous patterns for non-blocking applications
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

## Hands-On Learning

This module demonstrates data access patterns using the music streaming platform scenario:

**Table API examples:**

- High-frequency artist lookups (cache-like operations)
- Customer profile management (object-oriented operations)
- Session data handling (key-value patterns)

**SQL API examples:**

- Music recommendation analytics (complex joins)
- Sales reporting (aggregation queries)
- Playlist generation (window functions)

**Combined patterns:**

- Fast lookups + complex analytics in the same application
- Optimal API selection for different operation types

## Reference Applications

**[`04-table-api-app/`](../../ignite3-reference-apps/04-table-api-app/)**

- Complete Table API patterns
- Synchronous and asynchronous operations
- Performance optimization techniques

**[`05-sql-api-app/`](../../ignite3-reference-apps/05-sql-api-app/)**

- Advanced SQL query patterns
- Analytical workload examples
- Cross-table relationship queries

Run both applications to see API patterns in working code.

## Performance Philosophy

**Table API Strengths:**

- **Maximum performance** for simple operations
- **Type safety** with compile-time validation
- **Optimal network usage** with minimal serialization overhead
- **Direct partition access** for single-record operations

**SQL API Strengths:**

- **Complex query capabilities** across multiple tables
- **Analytical processing** with aggregations and window functions
- **Familiar SQL syntax** for developers and tools
- **Query optimization** by the distributed query engine

**Combined Approach:**

- Use Table API for high-frequency, simple operations
- Use SQL API for complex analytics and reporting
- Combine both in the same application for optimal performance

## Success Indicators

**You're ready for Module 04** when you can:

- Execute efficient single-record operations using Table API
- Write complex analytical queries using SQL API
- Choose the optimal API pattern for different scenarios
- Handle asynchronous operations confidently

## Architecture Patterns

This module introduces patterns you'll expand in advanced modules:

- **Module 04** will add transaction handling to these data access patterns
- **Module 05** will optimize the performance characteristics demonstrated here
- **Async patterns** learned here become essential for high-throughput applications

## Common Scenarios

**When to use Table API:**

- User session management (fast key-value lookups)
- Configuration data access (simple get/put operations)
- Real-time data feeds (high-frequency updates)

**When to use SQL API:**

- Business intelligence queries (complex analytics)
- Report generation (multi-table aggregations)
- Data exploration (ad-hoc query requirements)

**When to combine both:**

- Most production applications benefit from hybrid approaches
- Fast operations via Table API, analytics via SQL API

---

**Navigation:**

← [**Schema Design**](../02-schema-design/) | **Data Access APIs** | [**Distributed Operations**](../04-distributed-operations/) →

**Start Learning:** [**Table API Operations**](./01-table-api-operations.md)
