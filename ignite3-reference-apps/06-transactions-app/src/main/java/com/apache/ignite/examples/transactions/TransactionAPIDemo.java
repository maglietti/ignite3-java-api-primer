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

package com.apache.ignite.examples.transactions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transaction API Demo - Apache Ignite 3 transaction learning progression.
 * 
 * This demo orchestrates focused, educational examples that each demonstrate
 * one core transaction concept clearly. Each example is self-contained and 
 * can be run independently.
 * 
 * Learning Progression:
 * 1. BasicTransactions - Transaction lifecycle and CRUD operations
 * 2. TransactionIsolation - Isolation levels and concurrent behavior
 * 3. AsyncTransactions - Asynchronous transaction patterns
 * 4. BatchTransactions - Bulk operations and performance patterns
 * 
 * Each class focuses on demonstrating specific transaction capabilities
 * in a clear, educational manner with practical examples.
 */
public class TransactionAPIDemo {

    private static final Logger logger = LoggerFactory.getLogger(TransactionAPIDemo.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Apache Ignite 3 Transaction API Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Demonstrating ACID transaction patterns");
        System.out.println();
        
        try {
            // Run each demo in learning progression
            runDemo("Basic Transactions", 
                () -> BasicTransactions.main(new String[]{clusterAddress}));
                
            runDemo("Transaction Isolation", 
                () -> TransactionIsolation.main(new String[]{clusterAddress}));
                
            runDemo("Async Transactions", 
                () -> AsyncTransactions.main(new String[]{clusterAddress}));
                
            runDemo("Batch Transactions", 
                () -> BatchTransactions.main(new String[]{clusterAddress}));
                
            runDemo("Business Workflow Patterns", 
                () -> BusinessWorkflowPatterns.main(new String[]{clusterAddress}));
            
            System.out.println();
            System.out.println("=== Transaction API Demo completed successfully ===");
            
        } catch (Exception e) {
            logger.error("Demo execution failed", e);
            System.err.println("!!! Demo failed: " + e.getMessage());
            System.err.println("!!! Make sure Ignite cluster is running and sample data is loaded.");
        }
    }

    private static String getCurrentStep(String name) {
        switch (name) {
            case "Basic Transactions": return "1";
            case "Transaction Isolation": return "2";
            case "Async Transactions": return "3";
            case "Batch Transactions": return "4";
            case "Business Workflow Patterns": return "5";
            default: return "?";
        }
    }

    private static void runDemo(String name, Runnable demo) {
        System.out.println();
        System.out.println("=== [" + getCurrentStep(name) + "/5] " + name + " ===");
        
        try {
            demo.run();
        } catch (Exception e) {
            logger.error("Failed to run demo: " + name, e);
            throw new RuntimeException("Demo failed: " + name, e);
        }
        
        System.out.println("=== " + name + " demonstration completed ===");
    }
}