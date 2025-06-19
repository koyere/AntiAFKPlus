package me.koyere.antiafkplus.api.data;

import java.util.Map;

/**
 * Server-wide AFK statistics.
 */
public class AFKStatistics {
    private final int totalPlayers;
    private final int afkPlayers;
    private final int manualAfkPlayers;
    private final int autoAfkPlayers;
    private final double averageAfkTime;
    private final long totalAfkTime;
    private final Map<String, Integer> afkReasons;
    
    public AFKStatistics(int totalPlayers, int afkPlayers, int manualAfkPlayers, int autoAfkPlayers,
                        double averageAfkTime, long totalAfkTime, Map<String, Integer> afkReasons) {
        this.totalPlayers = totalPlayers;
        this.afkPlayers = afkPlayers;
        this.manualAfkPlayers = manualAfkPlayers;
        this.autoAfkPlayers = autoAfkPlayers;
        this.averageAfkTime = averageAfkTime;
        this.totalAfkTime = totalAfkTime;
        this.afkReasons = afkReasons;
    }
    
    public int getTotalPlayers() { return totalPlayers; }
    public int getAfkPlayers() { return afkPlayers; }
    public int getManualAfkPlayers() { return manualAfkPlayers; }
    public int getAutoAfkPlayers() { return autoAfkPlayers; }
    public double getAverageAfkTime() { return averageAfkTime; }
    public long getTotalAfkTime() { return totalAfkTime; }
    public Map<String, Integer> getAfkReasons() { return afkReasons; }
    
    public double getAfkPercentage() {
        return totalPlayers > 0 ? (double) afkPlayers / totalPlayers * 100.0 : 0.0;
    }
}