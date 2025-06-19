package me.koyere.antiafkplus.api.data;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Comprehensive information about the AntiAFKPlus plugin.
 */
public class PluginInfo {
    
    private final String version;
    private final String apiVersion;
    private final Instant startupTime;
    private final long startupDuration;
    private final boolean fullyInitialized;
    private final String platformType;
    private final Set<String> enabledModules;
    private final Set<String> availableLanguages;
    private final int bedrockPlayerCount;
    private final boolean debugMode;
    private final Map<String, Object> performanceMetrics;
    private final Map<String, String> systemInfo;
    
    public PluginInfo(String version, String apiVersion, Instant startupTime, long startupDuration,
                     boolean fullyInitialized, String platformType, Set<String> enabledModules,
                     Set<String> availableLanguages, int bedrockPlayerCount, boolean debugMode,
                     Map<String, Object> performanceMetrics, Map<String, String> systemInfo) {
        this.version = version;
        this.apiVersion = apiVersion;
        this.startupTime = startupTime;
        this.startupDuration = startupDuration;
        this.fullyInitialized = fullyInitialized;
        this.platformType = platformType;
        this.enabledModules = enabledModules;
        this.availableLanguages = availableLanguages;
        this.bedrockPlayerCount = bedrockPlayerCount;
        this.debugMode = debugMode;
        this.performanceMetrics = performanceMetrics;
        this.systemInfo = systemInfo;
    }
    
    // Getters
    public String getVersion() { return version; }
    public String getApiVersion() { return apiVersion; }
    public Instant getStartupTime() { return startupTime; }
    public long getStartupDuration() { return startupDuration; }
    public boolean isFullyInitialized() { return fullyInitialized; }
    public String getPlatformType() { return platformType; }
    public Set<String> getEnabledModules() { return enabledModules; }
    public Set<String> getAvailableLanguages() { return availableLanguages; }
    public int getBedrockPlayerCount() { return bedrockPlayerCount; }
    public boolean isDebugMode() { return debugMode; }
    public Map<String, Object> getPerformanceMetrics() { return performanceMetrics; }
    public Map<String, String> getSystemInfo() { return systemInfo; }
}