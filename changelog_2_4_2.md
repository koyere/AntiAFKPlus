# AntiAFKPlus v2.4.2 Changelog

## 🎯 Complete Folia 1.21.8 Compatibility

### Problem Resolved
**Issue**: The plugin failed to initialize on Folia 1.21.8 with `UnsupportedOperationException` (due to BukkitScheduler usage) and reflection errors in `runAtFixedRate`.

**Root Cause**: PlatformScheduler was using incorrect method signatures for Folia's `GlobalRegionScheduler` API, causing reflection failures during task scheduling.

**Error Stack**: 
```
Failed to schedule Folia repeating task: io.papermc.paper.threadedregions.scheduler.FoliaGlobalRegionScheduler.runAtFixedRate
Module system initialization failed: Cannot schedule repeating task on Folia without working scheduler
```

### Technical Solution Implemented

#### 🔧 Correct Folia Method Signatures
- Before: `runAtFixedRate(plugin.getClass(), Consumer.class, long.class, long.class, TimeUnit.class)`
- After: `runAtFixedRate(Plugin.class, Consumer.class, long.class, long.class)`
- Key: no `TimeUnit` parameter and timing in ticks directly (no ms conversion)

#### 🛡️ Stronger Fallback
- Always create a `ScheduledExecutorService` fallback before reflection
- Graceful degradation if native Folia API fails
- Dedicated daemon thread pool for fallback operations

#### ⚡ Additional API Fixes
- Adjusted `runDelayed` and `run` signatures for single-execution tasks
- Corrected `RegionScheduler` and `EntityScheduler` parameter types
- Consistent use of `Consumer<ScheduledTask>`

#### 🔁 Task Migration to PlatformScheduler (Folia-safe)
- Replaced `BukkitScheduler/BukkitRunnable` with `PlatformScheduler` where applicable:
  - `AFKManager`: periodic AFK check loop → `runTaskTimer(...)`
  - `PatternDetector`: periodic analysis → `runTaskTimerAsync(...)`
  - `AntiAFKActivityDetector`: cleanup and analysis → `runTaskTimerAsync(...)`; kicks/events via `runTaskForEntity(...)`
  - `MovementListener` (async chat): sync with `runTaskForEntity(player, ...)`
  - `AutoClickListener`: cleanup → `runTaskTimerAsync(...)`; kick via `runTaskForEntity(...)`

#### 🧹 Clean Plugin Shutdown
- Added `PlatformScheduler.shutdown()` and call it in `onDisable()` to close the fallback executor and avoid locked JARs on Windows

### What’s Fixed
- ✅ Full Folia 1.21.8 compatibility: initializes and runs correctly
- ✅ No `UnsupportedOperationException`: removed BukkitScheduler usage in Folia context
- ✅ Timers and tasks migrated to `PlatformScheduler`: pattern analysis, detectors and checks operational
- ✅ Commands and AFK detection working
- ✅ Zero impact on other servers: Paper, Spigot, Bukkit, Purpur unchanged

### Compatibility
- Paper/Spigot/Bukkit: ✅ unchanged, functionality preserved
- Purpur: ✅ compatible via Paper base
- Folia 1.21.8+: ✅ complete native support with correct API usage

### Technical Details
**Files Modified**:
- `platform/PlatformScheduler.java`: Folia signatures, stronger fallback and `shutdown()`
- `AntiAFKPlus.java`: call `platformScheduler.shutdown()` in `onDisable()`
- `afk/AFKManager.java`: timer → `PlatformScheduler.runTaskTimer(...)`
- `afk/PatternDetector.java`: timer → `runTaskTimerAsync(...)`; actions → `runTaskForEntity(...)`
- `afk/AntiAFKActivityDetector.java`: timers → `runTaskTimerAsync(...)`; events/kick → `runTaskForEntity(...)`
- `afk/MovementListener.java`: async chat → `runTaskForEntity(...)`
- `listener/AutoClickListener.java`: cleanup → `runTaskTimerAsync(...)`; kick → `runTaskForEntity(...)`
- `plugin.yml`: keep `folia-supported: true`

**API Compliance**:
- Native `GlobalRegionScheduler` integration via reflection
- Proper `Consumer<ScheduledTask>` parameter usage
- Tick-based timing (no `TimeUnit` conversions)
- Robust fallback with `ScheduledExecutorService`

---

**Version**: 2.4.2  
**Release Date**: 2025-09-09  
**Compatibility**: Minecraft 1.16 - 1.21.8  
**Java**: 17+

### Upgrade Instructions
1. Download AntiAFKPlus v2.4.2
2. Replace the old JAR in `/plugins`
3. Restart the server
4. No configuration changes required
5. On Folia, verify the log: “✅ Folia support initialized successfully”

### Testing Validation
- ✅ Folia 1.21.8: initializes without errors (real user log)
- ✅ Paper/Spigot: functionality preserved
- ✅ Performance: timers operational without exceptions
- ✅ AFK Detection: full feature set working across platforms

### Future Notes (optional)
- Potential refactor: per-player AFK check executed per entity/region to align 100% with Folia threading model.

**Professional-grade Folia compatibility achieved with zero regression on existing platforms!**
