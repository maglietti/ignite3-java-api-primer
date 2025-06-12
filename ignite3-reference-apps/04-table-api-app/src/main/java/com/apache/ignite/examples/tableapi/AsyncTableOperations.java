package com.apache.ignite.examples.tableapi;

import com.apache.ignite.examples.setup.model.Artist;
import com.apache.ignite.examples.setup.model.Album;
import com.apache.ignite.examples.setup.model.Track;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Advanced demonstration of asynchronous Table API operations with Apache Ignite 3.
 * 
 * This application showcases high-performance async patterns using the music store dataset.
 * 
 * Key concepts demonstrated:
 * - Advanced async operation chaining
 * - Parallel async execution patterns
 * - Error handling and recovery strategies
 * - Circuit breaker pattern implementation
 * - Retry logic with exponential backoff
 * - Timeout handling
 * - Performance monitoring and metrics
 * - Complex async workflows
 * 
 * Prerequisites:
 * 1. Ignite cluster running (use 00-docker/init-cluster.sh)
 * 2. Music store schema and data loaded (use 01-sample-data-setup)
 * 
 * Learning Objectives:
 * - Master advanced async programming patterns with Table API
 * - Implement production-ready error handling and recovery
 * - Build resilient async applications
 * - Understand performance implications of async operations
 * - Practice real-world async patterns and best practices
 */
public class AsyncTableOperations {
    
    private static final String CLUSTER_ENDPOINT = "127.0.0.1:10800";
    private static final int DEMO_TIMEOUT_SECONDS = 30;
    
    public static void main(String[] args) {
        System.out.println("=== Apache Ignite 3 Advanced Async Operations Demo ===");
        System.out.println("Demonstrating production-ready async patterns with music store data\n");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(CLUSTER_ENDPOINT)
                .build()) {
            
            System.out.println("✓ Connected to Ignite cluster at " + CLUSTER_ENDPOINT);
            
            // Execute all async demonstrations
            demonstrateAsyncChaining(client);
            demonstrateParallelExecution(client);
            demonstrateErrorHandlingPatterns(client);
            demonstrateCircuitBreakerPattern(client);
            demonstrateRetryWithBackoff(client);
            demonstrateTimeoutHandling(client);
            demonstrateComplexAsyncWorkflows(client);
            demonstratePerformanceOptimizations(client);
            
            System.out.println("\n=== Advanced Async Operations Demo Complete ===");
            
        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Demonstrates advanced async operation chaining patterns.
     * 
     * Concepts covered:
     * - Complex CompletableFuture chaining
     * - Conditional async flows
     * - Data transformation in async chains
     * - Error propagation through chains
     */
    private static void demonstrateAsyncChaining(IgniteClient client) {
        System.out.println("\n--- Advanced Async Operation Chaining ---");
        
        RecordView<Artist> artists = client.tables().table("Artist").recordView(Artist.class);
        RecordView<Album> albums = client.tables().table("Album").recordView(Album.class);
        RecordView<Track> tracks = client.tables().table("Track").recordView(Track.class);
        
        System.out.println("\n1. Complex Async Chain: Artist → Albums → Track Count:");
        
        // Create a complex async chain: get artist, get their albums, count tracks
        CompletableFuture<String> complexChain = artists
            .getAsync(null, new Artist(7000, null))
            .thenCompose(artist -> {
                if (artist == null) {
                    // Insert artist if not found
                    Artist newArtist = new Artist(7000, "Porcupine Tree");
                    return artists.upsertAsync(null, newArtist)
                        .thenCompose(ignored -> CompletableFuture.completedFuture(newArtist));
                } else {
                    return CompletableFuture.completedFuture(artist);
                }
            })
            .thenCompose(artist -> {
                // Get albums for this artist (simulate with sample data)
                List<Album> sampleAlbums = Arrays.asList(
                    new Album(7000, 7000, "In Absentia"),
                    new Album(7001, 7000, "Deadwing"),
                    new Album(7002, 7000, "Fear of a Blank Planet")
                );
                
                // Insert albums asynchronously
                return albums.upsertAllAsync(null, sampleAlbums)
                    .thenCompose(ignored -> CompletableFuture.completedFuture(sampleAlbums));
            })
            .thenCompose(albumList -> {
                // For each album, count tracks (simulate)
                List<CompletableFuture<Integer>> trackCountFutures = albumList.stream()
                    .map(album -> {
                        // Simulate track counting with async operation
                        return CompletableFuture.supplyAsync(() -> {
                            // Simulate different track counts per album
                            return 8 + (album.getAlbumId() % 3);  // 8, 9, or 10 tracks
                        });
                    })
                    .collect(Collectors.toList());
                
                // Combine all track count futures
                return CompletableFuture.allOf(trackCountFutures.toArray(new CompletableFuture[0]))
                    .thenApply(ignored -> {
                        int totalTracks = trackCountFutures.stream()
                            .mapToInt(future -> future.join())
                            .sum();
                        return totalTracks;
                    });
            })
            .thenApply(totalTracks -> {
                return "Artist 7000 has " + totalTracks + " tracks across all albums";
            })
            .exceptionally(throwable -> {
                return "Error in async chain: " + throwable.getMessage();
            });
        
        // Wait for the complex chain to complete
        try {
            String result = complexChain.get(DEMO_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            System.out.println("   ✓ Complex chain result: " + result);
        } catch (Exception e) {
            System.err.println("   ✗ Complex chain failed: " + e.getMessage());
        }
        
        System.out.println("\n2. Conditional Async Flow:");
        
        // Demonstrate conditional async execution based on data
        artists.getAsync(null, new Artist(7001, null))
            .thenCompose(existingArtist -> {
                if (existingArtist != null) {
                    // Artist exists - update their name
                    existingArtist.setName(existingArtist.getName() + " (Updated)");
                    return artists.upsertAsync(null, existingArtist)
                        .thenApply(ignored -> "Updated existing artist");
                } else {
                    // Artist doesn't exist - create new one
                    Artist newArtist = new Artist(7001, "Opeth");
                    return artists.upsertAsync(null, newArtist)
                        .thenApply(ignored -> "Created new artist");
                }
            })
            .thenAccept(action -> System.out.println("   ✓ Conditional flow: " + action))
            .join();  // Wait for completion in demo
        
        System.out.println("\n✓ Async chaining benefits:");
        System.out.println("  - Compose complex workflows without blocking");
        System.out.println("  - Handle conditional logic in async context");
        System.out.println("  - Transform data through async pipeline");
        System.out.println("  - Maintain type safety throughout the chain");
    }
    
    /**
     * Demonstrates parallel async execution patterns for maximum performance.
     * 
     * Concepts covered:
     * - Fork-join parallel execution
     * - CompletableFuture.allOf for coordination
     * - Load balancing across async operations
     * - Performance measurement
     */
    private static void demonstrateParallelExecution(IgniteClient client) {
        System.out.println("\n--- Parallel Async Execution Patterns ---");
        
        RecordView<Artist> artists = client.tables().table("Artist").recordView(Artist.class);
        
        System.out.println("\n1. Parallel Artist Loading:");
        
        // Prepare large dataset for parallel loading
        List<Artist> metalArtists = Arrays.asList(
            new Artist(7100, "Metallica"),
            new Artist(7101, "Iron Maiden"),
            new Artist(7102, "Black Sabbath"),
            new Artist(7103, "Judas Priest"),
            new Artist(7104, "Megadeth"),
            new Artist(7105, "Slayer"),
            new Artist(7106, "Anthrax"),
            new Artist(7107, "Pantera"),
            new Artist(7108, "Dio"),
            new Artist(7109, "Ozzy Osbourne")
        );
        
        long startTime = System.currentTimeMillis();
        
        // Create parallel async operations
        List<CompletableFuture<Void>> insertFutures = metalArtists.stream()
            .map(artist -> artists.upsertAsync(null, artist))
            .collect(Collectors.toList());
        
        // Wait for all parallel insertions to complete
        CompletableFuture<Void> allInserts = CompletableFuture.allOf(
            insertFutures.toArray(new CompletableFuture[0]));
        
        allInserts.thenRun(() -> {
            long endTime = System.currentTimeMillis();
            System.out.println("   ✓ Parallel loading of " + metalArtists.size() + 
                             " artists completed in " + (endTime - startTime) + "ms");
        }).join();  // Wait for completion in demo
        
        System.out.println("\n2. Parallel Retrieval with Processing:");
        
        // Prepare keys for parallel retrieval
        List<Integer> artistIds = Arrays.asList(7100, 7101, 7102, 7103, 7104);
        
        startTime = System.currentTimeMillis();
        
        // Create parallel get operations with processing
        List<CompletableFuture<String>> processingFutures = artistIds.stream()
            .map(id -> artists.getAsync(null, new Artist(id, null))
                .thenApply(artist -> {
                    if (artist != null) {
                        // Simulate some processing
                        String processed = artist.getName().toUpperCase() + " [ID:" + artist.getArtistId() + "]";
                        try {
                            Thread.sleep(10);  // Simulate processing time
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return processed;
                    } else {
                        return "NOT FOUND [ID:" + id + "]";
                    }
                }))
            .collect(Collectors.toList());
        
        // Collect all results
        CompletableFuture<List<String>> allResults = CompletableFuture.allOf(
            processingFutures.toArray(new CompletableFuture[0]))
            .thenApply(ignored -> processingFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
        
        allResults.thenAccept(results -> {
            long endTime = System.currentTimeMillis();
            System.out.println("   ✓ Parallel retrieval and processing completed in " + 
                             (endTime - startTime) + "ms:");
            results.forEach(result -> System.out.println("     " + result));
        }).join();  // Wait for completion in demo
        
        System.out.println("\n3. Fork-Join Pattern with Aggregation:");
        
        // Fork: parallel operations, Join: aggregate results
        CompletableFuture<Integer> countFuture = CompletableFuture.supplyAsync(() -> metalArtists.size());
        CompletableFuture<String> firstArtistFuture = artists.getAsync(null, new Artist(7100, null))
            .thenApply(artist -> artist != null ? artist.getName() : "Unknown");
        CompletableFuture<String> lastArtistFuture = artists.getAsync(null, new Artist(7109, null))
            .thenApply(artist -> artist != null ? artist.getName() : "Unknown");
        
        // Combine all three async operations
        CompletableFuture<String> aggregatedResult = CompletableFuture.allOf(
            countFuture, firstArtistFuture, lastArtistFuture)
            .thenApply(ignored -> {
                int count = countFuture.join();
                String first = firstArtistFuture.join();
                String last = lastArtistFuture.join();
                return String.format("Dataset: %d artists, First: %s, Last: %s", count, first, last);
            });
        
        aggregatedResult.thenAccept(summary -> 
            System.out.println("   ✓ Aggregated result: " + summary)).join();
        
        System.out.println("\n✓ Parallel execution benefits:");
        System.out.println("  - Maximum utilization of available resources");
        System.out.println("  - Reduced total execution time");
        System.out.println("  - Independent operation execution");
        System.out.println("  - Scalable performance patterns");
    }
    
    /**
     * Demonstrates comprehensive error handling patterns for async operations.
     * 
     * Concepts covered:
     * - Exception classification and handling
     * - Fallback strategies
     * - Error recovery patterns
     * - Logging and monitoring for async errors
     */
    private static void demonstrateErrorHandlingPatterns(IgniteClient client) {
        System.out.println("\n--- Comprehensive Error Handling Patterns ---");
        
        RecordView<Artist> artists = client.tables().table("Artist").recordView(Artist.class);
        
        System.out.println("\n1. Exception Classification and Handling:");
        
        // Simulate different types of errors
        CompletableFuture<String> errorHandlingDemo = artists
            .getAsync(null, new Artist(7200, null))
            .thenCompose(artist -> {
                if (artist == null) {
                    // Simulate a connection error
                    CompletableFuture<Artist> failedFuture = new CompletableFuture<>();
                    failedFuture.completeExceptionally(new RuntimeException("Simulated connection error"));
                    return failedFuture;
                } else {
                    return CompletableFuture.completedFuture(artist);
                }
            })
            .thenApply(artist -> "Success: " + artist.getName())
            .exceptionally(throwable -> {
                // Classify and handle different exception types
                if (throwable.getCause() instanceof RuntimeException) {
                    System.out.println("   ⚠ Handling RuntimeException: " + throwable.getMessage());
                    return "Recovered from runtime error";
                } else if (throwable instanceof TimeoutException) {
                    System.out.println("   ⚠ Handling timeout: " + throwable.getMessage());
                    return "Recovered from timeout";
                } else {
                    System.out.println("   ⚠ Handling unknown error: " + throwable.getMessage());
                    return "Recovered from unknown error";
                }
            });
        
        String result = errorHandlingDemo.join();
        System.out.println("   ✓ Error handling result: " + result);
        
        System.out.println("\n2. Fallback Strategy Pattern:");
        
        // Demonstrate fallback chain with multiple recovery options
        CompletableFuture<Artist> fallbackChain = artists
            .getAsync(null, new Artist(9999, null))  // Non-existent artist
            .thenCompose(artist -> {
                if (artist != null) {
                    return CompletableFuture.completedFuture(artist);
                } else {
                    // First fallback: try a different artist
                    return artists.getAsync(null, new Artist(7100, null));
                }
            })
            .thenCompose(artist -> {
                if (artist != null) {
                    return CompletableFuture.completedFuture(artist);
                } else {
                    // Second fallback: create a default artist
                    Artist defaultArtist = new Artist(9999, "Unknown Artist");
                    return artists.upsertAsync(null, defaultArtist)
                        .thenApply(ignored -> defaultArtist);
                }
            })
            .exceptionally(throwable -> {
                // Final fallback: return a hardcoded default
                System.out.println("   ⚠ All fallbacks failed, using hardcoded default");
                return new Artist(-1, "System Default");
            });
        
        Artist finalArtist = fallbackChain.join();
        System.out.println("   ✓ Fallback chain result: " + finalArtist);
        
        System.out.println("\n3. Error Recovery with Logging:");
        
        AsyncErrorHandler errorHandler = new AsyncErrorHandler();
        
        CompletableFuture<Void> monitoredOperation = artists.upsertAsync(null, new Artist(7201, "Tool"))
            .handle((result, throwable) -> {
                if (throwable != null) {
                    errorHandler.handleError("Artist upsert", throwable);
                    return null;
                } else {
                    errorHandler.recordSuccess("Artist upsert");
                    return result;
                }
            });
        
        monitoredOperation.join();
        errorHandler.printStatistics();
        
        System.out.println("\n✓ Error handling strategies:");
        System.out.println("  - Classify exceptions for appropriate handling");
        System.out.println("  - Implement fallback chains for resilience");
        System.out.println("  - Monitor and log errors for operational insight");
        System.out.println("  - Provide graceful degradation of functionality");
    }
    
    /**
     * Demonstrates circuit breaker pattern for preventing cascade failures.
     * 
     * Concepts covered:
     * - Circuit breaker state management
     * - Failure threshold configuration
     * - Automatic recovery mechanisms
     * - Fast-fail behavior
     */
    private static void demonstrateCircuitBreakerPattern(IgniteClient client) {
        System.out.println("\n--- Circuit Breaker Pattern ---");
        
        RecordView<Artist> artists = client.tables().table("Artist").recordView(Artist.class);
        
        System.out.println("\n1. Circuit Breaker Implementation:");
        
        AsyncCircuitBreaker circuitBreaker = new AsyncCircuitBreaker(
            3,      // failure threshold
            5000,   // timeout in milliseconds
            1000    // recovery timeout
        );
        
        // Simulate multiple operations, some of which will fail
        for (int i = 0; i < 8; i++) {
            final int operationId = i;
            
            CompletableFuture<String> operation = circuitBreaker.execute(() -> {
                // Simulate failures for operations 3, 4, 5
                if (operationId >= 3 && operationId <= 5) {
                    CompletableFuture<Artist> failedFuture = new CompletableFuture<>();
                    failedFuture.completeExceptionally(new RuntimeException("Simulated failure " + operationId));
                    return failedFuture.thenApply(artist -> "Success: " + artist.getName());
                } else {
                    // Successful operation
                    Artist artist = new Artist(7300 + operationId, "Artist " + operationId);
                    return artists.upsertAsync(null, artist)
                        .thenApply(ignored -> "Success: Operation " + operationId);
                }
            });
            
            operation.thenAccept(result -> 
                System.out.println("   ✓ Operation " + operationId + ": " + result))
                .exceptionally(throwable -> {
                    System.out.println("   ✗ Operation " + operationId + ": " + throwable.getMessage());
                    return null;
                }).join();
            
            // Small delay between operations
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("   ✓ Circuit breaker state: " + circuitBreaker.getState());
        
        System.out.println("\n2. Circuit Breaker Recovery:");
        
        // Wait for recovery timeout
        System.out.println("   ⏳ Waiting for circuit breaker recovery...");
        try {
            Thread.sleep(1500);  // Wait longer than recovery timeout
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Try operation again - should work now
        circuitBreaker.execute(() -> {
            Artist artist = new Artist(7399, "Recovery Test Artist");
            return artists.upsertAsync(null, artist)
                .thenApply(ignored -> "Recovery successful");
        }).thenAccept(result -> 
            System.out.println("   ✓ Recovery test: " + result)
        ).join();
        
        System.out.println("\n✓ Circuit breaker benefits:");
        System.out.println("  - Prevent cascade failures in distributed systems");
        System.out.println("  - Fast-fail behavior when service is unavailable");
        System.out.println("  - Automatic recovery when service becomes available");
        System.out.println("  - Configurable failure thresholds and timeouts");
    }
    
    /**
     * Demonstrates retry logic with exponential backoff.
     * 
     * Concepts covered:
     * - Exponential backoff calculation
     * - Jitter to prevent thundering herd
     * - Maximum retry limits
     * - Retry on specific error types
     */
    private static void demonstrateRetryWithBackoff(IgniteClient client) {
        System.out.println("\n--- Retry Logic with Exponential Backoff ---");
        
        RecordView<Artist> artists = client.tables().table("Artist").recordView(Artist.class);
        
        System.out.println("\n1. Basic Retry with Exponential Backoff:");
        
        AsyncRetryHandler retryHandler = new AsyncRetryHandler(3, 100, 2.0, 100);
        
        // Simulate an operation that fails twice then succeeds
        AtomicInteger attemptCounter = new AtomicInteger(0);
        
        CompletableFuture<String> retryOperation = retryHandler.executeWithRetry(() -> {
            int attempt = attemptCounter.incrementAndGet();
            System.out.println("   → Attempt " + attempt);
            
            if (attempt <= 2) {
                // Fail first two attempts
                CompletableFuture<String> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(new RuntimeException("Transient failure on attempt " + attempt));
                return failedFuture;
            } else {
                // Succeed on third attempt
                Artist artist = new Artist(7400, "Retry Success Artist");
                return artists.upsertAsync(null, artist)
                    .thenApply(ignored -> "Operation succeeded on attempt " + attempt);
            }
        });
        
        retryOperation.thenAccept(result -> 
            System.out.println("   ✓ Retry result: " + result)
        ).exceptionally(throwable -> {
            System.out.println("   ✗ All retries failed: " + throwable.getMessage());
            return null;
        }).join();
        
        System.out.println("\n2. Selective Retry Based on Error Type:");
        
        // Retry only on specific exception types
        AsyncRetryHandler selectiveRetry = new AsyncRetryHandler(2, 50, 1.5, 50);
        
        CompletableFuture<String> selectiveOperation = selectiveRetry.executeWithRetry(() -> {
            // Simulate a non-retryable error
            CompletableFuture<String> nonRetryableFuture = new CompletableFuture<>();
            nonRetryableFuture.completeExceptionally(new IllegalArgumentException("Non-retryable error"));
            return nonRetryableFuture;
        }, throwable -> {
            // Only retry on RuntimeException, not IllegalArgumentException
            return throwable instanceof RuntimeException && 
                   !(throwable instanceof IllegalArgumentException);
        });
        
        selectiveOperation.thenAccept(result -> 
            System.out.println("   ✓ Selective retry result: " + result)
        ).exceptionally(throwable -> {
            System.out.println("   ✓ Non-retryable error handled correctly: " + throwable.getMessage());
            return null;
        }).join();
        
        System.out.println("\n✓ Retry with backoff benefits:");
        System.out.println("  - Handle transient failures gracefully");
        System.out.println("  - Reduce load on struggling services");
        System.out.println("  - Configurable retry policies");
        System.out.println("  - Intelligent error classification");
    }
    
    /**
     * Demonstrates timeout handling patterns for async operations.
     * 
     * Concepts covered:
     * - Operation timeouts
     * - CompletableFuture.orTimeout usage
     * - Timeout recovery strategies
     * - Resource cleanup on timeout
     */
    private static void demonstrateTimeoutHandling(IgniteClient client) {
        System.out.println("\n--- Timeout Handling Patterns ---");
        
        RecordView<Artist> artists = client.tables().table("Artist").recordView(Artist.class);
        
        System.out.println("\n1. Basic Timeout Handling:");
        
        CompletableFuture<String> timeoutOperation = artists
            .getAsync(null, new Artist(7500, null))
            .orTimeout(5, TimeUnit.SECONDS)  // 5 second timeout
            .thenApply(artist -> {
                if (artist != null) {
                    return "Found: " + artist.getName();
                } else {
                    return "Artist not found";
                }
            })
            .exceptionally(throwable -> {
                if (throwable instanceof TimeoutException) {
                    System.out.println("   ⚠ Operation timed out after 5 seconds");
                    return "Operation timed out";
                } else {
                    System.out.println("   ⚠ Operation failed: " + throwable.getMessage());
                    return "Operation failed";
                }
            });
        
        String result = timeoutOperation.join();
        System.out.println("   ✓ Timeout handling result: " + result);
        
        System.out.println("\n2. Timeout with Retry:");
        
        // Combine timeout with retry logic
        CompletableFuture<String> timeoutWithRetry = retryWithTimeout(
            () -> {
                // Simulate slow operation
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(3000);  // 3 second delay
                        return "Slow operation completed";
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted", e);
                    }
                });
            },
            2000,  // 2 second timeout
            2      // 2 retries
        );
        
        timeoutWithRetry.thenAccept(retryResult -> 
            System.out.println("   ✓ Timeout with retry result: " + retryResult)
        ).exceptionally(throwable -> {
            System.out.println("   ✗ Timeout with retry failed: " + throwable.getMessage());
            return null;
        }).join();
        
        System.out.println("\n3. Resource Cleanup on Timeout:");
        
        AsyncResourceManager resourceManager = new AsyncResourceManager();
        
        CompletableFuture<String> resourceOperation = resourceManager.executeWithCleanup(
            () -> {
                // Simulate operation that needs resource cleanup
                return artists.upsertAsync(null, new Artist(7501, "Timeout Test Artist"))
                    .thenApply(ignored -> "Resource operation completed");
            },
            3000  // 3 second timeout
        );
        
        resourceOperation.thenAccept(cleanupResult -> 
            System.out.println("   ✓ Resource operation result: " + cleanupResult)
        ).exceptionally(throwable -> {
            System.out.println("   ✓ Resource cleanup triggered: " + throwable.getMessage());
            return null;
        }).join();
        
        System.out.println("\n✓ Timeout handling benefits:");
        System.out.println("  - Prevent indefinite blocking");
        System.out.println("  - Ensure responsive application behavior");
        System.out.println("  - Enable proper resource management");
        System.out.println("  - Support for retry strategies");
    }
    
    /**
     * Demonstrates complex async workflows combining multiple patterns.
     * 
     * Concepts covered:
     * - Multi-stage async pipelines
     * - Data transformation and validation
     * - Error handling at each stage
     * - Performance monitoring
     */
    private static void demonstrateComplexAsyncWorkflows(IgniteClient client) {
        System.out.println("\n--- Complex Async Workflows ---");
        
        RecordView<Artist> artists = client.tables().table("Artist").recordView(Artist.class);
        RecordView<Album> albums = client.tables().table("Album").recordView(Album.class);
        RecordView<Track> tracks = client.tables().table("Track").recordView(Track.class);
        
        System.out.println("\n1. Multi-Stage Data Pipeline:");
        
        // Complex workflow: validate → transform → store → verify
        CompletableFuture<String> dataImportPipeline = CompletableFuture
            .supplyAsync(() -> {
                // Stage 1: Data validation
                System.out.println("   → Stage 1: Validating input data");
                Map<String, Object> rawData = Map.of(
                    "artistName", "King Crimson",
                    "albumTitle", "In the Court of the Crimson King",
                    "trackCount", 5
                );
                
                // Simulate validation
                if (!rawData.containsKey("artistName") || 
                    ((String) rawData.get("artistName")).isEmpty()) {
                    throw new IllegalArgumentException("Invalid artist name");
                }
                
                return rawData;
            })
            .thenCompose(validatedData -> {
                // Stage 2: Transform and create entities
                System.out.println("   → Stage 2: Transforming data to entities");
                
                Artist artist = new Artist(7600, (String) validatedData.get("artistName"));
                Album album = new Album(7600, 7600, (String) validatedData.get("albumTitle"));
                
                // Store artist first
                return artists.upsertAsync(null, artist)
                    .thenCompose(ignored -> albums.upsertAsync(null, album))
                    .thenApply(ignored -> Map.of("artist", artist, "album", album));
            })
            .thenCompose(entities -> {
                // Stage 3: Create tracks
                System.out.println("   → Stage 3: Creating track records");
                
                List<Track> albumTracks = Arrays.asList(
                    createPipelineTrack(7600, 7600, "21st Century Schizoid Man", 442000),
                    createPipelineTrack(7601, 7600, "I Talk to the Wind", 362000),
                    createPipelineTrack(7602, 7600, "Epitaph", 513000),
                    createPipelineTrack(7603, 7600, "Moonchild", 732000),
                    createPipelineTrack(7604, 7600, "The Court of the Crimson King", 564000)
                );
                
                return tracks.upsertAllAsync(null, albumTracks)
                    .thenApply(ignored -> albumTracks.size());
            })
            .thenCompose(trackCount -> {
                // Stage 4: Verification
                System.out.println("   → Stage 4: Verifying stored data");
                
                return artists.getAsync(null, new Artist(7600, null))
                    .thenCompose(verifyArtist -> {
                        if (verifyArtist == null) {
                            throw new RuntimeException("Artist verification failed");
                        }
                        return CompletableFuture.completedFuture(
                            "Pipeline completed: " + verifyArtist.getName() + 
                            " with " + trackCount + " tracks");
                    });
            })
            .exceptionally(throwable -> {
                System.out.println("   ✗ Pipeline failed at stage: " + throwable.getMessage());
                return "Pipeline failed: " + throwable.getMessage();
            });
        
        String pipelineResult = dataImportPipeline.join();
        System.out.println("   ✓ Pipeline result: " + pipelineResult);
        
        System.out.println("\n2. Fan-Out/Fan-In Pattern:");
        
        // Fan-out: start multiple parallel operations
        // Fan-in: combine results
        List<Integer> artistIds = Arrays.asList(7600, 7100, 7101, 7102);
        
        CompletableFuture<Map<String, Object>> fanOutFanIn = CompletableFuture
            .supplyAsync(() -> {
                System.out.println("   → Fan-out: Starting parallel operations");
                return artistIds;
            })
            .thenCompose(ids -> {
                // Fan-out: parallel operations
                List<CompletableFuture<Artist>> artistFutures = ids.stream()
                    .map(id -> artists.getAsync(null, new Artist(id, null)))
                    .collect(Collectors.toList());
                
                // Fan-in: combine results
                return CompletableFuture.allOf(artistFutures.toArray(new CompletableFuture[0]))
                    .thenApply(ignored -> {
                        System.out.println("   → Fan-in: Combining results");
                        
                        List<Artist> foundArtists = artistFutures.stream()
                            .map(CompletableFuture::join)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                        
                        return Map.of(
                            "totalRequested", ids.size(),
                            "foundCount", foundArtists.size(),
                            "artists", foundArtists.stream()
                                .map(Artist::getName)
                                .collect(Collectors.toList())
                        );
                    });
            });
        
        fanOutFanIn.thenAccept(results -> {
            System.out.println("   ✓ Fan-out/Fan-in results:");
            System.out.println("     Requested: " + results.get("totalRequested"));
            System.out.println("     Found: " + results.get("foundCount"));
            @SuppressWarnings("unchecked")
            List<String> artistNames = (List<String>) results.get("artists");
            artistNames.forEach(name -> System.out.println("     - " + name));
        }).join();
        
        System.out.println("\n✓ Complex workflow benefits:");
        System.out.println("  - Compose multi-stage business processes");
        System.out.println("  - Handle data transformation pipelines");
        System.out.println("  - Implement parallel processing patterns");
        System.out.println("  - Maintain error handling throughout workflow");
    }
    
    /**
     * Demonstrates performance optimization techniques for async operations.
     * 
     * Concepts covered:
     * - Batching strategies
     * - Connection pooling optimization
     * - Memory usage patterns
     * - Performance monitoring
     */
    private static void demonstratePerformanceOptimizations(IgniteClient client) {
        System.out.println("\n--- Performance Optimization Techniques ---");
        
        RecordView<Artist> artists = client.tables().table("Artist").recordView(Artist.class);
        
        System.out.println("\n1. Optimal Batching Strategy:");
        
        // Compare different batching approaches
        List<Artist> largeDataset = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeDataset.add(new Artist(8000 + i, "Batch Artist " + i));
        }
        
        // Method 1: Single large batch
        long startTime = System.currentTimeMillis();
        artists.upsertAllAsync(null, largeDataset).join();
        long singleBatchTime = System.currentTimeMillis() - startTime;
        System.out.println("   ✓ Single batch (1000 records): " + singleBatchTime + "ms");
        
        // Method 2: Multiple smaller batches
        startTime = System.currentTimeMillis();
        int batchSize = 100;
        List<CompletableFuture<Void>> batchFutures = new ArrayList<>();
        
        for (int i = 0; i < largeDataset.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, largeDataset.size());
            List<Artist> batch = largeDataset.subList(i, endIndex);
            
            batchFutures.add(artists.upsertAllAsync(null, batch));
        }
        
        CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0])).join();
        long multiBatchTime = System.currentTimeMillis() - startTime;
        System.out.println("   ✓ Multi-batch (10x100 records): " + multiBatchTime + "ms");
        
        System.out.println("   ✓ Performance comparison: " + 
                          (singleBatchTime < multiBatchTime ? "Single batch faster" : "Multi-batch faster"));
        
        System.out.println("\n2. Async Operation Monitoring:");
        
        AsyncPerformanceMonitor monitor = new AsyncPerformanceMonitor();
        
        // Monitor multiple operations
        for (int i = 0; i < 10; i++) {
            final int operationId = i;
            
            monitor.monitorOperation("get-artist-" + operationId, 
                artists.getAsync(null, new Artist(8000 + operationId, null))
                    .thenApply(artist -> artist != null ? artist.getName() : "Not found"));
        }
        
        // Wait for all monitored operations
        monitor.waitForCompletion();
        monitor.printStatistics();
        
        System.out.println("\n3. Memory-Efficient Streaming Pattern:");
        
        // Process large datasets without loading everything into memory
        CompletableFuture<Integer> streamingProcess = processArtistsInStreams(artists, 8000, 8100);
        
        streamingProcess.thenAccept(processedCount -> 
            System.out.println("   ✓ Streaming processed " + processedCount + " artists")
        ).join();
        
        System.out.println("\n✓ Performance optimization techniques:");
        System.out.println("  - Choose optimal batch sizes for your workload");
        System.out.println("  - Monitor async operation performance");
        System.out.println("  - Use streaming for large datasets");
        System.out.println("  - Optimize resource utilization patterns");
    }
    
    // Helper methods and classes
    
    private static Track createPipelineTrack(Integer trackId, Integer albumId, String name, Integer milliseconds) {
        Track track = new Track();
        track.setTrackId(trackId);
        track.setAlbumId(albumId);
        track.setName(name);
        track.setMediaTypeId(1);
        track.setGenreId(1);
        track.setComposer("King Crimson");
        track.setMilliseconds(milliseconds);
        track.setBytes(milliseconds * 320);
        track.setUnitPrice(new BigDecimal("1.29"));
        return track;
    }
    
    private static <T> CompletableFuture<T> retryWithTimeout(
            Supplier<CompletableFuture<T>> operation, 
            long timeoutMs, 
            int maxRetries) {
        
        CompletableFuture<T> result = new CompletableFuture<>();
        retryOperation(operation, timeoutMs, maxRetries, 0, result);
        return result;
    }
    
    private static <T> void retryOperation(
            Supplier<CompletableFuture<T>> operation,
            long timeoutMs,
            int maxRetries,
            int attempt,
            CompletableFuture<T> result) {
        
        if (attempt > maxRetries) {
            result.completeExceptionally(new RuntimeException("Max retries exceeded"));
            return;
        }
        
        operation.get()
            .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .whenComplete((value, throwable) -> {
                if (throwable == null) {
                    result.complete(value);
                } else {
                    System.out.println("   → Retry " + (attempt + 1) + "/" + (maxRetries + 1) + 
                                     " failed: " + throwable.getMessage());
                    retryOperation(operation, timeoutMs, maxRetries, attempt + 1, result);
                }
            });
    }
    
    private static CompletableFuture<Integer> processArtistsInStreams(
            RecordView<Artist> artists, int startId, int endId) {
        
        CompletableFuture<Integer> result = new CompletableFuture<>();
        processArtistBatch(artists, startId, endId, 0, 10, result);
        return result;
    }
    
    private static void processArtistBatch(
            RecordView<Artist> artists,
            int startId,
            int endId,
            int processedCount,
            int batchSize,
            CompletableFuture<Integer> result) {
        
        if (startId >= endId) {
            result.complete(processedCount);
            return;
        }
        
        int currentEndId = Math.min(startId + batchSize, endId);
        List<Artist> batch = new ArrayList<>();
        
        for (int i = startId; i < currentEndId; i++) {
            batch.add(new Artist(i, null));
        }
        
        artists.getAllAsync(null, batch)
            .thenAccept(retrievedBatch -> {
                int batchProcessed = (int) retrievedBatch.stream()
                    .filter(Objects::nonNull)
                    .count();
                
                // Process next batch
                processArtistBatch(artists, currentEndId, endId, 
                                 processedCount + batchProcessed, batchSize, result);
            })
            .exceptionally(throwable -> {
                result.completeExceptionally(throwable);
                return null;
            });
    }
    
    // Supporting utility classes
    
    static class AsyncErrorHandler {
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger errorCount = new AtomicInteger(0);
        private final Map<String, AtomicInteger> errorTypes = new ConcurrentHashMap<>();
        
        void handleError(String operation, Throwable throwable) {
            errorCount.incrementAndGet();
            String errorType = throwable.getClass().getSimpleName();
            errorTypes.computeIfAbsent(errorType, k -> new AtomicInteger(0)).incrementAndGet();
            System.out.println("   ⚠ Error in " + operation + ": " + throwable.getMessage());
        }
        
        void recordSuccess(String operation) {
            successCount.incrementAndGet();
            System.out.println("   ✓ Success in " + operation);
        }
        
        void printStatistics() {
            System.out.println("   📊 Error Handler Statistics:");
            System.out.println("     Successes: " + successCount.get());
            System.out.println("     Errors: " + errorCount.get());
            errorTypes.forEach((type, count) -> 
                System.out.println("     " + type + ": " + count.get()));
        }
    }
    
    static class AsyncCircuitBreaker {
        private volatile boolean isOpen = false;
        private volatile long lastFailureTime = 0;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final int failureThreshold;
        private final long timeoutMs;
        private final long recoveryTimeoutMs;
        
        AsyncCircuitBreaker(int failureThreshold, long timeoutMs, long recoveryTimeoutMs) {
            this.failureThreshold = failureThreshold;
            this.timeoutMs = timeoutMs;
            this.recoveryTimeoutMs = recoveryTimeoutMs;
        }
        
        <T> CompletableFuture<T> execute(Supplier<CompletableFuture<T>> operation) {
            if (isOpen) {
                long timeSinceFailure = System.currentTimeMillis() - lastFailureTime;
                if (timeSinceFailure < recoveryTimeoutMs) {
                    return CompletableFuture.failedFuture(
                        new RuntimeException("Circuit breaker is open"));
                } else {
                    // Try to recover
                    isOpen = false;
                    failureCount.set(0);
                }
            }
            
            return operation.get()
                .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        int failures = failureCount.incrementAndGet();
                        if (failures >= failureThreshold) {
                            isOpen = true;
                            lastFailureTime = System.currentTimeMillis();
                        }
                    } else {
                        failureCount.set(0);
                        isOpen = false;
                    }
                });
        }
        
        String getState() {
            return isOpen ? "OPEN" : "CLOSED";
        }
    }
    
    static class AsyncRetryHandler {
        private final int maxRetries;
        private final long baseDelayMs;
        private final double backoffMultiplier;
        private final long jitterMs;
        
        AsyncRetryHandler(int maxRetries, long baseDelayMs, double backoffMultiplier, long jitterMs) {
            this.maxRetries = maxRetries;
            this.baseDelayMs = baseDelayMs;
            this.backoffMultiplier = backoffMultiplier;
            this.jitterMs = jitterMs;
        }
        
        <T> CompletableFuture<T> executeWithRetry(Supplier<CompletableFuture<T>> operation) {
            return executeWithRetry(operation, throwable -> true);
        }
        
        <T> CompletableFuture<T> executeWithRetry(
                Supplier<CompletableFuture<T>> operation,
                java.util.function.Predicate<Throwable> shouldRetry) {
            
            CompletableFuture<T> result = new CompletableFuture<>();
            executeWithRetryInternal(operation, shouldRetry, 0, result);
            return result;
        }
        
        private <T> void executeWithRetryInternal(
                Supplier<CompletableFuture<T>> operation,
                java.util.function.Predicate<Throwable> shouldRetry,
                int attempt,
                CompletableFuture<T> result) {
            
            operation.get().whenComplete((value, throwable) -> {
                if (throwable == null) {
                    result.complete(value);
                } else if (attempt >= maxRetries || !shouldRetry.test(throwable)) {
                    result.completeExceptionally(throwable);
                } else {
                    long delay = (long) (baseDelayMs * Math.pow(backoffMultiplier, attempt));
                    delay += (long) (Math.random() * jitterMs);  // Add jitter
                    
                    System.out.println("   ⏳ Retrying in " + delay + "ms (attempt " + (attempt + 1) + ")");
                    
                    CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS)
                        .execute(() -> executeWithRetryInternal(operation, shouldRetry, attempt + 1, result));
                }
            });
        }
    }
    
    static class AsyncResourceManager {
        private final Set<String> activeResources = ConcurrentHashMap.newKeySet();
        
        <T> CompletableFuture<T> executeWithCleanup(
                Supplier<CompletableFuture<T>> operation, 
                long timeoutMs) {
            
            String resourceId = "resource-" + System.currentTimeMillis();
            activeResources.add(resourceId);
            
            return operation.get()
                .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .whenComplete((result, throwable) -> {
                    // Cleanup resource
                    activeResources.remove(resourceId);
                    if (throwable instanceof TimeoutException) {
                        System.out.println("   🧹 Cleaned up resource " + resourceId + " after timeout");
                    } else if (throwable != null) {
                        System.out.println("   🧹 Cleaned up resource " + resourceId + " after error");
                    }
                });
        }
    }
    
    static class AsyncPerformanceMonitor {
        private final List<CompletableFuture<Void>> monitoredOperations = new ArrayList<>();
        private final Map<String, Long> operationTimes = new ConcurrentHashMap<>();
        
        <T> void monitorOperation(String operationName, CompletableFuture<T> operation) {
            long startTime = System.currentTimeMillis();
            
            CompletableFuture<Void> monitoredOp = operation.whenComplete((result, throwable) -> {
                long endTime = System.currentTimeMillis();
                operationTimes.put(operationName, endTime - startTime);
            }).thenApply(result -> null);
            
            monitoredOperations.add(monitoredOp);
        }
        
        void waitForCompletion() {
            CompletableFuture.allOf(monitoredOperations.toArray(new CompletableFuture[0])).join();
        }
        
        void printStatistics() {
            System.out.println("   📊 Performance Statistics:");
            long totalTime = operationTimes.values().stream().mapToLong(Long::longValue).sum();
            long avgTime = totalTime / operationTimes.size();
            long minTime = operationTimes.values().stream().mapToLong(Long::longValue).min().orElse(0);
            long maxTime = operationTimes.values().stream().mapToLong(Long::longValue).max().orElse(0);
            
            System.out.println("     Operations: " + operationTimes.size());
            System.out.println("     Total time: " + totalTime + "ms");
            System.out.println("     Average: " + avgTime + "ms");
            System.out.println("     Min: " + minTime + "ms");
            System.out.println("     Max: " + maxTime + "ms");
        }
    }
}