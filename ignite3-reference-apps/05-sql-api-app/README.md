# Apache Ignite 3 SQL API Application

SQL operations and analytics using Apache Ignite 3's Java SQL API.

**Related Documentation**: [SQL API Analytics](../../docs/03-data-access-apis/02-sql-api-analytics.md)

## Overview

Demonstrates Apache Ignite 3's SQL API for relational data access. Shows how to execute queries, process results, manage transactions, and optimize performance using the IgniteSql interface.

## What You'll Learn

- Query execution with IgniteSql interface
- Statement building and reuse
- ResultSet processing with SqlRow
- Parameter binding for secure queries
- Transaction integration
- Batch operations for bulk processing
- Async query patterns
- Object mapping with POJOs

## Prerequisites

- Apache Ignite 3 cluster running (see [00-docker setup](../00-docker/README.md))
- Sample data setup completed ([01-sample-data-setup](../01-sample-data-setup/))
- Java 17 or higher
- Maven 3.8+

## Applications

### 1. SQLAPIDemo

Main demonstration of SQL API concepts:

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.sqlapi.SQLAPIDemo"
```

**Key concepts:**

- Basic query execution
- Statement configuration and reuse
- Transaction integration
- Batch operations
- Object mapping
- Error handling patterns

### 2. DDLOperations

Schema management operations:

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.sqlapi.DDLOperations"
```

**Key concepts:**

- Zone creation and configuration
- Table creation with colocation
- Index management
- Schema introspection

### 3. DMLOperations

Data manipulation patterns:

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.sqlapi.DMLOperations"
```

**Key concepts:**

- INSERT with parameter binding
- UPDATE with conditions
- DELETE operations
- MERGE/UPSERT patterns
- Batch DML operations

### 4. QueryOperations

Complex query patterns:

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.sqlapi.QueryOperations"
```

**Key concepts:**

- Multi-table JOINs
- Aggregate queries
- Subqueries
- Async execution
- Result pagination

### 5. ProductionAnalyticsPatterns

Production-scale analytics:

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.sqlapi.ProductionAnalyticsPatterns"
```

**Key concepts:**

- Streaming result processing
- Hierarchical data navigation
- Performance optimization
- Memory-efficient processing

## Key SQL API Patterns

### Basic Query

```java
IgniteSql sql = client.sql();
ResultSet<SqlRow> artists = sql.execute(null, 
    "SELECT ArtistId, Name FROM Artist WHERE Name LIKE ?", "%Rock%");

while (artists.hasNext()) {
    SqlRow row = artists.next();
    int id = row.intValue("ArtistId");
    String name = row.stringValue("Name");
}
```

### Statement Reuse

```java
Statement stmt = sql.statementBuilder()
    .query("SELECT * FROM Artist WHERE Country = ?")
    .queryTimeout(30, TimeUnit.SECONDS)
    .pageSize(100)
    .build();

ResultSet<SqlRow> ukArtists = sql.execute(null, stmt, "UK");
ResultSet<SqlRow> usArtists = sql.execute(null, stmt, "USA");
```

### Batch Operations

```java
BatchedArguments batch = BatchedArguments.create()
    .add("Artist 1", "Country A")
    .add("Artist 2", "Country B");

long[] results = sql.executeBatch(null,
    "INSERT INTO Artist (Name, Country) VALUES (?, ?)", batch);
```

### Transactions

```java
try (Transaction tx = client.transactions().begin()) {
    sql.execute(tx, "INSERT INTO Artist (Name) VALUES (?)", "New Artist");
    sql.execute(tx, "INSERT INTO Album (ArtistId, Title) VALUES (?, ?)", 
        artistId, "New Album");
    tx.commit();
}
```

## Performance Patterns

- Reuse Statement objects for repeated queries
- Use parameter binding instead of string concatenation
- Leverage batch operations for bulk modifications
- Configure appropriate page sizes for large results
- Use async operations for high throughput

## Running the Examples

Run complete demo:

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.sqlapi.SQLAPIDemo"
```

Run specific operations:

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.sqlapi.QueryOperations"
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.sqlapi.ProductionAnalyticsPatterns"
```

## Common Issues

**Connection refused**: Ensure Docker cluster is running

**Missing data**: Run sample data setup first

**Memory issues**: Adjust pagination for large result sets
