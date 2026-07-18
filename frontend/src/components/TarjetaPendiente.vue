<script setup lang="ts">
defineProps<{
  hora: string
  titulo: string
  detalle: string
  tono?: 'normal' | 'atrasado' | 'proximo'
}>()

const emit = defineEmits<{
  completar: []
}>()
</script>

<template>
  <article class="tarjeta" :class="`tarjeta--${tono ?? 'normal'}`">
    <time>{{ hora }}</time>
    <div class="tarjeta__contenido">
      <h3>{{ titulo }}</h3>
      <p>{{ detalle }}</p>
    </div>
    <button type="button" class="boton-secundario" @click="emit('completar')">Hecho</button>
  </article>
</template>
