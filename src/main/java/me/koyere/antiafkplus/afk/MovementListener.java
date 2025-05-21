// MovementListener.java - English comments
package me.koyere.antiafkplus.afk;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MovementListener implements Listener {

    private final Map<UUID, Long> lastMovementTime = new HashMap<>();

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
        updateLastMovementTimestamp(player);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("antiafkplus.bypass")) return;

        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ() ||
                Math.abs(event.getFrom().getY() - event.getTo().getY()) > 0.1) {
            if (AntiAFKPlus.getInstance() != null && AntiAFKPlus.getInstance().getAfkManager() != null) {
                getAfkManager().onPlayerActivity(player);
            }
            updateLastMovementTimestamp(player);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        AntiAFKPlus plugin = AntiAFKPlus.getInstance();
        if (plugin == null || !plugin.isEnabled()) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.hasPermission("antiafkplus.bypass")) return;
            if (plugin.getAfkManager() != null) { // Check again inside sync task
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (AntiAFKPlus.getInstance() != null && AntiAFKPlus.getInstance().getAfkManager() != null) {
            getAfkManager().clearPlayerData(event.getPlayer());
        }
        this.lastMovementTime.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        if (event.isCancelled()) return;
        if (AntiAFKPlus.getInstance() != null && AntiAFKPlus.getInstance().getAfkManager() != null) {
            getAfkManager().clearPlayerData(event.getPlayer());
        }
        this.lastMovementTime.remove(event.getPlayer().getUniqueId());
    }

    private void updateLastMovementTimestamp(Player player) {
        lastMovementTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public long getLastMovementTimestamp(Player player) {
        return lastMovementTime.getOrDefault(player.getUniqueId(), System.currentTimeMillis());
    }
}