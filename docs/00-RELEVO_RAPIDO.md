# Relevo rápido

Leer este archivo al iniciar una sesión. Consultar el checklist o la guía operativa solo cuando la tarea lo requiera.

## Estado actual

- Rama remota: `main`.
- Commit desplegado: `aeb1f81fed396ecf189722ab7b94048dd1798fc6`.
- Release activo: `20260724T025339Z`.
- Producción: frontend, autenticación, agenda y PostgreSQL saludables.
- Migraciones: autenticación V1–V4 y agenda V1–V11 aplicadas correctamente.
- Último bloque: cuenta administrativa separada y resolución de la familia real del usuario.
- E2E HTTPS aprobado; 78 escenarios Playwright/axe completados en 320×700, 390×844 y 1280×900.

## Último bloque completado

- PR #38: la cuenta administrativa global se separó de `familia_test`; login y renovación redirigen automáticamente a `/admin` según el rol firmado.
- PR #39: `AdminAcceso` usa el proyecto Compose real para recuperar acceso sin exponer secretos ni modificar volúmenes.
- PR #40: `GET /familias` devuelve solo membresías activas del sujeto bajo RLS; el frontend eliminó el UUID fijo de `familia_test` en datos y SSE.
- Una membresía selecciona su familia automáticamente; varias habilitan cambio en el menú y ninguna muestra un estado explícito.
- Verificación local: 45 pruebas frontend, build Vite y 11 pruebas backend aprobadas.
- CI de PR `30062673683` y CI de `main` `30062763663`: frontend y backend/PostgreSQL 18 verdes.
- Release `20260724T025339Z` saludable; autenticación V1–V4 y agenda V1–V11 validadas.
- Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-aeb1f81-20260724T025339Z`.
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
