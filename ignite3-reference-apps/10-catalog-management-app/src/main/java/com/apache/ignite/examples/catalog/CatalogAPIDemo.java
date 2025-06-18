package com.apache.ignite.examples.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Catalog management patterns for production cluster operations.
 * 
 * Execution order reflects operational dependencies:
 * 1. Zone configuration establishes data placement policies
 * 2. Schema operations create tables within configured zones
 * 3. Introspection validates deployment and monitors cluster state
 * 
 * Production workflow:
 * - Zones define resource allocation and availability requirements
 * - Schema operations use type-safe APIs to prevent runtime errors
 * - Introspection enables operational monitoring and troubleshooting
 */
public class CatalogAPIDemo {

    private static final Logger logger = LoggerFactory.getLogger(CatalogAPIDemo.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Apache Ignite 3 Catalog Management Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Zone configuration, schema operations, and catalog introspection");
        
        try {
            runCatalogDemonstrations(clusterAddress);
            System.out.println("\n=== Catalog management demonstrations completed successfully ===");
            
        } catch (Exception e) {
            logger.error("Demo execution failed", e);
            System.err.println("!!! Error: " + e.getMessage());
        }
    }

    private static void runCatalogDemonstrations(String clusterAddress) {
        runDistributionZonesDemo(clusterAddress);
        runSchemaOperationsDemo(clusterAddress);
        runCatalogIntrospectionDemo(clusterAddress);
    }
    
    private static void runDistributionZonesDemo(String clusterAddress) {
        System.out.println("\n=== [1/3] Distribution Zones");
        System.out.println("--- Zone configuration for different workload patterns");
        DistributionZones.main(new String[]{clusterAddress});
        System.out.println(">>> Distribution zones completed");
    }
    
    private static void runSchemaOperationsDemo(String clusterAddress) {
        System.out.println("\n=== [2/3] Schema Operations");
        System.out.println("--- Table creation and DDL operations using Catalog API");
        SchemaOperations.main(new String[]{clusterAddress});
        System.out.println(">>> Schema operations completed");
    }
    
    private static void runCatalogIntrospectionDemo(String clusterAddress) {
        System.out.println("\n=== [3/3] Catalog Introspection");
        System.out.println("--- Schema discovery and analysis using system catalogs");
        CatalogIntrospection.main(new String[]{clusterAddress});
        System.out.println(">>> Catalog introspection completed");
    }

}