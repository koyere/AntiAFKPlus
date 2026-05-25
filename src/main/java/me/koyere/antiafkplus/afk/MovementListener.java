package me.koyere.antiafkplus.afk;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    // All maps use ConcurrentHashMap so the async PatternDetector can read
    // timestamp values concurrently with main-thread writes without data races.
    private final Map<UUID, Long> lastMovementTime = new ConcurrentHashMap<>();

    // Enhanced detection data structures
    private final Map<UUID, PlayerLocationData> lastLocationData = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastHeadRotationTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastJumpTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> jumpCounter = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastSwimStateChange = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastFlyStateChange = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastCommandTime = new ConcurrentHashMap<>();

    // v2.4 NEW: Keystroke detection for large AFK pools
    private final Map<UUID, Long> lastKeystrokeTime = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> lastMovementWasAutomatic = new ConcurrentHashMap<>();

    // v3.0.5: State for "Use Item / Place Block" toggle-mode bypass detection.
    private final Map<UUID, InteractContext> lastInteractContext = new ConcurrentHashMap<>();

    // v3.1: Timestamp of the last PlayerInteractEvent (recorded BEFORE the passive
    // filter). Used by PatternDetector to distinguish a stationary AFK miner (whose
    // client keeps firing LEFT_CLICK_BLOCK) from a player who is doing absolutely
    // nothing, so the detector can skip the latter without skipping the former.
    private final Map<UUID, Long> lastAnyInteractTime = new ConcurrentHashMap<>();

    // v3.1: Player position captured at the time of the most recent door/trapdoor
    // RIGHT_CLICK_BLOCK. When a door opens, subsequent auto-clicks may land on AIR
    // (the cursor passes through the open door) or on a block behind the door — both
    // should still be treated as passive when the player hasn't moved.
    private final Map<UUID, InteractContext> lastDoorToggleContext = new ConcurrentHashMap<>();

    // Configuration thresholds for micro-movement detection (loaded from config)
    private double microMovementThreshold = 0.1;
    private double headRotationThreshold = 5.0;
    private long jumpSpamThreshold = 1000;
    private int maxJumpsPerPeriod = 10;
    private long jumpResetPeriod = 30000;
    
    // v2.4 NEW: Keystroke timeout detection constants
    private static final long DEFAULT_KEYSTROKE_TIMEOUT_MS = 180000; // 3 minutes without keystrokes
    private static final double AUTOMATIC_MOVEMENT_VELOCITY_THRESHOLD = 0.15; // Water current movement speed

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

        // v3.0.5 NOTE: passive movement filtering is now handled centrally in
        // AFKManager.onPlayerActivity via the patternEnforcedAfk lock. We no
        // longer try to guess which PlayerMoveEvent is "passive" here, because
        // the actual fix is to require strong human input (chat, command,
        // inventory click, fishing, head rotation) before lifting an AFK state
        // that was triggered by the PatternDetector. The vehicle guard above
        // is preserved because it also affects the location-data feed used by
        // the PatternDetector itself.

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
                // Any real movement invalidates the door-toggle context so that
                // subsequent clicks on blocks behind an open door are not suppressed
                // (the player moved, so those clicks are genuine activity).
                lastDoorToggleContext.remove(player.getUniqueId());
                manager.onPlayerActivity(player, ActivityType.MOVEMENT);
                recorded = true;
            }
            if (headRotation) {
                manager.onPlayerActivity(player, ActivityType.HEAD_ROTATION);
                lastHeadRotationTime.put(player.getUniqueId(), now);
                // Head rotation always requires mouse input — update keystroke timer here
                // so that hasKeystrokeTimeout() correctly reflects real manual presence.
                // This mirrors the vehicle code path (lines 122-131) for consistency.
                updateLastKeystrokeTime(player);
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

    // No ignoreCancelled here — intentional. We run at NORMAL priority without
    // filtering cancelled events so that lastAnyInteractTime is updated regardless
    // of whether another plugin (at any priority) cancels the event. What matters
    // for AFK detection is that the client sent the click, not whether the action
    // was ultimately allowed. Adding ignoreCancelled = true would break the
    // PatternDetector gate for cobble generators on servers with anti-grief plugins
    // that cancel block-break events.
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("antiafkplus.bypass")) return;

        // Record that any interact happened — BEFORE the passive filter and before
        // checking cancellation status — so PatternDetector can distinguish a
        // stationary AFK miner (whose client keeps firing LEFT_CLICK_BLOCK) from
        // a player who is doing absolutely nothing.
        lastAnyInteractTime.put(player.getUniqueId(), System.currentTimeMillis());

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
     * Detects interact events that are mechanical repetitions of the previous
     * one — the characteristic fingerprint of Minecraft's accessibility toggle
     * options used as AFK bypasses:
     *
     * <ul>
     *   <li><b>"Use Item / Place Block" toggle (RIGHT_CLICK_BLOCK)</b> —
     *       e.g. holding right-click on a lever forever without real input.</li>
     *   <li><b>"Attack / Destroy" toggle (LEFT_CLICK_BLOCK)</b> —
     *       e.g. standing still in a cobble generator: each regenerated block
     *       causes the client to restart the dig sequence, firing
     *       {@code LEFT_CLICK_BLOCK} in a tight loop that continuously resets
     *       the AFK timer even though no human input occurred.</li>
     * </ul>
     *
     * Returns {@code true} only when, compared to the previous interact of the
     * <em>same action type</em>:
     * <ul>
     *   <li>same block coordinates and clicked face</li>
     *   <li>same item type held in the main hand</li>
     *   <li>the player has not moved nor rotated since the previous interact</li>
     * </ul>
     *
     * The first interact of each sequence is always counted as activity. Any
     * movement, camera rotation, tool swap, different block, or action-type
     * change breaks the sequence and the next interact counts as activity again.
     * Right-click and left-click sequences are tracked independently so
     * alternating between the two on the same block never produces false positives.
     *
     * <p>Public so {@link AntiAFKActivityDetector} can delegate here and remain
     * in sync with {@code MovementListener}.
     */
    public boolean isPassiveRepeatedInteract(Player player, PlayerInteractEvent event) {
        Action action = event.getAction();
        UUID uuid = player.getUniqueId();

        // Non-block actions (air clicks, entity clicks) cannot form a toggle loop
        // themselves, with one important exception: when a door or trapdoor opens,
        // the client's next auto-click passes THROUGH the now-open door and lands
        // on AIR. Without this check that air click would always reset the AFK
        // timer even though the player is standing completely still.
        //
        // Cobble-generator fix: on non-block actions we intentionally do NOT clear
        // lastInteractContext — see the original comment for the full rationale.
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.LEFT_CLICK_AIR) {
                InteractContext doorCtx = lastDoorToggleContext.get(uuid);
                if (doorCtx != null) {
                    org.bukkit.Location loc = player.getLocation();
                    if (doorCtx.matchesPlayerLocation(loc.getX(), loc.getY(), loc.getZ(),
                            loc.getYaw(), loc.getPitch())) {
                        return true; // Air click through open door while still → passive
                    }
                }
            }
            return false;
        }

        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return false;
        }

        // Normalize door and trapdoor interactions so toggle-mode bypass is detected:
        //
        // • face:  a closed door presents one face; after it opens the door panel
        //          rotates 90°, so the next click (if it still hits the door) reports
        //          a different BlockFace. Use BlockFace.SELF as a sentinel meaning
        //          "skip face comparison" so closed-face and open-face both map to the
        //          same context — the player hasn't moved, it's still the same door.
        //
        // • blockY: two-tall doors fire RIGHT_CLICK_BLOCK on top half (Y+1) or bottom
        //           half (Y) depending on the player's eye angle at the moment of the
        //           click. Normalize the top half to the bottom half so both map to the
        //           same canonical Y.
        String typeName = clicked.getType().name();
        boolean isDoor = typeName.endsWith("_DOOR") || typeName.endsWith("_TRAPDOOR");
        BlockFace storedFace = isDoor ? BlockFace.SELF : event.getBlockFace();
        int storedY = clicked.getY();
        if (isDoor && typeName.endsWith("_DOOR")) {
            try {
                if (clicked.getBlockData() instanceof org.bukkit.block.data.Bisected bisected
                        && bisected.getHalf() == org.bukkit.block.data.Bisected.Half.TOP) {
                    storedY = clicked.getY() - 1;
                }
            } catch (Exception ignored) { }
        }

        InteractContext current = new InteractContext(
                action.name(),
                clicked.getWorld().getUID(),
                clicked.getX(),
                storedY,
                clicked.getZ(),
                storedFace,
                player.getInventory().getItemInMainHand().getType().name(),
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ(),
                player.getLocation().getYaw(),
                player.getLocation().getPitch()
        );

        // Track door toggles so the air-click check above and the through-door
        // block-click check below have a reference player position to compare against.
        if (isDoor) {
            lastDoorToggleContext.put(uuid, current);
        }

        InteractContext previous = lastInteractContext.get(uuid);

        if (previous == null) {
            lastInteractContext.put(uuid, current);
            return false;
        }

        if (!previous.matchesTarget(current)) {
            // Click landed on a different block, face, or item. Before calling this
            // real activity, check whether it is a "through-door" click: the player
            // recently toggled a door and the auto-click now hits the block behind
            // the open door while the player has not moved at all.
            //
            // Distance guard: only suppress blocks within 2 Chebyshev units of the
            // door. Blocks farther away are independent — a lever, chest, or any
            // other functional block that the player deliberately walked to and
            // interacted with — and must NOT be suppressed even if the player's
            // feet happen to be at the same position as during the door toggle.
            InteractContext doorCtx = lastDoorToggleContext.get(uuid);
            if (doorCtx != null) {
                org.bukkit.Location loc = player.getLocation();
                if (doorCtx.matchesPlayerLocation(loc.getX(), loc.getY(), loc.getZ(),
                        loc.getYaw(), loc.getPitch())) {
                    int chebyshev = Math.max(Math.max(
                            Math.abs(current.blockX - doorCtx.blockX),
                            Math.abs(current.blockY - doorCtx.blockY)),
                            Math.abs(current.blockZ - doorCtx.blockZ));
                    if (chebyshev <= 2) {
                        lastInteractContext.put(uuid, current);
                        return true; // Block within door reach, player still → passive
                    }
                }
            }
            lastInteractContext.put(uuid, current);
            return false;
        }

        if (!previous.matchesPlayerStillness(current)) {
            lastInteractContext.put(uuid, current);
            return false;
        }

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
                event.getAction().name(),
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

        // Calculate horizontal displacement and velocity
        double deltaX = event.getTo().getX() - event.getFrom().getX();
        double deltaZ = event.getTo().getZ() - event.getFrom().getZ();
        double horizontalVelocity = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        // isInWater() uses the server's water-collision flag — more reliable than block checks.
        boolean inWater = player.isInWater();

        // Get previous movement data for direction-change analysis
        PlayerLocationData locationData = lastLocationData.get(uuid);
        if (locationData == null || locationData.locationHistory.isEmpty()) {
            return !inWater; // No history: trust land movement, distrust water movement
        }

        boolean appearsManual = false;

        // 1. Velocity-based detection (land only).
        //    Water currents routinely exceed any reasonable velocity threshold, so velocity
        //    alone is NOT a reliable indicator of deliberate key presses when in water.
        if (!inWater && horizontalVelocity > microMovementThreshold) {
            appearsManual = true;
        }

        // 2. Direction-change analysis.
        //    Manual players change direction unpredictably; water currents are smoother.
        //    We require a higher threshold in water (pools redirect players at corners,
        //    generating spurious direction changes that must not be mistaken for manual input).
        if (locationData.locationHistory.size() >= 3) {
            int histSize = locationData.locationHistory.size();
            List<LocationSnapshot> recent = locationData.locationHistory.subList(
                    Math.max(0, histSize - 3), histSize);
            double directionChanges = calculateDirectionChanges(recent);
            double threshold = inWater ? 0.8 : 0.5;
            if (directionChanges > threshold) {
                appearsManual = true;
            }
        }

        // 3. Head rotation is always a strong manual signal and is handled separately
        //    in the onPlayerMove block above (it also calls updateLastKeystrokeTime).
        //    We re-check here only to set appearsManual so the caller's logic stays correct.
        if (detectHeadRotation(player, event)) {
            appearsManual = true;
        }

        // Debug logging
        AntiAFKPlus plugin = AntiAFKPlus.getInstance();
        if (plugin != null && plugin.getConfigManager().isDebugEnabled() && horizontalVelocity > 0.05) {
            plugin.getLogger().info(String.format(
                "[DEBUG_Keystroke] %s: vel=%.3f, inWater=%s, manual=%s",
                player.getName(), horizontalVelocity, inWater, appearsManual));
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

        // Store historical data for pattern detection (used by PatternDetector).
        // 300 entries ≈ 15 s at 20 TPS — enough to capture a full large-pool circuit
        // before PatternDetector's 30-second analysis window fires.
        boolean inWater = player.isInWater();
        if (data.locationHistory.size() >= 300) {
            data.locationHistory.remove(0);
        }
        data.locationHistory.add(new LocationSnapshot(
                event.getTo().getX(),
                event.getTo().getY(),
                event.getTo().getZ(),
                event.getTo().getYaw(),
                event.getTo().getPitch(),
                System.currentTimeMillis(),
                inWater
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
        lastAnyInteractTime.remove(uuid);
        lastDoorToggleContext.remove(uuid);
    }

    private void updateLastMovementTimestamp(Player player) {
        lastMovementTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public long getLastMovementTimestamp(Player player) {
        // 0L = "never moved": AFKManager will compute timeSinceActivity = now - 0 = very large,
        // treating the player as immediately AFK. initializePlayerData() on join sets a real
        // timestamp, so this default only triggers if data was cleared mid-session.
        return lastMovementTime.getOrDefault(player.getUniqueId(), 0L);
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
            // Delegate to ConfigManager which reads the canonical key:
            // modules.pattern-detection.keystroke-timeout-seconds (current config)
            // with legacy fallback to keystroke-timeout-ms.
            return plugin.getConfigManager().getKeystrokeTimeoutMs();
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
    public long getLastAnyInteractTime(Player player) {
        return lastAnyInteractTime.getOrDefault(player.getUniqueId(), 0L);
    }

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
        // CopyOnWriteArrayList: main-thread writes + async PatternDetector reads are safe without external locks.
        public java.util.List<LocationSnapshot> locationHistory = new java.util.concurrent.CopyOnWriteArrayList<>();
    }

    public static class LocationSnapshot {
        public final double x, y, z;
        public final float yaw, pitch;
        public final long timestamp;
        /** True if the player was in water when this snapshot was recorded. */
        public final boolean inWater;

        public LocationSnapshot(double x, double y, double z, float yaw, float pitch, long timestamp, boolean inWater) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.timestamp = timestamp;
            this.inWater = inWater;
        }
    }

    /**
     * Snapshot of an interact event used to detect mechanical repetitions of
     * Minecraft's accessibility toggle options:
     * <ul>
     *   <li>"Use Item / Place Block" toggle → RIGHT_CLICK_BLOCK</li>
     *   <li>"Attack / Destroy" toggle      → LEFT_CLICK_BLOCK (cobble generators, etc.)</li>
     * </ul>
     * Captures the action type, click target and the player's exact position
     * and orientation at the moment of the click.
     *
     * <p>The {@code action} field is included in {@link #matchesTarget} so
     * right-click and left-click sequences are always tracked independently,
     * preventing false positives when a player alternates between the two on
     * the same block.
     */
    private static final class InteractContext {
        // Tolerance for comparing player coordinates: anything below this is
        // treated as "no movement". 0.001 blocks is well below the smallest
        // measurable Minecraft player movement.
        private static final double POSITION_EPSILON = 0.001;
        // Tolerance for yaw/pitch comparison: 0.5° is below human-perceivable
        // mouse adjustment but well above floating-point noise.
        private static final float ROTATION_EPSILON = 0.5f;

        /** "RIGHT_CLICK_BLOCK" or "LEFT_CLICK_BLOCK" — kept as String to avoid
         *  holding a reference to the Bukkit Action enum across reloads. */
        private final String action;
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

        InteractContext(String action,
                        java.util.UUID worldId, int blockX, int blockY, int blockZ,
                        BlockFace face, String mainHandItem,
                        double playerX, double playerY, double playerZ,
                        float playerYaw, float playerPitch) {
            this.action = action;
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

        /**
         * Returns true when both contexts describe the same action type, the
         * same block (world + coordinates + face) and the same item held.
         * Action type is compared first to keep right-click and left-click
         * sequences fully isolated from each other.
         *
         * {@link BlockFace#SELF} is stored exclusively for door/trapdoor blocks
         * (see {@code isPassiveRepeatedInteract}). The SELF check is placed
         * AFTER the blockX/Y/Z equality checks on purpose: Java short-circuits
         * the {@code &&} chain, so face relaxation is never reached when the
         * two contexts already differ in coordinates. SELF therefore cannot
         * "bleed" into non-door blocks — it only skips the face comparison when
         * both contexts already point at the exact same block position, which
         * is always the same door.
         */
        boolean matchesTarget(InteractContext other) {
            return java.util.Objects.equals(this.action,  other.action)
                    && java.util.Objects.equals(this.worldId, other.worldId)
                    && this.blockX == other.blockX
                    && this.blockY == other.blockY
                    && this.blockZ == other.blockZ
                    // SELF = "any face" sentinel, door/trapdoor-only. Safe because the
                    // coordinate checks above already short-circuit for different blocks.
                    && (this.face == BlockFace.SELF || other.face == BlockFace.SELF
                        || this.face == other.face)
                    && java.util.Objects.equals(this.mainHandItem, other.mainHandItem);
        }

        boolean matchesPlayerStillness(InteractContext other) {
            return Math.abs(this.playerX - other.playerX) < POSITION_EPSILON
                    && Math.abs(this.playerY - other.playerY) < POSITION_EPSILON
                    && Math.abs(this.playerZ - other.playerZ) < POSITION_EPSILON
                    && Math.abs(this.playerYaw - other.playerYaw) < ROTATION_EPSILON
                    && Math.abs(this.playerPitch - other.playerPitch) < ROTATION_EPSILON;
        }

        /** Checks whether the given raw player position/rotation is at the same spot
         *  as the position captured in this context. Used by the door-toggle suppression
         *  logic which doesn't have a second InteractContext to compare against. */
        boolean matchesPlayerLocation(double x, double y, double z, float yaw, float pitch) {
            return Math.abs(this.playerX - x) < POSITION_EPSILON
                    && Math.abs(this.playerY - y) < POSITION_EPSILON
                    && Math.abs(this.playerZ - z) < POSITION_EPSILON
                    && Math.abs(this.playerYaw - yaw) < ROTATION_EPSILON
                    && Math.abs(this.playerPitch - pitch) < ROTATION_EPSILON;
        }
    }
}
