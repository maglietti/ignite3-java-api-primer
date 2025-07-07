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

import org.apache.ignite.deployment.DeploymentUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

/**
 * Utility for deploying compute job classes to Ignite clusters.
 * 
 * Handles JAR deployment via REST API with fallback options for development
 * and production environments. Separates deployment concerns from core compute
 * demonstrations to maintain educational focus.
 * 
 * Key concepts:
 * - REST API deployment for automation
 * - Deployment unit management
 * - Error handling with recovery suggestions
 * - Development vs production deployment strategies
 */
public class ComputeJobDeployment {

    private static final Logger logger = LoggerFactory.getLogger(ComputeJobDeployment.class);

    // Deployment configuration
    private static final String DEPLOYMENT_UNIT_NAME = "compute-jobs";
    private static final String DEPLOYMENT_UNIT_VERSION = "1.0.0";
    private static final String DEFAULT_REST_HOST = "localhost";
    private static final int DEFAULT_REST_PORT = 10300;

    /**
     * Get deployment units for compute jobs.
     * 
     * @return List of deployment units for job class loading
     */
    public static List<DeploymentUnit> getDeploymentUnits() {
        return List.of(new DeploymentUnit(DEPLOYMENT_UNIT_NAME, DEPLOYMENT_UNIT_VERSION));
    }

    /**
     * Deploy compute job classes with automatic JAR detection and REST API deployment.
     * 
     * This method encapsulates the deployment complexity to keep core compute
     * demonstrations focused on educational patterns rather than infrastructure.
     * 
     * @return true if deployment successful or already exists, false if failed
     */
    public static boolean deployJobClasses() {
        System.out.println(">>> Deploying job classes to cluster...");
        
        try {
            // Find JAR file
            Path jarPath = findJobJar();
            if (jarPath == null) {
                System.err.println("!!! JAR file not found. Run 'mvn package' first.");
                return false;
            }
            
            System.out.println(">>> JAR file found: " + jarPath);
            
            // Deploy via REST API
            boolean deployed = deployViaRestApi(jarPath);
            if (deployed) {
                System.out.println(">>> Job classes deployed successfully");
                return true;
            } else {
                printFallbackInstructions(jarPath);
                return false;
            }
            
        } catch (Exception e) {
            logger.warn("Deployment failed", e);
            System.err.println("!!! Deployment failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Find the compute jobs JAR file in the target directory.
     */
    private static Path findJobJar() {
        try {
            Path targetDir = Paths.get("target");
            if (!Files.exists(targetDir)) {
                return null;
            }
            
            return Files.list(targetDir)
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .filter(path -> !path.getFileName().toString().endsWith("-sources.jar"))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Deploy JAR via REST API using multipart form data.
     * 
     * Uses Ignite 3's management REST API for programmatic deployment,
     * enabling CI/CD integration and automated development workflows.
     */
    private static boolean deployViaRestApi(Path jarPath) {
        try {
            String restApiUrl = String.format("http://%s:%d/management/v1/deployment/units/%s/%s?deployMode=MAJORITY",
                    DEFAULT_REST_HOST, DEFAULT_REST_PORT, DEPLOYMENT_UNIT_NAME, DEPLOYMENT_UNIT_VERSION);
            
            byte[] jarContent = Files.readAllBytes(jarPath);
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            String multipartData = createMultipartData(boundary, jarPath.getFileName().toString(), jarContent);
            
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(restApiUrl))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofString(multipartData, StandardCharsets.ISO_8859_1))
                    .timeout(Duration.ofSeconds(30))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                System.out.println(">>> JAR deployed successfully via REST API");
                return true;
            } else if (response.statusCode() == 409) {
                System.out.println(">>> Deployment unit already exists (HTTP 409) - continuing");
                return true;
            } else {
                System.err.println("!!! REST API deployment failed: HTTP " + response.statusCode());
                return false;
            }
            
        } catch (Exception e) {
            logger.debug("REST API deployment failed", e);
            return false;
        }
    }

    /**
     * Create multipart form data for JAR upload.
     */
    private static String createMultipartData(String boundary, String fileName, byte[] jarContent) {
        StringBuilder multipart = new StringBuilder();
        multipart.append("--").append(boundary).append("\r\n");
        multipart.append("Content-Disposition: form-data; name=\"unitContent\"; filename=\"").append(fileName).append("\"\r\n");
        multipart.append("Content-Type: application/java-archive\r\n");
        multipart.append("\r\n");
        
        // Convert bytes to ISO-8859-1 string for HTTP transport
        multipart.append(new String(jarContent, StandardCharsets.ISO_8859_1));
        
        multipart.append("\r\n--").append(boundary).append("--\r\n");
        return multipart.toString();
    }

    /**
     * Print fallback deployment instructions when REST API fails.
     */
    private static void printFallbackInstructions(Path jarPath) {
        System.err.println("!!! Automatic deployment failed. Use one of these alternatives:");
        System.err.println("");
        System.err.println("    Option 1: Deployment Script");
        System.err.println("    ./deploy-jar.sh " + DEPLOYMENT_UNIT_NAME + " " + DEPLOYMENT_UNIT_VERSION + " " + jarPath);
        System.err.println("");
        System.err.println("    Option 2: Manual REST API");
        System.err.println("    curl -X POST \\");
        System.err.println("      \"http://localhost:10300/management/v1/deployment/units/" + DEPLOYMENT_UNIT_NAME + "/" + DEPLOYMENT_UNIT_VERSION + "?deployMode=MAJORITY\" \\");
        System.err.println("      -H \"Content-Type: multipart/form-data\" \\");
        System.err.println("      -F \"unitContent=@" + jarPath + "\"");
        System.err.println("");
        System.err.println("    Option 3: Docker CLI");
        System.err.println("    docker run --rm -it --network=host apacheignite/ignite:3.0.0 cli");
        System.err.println("    cluster unit deploy " + DEPLOYMENT_UNIT_NAME + " " + jarPath);
    }
}