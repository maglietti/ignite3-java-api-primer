package com.apache.ignite.examples.sqlapi;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demonstrates DDL (Data Definition Language) operations using the Ignite 3 SQL API.
 * 
 * This class focuses on schema management operations including:
 * - Creating and dropping distribution zones
 * - Creating tables with colocation strategies
 * - Creating indexes for query performance
 * - Altering table structures
 * - Managing schema evolution
 * 
 * The emphasis is on using the Java SQL API for DDL operations rather than
 * SQL syntax itself, demonstrating how to manage database schema programmatically
 * in distributed environments.
 * 
 * Prerequisites:
 * - Ignite 3 cluster running on localhost:10800
 * - Appropriate permissions for DDL operations
 * 
 * @see org.apache.ignite.sql.IgniteSql#execute
 * @see org.apache.ignite.sql.IgniteSql#executeScript
 */
public class DDLOperations {
    
    private static final Logger logger = LoggerFactory.getLogger(DDLOperations.class);
    
    public static void main(String[] args) {
        logger.info("Starting DDL Operations Demo");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800")
                .build()) {
            
            DDLOperations demo = new DDLOperations();
            
            // Demonstrate various DDL operations
            demo.demonstrateZoneManagement(client);
            demo.demonstrateTableCreation(client);
            demo.demonstrateIndexManagement(client);
            demo.demonstrateSchemaEvolution(client);
            demo.demonstrateSchemaIntrospection(client);
            demo.cleanupDemoObjects(client);
            
            logger.info("DDL Operations Demo completed successfully");
            
        } catch (Exception e) {
            logger.error("DDL Operations Demo failed", e);
            System.exit(1);
        }
    }
    
    /**
     * Demonstrates distribution zone creation and management.
     * Zones define how data is distributed and replicated across the cluster.
     */
    private void demonstrateZoneManagement(IgniteClient client) {
        logger.info("=== Distribution Zone Management ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // Create a zone for demo purposes with specific replica and partition settings
            logger.info("Creating demo distribution zone...");
            sql.execute(null, 
                "CREATE ZONE IF NOT EXISTS DemoZone WITH REPLICAS=2, PARTITIONS=10, STORAGE_PROFILES='default'");
            
            logger.info("Demo zone created successfully");
            
            // Create a replicated zone for reference data
            logger.info("Creating replicated zone for reference data...");
            sql.execute(null,
                "CREATE ZONE IF NOT EXISTS DemoReplicatedZone WITH REPLICAS=3, PARTITIONS=1, STORAGE_PROFILES='default'");
            
            logger.info("Replicated zone created successfully");
            
            // Query system catalog to verify zone creation
            ResultSet<SqlRow> zones = sql.execute(null,
                "SELECT name, replicas, partitions FROM SYSTEM.ZONES WHERE name LIKE 'Demo%'");
            
            logger.info("Created zones:");
            while (zones.hasNext()) {
                SqlRow zone = zones.next();
                logger.info("  Zone: {} - Replicas: {}, Partitions: {}", 
                    zone.stringValue("name"),
                    zone.intValue("replicas"), 
                    zone.intValue("partitions"));
            }
            
        } catch (Exception e) {
            logger.error("Failed to manage distribution zones: {}", e.getMessage());
        }
    }
    
    /**
     * Demonstrates table creation with various configurations and colocation strategies.
     * Shows how to create tables optimized for distributed query performance.
     */
    private void demonstrateTableCreation(IgniteClient client) {
        logger.info("=== Table Creation with Colocation ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // Create a simple table in the demo zone
            logger.info("Creating DemoArtist table...");
            sql.execute(null, """
                CREATE TABLE IF NOT EXISTS DemoArtist (
                    ArtistId INTEGER PRIMARY KEY,
                    Name VARCHAR(120) NOT NULL,
                    Country VARCHAR(50),
                    Genre VARCHAR(50)
                ) WITH PRIMARY_ZONE='DemoZone'
                """);
            
            logger.info("DemoArtist table created");
            
            // Create a related table with colocation for optimal JOIN performance
            logger.info("Creating DemoAlbum table with colocation...");
            sql.execute(null, """
                CREATE TABLE IF NOT EXISTS DemoAlbum (
                    AlbumId INTEGER,
                    ArtistId INTEGER,
                    Title VARCHAR(160) NOT NULL,
                    ReleaseYear INTEGER,
                    PRIMARY KEY (AlbumId, ArtistId)
                ) WITH PRIMARY_ZONE='DemoZone', COLOCATION_COLUMNS='ArtistId'
                """);
            
            logger.info("DemoAlbum table created with colocation on ArtistId");
            
            // Create a reference data table in replicated zone
            logger.info("Creating DemoGenre reference table...");
            sql.execute(null, """
                CREATE TABLE IF NOT EXISTS DemoGenre (
                    GenreId INTEGER PRIMARY KEY,
                    Name VARCHAR(50) NOT NULL,
                    Description VARCHAR(200)
                ) WITH PRIMARY_ZONE='DemoReplicatedZone'
                """);
            
            logger.info("DemoGenre reference table created in replicated zone");
            
            // Verify table creation by querying system catalog
            ResultSet<SqlRow> tables = sql.execute(null,
                "SELECT table_name, table_schema FROM INFORMATION_SCHEMA.TABLES WHERE table_name LIKE 'Demo%'");
            
            logger.info("Created tables:");
            while (tables.hasNext()) {
                SqlRow table = tables.next();
                logger.info("  Table: {}.{}", 
                    table.stringValue("table_schema"),
                    table.stringValue("table_name"));
            }
            
        } catch (Exception e) {
            logger.error("Failed to create tables: {}", e.getMessage());
        }
    }
    
    /**
     * Demonstrates index creation and management for query performance optimization.
     * Shows how to create different types of indexes using the SQL API.
     */
    private void demonstrateIndexManagement(IgniteClient client) {
        logger.info("=== Index Management ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // Create index on artist name for efficient searches
            logger.info("Creating index on DemoArtist.Name...");
            sql.execute(null, "CREATE INDEX IF NOT EXISTS IDX_DemoArtist_Name ON DemoArtist (Name)");
            
            // Create composite index for multi-column searches
            logger.info("Creating composite index on DemoArtist...");
            sql.execute(null, "CREATE INDEX IF NOT EXISTS IDX_DemoArtist_Country_Genre ON DemoArtist (Country, Genre)");
            
            // Create foreign key index for JOIN performance
            logger.info("Creating foreign key index on DemoAlbum...");
            sql.execute(null, "CREATE INDEX IF NOT EXISTS IDX_DemoAlbum_ArtistId ON DemoAlbum (ArtistId)");
            
            // Create index on album title for text searches
            logger.info("Creating index on DemoAlbum.Title...");
            sql.execute(null, "CREATE INDEX IF NOT EXISTS IDX_DemoAlbum_Title ON DemoAlbum (Title)");
            
            logger.info("All indexes created successfully");
            
            // Query system catalog to verify index creation
            ResultSet<SqlRow> indexes = sql.execute(null,
                "SELECT index_name, table_name, column_name FROM INFORMATION_SCHEMA.INDEXES " +
                "WHERE table_name LIKE 'Demo%' ORDER BY table_name, index_name");
            
            logger.info("Created indexes:");
            String currentTable = "";
            while (indexes.hasNext()) {
                SqlRow index = indexes.next();
                String tableName = index.stringValue("table_name");
                
                if (!tableName.equals(currentTable)) {
                    logger.info("  Table: {}", tableName);
                    currentTable = tableName;
                }
                
                logger.info("    Index: {} on column: {}", 
                    index.stringValue("index_name"),
                    index.stringValue("column_name"));
            }
            
        } catch (Exception e) {
            logger.error("Failed to manage indexes: {}", e.getMessage());
        }
    }
    
    /**
     * Demonstrates schema evolution operations like adding columns and modifying tables.
     * Shows how to evolve database schema without service interruption.
     */
    private void demonstrateSchemaEvolution(IgniteClient client) {
        logger.info("=== Schema Evolution ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // Add new column to existing table
            logger.info("Adding new column to DemoArtist table...");
            sql.execute(null, "ALTER TABLE DemoArtist ADD COLUMN IF NOT EXISTS Website VARCHAR(200)");
            
            // Add another column with default value
            logger.info("Adding column with default value...");
            sql.execute(null, "ALTER TABLE DemoArtist ADD COLUMN IF NOT EXISTS IsActive BOOLEAN DEFAULT true");
            
            // Create index on new column
            logger.info("Creating index on new Website column...");
            sql.execute(null, "CREATE INDEX IF NOT EXISTS IDX_DemoArtist_Website ON DemoArtist (Website)");
            
            logger.info("Schema evolution completed successfully");
            
            // Verify schema changes by examining table structure
            ResultSet<SqlRow> columns = sql.execute(null,
                "SELECT column_name, data_type, is_nullable, column_default " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE table_name = 'DemoArtist' " +
                "ORDER BY ordinal_position");
            
            logger.info("Updated DemoArtist table structure:");
            while (columns.hasNext()) {
                SqlRow column = columns.next();
                String defaultValue = column.stringValue("column_default");
                logger.info("  Column: {} ({}){}{}", 
                    column.stringValue("column_name"),
                    column.stringValue("data_type"),
                    column.stringValue("is_nullable").equals("YES") ? " NULL" : " NOT NULL",
                    defaultValue != null ? " DEFAULT " + defaultValue : "");
            }
            
        } catch (Exception e) {
            logger.error("Failed to evolve schema: {}", e.getMessage());
        }
    }
    
    /**
     * Demonstrates schema introspection capabilities using system catalogs.
     * Shows how to query metadata about tables, columns, indexes, and zones.
     */
    private void demonstrateSchemaIntrospection(IgniteClient client) {
        logger.info("=== Schema Introspection ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // Get table count in demo schema
            ResultSet<SqlRow> tableCount = sql.execute(null,
                "SELECT COUNT(*) as table_count FROM INFORMATION_SCHEMA.TABLES WHERE table_name LIKE 'Demo%'");
            
            if (tableCount.hasNext()) {
                long count = tableCount.next().longValue("table_count");
                logger.info("Total demo tables: {}", count);
            }
            
            // Get column information for all demo tables
            ResultSet<SqlRow> columnInfo = sql.execute(null,
                "SELECT table_name, COUNT(*) as column_count " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE table_name LIKE 'Demo%' " +
                "GROUP BY table_name " +
                "ORDER BY table_name");
            
            logger.info("Column counts per table:");
            while (columnInfo.hasNext()) {
                SqlRow info = columnInfo.next();
                logger.info("  {}: {} columns", 
                    info.stringValue("table_name"),
                    info.longValue("column_count"));
            }
            
            // Get index information
            ResultSet<SqlRow> indexInfo = sql.execute(null,
                "SELECT table_name, COUNT(DISTINCT index_name) as index_count " +
                "FROM INFORMATION_SCHEMA.INDEXES " +
                "WHERE table_name LIKE 'Demo%' " +
                "GROUP BY table_name " +
                "ORDER BY table_name");
            
            logger.info("Index counts per table:");
            while (indexInfo.hasNext()) {
                SqlRow info = indexInfo.next();
                logger.info("  {}: {} indexes", 
                    info.stringValue("table_name"),
                    info.longValue("index_count"));
            }
            
            // Check zone assignments
            ResultSet<SqlRow> zoneInfo = sql.execute(null,
                "SELECT name as zone_name, replicas, partitions " +
                "FROM SYSTEM.ZONES " +
                "WHERE name LIKE 'Demo%' " +
                "ORDER BY name");
            
            logger.info("Zone configurations:");
            while (zoneInfo.hasNext()) {
                SqlRow zone = zoneInfo.next();
                logger.info("  Zone {}: {} replicas, {} partitions", 
                    zone.stringValue("zone_name"),
                    zone.intValue("replicas"),
                    zone.intValue("partitions"));
            }
            
        } catch (Exception e) {
            logger.error("Failed to introspect schema: {}", e.getMessage());
        }
    }
    
    /**
     * Cleans up all demo objects created during the demonstration.
     * Shows proper cleanup order for dependent objects.
     */
    private void cleanupDemoObjects(IgniteClient client) {
        logger.info("=== Cleanup Demo Objects ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // Drop indexes first (they depend on tables)
            logger.info("Dropping indexes...");
            sql.execute(null, "DROP INDEX IF EXISTS IDX_DemoArtist_Name");
            sql.execute(null, "DROP INDEX IF EXISTS IDX_DemoArtist_Country_Genre");
            sql.execute(null, "DROP INDEX IF EXISTS IDX_DemoArtist_Website");
            sql.execute(null, "DROP INDEX IF EXISTS IDX_DemoAlbum_ArtistId");
            sql.execute(null, "DROP INDEX IF EXISTS IDX_DemoAlbum_Title");
            
            // Drop tables in dependency order
            logger.info("Dropping tables...");
            sql.execute(null, "DROP TABLE IF EXISTS DemoAlbum");
            sql.execute(null, "DROP TABLE IF EXISTS DemoArtist");
            sql.execute(null, "DROP TABLE IF EXISTS DemoGenre");
            
            // Drop zones last (only after all tables using them are dropped)
            logger.info("Dropping zones...");
            sql.execute(null, "DROP ZONE IF EXISTS DemoZone");
            sql.execute(null, "DROP ZONE IF EXISTS DemoReplicatedZone");
            
            logger.info("All demo objects cleaned up successfully");
            
            // Verify cleanup
            ResultSet<SqlRow> remainingTables = sql.execute(null,
                "SELECT COUNT(*) as count FROM INFORMATION_SCHEMA.TABLES WHERE table_name LIKE 'Demo%'");
            
            if (remainingTables.hasNext()) {
                long count = remainingTables.next().longValue("count");
                if (count == 0) {
                    logger.info("Cleanup verification: No demo tables remaining");
                } else {
                    logger.warn("Cleanup verification: {} demo tables still exist", count);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to cleanup demo objects: {}", e.getMessage());
        }
    }
}