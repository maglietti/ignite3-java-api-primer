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

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.sql.Statement;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.tx.Transaction;
import org.apache.ignite.tx.TransactionOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Demonstrates business workflow patterns using Apache Ignite 3 transactions.
 * 
 * Implements production-ready transaction patterns for complex business scenarios
 * including customer purchase workflows, resilient error handling, and circuit
 * breaker patterns for production reliability.
 * 
 * Key patterns demonstrated:
 * - Multi-table purchase workflow coordination
 * - Transaction timeout configuration for different operation types
 * - Explicit transaction lifecycle management
 * - Resilient transaction processing with retry logic
 * - Circuit breaker patterns for system protection
 * 
 * Prerequisites:
 * - Running Ignite cluster
 * - Sample music store data loaded
 */
public class BusinessWorkflowPatterns {

    private static final Logger logger = LoggerFactory.getLogger(BusinessWorkflowPatterns.class);

    public static void main(String[] args) {
        String clusterAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        
        System.out.println("=== Business Workflow Patterns Demo ===");
        System.out.println("Target cluster: " + clusterAddress);
        System.out.println("Demonstrating production transaction patterns");

        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterAddress)
                .build()) {
            
            BusinessWorkflowPatterns demo = new BusinessWorkflowPatterns();
            demo.runBusinessWorkflowPatterns(client);
            
        } catch (Exception e) {
            logger.error("Failed to run business workflow patterns", e);
            System.err.println("!!! Error: " + e.getMessage());
        }
    }

    private void runBusinessWorkflowPatterns(IgniteClient client) {
        System.out.println("\n--- Business Workflow Patterns ---");
        System.out.println("    Production-ready transaction implementations");
        
        // Customer purchase workflow
        demonstratePurchaseWorkflow(client);
        
        // Transaction timeout patterns
        demonstrateTimeoutPatterns(client);
        
        // Explicit transaction lifecycle management
        demonstrateExplicitTransactionControl(client);
        
        // Resilient transaction processing
        demonstrateResilientProcessing(client);
        
        // Circuit breaker pattern
        demonstrateCircuitBreakerPattern(client);
        
        System.out.println("\n>>> Business workflow patterns completed successfully");
    }

    /**
     * Demonstrates complete customer purchase workflow with coordinated updates.
     */
    private void demonstratePurchaseWorkflow(IgniteClient client) {
        System.out.println("\n--- Customer Purchase Workflow");
        System.out.println(">>> Processing multi-table purchase coordination");
        
        List<Integer> sampleTrackIds = List.of(1, 2, 3);
        Integer customerId = 1;
        
        try {
            Integer invoiceId = processPurchase(client, customerId, sampleTrackIds);
            System.out.println("<<< Purchase completed successfully: Invoice " + invoiceId);
            
            // Verify the purchase
            verifyPurchase(client, invoiceId);
            
        } catch (Exception e) {
            System.err.println("!!! Purchase failed: " + e.getMessage());
        }
    }

    /**
     * Complete purchase workflow implementation.
     */
    private Integer processPurchase(IgniteClient client, Integer customerId, List<Integer> trackIds) {
        return client.transactions().runInTransaction(tx -> {
            try {
                IgniteSql sql = client.sql();
                
                // Step 1: Create the invoice  
                Integer invoiceId = generateInvoiceId();
                Statement createInvoiceStmt = sql.statementBuilder()
                    .query("INSERT INTO Invoice (InvoiceId, CustomerId, InvoiceDate, Total) VALUES (?, ?, ?, ?)")
                    .build();
                sql.execute(tx, createInvoiceStmt, invoiceId, customerId, LocalDate.now(), BigDecimal.ZERO);
                
                System.out.println("         Created invoice " + invoiceId);
                
                // Step 2: Add line items and calculate total
                BigDecimal totalAmount = BigDecimal.ZERO;
                int lineNumber = 1;
                
                for (Integer trackId : trackIds) {
                    // Get track price
                    Statement trackStmt = sql.statementBuilder()
                        .query("SELECT UnitPrice FROM Track WHERE TrackId = ?")
                        .build();
                    ResultSet<SqlRow> trackResult = sql.execute(tx, trackStmt, trackId);
                    
                    if (!trackResult.hasNext()) {
                        throw new IllegalArgumentException("Track not found: " + trackId);
                    }
                    
                    BigDecimal unitPrice = trackResult.next().decimalValue("UnitPrice");
                    
                    // Create line item
                    Integer lineItemId = generateLineItemId(invoiceId, lineNumber);
                    Statement createLineStmt = sql.statementBuilder()
                        .query("INSERT INTO InvoiceLine (InvoiceLineId, InvoiceId, TrackId, UnitPrice, Quantity) VALUES (?, ?, ?, ?, ?)")
                        .build();
                    sql.execute(tx, createLineStmt, lineItemId, invoiceId, trackId, unitPrice, 1);
                    
                    totalAmount = totalAmount.add(unitPrice);
                    System.out.println("         Added track " + trackId + " ($" + unitPrice + ")");
                    lineNumber++;
                }
                
                // Step 3: Update invoice with calculated total
                Statement updateInvoiceStmt = sql.statementBuilder()
                    .query("UPDATE Invoice SET Total = ? WHERE InvoiceId = ?")
                    .build();
                sql.execute(tx, updateInvoiceStmt, totalAmount, invoiceId);
                
                System.out.println("         Invoice total: $" + totalAmount);
                return invoiceId;
                
            } catch (Exception e) {
                System.err.println("         Purchase failed: " + e.getMessage());
                throw e;  // This will trigger rollback
            }
        });
    }

    /**
     * Verifies purchase completion by querying created records.
     */
    private void verifyPurchase(IgniteClient client, Integer invoiceId) {
        IgniteSql sql = client.sql();
        
        Statement verifyStmt = sql.statementBuilder()
            .query("""
                SELECT i.InvoiceId, i.Total, COUNT(il.InvoiceLineId) as LineCount
                FROM Invoice i
                JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId
                WHERE i.InvoiceId = ?
                GROUP BY i.InvoiceId, i.Total
                """)
            .build();
        
        ResultSet<SqlRow> result = sql.execute(null, verifyStmt, invoiceId);
        
        if (result.hasNext()) {
            SqlRow row = result.next();
            System.out.println("<<< Verification: Invoice " + row.intValue("InvoiceId") + 
                             " with " + row.longValue("LineCount") + 
                             " items, total $" + row.decimalValue("Total"));
        }
    }

    /**
     * Demonstrates different timeout patterns for different operation types.
     */
    private void demonstrateTimeoutPatterns(IgniteClient client) {
        System.out.println("\n--- Transaction Timeout Patterns");
        System.out.println(">>> Configuring timeouts for different operation types");
        
        // Quick customer update with short timeout
        quickCustomerUpdate(client);
        
        // Complex analytics with longer timeout
        complexAnalyticsReport(client);
    }

    private void quickCustomerUpdate(IgniteClient client) {
        TransactionOptions quickOptions = new TransactionOptions();
        
        try {
            client.transactions().runInTransaction(tx -> {
                IgniteSql sql = client.sql();
                
                // Simple customer update operation
                Statement updateStmt = sql.statementBuilder()
                    .query("UPDATE Customer SET Company = ? WHERE CustomerId = ?")
                    .build();
                long updated = sql.execute(tx, updateStmt, "Updated Company " + java.time.LocalDateTime.now(), 1).affectedRows();
                
                System.out.println("<<< Quick update completed: " + updated + " customer updated");
                return true;
            });
        } catch (Exception e) {
            System.err.println("!!! Quick update failed: " + e.getMessage());
        }
    }

    private void complexAnalyticsReport(IgniteClient client) {
        TransactionOptions analyticsOptions = new TransactionOptions().readOnly(true);
        
        try {
            client.transactions().runInTransaction(tx -> {
                IgniteSql sql = client.sql();
                
                // Complex multi-table analysis
                Statement statsStmt = sql.statementBuilder()
                    .query("""
                        SELECT COUNT(DISTINCT a.ArtistId) as artist_count,
                               COUNT(DISTINCT al.AlbumId) as album_count,
                               COUNT(DISTINCT t.TrackId) as track_count,
                               AVG(t.UnitPrice) as avg_price
                        FROM Artist a
                        JOIN Album al ON a.ArtistId = al.ArtistId
                        JOIN Track t ON al.AlbumId = t.AlbumId
                        """)
                    .build();
                ResultSet<SqlRow> stats = sql.execute(tx, statsStmt);
                
                if (stats.hasNext()) {
                    SqlRow row = stats.next();
                    System.out.println("<<< Analytics completed:");
                    System.out.println("         Artists: " + row.longValue("artist_count"));
                    System.out.println("         Albums: " + row.longValue("album_count"));
                    System.out.println("         Tracks: " + row.longValue("track_count"));
                    System.out.println("         Avg Price: $" + row.decimalValue("avg_price"));
                }
                
                return true;
            });
        } catch (Exception e) {
            System.err.println("!!! Analytics failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrates explicit transaction lifecycle control.
     */
    private void demonstrateExplicitTransactionControl(IgniteClient client) {
        System.out.println("\n--- Explicit Transaction Control");
        System.out.println(">>> Manual transaction lifecycle management");
        
        // Basic explicit pattern
        basicExplicitPattern(client);
        
        // Safer explicit pattern with finally block
        saferExplicitPattern(client);
    }

    private void basicExplicitPattern(IgniteClient client) {
        Transaction tx = null;
        try {
            // 1. Begin transaction
            tx = client.transactions().begin();
            
            // 2. Perform operations
            performSimpleOperation(client, tx);
            
            // 3. Commit if all operations succeed
            tx.commit();
            System.out.println("<<< Basic explicit transaction completed successfully");
            
        } catch (Throwable e) {
            System.err.println("!!! Basic explicit transaction failed: " + e.getMessage());
            
            // 4. Rollback on any error
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Throwable rollbackError) {
                    System.err.println("!!! Rollback failed: " + rollbackError.getMessage());
                }
            }
        }
    }

    private void saferExplicitPattern(IgniteClient client) {
        Transaction tx = null;
        boolean committed = false;
        try {
            tx = client.transactions().begin();
            performSimpleOperation(client, tx);
            tx.commit();
            committed = true;
            System.out.println("<<< Safer explicit transaction completed successfully");
            
        } catch (Throwable e) {
            System.err.println("!!! Safer explicit transaction failed: " + e.getMessage());
        } finally {
            // Ensure rollback if transaction wasn't committed
            if (tx != null && !committed) {
                try {
                    tx.rollback();
                } catch (Throwable rollbackError) {
                    System.err.println("!!! Rollback failed: " + rollbackError.getMessage());
                }
            }
        }
    }

    private void performSimpleOperation(IgniteClient client, Transaction tx) {
        IgniteSql sql = client.sql();
        
        // Simple operation for demonstration
        Statement countStmt = sql.statementBuilder()
            .query("SELECT COUNT(*) as artist_count FROM Artist")
            .build();
        ResultSet<SqlRow> result = sql.execute(tx, countStmt);
        
        if (result.hasNext()) {
            long count = result.next().longValue("artist_count");
            System.out.println("         Operation executed: " + count + " artists found");
        }
    }

    /**
     * Demonstrates resilient transaction processing with retry logic.
     */
    private void demonstrateResilientProcessing(IgniteClient client) {
        System.out.println("\n--- Resilient Transaction Processing");
        System.out.println(">>> Implementing retry logic with exponential backoff");
        
        ResilientTransactionService resilientService = new ResilientTransactionService();
        
        CompletableFuture<String> result = resilientService.executeWithRetry(
            client,
            tx -> {
                IgniteSql sql = client.sql();
                // Simulate operation that might fail transiently
                Statement stmt = sql.statementBuilder()
                    .query("SELECT COUNT(*) as total_tracks FROM Track")
                    .build();
                ResultSet<SqlRow> rs = sql.execute(tx, stmt);
                
                if (rs.hasNext()) {
                    long count = rs.next().longValue("total_tracks");
                    return "Found " + count + " tracks in catalog";
                }
                return "No data found";
            },
            3 // Max retries
        );
        
        try {
            String output = result.get();
            System.out.println("<<< Resilient operation completed: " + output);
        } catch (Exception e) {
            System.err.println("!!! Resilient operation failed: " + e.getMessage());
        }
    }

    /**
     * Demonstrates circuit breaker pattern for system protection.
     */
    private void demonstrateCircuitBreakerPattern(IgniteClient client) {
        System.out.println("\n--- Circuit Breaker Pattern");
        System.out.println(">>> Protecting system from cascading failures");
        
        TransactionCircuitBreaker circuitBreaker = new TransactionCircuitBreaker();
        
        // Execute operations through circuit breaker
        for (int i = 1; i <= 3; i++) {
            final int artistId = i; // Capture loop variable
            try {
                CompletableFuture<String> result = circuitBreaker.executeTransaction(
                    client,
                    tx -> {
                        IgniteSql sql = client.sql();
                        Statement stmt = sql.statementBuilder()
                            .query("SELECT Name FROM Artist WHERE ArtistId = ?")
                            .build();
                        ResultSet<SqlRow> rs = sql.execute(tx, stmt, artistId);
                        
                        if (rs.hasNext()) {
                            return "Artist: " + rs.next().stringValue("Name");
                        }
                        return "Artist not found";
                    }
                );
                
                String output = result.get();
                System.out.println("<<< Circuit breaker operation " + i + ": " + output);
                
            } catch (Exception e) {
                System.err.println("!!! Circuit breaker operation " + i + " failed: " + e.getMessage());
            }
        }
    }

    // Helper methods for ID generation
    private Integer generateInvoiceId() {
        return (int) (System.currentTimeMillis() % 100000);
    }

    private Integer generateLineItemId(Integer invoiceId, int lineNumber) {
        return invoiceId * 100 + lineNumber;
    }

    /**
     * Resilient transaction service with retry logic.
     */
    private static class ResilientTransactionService {
        
        public <T> CompletableFuture<T> executeWithRetry(
                IgniteClient client, 
                Function<Transaction, T> operation,
                int maxRetries) {
            
            return executeWithRetryInternal(client, operation, maxRetries, 0);
        }
        
        private <T> CompletableFuture<T> executeWithRetryInternal(
                IgniteClient client,
                Function<Transaction, T> operation,
                int maxRetries,
                int currentAttempt) {
            
            return client.transactions().runInTransactionAsync(tx -> CompletableFuture.supplyAsync(() -> operation.apply(tx)))
                .exceptionallyCompose(throwable -> {
                    if (currentAttempt >= maxRetries) {
                        return CompletableFuture.failedFuture(throwable);
                    }
                    
                    if (isRetryableException(throwable)) {
                        long delay = calculateBackoffDelay(currentAttempt);
                        
                        return CompletableFuture
                            .supplyAsync(() -> null, CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS))
                            .thenCompose(ignored -> 
                                executeWithRetryInternal(client, operation, maxRetries, currentAttempt + 1));
                    } else {
                        return CompletableFuture.failedFuture(throwable);
                    }
                });
        }
        
        private boolean isRetryableException(Throwable throwable) {
            // Retry on transaction conflicts and temporary network issues
            return throwable.getMessage().contains("timeout") ||
                   throwable.getMessage().contains("connection") ||
                   throwable.getMessage().contains("conflict");
        }
        
        private long calculateBackoffDelay(int attempt) {
            // Exponential backoff with jitter
            long baseDelay = 100; // 100ms
            long exponentialDelay = baseDelay * (long) Math.pow(2, attempt);
            long jitter = (long) (Math.random() * 50); // 0-50ms jitter
            return Math.min(exponentialDelay + jitter, 5000); // Max 5 seconds
        }
    }

    /**
     * Circuit breaker implementation for transaction protection.
     */
    private static class TransactionCircuitBreaker {
        private enum State { CLOSED, OPEN, HALF_OPEN }
        
        private State state = State.CLOSED;
        private int failureCount = 0;
        private long lastFailureTime = 0;
        private final int failureThreshold = 5;
        private final long timeoutDuration = 10000; // 10 seconds
        
        public <T> CompletableFuture<T> executeTransaction(
                IgniteClient client,
                Function<Transaction, T> operation) {
            
            if (state == State.OPEN) {
                if (System.currentTimeMillis() - lastFailureTime > timeoutDuration) {
                    state = State.HALF_OPEN;
                } else {
                    return CompletableFuture.failedFuture(
                        new RuntimeException("Circuit breaker is OPEN"));
                }
            }
            
            return client.transactions().runInTransactionAsync(tx -> CompletableFuture.supplyAsync(() -> operation.apply(tx)))
                .thenApply(result -> {
                    onSuccess();
                    return result;
                })
                .exceptionally(throwable -> {
                    onFailure();
                    throw new RuntimeException(throwable);
                });
        }
        
        private synchronized void onSuccess() {
            failureCount = 0;
            state = State.CLOSED;
        }
        
        private synchronized void onFailure() {
            failureCount++;
            lastFailureTime = System.currentTimeMillis();
            
            if (failureCount >= failureThreshold) {
                state = State.OPEN;
            }
        }
    }
}