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
 * Data loading utilities for Apache Ignite 3 music store dataset.
 * 
 * Demonstrates transactional data loading patterns with proper
 * dependency ordering and colocation-aware operations.
 */
public class DataLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);
    
    /**
     * Loads core sample music store data.
     * 
     * @param client Connected Ignite client
     */
    public static void loadCoreData(IgniteClient client) {
        logger.info("Loading core sample data...");
        
        try {
            client.transactions().runInTransaction(tx -> {
                loadReferenceData(client, tx);
                loadMusicData(client, tx);
                loadBusinessData(client, tx);
            });
            
            logger.info("Core sample data loaded successfully");
            
        } catch (Exception e) {
            logger.error("Failed to load core data: {}", e.getMessage());
            throw new RuntimeException("Core data loading failed", e);
        }
    }
    
    /**
     * Loads extended sample data including playlists and additional content.
     * 
     * @param client Connected Ignite client
     */
    public static void loadExtendedData(IgniteClient client) {
        logger.info("Loading extended sample data...");
        
        try {
            client.transactions().runInTransaction(tx -> {
                loadPlaylistData(client, tx);
                loadAdditionalTracks(client, tx);
            });
            
            logger.info("Extended sample data loaded successfully");
            
        } catch (Exception e) {
            logger.error("Failed to load extended data: {}", e.getMessage());
            throw new RuntimeException("Extended data loading failed", e);
        }
    }
    
    private static void loadReferenceData(IgniteClient client, Transaction tx) {
        Table genreTable = client.tables().table("Genre");
        RecordView<Genre> genreView = genreTable.recordView(Genre.class);
        
        genreView.upsert(tx, new Genre(1, "Rock"));
        genreView.upsert(tx, new Genre(2, "Jazz"));
        genreView.upsert(tx, new Genre(3, "Metal"));
        genreView.upsert(tx, new Genre(4, "Alternative & Punk"));
        genreView.upsert(tx, new Genre(5, "Blues"));
        
        Table mediaTypeTable = client.tables().table("MediaType");
        RecordView<MediaType> mediaTypeView = mediaTypeTable.recordView(MediaType.class);
        
        mediaTypeView.upsert(tx, new MediaType(1, "MPEG audio file"));
        mediaTypeView.upsert(tx, new MediaType(2, "Protected AAC audio file"));
        mediaTypeView.upsert(tx, new MediaType(3, "Protected MPEG-4 video file"));
    }
    
    private static void loadMusicData(IgniteClient client, Transaction tx) {
        Table artistTable = client.tables().table("Artist");
        RecordView<Artist> artistView = artistTable.recordView(Artist.class);
        
        artistView.upsert(tx, new Artist(1, "AC/DC"));
        artistView.upsert(tx, new Artist(2, "Accept"));
        artistView.upsert(tx, new Artist(3, "Aerosmith"));
        artistView.upsert(tx, new Artist(4, "Black Sabbath"));
        artistView.upsert(tx, new Artist(5, "Led Zeppelin"));
        
        Table albumTable = client.tables().table("Album");
        RecordView<Album> albumView = albumTable.recordView(Album.class);
        
        albumView.upsert(tx, new Album(1, 1, "For Those About To Rock We Salute You"));
        albumView.upsert(tx, new Album(2, 2, "Balls to the Wall"));
        albumView.upsert(tx, new Album(3, 2, "Restless and Wild"));
        albumView.upsert(tx, new Album(4, 3, "Big Ones"));
        albumView.upsert(tx, new Album(5, 4, "Paranoid"));
        
        Table trackTable = client.tables().table("Track");
        RecordView<Track> trackView = trackTable.recordView(Track.class);
        
        trackView.upsert(tx, new Track(1, 1, "For Those About To Rock (We Salute You)", 1, 1, "Angus Young, Malcolm Young, Brian Johnson", 343719, 11170334, new BigDecimal("0.99")));
        trackView.upsert(tx, new Track(2, 2, "Balls to the Wall", 1, 1, null, 342562, 5510424, new BigDecimal("0.99")));
        trackView.upsert(tx, new Track(3, 3, "Fast As a Shark", 1, 1, "F. Baltes, S. Kaufman, U. Dirkscneider & W. Hoffman", 230619, 3990994, new BigDecimal("0.99")));
        trackView.upsert(tx, new Track(4, 4, "Walk On Water", 1, 1, null, 295680, 4896186, new BigDecimal("0.99")));
        trackView.upsert(tx, new Track(5, 5, "Paranoid", 3, 1, "Anthony Iommi, William Ward, John Osbourne, Terence Butler", 162562, 2611465, new BigDecimal("0.99")));
    }
    
    private static void loadBusinessData(IgniteClient client, Transaction tx) {
        Table customerTable = client.tables().table("Customer");
        RecordView<Customer> customerView = customerTable.recordView(Customer.class);
        
        customerView.upsert(tx, new Customer(1, "Luís", "Gonçalves", "Embraer - Empresa Brasileira de Aeronáutica S.A.", "Av. Brigadeiro Faria Lima, 2170", "São José dos Campos", "SP", "Brazil", "12227-000", "+55 (12) 3923-5555", "+55 (12) 3923-5566", "luisg@embraer.com.br", 3));
        customerView.upsert(tx, new Customer(2, "Leonie", "Köhler", null, "Theodor-Heuss-Straße 34", "Stuttgart", null, "Germany", "70174", "+49 0711 2842222", null, "leonekohler@surfeu.de", 5));
        customerView.upsert(tx, new Customer(3, "François", "Tremblay", null, "1498 rue Bélanger", "Montréal", "QC", "Canada", "H2G 1A7", "+1 (514) 721-4711", null, "ftremblay@gmail.com", 3));
        
        Table employeeTable = client.tables().table("Employee");
        RecordView<Employee> employeeView = employeeTable.recordView(Employee.class);
        
        employeeView.upsert(tx, new Employee(1, "Adams", "Andrew", "General Manager", null, LocalDate.of(1962, 2, 18), LocalDate.of(2002, 8, 14), "11120 Jasper Ave NW", "Edmonton", "AB", "Canada", "T5K 2N1", "+1 (780) 428-9482", "+1 (780) 428-3457", "andrew@chinookcorp.com"));
        employeeView.upsert(tx, new Employee(2, "Edwards", "Nancy", "Sales Manager", 1, LocalDate.of(1958, 12, 8), LocalDate.of(2002, 5, 1), "825 8 Ave SW", "Calgary", "AB", "Canada", "T2P 2T3", "+1 (403) 262-3443", "+1 (403) 262-3322", "nancy@chinookcorp.com"));
        employeeView.upsert(tx, new Employee(3, "Peacock", "Jane", "Sales Support Agent", 2, LocalDate.of(1973, 8, 29), LocalDate.of(2002, 4, 1), "1111 6 Ave SW", "Calgary", "AB", "Canada", "T2P 5M5", "+1 (403) 262-3443", "+1 (403) 262-6712", "jane@chinookcorp.com"));
        
        Table invoiceTable = client.tables().table("Invoice");
        RecordView<Invoice> invoiceView = invoiceTable.recordView(Invoice.class);
        
        invoiceView.upsert(tx, new Invoice(1, 2, LocalDate.of(2009, 1, 1), "Theodor-Heuss-Straße 34", "Stuttgart", null, "Germany", "70174", new BigDecimal("1.98")));
        invoiceView.upsert(tx, new Invoice(2, 3, LocalDate.of(2009, 1, 2), "1498 rue Bélanger", "Montréal", "QC", "Canada", "H2G 1A7", new BigDecimal("3.96")));
        
        Table invoiceLineTable = client.tables().table("InvoiceLine");
        RecordView<InvoiceLine> invoiceLineView = invoiceLineTable.recordView(InvoiceLine.class);
        
        invoiceLineView.upsert(tx, new InvoiceLine(1, 1, 2, new BigDecimal("0.99"), 1));
        invoiceLineView.upsert(tx, new InvoiceLine(2, 1, 4, new BigDecimal("0.99"), 1));
        invoiceLineView.upsert(tx, new InvoiceLine(3, 2, 1, new BigDecimal("0.99"), 1));
        invoiceLineView.upsert(tx, new InvoiceLine(4, 2, 3, new BigDecimal("0.99"), 1));
    }
    
    private static void loadPlaylistData(IgniteClient client, Transaction tx) {
        Table playlistTable = client.tables().table("Playlist");
        RecordView<Playlist> playlistView = playlistTable.recordView(Playlist.class);
        
        playlistView.upsert(tx, new Playlist(1, "Music"));
        playlistView.upsert(tx, new Playlist(2, "Movies"));
        playlistView.upsert(tx, new Playlist(3, "TV Shows"));
        playlistView.upsert(tx, new Playlist(8, "Heavy Metal Classic"));
        
        Table playlistTrackTable = client.tables().table("PlaylistTrack");
        RecordView<PlaylistTrack> playlistTrackView = playlistTrackTable.recordView(PlaylistTrack.class);
        
        playlistTrackView.upsert(tx, new PlaylistTrack(1, 1));
        playlistTrackView.upsert(tx, new PlaylistTrack(1, 2));
        playlistTrackView.upsert(tx, new PlaylistTrack(1, 3));
        playlistTrackView.upsert(tx, new PlaylistTrack(8, 5));
    }
    
    private static void loadAdditionalTracks(IgniteClient client, Transaction tx) {
        Table trackTable = client.tables().table("Track");
        RecordView<Track> trackView = trackTable.recordView(Track.class);
        
        trackView.upsert(tx, new Track(6, 1, "Put The Finger On You", 1, 1, "Angus Young, Malcolm Young, Brian Johnson", 205662, 6713451, new BigDecimal("0.99")));
        trackView.upsert(tx, new Track(7, 1, "Let's Get It Up", 1, 1, "Angus Young, Malcolm Young, Brian Johnson", 233926, 7636561, new BigDecimal("0.99")));
        trackView.upsert(tx, new Track(8, 2, "Love Child", 1, 1, null, 225543, 4128558, new BigDecimal("0.99")));
    }
}