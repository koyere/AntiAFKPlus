# AntiAFKPlus v3.1

**Professional-grade AFK detection and management system with AFK Credit System and Server Transfer**

✅ **Minecraft 1.16 – 1.21.6+** | ✅ **Folia/Paper/Spigot/Bukkit/Purpur** | ✅ **Java & Bedrock Edition** | ✅ **Zero-lag Performance**

---

> **Where to get the plugin:**
> - **Compiled JAR (recommended):** [SpigotMC Premium](https://www.spigotmc.org/resources/antiafkplus-premium.134494/)
> - **Source code (free):** [GitHub](https://github.com/koyere/AntiAFKPlus)
>
> ⚠️ Versions prior to 3.0 are no longer supported or maintained. Please update to v3.x.

---

## What's New in v3.x

### v3.1 — Bug Fix Release

**Critical fixes:**
- Water AFK pool bypass — water current movement no longer counts as activity; only real player input does.
- Door spam-click bypass — repeatedly clicking a door no longer resets the AFK timer.
- "Use Item: toggle" bypass — holding right-click on levers or buttons no longer prevents detection.
- "Attack/Destroy: toggle" bypass on cobblestone generators — holding left-click on a cobble generator no longer prevents detection.
- Large AFK pool detection was completely non-functional — now works correctly.

**Other fixes:**
- Console `[Activity]` log spam — these messages are now gated behind `debug: true`.
- Empty messages (`""`) no longer produce blank chat lines.
- "No longer AFK" broadcast was missing or delayed — now fires immediately.
- AFK warning countdown showed wrong number — now shows the actual seconds remaining.
- AFK warnings not cleared after world change — now resets correctly.
- GUI shift+click did not decrease values — fixed.
- `GAMEMODE` and `COMMAND` afk-action types were silently ignored and fell through to `KICK` — both now work.
- `database.enabled` was ignored by SQL storage — fixed.
- DiscordSRV pattern alerts never fired — fixed.
- `performance.adaptive-intervals` had no effect — fixed.

**New features in v3.1:**
- Reward system is now fully functional.
- Analytics system is now fully functional (`/afkplus analytics`, `/afkplus analytics export`).
- Hologram support for DecentHolograms and FancyHolograms.
- Bedrock/Geyser message and GUI adaptation now fully connected.

---

### v3.0.4 — Performance

- Reduced load on the server main thread. Internal scheduling checks now run once per tick instead of per task, and are skipped entirely on Spigot and forks that don't support Paper's pause API. Expect more stable TPS on busy servers.

---

### v3.0.3 — Bug Fixes

- **Critical:** Players riding animals (horse, boat, camel, etc.) in water could bypass AFK detection indefinitely due to the animal's bobbing movement counting as activity. Fixed — only camera movement (real input) counts for mounted players.
- Empty AFK state messages (`""`) sent blank lines to all players. Fixed.
- `/afkplus reload` did not reload language files. Fixed — language changes now apply immediately.
- Config key `max-voluntary-afk-time-seconds` renamed to `max-afk-duration-seconds` (old key still works).

---

### v3.0.2 — New Features & Fixes

**New features:**
- **Global AFK Action** — configure what happens when a player goes AFK directly in `config.yml`, without needing zone management or WorldGuard:
  ```yaml
  afk-action:
    type: "TELEPORT"           # KICK | TELEPORT | MARK_AFK_ONLY | NONE
    teleport-location: "world,250,76,30"
    exempt-worlds:
      - "world_spawn"          # AFK shown in tab, but no kick/teleport
  ```
- **Hex color support** in language files (`&#e5be01`, `{#e5be01}`, `#e5be01` formats).

**Critical fixes:**
- Messages always in English on first startup — fixed.
- Players kicked even with `TELEPORT` configured — fixed. Any zone name now works (previously only `"spawn"` worked).
- Zone management required WorldGuard even for simple setups — `require-worldguard` now defaults to `false`.
- Warning times showed wrong numbers — fixed.
- Warning titles, teleport message, pattern detection warnings, and Bedrock messages were all hardcoded in English — all now read from language files.

---

### v3.0 — Major Rewrite

Complete rewrite from v2.x. Key additions:

- **In-game GUI** (`/afkplus gui`) — configure everything without editing files. Includes Detection Settings, Module Toggles, Zone Settings, Reward Settings, Language Selector, and live performance stats.
- **Detection Profiles** — one-click presets: Conservative (fewer false positives), Balanced (default), Aggressive (strict). Switch in the GUI or via config.
- **Visual Effects** — particles above AFK players, `[AFK]` tab list prefix, `[AFK]` display name prefix. Each independently configurable.
- **10 languages built-in** — EN, ES, FR, DE, PT, RU, ZH, JA, KO, IT. Switch instantly via GUI or config. Add your own by creating a `.yml` in `plugins/AntiAFKPlus/languages/`.
- **Credit system additions** — credit transfer between players (`/afkcredits transfer <player> <min>`), leaderboard (`/afkcredits top`), and admin credit multiplier events (`/afkplus event credits <mult> <min>`).
- **New integrations** — Vault economy and DiscordSRV (AFK notifications to Discord), both optional and reflection-based.
- **Config reduced 44%** — from 1033 to 580 lines. Single toggle per feature, all time values in seconds.
- **JAR size: ~400 KB** (down from 3.2 MB).
- **New commands:** `/afkplus performance`, `/afk status [player]`, `/afkcredits transfer`, `/afkcredits top`.

**Migration from v2.x:**

| What changed | What to do |
|---|---|
| `messages.yml` no longer exists | Customize messages in `languages/en.yml` instead |
| `enhanced-detection` section removed | Use `modules.pattern-detection.enabled` |
| `autoclick-detection` root key removed | Now under `modules.autoclick-detection.enabled` |
| Time keys renamed (`*-ms` → `*-seconds`) | Old `-ms` keys still work as fallback |

The plugin auto-migrates your config on first startup. **Back up your `plugins/AntiAFKPlus/` folder before upgrading.**

---

## Features

### Advanced Detection
- Pattern recognition for bot detection (circular movements, confined spaces, large AFK pools)
- Behavioral analysis with activity scoring and keystroke timeout detection
- Autoclick/macro detection with configurable thresholds
- Multi-activity tracking (movement, rotation, jumping, chat, commands, interactions)
- Manual vs automatic movement analysis (velocity variance)
- AFK time windows — pause kicks during configurable hours

### Modular Architecture
Enable/disable features independently in `config.yml`:
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
  credit-system:
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

### Performance
- Adaptive check intervals based on server TPS
- Object pooling and intelligent caching
- Real-time performance monitoring via `/afkplus performance`

---

## Commands & Permissions

### Core Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/afk` | Toggle AFK mode manually | `antiafkplus.afk` |
| `/afk list` | View all AFK players | `antiafkplus.list` |
| `/afk status [player]` | Check if a player is AFK | `antiafkplus.status.check` |
| `/afkplus reload` | Reload config and language files | `antiafkplus.reload` |
| `/afkplus gui` | Open the settings GUI | `antiafkplus.reload` |
| `/afkplus status` | Plugin status overview | `antiafkplus.stats` |
| `/afkplus performance` | Detailed performance metrics | `antiafkplus.stats` |
| `/afkplus analytics` | Live analytics summary | `antiafkplus.stats` |
| `/afkplus analytics export` | Force an immediate analytics export | `antiafkplus.stats` |
| `/afkplus event credits <mult> <min>` | Start a credit multiplier event | `antiafkplus.reload` |

### Credit System Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/afkback` | Return from AFK zone to original location | `antiafkplus.credit.return` |
| `/afkcredits` | Check AFK credit balance | `antiafkplus.credit.check` |
| `/afkcredits transfer <player> <min>` | Send credits to another player | `antiafkplus.credit.transfer` |
| `/afkcredits top [limit]` | Credit leaderboard | `antiafkplus.credit.check` |
| `/afkcredits history <player> [limit]` | Credit transaction history (SQL only) | `antiafkplus.credit.admin` |
| `/afkcredits give/take/set/reset <player>` | Admin: modify player credits | `antiafkplus.credit.admin` |

### Permissions
| Permission | Description |
|------------|-------------|
| `antiafkplus.bypass` | Bypass all AFK detection |
| `antiafkplus.bypass.detection` | Bypass basic detection only |
| `antiafkplus.bypass.patterns` | Bypass pattern detection |
| `antiafkplus.window.bypass` | Ignore AFK time windows |
| `antiafkplus.time.vip` | Custom AFK time limits |
| `antiafkplus.admin` | Admin commands access |
| `antiafkplus.credit.earn` | Earn AFK credits (default: true) |
| `antiafkplus.credit.use` | Use credits to delay AFK action (default: true) |
| `antiafkplus.credit.return` | Use `/afkback` (default: true) |
| `antiafkplus.credit.check` | Check credit balance (default: true) |
| `antiafkplus.credit.transfer` | Transfer credits to another player |
| `antiafkplus.credit.ratio.vip` | VIP credit earning ratio (4:1) |
| `antiafkplus.credit.ratio.premium` | Premium credit ratio (3:1) |
| `antiafkplus.credit.ratio.admin` | Admin credit ratio (2:1) |
| `antiafkplus.credit.admin` | Full credit system administration |

---

## Configuration

### Global AFK Action
What happens when a player goes AFK — no zone management or WorldGuard required:
```yaml
afk-action:
  type: "TELEPORT"           # KICK | TELEPORT | MARK_AFK_ONLY | NONE | GAMEMODE | COMMAND
  teleport-location: "world,250,76,30"
  gamemode: "SPECTATOR"      # Used when type: GAMEMODE
  command: "kick {player}"   # Used when type: COMMAND. Supports {player} and {uuid}
  exempt-worlds:
    - "world_spawn"          # AFK shown in tab here but not teleported/kicked
```

Action priority: Zone Management > Global `afk-action` > Server Transfer > KICK

### AFK Time Windows
Pause kicks during specific hours:
```yaml
afk-windows:
  enabled: true
  timezone: "SERVER"              # or any IANA ID (e.g. America/New_York)
  ranges:
    - "08:00-12:00"
    - "20:00-23:00"
  behavior-inside-window: "SKIP_ACTIONS"
  bypass-permission: "antiafkplus.window.bypass"
```

### Pattern Detection
```yaml
pattern-detection-settings:
  water-circle-radius: 3.0
  repetitive-movement-threshold: 0.82
  max-pattern-violations: 3
  large-pool-threshold: 25.0
  keystroke-timeout-ms: 180000     # 3 minutes
  automatic-movement-velocity-threshold: 0.15
```

### Visual Effects
```yaml
visual-effects:
  particles:
    enabled: true
    type: "CLOUD"
    count: 5
  tab-list:
    enabled: true
    afk-prefix: "&7[AFK] "
  name-tags:
    enabled: false
    afk-prefix: "&7[AFK] "
  holograms:
    enabled: false            # Requires DecentHolograms or FancyHolograms
    lines:
      - "&eAFK"
      - "&7{time}"
    height-offset: 2.5
```

### Analytics
```yaml
analytics:
  enabled: true
  export-format: "JSON"       # JSON or CSV
  # Reports saved to: plugins/AntiAFKPlus/analytics/
```

### Performance
```yaml
performance:
  auto-optimization: true
  adaptive-intervals: true
  max-tps-impact: 0.5
```

### Internationalization
```yaml
internationalization:
  default-language: "en"     # en, es, fr, de, pt, ru, zh, ja, ko, it
```

---

## Integrations

### PlaceholderAPI
| Placeholder | Returns |
|---|---|
| `%antiafkplus_status%` | `AFK`, `ACTIVE`, `MANUAL AFK` |
| `%antiafkplus_afktime%` | Seconds since last activity |
| `%antiafkplus_credits%` | Credit balance (minutes) |
| `%antiafkplus_credits_hours%` | Credit balance (hours) |
| `%antiafkplus_max_credits%` | Maximum credits allowed |
| `%antiafkplus_credit_ratio%` | Earning ratio (e.g. `5:1`) |
| `%antiafkplus_in_afk_zone%` | `true` / `false` |
| `%antiafkplus_credits_expire_days%` | Days until credits expire |
| `%antiafkplus_credits_rank%` | Leaderboard position |

### WorldGuard — Zone-based AFK management
```yaml
zone-management:
  enabled: true
  require-worldguard: false
  zones:
    spawn:
      kick-action: "TELEPORT"
      teleport-location: "world,0,100,0"
    afk:
      kick-action: "TRANSFER"
      transfer-server: "lobby"
```

### DiscordSRV
```yaml
integrations:
  discordsrv:
    enabled: true
    send-afk-notifications: true
    send-pattern-alerts: true    # Sends alert when suspicious pattern detected
```

### Vault Economy
```yaml
integrations:
  vault:
    enabled: true
```

### Bedrock Edition (Geyser/Floodgate)
Detected automatically. Enable with `bedrock-compatibility.enabled: true` (default).

---

## Server Transfer (Bungee/Velocity)

Transfer AFK players to another server via Plugin Messaging:
```yaml
server-transfer:
  enabled: true
  target-server: "lobby"
  proxy-channel: "auto"         # auto | bungeecord | namespaced
  fallback-action: "KICK"

  countdown:
    enabled: false
    seconds: 10
    title: "&cYou are AFK"
    subtitle: "&eMoving in {seconds}s"
    sound:
      enabled: true
      name: "ENTITY_EXPERIENCE_ORB_PICKUP"

  pipeline:
    enabled: false
  actions:
    - "TITLE: &cYou are AFK"
    - "WAIT: 1s"
    - "TRANSFER: lobby"
```

---

## Credit System

Players earn credits by being active. Credits can be spent to delay AFK punishments.

### Configuration
```yaml
credit-system:
  enabled: true
  credit-ratios:
    default: "5:1"          # 5 min active = 1 min AFK credit
    vip: "4:1"
    premium: "3:1"
    admin: "2:1"
  max-credits:
    default: 120            # minutes
    vip: 180
  afk-zone:
    enabled: true
    world: "world"
    location: "0,100,0"
  return-command:
    enabled: true
    cooldown-seconds: 10
  credit-decay:
    enabled: false
    expire-after-days: 7
  database:
    enabled: false
    save-interval-minutes: 5
    table-prefix: "afkplus_"  # Change per server for multi-server setups
```

### How it works
1. Active players earn credits based on their ratio and maximum.
2. When a player goes AFK and has credits, the final action is cancelled and 1 credit/minute is consumed.
3. If they return from AFK before credits run out, consumption stops with no penalty.
4. If credits reach 0, the player is sent to the AFK zone. Use `/afkback` to return.

### Multi-server setup (shared database)
Use different `table-prefix` values per server with the same MySQL database to keep credit economies independent:
```yaml
# Skyblock server:
credit-system:
  database:
    enabled: true
    table-prefix: "skyblock_"

# Survival server (same database):
credit-system:
  database:
    enabled: true
    table-prefix: "survival_"
```

### Database configuration
```yaml
database:
  type: "MySQL"       # MySQL or SQLite
  mysql:
    host: "localhost"
    port: 3306
    database: "antiafkplus"
    username: "root"
    password: "password"
```

---

## Developer API

### Installation via Jitpack

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
        <version>3.1</version>
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
    compileOnly 'com.github.koyere:AntiAFKPlus:3.1'
}
```

📚 **[View complete API documentation →](API.md)**

### Basic Usage
```java
AntiAFKPlusAPI api = AntiAFKPlusAPI.getInstance();

// AFK status
boolean isAfk = api.isAFK(player);
AFKStatus status = api.getAFKStatus(player);

// Activity
Duration timeSinceActivity = api.getTimeSinceLastActivity(player);

// Pattern detection
boolean suspicious = api.hasSuspiciousPatterns(player);
List<DetectedPattern> patterns = api.getDetectedPatterns(player);

// State listener
api.registerAFKStateListener(event -> {
    AFKStatus from = event.getFromStatus();
    AFKStatus to = event.getToStatus();
});

// Kick listener (cancellable)
api.registerAFKKickListener(event -> {
    event.setCancelled(true);
});
```

### Credit System API
```java
long balance = api.getCreditBalance(player);
api.addCredits(player, 15);
api.consumeCredits(player, 10);
boolean inZone = api.isInAFKZone(player);
boolean returned = api.returnFromAFKZone(player);

api.registerCreditEarnedListener(evt -> {
    long earned = evt.getCreditsEarned();
});
```

### Async Operations
```java
CompletableFuture<AFKStatus> future = api.getAFKStatusAsync(player);
Map<Player, AFKStatus> batch = api.getBatchAFKStatus(players);
```

---

## Troubleshooting

| Problem | Solution |
|---|---|
| Players not being detected | Check `modules.core-detection.enabled: true` and that the world is not in `disabled-worlds` |
| Pattern detection false positives | Increase `repetitive-movement-threshold` or switch to the Conservative detection profile |
| Large AFK pool not detected | Ensure `modules.pattern-detection.enabled: true` and `large-pool-threshold` is configured |
| Credits not earned | Check `activity-threshold` and `minimum-session-minutes` settings |
| Holograms not showing | Install DecentHolograms or FancyHolograms and set `visual-effects.holograms.enabled: true` |
| Server transfer not working | Ensure BungeeCord or Velocity is configured and the target server name is correct |
| Folia errors | Plugin supports Folia natively — ensure you are running v3.0+ |

Enable debug logging for detailed output:
```yaml
debug: true
```

Support: `/afkplus help` in-game, or GitHub Issues.

---

## Version History

### v3.1 — Bug Fix Release
See the [full changelog](CHANGELOG_3_1.md) for details. Key fixes: water pool bypass, door/lever/cobble generator bypasses, warning countdown values, analytics and reward system now functional, hologram support.

### v3.0.4 — Performance
Reduced main thread load. More stable TPS on busy servers. No behavior or config changes.

### v3.0.3 — Bug Fixes
Fixed animal-riding water bypass (critical), blank lines from empty messages, and reload not applying language changes. Added `max-afk-duration-seconds` config key.

### v3.0.2 — Features & Fixes
Added `afk-action` global config key (KICK/TELEPORT/MARK_AFK_ONLY/NONE), exempt worlds, hex color support. Fixed messages always defaulting to English and zone teleport not working with custom zone names.

### v3.0 — Major Rewrite
In-game GUI, detection profiles, visual effects, 10 built-in languages, credit transfer and leaderboard, Vault and DiscordSRV integrations. JAR size reduced from 3.2 MB to ~400 KB.

<details>
<summary>v2.x and earlier (archived, no longer supported)</summary>

- **v2.9** — AFK time windows (daily hour ranges to pause punishments)
- **v2.8** — API overhaul with real live data endpoints
- **v2.7** — Critical bug fixes (credit teleportation, pattern detection threading)
- **v2.6** — Server transfer system (Bungee/Velocity)
- **v2.5** — AFK credit system
- **v2.4.2** — Full Folia 1.21.8 compatibility
- **v2.4** — Large AFK pool detection
- **v2.0** — Enterprise architecture rewrite (modular system, Folia support, Bedrock compatibility)

</details>

---

## Compatibility

| Platform | Support | Notes |
|---|---|---|
| **Paper** | ✅ Full | Recommended platform |
| **Spigot** | ✅ Full | |
| **Bukkit** | ✅ Full | |
| **Purpur / Pufferfish** | ✅ Full | |
| **Folia** | ✅ Full | Native regionized scheduler |
| **Bedrock** | ✅ Full | Via Geyser/Floodgate |

- **Minecraft versions:** 1.16 — 1.21.6+
- **Java:** 17+ required

---

## Installation

1. Download `AntiAFKPlus-3.1.jar` from [SpigotMC](https://www.spigotmc.org/resources/antiafkplus-premium.134494/).
2. Place in your server's `plugins/` folder.
3. Start or restart the server.
4. Configure with `/afkplus gui` or edit `config.yml` directly.
5. Customize messages in `plugins/AntiAFKPlus/languages/en.yml` (or your language).

---

## Author & License

**Developed by Koyere** — Licensed under the **MIT License**.

- **Source code:** Free and open source on [GitHub](https://github.com/koyere/AntiAFKPlus)
- **Compiled builds:** Available on [SpigotMC Premium](https://www.spigotmc.org/resources/antiafkplus-premium.134494/)
- **Support and maintenance:** Only provided for v3.0 and later. Versions prior to 3.0 are no longer maintained or supported.
