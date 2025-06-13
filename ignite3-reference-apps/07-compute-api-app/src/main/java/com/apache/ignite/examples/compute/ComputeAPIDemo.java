package com.apache.ignite.examples.compute;

import org.apache.ignite.client.IgniteClient;

/**
 * Main demonstration application for Ignite 3 Compute API patterns using music store data.
 * 
 * This comprehensive demo showcases all major compute patterns and capabilities:
 * - Basic job submission and execution
 * - Data colocation for performance optimization
 * - Asynchronous job execution and monitoring
 * - Complex workflow orchestration
 * 
 * The demo provides a complete tour of Ignite 3's distributed computing capabilities
 * using realistic music store analytics scenarios.
 * 
 * Educational Structure:
 * 1. BasicComputeDemo - Simple job patterns and fundamentals
 * 2. ColocationComputeDemo - Data-local execution for performance
 * 3. AsyncComputePatterns - Non-blocking and parallel processing
 * 4. MusicStoreJobs - Specialized jobs for real-world analytics
 * 
 * Prerequisites:
 * - Running Ignite 3 cluster (see docker-compose.yml)
 * - Sample music store data loaded (run sample-data-setup first)
 * 
 * @since 1.0.0
 */
public class ComputeAPIDemo {
    
    private static final String IGNITE_URL = "http://localhost:10800";
    
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║             Ignite 3 Compute API - Complete Demo             ║");
        System.out.println("║                Music Store Analytics Platform                 ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        try (IgniteClient ignite = IgniteClient.builder()
                .addresses(IGNITE_URL)
                .build()) {
            
            System.out.println("✅ Connected to Ignite cluster");
            System.out.println("🎵 Running comprehensive compute demonstrations...");
            System.out.println();
            
            // Run all demonstrations
            runBasicComputeDemo(ignite);
            runColocationDemo(ignite);
            runAsyncPatternsDemo(ignite);
            runMusicStoreJobsDemo(ignite);
            
            System.out.println("\n🎉 All compute demonstrations completed successfully!");
            System.out.println("📚 Check individual demo classes for detailed implementation examples.");
            
        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            System.err.println("\n🔧 Troubleshooting:");
            System.err.println("   • Ensure Ignite cluster is running (see docker-compose.yml)");
            System.err.println("   • Verify sample data is loaded (run sample-data-setup)");
            System.err.println("   • Check network connectivity to " + IGNITE_URL);
            e.printStackTrace();
        }
    }
    
    /**
     * Demonstrates basic compute job patterns.
     */
    private static void runBasicComputeDemo(IgniteClient ignite) {
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. BASIC COMPUTE PATTERNS                                   │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        
        try {
            // Run basic compute demo
            BasicComputeDemo.main(new String[]{});
            System.out.println("✅ Basic compute patterns completed");
            
        } catch (Exception e) {
            System.err.println("❌ Basic compute demo failed: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates data colocation and broadcast patterns.
     */
    private static void runColocationDemo(IgniteClient ignite) {
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  2. DATA COLOCATION PATTERNS                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        
        try {
            // Run colocation demo
            ColocationComputeDemo.main(new String[]{});
            System.out.println("✅ Data colocation patterns completed");
            
        } catch (Exception e) {
            System.err.println("❌ Colocation demo failed: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates asynchronous execution patterns.
     */
    private static void runAsyncPatternsDemo(IgniteClient ignite) {
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  3. ASYNCHRONOUS EXECUTION PATTERNS                          │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        
        try {
            // Run async patterns demo
            AsyncComputePatterns.main(new String[]{});
            System.out.println("✅ Async execution patterns completed");
            
        } catch (Exception e) {
            System.err.println("❌ Async patterns demo failed: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates specialized music store analytics jobs.
     */
    private static void runMusicStoreJobsDemo(IgniteClient ignite) {
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  4. MUSIC STORE ANALYTICS JOBS                               │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        
        try {
            // Run specialized jobs demo
            MusicStoreJobs.main(new String[]{});
            System.out.println("✅ Music store analytics completed");
            
        } catch (Exception e) {
            System.err.println("❌ Music store jobs demo failed: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Displays help information for running the demos.
     */
    public static void showHelp() {
        System.out.println("Ignite 3 Compute API Demo - Help");
        System.out.println("================================");
        System.out.println();
        System.out.println("This demo showcases Ignite 3's distributed computing capabilities:");
        System.out.println();
        System.out.println("Demo Components:");
        System.out.println("  • BasicComputeDemo      - Job submission fundamentals");
        System.out.println("  • ColocationComputeDemo - Data-local execution patterns");
        System.out.println("  • AsyncComputePatterns  - Non-blocking job execution");
        System.out.println("  • MusicStoreJobs        - Real-world analytics scenarios");
        System.out.println();
        System.out.println("Prerequisites:");
        System.out.println("  1. Start Ignite cluster:");
        System.out.println("     cd ../00-docker && docker-compose up -d");
        System.out.println();
        System.out.println("  2. Load sample data:");
        System.out.println("     cd ../01-sample-data-setup && mvn exec:java");
        System.out.println();
        System.out.println("  3. Run this demo:");
        System.out.println("     mvn exec:java -Dexec.mainClass=\"com.apache.ignite.examples.compute.ComputeAPIDemo\"");
        System.out.println();
        System.out.println("Individual Demos:");
        System.out.println("  You can also run individual demo classes:");
        System.out.println("  • mvn exec:java -Dexec.mainClass=\"...BasicComputeDemo\"");
        System.out.println("  • mvn exec:java -Dexec.mainClass=\"...ColocationComputeDemo\"");
        System.out.println("  • mvn exec:java -Dexec.mainClass=\"...AsyncComputePatterns\"");
        System.out.println("  • mvn exec:java -Dexec.mainClass=\"...MusicStoreJobs\"");
        System.out.println();
        System.out.println("Learning Path:");
        System.out.println("  1. Start with BasicComputeDemo for fundamentals");
        System.out.println("  2. Learn data locality with ColocationComputeDemo");
        System.out.println("  3. Explore async patterns with AsyncComputePatterns");
        System.out.println("  4. See real applications with MusicStoreJobs");
        System.out.println();
        System.out.println("Documentation:");
        System.out.println("  📖 See ../../docs/07-compute-api-distributed-processing.md");
        System.out.println("  📝 Review JavaDoc comments in each demo class");
        System.out.println("  🔍 Examine job implementations for patterns");
    }
}