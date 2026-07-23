# Relevo rápido

Leer este archivo al iniciar una sesión. Consultar el checklist o la guía operativa solo cuando la tarea lo requiera.

## Estado actual

- Rama remota: `main`.
- Commit desplegado: `7723d3efca13f5606a0ec0f1482054079a0f17ae`.
- Release activo: `20260723T203410Z`.
- Producción: frontend, autenticación, agenda y PostgreSQL saludables.
- Migraciones: V1–V9 aplicadas correctamente.
- Último bloque: Objetos como dominio familiar persistente.
- E2E HTTPS aprobado; 51 escenarios Playwright/axe verdes en 320×700, 390×844 y 1280×900.

## Último bloque completado

- PR #30: migración V9 con objetos, ubicaciones jerárquicas e idempotencia; las tres tablas tienen RLS forzado.
- API autenticada para buscar por nombre, categoría o lugar, crear y editar/mover objetos con control optimista de versión.
- IDOR devuelve 404, las rutas no pueden cruzar familias y la auditoría registra altas y cambios con actor adulto.
- `/objetos` sustituyó al prototipo DEV; responde “¿Qué estás buscando?” y muestra rutas con `›`.
- Objetos participa en SSE familiar, reconexión y lectura ya cargada; las escrituras siguen solo en línea.
- Verificación local: 41 pruebas frontend, build Vite y 10 pruebas backend aprobadas.
- CI `30042503806`: frontend y backend/PostgreSQL 18 verdes; 51 escenarios Playwright/axe aprobados.
- Release `20260723T203410Z` saludable; V1–V9 validadas.
- Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-7723d3e-20260723T203410Z`.
- E2E HTTPS: altas, receta cifrada, aislamiento 404, auditoría, responsables e indexación aprobados.

## Siguiente bloque funcional

1. Implementar Web Push con mensaje bloqueado genérico y contenido privado solo tras autenticar.
2. Añadir preferencias por tipo de aviso y horario silencioso por familia/persona.
3. Deduplicar, reintentar y permitir escalamiento opcional sin filtrar datos sensibles.
4. Probar revocación, aislamiento familiar, múltiples dispositivos y navegadores sin permiso.
5. Mantener SSE como fuente de actualización en vivo y las escrituras offline deshabilitadas.

## Entrada operativa

```powershell
.\tools\agenda-ops.cmd Resumen
```

Actualizar este archivo reemplazando el estado anterior; no acumular una bitácora histórica. Mantenerlo por debajo de 60 líneas.
