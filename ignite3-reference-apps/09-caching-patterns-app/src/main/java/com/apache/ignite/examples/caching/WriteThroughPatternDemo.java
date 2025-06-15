package com.apache.ignite.examples.caching;

import org.apache.ignite.table.RecordView;
import org.apache.ignite.tx.IgniteTransactions;
import org.apache.ignite.tx.Transaction;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Demonstrates write-through pattern implementation using Ignite 3 transactions.
 * 
 * Music streaming services use write-through for customer data where:
 * - Data consistency is critical
 * - Updates must be immediately visible across systems
 * - Transaction guarantees are required
 * 
 * Write-through patterns ensure data consistency by updating both cache and
 * external data store in the same transaction. Customer profile updates require
 * this consistency to prevent data corruption across systems.
 * 
 * Key operations demonstrated:
 * - Transactional updates across cache and external store
 * - Atomic customer profile operations
 * - Batch processing with consistency guarantees
 * - Async transaction patterns
 * 
 * @see RecordView for entity-based cache operations
 * @see IgniteTransactions for transaction management
 * @see ExternalDataSource for primary data store simulation
 */
public class WriteThroughPatternDemo {
    
    private final RecordView<Customer> customerCache;
    private final IgniteTransactions transactions;
    private final ExternalDataSource externalDataSource;
    
    /**
     * Constructs write-through demo with required dependencies.
     * 
     * @param customerCache Ignite RecordView for customer caching
     * @param transactions Transaction manager for atomic operations
     * @param externalDataSource Primary data store for consistency
     */
    public WriteThroughPatternDemo(RecordView<Customer> customerCache,
                                  IgniteTransactions transactions,
                                  ExternalDataSource externalDataSource) {
        this.customerCache = customerCache;
        this.transactions = transactions;
        this.externalDataSource = externalDataSource;
    }
    
    /**
     * Updates customer information using write-through pattern.
     * 
     * Pattern implementation:
     * 1. Begin transaction
     * 2. Update cache (Ignite RecordView)
     * 3. Update external data store
     * 4. Commit transaction (both succeed or both fail)
     * 
     * This approach ensures that customer data remains consistent across
     * all systems, preventing scenarios where cache and external store
     * have different values.
     * 
     * @param customer Updated customer information
     * @throws RuntimeException if update fails in either system
     */
    public void updateCustomer(Customer customer) {
        System.out.printf("Updating customer %d using write-through pattern%n", customer.getCustomerId());
        
        transactions.runInTransaction(tx -> {
            try {
                System.out.printf("Starting transaction for customer %d update%n", customer.getCustomerId());
                
                // Step 1: Update cache within transaction
                customerCache.upsert(tx, customer);
                System.out.printf("Updated customer %d in cache%n", customer.getCustomerId());
                
                // Step 2: Update external data store
                boolean updated = externalDataSource.updateCustomer(customer);
                if (!updated) {
                    throw new RuntimeException("Failed to update customer in external store");
                }
                System.out.printf("Updated customer %d in external store%n", customer.getCustomerId());
                
                // Transaction automatically commits if no exceptions
                System.out.printf("Transaction committed for customer %d%n", customer.getCustomerId());
                
            } catch (Exception e) {
                System.err.printf("Transaction failed for customer %d: %s%n", 
                    customer.getCustomerId(), e.getMessage());
                // Transaction automatically rolls back on exception
                throw new RuntimeException("Failed to update customer: " + customer.getCustomerId(), e);
            }
        });
    }
    
    /**
     * Customer profile creation with write-through consistency.
     * 
     * Ensures new customer data is immediately available
     * in both cache and external systems.
     * 
     * This operation demonstrates atomic creation with existence checking
     * to prevent duplicate customer creation across systems.
     * 
     * @param customer New customer information
     * @return true if customer was created, false if already exists
     * @throws RuntimeException if creation fails
     */
    public boolean createCustomer(Customer customer) {
        System.out.printf("Creating customer %d using write-through pattern%n", customer.getCustomerId());
        
        try {
            return transactions.runInTransaction(tx -> {
                // Check if customer already exists in cache
                Customer keyRecord = Customer.builder()
                    .customerId(customer.getCustomerId())
                    .build();
                
                Customer existingCustomer = customerCache.get(tx, keyRecord);
                if (existingCustomer != null) {
                    System.out.printf("Customer %d already exists%n", customer.getCustomerId());
                    return false;
                }
                
                System.out.printf("Creating new customer %d%n", customer.getCustomerId());
                
                // Create in external data store first for validation
                boolean created = externalDataSource.createCustomer(customer);
                if (!created) {
                    throw new RuntimeException("Failed to create customer in external store");
                }
                System.out.printf("Created customer %d in external store%n", customer.getCustomerId());
                
                // Add to cache
                boolean inserted = customerCache.insert(tx, customer);
                if (!inserted) {
                    throw new RuntimeException("Failed to insert customer in cache");
                }
                System.out.printf("Created customer %d in cache%n", customer.getCustomerId());
                
                System.out.printf("Customer %d created successfully%n", customer.getCustomerId());
                return true;
            });
            
        } catch (Exception e) {
            System.err.printf("Failed to create customer %d: %s%n", 
                customer.getCustomerId(), e.getMessage());
            throw new RuntimeException("Failed to create customer: " + customer.getCustomerId(), e);
        }
    }
    
    /**
     * Batch customer updates for subscription changes.
     * 
     * Processes multiple customer updates while maintaining
     * consistency across all data stores.
     * 
     * This approach is particularly useful for:
     * - Bulk subscription tier changes
     * - Promotional offer applications
     * - Administrative account updates
     * 
     * @param customers List of customers to update
     * @throws RuntimeException if any update fails (all changes are rolled back)
     */
    public void updateSubscriptions(List<Customer> customers) {
        System.out.printf("Batch updating %d customers using write-through pattern%n", customers.size());
        
        transactions.runInTransaction(tx -> {
            try {
                System.out.printf("Starting batch transaction for %d customers%n", customers.size());
                
                for (Customer customer : customers) {
                    // Update cache
                    customerCache.upsert(tx, customer);
                    System.out.printf("Updated customer %d subscription in cache%n", customer.getCustomerId());
                    
                    // Update external data store
                    boolean updated = externalDataSource.updateCustomer(customer);
                    if (!updated) {
                        throw new RuntimeException("Failed to update customer " + customer.getCustomerId() + " in external store");
                    }
                    System.out.printf("Updated customer %d subscription in external store%n", customer.getCustomerId());
                }
                
                System.out.printf("Batch transaction committed for %d customers%n", customers.size());
                
            } catch (Exception e) {
                System.err.printf("Batch transaction failed: %s%n", e.getMessage());
                throw new RuntimeException("Failed to update customer subscriptions", e);
            }
        });
    }
    
    /**
     * Customer deletion with write-through consistency.
     * 
     * Removes customer data from both cache and external store
     * atomically to maintain data integrity.
     * 
     * @param customerId Customer ID to delete
     * @return true if customer was deleted, false if not found
     */
    public boolean deleteCustomer(int customerId) {
        System.out.printf("Deleting customer %d using write-through pattern%n", customerId);
        
        return transactions.runInTransaction(tx -> {
            Customer keyRecord = Customer.builder()
                .customerId(customerId)
                .build();
            
            // Check if customer exists
            Customer existingCustomer = customerCache.get(tx, keyRecord);
            if (existingCustomer == null) {
                System.out.printf("Customer %d not found for deletion%n", customerId);
                return false;
            }
            
            // Delete from external store first
            boolean deletedFromExternal = externalDataSource.deleteCustomer(customerId);
            if (!deletedFromExternal) {
                throw new RuntimeException("Failed to delete customer from external store");
            }
            System.out.printf("Deleted customer %d from external store%n", customerId);
            
            // Delete from cache
            boolean deletedFromCache = customerCache.delete(tx, keyRecord);
            if (!deletedFromCache) {
                throw new RuntimeException("Failed to delete customer from cache");
            }
            System.out.printf("Deleted customer %d from cache%n", customerId);
            
            System.out.printf("Customer %d deleted successfully%n", customerId);
            return true;
        });
    }
    
    /**
     * Asynchronous write-through for high-concurrency scenarios.
     * 
     * Maintains consistency while improving throughput
     * for concurrent customer updates.
     * 
     * This approach leverages Ignite's async transaction support
     * to handle multiple concurrent updates efficiently.
     * 
     * @param customer Customer information to update
     * @return CompletableFuture that completes when update is done
     */
    public CompletableFuture<Void> updateCustomerAsync(Customer customer) {
        System.out.printf("Async updating customer %d using write-through pattern%n", customer.getCustomerId());
        
        return transactions.runInTransactionAsync(tx -> {
            System.out.printf("Starting async transaction for customer %d%n", customer.getCustomerId());
            
            return customerCache.upsertAsync(tx, customer)
                .thenCompose(ignored -> {
                    System.out.printf("Async updated customer %d in cache%n", customer.getCustomerId());
                    return externalDataSource.updateCustomerAsync(customer);
                })
                .thenApply(updated -> {
                    if (!updated) {
                        throw new RuntimeException("Failed to update customer in external store");
                    }
                    System.out.printf("Async updated customer %d in external store%n", customer.getCustomerId());
                    return null;
                });
        }).thenAccept(ignored -> {
            System.out.printf("Async transaction committed for customer %d%n", customer.getCustomerId());
        });
    }
    
    /**
     * Read operations with write-through cache.
     * 
     * Reads from cache first, falling back to external source
     * while maintaining data consistency expectations.
     * 
     * In write-through patterns, cache reads are typically reliable
     * since all writes go through the cache. However, fallback
     * handling is still important for cache failures or data
     * that wasn't written through the cache.
     * 
     * @param customerId Customer ID to retrieve
     * @return Customer information or null if not found
     */
    public Customer getCustomer(int customerId) {
        System.out.printf("Retrieving customer %d from write-through cache%n", customerId);
        
        Customer keyRecord = Customer.builder()
            .customerId(customerId)
            .build();
        
        // Try cache first (should be reliable in write-through pattern)
        Customer customer = customerCache.get(null, keyRecord);
        if (customer != null) {
            System.out.printf("Retrieved customer %d from cache: %s%n", customerId, customer.getName());
            return customer;
        }
        
        System.out.printf("Customer %d not in cache, checking external source%n", customerId);
        
        // Fallback to external source (shouldn't happen often in write-through)
        customer = externalDataSource.loadCustomer(customerId);
        if (customer != null) {
            // Cache the result for future reads
            customerCache.upsert(null, customer);
            System.out.printf("Loaded and cached customer %d from external source%n", customerId);
        } else {
            System.out.printf("Customer %d not found in any system%n", customerId);
        }
        
        return customer;
    }
    
    /**
     * Customer payment method update with transaction isolation.
     * 
     * Demonstrates how write-through pattern handles sensitive data
     * updates that require strong consistency guarantees.
     * 
     * @param customerId Customer ID to update
     * @param paymentMethod New payment method information
     */
    public void updatePaymentMethod(int customerId, PaymentMethod paymentMethod) {
        System.out.printf("Updating payment method for customer %d%n", customerId);
        
        transactions.runInTransaction(tx -> {
            // Get current customer data
            Customer keyRecord = Customer.builder()
                .customerId(customerId)
                .build();
            
            Customer customer = customerCache.get(tx, keyRecord);
            if (customer == null) {
                throw new RuntimeException("Customer not found: " + customerId);
            }
            
            // Update payment method
            Customer updatedCustomer = customer.toBuilder()
                .paymentMethod(paymentMethod)
                .build();
            
            // Update cache
            customerCache.upsert(tx, updatedCustomer);
            System.out.printf("Updated payment method in cache for customer %d%n", customerId);
            
            // Update external store
            boolean updated = externalDataSource.updateCustomerPaymentMethod(customerId, paymentMethod);
            if (!updated) {
                throw new RuntimeException("Failed to update payment method in external store");
            }
            System.out.printf("Updated payment method in external store for customer %d%n", customerId);
        });
    }
    
    /**
     * Customer subscription upgrade with business logic validation.
     * 
     * Demonstrates write-through pattern with complex business rules
     * that require consistent validation across systems.
     * 
     * @param customerId Customer ID to upgrade
     * @param newSubscriptionTier New subscription tier
     * @return true if upgrade was successful
     */
    public boolean upgradeSubscription(int customerId, SubscriptionTier newSubscriptionTier) {
        System.out.printf("Upgrading subscription for customer %d to %s%n", customerId, newSubscriptionTier);
        
        return transactions.runInTransaction(tx -> {
            // Get current customer
            Customer keyRecord = Customer.builder()
                .customerId(customerId)
                .build();
            
            Customer customer = customerCache.get(tx, keyRecord);
            if (customer == null) {
                System.out.printf("Customer %d not found for upgrade%n", customerId);
                return false;
            }
            
            // Validate upgrade eligibility
            if (!customer.canUpgradeTo(newSubscriptionTier)) {
                System.out.printf("Customer %d not eligible for upgrade to %s%n", customerId, newSubscriptionTier);
                return false;
            }
            
            // Create updated customer
            Customer upgradedCustomer = customer.toBuilder()
                .subscriptionTier(newSubscriptionTier)
                .build();
            
            // Update external billing system first for validation
            boolean billingUpdated = externalDataSource.updateCustomerBilling(upgradedCustomer);
            if (!billingUpdated) {
                throw new RuntimeException("Failed to update billing system");
            }
            System.out.printf("Updated billing system for customer %d%n", customerId);
            
            // Update cache
            customerCache.upsert(tx, upgradedCustomer);
            System.out.printf("Updated subscription in cache for customer %d%n", customerId);
            
            System.out.printf("Successfully upgraded customer %d to %s%n", customerId, newSubscriptionTier);
            return true;
        });
    }
}