package me.koyere.antiafkplus.credit;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.events.PlayerAFKKickEvent;
import me.koyere.antiafkplus.events.PlayerAFKStateChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Listener del sistema de créditos.
 * - Intercepta el kick/teleport AFK para consumir créditos si hay saldo
 * - Detiene el consumo cuando el jugador vuelve a ACTIVO
 */
public class CreditListener implements Listener {

    private final AntiAFKPlus plugin;
    private final CreditManager creditManager;

    public CreditListener(AntiAFKPlus plugin, CreditManager creditManager) {
        this.plugin = plugin;
        this.creditManager = creditManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAfkKick(PlayerAFKKickEvent event) {
        if (!creditManager.isEnabled()) return;
        var player = event.getPlayer();

        // Si hay créditos, cancelar la acción y comenzar consumo por-minuto
        if (creditManager.getData(player.getUniqueId()).getBalanceMinutes() > 0) {
            event.setCancelled(true);
            creditManager.beginConsumeOnAfk(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAfkStateChange(PlayerAFKStateChangeEvent event) {
        if (!creditManager.isEnabled()) return;
        if (event.getToState() == PlayerAFKStateChangeEvent.AFKState.ACTIVE) {
            // Detener consumo si existía
            creditManager.stopConsume(event.getPlayer());

            // Auto-return: si el jugador está en zona AFK y se detecta actividad, volver tras el delay
            var player = event.getPlayer();
            boolean inZone = creditManager.isInAfkZone(player);
            boolean auto = plugin.getConfig().getBoolean("credit-system.return-command.auto-return.on-activity", false);
            int delaySec = plugin.getConfig().getInt("credit-system.return-command.auto-return.activity-delay-seconds", 30);
            if (inZone && auto) {
                long delayTicks = Math.max(0, delaySec) * 20L;
                // No hay API directa runTaskLaterForEntity; programamos diferido global y luego encajamos entidad
                plugin.getPlatformScheduler().runTaskLater(() -> {
                    if (creditManager.isInAfkZone(player)) {
                        plugin.getPlatformScheduler().runTaskForEntity(player, () -> {
                            if (creditManager.isInAfkZone(player)) {
                                creditManager.returnFromAFKZone(player);
                            }
                        });
                    }
                }, delayTicks);
            }
        }
    }
}
