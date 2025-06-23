package com.apache.ignite.examples.compute;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.compute.*;
import org.apache.ignite.deployment.DeploymentUnit;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    // Deployment unit configuration
    private static final String DEPLOYMENT_UNIT_NAME = "compute-jobs";
    private static final String DEPLOYMENT_UNIT_VERSION = "1.0.0";

    /**
     * Get deployment units for this application.
     */
    private static List<DeploymentUnit> getDeploymentUnits() {
        // For standalone clusters, jobs must be deployed externally via CLI
        // Using empty list will attempt to load classes from the classpath
        return List.of();
        
        // When properly deployed via CLI, use:
        // return List.of(new DeploymentUnit(DEPLOYMENT_UNIT_NAME, DEPLOYMENT_UNIT_VERSION));
    }

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
        JobDescriptor<Integer, String> artistJob = JobDescriptor.builder(ArtistAnalysisJob.class)
                .units(getDeploymentUnits())
                .build();
        
        try {
            // Execute on node where Artist with ArtistId=1 data resides for optimal performance
            JobTarget colocatedTarget = JobTarget.colocated("Artist", 
                Tuple.create().set("ArtistId", 1));
            
            String result = client.compute()
                    .execute(colocatedTarget, artistJob, 1); // AC/DC
            
            System.out.println("    <<< Artist analysis (colocated): " + result);
        } catch (Exception e) {
            System.err.println("    !!! Artist analysis failed: " + e.getMessage());
        }
        
        // Customer-specific processing colocated with customer data
        JobDescriptor<Integer, String> customerJob = JobDescriptor.builder(CustomerAnalysisJob.class)
                .units(getDeploymentUnits())
                .build();
        
        try {
            // Execute on node where Customer with CustomerId=1 data resides
            JobTarget customerTarget = JobTarget.colocated("Customer", 
                Tuple.create().set("CustomerId", 1));
            
            String customerResult = client.compute()
                    .execute(customerTarget, customerJob, 1);
            
            System.out.println("    <<< Customer analysis (colocated): " + customerResult);
        } catch (Exception e) {
            System.err.println("    !!! Customer analysis failed: " + e.getMessage());
        }
        
        // Demonstrate performance benefit of colocation
        demonstrateColocationPerformance(client);
    }

    /**
     * Demonstrates performance benefits of data colocation.
     */
    private void demonstrateColocationPerformance(IgniteClient client) {
        System.out.println("\n        --- Colocation Performance Comparison");
        
        JobDescriptor<Integer, String> salesJob = JobDescriptor.builder(ArtistSalesAnalysisJob.class)
                .units(getDeploymentUnits())
                .build();
        
        try {
            // Time execution with colocation
            long start = System.currentTimeMillis();
            JobTarget colocatedTarget = JobTarget.colocated("Artist", 
                Tuple.create().set("ArtistId", 1));
            String colocatedResult = client.compute()
                    .execute(colocatedTarget, salesJob, 1);
            long colocatedTime = System.currentTimeMillis() - start;
            
            // Time execution without colocation (any node)
            start = System.currentTimeMillis();
            String anyNodeResult = client.compute()
                    .execute(JobTarget.anyNode(client.clusterNodes()), salesJob, 1);
            long anyNodeTime = System.currentTimeMillis() - start;
            
            System.out.println("        >>> Colocated execution: " + colocatedTime + "ms - " + colocatedResult);
            System.out.println("        >>> Any node execution: " + anyNodeTime + "ms - " + anyNodeResult);
            System.out.println("        >>> Performance benefit demonstrates data locality optimization");
        } catch (Exception e) {
            System.err.println("        !!! Performance comparison failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrates broadcast job execution across all nodes.
     */
    private void demonstrateBroadcastJobs(IgniteClient client) {
        System.out.println("\n    --- Broadcast Jobs");
        System.out.println("    >>> Running jobs across all cluster nodes");
        
        // Cluster health check
        JobDescriptor<Void, String> healthJob = JobDescriptor.builder(ClusterHealthJob.class)
                .units(getDeploymentUnits())
                .build();
        
        try {
            Collection<String> results = client.compute()
                    .execute(BroadcastJobTarget.nodes(client.clusterNodes()), healthJob, null);
            
            System.out.println("    <<< Health check results from " + results.size() + " nodes:");
            int nodeIndex = 1;
            for (String health : results) {
                System.out.println("         Node " + nodeIndex++ + ": " + health);
            }
        } catch (Exception e) {
            System.err.println("    !!! Broadcast health check failed: " + e.getMessage());
        }
        
        // Data distribution analysis
        JobDescriptor<Void, Integer> dataJob = JobDescriptor.builder(LocalDataCountJob.class)
                .units(getDeploymentUnits())
                .build();
        
        try {
            Collection<Integer> dataCounts = client.compute()
                    .execute(BroadcastJobTarget.nodes(client.clusterNodes()), dataJob, null);
            
            System.out.println("    <<< Data distribution across nodes:");
            int totalRecords = 0;
            int nodeIndex = 1;
            for (Integer count : dataCounts) {
                System.out.println("         Node " + nodeIndex++ + ": " + count + " records");
                totalRecords += count;
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
        JobDescriptor<Void, Map<String, Integer>> mapJob = JobDescriptor.builder(GenreMapJob.class)
                .units(getDeploymentUnits())
                .build();
        
        try {
            // Map phase: collect genre data from each node
            Collection<Map<String, Integer>> mapResults = client.compute()
                    .execute(BroadcastJobTarget.nodes(client.clusterNodes()), mapJob, null);
            
            // Reduce phase: aggregate results
            Map<String, Integer> genreStats = new HashMap<>();
            for (Map<String, Integer> nodeResult : mapResults) {
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
            JobDescriptor<Void, List<String>> topArtistsJob = JobDescriptor.builder(TopArtistsJob.class)
                    .units(getDeploymentUnits())
                    .build();
            List<String> topArtists = client.compute()
                    .execute(JobTarget.anyNode(client.clusterNodes()), topArtistsJob, null);
            
            System.out.println("    >>> Step 1: Found " + topArtists.size() + " top artists");
            
            // Step 2: Analyze each artist in parallel
            List<CompletableFuture<String>> futures = new ArrayList<>();
            for (String artistName : topArtists) {
                JobDescriptor<String, String> artistDetailJob = JobDescriptor.builder(ArtistDetailJob.class)
                        .units(getDeploymentUnits())
                        .build();
                
                CompletableFuture<String> future = client.compute()
                        .executeAsync(JobTarget.anyNode(client.clusterNodes()), artistDetailJob, artistName);
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

    public static class ArtistAnalysisJob implements ComputeJob<Integer, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Integer artistId) {
            return CompletableFuture.supplyAsync(() -> {
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
            });
        }
    }

    public static class AlbumProcessingJob implements ComputeJob<Integer, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Integer albumId) {
            return CompletableFuture.supplyAsync(() -> {
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
            });
        }
    }

    public static class ClusterHealthJob implements ComputeJob<Void, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Void arg) {
            return CompletableFuture.completedFuture("Node healthy - Available memory: " + 
                   (Runtime.getRuntime().freeMemory() / (1024 * 1024)) + " MB");
        }
    }

    public static class LocalDataCountJob implements ComputeJob<Void, Integer> {
        @Override
        public CompletableFuture<Integer> executeAsync(JobExecutionContext context, Void arg) {
            return CompletableFuture.supplyAsync(() -> {
                IgniteSql sql = context.ignite().sql();
                
                try (ResultSet<SqlRow> result = sql.execute(null, 
                        "SELECT COUNT(*) as artist_count FROM Artist")) {
                    
                    if (result.hasNext()) {
                        return (int) result.next().longValue("artist_count");
                    }
                    return 0;
                }
            });
        }
    }

    public static class GenreMapJob implements ComputeJob<Void, Map<String, Integer>> {
        @Override
        public CompletableFuture<Map<String, Integer>> executeAsync(JobExecutionContext context, Void arg) {
            return CompletableFuture.supplyAsync(() -> {
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
            });
        }
    }

    public static class TopArtistsJob implements ComputeJob<Void, List<String>> {
        @Override
        public CompletableFuture<List<String>> executeAsync(JobExecutionContext context, Void arg) {
            return CompletableFuture.supplyAsync(() -> {
                IgniteSql sql = context.ignite().sql();
                List<String> topArtists = new ArrayList<>();
                
                try (ResultSet<SqlRow> result = sql.execute(null,
                        "SELECT Name FROM Artist LIMIT 3")) {
                    
                    while (result.hasNext()) {
                        topArtists.add(result.next().stringValue("Name"));
                    }
                }
                
                return topArtists;
            });
        }
    }

    public static class ArtistDetailJob implements ComputeJob<String, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, String artistName) {
            return CompletableFuture.supplyAsync(() -> {
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
            });
        }
    }

    public static class CustomerAnalysisJob implements ComputeJob<Integer, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Integer customerId) {
            return CompletableFuture.supplyAsync(() -> {
                IgniteSql sql = context.ignite().sql();
                
                try (ResultSet<SqlRow> result = sql.execute(null,
                        "SELECT c.FirstName, c.LastName, COUNT(i.InvoiceId) as purchase_count, " +
                        "SUM(il.UnitPrice * il.Quantity) as total_spent " +
                        "FROM Customer c " +
                        "LEFT JOIN Invoice i ON c.CustomerId = i.CustomerId " +
                        "LEFT JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId " +
                        "WHERE c.CustomerId = ? " +
                        "GROUP BY c.CustomerId, c.FirstName, c.LastName", customerId)) {
                    
                    if (result.hasNext()) {
                        SqlRow row = result.next();
                        String name = row.stringValue("FirstName") + " " + row.stringValue("LastName");
                        int purchases = (int) row.longValue("purchase_count");
                        Object totalObj = row.value("total_spent");
                        double total = totalObj != null ? ((Number) totalObj).doubleValue() : 0.0;
                        return name + " has " + purchases + " purchases, total spent: $" + String.format("%.2f", total);
                    }
                    return "Customer not found";
                }
            });
        }
    }

    public static class ArtistSalesAnalysisJob implements ComputeJob<Integer, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Integer artistId) {
            return CompletableFuture.supplyAsync(() -> {
                IgniteSql sql = context.ignite().sql();
                
                try (ResultSet<SqlRow> result = sql.execute(null,
                        "SELECT ar.Name, COUNT(il.InvoiceLineId) as sales_count, " +
                        "SUM(il.UnitPrice * il.Quantity) as total_revenue " +
                        "FROM Artist ar " +
                        "JOIN Album al ON ar.ArtistId = al.ArtistId " +
                        "JOIN Track t ON al.AlbumId = t.AlbumId " +
                        "JOIN InvoiceLine il ON t.TrackId = il.TrackId " +
                        "WHERE ar.ArtistId = ? " +
                        "GROUP BY ar.ArtistId, ar.Name", artistId)) {
                    
                    if (result.hasNext()) {
                        SqlRow row = result.next();
                        String name = row.stringValue("Name");
                        int sales = (int) row.longValue("sales_count");
                        Object revenueObj = row.value("total_revenue");
                        double revenue = revenueObj != null ? ((Number) revenueObj).doubleValue() : 0.0;
                        return name + ": " + sales + " sales, $" + String.format("%.2f", revenue) + " revenue";
                    }
                    return "No sales data for artist";
                }
            });
        }
    }
}