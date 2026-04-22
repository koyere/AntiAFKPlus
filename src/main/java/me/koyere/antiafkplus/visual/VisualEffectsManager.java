package me.koyere.antiafkplus.visual;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.events.PlayerAFKStateChangeEvent;
import me.koyere.antiafkplus.events.PlayerAFKStateChangeEvent.AFKState;
import me.koyere.antiafkplus.platform.PlatformScheduler;

/**
 * Manages visual indicators for AFK players including particles,
 * tab list prefixes, and name tag modifications.
 */
@SuppressWarnings("deprecation")
public class VisualEffectsManager implements Listener {

    private final AntiAFKPlus plugin;
    private final Map<UUID, String> originalTabNames = new ConcurrentHashMap<>();
    private final Map<UUID, String> originalDisplayNames = new ConcurrentHashMap<>();

    private PlatformScheduler.ScheduledTask particleTask;

    public VisualEffectsManager(AntiAFKPlus plugin) {
        this.plugin = plugin;

        // Register event listeners
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Start the repeating particle task (every 20 ticks = 1 second)
        startParticleTask();
    }

    // ============= Particle System =============

    /**
     * Starts the repeating task that spawns particles above AFK players.
     */
    private void startParticleTask() {
        particleTask = plugin.getPlatformScheduler().runTaskTimer(() -> {
            if (!plugin.getConfig().getBoolean("visual-effects.particles.enabled", false)) {
                return;
            }

            if (plugin.getAfkManager() == null) {
                return;
            }

            Set<UUID> afkUUIDs = plugin.getAfkManager().getAfkPlayerUUIDs();
            if (afkUUIDs.isEmpty()) {
                return;
            }

            String type = plugin.getConfig().getString("visual-effects.particles.type", "CLOUD");
            int count = plugin.getConfig().getInt("visual-effects.particles.count", 5);
            double offsetX = plugin.getConfig().getDouble("visual-effects.particles.offset-x", 0.3);
            double offsetY = plugin.getConfig().getDouble("visual-effects.particles.offset-y", 0.5);
            double offsetZ = plugin.getConfig().getDouble("visual-effects.particles.offset-z", 0.3);
            double speed = plugin.getConfig().getDouble("visual-effects.particles.speed", 0.02);

            Particle particle;
            try {
                particle = Particle.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid particle type in config: " + type);
                return;
            }

            for (UUID uuid : afkUUIDs) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    continue;
                }

                Location location = player.getLocation().add(0, 2.2, 0);
                try {
                    player.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to spawn particle for " + player.getName() + ": " + e.getMessage());
                }
            }
        }, 20L, 20L);
    }

    // ============= Event Handlers =============

    /**
     * Handles AFK state changes to update tab list and name tags.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAFKStateChange(PlayerAFKStateChangeEvent event) {
        Player player = event.getPlayer();
        AFKState toState = event.getToState();

        if (toState.name().startsWith("AFK")) {
            applyAFKVisuals(player);
        } else if (toState == AFKState.ACTIVE) {
            removeAFKVisuals(player);
        }
    }

    /**
     * Cleans up stored names when a player disconnects.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        originalTabNames.remove(uuid);
        originalDisplayNames.remove(uuid);
    }

    // ============= Tab List System =============

    /**
     * Applies the AFK prefix to the player's tab list name.
     */
    private void applyTabListPrefix(Player player) {
        if (!plugin.getConfig().getBoolean("visual-effects.tab-list.enabled", false)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        if (!originalTabNames.containsKey(uuid)) {
            String currentName = player.getPlayerListName();
            originalTabNames.put(uuid, currentName != null ? currentName : player.getName());
        }

        String prefix = plugin.getConfig().getString("visual-effects.tab-list.afk-prefix", "&7[AFK] ");
        String suffix = plugin.getConfig().getString("visual-effects.tab-list.afk-suffix", "");
        player.setPlayerListName(color(prefix) + player.getName() + color(suffix));
    }

    /**
     * Restores the player's original tab list name.
     */
    private void restoreTabListName(Player player) {
        if (!plugin.getConfig().getBoolean("visual-effects.tab-list.enabled", false)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        String originalName = originalTabNames.remove(uuid);
        if (originalName != null) {
            player.setPlayerListName(originalName);
        }
    }

    // ============= Name Tag System =============

    /**
     * Applies the AFK prefix to the player's display name.
     */
    private void applyNameTagPrefix(Player player) {
        if (!plugin.getConfig().getBoolean("visual-effects.name-tags.enabled", false)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        if (!originalDisplayNames.containsKey(uuid)) {
            String currentDisplay = player.getDisplayName();
            originalDisplayNames.put(uuid, currentDisplay != null ? currentDisplay : player.getName());
        }

        String prefix = plugin.getConfig().getString("visual-effects.name-tags.afk-prefix", "&7[AFK] ");
        String suffix = plugin.getConfig().getString("visual-effects.name-tags.afk-suffix", "");
        player.setDisplayName(color(prefix) + player.getName() + color(suffix));
    }

    /**
     * Restores the player's original display name.
     */
    private void restoreDisplayName(Player player) {
        if (!plugin.getConfig().getBoolean("visual-effects.name-tags.enabled", false)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        String originalDisplay = originalDisplayNames.remove(uuid);
        if (originalDisplay != null) {
            player.setDisplayName(originalDisplay);
        }
    }

    // ============= Combined Apply / Remove =============

    private void applyAFKVisuals(Player player) {
        applyTabListPrefix(player);
        applyNameTagPrefix(player);
    }

    private void removeAFKVisuals(Player player) {
        restoreTabListName(player);
        restoreDisplayName(player);
    }

    // ============= Lifecycle =============

    /**
     * Shuts down the visual effects manager, cancels tasks, and restores all modified names.
     */
    public void shutdown() {
        // Cancel the particle task
        if (particleTask != null && !particleTask.isCancelled()) {
            particleTask.cancel();
        }

        // Restore all modified player names
        for (Map.Entry<UUID, String> entry : originalTabNames.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                player.setPlayerListName(entry.getValue());
            }
        }
        for (Map.Entry<UUID, String> entry : originalDisplayNames.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                player.setDisplayName(entry.getValue());
            }
        }

        originalTabNames.clear();
        originalDisplayNames.clear();
    }

    // ============= Utility =============

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}
