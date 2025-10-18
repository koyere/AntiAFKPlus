package me.koyere.antiafkplus.api;

import me.koyere.antiafkplus.events.PlayerAFKPatternDetectedEvent;
import me.koyere.antiafkplus.events.PlayerAFKStateChangeEvent;
import me.koyere.antiafkplus.events.PlayerAFKWarningEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Listener interno que reenvía los eventos Bukkit al API público.
 */
public final class APIEventListener implements Listener {

    private final AntiAFKPlusAPIImpl api;

    public APIEventListener(AntiAFKPlusAPIImpl api) {
        this.api = api;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAFKStateChange(PlayerAFKStateChangeEvent event) {
        if (api != null) {
            api.handleInternalAFKStateChange(event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAFKWarning(PlayerAFKWarningEvent event) {
        if (api != null) {
            api.handleInternalAFKWarning(event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPatternDetected(PlayerAFKPatternDetectedEvent event) {
        if (api != null) {
            api.handleInternalPatternDetection(event);
        }
    }
}
