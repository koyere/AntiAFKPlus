// AntiAFKActivityDetector.java - Enhanced v2.0 with Event Integration
package me.koyere.antiafkplus.afk;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.config.ConfigManager;
import me.koyere.antiafkplus.events.PlayerAFKPatternDetectedEvent;
import me.koyere.antiafkplus.events.PlayerAFKStateChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced Activity Detector v2.0 - Detects specific player activities like fishing, mining,
 * and other interactions while they might be AFK, and triggers a reassessment of their AFK status.
 * Now integrated with the v2.0 event system and pattern detection.
 */
public class AntiAFKActivityDetector implements Listener {

    private final AntiAFKPlus plugin;
    private final AFKManager afkManager;
    private final ConfigManager configManager;

    // Enhanced activity tracking maps
    private final Map<UUID, Long> lastBlockBreakTime = new HashMap<>();
    private final Map<UUID, Long> lastFishingActivityTime = new HashMap<>();
    private final Map<UUID, Long> lastInteractionTime = new HashMap<>();
    private final Map<UUID, ActivityPattern> playerActivityPatterns = new HashMap<>();

    // Pattern detection thresholds
    private static final long SUSPICIOUS_ACTIVITY_WINDOW = 60000; // 1 minute
    private static final int SUSPICIOUS_ACTIVITY_THRESHOLD = 30; // Actions per window
    private static final long PATTERN_RESET_INTERVAL = 300000; // 5 minutes

    private BukkitTask cleanupTask;
    private BukkitTask patternAnalysisTask;

    public AntiAFKActivityDetector(AntiAFKPlus plugin) {
        this.plugin = plugin;
        this.afkManager = plugin.getAfkManager();
        this.configManager = plugin.getConfigManager();

        startCleanupTask();
        startPatternAnalysisTask();
    }

    private void startCleanupTask() {
        this.cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                long cutoff = currentTime - PATTERN_RESET_INTERVAL;

                // Clean old activity records
                lastBlockBreakTime.entrySet().removeIf(entry -> entry.getValue() < cutoff);
                lastFishingActivityTime.entrySet().removeIf(entry -> entry.getValue() < cutoff);
                lastInteractionTime.entrySet().removeIf(entry -> entry.getValue() < cutoff);

                // Reset activity patterns periodically
                playerActivityPatterns.entrySet().removeIf(entry -> {
                    ActivityPattern pattern = entry.getValue();
                    if (currentTime - pattern.lastReset > PATTERN_RESET_INTERVAL) {
                        pattern.resetCounters();
                        pattern.lastReset = currentTime;
                    }
                    return pattern.isEmpty();
                });

                if (configManager.isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG_Cleanup] Activity detector cleanup completed. " +
                            "Tracking " + playerActivityPatterns.size() + " players.");
                }
            }
        }.runTaskTimerAsynchronously(plugin, 6000L, 6000L); // Every 5 minutes
    }

    private void startPatternAnalysisTask() {
        this.patternAnalysisTask = new BukkitRunnable() {
            @Override
            public void run() {
                analyzeActivityPatterns();
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 30, 20L * 15); // Start after 30s, run every 15s
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Only process if the player is currently considered AFK
        if (!afkManager.isAFK(player)) {
            return;
        }

        // Check if the broken block is solid to filter out less significant interactions
        Material type = event.getBlock().getType();
        if (type.isSolid()) {
            long currentTime = System.currentTimeMillis();
            lastBlockBreakTime.put(uuid, currentTime);

            // Update activity pattern
            ActivityPattern pattern = playerActivityPatterns.computeIfAbsent(uuid, k -> new ActivityPattern());
            pattern.blockBreaks++;
            pattern.lastActivity = currentTime;

            // Check for suspicious mining patterns
            if (detectSuspiciousMining(player, pattern)) {
                triggerPatternDetectionEvent(player, PlayerAFKPatternDetectedEvent.PatternType.MINING_PATTERN,
                        0.7, "Repetitive block breaking detected");
            } else {
                // Normal activity - trigger state change
                afkManager.onPlayerActivity(player);
            }

            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG_Activity] " + player.getName() +
                        " broke a solid block (" + type.name() + ") while AFK - AFK status reassessed.");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFishing(PlayerFishEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Only process if the player is currently considered AFK
        if (!afkManager.isAFK(player)) {
            return;
        }

        PlayerFishEvent.State state = event.getState();
        if (state == PlayerFishEvent.State.CAUGHT_FISH ||
                state == PlayerFishEvent.State.FISHING ||
                state == PlayerFishEvent.State.REEL_IN ||
                state == PlayerFishEvent.State.IN_GROUND) {

            long currentTime = System.currentTimeMillis();
            lastFishingActivityTime.put(uuid, currentTime);

            // Update activity pattern
            ActivityPattern pattern = playerActivityPatterns.computeIfAbsent(uuid, k -> new ActivityPattern());
            pattern.fishingActions++;
            pattern.lastActivity = currentTime;

            // Check for fishing exploit patterns
            if (detectSuspiciousFishing(player, pattern)) {
                triggerPatternDetectionEvent(player, PlayerAFKPatternDetectedEvent.PatternType.FISHING_EXPLOIT,
                        0.8, "Automated fishing behavior detected");
            } else {
                // Normal activity - trigger state change
                afkManager.onPlayerActivity(player);
            }

            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG_Activity] " + player.getName() +
                        " performed fishing action (" + state.name() + ") while AFK - AFK status reassessed.");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Only process if the player is currently considered AFK
        if (!afkManager.isAFK(player)) {
            return;
        }

        // Filter for significant interactions (right-click with items, block interactions)
        if (event.getAction().name().contains("RIGHT_CLICK") || event.getAction().name().contains("LEFT_CLICK")) {
            long currentTime = System.currentTimeMillis();
            lastInteractionTime.put(uuid, currentTime);

            // Update activity pattern
            ActivityPattern pattern = playerActivityPatterns.computeIfAbsent(uuid, k -> new ActivityPattern());
            pattern.interactions++;
            pattern.lastActivity = currentTime;

            // Normal interaction - trigger activity
            afkManager.onPlayerActivity(player);

            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG_Activity] " + player.getName() +
                        " performed interaction (" + event.getAction().name() + ") while AFK - AFK status reassessed.");
            }
        }
    }

    private void analyzeActivityPatterns() {
        long currentTime = System.currentTimeMillis();

        for (Map.Entry<UUID, ActivityPattern> entry : playerActivityPatterns.entrySet()) {
            UUID uuid = entry.getKey();
            ActivityPattern pattern = entry.getValue();
            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline()) {
                continue;
            }

            // Check for overall suspicious activity levels
            if (currentTime - pattern.lastActivity < SUSPICIOUS_ACTIVITY_WINDOW) {
                int totalActions = pattern.blockBreaks + pattern.fishingActions + pattern.interactions;

                if (totalActions > SUSPICIOUS_ACTIVITY_THRESHOLD) {
                    // Very high activity level - might be automated
                    triggerPatternDetectionEvent(player,
                            PlayerAFKPatternDetectedEvent.PatternType.COMBINED_PATTERNS,
                            0.9, "Extremely high activity level detected: " + totalActions + " actions/minute");
                }
            }
        }
    }

    private boolean detectSuspiciousMining(Player player, ActivityPattern pattern) {
        long currentTime = System.currentTimeMillis();

        // Check for rapid consecutive block breaks
        if (currentTime - pattern.lastActivity < 1000 && pattern.blockBreaks > 10) {
            return true;
        }

        // Check for consistent mining intervals (possible bot behavior)
        if (pattern.blockBreaks > 20) {
            // Additional analysis could be added here for timing patterns
            return pattern.blockBreaks > 50; // High threshold for now
        }

        return false;
    }

    private boolean detectSuspiciousFishing(Player player, ActivityPattern pattern) {
        long currentTime = System.currentTimeMillis();

        // Check for rapid fishing actions
        if (currentTime - pattern.lastActivity < 2000 && pattern.fishingActions > 5) {
            return true;
        }

        // Check for excessive fishing without breaks
        if (pattern.fishingActions > 30) {
            return true;
        }

        return false;
    }

    private void triggerPatternDetectionEvent(Player player, PlayerAFKPatternDetectedEvent.PatternType patternType,
                                              double confidence, String reason) {
        // Create additional data map
        Map<String, Object> additionalData = new HashMap<>();
        ActivityPattern pattern = playerActivityPatterns.get(player.getUniqueId());
        if (pattern != null) {
            additionalData.put("block_breaks", pattern.blockBreaks);
            additionalData.put("fishing_actions", pattern.fishingActions);
            additionalData.put("interactions", pattern.interactions);
            additionalData.put("detection_reason", reason);
        }

        // Create pattern data
        PatternDetector.PatternData patternData = new PatternDetector.PatternData();
        if (pattern != null) {
            patternData.waterCircleDetections = 0; // Activity detector doesn't track these
            patternData.confinedSpaceDetections = 0;
            patternData.repetitivePatternDetections = 1; // This is repetitive activity
            patternData.pendulumDetections = 0;
            patternData.lastAnalysis = System.currentTimeMillis();
        }

        // Fire the pattern detection event
        PlayerAFKPatternDetectedEvent event = new PlayerAFKPatternDetectedEvent(
                player,
                patternType,
                confidence,
                1, // violation count - could be tracked separately
                SUSPICIOUS_ACTIVITY_WINDOW,
                player.getLocation(),
                patternData,
                additionalData
        );

        // Call event synchronously on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getPluginManager().callEvent(event);

            // Handle the event result if not cancelled
            if (!event.isCancelled()) {
                handlePatternDetectionResult(player, event);
            }
        });
    }

    private void handlePatternDetectionResult(Player player, PlayerAFKPatternDetectedEvent event) {
        switch (event.getSuggestedAction()) {
            case FORCE_AFK:
                afkManager.forceSetManualAFKState(player, true);
                if (event.getCustomMessage() != null) {
                    player.sendMessage(event.getCustomMessage());
                }
                break;
            case WARN_PLAYER:
                player.sendMessage("§e[AntiAFK] Warning: " +
                        event.getPatternType().getDisplayName() + " detected.");
                break;
            case KICK_PLAYER:
                String kickMessage = event.getCustomMessage() != null ?
                        event.getCustomMessage() :
                        "§cKicked for suspicious AFK activity: " + event.getPatternType().getDisplayName();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.kickPlayer(kickMessage);
                    }
                });
                break;
            case TELEPORT_AWAY:
                // Could implement teleportation logic here
                break;
            case LOG_ONLY:
            default:
                // Already logged by the event system
                break;
        }
    }

    public void shutdown() {
        if (this.cleanupTask != null && !this.cleanupTask.isCancelled()) {
            this.cleanupTask.cancel();
            plugin.getLogger().info("AntiAFKActivityDetector cleanup task cancelled.");
        }

        if (this.patternAnalysisTask != null && !this.patternAnalysisTask.isCancelled()) {
            this.patternAnalysisTask.cancel();
            plugin.getLogger().info("AntiAFKActivityDetector pattern analysis task cancelled.");
        }

        // Clear all tracking data
        lastBlockBreakTime.clear();
        lastFishingActivityTime.clear();
        lastInteractionTime.clear();
        playerActivityPatterns.clear();
    }

    public void clearPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        lastBlockBreakTime.remove(uuid);
        lastFishingActivityTime.remove(uuid);
        lastInteractionTime.remove(uuid);
        playerActivityPatterns.remove(uuid);
    }

    // Inner class for tracking activity patterns
    private static class ActivityPattern {
        public int blockBreaks = 0;
        public int fishingActions = 0;
        public int interactions = 0;
        public long lastActivity = System.currentTimeMillis();
        public long lastReset = System.currentTimeMillis();

        public void resetCounters() {
            blockBreaks = 0;
            fishingActions = 0;
            interactions = 0;
        }

        public boolean isEmpty() {
            return blockBreaks == 0 && fishingActions == 0 && interactions == 0 &&
                    (System.currentTimeMillis() - lastActivity) > 600000; // 10 minutes
        }
    }
}