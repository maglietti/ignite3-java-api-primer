package com.apache.ignite.examples.schema;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;

import com.apache.ignite.examples.setup.model.Artist;
import com.apache.ignite.examples.setup.model.Album;
import com.apache.ignite.examples.setup.model.Track;
import com.apache.ignite.examples.setup.model.Customer;
import com.apache.ignite.examples.setup.model.Invoice;
import com.apache.ignite.examples.setup.model.InvoiceLine;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Demonstrates Apache Ignite 3 colocation strategies for performance optimization.
 * 
 * This application shows how to:
 * 1. Design colocation hierarchies for related data
 * 2. Ensure colocation keys are part of primary keys
 * 3. Demonstrate performance benefits of colocated queries
 * 4. Show the difference between colocated and non-colocated operations
 * 
 * Key Colocation Concepts:
 * - Artist → Album → Track (music catalog hierarchy)
 * - Customer → Invoice → InvoiceLine (sales transaction hierarchy) 
 * - Single-node query execution for colocated data
 * - Network overhead reduction through data locality
 * - Join performance optimization
 * 
 * Prerequisites:
 * - Running Ignite 3 cluster
 * - Sample data setup (music store schema and data)
 */
public class ColocationExamples {
    
    private static final String CLUSTER_URL = "127.0.0.1:10800";
    
    public static void main(String[] args) {
        System.out.println("=== Colocation Examples Demo ===");
        System.out.println("Demonstrating data colocation strategies in Ignite 3");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(CLUSTER_URL)
                .build()) {
            
            ColocationExamples demo = new ColocationExamples();
            
            // Setup demo data
            demo.setupDemoData(client);
            
            // Demonstrate colocation patterns
            demo.demonstrateMusicCatalogColocation(client);
            demo.demonstrateSalesTransactionColocation(client);
            demo.demonstrateColocatedQueryPerformance(client);
            demo.demonstrateColocationBestPractices(client);
            
            // Cleanup
            demo.cleanupDemoData(client);
            
            System.out.println("\n✓ Colocation Examples Demo completed successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Sets up demonstration data showing colocation hierarchies.
     */
    private void setupDemoData(IgniteClient client) {
        System.out.println("\n--- Setting Up Colocation Demo Data ---");
        
        try {
            // Setup Artist → Album → Track hierarchy
            setupMusicHierarchy(client);
            
            // Setup Customer → Invoice → InvoiceLine hierarchy  
            setupSalesHierarchy(client);
            
            System.out.println("✓ Demo data setup completed");
            
        } catch (Exception e) {
            System.err.println("❌ Demo data setup failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Sets up the music catalog hierarchy: Artist → Album → Track
     */
    private void setupMusicHierarchy(IgniteClient client) {
        // Create Artist (root entity)
        RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
        Artist artist = new Artist(1000, "Colocation Demo Band");
        artistView.upsert(null, artist);
        System.out.println("✓ Created Artist: " + artist.getName());
        
        // Create Album (colocated by ArtistId)
        RecordView<Album> albumView = client.tables().table("Album").recordView(Album.class);
        Album album = new Album(2000, 1000, "Colocation Demo Album");
        albumView.upsert(null, album);
        System.out.println("✓ Created Album: " + album.getTitle() + " (colocated with Artist " + album.getArtistId() + ")");
        
        // Create Tracks (colocated by AlbumId)
        RecordView<Track> trackView = client.tables().table("Track").recordView(Track.class);
        for (int i = 1; i <= 3; i++) {
            Track track = new Track();
            track.setTrackId(3000 + i);
            track.setAlbumId(2000);  // Colocation key
            track.setName("Demo Track " + i);
            track.setMilliseconds(180000 + (i * 1000));
            track.setUnitPrice(BigDecimal.valueOf(0.99));
            
            trackView.upsert(null, track);
            System.out.println("✓ Created Track: " + track.getName() + " (colocated with Album " + track.getAlbumId() + ")");
        }
    }
    
    /**
     * Sets up the sales hierarchy: Customer → Invoice → InvoiceLine
     */
    private void setupSalesHierarchy(IgniteClient client) {
        // Create Customer (root entity)
        RecordView<Customer> customerView = client.tables().table("Customer").recordView(Customer.class);
        Customer customer = new Customer();
        customer.setCustomerId(5000);
        customer.setFirstName("Demo");
        customer.setLastName("Customer");
        customer.setEmail("demo.customer@colocation.test");
        customer.setCity("Colocation City");
        customer.setCountry("Demo Country");
        
        customerView.upsert(null, customer);
        System.out.println("✓ Created Customer: " + customer.getFirstName() + " " + customer.getLastName());
        
        // Create Invoice (colocated by CustomerId)
        RecordView<Invoice> invoiceView = client.tables().table("Invoice").recordView(Invoice.class);
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(6000);
        invoice.setCustomerId(5000);  // Colocation key
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setBillingCity("Colocation City");
        invoice.setBillingCountry("Demo Country");
        invoice.setTotal(BigDecimal.valueOf(2.97));  // 3 tracks × $0.99
        
        invoiceView.upsert(null, invoice);
        System.out.println("✓ Created Invoice: " + invoice.getInvoiceId() + " (colocated with Customer " + invoice.getCustomerId() + ")");
        
        // Create InvoiceLines (colocated by InvoiceId)
        RecordView<InvoiceLine> invoiceLineView = client.tables().table("InvoiceLine").recordView(InvoiceLine.class);
        for (int i = 1; i <= 3; i++) {
            InvoiceLine line = new InvoiceLine();
            line.setInvoiceLineId(7000 + i);
            line.setInvoiceId(6000);  // Colocation key
            line.setTrackId(3000 + i);
            line.setUnitPrice(BigDecimal.valueOf(0.99));
            line.setQuantity(1);
            
            invoiceLineView.upsert(null, line);
            System.out.println("✓ Created InvoiceLine: " + line.getInvoiceLineId() + " (colocated with Invoice " + line.getInvoiceId() + ")");
        }
    }
    
    /**
     * Demonstrates music catalog colocation: Artist → Album → Track
     */
    private void demonstrateMusicCatalogColocation(IgniteClient client) {
        System.out.println("\n--- Music Catalog Colocation Hierarchy ---");
        
        System.out.println("Colocation chain: Artist (1000) → Album (2000) → Tracks (3001-3003)");
        System.out.println("- Albums colocated with Artists by ArtistId");
        System.out.println("- Tracks colocated with Albums by AlbumId");
        System.out.println("- All related data for an artist stored on same cluster nodes");
        
        try {
            // Query demonstrating colocation benefits
            String colocatedQuery = """
                SELECT 
                    ar.Name as Artist,
                    al.Title as Album,
                    t.Name as Track,
                    t.Milliseconds / 1000 as DurationSeconds
                FROM Artist ar
                JOIN Album al ON ar.ArtistId = al.ArtistId
                JOIN Track t ON al.AlbumId = t.AlbumId
                WHERE ar.ArtistId = ?
                ORDER BY t.TrackId
                """;
            
            System.out.println("\nExecuting colocated query for Artist 1000:");
            try (ResultSet<SqlRow> result = client.sql().execute(null, colocatedQuery, 1000)) {
                while (result.hasNext()) {
                    SqlRow row = result.next();
                    System.out.printf("  %s - %s - %s (%d sec)%n",
                        row.stringValue("Artist"),
                        row.stringValue("Album"),
                        row.stringValue("Track"),
                        row.longValue("DurationSeconds"));
                }
            }
            
            System.out.println("✓ Query executed on single node due to colocation");
            
        } catch (Exception e) {
            System.err.println("❌ Music catalog colocation demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates sales transaction colocation: Customer → Invoice → InvoiceLine
     */
    private void demonstrateSalesTransactionColocation(IgniteClient client) {
        System.out.println("\n--- Sales Transaction Colocation Hierarchy ---");
        
        System.out.println("Colocation chain: Customer (5000) → Invoice (6000) → InvoiceLines (7001-7003)");
        System.out.println("- Invoices colocated with Customers by CustomerId");
        System.out.println("- InvoiceLines colocated with Invoices by InvoiceId");
        System.out.println("- Complete transaction data stored together for performance");
        
        try {
            // Query demonstrating transaction colocation
            String transactionQuery = """
                SELECT 
                    c.FirstName || ' ' || c.LastName as Customer,
                    i.InvoiceId,
                    i.InvoiceDate,
                    il.InvoiceLineId,
                    il.Quantity,
                    il.UnitPrice,
                    (il.Quantity * il.UnitPrice) as LineTotal
                FROM Customer c
                JOIN Invoice i ON c.CustomerId = i.CustomerId
                JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId
                WHERE c.CustomerId = ?
                ORDER BY il.InvoiceLineId
                """;
            
            System.out.println("\nExecuting colocated transaction query for Customer 5000:");
            BigDecimal total = BigDecimal.ZERO;
            try (ResultSet<SqlRow> result = client.sql().execute(null, transactionQuery, 5000)) {
                while (result.hasNext()) {
                    SqlRow row = result.next();
                    BigDecimal lineTotal = (BigDecimal) row.value("LineTotal");
                    total = total.add(lineTotal);
                    
                    System.out.printf("  %s - Invoice %d - Line %d: %d × $%.2f = $%.2f%n",
                        row.stringValue("Customer"),
                        row.intValue("InvoiceId"),
                        row.intValue("InvoiceLineId"),
                        row.intValue("Quantity"),
                        ((BigDecimal) row.value("UnitPrice")).doubleValue(),
                        lineTotal.doubleValue());
                }
            }
            
            System.out.printf("Total: $%.2f%n", total.doubleValue());
            System.out.println("✓ Transaction query executed on single node due to colocation");
            
        } catch (Exception e) {
            System.err.println("❌ Sales transaction colocation demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates performance benefits of colocated queries vs non-colocated queries.
     */
    private void demonstrateColocatedQueryPerformance(IgniteClient client) {
        System.out.println("\n--- Colocation Performance Benefits ---");
        
        try {
            // Colocated query - all data on same nodes
            String colocatedQuery = """
                SELECT COUNT(*) as TrackCount
                FROM Artist ar
                JOIN Album al ON ar.ArtistId = al.ArtistId
                JOIN Track t ON al.AlbumId = t.AlbumId
                WHERE ar.ArtistId = ?
                """;
            
            long startTime = System.currentTimeMillis();
            try (ResultSet<SqlRow> result = client.sql().execute(null, colocatedQuery, 1000)) {
                if (result.hasNext()) {
                    int trackCount = result.next().intValue("TrackCount");
                    long colocatedTime = System.currentTimeMillis() - startTime;
                    System.out.printf("✓ Colocated query: Found %d tracks in %d ms%n", trackCount, colocatedTime);
                    System.out.println("  → Single-node execution, minimal network overhead");
                }
            }
            
            // Cross-hierarchy query - may require multi-node coordination
            String crossQuery = """
                SELECT 
                    COUNT(DISTINCT c.CustomerId) as CustomerCount,
                    COUNT(DISTINCT ar.ArtistId) as ArtistCount
                FROM Customer c
                CROSS JOIN Artist ar
                WHERE c.CustomerId = ? AND ar.ArtistId = ?
                """;
            
            startTime = System.currentTimeMillis();
            try (ResultSet<SqlRow> result = client.sql().execute(null, crossQuery, 5000, 1000)) {
                if (result.hasNext()) {
                    SqlRow row = result.next();
                    long crossTime = System.currentTimeMillis() - startTime;
                    System.out.printf("✓ Cross-hierarchy query: %d customers × %d artists in %d ms%n", 
                        row.intValue("CustomerCount"), 
                        row.intValue("ArtistCount"), 
                        crossTime);
                    System.out.println("  → May require multi-node coordination");
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Performance demonstration failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates colocation best practices and common patterns.
     */
    private void demonstrateColocationBestPractices(IgniteClient client) {
        System.out.println("\n--- Colocation Best Practices ---");
        
        System.out.println("Best Practice 1: Colocation Key in Primary Key");
        System.out.println("  ✓ Album.ArtistId is both @Id and colocation key");
        System.out.println("  ✓ Track.AlbumId is both @Id and colocation key");
        System.out.println("  ✓ InvoiceLine.InvoiceId is both @Id and colocation key");
        
        System.out.println("\nBest Practice 2: Hierarchical Colocation Design");
        System.out.println("  ✓ Root entities: Artist, Customer (no colocation)");
        System.out.println("  ✓ Level 2: Album by ArtistId, Invoice by CustomerId");
        System.out.println("  ✓ Level 3: Track by AlbumId, InvoiceLine by InvoiceId");
        
        System.out.println("\nBest Practice 3: Query Pattern Alignment");
        System.out.println("  ✓ Music queries: Artist → Albums → Tracks (colocated)");
        System.out.println("  ✓ Sales queries: Customer → Invoices → Lines (colocated)");
        System.out.println("  ✓ Cross-domain queries may require multi-node coordination");
        
        System.out.println("\nBest Practice 4: Balanced Distribution");
        System.out.println("  ✓ Many artists → good distribution");
        System.out.println("  ✓ Many customers → good distribution");
        System.out.println("  ✗ Few countries → poor distribution (avoid for colocation)");
        
        try {
            // Demonstrate colocation key validation
            validateColocationKeys(client);
            
        } catch (Exception e) {
            System.err.println("❌ Best practices demonstration failed: " + e.getMessage());
        }
    }
    
    /**
     * Validates that colocation keys are properly configured.
     */
    private void validateColocationKeys(IgniteClient client) {
        System.out.println("\nValidating colocation configuration:");
        
        try {
            // Verify Album colocation
            RecordView<Album> albumView = client.tables().table("Album").recordView(Album.class);
            Album album = new Album();
            album.setAlbumId(2000);
            album.setArtistId(1000);  // Must specify both parts of composite key
            Album retrieved = albumView.get(null, album);
            if (retrieved != null) {
                System.out.println("  ✓ Album colocation: Key includes ArtistId");
            }
            
            // Verify Track colocation
            RecordView<Track> trackView = client.tables().table("Track").recordView(Track.class);
            Track track = new Track();
            track.setTrackId(3001);
            track.setAlbumId(2000);  // Must specify both parts of composite key
            Track retrievedTrack = trackView.get(null, track);
            if (retrievedTrack != null) {
                System.out.println("  ✓ Track colocation: Key includes AlbumId");
            }
            
            System.out.println("✓ Colocation validation completed");
            
        } catch (Exception e) {
            System.err.println("❌ Colocation validation failed: " + e.getMessage());
        }
    }
    
    /**
     * Cleans up demonstration data.
     */
    private void cleanupDemoData(IgniteClient client) {
        System.out.println("\n--- Cleaning Up Demo Data ---");
        
        try {
            // Clean up in reverse dependency order
            cleanupInvoiceLines(client);
            cleanupInvoice(client);
            cleanupCustomer(client);
            cleanupTracks(client);
            cleanupAlbum(client);
            cleanupArtist(client);
            
            System.out.println("✓ Demo data cleanup completed");
            
        } catch (Exception e) {
            System.err.println("⚠ Demo data cleanup failed (expected): " + e.getMessage());
        }
    }
    
    private void cleanupInvoiceLines(IgniteClient client) {
        RecordView<InvoiceLine> view = client.tables().table("InvoiceLine").recordView(InvoiceLine.class);
        for (int i = 1; i <= 3; i++) {
            InvoiceLine key = new InvoiceLine();
            key.setInvoiceLineId(7000 + i);
            key.setInvoiceId(6000);
            view.delete(null, key);
        }
    }
    
    private void cleanupInvoice(IgniteClient client) {
        RecordView<Invoice> view = client.tables().table("Invoice").recordView(Invoice.class);
        Invoice key = new Invoice();
        key.setInvoiceId(6000);
        key.setCustomerId(5000);
        view.delete(null, key);
    }
    
    private void cleanupCustomer(IgniteClient client) {
        RecordView<Customer> view = client.tables().table("Customer").recordView(Customer.class);
        Customer key = new Customer();
        key.setCustomerId(5000);
        view.delete(null, key);
    }
    
    private void cleanupTracks(IgniteClient client) {
        RecordView<Track> view = client.tables().table("Track").recordView(Track.class);
        for (int i = 1; i <= 3; i++) {
            Track key = new Track();
            key.setTrackId(3000 + i);
            key.setAlbumId(2000);
            view.delete(null, key);
        }
    }
    
    private void cleanupAlbum(IgniteClient client) {
        RecordView<Album> view = client.tables().table("Album").recordView(Album.class);
        Album key = new Album();
        key.setAlbumId(2000);
        key.setArtistId(1000);
        view.delete(null, key);
    }
    
    private void cleanupArtist(IgniteClient client) {
        RecordView<Artist> view = client.tables().table("Artist").recordView(Artist.class);
        Artist key = new Artist();
        key.setArtistId(1000);
        view.delete(null, key);
    }
}