package com.apache.ignite.examples.gettingstarted;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.catalog.annotations.*;
import org.apache.ignite.catalog.definitions.ZoneDefinition;
import org.apache.ignite.table.RecordView;

/**
 * Simple Hello World application showing the basics of Ignite 3.
 * 
 * Demonstrates the essential patterns every developer needs:
 * - Connecting to a cluster
 * - Creating distribution zones and tables  
 * - Storing and retrieving data
 * - Querying with SQL
 */
public class HelloWorldApp {
    
    @org.apache.ignite.catalog.annotations.Table(value = "SimpleBook", zone = @Zone(value = "QuickStart", storageProfiles = "default"))
    public static class Book {
        @Id
        private Integer id;
        
        @Column(length = 100)
        private String title;
        
        public Book() {}
        public Book(Integer id, String title) {
            this.id = id;
            this.title = title;
        }
        
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String toString() {
            return "Book{id=" + id + ", title='" + title + "'}";
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Hello World: Apache Ignite 3 ===");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800")
                .build()) {
            
            // 1. Create zone
            client.catalog().createZone(
                ZoneDefinition.builder("QuickStart")
                    .ifNotExists()
                    .replicas(2)
                    .storageProfiles("default")  // Required parameter!
                    .build()
            );
            System.out.println("Zone created: QuickStart");
            
            // 2. Create table
            client.catalog().dropTable("SimpleBook");
            client.catalog().createTable(Book.class);
            System.out.println("Table created: SimpleBook");
            
            // 3. Insert data
            RecordView<Book> books = client.tables()
                .table("SimpleBook")
                .recordView(Book.class);
            
            books.upsert(null, new Book(1, "1984"));
            books.upsert(null, new Book(2, "Brave New World"));
            System.out.println("Data inserted");
            
            // 4. Read data
            Book book = books.get(null, new Book(1, null));
            System.out.println("Found: " + book);
            
            // 5. Query with SQL
            var result = client.sql().execute(null, "SELECT id, title FROM SimpleBook ORDER BY id");
            System.out.println("All books:");
            while (result.hasNext()) {
                var row = result.next();
                System.out.println("  " + row.intValue("id") + ": " + row.stringValue("title"));
            }
            
            System.out.println("Success!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}