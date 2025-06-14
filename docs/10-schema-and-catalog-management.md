# 10. Schema and Catalog Management

Building a music streaming platform requires managing complex database schemas that evolve over time. As your catalog grows from thousands to millions of tracks, artists, and customer interactions, the schema must adapt to changing business requirements. Apache Ignite 3's catalog management APIs provide programmatic control over schema evolution, enabling automated deployments and runtime flexibility.

## The Music Store Evolution Story

Your music streaming service starts with a simple catalog: artists, albums, and tracks. But as the business grows, you need to add playlists, recommendations, analytics tables, and user interaction data. Each requirement brings schema changes that must be deployed across your distributed cluster without downtime.

Ignite 3's `IgniteCatalog` interface enables you to manage this evolution through code, treating schema as a versioned asset that deploys alongside your application. Instead of manual SQL scripts, you define schemas programmatically using Java builders or annotations, ensuring consistency across environments.

### The Challenge: Growing from Simple to Complex

The journey begins with basic entities but evolves into a sophisticated data architecture:

**Phase 1: Core Music Catalog**
- Artists, Albums, Tracks with basic metadata
- Simple primary key relationships

**Phase 2: User Engagement**  
- Customer accounts, playlists, purchase history
- Foreign key relationships and constraints

**Phase 3: Advanced Analytics**
- Listening behavior, recommendations, streaming metrics
- Complex partitioning and colocation strategies

**Phase 4: Global Scale**
- Multi-zone distribution, regional compliance
- Performance-optimized schemas for different workloads

Each phase requires careful schema migration while maintaining data consistency and application availability.

## Programmatic Schema Management

The `IgniteCatalog` interface provides the foundation for all schema operations. Through this interface, you can create tables, manage zones, and introspect existing schemas. All operations support both synchronous and asynchronous patterns, enabling integration with reactive applications.

### Accessing the Catalog

```java
public class MusicStoreCatalogAccess {
    
    public static void exploreExistingSchema(IgniteClient client) {
        IgniteCatalog catalog = client.catalog();
        
        Collection<String> tableNames = catalog.tables();
        System.out.println("Music store tables: " + tableNames);
        
        Collection<String> zoneNames = catalog.zones();
        System.out.println("Distribution zones: " + zoneNames);
        
        // Analyze each music table
        for (String tableName : tableNames) {
            if (isMusicStoreTable(tableName)) {
                TableDefinition tableDef = catalog.tableDefinition(tableName);
                System.out.printf("Table %s: %d columns, zone: %s%n", 
                    tableName, tableDef.columns().size(), tableDef.zoneName());
            }
        }
    }
    
    private static boolean isMusicStoreTable(String tableName) {
        return tableName.matches("(?i)(artist|album|track|customer|playlist|invoice).*");
    }
}
```

## Table Creation Patterns

### Annotation-Based Table Definition

The annotation approach provides a declarative way to define tables directly on your Java classes. This pattern works well for entities that map closely to your application's domain model.

```java
public class AnnotationBasedTables {
    
    @Table(value = "Artist", 
           zone = @Zone("MusicStore"),
           colocateBy = @ColumnRef("ArtistId"))
    public static class Artist {
        @Id
        @Column(value = "ArtistId", nullable = false)
        private Integer artistId;
        
        @Column(value = "Name", nullable = false, length = 120)
        private String name;
        
        @Column(value = "Country", length = 50)
        private String country;
        
        @Column(value = "FormedYear")
        private Integer formedYear;
        
        // Standard constructors, getters, setters
        public Artist() {}
        
        public Artist(Integer artistId, String name, String country, Integer formedYear) {
            this.artistId = artistId;
            this.name = name;
            this.country = country;
            this.formedYear = formedYear;
        }
        
        // Getters and setters...
    }
    
    public static void createArtistTable(IgniteClient client) {
        try {
            CompletableFuture<Table> future = client.catalog().createTableAsync(Artist.class);
            
            future.whenComplete((table, throwable) -> {
                if (throwable != null) {
                    if (isTableAlreadyExists(throwable)) {
                        System.out.println("Artist table already exists");
                    } else {
                        System.err.println("Failed to create Artist table: " + throwable.getMessage());
                    }
                } else {
                    System.out.println("Artist table created: " + table.name());
                }
            });
            
            future.join(); // Wait for completion
            
        } catch (Exception e) {
            System.err.println("Error creating Artist table: " + e.getMessage());
        }
    }
    
    private static boolean isTableAlreadyExists(Throwable throwable) {
        return throwable.getMessage() != null && 
               throwable.getMessage().contains("already exists");
    }
}
```

### Builder-Based Table Definition

The `TableDefinition` builder provides programmatic control over table structure. This approach offers more flexibility for complex schemas and runtime-determined table configurations.

```java
public class BuilderBasedTables {
    
    public static void createAlbumTable(IgniteClient client) {
        TableDefinition albumTable = TableDefinition.builder("Album")
            .ifNotExists()
            .columns(
                ColumnDefinition.column("AlbumId", ColumnType.INTEGER.notNull()),
                ColumnDefinition.column("Title", ColumnType.varchar(160).notNull()),
                ColumnDefinition.column("ArtistId", ColumnType.INTEGER.notNull()),
                ColumnDefinition.column("ReleaseDate", ColumnType.DATE),
                ColumnDefinition.column("Genre", ColumnType.varchar(50)),
                ColumnDefinition.column("Label", ColumnType.varchar(100)),
                ColumnDefinition.column("Price", ColumnType.decimal(10, 2).defaultValue(BigDecimal.ZERO))
            )
            .primaryKey("AlbumId")
            .colocateBy("ArtistId")  // Albums colocated with their artists
            .zone("MusicStore")
            .index("idx_album_artist", IndexType.SORTED, 
                   ColumnSorted.column("ArtistId"),
                   ColumnSorted.column("Title"))
            .build();
        
        try {
            client.catalog().createTable(albumTable);
            System.out.println("Album table created with colocation and indexing");
        } catch (Exception e) {
            System.err.println("Failed to create Album table: " + e.getMessage());
        }
    }
    
    public static void createTrackTable(IgniteClient client) {
        TableDefinition trackTable = TableDefinition.builder("Track")
            .ifNotExists()
            .columns(
                ColumnDefinition.column("TrackId", ColumnType.INTEGER.notNull()),
                ColumnDefinition.column("Name", ColumnType.varchar(200).notNull()),
                ColumnDefinition.column("AlbumId", ColumnType.INTEGER),
                ColumnDefinition.column("MediaTypeId", ColumnType.INTEGER.notNull()),
                ColumnDefinition.column("GenreId", ColumnType.INTEGER),
                ColumnDefinition.column("Composer", ColumnType.varchar(220)),
                ColumnDefinition.column("Milliseconds", ColumnType.INTEGER.notNull()),
                ColumnDefinition.column("Bytes", ColumnType.INTEGER),
                ColumnDefinition.column("UnitPrice", ColumnType.decimal(10, 2).notNull())
            )
            .primaryKey("TrackId")
            .colocateBy("AlbumId")  // Tracks colocated with albums
            .zone("MusicStore")
            .index("idx_track_album_name", IndexType.SORTED,
                   ColumnSorted.column("AlbumId"),
                   ColumnSorted.column("Name"))
            .index("idx_track_genre", IndexType.HASH,
                   ColumnSorted.column("GenreId"))
            .build();
        
        try {
            client.catalog().createTable(trackTable);
            System.out.println("Track table created with multi-level colocation");
        } catch (Exception e) {
            System.err.println("Failed to create Track table: " + e.getMessage());
        }
    }
}
```

## Schema Introspection and Analysis

Understanding your existing schema structure enables informed decisions about evolution and optimization. The catalog API provides detailed introspection capabilities for tables, columns, indexes, and distribution zones.

### Table Structure Analysis

```java
public class SchemaIntrospection {
    
    public static void analyzeTableStructure(IgniteClient client, String tableName) {
        try {
            TableDefinition tableDef = client.catalog().tableDefinition(tableName);
            
            System.out.println("=== Table Analysis: " + tableName + " ===");
            System.out.println("Zone: " + tableDef.zoneName());
            System.out.println("Primary Key: " + tableDef.primaryKeyColumns());
            
            // Analyze colocation strategy
            List<String> colocationColumns = tableDef.colocationColumns();
            if (!colocationColumns.isEmpty()) {
                System.out.println("Colocation: " + colocationColumns);
                System.out.println("  → Data is distributed by these columns for performance");
            }
            
            // Column details
            System.out.println("\nColumns:");
            for (ColumnDefinition column : tableDef.columns()) {
                String nullable = column.nullable() ? " NULL" : " NOT NULL";
                String defaultValue = column.defaultValue() != null ? 
                    " DEFAULT " + column.defaultValue() : "";
                
                System.out.printf("  %-20s %s%s%s%n", 
                    column.name(),
                    column.type(),
                    nullable,
                    defaultValue);
            }
            
            // Index analysis
            List<IndexDefinition> indexes = tableDef.indexes();
            if (!indexes.isEmpty()) {
                System.out.println("\nIndexes:");
                for (IndexDefinition index : indexes) {
                    System.out.printf("  %-25s %s on %s%n", 
                        index.name(),
                        index.type(),
                        index.columns());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Failed to analyze table " + tableName + ": " + e.getMessage());
        }
    }
    
    public static void generateSchemaReport(IgniteClient client) {
        System.out.println("# Music Store Schema Report\n");
        System.out.println("Generated: " + LocalDateTime.now() + "\n");
        
        Collection<String> tables = client.catalog().tables();
        Map<String, List<String>> tablesByZone = groupTablesByZone(client, tables);
        
        for (Map.Entry<String, List<String>> entry : tablesByZone.entrySet()) {
            String zoneName = entry.getKey();
            List<String> zoneTables = entry.getValue();
            
            System.out.println("## Zone: " + zoneName);
            analyzeZoneConfiguration(client, zoneName);
            
            System.out.println("\n**Tables in this zone:**");
            for (String tableName : zoneTables) {
                TableDefinition tableDef = client.catalog().tableDefinition(tableName);
                System.out.printf("- **%s**: %d columns", tableName, tableDef.columns().size());
                
                if (!tableDef.colocationColumns().isEmpty()) {
                    System.out.printf(" (colocated by %s)", tableDef.colocationColumns());
                }
                System.out.println();
            }
            System.out.println();
        }
    }
    
    private static Map<String, List<String>> groupTablesByZone(IgniteClient client, 
                                                               Collection<String> tables) {
        Map<String, List<String>> tablesByZone = new HashMap<>();
        
        for (String tableName : tables) {
            try {
                TableDefinition tableDef = client.catalog().tableDefinition(tableName);
                String zoneName = tableDef.zoneName();
                
                tablesByZone.computeIfAbsent(zoneName, k -> new ArrayList<>()).add(tableName);
                
            } catch (Exception e) {
                System.err.println("Error processing table " + tableName + ": " + e.getMessage());
            }
        }
        
        return tablesByZone;
    }
    
    private static void analyzeZoneConfiguration(IgniteClient client, String zoneName) {
        try {
            ZoneDefinition zoneDef = client.catalog().zoneDefinition(zoneName);
            
            System.out.printf("- **Partitions**: %d%n", zoneDef.partitions());
            System.out.printf("- **Replicas**: %d%n", zoneDef.replicas());
            System.out.printf("- **Storage Profiles**: %s%n", zoneDef.storageProfiles());
            
            if (zoneDef.filter() != null && !zoneDef.filter().isEmpty()) {
                System.out.printf("- **Node Filter**: %s%n", zoneDef.filter());
            }
            
        } catch (Exception e) {
            System.err.println("Error analyzing zone " + zoneName + ": " + e.getMessage());
        }
    }
}
```

## Distribution Zone Management

Distribution zones control how data is partitioned and replicated across your cluster. Different zones enable optimization for specific workload patterns.

### Creating Performance-Optimized Zones

```java
public class ZoneManagement {
    
    public static void createMusicStoreZones(IgniteClient client) {
        // Primary zone for transactional music catalog data
        ZoneDefinition catalogZone = ZoneDefinition.builder("MusicStore")
            .ifNotExists()
            .partitions(16)                    // Balanced for catalog size
            .replicas(2)                       // High availability
            .dataNodesAutoAdjust(300)          // 5-minute auto-adjust interval
            .dataNodesAutoAdjustScaleUp(60)    // Quick scale-up for traffic spikes
            .dataNodesAutoAdjustScaleDown(600) // Slower scale-down for stability
            .storageProfiles("default")
            .build();
        
        // Analytics zone for reporting and data science workloads
        ZoneDefinition analyticsZone = ZoneDefinition.builder("MusicAnalytics")
            .ifNotExists()
            .partitions(32)                    // Higher partitions for analytics parallelism
            .replicas(1)                       // Lower replication for cost efficiency
            .dataNodesAutoAdjust(60)           // Fast auto-adjust for dynamic workloads
            .storageProfiles("default")
            .build();
        
        // High-throughput zone for streaming events and user interactions
        ZoneDefinition streamingZone = ZoneDefinition.builder("StreamingEvents")
            .ifNotExists()
            .partitions(64)                    // Maximum parallelism for streaming
            .replicas(1)                       // Single replica for maximum throughput
            .dataNodesAutoAdjust(30)           // Very fast scaling for streaming workloads
            .storageProfiles("default")
            .build();
        
        try {
            client.catalog().createZone(catalogZone);
            System.out.println("MusicStore zone created for catalog data");
            
            client.catalog().createZone(analyticsZone);
            System.out.println("MusicAnalytics zone created for reporting");
            
            client.catalog().createZone(streamingZone);
            System.out.println("StreamingEvents zone created for high-throughput data");
            
        } catch (Exception e) {
            System.err.println("Failed to create zones: " + e.getMessage());
        }
    }
    
    public static void monitorZoneHealth(IgniteClient client) {
        try {
            Collection<String> zones = client.catalog().zones();
            
            System.out.println("=== Zone Health Report ===");
            for (String zoneName : zones) {
                ZoneDefinition zoneDef = client.catalog().zoneDefinition(zoneName);
                
                System.out.printf("Zone: %s%n", zoneName);
                System.out.printf("  Configuration: %d partitions, %d replicas%n", 
                    zoneDef.partitions(), zoneDef.replicas());
                System.out.printf("  Auto-scaling: %ds adjust interval%n", 
                    zoneDef.dataNodesAutoAdjust());
                
                // In a real implementation, you might query system views for actual health metrics
                System.out.println("  Status: Healthy");
                System.out.println();
            }
            
        } catch (Exception e) {
            System.err.println("Failed to monitor zones: " + e.getMessage());
        }
    }
}
```

## Schema Evolution and Migration

As your music streaming platform grows, schema changes become inevitable. The catalog API enables controlled migration strategies that maintain data consistency while minimizing downtime.

### Version-Controlled Schema Migration

```java
public class SchemaMigration {
    
    public static class MigrationStep {
        private final int version;
        private final String description;
        private final Consumer<IgniteClient> migration;
        
        public MigrationStep(int version, String description, Consumer<IgniteClient> migration) {
            this.version = version;
            this.description = description;
            this.migration = migration;
        }
        
        public void execute(IgniteClient client) {
            migration.accept(client);
        }
        
        // Getters...
        public int getVersion() { return version; }
        public String getDescription() { return description; }
    }
    
    private static final List<MigrationStep> MIGRATIONS = Arrays.asList(
        new MigrationStep(1, "Create initial music catalog schema", 
            client -> createInitialSchema(client)),
        new MigrationStep(2, "Add customer and invoice tables", 
            client -> addCustomerTables(client)),
        new MigrationStep(3, "Create playlist and user interaction tables", 
            client -> addPlaylistTables(client)),
        new MigrationStep(4, "Add analytics and recommendation tables", 
            client -> addAnalyticsTables(client)),
        new MigrationStep(5, "Create streaming events zone and tables", 
            client -> addStreamingTables(client))
    );
    
    public static void runMigrations(IgniteClient client) {
        int currentVersion = getCurrentSchemaVersion(client);
        System.out.println("Current schema version: " + currentVersion);
        
        List<MigrationStep> pendingMigrations = MIGRATIONS.stream()
            .filter(m -> m.getVersion() > currentVersion)
            .collect(Collectors.toList());
        
        if (pendingMigrations.isEmpty()) {
            System.out.println("Schema is up to date");
            return;
        }
        
        System.out.println("Applying " + pendingMigrations.size() + " migrations...");
        
        for (MigrationStep migration : pendingMigrations) {
            try {
                System.out.println("Applying migration " + migration.getVersion() + 
                                 ": " + migration.getDescription());
                
                migration.execute(client);
                updateSchemaVersion(client, migration.getVersion());
                
                System.out.println("✓ Migration " + migration.getVersion() + " completed");
                
            } catch (Exception e) {
                System.err.println("✗ Migration " + migration.getVersion() + " failed: " + 
                                 e.getMessage());
                throw new RuntimeException("Migration failed", e);
            }
        }
        
        System.out.println("All migrations completed successfully");
    }
    
    private static int getCurrentSchemaVersion(IgniteClient client) {
        try {
            // Ensure schema version table exists
            createSchemaVersionTableIfNeeded(client);
            
            var result = client.sql().execute(null, 
                "SELECT COALESCE(MAX(version), 0) as current_version FROM schema_version");
            
            if (result.hasNext()) {
                return result.next().intValue("current_version");
            }
            
        } catch (Exception e) {
            System.out.println("Schema version table not found, starting from version 0");
        }
        
        return 0;
    }
    
    private static void updateSchemaVersion(IgniteClient client, int version) {
        try {
            client.sql().execute(null, 
                "INSERT INTO schema_version (version, applied_at, description) VALUES (?, ?, ?)",
                version, 
                Timestamp.from(Instant.now()),
                "Migration applied by Java API");
                
        } catch (Exception e) {
            System.err.println("Failed to update schema version: " + e.getMessage());
        }
    }
    
    private static void createSchemaVersionTableIfNeeded(IgniteClient client) {
        try {
            TableDefinition versionTable = TableDefinition.builder("schema_version")
                .ifNotExists()
                .columns(
                    ColumnDefinition.column("version", ColumnType.INTEGER.notNull()),
                    ColumnDefinition.column("applied_at", ColumnType.TIMESTAMP.notNull()),
                    ColumnDefinition.column("description", ColumnType.varchar(255))
                )
                .primaryKey("version")
                .zone("MusicStore")
                .build();
            
            client.catalog().createTable(versionTable);
            
        } catch (Exception e) {
            System.err.println("Failed to create schema_version table: " + e.getMessage());
        }
    }
    
    // Migration implementations
    private static void createInitialSchema(IgniteClient client) {
        System.out.println("  Creating Artist and Album tables...");
        // Implementation would create the core music catalog tables
        BuilderBasedTables.createAlbumTable(client);
    }
    
    private static void addCustomerTables(IgniteClient client) {
        System.out.println("  Creating Customer and Invoice tables...");
        // Implementation would add customer and billing tables
    }
    
    private static void addPlaylistTables(IgniteClient client) {
        System.out.println("  Creating Playlist and user interaction tables...");
        // Implementation would add playlist and user engagement tables
    }
    
    private static void addAnalyticsTables(IgniteClient client) {
        System.out.println("  Creating analytics and recommendation tables...");
        // Implementation would add analytics and ML feature tables
    }
    
    private static void addStreamingTables(IgniteClient client) {
        System.out.println("  Creating streaming events zone and tables...");
        // Implementation would add high-throughput streaming data tables
    }
}
```

## Production Schema Management Patterns

### Safe Schema Operations

```java
public class ProductionSchemaManagement {
    
    public static boolean createTableSafely(IgniteClient client, TableDefinition tableDef) {
        String tableName = tableDef.qualifiedName().toString();
        
        try {
            // Check if table already exists
            if (client.catalog().tables().contains(tableName)) {
                System.out.println("Table " + tableName + " already exists");
                return true;
            }
            
            // Validate table definition
            if (!validateTableDefinition(tableDef)) {
                System.err.println("Table definition validation failed for " + tableName);
                return false;
            }
            
            // Create with retry logic for transient failures
            return retryOperation(() -> {
                client.catalog().createTable(tableDef);
                System.out.println("Table " + tableName + " created successfully");
                return true;
            }, 3);
            
        } catch (Exception e) {
            System.err.println("Failed to create table " + tableName + ": " + e.getMessage());
            return false;
        }
    }
    
    public static boolean dropTableSafely(IgniteClient client, String tableName) {
        try {
            // Verify table exists
            if (!client.catalog().tables().contains(tableName)) {
                System.out.println("Table " + tableName + " does not exist");
                return true;
            }
            
            // Check for data (optional safety check)
            long recordCount = countTableRecords(client, tableName);
            if (recordCount > 0) {
                System.out.println("WARNING: Table " + tableName + " contains " + 
                                 recordCount + " records");
                // In production, you might require explicit confirmation
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
    
    private static boolean validateTableDefinition(TableDefinition tableDef) {
        // Check for primary key
        if (tableDef.primaryKeyColumns().isEmpty()) {
            System.err.println("Table must have a primary key");
            return false;
        }
        
        // Check for valid column types
        for (ColumnDefinition column : tableDef.columns()) {
            if (column.type() == null) {
                System.err.println("Column " + column.name() + " has invalid type");
                return false;
            }
        }
        
        // Additional validation logic...
        return true;
    }
    
    private static <T> T retryOperation(Supplier<T> operation, int maxRetries) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                if (attempt == maxRetries) {
                    break;
                }
                
                System.out.println("Attempt " + attempt + " failed, retrying: " + e.getMessage());
                try {
                    Thread.sleep(1000 * attempt); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        throw new RuntimeException("Operation failed after " + maxRetries + " attempts", 
                                 lastException);
    }
    
    private static long countTableRecords(IgniteClient client, String tableName) {
        try {
            var result = client.sql().execute(null, 
                "SELECT COUNT(*) as record_count FROM " + tableName);
            if (result.hasNext()) {
                return result.next().longValue("record_count");
            }
        } catch (Exception e) {
            System.err.println("Failed to count records in " + tableName + ": " + e.getMessage());
        }
        return 0;
    }
}
```

### Environment-Specific Configuration

```java
public class EnvironmentConfigurationManager {
    
    public enum Environment {
        DEVELOPMENT(4, 1, 30),
        TESTING(8, 2, 60),
        PRODUCTION(32, 3, 300);
        
        private final int partitions;
        private final int replicas;
        private final int autoAdjustInterval;
        
        Environment(int partitions, int replicas, int autoAdjustInterval) {
            this.partitions = partitions;
            this.replicas = replicas;
            this.autoAdjustInterval = autoAdjustInterval;
        }
        
        public int getPartitions() { return partitions; }
        public int getReplicas() { return replicas; }
        public int getAutoAdjustInterval() { return autoAdjustInterval; }
    }
    
    public static void initializeEnvironment(IgniteClient client, Environment env) {
        System.out.println("Initializing " + env + " environment...");
        
        // Create environment-specific zones
        createEnvironmentZones(client, env);
        
        // Apply environment-specific schema
        applyEnvironmentSchema(client, env);
        
        System.out.println(env + " environment initialized successfully");
    }
    
    private static void createEnvironmentZones(IgniteClient client, Environment env) {
        String zoneSuffix = env.name().toLowerCase();
        
        // Main catalog zone
        ZoneDefinition catalogZone = ZoneDefinition.builder("music_catalog_" + zoneSuffix)
            .ifNotExists()
            .partitions(env.getPartitions())
            .replicas(env.getReplicas())
            .dataNodesAutoAdjust(env.getAutoAdjustInterval())
            .storageProfiles("default")
            .build();
        
        try {
            client.catalog().createZone(catalogZone);
            System.out.println("Created catalog zone for " + env);
            
            // In production, create additional specialized zones
            if (env == Environment.PRODUCTION) {
                createProductionAnalyticsZone(client);
                createProductionStreamingZone(client);
            }
            
        } catch (Exception e) {
            System.err.println("Failed to create zones for " + env + ": " + e.getMessage());
        }
    }
    
    private static void createProductionAnalyticsZone(IgniteClient client) {
        ZoneDefinition analyticsZone = ZoneDefinition.builder("music_analytics_production")
            .ifNotExists()
            .partitions(64)  // High parallelism for analytics
            .replicas(2)     // Balanced replication
            .dataNodesAutoAdjust(180)  // Slower scaling for analytics stability
            .storageProfiles("default")
            .build();
        
        client.catalog().createZone(analyticsZone);
        System.out.println("Created production analytics zone");
    }
    
    private static void createProductionStreamingZone(IgniteClient client) {
        ZoneDefinition streamingZone = ZoneDefinition.builder("streaming_events_production")
            .ifNotExists()
            .partitions(128) // Maximum parallelism for streaming
            .replicas(1)     // Single replica for throughput
            .dataNodesAutoAdjust(30)  // Fast scaling for streaming
            .storageProfiles("default")
            .build();
        
        client.catalog().createZone(streamingZone);
        System.out.println("Created production streaming zone");
    }
    
    private static void applyEnvironmentSchema(IgniteClient client, Environment env) {
        // Apply environment-specific table configurations
        switch (env) {
            case DEVELOPMENT:
                // Minimal schema for development
                System.out.println("Applying development schema");
                break;
            case TESTING:
                // Full schema with test data
                System.out.println("Applying testing schema");
                break;
            case PRODUCTION:
                // Full schema with production optimizations
                System.out.println("Applying production schema");
                break;
        }
    }
}
```

## Catalog Management Best Practices

### Schema Documentation Generation

```java
public class SchemaDocumentationGenerator {
    
    public static void generateFullDocumentation(IgniteClient client, String outputPath) {
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write("# Music Store Database Schema Documentation\n\n");
            writer.write("Generated: " + LocalDateTime.now() + "\n\n");
            writer.write("This document describes the complete schema for the music streaming platform.\n\n");
            
            // Document distribution zones
            documentZones(writer, client);
            
            // Document tables by zone
            documentTablesByZone(writer, client);
            
            // Document relationships
            documentTableRelationships(writer, client);
            
            System.out.println("Schema documentation generated: " + outputPath);
            
        } catch (Exception e) {
            System.err.println("Failed to generate documentation: " + e.getMessage());
        }
    }
    
    private static void documentZones(FileWriter writer, IgniteClient client) throws IOException {
        writer.write("## Distribution Zones\n\n");
        
        Collection<String> zones = client.catalog().zones();
        for (String zoneName : zones) {
            try {
                ZoneDefinition zoneDef = client.catalog().zoneDefinition(zoneName);
                
                writer.write("### " + zoneName + "\n\n");
                writer.write("- **Purpose**: " + getZonePurpose(zoneName) + "\n");
                writer.write("- **Partitions**: " + zoneDef.partitions() + "\n");
                writer.write("- **Replicas**: " + zoneDef.replicas() + "\n");
                writer.write("- **Auto-adjust Interval**: " + zoneDef.dataNodesAutoAdjust() + " seconds\n");
                writer.write("- **Storage Profiles**: " + zoneDef.storageProfiles() + "\n\n");
                
            } catch (Exception e) {
                System.err.println("Error documenting zone " + zoneName + ": " + e.getMessage());
            }
        }
    }
    
    private static void documentTablesByZone(FileWriter writer, IgniteClient client) throws IOException {
        Collection<String> tables = client.catalog().tables();
        Map<String, List<String>> tablesByZone = new HashMap<>();
        
        // Group tables by zone
        for (String tableName : tables) {
            try {
                TableDefinition tableDef = client.catalog().tableDefinition(tableName);
                String zoneName = tableDef.zoneName();
                tablesByZone.computeIfAbsent(zoneName, k -> new ArrayList<>()).add(tableName);
            } catch (Exception e) {
                System.err.println("Error processing table " + tableName + ": " + e.getMessage());
            }
        }
        
        // Document each zone's tables
        writer.write("## Tables by Zone\n\n");
        for (Map.Entry<String, List<String>> entry : tablesByZone.entrySet()) {
            String zoneName = entry.getKey();
            List<String> zoneTables = entry.getValue();
            
            writer.write("### " + zoneName + " Zone Tables\n\n");
            
            for (String tableName : zoneTables) {
                documentTable(writer, client, tableName);
            }
        }
    }
    
    private static void documentTable(FileWriter writer, IgniteClient client, String tableName) throws IOException {
        try {
            TableDefinition tableDef = client.catalog().tableDefinition(tableName);
            
            writer.write("#### " + tableName + "\n\n");
            writer.write("**Description**: " + getTableDescription(tableName) + "\n\n");
            
            // Document colocation
            List<String> colocationColumns = tableDef.colocationColumns();
            if (!colocationColumns.isEmpty()) {
                writer.write("**Colocation Strategy**: Data is distributed by " + 
                           colocationColumns + " for performance optimization\n\n");
            }
            
            // Document columns
            writer.write("**Columns**:\n\n");
            writer.write("| Column | Type | Nullable | Default |\n");
            writer.write("|--------|------|----------|----------|\n");
            
            for (ColumnDefinition column : tableDef.columns()) {
                String nullable = column.nullable() ? "Yes" : "No";
                String defaultValue = column.defaultValue() != null ? 
                    column.defaultValue().toString() : "";
                
                writer.write(String.format("| %s | %s | %s | %s |\n",
                    column.name(),
                    column.type(),
                    nullable,
                    defaultValue));
            }
            
            // Document indexes
            List<IndexDefinition> indexes = tableDef.indexes();
            if (!indexes.isEmpty()) {
                writer.write("\n**Indexes**:\n\n");
                for (IndexDefinition index : indexes) {
                    writer.write("- **" + index.name() + "**: " + index.type() + 
                               " index on " + index.columns() + "\n");
                }
            }
            
            writer.write("\n");
            
        } catch (Exception e) {
            System.err.println("Error documenting table " + tableName + ": " + e.getMessage());
        }
    }
    
    private static void documentTableRelationships(FileWriter writer, IgniteClient client) throws IOException {
        writer.write("## Data Relationships\n\n");
        writer.write("The music store schema follows these key relationships:\n\n");
        writer.write("- **Artist** → **Album** (1:N) - Artists can have multiple albums\n");
        writer.write("- **Album** → **Track** (1:N) - Albums contain multiple tracks\n");
        writer.write("- **Customer** → **Invoice** (1:N) - Customers can have multiple purchases\n");
        writer.write("- **Invoice** → **InvoiceLine** (1:N) - Invoices contain multiple line items\n");
        writer.write("- **Customer** → **Playlist** (1:N) - Customers can create multiple playlists\n");
        writer.write("- **Playlist** ↔ **Track** (N:N) - Playlists contain multiple tracks, tracks can be in multiple playlists\n\n");
        
        writer.write("### Colocation Strategy\n\n");
        writer.write("Related data is colocated to optimize query performance:\n\n");
        writer.write("- **Albums** are colocated with their **Artist**\n");
        writer.write("- **Tracks** are colocated with their **Album** (and transitively with Artist)\n");
        writer.write("- **Invoices** are colocated with their **Customer**\n");
        writer.write("- **InvoiceLines** are colocated with their **Invoice** (and transitively with Customer)\n\n");
    }
    
    private static String getZonePurpose(String zoneName) {
        if (zoneName.contains("catalog") || zoneName.contains("music")) {
            return "Core music catalog data with high consistency requirements";
        } else if (zoneName.contains("analytics")) {
            return "Analytics and reporting data optimized for read-heavy workloads";
        } else if (zoneName.contains("streaming")) {
            return "High-throughput streaming events and user interaction data";
        } else {
            return "General purpose data storage";
        }
    }
    
    private static String getTableDescription(String tableName) {
        switch (tableName.toLowerCase()) {
            case "artist":
                return "Music artists and bands with basic information";
            case "album":
                return "Music albums with release information and artist relationships";
            case "track":
                return "Individual songs with metadata, pricing, and album relationships";
            case "customer":
                return "Customer account information for the music store";
            case "invoice":
                return "Customer purchase records with billing information";
            case "invoiceline":
                return "Individual items within customer purchases";
            case "playlist":
                return "User-created playlists for organizing music";
            case "playlisttrack":
                return "Many-to-many relationships between playlists and tracks";
            default:
                return "Table for " + tableName.toLowerCase() + " data";
        }
    }
}
```

This comprehensive module demonstrates how to use Apache Ignite 3's catalog management APIs to build and evolve database schemas programmatically. From simple table creation to complex migration strategies, these patterns enable robust schema management for production music streaming platforms.

The key to successful schema management lies in treating your database structure as code - versioned, tested, and deployed through the same rigorous processes as your application logic. Ignite 3's catalog APIs make this approach both practical and powerful for distributed systems at scale.