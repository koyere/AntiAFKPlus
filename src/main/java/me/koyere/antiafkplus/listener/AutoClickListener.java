// AutoClickListener.java - English comments
package me.koyere.antiafkplus.listener;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.afk.AFKManager;
import me.koyere.antiafkplus.config.ConfigManager;
import me.koyere.antiafkplus.utils.AFKLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import me.koyere.antiafkplus.platform.PlatformScheduler;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Detects repetitive clicking without significant movement, which may indicate autoclickers.
 * This is an experimental feature.
 */
public class AutoClickListener implements Listener {

    private final AntiAFKPlus plugin;
    private final AFKManager afkManager;
    private final ConfigManager configManager;

    private final Map<UUID, List<Long>> clickHistory = new HashMap<>();

    private int clickWindowMs;
    private int clickThreshold;
    private long minIdleTimeToTriggerMs;
    private String autoclickAction;

    private PlatformScheduler.ScheduledTask cleanupTask;

    public AutoClickListener(AntiAFKPlus plugin) {
        this.plugin = plugin;
        this.afkManager = plugin.getAfkManager();
        this.configManager = plugin.getConfigManager();
        loadConfigurableSettings();

        Runnable cleanupBody = () -> {
            long veryOldCutoff = System.currentTimeMillis() - (3600_000 * 6); // 6 hours
            clickHistory.entrySet().removeIf(entry -> {
                List<Long> timestamps = entry.getValue();
                if (timestamps.isEmpty()) return true;
                return timestamps.get(timestamps.size() - 1) < veryOldCutoff && timestamps.size() < (configManager.getAutoclickClickThreshold() * 2);
            });
        };
        this.cleanupTask = plugin.getPlatformScheduler()
                .runTaskTimerAsync(cleanupBody, 20L * 60 * 30, 20L * 60 * 30);
    }

    private void loadConfigurableSettings() {
        // Use the new getters from ConfigManager
        this.clickWindowMs = configManager.getAutoclickClickWindowMs();
        this.clickThreshold = configManager.getAutoclickClickThreshold();
        this.minIdleTimeToTriggerMs = configManager.getAutoclickMinIdleTimeMs();
        this.autoclickAction = configManager.getAutoclickAction().toUpperCase(); // Ensure it's uppercase for switch
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // The main toggle configManager.isAutoclickDetectionEnabled() is checked in AntiAFKPlus before registering this listener.
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (player.hasPermission("antiafkplus.bypass.autoclick")) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        List<Long> history = clickHistory.computeIfAbsent(uuid, k -> new ArrayList<>());
        history.add(currentTime);
        history.removeIf(timestamp -> timestamp < currentTime - this.clickWindowMs);

        long lastMovementTimestamp = afkManager.getLastMovementTimestampForPlayer(player);
        long timeIdleMs = currentTime - lastMovementTimestamp;

        if (timeIdleMs >= this.minIdleTimeToTriggerMs && history.size() >= this.clickThreshold) {
            String playerName = player.getName();
            String logMessage = "[AutoClick] Suspicious activity for " + playerName +
                    ": " + history.size() + " clicks in " + (this.clickWindowMs / 1000) + "s " +
                    "while idle for " + (timeIdleMs / 1000) + "s.";

            AFKLogger.logActivity(logMessage);
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().warning(logMessage);
            }

            switch (this.autoclickAction) {
                case "SET_AFK":
                    afkManager.forceSetManualAFKState(player, true);
                    player.sendMessage(configManager.getMessageAutoclickSetAfk()); // Use new getter
                    history.clear();
                    break;
                case "KICK":
                    final String kickReason = configManager.getMessageAutoclickKickReason();
                    plugin.getPlatformScheduler().runTaskForEntity(player, () -> {
                        if (player.isOnline()) {
                            player.kickPlayer(kickReason);
                        }
                    });
                    history.clear();
                    break;
                case "LOG":
                    // Consider a player message even for LOG, if desired.
                    // player.sendMessage(configManager.getMessage("autoclick-detected-logged", "&7Suspicious clicking activity has been logged."));
                    break;
                case "NONE":
                default:
                    break;
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clickHistory.remove(event.getPlayer().getUniqueId());
    }

    public void shutdown() {
        if (this.cleanupTask != null && !this.cleanupTask.isCancelled()) {
            this.cleanupTask.cancel();
            plugin.getLogger().info("AutoClickListener cleanup task cancelled.");
        }
        clickHistory.clear();
    }

    public void reloadDetectorConfig() {
        loadConfigurableSettings();
        plugin.getLogger().info("AutoClickListener settings reloaded.");
    }
}
