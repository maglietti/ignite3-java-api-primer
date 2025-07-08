/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apache.ignite.examples.compute;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.compute.*;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.sql.Statement;
import org.apache.ignite.table.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Demonstrates production-scale compute patterns using Apache Ignite 3.
 * 
 * Implements comprehensive distributed computing patterns for real-world
 * music streaming platform scenarios including recommendation algorithms,
 * analytics workflows, and performance optimization strategies.
 * 
 * Key patterns demonstrated:
 * - Large-scale recommendation engine processing
 * - Hierarchical data colocation for performance
 * - Advanced MapReduce workflows with custom aggregation
 * - Circuit breaker patterns for compute resilience
 * - Performance monitoring and job lifecycle management
 * - Cross-node result coordination and merging
 * 
 * Prerequisites:
 * - Running Ignite cluster
 * - Sample music store data loaded
 */
public class ProductionComputePatterns {

    private static final Logger logger = LoggerFactory.getLogger(ProductionComputePatterns.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Production Compute Patterns Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Demonstrating production-scale distributed computing");

        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            ProductionComputePatterns demo = new ProductionComputePatterns();
            demo.runProductionComputePatterns(client);
            
        } catch (Exception e) {
            logger.error("Failed to run production compute patterns", e);
            System.err.println("!!! Error: " + e.getMessage());
        }
    }

    private void runProductionComputePatterns(IgniteClient client) {
        System.out.println("\n--- Production Compute Patterns ---");
        System.out.println("    Large-scale distributed processing implementations");
        
        // Deploy job classes for execution
        if (!ComputeJobDeployment.deployJobClasses()) {
            System.out.println(">>> Continuing with development deployment units");
        }
        
        // Large-scale recommendation engine
        demonstrateRecommendationEngine(client);
        
        // Performance-optimized data colocation
        demonstrateDataColocationPatterns(client);
        
        // Advanced MapReduce workflows
        demonstrateAdvancedMapReduce(client);
        
        // Circuit breaker patterns
        demonstrateComputeResilience(client);
        
        // Performance monitoring
        demonstratePerformanceMonitoring(client);
        
        System.out.println("\n>>> Production compute patterns completed successfully");
    }

    /**
     * Demonstrates large-scale recommendation engine processing.
     */
    private void demonstrateRecommendationEngine(IgniteClient client) {
        System.out.println("\n--- Recommendation Engine Processing");
        System.out.println(">>> Processing user profiles for personalized recommendations");
        
        try {
            // Simulate large-scale recommendation processing
            RecommendationProcessor processor = new RecommendationProcessor();
            CompletableFuture<RecommendationReport> result = processor.processRecommendations(client);
            
            RecommendationReport report = result.get(30, TimeUnit.SECONDS);
            System.out.println("<<< Recommendation processing completed:");
            System.out.println("         Users processed: " + report.usersProcessed);
            System.out.println("         Recommendations generated: " + report.recommendationsGenerated);
            System.out.println("         Processing time: " + report.processingTimeMs + "ms");
            System.out.println("         Nodes utilized: " + report.nodesUtilized);
            
        } catch (Exception e) {
            System.err.println("!!! Recommendation processing failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrates performance-optimized data colocation patterns.
     */
    private void demonstrateDataColocationPatterns(IgniteClient client) {
        System.out.println("\n--- Data Colocation Performance Patterns");
        System.out.println(">>> Optimizing job placement for data locality");
        
        try {
            // Artist-specific analytics with colocation
            List<Integer> artistIds = Arrays.asList(1, 2, 3, 4, 5);
            
            List<CompletableFuture<ArtistAnalytics>> futures = artistIds.stream()
                .map(artistId -> processArtistAnalytics(client, artistId))
                .collect(Collectors.toList());
            
            List<ArtistAnalytics> results = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList()))
                .get();
            
            System.out.println("<<< Artist analytics completed for " + results.size() + " artists");
            for (ArtistAnalytics analytics : results) {
                System.out.println("         Artist " + analytics.artistId + ": " + 
                                 analytics.albumCount + " albums, " + 
                                 analytics.trackCount + " tracks, $" + 
                                 analytics.totalRevenue + " revenue");
            }
            
        } catch (Exception e) {
            System.err.println("!!! Data colocation processing failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrates advanced MapReduce workflows with custom aggregation.
     */
    private void demonstrateAdvancedMapReduce(IgniteClient client) {
        System.out.println("\n--- Advanced MapReduce Workflows");
        System.out.println(">>> Executing distributed map-reduce for genre popularity analysis");
        
        try {
            // Map phase: Distribute genre analysis across nodes
            JobDescriptor<Void, String> mapJob = 
                JobDescriptor.<Void, String>builder(GenreMapJob.class)
                    .units(ComputeJobDeployment.getDeploymentUnits())
                    .resultClass(String.class)
                    .build();
            
            Collection<String> nodeNames = client.clusterNodes().stream()
                .map(node -> node.name())
                .collect(Collectors.toList());
            
            Map<String, CompletableFuture<String>> mapResults = new HashMap<>();
            
            for (String nodeName : nodeNames) {
                CompletableFuture<String> future = client.compute()
                    .executeAsync(JobTarget.anyNode(client.clusterNodes()), mapJob, null);
                mapResults.put(nodeName, future);
            }
            
            // Reduce phase: Aggregate results from all nodes
            Map<String, GenreMetrics> aggregatedMetrics = new HashMap<>();
            
            for (Map.Entry<String, CompletableFuture<String>> entry : mapResults.entrySet()) {
                String nodeResults = entry.getValue().get();
                
                // Parse JSON and merge results
                parseAndMergeGenreMetrics(nodeResults, aggregatedMetrics);
            }
            
            System.out.println("<<< MapReduce analysis completed for " + aggregatedMetrics.size() + " genres");
            
            // Display top 3 genres by track count
            aggregatedMetrics.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().trackCount, e1.getValue().trackCount))
                .limit(3)
                .forEach(entry -> {
                    GenreMetrics metrics = entry.getValue();
                    System.out.println("         " + entry.getKey() + ": " + 
                                     metrics.trackCount + " tracks, $" + 
                                     metrics.totalRevenue + " revenue");
                });
            
        } catch (Exception e) {
            System.err.println("!!! MapReduce processing failed: " + e.getMessage());
        }
    }
    
    private void parseAndMergeGenreMetrics(String json, Map<String, GenreMetrics> aggregatedMetrics) {
        // Find genres object
        int genresStart = json.indexOf("\"genres\":{");
        if (genresStart < 0) return;
        
        int start = genresStart + 11;
        int end = json.lastIndexOf("}}");
        if (end < start) return;
        
        String genresSection = json.substring(start, end);
        
        // Parse each genre
        int pos = 0;
        while (pos < genresSection.length()) {
            // Find genre name
            int nameStart = genresSection.indexOf("\"", pos);
            if (nameStart < 0) break;
            int nameEnd = genresSection.indexOf("\":", nameStart + 1);
            if (nameEnd < 0) break;
            
            String genreName = genresSection.substring(nameStart + 1, nameEnd);
            
            // Find track count
            int trackCountPos = genresSection.indexOf("\"trackCount\":", nameEnd);
            if (trackCountPos < 0) break;
            int trackCountStart = trackCountPos + 13;
            int trackCountEnd = genresSection.indexOf(",", trackCountStart);
            int trackCount = Integer.parseInt(genresSection.substring(trackCountStart, trackCountEnd).trim());
            
            // Find revenue
            int revenuePos = genresSection.indexOf("\"totalRevenue\":", trackCountEnd);
            if (revenuePos < 0) break;
            int revenueStart = revenuePos + 15;
            int revenueEnd = genresSection.indexOf("}", revenueStart);
            BigDecimal revenue = new BigDecimal(genresSection.substring(revenueStart, revenueEnd).trim());
            
            // Merge into aggregated results
            GenreMetrics newMetrics = new GenreMetrics(trackCount, revenue);
            aggregatedMetrics.merge(genreName, newMetrics, GenreMetrics::merge);
            
            // Move to next genre
            pos = revenueEnd + 1;
        }
    }

    /**
     * Demonstrates circuit breaker patterns for compute resilience.
     */
    private void demonstrateComputeResilience(IgniteClient client) {
        System.out.println("\n--- Compute Resilience Patterns");
        System.out.println(">>> Implementing circuit breaker for job protection");
        
        try {
            ComputeCircuitBreaker circuitBreaker = new ComputeCircuitBreaker();
            
            // Test resilient job execution
            for (int i = 1; i <= 3; i++) {
                try {
                    CompletableFuture<String> result = circuitBreaker.executeJob(
                        client,
                        JobDescriptor.builder(ResilientAnalyticsJob.class)
                            .units(ComputeJobDeployment.getDeploymentUnits())
                            .build(),
                        "analysis-" + i
                    );
                    
                    String output = result.get();
                    System.out.println("<<< Resilient job " + i + ": " + output);
                    
                } catch (Exception e) {
                    System.err.println("!!! Resilient job " + i + " failed: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.err.println("!!! Circuit breaker demonstration failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrates performance monitoring and job lifecycle management.
     */
    private void demonstratePerformanceMonitoring(IgniteClient client) {
        System.out.println("\n--- Performance Monitoring");
        System.out.println(">>> Tracking job execution metrics and resource utilization");
        
        try {
            JobPerformanceMonitor monitor = new JobPerformanceMonitor();
            
            // Execute monitored job
            JobDescriptor<Void, String> monitoredJob = JobDescriptor.builder(PerformanceTestJob.class)
                .units(ComputeJobDeployment.getDeploymentUnits())
                .build();
            
            CompletableFuture<String> result = monitor.executeWithMonitoring(
                client, monitoredJob, null);
            
            String output = result.get();
            JobMetrics metrics = monitor.getLastExecutionMetrics();
            
            System.out.println("<<< Job execution completed: " + output);
            System.out.println("<<< Performance metrics:");
            System.out.println("         Execution time: " + metrics.executionTimeMs + "ms");
            System.out.println("         Target node: " + metrics.targetNode);
            System.out.println("         Memory usage: " + metrics.memoryUsageMB + "MB");
            
        } catch (Exception e) {
            System.err.println("!!! Performance monitoring failed: " + e.getMessage());
        }
    }

    /**
     * Process artist-specific analytics with data colocation.
     */
    private CompletableFuture<ArtistAnalytics> processArtistAnalytics(IgniteClient client, Integer artistId) {
        JobDescriptor<Integer, String> job = JobDescriptor.<Integer, String>builder(ArtistAnalyticsJob.class)
            .units(ComputeJobDeployment.getDeploymentUnits())
            .resultClass(String.class)
            .build();
        
        // Use colocation to run job where artist data resides
        Tuple artistKey = Tuple.create(Map.of("ArtistId", artistId));
        return client.compute().executeAsync(
            JobTarget.colocated("Artist", artistKey), job, artistId)
            .thenApply(json -> {
                // Parse JSON back to ArtistAnalytics
                return parseArtistAnalytics(json);
            });
    }
    
    private ArtistAnalytics parseArtistAnalytics(String json) {
        // Simple JSON parsing
        int artistId = extractIntValue(json, "artistId");
        String artistName = extractStringValue(json, "artistName");
        int albumCount = extractIntValue(json, "albumCount");
        int trackCount = extractIntValue(json, "trackCount");
        BigDecimal totalRevenue = new BigDecimal(extractDecimalValue(json, "totalRevenue"));
        
        return new ArtistAnalytics(artistId, artistName, albumCount, trackCount, totalRevenue);
    }
    
    private int extractIntValue(String json, String key) {
        int keyPos = json.indexOf("\"" + key + "\":");
        if (keyPos < 0) return 0;
        int start = keyPos + key.length() + 3;
        int end = json.indexOf(",", start);
        if (end < 0) end = json.indexOf("}", start);
        return Integer.parseInt(json.substring(start, end).trim());
    }
    
    private String extractStringValue(String json, String key) {
        int keyPos = json.indexOf("\"" + key + "\":\"");
        if (keyPos < 0) return "";
        int start = keyPos + key.length() + 5;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
    
    private String extractDecimalValue(String json, String key) {
        int keyPos = json.indexOf("\"" + key + "\":");
        if (keyPos < 0) return "0";
        int start = keyPos + key.length() + 3;
        int end = json.indexOf("}", start);
        return json.substring(start, end).trim();
    }

    // Production job implementations

    /**
     * Recommendation processing job for large-scale user analysis.
     */
    public static class RecommendationEngineJob implements ComputeJob<Integer, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Integer batchSize) {
            return CompletableFuture.supplyAsync(() -> {
                IgniteSql sql = context.ignite().sql();
                
                // Process user purchasing patterns by joining Invoice and InvoiceLine tables
                Statement stmt = sql.statementBuilder()
                    .query("SELECT i.CustomerId, COUNT(il.InvoiceLineId) as purchase_count " +
                           "FROM InvoiceLine il " +
                           "JOIN Invoice i ON il.InvoiceId = i.InvoiceId " +
                           "GROUP BY i.CustomerId " +
                           "LIMIT ?")
                    .build();
                
                List<UserRecommendation> recommendations = new ArrayList<>();
                
                try (ResultSet<SqlRow> result = sql.execute(null, stmt, batchSize)) {
                    while (result.hasNext()) {
                        SqlRow row = result.next();
                        int customerId = row.intValue("CUSTOMERID");
                        long purchaseCount = row.longValue("PURCHASE_COUNT");
                        
                        // Generate personalized recommendations based on purchase history
                        UserRecommendation recommendation = generateRecommendation(sql, customerId, purchaseCount);
                        recommendations.add(recommendation);
                    }
                }
                
                // Convert to JSON-like string for serialization
                StringBuilder json = new StringBuilder("{\"recommendations\":[");
                for (int i = 0; i < recommendations.size(); i++) {
                    if (i > 0) json.append(",");
                    UserRecommendation rec = recommendations.get(i);
                    json.append("{\"customerId\":").append(rec.customerId)
                        .append(",\"genres\":[");
                    for (int j = 0; j < rec.recommendedGenres.size(); j++) {
                        if (j > 0) json.append(",");
                        json.append("\"").append(rec.recommendedGenres.get(j)).append("\"");
                    }
                    json.append("],\"purchaseHistory\":").append(rec.purchaseHistory).append("}");
                }
                json.append("],\"processingNode\":\"").append(context.ignite().name()).append("\"}");
                return json.toString();
            });
        }
        
        private UserRecommendation generateRecommendation(IgniteSql sql, int customerId, long purchaseCount) {
            // Analyze user's genre preferences
            Statement genreStmt = sql.statementBuilder()
                .query("SELECT g.Name, COUNT(*) as genre_count FROM Genre g " +
                       "JOIN Track t ON g.GenreId = t.GenreId " +
                       "JOIN InvoiceLine il ON t.TrackId = il.TrackId " +
                       "JOIN Invoice i ON il.InvoiceId = i.InvoiceId " +
                       "WHERE i.CustomerId = ? " +
                       "GROUP BY g.GenreId, g.Name ORDER BY genre_count DESC LIMIT 3")
                .build();
            
            List<String> preferredGenres = new ArrayList<>();
            try (ResultSet<SqlRow> genreResult = sql.execute(null, genreStmt, customerId)) {
                while (genreResult.hasNext()) {
                    preferredGenres.add(genreResult.next().stringValue("NAME"));
                }
            }
            
            return new UserRecommendation(customerId, preferredGenres, (int) purchaseCount);
        }
    }

    /**
     * Artist analytics job with data colocation optimization.
     */
    public static class ArtistAnalyticsJob implements ComputeJob<Integer, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Integer artistId) {
            return CompletableFuture.supplyAsync(() -> {
                IgniteSql sql = context.ignite().sql();
                
                // Get artist analytics with colocated data access
                Statement stmt = sql.statementBuilder()
                    .query("SELECT " +
                           "  a.ArtistId, " +
                           "  a.Name, " +
                           "  COUNT(DISTINCT al.AlbumId) as album_count, " +
                           "  COUNT(DISTINCT t.TrackId) as track_count, " +
                           "  SUM(il.UnitPrice * il.Quantity) as total_revenue " +
                           "FROM Artist a " +
                           "LEFT JOIN Album al ON a.ArtistId = al.ArtistId " +
                           "LEFT JOIN Track t ON al.AlbumId = t.AlbumId " +
                           "LEFT JOIN InvoiceLine il ON t.TrackId = il.TrackId " +
                           "WHERE a.ArtistId = ? " +
                           "GROUP BY a.ArtistId, a.Name")
                    .build();
                
                try (ResultSet<SqlRow> result = sql.execute(null, stmt, artistId)) {
                    if (result.hasNext()) {
                        SqlRow row = result.next();
                        String name = row.stringValue("NAME");
                        int albumCount = (int) row.longValue("ALBUM_COUNT");
                        int trackCount = (int) row.longValue("TRACK_COUNT");
                        BigDecimal revenue = row.decimalValue("TOTAL_REVENUE") != null ? 
                            row.decimalValue("TOTAL_REVENUE") : BigDecimal.ZERO;
                        
                        // Return JSON string for serialization
                        return "{\"artistId\":" + artistId + 
                               ",\"artistName\":\"" + name + "\"" +
                               ",\"albumCount\":" + albumCount +
                               ",\"trackCount\":" + trackCount +
                               ",\"totalRevenue\":" + revenue + "}";
                    }
                }
                
                return "{\"artistId\":" + artistId + ",\"artistName\":\"Unknown\",\"albumCount\":0,\"trackCount\":0,\"totalRevenue\":0}";
            });
        }
    }

    /**
     * Genre mapping job for MapReduce pattern.
     */
    public static class GenreMapJob implements ComputeJob<Void, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Void arg) {
            return CompletableFuture.supplyAsync(() -> {
                IgniteSql sql = context.ignite().sql();
                StringBuilder json = new StringBuilder("{\"genres\":{");
                
                Statement stmt = sql.statementBuilder()
                    .query("SELECT " +
                           "  g.Name, " +
                           "  COUNT(t.TrackId) as track_count, " +
                           "  SUM(il.UnitPrice * il.Quantity) as total_revenue " +
                           "FROM Genre g " +
                           "JOIN Track t ON g.GenreId = t.GenreId " +
                           "LEFT JOIN InvoiceLine il ON t.TrackId = il.TrackId " +
                           "GROUP BY g.GenreId, g.Name")
                    .build();
                
                boolean first = true;
                try (ResultSet<SqlRow> result = sql.execute(null, stmt)) {
                    while (result.hasNext()) {
                        SqlRow row = result.next();
                        String genreName = row.stringValue("NAME");
                        int trackCount = (int) row.longValue("TRACK_COUNT");
                        BigDecimal revenue = row.decimalValue("TOTAL_REVENUE") != null ? 
                            row.decimalValue("TOTAL_REVENUE") : BigDecimal.ZERO;
                        
                        if (!first) json.append(",");
                        first = false;
                        
                        json.append("\"").append(genreName).append("\":{");
                        json.append("\"trackCount\":").append(trackCount);
                        json.append(",\"totalRevenue\":").append(revenue);
                        json.append("}");
                    }
                }
                
                json.append("}}");
                return json.toString();
            });
        }
    }

    /**
     * Resilient analytics job for circuit breaker testing.
     */
    public static class ResilientAnalyticsJob implements ComputeJob<String, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, String analysisType) {
            return CompletableFuture.supplyAsync(() -> {
                IgniteSql sql = context.ignite().sql();
                
                // Simulate analytics processing
                Statement stmt = sql.statementBuilder()
                    .query("SELECT COUNT(*) as total_records FROM Artist")
                    .build();
                
                try (ResultSet<SqlRow> result = sql.execute(null, stmt)) {
                    if (result.hasNext()) {
                        long count = result.next().longValue("TOTAL_RECORDS");
                        return analysisType + " completed: " + count + " artists analyzed";
                    }
                }
                
                return analysisType + " completed: no data found";
            });
        }
    }

    /**
     * Performance test job for monitoring demonstration.
     */
    public static class PerformanceTestJob implements ComputeJob<Void, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Void arg) {
            return CompletableFuture.supplyAsync(() -> {
                long startTime = System.currentTimeMillis();
                
                IgniteSql sql = context.ignite().sql();
                
                // Simulate compute-intensive operation
                Statement stmt = sql.statementBuilder()
                    .query("SELECT COUNT(*) as track_count, AVG(UnitPrice) as avg_price FROM Track")
                    .build();
                
                try (ResultSet<SqlRow> result = sql.execute(null, stmt)) {
                    if (result.hasNext()) {
                        SqlRow row = result.next();
                        long executionTime = System.currentTimeMillis() - startTime;
                        
                        return "Performance test completed in " + executionTime + "ms: " +
                               row.longValue("TRACK_COUNT") + " tracks, avg price $" +
                               row.decimalValue("AVG_PRICE");
                    }
                }
                
                return "Performance test completed with no data";
            });
        }
    }

    // Supporting classes for production patterns

    /**
     * Recommendation processor for large-scale user analysis.
     */
    private static class RecommendationProcessor {
        public CompletableFuture<RecommendationReport> processRecommendations(IgniteClient client) {
            long startTime = System.currentTimeMillis();
            
            JobDescriptor<Integer, String> job = JobDescriptor.<Integer, String>builder(RecommendationEngineJob.class)
                .units(ComputeJobDeployment.getDeploymentUnits())
                .resultClass(String.class)
                .build();
            
            // Distribute processing across cluster nodes
            Collection<String> nodeNames = client.clusterNodes().stream()
                .map(node -> node.name())
                .limit(3) // Limit to 3 nodes for demo
                .collect(Collectors.toList());
            
            List<CompletableFuture<String>> futures = nodeNames.stream()
                .map(nodeName -> client.compute().executeAsync(
                    JobTarget.anyNode(client.clusterNodes()), job, 100))
                .collect(Collectors.toList());
            
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    int totalUsers = 0;
                    int totalRecommendations = 0;
                    
                    for (CompletableFuture<String> future : futures) {
                        String jsonResult = future.join();
                        // Parse JSON response
                        totalUsers += parseUserCount(jsonResult);
                        totalRecommendations += parseRecommendationCount(jsonResult);
                    }
                    
                    long processingTime = System.currentTimeMillis() - startTime;
                    
                    return new RecommendationReport(
                        totalUsers,
                        totalRecommendations,
                        processingTime,
                        nodeNames.size()
                    );
                });
        }
        
        private int parseUserCount(String json) {
            // Simple JSON parsing for user count
            int start = json.indexOf("\"recommendations\":[");
            if (start < 0) return 0;
            
            int count = 0;
            int pos = start;
            while ((pos = json.indexOf("{\"customerId\":", pos + 1)) > 0) {
                count++;
            }
            return count;
        }
        
        private int parseRecommendationCount(String json) {
            // Simple JSON parsing for total recommendation count
            int count = 0;
            int pos = 0;
            while ((pos = json.indexOf("\"genres\":[", pos + 1)) > 0) {
                int genreStart = pos + 11;
                int genreEnd = json.indexOf("]", genreStart);
                if (genreEnd > genreStart) {
                    String genreSection = json.substring(genreStart, genreEnd);
                    // Count commas + 1 for number of genres
                    count += genreSection.split(",").length;
                }
            }
            return count;
        }
    }

    /**
     * Circuit breaker for compute job protection.
     */
    private static class ComputeCircuitBreaker {
        private enum State { CLOSED, OPEN, HALF_OPEN }
        
        private State state = State.CLOSED;
        private int failureCount = 0;
        private long lastFailureTime = 0;
        private final int failureThreshold = 3;
        private final long timeoutDuration = 5000; // 5 seconds
        
        public <T, R> CompletableFuture<R> executeJob(
                IgniteClient client,
                JobDescriptor<T, R> jobDescriptor,
                T arg) {
            
            if (state == State.OPEN) {
                if (System.currentTimeMillis() - lastFailureTime > timeoutDuration) {
                    state = State.HALF_OPEN;
                } else {
                    return CompletableFuture.failedFuture(
                        new RuntimeException("Circuit breaker is OPEN"));
                }
            }
            
            return client.compute()
                .executeAsync(JobTarget.anyNode(client.clusterNodes()), jobDescriptor, arg)
                .thenApply(result -> {
                    onSuccess();
                    return result;
                })
                .exceptionally(throwable -> {
                    onFailure();
                    throw new RuntimeException(throwable);
                });
        }
        
        private synchronized void onSuccess() {
            failureCount = 0;
            state = State.CLOSED;
        }
        
        private synchronized void onFailure() {
            failureCount++;
            lastFailureTime = System.currentTimeMillis();
            
            if (failureCount >= failureThreshold) {
                state = State.OPEN;
            }
        }
    }

    /**
     * Performance monitor for job execution tracking.
     */
    private static class JobPerformanceMonitor {
        private JobMetrics lastMetrics;
        
        public <T, R> CompletableFuture<R> executeWithMonitoring(
                IgniteClient client,
                JobDescriptor<T, R> jobDescriptor,
                T arg) {
            
            long startTime = System.currentTimeMillis();
            Runtime runtime = Runtime.getRuntime();
            long startMemory = runtime.totalMemory() - runtime.freeMemory();
            
            String targetNode = client.clusterNodes().stream()
                .findFirst()
                .map(node -> node.name())
                .orElse("unknown");
            
            return client.compute()
                .executeAsync(JobTarget.anyNode(client.clusterNodes()), jobDescriptor, arg)
                .thenApply(result -> {
                    long executionTime = System.currentTimeMillis() - startTime;
                    long endMemory = runtime.totalMemory() - runtime.freeMemory();
                    long memoryUsage = (endMemory - startMemory) / (1024 * 1024); // MB
                    
                    lastMetrics = new JobMetrics(executionTime, targetNode, memoryUsage);
                    return result;
                });
        }
        
        public JobMetrics getLastExecutionMetrics() {
            return lastMetrics;
        }
    }

    // Data transfer objects

    public static class RecommendationReport implements Serializable {
        private static final long serialVersionUID = 1L;
        public final int usersProcessed;
        public final int recommendationsGenerated;
        public final long processingTimeMs;
        public final int nodesUtilized;
        
        public RecommendationReport(int usersProcessed, int recommendationsGenerated, 
                                  long processingTimeMs, int nodesUtilized) {
            this.usersProcessed = usersProcessed;
            this.recommendationsGenerated = recommendationsGenerated;
            this.processingTimeMs = processingTimeMs;
            this.nodesUtilized = nodesUtilized;
        }
    }

    public static class UserRecommendations implements Serializable {
        private static final long serialVersionUID = 1L;
        public final List<UserRecommendation> recommendations;
        public final String processingNode;
        
        public UserRecommendations(List<UserRecommendation> recommendations, String processingNode) {
            this.recommendations = recommendations;
            this.processingNode = processingNode;
        }
    }

    public static class UserRecommendation implements Serializable {
        private static final long serialVersionUID = 1L;
        public final int customerId;
        public final List<String> recommendedGenres;
        public final int purchaseHistory;
        
        public UserRecommendation(int customerId, List<String> recommendedGenres, int purchaseHistory) {
            this.customerId = customerId;
            this.recommendedGenres = recommendedGenres;
            this.purchaseHistory = purchaseHistory;
        }
    }

    public static class ArtistAnalytics implements Serializable {
        private static final long serialVersionUID = 1L;
        public final int artistId;
        public final String artistName;
        public final int albumCount;
        public final int trackCount;
        public final BigDecimal totalRevenue;
        
        public ArtistAnalytics(int artistId, String artistName, int albumCount, 
                             int trackCount, BigDecimal totalRevenue) {
            this.artistId = artistId;
            this.artistName = artistName;
            this.albumCount = albumCount;
            this.trackCount = trackCount;
            this.totalRevenue = totalRevenue;
        }
    }

    public static class GenreMetrics implements Serializable {
        private static final long serialVersionUID = 1L;
        public final int trackCount;
        public final BigDecimal totalRevenue;
        
        public GenreMetrics(int trackCount, BigDecimal totalRevenue) {
            this.trackCount = trackCount;
            this.totalRevenue = totalRevenue;
        }
        
        public static GenreMetrics merge(GenreMetrics a, GenreMetrics b) {
            return new GenreMetrics(
                a.trackCount + b.trackCount,
                a.totalRevenue.add(b.totalRevenue)
            );
        }
    }

    public static class JobMetrics implements Serializable {
        private static final long serialVersionUID = 1L;
        public final long executionTimeMs;
        public final String targetNode;
        public final long memoryUsageMB;
        
        public JobMetrics(long executionTimeMs, String targetNode, long memoryUsageMB) {
            this.executionTimeMs = executionTimeMs;
            this.targetNode = targetNode;
            this.memoryUsageMB = memoryUsageMB;
        }
    }
}