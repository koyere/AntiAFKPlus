package me.koyere.antiafkplus.api.events;

import me.koyere.antiafkplus.api.data.DetectedPattern;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Map;

/**
 * Event fired when a suspicious pattern is detected.
 */
public class PatternDetectionEvent {
    private final Player player;
    private final DetectedPattern pattern;
    private final Instant timestamp;
    private final Map<String, Object> eventData;
    private boolean cancelled = false;
    
    public PatternDetectionEvent(Player player, DetectedPattern pattern, Instant timestamp, Map<String, Object> eventData) {
        this.player = player;
        this.pattern = pattern;
        this.timestamp = timestamp;
        this.eventData = eventData;
    }
    
    public Player getPlayer() { return player; }
    public DetectedPattern getPattern() { return pattern; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, Object> getEventData() { return eventData; }
    
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}