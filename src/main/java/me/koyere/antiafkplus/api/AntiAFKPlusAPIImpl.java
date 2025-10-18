package me.koyere.antiafkplus.api;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.api.data.*;
import me.koyere.antiafkplus.api.events.*;
import me.koyere.antiafkplus.api.exceptions.AFKException;
import me.koyere.antiafkplus.afk.AFKManager;
import me.koyere.antiafkplus.afk.MovementListener;
import me.koyere.antiafkplus.afk.PatternDetector;
import me.koyere.antiafkplus.afk.PatternDetector.DetectedPatternRecord;
import me.koyere.antiafkplus.afk.PatternDetector.PatternData;
import me.koyere.antiafkplus.events.PlayerAFKPatternDetectedEvent;
import me.koyere.antiafkplus.events.PlayerAFKStateChangeEvent;
import me.koyere.antiafkplus.events.PlayerAFKWarningEvent;
import me.koyere.antiafkplus.compatibility.BedrockCompatibility;
import me.koyere.antiafkplus.i18n.LocalizationManager;
import me.koyere.antiafkplus.modules.ModuleManager;
import me.koyere.antiafkplus.performance.PerformanceOptimizer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Complete implementation of the AntiAFKPlus Enterprise API.
 * 
 * This implementation provides full functionality for all API methods,
 * with proper error handling, performance optimization, and thread safety.
 */
public class AntiAFKPlusAPIImpl implements AntiAFKPlusAPI {
    
    private final AntiAFKPlus plugin;
    
    // Event handling
    private final Map<String, Set<EventRegistration>> eventListeners = new ConcurrentHashMap<>();
    private final Map<UUID, AFKExemption> playerExemptions = new ConcurrentHashMap<>();
    private final Map<UUID, Duration> customTimeouts = new ConcurrentHashMap<>();
    private final AFKManager afkManager;
    private final MovementListener movementListener;
    private final PatternDetector patternDetector;
    private final Map<UUID, Deque<ActivityRecord>> activityHistory = new ConcurrentHashMap<>();
    private final Map<UUID, AFKSessionTracker> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, List<AFKHistoryData.AFKSession>> sessionHistory = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerAFKSummary> playerSummaries = new ConcurrentHashMap<>();
    private final Map<UUID, List<AFKWarningEvent>> warningHistory = new ConcurrentHashMap<>();

    private static final int MAX_ACTIVITY_RECORDS = 512;
    private static final int MAX_SESSIONS_STORED = 64;
    private static final Duration ACTIVITY_WINDOW = Duration.ofMinutes(5);
    private static final Duration ACTIVITY_HISTORY_WINDOW = Duration.ofMinutes(30);
    
    public AntiAFKPlusAPIImpl(AntiAFKPlus plugin) {
        this.plugin = plugin;
        this.afkManager = plugin.getAfkManager();
        this.movementListener = plugin.getMovementListener();
        this.patternDetector = afkManager != null ? afkManager.getPatternDetector() : null;
    }
    
    // ============= BASIC AFK STATUS =============
    
    @Override
    public boolean isAFK(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        
        if (!player.isOnline()) {
            return false;
        }
        
        return plugin.getPerformanceOptimizer().executeWithMonitoring("api-isAFK", () ->
                afkManager != null && afkManager.isAFK(player)
        );
    }
    
    @Override
    public boolean isAFK(UUID playerUUID) {
        Player player = Bukkit.getPlayer(playerUUID);
        return player != null && isAFK(player);
    }
    
    @Override
    public AFKStatus getAFKStatus(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        
        if (!player.isOnline()) {
            return AFKStatus.UNKNOWN;
        }
        
        return plugin.getPerformanceOptimizer().executeWithMonitoring("api-getAFKStatus", () -> {
            if (isExempt(player)) {
                return AFKStatus.EXEMPT;
            }
            if (afkManager == null) {
                return AFKStatus.UNKNOWN;
            }
            if (afkManager.isManuallyAFK(player)) {
                return AFKStatus.MANUAL_AFK;
            }
            if (afkManager.isAutoAFK(player)) {
                return AFKStatus.AUTO_AFK;
            }
            return AFKStatus.ACTIVE;
        });
    }
    
    @Override
    public boolean isManuallyAFK(Player player) {
        AFKStatus status = getAFKStatus(player);
        return status == AFKStatus.MANUAL_AFK;
    }
    
    @Override
    public boolean isAutomaticallyAFK(Player player) {
        AFKStatus status = getAFKStatus(player);
        return status == AFKStatus.AUTO_AFK;
    }
    
    // ============= AFK MANAGEMENT =============
    
    @Override
    public void setAFKStatus(Player player, AFKStatus afkStatus, String reason) throws AFKException {
        if (player == null) {
            throw new AFKException(AFKException.AFKErrorCode.PLAYER_NOT_FOUND, "Player cannot be null");
        }
        
        if (!player.isOnline()) {
            throw new AFKException(AFKException.AFKErrorCode.PLAYER_OFFLINE, "Player is not online");
        }
        
        if (afkStatus == null) {
            throw new AFKException(AFKException.AFKErrorCode.INVALID_STATUS, "AFK status cannot be null");
        }
        
        if (afkManager == null) {
            throw new AFKException(AFKException.AFKErrorCode.INTERNAL_ERROR, "AFK manager is not initialized");
        }

        plugin.getPerformanceOptimizer().executeWithMonitoring("api-setAFKStatus", () -> {
            AFKStatus target = afkStatus != null ? afkStatus : AFKStatus.ACTIVE;
            String apiReason = reason != null ? reason : "API call";

            switch (target) {
                case MANUAL_AFK -> afkManager.forceSetManualAFKState(player, true);
                case AUTO_AFK -> afkManager.forceSetAutoAFKState(player, true, apiReason);
                case ACTIVE -> afkManager.forceSetActive(player, PlayerAFKStateChangeEvent.AFKReason.API_CALL, apiReason);
                case EXEMPT -> addExemption(player, apiReason, null);
                case UNKNOWN -> {
                    // No state changes for UNKNOWN
                }
            }
            return null;
        });
    }
    
    @Override
    public AFKStatus toggleAFK(Player player) {
        if (player == null) {
            return AFKStatus.UNKNOWN;
        }
        if (afkManager == null) {
            return AFKStatus.UNKNOWN;
        }

        boolean nowManual = afkManager.toggleManualAFK(player);
        return nowManual ? AFKStatus.MANUAL_AFK : getAFKStatus(player);
    }
    
    @Override
    public void forceAFK(Player player, String reason, Duration duration) {
        if (player == null) {
            return;
        }
        if (afkManager == null) {
            plugin.getLogger().warning("AFK manager not ready; cannot force AFK for " + player.getName());
            return;
        }

        String apiReason = reason != null ? reason : "Forced by API";
        afkManager.forceSetManualAFKState(player, true);

        if (duration != null) {
            long ticks = Math.max(1L, duration.toMillis() / 50L);
            plugin.getPlatformScheduler().runTaskLater(() -> {
                if (player.isOnline() && afkManager.isManuallyAFK(player)) {
                    afkManager.forceSetActive(player, PlayerAFKStateChangeEvent.AFKReason.API_CALL, "duration_expired");
                }
            }, ticks);
        }
    }
    
    @Override
    public void removeAFK(Player player, String reason) {
        if (player == null) {
            return;
        }
        if (afkManager == null) {
            plugin.getLogger().warning("AFK manager not ready; cannot remove AFK for " + player.getName());
            return;
        }
        afkManager.forceSetActive(player, PlayerAFKStateChangeEvent.AFKReason.API_CALL,
                reason != null ? reason : "api_call");
    }
    
    // ============= ACTIVITY TRACKING =============
    
    @Override
    public Duration getTimeSinceLastActivity(Player player) {
        if (player == null || !player.isOnline()) {
            return Duration.ZERO;
        }
        
        return plugin.getPerformanceOptimizer().executeWithMonitoring("api-getTimeSinceLastActivity", () -> {
            if (afkManager == null) {
                return Duration.ZERO;
            }
            AFKManager.PlayerActivityData data = afkManager.getPlayerActivityData(player);
            if (data == null) {
                return Duration.ZERO;
            }
            long last = data.getLastActivityTimestamp();
            if (last <= 0) {
                return Duration.ZERO;
            }
            long diff = Math.max(0L, System.currentTimeMillis() - last);
            return Duration.ofMillis(diff);
        });
    }
    
    @Override
    public PlayerActivityInfo getActivityInfo(Player player) {
        if (player == null || !player.isOnline()) {
            return null;
        }
        
        return plugin.getPerformanceOptimizer().executeWithMonitoring("api-getActivityInfo", () -> {
            if (afkManager == null) {
                return null;
            }

            AFKManager.PlayerActivityData data = afkManager.getPlayerActivityData(player);
            if (data == null) {
                return null;
            }

            long now = System.currentTimeMillis();
            long lastTs = data.getLastActivityTimestamp();
            Instant lastInstant = lastTs > 0 ? Instant.ofEpochMilli(lastTs) : Instant.ofEpochMilli(now);
            Duration sinceLast = lastTs > 0 ? Duration.ofMillis(Math.max(0L, now - lastTs)) : Duration.ZERO;

            long windowMs = Duration.ofMinutes(5).toMillis();
            Map<ActivityType, Integer> rawCounts = data.getActivityCounts(windowMs);
            EnumMap<ActivityType, Integer> counts = new EnumMap<>(ActivityType.class);
            counts.putAll(rawCounts);

            EnumMap<ActivityType, Instant> lastTimes = new EnumMap<>(ActivityType.class);
            data.getLastActivityTimes().forEach((type, timestamp) -> {
                if (timestamp != null && timestamp > 0) {
                    lastTimes.put(type, Instant.ofEpochMilli(timestamp));
                }
            });

            int recentMovements = counts.getOrDefault(ActivityType.MOVEMENT, 0);
            int recentHeadRotations = counts.getOrDefault(ActivityType.HEAD_ROTATION, 0);
            int recentJumps = counts.getOrDefault(ActivityType.JUMP, 0);
            int recentCommands = counts.getOrDefault(ActivityType.COMMAND, 0);
            int recentInteractions =
                    counts.getOrDefault(ActivityType.INTERACTION, 0) +
                    counts.getOrDefault(ActivityType.BLOCK_BREAK, 0) +
                    counts.getOrDefault(ActivityType.BLOCK_PLACE, 0) +
                    counts.getOrDefault(ActivityType.ITEM_USE, 0) +
                    counts.getOrDefault(ActivityType.INVENTORY, 0) +
                    counts.getOrDefault(ActivityType.COMBAT, 0);

            int totalRecent = counts.values().stream().mapToInt(Integer::intValue).sum();
            double windowMinutes = windowMs / 60000.0;
            double averageRate = windowMinutes > 0 ? totalRecent / windowMinutes : 0.0;

            return new PlayerActivityInfo(
                player.getUniqueId(),
                player.getName(),
                lastInstant,
                data.getLastActivityType(),
                sinceLast,
                data.getActivityScore(),
                recentMovements,
                recentHeadRotations,
                recentJumps,
                recentCommands,
                recentInteractions,
                Collections.unmodifiableMap(counts),
                Collections.unmodifiableMap(lastTimes),
                data.isHighActivity(),
                data.isLowActivity(),
                averageRate
            );
        });
    }
    
    @Override
    public ActivityType getLastActivityType(Player player) {
        if (afkManager == null) {
            return ActivityType.UNKNOWN;
        }
        AFKManager.PlayerActivityData data = afkManager.getPlayerActivityData(player);
        return data != null ? data.getLastActivityType() : ActivityType.UNKNOWN;
    }
    
    @Override
    public void recordActivity(Player player, ActivityType activityType, Map<String, Object> data) {
        if (player == null || activityType == null) {
            return;
        }

        plugin.getPerformanceOptimizer().executeWithMonitoring("api-recordActivity", () -> {
            if (afkManager != null) {
                afkManager.recordCustomActivity(player, activityType);
            }
            plugin.getPerformanceOptimizer().updatePlayerActivity(player);
        });
    }
    
    @Override
    public ActivityStatistics getActivityStatistics(Player player, Duration period) {
        if (player == null || period == null) {
            return null;
        }
        
        return plugin.getPerformanceOptimizer().executeWithMonitoring("api-getActivityStatistics", () -> {
            if (afkManager == null) {
                return new ActivityStatistics(
                        player.getUniqueId(),
                        period,
                        Collections.emptyMap(),
                        0.0,
                        0.0,
                        ActivityType.UNKNOWN,
                        0
                );
            }

            AFKManager.PlayerActivityData data = afkManager.getPlayerActivityData(player);
            if (data == null) {
                return new ActivityStatistics(
                        player.getUniqueId(),
                        period,
                        Collections.emptyMap(),
                        0.0,
                        0.0,
                        ActivityType.UNKNOWN,
                        0
                );
            }

            long windowMs = Math.max(0L, period.toMillis());
            Map<ActivityType, Integer> rawCounts = data.getActivityCounts(windowMs);
            EnumMap<ActivityType, Integer> counts = new EnumMap<>(ActivityType.class);
            counts.putAll(rawCounts);

            int totalActivities = counts.values().stream().mapToInt(Integer::intValue).sum();
            double totalScore = counts.entrySet().stream()
                    .mapToDouble(entry -> entry.getKey().getActivityWeight() * entry.getValue())
                    .sum();
            double averageRate = windowMs > 0 ? totalActivities / (windowMs / 60000.0) : 0.0;

            ActivityType mostCommon = ActivityType.UNKNOWN;
            int max = 0;
            for (Map.Entry<ActivityType, Integer> entry : counts.entrySet()) {
                if (entry.getValue() > max) {
                    max = entry.getValue();
                    mostCommon = entry.getKey();
                }
            }

            return new ActivityStatistics(
                player.getUniqueId(),
                period,
                Collections.unmodifiableMap(counts),
                totalScore,
                averageRate,
                mostCommon,
                totalActivities
            );
        });
    }
    
    // ============= PATTERN DETECTION =============
    
    @Override
    public boolean hasSuspiciousPatterns(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        return plugin.getPerformanceOptimizer().executeWithMonitoring("api-hasSuspiciousPatterns", () -> {
            if (patternDetector == null) {
                return false;
            }
            PatternData data = patternDetector.getPlayerPatternData(player);
            if (data != null && data.getTotalDetections() > 0) {
                return true;
            }
            return !patternDetector.getRecentDetections(player).isEmpty();
        });
    }
    
    @Override
    public List<DetectedPattern> getDetectedPatterns(Player player) {
        if (player == null || !player.isOnline()) {
            return Collections.emptyList();
        }

        return plugin.getPerformanceOptimizer().executeWithMonitoring("api-getDetectedPatterns", () -> {
            if (patternDetector == null) {
                return Collections.emptyList();
            }
            List<DetectedPatternRecord> records = patternDetector.getRecentDetections(player);
            if (records.isEmpty()) {
                return Collections.emptyList();
            }
            List<DetectedPattern> converted = new ArrayList<>(records.size());
            for (DetectedPatternRecord record : records) {
                Map<String, Object> extras = new HashMap<>(record.getAdditionalData());
                extras.put("violationCount", record.getViolationCount());
                extras.put("detectionTimespanMs", record.getDetectionTimespan());
                extras.put("cancelled", record.isCancelled());
                if (record.getLocation() != null) {
                    var loc = record.getLocation();
                    extras.put("location", loc.getWorld().getName() + ":" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
                }
                converted.add(new DetectedPattern(
                        record.getPatternType().name(),
                        record.getConfidence(),
                        Instant.ofEpochMilli(record.getTimestamp()),
                        extras
                ));
            }
            return Collections.unmodifiableList(converted);
        });
    }
    
    @Override
    public double getPatternConfidence(Player player) {
        List<DetectedPattern> patterns = getDetectedPatterns(player);
        if (patterns.isEmpty()) {
            return 0.0;
        }
        
        return patterns.stream()
                .mapToDouble(DetectedPattern::getConfidence)
                .average()
                .orElse(0.0);
    }
    
    @Override
    public void clearPatternData(Player player) {
        if (player == null) {
            return;
        }
        
        plugin.getPerformanceOptimizer().executeWithMonitoring("api-clearPatternData", () -> {
            if (patternDetector != null) {
                patternDetector.clearPlayerData(player);
            }
        });
    }
    
    // ============= EXEMPTIONS AND BYPASS =============
    
    @Override
    public boolean isExempt(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }
        
        // Check permission-based exemption
        if (player.hasPermission("antiafkplus.bypass")) {
            return true;
        }
        
        // Check API-based exemptions
        AFKExemption exemption = playerExemptions.get(player.getUniqueId());
        return exemption != null && !exemption.isExpired();
    }
    
    @Override
    public void addExemption(Player player, String reason, Duration duration) {
        if (player == null || reason == null) {
            return;
        }
        
        boolean permanent = duration == null;
        AFKExemption exemption = new AFKExemption(reason, Instant.now(), duration, permanent);
        playerExemptions.put(player.getUniqueId(), exemption);
        
        plugin.debug("Added exemption for " + player.getName() + ": " + reason + 
                    (permanent ? " (permanent)" : " (duration: " + duration + ")"));
    }
    
    @Override
    public void removeExemption(Player player, String reason) {
        if (player == null) {
            return;
        }
        
        playerExemptions.remove(player.getUniqueId());
        plugin.debug("Removed exemption for " + player.getName() + 
                    (reason != null ? " (reason: " + reason + ")" : ""));
    }
    
    @Override
    public List<AFKExemption> getExemptions(Player player) {
        if (player == null) {
            return Collections.emptyList();
        }
        
        AFKExemption exemption = playerExemptions.get(player.getUniqueId());
        return exemption != null && !exemption.isExpired() ? 
               Collections.singletonList(exemption) : 
               Collections.emptyList();
    }
    
    // ============= AFK LISTS AND QUERIES =============
    
    @Override
    public Set<Player> getAFKPlayers() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(this::isAFK)
                .collect(Collectors.toSet());
    }
    
    @Override
    public Set<Player> getAFKPlayers(AFKStatus status) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> getAFKStatus(player) == status)
                .collect(Collectors.toSet());
    }
    
    @Override
    public Set<Player> getAFKPlayersInWorld(World world) {
        if (world == null) {
            return Collections.emptySet();
        }
        
        return world.getPlayers().stream()
                .filter(this::isAFK)
                .collect(Collectors.toSet());
    }
    
    @Override
    public Set<Player> getAFKPlayersNear(Location location, double radius) {
        if (location == null || location.getWorld() == null) {
            return Collections.emptySet();
        }
        
        return location.getWorld().getPlayers().stream()
                .filter(player -> isAFK(player) && 
                       player.getLocation().distance(location) <= radius)
                .collect(Collectors.toSet());
    }
    
    @Override
    public int getAFKPlayerCount() {
        return getAFKPlayers().size();
    }
    
    // ============= TIME LIMITS AND THRESHOLDS =============
    
    @Override
    public Duration getAFKTimeLimit(Player player) {
        if (player == null) {
            return Duration.ofMinutes(5); // Default
        }
        
        // Check for custom timeout
        Duration custom = customTimeouts.get(player.getUniqueId());
        if (custom != null) {
            return custom;
        }
        
        // This would integrate with configuration system
        return Duration.ofMinutes(5); // Placeholder
    }
    
    @Override
    public Duration getKickTimeLimit(Player player) {
        // This would integrate with configuration system
        return Duration.ofMinutes(10); // Placeholder
    }
    
    @Override
    public void setCustomAFKTimeLimit(Player player, Duration duration) {
        if (player == null) {
            return;
        }
        
        if (duration == null) {
            customTimeouts.remove(player.getUniqueId());
        } else {
            customTimeouts.put(player.getUniqueId(), duration);
        }
        
        plugin.debug("Set custom AFK time limit for " + player.getName() + ": " + duration);
    }
    
    @Override
    public Optional<Duration> getTimeUntilAFK(Player player) {
        if (player == null || !player.isOnline() || isAFK(player)) {
            return Optional.empty();
        }
        
        Duration timeSinceActivity = getTimeSinceLastActivity(player);
        Duration afkLimit = getAFKTimeLimit(player);
        Duration remaining = afkLimit.minus(timeSinceActivity);
        
        return remaining.isNegative() ? Optional.empty() : Optional.of(remaining);
    }
    
    @Override
    public Optional<Duration> getTimeUntilKick(Player player) {
        if (player == null || !player.isOnline() || !isAFK(player)) {
            return Optional.empty();
        }
        
        // This would integrate with kick timing logic
        return Optional.of(Duration.ofMinutes(5)); // Placeholder
    }
    
    // ============= WORLD AND ZONE MANAGEMENT =============
    
    @Override
    public boolean isAFKDetectionEnabled(World world) {
        if (world == null) {
            return false;
        }

        var configManager = plugin.getConfigManager();
        if (configManager == null) {
            return true;
        }

        String name = world.getName();
        if (configManager.getDisabledWorlds().contains(name)) {
            return false;
        }
        var enabledWorlds = configManager.getEnabledWorlds();
        if (!enabledWorlds.isEmpty() && !enabledWorlds.contains(name)) {
            return false;
        }
        return true;
    }
    
    @Override
    public void setAFKDetectionEnabled(World world, boolean enabled) {
        if (world == null) {
            return;
        }
        
        var configManager = plugin.getConfigManager();
        if (configManager != null) {
            configManager.setWorldDetectionEnabled(world.getName(), enabled);
        }
        plugin.debug("Set AFK detection in world " + world.getName() + " to " + enabled);
    }
    
    @Override
    public boolean isAFKAllowedAt(Location location) {
        return resolveZoneInfo(location)
                .map(AFKZoneInfo::isAfkAllowed)
                .orElse(true);
    }
    
    @Override
    public Optional<AFKZoneInfo> getAFKZoneAt(Location location) {
        return resolveZoneInfo(location);
    }
    
    // ============= STATISTICS AND ANALYTICS =============
    
    @Override
    public AFKStatistics getAFKStatistics() {
        return plugin.getPerformanceOptimizer().executeWithMonitoring("api-getAFKStatistics", () -> {
            Set<Player> onlinePlayers = new HashSet<>(Bukkit.getOnlinePlayers());
            Set<Player> afkPlayers = getAFKPlayers();

            long totalAfkMillis = 0L;
            for (Player player : onlinePlayers) {
                PlayerAFKStatistics stats = getPlayerStatistics(player);
                if (stats != null) {
                    totalAfkMillis += stats.getTotalAfkTime();
                }
            }
            double averageMinutes = onlinePlayers.isEmpty()
                    ? 0.0
                    : (totalAfkMillis / 1000.0 / 60.0) / onlinePlayers.size();

            Map<String, Integer> reasonCounts = new LinkedHashMap<>();
            if (afkManager != null) {
                Map<UUID, String> reasons = afkManager.getAfkDetectionReasonsSnapshot();
                for (String reason : reasons.values()) {
                    if (reason == null || reason.isEmpty()) {
                        continue;
                    }
                    reasonCounts.merge(reason, 1, Integer::sum);
                }
            }

            return new AFKStatistics(
                onlinePlayers.size(),
                afkPlayers.size(),
                (int) afkPlayers.stream().filter(this::isManuallyAFK).count(),
                (int) afkPlayers.stream().filter(this::isAutomaticallyAFK).count(),
                averageMinutes,
                totalAfkMillis,
                Collections.unmodifiableMap(reasonCounts)
            );
        });
    }
    
    @Override
    public PlayerAFKStatistics getPlayerStatistics(Player player) {
        if (player == null) {
            return null;
        }
        
        return plugin.getPerformanceOptimizer().executeWithMonitoring("api-getPlayerStatistics", () -> {
            if (playerSummaries == null) {
                return null;
            }
            UUID uuid = player.getUniqueId();
            PlayerAFKSummary summary = getPlayerSummary(uuid);
            AFKSessionTracker tracker = activeSessions.get(uuid);
            return summary.toStatistics(uuid, player.getName(), tracker);
        });
    }
    
    @Override
    public AFKHistoryData getAFKHistory(Player player, Duration period) {
        if (player == null || period == null) {
            return null;
        }
        
        return plugin.getPerformanceOptimizer().executeWithMonitoring("api-getAFKHistory", () -> {
            UUID uuid = player.getUniqueId();
            List<AFKHistoryData.AFKSession> sessions = getSessionsWithinPeriod(uuid, period, player);
            int manual = 0;
            int auto = 0;
            for (AFKHistoryData.AFKSession session : sessions) {
                if (session.getStatusType() == AFKStatus.MANUAL_AFK) {
                    manual++;
                } else if (session.getStatusType() == AFKStatus.AUTO_AFK) {
                    auto++;
                }
            }
            Map<String, Object> trends = new LinkedHashMap<>();
            trends.put("manualSessions", manual);
            trends.put("autoSessions", auto);
            trends.put("totalSessions", sessions.size());
            trends.put("periodSeconds", period.getSeconds());

            return new AFKHistoryData(
                uuid,
                period,
                sessions,
                Collections.unmodifiableMap(trends)
            );
        });
    }
    
    @Override
    public PerformanceMetrics getPerformanceMetrics() {
        var perfStats = plugin.getPerformanceOptimizer().getPerformanceStats();

        double cpuUsage = perfStats.getTps() <= 0 ? 100.0 : Math.max(0.0, Math.min(100.0, (1.0 - Math.min(perfStats.getTps(), 20.0) / 20.0) * 100.0));
        Map<String, Object> moduleMetrics = new LinkedHashMap<>();
        moduleMetrics.put("cacheSize", perfStats.getCacheSize());
        moduleMetrics.put("componentCount", perfStats.getComponentCount());
        moduleMetrics.put("highActivityPlayers", perfStats.getHighActivityPlayers());
        moduleMetrics.put("lowActivityPlayers", perfStats.getLowActivityPlayers());

        return new PerformanceMetrics(
            perfStats.getTotalOperations(),
            perfStats.getAverageExecutionTime(),
            perfStats.getMemoryUsage(),
            cpuUsage,
            100, // cache hit rate (no cache stats available)
            Collections.unmodifiableMap(moduleMetrics)
        );
    }
    
    // ============= EVENT HANDLING =============
    
    @Override
    public EventRegistration registerAFKStateListener(Consumer<AFKStateChangeEvent> listener) {
        return registerEventListener("afk-state-change", listener);
    }
    
    @Override
    public EventRegistration registerPatternDetectionListener(Consumer<PatternDetectionEvent> listener) {
        return registerEventListener("pattern-detection", listener);
    }
    
    @Override
    public EventRegistration registerWarningListener(Consumer<AFKWarningEvent> listener) {
        return registerEventListener("afk-warning", listener);
    }

    private EventRegistration registerEventListener(String eventType, Object listener) {
        EventRegistration registration = new EventRegistration(eventType, listener);
        eventListeners.computeIfAbsent(eventType, k -> new HashSet<>()).add(registration);
        
        plugin.debug("Registered event listener for " + eventType);
        return registration;
    }

    // Credit events registration
    @Override
    public EventRegistration registerCreditEarnedListener(java.util.function.Consumer<me.koyere.antiafkplus.api.events.CreditEarnedEvent> listener) {
        return registerEventListener("credit-earned", listener);
    }

    @Override
    public EventRegistration registerCreditConsumedListener(java.util.function.Consumer<me.koyere.antiafkplus.api.events.CreditConsumedEvent> listener) {
        return registerEventListener("credit-consumed", listener);
    }
    
    @Override
    public void unregisterListener(EventRegistration registration) {
        if (registration == null) {
            return;
        }
        
        Set<EventRegistration> listeners = eventListeners.get(registration.getEventType());
        if (listeners != null) {
            listeners.remove(registration);
            registration.markUnregistered();
            plugin.debug("Unregistered event listener for " + registration.getEventType());
        }
    }

    public void handleInternalActivity(Player player, ActivityType activityType) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        ActivityType type = activityType != null ? activityType : ActivityType.UNKNOWN;
        long now = System.currentTimeMillis();

        Deque<ActivityRecord> records = activityHistory.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        records.addLast(new ActivityRecord(type, now));

        long cutoff = now - ACTIVITY_HISTORY_WINDOW.toMillis();
        while (!records.isEmpty() && (records.size() > MAX_ACTIVITY_RECORDS || records.peekFirst().timestamp < cutoff)) {
            records.removeFirst();
        }
    }
    
    public void handleInternalAFKStateChange(PlayerAFKStateChangeEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }

        AFKStatus fromStatus = mapAFKState(event.getFromState());
        AFKStatus toStatus = mapAFKState(event.getToState());

        updateSessionTracking(event.getPlayer(), fromStatus, toStatus, event);

        Map<String, Object> data = new HashMap<>();
        data.put("afkReason", event.getReason().name());
        data.put("detectionMethod", event.getDetectionMethod());
        data.put("activityScore", event.getActivityScore());
        data.put("wasManual", event.wasManual());
        data.put("enteringAFK", event.isEnteringAFK());
        data.put("leavingAFK", event.isLeavingAFK());
        data.put("typeChange", event.isAFKTypeChange());
        data.put("rawFromState", event.getFromState().name());
        data.put("rawToState", event.getToState().name());

        AFKSessionTracker tracker = activeSessions.get(event.getPlayer().getUniqueId());
        if (tracker != null) {
            data.put("sessionStart", tracker.getStartTime());
            data.put("sessionStatus", tracker.getStatus().name());
            data.put("sessionDurationMillis", tracker.getDurationMillis());
            data.put("sessionReason", tracker.getReason());
        }

        fireAFKStateChangeEvent(event.getPlayer(), fromStatus, toStatus, event.getReason().name(), data);
    }

    public void handleInternalAFKWarning(PlayerAFKWarningEvent warningEvent) {
        if (warningEvent == null || warningEvent.getPlayer() == null) {
            return;
        }

        Player player = warningEvent.getPlayer();
        Duration remaining = Duration.ofSeconds(Math.max(0, warningEvent.getSecondsRemaining()));
        String baseMessage = warningEvent.getCustomMessage() != null ? warningEvent.getCustomMessage()
                : plugin.getConfigManager().getMessageKickWarning().replace("{seconds}", String.valueOf(warningEvent.getSecondsRemaining()));

        AFKWarningEvent apiEvent = new AFKWarningEvent(
                player,
                remaining,
                warningEvent.getWarningNumber(),
                warningEvent.getTotalWarnings(),
                baseMessage,
                Instant.now()
        );
        apiEvent.setSendTitle(warningEvent.shouldSendTitle());

        List<AFKWarningEvent> history = warningHistory.computeIfAbsent(player.getUniqueId(), k -> new LinkedList<>());
        history.add(apiEvent);
        while (history.size() > 25) {
            history.remove(0);
        }

        dispatchWarningEvent(apiEvent);

        if (apiEvent.isCancelled()) {
            warningEvent.setCancelled(true);
        }
        if (apiEvent.getCustomMessage() != null) {
            warningEvent.setCustomMessage(apiEvent.getCustomMessage());
        }
        warningEvent.setSendTitle(apiEvent.shouldSendTitle());
    }

    public void handleInternalPatternDetection(PlayerAFKPatternDetectedEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }

        Map<String, Object> data = new HashMap<>(event.getAdditionalData());
        data.put("violationCount", event.getViolationCount());
        data.put("detectionTimespanMs", event.getDetectionTimespan());
        data.put("confidence", event.getConfidence());
        if (event.getDetectionLocation() != null) {
            var loc = event.getDetectionLocation();
            data.put("location", loc.getWorld().getName() + ":" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        }

        DetectedPattern pattern = new DetectedPattern(
                event.getPatternType().name(),
                event.getConfidence(),
                Instant.now(),
                data
        );

        PatternDetectionEvent apiEvent = new PatternDetectionEvent(
                event.getPlayer(),
                pattern,
                Instant.now(),
                data
        );

        dispatchPatternEvent(apiEvent);

        if (apiEvent.isCancelled()) {
            event.setCancelled(true);
        }
    }

    private AFKStatus mapAFKState(PlayerAFKStateChangeEvent.AFKState state) {
        return switch (state) {
            case AFK_AUTO -> AFKStatus.AUTO_AFK;
            case AFK_MANUAL -> AFKStatus.MANUAL_AFK;
            case ACTIVE -> AFKStatus.ACTIVE;
        };
    }

    private Optional<AFKZoneInfo> resolveZoneInfo(Location location) {
        if (location == null) {
            return Optional.empty();
        }

        var world = location.getWorld();
        if (world == null) {
            return Optional.empty();
        }

        var config = plugin.getConfig();
        boolean zonesEnabled = config.getBoolean("zone-management.enabled", false);
        boolean defaultAllowed = config.getBoolean("zone-management.default-afk-allowed", true);
        int defaultTimeoutSeconds = config.getInt("zone-management.default-timeout-seconds", 300);
        Duration defaultTimeout = Duration.ofSeconds(Math.max(0, defaultTimeoutSeconds));

        if (!zonesEnabled) {
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("zone-management.enabled", false);
            props.put("afk-allowed", defaultAllowed);
            props.put("timeout-seconds", defaultTimeoutSeconds);
            return Optional.of(
                    new AFKZoneInfo("global", "global", defaultAllowed, defaultTimeout, Collections.unmodifiableMap(props))
            );
        }

        ZoneLookupResult lookup = findZone(location);
        String zoneName = lookup != null ? lookup.zoneName : "default";
        String zoneType = lookup != null ? lookup.zoneType : "default";

        ConfigurationSection zoneSection = config.getConfigurationSection("zone-management.zones." + zoneName);
        boolean afkAllowed = zoneSection != null
                ? zoneSection.getBoolean("afk-allowed", defaultAllowed)
                : defaultAllowed;
        int timeoutSeconds = zoneSection != null
                ? zoneSection.getInt("timeout-seconds", defaultTimeoutSeconds)
                : defaultTimeoutSeconds;
        Duration timeout = Duration.ofSeconds(Math.max(0, timeoutSeconds));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("afk-allowed", afkAllowed);
        properties.put("timeout-seconds", timeoutSeconds);
        if (zoneSection != null) {
            for (String key : zoneSection.getKeys(false)) {
                Object raw = zoneSection.get(key);
                properties.putIfAbsent(key, sanitizeConfigValue(raw));
            }
        }

        AFKZoneInfo info = new AFKZoneInfo(
                zoneName,
                zoneType,
                afkAllowed,
                timeout,
                Collections.unmodifiableMap(properties)
        );
        return Optional.of(info);
    }

    private ZoneLookupResult findZone(Location location) {
        if (location == null) {
            return null;
        }
        var config = plugin.getConfig();
        if (!config.getBoolean("zone-management.enabled", false)) {
            return null;
        }
        var wg = plugin.getWorldGuardIntegration();
        if (wg != null && wg.isAvailable()) {
            String zone = wg.determineZoneAt(location);
            if (zone != null) {
                return new ZoneLookupResult(zone, "region");
            }
        }
        if (config.contains("zone-management.zones.spawn")) {
            return new ZoneLookupResult("spawn", "config");
        }
        return null;
    }

    private Object sanitizeConfigValue(Object value) {
        if (value instanceof ConfigurationSection section) {
            Map<String, Object> nested = new LinkedHashMap<>();
            for (String key : section.getKeys(false)) {
                nested.put(key, sanitizeConfigValue(section.get(key)));
            }
            return nested;
        } else if (value instanceof List<?> list) {
            List<Object> cleaned = new ArrayList<>(list.size());
            for (Object element : list) {
                cleaned.add(sanitizeConfigValue(element));
            }
            return cleaned;
        }
        return value;
    }

    private void updateSessionTracking(Player player, AFKStatus fromStatus, AFKStatus toStatus,
                                       PlayerAFKStateChangeEvent event) {
        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Instant now = Instant.now();

        if (!fromStatus.isAFK() && toStatus.isAFK()) {
            AFKSessionTracker tracker = new AFKSessionTracker(
                    toStatus,
                    now,
                    event.getReason().name(),
                    event.getDetectionMethod(),
                    player.getLocation()
            );
            activeSessions.put(uuid, tracker);
            playerSummaries.computeIfAbsent(uuid, k -> new PlayerAFKSummary()).markSessionStart(now);
        } else if (fromStatus.isAFK() && toStatus.isAFK()) {
            AFKSessionTracker tracker = activeSessions.get(uuid);
            if (tracker != null) {
                tracker.update(toStatus, event.getReason().name(), event.getDetectionMethod());
            }
        } else if (fromStatus.isAFK() && !toStatus.isAFK()) {
            AFKSessionTracker tracker = activeSessions.remove(uuid);
            if (tracker != null) {
                AFKHistoryData.AFKSession session = tracker.toSession(now, player.getLocation());
                addSessionHistory(uuid, session);
                playerSummaries.computeIfAbsent(uuid, k -> new PlayerAFKSummary()).recordSession(session);
            }
        }
    }

    private void fireAFKStateChangeEvent(Player player, AFKStatus fromStatus, AFKStatus toStatus, String reason) {
        fireAFKStateChangeEvent(player, fromStatus, toStatus, reason, Collections.emptyMap());
    }

    private void fireAFKStateChangeEvent(Player player, AFKStatus fromStatus, AFKStatus toStatus,
                                         String reason, Map<String, Object> eventData) {
        Map<String, Object> payload = new HashMap<>();
        if (eventData != null && !eventData.isEmpty()) {
            payload.putAll(eventData);
        }

        AFKStateChangeEvent event = new AFKStateChangeEvent(
            player, fromStatus, toStatus, reason, Instant.now(), payload
        );
        
        Set<EventRegistration> listeners = eventListeners.get("afk-state-change");
        if (listeners != null) {
            for (EventRegistration registration : listeners) {
                try {
                    @SuppressWarnings("unchecked")
                    Consumer<AFKStateChangeEvent> listener = (Consumer<AFKStateChangeEvent>) registration.getListener();
                    listener.accept(event);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error firing AFK state change event: " + e.getMessage());
                }
            }
        }
    }

    private List<AFKHistoryData.AFKSession> getSessionsWithinPeriod(UUID uuid, Duration period, Player player) {
        if (uuid == null || period == null) {
            return Collections.emptyList();
        }

        Instant cutoff = Instant.now().minus(period);
        List<AFKHistoryData.AFKSession> result = new ArrayList<>();

        List<AFKHistoryData.AFKSession> stored = sessionHistory.get(uuid);
        if (stored != null) {
            synchronized (stored) {
                for (AFKHistoryData.AFKSession session : stored) {
                    Instant end = session.getEndTime() != null ? session.getEndTime() : Instant.now();
                    if (!end.isBefore(cutoff)) {
                        result.add(session);
                    }
                }
            }
        }

        AFKSessionTracker tracker = activeSessions.get(uuid);
        if (tracker != null) {
            Location loc = (player != null && player.isOnline()) ? player.getLocation() : null;
            AFKHistoryData.AFKSession current = tracker.toSession(Instant.now(), loc);
            Instant currentEnd = current.getEndTime() != null ? current.getEndTime() : Instant.now();
            if (!currentEnd.isBefore(cutoff)) {
                result.add(current);
            }
        }

        result.sort(Comparator.comparing(AFKHistoryData.AFKSession::getStartTime));
        return Collections.unmodifiableList(new ArrayList<>(result));
    }

    private void dispatchWarningEvent(AFKWarningEvent apiEvent) {
        Set<EventRegistration> listeners = eventListeners.get("afk-warning");
        if (listeners == null) {
            return;
        }
        for (EventRegistration registration : listeners) {
            try {
                @SuppressWarnings("unchecked")
                Consumer<AFKWarningEvent> listener = (Consumer<AFKWarningEvent>) registration.getListener();
                listener.accept(apiEvent);
            } catch (Exception e) {
                plugin.getLogger().warning("Error firing AFK warning event: " + e.getMessage());
            }
        }
    }

    private void dispatchPatternEvent(PatternDetectionEvent apiEvent) {
        Set<EventRegistration> listeners = eventListeners.get("pattern-detection");
        if (listeners == null) {
            return;
        }
        for (EventRegistration registration : listeners) {
            try {
                @SuppressWarnings("unchecked")
                Consumer<PatternDetectionEvent> listener = (Consumer<PatternDetectionEvent>) registration.getListener();
                listener.accept(apiEvent);
            } catch (Exception e) {
                plugin.getLogger().warning("Error firing pattern detection event: " + e.getMessage());
            }
        }
    }
    
    // ============= ASYNC OPERATIONS =============
    
    @Override
    public CompletableFuture<AFKStatus> getAFKStatusAsync(Player player) {
        return CompletableFuture.supplyAsync(() -> getAFKStatus(player));
    }
    
    @Override
    public CompletableFuture<ActivityStatistics> getActivityStatisticsAsync(Player player, Duration period) {
        return CompletableFuture.supplyAsync(() -> getActivityStatistics(player, period));
    }
    
    @Override
    public CompletableFuture<AFKHistoryData> getAFKHistoryAsync(Player player, Duration period) {
        return CompletableFuture.supplyAsync(() -> getAFKHistory(player, period));
    }
    
    // ============= CONFIGURATION =============
    
    @Override
    public boolean isModuleEnabled(String moduleName) {
        ModuleManager moduleManager = plugin.getModuleManager();
        return moduleManager != null && moduleManager.isModuleEnabled(moduleName);
    }
    
    @Override
    public Set<String> getEnabledModules() {
        ModuleManager moduleManager = plugin.getModuleManager();
        if (moduleManager == null) {
            return Collections.emptySet();
        }
        
        return moduleManager.getEnabledModules().stream()
                .map(module -> module.getConfigKey())
                .collect(Collectors.toSet());
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getConfigValue(String path, T defaultValue) {
        if (plugin.getConfigManager() == null) {
            return defaultValue;
        }
        
        Object value = plugin.getConfig().get(path, defaultValue);
        try {
            return (T) value;
        } catch (ClassCastException e) {
            plugin.getLogger().warning("Configuration value at '" + path + "' is not of expected type");
            return defaultValue;
        }
    }
    
    @Override
    public boolean isDebugMode() {
        return plugin.isDebugEnabled();
    }
    
    // ============= LOCALIZATION =============
    
    @Override
    public String getMessage(Player player, String messageKey, Object... placeholders) {
        LocalizationManager localizationManager = plugin.getLocalizationManager();
        if (localizationManager == null) {
            return "[" + messageKey + "]";
        }
        
        return localizationManager.getMessage(player, messageKey, placeholders);
    }
    
    @Override
    public Set<String> getAvailableLanguages() {
        LocalizationManager localizationManager = plugin.getLocalizationManager();
        return localizationManager != null ? 
               localizationManager.getAvailableLanguages() : 
               Collections.singleton("en");
    }
    
    @Override
    public String getPlayerLanguage(Player player) {
        LocalizationManager localizationManager = plugin.getLocalizationManager();
        return localizationManager != null ? 
               localizationManager.getPlayerLanguage(player) : 
               "en";
    }
    
    @Override
    public void setPlayerLanguage(Player player, String languageCode) {
        LocalizationManager localizationManager = plugin.getLocalizationManager();
        if (localizationManager != null) {
            localizationManager.setPlayerLanguage(player, languageCode);
        }
    }
    
    // ============= BEDROCK COMPATIBILITY =============
    
    @Override
    public boolean isBedrockPlayer(Player player) {
        BedrockCompatibility bedrockCompatibility = plugin.getBedrockCompatibility();
        return bedrockCompatibility != null && bedrockCompatibility.isBedrockPlayer(player);
    }
    
    @Override
    public Optional<BedrockPlayerInfo> getBedrockPlayerInfo(Player player) {
        BedrockCompatibility bedrockCompatibility = plugin.getBedrockCompatibility();
        if (bedrockCompatibility == null || !bedrockCompatibility.isBedrockPlayer(player)) {
            return Optional.empty();
        }
        
        var bedrockInfo = bedrockCompatibility.getBedrockPlayerInfo(player);
        if (bedrockInfo == null) {
            return Optional.empty();
        }
        
        var result = bedrockInfo.getDetectionResult();
        return Optional.of(new BedrockPlayerInfo(
            result.getDeviceType(),
            result.getInputMode(),
            result.getUiProfile(),
            result.getBedrockVersion(),
            result.getDetectionMethod(),
            result.getConfidence() / 100.0
        ));
    }
    
    // ============= UTILITY METHODS =============
    
    @Override
    public String getPluginVersion() {
        return plugin.getPluginVersion();
    }
    
    @Override
    public boolean isFullyInitialized() {
        return plugin.isFullyInitialized();
    }
    
    @Override
    public PluginInfo getPluginInfo() {
        return new PluginInfo(
            plugin.getPluginVersion(),
            getAPIVersion(),
            Instant.ofEpochMilli(System.currentTimeMillis() - plugin.getStartupTime()),
            plugin.getStartupTime(),
            plugin.isFullyInitialized(),
            plugin.getPlatformScheduler().getPlatformType().getDisplayName(),
            getEnabledModules(),
            getAvailableLanguages(),
            plugin.getBedrockCompatibility() != null ? 
                plugin.getBedrockCompatibility().getStats().getBedrockPlayerCount() : 0,
            plugin.isDebugEnabled(),
            new HashMap<>(), // performance metrics
            new HashMap<>() // system info
        );
    }

    // ============= CREDIT SYSTEM (v2.5) =============

    @Override
    public long getCreditBalance(Player player) {
        var cm = plugin.getCreditManager();
        if (cm == null) return 0L;
        return cm.getBalance(player);
    }

    @Override
    public boolean hasCredits(Player player, long minutes) {
        var cm = plugin.getCreditManager();
        return cm != null && cm.hasCredits(player, minutes);
    }

    @Override
    public boolean addCredits(Player player, long minutes) {
        var cm = plugin.getCreditManager();
        return cm != null && cm.addCredits(player, minutes);
    }

    @Override
    public boolean consumeCredits(Player player, long minutes) {
        var cm = plugin.getCreditManager();
        return cm != null && cm.consumeCredits(player, minutes);
    }

    @Override
    public boolean setCreditBalance(Player player, long minutes) {
        var cm = plugin.getCreditManager();
        return cm != null && cm.setCreditBalance(player, minutes);
    }

    @Override
    public String getCreditRatio(Player player) {
        var cm = plugin.getCreditManager();
        return cm != null ? cm.getRatioString(player) : "5:1";
    }

    @Override
    public long getMaxCredits(Player player) {
        var cm = plugin.getCreditManager();
        return cm != null ? cm.getMaxCredits(player) : 0L;
    }

    @Override
    public boolean isInAFKZone(Player player) {
        var cm = plugin.getCreditManager();
        return cm != null && cm.isInAfkZone(player);
    }

    @Override
    public Location getAFKZoneLocation(Player player) {
        var cm = plugin.getCreditManager();
        return cm != null ? cm.getAFKZoneLocation(player) : null;
    }

    @Override
    public Location getOriginalLocation(Player player) {
        var cm = plugin.getCreditManager();
        return cm != null ? cm.getOriginalLocation(player) : null;
    }

    @Override
    public boolean returnFromAFKZone(Player player) {
        var cm = plugin.getCreditManager();
        if (cm == null) return false;
        var result = cm.returnFromAFKZone(player);
        return result == me.koyere.antiafkplus.credit.CreditManager.ReturnResult.SUCCESS;
    }

    @Override
    public java.util.List<me.koyere.antiafkplus.api.data.CreditTransaction> getCreditHistory(Player player, int limit) {
        var cm = plugin.getCreditManager();
        if (cm == null || !cm.isHistoryAvailable()) return java.util.Collections.emptyList();
        return cm.getHistory(player, Math.max(1, Math.min(50, limit)));
    }

    @Override
    public java.time.Instant getCreditExpiration(Player player) {
        var cm = plugin.getCreditManager();
        if (cm == null) return null;
        return cm.getExpirationInstant(player);
    }

    private void addSessionHistory(UUID uuid, AFKHistoryData.AFKSession session) {
        List<AFKHistoryData.AFKSession> history = sessionHistory.computeIfAbsent(uuid, k -> Collections.synchronizedList(new LinkedList<>()));
        history.add(session);
        while (history.size() > MAX_SESSIONS_STORED) {
            history.remove(0);
        }
    }

    private List<AFKHistoryData.AFKSession> getSessionHistory(UUID uuid) {
        List<AFKHistoryData.AFKSession> history = sessionHistory.get(uuid);
        if (history == null) {
            return Collections.emptyList();
        }
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    private PlayerAFKSummary getPlayerSummary(UUID uuid) {
        return playerSummaries.computeIfAbsent(uuid, k -> new PlayerAFKSummary());
    }

    private static class ZoneLookupResult {
        private final String zoneName;
        private final String zoneType;

        private ZoneLookupResult(String zoneName, String zoneType) {
            this.zoneName = zoneName;
            this.zoneType = zoneType;
        }
    }

    private static class ActivityRecord {
        private final ActivityType type;
        private final long timestamp;

        private ActivityRecord(ActivityType type, long timestamp) {
            this.type = type;
            this.timestamp = timestamp;
        }
    }

    private static class AFKSessionTracker {
        private AFKStatus status;
        private final Instant startTime;
        private String reason;
        private String detectionMethod;
        private final Location startLocation;

        private AFKSessionTracker(AFKStatus status, Instant startTime, String reason, String detectionMethod, Location location) {
            this.status = status;
            this.startTime = startTime;
            this.reason = reason;
            this.detectionMethod = detectionMethod;
            this.startLocation = location != null ? location.clone() : null;
        }

        private void update(AFKStatus newStatus, String reason, String detectionMethod) {
            this.status = newStatus;
            if (reason != null) {
                this.reason = reason;
            }
            if (detectionMethod != null) {
                this.detectionMethod = detectionMethod;
            }
        }

        private AFKHistoryData.AFKSession toSession(Instant endTime, Location fallbackLocation) {
            Location location = startLocation != null ? startLocation.clone() : (fallbackLocation != null ? fallbackLocation.clone() : null);
            String sessionReason = reason != null ? reason : (detectionMethod != null ? detectionMethod : "unknown");
            return new AFKHistoryData.AFKSession(startTime, endTime, status, sessionReason, location);
        }

        private long getDurationMillis() {
            return Duration.between(startTime, Instant.now()).toMillis();
        }

        private AFKStatus getStatus() {
            return status;
        }

        private Instant getStartTime() {
            return startTime;
        }

        private String getReason() {
            return reason;
        }
    }

    private static class PlayerAFKSummary {
        private long totalAfkMillis = 0L;
        private int sessionCount = 0;
        private int manualSessions = 0;
        private int autoSessions = 0;
        private long longestSessionMillis = 0L;
        private Instant firstAfkStart;
        private Instant lastAfkEnd;

        private void markSessionStart(Instant start) {
            if (firstAfkStart == null) {
                firstAfkStart = start;
            }
        }

        private void recordSession(AFKHistoryData.AFKSession session) {
            long duration = session.getDuration().toMillis();
            totalAfkMillis += duration;
            sessionCount++;
            if (session.getStatusType() == AFKStatus.MANUAL_AFK) {
                manualSessions++;
            } else if (session.getStatusType() == AFKStatus.AUTO_AFK) {
                autoSessions++;
            }
            if (duration > longestSessionMillis) {
                longestSessionMillis = duration;
            }
            if (firstAfkStart == null) {
                firstAfkStart = session.getStartTime();
            }
            lastAfkEnd = session.getEndTime();
        }

        private PlayerAFKStatistics toStatistics(UUID uuid, String name, AFKSessionTracker activeTracker) {
            long activeMillis = activeTracker != null ? activeTracker.getDurationMillis() : 0L;
            int activeManual = activeTracker != null && activeTracker.getStatus() == AFKStatus.MANUAL_AFK ? 1 : 0;
            int activeAuto = activeTracker != null && activeTracker.getStatus() == AFKStatus.AUTO_AFK ? 1 : 0;

            long totalMillis = totalAfkMillis + activeMillis;
            int totalSessions = sessionCount + (activeTracker != null ? 1 : 0);
            long longest = Math.max(longestSessionMillis, activeMillis);
            Instant first = firstAfkStart != null ? firstAfkStart : (activeTracker != null ? activeTracker.getStartTime() : null);
            Instant last = activeTracker != null ? Instant.now() : lastAfkEnd;
            double averageSeconds = totalSessions > 0 ? (totalMillis / 1000.0) / totalSessions : 0.0;

            return new PlayerAFKStatistics(
                    uuid,
                    name,
                    totalMillis,
                    totalSessions,
                    longest,
                    averageSeconds,
                    manualSessions + activeManual,
                    autoSessions + activeAuto,
                    first,
                    last
            );
        }
    }
    
    /**
     * Clear player data on disconnect.
     */
    public void clearPlayerData(Player player) {
        if (player == null) {
            return;
        }
        
        UUID uuid = player.getUniqueId();
        playerExemptions.remove(uuid);
        customTimeouts.remove(uuid);
        activityHistory.remove(uuid);
        activeSessions.remove(uuid);
        sessionHistory.remove(uuid);
        playerSummaries.remove(uuid);
        warningHistory.remove(uuid);
        
        plugin.debug("Cleared API data for " + player.getName());
    }

    // ============= INTERNAL CREDIT EVENT DISPATCHERS =============

    public void fireCreditEarned(me.koyere.antiafkplus.api.events.CreditEarnedEvent event) {
        var listeners = eventListeners.get("credit-earned");
        if (listeners == null) return;
        for (EventRegistration reg : listeners) {
            if (!reg.isUnregistered() && reg.getListener() instanceof java.util.function.Consumer) {
                try {
                    @SuppressWarnings("unchecked")
                    java.util.function.Consumer<me.koyere.antiafkplus.api.events.CreditEarnedEvent> c =
                            (java.util.function.Consumer<me.koyere.antiafkplus.api.events.CreditEarnedEvent>) reg.getListener();
                    c.accept(event);
                } catch (Throwable ignored) {}
            }
        }
    }

    public void fireCreditConsumed(me.koyere.antiafkplus.api.events.CreditConsumedEvent event) {
        var listeners = eventListeners.get("credit-consumed");
        if (listeners == null) return;
        for (EventRegistration reg : listeners) {
            if (!reg.isUnregistered() && reg.getListener() instanceof java.util.function.Consumer) {
                try {
                    @SuppressWarnings("unchecked")
                    java.util.function.Consumer<me.koyere.antiafkplus.api.events.CreditConsumedEvent> c =
                            (java.util.function.Consumer<me.koyere.antiafkplus.api.events.CreditConsumedEvent>) reg.getListener();
                    c.accept(event);
                } catch (Throwable ignored) {}
            }
        }
    }
}
