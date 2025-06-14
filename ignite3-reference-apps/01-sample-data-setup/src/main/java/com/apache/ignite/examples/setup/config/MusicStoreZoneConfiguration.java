package com.apache.ignite.examples.setup.config;

import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MusicStoreZoneConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(MusicStoreZoneConfiguration.class);
    
    public static final String MUSIC_STORE_ZONE = "MusicStore";
    public static final String MUSIC_STORE_REPLICATED_ZONE = "MusicStoreReplicated";
    
    public static final int MUSIC_STORE_REPLICAS = 2;
    public static final int MUSIC_STORE_REPLICATED_REPLICAS = 3;
    public static final int DEFAULT_PARTITIONS = 25;
    
    public static void createDistributionZones(IgniteClient client) {
        createMusicStoreZone(client);
        createMusicStoreReplicatedZone(client);
    }
    
    public static void createMusicStoreZone(IgniteClient client) {
        try {
            if (!zoneExists(client, MUSIC_STORE_ZONE)) {
                String createZoneSQL = String.format(
                    "CREATE ZONE %s WITH " +
                    "REPLICAS=%d, " +
                    "PARTITIONS=%d, " +
                    "STORAGE_PROFILES='default'",
                    MUSIC_STORE_ZONE, MUSIC_STORE_REPLICAS, DEFAULT_PARTITIONS
                );
                
                client.sql().execute(null, createZoneSQL);
            }
        } catch (Exception e) {
            logger.error("Failed to create distribution zone {}: {}", MUSIC_STORE_ZONE, e.getMessage());
            throw new RuntimeException("Failed to create distribution zone: " + MUSIC_STORE_ZONE, e);
        }
    }
    
    public static void createMusicStoreReplicatedZone(IgniteClient client) {
        try {
            if (!zoneExists(client, MUSIC_STORE_REPLICATED_ZONE)) {
                String createZoneSQL = String.format(
                    "CREATE ZONE %s WITH " +
                    "REPLICAS=%d, " +
                    "PARTITIONS=%d, " +
                    "STORAGE_PROFILES='default'",
                    MUSIC_STORE_REPLICATED_ZONE, MUSIC_STORE_REPLICATED_REPLICAS, DEFAULT_PARTITIONS
                );
                
                client.sql().execute(null, createZoneSQL);
            }
        } catch (Exception e) {
            logger.error("Failed to create distribution zone {}: {}", MUSIC_STORE_REPLICATED_ZONE, e.getMessage());
            throw new RuntimeException("Failed to create distribution zone: " + MUSIC_STORE_REPLICATED_ZONE, e);
        }
    }
    
    public static void dropDistributionZones(IgniteClient client) {
        dropZone(client, MUSIC_STORE_ZONE.toUpperCase());
        dropZone(client, MUSIC_STORE_REPLICATED_ZONE.toUpperCase());
    }
    
    public static void dropZone(IgniteClient client, String zoneName) {
        try {
            if (zoneExists(client, zoneName)) {
                String dropZoneSQL = "DROP ZONE " + zoneName;
                client.sql().execute(null, dropZoneSQL);
                logger.info("Dropped distribution zone: {}", zoneName);
            } else {
                logger.info("Distribution zone does not exist: {}", zoneName);
            }
        } catch (Exception e) {
            logger.warn("Failed to drop distribution zone {}: {}", zoneName, e.getMessage());
        }
    }
    
    public static boolean zoneExists(IgniteClient client, String zoneName) {
        try {
            var resultSet = client.sql().execute(null, 
                "SELECT name FROM SYSTEM.ZONES WHERE name = ?", zoneName.toUpperCase());
            return resultSet.hasNext();
        } catch (Exception e) {
            logger.warn("Error checking if zone exists {}: {}", zoneName, e.getMessage());
            return false;
        }
    }
    
    public static void displayZoneInfo(IgniteClient client) {
        try {
            logger.info("Current distribution zones:");
            var resultSet = client.sql().execute(null, 
                "SELECT name, replicas, partitions FROM SYSTEM.ZONES ORDER BY name");
            
            resultSet.forEachRemaining(row -> {
                String name = row.stringValue("name");
                int replicas = row.intValue("replicas");
                int partitions = row.intValue("partitions");
                logger.info("  Zone: {} - Replicas: {}, Partitions: {}", name, replicas, partitions);
            });
        } catch (Exception e) {
            logger.error("Error displaying zone information: {}", e.getMessage());
        }
    }
}