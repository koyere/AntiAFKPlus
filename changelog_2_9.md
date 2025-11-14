# AntiAFKPlus v2.9 — AFK Time Windows

Release type: Feature Update
Compatibility: Minecraft 1.16 – 1.21.10 | Java 17+

---

## What's New

### 🕒 AFK Time Windows
- **Configurable Time Ranges**: Define daily hour ranges where AFK actions are paused or modified
- **Multiple Behaviors**: Choose how the plugin handles AFK detection during configured windows:
  - `SKIP_ACTIONS`: Suppress all warnings, countdowns, and kicks
  - `MESSAGE_ONLY`: Suppress actions and send informational message
  - `EXTEND_THRESHOLD`: Delay final action by configurable seconds
  - `DEFAULT`: Normal AFK enforcement (used outside windows)

- **Timezone Support**: Use server timezone or specify any IANA timezone ID (e.g., `America/New_York`, `Europe/London`)
- **Wrap-Around Ranges**: Support for overnight ranges (e.g., `22:00-02:00`)
- **Bypass Permission**: Staff can ignore windows with `antiafkplus.window.bypass`

### 📝 Configuration Example
```yaml
afk-windows:
  enabled: true
  timezone: "SERVER"              # or explicit IANA ID like "America/New_York"
  ranges:
    - "08:00-12:00"
    - "20:00-23:00"
  behavior-inside-window: "SKIP_ACTIONS"
  behavior-outside-window: "DEFAULT"
  extend-seconds: 900             # used when behavior is EXTEND_THRESHOLD
  bypass-permission: "antiafkplus.window.bypass"
```

### 🎮 Use Cases
- **Peak Hours Protection**: Disable AFK kicks during busy server hours
- **Event Periods**: Pause AFK enforcement during special events
- **Off-Hours Flexibility**: Allow extended AFK times during late night/early morning
- **Regional Adaptation**: Use different timezones for international servers

### 🔧 Technical Details
- **TimeWindowService**: Minute-level granularity with intelligent caching
- **AFKManager Integration**: Evaluates windows before warnings and final actions
- **Countdown/Transfer Compatibility**: Pipelines and sequences respect window controls
- **Reload Support**: `/afkplus reload` updates window settings without restart

### ✨ New Permission
- `antiafkplus.window.bypass`: Allows players to ignore AFK windows and receive normal enforcement
  - Automatically included in `antiafkplus.*` wildcard
  - Useful for staff testing and development accounts

---

## Compatibility

- **Minecraft**: 1.16 – 1.21.10
- **Platforms**: Bukkit, Spigot, Paper, Purpur, Folia
- **Java**: 17+
- **Backward Compatible**: All existing configurations work without changes

---

## Upgrade Notes

1. Replace the old JAR with `AntiAFKPlus v2.9`
2. Optional: Configure `afk-windows` section in `config.yml` to enable time-based protection
3. Optional: Customize the `afk-window-active` message in `messages.yml` for localization
4. Update your dependency to `2.9` if using the API:
   ```xml
   <dependency>
       <groupId>com.github.koyere</groupId>
       <artifactId>AntiAFKPlus</artifactId>
       <version>2.9</version>
       <scope>provided</scope>
   </dependency>
   ```

> No configuration changes are required. The plugin works with your existing setup. AFK windows are **disabled by default**.

---

## For Developers

**API Version**: 2.9 (unchanged from 2.8, no breaking changes)

The time window system is internal and transparent to the API. Developers using the API will benefit from the improved flexibility without code changes.

---

**Version**: 2.9
**Release Date**: 2025 (planned)
**Compatibility**: Minecraft 1.16 - 1.21.10
**Java**: 17+
