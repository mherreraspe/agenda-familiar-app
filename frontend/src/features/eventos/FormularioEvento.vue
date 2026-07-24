<script setup lang="ts">
import { nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { consultarSugerencias, crearEvento, type PerfilResumen, type SugerenciaFamiliar } from '../../api'
import ConfirmarDescarte from '../../components/formularios/ConfirmarDescarte.vue'
import FormularioAdaptativo from '../../components/formularios/FormularioAdaptativo.vue'
import { fechaFamiliarAInstant, redondearSiguienteCuarto } from '../../utils/fechaFamiliar'
import { validarEvento, type BorradorEvento, type ErroresEvento } from './validacionEvento'

const props = defineProps<{
  abierto: boolean
  perfiles: PerfilResumen[]
  zonaHoraria: string
}>()
const emit = defineEmits<{
  cerrar: []
  guardado: []
  modificado: [valor: boolean]
}>()

const borrador = reactive<BorradorEvento>({
  perfilId: '', titulo: '', tipo: '', lugar: '', direccion: '', notas: '', inicioEn: '', finEn: '',
  repetir: false, frecuencia: 'SEMANAL', intervalo: 1, hasta: ''
})
const errores = ref<ErroresEvento>({})
const errorGeneral = ref('')
const guardando = ref(false)
const sugerenciasTitulo = ref<SugerenciaFamiliar[]>([])
const sugerenciasLugar = ref<SugerenciaFamiliar[]>([])
const confirmarDescarte = ref<InstanceType<typeof ConfirmarDescarte> | null>(null)
let estadoInicial = ''
let reiniciando = false
let temporizadorTitulo: number | undefined
let temporizadorLugar: number | undefined

function serializar() {
  return JSON.stringify(borrador)
}

async function reiniciar() {
  reiniciando = true
  Object.assign(borrador, {
    perfilId: props.perfiles[0]?.id ?? '',
    titulo: '', tipo: '', lugar: '', direccion: '', notas: '',
    inicioEn: redondearSiguienteCuarto(new Date(), props.zonaHoraria),
    finEn: '', repetir: false, frecuencia: 'SEMANAL', intervalo: 1, hasta: ''
  })
  errores.value = {}
  errorGeneral.value = ''
  sugerenciasTitulo.value = []
  sugerenciasLugar.value = []
  await nextTick()
  estadoInicial = serializar()
  reiniciando = false
  emit('modificado', false)
}

watch(() => props.abierto, abierto => {
  if (abierto) void reiniciar()
  else {
    window.clearTimeout(temporizadorTitulo)
    window.clearTimeout(temporizadorLugar)
    sugerenciasTitulo.value = []
    sugerenciasLugar.value = []
  }
}, { immediate: true })

watch(() => props.perfiles, perfiles => {
  if (props.abierto && !borrador.perfilId && perfiles.length) {
    borrador.perfilId = perfiles[0].id
    estadoInicial = serializar()
  }
}, { deep: true })

watch(borrador, () => {
  if (!reiniciando) emit('modificado', serializar() !== estadoInicial)
}, { deep: true })

function programarSugerencias(campo: 'titulo' | 'lugar') {
  const esTitulo = campo === 'titulo'
  const consulta = (esTitulo ? borrador.titulo : borrador.lugar).trim()
  if (esTitulo) {
    window.clearTimeout(temporizadorTitulo)
    sugerenciasTitulo.value = []
  } else {
    window.clearTimeout(temporizadorLugar)
    sugerenciasLugar.value = []
  }
  if (consulta.length < 2) return

  const temporizador = window.setTimeout(async () => {
    try {
      const respuesta = await consultarSugerencias(consulta)
      if (esTitulo) {
        sugerenciasTitulo.value = respuesta.sugerencias.filter(item => item.tipo === 'EVENTO').slice(0, 3)
      } else {
        sugerenciasLugar.value = respuesta.sugerencias.filter(item => item.tipo === 'LUGAR').slice(0, 3)
      }
    } catch {
      if (esTitulo) sugerenciasTitulo.value = []
      else sugerenciasLugar.value = []
    }
  }, 275)

  if (esTitulo) temporizadorTitulo = temporizador
  else temporizadorLugar = temporizador
}

function cerrarSugerencias(campo: 'titulo' | 'lugar') {
  window.setTimeout(() => {
    if (campo === 'titulo') sugerenciasTitulo.value = []
    else sugerenciasLugar.value = []
  })
}

function seleccionarTitulo(sugerencia: SugerenciaFamiliar) {
  borrador.titulo = sugerencia.titulo
  borrador.lugar = sugerencia.lugar ?? ''
  borrador.direccion = sugerencia.direccion ?? ''
  sugerenciasTitulo.value = []
}

function seleccionarLugar(sugerencia: SugerenciaFamiliar) {
  borrador.lugar = sugerencia.lugar ?? sugerencia.titulo
  borrador.direccion = sugerencia.direccion ?? ''
  sugerenciasLugar.value = []
}

async function enfocarPrimerError() {
  await nextTick()
  document.querySelector<HTMLElement>('.formulario-adaptativo [aria-invalid="true"]')?.focus()
}

async function guardar() {
  errorGeneral.value = ''
  errores.value = validarEvento(borrador, props.zonaHoraria)
  if (Object.keys(errores.value).length) {
    errorGeneral.value = 'Revisa los campos indicados para guardar el evento.'
    await enfocarPrimerError()
    return
  }

  guardando.value = true
  try {
    await crearEvento({
      perfilId: borrador.perfilId || undefined,
      titulo: borrador.titulo.trim(),
      tipo: borrador.tipo || undefined,
      lugar: borrador.lugar.trim() || undefined,
      direccion: borrador.direccion.trim() || undefined,
      notas: borrador.notas.trim() || undefined,
      inicioEn: fechaFamiliarAInstant(borrador.inicioEn, props.zonaHoraria),
      finEn: borrador.finEn ? fechaFamiliarAInstant(borrador.finEn, props.zonaHoraria) : undefined,
      recurrencia: borrador.repetir ? {
        frecuencia: borrador.frecuencia,
        intervalo: borrador.intervalo,
        hasta: fechaFamiliarAInstant(borrador.hasta, props.zonaHoraria)
      } : undefined
    })
    estadoInicial = serializar()
    emit('modificado', false)
    emit('guardado')
  } catch (causa) {
    errorGeneral.value = causa instanceof Error ? causa.message : 'No se pudo guardar el evento.'
  } finally {
    guardando.value = false
  }
}

function preguntarDescarte() {
  return confirmarDescarte.value?.preguntar() ?? Promise.resolve(false)
}

defineExpose({ preguntarDescarte })
onBeforeUnmount(() => {
  window.clearTimeout(temporizadorTitulo)
  window.clearTimeout(temporizadorLugar)
})
</script>

<template>
  <FormularioAdaptativo
    :abierto="abierto"
    titulo="Nuevo evento"
    :guardando="guardando"
    @cerrar="emit('cerrar')"
    @guardar="guardar"
  >
    <p v-if="errorGeneral" class="resumen-errores" role="alert">{{ errorGeneral }}</p>

    <label for="evento-titulo">Título</label>
    <input
      id="evento-titulo"
      v-model="borrador.titulo"
      maxlength="180"
      autocomplete="off"
      autofocus
      :aria-invalid="Boolean(errores.titulo)"
      :aria-describedby="errores.titulo ? 'error-evento-titulo' : undefined"
      @input="programarSugerencias('titulo')"
      @blur="cerrarSugerencias('titulo')"
    />
    <p v-if="errores.titulo" id="error-evento-titulo" class="error-campo">{{ errores.titulo }}</p>
    <div v-if="sugerenciasTitulo.length" class="sugerencias" aria-label="Eventos familiares anteriores">
      <button
        v-for="sugerencia in sugerenciasTitulo"
        :key="`${sugerencia.tipo}-${sugerencia.entidadId}`"
        type="button"
        @mousedown.prevent
        @click="seleccionarTitulo(sugerencia)"
      >
        <strong>{{ sugerencia.titulo }}</strong><small>{{ sugerencia.lugar || 'Sin lugar guardado' }}</small>
      </button>
    </div>

    <label for="evento-inicio">Fecha y hora</label>
    <input
      id="evento-inicio"
      v-model="borrador.inicioEn"
      type="datetime-local"
      :aria-invalid="Boolean(errores.inicioEn)"
      :aria-describedby="errores.inicioEn ? 'error-evento-inicio' : 'ayuda-zona-horaria'"
    />
    <p v-if="errores.inicioEn" id="error-evento-inicio" class="error-campo">{{ errores.inicioEn }}</p>
    <small id="ayuda-zona-horaria">Zona horaria familiar: {{ zonaHoraria }}</small>

    <label for="evento-tipo">Tipo o categoría</label>
    <select id="evento-tipo" v-model="borrador.tipo">
      <option value="">Evento familiar</option>
      <option value="CITA">Cita</option>
      <option value="SALIDA">Salida o visita</option>
      <option value="SALUD">Salud</option>
      <option value="ESCUELA">Escuela</option>
      <option value="TRAMITE">Trámite</option>
      <option value="VIAJE">Viaje</option>
      <option value="CUMPLEANOS">Cumpleaños</option>
      <option value="CASA">Casa</option>
      <option value="OTRO">Otro</option>
    </select>

    <label for="evento-perfil">Persona</label>
    <select id="evento-perfil" v-model="borrador.perfilId">
      <option value="">Toda la familia / sin asignar</option>
      <option v-for="perfil in perfiles" :key="perfil.id" :value="perfil.id">{{ perfil.nombre }}</option>
    </select>

    <label for="evento-fin">Fin opcional</label>
    <input
      id="evento-fin"
      v-model="borrador.finEn"
      type="datetime-local"
      :aria-invalid="Boolean(errores.finEn)"
      :aria-describedby="errores.finEn ? 'error-evento-fin' : undefined"
    />
    <p v-if="errores.finEn" id="error-evento-fin" class="error-campo">{{ errores.finEn }}</p>

    <details>
      <summary>Más opciones</summary>
      <div class="detalles-progresivos">
        <label for="evento-lugar">Lugar opcional</label>
        <input
          id="evento-lugar"
          v-model="borrador.lugar"
          maxlength="300"
          autocomplete="off"
          @input="programarSugerencias('lugar')"
          @blur="cerrarSugerencias('lugar')"
        />
        <div v-if="sugerenciasLugar.length" class="sugerencias" aria-label="Lugares usados anteriormente">
          <button
            v-for="sugerencia in sugerenciasLugar"
            :key="`${sugerencia.tipo}-${sugerencia.entidadId}`"
            type="button"
            @mousedown.prevent
            @click="seleccionarLugar(sugerencia)"
          >
            <strong>{{ sugerencia.lugar || sugerencia.titulo }}</strong><small>{{ sugerencia.direccion || 'Sin dirección guardada' }}</small>
          </button>
        </div>

        <label for="evento-direccion">Dirección opcional</label>
        <input id="evento-direccion" v-model="borrador.direccion" maxlength="500" />
        <label for="evento-notas">Notas opcionales</label>
        <textarea id="evento-notas" v-model="borrador.notas" maxlength="1000" rows="3" />

        <label class="opcion-linea"><input v-model="borrador.repetir" type="checkbox" /> Repetir evento</label>
        <template v-if="borrador.repetir">
          <div class="campos-dobles">
            <label>Frecuencia<select v-model="borrador.frecuencia"><option value="DIARIA">Diaria</option><option value="SEMANAL">Semanal</option><option value="MENSUAL">Mensual</option></select></label>
            <label>Cada<input v-model.number="borrador.intervalo" type="number" min="1" max="30" /></label>
          </div>
          <label for="evento-hasta">Repetir hasta</label>
          <input
            id="evento-hasta"
            v-model="borrador.hasta"
            type="datetime-local"
            :aria-invalid="Boolean(errores.hasta)"
            :aria-describedby="errores.hasta ? 'error-evento-hasta' : undefined"
          />
          <p v-if="errores.hasta" id="error-evento-hasta" class="error-campo">{{ errores.hasta }}</p>
        </template>
      </div>
    </details>
  </FormularioAdaptativo>
  <ConfirmarDescarte ref="confirmarDescarte" />
</template>
