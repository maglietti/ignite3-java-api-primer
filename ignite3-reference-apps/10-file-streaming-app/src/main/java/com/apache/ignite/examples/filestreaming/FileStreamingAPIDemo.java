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

package com.apache.ignite.examples.filestreaming;

import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File Streaming API Demo - Orchestrates file-based backpressure streaming demonstrations.
 * 
 * This example demonstrates end-to-end backpressure propagation from file I/O through
 * to cluster ingestion using Ignite 3's reactive Data Streaming API. The demonstrations
 * show how properly implemented Flow.Publisher implementations can control upstream
 * data production based on downstream consumption capacity.
 * 
 * Key educational concepts:
 * - File-to-cluster streaming with reactive backpressure
 * - Memory-efficient line-by-line file processing
 * - Demand-driven data reading that prevents system overload
 * - Performance monitoring and rate adaptation
 * - Real-world file processing scenarios with realistic data
 * 
 * Business context:
 * Music streaming platforms process massive log files containing user interaction
 * events. These files often exceed cluster processing capacity when read at full
 * speed. This demo shows how reactive streams naturally balance file I/O rates
 * with distributed processing capacity to maintain system stability.
 * 
 * The demonstrations progress from normal processing through backpressure scenarios
 * to high-velocity streaming, showing how the same Flow API patterns handle
 * different load conditions automatically.
 * 
 * Prerequisites:
 * - Running Ignite 3 cluster (see 00-docker for setup)
 * - Sample data setup completed (see 01-sample-data-setup)
 * - Sufficient disk space for temporary CSV files (~50MB)
 * 
 * Usage:
 * mvn compile exec:java -Dexec.args="127.0.0.1:10800"
 */
public class FileStreamingAPIDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(FileStreamingAPIDemo.class);
    
    /**
     * Main entry point for file streaming demonstrations.
     * 
     * @param args optional cluster address (defaults to 127.0.0.1:10800)
     */
    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== [1/1] File Streaming API Demonstrations ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println();
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            // Verify cluster connectivity
            System.out.println(">>> Verifying cluster connectivity...");
            verifyClusterConnection(client);
            System.out.println("<<< Connected to Ignite 3 cluster");
            
            // Execute file streaming demonstrations
            runFileStreamingDemonstrations(client);
            
            System.out.println("\n=== File Streaming API Demonstrations Completed ===");
            System.out.println("Summary:");
            System.out.println("- Demonstrated file-to-cluster streaming with backpressure");
            System.out.println("- Showed memory-efficient line-by-line file processing");
            System.out.println("- Illustrated reactive flow control preventing system overload");
            System.out.println("- Provided performance metrics for different scenarios");
            
        } catch (Exception e) {
            logger.error("File streaming demonstrations failed", e);
            System.err.println("Error: " + e.getMessage());
            System.err.println("\nTroubleshooting:");
            System.err.println("1. Ensure Ignite 3 cluster is running (see 00-docker/)");
            System.err.println("2. Verify cluster address: " + clusterAddress);
            System.err.println("3. Check that sample data setup completed successfully");
            System.err.println("4. Ensure sufficient disk space for temporary files");
            System.exit(1);
        }
    }
    
    /**
     * Verifies basic cluster connectivity and readiness.
     */
    private static void verifyClusterConnection(IgniteClient client) {
        try {
            // Simple connectivity test
            var tables = client.tables();
            if (tables != null) {
                System.out.println("  Cluster connection verified");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to cluster at address. " +
                "Please ensure Ignite 3 cluster is running.", e);
        }
    }
    
    /**
     * Runs the complete suite of file streaming demonstrations.
     */
    private static void runFileStreamingDemonstrations(IgniteClient client) {
        System.out.println("=== File-Based Backpressure Streaming Demonstrations ===");
        System.out.println();
        
        try {
            // Run file backpressure streaming demonstrations
            System.out.println("--- Starting file streaming demonstrations...");
            System.out.println("    These demos show end-to-end backpressure from file I/O to cluster ingestion");
            System.out.println("    Watch how file reading rates automatically adapt to cluster processing capacity");
            System.out.println();
            
            runFileBackpressureDemo(client);
            
        } catch (Exception e) {
            logger.error("File streaming demonstrations failed", e);
            throw new RuntimeException("File streaming demonstrations failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Executes the file backpressure streaming demonstrations.
     */
    private static void runFileBackpressureDemo(IgniteClient client) {
        try {
            // Use the main method from FileBackpressureStreaming but with our client
            System.out.println("=== File-Based Backpressure Streaming Demo ===");
            System.out.println("This demonstration shows three scenarios:");
            System.out.println("1. Normal processing - cluster keeps up with file reading");
            System.out.println("2. Slow cluster - backpressure reduces file reading rate");
            System.out.println("3. High velocity - maximum throughput with efficient flow control");
            System.out.println();
            
            // Create instance to call demo methods
            FileBackpressureStreaming.main(new String[]{"127.0.0.1:10800"});
            
        } catch (Exception e) {
            logger.error("File backpressure demo failed", e);
            throw new RuntimeException("File backpressure demo failed", e);
        }
    }
}