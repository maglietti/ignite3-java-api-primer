package com.apache.ignite.examples.catalog;

import org.apache.ignite.catalog.IgniteCatalog;
import org.apache.ignite.catalog.definitions.TableDefinition;
import org.apache.ignite.catalog.definitions.ZoneDefinition;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.ignite.catalog.ColumnType.*;
import static org.apache.ignite.catalog.definitions.ColumnDefinition.column;

/**
 * Schema operations using Catalog API for type safety and build-time validation.
 * 
 * Catalog API prevents runtime DDL errors through compile-time checking.
 * Zone assignment controls data placement across cluster nodes.
 * Colocation strategies reduce network overhead for related data access.
 * Index placement affects query performance and storage efficiency.
 * 
 * Production patterns:
 * - Use fluent builders for complex table definitions
 * - Apply colocation for parent-child relationships
 * - Create indexes after bulk data loading for efficiency
 */
public class SchemaOperations {

    private static final Logger logger = LoggerFactory.getLogger(SchemaOperations.class);
    
    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Schema Operations Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            runSchemaOperations(client);
            
        } catch (Exception e) {
            logger.error("Failed to run schema operations", e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void runSchemaOperations(IgniteClient client) {
        IgniteCatalog catalog = client.catalog();
        IgniteSql sql = client.sql();
        
        System.out.println();
        System.out.println("--- Schema Operations ---");
        
        // Demonstrate core schema operations
        demonstrateTableCreation(catalog);
        demonstrateIndexCreation(sql);
        demonstrateSchemaQuery(catalog, sql);
        demonstrateSchemaCleanup(catalog);
        
        System.out.println();
        System.out.println(">>> Schema operations completed successfully");
    }

    /**
     * Table colocation reduces distributed join costs.
     * 
     * Zone assignment controls physical data placement and replica count.
     * Composite primary keys enable range queries and partial key lookups.
     * Colocation by foreign key co-locates related records on same nodes.
     */
    private static void demonstrateTableCreation(IgniteCatalog catalog) {
        System.out.println();
        System.out.println("1. Table Creation with Zone Assignment:");
        
        try {
            // Zone creation establishes data placement policy
            System.out.println("    Creating demo zone for table operations");
            ZoneDefinition demoZone = ZoneDefinition.builder("DemoZone")
                .ifNotExists()
                .partitions(8)
                .replicas(2)
                .storageProfiles("default")
                .build();
            catalog.createZone(demoZone);
            System.out.println("    >>> Created DemoZone (8 partitions, 2 replicas)");
            
            // Type-safe table creation prevents runtime DDL errors
            System.out.println("    Creating Artist table with Catalog API");
            TableDefinition artistTable = TableDefinition.builder("DemoArtist")
                .ifNotExists()
                .columns(
                    column("ArtistId", INT32),
                    column("Name", varchar(120).notNull())
                )
                .primaryKey("ArtistId")
                .zone("DemoZone")
                .build();
            Table createdArtistTable = catalog.createTable(artistTable);
            System.out.println("    >>> Created DemoArtist table in DemoZone");
            
            // Colocation by ArtistId reduces join network overhead
            System.out.println("    Creating Album table with colocation");
            TableDefinition albumTable = TableDefinition.builder("DemoAlbum")
                .ifNotExists()
                .columns(
                    column("AlbumId", INT32),
                    column("ArtistId", INT32),
                    column("Title", varchar(160).notNull())
                )
                .primaryKey("AlbumId", "ArtistId")
                .colocateBy("ArtistId")
                .zone("DemoZone")
                .build();
            catalog.createTable(albumTable);
            System.out.println("    >>> Created DemoAlbum table with ArtistId colocation");
            
            // Nested colocation maintains data locality hierarchy
            System.out.println("    Creating Track table with nested colocation");
            TableDefinition trackTable = TableDefinition.builder("DemoTrack")
                .ifNotExists()
                .columns(
                    column("TrackId", INT32),
                    column("AlbumId", INT32),
                    column("Name", varchar(200).notNull()),
                    column("DurationMs", INT32),
                    column("UnitPrice", decimal(10, 2))
                )
                .primaryKey("TrackId", "AlbumId")
                .colocateBy("AlbumId")
                .zone("DemoZone")
                .build();
            catalog.createTable(trackTable);
            System.out.println("    >>> Created DemoTrack table with AlbumId colocation");
            
        } catch (Exception e) {
            logger.error("Table creation failed", e);
            System.err.println("    !!! Table creation error: " + e.getMessage());
        }
    }

    /**
     * Index strategy balances query performance against write overhead.
     * 
     * Foreign key indexes accelerate join operations and referential integrity checks.
     * Composite indexes support multi-column predicates and sort operations.
     * Index creation after data loading reduces maintenance overhead during ingestion.
     */
    private static void demonstrateIndexCreation(IgniteSql sql) {
        System.out.println();
        System.out.println("2. Index Creation for Performance:");
        
        try {
            // Create index on Album.ArtistId
            System.out.println("    Creating index on Album.ArtistId");
            sql.execute(null, "CREATE INDEX IF NOT EXISTS IDX_DemoAlbum_ArtistId ON DemoAlbum (ArtistId)");
            System.out.println("    >>> Created index IDX_DemoAlbum_ArtistId");
            
            // Create index on Track.AlbumId
            System.out.println("    Creating index on Track.AlbumId");
            sql.execute(null, "CREATE INDEX IF NOT EXISTS IDX_DemoTrack_AlbumId ON DemoTrack (AlbumId)");
            System.out.println("    >>> Created index IDX_DemoTrack_AlbumId");
            
            // Create composite index on Track
            System.out.println("    Creating composite index on Track");
            sql.execute(null, "CREATE INDEX IF NOT EXISTS IDX_DemoTrack_Price_Duration ON DemoTrack (UnitPrice, DurationMs)");
            System.out.println("    >>> Created composite index IDX_DemoTrack_Price_Duration");
            
        } catch (Exception e) {
            logger.error("Index creation failed", e);
            System.err.println("    !!! Index creation error: " + e.getMessage());
        }
    }

    /**
     * Schema introspection enables operational tooling and monitoring.
     * 
     * Catalog API provides programmatic access to table metadata for automation.
     * System catalogs support ad-hoc queries for capacity planning and debugging.
     * Data operations validate schema correctness and connectivity.
     */
    private static void demonstrateSchemaQuery(IgniteCatalog catalog, IgniteSql sql) {
        System.out.println();
        System.out.println("3. Schema Query and Validation:");
        
        try {
            // Validate demo tables using operational testing
            System.out.println("    Validating demo tables with test operations");
            
            String[] tableNames = {"DEMOARTIST", "DEMOALBUM", "DEMOTRACK"};
            for (String tableName : tableNames) {
                try {
                    // Test table by querying its structure
                    ResultSet<SqlRow> countResult = sql.execute(null, "SELECT COUNT(*) as cnt FROM " + tableName);
                    if (countResult.hasNext()) {
                        long count = countResult.next().longValue("cnt");
                        System.out.printf("    >>> Table '%s' operational (%d records)%n", tableName, count);
                    }
                } catch (Exception e) {
                    System.out.printf("    !!! Table '%s' not found or not accessible%n", tableName);
                }
            }
            
            // Query system catalogs for detailed table information
            System.out.println("    Querying system catalogs for table information");
            ResultSet<SqlRow> tableResults = sql.execute(null, 
                "SELECT NAME, ZONE FROM SYSTEM.TABLES WHERE NAME LIKE 'DEMO%'");
            
            int tableCount = 0;
            while (tableResults.hasNext()) {
                SqlRow row = tableResults.next();
                System.out.printf("    >>> Table: %s (zone: %s)%n", 
                    row.stringValue("NAME"), 
                    row.stringValue("ZONE"));
                tableCount++;
            }
            System.out.printf("    >>> Total demo tables found: %d%n", tableCount);
            
            // Test basic data operations to validate schema
            System.out.println("    Validating tables with basic data operations");
            
            // Test DemoArtist table
            try {
                sql.execute(null, "INSERT INTO DemoArtist (ArtistId, Name) VALUES (1, 'Test Artist')");
            } catch (Exception e) {
                // Ignore duplicate key errors
            }
            ResultSet<SqlRow> artistResult = sql.execute(null, "SELECT COUNT(*) as cnt FROM DemoArtist");
            long artistCount = artistResult.hasNext() ? artistResult.next().longValue("cnt") : 0;
            System.out.printf("    >>> DemoArtist table operational (%d records)%n", artistCount);
            
        } catch (Exception e) {
            logger.error("Schema query failed", e);
            System.err.println("    !!! Schema query error: " + e.getMessage());
        }
    }

    /**
     * Schema cleanup prevents resource leaks and maintains cluster health.
     * 
     * Dependency order prevents cascading failures during cleanup operations.
     * Failed table drops indicate active transactions or foreign key constraints.
     * Zone cleanup requires all dependent tables to be removed first.
     */
    private static void demonstrateSchemaCleanup(IgniteCatalog catalog) {
        System.out.println();
        System.out.println("4. Schema Cleanup Operations:");
        
        try {
            // Drop tables using Catalog API (identifiers normalized to uppercase)
            System.out.println("    Dropping demo tables using Catalog API");
            String[] tables = {"DEMOTRACK", "DEMOALBUM", "DEMOARTIST"};
            
            for (String tableName : tables) {
                try {
                    catalog.dropTable(tableName);
                    System.out.println("    >>> Dropped table: " + tableName);
                } catch (Exception e) {
                    System.out.println("    !!! Could not drop table: " + tableName + " (may not exist)");
                }
            }
            
            // Drop zone using Catalog API
            System.out.println("    Dropping demo zone using Catalog API");
            try {
                catalog.dropZone("DEMOZONE");
                System.out.println("    >>> Dropped DEMOZONE");
            } catch (Exception e) {
                System.out.println("    !!! DEMOZONE could not be dropped (may have dependencies or not exist)");
            }
            
        } catch (Exception e) {
            logger.error("Schema cleanup failed", e);
            System.err.println("    !!! Schema cleanup error: " + e.getMessage());
        }
    }
}