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
import org.apache.ignite.catalog.annotations.*;
import org.apache.ignite.catalog.definitions.ZoneDefinition;
import org.apache.ignite.table.RecordView;

/**
 * Basic setup showing tables with relationships.
 * 
 * Demonstrates:
 * - Two related tables (Author and Book)
 * - Simple transactions
 * - JOIN queries
 */
public class BasicSetupDemo {
    
    @org.apache.ignite.catalog.annotations.Table(zone = @Zone(value = "Demo", storageProfiles = "default"))
    public static class Author {
        @Id
        private Integer id;
        
        @Column(length = 50)
        private String name;
        
        public Author() {}
        public Author(Integer id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String toString() {
            return "Author{id=" + id + ", name='" + name + "'}";
        }
    }
    
    @org.apache.ignite.catalog.annotations.Table(zone = @Zone(value = "Demo", storageProfiles = "default"))
    public static class Book {
        @Id
        private Integer id;
        
        @Column(length = 100)
        private String title;
        
        @Column(value = "authorId")
        private Integer authorId;
        
        public Book() {}
        public Book(Integer id, String title, Integer authorId) {
            this.id = id;
            this.title = title;
            this.authorId = authorId;
        }
        
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Integer getAuthorId() { return authorId; }
        public void setAuthorId(Integer authorId) { this.authorId = authorId; }
        
        public String toString() {
            return "Book{id=" + id + ", title='" + title + "', authorId=" + authorId + "}";
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Basic Setup: Related Tables ===");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800", "localhost:10801", "localhost:10802")
                .build()) {
            
            // Create zone
            client.catalog().createZone(
                ZoneDefinition.builder("Demo")
                    .ifNotExists()
                    .replicas(2)
                    .storageProfiles("default")  // Required parameter!
                    .build()
            );
            
            // Create tables
            client.catalog().dropTable("Author"); //drop if exists
            client.catalog().createTable(Author.class);
            client.catalog().dropTable("Book"); 
            client.catalog().createTable(Book.class);
            System.out.println("Tables created");
            
            // Get table views
            RecordView<Author> authors = client.tables()
                .table("Author")
                .recordView(Author.class);
                
            RecordView<Book> books = client.tables()
                .table("Book")
                .recordView(Book.class);
            
            // Insert data in a transaction
            client.transactions().runInTransaction(tx -> {
                authors.upsert(tx, new Author(1, "George Orwell"));
                authors.upsert(tx, new Author(2, "Aldous Huxley"));
                
                books.upsert(tx, new Book(1, "1984", 1));
                books.upsert(tx, new Book(2, "Animal Farm", 1));
                books.upsert(tx, new Book(3, "Brave New World", 2));
                
                return true;
            });
            System.out.println("Data inserted with transaction");
            
            // Query with JOIN
            var result = client.sql().execute(null,
                "SELECT a.name, b.title " +
                "FROM Author a JOIN Book b ON a.id = b.authorId " +
                "ORDER BY a.name, b.title");
            
            System.out.println("Books by author:");
            while (result.hasNext()) {
                var row = result.next();
                System.out.println("  " + row.stringValue("name") + 
                                 " - " + row.stringValue("title"));
            }
            
            System.out.println("Success!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}