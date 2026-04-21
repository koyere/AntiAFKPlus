// AFKPlusCommand.java - English comments
package me.koyere.antiafkplus.command;

import java.util.ArrayList;
import java.util.List; // For type reference
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.config.ConfigManager;

/**
 * Handles the /afkplus command, primarily for administrative tasks like reloading configuration.
 */
public class AFKPlusCommand implements CommandExecutor, TabCompleter { // Implement TabCompleter

    private final AntiAFKPlus plugin;
    private final ConfigManager configManager; // Store ConfigManager instance

    /**
     * Constructor for AFKPlusCommand.
     * @param plugin The main plugin instance.
     */
    public AFKPlusCommand(AntiAFKPlus plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager(); // Get ConfigManager from plugin
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("antiafkplus.reload")) {
                String noPermMsg = this.configManager.getMessage("no-permission", "&cYou do not have permission to use this command.");
                sender.sendMessage(noPermMsg);
                return true;
            }

            this.configManager.reloadConfiguration();

            String reloadedMsg = this.configManager.getMessage("config-reloaded", "&aAntiAFKPlus configuration and messages have been reloaded.");
            sender.sendMessage(reloadedMsg);
            plugin.getLogger().info("Configuration and messages reloaded by " + sender.getName() + ".");
            return true;
        }

        // Subcommand: /afkplus gui — opens the in-game settings GUI
        if (args.length == 1 && args[0].equalsIgnoreCase("gui")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(this.configManager.getMessage("player-only-command", "&cThis command can only be used by players."));
                return true;
            }
            if (!player.hasPermission("antiafkplus.reload")) {
                player.sendMessage(this.configManager.getMessage("no-permission", "&cYou do not have permission to use this command."));
                return true;
            }
            if (plugin.getGUIManager() != null) {
                plugin.getGUIManager().openMainMenu(player);
            }
            return true;
        }

        // Subcommand: /afkplus status — shows plugin status and analytics
        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            if (!sender.hasPermission("antiafkplus.stats")) {
                sender.sendMessage(this.configManager.getMessage("no-permission", "&cNo permission."));
                return true;
            }
            int afkCount = plugin.getAfkManager() != null ? plugin.getAfkManager().getAfkPlayerUUIDs().size() : 0;
            int onlineCount = org.bukkit.Bukkit.getOnlinePlayers().size();
            long uptimeMs = System.currentTimeMillis() - plugin.getStartupTime();
            long uptimeMin = uptimeMs / 60000;

            sender.sendMessage(color("&6=== AntiAFK+ Status ==="));
            sender.sendMessage(color("&7Version: &f" + plugin.getPluginVersion()));
            sender.sendMessage(color("&7Uptime: &f" + uptimeMin + " minutes"));
            sender.sendMessage(color("&7Online: &f" + onlineCount + " &7| AFK: &c" + afkCount));
            sender.sendMessage(color("&7Modules enabled: &f" + plugin.getModuleManager().getEnabledModuleCount() + "/" + plugin.getModuleManager().getModuleCount()));
            sender.sendMessage(color("&7Pattern detection: " + (plugin.getConfigManager().isPatternDetectionModuleEnabled() ? "&aActive" : "&cInactive")));
            sender.sendMessage(color("&7Credit system: " + (plugin.getCreditManager() != null && plugin.getCreditManager().isEnabled() ? "&aActive" : "&cInactive")));

            if (plugin.getPerformanceOptimizer() != null) {
                var stats = plugin.getPerformanceOptimizer().getPerformanceStats();
                sender.sendMessage(color("&7TPS: &f" + String.format("%.1f", stats.getTps()) + " &7| Memory: &f" + (stats.getMemoryUsage() / 1024 / 1024) + " MB"));
            }
            return true;
        }

        // Subcommand: /afkplus performance — detailed performance metrics
        if (args.length == 1 && args[0].equalsIgnoreCase("performance")) {
            if (!sender.hasPermission("antiafkplus.stats")) {
                sender.sendMessage(this.configManager.getMessage("no-permission", "&cNo permission."));
                return true;
            }
            if (plugin.getPerformanceOptimizer() == null) {
                sender.sendMessage(color("&cPerformance optimizer is not available."));
                return true;
            }
            var stats = plugin.getPerformanceOptimizer().getPerformanceStats();
            sender.sendMessage(color("&6=== AntiAFK+ Performance ==="));
            sender.sendMessage(color("&7Server TPS: &f" + String.format("%.1f", stats.getTps())));
            sender.sendMessage(color("&7Avg execution: &f" + String.format("%.3f", stats.getAverageExecutionTime()) + " ms"));
            sender.sendMessage(color("&7Total operations: &f" + stats.getTotalOperations()));
            sender.sendMessage(color("&7Memory usage: &f" + (stats.getMemoryUsage() / 1024 / 1024) + " MB"));
            sender.sendMessage(color("&7Cache entries: &f" + stats.getCacheSize()));
            sender.sendMessage(color("&7Components tracked: &f" + stats.getComponentCount()));
            sender.sendMessage(color("&7High activity players: &a" + stats.getHighActivityPlayers()));
            sender.sendMessage(color("&7Low activity players: &c" + stats.getLowActivityPlayers()));
            return true;
        }

        // Subcommand: /afkplus event credits <multiplier> <duration_minutes>
        if (args.length >= 1 && args[0].equalsIgnoreCase("event")) {
            if (!sender.hasPermission("antiafkplus.reload")) {
                sender.sendMessage(this.configManager.getMessage("no-permission", "&cNo permission."));
                return true;
            }
            if (args.length < 4 || !args[1].equalsIgnoreCase("credits")) {
                sender.sendMessage(color("&cUsage: /afkplus event credits <multiplier> <duration_minutes>"));
                sender.sendMessage(color("&7Example: /afkplus event credits 2 60"));
                return true;
            }
            double multiplier;
            int durationMinutes;
            try {
                multiplier = Double.parseDouble(args[2]);
                durationMinutes = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(color("&cInvalid number format."));
                return true;
            }
            if (multiplier <= 0 || multiplier > 10) {
                sender.sendMessage(color("&cMultiplier must be between 0.1 and 10."));
                return true;
            }
            if (durationMinutes <= 0 || durationMinutes > 1440) {
                sender.sendMessage(color("&cDuration must be between 1 and 1440 minutes."));
                return true;
            }

            // Store the event in config temporarily
            plugin.getConfig().set("credit-system.active-event.multiplier", multiplier);
            plugin.getConfig().set("credit-system.active-event.expires-at", System.currentTimeMillis() + (durationMinutes * 60000L));
            plugin.saveConfig();

            String msg = "&a[AntiAFK+] &fCredit multiplier event started: &e" + multiplier + "x &ffor &e" + durationMinutes + " minutes!";
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                p.sendMessage(color(msg));
            }
            sender.sendMessage(color("&aEvent started successfully."));
            return true;
        }

        // If no arguments or unknown subcommand, show plugin version or help.
        // Using a generic message for now.
        String mainCommandResponse = this.configManager.getMessage("plugin-info",
                        "&e" + plugin.getDescription().getName() + " version " + plugin.getDescription().getVersion() + " by Koyere. Use /afkplus reload to reload.")
                .replace("{version}", plugin.getDescription().getVersion())
                .replace("{name}", plugin.getDescription().getName());
        sender.sendMessage(mainCommandResponse);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            if (sender.hasPermission("antiafkplus.reload")) {
                if ("reload".startsWith(input)) completions.add("reload");
                if ("gui".startsWith(input) && sender instanceof Player) completions.add("gui");
                if ("event".startsWith(input)) completions.add("event");
            }
            if (sender.hasPermission("antiafkplus.stats")) {
                if ("status".startsWith(input)) completions.add("status");
                if ("performance".startsWith(input)) completions.add("performance");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("event")) {
            if ("credits".startsWith(args[1].toLowerCase())) completions.add("credits");
        }
        return completions.stream().sorted().collect(Collectors.toList());
    }

    @SuppressWarnings("deprecation")
    private String color(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}
