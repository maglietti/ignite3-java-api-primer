# Schema Annotations - Apache Ignite 3 Reference

**Schema-as-code with annotation-driven table creation**

📖 **Related Documentation**: [Schema as Code with Annotations](../../docs/03-schema-as-code-with-annotations.md)

## Overview

Master Ignite 3's schema-as-code approach using Java annotations. Learn how to define tables, zones, colocation, and indexes declaratively through POJOs.

## What You'll Learn

- **@Table Annotation**: Define table metadata and configuration
- **@Zone Configuration**: Control data placement and replication
- **@Column Mapping**: Map Java fields to database columns with constraints
- **@Id Keys**: Define simple and composite primary keys
- **@Index Creation**: Add indexes for query performance
- **Colocation Strategies**: Optimize performance with data placement

## Prerequisites

**Required**: Complete [sample-data-setup](../sample-data-setup/) to see annotation examples in action.

## Coming Soon

This reference application is in development. It will demonstrate:

### Annotation Patterns
```java
@Table(zone = @Zone(value = "MusicStore", storageProfiles = "default"),
       colocateBy = @ColumnRef("ArtistId"),
       indexes = @Index(value = "idx_artist", columns = @ColumnRef("ArtistId")))
public class Album {
    @Id Integer AlbumId;
    @Id Integer ArtistId;
    @Column(value = "Title", nullable = false) String Title;
}
```

### Zone Configuration
- Distribution zone creation and management
- Replication factor decisions
- Storage profile selection
- Zone-based data placement

### Colocation Strategies  
- Parent-child colocation for performance
- Multi-level colocation hierarchies
- Colocation key selection
- Query optimization through colocation

### Schema Evolution
- Adding new columns safely
- Index creation and management
- Schema migration patterns
- Backward compatibility

## Development Status

🚧 **In Development** - This module will be implemented as part of Phase 2B of the reference applications project.

## Related Modules

- **Foundation**: [sample-data-setup](../sample-data-setup/) - See annotations in practice
- **Next Steps**: [table-api-app](../table-api-app/) - Use the defined schema
- **See Also**: [catalog-management-app](../catalog-management-app/) - Advanced schema operations