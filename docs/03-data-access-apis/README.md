<!--
Licensed under Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)
SPDX-License-Identifier: CC-BY-SA-4.0
For full license text, see LICENSE-CC-BY-SA-4.0
-->

# Module 03: Data Access APIs

Your music platform has Artists, Albums, and Tracks stored across a distributed cluster with optimized relationships. Now users want to access this music - mobile apps need instant Artist lookups, web browsers want Album details, and analytics dashboards require complex reporting queries.

Different needs call for different approaches. Sometimes you know exactly which Artist you want (fast key-value access). Other times you need all Albums from the 1990s (complex SQL queries). Your platform needs both patterns working efficiently.

## How Data Access APIs Work

Ignite 3 provides three distinct data access patterns for your distributed music catalog. Key-Value API enables ultra-fast direct key lookups with minimal overhead. Table API provides object-oriented operations with type safety and business logic integration. SQL API enables complex analytical queries that span multiple tables and partitions.

Each API optimizes for different access patterns: Key-Value operations provide maximum performance for cache-like access, Table API eliminates object-relational mapping overhead for entity operations, and SQL API enables cross-table joins, aggregations, and filtering operations that would require thousands of individual record retrievals to accomplish manually.

## Data Access Implementation Patterns

### Chapter 1: [Table API Operations](./01-table-api-operations.md)

*Configure object-oriented data access with type safety*

Your mobile application needs complete Artist entity operations for user profiles and music library management. Implement RecordView patterns for type-safe business logic that leverage your colocation strategies for single-partition operations.

### Chapter 2: [SQL API Analytics](./02-sql-api-analytics.md)

*Execute distributed analytical queries*

Marketing requires complex analytics across Artist-Album-Track relationships with aggregation functions and filtering conditions. Configure distributed SQL execution that processes queries across multiple cluster nodes without data transfer overhead.

### Chapter 3: [Key-Value Operations](./03-key-value-operations.md)

*Maximize performance with direct field access*

Your application needs 50,000 session lookups per second and instant feature flag checks. Implement KeyValueView patterns that eliminate object serialization overhead for sub-millisecond performance in caching, session management, and configuration scenarios.

### Chapter 4: [Data Access API Decision Guide](./04-api-decision-guide.md)

*Strategic framework for selecting optimal data access APIs*

Match operation characteristics with appropriate API capabilities using decision criteria and performance patterns. Learn when to use Key-Value operations for maximum performance, Table API for business logic operations, and SQL API for complex analytics and reporting.

## Production Data Access Challenges

Your music platform now efficiently accesses distributed data through both direct object operations and complex analytical queries. Different application layers use optimal access patterns based on performance requirements and data complexity.

## Implementation References

**[`04-table-api-app/`](../../ignite3-reference-apps/04-table-api-app/)** and **[`05-sql-api-app/`](../../ignite3-reference-apps/05-sql-api-app/)**

Complete Table API, Key-Value, and SQL API implementations demonstrating object-oriented operations, high-performance key-value access, and distributed query execution patterns.

## Next Implementation Challenge

Your platform now handles fast data access and complex analytics efficiently. However, real applications require transactional workflows that span multiple operations. When users purchase music, you need atomic updates across payment processing, library management, and analytics tracking. Single API operations must combine into complex distributed transactions.

---

← [Schema Design](../02-schema-design/) | **Data Access APIs** | [Distributed Operations](../04-distributed-operations/) →

[Table API Operations](./01-table-api-operations.md)
