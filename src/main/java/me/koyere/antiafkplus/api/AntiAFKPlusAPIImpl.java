package me.koyere.antiafkplus.api;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.afk.AFKManager;
import org.bukkit.entity.Player;

/**
 * Internal implementation of the AntiAFKPlusAPI.
 * This class provides the concrete logic for the API methods.
 */
public class AntiAFKPlusAPIImpl implements AntiAFKPlusAPI {

    private final AntiAFKPlus plugin; // Kept for potential direct plugin access if needed
    private final AFKManager afkManager;

    /**
     * Constructor for AntiAFKPlusAPIImpl.
     * @param plugin The main plugin instance.
     */
    public AntiAFKPlusAPIImpl(AntiAFKPlus plugin) {
        this.plugin = plugin;
        this.afkManager = plugin.getAfkManager();
    }

    /**
     * Checks if a player is currently marked as AFK by the plugin.
     * @param player The player to check.
     * @return True if the player is AFK, false otherwise. Returns false if player is null.
     */
    @Override
    public boolean isAFK(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }
        return this.afkManager.isAFK(player);
    }

    /**
     * Gets the time in milliseconds since the player was last considered active (e.g., last movement).
     * @param player The player to check.
     * @return Milliseconds since last activity, or 0 if player is null or not tracked.
     */
    @Override
    public long getTimeSinceLastActivity(Player player) {
        if (player == null || !player.isOnline()) { // AFKManager might be null if plugin is disabling
            return 0;
        }
        long lastActivityTimestamp = this.afkManager.getLastMovementTimestampForPlayer(player);
        if (lastActivityTimestamp <= 0) return 0;
        return System.currentTimeMillis() - lastActivityTimestamp;
    }


    /**
     * Forcibly marks a player as AFK (manual mode).
     * If the player is already AFK, this ensures they are in manual AFK.
     * @param player The player to mark as AFK.
     */
    @Override
    public void markAsAFK(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        this.afkManager.forceSetManualAFKState(player, true);
    }

    /**
     * Forcibly unmarks a player as AFK.
     * If the player was AFK (either manually or automatically), this will make them active.
     * @param player The player to unmark as AFK.
     */
    @Override
    public void unmarkAFK(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        // Calling forceSetManualAFKState(player, false) will ensure they are not manually AFK
        // and will also call the general unmarkAsAFKInternal if they were in afkPlayers set.
        this.afkManager.forceSetManualAFKState(player, false);
    }
}