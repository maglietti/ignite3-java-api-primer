package com.apache.ignite.examples.catalog;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Distribution Zones - Zone configuration and management with Apache Ignite 3.
 * 
 * This class demonstrates distribution zone operations:
 * - Creating zones for different workload patterns
 * - Configuring zone properties (partitions, replicas, storage)
 * - Zone management and monitoring
 * - Best practices for production environments
 * 
 * Learning Focus:
 * - Distribution zone concepts and configuration
 * - Workload isolation strategies
 * - Performance tuning through zone properties
 * - Zone lifecycle management
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
        IgniteSql sql = client.sql();
        
        System.out.println("\n--- Distribution Zone Operations ---");
        
        // Demonstrate core zone operations
        demonstrateZoneCreation(sql);
        demonstrateZoneConfiguration(sql);
        demonstrateZoneIntrospection(sql);
        demonstrateZoneManagement(sql);
        
        System.out.println("\n✓ Distribution zone operations completed successfully");
    }

    /**
     * Demonstrates creating distribution zones for different workloads.
     * 
     * Shows zone creation patterns:
     * - OLTP zones for transactional workloads
     * - OLAP zones for analytical workloads
     * - Caching zones for read-heavy patterns
     * - Streaming zones for high-throughput ingestion
     */
    private static void demonstrateZoneCreation(IgniteSql sql) {
        System.out.println("\n1. Distribution Zone Creation:");
        
        try {
            // Create OLTP zone for transactional workloads
            System.out.println("   ⚡ Creating OLTP zone for transactional workloads");
            
            String createOLTPZoneSQL = """
                CREATE ZONE IF NOT EXISTS MusicStoreOLTP WITH
                PARTITIONS = 16,
                REPLICAS = 3,
                DATA_NODES_AUTO_ADJUST_SCALE_UP = 3,
                DATA_NODES_AUTO_ADJUST_SCALE_DOWN = 3
                """;
            
            sql.execute(null, createOLTPZoneSQL);
            System.out.println("   ✓ Created MusicStoreOLTP zone (16 partitions, 3 replicas)");
            
            // Create OLAP zone for analytical workloads
            System.out.println("   ⚡ Creating OLAP zone for analytical workloads");
            
            String createOLAPZoneSQL = """
                CREATE ZONE IF NOT EXISTS MusicStoreOLAP WITH
                PARTITIONS = 64,
                REPLICAS = 2,
                DATA_NODES_AUTO_ADJUST_SCALE_UP = 5,
                DATA_NODES_AUTO_ADJUST_SCALE_DOWN = 2
                """;
            
            sql.execute(null, createOLAPZoneSQL);
            System.out.println("   ✓ Created MusicStoreOLAP zone (64 partitions, 2 replicas)");
            
            // Create cache zone for read-heavy workloads
            System.out.println("   ⚡ Creating cache zone for read-heavy workloads");
            
            String createCacheZoneSQL = """
                CREATE ZONE IF NOT EXISTS MusicStoreCache WITH
                PARTITIONS = 8,
                REPLICAS = 5,
                DATA_NODES_AUTO_ADJUST_SCALE_UP = 2,
                DATA_NODES_AUTO_ADJUST_SCALE_DOWN = 1
                """;
            
            sql.execute(null, createCacheZoneSQL);
            System.out.println("   ✓ Created MusicStoreCache zone (8 partitions, 5 replicas)");
            
        } catch (Exception e) {
            logger.error("Zone creation failed", e);
            System.err.println("   ⚠ Zone creation error: " + e.getMessage());
        }
    }

    /**
     * Demonstrates zone configuration and tuning.
     * 
     * Shows advanced zone configuration:
     * - Auto-scaling parameters
     * - Storage profile selection
     * - Performance optimization settings
     * - Resource allocation strategies
     */
    private static void demonstrateZoneConfiguration(IgniteSql sql) {
        System.out.println("\n2. Zone Configuration and Tuning:");
        
        try {
            // Create streaming zone with high-throughput configuration
            System.out.println("   ⚡ Creating streaming zone with high-throughput configuration");
            
            String createStreamingZoneSQL = """
                CREATE ZONE IF NOT EXISTS MusicStoreStreaming WITH
                PARTITIONS = 128,
                REPLICAS = 1,
                DATA_NODES_AUTO_ADJUST_SCALE_UP = 10,
                DATA_NODES_AUTO_ADJUST_SCALE_DOWN = 5
                """;
            
            sql.execute(null, createStreamingZoneSQL);
            System.out.println("   ✓ Created MusicStoreStreaming zone (128 partitions, 1 replica)");
            
            // Create development zone with minimal resources
            System.out.println("   ⚡ Creating development zone with minimal configuration");
            
            String createDevZoneSQL = """
                CREATE ZONE IF NOT EXISTS MusicStoreDev WITH
                PARTITIONS = 4,
                REPLICAS = 1,
                DATA_NODES_AUTO_ADJUST_SCALE_UP = 1,
                DATA_NODES_AUTO_ADJUST_SCALE_DOWN = 1
                """;
            
            sql.execute(null, createDevZoneSQL);
            System.out.println("   ✓ Created MusicStoreDev zone (4 partitions, 1 replica)");
            
        } catch (Exception e) {
            logger.error("Zone configuration failed", e);
            System.err.println("   ⚠ Zone configuration error: " + e.getMessage());
        }
    }

    /**
     * Demonstrates zone introspection and monitoring.
     * 
     * Shows how to inspect zone properties:
     * - List existing zones
     * - Query zone configurations
     * - Monitor zone health and performance
     * - Validate zone assignments
     */
    private static void demonstrateZoneIntrospection(IgniteSql sql) {
        System.out.println("\n3. Zone Introspection and Monitoring:");
        
        try {
            // List all distribution zones
            System.out.println("   ⚡ Listing all distribution zones");
            List<String> zones = getZoneList(sql);
            
            System.out.printf("   ✓ Found %d distribution zones:%n", zones.size());
            for (String zone : zones) {
                System.out.printf("     • %s%n", zone);
            }
            
            // Query zone configurations
            System.out.println("   ⚡ Querying zone configurations");
            displayZoneConfigurations(sql);
            
            // Validate expected zones exist
            System.out.println("   ⚡ Validating expected zones");
            String[] expectedZones = {
                "MusicStoreOLTP", "MusicStoreOLAP", "MusicStoreCache", 
                "MusicStoreStreaming", "MusicStoreDev"
            };
            
            for (String expectedZone : expectedZones) {
                if (zones.contains(expectedZone)) {
                    System.out.printf("   ✓ Zone exists: %s%n", expectedZone);
                } else {
                    System.out.printf("   ⚠ Zone missing: %s%n", expectedZone);
                }
            }
            
        } catch (Exception e) {
            logger.error("Zone introspection failed", e);
            System.err.println("   ⚠ Zone introspection error: " + e.getMessage());
        }
    }

    /**
     * Demonstrates zone management operations.
     * 
     * Shows zone lifecycle management:
     * - Altering zone properties
     * - Zone maintenance operations
     * - Performance monitoring
     * - Cleanup and removal
     */
    private static void demonstrateZoneManagement(IgniteSql sql) {
        System.out.println("\n4. Zone Management Operations:");
        
        try {
            // Alter zone configuration
            System.out.println("   ⚡ Altering zone configuration for scaling");
            
            String alterZoneSQL = """
                ALTER ZONE MusicStoreDev SET
                DATA_NODES_AUTO_ADJUST_SCALE_UP = 2,
                DATA_NODES_AUTO_ADJUST_SCALE_DOWN = 1
                """;
            
            sql.execute(null, alterZoneSQL);
            System.out.println("   ✓ Updated MusicStoreDev zone auto-scaling parameters");
            
            // Demonstrate zone best practices
            System.out.println("   ⚡ Zone configuration best practices:");
            displayZoneBestPractices();
            
        } catch (Exception e) {
            logger.error("Zone management failed", e);
            System.err.println("   ⚠ Zone management error: " + e.getMessage());
        }
        
        // Cleanup demo zones
        cleanupDemoZones(sql);
    }

    // Helper methods

    private static List<String> getZoneList(IgniteSql sql) {
        List<String> zones = new ArrayList<>();
        try {
            // Query system views to get zone list
            ResultSet<SqlRow> rs = sql.execute(null, 
                "SELECT ZONE_NAME FROM INFORMATION_SCHEMA.DISTRIBUTION_ZONES");
            
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                zones.add(row.stringValue("ZONE_NAME"));
            }
        } catch (Exception e) {
            logger.warn("Failed to get zone list", e);
            // If system tables are not available, return sample zones
            zones.add("Default");
        }
        return zones;
    }

    private static void displayZoneConfigurations(IgniteSql sql) {
        try {
            // Query zone configurations from system views
            ResultSet<SqlRow> rs = sql.execute(null, """
                SELECT ZONE_NAME, PARTITIONS, REPLICAS, DATA_NODES_AUTO_ADJUST_SCALE_UP, DATA_NODES_AUTO_ADJUST_SCALE_DOWN
                FROM INFORMATION_SCHEMA.DISTRIBUTION_ZONES
                WHERE ZONE_NAME LIKE 'MusicStore%'
                ORDER BY ZONE_NAME
                """);
            
            System.out.println("   ✓ Zone configurations:");
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                System.out.printf("     • %s: %d partitions, %d replicas, scale-up: %d, scale-down: %d%n",
                    row.stringValue("ZONE_NAME"),
                    row.intValue("PARTITIONS"),
                    row.intValue("REPLICAS"),
                    row.intValue("DATA_NODES_AUTO_ADJUST_SCALE_UP"),
                    row.intValue("DATA_NODES_AUTO_ADJUST_SCALE_DOWN"));
            }
            
        } catch (Exception e) {
            logger.warn("Failed to query zone configurations", e);
            System.out.println("   ⚠ Zone configuration details not available");
        }
    }

    private static void displayZoneBestPractices() {
        System.out.println("     • OLTP workloads: Higher replica count (3+) for consistency");
        System.out.println("     • OLAP workloads: More partitions (64+) for parallelism");
        System.out.println("     • Cache workloads: High replica count (5+) for availability");
        System.out.println("     • Streaming workloads: Many partitions (128+), fewer replicas");
        System.out.println("     • Development: Minimal resources (4 partitions, 1 replica)");
        System.out.println("     • Auto-scaling: Set conservative scale-up/down values");
        System.out.println("     • Storage profiles: Choose based on performance vs cost requirements");
    }

    private static void cleanupDemoZones(IgniteSql sql) {
        System.out.println("\n5. Cleanup Demo Zones:");
        
        try {
            // Drop demo zones (note: zones with tables cannot be dropped)
            String[] zonesToDrop = {
                "MusicStoreDev", "MusicStoreStreaming", "MusicStoreCache", 
                "MusicStoreOLAP", "MusicStoreOLTP"
            };
            
            for (String zone : zonesToDrop) {
                try {
                    sql.execute(null, "DROP ZONE IF EXISTS " + zone);
                    System.out.printf("   ✓ Dropped zone: %s%n", zone);
                } catch (Exception e) {
                    System.out.printf("   ⚠ Could not drop zone %s: %s%n", zone, e.getMessage());
                    logger.debug("Zone cleanup failed for " + zone, e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Zone cleanup failed", e);
            System.err.println("   ⚠ Zone cleanup error: " + e.getMessage());
        }
    }
}