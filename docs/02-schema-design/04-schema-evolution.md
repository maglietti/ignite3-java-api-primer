<!--
Licensed under Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International (CC BY-NC-SA 4.0)
SPDX-License-Identifier: CC-BY-NC-SA-4.0
For full license text, see LICENSE-CC-BY-NC-SA-4.0
-->

# Chapter 2.4: Schema Changes

Your production schema change just broke the mobile app because column additions weren't backward compatible and the rolling deployment failed. While your database accepted the ALTER TABLE statement, older application versions couldn't handle the new schema structure, causing cascading failures across your distributed system.

This scenario happens when teams don't understand schema evolution approaches in distributed systems. Unlike traditional databases where schema changes affect a single server, distributed changes must coordinate across multiple nodes while maintaining compatibility with applications that might be at different deployment versions.

## Recommended Approach: Code-First Schema Evolution

**The recommended approach for production schema evolution in Ignite 3 is code-first evolution with automatic DDL generation.** This approach treats your annotated Java classes as the single source of truth for schema definition, eliminating the synchronization problems that cause most production schema failures.

### Why Code-First Evolution Works

Manual DDL changes cause production problems when:

- Multiple developers modify schemas independently, creating inconsistent environments
- Application code and database schema get out of sync during deployments
- Rolling deployments fail because schema versions don't match application expectations
- Rollback procedures must coordinate both database and application changes

Code-first evolution solves these problems by:

- Making your Java code the definitive schema definition
- Ensuring application and schema are always synchronized
- Enabling atomic rollbacks of both code and schema changes
- Preventing environment drift through version-controlled schema definitions

### Core Implementation Pattern

The recommended approach uses annotated entity classes with systematic deployment:

```java
// Your annotated class is the schema source of truth
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

// Deployment creates schema from annotations
public class SchemaDeployment {
    public static void deployProduction(IgniteClient client) {
        // Pre-deployment validation catches errors early
        SchemaValidator.validateSchemaAnnotations();
        
        // Systematic deployment handles dependencies correctly
        client.catalog().createTable(Artist.class);
        
        // Post-deployment verification ensures success
        verifyDeployment(client);
    }
}
```

**Evolution Benefits:**

- Schema always matches application code exactly
- No manual DDL synchronization between environments
- Version control tracks all schema changes through code changes
- Impossible to deploy applications with mismatched schemas
- Simplified rollback procedures (schema and code rollback together)

## Access Pattern Selection for Evolution Safety

Different access methods handle schema evolution differently. Key/Value operations provide more flexibility during schema changes, while Record operations offer stronger type safety but require more careful coordination during evolution.

### Key/Value Pattern: Evolution Flexibility

Key/Value operations handle schema changes gracefully because they work with primitive types that rarely change structure. When your schema evolves, the underlying key and value types remain stable.

```java
// Key/Value operations survive schema changes
Table artistTable = client.tables().table("Artist");
KeyValueView<Integer, String> artistNames = artistTable.keyValueView(Integer.class, String.class);

// These operations work regardless of schema changes to other columns
artistNames.put(null, 1, "The Beatles");
String name = artistNames.get(null, 1);
artistNames.remove(null, 1);

// Even when Artist table gains new columns, key/value operations continue working
// because they only interact with specific key/value columns
Map<Integer, String> batch = Map.of(
    1, "The Beatles",
    2, "AC/DC", 
    3, "Pink Floyd"
);
artistNames.putAll(null, batch);
Collection<String> names = artistNames.getAll(null, List.of(1, 2, 3));
```

**Evolution Benefits:**

- Schema changes don't break existing key/value operations
- No object deserialization issues during rolling deployments
- Minimal coordination required between application versions
- Faster rollback when schema changes cause problems

### Record Pattern: Type Safety with Evolution Risk

Record operations provide type safety but require careful management during schema evolution. Object deserialization can fail when schema versions mismatch between application and database.

```java
// Record operations require schema compatibility
Table artistTable = client.tables().table("Artist");
RecordView<Artist> artists = artistTable.recordView(Artist.class);

// These operations depend on Artist class structure matching table schema
Artist beatles = new Artist(1, "The Beatles");
artists.upsert(null, beatles);

// If Artist class adds fields but table doesn't, deserialization fails
Artist retrieved = artists.get(null, new Artist(1, null));
System.out.println(retrieved.getName()); // Might fail if schema mismatch

// During schema evolution, batch operations can fail if any object incompatible
Collection<Artist> batch = List.of(
    new Artist(1, "The Beatles"),
    new Artist(2, "AC/DC"),
    new Artist(3, "Pink Floyd")
);
artists.upsertAll(null, batch); // Fails if schema mismatch
```

**Evolution Challenges:**

- Object structure must match table schema exactly
- Deserialization errors during rolling deployments
- Requires coordinated deployment of schema and application changes
- Difficult rollback when schema changes break object compatibility

### Choosing Pattern Based on Evolution Requirements

Your choice between Key/Value and Record operations affects how you handle schema evolution:

```java
// Key/Value operations: Stable during schema evolution
public class SessionCache {
    private KeyValueView<String, String> sessions;
    
    public void storeSession(String sessionId, String userId) {
        sessions.put(null, sessionId, userId); // Works regardless of table changes
    }
    
    public String getUser(String sessionId) {
        return sessions.get(null, sessionId); // No deserialization risk
    }
}

// Record operations: Requires schema coordination
public class CustomerService {
    private RecordView<Customer> customers;
    
    public void updateCustomer(Customer customer) {
        customers.upsert(null, customer); // Fails if Customer class and table mismatch
    }
    
    public Customer getCustomerProfile(Integer customerId) {
        Customer key = new Customer();
        key.setCustomerId(customerId);
        return customers.get(null, key); // Deserialization depends on schema match
    }
}
```

**Evolution Safety Trade-offs:**

**Key/Value for Safe Evolution:**

- Continues working during schema changes
- No object compatibility issues
- Faster recovery from failed schema changes
- Suitable for systems requiring high availability during updates

**Record for Type Safety:**

- Compile-time validation of data access
- Rich domain modeling capabilities
- Better development experience
- Requires coordinated deployments for schema changes

### Choosing the Right Pattern for Evolution

**For systems requiring frequent schema changes and high availability during updates**: Use Key/Value operations combined with code-first schema evolution. This combination provides maximum flexibility during rolling deployments while maintaining schema consistency through version-controlled annotations.

**For systems with stable schemas and strong type safety requirements**: Use Record operations with carefully coordinated deployment procedures. Ensure thorough testing of schema changes in staging environments that mirror production deployment processes.

**Recommended hybrid approach**: Use Key/Value operations for high-frequency operational data and Record operations for configuration or reference data that changes infrequently. This balances evolution flexibility with development experience.

## Implementing Code-First Schema Evolution

The code-first approach requires systematic implementation to handle the complexity of distributed deployments. These patterns ensure your schema evolution maintains consistency across environments and provides clear error handling when problems occur.

### DDL Generation Process

When you call `client.catalog().createTable(Artist.class)`, the system generates DDL that matches your current class definition exactly:

```java
// Your annotated class defines the schema
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

**Generates exactly this SQL DDL:**

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

**Evolution Benefits of Generated DDL:**

- Schema always matches application code exactly
- No manual DDL synchronization between environments
- Version control tracks all schema changes through code changes
- Impossible to deploy applications with mismatched schemas
- Rollback complexity reduced (schema and code rollback together)

### Systematic Schema Deployment to Prevent Evolution Problems

Schema creation failures in production often happen because tables are created in the wrong order, causing foreign key constraint violations or missing dependencies. A systematic deployment approach handles these dependencies correctly and provides detailed error information when problems occur.

```java
public class SchemaSetup {
    
    public static void createCompleteSchema(IgniteClient client) {
        try {
            // Step 1: Create zones first to avoid dependency issues
            createDistributionZones(client);
            
            // Step 2: Create tables in correct dependency order
            createIndependentTables(client);
            createDependentTables(client);
            
            // Step 3: Verify deployment succeeded before marking complete
            verifySchemaIntegrity(client);
            
            System.out.println("✓ Schema deployment completed successfully");
            
        } catch (Exception e) {
            System.err.println("✗ Schema deployment failed: " + e.getMessage());
            // Log specific failure details for troubleshooting
            logDeploymentFailure(e);
            throw new RuntimeException("Schema deployment failed", e);
        }
    }
    
    private static void createDistributionZones(IgniteClient client) {
        // Create zones before tables to avoid "zone not found" errors
        ZoneDefinition musicStore = ZoneDefinition.builder("MusicStore")
            .ifNotExists() // Prevents errors on repeat deployments
            .replicas(2)
            .partitions(32)
            .storageProfiles("default")
            .build();
        
        ZoneDefinition referenceData = ZoneDefinition.builder("MusicStoreReplicated")
            .ifNotExists() // Safe for re-deployment
            .replicas(3)    // Higher replica count for reference data availability
            .partitions(16) // Fewer partitions for mostly-read data
            .storageProfiles("default")
            .build();
        
        client.catalog().createZone(musicStore);
        client.catalog().createZone(referenceData);
        
        System.out.println("✓ Distribution zones created");
    }
    
    private static void createIndependentTables(IgniteClient client) {
        // Create tables with no foreign key dependencies first
        // Prevents constraint violation errors during deployment
        
        // Reference data tables - no dependencies
        client.catalog().createTable(Genre.class);
        client.catalog().createTable(MediaType.class);
        
        // Root entity tables - no dependencies  
        client.catalog().createTable(Artist.class);
        client.catalog().createTable(Customer.class);
        client.catalog().createTable(Employee.class);
        client.catalog().createTable(Playlist.class);
        
        System.out.println("✓ Independent tables created");
    }
    
    private static void createDependentTables(IgniteClient client) {
        // Create tables with foreign key dependencies in correct order
        // Wrong order causes "referenced table does not exist" errors
        
        // Level 2: Reference root entities
        client.catalog().createTable(Album.class);      // Needs Artist
        client.catalog().createTable(Invoice.class);    // Needs Customer, Employee
        
        // Level 3: Reference level 2 tables
        client.catalog().createTable(Track.class);      // Needs Album, Genre, MediaType
        client.catalog().createTable(InvoiceLine.class); // Needs Invoice, Track
        client.catalog().createTable(PlaylistTrack.class); // Needs Playlist, Track
        
        System.out.println("✓ Dependent tables created");
    }
    
    private static void verifySchemaIntegrity(IgniteClient client) {
        // Verify deployment succeeded before marking complete
        // Catches partial failures that could cause runtime errors
        String[] expectedTables = {
            "Genre", "MediaType", "Artist", "Customer", "Employee", 
            "Playlist", "Album", "Invoice", "Track", "InvoiceLine", "PlaylistTrack"
        };
        
        for (String tableName : expectedTables) {
            try {
                Table table = client.tables().table(tableName);
                if (table == null) {
                    throw new RuntimeException("Schema verification failed: table not found: " + tableName);
                }
            } catch (Exception e) {
                throw new RuntimeException("Schema verification failed for table: " + tableName + 
                    ". Deployment may be incomplete.", e);
            }
        }
        
        System.out.println("✓ Schema integrity verified - all tables accessible");
    }
    
    private static void logDeploymentFailure(Exception e) {
        // Log detailed failure information for troubleshooting
        System.err.println("Deployment failure details:");
        System.err.println("- Error type: " + e.getClass().getSimpleName());
        System.err.println("- Error message: " + e.getMessage());
        if (e.getCause() != null) {
            System.err.println("- Root cause: " + e.getCause().getMessage());
        }
    }
}
```

### Pre-Deployment Validation to Prevent Runtime Failures

Schema deployment failures often happen because entity classes have missing annotations or configuration errors that only surface during deployment. Pre-deployment validation catches these issues before they cause production problems.

```java
public class SchemaValidator {
    
    public static void validateSchemaAnnotations() {
        // Validate entity classes before deployment to catch annotation errors
        // Prevents deployment failures that would require manual cleanup
        Class<?>[] entityClasses = {
            Artist.class, Album.class, Track.class,
            Customer.class, Invoice.class, InvoiceLine.class,
            Genre.class, MediaType.class, Employee.class,
            Playlist.class, PlaylistTrack.class
        };
        
        for (Class<?> entityClass : entityClasses) {
            validateEntityClass(entityClass);
        }
        
        System.out.println("✓ All entity classes validated for deployment");
    }
    
    private static void validateEntityClass(Class<?> entityClass) {
        // Validate @Table annotation exists
        if (!entityClass.isAnnotationPresent(Table.class)) {
            throw new RuntimeException("Entity class missing @Table annotation: " + 
                entityClass.getSimpleName() + ". Deployment will fail.");
        }
        
        // Validate @Id annotation exists
        boolean hasIdField = Arrays.stream(entityClass.getDeclaredFields())
            .anyMatch(field -> field.isAnnotationPresent(Id.class));
        
        if (!hasIdField) {
            throw new RuntimeException("Entity class missing @Id annotation: " + 
                entityClass.getSimpleName() + ". Primary key required for table creation.");
        }
        
        // Validate default constructor exists (required for deserialization)
        try {
            entityClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Entity class missing default constructor: " + 
                entityClass.getSimpleName() + ". Required for object serialization.");
        }
        
        // Validate colocation configuration
        validateColocationSetup(entityClass);
    }
    
    private static void validateColocationSetup(Class<?> entityClass) {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        
        if (tableAnnotation.colocateBy().length > 0) {
            // Validate colocation field is part of primary key
            // Misconfigured colocation causes data distribution problems
            String colocationField = tableAnnotation.colocateBy()[0].value();
            
            boolean colocationFieldIsId = Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .anyMatch(field -> {
                    Column columnAnnotation = field.getAnnotation(Column.class);
                    String columnName = columnAnnotation != null ? columnAnnotation.value() : field.getName();
                    return columnName.equals(colocationField);
                });
            
            if (!colocationFieldIsId) {
                throw new RuntimeException("Colocation configuration error in " + 
                    entityClass.getSimpleName() + ": colocation field '" + colocationField + 
                    "' must be part of primary key. This will cause incorrect data distribution.");
            }
        }
    }
}
```

### Production-Ready Deployment with Rollback Capability

Production schema deployments fail when they don't validate cluster health, don't test schema changes, or can't recover from partial failures. This pattern includes health checks, deployment verification, and clear rollback procedures.

```java
public class ProductionSchemaDeployment {
    
    public static void main(String[] args) {
        // Production schema deployment with comprehensive validation and rollback capability
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses("node1:10800", "node2:10800", "node3:10800")  // Production cluster
                .connectTimeout(30000)                                     // Extended timeout for cluster operations
                .build()) {
            
            System.out.println("=== Production Schema Deployment Starting ===");
            
            // Step 1: Pre-deployment validation to catch errors early
            System.out.println("--- Phase 1: Pre-deployment validation");
            SchemaValidator.validateSchemaAnnotations();
            
            // Step 2: Cluster health check to ensure deployment target is ready
            System.out.println("--- Phase 2: Cluster health validation");
            validateClusterHealth(client);
            
            // Step 3: Schema deployment with dependency management
            System.out.println("--- Phase 3: Schema deployment");
            SchemaSetup.createCompleteSchema(client);
            
            // Step 4: Post-deployment verification to ensure success
            System.out.println("--- Phase 4: Deployment verification");
            performPostDeploymentTests(client);
            
            System.out.println("=== Schema deployment completed successfully ===");
            
        } catch (Exception e) {
            System.err.println("=== Schema deployment failed - manual intervention required ===");
            System.err.println("Check logs for specific failure details and rollback procedures");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void validateClusterHealth(IgniteClient client) {
        try {
            // Verify cluster connectivity before attempting deployment
            client.sql().execute(null, "SELECT 1");
            System.out.println("✓ Cluster connectivity verified");
            
            // Additional cluster health checks could be added here
            // - Node count validation
            // - Disk space checks
            // - Network partition detection
            
        } catch (Exception e) {
            throw new RuntimeException("Cluster health check failed - deployment aborted. " +
                "Verify cluster status before retrying.", e);
        }
    }
    
    private static void performPostDeploymentTests(IgniteClient client) {
        try {
            // Verify schema deployment succeeded with functional tests
            testBasicCRUD(client);
            testColocationQueries(client);
            
            System.out.println("✓ Post-deployment verification completed");
            
        } catch (Exception e) {
            throw new RuntimeException("Post-deployment verification failed - " +
                "schema may be partially deployed. Manual verification required.", e);
        }
    }
    
    private static void testBasicCRUD(IgniteClient client) {
        // Test table accessibility and basic operations
        RecordView<Artist> artists = client.tables().table("Artist").recordView(Artist.class);
        
        // Insert test record
        Artist testArtist = new Artist(999999, "Deployment Test Artist");
        artists.upsert(null, testArtist);
        
        // Verify retrieval works
        Artist retrieved = artists.get(null, new Artist(999999, null));
        if (retrieved == null || !retrieved.getName().equals("Deployment Test Artist")) {
            throw new RuntimeException("Schema verification failed: basic CRUD operations not working on Artist table");
        }
        
        // Clean up test data
        artists.delete(null, new Artist(999999, null));
    }
    
    private static void testColocationQueries(IgniteClient client) {
        // Verify schema metadata is accessible and complete
        var result = client.sql().execute(null, 
            "SELECT COUNT(*) as table_count FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'");
        
        if (result.hasNext()) {
            var row = result.next();
            int tableCount = row.intValue("table_count");
            if (tableCount < 10) {  // Expected minimum table count
                throw new RuntimeException("Schema verification failed: expected at least 10 tables, found " + tableCount + 
                    ". Deployment may be incomplete.");
            }
        } else {
            throw new RuntimeException("Schema verification failed: unable to query system tables");
        }
    }
}
```

## Schema Evolution Patterns for Production Systems

Production schema changes cause downtime when not managed correctly. These patterns ensure your schema evolution maintains backward compatibility and handles the complexity of distributed deployments.

### Version Control Strategy for Schema Changes

Schema changes that aren't tracked in version control create deployment inconsistencies across environments. Keep entity definitions as code to ensure all environments use identical schemas.

```text
src/
├── main/
│   ├── java/
│   │   └── com/musicstore/entities/
│   │       ├── Artist.java          # Schema source of truth
│   │       ├── Album.java           # Version controlled with application
│   │       ├── Track.java           # Changes tracked in git
│   │       └── ...
│   └── resources/
│       └── schema/
│           ├── deployment.properties # Environment-specific settings
│           └── evolution.md         # Change log and rollback procedures
```

### Environment-Specific Configuration to Prevent Resource Issues

Production deployments fail when development zone configurations are used in production, causing resource exhaustion or insufficient replication. Configure zones appropriately for each environment's requirements.

```java
public class EnvironmentSpecificZones {
    
    public static ZoneDefinition createMusicStoreZone(String environment) {
        ZoneDefinition.Builder builder = ZoneDefinition.builder("MusicStore").ifNotExists();
        
        switch (environment.toLowerCase()) {
            case "development":
                // Single replica, minimal partitions for resource efficiency
                return builder.replicas(1).partitions(8).build();
                
            case "testing":
                // Balanced configuration for load testing
                return builder.replicas(2).partitions(16).build();
                
            case "production":
                // High availability with sufficient capacity
                return builder.replicas(3).partitions(32).build();
                
            default:
                throw new IllegalArgumentException("Unsupported environment: " + environment + 
                    ". Use 'development', 'testing', or 'production'");
        }
    }
}
```

### Safe Schema Evolution Patterns

Schema changes break applications when they remove columns or change data types that existing code depends on. Use additive changes and maintain backward compatibility during transitions.

```java
// Version 1: Initial schema
@Table(zone = @Zone(value = "MusicStore"))
public class Track {
    @Id private Integer TrackId;
    @Column(length = 200) private String Name;
    @Column(precision = 10, scale = 2) private BigDecimal UnitPrice;
}

// Version 2: Safe evolution - add nullable fields only
@Table(zone = @Zone(value = "MusicStore"))
public class Track {
    @Id private Integer TrackId;
    @Column(length = 200) private String Name;
    @Column(precision = 10, scale = 2) private BigDecimal UnitPrice;
    
    // Safe additions: nullable fields that don't break existing applications
    @Column(nullable = true) private Integer Milliseconds;
    @Column(nullable = true) private String Composer;
}

// Version 3: Unsafe evolution - don't do this in production
@Table(zone = @Zone(value = "MusicStore"))
public class Track {
    @Id private Integer TrackId;
    @Column(length = 200) private String Name;
    // @Column(precision = 10, scale = 2) private BigDecimal UnitPrice;  // REMOVED - breaks existing code
    
    @Column(nullable = true) private Integer Milliseconds;
    @Column(nullable = true) private String Composer;
    @Column(nullable = false) private String Category;  // NON-NULL - breaks existing data
}
```

**Evolution Safety Rules:**

- **Safe changes**: Add nullable columns, increase column lengths, add indexes
- **Dangerous changes**: Remove columns, add non-null columns, change data types
- **Breaking changes**: Modify primary keys, change colocation, alter constraints

## Best Practices Summary

**Recommended Approach: Code-First Schema Evolution**

The patterns in this chapter center around one core principle: **treat your annotated Java classes as the single source of truth for schema definition**. This code-first approach eliminates the synchronization problems that cause most production schema failures.

**Implementation Strategy:**

- Use annotated entity classes to define schemas
- Deploy with systematic validation and dependency management
- Combine with appropriate access patterns for your evolution requirements
- Implement environment-specific configurations for different deployment targets

**Evolution Safety Rules:**

- **Safe changes**: Add nullable columns, increase column lengths, add indexes
- **Dangerous changes**: Remove columns, add non-null columns, change data types
- **Breaking changes**: Modify primary keys, change colocation, alter constraints

**Access Pattern Selection:**

- **Key/Value patterns**: Maximum evolution flexibility for frequently changing schemas
- **Record patterns**: Type safety for stable schemas with coordinated deployment processes
- **Hybrid approach**: Key/Value for operational data, Record for reference data

When you follow this code-first approach with systematic deployment procedures, schema evolution becomes a controlled process that maintains system availability while enabling continuous development. The key is treating schema changes as potentially breaking operations that require the same careful coordination as any other distributed system change.
