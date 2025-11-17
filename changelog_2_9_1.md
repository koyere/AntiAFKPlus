# AntiAFKPlus v2.9.1 — Pattern Detection Respecting Config

Release type: Bugfix
Compatibility: Minecraft 1.16 – 1.21.10 | Java 17+

---

## What's Fixed
- Pattern Detection now fully honors config/module toggles; disabling the module or `enhanced-detection.pattern-detection` stops analysis and prevents flags/actions.
- Detector uses all configured thresholds (interval, sample counts, repetitive/area limits, large-pool and keystroke toggles) instead of hardcoded defaults.
- `/afkplus reload` cleanly restarts or shuts down pattern analysis to reflect updated settings without a server restart.

---

## Upgrade Notes
1. Replace the old JAR with `AntiAFKPlus v2.9.1`.
2. Reload with `/afkplus reload` or restart.
3. Verify your `modules.pattern-detection` and `pattern-detection-settings` values; they are now enforced.

---

**Version**: 2.9.1  
**Release Date**: 15/11/2025  
**Compatibility**: Minecraft 1.16 - 1.21.10  
**Java**: 17+
