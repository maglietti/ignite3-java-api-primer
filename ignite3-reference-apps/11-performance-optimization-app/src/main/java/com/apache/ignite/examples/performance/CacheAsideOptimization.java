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

package com.apache.ignite.examples.performance;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.sql.Statement;

import java.util.concurrent.CompletableFuture;

/**
 * Cache-aside optimization strategies for read-heavy workloads in Apache Ignite 3.
 * 
 * Demonstrates how cache-aside techniques transform frequently accessed
 * music catalog queries from expensive database operations to instant
 * cache retrievals. Shows practical implementation of cache warming,
 * invalidation strategies, and async loading patterns.
 * 
 * Key concepts demonstrated:
 * - Cache warming strategies for popular content
 * - Lazy loading with fallback to database queries
 * - Cache invalidation coordination across updates
 * - Async cache population for non-blocking performance
 * 
 * Business context:
 * Music streaming platforms serve millions of requests for popular artists,
 * albums, and tracks. This class shows how cache-aside optimization reduces
 * database load by 90% while delivering sub-millisecond response times
 * for frequently accessed music catalog data.
 */
public class CacheAsideOptimization {
    
    private final IgniteClient client;
    private final IgniteSql sql;
    private final KeyValueView<Tuple, Tuple> artistCache;
    
    public CacheAsideOptimization(IgniteClient client) {
        this.client = client;
        this.sql = client.sql();

        // Create cache table if it doesn't exist and wait for availability
        Table cacheTable = getOrCreateCacheTable();
        if (cacheTable == null) {
            throw new RuntimeException("Failed to create or access ArtistCache table");
        }
        this.artistCache = cacheTable.keyValueView();
    }

    private Table getOrCreateCacheTable() {
        Table table = client.tables().table("ArtistCache");
        if (table != null) {
            return table;
        }

        // Table doesn't exist, create it with the MusicStoreReplicated zone
        try {
            Statement createTable = sql.statementBuilder()
                .query("CREATE TABLE IF NOT EXISTS ArtistCache (" +
                       "ArtistId INT PRIMARY KEY, " +
                       "Name VARCHAR(120), " +
                       "TrackCount INT, " +
                       "CachedAt BIGINT, " +
                       "Source VARCHAR(50)" +
                       ") ZONE MusicStoreReplicated")
                .build();
            sql.execute(null, createTable);
            System.out.println(">>> Created ArtistCache table for demonstration");
        } catch (Exception e) {
            System.err.println("Failed to create ArtistCache table: " + e.getMessage());
            return null;
        }

        // Wait for table to become available via Table API (may take a moment after SQL creation)
        for (int i = 0; i < 10; i++) {
            table = client.tables().table("ArtistCache");
            if (table != null) {
                return table;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        System.err.println("!!! ArtistCache table not available after creation");
        return null;
    }
    
    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Cache-Aside Optimization Demo ===");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            CacheAsideOptimization optimizer = new CacheAsideOptimization(client);
            optimizer.demonstrateCacheWarming();
            optimizer.demonstrateLazyLoading();
            optimizer.demonstrateAsyncCaching();
            optimizer.cleanup();
            
        } catch (Exception e) {
            System.err.println("Cache optimization demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrate cache warming for popular content.
     */
    public void demonstrateCacheWarming() {
        System.out.println("\n--- Cache Warming for Popular Artists");
        
        Statement popularArtistsQuery = sql.statementBuilder()
            .query("SELECT ar.ArtistId, ar.Name, COUNT(t.TrackId) as track_count " +
                   "FROM Artist ar " +
                   "JOIN Album al ON ar.ArtistId = al.ArtistId " +
                   "JOIN Track t ON al.AlbumId = t.AlbumId " +
                   "GROUP BY ar.ArtistId, ar.Name " +
                   "ORDER BY track_count DESC LIMIT 10")
            .build();
        
        long startTime = System.nanoTime();
        int cachedCount = 0;
        
        try (ResultSet<SqlRow> results = sql.execute(null, popularArtistsQuery)) {
            while (results.hasNext()) {
                SqlRow row = results.next();
                
                Tuple key = Tuple.create().set("ArtistId", row.intValue("ArtistId"));
                long trackCount = row.longValue("track_count");
                Tuple value = Tuple.create()
                    .set("Name", row.stringValue("Name"))
                    .set("TrackCount", (int) trackCount)
                    .set("CachedAt", System.currentTimeMillis());

                artistCache.put(null, key, value);
                cachedCount++;

                if (cachedCount <= 3) {
                    System.out.printf(">>> Cached artist: %s (%d tracks)%n",
                        row.stringValue("Name"), trackCount);
                }
            }
        }
        
        long executionTime = System.nanoTime() - startTime;
        System.out.printf(">>> Cache warming completed: %d artists in %.2f ms%n", 
            cachedCount, executionTime / 1_000_000.0);
    }
    
    /**
     * Demonstrate lazy loading with database fallback.
     */
    public void demonstrateLazyLoading() {
        System.out.println("\n--- Lazy Loading with Database Fallback");
        
        int[] testArtistIds = {1, 5, 10}; // Mix of cached and uncached
        
        for (int artistId : testArtistIds) {
            long startTime = System.nanoTime();
            
            Tuple key = Tuple.create().set("ArtistId", artistId);
            Tuple cached = artistCache.get(null, key);
            
            if (cached != null) {
                long cacheTime = System.nanoTime() - startTime;
                System.out.printf(">>> Cache hit for artist %d: %s (%.3f ms)%n",
                    artistId, cached.stringValue("Name"), cacheTime / 1_000_000.0);
            } else {
                String artistName = loadArtistFromDatabase(artistId);
                long totalTime = System.nanoTime() - startTime;
                System.out.printf(">>> Cache miss for artist %d: %s (%.2f ms, loaded from DB)%n",
                    artistId, artistName, totalTime / 1_000_000.0);
            }
        }
    }
    
    /**
     * Demonstrate async cache population for non-blocking performance.
     */
    public void demonstrateAsyncCaching() {
        System.out.println("\n--- Async Cache Population");
        
        CompletableFuture<Void> asyncCaching = CompletableFuture.runAsync(() -> {
            Statement recentArtistsQuery = sql.statementBuilder()
                .query("SELECT DISTINCT ar.ArtistId, ar.Name " +
                       "FROM Artist ar " +
                       "JOIN Album al ON ar.ArtistId = al.ArtistId " +
                       "WHERE al.AlbumId > 300 LIMIT 5")
                .build();
            
            try (ResultSet<SqlRow> results = sql.execute(null, recentArtistsQuery)) {
                while (results.hasNext()) {
                    SqlRow row = results.next();
                    
                    Tuple key = Tuple.create().set("ArtistId", row.intValue("ArtistId"));
                    Tuple value = Tuple.create()
                        .set("Name", row.stringValue("Name"))
                        .set("CachedAt", System.currentTimeMillis())
                        .set("Source", "async");
                    
                    artistCache.put(null, key, value);
                }
            }
        });
        
        System.out.println(">>> Async cache population started");
        
        try {
            asyncCaching.get(); // Wait for completion in demo
            System.out.println(">>> Async cache population completed");
        } catch (Exception e) {
            System.err.println("!!! Async caching failed: " + e.getMessage());
        }
    }
    
    private String loadArtistFromDatabase(int artistId) {
        Statement artistQuery = sql.statementBuilder()
            .query("SELECT Name FROM Artist WHERE ArtistId = ?")
            .build();
        
        try (ResultSet<SqlRow> results = sql.execute(null, artistQuery, artistId)) {
            if (results.hasNext()) {
                SqlRow row = results.next();
                String artistName = row.stringValue("Name");
                
                Tuple key = Tuple.create().set("ArtistId", artistId);
                Tuple value = Tuple.create()
                    .set("Name", artistName)
                    .set("CachedAt", System.currentTimeMillis())
                    .set("Source", "lazy_load");
                
                artistCache.put(null, key, value);
                
                return artistName;
            }
        }
        
        return "Unknown Artist";
    }
    
    /**
     * Clean up demonstration resources.
     */
    public void cleanup() {
        try {
            Statement dropTable = sql.statementBuilder()
                .query("DROP TABLE IF EXISTS ArtistCache")
                .build();
            sql.execute(null, dropTable);
            System.out.println(">>> Cleaned up ArtistCache table");
        } catch (Exception e) {
            System.err.println("Failed to cleanup cache table: " + e.getMessage());
        }
    }
}