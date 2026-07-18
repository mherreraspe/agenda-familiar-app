<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import TarjetaPendiente from '../components/TarjetaPendiente.vue'
import {
  cerrarSesion as eliminarSesion,
  completarTarea,
  consultarCatalogo,
  consultarHoy,
  crearTarea,
  iniciarSesion,
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
const datos = ref<RespuestaHoy | null>(null)
const catalogo = ref<RespuestaCatalogo | null>(null)
const mostrarFormulario = ref(false)
const nuevaTarea = reactive({ titulo: '', descripcion: '', perfilId: '', fechaLimite: '' })

const fechaActual = new Intl.DateTimeFormat('es-PE', {
  weekday: 'long', day: 'numeric', month: 'long', timeZone: 'America/Lima'
}).format(new Date())

const pendientes = computed(() => datos.value?.tareas.filter(tarea => tarea.estado === 'PENDIENTE') ?? [])
const atrasadas = computed(() => pendientes.value.filter(tarea => new Date(tarea.fechaLimite) < new Date()))
const proximas = computed(() => pendientes.value.filter(tarea => new Date(tarea.fechaLimite) >= new Date()))

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
    mostrarFormulario.value = false
    mensaje.value = 'La tarea fue agregada.'
    await cargar()
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo guardar la tarea'
  } finally {
    cargando.value = false
  }
}

function salir() {
  eliminarSesion()
  datos.value = null
  sesionActiva.value = false
  mensaje.value = ''
}
</script>

<template>
  <main v-if="!sesionActiva" class="acceso">
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
        <div class="titulo-seccion"><div><span class="etiqueta etiqueta--arena">Calendario</span><h2>Próximas citas</h2></div></div>
        <article v-for="evento in catalogo?.eventos" :key="evento.id" class="tarjeta tarjeta--proximo">
          <time>{{ new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: datos?.zonaHoraria }).format(new Date(evento.inicioEn)) }}</time>
          <div class="tarjeta__contenido"><h3>{{ evento.titulo }}</h3><p>{{ evento.persona }} · {{ evento.lugar || 'Lugar por confirmar' }}</p></div>
          <span class="estado">{{ evento.estado }}</span>
        </article>
      </section>

      <section id="tratamientos" class="seccion">
        <div class="titulo-seccion"><div><span class="etiqueta etiqueta--verde">Cuidado</span><h2>Tratamientos</h2></div></div>
        <article v-for="tratamiento in catalogo?.tratamientos" :key="tratamiento.id" class="tarjeta">
          <div class="tarjeta__contenido"><h3>{{ tratamiento.persona }} · {{ tratamiento.medicamento }}</h3><p>{{ tratamiento.dosisIndicada }} · {{ tratamiento.frecuencia }}</p><small>{{ tratamiento.indicacion }}</small></div>
          <span class="estado">{{ tratamiento.estado }}</span>
        </article>
        <p class="aviso-medico">La aplicación conserva el texto ingresado por la familia; no calcula ni recomienda dosis.</p>
      </section>

      <section id="botiquin" class="seccion">
        <div class="titulo-seccion"><div><span class="etiqueta">Botiquín</span><h2>Medicamentos</h2></div></div>
        <article v-for="medicamento in catalogo?.medicamentos" :key="medicamento.id" class="tarjeta">
          <div class="tarjeta__contenido"><h3>{{ medicamento.nombre }}</h3><p>{{ medicamento.presentacion }} · {{ medicamento.concentracion }}</p><small>{{ medicamento.cantidad }} {{ medicamento.unidad }} · vence {{ medicamento.fechaVencimiento || 'sin fecha' }}</small></div>
          <span class="estado">{{ medicamento.estado }}</span>
        </article>
      </section>
    </main>

    <dialog :open="mostrarFormulario" class="dialogo">
      <form @submit.prevent="guardarTarea">
        <div class="titulo-seccion"><h2>Nueva tarea</h2><button type="button" class="cerrar" @click="mostrarFormulario = false">×</button></div>
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
    </dialog>

    <button type="button" class="agregar" @click="mostrarFormulario = true"><span aria-hidden="true">+</span> Agregar tarea</button>
    <nav class="navegacion" aria-label="Navegación principal">
      <a class="activo" href="#top">Hoy</a><a href="#calendario">Calendario</a><a href="#botiquin">Botiquín</a>
      <a href="#tratamientos">Tratamientos <span v-if="atrasadas.length" class="contador">{{ atrasadas.length }}</span></a>
    </nav>
  </div>
</template>
