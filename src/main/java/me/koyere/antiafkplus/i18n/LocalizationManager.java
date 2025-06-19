package me.koyere.antiafkplus.i18n;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Complete internationalization system for AntiAFKPlus.
 * Supports multiple languages, per-player language settings,
 * placeholder support, and dynamic language switching.
 */
public class LocalizationManager {
    
    private final AntiAFKPlus plugin;
    private final Logger logger;
    
    // Language data storage
    private final Map<String, LanguageData> languages = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerLanguages = new ConcurrentHashMap<>();
    
    // Configuration
    private String defaultLanguage = "en";
    private boolean autoDetectLanguage = true;
    private boolean fallbackToDefault = true;
    private boolean cacheMessages = true;
    private boolean developmentMode = false;
    
    // Caching
    private final Map<String, String> messageCache = new ConcurrentHashMap<>();
    private final Pattern placeholderPattern = Pattern.compile("\\{([^}]+)\\}");
    
    // Supported languages (built-in)
    private final Set<String> builtInLanguages = Set.of(
        "en", "es", "fr", "de", "it", "pt", "ru", "zh", "ja", "ko", 
        "nl", "pl", "sv", "no", "da", "fi", "tr", "ar", "he", "hi"
    );
    
    public LocalizationManager(AntiAFKPlus plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        
        loadConfiguration();
        setupLanguageDirectory();
        loadLanguages();
        
        logger.info("🌍 Localization system initialized with " + languages.size() + " languages");
        logger.info("   Default language: " + defaultLanguage);
        logger.info("   Available languages: " + String.join(", ", languages.keySet()));
    }
    
    /**
     * Load localization configuration from config.yml
     */
    private void loadConfiguration() {
        ConfigurationSection i18nConfig = plugin.getConfig().getConfigurationSection("internationalization");
        
        if (i18nConfig != null) {
            this.defaultLanguage = i18nConfig.getString("default-language", "en");
            this.autoDetectLanguage = i18nConfig.getBoolean("auto-detect-language", true);
            this.fallbackToDefault = i18nConfig.getBoolean("fallback-to-default", true);
            this.cacheMessages = i18nConfig.getBoolean("cache-messages", true);
            this.developmentMode = i18nConfig.getBoolean("development-mode", false);
        } else {
            // Create default configuration
            createDefaultI18nConfiguration();
        }
    }
    
    /**
     * Create default internationalization configuration.
     */
    private void createDefaultI18nConfiguration() {
        ConfigurationSection i18nConfig = plugin.getConfig().createSection("internationalization");
        
        i18nConfig.set("default-language", "en");
        i18nConfig.set("auto-detect-language", true);
        i18nConfig.set("fallback-to-default", true);
        i18nConfig.set("cache-messages", true);
        i18nConfig.set("development-mode", false);
        
        // Language-specific settings
        ConfigurationSection langSettings = i18nConfig.createSection("language-settings");
        langSettings.set("en.display-name", "English");
        langSettings.set("es.display-name", "Español");
        langSettings.set("fr.display-name", "Français");
        langSettings.set("de.display-name", "Deutsch");
        langSettings.set("it.display-name", "Italiano");
        langSettings.set("pt.display-name", "Português");
        langSettings.set("ru.display-name", "Русский");
        langSettings.set("zh.display-name", "中文");
        langSettings.set("ja.display-name", "日本語");
        langSettings.set("ko.display-name", "한국어");
        
        plugin.saveConfig();
    }
    
    /**
     * Setup the language directory and extract built-in language files.
     */
    private void setupLanguageDirectory() {
        File langDir = new File(plugin.getDataFolder(), "languages");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        
        // Extract built-in language files
        for (String langCode : builtInLanguages) {
            File langFile = new File(langDir, langCode + ".yml");
            if (!langFile.exists() || developmentMode) {
                extractLanguageFile(langCode, langFile);
            }
        }
    }
    
    /**
     * Extract a language file from the plugin resources.
     */
    private void extractLanguageFile(String langCode, File destination) {
        String resourcePath = "languages/" + langCode + ".yml";
        
        try (InputStream inputStream = plugin.getResource(resourcePath)) {
            if (inputStream != null) {
                Files.copy(inputStream, destination.toPath(), 
                          java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                logger.info("📄 Extracted language file: " + langCode + ".yml");
            } else {
                // Create minimal language file if resource doesn't exist
                createMinimalLanguageFile(langCode, destination);
            }
        } catch (IOException e) {
            logger.warning("Failed to extract language file for " + langCode + ": " + e.getMessage());
            createMinimalLanguageFile(langCode, destination);
        }
    }
    
    /**
     * Create a minimal language file with essential messages.
     */
    private void createMinimalLanguageFile(String langCode, File destination) {
        YamlConfiguration config = new YamlConfiguration();
        
        // Language metadata
        config.set("language.code", langCode);
        config.set("language.name", getLanguageDisplayName(langCode));
        config.set("language.author", "AntiAFKPlus Team");
        config.set("language.version", "2.0.0");
        
        // Essential messages (will be populated from built-in templates)
        populateEssentialMessages(config, langCode);
        
        try {
            config.save(destination);
            logger.info("📄 Created minimal language file: " + langCode + ".yml");
        } catch (IOException e) {
            logger.warning("Failed to create language file for " + langCode + ": " + e.getMessage());
        }
    }
    
    /**
     * Populate essential messages for a language.
     */
    private void populateEssentialMessages(YamlConfiguration config, String langCode) {
        // Get English templates and translate basic messages
        Map<String, String> templates = getMessageTemplates();
        
        ConfigurationSection messages = config.createSection("messages");
        
        for (Map.Entry<String, String> entry : templates.entrySet()) {
            String key = entry.getKey();
            String englishMessage = entry.getValue();
            
            // For now, just use English messages
            // In a real implementation, you'd have translation dictionaries
            messages.set(key, englishMessage);
        }
        
        // Add language-specific formatting
        config.set("formatting.color-prefix", "&e[AntiAFK]&r ");
        config.set("formatting.error-prefix", "&c[AntiAFK Error]&r ");
        config.set("formatting.success-prefix", "&a[AntiAFK]&r ");
        config.set("formatting.info-prefix", "&b[AntiAFK Info]&r ");
        
        // Date/time formatting
        config.set("formatting.date-format", getDateFormat(langCode));
        config.set("formatting.time-format", getTimeFormat(langCode));
        config.set("formatting.number-format", getNumberFormat(langCode));
    }
    
    /**
     * Get message templates (English base messages).
     */
    private Map<String, String> getMessageTemplates() {
        Map<String, String> templates = new HashMap<>();
        
        // Basic AFK messages
        templates.put("afk.player-now-afk", "&e{player} is now AFK.");
        templates.put("afk.player-no-longer-afk", "&a{player} is no longer AFK.");
        templates.put("afk.you-are-now-afk", "&eYou are now AFK.");
        templates.put("afk.you-are-no-longer-afk", "&aYou are no longer AFK.");
        templates.put("afk.already-afk", "&eYou are already AFK.");
        templates.put("afk.not-afk", "&eYou are not AFK.");
        
        // Warning messages
        templates.put("warnings.kick-warning", "&cYou will be kicked in {seconds} seconds for being AFK!");
        templates.put("warnings.final-warning", "&4FINAL WARNING: &cYou will be kicked in {seconds} seconds!");
        templates.put("warnings.pattern-detected", "&cSuspicious movement pattern detected: {pattern}");
        templates.put("warnings.autoclick-detected", "&cAutoclick detected! Please move naturally.");
        
        // Kick messages
        templates.put("kick.afk-timeout", "&cKicked for being AFK too long.");
        templates.put("kick.pattern-detection", "&cKicked for using AFK machines/bots.");
        templates.put("kick.autoclick", "&cKicked for using autoclick/macros.");
        
        // Command messages
        templates.put("commands.no-permission", "&cYou don't have permission to use this command.");
        templates.put("commands.player-not-found", "&cPlayer '{player}' not found.");
        templates.put("commands.reload-success", "&aConfiguration reloaded successfully!");
        templates.put("commands.module-enabled", "&aModule '{module}' has been enabled.");
        templates.put("commands.module-disabled", "&cModule '{module}' has been disabled.");
        templates.put("commands.invalid-syntax", "&cInvalid command syntax. Use: {usage}");
        
        // List command
        templates.put("list.header", "&e--- AFK Players ({count}) ---");
        templates.put("list.entry", "&7{player} &8- &e{time}");
        templates.put("list.footer", "&7Use /afk to toggle your AFK status");
        templates.put("list.no-afk-players", "&7No players are currently AFK.");
        
        // Status messages
        templates.put("status.afk-time", "&7AFK Time: &e{time}");
        templates.put("status.last-activity", "&7Last Activity: &e{activity}");
        templates.put("status.detection-method", "&7Detection: &e{method}");
        templates.put("status.activity-score", "&7Activity Score: &e{score}");
        
        // Module messages
        templates.put("modules.loading", "&7Loading module: {module}");
        templates.put("modules.loaded", "&aModule loaded: {module} v{version}");
        templates.put("modules.failed", "&cFailed to load module: {module}");
        templates.put("modules.disabled", "&7Module disabled: {module}");
        
        // Error messages
        templates.put("errors.config-error", "&cConfiguration error: {error}");
        templates.put("errors.database-error", "&cDatabase error: {error}");
        templates.put("errors.permission-error", "&cPermission system error: {error}");
        templates.put("errors.internal-error", "&cInternal error occurred. Please contact an administrator.");
        
        return templates;
    }
    
    /**
     * Load all available language files.
     */
    private void loadLanguages() {
        File langDir = new File(plugin.getDataFolder(), "languages");
        if (!langDir.exists()) {
            return;
        }
        
        File[] langFiles = langDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (langFiles == null) {
            return;
        }
        
        for (File langFile : langFiles) {
            String langCode = langFile.getName().replace(".yml", "");
            loadLanguageFile(langCode, langFile);
        }
        
        // Ensure default language is available
        if (!languages.containsKey(defaultLanguage)) {
            logger.warning("Default language '" + defaultLanguage + "' not found! Falling back to English.");
            defaultLanguage = "en";
            
            if (!languages.containsKey("en")) {
                logger.severe("English language file not found! Creating emergency fallback.");
                createEmergencyLanguage();
            }
        }
    }
    
    /**
     * Load a specific language file.
     */
    private void loadLanguageFile(String langCode, File langFile) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(langFile);
            
            LanguageData langData = new LanguageData(langCode);
            langData.setDisplayName(config.getString("language.name", getLanguageDisplayName(langCode)));
            langData.setAuthor(config.getString("language.author", "Unknown"));
            langData.setVersion(config.getString("language.version", "1.0.0"));
            
            // Load messages
            ConfigurationSection messages = config.getConfigurationSection("messages");
            if (messages != null) {
                loadMessagesRecursively(langData, messages, "");
            }
            
            // Load formatting settings
            ConfigurationSection formatting = config.getConfigurationSection("formatting");
            if (formatting != null) {
                langData.setDateFormat(formatting.getString("date-format", "MM/dd/yyyy"));
                langData.setTimeFormat(formatting.getString("time-format", "HH:mm:ss"));
                langData.setNumberFormat(formatting.getString("number-format", "#,##0.##"));
                langData.setColorPrefix(formatting.getString("color-prefix", "&e[AntiAFK]&r "));
                langData.setErrorPrefix(formatting.getString("error-prefix", "&c[Error]&r "));
                langData.setSuccessPrefix(formatting.getString("success-prefix", "&a[Success]&r "));
                langData.setInfoPrefix(formatting.getString("info-prefix", "&b[Info]&r "));
            }
            
            languages.put(langCode, langData);
            logger.info("📄 Loaded language: " + langCode + " (" + langData.getDisplayName() + ")");
            
        } catch (Exception e) {
            logger.warning("Failed to load language file " + langCode + ": " + e.getMessage());
        }
    }
    
    /**
     * Recursively load messages from configuration sections.
     */
    private void loadMessagesRecursively(LanguageData langData, ConfigurationSection section, String prefix) {
        for (String key : section.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            
            if (section.isConfigurationSection(key)) {
                loadMessagesRecursively(langData, section.getConfigurationSection(key), fullKey);
            } else {
                String message = section.getString(key);
                if (message != null) {
                    langData.addMessage(fullKey, message);
                }
            }
        }
    }
    
    /**
     * Create an emergency English language for fallback.
     */
    private void createEmergencyLanguage() {
        LanguageData emergency = new LanguageData("en");
        emergency.setDisplayName("English (Emergency)");
        emergency.setAuthor("AntiAFKPlus");
        emergency.setVersion("2.0.0");
        
        // Add essential messages
        emergency.addMessage("afk.player-now-afk", "{player} is now AFK.");
        emergency.addMessage("afk.player-no-longer-afk", "{player} is no longer AFK.");
        emergency.addMessage("kick.afk-timeout", "Kicked for being AFK too long.");
        emergency.addMessage("warnings.kick-warning", "You will be kicked in {seconds} seconds for being AFK!");
        emergency.addMessage("commands.no-permission", "You don't have permission to use this command.");
        emergency.addMessage("errors.internal-error", "An internal error occurred.");
        
        languages.put("en", emergency);
        logger.info("✅ Emergency English language created");
    }
    
    // ============= PUBLIC API =============
    
    /**
     * Get a localized message for a player.
     */
    public String getMessage(Player player, String key, Object... placeholders) {
        String langCode = getPlayerLanguage(player);
        return getMessage(langCode, key, placeholders);
    }
    
    /**
     * Get a localized message for a specific language.
     */
    public String getMessage(String langCode, String key, Object... placeholders) {
        String cacheKey = cacheMessages ? (langCode + ":" + key) : null;
        
        // Check cache first
        if (cacheKey != null && messageCache.containsKey(cacheKey)) {
            String cached = messageCache.get(cacheKey);
            return replacePlaceholders(cached, placeholders);
        }
        
        String message = getRawMessage(langCode, key);
        
        // Cache the raw message
        if (cacheKey != null && message != null) {
            messageCache.put(cacheKey, message);
        }
        
        return replacePlaceholders(message, placeholders);
    }
    
    /**
     * Get raw message without placeholder replacement.
     */
    private String getRawMessage(String langCode, String key) {
        LanguageData langData = languages.get(langCode);
        
        // Try to get message from requested language
        if (langData != null && langData.hasMessage(key)) {
            String message = langData.getMessage(key);
            return ChatColor.translateAlternateColorCodes('&', message);
        }
        
        // Fallback to default language
        if (fallbackToDefault && !langCode.equals(defaultLanguage)) {
            LanguageData defaultLangData = languages.get(defaultLanguage);
            if (defaultLangData != null && defaultLangData.hasMessage(key)) {
                String message = defaultLangData.getMessage(key);
                return ChatColor.translateAlternateColorCodes('&', message);
            }
        }
        
        // Return key as fallback
        return "[" + key + "]";
    }
    
    /**
     * Replace placeholders in a message.
     */
    private String replacePlaceholders(String message, Object... placeholders) {
        if (message == null || placeholders.length == 0) {
            return message;
        }
        
        // Convert placeholders to map
        Map<String, String> placeholderMap = new HashMap<>();
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String key = placeholders[i].toString();
            String value = placeholders[i + 1].toString();
            placeholderMap.put(key, value);
        }
        
        // Replace placeholders
        Matcher matcher = placeholderPattern.matcher(message);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = placeholderMap.getOrDefault(placeholder, "{" + placeholder + "}");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Send a localized message to a player.
     */
    public void sendMessage(Player player, String key, Object... placeholders) {
        String message = getMessage(player, key, placeholders);
        if (message != null && !message.trim().isEmpty()) {
            player.sendMessage(message);
        }
    }
    
    /**
     * Get a player's language code.
     */
    public String getPlayerLanguage(Player player) {
        String langCode = playerLanguages.get(player.getUniqueId());
        
        if (langCode == null && autoDetectLanguage) {
            langCode = detectPlayerLanguage(player);
            setPlayerLanguage(player, langCode);
        }
        
        return langCode != null ? langCode : defaultLanguage;
    }
    
    /**
     * Set a player's language.
     */
    public void setPlayerLanguage(Player player, String langCode) {
        if (languages.containsKey(langCode)) {
            playerLanguages.put(player.getUniqueId(), langCode);
            
            // Clear cache for this player
            if (cacheMessages) {
                messageCache.entrySet().removeIf(entry -> entry.getKey().startsWith(langCode + ":"));
            }
        } else {
            logger.warning("Attempted to set invalid language '" + langCode + "' for player " + player.getName());
        }
    }
    
    /**
     * Detect a player's language based on their client locale.
     */
    private String detectPlayerLanguage(Player player) {
        try {
            // Try to get client locale (requires specific server implementations)
            String locale = player.getLocale();
            if (locale != null && locale.contains("_")) {
                String langCode = locale.split("_")[0].toLowerCase();
                if (languages.containsKey(langCode)) {
                    return langCode;
                }
            }
        } catch (Exception e) {
            // Client locale detection failed, use default
        }
        
        return defaultLanguage;
    }
    
    /**
     * Get all available languages.
     */
    public Set<String> getAvailableLanguages() {
        return new HashSet<>(languages.keySet());
    }
    
    /**
     * Get language display name.
     */
    public String getLanguageDisplayName(String langCode) {
        LanguageData langData = languages.get(langCode);
        if (langData != null) {
            return langData.getDisplayName();
        }
        
        // Fallback to standard display names
        switch (langCode.toLowerCase()) {
            case "en": return "English";
            case "es": return "Español";
            case "fr": return "Français";
            case "de": return "Deutsch";
            case "it": return "Italiano";
            case "pt": return "Português";
            case "ru": return "Русский";
            case "zh": return "中文";
            case "ja": return "日本語";
            case "ko": return "한국어";
            case "nl": return "Nederlands";
            case "pl": return "Polski";
            case "sv": return "Svenska";
            case "no": return "Norsk";
            case "da": return "Dansk";
            case "fi": return "Suomi";
            case "tr": return "Türkçe";
            case "ar": return "العربية";
            case "he": return "עברית";
            case "hi": return "हिन्दी";
            default: return langCode.toUpperCase();
        }
    }
    
    /**
     * Get date format for a language.
     */
    private String getDateFormat(String langCode) {
        switch (langCode.toLowerCase()) {
            case "en": return "MM/dd/yyyy";
            case "de":
            case "fr":
            case "it":
            case "es":
            case "pt": return "dd/MM/yyyy";
            case "zh":
            case "ja":
            case "ko": return "yyyy/MM/dd";
            default: return "dd/MM/yyyy";
        }
    }
    
    /**
     * Get time format for a language.
     */
    private String getTimeFormat(String langCode) {
        switch (langCode.toLowerCase()) {
            case "en": return "h:mm:ss a";
            default: return "HH:mm:ss";
        }
    }
    
    /**
     * Get number format for a language.
     */
    private String getNumberFormat(String langCode) {
        switch (langCode.toLowerCase()) {
            case "en": return "#,##0.##";
            case "de":
            case "fr":
            case "it": return "#.##0,##";
            default: return "#,##0.##";
        }
    }
    
    /**
     * Reload all language files.
     */
    public void reload() {
        languages.clear();
        playerLanguages.clear();
        messageCache.clear();
        
        loadConfiguration();
        loadLanguages();
        
        logger.info("🔄 Localization system reloaded with " + languages.size() + " languages");
    }
    
    /**
     * Clear a player's language data (on disconnect).
     */
    public void clearPlayerData(Player player) {
        playerLanguages.remove(player.getUniqueId());
    }
    
    /**
     * Get localization statistics.
     */
    public LocalizationStats getStats() {
        return new LocalizationStats(
            languages.size(),
            playerLanguages.size(),
            messageCache.size(),
            defaultLanguage,
            autoDetectLanguage
        );
    }
    
    // ============= INNER CLASSES =============
    
    /**
     * Language data holder.
     */
    private static class LanguageData {
        private final String code;
        private String displayName;
        private String author;
        private String version;
        private String dateFormat;
        private String timeFormat;
        private String numberFormat;
        private String colorPrefix;
        private String errorPrefix;
        private String successPrefix;
        private String infoPrefix;
        
        private final Map<String, String> messages = new ConcurrentHashMap<>();
        
        public LanguageData(String code) {
            this.code = code;
        }
        
        // Getters and setters
        public String getCode() { return code; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getDateFormat() { return dateFormat; }
        public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }
        public String getTimeFormat() { return timeFormat; }
        public void setTimeFormat(String timeFormat) { this.timeFormat = timeFormat; }
        public String getNumberFormat() { return numberFormat; }
        public void setNumberFormat(String numberFormat) { this.numberFormat = numberFormat; }
        public String getColorPrefix() { return colorPrefix; }
        public void setColorPrefix(String colorPrefix) { this.colorPrefix = colorPrefix; }
        public String getErrorPrefix() { return errorPrefix; }
        public void setErrorPrefix(String errorPrefix) { this.errorPrefix = errorPrefix; }
        public String getSuccessPrefix() { return successPrefix; }
        public void setSuccessPrefix(String successPrefix) { this.successPrefix = successPrefix; }
        public String getInfoPrefix() { return infoPrefix; }
        public void setInfoPrefix(String infoPrefix) { this.infoPrefix = infoPrefix; }
        
        public void addMessage(String key, String message) {
            messages.put(key, message);
        }
        
        public String getMessage(String key) {
            return messages.get(key);
        }
        
        public boolean hasMessage(String key) {
            return messages.containsKey(key);
        }
        
        public int getMessageCount() {
            return messages.size();
        }
    }
    
    /**
     * Localization statistics.
     */
    public static class LocalizationStats {
        private final int languageCount;
        private final int playerLanguageCount;
        private final int cacheSize;
        private final String defaultLanguage;
        private final boolean autoDetectEnabled;
        
        public LocalizationStats(int languageCount, int playerLanguageCount, int cacheSize,
                               String defaultLanguage, boolean autoDetectEnabled) {
            this.languageCount = languageCount;
            this.playerLanguageCount = playerLanguageCount;
            this.cacheSize = cacheSize;
            this.defaultLanguage = defaultLanguage;
            this.autoDetectEnabled = autoDetectEnabled;
        }
        
        public int getLanguageCount() { return languageCount; }
        public int getPlayerLanguageCount() { return playerLanguageCount; }
        public int getCacheSize() { return cacheSize; }
        public String getDefaultLanguage() { return defaultLanguage; }
        public boolean isAutoDetectEnabled() { return autoDetectEnabled; }
    }
}