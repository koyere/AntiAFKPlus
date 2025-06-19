// PatternDetector.java - NEW v2.0 - Smart AFK Pool Detection
package me.koyere.antiafkplus.afk;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.utils.AFKLogger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

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
    private static final double CONFINED_SPACE_THRESHOLD = 5.0; // Maximum area for confined space
    private static final long PATTERN_ANALYSIS_INTERVAL = 30000; // 30 seconds between analyses
    private static final double REPETITIVE_MOVEMENT_THRESHOLD = 0.8; // Similarity threshold for repetitive patterns
    private static final int MAX_PATTERN_VIOLATIONS = 3; // Maximum violations before action

    // Player pattern tracking
    private final Map<UUID, PatternData> playerPatterns = new HashMap<>();
    private final Map<UUID, Integer> patternViolations = new HashMap<>();

    private BukkitTask analysisTask;

    public PatternDetector(AntiAFKPlus plugin, MovementListener movementListener, AFKManager afkManager) {
        this.plugin = plugin;
        this.movementListener = movementListener;
        this.afkManager = afkManager;
        startPatternAnalysis();
    }

    private void startPatternAnalysis() {
        this.analysisTask = new BukkitRunnable() {
            @Override
            public void run() {
                analyzeAllPlayerPatterns();
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 30, 20L * 10); // Start after 30s, run every 10s
    }

    private void analyzeAllPlayerPatterns() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission("antiafkplus.bypass")) continue;

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

        // Calculate center point of movement
        double centerX = history.stream().mapToDouble(pos -> pos.x).average().orElse(0);
        double centerZ = history.stream().mapToDouble(pos -> pos.z).average().orElse(0);

        // Check if all movements are within circle radius
        boolean allWithinRadius = history.stream().allMatch(pos -> {
            double distance = Math.sqrt(Math.pow(pos.x - centerX, 2) + Math.pow(pos.z - centerZ, 2));
            return distance <= WATER_CIRCLE_RADIUS;
        });

        if (!allWithinRadius) return false;

        // Check for circular movement pattern
        int circularMovements = 0;
        for (int i = 1; i < history.size(); i++) {
            MovementListener.LocationSnapshot prev = history.get(i - 1);
            MovementListener.LocationSnapshot curr = history.get(i);

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

        return circularMovements >= (history.size() * 0.6); // 60% of movements should be circular
    }

    private boolean detectConfinedSpacePattern(List<MovementListener.LocationSnapshot> history) {
        if (history.size() < MIN_SAMPLES_FOR_PATTERN) return false;

        // Calculate bounding box of movement
        double minX = history.stream().mapToDouble(pos -> pos.x).min().orElse(0);
        double maxX = history.stream().mapToDouble(pos -> pos.x).max().orElse(0);
        double minZ = history.stream().mapToDouble(pos -> pos.z).min().orElse(0);
        double maxZ = history.stream().mapToDouble(pos -> pos.z).max().orElse(0);

        double areaX = maxX - minX;
        double areaZ = maxZ - minZ;

        // Check if movement is confined to small area
        return areaX <= CONFINED_SPACE_THRESHOLD && areaZ <= CONFINED_SPACE_THRESHOLD;
    }

    private boolean detectRepetitivePattern(List<MovementListener.LocationSnapshot> history) {
        if (history.size() < 12) return false;

        // Split history into segments and compare similarity
        int segmentSize = history.size() / 3;
        List<MovementListener.LocationSnapshot> segment1 = history.subList(0, segmentSize);
        List<MovementListener.LocationSnapshot> segment2 = history.subList(segmentSize, segmentSize * 2);
        List<MovementListener.LocationSnapshot> segment3 = history.subList(segmentSize * 2, segmentSize * 3);

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

        int backAndForthCount = 0;

        for (int i = 2; i < history.size(); i++) {
            MovementListener.LocationSnapshot pos1 = history.get(i - 2);
            MovementListener.LocationSnapshot pos2 = history.get(i - 1);
            MovementListener.LocationSnapshot pos3 = history.get(i);

            // Check for back-and-forth movement (A -> B -> A pattern)
            double dist12 = calculateDistance(pos1, pos2);
            double dist23 = calculateDistance(pos2, pos3);
            double dist13 = calculateDistance(pos1, pos3);

            // If player moves away then back to nearly same position
            if (dist12 > 0.5 && dist23 > 0.5 && dist13 < 0.5) {
                backAndForthCount++;
            }
        }

        return backAndForthCount >= (history.size() * 0.3); // 30% of movements are back-and-forth
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
            // Force set player as AFK due to suspicious patterns
            afkManager.forceSetManualAFKState(player, true);

            // Send message to player
            player.sendMessage("§c[AntiAFK] Suspicious movement pattern detected. You have been marked as AFK.");

            // Log the action
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
        public long lastAnalysis = 0;
        public long firstDetection = 0;

        public int getTotalDetections() {
            return waterCircleDetections + confinedSpaceDetections +
                    repetitivePatternDetections + pendulumDetections;
        }

        public String getMostCommonPattern() {
            int max = Math.max(Math.max(waterCircleDetections, confinedSpaceDetections),
                    Math.max(repetitivePatternDetections, pendulumDetections));

            if (max == 0) return "none";
            if (max == waterCircleDetections) return "water_circle";
            if (max == confinedSpaceDetections) return "confined_space";
            if (max == repetitivePatternDetections) return "repetitive";
            return "pendulum";
        }
    }
}