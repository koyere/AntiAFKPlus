package me.koyere.antiafkplus.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI hook for AntiAFKPlus.
 * Supports:
 * %antiafkplus_status% => ACTIVE or AFK
 * %antiafkplus_afktime% => time in seconds since last movement
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
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String identifier) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) return "";

        Player player = Bukkit.getPlayer(offlinePlayer.getUniqueId());
        if (player == null) return "";

        switch (identifier.toLowerCase()) {
            case "status":
                return plugin.getAfkManager().isAFK(player) ? "AFK" : "ACTIVE";

            case "afktime":
                long last = plugin.getAfkManager().getLastMovement(player);
                long diff = (System.currentTimeMillis() - last) / 1000L;
                return String.valueOf(diff);

            default:
                return null;
        }
    }
}
