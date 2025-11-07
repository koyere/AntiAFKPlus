# AntiAFKPlus v2.8 — API Overhaul

Release type: Feature & API Upgrade
Compatibility: Minecraft 1.16 – 1.21.10 | Java 17+

---

## What's New

### 📊 Real Data Everywhere
- Activity, history, and statistics endpoints now return live server data.
- `getActivityInfo`, `getActivityStatistics`, `getAFKStatistics`, `getPlayerStatistics`, and `getAFKHistory` consume the internal tracking system (no more placeholders).

### 🚦 Reliable Events
- AFK warnings and pattern detections are always delivered to the public API.
- Listeners (`registerWarningListener`, `registerPatternDetectionListener`) can modify messages or cancel actions before the plugin kicks/teleports players.

### 🗺️ Zone Awareness
- New helpers (`isAFKAllowedAt`, `getAFKZoneAt`) resolve `zone-management` settings and WorldGuard regions on the fly.
- Worlds can be toggled at runtime with `setAFKDetectionEnabled`, persisting the change to `config.yml`.

### ⚙️ Ease of Use
- README/API docs include updated examples for the new endpoints (activity, zones, stats).
- API version bumped to `2.8`, ready for your integrations.

---

## Upgrade Notes
1. Replace the old JAR with `AntiAFKPlus v2.8`.
2. Update your dependency to `2.8` (Maven/Gradle snippet in the README).
3. Review the new API examples and adjust your listeners if you want to react to warnings/patterns.

> No configuration changes are required. Everything works with your existing `config.yml` / `messages.yml`.
