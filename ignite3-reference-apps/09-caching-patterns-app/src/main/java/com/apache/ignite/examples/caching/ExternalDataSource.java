package com.apache.ignite.examples.caching;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates external data source for caching pattern demonstrations.
 * 
 * In production environments, this would be replaced with actual
 * database connections, web service clients, or other data sources.
 * 
 * This simulation provides realistic response times and occasional
 * failures to demonstrate error handling patterns.
 */
public class ExternalDataSource {
    
    private final Map<Integer, Artist> artistStorage = new ConcurrentHashMap<>();
    private final Map<Integer, Customer> customerStorage = new ConcurrentHashMap<>();
    private final Map<Integer, Track> trackStorage = new ConcurrentHashMap<>();
    private final List<PlayEvent> playEventStorage = Collections.synchronizedList(new ArrayList<>());
    private final List<UserActivity> activityStorage = Collections.synchronizedList(new ArrayList<>());
    
    public ExternalDataSource() {
        initializeSampleData();
    }
    
    /**
     * Loads artist from external data source.
     * 
     * Simulates database lookup with realistic latency.
     */
    public Artist loadArtist(int artistId) {
        simulateLatency(50, 150); // 50-150ms latency
        return artistStorage.get(artistId);
    }
    
    /**
     * Loads multiple artists in batch.
     * 
     * More efficient than individual lookups, simulating
     * database batch operations.
     */
    public Map<Integer, Artist> loadArtists(Collection<Integer> artistIds) {
        simulateLatency(100, 250); // Batch operations take longer
        
        Map<Integer, Artist> results = new HashMap<>();
        for (Integer id : artistIds) {
            Artist artist = artistStorage.get(id);
            if (artist != null) {
                results.put(id, artist);
            }
        }
        return results;
    }
    
    /**
     * Async artist loading for non-blocking operations.
     */
    public CompletableFuture<Artist> loadArtistAsync(int artistId) {
        return CompletableFuture.supplyAsync(() -> loadArtist(artistId));
    }
    
    /**
     * Updates artist information in external store.
     */
    public boolean updateArtist(Artist artist) {
        simulateLatency(75, 200);
        
        if (shouldSimulateFailure()) {
            return false;
        }
        
        artistStorage.put(artist.getArtistId(), artist);
        return true;
    }
    
    /**
     * Loads customer from external data source.
     */
    public Customer loadCustomer(int customerId) {
        simulateLatency(30, 100);
        return customerStorage.get(customerId);
    }
    
    /**
     * Creates new customer in external store.
     */
    public boolean createCustomer(Customer customer) {
        simulateLatency(100, 300);
        
        if (shouldSimulateFailure()) {
            return false;
        }
        
        if (customerStorage.containsKey(customer.getCustomerId())) {
            return false; // Customer already exists
        }
        
        customerStorage.put(customer.getCustomerId(), customer);
        return true;
    }
    
    /**
     * Updates customer information in external store.
     */
    public boolean updateCustomer(Customer customer) {
        simulateLatency(50, 150);
        
        if (shouldSimulateFailure()) {
            return false;
        }
        
        customerStorage.put(customer.getCustomerId(), customer);
        return true;
    }
    
    /**
     * Async customer update for non-blocking operations.
     */
    public CompletableFuture<Boolean> updateCustomerAsync(Customer customer) {
        return CompletableFuture.supplyAsync(() -> updateCustomer(customer));
    }
    
    /**
     * Deletes customer from external store.
     */
    public boolean deleteCustomer(int customerId) {
        simulateLatency(75, 200);
        
        if (shouldSimulateFailure()) {
            return false;
        }
        
        return customerStorage.remove(customerId) != null;
    }
    
    /**
     * Updates customer payment method.
     */
    public boolean updateCustomerPaymentMethod(int customerId, PaymentMethod paymentMethod) {
        simulateLatency(100, 250);
        
        Customer customer = customerStorage.get(customerId);
        if (customer == null) {
            return false;
        }
        
        Customer updated = customer.toBuilder()
            .paymentMethod(paymentMethod)
            .build();
        
        customerStorage.put(customerId, updated);
        return true;
    }
    
    /**
     * Updates customer billing information.
     */
    public boolean updateCustomerBilling(Customer customer) {
        simulateLatency(150, 400); // Billing operations are slower
        
        if (shouldSimulateFailure()) {
            return false;
        }
        
        customerStorage.put(customer.getCustomerId(), customer);
        return true;
    }
    
    /**
     * Loads track from external data source.
     */
    public Track loadTrack(int trackId) {
        simulateLatency(40, 120);
        return trackStorage.get(trackId);
    }
    
    /**
     * Loads multiple tracks in batch.
     */
    public Map<Integer, Track> loadTracks(Collection<Integer> trackIds) {
        simulateLatency(80, 200);
        
        Map<Integer, Track> results = new HashMap<>();
        for (Integer id : trackIds) {
            Track track = trackStorage.get(id);
            if (track != null) {
                results.put(id, track);
            }
        }
        return results;
    }
    
    /**
     * Records play event directly to external analytics system.
     */
    public boolean recordPlayEventDirect(int customerId, int trackId) {
        simulateLatency(25, 75);
        
        PlayEvent event = PlayEvent.builder()
            .customerId(customerId)
            .trackId(trackId)
            .build();
        
        playEventStorage.add(event);
        return true;
    }
    
    /**
     * Batch inserts play events to external analytics system.
     */
    public boolean batchInsertPlayEvents(List<PlayEvent> playEvents) {
        simulateLatency(200, 500); // Batch operations take longer
        
        if (shouldSimulateFailure()) {
            return false;
        }
        
        playEventStorage.addAll(playEvents);
        return true;
    }
    
    /**
     * Processes user activity analytics.
     */
    public boolean processUserActivityAnalytics(UserActivity activity) {
        simulateLatency(50, 150);
        
        if (shouldSimulateFailure()) {
            return false;
        }
        
        activityStorage.add(activity);
        return true;
    }
    
    /**
     * Batch processes user activities.
     */
    public boolean batchProcessActivities(List<UserActivity> activities) {
        simulateLatency(300, 700);
        
        if (shouldSimulateFailure()) {
            return false;
        }
        
        activityStorage.addAll(activities);
        return true;
    }
    
    /**
     * Gets analytics data count for monitoring.
     */
    public Map<String, Integer> getAnalyticsStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("play_events", playEventStorage.size());
        stats.put("user_activities", activityStorage.size());
        stats.put("customers", customerStorage.size());
        stats.put("artists", artistStorage.size());
        stats.put("tracks", trackStorage.size());
        return stats;
    }
    
    /**
     * Cleanup resources.
     */
    public void cleanup() {
        // In a real implementation, this would close database connections,
        // HTTP clients, or other resources
        System.out.println("External data source cleanup completed");
    }
    
    private void initializeSampleData() {
        // Initialize sample artists
        for (int i = 1; i <= 20; i++) {
            Artist artist = Artist.builder()
                .artistId(i)
                .name("Artist " + i)
                .genre(getRandomGenre())
                .country(getRandomCountry())
                .albumCount(ThreadLocalRandom.current().nextInt(1, 10))
                .build();
            artistStorage.put(i, artist);
        }
        
        // Initialize sample tracks
        for (int i = 5001; i <= 5100; i++) {
            Track track = Track.builder()
                .trackId(i)
                .name("Track " + i)
                .artistId(1 + (i % 20))
                .albumId(1000 + (i % 50))
                .genre(getRandomGenre())
                .durationSeconds(ThreadLocalRandom.current().nextInt(120, 300))
                .streamingUrl("https://stream.example.com/track/" + i)
                .build();
            trackStorage.put(i, track);
        }
        
        System.out.printf("Initialized external data source with %d artists and %d tracks%n", 
            artistStorage.size(), trackStorage.size());
    }
    
    private void simulateLatency(int minMs, int maxMs) {
        try {
            int latency = ThreadLocalRandom.current().nextInt(minMs, maxMs + 1);
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private boolean shouldSimulateFailure() {
        // 5% chance of simulated failure
        return ThreadLocalRandom.current().nextDouble() < 0.05;
    }
    
    private String getRandomGenre() {
        String[] genres = {"Rock", "Pop", "Jazz", "Classical", "Electronic", "Hip-Hop", "Country", "Blues"};
        return genres[ThreadLocalRandom.current().nextInt(genres.length)];
    }
    
    private String getRandomCountry() {
        String[] countries = {"USA", "UK", "Canada", "Germany", "France", "Japan", "Australia", "Brazil"};
        return countries[ThreadLocalRandom.current().nextInt(countries.length)];
    }
}