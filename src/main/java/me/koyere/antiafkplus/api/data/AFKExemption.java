package me.koyere.antiafkplus.api.data;

import java.time.Duration;
import java.time.Instant;

/**
 * Represents an AFK exemption for a player.
 */
public class AFKExemption {
    private final String reason;
    private final Instant grantedTime;
    private final Duration duration;
    private final boolean permanent;
    
    public AFKExemption(String reason, Instant grantedTime, Duration duration, boolean permanent) {
        this.reason = reason;
        this.grantedTime = grantedTime;
        this.duration = duration;
        this.permanent = permanent;
    }
    
    public String getReason() { return reason; }
    public Instant getGrantedTime() { return grantedTime; }
    public Duration getDuration() { return duration; }
    public boolean isPermanent() { return permanent; }
    
    public boolean isExpired() {
        if (permanent) return false;
        return Instant.now().isAfter(grantedTime.plus(duration));
    }
}