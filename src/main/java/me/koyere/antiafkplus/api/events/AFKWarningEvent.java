package me.koyere.antiafkplus.api.events;

import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;

/**
 * Event fired when an AFK warning is sent to a player.
 */
public class AFKWarningEvent {
    private final Player player;
    private final Duration timeRemaining;
    private final int warningNumber;
    private final int totalWarnings;
    private final String warningMessage;
    private final Instant timestamp;
    private boolean cancelled = false;
    private String customMessage;
    private boolean sendTitle = true;
    
    public AFKWarningEvent(Player player, Duration timeRemaining, int warningNumber, int totalWarnings,
                          String warningMessage, Instant timestamp) {
        this.player = player;
        this.timeRemaining = timeRemaining;
        this.warningNumber = warningNumber;
        this.totalWarnings = totalWarnings;
        this.warningMessage = warningMessage;
        this.timestamp = timestamp;
    }
    
    public Player getPlayer() { return player; }
    public Duration getTimeRemaining() { return timeRemaining; }
    public int getWarningNumber() { return warningNumber; }
    public int getTotalWarnings() { return totalWarnings; }
    public String getWarningMessage() { return warningMessage; }
    public Instant getTimestamp() { return timestamp; }
    
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    
    public boolean isFinalWarning() { return warningNumber >= totalWarnings; }

    public String getCustomMessage() { return customMessage; }
    public void setCustomMessage(String customMessage) { this.customMessage = customMessage; }

    public boolean shouldSendTitle() { return sendTitle; }
    public void setSendTitle(boolean sendTitle) { this.sendTitle = sendTitle; }
}
