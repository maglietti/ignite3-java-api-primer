package com.apache.ignite.examples.transactions;

import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main demonstration application for Ignite 3 Transaction API patterns.
 * 
 * This application runs all transaction examples in sequence to demonstrate
 * the full range of transaction capabilities in Ignite 3, following the 
 * narrative structure from the documentation:
 * 
 * 1. BasicTransactionDemo - Your first transaction patterns
 * 2. AsyncTransactionDemo - Non-blocking operations for high-throughput
 * 3. TransactionPatterns - Advanced real-world production scenarios
 * 
 * Each demo tells the story of ensuring data consistency in music store
 * operations, from simple artist creation to complex customer workflows.
 * 
 * Prerequisites:
 * - Ignite cluster running (see 00-docker module)
 * - Music store tables created (Artist, Album, Track, Customer, Invoice, InvoiceLine)
 * 
 * Usage:
 *   mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.transactions.TransactionAPIDemo"
 */
public class TransactionAPIDemo {
    private static final Logger logger = LoggerFactory.getLogger(TransactionAPIDemo.class);
    
    public static void main(String[] args) {
        logger.info("🎵 Starting Ignite 3 Transaction API Demo");
        logger.info("=========================================");
        logger.info("Demonstrating data consistency in music store operations");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800")
                .build()) {
            
            logger.info("🔗 Connected to Ignite cluster at localhost:10800");
            
            // Demo 1: Basic Transaction Patterns (following documentation narrative)
            logger.info("\n" + "=".repeat(70));
            logger.info("📚 DEMO 1: Basic Transaction Patterns");
            logger.info("Following the documentation narrative from 'Your First Transaction'");
            logger.info("=".repeat(70));
            
            try {
                BasicTransactionDemo.main(new String[0]);
                logger.info("✅ Basic Transaction Demo completed successfully");
            } catch (Exception e) {
                logger.error("❌ Basic Transaction Demo failed", e);
            }
            
            // Small delay between demos for better readability
            Thread.sleep(2000);
            
            // Demo 2: Async Transaction Patterns
            logger.info("\n" + "=".repeat(70));
            logger.info("🚀 DEMO 2: Async Transaction Patterns");
            logger.info("Non-blocking operations for high-throughput music store applications");
            logger.info("=".repeat(70));
            
            try {
                AsyncTransactionDemo.main(new String[0]);
                logger.info("✅ Async Transaction Demo completed successfully");
            } catch (Exception e) {
                logger.error("❌ Async Transaction Demo failed", e);
            }
            
            // Small delay between demos
            Thread.sleep(2000);
            
            // Demo 3: Advanced Transaction Patterns
            logger.info("\n" + "=".repeat(70));
            logger.info("🎯 DEMO 3: Advanced Transaction Patterns");
            logger.info("Production-ready patterns for complex business workflows");
            logger.info("=".repeat(70));
            
            try {
                TransactionPatterns.main(new String[0]);
                logger.info("✅ Advanced Transaction Patterns completed successfully");
            } catch (Exception e) {
                logger.error("❌ Advanced Transaction Patterns failed", e);
            }
            
            // Summary
            logger.info("\n" + "=".repeat(70));
            logger.info("🎉 TRANSACTION API DEMO COMPLETE");
            logger.info("=".repeat(70));
            logger.info("You've seen how transactions ensure data consistency in:");
            logger.info("  • Artist and Album creation (referential integrity)");
            logger.info("  • Customer purchase workflows (multi-table operations)");
            logger.info("  • Batch operations (performance optimization)");
            logger.info("  • Async patterns (high-throughput scenarios)");
            logger.info("  • Error handling (production reliability)");
            logger.info("");
            logger.info("Your music store application can now handle:");
            logger.info("  ✓ Customer purchases with perfect consistency");
            logger.info("  ✓ Inventory updates without data loss");
            logger.info("  ✓ Playlist modifications with rollback safety");
            logger.info("  ✓ High-throughput operations with async patterns");
            logger.info("");
            logger.info("Next: Explore the Compute API for distributed processing!");
            
        } catch (Exception e) {
            logger.error("❌ Transaction API Demo failed", e);
            System.exit(1);
        }
    }
}