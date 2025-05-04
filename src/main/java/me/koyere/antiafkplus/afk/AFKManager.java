package me.koyere.antiafkplus.afk;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Manages AFK detection and manual AFK mode.
 */
public class AFKManager {

    private final AntiAFKPlus plugin;
    private final MovementListener movementListener;

    // Players marked as AFK automatically (for kick detection)
    private final Set<UUID> afkPlayers = new HashSet<>();

    // Tracks warning times already sent to each player
    private final Map<UUID, Set<Integer>> warningsSent = new HashMap<>();

    // Players in voluntary /afk mode
    private final Set<UUID> manualAfk = new HashSet<>();

    // Tracks when each player entered manual AFK
    private final Map<UUID, Long> manualAfkStartTime = new HashMap<>();

    public AFKManager(AntiAFKPlus plugin, MovementListener movementListener) {
        this.plugin = plugin;
        this.movementListener = movementListener;
        startAFKCheckTask();
    }

    private void startAFKCheckTask() {
        int intervalTicks = plugin.getConfigManager().getAfkCheckIntervalSeconds() * 20;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();

                    // Bypass check
                    if (player.hasPermission("antiafkplus.bypass")) continue;

                    // Ignore if world is not enabled
                    if (!plugin.getConfigManager().getEnabledWorlds().isEmpty()
                            && !plugin.getConfigManager().getEnabledWorlds().contains(player.getWorld().getName())) {
                        continue;
                    }

                    // Voluntary AFK: check time limit
                    if (manualAfk.contains(uuid)) {
                        long limitMillis = plugin.getConfigManager().getMaxVoluntaryAfkTimeSeconds() * 1000L;
                        if (limitMillis > 0 && manualAfkStartTime.containsKey(uuid)) {
                            long since = manualAfkStartTime.get(uuid);
                            if ((System.currentTimeMillis() - since) >= limitMillis) {
                                manualAfk.remove(uuid);
                                manualAfkStartTime.remove(uuid);
                                player.sendMessage(plugin.getConfigManager().getMessageVoluntaryAFKLimit());
                                String message = ChatColor.GREEN + player.getName() + " is no longer AFK.";
                                for (Player online : Bukkit.getOnlinePlayers()) {
                                    online.sendMessage(message);
                                }
                                continue;
                            }
                        }
                        continue; // skip further AFK check while in manual AFK mode
                    }

                    long lastMovement = movementListener.getLastMovement(player);
                    long afkThreshold = getPlayerAfkTime(player) * 1000L;
                    long timeSinceMovement = System.currentTimeMillis() - lastMovement;

                    if (timeSinceMovement >= afkThreshold) {
                        kickPlayer(player);
                    } else {
                        checkWarnings(player, timeSinceMovement, afkThreshold);
                        if (afkPlayers.contains(uuid) && timeSinceMovement < afkThreshold) {
                            unmarkAsAFK(player);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    private void checkWarnings(Player player, long timeSinceMovement, long afkThreshold) {
        int secondsRemaining = (int) ((afkThreshold - timeSinceMovement) / 1000L);

        for (int warningTime : plugin.getConfigManager().getAfkWarningTimes()) {
            if (secondsRemaining <= warningTime) {
                Set<Integer> sentWarnings = warningsSent.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
                if (!sentWarnings.contains(warningTime)) {
                    player.sendMessage(plugin.getConfigManager().getMessageKickWarning()
                            .replace("{seconds}", String.valueOf(secondsRemaining)));
                    sentWarnings.add(warningTime);
                }
            }
        }
    }

    private long getPlayerAfkTime(Player player) {
        for (Map.Entry<String, Integer> entry : plugin.getConfigManager().getPermissionTimes().entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                return entry.getValue();
            }
        }
        return plugin.getConfigManager().getDefaultAfkTime();
    }

    private void kickPlayer(Player player) {
        afkPlayers.add(player.getUniqueId());
        player.kickPlayer(plugin.getConfigManager().getMessageKicked());
        warningsSent.remove(player.getUniqueId());
    }

    private void markAsAFK(Player player) {
        afkPlayers.add(player.getUniqueId());
        String message = ChatColor.YELLOW + player.getName() + " is now AFK.";
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(message);
        }
    }

    private void unmarkAsAFK(Player player) {
        afkPlayers.remove(player.getUniqueId());
        String message = ChatColor.GREEN + player.getName() + " is no longer AFK.";
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(message);
        }
    }

    public boolean isAFK(Player player) {
        return afkPlayers.contains(player.getUniqueId()) || manualAfk.contains(player.getUniqueId());
    }

    /**
     * Toggles manual AFK mode. Returns true if entered AFK, false if exited.
     */
    public boolean toggleManualAFK(Player player) {
        UUID uuid = player.getUniqueId();
        if (manualAfk.contains(uuid)) {
            manualAfk.remove(uuid);
            manualAfkStartTime.remove(uuid);
            String message = ChatColor.GREEN + player.getName() + " is no longer AFK.";
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage(message);
            }
            return false;
        } else {
            manualAfk.add(uuid);
            manualAfkStartTime.put(uuid, System.currentTimeMillis());
            String message = ChatColor.YELLOW + player.getName() + " is now AFK.";
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage(message);
            }
            return true;
        }
    }

    public void unmarkManualAFKIfNeeded(Player player) {
        if (manualAfk.remove(player.getUniqueId())) {
            manualAfkStartTime.remove(player.getUniqueId());
            String message = ChatColor.GREEN + player.getName() + " is no longer AFK.";
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage(message);
            }
        }
    }

    public void clearPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        afkPlayers.remove(uuid);
        manualAfk.remove(uuid);
        manualAfkStartTime.remove(uuid);
        warningsSent.remove(uuid);
    }
}
