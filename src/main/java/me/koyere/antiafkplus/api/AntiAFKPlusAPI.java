package me.koyere.antiafkplus.api;

import org.bukkit.entity.Player;

/**
 * Public API for the AntiAFKPlus plugin.
 * Allows other plugins to interact with the AFK status of players.
 */
public interface AntiAFKPlusAPI {

    /**
     * Checks if a player is currently marked as AFK.
     * This includes both automatically detected AFK and manually set AFK.
     *
     * @param player The player to check.
     * @return {@code true} if the player is considered AFK, {@code false} otherwise.
     * Returns {@code false} if the player is null or offline.
     */
    boolean isAFK(Player player);

    /**
     * Gets the time in milliseconds since the player was last considered active
     * (e.g., last movement, chat, or other interaction tracked by the plugin).
     *
     * @param player The player to check.
     * @return Milliseconds since the player's last recorded activity.
     * Returns 0 if the player is null, offline, or not yet tracked.
     */
    long getTimeSinceLastActivity(Player player);

    /**
     * Forcibly marks a player as being in AFK mode (specifically, manual AFK).
     * If the player is already in manual AFK mode, their state will be refreshed.
     * If they are in automatic AFK mode, they will be switched to manual AFK.
     * If they are active, they will be set to manual AFK.
     * This will typically trigger any "enter AFK" announcements or effects.
     *
     * @param player The player to mark as AFK. Must not be null and should be online.
     */
    void markAsAFK(Player player);

    /**
     * Forcibly unmarks a player from AFK mode, making them active.
     * If the player was in any AFK mode (manual or automatic), they will be set to active.
     * This will typically trigger any "exit AFK" announcements or effects.
     *
     * @param player The player to unmark from AFK. Must not be null and should be online.
     */
    void unmarkAFK(Player player);

    // Future API methods could include:
    // - getAFKType(Player player) -> Enum { MANUAL, AUTO, NONE }
    // - addAFKExemptReason(Player player, String reasonKey)
    // - removeAFKExemptReason(Player player, String reasonKey)
    // - registerAFKListener(Consumer<PlayerAFKStateChangeEvent> listener) for API events
}