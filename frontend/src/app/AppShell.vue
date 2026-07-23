<script setup lang="ts">
import { ref } from 'vue'
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

const menuAnadir = ref<HTMLDetailsElement | null>(null)

function anadir(tipo: 'evento' | 'tarea' | 'tratamiento') {
  menuAnadir.value?.removeAttribute('open')
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
          <details ref="menuAnadir" class="menu-desplegable">
            <summary class="boton-anadir"><span aria-hidden="true">+</span> Añadir</summary>
            <div class="menu-desplegable__panel" aria-label="¿Qué deseas añadir?">
              <button type="button" @click="anadir('evento')">Evento</button>
              <button type="button" @click="anadir('tarea')">Tarea o recordatorio</button>
              <button type="button" @click="anadir('tratamiento')">Tratamiento</button>
            </div>
          </details>

          <details class="menu-desplegable menu-desplegable--avatar">
            <summary class="avatar" aria-label="Abrir menú de familia">Familia</summary>
            <div class="menu-desplegable__panel">
              <RouterLink :to="{ name: 'familia' }">Familia y permisos</RouterLink>
              <RouterLink :to="{ name: 'actividad' }">Actividad</RouterLink>
              <button type="button" @click="emit('salir')">Cerrar sesión</button>
            </div>
          </details>
        </div>
      </header>

      <main id="contenido-principal" class="app-shell__principal" tabindex="-1">
        <slot />
      </main>
    </div>
  </div>
</template>
