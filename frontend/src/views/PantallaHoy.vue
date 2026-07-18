<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import TarjetaPendiente from '../components/TarjetaPendiente.vue'
import {
  cerrarSesion as eliminarSesion,
  actuarAgenda,
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
  consultarSugerencias,
  crearEvento,
  crearPerfil,
  crearMedicamento,
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
  type RespuestaAuditoria,
  type RespuestaCatalogo,
  type RespuestaHoy,
  type RespuestaFamilia,
  type RespuestaCuota,
  type RespuestaOcurrencias,
  type SugerenciaFamiliar,
  type TareaResumen
} from '../api'
import { reducirImagenReceta, validarImagenReceta } from '../imagen'

const correo = ref('papa@familia.test')
const clave = ref('')
const cargando = ref(false)
const error = ref('')
const mensaje = ref('')
const sesionActiva = ref(false)
const restaurando = ref(true)
const datos = ref<RespuestaHoy | null>(null)
const catalogo = ref<RespuestaCatalogo | null>(null)
const agendaTratamientos = ref<RespuestaOcurrencias | null>(null)
const auditoria = ref<RespuestaAuditoria | null>(null)
const familia = ref<RespuestaFamilia | null>(null)
const cuota = ref<RespuestaCuota | null>(null)
const sugerenciasTitulo = ref<SugerenciaFamiliar[]>([])
const filtroPerfil = ref('TODOS')
const formulario = ref<'tarea' | 'evento' | 'medicamento' | 'tratamiento' | 'perfil' | null>(null)
const nuevaTarea = reactive({ titulo: '', descripcion: '', perfilId: '', fechaLimite: '', repetir: false, frecuencia: 'SEMANAL', intervalo: 1, hasta: '' })
const nuevoEvento = reactive({ perfilId: '', titulo: '', tipo: '', lugar: '', direccion: '', notas: '', inicioEn: '', finEn: '', repetir: false, frecuencia: 'SEMANAL', intervalo: 1, hasta: '' })
const nuevoMedicamento = reactive({ nombre: '', presentacion: '', concentracion: '', cantidad: 1, unidad: 'unidad', fechaVencimiento: '' })
const nuevoTratamiento = reactive({ perfilId: '', medicamentoId: '', nombre: '', indicacion: '', cantidadReceta: '', frecuencia: '', horario: '', horariosAdicionales: '', intervaloHoras: '', fechaInicio: '', fechaFin: '', responsablePerfilId: '', responsableAlternativoPerfilId: '' })
const recetaSeleccionada = ref<File | null>(null)
const recetaVisible = ref<{ id: string; url: string } | null>(null)
const perfilEditadoId = ref<string | null>(null)
const nuevoPerfil = reactive({ nombre: '', tipo: 'DEPENDIENTE' as 'ADULTO' | 'DEPENDIENTE', color: '#315b4c', relacion: '', usuarioId: '', permiso: 'ADULTO' as 'ADMINISTRADOR_FAMILIAR' | 'ADULTO', activo: true })

const fechaActual = new Intl.DateTimeFormat('es-PE', {
  weekday: 'long', day: 'numeric', month: 'long', timeZone: 'America/Lima'
}).format(new Date())

const coincideFiltro = (perfilId?: string) => filtroPerfil.value === 'TODOS' || perfilId === filtroPerfil.value
const pendientes = computed(() => datos.value?.tareas.filter(tarea => tarea.estado === 'PENDIENTE' && coincideFiltro(tarea.perfilId)) ?? [])
const atrasadas = computed(() => pendientes.value.filter(tarea => new Date(tarea.fechaLimite) < new Date()))
const proximas = computed(() => pendientes.value.filter(tarea => new Date(tarea.fechaLimite) >= new Date()))
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
const lugaresSugeridos = computed(() => {
  const consulta = nuevoEvento.lugar.trim().toLocaleLowerCase('es-PE')
  if (!consulta) return catalogo.value?.lugares.slice(0, 5) ?? []
  return catalogo.value?.lugares.filter(lugar => lugar.nombre.toLocaleLowerCase('es-PE').includes(consulta)).slice(0, 5) ?? []
})
const tituloFormulario = computed(() => ({
  tarea: 'Nueva tarea', evento: 'Nuevo evento', medicamento: 'Nuevo medicamento', tratamiento: 'Nuevo tratamiento', perfil: perfilEditadoId.value ? 'Editar perfil' : 'Nuevo perfil'
})[formulario.value ?? 'tarea'])
let temporizadorSugerencias: number | undefined

onMounted(async () => {
  try {
    await renovarSesion()
    sesionActiva.value = true
    await cargar()
  } catch {
    sesionActiva.value = false
  } finally {
    restaurando.value = false
  }
})

function hora(tarea: TareaResumen) {
  return new Intl.DateTimeFormat('es-PE', {
    weekday: 'short', hour: '2-digit', minute: '2-digit', timeZone: datos.value?.zonaHoraria ?? 'America/Lima'
  }).format(new Date(tarea.fechaLimite))
}

async function entrar() {
  cargando.value = true
  error.value = ''
  try {
    await iniciarSesion(correo.value, clave.value)
    sesionActiva.value = true
    await cargar()
    clave.value = ''
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo iniciar sesión'
  } finally {
    cargando.value = false
  }
}

async function cargar() {
  const [hoy, detalle, ocurrencias, historial, configuracion, almacenamiento] = await Promise.all([
    consultarHoy(), consultarCatalogo(), consultarOcurrencias(), consultarAuditoria(), consultarConfiguracionFamilia(), consultarCuota()
  ])
  datos.value = hoy
  catalogo.value = detalle
  agendaTratamientos.value = ocurrencias
  auditoria.value = historial
  familia.value = configuracion
  cuota.value = almacenamiento
  if (!nuevaTarea.perfilId && datos.value.perfiles.length) {
    nuevaTarea.perfilId = datos.value.perfiles[0].id
  }
  if (!nuevoEvento.perfilId && datos.value.perfiles.length) nuevoEvento.perfilId = datos.value.perfiles[0].id
  if (!nuevoTratamiento.perfilId && datos.value.perfiles.length) nuevoTratamiento.perfilId = datos.value.perfiles[0].id
}

async function marcarHecho(tarea: TareaResumen) {
  error.value = ''
  try {
    await completarTarea(tarea.id)
    mensaje.value = `${tarea.titulo} quedó marcado como hecho.`
    await cargar()
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
    await cargar()
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo guardar la tarea'
  } finally {
    cargando.value = false
  }
}

async function guardarEvento() {
  await ejecutarGuardado(async () => {
    await crearEvento({
      ...nuevoEvento,
      perfilId: nuevoEvento.perfilId || undefined,
      tipo: nuevoEvento.tipo || undefined,
      lugar: nuevoEvento.lugar || undefined,
      direccion: nuevoEvento.direccion || undefined,
      notas: nuevoEvento.notas || undefined,
      inicioEn: new Date(nuevoEvento.inicioEn).toISOString(),
      finEn: nuevoEvento.finEn ? new Date(nuevoEvento.finEn).toISOString() : undefined,
      recurrencia: nuevoEvento.repetir ? { frecuencia: nuevoEvento.frecuencia as 'DIARIA' | 'SEMANAL' | 'MENSUAL', intervalo: nuevoEvento.intervalo, hasta: new Date(nuevoEvento.hasta).toISOString() } : undefined
    })
    Object.assign(nuevoEvento, { perfilId: nuevoEvento.perfilId, titulo: '', tipo: '', lugar: '', direccion: '', notas: '', inicioEn: '', finEn: '', repetir: false, hasta: '' })
  }, 'El evento fue agregado.')
}

async function guardarMedicamento() {
  await ejecutarGuardado(async () => {
    await crearMedicamento({
      ...nuevoMedicamento,
      fechaVencimiento: nuevoMedicamento.fechaVencimiento || undefined
    })
    Object.assign(nuevoMedicamento, { nombre: '', presentacion: '', concentracion: '', cantidad: 1, unidad: 'unidad', fechaVencimiento: '' })
  }, 'El medicamento fue agregado.')
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
  }, 'El tratamiento fue agregado.')
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
    await cargar()
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
    await cargar()
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo eliminar la receta'
  } finally {
    cargando.value = false
  }
}

function seleccionarLugar(lugar: { nombre: string; direccion?: string }) {
  nuevoEvento.lugar = lugar.nombre
  nuevoEvento.direccion = lugar.direccion ?? ''
}

function actualizarSugerenciasTitulo() {
  window.clearTimeout(temporizadorSugerencias)
  if (nuevoEvento.titulo.trim().length < 2) {
    sugerenciasTitulo.value = []
    return
  }
  temporizadorSugerencias = window.setTimeout(async () => {
    try {
      sugerenciasTitulo.value = (await consultarSugerencias(nuevoEvento.titulo)).sugerencias
        .filter(sugerencia => sugerencia.tipo === 'EVENTO')
    } catch {
      sugerenciasTitulo.value = []
    }
  }, 250)
}

function seleccionarSugerencia(sugerencia: SugerenciaFamiliar) {
  nuevoEvento.titulo = sugerencia.titulo
  nuevoEvento.lugar = sugerencia.lugar ?? ''
  nuevoEvento.direccion = sugerencia.direccion ?? ''
  sugerenciasTitulo.value = []
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
    await cargar()
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo resolver la ocurrencia'
  } finally {
    cargando.value = false
  }
}

async function cerrarTratamientoActivo(tratamiento: RespuestaCatalogo['tratamientos'][number]) {
  const motivo = window.prompt('Motivo del cierre (opcional)')
  if (motivo === null) return
  cargando.value = true
  error.value = ''
  try {
    await cerrarTratamiento(tratamiento.id, motivo)
    mensaje.value = `${tratamiento.medicamento} quedó cerrado.`
    await cargar()
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo cerrar el tratamiento'
  } finally {
    cargando.value = false
  }
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
    await cargar()
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo cerrar el elemento'
  } finally {
    cargando.value = false
  }
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
    await cargar()
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo actualizar la agenda'
  } finally {
    cargando.value = false
  }
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
  }, 'La configuración familiar fue actualizada.')
}

async function ejecutarGuardado(accion: () => Promise<void>, confirmacion: string) {
  cargando.value = true
  error.value = ''
  try {
    await accion()
    formulario.value = null
    mensaje.value = confirmacion
    await cargar()
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
    datos.value = null
    catalogo.value = null
    agendaTratamientos.value = null
    auditoria.value = null
    familia.value = null
    cuota.value = null
    cerrarRecetaVisible()
    sesionActiva.value = false
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

  <div v-else class="aplicacion">
    <header class="cabecera">
      <div>
        <p class="sobretitulo">{{ fechaActual }}</p>
        <h1>Buenos días</h1>
        <p>{{ datos?.familia }} · Esto es lo importante hoy.</p>
      </div>
      <button type="button" class="avatar" aria-label="Cerrar sesión" @click="salir">Salir</button>
    </header>

    <main>
      <section class="miembros" aria-label="Filtrar agenda por persona">
        <button type="button" :class="{ activo: filtroPerfil === 'TODOS' }" :aria-pressed="filtroPerfil === 'TODOS'" @click="filtroPerfil = 'TODOS'">Todos</button>
        <button v-for="perfil in datos?.perfiles" :key="perfil.id" type="button"
          :class="{ activo: filtroPerfil === perfil.id }" :aria-pressed="filtroPerfil === perfil.id"
          :style="{ borderColor: perfil.color }" @click="filtroPerfil = perfil.id">
          {{ perfil.nombre }}
        </button>
      </section>

      <section class="resumen" aria-label="Resumen del día">
        <div><strong>{{ pendientes.length }}</strong><span>pendientes</span></div>
        <div><strong>{{ ocurrenciasPendientes.length }}</strong><span>tomas pendientes</span></div>
        <div><strong>{{ cantidadRevision }}</strong><span>por revisar</span></div>
      </section>

      <p v-if="mensaje" class="confirmacion" role="status">{{ mensaje }}</p>
      <p v-if="error" class="error" role="alert">{{ error }}</p>

      <section v-if="atrasadas.length" class="seccion seccion--alerta">
        <div class="titulo-seccion">
          <div><span class="etiqueta">Necesita atención</span><h2>Atrasado</h2></div>
        </div>
        <TarjetaPendiente
          v-for="tarea in atrasadas" :key="tarea.id" :hora="hora(tarea)" :titulo="tarea.titulo"
          :detalle="`${tarea.responsable} · ${tarea.descripcion ?? 'Sin detalles'}`" tono="atrasado"
          :recurrente="tarea.recurrente" @completar="marcarHecho(tarea)"
          @omitir="resolverAgenda('tareas', tarea, 'OMITIR')" @reprogramar="resolverAgenda('tareas', tarea, 'REPROGRAMAR')"
        />
      </section>

      <section class="seccion">
        <div class="titulo-seccion">
          <div><span class="etiqueta etiqueta--verde">En orden</span><h2>Próximos siete días</h2></div>
        </div>
        <TarjetaPendiente
          v-for="tarea in proximas" :key="tarea.id" :hora="hora(tarea)" :titulo="tarea.titulo"
          :detalle="`${tarea.responsable} · ${tarea.descripcion ?? 'Sin detalles'}`" tono="proximo"
          :recurrente="tarea.recurrente" @completar="marcarHecho(tarea)"
          @omitir="resolverAgenda('tareas', tarea, 'OMITIR')" @reprogramar="resolverAgenda('tareas', tarea, 'REPROGRAMAR')"
        />
        <p v-if="!proximas.length" class="estado-vacio">No hay pendientes para los próximos días.</p>
      </section>

      <section id="ocurrencias" class="seccion">
        <div class="titulo-seccion"><div><span class="etiqueta etiqueta--verde">Tratamientos</span><h2>Ocurrencias</h2></div></div>
        <article v-for="ocurrencia in ocurrenciasPendientes" :key="ocurrencia.id" class="tarjeta tarjeta--proximo">
          <time>{{ new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: datos?.zonaHoraria }).format(new Date(ocurrencia.programadaEn)) }}</time>
          <div class="tarjeta__contenido"><h3>{{ ocurrencia.tratamiento }}</h3><p>Para {{ ocurrencia.persona }}</p></div>
          <div class="acciones-ocurrencia">
            <button type="button" class="boton-secundario" :disabled="cargando" @click="resolverOcurrencia(ocurrencia, 'TOMADA')">Tomada</button>
            <button type="button" class="boton-secundario" :disabled="cargando" @click="resolverOcurrencia(ocurrencia, 'OMITIDA')">Omitir</button>
            <button type="button" class="boton-secundario" :disabled="cargando" @click="resolverOcurrencia(ocurrencia, 'POSPUESTA')">Posponer 30 min</button>
            <button type="button" class="boton-secundario" :disabled="cargando" @click="resolverOcurrencia(ocurrencia, 'REPROGRAMADA')">Reprogramar</button>
            <button type="button" class="boton-secundario" :disabled="cargando" @click="resolverOcurrencia(ocurrencia, 'CANCELADA')">Cancelar</button>
          </div>
        </article>
        <p v-if="!ocurrenciasPendientes.length" class="estado-vacio">No hay ocurrencias pendientes para este filtro.</p>
        <details v-if="historialOcurrencias.length" class="historial-ocurrencias">
          <summary>Ver historial de ocurrencias ({{ historialOcurrencias.length }})</summary>
          <article v-for="ocurrencia in historialOcurrencias" :key="`historial-${ocurrencia.id}`" class="tarjeta">
            <time>{{ new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: datos?.zonaHoraria }).format(new Date(ocurrencia.resueltaEn || ocurrencia.programadaEn)) }}</time>
            <div class="tarjeta__contenido"><h3>{{ ocurrencia.tratamiento }}</h3><p>{{ ocurrencia.persona }} · {{ ocurrencia.resueltaPorNombre || 'Adulto autorizado' }}</p></div>
            <span class="estado">{{ ocurrencia.estado.replace('_', ' ') }}</span>
          </article>
        </details>
      </section>

      <section id="revisar" class="seccion seccion--alerta">
        <div class="titulo-seccion"><div><span class="etiqueta">Necesita atención</span><h2>Revisar</h2></div></div>
        <TarjetaPendiente v-for="tarea in atrasadas" :key="`revisar-${tarea.id}`" :hora="hora(tarea)"
          :titulo="tarea.titulo" :detalle="`${tarea.responsable} · Tarea vencida`" tono="atrasado"
          :recurrente="tarea.recurrente" @completar="marcarHecho(tarea)"
          @omitir="resolverAgenda('tareas', tarea, 'OMITIR')" @reprogramar="resolverAgenda('tareas', tarea, 'REPROGRAMAR')" />
        <article v-for="evento in eventosAtrasados" :key="`revisar-${evento.id}`" class="tarjeta tarjeta--atrasado">
          <time>{{ new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: datos?.zonaHoraria }).format(new Date(evento.inicioEn)) }}</time>
          <div class="tarjeta__contenido"><h3>{{ evento.titulo }}</h3><p>Evento pendiente de revisión</p><small v-if="evento.recurrente">Recurrente</small></div>
          <div class="acciones-ocurrencia"><button type="button" class="boton-secundario" @click="resolverAgenda('eventos', evento, 'OMITIR')">Omitir</button><button type="button" class="boton-secundario" @click="resolverAgenda('eventos', evento, 'REPROGRAMAR')">Reprogramar</button></div>
        </article>
        <article v-for="elemento in elementosRevision" :key="elemento.id" class="tarjeta tarjeta--atrasado">
          <time v-if="elemento.fecha">{{ new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: datos?.zonaHoraria }).format(new Date(elemento.fecha)) }}</time>
          <div class="tarjeta__contenido"><h3>{{ elemento.titulo }}</h3><p>{{ elemento.motivo.replaceAll('_', ' ').toLowerCase() }}</p></div>
          <div v-if="elemento.origen === 'OCURRENCIA'" class="acciones-ocurrencia">
            <button type="button" class="boton-secundario" @click="resolverDesdeRevision(elemento, 'TOMADA')">Tomada</button>
            <button type="button" class="boton-secundario" @click="resolverDesdeRevision(elemento, 'OMITIDA')">Omitir</button>
            <button type="button" class="boton-secundario" @click="resolverDesdeRevision(elemento, 'POSPUESTA')">Posponer 30 min</button>
            <button type="button" class="boton-secundario" @click="resolverDesdeRevision(elemento, 'REPROGRAMADA')">Reprogramar</button>
            <button type="button" class="boton-secundario" @click="resolverDesdeRevision(elemento, 'CANCELADA')">Cancelar</button>
          </div>
          <button v-else type="button" class="boton-secundario" @click="resolverDesdeRevision(elemento)">Cerrar</button>
        </article>
        <p v-if="!elementosRevision.length && !atrasadas.length && !eventosAtrasados.length" class="estado-vacio">No hay elementos por revisar.</p>
      </section>

      <section id="calendario" class="seccion">
        <div class="titulo-seccion"><div><span class="etiqueta etiqueta--arena">Calendario</span><h2>Próximas citas</h2></div><button type="button" class="boton-secundario" @click="formulario = 'evento'">Agregar</button></div>
        <article v-for="evento in eventosFiltrados" :key="evento.id" class="tarjeta tarjeta--proximo">
          <time>{{ new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: datos?.zonaHoraria }).format(new Date(evento.inicioEn)) }}</time>
          <div class="tarjeta__contenido"><h3>{{ evento.titulo }}</h3><p>{{ [evento.persona, evento.lugar].filter(Boolean).join(' · ') || 'Sin persona ni lugar asignados' }}</p><small v-if="evento.recurrente">Recurrente</small></div>
          <div class="acciones-ocurrencia"><button type="button" class="boton-secundario" @click="resolverAgenda('eventos', evento, 'OMITIR')">Omitir</button><button type="button" class="boton-secundario" @click="resolverAgenda('eventos', evento, 'REPROGRAMAR')">Reprogramar</button></div>
        </article>
      </section>

      <section id="tratamientos" class="seccion">
        <div class="titulo-seccion"><div><span class="etiqueta etiqueta--verde">Cuidado</span><h2>Tratamientos</h2></div><button type="button" class="boton-secundario" @click="formulario = 'tratamiento'">Agregar</button></div>
        <article v-for="tratamiento in tratamientosFiltrados" :key="tratamiento.id" class="tarjeta">
          <div class="tarjeta__contenido"><h3>{{ tratamiento.persona }} · {{ tratamiento.medicamento }}</h3><p>Responsable: {{ tratamiento.responsable }}<span v-if="tratamiento.responsableAlternativo"> · alternativo: {{ tratamiento.responsableAlternativo }}</span></p><p>{{ tratamiento.intervaloHoras ? `Cada ${tratamiento.intervaloHoras} h desde ${tratamiento.horarios[0]}` : `Horarios: ${tratamiento.horarios.join(', ')}` }}</p><p v-if="tratamiento.dosisIndicada || tratamiento.frecuencia">{{ [tratamiento.dosisIndicada, tratamiento.frecuencia].filter(Boolean).join(' · ') }}</p><small v-if="tratamiento.indicacion">{{ tratamiento.indicacion }}</small></div>
          <div class="acciones-ocurrencia"><span class="estado">{{ tratamiento.estado }}</span><label v-if="!tratamiento.recetaId" class="boton-secundario boton-archivo">Agregar receta<input type="file" accept="image/jpeg,image/png" capture="environment" :disabled="cargando" @change="agregarReceta($event, tratamiento.id)" /></label><button v-if="tratamiento.recetaId" type="button" class="boton-secundario" :disabled="cargando" @click="verReceta(tratamiento.recetaId)">Ver receta</button><button v-if="tratamiento.recetaId" type="button" class="boton-secundario" :disabled="cargando" @click="borrarReceta(tratamiento.recetaId)">Eliminar receta</button><button v-if="tratamiento.estado === 'ACTIVO'" type="button" class="boton-secundario" :disabled="cargando" @click="cerrarTratamientoActivo(tratamiento)">Cerrar</button></div>
        </article>
        <p class="aviso-medico">La aplicación conserva el texto ingresado por la familia; no calcula ni recomienda dosis.</p>
      </section>

      <section id="vencimientos" class="seccion seccion--alerta">
        <div class="titulo-seccion"><div><span class="etiqueta">Vencimientos</span><h2>Vencimientos cercanos</h2></div></div>
        <article v-for="medicamento in vencimientosCercanos" :key="`vence-${medicamento.loteId || medicamento.id}`" class="tarjeta tarjeta--atrasado">
          <time>{{ medicamento.fechaVencimiento || 'Sin fecha' }}</time>
          <div class="tarjeta__contenido"><h3>{{ medicamento.nombre }}</h3><p>{{ medicamento.cantidad }} {{ medicamento.unidad }}</p></div>
          <span class="estado">{{ medicamento.estado.replace('_', ' ') }}</span>
        </article>
        <p v-if="!vencimientosCercanos.length" class="estado-vacio">No hay medicamentos vencidos ni próximos a vencer en 30 días.</p>
      </section>

      <section id="botiquin" class="seccion">
        <div class="titulo-seccion"><div><span class="etiqueta">Botiquín</span><h2>Medicamentos</h2></div><button type="button" class="boton-secundario" @click="formulario = 'medicamento'">Agregar</button></div>
        <div v-if="cuota" class="cuota" :class="`cuota--${cuota.nivel.toLowerCase()}`" role="status">
          <div><strong>Fotos privadas: {{ cuota.porcentaje }} %</strong><span>{{ (cuota.usadosBytes / 1048576).toFixed(1) }} MiB de {{ (cuota.cuotaBytes / 1073741824).toFixed(1) }} GiB</span></div>
          <progress :value="cuota.porcentaje" max="100">{{ cuota.porcentaje }} %</progress>
          <small v-if="cuota.porcentaje >= 70">El espacio disponible se está agotando. Elimina fotografías que ya no necesites.</small>
        </div>
        <article v-for="medicamento in catalogo?.medicamentos" :key="medicamento.loteId || medicamento.id" class="tarjeta">
          <div class="tarjeta__contenido"><h3>{{ medicamento.nombre }}</h3><p>{{ medicamento.presentacion }} · {{ medicamento.concentracion }}</p><small>{{ medicamento.cantidad }} {{ medicamento.unidad }} · vence {{ medicamento.fechaVencimiento || 'sin fecha' }}</small></div>
          <span class="estado">{{ medicamento.estado.replace('_', ' ') }}</span>
        </article>
      </section>

      <section id="historial" class="seccion">
        <div class="titulo-seccion"><div><span class="etiqueta etiqueta--arena">Trazabilidad</span><h2>Historial familiar</h2></div></div>
        <article v-for="entrada in auditoria?.entradas.slice(0, 20)" :key="`${entrada.entidad}-${entrada.entidadId}-${entrada.fecha}`" class="tarjeta">
          <time>{{ new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: datos?.zonaHoraria }).format(new Date(entrada.fecha)) }}</time>
          <div class="tarjeta__contenido"><h3>{{ entrada.titulo }}</h3><p>{{ entrada.actor }} · {{ entrada.operacion.toLowerCase() }}</p><small>{{ entrada.resumen }}</small></div>
        </article>
        <p v-if="!auditoria?.entradas.length" class="estado-vacio">Todavía no hay cambios registrados.</p>
      </section>

      <section id="familia" class="seccion">
        <div class="titulo-seccion"><div><span class="etiqueta etiqueta--verde">Familia</span><h2>Perfiles y permisos</h2></div><button v-if="familia?.puedeAdministrar" type="button" class="boton-secundario" @click="abrirPerfil()">Agregar</button></div>
        <article v-for="perfil in familia?.perfiles" :key="perfil.id" class="tarjeta" :class="{ 'perfil-inactivo': !perfil.activo }">
          <span class="muestra-color" :style="{ backgroundColor: perfil.color || '#c8d2ce' }" aria-hidden="true"></span>
          <div class="tarjeta__contenido"><h3>{{ perfil.nombre }}</h3><p>{{ perfil.tipo === 'ADULTO' ? 'Adulto' : 'Dependiente' }} · {{ perfil.relacion || 'Sin relación indicada' }}</p><small>{{ perfil.permiso === 'ADMINISTRADOR_FAMILIAR' ? 'Administrador familiar' : perfil.usuarioId ? 'Adulto con acceso' : 'Sin cuenta vinculada' }}<span v-if="!perfil.activo"> · Inactivo</span></small></div>
          <button v-if="familia?.puedeAdministrar" type="button" class="boton-secundario" @click="abrirPerfil(perfil)">Editar</button>
        </article>
        <p v-if="!familia?.puedeAdministrar" class="estado-vacio">Solo un administrador familiar puede cambiar perfiles y permisos.</p>
      </section>
    </main>

    <dialog :open="formulario !== null" class="dialogo">
      <div class="titulo-seccion"><h2>{{ tituloFormulario }}</h2><button type="button" class="cerrar" aria-label="Cerrar" @click="formulario = null">×</button></div>

      <form v-if="formulario === 'tarea'" @submit.prevent="guardarTarea">
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
        <button class="boton-principal" :disabled="cargando">Guardar tarea</button>
      </form>

      <form v-else-if="formulario === 'evento'" @submit.prevent="guardarEvento">
        <label>Persona opcional<select v-model="nuevoEvento.perfilId"><option value="">Toda la familia / sin asignar</option><option v-for="perfil in datos?.perfiles" :key="perfil.id" :value="perfil.id">{{ perfil.nombre }}</option></select></label>
        <label>Título<input v-model.trim="nuevoEvento.titulo" maxlength="180" autocomplete="off" required @input="actualizarSugerenciasTitulo" /></label>
        <div v-if="sugerenciasTitulo.length" class="sugerencias" aria-label="Registros familiares anteriores">
          <button v-for="sugerencia in sugerenciasTitulo" :key="`${sugerencia.tipo}-${sugerencia.entidadId}`" type="button" @click="seleccionarSugerencia(sugerencia)">
            <strong>{{ sugerencia.titulo }}</strong><small>{{ sugerencia.lugar || 'Sin lugar guardado' }}</small>
          </button>
        </div>
        <label>Tipo opcional<select v-model="nuevoEvento.tipo"><option value="">Sin tipo</option><option value="CONSULTA">Consulta</option><option value="VACUNA">Vacuna</option><option value="CONTROL">Control</option><option value="OTRO">Otro</option></select></label>
        <label>Lugar<input v-model.trim="nuevoEvento.lugar" maxlength="300" autocomplete="off" /></label>
        <div v-if="lugaresSugeridos.length" class="sugerencias" aria-label="Lugares usados anteriormente">
          <button v-for="lugar in lugaresSugeridos" :key="lugar.id" type="button" @click="seleccionarLugar(lugar)">
            <strong>{{ lugar.nombre }}</strong><small>{{ lugar.direccion || 'Sin dirección guardada' }}</small>
          </button>
        </div>
        <label>Dirección opcional<input v-model.trim="nuevoEvento.direccion" maxlength="500" /></label>
        <label>Notas opcionales<textarea v-model.trim="nuevoEvento.notas" maxlength="1000" rows="2" /></label>
        <label>Inicio<input v-model="nuevoEvento.inicioEn" type="datetime-local" required /></label>
        <label>Fin opcional<input v-model="nuevoEvento.finEn" type="datetime-local" /></label>
        <label class="opcion-linea"><input v-model="nuevoEvento.repetir" type="checkbox" /> Repetir evento</label>
        <div v-if="nuevoEvento.repetir" class="campos-dobles">
          <label>Frecuencia<select v-model="nuevoEvento.frecuencia"><option value="DIARIA">Diaria</option><option value="SEMANAL">Semanal</option><option value="MENSUAL">Mensual</option></select></label>
          <label>Cada<input v-model.number="nuevoEvento.intervalo" type="number" min="1" max="30" required /></label>
          <label>Hasta<input v-model="nuevoEvento.hasta" type="datetime-local" required /></label>
        </div>
        <button class="boton-principal" :disabled="cargando">Guardar evento</button>
      </form>

      <form v-else-if="formulario === 'medicamento'" @submit.prevent="guardarMedicamento">
        <label>Nombre<input v-model.trim="nuevoMedicamento.nombre" maxlength="180" required /></label>
        <label>Presentación<input v-model.trim="nuevoMedicamento.presentacion" maxlength="120" placeholder="Caja, frasco…" /></label>
        <label>Concentración<input v-model.trim="nuevoMedicamento.concentracion" maxlength="120" placeholder="Texto del envase" /></label>
        <div class="campos-dobles"><label>Cantidad<input v-model.number="nuevoMedicamento.cantidad" type="number" min="0" step="0.01" required /></label><label>Unidad<input v-model.trim="nuevoMedicamento.unidad" maxlength="40" required /></label></div>
        <label>Vencimiento opcional<input v-model="nuevoMedicamento.fechaVencimiento" type="date" /></label>
        <button class="boton-principal" :disabled="cargando">Guardar medicamento</button>
      </form>

      <form v-else-if="formulario === 'tratamiento'" @submit.prevent="guardarTratamiento">
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
        <button class="boton-principal" :disabled="cargando">Guardar tratamiento</button>
      </form>

      <form v-else-if="formulario === 'perfil'" @submit.prevent="guardarPerfil">
        <label>Nombre visible<input v-model.trim="nuevoPerfil.nombre" maxlength="120" required /></label>
        <label>Tipo<select v-model="nuevoPerfil.tipo"><option value="ADULTO">Adulto</option><option value="DEPENDIENTE">Dependiente</option></select></label>
        <div class="campos-dobles"><label>Relación<input v-model.trim="nuevoPerfil.relacion" maxlength="80" placeholder="Mamá, abuelo, hija…" /></label><label>Color<input v-model="nuevoPerfil.color" type="color" /></label></div>
        <template v-if="nuevoPerfil.tipo === 'ADULTO'">
          <label>ID de cuenta opcional<input v-model.trim="nuevoPerfil.usuarioId" type="text" placeholder="UUID de una cuenta existente" /></label>
          <label v-if="nuevoPerfil.usuarioId">Permiso<select v-model="nuevoPerfil.permiso"><option value="ADULTO">Adulto</option><option value="ADMINISTRADOR_FAMILIAR">Administrador familiar</option></select></label>
        </template>
        <label class="opcion-linea"><input v-model="nuevoPerfil.activo" type="checkbox" /> Perfil activo</label>
        <button class="boton-principal" :disabled="cargando">Guardar perfil</button>
      </form>
    </dialog>

    <dialog :open="recetaVisible !== null" class="dialogo dialogo--receta">
      <div class="titulo-seccion"><h2>Receta privada</h2><button type="button" class="cerrar" aria-label="Cerrar receta" @click="cerrarRecetaVisible">×</button></div>
      <img v-if="recetaVisible" :src="recetaVisible.url" alt="Fotografía privada de receta" />
      <p>Visible solo durante esta sesión autenticada.</p>
    </dialog>

    <button type="button" class="agregar" @click="formulario = 'tarea'"><span aria-hidden="true">+</span> Agregar tarea</button>
    <nav class="navegacion" aria-label="Navegación principal">
      <a class="activo" href="#top">Hoy</a><a href="#revisar">Revisar <span v-if="cantidadRevision" class="contador">{{ cantidadRevision }}</span></a><a href="#calendario">Calendario</a><a href="#botiquin">Botiquín</a>
      <a href="#tratamientos">Tratamientos</a><a href="#familia">Familia</a>
    </nav>
  </div>
</template>
