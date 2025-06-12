package com.apache.ignite.examples.tableapi;

import com.apache.ignite.examples.setup.model.Artist;
import com.apache.ignite.examples.setup.model.Album;
import com.apache.ignite.examples.setup.model.Track;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Comprehensive demonstration of RecordView operations with Apache Ignite 3.
 * 
 * This application showcases object-oriented data access patterns using the music store dataset.
 * 
 * Key concepts demonstrated:
 * - RecordView setup and configuration
 * - CRUD operations with POJOs
 * - Bulk operations for high performance
 * - Transaction integration
 * - Error handling patterns
 * - Complex entity operations with composite keys
 * 
 * Prerequisites:
 * 1. Ignite cluster running (use 00-docker/init-cluster.sh)
 * 2. Music store schema and data loaded (use 01-sample-data-setup)
 * 
 * Learning Objectives:
 * - Master RecordView API for object-oriented data access
 * - Understand when to use RecordView vs KeyValueView vs SQL
 * - Learn bulk operation patterns for performance
 * - Practice transaction-aware data operations
 */
public class RecordViewOperations {
    
    private static final String CLUSTER_ENDPOINT = "127.0.0.1:10800";
    
    public static void main(String[] args) {
        System.out.println("=== Apache Ignite 3 RecordView Operations Demo ===");
        System.out.println("Demonstrating object-oriented data access with music store entities\n");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(CLUSTER_ENDPOINT)
                .build()) {
            
            System.out.println("✓ Connected to Ignite cluster at " + CLUSTER_ENDPOINT);
            
            // Execute all RecordView demonstrations
            demonstrateBasicRecordOperations(client);
            demonstrateComplexEntityOperations(client);
            demonstrateBulkOperations(client);
            demonstrateTransactionIntegration(client);
            demonstrateAsyncOperations(client);
            
            System.out.println("\n=== RecordView Operations Demo Complete ===");
            
        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Demonstrates basic CRUD operations with RecordView using Artist entities.
     * 
     * Concepts covered:
     * - RecordView setup with type safety
     * - Insert (upsert) operations
     * - Read operations by primary key
     * - Update operations (upsert with existing key)
     * - Delete operations
     * - Conditional operations
     */
    private static void demonstrateBasicRecordOperations(IgniteClient client) {
        System.out.println("\n--- Basic RecordView CRUD Operations ---");
        
        // Get table and create strongly-typed RecordView
        Table artistTable = client.tables().table("Artist");
        RecordView<Artist> artists = artistTable.recordView(Artist.class);
        
        System.out.println("✓ Created RecordView<Artist> with automatic POJO mapping");
        
        // CREATE: Insert new artists
        System.out.println("\n1. CREATE Operations:");
        Artist dreamTheater = new Artist(1000, "Dream Theater");
        artists.upsert(null, dreamTheater);
        System.out.println("   ✓ Inserted: " + dreamTheater);
        
        Artist tool = new Artist(1001, "Tool");
        artists.upsert(null, tool);
        System.out.println("   ✓ Inserted: " + tool);
        
        // READ: Get artists by primary key
        System.out.println("\n2. READ Operations:");
        Artist foundDreamTheater = artists.get(null, new Artist(1000, null));
        if (foundDreamTheater != null) {
            System.out.println("   ✓ Found by key: " + foundDreamTheater);
        } else {
            System.out.println("   ✗ Artist not found");
        }
        
        // Alternative: inline key creation
        Artist foundTool = artists.get(null, new Artist(1001, null));
        System.out.println("   ✓ Found by inline key: " + foundTool);
        
        // UPDATE: Modify existing artist
        System.out.println("\n3. UPDATE Operations:");
        if (foundDreamTheater != null) {
            foundDreamTheater.setName("Dream Theater (Progressive Metal)");
            artists.upsert(null, foundDreamTheater);
            System.out.println("   ✓ Updated artist name: " + foundDreamTheater.getName());
        }
        
        // Verify update
        Artist updatedArtist = artists.get(null, new Artist(1000, null));
        System.out.println("   ✓ Verified update: " + updatedArtist);
        
        // DELETE: Remove artists
        System.out.println("\n4. DELETE Operations:");
        boolean deleted = artists.delete(null, new Artist(1001, null));
        System.out.println("   ✓ Delete successful: " + deleted);
        
        // Verify deletion
        Artist shouldBeNull = artists.get(null, new Artist(1001, null));
        System.out.println("   ✓ Verified deletion (should be null): " + shouldBeNull);
        
        // Conditional delete with exact match
        boolean conditionalDelete = artists.deleteExact(null, foundDreamTheater);
        System.out.println("   ✓ Conditional delete: " + conditionalDelete);
    }
    
    /**
     * Demonstrates RecordView operations with complex entities having composite keys.
     * 
     * Concepts covered:
     * - Composite primary keys (AlbumId + ArtistId)
     * - Data colocation benefits
     * - Foreign key relationships
     * - Complex entity CRUD patterns
     */
    private static void demonstrateComplexEntityOperations(IgniteClient client) {
        System.out.println("\n--- Complex Entity Operations (Albums & Tracks) ---");
        
        // First, ensure we have an artist to work with
        Table artistTable = client.tables().table("Artist");
        RecordView<Artist> artists = artistTable.recordView(Artist.class);
        Artist progressiveArtist = new Artist(2000, "Porcupine Tree");
        artists.upsert(null, progressiveArtist);
        System.out.println("✓ Setup artist: " + progressiveArtist);
        
        // Work with Albums (composite primary key: AlbumId + ArtistId)
        Table albumTable = client.tables().table("Album");
        RecordView<Album> albums = albumTable.recordView(Album.class);
        
        System.out.println("\n1. Album Operations (Composite Keys):");
        
        // Create albums with composite keys
        Album fearOfABlankPlanet = new Album(2000, 2000, "Fear of a Blank Planet");
        Album inAbsentia = new Album(2001, 2000, "In Absentia");
        
        albums.upsert(null, fearOfABlankPlanet);
        albums.upsert(null, inAbsentia);
        System.out.println("   ✓ Inserted album: " + fearOfABlankPlanet);
        System.out.println("   ✓ Inserted album: " + inAbsentia);
        
        // Read albums using composite keys
        Album foundAlbum = albums.get(null, new Album(2000, 2000, null));
        System.out.println("   ✓ Found album by composite key: " + foundAlbum);
        
        // Work with Tracks (even more complex with additional foreign keys)
        Table trackTable = client.tables().table("Track");
        RecordView<Track> tracks = trackTable.recordView(Track.class);
        
        System.out.println("\n2. Track Operations (Complex Entities):");
        
        // Create tracks with rich metadata
        Track track1 = new Track();
        track1.setTrackId(2000);
        track1.setAlbumId(2000);  // References Fear of a Blank Planet
        track1.setName("Fear of a Blank Planet");
        track1.setMediaTypeId(1);  // Assuming MP3
        track1.setGenreId(1);      // Assuming Rock
        track1.setComposer("Steven Wilson");
        track1.setMilliseconds(402000);  // ~6.7 minutes
        track1.setBytes(12500000);       // ~12.5 MB
        track1.setUnitPrice(new BigDecimal("1.29"));
        
        Track track2 = new Track();
        track2.setTrackId(2001);
        track2.setAlbumId(2000);
        track2.setName("My Ashes");
        track2.setMediaTypeId(1);
        track2.setGenreId(1);
        track2.setComposer("Steven Wilson");
        track2.setMilliseconds(309000);  // ~5.15 minutes
        track2.setBytes(9800000);        // ~9.8 MB
        track2.setUnitPrice(new BigDecimal("1.29"));
        
        tracks.upsert(null, track1);
        tracks.upsert(null, track2);
        System.out.println("   ✓ Inserted track: " + track1.getName() + " (" + 
                          track1.getMilliseconds()/1000 + "s)");
        System.out.println("   ✓ Inserted track: " + track2.getName() + " (" + 
                          track2.getMilliseconds()/1000 + "s)");
        
        // Read tracks using composite keys
        Track foundTrack = tracks.get(null, new Track(2000, 2000, null, null, null, null, null, null, null));
        System.out.println("   ✓ Found track by composite key: " + foundTrack.getName());
        
        // Update track metadata
        if (foundTrack != null) {
            foundTrack.setComposer("Steven Wilson, Richard Barbieri");
            tracks.upsert(null, foundTrack);
            System.out.println("   ✓ Updated track composer: " + foundTrack.getComposer());
        }
        
        System.out.println("\n✓ Complex entity operations demonstrate colocation benefits:");
        System.out.println("  - Artist, Album, and Track data for ArtistId=2000 are on the same node");
        System.out.println("  - This enables fast queries across the entire hierarchy");
    }
    
    /**
     * Demonstrates bulk operations for high-performance data loading and processing.
     * 
     * Concepts covered:
     * - Bulk insert with upsertAll
     * - Bulk read with getAll
     * - Bulk delete with deleteAll
     * - Performance benefits of batching
     * - Order preservation in bulk operations
     */
    private static void demonstrateBulkOperations(IgniteClient client) {
        System.out.println("\n--- Bulk Operations for High Performance ---");
        
        Table artistTable = client.tables().table("Artist");
        RecordView<Artist> artists = artistTable.recordView(Artist.class);
        
        System.out.println("\n1. Bulk Insert Operations:");
        
        // Create a batch of progressive metal artists
        List<Artist> progressiveArtists = Arrays.asList(
            new Artist(3000, "Opeth"),
            new Artist(3001, "Mastodon"),
            new Artist(3002, "Gojira"),
            new Artist(3003, "Periphery"),
            new Artist(3004, "Animals as Leaders"),
            new Artist(3005, "TesseracT"),
            new Artist(3006, "Haken"),
            new Artist(3007, "Thank You Scientist")
        );
        
        // Bulk upsert - highly optimized for network and processing
        artists.upsertAll(null, progressiveArtists);
        System.out.println("   ✓ Bulk inserted " + progressiveArtists.size() + " progressive metal artists");
        
        System.out.println("\n2. Bulk Read Operations:");
        
        // Prepare keys for bulk retrieval
        List<Artist> artistKeys = Arrays.asList(
            new Artist(3000, null),  // Opeth
            new Artist(3001, null),  // Mastodon
            new Artist(3002, null),  // Gojira
            new Artist(3003, null)   // Periphery
        );
        
        // Bulk get operation - maintains order
        List<Artist> foundArtists = artists.getAll(null, artistKeys);
        
        System.out.println("   ✓ Bulk retrieved " + foundArtists.size() + " artists:");
        for (int i = 0; i < artistKeys.size(); i++) {
            Artist key = artistKeys.get(i);
            Artist found = foundArtists.get(i);  // Order is preserved
            
            if (found != null) {
                System.out.println("     " + key.getArtistId() + " → " + found.getName());
            } else {
                System.out.println("     " + key.getArtistId() + " → NOT FOUND");
            }
        }
        
        System.out.println("\n3. Bulk Delete Operations:");
        
        // Delete some artists in bulk
        List<Artist> artistsToDelete = Arrays.asList(
            new Artist(3004, null),  // Animals as Leaders
            new Artist(3005, null),  // TesseracT
            new Artist(3006, null)   // Haken
        );
        
        List<Artist> deletedKeys = artists.deleteAll(null, artistsToDelete);
        System.out.println("   ✓ Bulk deleted " + deletedKeys.size() + " artists:");
        deletedKeys.forEach(deleted -> 
            System.out.println("     Deleted: " + deleted.getArtistId()));
        
        System.out.println("\n✓ Bulk operations provide significant performance benefits:");
        System.out.println("  - Single network round trip for multiple records");
        System.out.println("  - Server-side batch optimizations");
        System.out.println("  - Reduced connection overhead");
        System.out.println("  - Order preservation for predictable results");
    }
    
    /**
     * Demonstrates RecordView integration with Ignite transactions.
     * 
     * Concepts covered:
     * - Transaction-aware RecordView operations
     * - ACID guarantees across multiple tables
     * - Rollback scenarios
     * - Distributed transaction coordination
     */
    private static void demonstrateTransactionIntegration(IgniteClient client) {
        System.out.println("\n--- Transaction Integration ---");
        
        // Get RecordViews for multiple tables
        RecordView<Artist> artists = client.tables().table("Artist").recordView(Artist.class);
        RecordView<Album> albums = client.tables().table("Album").recordView(Album.class);
        RecordView<Track> tracks = client.tables().table("Track").recordView(Track.class);
        
        System.out.println("\n1. Multi-Table Transaction (Artist + Album + Tracks):");
        
        // Execute complex operation in a single transaction
        client.transactions().runInTransaction(tx -> {
            // Add new artist
            Artist newArtist = new Artist(4000, "Between the Buried and Me");
            artists.upsert(tx, newArtist);
            System.out.println("   ✓ Inserted artist in transaction: " + newArtist.getName());
            
            // Add album for this artist
            Album newAlbum = new Album(4000, 4000, "Colors");
            albums.upsert(tx, newAlbum);
            System.out.println("   ✓ Inserted album in transaction: " + newAlbum.getTitle());
            
            // Add multiple tracks for this album
            List<Track> albumTracks = Arrays.asList(
                createTrack(4000, 4000, "Foam Born (A) The Backtrack", 103000),
                createTrack(4001, 4000, "Foam Born (B) The Decade of Statues", 238000),
                createTrack(4002, 4000, "Informal Gluttony", 343000),
                createTrack(4003, 4000, "Sun of Nothing", 465000)
            );
            
            tracks.upsertAll(tx, albumTracks);
            System.out.println("   ✓ Inserted " + albumTracks.size() + " tracks in transaction");
            
            System.out.println("   ✓ All operations committed atomically");
            return true;  // Commit transaction
        });
        
        System.out.println("\n2. Transaction Rollback Demonstration:");
        
        try {
            client.transactions().runInTransaction(tx -> {
                // This operation will succeed
                Artist testArtist = new Artist(4001, "Test Artist");
                artists.upsert(tx, testArtist);
                System.out.println("   ✓ Inserted test artist: " + testArtist.getName());
                
                // This will cause the transaction to rollback
                throw new RuntimeException("Simulated error - rolling back transaction");
            });
        } catch (Exception e) {
            System.out.println("   ✓ Transaction rolled back due to: " + e.getMessage());
        }
        
        // Verify rollback - test artist should not exist
        Artist shouldNotExist = artists.get(null, new Artist(4001, null));
        System.out.println("   ✓ Verified rollback - test artist exists: " + (shouldNotExist != null));
        
        System.out.println("\n✓ Transaction benefits:");
        System.out.println("  - ACID guarantees across multiple tables and nodes");
        System.out.println("  - Automatic rollback on failures");
        System.out.println("  - Isolation from concurrent operations");
        System.out.println("  - Consistency across distributed data");
    }
    
    /**
     * Demonstrates asynchronous RecordView operations for high-performance applications.
     * 
     * Concepts covered:
     * - Async CRUD operations with CompletableFuture
     * - Chaining async operations
     * - Parallel async execution
     * - Error handling in async contexts
     */
    private static void demonstrateAsyncOperations(IgniteClient client) {
        System.out.println("\n--- Asynchronous Operations ---");
        
        RecordView<Artist> artists = client.tables().table("Artist").recordView(Artist.class);
        
        System.out.println("\n1. Basic Async Operations:");
        
        // Async insert
        Artist asyncArtist = new Artist(5000, "Plini");
        CompletableFuture<Void> insertFuture = artists.upsertAsync(null, asyncArtist);
        
        insertFuture
            .thenRun(() -> System.out.println("   ✓ Async insert completed: " + asyncArtist.getName()))
            .exceptionally(throwable -> {
                System.err.println("   ✗ Async insert failed: " + throwable.getMessage());
                return null;
            })
            .join();  // Wait for completion in demo
        
        // Async get
        CompletableFuture<Artist> getFuture = artists.getAsync(null, new Artist(5000, null));
        
        getFuture
            .thenAccept(found -> {
                if (found != null) {
                    System.out.println("   ✓ Async get completed: " + found.getName());
                } else {
                    System.out.println("   ✗ Artist not found");
                }
            })
            .join();  // Wait for completion in demo
        
        System.out.println("\n2. Chained Async Operations:");
        
        // Chain multiple async operations
        artists.getAsync(null, new Artist(5000, null))
            .thenCompose(artist -> {
                if (artist != null) {
                    // Update the artist name
                    artist.setName("Plini (Instrumental Progressive)");
                    return artists.upsertAsync(null, artist);
                } else {
                    return CompletableFuture.completedFuture(null);
                }
            })
            .thenRun(() -> System.out.println("   ✓ Chained async operations completed"))
            .exceptionally(throwable -> {
                System.err.println("   ✗ Chained operations failed: " + throwable.getMessage());
                return null;
            })
            .join();  // Wait for completion in demo
        
        System.out.println("\n3. Parallel Async Operations:");
        
        // Execute multiple operations in parallel
        String[] instrumentalArtists = {"Intervals", "Polyphia", "Chon", "TTNG"};
        
        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] futures = new CompletableFuture[instrumentalArtists.length];
        
        for (int i = 0; i < instrumentalArtists.length; i++) {
            Artist artist = new Artist(5100 + i, instrumentalArtists[i]);
            futures[i] = artists.upsertAsync(null, artist);
        }
        
        // Wait for all parallel operations to complete
        CompletableFuture.allOf(futures)
            .thenRun(() -> System.out.println("   ✓ All " + instrumentalArtists.length + 
                                           " parallel async operations completed"))
            .join();  // Wait for completion in demo
        
        System.out.println("\n✓ Async operation benefits:");
        System.out.println("  - Non-blocking execution for high throughput");
        System.out.println("  - Ability to chain complex workflows");
        System.out.println("  - Parallel execution for independent operations");
        System.out.println("  - Better resource utilization");
    }
    
    /**
     * Helper method to create Track instances with standard metadata.
     */
    private static Track createTrack(Integer trackId, Integer albumId, String name, Integer milliseconds) {
        Track track = new Track();
        track.setTrackId(trackId);
        track.setAlbumId(albumId);
        track.setName(name);
        track.setMediaTypeId(1);  // MP3
        track.setGenreId(1);      // Rock
        track.setComposer("Between the Buried and Me");
        track.setMilliseconds(milliseconds);
        track.setBytes(milliseconds * 320);  // Rough estimate for 320kbps MP3
        track.setUnitPrice(new BigDecimal("1.29"));
        return track;
    }
}