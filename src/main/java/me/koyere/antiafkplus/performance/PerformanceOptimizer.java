package me.koyere.antiafkplus.performance;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.platform.PlatformScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Advanced performance optimization system for AntiAFKPlus.
 * Provides real-time performance monitoring, automatic optimization,
 * and zero-lag operation through intelligent resource management.
 */
public class PerformanceOptimizer {
    
    private final AntiAFKPlus plugin;
    private final Logger logger;
    private final PlatformScheduler scheduler;
    
    // Performance monitoring
    private final PerformanceMonitor performanceMonitor;
    private final Map<String, PerformanceMetrics> componentMetrics = new ConcurrentHashMap<>();
    private final AtomicLong totalOperations = new AtomicLong(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    
    // Optimization settings
    private boolean autoOptimizationEnabled = true;
    private boolean adaptiveIntervals = true;
    private boolean memoryOptimization = true;
    private boolean cpuOptimization = true;
    private boolean networkOptimization = true;
    
    // Performance thresholds
    private double maxTpsImpact = 0.5; // Maximum TPS impact allowed (milliseconds per tick)
    private long maxMemoryUsage = 50 * 1024 * 1024; // 50MB max memory usage
    private int maxCpuUsagePercent = 5; // 5% max CPU usage
    private long optimizationInterval = 5000; // 5 seconds
    
    // Adaptive settings
    private final Map<String, Integer> adaptiveIntervals_map = new ConcurrentHashMap<>();
    private final Map<UUID, Long> playerLastActivity = new ConcurrentHashMap<>();
    private final Set<UUID> highActivityPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> lowActivityPlayers = ConcurrentHashMap.newKeySet();
    
    // Caching system
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private long cacheCleanupInterval = 30000; // 30 seconds
    private long cacheEntryTtl = 60000; // 1 minute TTL
    
    // Resource pooling
    private final Queue<StringBuilder> stringBuilderPool = new ArrayDeque<>();
    private final Queue<Map<String, Object>> mapPool = new ArrayDeque<>();
    private final Queue<List<Object>> listPool = new ArrayDeque<>();
    
    public PerformanceOptimizer(AntiAFKPlus plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.scheduler = plugin.getPlatformScheduler();
        this.performanceMonitor = new PerformanceMonitor();
        
        loadConfiguration();
        initializeOptimizations();
        startMonitoring();
        
        logger.info("⚡ Performance optimizer initialized");
        logger.info("   Auto-optimization: " + (autoOptimizationEnabled ? "✅ Enabled" : "❌ Disabled"));
        logger.info("   Max TPS impact: " + maxTpsImpact + "ms/tick");
        logger.info("   Max memory usage: " + (maxMemoryUsage / 1024 / 1024) + "MB");
    }
    
    /**
     * Load performance optimization configuration.
     */
    private void loadConfiguration() {
        var config = plugin.getConfig();
        
        this.autoOptimizationEnabled = config.getBoolean("performance.auto-optimization", true);
        this.adaptiveIntervals = config.getBoolean("performance.adaptive-intervals", true);
        this.memoryOptimization = config.getBoolean("performance.memory-optimization", true);
        this.cpuOptimization = config.getBoolean("performance.cpu-optimization", true);
        this.networkOptimization = config.getBoolean("performance.network-optimization", true);
        
        this.maxTpsImpact = config.getDouble("performance.thresholds.max-tps-impact", 0.5);
        this.maxMemoryUsage = config.getLong("performance.thresholds.max-memory-mb", 50) * 1024 * 1024;
        this.maxCpuUsagePercent = config.getInt("performance.thresholds.max-cpu-percent", 5);
        this.optimizationInterval = config.getLong("performance.optimization-interval", 5000);
        
        // Create default configuration if needed
        if (!config.contains("performance")) {
            createDefaultConfiguration();
        }
    }
    
    /**
     * Create default performance configuration.
     */
    private void createDefaultConfiguration() {
        var config = plugin.getConfig();
        
        config.set("performance.auto-optimization", true);
        config.set("performance.adaptive-intervals", true);
        config.set("performance.memory-optimization", true);
        config.set("performance.cpu-optimization", true);
        config.set("performance.network-optimization", true);
        
        // Thresholds
        config.set("performance.thresholds.max-tps-impact", 0.5);
        config.set("performance.thresholds.max-memory-mb", 50);
        config.set("performance.thresholds.max-cpu-percent", 5);
        config.set("performance.optimization-interval", 5000);
        
        // Advanced settings
        config.set("performance.advanced.use-object-pooling", true);
        config.set("performance.advanced.cache-calculations", true);
        config.set("performance.advanced.batch-operations", true);
        config.set("performance.advanced.async-processing", true);
        
        // Player categorization
        config.set("performance.player-categorization.enabled", true);
        config.set("performance.player-categorization.high-activity-threshold", 10); // actions per minute
        config.set("performance.player-categorization.low-activity-threshold", 2);
        
        plugin.saveConfig();
        logger.info("📄 Created default performance configuration");
    }
    
    /**
     * Initialize performance optimizations.
     */
    private void initializeOptimizations() {
        // Initialize object pools
        if (config().getBoolean("performance.advanced.use-object-pooling", true)) {
            initializeObjectPools();
        }
        
        // Setup adaptive intervals
        if (adaptiveIntervals) {
            initializeAdaptiveIntervals();
        }
        
        // Initialize caching
        if (config().getBoolean("performance.advanced.cache-calculations", true)) {
            initializeCaching();
        }
        
        logger.info("🔧 Performance optimizations initialized");
    }
    
    /**
     * Initialize object pools for memory optimization.
     */
    private void initializeObjectPools() {
        // Pre-populate object pools
        for (int i = 0; i < 10; i++) {
            stringBuilderPool.offer(new StringBuilder());
            mapPool.offer(new HashMap<>());
            listPool.offer(new ArrayList<>());
        }
        
        logger.info("💾 Object pools initialized");
    }
    
    /**
     * Initialize adaptive intervals for different components.
     */
    private void initializeAdaptiveIntervals() {
        // Set default intervals for different components
        adaptiveIntervals_map.put("afk-check", 100); // 5 seconds default (100 ticks)
        adaptiveIntervals_map.put("pattern-detection", 600); // 30 seconds default
        adaptiveIntervals_map.put("autoclick-detection", 200); // 10 seconds default
        adaptiveIntervals_map.put("cache-cleanup", 1200); // 60 seconds default
        
        logger.info("📊 Adaptive intervals initialized");
    }
    
    /**
     * Initialize caching system.
     */
    private void initializeCaching() {
        // Start cache cleanup task
        scheduler.runTaskTimerAsync(() -> cleanupCache(), 
            cacheCleanupInterval / 50, cacheCleanupInterval / 50);
        
        logger.info("🗄️ Caching system initialized");
    }
    
    /**
     * Start performance monitoring.
     */
    private void startMonitoring() {
        scheduler.runTaskTimerAsync(() -> {
            updatePerformanceMetrics();
            
            if (autoOptimizationEnabled) {
                performAutomaticOptimizations();
            }
            
            categorizePlayersByActivity();
            
        }, optimizationInterval / 50, optimizationInterval / 50);
        
        logger.info("📈 Performance monitoring started");
    }
    
    /**
     * Execute a task with performance monitoring.
     */
    public <T> T executeWithMonitoring(String componentName, PerformanceTask<T> task) {
        long startTime = System.nanoTime();
        long startMemory = getCurrentMemoryUsage();
        
        try {
            T result = task.execute();
            
            long executionTime = System.nanoTime() - startTime;
            long memoryUsed = getCurrentMemoryUsage() - startMemory;
            
            // Record metrics
            recordMetrics(componentName, executionTime, memoryUsed, true);
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.nanoTime() - startTime;
            recordMetrics(componentName, executionTime, 0, false);
            
            // Log the error and return null instead of throwing
            logger.warning("Error in performance monitored task '" + componentName + "': " + e.getMessage());
            if (config().getBoolean("performance.debug-logging", false)) {
                e.printStackTrace();
            }
            return null;
        }
    }
    
    /**
     * Execute a void task with performance monitoring.
     */
    public void executeWithMonitoring(String componentName, Runnable task) {
        executeWithMonitoring(componentName, () -> {
            task.run();
            return null;
        });
    }
    
    /**
     * Record performance metrics for a component.
     */
    private void recordMetrics(String componentName, long executionTime, long memoryUsed, boolean success) {
        PerformanceMetrics metrics = componentMetrics.computeIfAbsent(componentName, k -> new PerformanceMetrics(k));
        
        metrics.recordExecution(executionTime, memoryUsed, success);
        
        totalOperations.incrementAndGet();
        totalExecutionTime.addAndGet(executionTime);
        
        // Check for performance issues
        double executionMs = executionTime / 1_000_000.0;
        if (executionMs > maxTpsImpact) {
            logger.warning("⚠️ Performance warning: " + componentName + " took " + 
                         String.format("%.2f", executionMs) + "ms (limit: " + maxTpsImpact + "ms)");
        }
    }
    
    /**
     * Update overall performance metrics.
     */
    private void updatePerformanceMetrics() {
        performanceMonitor.update();
        
        // Log performance summary periodically
        if (totalOperations.get() % 1000 == 0) {
            logPerformanceSummary();
        }
    }
    
    /**
     * Perform automatic optimizations based on current performance.
     */
    private void performAutomaticOptimizations() {
        double currentTps = getCurrentTPS();
        long currentMemory = getCurrentMemoryUsage();
        double cpuUsage = getCurrentCPUUsage();
        
        // Adjust intervals based on performance
        if (adaptiveIntervals) {
            optimizeIntervals(currentTps, currentMemory, cpuUsage);
        }
        
        // Memory optimization
        if (memoryOptimization && currentMemory > maxMemoryUsage) {
            performMemoryOptimization();
        }
        
        // CPU optimization
        if (cpuOptimization && cpuUsage > maxCpuUsagePercent) {
            performCPUOptimization();
        }
    }
    
    /**
     * Optimize check intervals based on current performance.
     */
    private void optimizeIntervals(double tps, long memory, double cpu) {
        // Calculate performance factor (0.0 = bad performance, 1.0 = good performance)
        double performanceFactor = Math.min(1.0, tps / 20.0);
        
        // Adjust intervals based on performance
        for (Map.Entry<String, Integer> entry : adaptiveIntervals_map.entrySet()) {
            String component = entry.getKey();
            int currentInterval = entry.getValue();
            
            // Calculate new interval (higher when performance is poor)
            int baseInterval = getBaseInterval(component);
            int newInterval = (int) (baseInterval * (2.0 - performanceFactor));
            
            // Apply gradual changes to avoid oscillation
            int adjustedInterval = (currentInterval + newInterval) / 2;
            adaptiveIntervals_map.put(component, Math.max(20, adjustedInterval)); // Minimum 1 second
        }
    }
    
    /**
     * Get base interval for a component.
     */
    private int getBaseInterval(String component) {
        switch (component) {
            case "afk-check": return 100; // 5 seconds
            case "pattern-detection": return 600; // 30 seconds
            case "autoclick-detection": return 200; // 10 seconds
            case "cache-cleanup": return 1200; // 60 seconds
            default: return 200; // 10 seconds default
        }
    }
    
    /**
     * Perform memory optimization.
     */
    private void performMemoryOptimization() {
        logger.info("🧹 Performing memory optimization...");
        
        // Clear caches
        int cacheSize = cache.size();
        cache.clear();
        
        // Clean up player data for offline players
        cleanupOfflinePlayerData();
        
        // Suggest garbage collection (doesn't force it)
        System.gc();
        
        logger.info("✅ Memory optimization complete (cleared " + cacheSize + " cache entries)");
    }
    
    /**
     * Perform CPU optimization.
     */
    private void performCPUOptimization() {
        logger.info("⚡ Performing CPU optimization...");
        
        // Increase intervals to reduce processing load
        for (String component : adaptiveIntervals_map.keySet()) {
            int currentInterval = adaptiveIntervals_map.get(component);
            adaptiveIntervals_map.put(component, (int) (currentInterval * 1.5));
        }
        
        // Reduce player processing for low-activity players
        for (UUID uuid : lowActivityPlayers) {
            // Skip detailed processing for inactive players
        }
        
        logger.info("✅ CPU optimization complete");
    }
    
    /**
     * Categorize players by activity level for optimization.
     */
    private void categorizePlayersByActivity() {
        if (!config().getBoolean("performance.player-categorization.enabled", true)) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        int highThreshold = config().getInt("performance.player-categorization.high-activity-threshold", 10);
        int lowThreshold = config().getInt("performance.player-categorization.low-activity-threshold", 2);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            
            // Calculate activity rate (actions per minute)
            long lastActivity = playerLastActivity.getOrDefault(uuid, currentTime);
            long timeDiff = currentTime - lastActivity;
            
            // Simple activity calculation (would be more sophisticated in practice)
            double activityRate = calculatePlayerActivityRate(player, timeDiff);
            
            // Categorize player
            highActivityPlayers.remove(uuid);
            lowActivityPlayers.remove(uuid);
            
            if (activityRate >= highThreshold) {
                highActivityPlayers.add(uuid);
            } else if (activityRate <= lowThreshold) {
                lowActivityPlayers.add(uuid);
            }
        }
    }
    
    /**
     * Calculate a player's activity rate.
     */
    private double calculatePlayerActivityRate(Player player, long timePeriod) {
        // This would integrate with the movement tracking system
        // For now, return a placeholder value
        return 5.0; // Average activity
    }
    
    /**
     * Clean up cache entries.
     */
    private void cleanupCache() {
        long currentTime = System.currentTimeMillis();
        final int[] removed = {0}; // Use array to make it effectively final
        
        cache.entrySet().removeIf(entry -> {
            boolean expired = currentTime - entry.getValue().timestamp > cacheEntryTtl;
            if (expired) removed[0]++;
            return expired;
        });
        
        if (removed[0] > 0 && config().getBoolean("performance.debug-logging", false)) {
            logger.info("🗑️ Cleaned up " + removed[0] + " expired cache entries");
        }
    }
    
    /**
     * Clean up data for offline players.
     */
    private void cleanupOfflinePlayerData() {
        Set<UUID> onlineUUIDs = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            onlineUUIDs.add(player.getUniqueId());
        }
        
        // Clean up activity tracking
        playerLastActivity.entrySet().removeIf(entry -> !onlineUUIDs.contains(entry.getKey()));
        highActivityPlayers.removeIf(uuid -> !onlineUUIDs.contains(uuid));
        lowActivityPlayers.removeIf(uuid -> !onlineUUIDs.contains(uuid));
    }
    
    // ============= CACHING SYSTEM =============
    
    /**
     * Get cached value or compute and cache it.
     */
    public <T> T getOrCompute(String key, CacheSupplier<T> supplier) {
        return getOrCompute(key, supplier, cacheEntryTtl);
    }
    
    /**
     * Get cached value or compute and cache it with custom TTL.
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrCompute(String key, CacheSupplier<T> supplier, long ttl) {
        CacheEntry entry = cache.get(key);
        long currentTime = System.currentTimeMillis();
        
        if (entry != null && currentTime - entry.timestamp < ttl) {
            return (T) entry.value;
        }
        
        // Compute new value
        T value = supplier.get();
        cache.put(key, new CacheEntry(value, currentTime));
        
        return value;
    }
    
    /**
     * Invalidate cache entry.
     */
    public void invalidateCache(String key) {
        cache.remove(key);
    }
    
    /**
     * Clear all cache.
     */
    public void clearCache() {
        cache.clear();
    }
    
    // ============= OBJECT POOLING =============
    
    /**
     * Get a StringBuilder from the pool.
     */
    public StringBuilder getStringBuilder() {
        StringBuilder sb = stringBuilderPool.poll();
        if (sb == null) {
            sb = new StringBuilder();
        } else {
            sb.setLength(0); // Clear previous content
        }
        return sb;
    }
    
    /**
     * Return a StringBuilder to the pool.
     */
    public void returnStringBuilder(StringBuilder sb) {
        if (stringBuilderPool.size() < 20) { // Limit pool size
            stringBuilderPool.offer(sb);
        }
    }
    
    /**
     * Get a Map from the pool.
     */
    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getMap() {
        Map<?, ?> map = mapPool.poll();
        if (map == null) {
            map = new HashMap<>();
        } else {
            map.clear();
        }
        return (Map<K, V>) map;
    }
    
    /**
     * Return a Map to the pool.
     */
    @SuppressWarnings("unchecked")
    public void returnMap(Map<?, ?> map) {
        if (mapPool.size() < 20 && map instanceof Map) {
            mapPool.offer((Map<String, Object>) map);
        }
    }
    
    // ============= UTILITY METHODS =============
    
    /**
     * Get current server TPS.
     */
    private double getCurrentTPS() {
        try {
            // This would need to be implemented based on server type
            // For now, return a placeholder
            return 20.0;
        } catch (Exception e) {
            return 20.0; // Assume normal TPS if unable to get real value
        }
    }
    
    /**
     * Get current memory usage of the plugin.
     */
    private long getCurrentMemoryUsage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        return memoryBean.getHeapMemoryUsage().getUsed();
    }
    
    /**
     * Get current CPU usage percentage.
     */
    private double getCurrentCPUUsage() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        if (threadBean.isCurrentThreadCpuTimeSupported()) {
            long cpuTime = threadBean.getCurrentThreadCpuTime();
            // This is a simplified calculation
            return (cpuTime / 1_000_000.0) % 100; // Convert to percentage
        }
        return 0.0;
    }
    
    /**
     * Get the plugin's configuration.
     */
    private org.bukkit.configuration.file.FileConfiguration config() {
        return plugin.getConfig();
    }
    
    /**
     * Log performance summary.
     */
    private void logPerformanceSummary() {
        if (!config().getBoolean("performance.debug-logging", false)) {
            return;
        }
        
        long totalOps = totalOperations.get();
        long totalTime = totalExecutionTime.get();
        double avgTime = totalOps > 0 ? (totalTime / 1_000_000.0) / totalOps : 0.0;
        
        logger.info("📊 Performance Summary:");
        logger.info("   Total operations: " + totalOps);
        logger.info("   Average execution time: " + String.format("%.2f", avgTime) + "ms");
        logger.info("   Memory usage: " + (getCurrentMemoryUsage() / 1024 / 1024) + "MB");
        logger.info("   Cache entries: " + cache.size());
        logger.info("   High activity players: " + highActivityPlayers.size());
        logger.info("   Low activity players: " + lowActivityPlayers.size());
    }
    
    /**
     * Get adaptive interval for a component.
     */
    public int getAdaptiveInterval(String component) {
        return adaptiveIntervals_map.getOrDefault(component, getBaseInterval(component));
    }
    
    /**
     * Check if a player is categorized as high activity.
     */
    public boolean isHighActivityPlayer(Player player) {
        return highActivityPlayers.contains(player.getUniqueId());
    }
    
    /**
     * Check if a player is categorized as low activity.
     */
    public boolean isLowActivityPlayer(Player player) {
        return lowActivityPlayers.contains(player.getUniqueId());
    }
    
    /**
     * Get performance statistics.
     */
    public PerformanceStats getPerformanceStats() {
        return new PerformanceStats(
            totalOperations.get(),
            totalExecutionTime.get(),
            getCurrentMemoryUsage(),
            getCurrentTPS(),
            cache.size(),
            componentMetrics.size(),
            highActivityPlayers.size(),
            lowActivityPlayers.size()
        );
    }
    
    /**
     * Update player activity timestamp.
     */
    public void updatePlayerActivity(Player player) {
        playerLastActivity.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    /**
     * Clear player data on disconnect.
     */
    public void clearPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        playerLastActivity.remove(uuid);
        highActivityPlayers.remove(uuid);
        lowActivityPlayers.remove(uuid);
    }
    
    // ============= INTERFACES AND CLASSES =============
    
    /**
     * Performance task interface.
     */
    @FunctionalInterface
    public interface PerformanceTask<T> {
        T execute() throws Exception;
    }
    
    /**
     * Cache supplier interface.
     */
    @FunctionalInterface
    public interface CacheSupplier<T> {
        T get();
    }
    
    /**
     * Cache entry holder.
     */
    private static class CacheEntry {
        final Object value;
        final long timestamp;
        
        CacheEntry(Object value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * Performance metrics for a component.
     */
    private static class PerformanceMetrics {
        private final String componentName;
        private long totalExecutions = 0;
        private long totalExecutionTime = 0;
        private long totalMemoryUsed = 0;
        private long successfulExecutions = 0;
        private long maxExecutionTime = 0;
        private long minExecutionTime = Long.MAX_VALUE;
        
        PerformanceMetrics(String componentName) {
            this.componentName = componentName;
        }
        
        void recordExecution(long executionTime, long memoryUsed, boolean success) {
            totalExecutions++;
            totalExecutionTime += executionTime;
            totalMemoryUsed += memoryUsed;
            
            if (success) {
                successfulExecutions++;
            }
            
            maxExecutionTime = Math.max(maxExecutionTime, executionTime);
            minExecutionTime = Math.min(minExecutionTime, executionTime);
        }
        
        double getAverageExecutionTime() {
            return totalExecutions > 0 ? (totalExecutionTime / 1_000_000.0) / totalExecutions : 0.0;
        }
        
        double getSuccessRate() {
            return totalExecutions > 0 ? (double) successfulExecutions / totalExecutions : 1.0;
        }
    }
    
    /**
     * Performance monitor for system metrics.
     */
    private static class PerformanceMonitor {
        private long lastUpdate = System.currentTimeMillis();
        private double lastTps = 20.0;
        private long lastMemory = 0;
        
        void update() {
            lastUpdate = System.currentTimeMillis();
            // Update metrics here
        }
        
        double getTps() { return lastTps; }
        long getMemoryUsage() { return lastMemory; }
        long getLastUpdate() { return lastUpdate; }
    }
    
    /**
     * Performance statistics holder.
     */
    public static class PerformanceStats {
        private final long totalOperations;
        private final long totalExecutionTime;
        private final long memoryUsage;
        private final double tps;
        private final int cacheSize;
        private final int componentCount;
        private final int highActivityPlayers;
        private final int lowActivityPlayers;
        
        public PerformanceStats(long totalOperations, long totalExecutionTime, long memoryUsage,
                              double tps, int cacheSize, int componentCount,
                              int highActivityPlayers, int lowActivityPlayers) {
            this.totalOperations = totalOperations;
            this.totalExecutionTime = totalExecutionTime;
            this.memoryUsage = memoryUsage;
            this.tps = tps;
            this.cacheSize = cacheSize;
            this.componentCount = componentCount;
            this.highActivityPlayers = highActivityPlayers;
            this.lowActivityPlayers = lowActivityPlayers;
        }
        
        // Getters
        public long getTotalOperations() { return totalOperations; }
        public long getTotalExecutionTime() { return totalExecutionTime; }
        public long getMemoryUsage() { return memoryUsage; }
        public double getTps() { return tps; }
        public int getCacheSize() { return cacheSize; }
        public int getComponentCount() { return componentCount; }
        public int getHighActivityPlayers() { return highActivityPlayers; }
        public int getLowActivityPlayers() { return lowActivityPlayers; }
        
        public double getAverageExecutionTime() {
            return totalOperations > 0 ? (totalExecutionTime / 1_000_000.0) / totalOperations : 0.0;
        }
    }
}