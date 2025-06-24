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
 * - Non-blocking transaction operations using beginAsync()
 * - Proper CompletableFuture chaining without blocking calls
 * - Async error handling and rollback
 * - Performance benefits of async transactions
 * 
 * Learning Focus:
 * - Async transaction lifecycle with transactions.beginAsync()
 * - CompletableFuture composition without supplyAsync() wrapping
 * - Non-blocking error handling with proper rollback
 * - Async operation chaining for educational examples
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
        
        // Chain async transaction patterns
        CompletableFuture<Void> allTransactions = demonstrateAsyncCommit(transactions, artists)
            .thenCompose(ignored -> demonstrateAsyncChaining(transactions, artists))
            .thenCompose(ignored -> demonstrateAsyncErrorHandling(transactions, artists))
            .thenRun(() -> System.out.println("\n<<< Async transactions completed successfully"));
        
        // Wait for all async operations to complete
        allTransactions.get();
    }

    private static CompletableFuture<Void> demonstrateAsyncCommit(IgniteTransactions transactions, RecordView<Tuple> artists) {
        System.out.println("\n1. Async Transaction Commit:");
        
        return transactions.beginAsync()
            .thenCompose(tx -> {
                // Create test data
                Tuple newArtist = Tuple.create()
                    .set("ArtistId", 8001)
                    .set("Name", "Async Artist 1");
                
                System.out.println("   >>> Async upsert started...");
                
                // Chain upsert -> commit -> verification
                return artists.upsertAsync(tx, newArtist)
                    .thenCompose(ignored -> {
                        System.out.println("   <<< Async upsert completed");
                        System.out.println("   >>> Async commit started...");
                        return tx.commitAsync();
                    })
                    .thenCompose(ignored -> {
                        System.out.println("   <<< Async commit completed");
                        
                        // Verify the result asynchronously
                        Tuple key = Tuple.create().set("ArtistId", 8001);
                        return artists.getAsync(null, key);
                    })
                    .thenAccept(result -> {
                        if (result != null) {
                            System.out.println("   <<< Verified: " + result.stringValue("Name"));
                        }
                    })
                    .exceptionally(throwable -> {
                        System.out.println("   !!! Error in async commit, rolling back...");
                        try {
                            tx.rollback();
                        } catch (Exception rollbackError) {
                            logger.error("Rollback failed", rollbackError);
                        }
                        throw new RuntimeException("Async commit failed", throwable);
                    });
            });
    }

    private static CompletableFuture<Void> demonstrateAsyncChaining(IgniteTransactions transactions, RecordView<Tuple> artists) {
        System.out.println("\n2. Async Transaction Chaining:");
        
        final int artistId = 8002;
        
        return transactions.beginAsync()
            .thenCompose(tx -> {
                System.out.println("   >>> Step 1: Beginning transaction...");
                System.out.println("   >>> Step 2: Creating artist...");
                
                Tuple newArtist = Tuple.create()
                    .set("ArtistId", artistId)
                    .set("Name", "Chained Artist");
                
                return artists.upsertAsync(tx, newArtist)
                    .thenApply(ignored -> tx); // Pass transaction to next stage
            })
            // Read back the artist
            .thenCompose(tx -> {
                System.out.println("   >>> Step 3: Reading back artist...");
                Tuple key = Tuple.create().set("ArtistId", artistId);
                return artists.getAsync(tx, key)
                    .thenApply(artist -> new Object[]{tx, artist}); // Pass both tx and artist
            })
            // Update the artist
            .thenCompose(objects -> {
                Transaction tx = (Transaction) objects[0];
                Tuple artist = (Tuple) objects[1];
                
                System.out.println("   >>> Step 4: Updating artist...");
                Tuple updatedArtist = artist.set("Name", "Chained Updated Artist");
                return artists.upsertAsync(tx, updatedArtist)
                    .thenApply(ignored -> tx); // Pass transaction to commit
            })
            // Commit the transaction
            .thenCompose(tx -> {
                System.out.println("   >>> Step 5: Committing transaction...");
                return tx.commitAsync()
                    .thenAccept(ignored -> System.out.println("   <<< Transaction chain completed successfully"))
                    .exceptionally(throwable -> {
                        System.out.println("   !!! Commit failed, rolling back...");
                        try {
                            tx.rollback();
                        } catch (Exception rollbackError) {
                            logger.error("Rollback failed", rollbackError);
                        }
                        throw new RuntimeException("Transaction chain failed", throwable);
                    });
            })
            .thenCompose(ignored -> {
                // Cleanup
                System.out.println("   >>> Cleaning up test data...");
                Tuple key = Tuple.create().set("ArtistId", artistId);
                return artists.deleteAsync(null, key);
            })
            .exceptionally(throwable -> {
                logger.error("Async transaction chain failed", throwable);
                System.out.println("   <<< Transaction chain failed: " + throwable.getMessage());
                return null;
            });
    }

    private static CompletableFuture<Void> demonstrateAsyncErrorHandling(IgniteTransactions transactions, RecordView<Tuple> artists) {
        System.out.println("\n3. Async Error Handling:");
        
        final int artistId = 8003;
        
        return transactions.beginAsync()
            .thenCompose(tx -> {
                System.out.println("   >>> Starting transaction with potential error...");
                
                // Insert valid data
                Tuple newArtist = Tuple.create()
                    .set("ArtistId", artistId)
                    .set("Name", "Error Test Artist");
                
                return artists.upsertAsync(tx, newArtist)
                    .thenCompose(ignored -> {
                        // Simulate an error condition
                        System.out.println("   !!! Simulating error condition...");
                        CompletableFuture<Void> errorFuture = new CompletableFuture<>();
                        errorFuture.completeExceptionally(new RuntimeException("Simulated async error"));
                        return errorFuture;
                    })
                    .thenApply(ignored -> tx);
            })
            .handle((tx, throwable) -> {
                if (throwable != null) {
                    System.out.println("   !!! Error detected, rolling back transaction...");
                    try {
                        if (tx != null) {
                            tx.rollback();
                        }
                        System.out.println("   <<< Transaction rolled back due to error");
                        return CompletableFuture.completedFuture("rollback_success");
                    } catch (Exception rollbackError) {
                        logger.error("Rollback failed", rollbackError);
                        return CompletableFuture.completedFuture("rollback_failed");
                    }
                } else {
                    System.out.println("   <<< Transaction completed successfully");
                    return CompletableFuture.completedFuture("success");
                }
            })
            .thenCompose(resultFuture -> resultFuture)
            .thenCompose(result -> {
                // Verify rollback worked
                Tuple key = Tuple.create().set("ArtistId", artistId);
                return artists.getAsync(null, key)
                    .thenAccept(artist -> {
                        if (artist == null) {
                            System.out.println("   <<< Verified: Transaction was properly rolled back");
                        } else {
                            System.out.println("   !!! Unexpected: Data found after rollback");
                        }
                    });
            })
            .thenCompose(ignored -> {
                // Final cleanup
                return artists.deleteAsync(null, Tuple.create().set("ArtistId", 8001));
            });
    }
}