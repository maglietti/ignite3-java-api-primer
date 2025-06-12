package com.apache.ignite.examples.schema;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;

import com.apache.ignite.examples.setup.model.Artist;
import com.apache.ignite.examples.setup.model.Album;
import com.apache.ignite.examples.setup.model.Track;
import com.apache.ignite.examples.setup.model.Genre;
import com.apache.ignite.examples.setup.model.Customer;

/**
 * Demonstrates Apache Ignite 3 schema-as-code using annotated entity classes.
 * 
 * This application shows how to:
 * 1. Create tables from annotated POJOs automatically
 * 2. Use different annotation patterns (simple, composite keys, colocation)
 * 3. Work with distribution zones and indexes
 * 4. Perform basic CRUD operations on annotated entities
 * 
 * Key Ignite 3 Concepts Demonstrated:
 * - @Table annotation for table definitions
 * - @Zone annotation for distribution zones
 * - @Column annotation for field mapping
 * - @Id annotation for primary keys
 * - @ColumnRef and colocation for performance
 * - @Index annotation for secondary indexes
 * - DDL generation from annotations
 * - RecordView operations on annotated entities
 * 
 * Prerequisites:
 * - Running Ignite 3 cluster (use docker-compose from 00-docker module)
 * - Tables can be created automatically or pre-created with sample-data-setup
 */
public class AnnotatedEntitiesDemo {
    
    private static final String CLUSTER_URL = "127.0.0.1:10800";
    
    public static void main(String[] args) {
        System.out.println("=== Annotated Entities Demo ===");
        System.out.println("Demonstrating schema-as-code with Ignite 3 annotations");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(CLUSTER_URL)
                .build()) {
            
            AnnotatedEntitiesDemo demo = new AnnotatedEntitiesDemo();
            
            // Demonstrate table creation from annotations
            demo.demonstrateTableCreation(client);
            
            // Show different annotation patterns
            demo.demonstrateSimpleEntity(client);
            demo.demonstrateCompositeKeyEntity(client);
            demo.demonstrateColocationEntity(client);
            demo.demonstrateReferenceDataEntity(client);
            demo.demonstrateComplexEntity(client);
            
            System.out.println("\n✓ Annotated Entities Demo completed successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Demonstrates automatic table creation from annotated classes.
     * Shows how Ignite 3 generates DDL from annotations.
     */
    private void demonstrateTableCreation(IgniteClient client) {
        System.out.println("\n--- Table Creation from Annotations ---");
        
        try {
            // Try to create tables from annotated classes
            // These will fail if tables already exist, which is fine
            
            System.out.println("Creating tables from annotated POJOs:");
            
            // Simple entity with single primary key
            createTableSafely(client, "Artist", Artist.class);
            
            // Reference data entity (different zone)
            createTableSafely(client, "Genre", Genre.class);
            
            // Composite key entity with colocation
            createTableSafely(client, "Album", Album.class);
            
            // Complex entity with multiple relationships
            createTableSafely(client, "Track", Track.class);
            
            System.out.println("✓ Table creation demonstration completed");
            
        } catch (Exception e) {
            System.err.println("⚠ Table creation error (expected if tables exist): " + e.getMessage());
        }
    }
    
    /**
     * Helper method to create tables safely, handling "already exists" exceptions.
     */
    private void createTableSafely(IgniteClient client, String tableName, Class<?> entityClass) {
        try {
            client.catalog().createTable(entityClass);
            System.out.println("  ✓ Created table: " + tableName);
        } catch (Exception e) {
            if (e.getMessage().contains("already exists") || e.getMessage().contains("Table") && e.getMessage().contains("exist")) {
                System.out.println("  ℹ Table already exists: " + tableName);
            } else {
                System.out.println("  ❌ Failed to create " + tableName + ": " + e.getMessage());
                throw e;
            }
        }
    }
    
    /**
     * Demonstrates simple entity pattern with single primary key.
     * Artist table: @Table + @Zone + @Id + @Column
     */
    private void demonstrateSimpleEntity(IgniteClient client) {
        System.out.println("\n--- Simple Entity Pattern: Artist ---");
        
        try {
            Table artistTable = client.tables().table("Artist");
            RecordView<Artist> artistView = artistTable.recordView(Artist.class);
            
            System.out.println("Annotation features demonstrated:");
            System.out.println("- @Table: Marks class as Ignite table");
            System.out.println("- @Zone: Places table in 'MusicStore' distribution zone");
            System.out.println("- @Id: Single field primary key");
            System.out.println("- @Column: Field-to-column mapping with constraints");
            
            // Insert sample artist
            Artist artist = new Artist(999, "Demo Artist");
            artistView.upsert(null, artist);
            System.out.println("✓ Inserted: " + artist);
            
            // Retrieve by primary key
            Artist keyOnly = new Artist();
            keyOnly.setArtistId(999);
            Artist retrieved = artistView.get(null, keyOnly);
            System.out.println("✓ Retrieved: " + retrieved);
            
            // Clean up
            boolean deleted = artistView.delete(null, keyOnly);
            System.out.println("✓ Cleanup successful: " + deleted);
            
        } catch (Exception e) {
            System.err.println("❌ Simple entity demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates composite primary key pattern.
     * Album table: @Id on multiple fields for composite key
     */
    private void demonstrateCompositeKeyEntity(IgniteClient client) {
        System.out.println("\n--- Composite Key Entity Pattern: Album ---");
        
        try {
            Table albumTable = client.tables().table("Album");
            RecordView<Album> albumView = albumTable.recordView(Album.class);
            
            System.out.println("Annotation features demonstrated:");
            System.out.println("- @Id: Multiple fields forming composite primary key");
            System.out.println("- Composite key: (AlbumId, ArtistId)");
            System.out.println("- Both fields required for unique identification");
            
            // First ensure artist exists
            ensureArtistExists(client, 999);
            
            // Insert sample album with composite key
            Album album = new Album(9999, 999, "Demo Album");
            albumView.upsert(null, album);
            System.out.println("✓ Inserted: " + album);
            
            // Retrieve using composite key (both fields required)
            Album keyOnly = new Album();
            keyOnly.setAlbumId(9999);
            keyOnly.setArtistId(999);  // Required for composite key
            Album retrieved = albumView.get(null, keyOnly);
            System.out.println("✓ Retrieved: " + retrieved);
            
            // Clean up
            boolean deleted = albumView.delete(null, keyOnly);
            System.out.println("✓ Cleanup successful: " + deleted);
            
            // Clean up artist
            cleanupArtist(client, 999);
            
        } catch (Exception e) {
            System.err.println("❌ Composite key demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates colocation pattern for performance optimization.
     * Track table: @ColumnRef colocation with Album
     */
    private void demonstrateColocationEntity(IgniteClient client) {
        System.out.println("\n--- Colocation Entity Pattern: Track ---");
        
        try {
            Table trackTable = client.tables().table("Track");
            RecordView<Track> trackView = trackTable.recordView(Track.class);
            
            System.out.println("Annotation features demonstrated:");
            System.out.println("- @ColumnRef: colocateBy for data co-location");
            System.out.println("- Performance: Related data stored on same nodes");
            System.out.println("- Colocation key must be part of primary key");
            System.out.println("- Enables single-node joins for better performance");
            
            // Setup prerequisite data
            ensureArtistExists(client, 999);
            ensureAlbumExists(client, 9999, 999);
            ensureGenreExists(client, 99);
            
            // Insert sample track with colocation
            Track track = new Track();
            track.setTrackId(99999);
            track.setAlbumId(9999);  // Colocation key - must be part of PK
            track.setName("Demo Track");
            track.setGenreId(99);
            track.setMilliseconds(180000);  // 3 minutes
            track.setUnitPrice(java.math.BigDecimal.valueOf(0.99));
            
            trackView.upsert(null, track);
            System.out.println("✓ Inserted: " + track);
            System.out.println("  → Track colocated with Album " + track.getAlbumId());
            
            // Retrieve using composite key
            Track keyOnly = new Track();
            keyOnly.setTrackId(99999);
            keyOnly.setAlbumId(9999);  // Required for composite key and colocation
            Track retrieved = trackView.get(null, keyOnly);
            System.out.println("✓ Retrieved: " + retrieved);
            
            // Clean up
            boolean deleted = trackView.delete(null, keyOnly);
            System.out.println("✓ Cleanup successful: " + deleted);
            
            // Clean up prerequisites
            cleanupAlbum(client, 9999, 999);
            cleanupArtist(client, 999);
            cleanupGenre(client, 99);
            
        } catch (Exception e) {
            System.err.println("❌ Colocation demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates reference data pattern with different zone configuration.
     * Genre table: High replication for read-heavy reference data
     */
    private void demonstrateReferenceDataEntity(IgniteClient client) {
        System.out.println("\n--- Reference Data Entity Pattern: Genre ---");
        
        try {
            Table genreTable = client.tables().table("Genre");
            RecordView<Genre> genreView = genreTable.recordView(Genre.class);
            
            System.out.println("Annotation features demonstrated:");
            System.out.println("- @Zone: Different zone for reference data");
            System.out.println("- Higher replication: Better read performance");
            System.out.println("- Read-heavy workload optimization");
            System.out.println("- Lookup table pattern");
            
            // Insert sample genre
            Genre genre = new Genre(999, "Demo Genre");
            genreView.upsert(null, genre);
            System.out.println("✓ Inserted: " + genre);
            
            // Retrieve by primary key
            Genre keyOnly = new Genre();
            keyOnly.setGenreId(999);
            Genre retrieved = genreView.get(null, keyOnly);
            System.out.println("✓ Retrieved: " + retrieved);
            System.out.println("  → Available from multiple replicas for fast reads");
            
            // Clean up
            boolean deleted = genreView.delete(null, keyOnly);
            System.out.println("✓ Cleanup successful: " + deleted);
            
        } catch (Exception e) {
            System.err.println("❌ Reference data demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates complex entity with multiple relationships and indexes.
     * Customer table: Many fields, indexes, foreign key relationships
     */
    private void demonstrateComplexEntity(IgniteClient client) {
        System.out.println("\n--- Complex Entity Pattern: Customer ---");
        
        try {
            Table customerTable = client.tables().table("Customer");
            RecordView<Customer> customerView = customerTable.recordView(Customer.class);
            
            System.out.println("Annotation features demonstrated:");
            System.out.println("- @Index: Multiple secondary indexes");
            System.out.println("- Complex field mapping with various data types");
            System.out.println("- Nullable vs non-nullable columns");
            System.out.println("- String length constraints");
            
            // Insert sample customer with many fields
            Customer customer = new Customer();
            customer.setCustomerId(9999);
            customer.setFirstName("Demo");
            customer.setLastName("Customer");
            customer.setEmail("demo.customer@example.com");
            customer.setCity("Demo City");
            customer.setCountry("Demo Country");
            customer.setPhone("+1-555-0123");
            
            customerView.upsert(null, customer);
            System.out.println("✓ Inserted: " + customer);
            
            // Retrieve by primary key
            Customer keyOnly = new Customer();
            keyOnly.setCustomerId(9999);
            Customer retrieved = customerView.get(null, keyOnly);
            System.out.println("✓ Retrieved: " + retrieved);
            System.out.println("  → Indexes enable fast lookups by email, location, etc.");
            
            // Clean up
            boolean deleted = customerView.delete(null, keyOnly);
            System.out.println("✓ Cleanup successful: " + deleted);
            
        } catch (Exception e) {
            System.err.println("❌ Complex entity demo failed: " + e.getMessage());
        }
    }
    
    // Helper methods for managing test data dependencies
    
    private void ensureArtistExists(IgniteClient client, Integer artistId) {
        try {
            RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
            Artist artist = new Artist(artistId, "Demo Artist");
            artistView.upsert(null, artist);
        } catch (Exception e) {
            // Ignore if artist already exists
        }
    }
    
    private void ensureAlbumExists(IgniteClient client, Integer albumId, Integer artistId) {
        try {
            RecordView<Album> albumView = client.tables().table("Album").recordView(Album.class);
            Album album = new Album(albumId, artistId, "Demo Album");
            albumView.upsert(null, album);
        } catch (Exception e) {
            // Ignore if album already exists
        }
    }
    
    private void ensureGenreExists(IgniteClient client, Integer genreId) {
        try {
            RecordView<Genre> genreView = client.tables().table("Genre").recordView(Genre.class);
            Genre genre = new Genre(genreId, "Demo Genre");
            genreView.upsert(null, genre);
        } catch (Exception e) {
            // Ignore if genre already exists
        }
    }
    
    private void cleanupArtist(IgniteClient client, Integer artistId) {
        try {
            RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
            Artist key = new Artist();
            key.setArtistId(artistId);
            artistView.delete(null, key);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    private void cleanupAlbum(IgniteClient client, Integer albumId, Integer artistId) {
        try {
            RecordView<Album> albumView = client.tables().table("Album").recordView(Album.class);
            Album key = new Album();
            key.setAlbumId(albumId);
            key.setArtistId(artistId);
            albumView.delete(null, key);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    private void cleanupGenre(IgniteClient client, Integer genreId) {
        try {
            RecordView<Genre> genreView = client.tables().table("Genre").recordView(Genre.class);
            Genre key = new Genre();
            key.setGenreId(genreId);
            genreView.delete(null, key);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
}