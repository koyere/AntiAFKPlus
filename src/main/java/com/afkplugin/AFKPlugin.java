package com.afkplugin;

import com.afkplugin.commands.AFKCheckCommand;
import com.afkplugin.commands.AFKReloadCommand;
import com.afkplugin.listeners.AntiAFKListener;
import com.afkplugin.listeners.PlayerListener;
import com.afkplugin.managers.AFKManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class AFKPlugin extends JavaPlugin {
    public static AFKPlugin instance;
    private AFKManager afkManager;
    private FileConfiguration messages;

    @Override
    public void onEnable() {
        instance = this;
        // Save default configs if not present
        saveDefaultConfig();
        saveResource("config_en.yml", false);
        saveResource("messages.yml", false);
        saveResource("messages_en.yml", false);

        // Load language file based on config
        String lang = getConfig().getString("language", "es");
        messages = YamlConfiguration.loadConfiguration(
            new File(getDataFolder(), "messages_" + lang + ".yml")
        );

        afkManager = new AFKManager(this);
        getServer().getPluginManager().registerEvents(new AntiAFKListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getCommand("afkcheck").setExecutor(new AFKCheckCommand(this));
        getCommand("afkreload").setExecutor(new AFKReloadCommand(this));
        getLogger().info("AFKPlugin enabled for Minecraft 1.18+ with language: " + lang);
    }

    @Override
    public void onDisable() {
        getLogger().info("AFKPlugin disabled.");
    }

    public static AFKPlugin getInstance() {
        return instance;
    }

    public AFKManager getAfkManager() {
        return afkManager;
    }

    public FileConfiguration getMessages() {
        return messages;
    }
}
