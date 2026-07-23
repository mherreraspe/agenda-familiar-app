# Relevo rápido

Leer este archivo al iniciar una sesión. Consultar el checklist o la guía operativa solo cuando la tarea lo requiera.

## Estado actual

- Rama remota: `main`.
- Commit desplegado: `2210abf6d08e81c1d69ff901b3733fe4c366eaee`.
- Release activo: `20260723T134229Z`.
- Producción: frontend, autenticación, agenda y PostgreSQL saludables.
- Migraciones: V1–V8 aplicadas correctamente.
- Último bloque: Salud compacta y menús superiores mutuamente exclusivos.
- E2E HTTPS aprobado; 36 escenarios Playwright/axe verdes en 320×700, 390×844 y 1280×900.

## Último bloque completado

- PR #21: corrección de superposición entre Añadir y avatar; cierre exterior y con Escape.
- Salud muestra una sola subsección: Hoy, Tratamientos, Botiquín o Recetas; la selección queda en la URL.
- Cada colección de Salud muestra inicialmente cinco filas y ofrece expansión progresiva.
- La cuota de fotografías se presenta en Recetas y los vencimientos permanecen dentro de Botiquín.
- Los destinos del avatar conservan semántica de enlace y los activadores exponen `aria-expanded`/`aria-controls`.
- Objetos/Baúl no se muestra todavía: requiere migración, API, RLS/IDOR, auditoría y UI en un PR propio.
- CI final verde: frontend y backend de la PR #21, ejecución `30011966232`.
- Verificación local: 31 pruebas frontend, build Vite y 6 pruebas backend aprobadas.
- Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-2210abf-20260723T134229Z`.
- E2E HTTPS: altas, receta cifrada, aislamiento 404, auditoría, responsables e indexación aprobados.
- No hubo revisión visual autenticada manual porque el navegador de la sesión no estaba disponible; la cobertura automatizada multi-viewport y axe sí fue aprobada.

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
