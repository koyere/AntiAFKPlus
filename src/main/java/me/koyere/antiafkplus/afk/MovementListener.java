package me.koyere.antiafkplus.afk;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.api.data.ActivityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MovementListener implements Listener {

    private final Map<UUID, Long> lastMovementTime = new HashMap<>();

    // Enhanced detection data structures
    private final Map<UUID, PlayerLocationData> lastLocationData = new HashMap<>();
    private final Map<UUID, Long> lastHeadRotationTime = new HashMap<>();
    private final Map<UUID, Long> lastJumpTime = new HashMap<>();
    private final Map<UUID, Integer> jumpCounter = new HashMap<>();
    private final Map<UUID, Long> lastSwimStateChange = new HashMap<>();
    private final Map<UUID, Long> lastFlyStateChange = new HashMap<>();
    private final Map<UUID, Long> lastCommandTime = new HashMap<>();
    
    // v2.4 NEW: Keystroke detection for large AFK pools
    private final Map<UUID, Long> lastKeystrokeTime = new HashMap<>();
    private final Map<UUID, Boolean> lastMovementWasAutomatic = new HashMap<>();

    // Configuration thresholds for micro-movement detection
    private static final double MICRO_MOVEMENT_THRESHOLD = 0.1; // Minimum movement to count as activity
    private static final double HEAD_ROTATION_THRESHOLD = 5.0; // Minimum degrees for head rotation
    private static final long JUMP_SPAM_THRESHOLD = 1000; // Milliseconds between jumps to detect spam
    private static final int MAX_JUMPS_PER_PERIOD = 10; // Maximum jumps in spam detection period
    private static final long JUMP_RESET_PERIOD = 30000; // Reset jump counter every 30 seconds
    
    // v2.4 NEW: Keystroke timeout detection constants
    private static final long DEFAULT_KEYSTROKE_TIMEOUT_MS = 180000; // 3 minutes without keystrokes
    private static final double AUTOMATIC_MOVEMENT_VELOCITY_THRESHOLD = 0.15; // Water current movement speed

    // Constructor does not need AFKManager if it gets it via AntiAFKPlus.getInstance()
    public MovementListener() {}

    private AFKManager getAfkManager() {
        // Helper method to reduce verbosity
        AntiAFKPlus plugin = AntiAFKPlus.getInstance();
        if (plugin == null) {
            // This should ideally not happen if the plugin is enabled and listener is registered
            throw new IllegalStateException("AntiAFKPlus instance is null. Cannot get AFKManager.");
        }
        return plugin.getAfkManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (AntiAFKPlus.getInstance() != null && AntiAFKPlus.getInstance().getAfkManager() != null) {
            getAfkManager().clearPlayerData(player);
        }
        initializePlayerData(player);
        updateLastMovementTimestamp(player);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("antiafkplus.bypass")) return;

        // Enhanced movement detection
        boolean significantMovement = detectSignificantMovement(event);
        boolean headRotation = detectHeadRotation(player, event);
        boolean jumpActivity = detectJumpActivity(player, event);
        boolean swimStateChange = detectSwimStateChange(player);
        boolean flyStateChange = detectFlyStateChange(player);
        
        // v2.4 NEW: Detect if movement is from manual keystroke or automatic (water current)
        boolean isManualKeystroke = detectManualKeystroke(player, event);

        // Update location data for pattern detection
        updatePlayerLocationData(player, event);

        boolean recorded = false;
        AFKManager manager = AntiAFKPlus.getInstance() != null ? AntiAFKPlus.getInstance().getAfkManager() : null;

        if (manager != null) {
            long now = System.currentTimeMillis();
            if (significantMovement) {
                manager.onPlayerActivity(player, ActivityType.MOVEMENT);
                recorded = true;
            }
            if (headRotation) {
                manager.onPlayerActivity(player, ActivityType.HEAD_ROTATION);
                lastHeadRotationTime.put(player.getUniqueId(), now);
                recorded = true;
            }
            if (jumpActivity) {
                manager.onPlayerActivity(player, ActivityType.JUMP);
                lastJumpTime.put(player.getUniqueId(), now);
                recorded = true;
            }
            if (swimStateChange) {
                manager.onPlayerActivity(player, ActivityType.MOVEMENT);
                lastSwimStateChange.put(player.getUniqueId(), now);
                recorded = true;
            }
            if (flyStateChange) {
                manager.onPlayerActivity(player, ActivityType.MOVEMENT);
                lastFlyStateChange.put(player.getUniqueId(), now);
                recorded = true;
            }
            if (!recorded && isManualKeystroke) {
                manager.onPlayerActivity(player, ActivityType.CUSTOM);
                updateLastKeystrokeTime(player);
                recorded = true;
            } else if (recorded && isManualKeystroke) {
                updateLastKeystrokeTime(player);
            }
        }

        if (recorded) {
            updateLastMovementTimestamp(player);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        AntiAFKPlus plugin = AntiAFKPlus.getInstance();
        if (plugin == null || !plugin.isEnabled()) return;

        plugin.getPlatformScheduler().runTaskForEntity(player, () -> {
            if (player.hasPermission("antiafkplus.bypass")) return;
            if (plugin.getAfkManager() != null) {
                plugin.getAfkManager().onPlayerActivity(player, ActivityType.CHAT);
            }
            updateLastMovementTimestamp(player);
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (player.hasPermission("antiafkplus.bypass")) return;
            if (AntiAFKPlus.getInstance() != null && AntiAFKPlus.getInstance().getAfkManager() != null) {
                getAfkManager().onPlayerActivity(player, ActivityType.INVENTORY);
            }
            updateLastMovementTimestamp(player);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("antiafkplus.bypass")) return;
        if (AntiAFKPlus.getInstance() != null && AntiAFKPlus.getInstance().getAfkManager() != null) {
            getAfkManager().onPlayerActivity(player, ActivityType.INTERACTION);
        }
        updateLastMovementTimestamp(player);
    }

    // New event handler for command activity tracking
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("antiafkplus.bypass")) return;

        // Filter out AFK-related commands to prevent exploitation
        String command = event.getMessage().toLowerCase();
        if (command.startsWith("/afk") || command.startsWith("/afkplus")) {
            return; // Don't count AFK commands as activity
        }

        lastCommandTime.put(player.getUniqueId(), System.currentTimeMillis());

        if (AntiAFKPlus.getInstance() != null && AntiAFKPlus.getInstance().getAfkManager() != null) {
            getAfkManager().onPlayerActivity(player, ActivityType.COMMAND);
        }
        updateLastMovementTimestamp(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (AntiAFKPlus.getInstance() != null && AntiAFKPlus.getInstance().getAfkManager() != null) {
            getAfkManager().clearPlayerData(event.getPlayer());
        }
        clearPlayerData(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        if (event.isCancelled()) return;
        if (AntiAFKPlus.getInstance() != null && AntiAFKPlus.getInstance().getAfkManager() != null) {
            getAfkManager().clearPlayerData(event.getPlayer());
        }
        clearPlayerData(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("antiafkplus.bypass")) return;
        
        // Only count successful fishing attempts (catching fish, items, or pulling in rod)
        PlayerFishEvent.State state = event.getState();
        if (state == PlayerFishEvent.State.CAUGHT_FISH || 
            state == PlayerFishEvent.State.CAUGHT_ENTITY || 
            state == PlayerFishEvent.State.IN_GROUND ||
            state == PlayerFishEvent.State.REEL_IN) {
            
            if (AntiAFKPlus.getInstance() != null && AntiAFKPlus.getInstance().getAfkManager() != null) {
                getAfkManager().onPlayerActivity(player, ActivityType.FISHING);
            }
            updateLastMovementTimestamp(player);
        }
    }

    // Enhanced detection methods

    private boolean detectSignificantMovement(PlayerMoveEvent event) {
        if (event.getTo() == null) return false;

        double deltaX = Math.abs(event.getTo().getX() - event.getFrom().getX());
        double deltaY = Math.abs(event.getTo().getY() - event.getFrom().getY());
        double deltaZ = Math.abs(event.getTo().getZ() - event.getFrom().getZ());

        // Check for micro-movement threshold
        return deltaX > MICRO_MOVEMENT_THRESHOLD ||
                deltaY > MICRO_MOVEMENT_THRESHOLD ||
                deltaZ > MICRO_MOVEMENT_THRESHOLD;
    }

    private boolean detectHeadRotation(Player player, PlayerMoveEvent event) {
        if (event.getTo() == null) return false;

        double deltaYaw = Math.abs(event.getTo().getYaw() - event.getFrom().getYaw());
        double deltaPitch = Math.abs(event.getTo().getPitch() - event.getFrom().getPitch());

        // Normalize yaw difference (handle 360 degree wrap-around)
        if (deltaYaw > 180) {
            deltaYaw = 360 - deltaYaw;
        }

        return deltaYaw > HEAD_ROTATION_THRESHOLD || deltaPitch > HEAD_ROTATION_THRESHOLD;
    }

    private boolean detectJumpActivity(Player player, PlayerMoveEvent event) {
        if (event.getTo() == null) return false;

        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Check if player is jumping (Y velocity > 0 and not flying)
        boolean isJumping = event.getTo().getY() > event.getFrom().getY() &&
                !player.isFlying() &&
                player.isOnGround();

        if (isJumping) {
            Long lastJump = lastJumpTime.get(uuid);
            if (lastJump == null || (currentTime - lastJump) > JUMP_SPAM_THRESHOLD) {
                // Reset jump counter if enough time has passed
                if (lastJump == null || (currentTime - lastJump) > JUMP_RESET_PERIOD) {
                    jumpCounter.put(uuid, 1);
                } else {
                    int jumps = jumpCounter.getOrDefault(uuid, 0) + 1;
                    jumpCounter.put(uuid, jumps);

                    // If too many jumps in short period, might be AFK farm
                    if (jumps > MAX_JUMPS_PER_PERIOD) {
                        // Log suspicious jump activity
                        AntiAFKPlus plugin = AntiAFKPlus.getInstance();
                        if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().warning("[DEBUG_Jump] Suspicious jump activity detected for " +
                                    player.getName() + ": " + jumps + " jumps in short period");
                        }
                        return false; // Don't count excessive jumping as legitimate activity
                    }
                }
                return true;
            }
        }

        return false;
    }

    private boolean detectSwimStateChange(Player player) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        boolean isSwimming = player.isSwimming();

        PlayerLocationData data = lastLocationData.get(uuid);
        if (data != null && data.wasSwimming != isSwimming) {
            lastSwimStateChange.put(uuid, currentTime);
            data.wasSwimming = isSwimming;
            return true;
        } else if (data != null) {
            data.wasSwimming = isSwimming;
        }

        return false;
    }
    
    // v2.4 NEW: Manual keystroke detection method
    
    /**
     * Determines if a player's movement appears to be from manual keystroke input
     * rather than automatic movement (like water currents in large AFK pools).
     * 
     * This uses velocity analysis and movement patterns to distinguish between:
     * - Manual WASD key movement (irregular, directional changes)
     * - Automatic water current movement (consistent velocity, predictable direction)
     * 
     * @param player The player being analyzed
     * @param event The movement event
     * @return true if movement appears to be from manual input
     */
    private boolean detectManualKeystroke(Player player, PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom() == null) {
            return false;
        }
        
        UUID uuid = player.getUniqueId();
        
        // Calculate movement vector and velocity
        double deltaX = event.getTo().getX() - event.getFrom().getX();
        double deltaZ = event.getTo().getZ() - event.getFrom().getZ();
        double horizontalVelocity = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        
        // Check if player is in water (automatic movement is more likely)
        boolean inWater = player.getLocation().getBlock().isLiquid() || 
                         player.getEyeLocation().getBlock().isLiquid();
        
        // Get previous movement data for pattern analysis
        PlayerLocationData locationData = lastLocationData.get(uuid);
        if (locationData == null || locationData.locationHistory.isEmpty()) {
            // No previous data, assume manual for now
            lastMovementWasAutomatic.put(uuid, false);
            return true;
        }
        
        // Analyze movement characteristics
        boolean appearsManual = false;
        
        // 1. Velocity-based detection
        if (inWater) {
            // In water: manual movement typically has higher velocity or direction changes
            if (horizontalVelocity > AUTOMATIC_MOVEMENT_VELOCITY_THRESHOLD) {
                appearsManual = true; // Fast movement in water suggests swimming
            }
        } else {
            // On land: any significant movement is likely manual
            if (horizontalVelocity > MICRO_MOVEMENT_THRESHOLD) {
                appearsManual = true;
            }
        }
        
        // 2. Direction change analysis (manual movement has more direction changes)
        if (locationData.locationHistory.size() >= 3) {
            List<LocationSnapshot> recent = locationData.locationHistory.subList(
                    Math.max(0, locationData.locationHistory.size() - 3),
                    locationData.locationHistory.size()
            );
            
            double directionChanges = calculateDirectionChanges(recent);
            if (directionChanges > 0.5) { // Threshold for direction variability
                appearsManual = true;
            }
        }
        
        // 3. Check for head rotation (strong indicator of manual control)
        if (detectHeadRotation(player, event)) {
            appearsManual = true;
        }
        
        // 4. Context-based adjustments
        Boolean wasAutomatic = lastMovementWasAutomatic.get(uuid);
        if (wasAutomatic != null && wasAutomatic && !appearsManual) {
            // If previous movement was automatic and current shows no manual signs,
            // likely still automatic (water current)
            appearsManual = false;
        }
        
        // Store result for next analysis
        lastMovementWasAutomatic.put(uuid, !appearsManual);
        
        // Debug logging if enabled
        AntiAFKPlus plugin = AntiAFKPlus.getInstance();
        if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
            if (horizontalVelocity > 0.05) { // Only log if there's significant movement
                plugin.getLogger().info(String.format(
                    "[DEBUG_Keystroke] %s: velocity=%.3f, inWater=%s, manual=%s",
                    player.getName(), horizontalVelocity, inWater, appearsManual
                ));
            }
        }
        
        return appearsManual;
    }
    
    /**
     * Calculates direction change variance in recent movement history.
     * Higher values indicate more erratic/manual movement patterns.
     * 
     * @param recentHistory List of recent location snapshots
     * @return Direction change metric (0.0 to 1.0+)
     */
    private double calculateDirectionChanges(List<LocationSnapshot> recentHistory) {
        if (recentHistory.size() < 3) return 0.0;
        
        double totalAngleChange = 0.0;
        double previousAngle = Double.NaN;
        
        for (int i = 1; i < recentHistory.size(); i++) {
            LocationSnapshot prev = recentHistory.get(i - 1);
            LocationSnapshot curr = recentHistory.get(i);
            
            double deltaX = curr.x - prev.x;
            double deltaZ = curr.z - prev.z;
            
            if (Math.abs(deltaX) > 0.01 || Math.abs(deltaZ) > 0.01) { // Only if there's movement
                double currentAngle = Math.atan2(deltaZ, deltaX);
                
                if (!Double.isNaN(previousAngle)) {
                    double angleDiff = Math.abs(currentAngle - previousAngle);
                    if (angleDiff > Math.PI) {
                        angleDiff = 2 * Math.PI - angleDiff; // Normalize
                    }
                    totalAngleChange += angleDiff;
                }
                
                previousAngle = currentAngle;
            }
        }
        
        return totalAngleChange / Math.PI; // Normalize to roughly 0-1 range
    }
    
    /**
     * Updates the last keystroke timestamp for a player.
     * Called when we detect what appears to be manual input.
     * 
     * @param player The player who provided manual input
     */
    private void updateLastKeystrokeTime(Player player) {
        lastKeystrokeTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private boolean detectFlyStateChange(Player player) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        boolean isFlying = player.isFlying();

        PlayerLocationData data = lastLocationData.get(uuid);
        if (data != null && data.wasFlying != isFlying) {
            lastFlyStateChange.put(uuid, currentTime);
            data.wasFlying = isFlying;
            return true;
        } else if (data != null) {
            data.wasFlying = isFlying;
        }

        return false;
    }

    private void updatePlayerLocationData(Player player, PlayerMoveEvent event) {
        if (event.getTo() == null) return;

        UUID uuid = player.getUniqueId();
        PlayerLocationData data = lastLocationData.computeIfAbsent(uuid, k -> new PlayerLocationData());

        data.lastX = event.getTo().getX();
        data.lastY = event.getTo().getY();
        data.lastZ = event.getTo().getZ();
        data.lastYaw = event.getTo().getYaw();
        data.lastPitch = event.getTo().getPitch();
        data.lastUpdate = System.currentTimeMillis();

        // Store historical data for pattern detection (used by PatternDetector in future phases)
        if (data.locationHistory.size() >= 50) { // Keep last 50 positions
            data.locationHistory.remove(0);
        }
        data.locationHistory.add(new LocationSnapshot(
                event.getTo().getX(),
                event.getTo().getY(),
                event.getTo().getZ(),
                event.getTo().getYaw(),
                event.getTo().getPitch(),
                System.currentTimeMillis()
        ));
    }

    private void initializePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        updateLastMovementTimestamp(player);

        PlayerLocationData data = new PlayerLocationData();
        data.lastX = player.getLocation().getX();
        data.lastY = player.getLocation().getY();
        data.lastZ = player.getLocation().getZ();
        data.lastYaw = player.getLocation().getYaw();
        data.lastPitch = player.getLocation().getPitch();
        data.wasSwimming = player.isSwimming();
        data.wasFlying = player.isFlying();
        data.lastUpdate = System.currentTimeMillis();

        lastLocationData.put(uuid, data);
        
        // v2.4 NEW: Initialize keystroke tracking for new players
        updateLastKeystrokeTime(player);
    }

    private void clearPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        lastMovementTime.remove(uuid);
        lastLocationData.remove(uuid);
        lastHeadRotationTime.remove(uuid);
        lastJumpTime.remove(uuid);
        jumpCounter.remove(uuid);
        lastSwimStateChange.remove(uuid);
        lastFlyStateChange.remove(uuid);
        lastCommandTime.remove(uuid);
        
        // v2.4 NEW: Clear keystroke tracking data
        lastKeystrokeTime.remove(uuid);
        lastMovementWasAutomatic.remove(uuid);
    }

    private void updateLastMovementTimestamp(Player player) {
        lastMovementTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public long getLastMovementTimestamp(Player player) {
        return lastMovementTime.getOrDefault(player.getUniqueId(), System.currentTimeMillis());
    }

    // Enhanced getters for advanced detection data (used by other systems)

    public long getLastHeadRotationTime(Player player) {
        return lastHeadRotationTime.getOrDefault(player.getUniqueId(), 0L);
    }

    public long getLastJumpTime(Player player) {
        return lastJumpTime.getOrDefault(player.getUniqueId(), 0L);
    }

    public long getLastCommandTime(Player player) {
        return lastCommandTime.getOrDefault(player.getUniqueId(), 0L);
    }

    public PlayerLocationData getPlayerLocationData(Player player) {
        return lastLocationData.get(player.getUniqueId());
    }
    
    // v2.4 NEW: Keystroke detection methods
    
    /**
     * Gets the timestamp of the last manual keystroke detected for a player.
     * This is used to detect players who are only moving due to water currents
     * without any manual input (large AFK pools).
     * 
     * @param player The player to check
     * @return Last keystroke timestamp, or 0 if never detected
     */
    public long getLastKeystrokeTime(Player player) {
        return lastKeystrokeTime.getOrDefault(player.getUniqueId(), 0L);
    }
    
    /**
     * Gets the keystroke timeout threshold from configuration.
     * Falls back to default if not configured.
     * 
     * @return Keystroke timeout in milliseconds
     */
    public long getKeystrokeTimeoutMs() {
        AntiAFKPlus plugin = AntiAFKPlus.getInstance();
        if (plugin != null && plugin.getConfigManager() != null) {
            return plugin.getConfig().getLong("pattern-detection-settings.keystroke-timeout-ms", DEFAULT_KEYSTROKE_TIMEOUT_MS);
        }
        return DEFAULT_KEYSTROKE_TIMEOUT_MS;
    }
    
    /**
     * Checks if a player has exceeded the keystroke timeout threshold.
     * Used by PatternDetector for large AFK pool detection.
     * 
     * @param player The player to check
     * @return true if player hasn't provided manual input for too long
     */
    public boolean hasKeystrokeTimeout(Player player) {
        long lastKeystroke = getLastKeystrokeTime(player);
        if (lastKeystroke == 0L) {
            // If we've never detected a keystroke, use join time as baseline
            PlayerLocationData data = lastLocationData.get(player.getUniqueId());
            if (data != null && data.lastUpdate > 0) {
                lastKeystroke = data.lastUpdate;
            } else {
                return false; // Not enough data
            }
        }
        
        long timeSinceKeystroke = System.currentTimeMillis() - lastKeystroke;
        return timeSinceKeystroke > getKeystrokeTimeoutMs();
    }

    // Inner classes for data structures

    public static class PlayerLocationData {
        public double lastX, lastY, lastZ;
        public float lastYaw, lastPitch;
        public boolean wasSwimming = false;
        public boolean wasFlying = false;
        public long lastUpdate;
        public java.util.List<LocationSnapshot> locationHistory = new java.util.ArrayList<>();
    }

    public static class LocationSnapshot {
        public final double x, y, z;
        public final float yaw, pitch;
        public final long timestamp;

        public LocationSnapshot(double x, double y, double z, float yaw, float pitch, long timestamp) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.timestamp = timestamp;
        }
    }
}
