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
        
        printWelcomeBanner();
        logger.info("Apache Ignite 3 Music Store Sample Data Setup");
        logger.info("Target cluster: {}", clusterAddress);
        logger.info("Dataset mode: {}", loadExtended ? "COMPLETE (15,000+ records)" : "CORE (sample records)");
        logger.info("Schema action: {}", resetSchema ? "RESET (drop and recreate)" : "CREATE (preserve existing)");
        logger.info("");
        
        try (IgniteClient client = ConnectionUtils.connectToCluster(clusterAddress)) {
            
            if (resetSchema) {
                logger.info("CHECKPOINT 1/5: Resetting Schema");
                logger.info("   -> Removing existing tables and zones to start fresh...");
                SchemaUtils.dropSchema(client);
                logger.info("   -> Schema reset completed successfully");
                logger.info("");
            } else {
                logger.info("CHECKPOINT 1/5: Schema Validation");
                logger.info("   -> Checking for existing music store tables...");
                if (!SchemaUtils.checkSchemaAndPromptUser(client)) {
                    logger.info("   -> Setup cancelled by user");
                    return;
                }
                logger.info("   -> Schema validation completed");
                logger.info("");
            }
            
            logger.info("CHECKPOINT 2/5: Building Database Schema");
            logger.info("   -> Creating distribution zones for optimal data placement...");
            logger.info("   -> Creating 11 tables with proper relationships and indexes...");
            logger.info("   -> This may take 30-60 seconds as Ignite configures the distributed schema...");
            SchemaUtils.createSchema(client);
            logger.info("   -> Database schema created successfully!");
            logger.info("");
            
            logger.info("CHECKPOINT 3/5: Loading Core Sample Data");
            logger.info("   -> Adding essential music store data for development and testing...");
            DataLoader.loadCoreData(client);
            logger.info("   -> Core sample data loaded (5 artists, 5 albums, 5 tracks, 3 customers)");
            logger.info("");
            
            if (loadExtended) {
                logger.info("CHECKPOINT 4/5: Loading Complete Music Store Dataset");
                logger.info("   -> Processing 15,866-line SQL script with full music catalog...");
                logger.info("   -> This creates a realistic dataset for learning and development...");
                logger.info("   -> Expected completion time: 2-3 minutes depending on system performance...");
                DataLoader.loadExtendedData(client);
                logger.info("   -> Complete dataset loaded successfully!");
                logger.info("");
            } else {
                logger.info("CHECKPOINT 4/5: Skipped Extended Data");
                logger.info("   -> Run with --extended flag to load the complete 15,000+ record dataset");
                logger.info("");
            }
            
            logger.info("CHECKPOINT 5/5: Final Verification");
            logger.info("   -> Validating all tables and counting records...");
            verifySetup(client, loadExtended);
            
            printSuccessBanner(loadExtended);
            
        } catch (Exception e) {
            logger.error("Setup failed with error: {}", e.getMessage());
            logger.error("   -> Check that your Ignite cluster is running and accessible");
            logger.error("   -> Verify network connectivity to: {}", clusterAddress);
            logger.error("   -> Review the full error details below:");
            logger.error("", e);
            System.exit(1);
        }
    }
    
    private static void verifySetup(IgniteClient client, boolean loadExtended) {
        String[] tables = {"Artist", "Album", "Track", "Genre", "MediaType", 
                          "Customer", "Employee", "Invoice", "InvoiceLine", 
                          "Playlist", "PlaylistTrack"};
        
        logger.info("   -> Verifying table creation and data loading:");
        long totalRecords = 0;
        
        for (String table : tables) {
            long count = ConnectionUtils.getTableRowCount(client, table);
            totalRecords += count;
            
            // Add context about what each table contains
            String description = getTableDescription(table, count, loadExtended);
            logger.info("      * {}: {} rows {}", table, count, description);
        }
        
        logger.info("   -> Total records across all tables: {}", totalRecords);
        logger.info("   -> All tables verified successfully!");
        logger.info("");
    }
    
    private static String getTableDescription(String tableName, long count, boolean extended) {
        switch (tableName) {
            case "Artist": return extended ? "(complete music catalog)" : "(sample artists)";
            case "Album": return extended ? "(full album collection)" : "(sample albums)";
            case "Track": return extended ? "(complete track library)" : "(sample tracks)";
            case "Genre": return "(music genres for categorization)";
            case "MediaType": return "(audio/video format types)";
            case "Customer": return extended ? "(customer database)" : "(sample customers)";
            case "Employee": return "(store staff hierarchy)";
            case "Invoice": return extended ? "(complete sales history)" : "(sample transactions)";
            case "InvoiceLine": return extended ? "(detailed purchase records)" : "(sample line items)";
            case "Playlist": return extended ? "(user-created playlists)" : "(sample playlists)";
            case "PlaylistTrack": return extended ? "(playlist associations)" : "(sample playlist items)";
            default: return "";
        }
    }
    
    private static void printWelcomeBanner() {
        logger.info("=========================================================================");
        logger.info("                                                                         ");
        logger.info("    Apache Ignite 3 Music Store Sample Data Setup                      ");
        logger.info("                                                                         ");
        logger.info("  This application creates a complete music store dataset for          ");
        logger.info("  learning Apache Ignite 3 distributed database concepts.             ");
        logger.info("                                                                         ");
        logger.info("  Features:                                                            ");
        logger.info("  * Schema-as-code using annotated POJOs                              ");
        logger.info("  * Distribution zones for optimal data placement                     ");
        logger.info("  * Realistic sample data for development and testing                 ");
        logger.info("  * Complete music catalog with 15,000+ records (--extended)          ");
        logger.info("                                                                         ");
        logger.info("=========================================================================");
        logger.info("");
    }
    
    private static void printSuccessBanner(boolean loadExtended) {
        logger.info("=========================================================================");
        logger.info("                                                                         ");
        logger.info("                        SETUP COMPLETED!                               ");
        logger.info("                                                                         ");
        logger.info("  Your Apache Ignite 3 music store dataset is ready for use!          ");
        logger.info("                                                                         ");
        
        if (loadExtended) {
            logger.info("  Complete Dataset Loaded:                                           ");
            logger.info("     * 275+ Artists * 347+ Albums * 3,500+ Tracks                   ");
            logger.info("     * 59 Customers * 412+ Invoices * 18 Playlists                  ");
            logger.info("     * Full business relationships and realistic data                ");
        } else {
            logger.info("  Core Dataset Loaded:                                               ");
            logger.info("     * 5 Artists * 5 Albums * 5 Tracks * 3 Customers                ");
            logger.info("     * Perfect for development and API learning                     ");
            logger.info("     * Run with --extended for the complete dataset                 ");
        }
        
        logger.info("                                                                         ");
        logger.info("  Next Steps:                                                          ");
        logger.info("     * Explore data with SQL queries                                   ");
        logger.info("     * Study the annotated POJOs in model/ directory                  ");
        logger.info("     * Try other reference application modules                        ");
        logger.info("                                                                         ");
        logger.info("  Available Tables:                                                    ");
        logger.info("     Artist, Album, Track, Genre, MediaType, Customer,                ");
        logger.info("     Employee, Invoice, InvoiceLine, Playlist, PlaylistTrack          ");
        logger.info("                                                                         ");
        logger.info("=========================================================================");
    }
}