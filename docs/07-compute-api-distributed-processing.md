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
JobTarget colocatedTarget = JobTarget.colocated("Artist", 
    Tuple.create().set("ArtistId", 1));
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
    public JobTarget dataAwareTarget(IgniteClient client, Integer artistId) {
        // Execute where the Artist data is located for optimal performance
        return JobTarget.colocated("Artist", Tuple.create().set("ArtistId", artistId));
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
// Simple job that calculates artist name length
private static class ArtistNameLengthJob implements ComputeJob<String, Integer> {
    @Override
    public CompletableFuture<Integer> executeAsync(JobExecutionContext context, String artistName) {
        return CompletableFuture.completedFuture(artistName.length());
    }
}
```

### Input/Output Handling

#### Basic Input/Output Patterns

```java
// Job that processes artist data
public static class ArtistDataProcessingJob implements ComputeJob<String, ProcessingResult> {
    @Override
    public CompletableFuture<ProcessingResult> executeAsync(
            JobExecutionContext context, String artistName) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Process the artist name data
                String processed = processArtistName(artistName);
                
                // Return structured result
                return new ProcessingResult(
                    processed, 
                    System.currentTimeMillis(),
                    context.jobId().toString()
                );
                
            } catch (Exception e) {
                throw new RuntimeException("Artist processing failed", e);
            }
        });
    }
    
    private String processArtistName(String artistName) {
        // Simulate artist name processing
        return artistName.toUpperCase() + " (VERIFIED)";
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
// Job that processes collections of artist names
public static class ArtistBatchProcessingJob implements ComputeJob<List<String>, Map<String, Integer>> {
    @Override
    public CompletableFuture<Map<String, Integer>> executeAsync(
            JobExecutionContext context, List<String> artistNames) {
        
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Integer> results = new HashMap<>();
            
            for (String artistName : artistNames) {
                // Calculate name length for each artist
                results.put(artistName, artistName.length());
            }
            
            return results;
        });
    }
}

// Usage
JobDescriptor<List<String>, Map<String, Integer>> batchJob = 
    JobDescriptor.builder(ArtistBatchProcessingJob.class).build();

List<String> artistNames = Arrays.asList("AC/DC", "The Beatles", "Led Zeppelin");
Map<String, Integer> result = client.compute().execute(
    JobTarget.anyNode(client.clusterNodes()), batchJob, artistNames);

result.forEach((artist, nameLength) -> 
    System.out.println(artist + " has name length: " + nameLength));
```

#### Database Access in Compute Jobs

```java
// Job that accesses Chinook database during execution
public static class ArtistLookupJob implements ComputeJob<Integer, String> {
    @Override
    public CompletableFuture<String> executeAsync(
            JobExecutionContext context, Integer artistId) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Access the Artist table from within the job
                // Note: In real scenarios, you'd inject the client or use service lookup
                return queryArtistName(artistId);
                
            } catch (Exception e) {
                throw new RuntimeException("Artist lookup failed", e);
            }
        });
    }
    
    private String queryArtistName(Integer artistId) {
        // Simulate Artist database lookup
        switch (artistId) {
            case 1: return "AC/DC";
            case 2: return "Accept";
            case 3: return "Aerosmith";
            case 10: return "The Beatles";
            default: return "Unknown Artist " + artistId;
        }
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
public static class RobustArtistProcessingJob implements ComputeJob<String, String> {
    @Override
    public CompletableFuture<String> executeAsync(
            JobExecutionContext context, String artistName) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate artist name input
                if (artistName == null || artistName.trim().isEmpty()) {
                    throw new IllegalArgumentException("Invalid artist name");
                }
                
                // Process artist name with potential failures
                return processArtistWithErrorHandling(artistName);
                
            } catch (IllegalArgumentException e) {
                throw new ComputeException("Artist validation failed: " + e.getMessage());
            } catch (Exception e) {
                throw new ComputeException("Artist processing failed: " + e.getMessage(), e);
            }
        });
    }
    
    private String processArtistWithErrorHandling(String artistName) {
        try {
            // Simulate operation that might fail for certain artists
            if (artistName.toLowerCase().contains("error")) {
                throw new RuntimeException("Simulated artist processing error");
            }
            
            return "Processed artist: " + artistName + " ✓";
            
        } catch (RuntimeException e) {
            // Log error and provide fallback
            System.err.println("Artist processing error: " + e.getMessage());
            return "Error processing artist: " + artistName;
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
public static class RetryableArtistProcessingJob implements ComputeJob<String, String> {
    private static final int MAX_RETRIES = 3;
    
    @Override
    public CompletableFuture<String> executeAsync(
            JobExecutionContext context, String artistName) {
        
        return executeWithRetry(artistName, 0);
    }
    
    private CompletableFuture<String> executeWithRetry(String artistName, int attempt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return processArtistData(artistName);
                
            } catch (TransientException e) {
                if (attempt < MAX_RETRIES) {
                    System.out.println("Retry attempt " + (attempt + 1) + " for artist: " + artistName);
                    // Exponential backoff
                    try {
                        Thread.sleep(1000 * (1L << attempt));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry", ie);
                    }
                    return executeWithRetry(artistName, attempt + 1).join();
                } else {
                    throw new RuntimeException("Max retries exceeded for artist: " + artistName, e);
                }
            }
        });
    }
    
    private String processArtistData(String artistName) throws TransientException {
        // Simulate transient failure for artist processing
        if (Math.random() < 0.3) {
            throw new TransientException("Temporary artist processing failure");
        }
        return "Processed artist: " + artistName + " ✓";
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
public static class FaultTolerantAlbumProcessingJob implements ComputeJob<String, ProcessingResult> {
    @Override
    public CompletableFuture<ProcessingResult> executeAsync(
            JobExecutionContext context, String albumTitle) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Primary album processing path
                String result = primaryAlbumProcessing(albumTitle);
                return new ProcessingResult(result, true, null);
                
            } catch (Exception primaryError) {
                System.err.println("Primary album processing failed: " + primaryError.getMessage());
                
                try {
                    // Fallback album processing
                    String fallbackResult = fallbackAlbumProcessing(albumTitle);
                    return new ProcessingResult(fallbackResult, false, 
                        "Used fallback: " + primaryError.getMessage());
                    
                } catch (Exception fallbackError) {
                    // Final fallback - return error state
                    return new ProcessingResult(null, false, 
                        "Both primary and fallback failed for album: " + fallbackError.getMessage());
                }
            }
        });
    }
    
    private String primaryAlbumProcessing(String albumTitle) {
        // Main album processing logic
        return "Processed album: " + albumTitle.toUpperCase() + " [FULL ANALYSIS]";
    }
    
    private String fallbackAlbumProcessing(String albumTitle) {
        // Simplified fallback album processing
        return "Basic processing: " + albumTitle + " [BASIC ANALYSIS]";
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
    
    // Map phase job - processes chunks of track names
    public static class TrackAnalysisMapJob implements ComputeJob<List<String>, Map<String, Integer>> {
        @Override
        public CompletableFuture<Map<String, Integer>> executeAsync(
                JobExecutionContext context, List<String> trackNames) {
            
            return CompletableFuture.supplyAsync(() -> {
                Map<String, Integer> wordCounts = new HashMap<>();
                
                for (String trackName : trackNames) {
                    // Count words in track names
                    String[] words = trackName.toLowerCase().split("\\s+");
                    for (String word : words) {
                        wordCounts.merge(word, 1, Integer::sum);
                    }
                }
                
                return wordCounts;
            });
        }
    }
    
    // Reduce phase - aggregate results from multiple map jobs
    public static Map<String, Integer> performTrackWordAnalysis(
            IgniteClient client, List<List<String>> trackNameChunks) {
        
        JobDescriptor<List<String>, Map<String, Integer>> mapJob = 
            JobDescriptor.builder(TrackAnalysisMapJob.class).build();
        
        // Execute map phase in parallel
        List<CompletableFuture<Map<String, Integer>>> mapFutures = trackNameChunks.stream()
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

// Usage with Chinook track names
List<List<String>> trackChunks = Arrays.asList(
    Arrays.asList("For Those About To Rock", "Put The Finger On You", "Let's Get It Up"),
    Arrays.asList("Inject The Venom", "Snowballed", "Evil Walks"),
    Arrays.asList("Breaking The Rules", "Night Of The Long Knives", "Spellbound")
);

Map<String, Integer> wordCounts = MapReduceExample.performTrackWordAnalysis(client, trackChunks);
wordCounts.forEach((word, count) -> 
    System.out.println("Word '" + word + "' appears " + count + " times in track names"));
```

#### Streaming Aggregation

```java
public class StreamingAggregation {
    
    // Job that generates streaming playlist data
    public static class PlaylistGeneratorJob implements ComputeJob<Integer, Stream<String>> {
        @Override
        public CompletableFuture<Stream<String>> executeAsync(
                JobExecutionContext context, Integer trackCount) {
            
            return CompletableFuture.supplyAsync(() -> {
                String[] sampleTracks = {"Bohemian Rhapsody", "Stairway to Heaven", "Hotel California", 
                                       "Sweet Child O' Mine", "Smoke on the Water", "Imagine"};
                
                return IntStream.range(0, trackCount)
                    .mapToObj(i -> sampleTracks[i % sampleTracks.length] + " (" + i + ")")
                    .peek(track -> {
                        // Simulate track processing time
                        try { Thread.sleep(10); } catch (InterruptedException e) {}
                    });
            });
        }
    }
    
    // Aggregate streaming playlist results
    public static List<String> aggregateStreamingPlaylists(
            IgniteClient client, int playlistCount, int tracksPerPlaylist) {
        
        JobDescriptor<Integer, Stream<String>> job = 
            JobDescriptor.builder(PlaylistGeneratorJob.class).build();
        
        // Submit multiple streaming playlist jobs
        List<CompletableFuture<Stream<String>>> futures = IntStream.range(0, playlistCount)
            .mapToObj(i -> client.compute().executeAsync(
                JobTarget.anyNode(client.clusterNodes()), job, tracksPerPlaylist))
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
    
    // Job that calculates statistics for track duration data
    public static class TrackDurationStatsJob implements ComputeJob<List<Double>, Statistics> {
        @Override
        public CompletableFuture<Statistics> executeAsync(
                JobExecutionContext context, List<Double> trackDurations) {
            
            return CompletableFuture.supplyAsync(() -> {
                if (trackDurations.isEmpty()) {
                    return new Statistics(0, 0.0, 0.0, 0.0, 0.0);
                }
                
                int count = trackDurations.size();
                double totalDuration = trackDurations.stream().mapToDouble(Double::doubleValue).sum();
                double avgDuration = totalDuration / count;
                double minDuration = trackDurations.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                double maxDuration = trackDurations.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                
                return new Statistics(count, totalDuration, avgDuration, minDuration, maxDuration);
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
    
    // Aggregate statistics across multiple track duration chunks
    public static Statistics calculateTrackDurationStatistics(
            IgniteClient client, List<List<Double>> durationChunks) {
        
        JobDescriptor<List<Double>, Statistics> statsJob = 
            JobDescriptor.builder(TrackDurationStatsJob.class).build();
        
        // Calculate statistics for each duration chunk in parallel
        List<CompletableFuture<Statistics>> futures = durationChunks.stream()
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

// Usage with track durations (in minutes)
List<List<Double>> durationChunks = Arrays.asList(
    Arrays.asList(3.2, 4.1, 2.8, 5.0, 3.7),    // AC/DC tracks
    Arrays.asList(4.2, 6.3, 3.9, 4.8, 5.1),    // Led Zeppelin tracks
    Arrays.asList(2.9, 3.4, 4.7, 3.1, 5.8)     // Beatles tracks
);

Statistics result = StatisticalAggregation.calculateTrackDurationStatistics(client, durationChunks);
System.out.println("Track duration statistics: " + result);
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
    
    // Long-running track analysis job that supports cancellation
    public static class CancellableTrackAnalysisJob implements ComputeJob<Integer, String> {
        @Override
        public CompletableFuture<String> executeAsync(
                JobExecutionContext context, Integer trackCount) {
            
            return CompletableFuture.supplyAsync(() -> {
                for (int i = 0; i < trackCount; i++) {
                    // Check for cancellation
                    if (context.isCancelled()) {
                        return "Track analysis cancelled after processing " + i + " tracks";
                    }
                    
                    // Simulate track analysis work
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return "Track analysis interrupted after " + i + " tracks";
                    }
                }
                
                return "Track analysis completed for " + trackCount + " tracks";
            });
        }
    }
    
    // Demonstrate track analysis job cancellation
    public static void demonstrateJobCancellation(IgniteClient client) {
        JobDescriptor<Integer, String> job = JobDescriptor.builder(CancellableTrackAnalysisJob.class).build();
        
        // Submit long-running track analysis job
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
public static class GracefulPlaylistAnalysisJob implements ComputeJob<String, String> {
    @Override
    public CompletableFuture<String> executeAsync(
            JobExecutionContext context, String playlistName) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return performPlaylistAnalysis(context, playlistName);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Playlist analysis gracefully terminated";
            }
        });
    }
    
    private String performPlaylistAnalysis(JobExecutionContext context, String playlistName) 
            throws InterruptedException {
        
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < 10; i++) {
            // Check for cancellation frequently
            if (context.isCancelled()) {
                // Perform cleanup
                cleanupPlaylistAnalysis(result);
                return "Playlist analysis cancelled after " + i + " steps, cleanup completed";
            }
            
            // Analyze playlist tracks
            result.append("Analyzing track ").append(i).append(" in playlist: ").append(playlistName).append("\n");
            Thread.sleep(500); // Simulate track analysis
        }
        
        return "Playlist analysis completed: \n" + result.toString();
    }
    
    private void cleanupPlaylistAnalysis(StringBuilder partialResult) {
        // Perform necessary cleanup operations for playlist analysis
        partialResult.append("[PLAYLIST ANALYSIS CLEANUP PERFORMED]\n");
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
