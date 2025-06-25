# Apache Ignite 3 Architecture

## Architectural overview of Apache Ignite 3 distributed database system

This document examines Apache Ignite 3's architecture from design principles through implementation patterns.

## Introduction

Apache Ignite 3 implements a distributed database architecture designed for high-performance computing and analytics workloads. The system provides horizontal scalability, ACID transactions, and multi-platform client support through a consensus-based distributed design.

### Design Philosophy

Apache Ignite 3 is built on several foundational principles:

- **Schema-First Design**: All data operations are schema-driven, ensuring consistency between DDL definitions, internal models, and physical storage
- **Modular Architecture**: Clear separation of concerns with pluggable components and well-defined interfaces
- **Distributed-by-Design**: Built for horizontal scalability with partitioning and rebalancing
- **Multi-Platform Native**: Support for Java, .NET, C++, and Python with feature parity
- **Operational Management**: Unified management through CLI tools and REST APIs with cluster coordination

### Key Capabilities

Apache Ignite 3 provides a unified platform for multiple use cases:

- **ACID Transactions**: Distributed transactions with serializable isolation and hybrid logical clock coordination
- **Distributed SQL**: Standard SQL with JDBC/ODBC support and cost-based query optimization
- **Compute API**: Distributed computing with co-location support and multi-language execution
- **Real-time Analytics**: In-memory processing with persistent storage options
- **Multi-Model Storage**: Key-value and SQL access patterns over the same data

## High-Level Architecture

> [!NOTE]
> **Architecture Overview**: Apache Ignite 3's shared-nothing distributed design means every node is a full participant in the cluster, handling data, compute, and SQL operations. This eliminates single points of failure while enabling linear scalability.

### System Topology

Apache Ignite 3 operates as a shared-nothing distributed system where each node participates in cluster coordination, data storage, and query processing.

```mermaid
graph TB
    subgraph CT ["Cluster Topology"]
        subgraph DN ["Data Nodes"]
            N1[Node 1<br/>Data + Compute + SQL]
            N2[Node 2<br/>Data + Compute + SQL]
            N3[Node 3<br/>Data + Compute + SQL]
            N4[Node 4<br/>Data + Compute + SQL]
        end
        
        subgraph CA ["Client Applications"]
            APP1[Java Application]
            APP2[.NET Application]
            APP3[Python Analytics]
            APP4[C++ Service]
            WEB[Web Dashboard]
        end
        
        subgraph ES ["External Systems"]
            JDBC[JDBC Tools]
            ODBC[ODBC Applications]
            REST[REST Clients]
        end
    end
    
    subgraph CS ["Cluster Services"]
        CMG[Cluster Management Group]
        MS[Metastorage Service]
        PD[Placement Driver]
        CFG[Configuration Service]
    end
    
    APP1 --> N1
    APP2 --> N2
    APP3 --> N3
    APP4 --> N4
    WEB --> REST
    JDBC --> N1
    ODBC --> N2
    REST --> N3
    
    N1 --- N2
    N2 --- N3
    N3 --- N4
    N4 --- N1
    N1 --- N3
    N2 --- N4
    
    CMG -.-> N1
    CMG -.-> N2
    CMG -.-> N3
    CMG -.-> N4
    
    MS -.-> CMG
    PD -.-> MS
    CFG -.-> MS
```

Apache Ignite 3 uses a shared-nothing architecture where every node provides data storage, compute processing, and SQL capabilities. This design eliminates single points of failure and enables linear scaling. Client applications can connect to any cluster node for request processing.

Cluster coordination occurs through specialized services: the Cluster Management Group manages node membership, Metastorage maintains cluster metadata, and the Placement Driver coordinates partition leadership. This separation between data operations and coordination enables consistent performance while maintaining strong consistency guarantees.

### Data Distribution Model

Data in Apache Ignite 3 is automatically partitioned across cluster nodes with configurable replication factors and placement strategies.

```mermaid
graph TB
    subgraph "Logical Data Model"
        T1[Table: Users<br/>Partitions: 1024]
        T2[Table: Orders<br/>Partitions: 1024]
        T3[Table: Products<br/>Partitions: 512]
    end
    
    subgraph "Physical Distribution"
        subgraph "Node 1"
            P1_1[Users Partitions<br/>1-342]
            P2_1[Orders Partitions<br/>1-342]
            P3_1[Products Partitions<br/>1-171]
        end
        
        subgraph "Node 2"
            P1_2[Users Partitions<br/>343-684]
            P2_2[Orders Partitions<br/>343-684]
            P3_2[Products Partitions<br/>172-342]
        end
        
        subgraph "Node 3"
            P1_3[Users Partitions<br/>685-1024]
            P2_3[Orders Partitions<br/>685-1024]
            P3_3[Products Partitions<br/>343-512]
        end
    end
    
    T1 --> P1_1
    T1 --> P1_2
    T1 --> P1_3
    
    T2 --> P2_1
    T2 --> P2_2
    T2 --> P2_3
    
    T3 --> P3_1
    T3 --> P3_2
    T3 --> P3_3
```

Apache Ignite 3 partitions tables across cluster nodes using configurable partition counts. Each table splits into multiple partitions distributed evenly across available nodes. The system maintains configurable replica counts for fault tolerance.

Partition distribution follows the Rendezvous (Highest Random Weight) algorithm for consistent placement without coordination overhead. When nodes join or leave the cluster, the Placement Driver triggers rebalancing to maintain optimal distribution and replica counts.

This partitioning approach eliminates single points of failure while enabling parallel processing across the cluster. Failed nodes trigger automatic failover to replica partitions without data loss or service interruption.

### Service Architecture Overview

The system is organized into distinct service layers with clear responsibilities and interfaces.

```mermaid
graph TB
    subgraph "Client Interfaces"
        API[Public APIs<br/>SQL, Key-Value, Compute]
        PROTO[Binary Protocol<br/>Multi-platform]
        HTTP[REST API<br/>Management & Data]
    end
    
    subgraph "Service Layer"
        QP[Query Processor<br/>SQL Engine + Calcite]
        TM[Table Manager<br/>DDL & Schema]
        CM[Compute Manager<br/>Jobs & Colocation]
        TX[Transaction Manager<br/>ACID Coordination]
    end
    
    subgraph "Data Management Layer"
        CAT[Catalog Manager<br/>Schema Metadata]
        IDX[Index Manager<br/>Hash & Sorted Indexes]
        RM[Replica Manager<br/>Partition Coordination]
        SM[Storage Manager<br/>MVCC & Persistence]
    end
    
    subgraph "Infrastructure Layer"
        NET[Network Service<br/>Cluster Communication]
        RAFT[Raft Service<br/>Consensus Groups]
        VAULT[Vault Service<br/>Local Storage]
        LIFE[Lifecycle Manager<br/>Component Coordination]
    end
    
    API --> QP
    API --> TM
    API --> CM
    PROTO --> API
    HTTP --> API
    
    QP --> TX
    TM --> TX
    CM --> TX
    
    TX --> CAT
    TX --> IDX
    TX --> RM
    TX --> SM
    
    CAT --> RAFT
    RM --> RAFT
    SM --> VAULT
    
    All --> NET
    All --> LIFE
```

Apache Ignite 3 organizes services into distinct layers with clear responsibilities. The Infrastructure Layer provides networking, consensus protocols, and lifecycle management. The Data Management Layer handles schema metadata, indexing, and replica coordination.

The Service Layer processes user operations through specialized managers: the SQL Query Processor handles distributed query execution, the Table Manager manages DDL operations, and the Compute Manager coordinates distributed processing. The Transaction Manager ensures ACID properties across all distributed operations.

The Client Interfaces layer supports multiple access patterns through SQL, key-value, and compute APIs. This layer provides protocol translation between external clients and internal services while maintaining consistent semantics across all access methods.

## Node Architecture

### Node Lifecycle and Bootstrap

Each Apache Ignite 3 node follows a carefully orchestrated startup sequence that establishes cluster membership, initializes services, and begins participating in distributed operations.

```mermaid
sequenceDiagram
    participant Node as Starting Node
    participant CMG as Cluster Management Group
    participant MS as Metastorage
    participant PD as Placement Driver
    participant Services as Local Services
    
    Note over Node: Node Startup
    Node->>Node: Initialize Core Components
    Node->>CMG: Request Cluster Join
    CMG->>CMG: Validate Node Attributes
    CMG-->>Node: Join Approved
    
    Node->>MS: Initialize Metastorage Client
    MS-->>Node: Configuration & Topology
    
    Node->>Services: Start Local Services
    Services->>Services: Initialize Storage Engines
    Services->>Services: Start Table Managers
    Services->>Services: Initialize SQL Engine
    
    Node->>PD: Register for Partition Assignments
    PD->>MS: Update Node Status
    MS->>CMG: Propagate Topology Change
    
    Note over Node: Node Ready for Operations
```

Node startup follows a coordinated sequence to ensure cluster stability. Nodes first initialize core components and establish network connectivity. The Cluster Management Group validates node credentials and configuration before approving cluster membership.

Accepted nodes connect to Metastorage to receive current cluster configuration and topology information. Service initialization follows dependency order: storage engines initialize first, followed by table managers and SQL processing components.

The Placement Driver assigns partition responsibilities based on current cluster load and node capacity. Nodes begin serving client requests only after completing the full initialization sequence and receiving partition assignments.

### Node Internal Architecture

Each node contains all components necessary for distributed database operations, organized into functional layers.

```mermaid
graph TB
    subgraph "Single Node Architecture"
        subgraph "Client Interface Layer"
            CH[Client Handler<br/>TCP Server]
            REST[REST Handler<br/>HTTP Server]
            JMX[JMX Interface<br/>Management]
        end
        
        subgraph "Service Layer"
            SQL[SQL Query Processor]
            COMPUTE[Compute Engine]
            TABLE[Table Manager]
            TX_MGR[Transaction Manager]
        end
        
        subgraph "Data Layer"
            CATALOG[Catalog Manager]
            SCHEMA[Schema Manager]
            INDEX[Index Manager]
            REPLICA[Replica Manager]
        end
        
        subgraph "Storage Layer"
            MVCC[MVCC Engine]
            ROCKS[RocksDB Engine]
            PAGEMEM[Page Memory Engine]
            GC[Garbage Collector]
        end
        
        subgraph "Consensus Layer"
            RAFT_MGR[Raft Manager]
            META[Metastorage Client]
            PD_CLIENT[Placement Driver Client]
        end
        
        subgraph "Infrastructure Layer"
            NET[Network Service]
            CONFIG[Configuration Manager]
            VAULT[Local Vault]
            LIFECYCLE[Lifecycle Manager]
        end
    end
    
    CH --> SQL
    CH --> TABLE
    CH --> COMPUTE
    REST --> TABLE
    REST --> CONFIG
    
    SQL --> TX_MGR
    TABLE --> TX_MGR
    COMPUTE --> TX_MGR
    
    TX_MGR --> CATALOG
    TX_MGR --> REPLICA
    
    CATALOG --> RAFT_MGR
    REPLICA --> RAFT_MGR
    REPLICA --> MVCC
    
    MVCC --> ROCKS
    MVCC --> PAGEMEM
    MVCC --> GC
    
    RAFT_MGR --> META
    RAFT_MGR --> PD_CLIENT
    
    META --> NET
    CONFIG --> VAULT
    
    LIFECYCLE -.-> ALL
```

Each Apache Ignite 3 node contains all components necessary for distributed database operations. The layered architecture separates concerns while enabling collaboration across the cluster.

The Client Interface Layer handles connections through TCP servers, HTTP endpoints, and JMX interfaces. This layer implements multiple protocols including binary protocol for high-performance clients and REST for management operations.

The Service Layer processes core operations through the SQL engine for query execution, compute engine for distributed jobs, and table manager for schema operations. The Transaction Manager coordinates ACID properties across all service operations.

The Data Layer manages schema metadata through the catalog manager, maintains indexes for query performance, and coordinates replica consistency. This layer translates high-level operations into storage-level operations.

The Storage Layer implements MVCC for transaction isolation and provides pluggable storage engines. Available engines include RocksDB for persistent storage and PageMemory for high-performance operations. Garbage collection maintains system performance by cleaning obsolete data versions.

The Consensus Layer maintains cluster coordination through Raft protocols, while the Infrastructure Layer provides networking, configuration, and lifecycle management. The Lifecycle Manager coordinates startup and shutdown sequences across all components.

## Distributed System Fundamentals

### Cluster Formation and Membership

Apache Ignite 3 uses a hybrid approach to cluster formation, combining gossip-based discovery with consensus-based membership management.

```mermaid
graph TB
    subgraph "Cluster Formation Process"
        subgraph "Phase 1: Discovery"
            DISCOVER[Node Discovery<br/>ScaleCube Gossip]
            SEED[Seed Nodes<br/>Bootstrap List]
        end
        
        subgraph "Phase 2: Membership"
            CMG_FORM[CMG Formation<br/>Initial Nodes]
            VALIDATE[Node Validation<br/>Attributes & Config]
        end
        
        subgraph "Phase 3: Initialization"
            META_INIT[Metastorage Init<br/>Cluster Metadata]
            CONFIG_DIST[Config Distribution<br/>Cluster Settings]
        end
        
        subgraph "Phase 4: Service Start"
            TABLE_INIT[Table Services<br/>DDL Processing]
            PART_ASSIGN[Partition Assignment<br/>Data Distribution]
        end
    end
    
    SEED --> DISCOVER
    DISCOVER --> CMG_FORM
    CMG_FORM --> VALIDATE
    VALIDATE --> META_INIT
    META_INIT --> CONFIG_DIST
    CONFIG_DIST --> TABLE_INIT
    TABLE_INIT --> PART_ASSIGN
```

Apache Ignite 3 cluster formation follows a four-phase process ensuring stable operation before serving client requests.

Phase 1 uses ScaleCube gossip protocol for node discovery from configured seed addresses. Nodes establish network connectivity and identify potential cluster members.

Phase 2 forms the Cluster Management Group through credential validation and consensus establishment. This prevents unauthorized nodes from participating in cluster decisions.

Phase 3 initializes Metastorage for distributed metadata storage and distributes cluster configuration to all nodes. This ensures consistent operational parameters across the cluster.

Phase 4 activates table services and distributes partition assignments. Only after completing all phases do nodes begin serving client requests and processing data operations.

### Consensus and Coordination

The system uses multiple Raft groups for different coordination concerns, ensuring both performance and strong consistency.

```mermaid
graph TB
    subgraph "Consensus Architecture"
        subgraph "Cluster Management Group (CMG)"
            CMG_L[CMG Leader]
            CMG_F1[CMG Follower 1]
            CMG_F2[CMG Follower 2]
        end
        
        subgraph "Metastorage Group"
            MS_L[Metastorage Leader]
            MS_F1[Metastorage Follower 1]
            MS_F2[Metastorage Follower 2]
            MS_L1[Metastorage Learner 1]
            MS_L2[Metastorage Learner 2]
        end
        
        subgraph "Placement Driver Group"
            PD_L[Placement Driver Leader]
            PD_F1[Placement Driver Follower 1]
            PD_F2[Placement Driver Follower 2]
        end
        
        subgraph "Data Partition Groups"
            DP1[Partition 1 Group]
            DP2[Partition 2 Group]
            DP3[Partition N Group]
        end
    end
    
    CMG_L --> CMG_F1
    CMG_L --> CMG_F2
    
    MS_L --> MS_F1
    MS_L --> MS_F2
    MS_L -.-> MS_L1
    MS_L -.-> MS_L2
    
    PD_L --> PD_F1
    PD_L --> PD_F2
    
    CMG_L --> MS_L
    MS_L --> PD_L
    PD_L -.-> DP1
    PD_L -.-> DP2
    PD_L -.-> DP3
```

Apache Ignite 3 uses multiple Raft consensus groups for different coordination responsibilities, ensuring both performance and consistency.

The Cluster Management Group manages cluster membership through a three-node configuration. This group handles critical decisions about node joining and leaving the cluster.

The Metastorage Group maintains cluster metadata using a five-node configuration with three voting members and two learners. This provides high availability for metadata operations while enabling geographic distribution.

The Placement Driver Group coordinates partition placement decisions through a three-node configuration. This group makes real-time decisions about data distribution and rebalancing.

Data Partition Groups operate independently with three-node configurations per partition. This enables parallel processing of partition-specific operations without centralized coordination overhead.

This multi-group approach isolates different consensus concerns, preventing metadata operations from interfering with data operations while maintaining strong consistency across all system components.

### Transaction Processing Architecture

Apache Ignite 3 implements distributed ACID transactions using hybrid logical clocks for ordering and MVCC for concurrency control.

```mermaid
graph TB
    subgraph "Transaction Lifecycle"
        subgraph "Transaction Coordination"
            TC[Transaction Coordinator]
            TM[Transaction Manager]
            CC[Commit Coordinator]
        end
        
        subgraph "Concurrency Control"
            HLC[Hybrid Logical Clock]
            MVCC_ENGINE[MVCC Engine]
            LOCK[Lock Manager]
        end
        
        subgraph "Commit Process"
            CP[Commit Processor]
            LOG[Transaction Log]
            RECOVERY[Recovery Manager]
        end
        
        subgraph "Participants"
            P1[Partition 1<br/>Primary Replica]
            P2[Partition 2<br/>Primary Replica]
            P3[Partition N<br/>Primary Replica]
        end
    end
    
    TC --> TM
    TM --> HLC
    TM --> MVCC_ENGINE
    
    TC --> CC
    CC --> CP
    CP --> LOG
    
    TC -.-> P1
    TC -.-> P2
    TC -.-> P3
    
    MVCC_ENGINE --> LOCK
    LOG --> RECOVERY
```

Apache Ignite 3 implements distributed ACID transactions using hybrid logical clocks for ordering and MVCC for concurrency control.

The Transaction Coordinator manages transaction lifecycle across multiple partitions. For distributed transactions, the coordinator ensures all participants reach the same outcome through two-phase commit protocol.

Concurrency control uses Hybrid Logical Clock for consistent event ordering across nodes. The MVCC engine enables multiple concurrent transactions without blocking reads, while the Lock Manager prevents write conflicts during commit processing.

The Commit Process implements distributed durability through the Transaction Log and Recovery Manager. The Commit Processor coordinates the two-phase commit across all partition participants, ensuring atomicity for distributed operations.

Primary replicas execute transaction operations under coordinator supervision. This approach maintains ACID properties across distributed partitions while enabling high-throughput concurrent processing.

### Data Replication and Consistency

The system maintains strong consistency through Raft-based replication at the partition level with coordinated leadership management.

```mermaid
sequenceDiagram
    participant Client
    participant PD as Placement Driver
    participant Primary as Primary Replica
    participant Secondary1 as Secondary Replica 1
    participant Secondary2 as Secondary Replica 2
    
    Client->>PD: Locate Partition Leader
    PD-->>Client: Primary Replica Address
    
    Client->>Primary: Write Operation
    Primary->>Primary: Acquire Local Lock
    Primary->>Secondary1: Replicate Log Entry
    Primary->>Secondary2: Replicate Log Entry
    
    Secondary1-->>Primary: Ack
    Secondary2-->>Primary: Ack
    
    Primary->>Primary: Commit to Storage
    Primary-->>Client: Operation Success
    
    Primary->>Secondary1: Commit Notification
    Primary->>Secondary2: Commit Notification
```

Apache Ignite 3 maintains strong consistency through Raft-based replication at the partition level with coordinated leadership management.

Clients locate the primary replica for write operations through the Placement Driver's leadership information. This routing ensures writes reach the authoritative node without unnecessary network hops.

The primary replica coordinates consensus through the Raft protocol. Write operations acquire local locks, then replicate to secondary replicas before committing. The primary waits for majority acknowledgment before confirming operation success to the client.

This approach provides strong consistency guarantees while maintaining performance through direct primary access. Failed replicas trigger automatic leadership changes through the Placement Driver's lease management system.

## Component Architecture

### Core Component Lifecycle

All major components implement the `IgniteComponent` interface, providing standardized lifecycle management with dependency ordering and graceful shutdown capabilities.

```mermaid
sequenceDiagram
    participant LM as Lifecycle Manager
    participant C1 as Component 1
    participant C2 as Component 2
    participant C3 as Component 3
    
    Note over LM: Startup Phase
    LM->>C1: startAsync()
    C1-->>LM: CompletableFuture<Void>
    LM->>C2: startAsync()
    C2-->>LM: CompletableFuture<Void>
    LM->>C3: startAsync()
    C3-->>LM: CompletableFuture<Void>
    
    Note over LM: Shutdown Phase
    LM->>C3: beforeNodeStop()
    LM->>C2: beforeNodeStop()
    LM->>C1: beforeNodeStop()
    LM->>C3: stopAsync()
    LM->>C2: stopAsync()
    LM->>C1: stopAsync()
```

This lifecycle sequence diagram demonstrates the controlled startup and shutdown process that ensures proper component initialization and cleanup. During startup, the Lifecycle Manager starts components asynchronously in dependency order, allowing each component to complete its initialization before dependent components begin their startup. The shutdown process follows the reverse order, first calling beforeNodeStop() on all components to signal impending shutdown, then calling stopAsync() to perform graceful termination. This two-phase shutdown approach allows components to complete in-flight operations and release resources properly, ensuring data integrity and clean cluster departure.

### Manager Pattern Implementation

The system uses a manager-based architecture where each major subsystem is controlled by a dedicated manager component:

- **TableManager**: Table lifecycle and DDL operations
- **CatalogManager**: Schema and metadata management
- **ConfigurationManager**: Distributed configuration
- **MetaStorageManager**: Cluster metadata storage
- **TxManager**: Transaction coordination
- **ReplicaManager**: Partition replica management

## Storage Architecture

### Multi-Version Concurrency Control (MVCC)

Apache Ignite 3 implements MVCC at the storage layer to provide snapshot isolation and non-blocking concurrent access.

```mermaid
graph LR
    subgraph "Version Chain"
        V3[Version N<br/>Timestamp: T3<br/>State: Committed]
        V2[Version N-1<br/>Timestamp: T2<br/>State: Committed]
        V1[Version N-2<br/>Timestamp: T1<br/>State: Aborted]
    end
    
    subgraph "Transaction Views"
        T_T4[Transaction @ T4<br/>Sees: V3]
        T_T2_5[Transaction @ T2.5<br/>Sees: V2]
        T_T0_5[Transaction @ T0.5<br/>Sees: None]
    end
    
    V3 --> V2
    V2 --> V1
    
    T_T4 -.-> V3
    T_T2_5 -.-> V2
    T_T0_5 -.-> V1
```

Apache Ignite 3's MVCC system works like a time machine for database records, allowing different transactions to see different versions of the same data based on when they started their work. This temporal magic enables remarkable concurrency without the performance penalties of traditional locking.

Imagine each database row as a filing cabinet where every change creates a new file folder, but the old folders are never thrown away immediately. Each folder is timestamped and marked with its status – committed, aborted, or still in progress. When a transaction needs to read data, it doesn't just grab the latest version; instead, it looks for the most recent version that was committed before the transaction began.

In our example, a transaction that starts at time T4 sees the world as it existed at T4, which means it reads the committed version from T3. A transaction that started earlier at T2.5 sees an older but consistent view of the world, reading the version committed at T2. A very early transaction at T0.5 sees nothing at all – because no committed versions existed yet when it started.

The beauty lies in what happens to failed transactions. The aborted version at T1 becomes invisible to everyone, as if it never existed. This isn't deletion – it's temporal isolation. The transaction that created that version failed, so its changes never become part of the timeline that other transactions can see.

This approach eliminates the traditional tradeoff between consistency and performance. Readers never block writers, writers never block readers, and yet every transaction sees a perfectly consistent snapshot of the database as it existed at a specific point in time.

### Pluggable Storage Engines

The storage abstraction allows multiple storage engine implementations with different performance and durability characteristics.

```mermaid
graph TB
    subgraph "Storage API"
        SE[StorageEngine]
        MTS[MvTableStorage]
        MPS[MvPartitionStorage]
    end
    
    subgraph "RocksDB Implementation"
        RSE[RocksDbStorageEngine]
        RTS[RocksDbMvTableStorage]
        RPS[RocksDbMvPartitionStorage]
    end
    
    subgraph "Page Memory Implementation"
        PSE[PageMemoryStorageEngine]
        PTS[PageMemoryMvTableStorage]
        PPS[PageMemoryMvPartitionStorage]
    end
    
    SE -.-> RSE
    SE -.-> PSE
    MTS -.-> RTS
    MTS -.-> PTS
    MPS -.-> RPS
    MPS -.-> PPS
    
    RSE --> RTS
    RTS --> RPS
    PSE --> PTS
    PTS --> PPS
```

Apache Ignite 3's pluggable storage architecture embodies the principle that one size does not fit all in data management. Like a carpenter who chooses different tools for different jobs, the system allows tables to use different storage engines based on their specific needs.

The Storage API layer serves as the universal tool interface – defining a common language that any storage engine must speak. Whether you're building a high-speed cache or a durable archive, the interface contract remains the same: you must be able to store tables, manage partitions, and handle versioned data. This abstraction is what makes the magic possible.

RocksDB steps forward as the heavy-duty option, like a industrial-grade safe. It excels at persistent storage with excellent compression, making it ideal for large datasets that must survive node restarts and system failures. Its LSM-tree architecture optimizes for write-heavy workloads while providing excellent read performance through clever indexing strategies.

Page Memory offers a different value proposition, operating like a high-speed vault with an optional safety deposit box. It keeps frequently accessed data in memory for blazing fast access while optionally persisting to disk for durability. This hybrid approach bridges the gap between pure in-memory speed and persistent reliability.

The genius of this design becomes apparent in mixed workloads. Your user session cache might live in Page Memory for instant access, while your transaction history resides in RocksDB for long-term retention. Both can exist in the same cluster, managed by the same APIs, but optimized for their specific use patterns. The system even allows hot data to automatically migrate between storage types as access patterns change over time.

## SQL Processing Pipeline

### Query Execution Flow

SQL queries undergo a multi-phase processing pipeline with distributed execution capabilities.

```mermaid
sequenceDiagram
    participant C as Client
    participant QP as Query Processor
    participant PP as Prepare Service
    participant EP as Execution Service
    participant SE as Storage Engine
    
    C->>QP: SQL Query
    QP->>QP: REGISTERED
    QP->>QP: PARSING
    QP->>PP: Parse & Optimize
    PP->>PP: Calcite Planning
    PP-->>QP: Query Plan
    QP->>QP: OPTIMIZING
    QP->>QP: CURSOR_INITIALIZATION
    QP->>EP: Execute Plan
    EP->>SE: Fragment Execution
    SE-->>EP: Data Batches
    EP-->>QP: Result Cursor
    QP->>QP: EXECUTING
    QP-->>C: Result Set
    QP->>QP: TERMINATED
```

Apache Ignite 3's SQL processing pipeline transforms human-readable queries into distributed execution masterpieces through a carefully orchestrated journey that would make any compiler writer proud.

The adventure begins when a client submits a SQL query – perhaps something as simple as "SELECT * FROM customers WHERE region = 'WEST'". The Query Processor doesn't immediately spring into action; instead, it methodically moves through a series of states that ensure nothing gets overlooked.

First comes REGISTRATION, where the query gets a unique identity and joins the queue of work to be done. Then PARSING kicks in, where the SQL text gets dissected into its grammatical components – tables, columns, conditions, and operations. This isn't just syntax checking; it's the first step toward understanding what the user actually wants.

The OPTIMIZING phase is where the real magic happens. The Prepare Service hands the parsed query to Apache Calcite, which acts like a master strategist, considering dozens of ways to execute the query efficiently. Should it use an index? Which tables should be joined first? How can the work be distributed across nodes? The optimizer evaluates these options and produces an execution plan that balances speed, resource usage, and network traffic.

CURSOR_INITIALIZATION prepares the execution machinery, setting up the distributed coordination needed to run the plan across multiple nodes. Then the Execution Service takes over, breaking the plan into fragments and coordinating their execution across the cluster's storage engines.

Data flows back in batches rather than all at once – a crucial design choice that prevents memory exhaustion on large result sets. The Query Processor assembles these batches into a cursor that the client can consume at its own pace. Finally, the TERMINATED state ensures that all resources are properly cleaned up, preventing memory leaks in long-running systems.

### Distributed Query Execution

Complex queries are broken into fragments that execute across cluster nodes with coordinated data exchange.

```mermaid
graph TB
    subgraph "Coordinator Node"
        QC[Query Coordinator]
        RF[Root Fragment]
    end
    
    subgraph "Node 1"
        IF1[Intermediate Fragment 1]
        LF1[Leaf Fragment 1]
    end
    
    subgraph "Node 2"
        IF2[Intermediate Fragment 2]
        LF2[Leaf Fragment 2]
    end
    
    subgraph "Node 3"
        LF3[Leaf Fragment 3]
    end
    
    QC --> RF
    RF --> IF1
    RF --> IF2
    IF1 --> LF1
    IF2 --> LF2
    RF --> LF3
    
    LF1 -.->|Data Exchange| IF1
    LF2 -.->|Data Exchange| IF2
    LF3 -.->|Data Exchange| RF
    IF1 -.->|Aggregated Data| RF
    IF2 -.->|Aggregated Data| RF
```

This distributed query execution diagram illustrates how Apache Ignite 3 breaks complex queries into fragments that execute across multiple cluster nodes. The Query Coordinator manages the overall execution from a single node, while the Root Fragment coordinates the final result aggregation. Intermediate Fragments on Node 1 and Node 2 perform operations like joins, grouping, or filtering on data from their local Leaf Fragments. Leaf Fragments execute on nodes containing the relevant data partitions, performing table scans and applying predicates. The dotted lines represent data exchange between fragments, with Leaf Fragments sending filtered data to their local Intermediate Fragments, which then aggregate and forward results to the Root Fragment. This architecture minimizes data movement while enabling parallel processing across the cluster.

## Distributed System Patterns

### Raft Consensus Implementation

Apache Ignite 3 uses Raft consensus for both metadata management and data replication with separate groups for different concerns.

```mermaid
graph TB
    subgraph "Metastorage Raft Group"
        MS_L[Leader]
        MS_F1[Follower 1]
        MS_F2[Follower 2]
    end
    
    subgraph "Partition Raft Groups"
        subgraph "Partition 1"
            P1_L[Leader]
            P1_F1[Follower 1]
            P1_F2[Follower 2]
        end
        
        subgraph "Partition 2"
            P2_L[Leader]
            P2_F1[Follower 1]
            P2_F2[Follower 2]
        end
    end
    
    subgraph "Placement Driver"
        PD[Placement Driver]
    end
    
    MS_L --> MS_F1
    MS_L --> MS_F2
    P1_L --> P1_F1
    P1_L --> P1_F2
    P2_L --> P2_F1
    P2_L --> P2_F2
    
    PD --> MS_L
    PD -.->|Lease Management| P1_L
    PD -.->|Lease Management| P2_L
```

This Raft consensus implementation diagram shows how Apache Ignite 3 organizes consensus groups for different system concerns. The Metastorage Raft Group handles cluster metadata with a three-node configuration ensuring high availability for critical cluster information. Each data partition has its own Raft group (Partition 1 and Partition 2 shown as examples) with independent leader election and replication, allowing for concurrent operations across different partitions. The Placement Driver coordinates with the Metastorage leader to make partition placement decisions and manages lease grants to partition leaders (shown by dotted arrows). This multi-group approach isolates different types of consensus operations, preventing metadata operations from interfering with data operations while maintaining strong consistency guarantees across all system components.

### Placement Driver Pattern

The Placement Driver provides centralized coordination for partition placement decisions while maintaining high availability through Raft-based leadership.

```mermaid
sequenceDiagram
    participant PD as Placement Driver
    participant MS as Metastorage
    participant R1 as Replica 1
    participant R2 as Replica 2
    participant R3 as Replica 3
    
    Note over PD: Assignment Change Detected
    PD->>MS: Read Current Assignments
    MS-->>PD: Assignment Data
    PD->>PD: Select Primary Replica
    PD->>MS: Grant Lease to R1
    MS->>R1: Lease Granted
    MS->>R2: Lease Info
    MS->>R3: Lease Info
    
    Note over R1: Lease Active Period
    R1->>PD: Keep-alive
    PD-->>R1: Lease Renewed
    
    Note over PD: Failure Detection
    PD->>MS: Revoke R1 Lease
    PD->>MS: Grant Lease to R2
    MS->>R2: Lease Granted
```

This Placement Driver sequence diagram demonstrates the lease-based coordination mechanism for managing partition leadership. The process begins when the Placement Driver detects an assignment change and reads current assignments from Metastorage. Based on availability and placement policies, the Placement Driver selects a primary replica and grants it a lease through Metastorage, which then notifies all replicas of the lease information. During normal operation, the primary replica sends keep-alive messages to maintain the lease. When failure detection occurs (missed keep-alives, network partitions, or explicit failure signals), the Placement Driver revokes the current lease and grants a new one to another replica, ensuring continuous availability while preventing split-brain scenarios through centralized lease management.

## Client Architecture

### Multi-Platform Client Support

Apache Ignite 3 provides native clients for multiple platforms with consistent protocol and feature support.

```mermaid
graph TB
    subgraph "Client Platforms"
        JC[Java Client<br/>- Async API<br/>- Partition Awareness<br/>- Connection Pooling]
        NC[.NET Client<br/>- Task-based Async<br/>- LINQ Integration<br/>- Strong Typing]
        CC[C++ Client<br/>- Modern C++17<br/>- RAII Patterns<br/>- ODBC Driver]
        PC[Python Client<br/>- DB-API 2.0<br/>- Context Managers<br/>- Type Mapping]
    end
    
    subgraph "Binary Protocol"
        BP[Binary Protocol v3.0<br/>- Feature Negotiation<br/>- Versioned Handshake<br/>- Operation Codes]
    end
    
    subgraph "Server Handler"
        CH[Client Handler<br/>- Netty TCP Server<br/>- Message Routing<br/>- Connection Management]
    end
    
    JC --> BP
    NC --> BP
    CC --> BP
    PC --> BP
    
    BP --> CH
```

This multi-platform client architecture diagram showcases Apache Ignite 3's client ecosystem. Each platform provides native client implementations with platform-specific optimizations: Java Client with async APIs and partition awareness, .NET Client with Task-based async and LINQ integration, C++ Client with modern C++17 features and ODBC driver support, and Python Client with DB-API 2.0 compliance. All clients communicate through the standardized Binary Protocol v3.0, which handles feature negotiation, versioned handshakes, and operation codes, ensuring consistent functionality across platforms. The Server Handler provides the bridge between the protocol and internal server components, managing connections through Netty TCP servers, routing messages to appropriate services, and maintaining connection state for all client platforms.

### Connection Management and Load Balancing

Clients implement sophisticated connection management with partition awareness and automatic failover capabilities.

```mermaid
sequenceDiagram
    participant C as Client
    participant LB as Load Balancer
    participant N1 as Node 1
    participant N2 as Node 2
    participant N3 as Node 3
    
    C->>LB: Initialize Connections
    LB->>N1: Establish Connection
    LB->>N2: Establish Connection
    LB->>N3: Establish Connection
    
    C->>LB: Data Operation (Partition-aware)
    LB->>LB: Route to Primary Replica
    LB->>N2: Execute Operation
    N2-->>LB: Response
    LB-->>C: Response
    
    Note over N2: Node Failure
    N2-xLB: Connection Lost
    LB->>LB: Update Topology
    C->>LB: Next Operation
    LB->>N1: Failover to Available Node
    N1-->>LB: Response
    LB-->>C: Response
```

This connection management sequence diagram illustrates the sophisticated client-side load balancing and failover mechanisms in Apache Ignite 3. The client first initializes connections to multiple cluster nodes through the Load Balancer component, establishing a connection pool for high availability. When executing data operations, the Load Balancer uses partition awareness to route requests directly to the primary replica holding the target data, minimizing network hops and latency. When a node failure occurs (shown as Node 2 going down), the Load Balancer detects the connection loss, updates its topology view, and automatically routes subsequent operations to available nodes (Node 1 in this case). This ensures continuous operation without client-side errors, providing seamless failover capability while maintaining optimal performance through intelligent routing.

## Configuration Management

### Hierarchical Configuration Architecture

The system uses a schema-based configuration approach with separation between local and distributed settings.

```mermaid
graph TB
    subgraph "Configuration Sources"
        CS[Configuration Schemas<br/>@ConfigurationRoot]
        LC[Local Config<br/>Node-specific]
        DC[Distributed Config<br/>Cluster-wide]
    end
    
    subgraph "Storage"
        V[Vault<br/>Local KV Store]
        MS[Metastorage<br/>Distributed KV Store]
    end
    
    subgraph "Runtime"
        CM[Configuration Manager]
        CC[Configuration Changes]
        W[Watchers]
    end
    
    CS --> LC
    CS --> DC
    LC --> V
    DC --> MS
    
    V --> CM
    MS --> CM
    CM --> CC
    CC --> W
```

This hierarchical configuration architecture diagram shows how Apache Ignite 3 manages both local and distributed configuration settings. Configuration Schemas annotated with @ConfigurationRoot define the structure and validation rules for all settings. These schemas generate both Local Config for node-specific settings and Distributed Config for cluster-wide settings. Local configurations are stored in the Vault (local key-value store) on each node, while distributed configurations are stored in the cluster-wide Metastorage. The Configuration Manager coordinates access to both storage systems, propagating Configuration Changes through Watchers that notify components when settings are updated. This architecture enables runtime configuration updates without service interruption while maintaining consistency across the cluster.

## Security Architecture

### Authentication and Authorization

The security model integrates with the client protocol and provides extensible authentication mechanisms.

```mermaid
sequenceDiagram
    participant C as Client
    participant CH as Client Handler
    participant AUTH as Authenticator
    participant SEC as Security Manager
    
    C->>CH: Connection Request
    CH->>AUTH: Authenticate User
    AUTH->>AUTH: Validate Credentials
    AUTH-->>CH: Authentication Result
    
    alt Authentication Success
        CH->>SEC: Create Security Context
        SEC-->>CH: User Context
        CH-->>C: Connection Established
    else Authentication Failure
        CH-->>C: Authentication Error
    end
    
    C->>CH: Operation Request
    CH->>SEC: Authorize Operation
    SEC-->>CH: Authorization Result
    
    alt Authorized
        CH->>CH: Execute Operation
        CH-->>C: Operation Result
    else Unauthorized
        CH-->>C: Authorization Error
    end
```

This security sequence diagram demonstrates the two-phase security model in Apache Ignite 3, encompassing both authentication and authorization. When a client attempts to connect, the Client Handler delegates to the Authenticator for credential validation, which can support various authentication mechanisms (basic, LDAP, custom). Upon successful authentication, the Security Manager creates a user security context containing roles and permissions. For each subsequent operation request, the Security Manager performs authorization checks against the user's permissions before allowing operation execution. This design separates authentication concerns from authorization logic, enabling pluggable authentication providers while maintaining centralized access control. The alternative flows show that authentication failures prevent connection establishment, while authorization failures block specific operations without terminating the connection.

## Performance and Scalability Considerations

### Threading Model

The system employs specialized thread pools for different types of operations to optimize performance and resource utilization.

- **SQL Planning Pool**: Configurable thread pool for query optimization (default: 4 threads)
- **SQL Execution Pool**: Configurable thread pool for query execution (default: 4 threads)
- **Partition Operations**: Dedicated executors for data operations
- **Raft Operations**: Specialized pools for consensus operations
- **Network Operations**: Netty event loops for I/O operations

### Memory Management

- **Page-based Storage**: Efficient page-based memory management
- **MVCC Version Chains**: Efficient storage of row versions
- **Connection Pooling**: Resource reuse across client connections
- **Garbage Collection**: Automated cleanup of old versions and metadata

### Scalability Patterns

- **Horizontal Scaling**: Dynamic node addition/removal
- **Partition Distribution**: Automatic data rebalancing
- **Client Load Balancing**: Partition-aware request routing
- **Elastic Storage**: Multiple storage engines with different characteristics

## Operational Architecture

### Cluster Operations and Management

Apache Ignite 3 provides multiple interfaces for cluster management and monitoring, ensuring operational visibility and control.

```mermaid
graph TB
    subgraph "Management Interfaces"
        CLI[Ignite CLI<br/>Interactive & Scripted]
        REST[REST API<br/>HTTP Management]
        JMX[JMX Interface<br/>Metrics & Control]
        WEB[Web Console<br/>Visual Management]
    end
    
    subgraph "Operational Services"
        METRICS[Metrics Collection<br/>JVM & System Metrics]
        LOGGING[Distributed Logging<br/>Structured Events]
        HEALTH[Health Monitoring<br/>Component Status]
        BACKUP[Backup & Recovery<br/>Point-in-time Snapshots]
    end
    
    subgraph "Cluster Coordination"
        CMG_OPS[CMG Operations<br/>Node Join/Leave]
        CONFIG_OPS[Configuration Changes<br/>Runtime Updates]
        TOPOLOGY[Topology Management<br/>Partition Rebalancing]
        MAINTENANCE[Maintenance Mode<br/>Rolling Updates]
    end
    
    CLI --> CMG_OPS
    CLI --> CONFIG_OPS
    CLI --> TOPOLOGY
    
    REST --> METRICS
    REST --> HEALTH
    REST --> BACKUP
    
    JMX --> METRICS
    JMX --> LOGGING
    
    WEB --> REST
    WEB --> JMX
```

This cluster operations and management diagram illustrates the multiple interfaces and services that support Apache Ignite 3 operations. The Management Interfaces provide different entry points for administration: CLI for command-line operations, REST API for HTTP-based management, JMX for Java-based monitoring tools, and Web Console for graphical management. The Operational Services encompass metrics collection, distributed logging, health monitoring, and backup/recovery capabilities. The Cluster Coordination section handles fundamental cluster operations including node lifecycle management, configuration changes, topology management, and maintenance operations. The connections show that CLI interfaces are primarily used for cluster coordination tasks, REST API serves operational services, and the Web Console aggregates both REST and JMX data for unified management visibility.

### Deployment Patterns

The system supports various deployment configurations optimized for different use cases and operational requirements.

```mermaid
graph TB
    subgraph "Development Deployment"
        DEV[Single Node<br/>All-in-one Development]
    end
    
    subgraph "Production Deployment"
        subgraph "High Availability Cluster"
            PROD_N1[Node 1<br/>Data + Compute + SQL]
            PROD_N2[Node 2<br/>Data + Compute + SQL]
            PROD_N3[Node 3<br/>Data + Compute + SQL]
            PROD_N4[Node 4<br/>Data + Compute + SQL]
            PROD_N5[Node 5<br/>Data + Compute + SQL]
        end
        
        subgraph "Load Balancers"
            LB1[Application LB<br/>Client Connections]
            LB2[Management LB<br/>Admin Interfaces]
        end
    end
    
    subgraph "Hybrid Cloud Deployment"
        subgraph "On-Premise Tier"
            ONPREM_N1[Node 1]
            ONPREM_N2[Node 2]
        end
        
        subgraph "Cloud Tier"
            CLOUD_N1[Cloud Node 1]
            CLOUD_N2[Cloud Node 2]
            CLOUD_N3[Cloud Node 3]
        end
    end
    
    subgraph "Edge Computing"
        EDGE_N1[Edge Node 1<br/>Local Processing]
        EDGE_N2[Edge Node 2<br/>Local Processing]
        CENTRAL[Central Cluster<br/>Aggregation & Analytics]
    end
    
    LB1 --> PROD_N1
    LB1 --> PROD_N2
    LB1 --> PROD_N3
    LB2 --> PROD_N4
    LB2 --> PROD_N5
    
    ONPREM_N1 <--> CLOUD_N1
    ONPREM_N2 <--> CLOUD_N2
    
    EDGE_N1 -.-> CENTRAL
    EDGE_N2 -.-> CENTRAL
```

This deployment patterns diagram showcases the flexibility of Apache Ignite 3 across different operational environments. The Development Deployment shows a single-node configuration suitable for development and testing scenarios. The Production Deployment illustrates a high-availability cluster with five nodes behind load balancers, where Application LB handles client connections and Management LB handles administrative interfaces, ensuring both performance and operational separation. The Hybrid Cloud Deployment demonstrates how on-premise and cloud nodes can participate in the same cluster, enabling gradual cloud migration or hybrid operational models. The Edge Computing scenario shows edge nodes performing local processing with periodic synchronization to a central cluster for aggregation and analytics, supporting distributed computing patterns where data processing occurs close to data sources.

### Monitoring and Observability

The platform provides extensive monitoring capabilities through metrics, logging, and distributed tracing integration.

```mermaid
graph TB
    subgraph "Metrics Collection"
        JVM_METRICS[JVM Metrics<br/>Memory, GC, Threads]
        SYS_METRICS[System Metrics<br/>CPU, Network, Disk]
        APP_METRICS[Application Metrics<br/>Queries, Transactions]
        RAFT_METRICS[Consensus Metrics<br/>Leadership, Replication]
    end
    
    subgraph "Monitoring Stack"
        PROMETHEUS[Prometheus<br/>Metrics Storage]
        GRAFANA[Grafana<br/>Visualization]
        ALERT_MGR[Alert Manager<br/>Notification Rules]
    end
    
    subgraph "Logging Infrastructure"
        STRUCT_LOG[Structured Logging<br/>JSON Format]
        LOG_AGGR[Log Aggregation<br/>ELK Stack]
        AUDIT_LOG[Audit Logging<br/>Security Events]
    end
    
    subgraph "Distributed Tracing"
        TRACE_COLLECT[Trace Collection<br/>OpenTelemetry]
        TRACE_STORE[Trace Storage<br/>Jaeger/Zipkin]
        SPAN_ANALYSIS[Span Analysis<br/>Performance Insights]
    end
    
    JVM_METRICS --> PROMETHEUS
    SYS_METRICS --> PROMETHEUS
    APP_METRICS --> PROMETHEUS
    RAFT_METRICS --> PROMETHEUS
    
    PROMETHEUS --> GRAFANA
    PROMETHEUS --> ALERT_MGR
    
    STRUCT_LOG --> LOG_AGGR
    AUDIT_LOG --> LOG_AGGR
    
    TRACE_COLLECT --> TRACE_STORE
    TRACE_STORE --> SPAN_ANALYSIS
```

This monitoring and observability diagram presents the observability stack for Apache Ignite 3 operations. The Metrics Collection layer gathers data from four key areas: JVM metrics for runtime health, system metrics for infrastructure monitoring, application metrics for business logic performance, and Raft metrics for consensus system health. The Monitoring Stack follows industry-standard patterns with Prometheus for metrics storage and scraping, Grafana for visualization and dashboards, and Alert Manager for notification and escalation rules. The Logging Infrastructure provides structured JSON logging with aggregation through ELK stack and dedicated audit logging for security events. The Distributed Tracing section enables end-to-end request tracking through OpenTelemetry collection, storage in Jaeger or Zipkin, and span analysis for performance insights. This multi-layered approach ensures complete visibility into system behavior across all operational dimensions.

## Final Thoughts

Apache Ignite 3 represents a significant advancement in distributed database architecture, delivering a platform optimized for modern cloud-native applications and high-performance computing workloads.

### Key Architectural Strengths

#### Distributed-First Design

- Built from the ground up for horizontal scalability
- Automatic partition management and rebalancing
- Strong consistency through Raft consensus
- Fault tolerance with automatic failover

#### Performance Optimization

- MVCC-based concurrency without blocking
- Pluggable storage engines for different workload patterns
- Cost-based SQL optimization with distributed execution
- Partition-aware client routing for minimal network overhead

#### Operational Excellence

- Schema-driven data management with automatic validation
- Unified management through CLI and REST APIs
- Extensive monitoring and observability features
- Rolling updates and maintenance mode support

#### Developer Experience

- Multi-platform native clients with consistent APIs
- Standard SQL with JDBC/ODBC compatibility
- Distributed compute with data co-location
- Comprehensive testing and development tools

### Use Case Alignment

The architecture supports diverse application patterns:

- **Real-time Analytics**: In-memory processing with persistent storage
- **Transactional Applications**: ACID guarantees with high throughput
- **Distributed Computing**: Co-located computation with data locality
- **Hybrid Workloads**: Mixed OLTP and OLAP on the same platform
- **Cloud-Native Applications**: Kubernetes-ready with elastic scaling

This architecture demonstrates Apache Ignite 3's approach to building a modern, distributed database system that balances performance, consistency, and scalability while providing robust multi-platform support and extensibility.
