package me.koyere.antiafkplus.utils;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bstats.bukkit.Metrics;

public class bStatsManager {

    private static final int PLUGIN_ID = 25664; // Official AntiAFKPlus plugin ID on bStats

    public bStatsManager(AntiAFKPlus plugin) {
        Metrics metrics = new Metrics(plugin, PLUGIN_ID);

        // Optional: You can add custom charts here later if you want.
    }
}
