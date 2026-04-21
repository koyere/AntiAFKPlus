package me.koyere.antiafkplus.integrations;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.events.PlayerAFKStateChangeEvent;

/**
 * Optional DiscordSRV integration via reflection.
 * Sends AFK notifications to Discord channels.
 */
public class DiscordSRVIntegration implements Listener {

    private final AntiAFKPlus plugin;
    private final Logger logger;
    private boolean available = false;
    private Object discordSRVInstance;
    private Method getMainTextChannelMethod;

    public DiscordSRVIntegration(AntiAFKPlus plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        detectDiscordSRV();

        if (available) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            logger.info("DiscordSRV integration enabled.");
        }
    }

    private void detectDiscordSRV() {
        try {
            var discordPlugin = Bukkit.getPluginManager().getPlugin("DiscordSRV");
            if (discordPlugin == null || !discordPlugin.isEnabled()) return;

            Class<?> discordSRVClass = Class.forName("github.scarsz.discordsrv.DiscordSRV");
            Method getPlugin = discordSRVClass.getMethod("getPlugin");
            this.discordSRVInstance = getPlugin.invoke(null);
            this.getMainTextChannelMethod = discordSRVClass.getMethod("getMainTextChannel");
            this.available = true;
        } catch (ClassNotFoundException e) {
            // DiscordSRV not available
        } catch (Exception e) {
            logger.warning("Failed to initialize DiscordSRV integration: " + e.getMessage());
        }
    }

    public boolean isAvailable() {
        return available;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAFKStateChange(PlayerAFKStateChangeEvent event) {
        if (!available || !plugin.getConfig().getBoolean("integrations.discordsrv.send-afk-notifications", false)) {
            return;
        }

        Player player = event.getPlayer();
        String message;
        if (event.getToState().name().startsWith("AFK")) {
            message = "**" + player.getName() + "** is now AFK.";
        } else {
            message = "**" + player.getName() + "** is no longer AFK.";
        }

        sendToDiscord(message);
    }

    /**
     * Sends a message to the main Discord channel via DiscordSRV.
     */
    public void sendToDiscord(String message) {
        if (!available || discordSRVInstance == null) return;
        try {
            Object textChannel = getMainTextChannelMethod.invoke(discordSRVInstance);
            if (textChannel != null) {
                Method sendMessage = textChannel.getClass().getMethod("sendMessage", String.class);
                Object action = sendMessage.invoke(textChannel, message);
                // Queue the message (JDA pattern)
                Method queue = action.getClass().getMethod("queue");
                queue.invoke(action);
            }
        } catch (Exception e) {
            // Silently fail — Discord integration is optional
            if (plugin.isDebugEnabled()) {
                logger.warning("DiscordSRV message failed: " + e.getMessage());
            }
        }
    }
}
