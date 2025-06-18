package com.apache.ignite.examples.transactions;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.tx.Transaction;
import org.apache.ignite.tx.IgniteTransactions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Async Transactions - Asynchronous transaction patterns with Apache Ignite 3.
 * 
 * This class demonstrates asynchronous transaction processing:
 * - Non-blocking transaction operations
 * - CompletableFuture patterns with transactions
 * - Async error handling and rollback
 * - Performance benefits of async transactions
 * 
 * Learning Focus:
 * - Async transaction lifecycle
 * - CompletableFuture with transactions
 * - Non-blocking error handling
 * - Async operation chaining
 */
public class AsyncTransactions {

    private static final Logger logger = LoggerFactory.getLogger(AsyncTransactions.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Async Transactions Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            runAsyncTransactions(client);
            
        } catch (Exception e) {
            logger.error("Failed to run async transactions", e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void runAsyncTransactions(IgniteClient client) throws ExecutionException, InterruptedException {
        // Get the Artist table
        Table artistTable = client.tables().table("Artist");
        if (artistTable == null) {
            throw new RuntimeException("Artist table not found. Please run sample data setup first.");
        }
        
        RecordView<Tuple> artists = artistTable.recordView();
        IgniteTransactions transactions = client.transactions();
        
        System.out.println("\n--- Async Transaction Patterns ---");
        
        // Demonstrate async transaction patterns
        demonstrateAsyncCommit(transactions, artists);
        demonstrateAsyncChaining(transactions, artists);
        demonstrateAsyncErrorHandling(transactions, artists);
        
        System.out.println("\n✓ Async transactions completed successfully");
    }

    private static void demonstrateAsyncCommit(IgniteTransactions transactions, RecordView<Tuple> artists) 
            throws ExecutionException, InterruptedException {
        System.out.println("\n1. Async Transaction Commit:");
        
        Transaction tx = transactions.begin();
        
        try {
            // Create test data
            Tuple newArtist = Tuple.create()
                .set("ArtistId", 8001)
                .set("Name", "Async Artist 1");
            
            // Async upsert within transaction
            CompletableFuture<Void> upsertFuture = artists.upsertAsync(tx, newArtist);
            System.out.println("   ⚡ Async upsert started...");
            
            // Wait for upsert completion
            upsertFuture.get();
            System.out.println("   ✓ Async upsert completed");
            
            // Async commit
            CompletableFuture<Void> commitFuture = tx.commitAsync();
            System.out.println("   ⚡ Async commit started...");
            
            // Wait for commit completion
            commitFuture.get();
            System.out.println("   ✓ Async commit completed");
            
            // Verify the result
            Tuple key = Tuple.create().set("ArtistId", 8001);
            Tuple result = artists.get(null, key);
            if (result != null) {
                System.out.println("   ✓ Verified: " + result.stringValue("Name"));
            }
            
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }

    private static void demonstrateAsyncChaining(IgniteTransactions transactions, RecordView<Tuple> artists)
            throws ExecutionException, InterruptedException {
        System.out.println("\n2. Async Transaction Chaining:");
        
        final int artistId = 8002;
        
        CompletableFuture<String> chainedTransaction = CompletableFuture
            // Start with beginning a transaction
            .supplyAsync(() -> {
                System.out.println("   ⚡ Step 1: Beginning transaction...");
                return transactions.begin();
            })
            // Create and insert artist
            .thenCompose(tx -> {
                System.out.println("   ⚡ Step 2: Creating artist...");
                Tuple newArtist = Tuple.create()
                    .set("ArtistId", artistId)
                    .set("Name", "Chained Artist");
                
                return artists.upsertAsync(tx, newArtist)
                    .thenApply(ignored -> tx); // Pass transaction to next stage
            })
            // Read back the artist
            .thenCompose(tx -> {
                System.out.println("   ⚡ Step 3: Reading back artist...");
                Tuple key = Tuple.create().set("ArtistId", artistId);
                return artists.getAsync(tx, key)
                    .thenApply(artist -> new Object[]{tx, artist}); // Pass both tx and artist
            })
            // Update the artist
            .thenCompose(objects -> {
                Transaction tx = (Transaction) objects[0];
                Tuple artist = (Tuple) objects[1];
                
                System.out.println("   ⚡ Step 4: Updating artist...");
                Tuple updatedArtist = artist.set("Name", "Chained Updated Artist");
                return artists.upsertAsync(tx, updatedArtist)
                    .thenApply(ignored -> tx); // Pass transaction to commit
            })
            // Commit the transaction
            .thenCompose(tx -> {
                System.out.println("   ⚡ Step 5: Committing transaction...");
                return tx.commitAsync()
                    .thenApply(ignored -> "Transaction chain completed successfully");
            })
            // Handle any errors
            .exceptionally(throwable -> {
                logger.error("Async transaction chain failed", throwable);
                return "Transaction chain failed: " + throwable.getMessage();
            });
        
        String result = chainedTransaction.get();
        System.out.println("   ✓ " + result);
        
        // Cleanup
        Tuple key = Tuple.create().set("ArtistId", artistId);
        artists.delete(null, key);
    }

    private static void demonstrateAsyncErrorHandling(IgniteTransactions transactions, RecordView<Tuple> artists)
            throws ExecutionException, InterruptedException {
        System.out.println("\n3. Async Error Handling:");
        
        final int artistId = 8003;
        
        CompletableFuture<String> errorHandlingDemo = CompletableFuture
            .supplyAsync(() -> {
                System.out.println("   ⚡ Starting transaction with potential error...");
                return transactions.begin();
            })
            .thenCompose(tx -> {
                // Insert valid data
                Tuple newArtist = Tuple.create()
                    .set("ArtistId", artistId)
                    .set("Name", "Error Test Artist");
                
                return artists.upsertAsync(tx, newArtist)
                    .thenCompose(ignored -> {
                        // Simulate an error condition
                        System.out.println("   ⚠ Simulating error condition...");
                        CompletableFuture<Void> errorFuture = new CompletableFuture<>();
                        errorFuture.completeExceptionally(new RuntimeException("Simulated async error"));
                        return errorFuture;
                    })
                    .thenApply(ignored -> tx);
            })
            .handle((tx, throwable) -> {
                if (throwable != null) {
                    System.out.println("   ⚠ Error detected, rolling back transaction...");
                    try {
                        if (tx != null) {
                            tx.rollback();
                        }
                        return "Transaction rolled back due to error";
                    } catch (Exception rollbackError) {
                        logger.error("Rollback failed", rollbackError);
                        return "Rollback failed";
                    }
                } else {
                    return "Transaction completed successfully";
                }
            });
        
        String result = errorHandlingDemo.get();
        System.out.println("   ✓ " + result);
        
        // Verify rollback worked
        Tuple key = Tuple.create().set("ArtistId", artistId);
        Tuple artist = artists.get(null, key);
        if (artist == null) {
            System.out.println("   ✓ Verified: Transaction was properly rolled back");
        } else {
            System.out.println("   ⚠ Unexpected: Data found after rollback");
            artists.delete(null, key); // Cleanup
        }
        
        // Final cleanup
        artists.delete(null, Tuple.create().set("ArtistId", 8001));
    }
}