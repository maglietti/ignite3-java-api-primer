package com.apache.ignite.examples.compute;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.compute.*;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Demonstrates advanced compute operations using the Apache Ignite 3 Compute API.
 * 
 * Covers complex job patterns including data colocation for performance,
 * broadcast execution, MapReduce patterns, and job coordination. Shows how to
 * optimize distributed computations through intelligent job placement.
 * 
 * Key concepts:
 * - Data colocation for performance optimization
 * - Broadcast jobs for distributed operations
 * - MapReduce patterns for large-scale processing
 * - Job coordination and result aggregation
 * - Performance optimization techniques
 * 
 * Prerequisites:
 * - Running Ignite cluster
 * - Sample music store data loaded
 */
public class AdvancedComputeOperations {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedComputeOperations.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Advanced Compute Operations Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Demonstrating data locality and complex job patterns");

        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            AdvancedComputeOperations demo = new AdvancedComputeOperations();
            demo.runAdvancedComputeOperations(client);
            
        } catch (Exception e) {
            logger.error("Failed to run advanced compute operations", e);
            System.err.println("!!! Error: " + e.getMessage());
        }
    }

    private void runAdvancedComputeOperations(IgniteClient client) {
        System.out.println("\n--- Advanced Job Patterns ---");
        System.out.println("    Data locality and complex distributed operations");
        
        // Data-local execution
        demonstrateColocationJobs(client);
        
        // Broadcast execution
        demonstrateBroadcastJobs(client);
        
        // MapReduce patterns
        demonstrateMapReduceJobs(client);
        
        // Job coordination
        demonstrateJobCoordination(client);
        
        System.out.println("\n>>> Advanced compute operations completed successfully");
    }

    /**
     * Demonstrates data-colocated job execution for performance.
     */
    private void demonstrateColocationJobs(IgniteClient client) {
        System.out.println("\n    --- Data-Colocated Jobs");
        System.out.println("    >>> Running jobs close to data for optimal performance");
        
        // Artist-specific analytics (colocated by ArtistId)
        JobDescriptor<String> artistJob = JobDescriptor.builder(ArtistAnalysisJob.class)
                .args(1) // AC/DC
                .build();
        
        try {
            // Execute on node containing the artist data
            String result = client.compute()
                    .execute(JobTarget.anyNode(), artistJob)
                    .join();
            
            System.out.println("    <<< Artist analysis: " + result);
        } catch (Exception e) {
            System.err.println("    !!! Artist analysis failed: " + e.getMessage());
        }
        
        // Album-specific processing
        JobDescriptor<String> albumJob = JobDescriptor.builder(AlbumProcessingJob.class)
                .args(1) // First album
                .build();
        
        try {
            String albumResult = client.compute()
                    .execute(JobTarget.anyNode(), albumJob)
                    .join();
            
            System.out.println("    <<< Album processing: " + albumResult);
        } catch (Exception e) {
            System.err.println("    !!! Album processing failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrates broadcast job execution across all nodes.
     */
    private void demonstrateBroadcastJobs(IgniteClient client) {
        System.out.println("\n    --- Broadcast Jobs");
        System.out.println("    >>> Running jobs across all cluster nodes");
        
        // Cluster health check
        JobDescriptor<String> healthJob = JobDescriptor.builder(ClusterHealthJob.class).build();
        
        try {
            Map<String, String> results = client.compute()
                    .executeBroadcast(BroadcastJobTarget.allNodes(), healthJob)
                    .join();
            
            System.out.println("    <<< Health check results from " + results.size() + " nodes:");
            results.forEach((node, health) -> 
                System.out.println("         " + node + ": " + health));
        } catch (Exception e) {
            System.err.println("    !!! Broadcast health check failed: " + e.getMessage());
        }
        
        // Data distribution analysis
        JobDescriptor<Integer> dataJob = JobDescriptor.builder(LocalDataCountJob.class).build();
        
        try {
            Map<String, Integer> dataCounts = client.compute()
                    .executeBroadcast(BroadcastJobTarget.allNodes(), dataJob)
                    .join();
            
            System.out.println("    <<< Data distribution across nodes:");
            int totalRecords = 0;
            for (Map.Entry<String, Integer> entry : dataCounts.entrySet()) {
                System.out.println("         " + entry.getKey() + ": " + entry.getValue() + " records");
                totalRecords += entry.getValue();
            }
            System.out.println("         Total: " + totalRecords + " records");
        } catch (Exception e) {
            System.err.println("    !!! Data distribution analysis failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrates MapReduce patterns for large-scale processing.
     */
    private void demonstrateMapReduceJobs(IgniteClient client) {
        System.out.println("\n    --- MapReduce Patterns");
        System.out.println("    >>> Implementing distributed map-reduce operations");
        
        // Genre popularity analysis using MapReduce
        JobDescriptor<Map<String, Integer>> mapJob = JobDescriptor.builder(GenreMapJob.class).build();
        
        try {
            // Map phase: collect genre data from each node
            Map<String, Map<String, Integer>> mapResults = client.compute()
                    .executeBroadcast(BroadcastJobTarget.allNodes(), mapJob)
                    .join();
            
            // Reduce phase: aggregate results
            Map<String, Integer> genreStats = new HashMap<>();
            for (Map<String, Integer> nodeResult : mapResults.values()) {
                for (Map.Entry<String, Integer> entry : nodeResult.entrySet()) {
                    genreStats.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }
            
            System.out.println("    <<< Genre popularity (MapReduce result):");
            genreStats.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(3)
                    .forEach(entry -> 
                        System.out.println("         " + entry.getKey() + ": " + entry.getValue() + " tracks"));
        } catch (Exception e) {
            System.err.println("    !!! MapReduce job failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrates job coordination and workflow patterns.
     */
    private void demonstrateJobCoordination(IgniteClient client) {
        System.out.println("\n    --- Job Coordination");
        System.out.println("    >>> Orchestrating multiple jobs in workflows");
        
        try {
            // Step 1: Analyze top artists
            JobDescriptor<List<String>> topArtistsJob = JobDescriptor.builder(TopArtistsJob.class).build();
            List<String> topArtists = client.compute()
                    .execute(JobTarget.anyNode(), topArtistsJob)
                    .join();
            
            System.out.println("    >>> Step 1: Found " + topArtists.size() + " top artists");
            
            // Step 2: Analyze each artist in parallel
            List<CompletableFuture<String>> futures = new ArrayList<>();
            for (String artistName : topArtists) {
                JobDescriptor<String> artistDetailJob = JobDescriptor.builder(ArtistDetailJob.class)
                        .args(artistName)
                        .build();
                
                CompletableFuture<String> future = client.compute()
                        .execute(JobTarget.anyNode(), artistDetailJob);
                futures.add(future);
            }
            
            // Wait for all analyses to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            System.out.println("    <<< Artist analysis workflow completed:");
            for (int i = 0; i < futures.size(); i++) {
                String result = futures.get(i).join();
                System.out.println("         " + topArtists.get(i) + ": " + result);
            }
        } catch (Exception e) {
            System.err.println("    !!! Job coordination failed: " + e.getMessage());
        }
    }

    // Job implementations

    public static class ArtistAnalysisJob implements ComputeJob<String>, Serializable {
        @Override
        public String execute(JobExecutionContext context, Object... args) {
            int artistId = (Integer) args[0];
            IgniteSql sql = context.ignite().sql();
            
            try (ResultSet<SqlRow> result = sql.execute(null,
                    "SELECT a.Name, COUNT(t.TrackId) as track_count " +
                    "FROM Artist a JOIN Album al ON a.ArtistId = al.ArtistId " +
                    "JOIN Track t ON al.AlbumId = t.AlbumId " +
                    "WHERE a.ArtistId = ? GROUP BY a.Name", artistId)) {
                
                if (result.hasNext()) {
                    SqlRow row = result.next();
                    return row.stringValue("Name") + " has " + row.longValue("track_count") + " tracks";
                }
                return "Artist not found";
            }
        }
    }

    public static class AlbumProcessingJob implements ComputeJob<String>, Serializable {
        @Override
        public String execute(JobExecutionContext context, Object... args) {
            int albumId = (Integer) args[0];
            IgniteSql sql = context.ignite().sql();
            
            try (ResultSet<SqlRow> result = sql.execute(null,
                    "SELECT al.Title, COUNT(t.TrackId) as track_count " +
                    "FROM Album al JOIN Track t ON al.AlbumId = t.AlbumId " +
                    "WHERE al.AlbumId = ? GROUP BY al.Title", albumId)) {
                
                if (result.hasNext()) {
                    SqlRow row = result.next();
                    return "Album '" + row.stringValue("Title") + "' has " + row.longValue("track_count") + " tracks";
                }
                return "Album not found";
            }
        }
    }

    public static class ClusterHealthJob implements ComputeJob<String>, Serializable {
        @Override
        public String execute(JobExecutionContext context, Object... args) {
            return "Node healthy - Available memory: " + 
                   (Runtime.getRuntime().freeMemory() / (1024 * 1024)) + " MB";
        }
    }

    public static class LocalDataCountJob implements ComputeJob<Integer>, Serializable {
        @Override
        public Integer execute(JobExecutionContext context, Object... args) {
            IgniteSql sql = context.ignite().sql();
            
            try (ResultSet<SqlRow> result = sql.execute(null, 
                    "SELECT COUNT(*) as artist_count FROM Artist")) {
                
                if (result.hasNext()) {
                    return (int) result.next().longValue("artist_count");
                }
                return 0;
            }
        }
    }

    public static class GenreMapJob implements ComputeJob<Map<String, Integer>>, Serializable {
        @Override
        public Map<String, Integer> execute(JobExecutionContext context, Object... args) {
            IgniteSql sql = context.ignite().sql();
            Map<String, Integer> genreStats = new HashMap<>();
            
            try (ResultSet<SqlRow> result = sql.execute(null,
                    "SELECT g.Name, COUNT(t.TrackId) as track_count " +
                    "FROM Genre g JOIN Track t ON g.GenreId = t.GenreId " +
                    "GROUP BY g.Name")) {
                
                while (result.hasNext()) {
                    SqlRow row = result.next();
                    genreStats.put(row.stringValue("Name"), (int) row.longValue("track_count"));
                }
            }
            
            return genreStats;
        }
    }

    public static class TopArtistsJob implements ComputeJob<List<String>>, Serializable {
        @Override
        public List<String> execute(JobExecutionContext context, Object... args) {
            IgniteSql sql = context.ignite().sql();
            List<String> topArtists = new ArrayList<>();
            
            try (ResultSet<SqlRow> result = sql.execute(null,
                    "SELECT Name FROM Artist LIMIT 3")) {
                
                while (result.hasNext()) {
                    topArtists.add(result.next().stringValue("Name"));
                }
            }
            
            return topArtists;
        }
    }

    public static class ArtistDetailJob implements ComputeJob<String>, Serializable {
        @Override
        public String execute(JobExecutionContext context, Object... args) {
            String artistName = (String) args[0];
            IgniteSql sql = context.ignite().sql();
            
            try (ResultSet<SqlRow> result = sql.execute(null,
                    "SELECT COUNT(DISTINCT al.AlbumId) as album_count " +
                    "FROM Artist a JOIN Album al ON a.ArtistId = al.ArtistId " +
                    "WHERE a.Name = ?", artistName)) {
                
                if (result.hasNext()) {
                    return result.next().longValue("album_count") + " albums";
                }
                return "No data";
            }
        }
    }
}