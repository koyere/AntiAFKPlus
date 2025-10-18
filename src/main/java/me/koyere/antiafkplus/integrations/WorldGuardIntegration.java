package me.koyere.antiafkplus.integrations;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * WorldGuard integration via reflection (no compile-time dependency).
 * Provides region membership queries and simple zone resolution.
 */
public class WorldGuardIntegration {
    private final AntiAFKPlus plugin;
    private final boolean available;

    public WorldGuardIntegration(AntiAFKPlus plugin) {
        this.plugin = plugin;
        this.available = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
        if (available) {
            plugin.getLogger().info("WorldGuard detected: region-based zone management enabled.");
        } else {
            plugin.getLogger().info("WorldGuard not found: using config-only zones.");
        }
    }

    public boolean isAvailable() { return available; }

    /**
     * Returns region IDs at a given location. Empty if WorldGuard not available or on error.
     */
    public Set<String> getRegionIdsAt(Location loc) {
        if (!available || loc == null || loc.getWorld() == null) return Collections.emptySet();
        try {
            // Adapt Bukkit Location to WorldEdit Location
            Class<?> bukkitAdapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Method adapt = bukkitAdapter.getMethod("adapt", org.bukkit.Location.class);
            Object weLoc = adapt.invoke(null, loc);

            // WorldGuard access
            Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object wg = wgClass.getMethod("getInstance").invoke(null);
            Object platform = wgClass.getMethod("getPlatform").invoke(wg);
            Object container = platform.getClass().getMethod("getRegionContainer").invoke(platform);
            Object query = container.getClass().getMethod("createQuery").invoke(container);

            // Applicable regions
            Class<?> weLocationClass = Class.forName("com.sk89q.worldedit.util.Location");
            Object regionSet = query.getClass().getMethod("getApplicableRegions", weLocationClass).invoke(query, weLoc);

            // Iterate regions and collect IDs
            Set<String> ids = new HashSet<>();
            for (Object region : (Iterable<?>) regionSet.getClass().getMethod("getRegions").invoke(regionSet)) {
                String id = (String) region.getClass().getMethod("getId").invoke(region);
                if (id != null) ids.add(id.toLowerCase());
            }
            return ids;
        } catch (Throwable t) {
            plugin.getLogger().warning("WorldGuard reflection failed: " + t.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * Attempts to resolve the configured zone name for a location using WorldGuard regions.
     * It matches region IDs against keys under zone-management.zones.* (case-insensitive).
     */
    public String determineZoneAt(Location location) {
        if (location == null) return null;
        Set<String> regionIds = getRegionIdsAt(location);
        if (regionIds.isEmpty()) return null;
        var zonesSection = plugin.getConfig().getConfigurationSection("zone-management.zones");
        if (zonesSection == null) return null;
        for (String zoneKey : zonesSection.getKeys(false)) {
            if (regionIds.contains(zoneKey.toLowerCase())) {
                return zoneKey;
            }
            // Optional explicit mapping: zone-management.zones.<name>.region: <regionId>
            String explicit = plugin.getConfig().getString("zone-management.zones." + zoneKey + ".region", null);
            if (explicit != null && regionIds.contains(explicit.toLowerCase())) {
                return zoneKey;
            }
        }
        return null;
    }

    /**
     * Convenience wrapper for player-based checks.
     */
    public String determineZoneAt(Player player) {
        if (player == null) return null;
        return determineZoneAt(player.getLocation());
    }
}
