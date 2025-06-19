// PlayerAFKPatternDetectedEvent.java - NEW v2.0 - Pattern detection event
package me.koyere.antiafkplus.events;

import me.koyere.antiafkplus.afk.PatternDetector;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Called when the pattern detection system identifies suspicious player behavior.
 * This event is cancellable and allows custom handling of pattern violations.
 */
public class PlayerAFKPatternDetectedEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final PatternType patternType;
    private final double confidence;
    private final int violationCount;
    private final long detectionTimespan;
    private final Location detectionLocation;
    private final PatternDetector.PatternData patternData;
    private final Map<String, Object> additionalData;

    private boolean cancelled = false;
    private PatternAction suggestedAction = PatternAction.LOG_ONLY;
    private String customMessage;
    private boolean notifyAdmins = true;

    /**
     * Creates a new PlayerAFKPatternDetectedEvent.
     *
     * @param player The player whose pattern was detected
     * @param patternType The type of pattern detected
     * @param confidence Confidence level of detection (0.0 to 1.0)
     * @param violationCount Number of violations for this player
     * @param detectionTimespan Time span over which pattern was analyzed (milliseconds)
     * @param detectionLocation Location where pattern was detected
     * @param patternData Additional pattern analysis data
     * @param additionalData Extra data specific to the pattern type
     */
    public PlayerAFKPatternDetectedEvent(@NotNull Player player,
                                         @NotNull PatternType patternType,
                                         double confidence,
                                         int violationCount,
                                         long detectionTimespan,
                                         @NotNull Location detectionLocation,
                                         @NotNull PatternDetector.PatternData patternData,
                                         @NotNull Map<String, Object> additionalData) {
        super(player);
        this.patternType = patternType;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        this.violationCount = Math.max(1, violationCount);
        this.detectionTimespan = Math.max(0, detectionTimespan);
        this.detectionLocation = detectionLocation.clone();
        this.patternData = patternData;
        this.additionalData = additionalData;
    }

    /**
     * Gets the type of pattern that was detected.
     * @return Pattern type
     */
    @NotNull
    public PatternType getPatternType() {
        return patternType;
    }

    /**
     * Gets the confidence level of the pattern detection.
     * @return Confidence from 0.0 (uncertain) to 1.0 (certain)
     */
    public double getConfidence() {
        return confidence;
    }

    /**
     * Gets the confidence as a percentage.
     * @return Confidence percentage (0-100)
     */
    public int getConfidencePercentage() {
        return (int) Math.round(confidence * 100);
    }

    /**
     * Gets the number of pattern violations for this player.
     * @return Violation count
     */
    public int getViolationCount() {
        return violationCount;
    }

    /**
     * Gets the time span over which the pattern was analyzed.
     * @return Analysis timespan in milliseconds
     */
    public long getDetectionTimespan() {
        return detectionTimespan;
    }

    /**
     * Gets the time span over which the pattern was analyzed in seconds.
     * @return Analysis timespan in seconds
     */
    public long getDetectionTimespanSeconds() {
        return detectionTimespan / 1000L;
    }

    /**
     * Gets the location where the pattern was detected.
     * @return Detection location (cloned for safety)
     */
    @NotNull
    public Location getDetectionLocation() {
        return detectionLocation.clone();
    }

    /**
     * Gets the pattern analysis data.
     * @return Pattern data containing detection statistics
     */
    @NotNull
    public PatternDetector.PatternData getPatternData() {
        return patternData;
    }

    /**
     * Gets additional data specific to the pattern type.
     * @return Map of additional data
     */
    @NotNull
    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    /**
     * Gets a specific additional data value.
     * @param key Data key
     * @return Data value or null if not found
     */
    @Nullable
    public Object getAdditionalData(@NotNull String key) {
        return additionalData.get(key);
    }

    /**
     * Gets the suggested action for this pattern detection.
     * @return Suggested action
     */
    @NotNull
    public PatternAction getSuggestedAction() {
        return suggestedAction;
    }

    /**
     * Sets the suggested action for this pattern detection.
     * @param suggestedAction Action to take
     */
    public void setSuggestedAction(@NotNull PatternAction suggestedAction) {
        this.suggestedAction = suggestedAction;
    }

    /**
     * Gets the custom message for this detection.
     * @return Custom message or null if using default
     */
    @Nullable
    public String getCustomMessage() {
        return customMessage;
    }

    /**
     * Sets a custom message for this detection.
     * @param customMessage Custom message
     */
    public void setCustomMessage(@Nullable String customMessage) {
        this.customMessage = customMessage;
    }

    /**
     * Checks if admins should be notified of this detection.
     * @return true if admins should be notified
     */
    public boolean shouldNotifyAdmins() {
        return notifyAdmins;
    }

    /**
     * Sets whether admins should be notified of this detection.
     * @param notifyAdmins true to notify admins
     */
    public void setNotifyAdmins(boolean notifyAdmins) {
        this.notifyAdmins = notifyAdmins;
    }

    /**
     * Checks if this is a high-confidence detection.
     * @return true if confidence >= 0.8
     */
    public boolean isHighConfidence() {
        return confidence >= 0.8;
    }

    /**
     * Checks if this is a repeated violation.
     * @return true if violation count > 1
     */
    public boolean isRepeatedViolation() {
        return violationCount > 1;
    }

    /**
     * Checks if this detection should trigger immediate action.
     * @return true if high confidence and repeated violation
     */
    public boolean shouldTriggerImmediateAction() {
        return isHighConfidence() && isRepeatedViolation();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    /**
     * Enumeration of pattern types that can be detected.
     */
    public enum PatternType {
        /**
         * Circular movement pattern (water circles, AFK pools).
         */
        WATER_CIRCLE("Water Circle Movement", "Player moving in circular pattern, likely in water"),

        /**
         * Movement confined to a very small area.
         */
        CONFINED_SPACE("Confined Movement", "Player movement restricted to small area"),

        /**
         * Repetitive movement pattern.
         */
        REPETITIVE_MOVEMENT("Repetitive Pattern", "Player repeating the same movement sequence"),

        /**
         * Back-and-forth pendulum movement.
         */
        PENDULUM_MOVEMENT("Pendulum Movement", "Player moving back and forth in pendulum pattern"),

        /**
         * Suspicious clicking patterns.
         */
        AUTOCLICK_PATTERN("Autoclick Pattern", "Repetitive clicking detected, possible bot behavior"),

        /**
         * Fishing exploit patterns.
         */
        FISHING_EXPLOIT("Fishing Exploit", "Suspicious fishing behavior detected"),

        /**
         * Block breaking patterns.
         */
        MINING_PATTERN("Mining Pattern", "Repetitive block breaking pattern"),

        /**
         * Mob interaction patterns.
         */
        MOB_INTERACTION("Mob Interaction", "Suspicious mob interaction pattern"),

        /**
         * Multiple pattern types detected simultaneously.
         */
        COMBINED_PATTERNS("Combined Patterns", "Multiple suspicious patterns detected"),

        /**
         * Custom pattern from external detection.
         */
        CUSTOM("Custom Pattern", "Custom pattern detected by external system");

        private final String displayName;
        private final String description;

        PatternType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Enumeration of possible actions for pattern violations.
     */
    public enum PatternAction {
        /**
         * Only log the detection, no player action.
         */
        LOG_ONLY,

        /**
         * Send warning message to player.
         */
        WARN_PLAYER,

        /**
         * Force player into AFK mode.
         */
        FORCE_AFK,

        /**
         * Kick player from server.
         */
        KICK_PLAYER,

        /**
         * Teleport player away from the location.
         */
        TELEPORT_AWAY,

        /**
         * Temporarily freeze player movement.
         */
        FREEZE_PLAYER,

        /**
         * Execute custom command.
         */
        CUSTOM_COMMAND,

        /**
         * Notify staff members only.
         */
        NOTIFY_STAFF,

        /**
         * Take no action (cancelled by other plugin).
         */
        NO_ACTION
    }

    @Override
    public String toString() {
        return "PlayerAFKPatternDetectedEvent{" +
                "player=" + getPlayer().getName() +
                ", patternType=" + patternType +
                ", confidence=" + confidence +
                ", violationCount=" + violationCount +
                ", detectionTimespan=" + detectionTimespan +
                ", location=" + detectionLocation.getWorld().getName() +
                "(" + detectionLocation.getBlockX() + "," + detectionLocation.getBlockY() + "," + detectionLocation.getBlockZ() + ")" +
                ", suggestedAction=" + suggestedAction +
                ", cancelled=" + cancelled +
                '}';
    }
}