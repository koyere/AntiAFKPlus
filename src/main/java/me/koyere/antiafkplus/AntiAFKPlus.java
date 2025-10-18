package me.koyere.antiafkplus;

import me.koyere.antiafkplus.afk.AFKManager;
import me.koyere.antiafkplus.afk.AntiAFKActivityDetector;
import me.koyere.antiafkplus.afk.ItemPickupBlocker;
import me.koyere.antiafkplus.afk.MovementListener;
import me.koyere.antiafkplus.afk.PatternDetector;
import me.koyere.antiafkplus.api.AntiAFKPlusAPI;
import me.koyere.antiafkplus.api.AntiAFKPlusAPIImpl;
import me.koyere.antiafkplus.api.APIEventListener;
import me.koyere.antiafkplus.command.AFKCommand;
import me.koyere.antiafkplus.command.AFKPlusCommand;
import me.koyere.antiafkplus.config.ConfigManager;
import me.koyere.antiafkplus.listener.AutoClickListener;
import me.koyere.antiafkplus.placeholder.PlaceholderHook;
import me.koyere.antiafkplus.utils.bStatsManager;

// Enterprise imports
import me.koyere.antiafkplus.modules.ModuleManager;
import me.koyere.antiafkplus.platform.PlatformScheduler;
import me.koyere.antiafkplus.i18n.LocalizationManager;
import me.koyere.antiafkplus.compatibility.BedrockCompatibility;
import me.koyere.antiafkplus.performance.PerformanceOptimizer;
import me.koyere.antiafkplus.utils.AFKLogger;
import me.koyere.antiafkplus.credit.CreditManager;
import me.koyere.antiafkplus.credit.CreditListener;
import me.koyere.antiafkplus.integrations.WorldGuardIntegration;
import me.koyere.antiafkplus.transfer.ServerTransferService;
import me.koyere.antiafkplus.transfer.CountdownSequenceService;
import me.koyere.antiafkplus.transfer.ActionPipelineService;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * AntiAFKPlus v2.4.1 - Main Plugin Class
 * 
 * Professional-grade AFK detection and management system with advanced features:
 * • Modular architecture with enable/disable controls
 * • Folia/Paper/Spigot/Bukkit multi-platform compatibility
 * • Bedrock Edition support via Floodgate/Geyser
 * • Complete internationalization (20+ languages)
 * • Zero-lag performance optimization
 * • Advanced pattern detection and behavioral analysis
 * • Professional API for third-party integration
 */
public final class AntiAFKPlus extends JavaPlugin {

    // Plugin version constants
    private static final String PLUGIN_VERSION = "2.8";
    private static final String API_VERSION = "2.8";
    private static final String MIN_MIGRATION_VERSION = "1.0";

    // Core components
    private static AntiAFKPlus instance;
    private ConfigManager configManager;
    private AFKManager afkManager;
    private MovementListener movementListener;
    private AntiAFKPlusAPI api;

    // Enterprise components
    private ModuleManager moduleManager;
    private PlatformScheduler platformScheduler;
    private LocalizationManager localizationManager;
    private BedrockCompatibility bedrockCompatibility;
    private PerformanceOptimizer performanceOptimizer;
    private AFKLogger afkLogger;
    private CreditManager creditManager;
    private WorldGuardIntegration worldGuardIntegration;
    private ServerTransferService serverTransferService;
    private CountdownSequenceService countdownSequenceService;
    private ActionPipelineService actionPipelineService;

    // Command Handlers
    private AFKCommand afkCommandHandler;
    private AFKPlusCommand afkPlusCommandHandler;

    // Optional Listeners that might need specific shutdown/reload logic
    private AutoClickListener autoClickListenerInstance;
    private AntiAFKActivityDetector antiAFKActivityDetectorInstance;

    // Enterprise state tracking
    private long startupTime;
    private boolean fullyInitialized = false;
    private boolean debugEnabled = false;
    private boolean migrationRequired = false;

    @Override
    public void onEnable() {
        startupTime = System.currentTimeMillis();
        instance = this;

        try {
            // Initialize all components
            if (!initializePhase1_CoreInfrastructure() ||
                !initializePhase2_PlatformCompatibility() ||
                !initializePhase3_ConfigurationAndLocalization() ||
                !initializePhase4_ModuleSystem() ||
                !initializePhase5_IntegrationAndAPI() ||
                !initializePhase6_Finalization()) {
                getLogger().severe("§cInitialization failed! Disabling plugin.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            fullyInitialized = true;
            getLogger().info("AntiAFKPlus v" + PLUGIN_VERSION + " enabled successfully!");

        } catch (Exception e) {
            getLogger().severe("§cCritical error during initialization!");
            getLogger().severe("§cError: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("§6=== AntiAFKPlus Shutdown ===");
        getLogger().info("§6Gracefully shutting down all systems...");

        try {
            // Shutdown in reverse order of initialization for maximum stability
            
            // Clean up API and clear player data
            if (api instanceof AntiAFKPlusAPIImpl) {
                AntiAFKPlusAPIImpl apiImpl = (AntiAFKPlusAPIImpl) api;
                for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                    apiImpl.clearPlayerData(player);
                }
            }

            // Shutdown module system (this handles all module cleanup)
            if (moduleManager != null) {
                getLogger().info("§6Shutting down module system...");
                moduleManager.shutdown();
            }

            // Shutdown performance optimizer
            if (performanceOptimizer != null) {
                getLogger().info("§6Shutting down performance optimizer...");
                // performanceOptimizer.shutdown(); // Method may not exist yet
            }

            // Shutdown Bedrock compatibility  
            if (bedrockCompatibility != null) {
                getLogger().info("§6Shutting down Bedrock compatibility...");
                // bedrockCompatibility.shutdown(); // Method may not exist yet
            }

            // Shutdown localization manager
            if (localizationManager != null) {
                getLogger().info("§6Shutting down localization system...");
                // localizationManager.shutdown(); // Method may not exist yet
            }

            // Shutdown credit system
            if (creditManager != null) {
                getLogger().info("§6Shutting down credit system...");
                creditManager.shutdown();
            }

            // Shutdown platform scheduler
            if (platformScheduler != null) {
                getLogger().info("§6Shutting down platform scheduler...");
                platformScheduler.shutdown();
            }

            // Shutdown legacy components
            if (afkManager != null) {
                afkManager.shutdown();
            }
            if (autoClickListenerInstance != null) {
                autoClickListenerInstance.shutdown();
            }
            if (antiAFKActivityDetectorInstance != null) {
                antiAFKActivityDetectorInstance.shutdown();
            }

            // AFKLogger is static, no shutdown needed

            getLogger().info("§aShutdown completed successfully.");

        } catch (Exception e) {
            getLogger().severe("§cError during shutdown: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Nullify all references for garbage collection
            nullifyReferences();
            getLogger().info("§aAntiAFKPlus v" + PLUGIN_VERSION + " has been disabled.");
        }
    }

    // ============= INITIALIZATION PHASES =============

    /**
     * Phase 1: Core Infrastructure
     * Initializes logging, configuration, and migration systems.
     */
    private boolean initializePhase1_CoreInfrastructure() {
        try {
            // Initialize AFKLogger (placeholder for now)
            this.afkLogger = null;
            
            // Ensure default configuration exists first
            saveDefaultConfig();

            // Check for version migration after default config is saved
            checkVersionMigration();

            // Initialize configuration manager
            this.configManager = new ConfigManager(this);

            // Set debug mode from config
            this.debugEnabled = getConfig().getBoolean("debug", false);

            return true;

        } catch (Exception e) {
            getLogger().severe("§cConfiguration initialization failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Phase 2: Platform Detection & Scheduling
     * Detects server platform and initializes appropriate scheduler.
     */
    private boolean initializePhase2_PlatformCompatibility() {
        try {
            // Initialize platform scheduler (auto-detects Folia, Paper, Spigot, Bukkit)
            this.platformScheduler = new PlatformScheduler(this);
            return true;

        } catch (Exception e) {
            getLogger().severe("§cPlatform initialization failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Phase 3: Configuration & Localization
     * Initializes internationalization and localization systems.
     */
    private boolean initializePhase3_ConfigurationAndLocalization() {
        try {
            // Initialize localization manager
            this.localizationManager = new LocalizationManager(this);
            return true;

        } catch (Exception e) {
            getLogger().severe("§cLocalization initialization failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Phase 4: Module System
     * Initializes the modular architecture and loads all modules.
     */
    private boolean initializePhase4_ModuleSystem() {
        try {
            // Initialize performance optimizer first (needed by modules)
            this.performanceOptimizer = new PerformanceOptimizer(this);

            // Initialize module manager
            this.moduleManager = new ModuleManager(this);

            // Load and initialize all modules
            moduleManager.initializeModules();
            return true;

        } catch (Exception e) {
            getLogger().severe("§cModule system initialization failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Phase 5: Integration & API
     * Initializes external integrations and public API.
     */
    private boolean initializePhase5_IntegrationAndAPI() {
        try {
            // Initialize Bedrock compatibility
            if (getConfig().getBoolean("bedrock-compatibility.enabled", true)) {
                this.bedrockCompatibility = new BedrockCompatibility(this);
            }

            // Initialize WorldGuard integration (reflection-based)
            if (getConfig().getBoolean("modules.worldguard-integration.enabled", false) ||
                getConfig().getBoolean("zone-management.require-worldguard", false) ||
                getConfig().getBoolean("integrations.worldguard.enabled", false)) {
                this.worldGuardIntegration = new WorldGuardIntegration(this);
            }

            // Initialize legacy components for backward compatibility
            initializeLegacyComponents();

            // Initialize public API
            this.api = new AntiAFKPlusAPIImpl(this);
            if (this.api instanceof AntiAFKPlusAPIImpl apiImpl) {
                getServer().getPluginManager().registerEvents(new APIEventListener(apiImpl), this);
            }

            // Initialize PlaceholderAPI integration
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new PlaceholderHook(this).register();
            }

            // Initialize Credit System (v2.5 core - Phase 1)
            if (getConfig().getBoolean("modules.credit-system.enabled", false) &&
                getConfig().getBoolean("credit-system.enabled", false)) {
                this.creditManager = new me.koyere.antiafkplus.credit.CreditManager(this);
                getServer().getPluginManager().registerEvents(new me.koyere.antiafkplus.credit.CreditListener(this, this.creditManager), this);
                // Protección básica de zona AFK (opcional vía config)
                getServer().getPluginManager().registerEvents(new me.koyere.antiafkplus.credit.AFKZoneProtectionListener(this, this.creditManager), this);
                getLogger().info("§aCredit System initialized (Phase 1)");
            }

            // Initialize Server Transfer (v2.6 - Phase 1 backbone)
            this.serverTransferService = new ServerTransferService(this);
            this.countdownSequenceService = new CountdownSequenceService(this);
            this.actionPipelineService = new ActionPipelineService(this);
            // Registrar canales siempre: es inocuo y mejora robustez
            try {
                Bukkit.getMessenger().registerOutgoingPluginChannel(this, ServerTransferService.LEGACY_CHANNEL);
                Bukkit.getMessenger().registerOutgoingPluginChannel(this, ServerTransferService.NAMESPACE_CHANNEL);
                getLogger().info("§aServer transfer channels registered (BungeeCord & bungeecord:main)");
            } catch (Exception e) {
                getLogger().warning("Failed to register plugin messaging channels: " + e.getMessage());
            }

            return true;

        } catch (Exception e) {
            getLogger().severe("§cIntegration initialization failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Phase 6: Finalization
     * Completes initialization with metrics, commands, and final setup.
     */
    private boolean initializePhase6_Finalization() {
        try {
            // Register commands
            registerCommands();

            // Initialize bStats metrics
            try {
                new bStatsManager(this);
            } catch (Exception e) {
                getLogger().warning("Failed to initialize bStats: " + e.getMessage());
            }

            return true;

        } catch (Exception e) {
            getLogger().severe("§cFinalization failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Initializes legacy components for backward compatibility.
     */
    private void initializeLegacyComponents() {
        // Initialize enhanced MovementListener
        this.movementListener = new MovementListener();
        getServer().getPluginManager().registerEvents(this.movementListener, this);

        // Initialize AFKManager (legacy component)
        this.afkManager = new AFKManager(this, this.movementListener);

        // Initialize and register standard listeners
        ItemPickupBlocker itemPickupBlocker = new ItemPickupBlocker(this);
        getServer().getPluginManager().registerEvents(itemPickupBlocker, this);

        // Initialize AntiAFKActivityDetector
        this.antiAFKActivityDetectorInstance = new AntiAFKActivityDetector(this);
        getServer().getPluginManager().registerEvents(this.antiAFKActivityDetectorInstance, this);

        // Initialize AutoClickListener if enabled
        if (this.configManager.isAutoclickDetectionEnabled()) {
            this.autoClickListenerInstance = new AutoClickListener(this);
            getServer().getPluginManager().registerEvents(this.autoClickListenerInstance, this);
            // AutoClickListener enabled silently
        }
    }

    /**
     * Registers all plugin commands.
     */
    private void registerCommands() {
        // Register /afk command
        this.afkCommandHandler = new AFKCommand(this);
        if (getCommand("afk") != null) {
            getCommand("afk").setExecutor(this.afkCommandHandler);
            getCommand("afk").setTabCompleter(this.afkCommandHandler);
        } else {
            getLogger().severe("Failed to register 'afk' command! Check plugin.yml.");
        }

        // Register /afkplus command
        this.afkPlusCommandHandler = new AFKPlusCommand(this);
        if (getCommand("afkplus") != null) {
            getCommand("afkplus").setExecutor(this.afkPlusCommandHandler);
            getCommand("afkplus").setTabCompleter(this.afkPlusCommandHandler);
        } else {
            getLogger().severe("Failed to register 'afkplus' command! Check plugin.yml.");
        }

        // Register /afkback command (credit system)
        if (getCommand("afkback") != null) {
            getCommand("afkback").setExecutor(new me.koyere.antiafkplus.command.AFKBackCommand(this));
        }

        // Register /afkcredits command (credit system)
        if (getCommand("afkcredits") != null) {
            getCommand("afkcredits").setExecutor(new me.koyere.antiafkplus.command.AFKCreditsCommand(this));
        }
    }

    /**
     * Nullifies all references for garbage collection.
     */
    private void nullifyReferences() {
        // Enterprise components
        this.moduleManager = null;
        this.platformScheduler = null;
        this.localizationManager = null;
        this.bedrockCompatibility = null;
        this.performanceOptimizer = null;
        this.afkLogger = null;
        this.worldGuardIntegration = null;

        // Legacy components
        this.api = null;
        this.afkManager = null;
        this.movementListener = null;
        this.configManager = null;
        this.afkCommandHandler = null;
        this.afkPlusCommandHandler = null;
        this.autoClickListenerInstance = null;
        this.antiAFKActivityDetectorInstance = null;
        this.creditManager = null;
    }

    // ============= VERSION MIGRATION SYSTEM =============

    /**
     * Checks if configuration migration is needed from older versions.
     */
    private void checkVersionMigration() {
        if (!getDataFolder().exists() || !getConfig().contains("version")) {
            // Fresh installation
            getConfig().set("version", PLUGIN_VERSION);
            saveConfig();
            return;
        }

        String currentVersion = getConfig().getString("version", "1.0");
        
        // Check if config is incomplete (has less than 5 main sections)
        boolean isIncompleteConfig = getConfig().getKeys(false).size() < 5;
        
        if (isVersionOlderThan(currentVersion, PLUGIN_VERSION) || isIncompleteConfig) {
            // Force complete config regeneration for incomplete configs
            if (isIncompleteConfig) {
                getLogger().info("Incomplete configuration detected. Regenerating complete config...");
                // Delete current config file to force complete regeneration
                File configFile = new File(getDataFolder(), "config.yml");
                if (configFile.exists()) {
                    configFile.delete();
                }
                // Reload will now use the complete default config
                saveDefaultConfig();
                reloadConfig();
            } else {
                performConfigMigration(currentVersion);
            }
            
            getConfig().set("version", PLUGIN_VERSION);
            saveConfig();
        }
    }

    /**
     * Performs configuration migration from older versions.
     */
    private void performConfigMigration(String fromVersion) {
        try {
            // Simple migration - just add essential module settings if missing
            if (!getConfig().contains("modules")) {
                getConfig().set("modules.core-detection.enabled", true);
                getConfig().set("modules.core-events.enabled", true);
                getConfig().set("modules.core-api.enabled", true);
                getConfig().set("modules.core-commands.enabled", true);
            }
        } catch (Exception e) {
            getLogger().warning("Migration error: " + e.getMessage());
        }
    }

    /**
     * Compares version strings.
     */
    private boolean isVersionOlderThan(String version1, String version2) {
        try {
            // Clean versions (remove suffixes like -ENTERPRISE)
            String clean1 = version1.split("-")[0];
            String clean2 = version2.split("-")[0];
            
            String[] v1Parts = clean1.split("\\.");
            String[] v2Parts = clean2.split("\\.");

            int maxLength = Math.max(v1Parts.length, v2Parts.length);

            for (int i = 0; i < maxLength; i++) {
                int v1Part = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
                int v2Part = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;

                if (v1Part < v2Part) return true;
                if (v1Part > v2Part) return false;
            }

            return false;
        } catch (NumberFormatException e) {
            // If version parsing fails, assume upgrade needed
            return true;
        }
    }

    // ============= PUBLIC GETTER METHODS (API) =============

    /**
     * Gets the static instance of the plugin.
     */
    public static AntiAFKPlus getInstance() {
        return instance;
    }

    /**
     * Gets the configuration manager.
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Gets the AFK manager (legacy component).
     */
    public AFKManager getAfkManager() {
        return afkManager;
    }

    /**
     * Gets the public API.
     */
    public AntiAFKPlusAPI getAPI() {
        return api;
    }

    /**
     * Gets the module manager.
     */
    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    /**
     * Gets the platform scheduler.
     */
    public PlatformScheduler getPlatformScheduler() {
        return platformScheduler;
    }

    /**
     * Gets the server transfer service (may be used when TRANSFER_SERVER action is executed).
     */
    public ServerTransferService getServerTransferService() {
        return serverTransferService;
    }

    /** Countdown runner for per-player sequences (Fase 2). */
    public CountdownSequenceService getCountdownSequenceService() {
        return countdownSequenceService;
    }

    /** Action pipeline for scripted sequences (Fase 4). */
    public ActionPipelineService getActionPipelineService() { return actionPipelineService; }

    /**
     * Gets the localization manager.
     */
    public LocalizationManager getLocalizationManager() {
        return localizationManager;
    }

    /**
     * Gets the Bedrock compatibility layer.
     */
    public BedrockCompatibility getBedrockCompatibility() {
        return bedrockCompatibility;
    }

    /**
     * Gets the performance optimizer.
     */
    public PerformanceOptimizer getPerformanceOptimizer() {
        return performanceOptimizer;
    }

    /**
     * Gets the AFKLogger instance.
     */
    public AFKLogger getAFKLogger() {
        return afkLogger;
    }

    /**
     * Gets the AutoClickListener instance.
     */
    public AutoClickListener getAutoClickListenerInstance() {
        return autoClickListenerInstance;
    }

    /**
     * Gets the enhanced MovementListener.
     */
    public MovementListener getMovementListener() {
        return movementListener;
    }

    /**
     * Gets the CreditManager (may be null if credit module is disabled).
     */
    public me.koyere.antiafkplus.credit.CreditManager getCreditManager() {
        return creditManager;
    }

    /** WorldGuard integration helper (may be null). */
    public WorldGuardIntegration getWorldGuardIntegration() { return worldGuardIntegration; }

    // ============= UTILITY METHODS =============

    /**
     * Gets the plugin version.
     */
    public String getPluginVersion() {
        return PLUGIN_VERSION;
    }

    /**
     * Gets the API version.
     */
    public String getAPIVersion() {
        return API_VERSION;
    }

    /**
     * Gets the startup time in milliseconds.
     */
    public long getStartupTime() {
        return startupTime;
    }

    /**
     * Checks if the plugin is fully initialized.
     */
    public boolean isFullyInitialized() {
        return fullyInitialized;
    }

    /**
     * Checks if debug mode is enabled.
     */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    /**
     * Checks if migration was required.
     */
    public boolean wasMigrated() {
        return migrationRequired;
    }

    /**
     * Logs a debug message if debug mode is enabled.
     */
    public void debug(String message) {
        if (debugEnabled) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    /**
     * Logs an info message.
     */
    public void info(String message) {
        getLogger().info(message);
    }

    /**
     * Logs a warning message.
     */
    public void warning(String message) {
        getLogger().warning(message);
    }

    /**
     * Logs an error message.
     */
    public void error(String message) {
        getLogger().severe(message);
    }
}
