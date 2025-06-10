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
    
    // Define Artist table with annotations for Chinook music store
    @Table(
        zone = @Zone(value = "chinook_zone", storageProfiles = "default"),
        indexes = {
            @Index(value = "idx_artist_name", columns = { @ColumnRef("Name") }),
            @Index(value = "idx_artist_genre", columns = { @ColumnRef("primaryGenre") })
        }
    )
    public static class Artist {
        @Id
        @Column(value = "ArtistId", nullable = false)
        private Integer artistId;
        
        @Column(value = "Name", nullable = false, length = 120)
        private String name;
        
        @Column(value = "primary_genre", nullable = true, length = 50)
        private String primaryGenre;
        
        @Column(value = "formed_year", nullable = true)
        private Integer formedYear;
        
        @Column(value = "country", nullable = true, length = 50)
        private String country;
        
        @Column(value = "created_at", nullable = false)
        private LocalDateTime createdAt;
        
        // Constructors, getters, setters...
    }
    
    // Create Artist table with error handling
    public static void createArtistTable(IgniteClient client) {
        try {
            // Async table creation
            CompletableFuture<Void> future = client.catalog().createTableAsync(Artist.class);
            
            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    if (throwable.getCause() instanceof TableAlreadyExistsException) {
                        System.out.println("Artist table already exists - skipping creation");
                    } else {
                        System.err.println("Artist table creation failed: " + throwable.getMessage());
                    }
                } else {
                    System.out.println("Artist table created successfully");
                }
            });
            
            // Wait for completion (in production, handle asynchronously)
            future.join();
            
        } catch (Exception e) {
            System.err.println("Error creating Artist table: " + e.getMessage());
        }
    }
}
```

#### Manual Table Definition

```java
public class ManualTableDefinition {
    
    // Create Album table using TableDefinition API with colocation by ArtistId
    public static void createAlbumTable(IgniteClient client) {
        TableDefinition tableDefinition = TableDefinition.builder("Album")
            .ifNotExists()
            .columns(
                ColumnDefinition.builder("AlbumId", ColumnType.INT32).asNonNull().build(),
                ColumnDefinition.builder("Title", ColumnType.STRING).length(160).asNonNull().build(),
                ColumnDefinition.builder("ArtistId", ColumnType.INT32).asNonNull().build(),
                ColumnDefinition.builder("ReleaseDate", ColumnType.DATE).build(),
                ColumnDefinition.builder("Genre", ColumnType.STRING).length(50).build(),
                ColumnDefinition.builder("Label", ColumnType.STRING).length(100).build()
            )
            .primaryKey("AlbumId")
            .colocation("ArtistId")  // Colocate albums with their artists
            .zone("chinook_zone")
            .build();
        
        try {
            client.catalog().createTable(tableDefinition);
            System.out.println("Album table created with manual definition and colocation");
        } catch (Exception e) {
            System.err.println("Failed to create Album table: " + e.getMessage());
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
            // Create index for frequently queried album searches
            String createIndexSql = """
                CREATE INDEX IF NOT EXISTS idx_album_search 
                ON Album (Title, ArtistId, Genre)
                """;
            
            client.sql().execute(null, createIndexSql);
            System.out.println("Album search index created");
            
            // Create index for track searches by album
            String trackIndexSql = """
                CREATE INDEX IF NOT EXISTS idx_track_album 
                ON Track (AlbumId, Name)
                """;
            
            client.sql().execute(null, trackIndexSql);
            System.out.println("Track-Album index created");
            
            // Create partial index for rock genre albums
            String partialIndexSql = """
                CREATE INDEX IF NOT EXISTS idx_rock_albums 
                ON Album (Title, ArtistId) 
                WHERE Genre LIKE '%Rock%'
                """;
            
            client.sql().execute(null, partialIndexSql);
            System.out.println("Partial index for rock albums created");
            
        } catch (Exception e) {
            System.err.println("Failed to create indexes: " + e.getMessage());
        }
    }
    
    // Analyze index usage and performance
    public static void analyzeIndexPerformance(IgniteClient client, String tableName) {
        try {
            // Query to analyze index statistics for music searches
            String analyzeQuery = """
                EXPLAIN ANALYZE SELECT * FROM " + tableName + " 
                WHERE Title LIKE '%Rock%' AND ArtistId IN (1, 2, 3)
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
    
    // Create Chinook distribution zone with music-optimized configuration
    public static void createChinookZone(IgniteClient client) {
        try {
            ZoneDefinition zoneDefinition = ZoneDefinition.builder("chinook_zone")
                .ifNotExists()
                .partitions(16)              // Optimized for music catalog size
                .replicas(2)                 // 2 replicas for availability
                .dataNodesAutoAdjust(60)     // Faster auto-adjust for music workloads
                .dataNodesAutoAdjustScaleUp(1)   // Quick scale up for peak listening
                .dataNodesAutoAdjustScaleDown(180)  // Scale down delay (3 minutes)
                .storageProfiles("default")
                .build();
            
            client.catalog().createZone(zoneDefinition);
            System.out.println("Chinook zone created with music store optimizations");
            
            // Also create a separate zone for high-throughput streaming data
            ZoneDefinition streamingZone = ZoneDefinition.builder("chinook_streaming")
                .ifNotExists()
                .partitions(32)              // Higher partitions for streaming data
                .replicas(1)                 // Lower replication for performance
                .dataNodesAutoAdjust(30)     // Fast auto-adjust for streaming
                .storageProfiles("default")
                .build();
            
            client.catalog().createZone(streamingZone);
            System.out.println("Chinook streaming zone created for high-throughput data");
            
        } catch (Exception e) {
            System.err.println("Failed to create Chinook zones: " + e.getMessage());
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
    
    // Migration registry for Chinook music store
    private static final List<MigrationVersion> MIGRATIONS = Arrays.asList(
        new MigrationVersion(1, "Initial Chinook schema", () -> createInitialChinookSchema()),
        new MigrationVersion(2, "Add Artist performance indexes", () -> addArtistIndexes()),
        new MigrationVersion(3, "Create playlist and track tables", () -> createPlaylistTables()),
        new MigrationVersion(4, "Add Album colocation optimization", () -> optimizeAlbumColocation()),
        new MigrationVersion(5, "Create music analytics tables", () -> createAnalyticsTables())
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
    
    // Example migration methods for Chinook music store
    private static void createInitialChinookSchema() {
        // Implementation for initial Chinook schema creation
        System.out.println("Creating initial Chinook schema (Artist, Album tables)...");
    }
    
    private static void addArtistIndexes() {
        // Implementation for adding Artist performance indexes
        System.out.println("Adding Artist search and performance indexes...");
    }
    
    private static void createPlaylistTables() {
        // Implementation for creating Playlist and Track tables
        System.out.println("Creating Playlist, Track, and PlaylistTrack tables...");
    }
    
    private static void optimizeAlbumColocation() {
        // Implementation for Album-Artist colocation optimization
        System.out.println("Optimizing Album colocation by ArtistId...");
    }
    
    private static void createAnalyticsTables() {
        // Implementation for music analytics and reporting tables
        System.out.println("Creating music analytics and reporting tables...");
    }
}
```

## Complete Chinook Schema Example

### Creating the Full Music Store Schema

```java
public class ChinookSchemaManager {
    
    // Complete Chinook schema creation
    public static void createChinookSchema(IgniteClient client) {
        System.out.println("Creating complete Chinook music store schema...");
        
        try {
            // 1. Create zones first
            createChinookZones(client);
            
            // 2. Create core music tables
            createCoreMusicTables(client);
            
            // 3. Create customer and business tables
            createBusinessTables(client);
            
            // 4. Create performance indexes
            createPerformanceIndexes(client);
            
            System.out.println("Chinook schema creation completed successfully");
            
        } catch (Exception e) {
            System.err.println("Failed to create Chinook schema: " + e.getMessage());
            throw new RuntimeException("Schema creation failed", e);
        }
    }
    
    private static void createChinookZones(IgniteClient client) {
        // Main transactional zone for music catalog
        ZoneDefinition musicZone = ZoneDefinition.builder("chinook_music")
            .ifNotExists()
            .partitions(16)
            .replicas(2)
            .storageProfiles("default")
            .build();
        
        // Analytics zone for reporting and aggregations
        ZoneDefinition analyticsZone = ZoneDefinition.builder("chinook_analytics")
            .ifNotExists()
            .partitions(32)
            .replicas(1)  // Lower replication for analytics
            .storageProfiles("default")
            .build();
        
        client.catalog().createZone(musicZone);
        client.catalog().createZone(analyticsZone);
        
        System.out.println("Chinook zones created");
    }
    
    private static void createCoreMusicTables(IgniteClient client) {
        // Artist table (root of hierarchy)
        TableDefinition artistTable = TableDefinition.builder("Artist")
            .ifNotExists()
            .columns(
                ColumnDefinition.builder("ArtistId", ColumnType.INT32).asNonNull().build(),
                ColumnDefinition.builder("Name", ColumnType.STRING).length(120).asNonNull().build()
            )
            .primaryKey("ArtistId")
            .zone("chinook_music")
            .build();
        
        // Album table (colocated with Artist)
        TableDefinition albumTable = TableDefinition.builder("Album")
            .ifNotExists()
            .columns(
                ColumnDefinition.builder("AlbumId", ColumnType.INT32).asNonNull().build(),
                ColumnDefinition.builder("Title", ColumnType.STRING).length(160).asNonNull().build(),
                ColumnDefinition.builder("ArtistId", ColumnType.INT32).asNonNull().build()
            )
            .primaryKey("AlbumId")
            .colocation("ArtistId")  // Colocate with Artist
            .zone("chinook_music")
            .build();
        
        // Track table (colocated with Album/Artist)
        TableDefinition trackTable = TableDefinition.builder("Track")
            .ifNotExists()
            .columns(
                ColumnDefinition.builder("TrackId", ColumnType.INT32).asNonNull().build(),
                ColumnDefinition.builder("Name", ColumnType.STRING).length(200).asNonNull().build(),
                ColumnDefinition.builder("AlbumId", ColumnType.INT32).build(),
                ColumnDefinition.builder("MediaTypeId", ColumnType.INT32).asNonNull().build(),
                ColumnDefinition.builder("GenreId", ColumnType.INT32).build(),
                ColumnDefinition.builder("Composer", ColumnType.STRING).length(220).build(),
                ColumnDefinition.builder("Milliseconds", ColumnType.INT32).asNonNull().build(),
                ColumnDefinition.builder("Bytes", ColumnType.INT32).build(),
                ColumnDefinition.builder("UnitPrice", ColumnType.DECIMAL).precision(10).scale(2).asNonNull().build()
            )
            .primaryKey("TrackId")
            .colocation("AlbumId")  // Colocate with Album
            .zone("chinook_music")
            .build();
        
        // Create tables in dependency order
        client.catalog().createTable(artistTable);
        client.catalog().createTable(albumTable);
        client.catalog().createTable(trackTable);
        
        System.out.println("Core music tables created");
    }
    
    private static void createBusinessTables(IgniteClient client) {
        // Customer table
        TableDefinition customerTable = TableDefinition.builder("Customer")
            .ifNotExists()
            .columns(
                ColumnDefinition.builder("CustomerId", ColumnType.INT32).asNonNull().build(),
                ColumnDefinition.builder("FirstName", ColumnType.STRING).length(40).asNonNull().build(),
                ColumnDefinition.builder("LastName", ColumnType.STRING).length(20).asNonNull().build(),
                ColumnDefinition.builder("Email", ColumnType.STRING).length(60).asNonNull().build(),
                ColumnDefinition.builder("Country", ColumnType.STRING).length(40).build(),
                ColumnDefinition.builder("Phone", ColumnType.STRING).length(24).build()
            )
            .primaryKey("CustomerId")
            .zone("chinook_music")
            .build();
        
        // Invoice table (colocated with Customer)
        TableDefinition invoiceTable = TableDefinition.builder("Invoice")
            .ifNotExists()
            .columns(
                ColumnDefinition.builder("InvoiceId", ColumnType.INT32).asNonNull().build(),
                ColumnDefinition.builder("CustomerId", ColumnType.INT32).asNonNull().build(),
                ColumnDefinition.builder("InvoiceDate", ColumnType.TIMESTAMP).asNonNull().build(),
                ColumnDefinition.builder("BillingAddress", ColumnType.STRING).length(70).build(),
                ColumnDefinition.builder("BillingCity", ColumnType.STRING).length(40).build(),
                ColumnDefinition.builder("BillingCountry", ColumnType.STRING).length(40).build(),
                ColumnDefinition.builder("Total", ColumnType.DECIMAL).precision(10).scale(2).asNonNull().build()
            )
            .primaryKey("InvoiceId")
            .colocation("CustomerId")  // Colocate with Customer
            .zone("chinook_music")
            .build();
        
        // InvoiceLine table (colocated with Invoice/Customer)
        TableDefinition invoiceLineTable = TableDefinition.builder("InvoiceLine")
            .ifNotExists()
            .columns(
                ColumnDefinition.builder("InvoiceLineId", ColumnType.INT32).asNonNull().build(),
                ColumnDefinition.builder("InvoiceId", ColumnType.INT32).asNonNull().build(),
                ColumnDefinition.builder("TrackId", ColumnType.INT32).asNonNull().build(),
                ColumnDefinition.builder("UnitPrice", ColumnType.DECIMAL).precision(10).scale(2).asNonNull().build(),
                ColumnDefinition.builder("Quantity", ColumnType.INT32).asNonNull().build()
            )
            .primaryKey("InvoiceLineId")
            .colocation("InvoiceId")  // Colocate with Invoice
            .zone("chinook_music")
            .build();
        
        client.catalog().createTable(customerTable);
        client.catalog().createTable(invoiceTable);
        client.catalog().createTable(invoiceLineTable);
        
        System.out.println("Business tables created");
    }
    
    private static void createPerformanceIndexes(IgniteClient client) {
        // Artist search index
        client.sql().execute(null, 
            "CREATE INDEX IF NOT EXISTS idx_artist_name ON Artist (Name)");
        
        // Album search by artist and title
        client.sql().execute(null, 
            "CREATE INDEX IF NOT EXISTS idx_album_artist_title ON Album (ArtistId, Title)");
        
        // Track search by album and name
        client.sql().execute(null, 
            "CREATE INDEX IF NOT EXISTS idx_track_album_name ON Track (AlbumId, Name)");
        
        // Track search by genre for recommendations
        client.sql().execute(null, 
            "CREATE INDEX IF NOT EXISTS idx_track_genre ON Track (GenreId, UnitPrice)");
        
        // Customer email index for authentication
        client.sql().execute(null, 
            "CREATE INDEX IF NOT EXISTS idx_customer_email ON Customer (Email)");
        
        // Invoice date index for reporting
        client.sql().execute(null, 
            "CREATE INDEX IF NOT EXISTS idx_invoice_date ON Invoice (InvoiceDate, CustomerId)");
        
        // Invoice line track index for sales analytics
        client.sql().execute(null, 
            "CREATE INDEX IF NOT EXISTS idx_invoiceline_track ON InvoiceLine (TrackId, InvoiceId)");
        
        System.out.println("Performance indexes created");
    }
    
    // Validate schema after creation
    public static boolean validateChinookSchema(IgniteClient client) {
        String[] expectedTables = {
            "Artist", "Album", "Track", "Customer", "Invoice", "InvoiceLine"
        };
        
        Collection<String> actualTables = client.catalog().tables();
        
        for (String expectedTable : expectedTables) {
            if (!actualTables.contains(expectedTable)) {
                System.err.println("Missing table: " + expectedTable);
                return false;
            }
        }
        
        System.out.println("Chinook schema validation successful");
        return true;
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
    
    // Create Chinook zone based on environment
    public static void createChinookEnvironmentZone(IgniteClient client, Environment env) {
        ZoneDefinition zoneDef = ZoneDefinition.builder("chinook_" + env.name().toLowerCase())
            .ifNotExists()
            .partitions(env.getPartitions())
            .replicas(env.getReplicas())
            .storageProfiles("default")
            .build();
        
        try {
            client.catalog().createZone(zoneDef);
            System.out.println("Created Chinook zone for " + env + " environment");
            
            // In production, also create a separate analytics zone
            if (env == Environment.PRODUCTION) {
                ZoneDefinition analyticsZone = ZoneDefinition.builder("chinook_analytics")
                    .ifNotExists()
                    .partitions(64)  // Higher partitions for analytics workloads
                    .replicas(2)
                    .storageProfiles("default")
                    .build();
                
                client.catalog().createZone(analyticsZone);
                System.out.println("Created Chinook analytics zone for production");
            }
            
        } catch (Exception e) {
            System.err.println("Failed to create Chinook zone: " + e.getMessage());
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
            writer.write("# Chinook Music Store - Database Schema Documentation\n\n");
            writer.write("Generated on: " + LocalDateTime.now() + "\n\n");
            writer.write("This document describes the schema for the Chinook digital music store.\n\n");
            
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
