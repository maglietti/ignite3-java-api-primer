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

package com.apache.ignite.examples.caching;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Cache-Aside Patterns - Read-heavy catalog operations with Apache Ignite 3.
 * 
 * This class demonstrates cache-aside pattern for music catalog operations:
 * - Cache miss handling with external data source loading
 * - Cache hit optimization for frequently accessed data  
 * - Batch operations for related data loading
 * - Async patterns for non-blocking operations
 * - Cache warming strategies for application startup
 * 
 * Learning Focus:
 * - When to use cache-aside (read-heavy, can tolerate cache misses)
 * - Lazy loading patterns with external data sources
 * - Performance optimization through batching and async operations
 * - Cache warming strategies to improve startup performance
 */
public class CacheAsidePatterns {

    private static final Logger logger = LoggerFactory.getLogger(CacheAsidePatterns.class);
    
    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Cache-Aside Patterns Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            runCacheAsidePatterns(client);
            
        } catch (Exception e) {
            logger.error("Failed to run cache-aside patterns", e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void runCacheAsidePatterns(IgniteClient client) {
        // Get the Artist table for caching operations
        Table artistTable = client.tables().table("Artist");
        if (artistTable == null) {
            throw new RuntimeException("Artist table not found. Please run sample data setup first.");
        }
        
        KeyValueView<Tuple, Tuple> artistCache = artistTable.keyValueView();
        
        System.out.println("\n--- Cache-Aside Pattern Demonstrations ---");
        
        // Demonstrate core cache-aside patterns
        demonstrateCacheMissAndHit(artistCache);
        demonstrateBatchLoading(artistCache);
        demonstrateAsyncOperations(artistCache);
        demonstrateCacheWarming(artistCache);
        
        System.out.println("\n>>> Cache-aside patterns completed successfully");
    }

    /**
     * Demonstrates cache miss followed by cache hit pattern.
     * 
     * Shows the fundamental cache-aside flow:
     * 1. Check cache first
     * 2. On miss, load from external source
     * 3. Store in cache for future requests
     * 4. Subsequent requests hit cache
     */
    private static void demonstrateCacheMissAndHit(KeyValueView<Tuple, Tuple> artistCache) {
        System.out.println("\n1. Cache Miss and Hit Pattern:");
        
        Tuple artistKey = Tuple.create().set("ArtistId", 1);
        
        // First access - cache miss
        System.out.println("   First access (cache miss expected):");
        Tuple artist = artistCache.get(null, artistKey);
        if (artist != null) {
            System.out.println("   >>> Cache hit: " + artist.stringValue("Name"));
        } else {
            // Simulate loading from external data source
            System.out.println("   !!! Cache miss - loading from external source");
            Tuple newArtist = loadArtistFromExternalSource(1, "AC/DC");
            artistCache.put(null, artistKey, newArtist);
            System.out.println("   >>> Loaded and cached: " + newArtist.stringValue("Name"));
        }
        
        // Second access - cache hit
        System.out.println("   Second access (cache hit expected):");
        Tuple cachedArtist = artistCache.get(null, artistKey);
        if (cachedArtist != null) {
            System.out.println("   >>> Cache hit: " + cachedArtist.stringValue("Name"));
        }
    }

    /**
     * Demonstrates batch loading optimization.
     * 
     * Shows how to efficiently load multiple related items:
     * - Check cache for all requested items
     * - Identify missing items (cache misses)
     * - Batch load missing items from external source
     * - Update cache with loaded items
     */
    private static void demonstrateBatchLoading(KeyValueView<Tuple, Tuple> artistCache) {
        System.out.println("\n2. Batch Loading Pattern:");
        
        List<Integer> requestedArtistIds = List.of(10, 11, 12, 13, 14);
        Map<Integer, Tuple> results = new HashMap<>();
        List<Integer> cacheHits = new ArrayList<>();
        List<Integer> cacheMisses = new ArrayList<>();
        
        // Phase 1: Check cache for all requested items
        System.out.println("   Checking cache for " + requestedArtistIds.size() + " artists");
        for (Integer artistId : requestedArtistIds) {
            Tuple key = Tuple.create().set("ArtistId", artistId);
            Tuple artist = artistCache.get(null, key);
            
            if (artist != null) {
                results.put(artistId, artist);
                cacheHits.add(artistId);
            } else {
                cacheMisses.add(artistId);
            }
        }
        
        System.out.printf("   >>> Cache hits: %d, Cache misses: %d%n", cacheHits.size(), cacheMisses.size());
        
        // Phase 2: Batch load missing items
        if (!cacheMisses.isEmpty()) {
            System.out.println("   Batch loading " + cacheMisses.size() + " missing artists");
            Map<Integer, Tuple> loadedArtists = batchLoadArtistsFromExternalSource(cacheMisses);
            
            // Phase 3: Update cache with loaded items
            for (Map.Entry<Integer, Tuple> entry : loadedArtists.entrySet()) {
                Tuple key = Tuple.create().set("ArtistId", entry.getKey());
                artistCache.put(null, key, entry.getValue());
                results.put(entry.getKey(), entry.getValue());
            }
            System.out.println("   >>> Cached " + loadedArtists.size() + " newly loaded artists");
        }
        
        System.out.printf("   >>> Total results returned: %d/%d%n", results.size(), requestedArtistIds.size());
    }

    /**
     * Demonstrates async cache-aside operations.
     * 
     * Shows non-blocking patterns for better performance:
     * - Async cache lookups
     * - Async external data loading
     * - Chaining async operations
     */
    private static void demonstrateAsyncOperations(KeyValueView<Tuple, Tuple> artistCache) {
        System.out.println("\n3. Async Operations Pattern:");
        
        try {
            // Async single lookup
            System.out.println("   Async single artist lookup");
            CompletableFuture<Tuple> asyncArtist = getArtistAsync(artistCache, 20);
            Tuple result = asyncArtist.get();
            System.out.printf("   >>> Async result: %s%n", 
                result != null ? result.stringValue("Name") : "Not found");
            
            // Async batch operations
            System.out.println("   Async batch lookups");
            List<CompletableFuture<Tuple>> asyncResults = new ArrayList<>();
            
            for (int i = 21; i <= 25; i++) {
                asyncResults.add(getArtistAsync(artistCache, i));
            }
            
            // Wait for all async operations
            CompletableFuture<Void> allAsync = CompletableFuture.allOf(
                asyncResults.toArray(new CompletableFuture[0])
            );
            
            allAsync.get();
            System.out.printf("   >>> Completed %d async operations%n", asyncResults.size());
            
        } catch (Exception e) {
            logger.error("Async operations failed", e);
            System.err.println("   !!! Async operations error: " + e.getMessage());
        }
    }

    /**
     * Demonstrates cache warming strategies.
     * 
     * Shows how to pre-populate cache during application startup:
     * - Load frequently accessed data
     * - Optimize application startup performance
     * - Reduce cache miss rate during peak usage
     */
    private static void demonstrateCacheWarming(KeyValueView<Tuple, Tuple> artistCache) {
        System.out.println("\n4. Cache Warming Pattern:");
        
        // Simulate warming cache with popular artists
        List<Integer> popularArtistIds = List.of(30, 31, 32, 33, 34, 35);
        System.out.printf("   Warming cache with %d popular artists%n", popularArtistIds.size());
        
        long startTime = System.currentTimeMillis();
        int warmedCount = 0;
        
        for (Integer artistId : popularArtistIds) {
            Tuple key = Tuple.create().set("ArtistId", artistId);
            Tuple artist = artistCache.get(null, key);
            
            if (artist == null) {
                // Load and cache popular artist
                String artistName = "Popular Artist " + artistId;
                Tuple newArtist = loadArtistFromExternalSource(artistId, artistName);
                artistCache.put(null, key, newArtist);
                warmedCount++;
            }
        }
        
        long endTime = System.currentTimeMillis();
        System.out.printf("   >>> Warmed %d artists in %d ms%n", warmedCount, (endTime - startTime));
        
        // Verify cache warming effectiveness
        System.out.println("   Verifying cache warming effectiveness");
        long verifyStart = System.currentTimeMillis();
        int hitCount = 0;
        
        for (Integer artistId : popularArtistIds) {
            Tuple key = Tuple.create().set("ArtistId", artistId);
            Tuple artist = artistCache.get(null, key);
            if (artist != null) {
                hitCount++;
            }
        }
        
        long verifyEnd = System.currentTimeMillis();
        System.out.printf("   >>> Cache hit rate: %d/%d (%.1f%%) in %d ms%n", 
            hitCount, popularArtistIds.size(), 
            (hitCount * 100.0 / popularArtistIds.size()),
            (verifyEnd - verifyStart));
    }

    // Helper methods for external data source simulation

    private static Tuple loadArtistFromExternalSource(Integer artistId, String name) {
        // Simulate external data source latency
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // For KeyValue view, only include non-key columns in the value tuple
        return Tuple.create()
            .set("Name", name);
    }

    private static Map<Integer, Tuple> batchLoadArtistsFromExternalSource(List<Integer> artistIds) {
        // Simulate batch loading from external source
        Map<Integer, Tuple> results = new HashMap<>();
        
        for (Integer artistId : artistIds) {
            String name = "External Artist " + artistId;
            results.put(artistId, loadArtistFromExternalSource(artistId, name));
        }
        
        return results;
    }

    private static CompletableFuture<Tuple> getArtistAsync(KeyValueView<Tuple, Tuple> artistCache, Integer artistId) {
        return CompletableFuture.supplyAsync(() -> {
            Tuple key = Tuple.create().set("ArtistId", artistId);
            Tuple artist = artistCache.get(null, key);
            
            if (artist == null) {
                // Async load from external source
                String name = "Async Artist " + artistId;
                artist = loadArtistFromExternalSource(artistId, name);
                artistCache.put(null, key, artist);
            }
            
            return artist;
        });
    }
}