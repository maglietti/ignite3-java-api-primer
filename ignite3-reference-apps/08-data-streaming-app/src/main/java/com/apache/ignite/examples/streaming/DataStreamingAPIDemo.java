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

package com.apache.ignite.examples.streaming;

import org.apache.ignite.client.IgniteClient;

/**
 * Comprehensive demonstration of Ignite 3's Data Streaming API capabilities.
 * 
 * This example orchestrates all data streaming demonstrations to provide
 * a complete overview of high-throughput data ingestion patterns. It runs
 * all the reference applications in sequence to showcase the full range
 * of streaming capabilities from basic operations to advanced backpressure handling.
 * 
 * Key demonstrations included:
 * - Basic data streaming patterns and configuration
 * - High-volume bulk data ingestion optimization  
 * - Backpressure handling and flow control
 * - Performance monitoring and adaptive streaming
 * 
 * Business context:
 * A music streaming platform needs robust data ingestion capabilities
 * to handle everything from real-time user events to bulk historical data
 * migration. This demo shows how to implement streaming solutions that
 * scale from thousands to millions of events while maintaining data integrity
 * and system stability.
 * 
 * Usage:
 * Run this main class to see all streaming patterns demonstrated in sequence.
 * Each demonstration is self-contained and shows different aspects of the
 * Data Streaming API in action.
 */
public class DataStreamingAPIDemo {
    
    /**
     * Main method that orchestrates all data streaming demonstrations.
     * Runs each demo in sequence with clear separation and progress reporting.
     */
    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Apache Ignite 3 Data Streaming API Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Comprehensive high-throughput data ingestion demonstrations");
        
        try (IgniteClient ignite = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            System.out.println("\n--- Connecting to Ignite cluster");
            System.out.println(">>> Connected to: " + clusterAddress);
            System.out.println(">>> Cluster nodes: " + ignite.clusterNodes().size());
            
            runStreamingDemonstrations(clusterAddress);
            
            System.out.println("\n=== Data streaming demonstrations completed ===");
            
            // Background thread completion before process termination
            Thread.sleep(5000);
            
        } catch (Exception e) {
            System.err.println("!!! Error: " + e.getMessage());
            System.err.println("!!! Ensure Ignite cluster is running and sample data is loaded");
        }
    }
    
    private static void runStreamingDemonstrations(String clusterAddress) {
        System.out.println("\n=== [1/3] Basic Data Streaming");
        runBasicStreamingDemo(clusterAddress);
        
        System.out.println("\n=== [2/3] Bulk Data Ingestion");
        runBulkIngestionDemo(clusterAddress);
        
        System.out.println("\n=== [3/3] Backpressure Handling");
        runBackpressureDemo(clusterAddress);
    }
    
    private static void runBasicStreamingDemo(String clusterAddress) {
        System.out.println("--- Fundamental streaming patterns and configuration");
        try {
            BasicDataStreamerDemo.main(new String[]{clusterAddress});
            System.out.println(">>> Basic streaming patterns demonstrated");
        } catch (Exception e) {
            System.err.println("!!! Basic streaming demo failed: " + e.getMessage());
        }
    }

    private static void runBulkIngestionDemo(String clusterAddress) {
        System.out.println("--- High-volume data loading and performance optimization");
        try {
            BulkDataIngestion.main(new String[]{clusterAddress});
            System.out.println(">>> Bulk ingestion patterns demonstrated");
        } catch (Exception e) {
            System.err.println("!!! Bulk ingestion demo failed: " + e.getMessage());
        }
    }

    private static void runBackpressureDemo(String clusterAddress) {
        System.out.println("--- Flow control and adaptive rate limiting");
        try {
            BackpressureHandling.main(new String[]{clusterAddress});
            System.out.println(">>> Backpressure patterns demonstrated");
        } catch (Exception e) {
            System.err.println("!!! Backpressure demo failed: " + e.getMessage());
        }
    }
}