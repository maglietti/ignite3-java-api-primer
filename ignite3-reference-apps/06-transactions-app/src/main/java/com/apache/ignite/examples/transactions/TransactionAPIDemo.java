package com.apache.ignite.examples.transactions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transaction API Demo - Apache Ignite 3 transaction learning progression.
 * 
 * This demo orchestrates focused, educational examples that each demonstrate
 * one core transaction concept clearly. Each example is self-contained and 
 * can be run independently.
 * 
 * Learning Progression:
 * 1. BasicTransactions - Transaction lifecycle and CRUD operations
 * 2. TransactionIsolation - Isolation levels and concurrent behavior
 * 3. AsyncTransactions - Asynchronous transaction patterns
 * 4. BatchTransactions - Bulk operations and performance patterns
 * 
 * Each class focuses on demonstrating specific transaction capabilities
 * in a clear, educational manner with practical examples.
 */
public class TransactionAPIDemo {

    private static final Logger logger = LoggerFactory.getLogger(TransactionAPIDemo.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("==================================================");
        System.out.println("  Apache Ignite 3 Transaction API - Learning Demo");
        System.out.println("==================================================");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println();
        
        try {
            // Run each demo in learning progression
            runDemo("Basic Transactions", 
                () -> BasicTransactions.main(new String[]{clusterAddress}));
                
            runDemo("Transaction Isolation", 
                () -> TransactionIsolation.main(new String[]{clusterAddress}));
                
            runDemo("Async Transactions", 
                () -> AsyncTransactions.main(new String[]{clusterAddress}));
                
            runDemo("Batch Transactions", 
                () -> BatchTransactions.main(new String[]{clusterAddress}));
            
            System.out.println("\n==================================================");
            System.out.println("✓ All Transaction API examples completed successfully!");
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