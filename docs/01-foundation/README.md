# Module 01: Foundation

*Essential concepts for building distributed applications with Apache Ignite 3*

## About This Module

This foundational module introduces you to Apache Ignite 3 through practical, hands-on experience. You'll establish your development environment, make your first connection, and understand the core concepts that make distributed data management possible.

**Start here if you're new to Ignite 3** - this module builds the essential knowledge foundation for all advanced topics.

## Learning Objectives

By completing this module, you will:

- Understand what makes Ignite 3 unique among distributed data platforms
- Connect to an Ignite 3 cluster and perform basic operations
- Recognize how data distribution and consistency work in practice
- Feel confident progressing to schema design and advanced APIs

## Module Journey

### Chapter 1: [Introduction and Architecture](./01-introduction-and-architecture.md)

*Discover what Ignite 3 can do for your applications*

**What you'll understand:**

- Why distributed data management matters
- How Ignite 3 differs from traditional databases
- When to choose Ignite 3 for your projects
- Core platform capabilities and use cases

**Time investment:** 15-20 minutes reading

### Chapter 2: [Getting Started](./02-getting-started.md)

*Build your first working Ignite 3 application*

**What you'll accomplish:**

- Set up your development environment
- Connect to a running Ignite 3 cluster
- Create your first distributed table
- Store and retrieve data successfully

**Time investment:** 30-45 minutes hands-on

**Reference Application:** [`02-getting-started-app/`](../../ignite3-reference-apps/02-getting-started-app/)

### Chapter 3: [Distributed Data Fundamentals](./03-distributed-data-fundamentals.md)

*Understand how distributed systems work in practice*

**What you'll grasp:**

- How data gets distributed across cluster nodes
- Why consistency matters and how Ignite 3 maintains it
- How to think about performance in distributed environments
- Common patterns for building resilient applications

**Time investment:** 20-25 minutes reading + exploration

## Hands-On Learning

This module emphasizes practical experience. You'll work with a music store dataset that continues throughout all modules, reducing cognitive load and letting you focus on learning Ignite 3 concepts.

**Sample entities:**

- **Artist** - Individual musicians and bands
- **Album** - Music releases with metadata
- **Track** - Individual songs with pricing

These entities demonstrate real-world relationships and distributed data patterns you'll encounter in production applications.

## Prerequisites

**Technical requirements:**

- Java 17 or later
- Maven 3.8 or later
- Docker (for running Ignite 3 cluster)

**Knowledge assumptions:**

- Basic Java programming
- Familiarity with databases (SQL helpful but not required)
- Understanding of basic distributed systems concepts (beneficial but not essential)

## Success Indicators

**You're ready for Module 02** when you can:

- Explain why Ignite 3 is valuable for distributed applications
- Start an Ignite 3 cluster and connect a Java client
- Create a simple table and perform basic operations
- Understand how data gets distributed across nodes

## Common Questions

**Q: Do I need distributed systems experience?**
A: No. This module introduces concepts progressively with practical examples.

**Q: Can I skip to advanced modules?**
A: While possible, the foundation concepts here prevent confusion later.

**Q: How long does this module take?**
A: Most learners complete it in 1-2 hours, including hands-on exercises.

---

**Navigation:**

← [**Reference Materials**](../00-reference/) | **Foundation** | [**Schema Design**](../02-schema-design/) →

**Start Learning:** [**Introduction and Architecture**](./01-introduction-and-architecture.md)
