package me.koyere.antiafkplus.api.data;

import org.bukkit.Location;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Historical AFK data for a player.
 */
public class AFKHistoryData {
    private final UUID playerUUID;
    private final Duration period;
    private final List<AFKSession> sessions;
    private final Map<String, Object> trends;
    
    public AFKHistoryData(UUID playerUUID, Duration period, List<AFKSession> sessions, Map<String, Object> trends) {
        this.playerUUID = playerUUID;
        this.period = period;
        this.sessions = sessions;
        this.trends = trends;
    }
    
    public UUID getPlayerUUID() { return playerUUID; }
    public Duration getPeriod() { return period; }
    public List<AFKSession> getSessions() { return sessions; }
    public Map<String, Object> getTrends() { return trends; }
    
    /**
     * Individual AFK session data.
     */
    public static class AFKSession {
        private final Instant startTime;
        private final Instant endTime;
        private final AFKStatus statusType;
        private final String reason;
        private final Location location;
        
        public AFKSession(Instant startTime, Instant endTime, AFKStatus statusType, String reason, Location location) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.statusType = statusType;
            this.reason = reason;
            this.location = location;
        }
        
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        public AFKStatus getStatusType() { return statusType; }
        public String getReason() { return reason; }
        public Location getLocation() { return location; }
        
        public Duration getDuration() {
            return Duration.between(startTime, endTime != null ? endTime : Instant.now());
        }
    }
}