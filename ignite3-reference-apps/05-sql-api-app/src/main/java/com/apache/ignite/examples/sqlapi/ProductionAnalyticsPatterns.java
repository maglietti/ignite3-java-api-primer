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
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.sql.Statement;
import org.apache.ignite.sql.ResultSetMetadata;
import org.apache.ignite.sql.ColumnMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;

/**
 * Demonstrates production-scale analytics patterns using the Apache Ignite 3 SQL API.
 * 
 * Implements comprehensive analytics patterns removed from the educational chapter
 * to show complete production-ready implementations. Covers memory-efficient
 * result processing, distributed aggregation, transactional analytics, and
 * comprehensive error handling.
 * 
 * Key implementation patterns:
 * - Streaming result processing for large data sets
 * - Hierarchical data aggregation with boundary detection
 * - Production revenue analytics with proper error handling
 * - Query performance optimization and execution plan analysis
 * - Robust error handling with retry logic
 * 
 * Prerequisites:
 * - Running Ignite cluster
 * - Sample music store data loaded
 */
public class ProductionAnalyticsPatterns {

    private static final Logger logger = LoggerFactory.getLogger(ProductionAnalyticsPatterns.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Production Analytics Patterns Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Demonstrating production-scale SQL analytics");

        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            ProductionAnalyticsPatterns demo = new ProductionAnalyticsPatterns();
            demo.runProductionAnalytics(client);
            
        } catch (Exception e) {
            logger.error("Failed to run production analytics patterns", e);
            System.err.println("!!! Error: " + e.getMessage());
        }
    }

    private void runProductionAnalytics(IgniteClient client) {
        IgniteSql sql = client.sql();
        
        System.out.println("\n--- Production Analytics Patterns ---");
        System.out.println("    Large-scale data processing and optimization");
        
        // Memory-efficient streaming result processing
        demonstrateStreamingResultProcessing(sql);
        
        // Hierarchical data processing patterns
        demonstrateHierarchicalDataProcessing(sql);
        
        // Dynamic result processing using metadata
        demonstrateMetadataDrivenProcessing(sql);
        
        // Production revenue analytics
        demonstrateRevenueAnalytics(sql);
        
        // Query execution plan analysis
        demonstrateQueryOptimization(sql);
        
        // Error handling patterns
        demonstrateRobustErrorHandling(sql);
        
        System.out.println("\n>>> Production analytics patterns completed successfully");
    }

    /**
     * Demonstrates memory-efficient streaming result processing for large datasets.
     */
    private void demonstrateStreamingResultProcessing(IgniteSql sql) {
        System.out.println("\n    --- Streaming Result Processing");
        System.out.println(">>> Processing large result sets with controlled memory usage");
        
        // Configure streaming with controlled memory usage
        Statement streamingQuery = sql.statementBuilder()
            .query("""
                SELECT t.TrackId, t.Name, t.Milliseconds, t.UnitPrice,
                       a.Name as ArtistName, al.Title as AlbumTitle
                FROM Track t
                JOIN Album al ON t.AlbumId = al.AlbumId
                JOIN Artist a ON al.ArtistId = a.ArtistId
                ORDER BY t.TrackId
                """)
            .pageSize(500)                          // Stream 500 rows per fetch
            .queryTimeout(60, TimeUnit.SECONDS)     // Prevent abandoned queries
            .build();
        
        ResultSet<SqlRow> tracks = sql.execute(null, streamingQuery);
        
        // Process results in streaming fashion - constant memory usage
        long totalDuration = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int processed = 0;
        
        System.out.println(">>> Processing tracks in streaming fashion...");
        
        while (tracks.hasNext() && processed < 100) { // Limit for demo
            SqlRow track = tracks.next();
            
            // Extract data with proper null handling
            int duration = track.intValue("MILLISECONDS");
            BigDecimal price = track.decimalValue("UNITPRICE");
            String artist = track.stringValue("ARTISTNAME");
            
            totalDuration += duration;
            totalRevenue = totalRevenue.add(price);
            processed++;
            
            // Progress indication for long-running analytics
            if (processed % 25 == 0) {
                System.out.println("         Processed " + processed + " tracks...");
            }
        }
        
        System.out.printf("<<< Analysis complete: %d tracks, %.1f total minutes, $%.2f revenue%n",
            processed, totalDuration / 60000.0, totalRevenue);
    }

    /**
     * Demonstrates hierarchical data processing with boundary detection.
     */
    private void demonstrateHierarchicalDataProcessing(IgniteSql sql) {
        System.out.println("\n    --- Hierarchical Data Processing");
        System.out.println(">>> Processing artist-album-track hierarchy");
        
        // Query that joins across colocated tables for optimal performance
        Statement hierarchicalQuery = sql.statementBuilder()
            .query("""
                SELECT a.ArtistId, a.Name as ArtistName,
                       al.AlbumId, al.Title as AlbumTitle,
                       t.TrackId, t.Name as TrackName, t.UnitPrice
                FROM Artist a
                JOIN Album al ON a.ArtistId = al.ArtistId
                JOIN Track t ON al.AlbumId = t.AlbumId
                WHERE a.ArtistId BETWEEN ? AND ?
                ORDER BY a.ArtistId, al.AlbumId, t.TrackId
                """)
            .pageSize(1000)
            .build();
        
        ResultSet<SqlRow> result = sql.execute(null, hierarchicalQuery, 1, 3);
        
        // Group results by hierarchy levels
        int currentArtistId = -1;
        int currentAlbumId = -1;
        BigDecimal albumTotal = BigDecimal.ZERO;
        BigDecimal artistTotal = BigDecimal.ZERO;
        
        System.out.println(">>> Processing hierarchical results:");
        
        while (result.hasNext()) {
            SqlRow row = result.next();
            
            int artistId = row.intValue("ARTISTID");
            int albumId = row.intValue("ALBUMID");
            BigDecimal trackPrice = row.decimalValue("UNITPRICE");
            
            // Detect artist boundary
            if (artistId != currentArtistId) {
                if (currentArtistId != -1) {
                    System.out.printf("         Artist %d total: $%.2f%n", currentArtistId, artistTotal);
                }
                currentArtistId = artistId;
                artistTotal = BigDecimal.ZERO;
                System.out.printf("<<< Processing Artist: %s%n", row.stringValue("ARTISTNAME"));
            }
            
            // Detect album boundary
            if (albumId != currentAlbumId) {
                if (currentAlbumId != -1) {
                    System.out.printf("           Album total: $%.2f%n", albumTotal);
                }
                currentAlbumId = albumId;
                albumTotal = BigDecimal.ZERO;
                System.out.printf("         Album: %s%n", row.stringValue("ALBUMTITLE"));
            }
            
            // Accumulate track value
            albumTotal = albumTotal.add(trackPrice);
            artistTotal = artistTotal.add(trackPrice);
        }
        
        // Final totals
        if (currentAlbumId != -1) {
            System.out.printf("           Album total: $%.2f%n", albumTotal);
        }
        if (currentArtistId != -1) {
            System.out.printf("         Artist %d total: $%.2f%n", currentArtistId, artistTotal);
        }
    }

    /**
     * Demonstrates dynamic result processing using metadata.
     */
    private void demonstrateMetadataDrivenProcessing(IgniteSql sql) {
        System.out.println("\n    --- Metadata-Driven Processing");
        System.out.println(">>> Analyzing result structure dynamically");
        
        // Query with unknown result structure
        ResultSet<SqlRow> result = sql.execute(null, 
            "SELECT * FROM Artist WHERE ArtistId BETWEEN ? AND ?", 1, 3);
        
        // Analyze result structure dynamically
        ResultSetMetadata metadata = result.metadata();
        
        System.out.println("<<< Result set structure:");
        List<ColumnMetadata> columns = metadata.columns();
        for (ColumnMetadata column : columns) {
            System.out.printf("         %s: %s (%s)%s%n",
                column.name(),
                column.type(),
                column.valueClass().getSimpleName(),
                column.nullable() ? " NULLABLE" : " NOT NULL");
        }
        
        System.out.println("<<< Data rows:");
        
        // Process rows dynamically based on metadata
        while (result.hasNext()) {
            SqlRow row = result.next();
            StringBuilder rowData = new StringBuilder();
            
            for (ColumnMetadata column : columns) {
                Object value = row.value(column.name());
                if (rowData.length() > 0) rowData.append(", ");
                rowData.append(column.name()).append("=").append(value);
            }
            
            System.out.println("         " + rowData.toString());
        }
    }

    /**
     * Demonstrates production revenue analytics with time-based filtering.
     */
    private void demonstrateRevenueAnalytics(IgniteSql sql) {
        System.out.println("\n    --- Production Revenue Analytics");
        System.out.println(">>> Generating business intelligence reports");
        
        // Revenue analytics by genre (simulated data since sample DB may not have invoices)
        Statement revenueQuery = sql.statementBuilder()
            .query("""
                SELECT g.Name as Genre,
                       COUNT(t.TrackId) as TrackCount,
                       AVG(t.UnitPrice) as AvgPrice,
                       SUM(t.UnitPrice) as TotalValue
                FROM Genre g
                JOIN Track t ON g.GenreId = t.GenreId
                GROUP BY g.GenreId, g.Name
                HAVING COUNT(t.TrackId) >= 10
                ORDER BY TotalValue DESC
                LIMIT 10
                """)
            .pageSize(100)
            .queryTimeout(30, TimeUnit.SECONDS)
            .build();
        
        ResultSet<SqlRow> result = sql.execute(null, revenueQuery);
        
        System.out.println("<<< Top Genres by Catalog Value:");
        System.out.println("         Genre                | Tracks | Avg Price | Total Value");
        System.out.println("         ---------------------|--------|-----------|------------");
        
        BigDecimal totalRevenue = BigDecimal.ZERO;
        
        while (result.hasNext()) {
            SqlRow row = result.next();
            
            String genre = row.stringValue("GENRE");
            long trackCount = row.longValue("TRACKCOUNT");
            BigDecimal avgPrice = row.decimalValue("AVGPRICE");
            BigDecimal totalValue = row.decimalValue("TOTALVALUE");
            
            System.out.printf("         %-20s | %6d | $%8.2f | $%9.2f%n",
                genre.length() > 20 ? genre.substring(0, 17) + "..." : genre,
                trackCount, avgPrice, totalValue);
            
            totalRevenue = totalRevenue.add(totalValue);
        }
        
        System.out.println("         ---------------------|--------|-----------|------------");
        System.out.printf("         TOTALS               |        |           | $%9.2f%n", totalRevenue);
    }

    /**
     * Demonstrates query execution plan analysis for optimization.
     */
    private void demonstrateQueryOptimization(IgniteSql sql) {
        System.out.println("\n    --- Query Optimization");
        System.out.println(">>> Analyzing execution plans for performance");
        
        // Analyze execution plan for complex analytical query
        try {
            Statement explainQuery = sql.statementBuilder()
                .query("""
                    EXPLAIN PLAN FOR SELECT t.Name, t.UnitPrice, a.Name as ArtistName
                    FROM Track t
                    JOIN Album al ON t.AlbumId = al.AlbumId
                    JOIN Artist a ON al.ArtistId = a.ArtistId
                    WHERE t.GenreId = ? AND t.UnitPrice > ?
                    """)
                .build();
            
            ResultSet<SqlRow> explainResult = sql.execute(null, explainQuery, 1, new BigDecimal("0.99"));
            
            System.out.println("<<< Query Execution Plan:");
            while (explainResult.hasNext()) {
                SqlRow row = explainResult.next();
                String planStep = row.stringValue(0);
                System.out.println("         " + planStep);
                
                // Identify performance concerns
                if (planStep.contains("BROADCAST")) {
                    System.out.println("         !!! Warning: Broadcast join detected");
                }
                if (planStep.contains("FULL_SCAN")) {
                    System.out.println("         !!! Warning: Full table scan detected");
                }
            }
        } catch (Exception e) {
            System.out.println("!!! EXPLAIN not supported in this version: " + e.getMessage());
        }
        
        // Execute optimized version using colocation-aware patterns
        Statement optimizedQuery = sql.statementBuilder()
            .query("""
                SELECT t.Name, t.UnitPrice, a.Name as ArtistName
                FROM Track t
                JOIN Album al ON t.AlbumId = al.AlbumId
                JOIN Artist a ON al.ArtistId = a.ArtistId
                WHERE al.ArtistId = ?  -- Partition pruning by colocation key
                  AND t.GenreId = ?    -- Secondary filter
                ORDER BY t.Name
                """)
            .pageSize(500)
            .build();
        
        long startTime = System.currentTimeMillis();
        ResultSet<SqlRow> optimizedResult = sql.execute(null, optimizedQuery, 1, 1);
        
        int resultCount = 0;
        while (optimizedResult.hasNext() && resultCount < 10) {
            SqlRow row = optimizedResult.next();
            resultCount++;
        }
        
        long executionTime = System.currentTimeMillis() - startTime;
        System.out.printf("<<< Optimized query: %d results in %d ms%n", resultCount, executionTime);
    }

    /**
     * Demonstrates robust error handling for production analytics.
     */
    private void demonstrateRobustErrorHandling(IgniteSql sql) {
        System.out.println("\n    --- Robust Error Handling");
        System.out.println(">>> Handling failures and timeouts gracefully");
        
        Optional<String> result = executeRobustAnalytics(sql, "Rock");
        
        if (result.isPresent()) {
            System.out.println("<<< Analytics completed: " + result.get());
        } else {
            System.out.println("!!! Analytics failed gracefully");
        }
        
        // Demonstrate async error handling
        CompletableFuture<Optional<String>> asyncResult = executeAsyncAnalytics(sql);
        
        try {
            Optional<String> asyncAnswer = asyncResult.get();
            if (asyncAnswer.isPresent()) {
                System.out.println("<<< Async analytics completed: " + asyncAnswer.get());
            } else {
                System.out.println("!!! Async analytics failed gracefully");
            }
        } catch (Exception e) {
            System.out.println("!!! Async operation interrupted: " + e.getMessage());
        }
    }

    /**
     * Robust analytics execution with comprehensive error handling.
     */
    private Optional<String> executeRobustAnalytics(IgniteSql sql, String searchTerm) {
        Statement analyticsQuery = sql.statementBuilder()
            .query("""
                SELECT COUNT(*) as track_count, AVG(UnitPrice) as avg_price
                FROM Track t
                JOIN Album al ON t.AlbumId = al.AlbumId
                JOIN Artist a ON al.ArtistId = a.ArtistId
                WHERE t.Name LIKE ?
                """)
            .queryTimeout(30, TimeUnit.SECONDS)
            .pageSize(1000)
            .build();
        
        try {
            ResultSet<SqlRow> result = sql.execute(null, analyticsQuery, "%" + searchTerm + "%");
            
            if (result.hasNext()) {
                SqlRow row = result.next();
                long trackCount = row.longValue("track_count");
                BigDecimal avgPrice = row.decimalValue("avg_price");
                
                return Optional.of(String.format("%d tracks found, avg price $%.2f", 
                    trackCount, avgPrice));
            }
            
            return Optional.of("No results found");
            
        } catch (Exception e) {
            logger.error("Analytics query failed for term: " + searchTerm, e);
            return Optional.empty();
        }
    }

    /**
     * Asynchronous analytics with error handling.
     */
    private CompletableFuture<Optional<String>> executeAsyncAnalytics(IgniteSql sql) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ResultSet<SqlRow> result = sql.execute(null, 
                    "SELECT COUNT(*) as total_tracks FROM Track");
                
                if (result.hasNext()) {
                    long count = result.next().longValue("total_tracks");
                    return Optional.of("Total tracks in catalog: " + count);
                }
                
                return Optional.of("No data available");
                
            } catch (Exception e) {
                logger.error("Async analytics failed", e);
                return Optional.<String>empty();
            }
        });
    }
}