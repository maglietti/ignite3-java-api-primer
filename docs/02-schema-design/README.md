<!--
Licensed under Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)
SPDX-License-Identifier: CC-BY-SA-4.0
For full license text, see LICENSE-CC-BY-SA-4.0
-->

# Module 02: Schema Design

Your music platform has basic storage, but now you need real relationships. Artists release Albums, Albums contain Tracks, and your users expect lightning-fast browsing through these connections.

The challenge? These relationships need to work across distributed storage. When an Artist's albums span multiple servers, how do you keep related data together for performance? When users browse from Artist to Album to Track, how do you avoid network delays?

## The Schema Story

This is where schema design becomes your secret weapon. Instead of writing SQL scripts and hoping for the best, you'll define your data relationships directly in Java code. Annotations tell Ignite 3 exactly how to distribute your music catalog for optimal performance.

You'll start with simple Artist entities, then build Album relationships that stay close to their Artists, and finally create Track hierarchies that enable fast music browsing across your entire distributed platform.

## Building Your Music Schema

### Chapter 1: [Basic Annotations](./01-basic-annotations.md)

*From Java classes to distributed tables*

Your Artist class becomes a distributed table with a few simple annotations. You'll discover how @Table, @Zone, @Column, and @Id transform plain Java objects into schema definitions that deploy consistently across any environment.

### Chapter 2: [Relationships and Colocation](./02-relationships-and-colocation.md)

*Keeping related music together*

Albums belong to Artists, but in distributed storage, they might live on different servers. You'll learn colocation strategies that keep Artist data close to their Albums, enabling fast browsing without network delays. When related data lives on the same node, joins and queries execute locally without network round trips, dramatically reducing latency and increasing throughput for related data operations.

### Chapter 3: [Advanced Annotations](./03-advanced-annotations.md)

*Optimizing for real-world music platforms*

Your platform grows complex - multiple zones for different data types, indexes for fast searches, composite keys for complex relationships. You'll handle these advanced patterns while maintaining the simplicity of annotation-driven design.

### Chapter 4: [Schema Changes](./04-schema-evolution.md)

*Growing your platform safely*

Success means change. New features require new columns, performance improvements need new indexes. You'll learn how to evolve your schema safely while keeping your distributed music platform running.

## Your Evolving Platform

What starts as simple Artist storage becomes a sophisticated music platform. Each schema decision - how Albums relate to Artists, how Tracks connect to Albums, how indexes speed up searches - shapes your platform's performance and capabilities.

## Reference Application

**[`03-schema-annotations-app/`](../../ignite3-reference-apps/03-schema-annotations-app/)**

See all the schema patterns in working code - from basic Artist tables to complex Track hierarchies with optimized colocation.

## Where This Takes You

With your music schema optimized for distributed performance, you'll want to access this data efficiently. That's where data access APIs come in - taking advantage of the colocation and relationships you've designed to build fast, responsive music applications.

---

← [Foundation](../01-foundation/) | **Schema Design** | [Data Access APIs](../03-data-access-apis/) →

[Basic Annotations](./01-basic-annotations.md)
