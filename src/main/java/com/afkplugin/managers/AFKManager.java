package com.afkplugin.managers;

import com.afkplugin.AFKPlugin;
import org.bukkit.entity.Player;
import java.util.UUID;

public class AFKManager {
    private AFKPlugin plugin;

    public AFKManager(AFKPlugin plugin) {
        this.plugin = plugin;
    }

    public void updateActivity(Player player) {
        // Update last activity timestamp
    }

    public void saveAFKStatus(UUID playerId, boolean status) {
        // Save AFK status
    }
}
