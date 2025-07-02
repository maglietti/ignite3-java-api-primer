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

package com.apache.ignite.examples.transactions;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.tx.Transaction;
import org.apache.ignite.tx.IgniteTransactions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Basic Transactions - Fundamental transaction operations with Apache Ignite 3.
 * 
 * This class demonstrates the core concepts of transaction processing:
 * - Beginning and committing transactions
 * - Rolling back transactions on errors
 * - ACID compliance demonstration
 * - Basic error handling patterns
 * 
 * Learning Focus:
 * - Transaction lifecycle management
 * - Commit and rollback patterns
 * - Error handling in transactions
 * - ACID property demonstrations
 */
public class BasicTransactions {

    private static final Logger logger = LoggerFactory.getLogger(BasicTransactions.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Basic Transactions Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Demonstrating fundamental ACID transaction operations");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            runBasicTransactions(client);
            
        } catch (Exception e) {
            logger.error("Failed to run basic transactions", e);
            System.err.println("!!! Error: " + e.getMessage());
        }
    }

    private static void runBasicTransactions(IgniteClient client) {
        // Get the Artist table
        Table artistTable = client.tables().table("Artist");
        if (artistTable == null) {
            throw new RuntimeException("Artist table not found. Please run sample data setup first.");
        }
        
        RecordView<Tuple> artists = artistTable.recordView();
        IgniteTransactions transactions = client.transactions();
        
        System.out.println("\n--- Transaction Fundamentals ---");
        System.out.println("    Learning transaction lifecycle and ACID properties");
        
        // Demonstrate transaction lifecycle
        demonstrateCommitTransaction(transactions, artists);
        demonstrateRollbackTransaction(transactions, artists);
        demonstrateErrorHandling(transactions, artists);
        
        System.out.println("\n>>> Basic transactions completed successfully");
    }

    private static void demonstrateCommitTransaction(IgniteTransactions transactions, RecordView<Tuple> artists) {
        System.out.println("\n    --- Successful Transaction (Commit)");
        
        try {
            transactions.runInTransaction(tx -> {
                // Create a test artist
                Tuple newArtist = Tuple.create()
                    .set("ArtistId", 6001)
                    .set("Name", "Transaction Artist 1");
                
                // Insert within transaction
                artists.upsert(tx, newArtist);
                System.out.println(">>> Inserted artist in transaction");
                
                // Update within same transaction
                Tuple updatedArtist = newArtist.set("Name", "Updated Transaction Artist 1");
                artists.upsert(tx, updatedArtist);
                System.out.println(">>> Updated artist in transaction");
                
                // Transaction commits automatically on successful completion
            });
            
            System.out.println("<<< Transaction committed successfully");
            
            // Verify the data persisted
            Tuple key = Tuple.create().set("ArtistId", 6001);
            Tuple result = artists.get(null, key);
            if (result != null) {
                System.out.println("<<< Verified: " + result.stringValue("Name"));
            }
            
        } catch (Throwable e) {
            logger.error("Transaction failed and was automatically rolled back", e);
            throw e;
        }
    }

    private static void demonstrateRollbackTransaction(IgniteTransactions transactions, RecordView<Tuple> artists) {
        System.out.println("\n    --- Failed Transaction (Rollback)");
        
        try {
            transactions.runInTransaction((Consumer<Transaction>) tx -> {
                // Insert a test artist
                Tuple newArtist = Tuple.create()
                    .set("ArtistId", 6002)
                    .set("Name", "Transaction Artist 2");
                
                artists.upsert(tx, newArtist);
                System.out.println(">>> Inserted artist in transaction");
                
                // Simulate an error condition
                System.out.println(">>> Simulating error condition...");
                throw new RuntimeException("Simulated business logic error");
            });
            
        } catch (Throwable e) {
            System.out.println("<<< Transaction automatically rolled back due to error");
            
            // Verify the data was not persisted
            Tuple key = Tuple.create().set("ArtistId", 6002);
            Tuple result = artists.get(null, key);
            if (result == null) {
                System.out.println("<<< Verified: Artist not found (rollback worked)");
            } else {
                System.out.println("!!! Unexpected: Artist found after rollback");
            }
        }
    }

    private static void demonstrateErrorHandling(IgniteTransactions transactions, RecordView<Tuple> artists) {
        System.out.println("\n    --- Transaction Error Handling");
        
        // Demonstrate functional transaction pattern
        try {
            transactions.runInTransaction((Consumer<Transaction>) tx -> {
                // Insert test data
                Tuple newArtist = Tuple.create()
                    .set("ArtistId", 6003)
                    .set("Name", "Transaction Artist 3");
                
                artists.upsert(tx, newArtist);
                System.out.println(">>> Inserted artist in transaction");
                
                // Simulate successful business logic
                System.out.println(">>> Business logic executed successfully");
                
                // Return from lambda commits transaction
            });
            System.out.println("<<< Transaction committed with runInTransaction");
            
        } catch (Throwable e) {
            logger.error("Transaction failed and was automatically rolled back", e);
            System.out.println("<<< Transaction automatically rolled back");
        }
        
        // Verify final state
        Tuple key = Tuple.create().set("ArtistId", 6003);
        Tuple result = artists.get(null, key);
        if (result != null) {
            System.out.println("<<< Verified: " + result.stringValue("Name"));
        }
        
        // Cleanup test data
        cleanupTestData(artists);
    }

    private static void cleanupTestData(RecordView<Tuple> artists) {
        System.out.println("\n4. Cleanup Test Data:");
        
        for (int id = 6001; id <= 6003; id++) {
            Tuple key = Tuple.create().set("ArtistId", id);
            boolean deleted = artists.delete(null, key);
            if (deleted) {
                System.out.println(">>> Cleaned up artist " + id);
            }
        }
    }
}