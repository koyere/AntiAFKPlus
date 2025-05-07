package me.koyere.antiafkplus.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI hook for AntiAFKPlus.
 * Registers %antiafkplus_status%: returns "AFK" or "ACTIVE".
 */
public class PlaceholderHook extends PlaceholderExpansion {

    private final AntiAFKPlus plugin;

    public PlaceholderHook(AntiAFKPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "antiafkplus";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // Prevent unregistration on reload
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";

        if (identifier.equalsIgnoreCase("status")) {
            boolean isAfk = plugin.getAfkManager().isAFK(player);
            return isAfk ? "AFK" : "ACTIVE";
        }

        return null;
    }
}
