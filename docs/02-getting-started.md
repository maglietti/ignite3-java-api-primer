# 2. Getting Started

## Setup & Dependencies

### Maven Dependencies

```xml
<dependency>
    <groupId>org.apache.ignite</groupId>
    <artifactId>ignite-client</artifactId>
    <version>3.0.0</version>
</dependency>
```

### Gradle Dependencies

```groovy
implementation 'org.apache.ignite:ignite-client:3.0.0'
```

### Basic Project Setup

#### Creating a New Maven Project

Create a new Maven project with the following structure:

```
your-ignite-project/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    └── IgniteHelloWorld.java
```

#### Complete pom.xml Configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>ignite3-hello-world</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <ignite.version>3.0.0</ignite.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.apache.ignite</groupId>
            <artifactId>ignite-client</artifactId>
            <version>${ignite.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

#### Gradle Alternative

For Gradle users, create a `build.gradle` file:

```groovy
plugins {
    id 'java'
    id 'application'
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.apache.ignite:ignite-client:3.0.0'
}

application {
    mainClass = 'com.example.IgniteHelloWorld'
}
```

## Connection Patterns

### Thin Client Connection

```java
try (IgniteClient client = IgniteClient.builder()
        .addresses("127.0.0.1:10800")
        .build()) {
    // Work with client
}
```

### Embedded Server Setup

```java
IgniteServer server = IgniteServer.start(nodeName, configPath, workDir);
```

### Configuration Basics

#### Advanced Client Configuration

```java
IgniteClient client = IgniteClient.builder()
    .addresses("localhost:10800", "localhost:10801", "localhost:10802")
    .connectTimeout(5000)  // 5 seconds
    .operationTimeout(30000)  // 30 seconds
    .heartbeatInterval(3000)  // 3 seconds
    .retryPolicy(RetryLimitPolicy.builder().retryLimit(3).build())
    .build();
```

#### SSL Configuration

```java
SslConfiguration sslConfig = SslConfiguration.builder()
    .enabled(true)
    .keyStorePath("/path/to/keystore.jks")
    .keyStorePassword("keystorePassword")
    .trustStorePath("/path/to/truststore.jks")
    .trustStorePassword("truststorePassword")
    .build();

IgniteClient client = IgniteClient.builder()
    .addresses("localhost:10800")
    .ssl(sslConfig)
    .build();
```

#### Authentication Setup

```java
IgniteClient client = IgniteClient.builder()
    .addresses("localhost:10800")
    .authenticator(BasicAuthenticator.builder()
        .username("ignite")
        .password("password")
        .build())
    .build();
```

#### Connection Pooling and Timeouts

```java
IgniteClient client = IgniteClient.builder()
    .addresses("localhost:10800")
    .connectTimeout(10000)  // Connection timeout in milliseconds
    .operationTimeout(60000)  // Operation timeout in milliseconds
    .heartbeatInterval(5000)  // Heartbeat interval in milliseconds
    .backgroundReconnectInterval(2000)  // Reconnection interval
    .build();
```

## First Steps

### Hello World Example

Here's a complete "Hello World" example that demonstrates connecting to Ignite, creating a simple table, and performing basic operations:

```java
package com.example;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.catalog.annotations.*;
import org.apache.ignite.catalog.definitions.ZoneDefinition;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;

import java.util.List;
import java.util.ArrayList;

public class IgniteHelloWorld {
    
    // Define a simple POJO for our example
    @Table(
        zone = @Zone(value = "hello_zone", storageProfiles = "default")
    )
    public static class Person {
        @Id
        @Column(value = "id", nullable = false)
        private Integer id;
        
        @Column(value = "name", nullable = false, length = 50)
        private String name;
        
        @Column(value = "email", nullable = true, length = 100)
        private String email;
        
        // Default constructor
        public Person() {}
        
        // Constructor with parameters
        public Person(Integer id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
        
        // Getters and setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        @Override
        public String toString() {
            return "Person{id=" + id + ", name='" + name + "', email='" + email + "'}";
        }
    }
    
    public static void main(String[] args) {
        // Connect to Ignite cluster
        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800")
                .build()) {
            
            System.out.println("Connected to Ignite cluster: " + client.connections());
            
            // Step 1: Create a distribution zone
            createDistributionZone(client);
            
            // Step 2: Create a table from our POJO
            createTable(client);
            
            // Step 3: Perform CRUD operations
            performCrudOperations(client);
            
            // Step 4: Query data with SQL
            queryWithSQL(client);
            
            System.out.println("Hello World example completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void createDistributionZone(IgniteClient client) {
        try {
            ZoneDefinition zone = ZoneDefinition.builder("hello_zone")
                .ifNotExists()
                .partitions(4)
                .replicas(1)
                .storageProfiles("default")
                .build();
                
            client.catalog().createZone(zone);
            System.out.println("Created distribution zone: hello_zone");
        } catch (Exception e) {
            System.out.println("Zone already exists or error: " + e.getMessage());
        }
    }
    
    private static void createTable(IgniteClient client) {
        try {
            client.catalog().createTable(Person.class);
            System.out.println("Created table: Person");
        } catch (Exception e) {
            System.out.println("Table already exists or error: " + e.getMessage());
        }
    }
    
    private static void performCrudOperations(IgniteClient client) {
        // Get table and create record view
        Table table = client.tables().table("Person");
        RecordView<Person> recordView = table.recordView(Person.class);
        
        // INSERT: Add some people
        List<Person> people = new ArrayList<>();
        people.add(new Person(1, "Alice Johnson", "alice@example.com"));
        people.add(new Person(2, "Bob Smith", "bob@example.com"));
        people.add(new Person(3, "Charlie Brown", "charlie@example.com"));
        
        recordView.upsertAll(null, people);
        System.out.println("Inserted " + people.size() + " people");
        
        // READ: Get a specific person
        Person key = new Person();
        key.setId(1);
        Person alice = recordView.get(null, key);
        System.out.println("Retrieved: " + alice);
        
        // UPDATE: Modify Alice's email
        alice.setEmail("alice.johnson@newcompany.com");
        recordView.upsert(null, alice);
        System.out.println("Updated Alice's email");
        
        // Verify the update
        Person updatedAlice = recordView.get(null, key);
        System.out.println("After update: " + updatedAlice);
    }
    
    private static void queryWithSQL(IgniteClient client) {
        System.out.println("\n--- Querying with SQL ---");
        
        // Query all people
        var result = client.sql().execute(null, "SELECT * FROM Person ORDER BY id");
        
        while (result.hasNext()) {
            var row = result.next();
            System.out.println("Person: ID=" + row.intValue("id") + 
                             ", Name=" + row.stringValue("name") + 
                             ", Email=" + row.stringValue("email"));
        }
        
        // Count people
        var countResult = client.sql().execute(null, "SELECT COUNT(*) as total FROM Person");
        if (countResult.hasNext()) {
            var row = countResult.next();
            System.out.println("Total people in database: " + row.longValue("total"));
        }
    }
}
```

### Running the Hello World Example

1. **Start the Ignite cluster** (using Docker):

   ```bash
   # Create docker-compose.yml
   version: '3.8'
   services:
     ignite-node:
       image: apacheignite/ignite:3.0.0
       ports:
         - "10300:10300"
         - "10800:10800"
       environment:
         - IGNITE_CLUSTER_NAME=hello-cluster
   
   # Start the cluster
   docker-compose up -d
   ```

2. **Initialize the cluster**:

   ```bash
   docker run --rm -it --network=host apacheignite/ignite:3.0.0 cli
   connect http://localhost:10300
   cluster init --name=hello-cluster --metastorage-group=ignite-node
   exit
   ```

3. **Compile and run your application**:

   ```bash
   mvn compile exec:java -Dexec.mainClass="com.example.IgniteHelloWorld"
   ```

### Expected Output

```
Connected to Ignite cluster: [localhost:10800]
Created distribution zone: hello_zone
Created table: Person
Inserted 3 people
Retrieved: Person{id=1, name='Alice Johnson', email='alice@example.com'}
Updated Alice's email
After update: Person{id=1, name='Alice Johnson', email='alice.johnson@newcompany.com'}

--- Querying with SQL ---
Person: ID=1, Name=Alice Johnson, Email=alice.johnson@newcompany.com
Person: ID=2, Name=Bob Smith, Email=bob@example.com
Person: ID=3, Name=Charlie Brown, Email=charlie@example.com
Total people in database: 3
Hello World example completed successfully!
```

### Key Concepts Demonstrated

1. **Connection Management**: Proper use of try-with-resources
2. **Schema Definition**: Using annotations to define tables
3. **Distribution Zones**: Creating zones for data partitioning
4. **CRUD Operations**: Insert, read, update using the Table API
5. **SQL Integration**: Querying data with SQL
6. **Resource Cleanup**: Automatic cleanup with AutoCloseable

### Next Steps

After running this example successfully, you can:

- Explore more complex data models with relationships
- Learn about co-location for performance optimization
- Implement transaction patterns
- Try bulk data loading techniques
- Experiment with different distribution strategies
