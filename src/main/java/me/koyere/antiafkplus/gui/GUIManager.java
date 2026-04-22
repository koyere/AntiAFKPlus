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

    // ======================== Inventory Titles ========================

    private String mainMenuTitle() { return color(msg("gui.main-title")); }
    private String detectionTitle() { return color(msg("gui.det-title")); }
    private String moduleTitle() { return color(msg("gui.mod-title")); }
    private String generalTitle() { return color(msg("gui.gen-title")); }
    private String zoneTitle() { return color(msg("gui.zone-title")); }
    private String rewardTitle() { return color(msg("gui.reward-title")); }
    private String languageTitle() { return color(msg("gui.lang-title")); }

    // ======================== Menu Openers ========================

    /**
     * Opens the main settings menu for a player.
     */
    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, mainMenuTitle());

        // Slot 4: Plugin info head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setDisplayName(color(msg("gui.main-head-name").replace("{version}", plugin.getPluginVersion())));
            List<String> headLore = new ArrayList<>();
            headLore.add(color(msg("gui.main-head-subtitle")));
            headLore.add("");
            headLore.add(color(msg("gui.main-head-modules-loaded").replace("{count}", String.valueOf(plugin.getModuleManager().getModuleCount()))));
            headLore.add(color(msg("gui.main-head-modules-enabled").replace("{count}", String.valueOf(plugin.getModuleManager().getEnabledModuleCount()))));
            headLore.add(color(msg("gui.main-head-api").replace("{version}", plugin.getAPIVersion())));
            skullMeta.setLore(headLore);
            head.setItemMeta(skullMeta);
        }
        inv.setItem(4, head);

        // Slot 19: Detection Settings
        inv.setItem(19, createItem(Material.COMPASS, msg("gui.main-detection"),
                msg("gui.main-detection-lore1"), msg("gui.main-detection-lore2"), "", msg("gui.click-to-open")));

        // Slot 21: Module Toggles
        inv.setItem(21, createItem(Material.REDSTONE_TORCH, msg("gui.main-modules"),
                msg("gui.main-modules-lore1"), msg("gui.main-modules-lore2"), "", msg("gui.click-to-open")));

        // Slot 23: Credit System toggle
        boolean creditEnabled = plugin.getConfig().getBoolean("credit-system.enabled", false);
        String creditStatus = creditEnabled ? msg("gui.status-enabled") : msg("gui.status-disabled");
        inv.setItem(23, createItem(Material.GOLD_INGOT, msg("gui.main-credits"),
                msg("gui.status-label") + creditStatus, "",
                msg("gui.main-credits-lore1"), msg("gui.main-credits-lore2"), "",
                msg("gui.click-to-toggle"), msg("gui.requires-restart")));

        // Slot 25: Performance
        inv.setItem(25, buildPerformanceItem());

        // Slot 37: Profile selector
        inv.setItem(37, buildProfileItem());

        // Slot 39: Debug Toggle
        boolean debug = plugin.isDebugEnabled();
        String debugStatus = debug ? msg("gui.status-enabled") : msg("gui.status-disabled");
        inv.setItem(39, createItem(Material.COMMAND_BLOCK, msg("gui.main-debug"),
                msg("gui.status-label") + debugStatus, "", msg("gui.main-debug-lore"), msg("gui.click-to-toggle")));

        // Slot 41: Reload Config
        inv.setItem(41, createItem(Material.ANVIL, msg("gui.main-reload"),
                msg("gui.main-reload-lore1"), msg("gui.main-reload-lore2"), "", msg("gui.click-to-reload")));

        // Slot 29: General Settings
        inv.setItem(29, createItem(Material.COMPARATOR, msg("gui.main-general"),
                msg("gui.main-general-lore1"), msg("gui.main-general-lore2"), "", msg("gui.click-to-open")));

        // Slot 31: Zone Settings
        inv.setItem(31, createItem(Material.GRASS_BLOCK, msg("gui.main-zones"),
                msg("gui.main-zones-lore1"), msg("gui.main-zones-lore2"), "", msg("gui.click-to-open")));

        // Slot 33: Reward Settings
        inv.setItem(33, createItem(Material.DIAMOND, msg("gui.main-rewards"),
                msg("gui.main-rewards-lore1"), msg("gui.main-rewards-lore2"), "", msg("gui.click-to-open")));

        // Slot 43: Language Selector
        String currentLang = plugin.getConfig().getString("internationalization.default-language", "en");
        inv.setItem(43, createItem(Material.WRITABLE_BOOK, msg("gui.main-language"),
                msg("gui.current").replace("{value}", currentLang), "", msg("gui.click-to-change")));

        // Fill empty border slots with glass panes
        fillBorder(inv);

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), GUIType.MAIN_MENU);
    }

    /**
     * Opens the detection settings submenu.
     */
    public void openDetectionSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, detectionTitle());
        ConfigManager cfg = plugin.getConfigManager();

        // Slot 4: Title
        inv.setItem(4, createItem(Material.COMPASS, msg("gui.det-header"), msg("gui.det-header-lore")));

        // Slot 19: Water Circle Radius
        double radius = cfg.getWaterCircleRadius();
        int radiusAmount = Math.max(1, Math.min(64, (int) radius));
        ItemStack radiusItem = createItem(Material.WATER_BUCKET, msg("gui.det-water-radius"),
                msg("gui.current").replace("{value}", radius + " blocks"), "");
        addLore(radiusItem, lore("gui.det-water-radius-lore"));
        radiusItem.setAmount(radiusAmount);
        inv.setItem(19, radiusItem);

        // Slot 21: Min Samples
        int minSamples = cfg.getMinSamplesForPattern();
        int samplesAmount = Math.max(1, Math.min(64, minSamples / 4));
        ItemStack samplesItem = createItem(Material.PAPER, msg("gui.det-min-samples"),
                msg("gui.current").replace("{value}", String.valueOf(minSamples)), "");
        addLore(samplesItem, lore("gui.det-min-samples-lore"));
        samplesItem.setAmount(samplesAmount);
        inv.setItem(21, samplesItem);

        // Slot 23: Max Violations
        int maxViolations = cfg.getMaxPatternViolations();
        int violationsAmount = Math.max(1, Math.min(64, maxViolations));
        ItemStack violationsItem = createItem(Material.TNT, msg("gui.det-max-violations"),
                msg("gui.current").replace("{value}", String.valueOf(maxViolations)), "");
        addLore(violationsItem, lore("gui.det-max-violations-lore"));
        violationsItem.setAmount(violationsAmount);
        inv.setItem(23, violationsItem);

        // Slot 25: Pattern Analysis Interval
        long intervalMs = cfg.getPatternAnalysisInterval();
        long intervalSec = intervalMs / 1000;
        ItemStack intervalItem = createItem(Material.CLOCK, msg("gui.det-interval"),
                msg("gui.current").replace("{value}", intervalSec + "s (" + intervalMs + "ms)"), "");
        addLore(intervalItem, lore("gui.det-interval-lore"));
        inv.setItem(25, intervalItem);

        // Slot 37: Linear Movement Exclusion toggle
        boolean linearExclusion = cfg.isLinearMovementExclusionEnabled();
        inv.setItem(37, createToggle(msg("gui.det-linear"), linearExclusion,
                lore("gui.det-linear-lore")));

        // Slot 39: Large Pool Detection toggle
        boolean largePool = cfg.isLargePoolDetectionEnabled();
        inv.setItem(39, createToggle(msg("gui.det-large-pool"), largePool,
                lore("gui.det-large-pool-lore")));

        // Slot 41: Keystroke Timeout toggle
        boolean keystroke = cfg.isKeystrokeTimeoutDetectionEnabled();
        inv.setItem(41, createToggle(msg("gui.det-keystroke"), keystroke,
                lore("gui.det-keystroke-lore")));

        // Slot 49: Back button
        inv.setItem(49, createItem(Material.ARROW, msg("gui.back"), msg("gui.back-lore")));

        fillBorder(inv);

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), GUIType.DETECTION_SETTINGS);
    }

    /**
     * Opens the module toggles submenu.
     */
    public void openModuleSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, moduleTitle());

        // Slot 4: Title
        inv.setItem(4, createItem(Material.REDSTONE_TORCH, msg("gui.mod-header"), msg("gui.mod-header-lore")));

        // Place each feature module as a toggle
        int[] moduleSlots = {19, 20, 21, 22, 23, 24, 25, 26};
        for (int i = 0; i < FEATURE_MODULES.size() && i < moduleSlots.length; i++) {
            String moduleName = FEATURE_MODULES.get(i);
            boolean enabled = plugin.getModuleManager().isModuleEnabled(moduleName);
            String displayName = formatModuleName(moduleName);
            inv.setItem(moduleSlots[i], createToggle(displayName, enabled,
                    msg("gui.module-label").replace("{name}", moduleName),
                    "",
                    msg("gui.click-to-toggle")));
        }

        // Slot 49: Back button
        inv.setItem(49, createItem(Material.ARROW, msg("gui.back"), msg("gui.back-lore")));

        fillBorder(inv);

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), GUIType.MODULE_SETTINGS);
    }

    /**
     * Opens the general settings submenu.
     */
    public void openGeneralSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, generalTitle());
        var cfg = plugin.getConfig();

        inv.setItem(4, createItem(Material.COMPARATOR, msg("gui.gen-header"), msg("gui.gen-header-lore")));

        // Slot 19: Default AFK Time
        int afkTime = cfg.getInt("default-afk-time", 300);
        ItemStack afkItem = createItem(Material.CLOCK, msg("gui.gen-afk-time"),
                msg("gui.current").replace("{value}", afkTime + "s (" + (afkTime/60) + " min)"),
                "", msg("gui.click-increase"), msg("gui.click-decrease"));
        afkItem.setAmount(Math.max(1, Math.min(64, afkTime / 30)));
        inv.setItem(19, afkItem);

        // Slot 21: Check Interval
        int interval = cfg.getInt("afk-check-interval-seconds", 5);
        ItemStack intervalItem = createItem(Material.REPEATER, msg("gui.gen-check-interval"),
                msg("gui.current").replace("{value}", interval + "s"),
                "", msg("gui.click-increase"), msg("gui.click-decrease"));
        intervalItem.setAmount(Math.max(1, Math.min(64, interval)));
        inv.setItem(21, intervalItem);

        // Slot 23: Max Voluntary AFK Time
        int volTime = cfg.getInt("max-voluntary-afk-time-seconds", 600);
        ItemStack volItem = createItem(Material.HOPPER, msg("gui.gen-voluntary-time"),
                msg("gui.current").replace("{value}", volTime + "s (" + (volTime/60) + " min)"),
                "", msg("gui.click-increase"), msg("gui.click-decrease"));
        volItem.setAmount(Math.max(1, Math.min(64, volTime / 60)));
        inv.setItem(23, volItem);

        // Slot 37: Block Pickup toggle
        boolean blockPickup = cfg.getBoolean("block-pickup-while-afk", true);
        inv.setItem(37, createToggle(msg("gui.gen-block-pickup"), blockPickup,
                lore("gui.gen-block-pickup-lore")));

        // Slot 39: Broadcast AFK toggle
        boolean broadcast = cfg.getBoolean("broadcast-afk-state-changes", true);
        inv.setItem(39, createToggle(msg("gui.gen-broadcast"), broadcast,
                lore("gui.gen-broadcast-lore")));

        inv.setItem(49, createItem(Material.ARROW, msg("gui.back"), msg("gui.back-lore")));
        fillBorder(inv);
        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), GUIType.GENERAL_SETTINGS);
    }

    /**
     * Opens the zone settings submenu.
     */
    public void openZoneSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, zoneTitle());
        var cfg = plugin.getConfig();

        inv.setItem(4, createItem(Material.GRASS_BLOCK, msg("gui.zone-header"), msg("gui.zone-header-lore")));

        // Slot 19: Zone enabled toggle
        boolean zoneEnabled = cfg.getBoolean("zone-management.enabled", false);
        inv.setItem(19, createToggle(msg("gui.zone-enabled"), zoneEnabled,
                lore("gui.zone-enabled-lore")));

        // Slot 21: Require WorldGuard toggle
        boolean reqWG = cfg.getBoolean("zone-management.require-worldguard", true);
        inv.setItem(21, createToggle(msg("gui.zone-worldguard"), reqWG,
                lore("gui.zone-worldguard-lore")));

        // Slot 23: Default AFK Allowed toggle
        boolean afkAllowed = cfg.getBoolean("zone-management.default-afk-allowed", true);
        inv.setItem(23, createToggle(msg("gui.zone-afk-allowed"), afkAllowed,
                lore("gui.zone-afk-allowed-lore")));

        // Slot 25: Default Timeout
        int timeout = cfg.getInt("zone-management.default-timeout-seconds", 300);
        ItemStack timeoutItem = createItem(Material.CLOCK, msg("gui.zone-timeout"),
                msg("gui.current").replace("{value}", timeout + "s (" + (timeout/60) + " min)"),
                "", msg("gui.click-increase"), msg("gui.click-decrease"));
        timeoutItem.setAmount(Math.max(1, Math.min(64, timeout / 60)));
        inv.setItem(25, timeoutItem);

        // Slot 37: Region Inheritance toggle
        boolean inheritance = cfg.getBoolean("zone-management.allow-region-inheritance", true);
        inv.setItem(37, createToggle(msg("gui.zone-inheritance"), inheritance,
                lore("gui.zone-inheritance-lore")));

        inv.setItem(49, createItem(Material.ARROW, msg("gui.back"), msg("gui.back-lore")));
        fillBorder(inv);
        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), GUIType.ZONE_SETTINGS);
    }

    /**
     * Opens the reward settings submenu.
     */
    public void openRewardSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, rewardTitle());
        var cfg = plugin.getConfig();

        inv.setItem(4, createItem(Material.DIAMOND, msg("gui.reward-header"), msg("gui.reward-header-lore")));

        // Slot 19: Reward enabled toggle
        boolean enabled = cfg.getBoolean("reward-system.enabled", false);
        inv.setItem(19, createToggle(msg("gui.reward-enabled"), enabled,
                lore("gui.reward-enabled-lore")));

        // Slot 21: Require Vault toggle
        boolean reqVault = cfg.getBoolean("reward-system.require-vault", false);
        inv.setItem(21, createToggle(msg("gui.reward-vault"), reqVault,
                lore("gui.reward-vault-lore")));

        // Slot 23: Max Daily Rewards
        int maxDaily = cfg.getInt("reward-system.max-daily-rewards", 144);
        ItemStack maxItem = createItem(Material.CHEST, msg("gui.reward-max-daily"),
                msg("gui.current").replace("{value}", String.valueOf(maxDaily)),
                "", msg("gui.click-increase"), msg("gui.click-decrease"));
        maxItem.setAmount(Math.max(1, Math.min(64, maxDaily / 10)));
        inv.setItem(23, maxItem);

        // Slot 25: Required Active Time
        int activeTime = cfg.getInt("reward-system.require-active-time-minutes", 30);
        ItemStack activeItem = createItem(Material.EXPERIENCE_BOTTLE, msg("gui.reward-active-time"),
                msg("gui.current").replace("{value}", activeTime + " min"),
                "", msg("gui.click-increase"), msg("gui.click-decrease"));
        activeItem.setAmount(Math.max(1, Math.min(64, activeTime / 5)));
        inv.setItem(25, activeItem);

        // Slot 37: IP-based Limits toggle
        boolean ipLimits = cfg.getBoolean("reward-system.ip-based-limits", true);
        inv.setItem(37, createToggle(msg("gui.reward-ip-limits"), ipLimits,
                lore("gui.reward-ip-limits-lore")));

        inv.setItem(49, createItem(Material.ARROW, msg("gui.back"), msg("gui.back-lore")));
        fillBorder(inv);
        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), GUIType.REWARD_SETTINGS);
    }

    /**
     * Opens the language selector submenu.
     */
    public void openLanguageSelector(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, languageTitle());
        String currentLang = plugin.getConfig().getString("internationalization.default-language", "en");

        inv.setItem(4, createItem(Material.WRITABLE_BOOK, msg("gui.lang-header"),
                msg("gui.lang-header-lore"),
                msg("gui.current").replace("{value}", currentLang)));

        int[] langSlots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30};
        for (int i = 0; i < LANGUAGES.size() && i < langSlots.length; i++) {
            String[] lang = LANGUAGES.get(i);
            String code = lang[0];
            String name = lang[1];
            boolean isActive = code.equals(currentLang);
            Material mat = isActive ? Material.ENCHANTED_BOOK : Material.BOOK;
            inv.setItem(langSlots[i], createItem(mat, (isActive ? "&a✔ " : "&7") + name,
                    msg("gui.lang-code").replace("{code}", code),
                    isActive ? msg("gui.currently-active") : msg("gui.click-to-select")));
        }

        inv.setItem(49, createItem(Material.ARROW, msg("gui.back"), msg("gui.back-lore")));
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
            String status = enabled ? msg("gui.status-enabled") : msg("gui.status-disabled");
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

    /**
     * Splits a localized message on the pipe character for multi-line lore values.
     */
    private String[] lore(String key) {
        return msg(key).split("\\|");
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
            return createItem(Material.CLOCK, msg("gui.perf-title"), msg("gui.perf-unavailable"));
        }

        PerformanceOptimizer.PerformanceStats stats = optimizer.getPerformanceStats();
        double avgMs = stats.getAverageExecutionTime();
        long memoryMB = stats.getMemoryUsage() / (1024 * 1024);

        return createItem(Material.CLOCK, msg("gui.perf-title"),
                msg("gui.perf-tps").replace("{value}", String.format("%.1f", stats.getTps())),
                msg("gui.perf-exec").replace("{value}", String.format("%.2f", avgMs)),
                msg("gui.perf-memory").replace("{value}", String.valueOf(memoryMB)),
                msg("gui.perf-cache").replace("{value}", String.valueOf(stats.getCacheSize())),
                msg("gui.perf-ops").replace("{value}", String.valueOf(stats.getTotalOperations())),
                msg("gui.perf-high").replace("{value}", String.valueOf(stats.getHighActivityPlayers())),
                msg("gui.perf-low").replace("{value}", String.valueOf(stats.getLowActivityPlayers())));
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

        return createItem(Material.BOOK, msg("gui.profile-title"),
                msg("gui.current").replace("{value}", profileColor + capitalize(profile)),
                "",
                msg("gui.profile-conservative"),
                msg("gui.profile-balanced"),
                msg("gui.profile-aggressive"),
                "",
                msg("gui.click-cycle"));
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
        String status = enabled ? msg("gui.status-enabled") : msg("gui.status-disabled");

        List<String> lore = new ArrayList<>();
        lore.add(color(msg("gui.status-label") + status));
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

    /**
     * Appends additional lore lines to an existing item.
     * Used for items that combine msg() values with lore() split values.
     */
    private void addLore(ItemStack item, String... extraLines) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();
        for (String line : extraLines) {
            lore.add(color(line));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
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
