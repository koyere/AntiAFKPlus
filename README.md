# AntiAFKPlus Enterprise

**Professional-grade AFK detection and management system for Minecraft servers**

✅ **Minecraft 1.16 – 1.21.5** | ✅ **Folia/Paper/Spigot/Bukkit** | ✅ **Bedrock Compatible** | ✅ **Zero-lag Performance**

---

## 🚀 Enterprise Features

### 🏗️ **Modular Architecture**
- **Enable/disable modules** independently via configuration
- **Core modules**: Detection, Events, API, Commands
- **Feature modules**: Pattern Detection, Autoclick Detection, Player Protection, Zones, Rewards
- **Integration modules**: WorldGuard, PlaceholderAPI, Vault, DiscordSRV, Floodgate

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
- **Pattern recognition** for bot detection (circular movements, confined spaces)
- **Behavioral analysis** with activity scoring
- **Autoclick/macro detection** with configurable thresholds
- **Multi-activity tracking** (movement, rotation, jumping, chat, commands)

---

## 📦 Installation & Setup

1. **Download** the latest version
2. **Place** `AntiAFKPlus.jar` in your `/plugins` folder
3. **Restart** your server
4. **Configure** modules in `config.yml`
5. **Customize** messages in `messages.yml`

---

## 🎛️ Commands & Permissions

### Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/afk` | Toggle AFK mode manually | `antiafkplus.afk` |
| `/afk list` | View all AFK players | `antiafkplus.list` |
| `/afkplus reload` | Reload configuration | `antiafkplus.admin` |
| `/afkplus status` | View plugin status | `antiafkplus.admin` |

### Permissions
| Permission | Description |
|------------|-------------|
| `antiafkplus.bypass` | Bypass all AFK detection |
| `antiafkplus.bypass.detection` | Bypass basic detection only |
| `antiafkplus.bypass.patterns` | Bypass pattern detection |
| `antiafkplus.time.vip` | Custom AFK time limits |
| `antiafkplus.admin` | Admin commands access |

---

## ⚙️ Configuration

### Module System
Enable/disable features in `config.yml`:
```yaml
modules:
  pattern-detection:
    enabled: true
  autoclick-detection:
    enabled: true
  player-protection:
    enabled: true
  afk-zones:
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

### Pattern Detection
```yaml
pattern-detection-settings:
  water-circle-radius: 3.0
  repetitive-movement-threshold: 0.8
  max-pattern-violations: 3
```

---

## 🔌 Integrations

### PlaceholderAPI
- `%antiafkplus_status%` - Player AFK status
- `%antiafkplus_afktime%` - Time since last activity
- `%antiafkplus_activity_score%` - Activity score
- `%antiafkplus_pattern_confidence%` - Pattern detection confidence

### WorldGuard
- **Region-based AFK settings** with zone inheritance
- **Custom timeouts** per region
- **AFK restrictions** in specific areas

### Bedrock Edition
- **Automatic detection** of Bedrock players
- **UI adaptations** for touch controls
- **Menu optimizations** for mobile devices

---

## 💻 Developer API

### Basic Usage
```java
// Get API instance
AntiAFKPlusAPI api = AntiAFKPlusAPI.getInstance();

// Check AFK status
boolean isAfk = api.isAFK(player);
AFKStatus status = api.getAFKStatus(player);

// Activity tracking
Duration timeSinceActivity = api.getTimeSinceLastActivity(player);
PlayerActivityInfo activityInfo = api.getActivityInfo(player);

// Pattern detection
boolean hasSuspiciousPatterns = api.hasSuspiciousPatterns(player);
List<DetectedPattern> patterns = api.getDetectedPatterns(player);
```

### Event Handling
```java
// Register for AFK state changes
api.registerAFKStateListener(event -> {
    Player player = event.getPlayer();
    AFKStatus fromStatus = event.getFromStatus();
    AFKStatus toStatus = event.getToStatus();
    // Handle state change
});

// Register for pattern detection
api.registerPatternDetectionListener(event -> {
    if (event.getPattern().getConfidence() > 0.8) {
        // High confidence bot detection
    }
});
```

### Async Operations
```java
// Async status checking
CompletableFuture<AFKStatus> statusFuture = api.getAFKStatusAsync(player);
statusFuture.thenAccept(status -> {
    // Handle result
});
```

---

## 📊 Performance & Monitoring

### Performance Metrics
- **Real-time TPS impact** monitoring
- **Memory usage** tracking per module
- **Execution time** profiling
- **Cache hit rates** and optimization stats

### Debug Mode
Enable detailed logging:
```yaml
debug: true
performance:
  debug-logging: true
```

---

## 🔧 Troubleshooting

### Common Issues
- **High CPU usage**: Increase check intervals or disable heavy modules
- **Bedrock players not detected**: Enable Floodgate integration
- **Pattern detection false positives**: Adjust sensitivity thresholds
- **Performance issues**: Enable adaptive intervals and optimization

### Support
- 📖 **Documentation**: In-game `/afkplus help`
- 💬 **Discord Support**: https://discord.gg/xKUjn3EJzR
- 🐛 **Bug Reports**: GitHub Issues

---

## 📈 Statistics

This plugin uses **bStats** for anonymous usage statistics.  
Disable in `/plugins/bStats/config.yml` if needed.

---

## 👨‍💻 Author & License

**Developed by Koyere**  
Licensed under **MIT License** - Open source and free to use.

**Version**: 2.0.0-ENTERPRISE