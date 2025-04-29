package me.koyere.antiafkplus.api;

import org.bukkit.entity.Player;

/**
 * Public API for other plugins to interact with AntiAFKPlus.
 */
public interface AntiAFKPlusAPI {

    /**
     * Checks if a player is currently marked as AFK.
     * @param player The player to check.
     * @return true if the player is AFK.
     */
    boolean isAFK(Player player);

    /**
     * Marks a player as manually AFK (same effect as using /afk).
     * @param player The player to mark as AFK.
     */
    void markAsAFK(Player player);

    /**
     * Unmarks a player from manual AFK mode, if active.
     * @param player The player to unmark.
     */
    void unmarkAFK(Player player);
}
