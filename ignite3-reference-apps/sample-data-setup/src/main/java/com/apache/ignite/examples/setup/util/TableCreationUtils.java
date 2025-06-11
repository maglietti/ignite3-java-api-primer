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
 * Table creation utilities demonstrating Ignite 3 schema-as-code patterns.
 * 
 * This class showcases how to create distributed database schemas using:
 * - Annotation-driven table creation from POJOs
 * - Distribution zone management and configuration
 * - Proper dependency ordering for related tables
 * - Schema verification and integrity checking
 * - Cleanup and maintenance operations
 * 
 * Key Concepts Demonstrated:
 * - client.catalog().createTable(Class) - Schema-as-code table creation
 * - Distribution zones for data placement control
 * - Dependency management between related entities
 * - Error handling and rollback strategies
 * 
 * The music store schema represents a realistic distributed system design with:
 * - Reference data (Genre, MediaType) in highly replicated zones
 * - Hierarchical business data (Artist->Album->Track) with colocation
 * - Business workflows (Customer->Invoice->InvoiceLine) with transactional integrity
 */
public class TableCreationUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(TableCreationUtils.class);
    
    /**
     * Creates the complete music store schema including all 11 tables and 2 distribution zones.
     * 
     * This method demonstrates proper schema creation order:
     * 1. Distribution zones first (data placement foundation)
     * 2. Reference data tables (no dependencies)
     * 3. Root entities (Artist, Customer, Employee, Playlist)
     * 4. Dependent entities (Album, Invoice) 
     * 5. Leaf entities (Track, InvoiceLine, PlaylistTrack)
     * 
     * @param client Connected Ignite client for schema operations
     * @throws RuntimeException if schema creation fails
     */
    public static void createAllTables(IgniteClient client) {
        logger.info("Starting creation of all music store tables...");
        
        try {
            // Step 1: Create distribution zones first - these define data placement strategies
            MusicStoreZoneConfiguration.createDistributionZones(client);
            
            // Step 2: Create reference data tables - no dependencies on other entities
            createReferenceDataTables(client);
            
            // Step 3: Create root music entities - foundation of the music hierarchy
            createMusicEntityTables(client);
            
            // Step 4: Create business entities - customer and transaction management  
            createBusinessEntityTables(client);
            
            // Step 5: Create playlist entities - user-generated content management
            createPlaylistTables(client);
            
            logger.info("All music store tables created successfully");
            
        } catch (Exception e) {
            logger.error("Failed to create all tables: {}", e.getMessage());
            throw new RuntimeException("Failed to create music store schema", e);
        }
    }
    
    public static void createReferenceDataTables(IgniteClient client) {
        logger.info("Creating reference data tables...");
        
        createTableIfNotExists(client, Genre.class, "Genre");
        createTableIfNotExists(client, MediaType.class, "MediaType");
        
        logger.info("Reference data tables created");
    }
    
    public static void createMusicEntityTables(IgniteClient client) {
        logger.info("Creating music entity tables...");
        
        createTableIfNotExists(client, Artist.class, "Artist");
        createTableIfNotExists(client, Album.class, "Album");
        createTableIfNotExists(client, Track.class, "Track");
        
        logger.info("Music entity tables created");
    }
    
    public static void createBusinessEntityTables(IgniteClient client) {
        logger.info("Creating business entity tables...");
        
        createTableIfNotExists(client, Customer.class, "Customer");
        createTableIfNotExists(client, Employee.class, "Employee");
        createTableIfNotExists(client, Invoice.class, "Invoice");
        createTableIfNotExists(client, InvoiceLine.class, "InvoiceLine");
        
        logger.info("Business entity tables created");
    }
    
    public static void createPlaylistTables(IgniteClient client) {
        logger.info("Creating playlist tables...");
        
        createTableIfNotExists(client, Playlist.class, "Playlist");
        createTableIfNotExists(client, PlaylistTrack.class, "PlaylistTrack");
        
        logger.info("Playlist tables created");
    }
    
    private static void createTableIfNotExists(IgniteClient client, Class<?> entityClass, String tableName) {
        try {
            if (!DataSetupUtils.tableExists(client, tableName)) {
                client.catalog().createTable(entityClass);
                logger.info("Created table: {}", tableName);
            } else {
                logger.info("Table already exists: {}", tableName);
            }
        } catch (Exception e) {
            logger.error("Failed to create table {}: {}", tableName, e.getMessage());
            throw new RuntimeException("Failed to create table: " + tableName, e);
        }
    }
    
    public static void dropAllTables(IgniteClient client) {
        logger.info("Dropping all music store tables...");
        
        try {
            dropPlaylistTables(client);
            dropBusinessEntityTables(client);
            dropMusicEntityTables(client);
            dropReferenceDataTables(client);
            
            MusicStoreZoneConfiguration.dropDistributionZones(client);
            
            logger.info("All music store tables dropped successfully");
            
        } catch (Exception e) {
            logger.error("Failed to drop all tables: {}", e.getMessage());
        }
    }
    
    private static void dropPlaylistTables(IgniteClient client) {
        dropTableIfExists(client, "PlaylistTrack");
        dropTableIfExists(client, "Playlist");
    }
    
    private static void dropBusinessEntityTables(IgniteClient client) {
        dropTableIfExists(client, "InvoiceLine");
        dropTableIfExists(client, "Invoice");
        dropTableIfExists(client, "Customer");
        dropTableIfExists(client, "Employee");
    }
    
    private static void dropMusicEntityTables(IgniteClient client) {
        dropTableIfExists(client, "Track");
        dropTableIfExists(client, "Album");
        dropTableIfExists(client, "Artist");
    }
    
    private static void dropReferenceDataTables(IgniteClient client) {
        dropTableIfExists(client, "MediaType");
        dropTableIfExists(client, "Genre");
    }
    
    private static void dropTableIfExists(IgniteClient client, String tableName) {
        try {
            if (DataSetupUtils.tableExists(client, tableName)) {
                client.sql().execute(null, "DROP TABLE " + tableName);
                logger.info("Dropped table: {}", tableName);
            } else {
                logger.debug("Table does not exist: {}", tableName);
            }
        } catch (Exception e) {
            logger.warn("Failed to drop table {}: {}", tableName, e.getMessage());
        }
    }
    
    public static void displayAllTablesInfo(IgniteClient client) {
        logger.info("Music Store Schema Information:");
        
        MusicStoreZoneConfiguration.displayZoneInfo(client);
        DataSetupUtils.displayTables(client);
        
        String[] tables = {"Artist", "Album", "Track", "Genre", "MediaType", 
                          "Customer", "Employee", "Invoice", "InvoiceLine", 
                          "Playlist", "PlaylistTrack"};
        
        for (String tableName : tables) {
            if (DataSetupUtils.tableExists(client, tableName)) {
                DataSetupUtils.displayTableInfo(client, tableName);
                long rowCount = DataSetupUtils.getTableRowCount(client, tableName);
                if (rowCount >= 0) {
                    logger.info("  Row count: {}", rowCount);
                }
                logger.info("");
            }
        }
    }
    
    public static void verifySchemaIntegrity(IgniteClient client) {
        logger.info("Verifying music store schema integrity...");
        
        String[] expectedTables = {"Artist", "Album", "Track", "Genre", "MediaType", 
                                  "Customer", "Employee", "Invoice", "InvoiceLine", 
                                  "Playlist", "PlaylistTrack"};
        
        boolean allTablesExist = true;
        for (String tableName : expectedTables) {
            if (!DataSetupUtils.tableExists(client, tableName)) {
                logger.error("Missing table: {}", tableName);
                allTablesExist = false;
            } else {
                logger.debug("Table exists: {}", tableName);
            }
        }
        
        if (allTablesExist) {
            logger.info("Schema integrity verification passed - all tables exist");
        } else {
            logger.error("Schema integrity verification failed - missing tables");
            throw new RuntimeException("Schema integrity verification failed");
        }
    }
}