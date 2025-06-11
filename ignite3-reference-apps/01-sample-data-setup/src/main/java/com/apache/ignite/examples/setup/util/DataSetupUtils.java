package com.apache.ignite.examples.setup.util;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apache.ignite.examples.setup.config.IgniteConfiguration;

public class DataSetupUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSetupUtils.class);
    
    public static IgniteClient connectToCluster() {
        return IgniteConfiguration.createClient();
    }
    
    public static IgniteClient connectToCluster(String address) {
        return IgniteConfiguration.createClient(address);
    }
    
    public static IgniteClient connectToCluster(String... addresses) {
        return IgniteConfiguration.createClient(addresses);
    }
    
    public static void closeConnection(IgniteClient client) {
        IgniteConfiguration.closeClient(client);
    }
    
    public static boolean testConnection(String address) {
        return IgniteConfiguration.testConnection(address);
    }
    
    public static Table getTable(IgniteClient client, String tableName) {
        try {
            Table table = client.tables().table(tableName);
            if (table == null) {
                logger.warn("Table '{}' not found", tableName);
                return null;
            }
            return table;
        } catch (Exception e) {
            logger.error("Error accessing table '{}': {}", tableName, e.getMessage());
            return null;
        }
    }
    
    public static boolean tableExists(IgniteClient client, String tableName) {
        try {
            Table table = client.tables().table(tableName);
            return table != null;
        } catch (Exception e) {
            logger.debug("Table '{}' does not exist: {}", tableName, e.getMessage());
            return false;
        }
    }
    

    public static void displayTables(IgniteClient client) {
        try {
            logger.info("Existing tables in the cluster:");
            List<Table> tables = client.tables().tables();
            
            if (tables.isEmpty()) {
                logger.info("  No user tables found");
            } else {
                for (Table table : tables) {
                    logger.info("  Table: {}", table.name());
                }
            }
        } catch (Exception e) {
            logger.error("Error displaying table information: {}", e.getMessage());
        }
    }
    
    public static void displayTableInfo(IgniteClient client, String tableName) {
        try {
            logger.info("Table information for: {}", tableName);
            var resultSet = client.sql().execute(null,
                "SELECT column_name, data_type, is_nullable " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE table_name = ? ORDER BY ordinal_position", tableName);
            
            if (!resultSet.hasNext()) {
                logger.info("  No columns found or table does not exist");
            } else {
                resultSet.forEachRemaining(row -> {
                    String columnName = row.stringValue("column_name");
                    String dataType = row.stringValue("data_type");
                    String nullable = row.stringValue("is_nullable");
                    logger.info("  Column: {} - Type: {} - Nullable: {}", columnName, dataType, nullable);
                });
            }
        } catch (Exception e) {
            logger.error("Error displaying table information for '{}': {}", tableName, e.getMessage());
        }
    }
    
    public static long getTableRowCount(IgniteClient client, String tableName) {
        try {
            var resultSet = client.sql().execute(null, 
                "SELECT COUNT(*) as row_count FROM " + tableName);
            
            if (resultSet.hasNext()) {
                return resultSet.next().longValue("row_count");
            }
            return 0;
        } catch (Exception e) {
            logger.error("Error getting row count for table '{}': {}", tableName, e.getMessage());
            return -1;
        }
    }
    
    public static CompletableFuture<Void> performAsyncOperation(IgniteClient client, String operation) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Starting async operation: {}", operation);
                Thread.sleep(1000);
                logger.info("Completed async operation: {}", operation);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Async operation interrupted: {}", operation);
            }
        });
    }
    
    public static void waitForOperation(CompletableFuture<Void> future, String operationName) {
        try {
            future.get();
            logger.info("Operation completed successfully: {}", operationName);
        } catch (Exception e) {
            logger.error("Operation failed: {}: {}", operationName, e.getMessage());
        }
    }
}