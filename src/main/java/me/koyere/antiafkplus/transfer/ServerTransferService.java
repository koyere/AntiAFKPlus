package me.koyere.antiafkplus.transfer;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * ServerTransferService - Fase 1 (Backbone)
 *
 * Proporciona un método seguro para solicitar transferencia de servidor
 * mediante Plugin Messaging (BungeeCord/Velocity modo compatibilidad).
 *
 * Nota: En Fase 1 no se implementa countdown ni secuencias; solo salida inmediata
 * con fallback configurable. Totalmente Folia-safe si se invoca desde
 * PlatformScheduler (por entidad).
 */
public class ServerTransferService {

    private final AntiAFKPlus plugin;
    private final Logger logger;

    public static final String LEGACY_CHANNEL = "BungeeCord";        // Compatibilidad heredada
    public static final String NAMESPACE_CHANNEL = "bungeecord:main"; // Canal namespaced moderno

    public ServerTransferService(AntiAFKPlus plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * Intenta transferir al jugador al servidor objetivo. Devuelve true si el intento fue emitido.
     * Responsabilidad del proxy completar la acción.
     */
    public boolean transferPlayer(Player player, String targetServer) {
        if (player == null || !player.isOnline()) return false;

        // Validación básica de configuración
        boolean enabled = plugin.getConfig().getBoolean("server-transfer.enabled", false);
        if (!enabled) {
            logger.fine("Server transfer disabled in config.");
            return false;
        }

        if (targetServer == null || targetServer.trim().isEmpty()) {
            targetServer = plugin.getConfig().getString("server-transfer.target-server", "");
        }
        if (targetServer == null || targetServer.trim().isEmpty()) {
            logger.warning("Server transfer: target-server not configured.");
            return false;
        }

        // Canal preferido por config
        String mode = plugin.getConfig().getString("server-transfer.proxy-channel", "auto").toLowerCase();

        try {
            byte[] payload = buildConnectPayload(targetServer);

            // Asegurar registro de canales (idempotente)
            ensureChannelsRegistered();

            String chosen = chooseChannel(mode);
            if (chosen != null && Bukkit.getMessenger().isOutgoingChannelRegistered(plugin, chosen)) {
                player.sendPluginMessage(plugin, chosen, payload);
                return true;
            }

            // Si auto falló, intentar el otro canal disponible
            String alternate = LEGACY_CHANNEL.equals(chosen) ? NAMESPACE_CHANNEL : LEGACY_CHANNEL;
            if (alternate != null && Bukkit.getMessenger().isOutgoingChannelRegistered(plugin, alternate)) {
                player.sendPluginMessage(plugin, alternate, payload);
                return true;
            }

            // Reintentos configurables
            int attempts = Math.max(0, plugin.getConfig().getInt("server-transfer.retry-attempts", 0));
            int delay = Math.max(1, plugin.getConfig().getInt("server-transfer.retry-delay-ticks", 10));
            if (attempts > 0) {
                final boolean[] result = { false };
                for (int i = 0; i < attempts; i++) {
                    int run = i;
                    plugin.getPlatformScheduler().runTaskLater(() -> {
                        ensureChannelsRegistered();
                        String ch = chooseChannel(mode);
                        try {
                            if (ch != null && Bukkit.getMessenger().isOutgoingChannelRegistered(plugin, ch)) {
                                player.sendPluginMessage(plugin, ch, payload);
                                result[0] = true;
                            } else {
                                String alt = LEGACY_CHANNEL.equals(ch) ? NAMESPACE_CHANNEL : LEGACY_CHANNEL;
                                if (alt != null && Bukkit.getMessenger().isOutgoingChannelRegistered(plugin, alt)) {
                                    player.sendPluginMessage(plugin, alt, payload);
                                    result[0] = true;
                                }
                            }
                        } catch (Throwable ignore) {}
                    }, delay * (run + 1));
                }
                return result[0];
            }

            logger.warning("Server transfer: no outgoing plugin channel registered (BungeeCord/bungeecord:main).");
            return false;
        } catch (Throwable t) {
            logger.warning("Server transfer failed: " + t.getMessage());
            return false;
        }
    }

    private void ensureChannelsRegistered() {
        try {
            if (!Bukkit.getMessenger().isOutgoingChannelRegistered(plugin, LEGACY_CHANNEL)) {
                Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, LEGACY_CHANNEL);
            }
        } catch (Throwable ignored) {}
        try {
            if (!Bukkit.getMessenger().isOutgoingChannelRegistered(plugin, NAMESPACE_CHANNEL)) {
                Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, NAMESPACE_CHANNEL);
            }
        } catch (Throwable ignored) {}
    }

    private String chooseChannel(String mode) {
        switch (mode) {
            case "bungeecord":
            case "legacy":
                return LEGACY_CHANNEL;
            case "namespaced":
            case "modern":
                return NAMESPACE_CHANNEL;
            case "auto":
            default:
                // Preferir legacy por compatibilidad histórica
                if (Bukkit.getMessenger().isOutgoingChannelRegistered(plugin, LEGACY_CHANNEL)) {
                    return LEGACY_CHANNEL;
                }
                if (Bukkit.getMessenger().isOutgoingChannelRegistered(plugin, NAMESPACE_CHANNEL)) {
                    return NAMESPACE_CHANNEL;
                }
                return LEGACY_CHANNEL; // Valor por defecto si no hay ninguno aún
        }
    }

    /**
     * Construye el payload para el subcanal Connect (BungeeCord).
     * Formato: UTF-8 bytes de "Connect" + targetServer separados por longitud/orden.
     * Para simplicidad y compatibilidad se usa DataOutput manual en bytes.
     */
    private byte[] buildConnectPayload(String targetServer) throws Exception {
        // Estructura: [subcanal][server]
        java.io.ByteArrayOutputStream msg = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream out = new java.io.DataOutputStream(msg);
        out.writeUTF("Connect");
        out.writeUTF(targetServer);
        out.flush();
        return msg.toByteArray();
    }
}
