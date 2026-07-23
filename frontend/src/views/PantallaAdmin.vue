<script setup lang="ts">
import { nextTick, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import {
  cerrarSesion,
  consultarFamiliasPlataforma,
  crearFamiliaPlataforma,
  iniciarSesion,
  renovarSesion,
  type FamiliaAdministrada
} from '../api'

const correo = ref('papa@familia.test')
const clave = ref('')
const restaurando = ref(true)
const sesionActiva = ref(false)
const autorizado = ref(false)
const cargando = ref(false)
const error = ref('')
const familias = ref<FamiliaAdministrada[]>([])
const formularioAbierto = ref(false)
const dialogo = ref<HTMLDialogElement | null>(null)
const activador = ref<HTMLElement | null>(null)
const nuevaFamilia = reactive({ nombre: '', zonaHoraria: 'America/Lima' })

watch(formularioAbierto, async abierto => {
  await nextTick()
  if (abierto && dialogo.value && !dialogo.value.open) {
    dialogo.value.showModal()
    await nextTick()
    dialogo.value.querySelector<HTMLElement>('[autofocus]')?.focus()
  } else if (!abierto && dialogo.value?.open) {
    dialogo.value.close()
    await nextTick()
    activador.value?.focus()
  }
})

onMounted(async () => {
  try {
    const sesion = await renovarSesion()
    sesionActiva.value = true
    autorizado.value = sesion.rolPlataforma === 'ADMINISTRADOR_PLATAFORMA'
    if (autorizado.value) await cargarFamilias()
  } catch {
    sesionActiva.value = false
  } finally {
    restaurando.value = false
  }
})

async function entrar() {
  cargando.value = true
  error.value = ''
  try {
    const sesion = await iniciarSesion(correo.value, clave.value)
    sesionActiva.value = true
    autorizado.value = sesion.rolPlataforma === 'ADMINISTRADOR_PLATAFORMA'
    clave.value = ''
    if (autorizado.value) await cargarFamilias()
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo iniciar sesión.'
  } finally {
    cargando.value = false
  }
}

async function cargarFamilias() {
  const respuesta = await consultarFamiliasPlataforma()
  familias.value = respuesta.familias
}

function abrirFormulario(evento: MouseEvent) {
  activador.value = evento.currentTarget instanceof HTMLElement ? evento.currentTarget : null
  formularioAbierto.value = true
}

async function guardarFamilia() {
  cargando.value = true
  error.value = ''
  try {
    await crearFamiliaPlataforma(nuevaFamilia)
    Object.assign(nuevaFamilia, { nombre: '', zonaHoraria: 'America/Lima' })
    formularioAbierto.value = false
    await cargarFamilias()
  } catch (causa) {
    error.value = causa instanceof Error ? causa.message : 'No se pudo crear la familia.'
  } finally {
    cargando.value = false
  }
}

async function salir() {
  await cerrarSesion()
  sesionActiva.value = false
  autorizado.value = false
  familias.value = []
}

function fecha(valor: string) {
  return new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(valor))
}
</script>

<template>
  <main v-if="restaurando" class="acceso">
    <section class="panel-acceso panel-acceso--cargando" aria-live="polite">
      <img src="/icono.svg" alt="" width="72" height="72" />
      <p>Comprobando acceso administrativo…</p>
    </section>
  </main>

  <main v-else-if="!sesionActiva" class="acceso acceso--admin">
    <section class="panel-acceso">
      <img src="/icono.svg" alt="" width="72" height="72" />
      <p class="sobretitulo">OBU System · Administración</p>
      <h1>Control de familias</h1>
      <p>Ingresa con una cuenta autorizada para administrar la plataforma.</p>
      <form @submit.prevent="entrar">
        <label>Correo<input v-model.trim="correo" type="email" autocomplete="username" required /></label>
        <label>Contraseña<input v-model="clave" type="password" autocomplete="current-password" required /></label>
        <button class="boton-principal" :disabled="cargando">{{ cargando ? 'Ingresando…' : 'Ingresar' }}</button>
      </form>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <RouterLink to="/hoy">Volver a Agenda Familiar</RouterLink>
    </section>
  </main>

  <main v-else-if="!autorizado" class="acceso acceso--admin">
    <section class="panel-acceso panel-acceso--denegado">
      <p class="sobretitulo">Acceso restringido</p>
      <h1>Esta cuenta no administra la plataforma</h1>
      <p>Puedes continuar usando únicamente las familias a las que perteneces.</p>
      <RouterLink class="boton-principal enlace-boton" to="/hoy">Ir a Agenda Familiar</RouterLink>
      <button type="button" class="boton-secundario" @click="salir">Cerrar sesión</button>
    </section>
  </main>

  <main v-else class="admin-plataforma">
    <header class="admin-cabecera">
      <div><p class="sobretitulo">Administración de plataforma</p><h1>Familias</h1><p>Registro y acceso de los grupos familiares de OBU System.</p></div>
      <div class="admin-cabecera__acciones">
        <button type="button" class="boton-principal" @click="abrirFormulario">Nueva familia</button>
        <button type="button" class="boton-admin-salir" @click="salir">Cerrar sesión</button>
      </div>
    </header>

    <section class="admin-registro" aria-labelledby="titulo-registro-familias">
      <div class="admin-registro__titulo">
        <div><span class="etiqueta etiqueta--verde">Registro activo</span><h2 id="titulo-registro-familias">{{ familias.length }} {{ familias.length === 1 ? 'familia' : 'familias' }}</h2></div>
        <RouterLink to="/hoy">Abrir Agenda Familiar</RouterLink>
      </div>
      <div v-if="familias.length" class="admin-familias">
        <article v-for="familia in familias" :key="familia.id" class="admin-familia">
          <div class="admin-familia__marca" aria-hidden="true">F</div>
          <div><h3>{{ familia.nombre }}</h3><p>{{ familia.zonaHoraria }}</p><small>Creada {{ fecha(familia.creadaEn) }}</small></div>
          <span class="estado">Activa</span>
        </article>
      </div>
      <p v-else class="estado-vacio">Todavía no hay familias registradas.</p>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
    </section>

    <dialog ref="dialogo" class="dialogo dialogo--formulario" aria-labelledby="titulo-nueva-familia" @cancel.prevent="formularioAbierto = false">
      <div class="titulo-seccion dialogo__cabecera"><h2 id="titulo-nueva-familia">Nueva familia</h2><button type="button" class="cerrar" aria-label="Cerrar formulario" @click="formularioAbierto = false">×</button></div>
      <form id="formulario-familia-plataforma" @submit.prevent="guardarFamilia">
        <label>Nombre de la familia<input v-model.trim="nuevaFamilia.nombre" autofocus maxlength="120" required /></label>
        <label>Zona horaria<select v-model="nuevaFamilia.zonaHoraria" required><option value="America/Lima">Lima</option><option value="America/Bogota">Bogotá</option><option value="America/Mexico_City">Ciudad de México</option><option value="America/New_York">Nueva York</option><option value="Europe/Madrid">Madrid</option></select></label>
        <p class="ayuda-admin">Los miembros y sus invitaciones se añadirán desde el detalle de esta familia.</p>
      </form>
      <footer class="dialogo__acciones">
        <button type="button" class="boton-secundario" :disabled="cargando" @click="formularioAbierto = false">Cancelar</button>
        <button type="submit" form="formulario-familia-plataforma" class="boton-principal" :disabled="cargando">{{ cargando ? 'Creando…' : 'Crear familia' }}</button>
      </footer>
    </dialog>
  </main>
</template>
