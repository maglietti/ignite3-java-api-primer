package com.apache.ignite.examples.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Catalog API Demo - Apache Ignite 3 catalog management learning progression.
 * 
 * This demo orchestrates focused, educational examples that each demonstrate
 * one core catalog management concept clearly. Each example is self-contained 
 * and can be run independently.
 * 
 * Learning Progression:
 * 1. DistributionZones - Zone configuration and workload isolation
 * 2. SchemaOperations - Table creation, indexes, and DDL operations
 * 3. CatalogIntrospection - Schema discovery and analysis
 * 
 * Each class focuses on demonstrating specific catalog management capabilities
 * in a clear, educational manner with practical music streaming scenarios.
 * 
 * Key Learning Concepts:
 * - Zone Management: Configure zones for different workload characteristics
 * - Schema Operations: Create and manage tables with proper colocation
 * - Catalog Introspection: Discover and analyze existing schema structures
 * - Performance Optimization: Use zones and indexes for query optimization
 */
public class CatalogAPIDemo {

    private static final Logger logger = LoggerFactory.getLogger(CatalogAPIDemo.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("==========================================================");
        System.out.println("  Apache Ignite 3 Catalog Management - Learning Demo");
        System.out.println("==========================================================");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println();
        
        try {
            // Run each demo in learning progression
            runDemo("Distribution Zones", 
                () -> DistributionZones.main(new String[]{clusterAddress}));
                
            runDemo("Schema Operations", 
                () -> SchemaOperations.main(new String[]{clusterAddress}));
                
            runDemo("Catalog Introspection", 
                () -> CatalogIntrospection.main(new String[]{clusterAddress}));
            
            System.out.println("\n==========================================================");
            System.out.println("✓ All Catalog Management examples completed successfully!");
            System.out.println("==========================================================");
            
            // Display catalog management guidance
            displayCatalogManagementGuidance();
            
        } catch (Exception e) {
            logger.error("Demo execution failed", e);
            System.err.println("\nDemo failed: " + e.getMessage());
            System.err.println("Make sure Ignite cluster is running and accessible.");
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
     * Displays guidance for catalog management best practices.
     * 
     * Helps developers understand how to design and manage
     * distributed database schemas effectively.
     */
    private static void displayCatalogManagementGuidance() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Catalog Management Best Practices");
        System.out.println("=".repeat(60));
        
        System.out.println("\n🏗️ Distribution Zone Strategy:");
        System.out.println("   OLTP Zones: High replica count (3+) for consistency and availability");
        System.out.println("   OLAP Zones: More partitions (64+) for analytical query parallelism");
        System.out.println("   Cache Zones: High replica count (5+) for global read performance");
        System.out.println("   Streaming Zones: Many partitions (128+), fewer replicas for throughput");
        System.out.println("   Development Zones: Minimal resources for testing and development");
        
        System.out.println("\n📊 Schema Design Guidelines:");
        System.out.println("   Colocation: Group related data using colocation keys for performance");
        System.out.println("   Primary Keys: Choose keys that enable effective data distribution");
        System.out.println("   Indexes: Create indexes on frequently queried columns");
        System.out.println("   Composite Keys: Use for hierarchical data relationships");
        System.out.println("   Zone Assignment: Assign tables to appropriate zones based on access patterns");
        
        System.out.println("\n🔍 Schema Introspection Usage:");
        System.out.println("   Discovery: Use system catalogs to understand existing schema");
        System.out.println("   Analysis: Monitor index usage and query performance");
        System.out.println("   Documentation: Generate schema reports for team collaboration");
        System.out.println("   Validation: Verify schema integrity and optimization opportunities");
        System.out.println("   Migration: Plan schema changes using current state analysis");
        
        System.out.println("\n⚡ Performance Optimization:");
        System.out.println("   Query Patterns: Design indexes to match application query patterns");
        System.out.println("   Data Locality: Use colocation to minimize cross-node operations");
        System.out.println("   Zone Tuning: Adjust partition and replica counts based on workload");
        System.out.println("   Index Strategy: Balance query performance with storage overhead");
        System.out.println("   Monitoring: Continuously monitor and adjust based on usage patterns");
        
        System.out.println("\n📋 Operational Considerations:");
        System.out.println("   ┌─────────────────┬─────────────────┬───────────────────┬──────────────┐");
        System.out.println("   │ Workload Type   │ Partitions      │ Replicas          │ Use Case     │");
        System.out.println("   ├─────────────────┼─────────────────┼───────────────────┼──────────────┤");
        System.out.println("   │ OLTP            │ 16-32           │ 3-5               │ Transactions │");
        System.out.println("   │ OLAP            │ 64-128          │ 2-3               │ Analytics    │");
        System.out.println("   │ Cache           │ 8-16            │ 5-7               │ Read-heavy   │");
        System.out.println("   │ Streaming       │ 128-256         │ 1-2               │ High-volume  │");
        System.out.println("   │ Development     │ 4-8             │ 1                 │ Testing      │");
        System.out.println("   └─────────────────┴─────────────────┴───────────────────┴──────────────┘");
        
        System.out.println("\n🛠️ Development Workflow:");
        System.out.println("   1. Design: Plan zone strategy and table relationships");
        System.out.println("   2. Create: Use DDL operations to create zones and tables");
        System.out.println("   3. Validate: Use introspection to verify schema structure");
        System.out.println("   4. Optimize: Add indexes and tune zone configurations");
        System.out.println("   5. Monitor: Continuously analyze performance and adjust");
        
        System.out.println("\n💡 Production Tips:");
        System.out.println("   • Version Control: Track schema changes in version control");
        System.out.println("   • Automation: Use scripts for repeatable schema deployment");
        System.out.println("   • Testing: Validate schema changes in staging environments");
        System.out.println("   • Monitoring: Implement alerting for schema health and performance");
        System.out.println("   • Documentation: Maintain up-to-date schema documentation");
        
        System.out.println("\n📚 Next Steps:");
        System.out.println("   • Apply these patterns to your specific use case");
        System.out.println("   • Experiment with different zone configurations");
        System.out.println("   • Build monitoring and alerting for your schema");
        System.out.println("   • Consider advanced features like cross-datacenter replication");
        System.out.println("   • Plan for schema evolution and migration strategies");
    }
}