# Module 01: Foundation

## What You'll Accomplish

By completing this module, you will:

- Connect to Apache Ignite 3 clusters and perform basic distributed operations
- Create your first distributed tables with proper zone configuration
- Understand data distribution patterns across cluster nodes
- Build the foundation for schema design and advanced API usage

## Building on Previous Knowledge

This module assumes basic Java programming experience and familiarity with database concepts. No distributed systems knowledge is required - these concepts are introduced through practical examples using music store data.

## Module Overview

Foundation establishes core patterns for distributed data management. You'll work with Artist, Album, and Track entities throughout this module, building practical experience with connection management, table creation, and data operations that form the basis for all advanced Ignite 3 development.

## Implementation Pattern

### Chapter 1: [Introduction and Architecture](./01-introduction-and-architecture.md)

**What You'll Learn:** Platform capabilities and deployment patterns for distributed data applications

**Implementation Focus:** Understanding connection strategies and zone configuration patterns that enable distributed operations

### Chapter 2: [Getting Started](./02-getting-started.md)

**What You'll Build:** Working Ignite 3 application with cluster connection and basic table operations

**Implementation Focus:** Practical development environment setup and first distributed table implementation

**Reference Application:** [`02-getting-started-app/`](../../ignite3-reference-apps/02-getting-started-app/)

### Chapter 3: [Distributed Data Fundamentals](./03-distributed-data-fundamentals.md)

**What You'll Understand:** Data distribution mechanics and consistency patterns across cluster nodes

**Implementation Focus:** Performance considerations and resilience patterns for production applications

## Real-world Application

The music store dataset provides consistent context throughout your learning journey. Artist, Album, and Track entities demonstrate distributed data relationships while Customer and Invoice workflows show business logic patterns.

This approach reduces cognitive load by maintaining familiar entities while you focus on mastering Ignite 3 distributed data patterns.

## Prerequisites

- Java 17 or later
- Maven 3.8 or later
- Docker for running Ignite 3 cluster

## What You've Learned → Next Steps

Foundation module establishes connection patterns, basic table operations, and data distribution understanding. This knowledge enables schema design patterns in Module 02, where you'll learn annotation-driven table creation and colocation strategies for performance optimization.

---

**Module Navigation:**

← [Reference Materials](../00-reference/) | **Foundation** | [Schema Design](../02-schema-design/) →

**Start Implementation:** [Introduction and Architecture](./01-introduction-and-architecture.md)
