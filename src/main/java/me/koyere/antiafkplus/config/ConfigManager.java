package me.koyere.antiafkplus.config;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ConfigManager {

    private final AntiAFKPlus plugin;
    private FileConfiguration config;

    private int defaultAfkTime;
    private int afkCheckIntervalSeconds;
    private List<Integer> afkWarningTimes = new ArrayList<>();
    private Map<String, Integer> permissionTimes = new HashMap<>();
    private List<String> enabledWorlds;
    private String messageAfkEnter;
    private String messageAfkExit;
    private String messageKickWarning;
    private String messageKicked;

    public ConfigManager(AntiAFKPlus plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        this.defaultAfkTime = config.getInt("default-afk-time", 300);
        this.afkCheckIntervalSeconds = config.getInt("afk-check-interval-seconds", 5);

        this.afkWarningTimes.clear();
        if (config.isList("afk-warnings")) {
            for (Object obj : config.getList("afk-warnings")) {
                if (obj instanceof Integer) {
                    afkWarningTimes.add((Integer) obj);
                }
            }
        }

        this.permissionTimes.clear();
        if (config.isConfigurationSection("permission-times")) {
            for (String key : config.getConfigurationSection("permission-times").getKeys(false)) {
                int time = config.getInt("permission-times." + key, 300);
                this.permissionTimes.put(key, time);
            }
        }

        this.enabledWorlds = config.getStringList("enabled-worlds");

        this.messageAfkEnter = color(config.getString("messages.afk-enter", "&e{player} is now AFK."));
        this.messageAfkExit = color(config.getString("messages.afk-exit", "&a{player} is no longer AFK."));
        this.messageKickWarning = color(config.getString("messages.kick-warning", "&cYou will be kicked soon for being AFK!"));
        this.messageKicked = color(config.getString("messages.kicked", "&cYou have been kicked for being AFK."));
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public int getDefaultAfkTime() {
        return defaultAfkTime;
    }

    public int getAfkCheckIntervalSeconds() {
        return afkCheckIntervalSeconds;
    }

    public List<Integer> getAfkWarningTimes() {
        return afkWarningTimes;
    }

    public Map<String, Integer> getPermissionTimes() {
        return permissionTimes;
    }

    public List<String> getEnabledWorlds() {
        return enabledWorlds;
    }

    public String getMessageAfkEnter() {
        return messageAfkEnter;
    }

    public String getMessageAfkExit() {
        return messageAfkExit;
    }

    public String getMessageKickWarning() {
        return messageKickWarning;
    }

    public String getMessageKicked() {
        return messageKicked;
    }
}
