# 6. Transactions

## Transaction Basics (`IgniteTransactions`)

### ACID Properties in Ignite

Apache Ignite 3 provides full ACID compliance for transactions:

- **Atomicity**: All operations in a transaction succeed or fail together
- **Consistency**: Transactions maintain data integrity and constraints
- **Isolation**: Concurrent transactions don't interfere with each other
- **Durability**: Committed changes are permanently stored

```java
// Example demonstrating ACID properties with Chinook music data
client.transactions().runInTransaction(tx -> {
    Table artistTable = client.tables().table("Artist");
    Table albumTable = client.tables().table("Album");
    
    RecordView<Artist> artistView = artistTable.recordView(Artist.class);
    RecordView<Album> albumView = albumTable.recordView(Album.class);
    
    try {
        // Atomicity: Both operations succeed or both fail
        Artist artist = new Artist(100, "Radiohead");
        artistView.upsert(tx, artist);
        
        Album album = new Album(200, "OK Computer", 100);
        albumView.upsert(tx, album);
        
        // Consistency: Foreign key relationship maintained (Album.ArtistId -> Artist.ArtistId)
        // Isolation: Other transactions don't see partial state
        // Durability: Changes are persisted after commit
        
        return true; // Commit
    } catch (Exception e) {
        System.err.println("Transaction failed: " + e.getMessage());
        return false; // Rollback
    }
});
```

### Transaction Isolation Levels

Ignite 3 supports multiple isolation levels:

```java
// Default isolation level (READ_COMMITTED)
Transaction tx1 = client.transactions().begin();

// Specify isolation level explicitly
TransactionOptions options = new TransactionOptions()
    .readOnly(false)
    .timeout(Duration.ofSeconds(30))
    .label("data-processing-tx");

Transaction tx2 = client.transactions().begin(options);

// Read-only transactions for better performance
TransactionOptions readOnlyOptions = new TransactionOptions()
    .readOnly(true)
    .timeout(Duration.ofSeconds(10));

Transaction readOnlyTx = client.transactions().begin(readOnlyOptions);
```

#### Isolation Level Behaviors

| Isolation Level | Dirty Read | Non-Repeatable Read | Phantom Read |
|----------------|------------|--------------------|--------------|
| READ_COMMITTED | No | Yes | Yes |
| REPEATABLE_READ | No | No | Yes |
| SERIALIZABLE | No | No | No |

```java
// Example showing isolation level effects with Chinook data
public static void demonstrateIsolation(IgniteClient client) {
    // Transaction 1: Updates AC/DC's name
    CompletableFuture<Void> tx1 = CompletableFuture.runAsync(() -> {
        client.transactions().runInTransaction(tx -> {
            RecordView<Artist> view = client.tables().table("Artist").recordView(Artist.class);
            
            Artist artist = view.get(tx, createArtistKey(1));  // AC/DC
            artist.setName("AC/DC (Remastered Collection)");
            view.upsert(tx, artist);
            
            // Simulate long-running transaction (e.g., updating related albums)
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            
            return true;
        });
    });
    
    // Transaction 2: Reads artist concurrently
    CompletableFuture<Void> tx2 = CompletableFuture.runAsync(() -> {
        try { Thread.sleep(1000); } catch (InterruptedException e) {} // Start after tx1
        
        client.transactions().runInTransaction(tx -> {
            RecordView<Artist> view = client.tables().table("Artist").recordView(Artist.class);
            
            Artist artist = view.get(tx, createArtistKey(1));
            System.out.println("TX2 sees: " + artist.getName()); // May see old or new value depending on isolation
            
            return true;
        });
    });
    
    // Wait for both transactions
    CompletableFuture.allOf(tx1, tx2).join();
}

private static Artist createArtistKey(Integer id) {
    Artist key = new Artist();
    key.setArtistId(id);
    return key;
}
```

## Synchronous Transactions

### Explicit Transaction Management

```java
// Explicit transaction for updating artist information
Transaction tx = client.transactions().begin(new TransactionOptions().readOnly(false));
try {
    // Update artist name using SQL
    client.sql().execute(tx, 
        "UPDATE Artist SET Name = ? WHERE ArtistId = ?", 
        "Pink Floyd (50th Anniversary)", 3);
    
    // Update related album information
    client.sql().execute(tx,
        "UPDATE Album SET Title = Title || ' (Remastered)' WHERE ArtistId = ?", 
        3);
    
    tx.commit();
    System.out.println("Artist and albums updated successfully");
} catch (Exception e) {
    tx.rollback();
    System.err.println("Transaction failed: " + e.getMessage());
    throw e;
}
```

### `runInTransaction()` Pattern

```java
// Functional transaction pattern for updating track prices
client.transactions().runInTransaction(tx -> {
    RecordView<Track> trackView = client.tables().table("Track").recordView(Track.class);
    
    // Get track by ID
    Track track = trackView.get(tx, createTrackKey(1));
    if (track != null) {
        // Increase price by 10%
        BigDecimal newPrice = track.getUnitPrice().multiply(new BigDecimal("1.10"));
        track.setUnitPrice(newPrice);
        trackView.upsert(tx, track);
        
        System.out.println("Updated track price to: $" + newPrice);
    }
    
    return true;
});

private static Track createTrackKey(Integer id) {
    Track key = new Track();
    key.setTrackId(id);
    return key;
}
```

## Asynchronous Transactions

### Async Transaction Handling

```java
// Async transaction for updating artist popularity score
CompletableFuture<Void> fut = client.transactions().beginAsync().thenCompose(tx -> {
    RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
    Artist key = createArtistKey(10); // The Beatles
    
    return artistView.getAsync(tx, key)
        .thenCompose(artist -> {
            if (artist != null) {
                // Update artist name to include popularity indicator
                artist.setName(artist.getName() + " ⭐");
                return artistView.upsertAsync(tx, artist);
            } else {
                throw new RuntimeException("Artist not found");
            }
        })
        .thenCompose(ignored -> tx.commitAsync());
});

fut.thenRun(() -> System.out.println("Artist updated asynchronously"))
   .exceptionally(throwable -> {
       System.err.println("Async transaction failed: " + throwable.getMessage());
       return null;
   })
   .join();
```

### Combining with Async Operations

#### Async Transaction Patterns

```java
// Chain multiple async operations within a transaction
CompletableFuture<Void> complexAsyncTransaction = client.transactions().beginAsync()
    .thenCompose(tx -> {
        Table artistTable = client.tables().table("Artist");
        Table albumTable = client.tables().table("Album");
        
        RecordView<Artist> artistView = artistTable.recordView(Artist.class);
        RecordView<Album> albumView = albumTable.recordView(Album.class);
        
        // Create artist first
        Artist artist = new Artist(300, "Muse");
        
        return artistView.upsertAsync(tx, artist)
            .thenCompose(ignored -> {
                // Create album after artist
                Album album = new Album(300, "Origin of Symmetry", 300);
                return albumView.upsertAsync(tx, album);
            })
            .thenCompose(ignored -> {
                // Verify the data
                return artistView.getAsync(tx, createArtistKey(300));
            })
            .thenCompose(retrievedArtist -> {
                if (retrievedArtist != null) {
                    System.out.println("Verified artist: " + retrievedArtist.getName());
                    return tx.commitAsync();
                } else {
                    return tx.rollbackAsync().thenApply(ignored -> {
                        throw new RuntimeException("Artist verification failed");
                    });
                }
            })
            .exceptionally(throwable -> {
                System.err.println("Transaction failed: " + throwable.getMessage());
                return tx.rollbackAsync().join();
            });
    });

// Wait for completion
complexAsyncTransaction.join();
```

#### Parallel Operations in Transactions

```java
// Execute multiple independent operations in parallel within a transaction
CompletableFuture<Void> parallelTransaction = client.transactions().beginAsync()
    .thenCompose(tx -> {
        RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
        
        // Create multiple artists in parallel
        List<CompletableFuture<Void>> operations = new ArrayList<>();
        
        // Insert famous rock bands in parallel
        String[] bandNames = {"Coldplay", "Linkin Park", "Green Day", "Foo Fighters", 
                              "Red Hot Chili Peppers", "Nirvana", "Pearl Jam", 
                              "Soundgarden", "Alice in Chains", "Stone Temple Pilots"};
        
        for (int i = 0; i < bandNames.length; i++) {
            Artist artist = new Artist(400 + i, bandNames[i]);
            operations.add(artistView.upsertAsync(tx, artist));
        }
        
        // Wait for all parallel operations to complete
        return CompletableFuture.allOf(operations.toArray(new CompletableFuture[0]))
            .thenCompose(ignored -> tx.commitAsync())
            .exceptionally(throwable -> {
                System.err.println("Parallel transaction failed: " + throwable.getMessage());
                return tx.rollbackAsync().join();
            });
    });

parallelTransaction.join();
```

#### Async Transaction with Retry Logic

```java
public static CompletableFuture<Void> asyncTransactionWithRetry(
        IgniteClient client, int maxRetries) {
    
    return executeAsyncTransactionWithRetry(client, maxRetries, 0);
}

private static CompletableFuture<Void> executeAsyncTransactionWithRetry(
        IgniteClient client, int maxRetries, int attempt) {
    
    return client.transactions().beginAsync()
        .thenCompose(tx -> {
            RecordView<Artist> view = client.tables().table("Artist").recordView(Artist.class);
            
            // Simulate business logic that might fail
            Artist artist = new Artist(500 + attempt, "System Of A Down " + attempt);
            
            return view.upsertAsync(tx, artist)
                .thenCompose(ignored -> {
                    // Simulate random failure
                    if (Math.random() < 0.3) {
                        throw new RuntimeException("Simulated failure");
                    }
                    return tx.commitAsync();
                })
                .exceptionally(throwable -> {
                    tx.rollbackAsync();
                    throw new RuntimeException(throwable);
                });
        })
        .exceptionallyCompose(throwable -> {
            if (attempt < maxRetries) {
                System.out.println("Retrying transaction, attempt: " + (attempt + 1));
                // Exponential backoff
                return CompletableFuture.delayedExecutor(
                    Duration.ofMillis(100 * (1L << attempt))
                ).execute(() -> {});
                
                return executeAsyncTransactionWithRetry(client, maxRetries, attempt + 1);
            } else {
                System.err.println("Transaction failed after " + maxRetries + " retries");
                return CompletableFuture.failedFuture(throwable);
            }
        });
}

// Usage
asyncTransactionWithRetry(client, 3)
    .thenRun(() -> System.out.println("Async transaction with retry succeeded"))
    .exceptionally(throwable -> {
        System.err.println("Final failure: " + throwable.getMessage());
        return null;
    });
```

#### Timeout Handling in Async Transactions

```java
// Async transaction with timeout
CompletableFuture<Void> timedTransaction = client.transactions().beginAsync(
        new TransactionOptions().timeout(Duration.ofSeconds(5)))
    .thenCompose(tx -> {
        RecordView<Artist> view = client.tables().table("Artist").recordView(Artist.class);
        
        Artist artist = new Artist(600, "Tool");
        
        return view.upsertAsync(tx, artist)
            .orTimeout(3, TimeUnit.SECONDS)
            .thenCompose(ignored -> tx.commitAsync())
            .exceptionally(throwable -> {
                if (throwable instanceof TimeoutException) {
                    System.err.println("Operation timed out, rolling back");
                } else {
                    System.err.println("Transaction error: " + throwable.getMessage());
                }
                
                return tx.rollbackAsync().join();
            });
    });

timedTransaction.join();
```

## Transaction Options

### Read-Only Transactions

Read-only transactions provide better performance for queries that don't modify data:

```java
// Read-only transaction for reporting
TransactionOptions readOnlyOptions = new TransactionOptions()
    .readOnly(true)
    .timeout(Duration.ofMinutes(5))  // Longer timeout for reports
    .label("monthly-report");

client.transactions().runInTransaction(readOnlyOptions, tx -> {
    // Multiple read operations
    var artistCount = client.sql().execute(tx, "SELECT COUNT(*) as count FROM Artist")
        .next().longValue("count");
    
    var albumCount = client.sql().execute(tx, "SELECT COUNT(*) as count FROM Album")
        .next().longValue("count");
    
    var trackCount = client.sql().execute(tx, "SELECT COUNT(*) as count FROM Track")
        .next().longValue("count");
    
    System.out.printf("Report: %d artists, %d albums, %d tracks%n", 
        artistCount, albumCount, trackCount);
    
    return true;
});
```

### Timeout Configuration

```java
// Different timeout strategies
public class TransactionTimeouts {
    
    // Short timeout for simple operations
    public static void quickOperation(IgniteClient client) {
        TransactionOptions quickOptions = new TransactionOptions()
            .timeout(Duration.ofSeconds(5))
            .label("quick-update");
        
        client.transactions().runInTransaction(quickOptions, tx -> {
            RecordView<Artist> view = client.tables().table("Artist").recordView(Artist.class);
            
            Artist artist = view.get(tx, createArtistKey(1));
            artist.setName("Quick Update");
            view.upsert(tx, artist);
            
            return true;
        });
    }
    
    // Long timeout for complex operations
    public static void complexOperation(IgniteClient client) {
        TransactionOptions complexOptions = new TransactionOptions()
            .timeout(Duration.ofMinutes(10))
            .label("complex-processing");
        
        client.transactions().runInTransaction(complexOptions, tx -> {
            // Complex business logic that might take time
            performComplexCalculation(client, tx);
            return true;
        });
    }
    
    // Adaptive timeout based on data size
    public static void adaptiveTimeout(IgniteClient client, int recordCount) {
        Duration timeout = recordCount < 1000 ? 
            Duration.ofSeconds(30) : 
            Duration.ofMinutes(5);
        
        TransactionOptions adaptiveOptions = new TransactionOptions()
            .timeout(timeout)
            .label("adaptive-batch-" + recordCount);
        
        client.transactions().runInTransaction(adaptiveOptions, tx -> {
            // Process records with appropriate timeout
            processBatchOfRecords(client, tx, recordCount);
            return true;
        });
    }
    
    private static void performComplexCalculation(IgniteClient client, Transaction tx) {
        // Simulate complex operation
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
    }
    
    private static void processBatchOfRecords(IgniteClient client, Transaction tx, int count) {
        // Simulate batch processing
    }
}
```

### Error Handling and Rollback

#### Explicit Error Handling

```java
public static void explicitErrorHandling(IgniteClient client) {
    Transaction tx = null;
    try {
        tx = client.transactions().begin(new TransactionOptions()
            .readOnly(false)
            .timeout(Duration.ofSeconds(30)));
        
        RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
        RecordView<Album> albumView = client.tables().table("Album").recordView(Album.class);
        
        // Business operation 1
        Artist artist = new Artist(700, "Nickelback");
        artistView.upsert(tx, artist);
        
        // Simulate business validation
        if (artist.getName().contains("Nickelback")) {
            throw new BusinessValidationException("Artist not allowed in our catalog");
        }
        
        // Business operation 2
        Album album = new Album(700, "Silver Side Up", 700);
        albumView.upsert(tx, album);
        
        // Commit if all operations succeed
        tx.commit();
        System.out.println("Transaction committed successfully");
        
    } catch (BusinessValidationException e) {
        System.err.println("Business validation failed: " + e.getMessage());
        if (tx != null) tx.rollback();
    } catch (IgniteClientConnectionException e) {
        System.err.println("Connection error: " + e.getMessage());
        if (tx != null) tx.rollback();
    } catch (Exception e) {
        System.err.println("Unexpected error: " + e.getMessage());
        if (tx != null) tx.rollback();
    } finally {
        // Transaction is automatically closed if using try-with-resources
        // or explicitly close here if needed
    }
}

// Custom business exception
static class BusinessValidationException extends Exception {
    public BusinessValidationException(String message) {
        super(message);
    }
}
```

#### Functional Error Handling with runInTransaction

```java
public static boolean functionalErrorHandling(IgniteClient client) {
    return client.transactions().runInTransaction(tx -> {
        try {
            RecordView<Artist> view = client.tables().table("Artist").recordView(Artist.class);
            
            // Multiple operations that might fail
            performDatabaseOperations(client, tx);
            validateBusinessRules(client, tx);
            updateRelatedEntities(client, tx);
            
            System.out.println("All operations completed successfully");
            return true; // Commit
            
        } catch (BusinessValidationException e) {
            System.err.println("Business rule violation: " + e.getMessage());
            return false; // Rollback
        } catch (DataIntegrityException e) {
            System.err.println("Data integrity issue: " + e.getMessage());
            return false; // Rollback
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return false; // Rollback
        }
    });
}

private static void performDatabaseOperations(IgniteClient client, Transaction tx) 
        throws BusinessValidationException {
    // Database operations that might throw exceptions
}

private static void validateBusinessRules(IgniteClient client, Transaction tx) 
        throws BusinessValidationException {
    // Business validation logic
}

private static void updateRelatedEntities(IgniteClient client, Transaction tx) 
        throws DataIntegrityException {
    // Update related data
}

static class DataIntegrityException extends Exception {
    public DataIntegrityException(String message) {
        super(message);
    }
}
```

#### Transaction State Monitoring

```java
public class TransactionMonitor {
    
    public static void monitorTransaction(IgniteClient client) {
        Transaction tx = client.transactions().begin(new TransactionOptions()
            .timeout(Duration.ofSeconds(30))
            .label("monitored-transaction"));
        
        try {
            System.out.println("Transaction started: " + tx.isReadOnly());
            
            // Perform operations while monitoring state
            RecordView<Artist> view = client.tables().table("Artist").recordView(Artist.class);
            
            Artist artist = new Artist(800, "Radiohead");
            view.upsert(tx, artist);
            
            System.out.println("After upsert - transaction active");
            
            // Check if we should commit or rollback based on business logic
            if (shouldCommit(artist)) {
                tx.commit();
                System.out.println("Transaction committed");
            } else {
                tx.rollback();
                System.out.println("Transaction rolled back due to business rules");
            }
            
        } catch (Exception e) {
            System.err.println("Error in transaction: " + e.getMessage());
            try {
                tx.rollback();
                System.out.println("Transaction rolled back due to error");
            } catch (Exception rollbackError) {
                System.err.println("Rollback failed: " + rollbackError.getMessage());
            }
        }
    }
    
    private static boolean shouldCommit(Artist artist) {
        // Business logic to determine if transaction should commit
        return artist.getName() != null && !artist.getName().trim().isEmpty();
    }
}
```

### Best Practices for Transaction Management

1. **Keep Transactions Short**: Minimize transaction duration to reduce lock contention
2. **Use Read-Only Transactions**: For queries that don't modify data
3. **Set Appropriate Timeouts**: Based on operation complexity
4. **Handle All Exception Types**: Network, business logic, and system errors
5. **Use Functional Style**: `runInTransaction()` for automatic resource management
6. **Label Transactions**: For monitoring and debugging
7. **Avoid Nested Transactions**: Not supported - use single transaction scope
8. **Test Rollback Scenarios**: Ensure rollback logic works correctly

```java
// Complete example following best practices
public static boolean bestPracticeTransaction(IgniteClient client, 
                                           Integer artistId, String newName) {
    // Clear transaction labeling
    String transactionLabel = "update-artist-" + artistId;
    
    // Appropriate timeout for simple operation
    TransactionOptions options = new TransactionOptions()
        .timeout(Duration.ofSeconds(10))
        .label(transactionLabel)
        .readOnly(false);
    
    return client.transactions().runInTransaction(options, tx -> {
        try {
            RecordView<Artist> view = client.tables().table("Artist").recordView(Artist.class);
            
            // Read current state
            Artist artist = view.get(tx, createArtistKey(artistId));
            if (artist == null) {
                System.err.println("Artist not found: " + artistId);
                return false;
            }
            
            // Business validation
            if (newName == null || newName.trim().isEmpty()) {
                System.err.println("Invalid artist name");
                return false;
            }
            
            // Update
            artist.setName(newName.trim());
            view.upsert(tx, artist);
            
            System.out.println("Updated artist: " + artist.getName());
            return true; // Commit
            
        } catch (Exception e) {
            System.err.println("Transaction failed: " + e.getMessage());
            return false; // Rollback
        }
    });
}
```
