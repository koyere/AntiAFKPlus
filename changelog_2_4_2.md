# AntiAFKPlus v2.4.2 Changelog

## 🎯 Folia 1.21.8 Complete Compatibility Fix

### Problem Resolved
**Issue**: Plugin failed to initialize on Folia 1.21.8 servers with `UnsupportedOperationException` and `runAtFixedRate` method signature errors.

**Root Cause**: PlatformScheduler was using incorrect method signatures for Folia's `GlobalRegionScheduler` API, causing reflection failures during task scheduling.

**Error Stack**: 
```
Failed to schedule Folia repeating task: io.papermc.paper.threadedregions.scheduler.FoliaGlobalRegionScheduler.runAtFixedRate
Module system initialization failed: Cannot schedule repeating task on Folia without working scheduler
```

### Technical Solution Implemented

#### 🔧 **Fixed Method Signatures**
- **Before**: `runAtFixedRate(plugin.getClass(), Consumer.class, long.class, long.class, TimeUnit.class)`
- **After**: `runAtFixedRate(Plugin.class, Consumer.class, long.class, long.class)`
- **Key Fix**: Removed `TimeUnit.class` parameter (doesn't exist in Folia API)
- **Ticks Handling**: Use ticks directly instead of millisecond conversion

#### 🛡️ **Enhanced Fallback System**
- **Safety Net**: Fallback executor now always initialized before reflection attempts
- **Error Resilience**: Graceful degradation when native Folia API fails
- **Thread Management**: Dedicated daemon thread pool for Folia fallback operations

#### ⚡ **Additional API Corrections**
- Fixed `runDelayed` and `run` method signatures for single-execution tasks
- Corrected `RegionScheduler` and `EntityScheduler` parameter types
- Updated Consumer parameter to use proper `Consumer<ScheduledTask>` type

### What's Fixed
- ✅ **Complete Folia 1.21.8 compatibility** - Plugin initializes and runs perfectly
- ✅ **No startup crashes** - Eliminated all `UnsupportedOperationException` errors
- ✅ **PerformanceOptimizer working** - Cache cleanup and monitoring tasks now function
- ✅ **All AFK features operational** - Detection, commands, and configuration work normally
- ✅ **Zero impact on other servers** - Paper, Spigot, Bukkit, Purpur remain unchanged

### Platform Compatibility Matrix
| Server Type | Status | Notes |
|-------------|---------|-------|
| Paper/Spigot/Bukkit | ✅ Perfect | No changes, existing functionality preserved |
| Purpur | ✅ Perfect | Compatible through Paper base |
| Folia 1.21.8+ | ✅ Perfect | **NEW**: Complete native support with proper API usage |

### Technical Details
**Files Modified**:
- `PlatformScheduler.java`: Corrected all Folia scheduler method signatures
- `plugin.yml`: Maintained `folia-supported: true` declaration

**API Compliance**:
- Native Folia `GlobalRegionScheduler` integration
- Proper `Consumer<ScheduledTask>` parameter usage
- Correct tick-based timing (no TimeUnit conversion)
- Robust fallback to Java `ScheduledExecutorService`

---

**Version**: 2.4.2  
**Release Date**: 2025-09-09  
**Compatibility**: Minecraft 1.16 - 1.21.8  
**Java**: 17+

### Upgrade Instructions
1. Download AntiAFKPlus v2.4.2
2. Replace the old JAR file in your `/plugins` folder
3. Restart your server
4. **No configuration changes required**
5. Verify startup logs show "✅ Folia support initialized successfully" on Folia servers

### Testing Validation
- ✅ **Folia 1.21.8**: Complete initialization without errors
- ✅ **Paper/Spigot**: Existing functionality preserved 
- ✅ **Performance**: All caching and optimization tasks operational
- ✅ **AFK Detection**: Full feature set working across all platforms

**Professional-grade Folia compatibility achieved with zero regression on existing platforms!**