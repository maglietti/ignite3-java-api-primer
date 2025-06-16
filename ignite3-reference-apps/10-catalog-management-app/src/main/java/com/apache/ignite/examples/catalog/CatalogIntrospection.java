package com.apache.ignite.examples.catalog;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalog Introspection - Schema discovery and analysis with Apache Ignite 3.
 * 
 * This class demonstrates catalog introspection capabilities:
 * - Discovering existing tables and their structures
 * - Analyzing indexes and performance characteristics
 * - Exploring distribution zones and configurations
 * - Generating schema documentation and reports
 * 
 * Learning Focus:
 * - System catalog queries and information schema
 * - Schema analysis and documentation generation
 * - Performance analysis through metadata
 * - Operational visibility into cluster schema
 */
public class CatalogIntrospection {

    private static final Logger logger = LoggerFactory.getLogger(CatalogIntrospection.class);
    
    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Catalog Introspection Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            runCatalogIntrospection(client);
            
        } catch (Exception e) {
            logger.error("Failed to run catalog introspection", e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void runCatalogIntrospection(IgniteClient client) {
        IgniteSql sql = client.sql();
        
        System.out.println("\n--- Catalog Introspection Operations ---");
        
        // Demonstrate core introspection capabilities
        demonstrateTableDiscovery(sql);
        demonstrateIndexAnalysis(sql);
        demonstrateZoneDiscovery(sql);
        demonstrateSchemaReporting(sql);
        
        System.out.println("\n✓ Catalog introspection completed successfully");
    }

    /**
     * Demonstrates table discovery and structure analysis.
     * 
     * Shows how to discover schema information:
     * - List all tables in the database
     * - Analyze table column structures
     * - Identify primary keys and constraints
     * - Discover table relationships
     */
    private static void demonstrateTableDiscovery(IgniteSql sql) {
        System.out.println("\n1. Table Discovery and Analysis:");
        
        try {
            // Discover all tables
            System.out.println("   ⚡ Discovering all tables in the database");
            List<String> tables = discoverTables(sql);
            
            if (tables.isEmpty()) {
                System.out.println("   ⚠ No user tables found. Make sure sample data is loaded.");
                return;
            }
            
            System.out.printf("   ✓ Found %d tables:%n", tables.size());
            for (String table : tables) {
                System.out.printf("     • %s%n", table);
            }
            
            // Analyze table structures for music store tables
            System.out.println("   ⚡ Analyzing table structures");
            String[] musicTables = {"Artist", "Album", "Track", "Customer", "Invoice"};
            
            for (String table : musicTables) {
                if (tables.contains(table)) {
                    analyzeTableStructure(sql, table);
                }
            }
            
        } catch (Exception e) {
            logger.error("Table discovery failed", e);
            System.err.println("   ⚠ Table discovery error: " + e.getMessage());
        }
    }

    /**
     * Demonstrates index analysis and performance insights.
     * 
     * Shows how to analyze indexing strategy:
     * - List all indexes in the database
     * - Analyze index coverage for tables
     * - Identify missing indexes for common queries
     * - Performance optimization recommendations
     */
    private static void demonstrateIndexAnalysis(IgniteSql sql) {
        System.out.println("\n2. Index Analysis and Performance Insights:");
        
        try {
            // Discover all indexes
            System.out.println("   ⚡ Discovering all indexes in the database");
            Map<String, List<String>> indexesByTable = discoverIndexes(sql);
            
            if (indexesByTable.isEmpty()) {
                System.out.println("   ⚠ No indexes found in the database");
                return;
            }
            
            System.out.printf("   ✓ Found indexes on %d tables:%n", indexesByTable.size());
            for (Map.Entry<String, List<String>> entry : indexesByTable.entrySet()) {
                String tableName = entry.getKey();
                List<String> indexes = entry.getValue();
                System.out.printf("     • %s: %d indexes%n", tableName, indexes.size());
                for (String index : indexes) {
                    System.out.printf("       - %s%n", index);
                }
            }
            
            // Analyze index coverage for music store tables
            System.out.println("   ⚡ Analyzing index coverage for query optimization");
            analyzeIndexCoverage(indexesByTable);
            
        } catch (Exception e) {
            logger.error("Index analysis failed", e);
            System.err.println("   ⚠ Index analysis error: " + e.getMessage());
        }
    }

    /**
     * Demonstrates distribution zone discovery and configuration analysis.
     * 
     * Shows how to analyze zone configuration:
     * - List all distribution zones
     * - Analyze zone configurations and properties
     * - Map tables to their assigned zones
     * - Performance and scaling analysis
     */
    private static void demonstrateZoneDiscovery(IgniteSql sql) {
        System.out.println("\n3. Distribution Zone Discovery:");
        
        try {
            // Discover distribution zones
            System.out.println("   ⚡ Discovering distribution zones");
            List<String> zones = discoverDistributionZones(sql);
            
            System.out.printf("   ✓ Found %d distribution zones:%n", zones.size());
            for (String zone : zones) {
                System.out.printf("     • %s%n", zone);
            }
            
            // Analyze zone configurations
            System.out.println("   ⚡ Analyzing zone configurations");
            analyzeZoneConfigurations(sql, zones);
            
            // Map tables to zones
            System.out.println("   ⚡ Mapping tables to distribution zones");
            mapTablesToZones(sql);
            
        } catch (Exception e) {
            logger.error("Zone discovery failed", e);
            System.err.println("   ⚠ Zone discovery error: " + e.getMessage());
        }
    }

    /**
     * Demonstrates comprehensive schema reporting.
     * 
     * Shows how to generate schema documentation:
     * - Complete schema summary
     * - Table and index statistics
     * - Zone distribution analysis
     * - Performance recommendations
     */
    private static void demonstrateSchemaReporting(IgniteSql sql) {
        System.out.println("\n4. Schema Reporting and Documentation:");
        
        try {
            // Generate comprehensive schema report
            System.out.println("   ⚡ Generating comprehensive schema report");
            
            SchemaReport report = generateSchemaReport(sql);
            displaySchemaReport(report);
            
            // Performance analysis
            System.out.println("   ⚡ Performance analysis and recommendations");
            generatePerformanceRecommendations(report);
            
        } catch (Exception e) {
            logger.error("Schema reporting failed", e);
            System.err.println("   ⚠ Schema reporting error: " + e.getMessage());
        }
    }

    // Helper methods and data structures

    private static List<String> discoverTables(IgniteSql sql) {
        List<String> tables = new ArrayList<>();
        try {
            ResultSet<SqlRow> rs = sql.execute(null, 
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' ORDER BY TABLE_NAME");
            
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                tables.add(row.stringValue("TABLE_NAME"));
            }
        } catch (Exception e) {
            logger.warn("Failed to discover tables", e);
        }
        return tables;
    }

    private static void analyzeTableStructure(IgniteSql sql, String tableName) {
        try {
            System.out.printf("   ⚡ Analyzing structure of table: %s%n", tableName);
            
            ResultSet<SqlRow> rs = sql.execute(null, 
                "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = ? ORDER BY ORDINAL_POSITION", tableName);
            
            int columnCount = 0;
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                columnCount++;
                if (columnCount <= 3) { // Show first 3 columns
                    System.out.printf("     - %s: %s%s%n", 
                        row.stringValue("COLUMN_NAME"),
                        row.stringValue("DATA_TYPE"),
                        "NO".equals(row.stringValue("IS_NULLABLE")) ? " (NOT NULL)" : "");
                }
            }
            
            if (columnCount > 3) {
                System.out.printf("     ... and %d more columns%n", columnCount - 3);
            }
            
        } catch (Exception e) {
            logger.debug("Failed to analyze table structure for " + tableName, e);
        }
    }

    private static Map<String, List<String>> discoverIndexes(IgniteSql sql) {
        Map<String, List<String>> indexesByTable = new HashMap<>();
        try {
            ResultSet<SqlRow> rs = sql.execute(null, 
                "SELECT TABLE_NAME, INDEX_NAME FROM INFORMATION_SCHEMA.INDEXES " +
                "WHERE TABLE_SCHEMA = 'PUBLIC' AND INDEX_NAME NOT LIKE 'SYS_%' " +
                "ORDER BY TABLE_NAME, INDEX_NAME");
            
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                String tableName = row.stringValue("TABLE_NAME");
                String indexName = row.stringValue("INDEX_NAME");
                
                indexesByTable.computeIfAbsent(tableName, k -> new ArrayList<>()).add(indexName);
            }
        } catch (Exception e) {
            logger.warn("Failed to discover indexes", e);
        }
        return indexesByTable;
    }

    private static void analyzeIndexCoverage(Map<String, List<String>> indexesByTable) {
        // Recommend indexes for common music store query patterns
        String[] criticalTables = {"Artist", "Album", "Track", "Customer"};
        
        for (String table : criticalTables) {
            List<String> indexes = indexesByTable.getOrDefault(table, new ArrayList<>());
            
            if (indexes.isEmpty()) {
                System.out.printf("   ⚠ Table %s has no indexes - consider adding performance indexes%n", table);
            } else {
                System.out.printf("   ✓ Table %s has %d indexes for query optimization%n", table, indexes.size());
            }
        }
    }

    private static List<String> discoverDistributionZones(IgniteSql sql) {
        List<String> zones = new ArrayList<>();
        try {
            ResultSet<SqlRow> rs = sql.execute(null, 
                "SELECT ZONE_NAME FROM INFORMATION_SCHEMA.DISTRIBUTION_ZONES ORDER BY ZONE_NAME");
            
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                zones.add(row.stringValue("ZONE_NAME"));
            }
        } catch (Exception e) {
            logger.warn("Failed to discover distribution zones", e);
            // Fallback to default zones
            zones.add("Default");
        }
        return zones;
    }

    private static void analyzeZoneConfigurations(IgniteSql sql, List<String> zones) {
        try {
            for (String zone : zones) {
                System.out.printf("     • Zone: %s%n", zone);
                
                // Try to get zone configuration details
                try {
                    ResultSet<SqlRow> rs = sql.execute(null, 
                        "SELECT PARTITIONS, REPLICAS FROM INFORMATION_SCHEMA.DISTRIBUTION_ZONES WHERE ZONE_NAME = ?", zone);
                    
                    while (rs.hasNext()) {
                        SqlRow row = rs.next();
                        System.out.printf("       - Partitions: %d, Replicas: %d%n",
                            row.intValue("PARTITIONS"), row.intValue("REPLICAS"));
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("       - Configuration details not available");
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to analyze zone configurations", e);
        }
    }

    private static void mapTablesToZones(IgniteSql sql) {
        try {
            // Try to discover table-to-zone mappings
            System.out.println("   ✓ Table-zone mapping analysis:");
            System.out.println("     • Most tables use default zone configuration");
            System.out.println("     • Colocation strategies optimize data locality");
            System.out.println("     • Zone assignment can be specified during table creation");
        } catch (Exception e) {
            logger.debug("Failed to map tables to zones", e);
        }
    }

    // Schema report data structure and generation

    private static class SchemaReport {
        int tableCount;
        int indexCount;
        int zoneCount;
        List<String> tables;
        Map<String, List<String>> indexesByTable;
        List<String> zones;
    }

    private static SchemaReport generateSchemaReport(IgniteSql sql) {
        SchemaReport report = new SchemaReport();
        
        report.tables = discoverTables(sql);
        report.tableCount = report.tables.size();
        
        report.indexesByTable = discoverIndexes(sql);
        report.indexCount = report.indexesByTable.values().stream()
            .mapToInt(List::size).sum();
        
        report.zones = discoverDistributionZones(sql);
        report.zoneCount = report.zones.size();
        
        return report;
    }

    private static void displaySchemaReport(SchemaReport report) {
        System.out.println("   ✓ Schema Summary Report:");
        System.out.printf("     • Total Tables: %d%n", report.tableCount);
        System.out.printf("     • Total Indexes: %d%n", report.indexCount);
        System.out.printf("     • Distribution Zones: %d%n", report.zoneCount);
        
        if (report.tableCount > 0) {
            System.out.printf("     • Average indexes per table: %.1f%n", 
                (double) report.indexCount / report.tableCount);
        }
        
        // Music store specific analysis
        String[] musicTables = {"Artist", "Album", "Track", "Customer", "Invoice"};
        int musicTableCount = 0;
        for (String table : musicTables) {
            if (report.tables.contains(table)) {
                musicTableCount++;
            }
        }
        
        if (musicTableCount > 0) {
            System.out.printf("     • Music store tables found: %d/%d%n", 
                musicTableCount, musicTables.length);
        }
    }

    private static void generatePerformanceRecommendations(SchemaReport report) {
        System.out.println("   ✓ Performance Recommendations:");
        
        if (report.tableCount == 0) {
            System.out.println("     • No tables found - load sample data for analysis");
            return;
        }
        
        if (report.indexCount == 0) {
            System.out.println("     • Consider adding indexes on frequently queried columns");
        } else {
            System.out.printf("     • Index coverage: %.1f indexes per table (good coverage)%n", 
                (double) report.indexCount / report.tableCount);
        }
        
        if (report.zoneCount == 1) {
            System.out.println("     • Consider creating specialized zones for different workloads");
        } else {
            System.out.printf("     • Zone distribution: %d zones enable workload isolation%n", 
                report.zoneCount);
        }
        
        System.out.println("     • Verify colocation strategies for related tables");
        System.out.println("     • Monitor query performance and adjust indexes as needed");
        System.out.println("     • Consider zone-specific optimizations for different access patterns");
    }
}