// AFKManager.java - Enhanced v2.0 with Full Event Integration
package me.koyere.antiafkplus.afk;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.api.AntiAFKPlusAPIImpl;
import me.koyere.antiafkplus.api.data.ActivityType;
import me.koyere.antiafkplus.events.PlayerAFKKickEvent;
import me.koyere.antiafkplus.events.PlayerAFKStateChangeEvent;
import me.koyere.antiafkplus.events.PlayerAFKWarningEvent;
import me.koyere.antiafkplus.utils.AFKLogger;
import me.koyere.antiafkplus.time.TimeWindowService;
import me.koyere.antiafkplus.time.TimeWindowService.WindowBehavior;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import me.koyere.antiafkplus.platform.PlatformScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Enhanced AFK Manager v2.0 - Manages AFK detection, state, and voluntary AFK mode
 * with advanced pattern detection, behavioral analysis, and comprehensive event system.
 */
public class AFKManager {

    private static final long ACTIVITY_HISTORY_WINDOW_MS = Duration.ofMinutes(30).toMillis();
    private static final long ACTIVITY_SCORE_WINDOW_MS = Duration.ofMinutes(5).toMillis();
    private static final int MAX_STORED_ACTIVITIES = 512;

    private final AntiAFKPlus plugin;
    private final MovementListener movementListener;
    private final PatternDetector patternDetector;

    private final Set<UUID> afkPlayers = new HashSet<>(); // Players currently marked as AFK (auto or manual)
    private final Map<UUID, Set<Integer>> warningsSent = new HashMap<>(); // Tracks warnings sent to avoid spam
    private final Set<UUID> manualAfkUsernames = new HashSet<>(); // Players who used /afk command (stores UUID)
    private final Map<UUID, Long> manualAfkStartTimes = new HashMap<>(); // Tracks when manual AFK started

    // Enhanced tracking for v2.0
    private final Map<UUID, Long> afkDetectionTimes = new HashMap<>(); // When player was first detected as AFK
    private final Map<UUID, String> afkDetectionReasons = new HashMap<>(); // Why player was marked AFK
    private final Map<UUID, PlayerActivityData> playerActivityData = new HashMap<>(); // Enhanced activity tracking
    private final Map<UUID, Integer> warningCounts = new HashMap<>(); // Count of warnings sent per player
    private final Map<UUID, Long> windowMessageCooldown = new HashMap<>();
    
    // Professional fix: Prevent repeated kick/teleport actions
    private final Set<UUID> playersAlreadyActioned = new HashSet<>(); // Players who already received final AFK action

    private PlatformScheduler.ScheduledTask afkCheckTask;

    public AFKManager(AntiAFKPlus plugin, MovementListener movementListener) {
        this.plugin = plugin;
        this.movementListener = movementListener;
        this.patternDetector = new PatternDetector(plugin, movementListener, this);
        startAFKCheckTask();
    }

    private void startAFKCheckTask() {
        if (this.afkCheckTask != null && !this.afkCheckTask.isCancelled()) {
            this.afkCheckTask.cancel();
        }

        int intervalTicks = plugin.getConfigManager().getAfkCheckIntervalSeconds() * 20;
        if (intervalTicks <= 0) {
            plugin.getLogger().warning("AFK Check Interval (afk-check-interval) must be > 0. AFK check task will not start.");
            return;
        }

        Runnable taskBody = () -> {
                TimeWindowService.WindowEvaluation windowEvaluation = null;
                if (plugin.getTimeWindowService() != null) {
                    windowEvaluation = plugin.getTimeWindowService().evaluate();
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();

                    if (player.hasPermission("antiafkplus.bypass")) {
                        if (afkPlayers.contains(uuid) || manualAfkUsernames.contains(uuid)) {
                            AFKLogger.logActivity(player.getName() + " has bypass, ensuring AFK state is cleared.");
                            forceSetManualAFKState(player, false);
                        }
                        continue;
                    }

                    List<String> disabledWorlds = plugin.getConfigManager().getDisabledWorlds();
                    List<String> enabledWorlds = plugin.getConfigManager().getEnabledWorlds();
                    String currentWorldName = player.getWorld().getName();

                    // PROFESSIONAL FIX: Clear AFK state when player is in disabled world
                    // This prevents warnings and actions from persisting in disabled worlds
                    if (disabledWorlds.contains(currentWorldName)) {
                        // If player has AFK state, clear it completely
                        if (afkPlayers.contains(uuid) || manualAfkUsernames.contains(uuid)) {
                            AFKLogger.logActivity(player.getName() + " entered disabled world '" + currentWorldName + "', clearing AFK state.");

                            // Clear all AFK-related data
                            unmarkAsAFKInternal(player);
                            forceSetManualAFKState(player, false);
                            playersAlreadyActioned.remove(uuid);
                            warningCounts.remove(uuid);
                            afkDetectionTimes.remove(uuid);
                            afkDetectionReasons.remove(uuid);
                        }
                        continue;
                    }

                    // If enabled-worlds is configured and player is not in one, clear state
                    if (!enabledWorlds.isEmpty() && !enabledWorlds.contains(currentWorldName)) {
                        if (afkPlayers.contains(uuid) || manualAfkUsernames.contains(uuid)) {
                            AFKLogger.logActivity(player.getName() + " is in non-enabled world '" + currentWorldName + "', clearing AFK state.");

                            unmarkAsAFKInternal(player);
                            forceSetManualAFKState(player, false);
                            playersAlreadyActioned.remove(uuid);
                            warningCounts.remove(uuid);
                            afkDetectionTimes.remove(uuid);
                            afkDetectionReasons.remove(uuid);
                        }
                        continue;
                    }

                    // Refresh cached activity statistics window
                    refreshPlayerActivityData(player);

                    if (manualAfkUsernames.contains(uuid)) {
                        long voluntaryAfkLimitMillis = plugin.getConfigManager().getMaxVoluntaryAfkTimeSeconds() * 1000L;
                        if (voluntaryAfkLimitMillis > 0 && manualAfkStartTimes.containsKey(uuid)) {
                            long manualAfkDurationMillis = System.currentTimeMillis() - manualAfkStartTimes.get(uuid);
                            if (manualAfkDurationMillis >= voluntaryAfkLimitMillis) {
                                // Fire event for time limit exceeded
                                fireAFKStateChangeEvent(player, PlayerAFKStateChangeEvent.AFKState.AFK_MANUAL,
                                        PlayerAFKStateChangeEvent.AFKState.ACTIVE,
                                        PlayerAFKStateChangeEvent.AFKReason.TIME_LIMIT_EXCEEDED,
                                        "voluntary_time_limit", false);

                                player.sendMessage(plugin.getConfigManager().getMessageVoluntaryAFKLimit());
                                forceSetManualAFKState(player, false);
                            }
                        }
                        continue; // Skip auto AFK checks if manually AFK and not timed out
                    }

                    // Enhanced AFK detection with multiple activity checks
                    boolean shouldBeAFK = performEnhancedAFKCheck(player);
                    boolean applyWindowControls = false;
                    WindowBehavior windowBehavior = WindowBehavior.DEFAULT;
                    long windowExtendMillis = 0L;
                    if (windowEvaluation != null && windowEvaluation.featureEnabled() && windowEvaluation.insideWindow()) {
                        boolean bypass = hasWindowBypass(player, windowEvaluation.bypassPermission());
                        if (!bypass) {
                            applyWindowControls = true;
                            windowBehavior = windowEvaluation.behavior();
                            windowExtendMillis = windowEvaluation.extendMillis();
                        }
                    }

                    if (!shouldBeAFK && plugin.getCountdownSequenceService() != null
                            && plugin.getCountdownSequenceService().isRunning(player)) {
                        long countdownStart = plugin.getCountdownSequenceService().getCountdownStart(player);
                        long lastActivityTs = getLastRecordedActivityTimestamp(player);
                        if (countdownStart > 0 && lastActivityTs <= countdownStart + 50L) {
                            shouldBeAFK = true;
                        }
                    }

                    if (shouldBeAFK) {
                        if (!afkPlayers.contains(uuid)) {
                            String detectionReason = determineAFKReason(player);
                            markAsAFKInternal(player, "auto (" + detectionReason + ")", detectionReason);
                            afkDetectionTimes.put(uuid, System.currentTimeMillis());
                            afkDetectionReasons.put(uuid, detectionReason);
                        }

                        // Professional fix: Only execute final action once per AFK session
                        if (!playersAlreadyActioned.contains(uuid)) {
                            long timeSinceDetection = System.currentTimeMillis() - afkDetectionTimes.getOrDefault(uuid, System.currentTimeMillis());
                            boolean canExecuteAction = true;

                            if (applyWindowControls) {
                                switch (windowBehavior) {
                                    case SKIP_ACTIONS -> canExecuteAction = false;
                                    case MESSAGE_ONLY -> {
                                        canExecuteAction = false;
                                        sendWindowMessage(player, windowEvaluation);
                                    }
                                    case EXTEND_THRESHOLD -> {
                                        long required = Math.max(1000L, windowExtendMillis);
                                        canExecuteAction = timeSinceDetection >= required;
                                    }
                                    default -> { }
                                }
                            }

                            // Only take action if player has been AFK long enough (prevents immediate actions)
                            if (canExecuteAction && timeSinceDetection >= 1000) { // At least 1 second delay for safety
                                kickPlayerAfterAFK(player, timeSinceDetection);
                                playersAlreadyActioned.add(uuid); // Mark as actioned to prevent repeated calls
                            }
                        }
                    } else {
                        if (!applyWindowControls) {
                            checkWarnings(player);
                        }
                        if (afkPlayers.contains(uuid)) {
                            long afkDuration = System.currentTimeMillis() - afkDetectionTimes.getOrDefault(uuid, System.currentTimeMillis());
                            String detectionMethod = afkDetectionReasons.getOrDefault(uuid, "auto_detection");

                            // Fire state change event
                            fireAFKStateChangeEvent(player, PlayerAFKStateChangeEvent.AFKState.AFK_AUTO,
                                    PlayerAFKStateChangeEvent.AFKState.ACTIVE,
                                    determineActivityReason(player), detectionMethod, false);

                            AFKLogger.logAFKExit(player, "auto (activity detected)", afkDuration / 1000L);
                            unmarkAsAFKInternal(player);
                            afkDetectionTimes.remove(uuid);
                            afkDetectionReasons.remove(uuid);
                            warningCounts.remove(uuid);
                            
                            // Professional fix: Clear actioned state when player becomes active again
                            playersAlreadyActioned.remove(uuid);
                        }
                    }
                }
            };

        this.afkCheckTask = plugin.getPlatformScheduler()
                .runTaskTimer(taskBody, intervalTicks, intervalTicks);
        // AFK check task started silently
    }

    private boolean performEnhancedAFKCheck(Player player) {
        long currentTime = System.currentTimeMillis();
        long afkThresholdMillis = getPlayerAfkTime(player) * 1000L;

        // Check multiple activity indicators
        long lastMovement = movementListener.getLastMovementTimestamp(player);
        long lastHeadRotation = movementListener.getLastHeadRotationTime(player);
        long lastJump = movementListener.getLastJumpTime(player);
        long lastCommand = movementListener.getLastCommandTime(player);

        // Find the most recent activity
        long mostRecentActivity = Math.max(Math.max(lastMovement, lastHeadRotation),
                Math.max(lastJump, lastCommand));

        long timeSinceActivity = currentTime - mostRecentActivity;

        // Check if enough time has passed since last activity
        return timeSinceActivity >= afkThresholdMillis;
    }

    private String determineAFKReason(Player player) {
        long currentTime = System.currentTimeMillis();
        long lastMovement = movementListener.getLastMovementTimestamp(player);
        long lastHeadRotation = movementListener.getLastHeadRotationTime(player);
        long lastJump = movementListener.getLastJumpTime(player);
        long lastCommand = movementListener.getLastCommandTime(player);

        // Determine what type of inactivity caused AFK status
        if (currentTime - lastMovement > getPlayerAfkTime(player) * 1000L) {
            if (currentTime - lastHeadRotation > getPlayerAfkTime(player) * 1000L) {
                return "no_movement_or_rotation";
            }
            return "no_movement";
        } else if (currentTime - lastCommand > getPlayerAfkTime(player) * 1000L * 2) { // Commands have longer threshold
            return "no_commands";
        }

        return "general_inactivity";
    }

    private PlayerAFKStateChangeEvent.AFKReason determineActivityReason(Player player) {
        long currentTime = System.currentTimeMillis();
        long lastMovement = movementListener.getLastMovementTimestamp(player);
        long lastHeadRotation = movementListener.getLastHeadRotationTime(player);
        long lastJump = movementListener.getLastJumpTime(player);
        long lastCommand = movementListener.getLastCommandTime(player);

        // Find most recent activity type
        long mostRecent = Math.max(Math.max(lastMovement, lastHeadRotation), Math.max(lastJump, lastCommand));

        if (mostRecent == lastMovement && (currentTime - lastMovement) < 5000) {
            return PlayerAFKStateChangeEvent.AFKReason.MOVEMENT_DETECTED;
        } else if (mostRecent == lastHeadRotation && (currentTime - lastHeadRotation) < 5000) {
            return PlayerAFKStateChangeEvent.AFKReason.HEAD_ROTATION;
        } else if (mostRecent == lastJump && (currentTime - lastJump) < 5000) {
            return PlayerAFKStateChangeEvent.AFKReason.JUMP_ACTIVITY;
        } else if (mostRecent == lastCommand && (currentTime - lastCommand) < 5000) {
            return PlayerAFKStateChangeEvent.AFKReason.COMMAND_ACTIVITY;
        }

        return PlayerAFKStateChangeEvent.AFKReason.MOVEMENT_DETECTED; // Default
    }

    private long getLastRecordedActivityTimestamp(Player player) {
        if (player == null) {
            return System.currentTimeMillis();
        }
        PlayerActivityData data = playerActivityData.get(player.getUniqueId());
        if (data != null && data.getLastActivityTimestamp() > 0) {
            return data.getLastActivityTimestamp();
        }
        return movementListener.getLastMovementTimestamp(player);
    }

    private boolean hasWindowBypass(Player player, String permission) {
        return permission != null && !permission.isEmpty() && player.hasPermission(permission);
    }

    private void sendWindowMessage(Player player, TimeWindowService.WindowEvaluation evaluation) {
        if (player == null || evaluation == null) {
            return;
        }
        String message = plugin.getConfigManager().getMessageAfkWindowActive();
        if (message == null || message.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = windowMessageCooldown.get(player.getUniqueId());
        if (last != null && (now - last) < 60000L) {
            return;
        }
        String formatted = message.replace("{time}", evaluation.nextChangeDisplay());
        player.sendMessage(formatted);
        windowMessageCooldown.put(player.getUniqueId(), now);
    }

    private void refreshPlayerActivityData(Player player) {
        if (player == null) {
            return;
        }
        PlayerActivityData data = playerActivityData.get(player.getUniqueId());
        if (data != null) {
            data.refresh();
        }
    }

    private void recordPlayerActivity(Player player, ActivityType activityType, long timestamp) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        PlayerActivityData data = playerActivityData.computeIfAbsent(uuid, k -> new PlayerActivityData());
        data.recordActivity(activityType != null ? activityType : ActivityType.UNKNOWN, timestamp);
        if (plugin.getPerformanceOptimizer() != null) {
            plugin.getPerformanceOptimizer().updatePlayerActivity(player);
        }
    }

    private void checkWarnings(Player player) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long afkThresholdMillis = getPlayerAfkTime(player) * 1000L;

        // Find most recent activity for warning calculation
        long lastMovement = movementListener.getLastMovementTimestamp(player);
        long lastHeadRotation = movementListener.getLastHeadRotationTime(player);
        long lastJump = movementListener.getLastJumpTime(player);
        long lastCommand = movementListener.getLastCommandTime(player);

        long mostRecentActivity = Math.max(Math.max(lastMovement, lastHeadRotation),
                Math.max(lastJump, lastCommand));

        long timeSinceActivity = currentTime - mostRecentActivity;
        int secondsRemaining = Math.max(0, (int) ((afkThresholdMillis - timeSinceActivity) / 1000L));

        for (int warningTimeSeconds : plugin.getConfigManager().getAfkWarningTimes()) {
            if (secondsRemaining <= warningTimeSeconds) {
                Set<Integer> sentPlayerWarnings = warningsSent.computeIfAbsent(uuid, k -> new HashSet<>());
                if (!sentPlayerWarnings.contains(warningTimeSeconds)) {
                    // Fire warning event
                    PlayerAFKWarningEvent warningEvent = new PlayerAFKWarningEvent(
                            player,
                            secondsRemaining,
                            warningCounts.getOrDefault(uuid, 0) + 1,
                            plugin.getConfigManager().getAfkWarningTimes().size(),
                            PlayerAFKWarningEvent.WarningType.STANDARD,
                            timeSinceActivity,
                            determineLastActivityType(player)
                    );

                    if (plugin.getAPI() instanceof AntiAFKPlusAPIImpl apiImpl) {
                        apiImpl.handleInternalAFKWarning(warningEvent);
                    }

                    Bukkit.getPluginManager().callEvent(warningEvent);

                    if (!warningEvent.isCancelled()) {
                        String message = warningEvent.getCustomMessage() != null ?
                                warningEvent.getCustomMessage() :
                                plugin.getConfigManager().getMessageKickWarning()
                                        .replace("{seconds}", String.valueOf(secondsRemaining));

                        player.sendMessage(message);

                        // Send title if enabled
                        if (warningEvent.shouldSendTitle()) {
                            player.sendTitle("§c⚠ AFK Warning",
                                    "§e" + secondsRemaining + " seconds remaining",
                                    10, 70, 20);
                        }

                        AFKLogger.logAFKWarning(player, secondsRemaining);
                        sentPlayerWarnings.add(warningTimeSeconds);
                        warningCounts.put(uuid, warningCounts.getOrDefault(uuid, 0) + 1);
                    }
                }
            }
        }
    }

    private String determineLastActivityType(Player player) {
        long lastMovement = movementListener.getLastMovementTimestamp(player);
        long lastHeadRotation = movementListener.getLastHeadRotationTime(player);
        long lastJump = movementListener.getLastJumpTime(player);
        long lastCommand = movementListener.getLastCommandTime(player);

        long mostRecent = Math.max(Math.max(lastMovement, lastHeadRotation), Math.max(lastJump, lastCommand));

        if (mostRecent == lastMovement) return "movement";
        if (mostRecent == lastHeadRotation) return "head_rotation";
        if (mostRecent == lastJump) return "jump";
        if (mostRecent == lastCommand) return "command";

        return "unknown";
    }

    private long getPlayerAfkTime(Player player) {
        for (Map.Entry<String, Integer> entry : plugin.getConfigManager().getPermissionTimes().entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                return entry.getValue();
            }
        }
        return plugin.getConfigManager().getDefaultAfkTime();
    }

    private void kickPlayerAfterAFK(Player player, long afkTimeMillis) {
        String detectionMethod = afkDetectionReasons.getOrDefault(player.getUniqueId(), "standard");
        int warningsSentCount = warningCounts.getOrDefault(player.getUniqueId(), 0);

        // Fire kick event
        PlayerAFKKickEvent kickEvent = new PlayerAFKKickEvent(
                player,
                PlayerAFKKickEvent.KickReason.INACTIVITY_TIMEOUT,
                afkTimeMillis / 1000L,
                (System.currentTimeMillis() - movementListener.getLastMovementTimestamp(player)) / 1000L,
                detectionMethod,
                warningsSentCount,
                plugin.getConfigManager().getMessageKicked()
        );

        // Check if zone management is enabled and configure kick action override
        if (plugin.getConfig().getBoolean("zone-management.enabled", false)) {
            // Check if player is in spawn zone (or any configured zone)
            String zoneName = determinePlayerZone(player);
            if (zoneName != null && plugin.getConfig().contains("zone-management.zones." + zoneName)) {
                String action = plugin.getConfig().getString("zone-management.zones." + zoneName + ".kick-action", "kick");
                if ("TELEPORT".equalsIgnoreCase(action)) {
                    kickEvent.setCustomAction(PlayerAFKKickEvent.KickAction.TELEPORT);
                    String teleportLocation = plugin.getConfig().getString("zone-management.zones." + zoneName + ".teleport-location", "");
                    kickEvent.setCustomActionData(teleportLocation);
                } else if ("TRANSFER".equalsIgnoreCase(action) || "TRANSFER_SERVER".equalsIgnoreCase(action)) {
                    kickEvent.setCustomAction(PlayerAFKKickEvent.KickAction.TRANSFER_SERVER);
                    // Prefer per-zone server if provided; fallback to global target-server
                    String transferServer = plugin.getConfig().getString("zone-management.zones." + zoneName + ".transfer-server",
                            plugin.getConfig().getString("server-transfer.target-server", ""));
                    kickEvent.setCustomActionData(transferServer);
                } else if ("KICK".equalsIgnoreCase(action)) {
                    kickEvent.setCustomAction(PlayerAFKKickEvent.KickAction.KICK);
                } else if ("MARK_AFK_ONLY".equalsIgnoreCase(action) || "MARK".equalsIgnoreCase(action)) {
                    kickEvent.setCustomAction(PlayerAFKKickEvent.KickAction.MARK_AFK_ONLY);
                } else if ("GAMEMODE".equalsIgnoreCase(action)) {
                    kickEvent.setCustomAction(PlayerAFKKickEvent.KickAction.GAMEMODE);
                } else if ("COMMAND".equalsIgnoreCase(action)) {
                    kickEvent.setCustomAction(PlayerAFKKickEvent.KickAction.COMMAND);
                }
            }
        }

        // If no zone override applied (still default KICK) and server-transfer is enabled, set default TRANSFER_SERVER
        if (kickEvent.getCustomAction() == PlayerAFKKickEvent.KickAction.KICK) {
            boolean stEnabled = plugin.getConfig().getBoolean("server-transfer.enabled", false);
            String target = plugin.getConfig().getString("server-transfer.target-server", "");
            if (stEnabled && target != null && !target.trim().isEmpty()) {
                kickEvent.setCustomAction(PlayerAFKKickEvent.KickAction.TRANSFER_SERVER);
                kickEvent.setCustomActionData(target);
            }
        }

        Bukkit.getPluginManager().callEvent(kickEvent);

        if (!kickEvent.isCancelled()) {
            // Handle different kick actions
            switch (kickEvent.getCustomAction()) {
                case KICK:
                    if (player.isOnline()) {
                        player.kickPlayer(kickEvent.getKickMessage());
                        AFKLogger.logAFKKick(player, afkTimeMillis / 1000L);
                    }
                    break;
                case MARK_AFK_ONLY:
                    // Just mark as AFK, don't kick
                    forceSetManualAFKState(player, true);
                    break;
                case TELEPORT:
                    // Teleport player instead of kicking
                    if (player.isOnline()) {
                        teleportPlayer(player, kickEvent.getCustomActionData());
                        AFKLogger.logActivity(player.getName() + " teleported instead of kicked (AFK)");
                    }
                    break;
                case TRANSFER_SERVER:
                    // Transferir a otro servidor con soporte de cuenta atrás (si está habilitado)
                    if (player.isOnline()) {
                        String target = kickEvent.getCustomActionData();

                        Runnable doTransfer = () -> {
                            boolean ok = false;
                            try {
                                if (plugin.getServerTransferService() != null) {
                                    ok = plugin.getServerTransferService().transferPlayer(player, target);
                                }
                            } catch (Exception e) {
                                ok = false;
                            }

                            if (!ok) {
                                // Fallback configurable: por defecto, KICK
                                String fb = plugin.getConfig().getString("server-transfer.fallback-action", "KICK");
                                if ("NONE".equalsIgnoreCase(fb)) {
                                    return;
                                } else if ("TELEPORT".equalsIgnoreCase(fb)) {
                                    teleportPlayer(player, plugin.getConfig().getString("server-transfer.fallback-teleport-location", ""));
                                } else {
                                    if (player.isOnline()) {
                                        player.kickPlayer(kickEvent.getKickMessage());
                                        AFKLogger.logAFKKick(player, afkTimeMillis / 1000L);
                                    }
                                }
                            }
                        };

                        boolean pipelineEnabled = plugin.getConfig().getBoolean("server-transfer.pipeline.enabled", false);
                        java.util.List<String> steps = plugin.getConfig().getStringList("server-transfer.actions");
                        boolean hasSteps = steps != null && !steps.isEmpty();
                        if (pipelineEnabled && hasSteps && plugin.getActionPipelineService() != null) {
                            String msg = plugin.getConfigManager().getMessage("server-transfer.transferring", "&aTransferring...");
                            player.sendMessage(msg.replace("{server}", target == null ? "" : target));
                            plugin.getActionPipelineService().startPipelineFromConfig(player, doTransfer);
                        } else {
                            boolean countdownEnabled = plugin.getConfig().getBoolean("server-transfer.countdown.enabled", false);
                            int cdSeconds = Math.max(0, plugin.getConfig().getInt("server-transfer.countdown.seconds", 10));
                            if (countdownEnabled && cdSeconds > 0 && plugin.getCountdownSequenceService() != null) {
                                String msg = plugin.getConfigManager().getMessage("server-transfer.transferring", "&aTransferring...");
                                player.sendMessage(msg.replace("{server}", target == null ? "" : target));
                                plugin.getCountdownSequenceService().startServerTransferCountdown(player, doTransfer);
                            } else {
                                // Transferencia inmediata en el hilo de la entidad
                                plugin.getPlatformScheduler().runTaskForEntity(player, doTransfer);
                            }
                        }
                    }
                    break;
                case GAMEMODE:
                    // Could implement gamemode change
                    break;
                case COMMAND:
                    // Could execute custom command
                    break;
                case NONE:
                    // Do nothing
                    break;
                default:
                    // Default kick behavior
                    if (player.isOnline()) {
                        player.kickPlayer(kickEvent.getKickMessage());
                        AFKLogger.logAFKKick(player, afkTimeMillis / 1000L);
                    }
                    break;
            }
        }
    }

    /**
     * Safely teleports a player to specified coordinates.
     * Format: "world,x,y,z" or "world,x,y,z,yaw,pitch"
     * 
     * @param player The player to teleport
     * @param locationString The location string from config (can be null)
     */
    private void teleportPlayer(Player player, String locationString) {
        if (player == null || !player.isOnline()) {
            AFKLogger.logActivity("Cannot teleport offline player");
            return;
        }

        // Use default spawn if no location specified
        if (locationString == null || locationString.trim().isEmpty()) {
            Location spawnLocation = player.getWorld().getSpawnLocation();
            player.teleport(spawnLocation);
            player.sendMessage("§7[AntiAFK+] §aTeleported to spawn due to AFK.");
            AFKLogger.logActivity(player.getName() + " teleported to world spawn (AFK)");
            return;
        }

        try {
            String[] parts = locationString.split(",");
            if (parts.length < 4) {
                AFKLogger.logActivity("Invalid teleport location format: " + locationString);
                return;
            }

            String worldName = parts[0].trim();
            World world = Bukkit.getWorld(worldName);
            
            if (world == null) {
                AFKLogger.logActivity("World not found: " + worldName + ". Using current world.");
                world = player.getWorld();
            }

            double x = Double.parseDouble(parts[1].trim());
            double y = Double.parseDouble(parts[2].trim());
            double z = Double.parseDouble(parts[3].trim());

            // Optional yaw and pitch
            float yaw = 0.0f;
            float pitch = 0.0f;
            if (parts.length >= 6) {
                yaw = Float.parseFloat(parts[4].trim());
                pitch = Float.parseFloat(parts[5].trim());
            }

            Location teleportLocation = new Location(world, x, y, z, yaw, pitch);
            
            // Safety check: ensure location is safe (not in void, etc.)
            if (y < 0) {
                teleportLocation.setY(world.getSpawnLocation().getY());
            }

            player.teleport(teleportLocation);
            player.sendMessage("§7[AntiAFK+] §aTeleported due to AFK timeout.");
            AFKLogger.logActivity(player.getName() + " teleported to " + worldName + " (" + x + "," + y + "," + z + ") due to AFK");

        } catch (NumberFormatException e) {
            AFKLogger.logActivity("Invalid coordinates in teleport location: " + locationString);
            // Fallback to spawn
            Location spawnLocation = player.getWorld().getSpawnLocation();
            player.teleport(spawnLocation);
            player.sendMessage("§7[AntiAFK+] §aTeleported to spawn due to AFK.");
        } catch (Exception e) {
            AFKLogger.logActivity("Error teleporting player " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Internal method to mark a player as AFK. Handles adding to sets and broadcasting.
     * @param player The player.
     * @param reason The reason (e.g., "auto", "manual (command)", "manual (API)").
     * @param detectionMethod The detection method for event firing.
     */
    private void markAsAFKInternal(Player player, String reason, String detectionMethod) {
        UUID uuid = player.getUniqueId();
        boolean wasAlreadyAFK = afkPlayers.contains(uuid);
        boolean wasManualAFK = manualAfkUsernames.contains(uuid);

        if (wasAlreadyAFK && !reason.contains("manual")) {
            return; // Already AFK with same type
        }

        PlayerAFKStateChangeEvent.AFKState fromState = wasManualAFK ?
                PlayerAFKStateChangeEvent.AFKState.AFK_MANUAL :
                (wasAlreadyAFK ? PlayerAFKStateChangeEvent.AFKState.AFK_AUTO : PlayerAFKStateChangeEvent.AFKState.ACTIVE);

        PlayerAFKStateChangeEvent.AFKState toState = reason.contains("manual") ?
                PlayerAFKStateChangeEvent.AFKState.AFK_MANUAL :
                PlayerAFKStateChangeEvent.AFKState.AFK_AUTO;

        PlayerAFKStateChangeEvent.AFKReason eventReason = reason.contains("manual") ?
                PlayerAFKStateChangeEvent.AFKReason.MANUAL_TOGGLE :
                PlayerAFKStateChangeEvent.AFKReason.INACTIVITY_TIMEOUT;

        // Fire state change event
        PlayerAFKStateChangeEvent stateEvent = fireAFKStateChangeEvent(player, fromState, toState,
                eventReason, detectionMethod, reason.contains("manual"));

        if (stateEvent.isCancelled()) {
            return; // Event was cancelled, don't proceed
        }

        afkPlayers.add(uuid);
        warningsSent.remove(uuid);

        String afkBroadcastMessage = plugin.getConfigManager().getMessagePlayerNowAFK()
                .replace("{player}", player.getName());

        if(plugin.getConfigManager().shouldBroadcastAFKStateChanges()){
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.sendMessage(afkBroadcastMessage);
            }
        }
        AFKLogger.logAFKEnter(player, reason);
    }

    /**
     * Internal method to unmark a player from AFK. Handles removing from sets and broadcasting.
     * @param player The player.
     */
    private void unmarkAsAFKInternal(Player player) {
        UUID uuid = player.getUniqueId();
        if (!afkPlayers.remove(uuid)) {
            return; // Player was not in the general AFK set.
        }

        warningsSent.remove(uuid);

        // Cancelar cualquier cuenta atrás en curso (Fase 2) y pipelines (Fase 4)
        if (plugin.getCountdownSequenceService() != null) {
            plugin.getCountdownSequenceService().cancel(player);
        }
        if (plugin.getActionPipelineService() != null) {
            plugin.getActionPipelineService().cancel(player);
        }

        String notAfkBroadcastMessage = plugin.getConfigManager().getMessagePlayerNoLongerAFK()
                .replace("{player}", player.getName());

        if(plugin.getConfigManager().shouldBroadcastAFKStateChanges()){
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.sendMessage(notAfkBroadcastMessage);
            }
        }
    }

    private PlayerAFKStateChangeEvent fireAFKStateChangeEvent(Player player,
                                                              PlayerAFKStateChangeEvent.AFKState fromState,
                                                              PlayerAFKStateChangeEvent.AFKState toState,
                                                              PlayerAFKStateChangeEvent.AFKReason reason,
                                                              String detectionMethod, boolean wasManual) {
        PlayerActivityData activityData = playerActivityData.get(player.getUniqueId());
        double activityScore = activityData != null ? activityData.activityScore : 0.0;

        PlayerAFKStateChangeEvent event = new PlayerAFKStateChangeEvent(
                player, fromState, toState, reason, detectionMethod, activityScore, wasManual
        );

        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    public boolean isAFK(Player player) {
        if (player == null) return false;
        return afkPlayers.contains(player.getUniqueId());
    }

    /**
     * Checks if a player is specifically in manual AFK mode.
     * @param player The player to check.
     * @return True if the player is in manual AFK mode, false otherwise.
     */
    public boolean isManuallyAFK(Player player) {
        if (player == null) return false;
        return manualAfkUsernames.contains(player.getUniqueId());
    }

    public boolean toggleManualAFK(Player player) {
        UUID uuid = player.getUniqueId();
        if (manualAfkUsernames.contains(uuid)) { // Is manually AFK, turn it off
            forceSetManualAFKState(player, false);
            return false;
        } else { // Not manually AFK (or not AFK at all), turn it on
            if (isAFK(player) && !manualAfkUsernames.contains(uuid)) { // Is AFK (auto) but not manually
                player.sendMessage(plugin.getConfigManager().getMessageAlreadyAFK() + " (Switching to manual AFK mode).");
            } else if (isAFK(player)) { // Already manually AFK (should have been caught by first if, but defensive)
                player.sendMessage(plugin.getConfigManager().getMessageAlreadyAFK());
            }
            forceSetManualAFKState(player, true);
            return true;
        }
    }

    /**
     * Forces a player's manual AFK state.
     * @param player The player.
     * @param setAfk True to set as manually AFK, false to remove from AFK.
     */
    public void forceSetManualAFKState(Player player, boolean setAfk) {
        UUID uuid = player.getUniqueId();
        String apiReason = setAfk ? "manual (API)" : "manual (API activity/unmark)";

        if (setAfk) {
            boolean wasAlreadyManuallyAfk = manualAfkUsernames.contains(uuid);
            manualAfkUsernames.add(uuid);
            if (!manualAfkStartTimes.containsKey(uuid)) {
                manualAfkStartTimes.put(uuid, System.currentTimeMillis());
            }
            markAsAFKInternal(player, wasAlreadyManuallyAfk ? "manual (API - refresh)" : apiReason, "api_call");
        } else {
            boolean wasManuallyAfk = manualAfkUsernames.remove(uuid);
            manualAfkStartTimes.remove(uuid);
            boolean wasGenerallyAfk = afkPlayers.contains(uuid);

            if (wasGenerallyAfk || wasManuallyAfk) {
                PlayerAFKStateChangeEvent.AFKState fromState = wasManuallyAfk ?
                        PlayerAFKStateChangeEvent.AFKState.AFK_MANUAL :
                        PlayerAFKStateChangeEvent.AFKState.AFK_AUTO;

                fireAFKStateChangeEvent(player, fromState, PlayerAFKStateChangeEvent.AFKState.ACTIVE,
                        PlayerAFKStateChangeEvent.AFKReason.API_CALL, "api_call", false);
            }

            unmarkAsAFKInternal(player);

            // Professional fix: Clear actioned state when manually unmarked
            playersAlreadyActioned.remove(uuid);

            if (wasManuallyAfk) {
                AFKLogger.logAFKExit(player, apiReason, -1);
            } else if (wasGenerallyAfk) {
                AFKLogger.logAFKExit(player, "auto (API unmark)", -1);
            }
        }
    }

    public void forceSetAutoAFKState(Player player, boolean setAfk, String detectionMethod) {
        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        String method = detectionMethod != null ? detectionMethod : "api_call";

        if (setAfk) {
            if (manualAfkUsernames.contains(uuid)) {
                forceSetManualAFKState(player, false);
            }

            boolean wasAuto = afkPlayers.contains(uuid) && !manualAfkUsernames.contains(uuid);
            if (!wasAuto) {
                markAsAFKInternal(player, "auto (API)", method);
            }

            if (afkPlayers.contains(uuid)) {
                afkDetectionTimes.put(uuid, System.currentTimeMillis());
                afkDetectionReasons.put(uuid, method);
            }
        } else {
            if (afkPlayers.contains(uuid) && !manualAfkUsernames.contains(uuid)) {
                fireAFKStateChangeEvent(player, PlayerAFKStateChangeEvent.AFKState.AFK_AUTO,
                        PlayerAFKStateChangeEvent.AFKState.ACTIVE,
                        PlayerAFKStateChangeEvent.AFKReason.API_CALL, method, false);
                unmarkAsAFKInternal(player);
                afkDetectionTimes.remove(uuid);
                afkDetectionReasons.remove(uuid);
                playersAlreadyActioned.remove(uuid);
            }
        }
    }

    public void forceSetActive(Player player, PlayerAFKStateChangeEvent.AFKReason reason, String detectionMethod) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        PlayerAFKStateChangeEvent.AFKReason finalReason = reason != null ? reason : PlayerAFKStateChangeEvent.AFKReason.API_CALL;
        String method = detectionMethod != null ? detectionMethod : "api_call";

        if (manualAfkUsernames.contains(uuid)) {
            forceSetManualAFKState(player, false);
        }

        if (afkPlayers.contains(uuid) && !manualAfkUsernames.contains(uuid)) {
            fireAFKStateChangeEvent(player, PlayerAFKStateChangeEvent.AFKState.AFK_AUTO,
                    PlayerAFKStateChangeEvent.AFKState.ACTIVE,
                    finalReason, method, false);
            unmarkAsAFKInternal(player);
            afkDetectionTimes.remove(uuid);
            afkDetectionReasons.remove(uuid);
            playersAlreadyActioned.remove(uuid);
        }
    }

    public boolean isAutoAFK(Player player) {
        if (player == null) return false;
        UUID uuid = player.getUniqueId();
        return afkPlayers.contains(uuid) && !manualAfkUsernames.contains(uuid);
    }

    public Long getManualAFKStartTime(Player player) {
        if (player == null) return null;
        return manualAfkStartTimes.get(player.getUniqueId());
    }

    public Set<UUID> getAfkPlayerUUIDs() {
        return new HashSet<>(afkPlayers);
    }

    public Map<UUID, Long> getAfkDetectionTimesSnapshot() {
        return new HashMap<>(afkDetectionTimes);
    }

    public Map<UUID, String> getAfkDetectionReasonsSnapshot() {
        return new HashMap<>(afkDetectionReasons);
    }

    public Map<UUID, Long> getManualAfkStartTimesSnapshot() {
        return new HashMap<>(manualAfkStartTimes);
    }

    public void onPlayerActivity(Player player) {
        onPlayerActivity(player, ActivityType.UNKNOWN);
    }

    public void onPlayerActivity(Player player, ActivityType activityType) {
        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        boolean wasManuallyAfk = manualAfkUsernames.contains(uuid);

        if (wasManuallyAfk) {
            forceSetManualAFKState(player, false);
        }

        // Professional fix: Clear actioned state when player shows activity
        playersAlreadyActioned.remove(uuid);

        // Update activity tracking
        recordPlayerActivity(player, activityType, System.currentTimeMillis());

        if (plugin.getAPI() instanceof AntiAFKPlusAPIImpl apiImpl) {
            apiImpl.handleInternalActivity(player, activityType != null ? activityType : ActivityType.UNKNOWN);
        }
    }

    public void recordCustomActivity(Player player, ActivityType activityType) {
        if (player == null || activityType == null) {
            return;
        }
        recordPlayerActivity(player, activityType, System.currentTimeMillis());
    }

    public void clearPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        afkPlayers.remove(uuid);
        manualAfkUsernames.remove(uuid);
        manualAfkStartTimes.remove(uuid);
        warningsSent.remove(uuid);
        afkDetectionTimes.remove(uuid);
        afkDetectionReasons.remove(uuid);
        playerActivityData.remove(uuid);
        warningCounts.remove(uuid);
        windowMessageCooldown.remove(uuid);
        
        // Professional fix: Clear actioned state when player data is cleared
        playersAlreadyActioned.remove(uuid);

        // Clear pattern detector data
        patternDetector.clearPlayerData(player);

        AFKLogger.logActivity(player.getName() + "'s AFK data cleared.");
    }

    public long getLastMovementTimestampForPlayer(Player player) {
        if (player == null || movementListener == null) return System.currentTimeMillis();
        return movementListener.getLastMovementTimestamp(player);
    }

    // Enhanced getters for v2.0

    public String getAFKDetectionReason(Player player) {
        return afkDetectionReasons.get(player.getUniqueId());
    }

    public long getAFKDetectionTime(Player player) {
        return afkDetectionTimes.getOrDefault(player.getUniqueId(), 0L);
    }

    public PlayerActivityData getPlayerActivityData(Player player) {
        return playerActivityData.get(player.getUniqueId());
    }

    public PatternDetector getPatternDetector() {
        return patternDetector;
    }

    /**
     * PROFESSIONAL FIX: Public method to execute AFK action (kick/teleport) from external sources.
     * This allows PatternDetector to properly execute configured actions instead of just marking AFK.
     *
     * @param player The player to apply the action to
     * @param detectionReason The reason for triggering the action (e.g., "pattern_violation")
     */
    public void executeAFKAction(Player player, String detectionReason) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();

        // Mark as AFK first
        if (!afkPlayers.contains(uuid)) {
            markAsAFKInternal(player, "auto (" + detectionReason + ")", detectionReason);
            afkDetectionTimes.put(uuid, System.currentTimeMillis());
            afkDetectionReasons.put(uuid, detectionReason);
        }

        // Execute the configured action (kick/teleport/etc.) only once
        if (!playersAlreadyActioned.contains(uuid)) {
            long timeSinceDetection = System.currentTimeMillis() - afkDetectionTimes.getOrDefault(uuid, System.currentTimeMillis());
            kickPlayerAfterAFK(player, timeSinceDetection);
            playersAlreadyActioned.add(uuid);
        }
    }

    public void shutdown() {
        plugin.getLogger().info("Shutting down Enhanced AFKManager v2.0...");

        if (this.afkCheckTask != null && !this.afkCheckTask.isCancelled()) {
            this.afkCheckTask.cancel();
            this.afkCheckTask = null;
            plugin.getLogger().info("AFK check task successfully cancelled.");
        }

        if (this.patternDetector != null) {
            this.patternDetector.shutdown();
        }

        plugin.getLogger().info("Enhanced AFKManager v2.0 shutdown complete.");
    }

    /**
     * Determines which AFK zone the player is currently in.
     * For now, assumes all players are in the "spawn" zone.
     * In a full implementation, this would check player coordinates against zone boundaries.
     */
    private String determinePlayerZone(Player player) {
        // Prefer WorldGuard integration when available
        var wg = plugin.getWorldGuardIntegration();
        if (wg != null && wg.isAvailable() && plugin.getConfig().getBoolean("zone-management.enabled", false)) {
            String z = wg.determineZoneAt(player != null ? player.getLocation() : null);
            if (z != null) return z;
        }
        // Fallback: spawn zone if configured
        if (plugin.getConfig().contains("zone-management.zones.spawn")) return "spawn";
        return null;
    }

    // Inner class for enhanced activity tracking
    public static class PlayerActivityData {
        private final Deque<ActivityEntry> activityHistory = new ArrayDeque<>();
        private final EnumMap<ActivityType, Long> lastActivityTimes = new EnumMap<>(ActivityType.class);
        private long lastActivityTimestamp = 0L;
        private ActivityType lastActivityType = ActivityType.UNKNOWN;
        private double activityScore = 0.0;

        public synchronized void recordActivity(ActivityType type, long timestamp) {
            activityHistory.addLast(new ActivityEntry(type, timestamp));
            while (activityHistory.size() > MAX_STORED_ACTIVITIES) {
                activityHistory.removeFirst();
            }
            lastActivityTimes.put(type, timestamp);
            lastActivityTimestamp = timestamp;
            lastActivityType = type;
            trimHistory(timestamp);
            recalculateScore(timestamp);
        }

        public synchronized void refresh() {
            long now = System.currentTimeMillis();
            trimHistory(now);
            recalculateScore(now);
        }

        private void trimHistory(long currentTime) {
            while (!activityHistory.isEmpty() &&
                    currentTime - activityHistory.peekFirst().timestamp > ACTIVITY_HISTORY_WINDOW_MS) {
                activityHistory.removeFirst();
            }
        }

        private void recalculateScore(long currentTime) {
            double score = 0.0;
            for (ActivityEntry entry : activityHistory) {
                if (currentTime - entry.timestamp <= ACTIVITY_SCORE_WINDOW_MS) {
                    score += entry.type.getActivityWeight();
                }
            }
            activityScore = Math.min(100.0, score * 5.0); // Normalize to 0-100 range
        }

        public synchronized Map<ActivityType, Integer> getActivityCounts(long windowMs) {
            long cutoff = System.currentTimeMillis() - windowMs;
            EnumMap<ActivityType, Integer> counts = new EnumMap<>(ActivityType.class);
            for (ActivityEntry entry : activityHistory) {
                if (entry.timestamp >= cutoff) {
                    counts.merge(entry.type, 1, Integer::sum);
                }
            }
            return counts;
        }

        public synchronized Map<ActivityType, Long> getLastActivityTimes() {
            return new EnumMap<>(lastActivityTimes);
        }

        public synchronized int getTotalActivities(long windowMs) {
            long cutoff = System.currentTimeMillis() - windowMs;
            int total = 0;
            for (ActivityEntry entry : activityHistory) {
                if (entry.timestamp >= cutoff) {
                    total++;
                }
            }
            return total;
        }

        public synchronized double getActivityScore() {
            return activityScore;
        }

        public synchronized long getLastActivityTimestamp() {
            return lastActivityTimestamp;
        }

        public synchronized ActivityType getLastActivityType() {
            return lastActivityType;
        }

        public synchronized boolean isHighActivity() {
            return activityScore >= 50.0;
        }

        public synchronized boolean isLowActivity() {
            return activityScore <= 10.0;
        }

        private static class ActivityEntry {
            private final ActivityType type;
            private final long timestamp;

            private ActivityEntry(ActivityType type, long timestamp) {
                this.type = type;
                this.timestamp = timestamp;
            }
        }
    }
}
