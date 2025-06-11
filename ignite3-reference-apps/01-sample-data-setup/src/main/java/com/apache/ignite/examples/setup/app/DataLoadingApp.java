package com.apache.ignite.examples.setup.app;

import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apache.ignite.examples.setup.util.DataSetupUtils;
import com.apache.ignite.examples.setup.util.DataLoadingUtils;
import com.apache.ignite.examples.setup.util.BulkDataLoader;

public class DataLoadingApp {
    
    private static final Logger logger = LoggerFactory.getLogger(DataLoadingApp.class);
    
    public static void main(String[] args) {
        String igniteAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        String loadMode = args.length > 1 ? args[1] : "programmatic";
        
        logger.info("Starting Music Store Data Loading");
        logger.info("Connecting to Ignite cluster at: {}", igniteAddress);
        logger.info("Loading mode: {}", loadMode);
        
        try (IgniteClient client = DataSetupUtils.connectToCluster(igniteAddress)) {
            
            verifyTablesExist(client);
            
            switch (loadMode.toLowerCase()) {
                case "programmatic":
                    loadDataProgrammatically(client);
                    break;
                case "sql":
                    loadDataFromSqlScripts(client);
                    break;
                case "clear":
                    clearAllData(client);
                    break;
                case "extended":
                    loadExtendedData(client);
                    break;
                case "complete":
                    loadCompleteData(client);
                    break;
                default:
                    logger.error("Unknown load mode: {}. Use: programmatic, sql, clear, or extended", loadMode);
                    printUsage();
                    System.exit(1);
            }
            
            displayDataSummary(client);
            
        } catch (Exception e) {
            logger.error("Data loading failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
    
    private static void verifyTablesExist(IgniteClient client) {
        logger.info("Verifying that all required tables exist...");
        
        String[] requiredTables = {"Artist", "Album", "Track", "Genre", "MediaType", 
                                  "Customer", "Employee", "Invoice", "InvoiceLine", 
                                  "Playlist", "PlaylistTrack"};
        
        boolean allTablesExist = true;
        for (String table : requiredTables) {
            if (!DataSetupUtils.tableExists(client, table)) {
                logger.error("Required table does not exist: {}", table);
                allTablesExist = false;
            }
        }
        
        if (!allTablesExist) {
            logger.error("Some required tables are missing. Run ProjectInitializationApp first to create the schema.");
            System.exit(1);
        }
        
        logger.info("All required tables exist");
    }
    
    private static void loadDataProgrammatically(IgniteClient client) {
        logger.info("Loading data programmatically...");
        
        try {
            DataLoadingUtils.clearAllData(client);
            DataLoadingUtils.loadSampleData(client);
            logger.info("Programmatic data loading completed successfully");
        } catch (Exception e) {
            logger.error("Programmatic data loading failed: {}", e.getMessage());
            throw e;
        }
    }
    
    private static void loadDataFromSqlScripts(IgniteClient client) {
        logger.info("Loading data from SQL scripts...");
        
        try {
            DataLoadingUtils.clearAllData(client);
            BulkDataLoader.loadSampleDataFromScript(client);
            BulkDataLoader.validateDataLoad(client);
            logger.info("SQL script data loading completed successfully");
        } catch (Exception e) {
            logger.error("SQL script data loading failed: {}", e.getMessage());
            throw e;
        }
    }
    
    private static void clearAllData(IgniteClient client) {
        logger.info("Clearing all data from music store tables...");
        
        try {
            DataLoadingUtils.clearAllData(client);
            logger.info("All data cleared successfully");
        } catch (Exception e) {
            logger.error("Data clearing failed: {}", e.getMessage());
            throw e;
        }
    }
    
    private static void loadExtendedData(IgniteClient client) {
        logger.info("Loading extended sample data...");
        
        try {
            DataLoadingUtils.loadExtendedSampleData(client);
            BulkDataLoader.loadAdditionalDataFromScript(client);
            logger.info("Extended data loading completed successfully");
        } catch (Exception e) {
            logger.error("Extended data loading failed: {}", e.getMessage());
            throw e;
        }
    }
    
    private static void loadCompleteData(IgniteClient client) {
        logger.info("Loading compete Chinook data set...");
        
        try {
            DataLoadingUtils.clearAllData(client);
            BulkDataLoader.loadCompleteDataFromScript(client);
            BulkDataLoader.validateDataLoad(client);
            logger.info("Complete data loading completed successfully");
        } catch (Exception e) {
            logger.error("Complete data loading failed: {}", e.getMessage());
            throw e;
        }
    }

    private static void displayDataSummary(IgniteClient client) {
        logger.info("\n=== Data Loading Summary ===");
        
        String[] tables = {"Artist", "Album", "Track", "Genre", "MediaType", 
                          "Customer", "Employee", "Invoice", "InvoiceLine", 
                          "Playlist", "PlaylistTrack"};
        
        long totalRows = 0;
        for (String table : tables) {
            long count = DataSetupUtils.getTableRowCount(client, table);
            logger.info("Table {}: {} rows", table, count);
            totalRows += count;
        }
        
        logger.info("Total rows across all tables: {}", totalRows);
        logger.info("Data loading summary completed");
    }
    
    private static void printUsage() {
        logger.info("Usage: DataLoadingApp [ignite-address] [load-mode]");
        logger.info("  ignite-address: Ignite cluster address (default: 127.0.0.1:10800)");
        logger.info("  load-mode: One of the following:");
        logger.info("    programmatic - Load data using Java POJOs (default)");
        logger.info("    sql          - Load data from SQL scripts");
        logger.info("    clear        - Clear all data from tables");
        logger.info("    extended     - Load additional extended sample data");
        logger.info("    complete     - Load the complete Chinook Data Set");
        logger.info("");
        logger.info("Examples:");
        logger.info("  java -jar data-loading-app.jar");
        logger.info("  java -jar data-loading-app.jar 127.0.0.1:10800 programmatic");
        logger.info("  java -jar data-loading-app.jar 127.0.0.1:10800 clear");
    }
}