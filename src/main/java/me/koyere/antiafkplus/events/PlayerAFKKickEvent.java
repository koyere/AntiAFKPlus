// PlayerAFKKickEvent.java - NEW v2.0 - AFK kick event
package me.koyere.antiafkplus.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a player is about to be kicked for being AFK.
 * This event is cancellable and allows custom actions instead of kicking.
 */
public class PlayerAFKKickEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final KickReason kickReason;
    private final long afkDurationSeconds;
    private final long totalInactivitySeconds;
    private final String detectionMethod;
    private final int warningsSent;

    private boolean cancelled = false;
    private String kickMessage;
    private KickAction customAction = KickAction.KICK;
    private String customActionData;

    /**
     * Creates a new PlayerAFKKickEvent.
     *
     * @param player The player being kicked
     * @param kickReason The reason for the kick
     * @param afkDurationSeconds How long the player has been marked as AFK (seconds)
     * @param totalInactivitySeconds Total time since last activity (seconds)
     * @param detectionMethod Method used to detect AFK status
     * @param warningsSent Number of warnings sent before this kick
     * @param kickMessage The kick message to be shown
     */
    public PlayerAFKKickEvent(@NotNull Player player,
                              @NotNull KickReason kickReason,
                              long afkDurationSeconds,
                              long totalInactivitySeconds,
                              @NotNull String detectionMethod,
                              int warningsSent,
                              @NotNull String kickMessage) {
        super(player);
        this.kickReason = kickReason;
        this.afkDurationSeconds = Math.max(0, afkDurationSeconds);
        this.totalInactivitySeconds = Math.max(0, totalInactivitySeconds);
        this.detectionMethod = detectionMethod;
        this.warningsSent = Math.max(0, warningsSent);
        this.kickMessage = kickMessage;
    }

    /**
     * Gets the reason for this AFK kick.
     * @return Kick reason
     */
    @NotNull
    public KickReason getKickReason() {
        return kickReason;
    }

    /**
     * Gets how long the player has been marked as AFK in seconds.
     * @return AFK duration in seconds
     */
    public long getAfkDurationSeconds() {
        return afkDurationSeconds;
    }

    /**
     * Gets how long the player has been marked as AFK in milliseconds.
     * @return AFK duration in milliseconds
     */
    public long getAfkDurationMillis() {
        return afkDurationSeconds * 1000L;
    }

    /**
     * Gets the total time since the player's last activity in seconds.
     * @return Total inactivity time in seconds
     */
    public long getTotalInactivitySeconds() {
        return totalInactivitySeconds;
    }

    /**
     * Gets the total time since the player's last activity in milliseconds.
     * @return Total inactivity time in milliseconds
     */
    public long getTotalInactivityMillis() {
        return totalInactivitySeconds * 1000L;
    }

    /**
     * Gets the detection method that led to this kick.
     * @return Detection method (e.g., "standard", "pattern_detection", "autoclick")
     */
    @NotNull
    public String getDetectionMethod() {
        return detectionMethod;
    }

    /**
     * Gets the number of warnings sent to the player before this kick.
     * @return Number of warnings sent
     */
    public int getWarningsSent() {
        return warningsSent;
    }

    /**
     * Gets the kick message that will be displayed to the player.
     * @return Kick message
     */
    @NotNull
    public String getKickMessage() {
        return kickMessage;
    }

    /**
     * Sets a custom kick message.
     * @param kickMessage New kick message
     */
    public void setKickMessage(@NotNull String kickMessage) {
        this.kickMessage = kickMessage;
    }

    /**
     * Gets the custom action to perform instead of kicking.
     * @return Custom action
     */
    @NotNull
    public KickAction getCustomAction() {
        return customAction;
    }

    /**
     * Sets a custom action to perform instead of kicking.
     * @param customAction Action to perform
     */
    public void setCustomAction(@NotNull KickAction customAction) {
        this.customAction = customAction;
    }

    /**
     * Gets additional data for custom actions.
     * @return Custom action data or null
     */
    @Nullable
    public String getCustomActionData() {
        return customActionData;
    }

    /**
     * Sets additional data for custom actions.
     * For TELEPORT: destination coordinates or world name
     * For COMMAND: command to execute
     * For GAMEMODE: gamemode name
     * @param customActionData Action-specific data
     */
    public void setCustomActionData(@Nullable String customActionData) {
        this.customActionData = customActionData;
    }

    /**
     * Checks if warnings were sent before this kick.
     * @return true if warnings were sent
     */
    public boolean hadWarnings() {
        return warningsSent > 0;
    }

    /**
     * Checks if this kick is due to pattern detection.
     * @return true if pattern detection caused the kick
     */
    public boolean isPatternDetectionKick() {
        return kickReason == KickReason.PATTERN_DETECTION ||
                detectionMethod.contains("pattern");
    }

    /**
     * Checks if this kick is due to autoclick detection.
     * @return true if autoclick detection caused the kick
     */
    public boolean isAutoclickKick() {
        return kickReason == KickReason.AUTOCLICK_DETECTION ||
                detectionMethod.contains("autoclick");
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
     * Enumeration of kick reasons.
     */
    public enum KickReason {
        /**
         * Standard inactivity timeout.
         */
        INACTIVITY_TIMEOUT,

        /**
         * Suspicious movement pattern detected.
         */
        PATTERN_DETECTION,

        /**
         * Autoclicker or bot behavior detected.
         */
        AUTOCLICK_DETECTION,

        /**
         * Voluntary AFK time limit exceeded.
         */
        VOLUNTARY_TIME_LIMIT,

        /**
         * Administrative action.
         */
        ADMIN_ACTION,

        /**
         * API call or external plugin.
         */
        API_CALL,

        /**
         * Custom or other reason.
         */
        OTHER
    }

    /**
     * Enumeration of possible actions instead of kicking.
     */
    public enum KickAction {
        /**
         * Standard kick (remove from server).
         */
        KICK,

        /**
         * Teleport to a specific location (AFK room, spawn, etc).
         */
        TELEPORT,

        /**
         * Change player's gamemode (e.g., to spectator).
         */
        GAMEMODE,

        /**
         * Execute a custom command.
         */
        COMMAND,

        /**
         * Move to AFK state without kicking.
         */
        MARK_AFK_ONLY,

        /**
         * Send to another server (BungeeCord/Velocity).
         */
        TRANSFER_SERVER,

        /**
         * Do nothing (completely cancel the kick).
         */
        NONE
    }

    @Override
    public String toString() {
        return "PlayerAFKKickEvent{" +
                "player=" + getPlayer().getName() +
                ", kickReason=" + kickReason +
                ", afkDurationSeconds=" + afkDurationSeconds +
                ", totalInactivitySeconds=" + totalInactivitySeconds +
                ", detectionMethod='" + detectionMethod + '\'' +
                ", warningsSent=" + warningsSent +
                ", customAction=" + customAction +
                ", cancelled=" + cancelled +
                '}';
    }
}