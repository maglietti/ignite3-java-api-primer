package com.apache.ignite.examples.tableapi;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.RecordView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RecordView Examples - POJO-based operations with Apache Ignite 3.
 * 
 * This class demonstrates working with strongly-typed object operations:
 * - Using POJOs with type safety
 * - Object mapping patterns
 * - Working with the music store domain model
 * 
 * Learning Focus:
 * - RecordView with custom POJOs
 * - Type safety in database operations
 * - Domain object patterns
 * - Ignite 3 object mapping
 */
public class RecordViewExamples {

    private static final Logger logger = LoggerFactory.getLogger(RecordViewExamples.class);

    /**
     * Artist POJO for demonstration.
     * This represents the Artist entity from the music store domain model.
     */
    public static class Artist {
        private Integer artistId;
        private String name;

        public Artist() {} // Required for Ignite mapping

        public Artist(Integer artistId, String name) {
            this.artistId = artistId;
            this.name = name;
        }

        public Integer getArtistId() { return artistId; }
        public void setArtistId(Integer artistId) { this.artistId = artistId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        @Override
        public String toString() {
            return "Artist{id=" + artistId + ", name='" + name + "'}";
        }
    }

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== RecordView Examples Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Demonstrating type-safe POJO operations");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            runRecordViewExamples(client);
            
        } catch (Exception e) {
            logger.error("Failed to run RecordView examples", e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void runRecordViewExamples(IgniteClient client) {
        // Get the Artist table
        Table artistTable = client.tables().table("Artist");
        if (artistTable == null) {
            throw new RuntimeException("Artist table not found. Please run sample data setup first.");
        }
        
        // Get a strongly-typed record view
        RecordView<Artist> artists = artistTable.recordView(Artist.class);
        
        System.out.println("\n--- POJO-based Operations ---");
        System.out.println("    Using strongly-typed Artist objects for database operations");
        
        // Work with existing data
        demonstrateTypedOperations(artists);
        
        // Create and manipulate new records
        demonstrateObjectOperations(artists);
        
        System.out.println("\n>>> RecordView examples completed successfully");
    }

    private static void demonstrateTypedOperations(RecordView<Artist> artists) {
        System.out.println("\n    --- Reading Existing Artists");
        System.out.println("    >>> Retrieving sample data using POJO key objects");
        
        // Read some existing artists from the sample data
        for (int id = 1; id <= 3; id++) {
            Artist key = new Artist(id, null); // Only ID needed for lookup
            Artist artist = artists.get(null, key);
            
            if (artist != null) {
                System.out.println("    <<< " + artist);
            } else {
                System.out.println("    !!! Artist " + id + " not found");
            }
        }
    }

    private static void demonstrateObjectOperations(RecordView<Artist> artists) {
        System.out.println("\n    --- Object-Oriented Operations");
        System.out.println("    >>> Creating and manipulating POJO objects");
        
        // Create a new artist object
        Artist newArtist = new Artist(5002, "POJO Demo Artist");
        
        // Insert using the object
        artists.upsert(null, newArtist);
        System.out.println("    <<< Created: " + newArtist);
        
        // Read it back
        Artist key = new Artist(5002, null);
        Artist retrieved = artists.get(null, key);
        System.out.println("    <<< Retrieved: " + retrieved);
        
        // Update the object
        if (retrieved != null) {
            retrieved.setName("Updated POJO Artist");
            artists.upsert(null, retrieved);
            System.out.println("    <<< Updated: " + retrieved);
        }
        
        // Verify the update
        Artist updated = artists.get(null, key);
        System.out.println("    <<< Verified: " + updated);
        
        // Clean up
        boolean deleted = artists.delete(null, key);
        System.out.println("    <<< Cleanup: " + (deleted ? "Deleted successfully" : "Delete failed"));
    }
}