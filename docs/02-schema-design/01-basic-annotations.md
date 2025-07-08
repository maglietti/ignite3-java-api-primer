<!--
Licensed under Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International (CC BY-NC-SA 4.0)
SPDX-License-Identifier: CC-BY-NC-SA-4.0
For full license text, see LICENSE-CC-BY-NC-SA-4.0
-->

# Chapter 2.1: Basic Annotations and Table Creation

Your music platform launched successfully with Chapter 1's distributed foundation, but now you face the schema evolution crisis. Adding new fields like Artist biography and Album release dates requires coordinating SQL migrations across 12 cluster nodes. One missed migration file leaves Node 7 with a different schema version, causing query failures and data corruption. Your DevOps team spends hours manually synchronizing schemas between development, testing, and production environments.

Meanwhile, your application logic lives in Java classes that know nothing about the underlying table structure. When Album queries become slow, developers can't see that the performance problem stems from missing colocation between Artist and Album tables. Critical optimization decisions hide in separate SQL files, invisible to the business logic that depends on them.

## The Schema-Code Disconnect Crisis

Traditional database development creates a dangerous disconnect between your Java application code and the distributed schema that powers it.

- **Schema Fragmentation Crisis**: Your Artist entity exists as a Java class, a CREATE TABLE statement, a zone configuration XML file, colocation settings in properties files, and index definitions scattered across migration scripts. When you add Album.releaseDate, you must update six different files across three different repositories.

- **Deployment Coordination Nightmare**: Schema changes require precise coordination across multiple cluster nodes. Deploy the Java code before the schema migration, and queries fail with "column not found" errors. Deploy the schema before the Java code, and applications crash with "unknown field" exceptions. Miss one node during migration, and you get split-brain schema inconsistencies.

- **Performance Blind Spots**: Your Java developer adds Album.findByArtist() method that seems simple but unknowingly triggers expensive cross-node joins because the colocation strategy lives in a separate configuration file. Performance problems emerge in production when related data scatters across different nodes.

- **Environment Drift**: Development uses simplified single-zone configurations for fast iteration. Testing uses minimal replication for resource efficiency. Production uses complex multi-zone strategies for fault tolerance. Schema behavior differs dramatically across environments, causing bugs that only surface under production load.

- **Development Friction**: Adding a simple database field requires updating Java classes, SQL migration scripts, configuration files, and documentation. Development velocity slows as teams coordinate schema changes across multiple systems and deployment pipelines.

## How Ignite 3 Annotations Solve the Schema-Code Disconnect

Ignite 3's annotation-based schema design eliminates the disconnect between your Java application code and distributed database schema by making your Java classes the single source of truth for everything.

- **Schema-as-Code Revolution**: Your Java class becomes your schema definition, distribution strategy, performance optimization plan, and documentation all in one place. Add Album.releaseDate as a Java field with @Column annotation, and Ignite handles DDL generation, cluster deployment, and schema synchronization automatically.

- **Atomic Deployment**: Schema and code deploy together as a single unit. No coordination between SQL migrations and Java deployments. No timing issues between application updates and database changes. Compile your Java code, and your distributed schema is ready.

- **Performance Visibility**: Colocation strategies, zone configurations, and indexing decisions live directly in your Java classes where developers can see them. When you write Album.findByArtist(), the @Table(colocateBy = @ColumnRef("ArtistId")) annotation shows exactly why the query performs well.

- **Environment Consistency**: The same annotated Java classes generate identical distributed schemas across development, testing, and production. Environment drift disappears because there's only one schema definition source.

- **Developer Velocity**: Add database features by adding Java code. Annotations provide compile-time validation, IDE support, and automatic refactoring. Database schema changes follow normal Java development workflows instead of complex multi-step deployment procedures.

## Working with the Reference Application

The **`03-schema-annotations-app`** demonstrates the complete annotation-driven development workflow with realistic music store data. Run it to see how annotations transform Java classes into distributed tables:

```bash
cd ignite3-reference-apps/03-schema-annotations-app
mvn compile exec:java
```

This reference application evolves the simple Book entity from [Chapter 1.2](../01-foundation/02-getting-started.md) into a complete Artist-Album-Track hierarchy that showcases all six essential annotations working together in a production-realistic scenario.

## From Development Friction to Schema-as-Code Flow

The Book entity from [Chapter 1.2](../01-foundation/02-getting-started.md) demonstrated the fundamental shift from traditional schema management to annotation-driven development:

**Traditional Development Pain:**

```java
// Book.java - business logic
public class Book {
    private Integer id;
    private String title;
    // Business logic disconnected from schema
}
```

```sql
-- schema.sql - separate schema definition
CREATE TABLE Book (id INTEGER PRIMARY KEY, title VARCHAR(100));
CREATE ZONE BookZone WITH PARTITIONS=25, REPLICAS=1;
ALTER TABLE Book SET ZONE BookZone;
```

**Ignite 3 Annotation Solution:**

```java
// Single source of truth - schema IS code
@Table(zone = @Zone(value = "QuickStart", storageProfiles = "default"))
public class Book {
    @Id private Integer id;
    @Column(length = 100) private String title;
    // Schema definition embedded in business logic
}
```

This annotation-driven approach scales from simple entities to complex distributed systems while maintaining development velocity and operational simplicity.

### The Schema Evolution Story: Before and After Annotations

**The Traditional Multi-File Nightmare:**

Your music platform needs to add Album release dates and Artist biographies. Here's what the traditional approach requires:

```sql
-- migration_001.sql
CREATE TABLE Artist (
    ArtistId INT PRIMARY KEY, 
    Name VARCHAR(120)
);

-- migration_002.sql  
CREATE TABLE Album (
    AlbumId INT, 
    Title VARCHAR(160), 
    ArtistId INT,
    PRIMARY KEY (AlbumId, ArtistId)
) COLOCATE BY (ArtistId);

-- migration_003.sql
CREATE INDEX IFK_AlbumArtistId ON Album (ArtistId);

-- migration_004.sql (new requirement)
ALTER TABLE Album ADD COLUMN ReleaseDate DATE;
ALTER TABLE Artist ADD COLUMN Biography TEXT;
```

```java
// zone-config.java - programmatic zone configuration
ZoneDefinition musicStoreZone = ZoneDefinition.builder("MusicStore")
    .partitions(32)
    .replicas(2)
    .storageProfiles("default")
    .build();

client.catalog().createZone(musicStoreZone);
```

```java
// Artist.java - business logic separated from schema
public class Artist {
    private Integer artistId;   // Hope this matches ArtistId column
    private String name;        // Hope this matches Name column  
    private String biography;   // New field - did the migration run?
}
```

**Problems with this fragmented approach:**

- Six different files to update for simple schema changes
- No guarantee that Java fields match database columns
- Manual deployment coordination across 12 cluster nodes
- Different configurations between development, testing, and production
- Performance optimizations invisible to application developers

**Ignite 3 Unified Annotation Solution:**

```java
// Artist.java - complete schema definition in business logic
@Table(zone = @Zone(value = "MusicStore", storageProfiles = "default"))
public class Artist {
    @Id 
    @Column(value = "ArtistId", nullable = false)
    private Integer ArtistId;
    
    @Column(value = "Name", nullable = true, length = 120)
    private String Name;
    
    @Column(value = "Biography", nullable = true, length = 5000)
    private String Biography;  // Add field, annotation handles DDL
    
    // Standard constructors, getters, setters
    public Artist() {}
    
    public Artist(Integer artistId, String name, String biography) {
        this.ArtistId = artistId;
        this.Name = name;
        this.Biography = biography;
    }
    
    // Getters and setters...
}

// Album.java - colocation and indexing built into the class
@Table(
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    colocateBy = @ColumnRef("ArtistId"),  // Performance strategy visible to developers
    indexes = { @Index(value = "IDX_AlbumArtist", columns = { @ColumnRef("ArtistId") }) }
)
public class Album {
    @Id 
    @Column(value = "AlbumId", nullable = false)
    private Integer AlbumId;
    
    @Column(value = "Title", nullable = false, length = 160)
    private String Title;
    
    @Column(value = "ReleaseDate", nullable = true)
    private LocalDate ReleaseDate;  // Add field, schema updates automatically
    
    @Id  // Required for colocation - enforced by Ignite
    @Column(value = "ArtistId", nullable = false)
    private Integer ArtistId;  // Colocation key visible in code
    
    public Album() {}
    
    public Album(Integer albumId, String title, LocalDate releaseDate, Integer artistId) {
        this.AlbumId = albumId;
        this.Title = title;
        this.ReleaseDate = releaseDate;
        this.ArtistId = artistId;
    }
    
    // Getters and setters...
}
```

**Benefits of the unified approach:**

- **One Source of Truth**: Java class defines schema, colocation, indexes, and constraints
- **Automatic DDL Generation**: Add Biography field, Ignite generates and deploys ALTER TABLE
- **Visible Performance Strategy**: Developers see colocation strategy in the business logic
- **Compile-Time Validation**: IDE catches annotation errors before deployment
- **Environment Consistency**: Same classes generate identical schemas everywhere
- **Atomic Deployment**: Schema and code deploy together, eliminating timing issues

## From Java Class to Distributed Table: The Annotation Transformation Process

Understanding how Ignite transforms your annotated Java classes into production-ready distributed tables helps you design better schemas and debug issues when they arise.

### Phase 1: Annotation Processing and Validation

```java
@Table(zone = @Zone(value = "MusicStore", storageProfiles = "default"))
public class Artist {
    @Id 
    @Column(value = "ArtistId", nullable = false)
    private Integer ArtistId;  // Primary key validation
    
    @Column(value = "Name", nullable = true, length = 120)
    private String Name;       // Constraint validation
}
```

**What Ignite validates:**

- Every table must have at least one @Id field
- Column names must be valid SQL identifiers  
- Zone references must point to existing zones
- Colocation keys must be part of the primary key
- Index columns must reference actual table columns

### Phase 2: DDL Generation and Optimization

```java
// Your annotation-driven class
@Table(
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    colocateBy = @ColumnRef("ArtistId"),
    indexes = { @Index(value = "IDX_AlbumArtist", columns = { @ColumnRef("ArtistId") }) }
)
public class Album { ... }
```

**Generates optimized SQL DDL:**

```sql
-- Zone creation (if needed)
CREATE ZONE IF NOT EXISTS "MusicStore" 
WITH PARTITIONS=25, REPLICAS=1, STORAGE_PROFILES='default';

-- Table with colocation
CREATE TABLE Album (
    AlbumId INTEGER NOT NULL,
    Title VARCHAR(160) NOT NULL,
    ArtistId INTEGER NOT NULL,
    PRIMARY KEY (AlbumId, ArtistId)
) 
WITH PRIMARY_ZONE='MusicStore',
     COLOCATE_BY=(ArtistId);

-- Performance indexes
CREATE INDEX IDX_AlbumArtist ON Album (ArtistId);
```

### Phase 3: Distributed Deployment

```java
try (IgniteClient client = IgniteClient.builder()
        .addresses("node1:10800", "node2:10800", "node3:10800")
        .build()) {
    
    // Single method call orchestrates distributed deployment
    client.catalog().createTable(Artist.class);
    client.catalog().createTable(Album.class);
}
```

**Behind the scenes coordination:**

1. **DDL Generation**: Annotations convert to SQL DDL optimized for distributed execution
2. **Catalog Synchronization**: Schema metadata replicates to all cluster nodes
3. **Zone Initialization**: Distribution zones configure data placement and replication
4. **Partition Creation**: Tables split into partitions distributed across nodes
5. **Index Building**: Secondary indexes create on all relevant partitions
6. **Colocation Setup**: Related tables configure to store data on same nodes

### Phase 4: Runtime Operation Verification

```java
// Verify the transformation worked correctly
public class SchemaVerification {
    
    public void verifyDistributedSchema(IgniteClient client) {
        // Test table accessibility
        RecordView<Artist> artists = client.tables().table("Artist").recordView(Artist.class);
        RecordView<Album> albums = client.tables().table("Album").recordView(Album.class);
        
        // Verify colocation is working (should execute on same node)
        Artist testArtist = new Artist(999, "Test Artist", "Test Bio");
        Album testAlbum = new Album(999, "Test Album", LocalDate.now(), 999);
        
        artists.upsert(null, testArtist);
        albums.upsert(null, testAlbum);  // Should colocate with artist
        
        // Verify performance optimization
        Statement colocatedQuery = client.sql().statementBuilder()
            .query("SELECT a.Name, al.Title FROM Artist a JOIN Album al ON a.ArtistId = al.ArtistId WHERE a.ArtistId = ?")
            .build();
        
        // This join executes on a single node due to colocation
        var result = client.sql().execute(null, colocatedQuery, 999);
        
        System.out.println("Schema transformation successful - colocated joins working");
    }
}
```

**Runtime verification confirms:**

- Tables are accessible across all cluster nodes
- Colocation strategies place related data together
- Indexes improve query performance as expected
- Zone configurations distribute data according to specifications

This transformation process shows how Ignite bridges the gap between object-oriented Java development and distributed database operations, handling the complexity automatically while giving you full control over performance optimization.

## The Ignite Annotation Toolkit: Six Essential Annotations

Ignite 3 provides exactly six annotations that give you complete control over distributed schema design. These annotations cover every aspect of table creation, from basic structure to advanced performance optimization.

### @Table - The Foundation Annotation

Every Ignite entity starts with @Table, which transforms a Java class into a distributed table.

**Basic Usage:**

```java
@Table
public class SimpleEntity {
    @Id private Integer id;
    @Column private String name;
}
```

**Advanced Usage with All Attributes:**

```java
@Table(
    value = "custom_table_name",                     // Override table name
    schemaName = "music_schema",                     // Custom schema (default: PUBLIC)
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    colocateBy = { @ColumnRef("ArtistId") },         // Colocation strategy
    indexes = {
        @Index(value = "IDX_Artist", columns = { @ColumnRef("ArtistId") }),
        @Index(value = "IDX_Name", columns = { @ColumnRef("Name") })
    }
)
public class Album {
    @Id private Integer AlbumId;
    @Id private Integer ArtistId;  // Required for colocation
    @Column private String Name;
}
```

**@Table Attribute Reference:**

| Attribute | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `value` | String | No | Class name | Table name override |
| `schemaName` | String | No | "PUBLIC" | Database schema name |
| `zone` | @Zone | Yes | - | Distribution and storage strategy |
| `colocateBy` | @ColumnRef[] | No | {} | Column references for data colocation |
| `indexes` | @Index[] | No | {} | Secondary indexes for query performance |

### @Zone - Distribution and Storage Strategy

Defines how your data distributes across the cluster and what storage engine to use.

**Basic Zone Definition:**

```java
@Table(zone = @Zone(value = "MusicStore", storageProfiles = "default"))
public class Artist {
    @Id private Integer ArtistId;
    @Column private String Name;
}
```

**Production Zone with Full Configuration:**

```java
@Table(zone = @Zone(
    value = "MusicStoreProduction",                 // Zone name
    storageProfiles = "production_persistent",      // Storage engine profile  
    partitions = 64,                                // Data distribution
    replicas = 3,                                   // Fault tolerance
    quorumSize = 2,                                 // Minimum replicas for operations
    distributionAlgorithm = "rendezvous",           // Partition assignment algorithm
    dataNodesAutoAdjust = 300,                      // Auto-adjust timeout
    dataNodesAutoAdjustScaleUp = 100,               // Scale-up timeout  
    dataNodesAutoAdjustScaleDown = 300,             // Scale-down timeout
    filter = "$.storage == 'SSD'",                  // Node selection criteria
    consistencyMode = "STRONG_CONSISTENCY"          // Consistency guarantee
))
public class CriticalBusinessData {
    @Id private Integer id;
    @Column private String data;
}
```

**@Zone Attribute Reference:**

| Attribute | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `value` | String | Yes | - | Zone name that identifies the distribution zone |
| `storageProfiles` | String | Yes | - | Storage engine profile ("default", "persistent", etc.) |
| `partitions` | int | No | -1 (calculated) | Partition count - higher values enable more parallelism |
| `replicas` | int | No | -1 (zone default) | Replica count - higher values provide better fault tolerance |
| `quorumSize` | int | No | -1 (calculated) | Minimum replicas needed for operations |
| `distributionAlgorithm` | String | No | "" | How partitions assign to nodes |
| `dataNodesAutoAdjust` | int | No | -1 | General auto-adjustment timeout (seconds) |
| `dataNodesAutoAdjustScaleUp` | int | No | -1 | Scale-up specific timeout (seconds) |
| `dataNodesAutoAdjustScaleDown` | int | No | -1 | Scale-down specific timeout (seconds) |
| `filter` | String | No | "" | JSON filter for node selection |
| `consistencyMode` | String | No | "" | STRONG_CONSISTENCY or HIGH_AVAILABILITY |

### @Column - Field Definition and Constraints

Maps Java fields to database columns with type-specific constraints.

**Basic Column Mapping:**

```java
public class Track {
    @Id private Integer TrackId;
    @Column private String Name;  // Uses field name as column name
}
```

**Complete Column Configuration:**

```java
public class Track {
    @Id 
    @Column(value = "TrackId", nullable = false)
    private Integer TrackId;
    
    @Column(value = "Name", nullable = false, length = 200)
    private String Name;                    // VARCHAR(200) NOT NULL
    
    @Column(value = "UnitPrice", nullable = false, precision = 10, scale = 2)
    private BigDecimal UnitPrice;           // DECIMAL(10,2) NOT NULL
    
    @Column(value = "Milliseconds", nullable = true)
    private Integer Milliseconds;           // INTEGER (nullable)
    
    @Column(value = "Composer", nullable = true, length = 220)
    private String Composer;                // VARCHAR(220) (nullable)
    
    @Column(value = "ReleaseDate", nullable = true)
    private LocalDate ReleaseDate;          // DATE (nullable)
    
    @Column(
        value = "CustomData",
        columnDefinition = "VARCHAR(500) NOT NULL DEFAULT 'N/A'"
    )
    private String CustomData;              // Full SQL definition override
}
```

**@Column Attribute Reference:**

| Attribute | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `value` | String | No | Field name | Column name override |
| `nullable` | boolean | No | true | NULL constraint specification |
| `length` | int | No | -1 (unlimited) | String column length for VARCHAR |
| `precision` | int | No | -1 | Numeric precision for DECIMAL |
| `scale` | int | No | -1 | Numeric scale for DECIMAL |
| `columnDefinition` | String | No | "" | Full SQL column definition override |

### @Id - Primary Key Specification

Marks fields as components of the primary key with optional sort ordering.

**Simple Primary Key:**

```java
public class Artist {
    @Id private Integer ArtistId;           // Single-column primary key
    @Column private String Name;
}
```

**Composite Primary Key:**

```java
public class Album {
    @Id private Integer AlbumId;            // First part of composite key
    @Id private Integer ArtistId;           // Second part - enables colocation
    @Column private String Title;
}
```

**Primary Key with Sort Order:**

```java
public class PlaylistTrack {
    @Id(SortOrder.ASC) private Integer PlaylistId;    // Ascending order
    @Id(SortOrder.DESC) private Integer TrackId;      // Descending order
    @Column private Integer Position;
}
```

**@Id Attribute Reference:**

| Attribute | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `value` | SortOrder | No | DEFAULT | Primary key sort order |

**SortOrder Options:**

| Value | Description |
|-------|-------------|
| `DEFAULT` | System default ordering |
| `ASC` | Ascending order |
| `DESC` | Descending order |
| `ASC_NULLS_FIRST` | Ascending with nulls first |
| `ASC_NULLS_LAST` | Ascending with nulls last |
| `DESC_NULLS_FIRST` | Descending with nulls first |
| `DESC_NULLS_LAST` | Descending with nulls last |

### @Index - Performance Optimization

Creates secondary indexes to accelerate query performance.

**Single-Column Index:**

```java
@Table(
    indexes = { @Index(value = "IDX_ArtistName", columns = { @ColumnRef("Name") }) }
)
public class Artist {
    @Id private Integer ArtistId;
    @Column private String Name;            // Indexed for fast name searches
}
```

**Multi-Column Index with Sort Orders:**

```java
@Table(
    indexes = {
        @Index(
            value = "IDX_AlbumYear_Title",
            columns = {
                @ColumnRef(value = "ReleaseYear", sort = SortOrder.DESC),  // Recent first
                @ColumnRef(value = "Title", sort = SortOrder.ASC)         // Alphabetical
            },
            type = IndexType.SORTED                                      // B-tree index
        )
    }
)
public class Album {
    @Id private Integer AlbumId;
    @Column private String Title;
    @Column private Integer ReleaseYear;
}
```

**@Index Attribute Reference:**

| Attribute | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `value` | String | No | "" (generated) | Index name |
| `columns` | @ColumnRef[] | Yes | - | Index columns specification |
| `type` | IndexType | No | DEFAULT | Index type for optimization |

**IndexType Options:**

| Value | Description | Best For |
|-------|-------------|----------|
| `DEFAULT` | Ignite chooses optimal type | General purpose |
| `SORTED` | B-tree index | Range queries, sorting |
| `HASH` | Hash index | Equality queries only |

### @ColumnRef - Column References

References columns for colocation strategies and index definitions.

**Basic Column Reference:**

```java
@Table(
    colocateBy = { @ColumnRef("ArtistId") },         // Simple column reference
    indexes = { @Index(value = "IDX_Artist", columns = { @ColumnRef("ArtistId") }) }
)
public class Album { ... }
```

**Column Reference with Sort Order:**

```java
@Table(
    indexes = {
        @Index(
            value = "IDX_TrackLength",
            columns = { @ColumnRef(value = "Milliseconds", sort = SortOrder.DESC) }
        )
    }
)
public class Track {
    @Id private Integer TrackId;
    @Column private Integer Milliseconds;   // Longest tracks first
}
```

**Multi-Column Colocation:**

```java
@Table(
    colocateBy = {
        @ColumnRef("CustomerId"),               // Primary colocation key
        @ColumnRef("InvoiceDate")               // Secondary colocation key
    }
)
public class InvoiceLine {
    @Id private Integer InvoiceLineId;
    @Id private Integer CustomerId;
    @Id private LocalDate InvoiceDate;
    @Column private BigDecimal Amount;
}
```

**@ColumnRef Attribute Reference:**

| Attribute | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `value` | String | Yes | - | Column name for reference |
| `sort` | SortOrder | No | DEFAULT | Sort order for index columns |

These six annotations provide complete control over distributed table design, from basic structure to advanced performance optimization. Every Ignite 3 table uses these same annotations, whether simple development entities or complex production systems with millions of records.
