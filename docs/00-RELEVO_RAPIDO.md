# Relevo rápido

Leer este archivo al iniciar una sesión. Consultar el checklist o la guía operativa solo cuando la tarea lo requiera.

## Estado actual

- Rama remota: `main`.
- Commit desplegado: `b1518b3e681035892e5a60830d0d4e133380f04e`.
- Release activo: `20260724T141134Z`.
- Producción: frontend, autenticación, agenda y PostgreSQL saludables.
- Migraciones: autenticación V1–V4 y Agenda V1–V13 aplicadas correctamente.
- Último bloque: bandeja privada y generación de avisos de tareas, eventos, salud y botiquín.
- E2E HTTPS aprobado; 96 escenarios Playwright/axe completados en 320×700, 390×844 y 1280×900.

## Último bloque completado

- PR #48: campana global y bandeja `Pulso familiar`, inferior en móvil y lateral en escritorio.
- Leer un aviso no resuelve la tarea, evento, toma ni revisión de origen.
- Preferencias privadas por usuario y familia para tareas, eventos, salud, botiquín y horario silencioso.
- Las tareas pueden avisar al vencer; los eventos admiten avisos 24 h y 1 h antes.
- El motor genera avisos deduplicados, conserva aislamiento familiar y solo entrega a membresías activas.
- Agenda V13 añade preferencias, bandeja y suscripciones Web Push con FORCE RLS e índices por usuario.
- La pantalla bloqueada usa siempre texto genérico; endpoints y claves de dispositivo se validan y pueden revocarse.
- Las claves VAPID siguen vacías: la bandeja interna funciona y la activación push permanece deshabilitada con explicación visible.
- Verificación local: 50 pruebas frontend, build Vite y 11 pruebas backend aprobadas.
- Playwright/axe: 96 escenarios verdes; revisión real aprobada en los tres viewports.
- CI de PR `30099653318` y CI de `main` `30099813895`: frontend y backend verdes.
- Release `20260724T141134Z` saludable; V13 y E2E HTTPS real validados.
- Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-b1518b3-20260724T141133Z`.

## Siguiente bloque funcional

1. Autorizar y configurar claves VAPID mediante el flujo protegido, sin registrarlas en Git.
2. Probar entrega Web Push real en iPhone instalado como PWA y en un segundo dispositivo.
3. Verificar permiso denegado, renovación de suscripción, endpoint expirado y revocación por dispositivo.
4. Decidir si se añaden avisos de cambios familiares importantes o escalamiento opcional.
5. Continuar con sesiones activas y revocación individual, sin mezclar este alcance con Push.

## Entrada operativa

```powershell
.\tools\agenda-ops.cmd Resumen
```

Actualizar este archivo reemplazando el estado anterior; no acumular una bitácora histórica. Mantenerlo por debajo de 60 líneas.
