package com.apache.ignite.examples.transactions;

import com.apache.ignite.client.IgniteClient;
import com.apache.ignite.examples.setup.model.*;
import com.apache.ignite.examples.setup.util.DataSetupUtils;
import com.apache.ignite.sql.ResultSet;
import com.apache.ignite.sql.SqlRow;
import com.apache.ignite.table.RecordView;
import com.apache.ignite.tx.Transaction;
import com.apache.ignite.tx.TransactionException;
import com.apache.ignite.tx.TransactionOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates advanced transaction patterns and real-world scenarios with Ignite 3.
 * 
 * This application shows practical transaction usage patterns including:
 * - Mixing Table API and SQL API within transactions
 * - Batch operations with transaction management
 * - Complex business workflows with multiple entities
 * - Transaction state management and monitoring
 * - Performance optimization patterns
 * - Real-world error handling scenarios
 * 
 * Uses complete music store data model with hierarchical relationships.
 */
public class TransactionPatterns {
    private static final Logger logger = LoggerFactory.getLogger(TransactionPatterns.class);
    
    public static void main(String[] args) {
        logger.info("Starting TransactionPatterns demonstration");
        
        try (IgniteClient client = DataSetupUtils.getClient()) {
            TransactionPatterns patterns = new TransactionPatterns();
            
            // Demonstrate advanced transaction patterns
            patterns.mixedApiTransactionExample(client);
            patterns.batchOperationsExample(client);
            patterns.businessWorkflowExample(client);
            patterns.transactionStateExample(client);
            patterns.performanceOptimizationExample(client);
            patterns.complexErrorHandlingExample(client);
            
            logger.info("TransactionPatterns demonstration completed successfully");
            
        } catch (Exception e) {
            logger.error("TransactionPatterns demonstration failed", e);
        }
    }
    
    /**
     * Demonstrates mixing Table API and SQL API within the same transaction.
     * Shows how both APIs can work together seamlessly using the same transaction context.
     */
    public void mixedApiTransactionExample(IgniteClient client) {
        logger.info("=== Mixed API Transaction Example ===");
        
        client.transactions().runInTransaction(tx -> {
            try {
                // Use Table API for object-oriented operations
                RecordView<Customer> customerView = client.tables().table("Customer").recordView(Customer.class);
                
                Customer customer = customerView.get(tx, createCustomerKey(1));
                if (customer != null) {
                    customer.setCity("New York (Updated)");
                    customerView.upsert(tx, customer);
                    logger.info("Updated customer via Table API: {}", customer.getFirstName());
                }
                
                // Use SQL API for bulk operations in the same transaction
                client.sql().execute(tx,
                    "UPDATE Invoice SET BillingCity = ? WHERE CustomerId = ?",
                    customer.getCity(), customer.getCustomerId());
                
                logger.info("Updated invoices via SQL API for customer {}", customer.getCustomerId());
                
                // Query results using SQL API
                ResultSet<SqlRow> invoiceResult = client.sql().execute(tx,
                    "SELECT COUNT(*) as invoice_count FROM Invoice WHERE CustomerId = ?",
                    customer.getCustomerId());
                
                if (invoiceResult.hasNext()) {
                    long invoiceCount = invoiceResult.next().longValue("invoice_count");
                    logger.info("Customer {} has {} invoices", customer.getCustomerId(), invoiceCount);
                }
                
                return true;
                
            } catch (Exception e) {
                logger.error("Mixed API transaction failed", e);
                return false;
            }
        });
    }
    
    /**
     * Demonstrates efficient batch operations within transactions.
     * Shows how to process multiple records efficiently while maintaining data consistency.
     */
    public void batchOperationsExample(IgniteClient client) {
        logger.info("=== Batch Operations Example ===");
        
        // Batch update track prices with transaction
        TransactionOptions batchOptions = new TransactionOptions()
            .timeoutMillis(60000)  // Longer timeout for batch operations
            .readOnly(false);
        
        boolean success = client.transactions().runInTransaction(batchOptions, tx -> {
            try {
                RecordView<Track> trackView = client.tables().table("Track").recordView(Track.class);
                
                // Get tracks for a specific album
                ResultSet<SqlRow> trackResults = client.sql().execute(tx,
                    "SELECT TrackId FROM Track WHERE AlbumId = ? LIMIT 5", 1);
                
                List<Integer> trackIds = new ArrayList<>();
                while (trackResults.hasNext()) {
                    trackIds.add(trackResults.next().intValue("TrackId"));
                }
                
                logger.info("Processing {} tracks in batch", trackIds.size());
                
                // Batch process each track
                for (Integer trackId : trackIds) {
                    Track track = trackView.get(tx, createTrackKey(trackId));
                    if (track != null) {
                        // Increase price by 10%
                        BigDecimal currentPrice = track.getUnitPrice();
                        if (currentPrice != null) {
                            BigDecimal newPrice = currentPrice.multiply(new BigDecimal("1.10"));
                            track.setUnitPrice(newPrice);
                            trackView.upsert(tx, track);
                        }
                    }
                }
                
                logger.info("Batch price update completed for {} tracks", trackIds.size());
                return true;
                
            } catch (Exception e) {
                logger.error("Batch operations failed", e);
                return false;
            }
        });
        
        logger.info("Batch operations transaction result: {}", success ? "SUCCESS" : "FAILED");
    }
    
    /**
     * Demonstrates a complex business workflow involving multiple entities.
     * Shows a realistic scenario of creating a customer order with multiple line items.
     */
    public void businessWorkflowExample(IgniteClient client) {
        logger.info("=== Business Workflow Example ===");
        
        TransactionOptions workflowOptions = new TransactionOptions()
            .timeoutMillis(30000)
            .readOnly(false);
        
        boolean orderCreated = client.transactions().runInTransaction(workflowOptions, tx -> {
            try {
                RecordView<Invoice> invoiceView = client.tables().table("Invoice").recordView(Invoice.class);
                RecordView<InvoiceLine> invoiceLineView = client.tables().table("InvoiceLine").recordView(InvoiceLine.class);
                RecordView<Track> trackView = client.tables().table("Track").recordView(Track.class);
                
                // Step 1: Create new invoice (order)
                Invoice invoice = new Invoice();
                invoice.setInvoiceId(generateInvoiceId());
                invoice.setCustomerId(1);
                invoice.setInvoiceDate(java.time.LocalDate.now());
                invoice.setBillingAddress("123 Music St");
                invoice.setBillingCity("Boston");
                invoice.setBillingCountry("USA");
                invoice.setTotal(BigDecimal.ZERO);
                
                invoiceView.upsert(tx, invoice);
                logger.info("Created invoice: {}", invoice.getInvoiceId());
                
                // Step 2: Add line items to the order
                BigDecimal totalAmount = BigDecimal.ZERO;
                int[] trackIds = {1, 2, 3}; // Sample tracks
                
                for (int i = 0; i < trackIds.length; i++) {
                    Track track = trackView.get(tx, createTrackKey(trackIds[i]));
                    if (track != null) {
                        InvoiceLine line = new InvoiceLine();
                        line.setInvoiceLineId(generateInvoiceLineId(i));
                        line.setInvoiceId(invoice.getInvoiceId());
                        line.setTrackId(track.getTrackId());
                        line.setUnitPrice(track.getUnitPrice());
                        line.setQuantity(1);
                        
                        invoiceLineView.upsert(tx, line);
                        totalAmount = totalAmount.add(track.getUnitPrice());
                        
                        logger.info("Added line item for track: {}", track.getName());
                    }
                }
                
                // Step 3: Update invoice total
                invoice.setTotal(totalAmount);
                invoiceView.upsert(tx, invoice);
                
                logger.info("Order completed - Invoice: {}, Total: ${}", 
                    invoice.getInvoiceId(), totalAmount);
                
                return true;
                
            } catch (Exception e) {
                logger.error("Business workflow failed", e);
                return false;
            }
        });
        
        logger.info("Business workflow result: {}", orderCreated ? "ORDER CREATED" : "ORDER FAILED");
    }
    
    /**
     * Demonstrates transaction state management and monitoring.
     * Shows how to query transaction properties and make decisions based on state.
     */
    public void transactionStateExample(IgniteClient client) {
        logger.info("=== Transaction State Example ===");
        
        // Read-only transaction state check
        TransactionOptions readOnlyOptions = new TransactionOptions()
            .readOnly(true)
            .timeoutMillis(15000);
        
        Transaction readOnlyTx = client.transactions().begin(readOnlyOptions);
        try {
            boolean isReadOnly = readOnlyTx.isReadOnly();
            logger.info("Transaction is read-only: {}", isReadOnly);
            
            if (isReadOnly) {
                // Only perform read operations
                ResultSet<SqlRow> result = client.sql().execute(readOnlyTx, 
                    "SELECT COUNT(*) as count FROM Artist");
                long count = result.next().longValue("count");
                logger.info("Artist count (read-only): {}", count);
            }
            
            readOnlyTx.commit();
            
        } catch (Exception e) {
            readOnlyTx.rollback();
            logger.error("Read-only transaction failed", e);
        }
        
        // Write transaction state check
        TransactionOptions writeOptions = new TransactionOptions()
            .readOnly(false)
            .timeoutMillis(10000);
        
        Transaction writeTx = client.transactions().begin(writeOptions);
        try {
            boolean isReadOnly = writeTx.isReadOnly();
            logger.info("Write transaction is read-only: {}", isReadOnly);
            
            if (!isReadOnly) {
                // Can perform write operations
                RecordView<Artist> view = client.tables().table("Artist").recordView(Artist.class);
                
                Artist artist = new Artist();
                artist.setArtistId(3000);
                artist.setName("State Test Artist");
                view.upsert(writeTx, artist);
                
                logger.info("Inserted artist in write transaction");
            }
            
            writeTx.commit();
            
        } catch (Exception e) {
            writeTx.rollback();
            logger.error("Write transaction failed", e);
        }
    }
    
    /**
     * Demonstrates performance optimization patterns for transactions.
     * Shows how to use read-only transactions and batch operations efficiently.
     */
    public void performanceOptimizationExample(IgniteClient client) {
        logger.info("=== Performance Optimization Example ===");
        
        // Use read-only transaction for reporting
        long startTime = System.currentTimeMillis();
        
        TransactionOptions reportOptions = new TransactionOptions()
            .readOnly(true)
            .timeoutMillis(60000);
        
        client.transactions().runInTransaction(reportOptions, tx -> {
            // Multiple related queries in single read-only transaction
            ResultSet<SqlRow> artistStats = client.sql().execute(tx,
                "SELECT COUNT(*) as artist_count FROM Artist");
            
            ResultSet<SqlRow> albumStats = client.sql().execute(tx,
                "SELECT COUNT(*) as album_count FROM Album");
            
            ResultSet<SqlRow> trackStats = client.sql().execute(tx,
                "SELECT COUNT(*) as track_count FROM Track");
            
            long artists = artistStats.next().longValue("artist_count");
            long albums = albumStats.next().longValue("album_count");
            long tracks = trackStats.next().longValue("track_count");
            
            logger.info("Music Store Statistics - Artists: {}, Albums: {}, Tracks: {}", 
                artists, albums, tracks);
            
            return true;
        });
        
        long readOnlyTime = System.currentTimeMillis() - startTime;
        logger.info("Read-only transaction completed in {} ms", readOnlyTime);
        
        // Batch operations with appropriate timeout
        startTime = System.currentTimeMillis();
        
        TransactionOptions batchOptions = new TransactionOptions()
            .readOnly(false)
            .timeoutMillis(30000);
        
        client.transactions().runInTransaction(batchOptions, tx -> {
            // Use SQL for bulk operations when appropriate
            client.sql().execute(tx,
                "UPDATE Track SET Name = Name || ' (Optimized)' WHERE AlbumId = ? AND TrackId < ?",
                1, 5);
            
            logger.info("Bulk update completed via SQL");
            return true;
        });
        
        long batchTime = System.currentTimeMillis() - startTime;
        logger.info("Batch update transaction completed in {} ms", batchTime);
    }
    
    /**
     * Demonstrates complex error handling with multiple exception types.
     * Shows how to handle different failure scenarios gracefully.
     */
    public void complexErrorHandlingExample(IgniteClient client) {
        logger.info("=== Complex Error Handling Example ===");
        
        // Test different error scenarios
        testTransactionTimeout(client);
        testBusinessLogicError(client);
        testDataIntegrityError(client);
    }
    
    private void testTransactionTimeout(IgniteClient client) {
        logger.info("Testing transaction timeout handling");
        
        TransactionOptions shortTimeout = new TransactionOptions()
            .timeoutMillis(1000)  // Very short timeout
            .readOnly(false);
        
        boolean result = client.transactions().runInTransaction(shortTimeout, tx -> {
            try {
                // Simulate slow operation
                Thread.sleep(2000);
                
                RecordView<Artist> view = client.tables().table("Artist").recordView(Artist.class);
                Artist artist = new Artist();
                artist.setArtistId(4000);
                artist.setName("Timeout Test");
                view.upsert(tx, artist);
                
                return true;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                logger.warn("Expected timeout occurred: {}", e.getClass().getSimpleName());
                return false;
            }
        });
        
        logger.info("Timeout test result: {}", result ? "UNEXPECTED SUCCESS" : "EXPECTED FAILURE");
    }
    
    private void testBusinessLogicError(IgniteClient client) {
        logger.info("Testing business logic error handling");
        
        boolean result = client.transactions().runInTransaction(tx -> {
            try {
                RecordView<Artist> view = client.tables().table("Artist").recordView(Artist.class);
                
                Artist artist = new Artist();
                artist.setArtistId(4001);
                artist.setName("");  // Invalid name
                
                // Business validation
                if (artist.getName() == null || artist.getName().trim().isEmpty()) {
                    throw new IllegalArgumentException("Artist name cannot be empty");
                }
                
                view.upsert(tx, artist);
                return true;
                
            } catch (IllegalArgumentException e) {
                logger.info("Business validation caught: {}", e.getMessage());
                return false;  // Rollback
            }
        });
        
        logger.info("Business logic test result: {}", result ? "UNEXPECTED SUCCESS" : "EXPECTED ROLLBACK");
    }
    
    private void testDataIntegrityError(IgniteClient client) {
        logger.info("Testing data integrity error handling");
        
        Transaction tx = null;
        try {
            tx = client.transactions().begin();
            
            // Try to insert album with non-existent artist (should fail foreign key constraint)
            client.sql().execute(tx,
                "INSERT INTO Album (AlbumId, Title, ArtistId) VALUES (?, ?, ?)",
                9999, "Integrity Test Album", 9999);  // Non-existent artist
            
            tx.commit();
            logger.warn("Data integrity test unexpectedly succeeded");
            
        } catch (TransactionException e) {
            logger.info("Data integrity error handled: {}", e.getClass().getSimpleName());
            if (tx != null) {
                tx.rollback();
            }
        } catch (Exception e) {
            logger.info("General error handled: {}", e.getClass().getSimpleName());
            if (tx != null) {
                tx.rollback();
            }
        }
    }
    
    // Helper methods
    
    private Customer createCustomerKey(Integer customerId) {
        Customer key = new Customer();
        key.setCustomerId(customerId);
        return key;
    }
    
    private Track createTrackKey(Integer trackId) {
        Track key = new Track();
        key.setTrackId(trackId);
        return key;
    }
    
    private Integer generateInvoiceId() {
        return (int) (System.currentTimeMillis() % 100000);
    }
    
    private Integer generateInvoiceLineId(int index) {
        return (int) ((System.currentTimeMillis() + index) % 100000);
    }
}