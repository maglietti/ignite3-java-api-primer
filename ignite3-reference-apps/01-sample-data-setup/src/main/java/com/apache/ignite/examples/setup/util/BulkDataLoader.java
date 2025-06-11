package com.apache.ignite.examples.setup.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BulkDataLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(BulkDataLoader.class);
    
    public static void executeSqlScript(IgniteClient client, String scriptPath) {
        logger.info("Executing SQL script: {}", scriptPath);
        
        try (InputStream inputStream = BulkDataLoader.class.getResourceAsStream(scriptPath)) {
            if (inputStream == null) {
                throw new RuntimeException("SQL script not found: " + scriptPath);
            }
            
            List<String> statements = parseSqlScript(inputStream);
            executeStatements(client, statements);
            
            logger.info("Successfully executed SQL script: {}", scriptPath);
            
        } catch (IOException e) {
            logger.error("Error reading SQL script {}: {}", scriptPath, e.getMessage());
            throw new RuntimeException("Failed to execute SQL script: " + scriptPath, e);
        }
    }
    
    private static List<String> parseSqlScript(InputStream inputStream) throws IOException {
        List<String> statements = new ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }
                
                currentStatement.append(line).append(" ");
                
                if (line.endsWith(";")) {
                    String statement = currentStatement.toString().trim();
                    if (!statement.isEmpty()) {
                        statement = statement.substring(0, statement.length() - 1);
                        statements.add(statement);
                    }
                    currentStatement = new StringBuilder();
                }
            }
        }
        
        if (currentStatement.length() > 0) {
            String statement = currentStatement.toString().trim();
            if (!statement.isEmpty()) {
                statements.add(statement);
            }
        }
        
        logger.debug("Parsed {} SQL statements from script", statements.size());
        return statements;
    }
    
    private static void executeStatements(IgniteClient client, List<String> statements) {
        int successCount = 0;
        int errorCount = 0;
        
        for (String statement : statements) {
            try {
                client.sql().execute(null, statement);
                successCount++;
                logger.debug("Executed statement: {}", 
                    statement.length() > 100 ? statement.substring(0, 100) + "..." : statement);
                
            } catch (Exception e) {
                errorCount++;
                logger.warn("Failed to execute statement: {} - Error: {}", 
                    statement.length() > 100 ? statement.substring(0, 100) + "..." : statement, 
                    e.getMessage());
                
                if (isCreateStatement(statement) && e.getMessage().contains("already exists")) {
                    logger.debug("Ignoring 'already exists' error for CREATE statement");
                } else {
                    logger.error("Unexpected error executing statement: {}", e.getMessage());
                }
            }
        }
        
        logger.info("SQL execution completed - Success: {}, Errors: {}", successCount, errorCount);
        
        if (errorCount > 0 && successCount == 0) {
            throw new RuntimeException("All SQL statements failed to execute");
        }
    }
    
    private static boolean isCreateStatement(String statement) {
        String upperStatement = statement.toUpperCase().trim();
        return upperStatement.startsWith("CREATE TABLE") || 
               upperStatement.startsWith("CREATE ZONE") ||
               upperStatement.startsWith("CREATE INDEX");
    }
    
    public static void loadSchemaFromScript(IgniteClient client) {
        executeSqlScript(client, "/music-store-schema.sql");
    }
    
    public static void loadSampleDataFromScript(IgniteClient client) {
        executeSqlScript(client, "/sample-data.sql");
    }
    
    public static void loadAdditionalDataFromScript(IgniteClient client) {
        executeSqlScript(client, "/additional-albums.sql");
    }
    
    public static void loadCompleteDataFromScript(IgniteClient client) {
        executeSqlScript(client, "/music-store-complete.sql");
    }

    public static void executeCustomStatement(IgniteClient client, String sql) {
        try {
            logger.info("Executing custom SQL: {}", 
                sql.length() > 100 ? sql.substring(0, 100) + "..." : sql);
            
            var resultSet = client.sql().execute(null, sql);
            
            if (resultSet.hasNext()) {
                logger.info("SQL execution returned results");
                resultSet.forEachRemaining(row -> {
                    logger.debug("Result row: {}", row.toString());
                });
            } else {
                logger.info("SQL execution completed with no results");
            }
            
        } catch (Exception e) {
            logger.error("Failed to execute custom SQL: {}", e.getMessage());
            throw new RuntimeException("Failed to execute SQL", e);
        }
    }
    
    public static void executeBatch(IgniteClient client, List<String> statements) {
        logger.info("Executing batch of {} statements", statements.size());
        
        client.transactions().runInTransaction(tx -> {
            for (String statement : statements) {
                try {
                    client.sql().execute(tx, statement);
                } catch (Exception e) {
                    logger.error("Error in batch execution: {}", e.getMessage());
                    throw new RuntimeException("Batch execution failed", e);
                }
            }
        });
        
        logger.info("Batch execution completed successfully");
    }
    
    public static void validateDataLoad(IgniteClient client) {
        logger.info("Validating data load...");
        
        String[] tables = {"Artist", "Album", "Track", "Genre", "MediaType", 
                          "Customer", "Employee", "Invoice", "InvoiceLine", 
                          "Playlist", "PlaylistTrack"};
        
        boolean allTablesHaveData = true;
        
        for (String table : tables) {
            long count = DataSetupUtils.getTableRowCount(client, table);
            if (count > 0) {
                logger.info("Table {} has {} rows", table, count);
            } else if (count == 0) {
                logger.warn("Table {} is empty", table);
            } else {
                logger.error("Error getting row count for table {}", table);
                allTablesHaveData = false;
            }
        }
        
        if (allTablesHaveData) {
            logger.info("Data load validation completed successfully");
        } else {
            throw new RuntimeException("Data load validation failed");
        }
    }
}