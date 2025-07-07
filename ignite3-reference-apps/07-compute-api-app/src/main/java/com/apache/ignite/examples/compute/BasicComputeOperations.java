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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Demonstrates basic compute operations using the Apache Ignite 3 Compute API.
 * 
 * Covers fundamental job execution patterns including simple jobs, parameterized
 * jobs, SQL-based processing, and asynchronous execution. Shows how to submit
 * compute jobs to distributed cluster nodes with proper error handling.
 * 
 * Key concepts:
 * - Job creation and submission
 * - Parameterized job execution
 * - SQL processing within jobs
 * - Asynchronous job patterns
 * - Basic error handling
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

    /**
     * Execute basic compute operation demonstrations.
     * 
     * Shows progression from simple jobs to more complex patterns,
     * building understanding of distributed job execution concepts.
     */
    private void runBasicComputeOperations(IgniteClient client) {
        System.out.println("\n--- Basic Job Execution ---");
        System.out.println("    Learning fundamental compute patterns");
        
        // Deploy job classes for execution
        if (!ComputeJobDeployment.deployJobClasses()) {
            System.out.println(">>> Continuing with development deployment units");
        }
        
        // Simple job execution
        demonstrateSimpleJobs(client);
        
        // Parameterized jobs
        demonstrateParameterizedJobs(client);
        
        // SQL-based jobs
        demonstrateSqlJobs(client);
        
        // Asynchronous execution
        demonstrateAsyncJobs(client);
        
        System.out.println("\n>>> Basic compute operations completed successfully");
    }

    /**
     * Demonstrate simple job execution without parameters.
     * 
     * Shows basic job submission and result handling, fundamental
     * building blocks for distributed processing patterns.
     */
    private void demonstrateSimpleJobs(IgniteClient client) {
        System.out.println("\n--- Simple Job Execution");
        System.out.println(">>> Executing basic jobs on cluster nodes");
        
        try {
            // Hello World job - simplest possible compute job
            JobDescriptor<Void, String> helloJob = JobDescriptor.builder(HelloWorldJob.class)
                    .units(ComputeJobDeployment.getDeploymentUnits())
                    .build();
            
            String helloResult = client.compute()
                    .execute(JobTarget.anyNode(client.clusterNodes()), helloJob, null);
            
            System.out.println("<<< Job result: " + helloResult);
            
            // Node information job - access execution context
            JobDescriptor<Void, String> nodeJob = JobDescriptor.builder(NodeInfoJob.class)
                    .units(ComputeJobDeployment.getDeploymentUnits())
                    .build();
            
            String nodeInfo = client.compute()
                    .execute(JobTarget.anyNode(client.clusterNodes()), nodeJob, null);
            
            System.out.println("<<< Node info: " + nodeInfo);
            
        } catch (Exception e) {
            System.err.println("!!! Simple job execution failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrate parameterized job execution.
     * 
     * Shows how to pass parameters to jobs and process them,
     * enabling flexible job patterns for various use cases.
     */
    private void demonstrateParameterizedJobs(IgniteClient client) {
        System.out.println("\n--- Parameterized Jobs");
        System.out.println(">>> Executing jobs with input parameters");
        
        try {
            // Artist search job with string parameter
            JobDescriptor<String, String> searchJob = JobDescriptor.builder(ArtistSearchJob.class)
                    .units(ComputeJobDeployment.getDeploymentUnits())
                    .build();
            
            String searchResult = client.compute()
                    .execute(JobTarget.anyNode(client.clusterNodes()), searchJob, "AC/DC");
            
            System.out.println("<<< Search result: " + searchResult);
            
            // Track count job with no parameters but SQL execution
            JobDescriptor<Void, Integer> countJob = JobDescriptor.builder(TrackCountJob.class)
                    .units(ComputeJobDeployment.getDeploymentUnits())
                    .build();
            
            Integer trackCount = client.compute()
                    .execute(JobTarget.anyNode(client.clusterNodes()), countJob, null);
            
            System.out.println("<<< Total tracks in database: " + trackCount);
            
        } catch (Exception e) {
            System.err.println("!!! Parameterized job execution failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrate SQL-based job processing.
     * 
     * Shows how jobs can execute SQL queries and process results,
     * combining distributed computing with database capabilities.
     */
    private void demonstrateSqlJobs(IgniteClient client) {
        System.out.println("\n--- SQL-based Jobs");
        System.out.println(">>> Running jobs that execute database queries");
        
        try {
            // Genre analysis job with complex SQL processing
            JobDescriptor<Void, String> genreJob = JobDescriptor.builder(GenreAnalysisJob.class)
                    .units(ComputeJobDeployment.getDeploymentUnits())
                    .build();
            
            String genreResult = client.compute()
                    .execute(JobTarget.anyNode(client.clusterNodes()), genreJob, null);
            
            System.out.println("<<< Genre analysis: " + genreResult);
            
        } catch (Exception e) {
            System.err.println("!!! SQL job execution failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrate asynchronous job execution.
     * 
     * Shows non-blocking job submission and result handling,
     * enabling concurrent processing patterns for better performance.
     */
    private void demonstrateAsyncJobs(IgniteClient client) {
        System.out.println("\n--- Asynchronous Job Execution");
        System.out.println(">>> Running jobs asynchronously without blocking");
        
        try {
            // Submit multiple jobs asynchronously
            JobDescriptor<Void, String> helloJob = JobDescriptor.builder(HelloWorldJob.class)
                    .units(ComputeJobDeployment.getDeploymentUnits())
                    .build();
            
            JobDescriptor<Void, Integer> countJob = JobDescriptor.builder(TrackCountJob.class)
                    .units(ComputeJobDeployment.getDeploymentUnits())
                    .build();
            
            CompletableFuture<String> helloFuture = client.compute()
                    .executeAsync(JobTarget.anyNode(client.clusterNodes()), helloJob, null);
            
            CompletableFuture<Integer> countFuture = client.compute()
                    .executeAsync(JobTarget.anyNode(client.clusterNodes()), countJob, null);
            
            System.out.println(">>> Both jobs started asynchronously");
            
            // Wait for results
            String helloResult = helloFuture.join();
            Integer countResult = countFuture.join();
            
            System.out.println("<<< Async job 1: " + helloResult);
            System.out.println("<<< Async job 2: " + countResult + " tracks");
            
        } catch (Exception e) {
            System.err.println("!!! Async job execution failed: " + e.getMessage());
        }
    }

    // Job implementations

    /**
     * Simple job that returns a greeting message.
     * Demonstrates basic job structure and execution context access.
     */
    public static class HelloWorldJob implements ComputeJob<Void, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Void arg) {
            return CompletableFuture.completedFuture("Hello from Ignite Compute!");
        }
    }

    /**
     * Job that provides information about the execution node.
     * Shows how to access node context within job execution.
     */
    public static class NodeInfoJob implements ComputeJob<Void, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Void arg) {
            return CompletableFuture.completedFuture("Node: " + context.ignite().name());
        }
    }

    /**
     * Job that searches for an artist by name.
     * Demonstrates parameterized job execution with SQL queries.
     */
    public static class ArtistSearchJob implements ComputeJob<String, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, String artistName) {
            return CompletableFuture.supplyAsync(() -> {
                IgniteSql sql = context.ignite().sql();
                
                try (ResultSet<SqlRow> result = sql.execute(null,
                        "SELECT Name FROM Artist WHERE Name LIKE ?", "%" + artistName + "%")) {
                    
                    if (result.hasNext()) {
                        return "Found: " + result.next().stringValue("Name");
                    }
                    return "Artist not found: " + artistName;
                }
            });
        }
    }

    /**
     * Job that counts total tracks in the database.
     * Shows SQL aggregation within distributed job execution.
     */
    public static class TrackCountJob implements ComputeJob<Void, Integer> {
        @Override
        public CompletableFuture<Integer> executeAsync(JobExecutionContext context, Void arg) {
            return CompletableFuture.supplyAsync(() -> {
                IgniteSql sql = context.ignite().sql();
                
                try (ResultSet<SqlRow> result = sql.execute(null, "SELECT COUNT(*) as track_count FROM Track")) {
                    if (result.hasNext()) {
                        return (int) result.next().longValue("track_count");
                    }
                    return 0;
                }
            });
        }
    }

    /**
     * Job that analyzes genre popularity.
     * Demonstrates complex SQL processing with result aggregation.
     */
    public static class GenreAnalysisJob implements ComputeJob<Void, String> {
        @Override
        public CompletableFuture<String> executeAsync(JobExecutionContext context, Void arg) {
            return CompletableFuture.supplyAsync(() -> {
                IgniteSql sql = context.ignite().sql();
                
                try (ResultSet<SqlRow> result = sql.execute(null,
                        "SELECT g.Name, COUNT(t.TrackId) as track_count " +
                        "FROM Genre g JOIN Track t ON g.GenreId = t.GenreId " +
                        "GROUP BY g.GenreId, g.Name " +
                        "ORDER BY track_count DESC LIMIT 1")) {
                    
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