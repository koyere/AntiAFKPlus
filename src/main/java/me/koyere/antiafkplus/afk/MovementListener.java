package me.koyere.antiafkplus.afk;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MovementListener implements Listener {

    private final Map<UUID, Long> lastMovement = new HashMap<>();

    public MovementListener() {}

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        AntiAFKPlus.getInstance().getAfkManager().clearPlayerData(player); // Refuerzo
        updateLastMovement(player); // ⚠️ FIX: actualiza tiempo al entrar
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("antiafkplus.bypass")) return;

        if (hasMoved(event)) {
            AntiAFKPlus.getInstance().getAfkManager().unmarkManualAFKIfNeeded(player);
            updateLastMovement(player);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("antiafkplus.bypass")) return;

        AntiAFKPlus.getInstance().getAfkManager().unmarkManualAFKIfNeeded(player);
        updateLastMovement(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (player.hasPermission("antiafkplus.bypass")) return;

            AntiAFKPlus.getInstance().getAfkManager().unmarkManualAFKIfNeeded(player);
            updateLastMovement(player);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("antiafkplus.bypass")) return;

        AntiAFKPlus.getInstance().getAfkManager().unmarkManualAFKIfNeeded(player);
        updateLastMovement(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        AntiAFKPlus.getInstance().getAfkManager().clearPlayerData(event.getPlayer());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        AntiAFKPlus.getInstance().getAfkManager().clearPlayerData(event.getPlayer());
    }

    private void updateLastMovement(Player player) {
        lastMovement.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private boolean hasMoved(PlayerMoveEvent event) {
        return event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ();
    }

    public long getLastMovement(Player player) {
        return lastMovement.getOrDefault(player.getUniqueId(), System.currentTimeMillis());
    }
}
