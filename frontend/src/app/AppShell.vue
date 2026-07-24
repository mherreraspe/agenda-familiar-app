<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import IconoApp from '../components/IconoApp.vue'

type TipoAlta = 'evento' | 'tarea' | 'tratamiento' | 'medicamento' | 'objeto'

const props = withDefaults(defineProps<{
  titulo: string
  subtitulo: string
  familia?: string
  cantidadAtencion?: number
  etiquetaAnadir?: string
  tipoAnadirDirecto?: TipoAlta
  tiposAnadir?: TipoAlta[]
  mostrarAnadir?: boolean
  administradorPlataforma?: boolean
  familias?: Array<{ id: string; nombre: string }>
  familiaActivaId?: string
}>(), { mostrarAnadir: true, administradorPlataforma: false })

const familiaVisible = computed(() => {
  const nombre = props.familia?.trim()
  if (!nombre || /^familia_?test$/i.test(nombre)) return 'Mi familia'
  return nombre.replaceAll('_', ' ')
})
const tiposAnadirVisibles = computed<TipoAlta[]>(() => props.tiposAnadir?.length
  ? props.tiposAnadir
  : ['evento', 'tarea', 'objeto', 'tratamiento'])
const etiquetaTipoAlta: Record<TipoAlta, string> = {
  evento: 'Evento, cita o salida',
  tarea: 'Tarea',
  objeto: 'Objeto',
  tratamiento: 'Tratamiento',
  medicamento: 'Medicamento'
}

const emit = defineEmits<{
  anadir: [tipo: TipoAlta]
  salir: []
  cambiarFamilia: [familiaId: string]
}>()

const menuAnadir = ref<HTMLElement | null>(null)
const menuAvatar = ref<HTMLElement | null>(null)
const menuAbierto = ref<'anadir' | 'avatar' | null>(null)

function cerrarMenus() {
  menuAbierto.value = null
}

function alternarMenu(tipo: 'anadir' | 'avatar') {
  menuAbierto.value = menuAbierto.value === tipo ? null : tipo
}

function cerrarAlPulsarFuera(evento: PointerEvent) {
  if (!(evento.target instanceof Node)) return
  if (menuAnadir.value?.contains(evento.target) || menuAvatar.value?.contains(evento.target)) return
  cerrarMenus()
}

function cerrarConEscape(evento: KeyboardEvent) {
  if (evento.key === 'Escape') cerrarMenus()
}

onMounted(() => {
  document.addEventListener('pointerdown', cerrarAlPulsarFuera)
  document.addEventListener('keydown', cerrarConEscape)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', cerrarAlPulsarFuera)
  document.removeEventListener('keydown', cerrarConEscape)
})

function anadir(tipo: TipoAlta) {
  cerrarMenus()
  emit('anadir', tipo)
}

function activarAnadir() {
  if (props.tipoAnadirDirecto) {
    anadir(props.tipoAnadirDirecto)
    return
  }
  alternarMenu('anadir')
}
</script>

<template>
  <div class="app-shell">
    <nav class="app-shell__navegacion" aria-label="Navegación principal">
      <strong class="app-shell__marca">Agenda Familiar</strong>
      <RouterLink :to="{ name: 'hoy' }">
        <IconoApp nombre="hoy" /><span>Hoy</span>
        <span v-if="cantidadAtencion" class="contador">{{ cantidadAtencion }}</span>
      </RouterLink>
      <RouterLink :to="{ name: 'agenda' }"><IconoApp nombre="agenda" /><span>Agenda</span></RouterLink>
      <RouterLink :to="{ name: 'salud' }"><IconoApp nombre="salud" /><span>Salud</span></RouterLink>
      <RouterLink :to="{ name: 'objetos' }"><IconoApp nombre="objetos" /><span>Objetos</span></RouterLink>
    </nav>

    <div class="app-shell__contenido">
      <header class="app-shell__cabecera">
        <div>
          <h1>{{ titulo }}</h1>
          <p class="app-shell__contexto"><span>{{ familiaVisible }}</span><span aria-hidden="true">·</span><span>{{ subtitulo }}</span></p>
        </div>
        <div class="app-shell__acciones">
          <div v-if="mostrarAnadir" ref="menuAnadir" class="menu-desplegable">
            <button type="button" class="boton-anadir" :aria-controls="tipoAnadirDirecto ? undefined : 'menu-anadir'" :aria-expanded="tipoAnadirDirecto ? undefined : menuAbierto === 'anadir'" @click="activarAnadir"><IconoApp nombre="anadir" /> <span>{{ etiquetaAnadir || 'Añadir' }}</span></button>
            <button v-if="menuAbierto === 'anadir'" type="button" class="menu-desplegable__velo" aria-label="Cerrar menú" @click="cerrarMenus"></button>
            <div v-if="menuAbierto === 'anadir'" id="menu-anadir" class="menu-desplegable__panel" aria-label="¿Qué deseas añadir?">
              <button v-for="tipo in tiposAnadirVisibles" :key="tipo" type="button" @click="anadir(tipo)">{{ etiquetaTipoAlta[tipo] }}</button>
            </div>
          </div>

          <div ref="menuAvatar" class="menu-desplegable menu-desplegable--avatar">
            <button type="button" class="avatar" aria-label="Abrir menú de familia" aria-controls="menu-familia" :aria-expanded="menuAbierto === 'avatar'" @click="alternarMenu('avatar')"><IconoApp nombre="usuario" /></button>
            <button v-if="menuAbierto === 'avatar'" type="button" class="menu-desplegable__velo" aria-label="Cerrar menú" @click="cerrarMenus"></button>
            <div v-if="menuAbierto === 'avatar'" id="menu-familia" class="menu-desplegable__panel" aria-label="Menú de familia">
              <template v-if="familias && familias.length > 1">
                <p class="menu-desplegable__titulo">Familia activa</p>
                <button
                  v-for="opcion in familias"
                  :key="opcion.id"
                  type="button"
                  :aria-current="opcion.id === familiaActivaId ? 'true' : undefined"
                  @click="cerrarMenus(); emit('cambiarFamilia', opcion.id)"
                >
                  {{ opcion.nombre }}<span v-if="opcion.id === familiaActivaId" aria-hidden="true"> · Actual</span>
                </button>
                <span class="menu-desplegable__separador" aria-hidden="true"></span>
              </template>
              <RouterLink :to="{ name: 'familia' }" @click="cerrarMenus">Familia y permisos</RouterLink>
              <RouterLink :to="{ name: 'actividad' }" @click="cerrarMenus">Actividad</RouterLink>
              <RouterLink v-if="administradorPlataforma" :to="{ name: 'admin' }" @click="cerrarMenus">Administración</RouterLink>
              <button type="button" @click="cerrarMenus(); emit('salir')">Cerrar sesión</button>
            </div>
          </div>
        </div>
      </header>

      <main id="contenido-principal" class="app-shell__principal" tabindex="-1">
        <slot />
      </main>
    </div>
  </div>
</template>
