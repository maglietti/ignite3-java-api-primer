# Getting Started App - Apache Ignite 3 Reference

**Essential Ignite 3 operations and connection patterns**

**Related Documentation**: [Getting Started Guide](../../docs/01-foundation/02-getting-started.md)

## Overview

This module teaches Apache Ignite 3 fundamentals through focused examples. Each application demonstrates one key concept using minimal code.

## What You'll Learn

- **Client Connection Management**: Connect to all cluster nodes for optimal performance  
- **Default Zone Usage**: Apply Chapter 1.1 concepts using the default zone for development
- **Table Creation**: Define tables using POJO annotations with automatic zone assignment
- **CRUD Operations**: Insert, read, update, and delete data efficiently using type-safe APIs
- **SQL Integration**: Query data using standard SQL with the same data model
- **Multi-modal APIs**: Use both Table and SQL APIs on the same underlying data
- **Error Handling**: Handle common connection and operation errors
- **Resource Management**: Properly manage client resources and cleanup

## Prerequisites

- **⚠️ Required**: Apache Ignite 3 cluster running (see [00-docker setup](../00-docker/README.md))
- **Optional**: [01-sample-data-setup](../01-sample-data-setup/) for music store examples

> **Cluster Requirement**: The 3-node Ignite cluster from `00-docker` must be running before executing applications.

```bash
# Start cluster first
cd ../00-docker && docker-compose up -d
```

## Applications

### 1. HelloWorldApp

**Default zone pattern and multi-modal API usage**

Demonstrates the essential Ignite 3 patterns from Chapter 1.2:

1. **Multi-node Connection**: Connect to all cluster nodes for partition awareness
2. **Default Zone Usage**: Zero-configuration development pattern from Chapter 1.1
3. **Table API Operations**: Type-safe CRUD operations with automatic schema creation
4. **SQL API Integration**: Query the same data using standard SQL
5. **Resource Management**: Proper connection and cleanup patterns

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.gettingstarted.HelloWorldApp"
```

**Expected Output:**

```
=== Hello World: Apache Ignite 3 ===
>>> Connected with partition awareness
>>> Table created in default zone
>>> Books inserted using Table API
>>> Retrieved: Book{id=1, title='1984', author='George Orwell'}
>>> All books via SQL:
    1: 1984 by George Orwell
    2: Brave New World by Aldous Huxley
>>> Success! Default zone pattern working perfectly.
```

### 2. BasicSetupDemo

**Tables with relationships**

Shows related data patterns:

- Two tables (Author and Book)
- Transaction to insert related records
- JOIN query to combine data

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.gettingstarted.BasicSetupDemo"
```

**Expected Output:**

```
=== Basic Setup: Related Tables ===
Tables created
Data inserted with transaction
Books by author:
  Aldous Huxley - Brave New World
  George Orwell - 1984
  George Orwell - Animal Farm
Success!
```

### 3. ConnectionExamples

**Different connection patterns**

Shows connection options:

- Basic connection (development)
- Multi-node connection (production failover)
- Connection with custom timeouts

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.gettingstarted.ConnectionExamples"
```

**Expected Output:**

```
=== Connection Examples ===

1. Basic Connection:
  Connected to: [localhost:10800]
  Health check: OK

2. Multi-Node Connection:
  Multi-node connection failed: ...
  (This is normal with single-node setup)

3. Connection with Timeouts:
  Connected with custom timeouts
  Connect timeout: 5 seconds
  Operation timeout: 10 seconds
  Response time: 23ms
```

## Key Patterns

- **Connection**: Always specify all cluster node addresses for optimal performance and partition awareness
- **Default Zone**: Perfect for development and learning - zero configuration required
- **Table API**: Type-safe operations for known-key scenarios with compile-time validation
- **SQL API**: Flexible queries and analytics using standard ANSI SQL syntax  
- **Unified Model**: Same POJO classes work across Table API and SQL API seamlessly
- **Resource Management**: Use try-with-resources for automatic cleanup and connection management

## Running the Examples

Start the cluster first:

```bash
cd ../00-docker && docker-compose up -d
```

Run any example:

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.gettingstarted.HelloWorldApp"
```

## Common Issues

**Connection refused**: Make sure the Docker cluster is running
**Tables already exist**: This is normal - applications handle existing tables gracefully
**ClassNotFoundException after build success**: If you see compilation success but ClassNotFoundException, check for stray .class files in source directories:

```bash
# Remove any .class files from source directories
find src -name "*.class" -delete
mvn clean compile
```

## Next Steps

After completing these examples:

1. **[Schema Annotations](../03-schema-annotations-app/)**: Learn advanced POJO mapping
2. **[Table API](../04-table-api-app/)**: Master record and key-value operations  
3. **[SQL API](../05-sql-api-app/)**: Advanced SQL patterns and optimization
4. **[Transactions](../06-transactions-app/)**: Complex transaction scenarios

## Related Resources

- **Music Store Examples**: [sample-data-setup](../01-sample-data-setup/) for complete dataset
- **Docker Setup**: [docker](../00-docker/) for cluster management
- **Documentation**: [Getting Started Guide](../../docs/01-foundation/02-getting-started.md)
