# Chapter 2.1: Basic Annotations and Table Creation

Your Artist data is now distributed across the cluster, but when users browse Albums by artist, the queries hit different nodes to join related data. Response times spike under load because the database can't execute efficient joins when Artist and Album records are scattered across separate partitions.

This is where Ignite 3's annotation-based schema design transforms your distributed architecture. Instead of managing separate DDL scripts and hoping for optimal data placement, annotations embed your performance strategy directly into your Java classes.

## Working with the Reference Application

The **`03-schema-annotations-app`** demonstrates how annotations solve schema design challenges with the music store dataset. Run it to see colocation strategies and distribution zones in action:

```bash
cd ignite3-reference-apps/03-schema-annotations-app
mvn compile exec:java
```

The reference app shows how the Book example from [Chapter 1.2](../01-foundation/02-getting-started.md) scales to Artist-Album-Track hierarchies with colocation and distribution zones.

## How Schema-as-Code Eliminates Distribution Complexity

Your Book entity from [Chapter 1.2](../01-foundation/02-getting-started.md) demonstrated the fundamental pattern:

```java
@Table(zone = @Zone(value = "QuickStart", storageProfiles = "default"))
public class Book {
    @Id private Integer id;
    @Column(length = 100) private String title;
}
```

This worked for a single table. But when your application grows to handle Artist → Album → Track relationships, the challenges multiply:

- **Data Fragmentation**: Related entities scatter across different nodes
- **Join Performance**: Queries span multiple partitions for simple operations
- **Schema Consistency**: DDL scripts become environment-specific nightmares
- **Deployment Friction**: Schema changes require coordinated database migrations

## How Annotations Solve Distributed Schema Problems

Traditional database development forces you to manage schemas across multiple locations. You write SQL DDL scripts, maintain migration files, and coordinate schema changes between environments. In distributed databases, this complexity explodes.

### The Traditional Fragmented Approach

Your schema information lives in multiple disconnected files:

```sql
-- In schema.sql
CREATE TABLE Artist (ArtistId INT PRIMARY KEY, Name VARCHAR(120));
CREATE TABLE Album (AlbumId INT, Title VARCHAR(160), ArtistId INT, 
                   PRIMARY KEY (AlbumId, ArtistId)) COLOCATE BY (ArtistId);
CREATE INDEX IFK_AlbumArtistId ON Album (ArtistId);

-- Configuration in separate files
-- Distribution zones in XML or properties
-- Indexes defined separately
-- Colocation strategies documented elsewhere
```

Problems with this approach:
- Schema scattered across multiple files and systems
- No compile-time validation of relationships
- Manual synchronization between environments
- Performance optimizations hidden in separate configuration

### How Ignite 3 Unifies Schema Definition

Annotations consolidate everything into your Java classes:

```java
// Complete schema definition in one place
@Table(zone = @Zone(value = "MusicStore", storageProfiles = "default"))
public class Artist {
    @Id @Column(value = "ArtistId", nullable = false)
    private Integer ArtistId;
    
    @Column(value = "Name", nullable = true, length = 120)
    private String Name;
    // Constructors, getters, setters...
}

@Table(
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    colocateBy = @ColumnRef("ArtistId"),  // Performance optimization built-in
    indexes = { @Index(value = "IFK_AlbumArtistId", columns = { @ColumnRef("ArtistId") }) }
)
public class Album {
    @Id @Column(value = "AlbumId", nullable = false)
    private Integer AlbumId;
    
    @Column(value = "Title", nullable = false, length = 160)
    private String Title;
    
    @Id @Column(value = "ArtistId", nullable = false)  // Enables colocation
    private Integer ArtistId;
}
```

This approach provides:
- **Single Source of Truth**: Your Java class IS your schema
- **Runtime DDL Generation**: Ignite converts annotations to optimized SQL DDL
- **Performance by Design**: Colocation and indexing strategies are explicit
- **Environment Consistency**: Same schema definitions work everywhere
- **Development Workflow**: Schema changes follow normal code development process

## How Annotations Transform Into Distributed Tables

Understanding how your annotated classes become distributed tables helps you make better design decisions. The transformation follows this path:

**1. Schema Definition Phase**

```java
@Table(zone = @Zone(value = "MusicStore", storageProfiles = "default"))
public class Artist {
    @Id @Column(value = "ArtistId", nullable = false)
    private Integer ArtistId;  // Primary key definition
    
    @Column(value = "Name", nullable = true, length = 120)
    private String Name;       // String with length constraint
}
```

Annotations define schema structure with distributed-specific optimizations embedded directly in the class definition.

**2. Table Creation Phase**

```java
try (IgniteClient client = IgniteClient.builder()
        .addresses("127.0.0.1:10800")
        .build()) {
    
    // Single call creates complete distributed table
    client.catalog().createTable(Artist.class);
    // Behind the scenes: DDL generation, zone setup, distribution
}
```

**3. Cluster Distribution Phase**

Ignite handles the distributed complexity automatically:
- **DDL Generation**: Converts annotations to optimized SQL DDL
- **Catalog Synchronization**: Registers schema across all cluster nodes
- **Zone Configuration**: Applies data placement and replication strategies
- **Index Creation**: Builds secondary indexes according to specifications
- **Colocation Setup**: Configures performance optimizations for joins

**4. Production Operation**

Once created, tables operate transparently across the cluster. Your application code doesn't need to know which node stores which data—Ignite handles routing, consistency, and performance automatically.

## The Six Essential Annotations

Ignite 3 provides six annotations that give you complete control over distributed schema design:

### @Table - Primary Table Configuration

Marks a Java class as an Ignite table with distribution strategy:

- **Required**: Yes (on every entity class)
- **Key Attributes**:
  - `value` (optional): Table name override (defaults to class name)
  - `zone`: Distribution zone via `@Zone` annotation
  - `colocateBy` (optional): Colocation strategy via `@ColumnRef`
  - `indexes` (optional): Array of `@Index` definitions

### @Zone - Distribution Zone Definition  

Defines how data is distributed and replicated across the cluster:

- **Required**: Yes (within @Table)
- **Key Attributes**:
  - `value`: Zone name (e.g., "MusicStore", "MusicStoreReplicated")
  - `storageProfiles`: Storage engine configuration (typically "default")
  - `partitions` (optional): Number of partitions for data distribution
  - `replicas` (optional): Number of replicas for fault tolerance

### @Column - Field-to-Column Mapping

Maps Java fields to table columns with constraints:

- **Required**: Optional (defaults to field name)
- **Key Attributes**:
  - `value`: Column name (defaults to field name if not specified)
  - `nullable`: NULL constraint specification (default: true)
  - `length`: String column length limits (for VARCHAR columns)
  - `precision`: Numeric precision (for DECIMAL columns)
  - `scale`: Numeric scale (for DECIMAL columns)

### @Id - Primary Key Designation

Marks fields as components of the primary key:

- **Required**: Yes (at least one field per table)
- **Key Constraint**: For colocation, colocation key must be marked with @Id
- **No Attributes**: Simply marks the field as part of primary key

### @ColumnRef - Column Reference for Relationships

References columns for colocation and indexing:

- **Required**: When using colocation or indexes
- **Key Attributes**:
  - `value`: Referenced column name
  - `sort` (optional): Sort order for indexes (ASC, DESC)

### @Index - Secondary Index Definition

Creates database indexes for query performance:

- **Required**: Optional (used within @Table indexes array)
- **Key Attributes**:
  - `value`: Index name
  - `columns`: Array of `@ColumnRef` for indexed columns
  - `unique` (optional): Uniqueness constraint

## Building Relationships and Performance Strategies

Now that you understand how annotations solve schema distribution problems, you're ready to tackle entity relationships and performance optimization. The next chapter shows how to design Artist → Album → Track hierarchies that keep related data colocated for optimal join performance.

**Continue to [Chapter 2.2: Relationships and Colocation Strategies](02-relationships-and-colocation.md)** to build distributed table relationships that perform under load.