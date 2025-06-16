package com.apache.ignite.examples.transactions;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.tx.Transaction;
import org.apache.ignite.tx.TransactionOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Demonstrates basic transaction patterns following the narrative from the documentation.
 * 
 * This application mirrors the examples from the documentation, showing:
 * - Your first transaction: Artist and Album creation
 * - The runInTransaction pattern for automatic management
 * - Real-world customer purchase workflow
 * - TransactionOptions for different scenarios
 * 
 * Each example tells the story of ensuring data consistency in music store operations.
 */
public class BasicTransactionDemo {
    private static final Logger logger = LoggerFactory.getLogger(BasicTransactionDemo.class);
    
    public static void main(String[] args) {
        logger.info("🎵 Starting Basic Transaction Demo - Music Store Operations");
        logger.info("=============================================================");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800")
                .build()) {
            
            BasicTransactionDemo demo = new BasicTransactionDemo();
            
            // Example 1: Your First Transaction (from docs)
            logger.info("\n📖 Example 1: Your First Transaction");
            demo.createArtistAndAlbumExplicit(client);
            
            // Example 2: The runInTransaction Pattern (from docs)  
            logger.info("\n📖 Example 2: The runInTransaction Pattern");
            demo.createArtistAndAlbumAutomatic(client);
            
            // Example 3: Customer Purchase Workflow (from docs)
            logger.info("\n📖 Example 3: Customer Purchase Workflow");
            demo.processPurchase(client, 1, java.util.List.of(1, 2, 3));
            
            // Example 4: Transaction Options (from docs)
            logger.info("\n📖 Example 4: Transaction Options");
            demo.demonstrateTransactionOptions(client);
            
            logger.info("\n✅ Basic Transaction Demo completed successfully!");
            
        } catch (Exception e) {
            logger.error("❌ Basic Transaction Demo failed", e);
        }
    }
    
    /**
     * Demonstrates the fundamental transaction lifecycle from the documentation.
     * Creates a new artist and their debut album atomically.
     * If creating the album fails, we don't want an orphaned artist record.
     */
    public void createArtistAndAlbumExplicit(IgniteClient client) {
        logger.info("Creating Arctic Monkeys and their album 'AM' using explicit transaction...");
        
        // Step 1: Begin a transaction
        Transaction tx = client.transactions().begin();
        
        try {
            // Step 2: Get table views within the transaction
            RecordView<Tuple> artistTable = client.tables().table("Artist").recordView();
            RecordView<Tuple> albumTable = client.tables().table("Album").recordView();
            
            // Step 3: Create artist record
            Tuple artist = Tuple.create()
                .set("ArtistId", 1000)
                .set("Name", "Arctic Monkeys");
            artistTable.upsert(tx, artist);
            logger.info("🎤 Created artist: Arctic Monkeys");
            
            // Step 4: Create album record (linked to artist)
            Tuple album = Tuple.create()
                .set("AlbumId", 2000)
                .set("Title", "AM")
                .set("ArtistId", 1000);  // Foreign key relationship
            albumTable.upsert(tx, album);
            logger.info("💿 Created album: AM");
            
            // Step 5: Commit the transaction
            tx.commit();
            logger.info("✅ Artist and album created successfully");
            
        } catch (Exception e) {
            // Step 6: Rollback on any error
            tx.rollback();
            logger.error("❌ Transaction failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Demonstrates automatic transaction management using runInTransaction().
     * This pattern is recommended for most use cases as it handles lifecycle
     * management and provides cleaner code structure.
     */
    public void createArtistAndAlbumAutomatic(IgniteClient client) {
        logger.info("Creating Radiohead and their album 'OK Computer' using runInTransaction...");
        
        boolean success = client.transactions().runInTransaction(tx -> {
            try {
                RecordView<Tuple> artistTable = client.tables().table("Artist").recordView();
                RecordView<Tuple> albumTable = client.tables().table("Album").recordView();
                
                // Create artist
                Tuple artist = Tuple.create()
                    .set("ArtistId", 1001)
                    .set("Name", "Radiohead");
                artistTable.upsert(tx, artist);
                logger.info("🎤 Created artist: Radiohead");
                
                // Create album
                Tuple album = Tuple.create()
                    .set("AlbumId", 2001)
                    .set("Title", "OK Computer")
                    .set("ArtistId", 1001);
                albumTable.upsert(tx, album);
                logger.info("💿 Created album: OK Computer");
                
                // Return true to commit
                return true;
                
            } catch (Exception e) {
                logger.error("Error in transaction: " + e.getMessage());
                // Return false to rollback
                return false;
            }
        });
        
        if (success) {
            logger.info("✅ Transaction completed successfully");
        } else {
            logger.info("❌ Transaction was rolled back");
        }
    }
    
    /**
     * Demonstrates a realistic business workflow requiring transactions.
     * This example shows why ACID properties are crucial for maintaining
     * data consistency in multi-table operations.
     * 
     * When a customer buys tracks, multiple things must happen atomically:
     * 1. Create invoice with customer information
     * 2. Add line items for each purchased track  
     * 3. Calculate and set the total amount
     */
    public Integer processPurchase(IgniteClient client, Integer customerId, java.util.List<Integer> trackIds) {
        logger.info("Processing purchase for customer {} with {} tracks", customerId, trackIds.size());
        
        return client.transactions().runInTransaction(tx -> {
            try {
                // Get table access
                RecordView<Tuple> invoiceTable = client.tables().table("Invoice").recordView();
                RecordView<Tuple> invoiceLineTable = client.tables().table("InvoiceLine").recordView();
                IgniteSql sql = client.sql();
                
                // Step 1: Create the invoice
                Integer invoiceId = generateInvoiceId();
                Tuple invoice = Tuple.create()
                    .set("InvoiceId", invoiceId)
                    .set("CustomerId", customerId)
                    .set("InvoiceDate", LocalDate.now())
                    .set("BillingAddress", "123 Music Street")
                    .set("BillingCity", "Harmony")
                    .set("BillingCountry", "USA")
                    .set("Total", BigDecimal.ZERO);  // Will calculate later
                
                invoiceTable.upsert(tx, invoice);
                logger.info("📝 Created invoice {}", invoiceId);
                
                // Step 2: Add line items and calculate total
                BigDecimal totalAmount = BigDecimal.ZERO;
                for (int i = 0; i < trackIds.size(); i++) {
                    Integer trackId = trackIds.get(i);
                    
                    // Get track price using SQL in the same transaction
                    ResultSet<SqlRow> trackResult = sql.execute(tx,
                        "SELECT UnitPrice FROM Track WHERE TrackId = ?", trackId);
                    
                    if (!trackResult.hasNext()) {
                        throw new IllegalArgumentException("Track not found: " + trackId);
                    }
                    
                    BigDecimal unitPrice = trackResult.next().decimalValue("UnitPrice");
                    
                    // Create line item
                    Tuple lineItem = Tuple.create()
                        .set("InvoiceLineId", generateLineItemId(i))
                        .set("InvoiceId", invoiceId)
                        .set("TrackId", trackId)
                        .set("UnitPrice", unitPrice)
                        .set("Quantity", 1);
                    
                    invoiceLineTable.upsert(tx, lineItem);
                    totalAmount = totalAmount.add(unitPrice);
                    logger.info("🎵 Added track {} (${}) to invoice", trackId, unitPrice);
                }
                
                // Step 3: Update invoice with calculated total
                invoice = invoice.set("Total", totalAmount);
                invoiceTable.upsert(tx, invoice);
                
                logger.info("💰 Invoice total: ${}", totalAmount);
                logger.info("✅ Purchase completed successfully");
                
                return invoiceId;
                
            } catch (Exception e) {
                logger.error("❌ Purchase failed: " + e.getMessage());
                throw e;  // This will trigger rollback
            }
        });
    }
    
    /**
     * Demonstrates various TransactionOptions configurations following
     * the timeout and read-only examples from the documentation.
     */
    public void demonstrateTransactionOptions(IgniteClient client) {
        // Quick update with short timeout
        logger.info("Performing quick update with short timeout...");
        
        TransactionOptions quickOptions = new TransactionOptions()
            .timeoutMillis(5000)  // 5 seconds
            .readOnly(false);
        
        client.transactions().runInTransaction(tx -> {
            RecordView<Tuple> artistTable = client.tables().table("Artist").recordView();
            
            Tuple artist = artistTable.get(tx, Tuple.create().set("ArtistId", 1));
            if (artist != null) {
                artist = artist.set("Name", artist.stringValue("Name") + " (Updated)");
                artistTable.upsert(tx, artist);
                logger.info("⚡ Quick update completed");
            }
            return true;
        });
        
        // Read-only transaction for reporting
        logger.info("Generating statistics report with read-only transaction...");
        
        TransactionOptions reportOptions = new TransactionOptions()
            .timeoutMillis(60000)  // 60 seconds
            .readOnly(true);       // Read-only for better performance
        
        client.transactions().runInTransaction(tx -> {
            IgniteSql sql = client.sql();
            
            // Complex multi-table analysis
            ResultSet<SqlRow> stats = sql.execute(tx, """
                SELECT 
                    COUNT(DISTINCT a.ArtistId) as artist_count,
                    COUNT(DISTINCT al.AlbumId) as album_count,
                    COUNT(DISTINCT t.TrackId) as track_count,
                    AVG(t.UnitPrice) as avg_price
                FROM Artist a
                JOIN Album al ON a.ArtistId = al.ArtistId
                JOIN Track t ON al.AlbumId = t.AlbumId
                """);
            
            if (stats.hasNext()) {
                SqlRow row = stats.next();
                logger.info("📊 Music Store Statistics:");
                logger.info("   Artists: {}", row.longValue("artist_count"));
                logger.info("   Albums: {}", row.longValue("album_count"));
                logger.info("   Tracks: {}", row.longValue("track_count"));
                logger.info("   Average Price: ${}", String.format("%.2f", row.decimalValue("avg_price")));
            }
            
            return true;
        });
    }
    
    // Helper methods
    private Integer generateInvoiceId() {
        return (int) (System.currentTimeMillis() % 100000);
    }
    
    private Integer generateLineItemId(int index) {
        return (int) ((System.currentTimeMillis() + index) % 100000);
    }
}