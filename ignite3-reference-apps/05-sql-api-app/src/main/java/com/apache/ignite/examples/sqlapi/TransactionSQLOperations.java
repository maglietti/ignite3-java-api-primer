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

package com.apache.ignite.examples.sqlapi;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.sql.Statement;
import org.apache.ignite.tx.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Demonstrates SQL operations within transactions using Apache Ignite 3.
 * 
 * Shows how to integrate SQL operations with Ignite's ACID transaction system,
 * including transaction lifecycle management, error handling, and rollback
 * scenarios. Demonstrates both simple and complex transactional patterns.
 * 
 * Key concepts:
 * - SQL operations within transaction context
 * - Transaction lifecycle (begin, commit, rollback)
 * - ACID guarantees with distributed SQL
 * - Error handling and transaction recovery
 * - Performance considerations for transactional SQL
 * 
 * Prerequisites:
 * - Running Ignite cluster
 * - Sample music store data loaded
 */
public class TransactionSQLOperations {

    private static final Logger logger = LoggerFactory.getLogger(TransactionSQLOperations.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Transaction SQL Operations Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Demonstrating SQL within ACID transactions");

        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            TransactionSQLOperations demo = new TransactionSQLOperations();
            demo.runTransactionSQLOperations(client);
            
        } catch (Exception e) {
            logger.error("Failed to run transaction SQL operations", e);
            System.err.println("!!! Error: " + e.getMessage());
        }
    }

    private void runTransactionSQLOperations(IgniteClient client) {
        IgniteSql sql = client.sql();
        
        System.out.println("\n--- SQL within Transactions ---");
        System.out.println("    ACID guarantees for distributed SQL operations");
        
        // Simple transaction patterns
        demonstrateBasicTransaction(client, sql);
        
        // Complex multi-table transaction
        demonstrateComplexTransaction(client, sql);
        
        // Error handling and rollback scenarios
        demonstrateErrorHandlingTransaction(client, sql);
        
        // Performance patterns with transactions
        demonstrateTransactionPerformance(client, sql);
        
        System.out.println("\n>>> Transaction SQL operations completed successfully");
    }

    /**
     * Demonstrates basic transaction lifecycle with SQL operations.
     */
    private void demonstrateBasicTransaction(IgniteClient client, IgniteSql sql) {
        System.out.println("\n--- Basic Transaction Lifecycle");
        System.out.println(">>> Creating and committing simple transaction");
        
        Transaction tx = client.transactions().begin();
        try {
            // Insert new test artist within transaction
            sql.execute(tx, "INSERT INTO Artist (ArtistId, Name) VALUES (?, ?)", 9001, "Transaction Demo Artist");
            
            System.out.println("<<< Created artist with ID: 9001");
            
            // Update within same transaction
            sql.execute(tx, "UPDATE Artist SET Name = ? WHERE ArtistId = ?", 
                       "Updated Demo Artist", 9001);
            
            System.out.println("<<< Updated artist name within transaction");
            
            // Commit the transaction
            tx.commit();
            System.out.println("<<< Transaction committed successfully");
            
        } catch (Exception e) {
            tx.rollback();
            System.err.println("!!! Transaction failed and was rolled back: " + e.getMessage());
        }
        
        // Verify transaction results outside transaction
        ResultSet<SqlRow> verification = sql.execute(null,
                "SELECT COUNT(*) as artist_count FROM Artist WHERE Name = ?", "Updated Demo Artist");
        
        if (verification.hasNext()) {
            long count = verification.next().longValue("artist_count");
            System.out.println("<<< Verification: " + count + " demo artists found");
        }
    }

    /**
     * Demonstrates complex multi-table transaction with proper colocation.
     */
    private void demonstrateComplexTransaction(IgniteClient client, IgniteSql sql) {
        System.out.println("\n--- Complex Multi-Table Transaction");
        System.out.println(">>> Creating related data across multiple tables");
        
        Transaction tx = client.transactions().begin();
        try {
            // Step 1: Create new artist
            int artistId = 9002;
            sql.execute(tx, "INSERT INTO Artist (ArtistId, Name) VALUES (?, ?)", artistId, "Complex Demo Artist");
            System.out.println(">>> Step 1: Created artist with ID " + artistId);
                
            // Step 2: Create album for the artist  
            int albumId = 9001;
            sql.execute(tx, "INSERT INTO Album (AlbumId, ArtistId, Title) VALUES (?, ?, ?)",
                       albumId, artistId, "Demo Album");
            
            System.out.println(">>> Step 2: Created album for artist");
            
            // Step 3: Create tracks for the album
            for (int i = 1; i <= 3; i++) {
                sql.execute(tx, 
                       "INSERT INTO Track (TrackId, AlbumId, Name, MediaTypeId, GenreId, Milliseconds, UnitPrice) " +
                       "VALUES (?, ?, ?, 1, 1, 180000, 0.99)",
                       9000 + i, albumId, "Demo Track " + i);
            }
            
            System.out.println(">>> Step 3: Created 3 tracks for album");
            
            // Commit the entire transaction
            tx.commit();
            System.out.println("<<< Complex transaction committed successfully");
            
        } catch (Exception e) {
            tx.rollback();
            System.err.println("!!! Complex transaction failed and was rolled back: " + e.getMessage());
        }
        
        // Verify the complete data hierarchy
        String verificationQuery = """
                SELECT a.Name as Artist, al.Title as Album, COUNT(t.TrackId) as Tracks
                FROM Artist a
                JOIN Album al ON a.ArtistId = al.ArtistId  
                JOIN Track t ON al.AlbumId = t.AlbumId
                WHERE a.Name = 'Complex Demo Artist'
                GROUP BY a.Name, al.Title
                """;
        
        ResultSet<SqlRow> verification = sql.execute(null, verificationQuery);
        
        if (verification.hasNext()) {
            SqlRow row = verification.next();
            System.out.println("<<< Verification: " + row.stringValue("Artist") + 
                             " - " + row.stringValue("Album") + 
                             " (" + row.longValue("Tracks") + " tracks)");
        }
    }

    /**
     * Demonstrates error handling and rollback scenarios.
     */
    private void demonstrateErrorHandlingTransaction(IgniteClient client, IgniteSql sql) {
        System.out.println("\n--- Error Handling and Rollback");
        System.out.println(">>> Demonstrating transaction rollback on errors");
        
        // Count before transaction
        ResultSet<SqlRow> beforeCount = sql.execute(null, "SELECT COUNT(*) as artist_count FROM Artist");
        long countBefore = beforeCount.next().longValue("artist_count");
        
        Transaction tx = client.transactions().begin();
        try {
            // Insert valid artist
            sql.execute(tx, "INSERT INTO Artist (ArtistId, Name) VALUES (?, ?)", 9003, "Error Demo Artist");
            System.out.println(">>> Step 1: Inserted valid artist");
            
            // Try to insert invalid data (duplicate primary key for demonstration)
            try {
                sql.execute(tx, "INSERT INTO Artist (ArtistId, Name) VALUES (?, ?)", 
                           1, "This should fail - duplicate ID");
                System.out.println("!!! This should not be reached");
            } catch (Exception e) {
                System.out.println(">>> Step 2: Constraint violation detected");
                throw e; // Re-throw to trigger rollback
            }
            
            tx.commit();
            
        } catch (Exception e) {
            tx.rollback();
            System.out.println("<<< Transaction rolled back due to error");
        }
        
        // Verify rollback - count should be unchanged
        ResultSet<SqlRow> afterCount = sql.execute(null, "SELECT COUNT(*) as artist_count FROM Artist");
        long countAfter = afterCount.next().longValue("artist_count");
        
        System.out.println("<<< Artist count before: " + countBefore + ", after: " + countAfter);
        if (countBefore == countAfter) {
            System.out.println("<<< Rollback successful - no data changes persisted");
        } else {
            System.out.println("!!! Unexpected: data changes detected after rollback");
        }
    }

    /**
     * Demonstrates performance optimization patterns for transactional SQL.
     */
    private void demonstrateTransactionPerformance(IgniteClient client, IgniteSql sql) {
        System.out.println("\n--- Transaction Performance Patterns");
        System.out.println(">>> Optimizing SQL operations within transactions");
        
        // Prepare reusable statements for better performance
        
        Statement queryStatement = sql.statementBuilder()
                .query("SELECT ArtistId FROM Artist WHERE Name = ?")
                .queryTimeout(5, TimeUnit.SECONDS)
                .build();
        
        Transaction tx = client.transactions().begin();
        try {
            long startTime = System.currentTimeMillis();
            
            // Use prepared statements within transaction
            for (int i = 1; i <= 5; i++) {
                String artistName = "Perf Demo Artist " + i;
                int artistId = 9010 + i;
                
                // Insert using prepared statement
                sql.execute(tx, "INSERT INTO Artist (ArtistId, Name) VALUES (?, ?)", artistId, artistName);
                
                System.out.println(">>> Created artist " + i + " with ID " + artistId);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            tx.commit();
            System.out.println("<<< Performance test completed in " + duration + "ms");
            
        } catch (Exception e) {
            tx.rollback();
            System.err.println("!!! Performance test failed: " + e.getMessage());
        }
        
        // Clean up performance test data
        sql.execute(null, "DELETE FROM Artist WHERE Name LIKE 'Perf Demo Artist%'");
        System.out.println(">>> Performance test data cleaned up");
        
        // Clean up all demo data
        sql.execute(null, "DELETE FROM Track WHERE Name LIKE 'Demo Track%'");
        sql.execute(null, "DELETE FROM Album WHERE Title LIKE 'Demo Album%'");
        sql.execute(null, "DELETE FROM Artist WHERE Name LIKE '%Demo Artist%'");
        System.out.println(">>> All demo data cleaned up");
    }
}