<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

export interface AccionMenu {
  id: string
  etiqueta: string
  peligrosa?: boolean
}

withDefaults(defineProps<{
  acciones: AccionMenu[]
  etiqueta?: string
}>(), { etiqueta: 'Más acciones' })

const emit = defineEmits<{ seleccionar: [accion: string] }>()
const menu = ref<HTMLDetailsElement | null>(null)

function seleccionar(accion: string) {
  menu.value?.removeAttribute('open')
  emit('seleccionar', accion)
}

function cerrar() {
  menu.value?.removeAttribute('open')
}

function cerrarOtros() {
  if (menu.value?.open) return
  document.querySelectorAll<HTMLDetailsElement>('.menu-mas[open]').forEach(otro => {
    if (otro !== menu.value) otro.removeAttribute('open')
  })
}

function cerrarAlPulsarFuera(evento: PointerEvent) {
  if (evento.target instanceof Node && !menu.value?.contains(evento.target)) cerrar()
}

function cerrarConEscape(evento: KeyboardEvent) {
  if (evento.key === 'Escape' && menu.value?.open) {
    cerrar()
    menu.value.querySelector<HTMLElement>('summary')?.focus()
  }
}

onMounted(() => {
  document.addEventListener('pointerdown', cerrarAlPulsarFuera)
  document.addEventListener('keydown', cerrarConEscape)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', cerrarAlPulsarFuera)
  document.removeEventListener('keydown', cerrarConEscape)
})
</script>

<template>
  <details ref="menu" class="menu-mas">
    <summary :aria-label="etiqueta" @click="cerrarOtros">⋮</summary>
    <div class="menu-mas__panel">
      <button
        v-for="accion in acciones"
        :key="accion.id"
        type="button"
        :class="{ 'menu-mas__peligro': accion.peligrosa }"
        @click="seleccionar(accion.id)"
      >
        {{ accion.etiqueta }}
      </button>
    </div>
  </details>
</template>
