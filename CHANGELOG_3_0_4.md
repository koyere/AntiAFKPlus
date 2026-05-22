# AntiAFKPlus v3.0.4 — Changelog

---

## ⚡ Performance

### Reduced load on the server main thread

**Problem:** On servers with many players and active timers, the plugin could contribute to small TPS dips (for example, TPS hovering around 19 instead of a steady 20). Profiling tools (like spark) showed AntiAFKPlus as one of the points where the server was spending time waiting, even though CPU usage looked low.

**Cause:** An internal check the plugin ran on every scheduled task to know whether the server was paused. That check, repeated many times per second, produced small stalls that, added up, affected TPS.

**Fix:** The check was rewritten so that:
- It only really runs once per tick, no matter how many tasks are active.
- It is skipped entirely on platforms where it does not apply (Spigot and forks without Paper's pause API). On those, the cost is now literally zero.

**Expected result:** More stable TPS and a much smaller footprint in profiling reports, especially on servers with high concurrency, the credit system enabled, particles, or multiple worlds.

No visible plugin behavior was modified: AFK detection, punishments, messages, and commands work exactly the same.

---

## ✅ Compatibility

- Paper 1.16 — 1.21+
- Folia (all versions)
- Spigot 1.16+
- Purpur, Pufferfish, and other Paper forks
- Bedrock via Geyser/Floodgate (unchanged)
- PlaceholderAPI (unchanged)

---

## 📦 Migration

**No action required.**

- No changes to `config.yml`.
- No changes to language files (`languages/*.yml`).
- No changes to permissions.
- No changes to the public API.

Players currently in AFK state when the server restarts will keep their usual behavior.

---

## 🔧 How to Update

1. Stop the server.
2. Replace the plugin JAR with `AntiAFKPlus-3.0.4.jar`.
3. Start the server.

That's it.
