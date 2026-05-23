package me.koyere.antiafkplus.visual;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.events.PlayerAFKStateChangeEvent;
import me.koyere.antiafkplus.events.PlayerAFKStateChangeEvent.AFKState;
import me.koyere.antiafkplus.platform.PlatformScheduler;

/**
 * Manages visual indicators for AFK players: particles, tab-list prefix,
 * name-tag prefix, and floating holograms via DecentHolograms or FancyHolograms.
 *
 * <p>Hologram support is fully reflection-based — neither DecentHolograms nor
 * FancyHolograms is a hard dependency. The first compatible provider found at
 * startup is used. If neither is present (or initialisation fails), all hologram
 * calls become no-ops and the rest of the visual system continues normally.
 */
@SuppressWarnings("deprecation")
public class VisualEffectsManager implements Listener {

    private final AntiAFKPlus plugin;

    // --- Name / tab-list state ---
    private final Map<UUID, String> originalTabNames    = new ConcurrentHashMap<>();
    private final Map<UUID, String> originalDisplayNames = new ConcurrentHashMap<>();

    // --- Hologram state ---
    /** Null when no compatible hologram provider was found or holograms are disabled. */
    private HologramBackend hologramBackend;
    /** Maps player UUID → hologram id currently shown for that player. */
    private final Map<UUID, String> hologramIds = new ConcurrentHashMap<>();
    private PlatformScheduler.ScheduledTask hologramUpdateTask;

    // --- Scheduled tasks ---
    private PlatformScheduler.ScheduledTask particleTask;

    // =========================================================================
    //  Constructor
    // =========================================================================

    public VisualEffectsManager(AntiAFKPlus plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startParticleTask();
        initHologramSupport();
    }

    // =========================================================================
    //  Hologram Backend Interface
    // =========================================================================

    /**
     * Provider-agnostic contract for a hologram.
     * All methods are expected to swallow failures silently — they never throw.
     */
    private interface HologramBackend {
        /**
         * Creates (or completely replaces) a hologram at {@code loc} with the
         * given {@code lines}. If a hologram with this {@code id} already exists
         * it is removed first.
         */
        void show(String id, Location loc, List<String> lines);

        /**
         * Removes the hologram identified by {@code id}.
         * Must be a no-op when no such hologram exists.
         */
        void hide(String id);

        /** Called once on plugin shutdown. May be a no-op. */
        void shutdown();

        /** Human-readable provider name used in log messages. */
        String getName();
    }

    // =========================================================================
    //  DecentHolograms Backend
    // =========================================================================

    /**
     * Reflection-based backend for <a href="https://www.spigotmc.org/resources/96927/">
     * DecentHolograms</a>.
     *
     * <p>Uses {@code DHAPI} static methods:
     * <ul>
     *   <li>{@code createHologram(String, Location[, boolean])} — creates a hologram
     *       (3-arg preferred so we pass {@code saveToFile=false})</li>
     *   <li>{@code addHologramLine(Hologram, String)} — appends a text line</li>
     *   <li>{@code removeHologram(String)} — deletes by name</li>
     * </ul>
     */
    private static final class DecentHologramsBackend implements HologramBackend {

        private final Method removeHologram;
        private final Method createHologram;
        private final Method addHologramLine;
        /** True when the 3-arg createHologram(name, loc, saveToFile) variant was resolved. */
        private final boolean usesSaveArg;

        DecentHologramsBackend() throws ReflectiveOperationException {
            Class<?> dhapi = Class.forName("eu.decentsoftware.holograms.api.DHAPI");

            // Prefer the 3-arg version so holograms are not persisted to disk.
            Method cm;
            boolean saveArg;
            try {
                cm = dhapi.getMethod("createHologram", String.class, Location.class, boolean.class);
                saveArg = true;
            } catch (NoSuchMethodException e) {
                cm = dhapi.getMethod("createHologram", String.class, Location.class);
                saveArg = false;
            }
            createHologram = cm;
            usesSaveArg    = saveArg;

            // Derive the Hologram class from the method's own return type — avoids
            // hardcoding the package path across DH versions.
            Class<?> holoClass = createHologram.getReturnType();
            addHologramLine = dhapi.getMethod("addHologramLine", holoClass, String.class);
            removeHologram  = dhapi.getMethod("removeHologram",  String.class);
        }

        @Override
        public void show(String id, Location loc, List<String> lines) {
            try {
                // Silently remove any stale hologram with the same id first.
                try { removeHologram.invoke(null, id); } catch (Exception ignored) {}
                Object holo = usesSaveArg
                        ? createHologram.invoke(null, id, loc, false)
                        : createHologram.invoke(null, id, loc);
                for (String line : lines) {
                    addHologramLine.invoke(null, holo, line);
                }
            } catch (Exception ignored) {}
        }

        @Override
        public void hide(String id) {
            try { removeHologram.invoke(null, id); } catch (Exception ignored) {}
        }

        @Override
        public void shutdown() {}

        @Override
        public String getName() { return "DecentHolograms"; }
    }

    // =========================================================================
    //  FancyHolograms Backend
    // =========================================================================

    /**
     * Reflection-based backend for <a href="https://modrinth.com/plugin/fancyholograms">
     * FancyHolograms</a> (v2.x API).
     *
     * <p>Workflow (all via reflection):
     * <ol>
     *   <li>Obtain {@code HologramManager} from the plugin instance.</li>
     *   <li>Construct {@code TextHologramData(name, location)} and set text lines
     *       as a single {@code "\n"}-separated string via {@code setText(String)}.</li>
     *   <li>Call {@code manager.create(data)} then {@code manager.addHologram(hologram)}.</li>
     *   <li>To remove: call {@code manager.getHologram(name)} → unwrap Optional →
     *       {@code manager.removeHologram(hologram)}.</li>
     * </ol>
     */
    private static final class FancyHologramsBackend implements HologramBackend {

        private final Object  manager;
        private final Constructor<?> textDataCtor;
        private final Method  setTextMethod;
        private final Method  createMethod;
        private final Method  addMethod;
        private final Method  removeMethod;
        private final Method  getMethod;

        FancyHologramsBackend() throws ReflectiveOperationException {
            Plugin fhPlugin = Bukkit.getPluginManager().getPlugin("FancyHolograms");
            if (fhPlugin == null) throw new IllegalStateException("FancyHolograms plugin not found");

            manager = fhPlugin.getClass().getMethod("getHologramsManager").invoke(fhPlugin);
            if (manager == null) throw new IllegalStateException("HologramManager returned null");

            // TextHologramData(String name, Location location)
            Class<?> textDataClass = Class.forName(
                    "de.oliver.fancyholograms.api.hologram.type.TextHologramData");
            textDataCtor  = textDataClass.getConstructor(String.class, Location.class);
            setTextMethod = textDataClass.getMethod("setText", String.class);

            // HologramData / Hologram — class name varies between minor versions.
            Class<?> holoDataClass = resolveClass(
                    "de.oliver.fancyholograms.api.data.HologramData",
                    "de.oliver.fancyholograms.api.HologramData");
            Class<?> holoClass = resolveClass(
                    "de.oliver.fancyholograms.api.hologram.Hologram",
                    "de.oliver.fancyholograms.api.Hologram");

            Class<?> mgrClass = manager.getClass();
            createMethod = resolveMethod(mgrClass, "create",         holoDataClass);
            addMethod    = resolveMethod(mgrClass, "addHologram",    holoClass);
            removeMethod = resolveMethod(mgrClass, "removeHologram", holoClass);
            getMethod    = resolveMethod(mgrClass, "getHologram",    String.class);
        }

        /** Tries each fully-qualified class name in order; returns the first that loads. */
        private static Class<?> resolveClass(String... names) throws ClassNotFoundException {
            ClassNotFoundException last = null;
            for (String name : names) {
                try { return Class.forName(name); }
                catch (ClassNotFoundException e) { last = e; }
            }
            throw last; // last cannot be null because names.length >= 1
        }

        /**
         * Walks the class hierarchy (including interfaces) to find a method by
         * name and parameter types. Falls back gracefully across API refactors.
         */
        private static Method resolveMethod(Class<?> root, String name, Class<?>... params)
                throws NoSuchMethodException {
            Class<?> c = root;
            while (c != null) {
                try { return c.getDeclaredMethod(name, params); }
                catch (NoSuchMethodException ignored) {}
                for (Class<?> iface : c.getInterfaces()) {
                    try { return iface.getMethod(name, params); }
                    catch (NoSuchMethodException ignored) {}
                }
                c = c.getSuperclass();
            }
            throw new NoSuchMethodException(root.getName() + "#" + name);
        }

        @Override
        public void show(String id, Location loc, List<String> lines) {
            try {
                hide(id); // remove any existing hologram with this id first
                Object data = textDataCtor.newInstance(id, loc);
                setTextMethod.invoke(data, String.join("\n", lines));
                Object holo = createMethod.invoke(manager, data);
                addMethod.invoke(manager, holo);
            } catch (Exception ignored) {}
        }

        @Override
        @SuppressWarnings("unchecked")
        public void hide(String id) {
            try {
                java.util.Optional<Object> opt =
                        (java.util.Optional<Object>) getMethod.invoke(manager, id);
                if (opt != null && opt.isPresent()) {
                    removeMethod.invoke(manager, opt.get());
                }
            } catch (Exception ignored) {}
        }

        @Override
        public void shutdown() {}

        @Override
        public String getName() { return "FancyHolograms"; }
    }

    // =========================================================================
    //  Hologram Lifecycle
    // =========================================================================

    /**
     * Detects and initialises the first available hologram provider.
     * Priority order: DecentHolograms → FancyHolograms.
     *
     * <p>If {@code visual-effects.holograms.enabled} is false in config,
     * this method returns immediately leaving {@link #hologramBackend} null.
     */
    private void initHologramSupport() {
        if (!plugin.getConfig().getBoolean("visual-effects.holograms.enabled", false)) return;

        // --- DecentHolograms ---
        if (Bukkit.getPluginManager().getPlugin("DecentHolograms") != null) {
            try {
                hologramBackend = new DecentHologramsBackend();
                plugin.getLogger().info("[VisualEffects] Hologram provider: DecentHolograms");
                startHologramUpdateTask();
                return;
            } catch (Exception e) {
                plugin.getLogger().warning(
                        "[VisualEffects] DecentHolograms detected but failed to initialise: " + e.getMessage());
            }
        }

        // --- FancyHolograms ---
        if (Bukkit.getPluginManager().getPlugin("FancyHolograms") != null) {
            try {
                hologramBackend = new FancyHologramsBackend();
                plugin.getLogger().info("[VisualEffects] Hologram provider: FancyHolograms");
                startHologramUpdateTask();
                return;
            } catch (Exception e) {
                plugin.getLogger().warning(
                        "[VisualEffects] FancyHolograms detected but failed to initialise: " + e.getMessage());
            }
        }

        plugin.getLogger().info(
                "[VisualEffects] No hologram provider found (DecentHolograms / FancyHolograms). Holograms disabled.");
    }

    /**
     * Starts the repeating task that refreshes all active AFK holograms.
     * Each tick it removes-and-recreates the hologram above the player so that
     * both the text (time placeholder) and the position stay accurate.
     *
     * <p>The interval is read from {@code visual-effects.holograms.update-interval-seconds}
     * (default 5 → 100 ticks).
     */
    private void startHologramUpdateTask() {
        long intervalTicks =
                plugin.getConfig().getLong("visual-effects.holograms.update-interval-seconds", 5L) * 20L;

        hologramUpdateTask = plugin.getPlatformScheduler().runTaskTimer(() -> {
            if (hologramBackend == null || hologramIds.isEmpty()) return;

            double heightOffset = plugin.getConfig().getDouble(
                    "visual-effects.holograms.height-offset", 2.5);

            // Snapshot the entry set to avoid ConcurrentModificationException.
            for (Map.Entry<UUID, String> entry : new ArrayList<>(hologramIds.entrySet())) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null || !player.isOnline()) continue;

                String id     = entry.getValue();
                Location loc  = player.getLocation().add(0, heightOffset, 0);
                List<String> lines = buildHologramLines(player);

                // Remove-then-recreate is the safest update strategy across API versions.
                hologramBackend.hide(id);
                hologramBackend.show(id, loc, lines);
            }
        }, intervalTicks, intervalTicks);
    }

    /**
     * Shows an AFK hologram above {@code player}.
     * Called when the player enters AFK state (if holograms are enabled and a
     * backend is available).
     */
    private void showHologram(Player player) {
        if (hologramBackend == null) return;

        UUID   uuid        = player.getUniqueId();
        String id          = "aafkp_" + uuid.toString().replace("-", "");
        double heightOffset = plugin.getConfig().getDouble(
                "visual-effects.holograms.height-offset", 2.5);
        Location loc  = player.getLocation().add(0, heightOffset, 0);
        List<String> lines = buildHologramLines(player);

        hologramBackend.hide(id); // clean up any stale hologram from a previous session
        hologramBackend.show(id, loc, lines);
        hologramIds.put(uuid, id);
    }

    /**
     * Removes the AFK hologram above {@code player}.
     * Called when the player leaves AFK state or disconnects.
     */
    private void hideHologram(Player player) {
        if (hologramBackend == null) return;
        String id = hologramIds.remove(player.getUniqueId());
        if (id != null) hologramBackend.hide(id);
    }

    /**
     * Builds the list of hologram text lines for {@code player},
     * replacing {@code {player}} and {@code {time}} placeholders.
     *
     * <p>Falls back to two hard-coded lines when no lines are configured.
     */
    private List<String> buildHologramLines(Player player) {
        List<String> raw = plugin.getConfig().getStringList("visual-effects.holograms.lines");
        if (raw.isEmpty()) {
            raw = new ArrayList<>();
            raw.add("&c[AFK]");
            raw.add("&7{player}");
            raw.add("&eAFK for {time}");
        }

        long afkStartMs = (plugin.getAfkManager() != null)
                ? plugin.getAfkManager().getAFKDetectionTime(player)
                : 0L;
        String timeStr = (afkStartMs > 0)
                ? formatDuration(System.currentTimeMillis() - afkStartMs)
                : "0s";

        List<String> result = new ArrayList<>(raw.size());
        for (String line : raw) {
            result.add(color(line
                    .replace("{player}", player.getName())
                    .replace("{time}",   timeStr)));
        }
        return result;
    }

    /**
     * Formats a millisecond duration into a concise human-readable string.
     * Examples: {@code "45s"}, {@code "3m 40s"}, {@code "2h 15m"}.
     */
    private static String formatDuration(long ms) {
        long total   = ms / 1_000L;
        long hours   = total / 3_600L;
        long minutes = (total % 3_600L) / 60L;
        long seconds = total % 60L;
        if (hours   > 0) return hours   + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    // =========================================================================
    //  Particle System
    // =========================================================================

    /**
     * Starts the repeating task that spawns particles above AFK players.
     */
    private void startParticleTask() {
        particleTask = plugin.getPlatformScheduler().runTaskTimer(() -> {
            if (!plugin.getConfig().getBoolean("visual-effects.particles.enabled", false)) return;
            if (plugin.getAfkManager() == null) return;

            Set<UUID> afkUUIDs = plugin.getAfkManager().getAfkPlayerUUIDs();
            if (afkUUIDs.isEmpty()) return;

            String type    = plugin.getConfig().getString("visual-effects.particles.type", "CLOUD");
            int    count   = plugin.getConfig().getInt("visual-effects.particles.count", 5);
            double offsetX = plugin.getConfig().getDouble("visual-effects.particles.offset-x", 0.3);
            double offsetY = plugin.getConfig().getDouble("visual-effects.particles.offset-y", 0.5);
            double offsetZ = plugin.getConfig().getDouble("visual-effects.particles.offset-z", 0.3);
            double speed   = plugin.getConfig().getDouble("visual-effects.particles.speed", 0.02);

            Particle particle;
            try {
                particle = Particle.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid particle type in config: " + type);
                return;
            }

            for (UUID uuid : afkUUIDs) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) continue;
                Location location = player.getLocation().add(0, 2.2, 0);
                try {
                    player.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to spawn particle for " + player.getName() + ": " + e.getMessage());
                }
            }
        }, 20L, 20L);
    }

    // =========================================================================
    //  Event Handlers
    // =========================================================================

    /**
     * Handles AFK state changes to apply or remove tab-list, name-tag,
     * and hologram visuals.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAFKStateChange(PlayerAFKStateChangeEvent event) {
        Player   player  = event.getPlayer();
        AFKState toState = event.getToState();

        if (toState.name().startsWith("AFK")) {
            applyAFKVisuals(player);
        } else if (toState == AFKState.ACTIVE) {
            removeAFKVisuals(player);
        }
    }

    /**
     * Cleans up stored names and removes the AFK hologram when a player disconnects.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID   uuid   = player.getUniqueId();
        originalTabNames.remove(uuid);
        originalDisplayNames.remove(uuid);
        hideHologram(player);
    }

    // =========================================================================
    //  Tab-List System
    // =========================================================================

    private void applyTabListPrefix(Player player) {
        if (!plugin.getConfig().getBoolean("visual-effects.tab-list.enabled", false)) return;

        UUID uuid = player.getUniqueId();
        if (!originalTabNames.containsKey(uuid)) {
            String current = player.getPlayerListName();
            originalTabNames.put(uuid, current != null ? current : player.getName());
        }

        String prefix = plugin.getConfig().getString("visual-effects.tab-list.afk-prefix", "&7[AFK] ");
        String suffix = plugin.getConfig().getString("visual-effects.tab-list.afk-suffix", "");
        player.setPlayerListName(color(prefix) + player.getName() + color(suffix));
    }

    private void restoreTabListName(Player player) {
        if (!plugin.getConfig().getBoolean("visual-effects.tab-list.enabled", false)) return;
        String original = originalTabNames.remove(player.getUniqueId());
        if (original != null) player.setPlayerListName(original);
    }

    // =========================================================================
    //  Name-Tag System
    // =========================================================================

    private void applyNameTagPrefix(Player player) {
        if (!plugin.getConfig().getBoolean("visual-effects.name-tags.enabled", false)) return;

        UUID uuid = player.getUniqueId();
        if (!originalDisplayNames.containsKey(uuid)) {
            String current = player.getDisplayName();
            originalDisplayNames.put(uuid, current != null ? current : player.getName());
        }

        String prefix = plugin.getConfig().getString("visual-effects.name-tags.afk-prefix", "&7[AFK] ");
        String suffix = plugin.getConfig().getString("visual-effects.name-tags.afk-suffix", "");
        player.setDisplayName(color(prefix) + player.getName() + color(suffix));
    }

    private void restoreDisplayName(Player player) {
        if (!plugin.getConfig().getBoolean("visual-effects.name-tags.enabled", false)) return;
        String original = originalDisplayNames.remove(player.getUniqueId());
        if (original != null) player.setDisplayName(original);
    }

    // =========================================================================
    //  Combined Apply / Remove
    // =========================================================================

    private void applyAFKVisuals(Player player) {
        applyTabListPrefix(player);
        applyNameTagPrefix(player);
        showHologram(player);
    }

    private void removeAFKVisuals(Player player) {
        restoreTabListName(player);
        restoreDisplayName(player);
        hideHologram(player);
    }

    // =========================================================================
    //  Lifecycle
    // =========================================================================

    /**
     * Shuts down the visual effects manager: cancels all tasks, restores all
     * modified player names, and removes any active AFK holograms.
     */
    public void shutdown() {
        // Cancel scheduled tasks.
        if (particleTask != null && !particleTask.isCancelled()) {
            particleTask.cancel();
        }
        if (hologramUpdateTask != null && !hologramUpdateTask.isCancelled()) {
            hologramUpdateTask.cancel();
        }

        // Remove all active AFK holograms.
        if (hologramBackend != null) {
            for (String id : hologramIds.values()) {
                hologramBackend.hide(id);
            }
            hologramIds.clear();
            hologramBackend.shutdown();
        }

        // Restore all modified player names.
        for (Map.Entry<UUID, String> entry : originalTabNames.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                player.setPlayerListName(entry.getValue());
            }
        }
        for (Map.Entry<UUID, String> entry : originalDisplayNames.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                player.setDisplayName(entry.getValue());
            }
        }

        originalTabNames.clear();
        originalDisplayNames.clear();
    }

    // =========================================================================
    //  Utility
    // =========================================================================

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}
