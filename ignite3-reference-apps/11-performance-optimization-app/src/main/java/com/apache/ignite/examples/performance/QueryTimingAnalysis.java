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
 * Query timing analysis and performance measurement for Apache Ignite 3.
 * 
 * Demonstrates systematic performance measurement techniques that identify
 * query bottlenecks in distributed music streaming applications. Shows how
 * to measure execution characteristics and compare filtering strategies
 * to optimize query performance from seconds to milliseconds.
 * 
 * Key concepts demonstrated:
 * - Execution time measurement with nanosecond precision
 * - First result timing for streaming query optimization
 * - Filter strategy comparison for performance analysis
 * - Statement API usage for optimized query construction
 * 
 * Business context:
 * Music streaming platforms need sub-second query responses for real-time
 * recommendations and analytics. This class shows how to measure and optimize
 * queries that power genre popularity reports, artist search, and recommendation
 * engines across distributed datasets.
 */
public class QueryTimingAnalysis {
    
    private final IgniteSql sql;
    
    public QueryTimingAnalysis(IgniteClient client) {
        this.sql = client.sql();
    }
    
    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Query Timing Analysis Demo ===");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            QueryTimingAnalysis analyzer = new QueryTimingAnalysis(client);
            analyzer.demonstrateTimingMeasurement();
            analyzer.compareFilteringStrategies();
            
        } catch (Exception e) {
            System.err.println("Query timing analysis failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrate basic query timing measurement techniques.
     */
    public void demonstrateTimingMeasurement() {
        System.out.println("\n--- Basic Query Timing Measurement");
        
        Statement genreCountQuery = sql.statementBuilder()
            .query("SELECT COUNT(*) as track_count FROM Track WHERE GenreId = ?")
            .build();
            
        QueryMetrics metrics = analyzeStatement(genreCountQuery, 1);
        
        System.out.printf(">>> Query executed in %.2f ms%n", 
            metrics.totalTimeNanos / 1_000_000.0);
        System.out.printf(">>> First result returned in %.2f ms%n", 
            metrics.timeToFirstResultNanos / 1_000_000.0);
        System.out.printf(">>> Total results: %d%n", metrics.resultCount);
    }
    
    /**
     * Compare different filtering strategies to identify optimal approaches.
     */
    public void compareFilteringStrategies() {
        System.out.println("\n--- Filter Strategy Performance Comparison");
        
        int artistId = 1;
        
        Statement indexedQuery = sql.statementBuilder()
            .query("SELECT COUNT(*) FROM Track t JOIN Album al ON t.AlbumId = al.AlbumId WHERE al.ArtistId = ?")
            .build();
        
        Statement functionQuery = sql.statementBuilder()
            .query("SELECT COUNT(*) FROM Track t JOIN Album al ON t.AlbumId = al.AlbumId WHERE CAST(al.ArtistId AS VARCHAR) = CAST(? AS VARCHAR)")
            .build();
        
        QueryMetrics indexedMetrics = analyzeStatement(indexedQuery, artistId);
        QueryMetrics functionMetrics = analyzeStatement(functionQuery, artistId);
        
        System.out.printf(">>> Indexed filter: %.2f ms%n", 
            indexedMetrics.totalTimeNanos / 1_000_000.0);
        System.out.printf(">>> Function filter: %.2f ms%n", 
            functionMetrics.totalTimeNanos / 1_000_000.0);
        System.out.printf(">>> Performance ratio: %.1fx slower%n", 
            (double) functionMetrics.totalTimeNanos / indexedMetrics.totalTimeNanos);
    }
    
    /**
     * Measure query execution time and result characteristics using Statement API.
     */
    public QueryMetrics analyzeStatement(Statement statement, Object... params) {
        long startTime = System.nanoTime();
        int resultCount = 0;
        long firstResultTime = 0;
        
        try (ResultSet<SqlRow> results = sql.execute(null, statement, params)) {
            while (results.hasNext()) {
                SqlRow row = results.next();
                
                if (resultCount == 0) {
                    firstResultTime = System.nanoTime();
                }
                resultCount++;
            }
        }
        
        long totalTime = System.nanoTime() - startTime;
        long timeToFirstResult = firstResultTime > 0 ? firstResultTime - startTime : 0;
        
        return new QueryMetrics(resultCount, totalTime, timeToFirstResult);
    }
    
    /**
     * Query performance metrics for analysis and comparison.
     */
    public static class QueryMetrics {
        public final int resultCount;
        public final long totalTimeNanos;
        public final long timeToFirstResultNanos;
        
        public QueryMetrics(int resultCount, long totalTimeNanos, long timeToFirstResultNanos) {
            this.resultCount = resultCount;
            this.totalTimeNanos = totalTimeNanos;
            this.timeToFirstResultNanos = timeToFirstResultNanos;
        }
        
        public double getTotalTimeMillis() {
            return totalTimeNanos / 1_000_000.0;
        }
        
        public double getTimeToFirstResultMillis() {
            return timeToFirstResultNanos / 1_000_000.0;
        }
    }
}