package me.koyere.antiafkplus.api;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.api.data.*;
import me.koyere.antiafkplus.api.events.*;
import me.koyere.antiafkplus.api.exceptions.*;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * AntiAFKPlus Enterprise API v2.0
 * 
 * Complete public API for AntiAFKPlus Enterprise providing comprehensive
 * AFK management, player tracking, and integration capabilities.
 * 
 * This API is thread-safe and designed for high-performance usage
 * in production environments.
 * 
 * @version 2.0.0
 * @since 2.0.0
 */
public interface AntiAFKPlusAPI {
    
    // ============= STATIC ACCESS =============
    
    /**
     * Get the API instance.
     * @return The AntiAFKPlus API instance
     * @throws IllegalStateException if the plugin is not loaded or enabled
     */
    static AntiAFKPlusAPI getInstance() throws IllegalStateException {
        AntiAFKPlus plugin = AntiAFKPlus.getInstance();
        if (plugin == null || !plugin.isEnabled()) {
            throw new IllegalStateException("AntiAFKPlus is not loaded or enabled");
        }
        return plugin.getAPI();
    }
    
    /**
     * Check if the API is available.
     * @return true if the API is available for use
     */
    static boolean isAvailable() {
        try {
            getInstance();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }
    
    // ============= BASIC AFK STATUS =============
    
    /**
     * Check if a player is currently AFK.
     * 
     * @param player The player to check
     * @return true if the player is AFK (manual or automatic)
     * @throws IllegalArgumentException if player is null
     */
    boolean isAFK(Player player);
    
    /**
     * Check if a player is currently AFK by UUID.
     * 
     * @param playerUUID The player's UUID
     * @return true if the player is AFK, false if not AFK or not online
     */
    boolean isAFK(UUID playerUUID);
    
    /**
     * Get the AFK status type for a player.
     * 
     * @param player The player to check
     * @return The AFK status type
     */
    AFKStatus getAFKStatus(Player player);
    
    /**
     * Check if a player is manually AFK (used /afk command).
     * 
     * @param player The player to check
     * @return true if the player is manually AFK
     */
    boolean isManuallyAFK(Player player);
    
    /**
     * Check if a player is automatically AFK (detected by inactivity).
     * 
     * @param player The player to check
     * @return true if the player is automatically AFK
     */
    boolean isAutomaticallyAFK(Player player);
    
    // ============= AFK MANAGEMENT =============
    
    /**
     * Set a player's AFK status.
     * 
     * @param player The player
     * @param afkStatus The desired AFK status
     * @param reason The reason for the change
     * @throws AFKException if the operation fails
     */
    void setAFKStatus(Player player, AFKStatus afkStatus, String reason) throws AFKException;
    
    /**
     * Toggle a player's AFK status.
     * 
     * @param player The player
     * @return The new AFK status after toggling
     */
    AFKStatus toggleAFK(Player player);
    
    /**
     * Force a player to be AFK (bypasses permissions).
     * 
     * @param player The player
     * @param reason The reason for forcing AFK
     * @param duration Optional duration (null for indefinite)
     */
    void forceAFK(Player player, String reason, Duration duration);
    
    /**
     * Remove AFK status from a player.
     * 
     * @param player The player
     * @param reason The reason for removing AFK
     */
    void removeAFK(Player player, String reason);
    
    // ============= ACTIVITY TRACKING =============
    
    /**
     * Get the time since a player's last activity.
     * 
     * @param player The player
     * @return Duration since last activity
     */
    Duration getTimeSinceLastActivity(Player player);
    
    /**
     * Get detailed activity information for a player.
     * 
     * @param player The player
     * @return Activity information
     */
    PlayerActivityInfo getActivityInfo(Player player);
    
    /**
     * Get the last activity type for a player.
     * 
     * @param player The player
     * @return The type of the last recorded activity
     */
    ActivityType getLastActivityType(Player player);
    
    /**
     * Record custom activity for a player.
     * 
     * @param player The player
     * @param activityType The type of activity
     * @param data Additional activity data
     */
    void recordActivity(Player player, ActivityType activityType, Map<String, Object> data);
    
    /**
     * Get activity statistics for a player.
     * 
     * @param player The player
     * @param period The time period for statistics
     * @return Activity statistics
     */
    ActivityStatistics getActivityStatistics(Player player, Duration period);
    
    // ============= PATTERN DETECTION =============
    
    /**
     * Check if a player has been detected with suspicious patterns.
     * 
     * @param player The player
     * @return true if suspicious patterns were detected
     */
    boolean hasSuspiciousPatterns(Player player);
    
    /**
     * Get detected patterns for a player.
     * 
     * @param player The player
     * @return List of detected patterns
     */
    List<DetectedPattern> getDetectedPatterns(Player player);
    
    /**
     * Get the confidence score for pattern detection.
     * 
     * @param player The player
     * @return Confidence score (0.0 to 1.0)
     */
    double getPatternConfidence(Player player);
    
    /**
     * Clear pattern detection data for a player.
     * 
     * @param player The player
     */
    void clearPatternData(Player player);
    
    // ============= EXEMPTIONS AND BYPASS =============
    
    /**
     * Check if a player is exempt from AFK detection.
     * 
     * @param player The player
     * @return true if the player is exempt
     */
    boolean isExempt(Player player);
    
    /**
     * Add an exemption for a player.
     * 
     * @param player The player
     * @param reason The reason for exemption
     * @param duration Duration of exemption (null for permanent)
     */
    void addExemption(Player player, String reason, Duration duration);
    
    /**
     * Remove an exemption for a player.
     * 
     * @param player The player
     * @param reason The reason to remove (null for all)
     */
    void removeExemption(Player player, String reason);
    
    /**
     * Get all exemptions for a player.
     * 
     * @param player The player
     * @return List of active exemptions
     */
    List<AFKExemption> getExemptions(Player player);
    
    // ============= AFK LISTS AND QUERIES =============
    
    /**
     * Get all currently AFK players.
     * 
     * @return Set of AFK players
     */
    Set<Player> getAFKPlayers();
    
    /**
     * Get AFK players by status type.
     * 
     * @param status The AFK status to filter by
     * @return Set of players with the specified status
     */
    Set<Player> getAFKPlayers(AFKStatus status);
    
    /**
     * Get AFK players in a specific world.
     * 
     * @param world The world to check
     * @return Set of AFK players in the world
     */
    Set<Player> getAFKPlayersInWorld(World world);
    
    /**
     * Get AFK players within a radius of a location.
     * 
     * @param location The center location
     * @param radius The radius in blocks
     * @return Set of AFK players within the radius
     */
    Set<Player> getAFKPlayersNear(Location location, double radius);
    
    /**
     * Get the total count of AFK players.
     * 
     * @return Number of AFK players
     */
    int getAFKPlayerCount();
    
    // ============= TIME LIMITS AND THRESHOLDS =============
    
    /**
     * Get the AFK time limit for a player.
     * 
     * @param player The player
     * @return Duration until the player is marked as AFK
     */
    Duration getAFKTimeLimit(Player player);
    
    /**
     * Get the kick time limit for a player.
     * 
     * @param player The player
     * @return Duration until the player is kicked for being AFK
     */
    Duration getKickTimeLimit(Player player);
    
    /**
     * Set a custom AFK time limit for a player.
     * 
     * @param player The player
     * @param duration The time limit (null to reset to default)
     */
    void setCustomAFKTimeLimit(Player player, Duration duration);
    
    /**
     * Get remaining time until a player is marked as AFK.
     * 
     * @param player The player
     * @return Remaining time, or null if not applicable
     */
    Optional<Duration> getTimeUntilAFK(Player player);
    
    /**
     * Get remaining time until an AFK player is kicked.
     * 
     * @param player The player
     * @return Remaining time, or null if not applicable
     */
    Optional<Duration> getTimeUntilKick(Player player);
    
    // ============= WORLD AND ZONE MANAGEMENT =============
    
    /**
     * Check if AFK detection is enabled in a world.
     * 
     * @param world The world to check
     * @return true if AFK detection is enabled
     */
    boolean isAFKDetectionEnabled(World world);
    
    /**
     * Enable or disable AFK detection in a world.
     * 
     * @param world The world
     * @param enabled Whether to enable AFK detection
     */
    void setAFKDetectionEnabled(World world, boolean enabled);
    
    /**
     * Check if a location is in an AFK-allowed zone.
     * 
     * @param location The location to check
     * @return true if AFK is allowed at this location
     */
    boolean isAFKAllowedAt(Location location);
    
    /**
     * Get AFK zone information for a location.
     * 
     * @param location The location
     * @return AFK zone info, or null if not in a zone
     */
    Optional<AFKZoneInfo> getAFKZoneAt(Location location);
    
    // ============= STATISTICS AND ANALYTICS =============
    
    /**
     * Get comprehensive AFK statistics.
     * 
     * @return Server-wide AFK statistics
     */
    AFKStatistics getAFKStatistics();
    
    /**
     * Get player-specific AFK statistics.
     * 
     * @param player The player
     * @return Player's AFK statistics
     */
    PlayerAFKStatistics getPlayerStatistics(Player player);
    
    /**
     * Get historical AFK data for a player.
     * 
     * @param player The player
     * @param period The time period to analyze
     * @return Historical data
     */
    AFKHistoryData getAFKHistory(Player player, Duration period);
    
    /**
     * Get performance metrics for the AFK system.
     * 
     * @return Performance metrics
     */
    PerformanceMetrics getPerformanceMetrics();
    
    // ============= EVENT HANDLING =============
    
    /**
     * Register an event listener for AFK state changes.
     * 
     * @param listener The listener function
     * @return Registration handle for unregistering
     */
    EventRegistration registerAFKStateListener(Consumer<AFKStateChangeEvent> listener);
    
    /**
     * Register an event listener for pattern detection.
     * 
     * @param listener The listener function
     * @return Registration handle for unregistering
     */
    EventRegistration registerPatternDetectionListener(Consumer<PatternDetectionEvent> listener);
    
    /**
     * Register an event listener for AFK warnings.
     * 
     * @param listener The listener function
     * @return Registration handle for unregistering
     */
    EventRegistration registerWarningListener(Consumer<AFKWarningEvent> listener);
    
    /**
     * Unregister an event listener.
     * 
     * @param registration The registration handle
     */
    void unregisterListener(EventRegistration registration);
    
    // ============= ASYNC OPERATIONS =============
    
    /**
     * Asynchronously get AFK status for a player.
     * 
     * @param player The player
     * @return CompletableFuture with the AFK status
     */
    CompletableFuture<AFKStatus> getAFKStatusAsync(Player player);
    
    /**
     * Asynchronously get activity statistics.
     * 
     * @param player The player
     * @param period The time period
     * @return CompletableFuture with activity statistics
     */
    CompletableFuture<ActivityStatistics> getActivityStatisticsAsync(Player player, Duration period);
    
    /**
     * Asynchronously get AFK history data.
     * 
     * @param player The player
     * @param period The time period
     * @return CompletableFuture with history data
     */
    CompletableFuture<AFKHistoryData> getAFKHistoryAsync(Player player, Duration period);
    
    // ============= CONFIGURATION =============
    
    /**
     * Check if a specific module is enabled.
     * 
     * @param moduleName The module name
     * @return true if the module is enabled
     */
    boolean isModuleEnabled(String moduleName);
    
    /**
     * Get the list of enabled modules.
     * 
     * @return Set of enabled module names
     */
    Set<String> getEnabledModules();
    
    /**
     * Get plugin configuration value.
     * 
     * @param path The configuration path
     * @param defaultValue The default value if not found
     * @param <T> The value type
     * @return The configuration value
     */
    <T> T getConfigValue(String path, T defaultValue);
    
    /**
     * Check if debug mode is enabled.
     * 
     * @return true if debug mode is enabled
     */
    boolean isDebugMode();
    
    // ============= LOCALIZATION =============
    
    /**
     * Get a localized message for a player.
     * 
     * @param player The player
     * @param messageKey The message key
     * @param placeholders Placeholder values
     * @return The localized message
     */
    String getMessage(Player player, String messageKey, Object... placeholders);
    
    /**
     * Get available languages.
     * 
     * @return Set of available language codes
     */
    Set<String> getAvailableLanguages();
    
    /**
     * Get a player's language.
     * 
     * @param player The player
     * @return The player's language code
     */
    String getPlayerLanguage(Player player);
    
    /**
     * Set a player's language.
     * 
     * @param player The player
     * @param languageCode The language code
     */
    void setPlayerLanguage(Player player, String languageCode);
    
    // ============= BEDROCK COMPATIBILITY =============
    
    /**
     * Check if a player is using Bedrock Edition.
     * 
     * @param player The player
     * @return true if the player is using Bedrock
     */
    boolean isBedrockPlayer(Player player);
    
    /**
     * Get Bedrock player information.
     * 
     * @param player The player
     * @return Bedrock player info, or null if not a Bedrock player
     */
    Optional<BedrockPlayerInfo> getBedrockPlayerInfo(Player player);
    
    // ============= UTILITY METHODS =============
    
    /**
     * Get the plugin version.
     * 
     * @return The plugin version string
     */
    String getPluginVersion();
    
    /**
     * Get the API version.
     * 
     * @return The API version string
     */
    default String getAPIVersion() {
        return "2.0.0";
    }
    
    /**
     * Check if the plugin is fully initialized.
     * 
     * @return true if the plugin is fully initialized
     */
    boolean isFullyInitialized();
    
    /**
     * Get comprehensive plugin information.
     * 
     * @return Plugin information
     */
    PluginInfo getPluginInfo();
}