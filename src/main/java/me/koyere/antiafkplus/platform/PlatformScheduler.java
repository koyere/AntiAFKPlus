package me.koyere.antiafkplus.platform;

import me.koyere.antiafkplus.AntiAFKPlus;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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
    
    // Fallback executor for Folia when reflection fails
    private ScheduledExecutorService foliaFallbackExecutor;
    
    public PlatformScheduler(AntiAFKPlus plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.platformType = detectPlatform();
        this.supportsFolia = platformType == PlatformType.FOLIA;
        this.supportsAsyncChunks = detectAsyncChunkSupport();
        
        if (supportsFolia) {
            initializeFoliaSupport();
        }
        
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
        // Always create fallback executor first as safety net
        foliaFallbackExecutor = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "AntiAFKPlus-Folia-Fallback");
            t.setDaemon(true);
            return t;
        });
        
        try {
            // Load Folia scheduler classes
            foliaGlobalRegionSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            foliaRegionSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            foliaEntitySchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
            
            // Get the global region scheduler instance
            globalRegionScheduler = Bukkit.getServer().getClass()
                .getMethod("getGlobalRegionScheduler")
                .invoke(Bukkit.getServer());
            
            logger.info("✅ Folia support initialized successfully");
            
        } catch (Exception e) {
            logger.warning("❌ Failed to initialize Folia support, using fallback executor: " + e.getMessage());
            // Fallback executor already created above
            globalRegionScheduler = null; // Ensure we use fallback
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
        Runnable guardedTask = wrapWithPauseGuard(task);
        if (supportsFolia) {
            return runFoliaGlobalRepeatingTask(guardedTask, delayTicks, periodTicks);
        } else {
            BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, guardedTask, delayTicks, periodTicks);
            return new BukkitScheduledTask(bukkitTask);
        }
    }
    
    /**
     * Run a task asynchronously.
     * On Folia, this wraps the task in async execution on the global scheduler.
     */
    public ScheduledTask runTaskAsync(Runnable task) {
        if (supportsFolia) {
            // For Folia, wrap in async execution and run on global scheduler
            Runnable asyncWrapper = () -> java.util.concurrent.CompletableFuture.runAsync(task);
            return runFoliaGlobalTask(asyncWrapper, 1);
        } else {
            BukkitTask bukkitTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
            return new BukkitScheduledTask(bukkitTask);
        }
    }
    
    /**
     * Run a delayed async task.
     * On Folia, this wraps the task in async execution on the global scheduler.
     */
    public ScheduledTask runTaskLaterAsync(Runnable task, long delayTicks) {
        if (supportsFolia) {
            // For Folia, wrap in async execution and run on global scheduler
            Runnable asyncWrapper = () -> java.util.concurrent.CompletableFuture.runAsync(task);
            return runFoliaGlobalTask(asyncWrapper, delayTicks);
        } else {
            BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
            return new BukkitScheduledTask(bukkitTask);
        }
    }
    
    /**
     * Run a repeating async task.
     * On Folia, this uses the global scheduler with async handling.
     */
    public ScheduledTask runTaskTimerAsync(Runnable task, long delayTicks, long periodTicks) {
        Runnable guardedTask = wrapWithPauseGuard(task);
        if (supportsFolia) {
            // Folia doesn't support traditional async timers, use global scheduler with async wrapper
            return runFoliaAsyncTimer(guardedTask, delayTicks, periodTicks);
        } else {
            BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, guardedTask, delayTicks, periodTicks);
            return new BukkitScheduledTask(bukkitTask);
        }
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
            // For single execution tasks, use runDelayed if delay > 1, otherwise use run
            if (delayTicks > 1) {
                Object scheduledTask = globalRegionScheduler.getClass()
                    .getMethod("runDelayed", 
                              org.bukkit.plugin.Plugin.class, 
                              java.util.function.Consumer.class, 
                              long.class)
                    .invoke(globalRegionScheduler, plugin, 
                           (java.util.function.Consumer<Object>) (scheduledTaskObj) -> task.run(),
                           delayTicks); // Folia uses ticks directly
                
                return new FoliaScheduledTask(scheduledTask);
            } else {
                // For immediate execution, use run method
                Object scheduledTask = globalRegionScheduler.getClass()
                    .getMethod("run", 
                              org.bukkit.plugin.Plugin.class, 
                              java.util.function.Consumer.class)
                    .invoke(globalRegionScheduler, plugin, 
                           (java.util.function.Consumer<Object>) (scheduledTaskObj) -> task.run());
                
                return new FoliaScheduledTask(scheduledTask);
            }
        } catch (Exception e) {
            logger.warning("Failed to schedule Folia global task: " + e.getMessage());
            // Use fallback executor (NO Bukkit scheduler in Folia)
            if (foliaFallbackExecutor != null) {
                ScheduledFuture<?> future = foliaFallbackExecutor.schedule(task, delayTicks * 50L, TimeUnit.MILLISECONDS);
                return new JavaScheduledTask(future);
            } else {
                throw new RuntimeException("Cannot schedule task on Folia without working scheduler");
            }
        }
    }
    
    private ScheduledTask runFoliaGlobalRepeatingTask(Runnable task, long delayTicks, long periodTicks) {
        try {
            // Use correct Folia API signature: runAtFixedRate(Plugin, Consumer<ScheduledTask>, long, long)
            Object scheduledTask = globalRegionScheduler.getClass()
                .getMethod("runAtFixedRate", 
                          org.bukkit.plugin.Plugin.class, 
                          java.util.function.Consumer.class, 
                          long.class, 
                          long.class)
                .invoke(globalRegionScheduler, plugin, 
                       (java.util.function.Consumer<Object>) (scheduledTaskObj) -> task.run(),
                       delayTicks, // Folia uses ticks directly, NO conversion needed
                       periodTicks); // Folia uses ticks directly, NO conversion needed
            
            return new FoliaScheduledTask(scheduledTask);
        } catch (Exception e) {
            logger.warning("Failed to schedule Folia repeating task: " + e.getMessage());
            // Use fallback executor (NO Bukkit scheduler in Folia)
            if (foliaFallbackExecutor != null) {
                ScheduledFuture<?> future = foliaFallbackExecutor.scheduleAtFixedRate(task, 
                    delayTicks * 50L, periodTicks * 50L, TimeUnit.MILLISECONDS);
                return new JavaScheduledTask(future);
            } else {
                throw new RuntimeException("Cannot schedule repeating task on Folia without working scheduler");
            }
        }
    }
    
    private ScheduledTask runFoliaRegionTask(Location location, Runnable task, long delayTicks) {
        try {
            Object regionScheduler = Bukkit.getServer().getClass()
                .getMethod("getRegionScheduler")
                .invoke(Bukkit.getServer());
            
            // Use correct Folia RegionScheduler API signature
            Object scheduledTask = regionScheduler.getClass()
                .getMethod("run", 
                          org.bukkit.plugin.Plugin.class, 
                          org.bukkit.World.class, 
                          int.class, int.class, 
                          java.util.function.Consumer.class)
                .invoke(regionScheduler, plugin, location.getWorld(), 
                       location.getBlockX() >> 4, location.getBlockZ() >> 4,
                       (java.util.function.Consumer<Object>) (scheduledTaskObj) -> task.run());
            
            return new FoliaScheduledTask(scheduledTask);
        } catch (Exception e) {
            logger.warning("Failed to schedule Folia region task: " + e.getMessage());
            return runTask(task);
        }
    }
    
    private ScheduledTask runFoliaEntityTask(Entity entity, Runnable task, long delayTicks) {
        try {
            Object entityScheduler = entity.getClass()
                .getMethod("getScheduler")
                .invoke(entity);
            
            // Use correct Folia EntityScheduler API signature
            Object scheduledTask = entityScheduler.getClass()
                .getMethod("run", 
                          org.bukkit.plugin.Plugin.class, 
                          java.util.function.Consumer.class,
                          Runnable.class)
                .invoke(entityScheduler, plugin, 
                       (java.util.function.Consumer<Object>) (scheduledTaskObj) -> task.run(),
                       null); // retired callback
            
            return new FoliaScheduledTask(scheduledTask);
        } catch (Exception e) {
            logger.warning("Failed to schedule Folia entity task: " + e.getMessage());
            return runTask(task);
        }
    }
    
    /**
     * Handle async timer tasks for Folia by wrapping them in async execution.
     */
    private ScheduledTask runFoliaAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
        try {
            // For Folia, we schedule on the global region and wrap the task in async execution
            Runnable asyncWrapper = () -> {
                // Execute the task asynchronously using Java's CompletableFuture
                java.util.concurrent.CompletableFuture.runAsync(task);
            };
            
            // Use the global region scheduler for the timer
            return runFoliaGlobalRepeatingTask(asyncWrapper, delayTicks, periodTicks);
            
        } catch (Exception e) {
            logger.warning("Failed to schedule Folia async timer: " + e.getMessage());
            // Fallback: try to use a simple global repeating task without async wrapper
            return runFoliaGlobalRepeatingTask(task, delayTicks, periodTicks);
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
        if (!supportsFolia) {
            Bukkit.getScheduler().cancelTasks(plugin);
        } else {
            // Shutdown Folia fallback executor if it exists
            if (foliaFallbackExecutor != null && !foliaFallbackExecutor.isShutdown()) {
                foliaFallbackExecutor.shutdown();
            }
        }
        // For Folia native tasks, individual task cancellation would need to be implemented
    }

    /**
     * Gracefully shutdown scheduler resources (Folia fallback executor, etc.).
     * Should be called from plugin onDisable().
     */
    public void shutdown() {
        cancelAllTasks();
    }

    private Runnable wrapWithPauseGuard(Runnable original) {
        if (original == null) {
            return () -> {};
        }
        return () -> {
            if (ServerStateUtil.isServerPaused()) {
                return;
            }
            original.run();
        };
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
    
    /**
     * Java ScheduledFuture task wrapper for Folia fallback.
     */
    private static class JavaScheduledTask implements ScheduledTask {
        private final ScheduledFuture<?> future;
        
        public JavaScheduledTask(ScheduledFuture<?> future) {
            this.future = future;
        }
        
        @Override
        public void cancel() {
            future.cancel(false);
        }
        
        @Override
        public boolean isCancelled() {
            return future.isCancelled();
        }
        
        @Override
        public int getTaskId() {
            // Java futures don't have task IDs
            return -2;
        }
    }
}
