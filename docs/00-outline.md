# Apache Ignite 3 - Java API Primer

## Table of Contents

1. [Introduction & Overview](#1-introduction--overview)
2. [Getting Started](#2-getting-started)
3. [Schema-as-Code with Annotations](#3-schema-as-code-with-annotations)
4. [Table API - Object-Oriented Data Access](#4-table-api---object-oriented-data-access)
5. [SQL API - Relational Data Access](#5-sql-api---relational-data-access)
6. [Transactions](#6-transactions)
7. [Compute API - Distributed Processing](#7-compute-api---distributed-processing)
8. [Data Streaming](#8-data-streaming)
9. [Schema and Catalog Management](#9-schema-and-catalog-management)
10. [Advanced Topics](#10-advanced-topics)
11. [Integration Patterns](#11-integration-patterns)
12. [Best Practices & Common Patterns](#12-best-practices--common-patterns)

---

## 1. Introduction & Overview

### What is Apache Ignite 3

- Next-generation distributed computing platform
- In-memory data grid with persistent storage
- SQL and NoSQL capabilities
- Distributed computing engine

### Key Features and Capabilities

- ACID transactions
- SQL support with ANSI compliance
- Key-value operations
- Distributed computing
- Real-time streaming
- Multi-platform client support

### Java API Architecture Overview

- **Entry Points**: `Ignite`, `IgniteClient`, `IgniteServer`
- **Dual Access Paradigms**: Object-oriented (Table API) and SQL-based access
- **Async-First Design**: All operations support both sync and async patterns
- **Strong Typing**: Extensive use of generics and type-safe operations

### Entry Points

- **Thin Client**: `IgniteClient` - for remote connections
- **Embedded Server**: `IgniteServer` - for embedded nodes
- **Core Interface**: `Ignite` - unified API access point

---

## 2. Getting Started

### Setup & Dependencies

#### Maven Dependencies

```xml
<dependency>
    <groupId>org.apache.ignite</groupId>
    <artifactId>ignite-client</artifactId>
    <version>3.0.0</version>
</dependency>
```

#### Gradle Dependencies

```groovy
implementation 'org.apache.ignite:ignite-client:3.0.0'
```

#### Basic Project Setup

*[To be completed with specific setup instructions]*

### Connection Patterns

#### Thin Client Connection

```java
try (IgniteClient client = IgniteClient.builder()
        .addresses("127.0.0.1:10800")
        .build()) {
    // Work with client
}
```

#### Embedded Server Setup

```java
IgniteServer server = IgniteServer.start(nodeName, configPath, workDir);
```

#### Configuration Basics

- SSL configuration
- Authentication setup
- Timeout configuration
- Connection pooling

### First Steps

*[To be completed with "Hello World" example and basic table creation]*

---

## 3. Schema-as-Code with Annotations

### 3.1 Introduction to Annotations API

- Why annotations matter in distributed systems
- Schema-as-code benefits
- Annotation processing pipeline

### 3.2 Basic Table Definition

#### @Table Annotation Fundamentals

```java
@Table("my_table")
public class MyRecord {
    @Id
    private Integer id;
    
    @Column(value = "name", nullable = false, length = 50)
    private String name;
    
    private String description; // Auto-mapped column
}
```

#### @Column for Field Mapping

- `value()` - column name
- `nullable()` - nullability constraint
- `length()` - string length
- `precision()` & `scale()` - numeric precision

#### @Id for Primary Keys

- Single field primary keys
- Sort order specification
- Auto-generation strategies

### 3.3 Advanced Schema Features

#### Composite Primary Keys

```java
@Table("complex_table")
public class ComplexEntity {
    @Id
    private Long id;
    
    @Id
    @Column("region")
    private String region;
    
    // Other fields...
}
```

#### @Zone for Distribution Configuration

```java
@Table(
    value = "distributed_table",
    zone = @Zone(
        value = "my_zone",
        partitions = 16,
        replicas = 3,
        storageProfiles = "default"
    )
)
public class DistributedEntity {
    // Fields...
}
```

#### @Index for Secondary Indexes

```java
@Table(
    value = "indexed_table",
    indexes = @Index(
        value = "name_idx",
        columns = {
            @ColumnRef("name"),
            @ColumnRef(value = "created_date", sort = SortOrder.DESC)
        }
    )
)
public class IndexedEntity {
    // Fields...
}
```

#### @ColumnRef and Colocation

*[To be completed with colocation examples]*

### 3.4 Key-Value vs Record Mapping

- When to use separate key/value classes
- When to use single record classes
- Performance implications

### 3.5 DDL Generation and Catalog Integration

```java
// Create table from annotations
Table table = ignite.catalog().createTable(MyRecord.class);

// Create table from key-value classes
Table table = ignite.catalog().createTable(PersonKey.class, PersonValue.class);
```

### 3.6 POJO Mapping Deep Dive

- `Mapper.of()` auto-mapping
- Custom field-to-column mapping
- Type conversion system
- Working with complex types

---

## 4. Table API - Object-Oriented Data Access

### Table Management (`IgniteTables`)

- Listing tables
- Getting table references

### Key-Value Operations (`KeyValueView`)

#### Working with Tuples

```java
KeyValueView<Tuple, Tuple> kvView = client.tables().table("accounts").keyValueView();

Tuple key = Tuple.create().set("accountNumber", 123456);
Tuple value = Tuple.create()
    .set("firstName", "Val")
    .set("lastName", "Kulichenko")
    .set("balance", 100.00d);
kvView.put(null, key, value);
```

#### Working with POJOs

```java
KeyValueView<AccountKey, Account> kvView = client.tables()
    .table("accounts")
    .keyValueView(AccountKey.class, Account.class);

AccountKey key = new AccountKey(123456);
Account value = new Account("Val", "Kulichenko", 100.00d);
kvView.put(null, key, value);
```

#### Put, Get, Remove Operations

*[To be completed with comprehensive examples]*

### Record Operations (`RecordView`)

#### Insert, Update, Upsert, Delete

```java
RecordView<Tuple> accounts = client.tables().table("accounts").recordView();

Tuple newAccountTuple = Tuple.create()
    .set("accountNumber", 123456)
    .set("firstName", "Val")
    .set("lastName", "Kulichenko")
    .set("balance", 100.00d);
accounts.insert(null, newAccountTuple);
```

#### Bulk Operations

*[To be completed with bulk operation examples]*

### Async Operations

#### CompletableFuture Patterns

*[To be completed with async examples]*

#### Error Handling in Async Code

*[To be completed with error handling patterns]*

---

## 5. SQL API - Relational Data Access

### Basic SQL Operations (`IgniteSql`)

#### DDL Operations (CREATE, ALTER, DROP)

```java
client.sql().executeScript(
    "CREATE TABLE CITIES ("
    + "ID   INT PRIMARY KEY,"
    + "NAME VARCHAR);"
);
```

#### DML Operations (INSERT, UPDATE, DELETE)

*[To be completed with DML examples]*

#### Query Operations (SELECT)

```java
try (ResultSet<SqlRow> rs = client.sql().execute(null,
        "SELECT a.FIRST_NAME, a.LAST_NAME, c.NAME FROM ACCOUNTS a "
        + "INNER JOIN CITIES c on c.ID = a.CITY_ID ORDER BY a.ACCOUNT_ID")) {
    while (rs.hasNext()) {
        SqlRow row = rs.next();
        System.out.println(row.stringValue(0) + ", " + row.stringValue(1));
    }
}
```

### Prepared Statements

#### Statement Builders

```java
Statement stmt = client.sql().createStatement("INSERT INTO CITIES (ID, NAME) VALUES (?, ?)");
```

#### Parameter Binding

*[To be completed with parameter binding examples]*

#### Reusable Statements

*[To be completed with reusable statement patterns]*

### Result Processing

#### Working with `ResultSet<SqlRow>`

*[To be completed with result processing examples]*

#### POJO Mapping with `Mapper<T>`

```java
Statement statement = client.sql().statementBuilder()
    .query("SELECT a.FIRST_NAME as firstName, a.LAST_NAME as lastName, a.BALANCE FROM ACCOUNTS a")
    .build();

try (ResultSet<AccountInfo> rs = client.sql().execute(null, Mapper.of(AccountInfo.class), statement)) {
    while (rs.hasNext()) {
        AccountInfo row = rs.next();
        System.out.println(row.firstName + ", " + row.lastName);
    }
}
```

#### Iterating Through Results

*[To be completed with iteration patterns]*

### Batch Operations

#### Batch Inserts/Updates

```java
long rowsAdded = Arrays.stream(client.sql().executeBatch(tx,
    "INSERT INTO ACCOUNTS (ACCOUNT_ID, CITY_ID, FIRST_NAME, LAST_NAME, BALANCE) values (?, ?, ?, ?, ?)",
    BatchedArguments.of(1, 1, "John", "Doe", 1000.0d)
        .add(2, 1, "Jane", "Roe", 2000.0d)
        .add(3, 2, "Mary", "Major", 1500.0d)))
    .sum();
```

#### Performance Considerations

*[To be completed with performance guidance]*

### Async SQL Operations

```java
client.sql().executeAsync(null, stmt)
    .thenCompose(this::fetchAllRowsInto)
    .get();
```

---

## 6. Transactions

### Transaction Basics (`IgniteTransactions`)

- ACID properties in Ignite
- Transaction isolation levels

### Synchronous Transactions

#### Explicit Transaction Management

```java
Transaction tx = client.transactions().begin(new TransactionOptions().readOnly(false));
try {
    // Perform operations
    client.sql().execute(tx, stmt, 1, "Forest Hill");
    tx.commit();
} catch (Exception e) {
    tx.rollback();
    throw e;
}
```

#### `runInTransaction()` Pattern

```java
client.transactions().runInTransaction(tx -> {
    Account account = accounts.get(tx, key);
    account.balance += 200.0d;
    accounts.put(tx, key, account);
});
```

### Asynchronous Transactions

#### Async Transaction Handling

```java
CompletableFuture<Void> fut = client.transactions().beginAsync().thenCompose(tx ->
    accounts.getAsync(tx, key)
        .thenCompose(account -> {
            account.balance += 300.0d;
            return accounts.putAsync(tx, key, account);
        })
        .thenCompose(ignored -> tx.commitAsync())
);
fut.join();
```

#### Combining with Async Operations

*[To be completed with async combination patterns]*

### Transaction Options

- Read-only transactions
- Timeout configuration
- Error handling and rollback

---

## 7. Compute API - Distributed Processing

### Compute Jobs (`IgniteCompute`)

#### Job Creation and Deployment

```java
JobDescriptor<String, Void> job = JobDescriptor.builder(WordPrintJob.class)
    .units(new DeploymentUnit(DEPLOYMENT_UNIT_NAME, DEPLOYMENT_UNIT_VERSION))
    .build();
```

#### Job Execution Patterns

```java
JobTarget jobTarget = JobTarget.anyNode(client.clusterNodes());
client.compute().execute(jobTarget, job, "word");
```

#### Target Selection Strategies

*[To be completed with targeting strategies]*

### Job Implementation

#### `ComputeJob` Interface

```java
private static class WordLengthJob implements ComputeJob<String, Integer> {
    @Override
    public CompletableFuture<Integer> executeAsync(JobExecutionContext context, String arg) {
        return CompletableFuture.completedFuture(arg.length());
    }
}
```

#### Input/Output Handling

*[To be completed with I/O patterns]*

#### Error Handling in Jobs

*[To be completed with error handling]*

### Async Compute Operations

#### Parallel Job Execution

```java
CompletableFuture<Integer> jobFuture = client.compute().executeAsync(jobTarget, job, word);
List<Integer> results = jobFutures.stream()
    .map(CompletableFuture::join)
    .collect(Collectors.toList());
```

#### Result Aggregation

*[To be completed with aggregation patterns]*

### Advanced Topics

- Code deployment units
- Job cancellation

---

## 8. Data Streaming

### Data Streamer Setup

#### Configuration Options

```java
DataStreamerOptions options = DataStreamerOptions.builder()
    .pageSize(1000)
    .perPartitionParallelOperations(1)
    .autoFlushInterval(1000)
    .retryLimit(16)
    .build();
```

#### Performance Tuning

*[To be completed with tuning guidance]*

### Streaming Patterns

#### High-Throughput Ingestion

```java
KeyValueView<Tuple, Tuple> view = client.tables().table("accounts").keyValueView();

try (var publisher = new SubmissionPublisher<DataStreamerItem<Entry<Tuple, Tuple>>>()) {
    CompletableFuture<Void> streamerFut = view.streamData(publisher, options);
    
    // Submit data
    for (int i = 0; i < ACCOUNTS_COUNT; i++) {
        Tuple key = Tuple.create().set("accountNumber", i);
        Tuple value = Tuple.create()
            .set("name", "name" + i)
            .set("balance", rnd.nextLong(100_000))
            .set("active", rnd.nextBoolean());
        
        publisher.submit(DataStreamerItem.of(Map.entry(key, value)));
    }
}
```

#### Error Handling Strategies

*[To be completed with error handling]*

#### Backpressure Management

*[To be completed with backpressure patterns]*

---

## 9. Schema and Catalog Management

### Catalog API (`IgniteCatalog`)

- DDL operations from code
- Schema introspection

### Dynamic Schema Changes

- Adding/dropping tables
- Index management

---

## 10. Advanced Topics

### Error Handling

- Exception hierarchy
- Retry strategies
- Circuit breaker patterns

### Performance Optimization

- Connection pooling
- Batch operations
- Async patterns

### Monitoring and Metrics

- JMX metrics
- Connection monitoring

### Security

- Authentication setup
- SSL/TLS configuration

---

## 11. Integration Patterns

### Spring Framework Integration

- Spring Boot auto-configuration
- Spring Data integration

### JDBC Driver Usage

- JDBC connection setup
- Integration with existing JDBC tools

### Microservices Patterns

- Service discovery
- Configuration management

---

## 12. Best Practices & Common Patterns

### Resource Management

- Connection lifecycle
- Transaction boundaries

### Error Handling Strategies

*[To be completed with error patterns]*

### Performance Guidelines

*[To be completed with performance best practices]*

### Testing Strategies

*[To be completed with testing approaches]*

---

## Troubleshooting Guide

### Common Issues

*[To be completed with common problems and solutions]*

### Configuration Problems

*[To be completed with config troubleshooting]*

### Performance Issues

*[To be completed with performance troubleshooting]*

### Debugging Techniques

*[To be completed with debugging approaches]*

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
