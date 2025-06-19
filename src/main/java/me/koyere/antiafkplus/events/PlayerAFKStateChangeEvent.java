// PlayerAFKStateChangeEvent.java - NEW v2.0 - Base AFK state change event
package me.koyere.antiafkplus.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a player's AFK status changes (entering or leaving AFK).
 * This event is cancellable and provides detailed context about the state change.
 */
public class PlayerAFKStateChangeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final AFKState fromState;
    private final AFKState toState;
    private final AFKReason reason;
    private final String detectionMethod;
    private final long activityScore;
    private final boolean wasManual;

    private boolean cancelled = false;
    private String cancelReason;

    /**
     * Creates a new PlayerAFKStateChangeEvent.
     *
     * @param player The player whose AFK state is changing
     * @param fromState The previous AFK state
     * @param toState The new AFK state
     * @param reason The reason for the state change
     * @param detectionMethod The method used to detect the change (e.g., "movement", "pattern", "command")
     * @param activityScore The player's current activity score (0-100)
     * @param wasManual Whether this was triggered by manual action (/afk command)
     */
    public PlayerAFKStateChangeEvent(@NotNull Player player,
                                     @NotNull AFKState fromState,
                                     @NotNull AFKState toState,
                                     @NotNull AFKReason reason,
                                     @NotNull String detectionMethod,
                                     double activityScore,
                                     boolean wasManual) {
        super(player);
        this.fromState = fromState;
        this.toState = toState;
        this.reason = reason;
        this.detectionMethod = detectionMethod;
        this.activityScore = (long) Math.max(0, Math.min(100, activityScore));
        this.wasManual = wasManual;
    }

    /**
     * Gets the player's previous AFK state.
     * @return The state the player was in before this change
     */
    @NotNull
    public AFKState getFromState() {
        return fromState;
    }

    /**
     * Gets the player's new AFK state.
     * @return The state the player is changing to
     */
    @NotNull
    public AFKState getToState() {
        return toState;
    }

    /**
     * Gets the reason for the AFK state change.
     * @return The reason for the change
     */
    @NotNull
    public AFKReason getReason() {
        return reason;
    }

    /**
     * Gets the detection method that triggered this event.
     * @return Detection method (e.g., "movement", "pattern_detection", "command", "timeout")
     */
    @NotNull
    public String getDetectionMethod() {
        return detectionMethod;
    }

    /**
     * Gets the player's current activity score.
     * @return Activity score from 0 (completely inactive) to 100 (very active)
     */
    public long getActivityScore() {
        return activityScore;
    }

    /**
     * Checks if this state change was triggered manually (via /afk command).
     * @return true if manual, false if automatic detection
     */
    public boolean wasManual() {
        return wasManual;
    }

    /**
     * Checks if the player is entering AFK status.
     * @return true if changing from ACTIVE to any AFK state
     */
    public boolean isEnteringAFK() {
        return fromState == AFKState.ACTIVE && (toState == AFKState.AFK_AUTO || toState == AFKState.AFK_MANUAL);
    }

    /**
     * Checks if the player is leaving AFK status.
     * @return true if changing from any AFK state to ACTIVE
     */
    public boolean isLeavingAFK() {
        return (fromState == AFKState.AFK_AUTO || fromState == AFKState.AFK_MANUAL) && toState == AFKState.ACTIVE;
    }

    /**
     * Checks if this is a transition between AFK types (manual <-> auto).
     * @return true if switching between AFK_AUTO and AFK_MANUAL
     */
    public boolean isAFKTypeChange() {
        return (fromState == AFKState.AFK_AUTO && toState == AFKState.AFK_MANUAL) ||
                (fromState == AFKState.AFK_MANUAL && toState == AFKState.AFK_AUTO);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Sets the cancellation reason for debugging purposes.
     * @param reason Human-readable reason for cancellation
     */
    public void setCancelReason(@Nullable String reason) {
        this.cancelReason = reason;
        if (reason != null) {
            setCancelled(true);
        }
    }

    /**
     * Gets the reason this event was cancelled.
     * @return Cancellation reason or null if not cancelled
     */
    @Nullable
    public String getCancelReason() {
        return cancelReason;
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
     * Enumeration of possible AFK states.
     */
    public enum AFKState {
        /**
         * Player is active and not AFK.
         */
        ACTIVE,

        /**
         * Player was automatically detected as AFK by the system.
         */
        AFK_AUTO,

        /**
         * Player manually set themselves as AFK using /afk command.
         */
        AFK_MANUAL
    }

    /**
     * Enumeration of reasons for AFK state changes.
     */
    public enum AFKReason {
        /**
         * Player movement detected.
         */
        MOVEMENT_DETECTED,

        /**
         * Player head rotation detected.
         */
        HEAD_ROTATION,

        /**
         * Player jumped or performed significant movement.
         */
        JUMP_ACTIVITY,

        /**
         * Player executed a command.
         */
        COMMAND_ACTIVITY,

        /**
         * Player interacted with blocks/items/entities.
         */
        INTERACTION,

        /**
         * Player sent a chat message.
         */
        CHAT_ACTIVITY,

        /**
         * No activity detected within timeout period.
         */
        INACTIVITY_TIMEOUT,

        /**
         * Suspicious pattern detected (water circles, confined movement, etc).
         */
        PATTERN_DETECTION,

        /**
         * Autoclicker or repetitive clicking detected.
         */
        AUTOCLICK_DETECTION,

        /**
         * Player manually toggled AFK using /afk command.
         */
        MANUAL_TOGGLE,

        /**
         * API call or external plugin action.
         */
        API_CALL,

        /**
         * Voluntary AFK time limit exceeded.
         */
        TIME_LIMIT_EXCEEDED,

        /**
         * Player joined the server.
         */
        PLAYER_JOIN,

        /**
         * Player left the server.
         */
        PLAYER_QUIT,

        /**
         * Administrative action (forced by staff).
         */
        ADMIN_ACTION,

        /**
         * Other or unknown reason.
         */
        OTHER
    }

    @Override
    public String toString() {
        return "PlayerAFKStateChangeEvent{" +
                "player=" + getPlayer().getName() +
                ", fromState=" + fromState +
                ", toState=" + toState +
                ", reason=" + reason +
                ", detectionMethod='" + detectionMethod + '\'' +
                ", activityScore=" + activityScore +
                ", wasManual=" + wasManual +
                ", cancelled=" + cancelled +
                '}';
    }
}