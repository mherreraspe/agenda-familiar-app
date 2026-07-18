# Relevo rápido

Leer este archivo al iniciar una sesión. Consultar el checklist o la guía operativa solo cuando la tarea lo requiera.

## Estado actual

- Rama remota: `main`.
- Commit desplegado: `e5beb0bc904cc8d7bd99d96af137f90e8c568e3d`.
- Release activo: `20260718T205050Z`.
- Producción: frontend, autenticación, agenda y PostgreSQL saludables.
- Migraciones: V1–V7 aplicadas correctamente.
- Último bloque: recurrencia, acciones de agenda y administración familiar.
- E2E HTTPS y revisión móvil V7 autenticada: aprobados.

## Último bloque completado

- PR #11 integrado en `main`; CI `29660227013` verde (frontend 13 s, backend 1 min 11 s).
- Migración V7: series de tareas/eventos y acciones idempotentes con RLS forzado.
- Recurrencia diaria, semanal y mensual materializada sin borrar instancias anteriores.
- Omitir y reprogramar tareas/eventos desde “Hoy”, “Revisar” y calendario; reprogramación enlazada.
- Interfaz para perfiles adultos/dependientes, cuentas vinculadas y permisos, restringida a administradores.
- Pruebas PostgreSQL 18: recurrencia, historial, idempotencia, concurrencia, IDOR, RLS y permisos.
- Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-e5beb0b-20260718T205050Z`.
- Producción saludable; V7 aplicada una vez y E2E con aislamiento 404, auditoría e indexación aprobado.
- PWA autenticada a 390×844: bundle V7, recurrencia y gestión familiar visibles; sin desborde ni errores de consola.

## Siguiente bloque funcional

1. Fotografía opcional de receta desde la PWA con reducción y eliminación de EXIF/ubicación.
2. Almacenamiento privado, descarga autorizada, miniaturas y eliminación completa.
3. Cuota familiar con avisos y bloqueo de nuevas fotos al alcanzar el límite.
4. Pruebas de archivos inválidos/maliciosos, aislamiento, cuota y concurrencia.

## Entrada operativa

```powershell
.\tools\agenda-ops.cmd Resumen
```

Actualizar este archivo reemplazando el estado anterior; no acumular una bitácora histórica. Mantenerlo por debajo de 60 líneas.
