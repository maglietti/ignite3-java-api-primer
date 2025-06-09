# Apache Ignite 3 - Java API Primer

## Table of Contents

1. [Introduction & Overview](01-introduction-overview.md)
2. [Getting Started](02-getting-started.md)
3. [Schema-as-Code with Annotations](03-schema-as-code-with-annotations.md)
4. [Table API - Object-Oriented Data Access](04-table-api-object-oriented-data-access.md)
5. [SQL API - Relational Data Access](05-sql-api-relational-data-access.md)
6. [Transactions](06-transactions.md)
7. [Compute API - Distributed Processing](07-compute-api-distributed-processing.md)
8. [Data Streaming](08-data-streaming.md)
9. [Schema and Catalog Management](09-schema-and-catalog-management.md)
10. [Advanced Topics](10-advanced-topics.md)
11. [Integration Patterns](11-integration-patterns.md)
12. [Best Practices & Common Patterns](12-best-practices-common-patterns.md)
13. [Troubleshooting Guide](13-troubleshooting-guide.md)

---

The detailed content for each section has been moved to individual files. Please see the links in the Table of Contents above to access each section.

---

## Key API Design Patterns

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
- **Security**: Authentication and SSL/TLS support

---

*This primer provides a comprehensive guide to working with the Apache Ignite 3 Java API. Each section can be expanded with detailed examples, code samples, and practical guidance based on specific use cases and requirements.*
