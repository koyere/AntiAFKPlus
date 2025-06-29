# 🚀 AntiAFKPlus v2.1.0

**Bug Fix Release - Essential Improvements**

---

## 🐛 Bug Fixes

### ⚙️ **Configuration Management**
- **Fixed `/afkplus reload` command** - Now properly reloads all configuration files and settings
- **Improved config validation** - Better error handling and validation during reload
- **Fixed startup messages** - Removed color codes from console logs for cleaner output

### 🎣 **AFK Detection Improvements**  
- **Added fishing detection** - Fishing machines and AFK fishing farms are now properly detected
- **Enhanced activity tracking** - PlayerFishEvent now triggers anti-AFK systems
- **Better automation detection** - Catches fish, items, and reel-in actions count as player activity

### 🔧 **Code Quality**
- **Version consistency** - Updated all version references to 2.1
- **Cleaner logging** - Console messages no longer show formatting codes
- **Import optimization** - Better code organization and reduced redundancy

---

# 🚀 AntiAFKPlus v2.0

**Major Release - Complete Plugin Redesign**

---

## 🎉 What's New

### 🏗️ **Modular Architecture**
- **Enable/disable features independently** - Only use what you need
- **17 different modules** including core detection, pattern analysis, rewards, and integrations
- **Zero performance impact** from disabled modules
- **Easy customization** for any server type

### 🌐 **Multi-Platform Support**
- **Auto-detection** of Folia, Paper, Spigot, Bukkit, and Purpur
- **Automatic fallback** ensures compatibility on any server
- **Future-proof** design for upcoming Minecraft versions

### 🌍 **Complete Internationalization**
- **20+ languages** supported out of the box
- **Automatic language detection** from player's client
- **Per-player language settings** for multinational servers
- **Fully translatable** messages and interfaces

### 📱 **Bedrock Edition Support**
- **Automatic detection** of Bedrock players via Floodgate/Geyser
- **Touch-friendly interfaces** and menu adaptations
- **Optimized experience** for mobile and console players

---

## ⚡ Performance Improvements

### 🎯 **Zero-Lag Operation**
- **50% less CPU usage** compared to v1.x
- **Adaptive check intervals** based on server load
- **Smart caching system** reduces database queries by 80%
- **Object pooling** minimizes garbage collection impact

### 📊 **Real-Time Monitoring**
- **Live performance metrics** and TPS impact tracking
- **Automatic optimization** when server load increases
- **Memory usage limits** prevent server strain
- **Per-module performance tracking**

---

## 🤖 Advanced Detection Features

### 🔍 **Pattern Recognition**
- **Bot detection** for circular movements and confined spaces
- **Autoclick/macro detection** with configurable sensitivity
- **AFK pool detection** for common farming setups
- **False positive prevention** with smart algorithms

### 📈 **Behavioral Analysis**
- **Activity scoring system** tracks 18 different player actions
- **Movement pattern analysis** detects repetitive behavior
- **Micro-movement detection** catches subtle bot activities
- **Learning algorithms** improve detection over time

---

## 🔧 Enhanced Configuration

### ⚙️ **Simplified Setup**
- **Auto-configuration** for most servers
- **Configuration validation** prevents errors
- **Automatic migration** from v1.x settings
- **Hot-reload** for all settings without restart

### 🎛️ **Advanced Options**
- **Per-world AFK settings** with inheritance
- **Permission-based timeouts** for different player groups
- **Custom warning intervals** and messages
- **Zone-based AFK management** with WorldGuard integration

---

## 🔌 Integrations & API

### 🌟 **New Integrations**
- **WorldGuard** - Region-based AFK settings
- **Vault** - Economy integration for rewards
- **DiscordSRV** - Discord notifications for AFK events
- **Enhanced PlaceholderAPI** - 15+ new placeholders

### 💻 **Professional API**
- **80+ API methods** for developers
- **Event system** with 12 different event types
- **Async operations** for performance-critical applications
- **Thread-safe design** for multi-threaded plugins

---

## 🛡️ Player Protection Features

### 🔒 **AFK Player Safety**
- **Invulnerability system** prevents damage while AFK
- **Movement restriction** options to prevent exploitation
- **Interaction blocking** to prevent griefing
- **Automatic position restoration** when returning

### 🎁 **Reward System**
- **AFK rewards** for idle time (configurable)
- **Anti-abuse measures** prevent exploitation
- **Economic integration** with Vault-compatible plugins
- **Daily limits** and playtime requirements

---

## 🎨 Visual & UI Improvements

### ✨ **Enhanced Effects**
- **Hologram support** (DecentHolograms, HolographicDisplays)
- **Particle effects** for AFK players
- **Tab list modifications** show AFK status
- **Customizable visual indicators**

### 📱 **Mobile-Friendly Interface**
- **Touch-optimized menus** for Bedrock players
- **Simplified layouts** for smaller screens
- **Gesture-friendly controls** for mobile devices

---

## 🔧 Quality of Life Improvements

### 📋 **Better Commands**
- **Improved `/afk list`** with sorting and filtering
- **Status command** shows plugin health and performance
- **Debug commands** for troubleshooting
- **Auto-completion** for all commands

### 📊 **Enhanced Analytics**
- **Detailed statistics** tracking for administrators
- **Export functionality** for data analysis
- **Performance reports** for optimization
- **Usage patterns** help configure optimal settings

---

## 🐛 Bug Fixes & Stability

### 🛠️ **Major Fixes**
- **Memory leak** in player tracking system resolved
- **Thread safety** issues fixed for high-load servers
- **Edge cases** in AFK detection improved
- **Configuration reload** now works correctly in all scenarios

### 🔒 **Security Improvements**
- **Input validation** prevents configuration exploits
- **Permission checks** strengthened across all features
- **API access** properly secured for third-party plugins

---

## 📦 Migration & Compatibility

### 🔄 **Automatic Migration**
- **Seamless upgrade** from v1.x with automatic config conversion
- **Backup creation** before migration for safety
- **Setting preservation** maintains your customizations
- **Rollback option** if needed

### ✅ **Backward Compatibility**
- **API compatibility** maintained for existing plugins
- **Command structure** unchanged for user familiarity
- **Permission system** extended but compatible

---

## 📝 Installation Notes

1. **Backup your server** before upgrading
2. **Replace the old JAR** with the new v2.0 file
3. **Restart your server** (reload not supported for major versions)
4. **Review new config options** in `config.yml`
5. **Test AFK detection** with different player scenarios

**Minimum Requirements:**
- **Java 17+** (recommended: Java 21)
- **Minecraft 1.16+** (tested up to 1.21.5)
- **4GB+ RAM** for optimal performance with all modules

---

## 🔗 Links & Support

- 📖 **Documentation**: Updated wiki with all new features
- 💬 **Discord Support**: https://discord.gg/xKUjn3EJzR
- 🐛 **Bug Reports**: GitHub Issues
- ⭐ **Rate & Review**: Help others discover this plugin!

---

**Download AntiAFKPlus v2.0.0-ENTERPRISE now and experience the most advanced AFK management system for Minecraft!**

---

# Previous Versions

## [1.1] - 2025-05-04

### Added
- Configurable max voluntary AFK time (`max-voluntary-afk-time-seconds`)
- Separated `messages.yml` with full color support
- New command `/afk list` to view AFK players (with permission)
- New config value: `list-command-permission`
- New messages: `afk-list-header`, `afk-list-format`, `afk-list-empty`
- Public API (`AntiAFKPlusAPI`) for external plugin integration
- Enhanced logic to prevent auto-kick on player join
- Auto-clear AFK on movement or login activity

### Fixed
- Broadcasts not showing when entering/leaving AFK
- Kick message not using correct config value
- Warnings not appearing as expected
- Persistent kick loop after rejoin
- Reload system not reflecting changes in messages/config

## [1.0] - Initial Release

- Kick players after a configurable time
- Per-permission AFK times
- Bypass and reload support