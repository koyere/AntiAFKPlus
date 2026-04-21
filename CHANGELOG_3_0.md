# AntiAFKPlus v3.0 Premium — Changelog

## Overview
Complete rewrite from free to premium ($5.99). Optimized codebase, 10 new features, 10-language support, in-game GUI, unified message system, and JAR size reduced from 3.2 MB to 396 KB.

---

## New Features

### In-Game GUI Configuration (`/afkplus gui`)
- Full settings panel accessible in-game — no need to edit YAML files
- **Main Menu (54 slots):** Plugin info head, Detection Settings, Module Toggles, Credit System toggle, Performance stats (live TPS/memory/operations), Detection Profile selector, Debug toggle, Reload Config, AFK player count
- **Detection Settings submenu:** Displays water circle radius, min samples, max violations, pattern analysis interval. Toggles for linear movement exclusion, large pool detection, keystroke timeout. Back button
- **Module Toggles submenu:** 8 feature modules displayed as green wool (enabled) / red wool (disabled). Click to toggle — changes persist to config.yml and reload instantly
- All GUI feedback messages are localized via language files
- Requires permission `antiafkplus.reload`

### Detection Profiles
- One-click preset system accessible from the GUI main menu
- **Conservative:** max-violations=12, threshold=0.98, min-samples=50, grace-period=90s — fewest false positives, most tolerant
- **Balanced:** max-violations=8, threshold=0.95, min-samples=40, grace-period=60s — default, recommended for most servers
- **Aggressive:** max-violations=4, threshold=0.85, min-samples=25, grace-period=30s — strictest, catches more AFK bots
- Auto-detects current profile based on config values
- Cycles with a single click: Conservative → Balanced → Aggressive → Conservative

### Visual Effects System
- **Particles:** Configurable particle effects above AFK players. Supports any Bukkit particle type (CLOUD, HEART, FLAME, etc.), configurable count, speed, and XYZ offsets. Runs every second via async task
- **Tab List Prefix:** Automatically prepends `[AFK]` to player names in the tab list when they go AFK. Restores original name when they return. Configurable prefix text and color
- **Display Name Prefix:** Same as tab list but for the player's display name (visible in chat). Independent toggle
- Listens to `PlayerAFKStateChangeEvent` for automatic apply/remove
- Cleans up on player disconnect and plugin shutdown
- Enable: set `modules.visual-effects.enabled: true` in config, then configure under `visual-effects:` section
- Config options:
  ```yaml
  visual-effects:
    particles:
      enabled: true
      type: "CLOUD"        # Any Bukkit Particle enum value
      count: 5
      speed: 0.02
      offset-x: 0.3
      offset-y: 0.5
      offset-z: 0.3
    tab-list:
      enabled: true
      afk-prefix: "&7[AFK] "
    name-tags:
      enabled: false
      afk-prefix: "&7[AFK] "
  ```

### Credit System Enhancements
- **Transfer:** `/afkcredits transfer <player> <minutes>` — players can send credits to each other. Validates balance, max credits of recipient, prevents self-transfer. Transactions recorded in SQL history if enabled
- **Leaderboard:** `/afkcredits top [limit]` — displays top credit holders sorted by balance (default 10, max 50)
- **Multiplier Events:** `/afkplus event credits <multiplier> <duration_minutes>` — admins can start temporary credit multiplier events (e.g. `2 60` = double credits for 1 hour). Broadcasts to all online players. Validates range (0.1-10x, 1-1440 min)
- **New placeholder:** `%antiafkplus_credits_rank%` — shows the player's position in the credit leaderboard

### PlaceholderAPI Integration (Enhanced)
All placeholders require PlaceholderAPI installed on the server.

| Placeholder | Description | Example Output |
|---|---|---|
| `%antiafkplus_status%` | AFK status text (localized) | `AFK`, `ACTIVE`, `MANUAL AFK` |
| `%antiafkplus_afktime%` | Seconds since last activity | `120` |
| `%antiafkplus_credits%` | Credit balance in minutes | `45` |
| `%antiafkplus_credits_hours%` | Credit balance in hours | `2` |
| `%antiafkplus_max_credits%` | Maximum credits for player | `120` |
| `%antiafkplus_credit_ratio%` | Credit earning ratio | `5:1` |
| `%antiafkplus_in_afk_zone%` | Whether player is in AFK zone | `true` / `false` |
| `%antiafkplus_credits_expire_days%` | Days until credits expire | `5` |
| `%antiafkplus_credits_rank%` | Position in credit leaderboard | `3` |

**Tip:** To show `[AFK]` prefix in tab list or chat via PlaceholderAPI, edit your language file:
```yaml
# In languages/en.yml (or your language)
placeholder-status-afk: "&7[AFK] "
placeholder-status-active: ""
placeholder-status-manual-afk: "&e[AFK] "
```
Then use `%antiafkplus_status%` in your tab/chat plugin (TAB, LuckPerms, etc.).

Alternatively, the built-in Visual Effects system adds `[AFK]` to tab list automatically without PlaceholderAPI — see Visual Effects section above.

### Analytics & Performance Dashboard
- `/afkplus status` — shows: plugin version, uptime, online/AFK player count, enabled modules count, pattern detection status, credit system status, TPS, memory usage. Requires `antiafkplus.stats`
- `/afkplus performance` — shows: server TPS, average execution time, total operations, memory usage, cache entries, tracked components, high/low activity player counts. Requires `antiafkplus.stats`

### Vault Economy Integration
- Reflection-based — no compile-time dependency, works if Vault is installed
- API methods: `getBalance()`, `withdraw()`, `deposit()`, `has()`
- Enable: `integrations.vault.enabled: true`
- Accessible via `plugin.getVaultIntegration()` for other plugins

### DiscordSRV Integration
- Reflection-based — no compile-time dependency, works if DiscordSRV is installed
- Automatically sends AFK state change notifications to the main Discord channel
- Configurable: `integrations.discordsrv.send-afk-notifications: true`
- Enable: `integrations.discordsrv.enabled: true`

### Internationalization (10 Languages)
- **Unified message system:** `messages.yml` has been removed. All messages come from `languages/*.yml` files
- **Server language:** controlled by `internationalization.default-language` in config.yml (e.g. `"es"` for Spanish)
- **10 built-in languages:** English (en), Spanish (es), French (fr), German (de), Portuguese (pt), Russian (ru), Chinese (zh), Japanese (ja), Korean (ko), Italian (it)
- **~150 message keys** per language file covering: general, AFK state, commands, pattern detection, autoclicker, warnings, kick actions, protection, server transfer, admin notifications, debug, placeholders, statistics, zones, credit system (with all sub-sections), and GUI feedback
- **Auto-extraction:** Language files are extracted to `plugins/AntiAFKPlus/languages/` on first startup. Admins can edit them directly or add new languages
- **Player language detection:** `LocalizationManager` auto-detects player client locale for PlaceholderAPI responses
- **Custom languages:** Admins can create additional `.yml` files in the `languages/` folder following the same structure

---

## Commands Reference

| Command | Description | Permission |
|---|---|---|
| `/afk` | Toggle manual AFK mode | `antiafkplus.afk` |
| `/afk list` | List AFK players | `antiafkplus.list` |
| `/afk status [player]` | Check player AFK status | `antiafkplus.status.check` |
| `/afkplus reload` | Reload configuration | `antiafkplus.reload` |
| `/afkplus gui` | Open settings GUI | `antiafkplus.reload` |
| `/afkplus status` | Plugin status & analytics | `antiafkplus.stats` |
| `/afkplus performance` | Performance metrics | `antiafkplus.stats` |
| `/afkplus event credits <mult> <min>` | Start credit multiplier event | `antiafkplus.reload` |
| `/afkcredits` | Check your credit balance | `antiafkplus.credit.check` |
| `/afkcredits transfer <player> <min>` | Transfer credits | `antiafkplus.credit.transfer` |
| `/afkcredits top [limit]` | Credit leaderboard | `antiafkplus.credit.check` |
| `/afkcredits history <player> [limit]` | Credit transaction history | `antiafkplus.credit.admin` |
| `/afkcredits give/take/set <player> <min>` | Admin credit management | `antiafkplus.credit.admin` |
| `/afkcredits reset <player>` | Reset player credits | `antiafkplus.credit.admin` |
| `/afkback` | Return from AFK zone | `antiafkplus.credit.return` |

---

## Improvements

### Configuration
- config.yml reduced from 1033 to 580 lines (44% reduction)
- Removed all dead/unused config sections (migration-info, compatibility, technical, analytics.web-dashboard)
- Consolidated duplicate toggles: pattern detection (was 3 toggles, now 1), autoclick (was 2 locations, now 1), credit system (was 2 toggles, now 1)
- Standardized time units to seconds in config (was mixed ms/seconds). Keys renamed: `pattern-analysis-interval-ms` → `pattern-analysis-interval-seconds`, `keystroke-timeout-ms` → `keystroke-timeout-seconds`, `activity-grace-period-ms` → `activity-grace-period-seconds`. Legacy ms keys still supported as fallback
- Removed unimplemented integration stubs (vault, discordsrv) — re-added with real reflection-based code
- JAR size reduced from 3.2 MB to 396 KB (removed unnecessary Guava dependency that was bundled but never used)

### Code Quality
- Removed dead code: unused `afkLogger` field, `migrationRequired` flag, `MIN_MIGRATION_VERSION` constant, empty `ModuleManager` loops, orphaned `Module` framework
- Activity scoring weights now functional — config values in `activity-scoring-weights` section are actually applied to the score calculation via `PlayerActivityData`
- Movement detection thresholds now configurable — `movement-detection-settings` values are loaded into `MovementListener` (were hardcoded constants)
- Event system toggles now functional — `event-system` section controls whether Bukkit events are fired (disabling unused events improves performance)
- Module states reload on config change — `ModuleManager.reloadModuleStates()` syncs in-memory state with config.yml
- All `printStackTrace()` replaced with `Logger.log(Level.SEVERE, message, exception)`
- Java 17 pattern matching (`instanceof` patterns) used where applicable
- All comments standardized to English

### Detection System
- Single toggle: `modules.pattern-detection.enabled` (removed confusing `enhanced-detection` section with 3 overlapping toggles)
- Autoclick settings consolidated under `modules.autoclick-detection` (removed legacy root keys)

---

## Breaking Changes

| Change | Migration |
|---|---|
| `messages.yml` removed | Edit `languages/en.yml` (or your language file) instead |
| `enhanced-detection` section removed | Use `modules.pattern-detection.enabled` |
| `autoclick-detection` root key removed | Use `modules.autoclick-detection.enabled` |
| `autoclick-detection-settings` root section removed | Settings now under `modules.autoclick-detection` |
| `modules.credit-system.enabled` removed | Use `credit-system.enabled` only |
| Time keys renamed (`*-ms` → `*-seconds`) | Legacy `-ms` keys still work as fallback |
| `migration-info` section removed | No action needed |
| `compatibility` section removed | No action needed |
| `technical` section removed | No action needed |

---

## File Changes Summary
- **New Java classes:** `GUIManager`, `GUIType`, `VisualEffectsManager`, `VaultIntegration`, `DiscordSRVIntegration`
- **New resources:** 10 language files (`languages/en.yml` through `languages/it.yml`)
- **New docs:** `CHANGELOG_3_0.md`, `LICENSE` (commercial)
- **Modified Java classes:** `AntiAFKPlus`, `ConfigManager`, `AFKManager`, `AFKPlusCommand`, `AFKCreditsCommand`, `CreditManager`, `ModuleManager`, `MovementListener`, `PatternDetector`, `PlaceholderHook`, `LocalizationManager`
- **Modified resources:** `config.yml` (rewritten), `plugin.yml` (v3.0), `pom.xml` (v3.0, Guava removed)
- **Deleted:** `messages.yml`
- **Total Java files:** 70
- **Total resources:** 12 (config.yml + plugin.yml + 10 languages)
- **JAR size:** 396 KB
