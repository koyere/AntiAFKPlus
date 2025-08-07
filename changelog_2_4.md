# AntiAFKPlus v2.4 Changelog

## 🎯 Large AFK Pool Detection Enhancement

### Problem Identified
**Issue**: Players were bypassing AFK detection by creating large AFK pools (20x10+ blocks) that circumvented existing detection systems.

**Root Cause**: 
- Current detection focused on small confined spaces (≤5x5 blocks) and water circles (≤3 block radius)
- Large pools allowed players to move naturally via water currents without manual input
- System detected movement but couldn't distinguish between manual keystrokes and automatic water current movement

### Solution Implemented

#### 🔧 New Detection Features

**1. Keystroke Timeout Detection**
- Added detection for players who don't provide manual keyboard input for extended periods
- Distinguishes between manual WASD movement and automatic water current movement
- Configurable timeout threshold (default: 3 minutes)

**2. Large AFK Pool Pattern Recognition**
- Expanded detection to identify pools between 5x5 and 25x25 blocks
- Analyzes movement patterns to identify water current automation
- Multi-factor validation prevents false positives

**3. Enhanced Movement Analysis**
- Velocity analysis to detect consistent water current speeds
- Direction change analysis to identify artificial vs natural movement
- Automatic vs manual movement classification

#### 📋 Technical Changes

**Files Modified:**
- `MovementListener.java`: Added keystroke detection and movement analysis
- `PatternDetector.java`: Implemented large pool detection algorithms
- `config.yml`: Added configuration options for new detection methods

**New Configuration Options:**
```yaml
pattern-detection-settings:
  large-pool-threshold: 25.0  # Maximum area for large AFK pools
  keystroke-timeout-ms: 180000  # 3 minutes without manual input
  automatic-movement-velocity-threshold: 0.15  # Water current detection
```

#### 🎮 Detection Logic

**Large AFK Pool Detection Criteria:**
1. Movement area > 5x5 but < 25x25 blocks
2. Player in water for extended periods
3. Movement patterns consistent with water currents
4. No manual keystrokes detected for 3+ minutes

**Only triggers AFK when ALL criteria are met**, ensuring legitimate gameplay is not affected.

#### ✅ Benefits

- **Closes bypass loophole**: Large AFK pools (20x10+) are now detected effectively
- **Maintains accuracy**: Multi-factor validation prevents false positives
- **Configurable**: Server owners can adjust thresholds based on their needs
- **Performance optimized**: Minimal impact on server performance
- **Backward compatible**: All existing detection methods remain unchanged

---

**Version**: 2.4  
**Release Date**: 2025-08-07  
**Compatibility**: Minecraft 1.16 - 1.21.8
**Java**: 17+