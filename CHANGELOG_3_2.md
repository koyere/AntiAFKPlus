# AntiAFKPlus v3.2 — Changelog

## 🐛 Bug Fixes

- **No more instant kicks after a reload.** Players already online during a `/reload` (or PlugMan/live install) were marked AFK and kicked within seconds, ignoring `default-afk-time`. They are now treated as active and only go AFK after the configured time.

- **`permission-times` now applies the longest matching time.** A player with several matching ranks (or a wildcard/OP permission) could get a random, shorter timeout. The most generous matching value is now used. `default-afk-time` still applies when no rank matches.

- **No more AFK while actively riding.** Driving a boat, happy ghast, horse, etc. holding **W** (straight forward) wrongly triggered AFK; only **A/S/D** broke out. Active steering (W/A/S/D, jump, sneak) now counts as activity. Passive movement (e.g. a drifting boat) still does not, so AFK detection stays accurate. Steering detection requires Minecraft 1.21.3+.

> No configuration changes are required for any of these fixes.

## 🔧 How to Update

1. Stop the server.
2. Replace the JAR with `AntiAFKPlus-3.2.jar`.
3. Start the server.
