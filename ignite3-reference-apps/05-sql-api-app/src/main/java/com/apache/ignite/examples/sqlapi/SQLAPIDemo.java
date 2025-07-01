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

package com.apache.ignite.examples.sqlapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demonstrates Apache Ignite 3 SQL API for relational data access patterns.
 * 
 * Orchestrates focused demonstrations of SQL interface usage patterns from
 * simple queries to complex transactional operations. Each demonstration
 * focuses on teaching specific Java API concepts rather than SQL syntax.
 * 
 * Learning progression:
 * 1. BasicSQLOperations - Simple queries, parameters, ResultSet processing
 * 2. AdvancedSQLOperations - JOINs, aggregations, batch operations, mapping
 * 3. TransactionSQLOperations - SQL within ACID transactions
 * 
 * All demonstrations use the music store sample data to show practical
 * usage patterns for building distributed applications with relational
 * data access requirements.
 * 
 * Prerequisites:
 * - Running Ignite cluster
 * - Sample music store data loaded
 */
public class SQLAPIDemo {

    private static final Logger logger = LoggerFactory.getLogger(SQLAPIDemo.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Apache Ignite 3 SQL API Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Demonstrating relational data access patterns");
        System.out.println();
        
        try {
            // Run each demo in learning progression
            runDemo("Basic SQL Operations", 
                () -> BasicSQLOperations.main(new String[]{clusterAddress}));
                
            runDemo("Advanced SQL Operations", 
                () -> AdvancedSQLOperations.main(new String[]{clusterAddress}));
                
            runDemo("Transaction SQL Operations", 
                () -> TransactionSQLOperations.main(new String[]{clusterAddress}));
                
            runDemo("Production Analytics Patterns", 
                () -> ProductionAnalyticsPatterns.main(new String[]{clusterAddress}));
            
            System.out.println();
            System.out.println("=== SQL API Demo completed successfully ===");
            
        } catch (Exception e) {
            logger.error("Demo execution failed", e);
            System.err.println("!!! Demo failed: " + e.getMessage());
            System.err.println("!!! Make sure Ignite cluster is running and sample data is loaded.");
        }
    }

    private static String getCurrentStep(String name) {
        switch (name) {
            case "Basic SQL Operations": return "1";
            case "Advanced SQL Operations": return "2";
            case "Transaction SQL Operations": return "3";
            case "Production Analytics Patterns": return "4";
            default: return "?";
        }
    }

    private static void runDemo(String name, Runnable demo) {
        System.out.println();
        System.out.println("=== [" + getCurrentStep(name) + "/4] " + name + " ===");
        
        try {
            demo.run();
        } catch (Exception e) {
            logger.error("Failed to run demo: " + name, e);
            throw new RuntimeException("Demo failed: " + name, e);
        }
        
        System.out.println("=== " + name + " demonstration completed ===");
    }
}