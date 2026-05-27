# AntiAFKPlus v3.1 — Changelog

---

## Bug Fixes

- **Console spam from activity logs** — `[Activity]` log lines no longer appear in the console by default. They only show when `debug: true` is set in `config.yml`. Core AFK messages (player went AFK, returned, kicked) always appear regardless.

- **Blank chat lines when a message is set to `""`** — Setting any message to an empty string in the language file no longer sends a blank line to the player's chat. It is silently skipped.

- **AFK pool bypass via water current** — Players inside a water AFK pool could avoid detection indefinitely because water current movement was counted as real activity. Fixed: only manual input (swimming, looking around, jumping, sprinting) counts as activity.

- **Door spam-click bypass** — Repeatedly clicking a door (manually or with an auto-clicker) was resetting the AFK timer indefinitely. Fixed.

- **"Use Item: toggle" bypass on levers and buttons** — Holding right-click on levers or buttons prevented AFK detection. Fixed.

- **"Attack/Destroy: toggle" bypass on cobblestone generators** — Holding left-click on a cobble generator prevented AFK detection indefinitely. Fixed.

- **"No longer AFK" broadcast missing or delayed** — When a player returned from AFK by sending a chat message, the server broadcast frequently never appeared. It now fires immediately.

- **AFK warning messages showed wrong countdown value** — The "you will be kicked in {seconds}s" warning displayed the configured threshold instead of the actual time remaining. Fixed.

- **AFK warnings not reset after world change** — Players who received warnings and then moved to a disabled world would not receive them again on returning. Fixed.

- **GUI shift+click did not decrease numeric values** — In the settings GUI, shift-clicking was supposed to decrease a value but always increased it instead. Fixed.

- **Large AFK pool detection not working** — Players in pools larger than 5×5 blocks were never detected even with the feature enabled. Fixed.

- **`GAMEMODE` and `COMMAND` actions ignored in global AFK action** — Setting `afk-action.type: GAMEMODE` or `COMMAND` silently fell through to a kick. Both actions now work correctly. `GAMEMODE` reads `afk-action.gamemode`; `COMMAND` reads `afk-action.command` with `{player}` / `{uuid}` support.

---

## Config / Connectivity Fixes

- **`database.enabled` ignored by SQL storage** — SQL storage now activates correctly when either `database.enabled` or `credit-system.database.enabled` is `true`.

- **DiscordSRV pattern alerts never fired** — With `integrations.discordsrv.send-pattern-alerts: true`, a Discord message is now sent when a suspicious pattern is detected, including the player name, pattern type, and confidence level.

- **`performance.adaptive-intervals` had no effect on the AFK check task** — The option now correctly adjusts the AFK check interval based on server TPS.

---

## New Features

### Reward system (`reward-system`) — now functional
- Tracks AFK time and fires configured `reward-system.intervals.*` thresholds once per AFK session.
- Respects `require-active-time-minutes`, `max-daily-rewards`, and `require-vault`.
- Executes console commands and sends player messages. Supports `{player}` and `{uuid}` placeholders.

### Analytics system (`analytics`) — now functional
- Enable with `analytics.enabled: true` in `config.yml`.
- Automatic daily reports generated at midnight in JSON or CSV format (set via `analytics.export-format`). Files are saved to `plugins/AntiAFKPlus/analytics/`.
- New commands:
  - `/afkplus analytics` — shows a live in-memory summary.
  - `/afkplus analytics export` — forces an immediate export.
  - Permission required: `antiafkplus.stats`.

### Hologram support — DecentHolograms and FancyHolograms
- A floating hologram appears above AFK players when `visual-effects.holograms.enabled: true`.
- Auto-detects DecentHolograms first, then FancyHolograms. If neither is installed, holograms are simply disabled.
- Text lines, height offset, and update interval are all configurable under `visual-effects.holograms.*` in `config.yml`.
- Supports `{player}` and `{time}` placeholders (e.g. `&eAFK for {time}`).

### Bedrock / GUI adaptation (Geyser/Floodgate)
- Player-facing messages and GUI menus now adapt automatically when a player is connected from Bedrock Edition via Geyser or Floodgate.

---

## Compatibility

- Paper 1.16 — 26.1.2+
- Folia (all versions)
- Spigot 1.16+
- Purpur, Pufferfish, and other Paper forks
- Bedrock via Geyser/Floodgate
- PlaceholderAPI
- DecentHolograms *(optional)*
- FancyHolograms *(optional)*

---

## Migration

**Drop-in update — no changes required**, except for the two notes below.

| What changed | Action required |
|---|---|
| `enabled-worlds` default is now `[]` (all worlds) | If you relied on the old default, add your world names explicitly or leave the list empty to keep "all worlds" behaviour |
| `repetitive-movement-threshold` default lowered to `0.82` | Only relevant if you set this manually; otherwise no action needed |
| 10 new language keys under `analytics.*` | Custom translation files will fall back to English until updated |

---

## How to Update

1. Stop the server.
2. Replace the plugin JAR with `AntiAFKPlus-3.1.jar`.
3. Start the server.
