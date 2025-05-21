package me.koyere.antiafkplus.afk;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.config.ConfigManager; // For direct access if needed
import me.koyere.antiafkplus.afk.AFKManager;      // For direct access if needed
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority; // Good to specify if needed
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent; // Modern event
// PlayerPickupItemEvent is deprecated in newer versions but might be needed for wider compatibility
// However, EntityPickupItemEvent covers players too.
// For simplicity and modern practice, EntityPickupItemEvent is often sufficient.
// If you are supporting very old versions, you might keep both.
// org.bukkit.event.player.PlayerPickupItemEvent;

/**
 * Prevents players from picking up items while they are AFK,
 * if the feature is enabled in the plugin's configuration.
 */
public class ItemPickupBlocker implements Listener {

    private final AntiAFKPlus plugin; // Keep for getLogger, or pass ConfigManager/AFKManager directly
    private final ConfigManager configManager;
    private final AFKManager afkManager;

    /**
     * Constructor for ItemPickupBlocker.
     * @param plugin The main plugin instance.
     */
    public ItemPickupBlocker(AntiAFKPlus plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager(); // Get and store ConfigManager
        this.afkManager = plugin.getAfkManager();       // Get and store AFKManager
    }

    /**
     * Handles the EntityPickupItemEvent, which fires when any entity (including players)
     * attempts to pick up an item.
     * @param event The EntityPickupItemEvent.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true) // High priority to act before other plugins if necessary
    public void onEntityPickupItem(EntityPickupItemEvent event) { // Renamed method for clarity
        if (!(event.getEntity() instanceof Player)) {
            // Only interested if the entity picking up the item is a Player.
            return;
        }

        Player player = (Player) event.getEntity();
        if (shouldBlockPickupForPlayer(player)) { // Renamed method for clarity
            event.setCancelled(true);
            if (this.configManager.isDebugEnabled()) {
                this.plugin.getLogger().info("[DEBUG_ItemPickup] Blocked item pickup for AFK player: " + player.getName() + " (Item: " + event.getItem().getItemStack().getType() + ")");
            }
        }
    }

    /*
    // PlayerPickupItemEvent is often deprecated in favor of EntityPickupItemEvent.
    // If your target server versions widely use EntityPickupItemEvent for players,
    // this handler might be redundant or could even cause double processing if not careful.
    // For modern plugins (e.g., 1.12+), EntityPickupItemEvent is generally preferred.
    // I'm commenting it out, assuming EntityPickupItemEvent is sufficient.
    // If you need to support older versions where EntityPickupItemEvent might not cover players
    // or behave differently, you might re-enable it.

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPickupItem(org.bukkit.event.player.PlayerPickupItemEvent event) { // Fully qualified to distinguish if uncommented
        Player player = event.getPlayer();
        if (shouldBlockPickupForPlayer(player)) {
            event.setCancelled(true);
            if (this.configManager.isDebugEnabled()) {
                this.plugin.getLogger().info("[DEBUG_ItemPickup] Blocked item pickup (PlayerPickupItemEvent) for AFK player: " + player.getName());
            }
        }
    }
    */

    /**
     * Determines if item pickup should be blocked for the given player.
     * Pickup is blocked if the feature is enabled in the config and the player is AFK.
     * @param player The player to check.
     * @return True if item pickup should be blocked, false otherwise.
     */
    private boolean shouldBlockPickupForPlayer(Player player) {
        // Check bypass permission first
        if (player.hasPermission("antiafkplus.bypass.itempickup")) {
            return false;
        }
        return this.configManager.isBlockPickupWhileAFK() && this.afkManager.isAFK(player);
    }
}