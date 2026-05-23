# AntiAFKPlus v3.1 — Changelog

---

## 🐛 Bug Fixes

### AFK pool bypass via water current — Critical
Players inside a 1×1 AFK water pool were being un-marked immediately after detection, looping forever without punishment.
- Passive water-current movement is now ignored; only real input (swimming, looking around, sprinting, jumping) counts as activity.
- Servers using `MARK_AFK_ONLY` were most affected. No config change needed.

### "Use Item: toggle" bypass — Critical
Right-clicking a lever or button with Minecraft's accessibility toggle kept sending clicks indefinitely, preventing AFK detection.
- Repeated identical clicks on the same block face with the same item and no movement are no longer counted as activity.
- Normal play (building, eating, fishing, opening chests) is unaffected. No config change needed.

### Large AFK pool detection non-functional — Critical
Players in pools larger than 5×5 blocks were never detected even with the feature enabled.
- Fixed multiple independent bugs in the detection chain: passive velocity check, history snapshot isolation, mandatory pattern gate, and history buffer size (was 50 entries ≈ 2.5 s, now 300 ≈ 15 s).
- No config change needed; previously configured thresholds now take effect correctly.

### Linear-movement filter blocked water-pool detection — High
The false-positive filter for "player running in a straight line" also matched water currents, silently skipping pool analysis.
- Linear movement inside water is now treated as a current indicator, not active play.

### Pattern detection silently disabled on custom world names — High
`enabled-worlds` defaulted to `["world", "world_nether", "world_the_end"]`, disabling detection on any server with custom world names.
- **Default changed to `[]` (all worlds).** Servers that want to restrict detection must now list worlds explicitly.

### `repetitive-movement-threshold` too strict for rectangular pools — Medium
Default value `0.95` rejected pool corners, preventing detection of rectangular paths.
- **Default lowered to `0.82`.** Existing manual overrides are not affected.

### `getLastMovementTimestamp` returned wrong default — Low
After `clearPlayerData`, the AFK timer always computed "time since activity ≈ 0", delaying detection until the player moved.
- Default changed from `System.currentTimeMillis()` to `0L`. No config change needed.

### `GAMEMODE` and `COMMAND` actions ignored in global AFK action — High
Setting `afk-action.type: GAMEMODE` or `COMMAND` silently fell through to `KICK`.
- Both actions are now handled. `GAMEMODE` reads `afk-action.gamemode`; `COMMAND` reads `afk-action.command` with `{player}` / `{uuid}` support.

---

## 🔌 Config / Connectivity Fixes

### `database.enabled` ignored by SQL storage — Medium
- SQL storage now activates when either `database.enabled` **or** `credit-system.database.enabled` is true.

### `integrations.discordsrv.send-pattern-alerts` never fired — Medium
- DiscordSRV integration now listens to `PlayerAFKPatternDetectedEvent`. When `send-pattern-alerts: true`, it sends a message with the player name, pattern type, and confidence.

### `performance.adaptive-intervals` disconnected from AFK check task — Medium
- The AFK check task now reads and applies adaptive intervals from `PerformanceOptimizer` at startup and dynamically when TPS-based adjustments exceed 1 second of difference.

---

## 🎁 New Features

### Reward system (`reward-system`) — now fully functional
- Tracks AFK time and fires configured `reward-system.intervals.*` thresholds once per AFK session.
- Respects `require-active-time-minutes`, `max-daily-rewards`, and `require-vault`.
- Executes console commands and sends player messages; supports `{player}` and `{uuid}` placeholders.

### Analytics system (`analytics`) — now fully functional
Collect AFK session and pattern data, then export to disk.
- Enable with `analytics.enabled: true` in `config.yml`.
- Daily reports generated automatically at midnight (JSON or CSV, set via `analytics.export-format`). Files saved to `plugins/AntiAFKPlus/analytics/`.
- **New command:** `/afkplus analytics` — shows live in-memory summary. `/afkplus analytics export` — forces an immediate export. Permission: `antiafkplus.stats`.

### Hologram support — DecentHolograms and FancyHolograms
A floating hologram now appears above AFK players when `visual-effects.holograms.enabled: true`.
- Auto-detects DecentHolograms first, then FancyHolograms. No hard dependency on either; holograms are simply disabled if neither plugin is present.
- Lines, height offset, and update interval are all configurable under `visual-effects.holograms.*` in `config.yml`.
- Supports `{player}` and `{time}` placeholders (e.g. `&eAFK for {time}`).

### Bedrock / GUI adaptation — connected
- All player-facing messages in `AFKManager` now pass through `BedrockCompatibility.getAdaptedMessage()` when Geyser/Floodgate is active.
- All inventory creations in `GUIManager` now use `BedrockCompatibility.getAdaptedMenuSize()` with a minimum of 54 slots enforced.

---

## ✅ Compatibility

- Paper 1.16 — 1.21+
- Folia (all versions)
- Spigot 1.16+
- Purpur, Pufferfish, and other Paper forks
- Bedrock via Geyser/Floodgate
- PlaceholderAPI
- DecentHolograms *(optional)*
- FancyHolograms *(optional)*

---

## 📦 Migration

**Mostly drop-in.** Read the two notes below.

| What changed | Action required |
|---|---|
| `enabled-worlds` default is now `[]` (all worlds) | If you previously relied on the default, add your world names explicitly or leave the list empty to keep "all worlds" behaviour |
| `repetitive-movement-threshold` default lowered to `0.82` | Only relevant if you manually set this value; otherwise no action needed |
| Language files: 10 new keys under `analytics.*` | Custom translations will fall back to English until updated |
| No other config changes | — |

---

## 🔧 How to Update

1. Stop the server.
2. Replace the plugin JAR with `AntiAFKPlus-3.1.jar`.
3. Start the server.
