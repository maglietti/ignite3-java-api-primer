# Chapter 6: Building Production Applications

Your music streaming platform started as a proof of concept with 1,000 tracks. Now it serves 10 million users accessing 50 million tracks through complex recommendation algorithms, real-time analytics, and high-throughput streaming pipelines. Individual API features work well in isolation, but production applications require orchestrating multiple distributed systems capabilities into cohesive, scalable solutions.

This chapter demonstrates how the five core modules—Foundation, Schema Design, Data Access, Distributed Operations, and Performance Optimization—work together in production applications. You'll see how architectural decisions from Chapter 1 enable the advanced features in Chapter 5, and learn techniques for building maintainable distributed systems.

## Integrated Architecture Patterns

Production applications don't use individual Ignite 3 features in isolation. Real systems integrate schema design with performance optimization, combine transactional workflows with streaming ingestion, and balance consistency requirements with scalability needs. Understanding these integration approaches prevents architectural mismatches that cause performance bottlenecks and operational complexity.

### Foundation Integration Example

The foundation concepts from Chapter 1 enable advanced features throughout the application stack:

```java
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.client.IgniteClientConfiguration;
import org.apache.ignite.client.ClientConnectionException;

/**
 * Production-ready application orchestrator integrating all core modules.
 * Demonstrates how foundation concepts enable advanced distributed features.
 */
public class MusicPlatformOrchestrator {
    
    private final IgniteClient client;
    private final SchemaManager schemaManager;
    private final DataAccessLayer dataLayer;
    private final TransactionManager txManager;
    private final PerformanceManager perfManager;
    
    public MusicPlatformOrchestrator(String[] clusterNodes) {
        this.client = createProductionClient(clusterNodes);
        
        // Initialize integrated subsystems
        this.schemaManager = new SchemaManager(client);
        this.dataLayer = new DataAccessLayer(client);
        this.txManager = new TransactionManager(client);
        this.perfManager = new PerformanceManager(client);
    }
    
    /**
     * Production client configuration with fault tolerance and monitoring.
     * Foundation concepts that support all advanced features.
     */
    private IgniteClient createProductionClient(String[] nodes) {
        return IgniteClient.builder()
            .addresses(nodes)
            .reconnectThrottlingPeriod(2000)    // Chapter 1: Connection resilience
            .heartbeatInterval(10000)           // Chapter 1: Cluster health monitoring
            .connectTimeout(30000)              // Chapter 1: Reasonable timeout
            .ssl(configureSSL())                // Production security
            .metricsEnabled(true)               // Chapter 5: Performance monitoring
            .build();
    }
    
    /**
     * Initialize complete music platform with integrated subsystems.
     * Shows how modules work together for production deployment.
     */
    public void initializePlatform() {
        try {
            // Phase 1: Schema and Distribution (Chapter 2)
            System.out.println("=== [1/5] Initializing Schema and Distribution");
            schemaManager.createOptimizedZones();
            schemaManager.createProductionTables();
            schemaManager.createPerformanceIndexes();
            
            // Phase 2: Data Access Layer (Chapter 3)
            System.out.println("=== [2/5] Configuring Data Access Layer");
            dataLayer.initializeConnectionPools();
            dataLayer.configureCachingStrategies();
            dataLayer.validateDataIntegrity();
            
            // Phase 3: Transaction Management (Chapter 4)
            System.out.println("=== [3/5] Setting Up Transaction Management");
            txManager.configureIsolationLevels();
            txManager.setupDeadlockDetection();
            txManager.initializeWorkflowPatterns();
            
            // Phase 4: Performance Optimization (Chapter 5)
            System.out.println("=== [4/5] Optimizing Performance Systems");
            perfManager.initializeStreamingPipelines();
            perfManager.configureQueryOptimization();
            perfManager.setupPerformanceMonitoring();
            
            // Phase 5: Integration Validation
            System.out.println("=== [5/5] Validating Integrated Systems");
            validateIntegratedSystems();
            
            System.out.println(">>> Music platform initialized successfully");
            
        } catch (Exception e) {
            System.err.println("!!! Platform initialization failed: " + e.getMessage());
            throw new RuntimeException("Failed to initialize music platform", e);
        }
    }
    
    /**
     * Execute complex business workflow integrating all modules.
     * Demonstrates real-world usage scenarios combining multiple features.
     */
    public void processCustomerPurchaseWorkflow(int customerId, List<Integer> trackIds) {
        // Chapter 4: Distributed transaction across multiple tables
        try (Transaction tx = client.transactions().begin()) {
            
            // Chapter 3: Table API for object retrieval
            Customer customer = dataLayer.getCustomer(customerId);
            if (customer == null) {
                throw new BusinessException("Customer not found: " + customerId);
            }
            
            // Chapter 2: Colocation strategy ensures invoice and customer are on same node
            Invoice invoice = createInvoice(customer, tx);
            
            // Chapter 3: SQL API for complex pricing calculations
            List<Track> tracks = dataLayer.getTracksWithPricing(trackIds);
            BigDecimal totalAmount = calculateTotalAmount(tracks);
            
            // Chapter 4: Transactional consistency across invoice and line items
            for (Track track : tracks) {
                InvoiceLine line = new InvoiceLine(
                    invoice.getInvoiceId(),
                    track.getTrackId(),
                    track.getUnitPrice(),
                    1
                );
                dataLayer.insertInvoiceLine(line, tx);
            }
            
            // Update invoice total and commit
            invoice.setTotal(totalAmount);
            dataLayer.updateInvoice(invoice, tx);
            
            tx.commit();
            
            // Chapter 5: Async streaming for real-time analytics
            perfManager.streamPurchaseEvent(new PurchaseEvent(
                customerId, invoice.getInvoiceId(), trackIds, totalAmount
            ));
            
            System.out.printf(">>> Purchase completed: Customer %d, Invoice %d, Total $%.2f%n",
                customerId, invoice.getInvoiceId(), totalAmount);
                
        } catch (Exception e) {
            System.err.println("!!! Purchase workflow failed: " + e.getMessage());
            throw new BusinessException("Purchase processing failed", e);
        }
    }
    
    /**
     * Generate music recommendations using integrated compute and analytics.
     * Shows Chapter 4 (Compute) with Chapter 5 (Performance) optimization.
     */
    public List<TrackRecommendation> generateRecommendations(int customerId) {
        try {
            // Chapter 4: Compute API for collaborative filtering
            CompletableFuture<List<Integer>> similarCustomers = 
                client.compute().executeAsync(
                    Set.of(getCustomerNode(customerId)),
                    "FindSimilarCustomers",
                    customerId
                );
            
            // Chapter 5: Optimized query with proper indexes
            CompletableFuture<List<TrackRecommendation>> recommendations = 
                similarCustomers.thenCompose(customers -> 
                    dataLayer.getTrackRecommendationsOptimized(customers, customerId)
                );
            
            // Chapter 5: Caching for performance
            List<TrackRecommendation> results = recommendations.get(5, TimeUnit.SECONDS);
            perfManager.cacheRecommendations(customerId, results);
            
            System.out.printf(">>> Generated %d recommendations for customer %d%n", 
                results.size(), customerId);
            
            return results;
            
        } catch (Exception e) {
            System.err.println("!!! Recommendation generation failed: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Process high-volume streaming events with integrated pipeline.
     * Combines Chapter 5 streaming with Chapter 4 batch processing.
     */
    public void processStreamingEvents() {
        // Chapter 5: Data streaming for real-time ingestion
        DataStreamer<Integer, PlayEvent> streamer = client.dataStreamer(
            "PlayEvent", 
            ReceiverDescriptor.builder(PlayEventReceiver.class).build()
        );
        
        // Configure streaming pipeline
        streamer.batchSize(1000);           // Chapter 5: Optimal batch sizing
        streamer.autoFlushFrequency(500);   // Chapter 5: Flow control
        
        // Chapter 4: Periodic batch analytics
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Chapter 4: Compute API for distributed analytics
                client.compute().executeAsync(
                    client.clusterNodes(),
                    "AnalyzePlayPatterns",
                    System.currentTimeMillis()
                );
            } catch (Exception e) {
                System.err.println("!!! Analytics processing failed: " + e.getMessage());
            }
        }, 0, 60, TimeUnit.SECONDS);
        
        System.out.println(">>> Streaming pipeline initialized with integrated analytics");
    }
    
    private void validateIntegratedSystems() {
        // Validate schema integrity
        if (!schemaManager.validateSchemaConsistency()) {
            throw new RuntimeException("Schema validation failed");
        }
        
        // Validate data access functionality  
        if (!dataLayer.validateDataAccess()) {
            throw new RuntimeException("Data access validation failed");
        }
        
        // Validate transaction capabilities
        if (!txManager.validateTransactionSupport()) {
            throw new RuntimeException("Transaction validation failed");
        }
        
        // Validate performance systems
        if (!perfManager.validatePerformanceSystems()) {
            throw new RuntimeException("Performance validation failed");
        }
        
        System.out.println("<<< All integrated systems validated successfully");
    }
    
    public void shutdown() {
        try {
            perfManager.shutdown();
            txManager.shutdown();
            dataLayer.shutdown();
            schemaManager.shutdown();
            client.close();
            
            System.out.println(">>> Music platform shutdown complete");
            
        } catch (Exception e) {
            System.err.println("!!! Shutdown error: " + e.getMessage());
        }
    }
}
```

## Production Deployment Patterns

Moving from development to production requires systematic application of distributed systems techniques. Production deployments handle variable load, network failures, and data growth while maintaining consistency and performance requirements.

### Operational Patterns

```java
/**
 * Production operations manager for music platform deployment.
 * Integrates monitoring, maintenance, and operational procedures.
 */
public class ProductionOperationsManager {
    
    private final IgniteClient client;
    private final MetricsCollector metricsCollector;
    private final AlertingSystem alertingSystem;
    
    public ProductionOperationsManager(IgniteClient client) {
        this.client = client;
        this.metricsCollector = new MetricsCollector(client);
        this.alertingSystem = new AlertingSystem();
    }
    
    /**
     * Comprehensive health monitoring integrating all system components.
     * Monitors capabilities from all five modules for production readiness.
     */
    public SystemHealthReport performHealthCheck() {
        SystemHealthReport report = new SystemHealthReport();
        
        // Chapter 1: Foundation health
        report.setClusterHealth(checkClusterHealth());
        report.setConnectionHealth(checkConnectionHealth());
        
        // Chapter 2: Schema health  
        report.setSchemaHealth(checkSchemaHealth());
        report.setDistributionHealth(checkDistributionHealth());
        
        // Chapter 3: Data access health
        report.setTableApiHealth(checkTableApiHealth());
        report.setSqlApiHealth(checkSqlApiHealth());
        
        // Chapter 4: Transaction health
        report.setTransactionHealth(checkTransactionHealth());
        report.setComputeHealth(checkComputeHealth());
        
        // Chapter 5: Performance health
        report.setStreamingHealth(checkStreamingHealth());
        report.setQueryPerformanceHealth(checkQueryPerformanceHealth());
        
        // Generate alerts for unhealthy components
        generateHealthAlerts(report);
        
        return report;
    }
    
    /**
     * Performance monitoring across all operational patterns.
     * Tracks metrics essential for production system health.
     */
    public void startPerformanceMonitoring() {
        ScheduledExecutorService monitor = Executors.newScheduledThreadPool(3);
        
        // Real-time cluster metrics
        monitor.scheduleAtFixedRate(() -> {
            ClusterMetrics metrics = collectClusterMetrics();
            metricsCollector.recordClusterMetrics(metrics);
            
            if (metrics.getCpuUsage() > 80.0) {
                alertingSystem.sendAlert("High CPU usage detected: " + metrics.getCpuUsage() + "%");
            }
            
            if (metrics.getMemoryUsage() > 85.0) {
                alertingSystem.sendAlert("High memory usage detected: " + metrics.getMemoryUsage() + "%");
            }
            
        }, 0, 30, TimeUnit.SECONDS);
        
        // Query performance tracking
        monitor.scheduleAtFixedRate(() -> {
            QueryPerformanceMetrics queryMetrics = collectQueryMetrics();
            metricsCollector.recordQueryMetrics(queryMetrics);
            
            if (queryMetrics.getAverageQueryTime() > 1000) {
                alertingSystem.sendAlert("Slow queries detected: avg " + queryMetrics.getAverageQueryTime() + "ms");
            }
            
        }, 0, 60, TimeUnit.SECONDS);
        
        // Streaming pipeline health
        monitor.scheduleAtFixedRate(() -> {
            StreamingMetrics streamingMetrics = collectStreamingMetrics();
            metricsCollector.recordStreamingMetrics(streamingMetrics);
            
            if (streamingMetrics.getBacklogSize() > 10000) {
                alertingSystem.sendAlert("Streaming backlog growing: " + streamingMetrics.getBacklogSize() + " events");
            }
            
        }, 0, 45, TimeUnit.SECONDS);
        
        System.out.println(">>> Production monitoring started");
    }
    
    /**
     * Automated maintenance procedures for production stability.
     * Maintains system health using patterns from all modules.
     */
    public void performMaintenanceProcedures() {
        try {
            System.out.println("=== [1/4] Schema Maintenance");
            // Chapter 2: Schema optimization
            maintainSchemaHealth();
            rebuildDegradedIndexes();
            
            System.out.println("=== [2/4] Data Maintenance");  
            // Chapter 3: Data integrity
            validateDataConsistency();
            cleanupOrphanedRecords();
            
            System.out.println("=== [3/4] Transaction Maintenance");
            // Chapter 4: Transaction health
            cleanupStaleLocks();
            optimizeTransactionLogs();
            
            System.out.println("=== [4/4] Performance Maintenance");
            // Chapter 5: Performance optimization
            updateQueryStatistics();
            optimizeDataDistribution();
            
            System.out.println(">>> Maintenance procedures completed successfully");
            
        } catch (Exception e) {
            System.err.println("!!! Maintenance failed: " + e.getMessage());
            alertingSystem.sendCriticalAlert("Maintenance procedure failure: " + e.getMessage());
        }
    }
    
    /**
     * Capacity planning using integrated system metrics.
     * Predicts scaling requirements based on operational patterns.
     */
    public CapacityPlanningReport generateCapacityPlan() {
        CapacityPlanningReport report = new CapacityPlanningReport();
        
        // Analyze growth trends
        DataGrowthTrend dataTrend = analyzeDataGrowth();
        QueryLoadTrend queryTrend = analyzeQueryLoad();
        StreamingTrend streamingTrend = analyzeStreamingLoad();
        
        // Project capacity requirements
        report.setProjectedDataSize(dataTrend.projectDataSize(90)); // 90 days
        report.setProjectedQueryLoad(queryTrend.projectQueryLoad(90));
        report.setProjectedStreamingLoad(streamingTrend.projectStreamingLoad(90));
        
        // Recommend scaling actions
        if (report.getProjectedDataSize() > getCurrentCapacity() * 0.8) {
            report.addRecommendation("Scale storage capacity by 40% within 60 days");
        }
        
        if (report.getProjectedQueryLoad() > getCurrentQueryCapacity() * 0.75) {
            report.addRecommendation("Add 2 query nodes for read scaling within 45 days");
        }
        
        if (report.getProjectedStreamingLoad() > getCurrentStreamingCapacity() * 0.85) {
            report.addRecommendation("Increase streaming partitions and add processing nodes");
        }
        
        return report;
    }
    
    private ClusterMetrics collectClusterMetrics() {
        // Implementation would collect real cluster metrics
        return new ClusterMetrics(
            getCpuUsage(),
            getMemoryUsage(), 
            getDiskUsage(),
            getNetworkThroughput()
        );
    }
}
```

## Scaling Strategies

Production systems must handle growth in data volume, user load, and operational complexity. Scaling requires understanding how individual features interact and planning capacity increases that maintain performance characteristics.

### Horizontal Scaling Patterns

```java
/**
 * Scaling strategy manager for production music platform.
 * Demonstrates scaling patterns that preserve system characteristics.
 */
public class ScalingStrategyManager {
    
    private final IgniteClient client;
    private final ClusterTopologyManager topologyManager;
    
    public ScalingStrategyManager(IgniteClient client) {
        this.client = client;
        this.topologyManager = new ClusterTopologyManager(client);
    }
    
    /**
     * Scale cluster based on integrated system metrics.
     * Uses patterns from all modules to determine optimal scaling approach.
     */
    public ScalingPlan createScalingPlan(SystemMetrics metrics) {
        ScalingPlan plan = new ScalingPlan();
        
        // Chapter 2: Schema-aware scaling
        if (metrics.getDataGrowthRate() > 0.5) { // 50% growth
            plan.addAction(new ScalingAction(
                "Add data nodes",
                "Increase partition distribution capacity",
                ScalingAction.Priority.HIGH,
                () -> scaleDataNodes(metrics.getProjectedDataSize())
            ));
        }
        
        // Chapter 3: API load scaling
        if (metrics.getQueryLoadGrowth() > 0.3) { // 30% query growth
            plan.addAction(new ScalingAction(
                "Add query processing nodes",
                "Scale read capacity for Table and SQL APIs",
                ScalingAction.Priority.MEDIUM,
                () -> scaleQueryNodes(metrics.getProjectedQueryLoad())
            ));
        }
        
        // Chapter 4: Transaction scaling
        if (metrics.getTransactionThroughputGrowth() > 0.4) { // 40% transaction growth
            plan.addAction(new ScalingAction(
                "Optimize transaction coordinators",
                "Scale transaction processing capacity",
                ScalingAction.Priority.HIGH,
                () -> scaleTransactionCapacity(metrics.getProjectedTransactionLoad())
            ));
        }
        
        // Chapter 5: Streaming scaling
        if (metrics.getStreamingLoadGrowth() > 0.6) { // 60% streaming growth
            plan.addAction(new ScalingAction(
                "Scale streaming infrastructure",
                "Increase ingestion and processing capacity",
                ScalingAction.Priority.CRITICAL,
                () -> scaleStreamingCapacity(metrics.getProjectedStreamingLoad())
            ));
        }
        
        return plan;
    }
    
    /**
     * Execute scaling plan with minimal service disruption.
     * Maintains system availability during scaling operations.
     */
    public void executeScalingPlan(ScalingPlan plan) {
        System.out.println("=== Executing Scaling Plan");
        
        // Execute actions by priority
        plan.getActionsByPriority(ScalingAction.Priority.CRITICAL)
            .forEach(this::executeScalingAction);
            
        plan.getActionsByPriority(ScalingAction.Priority.HIGH)
            .forEach(this::executeScalingAction);
            
        plan.getActionsByPriority(ScalingAction.Priority.MEDIUM)
            .forEach(this::executeScalingAction);
        
        // Validate scaling results
        validateScalingResults(plan);
        
        System.out.println(">>> Scaling plan execution completed");
    }
    
    /**
     * Data node scaling preserves colocation patterns from Chapter 2.
     * Maintains performance characteristics during cluster expansion.
     */
    private void scaleDataNodes(long projectedDataSize) {
        try {
            System.out.println("--- Scaling data nodes for capacity: " + projectedDataSize + " bytes");
            
            // Calculate required nodes based on data size and replication
            int currentNodes = topologyManager.getDataNodeCount();
            int requiredNodes = calculateRequiredDataNodes(projectedDataSize);
            int nodesToAdd = requiredNodes - currentNodes;
            
            if (nodesToAdd > 0) {
                // Add nodes gradually to maintain stability
                for (int i = 0; i < nodesToAdd; i++) {
                    String nodeId = topologyManager.addDataNode();
                    
                    // Wait for node to join and rebalance
                    waitForNodeIntegration(nodeId);
                    
                    System.out.printf(">>> Added data node: %s (%d/%d)%n", nodeId, i + 1, nodesToAdd);
                }
                
                // Rebalance data distribution after all nodes added
                rebalanceDataDistribution();
                
            } else {
                System.out.println(">>> Current data node capacity sufficient");
            }
            
        } catch (Exception e) {
            System.err.println("!!! Data node scaling failed: " + e.getMessage());
            throw new ScalingException("Failed to scale data nodes", e);
        }
    }
    
    /**
     * Query node scaling optimizes for Chapter 3 API patterns.
     * Adds read capacity without affecting write performance.
     */
    private void scaleQueryNodes(double projectedQueryLoad) {
        try {
            System.out.println("--- Scaling query nodes for load: " + projectedQueryLoad + " QPS");
            
            int currentQueryNodes = topologyManager.getQueryNodeCount(); 
            int requiredQueryNodes = calculateRequiredQueryNodes(projectedQueryLoad);
            int nodesToAdd = requiredQueryNodes - currentQueryNodes;
            
            if (nodesToAdd > 0) {
                for (int i = 0; i < nodesToAdd; i++) {
                    String nodeId = topologyManager.addQueryNode();
                    
                    // Configure node for optimal query processing
                    configureQueryNode(nodeId);
                    
                    System.out.printf(">>> Added query node: %s (%d/%d)%n", nodeId, i + 1, nodesToAdd);
                }
                
                // Update client connection pool
                updateClientConnections();
                
            } else {
                System.out.println(">>> Current query node capacity sufficient");
            }
            
        } catch (Exception e) {
            System.err.println("!!! Query node scaling failed: " + e.getMessage());
            throw new ScalingException("Failed to scale query nodes", e);
        }
    }
    
    /**
     * Streaming capacity scaling for Chapter 5 performance requirements.
     * Maintains throughput characteristics during infrastructure changes.
     */
    private void scaleStreamingCapacity(double projectedStreamingLoad) {
        try {
            System.out.println("--- Scaling streaming capacity for load: " + projectedStreamingLoad + " events/sec");
            
            // Scale partition count for better parallelism
            int currentPartitions = getStreamingPartitionCount();
            int requiredPartitions = calculateRequiredPartitions(projectedStreamingLoad);
            
            if (requiredPartitions > currentPartitions) {
                scaleStreamingPartitions(requiredPartitions);
            }
            
            // Scale processing nodes
            int currentProcessingNodes = topologyManager.getStreamingProcessingNodeCount();
            int requiredProcessingNodes = calculateRequiredProcessingNodes(projectedStreamingLoad);
            
            if (requiredProcessingNodes > currentProcessingNodes) {
                addStreamingProcessingNodes(requiredProcessingNodes - currentProcessingNodes);
            }
            
            System.out.println(">>> Streaming capacity scaling completed");
            
        } catch (Exception e) {
            System.err.println("!!! Streaming capacity scaling failed: " + e.getMessage());
            throw new ScalingException("Failed to scale streaming capacity", e);
        }
    }
    
    private void executeScalingAction(ScalingAction action) {
        try {
            System.out.printf(">>> Executing: %s%n", action.getDescription());
            action.execute();
            System.out.printf("<<< Completed: %s%n", action.getDescription());
            
        } catch (Exception e) {
            System.err.printf("!!! Failed: %s - %s%n", action.getDescription(), e.getMessage());
            throw new ScalingException("Scaling action failed: " + action.getDescription(), e);
        }
    }
}
```

