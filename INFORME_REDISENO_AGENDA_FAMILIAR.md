# Informe técnico y funcional para el rediseño de Agenda Familiar

**Proyecto:** `mherreraspe/agenda-familiar-app`

**Fecha de elaboración:** 19 de julio de 2026

**Objetivo:** entregar una especificación ejecutable por un agente de desarrollo para reducir la saturación de la PWA, corregir los flujos bloqueantes y reorganizar la aplicación alrededor de las tareas reales de una familia.

> Este documento conserva la especificación base elaborada el 19 de julio de 2026. El avance efectivo de cada PR se mantiene en `docs/05-MATRIZ_REDISENO_UI.md` y el estado desplegado en `docs/00-RELEVO_RAPIDO.md`.

## 1. Resumen ejecutivo

Agenda Familiar ya dispone de capacidades funcionales valiosas: tareas, eventos, tratamientos, ocurrencias de tomas, botiquín, fotografías privadas de recetas, auditoría, perfiles y permisos. El problema principal no es la falta de funciones, sino que casi todas se presentan al mismo tiempo dentro de una sola vista.

La recomendación central es transformar la aplicación en una agenda operativa compartida, organizada alrededor de cuatro preguntas:

- **Hoy:** ¿qué toca hacer o confirmar ahora?
- **Agenda:** ¿qué viene después?
- **Salud:** ¿qué medicamento toca, quién lo administró y qué tenemos guardado?
- **Objetos:** ¿dónde guardamos algo importante?

La gestión de perfiles, permisos, sesiones, historial y configuración debe seguir disponible, pero no ocupar la navegación principal porque se usa con mucha menor frecuencia.

La nueva navegación móvil propuesta es:

```text
[ Hoy ] [ Agenda ] [ Salud ] [ Objetos ]
```

En pantallas amplias debe convertirse en un sidebar o navigation rail, conservando los mismos destinos.

### Decisiones principales

- Reducir la navegación principal de seis destinos a cuatro.
- Eliminar Revisar como destino principal: pasa a ser un estado o bandeja contextual dentro de Hoy.
- Unificar Botiquín y Tratamientos dentro de Salud.
- Mover Familia y permisos al avatar o menú de configuración.
- Reemplazar las tarjetas grandes y repetitivas por listas compactas, agrupadas por día.
- Mostrar una sola acción principal por fila y mover las demás a un menú Más.
- Convertir el FAB permanente “Agregar tarea” en una acción global “Añadir”, sin tapar contenido.
- Corregir primero el formulario de eventos antes del rediseño completo.
- Mantener los contratos REST y la base de datos intactos en los dos primeros PR.
- Implementar Objetos/Baúl, participantes múltiples y tareas sin hora exacta en PR posteriores porque requieren cambios de dominio.

## 2. Hallazgos verificados en el repositorio

### 2.1 Una sola vista concentra casi toda la aplicación

El router actual asigna `/`, `/calendario`, `/familia` y `/revisar` al mismo componente `PantallaHoy.vue`.

Archivo actual: `frontend/src/router.ts`.

Esto explica que las rutas no produzcan vistas realmente diferentes y que la interfaz dependa de anclas internas.

### 2.2 La navegación no refleja la ruta

La barra inferior actual usa enlaces `href="#..."`, y “Hoy” tiene la clase `activo` escrita de forma fija.

Consecuencias:

- “Hoy” aparece activo aunque el usuario esté viendo otra sección.
- El botón Atrás no se comporta como navegación real entre áreas.
- No existen cargas, errores ni estados independientes por vista.
- Las rutas declaradas no representan la arquitectura de información.

### 2.3 El botón global siempre crea una tarea

El botón fijo actual es `+ Agregar tarea`. Aparece incluso cuando el usuario está en Familia, Botiquín o Tratamientos y puede cubrir contenido. Una acción global no debe estar vinculada a un solo tipo de alta.

### 2.4 Los formularios están incrustados en la vista principal

Tarea, evento, medicamento, tratamiento y perfil están escritos dentro de `PantallaHoy.vue`. El diálogo se abre con el atributo `:open`, no mediante `showModal()`, y el CSS no establece un alto máximo ni scroll interno.

Esto causa el flujo bloqueante observado en móvil:

- el formulario puede superar la altura disponible;
- Guardar queda fuera de la zona visible;
- el contenido de fondo sigue formando parte del mismo árbol de interacción;
- los errores globales se muestran en la vista subyacente;
- no existe gestión consistente de foco, Escape ni restauración del foco.

### 2.5 Las sugerencias de lugares aparecen sin consulta

El cálculo actual devuelve hasta cinco lugares cuando el texto está vacío. El comportamiento deseado es:

- no mostrar resultados con campo vacío;
- empezar desde dos caracteres;
- debounce de 250–300 ms;
- máximo tres resultados;
- cerrar la lista al seleccionar, borrar o abandonar el campo.

### 2.6 Una acción recarga toda la aplicación

La función de carga actual solicita en paralelo Hoy, catálogo, ocurrencias, auditoría, configuración familiar y cuota. Después de muchas acciones se vuelve a ejecutar la carga completa. Esto acopla todos los módulos, aumenta solicitudes innecesarias y hace difícil crear estados de carga o error por pantalla.

### 2.7 Las capacidades actuales sí cubren gran parte del caso real

El frontend y la API ya admiten:

- evento con título e inicio obligatorios;
- persona, tipo, lugar, dirección, notas y fin opcionales;
- tipo de evento como texto de hasta 40 caracteres;
- tarea con responsable y fecha límite;
- tratamientos con varios horarios o intervalo;
- responsable y responsable alternativo;
- ocurrencias con estado, actor que resolvió y hora de resolución;
- botiquín con cantidad, unidad y vencimiento;
- historial de auditoría;
- fotografías privadas y cuota.

Por tanto, el rediseño inicial puede mejorar mucho la experiencia sin modificar el backend.

## 3. Necesidades familiares que debe resolver el producto

### 3.1 Coordinación diaria

El usuario no abre la aplicación para explorar todas las funciones. Entra para responder rápidamente:

- qué está pendiente;
- qué toma viene ahora;
- si otra persona ya administró el medicamento;
- qué actividad o compra se aproxima;
- qué necesita atención porque se venció o no se confirmó.

### 3.2 Eventos generales, no solo citas médicas

El dominio visual debe hablar de Eventos o Actividades, no limitarse a “citas”.

Ejemplos: control pediátrico, dentista u oftalmólogo, notaría, reunión de padres, cumpleaños, viaje, concurso, actividad escolar, mantenimiento, trámite y compromiso familiar.

“Salud”, “Escuela”, “Trámite”, “Viaje” o “Cumpleaños” pueden ser categorías opcionales. La API actual acepta un texto libre de hasta 40 caracteres, por lo que ampliar las opciones visibles no requiere inicialmente un cambio REST.

### 3.3 Tareas y recordatorios futuros

Una tarea familiar no siempre es inmediata. Puede ser comprar la torta antes del cumpleaños, comprar chorizo para la parrillada, comprar una camisa, sacar una cita, llevar documentos, sacar la basura o arreglar algo de la casa.

La alta debe ser rápida y no obligar a rellenar descripción, recurrencia o prioridad cuando no son necesarias.

### 3.4 Tratamientos compartidos entre cuidadores

El caso crítico no es únicamente recordar la hora. Es coordinar a varias personas:

```text
Amoxicilina — Alessio
✓ Tomada por Mamá a las 08:04
```

La aplicación ya recibe `resueltaPorNombre` y `resueltaEn`; el rediseño debe volver esa información visible y prioritaria.

Esto reduce dos riesgos de coordinación:

- creer que otra persona ya dio la toma cuando no ocurrió;
- administrar de nuevo una toma que ya fue registrada.

La aplicación debe conservar la indicación escrita por la familia y nunca calcular ni recomendar dosis.

### 3.5 Inventario del botiquín

El botiquín sirve para saber qué medicamento existe, cuánto queda, cuándo vence, dónde está, si está vinculado a un tratamiento y si conviene evitar una compra duplicada.

### 3.6 Objetos o “baúl familiar”

Se propone un nuevo dominio para registrar dónde se guardaron objetos importantes: partida notarial, bandera, mochilas de viaje, documentos, herramientas, cajas, repuestos y accesorios de temporada.

Este dominio no existe actualmente y requerirá un PR independiente con migración, API y frontend.

## 4. Fundamento de diseño

### 4.1 Navegación principal limitada

Android recomienda barras de navegación para tres a cinco destinos del mismo nivel. Apple también advierte que demasiadas pestañas generan saturación y problemas de distribución.

Aplicación concreta: Hoy, Agenda, Salud y Objetos. Familia queda en configuración. Revisar queda como estado.

### 4.2 Divulgación progresiva

Las opciones avanzadas o poco frecuentes deben aparecer después de las esenciales.

Ejemplos:

- primero título, persona y fecha; después lugar, notas, repetición y detalles;
- primero “Tomada”; después “Omitir”, “Posponer” o “Reprogramar” dentro de Más;
- primero nombre y ubicación del objeto; después foto, categoría y nota.

### 4.3 Reconocimiento antes que memoria

La aplicación debe permitir reconocer personas con avatar/color, lugares anteriores, ubicaciones recientes, tratamientos previos, horarios habituales y quién realizó una acción. No debe obligar a recordar y reescribir información ya conocida por el sistema.

### 4.4 Listas para información repetitiva

Las tareas, eventos, tomas, medicamentos y objetos son colecciones textuales. En móvil deben mostrarse como filas agrupadas, no como una gran tarjeta independiente por cada elemento.

Las tarjetas se reservan para el próximo elemento crítico, una alerta, un resumen, un estado vacío o bloques de información de alto nivel.

### 4.5 Una acción destacada por contexto

Cada fila debe tener una acción primaria clara:

| Elemento | Acción principal |
|---|---|
| Tarea | Marcar hecha |
| Toma | Marcar tomada |
| Evento | Abrir detalle |
| Medicamento | Abrir detalle |
| Tratamiento | Abrir detalle |
| Objeto | Abrir detalle |

Las acciones secundarias pasan a un menú Más.

### 4.6 Adaptación real por tamaño

- Compacto: una columna, barra inferior, lista o detalle.
- Desde 840 px: sidebar/navigation rail.
- Escritorio: hasta 1.200 px, dos paneles cuando aporte valor.
- Agenda de escritorio: calendario semanal y panel de tareas sin hora.
- Móvil: franja semanal horizontal y agenda vertical del día.

### 4.7 Accesibilidad mínima

- objetivos táctiles de al menos 44 × 44 px;
- reflow sin desplazamiento horizontal desde 320 px;
- foco visible;
- errores junto al campo;
- estado activo con `aria-current="page"`;
- iconos acompañados por etiqueta;
- diálogos modales con foco contenido, Escape y restauración del foco;
- mensajes de estado mediante `role="status"` o región viva apropiada.

## 5. Arquitectura de información objetivo

### 5.1 Navegación principal

```text
/hoy
/agenda
/salud
/objetos
```

### 5.2 Configuración secundaria

```text
/ajustes/familia
/ajustes/sesiones        # futuro
/actividad               # historial completo, si se mantiene separado
```

### 5.3 Compatibilidad de rutas

```text
/                       -> /hoy
/calendario             -> /agenda
/revisar                -> /hoy?filtro=atencion
/botiquin               -> /salud?seccion=botiquin
/tratamientos           -> /salud?seccion=tratamientos
/familia                -> /ajustes/familia
```

### 5.4 Qué no debe ser un destino principal

- Revisar: es un estado transversal.
- Añadir: es una acción.
- Perfil/Familia: es configuración poco frecuente.
- Historial: es detalle o trazabilidad secundaria.
- Vencimientos: es filtro o sección contextual de Salud.

## 6. Especificación de cada pantalla

### 6.1 Hoy

**Propósito:** permitir que una persona entienda en pocos segundos qué debe hacer o confirmar hoy.

#### Encabezado

```text
Domingo, 19 de julio                       [avatar]

Buenos días, Marco
Familia Herrera

[Todos ▾]                           [+ Añadir]
```

El avatar abre Familia y permisos, historial, ajustes y cerrar sesión.

#### Resumen compacto

Reemplazar tres tarjetas numéricas por una línea:

```text
Hoy: 1 tarea · 3 tomas · 1 evento
```

Los números pueden ser enlaces a filtros, pero no deben ocupar media pantalla.

#### Sección “Necesita atención”

Solo aparece cuando existe contenido: tareas vencidas, tomas no confirmadas, eventos pasados pendientes de revisión, medicamentos vencidos o tratamientos finalizados que requieren cierre.

```text
Necesita atención

08:00  Inhalador — Alessio
       Aún no confirmado                      [Marcar]
```

#### Sección “Próximo”

Mostrar un bloque destacado con el elemento temporal más cercano:

```text
10:30  Jarabe — Alessio
       5 ml según receta                      [Tomada]
```

Después de registrar:

```text
✓ Tomada por Mamá a las 10:34        [Deshacer]
```

#### Lista del día

Orden cronológico y compacto:

```text
☐ Comprar la torta
  Antes de las 5:00 p. m. · Papá                 ⋮

18:00  Reunión de padres
       Colegio · Mamá y Papá                     ⋮
```

#### Vista previa

Mostrar solo una síntesis de mañana o de los siguientes elementos:

```text
Mañana
• 08:00 — Inhalador
• Comprar camisa

Ver Agenda →
```

#### Elementos que deben salir de Hoy

- siete días completos;
- tratamientos completos;
- todo el botiquín;
- perfiles y permisos;
- historial completo;
- todas las próximas citas;
- cuota de fotos cuando está normal.

### 6.2 Agenda

**Propósito:** planificar y revisar tareas y eventos futuros.

Usar “Eventos” como categoría general. No llamar a toda la sección “Próximas citas”.

#### Móvil

```text
Agenda                                      [+ Evento]

[Todo] [Eventos] [Tareas]

Julio 2026

L 20   M 21   M 22   J 23   V 24   S 25   D 26
 •      2      •      3      1

Viernes 24

SIN HORA
☐ Comprar torta
  Papá · antes del cumpleaños

CON HORA
10:00  Notaría
       Papá · Notaría López

18:00  Cumpleaños de Carmen
       Toda la familia
```

#### Escritorio

- sidebar a la izquierda;
- calendario semanal al centro;
- panel lateral o franja para tareas sin hora;
- selección de un evento abre detalle sin perder el contexto;
- máximo 1.200 px de ancho útil.

#### Filtros y agrupación

- Todo, Eventos y Tareas.
- Persona seleccionada, compartida con las demás rutas.
- Agrupar por hoy, mañana y fecha.
- Grupos extensos colapsables.
- Opción de ir a una fecha.

#### Acciones

- Evento: tocar fila abre detalle; menú Más contiene editar, reprogramar, duplicar y cancelar.
- Tarea: checkbox completa; menú Más contiene editar, reprogramar, reasignar, omitir y eliminar.
- No mostrar “Omitir” y “Reprogramar” como botones permanentes en cada tarjeta.

### 6.3 Salud

**Propósito:** coordinar tratamientos y existencias sin convertir la pantalla principal en una ficha clínica.

Subnavegación:

```text
[Hoy] [Tratamientos] [Botiquín] [Recetas]
```

Puede representarse mediante tabs internas porque son vistas del mismo destino.

#### Salud > Hoy

```text
08:00  Amoxicilina — Alessio
       ✓ Tomada por Mamá a las 08:04

12:00  Inhalador — Alessio
       Pendiente                              [Tomada]

16:00  Amoxicilina — Alessio
       Próxima
```

Las acciones secundarias de toma —posponer, omitir, reprogramar, cancelar y ver indicación— deben estar en Más.

#### Salud > Tratamientos

Fila compacta:

```text
Amoxicilina · Alessio
3 veces al día · hasta el 25 de julio
Próxima: 16:00                                      ›
```

Detalle: persona, medicamento, indicación, horarios, fecha inicial y final, responsables, receta, historial de tomas, finalizar tratamiento y editar.

El aviso “la aplicación no calcula dosis” debe aparecer en la creación y detalle, no repetido entre todas las filas.

#### Salud > Botiquín

Buscador y filtros:

```text
Buscar medicamento

[Todos] [Poco stock] [Por vencer] [Vencidos]

Ibuprofeno
8 tabletas · vence nov. 2026
Ropero principal · caja del botiquín
```

La ubicación puede comenzar como nota visible sin cambiar el contrato actual; una integración estructurada con Objetos puede realizarse después.

#### Salud > Recetas

- recetas vinculadas a tratamientos;
- miniatura opcional;
- persona y tratamiento;
- fecha;
- almacenamiento privado;
- cuota solo destacada cuando supere umbrales.

#### Regla de seguridad

La aplicación registra el texto indicado, organiza horarios elegidos por la familia y registra quién confirmó una toma. No recomienda dosis, no modifica la indicación médica y no deduce cantidades clínicas.

### 6.4 Objetos

**Propósito:** permitir encontrar rápidamente algo que se guardó y se olvidó.

#### Pantalla principal

```text
Objetos
¿Dónde está?

[ Buscar objetos o ubicaciones                    ]

Guardados recientemente

Partida notarial
Habitación principal › ropero › caja de documentos

Bandera del Perú
Cuarto de estudio › baúl grande

Mochilas de viaje
Cochera › estante superior
```

#### Exploración por ubicación

```text
Casa
├── Habitación principal
│   └── Ropero
├── Cuarto de Alessio
├── Cochera
└── Lavandería
```

#### Formulario mínimo

```text
¿Qué guardaste?
¿Dónde?
Nota opcional
Foto opcional

[Guardar]
```

Mostrar ubicaciones recientes y permitir crear una nueva en línea.

Buscar por nombre del objeto, nota, categoría, ubicación y ruta completa de ubicación.

Este módulo no puede incluirse solo como rediseño frontend. Requiere migración Flyway, tablas de ubicaciones y objetos, RLS, endpoints REST, pruebas de aislamiento e IDOR, auditoría, UI y búsqueda. Se implementará en un PR propio después de estabilizar el AppShell.

## 7. Acción global “Añadir”

### Ubicación

No debe ser una pestaña. Tampoco debe ser un botón grande fijo que tape contenido.

Recomendación:

- móvil: botón `+ Añadir` en la barra superior;
- escritorio: botón al inicio del sidebar;
- puede abrir una hoja inferior o menú adaptativo.

### Opciones globales

```text
¿Qué deseas añadir?

Evento
Tarea o recordatorio
Tratamiento
Objeto
```

Dentro de Salud se conserva una acción contextual: Añadir medicamento al botiquín. Dentro de Agenda: Nuevo evento y Nueva tarea.

### Reglas

- recordar desde qué pantalla se abrió;
- después de guardar, volver a esa pantalla;
- restaurar el foco al control de apertura;
- no duplicar formularios para móvil y escritorio;
- reutilizar el mismo componente de dominio con un contenedor adaptativo.

## 8. Formularios propuestos

### 8.1 Contenedor adaptativo común

Componentes sugeridos:

- `FormularioAdaptativo.vue`
- `CabeceraFormulario.vue`
- `AccionesFormulario.vue`
- `ErrorCampo.vue`
- `ResumenErrores.vue`
- `ConfirmarDescarte.vue`

Comportamiento:

- URL: `?crear=evento|tarea|tratamiento|medicamento|objeto`;
- móvil: ocupa `100dvh`;
- escritorio: `<dialog>` abierto con `showModal()`;
- encabezado fijo;
- contenido con scroll interno;
- acciones Cancelar/Guardar persistentes;
- Escape cierra;
- Tab no sale del diálogo;
- el foco inicial va al título o primer campo;
- al cerrar, el foco vuelve al activador;
- borrador modificado solicita confirmación;
- botón Atrás del navegador cierra primero el formulario;
- acceso directo a URL con `crear` elimina la query en vez de sacar al usuario de la aplicación.

No usar `<dialog :open="...">` como único mecanismo modal. Usar una referencia al elemento y llamar `dialog.showModal()` y `dialog.close()`.

### 8.2 Evento

Campos iniciales: título, fecha, hora, duración o fin opcional y participantes/persona.

Más opciones: todo el día, categoría, lugar, dirección, notas, repetición y recordatorio anticipado.

Validaciones:

- título no vacío;
- inicio futuro;
- fin posterior al inicio;
- fin de recurrencia posterior al primer evento;
- conversiones usando la zona horaria familiar;
- conservar datos ante error 400;
- enfocar primer error.

Valores iniciales:

- inicio redondeado al próximo intervalo de 15 minutos;
- usar la zona horaria familiar;
- no usar silenciosamente la zona del dispositivo.

Sugerencias:

- iniciar con dos caracteres;
- debounce 250–300 ms;
- máximo tres;
- separar sugerencias de título y lugar;
- ocultar con valor vacío;
- permitir seguir escribiendo sin obligar a seleccionar.

Categorías visuales sugeridas: Salud, Escuela, Trámite, Viaje, Cumpleaños, Casa y Otro. La categoría es opcional y no debe bloquear el alta.

### 8.3 Tarea

Alta rápida: título, fecha mediante presets, persona y Guardar.

Presets: Hoy, Mañana, Este fin de semana y Elegir fecha.

Más opciones: hora, detalle, repetición, recordatorio y prioridad.

La API exige una fecha/hora `Instant`. Una tarea verdaderamente “sin hora” requiere una mejora de dominio posterior. En los PR de UI no se debe inventar una hora visible falsa.

Opciones para el PR posterior:

- añadir `todoElDia` y fecha local;
- mantener un instante interno, pero ocultarlo solo si existe una regla explícita documentada.

Se recomienda la primera opción.

### 8.4 Tratamiento

Alta básica:

- persona;
- nombre del medicamento o tratamiento;
- método de horario: horarios fijos o cada X horas;
- inicio;
- fin opcional.

Más detalles: vincular al botiquín, indicación, cantidad escrita en receta, responsable, responsable alternativo y foto de receta.

Confirmación previa:

```text
Se crearán tomas a las:
08:00 · 16:00 · 00:00

[Corregir] [Confirmar]
```

No calcular dosis. Solo confirmar el calendario derivado de las horas elegidas.

### 8.5 Medicamento

Alta básica: nombre, cantidad y unidad.

Más opciones: presentación, concentración, vencimiento, ubicación, foto y nota.

La ubicación estructurada depende del módulo Objetos. Hasta entonces puede seguir como dato no estructurado si se decide ampliar el API en un PR separado.

### 8.6 Objeto

- nombre;
- ubicación;
- nota opcional;
- foto opcional;
- categoría opcional.

Tiempo objetivo de alta: menos de 20 segundos para el caso mínimo.

## 9. Modelo de interacción de filas

### 9.1 Tarea

```text
☐ Comprar la torta
  Viernes · Papá                                  ⋮
```

- checkbox: completar;
- Más: reprogramar, editar, reasignar, omitir, eliminar;
- después de completar: mostrar Deshacer temporal.

### 9.2 Evento

```text
10:00  Control pediátrico
       Alessio · Clínica La Luz                   ⋮
```

- tocar: detalle;
- Más: editar, reprogramar, duplicar, cancelar;
- no usar “Omitir” como acción primaria.

### 9.3 Toma

```text
08:00  Amoxicilina · Alessio             [Tomada]
```

Después:

```text
✓ Mamá · 08:04                              [Deshacer]
```

Más: posponer, omitir, reprogramar, cancelar y ver indicación.

### 9.4 Tratamiento

```text
Amoxicilina · Alessio
3 veces al día · hasta 25 jul.                      ›
```

Tocar abre el detalle. Las acciones de receta y cierre quedan en el detalle.

### 9.5 Medicamento

```text
Paracetamol
8 tabletas · vence nov. 2026                        ›
```

Tocar abre el detalle. El estado se expresa mediante texto e icono, no mediante una gran insignia decorativa.

### 9.6 Objeto

```text
Bandera del Perú
Cuarto de estudio › baúl grande                     ›
```

Tocar abre el detalle. Más contiene mover, editar y eliminar.

## 10. Sistema visual

### 10.1 Principio

“Calma familiar” no significa aumentar el tamaño de cada tarjeta. Significa reducir competencia visual y facilitar el escaneo.

### 10.2 Tipografía

- marca o saludo: serif opcional;
- títulos funcionales: sans serif;
- título de pantalla: 28–32 px;
- sección: 18–22 px;
- texto principal: 16–17 px;
- secundario: 14–15 px;
- evitar mayúsculas decorativas repetidas.

### 10.3 Contenedores

- fondo cálido;
- verde principal;
- menos sombras;
- menos bordes;
- radio moderado;
- listas con separadores;
- tarjetas solo para bloques destacados.

### 10.4 Altura orientativa

- fila simple: 64–76 px;
- fila con detalle: 80–96 px;
- tarjeta destacada: según contenido;
- evitar tarjetas de 200–280 px para tres líneas.

### 10.5 Color

- verde: completado/disponible;
- ámbar: próximo/atención;
- rojo: vencido/error real;
- gris: secundario;
- colores de persona: avatar o pequeño indicador;
- no usar un borde grueso en cada fila sin un significado de estado.

### 10.6 Controles

- región táctil mínima: 44 × 44 px;
- estados hover, focus, active y disabled;
- foco visible;
- iconos SVG simples en código;
- etiqueta de texto siempre presente en navegación.

### 10.7 Safe areas

- respetar `env(safe-area-inset-bottom)`;
- ningún botón debe quedar debajo de la barra del sistema;
- reservar padding inferior si existe un control flotante;
- preferir no superponer acciones sobre las listas.

## 11. Arquitectura frontend propuesta

### 11.1 Dependencias

Mantener:

- Vue 3;
- TypeScript;
- Vite;
- Vue Router;
- Vitest;
- Vue Test Utils.

Añadir:

- Pinia;
- `@playwright/test`;
- `@axe-core/playwright` o integración equivalente.

No añadir una librería visual pesada.

### 11.2 Estructura sugerida

```text
frontend/src/
  app/
    AppShell.vue
    BarraSuperior.vue
    NavegacionInferior.vue
    NavegacionLateral.vue
    MenuAgregar.vue

  router/
    index.ts

  stores/
    sesion.ts
    familia.ts
    hoy.ts
    agenda.ts
    salud.ts
    interfaz.ts

  views/
    PantallaHoy.vue
    PantallaAgenda.vue
    PantallaSalud.vue
    PantallaObjetos.vue
    PantallaFamilia.vue

  features/
    tareas/
      FormularioTarea.vue
      FilaTarea.vue
      validacionTarea.ts
    eventos/
      FormularioEvento.vue
      FilaEvento.vue
      DetalleEvento.vue
      validacionEvento.ts
    salud/
      FilaToma.vue
      FilaTratamiento.vue
      FilaMedicamento.vue
      FormularioTratamiento.vue
      FormularioMedicamento.vue
    objetos/
      FormularioObjeto.vue
      FilaObjeto.vue
    familia/
      ListaPerfiles.vue
      FormularioPerfil.vue

  components/
    formularios/
      FormularioAdaptativo.vue
      AccionesFormulario.vue
      ErrorCampo.vue
      ConfirmarDescarte.vue
    estados/
      EstadoVacio.vue
      EstadoCarga.vue
      EstadoError.vue
      MensajeEstado.vue
    listas/
      MenuMas.vue
      EncabezadoGrupo.vue
```

Mantener nombres de dominio en español para seguir la convención del repositorio.

### 11.3 Estado compartido

Pinia:

- sesión;
- familia activa;
- perfiles;
- zona horaria;
- filtro de persona;
- badges globales;
- caché de recursos compartidos.

Estado local:

- borrador del formulario;
- menú abierto;
- tab interna;
- selección temporal.

No poner todos los borradores en el store global.

### 11.4 Carga de datos

Eliminar la dependencia de un único `cargar()` global. Crear acciones específicas:

```text
cargarHoy()
cargarAgenda()
cargarSalud()
cargarFamilia()
cargarCuota()
cargarAuditoria()
```

Después de una acción, invalidar solo los recursos afectados.

Ejemplos:

- completar tarea: Hoy + Agenda;
- marcar toma: Hoy + Salud + badge de atención;
- crear evento: Agenda + vista previa de Hoy;
- crear tratamiento: Salud + Hoy;
- editar perfil: Familia + selector global;
- subir receta: Salud + cuota + auditoría.

### 11.5 Enrutamiento

Usar `RouterLink` y nombres de ruta. El estado activo debe derivarse de `route.name` y exponer `aria-current="page"`. No usar anclas internas como navegación principal.

### 11.6 Formularios mediante URL

Composable sugerido: `useFormularioRuta()`.

Responsabilidades:

- leer `route.query.crear`;
- abrir;
- cerrar;
- detectar entrada directa;
- limpiar query;
- coordinar confirmación de descarte;
- restaurar foco;
- evitar navegación accidental.

### 11.7 Zona horaria

Crear utilidades únicas:

```text
redondearSiguienteCuarto()
fechaFamiliarAInstant()
instantAFechaFamiliar()
```

La fuente de verdad es `datos.zonaHoraria`, actualmente `America/Lima` para la familia de prueba.

No construir fechas con `new Date(valorLocal).toISOString()` sin aplicar explícitamente la zona familiar, porque ese código interpreta el dato en la zona del dispositivo.

## 12. Brechas de dominio que no deben ocultarse

| Necesidad | Estado actual | Tipo de cambio |
|---|---|---|
| Evento general | Compatible | Solo UI |
| Categorías generales | Compatible, `tipo` es texto | Solo UI |
| Evento con varias personas | No compatible; existe un solo `perfilId` | API + BD |
| Tarea sin hora exacta | No compatible de forma limpia; exige `Instant` | API + BD |
| Varios horarios de tratamiento | Compatible | Solo UI |
| Registrar quién dio la toma | Compatible | Solo UI |
| Actualización inmediata entre celulares | No implementada | SSE / sincronización |
| Botiquín | Compatible | Solo UI |
| Objetos/Baúl | No existe | API + BD + UI |
| Ubicaciones jerárquicas | No existe | API + BD + UI |
| Push privado | Pendiente | Backend + PWA |

El agente no debe simular en frontend una capacidad que el dominio no soporta.

## 13. Plan de implementación por PR

### Paso previo obligatorio: cerrar PR #16

Estado observado al elaborar este informe:

- PR #16 abierto;
- draft;
- mergeable;
- solo documentación;
- rama `agent/cerrar-bloque-v8`;
- base `main`.

Acciones:

- comprobar que sigue vigente y sin cambios incompatibles;
- marcarlo listo para revisión;
- confirmar CI o validaciones documentales requeridas;
- integrarlo;
- actualizar `main`;
- crear todas las ramas nuevas desde el `main` resultante.

No comenzar el rediseño desde la rama anterior.

### PR 1 — Formulario crítico de evento

**Rama sugerida:** `agent/formulario-evento-adaptativo`.

#### Alcance

- extraer `FormularioEvento.vue`;
- crear `FormularioAdaptativo.vue`;
- abrir mediante `?crear=evento`;
- `showModal()` y `close()`;
- móvil `100dvh`;
- escritorio modal amplio;
- scroll interno;
- encabezado y acciones persistentes;
- Cancelar/Guardar siempre visibles;
- validaciones temporales;
- zona horaria familiar;
- inicio por defecto al siguiente cuarto de hora;
- sugerencias desde dos caracteres;
- debounce;
- máximo tres resultados;
- errores locales;
- error 400 dentro del formulario;
- conservar valores;
- enfocar primer error;
- Escape;
- Atrás;
- confirmación de descarte;
- restauración del foco.

#### No incluir

- nueva navegación;
- Objetos;
- cambios de API;
- cambios de base de datos;
- rediseño completo de tarjetas;
- SSE;
- Push.

#### Archivos probables

```text
frontend/src/views/PantallaHoy.vue
frontend/src/features/eventos/FormularioEvento.vue
frontend/src/components/formularios/FormularioAdaptativo.vue
frontend/src/components/formularios/ConfirmarDescarte.vue
frontend/src/features/eventos/validacionEvento.ts
frontend/src/composables/useFormularioRuta.ts
frontend/src/utils/fechaFamiliar.ts
frontend/src/styles.css
frontend/src/router.ts
```

#### Pruebas

Vitest:

- apertura por query;
- cierre y limpieza de query;
- validación;
- error 400;
- sugerencias;
- timezone;
- borrador modificado.

Playwright mínimo para el bug:

- 320 × 700;
- 390 × 844;
- Guardar visible;
- scroll;
- Escape;
- Atrás;
- confirmación;
- POST mínimo;
- POST recurrente.

#### Definición de terminado

- `agenda-ops VerificarLocal`;
- CI verde;
- despliegue inmediato desde `main`;
- E2E HTTPS;
- revisión autenticada móvil y escritorio.

### PR 2 — AppShell y rediseño estructural

**Rama sugerida:** `agent/redisenio-estructural`. Crearla después de integrar PR 1.

#### Alcance

- instalar Pinia;
- crear AppShell;
- crear rutas reales;
- cuatro destinos;
- barra inferior con iconos y etiquetas;
- sidebar desde 840 px;
- filtro de persona compartido;
- separar vistas;
- simplificar tarjetas en filas;
- acción principal única;
- menú Más;
- unificar Salud;
- mover Familia al avatar;
- redirecciones antiguas;
- carga por dominio;
- estados vacíos/carga/error consistentes;
- ancho máximo 1.200 px;
- dos paneles cuando corresponda;
- no overflow desde 320 px.

#### Rutas

```text
/hoy
/agenda
/salud
/ajustes/familia
```

`/objetos` puede quedar fuera de producción hasta que exista su backend. No publicar un destino principal vacío. Durante este PR la navegación puede tener temporalmente tres destinos: Hoy, Agenda y Salud.

Alternativa aceptable: incluir Objetos detrás de feature flag desactivado, pero no mostrarlo al usuario hasta tener funcionalidad real.

#### No incluir

- migración;
- endpoints nuevos;
- participantes múltiples;
- tareas sin hora;
- Objetos funcional;
- SSE o Push.

#### Pruebas

- rutas reales;
- ruta activa;
- redirecciones;
- filtro compartido;
- una acción principal;
- menú Más;
- estados;
- 320, 390 y 1280 px;
- axe en vistas;
- regresión de todas las funciones existentes.

#### Definición de terminado

- ninguna función actual inaccesible;
- ninguna ruta muestra contenido de otra área;
- “Hoy” no muestra siete días completos;
- Salud integra tratamientos, tomas, botiquín, recetas y cuota;
- Familia accesible desde avatar;
- CI verde;
- despliegue;
- E2E real.

### PR 3 — Sincronización entre cuidadores

**Rama sugerida:** `agent/sincronizacion-familiar-sse`.

Este bloque ya figura como siguiente paso recomendado en la documentación del proyecto y es esencial para el caso “Mamá dio la toma y Papá debe verlo”.

#### Alcance

- SSE autenticado;
- reconexión con backoff;
- eventos por familia;
- invalidación selectiva de stores;
- reflejar tomas, tareas y eventos sin recarga manual;
- caché PWA de lectura;
- aviso sin conexión;
- sin escritura offline;
- preservar privacidad;
- evitar duplicados.

Resultado esperado: si Mamá marca una toma en su celular, el dispositivo abierto de Papá muestra `✓ Tomada por Mamá a las 08:04` sin recargar la pantalla.

No mezclar todavía Web Push, horarios silenciosos, escalamiento ni Objetos.

### PR 4 — Objetos/Baúl familiar

**Rama sugerida:** `agent/objetos-familiares`.

#### Modelo mínimo sugerido

```text
ubicaciones
- id
- familia_id
- padre_id opcional
- nombre
- descripcion opcional
- activo
- creado_en
- actualizado_en

objetos
- id
- familia_id
- ubicacion_id
- nombre
- nota opcional
- categoria opcional
- activo
- creado_por
- creado_en
- actualizado_en
```

Opcional posterior: archivo/foto, historial de movimientos, alias y etiquetas.

#### Requisitos

- Flyway V9 o siguiente disponible;
- RLS forzado;
- IDOR;
- auditoría;
- búsqueda;
- ruta de ubicación;
- alta rápida;
- mover objeto;
- archivar;
- pruebas de aislamiento.

#### REST sugerido

```text
GET    /api/v1/familias/{familiaId}/ubicaciones
POST   /api/v1/familias/{familiaId}/ubicaciones
PATCH  /api/v1/familias/{familiaId}/ubicaciones/{id}

GET    /api/v1/familias/{familiaId}/objetos?q=
POST   /api/v1/familias/{familiaId}/objetos
GET    /api/v1/familias/{familiaId}/objetos/{id}
PATCH  /api/v1/familias/{familiaId}/objetos/{id}
DELETE /api/v1/familias/{familiaId}/objetos/{id}
```

Al integrar este PR, añadir el cuarto destino Objetos.

### PR 5 — Mejoras de dominio de agenda

Separar de Objetos para evitar un PR excesivo.

Alcance propuesto:

- participantes múltiples para eventos;
- tareas y eventos de todo el día;
- tareas con fecha sin hora;
- recordatorios anticipados configurables;
- categorías normalizadas si se considera necesario.

Eventos múltiples requieren una relación:

```text
evento_perfiles
- familia_id
- evento_id
- perfil_id
```

No reemplazar el `perfilId` actual sin plan de compatibilidad. Diseñar transición de API.

### PR 6 — Notificaciones privadas

Después de SSE y estabilidad:

- Web Push genérico en pantalla bloqueada;
- contenido privado solo tras autenticación;
- preferencias;
- horario silencioso;
- deduplicación;
- reintentos;
- escalamiento opcional.

Ejemplo de notificación bloqueada:

```text
Agenda Familiar
Tienes un recordatorio pendiente.
```

No mostrar nombre del medicamento, persona ni detalle médico en la pantalla bloqueada.

## 14. Matriz de trazabilidad funcional

Antes de mover código, crear un archivo de control: `docs/05-MATRIZ_REDISENO_UI.md`.

| Función actual | Nueva ubicación | Acción primaria | Acción secundaria | API usada | Prueba |
|---|---|---|---|---|---|
| Completar tarea | Hoy/Agenda | checkbox | Más | acción tarea | Vitest/E2E |
| Omitir tarea | Más | — | menú | acción tarea | Vitest |
| Reprogramar tarea | Más | — | formulario | acción tarea | E2E |
| Crear evento | Agenda/Añadir | Guardar | — | `crearEvento` | E2E |
| Marcar toma | Hoy/Salud | Tomada | — | estado ocurrencia | E2E |
| Posponer toma | Más | — | menú | estado ocurrencia | E2E |
| Ver receta | Salud detalle | Ver | — | `descargarReceta` | E2E |
| Subir receta | Tratamiento detalle | Añadir | — | `subirReceta` | E2E |
| Botiquín | Salud | Abrir | filtros | catálogo | E2E |
| Perfiles | Ajustes/Familia | Editar | — | configuración | E2E |
| Historial | Ajustes/Actividad | Abrir | — | auditoría | E2E |
| Cuota | Salud/Recetas | — | — | cuota | Vitest |

Ninguna función puede desaparecer sin una decisión explícita.

## 15. Pruebas y criterios de aceptación

### 15.1 Vitest y Vue Test Utils

- router y redirecciones;
- estado activo;
- filtro de persona compartido;
- agrupación por día;
- validadores;
- utilidades de zona horaria;
- query de formulario;
- borrador sucio;
- errores de campo;
- error de servidor;
- sugerencias;
- menús Más;
- estados de carga, vacío y error;
- acciones primarias;
- invalidación selectiva del store.

### 15.2 Playwright en CI

Añadir Chromium con APIs simuladas.

Viewports:

- 320 × 700;
- 390 × 844;
- 1280 × 900.

Escenarios:

- autenticación;
- navegación por rutas;
- ruta activa;
- apertura/cierre de formulario;
- Escape;
- Atrás;
- confirmación de descarte;
- evento mínimo;
- evento recurrente;
- error 400;
- sugerencias;
- tarea;
- toma;
- tratamiento;
- receta;
- botiquín;
- perfiles;
- redirecciones antiguas;
- sin overflow;
- auditoría de accesibilidad.

### 15.3 Accesibilidad automatizada

Ejecutar axe sobre Hoy, Agenda, Salud, Familia, formularios, menús, diálogos y estados de error.

No aceptar violaciones críticas o serias sin justificación documentada.

### 15.4 Métricas de experiencia

- evento mínimo: menos de 30 segundos;
- tarea rápida: menos de 15 segundos;
- tratamiento básico: menos de 45 segundos;
- objeto mínimo: menos de 20 segundos;
- una sola acción primaria visible por fila;
- máximo cuatro destinos principales;
- al menos dos elementos accionables visibles en el primer viewport de 390 × 844 cuando existan;
- cero controles tapados por navegación o acciones fijas;
- objetivos táctiles de 44 px;
- cero desplazamiento horizontal a 320 px.

### 15.5 Regresión

Deben seguir accesibles:

- tareas;
- recurrencia;
- eventos;
- omisión;
- reprogramación;
- revisión;
- ocurrencias;
- tratamientos;
- responsables;
- botiquín;
- vencimientos;
- recetas;
- cuota;
- perfiles;
- permisos;
- auditoría;
- sesión.

## 16. Reglas de implementación para el agente

### Debe hacer

- leer la guía operativa;
- verificar rama, PR, CI y producción antes de editar;
- trabajar por PR pequeño;
- usar nombres de dominio en español;
- conservar seguridad, RLS e idempotencia;
- reutilizar APIs existentes en PR 1 y 2;
- mantener pruebas;
- ejecutar `agenda-ops VerificarLocal`;
- desplegar solo desde `main`;
- documentar cada cambio;
- realizar revisión móvil autenticada.

### No debe hacer

- reescribir backend y frontend simultáneamente en el PR de UI;
- introducir una librería visual pesada;
- ocultar funciones sin trazabilidad;
- simular participantes múltiples solo en frontend;
- inventar una hora para tareas “sin hora” sin contrato explícito;
- calcular dosis;
- mostrar datos médicos en notificaciones bloqueadas;
- guardar tokens en `localStorage`;
- modificar secretos;
- borrar volúmenes;
- saltarse CI;
- crear un único formulario universal lleno de condicionales;
- duplicar formulario móvil y escritorio;
- dejar Hoy activo de forma fija;
- usar anclas como navegación principal;
- recargar todos los endpoints después de cada acción.

## 17. Orden recomendado

0. Integrar PR #16.
1. PR formulario crítico de evento.
2. PR AppShell y rediseño estructural.
3. PR SSE y sincronización entre cuidadores.
4. PR Objetos/Baúl.
5. PR participantes múltiples y tareas sin hora.
6. PR Web Push y preferencias.

Esta secuencia prioriza desbloquear al usuario, reducir saturación, hacer confiable la coordinación entre celulares, añadir el nuevo dominio de Objetos, ampliar capacidades de agenda e incorporar avisos privados.

## 18. Definición final de éxito

El rediseño se considera exitoso cuando:

- al abrir la app se entiende inmediatamente qué toca hoy;
- una toma muestra quién la confirmó y cuándo;
- una persona puede crear o abandonar un evento sin quedar atrapada;
- Agenda muestra tareas y eventos en una estructura temporal clara;
- Salud reúne tomas, tratamientos, botiquín y recetas sin repetir acciones;
- la navegación refleja la ruta real;
- Familia y configuración siguen disponibles sin ocupar una pestaña principal;
- no se pierde ninguna función existente;
- la UI funciona desde 320 px hasta escritorio;
- los cambios se entregan por PR con CI verde;
- Objetos se incorpora como dominio real, no como maqueta vacía.

## 19. Fuentes de referencia

### Navegación y adaptación

- [Android Navigation Bar](https://developer.android.com/develop/ui/compose/components/navigation-bar)
- [Android layouts and navigation patterns](https://developer.android.com/design/ui/mobile/guides/layout-and-content/layout-and-nav-patterns)
- [Android common layouts](https://developer.android.com/design/ui/mobile/guides/layout-and-content/common-layouts)
- [Apple Tab Bars](https://developer.apple.com/design/human-interface-guidelines/tab-bars)
- [Apple Lists and Tables](https://developer.apple.com/design/human-interface-guidelines/lists-and-tables)

### Accesibilidad

- [WCAG 2.2](https://www.w3.org/TR/WCAG22/)
- [Target Size Minimum](https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum)
- [Reflow](https://www.w3.org/WAI/WCAG21/Understanding/reflow.html)
- [Error Identification](https://www.w3.org/WAI/WCAG22/Understanding/error-identification)
- [Dialog Modal Pattern](https://www.w3.org/WAI/ARIA/apg/patterns/dialog-modal/)
- [Status Messages](https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html)
- [Apple Buttons](https://developer.apple.com/design/human-interface-guidelines/buttons)

### Usabilidad y formularios

- [Progressive Disclosure](https://www.nngroup.com/articles/progressive-disclosure/)
- [Usability Heuristics](https://www.nngroup.com/articles/ten-usability-heuristics/)
- [Reduce Cognitive Load in Forms](https://www.nngroup.com/articles/4-principles-reduce-cognitive-load/)
- [GOV.UK Question Pages](https://design-system.service.gov.uk/patterns/question-pages/)

### Vue

- [Vue state management](https://vuejs.org/guide/scaling-up/state-management.html)
- [Vue Router navigation guards](https://router.vuejs.org/guide/advanced/navigation-guards)
- [Pinia](https://pinia.vuejs.org/)

### Coordinación de medicamentos

- [Medication management by informal caregivers](https://pmc.ncbi.nlm.nih.gov/articles/PMC5690891/)
- [Medication management apps usability](https://pmc.ncbi.nlm.nih.gov/articles/PMC5694345/)
- [Meds@HOME caregiver medication administration](https://pmc.ncbi.nlm.nih.gov/articles/PMC11420605/)
