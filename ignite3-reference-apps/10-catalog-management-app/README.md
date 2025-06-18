# Catalog Management App - Apache Ignite 3 Reference

**Programmatic schema and catalog management for music streaming platforms**

📖 **Related Documentation**: [Schema and Catalog Management](../../docs/02-schema-design/04-schema-evolution.md)

## Overview

This reference application demonstrates catalog management capabilities for Apache Ignite 3, focusing on building and evolving database schemas for music streaming platforms. Learn how to manage distribution zones, create tables programmatically, and implement schema evolution strategies using real-world scenarios.

## What You'll Learn

- **Distribution Zone Management**: Create and optimize zones for different workload patterns (OLTP, OLAP, streaming)
- **Programmatic Schema Creation**: Build tables using both annotation-based and builder API approaches
- **Schema Introspection**: Analyze existing schemas and generate documentation
- **Colocation Strategies**: Implement data locality optimization for performance
- **Performance Optimization**: Configure partitioning, replication, and indexing for specific use cases
- **Operational Management**: Monitor zone health, implement migrations, and establish best practices
- **Environment Configuration**: Adapt configurations for development, testing, and production environments

## Architecture

### Distribution Zone Strategy

The application demonstrates a multi-zone architecture optimized for different workload patterns:

```
Distribution Zones
├── MusicCatalogOLTP (16 partitions, 3 replicas)
│   └── Optimized for transactional catalog operations
├── MusicAnalyticsOLAP (64 partitions, 2 replicas)
│   └── Optimized for analytics and reporting workloads
├── UserInteractionStream (128 partitions, 1 replica)
│   └── Optimized for high-throughput streaming data
└── MusicMetadataCache (8 partitions, 5 replicas)
    └── Optimized for frequently accessed reference data
```

### Schema Hierarchy

```
Music Store Schema
├── Core Catalog Tables
│   ├── Artist (root entity)
│   ├── Album (colocated by ArtistId)
│   └── Track (colocated by AlbumId)
├── Customer Business Tables
│   ├── Customer (root entity)
│   ├── Invoice (colocated by CustomerId)
│   └── InvoiceLine (colocated by InvoiceId)
└── Performance Indexes
    ├── Name-based search indexes
    ├── Genre and category indexes
    └── Date and price range indexes
```

## Prerequisites

- Java 17+
- Maven 3.8+
- **⚠️ Required**: Apache Ignite 3 cluster running (see [00-docker setup](../00-docker/README.md))
- **⚠️ Required**: Sample data setup completed (see [01-sample-data-setup](../01-sample-data-setup/README.md))

## Quick Start

```bash
# Run the complete interactive demonstration
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.catalog.CatalogManagementDemo"

# Run non-interactive mode for automation
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.catalog.CatalogManagementDemo" -Dexec.args="--non-interactive"

# Run individual components
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.catalog.SchemaOperations"
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.catalog.CatalogManagement"
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.catalog.ZoneConfiguration"
```

## Applications

### Reference Applications

1. **CatalogManagementDemo** ⭐ - Complete interactive demonstration of all catalog management concepts
2. **SchemaOperations** - Schema introspection, analysis, and documentation generation
3. **CatalogManagement** - Programmatic table creation with colocation and indexing strategies
4. **ZoneConfiguration** - Distribution zone management for different workload patterns

### Key Features Demonstrated

- **🏗️ Zone Management**: Create OLTP, OLAP, streaming, and cache zones with optimal configurations
- **📋 Schema Creation**: Build tables using both annotation-based and programmatic approaches
- **🔍 Schema Analysis**: Introspect existing schemas and generate detailed documentation
- **⚡ Performance Tuning**: Optimize configurations for music streaming platform requirements
- **🛠️ Operational Patterns**: Health monitoring, migration strategies, and best practices

## Example Output

The demonstration generates detailed reports and shows real-time progress:

```
╔══════════════════════════════════════════════════════════════╗
║          Apache Ignite 3 Catalog Management Demo            ║
║               Music Streaming Platform Edition              ║
╚══════════════════════════════════════════════════════════════╝

🎯 Workload-Specific Zones Created:
  MusicCatalogOLTP      → 16 partitions, 3 replicas (OLTP)
  MusicAnalyticsOLAP    → 64 partitions, 2 replicas (OLAP)
  UserInteractionStream → 128 partitions, 1 replica (Streaming)
  MusicMetadataCache    → 8 partitions, 5 replicas (Caching)

🎵 Music Catalog Tables Created:
  Artist → Root entity with name and genre indexes
  Album  → Colocated by ArtistId with genre/price indexes
  Track  → Colocated by AlbumId with genre and duration indexes

📋 Table Analysis: Artist
─────────────────────────────────────────────────────────────
Zone: MusicCatalogOLTP
Primary Key: [ArtistId]
🎯 Colocation Strategy: [ArtistId]
   → Related data is co-located for optimal query performance
```

## Related Documentation

- 📖 [Schema and Catalog Management](../../docs/02-schema-design/04-schema-evolution.md) - Complete API guide with music streaming examples
- 📖 [Schema as Code](../../docs/02-schema-design/01-basic-annotations.md) - Annotation-based table definitions
- 📖 [SQL API](../../docs/03-data-access-apis/02-sql-api-analytics.md) - DDL operations and database interactions

## Status

✅ **Complete** - Module 10 implementation includes catalog management demonstrations with four fully functional reference applications showcasing zone management, schema creation, introspection, and operational best practices for music streaming platforms.