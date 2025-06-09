# 1. Introduction & Overview

## What is Apache Ignite 3

- Next-generation distributed computing platform
- In-memory data grid with persistent storage
- SQL and NoSQL capabilities
- Distributed computing engine

## Key Features and Capabilities

- ACID transactions
- SQL support with ANSI compliance
- Key-value operations
- Distributed computing
- Real-time streaming
- Multi-platform client support

## Java API Architecture Overview

- **Entry Points**: `Ignite`, `IgniteClient`, `IgniteServer`
- **Dual Access Paradigms**: Object-oriented (Table API) and SQL-based access
- **Async-First Design**: All operations support both sync and async patterns
- **Strong Typing**: Extensive use of generics and type-safe operations

## Entry Points

- **Thin Client**: `IgniteClient` - for remote connections
- **Embedded Server**: `IgniteServer` - for embedded nodes
- **Core Interface**: `Ignite` - unified API access point