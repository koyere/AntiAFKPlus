# AntiAFKPlus v3.0.5 — Changelog

---

## 🐛 Bug Fixes

### Players bypass AFK detection by standing in a small water pool (Critical)

**Problem:** A player standing inside a classic 1×1 AFK water pool (with stair or step blocks under the water) could completely avoid AFK punishments. Pattern detection correctly flagged the player as AFK after 8 violations (`confined_space`, `keystroke_timeout`), but a few seconds later the plugin immediately unmarked them — without any mouse, keyboard, or input of any kind from the player.

**Symptoms:**
- Console shows pattern violations accumulating normally (`confined_space`, `keystroke_timeout`)
- Player gets marked as AFK at the correct threshold (`auto (keystroke_timeout)`)
- Player is unmarked seconds later with reason `manual (API activity/unmark)`
- This loop repeats indefinitely — the player is never actually punished

**Cause:** When the water current pushed the player against the step blocks, the resulting tiny vertical movement was being read as legitimate player activity. That activity reset the AFK state set by the pattern detector.

**Fix:** The plugin now correctly identifies passive water-current movement and ignores it as activity. A water-pool push is only ignored when **all** of these are true at the same time:

- The player is inside water or lava
- The player is not rotating their camera (no mouse input)
- The player is not sprinting, sneaking, flying, or gliding (no held movement key)
- The horizontal speed is below the threshold typical of a real water current

If the player swims, walks, jumps, looks around, or sprints, the movement is counted as activity exactly as before. Players actually swimming manually are not affected.

Pattern detection continues monitoring trapped players normally, so AFK pools that try to defeat the plugin in other ways (e.g. extremely large pools) are still caught.

**Affected setups:** All servers, especially those using `MARK_AFK_ONLY` as the AFK action. Servers using `KICK` or `TELEPORT` were less impacted because those actions execute before the next movement tick.

---

### Players bypass AFK detection by abusing the "Use Item: toggle" option (Critical)

**Problem:** A player could fully bypass AFK detection by using Minecraft's accessibility option "Use Item / Place Block" set to **toggle mode**, then right-clicking a lever (or any other interactable block). The toggle keeps the right-click active, so the client sends an interaction every tick to the same block — no mouse, no keyboard, no real input from the player. The plugin counted each one as activity, the AFK timer never accumulated, and the player was never marked AFK.

**Symptoms:**
- Player stays in front of a lever, sign, button, door, repeater, etc. for hours
- Console shows no pattern violations
- Player is never marked AFK
- No movement, no rotation, no other input — yet the plugin treats them as active

**Cause:** Every right-click sent by the toggle was being treated as a legitimate interaction, exactly like a real click.

**Fix:** The plugin now distinguishes between a real click and a mechanical repetition of the previous one. A right-click on a block is ignored as activity only when **all** of these are true at the same time, compared to the previous click:

- Same block (same world and same coordinates)
- Same clicked face of the block
- Same item held in the main hand
- The player has not moved at all
- The player has not rotated the camera at all

If anything changes — the player looks around, walks, sneaks, sprints, switches item, clicks a different block, or clicks a different face — the next click counts as activity exactly as before. The first click of any sequence always counts. This means normal play (clicking levers, opening chests, talking to villagers, building, eating, drinking, blocking with a shield, fishing) is not affected at all.

The same filter is applied both when the player is active and when they are already AFK, so the toggle bypass cannot be used to either avoid going AFK or to escape an AFK state.

**Affected setups:** All servers. The bypass worked regardless of the configured AFK action (`KICK`, `TELEPORT`, `MARK_AFK_ONLY`).

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
- No changes to the message format players see.

Players currently in AFK state when the server restarts will keep their usual behavior.

---

## 🔧 How to Update

1. Stop the server.
2. Replace the plugin JAR with `AntiAFKPlus-3.0.5.jar`.
3. Start the server.

That's it.
