package com.apache.ignite.examples.compute;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.JobDescriptor;
import org.apache.ignite.compute.JobExecutionContext;
import org.apache.ignite.compute.JobTarget;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Demonstrates basic compute job submission patterns using the music store dataset.
 * 
 * This application showcases fundamental Ignite 3 Compute API usage:
 * - Simple job creation and execution
 * - Job targeting strategies (any node vs specific nodes)
 * - Basic result handling
 * - Integration with SQL API within compute jobs
 * 
 * The examples use music store data to demonstrate real-world job execution scenarios
 * including track analysis and artist information processing.
 * 
 * Educational Concepts:
 * - ComputeJob interface implementation
 * - JobDescriptor creation and configuration  
 * - JobTarget selection for optimal load distribution
 * - Accessing Ignite APIs within job execution context
 * 
 * @since 1.0.0
 */
public class BasicComputeDemo {
    
    private static final String IGNITE_URL = "http://localhost:10800";
    
    public static void main(String[] args) {
        System.out.println("Basic Compute Demo - Music Store Analysis");
        System.out.println("========================================");
        
        try (IgniteClient ignite = IgniteClient.builder()
                .addresses(IGNITE_URL)
                .build()) {
            
            // Demonstrate basic job patterns
            demonstrateSimpleTrackAnalysis(ignite);
            demonstrateArtistNameProcessing(ignite);
            demonstrateAlbumCountCalculation(ignite);
            
        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Demonstrates a simple compute job that calculates total track duration.
     * This job uses SQL within the compute context to access track data.
     */
    private static void demonstrateSimpleTrackAnalysis(IgniteClient ignite) {
        System.out.println("\n1. Simple Track Duration Analysis");
        System.out.println("   Computing total duration for selected tracks...");
        
        try {
            // Create job descriptor for track duration analysis
            JobDescriptor<Integer, Double> job = JobDescriptor.builder(TrackDurationJob.class).build();
            
            // Execute on any available node
            JobTarget target = JobTarget.anyNode(ignite.clusterNodes());
            
            // Submit job with track count parameter
            Integer trackCount = 5;
            Double totalDuration = ignite.compute().execute(target, job, trackCount);
            
            System.out.println("   → Total duration for first " + trackCount + " tracks: " 
                + String.format("%.2f", totalDuration) + " minutes");
            
        } catch (Exception e) {
            System.err.println("   ✗ Track analysis failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates string processing in compute jobs that doesn't require data access.
     * This shows CPU-intensive work that can run on any node.
     */
    private static void demonstrateArtistNameProcessing(IgniteClient ignite) {
        System.out.println("\n2. Artist Name Processing (CPU-intensive)");
        System.out.println("   Processing artist names without data access...");
        
        try {
            // Create job for pure computational work
            JobDescriptor<String, String> job = JobDescriptor.builder(NameProcessingJob.class).build();
            
            // Execute on any node since no data access is needed
            JobTarget target = JobTarget.anyNode(ignite.clusterNodes());
            
            String artistName = "The Beatles";
            String processedName = ignite.compute().execute(target, job, artistName);
            
            System.out.println("   → Original: " + artistName);
            System.out.println("   → Processed: " + processedName);
            
        } catch (Exception e) {
            System.err.println("   ✗ Name processing failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates a job that accesses multiple tables and returns aggregate results.
     * Shows more complex SQL operations within compute jobs.
     */
    private static void demonstrateAlbumCountCalculation(IgniteClient ignite) {
        System.out.println("\n3. Album Count Analysis");
        System.out.println("   Calculating album statistics across the catalog...");
        
        try {
            // Create job for album analysis
            JobDescriptor<Void, AlbumStats> job = JobDescriptor.builder(AlbumStatsJob.class).build();
            
            // Execute on any available node
            JobTarget target = JobTarget.anyNode(ignite.clusterNodes());
            
            AlbumStats stats = ignite.compute().execute(target, job, null);
            
            System.out.println("   → Total albums: " + stats.getTotalAlbums());
            System.out.println("   → Average tracks per album: " + String.format("%.1f", stats.getAverageTracksPerAlbum()));
            System.out.println("   → Most productive artist: " + stats.getMostProductiveArtist());
            
        } catch (Exception e) {
            System.err.println("   ✗ Album analysis failed: " + e.getMessage());
        }
    }
    
    /**
     * Job that calculates total duration for a specified number of tracks.
     * Demonstrates basic SQL access within compute jobs.
     */
    public static class TrackDurationJob implements ComputeJob<Integer, Double> {
        @Override
        public CompletableFuture<Double> executeAsync(JobExecutionContext context, Integer trackCount) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    IgniteSql sql = context.ignite().sql();
                    
                    // Query first N tracks and calculate total duration
                    try (ResultSet<SqlRow> rs = sql.execute(null, 
                        "SELECT Milliseconds FROM Track ORDER BY TrackId LIMIT ?", trackCount)) {
                        
                        double totalDuration = 0.0;
                        while (rs.hasNext()) {
                            SqlRow row = rs.next();
                            // Convert milliseconds to minutes
                            totalDuration += row.doubleValue("Milliseconds") / 60000.0;
                        }
                        
                        return totalDuration;
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Track duration calculation failed", e);
                }
            });
        }
    }
    
    /**
     * Job that performs CPU-intensive string processing without data access.
     * Demonstrates compute work that can run efficiently on any node.
     */
    public static class NameProcessingJob implements ComputeJob<String, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, String artistName) {
            return CompletableFuture.supplyAsync(() -> {
                // Simulate CPU-intensive name processing
                StringBuilder processed = new StringBuilder();
                
                // Convert to title case and add formatting
                String[] words = artistName.toLowerCase().split("\\s+");
                for (int i = 0; i < words.length; i++) {
                    if (i > 0) processed.append(" ");
                    
                    String word = words[i];
                    if (word.length() > 0) {
                        processed.append(Character.toUpperCase(word.charAt(0)))
                                .append(word.substring(1));
                    }
                }
                
                // Add analysis metadata
                int wordCount = words.length;
                int totalChars = artistName.replaceAll("\\s+", "").length();
                
                return String.format("%s [%d words, %d chars]", 
                    processed.toString(), wordCount, totalChars);
            });
        }
    }
    
    /**
     * Job that analyzes album statistics across the music catalog.
     * Demonstrates complex SQL operations and result aggregation in compute jobs.
     */
    public static class AlbumStatsJob implements ComputeJob<Void, AlbumStats> {
        @Override
        public CompletableFuture<AlbumStats> executeAsync(JobExecutionContext context, Void input) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    IgniteSql sql = context.ignite().sql();
                    
                    // Calculate album statistics
                    int totalAlbums = 0;
                    double averageTracksPerAlbum = 0.0;
                    String mostProductiveArtist = "Unknown";
                    
                    // Get total album count
                    try (ResultSet<SqlRow> rs = sql.execute(null, "SELECT COUNT(*) as AlbumCount FROM Album")) {
                        if (rs.hasNext()) {
                            totalAlbums = rs.next().intValue("AlbumCount");
                        }
                    }
                    
                    // Get average tracks per album
                    try (ResultSet<SqlRow> rs = sql.execute(null, 
                        "SELECT AVG(TrackCount) as AvgTracks FROM (" +
                        "  SELECT al.AlbumId, COUNT(t.TrackId) as TrackCount " +
                        "  FROM Album al LEFT JOIN Track t ON al.AlbumId = t.AlbumId " +
                        "  GROUP BY al.AlbumId" +
                        ")")) {
                        if (rs.hasNext()) {
                            averageTracksPerAlbum = rs.next().doubleValue("AvgTracks");
                        }
                    }
                    
                    // Find most productive artist (by album count)
                    try (ResultSet<SqlRow> rs = sql.execute(null, 
                        "SELECT ar.Name, COUNT(al.AlbumId) as AlbumCount " +
                        "FROM Artist ar LEFT JOIN Album al ON ar.ArtistId = al.ArtistId " +
                        "GROUP BY ar.ArtistId, ar.Name " +
                        "ORDER BY AlbumCount DESC LIMIT 1")) {
                        if (rs.hasNext()) {
                            SqlRow row = rs.next();
                            mostProductiveArtist = row.stringValue("Name") + 
                                " (" + row.intValue("AlbumCount") + " albums)";
                        }
                    }
                    
                    return new AlbumStats(totalAlbums, averageTracksPerAlbum, mostProductiveArtist);
                    
                } catch (Exception e) {
                    throw new RuntimeException("Album statistics calculation failed", e);
                }
            });
        }
    }
    
    /**
     * Data class representing album statistics results.
     * Demonstrates structured result objects from compute jobs.
     */
    public static class AlbumStats {
        private final int totalAlbums;
        private final double averageTracksPerAlbum;
        private final String mostProductiveArtist;
        
        public AlbumStats(int totalAlbums, double averageTracksPerAlbum, String mostProductiveArtist) {
            this.totalAlbums = totalAlbums;
            this.averageTracksPerAlbum = averageTracksPerAlbum;
            this.mostProductiveArtist = mostProductiveArtist;
        }
        
        public int getTotalAlbums() { return totalAlbums; }
        public double getAverageTracksPerAlbum() { return averageTracksPerAlbum; }
        public String getMostProductiveArtist() { return mostProductiveArtist; }
    }
}