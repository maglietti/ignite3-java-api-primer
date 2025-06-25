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

package com.apache.ignite.examples.caching;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.tx.Transaction;
import org.apache.ignite.tx.IgniteTransactions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Write-Through Patterns - Consistency-critical customer data operations with Apache Ignite 3.
 * 
 * This class demonstrates write-through pattern for customer profile operations:
 * - Synchronous updates to both cache and external data store
 * - ACID transaction guarantees for data consistency
 * - Customer profile management workflows
 * - Subscription tier management with consistency requirements
 * - Batch operations within transactions
 * 
 * Learning Focus:
 * - When to use write-through (consistency-critical data, can tolerate write latency)
 * - Transaction management for multi-system consistency
 * - Error handling and rollback scenarios
 * - Performance considerations for synchronous operations
 */
public class WriteThroughPatterns {

    private static final Logger logger = LoggerFactory.getLogger(WriteThroughPatterns.class);
    
    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Write-Through Patterns Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            runWriteThroughPatterns(client);
            
        } catch (Exception e) {
            logger.error("Failed to run write-through patterns", e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void runWriteThroughPatterns(IgniteClient client) {
        // Get the Customer table for write-through operations
        Table customerTable = client.tables().table("Customer");
        if (customerTable == null) {
            throw new RuntimeException("Customer table not found. Please run sample data setup first.");
        }
        
        RecordView<Tuple> customerCache = customerTable.recordView();
        IgniteTransactions transactions = client.transactions();
        
        System.out.println("\n--- Write-Through Pattern Demonstrations ---");
        
        // Demonstrate core write-through patterns
        demonstrateCustomerCreation(transactions, customerCache);
        demonstrateCustomerUpdate(transactions, customerCache);
        demonstrateSubscriptionUpgrade(transactions, customerCache);
        demonstrateBatchOperations(transactions, customerCache);
        
        System.out.println("\n>>> Write-through patterns completed successfully");
    }

    /**
     * Demonstrates customer creation with write-through pattern.
     * 
     * Shows atomic customer creation:
     * 1. Begin transaction
     * 2. Create customer in cache (Ignite)
     * 3. Simulate write to external system
     * 4. Commit transaction for consistency
     */
    private static void demonstrateCustomerCreation(IgniteTransactions transactions, RecordView<Tuple> customerCache) {
        System.out.println("\n1. Customer Creation Pattern:");
        
        // Create new customer with write-through
        Tuple newCustomer = Tuple.create()
            .set("CustomerId", 2001)
            .set("FirstName", "John")
            .set("LastName", "Streaming")
            .set("Email", "john.streaming@example.com")
            .set("Country", "USA");
        
        System.out.println("   Creating customer with write-through pattern");
        
        Transaction tx = transactions.begin();
        try {
            // Phase 1: Write to cache (Ignite)
            customerCache.upsert(tx, newCustomer);
            System.out.println("   >>> Customer written to cache");
            
            // Phase 2: Simulate write to external system
            simulateExternalSystemWrite("CREATE_CUSTOMER", newCustomer);
            System.out.println("   >>> Customer written to external system");
            
            // Phase 3: Commit transaction
            tx.commit();
            System.out.println("   >>> Transaction committed - customer creation complete");
            
        } catch (Exception e) {
            tx.rollback();
            logger.error("Customer creation failed", e);
            System.err.println("   !!! Customer creation failed: " + e.getMessage());
            throw new RuntimeException("Customer creation failed", e);
        }
        
        // Verify customer was created
        Tuple key = Tuple.create().set("CustomerId", 2001);
        Tuple customer = customerCache.get(null, key);
        if (customer != null) {
            System.out.printf("   >>> Verified customer: %s %s%n", 
                customer.stringValue("FirstName"), customer.stringValue("LastName"));
        }
    }

    /**
     * Demonstrates customer profile update with write-through pattern.
     * 
     * Shows consistent updates across systems:
     * - Read current customer data
     * - Apply business logic changes
     * - Write to both cache and external system atomically
     */
    private static void demonstrateCustomerUpdate(IgniteTransactions transactions, RecordView<Tuple> customerCache) {
        System.out.println("\n2. Customer Update Pattern:");
        
        System.out.println("   Updating customer profile with write-through pattern");
        
        Transaction tx = transactions.begin();
        try {
            // Phase 1: Read current customer data
            Tuple key = Tuple.create().set("CustomerId", 2001);
            Tuple currentCustomer = customerCache.get(tx, key);
            
            if (currentCustomer == null) {
                throw new RuntimeException("Customer not found for update");
            }
            
            System.out.printf("   Current customer: %s %s%n", 
                currentCustomer.stringValue("FirstName"), currentCustomer.stringValue("LastName"));
            
            // Phase 2: Apply business logic changes
            Tuple updatedCustomer = currentCustomer
                .set("FirstName", "John Premium")
                .set("Email", "john.premium@example.com");
            
            // Phase 3: Write to cache
            customerCache.upsert(tx, updatedCustomer);
            System.out.println("   >>> Customer updated in cache");
            
            // Phase 4: Write to external system
            simulateExternalSystemWrite("UPDATE_CUSTOMER", updatedCustomer);
            System.out.println("   >>> Customer updated in external system");
            
            // Phase 5: Commit transaction
            tx.commit();
            System.out.println("   >>> Transaction committed - customer update complete");
            
        } catch (Exception e) {
            tx.rollback();
            logger.error("Customer update failed", e);
            System.err.println("   !!! Customer update failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrates subscription upgrade with business logic validation.
     * 
     * Shows complex business workflow with write-through:
     * - Validate current subscription tier
     * - Apply upgrade business rules
     * - Update customer record atomically
     */
    private static void demonstrateSubscriptionUpgrade(IgniteTransactions transactions, RecordView<Tuple> customerCache) {
        System.out.println("\n3. Subscription Upgrade Pattern:");
        
        System.out.println("   Processing subscription upgrade with write-through pattern");
        
        Transaction tx = transactions.begin();
        try {
            // Phase 1: Read and validate current customer
            Tuple key = Tuple.create().set("CustomerId", 2001);
            Tuple currentCustomer = customerCache.get(tx, key);
            
            if (currentCustomer == null) {
                throw new RuntimeException("Customer not found for subscription upgrade");
            }
            
            // Phase 2: Business logic validation (using Company field as subscription indicator)
            String currentTier = currentCustomer.stringValue("Company");
            if (currentTier == null) {
                currentTier = "BASIC";
            }
            System.out.printf("   Current subscription: %s%n", currentTier);
            
            // Phase 3: Apply upgrade
            String newTier = upgradeSubscriptionTier(currentTier);
            Tuple upgradedCustomer = currentCustomer
                .set("Company", newTier + " Subscription");
            
            System.out.printf("   Upgrading subscription: %s → %s%n", currentTier, newTier);
            
            // Phase 4: Write to cache
            customerCache.upsert(tx, upgradedCustomer);
            System.out.println("   >>> Subscription updated in cache");
            
            // Phase 5: Write to external billing system
            simulateExternalSystemWrite("UPGRADE_SUBSCRIPTION", upgradedCustomer);
            System.out.println("   >>> Subscription updated in billing system");
            
            // Phase 6: Commit transaction
            tx.commit();
            System.out.printf("   >>> Subscription upgrade complete: %s%n", newTier);
            
        } catch (Exception e) {
            tx.rollback();
            logger.error("Subscription upgrade failed", e);
            System.err.println("   !!! Subscription upgrade failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrates batch operations with write-through pattern.
     * 
     * Shows how to process multiple customer updates atomically:
     * - Process multiple customers in single transaction
     * - Maintain consistency across all updates
     * - Handle partial failures with rollback
     */
    private static void demonstrateBatchOperations(IgniteTransactions transactions, RecordView<Tuple> customerCache) {
        System.out.println("\n4. Batch Operations Pattern:");
        
        // Create test customers for batch operations
        List<Tuple> testCustomers = new ArrayList<>();
        for (int i = 2002; i <= 2005; i++) {
            testCustomers.add(Tuple.create()
                .set("CustomerId", i)
                .set("FirstName", "Batch")
                .set("LastName", "Customer " + i)
                .set("Email", "batch" + i + "@example.com")
                .set("Country", "USA")
                .set("Company", "BASIC Subscription"));
        }
        
        System.out.printf("   Batch creating %d customers with write-through pattern%n", testCustomers.size());
        
        Transaction tx = transactions.begin();
        try {
            int processedCount = 0;
            
            for (Tuple customer : testCustomers) {
                // Write to cache
                customerCache.upsert(tx, customer);
                
                // Write to external system
                simulateExternalSystemWrite("CREATE_CUSTOMER", customer);
                
                processedCount++;
                
                // Simulate potential error on last customer
                if (processedCount == testCustomers.size()) {
                    // This would normally complete successfully
                    System.out.printf("   Processed %d/%d customers%n", processedCount, testCustomers.size());
                }
            }
            
            // Commit all batch operations
            tx.commit();
            System.out.printf("   >>> Batch operation committed: %d customers created%n", processedCount);
            
        } catch (Exception e) {
            tx.rollback();
            logger.error("Batch operations failed", e);
            System.err.println("   !!! Batch operations failed and rolled back: " + e.getMessage());
        }
        
        // Verify batch creation
        System.out.println("   Verifying batch creation results");
        int verifiedCount = 0;
        for (int i = 2002; i <= 2005; i++) {
            Tuple key = Tuple.create().set("CustomerId", i);
            Tuple customer = customerCache.get(null, key);
            if (customer != null) {
                verifiedCount++;
            }
        }
        System.out.printf("   >>> Verified %d/%d customers created successfully%n", verifiedCount, testCustomers.size());
        
        // Cleanup test data
        cleanupTestCustomers(transactions, customerCache);
    }

    // Helper methods

    private static void simulateExternalSystemWrite(String operation, Tuple customer) {
        // Simulate external system latency and processing
        try {
            Thread.sleep(5); // Simulate network/processing delay
            
            // Simulate external system validation
            if (customer.intValue("CustomerId") < 0) {
                throw new RuntimeException("External system rejected invalid customer ID");
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("External system write interrupted", e);
        }
    }

    private static String upgradeSubscriptionTier(String currentTier) {
        // Business logic for subscription upgrades
        switch (currentTier.toUpperCase()) {
            case "BASIC":
                return "PREMIUM";
            case "PREMIUM":
                return "FAMILY";
            case "FAMILY":
                return "PREMIUM_PLUS";
            default:
                return "PREMIUM";
        }
    }

    private static void cleanupTestCustomers(IgniteTransactions transactions, RecordView<Tuple> customerCache) {
        System.out.println("\n5. Cleanup Test Data:");
        
        transactions.runInTransaction(tx -> {
            int deletedCount = 0;
            
            for (int id = 2001; id <= 2005; id++) {
                Tuple key = Tuple.create().set("CustomerId", id);
                boolean deleted = customerCache.delete(tx, key);
                if (deleted) {
                    deletedCount++;
                }
            }
            
            System.out.printf("   >>> Cleaned up %d test customers%n", deletedCount);
        });
    }
}