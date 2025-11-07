# AntiAFKPlus API - Integration Guide

This guide explains how to integrate the AntiAFKPlus API into your Minecraft plugin.

> **Novedad v2.8**: La API devuelve datos en tiempo real (actividad, estadísticas, zonas) y expone nuevos eventos de advertencias/patrones con soporte de cancelación.

## Installation

### Maven

Add the Jitpack repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Then add the dependency:

```xml
<dependencies>
    <dependency>
        <groupId>com.github.koyere</groupId>
        <artifactId>AntiAFKPlus</artifactId>
        <version>2.8.2</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### Gradle

Add the Jitpack repository to your `build.gradle`:

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
```

Then add the dependency:

```groovy
dependencies {
    compileOnly 'com.github.koyere:AntiAFKPlus:2.8.2'
}
```

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.koyere:AntiAFKPlus:2.8.2")
}
```

## Plugin Configuration

Add AntiAFKPlus as a dependency in your `plugin.yml`:

```yaml
name: YourPlugin
version: 1.0.0
main: com.yourname.yourplugin.YourPlugin
depend: [AntiAFKPlus]
# Or if optional:
softdepend: [AntiAFKPlus]
```

## Basic Usage

### Getting the API Instance

```java
import me.koyere.antiafkplus.api.AntiAFKPlusAPI;

public class YourPlugin extends JavaPlugin {
    private AntiAFKPlusAPI afkApi;

    @Override
    public void onEnable() {
        // Check if AntiAFKPlus is available
        if (getServer().getPluginManager().getPlugin("AntiAFKPlus") == null) {
            getLogger().warning("AntiAFKPlus is not installed!");
            return;
        }

        // Get the API instance
        afkApi = AntiAFKPlusAPI.getInstance();
        getLogger().info("AntiAFKPlus API loaded successfully!");
    }
}
```

## Usage Examples

### 1. Check AFK Status

```java
import me.koyere.antiafkplus.api.data.AFKStatus;
import org.bukkit.entity.Player;

public void checkPlayerAFK(Player player) {
    AntiAFKPlusAPI api = AntiAFKPlusAPI.getInstance();

    // Simple check
    boolean isAfk = api.isAFK(player);
    if (isAfk) {
        player.sendMessage("You are AFK!");
    }

    // Get detailed status
    AFKStatus status = api.getAFKStatus(player);
    switch (status) {
        case ACTIVE:
            player.sendMessage("You are active");
            break;
        case AFK_IDLE:
            player.sendMessage("You are idle");
            break;
        case AFK_WARNED:
            player.sendMessage("You have been warned for inactivity");
            break;
        case AFK_VOLUNTARY:
            player.sendMessage("You are in voluntary AFK mode");
            break;
    }
}
```

### 2. Activity Information

```java
import me.koyere.antiafkplus.api.data.PlayerActivityInfo;
import java.time.Duration;

public void checkActivity(Player player) {
    AntiAFKPlusAPI api = AntiAFKPlusAPI.getInstance();

    // Time since last activity
    Duration timeSinceActivity = api.getTimeSinceLastActivity(player);
    long seconds = timeSinceActivity.getSeconds();
    player.sendMessage("Inactive for: " + seconds + " seconds");

    // Detailed information
    PlayerActivityInfo info = api.getActivityInfo(player);
    player.sendMessage("Activity score: " + info.getActivityScore());
}
```

### 3. Pattern Detection

```java
import me.koyere.antiafkplus.api.data.DetectedPattern;
import java.util.List;

public void checkPatterns(Player player) {
    AntiAFKPlusAPI api = AntiAFKPlusAPI.getInstance();

    // Check for suspicious patterns
    if (api.hasSuspiciousPatterns(player)) {
        List<DetectedPattern> patterns = api.getDetectedPatterns(player);

        for (DetectedPattern pattern : patterns) {
            getLogger().warning(player.getName() + " has pattern: " +
                pattern.getType() + " (confidence: " + pattern.getConfidence() + ")");
        }
    }
}
```

### 4. Credit System (v2.5+)

```java
public void manageCreditSystem(Player player) {
    AntiAFKPlusAPI api = AntiAFKPlusAPI.getInstance();

    // Get credit balance
    long balance = api.getCreditBalance(player);
    player.sendMessage("Credits: " + balance + " minutes");

    // Check if player has enough credits
    if (api.hasCredits(player, 30)) {
        player.sendMessage("You have at least 30 minutes of credits!");
    }

    // Credit administration (requires permissions)
    api.addCredits(player, 15); // Add 15 minutes
    api.consumeCredits(player, 5); // Consume 5 minutes

    // Additional information
    String ratio = api.getCreditRatio(player); // "5:1"
    long maxCredits = api.getMaxCredits(player);

    // AFK zone
    if (api.isInAFKZone(player)) {
        player.sendMessage("You are in the AFK zone");
        // Return player
        api.returnFromAFKZone(player);
    }
}
```

### 5. AFK Events

```java
import me.koyere.antiafkplus.api.events.EventRegistration;

public class YourPlugin extends JavaPlugin {
    private EventRegistration afkStateRegistration;
    private EventRegistration patternRegistration;

    @Override
    public void onEnable() {
        AntiAFKPlusAPI api = AntiAFKPlusAPI.getInstance();

        // Listen to AFK state changes
        afkStateRegistration = api.registerAFKStateListener(event -> {
            Player player = event.getPlayer();
            getLogger().info(player.getName() + " changed from " +
                event.getFromStatus() + " to " + event.getToStatus());
        });

        // Listen to pattern detection
        patternRegistration = api.registerPatternDetectionListener(event -> {
            Player player = event.getPlayer();
            DetectedPattern pattern = event.getPattern();

            if (pattern.getConfidence() > 0.8) {
                getLogger().warning(player.getName() +
                    " detected with suspicious pattern: " + pattern.getType());
            }
        });

        // Credit events
        api.registerCreditEarnedListener(event -> {
            Player player = event.getPlayer();
            long earned = event.getCreditsEarned();
            player.sendMessage("You earned " + earned + " AFK credits!");
        });

        api.registerCreditConsumedListener(event -> {
            Player player = event.getPlayer();
            long remaining = event.getRemainingCredits();
            if (remaining < 10) {
                player.sendMessage("Low credits remaining: " + remaining + " min");
            }
        });
    }

    @Override
    public void onDisable() {
        // Unregister listeners
        if (afkStateRegistration != null) {
            afkStateRegistration.unregister();
        }
        if (patternRegistration != null) {
            patternRegistration.unregister();
        }
    }
}
```

### 6. Asynchronous Operations

```java
import java.util.concurrent.CompletableFuture;

public void asyncOperations(Player player) {
    AntiAFKPlusAPI api = AntiAFKPlusAPI.getInstance();

    // Get status asynchronously
    CompletableFuture<AFKStatus> statusFuture = api.getAFKStatusAsync(player);

    statusFuture.thenAccept(status -> {
        // This code executes when the result is obtained
        player.sendMessage("Your AFK status is: " + status);
    }).exceptionally(throwable -> {
        getLogger().severe("Error getting status: " + throwable.getMessage());
        return null;
    });

    // Batch operations
    List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
    Map<Player, AFKStatus> batchStatus = api.getBatchAFKStatus(players);

    batchStatus.forEach((p, status) -> {
        getLogger().info(p.getName() + ": " + status);
    });
}
```

### 7. Statistics and Metrics

```java
import me.koyere.antiafkplus.api.data.AFKStatistics;
import me.koyere.antiafkplus.api.data.PlayerAFKStatistics;
import me.koyere.antiafkplus.api.data.PerformanceMetrics;

public void getStatistics(Player player) {
    AntiAFKPlusAPI api = AntiAFKPlusAPI.getInstance();

    // Server statistics
    AFKStatistics serverStats = api.getServerAFKStatistics();
    getLogger().info("AFK players: " + serverStats.getTotalAFKPlayers());

    // Player statistics
    PlayerAFKStatistics playerStats = api.getPlayerAFKStatistics(player);
    player.sendMessage("Total AFK time: " + playerStats.getTotalAFKTime());

    // Performance metrics
    PerformanceMetrics metrics = api.getPerformanceMetrics();
    getLogger().info("Memory usage: " + metrics.getMemoryUsage() + " MB");
}
```

## Common Use Cases

### Reward System

```java
public class RewardSystem {
    private final AntiAFKPlusAPI api;

    public RewardSystem() {
        this.api = AntiAFKPlusAPI.getInstance();
    }

    public void giveActivityReward(Player player) {
        PlayerActivityInfo info = api.getActivityInfo(player);

        // Reward very active players
        if (info.getActivityScore() > 80 && !api.isAFK(player)) {
            // Give reward
            player.sendMessage("Reward for being active!");
        }
    }
}
```

### Anti-Bot System

```java
public class AntiBotSystem {
    private final AntiAFKPlusAPI api;

    public AntiBotSystem() {
        this.api = AntiAFKPlusAPI.getInstance();

        // Listen to pattern detection
        api.registerPatternDetectionListener(event -> {
            Player player = event.getPlayer();
            DetectedPattern pattern = event.getPattern();

            // If confidence is very high, it's probably a bot
            if (pattern.getConfidence() > 0.9) {
                player.kickPlayer("Bot behavior detected");
            }
        });
    }
}
```

### Economy Integration

```java
public class EconomyIntegration {
    private final AntiAFKPlusAPI api;
    private final Economy economy; // Your economy system

    public void chargeForAFK(Player player) {
        if (api.isAFK(player)) {
            Duration afkTime = api.getTimeSinceLastActivity(player);
            long minutes = afkTime.toMinutes();

            // Charge per minute AFK
            double charge = minutes * 10.0;
            economy.withdrawPlayer(player, charge);
        }
    }
}
```

## Additional Information

### Versions

To use a specific version, change the version number:

```xml
<version>2.8.2</version>  <!-- Latest stable version -->
<version>2.6</version>  <!-- Previous version -->
<version>main-SNAPSHOT</version>  <!-- Latest development version (not recommended) -->
```

### Support and Documentation

- **GitHub**: https://github.com/koyere/AntiAFKPlus
- **Discord**: https://discord.gg/xKUjn3EJzR
- **Wiki**: See README.md for complete documentation

### Important Notes

1. **Scope**: Use `provided` or `compileOnly` since AntiAFKPlus must be installed on the server
2. **Dependencies**: Make sure to declare AntiAFKPlus in your `plugin.yml`
3. **Versions**: The API is compatible from version 2.0+
4. **Thread Safety**: All API operations are thread-safe
5. **Async**: Use `*Async()` methods for heavy operations

## Multi-Platform Support

AntiAFKPlus supports:
- Spigot, Paper, Purpur (1.16-1.21.8)
- Folia (1.19.4-1.21.8)
- Java & Bedrock Edition (via Floodgate)

The API works identically on all platforms.
