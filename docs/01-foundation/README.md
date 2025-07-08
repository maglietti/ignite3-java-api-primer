<!--
Licensed under Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)
SPDX-License-Identifier: CC-BY-SA-4.0
For full license text, see LICENSE-CC-BY-SA-4.0
-->

# Module 01: Foundation

Your music platform's PostgreSQL instance just crashed under the load of 50,000 concurrent connections attempting to browse artists. Read replicas lag 30 seconds behind master writes, cache hit ratios are below acceptable thresholds, and your DBA is proposing horizontal sharding with application-level routing.

Meanwhile, Spotify serves 400 million active users. Netflix streams globally. They're not running single-instance databases.

## How Distributed Architecture Works

Traditional scaling introduces operational complexity: read replicas with replication lag, sharded databases that break cross-shard joins, cache layers with invalidation race conditions, and application logic that grows more complex with each scaling layer.

Ignite 3 implements a different architecture. Your data resides natively across multiple cluster nodes. Applications connect to the cluster topology, not individual instances. When you persist an Artist entity, the cluster automatically distributes it across nodes based on partitioning algorithms. When you execute Album queries, the cluster processes requests across all nodes in parallel.

No replication lag. No manual shard management. No cache invalidation complexity. Just distributed storage that presents a unified interface to your application layer.

## Implementation Steps

### Chapter 1: [Introduction and Architecture](./01-introduction-and-architecture.md)

*Configure distributed storage architecture*

Your monitoring dashboard shows connection pool exhaustion and query timeouts. Users are experiencing page load failures. Configure Ignite 3 cluster topology to distribute load across multiple nodes while maintaining application simplicity.

### Chapter 2: [Getting Started](./02-getting-started.md)

*Implement distributed music catalog storage*

Configure cluster connectivity and implement distributed table operations. Connect to a live Ignite cluster and persist Artist entities across multiple nodes. No connection pool limits, no replication lag, no cache layer complexity.

**Implementation Reference:** [`02-getting-started-app/`](../../ignite3-reference-apps/02-getting-started-app/)

### Chapter 3: [Distributed Data Fundamentals](./03-distributed-data-fundamentals.md)

*Understand partition distribution and consistency*

Your Artist entities now reside across three cluster nodes simultaneously, but your application code remains unchanged. Examine the partitioning algorithms and consistency mechanisms that enable this transparent distribution.

## Next Implementation Challenge

Your music catalog now operates on distributed storage with improved performance. Ignite achieves this transparency through partition-aware routing that automatically directs operations to the correct cluster nodes without application-level changes. When nodes fail, the client automatically reconnects to healthy nodes and continues operations seamlessly. However, when users navigate from Artist to Album to Track entities, the data may reside on different cluster nodes, creating network latency that degrades performance.

Schema design and data colocation strategies address this challenge. You need to configure how entities distribute across the cluster topology.

## Prerequisites

- Java 17 or later
- Maven 3.8 or later  
- Docker for cluster deployment

---

← [Reference Materials](../00-reference/) | **Foundation** | [Schema Design](../02-schema-design/) →

[Introduction and Architecture](./01-introduction-and-architecture.md)
