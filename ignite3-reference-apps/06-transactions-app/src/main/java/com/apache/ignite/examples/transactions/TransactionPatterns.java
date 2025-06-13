package com.apache.ignite.examples.transactions;

import com.apache.ignite.client.IgniteClient;
import com.apache.ignite.sql.IgniteSql;
import com.apache.ignite.sql.ResultSet;
import com.apache.ignite.sql.SqlRow;
import com.apache.ignite.table.RecordView;
import com.apache.ignite.table.Tuple;
import com.apache.ignite.tx.Transaction;
import com.apache.ignite.tx.TransactionException;
import com.apache.ignite.tx.TransactionOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates advanced transaction patterns from the documentation.
 * 
 * This application shows real-world scenarios and optimization patterns:
 * - Mixed API usage (Table API + SQL API in same transaction)
 * - Batch operations with proper scoping
 * - Exception handling strategies with retry patterns
 * - Performance optimization techniques
 * - Transaction state management
 * 
 * These patterns help build production-ready music store applications
 * that handle complex business workflows with data consistency.
 */
public class TransactionPatterns {
    private static final Logger logger = LoggerFactory.getLogger(TransactionPatterns.class);
    
    public static void main(String[] args) {
        logger.info("🎯 Starting Advanced Transaction Patterns - Production Music Store Scenarios");
        logger.info("===============================================================================");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800")
                .build()) {
            
            TransactionPatterns patterns = new TransactionPatterns();
            
            // Example 1: Mixed API Usage (from docs)
            logger.info("\n📖 Example 1: Mixed API Usage - Customer Order Processing");
            patterns.processCustomerOrder(client, 1, List.of(1, 2, 3));
            
            // Example 2: Batch Operations (from docs)
            logger.info("\n📖 Example 2: Batch Operations - Album Catalog Import");
            patterns.importAlbumCatalog(client, createSampleAlbums());
            
            // Example 3: Exception Handling with Retry (from docs)
            logger.info("\n📖 Example 3: Exception Handling with Retry Logic");
            patterns.handleWithRetry(client, 3);
            
            // Example 4: Transaction State Management (from docs)
            logger.info("\n📖 Example 4: Transaction State Management");
            patterns.demonstrateTransactionStates(client);
            
            // Example 5: Transaction Scope Optimization (from docs)
            logger.info("\n📖 Example 5: Transaction Scope Optimization");
            patterns.demonstrateProperScoping(client);
            
            logger.info("\n✅ Advanced Transaction Patterns completed successfully!");
            
        } catch (Exception e) {
            logger.error("❌ Advanced Transaction Patterns failed", e);
        }
    }
    
    /**
     * Demonstrates using both Table API and SQL API within the same transaction.
     * This pattern is common in applications that need both object-oriented
     * operations and complex SQL queries in the same business workflow.
     */
    public void processCustomerOrder(IgniteClient client, Integer customerId, List<Integer> trackIds) {
        logger.info("👤 Processing order for customer {} with {} tracks", customerId, trackIds.size());
        
        client.transactions().runInTransaction(tx -> {
            try {
                // Use Table API for object-oriented operations
                RecordView<Tuple> invoiceTable = client.tables().table("Invoice").recordView();
                RecordView<Tuple> invoiceLineTable = client.tables().table("InvoiceLine").recordView();
                
                // Use SQL API for complex queries
                IgniteSql sql = client.sql();
                
                // Step 1: Get customer info using SQL
                ResultSet<SqlRow> customerResult = sql.execute(tx,
                    "SELECT FirstName, LastName, Email FROM Customer WHERE CustomerId = ?", 
                    customerId);
                
                if (!customerResult.hasNext()) {
                    throw new IllegalArgumentException("Customer not found: " + customerId);
                }
                
                SqlRow customer = customerResult.next();
                String customerName = customer.stringValue("FirstName") + " " + customer.stringValue("LastName");
                logger.info("📋 Customer: {}", customerName);
                
                // Step 2: Create invoice using Table API
                Integer invoiceId = generateInvoiceId();
                Tuple invoice = Tuple.create()
                    .set("InvoiceId", invoiceId)
                    .set("CustomerId", customerId)
                    .set("InvoiceDate", LocalDate.now())
                    .set("BillingAddress", "123 Music St")
                    .set("BillingCity", "Harmony")
                    .set("BillingCountry", "USA")
                    .set("Total", BigDecimal.ZERO);
                
                invoiceTable.upsert(tx, invoice);
                logger.info("📝 Created invoice {}", invoiceId);
                
                // Step 3: Process each track using mixed APIs
                BigDecimal totalAmount = BigDecimal.ZERO;
                for (int i = 0; i < trackIds.size(); i++) {
                    Integer trackId = trackIds.get(i);
                    
                    // Get track details using SQL (supports complex queries)
                    ResultSet<SqlRow> trackResult = sql.execute(tx, """
                        SELECT t.Name, t.UnitPrice, a.Name as ArtistName, al.Title as AlbumTitle
                        FROM Track t
                        JOIN Album al ON t.AlbumId = al.AlbumId
                        JOIN Artist a ON al.ArtistId = a.ArtistId
                        WHERE t.TrackId = ?
                        """, trackId);
                    
                    if (!trackResult.hasNext()) {
                        throw new IllegalArgumentException("Track not found: " + trackId);
                    }
                    
                    SqlRow track = trackResult.next();
                    BigDecimal unitPrice = track.decimalValue("UnitPrice");
                    String trackName = track.stringValue("Name");
                    
                    // Create invoice line using Table API (simple object operations)
                    Tuple invoiceLine = Tuple.create()
                        .set("InvoiceLineId", generateLineItemId(i))
                        .set("InvoiceId", invoiceId)
                        .set("TrackId", trackId)
                        .set("UnitPrice", unitPrice)
                        .set("Quantity", 1);
                    
                    invoiceLineTable.upsert(tx, invoiceLine);
                    totalAmount = totalAmount.add(unitPrice);
                    
                    logger.info("🎵 Added: {} - ${}", trackName, unitPrice);
                }
                
                // Step 4: Update invoice total using Table API
                invoice = invoice.set("Total", totalAmount);
                invoiceTable.upsert(tx, invoice);
                
                logger.info("💰 Order total: ${}", totalAmount);
                logger.info("✅ Customer order processed successfully");
                
                return true;
                
            } catch (Exception e) {
                logger.error("❌ Order processing failed: " + e.getMessage());
                throw e;  // Triggers rollback
            }
        });
    }
    
    /**
     * Demonstrates efficient batch operations within transactions.
     * Batching reduces network round-trips and improves throughput
     * for operations that affect multiple records.
     */
    public void importAlbumCatalog(IgniteClient client, List<AlbumData> albums) {
        logger.info("📦 Starting batch import of {} albums", albums.size());
        
        TransactionOptions batchOptions = new TransactionOptions()
            .timeoutMillis(300000)  // 5 minutes for large batches
            .readOnly(false);
        
        client.transactions().runInTransaction(batchOptions, tx -> {
            RecordView<Tuple> albumTable = client.tables().table("Album").recordView();
            RecordView<Tuple> trackTable = client.tables().table("Track").recordView();
            
            for (AlbumData albumData : albums) {
                // Insert album
                Tuple album = Tuple.create()
                    .set("AlbumId", albumData.getAlbumId())
                    .set("Title", albumData.getTitle())
                    .set("ArtistId", albumData.getArtistId());
                
                albumTable.upsert(tx, album);
                
                // Insert all tracks for this album
                for (TrackData trackData : albumData.getTracks()) {
                    Tuple track = Tuple.create()
                        .set("TrackId", trackData.getTrackId())
                        .set("Name", trackData.getName())
                        .set("AlbumId", albumData.getAlbumId())
                        .set("GenreId", trackData.getGenreId())
                        .set("UnitPrice", trackData.getUnitPrice());
                    
                    trackTable.upsert(tx, track);
                }
                
                logger.info("📀 Imported album: {} ({} tracks)", albumData.getTitle(), albumData.getTracks().size());
            }
            
            logger.info("✅ Batch import completed successfully");
            return true;
        });
    }
    
    /**
     * Demonstrates comprehensive exception handling with retry logic.
     * Shows how to handle different failure scenarios gracefully.
     */
    public boolean handleWithRetry(IgniteClient client, int maxRetries) {
        logger.info("🔄 Testing retry logic with {} max attempts", maxRetries);
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return client.transactions().runInTransaction(tx -> {
                    // Simulate business logic that might fail
                    if (Math.random() < 0.4) {  // 40% failure rate for demo
                        throw new RuntimeException("Simulated transient failure");
                    }
                    
                    // Perform actual business operations
                    RecordView<Tuple> artistTable = client.tables().table("Artist").recordView();
                    Tuple artist = Tuple.create()
                        .set("ArtistId", 99990 + attempt)
                        .set("Name", "Retry Test Artist " + attempt);
                    
                    artistTable.upsert(tx, artist);
                    logger.info("✅ Attempt {} succeeded", attempt);
                    return true;
                });
                
            } catch (Exception e) {
                logger.warn("❌ Attempt {} failed: {}", attempt, e.getMessage());
                
                if (attempt < maxRetries) {
                    // Exponential backoff
                    try {
                        long delay = 100 * (1L << (attempt - 1));
                        logger.info("⏳ Waiting {}ms before retry...", delay);
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                } else {
                    logger.error("🚫 All retry attempts exhausted");
                    return false;
                }
            }
        }
        return false;
    }
    
    /**
     * Demonstrates transaction state monitoring and conditional logic
     * based on transaction properties. This is useful for debugging
     * and implementing different behaviors based on transaction context.
     */
    public void demonstrateTransactionStates(IgniteClient client) {
        // Read-only transaction example
        logger.info("🔍 Testing read-only transaction state");
        
        TransactionOptions readOnlyOptions = new TransactionOptions()
            .readOnly(true)
            .timeoutMillis(30000);
        
        Transaction readOnlyTx = client.transactions().begin(readOnlyOptions);
        try {
            boolean isReadOnly = readOnlyTx.isReadOnly();
            logger.info("Transaction is read-only: {}", isReadOnly);
            
            if (isReadOnly) {
                // Only perform read operations
                IgniteSql sql = client.sql();
                ResultSet<SqlRow> result = sql.execute(readOnlyTx, 
                    "SELECT COUNT(*) as artist_count FROM Artist");
                
                if (result.hasNext()) {
                    long count = result.next().longValue("artist_count");
                    logger.info("📊 Total artists: {}", count);
                }
            }
            
            readOnlyTx.commit();
            
        } catch (Exception e) {
            readOnlyTx.rollback();
            logger.error("Read-only transaction failed: " + e.getMessage());
        }
        
        // Write transaction example
        logger.info("✏️ Testing write transaction state");
        
        TransactionOptions writeOptions = new TransactionOptions()
            .readOnly(false)
            .timeoutMillis(15000);
        
        Transaction writeTx = client.transactions().begin(writeOptions);
        try {
            boolean isReadOnly = writeTx.isReadOnly();
            logger.info("Transaction is read-only: {}", isReadOnly);
            
            if (!isReadOnly) {
                // Can perform write operations
                RecordView<Tuple> artistTable = client.tables().table("Artist").recordView();
                
                Tuple artist = Tuple.create()
                    .set("ArtistId", 99999)
                    .set("Name", "State Demo Artist");
                
                artistTable.upsert(writeTx, artist);
                logger.info("✅ Artist created in write transaction");
            }
            
            writeTx.commit();
            
        } catch (Exception e) {
            writeTx.rollback();
            logger.error("Write transaction failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates optimal transaction scoping for different scenarios.
     * Proper transaction scope balances data consistency with system performance.
     */
    public void demonstrateProperScoping(IgniteClient client) {
        // ✅ GOOD: Narrow scope for quick operations
        logger.info("✅ Good pattern: Narrow scope for single artist update");
        client.transactions().runInTransaction(tx -> {
            RecordView<Tuple> artistTable = client.tables().table("Artist").recordView();
            
            Tuple artist = artistTable.get(tx, Tuple.create().set("ArtistId", 1));
            if (artist != null) {
                artist = artist.set("Name", artist.stringValue("Name") + " (Scoped Update)");
                artistTable.upsert(tx, artist);
                logger.info("🎤 Updated single artist efficiently");
            }
            return true;
        });
        
        // ✅ GOOD: Wider scope for related operations
        logger.info("✅ Good pattern: Related operations (album + tracks) in same transaction");
        client.transactions().runInTransaction(tx -> {
            RecordView<Tuple> albumTable = client.tables().table("Album").recordView();
            RecordView<Tuple> trackTable = client.tables().table("Track").recordView();
            
            // Create album
            Tuple album = Tuple.create()
                .set("AlbumId", 99998)
                .set("Title", "Scoping Demo Album")
                .set("ArtistId", 1);
            albumTable.upsert(tx, album);
            
            // Create related tracks (they belong together)
            for (int i = 1; i <= 3; i++) {
                Tuple track = Tuple.create()
                    .set("TrackId", 99990 + i)
                    .set("Name", "Demo Track " + i)
                    .set("AlbumId", 99998)
                    .set("UnitPrice", BigDecimal.valueOf(0.99));
                trackTable.upsert(tx, track);
            }
            
            logger.info("💿 Created album with {} related tracks", 3);
            return true;
        });
        
        logger.info("💡 Avoid: Wide scope with unrelated operations (use separate transactions instead)");
    }
    
    // Helper methods and data classes
    private Integer generateInvoiceId() {
        return (int) (System.currentTimeMillis() % 100000);
    }
    
    private Integer generateLineItemId(int index) {
        return (int) ((System.currentTimeMillis() + index) % 100000);
    }
    
    /**
     * Creates sample album data for batch import demonstration.
     */
    private static List<AlbumData> createSampleAlbums() {
        List<AlbumData> albums = new ArrayList<>();
        
        AlbumData album1 = new AlbumData(50001, "Demo Album 1", 1);
        album1.getTracks().add(new TrackData(80001, "Demo Track 1", 1, BigDecimal.valueOf(0.99)));
        album1.getTracks().add(new TrackData(80002, "Demo Track 2", 1, BigDecimal.valueOf(1.29)));
        albums.add(album1);
        
        AlbumData album2 = new AlbumData(50002, "Demo Album 2", 1);
        album2.getTracks().add(new TrackData(80003, "Demo Track 3", 1, BigDecimal.valueOf(0.99)));
        albums.add(album2);
        
        return albums;
    }
    
    // Helper classes for demo data
    public static class AlbumData {
        private Integer albumId;
        private String title;
        private Integer artistId;
        private List<TrackData> tracks = new ArrayList<>();
        
        public AlbumData(Integer albumId, String title, Integer artistId) {
            this.albumId = albumId;
            this.title = title;
            this.artistId = artistId;
        }
        
        public Integer getAlbumId() { return albumId; }
        public String getTitle() { return title; }
        public Integer getArtistId() { return artistId; }
        public List<TrackData> getTracks() { return tracks; }
    }
    
    public static class TrackData {
        private Integer trackId;
        private String name;
        private Integer genreId;
        private BigDecimal unitPrice;
        
        public TrackData(Integer trackId, String name, Integer genreId, BigDecimal unitPrice) {
            this.trackId = trackId;
            this.name = name;
            this.genreId = genreId;
            this.unitPrice = unitPrice;
        }
        
        public Integer getTrackId() { return trackId; }
        public String getName() { return name; }
        public Integer getGenreId() { return genreId; }
        public BigDecimal getUnitPrice() { return unitPrice; }
    }
}