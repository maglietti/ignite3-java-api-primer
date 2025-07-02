# SQL API - Apache Ignite 3 Reference Application

**Demonstration of Ignite 3 Java SQL API for relational data access**

📖 **Related Documentation**: [SQL API - Relational Data Access](../../docs/03-data-access-apis/02-sql-api-analytics.md)

## Overview

This reference application demonstrates the Ignite 3 SQL API, focusing on Java interface usage patterns. Learn to use the `IgniteSql` interface and related classes for database operations, result processing, and transaction integration in distributed environments.

## What You'll Learn

### Core SQL API Concepts

- **IgniteSql Interface**: Primary gateway to all SQL operations
- **Statement Management**: Building reusable, configurable statements
- **ResultSet Processing**: Type-safe data extraction and iteration
- **Parameter Binding**: Secure, efficient parameterized queries
- **Transaction Integration**: ACID compliance with distributed operations

### Advanced Patterns

- **Asynchronous Operations**: Non-blocking query execution with CompletableFuture
- **Batch Operations**: Efficient bulk data processing with BatchedArguments
- **Object Mapping**: Automatic POJO conversion using Mapper interface
- **Error Handling**: Exception management and retry patterns
- **Performance Optimization**: Statement reuse, pagination, and optimization techniques

### Practical Examples

- **DDL Operations**: Schema management and evolution
- **DML Operations**: Data manipulation with validation
- **Complex Queries**: JOINs, aggregations, subqueries, and analytics
- **Result Processing**: Metadata introspection and dynamic processing

## Prerequisites

**Required**:

1. Ignite 3 cluster running on `localhost:10800`
2. Complete [sample-data-setup](../01-sample-data-setup/) to load the music store schema and data

## Reference Applications

### 1. SQLAPIDemo.java

**Demonstration of SQL API concepts**

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.sqlapi.SQLAPIDemo"
```

**Features demonstrated:**

- Basic SQL interface access and query execution
- Statement configuration and reuse patterns
- ResultSet processing with various data types
- Transaction integration (successful and rollback scenarios)
- Batch operations for bulk data processing
- Object mapping with single-column and custom mapping
- Error handling with retry patterns
- Performance optimization techniques

### 2. DDLOperations.java

**Schema management using the SQL API**

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.sqlapi.DDLOperations"
```

**Operations covered:**

- Distribution zone creation and configuration
- Table creation with colocation strategies
- Index management for query performance
- Schema evolution (adding columns, modifying structures)
- Schema introspection using system catalogs
- Proper cleanup and dependency management

### 3. DMLOperations.java

**Data manipulation patterns and best practices**

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.sqlapi.DMLOperations"
```

**Patterns demonstrated:**

- INSERT operations with parameter binding
- UPDATE operations with conditional logic
- DELETE operations with cascading considerations
- MERGE/UPSERT patterns for conflict resolution
- Batch DML operations for bulk processing
- Transactional DML for ACID compliance
- Data validation and constraint handling

### 4. QueryOperations.java

**Complex query patterns and analytical operations**

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.sqlapi.QueryOperations"
```

**Query types covered:**

- Simple queries with filtering and sorting
- Multi-table JOINs (INNER, LEFT, self-joins)
- Aggregate queries with GROUP BY and HAVING
- Subqueries and correlated subqueries
- Analytical queries for business intelligence
- Asynchronous query execution
- Paginated result processing
- Metadata introspection and dynamic processing
- Performance optimization patterns

### 5. ProductionAnalyticsPatterns.java

**Production-scale analytics patterns for music streaming platforms**

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.sqlapi.ProductionAnalyticsPatterns"
```

**Advanced patterns demonstrated:**

- **Streaming Result Processing**: Handle large result sets with memory efficiency
- **Hierarchical Data Processing**: Navigate complex artist → album → track relationships
- **Metadata-Driven Processing**: Dynamic query generation based on table schemas
- **Performance Optimization**: Statement caching, batch processing, and async patterns
- **Production-Scale Analytics**: Customer segmentation, revenue analysis, and trend detection
- **Error Handling**: Comprehensive retry logic and circuit breaker patterns
- **Resource Management**: Memory-efficient processing of large datasets
- **Real-world Scenarios**: Music platform analytics with millions of records

## Key SQL API Classes Demonstrated

### Core Interfaces

- **`IgniteSql`**: Main SQL interface obtained via `client.sql()`
- **`Statement`**: Configurable, reusable query statements
- **`ResultSet<SqlRow>`**: Type-safe result iteration and data extraction
- **`SqlRow`**: Individual row access with typed value extraction methods

### Supporting Classes

- **`BatchedArguments`**: Bulk operation parameter management
- **`Mapper<T>`**: Automatic object mapping configuration
- **`AsyncResultSet<SqlRow>`**: Non-blocking result processing
- **`Transaction`**: ACID transaction integration

### Utility Patterns

- **Statement Builder**: Configurable statement creation with timeouts and pagination
- **Parameter Binding**: Safe parameterization with `?` placeholders
- **Resource Management**: Proper cleanup with try-with-resources
- **Error Handling**: SQL exception management and retry logic

## Code Examples

### Basic Query Execution

```java
IgniteSql sql = client.sql();

// Simple query with parameters
ResultSet<SqlRow> artists = sql.execute(null, 
    "SELECT ArtistId, Name FROM Artist WHERE Name LIKE ? ORDER BY Name", 
    "%Rock%");

while (artists.hasNext()) {
    SqlRow row = artists.next();
    int id = row.intValue("ArtistId");
    String name = row.stringValue("Name");
    System.out.println("Artist " + id + ": " + name);
}
```

### Statement Configuration and Reuse

```java
Statement artistLookup = sql.statementBuilder()
    .query("SELECT ArtistId, Name FROM Artist WHERE Country = ?")
    .queryTimeout(30, TimeUnit.SECONDS)
    .pageSize(100)
    .build();

// Reuse with different parameters
ResultSet<SqlRow> ukArtists = sql.execute(null, artistLookup, "UK");
ResultSet<SqlRow> usArtists = sql.execute(null, artistLookup, "USA");
```

### Batch Operations

```java
BatchedArguments batch = BatchedArguments.create()
    .add("Artist 1", "Country A")
    .add("Artist 2", "Country B")
    .add("Artist 3", "Country C");

long[] results = sql.executeBatch(null,
    "INSERT INTO Artist (Name, Country) VALUES (?, ?)", batch);
```

### Transaction Integration

```java
try (Transaction tx = client.transactions().begin()) {
    sql.execute(tx, "INSERT INTO Artist (Name) VALUES (?)", "New Artist");
    sql.execute(tx, "INSERT INTO Album (ArtistId, Title) VALUES (?, ?)", 
        artistId, "New Album");
    tx.commit();
}
```

### Object Mapping

```java
// Single-column mapping
ResultSet<String> names = sql.execute(null,
    Mapper.of(String.class, "Name"),
    "SELECT Name FROM Artist ORDER BY Name");

// Custom POJO mapping (with @Column annotations)
ResultSet<Artist> artists = sql.execute(null,
    Mapper.of(Artist.class),
    "SELECT ArtistId, Name, Country FROM Artist WHERE Country = ?", "UK");
```

### Asynchronous Operations

```java
CompletableFuture<AsyncResultSet<SqlRow>> future = sql.executeAsync(null,
    "SELECT COUNT(*) as count FROM Artist");

future.thenAccept(resultSet -> {
    for (SqlRow row : resultSet.currentPage()) {
        long count = row.longValue("count");
        System.out.println("Total artists: " + count);
    }
    resultSet.closeAsync();
});
```

## Performance Best Practices

1. **Statement Reuse**: Create Statement objects for repeated queries
2. **Parameter Binding**: Always use `?` placeholders instead of string concatenation
3. **Batch Operations**: Use BatchedArguments for bulk modifications
4. **Pagination**: Configure appropriate page sizes for large result sets
5. **Async Processing**: Use async operations for high-throughput scenarios
6. **Resource Management**: Always use try-with-resources for proper cleanup
7. **Transaction Scope**: Keep transactions as short as possible
8. **Object Mapping**: Use Mapper<T> to reduce boilerplate code

## Error Handling Patterns

The reference applications demonstrate error handling:

- **SQL Exception Management**: Catching and handling of SqlException
- **Transaction Rollback**: Automatic rollback with try-with-resources
- **Retry Patterns**: Exponential backoff for transient failures
- **Batch Error Handling**: Partial success scenarios with SqlBatchException
- **Resource Cleanup**: Guaranteed cleanup even when errors occur

## Testing and Development

### Running Individual Examples

```bash
# Run complete demo
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.sqlapi.SQLAPIDemo"

# Run specific operation types
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.sqlapi.DDLOperations"
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.sqlapi.DMLOperations"
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.sqlapi.QueryOperations"

# Run production analytics patterns
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.sqlapi.ProductionAnalyticsPatterns"
```

### Prerequisites Check

Ensure the music store sample data is loaded:

```bash
cd ../01-sample-data-setup
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.setup.app.ProjectInitializationApp"
```

### Troubleshooting

- **Connection Issues**: Verify Ignite cluster is running on `localhost:10800`
- **Missing Data**: Run the sample data setup first
- **Permission Errors**: Ensure cluster allows DDL operations
- **Memory Issues**: Adjust pagination settings for large result sets

## Integration with Other Modules

- **Prerequisites**: [01-sample-data-setup](../01-sample-data-setup/) - Required music store schema and data
- **Related**: [04-table-api-app](../04-table-api-app/) - Object-oriented data access patterns
- **Next Steps**: [06-transactions-app](../06-transactions-app/) - Advanced transaction patterns
- **Advanced**: [07-compute-api-app](../07-compute-api-app/) - Distributed computing with SQL results

## Key Takeaways

1. **Java-First Approach**: Focus on Java API patterns rather than SQL syntax
2. **Type Safety**: Leverage strongly-typed interfaces for compile-time safety
3. **Resource Management**: Always use proper cleanup patterns
4. **Performance**: Use statement reuse, batching, and async operations appropriately
5. **Error Handling**: Implement exception management
6. **Transaction Integration**: Understand how SQL operations work within transactions
7. **Distributed Awareness**: Consider colocation and distribution in query design

The SQL API provides familiar SQL semantics while abstracting the complexity of distributed query execution, making it easy to build robust, high-performance applications on Ignite 3.
