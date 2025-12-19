package me.koyere.antiafkplus.credit;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.platform.PlatformScheduler;
import me.koyere.antiafkplus.api.data.CreditTransaction;
import me.koyere.antiafkplus.api.data.CreditTransactionType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import me.koyere.antiafkplus.credit.storage.CreditStorage;
import me.koyere.antiafkplus.credit.storage.FileCreditStorage;
import me.koyere.antiafkplus.credit.storage.SqlCreditStorage;

import java.time.Duration;
import me.koyere.antiafkplus.api.data.ActivityType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.ConfigurationSection;

/**
 * CreditManager gestiona el saldo de créditos de los jugadores, el earning periódico
 * y el consumo diferido para retrasar la acción AFK (kick/teleport).
 *
 * Fase 1: in-memory, sin persistencia; earning básico y consumo por-minuto cuando AFK.
 */
public class CreditManager {
    private final AntiAFKPlus plugin;
    private final Map<UUID, CreditData> credits = new ConcurrentHashMap<>();

    // Job global para earning (cada 60s)
    private PlatformScheduler.ScheduledTask earnTask;
    private PlatformScheduler.ScheduledTask decayTask;
    private PlatformScheduler.ScheduledTask saveTask;
    private CreditStorage storage;

    /**
     * Initializes the Credit Manager.
     * PROFESSIONAL FIX: Only starts tasks if the system is enabled, ensuring complete
     * isolation when disabled and preventing any interference with the AFK system.
     *
     * @param plugin The main plugin instance
     */
    public CreditManager(AntiAFKPlus plugin) {
        this.plugin = plugin;

        // Only initialize if enabled - ensures complete isolation when disabled
        if (isEnabled()) {
            initializeStorage();
            startEarningTask();
            startDecayTask();
            startSaveTask();
        }
    }

    public void shutdown() {
        if (earnTask != null && !earnTask.isCancelled()) {
            earnTask.cancel();
        }
        earnTask = null;
        // Cancelar tareas por jugador
        credits.values().forEach(CreditData::cancelConsumeTask);
        credits.clear();
        if (decayTask != null && !decayTask.isCancelled()) decayTask.cancel();
        decayTask = null;
        if (saveTask != null && !saveTask.isCancelled()) saveTask.cancel();
        saveTask = null;
        // Guardado final
        if (storage != null) storage.saveAll(credits);
        if (storage != null) storage.close();
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("credit-system.enabled", false) &&
               plugin.getConfig().getBoolean("modules.credit-system.enabled", false);
    }

    public CreditData getData(UUID uuid) {
        return credits.computeIfAbsent(uuid, CreditData::new);
    }

    // ====================== EARNING =======================

    private void startEarningTask() {
        // Corre cada 60s, Folia-safe
        long period = 20L * 60L;
        earnTask = plugin.getPlatformScheduler().runTaskTimer(this::tickEarning, period, period);
    }

    private void tickEarning() {
        if (!isEnabled()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline()) continue;
            if (player.hasPermission("antiafkplus.bypass")) continue;
            if (!player.hasPermission("antiafkplus.credit.earn")) continue;

            // Considerar como "activo" si no está AFK según AFKManager
            boolean isAfk = plugin.getAfkManager() != null && plugin.getAfkManager().isAFK(player);
            if (isAfk) continue;

            // Requisitos mínimos
            int minSession = plugin.getConfig().getInt("credit-system.earning-requirements.minimum-session-minutes", 5);
            double activityThreshold = plugin.getConfig().getDouble("credit-system.earning-requirements.activity-threshold", 0.3);

            // Medimos por heurística: tiempo en línea y score de actividad si disponible
            // (en esta fase, aproximamos a 1 minuto de tiempo activo por ciclo si no AFK)

            // Ratio y tope
            Ratio ratio = getRatioFor(player);
            long max = getMaxCreditsFor(player);

            // Acumular minutos activos en un contador simple por jugador
            CreditData data = getData(player.getUniqueId());
            // Usamos lastEarnedAt como marcador simple de progreso; aquí, por simplicidad, cada 5 minutos (minSession) sumamos 1 de crédito según ratio
            // Implementación mínima: sumar 1 crédito cada (ratio.active) minutos. Dado nuestro tick de 60s, acumulamos en metadata de sesión.
            // Para fase 1, simplificamos: cada tick (1 min) añadimos fracción y aplicamos cuando completamos ratio.
            addActiveMinuteProgress(player, data, ratio, max);
        }
    }

    private final Map<UUID, Integer> activeMinuteCounter = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> sessionMinuteCounter = new ConcurrentHashMap<>();

    // Cache para orden de prioridad de grupos (performance optimization)
    private final Map<String, List<String>> cachedPriorityOrders = new ConcurrentHashMap<>();

    private void addActiveMinuteProgress(Player player, CreditData data, Ratio ratio, long max) {
        UUID id = player.getUniqueId();
        int minutes = activeMinuteCounter.getOrDefault(id, 0) + 1;
        int session = sessionMinuteCounter.getOrDefault(id, 0) + 1;
        sessionMinuteCounter.put(id, session);

        // Requisitos mínimos de sesión y actividad
        int minSession = Math.max(0, plugin.getConfig().getInt("credit-system.earning-requirements.minimum-session-minutes", 5));
        if (session < minSession) {
            activeMinuteCounter.put(id, minutes); // seguimos contando progreso de ratio
            return;
        }
        double activityThreshold = Math.max(0.0, plugin.getConfig().getDouble("credit-system.earning-requirements.activity-threshold", 0.3));
        var afkMgr = plugin.getAfkManager();
        if (afkMgr != null) {
            var pdata = afkMgr.getPlayerActivityData(player);
            if (pdata != null) {
                double score = pdata.getActivityScore(); // 0..100
                if (score < activityThreshold * 100.0) {
                    activeMinuteCounter.put(id, minutes);
                    return;
                }
                // Tipos de actividad requeridos
                int requiredTypes = Math.max(0, plugin.getConfig().getInt("credit-system.earning-requirements.required-activity-types", 0));
                if (requiredTypes > 0) {
                    Map<ActivityType, Integer> counts = pdata.getActivityCounts(Duration.ofMinutes(5).toMillis());
                    int types = 0;
                    if (counts.getOrDefault(ActivityType.MOVEMENT, 0) > 0) types++;
                    if (counts.getOrDefault(ActivityType.HEAD_ROTATION, 0) > 0) types++;
                    if (counts.getOrDefault(ActivityType.JUMP, 0) > 0) types++;
                    if (counts.getOrDefault(ActivityType.COMMAND, 0) > 0) types++;
                    if (types < requiredTypes) {
                        activeMinuteCounter.put(id, minutes);
                        return;
                    }
                }
            }
        }
        // Si aún no alcanza el ratio de minutos activos para 1 crédito, guardar y salir
        if (minutes < ratio.activeMinutes) {
            activeMinuteCounter.put(id, minutes);
            return;
        }
        // Completa ciclo: otorgar creditMinutes
        activeMinuteCounter.put(id, 0);
        long grant = ratio.creditMinutes;
        // Bonus simple si reward-system habilitado e integración activada
        if (plugin.getConfig().getBoolean("modules.reward-system.enabled", false) &&
            plugin.getConfig().getBoolean("credit-system.integration.reward-system-bonus", true)) {
            grant += 1; // +1 minuto de bonus por ciclo de ratio
        }
        long newBalance = Math.min(data.getBalanceMinutes() + grant, max);
        long actuallyGranted = Math.max(0, newBalance - data.getBalanceMinutes());
        if (actuallyGranted > 0) {
            data.addMinutes(actuallyGranted);
            // Persist balance and registrar transacción si aplica
            if (storage != null) {
                storage.saveOne(player.getUniqueId(), data);
                if (storage.supportsHistory()) {
                    storage.recordTransaction(player.getUniqueId(), CreditTransactionType.EARN, actuallyGranted, data.getBalanceMinutes(), "earn", System.currentTimeMillis());
                }
            }
            // Notificación
            if (plugin.getConfig().getBoolean("credit-system.notifications.credit-earned", true)) {
                String msg = plugin.getConfigManager().getMessage("credit-system.earned", "&a+ {minutes}m credits");
                player.sendMessage(color(msg
                        .replace("{minutes}", String.valueOf(actuallyGranted))
                        .replace("{total}", String.valueOf(data.getBalanceMinutes()))));
            }
            // Fire API event
            if (plugin.getAPI() instanceof me.koyere.antiafkplus.api.AntiAFKPlusAPIImpl api) {
                api.fireCreditEarned(new me.koyere.antiafkplus.api.events.CreditEarnedEvent(
                        player, actuallyGranted, data.getBalanceMinutes(), java.time.Instant.now()
                ));
            }
        }
    }

    // ====================== CONSUMO =======================

    /**
     * Inicia consumo por-minuto cuando llega el evento de kick AFK y hay saldo suficiente.
     * Cancela el evento y programa decrementos de 1m/min. Al agotar, teleporta a zona AFK.
     */
    public boolean beginConsumeOnAfk(Player player) {
        if (!isEnabled()) return false;
        if (!player.hasPermission("antiafkplus.credit.use")) return false;
        CreditData data = getData(player.getUniqueId());
        if (data.getBalanceMinutes() <= 0) return false;
        if (data.isConsuming()) return true; // ya en consumo

        data.startConsuming(player.getLocation().clone());

        // Feedback inicio
        if (plugin.getConfig().getBoolean("credit-system.notifications.credit-consumed", true)) {
            String msg = plugin.getConfigManager().getMessage("credit-system.consuming-start", "&eUsing AFK credits...");
            player.sendMessage(color(msg));
        }

        // Programar tarea por-minuto
        long period = 20L * 60L; // 60s
        PlatformScheduler.ScheduledTask task = plugin.getPlatformScheduler().runTaskTimer(() -> {
            if (!player.isOnline()) {
                stopConsume(player);
                return;
            }
            // Si ya no está AFK, paramos consumo
            boolean isAfk = plugin.getAfkManager() != null && plugin.getAfkManager().isAFK(player);
            if (!isAfk) {
                stopConsume(player);
                return;
            }
            // Consumir 1 minuto
            if (data.consumeOneMinute()) {
                if (plugin.getConfig().getBoolean("credit-system.notifications.credit-consumed", true)) {
                    String msg = plugin.getConfigManager().getMessage("credit-system.consumed", "&c- 1m credits");
                    player.sendMessage(color(msg
                            .replace("{minutes}", "1")
                            .replace("{remaining}", String.valueOf(data.getBalanceMinutes()))));
                }
                // Guardar y registrar consumo
                if (storage != null) {
                    storage.saveOne(player.getUniqueId(), data);
                    if (storage.supportsHistory()) {
                        storage.recordTransaction(player.getUniqueId(), CreditTransactionType.CONSUME, 1, data.getBalanceMinutes(), "afk-consume", System.currentTimeMillis());
                    }
                }
                // Fire API event
                if (plugin.getAPI() instanceof me.koyere.antiafkplus.api.AntiAFKPlusAPIImpl api) {
                    api.fireCreditConsumed(new me.koyere.antiafkplus.api.events.CreditConsumedEvent(
                            player, 1, data.getBalanceMinutes(), java.time.Instant.now()
                    ));
                }

                // Low credits warning
                if (plugin.getConfig().getBoolean("credit-system.notifications.low-credits-warning", true)) {
                    long threshold = plugin.getConfig().getLong("credit-system.notifications.low-credits-threshold", 15);
                    if (data.getBalanceMinutes() <= threshold) {
                        if (!data.isLowCreditWarned() || data.getBalanceMinutes() % 5 == 0) { // limitar spam
                            String warn = plugin.getConfigManager().getMessage("credit-system.low-warning", "&eLow credits");
                            String msg = warn.replace("{remaining}", String.valueOf(data.getBalanceMinutes()));
                            plugin.getPlatformScheduler().runTaskForEntity(player, () -> player.sendMessage(color(msg)));
                            data.setLowCreditWarned(true);
                        }
                    } else {
                        data.setLowCreditWarned(false);
                    }
                }
                return;
            }
            // Saldo agotado → teletransportar a zona AFK y detener
            if (plugin.getConfig().getBoolean("credit-system.notifications.credit-exhausted", true)) {
                String msg = plugin.getConfigManager().getMessage("credit-system.exhausted", "&cCredits exhausted");
                player.sendMessage(color(msg));
            }
            stopConsume(player);
            teleportToAfkZone(player);
        }, period, period);

        data.setConsumeTask(task);
        return true;
    }

    private void startDecayTask() {
        // Lanza limpieza/expiración si está habilitado
        long hours = Math.max(1, plugin.getConfig().getLong("credit-system.credit-decay.cleanup-interval-hours", 24));
        long period = hours * 60L * 60L * 20L;
        decayTask = plugin.getPlatformScheduler().runTaskTimer(this::tickDecay, period, period);
    }

    private void tickDecay() {
        if (!isEnabled()) return;
        if (!plugin.getConfig().getBoolean("credit-system.credit-decay.enabled", false)) return;
        long expireDays = Math.max(1, plugin.getConfig().getLong("credit-system.credit-decay.expire-after-days", 7));
        long warnDays = Math.max(0, plugin.getConfig().getLong("credit-system.credit-decay.warning-days", 2));

        long nowMillis = System.currentTimeMillis();
        long nowEpochDay = java.time.LocalDate.now().toEpochDay();

        credits.forEach((uuid, data) -> {
            if (data.getBalanceMinutes() <= 0 || data.getLastEarnedAt() == null) return;
            long ageDays = java.time.Duration.between(data.getLastEarnedAt(), java.time.Instant.ofEpochMilli(nowMillis)).toDays();
            Player player = Bukkit.getPlayer(uuid);

            if (ageDays >= expireDays) {
                long prev = data.getBalanceMinutes();
                data.setBalanceMinutes(0);
                if (player != null && player.isOnline()) {
                    String msg = plugin.getConfigManager().getMessage("credit-system.decayed", "&cCredits expired");
                    plugin.getPlatformScheduler().runTaskForEntity(player, () -> player.sendMessage(color(msg)));
                }
                // Guardar y registrar decay
                if (storage != null) {
                    storage.saveOne(uuid, data);
                    if (storage.supportsHistory() && prev > 0) {
                        storage.recordTransaction(uuid, CreditTransactionType.DECAY, -prev, data.getBalanceMinutes(), "decay", System.currentTimeMillis());
                    }
                }
                data.setLowCreditWarned(false);
                data.setLastDecayWarningEpochDay(0);
                return;
            }

            if (warnDays > 0 && ageDays >= (expireDays - warnDays)) {
                if (data.getLastDecayWarningEpochDay() != nowEpochDay && player != null && player.isOnline()) {
                    long daysLeft = Math.max(0, expireDays - ageDays);
                    String msg = plugin.getConfigManager().getMessage("credit-system.decay-warning", "&eCredits expire in {days} days")
                            .replace("{days}", String.valueOf(daysLeft));
                    plugin.getPlatformScheduler().runTaskForEntity(player, () -> player.sendMessage(color(msg)));
                    data.setLastDecayWarningEpochDay(nowEpochDay);
                }
            }
        });
    }

    // ====================== PERSISTENCIA =======================

    private void initializeStorage() {
        // Selección de storage: SQL si habilitado y driver presente; si no, archivo
        Map<UUID, CreditData> loaded;
        try {
            if (plugin.getConfig().getBoolean("credit-system.database.enabled", false)) {
                String type = plugin.getConfig().getString("database.type", "SQLite");
                String prefix = plugin.getConfig().getString("credit-system.database.table-prefix", "afkplus_");
                if ("MySQL".equalsIgnoreCase(type)) {
                    String host = plugin.getConfig().getString("database.mysql.host", "localhost");
                    int port = plugin.getConfig().getInt("database.mysql.port", 3306);
                    String db = plugin.getConfig().getString("database.mysql.database", "antiafkplus");
                    String user = plugin.getConfig().getString("database.mysql.username", "root");
                    String pass = plugin.getConfig().getString("database.mysql.password", "password");
                    storage = new SqlCreditStorage(plugin, SqlCreditStorage.DbType.MYSQL, host, port, db, user, pass, prefix);
                } else {
                    storage = new SqlCreditStorage(plugin, SqlCreditStorage.DbType.SQLITE, null, 0, null, null, null, prefix);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Credit SQL storage unavailable: " + e.getMessage() + ". Falling back to file storage.");
            storage = null;
        }
        if (storage == null) {
            storage = new FileCreditStorage(plugin);
        }
        loaded = storage.loadAll();
        credits.putAll(loaded);
    }

    private void startSaveTask() {
        long minutes = Math.max(1, plugin.getConfig().getLong("credit-system.database.save-interval-minutes", 5));
        long period = minutes * 60L * 20L;
        saveTask = plugin.getPlatformScheduler().runTaskTimer(this::saveFromScheduler, period, period);
    }

    private void saveFromScheduler() {
        if (storage != null) storage.saveAll(new java.util.HashMap<>(credits));
    }

    public void stopConsume(Player player) {
        CreditData data = credits.get(player.getUniqueId());
        if (data == null) return;
        data.stopConsuming();
    }

    // ====================== UTILIDADES =======================

    private static class Ratio { long activeMinutes; long creditMinutes; }

    /**
     * DYNAMIC GROUP DETECTION: Reads all groups defined in credit-system.credit-ratios
     * and automatically checks for corresponding permissions.
     *
     * Permission format: antiafkplus.credit.ratio.<groupname>
     * Example: For "sponsor" group → antiafkplus.credit.ratio.sponsor
     *
     * Priority: Uses the first matching group in priority order (configurable or default).
     */
    private Ratio getRatioFor(Player player) {
        String basePath = "credit-system.credit-ratios.";

        // Obtener todos los grupos definidos en config
        ConfigurationSection ratiosSection = plugin.getConfig().getConfigurationSection(basePath);
        if (ratiosSection == null) {
            // Fallback si no existe la sección
            plugin.getLogger().warning("credit-system.credit-ratios section not found in config, using default 5:1");
            return parseRatio("5:1");
        }

        // Obtener lista de grupos con orden de prioridad
        List<String> priorityOrder = getPriorityOrder(ratiosSection.getKeys(false));

        // Buscar el primer grupo que coincida con los permisos del jugador
        String ratioStr = plugin.getConfig().getString(basePath + "default", "5:1");
        String matchedGroup = "default";

        for (String group : priorityOrder) {
            if (group.equals("default")) continue;

            String permission = "antiafkplus.credit.ratio." + group.toLowerCase().replace("+", "plus");
            if (player.hasPermission(permission)) {
                String configRatio = plugin.getConfig().getString(basePath + group, null);
                if (configRatio != null && !configRatio.isEmpty()) {
                    ratioStr = configRatio;
                    matchedGroup = group;
                    break; // Usar el primero que coincida (mayor prioridad)
                }
            }
        }

        // Debug logging (opcional)
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Player " + player.getName() + " matched credit ratio group: " + matchedGroup + " (" + ratioStr + ")");
        }

        return parseRatio(ratioStr);
    }

    /**
     * DYNAMIC GROUP DETECTION: Reads all groups defined in credit-system.max-credits
     * and automatically checks for corresponding permissions.
     *
     * Permission format: antiafkplus.credit.ratio.<groupname>
     * Example: For "sponsor" group → antiafkplus.credit.ratio.sponsor
     *
     * Priority: Uses the first matching group in priority order (same as credit ratios).
     */
    private long getMaxCreditsFor(Player player) {
        String basePath = "credit-system.max-credits.";

        // Obtener todos los grupos definidos en config
        ConfigurationSection maxSection = plugin.getConfig().getConfigurationSection(basePath);
        if (maxSection == null) {
            plugin.getLogger().warning("credit-system.max-credits section not found in config, using default 120");
            return 120L;
        }

        // Obtener lista de grupos con orden de prioridad
        List<String> priorityOrder = getPriorityOrder(maxSection.getKeys(false));

        // Buscar el primer grupo que coincida con los permisos del jugador
        long max = plugin.getConfig().getLong(basePath + "default", 120);
        String matchedGroup = "default";

        for (String group : priorityOrder) {
            if (group.equals("default")) continue;

            String permission = "antiafkplus.credit.ratio." + group.toLowerCase().replace("+", "plus");
            if (player.hasPermission(permission)) {
                long configMax = plugin.getConfig().getLong(basePath + group, -1);
                if (configMax >= 0) {
                    max = configMax;
                    matchedGroup = group;
                    break; // Usar el primero que coincida (mayor prioridad)
                }
            }
        }

        // Debug logging (opcional)
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Player " + player.getName() + " matched max credits group: " + matchedGroup + " (" + max + " minutes)");
        }

        return Math.max(0, max);
    }

    /**
     * Obtiene el orden de prioridad de grupos para determinar qué ratio/max-credits usar.
     * Soporta configuración personalizada o usa orden predeterminado.
     *
     * Orden predeterminado: admin > sponsor > premium+ > vip+ > premium > vip > otros > default
     *
     * @param groups Set de nombres de grupos desde la configuración
     * @return Lista ordenada por prioridad (mayor prioridad primero)
     */
    private List<String> getPriorityOrder(Set<String> groups) {
        // Cache key para evitar recalcular
        String cacheKey = String.join(",", new TreeSet<>(groups));

        return cachedPriorityOrders.computeIfAbsent(cacheKey, k -> {
            // Verificar si hay orden configurado manualmente
            List<String> configuredOrder = plugin.getConfig().getStringList("credit-system.group-priority-order");

            if (!configuredOrder.isEmpty()) {
                // Usar orden configurado si existe
                List<String> ordered = new ArrayList<>(configuredOrder);
                // Agregar grupos no listados al final (alfabéticamente)
                groups.stream()
                        .filter(g -> !ordered.contains(g))
                        .sorted()
                        .forEach(ordered::add);
                return ordered;
            }

            // Orden predeterminado basado en jerarquía común de servidores
            List<String> defaultOrder = Arrays.asList(
                    "admin",
                    "owner",
                    "sponsor",
                    "premium+",
                    "vip+",
                    "premium",
                    "vip",
                    "member",
                    "default"
            );

            List<String> result = new ArrayList<>();

            // Primero agregar los conocidos en orden de prioridad
            for (String known : defaultOrder) {
                if (groups.contains(known)) {
                    result.add(known);
                }
            }

            // Luego agregar los desconocidos alfabéticamente (custom groups)
            groups.stream()
                    .filter(g -> !defaultOrder.contains(g) && !g.equals("default"))
                    .sorted()
                    .forEach(result::add);

            // Default siempre al final si no estaba ya incluido
            if (groups.contains("default") && !result.contains("default")) {
                result.add("default");
            }

            return result;
        });
    }

    /**
     * Parsea un string de ratio en formato "active:credit" (ej: "5:1")
     *
     * @param ratioStr String en formato "active:credit"
     * @return Objeto Ratio con valores parseados, o valores por defecto si hay error
     */
    private Ratio parseRatio(String ratioStr) {
        Ratio r = new Ratio();
        String[] parts = ratioStr.split(":");
        long a = 5, c = 1;
        try {
            if (parts.length >= 2) {
                a = Long.parseLong(parts[0].trim());
                c = Long.parseLong(parts[1].trim());
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid credit ratio format: '" + ratioStr + "', using default 5:1. Format should be 'active:credit' (e.g., '5:1')");
        }
        r.activeMinutes = Math.max(1, a);
        r.creditMinutes = Math.max(1, c);
        return r;
    }

    /**
     * Limpia el caché de prioridad de grupos.
     * Debería llamarse cuando se recarga la configuración.
     */
    public void clearCache() {
        cachedPriorityOrders.clear();
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Credit group priority cache cleared");
        }
    }

    private void teleportToAfkZone(Player player) {
        // Prefer zone-management: first 'afk' zone, then 'spawn' zone
        if (plugin.getConfig().getBoolean("zone-management.enabled", false)) {
            String loc = plugin.getConfig().getString("zone-management.zones.afk.teleport-location", null);
            if (loc == null || loc.isBlank()) {
                loc = plugin.getConfig().getString("zone-management.zones.spawn.teleport-location", "");
            }
            if (loc != null && !loc.isEmpty()) {
                platformTeleport(player, loc);
                return;
            }
        }
        // Fallback: credit-system.afk-zone
        if (!plugin.getConfig().getBoolean("credit-system.afk-zone.enabled", true)) {
            // Sin zona válida: usar spawn del mundo actual
            plugin.getPlatformScheduler().runTaskForEntity(player, () -> player.teleport(player.getWorld().getSpawnLocation()));
            return;
        }
        String world = plugin.getConfig().getString("credit-system.afk-zone.world", player.getWorld().getName());
        String coords = plugin.getConfig().getString("credit-system.afk-zone.location", "0,100,0");
        platformTeleport(player, world + "," + coords);
    }

    private void platformTeleport(Player player, String locationString) {
        plugin.getPlatformScheduler().runTaskForEntity(player, () -> {
            try {
                String[] parts = locationString.split(",");
                if (parts.length < 4) {
                    String msg = plugin.getConfigManager().getMessage("credit-system.errors.invalid-location", "&cInvalid AFK zone location");
                    player.sendMessage(color(msg));
                    player.teleport(player.getWorld().getSpawnLocation());
                    return;
                }
                World w = Bukkit.getWorld(parts[0].trim());
                if (w == null) w = player.getWorld();
                double x = Double.parseDouble(parts[1].trim());
                double y = Double.parseDouble(parts[2].trim());
                double z = Double.parseDouble(parts[3].trim());
                float yaw = parts.length >= 6 ? Float.parseFloat(parts[4].trim()) : 0f;
                float pitch = parts.length >= 6 ? Float.parseFloat(parts[5].trim()) : 0f;
                Location dest = new Location(w, x, y, z, yaw, pitch);
                if (y < 0) dest.setY(w.getSpawnLocation().getY());
                player.teleport(dest);
                // Marcar bandera de zona AFK
                getData(player.getUniqueId()).setInAfkZone(true);
                String msg = plugin.getConfigManager().getMessage("credit-system.zone-teleport", "&aTeleported to AFK zone");
                player.sendMessage(color(msg));
            } catch (Exception ex) {
                player.teleport(player.getWorld().getSpawnLocation());
            }
        });
    }

    // ====================== API pública (Fase 2) =======================

    public enum ReturnResult { SUCCESS, NOT_IN_ZONE, NO_SAVED_LOCATION, COOLDOWN, TOO_FAR, UNSAFE_LOCATION, SYSTEM_DISABLED }

    public long getBalance(Player player) {
        return getData(player.getUniqueId()).getBalanceMinutes();
    }

    public long getMaxCredits(Player player) {
        return getMaxCreditsFor(player);
    }

    /**
     * API METHOD: Obtiene el ratio de créditos como string para un jugador.
     * Usa la misma lógica dinámica que getRatioFor() para detectar grupos.
     *
     * @param player Jugador a consultar
     * @return String con el ratio en formato "active:credit" (ej: "5:1")
     */
    public String getRatioString(Player player) {
        String basePath = "credit-system.credit-ratios.";

        // Obtener todos los grupos definidos en config
        ConfigurationSection ratiosSection = plugin.getConfig().getConfigurationSection(basePath);
        if (ratiosSection == null) {
            return "5:1"; // Fallback
        }

        // Obtener lista de grupos con orden de prioridad
        List<String> priorityOrder = getPriorityOrder(ratiosSection.getKeys(false));

        // Buscar el primer grupo que coincida con los permisos del jugador
        String ratioStr = plugin.getConfig().getString(basePath + "default", "5:1");

        for (String group : priorityOrder) {
            if (group.equals("default")) continue;

            String permission = "antiafkplus.credit.ratio." + group.toLowerCase().replace("+", "plus");
            if (player.hasPermission(permission)) {
                String configRatio = plugin.getConfig().getString(basePath + group, null);
                if (configRatio != null && !configRatio.isEmpty()) {
                    ratioStr = configRatio;
                    break;
                }
            }
        }

        return ratioStr;
    }

    public boolean isInAfkZone(Player player) {
        return getData(player.getUniqueId()).isInAfkZone();
    }

    public org.bukkit.Location getOriginalLocation(Player player) {
        return getData(player.getUniqueId()).getOriginalLocation();
    }

    public java.time.Instant getExpirationInstant(Player player) {
        if (!plugin.getConfig().getBoolean("credit-system.credit-decay.enabled", false)) return null;
        CreditData data = getData(player.getUniqueId());
        if (data.getBalanceMinutes() <= 0) return null;
        java.time.Instant last = data.getLastEarnedAt();
        if (last == null) return null;
        long expireDays = Math.max(1, plugin.getConfig().getLong("credit-system.credit-decay.expire-after-days", 7));
        return last.plus(java.time.Duration.ofDays(expireDays));
    }

    public ReturnResult returnFromAFKZone(Player player) {
        if (!isEnabled()) return ReturnResult.SYSTEM_DISABLED;
        CreditData data = getData(player.getUniqueId());
        if (!data.isInAfkZone()) return ReturnResult.NOT_IN_ZONE;
        if (data.getOriginalLocation() == null) return ReturnResult.NO_SAVED_LOCATION;

        long cooldownSec = plugin.getConfig().getLong("credit-system.return-command.cooldown-seconds", 10);
        long now = System.currentTimeMillis() / 1000L;
        long last = data.getLastReturnAtEpochSeconds();
        if (cooldownSec > 0 && last > 0 && (now - last) < cooldownSec) {
            return ReturnResult.COOLDOWN;
        }

        // Validar distancia máxima desde la zona
        int maxDist = plugin.getConfig().getInt("credit-system.return-command.max-distance-from-zone", 0);
        if (maxDist > 0) {
            Location zone = getAFKZoneLocation(player);
            if (zone != null && player.getWorld().equals(zone.getWorld())) {
                if (player.getLocation().distance(zone) > maxDist) {
                    return ReturnResult.TOO_FAR;
                }
            }
        }

        boolean validateOriginal = plugin.getConfig().getBoolean("credit-system.return-command.validate-original-location", true);
        data.setLastReturnAtEpochSeconds(now);
        plugin.getPlatformScheduler().runTaskForEntity(player, () -> {
            try {
                Location target = data.getOriginalLocation();
                if (validateOriginal && !isLocationSafe(target)) {
                    // Teleport a spawn y devolver código UNSAFE
                    player.teleport(player.getWorld().getSpawnLocation());
                    data.setInAfkZone(false);
                    throw new UnsafeLocationException();
                }
                player.teleport(target);
            } catch (Exception ex) {
                if (!(ex instanceof UnsafeLocationException)) {
                    player.teleport(player.getWorld().getSpawnLocation());
                }
            } finally {
                data.setInAfkZone(false);
            }
        });
        return validateOriginal ? ReturnResult.SUCCESS : ReturnResult.SUCCESS;
    }

    private boolean isLocationSafe(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (loc.getY() < 0 || loc.getY() > loc.getWorld().getMaxHeight()) return false;
        // Considerar seguro si el bloque destino no es sólido y la cabeza tampoco
        var block = loc.getBlock();
        var above = loc.clone().add(0, 1, 0).getBlock();
        return !(block.isSolid() || above.isSolid());
    }

    private static class UnsafeLocationException extends RuntimeException {}

    public boolean addCredits(Player player, long minutes) {
        if (minutes <= 0) return false;
        CreditData data = getData(player.getUniqueId());
        long max = getMaxCreditsFor(player);
        long newBal = Math.min(data.getBalanceMinutes() + minutes, max);
        boolean changed = newBal != data.getBalanceMinutes();
        if (changed) {
            data.setBalanceMinutes(newBal);
            if (storage != null) {
                storage.saveOne(player.getUniqueId(), data);
                if (storage.supportsHistory()) {
                    storage.recordTransaction(player.getUniqueId(), CreditTransactionType.EARN, minutes, data.getBalanceMinutes(), "api-add", System.currentTimeMillis());
                }
            }
        }
        return changed;
    }

    public boolean setCreditBalance(Player player, long minutes) {
        if (minutes < 0) minutes = 0;
        CreditData data = getData(player.getUniqueId());
        long max = getMaxCreditsFor(player);
        long newBal = Math.min(minutes, max);
        boolean changed = newBal != data.getBalanceMinutes();
        if (changed) {
            long delta = newBal - data.getBalanceMinutes();
            data.setBalanceMinutes(newBal);
            if (storage != null) {
                storage.saveOne(player.getUniqueId(), data);
                if (storage.supportsHistory()) {
                    storage.recordTransaction(player.getUniqueId(), delta >= 0 ? CreditTransactionType.SET : CreditTransactionType.SET, delta, data.getBalanceMinutes(), "api-set", System.currentTimeMillis());
                }
            }
        }
        return changed;
    }

    public boolean hasCredits(Player player, long minutes) {
        return getData(player.getUniqueId()).getBalanceMinutes() >= Math.max(0, minutes);
    }

    public boolean consumeCredits(Player player, long minutes) {
        if (minutes <= 0) return true;
        CreditData data = getData(player.getUniqueId());
        if (data.getBalanceMinutes() < minutes) return false;
        data.setBalanceMinutes(data.getBalanceMinutes() - minutes);
        if (storage != null) storage.saveOne(player.getUniqueId(), data);
        return true;
    }

    public Location getAFKZoneLocation(Player player) {
        // Priorizar zone-management .zones.afk o .zones.spawn si existe
        if (plugin.getConfig().getBoolean("zone-management.enabled", false)) {
            String loc = plugin.getConfig().getString("zone-management.zones.afk.teleport-location", null);
            if (loc == null || loc.isBlank()) {
                loc = plugin.getConfig().getString("zone-management.zones.spawn.teleport-location", "");
            }
            Location parsed = parseLocation(player, loc);
            if (parsed != null) return parsed;
        }
        String world = plugin.getConfig().getString("credit-system.afk-zone.world", player.getWorld().getName());
        String coords = plugin.getConfig().getString("credit-system.afk-zone.location", "0,100,0");
        return parseLocation(player, world + "," + coords);
    }

    private Location parseLocation(Player ref, String locationString) {
        try {
            if (locationString == null || locationString.trim().isEmpty()) return null;
            String[] parts = locationString.split(",");
            if (parts.length < 4) return null;
            World w = Bukkit.getWorld(parts[0].trim());
            if (w == null) w = ref.getWorld();
            double x = Double.parseDouble(parts[1].trim());
            double y = Double.parseDouble(parts[2].trim());
            double z = Double.parseDouble(parts[3].trim());
            float yaw = parts.length >= 6 ? Float.parseFloat(parts[4].trim()) : 0f;
            float pitch = parts.length >= 6 ? Float.parseFloat(parts[5].trim()) : 0f;
            Location dest = new Location(w, x, y, z, yaw, pitch);
            if (y < 0) dest.setY(w.getSpawnLocation().getY());
            return dest;
        } catch (Exception ignored) { return null; }
    }

    private String color(String s) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }

    // ====================== HISTORIAL / ADMIN =======================

    public java.util.List<CreditTransaction> getHistory(Player player, int limit) {
        if (storage == null || !storage.supportsHistory()) return java.util.Collections.emptyList();
        return storage.getHistory(player.getUniqueId(), Math.max(1, limit));
    }

    public boolean isHistoryAvailable() { return storage != null && storage.supportsHistory(); }

    public void adminGive(Player target, long minutes) {
        if (minutes <= 0) return;
        CreditData data = getData(target.getUniqueId());
        long max = getMaxCreditsFor(target);
        long newBal = Math.min(data.getBalanceMinutes() + minutes, max);
        long delta = newBal - data.getBalanceMinutes();
        if (delta <= 0) return;
        data.setBalanceMinutes(newBal);
        if (storage != null) {
            storage.saveOne(target.getUniqueId(), data);
            if (storage.supportsHistory()) {
                storage.recordTransaction(target.getUniqueId(), CreditTransactionType.ADMIN_GIVE, delta, data.getBalanceMinutes(), "admin-give", System.currentTimeMillis());
            }
        }
    }

    public void adminTake(Player target, long minutes) {
        if (minutes <= 0) return;
        CreditData data = getData(target.getUniqueId());
        long delta = Math.min(minutes, data.getBalanceMinutes());
        if (delta <= 0) return;
        data.setBalanceMinutes(data.getBalanceMinutes() - delta);
        if (storage != null) {
            storage.saveOne(target.getUniqueId(), data);
            if (storage.supportsHistory()) {
                storage.recordTransaction(target.getUniqueId(), CreditTransactionType.ADMIN_TAKE, -delta, data.getBalanceMinutes(), "admin-take", System.currentTimeMillis());
            }
        }
    }

    public void adminSet(Player target, long minutes) {
        if (minutes < 0) minutes = 0;
        CreditData data = getData(target.getUniqueId());
        long max = getMaxCreditsFor(target);
        long newBal = Math.min(minutes, max);
        long delta = newBal - data.getBalanceMinutes();
        if (delta == 0) return;
        data.setBalanceMinutes(newBal);
        if (storage != null) {
            storage.saveOne(target.getUniqueId(), data);
            if (storage.supportsHistory()) {
                storage.recordTransaction(target.getUniqueId(), CreditTransactionType.SET, delta, data.getBalanceMinutes(), "admin-set", System.currentTimeMillis());
            }
        }
    }

    public void adminReset(Player target) {
        CreditData data = getData(target.getUniqueId());
        long prev = data.getBalanceMinutes();
        if (prev == 0) return;
        data.setBalanceMinutes(0);
        if (storage != null) {
            storage.saveOne(target.getUniqueId(), data);
            if (storage.supportsHistory()) {
                storage.recordTransaction(target.getUniqueId(), CreditTransactionType.RESET, -prev, data.getBalanceMinutes(), "admin-reset", System.currentTimeMillis());
            }
        }
    }
}
