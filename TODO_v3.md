# AntiAFKPlus v3.0 Premium — TODO Completo

> Precio: $5.99 | Reescritura completa | Versión Premium
> Fecha de inicio: Abril 2026

---

## ÍNDICE

1. [FASE 1 — Limpieza de código muerto](#fase-1--limpieza-de-código-muerto)
2. [FASE 2 — Corrección de configuración ambigua y redundante](#fase-2--corrección-de-configuración-ambigua-y-redundante)
3. [FASE 3 — Activar funciones existentes que no funcionan](#fase-3--activar-funciones-existentes-que-no-funcionan)
4. [FASE 4 — Mejoras de calidad del config.yml](#fase-4--mejoras-de-calidad-del-configyml)
5. [FASE 5 — Features Premium nuevas](#fase-5--features-premium-nuevas)
6. [FASE 6 — Internacionalización real](#fase-6--internacionalización-real)
7. [FASE 7 — Pulido final y lanzamiento](#fase-7--pulido-final-y-lanzamiento)

---

## FASE 1 — Limpieza de código muerto ✅ COMPLETADA

Eliminar todo lo que existe en el código pero no hace absolutamente nada.

### 1.1 Campo `afkLogger` en AntiAFKPlus.java ✅
- **Archivo:** `AntiAFKPlus.java`
- **Resuelto:** Eliminado el campo `private AFKLogger afkLogger`, su getter `getAFKLogger()`, la asignación `this.afkLogger = null` en Phase 1, la referencia en `nullifyReferences()`, y el import `AFKLogger`. La clase `AFKLogger.java` (utilidad estática) se mantiene intacta.

### 1.2 Campo `migrationRequired` en AntiAFKPlus.java ✅
- **Resuelto:** Eliminado el campo `migrationRequired`, el getter `wasMigrated()`, y la constante `MIN_MIGRATION_VERSION`.

### 1.3 ModuleManager — loops vacíos ✅
- **Resuelto:** Eliminados los loops vacíos en `initializeModules()` y `shutdown()`. Reemplazados con comentarios claros de no-op. Eliminados campos muertos (`totalInitializationTime`, `totalShutdownTime`, `performanceLogging`, `logger`), imports no usados (`Bukkit`, `Collectors`, `Logger`), y el método `getPerformanceStats()` que nadie llamaba.

### 1.4 Clase `Module.java` — framework sin usuarios ✅ (pendiente para Fase 3.4)
- **Estado:** La clase se mantiene como base para la Fase 3.4 donde se crearán implementaciones concretas. Se agregaron marcadores `v3.0 TODO` en ModuleManager.

### 1.5 Imports no usados en AntiAFKPlus.java ✅
- **Resuelto:** Eliminados `import PatternDetector`, `import CreditListener`, `import AFKLogger`.

---

## FASE 2 — Corrección de configuración ambigua y redundante ✅ COMPLETADA

### 2.1 Triple toggle de Pattern Detection — CONSOLIDAR ✅
- **Resuelto:** Eliminada toda la sección `enhanced-detection` del config.yml. Eliminados los campos `enhancedDetectionEnabled`, `patternDetectionEnabled`, `behavioralAnalysisEnabled`, `advancedMovementTrackingEnabled` y el método `loadEnhancedDetectionSettings()` de ConfigManager. Los getters `isEnhancedDetectionEnabled()` e `isPatternDetectionEnabled()` se mantienen como `@Deprecated` delegando a `isPatternDetectionModuleEnabled()` para no romper PatternDetector.java y AFKManager.java. Toggle único: `modules.pattern-detection.enabled`.

### 2.2 Autoclick Detection duplicada — CONSOLIDAR ✅
- **Resuelto:** El toggle principal ahora lee de `modules.autoclick-detection.enabled`. `loadAutoclickSettings()` lee primero de `modules.autoclick-detection` y cae a las keys legacy como fallback para configs pre-v3.0. Eliminadas las secciones `autoclick-detection: true` y `autoclick-detection-settings:` del config.yml por defecto.

### 2.3 Unidades de tiempo inconsistentes — ESTANDARIZAR ✅
- **Resuelto:** Las keys de config ahora usan segundos: `pattern-analysis-interval-seconds`, `keystroke-timeout-seconds`, `activity-grace-period-seconds`. El código lee las nuevas keys y multiplica por 1000 internamente. Si detecta las keys legacy con sufijo `-ms`, las lee como fallback para backward compatibility.

### 2.4 Sección `credit-system` duplicada — SIMPLIFICAR ✅
- **Resuelto:** Eliminado `modules.credit-system.enabled` del config.yml. La condición en AntiAFKPlus.java ahora solo verifica `credit-system.enabled`. Un solo toggle.

### 2.5 Secciones de config sin implementación — ELIMINADAS ✅
- Eliminadas: `migration-info`, `compatibility`, `technical` (thread-pool-size, object-pool-size, etc.), `integrations.vault`, `integrations.discordsrv`, `analytics.web-dashboard`, `modules.vault-integration`.
- Eliminado `vault-integration` de ModuleManager.java.

---

## FASE 3 — Activar funciones existentes que no funcionan ✅ COMPLETADA

Estas son configuraciones que se cargan en memoria pero nunca se usan en la lógica del plugin.

### 3.1 Activity Scoring Weights — IMPLEMENTAR ✅
- **Archivos modificados:** `AFKManager.java`
- **Resuelto:** `PlayerActivityData` ahora recibe un `EnumMap<ActivityType, Double>` con los pesos de config. El método `recalculateScore()` usa `configWeights.getOrDefault(entry.type, entry.type.getActivityWeight())` — si hay peso en config lo usa, si no cae al default del enum. El método `buildActivityWeights()` en AFKManager lee los 4 pesos de ConfigManager (`movement`, `head-rotation`, `jump`, `command`) y los pasa al crear cada `PlayerActivityData`. Los admins ahora pueden ajustar cuánto "cuenta" cada tipo de actividad.

### 3.2 Movement Detection Settings — IMPLEMENTAR ✅
- **Archivos modificados:** `MovementListener.java`
- **Resuelto:** Las 5 constantes `static final` (`MICRO_MOVEMENT_THRESHOLD`, `HEAD_ROTATION_THRESHOLD`, `JUMP_SPAM_THRESHOLD`, `MAX_JUMPS_PER_PERIOD`, `JUMP_RESET_PERIOD`) reemplazadas por campos de instancia que se cargan desde ConfigManager en el constructor y vía `loadConfigThresholds()`. Los admins ahora pueden ajustar la sensibilidad de detección de movimiento, rotación de cabeza y saltos.

### 3.3 Event System Toggles — IMPLEMENTAR ✅
- **Archivos modificados:** `AFKManager.java`, `PatternDetector.java`
- **Resuelto:** Cada `callEvent()` ahora está envuelto en un check condicional:
  - `isAFKWarningEventsEnabled()` → antes de `PlayerAFKWarningEvent`
  - `isAFKKickEventsEnabled()` → antes de `PlayerAFKKickEvent`
  - `isAFKStateChangeEventsEnabled()` → antes de `PlayerAFKStateChangeEvent`
  - `isPatternDetectionEventsEnabled()` → antes de `PlayerAFKPatternDetectedEvent`
  - Los eventos se crean siempre (para uso interno) pero solo se disparan a listeners externos si el toggle está habilitado. Desactivar eventos mejora rendimiento en servidores que no usan plugins que escuchen estos eventos.

### 3.4 ModuleManager — Implementación real (pendiente para Fase 5)
- **Estado:** Diferido a Fase 5 donde se crearán módulos concretos (GUI, Visual Effects, etc.) que extiendan la clase `Module`.

---

## FASE 4 — Mejoras de calidad del config.yml ✅ COMPLETADA

### 4.1 Reducir tamaño del config.yml ✅
- **Resultado:** 1033 → 580 líneas (44% reducción). Eliminados comentarios redundantes, secciones muertas, y bloques de documentación excesivos. Cada sección tiene un header limpio y los comentarios inline son concisos.

### 4.2 Eliminar secciones de config sin implementación ✅
- **Eliminadas en Fase 2:** `migration-info`, `compatibility`, `technical`, `integrations.vault`, `integrations.discordsrv`, `analytics.web-dashboard`, `modules.vault-integration`, `modules.credit-system` (toggle duplicado), `enhanced-detection`, `autoclick-detection` (legacy raíz), `autoclick-detection-settings` (legacy raíz).
- **Mantenidas con razón:** `visual-effects`, `analytics`, `reward-system`, `database` — tienen config que será implementada en Fase 5 o ya tiene código parcial.

### 4.3 Eliminar mensajes de features inexistentes en messages.yml ✅
- **Eliminados (nunca referenciados en código):**
  - `activity-score-low`, `activity-score-warning`, `activity-score-critical` — activity score no se muestra al jugador
  - `behavioral-analysis-warning`, `enhanced-detection-warning` — nunca enviados
  - `micro-movement-detected`, `head-rotation-activity` — nunca enviados
  - `swim-state-activity`, `fly-state-activity` — nunca enviados
  - `afk-reward-earned`, `afk-reward-limit` — reward system no implementado
  - `hologram-afk-status`, `particle-effect-enabled` — visual effects no implementado
  - `migration-complete`, `migration-backup-created`, `migration-settings-preserved` — migración no usa mensajes
  - `version-enhanced-features`, `version-feature-disabled` — nunca enviados
  - Mensajes duplicados al final del archivo (`protection-vulnerable`, `protection-movement-blocked`)
- **Mantenidos:** `zone-entered-*` (se usarán cuando zone management se active), todos los mensajes de `credit-system`, `protection-*` (usados por PlayerProtectionListener), `server-transfer` (usados por ServerTransferService).
- **Resultado:** 280 → 233 líneas. Archivo limpio, sin duplicados, sin mensajes huérfanos.

### 4.4 Limpiar header del config.yml ✅
- **Resuelto:** Header actualizado a "AntiAFKPlus v3.0 Premium". Eliminadas las checkmarks de features no implementadas. Header conciso con solo links de documentación y Discord.

### 4.5 Reorganización general ✅
- **config.yml:** Secciones reordenadas en flujo lógico: Modules → Basic Settings → Warnings → Worlds → Toggles → Detection → Scoring → Events → Windows → Credits → Transfer → Protection → Zones → Rewards → Visual → Database → Analytics → i18n → Bedrock → Performance → Integrations → Permissions.
- **messages.yml:** Reorganizado con separadores claros por categoría. Eliminados comentarios de "Phase 2.0B/C/D" que referenciaban versiones internas.

---

## FASE 5 — Features Premium nuevas ✅ COMPLETADA

### 5.1 GUI de configuración in-game ✅ (PRIORIDAD ALTA)
- **Archivos creados:** `gui/GUIManager.java`, `gui/GUIType.java`
- **Archivos modificados:** `AntiAFKPlus.java`, `AFKPlusCommand.java`
- **Implementado:**
  - Comando `/afkplus gui` abre el menú principal (requiere permiso `antiafkplus.reload`)
  - **Menú Principal (54 slots):** Info del plugin (cabeza), Detection Settings, Module Toggles, Credit System info, Performance stats en tiempo real, Profile selector, Debug toggle, Reload config, AFK player count
  - **Submenú Detection Settings:** Muestra water circle radius, min samples, max violations, pattern analysis interval. Toggles para linear movement exclusion, large pool detection, keystroke timeout. Botón back.
  - **Submenú Module Toggles:** 8 módulos como LIME_WOOL/RED_WOOL. Click para toggle → guarda config → recarga automática.
  - Bordes de glass pane negros para apariencia profesional
  - Todos los cambios se persisten en config.yml y se recargan automáticamente
  - GUIManager se inicializa en Phase 6, se apaga en onDisable, se nullifica en cleanup
  - Tab completion incluye "gui" como subcomando

### 5.4 Perfiles de detección ✅ (PRIORIDAD ALTA)
- **Implementado dentro de GUIManager:**
  - Slot 37 del menú principal: selector de perfil que cicla entre conservative → balanced → aggressive
  - **Conservative:** max-violations=12, threshold=0.98, min-samples=50, grace-period=90s
  - **Balanced:** max-violations=8, threshold=0.95, min-samples=40, grace-period=60s
  - **Aggressive:** max-violations=4, threshold=0.85, min-samples=25, grace-period=30s
  - Auto-detecta el perfil actual basado en max-violations
  - Guarda en config.yml y recarga automáticamente

### 5.2 Visual Effects reales ✅ (PRIORIDAD ALTA)
- **Archivo creado:** `visual/VisualEffectsManager.java` (~220 líneas)
- **Archivos modificados:** `AntiAFKPlus.java` (campo, init, shutdown, getter)
- **Implementado:**
  - **Partículas:** Task repetitivo (cada 1s) que spawna partículas configurables sobre jugadores AFK. Tipo, cantidad, velocidad y offsets leídos de config.
  - **Tab List:** Prefijo [AFK] en la lista de jugadores. Guarda nombres originales y los restaura al volver de AFK.
  - **Name Tags:** Prefijo [AFK] sobre la cabeza del jugador. Mismo patrón de backup/restore.
  - Escucha `PlayerAFKStateChangeEvent` para aplicar/remover efectos automáticamente.
  - Limpieza en `PlayerQuitEvent` y `shutdown()`.
  - Solo se inicializa si `modules.visual-effects.enabled: true`.

### 5.3 Sistema de Analytics real ✅ (PRIORIDAD MEDIA)
- **Implementado en:** `AFKPlusCommand.java` subcomando `/afkplus status`
- **Muestra:** Versión, uptime, online/AFK count, módulos activos, estado de pattern detection y credit system, TPS y memoria.
- Requiere permiso `antiafkplus.stats`.

### 5.4 Perfiles de detección ✅ (PRIORIDAD ALTA) — Implementado en 5.1
- Ver sección 5.1 arriba. Integrado en el GUI como selector de perfil.

### 5.5 Transferencia de créditos entre jugadores ✅ (PRIORIDAD MEDIA)
- **Archivos modificados:** `CreditManager.java`, `AFKCreditsCommand.java`
- **Implementado:**
  - Método `CreditManager.transferCredits(Player from, Player to, long minutes)` con validación de saldo y max credits.
  - Comando `/afkcredits transfer <jugador> <minutos>` con permiso `antiafkplus.credit.transfer`.
  - Validaciones: saldo suficiente, jugador online, no auto-transferencia, max credits del receptor.
  - Transacciones registradas en historial SQL si está habilitado.

### 5.6 Leaderboard de créditos ✅ (PRIORIDAD MEDIA)
- **Archivos modificados:** `CreditManager.java`, `AFKCreditsCommand.java`, `PlaceholderHook.java`
- **Implementado:**
  - Método `CreditManager.getTopCredits(int limit)` que ordena jugadores por saldo descendente.
  - Comando `/afkcredits top [cantidad]` (default 10, max 50).
  - Placeholder `%antiafkplus_credits_rank%` que muestra la posición del jugador en el ranking.

### 5.7 Eventos de multiplicador de créditos ✅ (PRIORIDAD BAJA)
- **Implementado en:** `AFKPlusCommand.java` subcomando `/afkplus event credits <multiplicador> <duración_minutos>`
- **Funcionalidad:** Guarda multiplicador y timestamp de expiración en config. Broadcast a todos los jugadores online. Validación de rango (0.1-10x, 1-1440 min).
- Requiere permiso `antiafkplus.reload`.

### 5.8 Integración Vault/Economía ✅ (PRIORIDAD MEDIA)
- **Archivo creado:** `integrations/VaultIntegration.java`
- **Implementado vía reflection** (sin dependencia de compilación, igual que WorldGuard):
  - `getBalance(Player)`, `withdraw(Player, double)`, `deposit(Player, double)`, `has(Player, double)`
  - Auto-detecta Vault en el servidor y obtiene el Economy provider.
  - Se inicializa solo si `integrations.vault.enabled: true` en config.
  - Getter `getVaultIntegration()` en AntiAFKPlus.java.

### 5.9 Integración DiscordSRV ✅ (PRIORIDAD BAJA)
- **Archivo creado:** `integrations/DiscordSRVIntegration.java`
- **Implementado vía reflection** (sin dependencia de compilación):
  - Escucha `PlayerAFKStateChangeEvent` y envía notificaciones al canal principal de Discord.
  - Auto-detecta DiscordSRV y obtiene el canal de texto vía JDA reflection.
  - Se inicializa solo si `integrations.discordsrv.enabled: true` en config.
  - Configurable: `send-afk-notifications`, `send-pattern-alerts`.

### 5.10 Dashboard de rendimiento in-game ✅ (PRIORIDAD BAJA)
- **Implementado en:** `AFKPlusCommand.java` subcomando `/afkplus performance`
- **Muestra:** TPS, avg execution time, total operations, memoria, cache entries, componentes tracked, jugadores high/low activity.
- Requiere permiso `antiafkplus.stats`.

### Resumen Fase 5 — Archivos y métricas
- **Archivos nuevos:** 5 (`GUIManager.java`, `GUIType.java`, `VisualEffectsManager.java`, `VaultIntegration.java`, `DiscordSRVIntegration.java`)
- **Archivos modificados:** 6 (`AntiAFKPlus.java`, `AFKPlusCommand.java`, `AFKCreditsCommand.java`, `CreditManager.java`, `PlaceholderHook.java`, `config.yml`)
- **Nuevos comandos:** `/afkplus gui`, `/afkplus status`, `/afkplus performance`, `/afkplus event credits`, `/afkcredits transfer`, `/afkcredits top`
- **Nuevos placeholders:** `%antiafkplus_credits_rank%`
- **Total archivos compilados:** 65 → 70 (+5)
- **Compilación:** BUILD SUCCESS, 0 errores
- **Muestra:** TPS, avg execution time, total operations, memoria, cache entries, componentes tracked, jugadores high/low activity.
- Requiere permiso `antiafkplus.stats`.

---

## FASE 6 — Internacionalización real ✅ COMPLETADA

### 6.1 Sistema unificado de mensajes
**`messages.yml` eliminado.** Todos los mensajes vienen de `languages/*.yml`.

**Flujo:**
1. Al iniciar, `LocalizationManager` crea `languages/` y extrae los 10 archivos built-in
2. Carga todos los `.yml` del directorio
3. `ConfigManager.getMessage()` delega a `LocalizationManager` usando `internationalization.default-language`
4. Los getters pre-cargados se cargan desde el idioma default al iniciar
5. El admin edita directamente los archivos en `languages/` para personalizar mensajes

### 6.2 Cambios realizados ✅
1. **`setupLanguageDirectory()` activado** — Crea `plugins/AntiAFKPlus/languages/` y extrae los archivos built-in. En `developmentMode` sobreescribe los existentes.
2. **`loadLanguages()` reescrito** — Carga todos los archivos `.yml` del directorio `languages/`. Valida que el idioma default exista, cae a inglés si no. Loguea cuántos idiomas se cargaron.
3. **`BUILT_IN_LANGUAGES` actualizado** — `Set.of("en", "es", "fr", "de", "pt", "ru", "zh", "ja", "ko", "it")` (10 idiomas).
4. **10 archivos de idioma creados** en `src/main/resources/languages/`:
   - `en.yml` — English
   - `es.yml` — Español
   - `fr.yml` — Français
   - `de.yml` — Deutsch
   - `pt.yml` — Português
   - `ru.yml` — Русский
   - `zh.yml` — 中文
   - `ja.yml` — 日本語
   - `ko.yml` — 한국어
   - `it.yml` — Italiano
5. Cada archivo incluye: metadatos del idioma, ~30 mensajes traducidos, y configuración de formato (fecha, hora, números) localizada.

### Resumen Fase 6
- **Archivos nuevos:** 10 archivos de idioma completos (~190 líneas cada uno, ~147 message keys)
- **Archivos modificados:** `LocalizationManager.java` (3 métodos), `ConfigManager.java` (eliminado messages.yml, getMessage delega a LocalizationManager)
- **Archivos eliminados:** `messages.yml` — ya no existe, un solo sistema de mensajes
- **Recursos totales:** config.yml + plugin.yml + 10 idiomas = 12
- **Idiomas:** en, es, fr, de, pt, ru, zh, ja, ko, it
- **Compilación:** BUILD SUCCESS, 0 errores

---

## FASE 7 — Pulido final y lanzamiento ✅ COMPLETADA

### 7.1 Actualizar versiones ✅
- `pom.xml`: `2.9.5` → `3.0.0`
- `plugin.yml`: `2.9.5` → `3.0.0`, description actualizada a premium
- `AntiAFKPlus.java`: `PLUGIN_VERSION` y `API_VERSION` → `3.0.0`, Javadoc actualizado
- `config.yml`: version ya era `"3.0"` desde Fase 4

### 7.2 Actualizar licencia ✅
- `pom.xml`: MIT License → Commercial License
- `LICENSE`: Reescrito como licencia comercial (1 servidor, no redistribuir, no decompile)

### 7.3 Limpiar warnings del IDE ✅
- 8 `printStackTrace()` → reemplazados con `getLogger().log(Level.SEVERE, message, exception)`
- `instanceof` pattern → Java 17 pattern matching en `onDisable()`
- `new bStatsManager(this)` → asignado a campo `bStatsMetrics`
- `fromVersion` unused → `@SuppressWarnings("unused")`
- Import `java.util.logging.Level` agregado

### 7.4 Limpiar comentarios en español ✅
- 4 comentarios en español traducidos a inglés en `AntiAFKPlus.java`
- Javadocs de "Fase 2" y "Fase 4" simplificados

### 7.5 Documentación ✅
- `CHANGELOG_3_0.md` creado con todas las mejoras, features nuevas, breaking changes, y resumen de archivos

### 7.6 Build final ✅
- `mvn clean package` → **BUILD SUCCESS**
- JAR generado: `antiafkplus-3.0.0.jar`
- 70 archivos Java compilados, 12 recursos empaquetados, 0 errores

---

## RESUMEN DE PRIORIDADES

| Prioridad | Tarea | Impacto Premium |
|---|---|---|
| 🔴 ALTA | Fase 1: Limpieza código muerto | Base limpia |
| 🔴 ALTA | Fase 2: Config ambigua | Profesionalismo |
| 🔴 ALTA | Fase 4: Calidad config.yml | Primera impresión |
| 🔴 ALTA | 5.1: GUI in-game | ⭐ Feature estrella |
| 🔴 ALTA | 5.2: Visual Effects | Diferenciador visual |
| 🔴 ALTA | 5.4: Perfiles de detección | Facilidad de uso |
| 🟡 MEDIA | Fase 3: Activar funciones rotas | Funcionalidad completa |
| 🟡 MEDIA | Fase 6: Idiomas reales | Alcance global |
| 🟡 MEDIA | 5.3: Analytics | Valor para admins |
| 🟡 MEDIA | 5.5: Transfer créditos | Engagement jugadores |
| 🟡 MEDIA | 5.6: Leaderboard | Engagement jugadores |
| 🟡 MEDIA | 5.8: Vault integration | Ecosistema servidor |
| 🟢 BAJA | 5.7: Multiplicador eventos | Nice-to-have |
| 🟢 BAJA | 5.9: DiscordSRV | Nice-to-have |
| 🟢 BAJA | 5.10: Dashboard rendimiento | Nice-to-have |
| 🔴 ALTA | Fase 7: Pulido y lanzamiento | Release quality |
