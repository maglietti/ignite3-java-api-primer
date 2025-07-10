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
- Maven 3.8+ or Gradle (via wrapper)

## Applications

**SQLAPIDemo** - Orchestrator that runs all SQL demonstrations in a learning progression from basic queries through advanced analytics and production patterns.

**BasicSQLOperations** - Demonstrates fundamental SQL API usage including query execution, parameter binding, statement configuration, and ResultSet processing.

**AdvancedSQLOperations** - Shows complex query patterns including JOINs, aggregations, batch operations, and object mapping with type safety.

**TransactionSQLOperations** - Integrates SQL operations with ACID transactions demonstrating lifecycle management and error recovery.

**ProductionAnalyticsPatterns** - Implements production-scale analytics with memory-efficient streaming, hierarchical data processing, and query optimization.

### Running the Applications

From this directory, use Gradle to run each application:

```bash
# Run complete SQL API demo (all examples)
../gradlew runSQLAPIDemo

# Run individual demonstrations
../gradlew runBasicSQLOperations
../gradlew runAdvancedSQLOperations
../gradlew runTransactionSQLOperations
../gradlew runProductionAnalyticsPatterns

# Custom cluster address
../gradlew runSQLAPIDemo --args="192.168.1.100:10800"
```

## Application Details

### 1. SQLAPIDemo

Main demonstration of SQL API concepts:

**Maven:**
```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.sqlapi.SQLAPIDemo"
```

**Gradle:**
```bash
../gradlew run
```

**Key concepts:**

- Basic query execution
- Statement configuration and reuse
- Transaction integration
- Batch operations
- Object mapping
- Error handling patterns

### 2. BasicSQLOperations

Basic SQL operations:

**Maven:**
```bash
mvn compile exec:java@basic
```

**Gradle:**
```bash
../gradlew runBasicSQLOperations
```

**Key concepts:**

- Simple SELECT queries
- INSERT, UPDATE, DELETE operations
- Basic JOINs
- Query result processing

### 3. AdvancedSQLOperations

Advanced SQL patterns:

**Maven:**
```bash
mvn compile exec:java@advanced
```

**Gradle:**
```bash
../gradlew runAdvancedSQLOperations
```

**Key concepts:**

- Complex multi-table JOINs
- Aggregate functions
- Window functions
- Subqueries and CTEs
- Performance optimization

### 4. TransactionSQLOperations

SQL with transactions:

**Maven:**
```bash
mvn compile exec:java@transactions
```

**Gradle:**
```bash
../gradlew runTransactionSQLOperations
```

**Key concepts:**

- SQL within transactions
- ACID guarantees
- Rollback handling
- Multi-statement transactions
- Isolation levels

### 5. ProductionAnalyticsPatterns

Production-scale analytics:

**Maven:**
```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.sqlapi.ProductionAnalyticsPatterns"
```

**Gradle:**
```bash
../gradlew runProductionAnalyticsPatterns
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


## Common Issues

**Connection refused**: Ensure Docker cluster is running

**Missing data**: Run sample data setup first

**Memory issues**: Adjust pagination for large result sets
