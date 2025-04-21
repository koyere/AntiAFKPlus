package com.afkplugin.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;
import com.afkplugin.AFKPlugin;

public class PlayerListener implements Listener {
    private AFKPlugin plugin;

    public PlayerListener(AFKPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        AFKPlugin.getInstance().getAfkManager().updateActivity(player);
    }
}
