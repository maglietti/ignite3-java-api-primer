/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apache.ignite.examples.sqlapi;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.BatchedArguments;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.mapper.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * Demonstrates advanced SQL operations using the Apache Ignite 3 SQL API.
 * 
 * Covers complex query patterns including JOIN operations, aggregations,
 * batch processing, and object mapping. Shows how to work with hierarchical
 * data relationships and perform bulk operations efficiently.
 * 
 * Key concepts:
 * - Complex JOIN queries with multiple tables
 * - Aggregation and GROUP BY operations
 * - Batch operations for bulk data processing
 * - Object mapping with type-safe result processing
 * - Performance optimization patterns
 * 
 * Prerequisites:
 * - Running Ignite cluster
 * - Sample music store data loaded
 */
public class AdvancedSQLOperations {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedSQLOperations.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Advanced SQL Operations Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Demonstrating complex queries and bulk operations");

        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            AdvancedSQLOperations demo = new AdvancedSQLOperations();
            demo.runAdvancedSQLOperations(client);
            
        } catch (Exception e) {
            logger.error("Failed to run advanced SQL operations", e);
            System.err.println("!!! Error: " + e.getMessage());
        }
    }

    private void runAdvancedSQLOperations(IgniteClient client) {
        IgniteSql sql = client.sql();
        
        System.out.println("\n--- Advanced SQL Patterns ---");
        System.out.println("    Complex queries and bulk operations");
        
        // JOIN operations with hierarchical data
        demonstrateJoinOperations(sql);
        
        // Aggregation and analytical queries
        demonstrateAggregationQueries(sql);
        
        // Batch operations for bulk processing
        demonstrateBatchOperations(sql);
        
        // Object mapping for type-safe results
        demonstrateObjectMapping(sql);
        
        System.out.println("\n>>> Advanced SQL operations completed successfully");
    }

    /**
     * Demonstrates JOIN operations with the music store data model.
     */
    private void demonstrateJoinOperations(IgniteSql sql) {
        System.out.println("\n--- JOIN Operations");
        System.out.println(">>> Executing queries across related tables");
        
        // Simple JOIN between Artist and Album
        String artistAlbumQuery = """
                SELECT a.Name as ArtistName, al.Title as AlbumTitle
                FROM Artist a
                JOIN Album al ON a.ArtistId = al.ArtistId
                WHERE a.ArtistId <= 3
                ORDER BY a.Name, al.Title
                """;
        
        ResultSet<SqlRow> artistAlbums = sql.execute(null, artistAlbumQuery);
        
        System.out.println("<<< Artists and their albums:");
        while (artistAlbums.hasNext()) {
            SqlRow row = artistAlbums.next();
            String artist = row.stringValue("ArtistName");
            String album = row.stringValue("AlbumTitle");
            System.out.println("         " + artist + " - " + album);
        }
        
        // Multi-table JOIN with aggregation
        String trackStatsQuery = """
                SELECT a.Name as ArtistName, 
                       COUNT(t.TrackId) as TrackCount,
                       AVG(t.Milliseconds) as AvgDuration
                FROM Artist a
                JOIN Album al ON a.ArtistId = al.ArtistId
                JOIN Track t ON al.AlbumId = t.AlbumId
                WHERE a.ArtistId <= 5
                GROUP BY a.ArtistId, a.Name
                HAVING COUNT(t.TrackId) > 5
                ORDER BY TrackCount DESC
                """;
        
        ResultSet<SqlRow> trackStats = sql.execute(null, trackStatsQuery);
        
        System.out.println("<<< Artist track statistics:");
        while (trackStats.hasNext()) {
            SqlRow row = trackStats.next();
            String artist = row.stringValue("ArtistName");
            long trackCount = row.longValue("TrackCount");
            BigDecimal avgDuration = row.decimalValue("AvgDuration");
            System.out.println("         " + artist + ": " + trackCount + 
                             " tracks, avg " + (avgDuration.longValue() / 1000) + "s");
        }
    }

    /**
     * Demonstrates aggregation queries and analytical operations.
     */
    private void demonstrateAggregationQueries(IgniteSql sql) {
        System.out.println("\n--- Aggregation Queries");
        System.out.println(">>> Computing statistics and analytics");
        
        // Genre popularity analysis
        String genreStatsQuery = """
                SELECT g.Name as GenreName, 
                       COUNT(t.TrackId) as TrackCount,
                       SUM(t.Milliseconds) as TotalDuration
                FROM Genre g
                JOIN Track t ON g.GenreId = t.GenreId
                GROUP BY g.GenreId, g.Name
                ORDER BY TrackCount DESC
                LIMIT 5
                """;
        
        ResultSet<SqlRow> genreStats = sql.execute(null, genreStatsQuery);
        
        System.out.println("<<< Most popular genres by track count:");
        while (genreStats.hasNext()) {
            SqlRow row = genreStats.next();
            String genre = row.stringValue("GenreName");
            long trackCount = row.longValue("TrackCount");
            long totalDuration = row.longValue("TotalDuration");
            System.out.println("         " + genre + ": " + trackCount + 
                             " tracks (" + (totalDuration / (1000 * 60)) + " minutes)");
        }
        
        // Price analysis
        ResultSet<SqlRow> priceStats = sql.execute(null,
                "SELECT MIN(UnitPrice) as MinPrice, MAX(UnitPrice) as MaxPrice, " +
                "AVG(UnitPrice) as AvgPrice FROM Track");
        
        if (priceStats.hasNext()) {
            SqlRow row = priceStats.next();
            System.out.println("<<< Track pricing statistics:");
            System.out.println("         Min: $" + row.decimalValue("MinPrice"));
            System.out.println("         Max: $" + row.decimalValue("MaxPrice"));
            System.out.println("         Avg: $" + row.decimalValue("AvgPrice"));
        }
    }

    /**
     * Demonstrates batch operations for efficient bulk data processing.
     */
    private void demonstrateBatchOperations(IgniteSql sql) {
        System.out.println("\n--- Batch Operations");
        System.out.println(">>> Performing bulk data operations");
        
        // Create batch arguments for multiple test artists
        BatchedArguments artistBatch = BatchedArguments.create()
                .add(8001, "Batch Demo Artist 1")
                .add(8002, "Batch Demo Artist 2")
                .add(8003, "Batch Demo Artist 3");

        try {
            System.out.println(">>> Executing batch insert");
            
            // Execute batch insert
            long[] insertCounts = sql.executeBatch(null,
                    "INSERT INTO Artist (ArtistId, Name) VALUES (?, ?)",
                    artistBatch);

            System.out.println("<<< Batch insert completed: " + insertCounts.length + " artists");
            
            // Verify batch results
            ResultSet<SqlRow> batchResult = sql.execute(null,
                    "SELECT COUNT(*) as artist_count FROM Artist WHERE Name LIKE 'Batch Demo Artist%'");

            if (batchResult.hasNext()) {
                long count = batchResult.next().longValue("artist_count");
                System.out.println("<<< Verification: " + count + " batch demo artists created");
            }

        } catch (Exception e) {
            System.err.println("!!! Batch operation failed: " + e.getMessage());
        } finally {
            // Clean up batch test data
            sql.execute(null, "DELETE FROM Artist WHERE Name LIKE 'Batch Demo Artist%'");
            System.out.println(">>> Batch test data cleaned up");
        }
    }

    /**
     * Demonstrates object mapping for type-safe result processing.
     */
    private void demonstrateObjectMapping(IgniteSql sql) {
        System.out.println("\n--- Object Mapping");
        System.out.println(">>> Using type-safe object mapping");
        
        // Single-column mapping for artist names
        ResultSet<String> artistNames = sql.execute(null,
                Mapper.of(String.class, "Name"),
                "SELECT Name FROM Artist WHERE ArtistId <= 5 ORDER BY Name");

        System.out.println("<<< Artist names using single-column mapping:");
        artistNames.forEachRemaining(name -> 
            System.out.println("         " + name));
        
        // Numeric aggregation mapping
        ResultSet<Long> trackCounts = sql.execute(null,
                Mapper.of(Long.class, "track_count"),
                "SELECT COUNT(*) as track_count FROM Track");

        if (trackCounts.hasNext()) {
            Long totalTracks = trackCounts.next();
            System.out.println("<<< Total tracks using numeric mapping: " + totalTracks);
        }
        
        // Price range mapping
        ResultSet<String> priceRanges = sql.execute(null,
                Mapper.of(String.class, "price_range"),
                """
                SELECT CASE 
                    WHEN UnitPrice < 1.0 THEN 'Budget'
                    WHEN UnitPrice < 2.0 THEN 'Standard' 
                    ELSE 'Premium'
                END as price_range
                FROM Track 
                GROUP BY price_range 
                ORDER BY price_range
                """);
        
        System.out.println("<<< Available price ranges:");
        priceRanges.forEachRemaining(range -> 
            System.out.println("         " + range));
    }
}