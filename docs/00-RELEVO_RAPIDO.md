# Relevo rápido

Leer este archivo al iniciar una sesión. Consultar el checklist o la guía operativa solo cuando la tarea lo requiera.

## Estado actual

- Rama remota: `main`.
- Commit desplegado: `8acccbaeb56753898b2724a3ba6fd37f1cc194a1`.
- Release activo: `20260723T213222Z`.
- Producción: frontend, autenticación, agenda y PostgreSQL saludables.
- Migraciones: V1–V9 aplicadas correctamente.
- Último bloque: formularios modales y capas interactivas exclusivas.
- E2E HTTPS aprobado; 63 escenarios Playwright/axe verdes en 320×700, 390×844 y 1280×900.

## Último bloque completado

- PR #32: las altas generales y la receta usan la capa modal nativa del navegador y bloquean el fondo.
- El alta de tratamiento cabe en el viewport, desplaza solo el contenido y mantiene Guardar/Cancelar visibles.
- Solo puede permanecer abierto un menú Más; pulsar fuera o Escape lo cierra y devuelve el foco.
- Las altas de tarea, medicamento, tratamiento, perfil y objeto son mutuamente exclusivas y restauran el foco al activador.
- Verificación local: 42 pruebas frontend, build Vite y 10 pruebas backend aprobadas.
- CI `30046463114`: frontend y backend/PostgreSQL 18 verdes; 63 escenarios Playwright/axe aprobados.
- Release `20260723T213222Z` saludable; V1–V9 validadas.
- Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-8acccba-20260723T213222Z`.
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
