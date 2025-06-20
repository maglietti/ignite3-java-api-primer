package com.apache.ignite.examples.gettingstarted;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.catalog.annotations.*;
import org.apache.ignite.table.RecordView;

/**
 * Hello World application showing the basics of Ignite 3.
 * 
 * Demonstrates the essential patterns from Chapter 1.2:
 * - Connecting to a cluster with all node addresses for optimal performance
 * - Using the default zone for development scenarios
 * - Creating tables and performing CRUD operations
 * - Querying with SQL using the same data model
 */
public class HelloWorldApp {
    
    // Uses default zone - perfect for learning and development scenarios
    @org.apache.ignite.catalog.annotations.Table(value = "Book")
    public static class Book {
        @Id
        private Integer id;
        
        @Column(length = 100)
        private String title;
        
        @Column(length = 50)
        private String author;
        
        public Book() {}
        public Book(Integer id, String title, String author) {
            this.id = id;
            this.title = title;
            this.author = author;
        }
        
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        
        public String toString() {
            return "Book{id=" + id + ", title='" + title + "', author='" + author + "'}";
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Hello World: Apache Ignite 3 ===");
        
        // Implement multi-node connection strategy from Chapter 1.1
        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800", "localhost:10801", "localhost:10802")
                .build()) {
            
            System.out.println(">>> Connected with partition awareness");
            
            // 1. Create table (uses default zone automatically)
            client.catalog().dropTable("Book"); // Clean slate for repeated runs
            client.catalog().createTable(Book.class);
            System.out.println(">>> Table created in default zone");
            
            // 2. Get table view using Table API
            RecordView<Book> books = client.tables()
                .table("Book")
                .recordView(Book.class);
            
            // 3. Insert data using type-safe operations
            books.upsert(null, new Book(1, "1984", "George Orwell"));
            books.upsert(null, new Book(2, "Brave New World", "Aldous Huxley"));
            System.out.println(">>> Books inserted using Table API");
            
            // 4. Read data using type-safe operations
            Book book = books.get(null, new Book(1, null, null));
            System.out.println(">>> Retrieved: " + book);
            
            // 5. Query with SQL API - same data, different access pattern
            var result = client.sql().execute(null, "SELECT id, title, author FROM Book ORDER BY id");
            System.out.println(">>> All books via SQL:");
            while (result.hasNext()) {
                var row = result.next();
                System.out.println("    " + row.intValue("id") + ": " + 
                                 row.stringValue("title") + " by " + 
                                 row.stringValue("author"));
            }
            
            System.out.println(">>> Success! Default zone pattern working perfectly.");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}