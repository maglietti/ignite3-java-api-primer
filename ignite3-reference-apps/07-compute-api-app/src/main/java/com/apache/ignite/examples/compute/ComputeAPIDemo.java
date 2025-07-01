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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demonstration of Apache Ignite 3 Compute API patterns.
 * 
 * This orchestrator runs all compute examples in educational progression,
 * demonstrating distributed job execution capabilities for music store analytics.
 * 
 * Learning progression:
 * 1. BasicComputeOperations - Job submission fundamentals
 * 2. AdvancedComputeOperations - Data locality and complex patterns  
 * 3. ComputeJobWorkflows - Multi-step business process automation
 * 
 * Prerequisites:
 * - Running Ignite cluster
 * - Sample music store data loaded
 */
public class ComputeAPIDemo {

    private static final Logger logger = LoggerFactory.getLogger(ComputeAPIDemo.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Apache Ignite 3 Compute API Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Distributed job execution demonstrations");

        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            System.out.println("\n--- Connecting to Ignite cluster");
            System.out.println(">>> Successfully connected to: " + clusterAddress);
            
            runComputeDemonstrations(client, clusterAddress);
            
            System.out.println("\n=== Compute API demonstrations completed successfully ===");
            
        } catch (Exception e) {
            logger.error("Failed to run compute demonstrations", e);
            System.err.println("!!! Error: " + e.getMessage());
            System.err.println("!!! Ensure Ignite cluster is running and sample data is loaded");
        }
    }

    private static void runComputeDemonstrations(IgniteClient client, String clusterAddress) {
        System.out.println("\n=== [1/5] Basic Compute Operations");
        runBasicComputeDemo(clusterAddress);
        
        System.out.println("\n=== [2/5] Advanced Compute Operations");
        runAdvancedComputeDemo(clusterAddress);
        
        System.out.println("\n=== [3/5] Compute Job Workflows");
        runWorkflowDemo(clusterAddress);
        
        System.out.println("\n=== [4/5] Production Compute Patterns");
        runProductionPatternsDemo(clusterAddress);
        
        System.out.println("\n=== [5/5] Music Platform Intelligence");
        runMusicPlatformIntelligenceDemo(clusterAddress);
    }

    private static void runBasicComputeDemo(String clusterAddress) {
        System.out.println("--- Job submission fundamentals");
        try {
            BasicComputeOperations.main(new String[]{clusterAddress});
            System.out.println(">>> Basic compute operations completed");
        } catch (Exception e) {
            System.err.println("!!! Basic compute demo failed: " + e.getMessage());
        }
    }

    private static void runAdvancedComputeDemo(String clusterAddress) {
        System.out.println("--- Data locality and complex patterns");
        try {
            AdvancedComputeOperations.main(new String[]{clusterAddress});
            System.out.println(">>> Advanced compute operations completed");
        } catch (Exception e) {
            System.err.println("!!! Advanced compute demo failed: " + e.getMessage());
        }
    }

    private static void runWorkflowDemo(String clusterAddress) {
        System.out.println("--- Multi-step business process automation");
        try {
            ComputeJobWorkflows.main(new String[]{clusterAddress});
            System.out.println(">>> Job workflow demonstrations completed");
        } catch (Exception e) {
            System.err.println("!!! Workflow demo failed: " + e.getMessage());
        }
    }

    private static void runProductionPatternsDemo(String clusterAddress) {
        System.out.println("--- Production-scale distributed computing");
        try {
            ProductionComputePatterns.main(new String[]{clusterAddress});
            System.out.println(">>> Production patterns demonstrations completed");
        } catch (Exception e) {
            System.err.println("!!! Production patterns demo failed: " + e.getMessage());
        }
    }

    private static void runMusicPlatformIntelligenceDemo(String clusterAddress) {
        System.out.println("--- Documentation-aligned compute patterns");
        try {
            MusicPlatformIntelligence.main(new String[]{clusterAddress});
            System.out.println(">>> Music platform intelligence demonstrations completed");
        } catch (Exception e) {
            System.err.println("!!! Music platform intelligence demo failed: " + e.getMessage());
        }
    }
}