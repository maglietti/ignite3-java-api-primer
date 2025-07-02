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
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Async Basic Operations - Asynchronous patterns with Apache Ignite 3.
 * 
 * This class demonstrates fundamental async operations:
 * - CompletableFuture patterns
 * - Non-blocking database operations
 * - Async error handling
 * - Performance benefits of async operations
 * 
 * Learning Focus:
 * - Async API basics
 * - CompletableFuture patterns
 * - Non-blocking operations
 * - Error handling in async code
 */
public class AsyncBasicOperations {

    private static final Logger logger = LoggerFactory.getLogger(AsyncBasicOperations.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Async Basic Operations Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Demonstrating non-blocking operations with CompletableFuture");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            runAsyncOperations(client);
            
        } catch (Exception e) {
            logger.error("Failed to run async operations", e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void runAsyncOperations(IgniteClient client) throws ExecutionException, InterruptedException {
        // Get the Artist table
        Table artistTable = client.tables().table("Artist");
        if (artistTable == null) {
            throw new RuntimeException("Artist table not found. Please run sample data setup first.");
        }
        
        RecordView<Tuple> artists = artistTable.recordView();
        
        System.out.println("\n--- Async Operations ---");
        System.out.println("    Using CompletableFuture for non-blocking database operations");
        
        // Demonstrate basic async patterns
        demonstrateSimpleAsync(artists);
        
        // Demonstrate async chaining
        demonstrateAsyncChaining(artists);
        
        // Demonstrate error handling
        demonstrateAsyncErrorHandling(artists);
        
        System.out.println("\n>>> Async operations completed successfully");
    }

    private static void demonstrateSimpleAsync(RecordView<Tuple> artists) 
            throws ExecutionException, InterruptedException {
        System.out.println("\n    --- Simple Async Operations");
        System.out.println(">>> Starting non-blocking database operations");
        
        // Create test data
        Tuple newArtist = Tuple.create()
            .set("ArtistId", 5008)
            .set("Name", "Async Demo Artist");
        
        // ASYNC INSERT: Start the operation without waiting
        CompletableFuture<Void> insertFuture = artists.upsertAsync(null, newArtist);
        System.out.println(">>> Insert started asynchronously...");
        
        // Wait for completion
        insertFuture.get();
        System.out.println("<<< Insert completed");
        
        // ASYNC READ: Non-blocking read
        Tuple key = Tuple.create().set("ArtistId", 5008);
        CompletableFuture<Tuple> readFuture = artists.getAsync(null, key);
        System.out.println(">>> Read started asynchronously...");
        
        // Wait for result
        Tuple result = readFuture.get();
        System.out.println("<<< Read completed: " + result.stringValue("Name"));
        
        // CLEANUP
        artists.deleteAsync(null, key).get();
        System.out.println("<<< Cleanup completed");
    }

    private static void demonstrateAsyncChaining(RecordView<Tuple> artists) 
            throws ExecutionException, InterruptedException {
        System.out.println("\n    --- Async Chaining");
        System.out.println(">>> Chaining operations without blocking threads");
        
        final int artistId = 5009;
        
        // Chain operations without blocking
        CompletableFuture<String> chainedOperation = CompletableFuture
            // Start with creating an artist
            .supplyAsync(() -> {
                System.out.println(">>> Step 1: Creating artist...");
                return Tuple.create()
                    .set("ArtistId", artistId)
                    .set("Name", "Chained Artist");
            })
            // Insert the artist
            .thenCompose(artist -> {
                System.out.println(">>> Step 2: Inserting artist...");
                return artists.upsertAsync(null, artist)
                    .thenApply(ignored -> artist); // Pass the artist to next stage
            })
            // Read it back
            .thenCompose(artist -> {
                System.out.println(">>> Step 3: Reading artist back...");
                Tuple key = Tuple.create().set("ArtistId", artistId);
                return artists.getAsync(null, key);
            })
            // Extract the name
            .thenApply(retrievedArtist -> {
                System.out.println(">>> Step 4: Processing result...");
                if (retrievedArtist != null) {
                    return "Found: " + retrievedArtist.stringValue("Name");
                } else {
                    return "Not found";
                }
            });
        
        // Get the final result
        String finalResult = chainedOperation.get();
        System.out.println("<<< Chain completed: " + finalResult);
        
        // Cleanup
        Tuple key = Tuple.create().set("ArtistId", artistId);
        artists.deleteAsync(null, key).get();
        System.out.println("<<< Cleanup completed");
    }

    private static void demonstrateAsyncErrorHandling(RecordView<Tuple> artists) 
            throws ExecutionException, InterruptedException {
        System.out.println("\n    --- Async Error Handling");
        System.out.println(">>> Demonstrating error handling in async operations");
        
        // Try to read a non-existent artist
        Tuple key = Tuple.create().set("ArtistId", 99999);
        
        CompletableFuture<String> errorHandlingDemo = artists.getAsync(null, key)
            .thenApply(artist -> {
                if (artist != null) {
                    return "Found: " + artist.stringValue("Name");
                } else {
                    return "Artist not found - this is expected";
                }
            })
            .exceptionally(throwable -> {
                logger.warn("Error in async operation", throwable);
                return "Error handled: " + throwable.getMessage();
            });
        
        String result = errorHandlingDemo.get();
        System.out.println("<<< Error handling result: " + result);
        
        // Demonstrate successful error recovery
        CompletableFuture<String> recoveryDemo = artists.getAsync(null, key)
            .thenCompose(artist -> {
                if (artist == null) {
                    // Artist not found, create it
                    System.out.println(">>> Artist not found, creating...");
                    Tuple newArtist = Tuple.create()
                        .set("ArtistId", 99999)
                        .set("Name", "Recovery Artist");
                    
                    return artists.upsertAsync(null, newArtist)
                        .thenApply(ignored -> "Created recovery artist");
                } else {
                    return CompletableFuture.completedFuture("Artist already exists");
                }
            });
        
        String recoveryResult = recoveryDemo.get();
        System.out.println("<<< Recovery result: " + recoveryResult);
        
        // Cleanup
        artists.deleteAsync(null, key).get();
        System.out.println("<<< Cleanup completed");
    }
}