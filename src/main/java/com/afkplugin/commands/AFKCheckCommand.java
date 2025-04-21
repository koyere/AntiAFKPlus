package com.afkplugin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import com.afkplugin.AFKPlugin;

public class AFKCheckCommand implements CommandExecutor {
    private AFKPlugin plugin;

    public AFKCheckCommand(AFKPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(plugin.getMessages().getString("afk-message"));
        return true;
    }
}
