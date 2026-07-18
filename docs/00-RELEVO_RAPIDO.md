# Relevo rápido

Leer este archivo al iniciar una sesión. Consultar el checklist o la guía operativa solo cuando la tarea lo requiera.

## Estado actual

- Rama remota: `main`.
- Commit desplegado: `231a411f9edc193570697234919424ad09999c19`.
- Release activo: `20260718T163608Z`.
- Producción: frontend, autenticación, agenda y PostgreSQL saludables.
- Migraciones: V1–V5 aplicadas correctamente.
- Último bloque: auditoría visible, catálogo privado de lugares y sugerencias privadas por familia.
- E2E V5 y revisión móvil autenticada: aprobados.

## Trabajo local sin publicar

- Checklist actualizado después del despliegue.
- Guía operativa y comandos reutilizables para futuras sesiones.
- E2E V5 promovido de archivo temporal a `tools/e2e/validar-v5.py`.
- Optimización de contexto y logs completada: relevo corto y validaciones resumidas con logs locales.

## Siguiente bloque funcional

1. Historial completo para completar, omitir, posponer, reprogramar y cerrar.
2. Vencimientos cercanos y estados calculados del botiquín.
3. Horarios/intervalos y responsable alternativo.
4. Pruebas de transiciones, concurrencia e IDOR/RLS.

## Entrada operativa

```powershell
.\tools\agenda-ops.cmd Resumen
```

Actualizar este archivo reemplazando el estado anterior; no acumular una bitácora histórica. Mantenerlo por debajo de 60 líneas.
