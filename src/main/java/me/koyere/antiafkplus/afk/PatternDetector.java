// PatternDetector.java - NEW v2.0 - Smart AFK Pool Detection
package me.koyere.antiafkplus.afk;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.events.PlayerAFKPatternDetectedEvent;
import me.koyere.antiafkplus.utils.AFKLogger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import me.koyere.antiafkplus.platform.PlatformScheduler;

import java.util.*;

/**
 * Detects suspicious movement patterns that indicate AFK machines or pools.
 * Analyzes player movement history to identify repetitive or confined movement
 * patterns.
 *
 * THREAD-SAFETY (v2.8):
 * - Pattern analysis runs asynchronously (runTaskTimerAsync) for performance
 * - Event firing and post-processing happens synchronously on main thread
 * (runTaskForEntity)
 * - All data structures use ConcurrentHashMap/ConcurrentLinkedDeque for
 * thread-safe access
 * - This ensures Paper 1.21.8 compatibility and prevents IllegalStateException
 */
public class PatternDetector {

    private final AntiAFKPlus plugin;
    private final MovementListener movementListener;
    private final AFKManager afkManager;

    // Default pattern detection configuration (overridden by config)
    private static final double DEFAULT_WATER_CIRCLE_RADIUS = 3.0;
    private static final int DEFAULT_MIN_SAMPLES_FOR_PATTERN = 20;
    private static final double DEFAULT_CONFINED_SPACE_THRESHOLD = 5.0;
    private static final long DEFAULT_PATTERN_ANALYSIS_INTERVAL_MS = 30000;
    private static final double DEFAULT_REPETITIVE_MOVEMENT_THRESHOLD = 0.8;
    private static final int DEFAULT_MAX_PATTERN_VIOLATIONS = 3;
    private static final double DEFAULT_LARGE_POOL_THRESHOLD = 25.0;
    private static final int DEFAULT_MIN_SAMPLES_FOR_LARGE_POOL = 30;
    private static final long MINIMUM_ANALYSIS_INTERVAL_TICKS = 20L; // 1s safety floor

    // Player pattern tracking (thread-safe for concurrent access)
    private final Map<UUID, PatternData> playerPatterns = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Integer> patternViolations = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Deque<DetectedPatternRecord>> recentDetections = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int MAX_STORED_PATTERN_DETECTIONS = 64;

    private PlatformScheduler.ScheduledTask analysisTask;

    // Configurable values (loaded from ConfigManager)
    private double waterCircleRadius;
    private int minSamplesForPattern;
    private double confinedSpaceThreshold;
    private long patternAnalysisIntervalMs;
    private double repetitiveMovementThreshold;
    private int maxPatternViolations;
    private double largePoolThreshold;
    private int minSamplesForLargePool;
    private boolean largePoolDetectionEnabled;
    private boolean keystrokeTimeoutDetectionEnabled;

    public PatternDetector(AntiAFKPlus plugin, MovementListener movementListener, AFKManager afkManager) {
        this.plugin = plugin;
        this.movementListener = movementListener;
        this.afkManager = afkManager;
        loadConfigValues();
        restartPatternAnalysis();
    }

    public void reloadFromConfig() {
        loadConfigValues();
        restartPatternAnalysis();
    }

    private void loadConfigValues() {
        if (plugin.getConfigManager() == null) {
            // Fallback to defaults if config manager is unavailable
            this.waterCircleRadius = DEFAULT_WATER_CIRCLE_RADIUS;
            this.minSamplesForPattern = DEFAULT_MIN_SAMPLES_FOR_PATTERN;
            this.confinedSpaceThreshold = DEFAULT_CONFINED_SPACE_THRESHOLD;
            this.patternAnalysisIntervalMs = DEFAULT_PATTERN_ANALYSIS_INTERVAL_MS;
            this.repetitiveMovementThreshold = DEFAULT_REPETITIVE_MOVEMENT_THRESHOLD;
            this.maxPatternViolations = DEFAULT_MAX_PATTERN_VIOLATIONS;
            this.largePoolThreshold = DEFAULT_LARGE_POOL_THRESHOLD;
            this.minSamplesForLargePool = DEFAULT_MIN_SAMPLES_FOR_LARGE_POOL;
            this.largePoolDetectionEnabled = true;
            this.keystrokeTimeoutDetectionEnabled = true;
            return;
        }

        this.waterCircleRadius = plugin.getConfigManager().getWaterCircleRadius();
        this.minSamplesForPattern = plugin.getConfigManager().getMinSamplesForPattern();
        this.confinedSpaceThreshold = plugin.getConfigManager().getConfinedSpaceThreshold();
        this.patternAnalysisIntervalMs = plugin.getConfigManager().getPatternAnalysisInterval();
        this.repetitiveMovementThreshold = plugin.getConfigManager().getRepetitiveMovementThreshold();
        this.maxPatternViolations = plugin.getConfigManager().getMaxPatternViolations();
        this.largePoolThreshold = plugin.getConfigManager().getLargePoolThreshold();
        this.minSamplesForLargePool = plugin.getConfigManager().getMinSamplesForLargePool();
        this.largePoolDetectionEnabled = plugin.getConfigManager().isLargePoolDetectionEnabled();
        this.keystrokeTimeoutDetectionEnabled = plugin.getConfigManager().isKeystrokeTimeoutDetectionEnabled();
    }

    private void restartPatternAnalysis() {
        if (this.analysisTask != null && !this.analysisTask.isCancelled()) {
            this.analysisTask.cancel();
        }
        if (!isPatternDetectionGloballyEnabled()) {
            return;
        }
        Runnable analysisBody = this::analyzeAllPlayerPatterns;
        long intervalTicks = Math.max(MINIMUM_ANALYSIS_INTERVAL_TICKS, patternAnalysisIntervalMs / 50L);
        this.analysisTask = plugin.getPlatformScheduler()
                .runTaskTimerAsync(analysisBody, intervalTicks, intervalTicks);
    }

    private boolean isPatternDetectionGloballyEnabled() {
        if (plugin.getConfigManager() == null) {
            return false;
        }
        return plugin.getConfigManager().isPatternDetectionModuleEnabled()
                && plugin.getConfigManager().isEnhancedDetectionEnabled()
                && plugin.getConfigManager().isPatternDetectionEnabled();
    }

    private void analyzeAllPlayerPatterns() {
        if (!isPatternDetectionGloballyEnabled()) {
            return;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission("antiafkplus.bypass"))
                continue;

            // PROFESSIONAL FIX: Verify if player is in disabled world (same logic as
            // AFKManager)
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
            if (locationData == null || locationData.locationHistory.size() < minSamplesForPattern) {
                continue;
            }

            analyzePlayerPattern(player, locationData);
        }
    }

    private void analyzePlayerPattern(Player player, MovementListener.PlayerLocationData locationData) {
        UUID uuid = player.getUniqueId();
        PatternData patternData = playerPatterns.computeIfAbsent(uuid, k -> new PatternData());

        List<MovementListener.LocationSnapshot> history = locationData.locationHistory;
        if (history.size() < minSamplesForPattern)
            return;

        // Get recent movement history (last 20 positions)
        List<MovementListener.LocationSnapshot> recentHistory = history.subList(
                Math.max(0, history.size() - minSamplesForPattern),
                history.size());

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
            // Reduce violation count if no suspicious activity (thread-safe decrement)
            patternViolations.computeIfPresent(uuid, (k, v) -> Math.max(0, v - 1));
        }

        patternData.lastAnalysis = System.currentTimeMillis();
    }

    private boolean detectWaterCirclePattern(List<MovementListener.LocationSnapshot> history) {
        if (history.size() < 8)
            return false;

        // PROFESSIONAL FIX: Create defensive copy to prevent
        // ConcurrentModificationException
        // The original list can be modified by other threads during stream operations
        List<MovementListener.LocationSnapshot> safeCopy = new java.util.ArrayList<>(history);

        // Calculate center point of movement using thread-safe copy
        double centerX = safeCopy.stream().mapToDouble(pos -> pos.x).average().orElse(0);
        double centerZ = safeCopy.stream().mapToDouble(pos -> pos.z).average().orElse(0);

        // Check if all movements are within circle radius
        boolean allWithinRadius = safeCopy.stream().allMatch(pos -> {
            double distance = Math.sqrt(Math.pow(pos.x - centerX, 2) + Math.pow(pos.z - centerZ, 2));
            return distance <= waterCircleRadius;
        });

        if (!allWithinRadius)
            return false;

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
            if (angleDiff > Math.PI)
                angleDiff = 2 * Math.PI - angleDiff;

            // If angle change is consistent (suggesting circular movement)
            if (angleDiff > 0.1 && angleDiff < Math.PI / 2) {
                circularMovements++;
            }
        }

        return circularMovements >= (safeCopy.size() * 0.6); // 60% of movements should be circular
    }

    private boolean detectConfinedSpacePattern(List<MovementListener.LocationSnapshot> history) {
        if (history.size() < minSamplesForPattern)
            return false;

        // PROFESSIONAL FIX: Create defensive copy to prevent
        // ConcurrentModificationException
        List<MovementListener.LocationSnapshot> safeCopy = new java.util.ArrayList<>(history);

        // Calculate bounding box of movement using thread-safe copy
        double minX = safeCopy.stream().mapToDouble(pos -> pos.x).min().orElse(0);
        double maxX = safeCopy.stream().mapToDouble(pos -> pos.x).max().orElse(0);
        double minZ = safeCopy.stream().mapToDouble(pos -> pos.z).min().orElse(0);
        double maxZ = safeCopy.stream().mapToDouble(pos -> pos.z).max().orElse(0);

        double areaX = maxX - minX;
        double areaZ = maxZ - minZ;

        // Check if movement is confined to small area
        return areaX <= confinedSpaceThreshold && areaZ <= confinedSpaceThreshold;
    }

    private boolean detectRepetitivePattern(List<MovementListener.LocationSnapshot> history) {
        if (history.size() < 12)
            return false;

        // PROFESSIONAL FIX: Create defensive copy to prevent
        // ConcurrentModificationException
        List<MovementListener.LocationSnapshot> safeCopy = new java.util.ArrayList<>(history);

        // Split history into segments and compare similarity using thread-safe copy
        int segmentSize = safeCopy.size() / 3;
        List<MovementListener.LocationSnapshot> segment1 = new java.util.ArrayList<>(safeCopy.subList(0, segmentSize));
        List<MovementListener.LocationSnapshot> segment2 = new java.util.ArrayList<>(
                safeCopy.subList(segmentSize, segmentSize * 2));
        List<MovementListener.LocationSnapshot> segment3 = new java.util.ArrayList<>(
                safeCopy.subList(segmentSize * 2, segmentSize * 3));

        double similarity12 = calculatePatternSimilarity(segment1, segment2);
        double similarity23 = calculatePatternSimilarity(segment2, segment3);
        double similarity13 = calculatePatternSimilarity(segment1, segment3);

        // If segments are highly similar, it's likely a repetitive pattern
        return (similarity12 > repetitiveMovementThreshold ||
                similarity23 > repetitiveMovementThreshold ||
                similarity13 > repetitiveMovementThreshold);
    }

    private boolean detectPendulumPattern(List<MovementListener.LocationSnapshot> history) {
        if (history.size() < 10)
            return false;

        // PROFESSIONAL FIX: Create defensive copy to prevent
        // ConcurrentModificationException
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
     * Detects large AFK pools (20x10+ blocks) that bypass traditional confined
     * space detection.
     * This method identifies pools that are too large for the standard confined
     * space threshold
     * but still represent artificial AFK setups.
     * 
     * Detection criteria:
     * 1. Movement area larger than small pool threshold but smaller than large pool
     * threshold
     * 2. Player is in water for extended periods
     * 3. Movement patterns suggest water current automation
     * 4. No manual keystrokes detected for extended periods
     * 
     * @param player  The player being analyzed
     * @param history Recent movement history
     * @return true if large AFK pool pattern detected
     */
    private boolean detectLargeAFKPool(Player player, List<MovementListener.LocationSnapshot> history) {
        if (!largePoolDetectionEnabled) {
            return false;
        }
        if (history.size() < minSamplesForLargePool)
            return false;

        // PROFESSIONAL FIX: Create defensive copy to prevent
        // ConcurrentModificationException
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
        boolean isLargePoolSize = (areaX > confinedSpaceThreshold || areaZ > confinedSpaceThreshold) &&
                (totalArea <= largePoolThreshold * largePoolThreshold);

        if (!isLargePoolSize)
            return false;

        // Check if player has been in water for most of the tracking period
        boolean mostlyInWater = isPlayerMostlyInWater(player, safeCopy);
        if (!mostlyInWater)
            return false;

        // Check for automatic movement patterns (consistent velocity, minimal direction
        // changes)
        boolean hasAutomaticMovement = detectAutomaticMovementPattern(safeCopy);
        if (!hasAutomaticMovement)
            return false;

        // Final check: has player exceeded keystroke timeout?
        boolean hasKeystrokeTimeout = movementListener.hasKeystrokeTimeout(player);

        // Debug logging
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info(String.format(
                    "[DEBUG_LargePool] %s: area=%.1fx%.1f (%.1f), inWater=%s, autoMove=%s, keystrokeTimeout=%s",
                    player.getName(), areaX, areaZ, totalArea, mostlyInWater, hasAutomaticMovement,
                    hasKeystrokeTimeout));
        }

        return hasKeystrokeTimeout; // Only trigger if keystroke timeout is also present
    }

    /**
     * Checks if player has been mostly in water during the tracking period.
     * This helps identify AFK pool setups vs regular gameplay.
     * 
     * @param player  The player to check
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
        if (history.size() < 10)
            return false;

        // PROFESSIONAL FIX: Create defensive copy to prevent
        // ConcurrentModificationException
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
                    if (angleDiff > Math.PI)
                        angleDiff = 2 * Math.PI - angleDiff;

                    totalDirectionChanges += angleDiff;
                }

                validMeasurements++;
            }
        }

        if (validMeasurements < 5)
            return false;

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
        if (!keystrokeTimeoutDetectionEnabled) {
            return false;
        }
        return movementListener.hasKeystrokeTimeout(player);
    }

    private double calculatePatternSimilarity(List<MovementListener.LocationSnapshot> pattern1,
            List<MovementListener.LocationSnapshot> pattern2) {
        if (pattern1.size() != pattern2.size())
            return 0.0;

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
        final String playerName = player.getName(); // Cache name (final for lambda capture)

        // Thread-safe increment of violations (can be called from async thread)
        final int violations = patternViolations.compute(uuid, (k, v) -> (v == null ? 0 : v) + 1);

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().warning("[PatternDetector] Suspicious " + detectionReason +
                    " pattern detected for " + playerName +
                    " (Violations: " + violations + "/" + maxPatternViolations + ")");
        }

        AFKLogger.logActivity("Pattern Detection: " + playerName + " - " + detectionReason +
                " (violations: " + violations + ")");

        // Prepare event data (can be done in async thread - NO entity access here)
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("reason", detectionReason);
        additionalData.put("violations", violations);
        additionalData.put("totalDetections", patternData.getTotalDetections());

        PlayerAFKPatternDetectedEvent.PatternType patternType = mapPatternType(detectionReason);
        double confidence = computeConfidence(detectionReason, violations);

        // CRITICAL FIX: Execute ALL event firing and post-processing synchronously on
        // main thread
        // Paper 1.21.8 requires events to be fired from main thread, not async
        // IMPORTANT: ALL player entity access must happen inside runTaskForEntity
        // (including getLocation)
        plugin.getPlatformScheduler().runTaskForEntity(player, () -> {
            // Re-verify player is still online (they might have disconnected during async
            // analysis)
            if (!player.isOnline()) {
                return;
            }

            // Get player location NOW in main thread (safe entity access)
            Location detectionLocation = player.getLocation().clone();

            // Create and fire event synchronously
            PlayerAFKPatternDetectedEvent patternEvent = new PlayerAFKPatternDetectedEvent(
                    player,
                    patternType,
                    confidence,
                    violations,
                    patternAnalysisIntervalMs,
                    detectionLocation,
                    patternData,
                    additionalData);

            plugin.getServer().getPluginManager().callEvent(patternEvent);

            // Record detection (thread-safe map operation)
            recordDetectedPattern(player, patternEvent);

            // Check if event was cancelled by listeners
            if (patternEvent.isCancelled()) {
                return;
            }

            // v2.9.4 NEW: Send notifications to admins if configured
            if (plugin.getConfigManager().shouldSendPatternAlertsToAdmins()) {
                sendAdminNotification(player, detectionReason, violations, confidence);
            }

            // v2.9.4 NEW: Notify player on detection if configured
            if (plugin.getConfigManager().shouldNotifyPlayerOnPatternDetection()) {
                String message = patternEvent.getCustomMessage() != null ? patternEvent.getCustomMessage()
                        : "§c[AntiAFK] Suspicious movement pattern detected: " + detectionReason;
                player.sendMessage(message);
            }

            // Process suggested action from event
            if (processSuggestedAction(player, patternEvent)) {
                patternViolations.put(uuid, 0);
                return;
            }

            // Take action based on violation count
            if (violations >= maxPatternViolations) {
                // v2.9.4 NEW: Notify player on violation if configured
                if (plugin.getConfigManager().shouldNotifyPlayerOnViolation()) {
                    String violationMessage = patternEvent.getCustomMessage() != null ? patternEvent.getCustomMessage()
                            : "§c[AntiAFK] Maximum pattern violations reached (" + violations + "/"
                                    + maxPatternViolations + ")";
                    player.sendMessage(violationMessage);
                }

                // PROFESSIONAL FIX: Execute configured AFK action (kick/teleport) instead of
                // just marking AFK
                // This ensures pattern detection triggers the same action as regular AFK
                // detection
                afkManager.executeAFKAction(player, detectionReason);

                // v2.9.4 NEW: Notify player on action if configured
                if (plugin.getConfigManager().shouldNotifyPlayerOnAction()) {
                    String actionMessage = patternEvent.getCustomMessage() != null ? patternEvent.getCustomMessage()
                            : "§c[AntiAFK] Suspicious movement pattern detected. AFK action executed.";
                    player.sendMessage(actionMessage);
                }

                // Log the action
                AFKLogger.logActivity("Forced AFK due to pattern violations: " + playerName +
                        " (" + detectionReason + ")");

                // Reset violations after action (thread-safe)
                patternViolations.put(uuid, 0);
            }
        });
    }

    /**
     * v2.9.4 NEW: Sends pattern detection notifications to online admins with the
     * required permission.
     * This allows staff to monitor suspicious activity without spamming regular
     * players.
     * 
     * @param player          The player who triggered the pattern detection
     * @param detectionReason The type of pattern detected
     * @param violations      Current violation count
     * @param confidence      Detection confidence level (0.0 to 1.0)
     */
    private void sendAdminNotification(Player player, String detectionReason, int violations, double confidence) {
        String permission = plugin.getConfigManager().getAdminNotificationPermission();
        if (permission == null || permission.trim().isEmpty()) {
            return; // No permission configured, skip admin notifications
        }

        String adminMessage = String.format(
                "§6[AntiAFK Admin] §e%s §7detected for §e%s §7(Violations: §c%d§7/§c%d§7, Confidence: §a%.0f%%§7)",
                detectionReason.replace("_", " "),
                player.getName(),
                violations,
                maxPatternViolations,
                confidence * 100);

        // Send to all online players with the admin notification permission
        plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> p.hasPermission(permission))
                .forEach(admin -> admin.sendMessage(adminMessage));
    }

    public void clearPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        playerPatterns.remove(uuid);
        patternViolations.remove(uuid);
        recentDetections.remove(uuid);
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
        recentDetections.clear();
    }

    public List<DetectedPatternRecord> getRecentDetections(Player player) {
        if (player == null) {
            return Collections.emptyList();
        }
        Deque<DetectedPatternRecord> deque = recentDetections.get(player.getUniqueId());
        if (deque == null || deque.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(deque);
    }

    private boolean processSuggestedAction(Player player, PlayerAFKPatternDetectedEvent event) {
        PlayerAFKPatternDetectedEvent.PatternAction action = event.getSuggestedAction();
        if (action == null) {
            return false;
        }

        String defaultMessage = "§c[AntiAFK] Suspicious pattern detected. Please remain active.";
        switch (action) {
            case LOG_ONLY:
                return false;
            case NO_ACTION:
                return true;
            case WARN_PLAYER:
                player.sendMessage(event.getCustomMessage() != null ? event.getCustomMessage() : defaultMessage);
                return true;
            case FORCE_AFK:
                afkManager.forceSetAutoAFKState(player, true, "pattern_" + event.getPatternType().name().toLowerCase());
                return true;
            case KICK_PLAYER:
                if (player.isOnline()) {
                    player.kickPlayer(event.getCustomMessage() != null ? event.getCustomMessage() : defaultMessage);
                }
                return true;
            case TELEPORT_AWAY:
                afkManager.executeAFKAction(player, "pattern_" + event.getPatternType().name().toLowerCase());
                return true;
            case FREEZE_PLAYER:
                player.sendMessage(event.getCustomMessage() != null ? event.getCustomMessage() : defaultMessage);
                return true;
            case CUSTOM_COMMAND:
                if (event.getCustomMessage() != null && !event.getCustomMessage().isEmpty()) {
                    String command = event.getCustomMessage().replace("{player}", player.getName());
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
                    return true;
                }
                return false;
            case NOTIFY_STAFF:
                String notify = event.getCustomMessage() != null ? event.getCustomMessage()
                        : ("[AntiAFK] " + player.getName() + " flagged for pattern " + event.getPatternType().name());
                plugin.getServer().getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("antiafkplus.admin"))
                        .forEach(p -> p.sendMessage(notify));
                return true;
            default:
                return false;
        }
    }

    private void recordDetectedPattern(Player player, PlayerAFKPatternDetectedEvent event) {
        if (player == null || event == null) {
            return;
        }
        UUID uuid = player.getUniqueId();

        // Thread-safe: Use ConcurrentLinkedDeque for thread-safe operations
        Deque<DetectedPatternRecord> detections = recentDetections.computeIfAbsent(
                uuid, k -> new java.util.concurrent.ConcurrentLinkedDeque<>());

        detections.addLast(new DetectedPatternRecord(
                event.getPatternType(),
                event.getConfidence(),
                System.currentTimeMillis(),
                event.getViolationCount(),
                event.getDetectionTimespan(),
                event.getDetectionLocation(),
                event.getAdditionalData(),
                event.isCancelled()));

        // Remove old detections if exceeding limit
        while (detections.size() > MAX_STORED_PATTERN_DETECTIONS) {
            detections.removeFirst();
        }
    }

    private PlayerAFKPatternDetectedEvent.PatternType mapPatternType(String detectionReason) {
        if (detectionReason == null) {
            return PlayerAFKPatternDetectedEvent.PatternType.CUSTOM;
        }
        return switch (detectionReason.toLowerCase()) {
            case "water_circle" -> PlayerAFKPatternDetectedEvent.PatternType.WATER_CIRCLE;
            case "confined_space" -> PlayerAFKPatternDetectedEvent.PatternType.CONFINED_SPACE;
            case "repetitive", "repetitive_pattern" -> PlayerAFKPatternDetectedEvent.PatternType.REPETITIVE_MOVEMENT;
            case "pendulum" -> PlayerAFKPatternDetectedEvent.PatternType.PENDULUM_MOVEMENT;
            case "large_afk_pool" -> PlayerAFKPatternDetectedEvent.PatternType.COMBINED_PATTERNS;
            case "keystroke_timeout" -> PlayerAFKPatternDetectedEvent.PatternType.AUTOCLICK_PATTERN;
            case "mining_pattern" -> PlayerAFKPatternDetectedEvent.PatternType.MINING_PATTERN;
            default -> PlayerAFKPatternDetectedEvent.PatternType.CUSTOM;
        };
    }

    private double computeConfidence(String detectionReason, int violations) {
        double base;
        if (detectionReason == null) {
            base = 0.6;
        } else {
            switch (detectionReason.toLowerCase()) {
                case "water_circle":
                    base = 0.85;
                    break;
                case "confined_space":
                    base = 0.75;
                    break;
                case "repetitive":
                case "repetitive_pattern":
                    base = 0.7;
                    break;
                case "pendulum":
                    base = 0.65;
                    break;
                case "large_afk_pool":
                    base = 0.8;
                    break;
                case "keystroke_timeout":
                    base = 0.7;
                    break;
                case "mining_pattern":
                    base = 0.8;
                    break;
                default:
                    base = 0.6;
            }
        }
        return Math.min(1.0, base + Math.max(0, violations - 1) * 0.05);
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

            if (max == 0)
                return "none";
            if (max == waterCircleDetections)
                return "water_circle";
            if (max == confinedSpaceDetections)
                return "confined_space";
            if (max == repetitivePatternDetections)
                return "repetitive";
            if (max == pendulumDetections)
                return "pendulum";
            if (max == largePoolDetections)
                return "large_afk_pool"; // v2.4 NEW
            if (max == keystrokeTimeouts)
                return "keystroke_timeout"; // v2.4 NEW
            return "unknown";
        }
    }

    public static class DetectedPatternRecord {
        private final PlayerAFKPatternDetectedEvent.PatternType patternType;
        private final double confidence;
        private final long timestamp;
        private final int violationCount;
        private final long detectionTimespan;
        private final Location location;
        private final Map<String, Object> additionalData;
        private final boolean cancelled;

        public DetectedPatternRecord(PlayerAFKPatternDetectedEvent.PatternType patternType,
                double confidence,
                long timestamp,
                int violationCount,
                long detectionTimespan,
                Location location,
                Map<String, Object> additionalData,
                boolean cancelled) {
            this.patternType = patternType;
            this.confidence = confidence;
            this.timestamp = timestamp;
            this.violationCount = violationCount;
            this.detectionTimespan = detectionTimespan;
            this.location = location != null ? location.clone() : null;
            this.additionalData = additionalData != null ? new HashMap<>(additionalData) : Collections.emptyMap();
            this.cancelled = cancelled;
        }

        public PlayerAFKPatternDetectedEvent.PatternType getPatternType() {
            return patternType;
        }

        public double getConfidence() {
            return confidence;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public int getViolationCount() {
            return violationCount;
        }

        public long getDetectionTimespan() {
            return detectionTimespan;
        }

        public Location getLocation() {
            return location != null ? location.clone() : null;
        }

        public Map<String, Object> getAdditionalData() {
            return Collections.unmodifiableMap(additionalData);
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }
}
