# Table API - Apache Ignite 3 Reference

**Object-oriented data access patterns**

📖 **Related Documentation**: [Table API - Object-Oriented Data Access](../../docs/04-table-api-object-oriented-data-access.md)

## Overview

Learn Ignite 3's Table API for type-safe, object-oriented data operations. Work with POJOs, handle async operations, and optimize performance with bulk operations.

## What You'll Learn

- **RecordView**: Full-record operations with POJOs
- **KeyValueView**: Separate key and value handling
- **Async Operations**: Non-blocking data access patterns
- **Bulk Operations**: Efficient batch processing
- **POJO Mapping**: Automatic serialization/deserialization
- **Performance Optimization**: Best practices for table operations

## Prerequisites

- **⚠️ Required**: Apache Ignite 3 cluster running (see [00-docker setup](../00-docker/README.md))
- **Required**: Complete [01-sample-data-setup](../01-sample-data-setup/) to have tables and data available.

> **Cluster Requirement**: The 3-node Ignite cluster from `00-docker` must be running before executing this application.

## Coming Soon

This reference application is in development. It will demonstrate:

### RecordView Operations
```java
// Get table and create record view
Table artistTable = client.tables().table("Artist");
RecordView<Artist> artists = artistTable.recordView(Artist.class);

// CRUD operations
Artist artist = new Artist(1, "AC/DC");
artists.upsert(null, artist);
Artist retrieved = artists.get(null, artist);
```

### KeyValueView Operations  
```java
// Separate key/value operations
KeyValueView<Integer, Artist> artistsKV = artistTable.keyValueView(Integer.class, Artist.class);
artistsKV.put(null, 1, new Artist(1, "AC/DC"));
Artist artist = artistsKV.get(null, 1);
```

### Async Patterns
```java
// Non-blocking operations
CompletableFuture<Artist> future = artists.getAsync(null, artistKey);
future.thenAccept(artist -> System.out.println("Found: " + artist.getName()));
```

### Bulk Operations
```java
// Efficient batch processing
Map<Artist, Artist> batch = new HashMap<>();
batch.put(artist1, artist1);
batch.put(artist2, artist2);
artists.upsertAll(null, batch.entrySet());
```

### Working with Music Store Data
- Artist and Album management
- Track metadata operations
- Customer and Invoice handling
- Playlist management

## Development Status

🚧 **In Development** - This module will be implemented as part of Phase 2B of the reference applications project.

## Related Modules

- **Prerequisites**: [sample-data-setup](../sample-data-setup/) - Create tables and data
- **Foundation**: [schema-annotations-app](../schema-annotations-app/) - Understand table definitions
- **Next Steps**: [sql-api-app](../sql-api-app/) - Relational data access
- **See Also**: [transactions-app](../transactions-app/) - Transactional table operations