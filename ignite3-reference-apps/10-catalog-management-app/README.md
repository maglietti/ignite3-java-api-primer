# Catalog Management App - Apache Ignite 3 Reference

**Schema and catalog management operations with Apache Ignite 3**

📖 **Related Documentation**: [Schema and Catalog Management](../../docs/10-schema-and-catalog-management.md)

## Overview

This reference application demonstrates comprehensive schema and catalog management operations in Apache Ignite 3, including dynamic schema evolution, distribution zone management, and metadata operations using the music store sample dataset.

## What You'll Learn

- **Dynamic Schema Evolution**: Adding, modifying, and removing columns and indexes
- **Distribution Zone Management**: Creating, configuring, and managing data placement zones
- **Table Operations**: Creating, altering, and dropping tables programmatically
- **Index Management**: Creating and managing various index types for performance
- **Metadata Queries**: Accessing and analyzing schema metadata
- **Zone Rebalancing**: Managing data redistribution across nodes
- **Schema Versioning**: Handling schema changes in distributed environments
- **Catalog APIs**: Programmatic access to cluster metadata

## Architecture

### Catalog Structure

```
Ignite Catalog
├── Distribution Zones
│   ├── MusicStore (2 replicas)
│   └── MusicStoreReplicated (3 replicas)
├── Tables
│   ├── Music Entities (Artist, Album, Track)
│   ├── Business Entities (Customer, Invoice, InvoiceLine)
│   └── Reference Data (Genre, MediaType)
└── Indexes
    ├── Primary Key Indexes
    ├── Foreign Key Indexes
    └── Secondary Indexes
```

### Management Operations

- **Schema Evolution**: Add new columns to existing tables
- **Zone Configuration**: Modify replica counts and partitioning
- **Index Optimization**: Create performance-oriented indexes
- **Metadata Analysis**: Query system tables for insights

## Prerequisites

- Java 17+
- Maven 3.8+
- **⚠️ Required**: Apache Ignite 3 cluster running (see [00-docker setup](../00-docker/README.md))
- **⚠️ Required**: Sample data setup completed (see [01-sample-data-setup](../01-sample-data-setup/README.md))

## Quick Start

```bash
# Run complete catalog management demonstration
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.catalog.CatalogManagementDemo"

# Run specific management operations
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.catalog.SchemaOperations"
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.catalog.ZoneConfiguration"
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.catalog.IndexManagement"
```

## Applications

### Planned Reference Applications

1. **CatalogManagementDemo** - Overview of all catalog operations
2. **SchemaOperations** - Dynamic schema evolution examples
3. **ZoneConfiguration** - Distribution zone management
4. **IndexManagement** - Creating and optimizing indexes
5. **MetadataQueries** - Accessing cluster metadata
6. **SchemaEvolution** - Handling schema changes safely
7. **PerformanceAnalysis** - Schema-related performance optimization

## Related Documentation

- 📖 [Schema and Catalog Management](../../docs/10-schema-and-catalog-management.md) - Detailed catalog operations
- 📖 [Schema as Code](../../docs/03-schema-as-code-with-annotations.md) - Annotation-based schemas
- 📖 [SQL API](../../docs/05-sql-api-relational-data-access.md) - DDL operations

## Status

🚧 **Under Development** - This module is part of the planned Phase 2 implementation. Reference applications will be built using the established sample data and patterns from the sample-data-setup module.