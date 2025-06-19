package me.koyere.antiafkplus.api.data;

import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Comprehensive activity information for a player.
 */
public class PlayerActivityInfo {
    
    private final UUID playerUUID;
    private final String playerName;
    private final Instant lastActivity;
    private final ActivityType lastActivityType;
    private final Duration timeSinceLastActivity;
    private final double activityScore;
    private final int recentMovements;
    private final int recentHeadRotations;
    private final int recentJumps;
    private final int recentCommands;
    private final int recentInteractions;
    private final Map<ActivityType, Integer> activityCounts;
    private final Map<ActivityType, Instant> lastActivityTimes;
    private final boolean isHighActivity;
    private final boolean isLowActivity;
    private final double averageActivityRate; // activities per minute
    
    public PlayerActivityInfo(UUID playerUUID, String playerName, Instant lastActivity,
                            ActivityType lastActivityType, Duration timeSinceLastActivity,
                            double activityScore, int recentMovements, int recentHeadRotations,
                            int recentJumps, int recentCommands, int recentInteractions,
                            Map<ActivityType, Integer> activityCounts,
                            Map<ActivityType, Instant> lastActivityTimes,
                            boolean isHighActivity, boolean isLowActivity, double averageActivityRate) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.lastActivity = lastActivity;
        this.lastActivityType = lastActivityType;
        this.timeSinceLastActivity = timeSinceLastActivity;
        this.activityScore = activityScore;
        this.recentMovements = recentMovements;
        this.recentHeadRotations = recentHeadRotations;
        this.recentJumps = recentJumps;
        this.recentCommands = recentCommands;
        this.recentInteractions = recentInteractions;
        this.activityCounts = activityCounts;
        this.lastActivityTimes = lastActivityTimes;
        this.isHighActivity = isHighActivity;
        this.isLowActivity = isLowActivity;
        this.averageActivityRate = averageActivityRate;
    }
    
    /**
     * Get the player's UUID.
     * @return The player's UUID
     */
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    /**
     * Get the player's name.
     * @return The player's name
     */
    public String getPlayerName() {
        return playerName;
    }
    
    /**
     * Get the timestamp of the player's last activity.
     * @return The last activity timestamp
     */
    public Instant getLastActivity() {
        return lastActivity;
    }
    
    /**
     * Get the type of the player's last activity.
     * @return The last activity type
     */
    public ActivityType getLastActivityType() {
        return lastActivityType;
    }
    
    /**
     * Get the duration since the player's last activity.
     * @return Time since last activity
     */
    public Duration getTimeSinceLastActivity() {
        return timeSinceLastActivity;
    }
    
    /**
     * Get the player's current activity score.
     * Higher scores indicate more recent and varied activity.
     * @return The activity score (typically 0.0 to 100.0)
     */
    public double getActivityScore() {
        return activityScore;
    }
    
    /**
     * Get the number of recent movements (last 5 minutes).
     * @return Recent movement count
     */
    public int getRecentMovements() {
        return recentMovements;
    }
    
    /**
     * Get the number of recent head rotations (last 5 minutes).
     * @return Recent head rotation count
     */
    public int getRecentHeadRotations() {
        return recentHeadRotations;
    }
    
    /**
     * Get the number of recent jumps (last 5 minutes).
     * @return Recent jump count
     */
    public int getRecentJumps() {
        return recentJumps;
    }
    
    /**
     * Get the number of recent commands (last 5 minutes).
     * @return Recent command count
     */
    public int getRecentCommands() {
        return recentCommands;
    }
    
    /**
     * Get the number of recent interactions (last 5 minutes).
     * @return Recent interaction count
     */
    public int getRecentInteractions() {
        return recentInteractions;
    }
    
    /**
     * Get the total activity counts by type.
     * @return Map of activity types to their counts
     */
    public Map<ActivityType, Integer> getActivityCounts() {
        return activityCounts;
    }
    
    /**
     * Get the last time each activity type was performed.
     * @return Map of activity types to their last timestamps
     */
    public Map<ActivityType, Instant> getLastActivityTimes() {
        return lastActivityTimes;
    }
    
    /**
     * Check if the player is categorized as high activity.
     * @return true if the player has high activity
     */
    public boolean isHighActivity() {
        return isHighActivity;
    }
    
    /**
     * Check if the player is categorized as low activity.
     * @return true if the player has low activity
     */
    public boolean isLowActivity() {
        return isLowActivity;
    }
    
    /**
     * Get the player's average activity rate.
     * @return Activities per minute
     */
    public double getAverageActivityRate() {
        return averageActivityRate;
    }
    
    /**
     * Get the total number of recent activities.
     * @return Sum of all recent activity counts
     */
    public int getTotalRecentActivities() {
        return recentMovements + recentHeadRotations + recentJumps + recentCommands + recentInteractions;
    }
    
    /**
     * Get the count for a specific activity type.
     * @param activityType The activity type
     * @return The count for that activity type, or 0 if not found
     */
    public int getActivityCount(ActivityType activityType) {
        return activityCounts.getOrDefault(activityType, 0);
    }
    
    /**
     * Get the last time a specific activity was performed.
     * @param activityType The activity type
     * @return The last timestamp for that activity, or null if never performed
     */
    public Instant getLastActivityTime(ActivityType activityType) {
        return lastActivityTimes.get(activityType);
    }
    
    /**
     * Check if the player has performed a specific activity recently.
     * @param activityType The activity type
     * @param withinDuration The time frame to check
     * @return true if the activity was performed within the specified duration
     */
    public boolean hasRecentActivity(ActivityType activityType, Duration withinDuration) {
        Instant lastTime = getLastActivityTime(activityType);
        if (lastTime == null) {
            return false;
        }
        return Duration.between(lastTime, Instant.now()).compareTo(withinDuration) <= 0;
    }
    
    /**
     * Get a textual representation of the player's activity level.
     * @return Activity level description
     */
    public String getActivityLevelDescription() {
        if (isHighActivity) {
            return "High Activity";
        } else if (isLowActivity) {
            return "Low Activity";
        } else if (activityScore > 50) {
            return "Moderate Activity";
        } else if (activityScore > 20) {
            return "Light Activity";
        } else {
            return "Minimal Activity";
        }
    }
    
    @Override
    public String toString() {
        return String.format("PlayerActivityInfo{player=%s, lastActivity=%s, activityScore=%.1f, level=%s}",
                playerName, lastActivityType, activityScore, getActivityLevelDescription());
    }
}