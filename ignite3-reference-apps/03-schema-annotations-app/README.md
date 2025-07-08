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
- Maven 3.8+

## Applications

### 1. AnnotatedEntitiesDemo

Demonstrates annotation patterns across different entity types:

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.schema.AnnotatedEntitiesDemo"
```

**Key concepts:**

- Simple entities with single primary keys
- Composite keys for relationship tables
- Reference data with high replication
- CRUD operations on annotated entities

### 2. ColocationExamples

Shows data colocation strategies for distributed performance:

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.schema.ColocationExamples"
```

**Key concepts:**

- Artist → Album → Track hierarchy colocated by ArtistId
- Customer → Invoice → InvoiceLine hierarchy colocated by CustomerId
- Colocated query performance benefits
- Validation of colocation rules

### 3. SchemaEvolutionDemo

Demonstrates schema changes and DDL generation:

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.schema.SchemaEvolutionDemo"
```

**Key concepts:**

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

## Running the Examples

Start with the annotated entities demo:

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.schema.AnnotatedEntitiesDemo"
```

Then explore colocation patterns:

```bash
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.schema.ColocationExamples"
```

## Common Issues

**Table already exists**: Normal in demo environment - applications handle gracefully

**Colocation key error**: Colocation field must be part of primary key (annotated with @Id)

**Connection refused**: Ensure Docker cluster is running
