/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apache.ignite.examples.filestreaming;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks streaming performance metrics for file-to-cluster data flow analysis.
 * 
 * Monitors the relationship between file reading rates and cluster ingestion rates
 * to demonstrate backpressure propagation. Key metrics include reading speed,
 * processing throughput, backpressure events, and memory efficiency indicators.
 * 
 * These metrics help demonstrate how reactive streams control upstream data
 * production to match downstream consumption capacity, preventing memory bloat
 * and system overload during high-volume file processing scenarios.
 */
public class StreamingMetrics {
    
    // Core counters
    private final AtomicLong linesRead = new AtomicLong(0);
    private final AtomicLong bytesRead = new AtomicLong(0);
    private final AtomicLong eventsPublished = new AtomicLong(0);
    private final AtomicLong eventsRequested = new AtomicLong(0);
    private final AtomicLong backpressureEvents = new AtomicLong(0);
    
    // Timing and rate tracking
    private final AtomicLong startTime = new AtomicLong(0);
    private final AtomicLong lastRateCalculation = new AtomicLong(0);
    private final AtomicLong lastLinesCount = new AtomicLong(0);
    private final AtomicLong lastPublishedCount = new AtomicLong(0);
    private final AtomicReference<Double> currentFileReadRate = new AtomicReference<>(0.0);
    private final AtomicReference<Double> currentPublishRate = new AtomicReference<>(0.0);
    private final AtomicReference<Double> peakFileReadRate = new AtomicReference<>(0.0);
    private final AtomicReference<Double> peakPublishRate = new AtomicReference<>(0.0);
    
    // Resource monitoring
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
    private final AtomicReference<Double> currentMemoryUsageMB = new AtomicReference<>(0.0);
    private final AtomicReference<Double> peakMemoryUsageMB = new AtomicReference<>(0.0);
    private final AtomicReference<Double> currentCpuUsage = new AtomicReference<>(0.0);
    private final AtomicReference<Double> peakCpuUsage = new AtomicReference<>(0.0);
    private final AtomicLong lastCpuTime = new AtomicLong(0);
    private final AtomicLong lastUpTime = new AtomicLong(0);
    
    // State tracking
    private final AtomicReference<String> currentPhase = new AtomicReference<>("STARTING");
    private volatile boolean active = false;
    
    /**
     * Marks the start of streaming operations and initializes timing.
     */
    public void startStreaming() {
        long now = System.currentTimeMillis();
        startTime.set(now);
        lastRateCalculation.set(now);
        
        // Initialize resource monitoring
        updateResourceMetrics();
        
        active = true;
        currentPhase.set("ACTIVE");
    }
    
    /**
     * Marks the completion of streaming operations.
     */
    public void stopStreaming() {
        // Force a final rate calculation to capture last measurement
        forceRateUpdate();
        active = false;
        currentPhase.set("COMPLETED");
    }
    
    /**
     * Forces an immediate rate calculation regardless of timing.
     */
    public void forceRateUpdate() {
        long now = System.currentTimeMillis();
        long lastCalc = lastRateCalculation.get();
        updateRates(now, lastCalc);
        lastRateCalculation.set(now);
    }
    
    /**
     * Records that a line has been read from the file.
     * 
     * @param lineLength length of the line in bytes
     */
    public void recordLineRead(int lineLength) {
        linesRead.incrementAndGet();
        bytesRead.addAndGet(lineLength);
        updateRatesIfNeeded();
    }
    
    /**
     * Records that an event has been published to the DataStreamer.
     */
    public void recordEventPublished() {
        eventsPublished.incrementAndGet();
        updateRatesIfNeeded();
    }
    
    /**
     * Records that the DataStreamer has requested more events.
     * 
     * @param requestedCount number of events requested
     */
    public void recordEventsRequested(long requestedCount) {
        eventsRequested.addAndGet(requestedCount);
    }
    
    /**
     * Records a backpressure event where the publisher had to wait.
     * Also captures current resource usage to correlate with system constraints.
     */
    public void recordBackpressureEvent() {
        backpressureEvents.incrementAndGet();
        
        // Update resource metrics to capture state during backpressure
        updateResourceMetrics();
        
        // Log backpressure correlation for debugging
        if (backpressureEvents.get() % 100 == 0) { // Every 100th event to avoid spam
            System.out.printf("   !!! Backpressure event #%d - Memory: %.1f MB, CPU: %.1f%%%n",
                backpressureEvents.get(), getCurrentMemoryUsageMB(), getCurrentCpuUsage());
        }
    }
    
    /**
     * Updates the current phase of streaming operation.
     * 
     * @param phase current phase name
     */
    public void setPhase(String phase) {
        currentPhase.set(phase);
    }
    
    /**
     * Updates rate calculations if sufficient time has passed.
     * Rates are calculated every 500ms to capture more granular performance data.
     */
    private void updateRatesIfNeeded() {
        long now = System.currentTimeMillis();
        long lastCalc = lastRateCalculation.get();
        
        // Update rates every 500ms for better granularity
        if (now - lastCalc >= 500 && lastRateCalculation.compareAndSet(lastCalc, now)) {
            updateRates(now, lastCalc);
            updateResourceMetrics();
        }
    }
    
    /**
     * Calculates current rates based on changes since last calculation.
     * 
     * @param currentTime current timestamp for calculation
     * @param lastCalcTime timestamp of last calculation
     */
    private void updateRates(long currentTime, long lastCalcTime) {
        if (lastCalcTime == 0) {
            // First calculation, set baseline
            lastLinesCount.set(linesRead.get());
            lastPublishedCount.set(eventsPublished.get());
            return;
        }
        
        double intervalSeconds = (currentTime - lastCalcTime) / 1000.0;
        if (intervalSeconds <= 0) return;
        
        // Calculate rates based on change since last measurement
        long currentLines = linesRead.get();
        long currentPublished = eventsPublished.get();
        long lastLines = lastLinesCount.getAndSet(currentLines);
        long lastPublished = lastPublishedCount.getAndSet(currentPublished);
        
        double fileRate = (currentLines - lastLines) / intervalSeconds;
        double publishRate = (currentPublished - lastPublished) / intervalSeconds;
        
        currentFileReadRate.set(fileRate);
        currentPublishRate.set(publishRate);
        
        // Track peak rates
        peakFileReadRate.updateAndGet(current -> Math.max(current, fileRate));
        peakPublishRate.updateAndGet(current -> Math.max(current, publishRate));
    }
    
    /**
     * Updates system resource metrics including memory and CPU usage.
     */
    private void updateResourceMetrics() {
        // Memory usage calculation
        long usedMemory = memoryMXBean.getHeapMemoryUsage().getUsed();
        double memoryMB = usedMemory / (1024.0 * 1024.0);
        currentMemoryUsageMB.set(memoryMB);
        peakMemoryUsageMB.updateAndGet(current -> Math.max(current, memoryMB));
        
        // CPU usage calculation (requires some complexity for accuracy)
        updateCpuUsage();
    }
    
    /**
     * Updates CPU usage metrics using system management beans.
     */
    private void updateCpuUsage() {
        try {
            // Try to get process CPU load if available (Java 14+)
            if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunMXBean = 
                    (com.sun.management.OperatingSystemMXBean) osMXBean;
                
                double cpuLoad = sunMXBean.getProcessCpuLoad();
                if (cpuLoad >= 0.0) { // -1.0 means not available
                    double cpuPercent = cpuLoad * 100.0;
                    currentCpuUsage.set(cpuPercent);
                    peakCpuUsage.updateAndGet(current -> Math.max(current, cpuPercent));
                }
            }
        } catch (Exception e) {
            // CPU monitoring not available on this platform, continue without it
        }
    }
    
    // Getter methods for metrics access
    
    public long getLinesRead() { return linesRead.get(); }
    public long getBytesRead() { return bytesRead.get(); }
    public long getEventsPublished() { return eventsPublished.get(); }
    public long getEventsRequested() { return eventsRequested.get(); }
    public long getBackpressureEvents() { return backpressureEvents.get(); }
    
    public double getCurrentFileReadRate() { return currentFileReadRate.get(); }
    public double getCurrentPublishRate() { return currentPublishRate.get(); }
    public double getPeakFileReadRate() { return peakFileReadRate.get(); }
    public double getPeakPublishRate() { return peakPublishRate.get(); }
    
    public double getCurrentMemoryUsageMB() { return currentMemoryUsageMB.get(); }
    public double getPeakMemoryUsageMB() { return peakMemoryUsageMB.get(); }
    public double getCurrentCpuUsage() { return currentCpuUsage.get(); }
    public double getPeakCpuUsage() { return peakCpuUsage.get(); }
    
    public String getCurrentPhase() { return currentPhase.get(); }
    public boolean isActive() { return active; }
    
    /**
     * Gets the elapsed time since streaming started in milliseconds.
     */
    public long getElapsedTime() {
        if (startTime.get() == 0) return 0;
        return System.currentTimeMillis() - startTime.get();
    }
    
    /**
     * Gets the average file reading rate over the entire streaming period.
     */
    public double getAverageFileReadRate() {
        double elapsedSeconds = getElapsedTime() / 1000.0;
        return elapsedSeconds > 0 ? linesRead.get() / elapsedSeconds : 0.0;
    }
    
    /**
     * Gets the average publish rate over the entire streaming period.
     */
    public double getAveragePublishRate() {
        double elapsedSeconds = getElapsedTime() / 1000.0;
        return elapsedSeconds > 0 ? eventsPublished.get() / elapsedSeconds : 0.0;
    }
    
    /**
     * Gets the file reading rate in MB/sec.
     */
    public double getFileReadRateMBPerSec() {
        double elapsedSeconds = getElapsedTime() / 1000.0;
        if (elapsedSeconds <= 0) return 0.0;
        double mbRead = bytesRead.get() / (1024.0 * 1024.0);
        return mbRead / elapsedSeconds;
    }
    
    /**
     * Calculates the backpressure ratio (events requested vs events published).
     * A ratio close to 1.0 indicates efficient flow control.
     */
    public double getBackpressureRatio() {
        long requested = eventsRequested.get();
        long published = eventsPublished.get();
        return requested > 0 ? (double) published / requested : 0.0;
    }
    
    /**
     * Gets the memory efficiency ratio (MB processed per MB used).
     * Higher values indicate better memory efficiency.
     */
    public double getMemoryEfficiencyRatio() {
        double memoryUsed = getPeakMemoryUsageMB();
        double dataProcessed = getBytesRead() / (1024.0 * 1024.0);
        return memoryUsed > 0 ? dataProcessed / memoryUsed : 0.0;
    }
    
    /**
     * Determines if the system appears to be under resource pressure.
     */
    public boolean isUnderResourcePressure() {
        return getCurrentMemoryUsageMB() > 1000 || // > 1GB memory usage
               getCurrentCpuUsage() > 80 ||        // > 80% CPU usage
               getBackpressureEvents() > 100;      // Many backpressure events
    }
    
    /**
     * Returns a formatted summary of current streaming metrics including resource usage.
     */
    public String getFormattedSummary() {
        return String.format(
            "Lines: %,d | Published: %,d | File Rate: %.1f lines/sec | " +
            "Publish Rate: %.1f events/sec | Memory: %.1f MB | CPU: %.1f%% | " +
            "Phase: %s | Backpressure Events: %,d",
            getLinesRead(),
            getEventsPublished(),
            getCurrentFileReadRate(),
            getCurrentPublishRate(),
            getCurrentMemoryUsageMB(),
            getCurrentCpuUsage(),
            getCurrentPhase(),
            getBackpressureEvents()
        );
    }
    
    /**
     * Returns a detailed metrics report for final analysis.
     */
    public String getDetailedReport() {
        return String.format("""
            === File Streaming Metrics Report ===
            Total Lines Read: %,d
            Total Bytes Read: %,d (%.2f MB)
            Total Events Published: %,d
            Total Events Requested: %,d
            Backpressure Events: %,d
            
            File Reading Rate:
              Current: %.1f lines/sec
              Average: %.1f lines/sec
              Peak: %.1f lines/sec
              Data Rate: %.2f MB/sec
            
            Event Publishing Rate:
              Current: %.1f events/sec
              Average: %.1f events/sec
              Peak: %.1f events/sec
            
            System Resources:
              Current Memory: %.1f MB
              Peak Memory: %.1f MB
              Current CPU: %.1f%%
              Peak CPU: %.1f%%
            
            Flow Control:
              Backpressure Ratio: %.3f
              Memory Efficiency: %.2f MB processed/MB used
              Resource Pressure: %s
              Total Elapsed Time: %.2f seconds
              Final Phase: %s
            
            Resource Analysis:
              %s
            """,
            getLinesRead(),
            getBytesRead(),
            getBytesRead() / (1024.0 * 1024.0),
            getEventsPublished(),
            getEventsRequested(),
            getBackpressureEvents(),
            
            getCurrentFileReadRate(),
            getAverageFileReadRate(),
            getPeakFileReadRate(),
            getFileReadRateMBPerSec(),
            
            getCurrentPublishRate(),
            getAveragePublishRate(),
            getPeakPublishRate(),
            
            getCurrentMemoryUsageMB(),
            getPeakMemoryUsageMB(),
            getCurrentCpuUsage(),
            getPeakCpuUsage(),
            
            getBackpressureRatio(),
            getMemoryEfficiencyRatio(),
            isUnderResourcePressure() ? "YES" : "NO",
            getElapsedTime() / 1000.0,
            getCurrentPhase(),
            generateResourceAnalysis()
        );
    }
    
    /**
     * Generates a summary analysis of resource usage patterns.
     */
    private String generateResourceAnalysis() {
        StringBuilder analysis = new StringBuilder();
        
        // Memory analysis
        double memoryGrowth = getPeakMemoryUsageMB() - getCurrentMemoryUsageMB();
        if (memoryGrowth > 50) {
            analysis.append("High memory growth detected (").append(String.format("%.1f", memoryGrowth))
                   .append(" MB increase)\n              ");
        }
        
        // CPU analysis
        if (getPeakCpuUsage() > 50) {
            analysis.append("CPU intensive processing (peak ").append(String.format("%.1f", getPeakCpuUsage()))
                   .append("%)\n              ");
        }
        
        // Backpressure correlation
        long backpressureCount = getBackpressureEvents();
        if (backpressureCount > 0) {
            double backpressureRate = (double) backpressureCount / (getElapsedTime() / 1000.0);
            analysis.append("Backpressure rate: ").append(String.format("%.1f", backpressureRate))
                   .append(" events/sec\n              ");
        }
        
        // Efficiency assessment
        double efficiency = getMemoryEfficiencyRatio();
        if (efficiency > 10) {
            analysis.append("Excellent memory efficiency (").append(String.format("%.1f", efficiency))
                   .append(":1 ratio)");
        } else if (efficiency > 5) {
            analysis.append("Good memory efficiency (").append(String.format("%.1f", efficiency))
                   .append(":1 ratio)");
        } else if (efficiency > 0) {
            analysis.append("Low memory efficiency (").append(String.format("%.1f", efficiency))
                   .append(":1 ratio) - check for memory leaks");
        }
        
        return analysis.length() > 0 ? analysis.toString() : "Normal resource usage patterns";
    }
}