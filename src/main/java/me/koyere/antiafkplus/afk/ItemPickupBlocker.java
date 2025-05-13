package me.koyere.antiafkplus.afk;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

/**
 * Prevents players from picking up items while they are AFK,
 * if the feature is enabled in config.
 */
public class ItemPickupBlocker implements Listener {

    private final AntiAFKPlus plugin;

    public ItemPickupBlocker(AntiAFKPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (shouldBlock(player)) {
            event.setCancelled(true);
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] Blocked item pickup for AFK player: " + player.getName());
            }
        }
    }

    @EventHandler
    public void onPlayerPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (shouldBlock(player)) {
            event.setCancelled(true);
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] Blocked item pickup for AFK player: " + player.getName());
            }
        }
    }

    private boolean shouldBlock(Player player) {
        return plugin.getConfigManager().isBlockPickupWhileAFK()
                && plugin.getAfkManager().isAFK(player);
    }
}
