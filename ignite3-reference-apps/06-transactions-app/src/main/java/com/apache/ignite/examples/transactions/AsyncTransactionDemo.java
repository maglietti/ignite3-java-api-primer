package com.apache.ignite.examples.transactions;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.tx.TransactionOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Demonstrates asynchronous transaction patterns following the documentation narrative.
 * 
 * For high-throughput music store applications, asynchronous transactions prevent 
 * blocking threads while operations complete across the distributed cluster.
 * 
 * This application shows:
 * - Basic async patterns with CompletableFuture chains
 * - Parallel operations within transactions
 * - Async functional transactions with runInTransactionAsync()
 * - Proper error handling in async contexts
 * - Timeout management for async operations
 */
public class AsyncTransactionDemo {
    private static final Logger logger = LoggerFactory.getLogger(AsyncTransactionDemo.class);
    
    public static void main(String[] args) {
        logger.info("🚀 Starting Async Transaction Demo - Non-Blocking Music Store Operations");
        logger.info("=======================================================================");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800")
                .build()) {
            
            AsyncTransactionDemo demo = new AsyncTransactionDemo();
            
            // Example 1: Basic Async Pattern (from docs)
            logger.info("\n📖 Example 1: Basic Async Pattern");
            demo.createArtistAsync(client, "The Beatles").join();
            
            // Example 2: Parallel Operations (from docs)
            logger.info("\n📖 Example 2: Parallel Operations");
            demo.createMultipleArtistsAsync(client, Arrays.asList(
                "Queen", "Led Zeppelin", "Pink Floyd", "The Rolling Stones"
            )).join();
            
            // Example 3: Async Error Handling (from docs)
            logger.info("\n📖 Example 3: Async Error Handling");
            demo.safeAsyncOperation(client).join();
            
            // Example 4: Timeout Management (from docs)
            logger.info("\n📖 Example 4: Timeout Management");
            demo.asyncWithTimeout(client).join();
            
            logger.info("\n✅ Async Transaction Demo completed successfully!");
            
        } catch (Exception e) {
            logger.error("❌ Async Transaction Demo failed", e);
        }
    }
    
    /**
     * Demonstrates asynchronous transaction patterns for non-blocking operations.
     * Async transactions are crucial for high-throughput applications that need
     * to handle many concurrent operations efficiently.
     */
    public CompletableFuture<Void> createArtistAsync(IgniteClient client, String artistName) {
        logger.info("🚀 Starting async transaction for: {}", artistName);
        
        return client.transactions().beginAsync()
            .thenCompose(tx -> {
                RecordView<Tuple> artistTable = client.tables().table("Artist").recordView();
                
                Tuple artist = Tuple.create()
                    .set("ArtistId", generateArtistId())
                    .set("Name", artistName);
                
                return artistTable.upsertAsync(tx, artist)
                    .thenCompose(ignored -> {
                        logger.info("✅ Artist created: {}", artistName);
                        return tx.commitAsync();
                    })
                    .exceptionally(throwable -> {
                        logger.error("❌ Failed to create artist: " + throwable.getMessage());
                        tx.rollbackAsync();
                        throw new RuntimeException(throwable);
                    });
            });
    }
    
    /**
     * Demonstrates creating multiple artists in parallel within the same transaction.
     * Shows the power of async operations for bulk operations that can be parallelized.
     */
    public CompletableFuture<String> createMultipleArtistsAsync(IgniteClient client, List<String> artistNames) {
        logger.info("Creating {} artists in parallel using async transaction", artistNames.size());
        
        return client.transactions().runInTransactionAsync(tx -> {
            RecordView<Tuple> artistTable = client.tables().table("Artist").recordView();
            
            // Create all artists in parallel within the same transaction
            List<CompletableFuture<Void>> operations = artistNames.stream()
                .map(name -> {
                    Tuple artist = Tuple.create()
                        .set("ArtistId", generateArtistId())
                        .set("Name", name);
                    logger.info("🎤 Scheduling creation of artist: {}", name);
                    return artistTable.upsertAsync(tx, artist);
                })
                .collect(Collectors.toList());
            
            // Wait for all operations to complete
            return CompletableFuture.allOf(operations.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> {
                    logger.info("✅ Created {} artists in parallel", artistNames.size());
                    return "Success: " + artistNames.size() + " artists created";
                });
        });
    }
    
    /**
     * Demonstrates proper error handling in asynchronous transactions.
     * Error handling is crucial in async operations as exceptions
     * propagate through CompletableFuture chains.
     */
    public CompletableFuture<String> safeAsyncOperation(IgniteClient client) {
        logger.info("Demonstrating safe async operation with error handling...");
        
        return client.transactions().beginAsync()
            .thenCompose(tx -> {
                RecordView<Tuple> artistTable = client.tables().table("Artist").recordView();
                
                return artistTable.getAsync(tx, Tuple.create().set("ArtistId", 999))
                    .thenCompose(artist -> {
                        if (artist != null) {
                            // Update existing artist
                            logger.info("📝 Updating existing artist");
                            Tuple updated = artist.set("Name", artist.stringValue("Name") + " (Async Updated)");
                            return artistTable.upsertAsync(tx, updated);
                        } else {
                            // Create new artist
                            logger.info("➕ Creating new artist");
                            Tuple newArtist = Tuple.create()
                                .set("ArtistId", 999)
                                .set("Name", "Async Demo Artist");
                            return artistTable.upsertAsync(tx, newArtist);
                        }
                    })
                    .thenCompose(ignored -> tx.commitAsync())
                    .thenApply(ignored -> {
                        logger.info("✅ Async operation completed successfully");
                        return "Operation completed successfully";
                    })
                    .exceptionally(throwable -> {
                        // Handle any errors in the chain
                        logger.warn("⚠️ Transaction error: " + throwable.getMessage());
                        tx.rollbackAsync();
                        return "Operation failed: " + throwable.getMessage();
                    });
            })
            .exceptionally(throwable -> {
                // Handle transaction creation errors
                logger.error("❌ Failed to begin transaction: " + throwable.getMessage());
                return "Failed to begin transaction: " + throwable.getMessage();
            });
    }
    
    /**
     * Demonstrates timeout handling in asynchronous transactions.
     * Shows how to configure and handle timeouts at different levels.
     */
    public CompletableFuture<String> asyncWithTimeout(IgniteClient client) {
        logger.info("Demonstrating async transaction with timeout handling...");
        
        // Transaction with timeout configuration
        TransactionOptions options = new TransactionOptions()
            .timeoutMillis(10000)  // 10 second transaction timeout
            .readOnly(false);
        
        return client.transactions().beginAsync(options)
            .thenCompose(tx -> {
                RecordView<Tuple> artistTable = client.tables().table("Artist").recordView();
                Tuple artist = Tuple.create()
                    .set("ArtistId", generateArtistId())
                    .set("Name", "Timeout Test Artist");
                
                return artistTable.upsertAsync(tx, artist)
                    .orTimeout(5, TimeUnit.SECONDS)  // Operation-level timeout
                    .thenCompose(ignored -> {
                        logger.info("🎤 Artist created within timeout");
                        return tx.commitAsync();
                    })
                    .thenApply(ignored -> {
                        logger.info("✅ Timeout transaction completed successfully");
                        return "Timeout transaction completed successfully";
                    })
                    .exceptionally(throwable -> {
                        if (throwable.getCause() instanceof TimeoutException) {
                            logger.warn("⏰ Operation timed out");
                        } else {
                            logger.error("❌ Transaction error: " + throwable.getMessage());
                        }
                        
                        tx.rollbackAsync();
                        return "Transaction failed due to: " + throwable.getMessage();
                    });
            });
    }
    
    /**
     * Helper method to generate unique artist IDs for demo purposes.
     */
    private Integer generateArtistId() {
        return (int) (System.currentTimeMillis() % 100000);
    }
}