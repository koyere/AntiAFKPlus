// AFKManager.java - English comments
package me.koyere.antiafkplus.afk;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.utils.AFKLogger;
import org.bukkit.Bukkit;
// import org.bukkit.ChatColor; // Not used directly, messages should come from ConfigManager
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
// ConcurrentHashMap not strictly needed if all modifications happen on main thread.

/**
 * Manages AFK detection, state, and voluntary AFK mode.
 * Also handles warnings and kicking players based on inactivity.
 */
public class AFKManager {

    private final AntiAFKPlus plugin;
    private final MovementListener movementListener;

    private final Set<UUID> afkPlayers = new HashSet<>(); // Players currently marked as AFK (auto or manual)
    private final Map<UUID, Set<Integer>> warningsSent = new HashMap<>(); // Tracks warnings sent to avoid spam
    private final Set<UUID> manualAfkUsernames = new HashSet<>(); // Players who used /afk command (stores UUID)
    private final Map<UUID, Long> manualAfkStartTimes = new HashMap<>(); // Tracks when manual AFK started

    private BukkitTask afkCheckTask;

    public AFKManager(AntiAFKPlus plugin, MovementListener movementListener) {
        this.plugin = plugin;
        this.movementListener = movementListener;
        startAFKCheckTask();
    }

    private void startAFKCheckTask() {
        if (this.afkCheckTask != null && !this.afkCheckTask.isCancelled()) {
            this.afkCheckTask.cancel();
        }

        int intervalTicks = plugin.getConfigManager().getAfkCheckIntervalSeconds() * 20;
        if (intervalTicks <= 0) {
            plugin.getLogger().warning("AFK Check Interval (afk-check-interval) must be > 0. AFK check task will not start.");
            return;
        }

        this.afkCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();

                    if (player.hasPermission("antiafkplus.bypass")) {
                        if (afkPlayers.contains(uuid) || manualAfkUsernames.contains(uuid)) {
                            AFKLogger.logActivity(player.getName() + " has bypass, ensuring AFK state is cleared.");
                            // Use forceSetManualAFKState to ensure proper unmarking and logging
                            forceSetManualAFKState(player, false);
                        }
                        continue;
                    }

                    List<String> disabledWorlds = plugin.getConfigManager().getDisabledWorlds();
                    List<String> enabledWorlds = plugin.getConfigManager().getEnabledWorlds();
                    String currentWorldName = player.getWorld().getName();

                    if (disabledWorlds.contains(currentWorldName)) continue;
                    if (!enabledWorlds.isEmpty() && !enabledWorlds.contains(currentWorldName)) continue;

                    if (manualAfkUsernames.contains(uuid)) {
                        long voluntaryAfkLimitMillis = plugin.getConfigManager().getMaxVoluntaryAfkTimeSeconds() * 1000L;
                        if (voluntaryAfkLimitMillis > 0 && manualAfkStartTimes.containsKey(uuid)) {
                            long manualAfkDurationMillis = System.currentTimeMillis() - manualAfkStartTimes.get(uuid);
                            if (manualAfkDurationMillis >= voluntaryAfkLimitMillis) {
                                player.sendMessage(plugin.getConfigManager().getMessageVoluntaryAFKLimit());
                                // forceSetManualAFKState handles logging and unmarking correctly
                                forceSetManualAFKState(player, false); // Timed out from manual AFK
                            }
                        }
                        continue; // Skip auto AFK checks if manually AFK and not timed out
                    }

                    long lastMovementTimestamp = movementListener.getLastMovementTimestamp(player);
                    long afkThresholdMillis = getPlayerAfkTime(player) * 1000L;
                    long timeSinceLastMovementMillis = System.currentTimeMillis() - lastMovementTimestamp;

                    if (timeSinceLastMovementMillis >= afkThresholdMillis) {
                        if (!afkPlayers.contains(uuid)) {
                            markAsAFKInternal(player, "auto"); // Renamed internal method
                        }
                        kickPlayerAfterAFK(player, timeSinceLastMovementMillis);
                    } else {
                        checkWarnings(player, timeSinceLastMovementMillis, afkThresholdMillis);
                        if (afkPlayers.contains(uuid)) {
                            AFKLogger.logAFKExit(player, "auto (movement)", timeSinceLastMovementMillis / 1000L);
                            unmarkAsAFKInternal(player); // Renamed internal method
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
        plugin.getLogger().info("AFK check task started with an interval of " + (intervalTicks / 20) + " seconds.");
    }

    private void checkWarnings(Player player, long timeSinceMovementMillis, long afkThresholdMillis) {
        int secondsRemaining = Math.max(0, (int) ((afkThresholdMillis - timeSinceMovementMillis) / 1000L));
        for (int warningTimeSeconds : plugin.getConfigManager().getAfkWarningTimes()) {
            if (secondsRemaining <= warningTimeSeconds) {
                Set<Integer> sentPlayerWarnings = warningsSent.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
                if (!sentPlayerWarnings.contains(warningTimeSeconds)) {
                    String message = plugin.getConfigManager().getMessageKickWarning()
                            .replace("{seconds}", String.valueOf(secondsRemaining));
                    player.sendMessage(message);
                    AFKLogger.logAFKWarning(player, secondsRemaining);
                    sentPlayerWarnings.add(warningTimeSeconds);
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

    private void kickPlayerAfterAFK(Player player, long afkTimeMillis) {
        String kickMessage = plugin.getConfigManager().getMessageKicked();
        if (player.isOnline()) {
            player.kickPlayer(kickMessage);
            AFKLogger.logAFKKick(player, afkTimeMillis / 1000L);
        }
    }

    /**
     * Internal method to mark a player as AFK. Handles adding to sets and broadcasting.
     * @param player The player.
     * @param reason The reason (e.g., "auto", "manual (command)", "manual (API)").
     */
    private void markAsAFKInternal(Player player, String reason) {
        UUID uuid = player.getUniqueId();
        if (afkPlayers.contains(uuid)) {
            // If already in afkPlayers, but reason changes (e.g. from auto to manual via API), log new reason
            if (reason.contains("manual") && !manualAfkUsernames.contains(uuid)) {
                // If now being marked as manual, ensure it's logged if it wasn't before.
                // This specific case is more complex and handled by forceSetManualAFKState or toggle.
            } else if (reason.contains("manual") && manualAfkUsernames.contains(uuid)){
                // Already manually AFK, and markAsAFKInternal called again for manual. It's fine.
            } else {
                return; // Already AFK with the same broad category (e.g. auto and called again for auto)
            }
        }

        afkPlayers.add(uuid);
        warningsSent.remove(uuid);

        String afkBroadcastMessage = plugin.getConfigManager().getMessagePlayerNowAFK()
                .replace("{player}", player.getName());

        if(plugin.getConfigManager().shouldBroadcastAFKStateChanges()){
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.sendMessage(afkBroadcastMessage);
            }
        }
        AFKLogger.logAFKEnter(player, reason);
    }

    /**
     * Internal method to unmark a player from AFK. Handles removing from sets and broadcasting.
     * @param player The player.
     */
    private void unmarkAsAFKInternal(Player player) {
        UUID uuid = player.getUniqueId();
        if (!afkPlayers.remove(uuid)) {
            return; // Player was not in the general AFK set.
        }

        // These are typically handled by the methods that trigger unmarking (toggle, onPlayerActivity, forceSet)
        // manualAfkUsernames.remove(uuid);
        // manualAfkStartTimes.remove(uuid);
        warningsSent.remove(uuid);

        String notAfkBroadcastMessage = plugin.getConfigManager().getMessagePlayerNoLongerAFK()
                .replace("{player}", player.getName());

        if(plugin.getConfigManager().shouldBroadcastAFKStateChanges()){
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.sendMessage(notAfkBroadcastMessage);
            }
        }
        // AFKLogger.logAFKExit is called by the specific triggering methods (toggle, onPlayerActivity, forceSet)
        // as they have more context (like duration and specific reason).
    }

    public boolean isAFK(Player player) {
        if (player == null) return false;
        return afkPlayers.contains(player.getUniqueId());
    }

    /**
     * Checks if a player is specifically in manual AFK mode.
     * @param player The player to check.
     * @return True if the player is in manual AFK mode, false otherwise.
     */
    public boolean isManuallyAFK(Player player) {
        if (player == null) return false;
        return manualAfkUsernames.contains(player.getUniqueId());
    }

    public boolean toggleManualAFK(Player player) {
        UUID uuid = player.getUniqueId();
        if (manualAfkUsernames.contains(uuid)) { // Is manually AFK, turn it off
            forceSetManualAFKState(player, false); // Use the new method for consistency
            return false;
        } else { // Not manually AFK (or not AFK at all), turn it on
            if (isAFK(player) && !manualAfkUsernames.contains(uuid)) { // Is AFK (auto) but not manually
                player.sendMessage(plugin.getConfigManager().getMessageAlreadyAFK() + " (Switching to manual AFK mode).");
            } else if (isAFK(player)) { // Already manually AFK (should have been caught by first if, but defensive)
                player.sendMessage(plugin.getConfigManager().getMessageAlreadyAFK());
            }
            forceSetManualAFKState(player, true); // Use the new method for consistency
            return true;
        }
    }

    /**
     * Forces a player's manual AFK state.
     * @param player The player.
     * @param setAfk True to set as manually AFK, false to remove from AFK.
     */
    public void forceSetManualAFKState(Player player, boolean setAfk) {
        UUID uuid = player.getUniqueId();
        String apiReason = setAfk ? "manual (API)" : "manual (API activity/unmark)";

        if (setAfk) {
            boolean wasAlreadyManuallyAfk = manualAfkUsernames.contains(uuid);
            manualAfkUsernames.add(uuid); // Ensure they are in the manual set
            if (!manualAfkStartTimes.containsKey(uuid)) { // Only set start time if not already manually AFK
                manualAfkStartTimes.put(uuid, System.currentTimeMillis());
            }
            markAsAFKInternal(player, wasAlreadyManuallyAfk ? "manual (API - refresh)" : apiReason);
        } else {
            boolean wasManuallyAfk = manualAfkUsernames.remove(uuid);
            manualAfkStartTimes.remove(uuid);
            boolean wasGenerallyAfk = afkPlayers.contains(uuid); // Check before unmarkAsAFKInternal

            unmarkAsAFKInternal(player); // This removes from afkPlayers set and broadcasts

            if (wasManuallyAfk) {
                AFKLogger.logAFKExit(player, apiReason, -1);
            } else if (wasGenerallyAfk) { // Was AFK (e.g. auto) and API unmarks
                AFKLogger.logAFKExit(player, "auto (API unmark)", -1);
            }
            // If not AFK at all and unmark called, nothing really happens or logs, which is fine.
        }
    }


    public void onPlayerActivity(Player player) {
        UUID uuid = player.getUniqueId();
        boolean wasManuallyAfk = manualAfkUsernames.contains(uuid);

        if (wasManuallyAfk) {
            // Use forceSetManualAFKState to correctly handle unmarking
            forceSetManualAFKState(player, false); // Pass false to unmark
        }
        // If automatically AFK, the main AFKCheckTask will handle it based on updated movement time.
        // No need to call unmarkAsAFKInternal here for auto-AFK directly, as MovementListener
        // updates the timestamp, and the check task re-evaluates.
    }

    public void clearPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        afkPlayers.remove(uuid);
        manualAfkUsernames.remove(uuid);
        manualAfkStartTimes.remove(uuid);
        warningsSent.remove(uuid);
        AFKLogger.logActivity(player.getName() + "'s AFK data cleared.");
    }

    public long getLastMovementTimestampForPlayer(Player player) {
        if (player == null || movementListener == null) return System.currentTimeMillis(); // Defensive
        return movementListener.getLastMovementTimestamp(player);
    }

    public void shutdown() {
        plugin.getLogger().info("Shutting down AFKManager tasks...");
        if (this.afkCheckTask != null && !this.afkCheckTask.isCancelled()) {
            this.afkCheckTask.cancel();
            this.afkCheckTask = null;
            plugin.getLogger().info("AFK check task successfully cancelled.");
        } else {
            plugin.getLogger().info("AFK check task was not running or already cancelled.");
        }
    }
}