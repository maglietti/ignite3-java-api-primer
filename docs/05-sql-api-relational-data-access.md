# 5. SQL API - Relational Data Access

## Basic SQL Operations (`IgniteSql`)

### DDL Operations (CREATE, ALTER, DROP)

```java
client.sql().executeScript(
    "CREATE TABLE CITIES ("
    + "ID   INT PRIMARY KEY,"
    + "NAME VARCHAR);"
);
```

### DML Operations (INSERT, UPDATE, DELETE)

*[To be completed with DML examples]*

### Query Operations (SELECT)

```java
try (ResultSet<SqlRow> rs = client.sql().execute(null,
        "SELECT a.FIRST_NAME, a.LAST_NAME, c.NAME FROM ACCOUNTS a "
        + "INNER JOIN CITIES c on c.ID = a.CITY_ID ORDER BY a.ACCOUNT_ID")) {
    while (rs.hasNext()) {
        SqlRow row = rs.next();
        System.out.println(row.stringValue(0) + ", " + row.stringValue(1));
    }
}
```

## Prepared Statements

### Statement Builders

```java
Statement stmt = client.sql().createStatement("INSERT INTO CITIES (ID, NAME) VALUES (?, ?)");
```

### Parameter Binding

*[To be completed with parameter binding examples]*

### Reusable Statements

*[To be completed with reusable statement patterns]*

## Result Processing

### Working with `ResultSet<SqlRow>`

*[To be completed with result processing examples]*

### POJO Mapping with `Mapper<T>`

```java
Statement statement = client.sql().statementBuilder()
    .query("SELECT a.FIRST_NAME as firstName, a.LAST_NAME as lastName, a.BALANCE FROM ACCOUNTS a")
    .build();

try (ResultSet<AccountInfo> rs = client.sql().execute(null, Mapper.of(AccountInfo.class), statement)) {
    while (rs.hasNext()) {
        AccountInfo row = rs.next();
        System.out.println(row.firstName + ", " + row.lastName);
    }
}
```

### Iterating Through Results

*[To be completed with iteration patterns]*

## Batch Operations

### Batch Inserts/Updates

```java
long rowsAdded = Arrays.stream(client.sql().executeBatch(tx,
    "INSERT INTO ACCOUNTS (ACCOUNT_ID, CITY_ID, FIRST_NAME, LAST_NAME, BALANCE) values (?, ?, ?, ?, ?)",
    BatchedArguments.of(1, 1, "John", "Doe", 1000.0d)
        .add(2, 1, "Jane", "Roe", 2000.0d)
        .add(3, 2, "Mary", "Major", 1500.0d)))
    .sum();
```

### Performance Considerations

*[To be completed with performance guidance]*

## Async SQL Operations

```java
client.sql().executeAsync(null, stmt)
    .thenCompose(this::fetchAllRowsInto)
    .get();
```