package com.apache.ignite.examples.sqlapi;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates basic SQL operations using the Apache Ignite 3 SQL API.
 * 
 * Covers fundamental SQL interface usage patterns including query execution,
 * parameter binding, statement configuration, and ResultSet processing.
 * Focuses on teaching the Java SQL API usage rather than SQL syntax.
 * 
 * Key concepts:
 * - IgniteSql interface for query execution
 * - Parameter binding for security and type safety
 * - Statement reuse for performance optimization
 * - ResultSet processing and data type extraction
 * - Basic error handling patterns
 * 
 * Prerequisites:
 * - Running Ignite cluster
 * - Sample music store data loaded
 */
public class BasicSQLOperations {

    private static final Logger logger = LoggerFactory.getLogger(BasicSQLOperations.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Basic SQL Operations Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Demonstrating fundamental SQL API patterns");

        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            BasicSQLOperations demo = new BasicSQLOperations();
            demo.runBasicSQLOperations(client);
            
        } catch (Exception e) {
            logger.error("Failed to run basic SQL operations", e);
            System.err.println("!!! Error: " + e.getMessage());
        }
    }

    private void runBasicSQLOperations(IgniteClient client) {
        IgniteSql sql = client.sql();
        
        System.out.println("\n--- Basic SQL API Usage ---");
        System.out.println("    Learning SQL interface fundamentals");
        
        // Simple query execution
        demonstrateSimpleQueries(sql);
        
        // Parameter binding
        demonstrateParameterBinding(sql);
        
        // Statement configuration and reuse
        demonstrateStatementReuse(sql);
        
        // ResultSet processing
        demonstrateResultSetProcessing(sql);
        
        System.out.println("\n>>> Basic SQL operations completed successfully");
    }

    /**
     * Demonstrates simple query execution and IgniteSql interface access.
     */
    private void demonstrateSimpleQueries(IgniteSql sql) {
        System.out.println("\n    --- Simple Query Execution");
        System.out.println("    >>> Executing basic SELECT queries");
        
        // Simple query without parameters
        ResultSet<SqlRow> artists = sql.execute(null, "SELECT ArtistId, Name FROM Artist LIMIT 3");
        
        System.out.println("    <<< Sample artists from database:");
        while (artists.hasNext()) {
            SqlRow row = artists.next();
            int artistId = row.intValue("ArtistId");
            String name = row.stringValue("Name");
            System.out.println("         Artist " + artistId + ": " + name);
        }
        
        // Count query demonstrating aggregation
        ResultSet<SqlRow> countResult = sql.execute(null, "SELECT COUNT(*) as total FROM Artist");
        if (countResult.hasNext()) {
            long totalArtists = countResult.next().longValue("total");
            System.out.println("    <<< Total artists in database: " + totalArtists);
        }
    }

    /**
     * Demonstrates parameter binding for secure and type-safe queries.
     */
    private void demonstrateParameterBinding(IgniteSql sql) {
        System.out.println("\n    --- Parameter Binding");
        System.out.println("    >>> Using parameters for security and type safety");
        
        // String parameter binding
        String searchPattern = "%Rock%";
        ResultSet<SqlRow> rockArtists = sql.execute(null,
                "SELECT ArtistId, Name FROM Artist WHERE Name LIKE ? ORDER BY Name LIMIT 3",
                searchPattern);

        System.out.println("    <<< Artists with 'Rock' in name:");
        while (rockArtists.hasNext()) {
            SqlRow row = rockArtists.next();
            System.out.println("         " + row.intValue("ArtistId") + ": " + row.stringValue("Name"));
        }
        
        // Multiple parameter binding
        ResultSet<SqlRow> trackDetails = sql.execute(null,
                "SELECT Name, Milliseconds FROM Track WHERE TrackId BETWEEN ? AND ? ORDER BY TrackId",
                1, 3);
        
        System.out.println("    <<< Track details with multiple parameters:");
        while (trackDetails.hasNext()) {
            SqlRow row = trackDetails.next();
            String name = row.stringValue("Name");
            int duration = row.intValue("Milliseconds");
            System.out.println("         '" + name + "' - " + (duration / 1000) + " seconds");
        }
    }

    /**
     * Demonstrates statement configuration and reuse for performance optimization.
     */
    private void demonstrateStatementReuse(IgniteSql sql) {
        System.out.println("\n    --- Statement Configuration and Reuse");
        System.out.println("    >>> Creating reusable statements for better performance");
        
        // Create configured statement
        Statement artistLookup = sql.statementBuilder()
                .query("SELECT ArtistId, Name FROM Artist WHERE Name = ?")
                .queryTimeout(30, TimeUnit.SECONDS)
                .pageSize(100)
                .build();

        String[] searchArtists = {"AC/DC", "Metallica", "U2"};
        
        for (String artistName : searchArtists) {
            ResultSet<SqlRow> result = sql.execute(null, artistLookup, artistName);
            
            if (result.hasNext()) {
                SqlRow row = result.next();
                System.out.println("    <<< Found: " + row.stringValue("Name") + 
                                 " (ID: " + row.intValue("ArtistId") + ")");
            } else {
                System.out.println("    !!! Artist '" + artistName + "' not found");
            }
        }
        
        // Demonstrate pagination configuration
        Statement pagedQuery = sql.statementBuilder()
                .query("SELECT TrackId, Name FROM Track ORDER BY TrackId")
                .pageSize(5)  // Small page size for demo
                .build();

        ResultSet<SqlRow> tracks = sql.execute(null, pagedQuery);
        
        System.out.println("    <<< First few tracks (demonstrating pagination):");
        int count = 0;
        while (tracks.hasNext() && count < 5) {
            SqlRow track = tracks.next();
            System.out.println("         Track " + track.intValue("TrackId") + ": " + track.stringValue("Name"));
            count++;
        }
    }

    /**
     * Demonstrates ResultSet processing and data type handling.
     */
    private void demonstrateResultSetProcessing(IgniteSql sql) {
        System.out.println("\n    --- ResultSet Processing");
        System.out.println("    >>> Extracting different data types from results");
        
        // Query with multiple data types
        ResultSet<SqlRow> trackData = sql.execute(null,
                "SELECT TrackId, Name, Milliseconds, UnitPrice FROM Track WHERE TrackId <= 2");

        System.out.println("    <<< Track details with various data types:");
        while (trackData.hasNext()) {
            SqlRow row = trackData.next();
            
            int trackId = row.intValue("TrackId");
            String trackName = row.stringValue("Name");
            long duration = row.intValue("Milliseconds");
            BigDecimal price = row.decimalValue("UnitPrice");
            
            System.out.println("         Track " + trackId + ": '" + trackName + 
                             "' - " + (duration / 1000) + "s - $" + price);
        }
        
        // Demonstrate checking result type (rows vs affected count)
        ResultSet<SqlRow> updateResult = sql.execute(null,
                "UPDATE Artist SET Name = Name WHERE ArtistId = 999999"); // Non-existent ID
        
        if (updateResult.hasRowSet()) {
            System.out.println("    <<< Update returned data rows");
        } else {
            long affectedRows = updateResult.affectedRows();
            System.out.println("    <<< Update affected " + affectedRows + " rows");
        }
    }
}