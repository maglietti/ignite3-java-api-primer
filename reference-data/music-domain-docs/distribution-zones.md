# Understanding Distribution Zones in Apache Ignite 3

This document explains distribution zones in Apache Ignite 3, a critical concept for effective data management in a distributed environment.

## What Are Distribution Zones?

Distribution zones in Apache Ignite 3 define how data is distributed and replicated across the cluster. They control important aspects of data placement such as:

- **Partitioning**: How data is split across nodes
- **Replication**: How many copies of each partition exist
- **Storage profiles**: Which storage methods are used

```mermaid
flowchart TD
    A[Distribution Zone Definition] --> B[Partitioning Strategy]
    A --> C[Replication Factor]
    A --> D[Storage Profiles]
    
    B --> E[Node 1]
    B --> F[Node 2]
    B --> G[Node 3]
    
    C --> H[Data Redundancy]
    D --> I[Storage Settings]
    
    subgraph cluster[Ignite Cluster]
        E
        F
        G
    end
    
    classDef zoneClass fill:#e3f2fd,stroke:#2196f3,stroke-width:2px
    classDef nodeClass fill:#e8f5e9,stroke:#4caf50,stroke-width:2px
    
    class A,B,C,D,H,I zoneClass
    class E,F,G nodeClass
```

## Distribution Zones in the Chinook Demo

The Chinook demo defines two distribution zones:

1. **Chinook**: For primary entity tables with 2 replicas
2. **ChinookReplicated**: For reference tables with 3 replicas

These are defined in `TableUtils.java`:

```java
// Create the Chinook distribution zone with 2 replicas
ZoneDefinition zoneChinook = ZoneDefinition.builder("Chinook")
        .ifNotExists()
        .replicas(2)
        .storageProfiles("default")
        .build();

// Create the ChinookReplicated distribution zone with 3 replicas and 25 partitions
ZoneDefinition zoneChinookReplicated = ZoneDefinition.builder("ChinookReplicated")
        .ifNotExists()
        .replicas(3)
        .partitions(25)
        .storageProfiles("default")
        .build();
```

Tables are assigned to zones using the `@Zone` annotation:

```java
@Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default")
)
public class Artist {
    // Class body...
}
```

Alternatively, when using SQL-based creation with `BulkLoadApp`, zones are specified in the CREATE TABLE statement:

```sql
CREATE TABLE Artist (
    ArtistId INT PRIMARY KEY,
    Name VARCHAR
) ZONE Chinook STORAGE PROFILE 'default';
```

## Zone Configuration Options

### Replicas

The `replicas` parameter defines how many copies of each partition are stored across the cluster. For example:

- `replicas(1)`: No redundancy (data exists on only one node)
- `replicas(2)`: One backup copy exists (data exists on two nodes)
- `replicas(3)`: Two backup copies exist (data exists on three nodes)

Higher replica counts increase fault tolerance but consume more storage space.

### Partitions

The `partitions` parameter defines how many partitions the data is split into:

```java
.partitions(25)
```

More partitions allow for finer-grained distribution but increase overhead. The default is typically 256 partitions, which is suitable for most use cases.

### Storage Profiles

Storage profiles define how data is physically stored:

```java
.storageProfiles("default")
```

In Ignite 3, storage profiles define a storage engine and its configuration parameters. Currently, Ignite 3 supports these storage engines:

- Apache Ignite Page Memory (B+ tree) - available in both persistent and volatile (in-memory) variants
- RocksDB - a persistent storage based on Log-structured Merge-tree (LSM) optimized for write-heavy workloads

You can configure different storage profiles with various parameters such as checkpoint frequency, page size, and memory allocation based on your workload needs.

## Zone Assignment Strategy

### Primary Tables: Chinook Zone

Tables with complex relationships and frequent updates are assigned to the `Chinook` zone with 2 replicas:

- `Artist.java`: Music artists
- `Album.java`: Music albums
- `Track.java`: Music tracks
- `Customer.java`: Customer information
- `Invoice.java`: Billing information
- `InvoiceLine.java`: Invoice details
- `Employee.java`: Employee information
- `Playlist.java`: Music playlists
- `PlaylistTrack.java`: Playlist-track associations

```java
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default")
)
public class Artist {
    // Class body...
}
```

### Reference Tables: ChinookReplicated Zone

Tables that are mostly read-only and used for lookups are assigned to the `ChinookReplicated` zone with 3 replicas:

- `Genre.java`: Music genres
- `MediaType.java`: Media format types

```java
@Table(
    zone = @Zone(value = "ChinookReplicated", storageProfiles = "default")
)
public class Genre {
    // Class body...
}
```

## Data Placement Visualization

When data is inserted, it's distributed according to the zone configuration:

```mermaid
graph TD
    A[Insert Data] --> B{Hash Function}
    B --> C[Partition 1]
    B --> D[Partition 2]
    B --> E[Partition 3]
    
    subgraph "Node 1"
        C
        D2[Partition 2 Replica]
        E2[Partition 3 Replica]
    end
    
    subgraph "Node 2"
        C2[Partition 1 Replica]
        D
        E3[Partition 3 Replica 2]
    end
    
    subgraph "Node 3"
        C3[Partition 1 Replica 2]
        D3[Partition 2 Replica 2]
        E
    end
    
    classDef primary fill:#e3f2fd,stroke:#2196f3,stroke-width:2px
    classDef replica fill:#e8f5e9,stroke:#4caf50,stroke-width:2px
    
    class C,D,E primary
    class C2,C3,D2,D3,E2,E3 replica
```

## Co-location and Zones

Co-location works within a distribution zone, ensuring that related data ends up on the same node:

```java
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default"),
    colocateBy = @ColumnRef("ArtistId")
)
public class Album {
    // Class body...
}
```

This ensures that when you join `Artist` and `Album` tables, the data is already located on the same node, reducing network transfers.

### Rebalancing When Nodes Join or Leave

When a node joins or leaves the cluster, Ignite automatically rebalances data according to the zone configuration:

1. **Node Joining**: New partitions are created on the new node, and data is copied from existing replicas.
2. **Node Leaving**: Partition replicas on the leaving node are redistributed to other nodes.

The rebalancing process happens in the background and is designed to minimize disruption to ongoing operations. During rebalancing:

- Primary partitions continue serving read and write operations
- Rebalanced partitions become available as soon as they are created
- The cluster maintains the specified replica count throughout the process

## Creating Zones Programmatically

The demo creates zones programmatically in `TableUtils.java`:

```java
public static boolean createDistributionZones(IgniteClient client) {
    try {
        System.out.println("\n=== Creating Distribution Zones");

        // Create the Chinook distribution zone with 2 replicas
        ZoneDefinition zoneChinook = ZoneDefinition.builder("Chinook")
                .ifNotExists()
                .replicas(2)
                .storageProfiles("default")
                .build();
        System.out.println("--- Creating Distribution Zone: " + zoneChinook);
        client.catalog().createZone(zoneChinook);

        // Create the ChinookReplicated distribution zone with 3 replicas and 25 partitions
        ZoneDefinition zoneChinookReplicated = ZoneDefinition.builder("ChinookReplicated")
                .ifNotExists()
                .replicas(3)
                .partitions(25)
                .storageProfiles("default")
                .build();
        System.out.println("--- Creating Distribution Zone: " + zoneChinookReplicated);
        client.catalog().createZone(zoneChinookReplicated);

        System.out.println("=== Distribution zones created successfully");
        return true;
    } catch (Exception e) {
        System.err.println("Error creating distribution zones: " + e.getMessage());
        return false;
    }
}
```

Alternatively, zones can be created using SQL in the `BulkLoadApp`:

```sql
CREATE ZONE IF NOT EXISTS Chinook 
WITH STORAGE_PROFILES='default', REPLICAS=2;

CREATE ZONE IF NOT EXISTS ChinookReplicated 
WITH STORAGE_PROFILES='default', REPLICAS=3, PARTITIONS=25;
```

## Monitoring Zone Usage

You can monitor zone usage using Ignite's monitoring tools and SQL queries:

```sql
-- List all zones
SELECT * FROM system.zones;

-- Count partitions per zone
SELECT zone_name, COUNT(*) as partition_count 
FROM system.local_partition_states 
GROUP BY zone_name;

-- Check table assignments
select * from system.local_partition_states;
```

This information can help you:

- Verify zone configurations
- Monitor data distribution
- Track partition counts
- Plan capacity

## Zone Considerations and Best Practices

### Performance Considerations

- **Read-Heavy Workloads**: Use more replicas to distribute read operations
- **Write-Heavy Workloads**: Fewer replicas reduce write overhead
- **Balanced Workloads**: 2 replicas is often a good compromise

### Data Size Considerations

- **Small Reference Data**: Can use more replicas (like `ChinookReplicated`)
- **Large Data Sets**: Consider fewer replicas to reduce storage costs

### Availability Considerations

- **Critical Data**: Use more replicas (3+) for higher availability
- **Transient Data**: Fewer replicas may be acceptable

### Zone Evolution

As your application evolves, you may need to adjust zones:

- Start with 2 replicas for most zones
- Monitor performance and adjust as needed
- Consider creating specialized zones for specific workloads

### Workload-Specific Zone Examples

| Workload Type | Recommended Configuration | Rationale |
|---------------|---------------------------|-----------|
| Reference Data | 3+ replicas, smaller partition count | High availability, fast reads |
| Transactional Data | 2 replicas, larger partition count | Balance between writes and fault tolerance |
| Time-Series Data | 1-2 replicas, time-based partitioning | Optimize for sequential writes |
| Archive Data | 1 replica with persistence | Low access frequency, storage efficiency |
