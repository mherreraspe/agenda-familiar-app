# Estado y checklist de continuidad

**Corte:** 2026-07-18 (America/Lima)  
**Rama:** `main`  
**Commit desplegado:** `2daaf08c2849440fdb57b89df9d0a9a68735cf16`
**Versión del servidor:** `20260718T145740Z`
**Producción:** <https://www.obusystem.com>

Este documento es el punto de relevo para continuar el proyecto en otro chat. No contiene contraseñas ni secretos.

## Bloque completado — ocurrencias y bandeja Revisar

Integrado mediante los PR #3 y #4, desplegado y verificado en producción:

- [x] Migración `V4` preparada con horarios, ocurrencias, acciones idempotentes y elementos de revisión, todos con RLS forzado.
- [x] Estados de ocurrencia implementados: pendiente, tomada, omitida, pospuesta y cancelada.
- [x] Materialización móvil de 30 días anteriores y 7 posteriores con restricción única para evitar duplicados.
- [x] Bandeja `Revisar` preparada para tomas sin confirmar, tratamientos finalizados y medicamentos vencidos.
- [x] Alta rápida de tratamiento preparada con persona, nombre libre y horario; botiquín, cantidad de receta, indicación, frecuencia y fechas opcionales.
- [x] Citas flexibilizadas para exigir únicamente título e inicio; persona, tipo, lugar, dirección, notas y fin son opcionales.
- [x] Perfiles convertidos en filtros con opción `Todos` en la interfaz.
- [x] Pruebas añadidas para RLS, materialización idempotente, transiciones, posposición y cierre desde `Revisar`.
- [x] Validación local: Vitest, `vue-tsc`, build Vite y compilación de código/pruebas Java 21 aprobadas.
- [x] CI con Java 25 y Testcontainers PostgreSQL 18 aprobado en el PR #3: ejecución `29648360959` (frontend 13 s, backend 48 s).
- [x] El primer despliegue detectó un backfill incompatible con `FORCE RLS`; Flyway revirtió la transacción sin pérdida de datos y se restauró inmediatamente el release anterior.
- [x] Hotfix del backfill por familia y prueba de migración con propietario `NOBYPASSRLS` integrados mediante el PR #4; CI `29648871628` aprobado (frontend 18 s, backend 45 s).
- [x] PR integrado, despliegue y E2E móvil de este bloque aprobados.
- [x] Producción: Flyway validó 4 migraciones, aplicó V4 una vez y Spring arrancó en 7,92 s; autenticación, agenda y frontend saludables.
- [x] E2E HTTPS: login de Papá y Mamá 200, 3 perfiles, aislamiento de familia ajena 404, cita mínima 201, tratamiento rápido 201 y acción de ocurrencia idempotente verificada.
- [x] Revisión PWA autenticada: filtros `Todos`/Hijo/Mamá/Papá, ocurrencias, bandeja `Revisar` y navegación móvil visibles; consola sin errores ni advertencias.

## Trabajo en curso — auditoría y reutilización privada

Rama local `feat/auditoria-lugares-palabras`, aún no desplegada:

- [x] Migración `V5` preparada con catálogo privado de lugares, vínculo opcional desde eventos, palabras clave canónicas y `pg_trgm`; tablas con RLS forzado.
- [x] Lugar y dirección reutilizados se guardan por familia con última utilización y frecuencia; las sugerencias proceden exclusivamente de registros reales de esa familia.
- [x] Extractor asíncrono posterior al commit preparado con una lista conservadora de reglas; guarda como máximo dos palabras, origen, entidad y versión y nunca bloquea el alta.
- [x] Nombres, direcciones, diagnósticos y cantidades/dosis quedan fuera del extractor automático; prueba unitaria específica añadida.
- [x] Endpoint de autocompletado privado preparado para recuperar únicamente eventos y lugares existentes mediante historial, palabras clave y `pg_trgm`.
- [x] Historial de auditoría preparado para adultos autorizados con actor Papá/Mamá, operación, entidad y fecha/hora; altas y cambios de tareas también se auditan.
- [x] Tratamientos muestran por separado persona y responsable; el responsable alternativo puede elegirse en detalles progresivos.
- [x] Validación local: Vitest, `vue-tsc`, build Vite y compilación/pruebas unitarias Java 21 aprobadas.
- [ ] Testcontainers PostgreSQL 18, CI, PR, despliegue y E2E móvil de este bloque aprobados.

No marcar los requisitos equivalentes de la lista general como terminados hasta completar CI, despliegue y E2E.

## Completado y verificado

- [x] Repositorio local vinculado con `mherreraspe/agenda-familiar-app` y rama principal sincronizada.
- [x] CI de frontend y backend en GitHub Actions.
- [x] Vue 3, TypeScript, Vite y PWA instalable.
- [x] Spring Boot 4.1, Java 25 en CI y PostgreSQL 18 con Flyway.
- [x] Despliegue Docker Compose v2 detrás de Nginx y TLS válido.
- [x] Dominio canónico `www.obusystem.com`; HTTP y dominio raíz redirigen a HTTPS canónico.
- [x] Familia `familia_test` con tres perfiles: Papá, Mamá e Hijo.
- [x] Cuentas adultas de prueba para Papa y Mamá; Hijo es dependiente sin credenciales.
- [x] Inicio de sesión Argon2id y access token en memoria, nunca en `localStorage`.
- [x] Refresh token rotatorio guardado únicamente como hash.
- [x] Cookies `HttpOnly`, `Secure`, `SameSite=Lax` y defensa CSRF de doble envío.
- [x] Renovación automática de sesión, usuario actual y cierre/revocación de sesión.
- [x] RLS PostgreSQL y comprobación de membresía para aislar datos por familia.
- [x] Pantalla móvil “Hoy” con perfiles, pendientes, calendario, tratamientos y botiquín.
- [x] Alta desde la interfaz de tareas, eventos, medicamentos y tratamientos.
- [x] Completar tareas desde “Hoy”.
- [x] Catálogo inicial de medicamento, tratamiento y evento para `familia_test`.
- [x] Rate limiting restringido a inicio y renovación de sesión.
- [x] Pruebas frontend, build, Maven y Testcontainers PostgreSQL 18 aprobadas.
- [x] Revisión en navegador de 390 × 844 sin desborde de formularios ni errores de consola.
- [x] E2E en producción: login 200, CSRF inválido 403, renovar 200, reutilizar refresh 401, cerrar 204, renovar tras cierre 401 y HTTPS 200.

## Evidencia reciente

- PR de sesiones y formularios: <https://github.com/mherreraspe/agenda-familiar-app/pull/1>
- PR de sincronización idempotente de cuentas de prueba: <https://github.com/mherreraspe/agenda-familiar-app/pull/2>
- E2E confirmó `PERFILES=3`, `MEDICAMENTOS=1`, `TRATAMIENTOS=1` y `EVENTOS=1`.
- PR de ocurrencias y bandeja Revisar: <https://github.com/mherreraspe/agenda-familiar-app/pull/3>
- PR del hotfix de migración bajo RLS forzado: <https://github.com/mherreraspe/agenda-familiar-app/pull/4>
- Backups previos al despliegue: dumps custom de `agenda_familiar` y `autenticacion` con manifiesto SHA-256 en `/srv/agenda-familiar/backups/predeploy/pre-53fc275-20260718T144100Z/`.
- Credenciales: se entregaron al propietario en el chat y están configuradas únicamente en el `.env` protegido del servidor.

## Pendiente del MVP según `docs/`

### Dominio y experiencia

- [x] Ocurrencias de tratamientos: pendiente, tomada, omitida, pospuesta y cancelada.
- [x] Alta rápida de tratamiento con persona, nombre libre y horario; no exigir que el medicamento exista previamente en el botiquín.
- [x] Vincular un tratamiento con un medicamento del botiquín será opcional y podrá hacerse después.
- [x] “Cantidad indicada en la receta” será texto opcional; la aplicación solo conserva lo escrito y nunca calcula o recomienda dosis.
- [ ] Fotografía de receta y detalles como indicación, fechas o responsable alternativo serán campos opcionales/progresivos.
- [x] Bandeja “Revisar” para vencidos, tomas sin confirmar, tratamientos finalizados y medicamentos vencidos.
- [ ] Acciones completar, omitir, posponer, reprogramar y cerrar con historial.
- [ ] Filtros por miembro y sección de vencimientos cercanos en “Hoy”.
- [ ] Recurrencia de eventos y tareas sin perder el historial anterior.
- [x] Alta rápida de cita/actividad con solo título y fecha/hora obligatorios; persona, tipo, lugar, dirección y notas serán opcionales/progresivos.
- [ ] Después de guardar, procesar el registro de forma asíncrona y asociarle silenciosamente una o dos palabras clave canónicas mediante IA (por ejemplo, `pediatra` y `control`).
- [ ] La extracción de palabras clave nunca bloqueará el guardado, no producirá texto visible ni modificará lo escrito por el usuario; si falla, el registro seguirá siendo válido.
- [ ] Guardar palabra, origen (`IA`, `REGLA` o `USUARIO`), versión del extractor y entidad asociada para poder recalcular o auditar el índice.
- [ ] Excluir nombres de personas, direcciones, diagnósticos y cantidades/dosis de las palabras clave automáticas; esos datos tienen campos y controles de privacidad propios.
- [ ] Usar las palabras clave solo para recuperar títulos/lugares reales de la misma familia en el autocompletado; no generar ni inventar una respuesta al usuario.
- [ ] Catálogo privado de lugares por familia con nombre, dirección opcional, última utilización y frecuencia de uso.
- [ ] Al escribir un lugar conocido, sugerir sus ubicaciones anteriores; seleccionar una sugerencia rellenará la dirección, pero siempre podrá omitirse o editarse.
- [ ] El autocompletado nunca mezclará ni revelará lugares, direcciones o palabras clave de otra familia.
- [ ] Horarios/intervalos de tratamientos y responsable alternativo.
- [ ] Estados calculados del botiquín: disponible, por vencer, vencido, agotado y descartado.
- [ ] Gestión de perfiles, adultos, dependientes y permisos desde la interfaz.
- [x] Convertir Papá/Mamá/Hijo en filtros claros con opción “Todos”; no representan cambio de usuario.
- [ ] Mostrar “para quién”, responsable y “agregado/modificado por” con historial de auditoría visible para adultos autorizados.

### Sincronización y avisos

- [ ] SSE con reconexión para reflejar cambios entre dispositivos abiertos.
- [ ] Web Push con mensaje bloqueado genérico y contenido privado solo después de autenticar.
- [ ] Preferencias, horario silencioso, deduplicación, reintentos y escalamiento opcional.
- [ ] Caché de lectura PWA y aviso claro cuando no hay conexión; sin escritura offline en el MVP.

### Fotografías y cuota

- [ ] Captura/selección y reducción en la PWA.
- [ ] Eliminar EXIF/ubicación, validar tipo y rechazar imágenes enormes o inválidas.
- [ ] Almacenamiento privado cifrado, identificadores impredecibles y descarga autorizada.
- [ ] Miniaturas, eliminación completa y deduplicación limitada a una familia.
- [ ] Cuota inicial de 1 GiB con avisos 70/85/95 % y bloqueo de nuevas fotos al 100 %.

### Seguridad y administración

- [ ] Segundo factor para dispositivo nuevo y dispositivos confiables revocables.
- [ ] Pantalla de sesiones activas y revocación individual.
- [ ] Roles familiares completos y autorización por operación.
- [ ] Administración de plataforma: familias, suspensión, cuota y métricas sin contenido médico.
- [ ] Cambiar contraseña y retirar las credenciales provisionales antes de datos reales.
- [ ] Migrar JWT HS256 compartido a firma asimétrica como indica el diseño técnico.

### Operación y aceptación

- [ ] Backup diario `pg_dump` custom con 14 días de retención.
- [ ] Backup de archivos por hash, manifiesto SHA-256 y cifrado externo.
- [ ] Restauración automatizada de base, archivos y configuración, y prueba documentada.
- [ ] Logs JSON con rotación y alertas de disco, backup, push y cuota.
- [ ] OpenAPI validado en CI, ArchUnit y Playwright móvil automatizado.
- [ ] Pruebas de IDOR/RLS, CSRF, concurrencia, archivos maliciosos, cuota y restore completo.
- [ ] Piloto con dos dispositivos y prueba de tiempos de alta (<30 s cita, <45 s medicamento).
- [ ] Auditoría final requisito por requisito; declarar solo “sin fallos conocidos dentro de lo probado”, nunca ausencia absoluta de bugs.

## Próximo bloque recomendado

Completar trazabilidad y reutilización privada de datos familiares:

1. Mostrar “para quién”, responsable, agregado/modificado/resuelto por y fecha/hora con historial visible para adultos autorizados.
2. Guardar catálogo privado de lugares y direcciones usados por la familia y sugerir únicamente valores reales de esa familia.
3. Añadir palabras clave canónicas asíncronas con reglas conservadoras, origen, versión y aislamiento RLS; el guardado nunca dependerá del extractor.
4. Completar pruebas de IDOR/RLS, auditoría, fallo del extractor y autocompletado entre familias.
5. Publicar por PR, exigir CI verde, desplegar y repetir E2E móvil.

## Continuidad operativa

- El propietario suele operar remotamente desde el celular. Cuando GitHub requiera autorización web, iniciar el flujo oficial por código de dispositivo y entregar en el chat el enlace y el código temporal; no depender de una ventana local ni guardar el código, token o credencial.
- Repositorio local: `C:\Users\marco\Documents\codex\Proyecto\agenda-familiar-app`
- Clave SSH local protegida: `C:\Users\marco\Documents\codex\Proyecto\instanciaVM\ssh-key-2026-03-28.key`
- Servidor: `ubuntu@148.116.110.18`
- Raíz de despliegues: `/srv/agenda-familiar`
- Release actual enlazado desde `/srv/agenda-familiar/current`
- No reiniciar ni borrar volúmenes PostgreSQL para desplegar una versión.
- Antes de continuar: comprobar `git status -sb`, commit remoto, CI y salud pública.
