# AntiAFKPlus v2.8.1 - Critical Threading Fix

**Release Date:** 2025-10-20
**Type:** Bugfix Release
**Compatibility:** Paper 1.21.8, Spigot, Folia, Purpur (MC 1.16-1.21.8)

---

## 🔧 Critical Fixes

### Pattern Detection Event Threading (Paper 1.21.8 Compatibility)

**Issue:** Pattern detection events were being fired asynchronously, causing `IllegalStateException` on Paper 1.21.8 and preventing external API listeners from receiving events.

**Fixed:**
- ✅ All Bukkit events now fire synchronously on main thread (Paper 1.21.8 requirement)
- ✅ Eliminated async entity access (`player.getLocation()` now called in main thread)
- ✅ Converted internal data structures to thread-safe collections (ConcurrentHashMap/ConcurrentLinkedDeque)
- ✅ Implemented atomic operations for violation counters to prevent race conditions
- ✅ Added proper player disconnect handling during async analysis

**Impact:**
- External plugins using `api.registerPatternDetectionListener()` now receive events correctly
- Zero threading exceptions in Paper 1.21.8+
- Maintained performance (heavy analysis still runs asynchronously)

**Files Changed:**
- `PatternDetector.java` - Complete threading model refactor

---

## 📊 Performance

- ✅ **Zero performance regression** - Pattern analysis remains asynchronous
- ✅ **Thread-safe operations** - All concurrent access properly synchronized
- ✅ **Folia compatible** - Uses `runTaskForEntity` for player-specific operations

---

## 🔗 API Compatibility

**Fully compatible with v2.8 API** - No breaking changes for developers.

**Developers can now safely use:**
```java
AntiAFKPlusAPI api = AntiAFKPlusAPI.getInstance();
api.registerPatternDetectionListener(event -> {
    // This now works correctly on Paper 1.21.8+
    System.out.println("Pattern detected: " + event.getPattern().getType());
});
```

---

## 📦 Installation

**Maven:**
```xml
<dependency>
    <groupId>com.github.koyere</groupId>
    <artifactId>AntiAFKPlus</artifactId>
    <version>2.8.1</version>
    <scope>provided</scope>
</dependency>
```

**Gradle:**
```groovy
compileOnly 'com.github.koyere:AntiAFKPlus:2.8.1'
```

---

## 🐛 Known Issues

None reported in this release.

---

## 📝 Notes for Server Admins

- **Direct upgrade from v2.8** - No configuration changes required
- **Drop-in replacement** - Simply replace the JAR file and restart
- **No data migration needed**

---

## 🙏 Credits

Special thanks to the developer who reported the threading issue and helped validate the fix.

---

**Full Changelog:** [View on GitHub](https://github.com/koyere/AntiAFKPlus/blob/main/CHANGELOG.md)
**Download:** [GitHub Releases](https://github.com/koyere/AntiAFKPlus/releases/tag/2.8.1)
