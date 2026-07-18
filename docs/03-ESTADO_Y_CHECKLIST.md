# Estado y checklist de continuidad

**Corte:** 2026-07-18 (America/Lima)  
**Rama:** `agent/historial-estados-botiquin`
**Commit desplegado:** `231a411f9edc193570697234919424ad09999c19`
**Versión del servidor:** `20260718T163608Z`
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

## Bloque completado — auditoría y reutilización privada

Integrado mediante el PR #5, desplegado y verificado en producción:

- [x] Migración `V5` preparada con catálogo privado de lugares, vínculo opcional desde eventos, palabras clave canónicas y `pg_trgm`; tablas con RLS forzado.
- [x] Lugar y dirección reutilizados se guardan por familia con última utilización y frecuencia; las sugerencias proceden exclusivamente de registros reales de esa familia.
- [x] Extractor asíncrono posterior al commit preparado con una lista conservadora de reglas; guarda como máximo dos palabras, origen, entidad y versión y nunca bloquea el alta.
- [x] Nombres, direcciones, diagnósticos y cantidades/dosis quedan fuera del extractor automático; prueba unitaria específica añadida.
- [x] Endpoint de autocompletado privado preparado para recuperar únicamente eventos y lugares existentes mediante historial, palabras clave y `pg_trgm`.
- [x] Historial de auditoría preparado para adultos autorizados con actor Papá/Mamá, operación, entidad y fecha/hora; altas y cambios de tareas también se auditan.
- [x] Tratamientos muestran por separado persona y responsable; el responsable alternativo puede elegirse en detalles progresivos.
- [x] Validación local: Vitest, `vue-tsc`, build Vite y Maven `verify` con Java 25 aprobados.
- [x] Testcontainers PostgreSQL 18 y CI con Java 25 aprobados: ejecución `29650965749` (frontend 19 s, backend 56 s).
- [x] PR #5 integrado en `main`, release `20260718T163608Z` desplegado y migración V5 aplicada una vez.
- [x] Producción: frontend, agenda, autenticación y PostgreSQL saludables; ambos endpoints Actuator respondieron `UP`.
- [x] E2E HTTPS: evento 201 en 35 ms, indexación asíncrona, sugerencia de registro real, lugar privado, auditoría coincidente para Papá/Mamá, responsable e aislamiento de familia ajena 404.
- [x] Revisión PWA autenticada a 390 × 844: filtros, responsable, historial y evento E2E visibles; sin desborde horizontal ni errores o advertencias de consola.

## Bloque preparado — historial, botiquín y horarios

Integrado mediante el PR #7; CI verde y pendiente de despliegue y E2E móvil:

- [x] Reprogramación separada de la posposición rápida, con ocurrencia original inmutable y nueva ocurrencia enlazada.
- [x] Cierre anticipado de tratamientos idempotente, con motivo opcional, actor visible y cancelación trazable de pendientes.
- [x] Historial de ocurrencias resueltas visible por persona, estado, actor y fecha.
- [x] Estados del botiquín calculados con precedencia explícita y vencimientos cercanos a 30 días en “Hoy”.
- [x] Varios horarios o intervalo de 1–168 horas y responsable alternativo opcional.
- [x] Pruebas de transiciones competidoras, IDOR, RLS, estados, horarios e intervalos añadidas.
- [x] Validación local rápida aprobada y CI `29657913288` aprobado (frontend 16 s, backend 1 min 4 s).
- [x] PR #7 integrado en `main` como `f7c27c9`.
- [ ] Desplegar V6 y repetir E2E/revisión móvil.

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
- PR de auditoría visible y sugerencias privadas por familia: <https://github.com/mherreraspe/agenda-familiar-app/pull/5>
- PR de historial, estados del botiquín y horarios: <https://github.com/mherreraspe/agenda-familiar-app/pull/7>
- CI del PR #7: ejecución `29657913288`, frontend y backend aprobados con Testcontainers PostgreSQL 18.
- Release `20260718T163608Z`: Flyway registra V5 `auditoria lugares y palabras clave` como aplicada correctamente.
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
- [x] Acciones completar, omitir, posponer, reprogramar y cerrar con historial.
- [x] Filtros por miembro y sección de vencimientos cercanos en “Hoy”.
- [ ] Recurrencia de eventos y tareas sin perder el historial anterior.
- [x] Alta rápida de cita/actividad con solo título y fecha/hora obligatorios; persona, tipo, lugar, dirección y notas serán opcionales/progresivos.
- [x] Después de guardar, procesar el registro de forma asíncrona y asociarle silenciosamente una o dos palabras clave canónicas mediante IA o reglas (por ejemplo, `pediatra` y `control`).
- [x] La extracción de palabras clave nunca bloquea el guardado, no produce texto visible ni modifica lo escrito por el usuario; si falla, el registro sigue siendo válido.
- [x] Guardar palabra, origen (`IA`, `REGLA` o `USUARIO`), versión del extractor y entidad asociada para poder recalcular o auditar el índice.
- [x] Excluir nombres de personas, direcciones, diagnósticos y cantidades/dosis de las palabras clave automáticas; esos datos tienen campos y controles de privacidad propios.
- [x] Usar las palabras clave solo para recuperar títulos/lugares reales de la misma familia en el autocompletado; no generar ni inventar una respuesta al usuario.
- [x] Catálogo privado de lugares por familia con nombre, dirección opcional, última utilización y frecuencia de uso.
- [x] Al escribir un lugar conocido, sugerir sus ubicaciones anteriores; seleccionar una sugerencia rellena la dirección, pero siempre puede omitirse o editarse.
- [x] El autocompletado nunca mezcla ni revela lugares, direcciones o palabras clave de otra familia.
- [x] Horarios/intervalos de tratamientos y responsable alternativo.
- [x] Estados calculados del botiquín: disponible, por vencer, vencido, agotado y descartado.
- [ ] Gestión de perfiles, adultos, dependientes y permisos desde la interfaz.
- [x] Convertir Papá/Mamá/Hijo en filtros claros con opción “Todos”; no representan cambio de usuario.
- [x] Mostrar “para quién”, responsable y “agregado/modificado por” con historial de auditoría visible para adultos autorizados.

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

Completar acciones e información operativa de tratamientos y botiquín:

1. Completar, omitir, posponer, reprogramar y cerrar con historial íntegro y actor visible.
2. Añadir sección de vencimientos cercanos y estados calculados del botiquín: disponible, por vencer, vencido, agotado y descartado.
3. Completar horarios/intervalos y responsable alternativo con edición progresiva.
4. Ampliar pruebas de transiciones, concurrencia, IDOR/RLS y cálculo de estados.
5. Publicar por PR, exigir CI verde, desplegar y repetir E2E móvil.

## Continuidad operativa

- Leer `docs/04-GUIA_OPERATIVA_CODEX.md` y comenzar con `tools\agenda-ops.cmd Estado`; no reconstruir manualmente Java, Maven, SSH o los E2E.
- El propietario suele operar remotamente desde el celular. Cuando GitHub requiera autorización web, iniciar el flujo oficial por código de dispositivo y entregar en el chat el enlace y el código temporal; no depender de una ventana local ni guardar el código, token o credencial.
- Repositorio local: `C:\Users\marco\Documents\codex\Proyecto\agenda-familiar-app`
- Clave SSH local protegida: `C:\Users\marco\Documents\codex\Proyecto\instanciaVM\ssh-key-2026-03-28.key`
- Servidor: `ubuntu@148.116.110.18`
- Raíz de despliegues: `/srv/agenda-familiar`
- Release actual enlazado desde `/srv/agenda-familiar/current`
- No reiniciar ni borrar volúmenes PostgreSQL para desplegar una versión.
- Antes de continuar: comprobar `git status -sb`, commit remoto, CI y salud pública.
