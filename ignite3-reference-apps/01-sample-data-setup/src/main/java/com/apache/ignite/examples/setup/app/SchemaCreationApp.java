package com.apache.ignite.examples.setup.app;

import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apache.ignite.examples.setup.util.DataSetupUtils;
import com.apache.ignite.examples.setup.util.TableCreationUtils;
import com.apache.ignite.examples.setup.util.BulkDataLoader;
import com.apache.ignite.examples.setup.config.MusicStoreZoneConfiguration;

/**
 * Schema creation application demonstrating Ignite 3 schema management patterns.
 * 
 * This application showcases different approaches to schema creation and management:
 * - Annotation-driven schema creation (schema-as-code approach)
 * - SQL-based schema creation (traditional DDL approach)  
 * - Schema inspection and information display
 * - Safe schema cleanup and teardown
 * 
 * Educational Value:
 * - Compare schema-as-code vs traditional SQL DDL approaches
 * - Understand distribution zone configuration and management
 * - Learn proper schema lifecycle management
 * - See colocation and indexing strategies in action
 * 
 * This is ideal for learning Ignite 3 schema concepts before diving into data operations.
 * 
 * Usage Examples:
 *   mvn exec:java                                    # Create schema using annotations
 *   mvn exec:java -Dexec.args="127.0.0.1:10800 sql" # Create schema using SQL scripts
 *   mvn exec:java -Dexec.args="127.0.0.1:10800 info" # Display current schema info
 *   mvn exec:java -Dexec.args="127.0.0.1:10800 drop" # Clean up schema
 */
public class SchemaCreationApp {
    
    private static final Logger logger = LoggerFactory.getLogger(SchemaCreationApp.class);
    
    /**
     * Main entry point for schema management operations.
     * 
     * Supports multiple modes to demonstrate different schema approaches:
     * - annotation: Use schema-as-code with annotated POJOs (recommended)
     * - sql: Use traditional SQL DDL scripts
     * - drop: Clean up existing schema
     * - info: Display current schema information
     * 
     * @param args [0] cluster address (optional, default: 127.0.0.1:10800)
     *             [1] schema mode (optional, default: annotation)
     */
    public static void main(String[] args) {
        // Parse command line arguments with sensible defaults
        String igniteAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        String schemaMode = args.length > 1 ? args[1] : "annotation";
        
        logger.info("Starting Music Store Schema Creation");
        logger.info("Connecting to Ignite cluster at: {}", igniteAddress);
        logger.info("Schema creation mode: {}", schemaMode);
        
        // Use try-with-resources for proper client cleanup
        try (IgniteClient client = DataSetupUtils.connectToCluster(igniteAddress)) {
            
            // Dispatch to appropriate schema operation based on mode
            switch (schemaMode.toLowerCase()) {
                case "annotation":
                    createSchemaFromAnnotations(client);
                    break;
                case "sql":
                    createSchemaFromSql(client);
                    break;
                case "drop":
                    dropSchema(client);
                    break;
                case "info":
                    displaySchemaInfo(client);
                    break;
                default:
                    logger.error("Unknown schema mode: {}. Use: annotation, sql, drop, or info", schemaMode);
                    printUsage();
                    System.exit(1);
            }
            
            logger.info("Schema operation completed successfully");
            
        } catch (Exception e) {
            logger.error("Schema operation failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
    
    private static void createSchemaFromAnnotations(IgniteClient client) {
        logger.info("Creating schema from annotation-based POJOs...");
        
        try {
            logger.info("Step 1: Creating distribution zones...");
            MusicStoreZoneConfiguration.createDistributionZones(client);
            
            logger.info("Step 2: Creating reference data tables...");
            TableCreationUtils.createReferenceDataTables(client);
            
            logger.info("Step 3: Creating music entity tables...");
            TableCreationUtils.createMusicEntityTables(client);
            
            logger.info("Step 4: Creating business entity tables...");
            TableCreationUtils.createBusinessEntityTables(client);
            
            logger.info("Step 5: Creating playlist tables...");
            TableCreationUtils.createPlaylistTables(client);
            
            logger.info("Step 6: Verifying schema integrity...");
            TableCreationUtils.verifySchemaIntegrity(client);
            
            displaySchemaInfo(client);
            
            logger.info("Annotation-based schema creation completed successfully");
            
        } catch (Exception e) {
            logger.error("Annotation-based schema creation failed: {}", e.getMessage());
            throw e;
        }
    }
    
    private static void createSchemaFromSql(IgniteClient client) {
        logger.info("Creating schema from SQL scripts...");
        
        try {
            logger.info("Executing SQL schema script...");
            BulkDataLoader.loadSchemaFromScript(client);
            
            logger.info("Verifying schema integrity...");
            TableCreationUtils.verifySchemaIntegrity(client);
            
            displaySchemaInfo(client);
            
            logger.info("SQL-based schema creation completed successfully");
            
        } catch (Exception e) {
            logger.error("SQL-based schema creation failed: {}", e.getMessage());
            throw e;
        }
    }
    
    private static void dropSchema(IgniteClient client) {
        logger.info("Dropping music store schema...");
        
        try {
            logger.info("Step 1: Dropping all tables...");
            TableCreationUtils.dropAllTables(client);
            
            logger.info("Step 2: Dropping distribution zones...");
            MusicStoreZoneConfiguration.dropDistributionZones(client);
            
            logger.info("Step 3: Verifying schema cleanup...");
            DataSetupUtils.displayTables(client);
            MusicStoreZoneConfiguration.displayZoneInfo(client);
            
            logger.info("Schema drop completed successfully");
            
        } catch (Exception e) {
            logger.error("Schema drop failed: {}", e.getMessage());
            throw e;
        }
    }
    
    private static void displaySchemaInfo(IgniteClient client) {
        logger.info("\n=== Music Store Schema Information ===");
        
        try {
            logger.info("Distribution Zones:");
            MusicStoreZoneConfiguration.displayZoneInfo(client);
            
            logger.info("Tables:");
            TableCreationUtils.displayAllTablesInfo(client);
            
        } catch (Exception e) {
            logger.error("Error displaying schema information: {}", e.getMessage());
        }
    }
    
    private static void printUsage() {
        logger.info("Usage: SchemaCreationApp [ignite-address] [schema-mode]");
        logger.info("  ignite-address: Ignite cluster address (default: 127.0.0.1:10800)");
        logger.info("  schema-mode: One of the following:");
        logger.info("    annotation - Create schema from annotation-based POJOs (default)");
        logger.info("    sql        - Create schema from SQL scripts");
        logger.info("    drop       - Drop entire music store schema");
        logger.info("    info       - Display current schema information");
        logger.info("");
        logger.info("Examples:");
        logger.info("  java -jar schema-creation-app.jar");
        logger.info("  java -jar schema-creation-app.jar 127.0.0.1:10800 annotation");
        logger.info("  java -jar schema-creation-app.jar 127.0.0.1:10800 drop");
        logger.info("");
        logger.info("Schema Components:");
        logger.info("  Distribution Zones:");
        logger.info("    - MusicStore (2 replicas) - Primary business data");
        logger.info("    - MusicStoreReplicated (3 replicas) - Reference/lookup data");
        logger.info("  Tables:");
        logger.info("    - Music Entities: Artist, Album, Track");
        logger.info("    - Reference Data: Genre, MediaType");
        logger.info("    - Business Entities: Customer, Employee, Invoice, InvoiceLine");
        logger.info("    - Playlist Entities: Playlist, PlaylistTrack");
    }
}