package me.koyere.antiafkplus;

import me.koyere.antiafkplus.afk.AFKManager;
import me.koyere.antiafkplus.afk.MovementListener;
import me.koyere.antiafkplus.api.AntiAFKPlusAPI;
import me.koyere.antiafkplus.api.AntiAFKPlusAPIImpl;
import me.koyere.antiafkplus.command.AFKCommand;
import me.koyere.antiafkplus.command.AFKPlusCommand;
import me.koyere.antiafkplus.config.ConfigManager;
import me.koyere.antiafkplus.utils.bStatsManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class AntiAFKPlus extends JavaPlugin {

    private static AntiAFKPlus instance;
    private ConfigManager configManager;
    private MovementListener movementListener;
    private AFKManager afkManager;
    private AntiAFKPlusAPI api;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.movementListener = new MovementListener();
        this.afkManager = new AFKManager(this, movementListener);
        this.api = new AntiAFKPlusAPIImpl(this);

        getServer().getPluginManager().registerEvents(movementListener, this);
        getCommand("afkplus").setExecutor(new AFKPlusCommand(this));
        getCommand("afk").setExecutor(new AFKCommand(this));

        new bStatsManager(this);

        getLogger().info("AntiAFKPlus has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("AntiAFKPlus has been disabled!");
    }

    public static AntiAFKPlus getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public AFKManager getAfkManager() {
        return afkManager;
    }

    public AntiAFKPlusAPI getAPI() {
        return api;
    }
}
