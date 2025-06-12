package com.apache.ignite.examples.tableapi;

import org.apache.ignite.client.IgniteClient;

/**
 * Main demonstration application for Apache Ignite 3 Table API.
 * 
 * This application serves as the entry point for all Table API demonstrations,
 * providing a comprehensive showcase of object-oriented data access patterns.
 * 
 * Available Demonstrations:
 * 1. RecordViewOperations - Complete object operations with POJOs
 * 2. KeyValueOperations - Cache-like key-value operations
 * 3. AsyncTableOperations - Advanced async programming patterns
 * 
 * Each demonstration can be run independently or as part of this comprehensive demo.
 * 
 * Prerequisites:
 * 1. Ignite cluster running (use 00-docker/init-cluster.sh)
 * 2. Music store schema and data loaded (use 01-sample-data-setup)
 * 
 * Usage:
 * - Run without arguments: Complete Table API demonstration
 * - Run with specific demo name: Individual demonstration
 *   java TableAPIDemo recordview
 *   java TableAPIDemo keyvalue
 *   java TableAPIDemo async
 * 
 * Learning Objectives:
 * - Understand the complete Table API ecosystem
 * - Learn when to use each API approach
 * - Master object-oriented data access patterns
 * - Practice production-ready async programming
 */
public class TableAPIDemo {
    
    private static final String CLUSTER_ENDPOINT = "127.0.0.1:10800";
    
    public static void main(String[] args) {
        System.out.println("=== Apache Ignite 3 Table API Comprehensive Demo ===");
        System.out.println("Object-oriented data access with the music store dataset");
        System.out.println();
        
        if (args.length > 0) {
            runSpecificDemo(args[0]);
        } else {
            runCompleteDemo();
        }
    }
    
    /**
     * Runs a specific demonstration based on command line argument.
     */
    private static void runSpecificDemo(String demoName) {
        switch (demoName.toLowerCase()) {
            case "recordview":
            case "record":
                System.out.println("🎯 Running RecordView Operations Demo");
                RecordViewOperations.main(new String[0]);
                break;
                
            case "keyvalue":
            case "kv":
                System.out.println("🎯 Running KeyValueView Operations Demo");
                KeyValueOperations.main(new String[0]);
                break;
                
            case "async":
            case "asynchronous":
                System.out.println("🎯 Running Advanced Async Operations Demo");
                AsyncTableOperations.main(new String[0]);
                break;
                
            default:
                System.err.println("Unknown demo: " + demoName);
                System.err.println("Available demos: recordview, keyvalue, async");
                System.exit(1);
        }
    }
    
    /**
     * Runs the complete Table API demonstration with all patterns.
     */
    private static void runCompleteDemo() {
        System.out.println("🚀 Running Complete Table API Demonstration");
        System.out.println();
        
        // Verify cluster connectivity first
        if (!verifyClusterConnectivity()) {
            System.err.println("❌ Cannot connect to Ignite cluster");
            System.err.println("Please ensure cluster is running (use 00-docker/init-cluster.sh)");
            System.exit(1);
        }
        
        try {
            // Run all demonstrations in sequence
            runDemoWithHeader("RecordView Operations", 
                "Complete object-oriented CRUD operations",
                () -> RecordViewOperations.main(new String[0]));
            
            waitForUserInput();
            
            runDemoWithHeader("KeyValueView Operations", 
                "Cache-like key-value operations with type safety",
                () -> KeyValueOperations.main(new String[0]));
            
            waitForUserInput();
            
            runDemoWithHeader("Advanced Async Operations", 
                "Production-ready async programming patterns",
                () -> AsyncTableOperations.main(new String[0]));
            
            System.out.println();
            System.out.println("🎉 Complete Table API Demonstration Finished!");
            printSummaryAndNextSteps();
            
        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Verifies that we can connect to the Ignite cluster.
     */
    private static boolean verifyClusterConnectivity() {
        System.out.println("🔍 Verifying cluster connectivity...");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(CLUSTER_ENDPOINT)
                .build()) {
            
            // Try to list tables to verify connectivity
            client.tables().tables();
            System.out.println("✅ Successfully connected to cluster at " + CLUSTER_ENDPOINT);
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Failed to connect: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Runs a demonstration with a formatted header.
     */
    private static void runDemoWithHeader(String title, String description, Runnable demo) {
        System.out.println("=" .repeat(80));
        System.out.println("🎯 " + title);
        System.out.println("📝 " + description);
        System.out.println("=" .repeat(80));
        System.out.println();
        
        try {
            demo.run();
        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println();
        System.out.println("✅ " + title + " Complete");
        System.out.println();
    }
    
    /**
     * Waits for user input to continue (in interactive mode).
     */
    private static void waitForUserInput() {
        System.out.println("Press Enter to continue to the next demonstration...");
        try {
            System.in.read();
        } catch (Exception e) {
            // Continue automatically if input fails
        }
    }
    
    /**
     * Prints a summary of what was demonstrated and next steps for learning.
     */
    private static void printSummaryAndNextSteps() {
        System.out.println("📚 What You've Learned:");
        System.out.println();
        
        System.out.println("🔹 RecordView Operations:");
        System.out.println("  • Object-oriented CRUD with POJOs");
        System.out.println("  • Composite primary keys and colocation");
        System.out.println("  • Bulk operations for performance");
        System.out.println("  • Transaction integration patterns");
        System.out.println();
        
        System.out.println("🔹 KeyValueView Operations:");
        System.out.println("  • Cache-like key-value access patterns");
        System.out.println("  • Null value handling strategies");
        System.out.println("  • Conditional operations and atomic updates");
        System.out.println("  • Tuple-based operations for flexibility");
        System.out.println();
        
        System.out.println("🔹 Advanced Async Operations:");
        System.out.println("  • Complex async operation chaining");
        System.out.println("  • Error handling and recovery patterns");
        System.out.println("  • Circuit breaker and retry logic");
        System.out.println("  • Performance optimization techniques");
        System.out.println();
        
        System.out.println("🚀 Next Steps:");
        System.out.println();
        System.out.println("1. 📖 Study Module 5: SQL API");
        System.out.println("   Learn when SQL excels over Table API for complex queries");
        System.out.println("   File: docs/05-sql-api-relational-data-access.md");
        System.out.println();
        
        System.out.println("2. 🔄 Practice Module 6: Transactions");
        System.out.println("   Master distributed ACID guarantees");
        System.out.println("   File: docs/06-transactions.md");
        System.out.println();
        
        System.out.println("3. 💻 Experiment with the Code:");
        System.out.println("   • Modify the reference applications");
        System.out.println("   • Try different batch sizes and async patterns");
        System.out.println("   • Implement your own business logic");
        System.out.println();
        
        System.out.println("4. 🏗️ Build Your Own Application:");
        System.out.println("   • Use the music store model as a template");
        System.out.println("   • Apply Table API patterns to your domain");
        System.out.println("   • Combine with SQL API for complex queries");
        System.out.println();
        
        System.out.println("📍 Key Decision Points:");
        System.out.println();
        
        System.out.println("✅ Use Table API When:");
        System.out.println("  • You know the primary keys");
        System.out.println("  • Working with single records or small batches");
        System.out.println("  • Type safety is critical");
        System.out.println("  • Object-oriented patterns fit your domain");
        System.out.println();
        
        System.out.println("⚠️ Consider SQL API When:");
        System.out.println("  • Complex JOINs across multiple tables");
        System.out.println("  • Aggregate functions and GROUP BY operations");
        System.out.println("  • Range queries with WHERE clauses");
        System.out.println("  • Dynamic queries determined at runtime");
        System.out.println();
        
        System.out.println("🔗 Reference Materials:");
        System.out.println("  • Documentation: docs/04-table-api-object-oriented-data-access.md");
        System.out.println("  • Source Code: ignite3-reference-apps/04-table-api-app/");
        System.out.println("  • Music Store Model: 01-sample-data-setup/src/main/java/.../model/");
        System.out.println();
        
        System.out.println("Happy coding with Apache Ignite 3! 🎸🎵");
    }
}