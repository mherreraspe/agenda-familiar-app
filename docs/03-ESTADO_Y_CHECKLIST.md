# Estado y checklist de continuidad

**Corte:** 2026-07-23 (America/Lima)
**Rama:** `main`
**Commit desplegado:** `aeb1f81fed396ecf189722ab7b94048dd1798fc6`
**Versión del servidor:** `20260723T233543Z`
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

## Bloque completado — historial, botiquín y horarios

Integrado mediante los PR #7–#9, desplegado y verificado en producción:

- [x] Reprogramación separada de la posposición rápida, con ocurrencia original inmutable y nueva ocurrencia enlazada.
- [x] Cierre anticipado de tratamientos idempotente, con motivo opcional, actor visible y cancelación trazable de pendientes.
- [x] Historial de ocurrencias resueltas visible por persona, estado, actor y fecha.
- [x] Estados del botiquín calculados con precedencia explícita y vencimientos cercanos a 30 días en “Hoy”.
- [x] Varios horarios o intervalo de 1–168 horas y responsable alternativo opcional.
- [x] Pruebas de transiciones competidoras, IDOR, RLS, estados, horarios e intervalos añadidas.
- [x] Validación local rápida aprobada y CI `29657913288` aprobado (frontend 16 s, backend 1 min 4 s).
- [x] PR #7 integrado en `main` como `f7c27c9`.
- [x] Acción `Desplegar` integrada con paquete inmutable, backup predeploy de ambas bases y manifiesto SHA-256.
- [x] Release `20260718T200358Z` activo; Flyway aplicó V6 una vez y los cuatro contenedores están saludables.
- [x] E2E HTTPS de regresión aprobado: aislamiento 404, evento, indexación, sugerencias, auditoría y responsables.
- [x] Revisión PWA autenticada a 390 × 844: historial con actor, posposición, reprogramación, cierre y vencimientos visibles; sin desborde ni errores/advertencias de consola.

## Bloque completado — recurrencia y administración familiar

Integrado mediante el PR #11, desplegado y verificado en producción:

- [x] Migración `V7` con series de tareas/eventos y acciones de agenda, ambas con RLS forzado.
- [x] Recurrencia diaria, semanal y mensual materializada; cada instancia conserva estado e historial propios.
- [x] Omitir y reprogramar tareas/eventos desde “Hoy”, “Revisar” y calendario; la sucesora queda enlazada a la original.
- [x] Acciones idempotentes y transiciones condicionales ante solicitudes competidoras.
- [x] Gestión de perfiles adultos/dependientes, cuentas vinculadas y permisos desde la PWA; cambios restringidos al administrador familiar.
- [x] Pruebas de recurrencia, historial, idempotencia, concurrencia, permisos, IDOR y RLS con PostgreSQL 18.
- [x] CI `29660227013` aprobado (frontend 13 s, backend 1 min 11 s) y PR #11 integrado como `e5beb0b`.
- [x] Release `20260718T205050Z` activo; Flyway aplicó V7 una vez y todos los servicios están saludables.
- [x] E2E HTTPS aprobado y PWA autenticada a 390 × 844 sin desborde ni errores/advertencias de consola.

## Bloque completado — fotografías privadas y cuota familiar

Integrado mediante los PR #13–#15, desplegado y verificado en producción:

- [x] Migración `V8` con archivos familiares, UUID impredecibles, SHA-256, cuota y RLS forzado.
- [x] Captura/selección JPEG/PNG y reducción en la PWA; re-encodeado elimina EXIF, ubicación y datos anexos.
- [x] Original y miniatura cifrados con AES-256-GCM, descarga autenticada y eliminación física completa.
- [x] Deduplicación limitada a la familia y una receta activa por tratamiento, ambas seguras ante concurrencia.
- [x] Cuota inicial de 1 GiB serializada con avisos 70/85/95 % y bloqueo de nuevas fotos al límite.
- [x] Pruebas PostgreSQL 18 para formatos inválidos, tamaño, cifrado, cuota, concurrencia, IDOR y RLS.
- [x] CI verde: `29667840551`, `29668127678` y `29669240800`; `main` final en `5622571`.
- [x] Release `20260719T033351Z` activa, V8 aplicada y backup `pre-5622571-20260719T033351Z`.
- [x] E2E HTTPS de archivos/cuota aprobado y PWA a 390 × 844 sin desborde ni errores de consola.

## Bloque completado — formulario adaptativo y rediseño estructural

Integrado mediante los PR #17–#19, desplegado y verificado en producción:

- [x] Formulario de evento extraído, abierto mediante `?crear=evento` y adaptado a móvil/escritorio con acciones persistentes, scroll y confirmación de descarte.
- [x] Validación temporal y conversiones centralizadas con la zona horaria familiar; sugerencias desde dos caracteres, con debounce y máximo tres resultados.
- [x] AppShell adaptable, Pinia, rutas reales y navegación activa derivada del router con `aria-current`.
- [x] Hoy, Agenda, Salud, Familia y Actividad presentan únicamente su dominio; Familia y Actividad están en el menú del avatar.
- [x] Filas compactas con una acción primaria y menú Más; carga, error, reintento e invalidación separados por dominio.
- [x] Objetos/Baúl permanece fuera de navegación hasta disponer de migración, API, RLS/IDOR, auditoría y UI funcional.
- [x] Vitest: 30 pruebas; Playwright/axe: 30 escenarios en 320×700, 390×844 y 1280×900; build y 6 pruebas backend aprobadas.
- [x] CI final `30007766644` verde y PR #19 integrado como `368a0de`.
- [x] Release `20260723T124905Z` activo; servicios saludables, V1–V8 validadas y E2E HTTPS aprobado.
- [x] Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-368a0de-20260723T124904Z`.
- [x] La revisión visual autenticada manual quedó no ejecutada por falta de navegador en la sesión; no se sustituye ni se declara como realizada.

## Bloque completado — fundamento visual y compactación de Salud

Integrado mediante el PR #23, desplegado y verificado en producción:

- [x] Tipografías locales Atkinson Hyperlegible Next y Bricolage Grotesque, tokens cromáticos y superficies planas coherentes con un tablero doméstico.
- [x] Iconos SVG propios en navegación, Añadir y avatar; identificador `FAMILIA_TEST` sustituido visualmente por `Mi familia`.
- [x] Filtro de perfiles consolidado en un selector; subsecciones de Salud presentadas como pestañas ligeras.
- [x] Terminología visible de tratamientos simplificada de `Ocurrencias` a `Tomas`.
- [x] Tratamientos compactados con indicación, responsables y receta bajo divulgación progresiva; cinco filas visibles en 390×844.
- [x] Sin overflow horizontal en 390×844 y 1280×900; altura de fila de tratamiento reducida de 160 px a 109 px.
- [x] Contraste de estados de atención elevado a 5,14:1; axe sin impactos críticos o serios en 320×700, 390×844 y 1280×900.
- [x] Verificación local: 32 pruebas frontend, build Vite, 6 pruebas backend y `npm audit --omit=dev` sin vulnerabilidades.
- [x] CI `30018761203` verde, PR #23 integrado como `3e326f9` y release `20260723T151623Z` saludable.
- [x] Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-3e326f9-20260723T151623Z`.
- [x] E2E HTTPS aprobado: aislamiento 404, receta cifrada, auditoría, responsables e indexación asíncrona.

## Bloque completado — estados operativos e historial de tomas

Integrado mediante el PR #26, desplegado y verificado en producción:

- [x] Hoy vacío muestra un único estado `Todo está al día`; no presenta contadores en cero, historial ni secciones vacías.
- [x] `Por resolver` solo se renderiza cuando existe contenido y las tareas vencidas no se duplican en otra sección.
- [x] Historial de tomas trasladado a una vista propia de Salud con 10 registros por bloque y retorno explícito a Tomas.
- [x] Cuota de recetas oculta mientras el uso sea inferior al 70 %.
- [x] Altas contextuales únicas: Evento en Agenda, Tratamiento en Tratamientos y Medicamento en Botiquín.
- [x] Menús móviles convertidos en hojas inferiores con fondo atenuado, cierre exterior y exclusión mutua.
- [x] Prototipo visual de Objetos con búsqueda, recientes y rutas físicas; disponible solo en desarrollo y ausente del build productivo.
- [x] Verificación local: 34 pruebas frontend, build Vite y 6 pruebas backend aprobadas.
- [x] CI `30024979468` verde con 39 escenarios Playwright/axe; cero impactos graves y cero overflow en las vistas revisadas.
- [x] PR #26 integrado como `5032b10`; release `20260723T165603Z` saludable y V1–V8 validadas.
- [x] Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-5032b10-20260723T165603Z`.
- [x] E2E HTTPS aprobado: aislamiento 404, receta cifrada, auditoría, responsables e indexación asíncrona.

## Bloque completado — sincronización familiar y lectura offline

Integrado mediante el PR #28, desplegado y verificado en producción:

- [x] SSE autenticado con Bearer en memoria y comprobación de membresía antes de registrar cada conexión familiar.
- [x] Eventos opacos limitados a Hoy, Agenda y Salud; ninguna tarea, evento, toma o dato médico viaja en el aviso.
- [x] Latidos cada 25 segundos, reconexión con backoff y resincronización completa de los dominios afectados tras un corte.
- [x] Deduplicación acotada por ID; las operaciones idempotentes generan el mismo ID de sincronización.
- [x] Invalidación selectiva de datos y actualización en segundo plano para tareas, eventos, tratamientos y tomas.
- [x] Caché de lectura en memoria separada por sujeto, eliminada al cerrar sesión y sin persistir datos privados en el service worker.
- [x] Aviso accesible sin conexión y bloqueo central de escrituras, además de altas deshabilitadas en los formularios visibles.
- [x] Pruebas de aislamiento familiar, 40 publicaciones concurrentes, backoff, duplicados, caché e invalidación entre vistas.
- [x] Verificación local: 40 pruebas frontend, build Vite y 10 pruebas backend aprobadas.
- [x] CI `30036392835` verde con PostgreSQL 18 y 45 escenarios Playwright/axe en tres viewports.
- [x] PR #28 integrado como `c27b006`; release `20260723T190511Z` saludable y V1–V8 validadas.
- [x] Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-c27b006-20260723T190510Z`.
- [x] E2E HTTPS aprobado: aislamiento 404, receta cifrada, auditoría, responsables e indexación asíncrona.

## Bloque completado — Objetos como dominio familiar real

Integrado mediante el PR #30, desplegado y verificado en producción:

- [x] Migración `V9` con objetos, ubicaciones jerárquicas e idempotencia; las tres tablas usan RLS forzado y claves familiares.
- [x] Búsqueda normalizada por nombre, categoría y cualquier segmento de la ruta física, limitada a 100 resultados.
- [x] Altas idempotentes y edición/movimiento con versión optimista para evitar sobrescrituras entre dispositivos.
- [x] Autorización e IDOR con respuesta 404, auditoría de crear/actualizar y títulos visibles solo para adultos autorizados.
- [x] Ruta real `/objetos`, alta contextual, edición y rutas legibles con `›`; el prototipo DEV fue eliminado.
- [x] Invalidación `OBJETOS` por SSE, recarga en segundo plano y lectura ya cargada sin permitir escrituras offline.
- [x] Pruebas PostgreSQL 18 de RLS, aislamiento, IDOR, idempotencia, concurrencia, búsqueda, auditoría y conflicto de versión.
- [x] Verificación local: 41 pruebas frontend, build Vite y 10 pruebas backend aprobadas.
- [x] CI `30042503806` verde con 51 escenarios Playwright/axe en 320×700, 390×844 y 1280×900.
- [x] PR #30 integrado como `7723d3e`; release `20260723T203410Z` saludable y V1–V9 validadas.
- [x] Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-7723d3e-20260723T203410Z`.
- [x] E2E HTTPS aprobado: servicios saludables, aislamiento 404, receta cifrada, auditoría, responsables e indexación asíncrona.

## Bloque completado — formularios modales y capas exclusivas

Integrado mediante el PR #32, desplegado y verificado en producción:

- [x] Altas de tarea, medicamento, tratamiento, perfil y objeto convertidas en diálogos modales nativos que bloquean el fondo.
- [x] Alta de tratamiento adaptable: contenido desplazable y pie fijo con Guardar/Cancelar visible en móvil y escritorio.
- [x] Receta privada presentada en una única capa modal y cerrable con Escape.
- [x] Menús Más exclusivos, con cierre al pulsar fuera o Escape y devolución del foco al activador.
- [x] Foco inicial dirigido al primer campo útil y restaurado al cerrar cada alta.
- [x] Verificación local: 42 pruebas frontend, build Vite y 10 pruebas backend aprobadas.
- [x] CI `30046463114` verde con 63 escenarios Playwright/axe en 320×700, 390×844 y 1280×900.
- [x] PR #32 integrado como `8acccba`; release `20260723T213222Z` saludable y V1–V9 validadas.
- [x] Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-8acccba-20260723T213222Z`.
- [x] E2E HTTPS aprobado: servicios saludables, aislamiento 404, receta cifrada, auditoría, responsables e indexación asíncrona.

## Bloque completado — administración global y alta de familias

Integrado mediante el PR #34, desplegado y verificado en producción:

- [x] Rol global `ADMINISTRADOR_PLATAFORMA` persistido en autenticación, incluido en el JWT firmado y comprobado nuevamente por Agenda.
- [x] Ruta `/admin` exclusiva con listado y alta real de familias; no depende de estado simulado en frontend.
- [x] Creación idempotente aislada por actor, zona horaria validada y auditoría de plataforma sin contenido médico.
- [x] Los usuarios familiares reciben 403 y no ven el acceso de administración en AppShell.
- [x] Migraciones V3 de autenticación y V10 de Agenda aplicadas sin modificar secretos ni volúmenes.
- [x] Verificación local: 43 pruebas frontend, build Vite y 10 pruebas backend aprobadas.
- [x] CI de PR `30053417092` y CI de `main` `30053507456` verdes con PostgreSQL 18; 66 escenarios Playwright/axe en tres viewports.
- [x] PR #34 integrado como `c155e00`; release `20260723T233543Z` saludable.
- [x] Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-c155e00-20260723T233543Z`.
- [x] E2E HTTPS aprobado: servicios saludables, aislamiento 404, receta cifrada, auditoría, responsables e indexación asíncrona.

## Bloque completado — miembros, invitaciones y recuperación de acceso

Integrado mediante el PR #36, desplegado y verificado en producción:

- [x] `/admin` lista miembros vinculados y permite crear adultos con rol familiar explícito; el primer miembro debe ser administrador familiar.
- [x] Invitaciones de un solo uso válidas durante 48 horas, revocables e idempotentes; el invitado define su contraseña en `/activar`.
- [x] Restablecimientos de un solo uso válidos durante 30 minutos y revocación de todas las sesiones refresh al cambiar la contraseña.
- [x] La base conserva solo SHA-256 del token; el token viaja en el fragmento del enlace y se retira inmediatamente de la dirección visible.
- [x] Auditoría segura de creación, consumo y revocación, sin tokens, contraseñas ni contenido médico.
- [x] Pruebas PostgreSQL 18 de autorización global, expiración, revocación, reuso y consumo concurrente único.
- [x] Verificación local: 43 pruebas frontend, build Vite y 10 pruebas backend aprobadas.
- [x] CI de PR `30056539025` y CI de `main` `30056643411` verdes; 72 escenarios Playwright/axe en tres viewports.
- [x] PR #36 integrado como `d73b8c6`; release `20260724T003756Z` saludable, autenticación V1–V4 y Agenda V1–V11 validadas.
- [x] Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-d73b8c6-20260724T003756Z`.
- [x] E2E HTTPS aprobado: servicios saludables, aislamiento 404, receta cifrada, auditoría, responsables e indexación asíncrona.

## Bloque completado — acceso administrativo y familia activa

Integrado mediante los PR #38–#40, desplegado y verificado en producción:

- [x] La cuenta administrativa global queda separada de `familia_test`; el rol firmado deriva login y renovación a `/admin`.
- [x] `AdminAcceso` recupera el acceso mediante el proyecto Compose desplegado, sin mostrar secretos ni tocar volúmenes.
- [x] `GET /familias` enumera exclusivamente membresías activas del sujeto autenticado, comprobadas bajo el contexto RLS de cada familia.
- [x] Hoy, Agenda, Salud, Objetos, archivos y SSE usan la familia seleccionada; se eliminó el UUID fijo de `familia_test` del frontend.
- [x] Una familia se selecciona automáticamente, varias pueden alternarse limpiando caché/datos/SSE y cero familias muestra un estado explícito.
- [x] La comprobación de membresía usa el índice único existente `(familia_id, usuario_publico_id)` y conserva 404 para IDOR.
- [x] Verificación local: 45 pruebas frontend, build Vite y 11 pruebas backend; 78 escenarios Playwright/axe en tres viewports.
- [x] CI de PR `30062673683` y CI de `main` `30062763663` verdes con PostgreSQL 18.
- [x] PR #40 integrado como `aeb1f81`; release `20260724T025339Z` saludable, V1–V11 validadas.
- [x] Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-aeb1f81-20260724T025339Z`.
- [x] E2E HTTPS aprobado: servicios saludables, aislamiento 404, receta cifrada, auditoría, responsables e indexación asíncrona.

## Bloque completado — Salud práctica y vigencia de envases

Integrado mediante el PR #42, desplegado y verificado en producción:

- [x] Alta guiada de tratamiento para una o varias personas, con nombre corto, medicamento/dosis/aplicación opcionales y horas explícitas o intervalo mutuamente excluyentes.
- [x] Una fotografía de receta cifrada se comparte dentro del grupo de tratamiento, con bloqueo de concurrencia para impedir duplicados.
- [x] Hoy contiene únicamente tareas y tomas del día; `Por resolver` es una vista separada con atrasos y avisos de Botiquín.
- [x] Cada envase de Botiquín conserva estado sin abrir/abierto, apertura, duración posterior, avisos, agotado o descartado de forma independiente.
- [x] Vigencia efectiva calculada con la fecha más temprana entre vencimiento impreso y límite tras abrir.
- [x] V12 añade grupos, datos de aplicación, estado de envase, idempotencia con RLS forzado e índices por familia para grupos, pendientes y vigencia.
- [x] Migración validada con propietario `NOBYPASSRLS`; el backfill transaccional restaura y comprueba `FORCE ROW LEVEL SECURITY`.
- [x] Verificación local: 45 pruebas frontend, build Vite y 11 pruebas backend; 87 escenarios Playwright/axe en tres viewports.
- [x] CI de PR `30068576773` y CI de `main` `30068668429` verdes con PostgreSQL 18.
- [x] PR #42 integrado como `cbadc5a`; release `20260724T051336Z` saludable, V1–V12 validadas.
- [x] Backup predeploy: `/srv/agenda-familiar/backups/predeploy/pre-cbadc5a-20260724T051336Z`.
- [x] E2E HTTPS aprobado: servicios UP, migración V12 aplicada, aislamiento 404, receta cifrada, auditoría, responsables e indexación asíncrona.

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
- [x] AppShell adaptable con barra inferior móvil, navegación lateral desde 840 px y ancho útil máximo de 1.200 px.
- [x] Rutas reales `/hoy`, `/agenda`, `/salud`, `/objetos`, `/ajustes/familia` y `/actividad`, con redirecciones desde rutas anteriores.
- [x] Filtro de persona compartido mediante Pinia; Familia y Actividad disponibles desde el avatar.
- [x] Pantalla “Hoy” limitada a tareas y tomas del día; `Por resolver`, Agenda y Salud muestran sus propios dominios.
- [x] Acción global Añadir para evento, tarea, objeto y tratamiento; Objetos dispone además de alta contextual.
- [x] Tarjetas repetitivas convertidas en filas compactas con una acción principal y menú Más.
- [x] Carga global sustituida por cargas e invalidaciones específicas de Hoy, Agenda, Salud, Objetos, Familia y Actividad.
- [x] Auditoría axe añadida para rutas, menús y estados, sin aceptar impactos críticos o serios.
- [x] Salud dividida en Tomas, Tratamientos, Botiquín y Recetas; solo una subsección se renderiza por vez y cada lista inicia con un máximo de cinco filas.
- [x] Menús Añadir/Familia y menús Más mutuamente exclusivos, con cierre exterior y Escape, activadores accesibles y enlaces nativos preservados.
- [x] Altas generales y receta como diálogos modales nativos: bloquean el fondo, encajan en el viewport y restauran el foco.
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
- PR de backup y despliegue reproducible: <https://github.com/mherreraspe/agenda-familiar-app/pull/8>
- PR del hotfix de invocación Bash: <https://github.com/mherreraspe/agenda-familiar-app/pull/9>; CI `29658934355` aprobado.
- PR del formulario de evento adaptativo: <https://github.com/mherreraspe/agenda-familiar-app/pull/17>.
- PR de AppShell y rutas por dominio: <https://github.com/mherreraspe/agenda-familiar-app/pull/18>.
- PR de listas, cargas selectivas y accesibilidad: <https://github.com/mherreraspe/agenda-familiar-app/pull/19>; CI `30007766644` aprobado.
- PR de Salud compacta y menús exclusivos: <https://github.com/mherreraspe/agenda-familiar-app/pull/21>; CI `30011966232` aprobado y release `20260723T134229Z` verificado.
- PR del fundamento visual y compactación de tratamientos: <https://github.com/mherreraspe/agenda-familiar-app/pull/23>; CI `30018761203` aprobado y release `20260723T151623Z` verificado.
- PR de estados operativos, historial y prototipo de Objetos: <https://github.com/mherreraspe/agenda-familiar-app/pull/26>; CI `30024979468` aprobado y release `20260723T165603Z` verificado.
- PR de sincronización familiar SSE y lectura offline: <https://github.com/mherreraspe/agenda-familiar-app/pull/28>; CI `30036392835` aprobado y release `20260723T190511Z` verificado.
- PR de Objetos como dominio familiar real: <https://github.com/mherreraspe/agenda-familiar-app/pull/30>; CI `30042503806` aprobado y release `20260723T203410Z` verificado.
- PR de formularios modales y capas exclusivas: <https://github.com/mherreraspe/agenda-familiar-app/pull/32>; CI `30046463114` aprobado y release `20260723T213222Z` verificado.
- PR de administración global y alta de familias: <https://github.com/mherreraspe/agenda-familiar-app/pull/34>; CI `30053417092` y `30053507456` aprobados, release `20260723T233543Z` verificado.
- PR de miembros, invitaciones y recuperación de acceso: <https://github.com/mherreraspe/agenda-familiar-app/pull/36>; CI `30056539025` y `30056643411` aprobados, release `20260724T003756Z` verificado.
- PR de cuenta administrativa separada: <https://github.com/mherreraspe/agenda-familiar-app/pull/38>; CI `30058962041` aprobado.
- PR de recuperación administrativa con Compose: <https://github.com/mherreraspe/agenda-familiar-app/pull/39>; CI `30059403460` aprobado.
- PR de resolución de familia activa: <https://github.com/mherreraspe/agenda-familiar-app/pull/40>; CI `30062673683` y `30062763663` aprobados, release `20260724T025339Z` verificado.
- PR de Salud práctica y vigencia de envases: <https://github.com/mherreraspe/agenda-familiar-app/pull/42>; CI `30068576773` y `30068668429` aprobados, release `20260724T051336Z` verificado.
- Release `20260718T163608Z`: Flyway registra V5 `auditoria lugares y palabras clave` como aplicada correctamente.
- Backups previos al despliegue: dumps custom de `agenda_familiar` y `autenticacion` con manifiesto SHA-256 en `/srv/agenda-familiar/backups/predeploy/pre-53fc275-20260718T144100Z/`.
- Release `20260718T200358Z`: V6 aplicada una vez; backup predeploy con manifiesto en `/srv/agenda-familiar/backups/predeploy/pre-5b9bdd4-20260718T200357Z/`.
- Credenciales: se entregaron al propietario en el chat y están configuradas únicamente en el `.env` protegido del servidor.

## Pendiente del MVP según `docs/`

### Dominio y experiencia

- [x] Ocurrencias de tratamientos: pendiente, tomada, omitida, pospuesta y cancelada.
- [x] Alta rápida de tratamiento con persona, nombre libre y horario; no exigir que el medicamento exista previamente en el botiquín.
- [x] Vincular un tratamiento con un medicamento del botiquín será opcional y podrá hacerse después.
- [x] “Cantidad indicada en la receta” será texto opcional; la aplicación solo conserva lo escrito y nunca calcula o recomienda dosis.
- [x] Fotografía de receta y detalles como indicación, fechas o responsable alternativo son campos opcionales/progresivos.
- [x] Bandeja “Revisar” para vencidos, tomas sin confirmar, tratamientos finalizados y medicamentos vencidos.
- [x] Acciones completar, omitir, posponer, reprogramar y cerrar con historial.
- [x] Filtros por miembro y sección de vencimientos cercanos en “Hoy”.
- [x] Recurrencia de eventos y tareas sin perder el historial anterior.
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
- [x] Gestión de perfiles, adultos, dependientes y permisos desde la interfaz.
- [x] Convertir Papá/Mamá/Hijo en filtros claros con opción “Todos”; no representan cambio de usuario.
- [x] Mostrar “para quién”, responsable y “agregado/modificado por” con historial de auditoría visible para adultos autorizados.

### Sincronización y avisos

- [x] SSE con reconexión para reflejar cambios entre dispositivos abiertos.
- [ ] Web Push con mensaje bloqueado genérico y contenido privado solo después de autenticar.
- [ ] Preferencias, horario silencioso, deduplicación, reintentos y escalamiento opcional.
- [x] Caché de lectura PWA y aviso claro cuando no hay conexión; sin escritura offline en el MVP.

### Fotografías y cuota

- [x] Captura/selección y reducción en la PWA.
- [x] Eliminar EXIF/ubicación, validar tipo y rechazar imágenes enormes o inválidas.
- [x] Almacenamiento privado cifrado, identificadores impredecibles y descarga autorizada.
- [x] Miniaturas, eliminación completa y deduplicación limitada a una familia.
- [x] Cuota inicial de 1 GiB con avisos 70/85/95 % y bloqueo de nuevas fotos al 100 %.

### Seguridad y administración

- [ ] Segundo factor para dispositivo nuevo y dispositivos confiables revocables.
- [ ] Pantalla de sesiones activas y revocación individual.
- [ ] Roles familiares completos y autorización por operación.
- [x] Administración de plataforma: rol global, acceso exclusivo, listado y alta idempotente de familias.
- [x] Administración de plataforma: miembros por invitación y recuperación de acceso de un solo uso.
- [ ] Administración de plataforma: suspensión, cuota y métricas sin contenido médico.
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

Añadir Web Push privado sin convertir la notificación en una fuga de datos:

1. Solicitar permiso solo tras una acción explícita y registrar cada suscripción para el usuario y dispositivo autenticados.
2. Mostrar en la pantalla bloqueada solo un aviso genérico, sin nombres, medicamentos ni contenido familiar; el detalle se consulta dentro de la PWA autenticada.
3. Permitir revocar dispositivos, depurar endpoints inválidos y evitar entregas duplicadas.
4. Mantener Hoy como bandeja operativa y usar Push únicamente como aviso de cambios relevantes.
5. Probar aislamiento familiar, permiso denegado, renovación de suscripción, dispositivo revocado y concurrencia.

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
