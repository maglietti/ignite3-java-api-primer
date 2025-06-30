# Chapter 2.2: Relationships and Colocation Strategies

Your Album queries are slow because Artist data lives on different nodes, forcing expensive network joins for simple relationship navigation. When your application asks "show me The Beatles and all their albums," Ignite executes separate network operations to fetch the artist from node 1 and albums scattered across nodes 2 and 3. Each network hop adds 10-50ms latency, turning simple queries into performance bottlenecks.

## The Distributed Join Performance Problem

Distributed databases face a fundamental challenge: related data often lives on different nodes, making relationship queries expensive.

**Network Latency Multiplication:** Each related table access requires a network hop. A simple "artist and their albums" query becomes multiple network operations, multiplying latency by the number of relationships.

**Cross-Node Join Overhead:** When related data lives on different nodes, the database must transfer data across the network to execute joins. This serialization, transfer, and deserialization overhead can make simple queries 10-100x slower than equivalent single-node operations.

**Unpredictable Performance:** Query performance depends on which nodes store your data. The same query executes fast when related data happens to be colocated, but slow when it's distributed across multiple nodes.

**Resource Waste:** Moving data between nodes for joins consumes network bandwidth and CPU cycles that could be used for serving more requests.

## How Ignite 3 Colocation Solves Join Performance Problems

Colocation annotations eliminate distributed join overhead by guaranteeing related data lives together on the same nodes.

**Predictable Performance:** Related data always lives on the same nodes, so relationship queries execute as local operations with consistent microsecond response times.

**Network Elimination:** Joins between colocated tables require no network operations. All data access happens in local memory on the same nodes.

**Resource Efficiency:** CPU and network resources focus on serving requests instead of moving data between nodes for joins.

**Transparent Operation:** Your application queries work the same way, but execute faster because the database optimizes data placement automatically.

## Implementation: Colocation Strategies

### Basic Entity Structure

Start with the partition key decision. Every table needs a primary key that determines how data distributes across nodes. ArtistId becomes your partition key - all operations using this key execute on a single node, while operations without it may require cross-node coordination.

Transform your Java class into a distributed entity:

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

**Distribution Strategy:**

**@Table(zone = @Zone("MusicStore"))** places this table in the MusicStore distribution zone with 2 replicas across cluster nodes. The zone configuration determines replication strategy and storage engine settings.

**@Id** marks ArtistId as the partition key. Ignite uses this key to determine which node stores each record. Operations that include the partition key (single-artist lookups, artist updates) execute locally on one node. Operations without it (artist name searches, full table scans) require coordination across multiple nodes.

**@Column** constraints enforce data integrity at the distributed storage level. String length limits prevent network transfer bloat when syncing data between nodes.

### Reference Data Performance Problem and Solution

**Problem:** Genre lookups slow down your track queries because genre data scatters across different nodes from track data. When you query 1000 tracks, each track's genre lookup potentially hits different nodes, multiplying network latency by the number of genres referenced.

**Solution:** Reference data benefits from higher replication. More replicas mean higher probability that any node has local copies of commonly accessed data:

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

**Zone Performance Trade-offs:**

MusicStore zone (2 replicas) optimizes for write performance. Each write operation only needs to synchronize between 2 nodes, reducing write latency and increasing write throughput for frequently changing data like customer orders and track play counts.

MusicStoreReplicated zone (3+ replicas) optimizes for read performance. Each read operation has higher probability of finding data locally, eliminating network hops for genre and media type lookups. The trade-off: writes become slightly slower due to additional replica synchronization.

```java
// Zone configuration balances read vs write performance
ZoneDefinition operationalZone = ZoneDefinition.builder("MusicStore")
    .replicas(2)        // Fast writes for transactional data
    .build();

ZoneDefinition referenceZone = ZoneDefinition.builder("MusicStoreReplicated")  
    .replicas(3)        // Fast reads for lookup data
    .build();
```

Track queries with genre lookups execute faster because genre data is more likely to be cached locally on the same nodes that store track data.

## Data Type Optimization

Data type choices impact network transfer costs and storage efficiency. Each field in your entity gets serialized for network transmission between nodes and deserialized on the receiving end. Larger data types increase serialization overhead and network bandwidth consumption.

Consider distributed implications when choosing types:

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

**Network Transfer Performance:**

**Primary Key Types:**
- **Integer**: 4 bytes, efficient hash distribution for partitioning
- **Long**: 8 bytes, necessary for tables exceeding 2 billion records
- **String**: Variable length, avoid unless natural keys exist (UUID, product codes)

**Precision Data:**
- **BigDecimal**: Exact decimal precision, use for currency to avoid floating-point rounding errors in financial calculations
- **Double**: 8 bytes, acceptable for ratings and percentages where slight precision loss is tolerable

**String Length Strategy:**
- Oversized string columns waste network bandwidth and storage
- Undersized columns cause runtime truncation exceptions
- Profile your actual data: 50 (names), 100 (titles), 255 (descriptions)

**Temporal Data:**
- **LocalDate**: Date only, no timezone complexity
- **Instant**: UTC timestamps, consistent across distributed nodes regardless of server timezones
- **LocalDateTime**: Use only when timezone context is meaningful and consistent

Integer and Long types serialize fastest. String types add variable serialization overhead. BigDecimal adds precision but costs CPU cycles for arithmetic operations during aggregations.

### Constraint Enforcement

Ignite enforces constraints at the storage engine level across all nodes. Nullable constraints prevent null pointer exceptions during serialization. Length constraints prevent oversized data from consuming excessive network bandwidth during replica synchronization.

```java
@Table(zone = @Zone(value = "MusicStore", storageProfiles = "default"))
public class Customer {
    @Id
    @Column(value = "CustomerId", nullable = false)
    private Integer CustomerId;
    
    // Length based on actual data analysis - prevents truncation
    @Column(value = "FirstName", nullable = false, length = 40)
    private String FirstName;
    
    @Column(value = "LastName", nullable = false, length = 20)
    private String LastName;
    
    // Optional business data
    @Column(value = "Company", nullable = true, length = 80)
    private String Company;
    
    // Email uniqueness enforced at application level
    @Column(value = "Email", nullable = false, length = 60)
    private String Email;
    
    // Foreign key for potential colocation strategies
    @Column(value = "SupportRepId", nullable = true)
    private Integer SupportRepId;
    
    // Precise decimal for financial calculations
    @Column(value = "CreditLimit", nullable = true, precision = 12, scale = 2)
    private BigDecimal CreditLimit;
    
    public Customer() {}
    
    // Getters and setters...
}
```

## Schema Deployment

Deploy tables in dependency order to avoid foreign key constraint violations. Reference tables first, then entities that depend on them.

```java
try (IgniteClient client = IgniteClient.builder()
        .addresses("127.0.0.1:10800")
        .build()) {
    
    // 1. Reference data - no dependencies
    client.catalog().createTable(Genre.class);
    client.catalog().createTable(MediaType.class);
    
    // 2. Root entities
    client.catalog().createTable(Artist.class);
    client.catalog().createTable(Customer.class);
    
    // 3. Dependent entities
    client.catalog().createTable(Album.class);      // References Artist
    client.catalog().createTable(Track.class);      // References Album, Genre, MediaType
}
```

Schema changes propagate to all cluster nodes through the metadata synchronization protocol. Each node updates its local schema cache and begins accepting operations on the new table structure.

### Basic Operations

Operations with partition keys execute on single nodes. Operations without partition keys may require cross-node coordination:

```java
Table artistTable = client.tables().table("Artist");
RecordView<Artist> artistView = artistTable.recordView(Artist.class);

// Single-node operation - includes partition key
Artist beatles = new Artist(1, "The Beatles");
artistView.upsert(null, beatles);

// Single-node lookup - uses partition key for routing
Artist keyOnly = new Artist();
keyOnly.setArtistId(1);
Artist retrieved = artistView.get(null, keyOnly);
```

Ignite calculates partition assignment from the primary key value. Artist ID 1 maps to a specific partition, which lives on specific nodes. The get operation routes directly to those nodes without broadcasting.

## Advanced Colocation Implementation

Now that you understand how colocation solves distributed join problems, implement parent-child colocation patterns for your entity relationships.

**Common Colocation Scenarios:**
- Artist profiles with their complete discography
- Album details with all track listings  
- Customer orders with line items

### Parent-Child Colocation Implementation

Artist serves as the colocation anchor. Its ArtistId determines which nodes store the artist data:

```java
@Table(zone = @Zone(value = "MusicStore", storageProfiles = "default"))
public class Artist {
    @Id
    @Column(value = "ArtistId", nullable = false)
    private Integer ArtistId;  // Colocation anchor
    
    @Column(value = "Name", nullable = true, length = 120)
    private String Name;
    
    // Standard constructors, getters, setters...
}
```

Album colocates with Artist using the `colocateBy` annotation:

```java
@Table(
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    colocateBy = @ColumnRef("ArtistId")  // Follow Artist partitioning
)
public class Album {
    @Id
    @Column(value = "AlbumId", nullable = false)
    private Integer AlbumId;
    
    @Column(value = "Title", nullable = false, length = 160)
    private String Title;
    
    @Id  // Must be part of primary key for colocation
    @Column(value = "ArtistId", nullable = false)
    private Integer ArtistId;
    
    // Constructors, getters, setters...
}
```

**Colocation Requirements:**

The colocation field (ArtistId) must be part of Album's composite primary key. Ignite uses the ArtistId value to determine partitioning - albums with ArtistId=123 live on the same nodes as the artist record with ArtistId=123.

### Colocation Performance Impact Measurement

**Without colocation** - network overhead multiplies:

```java
// Artist on node 1, albums scattered across nodes 2-4
Artist artist = artists.get(null, artistKey);           // Network hop 1
Collection<Album> albums = albums.getAll(null, 
    albumKeys);                                         // Network hops 2-4
// Total: 4 network operations for one business query
```

**With colocation** - operations stay local:

```java
// Artist and all albums on same node set
Artist artist = artists.get(null, artistKey);           // Local operation
Collection<Album> albums = albums.getAll(null, 
    albumKeys);                                         // Local operation
// Total: 1 local operation for same business query
```

Query time drops from 40-200ms (network latency × hops) to 1-5ms (local storage access). Colocation eliminates network serialization, transfer, and deserialization overhead for related data operations.

## Next Steps

With entity relationships and colocation strategies established, advance to comprehensive annotation patterns that handle complex distributed scenarios:

**[Chapter 2.3: Advanced Annotations and Zone Configuration](03-advanced-annotations.md)** - Multi-level colocation hierarchies, composite foreign keys, and zone-specific performance tuning for production-scale distributed data management
