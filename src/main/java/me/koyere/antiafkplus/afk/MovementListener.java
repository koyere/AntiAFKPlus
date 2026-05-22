package me.koyere.antiafkplus.afk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.api.data.ActivityType;

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

    // v3.0.5: State for "Use Item / Place Block" toggle-mode bypass detection.
    // Keeps the last interact context per player so we can detect identical
    // right-clicks repeated tick-after-tick while the player stays still.
    private final Map<UUID, InteractContext> lastInteractContext = new HashMap<>();

    // Configuration thresholds for micro-movement detection (loaded from config)
    private double microMovementThreshold = 0.1;
    private double headRotationThreshold = 5.0;
    private long jumpSpamThreshold = 1000;
    private int maxJumpsPerPeriod = 10;
    private long jumpResetPeriod = 30000;
    
    // v2.4 NEW: Keystroke timeout detection constants
    private static final long DEFAULT_KEYSTROKE_TIMEOUT_MS = 180000; // 3 minutes without keystrokes
    private static final double AUTOMATIC_MOVEMENT_VELOCITY_THRESHOLD = 0.15; // Water current movement speed

    // v3.0.5: Threshold used by isPassiveLiquidMovement.
    // A real water-current push in Minecraft is ~0.014 blocks/tick. Manual swimming
    // with W held (no sprint) is ~0.10–0.13 blocks/tick. We pick 0.08 so that any
    // legitimate manual swim crosses it (no false positive) while still catching
    // small currents and step-block bobbing inside compact AFK pools.
    private static final double PASSIVE_LIQUID_VELOCITY_THRESHOLD = 0.08;

    // Constructor does not need AFKManager if it gets it via AntiAFKPlus.getInstance()
    public MovementListener() {
        loadConfigThresholds();
    }

    /**
     * Loads movement detection thresholds from config.
     * Falls back to safe defaults if config is not yet available.
     */
    public void loadConfigThresholds() {
        AntiAFKPlus plugin = AntiAFKPlus.getInstance();
        if (plugin != null && plugin.getConfigManager() != null) {
            this.microMovementThreshold = plugin.getConfigManager().getMicroMovementThreshold();
            this.headRotationThreshold = plugin.getConfigManager().getHeadRotationThreshold();
            this.jumpSpamThreshold = plugin.getConfigManager().getJumpSpamThreshold();
            this.maxJumpsPerPeriod = plugin.getConfigManager().getMaxJumpsPerPeriod();
            this.jumpResetPeriod = plugin.getConfigManager().getJumpResetPeriod();
        }
    }

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

        // v3.0.3 FIX: Passive vehicle movement bypass prevention
        // When a player is riding an entity (horse, donkey, camel, boat, etc.),
        // the entity's movement (especially bobbing in water) fires PlayerMoveEvent
        // for the rider. This passive movement must NOT count as legitimate player
        // activity, as it would immediately unmark AFK players who were correctly
        // detected by the PatternDetector.
        //
        // We still record location data for pattern detection so the PatternDetector
        // can continue monitoring the player's position while mounted.
        if (player.isInsideVehicle()) {
            // Always feed location data to PatternDetector for continued monitoring
            updatePlayerLocationData(player, event);

            // Only count explicit head rotation as activity while mounted,
            // since that requires actual player input (mouse movement)
            if (detectHeadRotation(player, event)) {
                AFKManager manager = AntiAFKPlus.getInstance() != null ? AntiAFKPlus.getInstance().getAfkManager() : null;
                if (manager != null) {
                    manager.onPlayerActivity(player, ActivityType.HEAD_ROTATION);
                    lastHeadRotationTime.put(player.getUniqueId(), System.currentTimeMillis());
                    updateLastMovementTimestamp(player);
                    updateLastKeystrokeTime(player);
                }
            }
            return;
        }

        // v3.0.4 FIX: Classic AFK water-pool bypass prevention
        // A player standing in a small water pool gets pushed by the current.
        // If the pool has step blocks underwater, deltaY can exceed
        // microMovementThreshold and detectSignificantMovement() would return true,
        // causing onPlayerActivity(MOVEMENT) to immediately unmark a player who was
        // correctly forced AFK by the PatternDetector (confined_space / keystroke_timeout).
        //
        // We treat liquid movement as passive when ALL of these hold:
        //   - the player is in water/lava
        //   - no head rotation in this tick (no mouse input)
        //   - not sprinting and not sneaking (no held key)
        //   - horizontal velocity below the manual-swim threshold
        //
        // PatternDetector keeps receiving position updates so it can still analyze
        // the player's movement and confirm AFK patterns.
        if (isPassiveLiquidMovement(player, event)) {
            updatePlayerLocationData(player, event);
            return;
        }

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
    public void onPlayerChat(AsyncChatEvent event) {
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

        // v3.0.5 FIX: "Use Item / Place Block" toggle-mode bypass prevention.
        // Minecraft's accessibility option lets the right-click action be toggled
        // on, which makes the client send a right-click on the same block every
        // tick while the player stays still. Each one fires PlayerInteractEvent,
        // which previously was always treated as activity and reset the AFK
        // timer indefinitely. We ignore an interact when ALL of these hold
        // compared to the previous interact:
        //   - same action (RIGHT_CLICK_BLOCK)
        //   - same block coordinates
        //   - same clicked face
        //   - same item in main hand
        //   - the player has not moved or rotated since the previous interact
        //
        // The first interact in a sequence still counts as activity (legitimate
        // human action). Any movement or camera rotation breaks the sequence
        // and the next interact counts again.
        if (isPassiveRepeatedInteract(player, event)) {
            return;
        }

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
        return deltaX > microMovementThreshold ||
                deltaY > microMovementThreshold ||
                deltaZ > microMovementThreshold;
    }

    private boolean detectHeadRotation(Player player, PlayerMoveEvent event) {
        if (event.getTo() == null) return false;

        double deltaYaw = Math.abs(event.getTo().getYaw() - event.getFrom().getYaw());
        double deltaPitch = Math.abs(event.getTo().getPitch() - event.getFrom().getPitch());

        // Normalize yaw difference (handle 360 degree wrap-around)
        if (deltaYaw > 180) {
            deltaYaw = 360 - deltaYaw;
        }

        return deltaYaw > headRotationThreshold || deltaPitch > headRotationThreshold;
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
            if (lastJump == null || (currentTime - lastJump) > jumpSpamThreshold) {
                // Reset jump counter if enough time has passed
                if (lastJump == null || (currentTime - lastJump) > jumpResetPeriod) {
                    jumpCounter.put(uuid, 1);
                } else {
                    int jumps = jumpCounter.getOrDefault(uuid, 0) + 1;
                    jumpCounter.put(uuid, jumps);

                    // If too many jumps in short period, might be AFK farm
                    if (jumps > maxJumpsPerPeriod) {
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

    /**
     * v3.0.5: Detects right-click interactions that are mechanical repetitions
     * of the previous one — characteristic fingerprint of Minecraft's
     * "Use Item / Place Block: toggle" accessibility option used as an AFK
     * bypass (e.g. holding right-click on a lever forever without input).
     *
     * Returns {@code true} only when, compared to the previous interact:
     *   - same action (must be RIGHT_CLICK_BLOCK; air clicks and left clicks
     *     are never considered passive)
     *   - same block coordinates and same clicked face
     *   - same item type held in the main hand
     *   - the player has not moved nor rotated since the previous interact
     *
     * The first interact of a sequence is always counted as activity. Any
     * movement, sneak, sprint or camera rotation breaks the sequence and the
     * next interact counts as activity again. This guarantees zero false
     * positives for normal play (clicking different blocks, looking around,
     * walking while clicking, etc.) while neutralizing the toggle bypass.
     *
     * Public so other listeners (e.g. {@code AntiAFKActivityDetector}) can
     * apply the same filter and stay in sync.
     */
    public boolean isPassiveRepeatedInteract(Player player, PlayerInteractEvent event) {
        // Only right-click on a real block can be turned into a toggle loop.
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            updateInteractContext(player, event);
            return false;
        }

        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            updateInteractContext(player, event);
            return false;
        }

        UUID uuid = player.getUniqueId();
        InteractContext previous = lastInteractContext.get(uuid);

        // Build the current context from the event + player state.
        InteractContext current = new InteractContext(
                clicked.getWorld().getUID(),
                clicked.getX(),
                clicked.getY(),
                clicked.getZ(),
                event.getBlockFace(),
                player.getInventory().getItemInMainHand().getType().name(),
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ(),
                player.getLocation().getYaw(),
                player.getLocation().getPitch()
        );

        // No previous interact recorded → this is the first one, count it.
        if (previous == null) {
            lastInteractContext.put(uuid, current);
            return false;
        }

        // Different block, face, item, world or action target → real activity.
        if (!previous.matchesTarget(current)) {
            lastInteractContext.put(uuid, current);
            return false;
        }

        // Player moved or rotated since previous interact → real activity.
        if (!previous.matchesPlayerStillness(current)) {
            lastInteractContext.put(uuid, current);
            return false;
        }

        // Identical interact, identical position, identical orientation:
        // toggle-mode repetition. Refresh the stored context (so the comparison
        // window keeps moving) but do NOT count as activity.
        lastInteractContext.put(uuid, current);
        return true;
    }

    private void updateInteractContext(Player player, PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            // Air interacts and the like still update the marker so that any
            // subsequent block click is treated as the start of a new sequence.
            lastInteractContext.remove(player.getUniqueId());
            return;
        }
        lastInteractContext.put(player.getUniqueId(), new InteractContext(
                clicked.getWorld().getUID(),
                clicked.getX(),
                clicked.getY(),
                clicked.getZ(),
                event.getBlockFace(),
                player.getInventory().getItemInMainHand().getType().name(),
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ(),
                player.getLocation().getYaw(),
                player.getLocation().getPitch()
        ));
    }

    /**
     * v3.0.4: Detects movement that is the result of being pushed by liquid
     * (water/lava current) without any actual player input. Used to fix the
     * classic AFK water-pool bypass where the current moves the player just
     * enough to fire {@link PlayerMoveEvent} and reset the AFK state right
     * after the {@link PatternDetector} flagged the player.
     *
     * Treated as passive when ALL of these hold:
     *  - the player's body or eyes are inside a liquid block
     *  - no head rotation is happening in this tick (no mouse input)
     *  - the player is not sprinting and not sneaking (no held movement key)
     *  - the player is not flying nor gliding
     *  - horizontal velocity is below the threshold typical of manual swim/walk input
     */
    private boolean isPassiveLiquidMovement(Player player, PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom() == null) {
            return false;
        }

        // 1. Must be in a liquid (water current is the only common natural pusher)
        boolean inLiquid = player.getLocation().getBlock().isLiquid()
                || player.getEyeLocation().getBlock().isLiquid();
        if (!inLiquid) {
            return false;
        }

        // 2. Any head movement implies real player input → not passive
        if (detectHeadRotation(player, event)) {
            return false;
        }

        // 3. Held movement keys imply real player input
        if (player.isSprinting() || player.isSneaking() || player.isFlying() || player.isGliding()) {
            return false;
        }

        // 4. Horizontal speed above the passive threshold implies the player is
        // actively swimming. Manual swim with W held is ~0.10–0.13 b/t, while a
        // natural water current is ~0.014 b/t, so 0.08 leaves a safe margin in
        // both directions.
        double dx = event.getTo().getX() - event.getFrom().getX();
        double dz = event.getTo().getZ() - event.getFrom().getZ();
        double horizontalVelocity = Math.sqrt(dx * dx + dz * dz);
        if (horizontalVelocity > PASSIVE_LIQUID_VELOCITY_THRESHOLD) {
            return false;
        }

        return true;
    }

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
            if (horizontalVelocity > microMovementThreshold) {
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

        // v3.0.5: Clear interact-context tracking
        lastInteractContext.remove(uuid);
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

    /**
     * v3.0.5: Snapshot of an interact event used to detect mechanical
     * repetitions of the toggle "Use Item / Place Block" accessibility option.
     * Captures the click target plus the player's exact position and
     * orientation at the moment of the click.
     */
    private static final class InteractContext {
        // Tolerance for comparing player coordinates: anything below this is
        // treated as "no movement". 0.001 blocks is well below the smallest
        // measurable Minecraft player movement.
        private static final double POSITION_EPSILON = 0.001;
        // Tolerance for yaw/pitch comparison: 0.5° is below human-perceivable
        // mouse adjustment but well above floating-point noise.
        private static final float ROTATION_EPSILON = 0.5f;

        private final java.util.UUID worldId;
        private final int blockX;
        private final int blockY;
        private final int blockZ;
        private final BlockFace face;
        private final String mainHandItem;
        private final double playerX;
        private final double playerY;
        private final double playerZ;
        private final float playerYaw;
        private final float playerPitch;

        InteractContext(java.util.UUID worldId, int blockX, int blockY, int blockZ,
                        BlockFace face, String mainHandItem,
                        double playerX, double playerY, double playerZ,
                        float playerYaw, float playerPitch) {
            this.worldId = worldId;
            this.blockX = blockX;
            this.blockY = blockY;
            this.blockZ = blockZ;
            this.face = face;
            this.mainHandItem = mainHandItem;
            this.playerX = playerX;
            this.playerY = playerY;
            this.playerZ = playerZ;
            this.playerYaw = playerYaw;
            this.playerPitch = playerPitch;
        }

        boolean matchesTarget(InteractContext other) {
            return java.util.Objects.equals(this.worldId, other.worldId)
                    && this.blockX == other.blockX
                    && this.blockY == other.blockY
                    && this.blockZ == other.blockZ
                    && this.face == other.face
                    && java.util.Objects.equals(this.mainHandItem, other.mainHandItem);
        }

        boolean matchesPlayerStillness(InteractContext other) {
            return Math.abs(this.playerX - other.playerX) < POSITION_EPSILON
                    && Math.abs(this.playerY - other.playerY) < POSITION_EPSILON
                    && Math.abs(this.playerZ - other.playerZ) < POSITION_EPSILON
                    && Math.abs(this.playerYaw - other.playerYaw) < ROTATION_EPSILON
                    && Math.abs(this.playerPitch - other.playerPitch) < ROTATION_EPSILON;
        }
    }
}
