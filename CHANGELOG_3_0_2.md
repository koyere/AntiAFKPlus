# AntiAFKPlus v3.0.2 — Changelog

---

## ✨ New Features

### Global AFK Action (`afk-action`)
You can now configure what happens when a player goes AFK **directly in config.yml** — no zone management or WorldGuard needed.

```yaml
afk-action:
  type: "TELEPORT"
  teleport-location: "world,250,76,30"
```

**Available actions:**
- `KICK` — Kick the player from the server (default)
- `TELEPORT` — Teleport the player to specific coordinates
- `MARK_AFK_ONLY` — Only mark as AFK, no kick or teleport
- `NONE` — Disable AFK action entirely

This is the simplest way to set up AFK teleportation. Just set the type and coordinates, done.

---

## 🐛 Bug Fixes

### Players kicked even with TELEPORT configured (Critical)
If you used zone management with a custom zone name (e.g., `spawn_final`, `lobby`, `hub`), the plugin ignored it and kicked the player anyway. Only a zone named exactly `"spawn"` worked. **Now any zone name works correctly.**

### Zone management required WorldGuard even for simple setups
`require-worldguard` defaulted to `true`, making zone management unusable without WorldGuard. **Now defaults to `false`.** If you need region-based zones, set it back to `true`.

### Teleport message always showed in English
The teleport message (`"Teleported due to AFK timeout"`) was hardcoded in English and ignored the language files. **Now it reads from your language file** using the `kick-action-teleport` key.

### Language files not fully translated
All 10 language files (en, es, fr, de, pt, ru, zh, ja, ko, it) had many messages still in English. **All messages are now fully translated** in every language.

---

## 📋 Improvements

### Better config documentation
- The `afk-action` section includes usage examples directly in config.yml
- The `zone-management` section now explains how it works with and without WorldGuard
- All available `kick-action` values are documented inline

### Startup validation warnings
The plugin now warns you in the console at startup if:
- `afk-action.type` has an unrecognized value
- `afk-action.type` is `TELEPORT` but `teleport-location` is empty
- A zone has `kick-action: TELEPORT` but no `teleport-location`

### Action priority chain
When a player exceeds the AFK time limit, the action is resolved in this order:

| Priority | Source | When it applies |
|----------|--------|-----------------|
| 1 | Zone Management | `zone-management.enabled: true` and player is in a configured zone |
| 2 | Global AFK Action | `afk-action.type` is set to something other than KICK |
| 3 | Server Transfer | `server-transfer.enabled: true` with a valid `target-server` |
| 4 | Default | KICK |

---

## 🔧 How to Update

1. Replace the plugin JAR with the new version
2. **Delete the `plugins/AntiAFKPlus/languages/` folder** so the new translated files are extracted
3. Restart the server
4. If you want the new global teleport feature, add this to your `config.yml`:

```yaml
afk-action:
  type: "TELEPORT"
  teleport-location: "world,250,76,30"
```

> **Note:** If you previously customized your language files, back them up before deleting the folder. You can re-apply your changes after the new files are extracted.

> **Tip:** If you were using `zone-management` only for a simple teleport, you can now disable it and use `afk-action` instead — much simpler.
