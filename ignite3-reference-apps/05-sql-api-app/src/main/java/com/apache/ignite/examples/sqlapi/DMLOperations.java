package com.apache.ignite.examples.sqlapi;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.sql.BatchedArguments;
import org.apache.ignite.tx.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates DML (Data Manipulation Language) operations using the Ignite 3 SQL API.
 * 
 * This class focuses on data manipulation operations including:
 * - INSERT operations with parameter binding
 * - UPDATE operations with conditional logic
 * - DELETE operations and cascading effects
 * - MERGE (UPSERT) operations for conflict resolution
 * - Batch operations for bulk data processing
 * - Transaction coordination for ACID compliance
 * 
 * The emphasis is on using the Java SQL API for data operations rather than
 * SQL syntax itself, demonstrating patterns for efficient data manipulation
 * in distributed environments.
 * 
 * Prerequisites:
 * - Ignite 3 cluster running on localhost:10800
 * - Sample music store data loaded (run sample-data-setup first)
 * 
 * @see org.apache.ignite.sql.IgniteSql#execute
 * @see org.apache.ignite.sql.IgniteSql#executeBatch
 * @see org.apache.ignite.tx.Transaction
 */
public class DMLOperations {
    
    private static final Logger logger = LoggerFactory.getLogger(DMLOperations.class);
    
    public static void main(String[] args) {
        logger.info("Starting DML Operations Demo");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800")
                .build()) {
            
            DMLOperations demo = new DMLOperations();
            
            // Demonstrate various DML operations
            demo.demonstrateInsertOperations(client);
            demo.demonstrateUpdateOperations(client);
            demo.demonstrateDeleteOperations(client);
            demo.demonstrateMergeOperations(client);
            demo.demonstrateBatchDMLOperations(client);
            demo.demonstrateTransactionalDML(client);
            demo.demonstrateDataValidation(client);
            demo.cleanupDemoData(client);
            
            logger.info("DML Operations Demo completed successfully");
            
        } catch (Exception e) {
            logger.error("DML Operations Demo failed", e);
            System.exit(1);
        }
    }
    
    /**
     * Demonstrates INSERT operations with various parameter binding patterns.
     * Shows how to safely insert data using the SQL API.
     */
    private void demonstrateInsertOperations(IgniteClient client) {
        logger.info("=== INSERT Operations ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // Single row insert with parameter binding
            logger.info("Inserting single artist record...");
            ResultSet<SqlRow> insertResult = sql.execute(null,
                "INSERT INTO Artist (Name, Country) VALUES (?, ?)",
                "DML Demo Artist", "Demo Country");
            
            if (!insertResult.hasRowSet()) {
                long affectedRows = insertResult.affectedRows();
                logger.info("Inserted {} artist record", affectedRows);
            }
            
            // Get the inserted artist ID for follow-up operations
            ResultSet<SqlRow> artistQuery = sql.execute(null,
                "SELECT ArtistId FROM Artist WHERE Name = ?", "DML Demo Artist");
            
            int demoArtistId = 0;
            if (artistQuery.hasNext()) {
                demoArtistId = artistQuery.next().intValue("ArtistId");
                logger.info("Demo artist created with ID: {}", demoArtistId);
            }
            
            // Insert related data with foreign key relationship
            if (demoArtistId > 0) {
                logger.info("Inserting related album...");
                sql.execute(null,
                    "INSERT INTO Album (ArtistId, Title) VALUES (?, ?)",
                    demoArtistId, "DML Demo Album");
                
                logger.info("Album inserted for artist ID: {}", demoArtistId);
            }
            
            // Insert with multiple data types
            logger.info("Inserting track with various data types...");
            
            // First get an album ID to use
            ResultSet<SqlRow> albumQuery = sql.execute(null,
                "SELECT AlbumId FROM Album WHERE Title = ?", "DML Demo Album");
            
            if (albumQuery.hasNext()) {
                int albumId = albumQuery.next().intValue("AlbumId");
                
                sql.execute(null,
                    "INSERT INTO Track (AlbumId, Name, MediaTypeId, GenreId, Milliseconds, UnitPrice) " +
                    "VALUES (?, ?, ?, ?, ?, ?)",
                    albumId, "DML Demo Track", 1, 1, 180000, new BigDecimal("0.99"));
                
                logger.info("Track inserted for album ID: {}", albumId);
            }
            
            // Conditional insert (INSERT WHERE NOT EXISTS pattern)
            logger.info("Demonstrating conditional insert...");
            ResultSet<SqlRow> conditionalInsert = sql.execute(null,
                "INSERT INTO Artist (Name, Country) " +
                "SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM Artist WHERE Name = ?)",
                "Conditional Artist", "Test Country", "Conditional Artist");
            
            if (!conditionalInsert.hasRowSet()) {
                long affected = conditionalInsert.affectedRows();
                logger.info("Conditional insert affected {} rows", affected);
            }
            
        } catch (Exception e) {
            logger.error("Failed to perform insert operations: {}", e.getMessage());
        }
    }
    
    /**
     * Demonstrates UPDATE operations with conditional logic and parameter binding.
     * Shows how to safely update data in distributed environments.
     */
    private void demonstrateUpdateOperations(IgniteClient client) {
        logger.info("=== UPDATE Operations ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // Simple update with WHERE clause
            logger.info("Updating artist country...");
            ResultSet<SqlRow> updateResult = sql.execute(null,
                "UPDATE Artist SET Country = ? WHERE Name = ?",
                "Updated Country", "DML Demo Artist");
            
            if (!updateResult.hasRowSet()) {
                long affectedRows = updateResult.affectedRows();
                logger.info("Updated {} artist records", affectedRows);
            }
            
            // Conditional update with CASE expression
            logger.info("Performing conditional update...");
            sql.execute(null,
                "UPDATE Album SET Title = CASE " +
                "    WHEN Title LIKE '%Demo%' THEN CONCAT(Title, ' - Updated') " +
                "    ELSE Title " +
                "END " +
                "WHERE ArtistId IN (SELECT ArtistId FROM Artist WHERE Name = ?)",
                "DML Demo Artist");
            
            logger.info("Conditional update completed");
            
            // Update with JOIN (subquery pattern)
            logger.info("Updating tracks based on album criteria...");
            ResultSet<SqlRow> joinUpdate = sql.execute(null,
                "UPDATE Track SET UnitPrice = UnitPrice * ? " +
                "WHERE AlbumId IN (" +
                "    SELECT al.AlbumId FROM Album al " +
                "    JOIN Artist ar ON al.ArtistId = ar.ArtistId " +
                "    WHERE ar.Name = ?" +
                ")",
                new BigDecimal("1.1"), "DML Demo Artist");
            
            if (!joinUpdate.hasRowSet()) {
                long affected = joinUpdate.affectedRows();
                logger.info("Updated {} track prices", affected);
            }
            
            // Bulk update with statistics
            logger.info("Performing bulk update with validation...");
            
            // First, check how many records would be affected
            ResultSet<SqlRow> countQuery = sql.execute(null,
                "SELECT COUNT(*) as count FROM Artist WHERE Country IS NULL");
            
            long nullCountryCount = 0;
            if (countQuery.hasNext()) {
                nullCountryCount = countQuery.next().longValue("count");
                logger.info("Found {} artists with NULL country", nullCountryCount);
            }
            
            // Update NULL countries to default value
            if (nullCountryCount > 0) {
                sql.execute(null,
                    "UPDATE Artist SET Country = ? WHERE Country IS NULL",
                    "Unknown");
                
                logger.info("Updated {} artists with default country", nullCountryCount);
            }
            
        } catch (Exception e) {
            logger.error("Failed to perform update operations: {}", e.getMessage());
        }
    }
    
    /**
     * Demonstrates DELETE operations with cascading considerations.
     * Shows safe deletion patterns for related data.
     */
    private void demonstrateDeleteOperations(IgniteClient client) {
        logger.info("=== DELETE Operations ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // Demonstrate cascading delete pattern (manual implementation)
            logger.info("Performing cascading delete...");
            
            // First, get the artist ID for demo data
            ResultSet<SqlRow> artistQuery = sql.execute(null,
                "SELECT ArtistId FROM Artist WHERE Name = ?", "DML Demo Artist");
            
            if (artistQuery.hasNext()) {
                int artistId = artistQuery.next().intValue("ArtistId");
                
                // Delete in dependency order: Tracks -> Albums -> Artists
                
                // Delete tracks first
                ResultSet<SqlRow> trackDelete = sql.execute(null,
                    "DELETE FROM Track WHERE AlbumId IN (" +
                    "    SELECT AlbumId FROM Album WHERE ArtistId = ?" +
                    ")", artistId);
                
                if (!trackDelete.hasRowSet()) {
                    long deletedTracks = trackDelete.affectedRows();
                    logger.info("Deleted {} tracks", deletedTracks);
                }
                
                // Delete albums next
                ResultSet<SqlRow> albumDelete = sql.execute(null,
                    "DELETE FROM Album WHERE ArtistId = ?", artistId);
                
                if (!albumDelete.hasRowSet()) {
                    long deletedAlbums = albumDelete.affectedRows();
                    logger.info("Deleted {} albums", deletedAlbums);
                }
                
                // Finally delete the artist
                ResultSet<SqlRow> artistDelete = sql.execute(null,
                    "DELETE FROM Artist WHERE ArtistId = ?", artistId);
                
                if (!artistDelete.hasRowSet()) {
                    long deletedArtists = artistDelete.affectedRows();
                    logger.info("Deleted {} artists", deletedArtists);
                }
            }
            
            // Conditional delete with EXISTS
            logger.info("Performing conditional delete...");
            ResultSet<SqlRow> conditionalDelete = sql.execute(null,
                "DELETE FROM Artist WHERE Name = ? AND NOT EXISTS (" +
                "    SELECT 1 FROM Album WHERE Album.ArtistId = Artist.ArtistId" +
                ")", "Conditional Artist");
            
            if (!conditionalDelete.hasRowSet()) {
                long affected = conditionalDelete.affectedRows();
                logger.info("Conditional delete affected {} rows", affected);
            }
            
            // Bulk delete with criteria
            logger.info("Performing bulk delete with date criteria...");
            
            // Create some test data first
            sql.execute(null,
                "INSERT INTO Artist (Name, Country) VALUES (?, ?)",
                "Temp Artist 1", "Temp Country");
            sql.execute(null,
                "INSERT INTO Artist (Name, Country) VALUES (?, ?)",
                "Temp Artist 2", "Temp Country");
            
            // Delete temporary artists
            ResultSet<SqlRow> bulkDelete = sql.execute(null,
                "DELETE FROM Artist WHERE Name LIKE ? AND Country = ?",
                "Temp Artist%", "Temp Country");
            
            if (!bulkDelete.hasRowSet()) {
                long deleted = bulkDelete.affectedRows();
                logger.info("Bulk delete removed {} temporary artists", deleted);
            }
            
        } catch (Exception e) {
            logger.error("Failed to perform delete operations: {}", e.getMessage());
        }
    }
    
    /**
     * Demonstrates MERGE (UPSERT) operations for conflict resolution.
     * Shows how to handle insert-or-update scenarios efficiently.
     */
    private void demonstrateMergeOperations(IgniteClient client) {
        logger.info("=== MERGE/UPSERT Operations ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // Since Ignite may not support full MERGE syntax, demonstrate UPSERT pattern
            // using INSERT with conflict resolution or UPDATE + INSERT pattern
            
            logger.info("Demonstrating UPSERT pattern...");
            
            // Check if artist exists
            String artistName = "Merge Demo Artist";
            ResultSet<SqlRow> existsQuery = sql.execute(null,
                "SELECT ArtistId, Country FROM Artist WHERE Name = ?", artistName);
            
            if (existsQuery.hasNext()) {
                // Artist exists, update it
                SqlRow existing = existsQuery.next();
                int artistId = existing.intValue("ArtistId");
                String currentCountry = existing.stringValue("Country");
                
                logger.info("Artist exists with ID: {}, current country: {}", artistId, currentCountry);
                
                sql.execute(null,
                    "UPDATE Artist SET Country = ? WHERE ArtistId = ?",
                    "Updated via UPSERT", artistId);
                
                logger.info("Updated existing artist via UPSERT pattern");
                
            } else {
                // Artist doesn't exist, insert it
                sql.execute(null,
                    "INSERT INTO Artist (Name, Country) VALUES (?, ?)",
                    artistName, "Inserted via UPSERT");
                
                logger.info("Inserted new artist via UPSERT pattern");
            }
            
            // Demonstrate batch UPSERT pattern
            logger.info("Demonstrating batch UPSERT pattern...");
            
            String[] upsertArtists = {
                "Upsert Artist 1", "Upsert Artist 2", "Upsert Artist 3"
            };
            
            for (String name : upsertArtists) {
                // Try to update first
                ResultSet<SqlRow> updateResult = sql.execute(null,
                    "UPDATE Artist SET Country = ? WHERE Name = ?",
                    "Batch Upsert Country", name);
                
                if (!updateResult.hasRowSet() && updateResult.affectedRows() == 0) {
                    // No rows updated, so insert
                    sql.execute(null,
                        "INSERT INTO Artist (Name, Country) VALUES (?, ?)",
                        name, "Batch Upsert Country");
                    
                    logger.info("Inserted new artist: {}", name);
                } else {
                    logger.info("Updated existing artist: {}", name);
                }
            }
            
            // Verify UPSERT results
            ResultSet<SqlRow> verifyQuery = sql.execute(null,
                "SELECT Name, Country FROM Artist WHERE Name LIKE ? ORDER BY Name",
                "%Upsert%");
            
            logger.info("UPSERT verification results:");
            while (verifyQuery.hasNext()) {
                SqlRow row = verifyQuery.next();
                logger.info("  {}: {}", row.stringValue("Name"), row.stringValue("Country"));
            }
            
        } catch (Exception e) {
            logger.error("Failed to perform merge operations: {}", e.getMessage());
        }
    }
    
    /**
     * Demonstrates batch DML operations for efficient bulk processing.
     * Shows how to use BatchedArguments for high-performance data operations.
     */
    private void demonstrateBatchDMLOperations(IgniteClient client) {
        logger.info("=== Batch DML Operations ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // Batch INSERT operations
            logger.info("Performing batch INSERT...");
            
            BatchedArguments insertBatch = BatchedArguments.create()
                .add("Batch Artist 1", "Batch Country A")
                .add("Batch Artist 2", "Batch Country B")
                .add("Batch Artist 3", "Batch Country A")
                .add("Batch Artist 4", "Batch Country C")
                .add("Batch Artist 5", "Batch Country B");
            
            long[] insertResults = sql.executeBatch(null,
                "INSERT INTO Artist (Name, Country) VALUES (?, ?)", insertBatch);
            
            logger.info("Batch INSERT completed: {} operations", insertResults.length);
            for (int i = 0; i < insertResults.length; i++) {
                logger.info("  Operation {}: {} rows affected", i + 1, insertResults[i]);
            }
            
            // Batch UPDATE operations
            logger.info("Performing batch UPDATE...");
            
            BatchedArguments updateBatch = BatchedArguments.create()
                .add("Updated Country 1", "Batch Artist 1")
                .add("Updated Country 2", "Batch Artist 2")
                .add("Updated Country 3", "Batch Artist 3");
            
            long[] updateResults = sql.executeBatch(null,
                "UPDATE Artist SET Country = ? WHERE Name = ?", updateBatch);
            
            logger.info("Batch UPDATE completed: {} operations", updateResults.length);
            long totalUpdated = 0;
            for (long result : updateResults) {
                totalUpdated += result;
            }
            logger.info("Total rows updated: {}", totalUpdated);
            
            // Verify batch operations
            ResultSet<SqlRow> batchVerify = sql.execute(null,
                "SELECT Name, Country FROM Artist WHERE Name LIKE ? ORDER BY Name",
                "Batch Artist%");
            
            logger.info("Batch operations verification:");
            while (batchVerify.hasNext()) {
                SqlRow row = batchVerify.next();
                logger.info("  {}: {}", row.stringValue("Name"), row.stringValue("Country"));
            }
            
        } catch (Exception e) {
            logger.error("Failed to perform batch DML operations: {}", e.getMessage());
        }
    }
    
    /**
     * Demonstrates transactional DML operations for ACID compliance.
     * Shows how to coordinate multiple DML operations within transactions.
     */
    private void demonstrateTransactionalDML(IgniteClient client) {
        logger.info("=== Transactional DML Operations ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // Successful transaction
            logger.info("Demonstrating successful transaction...");
            
            try (Transaction tx = client.transactions().begin()) {
                // Insert artist
                sql.execute(tx, "INSERT INTO Artist (Name, Country) VALUES (?, ?)",
                    "Transactional Artist", "TX Country");
                
                // Get artist ID
                ResultSet<SqlRow> artistQuery = sql.execute(tx,
                    "SELECT ArtistId FROM Artist WHERE Name = ?", "Transactional Artist");
                
                if (artistQuery.hasNext()) {
                    int artistId = artistQuery.next().intValue("ArtistId");
                    
                    // Insert album in same transaction
                    sql.execute(tx, "INSERT INTO Album (ArtistId, Title) VALUES (?, ?)",
                        artistId, "Transactional Album");
                    
                    logger.info("Created artist and album in transaction");
                }
                
                tx.commit();
                logger.info("Transaction committed successfully");
            }
            
            // Failed transaction (rollback scenario)
            logger.info("Demonstrating transaction rollback...");
            
            try (Transaction tx = client.transactions().begin()) {
                // Insert valid artist
                sql.execute(tx, "INSERT INTO Artist (Name, Country) VALUES (?, ?)",
                    "Rollback Artist", "RB Country");
                
                logger.info("Inserted artist in transaction");
                
                // Try to insert duplicate or invalid data that will fail
                try {
                    sql.execute(tx, "INSERT INTO Artist (ArtistId, Name) VALUES (?, ?)",
                        1, "This should fail due to duplicate key");
                    
                    tx.commit(); // This won't be reached if above fails
                    
                } catch (Exception e) {
                    logger.warn("Expected error in transaction: {}", e.getMessage());
                    // Transaction will be automatically rolled back when tx is closed
                }
                
            } // Transaction automatically rolled back here
            
            logger.info("Transaction rolled back as expected");
            
            // Verify rollback - "Rollback Artist" should not exist
            ResultSet<SqlRow> rollbackVerify = sql.execute(null,
                "SELECT COUNT(*) as count FROM Artist WHERE Name = ?", "Rollback Artist");
            
            if (rollbackVerify.hasNext()) {
                long count = rollbackVerify.next().longValue("count");
                logger.info("Rollback verification: {} 'Rollback Artist' records (should be 0)", count);
            }
            
            // Complex transaction with multiple operations
            logger.info("Demonstrating complex transaction...");
            
            try (Transaction tx = client.transactions().begin()) {
                // Multiple related operations
                sql.execute(tx, "INSERT INTO Artist (Name, Country) VALUES (?, ?)",
                    "Complex TX Artist", "Complex Country");
                
                ResultSet<SqlRow> complexArtistQuery = sql.execute(tx,
                    "SELECT ArtistId FROM Artist WHERE Name = ?", "Complex TX Artist");
                
                if (complexArtistQuery.hasNext()) {
                    int artistId = complexArtistQuery.next().intValue("ArtistId");
                    
                    // Insert multiple albums
                    sql.execute(tx, "INSERT INTO Album (ArtistId, Title) VALUES (?, ?)",
                        artistId, "Complex Album 1");
                    sql.execute(tx, "INSERT INTO Album (ArtistId, Title) VALUES (?, ?)",
                        artistId, "Complex Album 2");
                    
                    // Update artist with additional info
                    sql.execute(tx, "UPDATE Artist SET Country = ? WHERE ArtistId = ?",
                        "Updated Complex Country", artistId);
                    
                    logger.info("Performed multiple operations in complex transaction");
                }
                
                tx.commit();
                logger.info("Complex transaction committed successfully");
            }
            
        } catch (Exception e) {
            logger.error("Failed to perform transactional DML operations: {}", e.getMessage());
        }
    }
    
    /**
     * Demonstrates data validation and constraint handling in DML operations.
     * Shows how to handle validation errors and constraint violations.
     */
    private void demonstrateDataValidation(IgniteClient client) {
        logger.info("=== Data Validation and Constraints ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // Demonstrate NOT NULL constraint handling
            logger.info("Testing NOT NULL constraint...");
            try {
                sql.execute(null, "INSERT INTO Artist (Name, Country) VALUES (?, ?)",
                    null, "Test Country");
            } catch (Exception e) {
                logger.warn("Expected NOT NULL constraint violation: {}", e.getMessage());
            }
            
            // Demonstrate data type validation
            logger.info("Testing data type validation...");
            try {
                // This should work fine
                sql.execute(null, "INSERT INTO Track (AlbumId, Name, MediaTypeId, GenreId, Milliseconds, UnitPrice) " +
                    "VALUES (?, ?, ?, ?, ?, ?)",
                    1, "Validation Test Track", 1, 1, 200000, new BigDecimal("1.29"));
                
                logger.info("Valid data insertion succeeded");
                
                // Clean up test track
                sql.execute(null, "DELETE FROM Track WHERE Name = ?", "Validation Test Track");
                
            } catch (Exception e) {
                logger.error("Unexpected validation error: {}", e.getMessage());
            }
            
            // Demonstrate foreign key constraint handling (if enabled)
            logger.info("Testing referential integrity...");
            try {
                sql.execute(null, "INSERT INTO Album (ArtistId, Title) VALUES (?, ?)",
                    999999, "Album for Non-existent Artist");
            } catch (Exception e) {
                logger.warn("Expected foreign key constraint violation: {}", e.getMessage());
            }
            
            // Demonstrate business rule validation
            logger.info("Implementing business rule validation...");
            
            // Check if artist name already exists before inserting
            String newArtistName = "Validation Test Artist";
            ResultSet<SqlRow> existingCheck = sql.execute(null,
                "SELECT COUNT(*) as count FROM Artist WHERE Name = ?", newArtistName);
            
            if (existingCheck.hasNext()) {
                long count = existingCheck.next().longValue("count");
                if (count > 0) {
                    logger.warn("Business rule violation: Artist '{}' already exists", newArtistName);
                } else {
                    sql.execute(null, "INSERT INTO Artist (Name, Country) VALUES (?, ?)",
                        newArtistName, "Validation Country");
                    logger.info("Business rule validation passed, artist inserted");
                    
                    // Clean up
                    sql.execute(null, "DELETE FROM Artist WHERE Name = ?", newArtistName);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to demonstrate data validation: {}", e.getMessage());
        }
    }
    
    /**
     * Cleans up all demo data created during the demonstration.
     * Shows proper cleanup patterns for test data.
     */
    private void cleanupDemoData(IgniteClient client) {
        logger.info("=== Cleanup Demo Data ===");
        
        IgniteSql sql = client.sql();
        
        try {
            // Clean up in dependency order
            logger.info("Cleaning up demo data...");
            
            // Delete tracks for demo albums
            sql.execute(null,
                "DELETE FROM Track WHERE AlbumId IN (" +
                "    SELECT al.AlbumId FROM Album al " +
                "    JOIN Artist ar ON al.ArtistId = ar.ArtistId " +
                "    WHERE ar.Name LIKE '%Demo%' OR ar.Name LIKE '%Merge%' OR ar.Name LIKE '%Upsert%' " +
                "    OR ar.Name LIKE '%Batch%' OR ar.Name LIKE '%Transactional%' OR ar.Name LIKE '%Complex%'" +
                ")");
            
            // Delete demo albums
            sql.execute(null,
                "DELETE FROM Album WHERE ArtistId IN (" +
                "    SELECT ArtistId FROM Artist " +
                "    WHERE Name LIKE '%Demo%' OR Name LIKE '%Merge%' OR Name LIKE '%Upsert%' " +
                "    OR Name LIKE '%Batch%' OR Name LIKE '%Transactional%' OR Name LIKE '%Complex%'" +
                ")");
            
            // Delete demo artists
            ResultSet<SqlRow> cleanupResult = sql.execute(null,
                "DELETE FROM Artist " +
                "WHERE Name LIKE '%Demo%' OR Name LIKE '%Merge%' OR Name LIKE '%Upsert%' " +
                "OR Name LIKE '%Batch%' OR Name LIKE '%Transactional%' OR Name LIKE '%Complex%' " +
                "OR Name LIKE '%Temp%' OR Name LIKE '%Validation%'");
            
            if (!cleanupResult.hasRowSet()) {
                long deletedArtists = cleanupResult.affectedRows();
                logger.info("Cleaned up {} demo artist records", deletedArtists);
            }
            
            logger.info("Demo data cleanup completed");
            
        } catch (Exception e) {
            logger.error("Failed to cleanup demo data: {}", e.getMessage());
        }
    }
}