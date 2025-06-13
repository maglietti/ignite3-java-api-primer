package com.apache.ignite.examples.transactions;

import com.apache.ignite.client.IgniteClient;
import com.apache.ignite.examples.setup.model.Artist;
import com.apache.ignite.examples.setup.model.Album;
import com.apache.ignite.examples.setup.util.DataSetupUtils;
import com.apache.ignite.table.RecordView;
import com.apache.ignite.tx.TransactionOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Demonstrates asynchronous transaction patterns with the Ignite 3 Transaction API.
 * 
 * This application shows advanced async transaction usage including:
 * - Async transaction beginning and completion
 * - Chaining async operations within transactions
 * - Parallel operations in transactions
 * - Async functional transactions with runInTransactionAsync()
 * - Error handling and timeout patterns
 * - CompletableFuture composition patterns
 * 
 * Uses music store sample data for realistic async scenarios.
 */
public class AsyncTransactionDemo {
    private static final Logger logger = LoggerFactory.getLogger(AsyncTransactionDemo.class);
    
    public static void main(String[] args) {
        logger.info("Starting AsyncTransactionDemo");
        
        try (IgniteClient client = DataSetupUtils.getClient()) {
            AsyncTransactionDemo demo = new AsyncTransactionDemo();
            
            // Demonstrate different async transaction patterns
            demo.basicAsyncTransactionExample(client);
            demo.chainedAsyncOperationsExample(client);
            demo.parallelAsyncOperationsExample(client);
            demo.asyncFunctionalTransactionExample(client);
            demo.asyncErrorHandlingExample(client);
            demo.asyncTimeoutExample(client);
            
            logger.info("AsyncTransactionDemo completed successfully");
            
        } catch (Exception e) {
            logger.error("AsyncTransactionDemo failed", e);
        }
    }
    
    /**
     * Demonstrates basic async transaction pattern with beginAsync().
     * Shows the fundamental async transaction lifecycle.
     */
    public void basicAsyncTransactionExample(IgniteClient client) {
        logger.info("=== Basic Async Transaction Example ===");
        
        CompletableFuture<Void> future = client.transactions().beginAsync()
            .thenCompose(tx -> {
                logger.info("Async transaction started");
                
                RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
                Artist key = createArtistKey(2000);
                
                return artistView.getAsync(tx, key)
                    .thenCompose(artist -> {
                        if (artist != null) {
                            artist.setName(artist.getName() + " (Async Updated)");
                            return artistView.upsertAsync(tx, artist);
                        } else {
                            // Create new artist if not found
                            Artist newArtist = new Artist();
                            newArtist.setArtistId(2000);
                            newArtist.setName("New Async Artist");
                            return artistView.upsertAsync(tx, newArtist);
                        }
                    })
                    .thenCompose(ignored -> {
                        logger.info("Artist operation completed");
                        return tx.commitAsync();
                    })
                    .exceptionally(throwable -> {
                        logger.error("Async transaction failed", throwable);
                        tx.rollbackAsync();
                        throw new RuntimeException(throwable);
                    });
            });
        
        // Wait for completion
        future.join();
        logger.info("Basic async transaction completed");
    }
    
    /**
     * Demonstrates chaining multiple async operations within a transaction.
     * Shows how to compose sequential async operations with proper error handling.
     */
    public void chainedAsyncOperationsExample(IgniteClient client) {
        logger.info("=== Chained Async Operations Example ===");
        
        CompletableFuture<String> result = client.transactions().beginAsync()
            .thenCompose(tx -> {
                RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
                RecordView<Album> albumView = client.tables().table("Album").recordView(Album.class);
                
                // Chain 1: Create artist
                Artist artist = new Artist();
                artist.setArtistId(2001);
                artist.setName("Chained Artist");
                
                return artistView.upsertAsync(tx, artist)
                    .thenCompose(ignored -> {
                        logger.info("Artist created: {}", artist.getName());
                        
                        // Chain 2: Create album
                        Album album = new Album();
                        album.setAlbumId(3000);
                        album.setTitle("Chained Album");
                        album.setArtistId(2001);
                        
                        return albumView.upsertAsync(tx, album);
                    })
                    .thenCompose(ignored -> {
                        logger.info("Album created: Chained Album");
                        
                        // Chain 3: Verify data
                        return artistView.getAsync(tx, createArtistKey(2001));
                    })
                    .thenCompose(retrievedArtist -> {
                        if (retrievedArtist != null) {
                            logger.info("Verified artist exists: {}", retrievedArtist.getName());
                            return tx.commitAsync()
                                .thenApply(ignored -> "Chained operations completed successfully");
                        } else {
                            return tx.rollbackAsync()
                                .thenApply(ignored -> {
                                    throw new RuntimeException("Artist verification failed");
                                });
                        }
                    })
                    .exceptionally(throwable -> {
                        logger.error("Chained operations failed", throwable);
                        tx.rollbackAsync();
                        return "Chained operations failed: " + throwable.getMessage();
                    });
            });
        
        String message = result.join();
        logger.info("Chained operations result: {}", message);
    }
    
    /**
     * Demonstrates parallel async operations within a transaction.
     * Shows how to execute multiple independent operations concurrently.
     */
    public void parallelAsyncOperationsExample(IgniteClient client) {
        logger.info("=== Parallel Async Operations Example ===");
        
        CompletableFuture<Void> parallelOps = client.transactions().beginAsync()
            .thenCompose(tx -> {
                RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
                
                // Create multiple artists in parallel
                List<CompletableFuture<Void>> operations = Arrays.asList(
                    artistView.upsertAsync(tx, createArtist(2002, "Parallel Artist 1")),
                    artistView.upsertAsync(tx, createArtist(2003, "Parallel Artist 2")),
                    artistView.upsertAsync(tx, createArtist(2004, "Parallel Artist 3")),
                    artistView.upsertAsync(tx, createArtist(2005, "Parallel Artist 4"))
                );
                
                logger.info("Starting {} parallel operations", operations.size());
                
                // Wait for all parallel operations to complete
                return CompletableFuture.allOf(operations.toArray(new CompletableFuture[0]))
                    .thenCompose(ignored -> {
                        logger.info("All parallel operations completed");
                        return tx.commitAsync();
                    })
                    .exceptionally(throwable -> {
                        logger.error("Parallel operations failed", throwable);
                        tx.rollbackAsync();
                        throw new RuntimeException(throwable);
                    });
            });
        
        parallelOps.join();
        logger.info("Parallel async operations completed");
    }
    
    /**
     * Demonstrates async functional transactions using runInTransactionAsync().
     * Shows the async equivalent of runInTransaction() with CompletableFuture return.
     */
    public void asyncFunctionalTransactionExample(IgniteClient client) {
        logger.info("=== Async Functional Transaction Example ===");
        
        CompletableFuture<String> result = client.transactions().runInTransactionAsync(tx -> {
            RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
            
            return artistView.getAsync(tx, createArtistKey(1))
                .thenCompose(artist -> {
                    if (artist != null) {
                        artist.setName(artist.getName() + " (Async Functional)");
                        return artistView.upsertAsync(tx, artist)
                            .thenApply(ignored -> "Updated artist: " + artist.getName());
                    } else {
                        return CompletableFuture.completedFuture("Artist not found");
                    }
                });
        });
        
        String message = result.join();
        logger.info("Async functional transaction result: {}", message);
    }
    
    /**
     * Demonstrates error handling patterns in async transactions.
     * Shows how to handle exceptions and rollback in async contexts.
     */
    public void asyncErrorHandlingExample(IgniteClient client) {
        logger.info("=== Async Error Handling Example ===");
        
        CompletableFuture<String> errorHandling = client.transactions().beginAsync()
            .thenCompose(tx -> {
                RecordView<Artist> view = client.tables().table("Artist").recordView(Artist.class);
                
                return view.getAsync(tx, createArtistKey(2006))
                    .thenCompose(artist -> {
                        // Simulate business validation error
                        if (artist == null) {
                            return CompletableFuture.failedFuture(
                                new RuntimeException("Artist not found for async error handling test"));
                        }
                        
                        artist.setName(artist.getName() + " (Error Test)");
                        return view.upsertAsync(tx, artist);
                    })
                    .thenCompose(ignored -> tx.commitAsync())
                    .thenApply(ignored -> "Async error handling completed successfully")
                    .exceptionally(throwable -> {
                        logger.warn("Expected error occurred: {}", throwable.getMessage());
                        tx.rollbackAsync();
                        return "Async transaction rolled back due to: " + throwable.getMessage();
                    });
            });
        
        String result = errorHandling.join();
        logger.info("Async error handling result: {}", result);
    }
    
    /**
     * Demonstrates timeout handling in async transactions.
     * Shows how to configure and handle timeouts at different levels.
     */
    public void asyncTimeoutExample(IgniteClient client) {
        logger.info("=== Async Timeout Example ===");
        
        // Transaction with timeout configuration
        TransactionOptions options = new TransactionOptions()
            .timeoutMillis(10000)  // 10 second transaction timeout
            .readOnly(false);
        
        CompletableFuture<String> timedTransaction = client.transactions().beginAsync(options)
            .thenCompose(tx -> {
                RecordView<Artist> view = client.tables().table("Artist").recordView(Artist.class);
                Artist artist = createArtist(2007, "Timeout Test Artist");
                
                return view.upsertAsync(tx, artist)
                    .orTimeout(5, TimeUnit.SECONDS)  // Operation-level timeout
                    .thenCompose(ignored -> tx.commitAsync())
                    .thenApply(ignored -> "Timeout transaction completed successfully")
                    .exceptionally(throwable -> {
                        if (throwable.getCause() instanceof TimeoutException) {
                            logger.warn("Operation timed out");
                        } else {
                            logger.error("Transaction error", throwable);
                        }
                        
                        tx.rollbackAsync();
                        return "Transaction failed due to: " + throwable.getMessage();
                    });
            });
        
        String result = timedTransaction.join();
        logger.info("Timeout transaction result: {}", result);
    }
    
    /**
     * Helper method to create an Artist key object.
     */
    private Artist createArtistKey(Integer artistId) {
        Artist key = new Artist();
        key.setArtistId(artistId);
        return key;
    }
    
    /**
     * Helper method to create a complete Artist object.
     */
    private Artist createArtist(Integer artistId, String name) {
        Artist artist = new Artist();
        artist.setArtistId(artistId);
        artist.setName(name);
        return artist;
    }
}