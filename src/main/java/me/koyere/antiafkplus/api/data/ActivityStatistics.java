package me.koyere.antiafkplus.api.data;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Activity statistics for a player over a time period.
 */
public class ActivityStatistics {
    private final UUID playerUUID;
    private final Duration period;
    private final Map<ActivityType, Integer> activityCounts;
    private final double totalActivityScore;
    private final double averageActivityRate;
    private final ActivityType mostCommonActivity;
    private final int totalActivities;
    
    public ActivityStatistics(UUID playerUUID, Duration period, Map<ActivityType, Integer> activityCounts,
                             double totalActivityScore, double averageActivityRate, ActivityType mostCommonActivity,
                             int totalActivities) {
        this.playerUUID = playerUUID;
        this.period = period;
        this.activityCounts = activityCounts;
        this.totalActivityScore = totalActivityScore;
        this.averageActivityRate = averageActivityRate;
        this.mostCommonActivity = mostCommonActivity;
        this.totalActivities = totalActivities;
    }
    
    // Getters
    public UUID getPlayerUUID() { return playerUUID; }
    public Duration getPeriod() { return period; }
    public Map<ActivityType, Integer> getActivityCounts() { return activityCounts; }
    public double getTotalActivityScore() { return totalActivityScore; }
    public double getAverageActivityRate() { return averageActivityRate; }
    public ActivityType getMostCommonActivity() { return mostCommonActivity; }
    public int getTotalActivities() { return totalActivities; }
}