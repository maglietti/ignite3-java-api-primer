# Apache Ignite 3 SQL Engine Architecture

## Understanding Apache Calcite integration and SQL processing in Ignite 3

## Overview

Apache Ignite 3 uses **Apache Calcite** as its SQL engine foundation, providing standards-compliant SQL processing with distributed query planning and optimization. Unlike traditional database systems where SQL is an afterthought, Ignite 3 integrates Calcite deeply into its distributed architecture to enable efficient query processing across cluster nodes.

This document explains how Calcite powers Ignite 3's SQL capabilities and the architectural decisions that enable distributed SQL processing at scale.

## Apache Calcite Integration

### What is Apache Calcite?

Apache Calcite is a framework for building databases and data management systems. It provides:

- **SQL Parser**: Converts SQL text into abstract syntax trees
- **SQL Validator**: Validates syntax and semantics against schema metadata
- **Query Planner**: Transforms queries into optimized execution plans
- **Cost-Based Optimizer**: Selects efficient execution strategies
- **Rule Engine**: Applies optimization transformations

### Calcite in Ignite 3 Architecture

```mermaid
graph TB
    subgraph "SQL Request Flow"
        CLIENT["Client Application"] --> PARSER["SQL Parser<br/>(Calcite)"]
        PARSER --> VALIDATOR["SQL Validator<br/>(Calcite)"]
        VALIDATOR --> PLANNER["Query Planner<br/>(Calcite + Ignite)"]
        PLANNER --> OPTIMIZER["Cost-Based Optimizer<br/>(Calcite Rules)"]
        OPTIMIZER --> EXECUTOR["Distributed Executor<br/>(Ignite)"]
        EXECUTOR --> STORAGE["Storage Engines<br/>(aimem/aipersist/rocksdb)"]
    end
    
    subgraph "Distributed Coordination"
        EXECUTOR --> NODE1["Node 1 Execution"]
        EXECUTOR --> NODE2["Node 2 Execution"] 
        EXECUTOR --> NODE3["Node 3 Execution"]
    end
```

## SQL Processing Pipeline

### 1. Parsing and Validation

When you execute a SQL statement, Ignite 3 uses Calcite's parser:

```java
// Your SQL statement
Statement stmt = client.sql().statementBuilder()
    .query("SELECT a.Name, COUNT(*) FROM Artist a JOIN Album al ON a.ArtistId = al.ArtistId GROUP BY a.Name")
    .build();

// Internally, Calcite processes this through:
// 1. SqlParser.parseQuery() -> SqlNode (AST)
// 2. SqlValidator.validate() -> Validated SqlNode
// 3. SqlToRelConverter.convertQuery() -> RelNode (logical plan)
```

**Calcite Components Used:**

- **SqlParser**: Converts SQL text to Abstract Syntax Tree (SqlNode)
- **SqlValidator**: Validates against Ignite's schema metadata
- **SqlToRelConverter**: Transforms to relational algebra (RelNode tree)

### 2. Logical Planning

Calcite transforms validated SQL into a logical query plan:

```text
LogicalProject(Name=[$0], EXPR$1=[$1])
  LogicalAggregate(group=[{0}], EXPR$1=[COUNT()])
    LogicalProject(Name=[$1])
      LogicalJoin(condition=[=($0, $2)], joinType=[inner])
        LogicalTableScan(table=[[Artist]])
        LogicalTableScan(table=[[Album]])
```

**Key Transformations:**

- **Predicate Pushdown**: WHERE clauses move closer to data sources
- **Projection Pushdown**: SELECT columns filtered early
- **Join Reordering**: Optimal join sequence based on statistics

### 3. Physical Planning

Ignite 3 extends Calcite with distributed-aware rules:

```text
IgniteDistributedProject(Name=[$0], EXPR$1=[$1])
  IgniteReduceAggregate(group=[{0}], EXPR$1=[COUNT($0)])
    IgniteMapAggregate(group=[{0}], EXPR$1=[COUNT()])
      IgniteColocatedJoin(condition=[=($0, $2)])
        IgniteTableScan(table=[[Artist]], partitions=[0,1,2...31])
        IgniteTableScan(table=[[Album]], partitions=[0,1,2...31])
```

**Ignite-Specific Optimizations:**

- **Colocation Detection**: Identifies when joins can execute locally
- **Partition Pruning**: Eliminates unnecessary partition scans  
- **Map-Reduce Planning**: Splits aggregations across nodes
- **Index Selection**: Chooses optimal indexes for table scans

### 4. Distributed Execution

The physical plan executes across cluster nodes:

```java
// Execution coordinates across nodes
public class DistributedQueryExecution {
    
    // Each node executes its partition of the plan
    CompletableFuture<List<Row>> executeFragment(PlanFragment fragment) {
        return fragment.getPartitions().stream()
            .map(this::scanPartition)
            .collect(toAsyncList());
    }
    
    // Results aggregate back to coordinator
    List<Row> combineResults(List<CompletableFuture<List<Row>>> futures) {
        return futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .collect(toList());
    }
}
```

## Query Optimization Features

### Cost-Based Optimization

Calcite's cost-based optimizer uses statistics to choose execution plans:

```java
// Ignite maintains table statistics for optimization
public class TableStatistics {
    private final long rowCount;                    // Total rows in table
    private final Map<String, ColumnStats> columns; // Per-column statistics
    private final double[] partitionSizes;          // Partition size distribution
    
    // Cost estimation for different access paths
    public double estimateSelectivity(RexNode predicate) {
        // Use column histograms and null counts
        return histogramBasedEstimate(predicate);
    }
}
```

**Statistics Used:**

- **Row Counts**: Total rows per table and partition
- **Column Statistics**: Null counts, distinct values, histograms
- **Index Statistics**: Index selectivity and size
- **Partition Distribution**: Data distribution across nodes

### Rule-Based Optimizations

Calcite applies transformation rules to improve query performance:

```java
// Key optimization rules applied by Ignite 3
List<RelOptRule> optimizationRules = List.of(
    
    // Predicate pushdown - move filters closer to data
    FilterTableScanRule.INSTANCE,
    FilterProjectTransposeRule.INSTANCE,
    FilterJoinRule.FILTER_ON_JOIN,
    
    // Projection elimination - remove unused columns
    ProjectRemoveRule.INSTANCE,
    ProjectMergeRule.INSTANCE,
    
    // Join optimization - reorder and select algorithms  
    JoinCommuteRule.INSTANCE,
    JoinAssociateRule.INSTANCE,
    
    // Aggregate optimization - push partial aggregates
    AggregateProjectMergeRule.INSTANCE,
    AggregateJoinTransposeRule.EXTENDED,
    
    // Ignite-specific rules for distribution
    IgniteColocatedJoinRule.INSTANCE,
    IgniteMapReduceRule.INSTANCE
);
```

### Distributed Query Patterns

Ignite 3 optimizes for distributed execution patterns:

**1. Colocation-Aware Joins**

```sql
-- When data is colocated by ArtistId, this executes locally
SELECT a.Name, al.Title 
FROM Artist a 
JOIN Album al ON a.ArtistId = al.ArtistId 
WHERE a.ArtistId = 123
```

**Optimization**: Calcite detects colocation and generates local join plans.

**2. Map-Reduce Aggregations**

```sql
-- Aggregation splits across nodes for parallel processing
SELECT Genre, COUNT(*), AVG(UnitPrice)
FROM Track 
GROUP BY Genre
```

**Optimization**: Partial aggregates on each node, final reduction on coordinator.

**3. Partition Pruning**

```sql
-- Only scans partitions containing the specified key
SELECT * FROM Track WHERE TrackId = 12345
```

**Optimization**: Hash-based partition elimination reduces I/O.

## SQL Standard Compliance

### SQL Capabilities: Ignite 3 vs Full Apache Calcite

While Ignite 3 uses Apache Calcite as its foundation, it implements a subset of Calcite's full capabilities. The following comparison shows what's actually supported:

| Feature Category | Ignite 3 Implementation | Full Calcite Capability | Status |
|------------------|------------------------|---------------------------|--------|
| **Core SQL Operations** | | | |
| SELECT, INSERT, UPDATE, DELETE, UPSERT | ✅ Full support | ✅ Full support | Complete |
| Subqueries and correlated queries | ✅ Full support | ✅ Full support | Complete |
| CASE expressions and conditional logic | ✅ Full support | ✅ Full support | Complete |
| **Data Types** | | | |
| Numeric types (INT, DECIMAL, FLOAT, etc.) | ✅ Full support | ✅ Full support | Complete |
| String types (VARCHAR, CHAR) | ✅ Full support | ✅ Full support | Complete |
| Date/Time types (DATE, TIMESTAMP, etc.) | ✅ Full support | ✅ Full support | Complete |
| BOOLEAN, UUID, NULL | ✅ Full support | ✅ Full support | Complete |
| Collections (ARRAY, MAP) | ⚠️ Basic support | ✅ Full support | Limited |
| **JOIN Operations** | | | |
| INNER, LEFT, RIGHT, FULL OUTER, CROSS | ✅ Full support | ✅ Full support | Complete |
| NATURAL JOIN | ✅ Full support | ✅ Full support | Complete |
| Complex join expressions | ✅ Full support | ✅ Full support | Complete |
| **Aggregate Functions** | | | |
| COUNT, SUM, AVG, MIN, MAX | ✅ Full support | ✅ Full support | Complete |
| ANY_VALUE, SINGLE_VALUE | ✅ Full support | ✅ Full support | Complete |
| EVERY, SOME with FILTER | ✅ Full support | ✅ Full support | Complete |
| **Window Functions** | | | |
| ROW_NUMBER, RANK, DENSE_RANK | ❌ Not supported | ✅ Full support | Missing |
| LAG, LEAD, FIRST_VALUE, LAST_VALUE | ❌ Not supported | ✅ Full support | Missing |
| Analytical functions (NTILE, CUME_DIST) | ❌ Not supported | ✅ Full support | Missing |
| **Mathematical Functions** | | | |
| Basic math (ABS, SQRT, POWER, etc.) | ✅ Full support | ✅ Full support | Complete |
| Trigonometric functions | ✅ Full support | ✅ Full support | Complete |
| Statistical functions (STDDEV, VAR) | ❌ Not supported | ✅ Full support | Missing |
| **String Functions** | | | |
| UPPER, LOWER, SUBSTRING, CONCAT | ✅ Full support | ✅ Full support | Complete |
| Pattern matching (LIKE, SIMILAR TO) | ✅ Full support | ✅ Full support | Complete |
| Advanced string ops (SOUNDEX, TRANSLATE) | ✅ Full support | ✅ Full support | Complete |
| **Date/Time Functions** | | | |
| CURRENT_DATE, EXTRACT, date arithmetic | ✅ Full support | ✅ Full support | Complete |
| Unix timestamp functions | ✅ Full support | ✅ Full support | Complete |
| Time zone operations | ⚠️ Basic support | ✅ Full support | Limited |
| **JSON Functions** | | | |
| JSON_VALUE, JSON_QUERY, JSON_EXISTS | ✅ Full support | ✅ Full support | Complete |
| JSON_OBJECT, JSON_ARRAY | ✅ Full support | ✅ Full support | Complete |
| JSON predicates and manipulation | ✅ Full support | ✅ Full support | Complete |
| **Advanced Features** | | | |
| Common Table Expressions (WITH) | ✅ Full support | ✅ Full support | Complete |
| Recursive CTEs (WITH RECURSIVE) | ❌ Explicitly blocked | ✅ Full support | Missing |
| Set operations (UNION, INTERSECT, EXCEPT) | ✅ Full support | ✅ Full support | Complete |
| PIVOT/UNPIVOT operations | ❌ Not supported | ✅ Full support | Missing |
| MATCH_RECOGNIZE pattern matching | ❌ Not supported | ✅ Full support | Missing |
| **Collection Operations** | | | |
| Basic ARRAY and MAP operations | ⚠️ Basic support | ✅ Full support | Limited |
| MULTISET operations | ❌ Not supported | ✅ Full support | Missing |
| Collection querying (ARRAY_QUERY) | ❌ Not supported | ✅ Full support | Missing |

### Example of Supported SQL

```java
Statement supportedQuery = client.sql().statementBuilder()
    .query("""
        WITH artist_stats AS (
            SELECT a.ArtistId, a.Name,
                   COUNT(t.TrackId) as track_count,
                   AVG(t.UnitPrice) as avg_price
            FROM Artist a
            JOIN Album al ON a.ArtistId = al.ArtistId
            JOIN Track t ON al.AlbumId = t.AlbumId
            GROUP BY a.ArtistId, a.Name
        )
        SELECT Name, track_count, avg_price
        FROM artist_stats
        WHERE track_count > 10
        ORDER BY track_count DESC
        """)
    .build();
```

### Example of Unsupported SQL

```java
// This will fail - window functions not supported
Statement unsupportedQuery = client.sql().statementBuilder()
    .query("""
        SELECT a.Name,
               COUNT(t.TrackId) as track_count,
               ROW_NUMBER() OVER (ORDER BY COUNT(t.TrackId) DESC) as rank
        FROM Artist a
        JOIN Album al ON a.ArtistId = al.ArtistId
        JOIN Track t ON al.AlbumId = t.AlbumId
        GROUP BY a.ArtistId, a.Name
        """)
    .build();
```

### Data Type Handling

Calcite manages type conversions and compatibility:

```java
// Automatic type promotion and conversion
public class TypeConversion {
    
    // Integer to Decimal for precision
    "SELECT UnitPrice * Quantity"  // INT * DECIMAL -> DECIMAL
    
    // String to Date parsing
    "WHERE InvoiceDate > '2023-01-01'"  // VARCHAR -> DATE
    
    // Null handling with proper three-valued logic
    "SELECT Name WHERE ArtistId IS NOT NULL"
}
```

## Metadata Management

### Schema Information

Calcite integrates with Ignite's schema registry:

```java
// Schema metadata flows from Ignite to Calcite
public class IgniteSchema implements Schema {
    
    @Override
    public Table getTable(String name) {
        // Retrieve table metadata from Ignite catalog
        TableDefinition tableDef = igniteClient.catalog().table(name);
        
        // Convert to Calcite table representation
        return new IgniteTable(tableDef);
    }
    
    @Override
    public Set<String> getTableNames() {
        // List all tables in the schema
        return igniteClient.catalog().tables();
    }
}
```

### Column Metadata Normalization

**Critical Architecture Decision**: Ignite 3 normalizes all identifiers to uppercase:

```java
// Internal metadata representation
public class ColumnMetadata {
    private final String name;           // Always uppercase: "ARTISTID"
    private final SqlTypeName type;      // Calcite type: VARCHAR, INTEGER, etc.
    private final boolean nullable;      // Null constraint
    private final Integer precision;     // For numeric types
    private final Integer scale;         // For decimal types
    
    // All lookups must use uppercase names
    public Object getValue(SqlRow row, String columnName) {
        return row.value(columnName.toUpperCase());
    }
}
```

## Performance Considerations

### Query Plan Caching

Calcite query plans are cached for reuse:

```java
// Statement objects cache parsed and planned queries
Statement cachedStatement = client.sql().statementBuilder()
    .query("SELECT * FROM Artist WHERE Name LIKE ?")
    .build();

// Reuse avoids parsing and planning overhead
for (String pattern : patterns) {
    ResultSet<SqlRow> results = client.sql().execute(null, cachedStatement, pattern);
    // Process results...
}
```

### Statistics and Cost Estimation

Accurate statistics improve optimization:

```sql
-- Ignite maintains statistics automatically
ANALYZE TABLE Artist;
ANALYZE TABLE Album;  
ANALYZE TABLE Track;

-- Statistics include:
-- - Row counts per table and partition
-- - Column value distributions and histograms  
-- - Index selectivity measurements
-- - Partition size distributions
```

### Join Algorithm Selection

Calcite chooses optimal join algorithms based on data size and distribution:

**Hash Joins**: For moderate-sized tables with good hash distribution
**Nested Loop Joins**: For small tables or when indexes are available
**Merge Joins**: For sorted inputs or range predicates

## Troubleshooting SQL Issues

### Query Plan Analysis

Use EXPLAIN to understand Calcite's decisions:

```java
Statement explainStmt = client.sql().statementBuilder()
    .query("EXPLAIN PLAN FOR SELECT a.Name, COUNT(*) FROM Artist a JOIN Album al ON a.ArtistId = al.ArtistId GROUP BY a.Name")
    .build();

ResultSet<SqlRow> plan = client.sql().execute(null, explainStmt);
// Examine the physical execution plan
```

### Common Optimization Issues

**Problem**: Slow joins across non-colocated tables
**Solution**: Redesign schema for colocation or use broadcast joins for small tables

**Problem**: Inefficient aggregations
**Solution**: Ensure proper partitioning and consider pre-aggregation strategies

**Problem**: Full table scans instead of index usage
**Solution**: Update table statistics and verify index definitions

## Integration with Distributed Systems

### Fault Tolerance

Calcite query execution integrates with Ignite's fault tolerance:

```java
// Query execution handles node failures gracefully
public class FaultTolerantExecution {
    
    public CompletableFuture<ResultSet> executeQuery(Statement stmt) {
        return executeWithRetry(stmt)
            .handle((result, throwable) -> {
                if (throwable instanceof NodeFailureException) {
                    // Retry on different node
                    return executeOnBackupNode(stmt);
                }
                return result;
            });
    }
}
```

### Consistency Guarantees

SQL operations respect Ignite's MVCC transaction model:

- **Read Operations**: See consistent snapshots as of transaction start time
- **Write Operations**: Use distributed locking for consistency
- **DDL Operations**: Coordinate schema changes across all nodes


## Final Thoughts

Apache Calcite provides the SQL foundation that makes Ignite 3 a powerful distributed database:

- **Standards Compliance**: Full SQL support with proper semantics
- **Cost-Based Optimization**: Intelligent query planning using statistics
- **Distributed Awareness**: Optimization rules that understand data distribution
- **Extensibility**: Plugin architecture for custom optimizations

Understanding this architecture helps you:

- Write efficient SQL queries that leverage distribution
- Troubleshoot performance issues with query plans
- Design schemas that work well with the optimizer
- Make informed decisions about data modeling and partitioning

The deep integration between Calcite and Ignite's distributed storage enables SQL processing that scales horizontally while maintaining ACID properties and consistent performance.
