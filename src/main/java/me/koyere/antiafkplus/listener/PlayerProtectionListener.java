package me.koyere.antiafkplus.listener;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.afk.AFKManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener que maneja la protección de jugadores AFK.
 * Previene daño, movimiento forzado, y otras interacciones cuando un jugador está AFK.
 */
public class PlayerProtectionListener implements Listener {

    private final AntiAFKPlus plugin;
    private final AFKManager afkManager;
    
    // Cooldown para mensajes de notificación (evitar spam)
    private final Map<UUID, Long> messageCooldown = new ConcurrentHashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 3000; // 3 segundos

    public PlayerProtectionListener(AntiAFKPlus plugin) {
        this.plugin = plugin;
        this.afkManager = plugin.getAfkManager();
    }

    /**
     * Previene que jugadores AFK reciban daño si está habilitado en la configuración
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!afkManager.isAFK(player)) return;
        
        // Verificar si la invulnerabilidad está habilitada
        if (!plugin.getConfig().getBoolean("player-protection.invulnerability-enabled", true)) return;
        
        // Verificar tipos de daño bloqueados
        List<String> blockedTypes = plugin.getConfig().getStringList("player-protection.damage-types-blocked");
        String damageType = event.getCause().name();
        
        if (blockedTypes.contains(damageType)) {
            event.setCancelled(true);
            sendCooldownMessage(player, "protection-invulnerable", "&a[AntiAFK] You are protected from damage while AFK.");
        }
    }

    /**
     * Previene PvP contra jugadores AFK si está habilitado
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!afkManager.isAFK(victim)) return;
        
        // Verificar si la protección PvP está habilitada
        if (!plugin.getConfig().getBoolean("player-protection.prevent-pvp-invulnerability", true)) return;
        
        event.setCancelled(true);
        sendCooldownMessage(attacker, "protection-pvp-blocked", "&c[AntiAFK] You cannot attack AFK players.");
        sendCooldownMessage(victim, "protection-pvp-protected", "&a[AntiAFK] You are protected from PvP while AFK.");
    }

    /**
     * Previene movimiento forzado de jugadores AFK (empujones, knockback, etc.)
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!afkManager.isAFK(player)) return;
        
        // Verificar si la prevención de movimiento está habilitada
        if (!plugin.getConfig().getBoolean("player-protection.prevent-movement-while-afk", false)) return;
        
        // Solo cancelar si el movimiento no fue iniciado por el jugador
        // (esto previene empujones pero permite que el jugador se mueva para salir del AFK)
        if (isMovementForced(event)) {
            event.setCancelled(true);
            if (plugin.getConfig().getBoolean("player-protection.movement-restriction-message", true)) {
                sendCooldownMessage(player, "protection-movement-blocked", "&e[AntiAFK] Movement blocked while AFK.");
            }
        }
    }

    /**
     * Previene acceso al inventario de jugadores AFK
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!afkManager.isAFK(player)) return;
        
        if (plugin.getConfig().getBoolean("player-protection.block-inventory-access", true)) {
            event.setCancelled(true);
            sendCooldownMessage(player, "protection-inventory-blocked", "&e[AntiAFK] Inventory access blocked while AFK.");
        }
    }

    /**
     * Previene interacciones con bloques de jugadores AFK
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!afkManager.isAFK(player)) return;
        
        if (plugin.getConfig().getBoolean("player-protection.prevent-block-interaction", true)) {
            event.setCancelled(true);
            sendCooldownMessage(player, "protection-interaction-blocked", "&e[AntiAFK] Block interaction blocked while AFK.");
        }
    }

    /**
     * Controla la ejecución de comandos de jugadores AFK
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!afkManager.isAFK(player)) return;
        
        if (plugin.getConfig().getBoolean("player-protection.block-command-execution", false)) {
            String command = event.getMessage().toLowerCase().split(" ")[0];
            List<String> whitelist = plugin.getConfig().getStringList("player-protection.command-whitelist");
            
            // Verificar si el comando está en la whitelist
            boolean isWhitelisted = whitelist.stream().anyMatch(cmd -> 
                command.equals(cmd.toLowerCase()) || command.startsWith(cmd.toLowerCase() + " "));
            
            if (!isWhitelisted) {
                event.setCancelled(true);
                sendCooldownMessage(player, "protection-command-blocked", "&e[AntiAFK] Command execution blocked while AFK. Use /afk to return.");
            }
        }
    }

    /**
     * Determina si un movimiento fue forzado (por empujones, knockback, etc.)
     * vs movimiento iniciado por el jugador
     */
    private boolean isMovementForced(PlayerMoveEvent event) {
        // Si el jugador está en el suelo y el movimiento es muy pequeño, probablemente es forzado
        if (event.getFrom().distance(event.getTo()) < 0.1) {
            return false; // Movimiento muy pequeño, no cancelar
        }
        
        // Si el jugador está volando o nadando, es más probable que sea movimiento natural
        if (event.getPlayer().isFlying() || event.getPlayer().isSwimming()) {
            return false;
        }
        
        // Si hay una diferencia significativa en Y (caída/salto), probablemente es forzado
        double yDiff = Math.abs(event.getTo().getY() - event.getFrom().getY());
        if (yDiff > 0.5) {
            return true; // Movimiento vertical significativo, probablemente forzado
        }
        
        // Movimiento horizontal rápido puede ser knockback
        double horizontalDistance = Math.sqrt(
            Math.pow(event.getTo().getX() - event.getFrom().getX(), 2) +
            Math.pow(event.getTo().getZ() - event.getFrom().getZ(), 2)
        );
        
        return horizontalDistance > 1.0; // Movimiento horizontal rápido, probablemente forzado
    }

    /**
     * Envía un mensaje al jugador con cooldown para evitar spam
     */
    private void sendCooldownMessage(Player player, String messageKey, String defaultMessage) {
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastMessage = messageCooldown.get(playerId);
        
        if (lastMessage == null || (now - lastMessage) > MESSAGE_COOLDOWN_MS) {
            String message = plugin.getLocalizationManager().getMessage(player, messageKey);
            if (message == null || message.trim().isEmpty()) {
                message = defaultMessage;
            }
            // Aplicar colores al mensaje si es necesario
            if (message.contains("&")) {
                message = message.replace('&', '§');
            }
            player.sendMessage(message);
            messageCooldown.put(playerId, now);
        }
    }

    /**
     * Limpia el cooldown de mensajes cuando un jugador se desconecta
     */
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        messageCooldown.remove(event.getPlayer().getUniqueId());
    }
}