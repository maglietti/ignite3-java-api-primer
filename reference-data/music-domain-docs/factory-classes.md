# Apache Ignite 3 Factory Classes

This document provides detailed implementations of factory classes for Apache Ignite 3 that help structure your code following best practices.

## ZoneFactory

A factory class for creating distribution zones with standard configurations:

```java
/**
 * Factory class for creating distribution zones with standardized configurations.
 */
public class ZoneFactory {
    private ZoneFactory() {} // Prevent instantiation
    
    /**
     * Creates a standard application zone with 2 replicas.
     * Suitable for most application data.
     */
    public static ZoneDefinition createStandardZone(String zoneName) {
        return ZoneDefinition.builder(zoneName)
                .ifNotExists()
                .replicas(2)
                .storageProfiles("default")
                .build();
    }
    
    /**
     * Creates a replicated zone optimized for reference data.
     * Higher replica count for better read performance, fewer partitions.
     */
    public static ZoneDefinition createReplicatedZone(String zoneName) {
        return ZoneDefinition.builder(zoneName)
                .ifNotExists()
                .replicas(3)
                .partitions(25)
                .storageProfiles("default")
                .build();
    }
    
    /**
     * Creates a zone optimized for write-heavy workloads.
     * Lower replica count, higher partition count for better write throughput.
     */
    public static ZoneDefinition createWriteOptimizedZone(String zoneName) {
        return ZoneDefinition.builder(zoneName)
                .ifNotExists()
                .replicas(1)
                .partitions(512)
                .storageProfiles("default")
                .build();
    }
}
```

## TableFactory

A factory for creating database tables with standardized error handling:

```java
/**
 * Factory class for creating and managing database tables.
 */
public class TableFactory {
    private TableFactory() {}
    
    /**
     * Creates a table for the given POJO class.
     * 
     * @param client The Ignite client
     * @param pojoClass The POJO class to create a table for
     * @return true if successful, false otherwise
     */
    public static <T> boolean createTable(IgniteClient client, Class<T> pojoClass) {
        try {
            client.catalog().createTable(pojoClass);
            System.out.println("Created table for " + pojoClass.getSimpleName());
            return true;
        } catch (Exception e) {
            System.err.println("Error creating table for " + pojoClass.getSimpleName() + 
                               ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Creates a table if it doesn't exist.
     * 
     * @param client The Ignite client
     * @param pojoClass The POJO class to create a table for
     * @return true if successful, false otherwise
     */
    public static <T> boolean createTableIfNotExists(IgniteClient client, Class<T> pojoClass) {
        try {
            // Extract table name from class name or annotations
            String tableName = extractTableName(pojoClass);
            
            if (!TableUtils.tableExists(client, tableName)) {
                client.catalog().createTable(pojoClass);
                System.out.println("Created table for " + pojoClass.getSimpleName());
            } else {
                System.out.println("Table " + tableName + " already exists");
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error creating table for " + pojoClass.getSimpleName() + 
                               ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Extract table name from POJO class.
     */
    private static <T> String extractTableName(Class<T> pojoClass) {
        // Try to get name from @Table annotation
        Table tableAnnotation = pojoClass.getAnnotation(Table.class);
        if (tableAnnotation != null && tableAnnotation.name() != null && 
            !tableAnnotation.name().isEmpty()) {
            return tableAnnotation.name();
        }
        
        // Fall back to class name
        return pojoClass.getSimpleName();
    }
}
```

## RecordViewFactory

A factory for creating and managing record views:

```java
/**
 * Factory class for creating and managing record views.
 */
public class RecordViewFactory {
    private RecordViewFactory() {}
    
    /**
     * Gets a record view for the given POJO class.
     * 
     * @param client The Ignite client
     * @param tableName The name of the table
     * @param pojoClass The POJO class to create a record view for
     * @return The record view or null if an error occurs
     */
    public static <T> RecordView<T> getRecordView(IgniteClient client, String tableName, 
                                                 Class<T> pojoClass) {
        try {
            Table table = client.tables().table(tableName);
            return table.recordView(pojoClass);
        } catch (Exception e) {
            System.err.println("Error getting record view: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets a key-value view for the given POJO class.
     * 
     * @param client The Ignite client
     * @param tableName The name of the table
     * @param keyClass The key class
     * @param valueClass The value class
     * @return The key-value view or null if an error occurs
     */
    public static <K, V> KeyValueView<K, V> getKeyValueView(IgniteClient client, String tableName, 
                                                           Class<K> keyClass, Class<V> valueClass) {
        try {
            Table table = client.tables().table(tableName);
            return table.keyValueView(keyClass, valueClass);
        } catch (Exception e) {
            System.err.println("Error getting key-value view: " + e.getMessage());
            return null;
        }
    }
}
```

## QueryFactory

A factory for creating and executing common SQL queries:

```java
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for creating and executing SQL queries.
 */
public class QueryFactory {
    private QueryFactory() {}
    
    /**
     * Creates a findAll query.
     * 
     * @param client The Ignite client
     * @param tableName The name of the table to query
     * @param pojoClass The POJO class to map results to
     * @return A list of POJOs representing all rows in the table
     */
    public static <T> List<T> findAll(IgniteClient client, String tableName, Class<T> pojoClass) {
        List<T> results = new ArrayList<>();
        
        try {
            client.sql().execute(null, "SELECT * FROM " + tableName)
                .forEachRemaining(row -> {
                    T instance = mapRowToPojo(row, pojoClass);
                    results.add(instance);
                });
        } catch (Exception e) {
            System.err.println("Error executing findAll query: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Creates a findById query for tables with a single primary key.
     * 
     * @param client The Ignite client
     * @param tableName The name of the table to query
     * @param idColumn The name of the ID column
     * @param id The ID value to search for
     * @param pojoClass The POJO class to map results to
     * @return The POJO representing the found row, or null if not found
     */
    public static <T, K> T findById(IgniteClient client, String tableName, String idColumn, K id, 
                                   Class<T> pojoClass) {
        try {
            var result = client.sql().execute(null, 
                "SELECT * FROM " + tableName + " WHERE " + idColumn + " = ?", id);
                
            if (result.hasNext()) {
                return mapRowToPojo(result.next(), pojoClass);
            }
        } catch (Exception e) {
            System.err.println("Error executing findById query: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Find entity by composite ID (for two ID columns).
     * 
     * @param client The Ignite client
     * @param tableName The name of the table to query
     * @param idColumn1 The name of the first ID column
     * @param id1 The first ID value to search for
     * @param idColumn2 The name of the second ID column
     * @param id2 The second ID value to search for
     * @param pojoClass The POJO class to map results to
     * @return The POJO representing the found row, or null if not found
     */
    public static <T, K1, K2> T findByCompositeId(IgniteClient client, String tableName, 
                                                String idColumn1, K1 id1,
                                                String idColumn2, K2 id2, 
                                                Class<T> pojoClass) {
        try {
            var result = client.sql().execute(null, 
                "SELECT * FROM " + tableName + " WHERE " + idColumn1 + " = ? AND " + 
                idColumn2 + " = ?", id1, id2);
                
            if (result.hasNext()) {
                return mapRowToPojo(result.next(), pojoClass);
            }
        } catch (Exception e) {
            System.err.println("Error executing findByCompositeId query: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Map a SQL row to a POJO instance using reflection.
     * This generic implementation works with any class that has a default constructor
     * and fields following Java bean naming conventions.
     * 
     * @param row The SQL row to map
     * @param pojoClass The POJO class to map to
     * @return A new instance of the POJO class with fields populated from the row
     */
    private static <T> T mapRowToPojo(SqlRow row, Class<T> pojoClass) {
        try {
            // Create a new instance using the default constructor
            T instance = pojoClass.getDeclaredConstructor().newInstance();
            
            // Get all the declared fields in the class
            Field[] fields = pojoClass.getDeclaredFields();
            
            for (Field field : fields) {
                // Skip static or final fields
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                    continue;
                }
                
                // Get column name from @Column annotation or use field name
                String columnName;
                Column columnAnnotation = field.getAnnotation(Column.class);
                if (columnAnnotation != null && !columnAnnotation.value().isEmpty()) {
                    columnName = columnAnnotation.value();
                } else {
                    columnName = field.getName();
                }
                
                // Column names are often uppercase in SQL results
                try {
                    // Try exact match first
                    if (containsColumn(row, columnName)) {
                        setFieldValue(instance, field, row, columnName);
                    }
                    // Try uppercase
                    else if (containsColumn(row, columnName.toUpperCase())) {
                        setFieldValue(instance, field, row, columnName.toUpperCase());
                    }
                    // Try lowercase
                    else if (containsColumn(row, columnName.toLowerCase())) {
                        setFieldValue(instance, field, row, columnName.toLowerCase());
                    }
                } catch (Exception e) {
                    // Skip this field if there's any error
                    System.err.println("Could not map field " + field.getName() + 
                                      ": " + e.getMessage());
                }
            }
            
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Error mapping row to POJO: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if a column exists in the SqlRow.
     */
    private static boolean containsColumn(SqlRow row, String columnName) {
        for (int i = 0; i < row.columnCount(); i++) {
            if (row.columnName(i).equalsIgnoreCase(columnName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Set a field value based on its type.
     */
    private static <T> void setFieldValue(T instance, Field field, SqlRow row, 
                                         String columnName) throws Exception {
        // Save the original accessibility and make the field accessible
        boolean originalAccessible = field.canAccess(instance);
        field.setAccessible(true);
        
        try {
            Class<?> fieldType = field.getType();
            
            // Handle different types appropriately
            if (fieldType == Integer.class || fieldType == int.class) {
                try {
                    field.set(instance, row.intValue(columnName));
                } catch (Exception e) {
                    // Handle null for primitive types
                    if (fieldType == Integer.class) {
                        field.set(instance, null);
                    } else {
                        field.set(instance, 0);
                    }
                }
            } 
            else if (fieldType == Long.class || fieldType == long.class) {
                try {
                    field.set(instance, row.longValue(columnName));
                } catch (Exception e) {
                    if (fieldType == Long.class) {
                        field.set(instance, null);
                    } else {
                        field.set(instance, 0L);
                    }
                }
            } 
            else if (fieldType == Double.class || fieldType == double.class) {
                try {
                    field.set(instance, row.doubleValue(columnName));
                } catch (Exception e) {
                    if (fieldType == Double.class) {
                        field.set(instance, null);
                    } else {
                        field.set(instance, 0.0);
                    }
                }
            } 
            else if (fieldType == Boolean.class || fieldType == boolean.class) {
                try {
                    field.set(instance, row.booleanValue(columnName));
                } catch (Exception e) {
                    if (fieldType == Boolean.class) {
                        field.set(instance, null);
                    } else {
                        field.set(instance, false);
                    }
                }
            } 
            else if (fieldType == String.class) {
                try {
                    field.set(instance, row.stringValue(columnName));
                } catch (Exception e) {
                    field.set(instance, null);
                }
            } 
            else if (fieldType == BigDecimal.class) {
                try {
                    field.set(instance, row.decimalValue(columnName));
                } catch (Exception e) {
                    field.set(instance, null);
                }
            } 
            else if (fieldType == java.sql.Date.class || fieldType == java.time.LocalDate.class) {
                try {
                    // Handle date conversion
                    if (fieldType == java.sql.Date.class) {
                        field.set(instance, row.dateValue(columnName));
                    } else {
                        java.sql.Date date = row.dateValue(columnName);
                        if (date != null) {
                            field.set(instance, date.toLocalDate());
                        } else {
                            field.set(instance, null);
                        }
                    }
                } catch (Exception e) {
                    field.set(instance, null);
                }
            } 
            else if (fieldType == java.sql.Timestamp.class || 
                    fieldType == java.time.LocalDateTime.class) {
                try {
                    // Handle timestamp conversion
                    if (fieldType == java.sql.Timestamp.class) {
                        field.set(instance, row.timestampValue(columnName));
                    } else {
                        java.sql.Timestamp timestamp = row.timestampValue(columnName);
                        if (timestamp != null) {
                            field.set(instance, timestamp.toLocalDateTime());
                        } else {
                            field.set(instance, null);
                        }
                    }
                } catch (Exception e) {
                    field.set(instance, null);
                }
            } 
            else {
                // For other types, try to use Object value
                try {
                    field.set(instance, row.value(columnName));
                } catch (Exception e) {
                    field.set(instance, null);
                }
            }
        } finally {
            // Restore original accessibility
            field.setAccessible(originalAccessible);
        }
    }
}
```

## Example Usage

Here's how to use these factory classes together in an application:

```java
// Connect to the cluster
try (IgniteClient client = connectToCluster()) {
    if (client == null) {
        System.err.println("Failed to connect to the cluster. Exiting.");
        System.exit(1);
    }

    // Create a standard zone if it doesn't exist
    if (!TableUtils.zoneExists(client, "test_zone")) {
        ZoneDefinition zoneTestZone = ZoneFactory.createStandardZone("test_zone");
        client.catalog().createZone(zoneTestZone);
        System.out.println("Created test_zone");
    }

    // Create table from POJO class if it doesn't exist
    TableFactory.createTableIfNotExists(client, personPojo.class);

    // Get a record view for the table
    RecordView<personPojo> personView = RecordViewFactory.getRecordView(
        client, "personPojo", personPojo.class);

    // Create and insert a person
    personPojo person = personPojo.create(1, "P001", "John", "Doe", "Software Engineer");
    personView.upsert(null, person);
    
    // Find a person by composite ID
    personPojo foundPerson = QueryFactory.findByCompositeId(
        client, "personPojo", "id", 1, "id_str", "P001", personPojo.class);
    
    if (foundPerson != null) {
        System.out.println("Found: " + foundPerson.getFirstName() + " " + foundPerson.getLastName());
    }
    
    // Get all people
    List<personPojo> allPeople = QueryFactory.findAll(client, "personPojo", personPojo.class);
    System.out.println("Found " + allPeople.size() + " people");
}
```

These factory classes help structure your code, centralize common operations, and provide consistent configurations across your application.
