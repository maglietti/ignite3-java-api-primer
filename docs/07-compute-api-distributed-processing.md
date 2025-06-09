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

Ignite 3 provides flexible strategies for selecting where compute jobs execute:

```java
// Execute on any available node
JobTarget anyNode = JobTarget.anyNode(client.clusterNodes());
client.compute().execute(anyNode, job, "input");

// Execute on specific nodes
Set<ClusterNode> specificNodes = client.clusterNodes().stream()
    .filter(node -> node.name().startsWith("compute-"))
    .collect(Collectors.toSet());
JobTarget specificTarget = JobTarget.nodes(specificNodes);

// Execute on nodes colocated with data
JobTarget colocatedTarget = JobTarget.colocated("TableName", 
    Tuple.create().set("key", "value"));
client.compute().execute(colocatedTarget, job, "input");

// Execute on nodes matching criteria
JobTarget filteredTarget = JobTarget.nodes(
    client.clusterNodes().stream()
        .filter(node -> node.name().contains("worker"))
        .collect(Collectors.toSet())
);
```

#### Advanced Targeting Examples

```java
public class ComputeTargeting {
    
    // Round-robin job distribution
    private int nodeIndex = 0;
    
    public JobTarget roundRobinTarget(IgniteClient client) {
        List<ClusterNode> nodes = new ArrayList<>(client.clusterNodes());
        if (nodes.isEmpty()) {
            throw new IllegalStateException("No cluster nodes available");
        }
        
        ClusterNode selectedNode = nodes.get(nodeIndex % nodes.size());
        nodeIndex++;
        
        return JobTarget.node(selectedNode);
    }
    
    // Target based on resource availability
    public JobTarget resourceAwareTarget(IgniteClient client) {
        return client.clusterNodes().stream()
            .filter(node -> isNodeHealthy(node))
            .findFirst()
            .map(JobTarget::node)
            .orElse(JobTarget.anyNode(client.clusterNodes()));
    }
    
    // Data-aware targeting for optimal performance
    public JobTarget dataAwareTarget(IgniteClient client, String tableName, Object key) {
        // Execute where the data is located
        return JobTarget.colocated(tableName, key);
    }
    
    private boolean isNodeHealthy(ClusterNode node) {
        // Implement health check logic
        return true; // Simplified
    }
}
```

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

#### Basic Input/Output Patterns

```java
// Job with simple input/output
public static class DataProcessingJob implements ComputeJob<String, ProcessingResult> {
    @Override
    public CompletableFuture<ProcessingResult> executeAsync(
            JobExecutionContext context, String inputData) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Process the input data
                String processed = processData(inputData);
                
                // Return structured result
                return new ProcessingResult(
                    processed, 
                    System.currentTimeMillis(),
                    context.jobId().toString()
                );
                
            } catch (Exception e) {
                throw new RuntimeException("Processing failed", e);
            }
        });
    }
    
    private String processData(String input) {
        // Simulate data processing
        return input.toUpperCase() + "_PROCESSED";
    }
}

// Result class
public static class ProcessingResult {
    private final String result;
    private final long timestamp;
    private final String jobId;
    
    public ProcessingResult(String result, long timestamp, String jobId) {
        this.result = result;
        this.timestamp = timestamp;
        this.jobId = jobId;
    }
    
    // Getters
    public String getResult() { return result; }
    public long getTimestamp() { return timestamp; }
    public String getJobId() { return jobId; }
    
    @Override
    public String toString() {
        return "ProcessingResult{result='" + result + "', timestamp=" + timestamp + "'}";
    }
}
```

#### Complex Input/Output with Collections

```java
// Job that processes collections
public static class BatchProcessingJob implements ComputeJob<List<String>, Map<String, Integer>> {
    @Override
    public CompletableFuture<Map<String, Integer>> executeAsync(
            JobExecutionContext context, List<String> inputList) {
        
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Integer> results = new HashMap<>();
            
            for (String item : inputList) {
                // Process each item
                results.put(item, item.length());
            }
            
            return results;
        });
    }
}

// Usage
JobDescriptor<List<String>, Map<String, Integer>> batchJob = 
    JobDescriptor.builder(BatchProcessingJob.class).build();

List<String> inputData = Arrays.asList("apple", "banana", "cherry");
Map<String, Integer> result = client.compute().execute(
    JobTarget.anyNode(client.clusterNodes()), batchJob, inputData);

result.forEach((key, value) -> 
    System.out.println(key + " has length: " + value));
```

#### Database Access in Compute Jobs

```java
// Job that accesses database during execution
public static class DatabaseJob implements ComputeJob<Integer, String> {
    @Override
    public CompletableFuture<String> executeAsync(
            JobExecutionContext context, Integer artistId) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Access the database from within the job
                // Note: In real scenarios, you'd inject the client or use service lookup
                return queryArtistName(artistId);
                
            } catch (Exception e) {
                throw new RuntimeException("Database access failed", e);
            }
        });
    }
    
    private String queryArtistName(Integer artistId) {
        // Simulate database access
        return "Artist " + artistId;
    }
}
```

#### Serialization Considerations

```java
// Ensure input/output classes are serializable
public static class SerializableInput implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String data;
    private final Map<String, Object> metadata;
    
    public SerializableInput(String data, Map<String, Object> metadata) {
        this.data = data;
        this.metadata = new HashMap<>(metadata); // Defensive copy
    }
    
    // Getters
    public String getData() { return data; }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
}

public static class SerializableOutput implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String processedData;
    private final boolean success;
    private final String errorMessage;
    
    public SerializableOutput(String processedData, boolean success, String errorMessage) {
        this.processedData = processedData;
        this.success = success;
        this.errorMessage = errorMessage;
    }
    
    // Getters and methods
    public boolean isSuccess() { return success; }
    public String getProcessedData() { return processedData; }
    public String getErrorMessage() { return errorMessage; }
}
```

### Error Handling in Jobs

#### Basic Error Handling

```java
public static class RobustComputeJob implements ComputeJob<String, String> {
    @Override
    public CompletableFuture<String> executeAsync(
            JobExecutionContext context, String input) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate input
                if (input == null || input.trim().isEmpty()) {
                    throw new IllegalArgumentException("Invalid input data");
                }
                
                // Process data with potential failures
                return processWithErrorHandling(input);
                
            } catch (IllegalArgumentException e) {
                throw new ComputeException("Input validation failed: " + e.getMessage());
            } catch (Exception e) {
                throw new ComputeException("Processing failed: " + e.getMessage(), e);
            }
        });
    }
    
    private String processWithErrorHandling(String input) {
        try {
            // Simulate operation that might fail
            if (input.contains("error")) {
                throw new RuntimeException("Simulated processing error");
            }
            
            return "Processed: " + input;
            
        } catch (RuntimeException e) {
            // Log error and provide fallback
            System.err.println("Processing error: " + e.getMessage());
            return "Error processing: " + input;
        }
    }
}

// Custom exception for compute operations
public static class ComputeException extends RuntimeException {
    public ComputeException(String message) {
        super(message);
    }
    
    public ComputeException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

#### Error Handling with Retry Logic

```java
public static class RetryableComputeJob implements ComputeJob<String, String> {
    private static final int MAX_RETRIES = 3;
    
    @Override
    public CompletableFuture<String> executeAsync(
            JobExecutionContext context, String input) {
        
        return executeWithRetry(input, 0);
    }
    
    private CompletableFuture<String> executeWithRetry(String input, int attempt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return processData(input);
                
            } catch (TransientException e) {
                if (attempt < MAX_RETRIES) {
                    System.out.println("Retry attempt " + (attempt + 1) + " for: " + input);
                    // Exponential backoff
                    try {
                        Thread.sleep(1000 * (1L << attempt));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry", ie);
                    }
                    return executeWithRetry(input, attempt + 1).join();
                } else {
                    throw new RuntimeException("Max retries exceeded", e);
                }
            }
        });
    }
    
    private String processData(String input) throws TransientException {
        // Simulate transient failure
        if (Math.random() < 0.3) {
            throw new TransientException("Temporary failure");
        }
        return "Processed: " + input;
    }
    
    public static class TransientException extends Exception {
        public TransientException(String message) {
            super(message);
        }
    }
}
```

#### Graceful Degradation

```java
public static class FaultTolerantJob implements ComputeJob<String, ProcessingResult> {
    @Override
    public CompletableFuture<ProcessingResult> executeAsync(
            JobExecutionContext context, String input) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Primary processing path
                String result = primaryProcessing(input);
                return new ProcessingResult(result, true, null);
                
            } catch (Exception primaryError) {
                System.err.println("Primary processing failed: " + primaryError.getMessage());
                
                try {
                    // Fallback processing
                    String fallbackResult = fallbackProcessing(input);
                    return new ProcessingResult(fallbackResult, false, 
                        "Used fallback: " + primaryError.getMessage());
                    
                } catch (Exception fallbackError) {
                    // Final fallback - return error state
                    return new ProcessingResult(null, false, 
                        "Both primary and fallback failed: " + fallbackError.getMessage());
                }
            }
        });
    }
    
    private String primaryProcessing(String input) {
        // Main processing logic
        return "Primary: " + input.toUpperCase();
    }
    
    private String fallbackProcessing(String input) {
        // Simplified fallback processing
        return "Fallback: " + input.toLowerCase();
    }
    
    public static class ProcessingResult {
        private final String result;
        private final boolean usedPrimary;
        private final String errorMessage;
        
        public ProcessingResult(String result, boolean usedPrimary, String errorMessage) {
            this.result = result;
            this.usedPrimary = usedPrimary;
            this.errorMessage = errorMessage;
        }
        
        // Getters
        public String getResult() { return result; }
        public boolean isUsedPrimary() { return usedPrimary; }
        public String getErrorMessage() { return errorMessage; }
        public boolean isSuccess() { return result != null; }
    }
}
```

## Async Compute Operations

### Parallel Job Execution

```java
CompletableFuture<Integer> jobFuture = client.compute().executeAsync(jobTarget, job, word);
List<Integer> results = jobFutures.stream()
    .map(CompletableFuture::join)
    .collect(Collectors.toList());
```

### Result Aggregation

#### Map-Reduce Style Aggregation

```java
public class MapReduceExample {
    
    // Map phase job - processes individual chunks
    public static class MapJob implements ComputeJob<List<String>, Map<String, Integer>> {
        @Override
        public CompletableFuture<Map<String, Integer>> executeAsync(
                JobExecutionContext context, List<String> words) {
            
            return CompletableFuture.supplyAsync(() -> {
                Map<String, Integer> wordCounts = new HashMap<>();
                
                for (String word : words) {
                    wordCounts.merge(word.toLowerCase(), 1, Integer::sum);
                }
                
                return wordCounts;
            });
        }
    }
    
    // Reduce phase - aggregate results from multiple map jobs
    public static Map<String, Integer> performMapReduce(
            IgniteClient client, List<List<String>> chunks) {
        
        JobDescriptor<List<String>, Map<String, Integer>> mapJob = 
            JobDescriptor.builder(MapJob.class).build();
        
        // Execute map phase in parallel
        List<CompletableFuture<Map<String, Integer>>> mapFutures = chunks.stream()
            .map(chunk -> client.compute().executeAsync(
                JobTarget.anyNode(client.clusterNodes()), mapJob, chunk))
            .collect(Collectors.toList());
        
        // Wait for all map jobs to complete
        CompletableFuture<Void> allMaps = CompletableFuture.allOf(
            mapFutures.toArray(new CompletableFuture[0]));
        
        // Reduce phase - aggregate all results
        return allMaps.thenApply(ignored -> {
            Map<String, Integer> finalResult = new HashMap<>();
            
            for (CompletableFuture<Map<String, Integer>> future : mapFutures) {
                Map<String, Integer> partialResult = future.join();
                
                partialResult.forEach((word, count) -> 
                    finalResult.merge(word, count, Integer::sum));
            }
            
            return finalResult;
        }).join();
    }
}

// Usage
List<List<String>> wordChunks = Arrays.asList(
    Arrays.asList("hello", "world", "hello"),
    Arrays.asList("world", "ignite", "compute"),
    Arrays.asList("hello", "compute", "distributed")
);

Map<String, Integer> wordCounts = MapReduceExample.performMapReduce(client, wordChunks);
wordCounts.forEach((word, count) -> 
    System.out.println(word + ": " + count));
```

#### Streaming Aggregation

```java
public class StreamingAggregation {
    
    // Job that produces streaming results
    public static class DataGeneratorJob implements ComputeJob<Integer, Stream<String>> {
        @Override
        public CompletableFuture<Stream<String>> executeAsync(
                JobExecutionContext context, Integer count) {
            
            return CompletableFuture.supplyAsync(() -> {
                return IntStream.range(0, count)
                    .mapToObj(i -> "data-" + i)
                    .peek(data -> {
                        // Simulate processing time
                        try { Thread.sleep(10); } catch (InterruptedException e) {}
                    });
            });
        }
    }
    
    // Aggregate streaming results
    public static List<String> aggregateStreamingResults(
            IgniteClient client, int jobCount, int itemsPerJob) {
        
        JobDescriptor<Integer, Stream<String>> job = 
            JobDescriptor.builder(DataGeneratorJob.class).build();
        
        // Submit multiple streaming jobs
        List<CompletableFuture<Stream<String>>> futures = IntStream.range(0, jobCount)
            .mapToObj(i -> client.compute().executeAsync(
                JobTarget.anyNode(client.clusterNodes()), job, itemsPerJob))
            .collect(Collectors.toList());
        
        // Collect and merge all streams
        return futures.stream()
            .map(CompletableFuture::join)
            .flatMap(stream -> stream)
            .collect(Collectors.toList());
    }
}
```

#### Statistical Aggregation

```java
public class StatisticalAggregation {
    
    // Job that calculates statistics for a data chunk
    public static class StatsJob implements ComputeJob<List<Double>, Statistics> {
        @Override
        public CompletableFuture<Statistics> executeAsync(
                JobExecutionContext context, List<Double> numbers) {
            
            return CompletableFuture.supplyAsync(() -> {
                if (numbers.isEmpty()) {
                    return new Statistics(0, 0.0, 0.0, 0.0, 0.0);
                }
                
                int count = numbers.size();
                double sum = numbers.stream().mapToDouble(Double::doubleValue).sum();
                double mean = sum / count;
                double min = numbers.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                double max = numbers.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                
                return new Statistics(count, sum, mean, min, max);
            });
        }
    }
    
    // Statistics data class
    public static class Statistics {
        private final int count;
        private final double sum;
        private final double mean;
        private final double min;
        private final double max;
        
        public Statistics(int count, double sum, double mean, double min, double max) {
            this.count = count;
            this.sum = sum;
            this.mean = mean;
            this.min = min;
            this.max = max;
        }
        
        // Merge statistics from multiple chunks
        public Statistics merge(Statistics other) {
            int totalCount = this.count + other.count;
            double totalSum = this.sum + other.sum;
            double totalMean = totalSum / totalCount;
            double totalMin = Math.min(this.min, other.min);
            double totalMax = Math.max(this.max, other.max);
            
            return new Statistics(totalCount, totalSum, totalMean, totalMin, totalMax);
        }
        
        // Getters
        public int getCount() { return count; }
        public double getSum() { return sum; }
        public double getMean() { return mean; }
        public double getMin() { return min; }
        public double getMax() { return max; }
        
        @Override
        public String toString() {
            return String.format("Statistics{count=%d, sum=%.2f, mean=%.2f, min=%.2f, max=%.2f}", 
                count, sum, mean, min, max);
        }
    }
    
    // Aggregate statistics across multiple data chunks
    public static Statistics calculateDistributedStatistics(
            IgniteClient client, List<List<Double>> dataChunks) {
        
        JobDescriptor<List<Double>, Statistics> statsJob = 
            JobDescriptor.builder(StatsJob.class).build();
        
        // Calculate statistics for each chunk in parallel
        List<CompletableFuture<Statistics>> futures = dataChunks.stream()
            .map(chunk -> client.compute().executeAsync(
                JobTarget.anyNode(client.clusterNodes()), statsJob, chunk))
            .collect(Collectors.toList());
        
        // Aggregate all statistics
        return futures.stream()
            .map(CompletableFuture::join)
            .reduce(new Statistics(0, 0.0, 0.0, Double.MAX_VALUE, Double.MIN_VALUE), 
                   Statistics::merge);
    }
}

// Usage
List<List<Double>> dataChunks = Arrays.asList(
    Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0),
    Arrays.asList(6.0, 7.0, 8.0, 9.0, 10.0),
    Arrays.asList(11.0, 12.0, 13.0, 14.0, 15.0)
);

Statistics result = StatisticalAggregation.calculateDistributedStatistics(client, dataChunks);
System.out.println("Distributed statistics: " + result);
```

## Advanced Topics

### Code Deployment Units

Deployment units allow you to package and deploy code to cluster nodes:

```java
// Create a deployment unit
DeploymentUnit deploymentUnit = new DeploymentUnit(
    "my-compute-jobs",     // Unit name
    "1.0.0"                // Version
);

// Job descriptor with deployment unit
JobDescriptor<String, String> job = JobDescriptor.builder(MyComputeJob.class)
    .units(deploymentUnit)
    .options(JobExecutionOptions.builder()
        .maxRetries(3)
        .priority(1)
        .build())
    .build();

// Execute job
String result = client.compute().execute(
    JobTarget.anyNode(client.clusterNodes()), job, "input");
```

#### Deployment Unit Management

```java
public class DeploymentManager {
    
    // Deploy code to specific nodes
    public static void deployToNodes(IgniteClient client, 
                                   Set<ClusterNode> targetNodes,
                                   DeploymentUnit unit) {
        
        // Deploy unit to specific nodes
        targetNodes.forEach(node -> {
            try {
                deployToNode(client, node, unit);
                System.out.println("Deployed to node: " + node.name());
            } catch (Exception e) {
                System.err.println("Failed to deploy to node " + node.name() + ": " + e.getMessage());
            }
        });
    }
    
    private static void deployToNode(IgniteClient client, ClusterNode node, DeploymentUnit unit) {
        // Implementation would use Ignite's deployment API
        // This is a placeholder for the actual deployment logic
    }
    
    // Version management
    public static void upgradeDeployment(IgniteClient client, 
                                       String unitName, 
                                       String newVersion) {
        
        DeploymentUnit newUnit = new DeploymentUnit(unitName, newVersion);
        
        // Deploy new version
        client.clusterNodes().forEach(node -> {
            try {
                deployToNode(client, node, newUnit);
            } catch (Exception e) {
                System.err.println("Upgrade failed for node " + node.name());
            }
        });
    }
}
```

### Job Cancellation

#### Basic Job Cancellation

```java
public class JobCancellation {
    
    // Long-running job that supports cancellation
    public static class CancellableJob implements ComputeJob<Integer, String> {
        @Override
        public CompletableFuture<String> executeAsync(
                JobExecutionContext context, Integer iterations) {
            
            return CompletableFuture.supplyAsync(() -> {
                for (int i = 0; i < iterations; i++) {
                    // Check for cancellation
                    if (context.isCancelled()) {
                        return "Job cancelled at iteration " + i;
                    }
                    
                    // Simulate work
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return "Job interrupted at iteration " + i;
                    }
                }
                
                return "Job completed after " + iterations + " iterations";
            });
        }
    }
    
    // Demonstrate job cancellation
    public static void demonstrateJobCancellation(IgniteClient client) {
        JobDescriptor<Integer, String> job = JobDescriptor.builder(CancellableJob.class).build();
        
        // Submit long-running job
        CompletableFuture<String> jobFuture = client.compute().executeAsync(
            JobTarget.anyNode(client.clusterNodes()), job, 100);
        
        // Cancel after 2 seconds
        CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS)
            .execute(() -> {
                boolean cancelled = jobFuture.cancel(true);
                System.out.println("Job cancellation requested: " + cancelled);
            });
        
        // Handle result or cancellation
        try {
            String result = jobFuture.get(5, TimeUnit.SECONDS);
            System.out.println("Job result: " + result);
        } catch (CancellationException e) {
            System.out.println("Job was cancelled");
        } catch (TimeoutException e) {
            System.out.println("Job timed out");
            jobFuture.cancel(true);
        } catch (Exception e) {
            System.err.println("Job failed: " + e.getMessage());
        }
    }
}
```

#### Graceful Job Termination

```java
public static class GracefulJob implements ComputeJob<String, String> {
    @Override
    public CompletableFuture<String> executeAsync(
            JobExecutionContext context, String input) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return performWork(context, input);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Job gracefully terminated";
            }
        });
    }
    
    private String performWork(JobExecutionContext context, String input) 
            throws InterruptedException {
        
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < 10; i++) {
            // Check for cancellation frequently
            if (context.isCancelled()) {
                // Perform cleanup
                cleanup(result);
                return "Job cancelled after " + i + " steps, cleanup completed";
            }
            
            // Do work
            result.append("Step ").append(i).append(": ").append(input).append("\n");
            Thread.sleep(500); // Simulate work
        }
        
        return "Job completed: \n" + result.toString();
    }
    
    private void cleanup(StringBuilder partialResult) {
        // Perform necessary cleanup operations
        partialResult.append("[CLEANUP PERFORMED]\n");
    }
}
```

### Job Monitoring and Management

```java
public class JobMonitoring {
    
    // Monitor multiple jobs
    public static void monitorJobs(IgniteClient client, List<String> inputs) {
        JobDescriptor<String, String> job = JobDescriptor.builder(LongRunningJob.class).build();
        
        // Submit multiple jobs
        Map<String, CompletableFuture<String>> jobFutures = inputs.stream()
            .collect(Collectors.toMap(
                input -> input,
                input -> client.compute().executeAsync(
                    JobTarget.anyNode(client.clusterNodes()), job, input)
            ));
        
        // Monitor progress
        monitorProgress(jobFutures);
    }
    
    private static void monitorProgress(Map<String, CompletableFuture<String>> jobFutures) {
        ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
        
        monitor.scheduleAtFixedRate(() -> {
            long completed = jobFutures.values().stream()
                .mapToLong(future -> future.isDone() ? 1 : 0)
                .sum();
            
            System.out.println("Progress: " + completed + "/" + jobFutures.size() + " jobs completed");
            
            if (completed == jobFutures.size()) {
                monitor.shutdown();
                
                // Process results
                jobFutures.forEach((input, future) -> {
                    try {
                        String result = future.get();
                        System.out.println("Job for " + input + ": " + result);
                    } catch (Exception e) {
                        System.err.println("Job for " + input + " failed: " + e.getMessage());
                    }
                });
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
    
    // Job with progress reporting
    public static class LongRunningJob implements ComputeJob<String, String> {
        @Override
        public CompletableFuture<String> executeAsync(
                JobExecutionContext context, String input) {
            
            return CompletableFuture.supplyAsync(() -> {
                // Simulate long-running work with progress
                for (int i = 0; i < 10; i++) {
                    if (context.isCancelled()) {
                        return "Cancelled at step " + i;
                    }
                    
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return "Interrupted at step " + i;
                    }
                }
                
                return "Completed processing: " + input;
            });
        }
    }
}
```
