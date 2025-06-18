package com.apache.ignite.examples.catalog;

import org.apache.ignite.catalog.IgniteCatalog;
import org.apache.ignite.catalog.definitions.TableDefinition;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.IgniteTables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Catalog introspection for operational monitoring and capacity planning.
 * 
 * Multiple APIs provide different introspection capabilities:
 * - Catalog API offers type-safe metadata access for programmatic tools
 * - Tables API enables dynamic table discovery and connection validation
 * - System catalogs support ad-hoc SQL queries for operational analysis
 * 
 * Production applications use introspection for:
 * - Health checks and connectivity validation
 * - Capacity planning based on data distribution patterns
 * - Schema evolution and compatibility verification
 */
public class CatalogIntrospection {

    private static final Logger logger = LoggerFactory.getLogger(CatalogIntrospection.class);
    
    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Catalog Introspection Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            runCatalogIntrospection(client);
            
        } catch (Exception e) {
            logger.error("Failed to run catalog introspection", e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void runCatalogIntrospection(IgniteClient client) {
        IgniteCatalog catalog = client.catalog();
        IgniteTables tables = client.tables();
        IgniteSql sql = client.sql();
        
        System.out.println();
        System.out.println("--- Catalog Introspection Operations ---");
        
        // Demonstrate core introspection operations
        demonstrateBasicIntrospection(catalog, tables, sql);
        demonstrateSchemaValidation(sql);
        demonstrateDataAnalysis(sql);
        
        System.out.println();
        System.out.println(">>> Catalog introspection completed successfully");
    }

    /**
     * Multi-layer introspection provides operational flexibility.
     * 
     * Tables API enables clean table discovery without verbose SQL output.
     * System catalogs support direct metadata queries for capacity planning.
     * Operational validation confirms table accessibility and data integrity.
     */
    private static void demonstrateBasicIntrospection(IgniteCatalog catalog, IgniteTables tables, IgniteSql sql) {
        System.out.println();
        System.out.println("1. Basic Schema Introspection:");
        
        try {
            // Use Tables API to list all available tables
            System.out.println("    Discovering tables using Tables API");
            List<Table> allTables = tables.tables();
            System.out.printf("    >>> Found %d tables in cluster%n", allTables.size());
            
            // Show first few tables
            int displayCount = Math.min(5, allTables.size());
            for (int i = 0; i < displayCount; i++) {
                Table table = allTables.get(i);
                System.out.printf("    >>> Table: %s%n", table.name());
            }
            
            // Validate table accessibility using direct table access
            System.out.println("    Validating table accessibility");
            String[] sampleTables = {"ARTIST", "ALBUM", "TRACK", "CUSTOMER"};
            int accessibleTables = 0;
            
            for (String tableName : sampleTables) {
                try {
                    // Test table accessibility by getting a count
                    ResultSet<SqlRow> countResult = sql.execute(null, "SELECT COUNT(*) as cnt FROM " + tableName + " LIMIT 1");
                    if (countResult.hasNext()) {
                        long count = countResult.next().longValue("cnt");
                        System.out.printf("    >>> Table '%s' accessible (%d records)%n", tableName, count);
                        accessibleTables++;
                    }
                } catch (Exception e) {
                    System.out.printf("    !!! Table '%s' not accessible%n", tableName);
                }
            }
            
            // Query system catalogs for detailed table information
            System.out.println("    Querying system catalogs for table metadata");
            ResultSet<SqlRow> systemTables = sql.execute(null, 
                "SELECT NAME, ZONE FROM SYSTEM.TABLES LIMIT 5");
            
            int catalogTableCount = 0;
            while (systemTables.hasNext()) {
                SqlRow row = systemTables.next();
                System.out.printf("    >>> System catalog: %s (zone: %s)%n", 
                    row.stringValue("NAME"), 
                    row.stringValue("ZONE"));
                catalogTableCount++;
            }
            
            System.out.printf("    >>> Introspection summary: %d tables accessible, %d via system catalog%n", 
                accessibleTables, catalogTableCount);
            
        } catch (Exception e) {
            logger.error("Basic introspection failed", e);
            System.err.println("    !!! Basic introspection error: " + e.getMessage());
        }
    }

    /**
     * Operational validation prevents silent schema corruption.
     * 
     * Join queries validate foreign key relationships and colocation effectiveness.
     * Aggregate operations test query engine functionality across partitions.
     * Row counts indicate data distribution and replication health.
     */
    private static void demonstrateSchemaValidation(IgniteSql sql) {
        System.out.println();
        System.out.println("2. Schema Validation and Testing:");
        
        try {
            // Test Artist-Album relationship
            System.out.println("    Testing Artist-Album relationships");
            ResultSet<SqlRow> artistAlbums = sql.execute(null, """
                SELECT a.Name as artist_name, COUNT(al.AlbumId) as album_count
                FROM Artist a 
                LEFT JOIN Album al ON a.ArtistId = al.ArtistId
                GROUP BY a.ArtistId, a.Name
                HAVING COUNT(al.AlbumId) > 0
                LIMIT 5
                """);
            
            int artistCount = 0;
            while (artistAlbums.hasNext()) {
                SqlRow row = artistAlbums.next();
                System.out.printf("    >>> Artist '%s' has %d albums%n", 
                    row.stringValue("artist_name"), 
                    row.longValue("album_count"));
                artistCount++;
            }
            System.out.printf("    >>> Validated %d artist-album relationships%n", artistCount);
            
            // Test Album-Track relationship
            System.out.println("    Testing Album-Track relationships");
            ResultSet<SqlRow> albumTracks = sql.execute(null, """
                SELECT al.Title as album_title, COUNT(t.TrackId) as track_count
                FROM Album al
                LEFT JOIN Track t ON al.AlbumId = t.AlbumId
                GROUP BY al.AlbumId, al.Title
                HAVING COUNT(t.TrackId) > 0
                LIMIT 5
                """);
            
            int albumCount = 0;
            while (albumTracks.hasNext()) {
                SqlRow row = albumTracks.next();
                System.out.printf("    >>> Album '%s' has %d tracks%n", 
                    row.stringValue("album_title"), 
                    row.longValue("track_count"));
                albumCount++;
            }
            System.out.printf("    >>> Validated %d album-track relationships%n", albumCount);
            
        } catch (Exception e) {
            logger.error("Schema validation failed", e);
            System.err.println("    !!! Schema validation error: " + e.getMessage());
        }
    }

    /**
     * Data distribution analysis guides performance optimization.
     * 
     * Skewed data distribution affects query performance and load balancing.
     * Aggregate statistics reveal hotspots and capacity requirements.
     * Cross-table analysis validates colocation strategies and join efficiency.
     */
    private static void demonstrateDataAnalysis(IgniteSql sql) {
        System.out.println();
        System.out.println("3. Data Analysis and Distribution:");
        
        try {
            // Analyze genre distribution
            System.out.println("    Analyzing music genre distribution");
            ResultSet<SqlRow> genreStats = sql.execute(null, """
                SELECT g.Name as genre_name, COUNT(t.TrackId) as track_count
                FROM Genre g
                LEFT JOIN Track t ON g.GenreId = t.GenreId
                GROUP BY g.GenreId, g.Name
                ORDER BY track_count DESC
                LIMIT 5
                """);
            
            System.out.println("    >>> Top genres by track count:");
            while (genreStats.hasNext()) {
                SqlRow row = genreStats.next();
                System.out.printf("        %s: %d tracks%n", 
                    row.stringValue("genre_name"), 
                    row.longValue("track_count"));
            }
            
            // Analyze customer distribution
            System.out.println("    Analyzing customer distribution");
            ResultSet<SqlRow> customerStats = sql.execute(null, """
                SELECT Country, COUNT(*) as customer_count
                FROM Customer
                GROUP BY Country
                ORDER BY customer_count DESC
                LIMIT 5
                """);
            
            System.out.println("    >>> Top countries by customer count:");
            while (customerStats.hasNext()) {
                SqlRow row = customerStats.next();
                System.out.printf("        %s: %d customers%n", 
                    row.stringValue("Country"), 
                    row.longValue("customer_count"));
            }
            
            // Generate overall schema statistics
            System.out.println("    Generating overall schema statistics");
            
            // Count total artists
            ResultSet<SqlRow> artistTotal = sql.execute(null, "SELECT COUNT(*) as total FROM Artist");
            long totalArtists = artistTotal.hasNext() ? artistTotal.next().longValue("total") : 0;
            
            // Count total albums
            ResultSet<SqlRow> albumTotal = sql.execute(null, "SELECT COUNT(*) as total FROM Album");
            long totalAlbums = albumTotal.hasNext() ? albumTotal.next().longValue("total") : 0;
            
            // Count total tracks
            ResultSet<SqlRow> trackTotal = sql.execute(null, "SELECT COUNT(*) as total FROM Track");
            long totalTracks = trackTotal.hasNext() ? trackTotal.next().longValue("total") : 0;
            
            // Count total customers
            ResultSet<SqlRow> customerTotal = sql.execute(null, "SELECT COUNT(*) as total FROM Customer");
            long totalCustomers = customerTotal.hasNext() ? customerTotal.next().longValue("total") : 0;
            
            System.out.println("    >>> Schema statistics summary:");
            System.out.printf("        Artists: %d%n", totalArtists);
            System.out.printf("        Albums: %d%n", totalAlbums);
            System.out.printf("        Tracks: %d%n", totalTracks);
            System.out.printf("        Customers: %d%n", totalCustomers);
            
        } catch (Exception e) {
            logger.error("Data analysis failed", e);
            System.err.println("    !!! Data analysis error: " + e.getMessage());
        }
    }
}