package com.apache.ignite.examples.tableapi;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.lang.NullableValue;
import org.apache.ignite.lang.UnexpectedNullValueException;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Comprehensive demonstration of KeyValueView operations with Apache Ignite 3.
 * 
 * This application showcases cache-like key-value operations using the music store dataset.
 * 
 * Key concepts demonstrated:
 * - KeyValueView setup with different key/value types
 * - Basic put/get operations
 * - Null value handling patterns
 * - Conditional operations (putIfAbsent, replace)
 * - Get-and-modify atomic operations
 * - Bulk operations for performance
 * - Tuple-based operations for complex keys/values
 * - Async KeyValueView patterns
 * 
 * Prerequisites:
 * 1. Ignite cluster running (use 00-docker/init-cluster.sh)
 * 2. Music store schema and data loaded (use 01-sample-data-setup)
 * 
 * Learning Objectives:
 * - Master KeyValueView API for cache-like operations
 * - Understand when KeyValueView excels over RecordView
 * - Learn null value handling strategies
 * - Practice atomic get-and-modify patterns
 * - Master Tuple usage for complex operations
 */
public class KeyValueOperations {
    
    private static final String CLUSTER_ENDPOINT = "127.0.0.1:10800";
    
    public static void main(String[] args) {
        System.out.println("=== Apache Ignite 3 KeyValueView Operations Demo ===");
        System.out.println("Demonstrating cache-like key-value operations with music store data\n");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(CLUSTER_ENDPOINT)
                .build()) {
            
            System.out.println("✓ Connected to Ignite cluster at " + CLUSTER_ENDPOINT);
            
            // Execute all KeyValueView demonstrations
            demonstrateBasicKeyValueOperations(client);
            demonstrateNullValueHandling(client);
            demonstrateConditionalOperations(client);
            demonstrateAtomicOperations(client);
            demonstrateBulkKeyValueOperations(client);
            demonstrateTupleOperations(client);
            demonstrateAsyncKeyValueOperations(client);
            
            System.out.println("\n=== KeyValueView Operations Demo Complete ===");
            
        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Demonstrates basic KeyValueView operations with simple key-value pairs.
     * 
     * Concepts covered:
     * - KeyValueView creation with type safety
     * - Basic put and get operations
     * - getOrDefault for missing values
     * - Key-value separation benefits
     */
    private static void demonstrateBasicKeyValueOperations(IgniteClient client) {
        System.out.println("\n--- Basic KeyValueView Operations    ---");
        
        // Create KeyValueView for Artist table: Integer → String
        Table artistTable = client.tables().table("Artist");
        KeyValueView<Integer, String> artistNames = 
            artistTable.keyValueView(Integer.class, String.class);
        
        System.out.println("✓ Created KeyValueView<Integer, String> for artist names");
        
        System.out.println("\n1. Basic PUT Operations:");
        
        // Put simple key-value pairs
        artistNames.put(null, 6000, "Haken");
        artistNames.put(null, 6001, "Leprous");
        artistNames.put(null, 6002, "Karnivool");
        artistNames.put(null, 6003, "Katatonia");
        
        System.out.println("   ✓ Added 4 progressive artists using put()");
        
        System.out.println("\n2. Basic GET Operations:");
        
        // Get values by key
        String artistName = artistNames.get(null, 6000);
        if (artistName != null) {
            System.out.println("   ✓ Artist 6000: " + artistName);
        }
        
        String anotherName = artistNames.get(null, 6001);
        System.out.println("   ✓ Artist 6001: " + anotherName);
        
        System.out.println("\n3. GET with Default Values:");
        
        // getOrDefault for missing keys
        String unknownArtist = artistNames.getOrDefault(null, 9999, "Unknown Artist");
        System.out.println("   ✓ Artist 9999 (missing): " + unknownArtist);
        
        String knownArtist = artistNames.getOrDefault(null, 6002, "Default Name");
        System.out.println("   ✓ Artist 6002 (exists): " + knownArtist);
        
        System.out.println("\n✓ KeyValueView benefits for simple operations:");
        System.out.println("  - Direct key-value access without object creation");
        System.out.println("  - Type safety for both keys and values");
        System.out.println("  - Minimal overhead for cache-like patterns");
    }
    
    /**
     * Demonstrates null value handling patterns in KeyValueView.
     * 
     * Concepts covered:
     * - Explicit null value storage
     * - getNullable() for null-aware retrieval
     * - Exception handling for null values
     * - Difference between missing and null values
     */
    private static void demonstrateNullValueHandling(IgniteClient client) {
        System.out.println("\n--- Null Value Handling    ---");
        
        Table artistTable = client.tables().table("Artist");
        KeyValueView<Integer, String> artistNames = 
            artistTable.keyValueView(Integer.class, String.class);
        
        System.out.println("\n1. Storing Null Values:");
        
        // Explicitly store null value
        artistNames.put(null, 6010, null);
        System.out.println("   ✓ Stored explicit null for key 6010");
        
        // Store non-null value for comparison
        artistNames.put(null, 6011, "Steven Wilson");
        System.out.println("   ✓ Stored 'Steven Wilson' for key 6011");
        
        System.out.println("\n2. Null-Aware Retrieval:");
        
        // Use getNullable for null-aware retrieval
        NullableValue<String> nullableResult = artistNames.getNullable(null, 6010);
        if (nullableResult != null) {
            String value = nullableResult.get();  // This will be null
            System.out.println("   ✓ Key 6010 exists with null value: " + (value == null));
        }
        
        // Get nullable for existing non-null value
        NullableValue<String> nonNullResult = artistNames.getNullable(null, 6011);
        if (nonNullResult != null) {
            String value = nonNullResult.get();
            System.out.println("   ✓ Key 6011 exists with value: '" + value + "'");
        }
        
        // Get nullable for missing key
        NullableValue<String> missingResult = artistNames.getNullable(null, 6012);
        System.out.println("   ✓ Key 6012 (missing) nullable result: " + (missingResult == null));
        
        System.out.println("\n3. Exception Handling for Null Values:");
        
        // Regular get() throws exception for null values
        try {
            String nullValue = artistNames.get(null, 6010);
            System.out.println("   ✗ This should not print: " + nullValue);
        } catch (UnexpectedNullValueException e) {
            System.out.println("   ✓ get() threw UnexpectedNullValueException for null value");
        }
        
        // Regular get() works fine for non-null values
        try {
            String nonNullValue = artistNames.get(null, 6011);
            System.out.println("   ✓ get() worked for non-null value: " + nonNullValue);
        } catch (UnexpectedNullValueException e) {
            System.out.println("   ✗ Unexpected exception: " + e.getMessage());
        }
        
        System.out.println("\n✓ Null handling patterns:");
        System.out.println("  - Use getNullable() when null values are expected");
        System.out.println("  - Use get() when null values should cause errors");
        System.out.println("  - Distinguish between missing keys and null values");
    }
    
    /**
     * Demonstrates conditional operations that depend on current value state.
     * 
     * Concepts covered:
     * - putIfAbsent for conditional insertion
     * - replace operations with value checking
     * - Atomic conditional updates
     * - Use cases for conditional operations
     */
    private static void demonstrateConditionalOperations(IgniteClient client) {
        System.out.println("\n--- Conditional Operations    ---");
        
        Table artistTable = client.tables().table("Artist");
        KeyValueView<Integer, String> artistNames = 
            artistTable.keyValueView(Integer.class, String.class);
        
        System.out.println("\n1. PUT_IF_ABSENT Operations:");
        
        // putIfAbsent - only insert if key doesn't exist
        boolean inserted1 = artistNames.putIfAbsent(null, 6020, "Riverside");
        System.out.println("   ✓ putIfAbsent for new key 6020: " + inserted1);
        
        // Try putIfAbsent again - should fail since key exists
        boolean inserted2 = artistNames.putIfAbsent(null, 6020, "Different Name");
        System.out.println("   ✓ putIfAbsent for existing key 6020: " + inserted2);
        
        // Verify the original value is preserved
        String preserved = artistNames.get(null, 6020);
        System.out.println("   ✓ Preserved original value: " + preserved);
        
        System.out.println("\n2. REPLACE Operations:");
        
        // replace - only update if key exists
        boolean replaced1 = artistNames.replace(null, 6020, "Riverside (Progressive Rock)");
        System.out.println("   ✓ replace for existing key 6020: " + replaced1);
        
        // Try replace on non-existent key
        boolean replaced2 = artistNames.replace(null, 6021, "Should Not Work");
        System.out.println("   ✓ replace for missing key 6021: " + replaced2);
        
        System.out.println("\n3. Conditional Replace with Value Check:");
        
        // Get current value
        String currentValue = artistNames.get(null, 6020);
        System.out.println("   ✓ Current value for 6020: " + currentValue);
        
        // replace with value check - update only if current value matches
        boolean conditionalReplace1 = artistNames.replace(
            null, 6020, currentValue, "Riverside (Polish Progressive Rock)");
        System.out.println("   ✓ Conditional replace with correct value: " + conditionalReplace1);
        
        // Try conditional replace with wrong current value
        boolean conditionalReplace2 = artistNames.replace(
            null, 6020, "Wrong Value", "Should Not Update");
        System.out.println("   ✓ Conditional replace with wrong value: " + conditionalReplace2);
        
        // Verify final value
        String finalValue = artistNames.get(null, 6020);
        System.out.println("   ✓ Final value: " + finalValue);
        
        System.out.println("\n✓ Conditional operation use cases:");
        System.out.println("  - putIfAbsent: Cache initialization, avoiding overwrites");
        System.out.println("  - replace: Update existing values, fail if key missing");
        System.out.println("  - Conditional replace: Optimistic locking, conflict detection");
    }
    
    /**
     * Demonstrates atomic get-and-modify operations.
     * 
     * Concepts covered:
     * - getAndPut for atomic read-modify-write
     * - getAndReplace for atomic updates
     * - getAndRemove for atomic read-and-delete
     * - Atomicity guarantees in distributed environment
     */
    private static void demonstrateAtomicOperations(IgniteClient client) {
        System.out.println("\n--- Atomic Get-and-Modify Operations    ---");
        
        Table artistTable = client.tables().table("Artist");
        KeyValueView<Integer, String> artistNames = 
            artistTable.keyValueView(Integer.class, String.class);
        
        // Setup initial data
        artistNames.put(null, 6030, "Pain of Salvation");
        artistNames.put(null, 6031, "Pineapple Thief");
        
        System.out.println("\n1. GET_AND_PUT Operations:");
        
        // getAndPut - atomically get old value and set new value
        String oldValue1 = artistNames.getAndPut(null, 6030, "Pain of Salvation (Progressive Metal)");
        System.out.println("   ✓ getAndPut returned old value: '" + oldValue1 + "'");
        
        String currentValue1 = artistNames.get(null, 6030);
        System.out.println("   ✓ New value after getAndPut: '" + currentValue1 + "'");
        
        // getAndPut on non-existent key
        String oldValue2 = artistNames.getAndPut(null, 6032, "The Mars Volta");
        System.out.println("   ✓ getAndPut on new key returned: " + oldValue2);
        
        System.out.println("\n2. GET_AND_REPLACE Operations:");
        
        // getAndReplace - atomically get old value and replace (only if key exists)
        String oldValue3 = artistNames.getAndReplace(null, 6031, "The Pineapple Thief (UK)");
        System.out.println("   ✓ getAndReplace returned old value: '" + oldValue3 + "'");
        
        // getAndReplace on non-existent key
        String oldValue4 = artistNames.getAndReplace(null, 6033, "Should Not Work");
        System.out.println("   ✓ getAndReplace on missing key returned: " + oldValue4);
        
        System.out.println("\n3. GET_AND_REMOVE Operations:");
        
        // getAndRemove - atomically get value and delete
        String removedValue1 = artistNames.getAndRemove(null, 6031);
        System.out.println("   ✓ getAndRemove returned: '" + removedValue1 + "'");
        
        // Verify removal
        String shouldBeNull = artistNames.get(null, 6031);
        System.out.println("   ✓ Value after removal (should be null): " + shouldBeNull);
        
        // getAndRemove on non-existent key
        String removedValue2 = artistNames.getAndRemove(null, 6034);
        System.out.println("   ✓ getAndRemove on missing key returned: " + removedValue2);
        
        System.out.println("\n✓ Atomic operation benefits:");
        System.out.println("  - Guaranteed atomicity in distributed environment");
        System.out.println("  - Eliminate race conditions");
        System.out.println("  - Reduce network round trips");
        System.out.println("  - Enable lock-free programming patterns");
    }
    
    /**
     * Demonstrates bulk operations with KeyValueView for high performance.
     * 
     * Concepts covered:
     * - Bulk put with Map input
     * - Bulk get with Collection of keys
     * - Bulk remove with Collection of keys
     * - Performance benefits of batching
     */
    private static void demonstrateBulkKeyValueOperations(IgniteClient client) {
        System.out.println("\n--- Bulk KeyValueView Operations    ---");
        
        Table artistTable = client.tables().table("Artist");
        KeyValueView<Integer, String> artistNames = 
            artistTable.keyValueView(Integer.class, String.class);
        
        System.out.println("\n1. Bulk PUT Operations:");
        
        // Prepare bulk data as Map
        Map<Integer, String> djentArtists = new HashMap<>();
        djentArtists.put(6040, "Meshuggah");
        djentArtists.put(6041, "Periphery");
        djentArtists.put(6042, "TesseracT");
        djentArtists.put(6043, "Animals as Leaders");
        djentArtists.put(6044, "Vildhjarta");
        djentArtists.put(6045, "Monuments");
        
        // Bulk put operation
        artistNames.putAll(null, djentArtists);
        System.out.println("   ✓ Bulk inserted " + djentArtists.size() + " djent artists");
        
        System.out.println("\n2. Bulk GET Operations:");
        
        // Prepare keys for bulk retrieval
        Collection<Integer> keysToGet = Arrays.asList(6040, 6041, 6042, 6043);
        
        // Bulk get operation
        Map<Integer, String> retrieved = artistNames.getAll(null, keysToGet);
        
        System.out.println("   ✓ Bulk retrieved " + retrieved.size() + " artists:");
        retrieved.forEach((id, name) -> 
            System.out.println("     " + id + " → " + name));
        
        System.out.println("\n3. Bulk REMOVE Operations:");
        
        // Remove some artists in bulk
        Collection<Integer> keysToRemove = Arrays.asList(6044, 6045);
        
        Collection<Integer> removedKeys = artistNames.removeAll(null, keysToRemove);
        System.out.println("   ✓ Bulk removed " + removedKeys.size() + " artists:");
        removedKeys.forEach(key -> 
            System.out.println("     Removed key: " + key));
        
        // Verify removals
        System.out.println("\n4. Verification after Bulk Operations:");
        Collection<Integer> allKeys = Arrays.asList(6040, 6041, 6042, 6043, 6044, 6045);
        Map<Integer, String> finalState = artistNames.getAll(null, allKeys);
        
        System.out.println("   ✓ Final state after bulk operations:");
        allKeys.forEach(key -> {
            String value = finalState.get(key);
            System.out.println("     " + key + " → " + (value != null ? value : "REMOVED"));
        });
        
        System.out.println("\n✓ Bulk operation advantages:");
        System.out.println("  - Single network round trip for multiple keys");
        System.out.println("  - Reduced connection overhead");
        System.out.println("  - Server-side batch processing optimizations");
        System.out.println("  - Better throughput for large datasets");
    }
    
    /**
     * Demonstrates Tuple-based KeyValueView operations for complex keys and values.
     * 
     * Concepts covered:
     * - Tuple creation and manipulation
     * - Complex composite keys using Tuples
     * - Multi-field values using Tuples
     * - Flexible schema handling
     */
    private static void demonstrateTupleOperations(IgniteClient client) {
        System.out.println("\n--- Tuple-based Operations    ---");
        
        // Use Album table for composite key demonstration
        Table albumTable = client.tables().table("Album");
        KeyValueView<Tuple, Tuple> albumKV = albumTable.keyValueView();
        
        System.out.println("✓ Created Tuple-based KeyValueView for Album table");
        
        System.out.println("\n1. Composite Key Operations with Tuples:");
        
        // Create composite key using Tuple
        Tuple albumKey1 = Tuple.create()
            .set("AlbumId", 6100)
            .set("ArtistId", 6040);  // Meshuggah
        
        // Create value Tuple
        Tuple albumValue1 = Tuple.create()
            .set("Title", "ObZen");
        
        // Put album using Tuples
        albumKV.put(null, albumKey1, albumValue1);
        System.out.println("   ✓ Inserted album using Tuple key/value");
        
        // Create another album
        Tuple albumKey2 = Tuple.create()
            .set("AlbumId", 6101)
            .set("ArtistId", 6040);
        
        Tuple albumValue2 = Tuple.create()
            .set("Title", "Koloss");
        
        albumKV.put(null, albumKey2, albumValue2);
        System.out.println("   ✓ Inserted second album for same artist");
        
        System.out.println("\n2. Tuple Retrieval and Access:");
        
        // Get album using Tuple key
        Tuple retrievedValue = albumKV.get(null, albumKey1);
        if (retrievedValue != null) {
            String title = retrievedValue.stringValue("Title");
            System.out.println("   ✓ Retrieved album title: '" + title + "'");
        }
        
        System.out.println("\n3. Bulk Operations with Tuples:");
        
        // Prepare multiple Tuple keys
        List<Tuple> tupleKeys = Arrays.asList(
            Tuple.create().set("AlbumId", 6100).set("ArtistId", 6040),
            Tuple.create().set("AlbumId", 6101).set("ArtistId", 6040)
        );
        
        // Bulk get with Tuple keys
        Map<Tuple, Tuple> bulkResults = albumKV.getAll(null, tupleKeys);
        
        System.out.println("   ✓ Bulk retrieved " + bulkResults.size() + " albums:");
        bulkResults.forEach((key, value) -> {
            Integer albumId = key.intValue("AlbumId");
            Integer artistId = key.intValue("ArtistId");
            String title = value != null ? value.stringValue("Title") : "NULL";
            System.out.println("     Album(" + albumId + "," + artistId + ") → " + title);
        });
        
        System.out.println("\n4. Tuple Field Access Patterns:");
        
        // Demonstrate various Tuple access methods
        Tuple sampleTuple = Tuple.create()
            .set("StringField", "Sample Text")
            .set("IntField", 42)
            .set("BoolField", true)
            .set("NullField", null);
        
        System.out.println("   ✓ Tuple field access methods:");
        System.out.println("     String: " + sampleTuple.stringValue("StringField"));
        System.out.println("     Integer: " + sampleTuple.intValue("IntField"));
        System.out.println("     Boolean: " + sampleTuple.booleanValue("BoolField"));
        System.out.println("     Generic: " + sampleTuple.value("NullField"));
        
        System.out.println("\n✓ Tuple operation benefits:");
        System.out.println("  - Handle complex composite keys naturally");
        System.out.println("  - Flexible schema without POJO constraints");
        System.out.println("  - Dynamic field access and manipulation");
        System.out.println("  - Support for heterogeneous data types");
    }
    
    /**
     * Demonstrates asynchronous KeyValueView operations for non-blocking execution.
     * 
     * Concepts covered:
     * - Async put/get operations
     * - CompletableFuture chaining
     * - Error handling in async context
     * - Performance benefits of async operations
     */
    private static void demonstrateAsyncKeyValueOperations(IgniteClient client) {
        System.out.println("\n--- Asynchronous KeyValueView Operations    ---");
        
        Table artistTable = client.tables().table("Artist");
        KeyValueView<Integer, String> artistNames = 
            artistTable.keyValueView(Integer.class, String.class);
        
        System.out.println("\n1. Basic Async Operations:");
        
        // Async put operation
        CompletableFuture<Void> putFuture = artistNames.putAsync(null, 6050, "Ne Obliviscaris");
        
        putFuture
            .thenRun(() -> System.out.println("   ✓ Async put completed for Ne Obliviscaris"))
            .exceptionally(throwable -> {
                System.err.println("   ✗ Async put failed: " + throwable.getMessage());
                return null;
            })
            .join();  // Wait for completion in demo
        
        // Async get operation
        CompletableFuture<String> getFuture = artistNames.getAsync(null, 6050);
        
        getFuture
            .thenAccept(name -> {
                if (name != null) {
                    System.out.println("   ✓ Async get result: " + name);
                } else {
                    System.out.println("   ✗ Artist not found");
                }
            })
            .join();  // Wait for completion in demo
        
        System.out.println("\n2. Chained Async Operations:");
        
        // Chain multiple async operations
        artistNames.getAsync(null, 6050)
            .thenCompose(currentName -> {
                if (currentName != null) {
                    String newName = currentName + " (Australian Progressive Black Metal)";
                    return artistNames.getAndPutAsync(null, 6050, newName);
                } else {
                    return CompletableFuture.completedFuture(null);
                }
            })
            .thenAccept(oldName -> {
                System.out.println("   ✓ Chained operation - old name: " + oldName);
            })
            .join();  // Wait for completion in demo
        
        System.out.println("\n3. Parallel Async Operations:");
        
        // Execute multiple async operations in parallel
        Map<Integer, String> postRockArtists = Map.of(
            6060, "Godspeed You! Black Emperor",
            6061, "Explosions in the Sky",
            6062, "This Will Destroy You",
            6063, "Russian Circles"
        );
        
        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] futures = new CompletableFuture[postRockArtists.size()];
        int index = 0;
        
        for (Map.Entry<Integer, String> entry : postRockArtists.entrySet()) {
            futures[index++] = artistNames.putAsync(null, entry.getKey(), entry.getValue());
        }
        
        // Wait for all parallel operations
        CompletableFuture.allOf(futures)
            .thenRun(() -> System.out.println("   ✓ All " + postRockArtists.size() + 
                                           " parallel async puts completed"))
            .join();  // Wait for completion in demo
        
        System.out.println("\n4. Async Bulk Operations:");
        
        // Async bulk get
        Collection<Integer> keysToGet = Arrays.asList(6060, 6061, 6062, 6063);
        
        artistNames.getAllAsync(null, keysToGet)
            .thenAccept(results -> {
                System.out.println("   ✓ Async bulk get completed:");
                results.forEach((key, value) -> 
                    System.out.println("     " + key + " → " + value));
            })
            .join();  // Wait for completion in demo
        
        System.out.println("\n✓ Async KeyValueView benefits:");
        System.out.println("  - Non-blocking execution for high throughput");
        System.out.println("  - Efficient resource utilization");
        System.out.println("  - Parallel execution capabilities");
        System.out.println("  - Composable operation chains");
    }
}