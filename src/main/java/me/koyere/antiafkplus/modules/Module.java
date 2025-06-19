package me.koyere.antiafkplus.modules;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.logging.Logger;

/**
 * Abstract base class for all AntiAFKPlus modules.
 * Provides standardized lifecycle management, configuration handling,
 * and performance monitoring for modular functionality.
 */
public abstract class Module implements Listener {
    
    protected final AntiAFKPlus plugin;
    protected final String moduleName;
    protected final Logger logger;
    
    private boolean enabled = false;
    private boolean initialized = false;
    private long startupTime = 0;
    private long shutdownTime = 0;
    
    // Performance monitoring
    private long totalExecutionTime = 0;
    private int executionCount = 0;
    private boolean performanceMonitoring = false;
    
    public Module(AntiAFKPlus plugin, String moduleName) {
        this.plugin = plugin;
        this.moduleName = moduleName;
        this.logger = plugin.getLogger();
    }
    
    /**
     * Initialize the module. Called during plugin startup.
     * @return true if initialization was successful
     */
    public final boolean initialize() {
        if (initialized) {
            return true;
        }
        
        long start = System.currentTimeMillis();
        
        try {
            // Check if module is enabled in config
            if (!isEnabledInConfig()) {
                logger.info("Module '" + moduleName + "' is disabled in configuration.");
                return false;
            }
            
            // Load module configuration
            loadConfiguration();
            
            // Perform module-specific initialization
            if (onInitialize()) {
                this.enabled = true;
                this.initialized = true;
                this.startupTime = System.currentTimeMillis() - start;
                
                // Register events if this module has listeners
                registerEvents();
                
                logger.info("✅ Module '" + moduleName + "' initialized successfully in " + startupTime + "ms");
                return true;
            } else {
                logger.warning("❌ Module '" + moduleName + "' failed to initialize");
                return false;
            }
            
        } catch (Exception e) {
            logger.severe("❌ Module '" + moduleName + "' initialization failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Shutdown the module. Called during plugin shutdown or module disable.
     */
    public final void shutdown() {
        if (!initialized) {
            return;
        }
        
        long start = System.currentTimeMillis();
        
        try {
            // Unregister events
            unregisterEvents();
            
            // Perform module-specific shutdown
            onShutdown();
            
            this.enabled = false;
            this.initialized = false;
            this.shutdownTime = System.currentTimeMillis() - start;
            
            logger.info("🔄 Module '" + moduleName + "' shutdown completed in " + shutdownTime + "ms");
            
        } catch (Exception e) {
            logger.severe("❌ Module '" + moduleName + "' shutdown failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Reload the module configuration and restart if necessary.
     */
    public final void reload() {
        if (!initialized) {
            initialize();
            return;
        }
        
        logger.info("🔄 Reloading module '" + moduleName + "'...");
        
        try {
            // Check if module should still be enabled
            if (!isEnabledInConfig()) {
                shutdown();
                return;
            }
            
            // Reload configuration
            loadConfiguration();
            
            // Perform module-specific reload
            onReload();
            
            logger.info("✅ Module '" + moduleName + "' reloaded successfully");
            
        } catch (Exception e) {
            logger.severe("❌ Module '" + moduleName + "' reload failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Check if this module is enabled in the configuration.
     */
    private boolean isEnabledInConfig() {
        String configPath = "modules." + getConfigKey() + ".enabled";
        return plugin.getConfig().getBoolean(configPath, isEnabledByDefault());
    }
    
    /**
     * Load module-specific configuration from config.yml
     */
    private void loadConfiguration() {
        String configPath = "modules." + getConfigKey();
        ConfigurationSection moduleConfig = plugin.getConfig().getConfigurationSection(configPath);
        
        if (moduleConfig != null) {
            // Load performance monitoring setting
            this.performanceMonitoring = moduleConfig.getBoolean("performance-monitoring", false);
            
            // Let the module load its specific configuration
            loadModuleConfiguration(moduleConfig);
        } else {
            // Create default configuration section
            createDefaultConfiguration();
        }
    }
    
    /**
     * Create default configuration for this module if it doesn't exist.
     */
    private void createDefaultConfiguration() {
        String configPath = "modules." + getConfigKey();
        ConfigurationSection moduleConfig = plugin.getConfig().createSection(configPath);
        
        moduleConfig.set("enabled", isEnabledByDefault());
        moduleConfig.set("performance-monitoring", false);
        
        // Let the module create its default configuration
        createModuleDefaults(moduleConfig);
        
        plugin.saveConfig();
    }
    
    /**
     * Register event listeners for this module.
     */
    private void registerEvents() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Unregister event listeners for this module.
     */
    private void unregisterEvents() {
        try {
            // Bukkit doesn't have a direct way to unregister specific listeners
            // We'll rely on the plugin shutdown to handle this
        } catch (Exception e) {
            logger.warning("Could not unregister events for module '" + moduleName + "': " + e.getMessage());
        }
    }
    
    /**
     * Execute a task with performance monitoring if enabled.
     */
    protected final void executeWithMonitoring(Runnable task) {
        if (performanceMonitoring) {
            long start = System.nanoTime();
            try {
                task.run();
            } finally {
                long duration = System.nanoTime() - start;
                totalExecutionTime += duration;
                executionCount++;
            }
        } else {
            task.run();
        }
    }
    
    // Abstract methods that modules must implement
    
    /**
     * Get the configuration key for this module.
     * This is used to find the module's configuration section.
     */
    public abstract String getConfigKey();
    
    /**
     * Get the display name for this module.
     */
    public abstract String getDisplayName();
    
    /**
     * Get the description of what this module does.
     */
    public abstract String getDescription();
    
    /**
     * Get the version of this module.
     */
    public abstract String getVersion();
    
    /**
     * Get the list of dependencies this module requires.
     * @return List of plugin names this module depends on
     */
    public abstract List<String> getDependencies();
    
    /**
     * Get the list of soft dependencies this module can use.
     * @return List of plugin names this module can integrate with
     */
    public abstract List<String> getSoftDependencies();
    
    /**
     * Whether this module should be enabled by default.
     */
    public abstract boolean isEnabledByDefault();
    
    /**
     * Initialize the module. Called once during plugin startup.
     * @return true if initialization was successful
     */
    protected abstract boolean onInitialize();
    
    /**
     * Shutdown the module. Called during plugin shutdown.
     */
    protected abstract void onShutdown();
    
    /**
     * Reload the module. Called when configuration is reloaded.
     */
    protected abstract void onReload();
    
    /**
     * Load module-specific configuration.
     * @param config The configuration section for this module
     */
    protected abstract void loadModuleConfiguration(ConfigurationSection config);
    
    /**
     * Create default configuration for this module.
     * @param config The configuration section to populate
     */
    protected abstract void createModuleDefaults(ConfigurationSection config);
    
    // Getters
    
    public String getModuleName() {
        return moduleName;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public long getStartupTime() {
        return startupTime;
    }
    
    public long getShutdownTime() {
        return shutdownTime;
    }
    
    /**
     * Get performance statistics for this module.
     */
    public ModulePerformanceStats getPerformanceStats() {
        return new ModulePerformanceStats(
            moduleName,
            totalExecutionTime,
            executionCount,
            performanceMonitoring
        );
    }
    
    /**
     * Performance statistics holder.
     */
    public static class ModulePerformanceStats {
        private final String moduleName;
        private final long totalExecutionTime;
        private final int executionCount;
        private final boolean monitoringEnabled;
        
        public ModulePerformanceStats(String moduleName, long totalExecutionTime, 
                                    int executionCount, boolean monitoringEnabled) {
            this.moduleName = moduleName;
            this.totalExecutionTime = totalExecutionTime;
            this.executionCount = executionCount;
            this.monitoringEnabled = monitoringEnabled;
        }
        
        public String getModuleName() { return moduleName; }
        public long getTotalExecutionTime() { return totalExecutionTime; }
        public int getExecutionCount() { return executionCount; }
        public boolean isMonitoringEnabled() { return monitoringEnabled; }
        
        public double getAverageExecutionTime() {
            return executionCount > 0 ? (double) totalExecutionTime / executionCount / 1_000_000.0 : 0.0; // Convert to ms
        }
        
        @Override
        public String toString() {
            if (!monitoringEnabled) {
                return moduleName + ": Performance monitoring disabled";
            }
            
            return String.format("%s: %d executions, %.2fms avg, %.2fms total", 
                moduleName, executionCount, getAverageExecutionTime(), totalExecutionTime / 1_000_000.0);
        }
    }
}