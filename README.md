# AntiAFKPlus v2.8

**Professional-grade AFK detection and management system with AFK Credit System and Server Transfer**

✅ **Minecraft 1.16 – 1.21.8** | ✅ **Folia/Paper/Spigot/Bukkit/Purpur** | ✅ **Java & Bedrock Edition** | ✅ **Zero-lag Performance**

---

## 🚀 Enterprise Features

### 🏗️ **Modular Architecture**
- **Enable/disable modules** independently via configuration
- **Core modules**: Detection, Events, API, Commands, Credit System
- **Feature modules**: Pattern Detection, Autoclick Detection, Player Protection, AFK Zones, Rewards, Large Pool Detection
- **Integration modules**: WorldGuard, PlaceholderAPI, Vault, DiscordSRV, Floodgate, bStats

### 🌐 **Multi-Platform Support**
- **Auto-detection** of Folia, Paper, Spigot, Bukkit, Purpur
- **Automatic fallback** for maximum compatibility
- **Bedrock Edition support** via Floodgate/Geyser integration

### 🌍 **Complete Internationalization**
- **20+ languages** supported out of the box
- **Per-player language** detection and customization
- **Fully translatable** messages and UI elements

### ⚡ **Zero-Lag Performance**
- **Adaptive intervals** based on server load
- **Object pooling** and intelligent caching
- **Player activity categorization** for optimization
- **Real-time performance monitoring**

### 🤖 **Advanced Detection**
- **Pattern recognition** for bot detection (circular movements, confined spaces, large AFK pools)
- **Behavioral analysis** with activity scoring and keystroke timeout detection
- **Autoclick/macro detection** with configurable thresholds
- **Multi-activity tracking** (movement, rotation, jumping, chat, commands, interactions)
- **Large AFK pool detection** (20x10+ block water pools with current-based movement)
- **Manual vs automatic movement** analysis with velocity variance tracking

---

## 📦 Installation & Setup

1. **Download** the latest version
2. **Place** `AntiAFKPlus.jar` in your `/plugins` folder
3. **Restart** your server
4. **Configure** modules in `config.yml`
5. **Customize** messages in `messages.yml`

---

## 🎛️ Commands & Permissions

### Core Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/afk` | Toggle AFK mode manually | `antiafkplus.afk` |
| `/afk list` | View all AFK players | `antiafkplus.list` |
| `/afkplus reload` | Reload configuration | `antiafkplus.admin` |
| `/afkplus status` | View plugin status | `antiafkplus.admin` |

### Credit System Commands (v2.5)
| Command | Description | Permission |
|---------|-------------|------------|
| `/afkback` | Return from AFK zone to original location | `antiafkplus.credit.return` |
| `/afkreturn` | Alias for /afkback | `antiafkplus.credit.return` |
| `/afkcredits` | Check AFK credit balance and info | `antiafkplus.credit.check` |
| `/afkcredits history <player> [limit]` | Show recent credit transactions (SQL backend only) | `antiafkplus.credit.admin` |
| `/afkcredits reload` | Reload credit system config | `antiafkplus.admin.reload` |

### Core Permissions
| Permission | Description |
|------------|-------------|
| `antiafkplus.bypass` | Bypass all AFK detection |
| `antiafkplus.bypass.detection` | Bypass basic detection only |
| `antiafkplus.bypass.patterns` | Bypass pattern detection |
| `antiafkplus.time.vip` | Custom AFK time limits |
| `antiafkplus.admin` | Admin commands access |

### Credit System Permissions (v2.5)
| Permission | Description | Default |
|------------|-------------|----------|
| `antiafkplus.credit.earn` | Earn AFK credits through active play | `true` |
| `antiafkplus.credit.use` | Use AFK credits to delay teleportation | `true` |
| `antiafkplus.credit.return` | Use return commands from AFK zone | `true` |
| `antiafkplus.credit.check` | Check credit balance | `true` |
| `antiafkplus.credit.ratio.vip` | VIP credit earning ratio (4:1) | `false` |
| `antiafkplus.credit.ratio.premium` | Premium credit ratio (3:1) | `false` |
| `antiafkplus.credit.ratio.admin` | Admin credit ratio (2:1) | `op` |
| `antiafkplus.credit.admin` | Full credit system administration | `op` |

---

## ⚙️ Configuration

### Module System
Enable/disable features in `config.yml`:
```yaml
modules:
  core-detection:
    enabled: true
  pattern-detection:
    enabled: true
  autoclick-detection:
    enabled: true
  player-protection:
    enabled: true
  afk-zones:
    enabled: false
  reward-system:
    enabled: false
  credit-system:           # NEW in v2.5
    enabled: false
  visual-effects:
    enabled: false
  database:
    enabled: false
  analytics:
    enabled: false
  bedrock-compatibility:
    enabled: true
```

### Performance Optimization
```yaml
performance:
  auto-optimization: true
  adaptive-intervals: true
  max-tps-impact: 0.5
  max-memory-mb: 50
```

### Internationalization
```yaml
internationalization:
  default-language: "en"
  auto-detect-language: true
  fallback-to-default: true
```

### Enhanced Pattern Detection (v2.4+)
```yaml
pattern-detection-settings:
  # Small confined spaces
  water-circle-radius: 3.0
  repetitive-movement-threshold: 0.8
  max-pattern-violations: 3
  
  # Large AFK pools (v2.4)
  large-pool-threshold: 25.0
  keystroke-timeout-ms: 180000     # 3 minutes
  automatic-movement-velocity-threshold: 0.15
```

### AFK Credit System (v2.5)
```yaml
credit-system:
  enabled: false
  
  # Credit earning ratios (active_minutes:afk_credit_minutes)
  credit-ratios:
    default: "5:1"          # 5min active = 1min AFK credit
    vip: "4:1"             # Better ratio for VIP
    premium: "3:1"         # Best ratio for premium
  
  # Credit limits
  max-credits:
    default: 120            # 2 hours max
    vip: 180               # 3 hours for VIP
  
  # AFK zone configuration
  afk-zone:
    enabled: true
    world: "world"
    location: "0,100,0"
    
  # Return command settings
  return-command:
    enabled: true
    cooldown-seconds: 10
```

---

## 🔌 Integrations

### PlaceholderAPI
**Core Placeholders:**
- `%antiafkplus_status%` - Player AFK status
- `%antiafkplus_afktime%` - Time since last activity
- `%antiafkplus_activity_score%` - Activity score (0-100)
- `%antiafkplus_pattern_confidence%` - Pattern detection confidence

**Credit System Placeholders (v2.5):**
- `%antiafkplus_credits%` - Current credit balance in minutes
- `%antiafkplus_credits_hours%` - Credit balance in hours
- `%antiafkplus_max_credits%` - Maximum credits allowed
- `%antiafkplus_credit_ratio%` - Player's credit earning ratio
- `%antiafkplus_in_afk_zone%` - Whether player is in AFK zone
- `%antiafkplus_credits_expire_days%` - Days until credits expire (empty if not applicable)

### WorldGuard
- **Region-based AFK settings** with zone inheritance
- **Custom timeouts** per region
- **AFK restrictions** in specific areas
- **Direct integration**: resolves zones from WorldGuard regions (reflection, no compile-time dependency)

#### WorldGuard Setup (v2.5)
- Enable integration: set `modules.worldguard-integration.enabled: true` or `integrations.worldguard.enabled: true`.
- Define zones in `zone-management.zones` using keys matching WorldGuard region IDs (case-insensitive). Example:
```yaml
zone-management:
  enabled: true
  zones:
    afk:                       # WorldGuard region id: "afk"
      kick-action: "TELEPORT"
      teleport-location: "world,0,100,0"
    spawn:                     # WorldGuard region id: "spawn"
      kick-action: "TELEPORT"
      teleport-location: "world,10,100,10"
```
- Optional explicit mapping per zone: `zone-management.zones.<name>.region: <regionId>`.
- AFK teleport prefers `zones.afk.teleport-location`, then `zones.spawn.teleport-location`, else falls back to `credit-system.afk-zone`.
- AFK zone protection (credit system):
  - `credit-system.afk-zone.protection.prevent-damage`
  - `credit-system.afk-zone.protection.prevent-pvp`
  - `credit-system.afk-zone.protection.prevent-mob-spawning` (radius 10 around AFK zone location)

### Bedrock Edition (Java & Bedrock Cross-Platform)
- **Automatic detection** of Bedrock players via Floodgate/Geyser
- **UI adaptations** for touch controls and mobile interfaces
- **Menu optimizations** for mobile devices and controllers
- **Cross-platform compatibility** with all Bedrock Edition clients
- **Touch-friendly** command interfaces and feedback

---

## 🔁 Server Transfer & Scripted Sequences (v2.6)

AntiAFKPlus can transfer AFK players to another server (Bungee/Velocity via Plugin Messaging), with optional per‑second countdown and fully scripted pipelines.

### Behavior & Order
- Final AFK action order:
  1) Credit System: If credits exist, cancel the final action and consume credits; no transfer runs.
  2) Zone Management: If a zone defines `kick-action`, it takes priority (`TELEPORT`, `TRANSFER`, `KICK`, etc.).
  3) Global Server Transfer: If enabled and a `target-server` is set, the plugin uses `TRANSFER_SERVER` by default.
- If the player becomes active before completion, countdown/pipeline cancels automatically.

### Global Configuration
```yaml
server-transfer:
  enabled: true                 # Enable server transfer action
  target-server: "lobby"        # Target server name (proxy)
  proxy-channel: "auto"         # auto | bungeecord | namespaced

  # Fallback if transfer fails or no channel available
  fallback-action: "KICK"       # KICK | TELEPORT | NONE
  fallback-teleport-location: "world,0,100,0"  # Only for TELEPORT

  # Retry policy
  retry-attempts: 0
  retry-delay-ticks: 10

  # Countdown (per-second titles/subtitles/sounds)
  countdown:
    enabled: false
    seconds: 10
    title: "&cYou are AFK"
    subtitle: "&eMoving in {seconds}s"
    sound:
      enabled: true
      name: "ENTITY_EXPERIENCE_ORB_PICKUP"
      volume: 1.0
      pitch: 1.0

  # Scripted pipeline (executes steps in order)
  pipeline:
    enabled: false
  actions:
    - "TITLE: &cYou are AFK"
    - "SUBTITLE: &eMoving in {seconds}s"
    - "SOUND: ENTITY_EXPERIENCE_ORB_PICKUP,1.0,1.0"
    - "WAIT: 1s"
    - "MESSAGE: &7Transferring..."
    - "TRANSFER: lobby"
```

Notes:
- If `pipeline.enabled` and `actions` are set, the pipeline runs first.
- Otherwise, if `countdown.enabled` is true, a per-second countdown runs.
- Otherwise, the transfer executes immediately.
- No new commands or permissions are required for v2.6.

### Zone-based Transfer (WorldGuard)
```yaml
zone-management:
  enabled: true
  zones:
    spawn:
      kick-action: "TRANSFER"     # Use server transfer
      transfer-server: "lobby"    # Per-zone target (fallback to global target-server if empty)
```

### Messages
Add or customize in `messages.yml`:
```yaml
messages:
  server-transfer:
    transferring: "&7[AntiAFK+] &aTransferring you to &f{server}&a..."
    unavailable: "&7[AntiAFK+] &cServer transfer unavailable."
    failed: "&7[AntiAFK+] &cCould not transfer you."
```

### Compatibility
- BungeeCord and Velocity (with Bungee compatibility) via Plugin Messaging.
- Folia-safe using platform scheduler; no BukkitScheduler in Folia context.
- Fully backward compatible; disabled by default.

## 💻 Developer API (80+ Methods)

### 📦 Installation via Jitpack

Integrate AntiAFKPlus API into your plugin using Jitpack:

**Maven:**
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.koyere</groupId>
        <artifactId>AntiAFKPlus</artifactId>
        <version>2.8</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

**Gradle:**
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.koyere:AntiAFKPlus:2.8'
}
```

**Gradle (Kotlin DSL):**
```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.koyere:AntiAFKPlus:2.8")
}
```

📚 **[View complete API documentation →](API.md)**

### Basic Usage
```java
// Get API instance
AntiAFKPlusAPI api = AntiAFKPlusAPI.getInstance();

// Check AFK status
boolean isAfk = api.isAFK(player);
AFKStatus status = api.getAFKStatus(player);
AFKReason reason = api.getAFKReason(player);

// Activity tracking
Duration timeSinceActivity = api.getTimeSinceLastActivity(player);
PlayerActivityInfo activityInfo = api.getActivityInfo(player);
ActivityStatistics stats = api.getActivityStatistics(player);

// Pattern detection
boolean hasSuspiciousPatterns = api.hasSuspiciousPatterns(player);
List<DetectedPattern> patterns = api.getDetectedPatterns(player);
PatternAnalysis analysis = api.analyzeMovementPatterns(player);

// Performance metrics
PerformanceMetrics metrics = api.getPerformanceMetrics();
PluginInfo info = api.getPluginInfo();
```

### Credit System API (v2.5)
```java
// Credit management
long balance = api.getCreditBalance(player);
boolean hasCredits = api.hasCredits(player, 30); // 30 minutes
api.addCredits(player, 15); // Add 15 minutes
api.consumeCredits(player, 10); // Consume 10 minutes

// Credit information
String ratio = api.getCreditRatio(player); // "5:1"
long maxCredits = api.getMaxCredits(player);
List<CreditTransaction> history = api.getCreditHistory(player);
Instant expiration = api.getCreditExpiration(player);

// AFK zone management
boolean inZone = api.isInAFKZone(player);
Location zoneLocation = api.getAFKZoneLocation(player);
Location originalLoc = api.getOriginalLocation(player);
boolean returned = api.returnFromAFKZone(player);
```

### Event Handling (Comprehensive Event System)
```java
// AFK state changes
api.registerAFKStateListener(event -> {
    Player player = event.getPlayer();
    AFKStatus fromStatus = event.getFromStatus();
    AFKStatus toStatus = event.getToStatus();
    AFKReason reason = event.getReason();
    // Handle state change
});

// Pattern detection events
api.registerPatternDetectionListener(event -> {
    DetectedPattern pattern = event.getPattern();
    if (pattern.getConfidence() > 0.8) {
        // High confidence bot detection
        player.sendMessage("Suspicious pattern detected!");
    }
});

// AFK warnings and kicks
api.registerAFKWarningListener(event -> {
    // Player is about to be kicked
    int timeLeft = event.getTimeUntilKick();
});

api.registerAFKKickListener(event -> {
    // Player is being kicked/teleported
    KickAction action = event.getAction(); // KICK or TELEPORT
    event.setCancelled(true); // Cancel if needed
});

// Credit system events (v2.5)
api.registerCreditEarnedListener(event -> {
    long creditsEarned = event.getCreditsEarned();
    long totalCredits = event.getTotalCredits();
});

api.registerCreditConsumedListener(event -> {
    long creditsUsed = event.getCreditsConsumed();
    long remaining = event.getRemainingCredits();
});
```

### Async Operations & Advanced Features
```java
// Async operations
CompletableFuture<AFKStatus> statusFuture = api.getAFKStatusAsync(player);
CompletableFuture<PlayerActivityInfo> activityFuture = api.getActivityInfoAsync(player);
CompletableFuture<List<DetectedPattern>> patternsFuture = api.getDetectedPatternsAsync(player);

statusFuture.thenAccept(status -> {
    // Handle async result
    if (status == AFKStatus.AFK_WARNED) {
        // Player needs attention
    }
});

// Batch operations
Map<Player, AFKStatus> batchStatus = api.getBatchAFKStatus(players);
Map<Player, Long> batchCredits = api.getBatchCreditBalances(players);

// Statistics and analytics
AFKStatistics serverStats = api.getServerAFKStatistics();
PlayerAFKStatistics playerStats = api.getPlayerAFKStatistics(player);
BedrockPlayerInfo bedrockInfo = api.getBedrockPlayerInfo(player);

// Advanced exemptions and overrides
api.addAFKExemption(player, AFKExemption.TEMPORARY, Duration.ofMinutes(30));
api.removeAFKExemption(player, AFKExemption.TEMPORARY);
boolean hasExemption = api.hasAFKExemption(player, AFKExemption.PERMANENT);

// Zone and region management
Optional<AFKZoneInfo> zoneInfo = api.getAFKZoneAt(player.getLocation());
boolean allowedHere = api.isAFKAllowedAt(player.getLocation());
api.setAFKDetectionEnabled(player.getWorld(), false); // disable AFK detection temporarily
```

### Nuevo en la API v2.8
- Estadísticas en vivo: `getAFKStatistics`, `getPlayerStatistics`, `getPerformanceMetrics` ahora devuelven datos reales del servidor.
- Seguimiento de actividad: `getActivityInfo`, `getActivityStatistics` y `recordActivity` reflejan los eventos registrados por el núcleo.
- Gestión de zonas: `getAFKZoneAt` e `isAFKAllowedAt` resuelven la configuración de `zone-management` y WorldGuard.
- Advertencias y patrones: los listeners (`registerAFKStateListener`, `registerWarningListener`, `registerPatternDetectionListener`) reciben eventos completos con posibilidad de cancelar. 

---

## 📊 Performance & Monitoring

### Zero-Lag Architecture
- **Adaptive check intervals** based on server load (1-20 second intervals)
- **Player activity categorization** (High/Medium/Low activity players)
- **Object pooling** to minimize garbage collection
- **Intelligent caching** with TTL (Time-To-Live) management
- **Async processing** for heavy operations

### Performance Metrics & Monitoring
- **Real-time TPS impact** monitoring (target: <0.5% impact)
- **Memory usage** tracking per module (<50MB total)
- **Execution time** profiling for all operations
- **Cache hit rates** and optimization statistics
- **Database query optimization** (when database module enabled)
- **Platform-specific optimizations** (Folia thread-per-region, Paper async chunks)

### Debug Mode
Enable detailed logging:
```yaml
debug: true
performance:
  debug-logging: true
```

---

## 🔧 Troubleshooting

### Common Issues & Solutions

**Performance Issues:**
- **High CPU usage**: Increase check intervals, enable adaptive intervals, or disable heavy modules
- **Memory leaks**: Enable object pooling and cache management
- **TPS drops**: Reduce max-tps-impact in performance settings

**Detection Issues:**
- **Bedrock players not detected**: Enable Floodgate/Geyser integration
- **Pattern detection false positives**: Adjust sensitivity thresholds in pattern-detection-settings
- **Large AFK pools not detected**: Enable large-pool-threshold and keystroke-timeout detection
- **Credits not earned**: Check activity-threshold and minimum-session-minutes settings

**Platform-Specific Issues:**
- **Folia compatibility**: Ensure folia-supported: true and latest version (v2.4.2+)
- **Cross-version compatibility**: Plugin supports MC 1.16-1.21.8 automatically
- **Plugin conflicts**: Check for conflicting AFK plugins or schedulers

### Support & Resources
- 📖 **In-Game Help**: `/afkplus help` and `/afkplus status`
- 💬 **Discord Community**: https://discord.gg/xKUjn3EJzR
- 🐛 **Bug Reports**: GitHub Issues with full debug logs
- 📋 **Feature Requests**: GitHub Discussions
- 📚 **Wiki & Tutorials**: GitHub Wiki (comprehensive setup guides)
- 🔧 **Configuration Generator**: Online config builder tool

---

## 📈 Statistics & Analytics

**Anonymous Usage Statistics (bStats):**
- Server version and platform distribution
- Feature usage and module adoption rates
- Performance metrics and optimization insights
- Cross-platform compatibility statistics

**Disable statistics:** Edit `/plugins/bStats/config.yml` or set `modules.analytics.enabled: false`

**Plugin Analytics (Optional):**
- Enable `modules.analytics.enabled: true` for detailed server-specific insights
- Player activity patterns and AFK behavior analysis
- Credit system usage statistics and optimization recommendations
- Performance benchmarking and comparison tools

---

---

## 🚀 Version History & Compatibility

### Latest Releases
- **v2.8** (2025-10-18): API overhaul with real-time activity metrics, zone info and statistics
- **v2.6** (2025-09-15): Server transfer system with BungeeCord/Velocity support
- **v2.5** (2025-09-11): AFK Credit System, WorldGuard integration, AFK zone protection, SQL history
- **v2.4.2** (2025-09-09): Complete Folia 1.21.8 compatibility, method signature fixes
- **v2.4.1** (2025-09-06): Critical bug fix for repeated kick/teleport actions
- **v2.4** (2025-08-07): Large AFK pool detection, keystroke timeout analysis

### Platform Compatibility Matrix
| Platform | Version Support | Status | Notes |
|----------|----------------|--------|---------|
| **Paper** | 1.16 - 1.21.8 | ✅ Full | Recommended platform |
| **Spigot** | 1.16 - 1.21.8 | ✅ Full | Complete compatibility |
| **Bukkit** | 1.16 - 1.21.8 | ✅ Full | Legacy support |
| **Purpur** | 1.16 - 1.21.8 | ✅ Full | Enhanced features |
| **Folia** | 1.19.4 - 1.21.8 | ✅ Full | Native thread-per-region support |
| **Bedrock** | All versions | ✅ Full | Via Floodgate/Geyser |

### Java Version Requirements
- **Java 17+** required for all versions
- **Java 21** recommended for optimal performance
- **GraalVM** supported for enterprise deployments

---

## 👨‍💻 Author & License

**Developed by Koyere**  
Licensed under **MIT License** - Open source and free to use for all server types.

**Current Version**: v2.8 (API Overhaul)  
**Enterprise Features**: Available in all releases  
**Community Support**: Active development and updates

---

## 🎯 Quick Start Guide

1. **Download** latest version from releases
2. **Install** in `/plugins` folder
3. **Restart** server (generates default configs)
4. **Enable modules** you want in `config.yml`
5. **Configure** AFK times and detection settings
6. **Customize** messages in `messages.yml`
7. **Test** with `/afk` command
8. **Monitor** performance with `/afkplus status`

**For Credit System (v2.5):**
1. Set `credit-system.enabled: true`
2. Configure `afk-zone.location` coordinates
3. Adjust `credit-ratios` for different permission groups
4. Test credit earning with active gameplay
5. Test AFK zone teleportation and `/afkback` command

**Need help?** Join our Discord community or check the GitHub wiki!

---

## 💰 Guía del Sistema de Créditos (v2.5)

### Quick Activation
- In `config.yml` enable the module and the feature:
  - `modules.credit-system.enabled: true`
  - `credit-system.enabled: true`
- Optional: configure AFK zone location in `credit-system.afk-zone` or use existing `zone-management`.

### Key Configuration (example)
```yaml
modules:
  credit-system:
    enabled: true

credit-system:
  enabled: true
  credit-ratios:
    default: "5:1"
    vip: "4:1"
    premium: "3:1"
    admin: "2:1"
  max-credits:
    default: 120
    vip: 180
    premium: 240
    admin: 480
  earning-requirements:
    minimum-session-minutes: 5
    activity-threshold: 0.3
  afk-zone:
    enabled: true
    world: "world"
    location: "0,100,0"
  notifications:
    credit-earned: true
    credit-consumed: true
    credit-exhausted: true
    low-credits-warning: true
    low-credits-threshold: 15
  return-command:
    enabled: true
    cooldown-seconds: 10
  credit-decay:
    enabled: false
    expire-after-days: 7
    warning-days: 2
    cleanup-interval-hours: 24
  database:
    enabled: false
    save-interval-minutes: 5
```

### Permisos
- `antiafkplus.credit.earn` — ganar créditos (true)
- `antiafkplus.credit.use` — usar créditos para retrasar la acción AFK (true)
- `antiafkplus.credit.return` — usar `/afkback` (true)
- `antiafkplus.credit.check` — usar `/afkcredits` (true)
- `antiafkplus.credit.ratio.vip|premium|admin` — ratio mejorado (según grupo)
- `antiafkplus.reload` — recarga de config (`/afkcredits reload`)

### Commands
- `/afkcredits [reload]` — shows balance (minutes/hours), ratio and maximum; `reload` reloads config.
- `/afkback` (aliases: `/afkreturn`, `/returnfromafk`) — returns from AFK zone to original location, with cooldown.
- `/afkcredits <give|take|set|reset> <player> [minutes]` — credit administration (permission `antiafkplus.credit.admin`).
- `/afkcredits history <player> [limit]` — shows latest credit transactions (requires SQL backend enabled).

### Placeholders (PlaceholderAPI)
- `%antiafkplus_credits%` — credit balance (minutes)
- `%antiafkplus_credits_hours%` — credit balance (hours)
- `%antiafkplus_max_credits%` — maximum allowed
- `%antiafkplus_credit_ratio%` — active:credit ratio (e.g., "5:1")
- `%antiafkplus_in_afk_zone%` — `true|false` if player is in AFK zone
- `%antiafkplus_credits_expire_days%` — days until credits expire (empty if N/A)

### How It Works
1) Active players earn credits according to ratio and limits.
2) When AFK is detected, if balance > 0 the final action is cancelled and 1 credit/minute is consumed.
3) If the player becomes active again, consumption stops (no teleport).
4) If balance reaches 0, the player is teleported to the AFK zone; use `/afkback` to return.

### Credit API — Examples (Java)
```java
AntiAFKPlusAPI api = AntiAFKPlusAPI.getInstance();

// Basic queries
long balance = api.getCreditBalance(player);
long max = api.getMaxCredits(player);
String ratio = api.getCreditRatio(player); // "5:1"
boolean inZone = api.isInAFKZone(player);
Location zoneLoc = api.getAFKZoneLocation(player);
Location original = api.getOriginalLocation(player);

// Balance modification (administration)
api.addCredits(player, 15);         // +15 minutes
api.consumeCredits(player, 5);      // -5 minutes (if available)
api.setCreditBalance(player, 60);   // set balance to 60m (capped by max)

// Return from AFK zone
boolean ok = api.returnFromAFKZone(player);

// Credit events (non-Bukkit)
EventRegistration regEarned = api.registerCreditEarnedListener(evt -> {
    Player p = evt.getPlayer();
    long earned = evt.getCreditsEarned();
    long total = evt.getTotalCredits();
    // custom logic
});

EventRegistration regConsumed = api.registerCreditConsumedListener(evt -> {
    Player p = evt.getPlayer();
    long used = evt.getCreditsConsumed();
    long remaining = evt.getRemainingCredits();
    // custom logic
});
```

### Optimization Tips
- Keep `low-credits-threshold` reasonable (e.g., 10–20) to avoid spam.
- On Folia, do not switch to BukkitScheduler; AntiAFKPlus uses `PlatformScheduler` transparently.
- For large servers, keep `save-interval-minutes` ≥ 5 if persistence is enabled.

### Persistence (Lightweight & Optional)
- By default, persistence uses `credits.yml` (lightweight, zero dependencies).
- Optional SQL persistence (without increasing JAR size):
  - Requires JDBC driver on the server classpath:
    - SQLite: `org.sqlite.JDBC`
    - MySQL: `com.mysql.cj.jdbc.Driver`
  - If the driver is not available or the connection fails, the plugin automatically falls back to file.
- Configuration:
```yaml
credit-system:
  database:
    enabled: true
    save-interval-minutes: 5
# Global DB base (already present):
database:
  type: "SQLite" # SQLite, MySQL
  sqlite:
    file-name: "antiafkplus.db" # not used by credit system (credits.sqlite is used)
  mysql:
    host: "localhost"
    port: 3306
    database: "antiafkplus"
    username: "root"
    password: "password"
  # Optional table prefix for credit tables
credit-system:
  database:
    table-prefix: "afkplus_"
```
