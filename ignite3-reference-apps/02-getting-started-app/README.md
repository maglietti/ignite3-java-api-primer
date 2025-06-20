# Getting Started App - Apache Ignite 3 Reference

**Essential Ignite 3 operations and connection patterns**

**Related Documentation**: [Getting Started Guide](../../docs/01-foundation/02-getting-started.md)

## Overview

This module teaches Apache Ignite 3 fundamentals through simple, focused examples. Each application demonstrates one key concept using minimal code, following the KISS principle for educational clarity.

## What You'll Learn

- **Client Connection Management**: Connect to all cluster nodes for optimal performance
- **Distribution Zones**: Understand default zone vs custom zone creation patterns
- **Table Creation**: Define tables using POJO annotations with zone specifications
- **CRUD Operations**: Insert, read, update, and delete data efficiently
- **SQL Integration**: Query data using standard SQL across different zones
- **Transaction Patterns**: Ensure data consistency across operations
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

**Zone patterns and connection best practices**

Demonstrates two approaches to zone management:

1. **Custom Zone Pattern**: For production scenarios requiring fault tolerance (2+ replicas)
2. **Default Zone Pattern**: For development and testing scenarios (1 replica)
3. **Multi-node Connection**: Connect to all cluster nodes for optimal performance
4. **Data Operations**: Insert and read data across different zones
5. **SQL Queries**: Query across both custom and default zones

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.gettingstarted.HelloWorldApp"
```

**Expected Output:**

```
=== Hello World: Apache Ignite 3 ===
>>> Connected to all cluster nodes for optimal performance

=== Custom Zone Example (Production Pattern) ===
>>> Custom zone 'QuickStart' created (2 replicas for fault tolerance)
>>> Table 'SimpleBook' created in custom zone
>>> Books inserted into custom zone
>>> Retrieved from custom zone: Book{id=1, title='1984'}

=== Default Zone Example (Development Pattern) ===
>>> Table 'SimpleNote' created in default zone (no zone creation needed)
>>> Notes inserted into default zone
>>> Retrieved from default zone: Note{id=1, content='Remember to use multiple addresses for production'}

=== Zone Best Practices Summary ===
>>> Default Zone: Use for development, testing, simple scenarios (1 replica)
>>> Custom Zones: Use for production workloads requiring fault tolerance (2+ replicas)
>>> Performance: Always specify all cluster addresses for partition awareness

Success!
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
- **Default Zone**: Use for development and testing scenarios (automatically created with 1 replica)
- **Custom Zones**: Create for production workloads requiring fault tolerance (2+ replicas)  
- **Tables**: Define using simple POJO classes with @Table annotations (with or without zone specification)
- **Transactions**: Use for operations that must succeed or fail together
- **SQL**: Standard SQL works across tables in any zone

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
