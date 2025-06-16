package com.apache.ignite.examples.caching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caching API Demo - Apache Ignite 3 caching patterns learning progression.
 * 
 * This demo orchestrates focused, educational examples that each demonstrate
 * one core caching pattern clearly. Each example is self-contained and 
 * can be run independently.
 * 
 * Learning Progression:
 * 1. CacheAsidePatterns - Read-heavy catalog operations with lazy loading
 * 2. WriteThroughPatterns - Consistency-critical customer data operations
 * 3. WriteBehindPatterns - High-throughput analytics event recording
 * 
 * Each class focuses on demonstrating specific caching capabilities
 * in a clear, educational manner with practical music streaming scenarios.
 * 
 * Key Learning Concepts:
 * - Pattern Selection: Choose appropriate caching pattern based on data characteristics
 * - Performance Trade-offs: Understand latency vs consistency vs throughput
 * - Implementation Patterns: Learn practical techniques for each caching approach
 * - Production Considerations: Error handling, monitoring, and optimization
 */
public class CachingAPIDemo {

    private static final Logger logger = LoggerFactory.getLogger(CachingAPIDemo.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("==========================================================");
        System.out.println("  Apache Ignite 3 Caching Patterns - Learning Demo");
        System.out.println("==========================================================");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println();
        
        try {
            // Run each demo in learning progression
            runDemo("Cache-Aside Patterns", 
                () -> CacheAsidePatterns.main(new String[]{clusterAddress}));
                
            runDemo("Write-Through Patterns", 
                () -> WriteThroughPatterns.main(new String[]{clusterAddress}));
                
            runDemo("Write-Behind Patterns", 
                () -> WriteBehindPatterns.main(new String[]{clusterAddress}));
            
            System.out.println("\n==========================================================");
            System.out.println("✓ All Caching Pattern examples completed successfully!");
            System.out.println("==========================================================");
            
            // Display pattern selection guidance
            displayPatternSelectionGuidance();
            
        } catch (Exception e) {
            logger.error("Demo execution failed", e);
            System.err.println("\nDemo failed: " + e.getMessage());
            System.err.println("Make sure Ignite cluster is running and sample data is loaded.");
        }
    }

    private static void runDemo(String name, Runnable demo) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Running: " + name);
        System.out.println("=".repeat(60));
        
        try {
            demo.run();
        } catch (Exception e) {
            logger.error("Failed to run demo: " + name, e);
            throw new RuntimeException("Demo failed: " + name, e);
        }
        
        System.out.println("\n✓ " + name + " completed");
    }

    /**
     * Displays guidance for selecting appropriate caching patterns.
     * 
     * Helps developers understand when to use each pattern based on
     * data characteristics and application requirements.
     */
    private static void displayPatternSelectionGuidance() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Caching Pattern Selection Guidelines");
        System.out.println("=".repeat(60));
        
        System.out.println("\n🔍 Cache-Aside Pattern:");
        System.out.println("   Use when: Read-heavy workloads, can tolerate cache misses");
        System.out.println("   Examples: Product catalogs, user profiles, configuration data");
        System.out.println("   Benefits: Simple implementation, application controls caching logic");
        System.out.println("   Trade-offs: Cache misses cause latency, potential data staleness");
        
        System.out.println("\n🔒 Write-Through Pattern:");
        System.out.println("   Use when: Consistency critical, moderate write volume");
        System.out.println("   Examples: Financial transactions, user account updates");
        System.out.println("   Benefits: Strong consistency, no data loss risk");
        System.out.println("   Trade-offs: Higher write latency, reduced throughput");
        
        System.out.println("\n⚡ Write-Behind Pattern:");
        System.out.println("   Use when: High-throughput writes, eventual consistency acceptable");
        System.out.println("   Examples: Analytics events, logging, social media interactions");
        System.out.println("   Benefits: Low write latency, high throughput");
        System.out.println("   Trade-offs: Eventual consistency, complexity in failure handling");
        
        System.out.println("\n📊 Decision Matrix:");
        System.out.println("   ┌─────────────────┬─────────────┬───────────────┬──────────────┐");
        System.out.println("   │ Pattern         │ Consistency │ Write Latency │ Throughput   │");
        System.out.println("   ├─────────────────┼─────────────┼───────────────┼──────────────┤");
        System.out.println("   │ Cache-Aside     │ Eventual    │ N/A           │ High (reads) │");
        System.out.println("   │ Write-Through   │ Strong      │ High          │ Medium       │");
        System.out.println("   │ Write-Behind    │ Eventual    │ Low           │ Very High    │");
        System.out.println("   └─────────────────┴─────────────┴───────────────┴──────────────┘");
        
        System.out.println("\n💡 Production Tips:");
        System.out.println("   • Combine patterns: Use different patterns for different data types");
        System.out.println("   • Monitor performance: Track cache hit rates and latencies");
        System.out.println("   • Handle failures: Implement circuit breakers and fallback strategies");
        System.out.println("   • Optimize batch sizes: Balance latency and throughput for your workload");
        System.out.println("   • Plan for scale: Consider data distribution and node affinity");
        
        System.out.println("\n📚 Next Steps:");
        System.out.println("   • Module 10: Catalog Management - Schema operations and DDL patterns");
        System.out.println("   • Production Deployment: Monitoring, alerting, and operational patterns");
        System.out.println("   • Performance Tuning: Advanced optimization techniques");
    }
}