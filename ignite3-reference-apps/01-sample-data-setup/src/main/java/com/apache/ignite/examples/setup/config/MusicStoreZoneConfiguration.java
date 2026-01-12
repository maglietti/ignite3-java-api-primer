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
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
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
        createOrUpdateZone(client, MUSIC_STORE_ZONE, MUSIC_STORE_REPLICAS, DEFAULT_PARTITIONS);
    }

    public static void createMusicStoreReplicatedZone(IgniteClient client) {
        createOrUpdateZone(client, MUSIC_STORE_REPLICATED_ZONE, MUSIC_STORE_REPLICATED_REPLICAS, DEFAULT_PARTITIONS);
    }

    private static void createOrUpdateZone(IgniteClient client, String zoneName, int expectedReplicas, int expectedPartitions) {
        try {
            ZoneSettings currentSettings = getZoneSettings(client, zoneName);

            if (currentSettings == null) {
                // Zone does not exist, create it
                ZoneDefinition zoneDefinition = ZoneDefinition.builder(zoneName)
                    .replicas(expectedReplicas)
                    .partitions(expectedPartitions)
                    .storageProfiles("default")
                    .build();

                client.catalog().createZone(zoneDefinition);
                logger.info("Created zone {} with REPLICAS={}, PARTITIONS={}",
                    zoneName, expectedReplicas, expectedPartitions);
            } else {
                // Zone exists, check if settings match
                boolean replicasMismatch = currentSettings.replicas != expectedReplicas;
                boolean partitionsMismatch = currentSettings.partitions != expectedPartitions;

                if (replicasMismatch || partitionsMismatch) {
                    logger.warn("Zone {} exists with different settings: REPLICAS={} (expected {}), PARTITIONS={} (expected {})",
                        zoneName, currentSettings.replicas, expectedReplicas,
                        currentSettings.partitions, expectedPartitions);

                    if (replicasMismatch) {
                        // Partitions cannot be altered after creation, but replicas can
                        String alterSql = String.format("ALTER ZONE %s SET REPLICAS=%d",
                            zoneName.toUpperCase(), expectedReplicas);
                        client.sql().execute(null, alterSql);
                        logger.info("Altered zone {} REPLICAS: {} -> {}",
                            zoneName, currentSettings.replicas, expectedReplicas);
                    }

                    if (partitionsMismatch) {
                        // Partitions cannot be changed after zone creation
                        logger.warn("Zone {} has PARTITIONS={} but expected {}. Partition count cannot be altered after creation.",
                            zoneName, currentSettings.partitions, expectedPartitions);
                        logger.warn("To change partitions, drop all tables in the zone, drop the zone, and recreate.");
                    }
                } else {
                    logger.info("Zone {} exists with correct settings: REPLICAS={}, PARTITIONS={}",
                        zoneName, currentSettings.replicas, currentSettings.partitions);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to create or update zone {}: {}", zoneName, e.getMessage());
            throw new RuntimeException("Failed to create or update zone: " + zoneName, e);
        }
    }

    private static ZoneSettings getZoneSettings(IgniteClient client, String zoneName) {
        try (ResultSet<SqlRow> resultSet = client.sql().execute(null,
                "SELECT ZONE_REPLICAS, ZONE_PARTITIONS FROM SYSTEM.ZONES WHERE ZONE_NAME = ?",
                zoneName.toUpperCase())) {

            if (resultSet.hasNext()) {
                SqlRow row = resultSet.next();
                int replicas = row.intValue("ZONE_REPLICAS");
                int partitions = row.intValue("ZONE_PARTITIONS");
                return new ZoneSettings(replicas, partitions);
            }
            return null;
        } catch (Exception e) {
            logger.warn("Error checking zone settings for {}: {}", zoneName, e.getMessage());
            return null;
        }
    }

    private static class ZoneSettings {
        final int replicas;
        final int partitions;

        ZoneSettings(int replicas, int partitions) {
            this.replicas = replicas;
            this.partitions = partitions;
        }
    }
    
    public static void dropDistributionZones(IgniteClient client) {
        dropZone(client, MUSIC_STORE_ZONE);
        dropZone(client, MUSIC_STORE_REPLICATED_ZONE);
    }

    public static void dropZone(IgniteClient client, String zoneName) {
        try {
            if (zoneExists(client, zoneName)) {
                String dropZoneSQL = "DROP ZONE " + zoneName.toUpperCase();
                client.sql().execute(null, dropZoneSQL);
                logger.info("Dropped zone: {}", zoneName);
            } else {
                logger.info("Zone does not exist, skipping drop: {}", zoneName);
            }
        } catch (Exception e) {
            logger.warn("Failed to drop zone {}: {}", zoneName, e.getMessage());
        }
    }

    public static boolean zoneExists(IgniteClient client, String zoneName) {
        return getZoneSettings(client, zoneName) != null;
    }

    public static void displayZoneInfo(IgniteClient client) {
        try {
            logger.info("Current distribution zones:");
            try (ResultSet<SqlRow> resultSet = client.sql().execute(null,
                    "SELECT ZONE_NAME, ZONE_REPLICAS, ZONE_PARTITIONS, ZONE_CONSISTENCY_MODE FROM SYSTEM.ZONES ORDER BY ZONE_NAME")) {

                resultSet.forEachRemaining(row -> {
                    String name = row.stringValue("ZONE_NAME");
                    int replicas = row.intValue("ZONE_REPLICAS");
                    int partitions = row.intValue("ZONE_PARTITIONS");
                    String consistencyMode = row.stringValue("ZONE_CONSISTENCY_MODE");
                    logger.info("    {} - REPLICAS={}, PARTITIONS={}, CONSISTENCY={}",
                        name, replicas, partitions, consistencyMode);
                });
            }
        } catch (Exception e) {
            logger.error("Error displaying zone information: {}", e.getMessage());
        }
    }
}