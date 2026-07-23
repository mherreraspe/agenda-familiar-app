# Relevo rápido

Leer este archivo al iniciar una sesión. Consultar el checklist o la guía operativa solo cuando la tarea lo requiera.

## Estado actual

- Rama remota: `main`.
- Commit desplegado: `3e326f97b9ef21dc23c817a1eec72bb1c27c8370`.
- Release activo: `20260723T151623Z`.
- Producción: frontend, autenticación, agenda y PostgreSQL saludables.
- Migraciones: V1–V8 aplicadas correctamente.
- Último bloque: fundamento visual y compactación de tratamientos.
- E2E HTTPS aprobado; 36 escenarios Playwright/axe verdes en 320×700, 390×844 y 1280×900.

## Último bloque completado

- PR #23: fundamento visual de «tablero doméstico claro», con tipografías locales, paleta calmada e iconos SVG propios.
- Encabezado móvil reducido, identificador técnico de la familia oculto y filtro de persona convertido en un selector único.
- Salud usa pestañas ligeras y lenguaje familiar: `Tomas` sustituye a `Ocurrencias` en la interfaz.
- Los tratamientos muestran una fila compacta; indicación, responsables y receta quedan bajo `Ver detalles`.
- En 390×844 la fila de tratamiento bajó de 160 px a 109 px, entran cinco elementos accionables y no existe overflow horizontal.
- Objetos/Baúl no se muestra todavía: requiere migración, API, RLS/IDOR, auditoría y UI en un PR propio.
- Verificación local: 32 pruebas frontend, build Vite, 6 pruebas backend y auditoría de dependencias de producción sin vulnerabilidades.
- CI `30018761203`: frontend y backend verdes; 36 escenarios Playwright/axe aprobados.
- Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-3e326f9-20260723T151623Z`.
- E2E HTTPS: altas, receta cifrada, aislamiento 404, auditoría, responsables e indexación aprobados.
- Revisión visual aislada realizada en 390×844 y 1280×900; la validación autenticada de producción se cubrió mediante E2E HTTPS.

## Siguiente bloque funcional

1. SSE autenticado con reconexión para reflejar cambios entre dispositivos abiertos.
2. Invalidación selectiva de Hoy, Agenda y Salud al recibir eventos familiares.
3. Caché de lectura PWA y aviso claro sin conexión, manteniendo las escrituras solo en línea.
4. Pruebas de reconexión, privacidad, duplicados y concurrencia entre cuidadores.
5. Después: Objetos/Baúl como dominio real; Web Push queda para un bloque posterior.

## Entrada operativa

```powershell
.\tools\agenda-ops.cmd Resumen
```

Actualizar este archivo reemplazando el estado anterior; no acumular una bitácora histórica. Mantenerlo por debajo de 60 líneas.
