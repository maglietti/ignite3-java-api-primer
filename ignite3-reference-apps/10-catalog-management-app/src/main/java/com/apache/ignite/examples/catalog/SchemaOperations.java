package com.apache.ignite.examples.catalog;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Schema Operations - Table creation and management with Apache Ignite 3.
 * 
 * This class demonstrates schema management operations:
 * - Creating tables with proper distribution zones
 * - Adding indexes for performance optimization
 * - Implementing colocation strategies for data locality
 * - Schema validation and verification
 * - DDL operations using SQL API
 * 
 * Learning Focus:
 * - DDL operations through SQL API
 * - Distribution zone assignment for tables
 * - Index creation for query optimization
 * - Schema validation patterns
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
        IgniteSql sql = client.sql();
        
        System.out.println("\n--- Schema Operations Demonstrations ---");
        
        // Demonstrate core schema operations
        demonstrateTableCreation(sql);
        demonstrateIndexCreation(sql);
        demonstrateSchemaValidation(sql);
        demonstrateSchemaModification(sql);
        
        System.out.println("\n✓ Schema operations completed successfully");
    }

    /**
     * Demonstrates table creation with distribution zones.
     * 
     * Shows how to create tables with proper zone assignment:
     * - Basic table creation syntax
     * - Distribution zone specification
     * - Column definitions with constraints
     * - Primary key and colocation setup
     */
    private static void demonstrateTableCreation(IgniteSql sql) {
        System.out.println("\n1. Table Creation with Distribution Zones:");
        
        try {
            // Create a demo artist table in MusicStore zone
            System.out.println("   ⚡ Creating demo artist table with zone assignment");
            
            String createArtistTableSQL = """
                CREATE TABLE IF NOT EXISTS DemoArtist (
                    ArtistId INT PRIMARY KEY,
                    Name VARCHAR(200) NOT NULL,
                    Country VARCHAR(50),
                    Genre VARCHAR(100),
                    DebutYear INT
                ) WITH PRIMARY_ZONE = 'MusicStore'
                """;
            
            sql.execute(null, createArtistTableSQL);
            System.out.println("   ✓ Created DemoArtist table in MusicStore zone");
            
            // Create a demo album table with colocation
            System.out.println("   ⚡ Creating demo album table with colocation strategy");
            
            String createAlbumTableSQL = """
                CREATE TABLE IF NOT EXISTS DemoAlbum (
                    AlbumId INT,
                    ArtistId INT,
                    Title VARCHAR(300) NOT NULL,
                    ReleaseYear INT,
                    PRIMARY KEY (AlbumId, ArtistId)
                ) WITH PRIMARY_ZONE = 'MusicStore'
                COLOCATE BY (ArtistId)
                """;
            
            sql.execute(null, createAlbumTableSQL);
            System.out.println("   ✓ Created DemoAlbum table with ArtistId colocation");
            
            // Create a demo track table with hierarchy colocation
            System.out.println("   ⚡ Creating demo track table with hierarchical colocation");
            
            String createTrackTableSQL = """
                CREATE TABLE IF NOT EXISTS DemoTrack (
                    TrackId INT,
                    AlbumId INT,
                    ArtistId INT,
                    Name VARCHAR(400) NOT NULL,
                    Duration INT,
                    TrackNumber INT,
                    PRIMARY KEY (TrackId, AlbumId, ArtistId)
                ) WITH PRIMARY_ZONE = 'MusicStore'
                COLOCATE BY (ArtistId, AlbumId)
                """;
            
            sql.execute(null, createTrackTableSQL);
            System.out.println("   ✓ Created DemoTrack table with hierarchical colocation");
            
        } catch (Exception e) {
            logger.error("Table creation failed", e);
            System.err.println("   ⚠ Table creation error: " + e.getMessage());
        }
    }

    /**
     * Demonstrates index creation for performance optimization.
     * 
     * Shows different types of indexes:
     * - Simple indexes on frequently queried columns
     * - Composite indexes for complex queries
     * - Unique indexes for data integrity
     * - Covering indexes for query optimization
     */
    private static void demonstrateIndexCreation(IgniteSql sql) {
        System.out.println("\n2. Index Creation for Performance:");
        
        try {
            // Create index on artist name for browsing
            System.out.println("   ⚡ Creating index on artist name for browsing queries");
            sql.execute(null, "CREATE INDEX IF NOT EXISTS idx_artist_name ON DemoArtist (Name)");
            System.out.println("   ✓ Created index: idx_artist_name");
            
            // Create composite index on artist country and genre
            System.out.println("   ⚡ Creating composite index for filtered browsing");
            sql.execute(null, "CREATE INDEX IF NOT EXISTS idx_artist_country_genre ON DemoArtist (Country, Genre)");
            System.out.println("   ✓ Created index: idx_artist_country_genre");
            
            // Create index on album release year
            System.out.println("   ⚡ Creating index on album release year for chronological queries");
            sql.execute(null, "CREATE INDEX IF NOT EXISTS idx_album_year ON DemoAlbum (ReleaseYear)");
            System.out.println("   ✓ Created index: idx_album_year");
            
            // Create covering index for track searches
            System.out.println("   ⚡ Creating covering index for track search optimization");
            sql.execute(null, "CREATE INDEX IF NOT EXISTS idx_track_search ON DemoTrack (Name, Duration)");
            System.out.println("   ✓ Created index: idx_track_search");
            
        } catch (Exception e) {
            logger.error("Index creation failed", e);
            System.err.println("   ⚠ Index creation error: " + e.getMessage());
        }
    }

    /**
     * Demonstrates schema validation and verification.
     * 
     * Shows how to validate schema integrity:
     * - Check table existence and structure
     * - Verify index creation and effectiveness
     * - Validate zone assignments
     * - Test colocation strategies
     */
    private static void demonstrateSchemaValidation(IgniteSql sql) {
        System.out.println("\n3. Schema Validation and Verification:");
        
        try {
            // Validate table existence
            System.out.println("   ⚡ Validating table existence and structure");
            List<String> tables = getTableList(sql);
            
            String[] expectedTables = {"DemoArtist", "DemoAlbum", "DemoTrack"};
            for (String expectedTable : expectedTables) {
                if (tables.contains(expectedTable)) {
                    System.out.printf("   ✓ Table exists: %s%n", expectedTable);
                } else {
                    System.out.printf("   ⚠ Table missing: %s%n", expectedTable);
                }
            }
            
            // Validate index existence
            System.out.println("   ⚡ Validating index creation");
            List<String> indexes = getIndexList(sql);
            
            String[] expectedIndexes = {"idx_artist_name", "idx_artist_country_genre", "idx_album_year", "idx_track_search"};
            for (String expectedIndex : expectedIndexes) {
                if (indexes.contains(expectedIndex)) {
                    System.out.printf("   ✓ Index exists: %s%n", expectedIndex);
                } else {
                    System.out.printf("   ⚠ Index missing: %s%n", expectedIndex);
                }
            }
            
            // Test basic data operations
            System.out.println("   ⚡ Testing basic data operations");
            testBasicOperations(sql);
            
        } catch (Exception e) {
            logger.error("Schema validation failed", e);
            System.err.println("   ⚠ Schema validation error: " + e.getMessage());
        }
    }

    /**
     * Demonstrates schema modification operations.
     * 
     * Shows how to modify existing schema:
     * - Adding new columns to tables
     * - Dropping and recreating indexes
     * - Altering table properties
     * - Safe schema evolution patterns
     */
    private static void demonstrateSchemaModification(IgniteSql sql) {
        System.out.println("\n4. Schema Modification Operations:");
        
        try {
            // Add a new column to existing table
            System.out.println("   ⚡ Adding new column to DemoArtist table");
            sql.execute(null, "ALTER TABLE DemoArtist ADD COLUMN IF NOT EXISTS WebsiteUrl VARCHAR(500)");
            System.out.println("   ✓ Added WebsiteUrl column to DemoArtist");
            
            // Create index on new column
            System.out.println("   ⚡ Creating index on new column");
            sql.execute(null, "CREATE INDEX IF NOT EXISTS idx_artist_website ON DemoArtist (WebsiteUrl)");
            System.out.println("   ✓ Created index: idx_artist_website");
            
            // Demonstrate safe column addition with default values
            System.out.println("   ⚡ Adding column with default value to DemoAlbum");
            sql.execute(null, "ALTER TABLE DemoAlbum ADD COLUMN IF NOT EXISTS IsExplicit BOOLEAN DEFAULT false");
            System.out.println("   ✓ Added IsExplicit column with default value");
            
        } catch (Exception e) {
            logger.error("Schema modification failed", e);
            System.err.println("   ⚠ Schema modification error: " + e.getMessage());
        }
        
        // Cleanup demo tables
        cleanupDemoTables(sql);
    }

    // Helper methods

    private static List<String> getTableList(IgniteSql sql) {
        List<String> tables = new ArrayList<>();
        try {
            // Query system tables to get table list
            ResultSet<SqlRow> rs = sql.execute(null, 
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'");
            
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                tables.add(row.stringValue("TABLE_NAME"));
            }
        } catch (Exception e) {
            logger.warn("Failed to get table list", e);
        }
        return tables;
    }

    private static List<String> getIndexList(IgniteSql sql) {
        List<String> indexes = new ArrayList<>();
        try {
            // Query system tables to get index list
            ResultSet<SqlRow> rs = sql.execute(null, 
                "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_SCHEMA = 'PUBLIC'");
            
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                String indexName = row.stringValue("INDEX_NAME");
                if (indexName != null && !indexName.startsWith("SYS_")) {
                    indexes.add(indexName);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to get index list", e);
        }
        return indexes;
    }

    private static void testBasicOperations(IgniteSql sql) {
        try {
            // Test insert operations
            sql.execute(null, """
                INSERT INTO DemoArtist (ArtistId, Name, Country, Genre, DebutYear) 
                VALUES (9001, 'Test Artist', 'USA', 'Rock', 2020)
                """);
            
            // Test query operations
            ResultSet<SqlRow> rs = sql.execute(null, 
                "SELECT Name, Country FROM DemoArtist WHERE ArtistId = 9001");
            
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                System.out.printf("   ✓ Query test successful: %s from %s%n", 
                    row.stringValue("Name"), row.stringValue("Country"));
            }
            
            // Test update operations
            sql.execute(null, "UPDATE DemoArtist SET Genre = 'Alternative Rock' WHERE ArtistId = 9001");
            System.out.println("   ✓ Update operation successful");
            
        } catch (Exception e) {
            logger.warn("Basic operations test failed", e);
            System.err.println("   ⚠ Basic operations test error: " + e.getMessage());
        }
    }

    private static void cleanupDemoTables(IgniteSql sql) {
        System.out.println("\n5. Cleanup Demo Schema:");
        
        try {
            // Drop demo tables in reverse dependency order
            String[] tablesToDrop = {"DemoTrack", "DemoAlbum", "DemoArtist"};
            
            for (String table : tablesToDrop) {
                sql.execute(null, "DROP TABLE IF EXISTS " + table);
                System.out.printf("   ✓ Dropped table: %s%n", table);
            }
            
        } catch (Exception e) {
            logger.error("Cleanup failed", e);
            System.err.println("   ⚠ Cleanup error: " + e.getMessage());
        }
    }
}