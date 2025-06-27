# Chapter 2.2: Relationships and Colocation Strategies

## Learning Objectives

By completing this chapter, you will:

- Build your first distributed table step-by-step using practical examples
- Understand how to optimize table design for different access patterns
- Apply proper data type selection for distributed environments
- Implement basic parent-child relationships with colocation

## Building Your First Distributed Table

### The Foundation: Simple Entity Pattern

Every distributed application starts with simple entities. In Ignite 3, these become distributed tables automatically. Let's build an Artist entity step by step to understand each decision and its impact.

#### Step 1: Basic Entity Structure

Start with a simple Java class that represents a music artist:

```java
public class Artist {
    private Integer ArtistId;
    private String Name;
    
    // We'll add annotations next...
}
```

#### Step 2: Making It Distributed with Annotations

Now we transform this into a distributed table by adding Ignite 3 annotations:

```java
@Table(zone = @Zone(value = "MusicStore", storageProfiles = "default"))
public class Artist {
    @Id
    @Column(value = "ArtistId", nullable = false)
    private Integer ArtistId;
    
    @Column(value = "Name", nullable = true, length = 120)
    private String Name;
    
    // Ignite requires a default constructor for object creation
    public Artist() {}
    
    // Convenience constructor for application use
    public Artist(Integer artistId, String name) {
        this.ArtistId = artistId;
        this.Name = name;
    }
    
    // Standard getters and setters
    public Integer getArtistId() { return ArtistId; }
    public void setArtistId(Integer artistId) { this.ArtistId = artistId; }
    public String getName() { return Name; }
    public void setName(String name) { this.Name = name; }
}
```

**Understanding Each Annotation Choice:**

**@Table** - This declares the class as a distributed table:

- `zone = @Zone(value = "MusicStore")` - Places this table in the "MusicStore" distribution zone
- `storageProfiles = "default"` - Uses the default storage engine configuration
- **Impact**: Creates a table distributed across cluster nodes with 2 replicas (default)

**@Id** - Marks the primary key field:

- Essential for Ignite to identify unique records
- Determines how data is partitioned across nodes
- **Impact**: ArtistId becomes the partition key - all operations using this key are single-node

**@Column** - Controls column properties:

- `value = "ArtistId"` - Sets the SQL column name (could differ from Java field name)
- `nullable = false` - Creates NOT NULL constraint
- `length = 120` - Sets VARCHAR length for string columns
- **Impact**: Enforces data constraints at the database level

### Optimizing for Different Access Patterns

#### Reference Data: When Reads Outweigh Writes

Some data in your application changes rarely but gets read frequently. Music genres and media types are perfect examples - once created, they're mostly read-only but accessed by many queries.

For this pattern, use a different distribution zone optimized for read performance:

```java
@Table(zone = @Zone(value = "MusicStoreReplicated", storageProfiles = "default"))
public class Genre {
    @Id
    @Column(value = "GenreId", nullable = false)
    private Integer GenreId;
    
    @Column(value = "Name", nullable = true, length = 120)
    private String Name;
    
    public Genre() {}
    
    public Genre(Integer genreId, String name) {
        this.GenreId = genreId;
        this.Name = name;
    }
    
    // Getters and setters...
}
```

**Why a Different Zone Strategy?**

**"MusicStore" zone (operational data):**

- 2 replicas (default)
- Optimized for write performance
- Used for frequently changing data (Artists, Albums, Customer orders)

**"MusicStoreReplicated" zone (reference data):**

- 3+ replicas (configured separately)
- Optimized for read performance
- Used for lookup tables (Genres, MediaTypes, Countries)

**Performance Benefits:**

- **More Local Reads**: With more replicas, data is more likely to be local to any node
- **Better Query Performance**: Joins with reference data execute faster
- **Reduced Network Traffic**: Queries can satisfy reference lookups locally

```java
// When you configure zones, you might do:
ZoneDefinition operationalZone = ZoneDefinition.builder("MusicStore")
    .replicas(2)        // Fewer replicas = faster writes
    .build();

ZoneDefinition referenceZone = ZoneDefinition.builder("MusicStoreReplicated")
    .replicas(3)        // More replicas = faster reads
    .build();
```

### Understanding Data Types in Distributed Systems

When your data lives across multiple nodes, data type choices affect serialization, network transfer, and storage efficiency. Ignite 3 provides intelligent mapping between Java types and SQL types, but understanding these mappings helps you make optimal choices.

#### Practical Data Type Examples

Let's see how different Java types map to SQL and understand the implications for distributed storage:

```java
@Table(zone = @Zone(value = "MusicStore", storageProfiles = "default"))
public class Track {
    @Id
    @Column(value = "TrackId", nullable = false)
    private Integer TrackId;               // SQL: INTEGER - 4 bytes, good for IDs
    
    @Column(value = "Name", nullable = false, length = 200)
    private String Name;                   // SQL: VARCHAR(200) - variable length, UTF-8
    
    @Column(value = "IsExplicit", nullable = false)
    private Boolean IsExplicit;            // SQL: BOOLEAN - 1 byte, true/false
    
    @Column(value = "UnitPrice", nullable = false, precision = 10, scale = 2)
    private BigDecimal UnitPrice;          // SQL: DECIMAL(10,2) - exact precision for money
    
    @Column(value = "ReleaseDate", nullable = false)
    private LocalDate ReleaseDate;         // SQL: DATE - date only, no time
    
    @Column(value = "LastPlayed", nullable = true)
    private Instant LastPlayed;            // SQL: TIMESTAMP WITH LOCAL TIME ZONE - global timestamp
    
    @Column(value = "FileSizeBytes", nullable = true)
    private Long FileSizeBytes;            // SQL: BIGINT - 8 bytes, for large numbers
    
    @Column(value = "UserRating", nullable = true)
    private Double UserRating;             // SQL: DOUBLE - floating point for ratings
    
    // Constructors, getters, setters...
}
```

**Type Selection Guidelines:**

**For Primary Keys:**

- **Integer**: Best for most ID fields (4 bytes, good distribution)
- **Long**: When you need more than 2 billion records
- **String**: Only when you have natural string keys (like product codes)

**For Money/Precision:**

- **BigDecimal**: Always use for currency and financial calculations
- **Double/Float**: Only for approximate values (ratings, percentages)

**For Strings:**

- **length** parameter is critical: `@Column(length = 200)`
- Too small = data truncation errors
- Too large = wasted storage space
- Common sizes: 50 (names), 100 (titles), 255 (general text), 500+ (descriptions)

**For Dates/Times:**

- **LocalDate**: When you only need the date (birthdays, release dates)
- **LocalDateTime**: For local events tied to specific timezone contexts (scheduled events, New Year at midnight)
- **Instant**: For precision timestamps and global events (created_at, updated_at, audit logs)

**Network and Storage Impact:**

- Smaller types = faster serialization and network transfer
- Fixed-length types (Integer, Long) = predictable storage
- Variable-length types (String) = efficient but unpredictable storage

### Column Constraint Specifications

```java
@Table(zone = @Zone(value = "MusicStore", storageProfiles = "default"))
public class Customer {
    @Id
    @Column(value = "CustomerId", nullable = false)
    private Integer CustomerId;
    
    // Required field with length constraint
    @Column(value = "FirstName", nullable = false, length = 40)
    private String FirstName;
    
    // Required field with specific length
    @Column(value = "LastName", nullable = false, length = 20)
    private String LastName;
    
    // Optional field with length constraint
    @Column(value = "Company", nullable = true, length = 80)
    private String Company;
    
    // Required field with unique business constraint (enforced by application)
    @Column(value = "Email", nullable = false, length = 60)
    private String Email;
    
    // Optional foreign key reference
    @Column(value = "SupportRepId", nullable = true)
    private Integer SupportRepId;
    
    // Financial data with precision constraints
    @Column(value = "CreditLimit", nullable = true, precision = 12, scale = 2)
    private BigDecimal CreditLimit;
    
    public Customer() {}
    
    // Getters and setters...
}
```

### From Annotations to Working Tables

Once you've defined your entities with annotations, creating and using them follows a simple pattern. Let's walk through the complete lifecycle:

#### Step 1: Create the Distributed Schema

```java
try (IgniteClient client = IgniteClient.builder()
        .addresses("127.0.0.1:10800")
        .build()) {
    
    // Create tables in logical order (independent entities first)
    
    // 1. Reference data (no dependencies)
    client.catalog().createTable(Genre.class);
    client.catalog().createTable(MediaType.class);
    
    // 2. Root entities (independent)
    client.catalog().createTable(Artist.class);
    client.catalog().createTable(Customer.class);
    
    // 3. Dependent entities (reference others)
    client.catalog().createTable(Album.class);      // References Artist
    client.catalog().createTable(Track.class);      // References Album, Genre, MediaType
    
    System.out.println("✓ All tables created successfully across the cluster");
}
```

**What Just Happened:**

- **Cluster Coordination**: Schema changes propagated to all nodes automatically
- **DDL Generation**: Each class converted to optimized SQL DDL
- **Zone Assignment**: Tables placed in appropriate distribution zones
- **Index Creation**: Foreign key and performance indexes created
- **Storage Setup**: Storage engines configured according to profiles

#### Step 2: Start Using Your Distributed Tables

```java
// Get a view of your distributed Artist table
Table artistTable = client.tables().table("Artist");
RecordView<Artist> artistView = artistTable.recordView(Artist.class);

// Insert data - automatically distributed across nodes
Artist beatles = new Artist(1, "The Beatles");
artistView.upsert(null, beatles);  // 'null' = no explicit transaction

// The data is now stored on one or more cluster nodes based on partition key
System.out.println("✓ Artist stored and replicated across cluster");

// Retrieve data - Ignite routes to the correct node automatically
Artist keyOnly = new Artist();
keyOnly.setArtistId(1);  // Only primary key needed for lookup
Artist retrieved = artistView.get(null, keyOnly);

System.out.println("Retrieved: " + retrieved.getName());
// Output: Retrieved: The Beatles
```

**Key Points About Distributed Operations:**

- **Automatic Routing**: You don't specify which node to query - Ignite routes based on the partition key (ArtistId)
- **Transparent Replication**: Your data exists on multiple nodes for fault tolerance, but you work with it as a single logical table
- **Consistency Guarantees**: When you retrieve data, you get the latest committed version across all replicas
- **Performance**: Since Artist 1 data all lives on the same partition, this lookup is a single-node operation

## The Power of Composite Keys and Colocation

Now that you understand basic tables, let's explore the features that make Ignite 3 exceptionally fast for complex applications. The key insight: **related data should live together**.

In traditional databases, joins often require network round trips between different servers. In Ignite 3, you can guarantee that related data lives on the same cluster nodes through **colocation**.

### The Business Case: Music Store Relationships

Consider how data flows in a music store:

- An **Artist** creates multiple **Albums**
- Each **Album** contains many **Tracks**
- When users browse, they often want Artist → Album → Track information together

By colocating this data, we can serve complete artist discographies from a single node, eliminating network overhead.

### Implementing Parent-Child Colocation

Let's start with the parent entity - Artist remains simple with a single primary key:

```java
// Parent table - the "root" of our colocation hierarchy
@Table(zone = @Zone(value = "MusicStore", storageProfiles = "default"))
public class Artist {
    @Id
    @Column(value = "ArtistId", nullable = false)
    private Integer ArtistId;  // This becomes our "colocation anchor"
    
    @Column(value = "Name", nullable = true, length = 120)
    private String Name;
    
    // Standard constructors, getters, setters...
}
```

Now, create the child entity (Album) that colocates with Artist:

```java
// Child table - colocated with Artist for performance
@Table(
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    colocateBy = @ColumnRef("ArtistId")  // This is the magic line
)
public class Album {
    @Id
    @Column(value = "AlbumId", nullable = false)
    private Integer AlbumId;
    
    @Column(value = "Title", nullable = false, length = 160)
    private String Title;
    
    @Id  // Part of composite primary key AND the colocation key
    @Column(value = "ArtistId", nullable = false)
    private Integer ArtistId;
    
    // Constructors, getters, setters...
}
```

**Critical Points for Colocation:**

1. **colocateBy = @ColumnRef("ArtistId")** - This tells Ignite to place Album records on the same nodes as Artist records with matching ArtistId
2. **@Id on ArtistId** - The colocation field must be part of the primary key
3. **Composite Primary Key** - Album has a composite key: (AlbumId, ArtistId)

### How Colocation Transforms Performance

**Without Colocation** (traditional approach):

```java
// These operations might hit different nodes
Artist artist = artists.get(null, artistKey);           // Node 1
Collection<Album> albums = albums.getAll(null, 
    artist.getAlbumIds());                              // Nodes 1, 2, 3
```

Network overhead for cross-node operations reduces performance.

**With Colocation** (Ignite 3 approach):

```java
// These operations all execute on the same node(s)
Artist artist = artists.get(null, artistKey);           // Node 1
Collection<Album> albums = albums.getAll(null, 
    artist.getAlbumIds());                              // Node 1 (same node!)
```

All data for artist 123 lives together, enabling lightning-fast joins and aggregations.

## Next Steps

Understanding entity relationships and basic colocation strategies prepares you for advanced patterns and zone configuration:

- **[Chapter 2.3: Advanced Annotations and Zone Configuration](03-advanced-annotations.md)** - Master complex colocation hierarchies, composite keys, and multi-zone architectures

- **[Chapter 2.4: Schema Deployment and Access Patterns](04-schema-evolution.md)** - Learn production patterns for DDL generation, access pattern optimization, and schema management
