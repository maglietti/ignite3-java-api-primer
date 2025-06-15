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
        logger.info("Creating music store schema...");
        
        try {
            createDistributionZones(client);
            
            createTableIfNotExists(client, Genre.class, "Genre");
            createTableIfNotExists(client, MediaType.class, "MediaType");
            
            createTableIfNotExists(client, Artist.class, "Artist");
            createTableIfNotExists(client, Album.class, "Album");
            createTableIfNotExists(client, Track.class, "Track");
            
            createTableIfNotExists(client, Customer.class, "Customer");
            createTableIfNotExists(client, Employee.class, "Employee");
            createTableIfNotExists(client, Invoice.class, "Invoice");
            createTableIfNotExists(client, InvoiceLine.class, "InvoiceLine");
            
            createTableIfNotExists(client, Playlist.class, "Playlist");
            createTableIfNotExists(client, PlaylistTrack.class, "PlaylistTrack");
            
            logger.info("Music store schema created successfully");
            
        } catch (Exception e) {
            logger.error("Failed to create schema: {}", e.getMessage());
            throw new RuntimeException("Schema creation failed", e);
        }
    }
    
    /**
     * Drops all music store tables.
     * 
     * @param client Connected Ignite client
     */
    public static void dropSchema(IgniteClient client) {
        logger.info("Dropping music store schema...");
        
        String[] tables = {"PlaylistTrack", "InvoiceLine", "Track", "Invoice", 
                          "Playlist", "Album", "Employee", "Customer", "Artist", 
                          "MediaType", "Genre"};
        
        for (String tableName : tables) {
            try {
                client.sql().execute(null, "DROP TABLE IF EXISTS " + tableName);
                logger.debug("Dropped table: {}", tableName);
            } catch (Exception e) {
                logger.warn("Failed to drop table {}: {}", tableName, e.getMessage());
            }
        }
        
        dropDistributionZones(client);
        
        logger.info("Music store schema dropped");
    }
    
    /**
     * Checks if schema exists and prompts user for action if it does.
     * 
     * @param client Connected Ignite client
     * @return true if should proceed with schema creation, false if should exit
     */
    public static boolean checkSchemaAndPromptUser(IgniteClient client) {
        if (schemaExists(client)) {
            logger.warn("Music store schema already exists in the cluster");
            System.out.println("\nExisting music store schema detected!");
            System.out.println("Options:");
            System.out.println("  1. Continue (may cause errors if schema conflicts)");
            System.out.println("  2. Remove existing schema and recreate");
            System.out.println("  3. Exit");
            System.out.print("Please choose an option (1/2/3): ");
            
            try (Scanner scanner = new Scanner(System.in)) {
                String choice = scanner.nextLine().trim();
                
                switch (choice) {
                    case "1":
                        logger.info("Continuing with existing schema");
                        return true;
                    case "2":
                        logger.info("Removing existing schema and recreating");
                        dropSchema(client);
                        return true;
                    case "3":
                        logger.info("Exiting without changes");
                        return false;
                    default:
                        logger.warn("Invalid choice '{}', exiting", choice);
                        return false;
                }
            }
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
            MusicStoreZoneConfiguration.createDistributionZones(client);
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
    
    private static void createTableIfNotExists(IgniteClient client, Class<?> entityClass, String tableName) {
        try {
            if (!ConnectionUtils.tableExists(client, tableName)) {
                client.catalog().createTable(entityClass);
                logger.info("Created table: {}", tableName);
            } else {
                logger.debug("Table already exists: {}", tableName);
            }
        } catch (Exception e) {
            logger.error("Failed to create table {}: {}", tableName, e.getMessage());
            throw new RuntimeException("Failed to create table " + tableName, e);
        }
    }
}