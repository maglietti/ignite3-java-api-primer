package com.apache.ignite.examples.caching;

import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.tx.Transaction;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Demonstrates cache-aside pattern implementation using Ignite 3 KeyValueView API.
 * 
 * Music streaming services use cache-aside for catalog data where:
 * - Reads significantly outnumber writes
 * - Applications control cache population and invalidation
 * - Cache misses are acceptable for infrequently accessed data
 * 
 * This pattern puts applications in control of cache management. When users browse
 * artists, the application first checks Ignite for cached data, then loads from
 * the primary data store on cache misses.
 * 
 * Key operations demonstrated:
 * - Cache hit/miss handling
 * - Batch operations for performance
 * - Cache invalidation strategies
 * - Async operations for improved throughput
 * 
 * @see KeyValueView for cache operations
 * @see ExternalDataSource for primary data store simulation
 */
public class CacheAsidePatternDemo {
    
    private final KeyValueView<Integer, Artist> artistCache;
    private final ExternalDataSource externalDataSource;
    
    /**
     * Constructs cache-aside demo with required dependencies.
     * 
     * @param artistCache Ignite KeyValueView for artist caching
     * @param externalDataSource Primary data store for cache misses
     */
    public CacheAsidePatternDemo(KeyValueView<Integer, Artist> artistCache, 
                                ExternalDataSource externalDataSource) {
        this.artistCache = artistCache;
        this.externalDataSource = externalDataSource;
    }
    
    /**
     * Retrieves artist information using cache-aside pattern.
     * 
     * Pattern implementation:
     * 1. Check cache first (Ignite KeyValueView)
     * 2. On cache miss, load from external source
     * 3. Populate cache with loaded data
     * 4. Return data to caller
     * 
     * This approach provides fast access to frequently requested artists
     * while handling cache misses gracefully.
     * 
     * @param artistId Artist identifier to retrieve
     * @return Artist information or null if not found
     */
    public Artist getArtist(int artistId) {
        System.out.printf("Retrieving artist %d using cache-aside pattern%n", artistId);
        
        // Step 1: Check cache first
        Artist cached = artistCache.get(null, artistId);
        if (cached != null) {
            System.out.printf("Cache HIT for artist %d: %s%n", artistId, cached.getName());
            return cached;
        }
        
        System.out.printf("Cache MISS for artist %d, loading from external source%n", artistId);
        
        // Step 2: Cache miss - load from external data source
        Artist artist = externalDataSource.loadArtist(artistId);
        if (artist == null) {
            System.out.printf("Artist %d not found in external source%n", artistId);
            return null;
        }
        
        // Step 3: Populate cache for future requests
        artistCache.put(null, artistId, artist);
        System.out.printf("Cached artist %d: %s%n", artistId, artist.getName());
        
        return artist;
    }
    
    /**
     * Batch loading optimization for popular artists.
     * 
     * Reduces external data source load by batching requests
     * and using Ignite's bulk operations for efficiency.
     * 
     * This optimization is particularly effective for scenarios like:
     * - Loading popular playlists with multiple artists
     * - Generating recommendation lists
     * - Batch processing workflows
     * 
     * @param artistIds Collection of artist IDs to retrieve
     * @return Map of artist ID to Artist object
     */
    public Map<Integer, Artist> getPopularArtists(Collection<Integer> artistIds) {
        System.out.printf("Batch loading %d artists using cache-aside pattern%n", artistIds.size());
        
        // Check cache for all requested artists using bulk operation
        Map<Integer, Artist> cachedResults = artistCache.getAll(null, artistIds);
        System.out.printf("Cache provided %d/%d artists%n", cachedResults.size(), artistIds.size());
        
        // Identify cache misses
        Set<Integer> missedIds = artistIds.stream()
            .filter(id -> !cachedResults.containsKey(id))
            .collect(Collectors.toSet());
        
        if (missedIds.isEmpty()) {
            System.out.println("All artists found in cache - no external loading needed");
            return cachedResults;
        }
        
        System.out.printf("Loading %d missed artists from external source%n", missedIds.size());
        
        // Load missing artists from external source
        Map<Integer, Artist> loadedArtists = externalDataSource.loadArtists(missedIds);
        
        // Cache the loaded artists using bulk operation
        if (!loadedArtists.isEmpty()) {
            artistCache.putAll(null, loadedArtists);
            System.out.printf("Cached %d newly loaded artists%n", loadedArtists.size());
        }
        
        // Combine cached and loaded results
        Map<Integer, Artist> results = new HashMap<>(cachedResults);
        results.putAll(loadedArtists);
        
        System.out.printf("Returning %d total artists%n", results.size());
        return results;
    }
    
    /**
     * Cache invalidation for updated artist information.
     * 
     * Applications must handle cache invalidation when
     * external data changes to prevent stale data.
     * 
     * Two strategies demonstrated:
     * - Remove from cache (lazy loading on next access)
     * - Update cache with new values (immediate consistency)
     * 
     * @param artist Updated artist information
     */
    public void updateArtist(Artist artist) {
        System.out.printf("Updating artist %d: %s%n", artist.getArtistId(), artist.getName());
        
        // Update external data source first
        boolean updated = externalDataSource.updateArtist(artist);
        if (!updated) {
            throw new RuntimeException("Failed to update artist in external source");
        }
        
        // Strategy 1: Invalidate cache entry (lazy loading)
        // artistCache.remove(null, artist.getArtistId());
        // System.out.printf("Invalidated cache entry for artist %d%n", artist.getArtistId());
        
        // Strategy 2: Update cache with new values (immediate consistency)
        artistCache.put(null, artist.getArtistId(), artist);
        System.out.printf("Updated cache entry for artist %d%n", artist.getArtistId());
    }
    
    /**
     * Asynchronous cache-aside for non-blocking operations.
     * 
     * Improves response times for concurrent requests by
     * using Ignite's async API capabilities.
     * 
     * This approach is particularly beneficial for:
     * - High-concurrency web applications
     * - Non-blocking reactive applications
     * - Batch processing with parallel execution
     * 
     * @param artistId Artist identifier to retrieve
     * @return CompletableFuture containing artist information
     */
    public CompletableFuture<Artist> getArtistAsync(int artistId) {
        System.out.printf("Async retrieving artist %d using cache-aside pattern%n", artistId);
        
        return artistCache.getAsync(null, artistId)
            .thenCompose(cached -> {
                if (cached != null) {
                    System.out.printf("Async cache HIT for artist %d: %s%n", artistId, cached.getName());
                    return CompletableFuture.completedFuture(cached);
                }
                
                System.out.printf("Async cache MISS for artist %d, loading from external source%n", artistId);
                
                // Load from external source asynchronously
                return externalDataSource.loadArtistAsync(artistId)
                    .thenCompose(artist -> {
                        if (artist == null) {
                            System.out.printf("Artist %d not found in external source%n", artistId);
                            return CompletableFuture.completedFuture(null);
                        }
                        
                        // Cache the loaded artist
                        return artistCache.putAsync(null, artistId, artist)
                            .thenApply(ignored -> {
                                System.out.printf("Async cached artist %d: %s%n", artistId, artist.getName());
                                return artist;
                            });
                    });
            });
    }
    
    /**
     * Retrieves track information using cache-aside pattern.
     * 
     * Demonstrates cache-aside pattern for different entity types,
     * showing how the same pattern applies across various data models.
     * 
     * @param trackId Track identifier to retrieve
     * @return Track information or null if not found
     */
    public Track getTrack(int trackId) {
        System.out.printf("Retrieving track %d using cache-aside pattern%n", trackId);
        
        // Note: In a real implementation, you would have a separate KeyValueView for tracks
        // For this demo, we'll simulate track retrieval using the external data source
        
        // Check if we have track caching enabled (this would be a separate cache)
        Track track = externalDataSource.loadTrack(trackId);
        if (track != null) {
            System.out.printf("Loaded track %d: %s%n", trackId, track.getName());
        } else {
            System.out.printf("Track %d not found%n", trackId);
        }
        
        return track;
    }
    
    /**
     * Batch track loading for playlist operations.
     * 
     * Demonstrates how cache-aside pattern scales to related entity operations,
     * such as loading all tracks for a playlist.
     * 
     * @param trackIds Collection of track IDs to retrieve
     * @return Map of track ID to Track object
     */
    public Map<Integer, Track> getTracksForPlaylist(Collection<Integer> trackIds) {
        System.out.printf("Loading %d tracks for playlist using cache-aside pattern%n", trackIds.size());
        
        // In a real implementation, this would use a tracks cache
        Map<Integer, Track> tracks = externalDataSource.loadTracks(trackIds);
        System.out.printf("Loaded %d tracks for playlist%n", tracks.size());
        
        return tracks;
    }
    
    /**
     * Cache statistics and monitoring.
     * 
     * Provides visibility into cache performance for monitoring
     * and optimization purposes.
     * 
     * @return Map containing cache performance metrics
     */
    public Map<String, Object> getCacheStatistics() {
        // Note: In a real implementation, you would gather actual cache statistics
        // This is a simulation for demonstration purposes
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("cache_hits", 150);
        stats.put("cache_misses", 45);
        stats.put("hit_ratio", 0.77);
        stats.put("cached_entries", 120);
        
        System.out.println("Cache Statistics:");
        stats.forEach((key, value) -> System.out.printf("  %s: %s%n", key, value));
        
        return stats;
    }
    
    /**
     * Cache warming operation for application startup.
     * 
     * Pre-loads frequently accessed artists into cache to improve
     * initial response times after application startup.
     * 
     * @param popularArtistIds List of popular artist IDs to pre-load
     */
    public void warmCache(List<Integer> popularArtistIds) {
        System.out.printf("Warming cache with %d popular artists%n", popularArtistIds.size());
        
        // Load popular artists in batches to avoid overwhelming external source
        int batchSize = 50;
        for (int i = 0; i < popularArtistIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, popularArtistIds.size());
            List<Integer> batch = popularArtistIds.subList(i, endIndex);
            
            System.out.printf("Warming cache batch %d-%d%n", i + 1, endIndex);
            getPopularArtists(batch);
        }
        
        System.out.println("Cache warming completed");
    }
}