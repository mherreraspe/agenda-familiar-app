<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  consultarNotificaciones,
  guardarPreferenciasNotificacion,
  marcarNotificacionLeida,
  marcarTodasNotificacionesLeidas,
  registrarDispositivoNotificacion,
  revocarDispositivoNotificacion,
  type AvisoFamiliar,
  type PreferenciasNotificacion,
  type RespuestaNotificaciones
} from '../api'

const props = defineProps<{ familiaId: string }>()
const emit = defineEmits<{ contador: [cantidad: number] }>()
const router = useRouter()
const dialogo = ref<HTMLDialogElement | null>(null)
const datos = ref<RespuestaNotificaciones | null>(null)
const cargando = ref(false)
const guardando = ref(false)
const error = ref('')
const mensaje = ref('')
const preferencias = reactive<PreferenciasNotificacion>({
  tareas: true, eventos: true, salud: true, botiquin: true,
  silencioDesde: '22:00', silencioHasta: '07:00'
})

const hoy = computed(() => datos.value?.avisos.filter(aviso => esHoy(aviso.creadaEn)) ?? [])
const anteriores = computed(() => datos.value?.avisos.filter(aviso => !esHoy(aviso.creadaEn)) ?? [])
const navegadorCompatible = computed(() => typeof window !== 'undefined'
  && 'Notification' in window && 'serviceWorker' in navigator && 'PushManager' in window)
const permiso = computed(() => typeof Notification === 'undefined' ? 'no-disponible' : Notification.permission)

function esHoy(fecha: string) {
  const valor = new Date(fecha)
  const ahora = new Date()
  return valor.getFullYear() === ahora.getFullYear() && valor.getMonth() === ahora.getMonth()
    && valor.getDate() === ahora.getDate()
}

function hora(fecha: string) {
  return new Intl.DateTimeFormat('es-PE', { hour: '2-digit', minute: '2-digit' }).format(new Date(fecha))
}

function fecha(fechaAviso: string) {
  return new Intl.DateTimeFormat('es-PE', { day: 'numeric', month: 'short' }).format(new Date(fechaAviso))
}

function simbolo(tipo: AvisoFamiliar['tipo']) {
  return ({ TAREA: '✓', EVENTO: '◷', SALUD: '+', BOTIQUIN: '◇', FAMILIA: '⌂', SISTEMA: 'i' })[tipo]
}

async function cargar() {
  if (!props.familiaId) return
  cargando.value = true
  error.value = ''
  try {
    datos.value = await consultarNotificaciones()
    Object.assign(preferencias, datos.value.preferencias)
    emit('contador', datos.value.sinLeer)
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudieron consultar los avisos.'
  } finally {
    cargando.value = false
  }
}

async function abrir() {
  mensaje.value = ''
  dialogo.value?.showModal()
  await cargar()
}

function cerrar() {
  dialogo.value?.close()
}

async function ver(aviso: AvisoFamiliar) {
  if (!aviso.leidaEn) await marcarNotificacionLeida(aviso.id)
  cerrar()
  await router.push(aviso.destino)
  await cargar()
}

async function leerTodas() {
  await marcarTodasNotificacionesLeidas()
  await cargar()
}

async function guardarPreferencias() {
  guardando.value = true
  error.value = ''
  try {
    const guardadas = await guardarPreferenciasNotificacion({ ...preferencias })
    Object.assign(preferencias, guardadas)
    mensaje.value = 'Preferencias guardadas.'
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudieron guardar las preferencias.'
  } finally {
    guardando.value = false
  }
}

function convertirClavePublica(valor: string) {
  const normalizada = valor.replaceAll('-', '+').replaceAll('_', '/')
  const bytes = atob(normalizada.padEnd(Math.ceil(normalizada.length / 4) * 4, '='))
  return Uint8Array.from(bytes, caracter => caracter.charCodeAt(0))
}

function nombreDispositivo() {
  if (/iPhone|iPad/i.test(navigator.userAgent)) return 'iPhone o iPad'
  if (/Android/i.test(navigator.userAgent)) return 'Dispositivo Android'
  return 'Este navegador'
}

async function activarEnEsteDispositivo() {
  error.value = ''
  mensaje.value = ''
  if (!navegadorCompatible.value) {
    error.value = 'Este navegador no admite avisos web. En iPhone, instala la aplicación en la pantalla de inicio y vuelve a intentarlo.'
    return
  }
  if (!datos.value?.pushDisponible || !datos.value.clavePublica) {
    error.value = 'Los avisos al celular aún no están habilitados en el servidor.'
    return
  }
  try {
    const autorizacion = await Notification.requestPermission()
    if (autorizacion !== 'granted') {
      error.value = 'El permiso quedó bloqueado. Actívalo desde los ajustes del navegador.'
      return
    }
    const registro = await navigator.serviceWorker.ready
    const existente = await registro.pushManager.getSubscription()
    const suscripcion = existente ?? await registro.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: convertirClavePublica(datos.value.clavePublica)
    })
    const serializada = suscripcion.toJSON()
    if (!serializada.endpoint || !serializada.keys?.p256dh || !serializada.keys.auth) {
      throw new Error('El navegador no entregó una suscripción completa.')
    }
    await registrarDispositivoNotificacion({
      endpoint: serializada.endpoint,
      claveP256dh: serializada.keys.p256dh,
      claveAuth: serializada.keys.auth,
      dispositivo: nombreDispositivo()
    })
    mensaje.value = 'Avisos activados en este dispositivo.'
    await cargar()
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudieron activar los avisos.'
  }
}

async function revocar(id: string) {
  await revocarDispositivoNotificacion(id)
  mensaje.value = 'Dispositivo desactivado.'
  await cargar()
}

watch(() => props.familiaId, cargar, { immediate: true })
let temporizador: ReturnType<typeof setInterval> | null = null
onMounted(() => { temporizador = setInterval(cargar, 60_000) })
onBeforeUnmount(() => { if (temporizador) clearInterval(temporizador) })
defineExpose({ abrir, actualizar: cargar })
</script>

<template>
  <dialog ref="dialogo" class="dialogo centro-avisos" aria-labelledby="titulo-centro-avisos" @cancel.prevent="cerrar">
    <header class="centro-avisos__cabecera">
      <div>
        <p class="sobretitulo">Pulso familiar</p>
        <h2 id="titulo-centro-avisos">Avisos</h2>
      </div>
      <button type="button" class="cerrar" aria-label="Cerrar avisos" @click="cerrar">×</button>
    </header>

    <div class="centro-avisos__contenido">
      <div v-if="datos?.sinLeer" class="centro-avisos__resumen">
        <strong>{{ datos.sinLeer }} {{ datos.sinLeer === 1 ? 'aviso nuevo' : 'avisos nuevos' }}</strong>
        <button type="button" class="enlace-accion" @click="leerTodas">Marcar todo como leído</button>
      </div>
      <p v-if="cargando && !datos" class="estado-vacio" aria-live="polite">Actualizando avisos…</p>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <p v-if="mensaje" class="mensaje-exito" role="status">{{ mensaje }}</p>

      <section v-if="hoy.length" aria-labelledby="avisos-hoy">
        <h3 id="avisos-hoy" class="centro-avisos__grupo">Hoy</h3>
        <ol class="pulso-familiar">
          <li v-for="aviso in hoy" :key="aviso.id" :class="[`pulso-familiar__aviso--${aviso.tipo.toLowerCase()}`, { 'pulso-familiar__aviso--leido': aviso.leidaEn }]">
            <span class="pulso-familiar__simbolo" aria-hidden="true">{{ simbolo(aviso.tipo) }}</span>
            <time :datetime="aviso.creadaEn">{{ hora(aviso.creadaEn) }}</time>
            <button type="button" @click="ver(aviso)">
              <strong>{{ aviso.titulo }}</strong><span v-if="aviso.detalle">{{ aviso.detalle }}</span>
              <small>{{ aviso.leidaEn ? 'Leído' : 'Nuevo' }} · Ver detalle</small>
            </button>
          </li>
        </ol>
      </section>

      <section v-if="anteriores.length" aria-labelledby="avisos-anteriores">
        <h3 id="avisos-anteriores" class="centro-avisos__grupo">Anteriores</h3>
        <ol class="pulso-familiar">
          <li v-for="aviso in anteriores" :key="aviso.id" :class="[`pulso-familiar__aviso--${aviso.tipo.toLowerCase()}`, { 'pulso-familiar__aviso--leido': aviso.leidaEn }]">
            <span class="pulso-familiar__simbolo" aria-hidden="true">{{ simbolo(aviso.tipo) }}</span>
            <time :datetime="aviso.creadaEn">{{ fecha(aviso.creadaEn) }}</time>
            <button type="button" @click="ver(aviso)">
              <strong>{{ aviso.titulo }}</strong><span v-if="aviso.detalle">{{ aviso.detalle }}</span>
              <small>{{ aviso.leidaEn ? 'Leído' : 'Nuevo' }} · Ver detalle</small>
            </button>
          </li>
        </ol>
      </section>

      <div v-if="datos && !datos.avisos.length" class="estado-vacio centro-avisos__vacio">
        <strong>No hay avisos pendientes</strong>
        <span>Cuando algo necesite tu atención aparecerá aquí.</span>
      </div>

      <details v-if="datos" class="preferencias-avisos">
        <summary>Configurar avisos</summary>
        <div class="preferencias-avisos__contenido">
          <div class="preferencias-avisos__dispositivo">
            <div><strong>Avisos en este dispositivo</strong><small>La pantalla bloqueada nunca muestra información familiar o médica.</small></div>
            <button type="button" class="boton-secundario" :disabled="!datos.pushDisponible" @click="activarEnEsteDispositivo">Activar</button>
          </div>
          <p v-if="!datos.pushDisponible" class="ayuda-notificaciones">La bandeja privada está disponible. El envío al celular se activará cuando el servidor tenga sus claves de seguridad.</p>
          <fieldset>
            <legend>Quiero recibir avisos de</legend>
            <label><input v-model="preferencias.tareas" type="checkbox" /> Tareas</label>
            <label><input v-model="preferencias.eventos" type="checkbox" /> Eventos, citas y salidas</label>
            <label><input v-model="preferencias.salud" type="checkbox" /> Salud y tratamientos</label>
            <label><input v-model="preferencias.botiquin" type="checkbox" /> Botiquín</label>
          </fieldset>
          <fieldset class="preferencias-avisos__horario">
            <legend>Horario silencioso</legend>
            <label>Desde<input v-model="preferencias.silencioDesde" type="time" /></label>
            <label>Hasta<input v-model="preferencias.silencioHasta" type="time" /></label>
          </fieldset>
          <button type="button" class="boton-principal" :disabled="guardando" @click="guardarPreferencias">{{ guardando ? 'Guardando…' : 'Guardar preferencias' }}</button>
          <div v-if="datos.dispositivos.length" class="preferencias-avisos__lista">
            <strong>Dispositivos registrados</strong>
            <div v-for="dispositivo in datos.dispositivos" :key="dispositivo.id">
              <span>{{ dispositivo.nombre }}<small>{{ dispositivo.activo ? 'Activo' : 'Desactivado' }}</small></span>
              <button v-if="dispositivo.activo" type="button" class="enlace-accion" @click="revocar(dispositivo.id)">Desactivar</button>
            </div>
          </div>
        </div>
      </details>
    </div>
  </dialog>
</template>
