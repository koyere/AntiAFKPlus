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
- 🌍 Per-world AFK system (`enabled-worlds` & `disabled-worlds`)
- 🔐 AFK time per permission group
- 🛡️ Voluntary AFK time limit (`max-voluntary-afk-time-seconds`)
- 🚫 Prevent item pickup while AFK
- 📶 Detect autoclickers (experimental, optional)
- 🔧 Reload settings instantly with `/afkplus reload`
- 🎨 Fully customizable messages (`messages.yml`)
- 🔒 Bypass permission support
- 🧠 PlaceholderAPI integration: `%antiafkplus_status%`, `%antiafkplus_afktime%`
- 📈 Built-in bStats usage tracking
- ⚙️ Developer-friendly public API

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
| `antiafkplus.list`       | Allows access to `/afk list`                |
| `antiafkplus.admin`      | Allows use of admin commands                |

---

## ⚙️ Configuration

- All main settings are in `config.yml`
- All messages are now in `messages.yml` for better organization
- `enabled-worlds` and `disabled-worlds` supported
- Warnings before kick can be customized in seconds (`afk-warnings`)
- Custom timeouts based on permissions (`permission-times`)
- Toggle verbose logging with `debug: true`
- Enable item pickup restriction while AFK
- Enable or disable experimental autoclick detection

---

## 🧩 PlaceholderAPI Support

If PlaceholderAPI is installed, you can use:
- `%antiafkplus_status%` → `AFK` / `ACTIVE`
- `%antiafkplus_afktime%` → seconds since last movement

---

## 🧱 Developer API

AntiAFKPlus includes a clean and extensible API so you can interact with player AFK state.

**Example:**

```java
AntiAFKPlusAPI api = AntiAFKPlusAPI.getInstance();
boolean isAfk = api.isAFK(player);
```
You can also check last movement time and register your own behaviors via events.

## 📊 Metrics
This plugin uses bStats to collect anonymous usage statistics.
You can disable it in /plugins/bStats/config.yml.

## 👤 Author
Developed by Koyere
💬 Support Discord: https://discord.gg/xKUjn3EJzR

## 🛠️ License
Licensed under the MIT License — open, free and safe to use.