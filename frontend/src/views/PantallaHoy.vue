<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute } from 'vue-router'
import AppShell from '../app/AppShell.vue'
import TarjetaPendiente from '../components/TarjetaPendiente.vue'
import MenuMas from '../components/listas/MenuMas.vue'
import FormularioEvento from '../features/eventos/FormularioEvento.vue'
import { useFormularioRuta } from '../composables/useFormularioRuta'
import { useInterfazStore } from '../stores/interfaz'
import {
  cerrarSesion as eliminarSesion,
  actuarAgenda,
  actualizarObjeto,
  actualizarPerfil,
  cambiarEstadoOcurrencia,
  cerrarElementoRevision,
  cerrarTratamiento,
  completarTarea,
  consultarAuditoria,
  consultarConfiguracionFamilia,
  consultarCuota,
  consultarCatalogo,
  consultarHoy,
  consultarOcurrencias,
  consultarObjetos,
  crearPerfil,
  crearMedicamento,
  crearObjeto,
  crearTarea,
  crearTratamiento,
  descargarReceta,
  eliminarReceta,
  iniciarSesion,
  renovarSesion,
  subirReceta,
  type EstadoOcurrencia,
  type ElementoRevision,
  type OcurrenciaResumen,
  type ObjetoFamiliar,
  type RespuestaAuditoria,
  type RespuestaCatalogo,
  type RespuestaHoy,
  type RespuestaFamilia,
  type RespuestaCuota,
  type RespuestaOcurrencias,
  type RespuestaObjetos,
  type TareaResumen
} from '../api'
import { reducirImagenReceta, validarImagenReceta } from '../imagen'
import { suscribirEventosFamilia, type EstadoSincronizacion, type RecursoSincronizacion } from '../sincronizacion'

type SeccionPrincipal = 'hoy' | 'agenda' | 'salud' | 'objetos' | 'familia' | 'actividad'
type SeccionSalud = 'hoy' | 'tratamientos' | 'botiquin' | 'recetas'
type TipoAlta = 'evento' | 'tarea' | 'tratamiento' | 'medicamento' | 'objeto'

const props = withDefaults(defineProps<{ seccion?: SeccionPrincipal }>(), { seccion: 'hoy' })
const route = useRoute()
const interfaz = useInterfazStore()
const { filtroPerfil } = storeToRefs(interfaz)

const correo = ref('papa@familia.test')
const clave = ref('')
const cargando = ref(false)
const error = ref('')
const mensaje = ref('')
const sesionActiva = ref(false)
const administradorPlataforma = ref(false)
const restaurando = ref(true)
const enLinea = ref(typeof navigator === 'undefined' || navigator.onLine)
const estadoSincronizacion = ref<EstadoSincronizacion>('reconectando')
const datos = ref<RespuestaHoy | null>(null)
const catalogo = ref<RespuestaCatalogo | null>(null)
const agendaTratamientos = ref<RespuestaOcurrencias | null>(null)
const auditoria = ref<RespuestaAuditoria | null>(null)
const familia = ref<RespuestaFamilia | null>(null)
const cuota = ref<RespuestaCuota | null>(null)
const objetos = ref<RespuestaObjetos | null>(null)
const estadoVista = ref<'cargando' | 'lista' | 'error'>('cargando')
const errorVista = ref('')
const recursosCargados = reactive({ base: false, catalogo: false, ocurrencias: false, auditoria: false, familia: false, cuota: false, objetos: false })
let cerrarSincronizacion: (() => void) | null = null
let primeraNotificacionSincronizacion = true
const mostrarTodoSalud = reactive<Record<SeccionSalud, boolean>>({ hoy: false, tratamientos: false, botiquin: false, recetas: false })
const limiteHistorialTomas = ref(10)
const formulario = ref<'tarea' | 'medicamento' | 'tratamiento' | 'perfil' | 'objeto' | null>(null)
const dialogoFormulario = ref<HTMLDialogElement | null>(null)
const dialogoReceta = ref<HTMLDialogElement | null>(null)
let activadorFormulario: HTMLElement | null = null
let activadorReceta: HTMLElement | null = null
const nuevaTarea = reactive({ titulo: '', descripcion: '', perfilId: '', fechaLimite: '', repetir: false, frecuencia: 'SEMANAL', intervalo: 1, hasta: '' })
const nuevoMedicamento = reactive({ nombre: '', presentacion: '', concentracion: '', cantidad: 1, unidad: 'unidad', fechaVencimiento: '' })
const nuevoTratamiento = reactive({ perfilId: '', medicamentoId: '', nombre: '', indicacion: '', cantidadReceta: '', frecuencia: '', horario: '', horariosAdicionales: '', intervaloHoras: '', fechaInicio: '', fechaFin: '', responsablePerfilId: '', responsableAlternativoPerfilId: '' })
const busquedaObjetos = ref('')
const objetoEditadoId = ref<string | null>(null)
const nuevoObjeto = reactive({ nombre: '', categoria: '', notas: '', ruta: '', version: undefined as number | undefined })
let temporizadorBusquedaObjetos: ReturnType<typeof setTimeout> | null = null
const recetaSeleccionada = ref<File | null>(null)
const recetaVisible = ref<{ id: string; url: string } | null>(null)
const perfilEditadoId = ref<string | null>(null)
const nuevoPerfil = reactive({ nombre: '', tipo: 'DEPENDIENTE' as 'ADULTO' | 'DEPENDIENTE', color: '#315b4c', relacion: '', usuarioId: '', permiso: 'ADULTO' as 'ADMINISTRADOR_FAMILIAR' | 'ADULTO', activo: true })
const formularioEvento = ref<InstanceType<typeof FormularioEvento> | null>(null)
const {
  abierto: eventoAbierto,
  modificado: modificadoEvento,
  abrir: abrirFormularioEvento,
  cerrar: cerrarFormularioEvento,
  registrarConfirmacion
} = useFormularioRuta('evento')
registrarConfirmacion(() => formularioEvento.value?.preguntarDescarte() ?? Promise.resolve(false))

async function sincronizarDialogo(dialogo: HTMLDialogElement | null, abierto: boolean, devolverFoco: 'formulario' | 'receta') {
  await nextTick()
  if (!dialogo) return
  if (abierto && !dialogo.open) {
    const activo = document.activeElement
    if (devolverFoco === 'formulario' && !activadorFormulario) activadorFormulario = activo instanceof HTMLElement ? activo : null
    if (devolverFoco === 'receta' && !activadorReceta) activadorReceta = activo instanceof HTMLElement ? activo : null
    dialogo.showModal()
    await nextTick()
    const destino = dialogo.querySelector<HTMLElement>('[autofocus]')
      ?? dialogo.querySelector<HTMLElement>('form input, form select, form textarea')
      ?? dialogo.querySelector<HTMLElement>('button')
    destino?.focus()
  } else if (!abierto && dialogo.open) {
    dialogo.close()
    await nextTick()
    const activador = devolverFoco === 'formulario' ? activadorFormulario : activadorReceta
    activador?.focus()
    if (devolverFoco === 'formulario') activadorFormulario = null
    else activadorReceta = null
  }
}

watch(formulario, abierto => void sincronizarDialogo(dialogoFormulario.value, abierto !== null, 'formulario'))
watch(recetaVisible, abierta => void sincronizarDialogo(dialogoReceta.value, abierta !== null, 'receta'))
watch(eventoAbierto, abierto => {
  if (!abierto) return
  formulario.value = null
  cerrarRecetaVisible()
})

const fechaActual = new Intl.DateTimeFormat('es-PE', {
  weekday: 'long', day: 'numeric', month: 'long', timeZone: 'America/Lima'
}).format(new Date())

const coincideFiltro = (perfilId?: string) => filtroPerfil.value === 'TODOS' || perfilId === filtroPerfil.value
const pendientes = computed(() => datos.value?.tareas.filter(tarea => tarea.estado === 'PENDIENTE' && coincideFiltro(tarea.perfilId)) ?? [])
const atrasadas = computed(() => pendientes.value.filter(tarea => new Date(tarea.fechaLimite) < new Date()))
const proximas = computed(() => pendientes.value.filter(tarea => new Date(tarea.fechaLimite) >= new Date()))
const tareasHoy = computed(() => proximas.value.filter(tarea => claveFecha(tarea.fechaLimite) === claveFecha(new Date().toISOString())))
const eventosFiltrados = computed(() => catalogo.value?.eventos.filter(evento => evento.estado === 'PROGRAMADO' && new Date(evento.inicioEn) >= new Date() && coincideFiltro(evento.perfilId)) ?? [])
const eventosAtrasados = computed(() => catalogo.value?.eventos.filter(evento => evento.estado === 'PROGRAMADO' && new Date(evento.inicioEn) < new Date() && coincideFiltro(evento.perfilId)) ?? [])
const cantidadRevision = computed(() => elementosRevision.value.length + atrasadas.value.length + eventosAtrasados.value.length)
const tratamientosFiltrados = computed(() => catalogo.value?.tratamientos.filter(tratamiento => coincideFiltro(tratamiento.perfilId)) ?? [])
const ocurrenciasPendientes = computed(() => agendaTratamientos.value?.ocurrencias.filter(
  ocurrencia => ocurrencia.estado === 'PENDIENTE' && coincideFiltro(ocurrencia.perfilId)
) ?? [])
const historialOcurrencias = computed(() => agendaTratamientos.value?.ocurrencias.filter(
  ocurrencia => ocurrencia.estado !== 'PENDIENTE' && coincideFiltro(ocurrencia.perfilId)
).sort((a, b) => new Date(b.resueltaEn ?? b.programadaEn).getTime() - new Date(a.resueltaEn ?? a.programadaEn).getTime()) ?? [])
const vencimientosCercanos = computed(() => catalogo.value?.medicamentos.filter(
  medicamento => medicamento.estado === 'POR_VENCER' || medicamento.estado === 'VENCIDO'
).sort((a, b) => (a.fechaVencimiento ?? '').localeCompare(b.fechaVencimiento ?? '')) ?? [])
const elementosRevision = computed(() => agendaTratamientos.value?.revisar.filter(elemento => {
  if (filtroPerfil.value === 'TODOS' || elemento.origen !== 'OCURRENCIA') return true
  return agendaTratamientos.value?.ocurrencias.some(ocurrencia => ocurrencia.id === elemento.entidadId && coincideFiltro(ocurrencia.perfilId))
}) ?? [])
const seccionSalud = computed<SeccionSalud>(() => {
  const solicitada = route.query.seccion
  return solicitada === 'tratamientos' || solicitada === 'botiquin' || solicitada === 'recetas' ? solicitada : 'hoy'
})
const mostrandoHistorialTomas = computed(() => props.seccion === 'salud' && seccionSalud.value === 'hoy' && route.query.vista === 'historial')
const historialTomasVisible = computed(() => historialOcurrencias.value.slice(0, limiteHistorialTomas.value))
const hayContenidoHoy = computed(() => tareasHoy.value.length > 0 || ocurrenciasPendientes.value.length > 0 || cantidadRevision.value > 0)
const ocurrenciasVisibles = computed(() => ocurrenciasPendientes.value.slice(0, mostrarTodoSalud.hoy ? undefined : 5))
const tratamientosVisibles = computed(() => tratamientosFiltrados.value.slice(0, mostrarTodoSalud.tratamientos ? undefined : 5))
const medicamentosVisibles = computed(() => (catalogo.value?.medicamentos ?? []).slice(0, mostrarTodoSalud.botiquin ? undefined : 5))
const recetasVisibles = computed(() => tratamientosFiltrados.value.filter(tratamiento => tratamiento.recetaId).slice(0, mostrarTodoSalud.recetas ? undefined : 5))
const tituloFormulario = computed(() => ({
  tarea: 'Nueva tarea', medicamento: 'Nuevo medicamento', tratamiento: 'Nuevo tratamiento',
  perfil: perfilEditadoId.value ? 'Editar perfil' : 'Nuevo perfil', objeto: objetoEditadoId.value ? 'Editar objeto' : 'Nuevo objeto'
})[formulario.value ?? 'tarea'])
const etiquetaGuardarFormulario = computed(() => ({
  tarea: 'Guardar tarea', medicamento: 'Guardar medicamento', tratamiento: 'Guardar tratamiento',
  perfil: perfilEditadoId.value ? 'Guardar cambios' : 'Guardar perfil',
  objeto: objetoEditadoId.value ? 'Guardar cambios' : 'Guardar objeto'
})[formulario.value ?? 'tarea'])
const tituloPantalla = computed(() => ({
  hoy: 'Hoy', agenda: 'Agenda', salud: 'Salud', objetos: 'Objetos', familia: 'Familia y permisos', actividad: 'Actividad'
})[props.seccion])
const subtituloPantalla = computed(() => ({
  hoy: fechaActual, agenda: 'Tareas y eventos próximos', salud: 'Tomas, tratamientos, botiquín y recetas',
  objetos: 'Encuentra lo que guardó tu familia', familia: 'Personas, cuentas y permisos', actividad: 'Historial de cambios familiares'
})[props.seccion])
const tipoAnadirCabecera = computed<TipoAlta | undefined>(() => {
  if (props.seccion === 'agenda') return 'evento'
  if (props.seccion === 'objetos') return 'objeto'
  if (props.seccion === 'salud' && seccionSalud.value === 'tratamientos') return 'tratamiento'
  if (props.seccion === 'salud' && seccionSalud.value === 'botiquin') return 'medicamento'
  return undefined
})
const etiquetaAnadirCabecera = computed(() => tipoAnadirCabecera.value
  ? ({ evento: 'Evento', tarea: 'Tarea', tratamiento: 'Tratamiento', medicamento: 'Medicamento', objeto: 'Objeto' })[tipoAnadirCabecera.value]
  : 'Añadir')
const mostrarAnadirCabecera = computed(() => props.seccion !== 'familia' && props.seccion !== 'actividad' && !(props.seccion === 'salud' && (seccionSalud.value === 'recetas' || mostrandoHistorialTomas.value)))

function destinoSalud(destino: SeccionSalud) {
  const query = { ...route.query }
  delete query.vista
  if (destino === 'hoy') delete query.seccion
  else query.seccion = destino
  return { name: 'salud', query }
}

function destinoHistorialTomas() {
  return { name: 'salud', query: { ...route.query, vista: 'historial' } }
}

onMounted(async () => {
  window.addEventListener('online', actualizarEstadoRed)
  window.addEventListener('offline', actualizarEstadoRed)
  try {
    const sesion = await renovarSesion()
    administradorPlataforma.value = sesion.rolPlataforma === 'ADMINISTRADOR_PLATAFORMA'
    sesionActiva.value = true
    await cargarSeccion(props.seccion)
    iniciarSincronizacion()
  } catch {
    sesionActiva.value = false
  } finally {
    restaurando.value = false
  }
})

onBeforeUnmount(() => {
  cerrarSincronizacion?.()
  if (temporizadorBusquedaObjetos) clearTimeout(temporizadorBusquedaObjetos)
  window.removeEventListener('online', actualizarEstadoRed)
  window.removeEventListener('offline', actualizarEstadoRed)
})

function actualizarEstadoRed() {
  enLinea.value = navigator.onLine
  if (!enLinea.value) estadoSincronizacion.value = 'sin-conexion'
}

function iniciarSincronizacion() {
  cerrarSincronizacion?.()
  primeraNotificacionSincronizacion = true
  cerrarSincronizacion = suscribirEventosFamilia(async evento => {
    if (primeraNotificacionSincronizacion) {
      primeraNotificacionSincronizacion = false
      return
    }
    invalidarRecursos(evento.recursos)
    const recurso = props.seccion.toUpperCase() as RecursoSincronizacion
    if (evento.recursos.includes(recurso)) await cargarSeccion(props.seccion, false, true)
  }, estado => {
    estadoSincronizacion.value = estado
    enLinea.value = navigator.onLine
  })
}

function invalidarRecursos(recursos: RecursoSincronizacion[]) {
  if (recursos.includes('HOY')) {
    recursosCargados.base = false
    recursosCargados.catalogo = false
    recursosCargados.ocurrencias = false
  }
  if (recursos.includes('AGENDA')) {
    recursosCargados.base = false
    recursosCargados.catalogo = false
  }
  if (recursos.includes('SALUD')) {
    recursosCargados.base = false
    recursosCargados.catalogo = false
    recursosCargados.ocurrencias = false
    recursosCargados.cuota = false
  }
  if (recursos.includes('OBJETOS')) recursosCargados.objetos = false
}

watch(() => props.seccion, seccion => {
  mensaje.value = ''
  error.value = ''
  void cargarSeccion(seccion)
})

watch(busquedaObjetos, () => {
  if (props.seccion !== 'objetos') return
  if (temporizadorBusquedaObjetos) clearTimeout(temporizadorBusquedaObjetos)
  temporizadorBusquedaObjetos = setTimeout(() => void cargarObjetos(true, true).catch(causa => {
    error.value = causa instanceof Error ? causa.message : 'No se pudo completar la búsqueda.'
  }), 250)
})

function hora(tarea: TareaResumen) {
  return new Intl.DateTimeFormat('es-PE', {
    weekday: 'short', hour: '2-digit', minute: '2-digit', timeZone: datos.value?.zonaHoraria ?? 'America/Lima'
  }).format(new Date(tarea.fechaLimite))
}

function claveFecha(valor: string) {
  return new Intl.DateTimeFormat('en-CA', {
    year: 'numeric', month: '2-digit', day: '2-digit', timeZone: datos.value?.zonaHoraria ?? 'America/Lima'
  }).format(new Date(valor))
}

function abrirAlta(tipo: TipoAlta) {
  if (!enLinea.value) {
    error.value = 'Sin conexión. Vuelve a estar en línea para añadir o cambiar información.'
    return
  }
  const activo = document.activeElement instanceof HTMLElement ? document.activeElement : null
  const activador = activo?.closest('.menu-desplegable__panel')
    ? document.querySelector<HTMLElement>('.boton-anadir')
    : activo
  cerrarRecetaVisible()
  if (tipo === 'evento') {
    formulario.value = null
    void abrirFormularioEvento(activador)
    return
  }
  activadorFormulario = activador
  if (tipo === 'objeto') {
    objetoEditadoId.value = null
    Object.assign(nuevoObjeto, { nombre: '', categoria: '', notas: '', ruta: '', version: undefined })
  }
  formulario.value = tipo === 'medicamento' ? 'medicamento' : tipo
}

function cerrarFormulario() {
  formulario.value = null
}

async function entrar() {
  cargando.value = true
  error.value = ''
  try {
    const sesion = await iniciarSesion(correo.value, clave.value)
    administradorPlataforma.value = sesion.rolPlataforma === 'ADMINISTRADOR_PLATAFORMA'
    sesionActiva.value = true
    await cargarSeccion(props.seccion, true)
    iniciarSincronizacion()
    clave.value = ''
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo iniciar sesión'
  } finally {
    cargando.value = false
  }
}

async function cargarBase(forzar = false) {
  if (recursosCargados.base && !forzar) return
  datos.value = await consultarHoy()
  recursosCargados.base = true
  if (!nuevaTarea.perfilId && datos.value.perfiles.length) {
    nuevaTarea.perfilId = datos.value.perfiles[0].id
  }
  if (!nuevoTratamiento.perfilId && datos.value.perfiles.length) nuevoTratamiento.perfilId = datos.value.perfiles[0].id
}

async function cargarCatalogo(forzar = false) {
  if (recursosCargados.catalogo && !forzar) return
  catalogo.value = await consultarCatalogo()
  recursosCargados.catalogo = true
}

async function cargarOcurrencias(forzar = false) {
  if (recursosCargados.ocurrencias && !forzar) return
  agendaTratamientos.value = await consultarOcurrencias()
  recursosCargados.ocurrencias = true
}

async function cargarAuditoria(forzar = false) {
  if (recursosCargados.auditoria && !forzar) return
  auditoria.value = await consultarAuditoria()
  recursosCargados.auditoria = true
}

async function cargarFamilia(forzar = false) {
  if (recursosCargados.familia && !forzar) return
  familia.value = await consultarConfiguracionFamilia()
  recursosCargados.familia = true
}

async function cargarCuota(forzar = false) {
  if (recursosCargados.cuota && !forzar) return
  cuota.value = await consultarCuota()
  recursosCargados.cuota = true
}

async function cargarObjetos(forzar = false, buscar = false) {
  if (recursosCargados.objetos && !forzar && !buscar) return
  objetos.value = await consultarObjetos(busquedaObjetos.value)
  recursosCargados.objetos = true
}

async function cargarSeccion(seccion: SeccionPrincipal, forzar = false, enSegundoPlano = false) {
  if (!enSegundoPlano) {
    estadoVista.value = 'cargando'
    errorVista.value = ''
  }
  try {
    if (seccion === 'hoy') await Promise.all([cargarBase(forzar), cargarCatalogo(forzar), cargarOcurrencias(forzar)])
    if (seccion === 'agenda') await Promise.all([cargarBase(forzar), cargarCatalogo(forzar)])
    if (seccion === 'salud') await Promise.all([cargarBase(forzar), cargarCatalogo(forzar), cargarOcurrencias(forzar), cargarCuota(forzar)])
    if (seccion === 'objetos') await Promise.all([cargarBase(forzar), cargarObjetos(forzar)])
    if (seccion === 'familia') await Promise.all([cargarBase(forzar), cargarFamilia(forzar)])
    if (seccion === 'actividad') await Promise.all([cargarBase(forzar), cargarAuditoria(forzar)])
    if (!enSegundoPlano) estadoVista.value = 'lista'
  } catch (causa) {
    if (enSegundoPlano) {
      error.value = 'No se pudieron recuperar los últimos cambios. La aplicación volverá a intentarlo.'
    } else {
      errorVista.value = causa instanceof Error ? causa.message : 'No se pudo cargar esta sección.'
      estadoVista.value = 'error'
    }
  }
}

async function marcarHecho(tarea: TareaResumen) {
  error.value = ''
  try {
    await completarTarea(tarea.id)
    mensaje.value = `${tarea.titulo} quedó marcado como hecho.`
    await cargarBase(true)
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo completar la tarea'
  }
}

async function guardarTarea() {
  cargando.value = true
  error.value = ''
  try {
    await crearTarea({
      titulo: nuevaTarea.titulo, descripcion: nuevaTarea.descripcion, perfilId: nuevaTarea.perfilId,
      fechaLimite: new Date(nuevaTarea.fechaLimite).toISOString(),
      recurrencia: nuevaTarea.repetir ? { frecuencia: nuevaTarea.frecuencia as 'DIARIA' | 'SEMANAL' | 'MENSUAL', intervalo: nuevaTarea.intervalo, hasta: new Date(nuevaTarea.hasta).toISOString() } : undefined
    })
    nuevaTarea.titulo = ''
    nuevaTarea.descripcion = ''
    nuevaTarea.fechaLimite = ''
    nuevaTarea.repetir = false
    nuevaTarea.hasta = ''
    formulario.value = null
    mensaje.value = 'La tarea fue agregada.'
    await cargarBase(true)
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo guardar la tarea'
  } finally {
    cargando.value = false
  }
}

async function guardarMedicamento() {
  await ejecutarGuardado(async () => {
    await crearMedicamento({
      ...nuevoMedicamento,
      fechaVencimiento: nuevoMedicamento.fechaVencimiento || undefined
    })
    Object.assign(nuevoMedicamento, { nombre: '', presentacion: '', concentracion: '', cantidad: 1, unidad: 'unidad', fechaVencimiento: '' })
  }, 'El medicamento fue agregado.', () => cargarCatalogo(true))
}

async function guardarTratamiento() {
  let avisoReceta = ''
  await ejecutarGuardado(async () => {
    const creado = await crearTratamiento({
      perfilId: nuevoTratamiento.perfilId,
      medicamentoId: nuevoTratamiento.medicamentoId || undefined,
      nombre: nuevoTratamiento.nombre,
      indicacion: nuevoTratamiento.indicacion || undefined,
      cantidadReceta: nuevoTratamiento.cantidadReceta || undefined,
      frecuencia: nuevoTratamiento.frecuencia || undefined,
      horario: nuevoTratamiento.horario,
      horarios: nuevoTratamiento.horariosAdicionales.split(',').map(horario => horario.trim()).filter(Boolean),
      intervaloHoras: nuevoTratamiento.intervaloHoras ? Number(nuevoTratamiento.intervaloHoras) : undefined,
      fechaInicio: nuevoTratamiento.fechaInicio || undefined,
      fechaFin: nuevoTratamiento.fechaFin || undefined,
      responsablePerfilId: nuevoTratamiento.responsablePerfilId || undefined,
      responsableAlternativoPerfilId: nuevoTratamiento.responsableAlternativoPerfilId || undefined
    })
    if (recetaSeleccionada.value) {
      try {
        await subirReceta(creado.id, await reducirImagenReceta(recetaSeleccionada.value))
      } catch (causa) {
        avisoReceta = `El tratamiento fue creado, pero la receta no se guardó: ${causa instanceof Error ? causa.message : 'error desconocido'}`
      }
    }
    Object.assign(nuevoTratamiento, { perfilId: nuevoTratamiento.perfilId, medicamentoId: '', nombre: '', indicacion: '', cantidadReceta: '', frecuencia: '', horario: '', horariosAdicionales: '', intervaloHoras: '', fechaInicio: '', fechaFin: '', responsablePerfilId: '', responsableAlternativoPerfilId: '' })
    recetaSeleccionada.value = null
  }, 'El tratamiento fue agregado.', () => Promise.all([cargarCatalogo(true), cargarOcurrencias(true), cargarCuota(true)]).then(() => undefined))
  if (avisoReceta) mensaje.value = avisoReceta
}

function seleccionarReceta(evento: Event) {
  const entrada = evento.target as HTMLInputElement
  const archivo = entrada.files?.[0] ?? null
  if (!archivo) {
    recetaSeleccionada.value = null
    return
  }
  try {
    validarImagenReceta(archivo)
    recetaSeleccionada.value = archivo
    error.value = ''
  } catch (causa) {
    entrada.value = ''
    recetaSeleccionada.value = null
    error.value = causa instanceof Error ? causa.message : 'La fotografía no es válida'
  }
}

async function verReceta(archivoId: string) {
  cargando.value = true
  error.value = ''
  try {
    formulario.value = null
    cerrarRecetaVisible()
    const blob = await descargarReceta(archivoId)
    recetaVisible.value = { id: archivoId, url: URL.createObjectURL(blob) }
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo abrir la receta'
  } finally {
    cargando.value = false
  }
}

async function agregarReceta(evento: Event, tratamientoId: string) {
  const entrada = evento.target as HTMLInputElement
  const archivo = entrada.files?.[0]
  if (!archivo) return
  cargando.value = true
  error.value = ''
  try {
    validarImagenReceta(archivo)
    await subirReceta(tratamientoId, await reducirImagenReceta(archivo))
    mensaje.value = 'La fotografía de receta fue guardada de forma privada.'
    await Promise.all([cargarCatalogo(true), cargarCuota(true)])
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo guardar la receta'
  } finally {
    entrada.value = ''
    cargando.value = false
  }
}

function cerrarRecetaVisible() {
  if (recetaVisible.value) URL.revokeObjectURL(recetaVisible.value.url)
  recetaVisible.value = null
}

async function borrarReceta(archivoId: string) {
  if (!window.confirm('¿Eliminar completamente esta fotografía de receta?')) return
  cargando.value = true
  error.value = ''
  try {
    cerrarRecetaVisible()
    await eliminarReceta(archivoId)
    mensaje.value = 'La fotografía de receta fue eliminada.'
    await Promise.all([cargarCatalogo(true), cargarCuota(true)])
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo eliminar la receta'
  } finally {
    cargando.value = false
  }
}

async function eventoGuardado() {
  modificadoEvento.value = false
  await cerrarFormularioEvento(true)
  mensaje.value = 'El evento fue agregado.'
  await cargarCatalogo(true)
}

async function resolverOcurrencia(ocurrencia: OcurrenciaResumen, estado: Exclude<EstadoOcurrencia, 'PENDIENTE'>) {
  let pospuestaA: string | undefined
  if (estado === 'POSPUESTA') {
    pospuestaA = new Date(Date.now() + 30 * 60 * 1000).toISOString()
  }
  if (estado === 'REPROGRAMADA') {
    const valor = window.prompt('Nueva fecha y hora (AAAA-MM-DD HH:mm)')
    if (!valor) return
    const fecha = new Date(valor.replace(' ', 'T'))
    if (Number.isNaN(fecha.getTime())) {
      error.value = 'La nueva fecha y hora no es válida.'
      return
    }
    pospuestaA = fecha.toISOString()
  }
  cargando.value = true
  error.value = ''
  try {
    await cambiarEstadoOcurrencia(ocurrencia.id, estado, pospuestaA)
    mensaje.value = `${ocurrencia.tratamiento} quedó ${estado.toLowerCase()}.`
    await cargarOcurrencias(true)
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo resolver la ocurrencia'
  } finally {
    cargando.value = false
  }
}

function accionOcurrencia(ocurrencia: OcurrenciaResumen, accion: string) {
  const estados: Record<string, Exclude<EstadoOcurrencia, 'PENDIENTE'>> = {
    omitir: 'OMITIDA', posponer: 'POSPUESTA', reprogramar: 'REPROGRAMADA', cancelar: 'CANCELADA'
  }
  if (estados[accion]) void resolverOcurrencia(ocurrencia, estados[accion])
}

async function cerrarTratamientoActivo(tratamiento: RespuestaCatalogo['tratamientos'][number]) {
  const motivo = window.prompt('Motivo del cierre (opcional)')
  if (motivo === null) return
  cargando.value = true
  error.value = ''
  try {
    await cerrarTratamiento(tratamiento.id, motivo)
    mensaje.value = `${tratamiento.medicamento} quedó cerrado.`
    await Promise.all([cargarCatalogo(true), cargarOcurrencias(true)])
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo cerrar el tratamiento'
  } finally {
    cargando.value = false
  }
}

function accionTratamiento(tratamiento: RespuestaCatalogo['tratamientos'][number], accion: string) {
  if (accion === 'eliminar-receta' && tratamiento.recetaId) void borrarReceta(tratamiento.recetaId)
  if (accion === 'cerrar') void cerrarTratamientoActivo(tratamiento)
}

async function resolverDesdeRevision(elemento: ElementoRevision, estado?: Exclude<EstadoOcurrencia, 'PENDIENTE'>) {
  if (elemento.origen === 'OCURRENCIA' && estado) {
    const ocurrencia = agendaTratamientos.value?.ocurrencias.find(item => item.id === elemento.entidadId)
    if (ocurrencia) await resolverOcurrencia(ocurrencia, estado)
    return
  }
  cargando.value = true
  error.value = ''
  try {
    await cerrarElementoRevision(elemento.id)
    mensaje.value = `${elemento.titulo} quedó cerrado.`
    await cargarOcurrencias(true)
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo cerrar el elemento'
  } finally {
    cargando.value = false
  }
}

function accionRevision(elemento: ElementoRevision, accion: string) {
  const estados: Record<string, Exclude<EstadoOcurrencia, 'PENDIENTE'>> = {
    omitir: 'OMITIDA', posponer: 'POSPUESTA', reprogramar: 'REPROGRAMADA', cancelar: 'CANCELADA'
  }
  if (estados[accion]) void resolverDesdeRevision(elemento, estados[accion])
}

async function resolverAgenda(entidad: 'tareas' | 'eventos', elemento: { id: string; titulo: string }, accion: 'OMITIR' | 'REPROGRAMAR') {
  let fechaNueva: string | undefined
  if (accion === 'REPROGRAMAR') {
    const valor = window.prompt('Nueva fecha y hora (AAAA-MM-DD HH:mm)')
    if (!valor) return
    const fecha = new Date(valor.replace(' ', 'T'))
    if (Number.isNaN(fecha.getTime()) || fecha <= new Date()) {
      error.value = 'La nueva fecha y hora debe ser válida y futura.'
      return
    }
    fechaNueva = fecha.toISOString()
  }
  cargando.value = true
  error.value = ''
  try {
    await actuarAgenda(entidad, elemento.id, accion, fechaNueva)
    mensaje.value = `${elemento.titulo} quedó ${accion === 'OMITIR' ? 'omitido' : 'reprogramado'} sin borrar su historial.`
    if (entidad === 'tareas') await cargarBase(true)
    else await cargarCatalogo(true)
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo actualizar la agenda'
  } finally {
    cargando.value = false
  }
}

function accionAgenda(entidad: 'tareas' | 'eventos', elemento: { id: string; titulo: string }, accion: string) {
  if (accion === 'omitir') void resolverAgenda(entidad, elemento, 'OMITIR')
  if (accion === 'reprogramar') void resolverAgenda(entidad, elemento, 'REPROGRAMAR')
}

function abrirPerfil(perfil?: RespuestaFamilia['perfiles'][number]) {
  perfilEditadoId.value = perfil?.id ?? null
  Object.assign(nuevoPerfil, {
    nombre: perfil?.nombre ?? '', tipo: perfil?.tipo ?? 'DEPENDIENTE', color: perfil?.color ?? '#315b4c',
    relacion: perfil?.relacion ?? '', usuarioId: perfil?.usuarioId ?? '', permiso: perfil?.permiso ?? 'ADULTO',
    activo: perfil?.activo ?? true
  })
  formulario.value = 'perfil'
}

async function guardarPerfil() {
  await ejecutarGuardado(async () => {
    const datosPerfil = {
      nombre: nuevoPerfil.nombre, tipo: nuevoPerfil.tipo, color: nuevoPerfil.color || undefined,
      relacion: nuevoPerfil.relacion || undefined,
      usuarioId: nuevoPerfil.tipo === 'ADULTO' && nuevoPerfil.usuarioId ? nuevoPerfil.usuarioId : undefined,
      permiso: nuevoPerfil.tipo === 'ADULTO' && nuevoPerfil.usuarioId ? nuevoPerfil.permiso : undefined,
      activo: nuevoPerfil.activo
    }
    if (perfilEditadoId.value) await actualizarPerfil(perfilEditadoId.value, datosPerfil)
    else await crearPerfil(datosPerfil)
    perfilEditadoId.value = null
  }, 'La configuración familiar fue actualizada.', () => Promise.all([cargarFamilia(true), cargarBase(true)]).then(() => undefined))
}

function editarObjeto(objeto: ObjetoFamiliar) {
  objetoEditadoId.value = objeto.id
  Object.assign(nuevoObjeto, {
    nombre: objeto.nombre,
    categoria: objeto.categoria,
    notas: objeto.notas ?? '',
    ruta: objeto.ruta.join(' › '),
    version: objeto.version
  })
  formulario.value = 'objeto'
}

async function guardarObjeto() {
  const ruta = nuevoObjeto.ruta.split(/[›>]/).map(segmento => segmento.trim()).filter(Boolean)
  if (!ruta.length) {
    error.value = 'Indica al menos un lugar para guardar el objeto.'
    return
  }
  await ejecutarGuardado(async () => {
    const datosObjeto = { nombre: nuevoObjeto.nombre, categoria: nuevoObjeto.categoria, notas: nuevoObjeto.notas || undefined, ruta, version: nuevoObjeto.version }
    if (objetoEditadoId.value) await actualizarObjeto(objetoEditadoId.value, datosObjeto)
    else await crearObjeto(datosObjeto)
    Object.assign(nuevoObjeto, { nombre: '', categoria: '', notas: '', ruta: '', version: undefined })
    objetoEditadoId.value = null
  }, objetoEditadoId.value ? 'El objeto fue actualizado.' : 'El objeto fue guardado.', () => cargarObjetos(true))
}

async function ejecutarGuardado(accion: () => Promise<void>, confirmacion: string, recargar: () => Promise<void>) {
  cargando.value = true
  error.value = ''
  try {
    await accion()
    formulario.value = null
    mensaje.value = confirmacion
    await recargar()
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo guardar la información'
  } finally {
    cargando.value = false
  }
}

async function salir() {
  error.value = ''
  try {
    await eliminarSesion()
    cerrarSincronizacion?.()
    cerrarSincronizacion = null
    datos.value = null
    catalogo.value = null
    agendaTratamientos.value = null
    auditoria.value = null
    familia.value = null
    cuota.value = null
    Object.keys(recursosCargados).forEach(recurso => { recursosCargados[recurso as keyof typeof recursosCargados] = false })
    cerrarRecetaVisible()
    sesionActiva.value = false
    administradorPlataforma.value = false
    mensaje.value = ''
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo cerrar la sesión'
  }
}
</script>

<template>
  <main v-if="restaurando" class="acceso">
    <section class="panel-acceso panel-acceso--cargando" aria-live="polite">
      <img src="/icono.svg" alt="" width="72" height="72" />
      <p>Recuperando tu sesión segura…</p>
    </section>
  </main>

  <main v-else-if="!sesionActiva" class="acceso">
    <section class="panel-acceso">
      <img src="/icono.svg" alt="" width="72" height="72" />
      <p class="sobretitulo">OBU System</p>
      <h1>Agenda Familiar</h1>
      <p>Ingresa para ver únicamente la información de tu familia.</p>
      <form @submit.prevent="entrar">
        <label>Correo<input v-model.trim="correo" type="email" autocomplete="username" required /></label>
        <label>Clave<input v-model="clave" type="password" autocomplete="current-password" minlength="12" required /></label>
        <p v-if="error" class="error" role="alert">{{ error }}</p>
        <button class="boton-principal" :disabled="cargando">{{ cargando ? 'Ingresando…' : 'Ingresar' }}</button>
      </form>
      <small>Las notificaciones bloqueadas nunca muestran información médica.</small>
    </section>
  </main>

  <div v-else>
    <AppShell
      :titulo="tituloPantalla"
      :subtitulo="subtituloPantalla"
      :familia="datos?.familia"
      :cantidad-atencion="cantidadRevision"
      :etiqueta-anadir="etiquetaAnadirCabecera"
      :tipo-anadir-directo="tipoAnadirCabecera"
      :mostrar-anadir="mostrarAnadirCabecera"
      :administrador-plataforma="administradorPlataforma"
      @anadir="abrirAlta"
      @salir="salir"
    >
      <p v-if="!enLinea" class="aviso-conexion" role="status">
        <span aria-hidden="true"></span>
        Sin conexión: puedes consultar lo ya cargado. Para proteger los cambios, guardar estará disponible al volver a estar en línea.
      </p>
      <p v-else-if="estadoSincronizacion === 'reconectando'" class="aviso-conexion aviso-conexion--sincronizando" role="status">
        <span aria-hidden="true"></span>
        Recuperando los cambios de tu familia…
      </p>
      <p v-if="estadoVista === 'cargando'" class="estado-carga" role="status">Cargando {{ tituloPantalla.toLowerCase() }}…</p>
      <div v-else-if="estadoVista === 'error'" class="estado-error" role="alert">
        <p>{{ errorVista }}</p>
        <button type="button" class="boton-secundario" @click="cargarSeccion(seccion, true)">Reintentar</button>
      </div>
      <template v-else>
      <section v-if="seccion !== 'familia' && seccion !== 'actividad' && seccion !== 'objetos'" class="selector-persona" aria-label="Filtrar agenda por persona">
        <label for="filtro-persona">Ver agenda de</label>
        <select id="filtro-persona" v-model="filtroPerfil">
          <option value="TODOS">Todos</option>
          <option v-for="perfil in datos?.perfiles" :key="perfil.id" :value="perfil.id">{{ perfil.nombre }}</option>
        </select>
      </section>

      <nav v-if="seccion === 'salud'" class="subnavegacion" aria-label="Secciones de Salud">
        <RouterLink :to="destinoSalud('hoy')" :aria-current="seccionSalud === 'hoy' ? 'page' : undefined">Tomas</RouterLink>
        <RouterLink :to="destinoSalud('tratamientos')" :aria-current="seccionSalud === 'tratamientos' ? 'page' : undefined">Tratamientos</RouterLink>
        <RouterLink :to="destinoSalud('botiquin')" :aria-current="seccionSalud === 'botiquin' ? 'page' : undefined">Botiquín</RouterLink>
        <RouterLink :to="destinoSalud('recetas')" :aria-current="seccionSalud === 'recetas' ? 'page' : undefined">Recetas</RouterLink>
      </nav>

      <section v-if="seccion === 'hoy' && hayContenidoHoy" class="resumen" aria-label="Resumen del día">
        <div v-if="tareasHoy.length"><strong>{{ tareasHoy.length }}</strong><span>{{ tareasHoy.length === 1 ? 'tarea' : 'tareas' }}</span></div>
        <div v-if="ocurrenciasPendientes.length"><strong>{{ ocurrenciasPendientes.length }}</strong><span>{{ ocurrenciasPendientes.length === 1 ? 'toma pendiente' : 'tomas pendientes' }}</span></div>
        <div v-if="cantidadRevision"><strong>{{ cantidadRevision }}</strong><span>necesita atención</span></div>
      </section>

      <section v-if="seccion === 'hoy' && !hayContenidoHoy" class="estado-dia-libre" aria-labelledby="titulo-dia-libre">
        <span class="estado-dia-libre__marca" aria-hidden="true">✓</span>
        <div><h2 id="titulo-dia-libre">Todo está al día</h2><p>No hay tareas, tomas ni elementos que necesiten atención.</p></div>
        <RouterLink :to="{ name: 'agenda' }">Ver lo que viene</RouterLink>
      </section>

      <p v-if="mensaje" class="confirmacion" role="status">{{ mensaje }}</p>
      <p v-if="error" class="error" role="alert">{{ error }}</p>

      <section v-if="(seccion === 'hoy' && tareasHoy.length) || (seccion === 'agenda' && proximas.length)" class="seccion">
        <div class="titulo-seccion">
          <div><span class="etiqueta etiqueta--verde">{{ seccion === 'hoy' ? 'Hoy' : 'En orden' }}</span><h2>{{ seccion === 'hoy' ? 'Tareas de hoy' : 'Próximos siete días' }}</h2></div>
        </div>
        <TarjetaPendiente
          v-for="tarea in (seccion === 'hoy' ? tareasHoy : proximas)" :key="tarea.id" :hora="hora(tarea)" :titulo="tarea.titulo"
          :detalle="`${tarea.responsable} · ${tarea.descripcion ?? 'Sin detalles'}`" tono="proximo"
          :recurrente="tarea.recurrente" @completar="marcarHecho(tarea)"
          @omitir="resolverAgenda('tareas', tarea, 'OMITIR')" @reprogramar="resolverAgenda('tareas', tarea, 'REPROGRAMAR')"
        />
      </section>

      <section v-if="(seccion === 'hoy' && ocurrenciasPendientes.length) || (seccion === 'salud' && seccionSalud === 'hoy' && !mostrandoHistorialTomas)" id="ocurrencias" class="seccion">
        <div class="titulo-seccion"><div><h2>{{ seccion === 'salud' ? 'Tomas' : 'Tomas del día' }}</h2></div></div>
        <article v-for="ocurrencia in (seccion === 'salud' ? ocurrenciasVisibles : ocurrenciasPendientes)" :key="ocurrencia.id" class="tarjeta tarjeta--proximo">
          <time>{{ new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: datos?.zonaHoraria }).format(new Date(ocurrencia.programadaEn)) }}</time>
          <div class="tarjeta__contenido"><h3>{{ ocurrencia.tratamiento }}</h3><p>Para {{ ocurrencia.persona }}</p></div>
          <div class="acciones-ocurrencia">
            <button type="button" class="boton-accion" :disabled="cargando" @click="resolverOcurrencia(ocurrencia, 'TOMADA')">Tomada</button>
            <MenuMas
              :acciones="[{ id: 'omitir', etiqueta: 'Omitir' }, { id: 'posponer', etiqueta: 'Posponer 30 min' }, { id: 'reprogramar', etiqueta: 'Reprogramar' }, { id: 'cancelar', etiqueta: 'Cancelar', peligrosa: true }]"
              :etiqueta="`Más acciones para ${ocurrencia.tratamiento}`"
              @seleccionar="accionOcurrencia(ocurrencia, $event)"
            />
          </div>
        </article>
        <p v-if="!ocurrenciasPendientes.length" class="estado-vacio">No hay tomas pendientes para este filtro.</p>
        <button v-if="seccion === 'salud' && ocurrenciasPendientes.length > 5" type="button" class="boton-ver-mas" @click="mostrarTodoSalud.hoy = !mostrarTodoSalud.hoy">
          {{ mostrarTodoSalud.hoy ? 'Mostrar menos' : `Ver ${ocurrenciasPendientes.length - 5} más` }}
        </button>
        <RouterLink v-if="seccion === 'salud' && historialOcurrencias.length" class="enlace-historial" :to="destinoHistorialTomas()">Ver historial</RouterLink>
      </section>

      <section v-if="seccion === 'salud' && mostrandoHistorialTomas" class="seccion historial-tomas" aria-labelledby="titulo-historial-tomas">
        <RouterLink class="enlace-volver" :to="destinoSalud('hoy')">← Volver a Tomas</RouterLink>
        <div class="titulo-seccion"><div><span class="etiqueta etiqueta--verde">Registro familiar</span><h2 id="titulo-historial-tomas">Historial de tomas</h2></div></div>
        <article v-for="ocurrencia in historialTomasVisible" :key="`historial-${ocurrencia.id}`" class="tarjeta">
          <time>{{ new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: datos?.zonaHoraria }).format(new Date(ocurrencia.resueltaEn || ocurrencia.programadaEn)) }}</time>
          <div class="tarjeta__contenido"><h3>{{ ocurrencia.tratamiento }}</h3><p>{{ ocurrencia.persona }} · {{ ocurrencia.resueltaPorNombre || 'Adulto autorizado' }}</p></div>
          <span class="estado">{{ ocurrencia.estado.replace('_', ' ') }}</span>
        </article>
        <p v-if="!historialOcurrencias.length" class="estado-vacio">Todavía no hay tomas registradas.</p>
        <button v-if="historialOcurrencias.length > limiteHistorialTomas" type="button" class="boton-ver-mas" @click="limiteHistorialTomas += 10">Ver {{ Math.min(10, historialOcurrencias.length - limiteHistorialTomas) }} más</button>
      </section>

      <section v-if="seccion === 'hoy' && cantidadRevision" id="revisar" class="seccion seccion--alerta">
        <div class="titulo-seccion"><div><span class="etiqueta">Necesita atención</span><h2>Por resolver</h2></div></div>
        <TarjetaPendiente v-for="tarea in atrasadas" :key="`revisar-${tarea.id}`" :hora="hora(tarea)"
          :titulo="tarea.titulo" :detalle="`${tarea.responsable} · Tarea vencida`" tono="atrasado"
          :recurrente="tarea.recurrente" @completar="marcarHecho(tarea)"
          @omitir="resolverAgenda('tareas', tarea, 'OMITIR')" @reprogramar="resolverAgenda('tareas', tarea, 'REPROGRAMAR')" />
        <article v-for="evento in eventosAtrasados" :key="`revisar-${evento.id}`" class="tarjeta tarjeta--atrasado">
          <time>{{ new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: datos?.zonaHoraria }).format(new Date(evento.inicioEn)) }}</time>
          <div class="tarjeta__contenido"><h3>{{ evento.titulo }}</h3><p>Evento pendiente de revisión</p><small v-if="evento.recurrente">Recurrente</small></div>
          <MenuMas :acciones="[{ id: 'omitir', etiqueta: 'Omitir' }, { id: 'reprogramar', etiqueta: 'Reprogramar' }]" :etiqueta="`Más acciones para ${evento.titulo}`" @seleccionar="accionAgenda('eventos', evento, $event)" />
        </article>
        <article v-for="elemento in elementosRevision" :key="elemento.id" class="tarjeta tarjeta--atrasado">
          <time v-if="elemento.fecha">{{ new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: datos?.zonaHoraria }).format(new Date(elemento.fecha)) }}</time>
          <div class="tarjeta__contenido"><h3>{{ elemento.titulo }}</h3><p>{{ elemento.motivo.replaceAll('_', ' ').toLowerCase() }}</p></div>
          <div v-if="elemento.origen === 'OCURRENCIA'" class="acciones-ocurrencia">
            <button type="button" class="boton-accion" @click="resolverDesdeRevision(elemento, 'TOMADA')">Tomada</button>
            <MenuMas
              :acciones="[{ id: 'omitir', etiqueta: 'Omitir' }, { id: 'posponer', etiqueta: 'Posponer 30 min' }, { id: 'reprogramar', etiqueta: 'Reprogramar' }, { id: 'cancelar', etiqueta: 'Cancelar', peligrosa: true }]"
              :etiqueta="`Más acciones para ${elemento.titulo}`"
              @seleccionar="accionRevision(elemento, $event)"
            />
          </div>
          <button v-else type="button" class="boton-accion" @click="resolverDesdeRevision(elemento)">Cerrar</button>
        </article>
      </section>

      <section v-if="seccion === 'agenda'" id="calendario" class="seccion">
        <div class="titulo-seccion"><div><span class="etiqueta etiqueta--arena">Calendario</span><h2>Próximos eventos</h2></div></div>
        <article v-for="evento in eventosFiltrados" :key="evento.id" class="tarjeta tarjeta--proximo">
          <time>{{ new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: datos?.zonaHoraria }).format(new Date(evento.inicioEn)) }}</time>
          <div class="tarjeta__contenido"><h3>{{ evento.titulo }}</h3><p>{{ [evento.persona, evento.lugar].filter(Boolean).join(' · ') || 'Sin persona ni lugar asignados' }}</p><small v-if="evento.recurrente">Recurrente</small></div>
          <MenuMas :acciones="[{ id: 'omitir', etiqueta: 'Omitir' }, { id: 'reprogramar', etiqueta: 'Reprogramar' }]" :etiqueta="`Más acciones para ${evento.titulo}`" @seleccionar="accionAgenda('eventos', evento, $event)" />
        </article>
      </section>
      <p v-if="seccion === 'agenda' && !proximas.length && !eventosFiltrados.length" class="estado-vacio estado-vacio--centrado">No hay tareas ni eventos próximos.</p>

      <section v-if="seccion === 'salud' && seccionSalud === 'tratamientos'" id="tratamientos" class="seccion">
        <div class="titulo-seccion"><div><span class="etiqueta etiqueta--verde">Cuidado</span><h2>Tratamientos</h2></div></div>
        <article v-for="tratamiento in tratamientosVisibles" :key="tratamiento.id" class="tarjeta">
          <div class="tarjeta__contenido">
            <div class="tarjeta__encabezado"><h3>{{ tratamiento.medicamento }}</h3><span class="estado">{{ tratamiento.estado }}</span></div>
            <p>{{ tratamiento.persona }} · {{ tratamiento.intervaloHoras ? `cada ${tratamiento.intervaloHoras} h` : tratamiento.horarios.map(horario => horario.slice(0, 5)).join(' · ') }}</p>
            <details class="detalles-fila">
              <summary>Ver detalles</summary>
              <p>Responsable: {{ tratamiento.responsable }}<span v-if="tratamiento.responsableAlternativo"> · alternativo: {{ tratamiento.responsableAlternativo }}</span></p>
              <p v-if="tratamiento.dosisIndicada || tratamiento.frecuencia">{{ [tratamiento.dosisIndicada, tratamiento.frecuencia].filter(Boolean).join(' · ') }}</p>
              <small v-if="tratamiento.indicacion">{{ tratamiento.indicacion }}</small>
              <div class="detalles-fila__acciones">
                <label v-if="!tratamiento.recetaId" class="boton-accion boton-archivo">Agregar receta<input type="file" accept="image/jpeg,image/png" capture="environment" :disabled="cargando" @change="agregarReceta($event, tratamiento.id)" /></label>
                <button v-if="tratamiento.recetaId" type="button" class="boton-accion" :disabled="cargando" @click="verReceta(tratamiento.recetaId)">Ver receta</button>
              </div>
            </details>
          </div>
          <MenuMas v-if="tratamiento.recetaId || tratamiento.estado === 'ACTIVO'" :acciones="[{ id: 'eliminar-receta', etiqueta: 'Eliminar receta', peligrosa: true }, { id: 'cerrar', etiqueta: 'Finalizar tratamiento', peligrosa: true }].filter(accion => (accion.id !== 'eliminar-receta' || tratamiento.recetaId) && (accion.id !== 'cerrar' || tratamiento.estado === 'ACTIVO'))" :etiqueta="`Más acciones para ${tratamiento.medicamento}`" @seleccionar="accionTratamiento(tratamiento, $event)" />
        </article>
        <p v-if="!tratamientosFiltrados.length" class="estado-vacio">No hay tratamientos para este filtro.</p>
        <button v-if="tratamientosFiltrados.length > 5" type="button" class="boton-ver-mas" @click="mostrarTodoSalud.tratamientos = !mostrarTodoSalud.tratamientos">
          {{ mostrarTodoSalud.tratamientos ? 'Mostrar menos' : `Ver ${tratamientosFiltrados.length - 5} más` }}
        </button>
        <p class="aviso-medico">La aplicación conserva el texto ingresado por la familia; no calcula ni recomienda dosis.</p>
      </section>

      <section v-if="seccion === 'salud' && seccionSalud === 'botiquin' && vencimientosCercanos.length" id="vencimientos" class="seccion seccion--alerta">
        <div class="titulo-seccion"><div><span class="etiqueta">Vencimientos</span><h2>Vencimientos cercanos</h2></div></div>
        <article v-for="medicamento in vencimientosCercanos" :key="`vence-${medicamento.loteId || medicamento.id}`" class="tarjeta tarjeta--atrasado">
          <time>{{ medicamento.fechaVencimiento || 'Sin fecha' }}</time>
          <div class="tarjeta__contenido"><h3>{{ medicamento.nombre }}</h3><p>{{ medicamento.cantidad }} {{ medicamento.unidad }}</p></div>
          <span class="estado">{{ medicamento.estado.replace('_', ' ') }}</span>
        </article>
      </section>

      <section v-if="seccion === 'salud' && seccionSalud === 'botiquin'" id="botiquin" class="seccion">
        <div class="titulo-seccion"><div><span class="etiqueta">Botiquín</span><h2>Medicamentos</h2></div></div>
        <article v-for="medicamento in medicamentosVisibles" :key="medicamento.loteId || medicamento.id" class="tarjeta">
          <div class="tarjeta__contenido"><h3>{{ medicamento.nombre }}</h3><p>{{ medicamento.presentacion }} · {{ medicamento.concentracion }}</p><small>{{ medicamento.cantidad }} {{ medicamento.unidad }} · vence {{ medicamento.fechaVencimiento || 'sin fecha' }}</small></div>
          <span class="estado">{{ medicamento.estado.replace('_', ' ') }}</span>
        </article>
        <p v-if="!catalogo?.medicamentos.length" class="estado-vacio">No hay medicamentos guardados en el botiquín.</p>
        <button v-if="(catalogo?.medicamentos.length ?? 0) > 5" type="button" class="boton-ver-mas" @click="mostrarTodoSalud.botiquin = !mostrarTodoSalud.botiquin">
          {{ mostrarTodoSalud.botiquin ? 'Mostrar menos' : `Ver ${(catalogo?.medicamentos.length ?? 0) - 5} más` }}
        </button>
      </section>

      <section v-if="seccion === 'salud' && seccionSalud === 'recetas'" id="recetas" class="seccion">
        <div class="titulo-seccion"><div><span class="etiqueta etiqueta--arena">Archivos privados</span><h2>Recetas</h2></div></div>
        <div v-if="cuota && cuota.porcentaje >= 70" class="cuota" :class="`cuota--${cuota.nivel.toLowerCase()}`" role="status">
          <div><strong>Espacio usado: {{ cuota.porcentaje }} %</strong><span>{{ (cuota.usadosBytes / 1048576).toFixed(1) }} MiB de {{ (cuota.cuotaBytes / 1073741824).toFixed(1) }} GiB</span></div>
          <progress :value="cuota.porcentaje" max="100">{{ cuota.porcentaje }} %</progress>
          <small v-if="cuota.porcentaje >= 70">El espacio disponible se está agotando. Elimina fotografías que ya no necesites.</small>
        </div>
        <article v-for="tratamiento in recetasVisibles" :key="`receta-${tratamiento.id}`" class="tarjeta">
          <div class="tarjeta__contenido"><h3>{{ tratamiento.medicamento }}</h3><p>{{ tratamiento.persona }} · receta privada</p></div>
          <button type="button" class="boton-accion" :disabled="cargando" @click="verReceta(tratamiento.recetaId!)">Ver receta</button>
        </article>
        <p v-if="!recetasVisibles.length" class="estado-vacio">Todavía no hay recetas guardadas. Puedes agregarlas desde Tratamientos.</p>
        <button v-if="tratamientosFiltrados.filter(tratamiento => tratamiento.recetaId).length > 5" type="button" class="boton-ver-mas" @click="mostrarTodoSalud.recetas = !mostrarTodoSalud.recetas">
          {{ mostrarTodoSalud.recetas ? 'Mostrar menos' : `Ver ${tratamientosFiltrados.filter(tratamiento => tratamiento.recetaId).length - 5} más` }}
        </button>
      </section>

      <section v-if="seccion === 'objetos'" class="objetos-vista" aria-labelledby="titulo-busqueda-objetos">
        <div class="buscador-objetos">
          <label id="titulo-busqueda-objetos" for="busqueda-objetos">¿Qué estás buscando?</label>
          <input id="busqueda-objetos" v-model.trim="busquedaObjetos" type="search" placeholder="Objeto, categoría o lugar" autocomplete="off" />
        </div>

        <section class="seccion" aria-labelledby="titulo-objetos">
          <div class="titulo-seccion titulo-seccion--objetos">
            <div><span class="etiqueta etiqueta--verde">{{ busquedaObjetos ? 'Resultados' : 'Actualizados recientemente' }}</span><h2 id="titulo-objetos">{{ busquedaObjetos ? 'Objetos encontrados' : 'Objetos de la familia' }}</h2></div>
          </div>
          <div v-if="objetos?.objetos.length" class="lista-objetos">
            <article v-for="objeto in objetos.objetos" :key="objeto.id" class="fila-objeto">
              <span class="fila-objeto__marca" aria-hidden="true"></span>
              <div><h3>{{ objeto.nombre }}</h3><p>{{ objeto.ruta.join(' › ') }}</p><small>{{ objeto.categoria }}<span v-if="objeto.notas"> · {{ objeto.notas }}</span></small></div>
              <button type="button" class="boton-secundario" :aria-label="`Editar ${objeto.nombre}`" @click="editarObjeto(objeto)">Editar</button>
            </article>
          </div>
          <p v-else class="estado-vacio">{{ busquedaObjetos ? 'No encontramos objetos con esa búsqueda.' : 'Todavía no hay objetos guardados. Añade el primero indicando dónde está.' }}</p>
        </section>

        <section v-if="objetos?.ubicaciones.length" class="seccion lugares-objetos" aria-labelledby="titulo-lugares-objetos">
          <div class="titulo-seccion"><div><span class="etiqueta etiqueta--arena">Explorar</span><h2 id="titulo-lugares-objetos">Lugares de la casa</h2></div></div>
          <div class="lugares-objetos__ruta">
            <p v-for="ubicacion in objetos.ubicaciones" :key="ubicacion.ruta.join('/')"><strong>{{ ubicacion.ruta.join(' › ') }}</strong><span>{{ ubicacion.cantidad }} {{ ubicacion.cantidad === 1 ? 'objeto' : 'objetos' }}</span></p>
          </div>
        </section>
      </section>

      <section v-if="seccion === 'actividad'" id="historial" class="seccion">
        <div class="titulo-seccion"><div><span class="etiqueta etiqueta--arena">Trazabilidad</span><h2>Historial familiar</h2></div></div>
        <article v-for="entrada in auditoria?.entradas.slice(0, 20)" :key="`${entrada.entidad}-${entrada.entidadId}-${entrada.fecha}`" class="tarjeta">
          <time>{{ new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: datos?.zonaHoraria }).format(new Date(entrada.fecha)) }}</time>
          <div class="tarjeta__contenido"><h3>{{ entrada.titulo }}</h3><p>{{ entrada.actor }} · {{ entrada.operacion.toLowerCase() }}</p><small>{{ entrada.resumen }}</small></div>
        </article>
        <p v-if="!auditoria?.entradas.length" class="estado-vacio">Todavía no hay cambios registrados.</p>
      </section>

      <section v-if="seccion === 'familia'" id="familia" class="seccion">
        <div class="titulo-seccion"><div><span class="etiqueta etiqueta--verde">Familia</span><h2>Perfiles y permisos</h2></div><button v-if="familia?.puedeAdministrar" type="button" class="boton-secundario" @click="abrirPerfil()">Agregar</button></div>
        <article v-for="perfil in familia?.perfiles" :key="perfil.id" class="tarjeta" :class="{ 'perfil-inactivo': !perfil.activo }">
          <span class="muestra-color" :style="{ backgroundColor: perfil.color || '#c8d2ce' }" aria-hidden="true"></span>
          <div class="tarjeta__contenido"><h3>{{ perfil.nombre }}</h3><p>{{ perfil.tipo === 'ADULTO' ? 'Adulto' : 'Dependiente' }} · {{ perfil.relacion || 'Sin relación indicada' }}</p><small>{{ perfil.permiso === 'ADMINISTRADOR_FAMILIAR' ? 'Administrador familiar' : perfil.usuarioId ? 'Adulto con acceso' : 'Sin cuenta vinculada' }}<span v-if="!perfil.activo"> · Inactivo</span></small></div>
          <button v-if="familia?.puedeAdministrar" type="button" class="boton-secundario" @click="abrirPerfil(perfil)">Editar</button>
        </article>
        <p v-if="!familia?.puedeAdministrar" class="estado-vacio">Solo un administrador familiar puede cambiar perfiles y permisos.</p>
        </section>
      </template>
    </AppShell>

    <FormularioEvento
      v-if="datos"
      ref="formularioEvento"
      :abierto="eventoAbierto"
      :perfiles="datos.perfiles"
      :zona-horaria="datos.zonaHoraria"
      @cerrar="cerrarFormularioEvento()"
      @guardado="eventoGuardado"
      @modificado="modificadoEvento = $event"
    />

    <dialog ref="dialogoFormulario" class="dialogo dialogo--formulario" aria-labelledby="titulo-dialogo-formulario" @cancel.prevent="cerrarFormulario">
      <div class="titulo-seccion dialogo__cabecera"><h2 id="titulo-dialogo-formulario">{{ tituloFormulario }}</h2><button type="button" class="cerrar" aria-label="Cerrar formulario" @click="cerrarFormulario">×</button></div>

      <form v-if="formulario === 'tarea'" id="formulario-general" @submit.prevent="guardarTarea">
        <label>Título<input v-model.trim="nuevaTarea.titulo" maxlength="180" required /></label>
        <label>Responsable
          <select v-model="nuevaTarea.perfilId" required>
            <option v-for="perfil in datos?.perfiles" :key="perfil.id" :value="perfil.id">{{ perfil.nombre }}</option>
          </select>
        </label>
        <label>Fecha y hora<input v-model="nuevaTarea.fechaLimite" type="datetime-local" required /></label>
        <label>Detalle<textarea v-model.trim="nuevaTarea.descripcion" maxlength="1000" rows="3" /></label>
        <label class="opcion-linea"><input v-model="nuevaTarea.repetir" type="checkbox" /> Repetir tarea</label>
        <div v-if="nuevaTarea.repetir" class="campos-dobles">
          <label>Frecuencia<select v-model="nuevaTarea.frecuencia"><option value="DIARIA">Diaria</option><option value="SEMANAL">Semanal</option><option value="MENSUAL">Mensual</option></select></label>
          <label>Cada<input v-model.number="nuevaTarea.intervalo" type="number" min="1" max="30" required /></label>
          <label>Hasta<input v-model="nuevaTarea.hasta" type="datetime-local" required /></label>
        </div>
      </form>

      <form v-else-if="formulario === 'medicamento'" id="formulario-general" @submit.prevent="guardarMedicamento">
        <label>Nombre<input v-model.trim="nuevoMedicamento.nombre" maxlength="180" required /></label>
        <label>Presentación<input v-model.trim="nuevoMedicamento.presentacion" maxlength="120" placeholder="Caja, frasco…" /></label>
        <label>Concentración<input v-model.trim="nuevoMedicamento.concentracion" maxlength="120" placeholder="Texto del envase" /></label>
        <div class="campos-dobles"><label>Cantidad<input v-model.number="nuevoMedicamento.cantidad" type="number" min="0" step="0.01" required /></label><label>Unidad<input v-model.trim="nuevoMedicamento.unidad" maxlength="40" required /></label></div>
        <label>Vencimiento opcional<input v-model="nuevoMedicamento.fechaVencimiento" type="date" /></label>
      </form>

      <form v-else-if="formulario === 'tratamiento'" id="formulario-general" @submit.prevent="guardarTratamiento">
        <label>Persona<select v-model="nuevoTratamiento.perfilId" required><option v-for="perfil in datos?.perfiles" :key="perfil.id" :value="perfil.id">{{ perfil.nombre }}</option></select></label>
        <label>Nombre del tratamiento o medicamento<input v-model.trim="nuevoTratamiento.nombre" maxlength="180" required /></label>
        <label>Horario<input v-model="nuevoTratamiento.horario" type="time" required /></label>
        <details><summary>Más detalles opcionales</summary><div class="detalles-progresivos">
          <label>Vincular al botiquín<select v-model="nuevoTratamiento.medicamentoId"><option value="">Sin vincular</option><option v-for="medicamento in catalogo?.medicamentos" :key="medicamento.id" :value="medicamento.id">{{ medicamento.nombre }} · {{ medicamento.concentracion }}</option></select></label>
          <label>Indicación<textarea v-model.trim="nuevoTratamiento.indicacion" maxlength="1000" rows="2" /></label>
          <label>Cantidad indicada en la receta<input v-model.trim="nuevoTratamiento.cantidadReceta" maxlength="300" /></label>
          <label>Frecuencia o notas del horario<input v-model.trim="nuevoTratamiento.frecuencia" maxlength="300" /></label>
          <label>Horarios adicionales<input v-model.trim="nuevoTratamiento.horariosAdicionales" placeholder="14:00, 20:00" /></label>
          <label>Intervalo en horas<input v-model="nuevoTratamiento.intervaloHoras" type="number" min="1" max="168" placeholder="Usa solo un horario inicial" /></label>
          <label>Responsable opcional<select v-model="nuevoTratamiento.responsablePerfilId"><option value="">La misma persona</option><option v-for="perfil in datos?.perfiles" :key="perfil.id" :value="perfil.id">{{ perfil.nombre }}</option></select></label>
          <label>Responsable alternativo<select v-model="nuevoTratamiento.responsableAlternativoPerfilId"><option value="">Sin alternativo</option><option v-for="perfil in datos?.perfiles" :key="perfil.id" :value="perfil.id">{{ perfil.nombre }}</option></select></label>
          <label>Fotografía de receta opcional<input type="file" accept="image/jpeg,image/png" capture="environment" @change="seleccionarReceta" /><small>Se reduce y elimina ubicación/EXIF antes de guardarla cifrada.</small></label>
          <p v-if="recetaSeleccionada" class="confirmacion">{{ recetaSeleccionada.name }} · {{ (recetaSeleccionada.size / 1048576).toFixed(1) }} MiB</p>
          <div class="campos-dobles"><label>Inicio opcional<input v-model="nuevoTratamiento.fechaInicio" type="date" /></label><label>Fin opcional<input v-model="nuevoTratamiento.fechaFin" type="date" /></label></div>
        </div></details>
      </form>

      <form v-else-if="formulario === 'perfil'" id="formulario-general" @submit.prevent="guardarPerfil">
        <label>Nombre visible<input v-model.trim="nuevoPerfil.nombre" maxlength="120" required /></label>
        <label>Tipo<select v-model="nuevoPerfil.tipo"><option value="ADULTO">Adulto</option><option value="DEPENDIENTE">Dependiente</option></select></label>
        <div class="campos-dobles"><label>Relación<input v-model.trim="nuevoPerfil.relacion" maxlength="80" placeholder="Mamá, abuelo, hija…" /></label><label>Color<input v-model="nuevoPerfil.color" type="color" /></label></div>
        <template v-if="nuevoPerfil.tipo === 'ADULTO'">
          <label>ID de cuenta opcional<input v-model.trim="nuevoPerfil.usuarioId" type="text" placeholder="UUID de una cuenta existente" /></label>
          <label v-if="nuevoPerfil.usuarioId">Permiso<select v-model="nuevoPerfil.permiso"><option value="ADULTO">Adulto</option><option value="ADMINISTRADOR_FAMILIAR">Administrador familiar</option></select></label>
        </template>
        <label class="opcion-linea"><input v-model="nuevoPerfil.activo" type="checkbox" /> Perfil activo</label>
      </form>

      <form v-else-if="formulario === 'objeto'" id="formulario-general" @submit.prevent="guardarObjeto">
        <label>Nombre<input v-model.trim="nuevoObjeto.nombre" maxlength="180" required /></label>
        <label>Categoría<input v-model.trim="nuevoObjeto.categoria" maxlength="80" placeholder="Documentos, herramientas…" required /></label>
        <label>Ruta de ubicación<input v-model.trim="nuevoObjeto.ruta" maxlength="604" placeholder="Habitación principal › Ropero › Caja de documentos" required /><small>Separa cada nivel con ›.</small></label>
        <label>Nota opcional<textarea v-model.trim="nuevoObjeto.notas" maxlength="500" rows="3" placeholder="Un detalle que ayude a reconocerlo" /></label>
      </form>
      <footer class="dialogo__acciones">
        <button type="button" class="boton-secundario" :disabled="cargando" @click="cerrarFormulario">Cancelar</button>
        <button type="submit" form="formulario-general" class="boton-principal" :disabled="cargando || !enLinea">{{ etiquetaGuardarFormulario }}</button>
      </footer>
    </dialog>

    <dialog ref="dialogoReceta" class="dialogo dialogo--receta" aria-labelledby="titulo-dialogo-receta" @cancel.prevent="cerrarRecetaVisible">
      <div class="titulo-seccion dialogo__cabecera"><h2 id="titulo-dialogo-receta">Receta privada</h2><button type="button" class="cerrar" aria-label="Cerrar receta" @click="cerrarRecetaVisible">×</button></div>
      <img v-if="recetaVisible" :src="recetaVisible.url" alt="Fotografía privada de receta" />
      <p>Visible solo durante esta sesión autenticada.</p>
    </dialog>

  </div>
</template>
