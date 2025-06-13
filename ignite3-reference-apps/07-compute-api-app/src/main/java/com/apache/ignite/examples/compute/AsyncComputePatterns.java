package com.apache.ignite.examples.compute;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.JobDescriptor;
import org.apache.ignite.compute.JobExecution;
import org.apache.ignite.compute.JobExecutionContext;
import org.apache.ignite.compute.JobState;
import org.apache.ignite.compute.JobTarget;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.Tuple;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Demonstrates asynchronous compute patterns and job management using the music store dataset.
 * 
 * This application showcases advanced async patterns with Ignite 3 Compute API:
 * - Non-blocking job execution with CompletableFuture
 * - Parallel job processing for performance optimization
 * - Job state monitoring and execution control
 * - Error handling and retry patterns in async context
 * - Complex workflow orchestration with dependent jobs
 * 
 * The examples demonstrate how async compute patterns enable building responsive
 * applications that can handle multiple concurrent operations efficiently.
 * 
 * Educational Concepts:
 * - executeAsync() for non-blocking job submission
 * - submitAsync() for job execution control and monitoring
 * - CompletableFuture composition and chaining
 * - Parallel processing patterns with music store analytics
 * - Job state monitoring and lifecycle management
 * - Error handling and resilience patterns
 * 
 * @since 1.0.0
 */
public class AsyncComputePatterns {
    
    private static final String IGNITE_URL = "http://localhost:10800";
    
    public static void main(String[] args) {
        System.out.println("Async Compute Patterns Demo - Non-blocking Job Execution");
        System.out.println("=========================================================");
        
        try (IgniteClient ignite = IgniteClient.builder()
                .addresses(IGNITE_URL)
                .build()) {
            
            // Demonstrate async patterns
            demonstrateBasicAsyncExecution(ignite);
            demonstrateParallelArtistAnalysis(ignite);
            demonstrateJobExecutionControl(ignite);
            demonstrateAsyncWorkflowOrchestration(ignite);
            
        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Demonstrates basic asynchronous job execution without blocking.
     * Shows how to handle results when they complete.
     */
    private static void demonstrateBasicAsyncExecution(IgniteClient ignite) {
        System.out.println("\n1. Basic Async Job Execution");
        System.out.println("   Submitting non-blocking track analysis...");
        
        try {
            // Create job for track analysis
            JobDescriptor<Integer, TrackAnalysisResult> job = 
                JobDescriptor.builder(TrackAnalysisJob.class).build();
            
            JobTarget target = JobTarget.anyNode(ignite.clusterNodes());
            
            // Submit job asynchronously
            CompletableFuture<TrackAnalysisResult> future = ignite.compute()
                .executeAsync(target, job, 100); // Analyze first 100 tracks
            
            // Continue with other work while job executes
            System.out.println("   → Job submitted, continuing with other work...");
            simulateOtherWork();
            
            // Handle result when available
            future.thenAccept(result -> {
                System.out.println("   → Analysis complete: " + result.getTrackCount() + " tracks processed");
                System.out.println("   → Average duration: " + String.format("%.1f", result.getAverageDuration()) + " seconds");
                System.out.println("   → Most common genre: " + result.getMostCommonGenre());
            }).join(); // Wait for completion for demo purposes
            
        } catch (Exception e) {
            System.err.println("   ✗ Async track analysis failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates parallel execution of multiple jobs for different artists.
     * Shows how to process multiple artists concurrently and aggregate results.
     */
    private static void demonstrateParallelArtistAnalysis(IgniteClient ignite) {
        System.out.println("\n2. Parallel Artist Analysis");
        System.out.println("   Analyzing multiple artists concurrently...");
        
        try {
            // Create job for artist sales analysis
            JobDescriptor<Integer, ArtistSalesResult> job = 
                JobDescriptor.builder(ArtistSalesJob.class).build();
            
            // Submit jobs for multiple artists in parallel
            List<Integer> artistIds = Arrays.asList(1, 2, 3, 4, 5);
            List<CompletableFuture<ArtistSalesResult>> futures = new ArrayList<>();
            
            for (Integer artistId : artistIds) {
                JobTarget colocatedTarget = JobTarget.colocated("Artist", 
                    Tuple.create().set("ArtistId", artistId));
                
                CompletableFuture<ArtistSalesResult> future = ignite.compute()
                    .executeAsync(colocatedTarget, job, artistId);
                
                futures.add(future);
            }
            
            // Wait for all jobs to complete and collect results
            CompletableFuture<List<ArtistSalesResult>> allResults = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList()));
            
            // Process aggregated results
            allResults.thenAccept(results -> {
                System.out.println("   → Analyzed " + results.size() + " artists in parallel");
                
                // Find top artist by revenue
                Optional<ArtistSalesResult> topArtist = results.stream()
                    .max(Comparator.comparing(ArtistSalesResult::getTotalRevenue));
                
                if (topArtist.isPresent()) {
                    ArtistSalesResult top = topArtist.get();
                    System.out.println("   → Top artist: " + top.getArtistName() + 
                        " ($" + String.format("%.2f", top.getTotalRevenue()) + " revenue)");
                }
                
                // Calculate total revenue across all artists
                double totalRevenue = results.stream()
                    .mapToDouble(ArtistSalesResult::getTotalRevenue)
                    .sum();
                System.out.println("   → Combined revenue: $" + String.format("%.2f", totalRevenue));
                
            }).join(); // Wait for completion for demo purposes
            
        } catch (Exception e) {
            System.err.println("   ✗ Parallel artist analysis failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates job execution control with monitoring and state management.
     * Shows how to track job progress and manage long-running operations.
     */
    private static void demonstrateJobExecutionControl(IgniteClient ignite) {
        System.out.println("\n3. Job Execution Control and Monitoring");
        System.out.println("   Submitting long-running job with monitoring...");
        
        try {
            // Create long-running job
            JobDescriptor<Integer, String> job = 
                JobDescriptor.builder(LongRunningAnalysisJob.class).build();
            
            JobTarget target = JobTarget.anyNode(ignite.clusterNodes());
            
            // Submit job with execution control
            CompletableFuture<JobExecution<String>> executionFuture = ignite.compute()
                .submitAsync(target, job, 10); // Analyze with 10-second simulation
            
            JobExecution<String> execution = executionFuture.get();
            
            // Monitor job state
            monitorJobExecution(execution);
            
            // Get final result
            String result = execution.resultAsync().get(15, TimeUnit.SECONDS);
            System.out.println("   → Job completed with result: " + result);
            
        } catch (Exception e) {
            System.err.println("   ✗ Job execution control failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates complex async workflow orchestration with dependent jobs.
     * Shows how to chain compute operations and handle dependencies.
     */
    private static void demonstrateAsyncWorkflowOrchestration(IgniteClient ignite) {
        System.out.println("\n4. Async Workflow Orchestration");
        System.out.println("   Orchestrating dependent compute operations...");
        
        try {
            // Step 1: Get genre statistics
            JobDescriptor<Void, Map<String, Integer>> genreJob = 
                JobDescriptor.builder(GenreCountJob.class).build();
            
            CompletableFuture<Map<String, Integer>> genreStatsFuture = ignite.compute()
                .executeAsync(JobTarget.anyNode(ignite.clusterNodes()), genreJob, null);
            
            // Step 2: Based on genre stats, analyze top genres
            CompletableFuture<List<GenreAnalysisResult>> topGenreAnalysisFuture = genreStatsFuture
                .thenCompose(genreStats -> {
                    // Find top 3 genres by track count
                    List<String> topGenres = genreStats.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(3)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
                    
                    System.out.println("   → Top genres identified: " + topGenres);
                    
                    // Submit detailed analysis jobs for top genres
                    JobDescriptor<String, GenreAnalysisResult> detailJob = 
                        JobDescriptor.builder(DetailedGenreAnalysisJob.class).build();
                    
                    List<CompletableFuture<GenreAnalysisResult>> detailFutures = topGenres.stream()
                        .map(genre -> ignite.compute().executeAsync(
                            JobTarget.anyNode(ignite.clusterNodes()), detailJob, genre))
                        .collect(Collectors.toList());
                    
                    return CompletableFuture.allOf(detailFutures.toArray(new CompletableFuture[0]))
                        .thenApply(v -> detailFutures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList()));
                });
            
            // Step 3: Generate final report  
            CompletableFuture<String> reportFuture = topGenreAnalysisFuture
                .thenApply(AsyncComputePatterns::generateGenreReport);
            
            // Handle final result
            reportFuture.thenAccept(report -> {
                System.out.println("   → Workflow completed successfully");
                System.out.println("   → Report generated: " + report);
            }).join(); // Wait for completion for demo purposes
            
        } catch (Exception e) {
            System.err.println("   ✗ Workflow orchestration failed: " + e.getMessage());
        }
    }
    
    /**
     * Monitors job execution state and progress.
     */
    private static void monitorJobExecution(JobExecution<String> execution) {
        System.out.println("   → Monitoring job execution...");
        
        // Monitor state changes
        CompletableFuture<Void> monitoring = CompletableFuture.runAsync(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    JobState state = execution.stateAsync().get();
                    System.out.println("   → Job state: " + state.status() + 
                        " (ID: " + state.id() + ")");
                    
                    if (state.status().toString().equals("COMPLETED") || 
                        state.status().toString().equals("FAILED")) {
                        break;
                    }
                    
                    Thread.sleep(2000); // Check every 2 seconds
                }
            } catch (Exception e) {
                System.err.println("   → Monitoring failed: " + e.getMessage());
            }
        });
        
        // Don't wait for monitoring to complete, just start it
    }
    
    /**
     * Generates a report from genre analysis results.
     */
    private static String generateGenreReport(List<GenreAnalysisResult> results) {
        StringBuilder report = new StringBuilder();
        report.append("Top Genre Analysis Summary - ");
        
        for (GenreAnalysisResult result : results) {
            report.append(result.getGenreName()).append("(")
                  .append(result.getTrackCount()).append(" tracks), ");
        }
        
        return report.toString();
    }
    
    /**
     * Simulates other work being done while compute jobs run.
     */
    private static void simulateOtherWork() {
        try {
            Thread.sleep(1000); // Simulate 1 second of other work
            System.out.println("   → Other work completed while job runs in background");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // Job implementations
    
    /**
     * Job that analyzes track data and returns summary statistics.
     */
    public static class TrackAnalysisJob implements ComputeJob<Integer, TrackAnalysisResult> {
        @Override
        public CompletableFuture<TrackAnalysisResult> executeAsync(JobExecutionContext context, Integer trackLimit) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    IgniteSql sql = context.ignite().sql();
                    
                    int trackCount = 0;
                    double totalDuration = 0.0;
                    Map<String, Integer> genreCounts = new HashMap<>();
                    
                    // Analyze tracks with limit
                    try (ResultSet<SqlRow> rs = sql.execute(null, """
                        SELECT t.Milliseconds, g.Name as GenreName
                        FROM Track t
                        LEFT JOIN Genre g ON t.GenreId = g.GenreId
                        ORDER BY t.TrackId
                        LIMIT ?
                        """, trackLimit)) {
                        
                        while (rs.hasNext()) {
                            SqlRow row = rs.next();
                            trackCount++;
                            totalDuration += row.doubleValue("Milliseconds") / 1000.0; // Convert to seconds
                            
                            String genre = row.stringValue("GenreName");
                            if (genre != null) {
                                genreCounts.merge(genre, 1, Integer::sum);
                            }
                        }
                    }
                    
                    double averageDuration = trackCount > 0 ? totalDuration / trackCount : 0.0;
                    String mostCommonGenre = genreCounts.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("Unknown");
                    
                    return new TrackAnalysisResult(trackCount, averageDuration, mostCommonGenre);
                    
                } catch (Exception e) {
                    throw new RuntimeException("Track analysis failed", e);
                }
            });
        }
    }
    
    /**
     * Job that analyzes sales data for a specific artist.
     */
    public static class ArtistSalesJob implements ComputeJob<Integer, ArtistSalesResult> {
        @Override
        public CompletableFuture<ArtistSalesResult> executeAsync(JobExecutionContext context, Integer artistId) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    IgniteSql sql = context.ignite().sql();
                    
                    String artistName = "Unknown Artist";
                    double totalRevenue = 0.0;
                    int tracksSold = 0;
                    
                    // Get artist name
                    try (ResultSet<SqlRow> rs = sql.execute(null, 
                        "SELECT Name FROM Artist WHERE ArtistId = ?", artistId)) {
                        if (rs.hasNext()) {
                            artistName = rs.next().stringValue("Name");
                        }
                    }
                    
                    // Calculate sales data
                    try (ResultSet<SqlRow> rs = sql.execute(null, """
                        SELECT SUM(il.UnitPrice * il.Quantity) as Revenue,
                               SUM(il.Quantity) as TracksSold
                        FROM Artist ar
                        JOIN Album al ON ar.ArtistId = al.ArtistId
                        JOIN Track t ON al.AlbumId = t.AlbumId
                        JOIN InvoiceLine il ON t.TrackId = il.TrackId
                        WHERE ar.ArtistId = ?
                        """, artistId)) {
                        if (rs.hasNext()) {
                            SqlRow row = rs.next();
                            Object revenueObj = row.value("Revenue");
                            Object soldObj = row.value("TracksSold");
                            
                            if (revenueObj != null) {
                                totalRevenue = ((Number) revenueObj).doubleValue();
                            }
                            if (soldObj != null) {
                                tracksSold = ((Number) soldObj).intValue();
                            }
                        }
                    }
                    
                    return new ArtistSalesResult(artistId, artistName, totalRevenue, tracksSold);
                    
                } catch (Exception e) {
                    throw new RuntimeException("Artist sales analysis failed", e);
                }
            });
        }
    }
    
    /**
     * Long-running job that simulates complex analysis with progress.
     */
    public static class LongRunningAnalysisJob implements ComputeJob<Integer, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Integer durationSeconds) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    int steps = durationSeconds;
                    for (int i = 0; i < steps; i++) {
                        // Check for cancellation
                        if (context.isCancelled()) {
                            return "Analysis cancelled after " + i + " steps";
                        }
                        
                        Thread.sleep(1000); // Simulate 1 second of work per step
                    }
                    
                    return "Complex analysis completed after " + steps + " steps";
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return "Analysis interrupted";
                }
            });
        }
    }
    
    /**
     * Job that counts tracks by genre.
     */
    public static class GenreCountJob implements ComputeJob<Void, Map<String, Integer>> {
        @Override
        public CompletableFuture<Map<String, Integer>> executeAsync(JobExecutionContext context, Void input) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    IgniteSql sql = context.ignite().sql();
                    Map<String, Integer> genreCounts = new HashMap<>();
                    
                    try (ResultSet<SqlRow> rs = sql.execute(null, """
                        SELECT g.Name, COUNT(t.TrackId) as TrackCount
                        FROM Genre g
                        LEFT JOIN Track t ON g.GenreId = t.GenreId
                        GROUP BY g.GenreId, g.Name
                        """)) {
                        
                        while (rs.hasNext()) {
                            SqlRow row = rs.next();
                            genreCounts.put(row.stringValue("Name"), row.intValue("TrackCount"));
                        }
                    }
                    
                    return genreCounts;
                    
                } catch (Exception e) {
                    throw new RuntimeException("Genre count analysis failed", e);
                }
            });
        }
    }
    
    /**
     * Job that performs detailed analysis for a specific genre.
     */
    public static class DetailedGenreAnalysisJob implements ComputeJob<String, GenreAnalysisResult> {
        @Override
        public CompletableFuture<GenreAnalysisResult> executeAsync(JobExecutionContext context, String genreName) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    IgniteSql sql = context.ignite().sql();
                    
                    int trackCount = 0;
                    double averageDuration = 0.0;
                    double totalRevenue = 0.0;
                    
                    // Get detailed genre statistics
                    try (ResultSet<SqlRow> rs = sql.execute(null, """
                        SELECT COUNT(t.TrackId) as TrackCount,
                               AVG(t.Milliseconds) as AvgDuration,
                               SUM(COALESCE(il.UnitPrice * il.Quantity, 0)) as Revenue
                        FROM Genre g
                        LEFT JOIN Track t ON g.GenreId = t.GenreId
                        LEFT JOIN InvoiceLine il ON t.TrackId = il.TrackId
                        WHERE g.Name = ?
                        """, genreName)) {
                        
                        if (rs.hasNext()) {
                            SqlRow row = rs.next();
                            trackCount = row.intValue("TrackCount");
                            averageDuration = row.doubleValue("AvgDuration") / 1000.0; // Convert to seconds
                            Object revenueObj = row.value("Revenue");
                            if (revenueObj != null) {
                                totalRevenue = ((Number) revenueObj).doubleValue();
                            }
                        }
                    }
                    
                    return new GenreAnalysisResult(genreName, trackCount, averageDuration, totalRevenue);
                    
                } catch (Exception e) {
                    throw new RuntimeException("Detailed genre analysis failed", e);
                }
            });
        }
    }
    
    // Result classes
    
    public static class TrackAnalysisResult {
        private final int trackCount;
        private final double averageDuration;
        private final String mostCommonGenre;
        
        public TrackAnalysisResult(int trackCount, double averageDuration, String mostCommonGenre) {
            this.trackCount = trackCount;
            this.averageDuration = averageDuration;
            this.mostCommonGenre = mostCommonGenre;
        }
        
        public int getTrackCount() { return trackCount; }
        public double getAverageDuration() { return averageDuration; }
        public String getMostCommonGenre() { return mostCommonGenre; }
    }
    
    public static class ArtistSalesResult {
        private final Integer artistId;
        private final String artistName;
        private final double totalRevenue;
        private final int tracksSold;
        
        public ArtistSalesResult(Integer artistId, String artistName, double totalRevenue, int tracksSold) {
            this.artistId = artistId;
            this.artistName = artistName;
            this.totalRevenue = totalRevenue;
            this.tracksSold = tracksSold;
        }
        
        public Integer getArtistId() { return artistId; }
        public String getArtistName() { return artistName; }
        public double getTotalRevenue() { return totalRevenue; }
        public int getTracksSold() { return tracksSold; }
    }
    
    public static class GenreAnalysisResult {
        private final String genreName;
        private final int trackCount;
        private final double averageDuration;
        private final double totalRevenue;
        
        public GenreAnalysisResult(String genreName, int trackCount, double averageDuration, double totalRevenue) {
            this.genreName = genreName;
            this.trackCount = trackCount;
            this.averageDuration = averageDuration;
            this.totalRevenue = totalRevenue;
        }
        
        public String getGenreName() { return genreName; }
        public int getTrackCount() { return trackCount; }
        public double getAverageDuration() { return averageDuration; }
        public double getTotalRevenue() { return totalRevenue; }
    }
}