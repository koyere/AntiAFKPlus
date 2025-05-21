// AntiAFKActivityDetector.java - English comments
package me.koyere.antiafkplus.afk;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.config.ConfigManager; // For config access
import org.bukkit.Material;
// import org.bukkit.block.BlockFace; // Not currently used
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority; // Good to specify if needed
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask; // For storing the cleanup task if needed for cancellation

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Detects specific player activities like fishing or mining while they might be AFK,
 * and triggers a reassessment of their AFK status.
 */
public class AntiAFKActivityDetector implements Listener {

    private final AntiAFKPlus plugin;
    private final AFKManager afkManager; // Store AFKManager instance
    private final ConfigManager configManager; // Store ConfigManager instance

    // These maps track recent activity to potentially identify sophisticated AFK machines,
    // though current implementation is simple: any valid action resets AFK.
    private final Map<UUID, Long> lastBlockBreakTime = new HashMap<>(); // Renamed for clarity
    private final Map<UUID, Long> lastFishingActivityTime = new HashMap<>(); // Renamed for clarity

    private BukkitTask cleanupTask; // To store the task for potential cancellation onDisable

    /**
     * Constructor for AntiAFKActivityDetector.
     * @param plugin The main plugin instance.
     */
    public AntiAFKActivityDetector(AntiAFKPlus plugin) {
        this.plugin = plugin;
        this.afkManager = plugin.getAfkManager(); // Get AFKManager from plugin
        this.configManager = plugin.getConfigManager(); // Get ConfigManager from plugin

        // Optional task to clear old records from the HashMaps to prevent memory leaks over time.
        // This is a good practice if the server runs for very long periods with many unique players.
        this.cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                long cutoff = currentTime - 300_000; // 5 minutes ago

                // Using iterator to safely remove while iterating (or use removeIf for Java 8+)
                lastBlockBreakTime.entrySet().removeIf(entry -> entry.getValue() < cutoff);
                lastFishingActivityTime.entrySet().removeIf(entry -> entry.getValue() < cutoff);

                if (configManager.isDebugEnabled()) {
                    // Avoid logging this every 5 minutes unless truly necessary for debugging memory usage.
                    // plugin.getLogger().info("[DEBUG] Cleaned up old activity records in AntiAFKActivityDetector.");
                }
            }
        }.runTaskTimerAsynchronously(plugin, 6000L, 6000L); // Run every 5 minutes (6000 ticks), start after 5 mins. Async is fine for this.
    }

    /**
     * Handles BlockBreakEvent to detect mining activity.
     * If a player breaks a solid block while marked as AFK, their AFK status is reassessed.
     * @param event The BlockBreakEvent.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Only process if the player is currently considered AFK by the AFKManager.
        if (!afkManager.isAFK(player)) {
            return;
        }

        // Check if the broken block is solid (e.g., not air, water, grass).
        // This helps filter out less significant interactions.
        Material type = event.getBlock().getType();
        if (type.isSolid()) {
            lastBlockBreakTime.put(uuid, System.currentTimeMillis()); // Record this specific activity
            afkManager.onPlayerActivity(player); // Notify AFKManager about generic player activity

            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG_Activity] " + player.getName() + " broke a solid block ("+ type.name() +") while AFK - AFK status reassessed.");
            }
        }
    }

    /**
     * Handles PlayerFishEvent to detect fishing activity.
     * If a player is fishing or catches a fish while marked as AFK, their AFK status is reassessed.
     * @param event The PlayerFishEvent.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFishing(PlayerFishEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Only process if the player is currently considered AFK by the AFKManager.
        if (!afkManager.isAFK(player)) {
            return;
        }

        // Consider various fishing states as activity.
        PlayerFishEvent.State state = event.getState();
        if (state == PlayerFishEvent.State.CAUGHT_FISH ||
                state == PlayerFishEvent.State.FISHING ||      // When bobber is cast
                state == PlayerFishEvent.State.REEL_IN ||      // When player reels in (even without a catch)
                state == PlayerFishEvent.State.IN_GROUND) {    // Bobber stuck in ground (still an action)

            lastFishingActivityTime.put(uuid, System.currentTimeMillis()); // Record this specific activity
            afkManager.onPlayerActivity(player); // Notify AFKManager about generic player activity

            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG_Activity] " + player.getName() + " performed fishing action ("+ state.name() +") while AFK - AFK status reassessed.");
            }
        }
    }

    /**
     * Call this method from plugin's onDisable to cancel the cleanup task.
     */
    public void shutdown() {
        if (this.cleanupTask != null && !this.cleanupTask.isCancelled()) {
            this.cleanupTask.cancel();
            plugin.getLogger().info("AntiAFKActivityDetector cleanup task cancelled.");
        }
    }
}