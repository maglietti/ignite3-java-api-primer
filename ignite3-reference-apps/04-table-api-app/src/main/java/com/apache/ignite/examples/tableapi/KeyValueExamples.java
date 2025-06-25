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

package com.apache.ignite.examples.tableapi;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.KeyValueView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * KeyValue Examples - Cache-like operations with Apache Ignite 3.
 * 
 * This class demonstrates key-value access patterns:
 * - Put/get operations
 * - Working with separate key and value types
 * - Bulk operations
 * - Key-based data access patterns
 * 
 * Learning Focus:
 * - KeyValueView operations
 * - Cache-like usage patterns
 * - Key-based data access
 * - Performance considerations
 */
public class KeyValueExamples {

    private static final Logger logger = LoggerFactory.getLogger(KeyValueExamples.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== KeyValue Examples Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Demonstrating cache-like operations with KeyValueView");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            runKeyValueExamples(client);
            
        } catch (Exception e) {
            logger.error("Failed to run KeyValue examples", e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void runKeyValueExamples(IgniteClient client) {
        // Get the Artist table  
        Table artistTable = client.tables().table("Artist");
        if (artistTable == null) {
            throw new RuntimeException("Artist table not found. Please run sample data setup first.");
        }
        
        // Get a key-value view (Integer key, String value)
        KeyValueView<Integer, String> artistCache = artistTable.keyValueView(Integer.class, String.class);
        
        System.out.println("\n--- Cache-like Operations ---");
        System.out.println("    Using KeyValueView for high-performance cache patterns");
        
        // Demonstrate basic key-value operations
        demonstrateBasicKeyValue(artistCache);
        
        // Demonstrate bulk operations
        demonstrateBulkOperations(artistCache);
        
        System.out.println("\n>>> KeyValue examples completed successfully");
    }

    private static void demonstrateBasicKeyValue(KeyValueView<Integer, String> artistCache) {
        System.out.println("\n    --- Basic Key-Value Operations");
        System.out.println("    >>> Storing key-value pairs using cache-like API");
        
        // PUT: Store key-value pairs
        artistCache.put(null, 5003, "Cache Demo Artist 1");
        artistCache.put(null, 5004, "Cache Demo Artist 2");
        System.out.println("    <<< Stored two artists in cache");
        
        System.out.println("    >>> Retrieving values by key");
        // GET: Retrieve by key
        String artist1 = artistCache.get(null, 5003);
        String artist2 = artistCache.get(null, 5004);
        System.out.println("    <<< Retrieved: " + artist1);
        System.out.println("    <<< Retrieved: " + artist2);
        
        // GET non-existent key
        String missing = artistCache.get(null, 9999);
        System.out.println("    <<< Missing key result: " + missing);
        
        System.out.println("    >>> Updating existing cache entry");
        // UPDATE: Put with same key overwrites
        artistCache.put(null, 5003, "Updated Cache Artist 1");
        String updated = artistCache.get(null, 5003);
        System.out.println("    <<< Updated: " + updated);
        
        System.out.println("    >>> Removing cache entry");
        // REMOVE: Delete by key
        boolean removed = artistCache.remove(null, 5003);
        System.out.println("    <<< Removed: " + removed);
        
        // VERIFY removal
        String afterRemoval = artistCache.get(null, 5003);
        System.out.println("    <<< After removal: " + afterRemoval);
    }

    private static void demonstrateBulkOperations(KeyValueView<Integer, String> artistCache) {
        System.out.println("\n    --- Bulk Operations");
        System.out.println("    >>> Performing batch operations for better performance");
        
        // PUT ALL: Store multiple key-value pairs at once
        Map<Integer, String> artists = Map.of(
            5005, "Bulk Artist 1",
            5006, "Bulk Artist 2", 
            5007, "Bulk Artist 3"
        );
        
        artistCache.putAll(null, artists);
        System.out.println("    <<< Bulk inserted " + artists.size() + " artists");
        
        System.out.println("    >>> Retrieving multiple values at once");
        // GET ALL: Retrieve multiple values at once
        Set<Integer> keys = Set.of(5005, 5006, 5007, 9999); // Include non-existent key
        Map<Integer, String> retrieved = artistCache.getAll(null, keys);
        
        System.out.println("    <<< Bulk retrieved " + retrieved.size() + " artists:");
        retrieved.forEach((key, value) -> 
            System.out.println("         " + key + " -> " + value));
        
        System.out.println("    >>> Removing multiple keys at once");
        // REMOVE ALL: Delete multiple keys at once
        Set<Integer> keysToRemove = Set.of(5004, 5005, 5006, 5007);
        Set<Integer> removedKeys = Set.copyOf(artistCache.removeAll(null, keysToRemove));
        
        System.out.println("    <<< Bulk removed " + removedKeys.size() + " artists");
        System.out.println("    <<< Removed keys: " + removedKeys);
        
        // VERIFY bulk removal
        Map<Integer, String> afterBulkRemoval = artistCache.getAll(null, keysToRemove);
        System.out.println("    <<< After bulk removal: " + afterBulkRemoval.size() + " artists remain");
    }
}