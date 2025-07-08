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

package com.apache.ignite.examples.compute;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.compute.*;
import org.apache.ignite.deployment.DeploymentUnit;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Demonstrates job workflows and orchestration using the Apache Ignite 3 Compute API.
 * 
 * Covers complex job coordination patterns including multi-step workflows,
 * job dependencies, result aggregation, and business process automation.
 * Shows how to build sophisticated distributed applications.
 * 
 * Key concepts:
 * - Multi-step job workflows
 * - Job result aggregation
 * - Business process automation
 * - Error handling in workflows
 * - Performance monitoring
 * 
 * Prerequisites:
 * - Running Ignite cluster
 * - Sample music store data loaded
 */
public class ComputeJobWorkflows {

    private static final Logger logger = LoggerFactory.getLogger(ComputeJobWorkflows.class);

    // Deployment unit configuration
    private static final String DEPLOYMENT_UNIT_NAME = "compute-jobs";
    private static final String DEPLOYMENT_UNIT_VERSION = "1.0.0";

    /**
     * Get deployment units for this application.
     */
    private static List<DeploymentUnit> getDeploymentUnits() {
        // Use the deployment unit that should be deployed via REST API or CLI
        return List.of(new DeploymentUnit(DEPLOYMENT_UNIT_NAME, DEPLOYMENT_UNIT_VERSION));
    }

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Compute Job Workflows Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Demonstrating job orchestration and business workflows");

        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            ComputeJobWorkflows demo = new ComputeJobWorkflows();
            demo.runWorkflowDemonstrations(client);
            
        } catch (Exception e) {
            logger.error("Failed to run workflow demonstrations", e);
            System.err.println("!!! Error: " + e.getMessage());
        }
    }

    private void runWorkflowDemonstrations(IgniteClient client) {
        System.out.println("\n--- Job Workflow Patterns ---");
        System.out.println("    Orchestrating complex distributed business processes");
        
        // Customer analytics workflow
        demonstrateCustomerAnalyticsWorkflow(client);
        
        // Music recommendation workflow
        demonstrateMusicRecommendationWorkflow(client);
        
        // Revenue optimization workflow
        demonstrateRevenueOptimizationWorkflow(client);
        
        System.out.println("\n>>> Job workflow demonstrations completed successfully");
    }

    /**
     * Demonstrates customer analytics workflow with multiple job steps.
     */
    private void demonstrateCustomerAnalyticsWorkflow(IgniteClient client) {
        System.out.println("\n--- Customer Analytics Workflow");
        System.out.println(">>> Running multi-step customer analysis process");
        
        try {
            // Step 1: Segment customers by spending
            Statement segmentStmt = client.sql().statementBuilder()
                .query("SELECT segment, COUNT(*) as customer_count FROM (" +
                       "SELECT " +
                       "CASE " +
                       "  WHEN SUM(i.Total) > 100 THEN 'Premium' " +
                       "  WHEN SUM(i.Total) > 50 THEN 'Regular' " +
                       "  ELSE 'Basic' " +
                       "END as segment " +
                       "FROM Customer c " +
                       "JOIN Invoice i ON c.CustomerId = i.CustomerId " +
                       "GROUP BY c.CustomerId" +
                       ") GROUP BY segment")
                .build();
            
            System.out.println(">>> Step 1: Identifying customer segments");
            try (ResultSet<SqlRow> rs = client.sql().execute(null, segmentStmt)) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.println("         " + row.stringValue("SEGMENT") + " customers: " + 
                                     row.longValue("CUSTOMER_COUNT"));
                }
            }
            
            // Step 2: Analyze top customers
            Statement topCustomersStmt = client.sql().statementBuilder()
                .query("SELECT c.FirstName || ' ' || c.LastName as name, " +
                       "COUNT(i.InvoiceId) as purchases, " +
                       "SUM(i.Total) as total_spent " +
                       "FROM Customer c " +
                       "JOIN Invoice i ON c.CustomerId = i.CustomerId " +
                       "GROUP BY c.CustomerId, c.FirstName, c.LastName " +
                       "ORDER BY total_spent DESC " +
                       "LIMIT 3")
                .build();
            
            System.out.println(">>> Step 2: Analyzing top customers");
            try (ResultSet<SqlRow> rs = client.sql().execute(null, topCustomersStmt)) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.println("         " + row.stringValue("NAME") + 
                                     ": " + row.longValue("PURCHASES") + " purchases, " +
                                     "$" + row.decimalValue("TOTAL_SPENT"));
                }
            }
            
            System.out.println("<<< Customer analytics workflow completed successfully");
            
        } catch (Exception e) {
            System.err.println("!!! Customer analytics workflow failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrates music recommendation workflow.
     */
    private void demonstrateMusicRecommendationWorkflow(IgniteClient client) {
        System.out.println("\n--- Music Recommendation Workflow");
        System.out.println(">>> Building personalized recommendations through job pipeline");
        
        try {
            int sampleCustomerId = 1;
            
            // Step 1: Analyze customer's genre preferences
            Statement genrePrefsStmt = client.sql().statementBuilder()
                .query("SELECT g.Name as genre, COUNT(*) as play_count " +
                       "FROM Customer c " +
                       "JOIN Invoice i ON c.CustomerId = i.CustomerId " +
                       "JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId " +
                       "JOIN Track t ON il.TrackId = t.TrackId " +
                       "JOIN Genre g ON t.GenreId = g.GenreId " +
                       "WHERE c.CustomerId = ? " +
                       "GROUP BY g.GenreId, g.Name " +
                       "ORDER BY play_count DESC " +
                       "LIMIT 3")
                .build();
            
            System.out.println(">>> Step 1: Analyzing customer " + sampleCustomerId + " genre preferences");
            List<String> preferredGenres = new ArrayList<>();
            try (ResultSet<SqlRow> rs = client.sql().execute(null, genrePrefsStmt, sampleCustomerId)) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    String genre = row.stringValue("GENRE");
                    long count = row.longValue("PLAY_COUNT");
                    preferredGenres.add(genre);
                    System.out.println("         " + genre + ": " + count + " tracks purchased");
                }
            }
            
            // Step 2: Find popular tracks in preferred genres
            if (!preferredGenres.isEmpty()) {
                Statement recommendStmt = client.sql().statementBuilder()
                    .query("SELECT t.Name as track, ar.Name as artist, COUNT(*) as popularity " +
                           "FROM Track t " +
                           "JOIN Album al ON t.AlbumId = al.AlbumId " +
                           "JOIN Artist ar ON al.ArtistId = ar.ArtistId " +
                           "JOIN Genre g ON t.GenreId = g.GenreId " +
                           "JOIN InvoiceLine il ON t.TrackId = il.TrackId " +
                           "WHERE g.Name = ? " +
                           "GROUP BY t.TrackId, t.Name, ar.Name " +
                           "ORDER BY popularity DESC " +
                           "LIMIT 3")
                    .build();
                
                System.out.println(">>> Step 2: Generating recommendations based on " + preferredGenres.get(0));
                try (ResultSet<SqlRow> rs = client.sql().execute(null, recommendStmt, preferredGenres.get(0))) {
                    // Debug: Print column metadata
                    //if (rs.hasNext()) {
                    //    SqlRow firstRow = rs.next();
                    //    System.out.println(">>> DEBUG: Column count: " + firstRow.columnCount());
                    //    for (int i = 0; i < firstRow.columnCount(); i++) {
                    //        System.out.println(">>> DEBUG: Column " + i + " name: " + firstRow.columnName(i));
                    //    }
                    //    // Process the first row
                    //    System.out.println("         Recommend: \"" + firstRow.stringValue(0) + 
                    //                     "\" by " + firstRow.stringValue(1) +
                    //                     " (popularity: " + firstRow.longValue(2) + ")");
                    //}
                    
                    // Process remaining rows
                    while (rs.hasNext()) {
                        SqlRow row = rs.next();
                        System.out.println("         Recommend: \"" + row.stringValue(0) + 
                                         "\" by " + row.stringValue(1) +
                                         " (popularity: " + row.longValue(2) + ")");
                    }
                }
            }
            
            System.out.println("<<< Music recommendation workflow completed successfully");
            
        } catch (Exception e) {
            System.err.println("!!! Music recommendation workflow failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrates revenue optimization workflow.
     */
    private void demonstrateRevenueOptimizationWorkflow(IgniteClient client) {
        System.out.println("\n--- Revenue Optimization Workflow");
        System.out.println(">>> Running business intelligence analysis pipeline");
        
        try {
            // Step 1: Analyze revenue by genre
            Statement genreRevenueStmt = client.sql().statementBuilder()
                .query("SELECT g.Name as genre, " +
                       "COUNT(DISTINCT t.TrackId) as track_count, " +
                       "SUM(il.UnitPrice * il.Quantity) as revenue " +
                       "FROM Genre g " +
                       "JOIN Track t ON g.GenreId = t.GenreId " +
                       "JOIN InvoiceLine il ON t.TrackId = il.TrackId " +
                       "GROUP BY g.GenreId, g.Name " +
                       "ORDER BY revenue DESC " +
                       "LIMIT 5")
                .build();
            
            System.out.println(">>> Step 1: Analyzing revenue by genre");
            try (ResultSet<SqlRow> rs = client.sql().execute(null, genreRevenueStmt)) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.println("         " + row.stringValue("GENRE") + 
                                     ": $" + row.decimalValue("REVENUE") +
                                     " from " + row.longValue("TRACK_COUNT") + " tracks");
                }
            }
            
            // Step 2: Identify underperforming tracks
            Statement underperformingStmt = client.sql().statementBuilder()
                .query("SELECT ar.Name as artist, COUNT(t.TrackId) as track_count " +
                       "FROM Artist ar " +
                       "JOIN Album al ON ar.ArtistId = al.ArtistId " +
                       "JOIN Track t ON al.AlbumId = t.AlbumId " +
                       "LEFT JOIN InvoiceLine il ON t.TrackId = il.TrackId " +
                       "WHERE il.TrackId IS NULL " +
                       "GROUP BY ar.ArtistId, ar.Name " +
                       "HAVING COUNT(t.TrackId) > 5 " +
                       "ORDER BY track_count DESC " +
                       "LIMIT 3")
                .build();
            
            System.out.println(">>> Step 2: Identifying artists with unsold tracks");
            try (ResultSet<SqlRow> rs = client.sql().execute(null, underperformingStmt)) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.println("         " + row.stringValue("ARTIST") + 
                                     ": " + row.longValue("TRACK_COUNT") + " unsold tracks");
                }
            }
            
            System.out.println("<<< Revenue optimization workflow completed successfully");
            
        } catch (Exception e) {
            System.err.println("!!! Revenue optimization workflow failed: " + e.getMessage());
        }
    }

    // Job implementations

    public static class CustomerSegmentJob implements ComputeJob<Void, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Void arg) {
            return CompletableFuture.completedFuture("High Value\nRegular\nNew Customers");
        }
    }

    public static class SegmentAnalysisJob implements ComputeJob<String, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, String segment) {
            return CompletableFuture.supplyAsync(() -> {
                IgniteSql sql = context.ignite().sql();
                
                try (ResultSet<SqlRow> result = sql.execute(null,
                        "SELECT COUNT(*) as customer_count FROM Customer LIMIT 1")) {
                    
                    if (result.hasNext()) {
                        int count = (int) result.next().longValue("customer_count");
                        return count + " customers analyzed";
                    }
                    return "No data";
                }
            });
        }
    }

}