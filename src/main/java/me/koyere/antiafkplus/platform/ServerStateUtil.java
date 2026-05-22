package me.koyere.antiafkplus.platform;

import org.bukkit.Bukkit;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

/**
 * Utilidad centralizada para determinar si el servidor está pausado.
 * Paper y forks modernos exponen MinecraftServer#isPaused(); la detección
 * inicial se hace vía reflexión (una sola vez en el class init) y las
 * invocaciones posteriores usan un {@link MethodHandle} ya enlazado al
 * servidor, evitando el coste y los safepoints implícitos de
 * {@code Method.invoke()} en bucles de scheduler.
 *
 * Además, el resultado se cachea durante ~1 tick para que decenas de tareas
 * programadas que comparten el mismo tick no realicen comprobaciones
 * redundantes (en el spark profile esto se manifestaba como
 * {@code SafepointSynchronize::block} dominando el coste del plugin).
 *
 * Si la API no está disponible, siempre devuelve {@code false}.
 */
public final class ServerStateUtil {

    private static final MethodHandle IS_PAUSED_HANDLE;
    /**
     * Indica si la plataforma expone {@code MinecraftServer#isPaused()}.
     * Se publica como {@code public static final} para que los callers (por
     * ejemplo, {@code PlatformScheduler#wrapWithPauseGuard}) puedan
     * cortocircuitar sin overhead: el JIT trata el campo como una
     * constante y elimina la rama en compilación cuando es {@code false}
     * (Spigot/legacy).
     */
    public static final boolean PAUSE_SUPPORTED;

    /** Duración de la cache (~1 tick = 50 ms). */
    private static final long PAUSE_CACHE_NANOS = 50_000_000L;

    private static volatile long lastCheckNanos = 0L;
    private static volatile boolean lastResult = false;

    static {
        MethodHandle pauseHandle = null;
        boolean supported = false;

        try {
            Object craftServer = Bukkit.getServer();
            Method getServerMethod = craftServer.getClass().getDeclaredMethod("getServer");
            getServerMethod.setAccessible(true);
            Object serverInstance = getServerMethod.invoke(craftServer);
            Method isPausedMethod = serverInstance.getClass().getMethod("isPaused");
            isPausedMethod.setAccessible(true);

            // Enlazar el handle a la instancia del servidor para obtener un
            // call-site monomórfico sin parámetros, que el JIT puede inline.
            pauseHandle = MethodHandles.lookup()
                    .unreflect(isPausedMethod)
                    .bindTo(serverInstance);
            supported = true;
        } catch (Throwable ignored) {
            pauseHandle = null;
            supported = false;
        }

        IS_PAUSED_HANDLE = pauseHandle;
        PAUSE_SUPPORTED = supported;
    }

    private ServerStateUtil() {
    }

    /**
     * Devuelve {@code true} si el servidor está pausado (Paper / debug pause / autopause).
     * El resultado se cachea durante aproximadamente un tick para minimizar
     * llamadas repetidas dentro del mismo ciclo de scheduling.
     * En plataformas sin soporte o si ocurre algún error, devuelve {@code false}.
     */
    public static boolean isServerPaused() {
        if (!PAUSE_SUPPORTED || IS_PAUSED_HANDLE == null) {
            return false;
        }

        long now = System.nanoTime();
        if (now - lastCheckNanos < PAUSE_CACHE_NANOS) {
            return lastResult;
        }

        boolean result;
        try {
            result = (boolean) IS_PAUSED_HANDLE.invokeExact();
        } catch (Throwable ignored) {
            result = false;
        }

        lastResult = result;
        lastCheckNanos = now;
        return result;
    }
}
