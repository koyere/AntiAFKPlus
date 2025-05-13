package me.koyere.antiafkplus.listener;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

/**
 * Detects repetitive clicking without movement, which may indicate autoclickers.
 * If a player is clicking rapidly but hasn't moved for a while, we log it and optionally
 * treat it as AFK based on the plugin configuration.
 */
public class AutoClickListener implements Listener {

    private final AntiAFKPlus plugin = AntiAFKPlus.getInstance();

    // Stores recent click timestamps per player
    private final Map<UUID, List<Long>> clickHistory = new HashMap<>();

    // Detection settings
    private static final int CLICK_WINDOW_MS = 5000;
    private static final int CLICK_THRESHOLD = 20; // clicks in 5 seconds
    private static final int MIN_IDLE_MS = 60000;  // 60s of inactivity

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Skip if detection is disabled or player has bypass
        if (!plugin.getConfigManager().isAutoclickDetectionEnabled()) return;
        if (player.hasPermission("antiafkplus.bypass")) return;

        long now = System.currentTimeMillis();

        // Track click timestamps
        clickHistory.putIfAbsent(uuid, new ArrayList<>());
        List<Long> history = clickHistory.get(uuid);
        history.add(now);

        // Remove clicks outside the detection window
        history.removeIf(timestamp -> timestamp < now - CLICK_WINDOW_MS);

        // Check if player has been idle
        long lastMovement = plugin.getAfkManager().getLastMovement(player);
        long timeIdle = now - lastMovement;

        if (timeIdle >= MIN_IDLE_MS && history.size() >= CLICK_THRESHOLD) {
            // Log suspicious activity
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("[AFK] Possible autoclicker detected for " + player.getName() +
                        ": " + history.size() + " clicks in last " + (CLICK_WINDOW_MS / 1000) + "s without movement.");
            }

            // Optionally mark as manual AFK (can be expanded to custom punishments)
            plugin.getAfkManager().clearPlayerData(player);
            plugin.getAfkManager().toggleManualAFK(player);
        }
    }
}
