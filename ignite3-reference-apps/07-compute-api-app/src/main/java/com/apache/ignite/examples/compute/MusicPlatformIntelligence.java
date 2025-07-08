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
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Demonstrates advanced music platform intelligence patterns using Apache Ignite 3 Compute API.
 * 
 * Implements the enhanced code examples from the documentation showing real-world music
 * streaming platform patterns including distributed recommendation processing, MapReduce
 * analytics, and resilient job execution.
 * 
 * Key patterns from documentation:
 * - ArtistPopularityJob - demonstrates basic distributed data processing
 * - UserRecommendationJob - shows data locality optimization
 * - DistributedRecommendationProcessor - concurrent user processing
 * - MusicTrendMapReduceExample - platform-wide analytics
 * - ResilientMusicJobProcessor - production error handling
 * 
 * Prerequisites:
 * - Running Ignite cluster
 * - Sample music store data loaded
 */
public class MusicPlatformIntelligence {

    private static final Logger logger = LoggerFactory.getLogger(MusicPlatformIntelligence.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Music Platform Intelligence Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Demonstrating documentation-aligned compute patterns");

        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            MusicPlatformIntelligence demo = new MusicPlatformIntelligence();
            demo.runMusicPlatformIntelligence(client);
            
        } catch (Exception e) {
            logger.error("Failed to run music platform intelligence demo", e);
            System.err.println("!!! Error: " + e.getMessage());
        }
    }

    private void runMusicPlatformIntelligence(IgniteClient client) {
        System.out.println("\n--- Music Platform Intelligence Patterns ---");
        System.out.println("    Implementing documentation examples");
        
        // Deploy job classes for execution
        if (!ComputeJobDeployment.deployJobClasses()) {
            System.out.println(">>> Continuing with development deployment units");
        }
        
        // Basic artist popularity analysis
        demonstrateArtistPopularityAnalysis(client);
        
        // User recommendation processing
        demonstrateUserRecommendationProcessing(client);
        
        // Concurrent user processing
        demonstrateConcurrentUserProcessing(client);
        
        // MapReduce trend analysis
        demonstrateMapReduceTrendAnalysis(client);
        
        // Resilient job processing
        demonstrateResilientJobProcessing(client);
        
        System.out.println("\n>>> Music platform intelligence patterns completed successfully");
    }

    /**
     * Demonstrates artist popularity analysis from documentation example.
     */
    private void demonstrateArtistPopularityAnalysis(IgniteClient client) {
        System.out.println("\n--- Artist Popularity Analysis");
        System.out.println(">>> Executing distributed artist analytics");
        
        try {
            JobDescriptor<Void, String> job = 
                JobDescriptor.<Void, String>builder(ArtistPopularityJob.class)
                    .units(ComputeJobDeployment.getDeploymentUnits())
                    .resultClass(String.class)
                    .build();
            
            String jsonResults = client.compute()
                .execute(JobTarget.anyNode(client.clusterNodes()), job, null);
            
            // System.out.println(">>> Debug: Raw JSON result: " + jsonResults);
            Map<String, Long> results = parseArtistPopularityJson(jsonResults);
            
            System.out.println("<<< Artist popularity analysis completed:");
            results.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> 
                    System.out.println("         " + entry.getKey() + ": " + entry.getValue() + " plays"));
            
        } catch (Exception e) {
            System.err.println("!!! Artist popularity analysis failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrates user recommendation processing with data locality.
     */
    private void demonstrateUserRecommendationProcessing(IgniteClient client) {
        System.out.println("\n--- User Recommendation Processing");
        System.out.println(">>> Processing user recommendations with data locality");
        
        try {
            int customerId = 1;
            
            // Execute recommendation processing on the node containing user data
            Tuple userKey = Tuple.create(Map.of("CustomerId", customerId));
            JobTarget target = JobTarget.colocated("Customer", userKey);
            
            JobDescriptor<Integer, String> job = 
                JobDescriptor.<Integer, String>builder(UserRecommendationJob.class)
                    .units(ComputeJobDeployment.getDeploymentUnits())
                    .resultClass(String.class)
                    .build();
            
            String jsonRecommendations = client.compute()
                .execute(target, job, customerId);
            
            // System.out.println(">>> Debug: Raw recommendations JSON: " + jsonRecommendations);
            List<String> recommendations = parseRecommendationsJson(jsonRecommendations);
            
            System.out.println("<<< User " + customerId + " recommendations (" + recommendations.size() + " items): " + String.join(", ", recommendations));
            
        } catch (Exception e) {
            System.err.println("!!! User recommendation processing failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrates concurrent user processing as shown in documentation.
     */
    private void demonstrateConcurrentUserProcessing(IgniteClient client) {
        System.out.println("\n--- Concurrent User Processing");
        System.out.println(">>> Processing multiple users concurrently");
        
        try {
            DistributedRecommendationProcessor processor = 
                new DistributedRecommendationProcessor(client);
            
            // Process recommendations for 10 users concurrently
            List<Integer> userIds = IntStream.range(1, 11).boxed().collect(Collectors.toList());
            
            CompletableFuture<Map<Integer, List<String>>> allRecommendations = 
                processor.processUserRecommendations(userIds);
            
            Map<Integer, List<String>> results = allRecommendations.get();
            
            System.out.println("<<< Processed recommendations for " + results.size() + " users");
            results.entrySet().stream()
                .limit(3)
                .forEach(entry -> 
                    System.out.println("         User " + entry.getKey() + ": " + entry.getValue()));
            
        } catch (Exception e) {
            System.err.println("!!! Concurrent user processing failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrates MapReduce trend analysis from documentation.
     */
    private void demonstrateMapReduceTrendAnalysis(IgniteClient client) {
        System.out.println("\n--- MapReduce Trend Analysis");
        System.out.println(">>> Executing distributed trend detection");
        
        try {
            MusicTrendMapReduceExample mapReduce = new MusicTrendMapReduceExample();
            Map<String, Long> globalTrends = mapReduce.detectGlobalTrends(client);
            
            System.out.println("<<< Global trending tracks:");
            globalTrends.entrySet().stream()
                .limit(5)
                .forEach(entry -> 
                    System.out.println("         " + entry.getKey() + ": " + entry.getValue() + " plays"));
            
        } catch (Exception e) {
            System.err.println("!!! MapReduce trend analysis failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrates resilient job processing with retry logic.
     */
    private void demonstrateResilientJobProcessing(IgniteClient client) {
        System.out.println("\n--- Resilient Job Processing");
        System.out.println(">>> Testing retry logic and error handling");
        
        try {
            ResilientMusicJobProcessor processor = new ResilientMusicJobProcessor(client);
            
            JobDescriptor<String, String> job = JobDescriptor.builder(RecommendationAnalysisJob.class)
                .units(ComputeJobDeployment.getDeploymentUnits())
                .build();
            
            CompletableFuture<String> result = processor.executeWithResilience(
                JobTarget.anyNode(client.clusterNodes()), job, "genre-analysis");
            
            String output = result.get();
            System.out.println("<<< Resilient job completed: " + output);
            
        } catch (Exception e) {
            System.err.println("!!! Resilient job processing failed: " + e.getMessage());
        }
    }

    // Job implementations from documentation examples

    /**
     * Artist popularity analysis job as shown in documentation.
     */
    public static class ArtistPopularityJob implements ComputeJob<Void, String>, Serializable {
        private static final long serialVersionUID = 1L;
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Void input) {
            return CompletableFuture.supplyAsync(() -> {
                // Execute analysis using local data on this node
                IgniteSql sql = context.ignite().sql();
                Statement stmt = sql.statementBuilder()
                    .query("SELECT a.Name, COUNT(*) as play_count " +
                           "FROM Artist a JOIN Album al ON a.ArtistId = al.ArtistId " +
                           "JOIN Track t ON al.AlbumId = t.AlbumId " + 
                           "JOIN InvoiceLine il ON t.TrackId = il.TrackId " +
                           "GROUP BY a.ArtistId, a.Name")
                    .build();
                
                StringBuilder json = new StringBuilder("{");
                boolean first = true;
                try (ResultSet<SqlRow> rs = sql.execute(null, stmt)) {
                    while (rs.hasNext()) {
                        SqlRow row = rs.next();
                        if (!first) json.append(",");
                        first = false;
                        String name = row.stringValue("NAME").replace("\"", "\\\"");
                        json.append("\"").append(name).append("\":");
                        json.append(row.longValue("PLAY_COUNT"));
                    }
                }
                json.append("}");
                return json.toString();
            });
        }
    }

    /**
     * User recommendation job with data locality optimization.
     */
    public static class UserRecommendationJob implements ComputeJob<Integer, String>, Serializable {
        private static final long serialVersionUID = 1L;
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Integer customerId) {
            return CompletableFuture.supplyAsync(() -> {
                IgniteSql sql = context.ignite().sql();
                
                // Analyze local user listening patterns (no network access required)
                Statement stmt = sql.statementBuilder()
                    .query("SELECT g.Name as genre, COUNT(*) as play_count " +
                           "FROM Customer c " +
                           "JOIN Invoice i ON c.CustomerId = i.CustomerId " +
                           "JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId " +
                           "JOIN Track t ON il.TrackId = t.TrackId " +
                           "JOIN Genre g ON t.GenreId = g.GenreId " +
                           "WHERE c.CustomerId = ? " +
                           "GROUP BY g.GenreId, g.Name " +
                           "ORDER BY play_count DESC LIMIT 3")
                    .build();
                
                StringBuilder json = new StringBuilder("[");
                boolean hasResults = false;
                try (ResultSet<SqlRow> rs = sql.execute(null, stmt, customerId)) {
                    while (rs.hasNext()) {
                        if (hasResults) json.append(",");
                        hasResults = true;
                        SqlRow row = rs.next();
                        String genre = row.stringValue("GENRE").replace("\"", "\\\"");
                        json.append("\"").append(genre).append("\"");
                    }
                }
                
                if (!hasResults) {
                    // Default recommendations
                    json.append("\"Rock\",\"Pop\",\"Jazz\"");
                }
                
                json.append("]");
                return json.toString();
            });
        }
    }

    /**
     * Trend map job for MapReduce pattern.
     */
    public static class TrendMapJob implements ComputeJob<Void, String>, Serializable {
        private static final long serialVersionUID = 1L;
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Void input) {
            return CompletableFuture.supplyAsync(() -> {
                IgniteSql sql = context.ignite().sql();
                
                // Analyze local listening trends from past week
                Statement stmt = sql.statementBuilder()
                    .query("SELECT t.Name, COUNT(*) as plays " +
                           "FROM Track t " +
                           "JOIN InvoiceLine il ON t.TrackId = il.TrackId " +
                           "JOIN Invoice i ON il.InvoiceId = i.InvoiceId " +
                           "WHERE i.InvoiceDate >= CURRENT_DATE - INTERVAL '7' DAY " +
                           "GROUP BY t.TrackId, t.Name " +
                           "HAVING COUNT(*) > 5")  // Filter noise
                    .build();
                
                StringBuilder json = new StringBuilder("{");
                boolean first = true;
                try (ResultSet<SqlRow> rs = sql.execute(null, stmt)) {
                    while (rs.hasNext()) {
                        SqlRow row = rs.next();
                        if (!first) json.append(",");
                        first = false;
                        String name = row.stringValue("NAME").replace("\"", "\\\"");
                        json.append("\"").append(name).append("\":");
                        json.append(row.longValue("PLAYS"));
                    }
                }
                json.append("}");
                return json.toString();
            });
        }
    }

    /**
     * Simple recommendation analysis job for resilience testing.
     */
    public static class RecommendationAnalysisJob implements ComputeJob<String, String>, Serializable {
        private static final long serialVersionUID = 1L;
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, String analysisType) {
            return CompletableFuture.supplyAsync(() -> {
                IgniteSql sql = context.ignite().sql();
                
                Statement stmt = sql.statementBuilder()
                    .query("SELECT COUNT(*) as total_genres FROM Genre")
                    .build();
                
                try (ResultSet<SqlRow> rs = sql.execute(null, stmt)) {
                    if (rs.hasNext()) {
                        long count = rs.next().longValue("TOTAL_GENRES");
                        return analysisType + " completed: " + count + " genres analyzed";
                    }
                }
                
                return analysisType + " completed: no data found";
            });
        }
    }

    // Supporting classes from documentation

    /**
     * Distributed recommendation processor for concurrent user processing.
     */
    public static class DistributedRecommendationProcessor {
        private final IgniteClient client;
        
        public DistributedRecommendationProcessor(IgniteClient client) {
            this.client = client;
        }
        
        // Process recommendations for multiple users concurrently
        public CompletableFuture<Map<Integer, List<String>>> processUserRecommendations(
                List<Integer> userIds) {
            
            // Start all user recommendation jobs in parallel
            List<CompletableFuture<Pair<Integer, List<String>>>> futures = userIds.stream()
                .map(this::processUserRecommendationAsync)
                .collect(Collectors.toList());
            
            // Combine all results when complete
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toMap(
                        Pair::getKey,    // userId
                        Pair::getValue   // recommendations
                    )));
        }
        
        private CompletableFuture<Pair<Integer, List<String>>> processUserRecommendationAsync(
                Integer userId) {
            
            // Target job to node containing user data
            Tuple userKey = Tuple.create(Map.of("CustomerId", userId));
            JobTarget target = JobTarget.colocated("Customer", userKey);
            
            // Execute recommendation job asynchronously
            return client.compute()
                .executeAsync(target, 
                    JobDescriptor.<Integer, String>builder(UserRecommendationJob.class)
                        .units(ComputeJobDeployment.getDeploymentUnits())
                        .resultClass(String.class)
                        .build(), userId)
                .thenApply(jsonRecommendations -> {
                    List<String> recommendations = MusicPlatformIntelligence.parseRecommendationsJson(jsonRecommendations);
                    return new Pair<>(userId, recommendations);
                });
        }
    }

    /**
     * MapReduce example for music trend detection.
     */
    public static class MusicTrendMapReduceExample {
        
        // Orchestrate MapReduce workflow
        public Map<String, Long> detectGlobalTrends(IgniteClient client) {
            // Map Phase: Execute trend analysis on all nodes
            JobDescriptor<Void, String> mapJob = 
                JobDescriptor.<Void, String>builder(TrendMapJob.class)
                    .units(ComputeJobDeployment.getDeploymentUnits())
                    .resultClass(String.class)
                    .build();
            
            // Execute job on any node for simplicity (would normally use broadcast)
            String jsonResult = 
                client.compute().execute(JobTarget.anyNode(client.clusterNodes()), mapJob, null);
            
            Map<String, Long> singleNodeResult = parseTrendJson(jsonResult);
            
            // In a real implementation, this would aggregate results from multiple nodes
            Collection<Map<String, Long>> mapResults = List.of(singleNodeResult);
            
            // Reduce Phase: Aggregate all node results
            Map<String, Long> globalTrends = new HashMap<>();
            for (Map<String, Long> nodeResult : mapResults) {
                nodeResult.forEach((trackName, localPlays) -> 
                    globalTrends.merge(trackName, localPlays, Long::sum));
            }
            
            // Return top trending tracks globally
            return globalTrends.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(50)  // Top 50 trending tracks
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));
        }
    }

    /**
     * Resilient job processor with retry logic.
     */
    public static class ResilientMusicJobProcessor {
        private final IgniteClient client;
        private final int maxRetryAttempts = 3;
        private final Duration initialDelay = Duration.ofSeconds(1);
        
        public ResilientMusicJobProcessor(IgniteClient client) {
            this.client = client;
        }
        
        // Execute jobs with exponential backoff retry
        public <T> CompletableFuture<T> executeWithResilience(
                JobTarget target, JobDescriptor<?, T> job, Object input) {
            
            return executeWithRetryInternal(target, job, input, 0);
        }
        
        @SuppressWarnings("unchecked")
        private <T> CompletableFuture<T> executeWithRetryInternal(
                JobTarget target, JobDescriptor<?, T> job, Object input, int attempt) {
            
            return (CompletableFuture<T>) client.compute().executeAsync(target, (JobDescriptor<Object, T>) job, input)
                .exceptionallyCompose(throwable -> {
                    if (attempt >= maxRetryAttempts) {
                        return CompletableFuture.failedFuture(
                            new RuntimeException("Job failed after " + maxRetryAttempts + " attempts", throwable));
                    }
                    
                    if (isRetryableFailure(throwable)) {
                        long delayMs = initialDelay.toMillis() * (long) Math.pow(2, attempt);
                        
                        return CompletableFuture
                            .supplyAsync(() -> null, CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS))
                            .thenCompose(ignored -> 
                                executeWithRetryInternal(target, job, input, attempt + 1));
                    } else {
                        return CompletableFuture.failedFuture(throwable);
                    }
                });
        }
        
        private boolean isRetryableFailure(Throwable throwable) {
            String message = throwable.getMessage().toLowerCase();
            return message.contains("timeout") ||
                   message.contains("connection") ||
                   message.contains("node unavailable") ||
                   message.contains("cluster topology changed");
        }
    }

    /**
     * Simple pair class for internal use.
     */
    public static class Pair<K, V> implements Serializable {
        private final K key;
        private final V value;
        
        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }
        
        public K getKey() { return key; }
        public V getValue() { return value; }
    }
    
    // JSON parsing helper methods
    
    private Map<String, Long> parseArtistPopularityJson(String json) {
        Map<String, Long> result = new HashMap<>();
        if (json.equals("{}")) return result;
        
        try {
            // Remove outer braces
            String content = json.substring(1, json.length() - 1);
            if (content.isEmpty()) return result;
            
            // Split by comma but not within quotes
            int start = 0;
            boolean inQuotes = false;
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '"' && (i == 0 || content.charAt(i-1) != '\\')) {
                    inQuotes = !inQuotes;
                } else if (c == ',' && !inQuotes) {
                    parseEntry(content.substring(start, i), result);
                    start = i + 1;
                }
            }
            // Parse last entry
            if (start < content.length()) {
                parseEntry(content.substring(start), result);
            }
        } catch (Exception e) {
            logger.warn("Failed to parse artist popularity JSON: " + json, e);
        }
        
        return result;
    }
    
    private void parseEntry(String entry, Map<String, Long> result) {
        int colonPos = entry.lastIndexOf(":");
        if (colonPos > 0) {
            String artist = entry.substring(1, colonPos - 1);
            long plays = Long.parseLong(entry.substring(colonPos + 1).trim());
            result.put(artist, plays);
        }
    }
    
    private static List<String> parseRecommendationsJson(String json) {
        List<String> result = new ArrayList<>();
        if (json.equals("[]")) return result;
        
        try {
            // Remove outer brackets
            String content = json.substring(1, json.length() - 1).trim();
            if (content.isEmpty()) return result;
            
            // Handle quoted strings with commas
            int start = 0;
            boolean inQuotes = false;
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '"' && (i == 0 || content.charAt(i-1) != '\\')) {
                    if (!inQuotes) {
                        start = i + 1;
                        inQuotes = true;
                    } else {
                        result.add(content.substring(start, i));
                        inQuotes = false;
                    }
                }
            }
        } catch (Exception e) {
            // Fallback to simple parsing
            try {
                String content = json.substring(1, json.length() - 1);
                String[] items = content.split(",");
                for (String item : items) {
                    result.add(item.substring(1, item.length() - 1));
                }
            } catch (Exception ex) {
                // Return empty list on parse failure
            }
        }
        
        return result;
    }
    
    private static Map<String, Long> parseTrendJson(String json) {
        Map<String, Long> result = new HashMap<>();
        if (json.equals("{}")) return result;
        
        try {
            // Use the same parsing logic as artist popularity
            String content = json.substring(1, json.length() - 1);
            if (content.isEmpty()) return result;
            
            int start = 0;
            boolean inQuotes = false;
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '"' && (i == 0 || content.charAt(i-1) != '\\')) {
                    inQuotes = !inQuotes;
                } else if (c == ',' && !inQuotes) {
                    parseTrendEntry(content.substring(start, i), result);
                    start = i + 1;
                }
            }
            // Parse last entry
            if (start < content.length()) {
                parseTrendEntry(content.substring(start), result);
            }
        } catch (Exception e) {
            // Return empty map on parse failure
        }
        
        return result;
    }
    
    private static void parseTrendEntry(String entry, Map<String, Long> result) {
        int colonPos = entry.lastIndexOf(":");
        if (colonPos > 0) {
            String track = entry.substring(1, colonPos - 1);
            long plays = Long.parseLong(entry.substring(colonPos + 1).trim());
            result.put(track, plays);
        }
    }
}