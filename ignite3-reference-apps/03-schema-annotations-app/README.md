# 03-schema-annotations-app

Apache Ignite 3 Schema-as-Code with Annotations Reference Application

## Overview

This module demonstrates Apache Ignite 3's schema-as-code capabilities using annotations. Learn how to define database schemas directly in Java code using annotations, eliminating the need for separate DDL scripts while gaining type safety and performance optimizations.

## Key Features Demonstrated

### Core Annotation System

- **@Table**: Table definition and configuration
- **@Zone**: Distribution zone assignment and replication
- **@Column**: Field-to-column mapping with constraints
- **@Id**: Primary key designation (single and composite)
- **@ColumnRef**: Column references for colocation and indexing
- **@Index**: Secondary index definitions

### Schema Patterns

- **Simple Entities**: Single primary key, basic fields
- **Composite Keys**: Multi-field primary keys for relationships
- **Colocation Hierarchies**: Performance optimization through data placement
- **Reference Data**: High-replication patterns for lookup tables
- **Complex Entities**: Multiple indexes, relationships, and constraints

### Performance Optimizations

- **Data Colocation**: Related data stored on same cluster nodes
- **Index Strategies**: Optimized query performance
- **Zone Configuration**: Different replication strategies for different data types
- **Single-Node Queries**: Colocated joins without network overhead

## Applications Included

### 1. AnnotatedEntitiesDemo.java

**Purpose**: Comprehensive demonstration of annotation patterns
**Features**:

- Table creation from annotated POJOs
- Simple, composite, and colocated entity patterns
- Reference data and complex entity examples
- CRUD operations on annotated entities

**Key Concepts**:

```java
@Table(zone = @Zone(value = "MusicStore", storageProfiles = "default"))
public class Artist {
    @Id @Column(value = "ArtistId", nullable = false)
    private Integer ArtistId;
    
    @Column(value = "Name", nullable = true, length = 120)
    private String Name;
}
```

### 2. ColocationExamples.java

**Purpose**: Data colocation strategies for performance
**Features**:

- Music catalog hierarchy: Artist → Album → Track
- Sales transaction hierarchy: Customer → Invoice → InvoiceLine
- Colocated query performance demonstration
- Best practices and validation

**Key Concepts**:

```java
@Table(
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    colocateBy = @ColumnRef("ArtistId"),  // Albums with their artist
    indexes = { @Index(value = "IFK_AlbumArtistId", columns = { @ColumnRef("ArtistId") }) }
)
public class Album {
    @Id @Column(value = "AlbumId", nullable = false)
    private Integer AlbumId;
    
    @Id @Column(value = "ArtistId", nullable = false)  // Required for colocation
    private Integer ArtistId;
}
```

### 3. SchemaEvolutionDemo.java

**Purpose**: Schema evolution and DDL generation patterns
**Features**:

- Automatic DDL generation from annotations
- Schema validation and error handling
- Entity evolution strategies (V1 → V2)
- Complex schema creation with multiple indexes

**Key Concepts**:

- Version 1: Simple schema
- Version 2: Enhanced with additional fields and indexes
- Migration strategies for production environments

## Prerequisites

1. **Running Ignite 3 Cluster**

   ```bash
   cd ../00-docker
   docker-compose up -d
   ./init-cluster.sh
   ```

2. **Sample Data Setup** (Recommended)

   ```bash
   cd ../01-sample-data-setup
   mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.setup.app.ProjectInitializationApp"
   ```

## Running the Applications

### Quick Start - All Demos

```bash
# Run all demonstrations
mvn clean compile exec:java -Dexec.mainClass="com.apache.ignite.examples.schema.AnnotatedEntitiesDemo"
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.schema.ColocationExamples"
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.schema.SchemaEvolutionDemo"
```

### Individual Applications

#### 1. Annotated Entities Demo

```bash
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.schema.AnnotatedEntitiesDemo"
```

**Expected Output**:

- Table creation demonstrations
- Simple entity CRUD operations
- Composite key examples
- Colocation demonstrations
- Reference data patterns

#### 2. Colocation Examples

```bash
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.schema.ColocationExamples"
```

**Expected Output**:

- Music catalog hierarchy setup
- Sales transaction hierarchy setup
- Colocated query performance demos
- Best practices validation

#### 3. Schema Evolution Demo

```bash
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.schema.SchemaEvolutionDemo"
```

**Expected Output**:

- DDL generation from annotations
- Schema validation results
- Entity evolution (V1 → V2)
- Complex schema creation

## Key Learning Points

### 1. Schema Design Principles

**Distribution Zones**:

```java
// Operational data - fewer replicas for write performance
@Table(zone = @Zone(value = "MusicStore", storageProfiles = "default"))

// Reference data - more replicas for read performance  
@Table(zone = @Zone(value = "MusicStoreReplicated", storageProfiles = "default"))
```

**Colocation Requirements**:

```java
// CORRECT: Colocation key is part of primary key
@Table(colocateBy = @ColumnRef("ArtistId"))
public class Album {
    @Id @Column(value = "AlbumId", nullable = false)
    private Integer AlbumId;
    
    @Id @Column(value = "ArtistId", nullable = false)  // ✓ Part of PK
    private Integer ArtistId;
}
```

### 2. Performance Optimization

**Index Strategy**:

```java
@Index(value = "IFK_AlbumArtistId", columns = { @ColumnRef("ArtistId") })     // Foreign key
@Index(value = "IDX_Customer_Email", columns = { @ColumnRef("Email") })       // Business query
@Index(value = "IDX_Track_Genre_Price", columns = {                           // Composite
    @ColumnRef("GenreId"), @ColumnRef("UnitPrice") 
})
```

**Colocation Hierarchies**:

- Artist (root) → Album (by ArtistId) → Track (by AlbumId)
- Customer (root) → Invoice (by CustomerId) → InvoiceLine (by InvoiceId)

### 3. Common Patterns

**Simple Entity**:

- Single primary key
- Basic column mapping
- Minimal indexes

**Composite Key Entity**:

- Multiple @Id fields
- Includes colocation key
- Foreign key indexes

**Reference Data**:

- High replication zone
- Simple structure
- Read-optimized

## Troubleshooting

### Common Issues

1. **Table Already Exists**
   - Expected in demo environment
   - Applications handle gracefully
   - Use SQL DROP TABLE if reset needed

2. **Colocation Key Not in Primary Key**

   ```
   Error: Colocation key must be part of primary key
   ```

   - Ensure colocation field has @Id annotation
   - Both AlbumId and ArtistId must be @Id for Album table

3. **Connection Refused**
   - Ensure Ignite cluster is running
   - Check cluster URL (default: 127.0.0.1:10800)
   - Verify cluster initialization

### Reset Environment

```bash
# Stop and restart cluster
cd ../00-docker
docker-compose down
docker-compose up -d
./init-cluster.sh

# Recreate sample data
cd ../01-sample-data-setup
mvn exec:java -Dexec.mainClass="com.apache.ignite.examples.setup.app.ProjectInitializationApp"
```

## Related Documentation

- **Module 01**: Sample Data Setup - Provides the entity models used
- **Module 02**: Getting Started - Basic Ignite 3 operations
- **Module 04**: Table API - Using annotated schemas for CRUD operations
- **Module 05**: SQL API - Querying annotated schemas

## Advanced Topics

### Schema Evolution in Production

1. Create new table version with updated schema
2. Migrate data from old to new table  
3. Update application to use new schema
4. Drop old table after successful migration

### Performance Monitoring

- Monitor query execution plans
- Validate colocation effectiveness
- Track index usage statistics
- Measure join performance improvements

### Best Practices

1. **Design colocation based on query patterns**
2. **Use appropriate zones for data access patterns**
3. **Create indexes for foreign keys and common filters**
4. **Test schema changes in development first**
5. **Document colocation strategies for team understanding**
