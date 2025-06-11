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

### Complete Setup (Recommended)

```bash
mvn exec:java
# Runs ProjectInitializationApp - complete setup with data and reports
```

### Individual Components

```bash
# Schema only
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.setup.app.SchemaCreationApp"

# Data only (requires existing schema)  
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.setup.app.DataLoadingApp"

# Analytics and reports
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.setup.app.SampleAnalyticsApp"
```

### Custom Cluster Address

```bash
mvn exec:java -Dexec.args="192.168.1.100:10800"
```

## Applications

### 1. ProjectInitializationApp

**Purpose**: Complete setup for new users  
**What it does**:

- Tests cluster connection
- Creates distribution zones and tables
- Loads sample data transactionally
- Verifies schema integrity
- Generates sample reports

**Best for**: First-time setup, complete environment initialization

### 2. SchemaCreationApp  

**Purpose**: Schema management operations  
**Modes**:

- `annotation` - Create schema from annotated POJOs (default)
- `sql` - Create schema from SQL scripts
- `drop` - Remove entire schema
- `info` - Display current schema information

**Best for**: Understanding schema-as-code, zone configuration

### 3. DataLoadingApp

**Purpose**: Data population and management  
**Modes**:

- `programmatic` - Load via Java POJOs (default)  
- `sql` - Load from SQL scripts
- `clear` - Remove all data
- `extended` - Load additional sample data

**Best for**: Learning data loading patterns, transaction handling

### 4. SampleAnalyticsApp

**Purpose**: Query examples and reporting  
**Modes**:

- `all` - All available reports (default)
- `artists` - Artist-focused analysis
- `albums` - Album-focused analysis  
- `tracks` - Track-focused analysis
- `customers` - Customer-focused analysis
- `sales` - Sales and revenue analysis
- `complex` - Complex cross-entity analysis

**Best for**: Understanding SQL capabilities, join performance

## Sample Dataset

### Music Entities (11 tables total)

- **5 Artists**: AC/DC, Accept, Aerosmith, Black Sabbath, Led Zeppelin, etc.
- **15 Albums**: Representative albums from various artists
- **15+ Tracks**: Individual songs with metadata (duration, composer, price)
- **10 Genres**: Rock, Jazz, Metal, Classical, etc.
- **7 Media Types**: MP3, AAC, FLAC, etc.

### Business Entities  

- **4 Customers**: International customer base
- **3 Employees**: Hierarchical organization structure
- **Sample Invoices**: Purchase transactions with line items
- **8 Playlists**: User-created music collections

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
// Other modules import the shared model
import com.apache.ignite.examples.setup.model.Artist;
import com.apache.ignite.examples.setup.util.DataSetupUtils;

// Use the established connection and data
try (IgniteClient client = DataSetupUtils.connectToCluster()) {
    Table artistTable = client.tables().table("Artist");
    // Work with pre-loaded sample data
}
```

## Troubleshooting

### Connection Issues

```bash
# Test connection
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.setup.app.SampleAnalyticsApp" -Dexec.args="127.0.0.1:10800 info"
```

### Schema Issues  

```bash
# Check schema status
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.setup.app.SchemaCreationApp" -Dexec.args="127.0.0.1:10800 info"

# Recreate schema
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.setup.app.SchemaCreationApp" -Dexec.args="127.0.0.1:10800 drop"
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.setup.app.SchemaCreationApp"
```

### Data Issues

```bash
# Clear and reload data
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.setup.app.DataLoadingApp" -Dexec.args="127.0.0.1:10800 clear"
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.setup.app.DataLoadingApp"
```

## Next Steps

After running this setup:

1. **Explore the Data**: Use `SampleAnalyticsApp` to see what was created
2. **Study the Schema**: Examine the annotated POJOs in `src/main/java/model/`
3. **Learn the APIs**: Move to other reference application modules:
   - [Getting Started](../getting-started-app/) - Basic operations
   - [Table API](../table-api-app/) - Object-oriented data access
   - [SQL API](../sql-api-app/) - Relational operations

## Related Documentation

- 📖 [Introduction & Overview](../../docs/01-introduction-overview.md) - Ignite 3 fundamentals
- 📖 [Getting Started](../../docs/02-getting-started.md) - Basic operations
- 📖 [Schema as Code](../../docs/03-schema-as-code-with-annotations.md) - Annotation-based schemas

## Prerequisites

- Java 17+
- Maven 3.8+
- **⚠️ Required**: Apache Ignite 3 cluster running (see [00-docker setup](../00-docker/README.md))

> **Cluster Requirement**: All reference applications require the 3-node Ignite cluster from `00-docker` to be running. Start it with `docker-compose up -d` before proceeding.
