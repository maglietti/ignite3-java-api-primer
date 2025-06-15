package com.apache.ignite.examples.setup.util;

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
            MusicStoreZoneConfiguration.createDistributionZones(client);
            
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
        
        logger.info("Music store schema dropped");
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