package me.koyere.antiafkplus.compatibility;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Bedrock Edition compatibility layer for AntiAFKPlus.
 * Automatically detects Bedrock players and adapts UI/UX accordingly.
 * Supports Floodgate, GeyserMC, and other Bedrock bridge solutions.
 */
public class BedrockCompatibility implements Listener {
    
    private final AntiAFKPlus plugin;
    private final Logger logger;
    
    // Bedrock detection
    private final Set<UUID> bedrockPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BedrockPlayerInfo> bedrockPlayerInfo = new ConcurrentHashMap<>();
    
    // Integration support
    private boolean floodgateAvailable = false;
    private boolean geyserAvailable = false;
    private boolean customDetectionEnabled = true;
    
    // Floodgate integration (loaded via reflection)
    private Class<?> floodgateApiClass;
    private Method isFloodgatePlayerMethod;
    private Method getFloodgatePlayerMethod;
    
    // UI adaptations for Bedrock
    private boolean adaptMenus = true;
    private boolean adaptMessages = true;
    private boolean adaptHotkeys = true;
    private boolean adaptItemInteractions = true;
    
    public BedrockCompatibility(AntiAFKPlus plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        
        loadConfiguration();
        detectIntegrations();
        initializeFloodgateIntegration();
        
        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Bedrock compatibility initialized silently
    }
    
    /**
     * Load Bedrock compatibility configuration.
     */
    private void loadConfiguration() {
        var config = plugin.getConfig();
        
        this.adaptMenus = config.getBoolean("bedrock-compatibility.adapt-menus", true);
        this.adaptMessages = config.getBoolean("bedrock-compatibility.adapt-messages", true);
        this.adaptHotkeys = config.getBoolean("bedrock-compatibility.adapt-hotkeys", true);
        this.adaptItemInteractions = config.getBoolean("bedrock-compatibility.adapt-item-interactions", true);
        this.customDetectionEnabled = config.getBoolean("bedrock-compatibility.custom-detection", true);
        
        // Create default configuration if needed
        if (!config.contains("bedrock-compatibility")) {
            createDefaultConfiguration();
        }
    }
    
    /**
     * Create default Bedrock compatibility configuration.
     */
    private void createDefaultConfiguration() {
        var config = plugin.getConfig();
        
        config.set("bedrock-compatibility.enabled", true);
        config.set("bedrock-compatibility.adapt-menus", true);
        config.set("bedrock-compatibility.adapt-messages", true);
        config.set("bedrock-compatibility.adapt-hotkeys", true);
        config.set("bedrock-compatibility.adapt-item-interactions", true);
        config.set("bedrock-compatibility.custom-detection", true);
        
        // Detection methods
        config.set("bedrock-compatibility.detection-methods.floodgate", true);
        config.set("bedrock-compatibility.detection-methods.geyser", true);
        config.set("bedrock-compatibility.detection-methods.username-prefix", true);
        config.set("bedrock-compatibility.detection-methods.client-brand", true);
        
        // UI adaptations
        config.set("bedrock-compatibility.ui-adaptations.max-menu-slots", 27);
        config.set("bedrock-compatibility.ui-adaptations.use-simple-layouts", true);
        config.set("bedrock-compatibility.ui-adaptations.avoid-shift-click", true);
        config.set("bedrock-compatibility.ui-adaptations.use-touch-friendly-buttons", true);
        
        // Message adaptations
        config.set("bedrock-compatibility.message-adaptations.shorter-messages", true);
        config.set("bedrock-compatibility.message-adaptations.avoid-complex-formatting", true);
        config.set("bedrock-compatibility.message-adaptations.use-actionbar-sparingly", true);
        
        plugin.saveConfig();
    }
    
    /**
     * Detect available Bedrock integration plugins.
     */
    private void detectIntegrations() {
        // Check for Floodgate
        if (Bukkit.getPluginManager().getPlugin("floodgate") != null) {
            floodgateAvailable = true;
        }
        
        // Check for Geyser
        if (Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null ||
            Bukkit.getPluginManager().getPlugin("Geyser-Bukkit") != null) {
            geyserAvailable = true;
        }
    }
    
    /**
     * Initialize Floodgate integration using reflection.
     */
    private void initializeFloodgateIntegration() {
        if (!floodgateAvailable) {
            return;
        }
        
        try {
            // Load Floodgate API classes
            floodgateApiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            
            // Get methods
            Method getInstanceMethod = floodgateApiClass.getMethod("getInstance");
            Object floodgateInstance = getInstanceMethod.invoke(null);
            
            isFloodgatePlayerMethod = floodgateInstance.getClass().getMethod("isFloodgatePlayer", UUID.class);
            getFloodgatePlayerMethod = floodgateInstance.getClass().getMethod("getPlayer", UUID.class);
            
            // Floodgate API integration initialized
            
        } catch (Exception e) {
            logger.warning("❌ Failed to initialize Floodgate integration: " + e.getMessage());
            floodgateAvailable = false;
        }
    }
    
    /**
     * Detect if a player is using Bedrock Edition.
     */
    public boolean isBedrockPlayer(Player player) {
        return bedrockPlayers.contains(player.getUniqueId());
    }
    
    /**
     * Detect if a player is using Bedrock Edition with detailed analysis.
     */
    public BedrockDetectionResult detectBedrockPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        
        BedrockDetectionResult result = new BedrockDetectionResult();
        result.setPlayer(player);
        
        // Method 1: Floodgate detection (most reliable)
        if (floodgateAvailable && checkFloodgatePlayer(uuid)) {
            result.setBedrockPlayer(true);
            result.setDetectionMethod("Floodgate");
            result.setConfidence(100);
            
            // Get additional Floodgate info
            try {
                Object floodgatePlayer = getFloodgatePlayerMethod.invoke(
                    floodgateApiClass.getMethod("getInstance").invoke(null), uuid);
                
                if (floodgatePlayer != null) {
                    // Extract device info, input mode, etc.
                    extractFloodgateInfo(floodgatePlayer, result);
                }
            } catch (Exception e) {
                logger.warning("Failed to get Floodgate player info: " + e.getMessage());
            }
            
            return result;
        }
        
        // Method 2: Username prefix detection
        if (customDetectionEnabled && checkUsernamePrefix(name)) {
            result.setBedrockPlayer(true);
            result.setDetectionMethod("Username Prefix");
            result.setConfidence(85);
            return result;
        }
        
        // Method 3: Client brand detection (when available)
        String clientBrand = getClientBrand(player);
        if (clientBrand != null && checkClientBrand(clientBrand)) {
            result.setBedrockPlayer(true);
            result.setDetectionMethod("Client Brand");
            result.setConfidence(90);
            result.setClientBrand(clientBrand);
            return result;
        }
        
        // Method 4: Behavioral detection (less reliable)
        if (customDetectionEnabled) {
            int behaviorScore = analyzeBehavioralPatterns(player);
            if (behaviorScore >= 70) {
                result.setBedrockPlayer(true);
                result.setDetectionMethod("Behavioral Analysis");
                result.setConfidence(behaviorScore);
                return result;
            }
        }
        
        // Not detected as Bedrock
        result.setBedrockPlayer(false);
        result.setDetectionMethod("Java Edition (default)");
        result.setConfidence(60);
        
        return result;
    }
    
    /**
     * Check if player is a Floodgate player.
     */
    private boolean checkFloodgatePlayer(UUID uuid) {
        if (!floodgateAvailable || isFloodgatePlayerMethod == null) {
            return false;
        }
        
        try {
            Object floodgateInstance = floodgateApiClass.getMethod("getInstance").invoke(null);
            return (Boolean) isFloodgatePlayerMethod.invoke(floodgateInstance, uuid);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Extract additional information from Floodgate player object.
     */
    private void extractFloodgateInfo(Object floodgatePlayer, BedrockDetectionResult result) {
        try {
            // Get device type
            Object deviceOs = floodgatePlayer.getClass().getMethod("getDeviceOs").invoke(floodgatePlayer);
            if (deviceOs != null) {
                result.setDeviceType(deviceOs.toString());
            }
            
            // Get input mode
            Object inputMode = floodgatePlayer.getClass().getMethod("getInputMode").invoke(floodgatePlayer);
            if (inputMode != null) {
                result.setInputMode(inputMode.toString());
            }
            
            // Get UI profile
            Object uiProfile = floodgatePlayer.getClass().getMethod("getUiProfile").invoke(floodgatePlayer);
            if (uiProfile != null) {
                result.setUiProfile(uiProfile.toString());
            }
            
            // Get Bedrock version
            String version = (String) floodgatePlayer.getClass().getMethod("getVersion").invoke(floodgatePlayer);
            if (version != null) {
                result.setBedrockVersion(version);
            }
            
        } catch (Exception e) {
            logger.warning("Failed to extract Floodgate player info: " + e.getMessage());
        }
    }
    
    /**
     * Check username for Bedrock prefixes.
     */
    private boolean checkUsernamePrefix(String username) {
        if (username == null || username.length() < 2) {
            return false;
        }
        
        // Common Bedrock prefixes
        String[] bedrockPrefixes = {".", "_", "-", "~", "Bedrock", "PE", "MCPE", "Mobile"};
        
        String lowerName = username.toLowerCase();
        for (String prefix : bedrockPrefixes) {
            if (lowerName.startsWith(prefix.toLowerCase())) {
                return true;
            }
        }
        
        // Check for common Bedrock patterns
        if (lowerName.matches(".*[0-9]{4,}.*")) { // Many numbers (often from Xbox gamertags)
            return true;
        }
        
        return false;
    }
    
    /**
     * Get client brand from player (when available).
     */
    private String getClientBrand(Player player) {
        try {
            // Try to get client brand using reflection
            Method getHandleMethod = player.getClass().getMethod("getHandle");
            Object entityPlayer = getHandleMethod.invoke(player);
            
            // This would need to be adapted based on server implementation
            // For now, return null as it's not universally available
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Check client brand for Bedrock indicators.
     */
    private boolean checkClientBrand(String clientBrand) {
        if (clientBrand == null) {
            return false;
        }
        
        String lower = clientBrand.toLowerCase();
        return lower.contains("mcpe") || 
               lower.contains("bedrock") || 
               lower.contains("pocket") ||
               lower.contains("geyser");
    }
    
    /**
     * Analyze behavioral patterns to detect Bedrock players.
     */
    private int analyzeBehavioralPatterns(Player player) {
        int score = 0;
        
        // Note: This is a simplified behavioral analysis
        // In practice, you'd track player behavior over time
        
        // Check inventory interaction patterns
        // Bedrock players often have different interaction patterns
        
        // Check movement patterns
        // Bedrock movement can be slightly different due to touch controls
        
        // Check chat patterns
        // Bedrock players might use different chat features
        
        // For now, return a default score
        return 30; // Not confident enough to detect as Bedrock
    }
    
    /**
     * Register a player as Bedrock after detection.
     */
    private void registerBedrockPlayer(Player player, BedrockDetectionResult result) {
        UUID uuid = player.getUniqueId();
        
        bedrockPlayers.add(uuid);
        
        BedrockPlayerInfo info = new BedrockPlayerInfo();
        info.setDetectionResult(result);
        info.setDetectionTime(System.currentTimeMillis());
        
        bedrockPlayerInfo.put(uuid, info);
        
        // Bedrock player registered silently
        
        // Apply Bedrock-specific adaptations
        applyBedrockAdaptations(player, info);
    }
    
    /**
     * Apply Bedrock-specific adaptations for a player.
     */
    private void applyBedrockAdaptations(Player player, BedrockPlayerInfo info) {
        if (!plugin.getConfig().getBoolean("bedrock-compatibility.enabled", true)) {
            return;
        }
        
        // Send welcome message for Bedrock players
        if (adaptMessages) {
            sendBedrockWelcomeMessage(player, info);
        }
        
        // Apply UI adaptations (this would be used by other modules)
        // The actual UI adaptation happens in the modules that create menus
        
        // Bedrock adaptations applied
    }
    
    /**
     * Send a welcome message adapted for Bedrock players.
     */
    private void sendBedrockWelcomeMessage(Player player, BedrockPlayerInfo info) {
        // Use localization manager if available
        if (plugin.getModuleManager() != null) {
            // This would use the LocalizationManager
            player.sendMessage("§a[AntiAFK] §7Bedrock Edition detected! UI adapted for touch controls.");
        } else {
            player.sendMessage("§a[AntiAFK] §7Bedrock Edition detected! UI adapted for touch controls.");
        }
        
        // Send additional tips based on device type
        BedrockDetectionResult result = info.getDetectionResult();
        if (result.getDeviceType() != null) {
            switch (result.getDeviceType().toLowerCase()) {
                case "mobile":
                    player.sendMessage("§7💡 Tip: Use longer taps for inventory actions");
                    break;
                case "console":
                    player.sendMessage("§7💡 Tip: Controller navigation adapted");
                    break;
                case "desktop":
                    player.sendMessage("§7💡 Tip: Keyboard shortcuts available");
                    break;
            }
        }
    }
    
    // ============= EVENT HANDLERS =============
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Detect Bedrock player with a slight delay to ensure all data is available
        plugin.getPlatformScheduler().runTaskLater(() -> {
            BedrockDetectionResult result = detectBedrockPlayer(player);
            
            if (result.isBedrockPlayer()) {
                registerBedrockPlayer(player, result);
            }
        }, 20L); // 1 second delay
    }
    
    // ============= PUBLIC API =============
    
    /**
     * Get Bedrock player information.
     */
    public BedrockPlayerInfo getBedrockPlayerInfo(Player player) {
        return bedrockPlayerInfo.get(player.getUniqueId());
    }
    
    /**
     * Get adapted menu size for a player.
     */
    public int getAdaptedMenuSize(Player player, int defaultSize) {
        if (!isBedrockPlayer(player) || !adaptMenus) {
            return defaultSize;
        }
        
        // Bedrock has limitations on inventory size
        int maxSize = plugin.getConfig().getInt("bedrock-compatibility.ui-adaptations.max-menu-slots", 27);
        return Math.min(defaultSize, maxSize);
    }
    
    /**
     * Check if shift-click should be avoided for a player.
     */
    public boolean shouldAvoidShiftClick(Player player) {
        return isBedrockPlayer(player) && adaptItemInteractions &&
               plugin.getConfig().getBoolean("bedrock-compatibility.ui-adaptations.avoid-shift-click", true);
    }
    
    /**
     * Check if complex formatting should be avoided for a player.
     */
    public boolean shouldUseSimpleFormatting(Player player) {
        return isBedrockPlayer(player) && adaptMessages &&
               plugin.getConfig().getBoolean("bedrock-compatibility.message-adaptations.avoid-complex-formatting", true);
    }
    
    /**
     * Get adapted message length for a player.
     */
    public String getAdaptedMessage(Player player, String message) {
        if (!isBedrockPlayer(player) || !adaptMessages || message == null) {
            return message;
        }
        
        // Shorten messages for Bedrock players if configured
        if (plugin.getConfig().getBoolean("bedrock-compatibility.message-adaptations.shorter-messages", true)) {
            int maxLength = plugin.getConfig().getInt("bedrock-compatibility.message-adaptations.max-length", 100);
            if (message.length() > maxLength) {
                return message.substring(0, maxLength - 3) + "...";
            }
        }
        
        return message;
    }
    
    /**
     * Clear player data on disconnect.
     */
    public void clearPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        bedrockPlayers.remove(uuid);
        bedrockPlayerInfo.remove(uuid);
    }
    
    /**
     * Get Bedrock compatibility statistics.
     */
    public BedrockCompatibilityStats getStats() {
        return new BedrockCompatibilityStats(
            bedrockPlayers.size(),
            bedrockPlayerInfo.size(),
            floodgateAvailable,
            geyserAvailable,
            customDetectionEnabled
        );
    }
    
    // ============= INNER CLASSES =============
    
    /**
     * Bedrock detection result.
     */
    public static class BedrockDetectionResult {
        private Player player;
        private boolean bedrockPlayer;
        private String detectionMethod;
        private int confidence;
        private String deviceType;
        private String inputMode;
        private String uiProfile;
        private String bedrockVersion;
        private String clientBrand;
        
        // Getters and setters
        public Player getPlayer() { return player; }
        public void setPlayer(Player player) { this.player = player; }
        public boolean isBedrockPlayer() { return bedrockPlayer; }
        public void setBedrockPlayer(boolean bedrockPlayer) { this.bedrockPlayer = bedrockPlayer; }
        public String getDetectionMethod() { return detectionMethod; }
        public void setDetectionMethod(String detectionMethod) { this.detectionMethod = detectionMethod; }
        public int getConfidence() { return confidence; }
        public void setConfidence(int confidence) { this.confidence = confidence; }
        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
        public String getInputMode() { return inputMode; }
        public void setInputMode(String inputMode) { this.inputMode = inputMode; }
        public String getUiProfile() { return uiProfile; }
        public void setUiProfile(String uiProfile) { this.uiProfile = uiProfile; }
        public String getBedrockVersion() { return bedrockVersion; }
        public void setBedrockVersion(String bedrockVersion) { this.bedrockVersion = bedrockVersion; }
        public String getClientBrand() { return clientBrand; }
        public void setClientBrand(String clientBrand) { this.clientBrand = clientBrand; }
    }
    
    /**
     * Bedrock player information storage.
     */
    public static class BedrockPlayerInfo {
        private BedrockDetectionResult detectionResult;
        private long detectionTime;
        private Map<String, Object> adaptationSettings = new HashMap<>();
        
        public BedrockDetectionResult getDetectionResult() { return detectionResult; }
        public void setDetectionResult(BedrockDetectionResult detectionResult) { this.detectionResult = detectionResult; }
        public long getDetectionTime() { return detectionTime; }
        public void setDetectionTime(long detectionTime) { this.detectionTime = detectionTime; }
        public Map<String, Object> getAdaptationSettings() { return adaptationSettings; }
        public void setAdaptationSettings(Map<String, Object> adaptationSettings) { this.adaptationSettings = adaptationSettings; }
    }
    
    /**
     * Bedrock compatibility statistics.
     */
    public static class BedrockCompatibilityStats {
        private final int bedrockPlayerCount;
        private final int totalDetectedPlayers;
        private final boolean floodgateAvailable;
        private final boolean geyserAvailable;
        private final boolean customDetectionEnabled;
        
        public BedrockCompatibilityStats(int bedrockPlayerCount, int totalDetectedPlayers,
                                       boolean floodgateAvailable, boolean geyserAvailable,
                                       boolean customDetectionEnabled) {
            this.bedrockPlayerCount = bedrockPlayerCount;
            this.totalDetectedPlayers = totalDetectedPlayers;
            this.floodgateAvailable = floodgateAvailable;
            this.geyserAvailable = geyserAvailable;
            this.customDetectionEnabled = customDetectionEnabled;
        }
        
        public int getBedrockPlayerCount() { return bedrockPlayerCount; }
        public int getTotalDetectedPlayers() { return totalDetectedPlayers; }
        public boolean isFloodgateAvailable() { return floodgateAvailable; }
        public boolean isGeyserAvailable() { return geyserAvailable; }
        public boolean isCustomDetectionEnabled() { return customDetectionEnabled; }
    }
}