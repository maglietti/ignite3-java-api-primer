package com.apache.ignite.examples.catalog;

import org.apache.ignite.catalog.IgniteCatalog;
import org.apache.ignite.catalog.definitions.ZoneDefinition;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Distribution zone patterns for workload isolation and performance optimization.
 * 
 * Zone partitioning affects data locality and query performance.
 * Different replica counts provide availability vs consistency trade-offs.
 * Workload-specific zones prevent resource contention between OLTP and OLAP operations.
 * 
 * Production considerations:
 * - OLTP zones: higher replicas for consistency, moderate partitions for latency
 * - OLAP zones: more partitions for parallelism, fewer replicas for storage efficiency
 * - Cache zones: maximum replicas for read availability, minimal partitions for coordination
 */
public class DistributionZones {

    private static final Logger logger = LoggerFactory.getLogger(DistributionZones.class);
    
    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Distribution Zones Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            runDistributionZoneOperations(client);
            
        } catch (Exception e) {
            logger.error("Failed to run distribution zone operations", e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void runDistributionZoneOperations(IgniteClient client) {
        IgniteCatalog catalog = client.catalog();
        IgniteSql sql = client.sql();
        
        System.out.println();
        System.out.println("--- Distribution Zone Operations ---");
        
        // Demonstrate core zone operations
        demonstrateZoneCreation(catalog);
        demonstrateZoneQuery(sql);
        demonstrateZoneCleanup(catalog);
        
        System.out.println();
        System.out.println(">>> Distribution zone operations completed successfully");
    }

    /**
     * Workload isolation through zone configuration prevents resource contention.
     * 
     * OLTP zones optimize for consistency and low latency with balanced partition counts.
     * OLAP zones maximize parallelism through high partition counts for scan operations.
     * Cache zones prioritize read availability with maximum replica counts.
     */
    private static void demonstrateZoneCreation(IgniteCatalog catalog) {
        System.out.println();
        System.out.println("1. Zone Creation for Different Workloads:");
        
        try {
            // OLTP optimization: moderate partitions, high replicas for consistency
            System.out.println("    Creating OLTP zone for transactional workloads");
            ZoneDefinition oltpZone = ZoneDefinition.builder("MusicStoreOLTP")
                .ifNotExists()
                .partitions(16)
                .replicas(3)
                .storageProfiles("default")
                .build();
            catalog.createZone(oltpZone);
            System.out.println("    >>> Created MusicStoreOLTP zone (16 partitions, 3 replicas)");
            
            // OLAP optimization: high partitions for scan parallelism, fewer replicas
            System.out.println("    Creating OLAP zone for analytical workloads");
            ZoneDefinition olapZone = ZoneDefinition.builder("MusicStoreOLAP")
                .ifNotExists()
                .partitions(64)
                .replicas(2)
                .storageProfiles("default")
                .build();
            catalog.createZone(olapZone);
            System.out.println("    >>> Created MusicStoreOLAP zone (64 partitions, 2 replicas)");
            
            // Cache optimization: maximum replicas for read availability
            System.out.println("    Creating cache zone for read-heavy workloads");
            ZoneDefinition cacheZone = ZoneDefinition.builder("MusicStoreCache")
                .ifNotExists()
                .partitions(8)
                .replicas(5)
                .storageProfiles("default")
                .build();
            catalog.createZone(cacheZone);
            System.out.println("    >>> Created MusicStoreCache zone (8 partitions, 5 replicas)");
            
        } catch (Exception e) {
            logger.error("Zone creation failed", e);
            System.err.println("    !!! Zone creation error: " + e.getMessage());
        }
    }

    /**
     * Zone verification prevents deployment errors and validates configuration.
     * 
     * System catalogs provide clean metadata access without verbose SQL output.
     * Zone properties affect data placement and query execution plans.
     * Operational validation confirms zone creation and configuration accuracy.
     */
    private static void demonstrateZoneQuery(IgniteSql sql) {
        System.out.println();
        System.out.println("2. Zone Verification and Introspection:");
        
        try {
            // Verify zone creation using clean system catalog queries
            System.out.println("    Verifying zone creation using system catalogs");
            
            // Query created zones using system catalogs
            ResultSet<SqlRow> zoneResults = sql.execute(null, 
                "SELECT NAME, PARTITIONS, REPLICAS FROM SYSTEM.ZONES WHERE NAME LIKE 'MUSICSTORE%' ORDER BY NAME");
            
            int zoneCount = 0;
            while (zoneResults.hasNext()) {
                SqlRow row = zoneResults.next();
                System.out.printf("    >>> Zone: %s (partitions: %d, replicas: %d)%n", 
                    row.stringValue("NAME"), 
                    row.intValue("PARTITIONS"), 
                    row.intValue("REPLICAS"));
                zoneCount++;
            }
            System.out.printf("    >>> Total zones verified: %d%n", zoneCount);
            
        } catch (Exception e) {
            logger.error("Zone verification failed", e);
            System.err.println("    !!! Zone verification error: " + e.getMessage());
        }
    }

    /**
     * Zone lifecycle management for capacity optimization and cost control.
     * 
     * Dependency checking prevents data loss during zone removal.
     * Cleanup order matters: tables must be dropped before their zones.
     * Failed cleanup indicates active table dependencies or ongoing transactions.
     */
    private static void demonstrateZoneCleanup(IgniteCatalog catalog) {
        System.out.println();
        System.out.println("3. Zone Cleanup Operations:");
        
        try {
            // Clean up demo zones using Catalog API (identifiers normalized to uppercase)
            System.out.println("    Cleaning up demo zones using Catalog API");
            
            String[] demoZones = {"MUSICSTOREOLTP", "MUSICSTOREOLAP", "MUSICSTORECACHE"};
            
            for (String zoneName : demoZones) {
                try {
                    catalog.dropZone(zoneName);
                    System.out.println("    >>> Dropped zone: " + zoneName);
                } catch (Exception e) {
                    // Zone might have dependencies, which is fine for demo
                    System.out.println("    !!! Zone " + zoneName + " has dependencies (tables using it)");
                }
            }
            
        } catch (Exception e) {
            logger.error("Zone cleanup failed", e);
            System.err.println("    !!! Zone cleanup error: " + e.getMessage());
        }
    }
}