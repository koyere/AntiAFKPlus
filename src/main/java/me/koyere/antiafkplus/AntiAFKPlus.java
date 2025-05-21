package me.koyere.antiafkplus;

import me.koyere.antiafkplus.afk.AFKManager;
import me.koyere.antiafkplus.afk.AntiAFKActivityDetector;
import me.koyere.antiafkplus.afk.ItemPickupBlocker;
import me.koyere.antiafkplus.afk.MovementListener;
import me.koyere.antiafkplus.api.AntiAFKPlusAPI;
import me.koyere.antiafkplus.api.AntiAFKPlusAPIImpl;
import me.koyere.antiafkplus.command.AFKCommand;
import me.koyere.antiafkplus.command.AFKPlusCommand;
import me.koyere.antiafkplus.config.ConfigManager;
import me.koyere.antiafkplus.listener.AutoClickListener;
import me.koyere.antiafkplus.placeholder.PlaceholderHook;
import me.koyere.antiafkplus.utils.bStatsManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for AntiAFKPlus.
 * Handles the initialization and disabling of plugin components.
 */
public final class AntiAFKPlus extends JavaPlugin {

    private static AntiAFKPlus instance;
    private ConfigManager configManager;
    private AFKManager afkManager;
    private MovementListener movementListener;
    private AntiAFKPlusAPI api;

    // Command Handlers
    private AFKCommand afkCommandHandler;
    private AFKPlusCommand afkPlusCommandHandler;

    // Optional Listeners that might need specific shutdown/reload logic
    private AutoClickListener autoClickListenerInstance;
    private AntiAFKActivityDetector antiAFKActivityDetectorInstance;


    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig(); // Saves config.yml if not present

        this.configManager = new ConfigManager(this); // Manages config.yml and messages.yml

        this.movementListener = new MovementListener(); // Handles player movement and basic activity

        this.afkManager = new AFKManager(this, this.movementListener); // Core AFK logic

        this.api = new AntiAFKPlusAPIImpl(this); // Public API

        // Register MovementListener (handles join, quit, kick, and various activities)
        getServer().getPluginManager().registerEvents(this.movementListener, this);

        // Register other standard listeners
        ItemPickupBlocker itemPickupBlocker = new ItemPickupBlocker(this);
        getServer().getPluginManager().registerEvents(itemPickupBlocker, this);

        // Initialize and register AntiAFKActivityDetector
        this.antiAFKActivityDetectorInstance = new AntiAFKActivityDetector(this);
        getServer().getPluginManager().registerEvents(this.antiAFKActivityDetectorInstance, this);

        // Initialize and register AutoClickListener if enabled in config
        if (this.configManager.isAutoclickDetectionEnabled()) {
            this.autoClickListenerInstance = new AutoClickListener(this); // Pass plugin instance
            getServer().getPluginManager().registerEvents(this.autoClickListenerInstance, this);
            getLogger().info("AutoClickListener feature is enabled."); // Changed from warning to info for active feature
        } else {
            getLogger().info("Autoclick detection is disabled in config.yml.");
        }

        // Register commands and their tab completers
        this.afkCommandHandler = new AFKCommand(this);
        if (getCommand("afk") != null) {
            getCommand("afk").setExecutor(this.afkCommandHandler);
            getCommand("afk").setTabCompleter(this.afkCommandHandler);
        } else {
            getLogger().severe("Failed to register 'afk' command! Check plugin.yml.");
        }

        this.afkPlusCommandHandler = new AFKPlusCommand(this);
        if (getCommand("afkplus") != null) {
            getCommand("afkplus").setExecutor(this.afkPlusCommandHandler);
            getCommand("afkplus").setTabCompleter(this.afkPlusCommandHandler);
        } else {
            getLogger().severe("Failed to register 'afkplus' command! Check plugin.yml.");
        }

        // PlaceholderAPI hook
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this).register();
            getLogger().info("PlaceholderAPI detected. Placeholders registered.");
        } else {
            getLogger().info("PlaceholderAPI not found. Placeholders will not be available.");
        }

        // bStats metrics
        try {
            new bStatsManager(this);
            getLogger().info("bStats metrics successfully initialized.");
        } catch (Exception e) {
            getLogger().warning("Failed to initialize bStats: " + e.getMessage());
        }

        getLogger().info(this.getDescription().getName() + " version " + this.getDescription().getVersion() + " has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling " + this.getDescription().getName() + "...");

        // Shutdown managers and tasks
        if (this.afkManager != null) {
            this.afkManager.shutdown();
        }
        if (this.autoClickListenerInstance != null) {
            this.autoClickListenerInstance.shutdown();
        }
        if (this.antiAFKActivityDetectorInstance != null) {
            this.antiAFKActivityDetectorInstance.shutdown();
        }

        // Nullify references to help with garbage collection
        this.api = null;
        this.afkManager = null;
        this.movementListener = null;
        this.configManager = null;
        this.afkCommandHandler = null;
        this.afkPlusCommandHandler = null;
        this.autoClickListenerInstance = null;
        this.antiAFKActivityDetectorInstance = null;

        getLogger().info(this.getDescription().getName() + " has been disabled.");
    }

    /**
     * Gets the static instance of the plugin.
     * @return The AntiAFKPlus plugin instance.
     */
    public static AntiAFKPlus getInstance() {
        return instance;
    }

    /**
     * Gets the configuration manager for the plugin.
     * @return The ConfigManager instance.
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Gets the AFK manager for the plugin.
     * @return The AFKManager instance.
     */
    public AFKManager getAfkManager() {
        return afkManager;
    }

    /**
     * Gets the public API for AntiAFKPlus.
     * @return The AntiAFKPlusAPI implementation.
     */
    public AntiAFKPlusAPI getAPI() {
        return api;
    }

    /**
     * Gets the AutoClickListener instance if it's enabled.
     * Useful for commands like reload to call specific reload methods on the listener.
     * @return The AutoClickListener instance, or null if not enabled.
     */
    public AutoClickListener getAutoClickListenerInstance() {
        return autoClickListenerInstance;
    }

    // Getter for AntiAFKActivityDetectorInstance might also be useful if it had reloadable config
    // public AntiAFKActivityDetector getAntiAFKActivityDetectorInstance() {
    //    return antiAFKActivityDetectorInstance;
    // }
}