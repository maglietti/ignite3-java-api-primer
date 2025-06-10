# Bulk Loading the Chinook Database in Ignite 3

This document explains the bulk loading process for the Chinook database in Apache Ignite 3, detailing how the SQL file is imported and processed.

## Overview

The Chinook database is a sample music store database that includes tables for artists, albums, tracks, playlists, customers, employees, and sales information. The bulk loading process allows you to quickly populate the entire database schema and data from a SQL file.

## SQL File Structure and Example

The `chinook-ignite3.sql` file contains SQL statements to:

1. Create the necessary distribution zones
2. Create all tables with appropriate schema definitions
3. Populate these tables with sample data

The file follows a specific format:

- SQL statements are delimited by semicolons
- Table creation follows a dependency order (parent tables before child tables)
- Data insertion follows the same dependency order

Here's an example of what the SQL file contains:

```sql
-- Create the distribution zones
CREATE ZONE IF NOT EXISTS Chinook WITH STORAGE_PROFILES='default', REPLICAS=2;
CREATE ZONE IF NOT EXISTS ChinookReplicated WITH STORAGE_PROFILES='default', REPLICAS=3, PARTITIONS=25;

-- Create the Artist table
CREATE TABLE Artist (
    ArtistId INT PRIMARY KEY,
    Name VARCHAR
) ZONE Chinook STORAGE PROFILE 'default';

-- Create the Album table with co-location
CREATE TABLE Album (
    AlbumId INT,
    Title VARCHAR NOT NULL,
    ArtistId INT,
    PRIMARY KEY (AlbumId, ArtistId)
) ZONE Chinook STORAGE PROFILE 'default' COLOCATE BY (ArtistId);

-- Insert sample data
INSERT INTO Artist (ArtistId, Name) VALUES 
    (1, 'AC/DC'),
    (2, 'Accept'),
    (3, 'Aerosmith');

INSERT INTO Album (AlbumId, Title, ArtistId) VALUES 
    (1, 'For Those About To Rock We Salute You', 1),
    (2, 'Balls to the Wall', 2),
    (3, 'Restless and Wild', 2),
    (4, 'Let There Be Rock', 1);
```

## Placement of the SQL File

The SQL file should be placed in the `src/main/resources` directory of the project. This is a standard location for non-Java resources in Maven projects and allows the file to be loaded from the classpath at runtime.

```shell
src/main/resources/chinook-ignite3.sql
```

When you run the `BulkLoadApp`, it scans the resources directory for SQL files and presents them for selection.

## Running the BulkLoadApp

When you run the `BulkLoadApp`, you'll see the following user interface:

1. First, the app connects to the cluster:

   ```
   >>> Connected to the cluster: [localhost:10800]
   ```

2. If existing tables are detected, the app prompts you to drop them:

   ```
   Existing tables detected in the database.
   Do you want to drop existing tables before loading new data? (Y/N)
   ```

3. If you choose to drop tables, it may also ask about dropping zones:

   ```
   Do you also want to drop distribution zones? (Y/N)
   ```

4. The app then lists available SQL files:

   ```
   Available SQL files:
   1. chinook-ignite3.sql
   Select a file to load (1-1): 1
   ```

5. After selecting a file, it parses the SQL statements and asks for confirmation:

   ```
   Selected file: chinook-ignite3.sql
   Parsed 127 SQL statements from file.
   This will create tables and load data from the SQL file.
   Do you want to proceed? (Y/N)
   ```

6. Finally, it executes the SQL statements and verifies the results:

   ```
   === Starting bulk load from SQL file ===
   === Processing distribution zones, table definitions, and indexes ===
   [1/127] Executing: CREATE ZONE CREATE ZONE Chinook WITH STORAGE_PROFILES='defaul...
     Success!
   ...
   === Bulk load completed ===
   Successfully executed 127 out of 127 statements.

   === Verifying Chinook data ===
   Artists: 275
   Albums: 347
   Tracks: 3503
   ...
   Chinook database has been loaded successfully!
   ```

## The Bulk Loading Process

### 1. File Parsing

The `SqlImportUtils.parseSqlStatementsFromReader()` method reads the SQL file and parses it into individual SQL statements:

```java
List<String> sqlStatements = SqlImportUtils.parseSqlStatementsFromReader(reader);
```

This method:

- Reads the file line by line
- Handles statement delimiters (semicolons)
- Filters out comments and empty lines
- Produces a list of clean SQL statements ready for execution

Here's how the parsing process works in `SqlImportUtils.java`:

```java
public static List<String> parseSqlStatementsFromReader(BufferedReader reader) throws IOException {
    List<String> statements = new ArrayList<>();
    StringBuilder currentStatement = new StringBuilder();
    String line;
    boolean insideMultilineComment = false;
    boolean insideQuote = false;
    
    while ((line = reader.readLine()) != null) {
        // Handle multiline comments
        if (line.contains("/*") && !line.contains("*/")) {
            insideMultilineComment = true;
            continue;
        }
        
        if (insideMultilineComment) {
            if (line.contains("*/")) {
                insideMultilineComment = false;
            }
            continue;
        }
        
        // Skip empty lines and single-line comments
        line = line.trim();
        if (line.isEmpty() || line.startsWith("--")) {
            continue;
        }
        
        // Process the line
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            // Handle quotes to avoid detecting statement delimiters inside quoted strings
            if (c == '\'' && (i == 0 || line.charAt(i-1) != '\\')) {
                insideQuote = !insideQuote;
            }
            
            // If we find a delimiter outside of quotes, split the statement
            if (c == ';' && !insideQuote) {
                String statement = currentStatement.toString().trim();
                if (!statement.isEmpty() && !shouldIgnoreStatement(statement)) {
                    statements.add(statement);
                }
                currentStatement = new StringBuilder();
            } else {
                currentStatement.append(c);
            }
        }
        
        // Add a space between lines for readability
        if (currentStatement.length() > 0) {
            currentStatement.append(' ');
        }
    }
    
    // Add final statement if present
    String finalStatement = currentStatement.toString().trim();
    if (!finalStatement.isEmpty() && !shouldIgnoreStatement(finalStatement)) {
        statements.add(finalStatement);
    }
    
    return statements;
}
```

### 2. Zone and Schema Creation

The bulk loader first processes zone and table creation statements:

```java
// First, process zones and DDL statements
for (String statement : statements) {
    // Skip non-zone/table statements in first pass
    if (!isSchemaStatement(statement)) {
        continue;
    }
    
    // Execute the statement
    client.sql().execute(null, statement);
}
```

This ensures that:

- Distribution zones are created first
- Tables are created in the correct dependency order
- The database schema is fully established before data is loaded

### 3. Data Loading

After the schema is created, the bulk loader processes data insertion statements:

```java
// Then, process all DML statements
for (String statement : statements) {
    // Skip schema statements in second pass
    if (isSchemaStatement(statement)) {
        continue;
    }
    
    // Execute the statement
    client.sql().execute(null, statement);
}
```

This approach:

- Ensures data is inserted after all tables are created
- Handles potential foreign key constraints correctly
- Provides a clean separation between schema creation and data loading

### 4. Handling Large Inserts

For very large INSERT statements, the `BulkLoadApp` splits them into smaller batches:

```java
// For INSERT statements, check if batch splitting is needed
if (statementType.equals("INSERT")) {
    int approxRows = countInsertRows(statement);
    
    if (approxRows > MAX_BATCH_SIZE) {
        System.out.println("  Splitting large INSERT into smaller batches...");
        List<String> batches = splitLargeInsert(statement, MAX_BATCH_SIZE);
        System.out.println("  Created " + batches.size() + " batches");
        
        // Execute each batch
        int batchNum = 1;
        for (String batch : batches) {
            System.out.println("  Executing batch " + batchNum + "/" + batches.size());
            client.sql().execute(null, batch);
            batchNum++;
        }
        
        successCount++;
        System.out.println("  All batches executed successfully!");
        continue;
    }
}
```

The `splitLargeInsert` method divides a large INSERT statement into smaller chunks:

```java
public static List<String> splitLargeInsert(String statement, int batchSize) {
    List<String> batches = new ArrayList<>();
    
    // First, ensure this is an INSERT statement
    if (!statement.toUpperCase().trim().startsWith("INSERT")) {
        batches.add(statement);
        return batches;
    }
    
    // Find the VALUES keyword
    int valuesIndex = statement.toUpperCase().indexOf("VALUES");
    if (valuesIndex == -1) {
        batches.add(statement);
        return batches;
    }
    
    // Split into prefix (INSERT INTO table (columns)) and values part
    String prefix = statement.substring(0, valuesIndex + 6); // Include "VALUES"
    String valuesPart = statement.substring(valuesIndex + 6).trim();
    
    // Parse the value groups
    List<String> valueGroups = new ArrayList<>();
    StringBuilder currentGroup = new StringBuilder();
    int parenthesesLevel = 0;
    boolean inQuote = false;
    
    for (int i = 0; i < valuesPart.length(); i++) {
        char c = valuesPart.charAt(i);
        
        // Track quotes to avoid splitting inside quoted strings
        if (c == '\'' && (i == 0 || valuesPart.charAt(i-1) != '\\')) {
            inQuote = !inQuote;
        }
        
        // Track parentheses level
        if (c == '(' && !inQuote) parenthesesLevel++;
        if (c == ')' && !inQuote) parenthesesLevel--;
        
        // Add the character to the current group
        currentGroup.append(c);
        
        // If we've closed a top-level value group
        if (parenthesesLevel == 0 && currentGroup.length() > 0 && c == ')') {
            // Remove trailing commas
            String group = currentGroup.toString().trim();
            if (group.endsWith(",")) {
                group = group.substring(0, group.length() - 1).trim();
            }
            
            valueGroups.add(group);
            currentGroup = new StringBuilder();
            
            // Skip past any commas
            while (i + 1 < valuesPart.length() && valuesPart.charAt(i + 1) == ',') {
                i++;
            }
        }
    }
    
    // Create batched INSERT statements
    for (int i = 0; i < valueGroups.size(); i += batchSize) {
        StringBuilder batchStatement = new StringBuilder(prefix);
        for (int j = i; j < Math.min(i + batchSize, valueGroups.size()); j++) {
            if (j > i) batchStatement.append(", ");
            batchStatement.append(valueGroups.get(j));
        }
        batches.add(batchStatement.toString());
    }
    
    return batches;
}
```

### 5. Error Handling

The bulk loader implements robust error handling:

- Zone-related errors are handled gracefully (zones may already exist)
- Table creation errors are reported but don't halt the process
- Data insertion errors are logged but allow the process to continue

This ensures that the bulk load process can recover from minor issues and still complete successfully.

```java
try {
    // Execute the statement
    client.sql().execute(null, statement);
    successCount++;
    System.out.println("  Success!");
} catch (Exception e) {
    // Handle exceptions differently based on statement type
    if (isCreateZoneStatement(statement)) {
        System.out.println("  Note: Zone may already exist, continuing: " + e.getMessage());
    } else if (isDropStatement(statement)) {
        System.out.println("  Note: Drop operation failed, may be due to dependencies or non-existent object: " + e.getMessage());
    } else if (isCreateIndexStatement(statement)) {
        System.out.println("  Note: Index creation failed, may already exist: " + e.getMessage());
    } else {
        System.err.println("  Error executing statement: " + e.getMessage());
    }
}
```

### 6. Verification

After loading is complete, the process verifies that data was loaded correctly:

```java
SqlImportUtils.verifyChinookData(client);
```

This method checks record counts in major tables to confirm successful data loading.

```java
public static void verifyChinookData(IgniteClient client) {
    System.out.println("\n=== Verifying Chinook data ===");
    
    try {
        // Check Artist count
        var result = client.sql().execute(null, "SELECT COUNT(*) as cnt FROM Artist");
        if (result.hasNext()) {
            long count = result.next().longValue("cnt");
            System.out.println("Artists: " + count);
        }
        
        // Check Album count
        result = client.sql().execute(null, "SELECT COUNT(*) as cnt FROM Album");
        if (result.hasNext()) {
            long count = result.next().longValue("cnt");
            System.out.println("Albums: " + count);
        }
        
        // Check Track count
        result = client.sql().execute(null, "SELECT COUNT(*) as cnt FROM Track");
        if (result.hasNext()) {
            long count = result.next().longValue("cnt");
            System.out.println("Tracks: " + count);
        }
        
        // Check additional tables if they were created
        checkTableCount(client, "Customer");
        checkTableCount(client, "Employee");
        checkTableCount(client, "Invoice");
        checkTableCount(client, "InvoiceLine");
        checkTableCount(client, "Playlist");
        checkTableCount(client, "PlaylistTrack");
        
    } catch (Exception e) {
        System.err.println("Error verifying data: " + e.getMessage());
    }
}
```

## Implementation Details in BulkLoadApp.java

The `BulkLoadApp` class contains the main entry point for the bulk loading process. Here's how it works:

```java
public static void main(String[] args) {
    // Control logging
    java.util.logging.LogManager.getLogManager().reset();
    org.apache.logging.log4j.jul.LogManager.getLogManager();
    java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.WARNING);

    // Create a single scanner for all user input
    Scanner scanner = new Scanner(System.in);

    // Connect to the Ignite cluster
    try (IgniteClient client = ChinookUtils.connectToCluster()) {
        if (client == null) {
            System.err.println("Failed to connect to the cluster. Exiting.");
            return;
        }

        System.out.println(">>> Connected to the cluster: " + client.connections());

        // Check if tables already exist and ask to drop them
        if (TableUtils.tableExists(client, "Artist") || 
            TableUtils.tableExists(client, "Album") || 
            TableUtils.tableExists(client, "Track")) {
            
            System.out.println("Existing tables detected in the database.");
            System.out.println("Do you want to drop existing tables before loading new data? (Y/N)");
            String dropTablesInput = scanner.nextLine().trim().toUpperCase();
            
            if (dropTablesInput.equals("Y")) {
                // Drop all existing tables
                boolean tablesDropped = TableUtils.dropTables(client);
                // ... additional code ...
            }
        }

        // Check if required distribution zones exist, create if not
        if (!TableUtils.zoneExists(client, "Chinook")) {
            System.out.println("Required distribution zones not found. Creating zones...");
            boolean zonesCreated = TableUtils.createDistributionZones(client);
            // ... additional code ...
        }

        // Find SQL files in resources
        List<String> sqlFiles = findSqlFilesInResources();
        
        // Display available SQL files
        System.out.println("Available SQL files:");
        for (int i = 0; i < sqlFiles.size(); i++) {
            System.out.println((i + 1) + ". " + sqlFiles.get(i));
        }
        
        // Get user selection
        System.out.print("Select a file to load (1-" + sqlFiles.size() + "): ");
        int fileIndex = Integer.parseInt(scanner.nextLine().trim()) - 1;
        
        String selectedFile = sqlFiles.get(fileIndex);
        System.out.println("Selected file: " + selectedFile);

        // Read SQL file from resources
        InputStream sqlFileStream = BulkLoadApp.class.getResourceAsStream("/" + selectedFile);
        
        // Parse SQL statements from file
        List<String> sqlStatements;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(sqlFileStream, StandardCharsets.UTF_8))) {
            sqlStatements = SqlImportUtils.parseSqlStatementsFromReader(reader);
        }
        
        System.out.println("Parsed " + sqlStatements.size() + " SQL statements from file.");

        // Prompt user to confirm load
        System.out.println("This will create tables and load data from the SQL file.");
        System.out.println("Do you want to proceed? (Y/N)");
        String input = scanner.nextLine().trim().toUpperCase();

        if (!input.equals("Y")) {
            System.out.println("Operation cancelled by user.");
            return;
        }

        // Execute the SQL statements
        System.out.println("\n=== Starting bulk load from SQL file ===");
        int successCount = SqlImportUtils.executeSqlStatements(client, sqlStatements);
        
        System.out.println("\n=== Bulk load completed ===");
        System.out.println("Successfully executed " + successCount + " out of " + sqlStatements.size() + " statements.");
        
        // Verify the data was loaded by counting some records
        SqlImportUtils.verifyChinookData(client);

        System.out.println("\nChinook database has been loaded successfully!");
    } catch (Exception e) {
        System.err.println("Error during bulk load: " + e.getMessage());
        e.printStackTrace();
    } finally {
        // Always close the scanner
        scanner.close();
    }
}
```

## Entity Model Integration

The bulk loader works alongside the existing entity model classes. When using the SQL file method, both approaches can coexist:

1. **POJO-based schema creation** using `TableUtils.createTables()`
2. **SQL-based schema creation** using statements from the SQL file

The bulk loader primarily uses the SQL approach but also includes support for POJO-based operations if needed.

### Structure Consistency Between Approaches

The SQL statements in the bulk load file create tables with the same structure as the POJO definitions:

POJO definition:

```java
@Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default"),
        colocateBy = @ColumnRef("ArtistId"),
        indexes = {
            @Index(value = "IFK_AlbumArtistId", columns = { @ColumnRef("ArtistId") })
        }
)
public class Album {
    @Id
    @Column(value = "AlbumId", nullable = false)
    private Integer albumId;

    @Column(value = "Title", nullable = false)
    private String title;

    @Id
    @Column(value = "ArtistId", nullable = false)
    private Integer artistId;
    
    // Methods omitted for brevity
}
```

Equivalent SQL definition in the bulk load file:

```sql
CREATE TABLE Album (
    AlbumId INT,
    Title VARCHAR NOT NULL,
    ArtistId INT,
    PRIMARY KEY (AlbumId, ArtistId)
) ZONE Chinook STORAGE PROFILE 'default' COLOCATE BY (ArtistId);
```

This ensures that both approaches result in the same database schema.

## Co-location in Bulk Loading

The SQL file maintains the same co-location strategy as the POJO model:

- `Album` is co-located with `Artist` by `ArtistId`
- `Track` is co-located with `Album` by `AlbumId`
- `Invoice` is co-located with `Customer` by `CustomerId`
- `InvoiceLine` is co-located with `Invoice` by `InvoiceId`
- `PlaylistTrack` is co-located with `Playlist` by `PlaylistId`

This ensures that the performance benefits of co-location are preserved when using the bulk loading approach.

## Creating Custom SQL Files for Bulk Loading

You can create your own SQL files for bulk loading by following these steps:

1. Create a file with `.sql` extension in the `src/main/resources` directory
2. Structure your SQL file with the following sections:

   ```sql
   -- Create distribution zones
   CREATE ZONE IF NOT EXISTS ... WITH ...;
   
   -- Create tables
   CREATE TABLE ... ZONE ... STORAGE PROFILE ...;
   
   -- Create indexes (if needed)
   CREATE INDEX ... ON ...;
   
   -- Insert data
   INSERT INTO ... VALUES ...;
   ```

3. Run the `BulkLoadApp` and select your SQL file when prompted

### SQL File Best Practices

- Ensure statements are properly terminated with semicolons
- Create parent tables before child tables
- Create zones before tables
- Create tables before indexes
- Insert data only after tables are created
- Use the `IF NOT EXISTS` clause for idempotent operations
- Consider splitting large insert statements into smaller chunks
- Include comments to document your SQL file
