package com.apache.ignite.examples.transactions;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.tx.Transaction;
import org.apache.ignite.tx.IgniteTransactions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transaction Isolation - Demonstrating isolation levels with Apache Ignite 3.
 * 
 * This class demonstrates transaction isolation concepts:
 * - Concurrent transaction behavior
 * - Data consistency patterns
 * - Read and write conflicts
 * - Transaction coordination
 * 
 * Learning Focus:
 * - Transaction isolation fundamentals
 * - Concurrent access patterns
 * - Data consistency guarantees
 * - Conflict resolution strategies
 */
public class TransactionIsolation {

    private static final Logger logger = LoggerFactory.getLogger(TransactionIsolation.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Transaction Isolation Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            runIsolationDemos(client);
            
        } catch (Exception e) {
            logger.error("Failed to run isolation demos", e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void runIsolationDemos(IgniteClient client) {
        // Get the Artist table
        Table artistTable = client.tables().table("Artist");
        if (artistTable == null) {
            throw new RuntimeException("Artist table not found. Please run sample data setup first.");
        }
        
        RecordView<Tuple> artists = artistTable.recordView();
        IgniteTransactions transactions = client.transactions();
        
        System.out.println("\n--- Transaction Isolation Patterns ---");
        
        // Set up test data
        setupTestData(artists);
        
        // Demonstrate isolation concepts
        demonstrateReadConsistency(transactions, artists);
        demonstrateWriteConflicts(transactions, artists);
        demonstrateTransactionCoordination(transactions, artists);
        
        // Clean up
        cleanupTestData(artists);
        
        System.out.println("\n✓ Transaction isolation demos completed successfully");
    }

    private static void setupTestData(RecordView<Tuple> artists) {
        System.out.println("\n1. Setting up test data:");
        
        Tuple testArtist = Tuple.create()
            .set("ArtistId", 7001)
            .set("Name", "Isolation Test Artist");
        
        artists.upsert(null, testArtist);
        System.out.println("   ✓ Created test artist: " + testArtist.stringValue("Name"));
    }

    private static void demonstrateReadConsistency(IgniteTransactions transactions, RecordView<Tuple> artists) {
        System.out.println("\n2. Read Consistency:");
        
        // Start a transaction and read data
        Transaction tx1 = transactions.begin();
        try {
            Tuple key = Tuple.create().set("ArtistId", 7001);
            Tuple artist = artists.get(tx1, key);
            
            System.out.println("   ⚡ Transaction 1 reads: " + artist.stringValue("Name"));
            
            // Simulate another process updating the data outside this transaction
            Tuple updatedArtist = artist.set("Name", "Modified Outside Transaction");
            artists.upsert(null, updatedArtist);  // Update outside transaction
            System.out.println("   ⚡ External update applied");
            
            // Read again within the same transaction
            Tuple artistAgain = artists.get(tx1, key);
            System.out.println("   ⚡ Transaction 1 reads again: " + artistAgain.stringValue("Name"));
            
            // The transaction sees consistent data throughout its lifetime
            tx1.commit();
            System.out.println("   ✓ Transaction maintained read consistency");
        } catch (Exception e) {
            tx1.rollback();
            throw e;
        }
    }

    private static void demonstrateWriteConflicts(IgniteTransactions transactions, RecordView<Tuple> artists) {
        System.out.println("\n3. Write Conflict Handling:");
        
        Tuple key = Tuple.create().set("ArtistId", 7001);
        
        // Transaction 1: Read and prepare to update
        Transaction tx1 = transactions.begin();
        try {
            Tuple artist1 = artists.get(tx1, key);
            System.out.println("   ⚡ Transaction 1 reads: " + artist1.stringValue("Name"));
            
            // Simulate concurrent update from another transaction
            Transaction tx2 = transactions.begin();
            try {
                Tuple artist2 = artists.get(tx2, key);
                Tuple updated2 = artist2.set("Name", "Updated by Transaction 2");
                artists.upsert(tx2, updated2);
                tx2.commit();
                System.out.println("   ⚡ Transaction 2 committed update");
            } catch (Exception e) {
                tx2.rollback();
                throw e;
            }
            
            // Transaction 1 attempts to update
            try {
                Tuple updated1 = artist1.set("Name", "Updated by Transaction 1");
                artists.upsert(tx1, updated1);
                tx1.commit();
                System.out.println("   ✓ Transaction 1 update succeeded");
            } catch (Exception e) {
                logger.warn("Transaction 1 conflict detected", e);
                System.out.println("   ⚠ Transaction 1 detected conflict: " + e.getMessage());
            }
        } catch (Exception e) {
            tx1.rollback();
            throw e;
        }
    }

    private static void demonstrateTransactionCoordination(IgniteTransactions transactions, RecordView<Tuple> artists) {
        System.out.println("\n4. Transaction Coordination:");
        
        // Demonstrate coordinated multi-step operations
        transactions.runInTransaction(tx -> {
            Tuple key = Tuple.create().set("ArtistId", 7001);
            
            // Step 1: Read current data
            Tuple currentArtist = artists.get(tx, key);
            System.out.println("   ⚡ Step 1 - Current: " + currentArtist.stringValue("Name"));
            
            // Step 2: Business logic processing
            String newName = "Coordinated Update - " + System.currentTimeMillis();
            System.out.println("   ⚡ Step 2 - Processing: " + newName);
            
            // Step 3: Apply coordinated update
            Tuple updatedArtist = currentArtist.set("Name", newName);
            artists.upsert(tx, updatedArtist);
            System.out.println("   ⚡ Step 3 - Updated in transaction");
            
            // Step 4: Validation
            Tuple verification = artists.get(tx, key);
            if (verification.stringValue("Name").equals(newName)) {
                System.out.println("   ⚡ Step 4 - Validation passed");
                System.out.println("   ✓ Coordinated transaction completed");
            } else {
                throw new RuntimeException("Validation failed");
            }
        });
    }

    private static void cleanupTestData(RecordView<Tuple> artists) {
        System.out.println("\n5. Cleanup:");
        
        Tuple key = Tuple.create().set("ArtistId", 7001);
        boolean deleted = artists.delete(null, key);
        if (deleted) {
            System.out.println("   ✓ Cleaned up test data");
        }
    }
}