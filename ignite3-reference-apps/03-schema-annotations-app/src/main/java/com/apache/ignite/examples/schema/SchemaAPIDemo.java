package com.apache.ignite.examples.schema;

import org.apache.ignite.client.IgniteClient;

/**
 * Demonstrates Apache Ignite 3 schema annotations and entity mapping patterns.
 * 
 * Shows fundamental annotation patterns for mapping Java POJOs to distributed
 * Ignite tables. Covers basic annotations, colocation strategies, and schema
 * validation for building scalable music streaming platform data models.
 * 
 * Key concepts demonstrated:
 * - Basic entity annotation patterns (@Table, @Id, @Column, @Zone)
 * - Data colocation strategies for performance optimization
 * - Schema validation and DDL generation from annotations
 * - Progressive complexity from simple to advanced patterns
 * 
 * Prerequisites:
 * - Running Ignite cluster
 * - Sample data setup module (creates tables and zones)
 */
public class SchemaAPIDemo {
    
    private static final String CLUSTER_URL = "127.0.0.1:10800";
    
    public static void main(String[] args) {
        String clusterUrl = args.length > 0 ? args[0] : CLUSTER_URL;
        
        System.out.println("=== Apache Ignite 3 Schema Annotations API Demo ===");
        System.out.println("Target cluster: " + clusterUrl);
        System.out.println("Demonstrating entity mapping and colocation patterns");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(clusterUrl)
                .build()) {
            
            System.out.println("--- Connected to Ignite cluster at: " + clusterUrl);
            
            // Basic annotation patterns
            System.out.println();
            System.out.println("=== [1/3] Basic Annotation Patterns ===");
            BasicAnnotations basicDemo = new BasicAnnotations(client);
            basicDemo.demonstrateSimpleEntity();
            basicDemo.demonstrateReferenceEntity();
            basicDemo.demonstrateComplexEntity();
            basicDemo.demonstrateSchemaGeneration();
            System.out.println("=== Basic annotations demonstration completed ===");
            
            // Colocation patterns for performance
            System.out.println();
            System.out.println("=== [2/3] Colocation Patterns ===");
            ColocationPatterns colocationDemo = new ColocationPatterns(client);
            colocationDemo.demonstrateMusicColocation();
            colocationDemo.demonstrateSalesColocation();
            colocationDemo.demonstrateColocationBestPractices();
            System.out.println("=== Colocation patterns demonstration completed ===");
            
            // Schema validation and management
            System.out.println();
            System.out.println("=== [3/3] Schema Validation ===");
            SchemaValidation validationDemo = new SchemaValidation(client);
            validationDemo.demonstrateDDLGeneration();
            validationDemo.demonstrateSchemaValidation();
            validationDemo.demonstrateSchemaIntrospection();
            validationDemo.demonstrateValidationCleanup();
            System.out.println("=== Schema validation demonstration completed ===");
            
            System.out.println();
            System.out.println("=== Schema Annotations API Demo completed successfully ===");
            
        } catch (Exception e) {
            System.err.println("!!! Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}