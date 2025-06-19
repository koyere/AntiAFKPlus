package me.koyere.antiafkplus.api.events;

import me.koyere.antiafkplus.api.data.AFKStatus;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Map;

/**
 * Event fired when a player's AFK status changes.
 */
public class AFKStateChangeEvent {
    private final Player player;
    private final AFKStatus fromStatus;
    private final AFKStatus toStatus;
    private final String reason;
    private final Instant timestamp;
    private final Map<String, Object> eventData;
    
    public AFKStateChangeEvent(Player player, AFKStatus fromStatus, AFKStatus toStatus, 
                              String reason, Instant timestamp, Map<String, Object> eventData) {
        this.player = player;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.timestamp = timestamp;
        this.eventData = eventData;
    }
    
    public Player getPlayer() { return player; }
    public AFKStatus getFromStatus() { return fromStatus; }
    public AFKStatus getToStatus() { return toStatus; }
    public String getReason() { return reason; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, Object> getEventData() { return eventData; }
    
    public boolean isGoingAFK() { return !fromStatus.isAFK() && toStatus.isAFK(); }
    public boolean isLeavingAFK() { return fromStatus.isAFK() && !toStatus.isAFK(); }
}