# Relevo rápido

Leer este archivo al iniciar una sesión. Consultar el checklist o la guía operativa solo cuando la tarea lo requiera.

## Estado actual

- Rama remota: `main`.
- Commit desplegado: `17f175824c16305df5aa2d41592e5776424f145f`.
- Release activo: `20260724T111747Z`.
- Producción: frontend, autenticación, agenda y PostgreSQL saludables.
- Migraciones: autenticación V1–V4 y agenda V1–V12 aplicadas correctamente.
- Último bloque: gestión de miembros familiares y corrección de tratamientos activos.
- E2E HTTPS aprobado; 90 escenarios Playwright/axe completados en 320×700, 390×844 y 1280×900, más 6 verificaciones dirigidas del último cambio.

## Último bloque completado

- PR #44: Administración permite cambiar el rol familiar y dar de baja/reactivar una membresía sin borrar la cuenta ni su historial.
- El último administrador activo está protegido incluso ante cambios concurrentes; la familia elegida se muestra explícitamente y los diálogos no se apilan.
- Un tratamiento puede corregirse mientras está activo; conserva tomas resueltas y reemplaza únicamente horarios y ocurrencias futuras pendientes.
- Se eliminaron por mantenimiento los cuatro tratamientos de Herrera Huertas, sin archivos asociados ni ocurrencias huérfanas; respaldo manual disponible.
- Verificación local: 45 pruebas frontend, build Vite y 11 pruebas backend aprobadas.
- CI de PR `30088911525`: frontend y backend/PostgreSQL 18 verdes.
- Release `20260724T111747Z` saludable; autenticación V1–V4 y agenda V1–V12 validadas.
- Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-17f1758-20260724T111746Z`.
- Respaldo previo al borrado solicitado: `/srv/agenda-familiar/backups/manual/pre-borrado-tratamientos-20260724T071339Z.dump`.
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
