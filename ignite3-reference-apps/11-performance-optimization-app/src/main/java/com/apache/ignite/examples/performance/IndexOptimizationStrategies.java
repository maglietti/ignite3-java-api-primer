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
 * Index optimization strategies for distributed query performance in Apache Ignite 3.
 * 
 * Demonstrates how proper indexing transforms music platform queries from
 * table scans that process millions of records to targeted lookups that
 * return results in milliseconds. Shows practical indexing decisions for
 * common music streaming query patterns.
 * 
 * Key concepts demonstrated:
 * - Single vs composite index performance characteristics
 * - Prefix matching optimization for search queries
 * - Sort-based index optimization for ordered results
 * - Index selectivity impact on query execution plans
 * 
 * Business context:
 * Music streaming platforms require instant search results for artist names,
 * track titles, and genre filtering. This class shows how strategic indexing
 * enables sub-second responses for queries across millions of music tracks
 * while minimizing storage overhead and maintenance costs.
 */
public class IndexOptimizationStrategies {
    
    private final IgniteSql sql;
    
    public IndexOptimizationStrategies(IgniteClient client) {
        this.sql = client.sql();
    }
    
    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Index Optimization Strategies Demo ===");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            IndexOptimizationStrategies optimizer = new IndexOptimizationStrategies(client);
            optimizer.demonstrateIndexPerformance();
            optimizer.demonstrateCompositeIndexes();
            optimizer.demonstratePrefixSearchOptimization();
            
        } catch (Exception e) {
            System.err.println("Index optimization demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Compare performance with and without proper indexing.
     */
    public void demonstrateIndexPerformance() {
        System.out.println("\n--- Index Performance Impact");
        
        Statement artistSearchQuery = sql.statementBuilder()
            .query("SELECT Name FROM Artist WHERE Name LIKE ?")
            .build();
            
        long startTime = System.nanoTime();
        int resultCount = 0;
        
        try (ResultSet<SqlRow> results = sql.execute(null, artistSearchQuery, "A%")) {
            while (results.hasNext()) {
                results.next();
                resultCount++;
            }
        }
        
        long executionTime = System.nanoTime() - startTime;
        
        System.out.printf(">>> Artist name search returned %d results in %.2f ms%n", 
            resultCount, executionTime / 1_000_000.0);
        System.out.println(">>> Performance depends on index on Artist.Name column");
    }
    
    /**
     * Demonstrate composite index optimization for multi-column queries.
     */
    public void demonstrateCompositeIndexes() {
        System.out.println("\n--- Composite Index Optimization");
        
        Statement genreAlbumQuery = sql.statementBuilder()
            .query("SELECT t.Name, al.Title FROM Track t " +
                   "JOIN Album al ON t.AlbumId = al.AlbumId " +
                   "WHERE t.GenreId = ? AND t.UnitPrice > ? " +
                   "ORDER BY t.Name LIMIT 10")
            .build();
        
        long startTime = System.nanoTime();
        int resultCount = 0;
        
        try (ResultSet<SqlRow> results = sql.execute(null, genreAlbumQuery, 1, 0.99)) {
            while (results.hasNext()) {
                results.next();
                resultCount++;
            }
        }
        
        long executionTime = System.nanoTime() - startTime;
        
        System.out.printf(">>> Genre + price query returned %d results in %.2f ms%n", 
            resultCount, executionTime / 1_000_000.0);
        System.out.println(">>> Optimal index: (GenreId, UnitPrice, Name) for covering index benefit");
    }
    
    /**
     * Demonstrate prefix search optimization for partial string matching.
     */
    public void demonstratePrefixSearchOptimization() {
        System.out.println("\n--- Prefix Search Optimization");
        
        Statement trackPrefixQuery = sql.statementBuilder()
            .query("SELECT Name FROM Track WHERE Name LIKE ? ORDER BY Name LIMIT 5")
            .build();
        
        String[] searchPrefixes = {"Love%", "Rock%", "Blue%"};
        
        for (String prefix : searchPrefixes) {
            long startTime = System.nanoTime();
            int resultCount = 0;
            
            try (ResultSet<SqlRow> results = sql.execute(null, trackPrefixQuery, prefix)) {
                while (results.hasNext()) {
                    SqlRow row = results.next();
                    if (resultCount == 0) {
                        System.out.printf(">>> First result for '%s': %s%n", 
                            prefix, row.stringValue("Name"));
                    }
                    resultCount++;
                }
            }
            
            long executionTime = System.nanoTime() - startTime;
            System.out.printf(">>> Prefix search '%s' found %d tracks in %.2f ms%n", 
                prefix, resultCount, executionTime / 1_000_000.0);
        }
        
        System.out.println(">>> Prefix searches benefit from index on Track.Name column");
    }
}