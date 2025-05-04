
---

## 🗒️ `CHANGELOG.md` para la versión 1.1

**Ruta:** `AntiAFKPlus/CHANGELOG.md`

# AntiAFKPlus - Changelog

## [1.1] - 2025-05-04

### Added
- Configurable max voluntary AFK time (`max-voluntary-afk-time-seconds`)
- Separated `messages.yml` with full color support
- New command `/afk list` to view AFK players (with permission)
- New config value: `list-command-permission`
- New messages: `afk-list-header`, `afk-list-format`, `afk-list-empty`
- Public API (`AntiAFKPlusAPI`) for external plugin integration
- Enhanced logic to prevent auto-kick on player join
- Auto-clear AFK on movement or login activity

### Fixed
- Broadcasts not showing when entering/leaving AFK
- Kick message not using correct config value
- Warnings not appearing as expected
- Persistent kick loop after rejoin
- Reload system not reflecting changes in messages/config

---

## [1.0] - Initial Release

- Kick players after a configurable time
- Per-permission AFK times
- Bypass and reload support
