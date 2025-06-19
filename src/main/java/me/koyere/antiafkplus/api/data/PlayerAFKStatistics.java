package me.koyere.antiafkplus.api.data;

import java.time.Instant;
import java.util.UUID;

/**
 * Player-specific AFK statistics.
 */
public class PlayerAFKStatistics {
    private final UUID playerUUID;
    private final String playerName;
    private final long totalAfkTime;
    private final int afkSessions;
    private final long longestAfkSession;
    private final double averageSessionLength;
    private final int manualAfkCount;
    private final int autoAfkCount;
    private final Instant firstAfkTime;
    private final Instant lastAfkTime;
    
    public PlayerAFKStatistics(UUID playerUUID, String playerName, long totalAfkTime, int afkSessions,
                              long longestAfkSession, double averageSessionLength, int manualAfkCount,
                              int autoAfkCount, Instant firstAfkTime, Instant lastAfkTime) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.totalAfkTime = totalAfkTime;
        this.afkSessions = afkSessions;
        this.longestAfkSession = longestAfkSession;
        this.averageSessionLength = averageSessionLength;
        this.manualAfkCount = manualAfkCount;
        this.autoAfkCount = autoAfkCount;
        this.firstAfkTime = firstAfkTime;
        this.lastAfkTime = lastAfkTime;
    }
    
    // Getters
    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public long getTotalAfkTime() { return totalAfkTime; }
    public int getAfkSessions() { return afkSessions; }
    public long getLongestAfkSession() { return longestAfkSession; }
    public double getAverageSessionLength() { return averageSessionLength; }
    public int getManualAfkCount() { return manualAfkCount; }
    public int getAutoAfkCount() { return autoAfkCount; }
    public Instant getFirstAfkTime() { return firstAfkTime; }
    public Instant getLastAfkTime() { return lastAfkTime; }
}