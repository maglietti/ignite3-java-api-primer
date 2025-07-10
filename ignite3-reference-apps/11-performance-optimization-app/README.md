# Apache Ignite 3 Performance Optimization Application

Query tuning and scalability optimization techniques using Apache Ignite 3's distributed SQL engine.

**Related Documentation**: [Query Performance](../../docs/05-performance-scalability/03-query-performance.md)

## Overview

Demonstrates systematic performance optimization for music streaming platform queries. Shows how to transform slow analytics queries from 30-second timeouts to sub-second insights through query analysis, index optimization, join strategies, and caching techniques.

## Key Concepts

- **Query Timing Analysis**: Nanosecond-precision performance measurement
- **Execution Plan Analysis**: EXPLAIN PLAN FOR usage and optimization workflow
- **Index Optimization**: Strategic index design for distributed queries
- **Join Optimization**: Colocation-aware join strategies and data placement
- **Cache Optimization**: Cache-aside patterns for read-heavy workloads

## Prerequisites

- Apache Ignite 3 cluster running (see [00-docker setup](../00-docker/README.md))
- Sample data setup completed ([01-sample-data-setup](../01-sample-data-setup/))
- Java 17 or higher
- Maven 3.8+ or Gradle (via wrapper)

## Applications

| Application | Description | Run Command |
|-------------|-------------|-------------|
| PerformanceOptimizationAPIDemo | Orchestrator that runs all performance optimization demonstrations providing a complete toolkit for optimizing Apache Ignite 3 queries. | `../gradlew runPerformanceOptimizationAPIDemo` |
| QueryTimingAnalysis | Demonstrates systematic performance measurement techniques with nanosecond precision timing and filter strategy comparisons. | `../gradlew runQueryTimingAnalysis` |
| QueryExecutionPlanAnalysis | Shows execution plan analysis using EXPLAIN PLAN FOR syntax with optimization workflow for systematic query improvement. | `../gradlew runQueryExecutionPlanAnalysis` |
| IndexOptimizationStrategies | Focuses on index optimization strategies for distributed query performance including single vs composite indexes and selectivity analysis. | `../gradlew runIndexOptimizationStrategies` |
| OptimizedJoinStrategies | Demonstrates join optimization and data colocation strategies leveraging data locality for optimal join performance. | `../gradlew runOptimizedJoinStrategies` |
| CacheAsideOptimization | Shows cache-aside optimization strategies for read-heavy workloads including cache warming and lazy loading patterns. | `../gradlew runCacheAsideOptimization` |

### Running the Applications

From this directory, use Gradle to run each application:

```bash
# Run complete Performance Optimization API demo (all optimizations)
../gradlew runPerformanceOptimizationAPIDemo

# Run individual demonstrations
../gradlew runQueryTimingAnalysis
../gradlew runQueryExecutionPlanAnalysis
../gradlew runIndexOptimizationStrategies
../gradlew runOptimizedJoinStrategies
../gradlew runCacheAsideOptimization

# Custom cluster address
../gradlew runPerformanceOptimizationAPIDemo --args="192.168.1.100:10800"
```

## Application Details

### Query Timing Analysis (`QueryTimingAnalysis`)

- Execution time measurement with nanosecond precision
- First result timing for streaming query optimization
- Filter strategy comparison (indexed vs function-based)
- Performance ratio analysis and bottleneck identification

### Execution Plan Analysis (`QueryExecutionPlanAnalysis`)

- EXPLAIN PLAN FOR syntax usage and result processing
- Plan comparison for different query formulations (EXISTS vs JOIN vs Window functions)
- Complex analytics query plan analysis with CTEs and window functions
- Systematic optimization workflow using execution plans

### Index Optimization Strategies (`IndexOptimizationStrategies`)

- Single vs composite index performance characteristics
- Prefix matching optimization for search queries
- Sort-based index optimization for ordered results
- Index selectivity impact on query execution plans

### Optimized Join Strategies (`OptimizedJoinStrategies`)

- Colocation-aware join strategies for related data
- Join order optimization based on data distribution
- Broadcast vs shuffle join selection criteria
- Partition pruning for multi-table queries

### Cache Optimization (`CacheAsideOptimization`)

- Cache warming strategies for popular content
- Lazy loading with fallback to database queries
- Cache invalidation coordination across updates
- Async cache population for non-blocking performance


## Performance Targets

Optimization goals for music streaming platform:

- **Artist searches**: < 100ms with name-based indexes
- **Album lookups**: < 50ms with direct index access
- **Genre analytics**: < 1000ms for complex aggregations
- **Cache hits**: < 5ms for frequently accessed data

## Data Model

Uses music store sample dataset:

- **Artist**: Music artists with name-based searching
- **Album**: Albums with artist relationships and title browsing
- **Track**: Individual tracks with genre, pricing, and popularity data
- **Genre**: Music genres for classification and analytics
- **Customer/Invoice**: Purchase history for recommendation analysis

## Query Optimization Techniques

### Filter Strategy Optimization

- **Direct indexed filters**: Leverage database indexes for optimal performance
- **Function-based filters**: Avoid expressions that prevent index usage
- **Predicate pushdown**: Apply filters early in execution pipeline

### Join Order Optimization

- **Selectivity-based ordering**: Filter most selective tables first
- **Colocation awareness**: Leverage data placement for local execution
- **Partition pruning**: Target specific data partitions when possible

### Index Design Patterns

- **Single-column indexes**: Simple equality and range queries
- **Composite indexes**: Multi-column WHERE clauses and sorting
- **Covering indexes**: Include additional columns to avoid table lookups
- **Prefix indexes**: Optimize partial string matching operations

## Performance Measurement

### Query Metrics

- **Total execution time**: End-to-end query performance
- **Time to first result**: Streaming operation optimization
- **Result count**: Verification of query correctness
- **Performance ratios**: Comparison between optimization approaches

### Cache Metrics

- **Cache hit rates**: Effectiveness of caching strategy
- **Load times**: Database fallback performance
- **Async operation timing**: Non-blocking cache population
- **Memory utilization**: Cache size and eviction patterns

## Common Issues

**Slow queries**: Check execution plans and add appropriate indexes

**High memory usage**: Monitor cache size and implement eviction policies

**Timeout errors**: Optimize queries or increase timeout values

## Error Handling

Production-ready patterns:

- Resource management with try-with-resources
- Graceful degradation for cache misses
- Query timeout handling with fallback strategies
- Performance monitoring and alerting integration
