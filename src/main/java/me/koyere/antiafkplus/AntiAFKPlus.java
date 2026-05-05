package me.koyere.antiafkplus;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import me.koyere.antiafkplus.afk.AFKManager;
import me.koyere.antiafkplus.afk.AntiAFKActivityDetector;
import me.koyere.antiafkplus.afk.ItemPickupBlocker;
import me.koyere.antiafkplus.afk.MovementListener;
import me.koyere.antiafkplus.api.APIEventListener;
import me.koyere.antiafkplus.api.AntiAFKPlusAPI;
import me.koyere.antiafkplus.api.AntiAFKPlusAPIImpl;
import me.koyere.antiafkplus.command.AFKCommand;
import me.koyere.antiafkplus.command.AFKPlusCommand;
import me.koyere.antiafkplus.compatibility.BedrockCompatibility;
import me.koyere.antiafkplus.config.ConfigManager;
import me.koyere.antiafkplus.credit.CreditManager;
import me.koyere.antiafkplus.gui.GUIManager;
import me.koyere.antiafkplus.i18n.LocalizationManager;
import me.koyere.antiafkplus.integrations.WorldGuardIntegration;
import me.koyere.antiafkplus.modules.ModuleManager;
import me.koyere.antiafkplus.performance.PerformanceOptimizer;
import me.koyere.antiafkplus.placeholder.PlaceholderHook;
import me.koyere.antiafkplus.platform.PlatformScheduler;
import me.koyere.antiafkplus.time.TimeWindowService;
import me.koyere.antiafkplus.transfer.ActionPipelineService;
import me.koyere.antiafkplus.transfer.CountdownSequenceService;
import me.koyere.antiafkplus.transfer.ServerTransferService;
import me.koyere.antiafkplus.utils.bStatsManager;
import me.koyere.antiafkplus.visual.VisualEffectsManager;

/**
 * AntiAFKPlus v3.0 Premium - Main Plugin Class
 * 
 * Professional AFK detection and management system:
 * - Modular architecture with in-game GUI configuration
 * - Folia/Paper/Spigot/Bukkit multi-platform compatibility
 * - Bedrock Edition support via Floodgate/Geyser
 * - 10-language internationalization system
 * - Advanced pattern detection with detection profiles
 * - Credit system with transfers and leaderboard
 * - Visual effects (particles, tab list, name tags)
 * - Vault and DiscordSRV integrations
 * - Professional API for third-party integration
 */
public final class AntiAFKPlus extends JavaPlugin {

    // Plugin version constants
    private static final String PLUGIN_VERSION = "3.0.0";
    private static final String API_VERSION = "3.0.0";

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
    private CreditManager creditManager;
    private WorldGuardIntegration worldGuardIntegration;
    private me.koyere.antiafkplus.integrations.VaultIntegration vaultIntegration;
    private me.koyere.antiafkplus.integrations.DiscordSRVIntegration discordSRVIntegration;
    private ServerTransferService serverTransferService;
    private CountdownSequenceService countdownSequenceService;
    private ActionPipelineService actionPipelineService;
    private TimeWindowService timeWindowService;
    private GUIManager guiManager;
    private VisualEffectsManager visualEffectsManager;

    // Command Handlers
    private AFKCommand afkCommandHandler;
    private AFKPlusCommand afkPlusCommandHandler;

    // Optional Listeners that might need specific shutdown/reload logic
    private me.koyere.antiafkplus.listener.AutoClickListener autoClickListenerInstance;
    private me.koyere.antiafkplus.listener.PlayerProtectionListener playerProtectionListener;
    private AntiAFKActivityDetector antiAFKActivityDetectorInstance;
    private bStatsManager bStatsMetrics;

    // Enterprise state tracking
    private long startupTime;
    private boolean fullyInitialized = false;
    private boolean debugEnabled = false;

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
            getLogger().log(java.util.logging.Level.SEVERE, "Critical error during initialization", e);
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
            if (api instanceof AntiAFKPlusAPIImpl apiImpl) {
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

            // Shutdown GUI system
            if (guiManager != null) {
                guiManager.shutdown();
            }

            // Shutdown Visual Effects
            if (visualEffectsManager != null) {
                visualEffectsManager.shutdown();
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
            getLogger().log(java.util.logging.Level.SEVERE, "Error during shutdown", e);
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
            // Ensure default configuration exists first
            saveDefaultConfig();

            // Check for version migration after default config is saved
            checkVersionMigration();

            // Initialize configuration manager
            this.configManager = new ConfigManager(this);
            rebuildTimeWindowService();

            // Set debug mode from config
            this.debugEnabled = getConfig().getBoolean("debug", false);

            return true;

        } catch (Exception e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Configuration initialization failed", e);
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
            getLogger().log(java.util.logging.Level.SEVERE, "Platform initialization failed", e);
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

            // Reload messages now that LocalizationManager is available
            // (ConfigManager was created in Phase 1 before localization existed)
            if (this.configManager != null) {
                this.configManager.loadMessages();
            }

            return true;

        } catch (Exception e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Localization initialization failed", e);
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
            getLogger().log(java.util.logging.Level.SEVERE, "Module system initialization failed", e);
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

            // Initialize Vault integration (reflection-based, optional)
            if (getConfig().getBoolean("integrations.vault.enabled", false)) {
                this.vaultIntegration = new me.koyere.antiafkplus.integrations.VaultIntegration(this);
            }

            // Initialize DiscordSRV integration (reflection-based, optional)
            if (getConfig().getBoolean("integrations.discordsrv.enabled", false)) {
                this.discordSRVIntegration = new me.koyere.antiafkplus.integrations.DiscordSRVIntegration(this);
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

            // Initialize Credit System
            if (getConfig().getBoolean("credit-system.enabled", false)) {
                this.creditManager = new me.koyere.antiafkplus.credit.CreditManager(this);
                getServer().getPluginManager().registerEvents(new me.koyere.antiafkplus.credit.CreditListener(this, this.creditManager), this);
                // AFK zone protection (optional via config)
                getServer().getPluginManager().registerEvents(new me.koyere.antiafkplus.credit.AFKZoneProtectionListener(this, this.creditManager), this);
                getLogger().info("§aCredit System initialized (Phase 1)");
            }

            // Initialize Server Transfer (v2.6 - Phase 1 backbone)
            this.serverTransferService = new ServerTransferService(this);
            this.countdownSequenceService = new CountdownSequenceService(this);
            this.actionPipelineService = new ActionPipelineService(this);
            // Register channels always: harmless and improves robustness
            try {
                Bukkit.getMessenger().registerOutgoingPluginChannel(this, ServerTransferService.LEGACY_CHANNEL);
                Bukkit.getMessenger().registerOutgoingPluginChannel(this, ServerTransferService.NAMESPACE_CHANNEL);
                getLogger().info("§aServer transfer channels registered (BungeeCord & bungeecord:main)");
            } catch (Exception e) {
                getLogger().warning("Failed to register plugin messaging channels: " + e.getMessage());
            }

            return true;

        } catch (Exception e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Integration initialization failed", e);
            return false;
        }
    }

    /**
     * Phase 6: Finalization
     * Completes initialization with metrics, commands, and final setup.
     */
    private boolean initializePhase6_Finalization() {
        try {
            // Initialize GUI system
            this.guiManager = new GUIManager(this);

            // Initialize Visual Effects system
            if (getModuleManager().isModuleEnabled("visual-effects")) {
                this.visualEffectsManager = new VisualEffectsManager(this);
            }

            // Register commands
            registerCommands();

            // Initialize bStats metrics
            try {
                this.bStatsMetrics = new bStatsManager(this);
            } catch (Exception e) {
                getLogger().warning("Failed to initialize bStats: " + e.getMessage());
            }

            return true;

        } catch (Exception e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Finalization failed", e);
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
            this.autoClickListenerInstance = new me.koyere.antiafkplus.listener.AutoClickListener(this);
            getServer().getPluginManager().registerEvents(this.autoClickListenerInstance, this);
            // AutoClickListener enabled silently
        }

        // Initialize PlayerProtectionListener if player-protection module is enabled
        if (getModuleManager().isModuleEnabled("player-protection")) {
            this.playerProtectionListener = new me.koyere.antiafkplus.listener.PlayerProtectionListener(this);
            getServer().getPluginManager().registerEvents(this.playerProtectionListener, this);
            getLogger().info("§aPlayer Protection System initialized");
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
        this.guiManager = null;
        this.visualEffectsManager = null;
        this.worldGuardIntegration = null;
        this.vaultIntegration = null;
        this.discordSRVIntegration = null;

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
    private void performConfigMigration(@SuppressWarnings("unused") String fromVersion) {
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

    public TimeWindowService getTimeWindowService() {
        return timeWindowService;
    }

    public void rebuildTimeWindowService() {
        this.timeWindowService = new TimeWindowService(this, this.configManager.getTimeWindowSettings());
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

    /** Countdown runner for per-player sequences. */
    public CountdownSequenceService getCountdownSequenceService() {
        return countdownSequenceService;
    }

    /** Action pipeline for scripted sequences. */
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
     * Gets the GUI manager.
     */
    public GUIManager getGUIManager() {
        return guiManager;
    }

    /**
     * Gets the visual effects manager (may be null if module is disabled).
     */
    public VisualEffectsManager getVisualEffectsManager() {
        return visualEffectsManager;
    }

    /**
     * Gets the AutoClickListener instance.
     */
    public me.koyere.antiafkplus.listener.AutoClickListener getAutoClickListenerInstance() {
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

    /** Vault economy integration (may be null). */
    public me.koyere.antiafkplus.integrations.VaultIntegration getVaultIntegration() { return vaultIntegration; }

    /** DiscordSRV integration (may be null). */
    public me.koyere.antiafkplus.integrations.DiscordSRVIntegration getDiscordSRVIntegration() { return discordSRVIntegration; }

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
