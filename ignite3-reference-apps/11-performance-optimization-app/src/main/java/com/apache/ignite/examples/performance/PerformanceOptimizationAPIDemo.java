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

/**
 * Performance optimization and query tuning demonstrations for Apache Ignite 3.
 * 
 * This orchestrator runs all performance optimization examples to provide
 * a complete overview of query tuning, index optimization, and scalability
 * techniques. Each demonstration focuses on specific performance challenges
 * that music streaming platforms face when serving millions of users.
 * 
 * Key demonstrations included:
 * - Query performance analysis and timing measurement
 * - Execution plan analysis using EXPLAIN PLAN FOR
 * - Index optimization strategies for distributed data
 * - Join optimization and data colocation approaches
 * - Caching optimization for read-heavy workloads
 * 
 * Business context:
 * A music streaming platform must deliver sub-second query responses
 * while handling 10 million users and petabytes of music data. This demo
 * shows how to optimize queries from 30-second timeouts to instant responses
 * through systematic performance tuning techniques.
 * 
 * Usage:
 * Run this main class to see all performance optimization techniques
 * demonstrated in sequence. Each demonstration shows before/after comparisons
 * with specific performance improvements.
 */
public class PerformanceOptimizationAPIDemo {
    
    /**
     * Main method that orchestrates all performance optimization demonstrations.
     * Runs each demo in sequence with clear separation and progress reporting.
     */
    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Apache Ignite 3 Performance Optimization Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Query tuning and scalability optimization demonstrations");
        
        try (IgniteClient ignite = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            System.out.println("\n--- Connecting to Ignite cluster");
            System.out.println(">>> Connected to: " + clusterAddress);
            System.out.println(">>> Cluster nodes: " + ignite.clusterNodes().size());
            
            runPerformanceDemonstrations(clusterAddress);
            
            System.out.println("\n=== Performance optimization demonstrations completed ===");
            
        } catch (Exception e) {
            System.err.println("!!! Error: " + e.getMessage());
            System.err.println("!!! Ensure Ignite cluster is running and sample data is loaded");
        }
    }
    
    private static void runPerformanceDemonstrations(String clusterAddress) {
        System.out.println("\n=== [1/5] Query Performance Analysis");
        runQueryAnalysisDemo(clusterAddress);
        
        System.out.println("\n=== [2/5] Execution Plan Analysis");
        runExecutionPlanDemo(clusterAddress);
        
        System.out.println("\n=== [3/5] Index Optimization");
        runIndexOptimizationDemo(clusterAddress);
        
        System.out.println("\n=== [4/5] Join Optimization");
        runJoinOptimizationDemo(clusterAddress);
        
        System.out.println("\n=== [5/5] Cache Optimization");
        runCacheOptimizationDemo(clusterAddress);
    }
    
    private static void runQueryAnalysisDemo(String clusterAddress) {
        System.out.println("--- Query timing analysis and performance measurement");
        try {
            QueryTimingAnalysis.main(new String[]{clusterAddress});
            System.out.println(">>> Query analysis patterns demonstrated");
        } catch (Exception e) {
            System.err.println("!!! Query analysis demo failed: " + e.getMessage());
        }
    }

    private static void runExecutionPlanDemo(String clusterAddress) {
        System.out.println("--- EXPLAIN PLAN FOR analysis and query optimization");
        try {
            QueryExecutionPlanAnalysis.main(new String[]{clusterAddress});
            System.out.println(">>> Execution plan analysis patterns demonstrated");
        } catch (Exception e) {
            System.err.println("!!! Execution plan demo failed: " + e.getMessage());
        }
    }

    private static void runIndexOptimizationDemo(String clusterAddress) {
        System.out.println("--- Index strategies for distributed query performance");
        try {
            IndexOptimizationStrategies.main(new String[]{clusterAddress});
            System.out.println(">>> Index optimization patterns demonstrated");
        } catch (Exception e) {
            System.err.println("!!! Index optimization demo failed: " + e.getMessage());
        }
    }

    private static void runJoinOptimizationDemo(String clusterAddress) {
        System.out.println("--- Join strategies and data colocation optimization");
        try {
            OptimizedJoinStrategies.main(new String[]{clusterAddress});
            System.out.println(">>> Join optimization patterns demonstrated");
        } catch (Exception e) {
            System.err.println("!!! Join optimization demo failed: " + e.getMessage());
        }
    }

    private static void runCacheOptimizationDemo(String clusterAddress) {
        System.out.println("--- Cache-aside optimization for read-heavy workloads");
        try {
            CacheAsideOptimization.main(new String[]{clusterAddress});
            System.out.println(">>> Cache optimization patterns demonstrated");
        } catch (Exception e) {
            System.err.println("!!! Cache optimization demo failed: " + e.getMessage());
        }
    }
}