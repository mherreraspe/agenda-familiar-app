<script setup lang="ts">
defineProps<{ nombre: 'hoy' | 'agenda' | 'salud' | 'objetos' | 'anadir' | 'usuario' }>()
</script>

<template>
  <svg class="icono-app" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
    <template v-if="nombre === 'hoy'">
      <path d="M4 10.5 12 4l8 6.5" />
      <path d="M6.5 9.5V20h11V9.5M9.5 20v-6h5v6" />
    </template>
    <template v-else-if="nombre === 'agenda'">
      <rect x="3.5" y="5" width="17" height="15" rx="2.5" />
      <path d="M7.5 3.5V7M16.5 3.5V7M3.5 9.5h17M8 13h.01M12 13h.01M16 13h.01M8 16.5h.01M12 16.5h.01" />
    </template>
    <template v-else-if="nombre === 'salud'">
      <path d="M20.6 5.7a5.4 5.4 0 0 0-7.7 0L12 6.6l-.9-.9a5.4 5.4 0 0 0-7.7 7.7l.9.9L12 22l7.7-7.7.9-.9a5.4 5.4 0 0 0 0-7.7Z" />
      <path d="M7.4 12h2.3l1.2-2.4 2.1 5 1.2-2.6h2.4" />
    </template>
    <template v-else-if="nombre === 'anadir'">
      <path d="M12 5v14M5 12h14" />
    </template>
    <template v-else-if="nombre === 'objetos'">
      <path d="m4 8 8-4 8 4-8 4-8-4Z" />
      <path d="M4 8v8l8 4 8-4V8M12 12v8" />
    </template>
    <template v-else>
      <circle cx="12" cy="8" r="3.25" />
      <path d="M5.5 20c.7-4 3-6 6.5-6s5.8 2 6.5 6" />
    </template>
  </svg>
</template>
