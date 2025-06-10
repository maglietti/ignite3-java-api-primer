# Apache Ignite 3 Java API: A Developer's Guide

This guide serves as a practical introduction to using the Apache Ignite 3 Java API. Each section covers key operations with concise explanations and working examples.

## 1. Connecting to an Ignite Cluster

The first step in any Ignite application is establishing a connection to the cluster.

```java
// Setup connection to Ignite cluster
public static IgniteClient connectToCluster() {
    try {
        // Define node addresses - use your actual cluster nodes
        String[] nodeAddresses = {
                "localhost:10800", "localhost:10801", "localhost:10802"
        };

        // Build the client and connect
        IgniteClient client = IgniteClient.builder()
                .addresses(nodeAddresses)
                .build();

        System.out.println(">>> Connected to the cluster: " + client.connections());
        return client;
    } catch (Exception e) {
        System.err.println("An error occurred: " + e.getMessage());
        return null;
    }
}
```

Always use try-with-resources to ensure proper connection cleanup:

```java
try (IgniteClient client = connectToCluster()) {
    if (client == null) {
        System.err.println("Failed to connect to the cluster. Exiting.");
        return;
    }
    
    // Your Ignite operations here
}
```

## 2. Defining Data Models with POJOs

Ignite 3 uses Java POJOs with annotations to define database tables. Here's how to create a custom POJO with appropriate creation methods:

```java
// Define a POJO with table mapping annotations
@Table(
    zone = @Zone(value = "test_zone", storageProfiles = "default")
)
public static class personPojo {
    // Primary key fields (composite key)
    @Id
    Integer id;

    @Id
    @Column(value = "id_str", length = 20)
    String idStr;

    // Regular fields with column mappings
    @Column("f_name")
    String firstName;

    @Column("l_name")
    String lastName;

    // Field without explicit column mapping uses field name as column name
    String str;

    // Default constructor
    public personPojo() {}
    
    // Static factory method for creating instances
    public static personPojo create(Integer id, String idStr, String firstName, String lastName, String str) {
        personPojo person = new personPojo();
        person.id = id;
        person.idStr = idStr;
        person.firstName = firstName;
        person.lastName = lastName;
        person.str = str;
        return person;
    }

    // Getters and setters
    public Integer getId() {return id;}
    public void setId(Integer id) {this.id = id;}
    public String getIdStr() {return idStr;}
    public void setIdStr(String idStr) {this.idStr = idStr;}
    public String getFirstName() {return firstName;}
    public void setFirstName(String firstName) {this.firstName = firstName;}
    public String getLastName() {return lastName;}
    public void setLastName(String lastName) {this.lastName = lastName;}
    public String getStr() {return str;}
    public void setStr(String str) {this.str = str;}
    
    @Override
    public String toString() {
        return "personPojo{" +
                "id=" + id +
                ", idStr='" + idStr + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", str='" + str + '\'' +
                '}';
    }
}
```

Key annotations include:

- `@Table`: Defines the table with distribution zone
- `@Id`: Marks fields as part of the primary key
- `@Column`: Maps fields to columns with optional properties
- `@Zone`: Specifies a distribution zone for data partitioning

## 3. Creating Distribution Zones

Distribution zones control data partitioning and replication in Ignite. They're a core concept for scalability.

```java
// Create a custom distribution zone
ZoneDefinition zoneTestZone = ZoneDefinition.builder("test_zone")
        .ifNotExists()            // Skip if zone already exists
        .partitions(2)            // Number of data partitions
        .storageProfiles("default")  // Storage mechanism
        .build();
client.catalog().createZone(zoneTestZone);
```

For zones with higher replication, use:

```java
// Create a zone with 3 replicas for higher availability
ZoneDefinition zoneReplicated = ZoneDefinition.builder("replicated_zone")
        .ifNotExists()
        .replicas(3)              // Keep 3 copies of data
        .partitions(25)           // Fewer partitions
        .storageProfiles("default")
        .build();
client.catalog().createZone(zoneReplicated);
```

## 4. Creating Tables

Once you have defined your POJO and created a zone, you can create a table:

```java
// Create table from POJO definition
client.catalog().createTable(personPojo.class);
```

This automatically creates a table with appropriate schema based on your POJO annotations.

To check if a table exists before attempting to create it:

```java
// Check if table exists
public static boolean tableExists(IgniteClient client, String tableName) {
    try {
        return client.tables().table(tableName) != null;
    } catch (Exception e) {
        return false;
    }
}

// Usage
if (!tableExists(client, "personPojo")) {
    client.catalog().createTable(personPojo.class);
    System.out.println("Table created");
} else {
    System.out.println("Table already exists");
}
```

## 5. Creating and Manipulating Data

### Inserting Data

For inserting single objects, use the static factory method:

```java
// Get a reference to the table
Table personTable = client.tables().table("personPojo");
// Create a record view for the POJO class
RecordView<personPojo> personView = personTable.recordView(personPojo.class);

// Create and insert a single object using the factory method
personPojo person = personPojo.create(1, "P001", "John", "Doe", "Software Engineer");
personView.upsert(null, person);
System.out.println("Inserted: " + person);
```

For batch inserts (more efficient for multiple records):

```java
// Create multiple objects
List<personPojo> people = new ArrayList<>();
people.add(personPojo.create(2, "P002", "Jane", "Smith", "Data Scientist"));
people.add(personPojo.create(3, "P003", "Michael", "Johnson", "Product Manager"));
people.add(personPojo.create(4, "P004", "Emily", "Williams", "UX Designer"));

// Insert all records in a single batch operation
personView.upsertAll(null, people);
System.out.println("Inserted " + people.size() + " records in batch");
```

### Retrieving Data

For retrieving a record by its primary key:

```java
// For composite keys, create an object with just the key fields
personPojo keyPerson = new personPojo();
keyPerson.setId(3);
keyPerson.setIdStr("P003");

// Retrieve the record
personPojo retrievedPerson = personView.get(null, keyPerson);
System.out.println("Retrieved: " + retrievedPerson);
```

### Updating Data

Updates use the same upsert method with modified objects:

```java
// First retrieve the record
personPojo person = personView.get(null, keyPerson);

// Modify the object
person.setFirstName("Mike");  // Change from "Michael"
person.setStr("Senior Product Manager");  // Update job title

// Save the changes
personView.upsert(null, person);
System.out.println("Updated: " + person);
```

### Deleting Data

To delete a record, you only need the primary key fields:

```java
// Create an object with just the primary key fields
personPojo personToDelete = new personPojo();
personToDelete.setId(4);
personToDelete.setIdStr("P004");

// Delete the record
personView.delete(null, personToDelete);
System.out.println("Deleted person with ID: 4, ID_STR: P004");
```

## 6. Working with Transactions

Transactions ensure multiple operations succeed or fail as a unit, maintaining data consistency.

```java
// Perform multiple operations in a single transaction
boolean success = client.transactions().runInTransaction(tx -> {
    try {
        // Get the record view
        Table personTable = client.tables().table("personPojo");
        RecordView<personPojo> personView = personTable.recordView(personPojo.class);
        
        // Perform multiple operations
        personPojo person1 = personPojo.create(20, "P020", "Alice", "Johnson", "Data Engineer");
        personView.upsert(tx, person1);
        
        personPojo person2 = personPojo.create(21, "P021", "Bob", "Johnson", "Data Analyst");
        personView.upsert(tx, person2);
        
        // More operations as needed...
        
        return true;  // Commit the transaction
    } catch (Exception e) {
        System.err.println("Transaction error: " + e.getMessage());
        return false;  // Roll back the transaction
    }
});

System.out.println("Transaction completed with status: " + success);
```

Notes:

- Pass the transaction object (`tx`) to all operations within the transaction
- Return `true` to commit or `false` to roll back
- Any uncaught exception will also roll back the transaction

## 7. Factory Pattern Support

To improve code organization and maintainability, Apache Ignite 3 applications can benefit from applying the factory pattern. For detailed implementations of factory classes such as:

- ZoneFactory
- TableFactory
- RecordViewFactory
- QueryFactory

See the companion document "[Apache Ignite 3 Factory Classes](factory-classes.md)" for complete implementations and usage examples.

## 8. SQL Operations

While Ignite supports POJO-based operations, you can also use SQL for more complex tasks.

```java
// Execute a basic SQL query
var result = client.sql().execute(null, 
    "SELECT * FROM personPojo WHERE id > ? ORDER BY id", 10);

while (result.hasNext()) {
    var row = result.next();
    System.out.println("ID: " + row.intValue("ID") + 
                      ", Name: " + row.stringValue("F_NAME") + " " + 
                                   row.stringValue("L_NAME"));
}
```

[The rest of the developer guide would continue here...]

### Converting SQL Results to POJOs

```java
// Get all records using SQL and convert to POJOs
List<personPojo> allPeople = new ArrayList<>();

client.sql().execute(null, "SELECT * FROM personPojo")
    .forEachRemaining(row -> {
        personPojo person = new personPojo();
        
        // Map SQL result to POJO fields
        person.setId(row.intValue("ID"));
        person.setIdStr(row.stringValue("ID_STR"));
        person.setFirstName(row.stringValue("F_NAME"));
        person.setLastName(row.stringValue("L_NAME"));
        person.setStr(row.stringValue("STR"));
        
        allPeople.add(person);
    });

// Now you can work with the list of POJOs
allPeople.forEach(System.out::println);
```

### Handling NULL Values in SQL Results

```java
// Safely handle potentially NULL values in SQL results
client.sql().execute(null, "SELECT * FROM personPojo WHERE id = ?", 1)
    .forEachRemaining(row -> {
        personPojo person = new personPojo();
        person.setId(row.intValue("ID"));
        person.setIdStr(row.stringValue("ID_STR"));
        
        // Handle potentially NULL values with try-catch
        try { 
            person.setFirstName(row.stringValue("F_NAME")); 
        } catch (Exception e) { 
            person.setFirstName(null); 
        }
        
        try { 
            person.setLastName(row.stringValue("L_NAME")); 
        } catch (Exception e) { 
            person.setLastName(null); 
        }
        
        try { 
            person.setStr(row.stringValue("STR")); 
        } catch (Exception e) { 
            person.setStr(null); 
        }
        
        System.out.println("Person with NULL handling: " + person);
    });
```

### SQL Insert, Update, and Delete

```java
// Insert data with SQL
client.sql().execute(null,
    "INSERT INTO personPojo (id, id_str, f_name, l_name, str) VALUES (?, ?, ?, ?, ?)",
    30, "P030", "William", "Clark", "Project Lead");

// Update data with SQL
client.sql().execute(null,
    "UPDATE personPojo SET str = ? WHERE id = ? AND id_str = ?",
    "Senior Project Lead", 30, "P030");

// Delete data with SQL
client.sql().execute(null,
    "DELETE FROM personPojo WHERE id = ? AND id_str = ?", 
    30, "P030");
```

### SQL with Transactions

```java
client.transactions().runInTransaction(tx -> {
    try {
        // Multiple SQL operations in a transaction
        client.sql().execute(tx,
            "INSERT INTO personPojo (id, id_str, f_name, l_name, str) VALUES (?, ?, ?, ?, ?)",
            40, "P040", "James", "Wilson", "Developer");
            
        client.sql().execute(tx,
            "INSERT INTO personPojo (id, id_str, f_name, l_name, str) VALUES (?, ?, ?, ?, ?)",
            41, "P041", "Olivia", "Wilson", "Designer");
        
        return true;  // Commit
    } catch (Exception e) {
        return false;  // Rollback
    }
});
```

## 9. Inspecting Table Structure

You can examine the table structure created from your POJO:

```java
// Inspect table structure
public static void displayTableColumns(IgniteClient client, String tableName) {
    try {
        var result = client.sql().execute(null, "SELECT * FROM " + tableName + " LIMIT 1");

        if (result.hasNext()) {
            var row = result.next();

            System.out.println("\nColumns in " + tableName + " table:");
            for (int i = 0; i < row.columnCount(); i++) {
                String columnName = row.columnName(i);
                Object value = row.value(i);

                System.out.println("  - Column: " + columnName +
                        ", Value: " + value);
            }
        } else {
            System.out.println("No rows found in " + tableName + " table");
        }
    } catch (Exception e) {
        System.err.println("Error inspecting table: " + e.getMessage());
    }
}
```

This utility helps you understand how Ignite maps POJO fields to database columns, which is especially useful when debugging case sensitivity issues.

## 10. Working with Relationships and Joins

While Ignite doesn't enforce foreign keys, you can model relationships between tables:

```java
// Create related tables
@Table(
    zone = @Zone(value = "test_zone", storageProfiles = "default")
)
public static class departmentPojo {
    @Id
    Integer id;
    
    @Column("dept_name")
    String deptName;
    
    // Getters and setters...
}

// Create department table
client.catalog().createTable(departmentPojo.class);

// Add a department field to the person table
client.sql().execute(null, "ALTER TABLE personPojo ADD COLUMN dept_id INT");

// Insert departments
Table deptTable = client.tables().table("departmentPojo");
RecordView<departmentPojo> deptView = deptTable.recordView(departmentPojo.class);

departmentPojo dept1 = new departmentPojo();
dept1.setId(1);
dept1.setDeptName("Engineering");
deptView.upsert(null, dept1);

departmentPojo dept2 = new departmentPojo();
dept2.setId(2);
dept2.setDeptName("Design");
deptView.upsert(null, dept2);

// Update people with department references
client.sql().execute(null, 
    "UPDATE personPojo SET dept_id = 1 WHERE str LIKE '%Engineer%'");
client.sql().execute(null, 
    "UPDATE personPojo SET dept_id = 2 WHERE str LIKE '%Design%'");

// Join the tables with SQL
var joinResult = client.sql().execute(null,
    "SELECT p.id, p.f_name, p.l_name, d.dept_name " +
    "FROM personPojo p " +
    "JOIN departmentPojo d ON p.dept_id = d.id " +
    "ORDER BY d.dept_name, p.f_name");

System.out.println("Employees by department:");
while (joinResult.hasNext()) {
    var row = joinResult.next();
    System.out.println(row.stringValue("F_NAME") + " " + 
                      row.stringValue("L_NAME") + " - " + 
                      row.stringValue("DEPT_NAME"));
}
```

## 11. Database Cleanup

Proper cleanup when you're done:

```java
// Drop tables when finished
if (tableExists(client, "personPojo")) {
    client.catalog().dropTable("personPojo");
    System.out.println("Dropped personPojo table");
}

if (tableExists(client, "departmentPojo")) {
    client.catalog().dropTable("departmentPojo");
    System.out.println("Dropped departmentPojo table");
}

// Drop zone
var zResult = client.sql().execute(null, 
    "SELECT name FROM system.zones WHERE name = ?", "TEST_ZONE");
if (zResult.hasNext()) {
    client.catalog().dropZone("TEST_ZONE");
    System.out.println("Dropped TEST_ZONE");
}
```

## 12. Co-location for Performance

For related tables that are frequently joined, co-location improves performance by storing related data on the same cluster node:

```java
// Define co-located tables
@Table(
    zone = @Zone(value = "test_zone", storageProfiles = "default")
)
public static class companyPojo {
    @Id
    Integer id;
    
    @Column("company_name")
    String companyName;
    
    // Getters and setters...
}

@Table(
    zone = @Zone(value = "test_zone", storageProfiles = "default"),
    colocateBy = @ColumnRef("company_id")  // Co-locate by company ID
)
public static class employeePojo {
    @Id
    Integer id;
    
    @Column("company_id")  // Foreign key to company
    Integer companyId;
    
    @Column("emp_name")
    String empName;
    
    // Getters and setters...
}
```

This ensures that employees for the same company are stored on the same node as the company record, minimizing network transfers during joins.

## 13. Indexes for Query Performance

Create indexes to improve query performance:

```java
// Define a table with indexes
@Table(
    zone = @Zone(value = "test_zone", storageProfiles = "default"),
    indexes = {
        @Index(value = "idx_person_name", columns = { @ColumnRef("f_name"), @ColumnRef("l_name") }),
        @Index(value = "idx_person_role", columns = { @ColumnRef("str") })
    }
)
public static class indexedPersonPojo {
    @Id
    Integer id;
    
    // Other fields...
}
```

You can also create indexes with SQL:

```java
// Create an index with SQL
client.sql().execute(null,
    "CREATE INDEX idx_person_dept ON personPojo(dept_id)");
```

## 14. Error Handling

Consistent error handling is essential for robust applications:

```java
try {
    // Attempt database operation
    personPojo person = personView.get(null, keyPerson);
    // Process the result
} catch (IgniteClientConnectionException e) {
    // Handle connection issues
    System.err.println("Connection error: " + e.getMessage());
    System.err.println("Affected endpoint: " + e.endpoint());
} catch (IgniteClientFeatureNotSupportedByServerException e) {
    // Handle feature compatibility issues
    System.err.println("Feature not supported: " + e.getMessage());
} catch (Exception e) {
    // Handle other errors
    System.err.println("Operation failed: " + e.getMessage());
}
```

## 15. Complete Workflow Example

Here's a complete workflow that demonstrates creating a schema, inserting data, and running queries:

```java
try (IgniteClient client = connectToCluster()) {
    if (client == null) {
        System.err.println("Failed to connect to the cluster. Exiting.");
        return;
    }
    
    // Create zone if it doesn't exist
    if (!zoneExists(client, "test_zone")) {
        ZoneDefinition zoneTestZone = ZoneDefinition.builder("test_zone")
                .partitions(2)
                .storageProfiles("default")
                .build();
        client.catalog().createZone(zoneTestZone);
        System.out.println("Created test_zone");
    }
    
    // Create table if it doesn't exist
    if (!tableExists(client, "personPojo")) {
        client.catalog().createTable(personPojo.class);
        System.out.println("Created personPojo table");
    }
    
    // Get table and create record view
    Table personTable = client.tables().table("personPojo");
    RecordView<personPojo> personView = personTable.recordView(personPojo.class);
    
    // Insert sample data in a transaction
    client.transactions().runInTransaction(tx -> {
        try {
            // Insert multiple records
            List<personPojo> people = new ArrayList<>();
            people.add(personPojo.create(1, "P001", "John", "Doe", "Developer"));
            people.add(personPojo.create(2, "P002", "Jane", "Smith", "Designer"));
            people.add(personPojo.create(3, "P003", "Bob", "Johnson", "Manager"));
            
            personView.upsertAll(tx, people);
            return true;  // Commit
        } catch (Exception e) {
            System.err.println("Error in transaction: " + e.getMessage());
            return false;  // Rollback
        }
    });
    
    // Run a query to verify data
    var result = client.sql().execute(null,
        "SELECT str as role, COUNT(*) as count " +
        "FROM personPojo " +
        "GROUP BY str " +
        "ORDER BY count DESC");
    
    System.out.println("Staff by role:");
    while (result.hasNext()) {
        var row = result.next();
        System.out.println("  " + row.stringValue("role") + ": " + 
                         row.longValue("count"));
    }
    
    // Cleanup (optional)
    System.out.println("Workflow complete. Dropping test table.");
    client.catalog().dropTable("personPojo");
} catch (Exception e) {
    System.err.println("Workflow failed: " + e.getMessage());
    e.printStackTrace();
}
```

## Helper Methods Collection

Here's a collection of useful helper methods for common operations:

```java
// Check if a zone exists
public static boolean zoneExists(IgniteClient client, String zoneName) {
    try {
        var result = client.sql().execute(null,
                "SELECT name FROM system.zones WHERE name = ?", zoneName.toUpperCase());
        return result.hasNext();
    } catch (Exception e) {
        System.err.println("Error checking if zone exists: " + e.getMessage());
        return false;
    }
}

// Check if a table exists
public static boolean tableExists(IgniteClient client, String tableName) {
    try {
        return client.tables().table(tableName) != null;
    } catch (Exception e) {
        return false;
    }
}

// Get count of records in a table
public static long getTableCount(IgniteClient client, String tableName) {
    try {
        var result = client.sql().execute(null, 
                "SELECT COUNT(*) as cnt FROM " + tableName);
        if (result.hasNext()) {
            return result.next().longValue("cnt");
        }
        return 0;
    } catch (Exception e) {
        System.err.println("Error getting count: " + e.getMessage());
        return -1;
    }
}

// Drop multiple tables in one operation
public static boolean dropTables(IgniteClient client, String... tableNames) {
    try {
        for (String tableName : tableNames) {
            if (tableExists(client, tableName)) {
                client.catalog().dropTable(tableName);
                System.out.println("Dropped table: " + tableName);
            }
        }
        return true;
    } catch (Exception e) {
        System.err.println("Error dropping tables: " + e.getMessage());
        return false;
    }
}
```

For more information, consult the [Apache Ignite 3 documentation](https://ignite.apache.org/docs/ignite3/latest/) and explore the examples in this project's codebase.
