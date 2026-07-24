# Relevo rápido

Leer este archivo al iniciar una sesión. Consultar el checklist o la guía operativa solo cuando la tarea lo requiera.

## Estado actual

- Rama remota: `main`.
- Commit desplegado: `d73b8c69f4f397301f7fcdeca6df8f0adbd3030b`.
- Release activo: `20260724T003756Z`.
- Producción: frontend, autenticación, agenda y PostgreSQL saludables.
- Migraciones: autenticación V1–V4 y agenda V1–V11 aplicadas correctamente.
- Último bloque: miembros administrados, invitaciones y restablecimientos de un solo uso.
- E2E HTTPS aprobado; 72 escenarios Playwright/axe verdes en 320×700, 390×844 y 1280×900.

## Último bloque completado

- PR #36: `/admin` permite añadir miembros adultos y generar invitaciones de un solo uso válidas durante 48 horas.
- `/activar` permite al invitado elegir su contraseña; el token se retira de la dirección visible y la base conserva solo su hash SHA-256.
- El administrador puede generar restablecimientos válidos durante 30 minutos; al consumirlos se revocan todas las sesiones refresh del usuario.
- Creación, consumo y revocación quedan auditados; expiración, reuso y consumo concurrente están cubiertos con PostgreSQL 18.
- Verificación local: 43 pruebas frontend, build Vite y 10 pruebas backend aprobadas.
- CI de PR `30056539025` y CI de `main` `30056643411`: frontend y backend/PostgreSQL 18 verdes.
- Release `20260724T003756Z` saludable; autenticación V1–V4 y agenda V1–V11 validadas.
- Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-d73b8c6-20260724T003756Z`.
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
