package me.koyere.antiafkplus.afk;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.HashMap;
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

    // Configuration thresholds for micro-movement detection
    private static final double MICRO_MOVEMENT_THRESHOLD = 0.1; // Minimum movement to count as activity
    private static final double HEAD_ROTATION_THRESHOLD = 5.0; // Minimum degrees for head rotation
    private static final long JUMP_SPAM_THRESHOLD = 1000; // Milliseconds between jumps to detect spam
    private static final int MAX_JUMPS_PER_PERIOD = 10; // Maximum jumps in spam detection period
    private static final long JUMP_RESET_PERIOD = 30000; // Reset jump counter every 30 seconds

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

        // Update location data for pattern detection
        updatePlayerLocationData(player, event);

        // If any significant activity detected, update activity timestamp
        if (significantMovement || headRotation || jumpActivity || swimStateChange || flyStateChange) {
            if (AntiAFKPlus.getInstance() != null && AntiAFKPlus.getInstance().getAfkManager() != null) {
                getAfkManager().onPlayerActivity(player);
            }
            updateLastMovementTimestamp(player);

            // Update specific activity timestamps
            if (headRotation) {
                lastHeadRotationTime.put(player.getUniqueId(), System.currentTimeMillis());
            }
            if (jumpActivity) {
                lastJumpTime.put(player.getUniqueId(), System.currentTimeMillis());
            }
            if (swimStateChange) {
                lastSwimStateChange.put(player.getUniqueId(), System.currentTimeMillis());
            }
            if (flyStateChange) {
                lastFlyStateChange.put(player.getUniqueId(), System.currentTimeMillis());
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        AntiAFKPlus plugin = AntiAFKPlus.getInstance();
        if (plugin == null || !plugin.isEnabled()) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.hasPermission("antiafkplus.bypass")) return;
            if (plugin.getAfkManager() != null) {
                plugin.getAfkManager().onPlayerActivity(player);
            }
            updateLastMovementTimestamp(player);
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (player.hasPermission("antiafkplus.bypass")) return;
            if (AntiAFKPlus.getInstance() != null && AntiAFKPlus.getInstance().getAfkManager() != null) {
                getAfkManager().onPlayerActivity(player);
            }
            updateLastMovementTimestamp(player);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("antiafkplus.bypass")) return;
        if (AntiAFKPlus.getInstance() != null && AntiAFKPlus.getInstance().getAfkManager() != null) {
            getAfkManager().onPlayerActivity(player);
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
            getAfkManager().onPlayerActivity(player);
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
                getAfkManager().onPlayerActivity(player);
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