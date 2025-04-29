package me.koyere.antiafkplus.api;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.entity.Player;

/**
 * Internal implementation of the AntiAFKPlus API.
 */
public class AntiAFKPlusAPIImpl implements AntiAFKPlusAPI {

    private final AntiAFKPlus plugin;

    public AntiAFKPlusAPIImpl(AntiAFKPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isAFK(Player player) {
        return plugin.getAfkManager().isAFK(player);
    }

    @Override
    public void markAsAFK(Player player) {
        plugin.getAfkManager().toggleManualAFK(player); // Activamos modo manual si no estaba.
    }

    @Override
    public void unmarkAFK(Player player) {
        plugin.getAfkManager().unmarkManualAFKIfNeeded(player);
    }
}
