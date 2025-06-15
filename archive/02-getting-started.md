# 2. Getting Started

## 2.1 Your First Steps with Apache Ignite 3

### Why Start Here?

Apache Ignite 3 represents a fundamental shift in distributed database architecture. Unlike traditional databases that separate storage and compute, Ignite 3 co-locates data with processing power, enabling applications that think and act at memory speed while maintaining the durability and consistency of persistent storage.

This module introduces you to the essential patterns every Ignite 3 developer needs to master. By the end, you'll understand how to connect, store, and query data in a distributed environment that scales effortlessly from development to production.

## 2.2 Environment Setup

### Dependencies You Need

For any Ignite 3 application, you need just one dependency:

```xml
<dependency>
    <groupId>org.apache.ignite</groupId>
    <artifactId>ignite-client</artifactId>
    <version>3.0.0</version>
</dependency>
```

That's it! The client library includes everything needed to connect, define schemas, and perform operations.

### Running Cluster Setup

Before your first application, you need an Ignite 3 cluster running. The simplest approach uses Docker:

```bash
# Clone and start the reference cluster
git clone <your-repo>
cd ignite3-reference-apps/00-docker
docker-compose up -d

# Initialize the cluster (one-time setup)
./init-cluster.sh
```

This gives you a 3-node cluster perfect for development and learning.

## 2.3 Core Concepts in Action

### Your First Connection

Connecting to Ignite 3 is straightforward. The client automatically handles connection pooling, failover, and resource management:

```java
try (IgniteClient client = IgniteClient.builder()
        .addresses("localhost:10800")
        .build()) {
    
    // Your application logic here
    System.out.println("Connected to: " + client.connections());
}
// Client automatically closes and cleans up resources
```

**Why This Works:**
- **Try-with-resources**: Ensures proper cleanup even if exceptions occur
- **Single address**: For development; production uses multiple addresses for failover
- **Default timeouts**: Reasonable defaults for most applications

### Creating Your First Table

Instead of writing SQL DDL, Ignite 3 lets you define tables using Java classes:

```java
@Table(zone = @Zone(value = "QuickStart", storageProfiles = "default"))
public class Book {
    @Id
    @Column(value = "id", nullable = false)
    private Integer id;
    
    @Column(value = "title", nullable = false, length = 100)
    private String title;
    
    @Column(value = "author", nullable = false, length = 50)
    private String author;
    
    // Constructor, getters, setters...
}
```

**What This Achieves:**
- **Type Safety**: Your schema is validated at compile time
- **Automatic DDL**: Ignite generates the table structure
- **Zone Assignment**: Data goes to the "QuickStart" distribution zone
- **Performance**: Annotations drive indexing and partitioning strategies

### The Distribution Zone Concept

Before creating tables, you need a distribution zone - think of it as a "container" that controls how your data is distributed:

```java
// Create a zone for our application data
ZoneDefinition zone = ZoneDefinition.builder("QuickStart")
    .ifNotExists()
    .replicas(2)        // Keep 2 copies of each piece of data
    .storageProfiles("default")
    .build();

client.catalog().createZone(zone);
```

**Zone Benefits:**
- **Fault Tolerance**: Multiple replicas protect against node failures
- **Performance Control**: Choose optimal replica count for your workload
- **Storage Selection**: Pick storage engines optimized for your access patterns

## 2.4 Your First Complete Application

### Complete Working Example

Here's a minimal but complete Ignite 3 application that demonstrates all the essential patterns:

```java
package com.example;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.catalog.annotations.*;
import org.apache.ignite.catalog.definitions.ZoneDefinition;
import org.apache.ignite.table.RecordView;

public class HelloIgnite {
    
    @org.apache.ignite.catalog.annotations.Table(zone = @Zone(value = "QuickStart", storageProfiles = "default"))
    public static class Book {
        @Id
        private Integer id;
        
        @Column(length = 100)
        private String title;
        
        public Book() {}
        public Book(Integer id, String title) {
            this.id = id;
            this.title = title;
        }
        
        // Getters and setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String toString() {
            return "Book{id=" + id + ", title='" + title + "'}";
        }
    }
    
    public static void main(String[] args) {
        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800")
                .build()) {
            
            // 1. Create zone
            client.catalog().createZone(
                ZoneDefinition.builder("QuickStart")
                    .ifNotExists()
                    .replicas(2)
                    .storageProfiles("default")
                    .build()
            );
            
            // 2. Create table
            client.catalog().createTable(Book.class);
            
            // 3. Insert data
            RecordView<Book> books = client.tables()
                .table("Book")
                .recordView(Book.class);
            
            books.upsert(null, new Book(1, "1984"));
            
            // 4. Read data
            Book book = books.get(null, new Book(1, null));
            System.out.println("Found: " + book);
            
            // 5. Query with SQL
            var result = client.sql().execute(null, "SELECT * FROM Book");
            while (result.hasNext()) {
                var row = result.next();
                System.out.println("SQL result: " + row.stringValue("title"));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### What This Example Shows

**In just 40 lines**, this application demonstrates:

1. **Connection**: Simple client setup with automatic resource management
2. **Schema**: Table creation from Java classes using annotations
3. **Distribution**: Zone configuration controls data placement
4. **Storage**: Insert and retrieve operations using type-safe APIs  
5. **Querying**: SQL access to the same data

### Running the Example

```bash
# 1. Start cluster (reference Docker setup)
cd ignite3-reference-apps/00-docker && docker-compose up -d

# 2. Run the application
javac -cp ignite-client-3.0.0.jar HelloIgnite.java
java -cp .:ignite-client-3.0.0.jar HelloIgnite
```

**Expected Output:**
```
Found: Book{id=1, title='1984'}
SQL result: 1984
```

## 2.5 Understanding What Happened

### The Distribution Zone

When you created the "QuickStart" zone, Ignite 3 configured how your data spreads across cluster nodes:

```mermaid
flowchart TD
    A["QuickStart Zone"] --> B["Node 1: Replica 1"]
    A --> C["Node 2: Replica 2"] 
    A --> D["Node 3: Backup"]
    
    B --> E["Book table partition"]
    C --> F["Book table partition"]
```

**Why This Matters:**
- **Fault Tolerance**: With 2 replicas, your data survives node failures
- **Load Distribution**: Reads and writes spread across multiple nodes
- **Scalability**: Add more nodes to handle more data and traffic

### The Table Creation Process

```mermaid
sequenceDiagram
    participant App as Application
    participant Client as IgniteClient
    participant Catalog as Catalog Service
    participant Nodes as Cluster Nodes
    
    App->>Client: createTable(Book.class)
    Client->>Catalog: Register table schema
    Catalog->>Nodes: Distribute table definition
    Nodes->>Nodes: Create partitions in QuickStart zone
    Catalog->>Client: Table ready
    Client->>App: Success
```

### Type Safety in Action

Notice how the same `Book` class works for both object operations and SQL queries:

```java
// Object API - type safe
books.upsert(null, new Book(1, "1984"));
Book book = books.get(null, new Book(1, null));

// SQL API - same data, different access pattern
var result = client.sql().execute(null, "SELECT * FROM Book");
```

**This dual API approach means:**
- Use objects when you know exact keys and want type safety
- Use SQL for complex queries, joins, and analytical operations
- Both APIs access the same underlying distributed data

## 2.6 Building from Here

Now that you understand the basics, you're ready to explore:

**[Module 3: Schema Annotations](03-schema-as-code-with-annotations.md)** - Advanced table design patterns
**[Module 4: Table API](04-table-api-object-oriented-data-access.md)** - Object-oriented data operations  
**[Module 5: SQL API](05-sql-api-relational-data-access.md)** - Relational query patterns

**Hands-on Practice:** The [reference applications](../ignite3-reference-apps/) provide working examples of every concept, from simple getting started patterns through complex production scenarios using the complete music store dataset.
