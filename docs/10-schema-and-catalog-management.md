# 10. Schema and Catalog Management

## Introduction to Catalog Management

Apache Ignite 3's catalog management system provides programmatic control over the database schema through the `IgniteCatalog` interface. This enables dynamic schema evolution, automated DDL operations, and robust schema introspection capabilities.

### Key Benefits

1. **Version-Controlled Schema**: Schema changes can be tracked alongside application code
2. **Automated Deployment**: Schema updates can be automated as part of CI/CD pipelines
3. **Runtime Flexibility**: Tables and zones can be created/modified without cluster restarts
4. **Consistency Guarantees**: All schema operations are transactional and distributed
5. **Migration Support**: Built-in support for schema versioning and migration

## Catalog API (`IgniteCatalog`)

### Basic Catalog Operations

```java
public class CatalogBasics {
    
    // Get catalog instance
    public static void demonstrateCatalogAccess(IgniteClient client) {
        IgniteCatalog catalog = client.catalog();
        
        // List all tables
        Collection<String> tableNames = catalog.tables();
        System.out.println("Existing tables: " + tableNames);
        
        // List all zones
        Collection<String> zoneNames = catalog.zones();
        System.out.println("Existing zones: " + zoneNames);
    }
}
```

### DDL Operations from Code

#### Table Creation with Annotations

```java
public class TableCreation {
    
    // Define table with annotations
    @Table(
        zone = @Zone(value = "enterprise_zone", storageProfiles = "default"),
        indexes = {
            @Index(value = "idx_customer_email", columns = { @ColumnRef("email") }),
            @Index(value = "idx_customer_name", columns = { 
                @ColumnRef("lastName"), 
                @ColumnRef("firstName") 
            })
        }
    )
    public static class Customer {
        @Id
        @Column(value = "id", nullable = false)
        private Long id;
        
        @Column(value = "firstName", nullable = false, length = 50)
        private String firstName;
        
        @Column(value = "lastName", nullable = false, length = 50)
        private String lastName;
        
        @Column(value = "email", nullable = false, length = 100)
        private String email;
        
        @Column(value = "phoneNumber", nullable = true, length = 20)
        private String phoneNumber;
        
        @Column(value = "createdAt", nullable = false)
        private LocalDateTime createdAt;
        
        // Constructors, getters, setters...
    }
    
    // Create table with error handling
    public static void createCustomerTable(IgniteClient client) {
        try {
            // Async table creation
            CompletableFuture<Void> future = client.catalog().createTableAsync(Customer.class);
            
            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    if (throwable.getCause() instanceof TableAlreadyExistsException) {
                        System.out.println("Table already exists - skipping creation");
                    } else {
                        System.err.println("Table creation failed: " + throwable.getMessage());
                    }
                } else {
                    System.out.println("Table 'Customer' created successfully");
                }
            });
            
            // Wait for completion (in production, handle asynchronously)
            future.join();
            
        } catch (Exception e) {
            System.err.println("Error creating table: " + e.getMessage());
        }
    }
}
```

#### Manual Table Definition

```java
public class ManualTableDefinition {
    
    // Create table using TableDefinition API
    public static void createOrdersTable(IgniteClient client) {
        TableDefinition tableDefinition = TableDefinition.builder("Orders")
            .ifNotExists()
            .columns(
                ColumnDefinition.builder("orderId", ColumnType.INT64).asNonNull().build(),
                ColumnDefinition.builder("customerId", ColumnType.INT64).asNonNull().build(),
                ColumnDefinition.builder("orderDate", ColumnType.TIMESTAMP).asNonNull().build(),
                ColumnDefinition.builder("status", ColumnType.STRING).length(20).asNonNull().build(),
                ColumnDefinition.builder("totalAmount", ColumnType.DECIMAL).precision(10).scale(2).build()
            )
            .primaryKey("orderId", "customerId")
            .colocation("customerId")
            .zone("enterprise_zone")
            .build();
        
        try {
            client.catalog().createTable(tableDefinition);
            System.out.println("Orders table created with manual definition");
        } catch (Exception e) {
            System.err.println("Failed to create Orders table: " + e.getMessage());
        }
    }
}
```

### Schema Introspection

#### Table Schema Analysis

```java
public class SchemaIntrospection {
    
    // Analyze existing table schema
    public static void analyzeTableSchema(IgniteClient client, String tableName) {
        try {
            // Get table definition
            TableDefinition tableDef = client.catalog().tableDefinition(tableName);
            
            System.out.println("=== Table: " + tableName + " ===");
            System.out.println("Zone: " + tableDef.zone());
            System.out.println("Columns:");
            
            // Analyze columns
            for (ColumnDefinition column : tableDef.columns()) {
                System.out.printf("  %s: %s%s%s%n", 
                    column.name(),
                    column.type(),
                    column.nullable() ? " (nullable)" : " (NOT NULL)",
                    column.length() > 0 ? " length=" + column.length() : "");
            }
            
            // Primary key information
            System.out.println("Primary Key: " + tableDef.primaryKey());
            
            // Colocation information
            if (tableDef.colocation() != null && !tableDef.colocation().isEmpty()) {
                System.out.println("Colocation: " + tableDef.colocation());
            }
            
            // Index information
            System.out.println("Indexes:");
            for (IndexDefinition index : tableDef.indexes()) {
                System.out.printf("  %s: %s%n", index.name(), index.columns());
            }
            
        } catch (Exception e) {
            System.err.println("Failed to analyze table schema: " + e.getMessage());
        }
    }
    
    // Generate schema documentation
    public static void generateSchemaDocs(IgniteClient client) {
        System.out.println("# Database Schema Documentation\n");
        
        // List all tables
        Collection<String> tables = client.catalog().tables();
        
        for (String tableName : tables) {
            try {
                TableDefinition tableDef = client.catalog().tableDefinition(tableName);
                
                System.out.println("## Table: " + tableName);
                System.out.println("**Zone:** " + tableDef.zone());
                System.out.println("\n**Columns:**");
                
                for (ColumnDefinition column : tableDef.columns()) {
                    System.out.printf("- `%s` %s%s%n", 
                        column.name(),
                        column.type(),
                        column.nullable() ? "" : " NOT NULL");
                }
                
                System.out.println("\n**Primary Key:** " + tableDef.primaryKey());
                
                if (!tableDef.indexes().isEmpty()) {
                    System.out.println("\n**Indexes:**");
                    for (IndexDefinition index : tableDef.indexes()) {
                        System.out.printf("- `%s` on %s%n", index.name(), index.columns());
                    }
                }
                
                System.out.println();
                
            } catch (Exception e) {
                System.err.println("Error processing table " + tableName + ": " + e.getMessage());
            }
        }
    }
}
```

## Dynamic Schema Changes

### Adding and Dropping Tables

#### Production-Safe Table Management

```java
public class ProductionTableManagement {
    
    // Safe table creation with validation
    public static boolean createTableSafely(IgniteClient client, Class<?> tableClass) {
        String tableName = extractTableName(tableClass);
        
        try {
            // Check if table already exists
            if (client.catalog().tables().contains(tableName)) {
                System.out.println("Table " + tableName + " already exists");
                return true;
            }
            
            // Create with retry logic
            int maxRetries = 3;
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    client.catalog().createTable(tableClass);
                    System.out.println("Table " + tableName + " created successfully");
                    return true;
                    
                } catch (Exception e) {
                    if (attempt == maxRetries) {
                        throw e;
                    }
                    System.out.println("Attempt " + attempt + " failed, retrying: " + e.getMessage());
                    Thread.sleep(1000 * attempt); // Exponential backoff
                }
            }
            
        } catch (Exception e) {
            System.err.println("Failed to create table " + tableName + ": " + e.getMessage());
            return false;
        }
        
        return false;
    }
    
    // Safe table dropping with dependencies check
    public static boolean dropTableSafely(IgniteClient client, String tableName) {
        try {
            // Verify table exists
            if (!client.catalog().tables().contains(tableName)) {
                System.out.println("Table " + tableName + " does not exist");
                return true;
            }
            
            // Check for data before dropping (optional safety check)
            long recordCount = countRecords(client, tableName);
            if (recordCount > 0) {
                System.out.println("WARNING: Table " + tableName + " contains " + recordCount + " records");
                // In production, you might want to require explicit confirmation
            }
            
            // Drop table
            client.catalog().dropTable(tableName);
            System.out.println("Table " + tableName + " dropped successfully");
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to drop table " + tableName + ": " + e.getMessage());
            return false;
        }
    }
    
    // Count records in table for safety checks
    private static long countRecords(IgniteClient client, String tableName) {
        try {
            var result = client.sql().execute(null, "SELECT COUNT(*) as cnt FROM " + tableName);
            if (result.hasNext()) {
                return result.next().longValue("cnt");
            }
        } catch (Exception e) {
            System.err.println("Failed to count records in " + tableName + ": " + e.getMessage());
        }
        return 0;
    }
    
    // Extract table name from class annotations
    private static String extractTableName(Class<?> tableClass) {
        Table tableAnnotation = tableClass.getAnnotation(Table.class);
        if (tableAnnotation != null && !tableAnnotation.value().isEmpty()) {
            return tableAnnotation.value();
        }
        return tableClass.getSimpleName();
    }
}
```

### Index Management

#### Dynamic Index Operations

```java
public class IndexManagement {
    
    // Create index dynamically
    public static void createPerformanceIndexes(IgniteClient client) {
        try {
            // Create index for frequently queried columns
            String createIndexSql = """
                CREATE INDEX IF NOT EXISTS idx_customer_search 
                ON Customer (lastName, firstName, email)
                """;
            
            client.sql().execute(null, createIndexSql);
            System.out.println("Performance index created");
            
            // Create partial index for active customers
            String partialIndexSql = """
                CREATE INDEX IF NOT EXISTS idx_active_customers 
                ON Customer (email) 
                WHERE status = 'ACTIVE'
                """;
            
            client.sql().execute(null, partialIndexSql);
            System.out.println("Partial index for active customers created");
            
        } catch (Exception e) {
            System.err.println("Failed to create indexes: " + e.getMessage());
        }
    }
    
    // Analyze index usage and performance
    public static void analyzeIndexPerformance(IgniteClient client, String tableName) {
        try {
            // Query to analyze index statistics (if available)
            String analyzeQuery = """
                EXPLAIN ANALYZE SELECT * FROM " + tableName + " 
                WHERE lastName = 'Smith' AND email LIKE '%@gmail.com'
                """;
            
            var result = client.sql().execute(null, analyzeQuery);
            System.out.println("=== Query Execution Plan ===");
            
            while (result.hasNext()) {
                var row = result.next();
                System.out.println(row);
            }
            
        } catch (Exception e) {
            System.err.println("Failed to analyze index performance: " + e.getMessage());
        }
    }
    
    // Drop unused indexes
    public static void cleanupIndexes(IgniteClient client, String tableName) {
        // This is a simplified example - in production you'd want more sophisticated
        // analysis to determine which indexes are actually unused
        
        try {
            String dropIndexSql = "DROP INDEX IF EXISTS old_index_name";
            client.sql().execute(null, dropIndexSql);
            System.out.println("Cleaned up unused index");
            
        } catch (Exception e) {
            System.err.println("Failed to cleanup indexes: " + e.getMessage());
        }
    }
}
```

### Zone Management

#### Distribution Zone Operations

```java
public class ZoneManagement {
    
    // Create distribution zone with specific configuration
    public static void createEnterpriseZone(IgniteClient client) {
        try {
            ZoneDefinition zoneDefinition = ZoneDefinition.builder("enterprise_zone")
                .ifNotExists()
                .partitions(32)              // Higher partition count for scalability
                .replicas(3)                 // 3 replicas for high availability
                .dataNodesAutoAdjust(120)    // Auto-adjust timeout
                .dataNodesAutoAdjustScaleUp(1)   // Scale up delay
                .dataNodesAutoAdjustScaleDown(300)  // Scale down delay (5 minutes)
                .storageProfiles("default")
                .build();
            
            client.catalog().createZone(zoneDefinition);
            System.out.println("Enterprise zone created with high availability settings");
            
        } catch (Exception e) {
            System.err.println("Failed to create enterprise zone: " + e.getMessage());
        }
    }
    
    // Monitor zone configuration
    public static void monitorZones(IgniteClient client) {
        try {
            Collection<String> zones = client.catalog().zones();
            
            System.out.println("=== Zone Configuration Report ===");
            
            for (String zoneName : zones) {
                ZoneDefinition zoneDef = client.catalog().zoneDefinition(zoneName);
                
                System.out.println("Zone: " + zoneName);
                System.out.println("  Partitions: " + zoneDef.partitions());
                System.out.println("  Replicas: " + zoneDef.replicas());
                System.out.println("  Storage Profiles: " + zoneDef.storageProfiles());
                System.out.println("  Auto-adjust: " + zoneDef.dataNodesAutoAdjust());
                System.out.println();
            }
            
        } catch (Exception e) {
            System.err.println("Failed to monitor zones: " + e.getMessage());
        }
    }
}
```

## Schema Migration Patterns

### Version-Based Migration System

```java
public class SchemaMigration {
    
    public static class MigrationVersion {
        private final int version;
        private final String description;
        private final Runnable migration;
        
        public MigrationVersion(int version, String description, Runnable migration) {
            this.version = version;
            this.description = description;
            this.migration = migration;
        }
        
        // Getters...
    }
    
    // Migration registry
    private static final List<MigrationVersion> MIGRATIONS = Arrays.asList(
        new MigrationVersion(1, "Initial schema", () -> createInitialSchema()),
        new MigrationVersion(2, "Add customer indexes", () -> addCustomerIndexes()),
        new MigrationVersion(3, "Create audit table", () -> createAuditTable())
    );
    
    // Execute migrations
    public static void migrate(IgniteClient client) {
        int currentVersion = getCurrentSchemaVersion(client);
        System.out.println("Current schema version: " + currentVersion);
        
        for (MigrationVersion migration : MIGRATIONS) {
            if (migration.version > currentVersion) {
                System.out.println("Applying migration " + migration.version + ": " + migration.description);
                
                try {
                    migration.migration.run();
                    updateSchemaVersion(client, migration.version);
                    System.out.println("Migration " + migration.version + " completed");
                    
                } catch (Exception e) {
                    System.err.println("Migration " + migration.version + " failed: " + e.getMessage());
                    throw new RuntimeException("Migration failed", e);
                }
            }
        }
        
        System.out.println("All migrations completed successfully");
    }
    
    // Get current schema version from metadata table
    private static int getCurrentSchemaVersion(IgniteClient client) {
        try {
            // Create schema_version table if it doesn't exist
            createSchemaVersionTable(client);
            
            var result = client.sql().execute(null, "SELECT MAX(version) as current_version FROM schema_version");
            if (result.hasNext()) {
                Integer version = result.next().intValue("current_version");
                return version != null ? version : 0;
            }
        } catch (Exception e) {
            System.out.println("Schema version table not found, starting from version 0");
        }
        return 0;
    }
    
    // Update schema version
    private static void updateSchemaVersion(IgniteClient client, int version) {
        try {
            client.sql().execute(null, 
                "INSERT INTO schema_version (version, applied_at) VALUES (?, CURRENT_TIMESTAMP)",
                version);
        } catch (Exception e) {
            System.err.println("Failed to update schema version: " + e.getMessage());
        }
    }
    
    // Create schema version tracking table
    private static void createSchemaVersionTable(IgniteClient client) {
        try {
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS schema_version (
                    version INTEGER NOT NULL,
                    applied_at TIMESTAMP NOT NULL,
                    PRIMARY KEY (version)
                )
                """;
            
            client.sql().execute(null, createTableSql);
            
        } catch (Exception e) {
            System.err.println("Failed to create schema_version table: " + e.getMessage());
        }
    }
    
    // Example migration methods
    private static void createInitialSchema() {
        // Implementation for initial schema creation
        System.out.println("Creating initial schema...");
    }
    
    private static void addCustomerIndexes() {
        // Implementation for adding customer indexes
        System.out.println("Adding customer indexes...");
    }
    
    private static void createAuditTable() {
        // Implementation for creating audit table
        System.out.println("Creating audit table...");
    }
}
```

## Best Practices for Schema Management

### 1. Schema Validation

```java
public class SchemaValidation {
    
    // Validate schema before deployment
    public static boolean validateSchema(IgniteClient client, Class<?>... tableClasses) {
        boolean isValid = true;
        
        for (Class<?> tableClass : tableClasses) {
            if (!validateTableClass(tableClass)) {
                isValid = false;
            }
        }
        
        return isValid;
    }
    
    private static boolean validateTableClass(Class<?> tableClass) {
        // Check for @Table annotation
        if (!tableClass.isAnnotationPresent(Table.class)) {
            System.err.println("Class " + tableClass.getSimpleName() + " missing @Table annotation");
            return false;
        }
        
        // Check for @Id fields
        boolean hasIdField = Arrays.stream(tableClass.getDeclaredFields())
            .anyMatch(field -> field.isAnnotationPresent(Id.class));
        
        if (!hasIdField) {
            System.err.println("Class " + tableClass.getSimpleName() + " missing @Id field");
            return false;
        }
        
        // Additional validation logic...
        return true;
    }
}
```

### 2. Environment-Specific Configuration

```java
public class EnvironmentConfig {
    
    public enum Environment {
        DEVELOPMENT(1, 1),
        TESTING(2, 2),
        PRODUCTION(32, 3);
        
        private final int partitions;
        private final int replicas;
        
        Environment(int partitions, int replicas) {
            this.partitions = partitions;
            this.replicas = replicas;
        }
        
        public int getPartitions() { return partitions; }
        public int getReplicas() { return replicas; }
    }
    
    // Create zone based on environment
    public static void createEnvironmentZone(IgniteClient client, Environment env) {
        ZoneDefinition zoneDef = ZoneDefinition.builder("app_zone")
            .ifNotExists()
            .partitions(env.getPartitions())
            .replicas(env.getReplicas())
            .storageProfiles("default")
            .build();
        
        try {
            client.catalog().createZone(zoneDef);
            System.out.println("Created zone for " + env + " environment");
        } catch (Exception e) {
            System.err.println("Failed to create zone: " + e.getMessage());
        }
    }
}
```

### 3. Schema Documentation Generation

```java
public class SchemaDocumentation {
    
    // Generate comprehensive schema documentation
    public static void generateSchemaDoc(IgniteClient client, String outputPath) {
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write("# Database Schema Documentation\n\n");
            writer.write("Generated on: " + LocalDateTime.now() + "\n\n");
            
            // Document zones
            writer.write("## Distribution Zones\n\n");
            for (String zoneName : client.catalog().zones()) {
                documentZone(writer, client, zoneName);
            }
            
            // Document tables
            writer.write("## Tables\n\n");
            for (String tableName : client.catalog().tables()) {
                documentTable(writer, client, tableName);
            }
            
            System.out.println("Schema documentation generated: " + outputPath);
            
        } catch (Exception e) {
            System.err.println("Failed to generate schema documentation: " + e.getMessage());
        }
    }
    
    private static void documentZone(FileWriter writer, IgniteClient client, String zoneName) 
            throws Exception {
        ZoneDefinition zoneDef = client.catalog().zoneDefinition(zoneName);
        
        writer.write("### Zone: " + zoneName + "\n");
        writer.write("- **Partitions:** " + zoneDef.partitions() + "\n");
        writer.write("- **Replicas:** " + zoneDef.replicas() + "\n");
        writer.write("- **Storage Profiles:** " + zoneDef.storageProfiles() + "\n\n");
    }
    
    private static void documentTable(FileWriter writer, IgniteClient client, String tableName) 
            throws Exception {
        TableDefinition tableDef = client.catalog().tableDefinition(tableName);
        
        writer.write("### Table: " + tableName + "\n");
        writer.write("**Zone:** " + tableDef.zone() + "\n\n");
        
        writer.write("**Columns:**\n");
        for (ColumnDefinition column : tableDef.columns()) {
            writer.write(String.format("- `%s` %s%s\n", 
                column.name(),
                column.type(),
                column.nullable() ? "" : " NOT NULL"));
        }
        
        writer.write("\n**Primary Key:** " + tableDef.primaryKey() + "\n\n");
    }
}
```
