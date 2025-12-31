# Sistema de Control de Notificaciones de Detección de Patrones - v2.9.4

## Descripción General

Esta nueva funcionalidad permite controlar de manera granular cuándo y a quién se envían las notificaciones de detección de patrones sospechosos en AntiAFKPlus. Resuelve el problema de mensajes spam reportado por usuarios al permitir silenciar completamente las notificaciones a jugadores mientras mantiene alertas opcionales para administradores.

## Configuración

### Ubicación
`config.yml` → `modules.pattern-detection.notifications`

### Opciones Disponibles

```yaml
modules:
  pattern-detection:
    enabled: true
    # ... otras opciones de detección de patrones ...
    
    notifications:
      # Enviar notificación cuando se detecta un patrón por primera vez
      # Default: false (silencioso para evitar spam)
      notify-player-on-detection: false
      
      # Enviar notificación solo cuando se alcanzan las violaciones máximas
      # Default: false (completamente silencioso)
      notify-player-on-violation: false
      
      # Enviar notificación cuando se ejecuta la acción AFK
      # Default: false (acción ocurre silenciosamente)
      notify-player-on-action: false
      
      # Enviar alertas de detección de patrones a admins/staff
      # Los admins deben tener el permiso especificado abajo
      # Default: true (admins pueden monitorear actividad sospechosa)
      send-to-admins: true
      
      # Permiso requerido para que admins reciban alertas
      # Default: "antiafkplus.notify.patterns"
      admin-notification-permission: "antiafkplus.notify.patterns"
```

## Comportamiento por Defecto

Con la configuración por defecto:

- **Jugadores**: NO reciben ninguna notificación
  - No hay mensajes cuando se detecta un patrón
  - No hay mensajes cuando se alcanzan violaciones máximas
  - No hay mensajes cuando se ejecuta la acción AFK
  - La detección y acciones siguen funcionando normalmente

- **Administradores**: Reciben alertas SI tienen el permiso
  - Deben tener el permiso `antiafkplus.notify.patterns`
  - Reciben mensajes formateados con información detallada
  - Formato: `[AntiAFK Admin] <patrón> detected for <jugador> (Violations: X/Y, Confidence: Z%)`

## Casos de Uso

### Caso 1: Silenciar Completamente (Configuración por Defecto)
```yaml
notifications:
  notify-player-on-detection: false
  notify-player-on-violation: false
  notify-player-on-action: false
  send-to-admins: true
```
**Resultado**: Jugadores no ven nada, admins con permiso reciben alertas.

### Caso 2: Notificar Solo en Acción Final
```yaml
notifications:
  notify-player-on-detection: false
  notify-player-on-violation: false
  notify-player-on-action: true  # ← Cambiado
  send-to-admins: true
```
**Resultado**: Jugador solo ve mensaje cuando se ejecuta la acción AFK.

### Caso 3: Notificaciones Completas (Comportamiento Anterior)
```yaml
notifications:
  notify-player-on-detection: true  # ← Cambiado
  notify-player-on-violation: true  # ← Cambiado
  notify-player-on-action: true     # ← Cambiado
  send-to-admins: true
```
**Resultado**: Jugador recibe todos los mensajes (como en versiones anteriores).

### Caso 4: Solo Admins, Sin Notificaciones a Nadie
```yaml
notifications:
  notify-player-on-detection: false
  notify-player-on-violation: false
  notify-player-on-action: false
  send-to-admins: false  # ← Cambiado
```
**Resultado**: Detección funciona pero nadie recibe notificaciones (solo logs del servidor).

## Permisos

### Nuevo Permiso: `antiafkplus.notify.patterns`

**Descripción**: Permite a un jugador recibir notificaciones administrativas sobre detecciones de patrones.

**Uso Recomendado**: Asignar a staff/moderadores/admins que necesiten monitorear actividad sospechosa.

**Ejemplo con LuckPerms**:
```
/lp group admin permission set antiafkplus.notify.patterns true
/lp user <usuario> permission set antiafkplus.notify.patterns true
```

**Ejemplo con PermissionsEx**:
```
/pex group admin add antiafkplus.notify.patterns
/pex user <usuario> add antiafkplus.notify.patterns
```

## Formato de Mensajes

### Mensaje a Jugador (cuando está habilitado)

**En detección**:
```
§c[AntiAFK] Suspicious movement pattern detected: water_circle
```

**En violación**:
```
§c[AntiAFK] Maximum pattern violations reached (3/3)
```

**En acción**:
```
§c[AntiAFK] Suspicious movement pattern detected. AFK action executed.
```

### Mensaje a Admins

```
§6[AntiAFK Admin] §ewater circle §7detected for §eJugador123 §7(Violations: §c2§7/§c3§7, Confidence: §a85%§7)
```

Incluye:
- Tipo de patrón detectado
- Nombre del jugador
- Número de violaciones actual y máximo
- Nivel de confianza de la detección

## Migración desde Versiones Anteriores

Si actualizas desde una versión anterior de AntiAFKPlus:

1. **La configuración se agregará automáticamente** con valores por defecto
2. **Comportamiento cambia**: Los jugadores dejarán de ver mensajes por defecto
3. **Para mantener comportamiento anterior**: Cambia las tres opciones `notify-player-*` a `true`

## Solución de Problemas

### Los jugadores siguen viendo mensajes

**Verificar**:
1. Que `notify-player-on-detection`, `notify-player-on-violation` y `notify-player-on-action` estén en `false`
2. Ejecutar `/afkplus reload` después de cambiar la configuración
3. Revisar que no haya otros plugins enviando mensajes similares

### Los admins no reciben alertas

**Verificar**:
1. Que `send-to-admins` esté en `true`
2. Que el admin tenga el permiso `antiafkplus.notify.patterns` (o el configurado)
3. Ejecutar `/afkplus reload` después de cambiar permisos
4. Verificar con `/lp user <admin> permission check antiafkplus.notify.patterns`

### Quiero cambiar el permiso requerido

Edita `admin-notification-permission` en `config.yml`:
```yaml
admin-notification-permission: "mi.permiso.personalizado"
```

Luego ejecuta `/afkplus reload`

## Notas Técnicas

- Las notificaciones se envían en el hilo principal (thread-safe)
- La detección de patrones sigue funcionando independientemente de las notificaciones
- Las acciones AFK se ejecutan normalmente sin importar si hay notificaciones
- Los eventos de API no se ven afectados por esta configuración
- Los logs del servidor siempre registran las detecciones

## Changelog

**v2.9.4** - Sistema de Control de Notificaciones
- Agregada sección `modules.pattern-detection.notifications` en `config.yml`
- Agregados 5 métodos getter en `ConfigManager.java`
- Modificado `PatternDetector.handleSuspiciousPattern()` para notificaciones configurables
- Agregado método `PatternDetector.sendAdminNotification()` para alertas a staff
- Nuevo permiso: `antiafkplus.notify.patterns`
- Comportamiento por defecto: jugadores sin notificaciones, admins con alertas opcionales
