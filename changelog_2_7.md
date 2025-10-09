# AntiAFKPlus v2.7 — Critical Bug Fixes

Release type: Bug Fix Release
Compatibility: Minecraft 1.16 – 1.21.8+ | Java 17+

---

## What's Fixed

### 🔧 Credit System Teleportation (CRITICAL FIX)

**Problem**: AFK zone teleportation worked only 1-2 times, then stopped working. Players would see AFK warnings but never get teleported.

**Solution**: Fixed credit system logic to properly teleport players when credits are exhausted.

**What this means for you**:
- ✅ AFK zone teleportation now works **consistently every time**
- ✅ Credits consume properly minute-by-minute while AFK
- ✅ Players are **automatically teleported** when credits reach zero
- ✅ No more "stuck" players who see warnings but never get moved

---

### 🌍 Disabled Worlds Cleanup (CRITICAL FIX)

**Problem**: Players still received AFK warnings in disabled worlds (like `luna-afk-1`), even when those worlds were added to `disabled-worlds` configuration.

**Solution**: Plugin now completely clears AFK state when players enter disabled worlds.

**What this means for you**:
- ✅ **No more AFK warnings** in disabled worlds
- ✅ AFK state is automatically cleared when entering disabled worlds
- ✅ Works correctly with both `disabled-worlds` and `enabled-worlds` configuration
- ✅ Players can safely AFK in designated worlds without interference

---

### 🐛 Pattern Detection Errors (TECHNICAL FIX)

**Problem**: Server logs showed `ConcurrentModificationException` errors during pattern analysis.

**Solution**: Implemented thread-safe pattern detection across all detection methods.

**What this means for you**:
- ✅ **Clean server logs** without errors
- ✅ Pattern detection (water circles, large pools, confined spaces) works flawlessly
- ✅ Better server stability and performance
- ✅ No more error spam in console

---

### ⚙️ Credit System Isolation (ENHANCEMENT)

**Problem**: Credit system could potentially interfere with standard AFK detection even when disabled.

**Solution**: Complete isolation of credit system when disabled in configuration.

**What this means for you**:
- ✅ **Zero performance overhead** when credit system is disabled
- ✅ Standard AFK detection works perfectly without credit system
- ✅ Full backwards compatibility with previous configurations
- ✅ Better resource efficiency

---

## Configuration Examples

### Fix #1: Enable AFK Zone Teleportation with Credits
```yaml
modules:
  credit-system:
    enabled: true

credit-system:
  enabled: true
  afk-zone:
    enabled: true
    world: "world"
    location: "0,100,0"
```

### Fix #2: Disable AFK Detection in Specific Worlds
```yaml
disabled-worlds:
  - "luna-afk-1"
  - "creative-world"
  - "lobby"
```

---

## Upgrade Instructions

1. **Backup** your current `config.yml` and `messages.yml`
2. **Replace** old plugin JAR with AntiAFKPlus v2.7
3. **Restart** your server
4. **Test** AFK teleportation and disabled worlds functionality

**No configuration changes required** - all fixes are automatic!

---

## For Server Owners

**Critical Fixes**:
- AFK zone teleportation now reliable and consistent
- Disabled worlds work as expected (no more false warnings)
- Eliminated concurrent modification errors in logs
- Credit system properly isolated when disabled

**Performance**:
- Thread-safe operations throughout pattern detection
- Reduced resource usage when credit system is disabled
- Cleaner logs and better stability

**Compatibility**:
- 100% backwards compatible with v2.6 and v2.5 configurations
- No breaking changes
- All existing features continue to work normally

---

## Need Help?

- 📖 **In-Game**: `/afkplus help` and `/afkplus status`
- 💬 **Discord**: https://discord.gg/xKUjn3EJzR
- 🐛 **Bug Reports**: GitHub Issues
- 📚 **Documentation**: GitHub Wiki

---

**Version**: 2.7
**Release Date**: October 7, 2025
**Type**: Critical Bug Fix Release
