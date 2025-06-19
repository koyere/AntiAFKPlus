// PlayerAFKWarningEvent.java - NEW v2.0 - AFK warning event
package me.koyere.antiafkplus.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a player receives an AFK warning before being kicked.
 * This event is cancellable and allows customization of warning behavior.
 */
public class PlayerAFKWarningEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final int secondsRemaining;
    private final int warningNumber;
    private final int totalWarnings;
    private final WarningType warningType;
    private final long timeSinceLastActivity;
    private final String lastActivityType;

    private boolean cancelled = false;
    private String customMessage;
    private boolean sendTitle = true;
    private boolean sendSound = true;

    /**
     * Creates a new PlayerAFKWarningEvent.
     *
     * @param player The player receiving the warning
     * @param secondsRemaining Seconds until the player will be kicked
     * @param warningNumber The current warning number (1, 2, 3, etc.)
     * @param totalWarnings Total number of warnings configured
     * @param warningType The type of warning being sent
     * @param timeSinceLastActivity Milliseconds since last player activity
     * @param lastActivityType Description of the last detected activity
     */
    public PlayerAFKWarningEvent(@NotNull Player player,
                                 int secondsRemaining,
                                 int warningNumber,
                                 int totalWarnings,
                                 @NotNull WarningType warningType,
                                 long timeSinceLastActivity,
                                 @NotNull String lastActivityType) {
        super(player);
        this.secondsRemaining = Math.max(0, secondsRemaining);
        this.warningNumber = Math.max(1, warningNumber);
        this.totalWarnings = Math.max(1, totalWarnings);
        this.warningType = warningType;
        this.timeSinceLastActivity = Math.max(0, timeSinceLastActivity);
        this.lastActivityType = lastActivityType;
    }

    /**
     * Gets the number of seconds remaining until the player is kicked.
     * @return Seconds remaining
     */
    public int getSecondsRemaining() {
        return secondsRemaining;
    }

    /**
     * Gets the current warning number in the sequence.
     * @return Warning number (1-based)
     */
    public int getWarningNumber() {
        return warningNumber;
    }

    /**
     * Gets the total number of warnings configured.
     * @return Total warnings
     */
    public int getTotalWarnings() {
        return totalWarnings;
    }

    /**
     * Gets the type of warning being sent.
     * @return Warning type
     */
    @NotNull
    public WarningType getWarningType() {
        return warningType;
    }

    /**
     * Gets the time since the player's last activity in milliseconds.
     * @return Milliseconds since last activity
     */
    public long getTimeSinceLastActivity() {
        return timeSinceLastActivity;
    }

    /**
     * Gets the time since the player's last activity in seconds.
     * @return Seconds since last activity
     */
    public long getTimeSinceLastActivitySeconds() {
        return timeSinceLastActivity / 1000L;
    }

    /**
     * Gets a description of the player's last detected activity.
     * @return Activity type (e.g., "movement", "chat", "interaction")
     */
    @NotNull
    public String getLastActivityType() {
        return lastActivityType;
    }

    /**
     * Checks if this is the final warning before kick.
     * @return true if this is the last warning
     */
    public boolean isFinalWarning() {
        return warningNumber >= totalWarnings;
    }

    /**
     * Checks if this is the first warning in the sequence.
     * @return true if this is warning number 1
     */
    public boolean isFirstWarning() {
        return warningNumber == 1;
    }

    /**
     * Gets the custom warning message, if set.
     * @return Custom message or null if using default
     */
    @Nullable
    public String getCustomMessage() {
        return customMessage;
    }

    /**
     * Sets a custom warning message to override the default.
     * @param customMessage Custom message with placeholders
     */
    public void setCustomMessage(@Nullable String customMessage) {
        this.customMessage = customMessage;
    }

    /**
     * Checks if a title should be sent with the warning.
     * @return true if title should be sent
     */
    public boolean shouldSendTitle() {
        return sendTitle;
    }

    /**
     * Sets whether a title should be sent with the warning.
     * @param sendTitle true to send title
     */
    public void setSendTitle(boolean sendTitle) {
        this.sendTitle = sendTitle;
    }

    /**
     * Checks if a sound should be played with the warning.
     * @return true if sound should be played
     */
    public boolean shouldSendSound() {
        return sendSound;
    }

    /**
     * Sets whether a sound should be played with the warning.
     * @param sendSound true to play sound
     */
    public void setSendSound(boolean sendSound) {
        this.sendSound = sendSound;
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
     * Enumeration of warning types.
     */
    public enum WarningType {
        /**
         * Standard inactivity warning.
         */
        STANDARD,

        /**
         * Warning due to suspicious pattern detection.
         */
        PATTERN_DETECTION,

        /**
         * Warning due to autoclicker detection.
         */
        AUTOCLICK_DETECTION,

        /**
         * Critical warning (final warning before kick).
         */
        CRITICAL,

        /**
         * Custom warning type from external source.
         */
        CUSTOM
    }

    @Override
    public String toString() {
        return "PlayerAFKWarningEvent{" +
                "player=" + getPlayer().getName() +
                ", secondsRemaining=" + secondsRemaining +
                ", warningNumber=" + warningNumber +
                ", totalWarnings=" + totalWarnings +
                ", warningType=" + warningType +
                ", timeSinceLastActivity=" + timeSinceLastActivity +
                ", lastActivityType='" + lastActivityType + '\'' +
                ", cancelled=" + cancelled +
                '}';
    }
}