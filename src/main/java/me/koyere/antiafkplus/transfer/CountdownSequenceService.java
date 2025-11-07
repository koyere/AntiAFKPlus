package me.koyere.antiafkplus.transfer;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.platform.PlatformScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CountdownSequenceService - Fase 2
 * Ejecuta una cuenta atrás por jugador (Title/Subtitle/Sound por segundo)
 * y al finalizar invoca un callback (p.ej. transferencia de servidor).
 *
 * - Folia-safe: el bucle es global y despacha acciones por entidad por tick.
 * - Cancelable: si el jugador deja de estar AFK o se desconecta.
 */
public class CountdownSequenceService {

    private final AntiAFKPlus plugin;
    private final Map<UUID, RunningCountdown> running = new ConcurrentHashMap<>();

    public CountdownSequenceService(AntiAFKPlus plugin) {
        this.plugin = plugin;
    }

    public boolean isRunning(Player player) {
        return player != null && running.containsKey(player.getUniqueId());
    }

    public void cancel(Player player) {
        if (player == null) return;
        RunningCountdown rc = running.remove(player.getUniqueId());
        if (rc != null && rc.task != null) {
            rc.task.cancel();
        }
    }

    public long getCountdownStart(UUID uuid) {
        RunningCountdown rc = uuid != null ? running.get(uuid) : null;
        return rc != null ? rc.startedAt : -1L;
    }

    public long getCountdownStart(Player player) {
        return player == null ? -1L : getCountdownStart(player.getUniqueId());
    }

    /**
     * Inicia una cuenta atrás de transferencia de servidor con configuración desde config.yml.
     * Llama al runnable onComplete en el tick final si no fue cancelado.
     */
    public void startServerTransferCountdown(Player player, Runnable onComplete) {
        if (player == null || !player.isOnline()) return;

        // Cargar configuración
        boolean enabled = plugin.getConfig().getBoolean("server-transfer.countdown.enabled", false);
        int seconds = Math.max(0, plugin.getConfig().getInt("server-transfer.countdown.seconds", 10));
        if (!enabled || seconds <= 0) {
            // Sin countdown; ejecutar directamente
            if (onComplete != null) onComplete.run();
            return;
        }

        String title = color(plugin.getConfig().getString("server-transfer.countdown.title", "&cYou are AFK"));
        String subtitleTemplate = color(plugin.getConfig().getString("server-transfer.countdown.subtitle", "&eMoving in {seconds}s"));
        boolean soundEnabled = plugin.getConfig().getBoolean("server-transfer.countdown.sound.enabled", true);
        String soundName = plugin.getConfig().getString("server-transfer.countdown.sound.name", "ENTITY_EXPERIENCE_ORB_PICKUP");
        float volume = (float) plugin.getConfig().getDouble("server-transfer.countdown.sound.volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("server-transfer.countdown.sound.pitch", 1.0);

        // Preparar estado
        UUID uuid = player.getUniqueId();
        cancel(player); // cancelar cualquier countdown previo
        RunningCountdown rc = new RunningCountdown(seconds);
        running.put(uuid, rc);

        PlatformScheduler.ScheduledTask timer = plugin.getPlatformScheduler().runTaskTimer(() -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) {
                cancelByUuid(uuid);
                return;
            }

            if (plugin.getAfkManager() == null || !plugin.getAfkManager().isAFK(p)) {
                cancelByUuid(uuid);
                return;
            }

            int secondsLeft = rc.remaining;

            // Despachar acciones por entidad
            plugin.getPlatformScheduler().runTaskForEntity(p, () -> {
                String subtitle = subtitleTemplate.replace("{seconds}", String.valueOf(secondsLeft));
                p.sendTitle(title, subtitle, 5, 15, 5);

                if (soundEnabled) {
                    try {
                        Sound s = Sound.valueOf(soundName);
                        p.playSound(p.getLocation(), s, volume, pitch);
                    } catch (IllegalArgumentException ignored) {
                        // Sonido inválido: no reproducir
                    }
                }
            });

            rc.remaining--;
            if (rc.remaining <= 0) {
                // Finalizar y ejecutar acción
                cancelByUuid(uuid);
                if (onComplete != null) {
                    // Ejecutar en el hilo de la entidad para seguridad
                    plugin.getPlatformScheduler().runTaskForEntity(p, onComplete);
                }
            }
        }, 0, 20); // cada 20 ticks = 1s

        rc.task = timer;
    }

    private void cancelByUuid(UUID uuid) {
        RunningCountdown rc = running.remove(uuid);
        if (rc != null && rc.task != null) rc.task.cancel();
    }

    private String color(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    private static class RunningCountdown {
        volatile int remaining;
        final long startedAt;
        PlatformScheduler.ScheduledTask task;

        private RunningCountdown(int seconds) {
            this.remaining = seconds;
            this.startedAt = System.currentTimeMillis();
        }
    }
}
