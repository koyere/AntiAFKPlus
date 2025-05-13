package me.koyere.antiafkplus.utils;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.entity.Player;

/**
 * Centralized logger for AFK events.
 * Handles important messages related to AFK status for clarity in console.
 */
public class AFKLogger {

    private static final AntiAFKPlus plugin = AntiAFKPlus.getInstance();

    /**
     * Logs when a player enters AFK mode.
     * @param player The player entering AFK.
     * @param type "manual" or "auto"
     */
    public static void logAFKEnter(Player player, String type) {
        plugin.getLogger().info("[AFK] " + player.getName() + " is now AFK (" + type + ").");
    }

    /**
     * Logs when a player exits AFK mode.
     * @param player The player exiting AFK.
     * @param type "manual" or "auto"
     * @param totalSeconds Time in seconds since AFK started (optional, -1 to skip)
     */
    public static void logAFKExit(Player player, String type, long totalSeconds) {
        if (totalSeconds >= 0) {
            plugin.getLogger().info("[AFK] " + player.getName() + " returned from AFK (" + type + ") after " + totalSeconds + "s.");
        } else {
            plugin.getLogger().info("[AFK] " + player.getName() + " is no longer AFK (" + type + ").");
        }
    }

    /**
     * Logs when a player is kicked for being AFK.
     * @param player The player kicked.
     * @param durationSeconds Time in seconds since last movement.
     */
    public static void logAFKKick(Player player, long durationSeconds) {
        plugin.getLogger().warning("[AFK] " + player.getName() + " was kicked for being AFK after " + durationSeconds + "s.");
    }

    /**
     * Logs when a warning is sent before AFK kick.
     * @param player The player warned.
     * @param secondsRemaining Time left before kick.
     */
    public static void logAFKWarning(Player player, int secondsRemaining) {
        plugin.getLogger().info("[AFK] Warning sent to " + player.getName() + ": " + secondsRemaining + "s before kick.");
    }
}
