package com.apache.ignite.examples.gettingstarted;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.catalog.annotations.*;
import org.apache.ignite.catalog.definitions.ZoneDefinition;
import org.apache.ignite.table.RecordView;

/**
 * Simple Hello World application showing the basics of Ignite 3.
 * 
 * Demonstrates the essential patterns every developer needs:
 * - Connecting to a cluster with all node addresses for optimal performance
 * - Understanding default zone vs custom zone creation
 * - Creating tables and storing/retrieving data
 * - Querying with SQL
 */
public class HelloWorldApp {
    
    // Example 1: Table using custom zone for production scenarios requiring fault tolerance
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
    
    // Example 2: Table using default zone for simple development scenarios
    @org.apache.ignite.catalog.annotations.Table(value = "SimpleNote")
    public static class Note {
        @Id
        private Integer id;
        
        @Column(length = 200)
        private String content;
        
        public Note() {}
        public Note(Integer id, String content) {
            this.id = id;
            this.content = content;
        }
        
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String toString() {
            return "Note{id=" + id + ", content='" + content + "'}";
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Hello World: Apache Ignite 3 ===");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800", "localhost:10801", "localhost:10802")
                .build()) {
            
            System.out.println(">>> Connected to all cluster nodes for optimal performance");
            
            // Example 1: Custom Zone for Production Scenarios
            System.out.println("\n=== Custom Zone Example (Production Pattern) ===");
            
            client.catalog().createZone(
                ZoneDefinition.builder("QuickStart")
                    .ifNotExists()
                    .replicas(2)  // Fault tolerance with 2 replicas
                    .partitions(25)
                    .storageProfiles("default")
                    .build()
            );
            System.out.println(">>> Custom zone 'QuickStart' created (2 replicas for fault tolerance)");
            
            client.catalog().dropTable("SimpleBook");
            client.catalog().createTable(Book.class);
            System.out.println(">>> Table 'SimpleBook' created in custom zone");
            
            RecordView<Book> books = client.tables()
                .table("SimpleBook")
                .recordView(Book.class);
            
            books.upsert(null, new Book(1, "1984"));
            books.upsert(null, new Book(2, "Brave New World"));
            System.out.println(">>> Books inserted into custom zone");
            
            Book book = books.get(null, new Book(1, null));
            System.out.println(">>> Retrieved from custom zone: " + book);
            
            // Example 2: Default Zone for Development Scenarios  
            System.out.println("\n=== Default Zone Example (Development Pattern) ===");
            
            client.catalog().dropTable("SimpleNote");
            client.catalog().createTable(Note.class);
            System.out.println(">>> Table 'SimpleNote' created in default zone (no zone creation needed)");
            
            RecordView<Note> notes = client.tables()
                .table("SimpleNote")
                .recordView(Note.class);
            
            notes.upsert(null, new Note(1, "Remember to use multiple addresses for production"));
            notes.upsert(null, new Note(2, "Default zone good for development and testing"));
            System.out.println(">>> Notes inserted into default zone");
            
            Note note = notes.get(null, new Note(1, null));
            System.out.println(">>> Retrieved from default zone: " + note);
            
            // Demonstrate SQL access across both zones
            System.out.println("\n=== SQL Query Across Zones ===");
            var bookResult = client.sql().execute(null, "SELECT COUNT(*) as record_count FROM SimpleBook");
            if (bookResult.hasNext()) {
                System.out.println(">>> Books in custom zone: " + bookResult.next().longValue("record_count"));
            }
            
            var noteResult = client.sql().execute(null, "SELECT COUNT(*) as record_count FROM SimpleNote");  
            if (noteResult.hasNext()) {
                System.out.println(">>> Notes in default zone: " + noteResult.next().longValue("record_count"));
            }
            
            System.out.println("\n=== Zone Best Practices Summary ===");
            System.out.println(">>> Default Zone: Use for development, testing, simple scenarios (1 replica)");
            System.out.println(">>> Custom Zones: Use for production workloads requiring fault tolerance (2+ replicas)");
            System.out.println(">>> Performance: Always specify all cluster addresses for partition awareness");
            
            System.out.println("\nSuccess!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}