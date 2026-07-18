<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import TarjetaPendiente from '../components/TarjetaPendiente.vue'
import {
  cerrarSesion as eliminarSesion,
  completarTarea,
  consultarCatalogo,
  consultarHoy,
  crearEvento,
  crearMedicamento,
  crearTarea,
  crearTratamiento,
  iniciarSesion,
  renovarSesion,
  type RespuestaCatalogo,
  type RespuestaHoy,
  type TareaResumen
} from '../api'

const correo = ref('papa@familia.test')
const clave = ref('')
const cargando = ref(false)
const error = ref('')
const mensaje = ref('')
const sesionActiva = ref(false)
const restaurando = ref(true)
const datos = ref<RespuestaHoy | null>(null)
const catalogo = ref<RespuestaCatalogo | null>(null)
const formulario = ref<'tarea' | 'evento' | 'medicamento' | 'tratamiento' | null>(null)
const nuevaTarea = reactive({ titulo: '', descripcion: '', perfilId: '', fechaLimite: '' })
const nuevoEvento = reactive({ perfilId: '', titulo: '', tipo: 'CONSULTA', lugar: '', inicioEn: '', finEn: '' })
const nuevoMedicamento = reactive({ nombre: '', presentacion: '', concentracion: '', cantidad: 1, unidad: 'unidad', fechaVencimiento: '' })
const nuevoTratamiento = reactive({ perfilId: '', medicamentoId: '', indicacion: '', dosisIndicada: '', frecuencia: '', fechaInicio: '', fechaFin: '' })

const fechaActual = new Intl.DateTimeFormat('es-PE', {
  weekday: 'long', day: 'numeric', month: 'long', timeZone: 'America/Lima'
}).format(new Date())

const pendientes = computed(() => datos.value?.tareas.filter(tarea => tarea.estado === 'PENDIENTE') ?? [])
const atrasadas = computed(() => pendientes.value.filter(tarea => new Date(tarea.fechaLimite) < new Date()))
const proximas = computed(() => pendientes.value.filter(tarea => new Date(tarea.fechaLimite) >= new Date()))
const tituloFormulario = computed(() => ({
  tarea: 'Nueva tarea', evento: 'Nuevo evento', medicamento: 'Nuevo medicamento', tratamiento: 'Nuevo tratamiento'
})[formulario.value ?? 'tarea'])

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
  const [hoy, detalle] = await Promise.all([consultarHoy(), consultarCatalogo()])
  datos.value = hoy
  catalogo.value = detalle
  if (!nuevaTarea.perfilId && datos.value.perfiles.length) {
    nuevaTarea.perfilId = datos.value.perfiles[0].id
  }
  if (!nuevoEvento.perfilId && datos.value.perfiles.length) nuevoEvento.perfilId = datos.value.perfiles[0].id
  if (!nuevoTratamiento.perfilId && datos.value.perfiles.length) nuevoTratamiento.perfilId = datos.value.perfiles[0].id
  if (!nuevoTratamiento.medicamentoId && catalogo.value.medicamentos.length) {
    nuevoTratamiento.medicamentoId = catalogo.value.medicamentos[0].id
  }
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
      ...nuevaTarea,
      fechaLimite: new Date(nuevaTarea.fechaLimite).toISOString()
    })
    nuevaTarea.titulo = ''
    nuevaTarea.descripcion = ''
    nuevaTarea.fechaLimite = ''
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
      inicioEn: new Date(nuevoEvento.inicioEn).toISOString(),
      finEn: nuevoEvento.finEn ? new Date(nuevoEvento.finEn).toISOString() : undefined
    })
    Object.assign(nuevoEvento, { perfilId: nuevoEvento.perfilId, titulo: '', tipo: 'CONSULTA', lugar: '', inicioEn: '', finEn: '' })
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
  await ejecutarGuardado(async () => {
    await crearTratamiento({
      ...nuevoTratamiento,
      fechaFin: nuevoTratamiento.fechaFin || undefined
    })
    Object.assign(nuevoTratamiento, { perfilId: nuevoTratamiento.perfilId, medicamentoId: nuevoTratamiento.medicamentoId, indicacion: '', dosisIndicada: '', frecuencia: '', fechaInicio: '', fechaFin: '' })
  }, 'El tratamiento fue agregado.')
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
      <section class="miembros" aria-label="Miembros de la familia">
        <span v-for="perfil in datos?.perfiles" :key="perfil.id" :style="{ borderColor: perfil.color }">
          {{ perfil.nombre }}
        </span>
      </section>

      <section class="resumen" aria-label="Resumen del día">
        <div><strong>{{ pendientes.length }}</strong><span>pendientes</span></div>
        <div><strong>{{ catalogo?.tratamientos.length ?? 0 }}</strong><span>tratamientos</span></div>
        <div><strong>{{ datos?.perfiles.length ?? 0 }}</strong><span>familiares</span></div>
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
          @completar="marcarHecho(tarea)"
        />
      </section>

      <section class="seccion">
        <div class="titulo-seccion">
          <div><span class="etiqueta etiqueta--verde">En orden</span><h2>Próximos siete días</h2></div>
        </div>
        <TarjetaPendiente
          v-for="tarea in proximas" :key="tarea.id" :hora="hora(tarea)" :titulo="tarea.titulo"
          :detalle="`${tarea.responsable} · ${tarea.descripcion ?? 'Sin detalles'}`" tono="proximo"
          @completar="marcarHecho(tarea)"
        />
        <p v-if="!proximas.length" class="estado-vacio">No hay pendientes para los próximos días.</p>
      </section>

      <section id="calendario" class="seccion">
        <div class="titulo-seccion"><div><span class="etiqueta etiqueta--arena">Calendario</span><h2>Próximas citas</h2></div><button type="button" class="boton-secundario" @click="formulario = 'evento'">Agregar</button></div>
        <article v-for="evento in catalogo?.eventos" :key="evento.id" class="tarjeta tarjeta--proximo">
          <time>{{ new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: datos?.zonaHoraria }).format(new Date(evento.inicioEn)) }}</time>
          <div class="tarjeta__contenido"><h3>{{ evento.titulo }}</h3><p>{{ evento.persona }} · {{ evento.lugar || 'Lugar por confirmar' }}</p></div>
          <span class="estado">{{ evento.estado }}</span>
        </article>
      </section>

      <section id="tratamientos" class="seccion">
        <div class="titulo-seccion"><div><span class="etiqueta etiqueta--verde">Cuidado</span><h2>Tratamientos</h2></div><button type="button" class="boton-secundario" :disabled="!catalogo?.medicamentos.length" @click="formulario = 'tratamiento'">Agregar</button></div>
        <article v-for="tratamiento in catalogo?.tratamientos" :key="tratamiento.id" class="tarjeta">
          <div class="tarjeta__contenido"><h3>{{ tratamiento.persona }} · {{ tratamiento.medicamento }}</h3><p>{{ tratamiento.dosisIndicada }} · {{ tratamiento.frecuencia }}</p><small>{{ tratamiento.indicacion }}</small></div>
          <span class="estado">{{ tratamiento.estado }}</span>
        </article>
        <p class="aviso-medico">La aplicación conserva el texto ingresado por la familia; no calcula ni recomienda dosis.</p>
      </section>

      <section id="botiquin" class="seccion">
        <div class="titulo-seccion"><div><span class="etiqueta">Botiquín</span><h2>Medicamentos</h2></div><button type="button" class="boton-secundario" @click="formulario = 'medicamento'">Agregar</button></div>
        <article v-for="medicamento in catalogo?.medicamentos" :key="medicamento.id" class="tarjeta">
          <div class="tarjeta__contenido"><h3>{{ medicamento.nombre }}</h3><p>{{ medicamento.presentacion }} · {{ medicamento.concentracion }}</p><small>{{ medicamento.cantidad }} {{ medicamento.unidad }} · vence {{ medicamento.fechaVencimiento || 'sin fecha' }}</small></div>
          <span class="estado">{{ medicamento.estado }}</span>
        </article>
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
        <button class="boton-principal" :disabled="cargando">Guardar tarea</button>
      </form>

      <form v-else-if="formulario === 'evento'" @submit.prevent="guardarEvento">
        <label>Persona<select v-model="nuevoEvento.perfilId" required><option v-for="perfil in datos?.perfiles" :key="perfil.id" :value="perfil.id">{{ perfil.nombre }}</option></select></label>
        <label>Título<input v-model.trim="nuevoEvento.titulo" maxlength="180" required /></label>
        <label>Tipo<select v-model="nuevoEvento.tipo"><option value="CONSULTA">Consulta</option><option value="VACUNA">Vacuna</option><option value="CONTROL">Control</option><option value="OTRO">Otro</option></select></label>
        <label>Lugar<input v-model.trim="nuevoEvento.lugar" maxlength="300" /></label>
        <label>Inicio<input v-model="nuevoEvento.inicioEn" type="datetime-local" required /></label>
        <label>Fin opcional<input v-model="nuevoEvento.finEn" type="datetime-local" /></label>
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
        <label>Medicamento<select v-model="nuevoTratamiento.medicamentoId" required><option v-for="medicamento in catalogo?.medicamentos" :key="medicamento.id" :value="medicamento.id">{{ medicamento.nombre }} · {{ medicamento.concentracion }}</option></select></label>
        <label>Indicación<textarea v-model.trim="nuevoTratamiento.indicacion" maxlength="1000" rows="2" required /></label>
        <label>Dosis indicada<input v-model.trim="nuevoTratamiento.dosisIndicada" maxlength="300" required /></label>
        <label>Frecuencia<input v-model.trim="nuevoTratamiento.frecuencia" maxlength="300" placeholder="Texto de la receta" required /></label>
        <div class="campos-dobles"><label>Inicio<input v-model="nuevoTratamiento.fechaInicio" type="date" required /></label><label>Fin opcional<input v-model="nuevoTratamiento.fechaFin" type="date" /></label></div>
        <button class="boton-principal" :disabled="cargando">Guardar tratamiento</button>
      </form>
    </dialog>

    <button type="button" class="agregar" @click="formulario = 'tarea'"><span aria-hidden="true">+</span> Agregar tarea</button>
    <nav class="navegacion" aria-label="Navegación principal">
      <a class="activo" href="#top">Hoy</a><a href="#calendario">Calendario</a><a href="#botiquin">Botiquín</a>
      <a href="#tratamientos">Tratamientos <span v-if="atrasadas.length" class="contador">{{ atrasadas.length }}</span></a>
    </nav>
  </div>
</template>
