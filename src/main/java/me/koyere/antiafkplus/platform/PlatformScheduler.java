package me.koyere.antiafkplus.platform;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Platform-agnostic scheduler that automatically detects and adapts to 
 * Folia, Paper, Spigot, Bukkit, Purpur, and other server implementations.
 * Provides unified scheduling API with optimal performance for each platform.
 */
public class PlatformScheduler {
    
    private final AntiAFKPlus plugin;
    private final Logger logger;
    private final PlatformType platformType;
    private final boolean supportsFolia;
    private final boolean supportsAsyncChunks;
    
    // Folia-specific classes (loaded via reflection to avoid hard dependencies)
    private Class<?> foliaGlobalRegionSchedulerClass;
    private Class<?> foliaRegionSchedulerClass;
    private Class<?> foliaEntitySchedulerClass;
    private Object globalRegionScheduler;
    
    public PlatformScheduler(AntiAFKPlus plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.platformType = detectPlatform();
        this.supportsFolia = platformType == PlatformType.FOLIA;
        this.supportsAsyncChunks = detectAsyncChunkSupport();
        
        if (supportsFolia) {
            initializeFoliaSupport();
        }
        
        logger.info("🔧 Platform detected: " + platformType.getDisplayName());
        logger.info("   Folia support: " + (supportsFolia ? "✅ Active" : "❌ Not needed"));
        logger.info("   Async chunks: " + (supportsAsyncChunks ? "✅ Supported" : "❌ Not available"));
    }
    
    /**
     * Detect the server platform type.
     */
    private PlatformType detectPlatform() {
        String serverVersion = Bukkit.getVersion().toLowerCase();
        String serverName = Bukkit.getName().toLowerCase();
        
        // Check for Folia first (most specific)
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return PlatformType.FOLIA;
        } catch (ClassNotFoundException ignored) {}
        
        // Check for Paper derivatives
        if (serverName.contains("purpur") || serverVersion.contains("purpur")) {
            return PlatformType.PURPUR;
        }
        
        if (serverName.contains("paper") || serverVersion.contains("paper")) {
            return PlatformType.PAPER;
        }
        
        // Check for Spigot
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return PlatformType.SPIGOT;
        } catch (ClassNotFoundException ignored) {}
        
        // Fallback to Bukkit
        return PlatformType.BUKKIT;
    }
    
    /**
     * Detect if the server supports async chunk operations.
     */
    private boolean detectAsyncChunkSupport() {
        try {
            // Check if PaperLib or similar async chunk methods exist
            Class.forName("io.papermc.lib.PaperLib");
            return true;
        } catch (ClassNotFoundException e) {
            try {
                // Check for native Paper async chunk methods
                Bukkit.getServer().getClass().getMethod("getChunkAtAsync", 
                    org.bukkit.World.class, int.class, int.class);
                return true;
            } catch (NoSuchMethodException ignored) {}
        }
        
        return false;
    }
    
    /**
     * Initialize Folia-specific support using reflection.
     */
    private void initializeFoliaSupport() {
        try {
            // Load Folia scheduler classes
            foliaGlobalRegionSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            foliaRegionSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            foliaEntitySchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
            
            // Get the global region scheduler instance
            globalRegionScheduler = Bukkit.getServer().getClass()
                .getMethod("getGlobalRegionScheduler")
                .invoke(Bukkit.getServer());
            
            logger.info("✅ Folia scheduler support initialized successfully");
            
        } catch (Exception e) {
            logger.warning("❌ Failed to initialize Folia support: " + e.getMessage());
            // Fallback to Bukkit scheduler
        }
    }
    
    // ============= UNIFIED SCHEDULING API =============
    
    /**
     * Run a task on the main/global thread.
     * - Folia: Uses GlobalRegionScheduler
     * - Others: Uses BukkitScheduler main thread
     */
    public ScheduledTask runTask(Runnable task) {
        if (supportsFolia) {
            return runFoliaGlobalTask(task, 1);
        } else {
            BukkitTask bukkitTask = Bukkit.getScheduler().runTask(plugin, task);
            return new BukkitScheduledTask(bukkitTask);
        }
    }
    
    /**
     * Run a delayed task on the main/global thread.
     */
    public ScheduledTask runTaskLater(Runnable task, long delayTicks) {
        if (supportsFolia) {
            return runFoliaGlobalTask(task, delayTicks);
        } else {
            BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
            return new BukkitScheduledTask(bukkitTask);
        }
    }
    
    /**
     * Run a repeating task on the main/global thread.
     */
    public ScheduledTask runTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        if (supportsFolia) {
            return runFoliaGlobalRepeatingTask(task, delayTicks, periodTicks);
        } else {
            BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
            return new BukkitScheduledTask(bukkitTask);
        }
    }
    
    /**
     * Run a task asynchronously.
     * This works the same on all platforms.
     */
    public ScheduledTask runTaskAsync(Runnable task) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        return new BukkitScheduledTask(bukkitTask);
    }
    
    /**
     * Run a delayed async task.
     */
    public ScheduledTask runTaskLaterAsync(Runnable task, long delayTicks) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
        return new BukkitScheduledTask(bukkitTask);
    }
    
    /**
     * Run a repeating async task.
     */
    public ScheduledTask runTaskTimerAsync(Runnable task, long delayTicks, long periodTicks) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        return new BukkitScheduledTask(bukkitTask);
    }
    
    /**
     * Run a task in a specific region (Folia) or on main thread (others).
     * For non-Folia servers, this behaves like runTask().
     */
    public ScheduledTask runTaskInRegion(Location location, Runnable task) {
        if (supportsFolia) {
            return runFoliaRegionTask(location, task, 1);
        } else {
            return runTask(task);
        }
    }
    
    /**
     * Run a task for a specific entity (Folia) or on main thread (others).
     */
    public ScheduledTask runTaskForEntity(Entity entity, Runnable task) {
        if (supportsFolia) {
            return runFoliaEntityTask(entity, task, 1);
        } else {
            return runTask(task);
        }
    }
    
    /**
     * Schedule a task to run at the next tick.
     * More efficient than runTaskLater(task, 1) on some platforms.
     */
    public ScheduledTask runTaskNextTick(Runnable task) {
        return runTaskLater(task, 1);
    }
    
    // ============= FOLIA-SPECIFIC IMPLEMENTATIONS =============
    
    private ScheduledTask runFoliaGlobalTask(Runnable task, long delayTicks) {
        try {
            Object scheduledTask = globalRegionScheduler.getClass()
                .getMethod("run", plugin.getClass(), java.util.function.Consumer.class)
                .invoke(globalRegionScheduler, plugin, (java.util.function.Consumer<Object>) (ignored) -> task.run());
            
            return new FoliaScheduledTask(scheduledTask);
        } catch (Exception e) {
            logger.warning("Failed to schedule Folia global task: " + e.getMessage());
            // Fallback to Bukkit scheduler
            BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
            return new BukkitScheduledTask(bukkitTask);
        }
    }
    
    private ScheduledTask runFoliaGlobalRepeatingTask(Runnable task, long delayTicks, long periodTicks) {
        try {
            Object scheduledTask = globalRegionScheduler.getClass()
                .getMethod("runAtFixedRate", plugin.getClass(), java.util.function.Consumer.class, 
                          long.class, long.class, TimeUnit.class)
                .invoke(globalRegionScheduler, plugin, 
                       (java.util.function.Consumer<Object>) (ignored) -> task.run(),
                       delayTicks * 50L, // Convert ticks to milliseconds
                       periodTicks * 50L,
                       TimeUnit.MILLISECONDS);
            
            return new FoliaScheduledTask(scheduledTask);
        } catch (Exception e) {
            logger.warning("Failed to schedule Folia repeating task: " + e.getMessage());
            BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
            return new BukkitScheduledTask(bukkitTask);
        }
    }
    
    private ScheduledTask runFoliaRegionTask(Location location, Runnable task, long delayTicks) {
        try {
            Object regionScheduler = Bukkit.getServer().getClass()
                .getMethod("getRegionScheduler")
                .invoke(Bukkit.getServer());
            
            Object scheduledTask = regionScheduler.getClass()
                .getMethod("run", plugin.getClass(), location.getWorld().getClass(), 
                          int.class, int.class, Runnable.class)
                .invoke(regionScheduler, plugin, location.getWorld(), 
                       location.getBlockX() >> 4, location.getBlockZ() >> 4, task);
            
            return new FoliaScheduledTask(scheduledTask);
        } catch (Exception e) {
            logger.warning("Failed to schedule Folia region task: " + e.getMessage());
            return runTask(task);
        }
    }
    
    private ScheduledTask runFoliaEntityTask(Entity entity, Runnable task, long delayTicks) {
        try {
            Object scheduledTask = entity.getClass()
                .getMethod("getScheduler")
                .invoke(entity);
            
            scheduledTask.getClass()
                .getMethod("run", plugin.getClass(), Runnable.class, 
                          Runnable.class)
                .invoke(scheduledTask, plugin, task, null);
            
            return new FoliaScheduledTask(scheduledTask);
        } catch (Exception e) {
            logger.warning("Failed to schedule Folia entity task: " + e.getMessage());
            return runTask(task);
        }
    }
    
    // ============= UTILITY METHODS =============
    
    /**
     * Get the current platform type.
     */
    public PlatformType getPlatformType() {
        return platformType;
    }
    
    /**
     * Check if the platform supports Folia's regionized threading.
     */
    public boolean supportsFolia() {
        return supportsFolia;
    }
    
    /**
     * Check if the platform supports async chunk operations.
     */
    public boolean supportsAsyncChunks() {
        return supportsAsyncChunks;
    }
    
    /**
     * Check if we're running on the main thread.
     * On Folia, this checks if we're on any region thread.
     */
    public boolean isMainThread() {
        if (supportsFolia) {
            try {
                return (Boolean) Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
                    .getMethod("isGlobalTickThread")
                    .invoke(null);
            } catch (Exception e) {
                return Bukkit.isPrimaryThread();
            }
        } else {
            return Bukkit.isPrimaryThread();
        }
    }
    
    /**
     * Cancel all tasks created by this scheduler.
     */
    public void cancelAllTasks() {
        // Bukkit/Spigot/Paper tasks are automatically cancelled when the plugin is disabled
        // Folia tasks need to be tracked separately if we want to cancel them manually
        if (!supportsFolia) {
            Bukkit.getScheduler().cancelTasks(plugin);
        }
        // For Folia, individual task cancellation would need to be implemented
    }
    
    // ============= INNER CLASSES =============
    
    /**
     * Platform types enumeration.
     */
    public enum PlatformType {
        BUKKIT("Bukkit"),
        SPIGOT("Spigot"),
        PAPER("Paper"),
        PURPUR("Purpur"),
        FOLIA("Folia");
        
        private final String displayName;
        
        PlatformType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Universal scheduled task interface.
     */
    public interface ScheduledTask {
        void cancel();
        boolean isCancelled();
        int getTaskId();
    }
    
    /**
     * Bukkit task wrapper.
     */
    private static class BukkitScheduledTask implements ScheduledTask {
        private final BukkitTask bukkitTask;
        
        public BukkitScheduledTask(BukkitTask bukkitTask) {
            this.bukkitTask = bukkitTask;
        }
        
        @Override
        public void cancel() {
            bukkitTask.cancel();
        }
        
        @Override
        public boolean isCancelled() {
            return bukkitTask.isCancelled();
        }
        
        @Override
        public int getTaskId() {
            return bukkitTask.getTaskId();
        }
    }
    
    /**
     * Folia task wrapper.
     */
    private static class FoliaScheduledTask implements ScheduledTask {
        private final Object foliaTask;
        private boolean cancelled = false;
        
        public FoliaScheduledTask(Object foliaTask) {
            this.foliaTask = foliaTask;
        }
        
        @Override
        public void cancel() {
            try {
                foliaTask.getClass().getMethod("cancel").invoke(foliaTask);
                cancelled = true;
            } catch (Exception e) {
                // Ignore cancellation errors
            }
        }
        
        @Override
        public boolean isCancelled() {
            try {
                return (Boolean) foliaTask.getClass().getMethod("isCancelled").invoke(foliaTask);
            } catch (Exception e) {
                return cancelled;
            }
        }
        
        @Override
        public int getTaskId() {
            // Folia doesn't use integer task IDs like Bukkit
            return -1;
        }
    }
}