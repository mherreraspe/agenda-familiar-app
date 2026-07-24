<script setup lang="ts">
import MenuMas from './listas/MenuMas.vue'

defineProps<{
  hora: string
  titulo: string
  detalle: string
  tono?: 'normal' | 'atrasado' | 'proximo'
  recurrente?: boolean
}>()

const emit = defineEmits<{
  completar: []
  omitir: []
  reprogramar: []
}>()

function accionSecundaria(accion: string) {
  if (accion === 'omitir') emit('omitir')
  if (accion === 'reprogramar') emit('reprogramar')
}
</script>

<template>
  <article class="tarjeta" :class="`tarjeta--${tono ?? 'normal'}`">
    <time>{{ hora }}</time>
    <div class="tarjeta__contenido">
      <span class="tipo-entrada tipo-entrada--tarea"><span aria-hidden="true">✓</span> Tarea</span>
      <h3>{{ titulo }}</h3>
      <p>{{ detalle }}</p>
      <small v-if="recurrente">Recurrente</small>
    </div>
    <div class="acciones-ocurrencia">
      <button type="button" class="boton-accion" @click="emit('completar')">Hecho</button>
      <MenuMas
        :acciones="[{ id: 'omitir', etiqueta: 'Omitir' }, { id: 'reprogramar', etiqueta: 'Reprogramar' }]"
        :etiqueta="`Más acciones para ${titulo}`"
        @seleccionar="accionSecundaria"
      />
    </div>
  </article>
</template>
