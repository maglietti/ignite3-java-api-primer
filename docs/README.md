# Apache Ignite 3 Java API Primer

Modern applications demand more than traditional databases can deliver. When data grows beyond single-node capacity, when millisecond response times matter at scale, and when application logic needs to execute close to data, you need a distributed data platform built for performance.

Apache Ignite 3 delivers that platform. This primer teaches you how to harness its Java API to build applications that scale horizontally while maintaining ACID transactions, execute distributed computing workloads, and achieve performance that traditional architectures cannot match.

## Why This Primer Exists

Learning distributed data platforms requires connecting concepts to working code. Academic explanations of distributed systems rarely translate to production applications. Random code samples lack context for real-world usage patterns.

This primer bridges that gap through structured learning modules that build systematically from connection fundamentals to production-ready patterns. Each concept connects to executable reference applications using a consistent dataset, letting you focus on mastering Ignite 3 rather than context switching between different examples.

## What You'll Master

**Foundation**: Connect to Ignite 3 clusters, understand distributed data principles, and perform basic operations that form the building blocks for everything else.

**Schema Design**: Model data relationships using annotations that optimize for distributed performance. Learn colocation strategies that make complex queries fast and implement schema evolution patterns for production deployment.

**Data Access**: Choose between Table API and SQL API based on your use case. Handle both synchronous and asynchronous operations efficiently. Execute analytical queries that leverage Ignite 3's distributed architecture.

**Distributed Operations**: Manage ACID transactions across multiple nodes. Implement distributed computing patterns that bring processing to data. Handle complex workflows with proper error recovery.

**Performance & Scalability**: Optimize data ingestion for high-throughput scenarios. Implement caching strategies that improve application responsiveness. Monitor and tune query performance for production workloads.

## Learning Approach

This primer uses progressive complexity within a consistent context. Instead of jumping between disconnected examples, you'll work with a music streaming platform dataset throughout all modules. Artist, Album, and Track entities demonstrate real-world relationships while Customer and Invoice workflows show business logic patterns.

This approach reduces cognitive load while demonstrating how distributed data patterns apply to production applications. You'll understand not just how to use Ignite 3's APIs, but why specific patterns matter for distributed systems.

## Prerequisites

You need Java 17+ and basic programming experience. Database knowledge helps but isn't required. Understanding distributed systems concepts benefits your learning but isn't essential - the modules introduce these concepts through practical examples.

Docker is required for running Ignite 3 clusters locally, but no prior Docker expertise is needed.

## Starting Your Journey

**New to Ignite 3?** Begin with [**Foundation**](./01-foundation/) to establish core concepts through hands-on exercises.

**Need architectural context first?** Start with [**Reference Materials**](./00-reference/) to understand platform design before diving into practical implementation.

**Working on specific requirements?** Jump directly to the module that matches your current needs, though foundation concepts will help prevent confusion later.

Each module builds on previous concepts while remaining focused on specific learning objectives. Reference applications provide working code that demonstrates the concepts in action.

---

**Ready to begin?** [**Start with Foundation**](./01-foundation/01-introduction-and-architecture.md)
