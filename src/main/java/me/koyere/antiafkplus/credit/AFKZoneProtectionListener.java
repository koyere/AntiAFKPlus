package me.koyere.antiafkplus.credit;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Protección básica en la zona AFK del sistema de créditos.
 * - Cancela daño recibido por jugadores con bandera inAfkZone (si enabled en config)
 * - Cancela PVP si el atacante o el objetivo está marcado
 * - Opcionalmente reduce spawns cercanos a la ubicación de la zona AFK
 */
public class AFKZoneProtectionListener implements Listener {
    private final AntiAFKPlus plugin;
    private final CreditManager creditManager;
    private final java.util.Map<java.util.UUID, Long> pvpNotifyCooldown = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.UUID, Long> damageNotifyCooldown = new java.util.concurrent.ConcurrentHashMap<>();

    public AFKZoneProtectionListener(AntiAFKPlus plugin, CreditManager creditManager) {
        this.plugin = plugin;
        this.creditManager = creditManager;
    }

    private boolean protectDamage() {
        return plugin.getConfig().getBoolean("credit-system.afk-zone.protection.prevent-damage", false);
    }

    private boolean protectPvp() {
        return plugin.getConfig().getBoolean("credit-system.afk-zone.protection.prevent-pvp", false);
    }

    private boolean preventMobSpawning() {
        return plugin.getConfig().getBoolean("credit-system.afk-zone.protection.prevent-mob-spawning", false);
    }

    private boolean isInAfkZone(Player p) {
        return creditManager != null && creditManager.isInAfkZone(p);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!protectDamage()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (isInAfkZone(player)) {
            event.setCancelled(true);
            long now = System.currentTimeMillis();
            long last = damageNotifyCooldown.getOrDefault(player.getUniqueId(), 0L);
            if (now - last > 2000) { // 2s cooldown
                String msg = plugin.getConfigManager().getMessage("credit-system.protection.damage-prevented", "&eAFK zone: damage prevented.");
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', msg));
                damageNotifyCooldown.put(player.getUniqueId(), now);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!protectPvp()) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = null;
        if (event.getDamager() instanceof Player p) attacker = p;
        // Si cualquiera está en zona AFK -> cancelar
        if ((attacker != null && isInAfkZone(attacker)) || isInAfkZone(victim)) {
            event.setCancelled(true);
            // Notificar al atacante con cooldown anti-spam (2s)
            if (attacker != null && attacker.isOnline()) {
                long now = System.currentTimeMillis();
                long last = pvpNotifyCooldown.getOrDefault(attacker.getUniqueId(), 0L);
                if (now - last > 2000) {
                    String msg = plugin.getConfigManager().getMessage("credit-system.protection.pvp-prevented", "&eAFK zone: PVP is disabled here.");
                    attacker.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', msg));
                    pvpNotifyCooldown.put(attacker.getUniqueId(), now);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!preventMobSpawning()) return;
        Location zone = null;
        if (event.getLocation() != null && event.getLocation().getWorld() != null) {
            // Usamos la ubicación de zona AFK por defecto
            zone = creditManager != null && !plugin.getServer().getOnlinePlayers().isEmpty() ?
                    creditManager.getAFKZoneLocation(plugin.getServer().getOnlinePlayers().iterator().next()) : null;
        }
        if (zone == null) return;
        if (!event.getLocation().getWorld().equals(zone.getWorld())) return;
        double distSq = event.getLocation().distanceSquared(zone);
        // Radio fijo ligero para no añadir config nueva
        if (distSq <= (10 * 10)) {
            event.setCancelled(true);
        }
    }
}
