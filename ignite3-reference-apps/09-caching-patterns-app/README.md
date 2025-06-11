# Caching Patterns App - Apache Ignite 3 Reference

**Advanced caching strategies and patterns with Apache Ignite 3**

📖 **Related Documentation**: [Caching Patterns](../../docs/09-caching-patterns.md)

## Overview

This reference application demonstrates advanced caching patterns and strategies using Apache Ignite 3, including cache-aside, write-through, write-behind, and refresh-ahead patterns with the music store sample dataset.

## What You'll Learn

- **Cache-Aside Pattern**: Manual cache management with explicit loading and invalidation
- **Write-Through Pattern**: Synchronous writes to both cache and persistent storage
- **Write-Behind Pattern**: Asynchronous write batching for performance optimization
- **Refresh-Ahead Pattern**: Proactive cache refresh before expiration
- **Cache Warming**: Strategies for preloading critical data
- **Cache Hierarchies**: Multi-level caching architectures
- **Eviction Policies**: Managing cache size and memory usage
- **Cache Metrics**: Monitoring hit ratios and performance

## Architecture

### Caching Layers

```
Application Layer
├── L1 Cache (Local/Near Cache)
├── L2 Cache (Ignite Distributed Cache)
└── L3 Storage (Database/Persistent Store)
```

### Pattern Examples

- **Popular Albums Cache**: Cache-aside pattern for frequently accessed albums
- **Artist Profile Cache**: Write-through pattern for artist information updates
- **Track Analytics Cache**: Write-behind pattern for high-volume track play counts
- **Genre Metadata Cache**: Refresh-ahead pattern for reference data

## Prerequisites

- Java 17+
- Maven 3.8+
- **⚠️ Required**: Apache Ignite 3 cluster running (see [00-docker setup](../00-docker/README.md))
- **⚠️ Required**: Sample data setup completed (see [01-sample-data-setup](../01-sample-data-setup/README.md))

## Quick Start

```bash
# Run complete caching patterns demonstration
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.caching.CachingPatternsDemo"

# Run specific pattern examples
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.caching.CacheAsidePattern"
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.caching.WriteThroughPattern"
mvn compile exec:java -Dexec.mainClass="com.apache.ignite.examples.caching.WriteHindPattern"
```

## Applications

### Planned Reference Applications

1. **CachingPatternsDemo** - Overview of all caching patterns
2. **CacheAsidePattern** - Manual cache management examples
3. **WriteThroughPattern** - Synchronous cache and storage updates
4. **WriteBehindPattern** - Asynchronous write batching
5. **RefreshAheadPattern** - Proactive cache refresh strategies
6. **CacheWarmupStrategies** - Cache preloading techniques
7. **CacheHierarchyDemo** - Multi-level caching examples
8. **CacheMetricsAnalysis** - Performance monitoring and analysis

## Related Documentation

- 📖 [Caching Patterns](../../docs/09-caching-patterns.md) - Detailed caching strategies
- 📖 [Table API](../../docs/04-table-api-object-oriented-data-access.md) - Object-oriented data access
- 📖 [Advanced Topics](../../docs/11-advanced-topics.md) - Performance optimization

## Status

🚧 **Under Development** - This module is part of the planned Phase 2 implementation. Reference applications will be built using the established sample data and patterns from the sample-data-setup module.