// AFKManager.java - Enhanced v2.0 with Full Event Integration
package me.koyere.antiafkplus.afk;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.events.PlayerAFKKickEvent;
import me.koyere.antiafkplus.events.PlayerAFKStateChangeEvent;
import me.koyere.antiafkplus.events.PlayerAFKWarningEvent;
import me.koyere.antiafkplus.utils.AFKLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import me.koyere.antiafkplus.platform.PlatformScheduler;

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

                    if (disabledWorlds.contains(currentWorldName)) continue;
                    if (!enabledWorlds.isEmpty() && !enabledWorlds.contains(currentWorldName)) continue;

                    // Update player activity data
                    updatePlayerActivityData(player);

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
                            
                            // Only take action if player has been AFK long enough (prevents immediate actions)
                            if (timeSinceDetection >= 1000) { // At least 1 second delay for safety
                                kickPlayerAfterAFK(player, timeSinceDetection);
                                playersAlreadyActioned.add(uuid); // Mark as actioned to prevent repeated calls
                            }
                        }
                    } else {
                        checkWarnings(player);
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

    private void updatePlayerActivityData(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerActivityData data = playerActivityData.computeIfAbsent(uuid, k -> new PlayerActivityData());

        long currentTime = System.currentTimeMillis();
        data.lastUpdate = currentTime;

        // Update activity counters
        long lastMovement = movementListener.getLastMovementTimestamp(player);
        long lastHeadRotation = movementListener.getLastHeadRotationTime(player);
        long lastJump = movementListener.getLastJumpTime(player);
        long lastCommand = movementListener.getLastCommandTime(player);

        // Count recent activities (last 5 minutes)
        long fiveMinutesAgo = currentTime - 300000;

        if (lastMovement > fiveMinutesAgo) data.recentMovements++;
        if (lastHeadRotation > fiveMinutesAgo) data.recentHeadRotations++;
        if (lastJump > fiveMinutesAgo) data.recentJumps++;
        if (lastCommand > fiveMinutesAgo) data.recentCommands++;

        // Reset counters periodically
        if (currentTime - data.lastReset > 300000) { // Every 5 minutes
            data.recentMovements = 0;
            data.recentHeadRotations = 0;
            data.recentJumps = 0;
            data.recentCommands = 0;
            data.lastReset = currentTime;
        }

        // Calculate activity score (higher = more active)
        data.activityScore = calculateActivityScore(data);
    }

    private double calculateActivityScore(PlayerActivityData data) {
        // Weighted activity score based on different types of activity
        double score = 0.0;

        score += data.recentMovements * 1.0;      // Base movement
        score += data.recentHeadRotations * 1.5;  // Head rotation is more intentional
        score += data.recentJumps * 0.8;          // Jumps can be automated
        score += data.recentCommands * 2.0;       // Commands are highly intentional

        return Math.min(score, 100.0); // Cap at 100
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

        // Check if zone management is enabled and configure teleportation
        if (plugin.getConfig().getBoolean("zone-management.enabled", false)) {
            // Check if player is in spawn zone (or any configured zone)
            String zoneName = determinePlayerZone(player);
            if (zoneName != null && plugin.getConfig().contains("zone-management.zones." + zoneName)) {
                String action = plugin.getConfig().getString("zone-management.zones." + zoneName + ".kick-action", "kick");
                if ("TELEPORT".equalsIgnoreCase(action)) {
                    kickEvent.setCustomAction(PlayerAFKKickEvent.KickAction.TELEPORT);
                    String teleportLocation = plugin.getConfig().getString("zone-management.zones." + zoneName + ".teleport-location", "");
                    kickEvent.setCustomActionData(teleportLocation);
                }
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

    public void onPlayerActivity(Player player) {
        UUID uuid = player.getUniqueId();
        boolean wasManuallyAfk = manualAfkUsernames.contains(uuid);

        if (wasManuallyAfk) {
            forceSetManualAFKState(player, false);
        }

        // Professional fix: Clear actioned state when player shows activity
        playersAlreadyActioned.remove(uuid);

        // Update activity tracking
        updatePlayerActivityData(player);
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
            String z = wg.determineZoneAt(player);
            if (z != null) return z;
        }
        // Fallback: spawn zone if configured
        if (plugin.getConfig().contains("zone-management.zones.spawn")) return "spawn";
        return null;
    }

    // Inner class for enhanced activity tracking
    public static class PlayerActivityData {
        public int recentMovements = 0;
        public int recentHeadRotations = 0;
        public int recentJumps = 0;
        public int recentCommands = 0;
        public double activityScore = 0.0;
        public long lastUpdate = System.currentTimeMillis();
        public long lastReset = System.currentTimeMillis();

        public boolean isHighActivity() {
            return activityScore > 50.0;
        }

        public boolean isLowActivity() {
            return activityScore < 10.0;
        }
    }
}
