<script setup lang="ts">
import { ref } from 'vue'

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
</script>

<template>
  <details ref="menu" class="menu-mas">
    <summary :aria-label="etiqueta">⋮</summary>
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
