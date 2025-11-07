package me.koyere.antiafkplus.platform;

import org.bukkit.Bukkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Utilidad centralizada para determinar si el servidor está pausado.
 * Paper y forks modernos exponen MinecraftServer#isPaused(); la detección
 * se hace vía reflexión para mantener compatibilidad con otras plataformas.
 * Si la API no está disponible, siempre devuelve false.
 */
public final class ServerStateUtil {

    private static final Object MINECRAFT_SERVER;
    private static final Method IS_PAUSED_METHOD;
    private static final boolean PAUSE_SUPPORTED;

    static {
        Object serverInstance = null;
        Method pauseMethod = null;
        boolean supported = false;

        try {
            Object craftServer = Bukkit.getServer();
            Method getServerMethod = craftServer.getClass().getDeclaredMethod("getServer");
            getServerMethod.setAccessible(true);
            serverInstance = getServerMethod.invoke(craftServer);
            pauseMethod = serverInstance.getClass().getMethod("isPaused");
            supported = true;
        } catch (Exception ignored) {
            serverInstance = null;
            pauseMethod = null;
            supported = false;
        }

        MINECRAFT_SERVER = serverInstance;
        IS_PAUSED_METHOD = pauseMethod;
        PAUSE_SUPPORTED = supported;
    }

    private ServerStateUtil() {
    }

    /**
     * Devuelve true si el servidor está pausado (Paper / debug pause / autopause).
     * En plataformas sin soporte o si ocurre algún error reflexivo, devuelve false.
     */
    public static boolean isServerPaused() {
        if (!PAUSE_SUPPORTED || IS_PAUSED_METHOD == null || MINECRAFT_SERVER == null) {
            return false;
        }
        try {
            Object result = IS_PAUSED_METHOD.invoke(MINECRAFT_SERVER);
            return result instanceof Boolean && (Boolean) result;
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return false;
        }
    }
}
