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
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        boolean isNowAfk = plugin.getAfkManager().toggleManualAFK(player);

        if (isNowAfk) {
            String message = ChatColor.YELLOW + player.getName() + " is now AFK (manually).";
            Bukkit.getServer().broadcastMessage(message);

            // Optional: show a title
            player.sendTitle(ChatColor.YELLOW + "You are now AFK", ChatColor.GRAY + "Move to return", 10, 70, 20);
        } else {
            String message = ChatColor.GREEN + player.getName() + " is no longer AFK.";
            Bukkit.getServer().broadcastMessage(message);

            // Optional: show a title
            player.sendTitle(ChatColor.GREEN + "You are no longer AFK", ChatColor.GRAY + "Welcome back!", 10, 70, 20);
        }

        return true;
    }
}
