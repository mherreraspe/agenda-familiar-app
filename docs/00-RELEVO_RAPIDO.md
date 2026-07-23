# Relevo rápido

Leer este archivo al iniciar una sesión. Consultar el checklist o la guía operativa solo cuando la tarea lo requiera.

## Estado actual

- Rama remota: `main`.
- Commit desplegado: `56225719f04eff1b17e0b8d95ef2d7c84879c3e6`.
- Release activo: `20260719T033351Z`.
- Producción: frontend, autenticación, agenda y PostgreSQL saludables.
- Migraciones: V1–V8 aplicadas correctamente.
- Último bloque: fotografías privadas de recetas y cuota familiar.
- E2E HTTPS y revisión móvil V8 autenticada: aprobados.

## Último bloque completado

- PR #13 funcional y PR #14–#15 operativos integrados; CI final verde en frontend y backend.
- Migración V8 con metadatos, UUID impredecibles, SHA-256 y RLS forzado para archivos familiares.
- Originales y miniaturas re-encodeados sin EXIF/ubicación y cifrados con AES-256-GCM en volumen privado.
- Captura/selección JPEG/PNG, reducción móvil, visualización autenticada y eliminación completa desde la PWA.
- Cuota de 1 GiB serializada por familia, niveles 70/85/95/100 %, deduplicación familiar y bloqueo al límite.
- Pruebas PostgreSQL 18 para formato/tamaño, cifrado, IDOR/RLS, cuota y subidas competidoras.
- Bootstrap de clave persistente y permisos del volumen corregidos manteniendo Agenda sin privilegios.
- Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-5622571-20260719T033351Z`.
- E2E: alta 201, descarga cifrada, borrado, aislamiento 404, cuota, auditoría e indexación aprobados.
- PWA a 390×844: bundle `index-I8YWU03E.js`, receta y cuota visibles; sin desborde ni errores de consola.

## Siguiente bloque funcional

1. SSE con reconexión para reflejar cambios entre dispositivos abiertos.
2. Caché de lectura PWA y aviso claro sin conexión, manteniendo las escrituras solo en línea.
3. Web Push genérico en pantalla bloqueada y contenido privado únicamente tras autenticar.
4. Preferencias, horario silencioso, deduplicación y reintentos de notificaciones.
5. Pruebas de reconexión, privacidad, concurrencia entre dispositivos y experiencia offline.

## Entrada operativa

```powershell
.\tools\agenda-ops.cmd Resumen
```

Actualizar este archivo reemplazando el estado anterior; no acumular una bitácora histórica. Mantenerlo por debajo de 60 líneas.
