package me.koyere.antiafkplus.config;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles loading and providing access to plugin configuration (config.yml)
 * and customizable messages (messages.yml).
 */
public class ConfigManager {

    private final AntiAFKPlus plugin;
    private FileConfiguration config; // For config.yml
    private FileConfiguration messages; // For messages.yml
    private File messagesFile; // File object for messages.yml

    // General settings
    private int defaultAfkTime;
    private int afkCheckIntervalSeconds;
    private int maxVoluntaryAfkTimeSeconds;

    // Feature toggles & behavior
    private boolean debugEnabled;
    private boolean blockPickupWhileAFK;
    private boolean autoclickDetectionEnabled; // This is the main on/off switch
    private boolean broadcastAfkStateChanges;

    // Autoclicker specific settings
    private int autoclickClickWindowMs;
    private int autoclickClickThreshold;
    private long autoclickMinIdleTimeMs;
    private String autoclickAction;


    // Permissions
    private String listCommandPermission;

    // AFK logic related
    private List<Integer> afkWarningTimes;
    private Map<String, Integer> permissionTimes;
    private List<String> enabledWorlds;
    private List<String> disabledWorlds;

    // Message strings
    private String messagePlayerNowAFK;
    private String messagePlayerNoLongerAFK;
    private String messageKickWarning;
    private String messageKicked;
    private String messageVoluntaryAFKLimit;
    private String messageAlreadyAFK;
    // Messages for autoclicker
    private String messageAutoclickSetAfk;
    private String messageAutoclickKickReason;


    /**
     * Constructor for ConfigManager.
     * Initializes and loads configurations.
     * @param plugin The main plugin instance.
     */
    public ConfigManager(AntiAFKPlus plugin) {
        this.plugin = plugin;
        loadConfigValues();
        loadMessages();
    }

    /**
     * Loads values from config.yml into memory.
     * Saves the default config.yml if it doesn't exist and reloads it.
     */
    public void loadConfigValues() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        this.defaultAfkTime = config.getInt("default-afk-time", 300);
        this.afkCheckIntervalSeconds = config.getInt("afk-check-interval-seconds", 5);
        if (this.afkCheckIntervalSeconds <= 0) {
            plugin.getLogger().warning("Config 'afk-check-interval-seconds' must be > 0. Defaulting to 5 seconds.");
            this.afkCheckIntervalSeconds = 5;
        }
        this.maxVoluntaryAfkTimeSeconds = config.getInt("max-voluntary-afk-time-seconds", 600);

        this.debugEnabled = config.getBoolean("debug", false);
        this.blockPickupWhileAFK = config.getBoolean("block-pickup-while-afk", true);
        this.autoclickDetectionEnabled = config.getBoolean("autoclick-detection", false); // Main toggle for the feature
        this.broadcastAfkStateChanges = config.getBoolean("broadcast-afk-state-changes", true);

        // Load autoclicker specific settings
        // These will only be used if autoclick-detection (the main toggle above) is true.
        this.autoclickClickWindowMs = config.getInt("autoclick-detection-settings.click-window-ms", 5000);
        this.autoclickClickThreshold = config.getInt("autoclick-detection-settings.click-threshold", 20);
        this.autoclickMinIdleTimeMs = config.getLong("autoclick-detection-settings.min-idle-time-ms", 60000L);
        this.autoclickAction = config.getString("autoclick-detection-settings.action", "LOG").toUpperCase();


        this.listCommandPermission = config.getString("list-command-permission", "antiafkplus.list");

        this.afkWarningTimes = config.getIntegerList("afk-warnings");
        if (this.afkWarningTimes == null) {
            this.afkWarningTimes = new ArrayList<>();
        }

        this.permissionTimes = new HashMap<>();
        ConfigurationSection permTimesSection = config.getConfigurationSection("permission-times");
        if (permTimesSection != null) {
            for (String key : permTimesSection.getKeys(false)) {
                if (key != null && !key.trim().isEmpty()) {
                    int time = permTimesSection.getInt(key, this.defaultAfkTime);
                    this.permissionTimes.put(key.trim(), time);
                }
            }
        }

        this.enabledWorlds = config.getStringList("enabled-worlds");
        if (this.enabledWorlds == null) {
            this.enabledWorlds = Collections.emptyList();
        }
        this.disabledWorlds = config.getStringList("disabled-worlds");
        if (this.disabledWorlds == null) {
            this.disabledWorlds = Collections.emptyList();
        }
    }

    /**
     * Loads messages from messages.yml into memory.
     * Saves the default messages.yml if it doesn't exist.
     */
    public void loadMessages() {
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);

        this.messagePlayerNowAFK = loadColoredString("messages.player-now-afk", "&e{player} is now AFK.");
        this.messagePlayerNoLongerAFK = loadColoredString("messages.player-no-longer-afk", "&a{player} is no longer AFK.");
        this.messageKickWarning = loadColoredString("messages.kick-warning", "&cYou will be kicked in {seconds}s for being AFK!");
        this.messageKicked = loadColoredString("messages.kicked-for-afk", "&cYou have been kicked for being AFK.");
        this.messageVoluntaryAFKLimit = loadColoredString("messages.afk-voluntary-time-limit", "&cYou have been removed from AFK mode due to time limit.");
        this.messageAlreadyAFK = loadColoredString("messages.already-afk", "&eYou are already AFK.");

        // Load autoclicker messages
        this.messageAutoclickSetAfk = loadColoredString("messages.autoclick-detected-set-afk", "&cSuspicious clicking detected. You have been set to AFK.");
        this.messageAutoclickKickReason = loadColoredString("messages.autoclick-detected-kick-reason", "&cKicked for suspicious clicking activity (autoclick).");
    }

    private String loadColoredString(String path, String defaultValue) {
        return ChatColor.translateAlternateColorCodes('&', messages.getString(path, defaultValue));
    }

    public String getMessage(String key, String defaultValue) {
        return ChatColor.translateAlternateColorCodes('&', messages.getString("messages." + key, defaultValue));
    }

    public String getMessageByFullPath(String fullPath, String defaultValue) {
        return ChatColor.translateAlternateColorCodes('&', messages.getString(fullPath, defaultValue));
    }


    // --- Getters for loaded configuration values ---

    public int getDefaultAfkTime() { return defaultAfkTime; }
    public int getAfkCheckIntervalSeconds() { return afkCheckIntervalSeconds; }
    public int getMaxVoluntaryAfkTimeSeconds() { return maxVoluntaryAfkTimeSeconds; }
    public boolean isDebugEnabled() { return debugEnabled; }
    public boolean isBlockPickupWhileAFK() { return blockPickupWhileAFK; }
    public boolean isAutoclickDetectionEnabled() { return autoclickDetectionEnabled; } // Main toggle
    public boolean shouldBroadcastAFKStateChanges() { return broadcastAfkStateChanges; }
    public String getListCommandPermission() { return listCommandPermission; }
    public List<Integer> getAfkWarningTimes() { return Collections.unmodifiableList(afkWarningTimes); }
    public Map<String, Integer> getPermissionTimes() { return Collections.unmodifiableMap(permissionTimes); }
    public List<String> getEnabledWorlds() { return Collections.unmodifiableList(enabledWorlds); }
    public List<String> getDisabledWorlds() { return Collections.unmodifiableList(disabledWorlds); }

    // --- Getters for Autoclicker specific settings ---
    public int getAutoclickClickWindowMs() { return autoclickClickWindowMs; }
    public int getAutoclickClickThreshold() { return autoclickClickThreshold; }
    public long getAutoclickMinIdleTimeMs() { return autoclickMinIdleTimeMs; }
    public String getAutoclickAction() { return autoclickAction; }


    // --- Getters for pre-loaded messages ---
    public String getMessagePlayerNowAFK() { return messagePlayerNowAFK; }
    public String getMessagePlayerNoLongerAFK() { return messagePlayerNoLongerAFK; }
    public String getMessageKickWarning() { return messageKickWarning; }
    public String getMessageKicked() { return messageKicked; }
    public String getMessageVoluntaryAFKLimit() { return messageVoluntaryAFKLimit; }
    public String getMessageAlreadyAFK() { return messageAlreadyAFK; }
    // Getters for autoclicker messages
    public String getMessageAutoclickSetAfk() { return messageAutoclickSetAfk; }
    public String getMessageAutoclickKickReason() { return messageAutoclickKickReason; }
}