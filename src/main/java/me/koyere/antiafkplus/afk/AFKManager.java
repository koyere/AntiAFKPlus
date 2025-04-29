package me.koyere.antiafkplus.afk;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class AFKManager {

    private final AntiAFKPlus plugin;
    private final MovementListener movementListener;
    private final Set<UUID> afkPlayers = new HashSet<>();
    private final Map<UUID, Set<Integer>> warningsSent = new HashMap<>();
    private final Set<UUID> manualAfk = new HashSet<>();

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
                    if (player.hasPermission("antiafkplus.bypass") || manualAfk.contains(player.getUniqueId())) {
                        continue;
                    }

                    if (!plugin.getConfigManager().getEnabledWorlds().isEmpty()
                            && !plugin.getConfigManager().getEnabledWorlds().contains(player.getWorld().getName())) {
                        continue;
                    }

                    long lastMovement = movementListener.getLastMovement(player);
                    long afkThreshold = getPlayerAfkTime(player) * 1000L;
                    long timeSinceMovement = System.currentTimeMillis() - lastMovement;

                    if (timeSinceMovement >= afkThreshold) {
                        kickPlayer(player);
                    } else {
                        checkWarnings(player, timeSinceMovement, afkThreshold);
                        if (afkPlayers.contains(player.getUniqueId()) && timeSinceMovement < afkThreshold) {
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
        return afkPlayers.contains(player.getUniqueId());
    }

    public boolean toggleManualAFK(Player player) {
        UUID uuid = player.getUniqueId();
        if (manualAfk.contains(uuid)) {
            manualAfk.remove(uuid);
            return false;
        } else {
            manualAfk.add(uuid);
            return true;
        }
    }

    public void unmarkManualAFKIfNeeded(Player player) {
        if (manualAfk.remove(player.getUniqueId())) {
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
        warningsSent.remove(uuid);
    }
}
