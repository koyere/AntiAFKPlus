package me.koyere.antiafkplus.command;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AFKCommand implements CommandExecutor {

    private final AntiAFKPlus plugin;

    public AFKCommand(AntiAFKPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Subcomando: /afk list
        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            String permission = plugin.getConfigManager().getListCommandPermission(); // configurable
            if (!sender.hasPermission(permission)) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            int afkCount = 0;
            sender.sendMessage(plugin.getConfigManager().getMessage("afk-list-header"));

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (plugin.getAfkManager().isAFK(online)) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("afk-list-format")
                            .replace("{player}", online.getName()));
                    afkCount++;
                }
            }

            if (afkCount == 0) {
                sender.sendMessage(plugin.getConfigManager().getMessage("afk-list-empty"));
            }

            return true;
        }

        // Comando principal: /afk
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        boolean isNowAfk = plugin.getAfkManager().toggleManualAFK(player);

        if (isNowAfk) {
            String message = plugin.getConfigManager().getMessageAfkEnter().replace("{player}", player.getName());
            Bukkit.getServer().broadcastMessage(message);
            player.sendTitle(ChatColor.YELLOW + "You are now AFK", ChatColor.GRAY + "Move to return", 10, 70, 20);
        } else {
            String message = plugin.getConfigManager().getMessageAfkExit().replace("{player}", player.getName());
            Bukkit.getServer().broadcastMessage(message);
            player.sendTitle(ChatColor.GREEN + "You are no longer AFK", ChatColor.GRAY + "Welcome back!", 10, 70, 20);
        }

        return true;
    }
}
