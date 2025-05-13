package me.koyere.antiafkplus.afk;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.utils.AFKLogger;
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

    private final Set<UUID> afkPlayers = new HashSet<>();
    private final Map<UUID, Set<Integer>> warningsSent = new HashMap<>();
    private final Set<UUID> manualAfk = new HashSet<>();
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

                    if (player.hasPermission("antiafkplus.bypass")) continue;

                    List<String> disabled = plugin.getConfigManager().getDisabledWorlds();
                    List<String> enabled = plugin.getConfigManager().getEnabledWorlds();
                    String world = player.getWorld().getName();

                    if (disabled.contains(world)) continue;
                    if (!enabled.isEmpty() && !enabled.contains(world)) continue;

                    // Manual AFK time limit check
                    if (manualAfk.contains(uuid)) {
                        long limitMillis = plugin.getConfigManager().getMaxVoluntaryAfkTimeSeconds() * 1000L;
                        if (limitMillis > 0 && manualAfkStartTime.containsKey(uuid)) {
                            long since = manualAfkStartTime.get(uuid);
                            if ((System.currentTimeMillis() - since) >= limitMillis) {
                                manualAfk.remove(uuid);
                                manualAfkStartTime.remove(uuid);
                                player.sendMessage(plugin.getConfigManager().getMessageVoluntaryAFKLimit());
                                AFKLogger.logAFKExit(player, "manual (timeout)", (System.currentTimeMillis() - since) / 1000L);
                                unmarkAsAFK(player);
                                continue;
                            }
                        }
                        continue;
                    }

                    long lastMovement = movementListener.getLastMovement(player);
                    long afkThreshold = getPlayerAfkTime(player) * 1000L;
                    long timeSinceMovement = System.currentTimeMillis() - lastMovement;

                    if (timeSinceMovement >= afkThreshold) {
                        kickPlayer(player, timeSinceMovement);
                    } else {
                        checkWarnings(player, timeSinceMovement, afkThreshold);
                        if (afkPlayers.contains(uuid) && timeSinceMovement < afkThreshold) {
                            unmarkAsAFK(player);
                            AFKLogger.logAFKExit(player, "auto", timeSinceMovement / 1000L);
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
                    AFKLogger.logAFKWarning(player, secondsRemaining);
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

    private void kickPlayer(Player player, long afkTimeMillis) {
        afkPlayers.add(player.getUniqueId());
        player.kickPlayer(plugin.getConfigManager().getMessageKicked());
        warningsSent.remove(player.getUniqueId());
        AFKLogger.logAFKKick(player, afkTimeMillis / 1000L);
    }

    private void markAsAFK(Player player) {
        afkPlayers.add(player.getUniqueId());
        String message = ChatColor.YELLOW + player.getName() + " is now AFK.";
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(message);
        }
        AFKLogger.logAFKEnter(player, manualAfk.contains(player.getUniqueId()) ? "manual" : "auto");
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

    public boolean toggleManualAFK(Player player) {
        UUID uuid = player.getUniqueId();
        if (manualAfk.contains(uuid)) {
            manualAfk.remove(uuid);
            manualAfkStartTime.remove(uuid);
            AFKLogger.logAFKExit(player, "manual", -1);
            unmarkAsAFK(player);
            return false;
        } else {
            manualAfk.add(uuid);
            manualAfkStartTime.put(uuid, System.currentTimeMillis());
            markAsAFK(player);
            return true;
        }
    }

    public void unmarkManualAFKIfNeeded(Player player) {
        if (manualAfk.remove(player.getUniqueId())) {
            manualAfkStartTime.remove(player.getUniqueId());
            AFKLogger.logAFKExit(player, "manual", -1);
            unmarkAsAFK(player);
        }
    }

    public void clearPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        afkPlayers.remove(uuid);
        manualAfk.remove(uuid);
        manualAfkStartTime.remove(uuid);
        warningsSent.remove(uuid);
    }

    /**
     * Returns the timestamp of the last known movement of the player.
     * Used for %antiafkplus_afktime% placeholder.
     */
    public long getLastMovement(Player player) {
        return movementListener.getLastMovement(player);
    }
}
