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

import java.util.Scanner;

import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apache.ignite.examples.setup.config.MusicStoreZoneConfiguration;
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
 * Schema management utilities for Apache Ignite 3 music store dataset.
 * 
 * Demonstrates schema-as-code patterns using annotated POJOs to create
 * distributed database schemas with proper dependency ordering and
 * distribution zone configuration.
 */
public class SchemaUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(SchemaUtils.class);
    
    /**
     * Creates the complete music store schema with all tables and zones.
     * 
     * @param client Connected Ignite client
     */
    public static void createSchema(IgniteClient client) {
        try {
            logger.info("--- Distribution Zones");
            createDistributionZones(client);
            
            logger.info("--- Reference Tables");
            createTableIfNotExists(client, Genre.class, "Genre", "Music genre classifications", 1, 11);
            createTableIfNotExists(client, MediaType.class, "MediaType", "Audio/video format types", 2, 11);
            
            logger.info("--- Core Music Entities");
            createTableIfNotExists(client, Artist.class, "Artist", "Music artists and bands (root entity)", 3, 11);
            createTableIfNotExists(client, Album.class, "Album", "Albums (colocated by ArtistId)", 4, 11);
            createTableIfNotExists(client, Track.class, "Track", "Individual songs (colocated by AlbumId)", 5, 11);
            
            logger.info("--- Business Entities");
            createTableIfNotExists(client, Customer.class, "Customer", "Store customers (root entity)", 6, 11);
            createTableIfNotExists(client, Employee.class, "Employee", "Staff with hierarchy", 7, 11);
            createTableIfNotExists(client, Invoice.class, "Invoice", "Purchase transactions (colocated by CustomerId)", 8, 11);
            createTableIfNotExists(client, InvoiceLine.class, "InvoiceLine", "Purchase line items (colocated by InvoiceId)", 9, 11);
            
            logger.info("--- Playlist Entities");
            createTableIfNotExists(client, Playlist.class, "Playlist", "User-created playlists", 10, 11);
            createTableIfNotExists(client, PlaylistTrack.class, "PlaylistTrack", "Playlist-track associations (colocated by PlaylistId)", 11, 11);
            
        } catch (Exception e) {
            logger.error("Schema creation failed: {}", e.getMessage());
            throw new RuntimeException("Schema creation failed", e);
        }
    }
    
    /**
     * Drops all music store tables.
     * 
     * @param client Connected Ignite client
     */
    public static void dropSchema(IgniteClient client) {
        logger.info(">>> Removing existing music store schema");
        logger.info("--- Dropping tables in dependency order (child tables first)");
        
        String[] tables = {"PlaylistTrack", "InvoiceLine", "Track", "Invoice", 
                          "Playlist", "Album", "Employee", "Customer", "Artist", 
                          "MediaType", "Genre"};
        
        int count = 0;
        for (String tableName : tables) {
            try {
                client.sql().execute(null, "DROP TABLE IF EXISTS " + tableName);
                count++;
                logger.info("<<< Dropped table: {} ({}/{})", tableName, count, tables.length);
            } catch (Exception e) {
                logger.warn("* Failed to drop table {}: {}", tableName, e.getMessage());
            }
        }
        
        logger.info("--- Dropping distribution zones");
        dropDistributionZones(client);
        
        logger.info("<<< Schema cleanup completed ({} tables processed)", tables.length);
    }
    
    /**
     * Checks if schema exists and prompts user for action if it does.
     * 
     * @param client Connected Ignite client
     * @return true if should proceed with schema creation, false if should exit
     */
    public static boolean checkSchemaAndPromptUser(IgniteClient client) {
        if (schemaExists(client)) {
            logger.info(">>> Checking for existing music store tables");
            logger.info("    Found existing music store tables in the cluster");
            logger.info("    This may indicate a previous setup or conflicting data");
            
            System.out.println("\n=================================================================");
            System.out.println("  EXISTING SCHEMA DETECTED                                       ");
            System.out.println("                                                               ");
            System.out.println("  Found music store tables already in your Ignite cluster.    ");
            System.out.println("  How would you like to proceed?                              ");
            System.out.println("                                                               ");
            System.out.println("  Options:                                                     ");
            System.out.println("    1. Continue (skip table creation, load data only)         ");
            System.out.println("    2. Clean slate (remove existing tables and recreate)      ");
            System.out.println("    3. Exit (cancel setup)                                    ");
            System.out.println("                                                               ");
            System.out.println("  Tip: Option 2 is recommended for clean development         ");
            System.out.println("=================================================================");
            System.out.print("Please choose an option (1/2/3): ");
            
            try (Scanner scanner = new Scanner(System.in)) {
                if (!scanner.hasNextLine()) {
                    logger.warn("    No interactive input available, continuing with existing schema");
                    logger.info("    This may cause conflicts if schema structure differs");
                    return true;
                }
                
                String choice = scanner.nextLine().trim();
                
                switch (choice) {
                    case "1":
                        logger.info("    Continuing with existing schema");
                        logger.info("    Will attempt to load data into existing tables");
                        return true;
                    case "2":
                        logger.info("    Starting clean slate setup");
                        logger.info("    Removing existing schema for complete recreation");
                        dropSchema(client);
                        return true;
                    case "3":
                        logger.info("    Setup cancelled by user choice");
                        return false;
                    default:
                        logger.warn("    Invalid choice '{}', cancelling setup for safety", choice);
                        logger.info("    Run again and choose 1, 2, or 3");
                        return false;
                }
            } catch (Exception e) {
                logger.warn("    Error reading user input: {}", e.getMessage());
                logger.info("    Defaulting to continue with existing schema");
                return true;
            }
        } else {
            logger.info(">>> Checking for existing music store tables");
        }
        return true;
    }
    
    /**
     * Checks if music store schema exists by looking for key tables.
     * 
     * @param client Connected Ignite client
     * @return true if schema exists
     */
    private static boolean schemaExists(IgniteClient client) {
        return ConnectionUtils.tableExists(client, "Artist") || 
               ConnectionUtils.tableExists(client, "Album") ||
               ConnectionUtils.tableExists(client, "Track");
    }
    
    /**
     * Creates distribution zones with defensive error handling.
     * 
     * @param client Connected Ignite client
     */
    private static void createDistributionZones(IgniteClient client) {
        try {
            logger.info(">>> Creating distribution zone: MusicStore");
            MusicStoreZoneConfiguration.createMusicStoreZone(client);
            logger.info("<<< Created distribution zone: MusicStore");
            
            logger.info(">>> Creating distribution zone: MusicStoreReplicated");
            MusicStoreZoneConfiguration.createMusicStoreReplicatedZone(client);
            logger.info("<<< Created distribution zone: MusicStoreReplicated");
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                logger.debug("Distribution zones already exist, continuing");
            } else {
                throw e;
            }
        }
    }
    
    /**
     * Drops distribution zones.
     * 
     * @param client Connected Ignite client
     */
    private static void dropDistributionZones(IgniteClient client) {
        try {
            client.sql().execute(null, "DROP ZONE IF EXISTS MusicStore");
            client.sql().execute(null, "DROP ZONE IF EXISTS MusicStoreReplicated");
            logger.debug("Dropped distribution zones");
        } catch (Exception e) {
            logger.warn("Failed to drop distribution zones: {}", e.getMessage());
        }
    }
    
    private static void createTableIfNotExists(IgniteClient client, Class<?> entityClass, String tableName, String description, int current, int total) {
        try {
            if (!ConnectionUtils.tableExists(client, tableName)) {
                logger.info(">>> Creating table: {} - {} ({}/{})", tableName, description, current, total);
                client.catalog().createTable(entityClass);
                logger.info("<<< Created table: {}", tableName);
            } else {
                logger.info("    Table already exists: {} ({}/{})", tableName, current, total);
            }
        } catch (Exception e) {
            logger.error("    Failed to create table {}: {}", tableName, e.getMessage());
            throw new RuntimeException("Failed to create table " + tableName, e);
        }
    }
}