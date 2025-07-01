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

package com.apache.ignite.examples.performance;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.sql.Statement;

/**
 * Query execution plan analysis for Apache Ignite 3 performance optimization.
 * 
 * Demonstrates using EXPLAIN PLAN FOR to understand query performance characteristics
 * and identify optimization opportunities. Shows how to analyze execution plans for
 * different query patterns and formulations in distributed music platform scenarios.
 * 
 * Key concepts demonstrated:
 * - EXPLAIN PLAN FOR syntax and result processing
 * - Execution plan comparison for different query approaches
 * - Join algorithm identification and optimization
 * - Index usage analysis and table scan detection
 * 
 * Business context:
 * Music streaming platforms need to understand why some queries execute in milliseconds
 * while others take seconds. This class shows how to use execution plan analysis
 * to identify bottlenecks and guide optimization decisions for artist searches,
 * customer analytics, and complex aggregation queries.
 */
public class QueryExecutionPlanAnalysis {
    
    private final IgniteSql sql;
    
    public QueryExecutionPlanAnalysis(IgniteClient client) {
        this.sql = client.sql();
    }
    
    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Query Execution Plan Analysis Demo ===");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            QueryExecutionPlanAnalysis analyzer = new QueryExecutionPlanAnalysis(client);
            analyzer.demonstrateBasicPlanAnalysis();
            analyzer.compareQueryFormulations();
            analyzer.analyzeComplexAnalyticsPlans();
            analyzer.demonstrateOptimizationWorkflow();
            
        } catch (Exception e) {
            System.err.println("Execution plan analysis failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrate basic execution plan analysis for artist search queries.
     */
    public void demonstrateBasicPlanAnalysis() {
        System.out.println("\n--- Basic Execution Plan Analysis");
        
        Statement artistSearchQuery = sql.statementBuilder()
            .query("SELECT ar.ArtistId, ar.Name, COUNT(al.AlbumId) as AlbumCount " +
                   "FROM Artist ar " +
                   "LEFT JOIN Album al ON ar.ArtistId = al.ArtistId " +
                   "WHERE LOWER(ar.Name) LIKE LOWER(?) " +
                   "GROUP BY ar.ArtistId, ar.Name " +
                   "ORDER BY ar.Name")
            .build();
        
        System.out.println("=== Artist Search Query Execution Plan ===");
        analyzeQueryExecutionPlan(artistSearchQuery, "%rock%");
        
        System.out.println("\nKey observations to look for:");
        System.out.println("- Table scan vs index scan operations");
        System.out.println("- Join algorithm selection (nested loop, hash, merge)");
        System.out.println("- Filter pushdown effectiveness");
        System.out.println("- Sort operation placement and cost");
        System.out.println("- Network data movement characteristics");
    }
    
    /**
     * Compare execution plans for different query formulations.
     */
    public void compareQueryFormulations() {
        System.out.println("\n--- Query Formulation Comparison");
        
        // Approach 1: Subquery with EXISTS
        Statement existsQuery = sql.statementBuilder()
            .query("SELECT ar.ArtistId, ar.Name " +
                   "FROM Artist ar " +
                   "WHERE EXISTS (" +
                   "    SELECT 1 FROM Album al " +
                   "    JOIN Track t ON al.AlbumId = t.AlbumId " +
                   "    WHERE al.ArtistId = ar.ArtistId " +
                   "    AND t.UnitPrice > ?" +
                   ") ORDER BY ar.Name")
            .build();
        
        // Approach 2: JOIN with DISTINCT
        Statement joinQuery = sql.statementBuilder()
            .query("SELECT DISTINCT ar.ArtistId, ar.Name " +
                   "FROM Artist ar " +
                   "JOIN Album al ON ar.ArtistId = al.ArtistId " +
                   "JOIN Track t ON al.AlbumId = t.AlbumId " +
                   "WHERE t.UnitPrice > ? " +
                   "ORDER BY ar.Name")
            .build();
        
        // Approach 3: Aggregation with MAX
        Statement aggregationQuery = sql.statementBuilder()
            .query("SELECT ar.ArtistId, ar.Name, MAX(t.UnitPrice) as HighestTrackPrice " +
                   "FROM Artist ar " +
                   "JOIN Album al ON ar.ArtistId = al.ArtistId " +
                   "JOIN Track t ON al.AlbumId = t.AlbumId " +
                   "WHERE t.UnitPrice > ? " +
                   "GROUP BY ar.ArtistId, ar.Name " +
                   "ORDER BY ar.Name")
            .build();
        
        System.out.println("\n1. EXISTS Subquery Approach:");
        analyzeQueryExecutionPlan(existsQuery, 1.0);
        
        System.out.println("\n2. JOIN with DISTINCT Approach:");
        analyzeQueryExecutionPlan(joinQuery, 1.0);
        
        System.out.println("\n3. Aggregation Approach:");
        analyzeQueryExecutionPlan(aggregationQuery, 1.0);
        
        System.out.println("\n--- Comparison Guidelines ---");
        System.out.println("Evaluate plans based on:");
        System.out.println("- Estimated row counts at each step");
        System.out.println("- Join algorithm efficiency");
        System.out.println("- Sort and aggregation placement");
        System.out.println("- Overall execution cost estimates");
    }
    
    /**
     * Analyze execution plans for complex analytics queries.
     */
    public void analyzeComplexAnalyticsPlans() {
        System.out.println("\n--- Complex Analytics Query Plans");
        
        Statement complexAnalyticsQuery = sql.statementBuilder()
            .query("SELECT g.GenreId, g.Name as GenreName, " +
                   "COUNT(il.InvoiceLineId) as SalesCount, " +
                   "SUM(il.Quantity * il.UnitPrice) as Revenue, " +
                   "COUNT(DISTINCT i.CustomerId) as UniqueCustomers " +
                   "FROM Genre g " +
                   "JOIN Track t ON g.GenreId = t.GenreId " +
                   "JOIN InvoiceLine il ON t.TrackId = il.TrackId " +
                   "JOIN Invoice i ON il.InvoiceId = i.InvoiceId " +
                   "GROUP BY g.GenreId, g.Name " +
                   "ORDER BY Revenue DESC LIMIT 10")
            .build();
        
        System.out.println("=== Complex Analytics Query Execution Plan ===");
        analyzeQueryExecutionPlan(complexAnalyticsQuery);
        
        System.out.println("\n--- Complex Query Plan Analysis ---");
        System.out.println("Advanced optimization indicators:");
        System.out.println("- Multi-table join optimization");
        System.out.println("- Aggregation execution strategy");
        System.out.println("- Hash vs sort-based operations");
        System.out.println("- Partition pruning effectiveness");
        System.out.println("- Memory usage for intermediate results");
    }
    
    /**
     * Demonstrate systematic query optimization workflow using execution plans.
     */
    public void demonstrateOptimizationWorkflow() {
        System.out.println("\n--- Query Optimization Workflow");
        
        System.out.println("\n=== Step-by-Step Optimization Process ===");
        
        System.out.println("\n1. Baseline Query Analysis");
        System.out.println("   - Run EXPLAIN PLAN FOR on current query");
        System.out.println("   - Identify table scans and expensive operations");
        System.out.println("   - Note join algorithms and sort operations");
        
        System.out.println("\n2. Index Impact Assessment");
        System.out.println("   - Create candidate indexes");
        System.out.println("   - Re-run EXPLAIN PLAN FOR with indexes");
        System.out.println("   - Compare execution plans before/after");
        
        System.out.println("\n3. Query Restructuring Evaluation");
        System.out.println("   - Test alternative query formulations");
        System.out.println("   - Analyze plans for each approach");
        System.out.println("   - Select approach with optimal plan");
        
        System.out.println("\n4. Zone Configuration Validation");
        System.out.println("   - Verify partition pruning effectiveness");
        System.out.println("   - Check parallel execution utilization");
        System.out.println("   - Validate data locality optimizations");
        
        System.out.println("\n5. Performance Verification");
        System.out.println("   - Execute optimized query with timing");
        System.out.println("   - Compare actual vs estimated costs");
        System.out.println("   - Monitor resource utilization patterns");
        
        System.out.println("\n=== Execution Plan Key Indicators ===");
        System.out.println("✓ Index scans instead of table scans");
        System.out.println("✓ Hash joins for large datasets");
        System.out.println("✓ Merge joins for sorted inputs");
        System.out.println("✓ Early filtering (predicate pushdown)");
        System.out.println("✓ Parallel execution across partitions");
        System.out.println("✓ Minimal data movement between nodes");
    }
    
    /**
     * Execute EXPLAIN PLAN FOR and analyze the execution plan.
     */
    private void analyzeQueryExecutionPlan(Statement query, Object... params) {
        try {
            Statement explainQuery = sql.statementBuilder()
                .query("EXPLAIN PLAN FOR " + query.query())
                .build();
                
            try (ResultSet<SqlRow> planResults = sql.execute(null, explainQuery, params)) {
                
                System.out.println("Query Execution Plan:");
                int stepNumber = 1;
                
                while (planResults.hasNext()) {
                    SqlRow planRow = planResults.next();
                    
                    // The exact column names may vary based on Ignite 3 implementation
                    // Common execution plan information typically includes:
                    String planStep = planRow.stringValue(0); // Plan step description
                    
                    System.out.printf("%2d. %s%n", stepNumber++, planStep);
                }
                
                if (stepNumber == 1) {
                    System.out.println(">>> No execution plan returned - check EXPLAIN syntax");
                }
                
            }
            
        } catch (Exception e) {
            System.err.println("Failed to analyze execution plan: " + e.getMessage());
            System.err.println("Note: EXPLAIN PLAN FOR syntax may vary by Ignite 3 version");
            
            // Fallback: show the query that would be analyzed
            System.out.println("Query to be analyzed:");
            System.out.println(query.query());
        }
    }
}