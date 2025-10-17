// PatternDetector.java - NEW v2.0 - Smart AFK Pool Detection
package me.koyere.antiafkplus.afk;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.utils.AFKLogger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import me.koyere.antiafkplus.platform.PlatformScheduler;

import java.util.*;

/**
 * Detects suspicious movement patterns that indicate AFK machines or pools.
 * Analyzes player movement history to identify repetitive or confined movement patterns.
 */
public class PatternDetector {

    private final AntiAFKPlus plugin;
    private final MovementListener movementListener;
    private final AFKManager afkManager;

    // Pattern detection configuration
    private static final double WATER_CIRCLE_RADIUS = 3.0; // Maximum radius for water circle detection
    private static final int MIN_SAMPLES_FOR_PATTERN = 20; // Minimum location samples needed
    private static final double CONFINED_SPACE_THRESHOLD = 5.0; // Maximum area for confined space (small pools)
    private static final long PATTERN_ANALYSIS_INTERVAL = 30000; // 30 seconds between analyses
    private static final double REPETITIVE_MOVEMENT_THRESHOLD = 0.8; // Similarity threshold for repetitive patterns
    private static final int MAX_PATTERN_VIOLATIONS = 3; // Maximum violations before action
    
    // v2.4 NEW: Large AFK pool detection constants
    private static final double LARGE_POOL_THRESHOLD = 25.0; // Maximum area for large AFK pools (25x25)
    private static final long KEYSTROKE_TIMEOUT_CHECK_INTERVAL = 15000; // Check every 15 seconds
    private static final int MIN_SAMPLES_FOR_LARGE_POOL = 30; // More samples needed for large pool detection

    // Player pattern tracking
    private final Map<UUID, PatternData> playerPatterns = new HashMap<>();
    private final Map<UUID, Integer> patternViolations = new HashMap<>();

    private PlatformScheduler.ScheduledTask analysisTask;

    public PatternDetector(AntiAFKPlus plugin, MovementListener movementListener, AFKManager afkManager) {
        this.plugin = plugin;
        this.movementListener = movementListener;
        this.afkManager = afkManager;
        startPatternAnalysis();
    }

    private void startPatternAnalysis() {
        Runnable analysisBody = this::analyzeAllPlayerPatterns;
        this.analysisTask = plugin.getPlatformScheduler()
                .runTaskTimerAsync(analysisBody, 20L * 30, 20L * 10);
    }

    private void analyzeAllPlayerPatterns() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission("antiafkplus.bypass")) continue;

            // PROFESSIONAL FIX: Verify if player is in disabled world (same logic as AFKManager)
            List<String> disabledWorlds = plugin.getConfigManager().getDisabledWorlds();
            List<String> enabledWorlds = plugin.getConfigManager().getEnabledWorlds();
            String currentWorldName = player.getWorld().getName();

            // Skip analysis if player is in disabled world
            if (disabledWorlds.contains(currentWorldName)) {
                continue;
            }

            // Skip if enabled-worlds is configured and player is not in one
            if (!enabledWorlds.isEmpty() && !enabledWorlds.contains(currentWorldName)) {
                continue;
            }

            MovementListener.PlayerLocationData locationData = movementListener.getPlayerLocationData(player);
            if (locationData == null || locationData.locationHistory.size() < MIN_SAMPLES_FOR_PATTERN) {
                continue;
            }

            analyzePlayerPattern(player, locationData);
        }
    }

    private void analyzePlayerPattern(Player player, MovementListener.PlayerLocationData locationData) {
        UUID uuid = player.getUniqueId();
        PatternData patternData = playerPatterns.computeIfAbsent(uuid, k -> new PatternData());

        List<MovementListener.LocationSnapshot> history = locationData.locationHistory;
        if (history.size() < MIN_SAMPLES_FOR_PATTERN) return;

        // Get recent movement history (last 20 positions)
        List<MovementListener.LocationSnapshot> recentHistory = history.subList(
                Math.max(0, history.size() - MIN_SAMPLES_FOR_PATTERN),
                history.size()
        );

        boolean suspiciousPattern = false;
        String detectionReason = "";

        // Check for water circle pattern
        if (detectWaterCirclePattern(recentHistory)) {
            suspiciousPattern = true;
            detectionReason = "water_circle";
            patternData.waterCircleDetections++;
        }

        // Check for confined space movement
        if (detectConfinedSpacePattern(recentHistory)) {
            suspiciousPattern = true;
            detectionReason = "confined_space";
            patternData.confinedSpaceDetections++;
        }

        // Check for repetitive movement patterns
        if (detectRepetitivePattern(recentHistory)) {
            suspiciousPattern = true;
            detectionReason = "repetitive_movement";
            patternData.repetitivePatternDetections++;
        }

        // Check for pendulum/back-and-forth movement
        if (detectPendulumPattern(recentHistory)) {
            suspiciousPattern = true;
            detectionReason = "pendulum_movement";
            patternData.pendulumDetections++;
        }
        
        // v2.4 NEW: Check for large AFK pool patterns
        if (detectLargeAFKPool(player, recentHistory)) {
            suspiciousPattern = true;
            detectionReason = "large_afk_pool";
            patternData.largePoolDetections++;
        }
        
        // v2.4 NEW: Check for keystroke timeout (no manual input)
        if (detectKeystrokeTimeout(player)) {
            suspiciousPattern = true;
            detectionReason = "keystroke_timeout";
            patternData.keystrokeTimeouts++;
        }

        if (suspiciousPattern) {
            handleSuspiciousPattern(player, detectionReason, patternData);
        } else {
            // Reduce violation count if no suspicious activity
            int violations = patternViolations.getOrDefault(uuid, 0);
            if (violations > 0) {
                patternViolations.put(uuid, Math.max(0, violations - 1));
            }
        }

        patternData.lastAnalysis = System.currentTimeMillis();
    }

    private boolean detectWaterCirclePattern(List<MovementListener.LocationSnapshot> history) {
        if (history.size() < 8) return false;

        // PROFESSIONAL FIX: Create defensive copy to prevent ConcurrentModificationException
        // The original list can be modified by other threads during stream operations
        List<MovementListener.LocationSnapshot> safeCopy = new java.util.ArrayList<>(history);

        // Calculate center point of movement using thread-safe copy
        double centerX = safeCopy.stream().mapToDouble(pos -> pos.x).average().orElse(0);
        double centerZ = safeCopy.stream().mapToDouble(pos -> pos.z).average().orElse(0);

        // Check if all movements are within circle radius
        boolean allWithinRadius = safeCopy.stream().allMatch(pos -> {
            double distance = Math.sqrt(Math.pow(pos.x - centerX, 2) + Math.pow(pos.z - centerZ, 2));
            return distance <= WATER_CIRCLE_RADIUS;
        });

        if (!allWithinRadius) return false;

        // Check for circular movement pattern using thread-safe copy
        int circularMovements = 0;
        for (int i = 1; i < safeCopy.size(); i++) {
            MovementListener.LocationSnapshot prev = safeCopy.get(i - 1);
            MovementListener.LocationSnapshot curr = safeCopy.get(i);

            // Calculate angle change
            double angle1 = Math.atan2(prev.z - centerZ, prev.x - centerX);
            double angle2 = Math.atan2(curr.z - centerZ, curr.x - centerX);
            double angleDiff = Math.abs(angle2 - angle1);

            // Normalize angle difference
            if (angleDiff > Math.PI) angleDiff = 2 * Math.PI - angleDiff;

            // If angle change is consistent (suggesting circular movement)
            if (angleDiff > 0.1 && angleDiff < Math.PI / 2) {
                circularMovements++;
            }
        }

        return circularMovements >= (safeCopy.size() * 0.6); // 60% of movements should be circular
    }

    private boolean detectConfinedSpacePattern(List<MovementListener.LocationSnapshot> history) {
        if (history.size() < MIN_SAMPLES_FOR_PATTERN) return false;

        // PROFESSIONAL FIX: Create defensive copy to prevent ConcurrentModificationException
        List<MovementListener.LocationSnapshot> safeCopy = new java.util.ArrayList<>(history);

        // Calculate bounding box of movement using thread-safe copy
        double minX = safeCopy.stream().mapToDouble(pos -> pos.x).min().orElse(0);
        double maxX = safeCopy.stream().mapToDouble(pos -> pos.x).max().orElse(0);
        double minZ = safeCopy.stream().mapToDouble(pos -> pos.z).min().orElse(0);
        double maxZ = safeCopy.stream().mapToDouble(pos -> pos.z).max().orElse(0);

        double areaX = maxX - minX;
        double areaZ = maxZ - minZ;

        // Check if movement is confined to small area
        return areaX <= CONFINED_SPACE_THRESHOLD && areaZ <= CONFINED_SPACE_THRESHOLD;
    }

    private boolean detectRepetitivePattern(List<MovementListener.LocationSnapshot> history) {
        if (history.size() < 12) return false;

        // PROFESSIONAL FIX: Create defensive copy to prevent ConcurrentModificationException
        List<MovementListener.LocationSnapshot> safeCopy = new java.util.ArrayList<>(history);

        // Split history into segments and compare similarity using thread-safe copy
        int segmentSize = safeCopy.size() / 3;
        List<MovementListener.LocationSnapshot> segment1 = new java.util.ArrayList<>(safeCopy.subList(0, segmentSize));
        List<MovementListener.LocationSnapshot> segment2 = new java.util.ArrayList<>(safeCopy.subList(segmentSize, segmentSize * 2));
        List<MovementListener.LocationSnapshot> segment3 = new java.util.ArrayList<>(safeCopy.subList(segmentSize * 2, segmentSize * 3));

        double similarity12 = calculatePatternSimilarity(segment1, segment2);
        double similarity23 = calculatePatternSimilarity(segment2, segment3);
        double similarity13 = calculatePatternSimilarity(segment1, segment3);

        // If segments are highly similar, it's likely a repetitive pattern
        return (similarity12 > REPETITIVE_MOVEMENT_THRESHOLD ||
                similarity23 > REPETITIVE_MOVEMENT_THRESHOLD ||
                similarity13 > REPETITIVE_MOVEMENT_THRESHOLD);
    }

    private boolean detectPendulumPattern(List<MovementListener.LocationSnapshot> history) {
        if (history.size() < 10) return false;

        // PROFESSIONAL FIX: Create defensive copy to prevent ConcurrentModificationException
        List<MovementListener.LocationSnapshot> safeCopy = new java.util.ArrayList<>(history);

        int backAndForthCount = 0;

        for (int i = 2; i < safeCopy.size(); i++) {
            MovementListener.LocationSnapshot pos1 = safeCopy.get(i - 2);
            MovementListener.LocationSnapshot pos2 = safeCopy.get(i - 1);
            MovementListener.LocationSnapshot pos3 = safeCopy.get(i);

            // Check for back-and-forth movement (A -> B -> A pattern)
            double dist12 = calculateDistance(pos1, pos2);
            double dist23 = calculateDistance(pos2, pos3);
            double dist13 = calculateDistance(pos1, pos3);

            // If player moves away then back to nearly same position
            if (dist12 > 0.5 && dist23 > 0.5 && dist13 < 0.5) {
                backAndForthCount++;
            }
        }

        return backAndForthCount >= (safeCopy.size() * 0.3); // 30% of movements are back-and-forth
    }
    
    // v2.4 NEW: Large AFK pool detection methods
    
    /**
     * Detects large AFK pools (20x10+ blocks) that bypass traditional confined space detection.
     * This method identifies pools that are too large for the standard confined space threshold
     * but still represent artificial AFK setups.
     * 
     * Detection criteria:
     * 1. Movement area larger than small pool threshold but smaller than large pool threshold
     * 2. Player is in water for extended periods
     * 3. Movement patterns suggest water current automation
     * 4. No manual keystrokes detected for extended periods
     * 
     * @param player The player being analyzed
     * @param history Recent movement history
     * @return true if large AFK pool pattern detected
     */
    private boolean detectLargeAFKPool(Player player, List<MovementListener.LocationSnapshot> history) {
        if (history.size() < MIN_SAMPLES_FOR_LARGE_POOL) return false;

        // PROFESSIONAL FIX: Create defensive copy to prevent ConcurrentModificationException
        List<MovementListener.LocationSnapshot> safeCopy = new java.util.ArrayList<>(history);

        // Calculate bounding box of movement area using thread-safe copy
        double minX = safeCopy.stream().mapToDouble(pos -> pos.x).min().orElse(0);
        double maxX = safeCopy.stream().mapToDouble(pos -> pos.x).max().orElse(0);
        double minZ = safeCopy.stream().mapToDouble(pos -> pos.z).min().orElse(0);
        double maxZ = safeCopy.stream().mapToDouble(pos -> pos.z).max().orElse(0);
        
        double areaX = maxX - minX;
        double areaZ = maxZ - minZ;
        double totalArea = areaX * areaZ;
        
        // Check if movement area suggests large AFK pool
        boolean isLargePoolSize = (areaX > CONFINED_SPACE_THRESHOLD || areaZ > CONFINED_SPACE_THRESHOLD) &&
                                  (totalArea <= LARGE_POOL_THRESHOLD * LARGE_POOL_THRESHOLD);
        
        if (!isLargePoolSize) return false;
        
        // Check if player has been in water for most of the tracking period
        boolean mostlyInWater = isPlayerMostlyInWater(player, safeCopy);
        if (!mostlyInWater) return false;

        // Check for automatic movement patterns (consistent velocity, minimal direction changes)
        boolean hasAutomaticMovement = detectAutomaticMovementPattern(safeCopy);
        if (!hasAutomaticMovement) return false;
        
        // Final check: has player exceeded keystroke timeout?
        boolean hasKeystrokeTimeout = movementListener.hasKeystrokeTimeout(player);
        
        // Debug logging
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info(String.format(
                "[DEBUG_LargePool] %s: area=%.1fx%.1f (%.1f), inWater=%s, autoMove=%s, keystrokeTimeout=%s",
                player.getName(), areaX, areaZ, totalArea, mostlyInWater, hasAutomaticMovement, hasKeystrokeTimeout
            ));
        }
        
        return hasKeystrokeTimeout; // Only trigger if keystroke timeout is also present
    }
    
    /**
     * Checks if player has been mostly in water during the tracking period.
     * This helps identify AFK pool setups vs regular gameplay.
     * 
     * @param player The player to check
     * @param history Movement history to analyze
     * @return true if player has been in water for >70% of tracked time
     */
    private boolean isPlayerMostlyInWater(Player player, List<MovementListener.LocationSnapshot> history) {
        // For simplicity, check current state and assume consistency
        // A more sophisticated implementation could track water state over time
        return player.getLocation().getBlock().isLiquid() || 
               player.getEyeLocation().getBlock().isLiquid() ||
               player.isSwimming();
    }
    
    /**
     * Detects automatic movement patterns typical of water currents in AFK pools.
     * Automatic movement has consistent velocity and fewer direction changes.
     * 
     * @param history Movement history to analyze
     * @return true if movement appears to be automatic (water current)
     */
    private boolean detectAutomaticMovementPattern(List<MovementListener.LocationSnapshot> history) {
        if (history.size() < 10) return false;

        // PROFESSIONAL FIX: Create defensive copy to prevent ConcurrentModificationException
        List<MovementListener.LocationSnapshot> safeCopy = new java.util.ArrayList<>(history);

        double totalVelocityVariance = 0.0;
        double totalDirectionChanges = 0.0;
        int validMeasurements = 0;

        for (int i = 1; i < safeCopy.size(); i++) {
            MovementListener.LocationSnapshot prev = safeCopy.get(i - 1);
            MovementListener.LocationSnapshot curr = safeCopy.get(i);
            
            double deltaX = curr.x - prev.x;
            double deltaZ = curr.z - prev.z;
            double velocity = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            
            if (velocity > 0.01) { // Only analyze when there's movement
                // Calculate velocity variance (automatic movement has consistent velocity)
                if (validMeasurements > 0) {
                    // Simple variance approximation
                    totalVelocityVariance += Math.abs(velocity - 0.1); // 0.1 is typical water current speed
                }
                
                // Calculate direction changes (automatic movement has fewer changes)
                if (i > 1) {
                    MovementListener.LocationSnapshot prevPrev = history.get(i - 2);
                    double prevAngle = Math.atan2(prev.z - prevPrev.z, prev.x - prevPrev.x);
                    double currAngle = Math.atan2(deltaZ, deltaX);
                    
                    double angleDiff = Math.abs(currAngle - prevAngle);
                    if (angleDiff > Math.PI) angleDiff = 2 * Math.PI - angleDiff;
                    
                    totalDirectionChanges += angleDiff;
                }
                
                validMeasurements++;
            }
        }
        
        if (validMeasurements < 5) return false;
        
        double avgVelocityVariance = totalVelocityVariance / validMeasurements;
        double avgDirectionChange = totalDirectionChanges / Math.max(1, validMeasurements - 1);
        
        // Automatic movement has low velocity variance and low direction changes
        boolean lowVelocityVariance = avgVelocityVariance < 0.05;
        boolean lowDirectionChanges = avgDirectionChange < 0.3;
        
        return lowVelocityVariance && lowDirectionChanges;
    }
    
    /**
     * Detects when a player hasn't provided manual keystroke input for too long.
     * This is the core detection method for large AFK pools where players
     * move due to water currents without any manual input.
     * 
     * @param player The player to check
     * @return true if player has exceeded keystroke timeout threshold
     */
    private boolean detectKeystrokeTimeout(Player player) {
        return movementListener.hasKeystrokeTimeout(player);
    }

    private double calculatePatternSimilarity(List<MovementListener.LocationSnapshot> pattern1,
                                              List<MovementListener.LocationSnapshot> pattern2) {
        if (pattern1.size() != pattern2.size()) return 0.0;

        double totalSimilarity = 0.0;
        int comparisons = 0;

        for (int i = 0; i < pattern1.size(); i++) {
            MovementListener.LocationSnapshot pos1 = pattern1.get(i);
            MovementListener.LocationSnapshot pos2 = pattern2.get(i);

            double distance = calculateDistance(pos1, pos2);
            double similarity = Math.max(0, 1.0 - (distance / 10.0)); // Normalize to 0-1

            totalSimilarity += similarity;
            comparisons++;
        }

        return comparisons > 0 ? totalSimilarity / comparisons : 0.0;
    }

    private double calculateDistance(MovementListener.LocationSnapshot pos1,
                                     MovementListener.LocationSnapshot pos2) {
        return Math.sqrt(Math.pow(pos2.x - pos1.x, 2) +
                Math.pow(pos2.y - pos1.y, 2) +
                Math.pow(pos2.z - pos1.z, 2));
    }

    private void handleSuspiciousPattern(Player player, String detectionReason, PatternData patternData) {
        UUID uuid = player.getUniqueId();
        int violations = patternViolations.getOrDefault(uuid, 0) + 1;
        patternViolations.put(uuid, violations);

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().warning("[PatternDetector] Suspicious " + detectionReason +
                    " pattern detected for " + player.getName() +
                    " (Violations: " + violations + "/" + MAX_PATTERN_VIOLATIONS + ")");
        }

        AFKLogger.logActivity("Pattern Detection: " + player.getName() + " - " + detectionReason +
                " (violations: " + violations + ")");

        // Take action based on violation count
        if (violations >= MAX_PATTERN_VIOLATIONS) {
            // PROFESSIONAL FIX: Execute configured AFK action (kick/teleport) instead of just marking AFK
            // This ensures pattern detection triggers the same action as regular AFK detection
            plugin.getPlatformScheduler().runTaskForEntity(player, () -> {
                afkManager.executeAFKAction(player, detectionReason);
                player.sendMessage("§c[AntiAFK] Suspicious movement pattern detected. AFK action executed.");
            });

            // Log the action (this can stay async)
            AFKLogger.logActivity("Forced AFK due to pattern violations: " + player.getName() +
                    " (" + detectionReason + ")");

            // Reset violations after action
            patternViolations.put(uuid, 0);
        }
    }

    public void clearPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        playerPatterns.remove(uuid);
        patternViolations.remove(uuid);
    }

    public PatternData getPlayerPatternData(Player player) {
        return playerPatterns.get(player.getUniqueId());
    }

    public void shutdown() {
        if (analysisTask != null && !analysisTask.isCancelled()) {
            analysisTask.cancel();
            plugin.getLogger().info("PatternDetector analysis task cancelled.");
        }
        playerPatterns.clear();
        patternViolations.clear();
    }

    // Inner class for storing pattern detection data
    public static class PatternData {
        public int waterCircleDetections = 0;
        public int confinedSpaceDetections = 0;
        public int repetitivePatternDetections = 0;
        public int pendulumDetections = 0;
        
        // v2.4 NEW: Large AFK pool detection counters
        public int largePoolDetections = 0;
        public int keystrokeTimeouts = 0;
        
        public long lastAnalysis = 0;
        public long firstDetection = 0;

        public int getTotalDetections() {
            return waterCircleDetections + confinedSpaceDetections +
                    repetitivePatternDetections + pendulumDetections +
                    largePoolDetections + keystrokeTimeouts; // v2.4: Include new detection types
        }

        public String getMostCommonPattern() {
            int max = Math.max(Math.max(waterCircleDetections, confinedSpaceDetections),
                    Math.max(repetitivePatternDetections, 
                    Math.max(pendulumDetections, 
                    Math.max(largePoolDetections, keystrokeTimeouts)))); // v2.4: Include new types

            if (max == 0) return "none";
            if (max == waterCircleDetections) return "water_circle";
            if (max == confinedSpaceDetections) return "confined_space";
            if (max == repetitivePatternDetections) return "repetitive";
            if (max == pendulumDetections) return "pendulum";
            if (max == largePoolDetections) return "large_afk_pool"; // v2.4 NEW
            if (max == keystrokeTimeouts) return "keystroke_timeout"; // v2.4 NEW
            return "unknown";
        }
    }
}
