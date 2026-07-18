<script setup lang="ts">
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
</script>

<template>
  <article class="tarjeta" :class="`tarjeta--${tono ?? 'normal'}`">
    <time>{{ hora }}</time>
    <div class="tarjeta__contenido">
      <h3>{{ titulo }}</h3>
      <p>{{ detalle }}</p>
      <small v-if="recurrente">Recurrente</small>
    </div>
    <div class="acciones-ocurrencia">
      <button type="button" class="boton-secundario" @click="emit('completar')">Hecho</button>
      <button type="button" class="boton-secundario" @click="emit('omitir')">Omitir</button>
      <button type="button" class="boton-secundario" @click="emit('reprogramar')">Reprogramar</button>
    </div>
  </article>
</template>
