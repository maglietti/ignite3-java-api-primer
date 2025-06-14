# Apache Ignite 3 - Java API Primer

## Table of Contents

1. [Introduction & Overview](./docs/01-introduction-overview.md)
2. [Getting Started](./docs/02-getting-started.md)
3. [Schema-as-Code with Annotations](./docs/03-schema-as-code-with-annotations.md)
4. [Table API - Object-Oriented Data Access](./docs/04-table-api-object-oriented-data-access.md)
5. [SQL API - Relational Data Access](./docs/05-sql-api-relational-data-access.md)
6. [Transactions](./docs/06-transactions.md)
7. [Compute API - Distributed Processing](./docs/07-compute-api-distributed-processing.md)
8. [Data Streaming](./docs/08-data-streaming-high-throughput-ingestion.md)
9. [Caching Patterns](./docs/09-caching-patterns-java-implementations.md)
10. [Schema and Catalog Management](./docs/10-schema-and-catalog-management.md)

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
- **Caching**: Distributed caching with multiple patterns
- **Security**: Authentication and SSL/TLS support

---

*This primer provides a guide to working with the Apache Ignite 3 Java API. Each section can be expanded with detailed examples, code samples, and practical guidance based on specific use cases and requirements.*
