<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { consultarEnlaceAcceso, consumirEnlaceAcceso, type EnlaceAccesoPublico } from '../api'

const token = ref('')
const enlace = ref<EnlaceAccesoPublico | null>(null)
const clave = ref('')
const confirmacion = ref('')
const cargando = ref(true)
const guardando = ref(false)
const completado = ref(false)
const error = ref('')

onMounted(async () => {
  const parametros = new URLSearchParams(location.hash.slice(1))
  token.value = parametros.get('token') ?? ''
  history.replaceState(null, '', location.pathname)
  if (!token.value) {
    error.value = 'El enlace no está completo.'
    cargando.value = false
    return
  }
  try {
    enlace.value = await consultarEnlaceAcceso(token.value)
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'El enlace ya no está disponible.'
  } finally {
    cargando.value = false
  }
})

async function guardar() {
  error.value = ''
  if (clave.value !== confirmacion.value) {
    error.value = 'Las contraseñas no coinciden.'
    return
  }
  guardando.value = true
  try {
    await consumirEnlaceAcceso(token.value, clave.value)
    token.value = ''
    clave.value = ''
    confirmacion.value = ''
    completado.value = true
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo completar el acceso.'
  } finally {
    guardando.value = false
  }
}
</script>

<template>
  <main class="acceso acceso--admin">
    <section class="panel-acceso panel-activar">
      <img src="/icono-192.png" alt="" width="72" height="72" />
      <p class="sobretitulo">Obu Familia · Acceso seguro</p>
      <template v-if="cargando"><h1>Comprobando enlace…</h1></template>
      <template v-else-if="completado">
        <h1>Contraseña guardada</h1>
        <p>El enlace quedó invalidado. Ya puedes ingresar con tu correo y la nueva contraseña.</p>
        <RouterLink class="boton-principal enlace-boton" to="/hoy">Ingresar</RouterLink>
      </template>
      <template v-else-if="enlace">
        <h1>{{ enlace.tipo === 'INVITACION' ? 'Activa tu acceso' : 'Crea una nueva contraseña' }}</h1>
        <p v-if="enlace.familia">Te invitaron a <strong>{{ enlace.familia }}</strong>.</p>
        <p>Cuenta: {{ enlace.correo }}</p>
        <form @submit.prevent="guardar">
          <label>Nueva contraseña<input v-model="clave" type="password" autocomplete="new-password" minlength="12" maxlength="128" required /></label>
          <label>Repetir contraseña<input v-model="confirmacion" type="password" autocomplete="new-password" minlength="12" maxlength="128" required /></label>
          <button class="boton-principal" :disabled="guardando">{{ guardando ? 'Guardando…' : 'Guardar contraseña' }}</button>
        </form>
      </template>
      <template v-else>
        <h1>Este enlace no está disponible</h1>
        <p>Puede haber vencido, haber sido revocado o ya haberse utilizado. Solicita uno nuevo al administrador.</p>
      </template>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
    </section>
  </main>
</template>
