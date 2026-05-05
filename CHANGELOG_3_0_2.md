# AntiAFKPlus v3.0.2 — Changelog

---

## ✨ New Features

### Global AFK Action (`afk-action`)
Configure what happens when a player goes AFK **directly in config.yml** — no zone management or WorldGuard needed.

```yaml
afk-action:
  type: "TELEPORT"
  teleport-location: "world,250,76,30"
```

**Available actions:** `KICK`, `TELEPORT`, `MARK_AFK_ONLY`, `NONE`

### Exempt Worlds (`afk-action.exempt-worlds`)
Worlds where players get marked as AFK (tab list prefix, visual effects) but are **not** teleported or kicked. Perfect for spawn worlds.

```yaml
afk-action:
  type: "TELEPORT"
  teleport-location: "world,250,76,30"
  exempt-worlds:
    - "world_spawn"
```

Players in `world_spawn` will show as AFK in tab but won't be teleported. Players in other worlds will be teleported normally.

### Hex Color Support
Language files and messages now support hex colors (1.16+):
- `&#e5be01` format
- `{#e5be01}` format
- `#e5be01` format

Example in your language file:
```yaml
kick-action-teleport: "&#e5be01[Server] &fYou have been teleported."
```

---

## 🐛 Bug Fixes

### Messages always in English on first startup (Critical)
The `ConfigManager` loaded messages **before** the `LocalizationManager` was initialized, so all cached messages (kick warning, AFK state, teleport) defaulted to English. Even after restart, the cached values stayed in English. **Fixed:** Messages are now reloaded after the localization system is ready.

### Players kicked even with TELEPORT configured
Custom zone names (e.g., `spawn_final`, `lobby`) were ignored — only `"spawn"` worked. **Now any zone name works.**

### Zone management required WorldGuard for simple setups
`require-worldguard` defaulted to `true`. **Now defaults to `false`.**

### Warning times showed wrong numbers (29 instead of 30)
Warnings now display the **exact configured time** (30, 10, 5) instead of the calculated remainder that varied with the check interval.

### Warning titles on screen always in English
The on-screen title (`"⚠ AFK Warning"`) was hardcoded. **Now reads from your language file** (`warning-title-standard`, `warning-subtitle-standard`).

### Teleport message always in English
Hardcoded. **Now reads `kick-action-teleport` from your language file.**

### Pattern detection warnings in English
Hardcoded. **Now reads `suspicious-activity` and `pattern-violation-kicked` from your language file.**

### Bedrock welcome messages in English
Hardcoded. **Now reads from language file** (`bedrock-detected`, `bedrock-tip-mobile`, `bedrock-tip-console`, `bedrock-tip-desktop`).

### Language files incomplete
All 10 languages now fully translated. No English leftovers.

---

## 📋 Improvements

- Config documentation with inline examples for `afk-action` and `zone-management`
- Startup validation warns about misconfigured teleport locations
- Action priority: Zone Management > Global `afk-action` > Server Transfer > KICK

---

## 🔧 How to Update

1. Replace the plugin JAR
2. **Delete `plugins/AntiAFKPlus/languages/`** so new translated files are extracted
3. Restart the server
4. Add to your `config.yml`:

```yaml
afk-action:
  type: "TELEPORT"
  teleport-location: "world,250,76,30"
  exempt-worlds:
    - "world_spawn"
```

> **Note:** Back up your language files if you customized them. Re-apply changes after the new files are extracted.
