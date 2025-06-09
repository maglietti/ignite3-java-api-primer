# 3. Schema-as-Code with Annotations

## 3.1 Introduction to Annotations API

- Why annotations matter in distributed systems
- Schema-as-code benefits
- Annotation processing pipeline

## 3.2 Basic Table Definition

### @Table Annotation Fundamentals

```java
@Table("my_table")
public class MyRecord {
    @Id
    private Integer id;
    
    @Column(value = "name", nullable = false, length = 50)
    private String name;
    
    private String description; // Auto-mapped column
}
```

### @Column for Field Mapping

- `value()` - column name
- `nullable()` - nullability constraint
- `length()` - string length
- `precision()` & `scale()` - numeric precision

### @Id for Primary Keys

- Single field primary keys
- Sort order specification
- Auto-generation strategies

## 3.3 Advanced Schema Features

### Composite Primary Keys

```java
@Table("complex_table")
public class ComplexEntity {
    @Id
    private Long id;
    
    @Id
    @Column("region")
    private String region;
    
    // Other fields...
}
```

### @Zone for Distribution Configuration

```java
@Table(
    value = "distributed_table",
    zone = @Zone(
        value = "my_zone",
        partitions = 16,
        replicas = 3,
        storageProfiles = "default"
    )
)
public class DistributedEntity {
    // Fields...
}
```

### @Index for Secondary Indexes

```java
@Table(
    value = "indexed_table",
    indexes = @Index(
        value = "name_idx",
        columns = {
            @ColumnRef("name"),
            @ColumnRef(value = "created_date", sort = SortOrder.DESC)
        }
    )
)
public class IndexedEntity {
    // Fields...
}
```

### @ColumnRef and Colocation

*[To be completed with colocation examples]*

## 3.4 Key-Value vs Record Mapping

- When to use separate key/value classes
- When to use single record classes
- Performance implications

## 3.5 DDL Generation and Catalog Integration

```java
// Create table from annotations
Table table = ignite.catalog().createTable(MyRecord.class);

// Create table from key-value classes
Table table = ignite.catalog().createTable(PersonKey.class, PersonValue.class);
```

## 3.6 POJO Mapping Deep Dive

- `Mapper.of()` auto-mapping
- Custom field-to-column mapping
- Type conversion system
- Working with complex types