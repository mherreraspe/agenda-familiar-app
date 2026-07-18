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

## Integrado pendiente de despliegue

- PR #7 integrado en `main`: `f7c27c91b3a06ebe008747f713707cf502a49661`.
- Migración V6: reprogramación enlazada, cierre idempotente y horarios/intervalos.
- UI: historial con actor, posposición rápida, reprogramación, cierre anticipado y vencimientos cercanos.
- Botiquín: estados calculados `DISPONIBLE`, `POR_VENCER`, `VENCIDO`, `AGOTADO` y `DESCARTADO`.
- Tratamientos: varios horarios, intervalo y responsable alternativo.
- `VerificarLocal`: aprobado. CI del PR #7: frontend y backend aprobados con Testcontainers.

## Trabajo operativo en curso

- Rama: `agent/despliegue-operativo-v6`.
- Añadir `agenda-ops.cmd Desplegar` con backup predeploy y paquete inmutable.

## Siguiente paso

1. Integrar la acción de despliegue con CI verde.
2. Desplegar V6 y repetir E2E móvil.
3. Actualizar el commit/release desplegado y reemplazar este relevo.

## Entrada operativa

```powershell
.\tools\agenda-ops.cmd Resumen
```

Actualizar este archivo reemplazando el estado anterior; no acumular una bitácora histórica. Mantenerlo por debajo de 60 líneas.
