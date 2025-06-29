// AFKPlusCommand.java - English comments
package me.koyere.antiafkplus.command;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.config.ConfigManager; // For type reference
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter; // For tab completion
// import org.bukkit.ChatColor; // Not directly needed if using ConfigManager for messages

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
                // Using a generic no-permission message from ConfigManager if available,
                // otherwise, a hardcoded one.
                String noPermMsg = this.configManager.getMessage("no-permission", "&cYou do not have permission to use this command.");
                sender.sendMessage(noPermMsg);
                return true;
            }

            // Reload configuration files using the proper reload method
            this.configManager.reloadConfiguration();

            // Send confirmation message, preferably from messages.yml
            String reloadedMsg = this.configManager.getMessage("config-reloaded", "&aAntiAFKPlus configuration and messages have been reloaded.");
            sender.sendMessage(reloadedMsg);
            plugin.getLogger().info("Configuration and messages reloaded by " + sender.getName() + ".");
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
            if ("reload".startsWith(args[0].toLowerCase())) {
                if (sender.hasPermission("antiafkplus.reload")) {
                    completions.add("reload");
                }
            }
        }
        return completions.stream().sorted().collect(Collectors.toList()); // Sort completions alphabetically
    }
}