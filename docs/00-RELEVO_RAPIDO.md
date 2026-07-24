# Relevo rápido

Leer este archivo al iniciar una sesión. Consultar el checklist o la guía operativa solo cuando la tarea lo requiera.

## Estado actual

- Rama remota: `main`.
- Commit desplegado: `cbadc5a70d5ac546f519a2fbf5bb8e2f41308e9a`.
- Release activo: `20260724T051336Z`.
- Producción: frontend, autenticación, agenda y PostgreSQL saludables.
- Migraciones: autenticación V1–V4 y agenda V1–V12 aplicadas correctamente.
- Último bloque: Salud práctica, tratamientos agrupados y vigencia independiente de envases.
- E2E HTTPS aprobado; 87 escenarios Playwright/axe completados en 320×700, 390×844 y 1280×900.

## Último bloque completado

- PR #42: un tratamiento admite varias personas, horas explícitas o intervalo, dosis/aplicación opcionales y una receta cifrada compartida por el grupo.
- Botiquín modela cada envase por separado: sin abrir/abierto, fecha de apertura, duración posterior, avisos, agotado y descartado.
- La vigencia usa la fecha más temprana entre vencimiento impreso y límite tras abrir; consultas, escrituras, idempotencia e índices permanecen acotados por familia.
- Hoy muestra solo tareas y tomas del día; `Por resolver` es una vista separada e incorpora avisos de Botiquín sin sobrecargar Hoy.
- Verificación local: 45 pruebas frontend, build Vite y 11 pruebas backend aprobadas.
- CI de PR `30068576773` y CI de `main` `30068668429`: frontend y backend/PostgreSQL 18 verdes.
- Release `20260724T051336Z` saludable; autenticación V1–V4 y agenda V1–V12 validadas.
- Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-cbadc5a-20260724T051336Z`.
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
