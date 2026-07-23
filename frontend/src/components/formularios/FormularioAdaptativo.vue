<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'

const props = defineProps<{
  abierto: boolean
  titulo: string
  guardando?: boolean
}>()
const emit = defineEmits<{
  cerrar: []
  guardar: []
}>()

const dialogo = ref<HTMLDialogElement | null>(null)

watch(() => props.abierto, async abierto => {
  await nextTick()
  if (abierto && dialogo.value && !dialogo.value.open) {
    dialogo.value.showModal()
    await nextTick()
    const destino = dialogo.value.querySelector<HTMLElement>('[autofocus]')
      ?? dialogo.value.querySelector<HTMLElement>('input, select, textarea')
      ?? dialogo.value.querySelector<HTMLElement>('button')
    destino?.focus()
  } else if (!abierto && dialogo.value?.open) {
    dialogo.value.close()
  }
}, { immediate: true })
</script>

<template>
  <dialog
    ref="dialogo"
    class="formulario-adaptativo"
    :aria-labelledby="'titulo-formulario-adaptativo'"
    @cancel.prevent="emit('cerrar')"
  >
    <form class="formulario-adaptativo__estructura" novalidate @submit.prevent="emit('guardar')">
      <header class="formulario-adaptativo__cabecera">
        <button type="button" class="cerrar" aria-label="Cerrar formulario" @click="emit('cerrar')">×</button>
        <h2 id="titulo-formulario-adaptativo">{{ titulo }}</h2>
      </header>
      <div class="formulario-adaptativo__contenido">
        <slot />
      </div>
      <footer class="formulario-adaptativo__acciones">
        <button type="button" class="boton-secundario" :disabled="guardando" @click="emit('cerrar')">Cancelar</button>
        <button type="submit" class="boton-principal" :disabled="guardando">
          {{ guardando ? 'Guardando…' : 'Guardar evento' }}
        </button>
      </footer>
    </form>
  </dialog>
</template>
