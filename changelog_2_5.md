# AntiAFKPlus v2.5 — Release Notes

## 🎯 AFK Credit System - Earn Time to Be Away

### New Feature Overview
**Revolutionary AFK Credit System**: Players can now earn AFK time by being active! For every X minutes of active gameplay, players earn Y minutes of AFK allowance before being teleported to a designated AFK zone.

## What’s New

**1. AFK Credit Earning System**
- Players earn AFK credits by being actively engaged in gameplay
- Configurable credit ratios based on permissions (default: 5 minutes active = 1 minute AFK credit)
- Real-time credit accumulation tracking with activity validation
- Maximum credit limits to prevent indefinite accumulation

**2. Credit-Based AFK Protection**
- When a player goes AFK, the system first consumes available credits
- Credits provide immunity from immediate kick/teleportation
- Once credits are exhausted, player is teleported to designated AFK zone
- Smooth transition from credit consumption to zone teleportation

**AFK Credit System**
- Earn credits while active; consume credits to delay AFK action.
- Teleport to AFK zone when credits are exhausted; `/afkback` returns you.

**WorldGuard Integration**
- Zones resolved from regions (reflection, no hard dependency).
- Teleport priority: `zones.afk` → `zones.spawn` → `credit-system.afk-zone`.

**AFK Zone Protection**
- Optional damage/PVP/mob-spawn protections near the AFK zone.

**SQL History (Optional)**
- Track earn/consume/admin/decay transactions when SQL is enabled.

**New Placeholder**
- `%antiafkplus_credits_expire_days%` — days until credits expire.

---

## How It Works
1) Active players earn credits (ratio-based) up to a configurable cap.
2) When AFK is detected, 1 credit/minute is consumed to delay the action.
3) At 0 credits, the player is teleported to the AFK zone; `/afkback` returns them.
  - `onAfkStateChange` (MONITOR): stops consumption when returning to ACTIVE.
- `CreditData` (new):
  - Balance, consumption state, per‑player task, original location (for return).

### Configuration (added)
```yaml
modules:
  credit-system:
    enabled: false

credit-system:
  enabled: false
  credit-ratios:
    default: "5:1"
    vip: "4:1"
    premium: "3:1"
    admin: "2:1"
  max-credits:
    default: 120
    vip: 180
    premium: 240
    admin: 480
  earning-requirements:
    minimum-session-minutes: 5
    activity-threshold: 0.3
  afk-zone:
    enabled: true
    world: "world"
    location: "0,100,0"
  notifications:
    credit-earned: true
    credit-consumed: true
    credit-exhausted: true
  return-command:
    enabled: true
    cooldown-seconds: 10
```

### Messages (added)
```yaml
messages:
  credit-system:
    earned: "&a+ &f{minutes}m &7AFK credits earned! &8(&f{total}m &7total)"
    consuming-start: "&eUsing AFK credits to delay action..."
    consumed: "&c- &f{minutes}m &7AFK credits used &8(&f{remaining}m &7left)"
    exhausted: "&c❌ &7AFK credits exhausted. You will be moved."
    zone-teleport: "&7[AntiAFK+] &aTeleported to AFK zone."
    errors:
      invalid-location: "&7[AntiAFK+] &cInvalid AFK zone location configured."
```

### Permissions (declared)
- `antiafkplus.credit.earn` — earn credits (default true)
- `antiafkplus.credit.use` — use credits to delay AFK action (default true)
- `antiafkplus.credit.ratio.vip|premium|admin` — improved ratios (default false/op)
- `antiafkplus.credit.admin` — full credit administration

### Execution Flow
1) Active player earns credits according to ratio and limits.
2) On AFK, if credits available → cancel action and consume 1/min.
3) On activity → stop consumption (no teleport).
4) On exhaustion → teleport to AFK zone (zone‑management if present; fallback to `credit-system.afk-zone`).

### Compatibility & Performance
- Folia‑safe (no BukkitScheduler in Folia context).
- No changes to AFKManager loop (event‑driven).
- Earning: one global job/min; consumption: per‑player task while AFK with balance.
- Respects `antiafkplus.bypass`.

---

#### 🎮 Configuration System

**New Configuration Section** (`config.yml`):
```yaml
# ===============================================================================
# 💰 AFK CREDIT SYSTEM
# ===============================================================================

credit-system:
  enabled: true
  
  # Credit earning ratios (active_minutes:credit_minutes)
  # For every X minutes of active play, earn Y minutes of AFK credits
  credit-ratios:
    default: "5:1"           # Default: 5 minutes active = 1 minute AFK credit
    vip: "4:1"              # VIP players: better ratio
    premium: "3:1"          # Premium: even better ratio
    admin: "2:1"            # Admin: best ratio
  
  # Credit limits and management
  max-credits:
    default: 120            # Maximum 2 hours of credits
    vip: 180               # VIP: 3 hours max
    premium: 240           # Premium: 4 hours max
    admin: 480             # Admin: 8 hours max
  
  # Credit expiration system
  credit-decay:
    enabled: true
    expire-after-days: 7    # Credits expire after 7 days of inactivity
    warning-days: 2         # Warn players 2 days before expiration
    cleanup-interval-hours: 24  # Check for expired credits every 24 hours
  
  # Activity requirements for earning credits
  earning-requirements:
    minimum-session-minutes: 5      # Must play at least 5 minutes to start earning
    activity-threshold: 0.3         # Minimum activity score to earn credits
    required-activity-types: 2      # Must perform at least 2 different activity types
    anti-abuse-checks: true         # Enable pattern detection during credit earning
  
  # AFK zone configuration
  afk-zone:
    enabled: true
    world: "world"          # World for AFK zone
    location: "0,100,0"     # X,Y,Z coordinates
    # Optional: yaw,pitch - "0,100,0,0,0"
    
    # Zone protection settings
    protection:
      prevent-damage: true      # Protect players from damage in AFK zone
      prevent-pvp: true         # Disable PVP in AFK zone
      prevent-mob-spawning: true # Prevent hostile mobs
      allow-chat: true          # Allow players to chat while in AFK zone
      show-zone-info: true      # Display zone info on teleport
  
  # Return command configuration
  return-command:
    enabled: true
    command-aliases:
      - "afkback"
      - "afkreturn" 
      - "returnfromafk"
    
    cooldown-seconds: 10        # Cooldown between uses
    max-distance-from-zone: 50  # Maximum distance from AFK zone to use command
    validate-original-location: true  # Check if original location is still safe
    
    # Auto-return options
    auto-return:
      on-activity: false        # Automatically return when player becomes active
      activity-delay-seconds: 30 # Delay before auto-return triggers
  
  # Database integration
  database:
    enabled: false              # Enable persistent credit storage
    table-prefix: "afkplus_"    # Database table prefix
    save-interval-minutes: 5    # How often to save credit data
    cleanup-old-records-days: 30 # Remove records older than 30 days
  
  # Notifications and messages
  notifications:
    credit-earned: true         # Notify when credits are earned
    credit-consumed: true       # Notify when credits are consumed
    low-credits-warning: true   # Warn when credits are running low
    low-credits-threshold: 15   # Warn when less than 15 minutes remain
    zone-teleport: true         # Notify on AFK zone teleport
    successful-return: true     # Notify on successful return
  
  # Integration settings
  integration:
    reward-system-bonus: true   # Give credit bonuses through reward system
    placeholder-updates: true   # Enable placeholder updates
    api-access: true           # Allow API access for other plugins
    event-broadcasting: true    # Broadcast credit events for other plugins
```

**New Messages** (`messages.yml`):
```yaml
# Credit system messages
credit-system:
  earned: "&a+ &f{minutes}m &7AFK credits earned! &8(&f{total}m &7total)"
  consumed: "&c- &f{minutes}m &7AFK credits used &8(&f{remaining}m &7left)"
  low-warning: "&e⚠ &7Low AFK credits: &f{remaining}m &7remaining"
  exhausted: "&c❌ &7AFK credits exhausted! Teleporting to AFK zone..."
  zone-teleport: "&7[AntiAFK+] &aTeleported to AFK zone. Use &f/afkback &ato return."
  
  # Return command messages  
  return:
    success: "&7[AntiAFK+] &aReturned to your previous location!"
    not-in-zone: "&7[AntiAFK+] &cYou must be in the AFK zone to use this command."
    no-saved-location: "&7[AntiAFK+] &cNo saved location found."
    unsafe-location: "&7[AntiAFK+] &cYour previous location is no longer safe. Teleported to spawn."
    cooldown: "&7[AntiAFK+] &cPlease wait &f{seconds}s &cbefore using this command again."
    
  # Status messages
  status:
    balance: "&7AFK Credits: &f{minutes}m &8(&f{hours}h {remaining_minutes}m&8)"
    ratio: "&7Credit Ratio: &f{active}m &7active = &f{credit}m &7AFK credit"
    max-credits: "&7Maximum Credits: &f{max}m"
    expires: "&7Credits expire in: &f{days} &7days"
    
  # Error messages
  errors:
    system-disabled: "&7[AntiAFK+] &cCredit system is disabled."
    invalid-location: "&7[AntiAFK+] &cInvalid AFK zone location configured."
```

#### 🎮 New Commands

**Primary Commands:**
- `/afkback` - Return from AFK zone to original location
- `/afkreturn` - Alias for /afkback  
- `/afkcredits` - Check current credit balance and information
- `/afkcredits reload` - Reload credit system configuration (admin)

**Command Details:**
```yaml
commands:
  afkback:
    description: Return from AFK zone to your previous location
    usage: /afkback
    permission: antiafkplus.credit.return
    aliases: [afkreturn, returnfromafk]
    
  afkcredits:
    description: Check your AFK credit balance
    usage: /afkcredits [reload]
    permission: antiafkplus.credit.check
    permission-reload: antiafkplus.admin.reload
```

#### 🔐 New Permissions

```yaml
permissions:
  # Credit system core
  antiafkplus.credit.earn:
    description: Allows earning AFK credits
    default: true
    
  antiafkplus.credit.use:
    description: Allows using AFK credits
    default: true
    
  antiafkplus.credit.return:
    description: Allows using return commands
    default: true
    
  antiafkplus.credit.check:
    description: Allows checking credit balance
    default: true
  
  # Credit ratio permissions (determines earning rate)
  antiafkplus.credit.ratio.vip:
    description: VIP credit earning ratio
    default: false
    
  antiafkplus.credit.ratio.premium:
    description: Premium credit earning ratio  
    default: false
    
  antiafkplus.credit.ratio.admin:
    description: Admin credit earning ratio
    default: op
  
  # Administrative permissions
  antiafkplus.credit.admin:
    description: Full credit system administration
    default: op
    children:
      - antiafkplus.credit.give
      - antiafkplus.credit.take
      - antiafkplus.credit.set
      - antiafkplus.credit.reset
```

#### ⚡ New API Methods

**Public API Expansion** (`AntiAFKPlusAPI`):
```java
// Credit management
public long getCreditBalance(Player player);
public boolean hasCredits(Player player, long minutes);
public boolean addCredits(Player player, long minutes);
public boolean consumeCredits(Player player, long minutes);
public boolean setCreditBalance(Player player, long minutes);

// Credit information
public String getCreditRatio(Player player);
public long getMaxCredits(Player player);
public List<CreditTransaction> getCreditHistory(Player player);
public Instant getCreditExpiration(Player player);

// AFK zone management  
public boolean isInAFKZone(Player player);
public Location getAFKZoneLocation();
public Location getOriginalLocation(Player player);
public boolean returnFromAFKZone(Player player);

// Credit events
public void registerCreditListener(CreditEventListener listener);
public void unregisterCreditListener(CreditEventListener listener);
```

#### 🎯 Credit System Logic Flow

**1. Credit Earning Process:**
```
Player Active (5 minutes) → Activity Validation → Credit Calculation → Credit Award
                         ↓
Activity Score Check → Pattern Detection → Anti-Abuse Validation → Credit Storage
```

**2. AFK Detection with Credits:**
```
Player Goes AFK → Check Credit Balance → Has Credits? 
                                      ↓
               Yes: Consume Credits → Continue Monitoring
                                      ↓
               No: Save Location → Teleport to AFK Zone → Enable Return Command
```

**3. Return Process:**
```
Player Uses /afkback → Validate in AFK Zone → Check Original Location Safety
                                           ↓
                    Safe: Teleport Back → Clear AFK Status → Success Message
                                           ↓
                    Unsafe: Teleport to Spawn → Warning Message
```

#### ✅ Benefits and Features

**Player Benefits:**
- **Earn AFK Time**: Active gameplay is rewarded with AFK allowance
- **Flexible AFK**: No immediate kicks, gradual credit consumption
- **Safe Return**: Always able to return to original location
- **Transparent System**: Clear feedback on credit earning and consumption

**Server Owner Benefits:**
- **Configurable Ratios**: Adjust credit earning rates by permission groups
- **Abuse Prevention**: Anti-pattern detection during credit earning
- **Zone Management**: Designated safe AFK areas
- **Database Support**: Optional persistent credit storage

**Technical Benefits:**
- **Modular Design**: Integrates seamlessly with existing module system
- **Performance Optimized**: Minimal impact on server performance
- **API Complete**: Full programmatic access for other plugins
- **Event System**: Comprehensive events for custom integrations

#### 🔄 Integration Points

**Existing System Integration:**
- **AFKManager**: Credit verification before actions
- **PatternDetector**: Anti-abuse during credit earning
- **ModuleManager**: Full module lifecycle support
- **PlaceholderAPI**: Credit balance and status placeholders
- **Reward System**: Bonus credits through reward intervals
- **Zone Management**: AFK zone configuration and management

#### 🧪 Testing Scenarios

**Core Functionality Tests:**
1. **Credit Earning**: Verify active play generates credits at correct ratios
2. **Credit Consumption**: Confirm AFK periods consume credits properly
3. **Zone Teleportation**: Test teleport when credits exhausted
4. **Return Command**: Validate return to original location
5. **Permission Integration**: Test different ratios by permission groups

**Edge Case Testing:**
1. **Location Safety**: Handle unsafe original locations
2. **World Changes**: Manage cross-world teleportation
3. **Server Restart**: Verify credit persistence (if database enabled)
4. **Concurrent Usage**: Test multiple players earning/using credits
5. **Configuration Reload**: Ensure system adapts to config changes

### Compatibility

- **Minecraft**: 1.16 - 1.21.8+ (unchanged)
- **Java**: 17+ (unchanged)  
- **Platforms**: Paper, Spigot, Bukkit, Purpur, Folia (full compatibility)
- **Dependencies**: None required, Vault optional for database features

### Migration and Upgrade

**Automatic Migration:**
- Existing configurations remain unchanged
- Credit system starts disabled by default
- No breaking changes to existing functionality
- Seamless integration with current AFK detection

**Manual Configuration:**
- Server owners must enable `credit-system.enabled: true`
- Configure AFK zone location
- Adjust credit ratios for permission groups
- Customize messages and notifications

---

**Version**: 2.5  
**Release Date**: 11/09/2025

**Compatibility**: Minecraft 1.16 - 1.21.8+

**Java**: 17+

---