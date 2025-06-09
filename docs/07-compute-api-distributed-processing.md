# 7. Compute API - Distributed Processing

## Compute Jobs (`IgniteCompute`)

### Job Creation and Deployment

```java
JobDescriptor<String, Void> job = JobDescriptor.builder(WordPrintJob.class)
    .units(new DeploymentUnit(DEPLOYMENT_UNIT_NAME, DEPLOYMENT_UNIT_VERSION))
    .build();
```

### Job Execution Patterns

```java
JobTarget jobTarget = JobTarget.anyNode(client.clusterNodes());
client.compute().execute(jobTarget, job, "word");
```

### Target Selection Strategies

*[To be completed with targeting strategies]*

## Job Implementation

### `ComputeJob` Interface

```java
private static class WordLengthJob implements ComputeJob<String, Integer> {
    @Override
    public CompletableFuture<Integer> executeAsync(JobExecutionContext context, String arg) {
        return CompletableFuture.completedFuture(arg.length());
    }
}
```

### Input/Output Handling

*[To be completed with I/O patterns]*

### Error Handling in Jobs

*[To be completed with error handling]*

## Async Compute Operations

### Parallel Job Execution

```java
CompletableFuture<Integer> jobFuture = client.compute().executeAsync(jobTarget, job, word);
List<Integer> results = jobFutures.stream()
    .map(CompletableFuture::join)
    .collect(Collectors.toList());
```

### Result Aggregation

*[To be completed with aggregation patterns]*

## Advanced Topics

- Code deployment units
- Job cancellation