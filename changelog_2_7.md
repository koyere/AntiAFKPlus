# AntiAFKPlus v2.7 — Playtime Triggers, Return Actions, Notifier, GUI, DB Sync (Plan)

## Goal
Close feature gaps vs “JetsAntiAFKPro” while keeping AntiAFKPlus’ standards: Folia‑safe scheduling, zero‑lag defaults, full backward compatibility, and clean modular design.

## Scope (High Level)
- Active Playtime Tracker module (playtime, triggers, repeat actions)
- On‑Return Actions (pipeline when leaving AFK)
- AFK Notifier (inform sender if target is AFK)
- Playtime GUI (configurable inventory view)
- Persistent Playtime Storage (SQLite/MySQL) with Bungee sync
- Placeholder expansion for playtime and leaderboards
- API additions for playtime queries and hooks

## Phases (Work Plan)

1) Module: Playtime Tracker (Core)
- Add `modules.playtime-tracker.enabled` (default: false)
- Track “active playtime” (movement/commands/interaction thresholds) separate from AFK/credits
- Configurable accumulation window (e.g., per‑tick, per‑second aggregation)
- Respect performance optimizer (batch updates, write‑behind)

2) Triggers: Time‑Based Actions (Repeat)
- Config: `playtime-tracker.actions:`
  - `repeat-every: 10m` (support s/m/h format)
  - `perms:` override/ratios by permission (optional)
  - `pipeline:` steps list (TITLE/SUBTITLE/SOUND/MESSAGE/WAIT/COMMAND/TRANSFER)
- Ensure per‑player last‑execution tracking and deduplication
- Allow “send to other server after X playtime” (uses existing transfer service)

3) On‑Return Actions (Pipeline)
- Config: `actions.on-return.enabled` + `actions.on-return.pipeline` (list of steps)
- Fire when AFK → ACTIVE (manual or auto), debounce to avoid spam
- Folia‑safe via PlatformScheduler per entity

4) AFK Notifier (Sender Warning)
- Feature: Inform sender when messaging an AFK player
- Config: `afk-notifier.enabled`, `afk-notifier.message`, `cooldown-seconds`
- Hooks:
  - Core: detect standard `/tell`/`/msg`/`/w` commands (command preprocess)
  - Soft‑depends: EssentialsX/CMI format detection (best‑effort)
  - Fallback: no notifier for custom PM systems without explicit hooks
- Folia‑safe, no heavy listeners; optional regex match for PM commands

5) Playtime GUI (Configurable)
- Command: `/playtime [player]` (permission `antiafkplus.playtime.view`)
- Inventory GUI with configurable layout (title, rows, items, placeholders)
- Pages: total/daily/weekly, session, last activity; support skull, lore templates
- Config section: `playtime-gui.*` (enabled, layout, items)

6) Persistent Storage (SQLite/MySQL)
- Config: `database.playtime.*` (type: SQLite|MySQL, table-prefix, pool, SSL)
- Schema:
  - `afkplus_playtime(player_uuid PK, total_seconds, daily_seconds, weekly_seconds, updated_at)`
  - `afkplus_playtime_history(id PK, player_uuid, delta_seconds, reason, ts)` (optional)
- Write‑behind cache with periodic flush; async I/O; graceful shutdown flush
- Migration tasks and safety (auto‑create tables)

7) Placeholders & Leaderboards
- New placeholders:
  - `%antiafkplus_playtime_total%`, `%..._daily%`, `%..._weekly%`, `%..._session%`
  - `%antiafkplus_top_playtime_{n}%` (name/time) — optional, cache results
- Optional `/playtime top [daily|weekly|total]` (permission `antiafkplus.playtime.top`)

8) API Additions
- `getPlaytimeTotal(UUID)`, `getPlaytimeDaily(UUID)`, `getPlaytimeWeekly(UUID)`
- `getPlaytimeSession(UUID)`, `getLastActivity(UUID)`
- `registerPlaytimeListener(...)` for triggers
- Async variants returning `CompletableFuture`

9) Docs & i18n
- README: new section “Playtime Tracker & GUI” with examples
- messages.yml: texts for GUI, notifier, return actions, and triggers
- i18n via LocalizationManager

10) QA & Performance
- Folia/Paper/Spigot validated
- Load test: 100 players, triggers every 10m, DB write‑behind under 5ms/tick
- Config validation in ConfigManager (safe defaults, guards)

## Configuration Draft (Proposed)
```yaml
modules:
  playtime-tracker:
    enabled: false

playtime-tracker:
  accumulation-interval-ticks: 20     # aggregate active time every 20 ticks
  min-activity-threshold: 0.2         # reuse activity score heuristics
  respect-afk: true                   # do not count while AFK

  actions:
    enabled: false
    repeat:
      every: "10m"                   # 10 minutes of active play
      per-permission:
        antiafkplus.playtime.vip: "8m"
      pipeline:
        - "TITLE: &aThanks for playing!"
        - "SUBTITLE: &eYou’ve earned a reward"
        - "COMMAND: eco give {player} 10"
        - "MESSAGE: &7You received &a$10 &7for playtime"

    transfer-after:
      enabled: false
      total-playtime: "2h"            # send after 2 hours total playtime
      target-server: "lobby-2"        # overrides global if set

actions:
  on-return:
    enabled: false
    debounce-seconds: 3
    pipeline:
      - "TITLE: &aWelcome back!"
      - "SOUND: UI_BUTTON_CLICK,1.0,1.0"

afk-notifier:
  enabled: false
  message: "&eNote: &f{target} &eis currently AFK"
  cooldown-seconds: 30
  commands:
    - "/msg"
    - "/tell"
    - "/w"

playtime-gui:
  enabled: false
  command-aliases: ["playtime", "ptime"]
  title: "&bPlayer Time"
  rows: 3
  items:
    total:
      slot: 10
      material: "CLOCK"
      name: "&eTotal: &f{total}"
      lore:
        - "&7Daily: &f{daily}"
        - "&7Weekly: &f{weekly}"
    session:
      slot: 13
      material: "PAPER"
      name: "&eSession: &f{session}"

# Database for playtime (separate from credits)
database:
  playtime:
    type: "SQLite"           # SQLite | MySQL
    table-prefix: "afkplus_"
    mysql:
      host: "localhost"
      port: 3306
      database: "minecraft"
      user: "user"
      password: "pass"
      use-ssl: false
    pool:
      max-pool-size: 5
      connection-timeout: 30000
```

## Permissions & Commands (Planned)
- `antiafkplus.playtime.view` — use `/playtime [player]` and open GUI
- `antiafkplus.playtime.top` — view leaderboards
- No changes to existing AFK permissions

## Backward Compatibility
- All new features disabled by default
- No behavior changes unless explicitly enabled
- Uses existing PlatformScheduler for Folia safety

## Testing Checklist (Manual)
- Validate playtime accumulation and repeat triggers
- Verify on‑return pipeline executes once per return (debounced)
- Confirm AFK notifier fires on PM commands and respects cooldown
- Exercise GUI navigation and placeholders
- Test MySQL sync across Bungee network; verify write‑behind and shutdown flush

---

Status: Planning complete. Implementation will target v2.7 in phases.
