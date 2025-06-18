package com.apache.ignite.examples.compute;

import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive demonstration of Apache Ignite 3 Compute API patterns.
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
        System.out.println("Comprehensive distributed job execution demonstrations");

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
        System.out.println("\n=== [1/3] Basic Compute Operations");
        runBasicComputeDemo(clusterAddress);
        
        System.out.println("\n=== [2/3] Advanced Compute Operations");
        runAdvancedComputeDemo(clusterAddress);
        
        System.out.println("\n=== [3/3] Compute Job Workflows");
        runWorkflowDemo(clusterAddress);
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
}