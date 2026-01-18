# Sample Data Setup - Apache Ignite 3 Reference

**Foundation module for Apache Ignite 3 music store sample dataset**

**Related Documentation**: [Introduction & Overview](../../docs/01-foundation/01-introduction-and-architecture.md)

## Overview

This module provides the foundation for all Apache Ignite 3 reference applications by creating a complete music store sample dataset. It demonstrates fundamental Ignite 3 concepts and provides a realistic data model for learning distributed systems patterns.

## Prerequisites

- Java 17+
- Maven 3.8+

> [!WARNING]
> Apache Ignite 3 cluster must be running (see [00-docker setup](../00-docker/README.md))

## Application

| Application | Description | Run Command |
|-------------|-------------|-------------|
| MusicStoreSetup | Initializes the music store database schema and populates it with sample data. This application creates 11 tables across 2 distribution zones and loads a complete dataset with 15,000+ records by default, or core sample data when using --core flag. It supports schema reset, custom cluster addresses, and provides detailed progress tracking throughout the setup process. | `./gradlew setupData` |

> [!NOTE]
> For automation and CI/CD, use `./gradlew setupDataAuto` which runs non-interactively with `--reset --core` flags.

## What You'll Learn

- **Schema-as-Code**: Define tables using Java annotations
- **Distribution Zones**: Configure data placement and replication strategies  
- **Colocation**: Performance through co-locating related data
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

> [!TIP]
> **Cluster Requirement**: All reference applications require the 3-node Ignite cluster from `00-docker` to be running. Start it with `docker-compose up -d` before proceeding.

### Complete Dataset Setup (Default)

```bash
# Interactive setup with progress tracking
# Creates schema with 11 tables and 2 distribution zones
# Loads complete dataset (15,000+ records)
# If schema exists, presents an options menu
mvn compile exec:java
```

### Core Dataset Setup (Minimal Data)

```bash
# Minimal sample data for quick testing
# Loads core sample data only (5 artists, 5 albums, 5 tracks)
# Perfect for development and basic testing
mvn compile exec:java -Dexec.args="--core"
```

### Reset Setup (Clean Slate)

```bash
# Drops existing schema and recreates with complete dataset
# Perfect for clean development environment
mvn compile exec:java -Dexec.args="--reset"

# Complete clean slate with minimal data
# Fastest setup for basic testing
mvn compile exec:java -Dexec.args="--reset --core"
```

### Gradle Commands

**From the root `ignite3-reference-apps` directory** (recommended):

```bash
# Interactive setup with complete dataset (default)
./gradlew setupData

# Interactive setup with options
./gradlew setupData --args="--reset"
./gradlew setupData --args="--core"
./gradlew setupData --args="--reset --core"

# Non-interactive setup (uses --reset --core, no prompts)
./gradlew setupDataAuto
```

**From this directory** (`01-sample-data-setup`):

```bash
# Complete dataset setup (default)
../gradlew runMusicStoreSetup

# Core dataset setup (minimal data)
../gradlew runMusicStoreSetup --args="--core"

# Reset and reload complete dataset
../gradlew runMusicStoreSetup --args="--reset"

# Reset and load minimal data
../gradlew runMusicStoreSetup --args="--reset --core"

# Custom cluster address with complete dataset
../gradlew runMusicStoreSetup --args="192.168.1.100:10800"

# Custom cluster address with minimal data
../gradlew runMusicStoreSetup --args="192.168.1.100:10800 --core"

# Show help with all options
../gradlew runMusicStoreSetup --args="--help"
```

> [!TIP]
> Use `setupDataAuto` for automation and CI/CD pipelines. It runs non-interactively with `--reset --core` flags, avoiding any prompts that could cause hangs.

### Custom Cluster Address

```bash
# Custom cluster with complete dataset (default)
mvn compile exec:java -Dexec.args="192.168.1.100:10800"

# Custom cluster with minimal data
mvn compile exec:java -Dexec.args="192.168.1.100:10800 --core"

# Custom cluster with reset and complete dataset
mvn compile exec:java -Dexec.args="192.168.1.100:10800 --reset"

# Custom cluster with reset and minimal data
mvn compile exec:java -Dexec.args="192.168.1.100:10800 --reset --core"

# Show help with all available options
mvn compile exec:java -Dexec.args="--help"
```

### What You'll See

The application provides detailed progress tracking and status updates:

- **Welcome Banner**: Overview of what the setup will accomplish
- **Connection Status**: Clear connection feedback and troubleshooting tips
- **Schema Progress**: Step-by-step table creation with descriptions
- **Data Loading**: Transactional batch operations with progress indicators
- **Bulk Loading**: Real-time progress for complete dataset processing
- **Verification**: Complete data validation with record counts
- **Success Summary**: Final status with next steps and available tables

### User-Friendly Features

- **Checkpoint System**: 5 clear checkpoints with progress indicators
- **Error Guidance**: Helpful troubleshooting tips for common issues
- **Interactive Prompts**: Friendly menus for existing schema conflicts
- **Progress Reporting**: Real-time updates during long-running operations
- **Comfort Noise**: Reassuring messages about normal Ignite behavior
- **Visual Feedback**: Clear formatting for easy scanning

## Sample Dataset

> [!NOTE]
> **Data Loading Logic**: The application loads **complete dataset by default** (15,000+ records). Use `--core` flag for minimal sample data during development and testing.

### Complete Dataset (Default)

**Full Music Store Dataset**:

- **275+ Artists**: Complete music artist catalog
- **347+ Albums**: Full album collection across genres
- **3,503+ Tracks**: Complete track library with metadata
- **25 Genres**: Full genre classification
- **5 Media Types**: All supported media formats
- **59 Customers**: Complete customer database
- **8 Employees**: Full organizational structure
- **412+ Invoices**: Complete transaction history
- **2,240+ Invoice Lines**: Detailed purchase records
- **18 Playlists**: User-generated playlists
- **8,715+ Playlist Tracks**: Complete playlist associations

### Core Dataset (--core)

**Music Entities**:

- **5 Artists**: AC/DC, Accept, Aerosmith, Black Sabbath, Led Zeppelin
- **5 Albums**: Representative albums from various artists
- **5 Tracks**: Individual songs with metadata (duration, composer, price)
- **5 Genres**: Rock, Jazz, Metal, Alternative & Punk, Blues
- **3 Media Types**: MPEG, AAC, MPEG-4 video

**Business Entities**:

- **3 Customers**: International customer base
- **3 Employees**: Hierarchical organization structure
- **2 Invoices**: Purchase transactions with line items

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
