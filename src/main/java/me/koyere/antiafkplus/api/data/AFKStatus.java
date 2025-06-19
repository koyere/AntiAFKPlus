package me.koyere.antiafkplus.api.data;

/**
 * Enumeration of possible AFK status states for players.
 */
public enum AFKStatus {
    /**
     * Player is active and not AFK.
     */
    ACTIVE("Active", false),
    
    /**
     * Player is manually AFK (used /afk command).
     */
    MANUAL_AFK("Manual AFK", true),
    
    /**
     * Player is automatically detected as AFK due to inactivity.
     */
    AUTO_AFK("Auto AFK", true),
    
    /**
     * Player is exempt from AFK detection.
     */
    EXEMPT("Exempt", false),
    
    /**
     * Player's AFK status is unknown or not tracked.
     */
    UNKNOWN("Unknown", false);
    
    private final String displayName;
    private final boolean isAFK;
    
    AFKStatus(String displayName, boolean isAFK) {
        this.displayName = displayName;
        this.isAFK = isAFK;
    }
    
    /**
     * Get the human-readable display name for this status.
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if this status represents an AFK state.
     * @return true if the player is considered AFK
     */
    public boolean isAFK() {
        return isAFK;
    }
    
    /**
     * Check if this is a manual AFK status.
     * @return true if manually set AFK
     */
    public boolean isManual() {
        return this == MANUAL_AFK;
    }
    
    /**
     * Check if this is an automatic AFK status.
     * @return true if automatically detected AFK
     */
    public boolean isAutomatic() {
        return this == AUTO_AFK;
    }
    
    /**
     * Check if the player is active.
     * @return true if the player is active
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    /**
     * Check if the player is exempt.
     * @return true if the player is exempt
     */
    public boolean isExempt() {
        return this == EXEMPT;
    }
}