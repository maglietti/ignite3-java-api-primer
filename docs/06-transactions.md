# 6. Transactions

## Getting Started with Transactions

Access the transaction manager through the main Ignite client interface:

```java
// Get transaction manager
IgniteTransactions transactions = client.transactions();

// Begin a transaction
Transaction tx = transactions.begin();
```

The `Transaction` interface provides methods for commit, rollback, and state management. The `IgniteTransactions` interface offers several patterns for transaction execution.

## Transaction Interface Usage

### Basic Transaction Pattern

```java
// Explicit transaction management
Transaction tx = client.transactions().begin();
try {
    // Use transaction with Table API
    RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
    Artist artist = new Artist(100, "Led Zeppelin");
    artistView.upsert(tx, artist);
    
    // Use transaction with SQL API
    client.sql().execute(tx, 
        "INSERT INTO Album (AlbumId, Title, ArtistId) VALUES (?, ?, ?)",
        200, "Led Zeppelin IV", 100);
    
    tx.commit();
} catch (Exception e) {
    tx.rollback();
    throw e;
}
```

### Closure-based Transactions

The `runInTransaction()` method handles commit/rollback automatically:

```java
// Automatic transaction management
boolean success = client.transactions().runInTransaction(tx -> {
    RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
    
    Artist artist = new Artist(101, "The Beatles");
    artistView.upsert(tx, artist);
    
    // Return true to commit, false to rollback
    return true;
});
```

## Synchronous Transaction Patterns

### Using Transaction with Table API

```java
// Combine Table API operations in a transaction
Transaction tx = client.transactions().begin();
try {
    RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
    RecordView<Album> albumView = client.tables().table("Album").recordView(Album.class);
    
    // Insert artist
    Artist artist = new Artist(102, "Pink Floyd");
    artistView.upsert(tx, artist);
    
    // Insert related album
    Album album = new Album(201, "The Dark Side of the Moon", 102);
    albumView.upsert(tx, album);
    
    tx.commit();
    System.out.println("Artist and album created");
} catch (Exception e) {
    tx.rollback();
    throw e;
}
```

### Using Transaction with SQL API

```java
// Execute multiple SQL statements in a transaction
Transaction tx = client.transactions().begin();
try {
    client.sql().execute(tx, 
        "UPDATE Track SET UnitPrice = UnitPrice * 1.1 WHERE AlbumId = ?", 
        201);
    
    client.sql().execute(tx,
        "INSERT INTO Playlist (PlaylistId, Name) VALUES (?, ?)", 
        50, "Rock Classics");
    
    tx.commit();
} catch (Exception e) {
    tx.rollback();
    throw e;
}
```

### Mixing Table API and SQL API

```java
// Use both APIs within the same transaction
client.transactions().runInTransaction(tx -> {
    // Use Table API for object operations
    RecordView<Customer> customerView = client.tables().table("Customer").recordView(Customer.class);
    Customer customer = customerView.get(tx, createCustomerKey(1));
    
    if (customer != null) {
        // Use SQL API for bulk operations
        client.sql().execute(tx,
            "UPDATE Invoice SET BillingCity = ? WHERE CustomerId = ?",
            customer.getCity(), customer.getCustomerId());
        
        return true;
    }
    return false;
});

private static Customer createCustomerKey(Integer id) {
    Customer key = new Customer();
    key.setCustomerId(id);
    return key;
}
```

## Asynchronous Transaction Patterns

### Basic Async Transaction

```java
// Begin transaction asynchronously
CompletableFuture<Void> future = client.transactions().beginAsync()
    .thenCompose(tx -> {
        RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
        Artist key = createArtistKey(103);
        
        return artistView.getAsync(tx, key)
            .thenCompose(artist -> {
                if (artist != null) {
                    artist.setName(artist.getName() + " (Updated)");
                    return artistView.upsertAsync(tx, artist);
                } else {
                    return CompletableFuture.failedFuture(
                        new RuntimeException("Artist not found"));
                }
            })
            .thenCompose(ignored -> tx.commitAsync())
            .exceptionally(throwable -> {
                tx.rollbackAsync();
                throw new RuntimeException(throwable);
            });
    });

future.join();
```

### Async runInTransaction Pattern

```java
// Async functional transaction pattern
CompletableFuture<String> result = client.transactions().runInTransactionAsync(tx -> {
    RecordView<Track> trackView = client.tables().table("Track").recordView(Track.class);
    
    return trackView.getAsync(tx, createTrackKey(1))
        .thenCompose(track -> {
            if (track != null) {
                track.setName(track.getName() + " (Remastered)");
                return trackView.upsertAsync(tx, track)
                    .thenApply(ignored -> "Track updated: " + track.getName());
            } else {
                return CompletableFuture.completedFuture("Track not found");
            }
        });
});

String message = result.join();
System.out.println(message);
```

### Chaining Async Operations

```java
// Chain multiple async operations within a transaction
CompletableFuture<Void> chainedOps = client.transactions().beginAsync()
    .thenCompose(tx -> {
        RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
        RecordView<Album> albumView = client.tables().table("Album").recordView(Album.class);
        
        // Chain operations: create artist then album
        Artist artist = new Artist(104, "Queen");
        
        return artistView.upsertAsync(tx, artist)
            .thenCompose(ignored -> {
                Album album = new Album(202, "Bohemian Rhapsody", 104);
                return albumView.upsertAsync(tx, album);
            })
            .thenCompose(ignored -> tx.commitAsync())
            .exceptionally(throwable -> {
                tx.rollbackAsync();
                throw new RuntimeException(throwable);
            });
    });

chainedOps.join();
```

### Parallel Async Operations

```java
// Execute multiple operations in parallel within a transaction
CompletableFuture<Void> parallelOps = client.transactions().beginAsync()
    .thenCompose(tx -> {
        RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
        
        // Create multiple artists in parallel
        List<CompletableFuture<Void>> operations = Arrays.asList(
            artistView.upsertAsync(tx, new Artist(105, "The Rolling Stones")),
            artistView.upsertAsync(tx, new Artist(106, "The Who")),
            artistView.upsertAsync(tx, new Artist(107, "Led Zeppelin"))
        );
        
        // Wait for all operations to complete
        return CompletableFuture.allOf(operations.toArray(new CompletableFuture[0]))
            .thenCompose(ignored -> tx.commitAsync())
            .exceptionally(throwable -> {
                tx.rollbackAsync();
                throw new RuntimeException(throwable);
            });
    });

parallelOps.join();
```

### Error Handling in Async Transactions

```java
// Async transaction with error handling
CompletableFuture<String> asyncWithErrorHandling = client.transactions().beginAsync()
    .thenCompose(tx -> {
        RecordView<Artist> view = client.tables().table("Artist").recordView(Artist.class);
        
        return view.getAsync(tx, createArtistKey(108))
            .thenCompose(artist -> {
                if (artist != null) {
                    artist.setName(artist.getName() + " (Updated)");
                    return view.upsertAsync(tx, artist);
                } else {
                    return CompletableFuture.failedFuture(
                        new RuntimeException("Artist not found"));
                }
            })
            .thenCompose(ignored -> tx.commitAsync())
            .thenApply(ignored -> "Artist updated successfully")
            .exceptionally(throwable -> {
                tx.rollbackAsync();
                return "Transaction failed: " + throwable.getMessage();
            });
    });

String result = asyncWithErrorHandling.join();
System.out.println(result);
```

### Timeout Configuration

```java
// Set timeout for async transaction
TransactionOptions options = new TransactionOptions()
    .timeoutMillis(5000)  // 5 second timeout
    .readOnly(false);

CompletableFuture<Void> timedTransaction = client.transactions().beginAsync(options)
    .thenCompose(tx -> {
        RecordView<Artist> view = client.tables().table("Artist").recordView(Artist.class);
        Artist artist = new Artist(109, "Tool");
        
        return view.upsertAsync(tx, artist)
            .orTimeout(3, TimeUnit.SECONDS)  // Additional operation timeout
            .thenCompose(ignored -> tx.commitAsync())
            .exceptionally(throwable -> {
                if (throwable instanceof TimeoutException) {
                    System.err.println("Operation timed out");
                }
                tx.rollbackAsync();
                throw new RuntimeException(throwable);
            });
    });

timedTransaction.join();
```

## TransactionOptions Configuration

### Read-Only Transactions

Use `readOnly(true)` for transactions that only query data:

```java
// Configure read-only transaction
TransactionOptions readOnlyOptions = new TransactionOptions()
    .readOnly(true)
    .timeoutMillis(30000);  // 30 second timeout

client.transactions().runInTransaction(readOnlyOptions, tx -> {
    // Read operations only
    ResultSet<SqlRow> artistCount = client.sql().execute(tx, 
        "SELECT COUNT(*) as count FROM Artist");
    
    ResultSet<SqlRow> albumCount = client.sql().execute(tx, 
        "SELECT COUNT(*) as count FROM Album");
    
    long artists = artistCount.next().longValue("count");
    long albums = albumCount.next().longValue("count");
    
    System.out.printf("Found %d artists and %d albums%n", artists, albums);
    return true;
});
```

### Timeout Configuration

```java
// Short timeout for quick operations
TransactionOptions quickOptions = new TransactionOptions()
    .timeoutMillis(5000)   // 5 seconds
    .readOnly(false);

client.transactions().runInTransaction(quickOptions, tx -> {
    RecordView<Artist> view = client.tables().table("Artist").recordView(Artist.class);
    Artist artist = view.get(tx, createArtistKey(110));
    if (artist != null) {
        artist.setName(artist.getName() + " (Updated)");
        view.upsert(tx, artist);
    }
    return true;
});

// Long timeout for complex operations
TransactionOptions complexOptions = new TransactionOptions()
    .timeoutMillis(300000)  // 5 minutes
    .readOnly(false);

client.transactions().runInTransaction(complexOptions, tx -> {
    // Perform complex multi-table operations
    performBulkOperations(client, tx);
    return true;
});

private static void performBulkOperations(IgniteClient client, Transaction tx) {
    // Complex business logic implementation
}
```

### Default vs Custom Options

```java
// Using default options
Transaction defaultTx = client.transactions().begin();

// Using custom options
TransactionOptions customOptions = new TransactionOptions()
    .timeoutMillis(15000)
    .readOnly(false);

Transaction customTx = client.transactions().begin(customOptions);

// Options with functional pattern
client.transactions().runInTransaction(
    new TransactionOptions().timeoutMillis(10000).readOnly(true),
    tx -> {
        // Transaction operations
        return true;
    }
);
```

## Exception Handling Patterns

### Try-Catch with Explicit Transactions

```java
public static void explicitTransactionHandling(IgniteClient client) {
    Transaction tx = null;
    try {
        TransactionOptions options = new TransactionOptions()
            .timeoutMillis(30000)
            .readOnly(false);
        
        tx = client.transactions().begin(options);
        
        RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
        
        // Perform operations
        Artist artist = new Artist(111, "Metallica");
        artistView.upsert(tx, artist);
        
        // Validate business rules
        validateArtist(artist);
        
        // Commit on success
        tx.commit();
        System.out.println("Transaction committed");
        
    } catch (TransactionException e) {
        System.err.println("Transaction error: " + e.getMessage());
        if (tx != null) tx.rollback();
    } catch (RuntimeException e) {
        System.err.println("Business error: " + e.getMessage());
        if (tx != null) tx.rollback();
    } catch (Exception e) {
        System.err.println("Unexpected error: " + e.getMessage());
        if (tx != null) tx.rollback();
        throw e;
    }
}

private static void validateArtist(Artist artist) {
    if (artist.getName() == null || artist.getName().trim().isEmpty()) {
        throw new RuntimeException("Artist name cannot be empty");
    }
}
```

### Functional Error Handling

```java
// Use runInTransaction for automatic commit/rollback
public static boolean functionalErrorHandling(IgniteClient client) {
    return client.transactions().runInTransaction(tx -> {
        try {
            RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
            RecordView<Album> albumView = client.tables().table("Album").recordView(Album.class);
            
            // Create artist
            Artist artist = new Artist(112, "Iron Maiden");
            artistView.upsert(tx, artist);
            
            // Create album
            Album album = new Album(203, "The Number of the Beast", 112);
            albumView.upsert(tx, album);
            
            // Validate data
            if (artist.getName() != null && album.getTitle() != null) {
                System.out.println("Artist and album created successfully");
                return true; // Commit
            } else {
                System.err.println("Invalid data");
                return false; // Rollback
            }
            
        } catch (Exception e) {
            System.err.println("Transaction failed: " + e.getMessage());
            return false; // Rollback
        }
    });
}
```

### Exception Types

```java
// Handle specific Ignite transaction exceptions
public static void handleSpecificExceptions(IgniteClient client) {
    try {
        client.transactions().runInTransaction(tx -> {
            RecordView<Artist> view = client.tables().table("Artist").recordView(Artist.class);
            
            Artist artist = new Artist(113, "AC/DC");
            view.upsert(tx, artist);
            
            return true;
        });
    } catch (TransactionException e) {
        // Handle transaction-specific errors
        System.err.println("Transaction error: " + e.getMessage());
    } catch (RuntimeException e) {
        // Handle runtime errors
        System.err.println("Runtime error: " + e.getMessage());
    }
}
```

### Transaction State Queries

```java
// Check transaction properties
public static void queryTransactionState(IgniteClient client) {
    TransactionOptions options = new TransactionOptions()
        .timeoutMillis(30000)
        .readOnly(true);
    
    Transaction tx = client.transactions().begin(options);
    
    try {
        // Query transaction state
        boolean isReadOnly = tx.isReadOnly();
        System.out.println("Transaction is read-only: " + isReadOnly);
        
        // Perform operations based on state
        if (isReadOnly) {
            // Only query operations
            ResultSet<SqlRow> result = client.sql().execute(tx, 
                "SELECT COUNT(*) FROM Artist");
            System.out.println("Artist count: " + result.next().longValue(0));
        } else {
            // Modify operations allowed
            RecordView<Artist> view = client.tables().table("Artist").recordView(Artist.class);
            Artist artist = new Artist(114, "The Beatles");
            view.upsert(tx, artist);
        }
        
        tx.commit();
    } catch (Exception e) {
        tx.rollback();
        throw e;
    }
}
```

## Complete Example

```java
/**
 * Demonstrates transaction patterns with music store data
 */
public class TransactionExamples {
    
    // Update artist and create new album in a single transaction
    public static boolean updateArtistAndCreateAlbum(IgniteClient client, 
                                                    Integer artistId, 
                                                    String albumTitle) {
        TransactionOptions options = new TransactionOptions()
            .timeoutMillis(10000)
            .readOnly(false);
        
        return client.transactions().runInTransaction(options, tx -> {
            try {
                RecordView<Artist> artistView = client.tables().table("Artist").recordView(Artist.class);
                RecordView<Album> albumView = client.tables().table("Album").recordView(Album.class);
                
                // Read and update artist
                Artist artist = artistView.get(tx, createArtistKey(artistId));
                if (artist == null) {
                    System.err.println("Artist not found: " + artistId);
                    return false;
                }
                
                // Update artist name
                artist.setName(artist.getName() + " (Deluxe Edition)");
                artistView.upsert(tx, artist);
                
                // Create new album
                Album album = new Album(generateAlbumId(), albumTitle, artistId);
                albumView.upsert(tx, album);
                
                System.out.println("Updated artist and created album: " + albumTitle);
                return true; // Commit
                
            } catch (Exception e) {
                System.err.println("Transaction failed: " + e.getMessage());
                return false; // Rollback
            }
        });
    }
    
    // Helper methods
    private static Artist createArtistKey(Integer id) {
        Artist key = new Artist();
        key.setArtistId(id);
        return key;
    }
    
    private static Integer generateAlbumId() {
        return (int) (System.currentTimeMillis() % 100000);
    }
}
```

## Key Interface Methods

### IgniteTransactions Methods

```java
// Begin transactions
Transaction begin()
Transaction begin(TransactionOptions options)
CompletableFuture<Transaction> beginAsync()
CompletableFuture<Transaction> beginAsync(TransactionOptions options)

// Functional transactions
void runInTransaction(Consumer<Transaction> closure)
void runInTransaction(Consumer<Transaction> closure, TransactionOptions options)
<T> T runInTransaction(Function<Transaction, T> closure)
<T> T runInTransaction(Function<Transaction, T> closure, TransactionOptions options)

// Async functional transactions
<T> CompletableFuture<T> runInTransactionAsync(Function<Transaction, CompletableFuture<T>> closure)
<T> CompletableFuture<T> runInTransactionAsync(Function<Transaction, CompletableFuture<T>> closure, TransactionOptions options)
```

### Transaction Methods

```java
// Synchronous operations
void commit() throws TransactionException
void rollback() throws TransactionException

// Asynchronous operations
CompletableFuture<Void> commitAsync()
CompletableFuture<Void> rollbackAsync()

// State queries
boolean isReadOnly()
```

### TransactionOptions Properties

```java
// Configure timeout (in milliseconds)
TransactionOptions timeoutMillis(long timeoutMillis)
long timeoutMillis()

// Configure read-only mode
TransactionOptions readOnly(boolean readOnly)
boolean readOnly()
```
