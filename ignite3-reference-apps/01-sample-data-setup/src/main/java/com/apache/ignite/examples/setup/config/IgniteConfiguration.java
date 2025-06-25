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

package com.apache.ignite.examples.setup.config;

import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ignite client configuration and connection management utilities.
 * 
 * This class demonstrates proper Ignite 3 client connection patterns:
 * - Builder Pattern: Uses IgniteClient.builder() for flexible configuration
 * - Error Handling: Comprehensive exception handling with meaningful messages
 * - Connection Flexibility: Support for single or multiple cluster addresses
 * - Resource Management: Clients returned by these methods should be used with try-with-resources
 * 
 * Production Considerations:
 * - Connection pooling is handled internally by the IgniteClient
 * - Each client can handle multiple concurrent operations
 * - Always close clients properly to release resources
 * - Consider connection timeouts and retry policies for production use
 */
public class IgniteConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(IgniteConfiguration.class);
    
    /**
     * Default Ignite cluster address for local development.
     * Points to standard Ignite 3 client connector port.
     */
    public static final String DEFAULT_IGNITE_ADDRESS = "127.0.0.1:10800";
    
    /**
     * Creates an Ignite client connection using the default local address.
     * Convenience method for development and testing scenarios.
     * 
     * @return Connected IgniteClient instance
     * @throws RuntimeException if connection fails
     */
    public static IgniteClient createClient() {
        return createClient(DEFAULT_IGNITE_ADDRESS);
    }
    
    /**
     * Creates an Ignite client connection to a specific cluster address.
     * 
     * This demonstrates the basic Ignite 3 connection pattern:
     * 1. Use IgniteClient.builder() for configuration
     * 2. Specify cluster addresses
     * 3. Handle connection errors appropriately
     * 
     * @param address Cluster address in format "host:port"
     * @return Connected IgniteClient instance  
     * @throws RuntimeException if connection fails
     */
    public static IgniteClient createClient(String address) {
        logger.info("Creating Ignite client connection to: {}", address);
        
        try {
            // Use the builder pattern for flexible client configuration
            // Additional options like authentication, SSL, timeouts can be added here
            IgniteClient client = IgniteClient.builder()
                    .addresses(address)
                    .build();
            
            logger.info("Successfully connected to Ignite cluster at: {}", address);
            return client;
            
        } catch (Exception e) {
            logger.error("Failed to connect to Ignite cluster at: {}. Error: {}", address, e.getMessage());
            throw new RuntimeException("Unable to establish connection to Ignite cluster", e);
        }
    }
    
    public static IgniteClient createClient(String... addresses) {
        logger.info("Creating Ignite client connection to cluster: {}", String.join(", ", addresses));
        
        try {
            IgniteClient client = IgniteClient.builder()
                    .addresses(addresses)
                    .build();
            
            logger.info("Successfully connected to Ignite cluster");
            return client;
            
        } catch (Exception e) {
            logger.error("Failed to connect to Ignite cluster. Error: {}", e.getMessage());
            throw new RuntimeException("Unable to establish connection to Ignite cluster", e);
        }
    }
    
    public static void closeClient(IgniteClient client) {
        if (client != null) {
            try {
                client.close();
                logger.info("Ignite client connection closed successfully");
            } catch (Exception e) {
                logger.warn("Error closing Ignite client: {}", e.getMessage());
            }
        }
    }
    
    public static boolean testConnection(String address) {
        try (IgniteClient testClient = createClient(address)) {
            logger.info("Connection test successful for: {}", address);
            return true;
        } catch (Exception e) {
            logger.error("Connection test failed for: {}. Error: {}", address, e.getMessage());
            return false;
        }
    }
}