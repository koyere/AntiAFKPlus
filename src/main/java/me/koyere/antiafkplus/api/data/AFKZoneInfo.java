package me.koyere.antiafkplus.api.data;

import java.time.Duration;
import java.util.Map;

/**
 * Information about an AFK zone.
 */
public class AFKZoneInfo {
    private final String zoneName;
    private final String zoneType;
    private final boolean afkAllowed;
    private final Duration customTimeout;
    private final Map<String, Object> zoneProperties;
    
    public AFKZoneInfo(String zoneName, String zoneType, boolean afkAllowed, Duration customTimeout, Map<String, Object> zoneProperties) {
        this.zoneName = zoneName;
        this.zoneType = zoneType;
        this.afkAllowed = afkAllowed;
        this.customTimeout = customTimeout;
        this.zoneProperties = zoneProperties;
    }
    
    public String getZoneName() { return zoneName; }
    public String getZoneType() { return zoneType; }
    public boolean isAfkAllowed() { return afkAllowed; }
    public Duration getCustomTimeout() { return customTimeout; }
    public Map<String, Object> getZoneProperties() { return zoneProperties; }
}