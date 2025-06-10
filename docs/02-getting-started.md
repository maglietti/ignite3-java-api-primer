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

Here's a complete "Hello World" example that demonstrates connecting to Ignite, creating a simple table, and performing basic operations using the Chinook music dataset:

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
    
    // Define a simple POJO for our music store example
    @Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default")
    )
    public static class Artist {
        @Id
        @Column(value = "ArtistId", nullable = false)
        private Integer ArtistId;
        
        @Column(value = "Name", nullable = false, length = 120)
        private String Name;
        
        // Default constructor
        public Artist() {}
        
        // Constructor with parameters
        public Artist(Integer artistId, String name) {
            this.ArtistId = artistId;
            this.Name = name;
        }
        
        // Getters and setters
        public Integer getArtistId() { return ArtistId; }
        public void setArtistId(Integer artistId) { this.ArtistId = artistId; }
        public String getName() { return Name; }
        public void setName(String name) { this.Name = name; }
        
        @Override
        public String toString() {
            return "Artist{ArtistId=" + ArtistId + ", Name='" + Name + "'}";
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
            ZoneDefinition zone = ZoneDefinition.builder("Chinook")
                .ifNotExists()
                .partitions(4)
                .replicas(2)
                .storageProfiles("default")
                .build();
                
            client.catalog().createZone(zone);
            System.out.println("Created distribution zone: Chinook");
        } catch (Exception e) {
            System.out.println("Zone already exists or error: " + e.getMessage());
        }
    }
    
    private static void createTable(IgniteClient client) {
        try {
            client.catalog().createTable(Artist.class);
            System.out.println("Created table: Artist");
        } catch (Exception e) {
            System.out.println("Table already exists or error: " + e.getMessage());
        }
    }
    
    private static void performCrudOperations(IgniteClient client) {
        // Get table and create record view
        Table table = client.tables().table("Artist");
        RecordView<Artist> recordView = table.recordView(Artist.class);
        
        // INSERT: Add some artists
        List<Artist> artists = new ArrayList<>();
        artists.add(new Artist(1, "The Beatles"));
        artists.add(new Artist(2, "Led Zeppelin"));
        artists.add(new Artist(3, "Pink Floyd"));
        
        recordView.upsertAll(null, artists);
        System.out.println("Inserted " + artists.size() + " artists");
        
        // READ: Get a specific artist
        Artist key = new Artist();
        key.setArtistId(1);
        Artist beatles = recordView.get(null, key);
        System.out.println("Retrieved: " + beatles);
        
        // UPDATE: Modify the artist name (just for demonstration)
        beatles.setName("The Beatles (Remastered)");
        recordView.upsert(null, beatles);
        System.out.println("Updated Beatles entry");
        
        // Verify the update
        Artist updatedBeatles = recordView.get(null, key);
        System.out.println("After update: " + updatedBeatles);
    }
    
    private static void queryWithSQL(IgniteClient client) {
        System.out.println("\n--- Querying with SQL ---");
        
        // Query all artists
        var result = client.sql().execute(null, "SELECT * FROM Artist ORDER BY ArtistId");
        
        while (result.hasNext()) {
            var row = result.next();
            System.out.println("Artist: ID=" + row.intValue("ArtistId") + 
                             ", Name=" + row.stringValue("Name"));
        }
        
        // Count artists
        var countResult = client.sql().execute(null, "SELECT COUNT(*) as total FROM Artist");
        if (countResult.hasNext()) {
            var row = countResult.next();
            System.out.println("Total artists in database: " + row.longValue("total"));
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
Created distribution zone: Chinook
Created table: Artist
Inserted 3 artists
Retrieved: Artist{ArtistId=1, Name='The Beatles'}
Updated Beatles entry
After update: Artist{ArtistId=1, Name='The Beatles (Remastered)'}

--- Querying with SQL ---
Artist: ID=1, Name=The Beatles (Remastered)
Artist: ID=2, Name=Led Zeppelin
Artist: ID=3, Name=Pink Floyd
Total artists in database: 3
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

- Explore the full Chinook music dataset with Albums and Tracks
- Learn about co-location for performance optimization (Album colocated with Artist)
- Implement transaction patterns for related data operations
- Try bulk data loading techniques with the complete Chinook dataset
- Experiment with different distribution strategies for music and business data
