# Apache Ignite 3 Schema Annotations Application

Schema-as-code patterns using Apache Ignite 3's annotation system.

**Related Documentation**: [Schema Design Guide](../../docs/02-schema-design/01-basic-annotations.md)

## Overview

Demonstrates defining database schemas in Java code using annotations. Shows how to create tables, configure data distribution, and optimize performance through colocation strategies.

## What You'll Learn

- Table creation from annotated POJOs
- Data colocation for performance optimization
- Zone configuration for different data patterns
- Primary and composite key definitions
- Index creation and optimization
- Schema evolution strategies

## Prerequisites

- Apache Ignite 3 cluster running (see [00-docker setup](../00-docker/README.md))
- Sample data setup completed ([01-sample-data-setup](../01-sample-data-setup/))
- Java 17 or higher
- Maven 3.8+ or Gradle (via wrapper)

## Application

| Application | Description | Run Command |
|-------------|-------------|-------------|
| SchemaAPIDemo | Demonstrates Apache Ignite 3's schema annotation capabilities for mapping POJOs to distributed tables. Shows basic annotation patterns, data colocation strategies for performance optimization, and schema validation features within a music streaming platform context. | `../gradlew runSchemaAPIDemo` |

### Running the Application

From this directory, use Gradle to run:

```bash
# Run the complete schema annotations demo
../gradlew runSchemaAPIDemo

# Custom cluster address
../gradlew runSchemaAPIDemo --args="192.168.1.100:10800"
```

## Demo Components

### Main Demo (SchemaAPIDemo)

Run all schema demonstrations:

**Maven:**
```bash
mvn compile exec:java
```

**Gradle:**
```bash
../gradlew run
```

### Individual Demonstrations

> [!NOTE]
> The individual classes shown below don't have main methods. They are demonstrated through SchemaAPIDemo.

The SchemaAPIDemo demonstrates:

1. **AnnotatedEntitiesDemo**
   - Simple entities with single primary keys
   - Composite keys for relationship tables
   - Reference data with high replication
   - CRUD operations on annotated entities

2. **ColocationExamples**
   - Artist → Album → Track hierarchy colocated by ArtistId
   - Customer → Invoice → InvoiceLine hierarchy colocated by CustomerId
   - Colocated query performance benefits
   - Validation of colocation rules

3. **SchemaEvolutionDemo**
   - DDL generation from annotations
   - Schema validation
   - Entity version migration (V1 → V2)
   - Complex schema creation

## Key Annotation Patterns

### Basic Table Definition

```java
@Table(zone = @Zone(value = "MusicStore", storageProfiles = "default"))
public class Artist {
    @Id @Column(value = "ArtistId", nullable = false)
    private Integer ArtistId;
    
    @Column(value = "Name", nullable = true, length = 120)
    private String Name;
}
```

### Colocation Configuration

```java
@Table(
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    colocateBy = @ColumnRef("ArtistId"),
    indexes = { @Index(value = "IFK_AlbumArtistId", columns = { @ColumnRef("ArtistId") }) }
)
public class Album {
    @Id @Column(value = "AlbumId", nullable = false)
    private Integer AlbumId;
    
    @Id @Column(value = "ArtistId", nullable = false)
    private Integer ArtistId;
}
```

## Distribution Strategy

- **MusicStore Zone**: 2 replicas for operational data
- **MusicStoreReplicated Zone**: 3 replicas for reference data
- Colocation keeps related data on same nodes for join performance


## Common Issues

**Table already exists**: Normal in demo environment - applications handle gracefully

**Colocation key error**: Colocation field must be part of primary key (annotated with @Id)

**Connection refused**: Ensure Docker cluster is running
