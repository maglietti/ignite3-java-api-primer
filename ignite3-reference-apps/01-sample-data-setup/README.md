# Sample Data Setup - Apache Ignite 3 Reference

**Foundation module for Apache Ignite 3 music store sample dataset**

📖 **Related Documentation**: [Introduction & Overview](../../docs/01-introduction-overview.md)

## Overview

This module provides the foundation for all Apache Ignite 3 reference applications by creating a complete music store sample dataset. It demonstrates fundamental Ignite 3 concepts and provides a realistic data model for learning distributed systems patterns.

## What You'll Learn

- **Schema-as-Code**: Define tables using Java annotations
- **Distribution Zones**: Configure data placement and replication strategies  
- **Colocation**: Optimize performance by co-locating related data
- **Composite Keys**: Handle multi-field primary keys in distributed systems
- **Reference Data**: Manage lookup tables with high replication
- **Transactional Operations**: Load data safely with ACID guarantees

## Architecture

### Distribution Strategy

```
MusicStore Zone (2 replicas)          MusicStoreReplicated Zone (3 replicas)
├── Artist (root entity)              ├── Genre (lookup data)
├── Album (colocated by ArtistId)     └── MediaType (lookup data)
├── Track (colocated by AlbumId)       
├── Customer (root entity)             
├── Invoice (colocated by CustomerId)  
├── InvoiceLine (colocated by InvoiceId)
├── Employee (hierarchical)
├── Playlist 
└── PlaylistTrack (colocated by PlaylistId)
```

### Colocation Chains

1. **Music Hierarchy**: Artist → Album (by ArtistId) → Track (by AlbumId)
2. **Business Hierarchy**: Customer → Invoice (by CustomerId) → InvoiceLine (by InvoiceId)  
3. **Playlist Hierarchy**: Playlist → PlaylistTrack (by PlaylistId)

## Quick Start

> **Cluster Requirement**: All reference applications require the 3-node Ignite cluster from `00-docker` to be running. Start it with `docker-compose up -d` before proceeding.

### Basic Setup

```bash
mvn compile exec:java
# Creates schema and loads core sample data
```

### Extended Setup

```bash
mvn compile exec:java -Dexec.args="--extended"
# Creates schema and loads core + extended sample data
```

### Reset Setup

```bash
mvn compile exec:java -Dexec.args="--reset"
# Drops existing schema and recreates with core data

mvn compile exec:java -Dexec.args="--reset --extended"
# Drops existing schema and recreates with extended data
```

### Custom Cluster Address

```bash
mvn compile exec:java -Dexec.args="192.168.1.100:10800"
mvn compile exec:java -Dexec.args="192.168.1.100:10800 --extended"
mvn compile exec:java -Dexec.args="192.168.1.100:10800 --reset"
```

## Sample Dataset

### Music Entities (11 tables total)

- **5 Artists**: AC/DC, Accept, Aerosmith, Black Sabbath, Led Zeppelin
- **5 Albums**: Representative albums from various artists
- **8 Tracks**: Individual songs with metadata (duration, composer, price)
- **5 Genres**: Rock, Jazz, Metal, Alternative & Punk, Blues
- **3 Media Types**: MPEG, AAC, MPEG-4 video

### Business Entities  

- **3 Customers**: International customer base
- **3 Employees**: Hierarchical organization structure
- **2 Invoices**: Purchase transactions with line items
- **4 Playlists**: User-created music collections

## Key Concepts Demonstrated

### Schema-as-Code Example

```java
@Table(zone = @Zone(value = "MusicStore", storageProfiles = "default"),
       colocateBy = @ColumnRef("ArtistId"))
public class Album {
    @Id Integer AlbumId;
    @Id Integer ArtistId;  // Also the colocation key
    @Column(nullable = false) String Title;
}
```

### Distribution Zone Configuration

```java
// Business data - balanced replication
CREATE ZONE MusicStore WITH REPLICAS=2, PARTITIONS=25

// Reference data - high availability  
CREATE ZONE MusicStoreReplicated WITH REPLICAS=3, PARTITIONS=25
```

### Transactional Data Loading

```java
client.transactions().runInTransaction(tx -> {
    // Load related data together atomically
    artists.upsert(tx, artist);
    albums.upsert(tx, album);  
    tracks.upsert(tx, track);
});
```

## Integration with Other Modules

This module serves as the foundation for all other reference applications:

```java
// Other modules can reference the shared utilities
import com.apache.ignite.examples.setup.util.ConnectionUtils;

// Use the established connection and data
try (IgniteClient client = ConnectionUtils.connectToCluster()) {
    Table artistTable = client.tables().table("Artist");
    // Work with pre-loaded sample data
}
```

## Next Steps

After running this setup:

1. **Explore the Data**: Use SQL queries to see what was created
2. **Study the Schema**: Examine the annotated POJOs in `src/main/java/model/`
3. **Learn the APIs**: Move to other reference application modules:
   - [Getting Started](../02-getting-started-app/) - Basic operations
   - [Table API](../04-table-api-app/) - Object-oriented data access
   - [SQL API](../05-sql-api-app/) - Relational operations

## Related Documentation

- 📖 [Introduction & Overview](../../docs/01-introduction-overview.md) - Ignite 3 fundamentals
- 📖 [Getting Started](../../docs/02-getting-started.md) - Basic operations
- 📖 [Schema as Code](../../docs/03-schema-as-code-with-annotations.md) - Annotation-based schemas

## Prerequisites

- Java 17+
- Maven 3.8+
- **⚠️ Required**: Apache Ignite 3 cluster running (see [00-docker setup](../00-docker/README.md))