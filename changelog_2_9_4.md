# AntiAFKPlus v2.9.4

**Release Date:** January 2026

## Summary

This version drastically reduces **false positives** in pattern detection. Players will no longer be kicked for running in a straight line (forests, tunnels, paths) or moving normally around their bases.

---

## New Features

### Linear Movement Exclusion
Players running in a straight line will no longer be flagged as AFK.

```yaml
modules:
  pattern-detection:
    linear-movement-exclusion: true
    linear-movement-threshold: 0.3
    min-direction-variance: 0.15
```

### Activity Grace Period
Players who have been recently active (commands, jumping, looking around) get a grace period where pattern detection is temporarily suspended.

```yaml
modules:
  pattern-detection:
    activity-grace-period-ms: 60000
```

---

## Improved Defaults

| Setting | Previous | New |
|---------|----------|-----|
| `water-circle-radius` | 4.0 | 5.0 |
| `min-samples-for-pattern` | 30 | 40 |
| `confined-space-threshold` | 8.0 | 12.0 |
| `repetitive-movement-threshold` | 0.9 | 0.95 |
| `max-pattern-violations` | 5 | 8 |

---

## Upgrade Guide

### Option 1: Regenerate config (Recommended)
Delete your `config.yml` and restart the server. A new one with all v2.9.4 defaults will be generated.

### Option 2: Manual Update
Add this to your `modules.pattern-detection` section:

```yaml
# New in v2.9.4
linear-movement-exclusion: true
linear-movement-threshold: 0.3
min-direction-variance: 0.15
activity-grace-period-ms: 60000

# Updated values
water-circle-radius: 5.0
min-samples-for-pattern: 40
confined-space-threshold: 12.0
repetitive-movement-threshold: 0.95
max-pattern-violations: 8
```

---

## Bug Fixes

- False positives when running through forests, tunnels, and paths
- Incorrect kicks when exploring own bases
- Violation accumulation during normal gameplay

---

## Compatibility

- **Minecraft:** 1.16 - 1.21.11
- **Servers:** Bukkit, Spigot, Paper, Purpur, Folia
- **Java:** 17+
- **Breaking Changes:** None

---

## API Changes

New methods in `ConfigManager`:
- `getActivityGracePeriodMs()`
- `getLinearMovementThreshold()`
- `getMinDirectionVariance()`
- `isLinearMovementExclusionEnabled()`
