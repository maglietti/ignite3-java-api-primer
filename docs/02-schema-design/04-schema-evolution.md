# Chapter 2.4: Schema Evolution and Migration Patterns

## Learning Objectives

By completing this chapter, you will:

- Choose between Key/Value and Record access patterns for your use cases
- Generate and validate automatic DDL from your annotated classes
- Implement production-ready schema deployment patterns
- Apply best practices for schema management and evolution

## Choosing Your Data Access Pattern

Ignite 3 provides two primary approaches to working with your distributed tables: **Key/Value operations** for simple access patterns, and **Record operations** for full object handling. Understanding when to use each approach optimizes both development speed and runtime performance.

### Key/Value Pattern: When Speed Matters Most

Use the Key/Value pattern when you need maximum performance for simple operations:

**Perfect for:**

- Cache-like operations (get, put, remove by key)
- High-frequency lookups
- Simple data structures
- APIs that need sub-millisecond response times

```java
// Key/Value operations - maximum performance
Table artistTable = client.tables().table("Artist");
KeyValueView<Integer, String> artistNames = artistTable.keyValueView(Integer.class, String.class);

// Lightning-fast operations
artistNames.put(null, 1, "The Beatles");           // Store just the name
String name = artistNames.get(null, 1);            // Retrieve just the name
artistNames.remove(null, 1);                       // Remove by key

// Batch operations for even better performance
Map<Integer, String> batch = Map.of(
    1, "The Beatles",
    2, "AC/DC", 
    3, "Pink Floyd"
);
artistNames.putAll(null, batch);                   // Bulk insert
Collection<String> names = artistNames.getAll(null, List.of(1, 2, 3)); // Bulk retrieve
```

**Performance Benefits:**

- **Minimal serialization overhead**: Only key and single value transmitted
- **Optimal network usage**: Smallest possible payload sizes
- **Direct partition access**: No complex object mapping
- **Batch optimization**: Bulk operations use minimal bandwidth

### Record Pattern: When Objects Matter

Use the Record pattern when you need full object functionality:

**Perfect for:**

- Complex business entities with multiple fields
- Object-oriented application design
- Rich domain models with business logic
- When you need type safety and IDE support

```java
// Record operations - full object handling
Table artistTable = client.tables().table("Artist");
RecordView<Artist> artists = artistTable.recordView(Artist.class);

// Full object operations
Artist beatles = new Artist(1, "The Beatles");
artists.upsert(null, beatles);                     // Store complete object

Artist retrieved = artists.get(null, new Artist(1, null)); // Retrieve complete object
System.out.println(retrieved.getName());           // Type-safe access

// Complex operations with full objects
Collection<Artist> batch = List.of(
    new Artist(1, "The Beatles"),
    new Artist(2, "AC/DC"),
    new Artist(3, "Pink Floyd")
);
artists.upsertAll(null, batch);                    // Bulk insert full objects
```

**Development Benefits:**

- **Type safety**: Compile-time error detection
- **IDE support**: Auto-completion and refactoring
- **Business logic**: Methods and validation in your entities
- **Maintainability**: Clear object-oriented design

### Making the Right Choice for Your Application

Consider this decision framework:

```java
// Use Key/Value when:
public class SessionCache {
    private KeyValueView<String, String> sessions;
    
    public void storeSession(String sessionId, String userId) {
        sessions.put(null, sessionId, userId);      // Simple, fast
    }
    
    public String getUser(String sessionId) {
        return sessions.get(null, sessionId);       // Sub-millisecond lookup
    }
}

// Use Record when:
public class CustomerService {
    private RecordView<Customer> customers;
    
    public void updateCustomer(Customer customer) {
        customers.upsert(null, customer);           // Full object with validation
    }
    
    public Customer getCustomerProfile(Integer customerId) {
        Customer key = new Customer();
        key.setCustomerId(customerId);
        return customers.get(null, key);            // Rich domain object
    }
}
```

**Performance vs Functionality Trade-off:**

**Key/Value Advantages:**

- Fastest possible operations
- Minimal memory usage
- Optimal for high-frequency simple operations
- Best for cache-like workloads

**Record Advantages:**

- Rich object models
- Business logic integration
- Type safety and IDE support
- Better for complex applications

## Automatic DDL Generation

One of Ignite 3's most powerful features is automatic DDL generation from your annotated classes. This eliminates schema synchronization issues and ensures your database schema always matches your application code.

### Understanding the DDL Generation Process

When you call `client.catalog().createTable(Artist.class)`, here's what happens:

```java
// Your annotated class
@Table(
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    indexes = { @Index(value = "IDX_ArtistName", columns = { @ColumnRef("Name") }) }
)
public class Artist {
    @Id
    @Column(value = "ArtistId", nullable = false)
    private Integer ArtistId;
    
    @Column(value = "Name", nullable = true, length = 120)
    private String Name;
}
```

**Generates this SQL DDL:**

```sql
-- Zone creation (if not exists)
CREATE ZONE IF NOT EXISTS MusicStore WITH REPLICAS=2, PARTITIONS=32, STORAGE_PROFILES='default';

-- Table creation
CREATE TABLE Artist (
    ArtistId INTEGER NOT NULL,
    Name VARCHAR(120),
    PRIMARY KEY (ArtistId)
) WITH PRIMARY_ZONE='MusicStore';

-- Index creation
CREATE INDEX IDX_ArtistName ON Artist (Name);
```

**What Ignite Handles Automatically:**

- **Type mapping**: Java types → SQL types with optimal settings
- **Constraint generation**: NOT NULL, PRIMARY KEY constraints
- **Zone assignment**: Tables placed in correct distribution zones
- **Index creation**: Secondary indexes for performance
- **Cluster distribution**: Schema propagated to all nodes

### Creating Your Complete Schema

Production applications need a systematic approach to schema creation. Here's a pattern that handles dependencies and provides good error handling:

```java
public class SchemaSetup {
    
    public static void createCompleteSchema(IgniteClient client) {
        try {
            // Step 1: Create zones first
            createDistributionZones(client);
            
            // Step 2: Create tables in dependency order
            createIndependentTables(client);
            createDependentTables(client);
            
            // Step 3: Verify schema creation
            verifySchemaIntegrity(client);
            
            System.out.println("✓ Complete schema created successfully");
            
        } catch (Exception e) {
            System.err.println("✗ Schema creation failed: " + e.getMessage());
            throw new RuntimeException("Schema setup failed", e);
        }
    }
    
    private static void createDistributionZones(IgniteClient client) {
        // Operational zone - balanced performance
        ZoneDefinition musicStore = ZoneDefinition.builder("MusicStore")
            .ifNotExists()
            .replicas(2)
            .partitions(32)
            .storageProfiles("default")
            .build();
        
        // Reference data zone - optimized for reads
        ZoneDefinition referenceData = ZoneDefinition.builder("MusicStoreReplicated")
            .ifNotExists()
            .replicas(3)
            .partitions(16)
            .storageProfiles("default")
            .build();
        
        client.catalog().createZone(musicStore);
        client.catalog().createZone(referenceData);
        
        System.out.println("✓ Distribution zones created");
    }
    
    private static void createIndependentTables(IgniteClient client) {
        // Tables with no dependencies - can be created first
        
        // Reference data
        client.catalog().createTable(Genre.class);
        client.catalog().createTable(MediaType.class);
        
        // Root entities
        client.catalog().createTable(Artist.class);
        client.catalog().createTable(Customer.class);
        client.catalog().createTable(Employee.class);
        client.catalog().createTable(Playlist.class);
        
        System.out.println("✓ Independent tables created");
    }
    
    private static void createDependentTables(IgniteClient client) {
        // Tables that reference other tables - create in dependency order
        
        // Second level - reference root entities
        client.catalog().createTable(Album.class);      // References Artist
        client.catalog().createTable(Invoice.class);    // References Customer
        
        // Third level - reference second level
        client.catalog().createTable(Track.class);      // References Album, Genre, MediaType
        client.catalog().createTable(InvoiceLine.class); // References Invoice, Track
        client.catalog().createTable(PlaylistTrack.class); // References Playlist, Track
        
        System.out.println("✓ Dependent tables created");
    }
    
    private static void verifySchemaIntegrity(IgniteClient client) {
        // Verify all expected tables exist
        String[] expectedTables = {
            "Genre", "MediaType", "Artist", "Customer", "Employee", 
            "Playlist", "Album", "Invoice", "Track", "InvoiceLine", "PlaylistTrack"
        };
        
        for (String tableName : expectedTables) {
            try {
                Table table = client.tables().table(tableName);
                if (table == null) {
                    throw new RuntimeException("Table not found: " + tableName);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to verify table: " + tableName, e);
            }
        }
        
        System.out.println("✓ Schema integrity verified");
    }
}
```

### Schema Validation and Error Handling

Robust schema setup includes validation and clear error messages:

```java
public class SchemaValidator {
    
    public static void validateSchemaAnnotations() {
        // Validate that all entity classes have proper annotations
        Class<?>[] entityClasses = {
            Artist.class, Album.class, Track.class,
            Customer.class, Invoice.class, InvoiceLine.class,
            Genre.class, MediaType.class, Employee.class,
            Playlist.class, PlaylistTrack.class
        };
        
        for (Class<?> entityClass : entityClasses) {
            validateEntityClass(entityClass);
        }
        
        System.out.println("✓ All entity annotations validated");
    }
    
    private static void validateEntityClass(Class<?> entityClass) {
        // Check @Table annotation
        if (!entityClass.isAnnotationPresent(Table.class)) {
            throw new RuntimeException("Missing @Table annotation: " + entityClass.getSimpleName());
        }
        
        // Check for @Id annotations
        boolean hasIdField = Arrays.stream(entityClass.getDeclaredFields())
            .anyMatch(field -> field.isAnnotationPresent(Id.class));
        
        if (!hasIdField) {
            throw new RuntimeException("Missing @Id annotation: " + entityClass.getSimpleName());
        }
        
        // Check default constructor
        try {
            entityClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Missing default constructor: " + entityClass.getSimpleName());
        }
        
        // Validate colocation setup
        validateColocationSetup(entityClass);
    }
    
    private static void validateColocationSetup(Class<?> entityClass) {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        
        if (tableAnnotation.colocateBy().length > 0) {
            // If colocation is specified, ensure colocation field is part of primary key
            String colocationField = tableAnnotation.colocateBy()[0].value();
            
            boolean colocationFieldIsId = Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .anyMatch(field -> {
                    Column columnAnnotation = field.getAnnotation(Column.class);
                    String columnName = columnAnnotation != null ? columnAnnotation.value() : field.getName();
                    return columnName.equals(colocationField);
                });
            
            if (!colocationFieldIsId) {
                throw new RuntimeException("Colocation field must be part of primary key: " + 
                    entityClass.getSimpleName() + "." + colocationField);
            }
        }
    }
}
```

### Production Deployment Pattern

Here's a complete pattern for production schema deployment:

```java
public class ProductionSchemaDeployment {
    
    public static void main(String[] args) {
        // Production schema deployment with comprehensive error handling
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses("node1:10800", "node2:10800", "node3:10800")  // Production cluster
                .connectTimeout(30000)                                     // Extended timeout
                .build()) {
            
            System.out.println("=== Production Schema Deployment ===");
            
            // Step 1: Validate annotations before deployment
            System.out.println("--- Validating entity annotations");
            SchemaValidator.validateSchemaAnnotations();
            
            // Step 2: Check cluster health
            System.out.println("--- Checking cluster health");
            validateClusterHealth(client);
            
            // Step 3: Deploy schema
            System.out.println("--- Deploying schema");
            SchemaSetup.createCompleteSchema(client);
            
            // Step 4: Verify deployment
            System.out.println("--- Verifying deployment");
            performPostDeploymentTests(client);
            
            System.out.println("=== Schema deployment completed successfully ===");
            
        } catch (Exception e) {
            System.err.println("=== Schema deployment failed ===");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void validateClusterHealth(IgniteClient client) {
        try {
            // Basic connectivity test
            client.sql().execute(null, "SELECT 1");
            System.out.println("✓ Cluster connectivity verified");
            
        } catch (Exception e) {
            throw new RuntimeException("Cluster health check failed", e);
        }
    }
    
    private static void performPostDeploymentTests(IgniteClient client) {
        try {
            // Test basic operations on each table
            testBasicCRUD(client);
            testColocationQueries(client);
            
            System.out.println("✓ Post-deployment tests passed");
            
        } catch (Exception e) {
            throw new RuntimeException("Post-deployment tests failed", e);
        }
    }
    
    private static void testBasicCRUD(IgniteClient client) {
        // Test Artist table
        RecordView<Artist> artists = client.tables().table("Artist").recordView(Artist.class);
        
        Artist testArtist = new Artist(999999, "Test Artist");
        artists.upsert(null, testArtist);
        
        Artist retrieved = artists.get(null, new Artist(999999, null));
        if (retrieved == null || !retrieved.getName().equals("Test Artist")) {
            throw new RuntimeException("Basic CRUD test failed for Artist table");
        }
        
        artists.delete(null, new Artist(999999, null));  // Cleanup
    }
    
    private static void testColocationQueries(IgniteClient client) {
        // Test that colocated queries work correctly
        var result = client.sql().execute(null, 
            "SELECT COUNT(*) as table_count FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'");
        
        if (result.hasNext()) {
            var row = result.next();
            int tableCount = row.intValue("table_count");
            if (tableCount < 10) {  // Should have at least 10 tables
                throw new RuntimeException("Expected table count not met: " + tableCount);
            }
        }
    }
}
```

## Best Practices for Production Schema Management

### 1. Version Control Integration

Keep your entity definitions in version control alongside your application code:

```text
src/
├── main/
│   ├── java/
│   │   └── com/musicstore/entities/
│   │       ├── Artist.java          # Schema definitions
│   │       ├── Album.java
│   │       ├── Track.java
│   │       └── ...
│   └── resources/
│       └── schema/
│           ├── deployment.properties
│           └── README.md            # Schema documentation
```

### 2. Environment-Specific Configuration

Use different zone configurations for different environments:

```java
public class EnvironmentSpecificZones {
    
    public static ZoneDefinition createMusicStoreZone(String environment) {
        ZoneDefinition.Builder builder = ZoneDefinition.builder("MusicStore").ifNotExists();
        
        switch (environment.toLowerCase()) {
            case "development":
                return builder.replicas(1).partitions(8).build();      // Minimal resources
                
            case "testing":
                return builder.replicas(2).partitions(16).build();     // Balanced
                
            case "production":
                return builder.replicas(3).partitions(32).build();     // High availability
                
            default:
                throw new IllegalArgumentException("Unknown environment: " + environment);
        }
    }
}
```

### 3. Schema Evolution Patterns

Plan for schema changes over time:

```java
// Version 1: Initial schema
@Table(zone = @Zone(value = "MusicStore"))
public class Track {
    @Id private Integer TrackId;
    @Column(length = 200) private String Name;
    @Column(precision = 10, scale = 2) private BigDecimal UnitPrice;
}

// Version 2: Add new optional fields (safe change)
@Table(zone = @Zone(value = "MusicStore"))
public class Track {
    @Id private Integer TrackId;
    @Column(length = 200) private String Name;
    @Column(precision = 10, scale = 2) private BigDecimal UnitPrice;
    
    // New optional fields - backwards compatible
    @Column(nullable = true) private Integer Milliseconds;
    @Column(nullable = true) private String Composer;
}
```

## Key Takeaways

Understanding schema evolution and production patterns enables you to:

- **Choose optimal access patterns** based on your application's performance requirements
- **Generate reliable DDL** automatically from your annotated entities
- **Deploy schemas systematically** with proper error handling and validation
- **Manage schema evolution** while maintaining backwards compatibility

These patterns ensure your distributed schemas evolve smoothly from development through production, maintaining high performance and reliability at scale.

## Series Conclusion

You've now mastered the complete schema design lifecycle in Ignite 3, from basic annotations through advanced colocation patterns to production deployment. The next phase of your journey focuses on leveraging these schemas through Ignite 3's powerful data access APIs.

- **Continue Learning**: **[Module 3: Data Access APIs](../03-data-access-apis/01-table-api-operations.md)** - Apply your schema knowledge to master object-oriented and relational data access patterns
