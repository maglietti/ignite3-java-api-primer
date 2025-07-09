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
 * Demonstrates Apache Ignite 3 client connection patterns for different deployment scenarios.
 * 
 * This class showcases three essential connection patterns:
 * - Single-node connection for development environments</li>
 * - Multi-node connection with automatic failover for production deployments</li>
 * - Connection timing and basic performance validation</li>
 * 
 * Ignite 3 Concepts Demonstrated:
 *
 * - Client builder pattern with address configuration</li>
 * - Automatic connection failover and load balancing</li>
 * - Cluster topology discovery</li>
 * - Basic SQL operations for connectivity validation</li>
 * 
 * Usage: Run this class to see connection examples in action.
 * Expected warnings for ports 10801/10802 demonstrate failover behavior.
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
        System.out.println("  Note: Connection warnings for ports 10801/10802 are expected");
        System.out.println("  This demonstrates failover behavior in production clusters");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(
                    "localhost:10800",  // Primary node (running)
                    "localhost:10801",  // Backup node (not running - shows failover)
                    "localhost:10802"   // Backup node (not running - shows failover)
                )
                .build()) {
            
            System.out.println("  Connected with failover support");
            System.out.println("  Active connections: " + client.connections());
            
            var clusterNodes = client.clusterNodes();
            System.out.println("  Cluster size: " + clusterNodes.size() + " nodes");
            
            clusterNodes.forEach(node -> {
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
            
            long start = System.currentTimeMillis();
            var resultSet = client.sql().execute(null, "SELECT CURRENT_TIMESTAMP as server_time");
            long end = System.currentTimeMillis();
            
            while (resultSet.hasNext()) {
                var row = resultSet.next();
                System.out.println("  Server time: " + row.value("server_time"));
                System.out.println("  Response time: " + (end - start) + "ms");
            }
            
        } catch (Exception e) {
            if (e.getMessage().contains("Handshake error")) {
                System.err.println("  Some backup connections failed (normal in multi-node setup)");
            } else {
                System.err.println("  Performance test failed: " + e.getMessage());
            }
        }
    }
}