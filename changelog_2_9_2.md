# AntiAFKPlus v2.9.2 — Dynamic Credit Group Detection & Multi-Server

Release type: Bugfix & Enhancement
Compatibility: Minecraft 1.16 – 1.21.11 | Java 17+

---

## What's Fixed

### 🔧 Credit System Group Detection
- **Dynamic Group Reading**: Plugin now automatically detects ALL groups defined in `credit-ratios` and `max-credits` config sections
- **No More Hardcoded Limitations**: Previously only supported `admin`, `premium`, and `vip` groups — now supports unlimited custom groups
- **Custom Group Support**: Works with any group name: `sponsor`, `premium+`, `vip+`, `owner`, `member`, etc.
- **Permission Format**: Uses `antiafkplus.credit.ratio.<groupname>` pattern (e.g., `antiafkplus.credit.ratio.sponsor`)
- **Priority System**: Implements configurable group priority order (admin > owner > sponsor > premium+ > vip+ > premium > vip > default)

### 📝 Technical Improvements
- Added dynamic `ConfigurationSection` reading for group detection
- Implemented intelligent caching system for group priority calculations
- Cache automatically clears on `/afkplus reload` for instant config updates
- Enhanced debug logging to show matched groups per player
- Improved error handling with fallback to default values

---

## What's Documented

### 🌐 Multi-Server Credit System Setup
- **Fully Documented**: Complete documentation for running multiple servers (Skyblock, Survival, etc.) with separate credit systems sharing one database
- **Table Prefix Feature**: Use `table-prefix` config option to separate credit tables per server
- **Configuration Examples**: Step-by-step guides added to `config.yml` and `README.md`
- **New Documentation File**: `MULTI_SERVER_DOCUMENTATION.md` with complete setup guide, troubleshooting, and security best practices

**How It Works:**
- Each server uses a different `table-prefix` (e.g., `skyblock_`, `survival_`)
- All servers connect to the same MySQL/SQLite database
- Credits are completely independent per server
- No code changes needed — configuration only!

---

## Configuration Example

```yaml
credit-system:
  credit-ratios:
    default: '5:1'
    vip: '4:1'
    premium: '3:1'
    premium+: '2:1'
    sponsor: '1:30'     # ✅ Now works!
    owner: '1:60'       # ✅ Custom groups supported!

  max-credits:
    default: 120
    vip: 180
    premium: 240
    sponsor: 500
    owner: 1000

  # Optional: Customize priority order
  group-priority-order:
    - owner
    - admin
    - sponsor
    - premium+
    - vip+
    - premium
    - vip
```

### Multi-Server Setup Example

**Skyblock Server** (`config.yml`):
```yaml
credit-system:
  database:
    enabled: true
    table-prefix: "skyblock_"  # Skyblock tables

database:
  type: "MySQL"
  mysql:
    host: "localhost"
    database: "antiafkplus"
```

**Survival Server** (`config.yml`):
```yaml
credit-system:
  database:
    enabled: true
    table-prefix: "survival_"  # Survival tables

database:
  type: "MySQL"
  mysql:
    host: "localhost"
    database: "antiafkplus"  # Same database!
```

**Result**: Independent credit systems (`skyblock_credits`, `survival_credits`) in one shared database.

### Permission Setup (LuckPerms Example)
```bash
/lp group sponsor permission set antiafkplus.credit.ratio.sponsor true
/lp group premium+ permission set antiafkplus.credit.ratio.premiumplus true
```

**Note**: Plus signs (`+`) in group names convert to `plus` in permission nodes.

---

## Compatibility

- **Minecraft**: 1.16 – 1.21.11
- **Platforms**: Bukkit, Spigot, Paper, Purpur, Folia
- **Java**: 17+
- **Permission Plugins**: LuckPerms, PermissionsEx, GroupManager, etc.
- **Fully Backward Compatible**: Existing configurations continue to work

---

## Upgrade Notes

1. Replace the old JAR with `AntiAFKPlus v2.9.2`
2. Update LuckPerms (or other permission plugin) with new group permissions:
   - Format: `antiafkplus.credit.ratio.<groupname>`
3. Optional: Customize group priority order in `config.yml`
4. **NEW**: Check `config.yml` for multi-server `table-prefix` examples if you run multiple servers
5. Reload with `/afkplus reload` to apply changes
6. Enable `debug: true` in config to verify group detection

> Existing `admin`, `premium`, and `vip` groups continue to work without changes. New custom groups require permission node setup.

> **Multi-Server Users**: If you want to separate credit systems across multiple servers, configure different `table-prefix` values in each server's `config.yml` (see documentation).

---

## For Developers

**API Version**: 2.9.2 (unchanged API, internal improvements only)

No breaking changes to the public API. The credit system now uses dynamic group detection internally, improving flexibility for server administrators.

---

**Version**: 2.9.2
**Release Date**: 19/12/2025 (Documentation Update)
**Compatibility**: Minecraft 1.16 - 1.21.11
**Java**: 17+
