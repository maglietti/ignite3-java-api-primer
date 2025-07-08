<!--
Licensed under Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International (CC BY-NC-SA 4.0)
SPDX-License-Identifier: CC-BY-NC-SA-4.0
For full license text, see LICENSE-CC-BY-NC-SA-4.0
-->

# Apache Ignite 3 Java API Architecture

## Java API structure and implementation patterns

This document examines the Apache Ignite 3 Java API architecture, organizing components into logical layers and documenting their relationships and interaction patterns.

> **Related Architecture**: For distributed system concepts and node architecture, see [Ignite 3 Architecture](./ignite3-arch.md). For storage implementation details, see [Storage System Architecture](./storage-system-arch.md).

## Architecture Layers

### I. Connection & Lifecycle Layer

The foundation layer responsible for establishing connections and managing node lifecycles.

```
Entry Points & Builders
├── IgniteClient (Thin Client)
│   ├── IgniteClient.Builder
│   │   ├── addresses() - server endpoints
│   │   ├── authenticator() - security configuration
│   │   ├── ssl() - SSL/TLS settings
│   │   ├── retryPolicy() - retry strategies
│   │   ├── connectTimeout() - connection timeouts
│   │   ├── heartbeatInterval() - keepalive settings
│   │   └── metricsEnabled() - monitoring
│   ├── IgniteClientConfiguration
│   └── Connection Management
│       ├── Connection pooling
│       ├── Load balancing
│       ├── Failover handling
│       └── Background reconnection
├── IgniteServer (Embedded Node)
│   ├── IgniteServer.Builder
│   │   ├── serviceLoaderClassLoader() - plugin loading
│   │   └── asyncContinuationExecutor() - async handling
│   ├── InitParameters
│   │   ├── Cluster initialization
│   │   ├── Meta storage configuration
│   │   └── CMG (Cluster Management Group) setup
│   └── Cluster Lifecycle
│       ├── Node startup/shutdown
│       ├── Cluster initialization
│       └── Topology management
└── Core Ignite Interface
    ├── name() - node identification
    ├── tables() - table access
    ├── transactions() - transaction management
    ├── sql() - SQL operations
    ├── compute() - distributed computing
    ├── catalog() - schema management
    └── clusterNodes() - topology information
```

**Key Characteristics:**

- Builder pattern for configuration
- Resource management through AutoCloseable
- Async-first design with CompletableFuture support
- Connection pooling and failover

---

### II. Schema Definition & Management Layer

Comprehensive schema definition through annotations and dynamic management through the catalog API.

```
Annotations API (Schema-as-Code)
├── Table Definition
│   ├── @Table
│   │   ├── value() - table name
│   │   ├── schemaName() - schema namespace
│   │   ├── indexes() - secondary index definitions
│   │   ├── primaryKeyType() - PK strategy (DEFAULT, SORTED, HASH)
│   │   ├── colocateBy() - colocation configuration
│   │   └── zone() - distribution zone
│   ├── @Column
│   │   ├── value() - column name
│   │   ├── nullable() - nullability constraint
│   │   ├── length() - string length
│   │   ├── precision() & scale() - numeric precision
│   │   └── columnDefinition() - custom SQL definition
│   └── @Id
│       ├── value() - sort order (DEFAULT, ASC, DESC)
│       └── Composite key support
├── Distribution & Performance
│   ├── @Zone
│   │   ├── value() - zone name
│   │   ├── storageProfiles() - storage configuration
│   │   ├── partitions() - partition count
│   │   ├── replicas() - replication factor
│   │   ├── quorumSize() - consistency quorum
│   │   ├── distributionAlgorithm() - partitioning strategy
│   │   ├── dataNodesAutoAdjust*() - auto-scaling
│   │   ├── filter() - node filtering
│   │   └── consistencyMode() - consistency level
│   ├── @Index
│   │   ├── value() - index name
│   │   ├── columns() - indexed columns
│   │   └── type() - index type (DEFAULT, SORTED, HASH)
│   └── @ColumnRef
│       ├── value() - column name
│       └── sort() - sort order
└── Processing & Generation
    ├── CreateFromAnnotationsImpl
    │   ├── processRecordClass() - single POJO processing
    │   ├── processKeyValueClasses() - key-value processing
    │   └── SQL DDL generation
    ├── Validation & Type Mapping
    │   ├── Annotation validation
    │   ├── Java-to-SQL type mapping
    │   └── Constraint validation
    └── Integration Points
        ├── Catalog API integration
        ├── Table API integration
        └── SQL API integration

Catalog Management API
├── IgniteCatalog
│   ├── createTable(Class) - from annotated record class
│   ├── createTable(keyClass, valueClass) - key-value mapping
│   ├── createTableAsync() - async table creation
│   ├── tableDefinition() - schema introspection
│   ├── tableDefinitionAsync() - async schema access
│   └── DDL Operations
│       ├── Zone management
│       ├── Index operations
│       └── Schema evolution
└── Dynamic Schema Operations
    ├── Runtime table creation
    ├── Schema modification
    ├── Index management
    └── Zone configuration
```

**Key Characteristics:**

- Annotation-driven schema definition
- Automatic DDL generation
- Enterprise distribution features (zones, colocation)
- Type-safe schema operations

---

### III. Data Access Layer

Dual-paradigm data access supporting both object-oriented and relational approaches.

```
Table API (Object-Oriented Access)
├── IgniteTables (Table Management)
│   ├── tables() - list all tables
│   ├── tablesAsync() - async table listing
│   ├── table(name) - get table reference
│   └── tableAsync(name) - async table access
├── KeyValueView<K,V> (Key-Value Operations)
│   ├── Basic Operations
│   │   ├── get(tx, key) - single key retrieval
│   │   ├── getAsync(tx, key) - async retrieval
│   │   ├── put(tx, key, value) - single key storage
│   │   ├── putAsync(tx, key, value) - async storage
│   │   ├── remove(tx, key) - key removal
│   │   └── removeAsync(tx, key) - async removal
│   ├── Bulk Operations
│   │   ├── getAll(tx, keys) - multi-key retrieval
│   │   ├── getAllAsync(tx, keys) - async multi-get
│   │   ├── putAll(tx, pairs) - bulk storage
│   │   ├── putAllAsync(tx, pairs) - async bulk storage
│   │   ├── removeAll(tx, keys) - bulk removal
│   │   └── removeAllAsync(tx, keys) - async bulk removal
│   ├── Conditional Operations
│   │   ├── putIfAbsent() - conditional put
│   │   ├── replace() - conditional replace
│   │   ├── compareAndSet() - atomic compare-and-swap
│   │   └── Async variants
│   └── Streaming Integration
│       ├── streamData() - high-throughput ingestion
│       └── DataStreamerOptions configuration
├── RecordView<T> (Record Operations)
│   ├── CRUD Operations
│   │   ├── insert(tx, record) - record insertion
│   │   ├── insertAsync(tx, record) - async insertion
│   │   ├── update(tx, record) - record update
│   │   ├── updateAsync(tx, record) - async update
│   │   ├── upsert(tx, record) - insert or update
│   │   ├── upsertAsync(tx, record) - async upsert
│   │   ├── delete(tx, key) - record deletion
│   │   └── deleteAsync(tx, key) - async deletion
│   ├── Bulk Operations
│   │   ├── insertAll(tx, records) - bulk insertion
│   │   ├── insertAllAsync(tx, records) - async bulk insert
│   │   ├── updateAll(tx, records) - bulk update
│   │   ├── updateAllAsync(tx, records) - async bulk update
│   │   ├── upsertAll(tx, records) - bulk upsert
│   │   ├── upsertAllAsync(tx, records) - async bulk upsert
│   │   ├── deleteAll(tx, keys) - bulk deletion
│   │   └── deleteAllAsync(tx, keys) - async bulk deletion
│   ├── Conditional Operations
│   │   ├── insertIfAbsent() - conditional insert
│   │   ├── updateIf() - conditional update
│   │   ├── deleteIf() - conditional delete
│   │   └── Async variants
│   └── Query Integration
│       ├── SQL query support
│       └── Result mapping
└── POJO Mapping System
    ├── Mapper<T>
    │   ├── Mapper.of(Class) - auto-mapping
    │   ├── builder() - manual mapping
    │   └── Type conversion support
    ├── PojoMapper<T>
    │   ├── Multi-column mapping
    │   ├── Field-to-column mapping
    │   └── Custom name mapping
    ├── MapperBuilder<T>
    │   ├── map(field, column) - explicit mapping
    │   ├── automap() - automatic mapping
    │   ├── convert(column, converter) - type conversion
    │   └── build() - mapper creation
    └── Type Conversion
        ├── Built-in converters (primitives, dates, UUID)
        ├── Custom converters
        └── Enum support

SQL API (Relational Access)
├── IgniteSql (SQL Facade)
│   ├── DDL Operations
│   │   ├── CREATE TABLE/INDEX/ZONE
│   │   ├── ALTER TABLE
│   │   ├── DROP TABLE/INDEX/ZONE
│   │   └── Schema introspection
│   ├── DML Operations
│   │   ├── INSERT - data insertion
│   │   ├── UPDATE - data modification
│   │   ├── DELETE - data removal
│   │   └── MERGE - upsert operations
│   ├── Query Operations
│   │   ├── SELECT - data retrieval
│   │   ├── Complex joins
│   │   ├── Aggregations
│   │   └── Window functions
│   └── Script Execution
│       ├── executeScript() - multi-statement
│       └── executeScriptAsync() - async script execution
├── Statement Management
│   ├── Statement
│   │   ├── createStatement() - prepared statements
│   │   ├── Parameter binding
│   │   └── Reusable execution
│   ├── Statement.StatementBuilder
│   │   ├── query() - SQL text
│   │   ├── queryTimeout() - execution timeout
│   │   ├── pageSize() - result pagination
│   │   ├── properties() - execution properties
│   │   └── build() - statement creation
│   └── Parameter Management
│       ├── Positional parameters (?)
│       ├── Type-safe binding
│       └── Null value handling
├── Result Processing
│   ├── ResultSet<SqlRow>
│   │   ├── hasNext() - result iteration
│   │   ├── next() - row access
│   │   ├── affectedRows() - modification count
│   │   └── Column access methods
│   ├── ResultSet<T> (Mapped Results)
│   │   ├── POJO result mapping
│   │   ├── Custom mapper integration
│   │   └── Type-safe access
│   ├── AsyncResultSet<T>
│   │   ├── fetchNextPage() - async pagination
│   │   ├── hasMorePages() - pagination check
│   │   └── Stream processing support
│   └── Batch Operations
│       ├── BatchedArguments - batch parameter sets
│       ├── executeBatch() - batch execution
│       ├── executeBatchAsync() - async batch
│       └── Batch result handling
└── SQL Execution Patterns
    ├── Synchronous Execution
    │   ├── execute() - immediate execution
    │   ├── Parameter binding
    │   └── Result processing
    ├── Asynchronous Execution
    │   ├── executeAsync() - non-blocking execution
    │   ├── CompletableFuture integration
    │   └── Async result handling
    ├── Batch Execution
    │   ├── DML batch operations
    │   ├── Performance optimization
    │   └── Error handling
    └── Transaction Integration
        ├── Transactional execution
        ├── Isolation level support
        └── Rollback handling
```

**Key Characteristics:**

- Dual access paradigms (OO and relational)
- Comprehensive async support
- Type-safe operations
- Enterprise-grade batch processing

---

### IV. Transaction Management Layer

ACID transaction support with flexible programming models.

```
Transaction API
├── IgniteTransactions (Transaction Management)
│   ├── begin() - explicit transaction start
│   ├── begin(options) - configured transaction start
│   ├── beginAsync() - async transaction start
│   ├── beginAsync(options) - async configured start
│   ├── runInTransaction() - functional transaction
│   │   ├── Automatic transaction management
│   │   ├── Exception handling
│   │   └── Rollback on error
│   └── runInTransactionAsync() - async functional
│       ├── CompletableFuture integration
│       ├── Async exception handling
│       └── Async rollback
├── Transaction Context
│   ├── Transaction Interface
│   │   ├── commit() - transaction commit
│   │   ├── commitAsync() - async commit
│   │   ├── rollback() - transaction rollback
│   │   ├── rollbackAsync() - async rollback
│   │   ├── isReadOnly() - read-only check
│   │   └── timeout() - timeout configuration
│   ├── TransactionOptions
│   │   ├── readOnly() - read-only transactions
│   │   ├── timeout() - transaction timeout
│   │   ├── label() - transaction labeling
│   │   └── isolation() - isolation level
│   ├── Isolation Levels
│   │   ├── READ_COMMITTED - default isolation
│   │   ├── REPEATABLE_READ - higher consistency
│   │   └── SERIALIZABLE - strictest isolation
│   └── Transaction States
│       ├── ACTIVE - transaction in progress
│       ├── COMMITTED - successfully committed
│       ├── ABORTED - rolled back
│       └── PREPARING - in commit phase
└── Integration Patterns
    ├── Table API + Transactions
    │   ├── KeyValueView operations
    │   ├── RecordView operations
    │   └── Bulk operations
    ├── SQL API + Transactions
    │   ├── DML operations
    │   ├── Query operations
    │   └── Batch operations
    ├── Mixed Operations
    │   ├── Table + SQL in same transaction
    │   ├── Cross-table operations
    │   └── Distributed transactions
    └── Error Handling & Rollback
        ├── Exception-based rollback
        ├── Timeout handling
        ├── Deadlock detection
        └── Recovery strategies
```

**Key Characteristics:**

- ACID compliance
- Multiple isolation levels
- Functional and imperative styles
- Comprehensive error handling

---

### V. Compute & Processing Layer

Distributed computation with dynamic code deployment.

```
Compute API
├── IgniteCompute (Job Execution)
│   ├── execute() - synchronous job execution
│   │   ├── Single job execution
│   │   ├── Result collection
│   │   └── Error handling
│   ├── executeAsync() - asynchronous job execution
│   │   ├── CompletableFuture results
│   │   ├── Non-blocking execution
│   │   └── Async error handling
│   ├── submit() - job submission
│   │   ├── Job queuing
│   │   ├── Future-based results
│   │   └── Cancellation support
│   └── broadcast() - broadcast execution
│       ├── All-node execution
│       ├── Result aggregation
│       └── Failure handling
├── Job Definition
│   ├── ComputeJob<T,R>
│   │   ├── executeAsync() - job implementation
│   │   ├── JobExecutionContext - execution context
│   │   ├── Input/Output typing
│   │   └── Error propagation
│   ├── JobDescriptor
│   │   ├── builder() - job configuration
│   │   ├── jobClass() - job implementation class
│   │   ├── units() - deployment units
│   │   ├── options() - execution options
│   │   └── build() - descriptor creation
│   ├── JobTarget
│   │   ├── anyNode() - any available node
│   │   ├── nodes() - specific nodes
│   │   ├── colocated() - data-colocated execution
│   │   └── filter() - node filtering
│   └── DeploymentUnit
│       ├── name() - unit name
│       ├── version() - version management
│       ├── artifacts() - code artifacts
│       └── dependencies() - dependency management
├── Job Management
│   ├── Job Lifecycle
│   │   ├── Job submission
│   │   ├── Execution scheduling
│   │   ├── Progress monitoring
│   │   └── Completion handling
│   ├── Result Collection
│   │   ├── Single job results
│   │   ├── Batch job results
│   │   ├── Streaming results
│   │   └── Error aggregation
│   ├── Error Handling
│   │   ├── Job failures
│   │   ├── Node failures
│   │   ├── Timeout handling
│   │   └── Retry strategies
│   └── Cancellation Support
│       ├── Job cancellation
│       ├── Graceful shutdown
│       └── Resource cleanup
└── Advanced Patterns
    ├── MapReduce Style Operations
    │   ├── Map phase distribution
    │   ├── Reduce phase aggregation
    │   ├── Intermediate result handling
    │   └── Fault tolerance
    ├── Parallel Job Execution
    │   ├── Concurrent job submission
    │   ├── Load balancing
    │   ├── Resource management
    │   └── Result synchronization
    ├── Result Aggregation
    │   ├── Custom aggregation logic
    │   ├── Streaming aggregation
    │   ├── Fault-tolerant collection
    │   └── Memory management
    └── Dynamic Code Deployment
        ├── Runtime class loading
        ├── Version management
        ├── Dependency resolution
        └── Hot deployment
```

**Key Characteristics:**

- Dynamic code deployment
- Fault-tolerant execution
- Flexible targeting strategies
- Enterprise-grade job management

---

### VI. Streaming & Performance Layer

High-performance data ingestion and optimization features.

```
Data Streaming API
├── DataStreamer Operations
│   ├── streamData() - high-throughput ingestion
│   │   ├── Reactive streams integration
│   │   ├── Backpressure handling
│   │   ├── Flow control
│   │   └── Completion signaling
│   ├── DataStreamerOptions
│   │   ├── pageSize() - batch size configuration
│   │   ├── perPartitionParallelOperations() - parallelism
│   │   ├── autoFlushInterval() - automatic flushing
│   │   ├── retryLimit() - retry configuration
│   │   └── receiver() - custom processing
│   ├── DataStreamerItem<T>
│   │   ├── of() - item creation
│   │   ├── operation() - operation type
│   │   ├── payload() - data payload
│   │   └── partition() - partition hint
│   └── Performance Tuning
│       ├── Batch size optimization
│       ├── Parallelism configuration
│       ├── Memory management
│       └── Network optimization
├── Streaming Patterns
│   ├── Reactive Streams Integration
│   │   ├── Publisher/Subscriber model
│   │   ├── Flow control
│   │   ├── Demand signaling
│   │   └── Error propagation
│   ├── Backpressure Handling
│   │   ├── Flow control mechanisms
│   │   ├── Buffer management
│   │   ├── Rate limiting
│   │   └── Memory protection
│   ├── Error Recovery
│   │   ├── Retry strategies
│   │   ├── Dead letter handling
│   │   ├── Partial failure recovery
│   │   └── State consistency
│   └── Batch Processing
│       ├── Micro-batching
│       ├── Adaptive batching
│       ├── Compression
│       └── Checkpointing
└── Performance Features
    ├── Partition Awareness
    │   ├── Intelligent routing
    │   ├── Local processing
    │   ├── Network optimization
    │   └── Load balancing
    ├── Connection Pooling
    │   ├── Connection reuse
    │   ├── Pool sizing
    │   ├── Health monitoring
    │   └── Failover support
    ├── Async Operations
    │   ├── Non-blocking I/O
    │   ├── CompletableFuture integration
    │   ├── Event-driven processing
    │   └── Resource efficiency
    └── Bulk Operations
        ├── Batch API optimization
        ├── Vectorized operations
        ├── Memory efficiency
        └── Network batching
```

**Key Characteristics:**

- Reactive streams support
- Enterprise-grade performance
- Adaptive optimization
- Resource-efficient processing

---

### VII. Integration & Configuration Layer

Framework integration and enterprise configuration management.

```
Client Configuration
├── Connection Settings
│   ├── addresses() - server endpoints
│   ├── addressFinder() - dynamic discovery
│   ├── connectTimeout() - connection timeout
│   ├── backgroundReconnectInterval() - reconnection
│   ├── heartbeatInterval() - keepalive
│   ├── heartbeatTimeout() - health check timeout
│   └── operationTimeout() - operation timeout
├── Authentication
│   ├── BasicAuthenticator
│   │   ├── username() - basic auth username
│   │   └── password() - basic auth password
│   ├── Custom Authenticators
│   │   ├── IgniteClientAuthenticator interface
│   │   ├── Custom authentication logic
│   │   └── Token-based authentication
│   └── Security Integration
│       ├── LDAP integration
│       ├── Kerberos support
│       └── OAuth integration
├── Retry Policies
│   ├── RetryReadPolicy - read operation retries
│   ├── RetryLimitPolicy - limited retry attempts
│   ├── Custom retry policies
│   └── Circuit breaker patterns
├── Performance Tuning
│   ├── asyncContinuationExecutor() - async execution
│   ├── Connection pooling
│   ├── Request batching
│   └── Caching strategies
└── Security Configuration
    ├── SSL/TLS Configuration
    │   ├── SslConfiguration
    │   ├── Certificate management
    │   ├── Cipher suite selection
    │   └── Protocol version
    ├── Trust store management
    ├── Key store management
    └── Mutual authentication

Framework Integration
├── Spring Integration
│   ├── spring-boot-ignite-client-autoconfigure
│   │   ├── Auto-configuration classes
│   │   ├── Conditional bean creation
│   │   ├── Property binding
│   │   └── Health indicators
│   ├── spring-boot-starter-ignite-client
│   │   ├── Dependency management
│   │   ├── Default configurations
│   │   └── Quick setup
│   ├── spring-data-ignite
│   │   ├── Repository abstractions
│   │   ├── Query derivation
│   │   ├── Custom repository implementations
│   │   └── Transaction management
│   └── Auto-configuration Support
│       ├── IgniteClient bean creation
│       ├── Configuration property binding
│       ├── Metrics integration
│       └── Actuator endpoints
├── JDBC Integration
│   ├── Standard JDBC Driver
│   │   ├── jdbc:ignite:thin:// URL format
│   │   ├── DriverManager integration
│   │   ├── DataSource support
│   │   └── Connection pooling
│   ├── Connection Management
│   │   ├── Connection lifecycle
│   │   ├── Statement caching
│   │   ├── Result set handling
│   │   └── Metadata support
│   └── SQL Compatibility
│       ├── ANSI SQL support
│       ├── JDBC API compliance
│       ├── DatabaseMetaData
│       └── Prepared statement support
└── Platform Clients
    ├── .NET Client (C#)
    │   ├── Apache.Ignite NuGet package
    │   ├── Async/await support
    │   ├── LINQ integration
    │   └── .NET Core compatibility
    ├── C++ Client
    │   ├── Native C++ implementation
    │   ├── Cross-platform support
    │   ├── STL integration
    │   └── Memory management
    └── Python Client
        ├── DBAPI 2.0 compliance
        ├── Async support
        ├── Pandas integration
        └── SQLAlchemy support
```

**Key Characteristics:**

- Comprehensive framework integration
- Enterprise security features
- Multi-platform support
- Standards compliance

---

### VIII. Monitoring & Operations Layer

Production-ready monitoring, error handling, and resource management.

```
Metrics & Monitoring
├── JMX Metrics Integration
│   ├── MBean registration
│   ├── Standard metrics exposure
│   ├── Custom metrics support
│   └── Management console integration
├── Connection Monitoring
│   ├── Active connection tracking
│   ├── Connection health monitoring
│   ├── Reconnection statistics
│   └── Failover metrics
├── Performance Metrics
│   ├── Operation latency
│   ├── Throughput metrics
│   ├── Resource utilization
│   └── Cache hit ratios
└── Health Checks
    ├── Client health status
    ├── Server connectivity
    ├── Operation success rates
    └── Resource availability

Error Handling
├── Exception Hierarchy
│   ├── IgniteException - base exception
│   ├── IgniteClientException - client-specific errors
│   ├── SqlException - SQL operation errors
│   ├── TransactionException - transaction errors
│   └── ComputeException - compute operation errors
├── Retry Strategies
│   ├── Exponential backoff
│   ├── Linear retry
│   ├── Custom retry logic
│   └── Circuit breaker patterns
├── Circuit Breaker Patterns
│   ├── Failure threshold configuration
│   ├── Recovery time windows
│   ├── Fallback mechanisms
│   └── Health monitoring
└── Timeout Management
    ├── Operation timeouts
    ├── Connection timeouts
    ├── Transaction timeouts
    └── Compute job timeouts

Resource Management
├── AutoCloseable Support
│   ├── Client lifecycle management
│   ├── ResultSet resource cleanup
│   ├── Transaction resource cleanup
│   └── Stream resource management
├── Connection Lifecycle
│   ├── Connection establishment
│   ├── Connection pooling
│   ├── Connection health monitoring
│   └── Connection cleanup
├── Memory Management
│   ├── Result set streaming
│   ├── Large object handling
│   ├── Memory pressure detection
│   └── Garbage collection optimization
└── Cleanup Patterns
    ├── Try-with-resources patterns
    ├── Explicit resource cleanup
    ├── Async resource cleanup
    └── Error recovery cleanup
```

**Key Characteristics:**

- Production-ready monitoring
- Comprehensive error handling
- Automated resource management
- Enterprise operations support

---

## Cross-Layer Interactions

### Schema-to-Data Flow

```
Annotations (@Table, @Column, @Id) 
    ↓ [Annotation Processing]
DDL Generation (CREATE TABLE statements)
    ↓ [Catalog API]
Table Creation (IgniteCatalog.createTable())
    ↓ [Table API Integration]
POJO Mapping (Mapper<T> generation)
    ↓ [Data Access]
KeyValueView<K,V> / RecordView<T> operations
```

### Transaction Integration Flow

```
Transaction Context (IgniteTransactions.begin())
    ↓ [Context Propagation]
Table API Operations (with transaction parameter)
    ↓ [Same Transaction]
SQL API Operations (with transaction parameter)
    ↓ [Commit/Rollback]
Transaction Resolution (commit() or rollback())
```

### Async Operation Flow

```
Async API Call (xxxAsync() methods)
    ↓ [CompletableFuture]
Async Continuation Executor
    ↓ [Result Processing]
Completion Handlers (.thenApply(), .thenCompose())
    ↓ [Error Handling]
Exception Handlers (.exceptionally(), .handle())
```

## Design Principles

1. **Async-First**: All operations support both synchronous and asynchronous execution
2. **Type Safety**: Extensive use of generics and compile-time type checking
3. **Resource Management**: AutoCloseable interfaces and try-with-resources support
4. **Builder Patterns**: Fluent configuration APIs throughout
5. **Fail-Fast**: Early validation and detailed error reporting
6. **Performance**: Built-in optimizations for enterprise workloads
7. **Standards Compliance**: JDBC, JMX, and other standard integrations
8. **Extensibility**: Plugin architectures and custom implementations

## Integration Patterns

### Layered Integration

- **Schema Definition** (Layer II) drives **Data Access** (Layer III)
- **Transaction Management** (Layer IV) spans **Data Access** (Layer III)
- **Monitoring** (Layer VIII) observes all operational layers

### Cross-Cutting Concerns

- **Async Support**: Implemented across all operational layers
- **Error Handling**: Consistent patterns across all APIs
- **Resource Management**: Uniform cleanup across all components
- **Security**: Authentication and authorization across all access points

This architecture provides a solid foundation for understanding and working with the Apache Ignite 3 Java API ecosystem.
