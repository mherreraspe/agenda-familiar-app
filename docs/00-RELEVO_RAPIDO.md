# Relevo rápido

Leer este archivo al iniciar una sesión. Consultar el checklist o la guía operativa solo cuando la tarea lo requiera.

## Estado actual

- Rama remota: `main`.
- Commit desplegado: `7a10f2e9ebab0a34bb1075662052abcc8521ba0a`.
- Release activo: `20260724T185658Z`.
- Producción: frontend, autenticación, agenda y PostgreSQL saludables.
- Migraciones: autenticación V1–V4 y Agenda V1–V13 aplicadas correctamente.
- Último bloque: adopción de la marca `Obu Familia` y nuevo icono PWA basado en el búho original.
- E2E HTTPS aprobado; 96 escenarios Playwright/axe completados en 320×700, 390×844 y 1280×900.

## Último bloque completado

- PR #50: la experiencia familiar visible, el título, el manifiesto y el Web Push genérico usan `Obu Familia`.
- El icono conserva la forma y los colores del búho original y añade un calendario inequívoco bajo sus patas.
- El manifiesto publica PNG normales y `maskable` en 192/512 px; también incluye favicon y `apple-touch-icon`.
- La variante `maskable` mantiene orejas, patas y calendario dentro del área segura de recorte de Android.
- `OBU System` se conserva únicamente como identidad de la administración global de plataforma.
- No hubo cambios de modelo de datos ni nuevas migraciones; Agenda V1–V13 sigue aplicada.
- La bandeja interna continúa activa; Web Push permanece deshabilitado hasta autorizar claves VAPID.
- Verificación local: 50 pruebas frontend, build Vite y 11 pruebas backend aprobadas.
- Playwright/axe: 96 escenarios verdes; revisión de marca aprobada en los tres viewports.
- CI de PR `30118049481` y CI de `main` `30118213368`: frontend y backend verdes.
- Release `20260724T185658Z` saludable; manifiesto `Obu Familia` y E2E HTTPS real validados.
- Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-7a10f2e-20260724T185657Z`.

## Siguiente bloque funcional

1. Autorizar y configurar claves VAPID mediante el flujo protegido, sin registrarlas en Git.
2. Probar entrega Web Push real en iPhone instalado como PWA y en un segundo dispositivo.
3. Verificar permiso denegado, renovación de suscripción, endpoint expirado y revocación por dispositivo.
4. Decidir si se añaden avisos de cambios familiares importantes o escalamiento opcional.
5. Continuar con sesiones activas y revocación individual, sin mezclar este alcance con Push.

## Entrada operativa

```powershell
.\tools\agenda-ops.cmd Resumen
```

Actualizar este archivo reemplazando el estado anterior; no acumular una bitácora histórica. Mantenerlo por debajo de 60 líneas.
