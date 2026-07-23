# Relevo rápido

Leer este archivo al iniciar una sesión. Consultar el checklist o la guía operativa solo cuando la tarea lo requiera.

## Estado actual

- Rama remota: `main`.
- Commit desplegado: `c27b0061f5a40e107929c641d7bf5b9ca219d1ff`.
- Release activo: `20260723T190511Z`.
- Producción: frontend, autenticación, agenda y PostgreSQL saludables.
- Migraciones: V1–V8 aplicadas correctamente.
- Último bloque: sincronización familiar SSE y lectura durante cortes de red.
- E2E HTTPS aprobado; 45 escenarios Playwright/axe verdes en 320×700, 390×844 y 1280×900.

## Último bloque completado

- PR #28: canal SSE autenticado con comprobación de membresía y conexiones aisladas por familia.
- Los eventos no contienen datos privados; solo IDs opacos y dominios afectados: Hoy, Agenda o Salud.
- Reconexión con backoff de hasta 30 segundos, latidos, resincronización tras cortes y deduplicación acotada.
- Tareas, eventos, tratamientos y tomas invalidan selectivamente las vistas familiares abiertas.
- Las actualizaciones remotas se cargan en segundo plano sin vaciar la pantalla.
- La PWA conserva lecturas ya cargadas en memoria separadas por sujeto y limpia la caché al cerrar sesión.
- Sin conexión se muestra un aviso claro; las altas y toda escritura permanecen bloqueadas.
- Verificación local: 40 pruebas frontend, build Vite y 10 pruebas backend aprobadas.
- CI `30036392835`: frontend y backend/PostgreSQL 18 verdes; 45 escenarios Playwright/axe aprobados.
- Release `20260723T190511Z` saludable; V1–V8 validadas sin migraciones nuevas.
- Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-c27b006-20260723T190510Z`.
- E2E HTTPS: altas, receta cifrada, aislamiento 404, auditoría, responsables e indexación aprobados.

## Siguiente bloque funcional

1. Implementar Objetos como dominio real en un PR independiente.
2. Diseñar migración V9, API y búsqueda por nombre, categoría y ruta física.
3. Aplicar RLS forzado, comprobaciones IDOR, auditoría y pruebas de aislamiento/concurrencia.
4. Integrar la UI validada sin persistencia simulada y mantener el prototipo fuera de producción hasta completar el backend.
5. Web Push privado, preferencias y horario silencioso quedan para un bloque posterior.

## Entrada operativa

```powershell
.\tools\agenda-ops.cmd Resumen
```

Actualizar este archivo reemplazando el estado anterior; no acumular una bitácora histórica. Mantenerlo por debajo de 60 líneas.
