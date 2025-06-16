/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apache.ignite.examples.catalog;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.client.IgniteClient.builder();
import org.apache.ignite.catalog.IgniteCatalog;
import org.apache.ignite.catalog.definitions.TableDefinition;
import org.apache.ignite.catalog.definitions.ColumnDefinition;
import org.apache.ignite.catalog.definitions.ZoneDefinition;
import org.apache.ignite.catalog.definitions.IndexDefinition;
import org.apache.ignite.catalog.definitions.ColumnSorted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Demonstrates schema introspection and analysis capabilities using Apache Ignite 3's catalog APIs.
 * 
 * This application shows how to:
 * - Discover existing tables and zones in a music streaming platform
 * - Analyze table structures including columns, indexes, and colocation strategies
 * - Generate schema reports for documentation and auditing
 * - Understand relationships between tables and their distribution zones
 * 
 * Schema introspection is essential for:
 * - Migration planning and schema evolution
 * - Performance optimization through understanding data distribution
 * - Documentation generation for development teams
 * - Audit compliance and schema validation
 */
public class SchemaOperations {
    
    private static final Logger logger = LoggerFactory.getLogger(SchemaOperations.class);
    
    // Connection configuration for music streaming platform
    private static final String IGNITE_HOST = "localhost";
    private static final int IGNITE_PORT = 10800;
    
    public static void main(String[] args) {
        logger.info("Starting Schema Operations demonstration...");
        
        // Connect to the Ignite cluster
        try (IgniteClient client = IgniteClient.builder().builder()
                .addresses(IGNITE_HOST + ":" + IGNITE_PORT)
                .build()) {
            
            logger.info("Connected to Ignite cluster at {}:{}", IGNITE_HOST, IGNITE_PORT);
            
            // Demonstrate catalog discovery
            discoverMusicStoreSchema(client);
            
            // Analyze specific tables for the music streaming platform
            analyzeMusicStoreTables(client);
            
            // Generate comprehensive schema reports
            generateSchemaDocumentation(client);
            
            // Demonstrate zone analysis for performance optimization
            analyzeDistributionZones(client);
            
            logger.info("Schema Operations demonstration completed successfully");
            
        } catch (Exception e) {
            logger.error("Schema operations failed", e);
            System.exit(1);
        }
    }
    
    /**
     * Discovers and catalogs the existing schema structure for a music streaming platform.
     * This method demonstrates basic catalog access patterns and provides an overview
     * of the distributed database structure.
     */
    private static void discoverMusicStoreSchema(IgniteClient client) {
        logger.info("=== Discovering Music Store Schema ===");
        
        IgniteCatalog catalog = client.catalog();
        
        try {
            // Discover all tables in the cluster
            Collection<String> tableNames = catalog.tables();
            logger.info("Found {} tables in the music store database", tableNames.size());
            
            // Filter for music store tables specifically
            List<String> musicTables = tableNames.stream()
                .filter(SchemaOperations::isMusicStoreTable)
                .sorted()
                .collect(Collectors.toList());
            
            if (musicTables.isEmpty()) {
                logger.warn("No music store tables found. Database may not be initialized.");
                return;
            }
            
            System.out.println("\n🎵 Music Store Tables:");
            for (String tableName : musicTables) {
                try {
                    TableDefinition tableDef = catalog.tableDefinition(tableName);
                    System.out.printf("  %-15s → Zone: %-20s Columns: %d%n", 
                        tableName, 
                        tableDef.zoneName(), 
                        tableDef.columns().size());
                } catch (Exception e) {
                    logger.warn("Could not analyze table {}: {}", tableName, e.getMessage());
                }
            }
            
            // Discover distribution zones
            Collection<String> zoneNames = catalog.zones();
            logger.info("Found {} distribution zones", zoneNames.size());
            
            System.out.println("\n🏗️  Distribution Zones:");
            for (String zoneName : zoneNames) {
                try {
                    ZoneDefinition zoneDef = catalog.zoneDefinition(zoneName);
                    List<String> tablesInZone = getTablesInZone(catalog, tableNames, zoneName);
                    
                    System.out.printf("  %-20s → Partitions: %d, Replicas: %d, Tables: %d%n",
                        zoneName,
                        zoneDef.partitions(),
                        zoneDef.replicas(),
                        tablesInZone.size());
                } catch (Exception e) {
                    logger.warn("Could not analyze zone {}: {}", zoneName, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to discover schema", e);
        }
    }
    
    /**
     * Performs detailed analysis of music store tables, focusing on structure,
     * indexing strategies, and colocation patterns that affect query performance.
     */
    private static void analyzeMusicStoreTables(IgniteClient client) {
        logger.info("=== Analyzing Music Store Table Structures ===");
        
        IgniteCatalog catalog = client.catalog();
        
        // Define tables of interest for detailed analysis
        String[] tablesToAnalyze = {"Artist", "Album", "Track", "Customer", "Invoice", "Playlist"};
        
        for (String tableName : tablesToAnalyze) {
            try {
                if (catalog.tables().contains(tableName)) {
                    analyzeTableStructure(catalog, tableName);
                } else {
                    logger.info("Table {} not found in the schema", tableName);
                }
            } catch (Exception e) {
                logger.warn("Failed to analyze table {}: {}", tableName, e.getMessage());
            }
        }
    }
    
    /**
     * Analyzes the structure of a specific table, providing insights into
     * columns, indexes, colocation strategy, and performance characteristics.
     */
    private static void analyzeTableStructure(IgniteCatalog catalog, String tableName) {
        try {
            TableDefinition tableDef = catalog.tableDefinition(tableName);
            
            System.out.println("\n📋 Table Analysis: " + tableName);
            System.out.println("─".repeat(50));
            
            // Basic table information
            System.out.println("Zone: " + tableDef.zoneName());
            System.out.println("Primary Key: " + tableDef.primaryKeyColumns());
            
            // Analyze colocation strategy for performance optimization
            List<String> colocationColumns = tableDef.colocationColumns();
            if (!colocationColumns.isEmpty()) {
                System.out.println("🎯 Colocation Strategy: " + colocationColumns);
                System.out.println("   → Related data is co-located for optimal query performance");
                System.out.println("   → Joins with colocated tables will be local operations");
            } else {
                System.out.println("⚠️  No colocation strategy defined");
            }
            
            // Column analysis with data type insights
            System.out.println("\n📊 Columns (" + tableDef.columns().size() + " total):");
            for (ColumnDefinition column : tableDef.columns()) {
                String nullable = column.nullable() ? "NULL" : "NOT NULL";
                String defaultValue = column.defaultValue() != null ? 
                    " DEFAULT " + column.defaultValue() : "";
                
                System.out.printf("  %-20s %-20s %-8s%s%n", 
                    column.name(),
                    column.type(),
                    nullable,
                    defaultValue);
            }
            
            // Index analysis for query optimization insights
            List<IndexDefinition> indexes = tableDef.indexes();
            if (!indexes.isEmpty()) {
                System.out.println("\n🚀 Indexes (" + indexes.size() + " total):");
                for (IndexDefinition index : indexes) {
                    System.out.printf("  %-30s %s on %s%n", 
                        index.name(),
                        index.type(),
                        index.columns());
                }
                System.out.println("   → Indexes accelerate query performance for these columns");
            } else {
                System.out.println("\n⚠️  No indexes defined - consider adding for query optimization");
            }
            
            // Provide table-specific insights
            provideTableInsights(tableName, tableDef);
            
        } catch (Exception e) {
            logger.error("Failed to analyze table structure for {}", tableName, e);
        }
    }
    
    /**
     * Provides specific insights and recommendations for different table types
     * in the music streaming platform based on their role and usage patterns.
     */
    private static void provideTableInsights(String tableName, TableDefinition tableDef) {
        System.out.println("\n💡 Table Insights:");
        
        switch (tableName.toLowerCase()) {
            case "artist":
                System.out.println("   → Root entity in music hierarchy");
                System.out.println("   → Consider indexes on Name for artist search functionality");
                System.out.println("   → Good candidate for caching due to read-heavy access patterns");
                break;
                
            case "album":
                List<String> albumColocation = tableDef.colocationColumns();
                if (albumColocation.contains("ArtistId")) {
                    System.out.println("   → ✅ Properly colocated with Artist for efficient joins");
                } else {
                    System.out.println("   → ⚠️  Consider colocation by ArtistId for better performance");
                }
                System.out.println("   → Indexes on Title and ArtistId support album browsing queries");
                break;
                
            case "track":
                List<String> trackColocation = tableDef.colocationColumns();
                if (trackColocation.contains("AlbumId")) {
                    System.out.println("   → ✅ Properly colocated with Album for efficient access");
                } else {
                    System.out.println("   → ⚠️  Consider colocation by AlbumId for better performance");
                }
                System.out.println("   → Critical table for streaming performance - optimize carefully");
                System.out.println("   → Indexes on GenreId and AlbumId support playlist generation");
                break;
                
            case "customer":
                System.out.println("   → Root entity for customer data hierarchy");
                System.out.println("   → Email index supports authentication and user lookup");
                System.out.println("   → Consider data privacy implications for customer information");
                break;
                
            case "invoice":
                List<String> invoiceColocation = tableDef.colocationColumns();
                if (invoiceColocation.contains("CustomerId")) {
                    System.out.println("   → ✅ Properly colocated with Customer for transaction efficiency");
                } else {
                    System.out.println("   → ⚠️  Consider colocation by CustomerId for better performance");
                }
                System.out.println("   → Date-based indexes support sales reporting and analytics");
                break;
                
            case "playlist":
                System.out.println("   → High-frequency read/write operations from user interactions");
                System.out.println("   → Consider separate zone for user-generated content");
                System.out.println("   → Optimize for concurrent access patterns");
                break;
                
            default:
                System.out.println("   → Analyze usage patterns to optimize structure and indexing");
        }
    }
    
    /**
     * Generates comprehensive schema documentation that can be used for
     * development team reference, audit compliance, and migration planning.
     */
    private static void generateSchemaDocumentation(IgniteClient client) {
        logger.info("=== Generating Schema Documentation ===");
        
        IgniteCatalog catalog = client.catalog();
        
        try {
            System.out.println("\n📚 Music Store Database Schema Report");
            System.out.println("═".repeat(60));
            System.out.println("Generated: " + LocalDateTime.now());
            System.out.println("Platform: Apache Ignite 3 Distributed Database");
            
            // Document distribution zones with their purpose and configuration
            Collection<String> zones = catalog.zones();
            System.out.println("\n🏗️  Distribution Zones (" + zones.size() + " total):");
            
            for (String zoneName : zones) {
                try {
                    documentZone(catalog, zoneName);
                } catch (Exception e) {
                    logger.warn("Could not document zone {}: {}", zoneName, e.getMessage());
                }
            }
            
            // Document tables grouped by their distribution zones
            Collection<String> tables = catalog.tables();
            Map<String, List<String>> tablesByZone = groupTablesByZone(catalog, tables);
            
            System.out.println("\n📋 Tables by Distribution Zone:");
            for (Map.Entry<String, List<String>> entry : tablesByZone.entrySet()) {
                String zoneName = entry.getKey();
                List<String> zoneTables = entry.getValue();
                
                System.out.println("\n" + zoneName + " Zone:");
                for (String tableName : zoneTables) {
                    try {
                        TableDefinition tableDef = catalog.tableDefinition(tableName);
                        System.out.printf("  %-15s → %d columns", tableName, tableDef.columns().size());
                        
                        if (!tableDef.colocationColumns().isEmpty()) {
                            System.out.printf(" (colocated by %s)", tableDef.colocationColumns());
                        }
                        
                        if (!tableDef.indexes().isEmpty()) {
                            System.out.printf(" [%d indexes]", tableDef.indexes().size());
                        }
                        
                        System.out.println();
                    } catch (Exception e) {
                        logger.warn("Could not document table {}: {}", tableName, e.getMessage());
                    }
                }
            }
            
            // Document data relationships and colocation strategy
            System.out.println("\n🔗 Data Relationships & Colocation Strategy:");
            System.out.println("  Artist → Album → Track (hierarchical colocation)");
            System.out.println("  Customer → Invoice → InvoiceLine (customer-centric colocation)");
            System.out.println("  Playlist ↔ Track (many-to-many with separate association table)");
            System.out.println("\n  Benefits:");
            System.out.println("  • Related data stored on same nodes for efficient joins");
            System.out.println("  • Reduced network overhead for common query patterns");
            System.out.println("  • Improved transaction performance for related operations");
            
        } catch (Exception e) {
            logger.error("Failed to generate schema documentation", e);
        }
    }
    
    /**
     * Documents the configuration and purpose of a distribution zone,
     * providing insights into its role in the overall data architecture.
     */
    private static void documentZone(IgniteCatalog catalog, String zoneName) {
        try {
            ZoneDefinition zoneDef = catalog.zoneDefinition(zoneName);
            
            System.out.printf("\n  %s:%n", zoneName);
            System.out.printf("    Purpose: %s%n", getZonePurpose(zoneName));
            System.out.printf("    Partitions: %d (controls parallelism)%n", zoneDef.partitions());
            System.out.printf("    Replicas: %d (controls availability)%n", zoneDef.replicas());
            System.out.printf("    Auto-adjust: %d seconds (scaling responsiveness)%n", 
                zoneDef.dataNodesAutoAdjust());
            System.out.printf("    Storage: %s%n", zoneDef.storageProfiles());
            
            if (zoneDef.filter() != null && !zoneDef.filter().isEmpty()) {
                System.out.printf("    Node Filter: %s%n", zoneDef.filter());
            }
            
        } catch (Exception e) {
            logger.warn("Could not document zone {}: {}", zoneName, e.getMessage());
        }
    }
    
    /**
     * Analyzes distribution zones to understand performance characteristics
     * and optimization opportunities for the music streaming workload.
     */
    private static void analyzeDistributionZones(IgniteClient client) {
        logger.info("=== Analyzing Distribution Zone Performance ===");
        
        IgniteCatalog catalog = client.catalog();
        
        try {
            Collection<String> zones = catalog.zones();
            
            System.out.println("\n⚡ Zone Performance Analysis:");
            
            for (String zoneName : zones) {
                try {
                    ZoneDefinition zoneDef = catalog.zoneDefinition(zoneName);
                    List<String> tablesInZone = getTablesInZone(catalog, catalog.tables(), zoneName);
                    
                    System.out.printf("\n📊 %s Zone:%n", zoneName);
                    
                    // Analyze configuration for workload suitability
                    analyzeZoneConfiguration(zoneDef, tablesInZone);
                    
                    // Provide optimization recommendations
                    provideZoneRecommendations(zoneName, zoneDef, tablesInZone);
                    
                } catch (Exception e) {
                    logger.warn("Could not analyze zone {}: {}", zoneName, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to analyze distribution zones", e);
        }
    }
    
    /**
     * Analyzes zone configuration parameters and their impact on performance
     * for different types of workloads in the music streaming platform.
     */
    private static void analyzeZoneConfiguration(ZoneDefinition zoneDef, List<String> tables) {
        int partitions = zoneDef.partitions();
        int replicas = zoneDef.replicas();
        int autoAdjust = zoneDef.dataNodesAutoAdjust();
        
        System.out.printf("  Configuration: %d partitions, %d replicas%n", partitions, replicas);
        System.out.printf("  Tables: %s%n", tables);
        
        // Analyze partition count for parallelism
        if (partitions >= 32) {
            System.out.println("  ✅ High parallelism configuration - good for analytics workloads");
        } else if (partitions >= 16) {
            System.out.println("  ✅ Balanced parallelism - suitable for mixed workloads");
        } else {
            System.out.println("  ⚠️  Lower parallelism - may limit throughput for large datasets");
        }
        
        // Analyze replication for availability vs performance
        if (replicas >= 3) {
            System.out.println("  ✅ High availability configuration - good for critical data");
        } else if (replicas == 2) {
            System.out.println("  ✅ Balanced availability/performance configuration");
        } else {
            System.out.println("  ⚠️  Single replica - maximum performance but no fault tolerance");
        }
        
        // Analyze auto-scaling responsiveness
        if (autoAdjust <= 60) {
            System.out.println("  ✅ Fast auto-scaling - responsive to traffic changes");
        } else if (autoAdjust <= 300) {
            System.out.println("  ✅ Moderate auto-scaling - stable for normal operations");
        } else {
            System.out.println("  ⚠️  Slow auto-scaling - may not respond quickly to traffic spikes");
        }
    }
    
    /**
     * Provides specific optimization recommendations for different zone types
     * based on their intended workload and current configuration.
     */
    private static void provideZoneRecommendations(String zoneName, ZoneDefinition zoneDef, 
                                                  List<String> tables) {
        System.out.println("  💡 Recommendations:");
        
        if (zoneName.toLowerCase().contains("catalog") || zoneName.toLowerCase().contains("music")) {
            System.out.println("    • Transactional zone - optimize for consistency and availability");
            System.out.println("    • Consider read replicas for heavy catalog browsing workloads");
            if (zoneDef.replicas() < 2) {
                System.out.println("    • ⚠️  Increase replicas to at least 2 for high availability");
            }
        } else if (zoneName.toLowerCase().contains("analytics")) {
            System.out.println("    • Analytics zone - optimize for read throughput and parallelism");
            System.out.println("    • Single replica acceptable for cost efficiency");
            if (zoneDef.partitions() < 32) {
                System.out.println("    • Consider increasing partitions for better analytics parallelism");
            }
        } else if (zoneName.toLowerCase().contains("streaming") || zoneName.toLowerCase().contains("events")) {
            System.out.println("    • Streaming zone - optimize for write throughput");
            System.out.println("    • Fast auto-scaling essential for traffic spikes");
            if (zoneDef.dataNodesAutoAdjust() > 60) {
                System.out.println("    • ⚠️  Consider faster auto-scaling (≤60s) for streaming workloads");
            }
        } else {
            System.out.println("    • Review zone purpose and optimize configuration accordingly");
        }
        
        // Table-specific recommendations
        if (tables.contains("Track") || tables.contains("Playlist")) {
            System.out.println("    • High-frequency access tables detected");
            System.out.println("    • Monitor query patterns and consider additional indexes");
        }
        
        if (tables.contains("Customer") || tables.contains("Invoice")) {
            System.out.println("    • Transactional data detected - ensure ACID compliance");
            System.out.println("    • Consider data privacy and retention policies");
        }
    }
    
    // Helper methods
    
    /**
     * Determines if a table belongs to the music store domain based on naming patterns.
     */
    private static boolean isMusicStoreTable(String tableName) {
        return tableName.matches("(?i)(artist|album|track|customer|playlist|invoice|genre|media).*");
    }
    
    /**
     * Groups tables by their distribution zone for organizational analysis.
     */
    private static Map<String, List<String>> groupTablesByZone(IgniteCatalog catalog, 
                                                              Collection<String> tables) {
        Map<String, List<String>> tablesByZone = new HashMap<>();
        
        for (String tableName : tables) {
            try {
                TableDefinition tableDef = catalog.tableDefinition(tableName);
                String zoneName = tableDef.zoneName();
                
                tablesByZone.computeIfAbsent(zoneName, k -> new ArrayList<>()).add(tableName);
                
            } catch (Exception e) {
                logger.warn("Could not determine zone for table {}: {}", tableName, e.getMessage());
            }
        }
        
        return tablesByZone;
    }
    
    /**
     * Gets all tables that belong to a specific distribution zone.
     */
    private static List<String> getTablesInZone(IgniteCatalog catalog, Collection<String> allTables, 
                                               String zoneName) {
        List<String> tablesInZone = new ArrayList<>();
        
        for (String tableName : allTables) {
            try {
                TableDefinition tableDef = catalog.tableDefinition(tableName);
                if (zoneName.equals(tableDef.zoneName())) {
                    tablesInZone.add(tableName);
                }
            } catch (Exception e) {
                logger.warn("Could not check zone for table {}: {}", tableName, e.getMessage());
            }
        }
        
        return tablesInZone;
    }
    
    /**
     * Determines the business purpose of a distribution zone based on naming conventions.
     */
    private static String getZonePurpose(String zoneName) {
        String lowerName = zoneName.toLowerCase();
        
        if (lowerName.contains("catalog") || lowerName.contains("music")) {
            return "Core music catalog data - high consistency, moderate throughput";
        } else if (lowerName.contains("analytics")) {
            return "Analytics and reporting - read-optimized, high parallelism";
        } else if (lowerName.contains("streaming") || lowerName.contains("events")) {
            return "High-throughput streaming data - write-optimized, fast scaling";
        } else if (lowerName.contains("customer") || lowerName.contains("transaction")) {
            return "Customer and transaction data - ACID compliance, high availability";
        } else {
            return "General purpose data storage";
        }
    }
}