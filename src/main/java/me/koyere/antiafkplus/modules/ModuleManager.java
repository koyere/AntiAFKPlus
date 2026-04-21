package me.koyere.antiafkplus.modules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.koyere.antiafkplus.AntiAFKPlus;

/**
 * Manages all AntiAFKPlus modules with state tracking.
 * 
 * v3.0 TODO: Rewrite to use actual Module instances with lifecycle management.
 * Currently tracks module enabled/disabled state from config for use by other components.
 */
public class ModuleManager {
    
    private final AntiAFKPlus plugin;
    
    // Module state registry
    private final Map<String, Boolean> moduleStates = new ConcurrentHashMap<>();
    private final List<String> enabledModules = new ArrayList<>();
    
    public ModuleManager(AntiAFKPlus plugin) {
        this.plugin = plugin;
        registerBuiltInModules();
    }
    
    /**
     * Register all built-in modules (simplified implementation).
     */
    private void registerBuiltInModules() {
        // Core modules (always enabled)
        registerModuleState("core-detection", true);
        registerModuleState("core-events", true);
        registerModuleState("core-api", true);
        registerModuleState("core-commands", true);
        
        // Feature modules (configurable)
        registerModuleState("pattern-detection", plugin.getConfig().getBoolean("modules.pattern-detection.enabled", true));
        registerModuleState("autoclick-detection", plugin.getConfig().getBoolean("modules.autoclick-detection.enabled", true));
        registerModuleState("player-protection", plugin.getConfig().getBoolean("modules.player-protection.enabled", true));
        registerModuleState("afk-zones", plugin.getConfig().getBoolean("modules.afk-zones.enabled", false));
        registerModuleState("reward-system", plugin.getConfig().getBoolean("modules.reward-system.enabled", false));
        registerModuleState("visual-effects", plugin.getConfig().getBoolean("modules.visual-effects.enabled", false));
        registerModuleState("database", plugin.getConfig().getBoolean("modules.database.enabled", false));
        registerModuleState("analytics", plugin.getConfig().getBoolean("modules.analytics.enabled", false));
        
        // Integration modules
        registerModuleState("worldguard-integration", plugin.getConfig().getBoolean("modules.worldguard-integration.enabled", false));
        registerModuleState("placeholderapi-integration", plugin.getConfig().getBoolean("modules.placeholderapi-integration.enabled", true));
        registerModuleState("discordsrv-integration", plugin.getConfig().getBoolean("modules.discordsrv-integration.enabled", false));
        registerModuleState("floodgate-integration", plugin.getConfig().getBoolean("modules.floodgate-integration.enabled", true));
        
        // Platform compatibility modules
        registerModuleState("folia-compatibility", plugin.getConfig().getBoolean("modules.folia-compatibility.enabled", true));
        registerModuleState("bedrock-compatibility", plugin.getConfig().getBoolean("modules.bedrock-compatibility.enabled", true));

        // Credit system (v2.5)
        registerModuleState("credit-system", plugin.getConfig().getBoolean("modules.credit-system.enabled", false));
        
    }
    
    /**
     * Register a module state (simplified version).
     */
    private void registerModuleState(String name, boolean enabled) {
        moduleStates.put(name, enabled);
        if (enabled) {
            enabledModules.add(name);
        }
    }
    
    /**
     * Initialize all modules.
     */
    public void initializeModules() {
        // Module state is tracked but lifecycle is managed
        // by individual components in AntiAFKPlus.initializeLegacyComponents()
    }

    /**
     * Reloads module enabled/disabled states from the current config.
     * Called after config reload to sync in-memory state with config.yml.
     */
    public void reloadModuleStates() {
        moduleStates.clear();
        enabledModules.clear();
        registerBuiltInModules();
    }
    
    /**
     * Shutdown all modules.
     */
    public void shutdown() {
        // Currently a no-op — shutdown is managed by AntiAFKPlus.onDisable()
    }
    
    /**
     * Check if a module is enabled.
     */
    public boolean isModuleEnabled(String moduleName) {
        return moduleStates.getOrDefault(moduleName, false);
    }
    
    /**
     * Get a module by name.
     * v3.0 TODO: Return actual Module instance.
     */
    public Module getModule(String name) {
        return null;
    }
    
    /**
     * Get all enabled modules.
     * v3.0 TODO: Return actual Module instances.
     */
    public List<Module> getEnabledModules() {
        return new ArrayList<>();
    }
    
    /**
     * Get enabled module names.
     */
    public List<String> getEnabledModuleNames() {
        return new ArrayList<>(enabledModules);
    }
    
    /**
     * Get module count.
     */
    public int getModuleCount() {
        return moduleStates.size();
    }
    
    /**
     * Get enabled module count.
     */
    public int getEnabledModuleCount() {
        return enabledModules.size();
    }
}
