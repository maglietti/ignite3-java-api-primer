package com.apache.ignite.examples.compute;

import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.JobExecutionContext;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;

import java.util.concurrent.CompletableFuture;

/**
 * Advanced compute job implementations for data locality and performance demonstrations.
 * 
 * Contains specialized job classes that showcase distributed computing patterns
 * including data colocation, broadcast execution, and performance optimization.
 * These jobs are designed to demonstrate real-world distributed processing scenarios.
 */
public class AdvancedComputeJobs {

    /**
     * Job that analyzes artist data with focus on data locality.
     * Executes SQL queries on colocated data for optimal performance.
     */
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

    /**
     * Job that analyzes customer purchase history and spending patterns.
     * Demonstrates complex join operations with data locality benefits.
     */
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

    /**
     * Job for performance comparison between colocated and non-colocated execution.
     * Calculates artist sales metrics with detailed revenue analysis.
     */
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

    /**
     * Job that reports cluster node health status.
     * Used in broadcast execution to gather cluster-wide health information.
     */
    public static class ClusterHealthJob implements ComputeJob<Void, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Void arg) {
            return CompletableFuture.completedFuture("Node healthy - Available memory: " + 
                   (Runtime.getRuntime().freeMemory() / (1024 * 1024)) + " MB");
        }
    }

    /**
     * Job that counts local data records on each node.
     * Demonstrates data distribution analysis across cluster nodes.
     */
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

    /**
     * MapReduce job that generates CSV output for genre statistics.
     * Returns string-based results to avoid serialization complexity.
     */
    public static class GenreMapJob implements ComputeJob<Void, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Void arg) {
            return CompletableFuture.supplyAsync(() -> {
                IgniteSql sql = context.ignite().sql();
                StringBuilder csvResult = new StringBuilder();
                
                try (ResultSet<SqlRow> result = sql.execute(null,
                        "SELECT g.Name, COUNT(t.TrackId) as track_count " +
                        "FROM Genre g JOIN Track t ON g.GenreId = t.GenreId " +
                        "GROUP BY g.Name")) {
                    
                    while (result.hasNext()) {
                        SqlRow row = result.next();
                        csvResult.append(row.stringValue("Name"))
                                 .append(",")
                                 .append(row.longValue("track_count"))
                                 .append("\n");
                    }
                }
                
                return csvResult.toString();
            });
        }
    }

    /**
     * Job that returns top artists for workflow coordination examples.
     * Uses newline-separated string format for compatibility.
     */
    public static class TopArtistsJob implements ComputeJob<Void, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Void arg) {
            return CompletableFuture.supplyAsync(() -> {
                IgniteSql sql = context.ignite().sql();
                StringBuilder csvResult = new StringBuilder();
                
                try (ResultSet<SqlRow> result = sql.execute(null,
                        "SELECT Name FROM Artist LIMIT 3")) {
                    
                    while (result.hasNext()) {
                        csvResult.append(result.next().stringValue("Name")).append("\n");
                    }
                }
                
                return csvResult.toString();
            });
        }
    }

    /**
     * Job that provides detailed artist information for workflow processing.
     * Counts albums for specified artist names.
     */
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
}