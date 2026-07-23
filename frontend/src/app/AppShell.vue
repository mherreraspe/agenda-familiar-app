<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

defineProps<{
  titulo: string
  subtitulo: string
  familia?: string
  cantidadAtencion?: number
}>()

const emit = defineEmits<{
  anadir: [tipo: 'evento' | 'tarea' | 'tratamiento']
  salir: []
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

function anadir(tipo: 'evento' | 'tarea' | 'tratamiento') {
  cerrarMenus()
  emit('anadir', tipo)
}
</script>

<template>
  <div class="app-shell">
    <nav class="app-shell__navegacion" aria-label="Navegación principal">
      <strong class="app-shell__marca">Agenda Familiar</strong>
      <RouterLink :to="{ name: 'hoy' }">
        <span aria-hidden="true">⌂</span><span>Hoy</span>
        <span v-if="cantidadAtencion" class="contador">{{ cantidadAtencion }}</span>
      </RouterLink>
      <RouterLink :to="{ name: 'agenda' }"><span aria-hidden="true">▦</span><span>Agenda</span></RouterLink>
      <RouterLink :to="{ name: 'salud' }"><span aria-hidden="true">＋</span><span>Salud</span></RouterLink>
    </nav>

    <div class="app-shell__contenido">
      <header class="app-shell__cabecera">
        <div>
          <p class="sobretitulo">{{ familia || 'Agenda Familiar' }}</p>
          <h1>{{ titulo }}</h1>
          <p>{{ subtitulo }}</p>
        </div>
        <div class="app-shell__acciones">
          <div ref="menuAnadir" class="menu-desplegable">
            <button type="button" class="boton-anadir" aria-controls="menu-anadir" :aria-expanded="menuAbierto === 'anadir'" @click="alternarMenu('anadir')"><span aria-hidden="true">+</span> Añadir</button>
            <div v-if="menuAbierto === 'anadir'" id="menu-anadir" class="menu-desplegable__panel" aria-label="¿Qué deseas añadir?">
              <button type="button" @click="anadir('evento')">Evento</button>
              <button type="button" @click="anadir('tarea')">Tarea o recordatorio</button>
              <button type="button" @click="anadir('tratamiento')">Tratamiento</button>
            </div>
          </div>

          <div ref="menuAvatar" class="menu-desplegable menu-desplegable--avatar">
            <button type="button" class="avatar" aria-label="Abrir menú de familia" aria-controls="menu-familia" :aria-expanded="menuAbierto === 'avatar'" @click="alternarMenu('avatar')">Familia</button>
            <div v-if="menuAbierto === 'avatar'" id="menu-familia" class="menu-desplegable__panel" aria-label="Menú de familia">
              <RouterLink :to="{ name: 'familia' }" @click="cerrarMenus">Familia y permisos</RouterLink>
              <RouterLink :to="{ name: 'actividad' }" @click="cerrarMenus">Actividad</RouterLink>
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
