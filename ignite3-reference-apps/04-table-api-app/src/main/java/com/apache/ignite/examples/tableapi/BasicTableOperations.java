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

package com.apache.ignite.examples.tableapi;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic Table Operations - CRUD operations with Apache Ignite 3.
 * 
 * This class demonstrates the fundamentals of working with Ignite tables:
 * - Connecting to a cluster
 * - Accessing tables and views
 * - Create, Read, Update, Delete operations
 * - Error handling
 * 
 * Learning Focus:
 * - Table API basics
 * - Tuple-based operations
 * - Connection patterns
 * - Error handling
 */
public class BasicTableOperations {

    private static final Logger logger = LoggerFactory.getLogger(BasicTableOperations.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Basic Table Operations Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Demonstrating fundamental CRUD operations with Tuple API");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            runBasicOperations(client);
            
        } catch (Exception e) {
            logger.error("Failed to run basic table operations", e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void runBasicOperations(IgniteClient client) {
        // Get the Artist table
        Table artistTable = client.tables().table("Artist");
        if (artistTable == null) {
            throw new RuntimeException("Artist table not found. Please run sample data setup first.");
        }
        
        // Get a record view for tuple-based operations
        RecordView<Tuple> artists = artistTable.recordView();
        
        System.out.println("\n--- Basic CRUD Operations ---");
        System.out.println("    Using Tuple API for type-flexible operations");
        
        // CREATE: Insert a new artist
        createArtist(artists);
        
        // READ: Retrieve the artist
        readArtist(artists);
        
        // UPDATE: Modify the artist
        updateArtist(artists);
        
        // READ: Verify the update
        readArtist(artists);
        
        // DELETE: Remove the artist
        deleteArtist(artists);
        
        // READ: Verify deletion
        verifyDeletion(artists);
        
        System.out.println("\n>>> Basic table operations completed successfully");
    }

    private static void createArtist(RecordView<Tuple> artists) {
        System.out.println("\n--- CREATE Operation");
        System.out.println(">>> Creating new artist record using Tuple API");
        
        // Create a new artist record using Tuple
        Tuple newArtist = Tuple.create()
            .set("ArtistId", 5001)
            .set("Name", "Demo Artist");
        
        // Insert the record
        artists.upsert(null, newArtist);
        
        System.out.println("<<< Created artist: " + newArtist.stringValue("Name"));
    }

    private static void readArtist(RecordView<Tuple> artists) {
        System.out.println("\n--- READ Operation");
        System.out.println(">>> Retrieving artist by primary key");
        
        // Create a key tuple to find the artist
        Tuple key = Tuple.create().set("ArtistId", 5001);
        
        // Retrieve the artist
        Tuple artist = artists.get(null, key);
        
        if (artist != null) {
            System.out.println("<<< Found artist: " + artist.stringValue("Name") + 
                             " (ID: " + artist.intValue("ArtistId") + ")");
        } else {
            System.out.println("!!! Artist not found");
        }
    }

    private static void updateArtist(RecordView<Tuple> artists) {
        System.out.println("\n--- UPDATE Operation");
        System.out.println(">>> Modifying existing artist record");
        
        // First get the current record
        Tuple key = Tuple.create().set("ArtistId", 5001);
        Tuple artist = artists.get(null, key);
        
        if (artist != null) {
            // Update the name
            Tuple updatedArtist = artist.set("Name", "Updated Demo Artist");
            
            // Save the changes
            artists.upsert(null, updatedArtist);
            
            System.out.println("<<< Updated artist name to: " + updatedArtist.stringValue("Name"));
        } else {
            System.out.println("!!! Artist not found for update");
        }
    }

    private static void deleteArtist(RecordView<Tuple> artists) {
        System.out.println("\n--- DELETE Operation");
        System.out.println(">>> Removing artist record");
        
        // Create key for the artist to delete
        Tuple key = Tuple.create().set("ArtistId", 5001);
        
        // Delete the record
        boolean deleted = artists.delete(null, key);
        
        if (deleted) {
            System.out.println("<<< Artist deleted successfully");
        } else {
            System.out.println("!!! Artist not found for deletion");
        }
    }

    private static void verifyDeletion(RecordView<Tuple> artists) {
        System.out.println("\n--- VERIFY Deletion");
        System.out.println(">>> Confirming deletion was successful");
        
        // Try to find the deleted artist
        Tuple key = Tuple.create().set("ArtistId", 5001);
        Tuple artist = artists.get(null, key);
        
        if (artist == null) {
            System.out.println("<<< Confirmed: Artist successfully deleted");
        } else {
            System.out.println("!!! Unexpected: Artist still exists");
        }
    }
}