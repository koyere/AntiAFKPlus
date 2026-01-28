# AntiAFKPlus v2.9.3 — AFK Player Protection System & PlaceholderAPI Fix

Release type: Feature & Bugfix
Compatibility: Minecraft 1.16 – 1.21.11 | Java 17+

---

## What's New

### 🛡️ Complete AFK Player Protection System
- **Full Protection**: AFK players are now completely protected from external interference
- **PvP Protection**: Other players cannot attack or damage AFK players
- **Movement Protection**: Prevents forced movement from knockback, explosions, and player pushing
- **Damage Immunity**: Configurable protection from fall damage, fire, drowning, lava, and other environmental hazards
- **Interaction Control**: Optional blocking of inventory access, block interactions, and command execution while AFK
- **Smart Detection**: Distinguishes between player-initiated movement and forced movement (knockback, pushing)

### 🔕 Pattern Detection Notification Control System
- **Granular Control**: Configure when and to whom pattern detection messages are sent
- **Silent by Default**: Players no longer receive spam messages about pattern detections
- **Admin Alerts**: Optional notifications to staff members with specific permission
- **Three Notification Types**: Control messages on detection, violation, and action execution independently
- **Customizable Permission**: Configure which permission is required for admin notifications
- **Solves Spam Issue**: Addresses user reports of repetitive "[AntiAFK] Suspicious movement pattern detected" messages

### 🎯 Improved Pattern Detection Defaults & Configuration
- **Reduced False Positives**: Adjusted detection thresholds based on user feedback
  - Confined space: 5.0 → 8.0 blocks (allows small builds without triggering)
  - Repetitive movement: 0.8 → 0.9 similarity (90% required, fewer false positives)
  - Pattern violations: 3 → 5 (more tolerant before taking action)
  - Water circle radius: 3.0 → 4.0 blocks (allows small legitimate pools)
  - Min samples: 20 → 30 (~2.5 minutes of data for reliable detection)
- **Comprehensive Documentation**: Every setting now includes:
  - Recommended value ranges
  - Detailed explanations of how it works
  - Impact on false positives vs detection accuracy
- **Eliminated Configuration Confusion**: Removed duplicate `pattern-detection-settings` section
- **Quick Start Guide**: Added helpful guide at top of config.yml with common adjustments
- **Zone Management Safety**: Disabled by default to prevent unsafe teleports (was enabled)

### 🔧 PlaceholderAPI Custom Messages Fix
- **Fixed Custom Placeholders**: `placeholder-status-afk`, `placeholder-status-active`, and related placeholders now work correctly
- **Supports Color Codes**: Custom messages with `&` color codes now display properly in tab lists and other PlaceholderAPI integrations
- **Enhanced Status Detection**: Distinguishes between Manual AFK (`/afk` command) and Auto AFK (automatic detection)

---

## Configuration

### Player Protection Settings
Add to your `config.yml`:

```yaml
modules:
  player-protection:
    enabled: true  # Enable the protection system

player-protection:
  # Movement Protection
  prevent-movement-while-afk: false  # Set to true to prevent forced movement
  movement-restriction-message: true
  
  # Damage Protection
  invulnerability-enabled: true
  invulnerability-delay-ms: 5000
  damage-types-blocked:
    - "FALL"
    - "DROWNING" 
    - "FIRE"
    - "LAVA"
  prevent-pvp-invulnerability: true  # Prevents PvP against AFK players
  
  # Interaction Control
  block-inventory-access: true
  block-command-execution: false
  command-whitelist:
    - "/afk"
    - "/help" 
    - "/spawn"
  prevent-block-interaction: true
```

### Pattern Detection Notification Settings
Add to your `config.yml`:

```yaml
modules:
  pattern-detection:
    enabled: true
    # ... other pattern detection settings ...
    
    # v2.9.3 NEW: Notification control system
    notifications:
      notify-player-on-detection: false  # Silent by default
      notify-player-on-violation: false  # No spam on violations
      notify-player-on-action: false     # No message when action executed
      send-to-admins: true               # Admins receive alerts
      admin-notification-permission: "antiafkplus.notify.patterns"
```

**New Permission**: `antiafkplus.notify.patterns`  
Grant this permission to staff members who should receive pattern detection alerts.

### Custom PlaceholderAPI Messages
Add to your `messages.yml`:

```yaml
messages:
  # PlaceholderAPI status messages (supports color codes)
  placeholder-status-afk: "AFK"
  placeholder-status-active: "ACTIVE"
  placeholder-status-manual-afk: "MANUAL AFK"
  placeholder-status-auto-afk: "AUTO AFK"
  
  # Protection system messages
  protection-invulnerable: "&a[AntiAFK] You are now invulnerable while AFK."
  protection-vulnerable: "&c[AntiAFK] You are no longer invulnerable."
  protection-movement-blocked: "&e[AntiAFK] Movement blocked while AFK."
  protection-pvp-blocked: "&c[AntiAFK] You cannot attack AFK players."
  protection-pvp-protected: "&a[AntiAFK] You are protected from PvP while AFK."
  protection-inventory-blocked: "&e[AntiAFK] Inventory access blocked while AFK."
  protection-interaction-blocked: "&e[AntiAFK] Block interaction blocked while AFK."
  protection-command-blocked: "&e[AntiAFK] Command execution blocked while AFK. Use /afk to return."
```

---

## Use Cases

### 🎯 Recommended Settings for Different Server Types

**PvP Servers:**
```yaml
player-protection:
  prevent-movement-while-afk: true   # Prevent combat advantage abuse
  prevent-pvp-invulnerability: true  # Full PvP protection
  invulnerability-enabled: true      # Environmental protection
```

**Survival/SMP Servers:**
```yaml
player-protection:
  prevent-movement-while-afk: false  # Allow natural movement
  prevent-pvp-invulnerability: true  # Prevent griefing
  invulnerability-enabled: true      # Protect from mobs/environment
```

**Creative/Build Servers:**
```yaml
player-protection:
  prevent-movement-while-afk: false
  prevent-pvp-invulnerability: false # PvP usually disabled anyway
  block-inventory-access: false      # Allow building while AFK
  prevent-block-interaction: false
```

---

## Compatibility

- **Minecraft**: 1.16 – 1.21.10
- **Platforms**: Bukkit, Spigot, Paper, Purpur, **Folia** (fully compatible)
- **Java**: 17+
- **PlaceholderAPI**: Enhanced support with custom messages
- **Fully Backward Compatible**: Existing configurations continue to work

---

## Installation & Upgrade

### New Installations
1. Download `AntiAFKPlus v2.9.3`
2. Place in `/plugins` folder
3. Restart server
4. Configure protection settings in `config.yml`
5. Customize messages in `messages.yml`

### Upgrading from Previous Versions

**From v2.9.2 or earlier:**
1. Replace JAR file with v2.9.3
2. **Manual Config Update Required**: Add the new `player-protection` section to your `config.yml` (see configuration above)
3. **Manual Messages Update Required**: Add the new protection messages to your `messages.yml` (see configuration above)
4. Restart server
5. Verify protection is working with `/afkplus status`

**⚠️ Important**: This version requires manual configuration updates. The plugin will work without them, but protection features will be disabled.

---

## Fixed Issues

### PlaceholderAPI Problems
- ✅ **Fixed**: Custom `placeholder-status-afk: "&7[AFK]"` messages now work correctly
- ✅ **Fixed**: Color codes in placeholder messages are properly applied
- ✅ **Fixed**: Tab list and other PlaceholderAPI integrations show custom messages instead of hardcoded "AFK/ACTIVE"

### AFK Player Vulnerability
- ✅ **Fixed**: AFK players can no longer be easily removed from AFK state by other players
- ✅ **Fixed**: Prevents griefing through forced movement (pushing, knockback)
- ✅ **Fixed**: Configurable protection from environmental damage
- ✅ **Fixed**: PvP protection prevents combat abuse

### Pattern Detection Spam Messages
- ✅ **Fixed**: Players no longer receive repetitive "[AntiAFK] Suspicious movement pattern detected" messages
- ✅ **Fixed**: Notification spam when pattern violations accumulate
- ✅ **Fixed**: Added granular control over when notifications are sent
- ✅ **New**: Admin-only alerts with customizable permission system

### Pattern Detection False Positives
- ✅ **Fixed**: Players flagged incorrectly for "repetitive movement" during normal gameplay
- ✅ **Fixed**: "Confined space" detection triggering in outdoor areas and small builds
- ✅ **Fixed**: Overly aggressive detection thresholds causing legitimate players to be marked AFK
- ✅ **Improved**: All thresholds adjusted to more conservative values based on user feedback
- ✅ **Improved**: Detection now requires more samples (30 vs 20) for reliable pattern identification

### Configuration Issues
- ✅ **Fixed**: Duplicate `pattern-detection-settings` section causing confusion
- ✅ **Fixed**: Unclear which configuration section to edit (now clearly documented)
- ✅ **Fixed**: Missing value ranges and explanations for pattern detection settings
- ✅ **Fixed**: Zone management enabled by default with unsafe teleport coordinates
- ✅ **New**: Quick start guide at top of config.yml with common troubleshooting
- ✅ **New**: Comprehensive documentation for every pattern detection setting

---

## For Server Administrators

### Testing the Protection System
1. Set yourself AFK with `/afk`
2. Have another player try to hit you (should be blocked)
3. Try taking fall damage (should be blocked if configured)
4. Test forced movement prevention (if enabled)
5. Check that protection messages appear correctly

### Performance Impact
- **Minimal**: Uses efficient event handling with cooldown systems
- **Folia Compatible**: Thread-safe implementation
- **Memory Efficient**: Automatic cleanup on player disconnect

### Troubleshooting
- **Protection not working**: Verify `modules.player-protection.enabled: true` in config.yml
- **PlaceholderAPI issues**: Ensure messages.yml contains the new placeholder entries
- **Folia compatibility**: No special configuration needed, works out of the box

---

**Version**: 2.9.3  
**Release Date**: December 30, 2025  
**Compatibility**: Minecraft 1.16 - 1.21.11  
**Java**: 17+
