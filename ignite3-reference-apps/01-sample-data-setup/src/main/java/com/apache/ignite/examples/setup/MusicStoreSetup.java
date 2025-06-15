package com.apache.ignite.examples.setup;

import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apache.ignite.examples.setup.util.ConnectionUtils;
import com.apache.ignite.examples.setup.util.SchemaUtils;
import com.apache.ignite.examples.setup.util.DataLoader;

/**
 * Apache Ignite 3 Music Store Sample Data Setup Application.
 * 
 * This application creates a complete music store dataset for learning
 * Apache Ignite 3 distributed database concepts and API patterns.
 * 
 * Features:
 * - Schema-as-code using annotated POJOs
 * - Distribution zones for optimal data placement
 * - Transactional data loading with ACID guarantees
 * - Realistic sample data for development and testing
 * 
 * Usage:
 *   mvn exec:java                                    # Default setup
 *   mvn exec:java -Dexec.args="--extended"          # Include extended data
 *   mvn exec:java -Dexec.args="--reset"             # Drop existing schema and recreate
 *   mvn exec:java -Dexec.args="--reset --extended"  # Reset with extended data
 *   mvn exec:java -Dexec.args="192.168.1.100:10800" # Custom cluster address
 *   mvn exec:java -Dexec.args="192.168.1.100:10800 --extended" # Both options
 */
public class MusicStoreSetup {
    
    private static final Logger logger = LoggerFactory.getLogger(MusicStoreSetup.class);
    
    public static void main(String[] args) {
        String clusterAddress = "127.0.0.1:10800";
        boolean loadExtended = false;
        boolean resetSchema = false;
        
        for (String arg : args) {
            if (arg.equals("--extended")) {
                loadExtended = true;
            } else if (arg.equals("--reset")) {
                resetSchema = true;
            } else if (!arg.startsWith("--")) {
                clusterAddress = arg;
            }
        }
        
        logger.info("Starting Apache Ignite 3 Music Store Setup");
        logger.info("Cluster address: {}", clusterAddress);
        logger.info("Extended data: {}", loadExtended ? "enabled" : "disabled");
        logger.info("Reset schema: {}", resetSchema ? "enabled" : "disabled");
        
        try (IgniteClient client = ConnectionUtils.connectToCluster(clusterAddress)) {
            
            if (resetSchema) {
                logger.info("=== Resetting Schema ===");
                SchemaUtils.dropSchema(client);
            }
            
            logger.info("=== Creating Schema ===");
            SchemaUtils.createSchema(client);
            
            logger.info("=== Loading Core Data ===");
            DataLoader.loadCoreData(client);
            
            if (loadExtended) {
                logger.info("=== Loading Extended Data ===");
                DataLoader.loadExtendedData(client);
            }
            
            logger.info("=== Verification ===");
            verifySetup(client);
            
            logger.info("=== Setup Complete ===");
            logger.info("Music store dataset is ready for use!");
            logger.info("Available tables: Artist, Album, Track, Genre, MediaType, Customer, Employee, Invoice, InvoiceLine, Playlist, PlaylistTrack");
            
        } catch (Exception e) {
            logger.error("Setup failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
    
    private static void verifySetup(IgniteClient client) {
        String[] tables = {"Artist", "Album", "Track", "Genre", "MediaType", 
                          "Customer", "Employee", "Invoice", "InvoiceLine", 
                          "Playlist", "PlaylistTrack"};
        
        logger.info("Data verification:");
        for (String table : tables) {
            long count = ConnectionUtils.getTableRowCount(client, table);
            logger.info("  {}: {} rows", table, count);
        }
    }
}