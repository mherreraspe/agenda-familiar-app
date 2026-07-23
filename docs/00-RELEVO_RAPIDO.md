# Relevo rápido

Leer este archivo al iniciar una sesión. Consultar el checklist o la guía operativa solo cuando la tarea lo requiera.

## Estado actual

- Rama remota: `main`.
- Commit desplegado: `368a0de129a68334be3732f20f0ce3ec2ff400b4`.
- Release activo: `20260723T124905Z`.
- Producción: frontend, autenticación, agenda y PostgreSQL saludables.
- Migraciones: V1–V8 aplicadas correctamente.
- Último bloque: formulario de evento adaptativo y rediseño estructural de la PWA.
- E2E HTTPS aprobado; Playwright/axe verde en 320×700, 390×844 y 1280×900.

## Último bloque completado

- PR #17: formulario de evento por URL, contenedor adaptativo, zona horaria familiar, validaciones, sugerencias y conservación del borrador.
- PR #18: AppShell, Pinia, rutas reales, navegación adaptable, filtro compartido y Familia/Actividad en el avatar.
- PR #19: filas compactas, una acción primaria, menú Más, carga e invalidación por dominio y estados independientes.
- Rutas activas: `/hoy`, `/agenda`, `/salud`, `/ajustes/familia` y `/actividad`; rutas antiguas redirigen de forma compatible.
- Objetos/Baúl no se muestra todavía: requiere migración, API, RLS/IDOR, auditoría y UI en un PR propio.
- CI final verde: frontend y backend de la PR #19, ejecución `30007766644`.
- Verificación local: 30 pruebas frontend, build Vite y 6 pruebas backend aprobadas.
- Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-368a0de-20260723T124904Z`.
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
