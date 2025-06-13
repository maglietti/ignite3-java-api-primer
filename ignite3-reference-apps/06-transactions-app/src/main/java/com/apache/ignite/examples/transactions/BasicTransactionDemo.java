package com.apache.ignite.examples.transactions;

import com.apache.ignite.client.IgniteClient;
import com.apache.ignite.examples.setup.model.Artist;
import com.apache.ignite.examples.setup.model.Album;
import com.apache.ignite.examples.setup.util.DataSetupUtils;
import com.apache.ignite.table.RecordView;
import com.apache.ignite.tx.Transaction;
import com.apache.ignite.tx.TransactionOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demonstrates basic transaction patterns with the Ignite 3 Transaction API.
 * 
 * This application shows fundamental transaction usage including:
 * - Explicit transaction management (begin/commit/rollback)
 * - Closure-based transactions with runInTransaction()
 * - TransactionOptions configuration
 * - Integration with Table API and SQL API
 * - Exception handling patterns
 * 
 * Uses music store sample data for realistic examples.
 */
public class BasicTransactionDemo {
    private static final Logger logger = LoggerFactory.getLogger(BasicTransactionDemo.class);
    
    public static void main(String[] args) {
        logger.info("Starting BasicTransactionDemo");
        
        try (IgniteClient client = DataSetupUtils.getClient()) {
            BasicTransactionDemo demo = new BasicTransactionDemo();
            
            // Demonstrate different transaction patterns
            demo.explicitTransactionExample(client);
            demo.functionalTransactionExample(client);
            demo.readOnlyTransactionExample(client);
            demo.transactionWithCustomOptionsExample(client);
            demo.transactionErrorHandlingExample(client);
            
            logger.info("BasicTransactionDemo completed successfully");
            
        } catch (Exception e) {
            logger.error("BasicTransactionDemo failed", e);
        }
    }
    
    /**
     * Demonstrates explicit transaction management with begin/commit/rollback.
     * This pattern provides full control over transaction lifecycle.
     */
    public void explicitTransactionExample(IgniteClient client) {
        logger.info("=== Explicit Transaction Example ===");
        
        Transaction tx = null;
        try {
            // Begin transaction with custom options
            TransactionOptions options = new TransactionOptions()
                .timeoutMillis(10000)
                .readOnly(false);
            
            tx = client.transactions().begin(options);
            logger.info("Transaction started");
            
            // Use transaction with Table API
            RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
            
            Artist artist = new Artist();
            artist.setArtistId(1000);
            artist.setName("Demo Artist (Explicit)");
            
            artistView.upsert(tx, artist);
            logger.info("Inserted artist: {}", artist.getName());
            
            // Use transaction with SQL API
            client.sql().execute(tx, 
                "INSERT INTO Album (AlbumId, Title, ArtistId) VALUES (?, ?, ?)",
                2000, "Demo Album (Explicit)", 1000);
            
            logger.info("Inserted album via SQL");
            
            // Commit transaction
            tx.commit();
            logger.info("Transaction committed successfully");
            
        } catch (Exception e) {
            logger.error("Transaction failed", e);
            if (tx != null) {
                try {
                    tx.rollback();
                    logger.info("Transaction rolled back");
                } catch (Exception rollbackError) {
                    logger.error("Rollback failed", rollbackError);
                }
            }
        }
    }
    
    /**
     * Demonstrates closure-based transactions using runInTransaction().
     * This pattern handles commit/rollback automatically based on return value.
     */
    public void functionalTransactionExample(IgniteClient client) {
        logger.info("=== Functional Transaction Example ===");
        
        boolean success = client.transactions().runInTransaction(tx -> {
            try {
                RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
                RecordView<Album> albumView = client.tables().table("Album").recordView(Album.class);
                
                // Create artist
                Artist artist = new Artist();
                artist.setArtistId(1001);
                artist.setName("Demo Artist (Functional)");
                artistView.upsert(tx, artist);
                
                // Create related album
                Album album = new Album();
                album.setAlbumId(2001);
                album.setTitle("Demo Album (Functional)");
                album.setArtistId(1001);
                albumView.upsert(tx, album);
                
                logger.info("Created artist '{}' and album '{}'", 
                    artist.getName(), album.getTitle());
                
                return true; // Commit transaction
                
            } catch (Exception e) {
                logger.error("Error in functional transaction", e);
                return false; // Rollback transaction
            }
        });
        
        if (success) {
            logger.info("Functional transaction completed successfully");
        } else {
            logger.warn("Functional transaction was rolled back");
        }
    }
    
    /**
     * Demonstrates read-only transactions for query operations.
     * Read-only transactions provide better performance for queries.
     */
    public void readOnlyTransactionExample(IgniteClient client) {
        logger.info("=== Read-Only Transaction Example ===");
        
        TransactionOptions readOnlyOptions = new TransactionOptions()
            .readOnly(true)
            .timeoutMillis(30000); // Longer timeout for reporting
        
        client.transactions().runInTransaction(readOnlyOptions, tx -> {
            // Multiple read operations within single transaction
            var artistResult = client.sql().execute(tx, 
                "SELECT COUNT(*) as count FROM Artist");
            long artistCount = artistResult.next().longValue("count");
            
            var albumResult = client.sql().execute(tx, 
                "SELECT COUNT(*) as count FROM Album");
            long albumCount = albumResult.next().longValue("count");
            
            logger.info("Database statistics - Artists: {}, Albums: {}", 
                artistCount, albumCount);
            
            return true;
        });
    }
    
    /**
     * Demonstrates various TransactionOptions configurations.
     * Shows how to customize timeout, read-only mode, and other settings.
     */
    public void transactionWithCustomOptionsExample(IgniteClient client) {
        logger.info("=== Custom Transaction Options Example ===");
        
        // Quick operation with short timeout
        TransactionOptions quickOptions = new TransactionOptions()
            .timeoutMillis(5000)
            .readOnly(false);
        
        client.transactions().runInTransaction(quickOptions, tx -> {
            RecordView<Artist> view = client.tables().table("Artist").recordView(Artist.class);
            
            Artist artist = view.get(tx, createArtistKey(1));
            if (artist != null) {
                artist.setName(artist.getName() + " (Quick Update)");
                view.upsert(tx, artist);
                logger.info("Quick update completed for artist: {}", artist.getName());
            }
            
            return true;
        });
        
        // Complex operation with longer timeout
        TransactionOptions complexOptions = new TransactionOptions()
            .timeoutMillis(60000)
            .readOnly(false);
        
        client.transactions().runInTransaction(complexOptions, tx -> {
            // Simulate complex business logic
            logger.info("Performing complex operation with extended timeout");
            
            // Complex multi-table operations would go here
            performComplexOperations(client, tx);
            
            return true;
        });
    }
    
    /**
     * Demonstrates comprehensive error handling patterns.
     * Shows handling of different exception types and rollback scenarios.
     */
    public void transactionErrorHandlingExample(IgniteClient client) {
        logger.info("=== Error Handling Example ===");
        
        // Example 1: Handle validation errors
        boolean result = client.transactions().runInTransaction(tx -> {
            try {
                RecordView<Artist> view = client.tables().table("Artist").recordView(Artist.class);
                
                Artist artist = new Artist();
                artist.setArtistId(1002);
                artist.setName(""); // Invalid name
                
                // Business validation
                if (artist.getName() == null || artist.getName().trim().isEmpty()) {
                    logger.warn("Artist name validation failed");
                    return false; // Rollback
                }
                
                view.upsert(tx, artist);
                return true; // Commit
                
            } catch (Exception e) {
                logger.error("Unexpected error in transaction", e);
                return false; // Rollback
            }
        });
        
        logger.info("Validation transaction result: {}", result ? "SUCCESS" : "FAILED");
        
        // Example 2: Try-catch with explicit transaction
        Transaction tx = null;
        try {
            tx = client.transactions().begin();
            
            RecordView<Artist> view = client.tables().table("Artist").recordView(Artist.class);
            
            Artist artist = new Artist();
            artist.setArtistId(1003);
            artist.setName("Valid Artist");
            view.upsert(tx, artist);
            
            // Simulate potential failure
            if (System.currentTimeMillis() % 2 == 0) {
                throw new RuntimeException("Simulated business error");
            }
            
            tx.commit();
            logger.info("Explicit transaction committed");
            
        } catch (RuntimeException e) {
            logger.warn("Business logic error: {}", e.getMessage());
            if (tx != null) {
                tx.rollback();
                logger.info("Transaction rolled back due to business error");
            }
        } catch (Exception e) {
            logger.error("System error in transaction", e);
            if (tx != null) {
                tx.rollback();
                logger.info("Transaction rolled back due to system error");
            }
        }
    }
    
    /**
     * Creates an Artist key object for lookups.
     * Only the primary key field (ArtistId) needs to be set.
     */
    private Artist createArtistKey(Integer artistId) {
        Artist key = new Artist();
        key.setArtistId(artistId);
        return key;
    }
    
    /**
     * Simulates complex business operations that might require longer timeouts.
     * In real applications, this could involve multiple table operations,
     * external service calls, or complex calculations.
     */
    private void performComplexOperations(IgniteClient client, Transaction tx) {
        // Simulate processing time
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        logger.info("Complex operations completed");
    }
}