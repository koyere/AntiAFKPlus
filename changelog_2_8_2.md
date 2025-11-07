# AntiAFKPlus v2.8.2 (Hotfix Rollup)

## Bug Fixes
- **Pause-aware scheduling** – Introduced a shared `ServerStateUtil` and wrapped every repeating task scheduled through `PlatformScheduler` so they no longer spin while Paper/Folia is paused or running in auto-pause mode. This eliminates the repeated CPU spikes reported when the server was suspended.
- **Stable transfer countdowns** – Countdown sequences now persist their start timestamp and AFK state. Players that have not produced real activity after the countdown started will no longer flip back to ACTIVE each tick, so the “title disable kick / activity detected loop” is gone and the final action fires exactly once.

These fixes are fully backwards compatible with the v2.8 API—only internal scheduling and AFK state transitions were touched.
