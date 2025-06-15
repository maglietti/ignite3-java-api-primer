package com.apache.ignite.examples.setup.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SQL script execution utilities for Apache Ignite 3 music store dataset.
 * 
 * Provides functionality to parse and execute large SQL scripts containing
 * schema definitions and data loading statements with batch processing
 * capabilities for optimal performance.
 */
public class SqlScriptLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(SqlScriptLoader.class);
    
    private static final List<String> IGNORED_PREFIXES = Arrays.asList(
        "SET", "BEGIN TRANSACTION", "COMMIT", "--", "/*"
    );
    
    private static final int MAX_BATCH_SIZE = 1000;
    
    /**
     * Loads data from SQL script file in resources.
     * 
     * @param client Connected Ignite client
     * @param scriptPath Path to SQL script in resources
     * @return Number of successfully executed statements
     */
    public static int loadFromScript(IgniteClient client, String scriptPath) {
        logger.info("Loading data from SQL script: {}", scriptPath);
        
        try {
            InputStream scriptStream = SqlScriptLoader.class.getResourceAsStream("/" + scriptPath);
            if (scriptStream == null) {
                throw new RuntimeException("SQL script not found: " + scriptPath);
            }
            
            List<String> statements;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(scriptStream, StandardCharsets.UTF_8))) {
                statements = parseSqlStatementsFromReader(reader);
            }
            
            logger.info("Parsed {} SQL statements from script", statements.size());
            
            return executeSqlStatements(client, statements);
            
        } catch (Exception e) {
            logger.error("Failed to load SQL script {}: {}", scriptPath, e.getMessage());
            throw new RuntimeException("SQL script loading failed", e);
        }
    }
    
    /**
     * Verifies loaded data by checking table row counts.
     * 
     * @param client Connected Ignite client
     */
    public static void verifyDataLoad(IgniteClient client) {
        logger.info("Verifying data load...");
        
        String[] tables = {"Artist", "Album", "Track", "Genre", "MediaType", 
                          "Customer", "Employee", "Invoice", "InvoiceLine", 
                          "Playlist", "PlaylistTrack"};
        
        for (String table : tables) {
            try {
                var result = client.sql().execute(null, "SELECT COUNT(*) as cnt FROM " + table);
                if (result.hasNext()) {
                    long count = result.next().longValue("cnt");
                    logger.info("{}: {} rows", table, count);
                }
            } catch (Exception e) {
                logger.debug("{}: Table not available", table);
            }
        }
    }
    
    private static List<String> parseSqlStatementsFromReader(BufferedReader reader) throws IOException {
        List<String> statements = new ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();
        String line;
        boolean insideMultilineComment = false;
        boolean insideQuote = false;
        
        while ((line = reader.readLine()) != null) {
            if (line.contains("/*") && !line.contains("*/")) {
                insideMultilineComment = true;
                continue;
            }
            
            if (insideMultilineComment) {
                if (line.contains("*/")) {
                    insideMultilineComment = false;
                }
                continue;
            }
            
            line = line.trim();
            if (line.isEmpty() || line.startsWith("--")) {
                continue;
            }
            
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                
                if (c == '\'' && (i == 0 || line.charAt(i-1) != '\\')) {
                    insideQuote = !insideQuote;
                }
                
                if (c == ';' && !insideQuote) {
                    String statement = currentStatement.toString().trim();
                    if (!statement.isEmpty() && !shouldIgnoreStatement(statement)) {
                        statements.add(statement);
                    }
                    currentStatement = new StringBuilder();
                } else {
                    currentStatement.append(c);
                }
            }
            
            if (currentStatement.length() > 0) {
                currentStatement.append(' ');
            }
        }
        
        String finalStatement = currentStatement.toString().trim();
        if (!finalStatement.isEmpty() && !shouldIgnoreStatement(finalStatement)) {
            statements.add(finalStatement);
        }
        
        return statements;
    }
    
    private static boolean shouldIgnoreStatement(String statement) {
        String upperStatement = statement.toUpperCase();
        
        for (String prefix : IGNORED_PREFIXES) {
            if (upperStatement.startsWith(prefix)) {
                return true;
            }
        }
        
        return false;
    }
    
    private static int executeSqlStatements(IgniteClient client, List<String> statements) {
        int successCount = 0;
        int currentStatement = 0;
        int totalStatements = statements.size();
        
        logger.info("Processing schema statements");
        for (String statement : statements) {
            currentStatement++;
            
            if (!isSchemaStatement(statement)) {
                continue;
            }
            
            try {
                String stmtType = getSchemaStatementType(statement);
                logger.debug("[{}/{}] Executing: {}", currentStatement, totalStatements, stmtType);
                
                client.sql().execute(null, statement);
                successCount++;
            } catch (Exception e) {
                if (isCreateZoneStatement(statement) || isDropStatement(statement)) {
                    logger.debug("Schema operation note: {}", e.getMessage());
                } else {
                    logger.warn("Error executing schema statement: {}", e.getMessage());
                }
            }
        }
        
        logger.info("Loading data statements");
        currentStatement = 0;
        
        for (String statement : statements) {
            currentStatement++;
            
            if (isSchemaStatement(statement)) {
                continue;
            }
            
            try {
                String statementType = getStatementType(statement);
                String targetTable = getTargetTable(statement);
                
                if (statementType.equals("INSERT")) {
                    int approxRows = countInsertRows(statement);
                    logger.debug("[{}/{}] {} for table {} with {} rows", 
                               currentStatement, totalStatements, statementType, targetTable, approxRows);
                    
                    if (approxRows > MAX_BATCH_SIZE) {
                        logger.debug("Splitting large INSERT into smaller batches");
                        List<String> batches = splitLargeInsert(statement, MAX_BATCH_SIZE);
                        
                        for (String batch : batches) {
                            client.sql().execute(null, batch);
                        }
                        
                        successCount++;
                        continue;
                    }
                }
                
                client.sql().execute(null, statement);
                successCount++;
            } catch (Exception e) {
                logger.warn("Error executing data statement: {}", e.getMessage());
            }
        }
        
        return successCount;
    }
    
    private static boolean isSchemaStatement(String statement) {
        String normalizedStatement = statement.trim().toUpperCase();
        
        return normalizedStatement.startsWith("CREATE ZONE") || 
            normalizedStatement.startsWith("CREATE TABLE") || 
            normalizedStatement.startsWith("CREATE INDEX") || 
            normalizedStatement.startsWith("DROP TABLE") ||
            normalizedStatement.startsWith("DROP ZONE") ||
            normalizedStatement.startsWith("DROP INDEX");
    }
    
    private static boolean isCreateZoneStatement(String statement) {
        return statement.trim().toUpperCase().startsWith("CREATE ZONE");
    }
    
    private static boolean isDropStatement(String statement) {
        String normalizedStatement = statement.trim().toUpperCase();
        return normalizedStatement.startsWith("DROP TABLE") || 
            normalizedStatement.startsWith("DROP ZONE") ||
            normalizedStatement.startsWith("DROP INDEX");
    }
    
    private static String getSchemaStatementType(String statement) {
        String upperStatement = statement.trim().toUpperCase();
        if (upperStatement.startsWith("CREATE ZONE")) return "CREATE ZONE";
        if (upperStatement.startsWith("CREATE TABLE")) return "CREATE TABLE";
        if (upperStatement.startsWith("CREATE INDEX")) return "CREATE INDEX";
        if (upperStatement.startsWith("DROP")) return "DROP";
        return "SCHEMA";
    }
    
    private static String getStatementType(String statement) {
        String upperStatement = statement.trim().toUpperCase();
        if (upperStatement.startsWith("INSERT")) return "INSERT";
        if (upperStatement.startsWith("UPDATE")) return "UPDATE";
        if (upperStatement.startsWith("DELETE")) return "DELETE";
        if (upperStatement.startsWith("SELECT")) return "SELECT";
        return "SQL";
    }
    
    private static String getTargetTable(String statement) {
        String pattern = "(?i)(INTO|FROM|UPDATE)\\s+([\\w]+)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(statement);
        if (m.find()) {
            return m.group(2);
        }
        return "unknown";
    }
    
    private static int countInsertRows(String statement) {
        String upperStatement = statement.toUpperCase();
        if (!upperStatement.contains("VALUES")) {
            return 1;
        }
        
        int valueCount = 0;
        boolean inQuote = false;
        int parenthesesLevel = 0;
        
        int valuesIndex = upperStatement.indexOf("VALUES");
        if (valuesIndex == -1) {
            return 1;
        }
        
        for (int i = valuesIndex + 6; i < statement.length(); i++) {
            char c = statement.charAt(i);
            
            if (c == '\'' && (i == 0 || statement.charAt(i-1) != '\\')) {
                inQuote = !inQuote;
            }
            
            if (!inQuote) {
                if (c == '(') {
                    parenthesesLevel++;
                    if (parenthesesLevel == 1) {
                        valueCount++;
                    }
                } else if (c == ')') {
                    parenthesesLevel--;
                }
            }
        }
        
        return Math.max(1, valueCount);
    }
    
    private static List<String> splitLargeInsert(String statement, int batchSize) {
        List<String> batches = new ArrayList<>();
        
        if (!statement.toUpperCase().trim().startsWith("INSERT")) {
            batches.add(statement);
            return batches;
        }
        
        int valuesIndex = statement.toUpperCase().indexOf("VALUES");
        if (valuesIndex == -1) {
            batches.add(statement);
            return batches;
        }
        
        String prefix = statement.substring(0, valuesIndex + 6);
        String valuesPart = statement.substring(valuesIndex + 6).trim();
        
        List<String> valueGroups = new ArrayList<>();
        StringBuilder currentGroup = new StringBuilder();
        int parenthesesLevel = 0;
        boolean inQuote = false;
        
        for (int i = 0; i < valuesPart.length(); i++) {
            char c = valuesPart.charAt(i);
            
            if (c == '\'' && (i == 0 || valuesPart.charAt(i-1) != '\\')) {
                inQuote = !inQuote;
            }
            
            if (c == '(' && !inQuote) parenthesesLevel++;
            if (c == ')' && !inQuote) parenthesesLevel--;
            
            currentGroup.append(c);
            
            if (parenthesesLevel == 0 && currentGroup.length() > 0 && c == ')') {
                String group = currentGroup.toString().trim();
                if (group.endsWith(",")) {
                    group = group.substring(0, group.length() - 1).trim();
                }
                
                valueGroups.add(group);
                currentGroup = new StringBuilder();
                
                while (i + 1 < valuesPart.length() && valuesPart.charAt(i + 1) == ',') {
                    i++;
                }
            }
        }
        
        for (int i = 0; i < valueGroups.size(); i += batchSize) {
            StringBuilder batchStatement = new StringBuilder(prefix);
            for (int j = i; j < Math.min(i + batchSize, valueGroups.size()); j++) {
                if (j > i) batchStatement.append(", ");
                batchStatement.append(valueGroups.get(j));
            }
            batches.add(batchStatement.toString());
        }
        
        return batches;
    }
}