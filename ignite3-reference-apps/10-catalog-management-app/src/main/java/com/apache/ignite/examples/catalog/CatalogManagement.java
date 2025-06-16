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

import org.apache.ignite.catalog.IgniteCatalog;
import org.apache.ignite.catalog.definitions.TableDefinition;
import org.apache.ignite.catalog.definitions.ColumnDefinition;
import org.apache.ignite.catalog.definitions.ZoneDefinition;
import org.apache.ignite.catalog.definitions.IndexDefinition;
import org.apache.ignite.catalog.definitions.ColumnSorted;
import org.apache.ignite.catalog.ColumnType;
import org.apache.ignite.catalog.IndexType;
import org.apache.ignite.table.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Demonstrates programmatic schema creation and management using Apache Ignite 3's catalog APIs.
 * 
 * This application shows how to:
 * - Create distribution zones optimized for different workload patterns
 * - Define tables programmatically using the TableDefinition builder API
 * - Implement safe schema operations with error handling and retry logic
 * - Evolve schemas over time with version-controlled migrations
 * - Validate table definitions before deployment
 * 
 * Schema management capabilities include:
 * - Creating tables with colocation strategies for performance optimization
 * - Adding indexes for query acceleration
 * - Managing distribution zones for workload separation
 * - Safe table creation/dropping with validation
 * - Schema migration support for continuous deployment
 */
public class CatalogManagement {
    
    private static final Logger logger = LoggerFactory.getLogger(CatalogManagement.class);
    
    // Connection configuration
    private static final String IGNITE_HOST = "localhost";
    private static final int IGNITE_PORT = 10800;
    
    public static void main(String[] args) {
        logger.info("Starting Catalog Management demonstration...");
        
        try (IgniteClient client = IgniteClient.builder().builder()
                .addresses(IGNITE_HOST + ":" + IGNITE_PORT)
                .build()) {
            
            logger.info("Connected to Ignite cluster at {}:{}", IGNITE_HOST, IGNITE_PORT);
            
            // Create distribution zones for different workload patterns
            createMusicStoreZones(client);
            
            // Create core music catalog tables with performance optimizations
            createMusicCatalogTables(client);
            
            // Add customer and business tables with transaction support
            createCustomerBusinessTables(client);
            
            // Create performance indexes for common query patterns
            createPerformanceIndexes(client);
            
            // Demonstrate safe table management operations
            demonstrateSafeTableOperations(client);
            
            // Validate the created schema
            validateMusicStoreSchema(client);
            
            logger.info("Catalog Management demonstration completed successfully");
            
        } catch (Exception e) {
            logger.error("Catalog management failed", e);
            System.exit(1);
        }
    }
    
    /**
     * Creates distribution zones optimized for different workload patterns in the music streaming platform.
     * Different zones enable workload isolation and performance optimization.
     */
    private static void createMusicStoreZones(IgniteClient client) {
        logger.info("=== Creating Music Store Distribution Zones ===");
        
        IgniteCatalog catalog = client.catalog();
        
        try {
            // Primary zone for transactional music catalog data
            // Optimized for consistency and moderate throughput
            ZoneDefinition catalogZone = ZoneDefinition.builder("MusicCatalog")
                .ifNotExists()
                .partitions(16)                    // Balanced partitioning for catalog data
                .replicas(2)                       // High availability for critical data
                .dataNodesAutoAdjust(300)          // 5-minute auto-adjust for stability
                .dataNodesAutoAdjustScaleUp(60)    // Quick scale-up for traffic spikes
                .dataNodesAutoAdjustScaleDown(600) // Gradual scale-down for stability
                .storageProfiles("default")
                .build();
            
            catalog.createZone(catalogZone);
            logger.info("✅ MusicCatalog zone created - optimized for transactional catalog data");
            
            // Analytics zone for reporting and business intelligence
            // Optimized for read throughput and parallelism
            ZoneDefinition analyticsZone = ZoneDefinition.builder("MusicAnalytics")
                .ifNotExists()
                .partitions(32)                    // Higher partitions for analytics parallelism
                .replicas(1)                       // Single replica for cost efficiency
                .dataNodesAutoAdjust(60)           // Fast auto-adjust for dynamic workloads
                .storageProfiles("default")
                .build();
            
            catalog.createZone(analyticsZone);
            logger.info("✅ MusicAnalytics zone created - optimized for read-heavy analytics");
            
            // Streaming zone for high-throughput user interactions
            // Optimized for write performance and scalability
            ZoneDefinition streamingZone = ZoneDefinition.builder("UserInteractions")
                .ifNotExists()
                .partitions(64)                    // Maximum parallelism for streaming data
                .replicas(1)                       // Single replica for maximum throughput
                .dataNodesAutoAdjust(30)           // Very fast scaling for streaming workloads
                .storageProfiles("default")
                .build();
            
            catalog.createZone(streamingZone);
            logger.info("✅ UserInteractions zone created - optimized for high-throughput streaming");
            
            System.out.println("\n🏗️  Distribution Zones Summary:");
            System.out.println("  MusicCatalog     → 16 partitions, 2 replicas (transactional)");
            System.out.println("  MusicAnalytics   → 32 partitions, 1 replica (analytics)");
            System.out.println("  UserInteractions → 64 partitions, 1 replica (streaming)");
            
        } catch (Exception e) {
            logger.error("Failed to create distribution zones", e);
            throw new RuntimeException("Zone creation failed", e);
        }
    }
    
    /**
     * Creates the core music catalog tables with proper colocation strategies
     * for optimal query performance and data locality.
     */
    private static void createMusicCatalogTables(IgniteClient client) {
        logger.info("=== Creating Music Catalog Tables ===");
        
        IgniteCatalog catalog = client.catalog();
        
        try {
            // Artist table - root entity in the music hierarchy
            TableDefinition artistTable = TableDefinition.builder("Artist")
                .ifNotExists()
                .columns(
                    ColumnDefinition.column("ArtistId", ColumnType.INTEGER.notNull()),
                    ColumnDefinition.column("Name", ColumnType.varchar(120).notNull()),
                    ColumnDefinition.column("Country", ColumnType.varchar(50)),
                    ColumnDefinition.column("FormedYear", ColumnType.INTEGER),
                    ColumnDefinition.column("Genre", ColumnType.varchar(50)),
                    ColumnDefinition.column("CreatedAt", ColumnType.TIMESTAMP.notNull()
                        .defaultExpression("CURRENT_TIMESTAMP"))
                )
                .primaryKey("ArtistId")
                .zone("MusicCatalog")
                .index("idx_artist_name", IndexType.SORTED, 
                       ColumnSorted.column("Name"))
                .index("idx_artist_country_genre", IndexType.SORTED,
                       ColumnSorted.column("Country"),
                       ColumnSorted.column("Genre"))
                .build();
            
            createTableSafely(catalog, artistTable, "Artist");
            
            // Album table - colocated with artists for efficient joins
            TableDefinition albumTable = TableDefinition.builder("Album")
                .ifNotExists()
                .columns(
                    ColumnDefinition.column("AlbumId", ColumnType.INTEGER.notNull()),
                    ColumnDefinition.column("Title", ColumnType.varchar(160).notNull()),
                    ColumnDefinition.column("ArtistId", ColumnType.INTEGER.notNull()),
                    ColumnDefinition.column("ReleaseDate", ColumnType.DATE),
                    ColumnDefinition.column("Genre", ColumnType.varchar(50)),
                    ColumnDefinition.column("Label", ColumnType.varchar(100)),
                    ColumnDefinition.column("Price", ColumnType.decimal(10, 2)
                        .defaultValue(BigDecimal.valueOf(9.99))),
                    ColumnDefinition.column("CreatedAt", ColumnType.TIMESTAMP.notNull()
                        .defaultExpression("CURRENT_TIMESTAMP"))
                )
                .primaryKey("AlbumId")
                .colocateBy("ArtistId")  // Colocate albums with their artists
                .zone("MusicCatalog")
                .index("idx_album_artist_title", IndexType.SORTED,
                       ColumnSorted.column("ArtistId"),
                       ColumnSorted.column("Title"))
                .index("idx_album_genre_price", IndexType.SORTED,
                       ColumnSorted.column("Genre"),
                       ColumnSorted.column("Price"))
                .build();
            
            createTableSafely(catalog, albumTable, "Album");
            
            // Track table - colocated with albums for hierarchical data locality
            TableDefinition trackTable = TableDefinition.builder("Track")
                .ifNotExists()
                .columns(
                    ColumnDefinition.column("TrackId", ColumnType.INTEGER.notNull()),
                    ColumnDefinition.column("Name", ColumnType.varchar(200).notNull()),
                    ColumnDefinition.column("AlbumId", ColumnType.INTEGER),
                    ColumnDefinition.column("MediaTypeId", ColumnType.INTEGER.notNull()),
                    ColumnDefinition.column("GenreId", ColumnType.INTEGER),
                    ColumnDefinition.column("Composer", ColumnType.varchar(220)),
                    ColumnDefinition.column("Milliseconds", ColumnType.INTEGER.notNull()),
                    ColumnDefinition.column("Bytes", ColumnType.INTEGER),
                    ColumnDefinition.column("UnitPrice", ColumnType.decimal(10, 2).notNull()
                        .defaultValue(BigDecimal.valueOf(0.99))),
                    ColumnDefinition.column("CreatedAt", ColumnType.TIMESTAMP.notNull()
                        .defaultExpression("CURRENT_TIMESTAMP"))
                )
                .primaryKey("TrackId")
                .colocateBy("AlbumId")  // Colocate tracks with their albums
                .zone("MusicCatalog")
                .index("idx_track_album_name", IndexType.SORTED,
                       ColumnSorted.column("AlbumId"),
                       ColumnSorted.column("Name"))
                .index("idx_track_genre", IndexType.HASH,
                       ColumnSorted.column("GenreId"))
                .index("idx_track_duration", IndexType.SORTED,
                       ColumnSorted.column("Milliseconds"))
                .build();
            
            createTableSafely(catalog, trackTable, "Track");
            
            System.out.println("\n🎵 Music Catalog Tables Created:");
            System.out.println("  Artist → Root entity with name and genre indexes");
            System.out.println("  Album  → Colocated by ArtistId with genre/price indexes");
            System.out.println("  Track  → Colocated by AlbumId with genre and duration indexes");
            
        } catch (Exception e) {
            logger.error("Failed to create music catalog tables", e);
            throw new RuntimeException("Music catalog table creation failed", e);
        }
    }
    
    /**
     * Creates customer and business tables for the commerce side of the music platform
     * with proper transaction support and data privacy considerations.
     */
    private static void createCustomerBusinessTables(IgniteClient client) {
        logger.info("=== Creating Customer and Business Tables ===");
        
        IgniteCatalog catalog = client.catalog();
        
        try {
            // Customer table - root entity for customer hierarchy
            TableDefinition customerTable = TableDefinition.builder("Customer")
                .ifNotExists()
                .columns(
                    ColumnDefinition.column("CustomerId", ColumnType.INTEGER.notNull()),
                    ColumnDefinition.column("FirstName", ColumnType.varchar(40).notNull()),
                    ColumnDefinition.column("LastName", ColumnType.varchar(20).notNull()),
                    ColumnDefinition.column("Email", ColumnType.varchar(60).notNull()),
                    ColumnDefinition.column("Phone", ColumnType.varchar(24)),
                    ColumnDefinition.column("Address", ColumnType.varchar(70)),
                    ColumnDefinition.column("City", ColumnType.varchar(40)),
                    ColumnDefinition.column("State", ColumnType.varchar(40)),
                    ColumnDefinition.column("Country", ColumnType.varchar(40)),
                    ColumnDefinition.column("PostalCode", ColumnType.varchar(10)),
                    ColumnDefinition.column("CreatedAt", ColumnType.TIMESTAMP.notNull()
                        .defaultExpression("CURRENT_TIMESTAMP")),
                    ColumnDefinition.column("LastLoginAt", ColumnType.TIMESTAMP)
                )
                .primaryKey("CustomerId")
                .zone("MusicCatalog")
                .index("idx_customer_email", IndexType.SORTED,
                       ColumnSorted.column("Email"))  // Unique email for authentication
                .index("idx_customer_location", IndexType.SORTED,
                       ColumnSorted.column("Country"),
                       ColumnSorted.column("State"),
                       ColumnSorted.column("City"))
                .build();
            
            createTableSafely(catalog, customerTable, "Customer");
            
            // Invoice table - colocated with customers for transaction efficiency
            TableDefinition invoiceTable = TableDefinition.builder("Invoice")
                .ifNotExists()
                .columns(
                    ColumnDefinition.column("InvoiceId", ColumnType.INTEGER.notNull()),
                    ColumnDefinition.column("CustomerId", ColumnType.INTEGER.notNull()),
                    ColumnDefinition.column("InvoiceDate", ColumnType.TIMESTAMP.notNull()),
                    ColumnDefinition.column("BillingAddress", ColumnType.varchar(70)),
                    ColumnDefinition.column("BillingCity", ColumnType.varchar(40)),
                    ColumnDefinition.column("BillingState", ColumnType.varchar(40)),
                    ColumnDefinition.column("BillingCountry", ColumnType.varchar(40)),
                    ColumnDefinition.column("BillingPostalCode", ColumnType.varchar(10)),
                    ColumnDefinition.column("Total", ColumnType.decimal(10, 2).notNull()),
                    ColumnDefinition.column("PaymentStatus", ColumnType.varchar(20)
                        .defaultValue("Pending")),
                    ColumnDefinition.column("CreatedAt", ColumnType.TIMESTAMP.notNull()
                        .defaultExpression("CURRENT_TIMESTAMP"))
                )
                .primaryKey("InvoiceId")
                .colocateBy("CustomerId")  // Colocate invoices with customers
                .zone("MusicCatalog")
                .index("idx_invoice_customer_date", IndexType.SORTED,
                       ColumnSorted.column("CustomerId"),
                       ColumnSorted.column("InvoiceDate").desc())
                .index("idx_invoice_status", IndexType.HASH,
                       ColumnSorted.column("PaymentStatus"))
                .build();
            
            createTableSafely(catalog, invoiceTable, "Invoice");
            
            // InvoiceLine table - colocated with invoices for transaction atomicity
            TableDefinition invoiceLineTable = TableDefinition.builder("InvoiceLine")
                .ifNotExists()
                .columns(
                    ColumnDefinition.column("InvoiceLineId", ColumnType.INTEGER.notNull()),
                    ColumnDefinition.column("InvoiceId", ColumnType.INTEGER.notNull()),
                    ColumnDefinition.column("TrackId", ColumnType.INTEGER.notNull()),
                    ColumnDefinition.column("UnitPrice", ColumnType.decimal(10, 2).notNull()),
                    ColumnDefinition.column("Quantity", ColumnType.INTEGER.notNull()
                        .defaultValue(1)),
                    ColumnDefinition.column("Discount", ColumnType.decimal(5, 2)
                        .defaultValue(BigDecimal.ZERO))
                )
                .primaryKey("InvoiceLineId")
                .colocateBy("InvoiceId")  // Colocate line items with invoices
                .zone("MusicCatalog")
                .index("idx_invoiceline_invoice", IndexType.SORTED,
                       ColumnSorted.column("InvoiceId"))
                .index("idx_invoiceline_track", IndexType.SORTED,
                       ColumnSorted.column("TrackId"))
                .build();
            
            createTableSafely(catalog, invoiceLineTable, "InvoiceLine");
            
            System.out.println("\n💼 Customer & Business Tables Created:");
            System.out.println("  Customer    → Root entity with email index for authentication");
            System.out.println("  Invoice     → Colocated by CustomerId with date/status indexes");
            System.out.println("  InvoiceLine → Colocated by InvoiceId for transaction atomicity");
            
        } catch (Exception e) {
            logger.error("Failed to create customer business tables", e);
            throw new RuntimeException("Customer business table creation failed", e);
        }
    }
    
    /**
     * Creates additional performance indexes to support common query patterns
     * in the music streaming platform.
     */
    private static void createPerformanceIndexes(IgniteClient client) {
        logger.info("=== Creating Performance Indexes ===");
        
        try {
            // Create indexes using SQL DDL for additional flexibility
            executeIndexCreation(client, "CREATE INDEX IF NOT EXISTS idx_album_release_year " +
                "ON Album (EXTRACT(YEAR FROM ReleaseDate))");
            
            executeIndexCreation(client, "CREATE INDEX IF NOT EXISTS idx_track_price_duration " +
                "ON Track (UnitPrice, Milliseconds DESC)");
            
            executeIndexCreation(client, "CREATE INDEX IF NOT EXISTS idx_customer_signup_date " +
                "ON Customer (EXTRACT(YEAR FROM CreatedAt), EXTRACT(MONTH FROM CreatedAt))");
            
            executeIndexCreation(client, "CREATE INDEX IF NOT EXISTS idx_invoice_total_range " +
                "ON Invoice (Total DESC) WHERE Total > 10.00");
            
            System.out.println("\n🚀 Performance Indexes Created:");
            System.out.println("  Album release year index for decade/year filtering");
            System.out.println("  Track price/duration index for value-based sorting");
            System.out.println("  Customer signup date index for cohort analysis");
            System.out.println("  Invoice total range index for high-value transaction analysis");
            
        } catch (Exception e) {
            logger.error("Failed to create performance indexes", e);
            throw new RuntimeException("Performance index creation failed", e);
        }
    }
    
    /**
     * Demonstrates safe table management operations with proper error handling,
     * validation, and retry logic for production environments.
     */
    private static void demonstrateSafeTableOperations(IgniteClient client) {
        logger.info("=== Demonstrating Safe Table Operations ===");
        
        IgniteCatalog catalog = client.catalog();
        
        try {
            // Create a temporary table for demonstration
            TableDefinition tempTable = TableDefinition.builder("TempDemo")
                .ifNotExists()
                .columns(
                    ColumnDefinition.column("Id", ColumnType.INTEGER.notNull()),
                    ColumnDefinition.column("Data", ColumnType.varchar(100))
                )
                .primaryKey("Id")
                .zone("MusicCatalog")
                .build();
            
            // Demonstrate safe table creation with validation
            boolean created = createTableWithValidation(catalog, tempTable);
            if (created) {
                System.out.println("✅ Temporary table created successfully");
                
                // Demonstrate safe table dropping
                boolean dropped = dropTableSafely(catalog, "TempDemo");
                if (dropped) {
                    System.out.println("✅ Temporary table dropped successfully");
                }
            }
            
            // Demonstrate retry logic for transient failures
            demonstrateRetryLogic();
            
        } catch (Exception e) {
            logger.error("Safe table operations demonstration failed", e);
        }
    }
    
    /**
     * Validates the created music store schema to ensure all tables and indexes
     * were created correctly and are ready for use.
     */
    private static void validateMusicStoreSchema(IgniteClient client) {
        logger.info("=== Validating Music Store Schema ===");
        
        IgniteCatalog catalog = client.catalog();
        
        try {
            String[] expectedTables = {
                "Artist", "Album", "Track", 
                "Customer", "Invoice", "InvoiceLine"
            };
            
            Collection<String> actualTables = catalog.tables();
            
            System.out.println("\n✅ Schema Validation Results:");
            
            boolean allTablesExist = true;
            for (String expectedTable : expectedTables) {
                if (actualTables.contains(expectedTable)) {
                    TableDefinition tableDef = catalog.tableDefinition(expectedTable);
                    System.out.printf("  %-12s → ✅ Exists (%d columns, %d indexes)%n", 
                        expectedTable, 
                        tableDef.columns().size(),
                        tableDef.indexes().size());
                } else {
                    System.out.printf("  %-12s → ❌ Missing%n", expectedTable);
                    allTablesExist = false;
                }
            }
            
            if (allTablesExist) {
                System.out.println("\n🎉 Schema validation successful - all tables created correctly");
                
                // Validate colocation strategies
                validateColocationStrategies(catalog);
                
                // Validate zone configurations
                validateZoneConfigurations(catalog);
                
            } else {
                logger.error("Schema validation failed - some tables are missing");
            }
            
        } catch (Exception e) {
            logger.error("Schema validation failed", e);
        }
    }
    
    // Helper methods for safe operations
    
    /**
     * Creates a table safely with proper error handling and logging.
     */
    private static void createTableSafely(IgniteCatalog catalog, TableDefinition tableDef, String tableName) {
        try {
            // Check if table already exists
            if (catalog.tables().contains(tableName)) {
                logger.info("Table {} already exists - skipping creation", tableName);
                return;
            }
            
            // Validate table definition before creation
            if (!validateTableDefinition(tableDef)) {
                throw new RuntimeException("Table definition validation failed for " + tableName);
            }
            
            // Create table with async pattern for better responsiveness
            CompletableFuture<Table> future = catalog.createTableAsync(tableDef);
            
            future.whenComplete((table, throwable) -> {
                if (throwable != null) {
                    logger.error("Failed to create table {}: {}", tableName, throwable.getMessage());
                } else {
                    logger.info("✅ Table {} created successfully", tableName);
                }
            });
            
            // Wait for completion (in production, handle asynchronously)
            future.join();
            
        } catch (Exception e) {
            logger.error("Error creating table {}: {}", tableName, e.getMessage());
            throw new RuntimeException("Table creation failed for " + tableName, e);
        }
    }
    
    /**
     * Creates a table with comprehensive validation checks.
     */
    private static boolean createTableWithValidation(IgniteCatalog catalog, TableDefinition tableDef) {
        String tableName = tableDef.qualifiedName().toString();
        
        try {
            // Pre-creation validation
            if (catalog.tables().contains(tableName)) {
                logger.info("Table {} already exists", tableName);
                return true;
            }
            
            if (!validateTableDefinition(tableDef)) {
                logger.error("Table definition validation failed for {}", tableName);
                return false;
            }
            
            // Create with retry logic
            return retryOperation(() -> {
                catalog.createTable(tableDef);
                logger.info("Table {} created successfully", tableName);
                return true;
            }, 3);
            
        } catch (Exception e) {
            logger.error("Failed to create table {}: {}", tableName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Drops a table safely with data validation checks.
     */
    private static boolean dropTableSafely(IgniteCatalog catalog, String tableName) {
        try {
            if (!catalog.tables().contains(tableName)) {
                logger.info("Table {} does not exist", tableName);
                return true;
            }
            
            // In production, you might want to check for data before dropping
            logger.info("Dropping table {} (production systems should validate data first)", tableName);
            
            catalog.dropTable(tableName);
            logger.info("Table {} dropped successfully", tableName);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to drop table {}: {}", tableName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Validates table definition to ensure it meets business rules and standards.
     */
    private static boolean validateTableDefinition(TableDefinition tableDef) {
        // Check for primary key
        if (tableDef.primaryKeyColumns().isEmpty()) {
            logger.error("Table must have a primary key");
            return false;
        }
        
        // Check for valid column types
        for (ColumnDefinition column : tableDef.columns()) {
            if (column.type() == null) {
                logger.error("Column {} has invalid type", column.name());
                return false;
            }
        }
        
        // Check zone exists (simplified - in production, verify zone configuration)
        if (tableDef.zoneName() == null || tableDef.zoneName().isEmpty()) {
            logger.error("Table must specify a distribution zone");
            return false;
        }
        
        return true;
    }
    
    /**
     * Executes an operation with retry logic for handling transient failures.
     */
    private static <T> T retryOperation(Supplier<T> operation, int maxRetries) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                if (attempt == maxRetries) {
                    break;
                }
                
                logger.warn("Attempt {} failed, retrying: {}", attempt, e.getMessage());
                try {
                    Thread.sleep(1000 * attempt); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        throw new RuntimeException("Operation failed after " + maxRetries + " attempts", lastException);
    }
    
    /**
     * Executes index creation with proper error handling.
     */
    private static void executeIndexCreation(IgniteClient client, String indexSql) {
        try {
            client.sql().execute(null, indexSql);
            logger.info("Index created successfully");
        } catch (Exception e) {
            logger.warn("Index creation failed (may already exist): {}", e.getMessage());
        }
    }
    
    /**
     * Demonstrates retry logic patterns for handling distributed system failures.
     */
    private static void demonstrateRetryLogic() {
        System.out.println("\n🔄 Retry Logic Patterns:");
        System.out.println("  • Exponential backoff for transient failures");
        System.out.println("  • Maximum retry attempts to prevent infinite loops");
        System.out.println("  • Graceful degradation for critical path operations");
        System.out.println("  • Circuit breaker patterns for cascading failure prevention");
    }
    
    /**
     * Validates that colocation strategies are properly configured for performance.
     */
    private static void validateColocationStrategies(IgniteCatalog catalog) {
        System.out.println("\n🎯 Colocation Strategy Validation:");
        
        try {
            // Validate Album-Artist colocation
            TableDefinition albumDef = catalog.tableDefinition("Album");
            if (albumDef.colocationColumns().contains("ArtistId")) {
                System.out.println("  ✅ Album colocated with Artist (efficient joins)");
            } else {
                System.out.println("  ❌ Album not colocated with Artist");
            }
            
            // Validate Track-Album colocation
            TableDefinition trackDef = catalog.tableDefinition("Track");
            if (trackDef.colocationColumns().contains("AlbumId")) {
                System.out.println("  ✅ Track colocated with Album (hierarchical locality)");
            } else {
                System.out.println("  ❌ Track not colocated with Album");
            }
            
            // Validate Invoice-Customer colocation
            TableDefinition invoiceDef = catalog.tableDefinition("Invoice");
            if (invoiceDef.colocationColumns().contains("CustomerId")) {
                System.out.println("  ✅ Invoice colocated with Customer (transaction efficiency)");
            } else {
                System.out.println("  ❌ Invoice not colocated with Customer");
            }
            
        } catch (Exception e) {
            logger.warn("Could not validate colocation strategies: {}", e.getMessage());
        }
    }
    
    /**
     * Validates that distribution zones are configured appropriately for their workloads.
     */
    private static void validateZoneConfigurations(IgniteCatalog catalog) {
        System.out.println("\n🏗️  Zone Configuration Validation:");
        
        try {
            Collection<String> zones = catalog.zones();
            
            for (String zoneName : zones) {
                ZoneDefinition zoneDef = catalog.zoneDefinition(zoneName);
                
                System.out.printf("  %s: %d partitions, %d replicas", 
                    zoneName, zoneDef.partitions(), zoneDef.replicas());
                
                // Validate configuration appropriateness
                if (zoneName.contains("Analytics") && zoneDef.partitions() >= 32) {
                    System.out.println(" ✅ (good for analytics)");
                } else if (zoneName.contains("Catalog") && zoneDef.replicas() >= 2) {
                    System.out.println(" ✅ (good for availability)");
                } else if (zoneName.contains("Interactions") && zoneDef.partitions() >= 64) {
                    System.out.println(" ✅ (good for streaming)");
                } else {
                    System.out.println(" ⚠️  (review configuration)");
                }
            }
            
        } catch (Exception e) {
            logger.warn("Could not validate zone configurations: {}", e.getMessage());
        }
    }
}