# Relevo rápido

Leer este archivo al iniciar una sesión. Consultar el checklist o la guía operativa solo cuando la tarea lo requiera.

## Estado actual

- Rama remota: `main`.
- Commit desplegado: `5b9bdd4fc54423eb8e450ecf4a4bce49d3dd32ae`.
- Release activo: `20260718T200358Z`.
- Producción: frontend, autenticación, agenda y PostgreSQL saludables.
- Migraciones: V1–V6 aplicadas correctamente.
- Último bloque: historial de acciones, estados/vencimientos del botiquín y horarios/intervalos.
- E2E de regresión y revisión móvil V6 autenticada: aprobados.

## Último bloque completado

- PR #7: funcionalidad V6. PR #8 y #9: backup/despliegue reproducible y hotfix Bash.
- Migración V6: reprogramación enlazada, cierre idempotente y horarios/intervalos.
- UI: historial con actor, posposición rápida, reprogramación, cierre anticipado y vencimientos cercanos.
- Botiquín: estados calculados `DISPONIBLE`, `POR_VENCER`, `VENCIDO`, `AGOTADO` y `DESCARTADO`.
- Tratamientos: varios horarios, intervalo y responsable alternativo.
- CI: frontend y backend aprobados con Testcontainers.
- Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-5b9bdd4-20260718T200357Z`.
- Producción saludable; V6 aplicada una vez, E2E V5 aprobado y móvil 390×844 sin desborde ni errores de consola.

## Siguiente bloque funcional

1. Recurrencia de eventos y tareas sin perder el historial anterior.
2. Omitir y reprogramar tareas/eventos desde “Hoy” y “Revisar”.
3. Gestión de perfiles, adultos, dependientes y permisos desde la interfaz.
4. Pruebas de recurrencia, transiciones, concurrencia e IDOR/RLS.

## Entrada operativa

```powershell
.\tools\agenda-ops.cmd Resumen
```

Actualizar este archivo reemplazando el estado anterior; no acumular una bitácora histórica. Mantenerlo por debajo de 60 líneas.
