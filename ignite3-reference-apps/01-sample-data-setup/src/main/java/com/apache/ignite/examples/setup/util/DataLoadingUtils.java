package com.apache.ignite.examples.setup.util;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;
import org.apache.ignite.tx.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apache.ignite.examples.setup.model.Album;
import com.apache.ignite.examples.setup.model.Artist;
import com.apache.ignite.examples.setup.model.Customer;
import com.apache.ignite.examples.setup.model.Employee;
import com.apache.ignite.examples.setup.model.Genre;
import com.apache.ignite.examples.setup.model.Invoice;
import com.apache.ignite.examples.setup.model.InvoiceLine;
import com.apache.ignite.examples.setup.model.MediaType;
import com.apache.ignite.examples.setup.model.Playlist;
import com.apache.ignite.examples.setup.model.PlaylistTrack;
import com.apache.ignite.examples.setup.model.Track;

/**
 * Data loading utilities demonstrating transactional data operations in Ignite 3.
 * 
 * This class showcases distributed systems patterns:
 * - Transactional Data Loading: All related data loaded atomically  
 * - POJO-based Operations: Type-safe data operations with RecordView
 * - Colocation-Aware Loading: Data loaded respecting colocation strategies
 * - Dependency Order: Related entities loaded in correct sequence
 * - Error Recovery: Transaction rollback on failures
 * 
 * Key Concepts Demonstrated:
 * - client.transactions().runInTransaction() - Functional transaction pattern
 * - RecordView<T> for type-safe table operations 
 * - ACID guarantees across multiple tables and partitions
 * - Realistic sample data for meaningful testing and development
 * 
 * The loading strategy ensures data consistency across the distributed cluster
 * while demonstrating proper patterns for production applications.
 */
public class DataLoadingUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(DataLoadingUtils.class);
    
    /**
     * Loads complete sample music store data using a single transaction.
     * 
     * This demonstrates the recommended pattern for loading related data:
     * 1. Use functional transactions (runInTransaction) for automatic cleanup
     * 2. Load data in dependency order (reference -> root -> dependent entities)
     * 3. Leverage colocation by loading related entities together
     * 4. All operations succeed or all are rolled back (ACID guarantee)
     * 
     * The transaction spans multiple tables and potentially multiple cluster nodes,
     * showing Ignite 3's distributed transaction capabilities.
     * 
     * @param client Connected Ignite client for data operations
     * @throws RuntimeException if data loading fails
     */
    public static void loadSampleData(IgniteClient client) {
        logger.info("Loading sample music store data...");
        
        try {
            // Use functional transaction pattern - automatic commit/rollback
            // All data loading happens in a single distributed transaction
            client.transactions().runInTransaction(tx -> {
                loadReferenceData(client, (Transaction) tx);
                loadMusicData(client, (Transaction) tx);
                loadBusinessData(client, (Transaction) tx);
                loadPlaylistData(client, (Transaction) tx);
            });
            
            logger.info("Sample data loaded successfully");
            
        } catch (Exception e) {
            logger.error("Failed to load sample data: {}", e.getMessage());
            throw new RuntimeException("Failed to load sample data", e);
        }
    }
    
    private static void loadReferenceData(IgniteClient client, Transaction tx) {
        logger.info("Loading reference data...");
        
        Table genreTable = client.tables().table("Genre");
        RecordView<Genre> genreView = genreTable.recordView(Genre.class);
        
        genreView.upsert(tx, new Genre(1, "Rock"));
        genreView.upsert(tx, new Genre(2, "Jazz"));
        genreView.upsert(tx, new Genre(3, "Metal"));
        genreView.upsert(tx, new Genre(4, "Alternative & Punk"));
        genreView.upsert(tx, new Genre(5, "Rock And Roll"));
        
        Table mediaTypeTable = client.tables().table("MediaType");
        RecordView<MediaType> mediaTypeView = mediaTypeTable.recordView(MediaType.class);
        
        mediaTypeView.upsert(tx, new MediaType(1, "MPEG audio file"));
        mediaTypeView.upsert(tx, new MediaType(2, "Protected AAC audio file"));
        mediaTypeView.upsert(tx, new MediaType(3, "Protected MPEG-4 video file"));
        mediaTypeView.upsert(tx, new MediaType(4, "Purchased AAC audio file"));
        mediaTypeView.upsert(tx, new MediaType(5, "AAC audio file"));
        
        logger.info("Reference data loaded");
    }
    
    private static void loadMusicData(IgniteClient client, Transaction tx) {
        logger.info("Loading music data...");
        
        Table artistTable = client.tables().table("Artist");
        RecordView<Artist> artistView = artistTable.recordView(Artist.class);
        
        artistView.upsert(tx, new Artist(1, "AC/DC"));
        artistView.upsert(tx, new Artist(2, "Accept"));
        artistView.upsert(tx, new Artist(3, "Aerosmith"));
        artistView.upsert(tx, new Artist(4, "Alanis Morissette"));
        artistView.upsert(tx, new Artist(5, "Alice In Chains"));
        
        Table albumTable = client.tables().table("Album");
        RecordView<Album> albumView = albumTable.recordView(Album.class);
        
        albumView.upsert(tx, new Album(1, 1, "For Those About To Rock We Salute You"));
        albumView.upsert(tx, new Album(2, 2, "Balls to the Wall"));
        albumView.upsert(tx, new Album(3, 2, "Restless and Wild"));
        albumView.upsert(tx, new Album(4, 1, "Let There Be Rock"));
        albumView.upsert(tx, new Album(5, 3, "Big Ones"));
        
        Table trackTable = client.tables().table("Track");
        RecordView<Track> trackView = trackTable.recordView(Track.class);
        
        trackView.upsert(tx, new Track(1, 1, "For Those About To Rock (We Salute You)", 1, 1, 
                                     "Angus Young, Malcolm Young, Brian Johnson", 343719, 11170334, new BigDecimal("0.99")));
        trackView.upsert(tx, new Track(2, 1, "Put The Finger On You", 1, 1, 
                                     "Angus Young, Malcolm Young, Brian Johnson", 205662, 6713451, new BigDecimal("0.99")));
        trackView.upsert(tx, new Track(3, 1, "Let's Get It Up", 1, 1, 
                                     "Angus Young, Malcolm Young, Brian Johnson", 233926, 7636561, new BigDecimal("0.99")));
        trackView.upsert(tx, new Track(4, 1, "Inject The Venom", 1, 1, 
                                     "Angus Young, Malcolm Young, Brian Johnson", 210834, 6852860, new BigDecimal("0.99")));
        trackView.upsert(tx, new Track(5, 1, "Snowballed", 1, 1, 
                                     "Angus Young, Malcolm Young, Brian Johnson", 203102, 6599424, new BigDecimal("0.99")));
        
        logger.info("Music data loaded");
    }
    
    private static void loadBusinessData(IgniteClient client, Transaction tx) {
        logger.info("Loading business data...");
        
        Table employeeTable = client.tables().table("Employee");
        RecordView<Employee> employeeView = employeeTable.recordView(Employee.class);
        
        employeeView.upsert(tx, new Employee(1, "Adams", "Andrew", "General Manager", null,
                                           LocalDate.of(1962, 2, 18), LocalDate.of(2002, 8, 14),
                                           "11120 Jasper Ave NW", "Edmonton", "AB", "Canada", "T5K 2N1",
                                           "+1 (780) 428-9482", "+1 (780) 428-3457", "andrew@musicstore.com"));
        
        employeeView.upsert(tx, new Employee(2, "Edwards", "Nancy", "Sales Manager", 1,
                                           LocalDate.of(1958, 12, 8), LocalDate.of(2002, 5, 1),
                                           "825 8 Ave SW", "Calgary", "AB", "Canada", "T2P 2T3",
                                           "+1 (403) 262-3443", "+1 (403) 262-3322", "nancy@musicstore.com"));
        
        Table customerTable = client.tables().table("Customer");
        RecordView<Customer> customerView = customerTable.recordView(Customer.class);
        
        customerView.upsert(tx, new Customer(1, "Luís", "Gonçalves", "Embraer - Empresa Brasileira de Aeronáutica S.A.",
                                           "Av. Brigadeiro Faria Lima, 2170", "São José dos Campos", "SP", "Brazil", "12227-000",
                                           "+55 (12) 3923-5555", "+55 (12) 3923-5566", "luisg@embraer.com.br", 3));
        
        customerView.upsert(tx, new Customer(2, "Leonie", "Köhler", null,
                                           "Theodor-Heuss-Straße 34", "Stuttgart", null, "Germany", "70174",
                                           "+49 0711 2842222", null, "leonekohler@surfeu.de", 5));
        
        Table invoiceTable = client.tables().table("Invoice");
        RecordView<Invoice> invoiceView = invoiceTable.recordView(Invoice.class);
        
        invoiceView.upsert(tx, new Invoice(1, 1, LocalDate.of(2009, 1, 1),
                                         "Av. Brigadeiro Faria Lima, 2170", "São José dos Campos", "SP", "Brazil", "12227-000",
                                         new BigDecimal("1.98")));
        
        invoiceView.upsert(tx, new Invoice(2, 2, LocalDate.of(2009, 1, 2),
                                         "Theodor-Heuss-Straße 34", "Stuttgart", null, "Germany", "70174",
                                         new BigDecimal("3.96")));
        
        Table invoiceLineTable = client.tables().table("InvoiceLine");
        RecordView<InvoiceLine> invoiceLineView = invoiceLineTable.recordView(InvoiceLine.class);
        
        invoiceLineView.upsert(tx, new InvoiceLine(1, 1, 1, new BigDecimal("0.99"), 1));
        invoiceLineView.upsert(tx, new InvoiceLine(2, 1, 2, new BigDecimal("0.99"), 1));
        invoiceLineView.upsert(tx, new InvoiceLine(3, 2, 3, new BigDecimal("0.99"), 1));
        invoiceLineView.upsert(tx, new InvoiceLine(4, 2, 4, new BigDecimal("0.99"), 1));
        
        logger.info("Business data loaded");
    }
    
    private static void loadPlaylistData(IgniteClient client, Transaction tx) {
        logger.info("Loading playlist data...");
        
        Table playlistTable = client.tables().table("Playlist");
        RecordView<Playlist> playlistView = playlistTable.recordView(Playlist.class);
        
        playlistView.upsert(tx, new Playlist(1, "Music"));
        playlistView.upsert(tx, new Playlist(2, "Movies"));
        playlistView.upsert(tx, new Playlist(3, "TV Shows"));
        playlistView.upsert(tx, new Playlist(4, "Audiobooks"));
        playlistView.upsert(tx, new Playlist(5, "90's Music"));
        
        Table playlistTrackTable = client.tables().table("PlaylistTrack");
        RecordView<PlaylistTrack> playlistTrackView = playlistTrackTable.recordView(PlaylistTrack.class);
        
        playlistTrackView.upsert(tx, new PlaylistTrack(1, 1));
        playlistTrackView.upsert(tx, new PlaylistTrack(1, 2));
        playlistTrackView.upsert(tx, new PlaylistTrack(1, 3));
        playlistTrackView.upsert(tx, new PlaylistTrack(5, 1));
        playlistTrackView.upsert(tx, new PlaylistTrack(5, 2));
        
        logger.info("Playlist data loaded");
    }
    
    public static void loadExtendedSampleData(IgniteClient client) {
        logger.info("Loading extended sample data...");
        
        try {
            client.transactions().runInTransaction(tx -> {
                loadAdditionalArtists(client, (Transaction) tx);
                loadAdditionalAlbums(client, (Transaction) tx);
                loadAdditionalTracks(client, (Transaction) tx);
            });
            
            logger.info("Extended sample data loaded successfully");
            
        } catch (Exception e) {
            logger.error("Failed to load extended sample data: {}", e.getMessage());
            throw new RuntimeException("Failed to load extended sample data", e);
        }
    }
    
    private static void loadAdditionalArtists(IgniteClient client, Transaction tx) {
        Table artistTable = client.tables().table("Artist");
        RecordView<Artist> artistView = artistTable.recordView(Artist.class);
        
        artistView.upsert(tx, new Artist(6, "Black Sabbath"));
        artistView.upsert(tx, new Artist(7, "Chico Buarque"));
        artistView.upsert(tx, new Artist(8, "Deep Purple"));
        artistView.upsert(tx, new Artist(9, "Metallica"));
        artistView.upsert(tx, new Artist(10, "Led Zeppelin"));
    }
    
    private static void loadAdditionalAlbums(IgniteClient client, Transaction tx) {
        Table albumTable = client.tables().table("Album");
        RecordView<Album> albumView = albumTable.recordView(Album.class);
        
        albumView.upsert(tx, new Album(6, 6, "Paranoid"));
        albumView.upsert(tx, new Album(7, 7, "Minha Historia"));
        albumView.upsert(tx, new Album(8, 8, "Machine Head"));
        albumView.upsert(tx, new Album(9, 9, "Master of Puppets"));
        albumView.upsert(tx, new Album(10, 10, "Led Zeppelin IV"));
    }
    
    private static void loadAdditionalTracks(IgniteClient client, Transaction tx) {
        Table trackTable = client.tables().table("Track");
        RecordView<Track> trackView = trackTable.recordView(Track.class);
        
        trackView.upsert(tx, new Track(6, 6, "Paranoid", 1, 3, "Tony Iommi, Ozzy Osbourne, Geezer Butler, Bill Ward", 170333, 5566397, new BigDecimal("0.99")));
        trackView.upsert(tx, new Track(7, 6, "Iron Man", 1, 3, "Tony Iommi, Ozzy Osbourne, Geezer Butler, Bill Ward", 356666, 11673881, new BigDecimal("0.99")));
        trackView.upsert(tx, new Track(8, 8, "Highway Star", 1, 1, "Ritchie Blackmore, Ian Gillan, Roger Glover, Jon Lord, Ian Paice", 386679, 12641711, new BigDecimal("0.99")));
        trackView.upsert(tx, new Track(9, 9, "Master of Puppets", 1, 3, "James Hetfield, Lars Ulrich, Kirk Hammett, Cliff Burton", 515333, 16863326, new BigDecimal("0.99")));
        trackView.upsert(tx, new Track(10, 10, "Stairway to Heaven", 1, 1, "Jimmy Page, Robert Plant", 482333, 15783426, new BigDecimal("0.99")));
    }
    
    public static void clearAllData(IgniteClient client) {
        logger.info("Clearing all data from music store tables...");
        
        try {
            String[] tables = {"PlaylistTrack", "Playlist", "InvoiceLine", "Invoice", 
                              "Track", "Album", "Artist", "Customer", "Employee", 
                              "MediaType", "Genre"};
            
            for (String table : tables) {
                if (DataSetupUtils.tableExists(client, table)) {
                    client.sql().execute(null, "DELETE FROM " + table);
                    logger.info("Cleared data from table: {}", table);
                }
            }
            
            logger.info("All data cleared successfully");
            
        } catch (Exception e) {
            logger.error("Failed to clear data: {}", e.getMessage());
            throw new RuntimeException("Failed to clear data", e);
        }
    }
}