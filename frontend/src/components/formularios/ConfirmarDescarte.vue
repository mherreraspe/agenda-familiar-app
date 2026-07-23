<script setup lang="ts">
import { nextTick, ref } from 'vue'

const dialogo = ref<HTMLDialogElement | null>(null)
let resolver: ((resultado: boolean) => void) | null = null

async function preguntar() {
  if (!dialogo.value) return false
  await nextTick()
  dialogo.value.showModal()
  return new Promise<boolean>(resolve => { resolver = resolve })
}

function responder(resultado: boolean) {
  dialogo.value?.close()
  resolver?.(resultado)
  resolver = null
}

defineExpose({ preguntar })
</script>

<template>
  <dialog ref="dialogo" class="confirmar-descarte" aria-labelledby="titulo-descarte" @cancel.prevent="responder(false)">
    <h2 id="titulo-descarte">¿Descartar los cambios?</h2>
    <p>La información que escribiste no se guardará.</p>
    <div class="confirmar-descarte__acciones">
      <button type="button" class="boton-secundario" autofocus @click="responder(false)">Seguir editando</button>
      <button type="button" class="boton-peligro" @click="responder(true)">Descartar</button>
    </div>
  </dialog>
</template>
