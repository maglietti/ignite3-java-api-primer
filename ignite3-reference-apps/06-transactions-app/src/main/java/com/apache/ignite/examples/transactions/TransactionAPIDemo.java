package com.apache.ignite.examples.transactions;

import com.apache.ignite.client.IgniteClient;
import com.apache.ignite.examples.setup.util.DataSetupUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main demonstration application for Ignite 3 Transaction API patterns.
 * 
 * This application runs all transaction examples in sequence to demonstrate
 * the full range of transaction capabilities in Ignite 3:
 * 
 * 1. BasicTransactionDemo - Fundamental transaction patterns
 * 2. AsyncTransactionDemo - Asynchronous transaction patterns  
 * 3. TransactionPatterns - Advanced real-world scenarios
 * 
 * Each demo is self-contained and demonstrates specific aspects of the
 * Transaction API using music store sample data.
 * 
 * Prerequisites:
 * - Ignite cluster running (see 00-docker module)
 * - Sample data setup completed (see 01-sample-data-setup module)
 * 
 * Usage:
 *   mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.transactions.TransactionAPIDemo"
 */
public class TransactionAPIDemo {
    private static final Logger logger = LoggerFactory.getLogger(TransactionAPIDemo.class);
    
    public static void main(String[] args) {
        logger.info("Starting Ignite 3 Transaction API Demo");
        logger.info("=====================================");
        
        try (IgniteClient client = DataSetupUtils.getClient()) {
            // Verify connection
            logger.info("Connected to Ignite cluster");
            
            // Run basic transaction demonstrations
            logger.info("\n🔄 Running Basic Transaction Demonstrations...");
            BasicTransactionDemo.main(new String[0]);
            
            // Small delay between demos
            Thread.sleep(2000);
            
            // Run async transaction demonstrations
            logger.info("\n⚡ Running Async Transaction Demonstrations...");
            AsyncTransactionDemo.main(new String[0]);
            
            // Small delay between demos
            Thread.sleep(2000);
            
            // Run advanced transaction pattern demonstrations
            logger.info("\n🎯 Running Advanced Transaction Pattern Demonstrations...");
            TransactionPatterns.main(new String[0]);
            
            logger.info("\n✅ All Transaction API demonstrations completed successfully!");
            logger.info("=====================================");
            
        } catch (Exception e) {
            logger.error("❌ Transaction API Demo failed", e);
            System.exit(1);
        }
    }
}