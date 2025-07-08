<!--
Licensed under Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International (CC BY-NC-SA 4.0)
SPDX-License-Identifier: CC-BY-NC-SA-4.0
For full license text, see LICENSE-CC-BY-NC-SA-4.0
-->

# Chapter 2.3: Advanced Annotations and Zone Configuration

Your music platform now handles millions of tracks across multiple zones, but complex queries timeout because indexes span multiple partitions and composite keys create hotspots. Artist catalog queries that should execute in milliseconds now take seconds as data fragments across nodes. Junction tables for playlists generate excessive network traffic, and employee hierarchy queries create cascading performance problems.

Advanced annotations solve these distributed schema challenges through strategic colocation hierarchies, zone-specific optimization, and composite index patterns that transform cross-node operations into single-partition queries.

## Three-Level Colocation Hierarchy

### Album Entity: Intermediate Colocation Level

The Album entity creates the middle tier of our colocation hierarchy by declaring `ArtistId` as both foreign key and colocation anchor:

```java
@Table(
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    colocateBy = @ColumnRef("ArtistId"),
    indexes = { @Index(value = "IFK_AlbumArtistId", columns = { @ColumnRef("ArtistId") }) }
)
public class Album {
    @Id
    @Column(value = "AlbumId", nullable = false)
    private Integer AlbumId;
    
    @Id  // Required: colocation key must participate in primary key
    @Column(value = "ArtistId", nullable = false)
    private Integer ArtistId;
    
    @Column(value = "Title", nullable = false, length = 160)
    private String Title;
    
    public Album() {}
    
    public Album(Integer albumId, Integer artistId, String title) {
        this.AlbumId = albumId;
        this.ArtistId = artistId;
        this.Title = title;
    }
    
    // Getters and setters...
}
```

The `colocateBy` annotation instructs Ignite to store all albums for the same artist on identical cluster nodes as that artist's data. The `ArtistId` must be declared as `@Id` because Ignite requires colocation keys to participate in the primary key, ensuring correct data partitioning and enabling single-node operations for queries using both `AlbumId` and `ArtistId`.

This composite primary key structure means each album has a unique `AlbumId` within its artist scope, while the combination `(AlbumId, ArtistId)` remains globally unique. All albums for Artist 1 reside on the same partition as Artist 1 itself.

The performance impact becomes apparent in artist-album queries:

```java
String colocatedQuery = """
    SELECT ar.Name, al.Title 
    FROM Artist ar 
    JOIN Album al ON ar.ArtistId = al.ArtistId 
    WHERE ar.ArtistId = ?
    """;
```

This query executes on a single node with no network overhead because all related data is guaranteed to be colocated.

### Track Entity: Final Colocation Level

The Track entity completes the three-level hierarchy by colocating with `AlbumId`, creating a chain where Artist → Album → Track data all resides on identical nodes:

```java
@Table(
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    colocateBy = @ColumnRef("AlbumId"),
    indexes = {
        @Index(value = "IFK_TrackAlbumId", columns = { @ColumnRef("AlbumId") }),
        @Index(value = "IFK_TrackGenreId", columns = { @ColumnRef("GenreId") }),
        @Index(value = "IFK_TrackMediaTypeId", columns = { @ColumnRef("MediaTypeId") }),
        @Index(value = "IDX_TrackName", columns = { @ColumnRef("Name") })
    }
)
public class Track {
    @Id
    @Column(value = "TrackId", nullable = false)
    private Integer TrackId;
    
    @Id  // Colocation key must be part of primary key
    @Column(value = "AlbumId", nullable = true)
    private Integer AlbumId;
    
    @Column(value = "Name", nullable = false, length = 200)
    private String Name;
    
    @Column(value = "MediaTypeId", nullable = false)
    private Integer MediaTypeId;
    
    @Column(value = "GenreId", nullable = true)
    private Integer GenreId;
    
    @Column(value = "Composer", nullable = true, length = 220)
    private String Composer;
    
    @Column(value = "Milliseconds", nullable = false)
    private Integer Milliseconds;
    
    @Column(value = "Bytes", nullable = true)
    private Integer Bytes;
    
    @Column(value = "UnitPrice", nullable = false, precision = 10, scale = 2)
    private BigDecimal UnitPrice;
    
    public Track() {}
    
    public Track(Integer trackId, Integer albumId, String name, 
                 Integer mediaTypeId, BigDecimal unitPrice) {
        this.TrackId = trackId;
        this.AlbumId = albumId;
        this.Name = name;
        this.MediaTypeId = mediaTypeId;
        this.UnitPrice = unitPrice;
    }
    
    // Getters and setters...
}
```

The index strategy addresses specific query patterns: foreign key indexes (`IFK_TrackAlbumId`, `IFK_TrackGenreId`, `IFK_TrackMediaTypeId`) enable efficient joins and referential integrity checks, while business indexes (`IDX_TrackName`) support application features like track search functionality.

This creates a complete colocation chain where Artist data on Node A automatically colocates all related Albums and Tracks on the same node. The result transforms complex artist discography queries into single-node operations:

```java
String singleNodeQuery = """
    SELECT ar.Name as Artist, al.Title as Album, t.Name as Track, t.UnitPrice
    FROM Artist ar
    JOIN Album al ON ar.ArtistId = al.ArtistId  
    JOIN Track t ON al.AlbumId = t.AlbumId
    WHERE ar.ArtistId = ?
    ORDER BY al.Title, t.Name
    """;
```

All related data resides on the same partition, eliminating network traffic and distributed join overhead.

## Zone Configuration for Data Access Patterns

Different data access patterns require different zone configurations. Transactional data needs write optimization while reference data needs read optimization across more replicas:

```java
// Transactional data - optimized for write throughput
@Table(zone = @Zone(value = "MusicStore", storageProfiles = "default"))
public class Invoice {
    @Id
    @Column(value = "InvoiceId", nullable = false)
    private Integer InvoiceId;
    
    @Id  // Colocate with customer data
    @Column(value = "CustomerId", nullable = false)
    private Integer CustomerId;
    
    @Column(value = "InvoiceDate", nullable = false)
    private LocalDate InvoiceDate;
    
    @Column(value = "BillingAddress", nullable = true, length = 70)
    private String BillingAddress;
    
    @Column(value = "BillingCity", nullable = true, length = 40)
    private String BillingCity;
    
    @Column(value = "BillingState", nullable = true, length = 40)
    private String BillingState;
    
    @Column(value = "BillingCountry", nullable = true, length = 40)
    private String BillingCountry;
    
    @Column(value = "BillingPostalCode", nullable = true, length = 10)
    private String BillingPostalCode;
    
    @Column(value = "Total", nullable = false, precision = 10, scale = 2)
    private BigDecimal Total;
    
    public Invoice() {}
    // Getters and setters...
}

// Reference data - optimized for read performance
@Table(zone = @Zone(value = "MusicStoreReplicated", storageProfiles = "default"))
public class Genre {
    @Id
    @Column(value = "GenreId", nullable = false)
    private Integer GenreId;
    
    @Column(value = "Name", nullable = true, length = 120)
    private String Name;
    
    public Genre() {}
    // Getters and setters...
}
```

## Composite Index Strategies

Composite indexes solve performance problems in queries that filter or sort on multiple columns, preventing full table scans when single-column indexes prove insufficient:

```java
@Table(
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    indexes = {
        @Index(value = "IFK_InvoiceLineInvoiceId", columns = { @ColumnRef("InvoiceId") }),
        @Index(value = "IFK_InvoiceLineTrackId", columns = { @ColumnRef("TrackId") }),
        
        // Composite index for price/quantity queries
        @Index(
            value = "IDX_InvoiceLine_Price_Qty",
            columns = { 
                @ColumnRef("UnitPrice"), 
                @ColumnRef(value = "Quantity", sort = SortOrder.DESC) 
            }
        ),
        
        // Composite index for business constraint enforcement
        @Index(
            value = "IDX_InvoiceLine_Invoice_Track",
            columns = { @ColumnRef("InvoiceId"), @ColumnRef("TrackId") }
        )
    },
    colocateBy = @ColumnRef("InvoiceId")
)
public class InvoiceLine {
    @Id
    @Column(value = "InvoiceLineId", nullable = false)
    private Integer InvoiceLineId;
    
    @Id  // Required for colocation
    @Column(value = "InvoiceId", nullable = false)
    private Integer InvoiceId;
    
    @Column(value = "TrackId", nullable = false)
    private Integer TrackId;
    
    @Column(value = "UnitPrice", nullable = false, precision = 10, scale = 2)
    private BigDecimal UnitPrice;
    
    @Column(value = "Quantity", nullable = false)
    private Integer Quantity;
    
    public InvoiceLine() {}
    
    // Getters and setters...
}
```

The `IDX_InvoiceLine_Price_Qty` composite index optimizes queries filtering by unit price with quantity-based sorting, while `IDX_InvoiceLine_Invoice_Track` ensures unique constraint enforcement across invoice-track combinations.

## Junction Tables for Many-to-Many Relationships

Many-to-many relationships require junction tables that balance query performance with storage efficiency. The playlist-track relationship demonstrates how colocation strategy affects cross-reference query performance:

```java
@Table(
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    indexes = {
        @Index(value = "IFK_PlaylistTrackPlaylistId", columns = { @ColumnRef("PlaylistId") }),
        @Index(value = "IFK_PlaylistTrackTrackId", columns = { @ColumnRef("TrackId") })
    },
    colocateBy = @ColumnRef("PlaylistId")
)
public class PlaylistTrack {
    @Id
    @Column(value = "PlaylistId", nullable = false)
    private Integer PlaylistId;
    
    @Id
    @Column(value = "TrackId", nullable = false)
    private Integer TrackId;
    
    public PlaylistTrack() {}
    
    public PlaylistTrack(Integer playlistId, Integer trackId) {
        this.PlaylistId = playlistId;
        this.TrackId = trackId;
    }
    
    // Getters and setters...
}
```

Colocating by `PlaylistId` ensures that all tracks for a specific playlist reside on the same nodes, optimizing "get all tracks in playlist" queries while potentially creating cross-node overhead for "get all playlists containing track" queries.

## Self-Referencing Hierarchical Structures

Self-referencing entities like employee hierarchies present unique indexing challenges because hierarchical queries can create deep recursive joins that span multiple partitions:

```java
@Table(
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    indexes = {
        @Index(value = "IFK_EmployeeReportsTo", columns = { @ColumnRef("ReportsTo") }),
        @Index(value = "IDX_Employee_Email", columns = { @ColumnRef("Email") })
    }
)
public class Employee {
    @Id
    @Column(value = "EmployeeId", nullable = false)
    private Integer EmployeeId;
    
    @Column(value = "LastName", nullable = false, length = 20)
    private String LastName;
    
    @Column(value = "FirstName", nullable = false, length = 20)
    private String FirstName;
    
    @Column(value = "Title", nullable = true, length = 30)
    private String Title;
    
    @Column(value = "ReportsTo", nullable = true)
    private Integer ReportsTo;
    
    @Column(value = "BirthDate", nullable = true)
    private LocalDate BirthDate;
    
    @Column(value = "HireDate", nullable = true)
    private LocalDate HireDate;
    
    @Column(value = "Address", nullable = true, length = 70)
    private String Address;
    
    @Column(value = "City", nullable = true, length = 40)
    private String City;
    
    @Column(value = "State", nullable = true, length = 40)
    private String State;
    
    @Column(value = "Country", nullable = true, length = 40)
    private String Country;
    
    @Column(value = "PostalCode", nullable = true, length = 10)
    private String PostalCode;
    
    @Column(value = "Phone", nullable = true, length = 24)
    private String Phone;
    
    @Column(value = "Fax", nullable = true, length = 24)
    private String Fax;
    
    @Column(value = "Email", nullable = true, length = 60)
    private String Email;
    
    public Employee() {}
    
    // Getters and setters...
}
```

The `IFK_EmployeeReportsTo` index enables efficient manager-subordinate queries, while the `IDX_Employee_Email` index supports authentication and lookup operations.

## Zone Configuration for Workload Optimization

Different workload patterns require specialized zone configurations that balance replication, partitioning, and storage characteristics:

```java
// Write-heavy transactional data
ZoneDefinition operationalZone = ZoneDefinition.builder("MusicStore")
    .replicas(2)
    .partitions(32)
    .storageProfiles("default")
    .build();

// Read-heavy reference data
ZoneDefinition referenceZone = ZoneDefinition.builder("MusicStoreReplicated")
    .replicas(3)
    .partitions(16)
    .storageProfiles("default")
    .build();

// Analytics and bulk processing
ZoneDefinition analyticsZone = ZoneDefinition.builder("Analytics")
    .replicas(2)
    .partitions(64)
    .storageProfiles("bulk_storage")
    .build();

client.catalog().createZone(operationalZone);
client.catalog().createZone(referenceZone);
client.catalog().createZone(analyticsZone);
```

The operational zone uses fewer replicas to optimize write throughput, while the reference zone increases replicas for better read distribution. The analytics zone maximizes partitions for parallel processing capabilities.

## Advanced Pattern Performance Impact

These advanced annotation patterns solve specific distributed database performance problems:

**Single-Node Query Execution**: Three-level colocation hierarchies eliminate network overhead in complex joins across Artist → Album → Track relationships.

**Composite Index Optimization**: Multi-column indexes prevent full table scans in queries that filter or sort on multiple columns simultaneously.

**Zone-Specific Workloads**: Specialized replica and partition configurations optimize write throughput for transactional data while maximizing read performance for reference data.

**Junction Table Efficiency**: Strategic colocation decisions in many-to-many relationships minimize cross-node traffic for primary query patterns.

The result transforms distributed complexity into predictable single-partition operations with measurable performance improvements in complex query scenarios.

---

**Next**: [Schema Changes](04-schema-evolution.md) - Production deployment patterns, DDL generation strategies, and access pattern optimization for real applications.
