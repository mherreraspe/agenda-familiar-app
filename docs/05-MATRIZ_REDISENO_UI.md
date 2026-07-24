# Matriz de trazabilidad del rediseño UI

Esta matriz es el control obligatorio para reorganizar la interfaz sin ocultar capacidades existentes. Debe actualizarse en cada PR que mueva una función o cambie su interacción.

| Función actual | Nueva ubicación | Acción primaria | Acción secundaria | API usada | Prueba prevista |
|---|---|---|---|---|---|
| Completar tarea | Hoy / Agenda | Checkbox | Más | `actuarAgenda(..., COMPLETAR)` | Vitest / E2E |
| Omitir tarea | Agenda / Más | — | Menú Más | `actuarAgenda(..., OMITIR)` | Vitest |
| Reprogramar tarea | Agenda / Más | — | Formulario | `actuarAgenda(..., REPROGRAMAR)` | E2E |
| Crear tarea | Agenda / Añadir / Tarea | Guardar | — | `crearTarea` | Vitest / E2E |
| Crear evento, cita o salida | Agenda / Añadir / Evento, cita o salida | Guardar | Tipo o categoría visible | `crearEvento` | Vitest / E2E |
| Distinguir tarea de evento | Agenda / Tarjeta | Insignia textual y símbolo; `Hecho` solo en tarea | Color de apoyo | `consultarHoy`, `consultarCatalogo` | E2E / axe |
| Omitir evento | Agenda / Más | — | Menú Más | `actuarAgenda(..., OMITIR)` | Vitest |
| Reprogramar evento | Agenda / Más | — | Formulario | `actuarAgenda(..., REPROGRAMAR)` | E2E |
| Consultar avisos privados | Cabecera / Campana / Pulso familiar | Abrir aviso | Marcar todo leído | `consultarNotificaciones`, `marcarNotificacionLeida` | Vitest / IT / E2E / axe |
| Configurar avisos | Pulso familiar / Configurar avisos | Guardar preferencias | Activar o revocar dispositivo | `guardarPreferenciasNotificacion`, `registrarPush`, `revocarPush` | Vitest / IT / E2E |
| Programar recordatorio de tarea | Agenda / Añadir / Tarea | Avisar al vencer | Desactivar aviso | `crearTarea` | Vitest / IT / E2E |
| Programar avisos de evento | Agenda / Añadir / Evento, cita o salida / Más opciones | 24 h antes | 1 h antes | `crearEvento` | Vitest / IT / E2E |
| Marcar toma | Hoy / Salud | Tomada | — | `cambiarEstadoOcurrencia` | E2E |
| Posponer toma | Salud / Más | — | Menú Más | `cambiarEstadoOcurrencia` | E2E |
| Omitir toma | Salud / Más | — | Menú Más | `cambiarEstadoOcurrencia` | E2E |
| Reprogramar toma | Salud / Más | — | Formulario | `cambiarEstadoOcurrencia` | E2E |
| Cerrar revisión | Hoy / Por resolver | Cerrar | — | `cerrarElementoRevision` | E2E |
| Crear tratamiento para una o varias personas | Salud / Tratamientos | Guardar | Receta opcional | `crearTratamiento` (grupo idempotente) | IT / E2E |
| Editar tratamiento activo | Salud / Tratamientos / Más | Guardar cambios | Finalizar y crear otro si cambian las personas | `actualizarTratamiento` (grupo idempotente) | IT / E2E |
| Cerrar tratamiento | Salud / Detalle | Finalizar | — | `cerrarTratamiento` | E2E |
| Ver receta | Salud / Detalle | Ver | — | `descargarReceta` | E2E |
| Subir receta | Salud / Detalle | Añadir | — | `subirReceta` | E2E |
| Eliminar receta | Salud / Detalle | — | Eliminar | `eliminarReceta` | E2E |
| Ver cuota | Salud / Recetas | — | — | `consultarCuota` | Vitest |
| Ver botiquín | Salud / Botiquín | Revisar envase | Agotado / descartar | `consultarCatalogo` | IT / E2E |
| Crear medicamento | Salud / Botiquín | Guardar | — | `crearMedicamento` | E2E |
| Marcar envase abierto | Salud / Botiquín | Marcar abierto | Avisos configurables | `actualizarEnvase` | IT / E2E |
| Ver vigencia efectiva | Salud / Botiquín / Por resolver | Revisar | — | `consultarCatalogo` | IT / E2E |
| Buscar objeto | Objetos / Buscador | Escribir consulta | Explorar lugares | `consultarObjetos` | IT / E2E |
| Crear objeto | Objetos / Añadir | Guardar | — | `crearObjeto` | IT / E2E |
| Editar o mover objeto | Objetos / Fila | Editar | — | `actualizarObjeto` | IT / E2E |
| Administrar perfiles | Ajustes / Familia | Editar | — | `consultarConfiguracionFamilia`, `crearPerfil`, `actualizarPerfil` | E2E |
| Ver auditoría | Ajustes / Actividad | Abrir | — | `consultarAuditoria` | E2E |
| Filtrar por persona | Hoy / Agenda / Salud | Elegir persona | — | Estado compartido | Vitest / E2E |
| Cerrar sesión | Menú de avatar | Cerrar sesión | — | `cerrarSesion` | E2E |
| Cambiar de familia | Menú de avatar | Elegir familia | Estado sin familia | `consultarFamiliasUsuario`, `establecerFamiliaActiva` | Vitest / IT / E2E |
| Reflejar cambios familiares | Hoy / Agenda / Salud / Objetos | Automática | Aviso de reconexión | SSE `/familias/{id}/eventos` | Vitest / IT / E2E |
| Consultar durante un corte | Hoy / Agenda / Salud / Objetos | Lectura ya cargada | Aviso sin conexión | Caché en memoria por sujeto | Vitest / E2E |
| Administrar familias | Administración / Familias | Crear familia | Error y reintento | `consultarFamiliasPlataforma`, `crearFamiliaPlataforma` | IT / E2E |
| Invitar miembro | Administración / Familia | Generar invitación | Revocar o reenviar | `crearMiembroPlataforma`, `crearInvitacionPlataforma` | IT / E2E |
| Gestionar membresía | Administración / Familia / Miembro | Guardar rol o acceso | Dar de baja / reactivar | `actualizarMiembroPlataforma` | IT / E2E |
| Restablecer acceso | Administración / Miembro | Generar enlace | Generar uno nuevo | `crearRestablecimientoPlataforma`, `consumirEnlaceAcceso` | IT / E2E |

## Estado por PR

- PR 1 — Formulario de evento: extraído a un componente de dominio, abierto con `?crear=evento`, validado en la zona horaria familiar y cubierto en 320×700, 390×844 y 1280×900.
- PR 2A — AppShell y rutas: Pinia incorporado para el filtro compartido, destinos reales `/hoy`, `/agenda` y `/salud`, Familia/Actividad en el menú secundario y redirecciones compatibles desde las rutas anteriores. Objetos permanece oculto hasta disponer de dominio real.
- PR 2B — Integrado mediante PR #19 y desplegado en `20260723T124905Z`: listas compactas, una acción principal por fila, menú Más, carga e invalidación por dominio, estados independientes y auditoría axe.
- Ajuste posterior — PR #21 desplegado en `20260723T134229Z`: Salud usa cuatro subsecciones exclusivas, limita las listas inicialmente a cinco filas y evita la apertura simultánea de Añadir/Familia.
- Fundamento visual — PR #23 desplegado en `20260723T151623Z`: tipografías legibles, tokens cromáticos, iconos SVG, encabezado y filtro compactos, terminología `Tomas` y detalle progresivo de tratamientos; axe verde en los tres viewports.
- Interacción operativa — PR #26 desplegado en `20260723T165603Z`: Hoy oculta secciones vacías, el historial de tomas usa una vista paginada, las altas son contextuales y la cuota normal no compite con el contenido.
- Sincronización familiar — PR #28 desplegado en `20260723T190511Z`: SSE autenticado por familia, backoff, deduplicación, invalidación de Hoy/Agenda/Salud y lectura ya cargada sin escrituras offline; 45 escenarios Playwright/axe verdes.
- Objetos — PR #30 desplegado en `20260723T203410Z`: V9, API, búsqueda, alta/edición, RLS/IDOR, idempotencia, concurrencia, auditoría y SSE; el prototipo DEV fue retirado.
- Capas exclusivas — PR #32 desplegado en `20260723T213222Z`: altas generales y receta como modales nativos, tratamiento adaptable con acciones fijas, foco restaurado y un solo menú Más abierto; 63 escenarios Playwright/axe verdes.
- Administración global — PR #34 desplegado en `20260723T233543Z`: rol global, `/admin`, listado y alta real e idempotente de familias, autorización backend y auditoría; 66 escenarios Playwright/axe verdes.
- Acceso administrado — PR #36 desplegado en `20260724T003756Z`: miembros adultos, invitaciones de 48 h, restablecimientos de 30 min, tokens de un solo uso, sesiones revocadas y capas exclusivas; 72 escenarios Playwright/axe verdes.
- Separación administrativa — PR #38: la cuenta global deriva a `/admin` y queda fuera de `familia_test`; PR #39 corrige la recuperación operativa con el proyecto Compose real.
- Familia activa — PR #40 desplegado en `20260724T025339Z`: membresías resueltas desde el JWT bajo RLS, sin UUID fijo; selección automática o cambio en el menú, con limpieza de datos y SSE; 78 escenarios Playwright/axe completados.
- Salud práctica — PR #42 desplegado en `20260724T051336Z`: tratamiento grupal con horarios guiados y receta compartida, Hoy/Por resolver separados, envases independientes y vigencia por vencimiento o apertura; V12/RLS/índices y 87 escenarios Playwright/axe verdes.
- Miembros y tratamientos — PR #44 desplegado en `20260724T111747Z`: familia administrada visible, cambio de rol y baja/reactivación con protección del último administrador; edición de tratamientos activos conservando historial y sustituyendo solo pendientes futuros; CI PostgreSQL 18 y E2E HTTPS verdes.
- Agenda legible — PR #46 desplegado en `20260724T123957Z`: altas de tarea y evento/cita/salida, insignias de tipo sin depender solo del color, categoría visible y término `recordatorio` retirado hasta disponer de notificación; 93 escenarios Playwright/axe verdes.
- Notificaciones privadas — PR #48 desplegado en `20260724T141134Z`: campana y bandeja `Pulso familiar`, preferencias por usuario, horario silencioso, recordatorios de tareas/eventos y motor deduplicado para Salud y Botiquín; V13/FORCE RLS, 96 escenarios Playwright/axe y revisión real en los tres viewports. Push queda preparado pero deshabilitado hasta autorizar claves VAPID.
- PR 4 completado mediante #30. El siguiente bloque es la activación y prueba real de Web Push en dos dispositivos, sin ampliar el contenido visible en la pantalla bloqueada.

Una función solo puede cambiar de ubicación o quedar fuera de producción mediante una decisión explícita registrada aquí.
