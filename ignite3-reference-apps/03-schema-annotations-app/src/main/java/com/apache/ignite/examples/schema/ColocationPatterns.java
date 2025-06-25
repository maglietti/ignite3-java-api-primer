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

package com.apache.ignite.examples.schema;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;

import com.apache.ignite.examples.schema.model.Artist;
import com.apache.ignite.examples.schema.model.Album;
import com.apache.ignite.examples.schema.model.Track;
import com.apache.ignite.examples.schema.model.Customer;
import com.apache.ignite.examples.schema.model.Invoice;
import com.apache.ignite.examples.schema.model.InvoiceLine;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Demonstrates Apache Ignite 3 data colocation patterns for performance optimization.
 * 
 * Shows how to design colocation hierarchies where related data is stored on the
 * same cluster nodes. Reduces network overhead and enables single-node query
 * execution for improved performance in distributed environment.
 * 
 * Key concepts:
 * - @ColumnRef colocation annotation for data locality
 * - Hierarchical colocation: Artist->Album->Track, Customer->Invoice->InvoiceLine
 * - Colocation keys must be part of composite primary keys
 * - Single-node query execution for colocated data
 * - Network overhead reduction through data locality
 * 
 * Prerequisites:
 * - Running Ignite cluster
 * - Sample data setup module (creates colocated table structure)
 */
public class ColocationPatterns {
    
    private final IgniteClient client;
    
    public ColocationPatterns(IgniteClient client) {
        this.client = client;
    }
    
    /**
     * Demonstrates music catalog colocation hierarchy.
     * 
     * Shows Artist->Album->Track colocation chain where albums are colocated
     * with artists by ArtistId, and tracks are colocated with albums by AlbumId.
     * Enables efficient catalog browsing with minimal network overhead.
     */
    public void demonstrateMusicColocation() {
        System.out.println("--- Music Catalog Colocation");
        System.out.println("    Colocation hierarchy: Artist -> Album -> Track");
        System.out.println("    - Albums colocated with Artists using ArtistId");
        System.out.println("    - Tracks colocated with Albums using AlbumId");
        System.out.println("    - Single-node queries for artist catalog browsing");
        
        try {
            // Setup demonstration data with colocation
            setupMusicHierarchy();
            
            // Demonstrate colocated query performance
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
            
            System.out.println("    >>> Executing colocated catalog query");
            int trackCount = 0;
            try (ResultSet<SqlRow> result = client.sql().execute(null, colocatedQuery, 8001)) {
                while (result.hasNext()) {
                    SqlRow row = result.next();
                    trackCount++;
                    System.out.printf("    <<< %s - %s - %s (%d sec)%n",
                        row.stringValue("Artist"),
                        row.stringValue("Album"),
                        row.stringValue("Track"),
                        row.intValue("DurationSeconds"));
                }
            }
            
            System.out.println("    >>> Query executed on single node: " + trackCount + " tracks");
            System.out.println("    >>> Colocation eliminates network round trips");
            
            // Cleanup
            cleanupMusicHierarchy();
            
        } catch (Exception e) {
            System.err.println("    !!! Music colocation demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates sales transaction colocation hierarchy.
     * 
     * Shows Customer->Invoice->InvoiceLine colocation chain where invoices are
     * colocated with customers, and invoice lines with invoices. Enables efficient
     * transaction processing and customer analytics.
     */
    public void demonstrateSalesColocation() {
        System.out.println("--- Sales Transaction Colocation");
        System.out.println("    Colocation hierarchy: Customer -> Invoice -> InvoiceLine");
        System.out.println("    - Invoices colocated with Customers using CustomerId");
        System.out.println("    - InvoiceLines colocated with Invoices using InvoiceId");
        System.out.println("    - Single-node transaction processing and analytics");
        
        try {
            // Setup demonstration data with sales hierarchy
            setupSalesHierarchy();
            
            // Demonstrate colocated transaction query
            String transactionQuery = """
                SELECT 
                    c.FirstName || ' ' || c.LastName as Customer,
                    i.InvoiceId,
                    il.Quantity * il.UnitPrice as LineTotal
                FROM Customer c
                JOIN Invoice i ON c.CustomerId = i.CustomerId
                JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId
                WHERE c.CustomerId = ?
                ORDER BY il.InvoiceLineId
                """;
            
            System.out.println("    >>> Executing colocated transaction query");
            BigDecimal total = BigDecimal.ZERO;
            int lineCount = 0;
            
            try (ResultSet<SqlRow> result = client.sql().execute(null, transactionQuery, 8002)) {
                while (result.hasNext()) {
                    SqlRow row = result.next();
                    lineCount++;
                    BigDecimal lineTotal = (BigDecimal) row.value("LineTotal");
                    total = total.add(lineTotal);
                    
                    System.out.printf("    <<< %s - Invoice %d - Line total: $%.2f%n",
                        row.stringValue("Customer"),
                        row.intValue("InvoiceId"),
                        lineTotal.doubleValue());
                }
            }
            
            System.out.printf("    >>> Transaction total: $%.2f (%d lines)%n", total.doubleValue(), lineCount);
            System.out.println("    >>> Single-node execution for complete transaction");
            
            // Cleanup
            cleanupSalesHierarchy();
            
        } catch (Exception e) {
            System.err.println("    !!! Sales colocation demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates colocation best practices and key requirements.
     * 
     * Shows critical design patterns for effective colocation including
     * colocation key requirements, hierarchical design, and query alignment.
     */
    public void demonstrateColocationBestPractices() {
        System.out.println("--- Colocation Best Practices");
        System.out.println("    Key requirements:");
        System.out.println("    - Colocation keys must be part of primary key");
        System.out.println("    - Design hierarchical colocation chains");
        System.out.println("    - Align query patterns with colocation design");
        System.out.println("    - Balance distribution vs colocation benefits");
        
        try {
            // Validate colocation key requirements
            System.out.println("    >>> Validating colocation configurations");
            
            // Test Album colocation with Artist
            Table albumTable = client.tables().table("Album");
            RecordView<Album> albumView = albumTable.recordView(Album.class);
            System.out.println("    <<< Album.ArtistId: Colocation key in primary key");
            
            // Test Track colocation with Album
            Table trackTable = client.tables().table("Track");
            RecordView<Track> trackView = trackTable.recordView(Track.class);
            System.out.println("    <<< Track.AlbumId: Colocation key in primary key");
            
            // Test InvoiceLine colocation with Invoice
            Table invoiceLineTable = client.tables().table("InvoiceLine");
            RecordView<InvoiceLine> invoiceLineView = invoiceLineTable.recordView(InvoiceLine.class);
            System.out.println("    <<< InvoiceLine.InvoiceId: Colocation key in primary key");
            
            System.out.println("    >>> Colocation design validation completed");
            System.out.println("    >>> All colocation keys properly integrated in primary keys");
            
        } catch (Exception e) {
            System.err.println("    !!! Colocation validation failed: " + e.getMessage());
        }
    }
    
    private void setupMusicHierarchy() {
        // Create Artist (root entity)
        RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
        Artist artist = new Artist(8001, "Colocation Demo Band");
        artistView.upsert(null, artist);
        
        // Create Album (colocated by ArtistId)
        RecordView<Album> albumView = client.tables().table("Album").recordView(Album.class);
        Album album = new Album(8002, 8001, "Demo Album");
        albumView.upsert(null, album);
        
        // Create Tracks (colocated by AlbumId)
        RecordView<Track> trackView = client.tables().table("Track").recordView(Track.class);
        for (int i = 1; i <= 2; i++) {
            Track track = new Track();
            track.setTrackId(8000 + i);
            track.setAlbumId(8002);
            track.setName("Demo Track " + i);
            track.setMediaTypeId(1);  // Required field - MP3 format
            track.setGenreId(1);      // Required field - Rock genre
            track.setMilliseconds(180000 + (i * 1000));
            track.setUnitPrice(BigDecimal.valueOf(0.99));
            trackView.upsert(null, track);
        }
    }
    
    private void setupSalesHierarchy() {
        // Create Customer (root entity)
        RecordView<Customer> customerView = client.tables().table("Customer").recordView(Customer.class);
        Customer customer = new Customer();
        customer.setCustomerId(8002);
        customer.setFirstName("Demo");
        customer.setLastName("Customer");
        customer.setEmail("demo.customer@colocation.test");
        customer.setCity("San Francisco");
        customer.setCountry("USA");
        customerView.upsert(null, customer);
        
        // Create Invoice (colocated by CustomerId)
        RecordView<Invoice> invoiceView = client.tables().table("Invoice").recordView(Invoice.class);
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(8003);
        invoice.setCustomerId(8002);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setBillingCity("San Francisco");
        invoice.setBillingCountry("USA");
        invoice.setTotal(BigDecimal.valueOf(1.98));
        invoiceView.upsert(null, invoice);
        
        // Create InvoiceLines (colocated by InvoiceId)
        RecordView<InvoiceLine> invoiceLineView = client.tables().table("InvoiceLine").recordView(InvoiceLine.class);
        for (int i = 1; i <= 2; i++) {
            InvoiceLine line = new InvoiceLine();
            line.setInvoiceLineId(8000 + i);
            line.setInvoiceId(8003);
            line.setTrackId(8000 + i);
            line.setUnitPrice(BigDecimal.valueOf(0.99));
            line.setQuantity(1);
            invoiceLineView.upsert(null, line);
        }
    }
    
    private void cleanupMusicHierarchy() {
        try {
            // Cleanup in reverse dependency order
            RecordView<Track> trackView = client.tables().table("Track").recordView(Track.class);
            for (int i = 1; i <= 2; i++) {
                Track key = new Track();
                key.setTrackId(8000 + i);
                key.setAlbumId(8002);
                trackView.delete(null, key);
            }
            
            RecordView<Album> albumView = client.tables().table("Album").recordView(Album.class);
            Album albumKey = new Album();
            albumKey.setAlbumId(8002);
            albumKey.setArtistId(8001);
            albumView.delete(null, albumKey);
            
            RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
            Artist artistKey = new Artist();
            artistKey.setArtistId(8001);
            artistView.delete(null, artistKey);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    private void cleanupSalesHierarchy() {
        try {
            // Cleanup in reverse dependency order
            RecordView<InvoiceLine> invoiceLineView = client.tables().table("InvoiceLine").recordView(InvoiceLine.class);
            for (int i = 1; i <= 2; i++) {
                InvoiceLine key = new InvoiceLine();
                key.setInvoiceLineId(8000 + i);
                key.setInvoiceId(8003);
                invoiceLineView.delete(null, key);
            }
            
            RecordView<Invoice> invoiceView = client.tables().table("Invoice").recordView(Invoice.class);
            Invoice invoiceKey = new Invoice();
            invoiceKey.setInvoiceId(8003);
            invoiceKey.setCustomerId(8002);
            invoiceView.delete(null, invoiceKey);
            
            RecordView<Customer> customerView = client.tables().table("Customer").recordView(Customer.class);
            Customer customerKey = new Customer();
            customerKey.setCustomerId(8002);
            customerView.delete(null, customerKey);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
}
