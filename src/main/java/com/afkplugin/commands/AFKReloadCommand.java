package com.afkplugin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import com.afkplugin.AFKPlugin;

public class AFKReloadCommand implements CommandExecutor {
    private AFKPlugin plugin;

    public AFKReloadCommand(AFKPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.reloadConfig();
        plugin.getLogger().info("Configuration reloaded.");
        return true;
    }
}
