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

package com.apache.ignite.examples.setup.config;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.catalog.definitions.ZoneDefinition;
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
                ZoneDefinition musicStoreZone = ZoneDefinition.builder('"' + MUSIC_STORE_ZONE + '"')
                    .ifNotExists()
                    .replicas(MUSIC_STORE_REPLICAS)
                    .partitions(DEFAULT_PARTITIONS)
                    .storageProfiles("default")
                    .build();
                
                client.catalog().createZone(musicStoreZone);
                logger.info("Created distribution zone {} (default STRONG_CONSISTENCY mode)", MUSIC_STORE_ZONE);
            }
        } catch (Exception e) {
            logger.error("Failed to create distribution zone {}: {}", MUSIC_STORE_ZONE, e.getMessage());
            throw new RuntimeException("Failed to create distribution zone: " + MUSIC_STORE_ZONE, e);
        }
    }
    
    public static void createMusicStoreReplicatedZone(IgniteClient client) {
        try {
            if (!zoneExists(client, MUSIC_STORE_REPLICATED_ZONE)) {
                ZoneDefinition replicatedZone = ZoneDefinition.builder('"' + MUSIC_STORE_REPLICATED_ZONE + '"')
                    .ifNotExists()
                    .replicas(MUSIC_STORE_REPLICATED_REPLICAS)
                    .partitions(DEFAULT_PARTITIONS)
                    .storageProfiles("default")
                    .build();
                
                client.catalog().createZone(replicatedZone);
                logger.info("Created distribution zone {} (default STRONG_CONSISTENCY mode)", MUSIC_STORE_REPLICATED_ZONE);
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
                "SELECT name, replicas, partitions, consistency_mode FROM SYSTEM.ZONES ORDER BY name");
            
            resultSet.forEachRemaining(row -> {
                String name = row.stringValue("NAME");
                int replicas = row.intValue("REPLICAS");
                int partitions = row.intValue("PARTITIONS");
                String consistencyMode = row.stringValue("CONSISTENCY_MODE");
                logger.info("  Zone: {} - Replicas: {}, Partitions: {}, Consistency: {}", 
                    name, replicas, partitions, consistencyMode);
            });
        } catch (Exception e) {
            logger.error("Error displaying zone information: {}", e.getMessage());
        }
    }
}