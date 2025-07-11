# Apache Ignite 3 Getting Started Application

Basic connection patterns and operations using Apache Ignite 3's Java API.

**Related Documentation**: [Getting Started Guide](../../docs/01-foundation/02-getting-started.md)

## Overview

Demonstrates fundamental Apache Ignite 3 operations: connecting to a cluster, creating tables, and performing basic CRUD operations using both Table and SQL APIs.

## What You'll Learn

- Client connection patterns with multi-node clusters
- Table creation using POJO annotations
- CRUD operations with Table API
- SQL queries on the same data model
- Transaction basics for related data
- Error handling and resource management

## Prerequisites

- Apache Ignite 3 cluster running (see [00-docker setup](../00-docker/README.md))
- Java 17 or higher
- Maven 3.8+ or Gradle (via wrapper)

```bash
# Start cluster first
cd ../00-docker && docker-compose up -d
```

## Applications

| Application | Description | Run Command |
|-------------|-------------|-------------|
| HelloWorldApp | The simplest introduction to Apache Ignite 3. Creates a Book table, performs CRUD operations using the RecordView API, and demonstrates SQL queries. Uses multi-node connection for partition awareness. | `../gradlew runHelloWorld` |
| BasicSetupDemo | Demonstrates working with related tables (Author and Book) using foreign key relationships. Shows zone creation, transactional inserts, and JOIN queries for retrieving related data. | `../gradlew runBasicSetupDemo` |
| ConnectionExamples | Showcases different client connection patterns including single-node development connections, multi-node production configurations with failover, and performance testing of SQL queries. | `../gradlew runConnectionExamples` |

### Running the Applications

From this directory, use Gradle to run each application:

```bash
# Hello World - Basic CRUD operations
../gradlew runHelloWorld

# Basic Setup - Related tables and transactions  
../gradlew runBasicSetupDemo

# Connection Examples - Various connection patterns
../gradlew runConnectionExamples

# Custom cluster address
../gradlew runHelloWorld --args="192.168.1.100:10800"
```

## Application Details

### 1. HelloWorldApp

Connects to Ignite cluster and demonstrates basic operations:

1. Multi-node connection for partition awareness
2. Table creation in default zone
3. Type-safe CRUD operations with Table API
4. SQL queries on the same data
5. Proper resource cleanup

**Maven:**
```bash
mvn compile exec:java@hello
```

**Gradle:**
```bash
../gradlew runHelloWorld
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
>>> Demo completed successfully
```

### 2. BasicSetupDemo

Creates related tables and demonstrates transactions:

- Two tables (Author and Book)
- Transaction to insert related records
- JOIN query to combine data

**Maven:**
```bash
mvn compile exec:java@setup
```

**Gradle:**
```bash
../gradlew runBasicSetupDemo
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

Demonstrates various client connection configurations:

- Basic connection (development)
- Multi-node connection (production failover)
- Connection with custom timeouts

**Maven:**
```bash
mvn compile exec:java@connection
```

**Gradle:**
```bash
../gradlew runConnectionExamples
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

- Specify all cluster node addresses for partition awareness
- Default zone requires zero configuration
- Table API provides type-safe operations
- SQL API enables flexible queries
- POJOs work across both APIs
- Use try-with-resources for cleanup
