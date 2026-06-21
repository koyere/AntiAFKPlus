# AntiAFKPlus v3.1.1 — Changelog

---

## 🐛 Bug Fixes

### Server idling below 20 TPS even with all modules disabled — Critical
With the plugin enabled, servers dropped to ~17-18 TPS (with low MSPT) and a spark profile showed the server thread parked. It happened regardless of which modules were turned on.
- Root cause: the performance optimizer compared the whole JVM heap usage against a 50 MB budget — always true on a real server — and forced a full `System.gc()` every 5 seconds. Each forced GC is a stop-the-world pause that breaks tick timing.
- Fix: the periodic forced GC has been removed. Memory cleanup now runs only under genuine heap pressure (used heap above 90% of the JVM max). CPU usage is also measured correctly now, so it no longer triggers spurious internal adjustments. No config change needed.

### EssentialsX (and similar AFK plugins) conflict — High
Players typing `/afk` were immediately unmarked as AFK a few seconds later with the message "X is no longer AFK", even without touching the keyboard or mouse. This still happened on the first 3.1.1 build; it is now fully resolved.
- Root cause: EssentialsX (and any plugin with an AFK teleport feature) processes `/afk` independently and teleports the player to its own AFK zone. The teleport — and the landing/gravity movement right after it — was counted as real player activity, immediately cancelling the manual AFK state.
- Fix: when a plugin teleports a player who went manually AFK within the last 15 seconds, neither the teleport nor the brief movement that follows it unmarks them. After the 15-second grace period, plugin teleports count as normal activity. No config change needed.
- **Recommended config action**: Disable EssentialsX's `/afk` in its config to avoid dual handling:
  ```yaml
  # EssentialsX config.yml
  disabled-commands:
    - afk
  ```
  Then configure teleportation in AntiAFKPlus instead (`afk-action.type: TELEPORT` or `zone-management`).

### Door spam-click bypass — Critical
Repeatedly right-clicking a door (manually or via auto-clicker / Minecraft accessibility toggle) reset the AFK timer indefinitely. Noteblock, gate, and lever were correctly detected; the door was the sole exception.

Two concurrent bugs:
- **Face alternation**: a door's clickable face changes when it opens (panel rotates 90°). The passive-repeat filter compared faces exactly, so each click was treated as a new action. Fix: doors and trapdoors now use a face-agnostic comparison. Two-tall door clicks are normalized to the bottom half.
- **Air clicks through the open door**: after the door opens, the auto-click lands on air. Fix: air clicks are suppressed when the player has not moved since the last door toggle. The same suppression applies to clicks on blocks within 2 blocks of the door (the "block behind the open door" scenario) but not beyond, so functional blocks farther away are never affected.

No config change needed.

### AFK warning title/subtitle shows wrong countdown — Medium
The title overlay sent during AFK warnings (`warning-subtitle-standard`) displayed the configured threshold (e.g. 60, 30, 10) instead of the actual seconds remaining, identical to the chat message bug fixed in v3.1. Only the subtitle was affected; the chat message was already corrected.
- Fix: `{seconds}` in the subtitle now uses `secondsRemaining` (actual time left). No config change needed.

### AFK warnings not reset after world change — Low
Players who received AFK warnings before switching to a disabled or non-enabled world still had those warnings recorded. On returning to a monitored world, the same warnings never fired again for that session.
- Fix: pending warnings are cleared whenever a player enters a disabled or non-enabled world, regardless of whether they were already marked AFK. No config change needed.

### Blank lines in chat when language messages are set to `""` — Medium
Setting Bedrock-related messages to `""` in the language file (`bedrock-detected`, `bedrock-tip-mobile`, `bedrock-tip-console`, `bedrock-tip-desktop`) caused a blank chat line for Bedrock players on join instead of silence.
- Fix: all Bedrock welcome and tip messages now check for null/empty before sending. Setting any of these keys to `""` correctly suppresses the message. No config change needed.

---

## 🔧 How to Update

1. Stop the server.
2. Replace the plugin JAR with `AntiAFKPlus-3.1.1.jar`.
3. Start the server — no config changes required unless noted above.
