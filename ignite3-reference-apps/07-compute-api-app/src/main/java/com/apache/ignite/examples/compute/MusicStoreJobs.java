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

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Specialized compute jobs for music store analytics and business intelligence.
 * 
 * This application demonstrates real-world compute scenarios that music streaming
 * platforms and digital music stores commonly implement:
 * - Customer recommendation algorithms
 * - Sales performance analytics
 * - Content popularity analysis
 * - Revenue optimization jobs
 * 
 * These jobs showcase advanced patterns like:
 * - MapReduce for distributed analytics
 * - Multi-step job workflows
 * - Complex data aggregation
 * - Business intelligence reporting
 * 
 * Educational Concepts:
 * - Production-ready job implementations
 * - Complex business logic in distributed jobs
 * - Performance optimization through data locality
 * - Error handling and resilience patterns
 * - Structured result aggregation
 * 
 * @since 1.0.0
 */
public class MusicStoreJobs {
    
    private static final String IGNITE_URL = "http://localhost:10800";
    
    public static void main(String[] args) {
        System.out.println("Music Store Analytics Jobs - Business Intelligence Platform");
        System.out.println("===========================================================");
        
        try (IgniteClient ignite = IgniteClient.builder()
                .addresses(IGNITE_URL)
                .build()) {
            
            // Run specialized analytics jobs
            demonstrateRecommendationEngine(ignite);
            demonstrateSalesPerformanceAnalysis(ignite);
            demonstrateContentPopularityAnalysis(ignite);
            demonstrateRevenueOptimizationAnalysis(ignite);
            
        } catch (Exception e) {
            System.err.println("Music store analytics failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Demonstrates a recommendation engine that suggests tracks based on customer purchase history.
     * Uses colocated execution to process recommendations where customer data resides.
     */
    private static void demonstrateRecommendationEngine(IgniteClient ignite) {
        System.out.println("\n1. Customer Recommendation Engine");
        System.out.println("   Generating personalized music recommendations...");
        
        try {
            // Create recommendation job
            JobDescriptor<Integer, CustomerRecommendations> job = 
                JobDescriptor.builder(CustomerRecommendationJob.class).build();
            
            // Execute colocated with customer data
            Integer customerId = 1;
            JobTarget colocatedTarget = JobTarget.colocated("Customer", 
                Tuple.create().set("CustomerId", customerId));
            
            CustomerRecommendations recommendations = ignite.compute().execute(colocatedTarget, job, customerId);
            
            System.out.println("   → Customer: " + recommendations.getCustomerName());
            System.out.println("   → Purchase history: " + recommendations.getPurchasedGenres().size() + " genres");
            System.out.println("   → Recommendations generated: " + recommendations.getRecommendedTracks().size());
            System.out.println("   → Top recommendation: " + 
                (recommendations.getRecommendedTracks().isEmpty() ? "None" : 
                 recommendations.getRecommendedTracks().get(0).getTrackName()));
            
        } catch (Exception e) {
            System.err.println("   ✗ Recommendation engine failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates sales performance analysis across all artists using broadcast execution.
     * Aggregates sales data from all nodes to generate comprehensive reports.
     */
    private static void demonstrateSalesPerformanceAnalysis(IgniteClient ignite) {
        System.out.println("\n2. Sales Performance Analysis");
        System.out.println("   Analyzing sales performance across all artists...");
        
        try {
            // Map phase: gather sales data from each node
            JobDescriptor<Void, List<ArtistSalesMetrics>> mapJob = 
                JobDescriptor.builder(SalesAnalysisMapJob.class).build();
            
            BroadcastJobTarget broadcastTarget = BroadcastJobTarget.table("Artist");
            Collection<List<ArtistSalesMetrics>> mapResults = ignite.compute().execute(broadcastTarget, mapJob, null);
            
            // Reduce phase: aggregate results
            List<ArtistSalesMetrics> allArtistMetrics = new ArrayList<>();
            for (List<ArtistSalesMetrics> nodeResults : mapResults) {
                allArtistMetrics.addAll(nodeResults);
            }
            
            // Merge metrics for artists that appear on multiple nodes
            Map<String, ArtistSalesMetrics> mergedMetrics = new HashMap<>();
            for (ArtistSalesMetrics metrics : allArtistMetrics) {
                mergedMetrics.merge(metrics.getArtistName(), metrics, ArtistSalesMetrics::merge);
            }
            
            // Display top performers
            List<ArtistSalesMetrics> topPerformers = mergedMetrics.values().stream()
                .sorted(Comparator.comparing(ArtistSalesMetrics::getTotalRevenue).reversed())
                .limit(5)
                .collect(Collectors.toList());
            
            System.out.println("   → Top 5 Artists by Revenue:");
            for (int i = 0; i < topPerformers.size(); i++) {
                ArtistSalesMetrics artist = topPerformers.get(i);
                System.out.println("     " + (i + 1) + ". " + artist.getArtistName() + 
                    " - $" + String.format("%.2f", artist.getTotalRevenue()) + 
                    " (" + artist.getTracksSold() + " tracks sold)");
            }
            
        } catch (Exception e) {
            System.err.println("   ✗ Sales performance analysis failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates content popularity analysis using distributed track analytics.
     * Identifies trending tracks and genres across the platform.
     */
    private static void demonstrateContentPopularityAnalysis(IgniteClient ignite) {
        System.out.println("\n3. Content Popularity Analysis");
        System.out.println("   Analyzing track and genre popularity trends...");
        
        try {
            // Analyze content popularity
            JobDescriptor<Void, ContentPopularityReport> job = 
                JobDescriptor.builder(ContentPopularityJob.class).build();
            
            JobTarget target = JobTarget.anyNode(ignite.clusterNodes());
            ContentPopularityReport report = ignite.compute().execute(target, job, null);
            
            System.out.println("   → Total tracks analyzed: " + report.getTotalTracks());
            System.out.println("   → Most popular genre: " + report.getMostPopularGenre() + 
                " (" + report.getMostPopularGenreCount() + " tracks)");
            System.out.println("   → Average track popularity: " + 
                String.format("%.1f", report.getAveragePopularityScore()));
            
            System.out.println("   → Top 3 Trending Tracks:");
            List<TrackPopularity> topTracks = report.getTopTracks();
            for (int i = 0; i < Math.min(3, topTracks.size()); i++) {
                TrackPopularity track = topTracks.get(i);
                System.out.println("     " + (i + 1) + ". " + track.getTrackName() + 
                    " (Score: " + String.format("%.1f", track.getPopularityScore()) + ")");
            }
            
        } catch (Exception e) {
            System.err.println("   ✗ Content popularity analysis failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates revenue optimization analysis using parallel processing.
     * Identifies opportunities for pricing and content strategy optimization.
     */
    private static void demonstrateRevenueOptimizationAnalysis(IgniteClient ignite) {
        System.out.println("\n4. Revenue Optimization Analysis");
        System.out.println("   Analyzing revenue optimization opportunities...");
        
        try {
            // Run revenue optimization analysis
            JobDescriptor<Void, RevenueOptimizationReport> job = 
                JobDescriptor.builder(RevenueOptimizationJob.class).build();
            
            JobTarget target = JobTarget.anyNode(ignite.clusterNodes());
            RevenueOptimizationReport report = ignite.compute().execute(target, job, null);
            
            System.out.println("   → Total revenue analyzed: $" + String.format("%.2f", report.getTotalRevenue()));
            System.out.println("   → Average price per track: $" + String.format("%.2f", report.getAveragePricePerTrack()));
            System.out.println("   → Revenue per customer: $" + String.format("%.2f", report.getRevenuePerCustomer()));
            System.out.println("   → Most profitable genre: " + report.getMostProfitableGenre());
            
            System.out.println("   → Optimization Recommendations:");
            for (String recommendation : report.getOptimizationRecommendations()) {
                System.out.println("     • " + recommendation);
            }
            
        } catch (Exception e) {
            System.err.println("   ✗ Revenue optimization analysis failed: " + e.getMessage());
        }
    }
    
    // Job implementations
    
    /**
     * Job that generates personalized music recommendations for a customer.
     * Uses collaborative filtering based on purchase history and genre preferences.
     */
    public static class CustomerRecommendationJob implements ComputeJob<Integer, CustomerRecommendations> {
        @Override
        public CompletableFuture<CustomerRecommendations> executeAsync(JobExecutionContext context, Integer customerId) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    IgniteSql sql = context.ignite().sql();
                    
                    String customerName = "Unknown Customer";
                    Set<String> purchasedGenres = new HashSet<>();
                    List<TrackRecommendation> recommendations = new ArrayList<>();
                    
                    // Get customer information
                    try (ResultSet<SqlRow> rs = sql.execute(null, 
                        "SELECT FirstName, LastName FROM Customer WHERE CustomerId = ?", customerId)) {
                        if (rs.hasNext()) {
                            SqlRow row = rs.next();
                            customerName = row.stringValue("FirstName") + " " + row.stringValue("LastName");
                        }
                    }
                    
                    // Get customer's genre preferences from purchase history
                    try (ResultSet<SqlRow> rs = sql.execute(null, """
                        SELECT DISTINCT g.Name as GenreName
                        FROM Customer c
                        JOIN Invoice i ON c.CustomerId = i.CustomerId
                        JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId
                        JOIN Track t ON il.TrackId = t.TrackId
                        JOIN Genre g ON t.GenreId = g.GenreId
                        WHERE c.CustomerId = ?
                        """, customerId)) {
                        while (rs.hasNext()) {
                            purchasedGenres.add(rs.next().stringValue("GenreName"));
                        }
                    }
                    
                    // Generate recommendations based on similar customers
                    if (!purchasedGenres.isEmpty()) {
                        String genreList = purchasedGenres.stream()
                            .map(g -> "'" + g + "'")
                            .collect(Collectors.joining(","));
                        
                        try (ResultSet<SqlRow> rs = sql.execute(null, 
                            "SELECT t.Name as TrackName, ar.Name as ArtistName, g.Name as GenreName " +
                            "FROM Track t " +
                            "JOIN Album al ON t.AlbumId = al.AlbumId " +
                            "JOIN Artist ar ON al.ArtistId = ar.ArtistId " +
                            "JOIN Genre g ON t.GenreId = g.GenreId " +
                            "WHERE g.Name IN (" + genreList + ") " +
                            "AND t.TrackId NOT IN (" +
                            "  SELECT il.TrackId FROM Invoice i " +
                            "  JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId " +
                            "  WHERE i.CustomerId = ?" +
                            ") " +
                            "ORDER BY RANDOM() LIMIT 10", customerId)) {
                            
                            while (rs.hasNext()) {
                                SqlRow row = rs.next();
                                recommendations.add(new TrackRecommendation(
                                    row.stringValue("TrackName"),
                                    row.stringValue("ArtistName"),
                                    row.stringValue("GenreName"),
                                    Math.random() * 5.0 // Simulated recommendation score
                                ));
                            }
                        }
                    }
                    
                    return new CustomerRecommendations(customerId, customerName, purchasedGenres, recommendations);
                    
                } catch (Exception e) {
                    throw new RuntimeException("Customer recommendation generation failed", e);
                }
            });
        }
    }
    
    /**
     * Map job that analyzes sales metrics for artists on each node.
     */
    public static class SalesAnalysisMapJob implements ComputeJob<Void, List<ArtistSalesMetrics>> {
        @Override
        public CompletableFuture<List<ArtistSalesMetrics>> executeAsync(JobExecutionContext context, Void input) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    IgniteSql sql = context.ignite().sql();
                    List<ArtistSalesMetrics> artistMetrics = new ArrayList<>();
                    
                    // Analyze local artist sales data
                    try (ResultSet<SqlRow> rs = sql.execute(null, """
                        SELECT ar.Name as ArtistName,
                               COUNT(DISTINCT al.AlbumId) as AlbumCount,
                               COUNT(DISTINCT t.TrackId) as TrackCount,
                               SUM(COALESCE(il.Quantity, 0)) as TracksSold,
                               SUM(COALESCE(il.UnitPrice * il.Quantity, 0)) as TotalRevenue
                        FROM Artist ar
                        LEFT JOIN Album al ON ar.ArtistId = al.ArtistId
                        LEFT JOIN Track t ON al.AlbumId = t.AlbumId
                        LEFT JOIN InvoiceLine il ON t.TrackId = il.TrackId
                        GROUP BY ar.ArtistId, ar.Name
                        HAVING COUNT(DISTINCT al.AlbumId) > 0
                        """)) {
                        
                        while (rs.hasNext()) {
                            SqlRow row = rs.next();
                            String artistName = row.stringValue("ArtistName");
                            int albumCount = row.intValue("AlbumCount");
                            int trackCount = row.intValue("TrackCount");
                            
                            Object soldObj = row.value("TracksSold");
                            Object revenueObj = row.value("TotalRevenue");
                            
                            int tracksSold = soldObj != null ? ((Number) soldObj).intValue() : 0;
                            double totalRevenue = revenueObj != null ? ((Number) revenueObj).doubleValue() : 0.0;
                            
                            artistMetrics.add(new ArtistSalesMetrics(
                                artistName, albumCount, trackCount, tracksSold, totalRevenue));
                        }
                    }
                    
                    return artistMetrics;
                    
                } catch (Exception e) {
                    throw new RuntimeException("Sales analysis failed", e);
                }
            });
        }
    }
    
    /**
     * Job that analyzes content popularity across tracks and genres.
     */
    public static class ContentPopularityJob implements ComputeJob<Void, ContentPopularityReport> {
        @Override
        public CompletableFuture<ContentPopularityReport> executeAsync(JobExecutionContext context, Void input) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    IgniteSql sql = context.ignite().sql();
                    
                    int totalTracks = 0;
                    String mostPopularGenre = "Unknown";
                    int mostPopularGenreCount = 0;
                    double averagePopularityScore = 0.0;
                    List<TrackPopularity> topTracks = new ArrayList<>();
                    
                    // Get total track count
                    try (ResultSet<SqlRow> rs = sql.execute(null, "SELECT COUNT(*) as TrackCount FROM Track")) {
                        if (rs.hasNext()) {
                            totalTracks = rs.next().intValue("TrackCount");
                        }
                    }
                    
                    // Find most popular genre by track count
                    try (ResultSet<SqlRow> rs = sql.execute(null, """
                        SELECT g.Name, COUNT(t.TrackId) as TrackCount
                        FROM Genre g
                        LEFT JOIN Track t ON g.GenreId = t.GenreId
                        GROUP BY g.GenreId, g.Name
                        ORDER BY TrackCount DESC
                        LIMIT 1
                        """)) {
                        if (rs.hasNext()) {
                            SqlRow row = rs.next();
                            mostPopularGenre = row.stringValue("Name");
                            mostPopularGenreCount = row.intValue("TrackCount");
                        }
                    }
                    
                    // Calculate track popularity based on sales and create top tracks list
                    try (ResultSet<SqlRow> rs = sql.execute(null, """
                        SELECT t.Name as TrackName,
                               ar.Name as ArtistName,
                               SUM(COALESCE(il.Quantity, 0)) as SalesCount,
                               t.Milliseconds as Duration
                        FROM Track t
                        JOIN Album al ON t.AlbumId = al.AlbumId
                        JOIN Artist ar ON al.ArtistId = ar.ArtistId
                        LEFT JOIN InvoiceLine il ON t.TrackId = il.TrackId
                        GROUP BY t.TrackId, t.Name, ar.Name, t.Milliseconds
                        ORDER BY SalesCount DESC, t.Name
                        LIMIT 10
                        """)) {
                        
                        double totalPopularity = 0.0;
                        int trackCount = 0;
                        
                        while (rs.hasNext()) {
                            SqlRow row = rs.next();
                            String trackName = row.stringValue("TrackName");
                            String artistName = row.stringValue("ArtistName");
                            
                            Object salesObj = row.value("SalesCount");
                            int salesCount = salesObj != null ? ((Number) salesObj).intValue() : 0;
                            
                            // Simple popularity score: sales count + duration factor
                            double duration = row.doubleValue("Milliseconds") / 1000.0;
                            double popularityScore = salesCount * 10.0 + (duration / 60.0); // Favor longer tracks slightly
                            
                            topTracks.add(new TrackPopularity(trackName, artistName, popularityScore, salesCount));
                            totalPopularity += popularityScore;
                            trackCount++;
                        }
                        
                        averagePopularityScore = trackCount > 0 ? totalPopularity / trackCount : 0.0;
                    }
                    
                    return new ContentPopularityReport(totalTracks, mostPopularGenre, 
                        mostPopularGenreCount, averagePopularityScore, topTracks);
                    
                } catch (Exception e) {
                    throw new RuntimeException("Content popularity analysis failed", e);
                }
            });
        }
    }
    
    /**
     * Job that analyzes revenue optimization opportunities.
     */
    public static class RevenueOptimizationJob implements ComputeJob<Void, RevenueOptimizationReport> {
        @Override
        public CompletableFuture<RevenueOptimizationReport> executeAsync(JobExecutionContext context, Void input) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    IgniteSql sql = context.ignite().sql();
                    
                    double totalRevenue = 0.0;
                    double averagePricePerTrack = 0.0;
                    double revenuePerCustomer = 0.0;
                    String mostProfitableGenre = "Unknown";
                    List<String> optimizationRecommendations = new ArrayList<>();
                    
                    // Calculate total revenue
                    try (ResultSet<SqlRow> rs = sql.execute(null, 
                        "SELECT SUM(UnitPrice * Quantity) as TotalRevenue FROM InvoiceLine")) {
                        if (rs.hasNext()) {
                            Object revenueObj = rs.next().value("TotalRevenue");
                            if (revenueObj != null) {
                                totalRevenue = ((Number) revenueObj).doubleValue();
                            }
                        }
                    }
                    
                    // Calculate average price per track
                    try (ResultSet<SqlRow> rs = sql.execute(null, 
                        "SELECT AVG(UnitPrice) as AvgPrice FROM InvoiceLine")) {
                        if (rs.hasNext()) {
                            averagePricePerTrack = rs.next().doubleValue("AvgPrice");
                        }
                    }
                    
                    // Calculate revenue per customer
                    try (ResultSet<SqlRow> rs = sql.execute(null, """
                        SELECT AVG(CustomerRevenue) as AvgRevenuePerCustomer
                        FROM (
                            SELECT i.CustomerId, SUM(il.UnitPrice * il.Quantity) as CustomerRevenue
                            FROM Invoice i
                            JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId
                            GROUP BY i.CustomerId
                        )
                        """)) {
                        if (rs.hasNext()) {
                            revenuePerCustomer = rs.next().doubleValue("AvgRevenuePerCustomer");
                        }
                    }
                    
                    // Find most profitable genre
                    try (ResultSet<SqlRow> rs = sql.execute(null, """
                        SELECT g.Name, SUM(il.UnitPrice * il.Quantity) as GenreRevenue
                        FROM Genre g
                        JOIN Track t ON g.GenreId = t.GenreId
                        JOIN InvoiceLine il ON t.TrackId = il.TrackId
                        GROUP BY g.GenreId, g.Name
                        ORDER BY GenreRevenue DESC
                        LIMIT 1
                        """)) {
                        if (rs.hasNext()) {
                            mostProfitableGenre = rs.next().stringValue("Name");
                        }
                    }
                    
                    // Generate optimization recommendations
                    if (averagePricePerTrack < 1.0) {
                        optimizationRecommendations.add("Consider price optimization - average track price is below $1.00");
                    }
                    if (revenuePerCustomer < 50.0) {
                        optimizationRecommendations.add("Focus on customer lifetime value - low revenue per customer");
                    }
                    optimizationRecommendations.add("Promote " + mostProfitableGenre + " genre content for higher margins");
                    optimizationRecommendations.add("Implement dynamic pricing based on track popularity");
                    
                    return new RevenueOptimizationReport(totalRevenue, averagePricePerTrack, 
                        revenuePerCustomer, mostProfitableGenre, optimizationRecommendations);
                    
                } catch (Exception e) {
                    throw new RuntimeException("Revenue optimization analysis failed", e);
                }
            });
        }
    }
    
    // Result classes
    
    public static class CustomerRecommendations implements Serializable {
        private final Integer customerId;
        private final String customerName;
        private final Set<String> purchasedGenres;
        private final List<TrackRecommendation> recommendedTracks;
        
        public CustomerRecommendations(Integer customerId, String customerName, 
                                     Set<String> purchasedGenres, List<TrackRecommendation> recommendedTracks) {
            this.customerId = customerId;
            this.customerName = customerName;
            this.purchasedGenres = purchasedGenres;
            this.recommendedTracks = recommendedTracks;
        }
        
        // Getters
        public Integer getCustomerId() { return customerId; }
        public String getCustomerName() { return customerName; }
        public Set<String> getPurchasedGenres() { return purchasedGenres; }
        public List<TrackRecommendation> getRecommendedTracks() { return recommendedTracks; }
    }
    
    public static class TrackRecommendation implements Serializable {
        private final String trackName;
        private final String artistName;
        private final String genreName;
        private final double recommendationScore;
        
        public TrackRecommendation(String trackName, String artistName, String genreName, double recommendationScore) {
            this.trackName = trackName;
            this.artistName = artistName;
            this.genreName = genreName;
            this.recommendationScore = recommendationScore;
        }
        
        // Getters
        public String getTrackName() { return trackName; }
        public String getArtistName() { return artistName; }
        public String getGenreName() { return genreName; }
        public double getRecommendationScore() { return recommendationScore; }
    }
    
    public static class ArtistSalesMetrics implements Serializable {
        private final String artistName;
        private final int albumCount;
        private final int trackCount;
        private final int tracksSold;
        private final double totalRevenue;
        
        public ArtistSalesMetrics(String artistName, int albumCount, int trackCount, int tracksSold, double totalRevenue) {
            this.artistName = artistName;
            this.albumCount = albumCount;
            this.trackCount = trackCount;
            this.tracksSold = tracksSold;
            this.totalRevenue = totalRevenue;
        }
        
        public ArtistSalesMetrics merge(ArtistSalesMetrics other) {
            return new ArtistSalesMetrics(
                this.artistName, // Assume same artist
                this.albumCount + other.albumCount,
                this.trackCount + other.trackCount,
                this.tracksSold + other.tracksSold,
                this.totalRevenue + other.totalRevenue
            );
        }
        
        // Getters
        public String getArtistName() { return artistName; }
        public int getAlbumCount() { return albumCount; }
        public int getTrackCount() { return trackCount; }
        public int getTracksSold() { return tracksSold; }
        public double getTotalRevenue() { return totalRevenue; }
    }
    
    public static class ContentPopularityReport implements Serializable {
        private final int totalTracks;
        private final String mostPopularGenre;
        private final int mostPopularGenreCount;
        private final double averagePopularityScore;
        private final List<TrackPopularity> topTracks;
        
        public ContentPopularityReport(int totalTracks, String mostPopularGenre, int mostPopularGenreCount,
                                     double averagePopularityScore, List<TrackPopularity> topTracks) {
            this.totalTracks = totalTracks;
            this.mostPopularGenre = mostPopularGenre;
            this.mostPopularGenreCount = mostPopularGenreCount;
            this.averagePopularityScore = averagePopularityScore;
            this.topTracks = topTracks;
        }
        
        // Getters
        public int getTotalTracks() { return totalTracks; }
        public String getMostPopularGenre() { return mostPopularGenre; }
        public int getMostPopularGenreCount() { return mostPopularGenreCount; }
        public double getAveragePopularityScore() { return averagePopularityScore; }
        public List<TrackPopularity> getTopTracks() { return topTracks; }
    }
    
    public static class TrackPopularity implements Serializable {
        private final String trackName;
        private final String artistName;
        private final double popularityScore;
        private final int salesCount;
        
        public TrackPopularity(String trackName, String artistName, double popularityScore, int salesCount) {
            this.trackName = trackName;
            this.artistName = artistName;
            this.popularityScore = popularityScore;
            this.salesCount = salesCount;
        }
        
        // Getters
        public String getTrackName() { return trackName; }
        public String getArtistName() { return artistName; }
        public double getPopularityScore() { return popularityScore; }
        public int getSalesCount() { return salesCount; }
    }
    
    public static class RevenueOptimizationReport implements Serializable {
        private final double totalRevenue;
        private final double averagePricePerTrack;
        private final double revenuePerCustomer;
        private final String mostProfitableGenre;
        private final List<String> optimizationRecommendations;
        
        public RevenueOptimizationReport(double totalRevenue, double averagePricePerTrack, double revenuePerCustomer,
                                       String mostProfitableGenre, List<String> optimizationRecommendations) {
            this.totalRevenue = totalRevenue;
            this.averagePricePerTrack = averagePricePerTrack;
            this.revenuePerCustomer = revenuePerCustomer;
            this.mostProfitableGenre = mostProfitableGenre;
            this.optimizationRecommendations = optimizationRecommendations;
        }
        
        // Getters
        public double getTotalRevenue() { return totalRevenue; }
        public double getAveragePricePerTrack() { return averagePricePerTrack; }
        public double getRevenuePerCustomer() { return revenuePerCustomer; }
        public String getMostProfitableGenre() { return mostProfitableGenre; }
        public List<String> getOptimizationRecommendations() { return optimizationRecommendations; }
    }
}