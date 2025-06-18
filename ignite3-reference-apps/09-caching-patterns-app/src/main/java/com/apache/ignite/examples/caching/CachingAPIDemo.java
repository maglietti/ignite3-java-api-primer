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
        
        System.out.println("=== Apache Ignite 3 Caching Patterns Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Comprehensive caching pattern demonstrations");
        
        try {
            runCachingDemonstrations(clusterAddress);
            
            System.out.println("\n=== Caching pattern demonstrations completed successfully ===");
            
        } catch (Exception e) {
            logger.error("Demo execution failed", e);
            System.err.println("!!! Error: " + e.getMessage());
            System.err.println("!!! Ensure Ignite cluster is running and sample data is loaded");
        }
    }

    private static void runCachingDemonstrations(String clusterAddress) {
        System.out.println("\n=== [1/3] Cache-Aside Patterns");
        runCacheAsideDemo(clusterAddress);
        
        System.out.println("\n=== [2/3] Write-Through Patterns");
        runWriteThroughDemo(clusterAddress);
        
        System.out.println("\n=== [3/3] Write-Behind Patterns");
        runWriteBehindDemo(clusterAddress);
    }

    private static void runCacheAsideDemo(String clusterAddress) {
        System.out.println("--- Read-heavy catalog operations with lazy loading");
        try {
            CacheAsidePatterns.main(new String[]{clusterAddress});
            System.out.println(">>> Cache-aside patterns completed");
        } catch (Exception e) {
            System.err.println("!!! Cache-aside demo failed: " + e.getMessage());
        }
    }

    private static void runWriteThroughDemo(String clusterAddress) {
        System.out.println("--- Consistency-critical customer data operations");
        try {
            WriteThroughPatterns.main(new String[]{clusterAddress});
            System.out.println(">>> Write-through patterns completed");
        } catch (Exception e) {
            System.err.println("!!! Write-through demo failed: " + e.getMessage());
        }
    }

    private static void runWriteBehindDemo(String clusterAddress) {
        System.out.println("--- High-throughput analytics event recording");
        try {
            WriteBehindPatterns.main(new String[]{clusterAddress});
            System.out.println(">>> Write-behind patterns completed");
        } catch (Exception e) {
            System.err.println("!!! Write-behind demo failed: " + e.getMessage());
        }
    }

}