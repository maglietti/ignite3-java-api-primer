# Sample Data Setup - Apache Ignite 3 Reference

**Foundation module for Apache Ignite 3 music store sample dataset**

**Related Documentation**: [Introduction & Overview](../../docs/01-foundation/01-introduction-and-architecture.md)

## Overview

This module provides the foundation for all Apache Ignite 3 reference applications by creating a complete music store sample dataset. It demonstrates fundamental Ignite 3 concepts and provides a realistic data model for learning distributed systems patterns.

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

> **Cluster Requirement**: All reference applications require the 3-node Ignite cluster from `00-docker` to be running. Start it with `docker-compose up -d` before proceeding.

### Basic Setup (Core Dataset)

```bash
# Interactive setup with progress tracking
# Creates schema with 11 tables and 2 distribution zones
# Loads core sample data (5 artists, 5 albums, 5 tracks)
# If schema exists, presents friendly options menu
mvn compile exec:java
```

### Extended Setup (Complete Dataset)

```bash
# Complete music store catalog setup
# Skips core sample data and loads complete 15,866-line SQL script instead
# Includes 275+ artists, 3,500+ tracks - everything you need
# Uses optimized batch processing for performance
# Takes 2-3 minutes with detailed progress reporting
mvn compile exec:java -Dexec.args="--extended"
```

### Reset Setup (Clean Slate)

```bash
# Drops existing schema and recreates with core data
# Perfect for clean development environment
mvn compile exec:java -Dexec.args="--reset"

# Complete clean slate with full dataset
# Recommended for complete learning environment
mvn compile exec:java -Dexec.args="--reset --extended"
```

### Custom Cluster Address

```bash
mvn compile exec:java -Dexec.args="192.168.1.100:10800"
mvn compile exec:java -Dexec.args="192.168.1.100:10800 --extended"
mvn compile exec:java -Dexec.args="192.168.1.100:10800 --reset"
```

### What You'll See

The application provides detailed progress tracking and status updates:

- **Welcome Banner**: Overview of what the setup will accomplish
- **Connection Status**: Clear connection feedback and troubleshooting tips
- **Schema Progress**: Step-by-step table creation with descriptions
- **Data Loading**: Transactional batch operations with progress indicators
- **Bulk Loading**: Real-time progress for extended dataset processing
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

> **Data Loading Logic**: The application uses **exclusive data loading** - either core sample data OR complete dataset, never both. The `--extended` flag replaces the sample data with the complete catalog.

### Core Data (Default)

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

### Complete Dataset (--extended)

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

## Prerequisites

- Java 17+
- Maven 3.8+
- **⚠️ Required**: Apache Ignite 3 cluster running (see [00-docker setup](../00-docker/README.md))
