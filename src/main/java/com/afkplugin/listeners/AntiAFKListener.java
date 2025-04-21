package com.afkplugin.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import com.afkplugin.AFKPlugin;

public class AntiAFKListener implements Listener {
    private AFKPlugin plugin;

    public AntiAFKListener(AFKPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        event.getPlayer().sendMessage(plugin.getMessages().getString("afk-message"));
    }
}
