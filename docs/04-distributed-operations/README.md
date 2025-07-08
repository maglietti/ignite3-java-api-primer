<!--
Licensed under Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)
SPDX-License-Identifier: CC-BY-SA-4.0
For full license text, see LICENSE-CC-BY-SA-4.0
-->

# Module 04: Distributed Operations

Your music platform now handles fast lookups and complex queries across distributed storage. But real applications need more than individual operations - they need transactional workflows. When users purchase music, you need to charge their card, update their library, and increment sales analytics atomically. When one step fails, everything must rollback consistently.

Traditional distributed systems force you to choose: either give up ACID guarantees for eventual consistency, or accept the complexity of two-phase commit protocols with coordinator nodes and timeout handling.

## How Distributed Transactions Work

Ignite 3 implements distributed ACID transactions without coordinator bottlenecks. Each participating node acts as a local coordinator for its data partitions, using Raft consensus to achieve atomic commit decisions across all transaction participants. This eliminates single points of failure and reduces network round-trips compared to traditional two-phase commit architectures that rely on centralized transaction managers. Purchase workflows execute across multiple cluster nodes while maintaining transaction isolation. When a payment processor fails, the entire transaction rolls back automatically across all participating nodes.

Beyond transactions, you can execute compute jobs directly on nodes containing relevant data. Instead of transferring user listening history across the network for recommendation processing, algorithms run where the data resides, eliminating serialization overhead and network latency.

## Implementation Patterns

### Chapter 1: [Transaction Fundamentals](./01-transaction-fundamentals.md)

*Configure ACID transactions across distributed nodes*

Your purchase workflows need atomic execution across payment processing, library updates, and analytics increments. Configure transaction boundaries and isolation levels that maintain consistency without coordinator bottlenecks.

### Chapter 2: [Compute API Processing](./03-compute-api-processing.md)

*Execute algorithms where data resides*

Process recommendation algorithms directly on nodes containing user listening data. Deploy compute jobs that execute locally on each partition, eliminating network transfer overhead for large dataset processing.

## Production Transaction Challenges

Your music platform now handles complex purchase workflows and recommendation processing across distributed nodes. User activity generates millions of transactions per hour, requiring optimized performance while maintaining transactional consistency.

## Implementation References

**[`06-transactions-app/`](../../ignite3-reference-apps/06-transactions-app/)** and **[`07-compute-api-app/`](../../ignite3-reference-apps/07-compute-api-app/)**

Complete transaction and compute implementations showing ACID workflow patterns and compute job deployment across cluster nodes.

## Next Implementation Challenge

Transactional workflows and compute jobs now execute efficiently across your distributed cluster. However, production traffic generates millions of events per hour - play events, purchase transactions, user interactions. Your system needs high-throughput streaming and intelligent caching to handle this scale while maintaining the transactional consistency you've implemented.

---

← [Data Access APIs](../03-data-access-apis/) | **Distributed Operations** | [Performance and Scalability](../05-performance-scalability/) →

[Transaction Fundamentals](./01-transaction-fundamentals.md)
