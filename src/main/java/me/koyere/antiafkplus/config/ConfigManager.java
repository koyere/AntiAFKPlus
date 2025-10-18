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
 * Enhanced ConfigManager v2.0 - Handles loading and providing access to plugin configuration (config.yml)
 * and customizable messages (messages.yml) with advanced v2.0 features including pattern detection,
 * behavioral analysis, and comprehensive event system configuration.
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

    // Enhanced v2.0 detection settings
    private boolean enhancedDetectionEnabled;
    private boolean patternDetectionEnabled;
    private boolean behavioralAnalysisEnabled;
    private boolean advancedMovementTrackingEnabled;

    // Pattern detection specific settings
    private double waterCircleRadius;
    private int minSamplesForPattern;
    private double confinedSpaceThreshold;
    private long patternAnalysisInterval;
    private double repetitiveMovementThreshold;
    private int maxPatternViolations;

    // Movement detection thresholds
    private double microMovementThreshold;
    private double headRotationThreshold;
    private long jumpSpamThreshold;
    private int maxJumpsPerPeriod;
    private long jumpResetPeriod;

    // Activity scoring weights
    private double movementWeight;
    private double headRotationWeight;
    private double jumpWeight;
    private double commandWeight;

    // Autoclicker specific settings
    private int autoclickClickWindowMs;
    private int autoclickClickThreshold;
    private long autoclickMinIdleTimeMs;
    private String autoclickAction;

    // Event system settings
    private boolean enableAFKStateChangeEvents;
    private boolean enableAFKWarningEvents;
    private boolean enableAFKKickEvents;
    private boolean enablePatternDetectionEvents;

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
    // Enhanced v2.0 messages
    private String messagePatternDetected;
    private String messageSuspiciousActivity;
    private String messageEnhancedDetectionWarning;

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

        // General AFK settings
        this.defaultAfkTime = config.getInt("default-afk-time", 300);
        this.afkCheckIntervalSeconds = config.getInt("afk-check-interval-seconds", 5);
        if (this.afkCheckIntervalSeconds <= 0) {
            plugin.getLogger().warning("Config 'afk-check-interval-seconds' must be > 0. Defaulting to 5 seconds.");
            this.afkCheckIntervalSeconds = 5;
        }
        this.maxVoluntaryAfkTimeSeconds = config.getInt("max-voluntary-afk-time-seconds", 600);

        // Feature toggles
        this.debugEnabled = config.getBoolean("debug", false);
        this.blockPickupWhileAFK = config.getBoolean("block-pickup-while-afk", true);
        this.autoclickDetectionEnabled = config.getBoolean("autoclick-detection", false);
        this.broadcastAfkStateChanges = config.getBoolean("broadcast-afk-state-changes", true);

        // Enhanced v2.0 detection settings
        loadEnhancedDetectionSettings();

        // Pattern detection settings
        loadPatternDetectionSettings();

        // Movement detection thresholds
        loadMovementDetectionSettings();

        // Activity scoring weights
        loadActivityScoringSettings();

        // Autoclicker specific settings
        loadAutoclickSettings();

        // Event system settings
        loadEventSystemSettings();

        // Commands and permissions
        this.listCommandPermission = config.getString("list-command-permission", "antiafkplus.list");

        // AFK warnings and times
        loadAFKTimingSettings();

        // World settings
        loadWorldSettings();
    }

    private void loadEnhancedDetectionSettings() {
        ConfigurationSection enhancedSection = config.getConfigurationSection("enhanced-detection");
        if (enhancedSection != null) {
            this.enhancedDetectionEnabled = enhancedSection.getBoolean("enabled", true);
            this.patternDetectionEnabled = enhancedSection.getBoolean("pattern-detection", true);
            this.behavioralAnalysisEnabled = enhancedSection.getBoolean("behavioral-analysis", true);
            this.advancedMovementTrackingEnabled = enhancedSection.getBoolean("advanced-movement-tracking", true);
        } else {
            // Set defaults if section doesn't exist
            this.enhancedDetectionEnabled = true;
            this.patternDetectionEnabled = true;
            this.behavioralAnalysisEnabled = true;
            this.advancedMovementTrackingEnabled = true;
        }
    }

    private void loadPatternDetectionSettings() {
        ConfigurationSection patternSection = config.getConfigurationSection("pattern-detection-settings");
        if (patternSection != null) {
            this.waterCircleRadius = patternSection.getDouble("water-circle-radius", 3.0);
            this.minSamplesForPattern = patternSection.getInt("min-samples-for-pattern", 20);
            this.confinedSpaceThreshold = patternSection.getDouble("confined-space-threshold", 5.0);
            this.patternAnalysisInterval = patternSection.getLong("pattern-analysis-interval-ms", 30000L);
            this.repetitiveMovementThreshold = patternSection.getDouble("repetitive-movement-threshold", 0.8);
            this.maxPatternViolations = patternSection.getInt("max-pattern-violations", 3);
        } else {
            // Set defaults
            this.waterCircleRadius = 3.0;
            this.minSamplesForPattern = 20;
            this.confinedSpaceThreshold = 5.0;
            this.patternAnalysisInterval = 30000L;
            this.repetitiveMovementThreshold = 0.8;
            this.maxPatternViolations = 3;
        }
    }

    private void loadMovementDetectionSettings() {
        ConfigurationSection movementSection = config.getConfigurationSection("movement-detection-settings");
        if (movementSection != null) {
            this.microMovementThreshold = movementSection.getDouble("micro-movement-threshold", 0.1);
            this.headRotationThreshold = movementSection.getDouble("head-rotation-threshold", 5.0);
            this.jumpSpamThreshold = movementSection.getLong("jump-spam-threshold-ms", 1000L);
            this.maxJumpsPerPeriod = movementSection.getInt("max-jumps-per-period", 10);
            this.jumpResetPeriod = movementSection.getLong("jump-reset-period-ms", 30000L);
        } else {
            // Set defaults
            this.microMovementThreshold = 0.1;
            this.headRotationThreshold = 5.0;
            this.jumpSpamThreshold = 1000L;
            this.maxJumpsPerPeriod = 10;
            this.jumpResetPeriod = 30000L;
        }
    }

    private void loadActivityScoringSettings() {
        ConfigurationSection scoringSection = config.getConfigurationSection("activity-scoring-weights");
        if (scoringSection != null) {
            this.movementWeight = scoringSection.getDouble("movement", 1.0);
            this.headRotationWeight = scoringSection.getDouble("head-rotation", 1.5);
            this.jumpWeight = scoringSection.getDouble("jump", 0.8);
            this.commandWeight = scoringSection.getDouble("command", 2.0);
        } else {
            // Set defaults
            this.movementWeight = 1.0;
            this.headRotationWeight = 1.5;
            this.jumpWeight = 0.8;
            this.commandWeight = 2.0;
        }
    }

    private void loadAutoclickSettings() {
        this.autoclickClickWindowMs = config.getInt("autoclick-detection-settings.click-window-ms", 5000);
        this.autoclickClickThreshold = config.getInt("autoclick-detection-settings.click-threshold", 20);
        this.autoclickMinIdleTimeMs = config.getLong("autoclick-detection-settings.min-idle-time-ms", 60000L);
        this.autoclickAction = config.getString("autoclick-detection-settings.action", "LOG").toUpperCase();
    }

    private void loadEventSystemSettings() {
        ConfigurationSection eventSection = config.getConfigurationSection("event-system");
        if (eventSection != null) {
            this.enableAFKStateChangeEvents = eventSection.getBoolean("afk-state-change-events", true);
            this.enableAFKWarningEvents = eventSection.getBoolean("afk-warning-events", true);
            this.enableAFKKickEvents = eventSection.getBoolean("afk-kick-events", true);
            this.enablePatternDetectionEvents = eventSection.getBoolean("pattern-detection-events", true);
        } else {
            // Set defaults
            this.enableAFKStateChangeEvents = true;
            this.enableAFKWarningEvents = true;
            this.enableAFKKickEvents = true;
            this.enablePatternDetectionEvents = true;
        }
    }

    private void loadAFKTimingSettings() {
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
    }

    private void loadWorldSettings() {
        List<String> enabled = config.getStringList("enabled-worlds");
        List<String> disabled = config.getStringList("disabled-worlds");
        this.enabledWorlds = enabled != null ? new ArrayList<>(enabled) : new ArrayList<>();
        this.disabledWorlds = disabled != null ? new ArrayList<>(disabled) : new ArrayList<>();
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

        // Load standard messages
        this.messagePlayerNowAFK = loadColoredString("messages.player-now-afk", "&e{player} is now AFK.");
        this.messagePlayerNoLongerAFK = loadColoredString("messages.player-no-longer-afk", "&a{player} is no longer AFK.");
        this.messageKickWarning = loadColoredString("messages.kick-warning", "&cYou will be kicked in {seconds}s for being AFK!");
        this.messageKicked = loadColoredString("messages.kicked-for-afk", "&cYou have been kicked for being AFK.");
        this.messageVoluntaryAFKLimit = loadColoredString("messages.afk-voluntary-time-limit", "&cYou have been removed from AFK mode due to time limit.");
        this.messageAlreadyAFK = loadColoredString("messages.already-afk", "&eYou are already AFK.");

        // Load autoclicker messages
        this.messageAutoclickSetAfk = loadColoredString("messages.autoclick-detected-set-afk", "&cSuspicious clicking detected. You have been set to AFK.");
        this.messageAutoclickKickReason = loadColoredString("messages.autoclick-detected-kick-reason", "&cKicked for suspicious clicking activity (autoclick).");

        // Load enhanced v2.0 messages
        this.messagePatternDetected = loadColoredString("messages.pattern-detected", "&c[AntiAFK] Suspicious movement pattern detected: {pattern}");
        this.messageSuspiciousActivity = loadColoredString("messages.suspicious-activity", "&e[AntiAFK] Suspicious activity detected. Please move normally.");
        this.messageEnhancedDetectionWarning = loadColoredString("messages.enhanced-detection-warning", "&6[AntiAFK] Enhanced detection system is monitoring your activity.");
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

    // --- Standard getters for loaded configuration values ---

    public int getDefaultAfkTime() { return defaultAfkTime; }
    public int getAfkCheckIntervalSeconds() { return afkCheckIntervalSeconds; }
    public int getMaxVoluntaryAfkTimeSeconds() { return maxVoluntaryAfkTimeSeconds; }
    public boolean isDebugEnabled() { return debugEnabled; }
    public boolean isBlockPickupWhileAFK() { return blockPickupWhileAFK; }
    public boolean isAutoclickDetectionEnabled() { return autoclickDetectionEnabled; }
    public boolean shouldBroadcastAFKStateChanges() { return broadcastAfkStateChanges; }
    public String getListCommandPermission() { return listCommandPermission; }
    public List<Integer> getAfkWarningTimes() { return Collections.unmodifiableList(afkWarningTimes); }
    public Map<String, Integer> getPermissionTimes() { return Collections.unmodifiableMap(permissionTimes); }
    public List<String> getEnabledWorlds() { return Collections.unmodifiableList(enabledWorlds); }
    public List<String> getDisabledWorlds() { return Collections.unmodifiableList(disabledWorlds); }

    public synchronized void setWorldDetectionEnabled(String worldName, boolean enabled) {
        if (worldName == null || worldName.trim().isEmpty()) {
            return;
        }

        String normalized = worldName.trim();
        if (enabled) {
            if (!enabledWorlds.contains(normalized)) {
                enabledWorlds.add(normalized);
            }
            disabledWorlds.remove(normalized);
        } else {
            if (!disabledWorlds.contains(normalized)) {
                disabledWorlds.add(normalized);
            }
            enabledWorlds.remove(normalized);
        }

        config.set("enabled-worlds", enabledWorlds);
        config.set("disabled-worlds", disabledWorlds);
        plugin.saveConfig();
        loadWorldSettings();
    }

    // --- Enhanced v2.0 detection getters ---

    public boolean isEnhancedDetectionEnabled() { return enhancedDetectionEnabled; }
    public boolean isPatternDetectionEnabled() { return patternDetectionEnabled; }
    public boolean isBehavioralAnalysisEnabled() { return behavioralAnalysisEnabled; }
    public boolean isAdvancedMovementTrackingEnabled() { return advancedMovementTrackingEnabled; }

    // --- Pattern detection getters ---

    public double getWaterCircleRadius() { return waterCircleRadius; }
    public int getMinSamplesForPattern() { return minSamplesForPattern; }
    public double getConfinedSpaceThreshold() { return confinedSpaceThreshold; }
    public long getPatternAnalysisInterval() { return patternAnalysisInterval; }
    public double getRepetitiveMovementThreshold() { return repetitiveMovementThreshold; }
    public int getMaxPatternViolations() { return maxPatternViolations; }

    // --- Movement detection getters ---

    public double getMicroMovementThreshold() { return microMovementThreshold; }
    public double getHeadRotationThreshold() { return headRotationThreshold; }
    public long getJumpSpamThreshold() { return jumpSpamThreshold; }
    public int getMaxJumpsPerPeriod() { return maxJumpsPerPeriod; }
    public long getJumpResetPeriod() { return jumpResetPeriod; }

    // --- Activity scoring getters ---

    public double getMovementWeight() { return movementWeight; }
    public double getHeadRotationWeight() { return headRotationWeight; }
    public double getJumpWeight() { return jumpWeight; }
    public double getCommandWeight() { return commandWeight; }

    // --- Autoclicker getters ---

    public int getAutoclickClickWindowMs() { return autoclickClickWindowMs; }
    public int getAutoclickClickThreshold() { return autoclickClickThreshold; }
    public long getAutoclickMinIdleTimeMs() { return autoclickMinIdleTimeMs; }
    public String getAutoclickAction() { return autoclickAction; }

    // --- Event system getters ---

    public boolean isAFKStateChangeEventsEnabled() { return enableAFKStateChangeEvents; }
    public boolean isAFKWarningEventsEnabled() { return enableAFKWarningEvents; }
    public boolean isAFKKickEventsEnabled() { return enableAFKKickEvents; }
    public boolean isPatternDetectionEventsEnabled() { return enablePatternDetectionEvents; }

    // --- Standard message getters ---

    public String getMessagePlayerNowAFK() { return messagePlayerNowAFK; }
    public String getMessagePlayerNoLongerAFK() { return messagePlayerNoLongerAFK; }
    public String getMessageKickWarning() { return messageKickWarning; }
    public String getMessageKicked() { return messageKicked; }
    public String getMessageVoluntaryAFKLimit() { return messageVoluntaryAFKLimit; }
    public String getMessageAlreadyAFK() { return messageAlreadyAFK; }

    // --- Autoclicker message getters ---

    public String getMessageAutoclickSetAfk() { return messageAutoclickSetAfk; }
    public String getMessageAutoclickKickReason() { return messageAutoclickKickReason; }

    // --- Enhanced v2.0 message getters ---

    public String getMessagePatternDetected() { return messagePatternDetected; }
    public String getMessageSuspiciousActivity() { return messageSuspiciousActivity; }
    public String getMessageEnhancedDetectionWarning() { return messageEnhancedDetectionWarning; }

    /**
     * Validates the current configuration and logs any issues.
     * @return true if configuration is valid, false if there are critical issues
     */
    public boolean validateConfiguration() {
        boolean isValid = true;

        // Validate timing settings
        if (defaultAfkTime <= 0) {
            plugin.getLogger().severe("default-afk-time must be greater than 0!");
            isValid = false;
        }

        if (afkCheckIntervalSeconds <= 0) {
            plugin.getLogger().severe("afk-check-interval-seconds must be greater than 0!");
            isValid = false;
        }

        // Validate pattern detection settings
        if (patternDetectionEnabled) {
            if (waterCircleRadius <= 0) {
                plugin.getLogger().warning("water-circle-radius should be greater than 0 for effective detection.");
            }
            if (minSamplesForPattern < 5) {
                plugin.getLogger().warning("min-samples-for-pattern should be at least 5 for reliable detection.");
            }
        }

        // Validate movement thresholds
        if (microMovementThreshold < 0) {
            plugin.getLogger().warning("micro-movement-threshold should not be negative.");
        }

        if (enhancedDetectionEnabled) {
            plugin.getLogger().info("Enhanced Detection v2.0 is enabled with the following features:");
            plugin.getLogger().info("  - Pattern Detection: " + (patternDetectionEnabled ? "ENABLED" : "DISABLED"));
            plugin.getLogger().info("  - Behavioral Analysis: " + (behavioralAnalysisEnabled ? "ENABLED" : "DISABLED"));
            plugin.getLogger().info("  - Advanced Movement Tracking: " + (advancedMovementTrackingEnabled ? "ENABLED" : "DISABLED"));
        }

        return isValid;
    }

    /**
     * Reloads all configuration values and validates them.
     */
    public void reloadConfiguration() {
        loadConfigValues();
        loadMessages();
        validateConfiguration();
        plugin.getLogger().info("Configuration reloaded successfully with v2.0 enhancements.");
    }
}
