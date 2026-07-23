# Matriz de trazabilidad del rediseño UI

Esta matriz es el control obligatorio para reorganizar la interfaz sin ocultar capacidades existentes. Debe actualizarse en cada PR que mueva una función o cambie su interacción.

| Función actual | Nueva ubicación | Acción primaria | Acción secundaria | API usada | Prueba prevista |
|---|---|---|---|---|---|
| Completar tarea | Hoy / Agenda | Checkbox | Más | `actuarAgenda(..., COMPLETAR)` | Vitest / E2E |
| Omitir tarea | Agenda / Más | — | Menú Más | `actuarAgenda(..., OMITIR)` | Vitest |
| Reprogramar tarea | Agenda / Más | — | Formulario | `actuarAgenda(..., REPROGRAMAR)` | E2E |
| Crear tarea | Agenda / Añadir | Guardar | — | `crearTarea` | E2E |
| Crear evento | Agenda / Añadir | Guardar | — | `crearEvento` | Vitest / E2E |
| Omitir evento | Agenda / Más | — | Menú Más | `actuarAgenda(..., OMITIR)` | Vitest |
| Reprogramar evento | Agenda / Más | — | Formulario | `actuarAgenda(..., REPROGRAMAR)` | E2E |
| Marcar toma | Hoy / Salud | Tomada | — | `cambiarEstadoOcurrencia` | E2E |
| Posponer toma | Salud / Más | — | Menú Más | `cambiarEstadoOcurrencia` | E2E |
| Omitir toma | Salud / Más | — | Menú Más | `cambiarEstadoOcurrencia` | E2E |
| Reprogramar toma | Salud / Más | — | Formulario | `cambiarEstadoOcurrencia` | E2E |
| Cerrar revisión | Hoy / Atención | Cerrar | — | `cerrarElementoRevision` | E2E |
| Crear tratamiento | Salud / Tratamientos | Guardar | — | `crearTratamiento` | E2E |
| Cerrar tratamiento | Salud / Detalle | Finalizar | — | `cerrarTratamiento` | E2E |
| Ver receta | Salud / Detalle | Ver | — | `descargarReceta` | E2E |
| Subir receta | Salud / Detalle | Añadir | — | `subirReceta` | E2E |
| Eliminar receta | Salud / Detalle | — | Eliminar | `eliminarReceta` | E2E |
| Ver cuota | Salud / Recetas | — | — | `consultarCuota` | Vitest |
| Ver botiquín | Salud / Botiquín | Abrir detalle | Filtros | `consultarCatalogo` | E2E |
| Crear medicamento | Salud / Botiquín | Guardar | — | `crearMedicamento` | E2E |
| Ver vencimientos | Salud / Botiquín | Abrir detalle | Filtro | `consultarCatalogo` | E2E |
| Administrar perfiles | Ajustes / Familia | Editar | — | `consultarConfiguracionFamilia`, `crearPerfil`, `actualizarPerfil` | E2E |
| Ver auditoría | Ajustes / Actividad | Abrir | — | `consultarAuditoria` | E2E |
| Filtrar por persona | Hoy / Agenda / Salud | Elegir persona | — | Estado compartido | Vitest / E2E |
| Cerrar sesión | Menú de avatar | Cerrar sesión | — | `cerrarSesion` | E2E |

## Estado por PR

- PR 1 — Formulario de evento: extraído a un componente de dominio, abierto con `?crear=evento`, validado en la zona horaria familiar y cubierto en 320×700, 390×844 y 1280×900.
- PR 2A — AppShell y rutas: Pinia incorporado para el filtro compartido, destinos reales `/hoy`, `/agenda` y `/salud`, Familia/Actividad en el menú secundario y redirecciones compatibles desde las rutas anteriores. Objetos permanece oculto hasta disponer de dominio real.
- PR 2B — Listas compactas, menú Más, carga por dominio y accesibilidad automatizada: pendiente.
- PR 3 en adelante: pendiente.

Una función solo puede cambiar de ubicación o quedar fuera de producción mediante una decisión explícita registrada aquí.
