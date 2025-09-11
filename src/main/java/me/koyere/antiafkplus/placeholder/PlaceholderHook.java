// PlaceholderHook.java - English comments
package me.koyere.antiafkplus.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.afk.AFKManager; // For AFKManager reference
import org.bukkit.Bukkit; // Not strictly needed here if using offlinePlayer.getPlayer()
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI Expansion for AntiAFKPlus.
 * This class provides placeholders that can be used in other plugins
 * that support PlaceholderAPI.
 *
 * Supported Placeholders:
 * %antiafkplus_status% - Returns "AFK" or "ACTIVE" for the player.
 * %antiafkplus_afktime% - Returns the time in seconds since the player's last recorded activity.
 */
public class PlaceholderHook extends PlaceholderExpansion {

    private final AntiAFKPlus plugin;
    private final AFKManager afkManager; // Store a direct reference for convenience and efficiency

    /**
     * Constructor for the PlaceholderHook.
     * @param plugin The main instance of AntiAFKPlus.
     */
    public PlaceholderHook(AntiAFKPlus plugin) {
        this.plugin = plugin;
        this.afkManager = plugin.getAfkManager(); // Get and store AFKManager
    }

    /**
     * The identifier for this expansion. This is the prefix used in placeholders.
     * Example: %antiafkplus_status%
     * @return The identifier string "antiafkplus".
     */
    @Override
    public @NotNull String getIdentifier() {
        return "antiafkplus";
    }

    /**
     * The author of this expansion.
     * @return The primary author of the plugin.
     */
    @Override
    public @NotNull String getAuthor() {
        // It's safer to check if the authors list is empty or null,
        // though for a plugin.yml, it should always have at least one.
        if (plugin.getDescription().getAuthors() != null && !plugin.getDescription().getAuthors().isEmpty()) {
            return plugin.getDescription().getAuthors().get(0);
        }
        return "Koyere"; // Fallback author
    }

    /**
     * The version of this expansion, usually matches the plugin version.
     * @return The plugin's version string.
     */
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * Determines if the expansion should persist.
     * Returning true means the expansion will not be unregistered when /papi reload is used.
     * @return True to persist, false otherwise.
     */
    @Override
    public boolean persist() {
        return true; // This is common for plugin-specific expansions
    }

    /**
     * Called when a placeholder with the anttiafkplus identifier is requested.
     * @param offlinePlayer The player for whom the placeholder is requested. Can be null.
     * @param identifier The specific placeholder requested (e.g., "status", "afktime").
     * @return The value of the placeholder, or null if the identifier is not recognized.
     * Returns an empty string for offline players or invalid player objects.
     */
    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String identifier) {
        if (offlinePlayer == null) {
            return ""; // Cannot process placeholders for a null OfflinePlayer
        }

        // Attempt to get the online Player instance.
        // Some placeholders might only make sense for online players.
        Player player = offlinePlayer.getPlayer(); // This gets the Player object if online, null otherwise.

        if (player == null || !player.isOnline()) {
            // Decide how to handle offline players. For AFK status/time, it usually implies they are not active.
            // For 'status', returning "OFFLINE" or empty might be appropriate.
            // For 'afktime', it might be irrelevant or could show time since last online activity if tracked.
            // Current implementation returns empty for offline players.
            return "";
        }

        // Ensure AFKManager is available (it should be if plugin is enabled)
        if (this.afkManager == null) {
            return "AFKManager N/A"; // Or null, or empty string
        }

        switch (identifier.toLowerCase()) {
            case "status":
                // Uses the AFKManager's isAFK method to determine current status.
                return this.afkManager.isAFK(player) ? "AFK" : "ACTIVE"; // Consider making "AFK" and "ACTIVE" configurable via messages.yml

            case "afktime":
                // Uses the AFKManager's method to get the last movement/activity timestamp.
                // The method was renamed to getLastMovementTimestampForPlayer.
                long lastActivityTimestamp = this.afkManager.getLastMovementTimestampForPlayer(player);
                if (lastActivityTimestamp <= 0) { // If no valid timestamp, means not tracked or just joined
                    return "0"; // Or "N/A"
                }
                long timeSinceActivityMillis = System.currentTimeMillis() - lastActivityTimestamp;
                long timeSinceActivitySeconds = timeSinceActivityMillis / 1000L;
                return String.valueOf(timeSinceActivitySeconds);

            // --- Credit System Placeholders (v2.5) ---
            case "credits": {
                var cm = plugin.getCreditManager();
                if (cm == null || !cm.isEnabled()) return "";
                return String.valueOf(cm.getBalance(player));
            }
            case "credits_hours": {
                var cm = plugin.getCreditManager();
                if (cm == null || !cm.isEnabled()) return "";
                long minutes = cm.getBalance(player);
                long hours = minutes / 60;
                return String.valueOf(hours);
            }
            case "max_credits": {
                var cm = plugin.getCreditManager();
                if (cm == null || !cm.isEnabled()) return "";
                return String.valueOf(cm.getMaxCredits(player));
            }
            case "credit_ratio": {
                var cm = plugin.getCreditManager();
                if (cm == null || !cm.isEnabled()) return "";
                return cm.getRatioString(player);
            }
            case "in_afk_zone": {
                var cm = plugin.getCreditManager();
                if (cm == null || !cm.isEnabled()) return "false";
                return cm.isInAfkZone(player) ? "true" : "false";
            }
            case "credits_expire_days": {
                var cm = plugin.getCreditManager();
                if (cm == null || !cm.isEnabled()) return "";
                java.time.Instant exp = cm.getExpirationInstant(player);
                if (exp == null) return "";
                long days = java.time.Duration.between(java.time.Instant.now(), exp).toDays();
                if (days < 0) days = 0;
                return String.valueOf(days);
            }

            default:
                // Unknown identifier for this expansion.
                return null;
        }
    }
}
