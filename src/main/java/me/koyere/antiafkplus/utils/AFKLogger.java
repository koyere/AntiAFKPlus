package me.koyere.antiafkplus.utils;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.entity.Player;
// No specific java.util.logging.Level import needed if only using plugin.getLogger()

/**
 * Centralized logger for AFK events.
 * Handles important messages related to AFK status for clarity in console.
 */
public class AFKLogger {

    // No static plugin instance here to avoid early initialization issues.

    /**
     * Logs when a player enters AFK mode.
     * @param player The player entering AFK.
     * @param type "manual" or "auto"
     */
    public static void logAFKEnter(Player player, String type) {
        AntiAFKPlus pluginInstance = AntiAFKPlus.getInstance();
        if (pluginInstance != null) {
            pluginInstance.getLogger().info("[AFK] " + player.getName() + " is now AFK (" + type + ").");
        } else {
            // Fallback or error if plugin instance is not available
            System.out.println("[AFKLogger_ERROR] AntiAFKPlus instance not available for logAFKEnter. Player: " + (player != null ? player.getName() : "null") + ", Type: " + type);
        }
    }

    /**
     * Logs when a player exits AFK mode.
     * @param player The player exiting AFK.
     * @param type "manual" or "auto"
     * @param totalSeconds Time in seconds since AFK started (optional, -1 to skip)
     */
    public static void logAFKExit(Player player, String type, long totalSeconds) {
        AntiAFKPlus pluginInstance = AntiAFKPlus.getInstance();
        if (pluginInstance != null) {
            if (totalSeconds >= 0) {
                pluginInstance.getLogger().info("[AFK] " + player.getName() + " returned from AFK (" + type + ") after " + totalSeconds + "s.");
            } else {
                pluginInstance.getLogger().info("[AFK] " + player.getName() + " is no longer AFK (" + type + ").");
            }
        } else {
            System.out.println("[AFKLogger_ERROR] AntiAFKPlus instance not available for logAFKExit. Player: " + (player != null ? player.getName() : "null") + ", Type: " + type);
        }
    }

    /**
     * Logs when a player is kicked for being AFK.
     * @param player The player kicked.
     * @param durationSeconds Time in seconds since last movement.
     */
    public static void logAFKKick(Player player, long durationSeconds) {
        AntiAFKPlus pluginInstance = AntiAFKPlus.getInstance();
        if (pluginInstance != null) {
            pluginInstance.getLogger().warning("[AFK] " + player.getName() + " was kicked for being AFK after " + durationSeconds + "s.");
        } else {
            System.out.println("[AFKLogger_ERROR] AntiAFKPlus instance not available for logAFKKick. Player: " + (player != null ? player.getName() : "null"));
        }
    }

    /**
     * Logs when a warning is sent before AFK kick.
     * @param player The player warned.
     * @param secondsRemaining Time left before kick.
     */
    public static void logAFKWarning(Player player, int secondsRemaining) {
        AntiAFKPlus pluginInstance = AntiAFKPlus.getInstance();
        if (pluginInstance != null) {
            pluginInstance.getLogger().info("[AFK] Warning sent to " + player.getName() + ": " + secondsRemaining + "s before kick.");
        } else {
            System.out.println("[AFKLogger_ERROR] AntiAFKPlus instance not available for logAFKWarning. Player: " + (player != null ? player.getName() : "null"));
        }
    }

    /**
     * Generic activity logger for internal plugin use.
     * This can be used for debugging or tracking specific plugin actions.
     * @param message The message to log.
     */
    public static void logActivity(String message) {
        AntiAFKPlus pluginInstance = AntiAFKPlus.getInstance();
        if (pluginInstance != null) {
            // Example: only log if debug is enabled in config, or always log as INFO
            // Assuming a method like isDebugEnabled() exists in your ConfigManager
            // For simplicity, let's assume it logs if debug is on or if no such config exists (logs as INFO)
            boolean debugEnabled = false; // Default to false
            if (pluginInstance.getConfigManager() != null) { // Ensure ConfigManager is also available
                // debugEnabled = pluginInstance.getConfigManager().isDebugEnabled(); // Hypothetical method
            }

            if (debugEnabled) { // If you implement a debug mode check
                pluginInstance.getLogger().info("[Activity_DEBUG] " + message);
            } else {
                // If not for debug, or if debug isn't a feature for this specific log,
                // you might log it as a standard INFO or not at all, depending on the message's nature.
                // For now, let's log it as info if not specifically a debug message.
                // pluginInstance.getLogger().info("[Activity] " + message);
                // For the logActivity("player data cleared") example, it's fine as INFO.
                pluginInstance.getLogger().info("[Activity] " + message);
            }

        } else {
            System.out.println("[AFKLogger_ERROR] AntiAFKPlus instance not available for logActivity. Message: " + message);
        }
    }
}