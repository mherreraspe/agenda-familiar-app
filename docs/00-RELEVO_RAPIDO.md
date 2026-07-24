# Relevo rápido

Leer este archivo al iniciar una sesión. Consultar el checklist o la guía operativa solo cuando la tarea lo requiera.

## Estado actual

- Rama remota: `main`.
- Commit desplegado: `23e65f7cab7324ccb1ff0b62cc12771005900d67`.
- Release activo: `20260724T123957Z`.
- Producción: frontend, autenticación, agenda y PostgreSQL saludables.
- Migraciones: autenticación V1–V4 y agenda V1–V12 aplicadas correctamente.
- Último bloque: diferenciación de tareas, eventos, citas y salidas en Agenda.
- E2E HTTPS aprobado; 93 escenarios Playwright/axe completados en 320×700, 390×844 y 1280×900.

## Último bloque completado

- PR #46: Agenda ofrece `Tarea` y `Evento, cita o salida` desde un alta contextual limitada a ese dominio.
- Las tarjetas distinguen el tipo mediante texto, símbolo y color; las tareas conservan `Hecho` y los eventos no simulan una acción de completar.
- El tipo o categoría del evento es visible y admite `Cita` y `Salida o visita` sin migración de base de datos.
- `Tarea o recordatorio` se renombró a `Tarea` mientras no exista una notificación real.
- Verificación local: 47 pruebas frontend, build Vite y 11 pruebas backend aprobadas.
- Playwright/axe: 93 escenarios verdes en 320×700, 390×844 y 1280×900.
- CI de PR `30092897920` y CI de `main` `30093885014`: frontend y backend verdes.
- Release `20260724T123957Z` saludable; autenticación V1–V4 y agenda V1–V12 validadas.
- Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-23e65f7-20260724T123957Z`.
- La PWA adoptó el nuevo frontend tras una recarga; una pestaña abierta durante el despliegue mostró el release anterior en su primer render.
- E2E HTTPS: altas, receta cifrada, aislamiento 404, auditoría, responsables e indexación aprobados.

## Siguiente bloque funcional

1. Implementar Web Push solo después de permiso explícito del usuario y por dispositivo autenticado.
2. Enviar notificaciones privadas sin nombres, medicamentos ni contenido sensible en la pantalla bloqueada; abrir la PWA autenticada para ver el detalle.
3. Aislar suscripciones por usuario y familia, permitir revocar cada dispositivo y eliminar endpoints inválidos.
4. Mantener Hoy como bandeja operativa; las notificaciones no deben duplicar ni reemplazar sus estados de atención.
5. Probar permisos denegados, aislamiento, renovación de suscripción, entrega duplicada y dispositivo revocado.

## Entrada operativa

```powershell
.\tools\agenda-ops.cmd Resumen
```

Actualizar este archivo reemplazando el estado anterior; no acumular una bitácora histórica. Mantenerlo por debajo de 60 líneas.
