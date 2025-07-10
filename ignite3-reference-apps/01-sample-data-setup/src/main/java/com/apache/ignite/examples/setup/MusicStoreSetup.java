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
 * 
 * Maven:
 *   mvn exec:java                                    # Default complete dataset
 *   mvn exec:java -Dexec.args="--core"              # Load minimal sample data only
 *   mvn exec:java -Dexec.args="--reset"             # Drop existing schema and recreate
 *   mvn exec:java -Dexec.args="--reset --core"      # Reset with minimal data
 *   mvn exec:java -Dexec.args="192.168.1.100:10800" # Custom cluster address
 *   mvn exec:java -Dexec.args="192.168.1.100:10800 --core" # Both options
 *   mvn exec:java -Dexec.args="--help"              # Show help information
 * 
 * Gradle:
 *   ./gradlew setupData                              # Default complete dataset
 *   ./gradlew setupData --args="--core"             # Load minimal sample data only
 *   ./gradlew setupData --args="--reset"            # Drop existing schema and recreate
 *   ./gradlew setupData --args="--reset --core"     # Reset with minimal data
 *   ./gradlew setupData --args="192.168.1.100:10800" # Custom cluster address
 *   ./gradlew setupData --args="192.168.1.100:10800 --core" # Both options
 *   ./gradlew setupData --args="--help"             # Show help information
 */
public class MusicStoreSetup {
    
    private static final Logger logger = LoggerFactory.getLogger(MusicStoreSetup.class);
    
    public static void main(String[] args) {
        String clusterAddress = "127.0.0.1:10800";
        boolean loadExtended = true;
        boolean resetSchema = false;
        
        for (String arg : args) {
            if (arg.equals("--help")) {
                displayHelp();
                return;
            } else if (arg.equals("--core")) {
                loadExtended = false;
            } else if (arg.equals("--reset")) {
                resetSchema = true;
            } else if (!arg.startsWith("--")) {
                clusterAddress = arg;
            }
        }
        
        logger.info("=== Apache Ignite 3 Music Store Sample Data Setup ===");
        logger.info("Target cluster: {}", clusterAddress);
        logger.info("Dataset mode: {}", loadExtended ? "COMPLETE (15,000+ records)" : "CORE (sample records)");
        logger.info("Schema action: {}", resetSchema ? "RESET (drop and recreate)" : "CREATE (preserve existing)");
        logger.info("");
        
        logger.info("--- Connecting to Ignite cluster at {}", clusterAddress);
        logger.info("Note: You may see partition assignment notifications - this is normal");
        
        try (IgniteClient client = ConnectionUtils.connectToCluster(clusterAddress)) {
            
            if (resetSchema) {
                logger.info("=== [1/5] Schema Reset");
                logger.info("--- Removing existing tables and zones to start fresh");
                SchemaUtils.dropSchema(client);
                logger.info("=== Schema reset completed");
                logger.info("");
            } else {
                logger.info("=== [1/5] Schema Validation");
                if (!SchemaUtils.checkSchemaAndPromptUser(client)) {
                    logger.info("Setup cancelled by user");
                    return;
                }
                logger.info("<<< Schema validation completed");
                logger.info("");
            }
            
            logger.info("=== [2/5] Schema Creation");
            logger.info("--- Processing distribution zones and table definitions");
            logger.info("This may take 30-60 seconds as Ignite configures the distributed schema");
            SchemaUtils.createSchema(client);
            logger.info("--- Schema created: 2 zones, 11 tables, optimized for colocation");
            logger.info("=== Database schema created successfully");
            logger.info("");
            
            if (loadExtended) {
                logger.info("=== [3/5] Complete Data Loading");
                logger.info("--- Loading complete music store dataset with optimized batch processing");
                logger.info("Processing 275+ artists, 347+ albums, 3,500+ tracks with native Ignite APIs");
                logger.info("Expected completion time: 30-60 seconds with parallel loading");
                DataLoader.loadCompleteDataset(client);
                logger.info("=== Complete dataset loaded successfully");
                logger.info("");
                
                logger.info("=== [4/5] Core Data Loading");
                logger.info("!!! Skipped (using complete dataset instead)");
                logger.info("");
            } else {
                logger.info("=== [3/5] Complete Data Loading");
                logger.info("!!! Skipped (use default behavior for complete dataset)");
                logger.info("");
                
                logger.info("=== [4/5] Core Data Loading");
                logger.info("--- Loading minimal sample data");
                DataLoader.loadCoreData(client);
                logger.info("=== Core data loaded");
                logger.info("");
            }
            
            logger.info("=== [5/5] Verification");
            logger.info("--- Verifying data load");
            verifySetup(client, loadExtended);
            
        } catch (Exception e) {
            logger.error("Setup failed: {}", e.getMessage());
            logger.error("  Check that your Ignite cluster is running and accessible");
            logger.error("  Verify network connectivity to: {}", clusterAddress);
            logger.error("  Review the full error details below:");
            logger.error("", e);
            System.exit(1);
        }
    }
    
    private static void verifySetup(IgniteClient client, boolean loadExtended) {
        String[] tables = {"Artist", "Album", "Track", "Genre", "MediaType", 
                          "Customer", "Employee", "Invoice", "InvoiceLine", 
                          "Playlist", "PlaylistTrack"};
        
        long totalRecords = 0;
        
        for (String table : tables) {
            long count = ConnectionUtils.getTableRowCount(client, table);
            totalRecords += count;
            
            String description = getTableDescription(table, count, loadExtended);
            logger.info("{}: {} {}", table, count, description);
        }
        
        logger.info("Total: {} records across {} tables", totalRecords, tables.length);
        logger.info("All tables verified successfully");
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
    
    /**
     * Displays comprehensive help information for both Maven and Gradle usage.
     */
    private static void displayHelp() {
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════════════════════");
        System.out.println("  Apache Ignite 3 Music Store Sample Data Setup");
        System.out.println("═══════════════════════════════════════════════════════════════════════════════");
        System.out.println();
        System.out.println("Creates and populates a complete music store database for learning Apache Ignite 3.");
        System.out.println("Demonstrates schema-as-code, distribution zones, and transactional data loading.");
        System.out.println();
        
        System.out.println("PREREQUISITES:");
        System.out.println("  • Apache Ignite 3 cluster running (start with: cd 00-docker && ./init-cluster.sh)");
        System.out.println("  • Java 17 or higher");
        System.out.println("  • Maven 3.8+ OR Gradle (wrapper included)");
        System.out.println();
        
        System.out.println("MAVEN USAGE:");
        System.out.println("  mvn exec:java                                    # Default: complete dataset (15,000+ records)");
        System.out.println("  mvn exec:java -Dexec.args=\"--core\"              # Load minimal sample data only");
        System.out.println("  mvn exec:java -Dexec.args=\"--reset\"             # Drop existing schema and recreate");
        System.out.println("  mvn exec:java -Dexec.args=\"--reset --core\"      # Reset with minimal data");
        System.out.println("  mvn exec:java -Dexec.args=\"192.168.1.100:10800\" # Custom cluster address");
        System.out.println("  mvn exec:java -Dexec.args=\"192.168.1.100:10800 --core\" # Custom address + core data");
        System.out.println("  mvn exec:java -Dexec.args=\"--help\"              # Show this help");
        System.out.println();
        
        System.out.println("GRADLE USAGE:");
        System.out.println("  ./gradlew setupData                              # Default: complete dataset (15,000+ records)");
        System.out.println("  ./gradlew setupData --args=\"--core\"             # Load minimal sample data only");
        System.out.println("  ./gradlew setupData --args=\"--reset\"            # Drop existing schema and recreate");
        System.out.println("  ./gradlew setupData --args=\"--reset --core\"     # Reset with minimal data");
        System.out.println("  ./gradlew setupData --args=\"192.168.1.100:10800\" # Custom cluster address");
        System.out.println("  ./gradlew setupData --args=\"192.168.1.100:10800 --core\" # Custom address + core data");
        System.out.println("  ./gradlew setupData --args=\"--help\"             # Show this help");
        System.out.println();
        
        System.out.println("OPTIONS:");
        System.out.println("  --core     Load minimal sample data (5 artists, 5 albums, 5 tracks, etc.)");
        System.out.println("             Fast setup for basic testing and development");
        System.out.println();
        System.out.println("  --reset    Drop existing schema and recreate from scratch");
        System.out.println("             Recommended for clean development environments");
        System.out.println();
        System.out.println("  <address>  Custom cluster address (host:port format)");
        System.out.println("             Default: 127.0.0.1:10800");
        System.out.println();
        
        System.out.println("WHAT THIS CREATES:");
        System.out.println("  • 2 Distribution Zones (MusicStore, MusicStoreReplicated)");
        System.out.println("  • 11 Tables (Artist, Album, Track, Customer, Invoice, etc.)");
        System.out.println("  • Sample Data: 5-25 records (--core) OR 15,000+ records (default)");
        System.out.println("  • Optimized colocation for join performance");
        System.out.println();
        
        System.out.println("EXAMPLES:");
        System.out.println("  # Quick setup for development");
        System.out.println("  ./gradlew setupData --args=\"--reset --core\"");
        System.out.println();
        System.out.println("  # Production-like dataset");
        System.out.println("  ./gradlew setupData --args=\"--reset\"");
        System.out.println();
        System.out.println("  # Connect to remote cluster");
        System.out.println("  ./gradlew setupData --args=\"192.168.1.100:10800 --reset\"");
        System.out.println();
        
        System.out.println("Need help? Check the README.md or visit:");
        System.out.println("https://ignite.apache.org/docs/ignite3/latest/");
        System.out.println();
    }
}