# AntiAFKPlus

AntiAFKPlus is a lightweight and highly configurable Minecraft plugin that automatically detects AFK players and takes action based on their inactivity.

✅ Supports Minecraft versions **1.16 – 1.21.5**  
✅ No dependencies required (optional PlaceholderAPI and bStats)  
✅ Built with performance and customization in mind

---

## 🔧 Features

- ⏱️ Kick players after configurable AFK time
- 🧪 Warn players before being kicked (multiple intervals)
- 🎭 Manual AFK toggle command (`/afk`)
- 📋 View who is AFK with `/afk list`
- 🌍 Per-world AFK system (world whitelist)
- 🔐 AFK time per permission group
- 🔒 Bypass permission support
- 🛑 Exclude creative/spectator players
- 📦 Reload settings with `/afkplus reload`
- 🎨 Fully customizable messages (via `messages.yml`)
- 🧠 Optional AFK voluntary limit (for `/afk`)
- 📈 Built-in bStats usage tracking
- ⚙️ Clean API for other plugins to integrate

---

## 📦 Commands & Permissions

| Command              | Description                         | Permission            |
|----------------------|-------------------------------------|------------------------|
| `/afk`               | Toggle AFK mode manually            | `antiafkplus.afk`      |
| `/afk list`          | Show list of AFK players            | `antiafkplus.list`     |
| `/afkplus reload`    | Reload config and messages          | `antiafkplus.admin`    |

| Permission               | Description                                 |
|--------------------------|---------------------------------------------|
| `antiafkplus.bypass`     | Exempts player from AFK checks              |
| `antiafkplus.afk`        | Allows use of `/afk` command                |
| `antiafkplus.list`       | Allows access to `/afk list`               |
| `antiafkplus.admin`      | Allows use of admin commands                |

---

## 🔄 Configuration

- All main settings are in `config.yml`
- All messages are in `messages.yml`
- Permission times and AFK check interval customizable
- Compatible with PlaceholderAPI (`%antiafkplus_afktime%`)

---

## 📦 Developer API

AntiAFKPlus includes a public API so you can interact with AFK status programmatically.

Example usage:

```java
AntiAFKPlusAPI api = AntiAFKPlusAPI.getInstance();
boolean isAfk = api.isAFK(player);
```
## 🧱 Metrics
This plugin uses bStats to collect anonymous usage statistics.
You can disable it in the /plugins/bStats/config.yml.

## 👤 Author
Developed by Koyere

Discord: https://discord.gg/xKUjn3EJzR
## 🛠️ License
Licensed under the MIT License — open, free and safe to use.