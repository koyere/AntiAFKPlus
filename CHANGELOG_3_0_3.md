# AntiAFKPlus v3.0.3 — Changelog

---

## 🐛 Bug Fixes

### Players bypass AFK detection by riding animals in water (Critical)

**Problem:** A player sitting on any rideable entity (horse, donkey, camel, boat, etc.) in water could completely avoid AFK punishments. The natural bobbing motion of the animal in water was incorrectly counted as player activity, causing the plugin to immediately unmark the player as AFK — even though the PatternDetector had correctly identified them as idle.

**Symptoms:**
- Console shows pattern violations accumulating normally (`repetitive_movement`, `keystroke_timeout`)
- Player gets marked as AFK at the correct threshold
- Player is immediately unmarked (~1 second later) with reason `"manual (API activity/unmark)"`
- This loop repeats indefinitely — the player is never actually punished

**Fix:** The plugin now correctly identifies passive vehicle movement and ignores it as activity. Players mounted on entities will only be considered active if they move their camera (mouse input), which requires real player interaction. Pattern detection continues monitoring mounted players normally.

**Affected setups:** All servers, but especially those using `MARK_AFK_ONLY` as the AFK action. Servers using `KICK` or `TELEPORT` were less impacted since those actions execute before the next movement tick.

---

### Blank lines in chat when AFK state messages are set to empty

**Problem:** Setting `player-now-afk: ""` or `player-no-longer-afk: ""` in the language file to disable AFK state broadcasts still sent blank lines to all online players.

**Fix:** The plugin now checks if the message is empty or whitespace-only before broadcasting. Setting either message to `""` will now completely suppress it — no blank lines, no chat spam.

---

### Language file changes not applied on `/afkplus reload`

**Problem:** Editing language YAML files (e.g., `plugins/AntiAFKPlus/languages/en.yml`) required a full server restart to take effect. The `/afkplus reload` command only reloaded `config.yml` but not the language files.

**Fix:** `/afkplus reload` now also reloads all language files. Changes to messages, translations, and custom text take effect immediately without restarting.

---

## 📋 Improvements

### Renamed `max-voluntary-afk-time-seconds` → `max-afk-duration-seconds`

The old name was confusing because it also applies to players marked AFK by the plugin (via `MARK_AFK_ONLY`), not just voluntary `/afk` usage.

**New config key:**
```yaml
# Maximum seconds a player can remain in AFK state before being forced active.
# Applies to both voluntary (/afk) and plugin-marked (MARK_AFK_ONLY) AFK states.
# Set to 0 to disable (players stay AFK indefinitely until they move).
max-afk-duration-seconds: 600
```

The old key `max-voluntary-afk-time-seconds` still works for backward compatibility — no migration needed.

---

## ✅ Compatibility

- Paper 1.16 — 26.1.2+
- Folia (all versions)
- Spigot 1.16+
- Purpur, Pufferfish, and other Paper forks
- No config changes required

---

## 🔧 How to Update

1. Stop the server
2. Replace the plugin JAR with `AntiAFKPlus-3.0.3.jar`
3. Start the server

No config changes required. The old `max-voluntary-afk-time-seconds` key still works.

**Optional:** Rename `max-voluntary-afk-time-seconds` to `max-afk-duration-seconds` in your `config.yml` for clarity. Both keys are supported.
