# 4. Table API - Object-Oriented Data Access

## Table Management (`IgniteTables`)

### Listing Tables

The `IgniteTables` interface provides methods to discover and access tables in your Ignite cluster:

```java
// Get all available tables
CompletableFuture<List<Table>> allTables = client.tables().tablesAsync();
allTables.thenAccept(tables -> {
    System.out.println("Available tables:");
    tables.forEach(table -> System.out.println("- " + table.name()));
});

// Synchronous version
List<Table> tables = client.tables().tables();
for (Table table : tables) {
    System.out.println("Table: " + table.name());
}
```

### Getting Table References

```java
// Get a specific table by name
Table artistTable = client.tables().table("Artist");
if (artistTable != null) {
    System.out.println("Found table: " + artistTable.name());
} else {
    System.out.println("Table not found");
}

// Async version
CompletableFuture<Table> tableAsync = client.tables().tableAsync("Artist");
tableAsync.thenAccept(table -> {
    if (table != null) {
        System.out.println("Async found table: " + table.name());
    }
});

// Safe table access with error handling
public static Table getTableSafely(IgniteClient client, String tableName) {
    try {
        return client.tables().table(tableName);
    } catch (Exception e) {
        System.err.println("Error accessing table '" + tableName + "': " + e.getMessage());
        return null;
    }
}
```

### Table Information and Metadata

```java
// Get table metadata
Table table = client.tables().table("Artist");
if (table != null) {
    System.out.println("Table name: " + table.name());
    
    // You can also inspect schema through SQL
    var result = client.sql().execute(null, 
        "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE " +
        "FROM INFORMATION_SCHEMA.COLUMNS " +
        "WHERE TABLE_NAME = ? " +
        "ORDER BY ORDINAL_POSITION", 
        table.name());
    
    System.out.println("Schema for " + table.name() + ":");
    while (result.hasNext()) {
        var row = result.next();
        System.out.printf("  %s: %s (%s)%n", 
            row.stringValue("COLUMN_NAME"),
            row.stringValue("DATA_TYPE"),
            row.stringValue("IS_NULLABLE"));
    }
}
```

## Key-Value Operations (`KeyValueView`)

### Working with Tuples

```java
KeyValueView<Tuple, Tuple> kvView = client.tables().table("accounts").keyValueView();

Tuple key = Tuple.create().set("accountNumber", 123456);
Tuple value = Tuple.create()
    .set("firstName", "Val")
    .set("lastName", "Kulichenko")
    .set("balance", 100.00d);
kvView.put(null, key, value);
```

### Working with POJOs

```java
KeyValueView<AccountKey, Account> kvView = client.tables()
    .table("accounts")
    .keyValueView(AccountKey.class, Account.class);

AccountKey key = new AccountKey(123456);
Account value = new Account("Val", "Kulichenko", 100.00d);
kvView.put(null, key, value);
```

### Put, Get, Remove Operations

#### Basic Put Operations

```java
// Get KeyValueView for Artist table
Table artistTable = client.tables().table("Artist");
KeyValueView<Tuple, Tuple> kvView = artistTable.keyValueView();

// CREATE: Put a new artist using Tuples
Tuple key = Tuple.create().set("ArtistId", 100);
Tuple value = Tuple.create().set("Name", "The Beatles");
kvView.put(null, key, value);
System.out.println("Added artist with Tuple");

// Add more artists for demonstration
Tuple key2 = Tuple.create().set("ArtistId", 101);
Tuple value2 = Tuple.create().set("Name", "Led Zeppelin");
kvView.put(null, key2, value2);

Tuple key3 = Tuple.create().set("ArtistId", 102);
Tuple value3 = Tuple.create().set("Name", "Pink Floyd");
kvView.put(null, key3, value3);
System.out.println("Added multiple artists with Tuples");
```

#### Advanced Put Operations with POJOs

```java
// Define key and value classes for complex tables
public static class AlbumKey {
    @Id
    @Column(value = "AlbumId", nullable = false)
    private Integer AlbumId;
    
    @Id
    @Column(value = "ArtistId", nullable = false)
    private Integer ArtistId;
    
    public AlbumKey() {}
    public AlbumKey(Integer albumId, Integer artistId) {
        this.AlbumId = albumId;
        this.ArtistId = artistId;
    }
    
    // Getters and setters
    public Integer getAlbumId() { return AlbumId; }
    public void setAlbumId(Integer albumId) { this.AlbumId = albumId; }
    public Integer getArtistId() { return ArtistId; }
    public void setArtistId(Integer artistId) { this.ArtistId = artistId; }
}

public static class AlbumValue {
    @Column(value = "Title", nullable = false, length = 160)
    private String Title;
    
    public AlbumValue() {}
    public AlbumValue(String title) {
        this.Title = title;
    }
    
    // Getters and setters
    public String getTitle() { return Title; }
    public void setTitle(String title) { this.Title = title; }
}

// Use strongly-typed KeyValueView
Table albumTable = client.tables().table("Album");
KeyValueView<AlbumKey, AlbumValue> albumKvView = 
    albumTable.keyValueView(AlbumKey.class, AlbumValue.class);

// Put album with proper key and value
AlbumKey albumKey = new AlbumKey(200, 100);  // Album ID 200, Artist ID 100 (The Beatles)
AlbumValue albumValue = new AlbumValue("Abbey Road");
albumKvView.put(null, albumKey, albumValue);
System.out.println("Added album: " + albumValue.getTitle());

// Add more albums for The Beatles
albumKvView.put(null, new AlbumKey(201, 100), new AlbumValue("Sgt. Pepper's Lonely Hearts Club Band"));
albumKvView.put(null, new AlbumKey(202, 100), new AlbumValue("Revolver"));
```

#### Get Operations

```java
// GET: Retrieve by key
Tuple artistKey = Tuple.create().set("ArtistId", 100);
Tuple artistValue = kvView.get(null, artistKey);
if (artistValue != null) {
    System.out.println("Found artist: " + artistValue.stringValue("Name"));
} else {
    System.out.println("Artist not found");
}

// Get with strongly-typed classes
AlbumKey searchKey = new AlbumKey(200, 100);
AlbumValue foundAlbum = albumKvView.get(null, searchKey);
if (foundAlbum != null) {
    System.out.println("Found album: " + foundAlbum.getTitle());
} else {
    System.out.println("Album not found");
}
```

#### Bulk Get Operations

```java
// Get multiple records at once
Collection<Tuple> keys = Arrays.asList(
    Tuple.create().set("ArtistId", 100),
    Tuple.create().set("ArtistId", 101),
    Tuple.create().set("ArtistId", 102)
);

Map<Tuple, Tuple> results = kvView.getAll(null, keys);
results.forEach((key, value) -> {
    if (value != null) {
        System.out.println("Artist " + key.intValue("ArtistId") + 
                         ": " + value.stringValue("Name"));
    }
});
```

#### Remove Operations

```java
// REMOVE: Delete by key
Tuple keyToRemove = Tuple.create().set("ArtistId", 100);
boolean removed = kvView.remove(null, keyToRemove);
if (removed) {
    System.out.println("Artist removed successfully");
} else {
    System.out.println("Artist not found for removal");
}

// Remove with value check (conditional remove)
Tuple expectedValue = Tuple.create().set("Name", "The Beatles");
boolean conditionallyRemoved = kvView.remove(null, keyToRemove, expectedValue);
System.out.println("Conditionally removed: " + conditionallyRemoved);

// Bulk remove
Collection<Tuple> keysToRemove = Arrays.asList(
    Tuple.create().set("ArtistId", 101),
    Tuple.create().set("ArtistId", 102)
);

Collection<Tuple> removedKeys = kvView.removeAll(null, keysToRemove);
System.out.println("Removed " + removedKeys.size() + " artists");
```

#### Conditional Operations

```java
// PUT_IF_ABSENT: Only insert if key doesn't exist
Tuple newKey = Tuple.create().set("ArtistId", 103);
Tuple newValue = Tuple.create().set("Name", "Queen");
boolean inserted = kvView.putIfAbsent(null, newKey, newValue);
if (inserted) {
    System.out.println("New artist added");
} else {
    System.out.println("Artist already exists");
}

// REPLACE: Only update if key exists
Tuple updateValue = Tuple.create().set("Name", "Queen (Updated)");
boolean replaced = kvView.replace(null, newKey, updateValue);
System.out.println("Artist updated: " + replaced);

// REPLACE with value check
Tuple currentValue = Tuple.create().set("Name", "Queen (Updated)");
Tuple newValueForReplace = Tuple.create().set("Name", "Queen (Final)");
boolean conditionallyReplaced = kvView.replace(null, newKey, currentValue, newValueForReplace);
System.out.println("Conditionally replaced: " + conditionallyReplaced);
```

## Record Operations (`RecordView`)

### Insert, Update, Upsert, Delete

```java
RecordView<Tuple> accounts = client.tables().table("accounts").recordView();

Tuple newAccountTuple = Tuple.create()
    .set("accountNumber", 123456)
    .set("firstName", "Val")
    .set("lastName", "Kulichenko")
    .set("balance", 100.00d);
accounts.insert(null, newAccountTuple);
```

### Bulk Operations

#### Bulk Insert Operations

```java
// Using RecordView for bulk inserts with POJOs
@Table(zone = @Zone(value = "Chinook", storageProfiles = "default"))
public static class Artist {
    @Id
    @Column(value = "ArtistId", nullable = false)
    private Integer ArtistId;
    
    @Column(value = "Name", nullable = true, length = 120)
    private String Name;
    
    public Artist() {}
    public Artist(Integer artistId, String name) {
        this.ArtistId = artistId;
        this.Name = name;
    }
    
    // Getters and setters
    public Integer getArtistId() { return ArtistId; }
    public void setArtistId(Integer artistId) { this.ArtistId = artistId; }
    public String getName() { return Name; }
    public void setName(String name) { this.Name = name; }
    
    @Override
    public String toString() {
        return "Artist{ArtistId=" + ArtistId + ", Name='" + Name + "'}";
    }
}

// Bulk insert with RecordView
Table artistTable = client.tables().table("Artist");
RecordView<Artist> artistView = artistTable.recordView(Artist.class);

// Create multiple artists from Chinook dataset
List<Artist> artists = Arrays.asList(
    new Artist(1, "AC/DC"),
    new Artist(2, "Accept"),
    new Artist(3, "Aerosmith"),
    new Artist(4, "Alanis Morissette"),
    new Artist(5, "Alice In Chains"),
    new Artist(6, "Antônio Carlos Jobim"),
    new Artist(7, "Apocalyptica"),
    new Artist(8, "Audioslave"),
    new Artist(9, "BackBeat"),
    new Artist(10, "The Beatles")
);

// Bulk insert using upsertAll
artistView.upsertAll(null, artists);
System.out.println("Upserted " + artists.size() + " artists from Chinook dataset");
```

#### Bulk Update Operations

```java
// Bulk update existing records
List<Artist> updatedArtists = Arrays.asList(
    new Artist(1, "AC/DC (Remastered)"),
    new Artist(2, "Accept (Special Edition)"),
    new Artist(3, "Aerosmith (Greatest Hits)")
);

// Upsert will update existing records
artistView.upsertAll(null, updatedArtists);
System.out.println("Updated " + updatedArtists.size() + " artist names");
```

#### Bulk Delete Operations

```java
// Delete multiple records by key
List<Artist> keysToDelete = Arrays.asList(
    new Artist(4, null),  // Only ID needed for deletion
    new Artist(5, null)
);

Collection<Artist> deletedKeys = artistView.deleteAll(null, keysToDelete);
System.out.println("Deleted " + deletedKeys.size() + " artists");

// Alternative: delete by providing key objects
List<Artist> artistsToDelete = new ArrayList<>();
artistsToDelete.add(new Artist());
artistsToDelete.get(0).setArtistId(1);
artistsToDelete.add(new Artist());
artistsToDelete.get(1).setArtistId(2);

Collection<Artist> deletedArtists = artistView.deleteAll(null, artistsToDelete);
System.out.println("Deleted artists: " + deletedArtists.size());
```

#### Performance Considerations for Bulk Operations

```java
// Efficient bulk loading pattern
public static void bulkLoadArtists(IgniteClient client, List<Artist> artists) {
    Table table = client.tables().table("Artist");
    RecordView<Artist> view = table.recordView(Artist.class);
    
    // Process in batches for better performance
    int batchSize = 1000;
    for (int i = 0; i < artists.size(); i += batchSize) {
        int endIndex = Math.min(i + batchSize, artists.size());
        List<Artist> batch = artists.subList(i, endIndex);
        
        view.upsertAll(null, batch);
        System.out.println("Processed batch: " + (i / batchSize + 1) + 
                         ", records: " + batch.size());
    }
}

// Usage
List<Artist> manyArtists = new ArrayList<>();
for (int i = 1; i <= 10000; i++) {
    manyArtists.add(new Artist(i, "Artist " + i));
}

bulkLoadArtists(client, manyArtists);
```

#### Bulk Operations with KeyValueView

```java
// Bulk operations using KeyValueView with Tuples
KeyValueView<Tuple, Tuple> kvView = client.tables().table("Artist").keyValueView();

// Prepare bulk data
Map<Tuple, Tuple> bulkData = new HashMap<>();
for (int i = 100; i < 110; i++) {
    Tuple key = Tuple.create().set("ArtistId", i);
    Tuple value = Tuple.create().set("Name", "Bulk Artist " + i);
    bulkData.put(key, value);
}

// Bulk put
kvView.putAll(null, bulkData);
System.out.println("Bulk inserted " + bulkData.size() + " artists with KeyValueView");

// Bulk get
Collection<Tuple> keys = bulkData.keySet();
Map<Tuple, Tuple> retrieved = kvView.getAll(null, keys);
System.out.println("Retrieved " + retrieved.size() + " artists");

// Bulk remove
Collection<Tuple> removedKeys = kvView.removeAll(null, keys);
System.out.println("Removed " + removedKeys.size() + " artists");
```

## Async Operations

### CompletableFuture Patterns

#### Basic Async Operations

```java
// Async put operation
Table artistTable = client.tables().table("Artist");
RecordView<Artist> artistView = artistTable.recordView(Artist.class);

Artist newArtist = new Artist(200, "Dream Theater");

// Non-blocking insert
CompletableFuture<Void> insertFuture = artistView.upsertAsync(null, newArtist);
insertFuture.thenRun(() -> {
    System.out.println("Artist inserted asynchronously");
}).exceptionally(throwable -> {
    System.err.println("Insert failed: " + throwable.getMessage());
    return null;
});

// Async get operation
Artist keyArtist = new Artist();
keyArtist.setArtistId(200);

CompletableFuture<Artist> getFuture = artistView.getAsync(null, keyArtist);
getFuture.thenAccept(artist -> {
    if (artist != null) {
        System.out.println("Found artist: " + artist.getName());
    } else {
        System.out.println("Artist not found");
    }
});
```

#### Chaining Async Operations

```java
// Chain multiple async operations
CompletableFuture<Void> chainedOperations = artistView
    .getAsync(null, keyArtist)
    .thenCompose(artist -> {
        if (artist != null) {
            // Update the artist
            artist.setName(artist.getName() + " (Updated)");
            return artistView.upsertAsync(null, artist);
        } else {
            // Insert new artist if not found
            Artist newArtist = new Artist(200, "Tool");
            return artistView.upsertAsync(null, newArtist);
        }
    })
    .thenRun(() -> {
        System.out.println("Chained operations completed");
    })
    .exceptionally(throwable -> {
        System.err.println("Chained operation failed: " + throwable.getMessage());
        return null;
    });

// Wait for completion if needed
chainedOperations.join();
```

#### Parallel Async Operations

```java
// Execute multiple operations in parallel
List<CompletableFuture<Void>> futures = new ArrayList<>();

// Insert more Chinook artists in parallel
String[] artistNames = {"Billy Cobham", "Buddy Guy", "Chico Buarque", "Cidade Negra", 
                       "Cláudio Zoli", "Various Artists", "Led Zeppelin", "Frank Zappa", 
                       "Foo Fighters", "Metallica"};

for (int i = 0; i < artistNames.length; i++) {
    Artist artist = new Artist(300 + i, artistNames[i]);
    CompletableFuture<Void> future = artistView.upsertAsync(null, artist);
    futures.add(future);
}

// Wait for all operations to complete
CompletableFuture<Void> allFutures = CompletableFuture.allOf(
    futures.toArray(new CompletableFuture[0])
);

allFutures.thenRun(() -> {
    System.out.println("All parallel operations completed");
}).join();
```

#### Async Bulk Operations

```java
// Async bulk operations with more Chinook artists
List<Artist> bulkArtists = Arrays.asList(
    new Artist(400, "Iron Maiden"),
    new Artist(401, "Pearl Jam"),
    new Artist(402, "Red Hot Chili Peppers")
);

CompletableFuture<Void> bulkInsert = artistView.upsertAllAsync(null, bulkArtists);
bulkInsert.thenRun(() -> {
    System.out.println("Bulk insert completed asynchronously");
    
    // Chain with bulk get
    return artistView.getAllAsync(null, bulkArtists);
}).thenCompose(ignored -> artistView.getAllAsync(null, bulkArtists))
  .thenAccept(results -> {
    System.out.println("Retrieved " + results.size() + " artists");
    results.forEach(artist -> System.out.println("  - " + artist));
});
```

#### Advanced Async Patterns

```java
// Async with timeout
CompletableFuture<Artist> getWithTimeout = artistView.getAsync(null, keyArtist)
    .orTimeout(5, TimeUnit.SECONDS)
    .exceptionally(throwable -> {
        if (throwable instanceof TimeoutException) {
            System.err.println("Operation timed out");
        }
        return null;
    });

// Async with retry logic
public static CompletableFuture<Void> insertWithRetry(
        RecordView<Artist> view, Artist artist, int maxRetries) {
    
    return view.upsertAsync(null, artist)
        .exceptionallyCompose(throwable -> {
            if (maxRetries > 0) {
                System.out.println("Retrying insert, attempts left: " + maxRetries);
                return insertWithRetry(view, artist, maxRetries - 1);
            } else {
                return CompletableFuture.failedFuture(throwable);
            }
        });
}

// Usage
Artist retryArtist = new Artist(500, "System Of A Down");
insertWithRetry(artistView, retryArtist, 3)
    .thenRun(() -> System.out.println("Insert succeeded with retry"))
    .exceptionally(throwable -> {
        System.err.println("Insert failed after retries: " + throwable.getMessage());
        return null;
    });
```

#### Combining Sync and Async Operations

```java
// Sometimes you need to mix sync and async operations
public static void hybridOperations(IgniteClient client) {
    Table table = client.tables().table("Artist");
    RecordView<Artist> view = table.recordView(Artist.class);
    
    // Sync operation to check if artist exists
    Artist checkArtist = new Artist();
    checkArtist.setArtistId(600);
    Artist existing = view.get(null, checkArtist);
    
    if (existing == null) {
        // Async insert if doesn't exist
        Artist newArtist = new Artist(600, "Rage Against The Machine");
        view.upsertAsync(null, newArtist)
            .thenRun(() -> System.out.println("Hybrid: Artist created"))
            .join(); // Wait for completion
    } else {
        // Async update if exists
        existing.setName(existing.getName() + " (Updated)");
        view.upsertAsync(null, existing)
            .thenRun(() -> System.out.println("Hybrid: Artist updated"))
            .join();
    }
}
```

### Error Handling in Async Code

#### Basic Error Handling

```java
// Handle exceptions in async operations
Artist testArtist = new Artist(700, "Nirvana");

artistView.upsertAsync(null, testArtist)
    .thenRun(() -> {
        System.out.println("Success: Artist inserted");
    })
    .exceptionally(throwable -> {
        System.err.println("Error inserting artist: " + throwable.getMessage());
        throwable.printStackTrace();
        return null;
    });
```

#### Specific Exception Handling

```java
// Handle different types of exceptions
artistView.getAsync(null, keyArtist)
    .thenAccept(artist -> {
        if (artist != null) {
            System.out.println("Found: " + artist);
        } else {
            System.out.println("Artist not found");
        }
    })
    .exceptionally(throwable -> {
        if (throwable instanceof IgniteClientConnectionException) {
            System.err.println("Connection error: " + throwable.getMessage());
        } else if (throwable instanceof TimeoutException) {
            System.err.println("Operation timed out");
        } else {
            System.err.println("Unexpected error: " + throwable.getMessage());
        }
        return null;
    });
```

#### Error Recovery Patterns

```java
// Async operation with fallback
public static CompletableFuture<Artist> getArtistWithFallback(
        RecordView<Artist> view, Artist key, Artist fallbackArtist) {
    
    return view.getAsync(null, key)
        .thenApply(artist -> artist != null ? artist : fallbackArtist)
        .exceptionally(throwable -> {
            System.err.println("Get failed, using fallback: " + throwable.getMessage());
            return fallbackArtist;
        });
}

// Circuit breaker pattern for async operations
public static class AsyncCircuitBreaker {
    private volatile boolean isOpen = false;
    private volatile long lastFailureTime = 0;
    private final long timeout = 60000; // 1 minute
    
    public <T> CompletableFuture<T> execute(Supplier<CompletableFuture<T>> operation) {
        if (isOpen && (System.currentTimeMillis() - lastFailureTime) < timeout) {
            return CompletableFuture.failedFuture(
                new RuntimeException("Circuit breaker is open"));
        }
        
        return operation.get()
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    isOpen = true;
                    lastFailureTime = System.currentTimeMillis();
                } else {
                    isOpen = false;
                }
            });
    }
}

// Usage
AsyncCircuitBreaker circuitBreaker = new AsyncCircuitBreaker();
Artist searchKey = new Artist();
searchKey.setArtistId(800);

circuitBreaker.execute(() -> artistView.getAsync(null, searchKey))
    .thenAccept(artist -> {
        System.out.println("Circuit breaker success: " + artist);
    })
    .exceptionally(throwable -> {
        System.err.println("Circuit breaker failed: " + throwable.getMessage());
        return null;
    });
```

#### Comprehensive Error Handling Utility

```java
public static class AsyncTableOperations {
    
    public static <T> CompletableFuture<T> withErrorHandling(
            CompletableFuture<T> operation, String operationName) {
        
        return operation.exceptionally(throwable -> {
            System.err.println("Error in " + operationName + ": " + throwable.getMessage());
            
            if (throwable instanceof IgniteClientConnectionException) {
                System.err.println("Connection issue - check cluster status");
            } else if (throwable instanceof SqlException) {
                System.err.println("SQL error - check query syntax and data");
            } else if (throwable instanceof TimeoutException) {
                System.err.println("Timeout - operation took too long");
            }
            
            return null;
        });
    }
    
    public static <T> CompletableFuture<T> withRetry(
            Supplier<CompletableFuture<T>> operation, 
            int maxRetries, 
            String operationName) {
        
        return operation.get()
            .exceptionallyCompose(throwable -> {
                if (maxRetries > 0) {
                    System.out.println("Retrying " + operationName + 
                                     ", attempts left: " + maxRetries);
                    return withRetry(operation, maxRetries - 1, operationName);
                } else {
                    System.err.println("All retry attempts failed for " + operationName);
                    return CompletableFuture.failedFuture(throwable);
                }
            });
    }
}

// Usage examples
Artist testArtist = new Artist(900, "Soundgarden");

// With error handling
AsyncTableOperations.withErrorHandling(
    artistView.upsertAsync(null, testArtist),
    "insert artist"
).thenRun(() -> {
    System.out.println("Artist inserted successfully");
});

// With retry and error handling
AsyncTableOperations.withRetry(
    () -> artistView.getAsync(null, testArtist),
    3,
    "get artist"
).thenAccept(artist -> {
    if (artist != null) {
        System.out.println("Artist retrieved: " + artist);
    }
});
```

#### Best Practices for Async Error Handling

1. **Always Handle Exceptions**: Use `.exceptionally()` or `.handle()` to catch errors
2. **Specific Error Types**: Handle different exception types appropriately
3. **Fallback Strategies**: Provide default values or alternative operations
4. **Logging**: Log errors with sufficient context for debugging
5. **Circuit Breakers**: Implement circuit breakers for resilience
6. **Timeouts**: Set appropriate timeouts to prevent hanging operations
7. **Retry Logic**: Implement exponential backoff for transient errors

```java
// Complete example with all best practices
public static CompletableFuture<Void> robustAsyncOperation(
        RecordView<Artist> view, Artist artist) {
    
    return view.upsertAsync(null, artist)
        .orTimeout(10, TimeUnit.SECONDS)
        .exceptionallyCompose(throwable -> {
            if (throwable instanceof TimeoutException) {
                System.err.println("Operation timed out, retrying...");
                return view.upsertAsync(null, artist)
                    .orTimeout(20, TimeUnit.SECONDS);
            } else {
                return CompletableFuture.failedFuture(throwable);
            }
        })
        .exceptionally(throwable -> {
            System.err.println("Final error: " + throwable.getMessage());
            // Could send to error tracking service
            return null;
        });
}
```
