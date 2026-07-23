# Relevo rápido

Leer este archivo al iniciar una sesión. Consultar el checklist o la guía operativa solo cuando la tarea lo requiera.

## Estado actual

- Rama remota: `main`.
- Commit desplegado: `5032b1067bb8c5c588bde8ff1e26cf269b92b01b`.
- Release activo: `20260723T165603Z`.
- Producción: frontend, autenticación, agenda y PostgreSQL saludables.
- Migraciones: V1–V8 aplicadas correctamente.
- Último bloque: estados operativos, historial de tomas y prototipo de Objetos.
- E2E HTTPS aprobado; 39 escenarios Playwright/axe verdes en 320×700, 390×844 y 1280×900.

## Último bloque completado

- PR #26: Hoy vacío se reduce a `Todo está al día`; `Por resolver` solo aparece con contenido y no duplica tareas vencidas.
- El historial de tomas sale de Hoy y usa una vista propia en Salud con carga de 10 registros por bloque.
- Agenda, Tratamientos y Botiquín usan una única alta contextual; los menús móviles se presentan como hojas inferiores.
- La cuota de recetas permanece oculta por debajo del 70 %.
- Prototipo visual de Objetos disponible solo en desarrollo bajo `/prototipo/objetos`; está ausente del build productivo.
- El nombre previsto es `Objetos`; la pantalla responde a `¿Qué estás buscando?` y muestra rutas como `Habitación › Ropero › Caja`.
- Objetos todavía requiere migración, API, RLS/IDOR, auditoría y UI productiva en un PR propio.
- Verificación local: 34 pruebas frontend, build Vite y 6 pruebas backend aprobadas.
- CI `30024979468`: frontend y backend verdes; 39 escenarios Playwright/axe aprobados.
- Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-5032b10-20260723T165603Z`.
- E2E HTTPS: altas, receta cifrada, aislamiento 404, auditoría, responsables e indexación aprobados.
- Revisión visual aislada realizada en 390×844 para Hoy, Agenda, Salud, Historial y Objetos; cero overflow y axe sin impactos graves.

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
