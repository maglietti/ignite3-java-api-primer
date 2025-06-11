package com.apache.ignite.examples.setup.app;

import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apache.ignite.examples.setup.util.DataSetupUtils;
import com.apache.ignite.examples.setup.util.TableCreationUtils;
import com.apache.ignite.examples.setup.util.DataLoadingUtils;
import com.apache.ignite.examples.setup.util.ReportingUtils;

/**
 * Complete initialization application for the Apache Ignite 3 Music Store sample dataset.
 * 
 * This is the recommended starting point for new users. It performs a complete setup:
 * 1. Tests connection to the Ignite 3 cluster
 * 2. Creates the complete music store schema (11 tables, 2 distribution zones)
 * 3. Loads sample data using transactional operations
 * 4. Verifies the setup with integrity checks
 * 5. Generates sample analytical reports
 * 
 * Usage:
 *   mvn exec:java                                    # Connect to 127.0.0.1:10800
 *   mvn exec:java -Dexec.args="192.168.1.100:10800" # Custom cluster address
 * 
 * This creates a complete learning environment for exploring Ignite 3 features.
 */
public class ProjectInitializationApp {
    
    private static final Logger logger = LoggerFactory.getLogger(ProjectInitializationApp.class);
    
    /**
     * Main entry point for complete music store sample data setup.
     * 
     * @param args Optional cluster address (default: 127.0.0.1:10800)
     */
    public static void main(String[] args) {
        // Allow custom cluster address via command line argument
        String igniteAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        logger.info("Starting Apache Ignite 3 Music Store Sample Data Setup");
        logger.info("Connecting to Ignite cluster at: {}", igniteAddress);
        
        // Use try-with-resources to ensure proper client cleanup
        // This pattern is essential for production Ignite 3 applications
        try (IgniteClient client = DataSetupUtils.connectToCluster(igniteAddress)) {
            
            // Phase 1: Verify we can connect and communicate with the cluster
            logger.info("=== Phase 1: Connection Test ===");
            testConnection(client);
            
            // Phase 2: Create the complete schema using annotation-based POJOs
            // This demonstrates Ignite 3's schema-as-code capabilities
            logger.info("=== Phase 2: Schema Creation ===");
            createSchema(client);
            
            // Phase 3: Load sample data using both programmatic and transactional approaches
            // This shows proper data loading patterns for Ignite 3
            logger.info("=== Phase 3: Data Loading ===");
            loadSampleData(client);
            
            // Phase 4: Verify everything was created correctly
            // Important for confirming setup success
            logger.info("=== Phase 4: Verification ===");
            verifySetup(client);
            
            // Phase 5: Generate reports to demonstrate the data and query capabilities
            // This gives immediate feedback on what was created
            logger.info("=== Phase 5: Sample Reports ===");
            generateSampleReports(client);
            
            // Success! The environment is ready for learning and development
            logger.info("=== Setup Complete ===");
            logger.info("Music store sample data setup completed successfully!");
            logger.info("You can now use the following tables in your applications:");
            logger.info("  - Artist, Album, Track (music entities)");
            logger.info("  - Genre, MediaType (reference data)");
            logger.info("  - Customer, Employee, Invoice, InvoiceLine (business entities)");
            logger.info("  - Playlist, PlaylistTrack (playlist entities)");
            
        } catch (Exception e) {
            logger.error("Setup failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
    
    private static void testConnection(IgniteClient client) {
        try {
            logger.info("Testing connection to Ignite cluster...");
            DataSetupUtils.displayClusterInfo(client);
            logger.info("Connection test successful");
        } catch (Exception e) {
            logger.error("Connection test failed: {}", e.getMessage());
            throw new RuntimeException("Unable to connect to Ignite cluster", e);
        }
    }
    
    private static void createSchema(IgniteClient client) {
        try {
            logger.info("Creating music store schema...");
            TableCreationUtils.createAllTables(client);
            TableCreationUtils.verifySchemaIntegrity(client);
            logger.info("Schema creation completed successfully");
        } catch (Exception e) {
            logger.error("Schema creation failed: {}", e.getMessage());
            throw new RuntimeException("Failed to create music store schema", e);
        }
    }
    
    private static void loadSampleData(IgniteClient client) {
        try {
            logger.info("Loading sample music store data...");
            DataLoadingUtils.loadSampleData(client);
            DataLoadingUtils.loadExtendedSampleData(client);
            logger.info("Sample data loading completed successfully");
        } catch (Exception e) {
            logger.error("Data loading failed: {}", e.getMessage());
            throw new RuntimeException("Failed to load sample data", e);
        }
    }
    
    private static void verifySetup(IgniteClient client) {
        try {
            logger.info("Verifying setup...");
            TableCreationUtils.displayAllTablesInfo(client);
            
            String[] tables = {"Artist", "Album", "Track", "Genre", "MediaType", 
                              "Customer", "Employee", "Invoice", "InvoiceLine", 
                              "Playlist", "PlaylistTrack"};
            
            logger.info("Row counts by table:");
            for (String table : tables) {
                long count = DataSetupUtils.getTableRowCount(client, table);
                logger.info("  {}: {} rows", table, count);
            }
            
            logger.info("Setup verification completed successfully");
        } catch (Exception e) {
            logger.error("Setup verification failed: {}", e.getMessage());
            throw new RuntimeException("Setup verification failed", e);
        }
    }
    
    private static void generateSampleReports(IgniteClient client) {
        try {
            logger.info("Generating sample reports...");
            ReportingUtils.generateSampleReports(client);
            ReportingUtils.displayDetailedTrackInfo(client, "AC/DC");
            ReportingUtils.runComplexQueries(client);
            logger.info("Sample reports generated successfully");
        } catch (Exception e) {
            logger.warn("Sample report generation failed: {}", e.getMessage());
        }
    }
}