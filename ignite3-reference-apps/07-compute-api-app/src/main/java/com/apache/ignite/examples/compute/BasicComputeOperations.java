package com.apache.ignite.examples.compute;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.JobDescriptor;
import org.apache.ignite.compute.JobExecutionContext;
import org.apache.ignite.compute.JobTarget;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Demonstrates basic compute operations using the Apache Ignite 3 Compute API.
 * 
 * Covers fundamental job submission patterns including simple job execution,
 * parameter passing, result handling, and error management. Shows how to
 * distribute computational tasks across the cluster.
 * 
 * Key concepts:
 * - Job definition and submission
 * - JobTarget for node selection
 * - Synchronous and asynchronous execution
 * - Parameter passing to jobs
 * - Result collection and error handling
 * 
 * Prerequisites:
 * - Running Ignite cluster
 * - Sample music store data loaded
 */
public class BasicComputeOperations {

    private static final Logger logger = LoggerFactory.getLogger(BasicComputeOperations.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Basic Compute Operations Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Demonstrating distributed job execution fundamentals");

        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            BasicComputeOperations demo = new BasicComputeOperations();
            demo.runBasicComputeOperations(client);
            
        } catch (Exception e) {
            logger.error("Failed to run basic compute operations", e);
            System.err.println("!!! Error: " + e.getMessage());
        }
    }

    private void runBasicComputeOperations(IgniteClient client) {
        System.out.println("\n--- Basic Job Execution ---");
        System.out.println("    Learning fundamental compute patterns");
        
        // Simple job execution
        demonstrateSimpleJobs(client);
        
        // Jobs with parameters
        demonstrateParameterizedJobs(client);
        
        // Jobs with SQL queries
        demonstrateSQLJobs(client);
        
        // Async job execution
        demonstrateAsyncJobs(client);
        
        System.out.println("\n>>> Basic compute operations completed successfully");
    }

    /**
     * Demonstrates simple job execution without parameters.
     */
    private void demonstrateSimpleJobs(IgniteClient client) {
        System.out.println("\n    --- Simple Job Execution");
        System.out.println("    >>> Executing basic jobs on cluster nodes");
        
        // Simple greeting job
        JobDescriptor<Void, String> greetingJob = JobDescriptor.builder(HelloWorldJob.class).build();
        
        try {
            String result = client.compute()
                    .execute(JobTarget.anyNode(client.clusterNodes()), greetingJob, null);
            
            System.out.println("    <<< Job result: " + result);
        } catch (Exception e) {
            System.err.println("    !!! Job execution failed: " + e.getMessage());
        }
        
        // Node information job
        JobDescriptor<Void, String> nodeInfoJob = JobDescriptor.builder(NodeInfoJob.class).build();
        
        try {
            String nodeInfo = client.compute()
                    .execute(JobTarget.anyNode(client.clusterNodes()), nodeInfoJob, null);
            
            System.out.println("    <<< Node info: " + nodeInfo);
        } catch (Exception e) {
            System.err.println("    !!! Node info job failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrates jobs with parameters.
     */
    private void demonstrateParameterizedJobs(IgniteClient client) {
        System.out.println("\n    --- Parameterized Jobs");
        System.out.println("    >>> Executing jobs with input parameters");
        
        // Artist search job
        JobDescriptor<String, String> searchJob = JobDescriptor.builder(ArtistSearchJob.class).build();
        
        try {
            String result = client.compute()
                    .execute(JobTarget.anyNode(client.clusterNodes()), searchJob, "AC/DC");
            
            System.out.println("    <<< Search result: " + result);
        } catch (Exception e) {
            System.err.println("    !!! Search job failed: " + e.getMessage());
        }
        
        // Track count job
        JobDescriptor<Void, Integer> countJob = JobDescriptor.builder(TrackCountJob.class).build();
        
        try {
            Integer trackCount = client.compute()
                    .execute(JobTarget.anyNode(client.clusterNodes()), countJob, null);
            
            System.out.println("    <<< Total tracks in database: " + trackCount);
        } catch (Exception e) {
            System.err.println("    !!! Count job failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrates jobs that execute SQL queries.
     */
    private void demonstrateSQLJobs(IgniteClient client) {
        System.out.println("\n    --- SQL-based Jobs");
        System.out.println("    >>> Running jobs that execute database queries");
        
        // Genre analysis job
        JobDescriptor<Void, String> genreJob = JobDescriptor.builder(GenreAnalysisJob.class).build();
        
        try {
            String analysis = client.compute()
                    .execute(JobTarget.anyNode(client.clusterNodes()), genreJob, null);
            
            System.out.println("    <<< Genre analysis: " + analysis);
        } catch (Exception e) {
            System.err.println("    !!! Genre analysis failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrates asynchronous job execution.
     */
    private void demonstrateAsyncJobs(IgniteClient client) {
        System.out.println("\n    --- Asynchronous Job Execution");
        System.out.println("    >>> Running jobs asynchronously without blocking");
        
        // Start multiple jobs asynchronously
        JobDescriptor<Void, String> job1 = JobDescriptor.builder(HelloWorldJob.class).build();
        JobDescriptor<Void, Integer> job2 = JobDescriptor.builder(TrackCountJob.class).build();
        
        try {
            CompletableFuture<String> future1 = client.compute()
                    .executeAsync(JobTarget.anyNode(client.clusterNodes()), job1, null);
            
            CompletableFuture<Integer> future2 = client.compute()
                    .executeAsync(JobTarget.anyNode(client.clusterNodes()), job2, null);
            
            System.out.println("    >>> Both jobs started asynchronously");
            
            // Wait for completion
            String result1 = future1.join();
            Integer result2 = future2.join();
            
            System.out.println("    <<< Async job 1: " + result1);
            System.out.println("    <<< Async job 2: " + result2 + " tracks");
        } catch (Exception e) {
            System.err.println("    !!! Async job execution failed: " + e.getMessage());
        }
    }

    // Job implementations

    public static class HelloWorldJob implements ComputeJob<Void, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Void arg) {
            return CompletableFuture.completedFuture("Hello from Ignite Compute!");
        }
    }

    public static class NodeInfoJob implements ComputeJob<Void, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Void arg) {
            return CompletableFuture.completedFuture("Node: " + context.ignite().name());
        }
    }

    public static class ArtistSearchJob implements ComputeJob<String, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, String searchTerm) {
            return CompletableFuture.supplyAsync(() -> {
                if (searchTerm == null || searchTerm.isEmpty()) return "No search term provided";
                
                IgniteSql sql = context.ignite().sql();
                
                try (ResultSet<SqlRow> result = sql.execute(null, 
                        "SELECT Name FROM Artist WHERE Name LIKE ? LIMIT 1", 
                        "%" + searchTerm + "%")) {
                    
                    if (result.hasNext()) {
                        return "Found: " + result.next().stringValue("Name");
                    } else {
                        return "Artist not found: " + searchTerm;
                    }
                }
            });
        }
    }

    public static class TrackCountJob implements ComputeJob<Void, Integer> {
        @Override
        public CompletableFuture<Integer> executeAsync(JobExecutionContext context, Void arg) {
            return CompletableFuture.supplyAsync(() -> {
                IgniteSql sql = context.ignite().sql();
                
                try (ResultSet<SqlRow> result = sql.execute(null, 
                        "SELECT COUNT(*) as track_count FROM Track")) {
                    
                    if (result.hasNext()) {
                        return (int) result.next().longValue("track_count");
                    }
                    return 0;
                }
            });
        }
    }

    public static class GenreAnalysisJob implements ComputeJob<Void, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Void arg) {
            return CompletableFuture.supplyAsync(() -> {
                IgniteSql sql = context.ignite().sql();
                
                try (ResultSet<SqlRow> result = sql.execute(null, 
                        "SELECT g.Name, COUNT(t.TrackId) as track_count " +
                        "FROM Genre g JOIN Track t ON g.GenreId = t.GenreId " +
                        "GROUP BY g.Name ORDER BY track_count DESC LIMIT 1")) {
                    
                    if (result.hasNext()) {
                        SqlRow row = result.next();
                        return "Most popular genre: " + row.stringValue("Name") + 
                               " (" + row.longValue("track_count") + " tracks)";
                    }
                    return "No genre data found";
                }
            });
        }
    }
}