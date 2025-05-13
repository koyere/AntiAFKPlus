package me.koyere.antiafkplus.afk;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Detects player activity during AFK mode (like fishing or mining),
 * and resets their AFK status if valid action is performed.
 */
public class AntiAFKActivityDetector implements Listener {

    private final AntiAFKPlus plugin;
    private final Map<UUID, Long> lastBlockBreak = new HashMap<>();
    private final Map<UUID, Long> lastFishing = new HashMap<>();

    public AntiAFKActivityDetector(AntiAFKPlus plugin) {
        this.plugin = plugin;

        // Optional task to clear old records every X minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                long cutoff = System.currentTimeMillis() - 300_000; // 5 minutes
                lastBlockBreak.entrySet().removeIf(e -> e.getValue() < cutoff);
                lastFishing.entrySet().removeIf(e -> e.getValue() < cutoff);
            }
        }.runTaskTimer(plugin, 6000L, 6000L); // 5 mins
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!plugin.getAfkManager().isAFK(player)) return;

        // Check if player is repetitively mining in same direction
        Material type = event.getBlock().getType();
        if (type.isSolid()) {
            lastBlockBreak.put(uuid, System.currentTimeMillis());
            plugin.getAfkManager().unmarkManualAFKIfNeeded(player);

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] " + player.getName() + " broke a block while AFK - resetting AFK status.");
            }
        }
    }

    @EventHandler
    public void onFishing(PlayerFishEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!plugin.getAfkManager().isAFK(player)) return;

        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH ||
                event.getState() == PlayerFishEvent.State.FISHING) {

            lastFishing.put(uuid, System.currentTimeMillis());
            plugin.getAfkManager().unmarkManualAFKIfNeeded(player);

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] " + player.getName() + " performed fishing while AFK - resetting AFK status.");
            }
        }
    }
}
