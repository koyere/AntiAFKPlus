package me.koyere.antiafkplus.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.config.ConfigManager;
import me.koyere.antiafkplus.performance.PerformanceOptimizer;

/**
 * Manages all in-game GUI menus for AntiAFKPlus.
 * Provides settings panels, module toggles, and real-time status displays.
 */
@SuppressWarnings("deprecation")
public class GUIManager implements Listener {

    private static final String MAIN_MENU_TITLE = ChatColor.DARK_GRAY + "AntiAFK+ " + ChatColor.GOLD + "Settings";
    private static final String DETECTION_TITLE = ChatColor.DARK_GRAY + "AntiAFK+ " + ChatColor.GOLD + "Detection";
    private static final String MODULE_TITLE = ChatColor.DARK_GRAY + "AntiAFK+ " + ChatColor.GOLD + "Modules";
    private static final String GENERAL_TITLE = ChatColor.DARK_GRAY + "AntiAFK+ " + ChatColor.GOLD + "General";
    private static final String ZONE_TITLE = ChatColor.DARK_GRAY + "AntiAFK+ " + ChatColor.GOLD + "Zones";
    private static final String REWARD_TITLE = ChatColor.DARK_GRAY + "AntiAFK+ " + ChatColor.GOLD + "Rewards";
    private static final String LANGUAGE_TITLE = ChatColor.DARK_GRAY + "AntiAFK+ " + ChatColor.GOLD + "Language";

    private static final List<String> FEATURE_MODULES = Arrays.asList(
            "pattern-detection",
            "autoclick-detection",
            "player-protection",
            "afk-zones",
            "reward-system",
            "visual-effects",
            "database",
            "analytics"
    );

    private static final List<String[]> LANGUAGES = Arrays.asList(
            new String[]{"en", "English", "BOOK"},
            new String[]{"es", "Español", "BOOK"},
            new String[]{"fr", "Français", "BOOK"},
            new String[]{"de", "Deutsch", "BOOK"},
            new String[]{"pt", "Português", "BOOK"},
            new String[]{"ru", "Русский", "BOOK"},
            new String[]{"zh", "中文", "BOOK"},
            new String[]{"ja", "日本語", "BOOK"},
            new String[]{"ko", "한국어", "BOOK"},
            new String[]{"it", "Italiano", "BOOK"}
    );

    private final AntiAFKPlus plugin;
    private final Map<UUID, GUIType> openGUIs = new ConcurrentHashMap<>();

    public GUIManager(AntiAFKPlus plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // ======================== Menu Openers ========================

    /**
     * Opens the main settings menu for a player.
     */
    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, MAIN_MENU_TITLE);

        // Slot 4: Plugin info head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setDisplayName(color("&6AntiAFK+ &7v" + plugin.getPluginVersion()));
            List<String> headLore = new ArrayList<>();
            headLore.add(color("&8Advanced AFK Detection & Management"));
            headLore.add("");
            headLore.add(color("&7Modules loaded: &f" + plugin.getModuleManager().getModuleCount()));
            headLore.add(color("&7Modules enabled: &a" + plugin.getModuleManager().getEnabledModuleCount()));
            headLore.add(color("&7API version: &f" + plugin.getAPIVersion()));
            skullMeta.setLore(headLore);
            head.setItemMeta(skullMeta);
        }
        inv.setItem(4, head);

        // Slot 19: Detection Settings
        inv.setItem(19, createItem(Material.COMPASS, "&e⚙ Detection Settings",
                "&7Configure pattern detection",
                "&7thresholds and analysis.",
                "",
                "&aClick to open"));

        // Slot 21: Module Toggles
        inv.setItem(21, createItem(Material.REDSTONE_TORCH, "&e⚙ Module Toggles",
                "&7Enable or disable individual",
                "&7plugin modules.",
                "",
                "&aClick to open"));

        // Slot 23: Credit System toggle
        boolean creditEnabled = plugin.getConfig().getBoolean("credit-system.enabled", false);
        inv.setItem(23, createItem(Material.GOLD_INGOT, "&e💰 Credit System",
                "&7Status: " + (creditEnabled ? "&aEnabled" : "&cDisabled"),
                "",
                "&7Credits allow players to earn",
                "&7AFK protection time.",
                "",
                "&aClick to toggle",
                "&c⚠ Requires server restart to fully apply"));

        // Slot 25: Performance
        inv.setItem(25, buildPerformanceItem());

        // Slot 37: Profile selector
        inv.setItem(37, buildProfileItem());

        // Slot 39: Debug Toggle
        boolean debug = plugin.isDebugEnabled();
        inv.setItem(39, createItem(Material.COMMAND_BLOCK, "&e🔧 Debug Mode",
                "&7Status: " + (debug ? "&aEnabled" : "&cDisabled"),
                "",
                "&7Toggles verbose debug logging.",
                "&aClick to toggle"));

        // Slot 41: Reload Config
        inv.setItem(41, createItem(Material.ANVIL, "&e🔄 Reload Config",
                "&7Reloads configuration and",
                "&7language files from disk.",
                "",
                "&aClick to reload"));

        // Slot 29: General Settings
        inv.setItem(29, createItem(Material.COMPARATOR, "&e⚙ General Settings",
                "&7AFK times, check intervals,",
                "&7and feature toggles.",
                "",
                "&aClick to open"));

        // Slot 31: Zone Settings
        inv.setItem(31, createItem(Material.GRASS_BLOCK, "&e🗺 Zone Settings",
                "&7Configure AFK zone",
                "&7management and timeouts.",
                "",
                "&aClick to open"));

        // Slot 33: Reward Settings
        inv.setItem(33, createItem(Material.DIAMOND, "&e🎁 Reward Settings",
                "&7Configure AFK reward",
                "&7system and limits.",
                "",
                "&aClick to open"));

        // Slot 43: Language Selector
        String currentLang = plugin.getConfig().getString("internationalization.default-language", "en");
        inv.setItem(43, createItem(Material.WRITABLE_BOOK, "&e🌍 Language",
                "&7Current: &f" + currentLang,
                "",
                "&aClick to change"));

        // Fill empty border slots with glass panes
        fillBorder(inv);

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), GUIType.MAIN_MENU);
    }

    /**
     * Opens the detection settings submenu.
     */
    public void openDetectionSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, DETECTION_TITLE);
        ConfigManager cfg = plugin.getConfigManager();

        // Slot 4: Title
        inv.setItem(4, createItem(Material.COMPASS, "&6Detection Settings",
                "&8Configure pattern detection parameters"));

        // Slot 19: Water Circle Radius
        double radius = cfg.getWaterCircleRadius();
        int radiusAmount = Math.max(1, Math.min(64, (int) radius));
        ItemStack radiusItem = createItem(Material.WATER_BUCKET, "&b Water Circle Radius",
                "&7Current: &f" + radius + " blocks",
                "",
                "&7Radius used to detect circular",
                "&7water pool AFK patterns.");
        radiusItem.setAmount(radiusAmount);
        inv.setItem(19, radiusItem);

        // Slot 21: Min Samples
        int minSamples = cfg.getMinSamplesForPattern();
        int samplesAmount = Math.max(1, Math.min(64, minSamples / 4));
        ItemStack samplesItem = createItem(Material.PAPER, "&f Min Samples",
                "&7Current: &f" + minSamples,
                "",
                "&7Minimum movement samples needed",
                "&7before pattern analysis runs.");
        samplesItem.setAmount(samplesAmount);
        inv.setItem(21, samplesItem);

        // Slot 23: Max Violations
        int maxViolations = cfg.getMaxPatternViolations();
        int violationsAmount = Math.max(1, Math.min(64, maxViolations));
        ItemStack violationsItem = createItem(Material.TNT, "&c Max Violations",
                "&7Current: &f" + maxViolations,
                "",
                "&7Maximum pattern violations before",
                "&7action is taken on the player.");
        violationsItem.setAmount(violationsAmount);
        inv.setItem(23, violationsItem);

        // Slot 25: Pattern Analysis Interval
        long intervalMs = cfg.getPatternAnalysisInterval();
        long intervalSec = intervalMs / 1000;
        inv.setItem(25, createItem(Material.CLOCK, "&e Pattern Analysis Interval",
                "&7Current: &f" + intervalSec + "s &7(" + intervalMs + "ms)",
                "",
                "&7How often the pattern analysis",
                "&7engine runs its checks."));

        // Slot 37: Linear Movement Exclusion toggle
        boolean linearExclusion = cfg.isLinearMovementExclusionEnabled();
        inv.setItem(37, createToggle("Linear Movement Exclusion", linearExclusion,
                "&7Excludes straight-line movement",
                "&7from pattern detection to reduce",
                "&7false positives."));

        // Slot 39: Large Pool Detection toggle
        boolean largePool = cfg.isLargePoolDetectionEnabled();
        inv.setItem(39, createToggle("Large Pool Detection", largePool,
                "&7Detects players AFK in large",
                "&7water pools or open areas."));

        // Slot 41: Keystroke Timeout toggle
        boolean keystroke = cfg.isKeystrokeTimeoutDetectionEnabled();
        inv.setItem(41, createToggle("Keystroke Timeout", keystroke,
                "&7Detects lack of keyboard input",
                "&7over extended periods."));

        // Slot 49: Back button
        inv.setItem(49, createItem(Material.ARROW, "&c← Back",
                "&7Return to main menu"));

        fillBorder(inv);

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), GUIType.DETECTION_SETTINGS);
    }

    /**
     * Opens the module toggles submenu.
     */
    public void openModuleSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, MODULE_TITLE);

        // Slot 4: Title
        inv.setItem(4, createItem(Material.REDSTONE_TORCH, "&6Module Toggles",
                "&8Enable or disable plugin modules"));

        // Place each feature module as a toggle
        int[] moduleSlots = {19, 20, 21, 22, 23, 24, 25, 26};
        for (int i = 0; i < FEATURE_MODULES.size() && i < moduleSlots.length; i++) {
            String moduleName = FEATURE_MODULES.get(i);
            boolean enabled = plugin.getModuleManager().isModuleEnabled(moduleName);
            String displayName = formatModuleName(moduleName);
            inv.setItem(moduleSlots[i], createToggle(displayName, enabled,
                    "&7Module: &f" + moduleName,
                    "",
                    "&aClick to toggle"));
        }

        // Slot 49: Back button
        inv.setItem(49, createItem(Material.ARROW, "&c← Back",
                "&7Return to main menu"));

        fillBorder(inv);

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), GUIType.MODULE_SETTINGS);
    }

    /**
     * Opens the general settings submenu.
     */
    public void openGeneralSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, GENERAL_TITLE);
        var cfg = plugin.getConfig();

        inv.setItem(4, createItem(Material.COMPARATOR, "&6General Settings",
                "&8AFK times and feature toggles"));

        // Slot 19: Default AFK Time
        int afkTime = cfg.getInt("default-afk-time", 300);
        ItemStack afkItem = createItem(Material.CLOCK, "&e Default AFK Time",
                "&7Current: &f" + afkTime + "s &7(" + (afkTime/60) + " min)",
                "", "&a+Click: increase", "&c+Sneak+Click: decrease");
        afkItem.setAmount(Math.max(1, Math.min(64, afkTime / 30)));
        inv.setItem(19, afkItem);

        // Slot 21: Check Interval
        int interval = cfg.getInt("afk-check-interval-seconds", 5);
        ItemStack intervalItem = createItem(Material.REPEATER, "&e Check Interval",
                "&7Current: &f" + interval + "s",
                "", "&a+Click: increase", "&c+Sneak+Click: decrease");
        intervalItem.setAmount(Math.max(1, Math.min(64, interval)));
        inv.setItem(21, intervalItem);

        // Slot 23: Max Voluntary AFK Time
        int volTime = cfg.getInt("max-voluntary-afk-time-seconds", 600);
        ItemStack volItem = createItem(Material.HOPPER, "&e Max Voluntary AFK",
                "&7Current: &f" + volTime + "s &7(" + (volTime/60) + " min)",
                "", "&a+Click: increase", "&c+Sneak+Click: decrease");
        volItem.setAmount(Math.max(1, Math.min(64, volTime / 60)));
        inv.setItem(23, volItem);

        // Slot 37: Block Pickup toggle
        boolean blockPickup = cfg.getBoolean("block-pickup-while-afk", true);
        inv.setItem(37, createToggle("Block Pickup While AFK", blockPickup,
                "&7Prevents AFK players from",
                "&7picking up items."));

        // Slot 39: Broadcast AFK toggle
        boolean broadcast = cfg.getBoolean("broadcast-afk-state-changes", true);
        inv.setItem(39, createToggle("Broadcast AFK Changes", broadcast,
                "&7Broadcasts when players go",
                "&7AFK or return."));

        inv.setItem(49, createItem(Material.ARROW, "&c← Back", "&7Return to main menu"));
        fillBorder(inv);
        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), GUIType.GENERAL_SETTINGS);
    }

    /**
     * Opens the zone settings submenu.
     */
    public void openZoneSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ZONE_TITLE);
        var cfg = plugin.getConfig();

        inv.setItem(4, createItem(Material.GRASS_BLOCK, "&6Zone Management",
                "&8Configure AFK zone settings"));

        // Slot 19: Zone enabled toggle
        boolean zoneEnabled = cfg.getBoolean("zone-management.enabled", false);
        inv.setItem(19, createToggle("Zone Management", zoneEnabled,
                "&7Enable zone-based AFK management.",
                "&7Requires WorldGuard."));

        // Slot 21: Require WorldGuard toggle
        boolean reqWG = cfg.getBoolean("zone-management.require-worldguard", true);
        inv.setItem(21, createToggle("Require WorldGuard", reqWG,
                "&7Require WorldGuard plugin",
                "&7for zone features."));

        // Slot 23: Default AFK Allowed toggle
        boolean afkAllowed = cfg.getBoolean("zone-management.default-afk-allowed", true);
        inv.setItem(23, createToggle("Default AFK Allowed", afkAllowed,
                "&7Allow AFK by default in",
                "&7zones without explicit config."));

        // Slot 25: Default Timeout
        int timeout = cfg.getInt("zone-management.default-timeout-seconds", 300);
        ItemStack timeoutItem = createItem(Material.CLOCK, "&e Default Zone Timeout",
                "&7Current: &f" + timeout + "s &7(" + (timeout/60) + " min)",
                "", "&a+Click: increase", "&c+Sneak+Click: decrease");
        timeoutItem.setAmount(Math.max(1, Math.min(64, timeout / 60)));
        inv.setItem(25, timeoutItem);

        // Slot 37: Region Inheritance toggle
        boolean inheritance = cfg.getBoolean("zone-management.allow-region-inheritance", true);
        inv.setItem(37, createToggle("Region Inheritance", inheritance,
                "&7Allow zones to inherit settings",
                "&7from parent WorldGuard regions."));

        inv.setItem(49, createItem(Material.ARROW, "&c← Back", "&7Return to main menu"));
        fillBorder(inv);
        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), GUIType.ZONE_SETTINGS);
    }

    /**
     * Opens the reward settings submenu.
     */
    public void openRewardSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, REWARD_TITLE);
        var cfg = plugin.getConfig();

        inv.setItem(4, createItem(Material.DIAMOND, "&6Reward System",
                "&8Configure AFK reward settings"));

        // Slot 19: Reward enabled toggle
        boolean enabled = cfg.getBoolean("reward-system.enabled", false);
        inv.setItem(19, createToggle("Reward System", enabled,
                "&7Enable rewards for AFK players."));

        // Slot 21: Require Vault toggle
        boolean reqVault = cfg.getBoolean("reward-system.require-vault", false);
        inv.setItem(21, createToggle("Require Vault", reqVault,
                "&7Require Vault economy plugin",
                "&7for monetary rewards."));

        // Slot 23: Max Daily Rewards
        int maxDaily = cfg.getInt("reward-system.max-daily-rewards", 144);
        ItemStack maxItem = createItem(Material.CHEST, "&e Max Daily Rewards",
                "&7Current: &f" + maxDaily,
                "", "&a+Click: increase", "&c+Sneak+Click: decrease");
        maxItem.setAmount(Math.max(1, Math.min(64, maxDaily / 10)));
        inv.setItem(23, maxItem);

        // Slot 25: Required Active Time
        int activeTime = cfg.getInt("reward-system.require-active-time-minutes", 30);
        ItemStack activeItem = createItem(Material.EXPERIENCE_BOTTLE, "&e Required Active Time",
                "&7Current: &f" + activeTime + " min",
                "", "&a+Click: increase", "&c+Sneak+Click: decrease");
        activeItem.setAmount(Math.max(1, Math.min(64, activeTime / 5)));
        inv.setItem(25, activeItem);

        // Slot 37: IP-based Limits toggle
        boolean ipLimits = cfg.getBoolean("reward-system.ip-based-limits", true);
        inv.setItem(37, createToggle("IP-Based Limits", ipLimits,
                "&7Limit rewards per IP address",
                "&7to prevent alt abuse."));

        inv.setItem(49, createItem(Material.ARROW, "&c← Back", "&7Return to main menu"));
        fillBorder(inv);
        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), GUIType.REWARD_SETTINGS);
    }

    /**
     * Opens the language selector submenu.
     */
    public void openLanguageSelector(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, LANGUAGE_TITLE);
        String currentLang = plugin.getConfig().getString("internationalization.default-language", "en");

        inv.setItem(4, createItem(Material.WRITABLE_BOOK, "&6Language Selection",
                "&8Select the server language",
                "&7Current: &f" + currentLang));

        int[] langSlots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30};
        for (int i = 0; i < LANGUAGES.size() && i < langSlots.length; i++) {
            String[] lang = LANGUAGES.get(i);
            String code = lang[0];
            String name = lang[1];
            boolean isActive = code.equals(currentLang);
            Material mat = isActive ? Material.ENCHANTED_BOOK : Material.BOOK;
            inv.setItem(langSlots[i], createItem(mat, (isActive ? "&a✔ " : "&7") + name,
                    "&7Code: &f" + code,
                    isActive ? "&aCurrently active" : "&eClick to select"));
        }

        inv.setItem(49, createItem(Material.ARROW, "&c← Back", "&7Return to main menu"));
        fillBorder(inv);
        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), GUIType.LANGUAGE_SELECTOR);
    }

    /**
     * Closes all open GUIs and clears tracking state.
     * Called during plugin shutdown.
     */
    public void shutdown() {
        for (UUID uuid : openGUIs.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.closeInventory();
            }
        }
        openGUIs.clear();
    }

    // ======================== Event Handlers ========================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        GUIType guiType = openGUIs.get(uuid);
        if (guiType == null) {
            return;
        }

        // Always cancel clicks in our GUIs
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        int slot = event.getRawSlot();

        switch (guiType) {
            case MAIN_MENU -> handleMainMenuClick(player, slot);
            case DETECTION_SETTINGS -> handleDetectionClick(player, slot);
            case MODULE_SETTINGS -> handleModuleClick(player, slot);
            case CREDIT_SETTINGS -> { /* Reserved for future use */ }
            case GENERAL_SETTINGS -> handleGeneralClick(player, slot);
            case ZONE_SETTINGS -> handleZoneClick(player, slot);
            case REWARD_SETTINGS -> handleRewardClick(player, slot);
            case LANGUAGE_SELECTOR -> handleLanguageClick(player, slot);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            openGUIs.remove(player.getUniqueId());
        }
    }

    // ======================== Click Handlers ========================

    private void handleMainMenuClick(Player player, int slot) {
        switch (slot) {
            case 19 -> openDetectionSettings(player);
            case 21 -> openModuleSettings(player);
            case 23 -> {
                boolean current = plugin.getConfig().getBoolean("credit-system.enabled", false);
                plugin.getConfig().set("credit-system.enabled", !current);
                plugin.saveConfig();
                sendToggleMessage(player, "Credit System", !current, true);
                openMainMenu(player);
            }
            case 25 -> {
                player.sendMessage(msg("gui.performance-refreshed"));
                openMainMenu(player);
            }
            case 37 -> cycleProfile(player);
            case 39 -> toggleDebug(player);
            case 41 -> reloadConfig(player);
            case 43 -> openLanguageSelector(player);
            case 29 -> openGeneralSettings(player);
            case 31 -> openZoneSettings(player);
            case 33 -> openRewardSettings(player);
        }
    }

    private void handleDetectionClick(Player player, int slot) {
        switch (slot) {
            case 37 -> {
                boolean current = plugin.getConfigManager().isLinearMovementExclusionEnabled();
                plugin.getConfig().set("modules.pattern-detection.linear-movement-exclusion", !current);
                plugin.saveConfig();
                plugin.getConfigManager().reloadConfiguration();
                sendToggleMessage(player, "Linear Movement Exclusion", !current, false);
                openDetectionSettings(player);
            }
            case 39 -> {
                boolean current = plugin.getConfigManager().isLargePoolDetectionEnabled();
                plugin.getConfig().set("modules.pattern-detection.large-pool-detection", !current);
                plugin.saveConfig();
                plugin.getConfigManager().reloadConfiguration();
                sendToggleMessage(player, "Large Pool Detection", !current, false);
                openDetectionSettings(player);
            }
            case 41 -> {
                boolean current = plugin.getConfigManager().isKeystrokeTimeoutDetectionEnabled();
                plugin.getConfig().set("modules.pattern-detection.keystroke-timeout-detection", !current);
                plugin.saveConfig();
                plugin.getConfigManager().reloadConfiguration();
                sendToggleMessage(player, "Keystroke Timeout", !current, false);
                openDetectionSettings(player);
            }
            case 49 -> openMainMenu(player);
        }
    }

    private void handleModuleClick(Player player, int slot) {
        if (slot == 49) {
            openMainMenu(player);
            return;
        }

        int[] moduleSlots = {19, 20, 21, 22, 23, 24, 25, 26};
        for (int i = 0; i < moduleSlots.length && i < FEATURE_MODULES.size(); i++) {
            if (slot == moduleSlots[i]) {
                String moduleName = FEATURE_MODULES.get(i);
                boolean current = plugin.getModuleManager().isModuleEnabled(moduleName);
                boolean newState = !current;

                plugin.getConfig().set("modules." + moduleName + ".enabled", newState);
                plugin.saveConfig();
                plugin.getConfigManager().reloadConfiguration();

                sendToggleMessage(player, formatModuleName(moduleName), newState, false);
                openModuleSettings(player);
                return;
            }
        }
    }

    private void handleGeneralClick(Player player, int slot) {
        var cfg = plugin.getConfig();
        switch (slot) {
            case 19 -> {
                // AFK Time: +30s click, -30s sneak+click
                int current = cfg.getInt("default-afk-time", 300);
                int newVal = player.isSneaking() ? Math.max(30, current - 30) : current + 30;
                cfg.set("default-afk-time", newVal);
                plugin.saveConfig();
                plugin.getConfigManager().reloadConfiguration();
                openGeneralSettings(player);
            }
            case 21 -> {
                int current = cfg.getInt("afk-check-interval-seconds", 5);
                int newVal = player.isSneaking() ? Math.max(1, current - 1) : Math.min(60, current + 1);
                cfg.set("afk-check-interval-seconds", newVal);
                plugin.saveConfig();
                plugin.getConfigManager().reloadConfiguration();
                openGeneralSettings(player);
            }
            case 23 -> {
                int current = cfg.getInt("max-voluntary-afk-time-seconds", 600);
                int newVal = player.isSneaking() ? Math.max(60, current - 60) : current + 60;
                cfg.set("max-voluntary-afk-time-seconds", newVal);
                plugin.saveConfig();
                plugin.getConfigManager().reloadConfiguration();
                openGeneralSettings(player);
            }
            case 37 -> {
                boolean current = cfg.getBoolean("block-pickup-while-afk", true);
                cfg.set("block-pickup-while-afk", !current);
                plugin.saveConfig();
                plugin.getConfigManager().reloadConfiguration();
                sendToggleMessage(player, "Block Pickup While AFK", !current, false);
                openGeneralSettings(player);
            }
            case 39 -> {
                boolean current = cfg.getBoolean("broadcast-afk-state-changes", true);
                cfg.set("broadcast-afk-state-changes", !current);
                plugin.saveConfig();
                plugin.getConfigManager().reloadConfiguration();
                sendToggleMessage(player, "Broadcast AFK Changes", !current, false);
                openGeneralSettings(player);
            }
            case 49 -> openMainMenu(player);
        }
    }

    private void handleZoneClick(Player player, int slot) {
        var cfg = plugin.getConfig();
        switch (slot) {
            case 19 -> {
                boolean current = cfg.getBoolean("zone-management.enabled", false);
                cfg.set("zone-management.enabled", !current);
                plugin.saveConfig();
                plugin.getConfigManager().reloadConfiguration();
                sendToggleMessage(player, "Zone Management", !current, false);
                openZoneSettings(player);
            }
            case 21 -> {
                boolean current = cfg.getBoolean("zone-management.require-worldguard", true);
                cfg.set("zone-management.require-worldguard", !current);
                plugin.saveConfig();
                plugin.getConfigManager().reloadConfiguration();
                sendToggleMessage(player, "Require WorldGuard", !current, false);
                openZoneSettings(player);
            }
            case 23 -> {
                boolean current = cfg.getBoolean("zone-management.default-afk-allowed", true);
                cfg.set("zone-management.default-afk-allowed", !current);
                plugin.saveConfig();
                plugin.getConfigManager().reloadConfiguration();
                sendToggleMessage(player, "Default AFK Allowed", !current, false);
                openZoneSettings(player);
            }
            case 25 -> {
                int current = cfg.getInt("zone-management.default-timeout-seconds", 300);
                int newVal = player.isSneaking() ? Math.max(60, current - 60) : current + 60;
                cfg.set("zone-management.default-timeout-seconds", newVal);
                plugin.saveConfig();
                plugin.getConfigManager().reloadConfiguration();
                openZoneSettings(player);
            }
            case 37 -> {
                boolean current = cfg.getBoolean("zone-management.allow-region-inheritance", true);
                cfg.set("zone-management.allow-region-inheritance", !current);
                plugin.saveConfig();
                plugin.getConfigManager().reloadConfiguration();
                sendToggleMessage(player, "Region Inheritance", !current, false);
                openZoneSettings(player);
            }
            case 49 -> openMainMenu(player);
        }
    }

    private void handleRewardClick(Player player, int slot) {
        var cfg = plugin.getConfig();
        switch (slot) {
            case 19 -> {
                boolean current = cfg.getBoolean("reward-system.enabled", false);
                cfg.set("reward-system.enabled", !current);
                plugin.saveConfig();
                plugin.getConfigManager().reloadConfiguration();
                sendToggleMessage(player, "Reward System", !current, false);
                openRewardSettings(player);
            }
            case 21 -> {
                boolean current = cfg.getBoolean("reward-system.require-vault", false);
                cfg.set("reward-system.require-vault", !current);
                plugin.saveConfig();
                plugin.getConfigManager().reloadConfiguration();
                sendToggleMessage(player, "Require Vault", !current, false);
                openRewardSettings(player);
            }
            case 23 -> {
                int current = cfg.getInt("reward-system.max-daily-rewards", 144);
                int newVal = player.isSneaking() ? Math.max(10, current - 10) : current + 10;
                cfg.set("reward-system.max-daily-rewards", newVal);
                plugin.saveConfig();
                openRewardSettings(player);
            }
            case 25 -> {
                int current = cfg.getInt("reward-system.require-active-time-minutes", 30);
                int newVal = player.isSneaking() ? Math.max(5, current - 5) : current + 5;
                cfg.set("reward-system.require-active-time-minutes", newVal);
                plugin.saveConfig();
                openRewardSettings(player);
            }
            case 37 -> {
                boolean current = cfg.getBoolean("reward-system.ip-based-limits", true);
                cfg.set("reward-system.ip-based-limits", !current);
                plugin.saveConfig();
                sendToggleMessage(player, "IP-Based Limits", !current, false);
                openRewardSettings(player);
            }
            case 49 -> openMainMenu(player);
        }
    }

    private void handleLanguageClick(Player player, int slot) {
        int[] langSlots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30};
        for (int i = 0; i < langSlots.length && i < LANGUAGES.size(); i++) {
            if (slot == langSlots[i]) {
                String code = LANGUAGES.get(i)[0];
                String name = LANGUAGES.get(i)[1];
                plugin.getConfig().set("internationalization.default-language", code);
                plugin.saveConfig();
                plugin.getConfigManager().reloadConfiguration();
                if (plugin.getLocalizationManager() != null) {
                    plugin.getLocalizationManager().reload();
                }
                player.sendMessage(msg("gui.toggle-enabled").replace("{name}", "Language: " + name));
                openLanguageSelector(player);
                return;
            }
        }
        if (slot == 49) openMainMenu(player);
    }

    // ======================== Actions ========================

    private void cycleProfile(Player player) {
        int currentViolations = plugin.getConfigManager().getMaxPatternViolations();
        String currentProfile = detectCurrentProfile(currentViolations);
        String nextProfile = getNextProfile(currentProfile);

        applyProfile(nextProfile);

        plugin.saveConfig();
        plugin.getConfigManager().reloadConfiguration();

        if (plugin.getAfkManager() != null) {
            plugin.getAfkManager().handleConfigReload();
        }

        player.sendMessage(msg("gui.profile-set").replace("{profile}", capitalize(nextProfile)));
        openMainMenu(player);
    }

    private void toggleDebug(Player player) {
        boolean current = plugin.isDebugEnabled();
        plugin.getConfig().set("debug", !current);
        plugin.saveConfig();
        plugin.getConfigManager().reloadConfiguration();
        sendToggleMessage(player, "Debug Mode", !current, false);
        openMainMenu(player);
    }

    private void reloadConfig(Player player) {
        plugin.getConfigManager().reloadConfiguration();
        if (plugin.getAfkManager() != null) {
            plugin.getAfkManager().handleConfigReload();
        }
        player.sendMessage(msg("gui.config-reloaded"));
        openMainMenu(player);
    }

    /**
     * Sends a localized toggle feedback message to the player.
     */
    private void sendToggleMessage(Player player, String name, boolean enabled, boolean requiresRestart) {
        if (requiresRestart) {
            String status = enabled ? "&aEnabled" : "&cDisabled";
            player.sendMessage(msg("gui.toggle-restart").replace("{name}", name).replace("{status}", color(status)));
        } else {
            String key = enabled ? "gui.toggle-enabled" : "gui.toggle-disabled";
            player.sendMessage(msg(key).replace("{name}", name));
        }
    }

    /**
     * Gets a localized message from the language system using the server's default language.
     */
    private String msg(String key) {
        return plugin.getConfigManager().getMessage(key, "[" + key + "]");
    }

    // ======================== Profile Logic ========================

    private String detectCurrentProfile(int maxViolations) {
        if (maxViolations >= 12) return "conservative";
        if (maxViolations >= 8) return "balanced";
        return "aggressive";
    }

    private String getNextProfile(String current) {
        return switch (current) {
            case "conservative" -> "balanced";
            case "balanced" -> "aggressive";
            default -> "conservative";
        };
    }

    private void applyProfile(String profile) {
        switch (profile) {
            case "conservative" -> {
                plugin.getConfig().set("modules.pattern-detection.max-pattern-violations", 12);
                plugin.getConfig().set("modules.pattern-detection.repetitive-movement-threshold", 0.98);
                plugin.getConfig().set("modules.pattern-detection.min-samples-for-pattern", 50);
                plugin.getConfig().set("modules.pattern-detection.activity-grace-period-seconds", 90);
            }
            case "balanced" -> {
                plugin.getConfig().set("modules.pattern-detection.max-pattern-violations", 8);
                plugin.getConfig().set("modules.pattern-detection.repetitive-movement-threshold", 0.95);
                plugin.getConfig().set("modules.pattern-detection.min-samples-for-pattern", 40);
                plugin.getConfig().set("modules.pattern-detection.activity-grace-period-seconds", 60);
            }
            case "aggressive" -> {
                plugin.getConfig().set("modules.pattern-detection.max-pattern-violations", 4);
                plugin.getConfig().set("modules.pattern-detection.repetitive-movement-threshold", 0.85);
                plugin.getConfig().set("modules.pattern-detection.min-samples-for-pattern", 25);
                plugin.getConfig().set("modules.pattern-detection.activity-grace-period-seconds", 30);
            }
        }
    }

    // ======================== Item Builders ========================

    private ItemStack buildPerformanceItem() {
        PerformanceOptimizer optimizer = plugin.getPerformanceOptimizer();
        if (optimizer == null) {
            return createItem(Material.CLOCK, "&e📊 Performance",
                    "&7Performance optimizer not available.");
        }

        PerformanceOptimizer.PerformanceStats stats = optimizer.getPerformanceStats();
        double avgMs = stats.getAverageExecutionTime();
        long memoryMB = stats.getMemoryUsage() / (1024 * 1024);

        return createItem(Material.CLOCK, "&e📊 Performance",
                "&7TPS: &f" + String.format("%.1f", stats.getTps()),
                "&7Avg exec time: &f" + String.format("%.2f", avgMs) + "ms",
                "&7Memory: &f" + memoryMB + " MB",
                "&7Cache entries: &f" + stats.getCacheSize(),
                "&7Operations: &f" + stats.getTotalOperations(),
                "&7High activity: &a" + stats.getHighActivityPlayers(),
                "&7Low activity: &c" + stats.getLowActivityPlayers());
    }

    private ItemStack buildProfileItem() {
        int maxViolations = plugin.getConfigManager().getMaxPatternViolations();
        String profile = detectCurrentProfile(maxViolations);
        String profileColor = switch (profile) {
            case "conservative" -> "&a";
            case "balanced" -> "&e";
            case "aggressive" -> "&c";
            default -> "&7";
        };

        return createItem(Material.BOOK, "&e📋 Detection Profile",
                "&7Current: " + profileColor + capitalize(profile),
                "",
                "&7Conservative: &aLenient &7(12 violations)",
                "&7Balanced: &eModerate &7(8 violations)",
                "&7Aggressive: &cStrict &7(4 violations)",
                "",
                "&aClick to cycle profiles");
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(color(name));
        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(color(line));
        }
        meta.setLore(loreList);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createToggle(String name, boolean enabled, String... description) {
        Material mat = enabled ? Material.LIME_WOOL : Material.RED_WOOL;
        String status = enabled ? "&aEnabled" : "&cDisabled";

        List<String> lore = new ArrayList<>();
        lore.add(color("&7Status: " + status));
        lore.add("");
        for (String line : description) {
            lore.add(color(line));
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(color("&f" + name));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ======================== Utilities ========================

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private void fillBorder(Inventory inv) {
        ItemStack pane = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        // Top row
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, pane);
        }
        // Bottom row
        for (int i = 45; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, pane);
        }
        // Left and right columns
        for (int row = 1; row < 5; row++) {
            int left = row * 9;
            int right = left + 8;
            if (inv.getItem(left) == null) inv.setItem(left, pane);
            if (inv.getItem(right) == null) inv.setItem(right, pane);
        }
    }

    private String formatModuleName(String moduleName) {
        if (moduleName == null) return "Unknown";
        String[] parts = moduleName.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
