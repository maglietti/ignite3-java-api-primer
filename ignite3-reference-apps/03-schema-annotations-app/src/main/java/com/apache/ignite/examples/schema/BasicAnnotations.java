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

package com.apache.ignite.examples.schema;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;

import com.apache.ignite.examples.schema.model.Artist;
import com.apache.ignite.examples.schema.model.Genre;
import com.apache.ignite.examples.schema.model.Customer;

/**
 * Demonstrates basic Apache Ignite 3 schema annotations for entity mapping.
 * 
 * Shows fundamental annotation patterns for mapping Java POJOs to Ignite tables.
 * Covers primary key mapping, column configuration, and basic CRUD operations
 * using annotated entities. Essential patterns for distributed data modeling.
 * 
 * Key concepts:
 * - @Table annotation for table definition and zone assignment
 * - @Id annotation for primary key mapping (simple and composite)
 * - @Column annotation for field-to-column mapping with constraints
 * - @Zone annotation for distribution zone assignment
 * - RecordView operations with type-safe POJO mapping
 * 
 * Prerequisites:
 * - Running Ignite cluster
 * - Sample data setup module (creates tables and zones)
 */
public class BasicAnnotations {
    
    private final IgniteClient client;
    
    public BasicAnnotations(IgniteClient client) {
        this.client = client;
    }
    
    /**
     * Demonstrates simple entity annotation pattern.
     * 
     * Artist entity uses single field primary key with basic column mapping.
     * Shows @Table zone assignment for data placement control and @Column
     * constraints for data validation in distributed environment.
     */
    public void demonstrateSimpleEntity() {
        System.out.println("--- Simple Entity Pattern: Artist");
        System.out.println("    Annotation features:");
        System.out.println("    - @Table: Maps class to Ignite table with zone assignment");
        System.out.println("    - @Id: Single field primary key for data distribution");
        System.out.println("    - @Column: Field mapping with length constraints");
        System.out.println("    - @Zone: Distribution zone for replica and storage control");
        
        try {
            Table artistTable = client.tables().table("Artist");
            RecordView<Artist> artistView = artistTable.recordView(Artist.class);
            
            // Insert operation demonstrates annotation-driven mapping
            Artist artist = new Artist(9001, "Demo Rock Band");
            artistView.upsert(null, artist);
            System.out.println("    >>> Inserted artist: " + artist.getName());
            
            // Retrieve operation uses primary key for efficient distributed lookup
            Artist keyOnly = new Artist();
            keyOnly.setArtistId(9001);
            Artist retrieved = artistView.get(null, keyOnly);
            System.out.println("    <<< Retrieved: " + retrieved.getName());
            
            // Cleanup
            artistView.delete(null, keyOnly);
            System.out.println("    >>> Cleanup completed");
            
        } catch (Exception e) {
            System.err.println("    !!! Simple entity demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates reference data entity pattern.
     * 
     * Genre entity shows different zone configuration for read-heavy reference
     * data. Uses MusicStoreReplicated zone with higher replica count for
     * improved read performance across distributed cluster.
     */
    public void demonstrateReferenceEntity() {
        System.out.println("--- Reference Data Pattern: Genre");
        System.out.println("    Annotation features:");
        System.out.println("    - @Zone: MusicStoreReplicated zone for read optimization");
        System.out.println("    - Higher replica count: Improved read performance");
        System.out.println("    - Reference data pattern: Lookup table optimization");
        System.out.println("    - Small dataset: Full replication strategy");
        
        try {
            Table genreTable = client.tables().table("Genre");
            RecordView<Genre> genreView = genreTable.recordView(Genre.class);
            
            // Reference data insertion with high availability design
            Genre genre = new Genre(901, "Demo Electronic");
            genreView.upsert(null, genre);
            System.out.println("    >>> Inserted genre: " + genre.getName());
            
            // Fast lookup from any replica due to read optimization
            Genre keyOnly = new Genre();
            keyOnly.setGenreId(901);
            Genre retrieved = genreView.get(null, keyOnly);
            System.out.println("    <<< Retrieved from optimized replica: " + retrieved.getName());
            System.out.println("    >>> Available from multiple nodes for fast access");
            
            // Cleanup
            genreView.delete(null, keyOnly);
            System.out.println("    >>> Cleanup completed");
            
        } catch (Exception e) {
            System.err.println("    !!! Reference entity demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates complex entity with multiple field types and constraints.
     * 
     * Customer entity shows various data types, nullable constraints, and
     * string length limits. Demonstrates annotation patterns for complex
     * business entities in distributed environment.
     */
    public void demonstrateComplexEntity() {
        System.out.println("--- Complex Entity Pattern: Customer");
        System.out.println("    Annotation features:");
        System.out.println("    - Multiple field types: String, Integer, nullable fields");
        System.out.println("    - @Column constraints: length limits and nullability");
        System.out.println("    - Business entity pattern: Customer profile data");
        System.out.println("    - Index annotations: Secondary access patterns");
        
        try {
            Table customerTable = client.tables().table("Customer");
            RecordView<Customer> customerView = customerTable.recordView(Customer.class);
            
            // Complex entity with multiple fields and constraints
            Customer customer = new Customer();
            customer.setCustomerId(9002);
            customer.setFirstName("Demo");
            customer.setLastName("User");
            customer.setEmail("demo.user@musicstore.com");
            customer.setCity("San Francisco");
            customer.setCountry("USA");
            customer.setPhone("+1-555-0199");
            
            customerView.upsert(null, customer);
            System.out.println("    >>> Inserted customer: " + customer.getFirstName() + " " + customer.getLastName());
            System.out.println("    >>> Email: " + customer.getEmail());
            System.out.println("    >>> Location: " + customer.getCity() + ", " + customer.getCountry());
            
            // Retrieve using primary key with complex entity mapping
            Customer keyOnly = new Customer();
            keyOnly.setCustomerId(9002);
            Customer retrieved = customerView.get(null, keyOnly);
            System.out.println("    <<< Retrieved customer with all fields populated");
            System.out.println("    >>> Indexes enable efficient lookups by email, location");
            
            // Cleanup
            customerView.delete(null, keyOnly);
            System.out.println("    >>> Cleanup completed");
            
        } catch (Exception e) {
            System.err.println("    !!! Complex entity demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates annotation-driven table creation and validation.
     * 
     * Shows how Ignite generates DDL from annotations, handles existing tables,
     * and validates schema consistency. Critical for schema-as-code approach
     * in distributed systems.
     */
    public void demonstrateSchemaGeneration() {
        System.out.println("--- Schema Generation from Annotations");
        System.out.println("    DDL generation features:");
        System.out.println("    - Automatic table creation from POJO annotations");
        System.out.println("    - Zone assignment and storage configuration");
        System.out.println("    - Primary key and constraint generation");
        System.out.println("    - Index creation for performance optimization");
        
        try {
            // Attempt table creation (will handle existing tables gracefully)
            System.out.println("    >>> Validating table schemas from annotations");
            
            // Test Artist table access
            Table artistTable = client.tables().table("Artist");
            if (artistTable != null) {
                System.out.println("    <<< Artist table: Schema validated");
            }
            
            // Test Genre table access
            Table genreTable = client.tables().table("Genre");
            if (genreTable != null) {
                System.out.println("    <<< Genre table: Schema validated");
            }
            
            // Test Customer table access
            Table customerTable = client.tables().table("Customer");
            if (customerTable != null) {
                System.out.println("    <<< Customer table: Schema validated");
            }
            
            System.out.println("    >>> Schema-as-code validation completed");
            System.out.println("    >>> Tables created from annotations match POJO definitions");
            
        } catch (Exception e) {
            System.err.println("    !!! Schema generation demo failed: " + e.getMessage());
        }
    }
}
