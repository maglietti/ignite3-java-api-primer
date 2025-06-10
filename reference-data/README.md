# Reference Data for Apache Ignite 3 Java API Primer

This directory contains reference datasets used throughout the documentation examples, implementing a **dual-domain strategy** for optimal learning and practical applicability.

## Strategy Overview

The documentation uses two primary data domains:

1. **Music Domain (Primary - 80% of examples)**: Chinook database - digital media store
2. **Business Domain (Secondary - 20% of examples)**: Northwind-style business entities

## Music Domain (Chinook Dataset)

**Source**: Adapted from the `ignite3-chinook-demo` repository  
**Used in sections**: 1-8, and foundational examples in 9-14  
**Location**: `./music-domain/` and `./music-domain-docs/`

### Entities
- **Artist**: Music artists and bands
- **Album**: Music albums (colocated with Artist)
- **Track**: Individual songs (colocated with Album) 
- **Genre**: Music genres (replicated reference data)
- **MediaType**: Audio file formats (replicated reference data)
- **Customer**: Music store customers
- **Invoice**: Customer purchases (colocated with Customer)
- **InvoiceLine**: Purchase line items (colocated with Invoice)
- **Employee**: Store employees
- **Playlist**: User-created playlists
- **PlaylistTrack**: Playlist contents (colocated with Playlist)

### Why Chinook for Primary Examples?
- **Rich relationships**: Natural hierarchies (Artist → Album → Track)
- **Colocation patterns**: Perfect for demonstrating data locality optimizations
- **Complex queries**: Multi-level JOINs and aggregations
- **Universal appeal**: Music is universally understood
- **Progressive complexity**: Can start simple and build sophisticated scenarios

### Key Features
- **Distribution Zones**: 
  - `Chinook` zone (2 replicas) for transactional data
  - `ChinookReplicated` zone (3 replicas) for reference data
- **Colocation Strategy**: Hierarchical data placement for optimal performance
- **Composite Keys**: Demonstrates advanced key design patterns
- **Realistic Data**: Based on real iTunes-style digital music store

## Business Domain (Recommended: Northwind-Style)

**Recommended for sections**: 11 (Integration Patterns), 12 (Best Practices), selected examples in 14 (Caching)

### Recommended Entities

```java
// Core business entities
@Table(zone = @Zone(value = "business_zone"))
public class Customer {
    @Id private Long customerId;
    @Column private String companyName;
    @Column private String contactName;
    @Column private String country;
    @Column private String city;
    @Column private String email;
}

@Table(zone = @Zone(value = "business_zone"), colocateBy = @ColumnRef("customerId"))
public class Order {
    @Id private Long orderId;
    @Id @Column private Long customerId;
    @Column private LocalDate orderDate;
    @Column private String status;
    @Column private BigDecimal totalAmount;
}

@Table(zone = @Zone(value = "business_zone"))
public class Product {
    @Id private Long productId;
    @Column private String productName;
    @Column private Long categoryId;
    @Column private BigDecimal unitPrice;
    @Column private Integer unitsInStock;
}

@Table(zone = @Zone(value = "business_zone"), colocateBy = @ColumnRef("orderId"))
public class OrderDetail {
    @Id private Long orderId;
    @Id private Long productId;
    @Column private Integer quantity;
    @Column private BigDecimal unitPrice;
    @Column private BigDecimal discount;
}

@Table(zone = @Zone(value = "business_replicated"))
public class Category {
    @Id private Long categoryId;
    @Column private String categoryName;
    @Column private String description;
}
```

### Why Business Domain for Selected Examples?
- **Enterprise relevance**: More relatable for business application developers
- **Integration patterns**: Natural fit for enterprise integration scenarios
- **Realistic transactions**: Order processing, inventory management
- **Caching patterns**: Product catalogs, customer data are common cache scenarios

## Implementation Status

### ✅ Completed
- [x] Copied Chinook dataset from `ignite3-chinook-demo`
- [x] Organized music domain model classes
- [x] Imported documentation and schema
- [x] Defined dual-domain strategy

### ⏳ Next Steps
- [ ] Create business domain model classes (Northwind-style)
- [ ] Update sections 1-8 to use consistent Chinook entities
- [ ] Update sections 11-14 to strategically use business domain where appropriate
- [ ] Create cross-reference examples showing pattern translation between domains
- [ ] Add data generation utilities for both domains

## Files Structure

```
reference-data/
├── README.md                          # This file
├── CHINOOK_README.md                  # Original Chinook demo README
├── chinook-ignite3.sql               # SQL schema for Chinook
├── music-domain/                     # Chinook Java model classes
│   ├── Album.java
│   ├── Artist.java
│   ├── Customer.java
│   ├── Employee.java
│   ├── Genre.java
│   ├── Invoice.java
│   ├── InvoiceLine.java
│   ├── MediaType.java
│   ├── Playlist.java
│   ├── PlaylistTrack.java
│   └── Track.java
└── music-domain-docs/                # Chinook documentation
    ├── annotations.md
    ├── bulk-load.md
    ├── data-model.md
    ├── developer-guide.md
    ├── distribution-zones.md
    ├── examples.md
    ├── factory-classes.md
    ├── getting-started.md
    ├── pojo-mapping.md
    ├── quickstart-guide.md
    └── storage-profiles.md
```

## Usage Guidelines

### When to Use Music Domain (Chinook)
- Schema design and annotations (Section 3)
- Table API operations (Section 4)  
- SQL API operations (Section 5)
- Transaction examples (Section 6)
- Compute API examples (Section 7)
- Data streaming basic examples (Section 8)
- Any foundational concept demonstration

### When to Use Business Domain
- Enterprise integration patterns (Section 11)
- Production best practices (Section 12)
- Business-specific caching scenarios (Section 14)
- Real-world transaction scenarios
- Microservices integration examples

### Naming Conventions

**Music Domain (Chinook-style)**:
- PascalCase field names (matching original Chinook): `ArtistId`, `Name`
- Composite keys for relationships: `(AlbumId, ArtistId)`
- Zone names: `Chinook`, `ChinookReplicated`

**Business Domain (Modern Java-style)**:
- camelCase field names: `customerId`, `companyName`
- Single Long IDs where possible: `customerId`, `orderId`
- Zone names: `business_zone`, `business_replicated`

This dual-domain approach balances consistency (reducing cognitive load) with appropriateness (using the best domain for each concept), resulting in more effective learning materials.