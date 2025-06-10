# Apache Ignite 3 Annotations Reference

This document provides a comprehensive reference for the annotations used in Apache Ignite 3 to map Java POJOs to database tables.

## Core Annotations

### @Table

Marks a Java class as an Ignite table and configures its properties.

**Attributes**:

- `zone`: Defines which distribution zone the table belongs to
- `colocateBy`: Specifies which column should be used for co-location
- `name`: (Optional) Specifies the table name; defaults to the class name
- `indexes`: (Optional) Defines indexes on the table

**Example**:

```java
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default"),
    colocateBy = @ColumnRef("ArtistId"),
    indexes = {
        @Index(value = "IFK_AlbumArtistId", columns = { @ColumnRef("ArtistId") })
    }
)
public class Album {
    // Class body...
}
```

### @Zone

Defines which distribution zone the table belongs to and which storage profiles it can use.

**Attributes**:

- `value`: The name of the distribution zone
- `storageProfiles`: The storage profiles to use for this table

**Example**:

```java
@Zone(value = "Chinook", storageProfiles = "default")
```

### @Column

Maps a Java field to a table column with specific properties.

**Attributes**:

- `value`: The name of the column
- `nullable`: Specifies whether the column can contain NULL values
- `length`: (Optional) For string columns, specifies the maximum length
- `precision`: (Optional) For decimal columns, specifies the precision
- `scale`: (Optional) For decimal columns, specifies the scale

**Example**:

```java
@Column(value = "Name", nullable = false, length = 120)
private String name;
```

### @Id

Marks a field as part of the primary key.

**Example**:

```java
@Id
@Column(value = "ArtistId", nullable = false)
private Integer artistId;
```

### @ColumnRef

References a column for co-location purposes.

**Attributes**:

- `value`: The name of the referenced column

**Example**:

```java
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default"),
    colocateBy = @ColumnRef("ArtistId")
)
```

### @Index

Defines an index on one or more columns.

**Attributes**:

- `value`: The name of the index
- `columns`: The columns to include in the index
- `unique`: (Optional) Whether the index should enforce uniqueness

**Example**:

```java
@Index(value = "IFK_AlbumArtistId", columns = { @ColumnRef("ArtistId") })
```

## Usage Patterns

### Basic Table Definition

```java
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default")
)
public class Artist {
    @Id
    @Column(value = "ArtistId", nullable = false)
    private Integer artistId;

    @Column(value = "Name", nullable = true)
    private String name;
    
    // Constructors, getters, setters...
}
```

### Co-located Table Definition

```java
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default"),
    colocateBy = @ColumnRef("ArtistId")
)
public class Album {
    @Id
    @Column(value = "AlbumId", nullable = false)
    private Integer albumId;

    @Column(value = "Title", nullable = false)
    private String title;

    @Id
    @Column(value = "ArtistId", nullable = false)
    private Integer artistId;
    
    // Constructors, getters, setters...
}
```

### Complex Table with Various Data Types

```java
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default"),
    colocateBy = @ColumnRef("AlbumId")
)
public class Track {
    @Id
    @Column(value = "TrackId", nullable = false)
    private Integer trackId;

    @Column(value = "Name", nullable = false)
    private String name;

    @Id
    @Column(value = "AlbumId", nullable = true)
    private Integer albumId;

    @Column(value = "MediaTypeId", nullable = false)
    private Integer mediaTypeId;

    @Column(value = "GenreId", nullable = true)
    private Integer genreId;

    @Column(value = "Composer", nullable = true)
    private String composer;

    @Column(value = "Milliseconds", nullable = false)
    private Integer milliseconds;

    @Column(value = "Bytes", nullable = true)
    private Integer bytes;

    @Column(value = "UnitPrice", nullable = false)
    private BigDecimal unitPrice;
    
    // Constructors, getters, setters...
}
```

### Table with Index Definitions

```java
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default"),
    indexes = {
        @Index(value = "IDX_CUSTOMER_EMAIL", columns = { @ColumnRef("Email") }, unique = true),
        @Index(value = "IDX_CUSTOMER_LOCATION", columns = { @ColumnRef("Country"), @ColumnRef("City") })
    }
)
public class Customer {
    @Id
    @Column(value = "CustomerId", nullable = false)
    private Integer customerId;

    @Column(value = "Email", nullable = false)
    private String email;

    @Column(value = "Country", nullable = true)
    private String country;

    @Column(value = "City", nullable = true)
    private String city;
    
    // Additional fields and methods...
}
```

## SQL Equivalent

The annotations in Java map to SQL DDL statements:

Java annotation:

```java
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default")
)
public class Artist {
    @Id
    @Column(value = "ArtistId", nullable = false)
    private Integer artistId;

    @Column(value = "Name", nullable = true)
    private String name;
}
```

SQL equivalent:

```sql
CREATE TABLE Artist (
    ArtistId INT PRIMARY KEY,
    Name VARCHAR
) ZONE Chinook STORAGE PROFILE 'default';
```

Java annotation with co-location:

```java
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default"),
    colocateBy = @ColumnRef("ArtistId")
)
public class Album {
    @Id
    @Column(value = "AlbumId", nullable = false)
    private Integer albumId;

    @Column(value = "Title", nullable = false)
    private String title;

    @Id
    @Column(value = "ArtistId", nullable = false)
    private Integer artistId;
}
```

SQL equivalent with co-location:

```sql
CREATE TABLE Album (
    AlbumId INT,
    Title VARCHAR NOT NULL,
    ArtistId INT,
    PRIMARY KEY (AlbumId, ArtistId)
) ZONE Chinook STORAGE PROFILE 'default' COLOCATE BY (ArtistId);
```

Java annotation with indexes:

```java
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default"),
    indexes = {
        @Index(value = "IDX_ARTIST_NAME", columns = { @ColumnRef("Name") })
    }
)
public class Artist {
    @Id
    @Column(value = "ArtistId", nullable = false)
    private Integer artistId;

    @Column(value = "Name", nullable = true)
    private String name;
}
```

SQL equivalent with indexes:

```sql
CREATE TABLE Artist (
    ArtistId INT PRIMARY KEY,
    Name VARCHAR
) ZONE Chinook STORAGE PROFILE 'default';

CREATE INDEX IDX_ARTIST_NAME ON Artist(Name);
```

## Best Practices

### Primary Keys

- Always mark primary key fields with `@Id`
- Primary keys should not be null, so use `nullable = false`
- For complex primary keys, mark all components with `@Id`
- Use meaningful and consistent names for primary key fields

### Data Types

- Use appropriate Java types for columns
- For nullable fields, use object types (Integer) instead of primitives (int)
- For decimal values, use BigDecimal with appropriate precision and scale
- Use java.time classes (LocalDate, LocalDateTime) for date/time fields

### Co-location

- Co-locate tables that are frequently joined
- The co-location key should be part of the primary key
- The co-location key should match the type and name in the referenced table
- Design co-location based on your query patterns

### Naming

- Be consistent with column names
- Consider using the same name for related columns across tables
- Follow a naming convention for primary and foreign keys
- Use clear, descriptive names for indexes

### Distribution Zones and Storage Profiles

- Group related tables in the same distribution zone
- Choose appropriate replica counts based on data importance
- Select storage profiles based on access patterns:
  - Write-heavy workloads: Consider RocksDB storage engine
  - Read-heavy or balanced workloads: Apache Ignite Page Memory engine

## Creating Tables from Annotations

Tables are created from annotated POJOs using the `createTable` method of the `IgniteCatalog` API:

```java
// Create a table from an annotated class
client.catalog().createTable(Artist.class);
```

This method:

1. Reads the annotations from the class
2. Creates a table definition based on the annotations
3. Creates the table in the Ignite catalog using the specified distribution zone and storage profile

You can see this in action in the `TableUtils.java` class:

```java
public static boolean createTables(IgniteClient client) {
    try {
        System.out.println("=== Creating tables ===");

        // Use IgniteCatalog.createTable to create tables from annotated classes
        System.out.println("--- Creating Artist table");
        client.catalog().createTable(Artist.class);

        System.out.println("--- Creating Genre table");
        client.catalog().createTable(Genre.class);

        // Additional tables...

        return true;
    } catch (Exception e) {
        System.err.println("Error creating tables: " + e.getMessage());
        e.printStackTrace();
        return false;
    }
}
```

## Comparison with Ignite 2.x Annotations

Ignite 3 introduces several changes compared to Ignite 2.x:

| Feature | Ignite 2.x | Ignite 3 |
|---------|------------|----------|
| Table Definition | `@QuerySqlTable` | `@Table` |
| Zone/Region | `@CacheConfiguration` | `@Zone` |
| Co-location | `@AffinityKeyMapped` | `colocateBy = @ColumnRef()` |
| Storage | `@DataRegionConfiguration` | Storage Profiles in `@Zone` |
| Indexes | `@QuerySqlField(index=true)` | `@Index` |
