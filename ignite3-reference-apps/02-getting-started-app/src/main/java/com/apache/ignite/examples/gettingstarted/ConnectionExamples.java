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

package com.apache.ignite.examples.gettingstarted;

import org.apache.ignite.client.IgniteClient;

/**
 * Simple connection examples for different scenarios.
 * 
 * Shows:
 * - Basic connection
 * - Multi-node connection (for production)
 * - Performance testing
 */
public class ConnectionExamples {
    
    public static void main(String[] args) {
        System.out.println("=== Connection Examples ===");
        
        // Example 1: Basic connection (development)
        basicConnection();
        
        // Example 2: Multi-node connection (production)
        multiNodeConnection();
        
        // Example 3: Performance test
        connectionWithTimeouts();
    }
    
    private static void basicConnection() {
        System.out.println("\n1. Basic Connection:");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800")
                .build()) {
            
            System.out.println("  Connected to: " + client.connections());
            
            // Simple health check
            var result = client.sql().execute(null, "SELECT 1 as test");
            if (result.hasNext()) {
                System.out.println("  Health check: OK");
            }
            
        } catch (Exception e) {
            System.err.println("  Connection failed: " + e.getMessage());
        }
    }
    
    private static void multiNodeConnection() {
        System.out.println("\n2. Multi-Node Connection:");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(
                    "localhost:10800",  // First node
                    "localhost:10801",  // Second node  
                    "localhost:10802"   // Third node
                )
                .build()) {
            
            System.out.println("  Connected with failover support");
            System.out.println("  Active connections: " + client.connections());
            
            // Check cluster size
            var result = client.clusterNodes();
            System.out.println("  Cluster size: " + result.size() + " nodes");
            
            // Process each node using streams
            result.forEach(node -> {
                System.out.println("  Node: " + node.name() + " (" + node.address() + ")");
            });
            
        } catch (Exception e) {
            System.err.println("  Multi-node connection failed: " + e.getMessage());
            System.err.println("  (This is normal with single-node setup)");
        }
    }
    
    private static void connectionWithTimeouts() {
        System.out.println("\n3. Basic Performance Test:");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800")
                .build()) {
            
            System.out.println("  Connected for performance testing");
            
            // Test operation timing
            long start = System.currentTimeMillis();
            var result = client.sql().execute(null, "SELECT CURRENT_TIMESTAMP");
            long end = System.currentTimeMillis();
            
            // Use hasRowSet() and next() directly on ResultSet
            if (result.hasRowSet()) {
                var row = result.next();
                System.out.println("  Server time: " + row.value("CURRENT_TIMESTAMP"));
                System.out.println("  Response time: " + (end - start) + "ms");
            }
            
        } catch (Exception e) {
            if (e.getMessage().contains("Handshake error")) {
                System.err.println("  Some backup connections failed (normal in multi-node setup)");
            } else {
                System.err.println("  Timeout connection failed: " + e.getMessage());
            }
        }
    }
}