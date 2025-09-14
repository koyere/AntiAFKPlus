# AntiAFKPlus v2.6 — Server Transfer & Scripted Sequences

Release type: Feature Release  
Compatibility: Minecraft 1.16 – 1.21.8+ | Java 17+

## What’s New

### Server Transfer (Bungee/Velocity)
- Native final action `TRANSFER_SERVER` to move AFK players to another server via Plugin Messaging.
- Global configuration with robust channel handling and optional retry policy.
- Zone-based overrides: per-zone transfer with `kick-action: TRANSFER` and `transfer-server`.

### Countdown & Titles/Sounds
- Optional per-second countdown with Title/Subtitle and Sound before transferring.
- Fully Folia-safe with entity-bound scheduling.
- Auto-cancels if the player becomes active.

### Scripted Action Pipeline
- New pipeline engine to run steps in order: `TITLE`, `SUBTITLE`, `SOUND`, `MESSAGE`, `WAIT`, `TRANSFER`.
- Configurable in `config.yml` with simple DSL-like syntax.
- Cancels automatically when leaving AFK.

### Robustness & Fallbacks
- Auto-registration of `BungeeCord` and `bungeecord:main` channels.
- Channel selection: `auto | bungeecord | namespaced`.
- Fallback actions: `KICK | TELEPORT | NONE` with optional teleport location.

## How It Works (Action Order)
1) Credit System: If credits exist, the kick event is cancelled and credits are consumed; no transfer runs.
2) Zone Management: Zone `kick-action` takes priority (`TELEPORT`, `TRANSFER`, etc.).
3) Global Transfer: If enabled and a `target-server` is configured, `TRANSFER_SERVER` is used by default.

If the player becomes active mid-flow, any countdown/pipeline is cancelled.

## Configuration

Global settings:
```yaml
server-transfer:
  enabled: true
  target-server: "lobby"
  proxy-channel: "auto"        # auto | bungeecord | namespaced

  # Fallbacks
  fallback-action: "KICK"      # KICK | TELEPORT | NONE
  fallback-teleport-location: "world,0,100,0"

  # Retry policy
  retry-attempts: 0
  retry-delay-ticks: 10

  # Countdown
  countdown:
    enabled: false
    seconds: 10
    title: "&cYou are AFK"
    subtitle: "&eMoving in {seconds}s"
    sound:
      enabled: true
      name: "ENTITY_EXPERIENCE_ORB_PICKUP"
      volume: 1.0
      pitch: 1.0

  # Scripted pipeline
  pipeline:
    enabled: false
  actions:
    - "TITLE: &cYou are AFK"
    - "SUBTITLE: &eMoving in {seconds}s"
    - "SOUND: ENTITY_EXPERIENCE_ORB_PICKUP,1.0,1.0"
    - "WAIT: 1s"
    - "MESSAGE: &7Transferring..."
    - "TRANSFER: lobby"
```

Zone-based transfer example:
```yaml
zone-management:
  enabled: true
  zones:
    spawn:
      kick-action: "TRANSFER"
      transfer-server: "lobby"
```

## Messages
Add or customize in `messages.yml`:
```yaml
messages:
  server-transfer:
    transferring: "&7[AntiAFK+] &aTransferring you to &f{server}&a..."
    unavailable: "&7[AntiAFK+] &cServer transfer unavailable."
    failed: "&7[AntiAFK+] &cCould not transfer you."
```

## Permissions & Commands
- No new commands or permissions in v2.6. The feature is fully configurable via `config.yml`.

## Compatibility & Notes
- BungeeCord and Velocity (Bungee compatibility) via Plugin Messaging.
- Folia-safe scheduling through PlatformScheduler; no BukkitScheduler in Folia context.
- Fully backward compatible; disabled by default.

## Migration
- No breaking changes. Existing configurations remain valid.
- To enable: set `server-transfer.enabled: true` and configure `target-server`.

## Testing Checklist
- Validate transfer on Bungee/Velocity with correct `target-server` names.
- Try countdown and pipeline separately.
- Confirm fallback behavior (KICK/TELEPORT/NONE) when channels are unavailable.
- Verify zone-based `TRANSFER` overrides global behavior.
