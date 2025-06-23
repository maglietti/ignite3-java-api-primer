package com.apache.ignite.examples.compute;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.JobDescriptor;
import org.apache.ignite.compute.JobExecutionContext;
import org.apache.ignite.compute.JobTarget;
import org.apache.ignite.deployment.DeploymentUnit;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.List;

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
        
        // Deploy job classes before executing them
        try {
            deployJobClasses(client);
            System.out.println("    >>> Job classes deployed successfully");
        } catch (Exception e) {
            System.err.println("    !!! Failed to deploy job classes: " + e.getMessage());
            System.err.println("    !!! Jobs will not execute properly without deployment");
        }
        
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
     * Deploy the JAR containing job classes to the Ignite cluster.
     */
    private void deployJobClasses(IgniteClient client) throws IOException {
        System.out.println("    >>> Deploying job classes to cluster...");
        
        // Find the JAR file in the target directory
        Path jarPath = Paths.get("target/07-compute-api-app-1.0.0.jar");
        if (!Files.exists(jarPath)) {
            throw new IOException("JAR file not found: " + jarPath + ". Run 'mvn package' first.");
        }
        
        System.out.println("    >>> JAR file found: " + jarPath);
        
        try {
            // Attempt programmatic deployment via REST API
            deployViaRestAPI(jarPath);
            System.out.println("    >>> JAR deployed successfully via REST API");
            
        } catch (Exception e) {
            System.out.println("    >>> REST API deployment failed: " + e.getMessage());
            System.out.println("    >>> Fallback: Deploy manually using CLI or Docker:");
            System.out.println("    >>>   CLI: ignite deployment deploy " + DEPLOYMENT_UNIT_NAME + " " + jarPath.toAbsolutePath());
            System.out.println("    >>>   Docker: docker run --rm -it --network=host apacheignite/ignite:3.0.0 cli");
            System.out.println("    >>> Continuing with job execution (will fail if not deployed)...");
        }
    }
    
    /**
     * Deploy JAR file via REST API using multipart form upload.
     */
    private void deployViaRestAPI(Path jarPath) throws Exception {
        // Ignite REST API typically runs on port 10300
        String restApiUrl = "http://127.0.0.1:10300/management/v1/deployment/units/" 
                          + DEPLOYMENT_UNIT_NAME + "/" + DEPLOYMENT_UNIT_VERSION + "?deployMode=MAJORITY";
        
        // Read JAR file content
        byte[] jarContent = Files.readAllBytes(jarPath);
        
        // Create multipart form data
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        String multipartData = createMultipartData(boundary, jarPath.getFileName().toString(), jarContent);
        
        // Create HTTP request
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(restApiUrl))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(multipartData, StandardCharsets.ISO_8859_1))
                .build();
        
        // Send request and check response
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200 || response.statusCode() == 201) {
            System.out.println("    >>> Deployment successful (HTTP " + response.statusCode() + ")");
        } else if (response.statusCode() == 409) {
            System.out.println("    >>> Deployment unit already exists (HTTP 409) - continuing");
        } else {
            throw new RuntimeException("Deployment failed with HTTP " + response.statusCode() + ": " + response.body());
        }
    }
    
    /**
     * Create multipart form data for JAR file upload.
     */
    private String createMultipartData(String boundary, String filename, byte[] content) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"unitContent\"; filename=\"").append(filename).append("\"\r\n");
        sb.append("Content-Type: application/octet-stream\r\n");
        sb.append("\r\n");
        
        // Convert binary content to ISO-8859-1 string (preserves bytes)
        sb.append(new String(content, StandardCharsets.ISO_8859_1));
        
        sb.append("\r\n--").append(boundary).append("--\r\n");
        
        return sb.toString();
    }

    // Deployment unit configuration
    private static final String DEPLOYMENT_UNIT_NAME = "compute-jobs";
    private static final String DEPLOYMENT_UNIT_VERSION = "1.0.0";

    /**
     * Get deployment units for this application.
     * Returns the deployment unit that should contain our job classes.
     */
    private static List<DeploymentUnit> getDeploymentUnits() {
        // Use the deployment unit that should be deployed via REST API or CLI
        return List.of(new DeploymentUnit(DEPLOYMENT_UNIT_NAME, DEPLOYMENT_UNIT_VERSION));
    }

    /**
     * Demonstrates simple job execution without parameters.
     */
    private void demonstrateSimpleJobs(IgniteClient client) {
        System.out.println("\n    --- Simple Job Execution");
        System.out.println("    >>> Executing basic jobs on cluster nodes");
        
        // Simple greeting job
        JobDescriptor<Void, String> greetingJob = JobDescriptor.builder(HelloWorldJob.class)
                .units(getDeploymentUnits())  // Get appropriate deployment units for current mode
                .build();
        
        try {
            String result = client.compute()
                    .execute(JobTarget.anyNode(client.clusterNodes()), greetingJob, null);
            
            System.out.println("    <<< Job result: " + result);
        } catch (Exception e) {
            System.err.println("    !!! Job execution failed: " + e.getMessage());
        }
        
        // Node information job
        JobDescriptor<Void, String> nodeInfoJob = JobDescriptor.builder(NodeInfoJob.class)
                .units(getDeploymentUnits())  // Empty list for embedded mode
                .build();
        
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
        JobDescriptor<String, String> searchJob = JobDescriptor.builder(ArtistSearchJob.class)
                .units(getDeploymentUnits())  // Empty list for embedded mode
                .build();
        
        try {
            String result = client.compute()
                    .execute(JobTarget.anyNode(client.clusterNodes()), searchJob, "AC/DC");
            
            System.out.println("    <<< Search result: " + result);
        } catch (Exception e) {
            System.err.println("    !!! Search job failed: " + e.getMessage());
        }
        
        // Track count job
        JobDescriptor<Void, Integer> countJob = JobDescriptor.builder(TrackCountJob.class)
                .units(getDeploymentUnits())  // Empty list for embedded mode
                .build();
        
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
        JobDescriptor<Void, String> genreJob = JobDescriptor.builder(GenreAnalysisJob.class)
                .units(getDeploymentUnits())  // Empty list for embedded mode
                .build();
        
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
        JobDescriptor<Void, String> job1 = JobDescriptor.builder(HelloWorldJob.class)
                .units(getDeploymentUnits())  // Empty list for embedded mode
                .build();
        JobDescriptor<Void, Integer> job2 = JobDescriptor.builder(TrackCountJob.class)
                .units(getDeploymentUnits())  // Empty list for embedded mode
                .build();
        
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