# Estado y checklist de continuidad

**Corte:** 2026-07-18 (America/Lima)  
**Rama:** `main`  
**Commit desplegado:** `3ddb3ae46ef6f25122c513ff0c63dcbe8123e33e`  
**Versión del servidor:** `20260718T073448Z`  
**Producción:** <https://www.obusystem.com>

Este documento es el punto de relevo para continuar el proyecto en otro chat. No contiene contraseñas ni secretos.

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
- Credenciales: se entregaron al propietario en el chat y están configuradas únicamente en el `.env` protegido del servidor.

## Pendiente del MVP según `docs/`

### Dominio y experiencia

- [ ] Ocurrencias de tratamientos: pendiente, tomada, omitida, pospuesta y cancelada.
- [ ] Alta rápida de tratamiento con persona, nombre libre y horario; no exigir que el medicamento exista previamente en el botiquín.
- [ ] Vincular un tratamiento con un medicamento del botiquín será opcional y podrá hacerse después.
- [ ] “Cantidad indicada en la receta” será texto opcional; la aplicación solo conserva lo escrito y nunca calcula o recomienda dosis.
- [ ] Fotografía de receta y detalles como indicación, fechas o responsable alternativo serán campos opcionales/progresivos.
- [ ] Bandeja “Revisar” para vencidos, tomas sin confirmar, tratamientos finalizados y medicamentos vencidos.
- [ ] Acciones completar, omitir, posponer, reprogramar y cerrar con historial.
- [ ] Filtros por miembro y sección de vencimientos cercanos en “Hoy”.
- [ ] Recurrencia de eventos y tareas sin perder el historial anterior.
- [ ] Alta rápida de cita/actividad con solo título y fecha/hora obligatorios; persona, tipo, lugar, dirección y notas serán opcionales/progresivos.
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
- [ ] Convertir Papá/Mamá/Hijo en filtros claros con opción “Todos”; no representan cambio de usuario.
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

Implementar primero ocurrencias de tratamiento y bandeja “Revisar”:

1. Crear migración `V4` con horarios, ocurrencias y elementos de revisión, todos con RLS.
2. Materializar ocurrencias próximas de forma idempotente.
3. Exponer consulta y acciones tomada/omitida/pospuesta/cancelada.
4. Incluir atrasados y vencimientos en “Hoy” y “Revisar”.
5. Añadir pruebas de aislamiento, transición de estados e idempotencia.
6. Publicar por PR, exigir CI verde, desplegar y repetir E2E.

## Continuidad operativa

- Repositorio local: `C:\Users\marco\Documents\codex\Proyecto\agenda-familiar-app`
- Clave SSH local protegida: `C:\Users\marco\Documents\codex\Proyecto\instanciaVM\ssh-key-2026-03-28.key`
- Servidor: `ubuntu@148.116.110.18`
- Raíz de despliegues: `/srv/agenda-familiar`
- Release actual enlazado desde `/srv/agenda-familiar/current`
- No reiniciar ni borrar volúmenes PostgreSQL para desplegar una versión.
- Antes de continuar: comprobar `git status -sb`, commit remoto, CI y salud pública.
