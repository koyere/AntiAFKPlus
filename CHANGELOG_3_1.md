# AntiAFKPlus v3.1 — Changelog

---

## 🐛 Bug Fixes

### AFK pool bypass via water current — Critical
Players inside a 1×1 AFK water pool were being un-marked immediately after detection, looping forever without punishment.
- Passive water-current movement is now ignored; only real input (swimming, looking around, sprinting, jumping) counts as activity.
- Servers using `MARK_AFK_ONLY` were most affected. No config change needed.

### Door spam-click bypass — Critical
Repeatedly right-clicking a door (manually or via auto-clicker) reset the AFK timer indefinitely. Noteblock, gate, and lever were all correctly detected; the door was the sole exception.

Root cause — two concurrent bugs:

**Sub-bug A — face alternation.** A door's clickable face changes when it opens (e.g. `SOUTH` closed → `EAST` open, because the panel rotates 90°). The passive-repeat filter compared `BlockFace` exactly, so closed-face and open-face were treated as different blocks, and each click was counted as fresh activity.

- Fix: door and trapdoor blocks now store `BlockFace.SELF` as a sentinel in the interact context. `matchesTarget` skips the face comparison when either side is `SELF`, so both faces map to the same canonical context.
- Additionally, two-tall doors may fire on the top half (Y+1) or bottom half (Y) depending on eye angle. The stored Y is now normalized to the bottom half so both halves match the same context.

**Sub-bug B — air clicks through the open door.** After the door opens, the cursor passes through the empty doorway and fires `RIGHT_CLICK_AIR`. Air clicks were never checked by the passive filter, so every one reset the AFK timer.

- Fix: when the player's position and orientation match the stored door-toggle context (no movement, no camera rotation), `RIGHT_CLICK_AIR` events are suppressed. The same stillness check also suppresses `RIGHT_CLICK_BLOCK` on the block behind the open door.
- The door-toggle context is cleared automatically on any significant player movement, so the suppression window closes the moment the player walks.

No config change needed. Normal door use (moving, looking around before clicking, switching items) is unaffected.

### "Use Item: toggle" bypass — Critical
Right-clicking a lever or button with Minecraft's accessibility toggle kept sending clicks indefinitely, preventing AFK detection.
- Repeated identical clicks on the same block face with the same item and no movement are no longer counted as activity.
- Normal play (building, eating, fishing, opening chests) is unaffected. No config change needed.

### "Attack/Destroy: toggle" bypass via cobble generator — Critical
Mining a cobble generator with Minecraft's "Attack/Destroy" accessibility toggle prevented AFK detection indefinitely.
- Each regenerated cobblestone block caused the client to restart the dig sequence, firing a fresh left-click event that reset the AFK timer in a tight loop.
- Left-click repetitions on the same block coordinates with the same tool and no player movement or camera rotation are now filtered as passive, using the same detection logic already in place for the right-click toggle.
- Legitimate mining (advancing position, looking around, switching blocks) is unaffected. No config change needed.
- **Design note:** A player mining a cobble generator while standing completely still and without moving the camera will be treated the same as toggle mode, since the plugin cannot distinguish the two at the event level. Any micro-movement or camera rotation resets the sequence. The AFK timeout is the grace period — increase it if your server has intensive manual grinders.

### "Attack/Destroy: toggle" cobble generator bypass — residual path (two sub-bugs) — Critical
After the initial cobble generator fix, a tester confirmed detection still failed: only 1 pattern violation fired and then nothing.

**Sub-bug A — `LEFT_CLICK_AIR` cleared the interact context between regenerations.**
When a block breaks, the client briefly sends `LEFT_CLICK_AIR`. The previous code called `lastInteractContext.remove(uuid)` on any non-block action, so the next `LEFT_CLICK_BLOCK` on the regenerated cobble found `previous == null` and was treated as the first click in a new sequence — counting as fresh activity and resetting the AFK timer on every cycle.
- Fix: non-block actions no longer clear the stored context. The previous block snapshot is preserved; when the regenerated cobble fires `LEFT_CLICK_BLOCK` at the same coordinates with the same tool and player position, `matchesTarget` + `matchesPlayerStillness` correctly classify it as passive. Any real activity (movement, head rotation, different block, tool swap) still breaks the sequence.

**Sub-bug B — `PatternDetector` skipped stationary players after the first analysis cycle.**
The check `if (locationData.lastUpdate <= patternData.lastAnalysis)` was designed to skip re-analysis when nothing changed. `locationData.lastUpdate` is only advanced by `PlayerMoveEvent`. A player standing completely still (cobble gen, bedrock) never fires `PlayerMoveEvent`, so after the first analysis cycle `lastUpdate` never advanced past `lastAnalysis` → the player was permanently skipped → only 1 violation total, never reaching `maxPatternViolations`.
- Fix: the `lastUpdate`-only gate is replaced by a lightweight activity gate that also considers `lastAnyInteractTime` (a new timestamp recorded at the start of every `PlayerInteractEvent`, before the passive filter). A cobble-gen miner's client keeps sending `LEFT_CLICK_BLOCK` → `lastAnyInteractTime` stays fresh → analysis runs every cycle and violations accumulate correctly. A player who is genuinely idle (no interact, no move, no command) still gets skipped as before.

### "No longer AFK" broadcast missing or delayed — High
When a player broke automatic AFK by sending a chat message, the "X is no longer AFK" broadcast frequently never appeared.
- Root cause: `onPlayerActivity` did not immediately unmark auto-detected AFK players. It recorded the activity and cleared the action lock, but the player remained in `afkPlayers` until the next AFK check cycle (up to `afk-check-interval-seconds` later, default 5 s). If the kick fired in that same cycle the broadcast was permanently lost; if the player disconnected before the next cycle the same happened.
- Fix: `onPlayerActivity` now immediately fires the state-change event, calls `unmarkAsAFKInternal`, and clears all per-session maps (`afkDetectionTimes`, `afkDetectionReasons`, `warningCounts`) for auto-AFK players the moment activity is detected — identical to what the check task did, but instant.

### AFK warning messages show wrong countdown value — Medium
The "You will be kicked in {seconds}s" warning displayed the *configured threshold* (e.g. 60, 30, 10) instead of the *actual seconds remaining* until the AFK threshold.
- With `default-afk-time: 30` and `afk-warnings: [60, 30, 10]`, both the 60 s and 30 s warnings fired simultaneously and both said the wrong number (e.g. "kicked in 60 s" when only 25 s remained).
- Fix: `{seconds}` is now replaced with `secondsRemaining` (the real calculated value) rather than `warningTimeSeconds` (the list entry) in both the chat message and the title/subtitle overlay. No config change needed.
- **Config note:** `afk-warnings` entries should be smaller than `default-afk-time` (e.g. with `default-afk-time: 300` use `[120, 60, 30, 10]`). Entries larger than the threshold all fire simultaneously at the moment the timer starts because `secondsRemaining` can never exceed the threshold.

### AFK warnings not reset after world change — Low
Players who received AFK warnings before changing to a disabled or non-enabled world still had those warnings recorded internally. On returning to a monitored world, `warningsSent` was already populated so the same warnings never fired again during that session.
- Root cause: `warningsSent.remove(uuid)` was inside the `if (afkPlayers.contains(uuid) || manualAfkUsernames.contains(uuid))` guard, so it only cleared for players already in AFK state — not for players who had received warnings but were not yet marked AFK.
- Fix: `warningsSent.remove(uuid)` is now called unconditionally before `continue` in both the `disabled-worlds` and `enabled-worlds` boundary checks. No config change needed.

### GUI shift+click does not decrease numeric values — Medium
In the settings GUI, "Sneak+Click: decrease" lore instructions appeared on adjustable items (AFK time, check interval, Required Active Time, etc.) but shift-clicking never decreased the value — it always increased.
- Root cause: `player.isSneaking()` always returns `false` inside an open inventory; Minecraft does not propagate the sneak key state to the server while a container is open.
- Fix: click handlers now receive `event.isShiftClick()` from the `InventoryClickEvent`, which correctly reflects whether Shift was held during the click. No config change needed.

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

- Paper 1.16 — 26.1.2+
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
