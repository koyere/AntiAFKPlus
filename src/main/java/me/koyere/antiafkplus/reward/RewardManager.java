package me.koyere.antiafkplus.reward;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.events.PlayerAFKStateChangeEvent;
import me.koyere.antiafkplus.platform.PlatformScheduler;

/**
 * Executes configured commands and messages when a player has been AFK
 * for a specified number of minutes (reward-system.intervals in config.yml).
 *
 * Eligibility requirements (all must pass):
 *   - reward-system.enabled: true
 *   - Player was active for >= require-active-time-minutes before going AFK
 *   - Daily reward count < max-daily-rewards
 *   - If require-vault: true, Vault must be loaded
 */
public class RewardManager implements Listener {

    private final AntiAFKPlus plugin;

    // Per-session AFK tracking (cleared when player goes active or disconnects)
    private final Map<UUID, Long> afkStartTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Integer>> firedThresholds = new ConcurrentHashMap<>();

    // Active-time tracking (to enforce require-active-time-minutes)
    private final Map<UUID, Long> activeStartTimes = new ConcurrentHashMap<>();

    // Daily reward counts; reset when the calendar date changes
    private final Map<UUID, Integer> dailyRewardCount = new ConcurrentHashMap<>();
    private final Map<UUID, LocalDate> lastRewardDate = new ConcurrentHashMap<>();

    private PlatformScheduler.ScheduledTask rewardTask;

    public RewardManager(AntiAFKPlus plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // Seed active-start times for players already online (e.g. on /reload)
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            activeStartTimes.put(p.getUniqueId(), now);
        }
        startRewardTask();
    }

    private void startRewardTask() {
        // Check every 60 seconds (1200 ticks at 20 TPS)
        this.rewardTask = plugin.getPlatformScheduler().runTaskTimer(this::checkRewards, 1200L, 1200L);
    }

    // ===================== Event Handlers =====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAFKStateChange(PlayerAFKStateChangeEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (event.isEnteringAFK()) {
            long requiredMs = plugin.getConfig()
                    .getLong("reward-system.require-active-time-minutes", 30) * 60_000L;
            long activeMs = now - activeStartTimes.getOrDefault(uuid, now);

            if (activeMs >= requiredMs) {
                afkStartTimes.put(uuid, now);
                firedThresholds.put(uuid, new HashSet<>());
            }
            // Not enough active time → no reward session started

        } else if (event.isLeavingAFK()) {
            // Record when they went active so the next AFK session can measure active time
            activeStartTimes.put(uuid, now);
            afkStartTimes.remove(uuid);
            firedThresholds.remove(uuid);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        activeStartTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        afkStartTimes.remove(uuid);
        firedThresholds.remove(uuid);
        activeStartTimes.remove(uuid);
    }

    // ===================== Reward Logic =====================

    private void checkRewards() {
        if (!plugin.getConfig().getBoolean("reward-system.enabled", false)) {
            return;
        }
        if (plugin.getConfig().getBoolean("reward-system.require-vault", false)
                && plugin.getVaultIntegration() == null) {
            return;
        }

        int maxDaily = plugin.getConfig().getInt("reward-system.max-daily-rewards", 144);
        LocalDate today = LocalDate.now();
        long now = System.currentTimeMillis();
        List<String> sortedKeys = getSortedIntervalKeys();

        for (UUID uuid : new ArrayList<>(afkStartTimes.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                afkStartTimes.remove(uuid);
                firedThresholds.remove(uuid);
                continue;
            }

            // Daily reset when the calendar date has changed
            LocalDate lastDate = lastRewardDate.get(uuid);
            if (lastDate != null && !lastDate.equals(today)) {
                dailyRewardCount.put(uuid, 0);
            }

            int given = dailyRewardCount.getOrDefault(uuid, 0);
            if (given >= maxDaily) {
                continue;
            }

            long afkMinutes = (now - afkStartTimes.get(uuid)) / 60_000L;
            Set<Integer> fired = firedThresholds.computeIfAbsent(uuid, k -> new HashSet<>());

            for (String key : sortedKeys) {
                int threshold;
                try {
                    threshold = Integer.parseInt(key);
                } catch (NumberFormatException e) {
                    continue;
                }

                if (afkMinutes >= threshold && !fired.contains(threshold)) {
                    fired.add(threshold);
                    given++;
                    dailyRewardCount.put(uuid, given);
                    lastRewardDate.put(uuid, today);
                    executeReward(player, "reward-system.intervals." + key);
                    if (given >= maxDaily) {
                        break;
                    }
                }
            }
        }
    }

    private List<String> getSortedIntervalKeys() {
        var section = plugin.getConfig().getConfigurationSection("reward-system.intervals");
        if (section == null) {
            return List.of();
        }
        List<String> keys = new ArrayList<>(section.getKeys(false));
        keys.sort((a, b) -> {
            try {
                return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
            } catch (NumberFormatException e) {
                return a.compareTo(b);
            }
        });
        return keys;
    }

    private void executeReward(Player player, String configPath) {
        String name = player.getName();
        String uuid = player.getUniqueId().toString();

        for (String cmd : plugin.getConfig().getStringList(configPath + ".commands")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    cmd.replace("{player}", name).replace("{uuid}", uuid));
        }

        for (String rawMsg : plugin.getConfig().getStringList(configPath + ".messages")) {
            String formatted = ChatColor.translateAlternateColorCodes('&',
                    rawMsg.replace("{player}", name).replace("{uuid}", uuid));
            player.sendMessage(formatted);
        }
    }

    // ===================== Lifecycle =====================

    public void shutdown() {
        if (rewardTask != null && !rewardTask.isCancelled()) {
            rewardTask.cancel();
        }
        afkStartTimes.clear();
        activeStartTimes.clear();
        firedThresholds.clear();
        dailyRewardCount.clear();
        lastRewardDate.clear();
    }
}
