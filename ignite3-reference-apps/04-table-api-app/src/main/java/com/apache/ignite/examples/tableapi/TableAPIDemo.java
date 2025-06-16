package com.apache.ignite.examples.tableapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Table API Demo - Apache Ignite 3 learning progression.
 * 
 * This demo orchestrates focused, educational examples that each demonstrate
 * one core concept clearly. Each example is self-contained and can be run 
 * independently.
 * 
 * Learning Progression:
 * 1. BasicTableOperations - CRUD operations with Tuples
 * 2. RecordViewExamples - Type-safe POJO operations  
 * 3. KeyValueExamples - Cache-like access patterns
 * 4. AsyncBasicOperations - Asynchronous operation patterns
 * 
 * Each class focuses on demonstrating specific Table API capabilities
 * in a clear, educational manner with practical examples.
 */
public class TableAPIDemo {

    private static final Logger logger = LoggerFactory.getLogger(TableAPIDemo.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("==================================================");
        System.out.println("    Apache Ignite 3 Table API - Learning Demo");
        System.out.println("==================================================");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println();
        
        try {
            // Run each demo in learning progression
            runDemo("Basic Table Operations", 
                () -> BasicTableOperations.main(new String[]{clusterAddress}));
                
            runDemo("RecordView Examples", 
                () -> RecordViewExamples.main(new String[]{clusterAddress}));
                
            runDemo("KeyValue Examples", 
                () -> KeyValueExamples.main(new String[]{clusterAddress}));
                
            runDemo("Async Basic Operations", 
                () -> AsyncBasicOperations.main(new String[]{clusterAddress}));
            
            System.out.println("\n==================================================");
            System.out.println("✓ All Table API examples completed successfully!");
            System.out.println("==================================================");
            
        } catch (Exception e) {
            logger.error("Demo execution failed", e);
            System.err.println("\nDemo failed: " + e.getMessage());
            System.err.println("Make sure Ignite cluster is running and sample data is loaded.");
        }
    }

    private static void runDemo(String name, Runnable demo) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("Running: " + name);
        System.out.println("=".repeat(50));
        
        try {
            demo.run();
        } catch (Exception e) {
            logger.error("Failed to run demo: " + name, e);
            throw new RuntimeException("Demo failed: " + name, e);
        }
        
        System.out.println("\n✓ " + name + " completed");
    }
}