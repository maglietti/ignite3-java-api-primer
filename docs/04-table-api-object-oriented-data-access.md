# 4. Table API - Object-Oriented Data Access

## Table Management (`IgniteTables`)

- Listing tables
- Getting table references

## Key-Value Operations (`KeyValueView`)

### Working with Tuples

```java
KeyValueView<Tuple, Tuple> kvView = client.tables().table("accounts").keyValueView();

Tuple key = Tuple.create().set("accountNumber", 123456);
Tuple value = Tuple.create()
    .set("firstName", "Val")
    .set("lastName", "Kulichenko")
    .set("balance", 100.00d);
kvView.put(null, key, value);
```

### Working with POJOs

```java
KeyValueView<AccountKey, Account> kvView = client.tables()
    .table("accounts")
    .keyValueView(AccountKey.class, Account.class);

AccountKey key = new AccountKey(123456);
Account value = new Account("Val", "Kulichenko", 100.00d);
kvView.put(null, key, value);
```

### Put, Get, Remove Operations

*[To be completed with comprehensive examples]*

## Record Operations (`RecordView`)

### Insert, Update, Upsert, Delete

```java
RecordView<Tuple> accounts = client.tables().table("accounts").recordView();

Tuple newAccountTuple = Tuple.create()
    .set("accountNumber", 123456)
    .set("firstName", "Val")
    .set("lastName", "Kulichenko")
    .set("balance", 100.00d);
accounts.insert(null, newAccountTuple);
```

### Bulk Operations

*[To be completed with bulk operation examples]*

## Async Operations

### CompletableFuture Patterns

*[To be completed with async examples]*

### Error Handling in Async Code

*[To be completed with error handling patterns]*