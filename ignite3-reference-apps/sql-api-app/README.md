# SQL API - Apache Ignite 3 Reference

**Relational data access and complex queries**

📖 **Related Documentation**: [SQL API - Relational Data Access](../../docs/05-sql-api-relational-data-access.md)

## Overview

Master Ignite 3's SQL capabilities for complex queries, joins, and analytics. Learn DDL/DML operations, parameterized queries, and POJO result mapping.

## What You'll Learn

- **DDL Operations**: Create, alter, and drop tables via SQL
- **DML Operations**: Insert, update, delete with SQL
- **Complex Queries**: Joins, aggregations, and analytics
- **Parameterized Queries**: Safe, efficient query patterns
- **Result Mapping**: Convert SQL results to POJOs
- **Query Optimization**: Performance tuning for distributed queries

## Prerequisites

**Required**: Complete [sample-data-setup](../sample-data-setup/) to have the music store schema and data.

## Coming Soon

This reference application is in development. It will demonstrate:

### DDL Operations
```sql
-- Create tables via SQL
CREATE TABLE Artist (
    ArtistId INTEGER PRIMARY KEY,
    Name VARCHAR(120) NOT NULL
) WITH PRIMARY_ZONE='MusicStore';

-- Create indexes
CREATE INDEX idx_album_artist ON Album (ArtistId);
```

### DML Operations
```java
// Parameterized inserts
client.sql().execute(null, 
    "INSERT INTO Artist (ArtistId, Name) VALUES (?, ?)", 
    1, "AC/DC");

// Batch operations
BatchedArguments batch = BatchedArguments.of(
    Arguments.of(1, "AC/DC"),
    Arguments.of(2, "Accept")
);
client.sql().executeBatch(null, "INSERT INTO Artist VALUES (?, ?)", batch);
```

### Complex Queries
```java
// Multi-table joins
ResultSet<ArtistWithAlbums> results = client.sql().execute(null,
    "SELECT a.Name as ArtistName, COUNT(al.AlbumId) as AlbumCount " +
    "FROM Artist a LEFT JOIN Album al ON a.ArtistId = al.ArtistId " +
    "GROUP BY a.ArtistId, a.Name ORDER BY AlbumCount DESC");
```

### Analytics Examples
- Artist popularity analysis
- Album and track statistics  
- Customer purchase patterns
- Revenue reporting
- Genre distribution analysis

### POJO Mapping
```java
// Map results to POJOs
ResultSet<Artist> artists = client.sql().execute(null, 
    Mapper.of(Artist.class),
    "SELECT * FROM Artist WHERE Name LIKE ?", "A%");
```

## Development Status

🚧 **In Development** - This module will be implemented as part of Phase 2B of the reference applications project.

## Related Modules

- **Prerequisites**: [sample-data-setup](../sample-data-setup/) - Schema and data required
- **Foundation**: [table-api-app](../table-api-app/) - Object-oriented operations
- **Next Steps**: [transactions-app](../transactions-app/) - Transactional SQL
- **See Also**: [data-streaming-app](../data-streaming-app/) - Bulk data operations