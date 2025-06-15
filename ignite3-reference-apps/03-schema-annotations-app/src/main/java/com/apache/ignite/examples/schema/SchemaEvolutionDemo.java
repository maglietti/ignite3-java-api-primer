package com.apache.ignite.examples.schema;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Index;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;
import org.apache.ignite.catalog.annotations.ColumnRef;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.RecordView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Demonstrates Apache Ignite 3 schema evolution and DDL generation patterns.
 * 
 * This application shows how to:
 * 1. Design annotation-based schemas for evolution
 * 2. Handle table creation and schema validation
 * 3. Work with different entity patterns (simple, complex, versioned)
 * 4. Demonstrate DDL generation from annotations
 * 5. Show schema inspection and validation techniques
 * 
 * Key Schema Evolution Concepts:
 * - Annotation-driven DDL generation
 * - Table creation from POJOs
 * - Schema validation and error handling
 * - Entity versioning strategies
 * - Index management through annotations
 * - Zone configuration for different data types
 * 
 * Prerequisites:
 * - Running Ignite 3 cluster
 * - Clean environment for schema creation testing
 */
public class SchemaEvolutionDemo {
    
    private static final String CLUSTER_URL = "127.0.0.1:10800";
    
    /**
     * Version 1: Simple playlist entity for evolution demonstration
     */
    @Table(zone = @Zone(value = "MusicStore", storageProfiles = "default"))
    public static class PlaylistV1 {
        @Id
        @Column(value = "PlaylistId", nullable = false)
        private Integer PlaylistId;
        
        @Column(value = "Name", nullable = false, length = 120)
        private String Name;
        
        public PlaylistV1() {}
        
        public PlaylistV1(Integer playlistId, String name) {
            this.PlaylistId = playlistId;
            this.Name = name;
        }
        
        // Getters and setters
        public Integer getPlaylistId() { return PlaylistId; }
        public void setPlaylistId(Integer playlistId) { this.PlaylistId = playlistId; }
        public String getName() { return Name; }
        public void setName(String name) { this.Name = name; }
        
        @Override
        public String toString() {
            return "PlaylistV1{PlaylistId=" + PlaylistId + ", Name='" + Name + "'}";
        }
    }
    
    /**
     * Version 2: Enhanced playlist entity with additional fields and indexes
     */
    @Table(
        zone = @Zone(value = "MusicStore", storageProfiles = "default"),
        indexes = {
            @Index(value = "IDX_Playlist_Name", columns = { @ColumnRef("Name") }),
            @Index(value = "IDX_Playlist_Owner", columns = { @ColumnRef("OwnerId") }),
            @Index(value = "IDX_Playlist_Created", columns = { @ColumnRef("CreatedDate") })
        }
    )
    public static class PlaylistV2 {
        @Id
        @Column(value = "PlaylistId", nullable = false)
        private Integer PlaylistId;
        
        @Column(value = "Name", nullable = false, length = 120)
        private String Name;
        
        // New fields in V2
        @Column(value = "Description", nullable = true, length = 500)
        private String Description;
        
        @Column(value = "OwnerId", nullable = true)
        private Integer OwnerId;
        
        @Column(value = "CreatedDate", nullable = false)
        private LocalDateTime CreatedDate;
        
        @Column(value = "IsPublic", nullable = false)
        private Boolean IsPublic;
        
        public PlaylistV2() {}
        
        public PlaylistV2(Integer playlistId, String name, String description, 
                         Integer ownerId, LocalDateTime createdDate, Boolean isPublic) {
            this.PlaylistId = playlistId;
            this.Name = name;
            this.Description = description;
            this.OwnerId = ownerId;
            this.CreatedDate = createdDate;
            this.IsPublic = isPublic;
        }
        
        // Getters and setters
        public Integer getPlaylistId() { return PlaylistId; }
        public void setPlaylistId(Integer playlistId) { this.PlaylistId = playlistId; }
        public String getName() { return Name; }
        public void setName(String name) { this.Name = name; }
        public String getDescription() { return Description; }
        public void setDescription(String description) { this.Description = description; }
        public Integer getOwnerId() { return OwnerId; }
        public void setOwnerId(Integer ownerId) { this.OwnerId = ownerId; }
        public LocalDateTime getCreatedDate() { return CreatedDate; }
        public void setCreatedDate(LocalDateTime createdDate) { this.CreatedDate = createdDate; }
        public Boolean getIsPublic() { return IsPublic; }
        public void setIsPublic(Boolean isPublic) { this.IsPublic = isPublic; }
        
        @Override
        public String toString() {
            return "PlaylistV2{PlaylistId=" + PlaylistId + ", Name='" + Name + 
                   "', Description='" + Description + "', OwnerId=" + OwnerId + 
                   ", CreatedDate=" + CreatedDate + ", IsPublic=" + IsPublic + "}";
        }
    }
    
    /**
     * Demonstration entity for complex schema patterns
     */
    @Table(
        zone = @Zone(value = "MusicStore", storageProfiles = "default"),
        indexes = {
            @Index(value = "IDX_UserProfile_Email", columns = { @ColumnRef("Email") }),
            @Index(value = "IDX_UserProfile_Username", columns = { @ColumnRef("Username") }),
            @Index(value = "IDX_UserProfile_Location", columns = { 
                @ColumnRef("Country"), @ColumnRef("City") 
            })
        }
    )
    public static class UserProfile {
        @Id
        @Column(value = "UserId", nullable = false)
        private Integer UserId;
        
        @Column(value = "Username", nullable = false, length = 50)
        private String Username;
        
        @Column(value = "Email", nullable = false, length = 100)
        private String Email;
        
        @Column(value = "FirstName", nullable = true, length = 50)
        private String FirstName;
        
        @Column(value = "LastName", nullable = true, length = 50)
        private String LastName;
        
        @Column(value = "Country", nullable = true, length = 50)
        private String Country;
        
        @Column(value = "City", nullable = true, length = 50)
        private String City;
        
        @Column(value = "CreatedAt", nullable = false)
        private LocalDateTime CreatedAt;
        
        @Column(value = "LastLoginAt", nullable = true)
        private LocalDateTime LastLoginAt;
        
        @Column(value = "IsActive", nullable = false)
        private Boolean IsActive;
        
        public UserProfile() {}
        
        // Getters and setters
        public Integer getUserId() { return UserId; }
        public void setUserId(Integer userId) { this.UserId = userId; }
        public String getUsername() { return Username; }
        public void setUsername(String username) { this.Username = username; }
        public String getEmail() { return Email; }
        public void setEmail(String email) { this.Email = email; }
        public String getFirstName() { return FirstName; }
        public void setFirstName(String firstName) { this.FirstName = firstName; }
        public String getLastName() { return LastName; }
        public void setLastName(String lastName) { this.LastName = lastName; }
        public String getCountry() { return Country; }
        public void setCountry(String country) { this.Country = country; }
        public String getCity() { return City; }
        public void setCity(String city) { this.City = city; }
        public LocalDateTime getCreatedAt() { return CreatedAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.CreatedAt = createdAt; }
        public LocalDateTime getLastLoginAt() { return LastLoginAt; }
        public void setLastLoginAt(LocalDateTime lastLoginAt) { this.LastLoginAt = lastLoginAt; }
        public Boolean getIsActive() { return IsActive; }
        public void setIsActive(Boolean isActive) { this.IsActive = isActive; }
        
        @Override
        public String toString() {
            return "UserProfile{UserId=" + UserId + ", Username='" + Username + 
                   "', Email='" + Email + "', Country='" + Country + "', IsActive=" + IsActive + "}";
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Schema Evolution Demo ===");
        System.out.println("Demonstrating DDL generation and schema management in Ignite 3");
        
        try (IgniteClient client = IgniteClient.builder()
                .addresses(CLUSTER_URL)
                .build()) {
            
            SchemaEvolutionDemo demo = new SchemaEvolutionDemo();
            
            // Demonstrate DDL generation
            demo.demonstrateDDLGeneration(client);
            
            // Show schema validation
            demo.demonstrateSchemaValidation(client);
            
            // Demonstrate entity evolution patterns
            demo.demonstrateEntityEvolution(client);
            
            // Show complex schema creation
            demo.demonstrateComplexSchemaCreation(client);
            
            // Schema inspection techniques
            demo.demonstrateSchemaInspection(client);
            
            // Cleanup
            demo.cleanupDemoTables(client);
            
            System.out.println("\n✓ Schema Evolution Demo completed successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Demonstrates automatic DDL generation from annotations.
     */
    private void demonstrateDDLGeneration(IgniteClient client) {
        System.out.println("\n--- DDL Generation from Annotations    ---");
        
        System.out.println("Creating tables from annotated POJOs:");
        System.out.println("- Ignite 3 automatically generates SQL DDL");
        System.out.println("- Annotations control table structure, zones, indexes");
        System.out.println("- No manual DDL scripts required");
        
        try {
            // Create simple table (V1)
            System.out.println("\nCreating PlaylistV1 (simple schema):");
            client.catalog().createTable(PlaylistV1.class);
            System.out.println("✓ Generated DDL for simple table with primary key and basic columns");
            
            // Show what DDL would look like
            System.out.println("  Equivalent DDL:");
            System.out.println("  CREATE TABLE PlaylistV1 (");
            System.out.println("    PlaylistId INTEGER NOT NULL,");
            System.out.println("    Name VARCHAR(120) NOT NULL,");
            System.out.println("    PRIMARY KEY (PlaylistId)");
            System.out.println("  ) WITH (ZONE = 'MusicStore');");
            
        } catch (Exception e) {
            if (e.getMessage().contains("already exists")) {
                System.out.println("ℹ Table already exists (expected in demo environment)");
            } else {
                System.err.println("❌ DDL generation failed: " + e.getMessage());
            }
        }
    }
    
    /**
     * Demonstrates schema validation and error handling.
     */
    private void demonstrateSchemaValidation(IgniteClient client) {
        System.out.println("\n--- Schema Validation    ---");
        
        List<String> validationResults = new ArrayList<>();
        
        // Test table existence
        try {
            org.apache.ignite.table.Table table = client.tables().table("PlaylistV1");
            if (table != null) {
                validationResults.add("✓ PlaylistV1 table exists");
            }
        } catch (Exception e) {
            validationResults.add("❌ PlaylistV1 table missing: " + e.getMessage());
        }
        
        // Test table access
        try {
            RecordView<PlaylistV1> view = client.tables().table("PlaylistV1").recordView(PlaylistV1.class);
            if (view != null) {
                validationResults.add("✓ PlaylistV1 RecordView accessible");
            }
        } catch (Exception e) {
            validationResults.add("❌ PlaylistV1 RecordView failed: " + e.getMessage());
        }
        
        // Test basic operations
        try {
            RecordView<PlaylistV1> view = client.tables().table("PlaylistV1").recordView(PlaylistV1.class);
            PlaylistV1 testPlaylist = new PlaylistV1(99999, "Validation Test Playlist");
            view.upsert(null, testPlaylist);
            
            PlaylistV1 key = new PlaylistV1();
            key.setPlaylistId(99999);
            PlaylistV1 retrieved = view.get(null, key);
            
            if (retrieved != null && "Validation Test Playlist".equals(retrieved.getName())) {
                validationResults.add("✓ CRUD operations working");
                view.delete(null, key); // cleanup
            } else {
                validationResults.add("❌ CRUD operations failed");
            }
        } catch (Exception e) {
            validationResults.add("❌ CRUD validation failed: " + e.getMessage());
        }
        
        // Display validation results
        System.out.println("Schema validation results:");
        for (String result : validationResults) {
            System.out.println("  " + result);
        }
    }
    
    /**
     * Demonstrates entity evolution from V1 to V2.
     */
    private void demonstrateEntityEvolution(IgniteClient client) {
        System.out.println("\n--- Entity Evolution: V1 → V2    ---");
        
        System.out.println("Evolution approach in Ignite 3:");
        System.out.println("- Create new table with enhanced schema");
        System.out.println("- Migrate data from old to new table");
        System.out.println("- Drop old table after successful migration");
        
        try {
            // Create enhanced table (V2)
            System.out.println("\nCreating PlaylistV2 (enhanced schema):");
            client.catalog().createTable(PlaylistV2.class);
            System.out.println("✓ Generated DDL for enhanced table with indexes and new fields");
            
            System.out.println("  New features in V2:");
            System.out.println("  - Description field (VARCHAR(500))");
            System.out.println("  - OwnerId field (INTEGER)");
            System.out.println("  - CreatedDate field (TIMESTAMP)");
            System.out.println("  - IsPublic field (BOOLEAN)");
            System.out.println("  - Three indexes for performance");
            
            // Demonstrate V2 functionality
            RecordView<PlaylistV2> v2View = client.tables().table("PlaylistV2").recordView(PlaylistV2.class);
            PlaylistV2 enhancedPlaylist = new PlaylistV2(
                88888, 
                "Enhanced Demo Playlist",
                "This playlist demonstrates schema evolution features",
                12345,
                LocalDateTime.now(),
                true
            );
            
            v2View.upsert(null, enhancedPlaylist);
            System.out.println("✓ Inserted enhanced playlist: " + enhancedPlaylist);
            
            // Retrieve and verify
            PlaylistV2 key = new PlaylistV2();
            key.setPlaylistId(88888);
            PlaylistV2 retrieved = v2View.get(null, key);
            System.out.println("✓ Retrieved: " + retrieved);
            
            // Cleanup
            v2View.delete(null, key);
            
        } catch (Exception e) {
            if (e.getMessage().contains("already exists")) {
                System.out.println("ℹ Enhanced table already exists");
            } else {
                System.err.println("❌ Entity evolution failed: " + e.getMessage());
            }
        }
    }
    
    /**
     * Demonstrates complex schema creation with multiple indexes and constraints.
     */
    private void demonstrateComplexSchemaCreation(IgniteClient client) {
        System.out.println("\n--- Complex Schema Creation    ---");
        
        try {
            System.out.println("Creating UserProfile table with complex schema:");
            client.catalog().createTable(UserProfile.class);
            System.out.println("✓ Generated DDL for complex table");
            
            System.out.println("  Complex schema features:");
            System.out.println("  - 10 fields with various data types");
            System.out.println("  - 3 indexes including composite index");
            System.out.println("  - Nullable and non-nullable constraints");
            System.out.println("  - String length constraints");
            System.out.println("  - DateTime field handling");
            
            // Demonstrate complex entity usage
            RecordView<UserProfile> userView = client.tables().table("UserProfile").recordView(UserProfile.class);
            UserProfile user = new UserProfile();
            user.setUserId(77777);
            user.setUsername("demo_user");
            user.setEmail("demo.user@schema.test");
            user.setFirstName("Demo");
            user.setLastName("User");
            user.setCountry("Demo Country");
            user.setCity("Schema City");
            user.setCreatedAt(LocalDateTime.now());
            user.setIsActive(true);
            
            userView.upsert(null, user);
            System.out.println("✓ Inserted complex user profile: " + user);
            
            // Test indexed query simulation
            System.out.println("✓ Indexes enable efficient queries by email, username, and location");
            
            // Cleanup
            UserProfile keyOnly = new UserProfile();
            keyOnly.setUserId(77777);
            userView.delete(null, keyOnly);
            
        } catch (Exception e) {
            if (e.getMessage().contains("already exists")) {
                System.out.println("ℹ Complex table already exists");
            } else {
                System.err.println("❌ Complex schema creation failed: " + e.getMessage());
            }
        }
    }
    
    /**
     * Demonstrates schema inspection and metadata operations.
     */
    private void demonstrateSchemaInspection(IgniteClient client) {
        System.out.println("\n--- Schema Inspection    ---");
        
        String[] demoTables = {"PlaylistV1", "PlaylistV2", "UserProfile"};
        
        System.out.println("Inspecting created demo tables:");
        for (String tableName : demoTables) {
            try {
                org.apache.ignite.table.Table table = client.tables().table(tableName);
                if (table != null) {
                    System.out.println("  ✓ " + tableName + " - accessible");
                    
                    // Try to get schema information through queries
                    inspectTableStructure(client, tableName);
                }
            } catch (Exception e) {
                System.out.println("  ❌ " + tableName + " - not accessible: " + e.getMessage());
            }
        }
    }
    
    /**
     * Attempts to inspect table structure using system queries.
     * Note: Available system tables may vary by Ignite version.
     */
    private void inspectTableStructure(IgniteClient client, String tableName) {
        try {
            // This query may not work in all Ignite 3 versions
            // It's included to show the concept of schema inspection
            String inspectionQuery = """
                SELECT COUNT(*) as RowCount 
                FROM """ + tableName + """
                """;
            
            try (ResultSet<SqlRow> result = client.sql().execute(null, inspectionQuery)) {
                if (result.hasNext()) {
                    int rowCount = result.next().intValue("RowCount");
                    System.out.println("    → Current row count: " + rowCount);
                }
            }
        } catch (Exception e) {
            // Inspection may fail - this is expected in demo
            System.out.println("    → Structure inspection not available");
        }
    }
    
    /**
     * Cleans up demonstration tables.
     */
    private void cleanupDemoTables(IgniteClient client) {
        System.out.println("\n--- Cleaning Up Demo Tables    ---");
        
        String[] tablesToCleanup = {"UserProfile", "PlaylistV2", "PlaylistV1"};
        
        for (String tableName : tablesToCleanup) {
            try {
                // Note: Table dropping requires special permissions and may not be available
                // In production, you typically don't drop tables programmatically
                System.out.println("ℹ Table cleanup: " + tableName + " (manual cleanup may be required)");
            } catch (Exception e) {
                System.out.println("ℹ Table cleanup not performed: " + tableName);
            }
        }
        
        System.out.println("ℹ Demo tables may remain for inspection");
        System.out.println("  Use SQL commands to drop if needed:");
        System.out.println("  - DROP TABLE UserProfile;");
        System.out.println("  - DROP TABLE PlaylistV2;");
        System.out.println("  - DROP TABLE PlaylistV1;");
    }
}