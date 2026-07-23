# Relevo rápido

Leer este archivo al iniciar una sesión. Consultar el checklist o la guía operativa solo cuando la tarea lo requiera.

## Estado actual

- Rama remota: `main`.
- Commit desplegado: `c155e00f974645a612e4561cdb50ed640319a249`.
- Release activo: `20260723T233543Z`.
- Producción: frontend, autenticación, agenda y PostgreSQL saludables.
- Migraciones: autenticación V1–V3 y agenda V1–V10 aplicadas correctamente.
- Último bloque: administración global y alta real de familias.
- E2E HTTPS aprobado; 66 escenarios Playwright/axe verdes en 320×700, 390×844 y 1280×900.

## Último bloque completado

- PR #34: rol global `ADMINISTRADOR_PLATAFORMA` firmado en JWT y protegido en backend.
- Ruta `/admin` con listado y alta real e idempotente de familias; acceso oculto y denegado a usuarios familiares.
- Auditoría de plataforma sin contenido médico y migración V10 con aislamiento por actor.
- Verificación local: 43 pruebas frontend, build Vite y 10 pruebas backend aprobadas.
- CI de PR `30053417092` y CI de `main` `30053507456`: frontend y backend/PostgreSQL 18 verdes.
- Release `20260723T233543Z` saludable; autenticación V1–V3 y agenda V1–V10 validadas.
- Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-c155e00-20260723T233543Z`.
- E2E HTTPS: altas, receta cifrada, aislamiento 404, auditoría, responsables e indexación aprobados.

## Siguiente bloque funcional

1. Añadir miembros desde `/admin` mediante invitaciones de un solo uso, con vencimiento y hash del token.
2. Entregar un enlace que permita al invitado establecer su propia contraseña; nunca mostrar ni almacenar la contraseña en claro.
3. Permitir al administrador iniciar un restablecimiento mediante enlace de un solo uso, con revocación de sesiones al completarlo.
4. Auditar creación, consumo, expiración y revocación sin guardar tokens ni contenido familiar.
5. Probar aislamiento, enumeración, reuso, concurrencia, expiración, CSRF e IDOR antes de desplegar.

## Entrada operativa

```powershell
.\tools\agenda-ops.cmd Resumen
```

Actualizar este archivo reemplazando el estado anterior; no acumular una bitácora histórica. Mantenerlo por debajo de 60 líneas.
