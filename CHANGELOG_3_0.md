# AntiAFKPlus v3.0 Premium — What's New

> Complete rewrite. New features, in-game GUI, 10 languages, optimized to 396 KB.

---

## 🖥️ In-Game GUI — Configure everything without editing files

Open with `/afkplus gui` (requires `antiafkplus.reload` permission).

**Main Menu** — Central hub with 11 interactive buttons:
- Detection Settings, Module Toggles, General Settings, Zone Settings, Reward Settings
- Credit System toggle, Detection Profile selector, Debug toggle, Reload Config
- Performance stats (live TPS, memory, operations)
- Language selector — change the server language instantly

**General Settings** — Adjust AFK timing without restarting:
- Default AFK Time: click to add 30 seconds, sneak+click to subtract
- Check Interval: click +1s, sneak+click -1s
- Max Voluntary AFK Time: click +60s, sneak+click -60s
- Toggle: Block item pickup while AFK
- Toggle: Broadcast AFK state changes to chat

**Detection Settings** — View and toggle pattern detection features:
- Displays current values: water circle radius, min samples, max violations, analysis interval
- Toggle: Linear movement exclusion (reduces false positives)
- Toggle: Large pool detection
- Toggle: Keystroke timeout detection

**Module Toggles** — Enable/disable 8 feature modules with one click:
- Pattern Detection, Autoclick Detection, Player Protection, AFK Zones
- Reward System, Visual Effects, Database, Analytics
- Changes apply immediately — the wool color switches from red to green (or vice versa)

**Zone Settings** — Configure AFK zone management:
- Toggle: Zone management on/off
- Toggle: Require WorldGuard
- Toggle: Default AFK allowed in zones
- Adjust: Default zone timeout (click/sneak+click)
- Toggle: Region inheritance from parent WorldGuard regions

**Reward Settings** — Configure the AFK reward system:
- Toggle: Reward system on/off
- Toggle: Require Vault economy
- Adjust: Max daily rewards (±10 per click)
- Adjust: Required active time before rewards (±5 min per click)
- Toggle: IP-based limits to prevent alt abuse

**Language Selector** — 10 languages displayed as books:
- The active language shows as an enchanted book with a green ✔
- Click any language to switch instantly — the entire plugin changes language immediately
- No restart needed

All changes made in the GUI are saved to `config.yml` automatically and take effect immediately.

---

## 📋 Detection Profiles — One-click sensitivity presets

Instead of manually adjusting 4+ detection parameters, choose a preset:

| Profile | Max Violations | Threshold | Min Samples | Grace Period | Best For |
|---|---|---|---|---|---|
| **Conservative** | 12 | 0.98 | 50 | 90s | Survival servers, fewer false positives |
| **Balanced** | 8 | 0.95 | 40 | 60s | Most servers (default) |
| **Aggressive** | 4 | 0.85 | 25 | 30s | Minigame servers, strict anti-AFK |

How to use: Open the GUI (`/afkplus gui`) and click the book icon "Detection Profile". Each click cycles to the next profile. The change applies immediately.

---

## ✨ Visual Effects — See who's AFK at a glance

Three visual indicators for AFK players, all independently configurable:

**Particles** — Floating particles above AFK players' heads:
```yaml
visual-effects:
  particles:
    enabled: true
    type: "CLOUD"       # Any Minecraft particle: CLOUD, HEART, FLAME, SMOKE, etc.
    count: 5
    speed: 0.02
    offset-x: 0.3
    offset-y: 0.5
    offset-z: 0.3
```

**Tab List Prefix** — `[AFK]` appears before the player's name in the tab list:
```yaml
visual-effects:
  tab-list:
    enabled: true
    afk-prefix: "&7[AFK] "
```

**Display Name Prefix** — `[AFK]` appears before the player's name in chat:
```yaml
visual-effects:
  name-tags:
    enabled: false
    afk-prefix: "&7[AFK] "
```

To enable: set `modules.visual-effects.enabled: true` in config.yml, or toggle it in the GUI under Module Toggles.

Effects apply automatically when a player goes AFK and are removed when they return. Names are restored on disconnect and plugin shutdown.

---

## 💰 Credit System — New features

### Transfer credits between players
Players can send their AFK credits to others:
```
/afkcredits transfer Steve 30
```
Sends 30 minutes of credits to Steve. Validates: sufficient balance, recipient's max credits, prevents self-transfer.

Permission: `antiafkplus.credit.transfer`

### Credit leaderboard
See who has the most credits:
```
/afkcredits top        → shows top 10
/afkcredits top 25     → shows top 25
```

### Credit multiplier events
Admins can start temporary bonus events:
```
/afkplus event credits 2 60     → double credits for 1 hour
/afkplus event credits 3 30     → triple credits for 30 minutes
```
Broadcasts to all online players when activated. Multiplier range: 0.1x to 10x. Duration: 1 to 1440 minutes.

Permission: `antiafkplus.reload`

---

## 📊 PlaceholderAPI — All available placeholders

Requires PlaceholderAPI installed on the server.

| Placeholder | Returns | Example |
|---|---|---|
| `%antiafkplus_status%` | AFK status text | `AFK`, `ACTIVE`, `MANUAL AFK` |
| `%antiafkplus_afktime%` | Seconds since last activity | `120` |
| `%antiafkplus_credits%` | Credit balance (minutes) | `45` |
| `%antiafkplus_credits_hours%` | Credit balance (hours) | `2` |
| `%antiafkplus_max_credits%` | Max credits for player | `120` |
| `%antiafkplus_credit_ratio%` | Earning ratio | `5:1` |
| `%antiafkplus_in_afk_zone%` | In AFK zone? | `true` / `false` |
| `%antiafkplus_credits_expire_days%` | Days until credits expire | `5` |
| `%antiafkplus_credits_rank%` | Leaderboard position | `3` |

**Tip — Show `[AFK]` in tab list via PlaceholderAPI:**

Edit your language file (`languages/en.yml`):
```yaml
placeholder-status-afk: "&7[AFK] "
placeholder-status-active: ""
placeholder-status-manual-afk: "&e[AFK] "
```
Then use `%antiafkplus_status%` in your tab/chat plugin (TAB, LuckPerms, etc.). When the player is active, it returns empty — when AFK, it shows `[AFK]`.

Alternatively, the built-in Visual Effects tab list prefix does this automatically without PlaceholderAPI.

---

## 📈 Plugin Status & Performance

### `/afkplus status` — Quick overview
Shows: plugin version, server uptime, online/AFK player count, enabled modules, pattern detection and credit system status, TPS, memory usage.

Permission: `antiafkplus.stats`

### `/afkplus performance` — Detailed metrics
Shows: server TPS, average execution time per operation, total operations count, memory usage, cache entries, tracked components, high/low activity player counts.

Permission: `antiafkplus.stats`

Both are also visible in the GUI main menu (Performance item shows live stats on hover).

---

## 🔗 Integrations

### Vault Economy
Connects with any Vault-compatible economy plugin (EssentialsX, CMI, etc.). Detected automatically via reflection — no extra dependencies needed.

Enable: `integrations.vault.enabled: true`

### DiscordSRV
Sends AFK notifications to your Discord server automatically. When a player goes AFK or returns, a message is posted to the main Discord channel.

Enable: `integrations.discordsrv.enabled: true`

Configure: `integrations.discordsrv.send-afk-notifications: true`

Both integrations are optional — the plugin works perfectly without them.

---

## 🌍 10 Languages — Full internationalization

Every message in the plugin — commands, warnings, GUI text, placeholders, credit system, admin notifications — comes from language files.

**Included languages:**

| Code | Language | Code | Language |
|---|---|---|---|
| `en` | English | `ru` | Русский |
| `es` | Español | `zh` | 中文 |
| `fr` | Français | `ja` | 日本語 |
| `de` | Deutsch | `ko` | 한국어 |
| `pt` | Português | `it` | Italiano |

**How to change the server language:**

Option 1 — In the GUI: `/afkplus gui` → click the book icon "Language" → click your language. Changes instantly.

Option 2 — In config.yml:
```yaml
internationalization:
  default-language: "es"    # Change to any language code
```
Then `/afkplus reload`.

**How to customize messages:**

Edit the language file directly: `plugins/AntiAFKPlus/languages/en.yml` (or your language). All messages are there — AFK state, warnings, commands, GUI text, credit system, everything.

**How to add a new language:**

Create a new `.yml` file in `plugins/AntiAFKPlus/languages/` (e.g. `nl.yml`), copy the structure from `en.yml`, translate the messages, and set `default-language: "nl"` in config.yml.

---

## ⚙️ Configuration improvements

- **Config.yml reduced 44%** — from 1033 to 580 lines. Cleaner, less intimidating, better organized
- **Single toggle per feature** — no more confusing duplicate toggles. Each feature has one on/off switch
- **Time units standardized** — all config values use seconds (was a mix of seconds and milliseconds)
- **Activity scoring now works** — the `activity-scoring-weights` section actually affects detection. Adjust how much each activity type (movement, jumping, commands, chat, etc.) counts toward the player's activity score
- **Movement thresholds now configurable** — `movement-detection-settings` values are actually applied. Adjust sensitivity for micro-movement, head rotation, and jump spam detection
- **Event system toggles work** — disable unused Bukkit events in `event-system` section to improve performance on servers that don't use them
- **Plugin size: 396 KB** — down from 3.2 MB (removed unnecessary bundled library)

---

## 📝 All Commands

| Command | What it does | Permission |
|---|---|---|
| `/afk` | Toggle your AFK status | `antiafkplus.afk` |
| `/afk list` | See who's AFK | `antiafkplus.list` |
| `/afk status [player]` | Check if a player is AFK | `antiafkplus.status.check` |
| `/afkplus reload` | Reload config and language files | `antiafkplus.reload` |
| `/afkplus gui` | Open the settings GUI | `antiafkplus.reload` |
| `/afkplus status` | Plugin status and analytics | `antiafkplus.stats` |
| `/afkplus performance` | Detailed performance metrics | `antiafkplus.stats` |
| `/afkplus event credits <mult> <min>` | Start a credit multiplier event | `antiafkplus.reload` |
| `/afkcredits` | Check your credit balance | `antiafkplus.credit.check` |
| `/afkcredits transfer <player> <min>` | Send credits to another player | `antiafkplus.credit.transfer` |
| `/afkcredits top [limit]` | Credit leaderboard | `antiafkplus.credit.check` |
| `/afkcredits history <player> [limit]` | View credit transaction history | `antiafkplus.credit.admin` |
| `/afkcredits give/take/set <player> <min>` | Admin: modify player credits | `antiafkplus.credit.admin` |
| `/afkcredits reset <player>` | Admin: reset player credits | `antiafkplus.credit.admin` |
| `/afkback` | Return from AFK zone | `antiafkplus.credit.return` |

---

## ⚠️ Upgrading from v2.x

| What changed | What to do |
|---|---|
| `messages.yml` no longer exists | Your messages are now in `languages/en.yml`. Edit that file instead |
| `enhanced-detection` section removed | Pattern detection is controlled by `modules.pattern-detection.enabled` |
| `autoclick-detection` root key removed | Now under `modules.autoclick-detection.enabled` |
| Time keys renamed (`*-ms` → `*-seconds`) | Old `-ms` keys still work as fallback, but update when convenient |
| `modules.credit-system.enabled` removed | Use `credit-system.enabled` only |

The plugin auto-migrates your config on first startup. A backup is recommended before upgrading.

---

## 🔧 Compatibility

### Server Platforms
The plugin auto-detects your server platform and adapts automatically:

| Platform | Supported | Notes |
|---|---|---|
| **Bukkit** | ✅ | Base compatibility |
| **Spigot** | ✅ | Full support |
| **Paper** | ✅ | Recommended. Built against Paper API 1.20.1 |
| **Purpur** | ✅ | Full support |
| **Folia** | ✅ | Full support via regionized scheduler (reflection-based, no hard dependency) |

### Minecraft Versions
- **Minimum:** 1.16 (`api-version: '1.16'` in plugin.yml)
- **Tested up to:** 1.21.x
- **Java requirement:** Java 17 or higher

### Bedrock Edition
Bedrock players connecting via Geyser/Floodgate are detected automatically. Detection methods (in priority order):
1. Floodgate API (reflection-based)
2. Geyser API (reflection-based)
3. Username prefix (configurable)
4. Client brand detection

No extra configuration needed — enable `bedrock-compatibility.enabled: true` (default).

### Optional Plugin Integrations
These plugins are **not required** — AntiAFKPlus works standalone. If detected, extra features activate automatically:

| Plugin | What it enables |
|---|---|
| **PlaceholderAPI** | 9 placeholders (`%antiafkplus_status%`, credits, etc.) |
| **WorldGuard** | Zone-based AFK management with region flags |
| **Vault** | Economy integration (balance, withdraw, deposit) |
| **DiscordSRV** | AFK notifications sent to Discord channels |
| **Floodgate/Geyser** | Bedrock player detection and UI adaptations |

All integrations use reflection — no compile-time dependencies, no extra JARs needed.

---

## 📦 Installation

### Fresh install
1. Download `antiafkplus-3.0.0.jar` (396 KB)
2. Place in your server's `plugins/` folder
3. Start or restart the server
4. The plugin creates: `config.yml` and `languages/` folder with 10 language files
5. Configure in-game with `/afkplus gui` or edit `config.yml`

### Upgrading from v2.x
1. **Back up** your current `plugins/AntiAFKPlus/` folder
2. Replace the old JAR with `antiafkplus-3.0.0.jar`
3. Start the server — the plugin detects the old config version and migrates automatically
4. Your `config.yml` settings are preserved. New sections are added with defaults
5. `messages.yml` is no longer used — your messages are now in `languages/en.yml` (or your language)
6. Review the new `languages/en.yml` file and customize any messages you had changed in the old `messages.yml`
7. Optional: open `/afkplus gui` to explore the new settings

### Compatibility with v2.x configs
- The plugin reads the `version` field in `config.yml` to detect old configs
- If the config has fewer than 5 main sections, it regenerates completely (backup first!)
- Legacy config keys (`pattern-analysis-interval-ms`, `autoclick-detection-settings`, etc.) are still read as fallback — you don't need to rename them immediately
- The `enhanced-detection` section is ignored if present (no errors, just unused)

---

## 🔌 API Changes for Developers

The public API interface (`AntiAFKPlusAPI`) is **backward compatible** — no methods were removed or had their signatures changed. Plugins using the v2.x API will continue to work without modification.

### What's the same
- All existing API methods (`isAFK()`, `getAFKStatus()`, `getActivityStatistics()`, etc.) work identically
- Event classes (`PlayerAFKStateChangeEvent`, `PlayerAFKWarningEvent`, `PlayerAFKKickEvent`, `PlayerAFKPatternDetectedEvent`) are unchanged
- The API is accessed the same way: `AntiAFKPlusAPI api = AntiAFKPlusAPI.getInstance();`

### What's new in the API
- **Event system toggles:** Events can now be disabled via config (`event-system` section). If disabled, `Bukkit.getPluginManager().callEvent()` is not called, but the event object is still created internally. Plugins listening for these events should check if the feature is enabled
- **Credit system methods:** `CreditManager` now has `transferCredits(Player from, Player to, long minutes)` and `getTopCredits(int limit)`
- **Module state reload:** `ModuleManager.reloadModuleStates()` syncs in-memory module states with config after a reload
- **Visual effects manager:** Accessible via `plugin.getVisualEffectsManager()` (may be null if module is disabled)
- **GUI manager:** Accessible via `plugin.getGUIManager()`
- **Vault integration:** Accessible via `plugin.getVaultIntegration()` (may be null)
- **DiscordSRV integration:** Accessible via `plugin.getDiscordSRVIntegration()` (may be null)

### Maven dependency
```xml
<dependency>
    <groupId>me.koyere</groupId>
    <artifactId>antiafkplus</artifactId>
    <version>3.0.0</version>
    <scope>provided</scope>
</dependency>
```

### API version
The API version constant in the interface default method still returns `"2.0.0"` for backward compatibility. The plugin version is `3.0.0` and can be retrieved via `AntiAFKPlus.getInstance().getPluginVersion()`.

---

## 📊 Technical Summary

| Metric | v2.9.5 | v3.0.0 |
|---|---|---|
| JAR size | ~3.2 MB | 396 KB |
| Config lines | 1033 | 580 |
| Languages | 1 (English only) | 10 |
| GUI menus | 0 | 8 |
| Commands | 4 | 15 (4 base + 11 subcommands) |
| Placeholders | 6 | 9 |
| Integrations | 2 (PAPI, WorldGuard) | 5 (+Vault, DiscordSRV, Geyser) |
| Java version | 17 | 17 |
| Min MC version | 1.16 | 1.16 |
| Folia support | Yes | Yes |
| Bedrock support | Yes | Yes |
