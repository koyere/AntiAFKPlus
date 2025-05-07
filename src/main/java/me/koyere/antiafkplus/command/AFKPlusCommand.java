package me.koyere.antiafkplus.command;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;

/**
 * Handles the /afkplus command.
 * Currently supports: /afkplus reload
 */
public class AFKPlusCommand implements CommandExecutor {

    private final AntiAFKPlus plugin;

    public AFKPlusCommand(AntiAFKPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Permission check
        if (!sender.hasPermission("antiafkplus.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        // Handle reload
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.getConfigManager().loadConfig();
            plugin.getConfigManager().loadMessages(); // Added: reload messages.yml
            sender.sendMessage(ChatColor.GREEN + "AntiAFKPlus configuration and messages reloaded.");
            return true;
        }

        // Default usage message
        sender.sendMessage(ChatColor.YELLOW + "Usage: /afkplus reload");
        return true;
    }
}
