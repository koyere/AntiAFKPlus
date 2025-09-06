# AntiAFKPlus v2.4.1 Changelog

## 🐛 Critical Bug Fix

### Issue Resolved
**Problem**: Players were being kicked/teleported repeatedly every 5 seconds instead of only once after reaching the AFK timeout threshold.

**Root Cause**: The AFK check task was calling the kick/teleport action repeatedly during each check interval (every 5 seconds) instead of executing it only once per AFK session.

### Solution Implemented

#### 🔧 Technical Fix
- **Added action state tracking** to prevent repeated kick/teleport actions for the same AFK session
- **Implemented safety delay** to prevent immediate actions upon AFK detection
- **Added automatic cleanup** of action state when players become active again
- **Maintained full compatibility** with all existing features and API

#### ✅ Result
- Players are now **kicked/teleported only once** after reaching their AFK timeout
- AFK timeout configurations (like `default-afk-time: 600`) now work correctly
- All other plugin functionality remains unchanged and fully operational

---

**Version**: 2.4.1  
**Release Date**: 2025-09-06  
**Type**: Critical Bug Fix  
**Compatibility**: Minecraft 1.16 - 1.21.8
**Java**: 17+

### Upgrade Notes
- **No configuration changes required**
- **Fully backward compatible**
- Simply replace the JAR file and restart your server