/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        try {
            client.transactions().runInTransaction(tx -> {
                logger.info("--- Step 1: Loading reference data (genres, media types)");
                logger.info(">>> Loading: genres");
                loadGenres(client, tx);
                logger.info("<<< Loaded: 5 genres");
                logger.info(">>> Loading: media types");
                loadMediaTypes(client, tx);
                logger.info("<<< Loaded: 3 media types");
                
                logger.info("--- Step 2: Loading music entities (artists, albums, tracks)");
                logger.info(">>> Loading: artists");
                loadArtists(client, tx);
                logger.info("<<< Loaded: 5 artists");
                logger.info(">>> Loading: albums");
                loadAlbums(client, tx);
                logger.info("<<< Loaded: 5 albums");
                logger.info(">>> Loading: tracks");
                loadTracks(client, tx);
                logger.info("<<< Loaded: 5 tracks");
                
                logger.info("--- Step 3: Loading business entities (customers, employees, invoices)");
                logger.info(">>> Loading: customers");
                loadCustomers(client, tx);
                logger.info("<<< Loaded: 3 customers");
                logger.info(">>> Loading: employees");
                loadEmployees(client, tx);
                logger.info("<<< Loaded: 3 employees");
                logger.info(">>> Loading: invoices");
                loadInvoices(client, tx);
                logger.info("<<< Loaded: 2 invoices");
            });
            
        } catch (Exception e) {
            logger.error("Core data loading failed: {}", e.getMessage());
            throw new RuntimeException("Core data loading failed", e);
        }
    }
    
    /**
     * Loads complete music store dataset using optimized batch processing.
     * 
     * Uses native Ignite APIs with parallel loading for maximum performance.
     * Processes data directly from SQL script without character parsing overhead.
     * 
     * @param client Connected Ignite client
     */
    public static void loadCompleteDataset(IgniteClient client) {
        logger.info("Loading complete music store dataset using SQL script processing");
        logger.info("Processing complete music store catalog");
        
        try {
            logger.info("Loading data only (schema already created from POJOs)");
            int executedStatements = SqlScriptLoader.loadDataOnlyFromScript(client, "music-store-complete.sql");
            logger.info("Complete dataset loaded successfully");
            logger.info("Executed {} data statements successfully", executedStatements);
            
            logger.info("Verifying complete dataset integrity");
            SqlScriptLoader.verifyDataLoad(client);
            logger.info("All data verified - complete music store catalog is ready");
            
        } catch (Exception e) {
            logger.error("Complete dataset loading failed: {}", e.getMessage());
            throw new RuntimeException("Complete dataset loading failed", e);
        }
    }
    
    /**
     * Loads complete music store dataset from SQL script (legacy method).
     * 
     * @param client Connected Ignite client
     * @deprecated Use loadCompleteDataset for better performance
     */
    @Deprecated
    public static void loadExtendedData(IgniteClient client) {
        logger.info("Processing complete music store catalog (15,866-line SQL script)");
        logger.info("This includes 275+ artists, 347+ albums, 3,500+ tracks");
        logger.info("Plus complete customer database and transaction history");
        logger.info("Using optimized batch processing for performance");
        
        try {
            int executedStatements = SqlScriptLoader.loadFromScript(client, "music-store-complete.sql");
            logger.info("Bulk loading completed successfully");
            logger.info("Executed {} SQL statements with batch optimization", executedStatements);
            
            logger.info("Verifying complete dataset integrity");
            SqlScriptLoader.verifyDataLoad(client);
            logger.info("All data verified - complete music store catalog is ready");
            
        } catch (Exception e) {
            logger.error("Extended dataset loading failed: {}", e.getMessage());
            throw new RuntimeException("Complete dataset loading failed", e);
        }
    }
    
    private static void loadGenres(IgniteClient client, Transaction tx) {
        Table genreTable = client.tables().table("Genre");
        RecordView<Genre> genreView = genreTable.recordView(Genre.class);
        
        genreView.upsert(tx, new Genre(1, "Rock"));
        genreView.upsert(tx, new Genre(2, "Jazz"));
        genreView.upsert(tx, new Genre(3, "Metal"));
        genreView.upsert(tx, new Genre(4, "Alternative & Punk"));
        genreView.upsert(tx, new Genre(5, "Blues"));
    }
    
    private static void loadMediaTypes(IgniteClient client, Transaction tx) {
        Table mediaTypeTable = client.tables().table("MediaType");
        RecordView<MediaType> mediaTypeView = mediaTypeTable.recordView(MediaType.class);
        
        mediaTypeView.upsert(tx, new MediaType(1, "MPEG audio file"));
        mediaTypeView.upsert(tx, new MediaType(2, "Protected AAC audio file"));
        mediaTypeView.upsert(tx, new MediaType(3, "Protected MPEG-4 video file"));
    }
    
    private static void loadArtists(IgniteClient client, Transaction tx) {
        Table artistTable = client.tables().table("Artist");
        RecordView<Artist> artistView = artistTable.recordView(Artist.class);
        
        artistView.upsert(tx, new Artist(1, "AC/DC"));
        artistView.upsert(tx, new Artist(2, "Accept"));
        artistView.upsert(tx, new Artist(3, "Aerosmith"));
        artistView.upsert(tx, new Artist(4, "Black Sabbath"));
        artistView.upsert(tx, new Artist(5, "Led Zeppelin"));
    }
    
    private static void loadAlbums(IgniteClient client, Transaction tx) {
        Table albumTable = client.tables().table("Album");
        RecordView<Album> albumView = albumTable.recordView(Album.class);
        
        albumView.upsert(tx, new Album(1, 1, "For Those About To Rock We Salute You"));
        albumView.upsert(tx, new Album(2, 2, "Balls to the Wall"));
        albumView.upsert(tx, new Album(3, 2, "Restless and Wild"));
        albumView.upsert(tx, new Album(4, 3, "Big Ones"));
        albumView.upsert(tx, new Album(5, 4, "Paranoid"));
    }
    
    private static void loadTracks(IgniteClient client, Transaction tx) {
        Table trackTable = client.tables().table("Track");
        RecordView<Track> trackView = trackTable.recordView(Track.class);
        
        trackView.upsert(tx, new Track(1, 1, "For Those About To Rock (We Salute You)", 1, 1, "Angus Young, Malcolm Young, Brian Johnson", 343719, 11170334, new BigDecimal("0.99")));
        trackView.upsert(tx, new Track(2, 2, "Balls to the Wall", 1, 1, null, 342562, 5510424, new BigDecimal("0.99")));
        trackView.upsert(tx, new Track(3, 3, "Fast As a Shark", 1, 1, "F. Baltes, S. Kaufman, U. Dirkscneider & W. Hoffman", 230619, 3990994, new BigDecimal("0.99")));
        trackView.upsert(tx, new Track(4, 4, "Walk On Water", 1, 1, null, 295680, 4896186, new BigDecimal("0.99")));
        trackView.upsert(tx, new Track(5, 5, "Paranoid", 3, 1, "Anthony Iommi, William Ward, John Osbourne, Terence Butler", 162562, 2611465, new BigDecimal("0.99")));
    }
    
    private static void loadCustomers(IgniteClient client, Transaction tx) {
        Table customerTable = client.tables().table("Customer");
        RecordView<Customer> customerView = customerTable.recordView(Customer.class);
        
        customerView.upsert(tx, new Customer(1, "Luís", "Gonçalves", "Embraer - Empresa Brasileira de Aeronáutica S.A.", "Av. Brigadeiro Faria Lima, 2170", "São José dos Campos", "SP", "Brazil", "12227-000", "+55 (12) 3923-5555", "+55 (12) 3923-5566", "luisg@embraer.com.br", 3));
        customerView.upsert(tx, new Customer(2, "Leonie", "Köhler", null, "Theodor-Heuss-Straße 34", "Stuttgart", null, "Germany", "70174", "+49 0711 2842222", null, "leonekohler@surfeu.de", 5));
        customerView.upsert(tx, new Customer(3, "François", "Tremblay", null, "1498 rue Bélanger", "Montréal", "QC", "Canada", "H2G 1A7", "+1 (514) 721-4711", null, "ftremblay@gmail.com", 3));
    }
    
    private static void loadEmployees(IgniteClient client, Transaction tx) {
        Table employeeTable = client.tables().table("Employee");
        RecordView<Employee> employeeView = employeeTable.recordView(Employee.class);
        
        employeeView.upsert(tx, new Employee(1, "Adams", "Andrew", "General Manager", null, LocalDate.of(1962, 2, 18), LocalDate.of(2002, 8, 14), "11120 Jasper Ave NW", "Edmonton", "AB", "Canada", "T5K 2N1", "+1 (780) 428-9482", "+1 (780) 428-3457", "andrew@chinookcorp.com"));
        employeeView.upsert(tx, new Employee(2, "Edwards", "Nancy", "Sales Manager", 1, LocalDate.of(1958, 12, 8), LocalDate.of(2002, 5, 1), "825 8 Ave SW", "Calgary", "AB", "Canada", "T2P 2T3", "+1 (403) 262-3443", "+1 (403) 262-3322", "nancy@chinookcorp.com"));
        employeeView.upsert(tx, new Employee(3, "Peacock", "Jane", "Sales Support Agent", 2, LocalDate.of(1973, 8, 29), LocalDate.of(2002, 4, 1), "1111 6 Ave SW", "Calgary", "AB", "Canada", "T2P 5M5", "+1 (403) 262-3443", "+1 (403) 262-6712", "jane@chinookcorp.com"));
    }
    
    private static void loadInvoices(IgniteClient client, Transaction tx) {
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
    
}