package me.koyere.antiafkplus.config;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * Handles loading and providing access to config and messages.
 */
public class ConfigManager {

    private final AntiAFKPlus plugin;
    private FileConfiguration config;
    private FileConfiguration messages;

    private int defaultAfkTime;
    private int afkCheckIntervalSeconds;
    private int maxVoluntaryAfkTimeSeconds;

    private boolean debugEnabled;
    private boolean blockPickupWhileAFK;
    private boolean autoclickDetectionEnabled;

    private String listCommandPermission;

    private List<Integer> afkWarningTimes = new ArrayList<>();
    private Map<String, Integer> permissionTimes = new HashMap<>();
    private List<String> enabledWorlds;
    private List<String> disabledWorlds;

    private String messageAfkEnter;
    private String messageAfkExit;
    private String messageKickWarning;
    private String messageKicked;
    private String messageVoluntaryAFKLimit;

    private String messageStatusSelf;
    private String messageStatusAFK;
    private String messageStatusActive;

    public ConfigManager(AntiAFKPlus plugin) {
        this.plugin = plugin;
        loadConfig();
        loadMessages();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        this.defaultAfkTime = config.getInt("default-afk-time", 300);
        this.afkCheckIntervalSeconds = config.getInt("afk-check-interval-seconds", 5);
        this.maxVoluntaryAfkTimeSeconds = config.getInt("max-voluntary-afk-time-seconds", 600);

        this.debugEnabled = config.getBoolean("debug", false);
        this.blockPickupWhileAFK = config.getBoolean("block-pickup-while-afk", true);
        this.autoclickDetectionEnabled = config.getBoolean("autoclick-detection", true);

        this.listCommandPermission = config.getString("list-command-permission", "antiafkplus.list");

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
        this.disabledWorlds = config.getStringList("disabled-worlds");
    }

    public void loadMessages() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(file);

        this.messageAfkEnter = color(messages.getString("messages.afk-enter", "&e{player} is now AFK."));
        this.messageAfkExit = color(messages.getString("messages.afk-exit", "&a{player} is no longer AFK."));
        this.messageKickWarning = color(messages.getString("messages.kick-warning", "&cYou will be kicked soon for being AFK!"));
        this.messageKicked = color(messages.getString("messages.kicked", "&cYou have been kicked for being AFK."));
        this.messageVoluntaryAFKLimit = color(messages.getString("messages.afk-voluntary-time-limit", "&cYou have been removed from AFK mode due to time limit."));
        this.messageStatusSelf = color(messages.getString("messages.afk-status-self", "&fYou are currently: {status}"));
        this.messageStatusAFK = color(messages.getString("messages.afk-status-afk", "&c{player} is currently AFK."));
        this.messageStatusActive = color(messages.getString("messages.afk-status-active", "&a{player} is currently active."));
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getMessage(String key) {
        return color(messages.getString("messages." + key, "&7[" + key + "]"));
    }

    // Getters

    public int getDefaultAfkTime() {
        return defaultAfkTime;
    }

    public int getAfkCheckIntervalSeconds() {
        return afkCheckIntervalSeconds;
    }

    public int getMaxVoluntaryAfkTimeSeconds() {
        return maxVoluntaryAfkTimeSeconds;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public boolean isBlockPickupWhileAFK() {
        return blockPickupWhileAFK;
    }

    public boolean isAutoclickDetectionEnabled() {
        return autoclickDetectionEnabled;
    }

    public String getListCommandPermission() {
        return listCommandPermission;
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

    public List<String> getDisabledWorlds() {
        return disabledWorlds;
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

    public String getMessageVoluntaryAFKLimit() {
        return messageVoluntaryAFKLimit;
    }

    public String getMessageStatusSelf() {
        return messageStatusSelf;
    }

    public String getMessageStatusAFK() {
        return messageStatusAFK;
    }

    public String getMessageStatusActive() {
        return messageStatusActive;
    }
}
