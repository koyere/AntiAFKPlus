package me.koyere.antiafkplus.utils;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bstats.bukkit.Metrics;
// import org.bstats.charts.SimplePie; // Example for custom charts

/**
 * Manages the initialization of bStats metrics for the AntiAFKPlus plugin.
 * This class handles sending anonymous statistical data to bStats.org,
 * helping plugin developers understand how their plugins are used.
 */
public class bStatsManager {

    // The plugin ID for AntiAFKPlus on bStats.org
    // Ensure this ID is correct and officially assigned to your plugin.
    private static final int BSTATS_PLUGIN_ID = 25664; // Used your provided ID

    /**
     * Constructor for bStatsManager.
     * Initializes bStats metrics for the plugin.
     *
     * @param plugin The main instance of the AntiAFKPlus plugin.
     */
    public bStatsManager(AntiAFKPlus plugin) {
        // Initialize bStats Metrics.
        // This will start sending anonymous data to bStats.org.
        // Server owners can disable bStats data collection globally in the bStats config file.
        Metrics metrics = new Metrics(plugin, BSTATS_PLUGIN_ID);

        // Optional: Add custom charts here to track specific plugin features or configurations.
        // For example, to track how many servers have a specific feature enabled:
        /*
        metrics.addCustomChart(new SimplePie("autoclick_detection_enabled", () -> {
            if (plugin.getConfigManager() != null) { // Ensure ConfigManager is available
                return plugin.getConfigManager().isAutoclickDetectionEnabled() ? "Enabled" : "Disabled";
            }
            return "Unknown"; // Fallback if ConfigManager isn't ready or available
        }));
        */

        // Another example:
        /*
        metrics.addCustomChart(new SimplePie("default_afk_time_range", () -> {
            if (plugin.getConfigManager() != null) {
                int time = plugin.getConfigManager().getDefaultAfkTime();
                if (time <= 60) return "1 minute or less";
                if (time <= 300) return "1-5 minutes";
                if (time <= 600) return "5-10 minutes";
                return "More than 10 minutes";
            }
            return "Unknown";
        }));
        */

        // Plugin.getLogger().info("bStats metrics initialized."); // This log is already in AntiAFKPlus.java
    }
}