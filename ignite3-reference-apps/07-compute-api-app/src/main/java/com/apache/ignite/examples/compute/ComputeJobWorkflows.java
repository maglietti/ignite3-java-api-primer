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

/**
 * Demonstrates job workflows and orchestration using the Apache Ignite 3 Compute API.
 * 
 * Covers complex job coordination patterns including multi-step workflows,
 * job dependencies, result aggregation, and business process automation.
 * Shows how to build sophisticated distributed applications.
 * 
 * Key concepts:
 * - Multi-step job workflows
 * - Job result aggregation
 * - Business process automation
 * - Error handling in workflows
 * - Performance monitoring
 * 
 * Prerequisites:
 * - Running Ignite cluster
 * - Sample music store data loaded
 */
public class ComputeJobWorkflows {

    private static final Logger logger = LoggerFactory.getLogger(ComputeJobWorkflows.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Compute Job Workflows Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Demonstrating job orchestration and business workflows");

        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            ComputeJobWorkflows demo = new ComputeJobWorkflows();
            demo.runWorkflowDemonstrations(client);
            
        } catch (Exception e) {
            logger.error("Failed to run workflow demonstrations", e);
            System.err.println("!!! Error: " + e.getMessage());
        }
    }

    private void runWorkflowDemonstrations(IgniteClient client) {
        System.out.println("\n--- Job Workflow Patterns ---");
        System.out.println("    Orchestrating complex distributed business processes");
        
        // Customer analytics workflow
        demonstrateCustomerAnalyticsWorkflow(client);
        
        // Music recommendation workflow
        demonstrateMusicRecommendationWorkflow(client);
        
        // Revenue optimization workflow
        demonstrateRevenueOptimizationWorkflow(client);
        
        System.out.println("\n>>> Job workflow demonstrations completed successfully");
    }

    /**
     * Demonstrates customer analytics workflow with multiple job steps.
     */
    private void demonstrateCustomerAnalyticsWorkflow(IgniteClient client) {
        System.out.println("\n    --- Customer Analytics Workflow");
        System.out.println("    >>> Running multi-step customer analysis process");
        
        try {
            // Step 1: Get customer segments
            JobDescriptor<List<String>> segmentJob = JobDescriptor.builder(CustomerSegmentJob.class).build();
            List<String> segments = client.compute()
                    .execute(JobTarget.anyNode(), segmentJob)
                    .join();
            
            System.out.println("    >>> Step 1: Identified " + segments.size() + " customer segments");
            
            // Step 2: Analyze each segment in parallel
            List<CompletableFuture<String>> futures = new ArrayList<>();
            for (String segment : segments) {
                JobDescriptor<String> analysisJob = JobDescriptor.builder(SegmentAnalysisJob.class)
                        .args(segment)
                        .build();
                
                CompletableFuture<String> future = client.compute()
                        .execute(JobTarget.anyNode(), analysisJob);
                futures.add(future);
            }
            
            // Step 3: Aggregate results
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            System.out.println("    <<< Customer analytics workflow completed:");
            for (int i = 0; i < futures.size(); i++) {
                String result = futures.get(i).join();
                System.out.println("         " + segments.get(i) + ": " + result);
            }
        } catch (Exception e) {
            System.err.println("    !!! Customer analytics workflow failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrates music recommendation workflow with data dependencies.
     */
    private void demonstrateMusicRecommendationWorkflow(IgniteClient client) {
        System.out.println("\n    --- Music Recommendation Workflow");
        System.out.println("    >>> Building personalized recommendations through job pipeline");
        
        try {
            // Step 1: Analyze listening patterns
            JobDescriptor<Map<String, Integer>> patternJob = JobDescriptor.builder(ListeningPatternJob.class).build();
            Map<String, Integer> patterns = client.compute()
                    .execute(JobTarget.anyNode(), patternJob)
                    .join();
            
            System.out.println("    >>> Step 1: Analyzed listening patterns (" + patterns.size() + " genres)");
            
            // Step 2: Generate recommendations based on patterns
            String topGenre = patterns.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Rock");
            
            JobDescriptor<List<String>> recommendationJob = JobDescriptor.builder(RecommendationJob.class)
                    .args(topGenre)
                    .build();
            
            List<String> recommendations = client.compute()
                    .execute(JobTarget.anyNode(), recommendationJob)
                    .join();
            
            System.out.println("    >>> Step 2: Generated " + recommendations.size() + " recommendations");
            
            // Step 3: Rank and filter recommendations
            JobDescriptor<List<String>> rankingJob = JobDescriptor.builder(RankingJob.class)
                    .args(recommendations.toArray())
                    .build();
            
            List<String> rankedRecommendations = client.compute()
                    .execute(JobTarget.anyNode(), rankingJob)
                    .join();
            
            System.out.println("    <<< Music recommendation workflow completed:");
            System.out.println("         Top genre: " + topGenre);
            System.out.println("         Top recommendations: " + rankedRecommendations.subList(0, Math.min(3, rankedRecommendations.size())));
        } catch (Exception e) {
            System.err.println("    !!! Music recommendation workflow failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrates revenue optimization workflow with business intelligence.
     */
    private void demonstrateRevenueOptimizationWorkflow(IgniteClient client) {
        System.out.println("\n    --- Revenue Optimization Workflow");
        System.out.println("    >>> Running business intelligence analysis pipeline");
        
        try {
            // Step 1: Calculate current metrics
            JobDescriptor<Map<String, Double>> metricsJob = JobDescriptor.builder(RevenueMetricsJob.class).build();
            Map<String, Double> metrics = client.compute()
                    .execute(JobTarget.anyNode(), metricsJob)
                    .join();
            
            System.out.println("    >>> Step 1: Current revenue metrics calculated");
            
            // Step 2: Identify optimization opportunities
            JobDescriptor<List<String>> opportunityJob = JobDescriptor.builder(OptimizationOpportunityJob.class)
                    .args(metrics)
                    .build();
            
            List<String> opportunities = client.compute()
                    .execute(JobTarget.anyNode(), opportunityJob)
                    .join();
            
            System.out.println("    >>> Step 2: Identified " + opportunities.size() + " optimization opportunities");
            
            // Step 3: Generate action plan
            JobDescriptor<String> actionPlanJob = JobDescriptor.builder(ActionPlanJob.class)
                    .args(opportunities.toArray())
                    .build();
            
            String actionPlan = client.compute()
                    .execute(JobTarget.anyNode(), actionPlanJob)
                    .join();
            
            System.out.println("    <<< Revenue optimization workflow completed:");
            System.out.println("         Current revenue: $" + String.format("%.2f", metrics.getOrDefault("total_revenue", 0.0)));
            System.out.println("         Action plan: " + actionPlan);
        } catch (Exception e) {
            System.err.println("    !!! Revenue optimization workflow failed: " + e.getMessage());
        }
    }

    // Job implementations

    public static class CustomerSegmentJob implements ComputeJob<List<String>>, Serializable {
        @Override
        public List<String> execute(JobExecutionContext context, Object... args) {
            return Arrays.asList("High Value", "Regular", "New Customers");
        }
    }

    public static class SegmentAnalysisJob implements ComputeJob<String>, Serializable {
        @Override
        public String execute(JobExecutionContext context, Object... args) {
            String segment = (String) args[0];
            IgniteSql sql = context.ignite().sql();
            
            try (ResultSet<SqlRow> result = sql.execute(null,
                    "SELECT COUNT(*) as customer_count FROM Customer LIMIT 1")) {
                
                if (result.hasNext()) {
                    int count = (int) result.next().longValue("customer_count");
                    return count + " customers analyzed";
                }
                return "No data";
            }
        }
    }

    public static class ListeningPatternJob implements ComputeJob<Map<String, Integer>>, Serializable {
        @Override
        public Map<String, Integer> execute(JobExecutionContext context, Object... args) {
            IgniteSql sql = context.ignite().sql();
            Map<String, Integer> patterns = new HashMap<>();
            
            try (ResultSet<SqlRow> result = sql.execute(null,
                    "SELECT g.Name, COUNT(t.TrackId) as track_count " +
                    "FROM Genre g JOIN Track t ON g.GenreId = t.GenreId " +
                    "GROUP BY g.Name")) {
                
                while (result.hasNext()) {
                    SqlRow row = result.next();
                    patterns.put(row.stringValue("Name"), (int) row.longValue("track_count"));
                }
            }
            
            return patterns;
        }
    }

    public static class RecommendationJob implements ComputeJob<List<String>>, Serializable {
        @Override
        public List<String> execute(JobExecutionContext context, Object... args) {
            String genre = (String) args[0];
            IgniteSql sql = context.ignite().sql();
            List<String> recommendations = new ArrayList<>();
            
            try (ResultSet<SqlRow> result = sql.execute(null,
                    "SELECT t.Name FROM Track t " +
                    "JOIN Genre g ON t.GenreId = g.GenreId " +
                    "WHERE g.Name = ? LIMIT 5", genre)) {
                
                while (result.hasNext()) {
                    recommendations.add(result.next().stringValue("Name"));
                }
            }
            
            return recommendations;
        }
    }

    public static class RankingJob implements ComputeJob<List<String>>, Serializable {
        @Override
        public List<String> execute(JobExecutionContext context, Object... args) {
            List<String> tracks = new ArrayList<>();
            for (Object arg : args) {
                if (arg instanceof List) {
                    tracks.addAll((List<String>) arg);
                }
            }
            Collections.shuffle(tracks); // Simple ranking simulation
            return tracks;
        }
    }

    public static class RevenueMetricsJob implements ComputeJob<Map<String, Double>>, Serializable {
        @Override
        public Map<String, Double> execute(JobExecutionContext context, Object... args) {
            IgniteSql sql = context.ignite().sql();
            Map<String, Double> metrics = new HashMap<>();
            
            try (ResultSet<SqlRow> result = sql.execute(null,
                    "SELECT SUM(UnitPrice * Quantity) as total_revenue FROM InvoiceLine")) {
                
                if (result.hasNext()) {
                    Object revenueObj = result.next().value("total_revenue");
                    double revenue = revenueObj != null ? ((Number) revenueObj).doubleValue() : 0.0;
                    metrics.put("total_revenue", revenue);
                }
            }
            
            return metrics;
        }
    }

    public static class OptimizationOpportunityJob implements ComputeJob<List<String>>, Serializable {
        @Override
        public List<String> execute(JobExecutionContext context, Object... args) {
            Map<String, Double> metrics = (Map<String, Double>) args[0];
            List<String> opportunities = new ArrayList<>();
            
            double revenue = metrics.getOrDefault("total_revenue", 0.0);
            if (revenue < 1000.0) {
                opportunities.add("Increase marketing efforts");
                opportunities.add("Expand music catalog");
            }
            opportunities.add("Optimize pricing strategy");
            
            return opportunities;
        }
    }

    public static class ActionPlanJob implements ComputeJob<String>, Serializable {
        @Override
        public String execute(JobExecutionContext context, Object... args) {
            List<String> opportunities = Arrays.asList((String[]) args[0]);
            return "Implement " + opportunities.size() + " optimization strategies";
        }
    }
}