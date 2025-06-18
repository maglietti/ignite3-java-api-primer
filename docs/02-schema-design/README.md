# Module 02: Schema Design

*Schema-as-code patterns for distributed data modeling*

## About This Module

This module transforms how you think about database schema design. Instead of writing SQL DDL scripts and managing schema across environments, you'll learn to define your distributed data models directly in Java using annotations.

**Essential for all Ignite 3 development** - schema design determines your application's performance, scalability, and maintainability.

## Learning Objectives

By completing this module, you will:

- Master the six core annotations for distributed table design
- Implement colocation strategies that optimize query performance
- Deploy schemas programmatically across different environments
- Understand how to evolve schemas safely in production

## Module Journey

### Chapter 1: [Basic Annotations](./01-basic-annotations.md)

*Master the fundamental building blocks of schema-as-code*

**What you'll master:**

- `@Table`, `@Zone`, `@Column`, `@Id` annotations
- How annotations eliminate schema synchronization issues
- Creating your first production-ready table definitions
- Understanding the journey from annotations to distributed tables

**Key concepts:** Schema-as-code, annotation validation, DDL generation

### Chapter 2: [Relationships and Colocation](./02-relationships-and-colocation.md)

*Design entity relationships that perform at scale*

**What you'll implement:**

- Artist → Album → Track hierarchies with proper colocation
- Using `@ColumnRef` and `colocateBy` for performance optimization
- Composite primary keys for distributed relationships
- Real-world music platform entity design

**Key concepts:** Data colocation, performance optimization, entity relationships

### Chapter 3: [Advanced Annotations](./03-advanced-annotations.md)

*Handle complex scenarios with sophisticated patterns*

**What you'll build:**

- Multi-zone architectures for different data types
- Advanced indexing strategies with `@Index`
- Complex primary key patterns for distributed systems
- Production-grade schema configurations

**Key concepts:** Distribution zones, indexing strategies, advanced patterns

### Chapter 4: [Schema Evolution](./04-schema-evolution.md)

*Deploy and evolve schemas in production environments*

**What you'll accomplish:**

- Automatic DDL generation from annotated classes
- Production deployment patterns with error handling
- Schema validation and integrity checking
- Environment-specific configuration management

**Key concepts:** DDL generation, deployment automation, schema validation

## Hands-On Learning

This module uses the music store dataset to demonstrate real-world schema design decisions:

**Entity progression:**

1. **Simple entities** (Artist) - Basic annotation patterns
2. **Related entities** (Album) - Colocation and relationships  
3. **Complex hierarchies** (Track) - Advanced patterns
4. **Business entities** (Customer, Invoice) - Production scenarios

Each chapter builds upon the previous, creating a complete distributed data model.

## Reference Application

**[`03-schema-annotations-app/`](../../ignite3-reference-apps/03-schema-annotations-app/)**

This reference application demonstrates all schema design patterns in working code:

- Complete entity definitions with all annotation patterns
- Colocation strategies and performance optimizations
- Production deployment procedures
- Schema evolution examples

Run alongside your learning to see concepts in executable form.

## Design Philosophy

**Schema-as-Code Benefits:**

- **Single source of truth** - Your Java classes ARE your schema
- **Compile-time safety** - Invalid schemas fail at compile time
- **Environment consistency** - Same definitions work everywhere
- **Performance by design** - Colocation and indexing explicit and visible

**Performance-First Approach:**

- Colocation strategies designed into the schema
- Distribution zones planned for workload patterns
- Indexing strategies aligned with query requirements

## Success Indicators

**You're ready for Module 03** when you can:

- Define complete entity models using annotations
- Implement colocation strategies for related data
- Deploy schemas programmatically with error handling
- Understand how schema decisions impact performance

## Building Toward Production

This module prepares you for production-grade schema management:

- **Module 03** will leverage these schemas for efficient data access
- **Module 04** will build transactions around these entity relationships
- **Module 05** will optimize the performance characteristics you define here

---

**Navigation:**

← [**Foundation**](../01-foundation/) | **Schema Design** | [**Data Access APIs**](../03-data-access-apis/) →

**Start Learning:** [**Basic Annotations**](./01-basic-annotations.md)
