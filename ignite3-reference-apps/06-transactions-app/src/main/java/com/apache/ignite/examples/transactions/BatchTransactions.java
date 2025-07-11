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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Batch Transactions - Bulk operations within transactions in Apache Ignite 3.
 * 
 * This class demonstrates batch processing patterns with transactions:
 * - Bulk insert operations
 * - Batch update patterns
 * - Large dataset handling
 * - Performance optimization techniques
 * 
 * Learning Focus:
 * - Batch processing in transactions
 * - Performance optimization patterns
 * - Memory management with large datasets
 * - Error handling in batch operations
 */
public class BatchTransactions {

    private static final Logger logger = LoggerFactory.getLogger(BatchTransactions.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Batch Transactions Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            runBatchTransactions(client);
            
        } catch (Exception e) {
            logger.error("Failed to run batch transactions", e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void runBatchTransactions(IgniteClient client) {
        // Get the Artist table
        Table artistTable = client.tables().table("Artist");
        if (artistTable == null) {
            throw new RuntimeException("Artist table not found. Please run sample data setup first.");
        }
        
        RecordView<Tuple> artists = artistTable.recordView();
        IgniteTransactions transactions = client.transactions();
        
        System.out.println("\n--- Batch Transaction Patterns ---");
        
        // Demonstrate batch processing patterns
        demonstrateBatchInsert(transactions, artists);
        demonstrateBatchUpdate(transactions, artists);
        demonstrateBatchErrorHandling(transactions, artists);
        
        System.out.println("\n<<< Batch transactions completed successfully");
    }

    private static void demonstrateBatchInsert(IgniteTransactions transactions, RecordView<Tuple> artists) {
        System.out.println("\n1. Batch Insert Transaction:");
        
        // Prepare batch data
        List<Tuple> artistBatch = new ArrayList<>();
        for (int i = 9001; i <= 9010; i++) {
            Tuple artist = Tuple.create()
                .set("ArtistId", i)
                .set("Name", "Batch Artist " + i);
            artistBatch.add(artist);
        }
        
        System.out.println("   >>> Prepared " + artistBatch.size() + " artists for batch insert");
        
        // Execute batch insert with functional transaction
        try {
            transactions.runInTransaction(tx -> {
                long startTime = System.currentTimeMillis();
                
                for (Tuple artist : artistBatch) {
                    artists.upsert(tx, artist);
                }
                
                long insertTime = System.currentTimeMillis() - startTime;
                System.out.println("   >>> Batch insert completed in " + insertTime + "ms");
                
                // Transaction commits automatically
            });
            
            System.out.println("   <<< Transaction committed successfully");
            
        } catch (Throwable e) {
            logger.error("Batch insert failed and was automatically rolled back", e);
            throw new RuntimeException("Batch insert failed", e);
        }
        
        // Verify batch insert
        verifyBatchData(artists, 9001, 9010, "Batch Artist");
    }

    private static void demonstrateBatchUpdate(IgniteTransactions transactions, RecordView<Tuple> artists) {
        System.out.println("\n2. Batch Update Transaction:");
        
        // Update all batch artists with functional transaction
        try {
            int updateCount = transactions.runInTransaction(tx -> {
                long startTime = System.currentTimeMillis();
                int count = 0;
                
                for (int id = 9001; id <= 9010; id++) {
                    Tuple key = Tuple.create().set("ArtistId", id);
                    Tuple artist = artists.get(tx, key);
                    
                    if (artist != null) {
                        Tuple updatedArtist = artist.set("Name", "Updated Batch Artist " + id);
                        artists.upsert(tx, updatedArtist);
                        count++;
                    }
                }
                
                long updateTime = System.currentTimeMillis() - startTime;
                System.out.println("   >>> Batch update of " + count + " records in " + updateTime + "ms");
                
                return count; // Transaction commits automatically
            });
            
            System.out.println("   <<< Batch update committed successfully");
            
        } catch (Throwable e) {
            logger.error("Batch update failed and was automatically rolled back", e);
            throw new RuntimeException("Batch update failed", e);
        }
        
        // Verify batch update
        verifyBatchData(artists, 9001, 9010, "Updated Batch Artist");
    }

    private static void demonstrateBatchErrorHandling(IgniteTransactions transactions, RecordView<Tuple> artists) {
        System.out.println("\n3. Batch Error Handling:");
        
        // Attempt batch operation with intentional error using functional transaction
        try {
            transactions.runInTransaction((Consumer<Transaction>) tx -> {
                int processedCount = 0;
                
                // Process first few records successfully
                for (int id = 9001; id <= 9005; id++) {
                    Tuple key = Tuple.create().set("ArtistId", id);
                    Tuple artist = artists.get(tx, key);
                    
                    if (artist != null) {
                        Tuple updatedArtist = artist.set("Name", "Error Test Artist " + id);
                        artists.upsert(tx, updatedArtist);
                        processedCount++;
                    }
                }
                
                System.out.println("   >>> Processed " + processedCount + " records successfully");
                
                // Simulate error during processing
                System.out.println("   !!! Simulating error during batch processing...");
                throw new RuntimeException("Simulated batch processing error");
            });
            
        } catch (Throwable e) {
            System.out.println("   <<< Error detected, transaction automatically rolled back: " + e.getMessage());
        }
        
        // Verify that no partial updates were committed
        System.out.println("   >>> Verifying rollback...");
        boolean allRolledBack = true;
        for (int id = 9001; id <= 9005; id++) {
            Tuple key = Tuple.create().set("ArtistId", id);
            Tuple artist = artists.get(null, key);
            
            if (artist != null && artist.stringValue("Name").startsWith("Error Test Artist")) {
                allRolledBack = false;
                break;
            }
        }
        
        if (allRolledBack) {
            System.out.println("   <<< Verified: All partial updates rolled back successfully");
        } else {
            System.out.println("   !!! Warning: Some partial updates may have persisted");
        }
        
        // Cleanup all test data
        cleanupBatchData(transactions, artists);
    }

    private static void verifyBatchData(RecordView<Tuple> artists, int startId, int endId, String expectedPrefix) {
        System.out.println("   >>> Verifying batch data...");
        int verifiedCount = 0;
        
        for (int id = startId; id <= endId; id++) {
            Tuple key = Tuple.create().set("ArtistId", id);
            Tuple artist = artists.get(null, key);
            
            if (artist != null && artist.stringValue("Name").startsWith(expectedPrefix)) {
                verifiedCount++;
            }
        }
        
        System.out.println("   <<< Verified " + verifiedCount + "/" + (endId - startId + 1) + " records");
    }

    private static void cleanupBatchData(IgniteTransactions transactions, RecordView<Tuple> artists) {
        System.out.println("\n4. Cleanup Batch Data:");
        
        transactions.runInTransaction(tx -> {
            int deletedCount = 0;
            
            for (int id = 9001; id <= 9010; id++) {
                Tuple key = Tuple.create().set("ArtistId", id);
                boolean deleted = artists.delete(tx, key);
                if (deleted) {
                    deletedCount++;
                }
            }
            
            System.out.println("   <<< Cleaned up " + deletedCount + " test records");
        });
    }
}