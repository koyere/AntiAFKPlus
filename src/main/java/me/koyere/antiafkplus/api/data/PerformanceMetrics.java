package me.koyere.antiafkplus.api.data;

import java.util.Map;

/**
 * Performance metrics for the AFK system.
 */
public class PerformanceMetrics {
    private final long totalOperations;
    private final double averageExecutionTime;
    private final long memoryUsage;
    private final double cpuUsage;
    private final int cacheHitRate;
    private final Map<String, Object> moduleMetrics;
    
    public PerformanceMetrics(long totalOperations, double averageExecutionTime, long memoryUsage,
                             double cpuUsage, int cacheHitRate, Map<String, Object> moduleMetrics) {
        this.totalOperations = totalOperations;
        this.averageExecutionTime = averageExecutionTime;
        this.memoryUsage = memoryUsage;
        this.cpuUsage = cpuUsage;
        this.cacheHitRate = cacheHitRate;
        this.moduleMetrics = moduleMetrics;
    }
    
    // Getters
    public long getTotalOperations() { return totalOperations; }
    public double getAverageExecutionTime() { return averageExecutionTime; }
    public long getMemoryUsage() { return memoryUsage; }
    public double getCpuUsage() { return cpuUsage; }
    public int getCacheHitRate() { return cacheHitRate; }
    public Map<String, Object> getModuleMetrics() { return moduleMetrics; }
}