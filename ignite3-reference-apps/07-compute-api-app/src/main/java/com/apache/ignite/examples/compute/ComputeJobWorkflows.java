package com.apache.ignite.examples.compute;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.compute.*;
import org.apache.ignite.deployment.DeploymentUnit;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    // Deployment unit configuration
    private static final String DEPLOYMENT_UNIT_NAME = "compute-jobs";
    private static final String DEPLOYMENT_UNIT_VERSION = "1.0.0";

    /**
     * Get deployment units for this application.
     */
    private static List<DeploymentUnit> getDeploymentUnits() {
        // Use the deployment unit that should be deployed via REST API or CLI
        return List.of(new DeploymentUnit(DEPLOYMENT_UNIT_NAME, DEPLOYMENT_UNIT_VERSION));
    }

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
            JobDescriptor<Void, List<String>> segmentJob = JobDescriptor.builder(CustomerSegmentJob.class)
                    .units(getDeploymentUnits())
                    .build();
            List<String> segments = client.compute()
                    .execute(JobTarget.anyNode(client.clusterNodes()), segmentJob, null);
            
            System.out.println("    >>> Step 1: Identified " + segments.size() + " customer segments");
            
            // Step 2: Analyze each segment in parallel
            List<CompletableFuture<String>> futures = new ArrayList<>();
            for (String segment : segments) {
                JobDescriptor<String, String> analysisJob = JobDescriptor.builder(SegmentAnalysisJob.class)
                        .units(getDeploymentUnits())
                        .build();
                
                CompletableFuture<String> future = client.compute()
                        .executeAsync(JobTarget.anyNode(client.clusterNodes()), analysisJob, segment);
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
            JobDescriptor<Void, Map<String, Integer>> patternJob = JobDescriptor.builder(ListeningPatternJob.class)
                    .units(getDeploymentUnits())
                    .build();
            Map<String, Integer> patterns = client.compute()
                    .execute(JobTarget.anyNode(client.clusterNodes()), patternJob, null);
            
            System.out.println("    >>> Step 1: Analyzed listening patterns (" + patterns.size() + " genres)");
            
            // Step 2: Generate recommendations based on patterns
            String topGenre = patterns.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Rock");
            
            JobDescriptor<String, List<String>> recommendationJob = JobDescriptor.builder(RecommendationJob.class)
                    .units(getDeploymentUnits())
                    .build();
            
            List<String> recommendations = client.compute()
                    .execute(JobTarget.anyNode(client.clusterNodes()), recommendationJob, topGenre);
            
            System.out.println("    >>> Step 2: Generated " + recommendations.size() + " recommendations");
            
            // Step 3: Rank and filter recommendations
            JobDescriptor<List<String>, List<String>> rankingJob = JobDescriptor.builder(RankingJob.class)
                    .units(getDeploymentUnits())
                    .build();
            
            List<String> rankedRecommendations = client.compute()
                    .execute(JobTarget.anyNode(client.clusterNodes()), rankingJob, recommendations);
            
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
            JobDescriptor<Void, Map<String, Double>> metricsJob = JobDescriptor.builder(RevenueMetricsJob.class)
                    .units(getDeploymentUnits())
                    .build();
            Map<String, Double> metrics = client.compute()
                    .execute(JobTarget.anyNode(client.clusterNodes()), metricsJob, null);
            
            System.out.println("    >>> Step 1: Current revenue metrics calculated");
            
            // Step 2: Identify optimization opportunities
            JobDescriptor<Map<String, Double>, List<String>> opportunityJob = JobDescriptor.builder(OptimizationOpportunityJob.class)
                    .units(getDeploymentUnits())
                    .build();
            
            List<String> opportunities = client.compute()
                    .execute(JobTarget.anyNode(client.clusterNodes()), opportunityJob, metrics);
            
            System.out.println("    >>> Step 2: Identified " + opportunities.size() + " optimization opportunities");
            
            // Step 3: Generate action plan
            JobDescriptor<List<String>, String> actionPlanJob = JobDescriptor.builder(ActionPlanJob.class)
                    .units(getDeploymentUnits())
                    .build();
            
            String actionPlan = client.compute()
                    .execute(JobTarget.anyNode(client.clusterNodes()), actionPlanJob, opportunities);
            
            System.out.println("    <<< Revenue optimization workflow completed:");
            System.out.println("         Current revenue: $" + String.format("%.2f", metrics.getOrDefault("total_revenue", 0.0)));
            System.out.println("         Action plan: " + actionPlan);
        } catch (Exception e) {
            System.err.println("    !!! Revenue optimization workflow failed: " + e.getMessage());
        }
    }

    // Job implementations

    public static class CustomerSegmentJob implements ComputeJob<Void, List<String>> {
        @Override
        public CompletableFuture<List<String>> executeAsync(JobExecutionContext context, Void arg) {
            return CompletableFuture.completedFuture(Arrays.asList("High Value", "Regular", "New Customers"));
        }
    }

    public static class SegmentAnalysisJob implements ComputeJob<String, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, String segment) {
            return CompletableFuture.supplyAsync(() -> {
                IgniteSql sql = context.ignite().sql();
                
                try (ResultSet<SqlRow> result = sql.execute(null,
                        "SELECT COUNT(*) as customer_count FROM Customer LIMIT 1")) {
                    
                    if (result.hasNext()) {
                        int count = (int) result.next().longValue("customer_count");
                        return count + " customers analyzed";
                    }
                    return "No data";
                }
            });
        }
    }

    public static class ListeningPatternJob implements ComputeJob<Void, Map<String, Integer>> {
        @Override
        public CompletableFuture<Map<String, Integer>> executeAsync(JobExecutionContext context, Void arg) {
            return CompletableFuture.supplyAsync(() -> {
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
            });
        }
    }

    public static class RecommendationJob implements ComputeJob<String, List<String>> {
        @Override
        public CompletableFuture<List<String>> executeAsync(JobExecutionContext context, String genre) {
            return CompletableFuture.supplyAsync(() -> {
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
            });
        }
    }

    public static class RankingJob implements ComputeJob<List<String>, List<String>> {
        @Override
        public CompletableFuture<List<String>> executeAsync(JobExecutionContext context, List<String> tracks) {
            return CompletableFuture.supplyAsync(() -> {
                List<String> rankedTracks = new ArrayList<>(tracks);
                Collections.shuffle(rankedTracks); // Simple ranking simulation
                return rankedTracks;
            });
        }
    }

    public static class RevenueMetricsJob implements ComputeJob<Void, Map<String, Double>> {
        @Override
        public CompletableFuture<Map<String, Double>> executeAsync(JobExecutionContext context, Void arg) {
            return CompletableFuture.supplyAsync(() -> {
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
            });
        }
    }

    public static class OptimizationOpportunityJob implements ComputeJob<Map<String, Double>, List<String>> {
        @Override
        public CompletableFuture<List<String>> executeAsync(JobExecutionContext context, Map<String, Double> metrics) {
            return CompletableFuture.supplyAsync(() -> {
                List<String> opportunities = new ArrayList<>();
                
                double revenue = metrics.getOrDefault("total_revenue", 0.0);
                if (revenue < 1000.0) {
                    opportunities.add("Increase marketing efforts");
                    opportunities.add("Expand music catalog");
                }
                opportunities.add("Optimize pricing strategy");
                
                return opportunities;
            });
        }
    }

    public static class ActionPlanJob implements ComputeJob<List<String>, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, List<String> opportunities) {
            return CompletableFuture.completedFuture("Implement " + opportunities.size() + " optimization strategies");
        }
    }
}