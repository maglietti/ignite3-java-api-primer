package com.apache.ignite.examples.compute;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.compute.BroadcastJobTarget;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.JobDescriptor;
import org.apache.ignite.compute.JobExecutionContext;
import org.apache.ignite.compute.JobTarget;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.Tuple;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Demonstrates data colocation patterns in compute jobs using the music store dataset.
 * 
 * This application showcases advanced Ignite 3 Compute API features focusing on data locality:
 * - Colocated job execution for performance optimization
 * - Broadcast jobs across table partitions
 * - Data-aware job targeting strategies
 * - Complex analysis patterns with local data access
 * 
 * The examples demonstrate how executing code where data resides dramatically improves
 * performance by eliminating network overhead for data access.
 * 
 * Educational Concepts:
 * - JobTarget.colocated() for data-local execution
 * - BroadcastJobTarget.table() for partition-aware broadcasting
 * - Colocation benefits for performance and scalability
 * - MapReduce patterns with data locality
 * - Result aggregation from distributed computations
 * 
 * @since 1.0.0
 */
public class ColocationComputeDemo {
    
    private static final String IGNITE_URL = "http://localhost:10800";
    
    public static void main(String[] args) {
        System.out.println("Colocation Compute Demo - Data-Local Processing");
        System.out.println("===============================================");
        
        try (IgniteClient ignite = IgniteClient.builder()
                .addresses(IGNITE_URL)
                .build()) {
            
            // Demonstrate colocation patterns
            demonstrateColocatedArtistAnalysis(ignite);
            demonstrateBroadcastTrackStatistics(ignite);
            demonstrateCustomerPurchaseAnalysis(ignite);
            demonstrateGenreDistributionMapReduce(ignite);
            
        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Demonstrates colocated job execution for a specific artist.
     * The job runs on the node where the artist's data is stored.
     */
    private static void demonstrateColocatedArtistAnalysis(IgniteClient ignite) {
        System.out.println("\n1. Colocated Artist Analysis");
        System.out.println("   Processing artist data where it's stored...");
        
        try {
            // Create job descriptor for artist analysis
            JobDescriptor<Integer, ArtistReport> job = JobDescriptor.builder(ArtistAnalysisJob.class).build();
            
            // Execute on node where Artist with ArtistId=1 is colocated
            Integer artistId = 1;
            JobTarget colocatedTarget = JobTarget.colocated("Artist", 
                Tuple.create().set("ArtistId", artistId));
            
            ArtistReport report = ignite.compute().execute(colocatedTarget, job, artistId);
            
            System.out.println("   → Artist: " + report.getArtistName());
            System.out.println("   → Album count: " + report.getAlbumCount());
            System.out.println("   → Total tracks: " + report.getTotalTracks());
            System.out.println("   → Average album length: " + String.format("%.1f", report.getAverageAlbumLength()) + " minutes");
            System.out.println("   → Revenue generated: $" + String.format("%.2f", report.getTotalRevenue()));
            
        } catch (Exception e) {
            System.err.println("   ✗ Colocated artist analysis failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates broadcast execution across all table partitions.
     * Each node processes its local Track data and returns statistics.
     */
    private static void demonstrateBroadcastTrackStatistics(IgniteClient ignite) {
        System.out.println("\n2. Broadcast Track Statistics");
        System.out.println("   Gathering statistics from all nodes...");
        
        try {
            // Create job for local track statistics
            JobDescriptor<Void, TrackStatistics> job = JobDescriptor.builder(LocalTrackStatsJob.class).build();
            
            // Broadcast to all nodes that hold Track table data
            BroadcastJobTarget broadcastTarget = BroadcastJobTarget.table("Track");
            Collection<TrackStatistics> nodeStats = ignite.compute().execute(broadcastTarget, job, null);
            
            // Aggregate results from all nodes
            TrackStatistics globalStats = aggregateTrackStatistics(nodeStats);
            
            System.out.println("   → Total tracks across cluster: " + globalStats.getTrackCount());
            System.out.println("   → Average track duration: " + String.format("%.1f", globalStats.getAverageDuration()) + " seconds");
            System.out.println("   → Shortest track: " + String.format("%.1f", globalStats.getMinDuration()) + " seconds");
            System.out.println("   → Longest track: " + String.format("%.1f", globalStats.getMaxDuration()) + " seconds");
            System.out.println("   → Nodes processed: " + nodeStats.size());
            
        } catch (Exception e) {
            System.err.println("   ✗ Broadcast track statistics failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates customer purchase analysis with colocated execution.
     * Analyzes purchases for a specific customer where their data is stored.
     */
    private static void demonstrateCustomerPurchaseAnalysis(IgniteClient ignite) {
        System.out.println("\n3. Colocated Customer Purchase Analysis");
        System.out.println("   Analyzing customer purchases locally...");
        
        try {
            // Create job for customer purchase analysis
            JobDescriptor<Integer, CustomerPurchaseReport> job = 
                JobDescriptor.builder(CustomerPurchaseAnalysisJob.class).build();
            
            // Execute on node where Customer data is colocated
            Integer customerId = 1;
            JobTarget colocatedTarget = JobTarget.colocated("Customer", 
                Tuple.create().set("CustomerId", customerId));
            
            CustomerPurchaseReport report = ignite.compute().execute(colocatedTarget, job, customerId);
            
            System.out.println("   → Customer: " + report.getCustomerName());
            System.out.println("   → Total purchases: " + report.getPurchaseCount());
            System.out.println("   → Total spent: $" + String.format("%.2f", report.getTotalSpent()));
            System.out.println("   → Favorite genre: " + report.getFavoriteGenre());
            System.out.println("   → Average purchase: $" + String.format("%.2f", report.getAveragePurchase()));
            
        } catch (Exception e) {
            System.err.println("   ✗ Customer purchase analysis failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates MapReduce pattern with genre distribution analysis.
     * Map phase runs on each node, reduce phase aggregates results.
     */
    private static void demonstrateGenreDistributionMapReduce(IgniteClient ignite) {
        System.out.println("\n4. Genre Distribution MapReduce");
        System.out.println("   Analyzing genre distribution across the cluster...");
        
        try {
            // Map phase: analyze genres on each node
            JobDescriptor<Void, Map<String, GenreStats>> mapJob = 
                JobDescriptor.builder(GenreAnalysisMapJob.class).build();
            
            BroadcastJobTarget broadcastTarget = BroadcastJobTarget.table("Track");
            Collection<Map<String, GenreStats>> mapResults = ignite.compute().execute(broadcastTarget, mapJob, null);
            
            // Reduce phase: aggregate results
            Map<String, GenreStats> globalGenreStats = new HashMap<>();
            for (Map<String, GenreStats> nodeResult : mapResults) {
                nodeResult.forEach((genre, stats) -> 
                    globalGenreStats.merge(genre, stats, GenreStats::merge));
            }
            
            // Display top genres by track count
            System.out.println("   → Top genres by track count:");
            globalGenreStats.entrySet().stream()
                .sorted(Map.Entry.<String, GenreStats>comparingByValue(
                    Comparator.comparing(GenreStats::getTrackCount)).reversed())
                .limit(5)
                .forEach(entry -> System.out.println("     • " + entry.getKey() + 
                    ": " + entry.getValue().getTrackCount() + " tracks, " +
                    String.format("%.1f", entry.getValue().getAverageDuration()) + "s avg"));
            
        } catch (Exception e) {
            System.err.println("   ✗ Genre distribution analysis failed: " + e.getMessage());
        }
    }
    
    /**
     * Aggregates track statistics from multiple nodes into global statistics.
     */
    private static TrackStatistics aggregateTrackStatistics(Collection<TrackStatistics> nodeStats) {
        int totalTracks = 0;
        double totalDuration = 0.0;
        double minDuration = Double.MAX_VALUE;
        double maxDuration = Double.MIN_VALUE;
        
        for (TrackStatistics stats : nodeStats) {
            totalTracks += stats.getTrackCount();
            totalDuration += stats.getTotalDuration();
            minDuration = Math.min(minDuration, stats.getMinDuration());
            maxDuration = Math.max(maxDuration, stats.getMaxDuration());
        }
        
        double averageDuration = totalTracks > 0 ? totalDuration / totalTracks : 0.0;
        
        return new TrackStatistics(totalTracks, totalDuration, averageDuration, minDuration, maxDuration);
    }
    
    /**
     * Job that analyzes a specific artist's catalog and sales data.
     * Runs colocated with artist data for optimal performance.
     */
    public static class ArtistAnalysisJob implements ComputeJob<Integer, ArtistReport> {
        @Override
        public CompletableFuture<ArtistReport> executeAsync(JobExecutionContext context, Integer artistId) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    IgniteSql sql = context.ignite().sql();
                    
                    String artistName = "Unknown Artist";
                    int albumCount = 0;
                    int totalTracks = 0;
                    double averageAlbumLength = 0.0;
                    double totalRevenue = 0.0;
                    
                    // Get artist name
                    try (ResultSet<SqlRow> rs = sql.execute(null, 
                        "SELECT Name FROM Artist WHERE ArtistId = ?", artistId)) {
                        if (rs.hasNext()) {
                            artistName = rs.next().stringValue("Name");
                        }
                    }
                    
                    // Get album and track statistics
                    try (ResultSet<SqlRow> rs = sql.execute(null, """
                        SELECT COUNT(DISTINCT al.AlbumId) as AlbumCount,
                               COUNT(t.TrackId) as TrackCount,
                               AVG(t.Milliseconds) as AvgLength
                        FROM Artist ar
                        LEFT JOIN Album al ON ar.ArtistId = al.ArtistId
                        LEFT JOIN Track t ON al.AlbumId = t.AlbumId
                        WHERE ar.ArtistId = ?
                        """, artistId)) {
                        if (rs.hasNext()) {
                            SqlRow row = rs.next();
                            albumCount = row.intValue("AlbumCount");
                            totalTracks = row.intValue("TrackCount");
                            averageAlbumLength = row.doubleValue("AvgLength") / 60000.0; // Convert to minutes
                        }
                    }
                    
                    // Calculate revenue from sales
                    try (ResultSet<SqlRow> rs = sql.execute(null, """
                        SELECT SUM(il.UnitPrice * il.Quantity) as Revenue
                        FROM Artist ar
                        JOIN Album al ON ar.ArtistId = al.ArtistId
                        JOIN Track t ON al.AlbumId = t.AlbumId
                        JOIN InvoiceLine il ON t.TrackId = il.TrackId
                        WHERE ar.ArtistId = ?
                        """, artistId)) {
                        if (rs.hasNext()) {
                            SqlRow row = rs.next();
                            Object revenueObj = row.value("Revenue");
                            if (revenueObj != null) {
                                totalRevenue = ((Number) revenueObj).doubleValue();
                            }
                        }
                    }
                    
                    return new ArtistReport(artistId, artistName, albumCount, totalTracks, 
                        averageAlbumLength, totalRevenue);
                    
                } catch (Exception e) {
                    throw new RuntimeException("Artist analysis failed", e);
                }
            });
        }
    }
    
    /**
     * Job that calculates local track statistics on each node.
     * Part of broadcast execution pattern.
     */
    public static class LocalTrackStatsJob implements ComputeJob<Void, TrackStatistics> {
        @Override
        public CompletableFuture<TrackStatistics> executeAsync(JobExecutionContext context, Void input) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    IgniteSql sql = context.ignite().sql();
                    
                    // Calculate statistics for local track data
                    try (ResultSet<SqlRow> rs = sql.execute(null, """
                        SELECT COUNT(*) as TrackCount,
                               SUM(Milliseconds) as TotalDuration,
                               AVG(Milliseconds) as AvgDuration,
                               MIN(Milliseconds) as MinDuration,
                               MAX(Milliseconds) as MaxDuration
                        FROM Track
                        """)) {
                        
                        if (rs.hasNext()) {
                            SqlRow row = rs.next();
                            int trackCount = row.intValue("TrackCount");
                            double totalDuration = row.doubleValue("TotalDuration") / 1000.0; // Convert to seconds
                            double avgDuration = row.doubleValue("AvgDuration") / 1000.0;
                            double minDuration = row.doubleValue("MinDuration") / 1000.0;
                            double maxDuration = row.doubleValue("MaxDuration") / 1000.0;
                            
                            return new TrackStatistics(trackCount, totalDuration, avgDuration, minDuration, maxDuration);
                        }
                    }
                    
                    return new TrackStatistics(0, 0.0, 0.0, 0.0, 0.0);
                    
                } catch (Exception e) {
                    throw new RuntimeException("Local track statistics calculation failed", e);
                }
            });
        }
    }
    
    /**
     * Job that analyzes customer purchase patterns with colocated execution.
     */
    public static class CustomerPurchaseAnalysisJob implements ComputeJob<Integer, CustomerPurchaseReport> {
        @Override
        public CompletableFuture<CustomerPurchaseReport> executeAsync(JobExecutionContext context, Integer customerId) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    IgniteSql sql = context.ignite().sql();
                    
                    String customerName = "Unknown Customer";
                    int purchaseCount = 0;
                    double totalSpent = 0.0;
                    String favoriteGenre = "Unknown";
                    
                    // Get customer name
                    try (ResultSet<SqlRow> rs = sql.execute(null, 
                        "SELECT FirstName, LastName FROM Customer WHERE CustomerId = ?", customerId)) {
                        if (rs.hasNext()) {
                            SqlRow row = rs.next();
                            customerName = row.stringValue("FirstName") + " " + row.stringValue("LastName");
                        }
                    }
                    
                    // Get purchase statistics
                    try (ResultSet<SqlRow> rs = sql.execute(null, """
                        SELECT COUNT(DISTINCT i.InvoiceId) as PurchaseCount,
                               SUM(il.UnitPrice * il.Quantity) as TotalSpent
                        FROM Customer c
                        JOIN Invoice i ON c.CustomerId = i.CustomerId
                        JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId
                        WHERE c.CustomerId = ?
                        """, customerId)) {
                        if (rs.hasNext()) {
                            SqlRow row = rs.next();
                            purchaseCount = row.intValue("PurchaseCount");
                            Object spentObj = row.value("TotalSpent");
                            if (spentObj != null) {
                                totalSpent = ((Number) spentObj).doubleValue();
                            }
                        }
                    }
                    
                    // Find favorite genre
                    try (ResultSet<SqlRow> rs = sql.execute(null, """
                        SELECT g.Name, COUNT(*) as GenreCount
                        FROM Customer c
                        JOIN Invoice i ON c.CustomerId = i.CustomerId
                        JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId
                        JOIN Track t ON il.TrackId = t.TrackId
                        JOIN Genre g ON t.GenreId = g.GenreId
                        WHERE c.CustomerId = ?
                        GROUP BY g.GenreId, g.Name
                        ORDER BY GenreCount DESC
                        LIMIT 1
                        """, customerId)) {
                        if (rs.hasNext()) {
                            favoriteGenre = rs.next().stringValue("Name");
                        }
                    }
                    
                    double averagePurchase = purchaseCount > 0 ? totalSpent / purchaseCount : 0.0;
                    
                    return new CustomerPurchaseReport(customerId, customerName, purchaseCount, 
                        totalSpent, favoriteGenre, averagePurchase);
                    
                } catch (Exception e) {
                    throw new RuntimeException("Customer purchase analysis failed", e);
                }
            });
        }
    }
    
    /**
     * Map job that analyzes genre distribution on each node.
     * Part of MapReduce pattern for distributed genre analysis.
     */
    public static class GenreAnalysisMapJob implements ComputeJob<Void, Map<String, GenreStats>> {
        @Override
        public CompletableFuture<Map<String, GenreStats>> executeAsync(JobExecutionContext context, Void input) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    IgniteSql sql = context.ignite().sql();
                    Map<String, GenreStats> genreStats = new HashMap<>();
                    
                    // Analyze local genre data
                    try (ResultSet<SqlRow> rs = sql.execute(null, """
                        SELECT g.Name as GenreName,
                               COUNT(t.TrackId) as TrackCount,
                               AVG(t.Milliseconds) as AvgDuration
                        FROM Genre g
                        LEFT JOIN Track t ON g.GenreId = t.GenreId
                        GROUP BY g.GenreId, g.Name
                        """)) {
                        
                        while (rs.hasNext()) {
                            SqlRow row = rs.next();
                            String genreName = row.stringValue("GenreName");
                            int trackCount = row.intValue("TrackCount");
                            double avgDuration = row.doubleValue("AvgDuration") / 1000.0; // Convert to seconds
                            
                            genreStats.put(genreName, new GenreStats(trackCount, avgDuration));
                        }
                    }
                    
                    return genreStats;
                    
                } catch (Exception e) {
                    throw new RuntimeException("Genre analysis failed", e);
                }
            });
        }
    }
    
    // Data classes for job results
    
    public static class ArtistReport {
        private final Integer artistId;
        private final String artistName;
        private final int albumCount;
        private final int totalTracks;
        private final double averageAlbumLength;
        private final double totalRevenue;
        
        public ArtistReport(Integer artistId, String artistName, int albumCount, int totalTracks, 
                           double averageAlbumLength, double totalRevenue) {
            this.artistId = artistId;
            this.artistName = artistName;
            this.albumCount = albumCount;
            this.totalTracks = totalTracks;
            this.averageAlbumLength = averageAlbumLength;
            this.totalRevenue = totalRevenue;
        }
        
        // Getters
        public Integer getArtistId() { return artistId; }
        public String getArtistName() { return artistName; }
        public int getAlbumCount() { return albumCount; }
        public int getTotalTracks() { return totalTracks; }
        public double getAverageAlbumLength() { return averageAlbumLength; }
        public double getTotalRevenue() { return totalRevenue; }
    }
    
    public static class TrackStatistics {
        private final int trackCount;
        private final double totalDuration;
        private final double averageDuration;
        private final double minDuration;
        private final double maxDuration;
        
        public TrackStatistics(int trackCount, double totalDuration, double averageDuration, 
                              double minDuration, double maxDuration) {
            this.trackCount = trackCount;
            this.totalDuration = totalDuration;
            this.averageDuration = averageDuration;
            this.minDuration = minDuration;
            this.maxDuration = maxDuration;
        }
        
        // Getters
        public int getTrackCount() { return trackCount; }
        public double getTotalDuration() { return totalDuration; }
        public double getAverageDuration() { return averageDuration; }
        public double getMinDuration() { return minDuration; }
        public double getMaxDuration() { return maxDuration; }
    }
    
    public static class CustomerPurchaseReport {
        private final Integer customerId;
        private final String customerName;
        private final int purchaseCount;
        private final double totalSpent;
        private final String favoriteGenre;
        private final double averagePurchase;
        
        public CustomerPurchaseReport(Integer customerId, String customerName, int purchaseCount, 
                                    double totalSpent, String favoriteGenre, double averagePurchase) {
            this.customerId = customerId;
            this.customerName = customerName;
            this.purchaseCount = purchaseCount;
            this.totalSpent = totalSpent;
            this.favoriteGenre = favoriteGenre;
            this.averagePurchase = averagePurchase;
        }
        
        // Getters
        public Integer getCustomerId() { return customerId; }
        public String getCustomerName() { return customerName; }
        public int getPurchaseCount() { return purchaseCount; }
        public double getTotalSpent() { return totalSpent; }
        public String getFavoriteGenre() { return favoriteGenre; }
        public double getAveragePurchase() { return averagePurchase; }
    }
    
    public static class GenreStats {
        private final int trackCount;
        private final double averageDuration;
        
        public GenreStats(int trackCount, double averageDuration) {
            this.trackCount = trackCount;
            this.averageDuration = averageDuration;
        }
        
        public GenreStats merge(GenreStats other) {
            // Simple merge - in real implementation you'd properly weight averages
            return new GenreStats(
                this.trackCount + other.trackCount,
                (this.averageDuration + other.averageDuration) / 2.0
            );
        }
        
        // Getters
        public int getTrackCount() { return trackCount; }
        public double getAverageDuration() { return averageDuration; }
    }
}