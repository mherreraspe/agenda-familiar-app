<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  cerrarSesion, consultarCuentasPlataforma, consultarEnlacesPlataforma, consultarFamiliasPlataforma,
  consultarMiembrosPlataforma, crearFamiliaPlataforma, crearInvitacionPlataforma,
  actualizarMiembroPlataforma,
  crearMiembroPlataforma, crearRestablecimientoPlataforma, iniciarSesion, renovarSesion,
  revocarEnlacePlataforma, type CuentaAdministrada, type EnlaceAccesoAdministrado,
  type EnlaceAccesoGenerado, type FamiliaAdministrada, type MiembroPlataforma
} from '../api'

const router = useRouter()
const correo = ref('')
const clave = ref('')
const restaurando = ref(true)
const sesionActiva = ref(false)
const autorizado = ref(false)
const cargando = ref(false)
const error = ref('')
const familias = ref<FamiliaAdministrada[]>([])
const familiaActiva = ref<FamiliaAdministrada | null>(null)
const miembros = ref<MiembroPlataforma[]>([])
const enlaces = ref<EnlaceAccesoAdministrado[]>([])
const cuentas = ref<CuentaAdministrada[]>([])
const dialogoFamilia = ref<HTMLDialogElement | null>(null)
const dialogoMiembro = ref<HTMLDialogElement | null>(null)
const dialogoEnlace = ref<HTMLDialogElement | null>(null)
const dialogoGestion = ref<HTMLDialogElement | null>(null)
const activador = ref<HTMLElement | null>(null)
const enlaceGenerado = ref<EnlaceAccesoGenerado | null>(null)
const enlaceCompleto = ref('')
const copiado = ref(false)
const miembroExistente = ref<MiembroPlataforma | null>(null)
const miembroGestionado = ref<MiembroPlataforma | null>(null)
const nuevaFamilia = reactive({ nombre: '', zonaHoraria: 'America/Lima' })
const nuevoMiembro = reactive({ nombre: '', correo: '', permiso: 'ADULTO' as MiembroPlataforma['permiso'] })
const gestionMiembro = reactive({ permiso: 'ADULTO' as MiembroPlataforma['permiso'], activo: true })

onMounted(async () => {
  try {
    const sesion = await renovarSesion()
    sesionActiva.value = true
    autorizado.value = sesion.rolPlataforma === 'ADMINISTRADOR_PLATAFORMA'
    if (!autorizado.value) {
      await router.replace({ name: 'hoy' })
      return
    }
    await cargarFamilias()
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
    if (!autorizado.value) {
      await router.replace({ name: 'hoy' })
      return
    }
    await cargarFamilias()
  } catch (causa) {
    error.value = mensaje(causa, 'No se pudo iniciar sesión.')
  } finally {
    cargando.value = false
  }
}

async function cargarFamilias() {
  familias.value = (await consultarFamiliasPlataforma()).familias
}

async function seleccionarFamilia(familia: FamiliaAdministrada) {
  familiaActiva.value = familia
  error.value = ''
  await cargarDetalle()
  await nextTick()
  document.querySelector('#miembros-familia')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function cargarDetalle() {
  if (!familiaActiva.value) return
  const [respuestaMiembros, respuestaEnlaces] = await Promise.all([
    consultarMiembrosPlataforma(familiaActiva.value.id), consultarEnlacesPlataforma(familiaActiva.value.id)
  ])
  miembros.value = respuestaMiembros.miembros
  enlaces.value = respuestaEnlaces.enlaces
  cuentas.value = (await consultarCuentasPlataforma(miembros.value.map(miembro => miembro.usuarioId))).cuentas
}

function abrir(dialogo: HTMLDialogElement | null, evento?: Event) {
  if (evento?.currentTarget instanceof HTMLElement) activador.value = evento.currentTarget
  if (!document.querySelector('dialog:modal')) dialogo?.showModal()
}

function cerrar(dialogo: HTMLDialogElement | null) {
  dialogo?.close()
  nextTick(() => activador.value?.focus())
}

function abrirNuevoMiembro(evento: Event) {
  miembroExistente.value = null
  Object.assign(nuevoMiembro, { nombre: '', correo: '', permiso: miembros.value.some(miembro => miembro.activo) ? 'ADULTO' : 'ADMINISTRADOR_FAMILIAR' })
  abrir(dialogoMiembro.value, evento)
  nextTick(() => dialogoMiembro.value?.querySelector<HTMLElement>('[autofocus]')?.focus())
}

function abrirGestion(miembro: MiembroPlataforma, evento: Event) {
  miembroGestionado.value = miembro
  Object.assign(gestionMiembro, { permiso: miembro.permiso, activo: miembro.activo })
  error.value = ''
  abrir(dialogoGestion.value, evento)
}

async function guardarGestion() {
  if (!familiaActiva.value || !miembroGestionado.value) return
  cargando.value = true
  error.value = ''
  try {
    const pendiente = invitacion(miembroGestionado.value)
    if (!gestionMiembro.activo && pendiente?.estado === 'PENDIENTE') await revocarEnlacePlataforma(pendiente.id)
    await actualizarMiembroPlataforma(familiaActiva.value.id, miembroGestionado.value.perfilId, gestionMiembro)
    cerrar(dialogoGestion.value)
    await cargarDetalle()
  } catch (causa) {
    error.value = mensaje(causa, 'No se pudo actualizar el acceso familiar.')
  } finally { cargando.value = false }
}

function abrirInvitacion(miembro: MiembroPlataforma, evento: Event) {
  miembroExistente.value = miembro
  Object.assign(nuevoMiembro, { nombre: miembro.nombre, correo: '', permiso: miembro.permiso })
  abrir(dialogoMiembro.value, evento)
  nextTick(() => dialogoMiembro.value?.querySelector<HTMLElement>('[autofocus]')?.focus())
}

async function guardarFamilia() {
  cargando.value = true
  error.value = ''
  try {
    const creada = await crearFamiliaPlataforma(nuevaFamilia)
    Object.assign(nuevaFamilia, { nombre: '', zonaHoraria: 'America/Lima' })
    cerrar(dialogoFamilia.value)
    await cargarFamilias()
    await seleccionarFamilia(creada)
  } catch (causa) {
    error.value = mensaje(causa, 'No se pudo crear la familia.')
  } finally { cargando.value = false }
}

async function guardarMiembro() {
  if (!familiaActiva.value) return
  cargando.value = true
  error.value = ''
  try {
    const usuarioId = miembroExistente.value?.usuarioId ?? crypto.randomUUID()
    if (!miembroExistente.value) {
      await crearMiembroPlataforma(familiaActiva.value.id, {
        usuarioId, nombre: nuevoMiembro.nombre, permiso: nuevoMiembro.permiso
      })
    }
    const generado = await crearInvitacionPlataforma({ usuarioId, familiaId: familiaActiva.value.id,
      familiaNombre: familiaActiva.value.nombre, correo: nuevoMiembro.correo })
    cerrar(dialogoMiembro.value)
    await cargarDetalle()
    mostrarEnlace(generado)
  } catch (causa) {
    error.value = mensaje(causa, 'No se pudo generar la invitación.')
    await cargarDetalle().catch(() => undefined)
  } finally { cargando.value = false }
}

async function restablecer(miembro: MiembroPlataforma, evento: Event) {
  activador.value = evento.currentTarget instanceof HTMLElement ? evento.currentTarget : null
  cargando.value = true
  error.value = ''
  try { mostrarEnlace(await crearRestablecimientoPlataforma(miembro.usuarioId)) }
  catch (causa) { error.value = mensaje(causa, 'No se pudo generar el restablecimiento.') }
  finally { cargando.value = false }
}

async function revocar(enlace: EnlaceAccesoAdministrado) {
  cargando.value = true
  error.value = ''
  try { await revocarEnlacePlataforma(enlace.id); await cargarDetalle() }
  catch (causa) { error.value = mensaje(causa, 'No se pudo revocar el enlace.') }
  finally { cargando.value = false }
}

function mostrarEnlace(generado: EnlaceAccesoGenerado) {
  enlaceGenerado.value = generado
  enlaceCompleto.value = new URL(generado.enlace, location.origin).href
  copiado.value = false
  nextTick(() => dialogoEnlace.value?.showModal())
}

async function copiarEnlace() {
  await navigator.clipboard.writeText(enlaceCompleto.value)
  copiado.value = true
}

function cuenta(miembro: MiembroPlataforma) { return cuentas.value.find(item => item.usuarioId === miembro.usuarioId) }
function invitacion(miembro: MiembroPlataforma) { return enlaces.value.find(item => item.usuarioId === miembro.usuarioId && item.tipo === 'INVITACION') }
function fecha(valor: string) { return new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(valor)) }
function mensaje(causa: unknown, reserva: string) { return causa instanceof Error ? causa.message : reserva }

async function salir() {
  await cerrarSesion()
  sesionActiva.value = false
  autorizado.value = false
  familias.value = []
  familiaActiva.value = null
}
</script>

<template>
  <main v-if="restaurando" class="acceso"><section class="panel-acceso panel-acceso--cargando" aria-live="polite"><img src="/icono-192.png" alt="" width="72" height="72" /><p>Comprobando acceso administrativo…</p></section></main>

  <main v-else-if="!sesionActiva" class="acceso acceso--admin">
    <section class="panel-acceso"><img src="/icono-192.png" alt="" width="72" height="72" /><p class="sobretitulo">OBU System · Administración</p><h1>Control de familias</h1><p>Ingresa con una cuenta autorizada para administrar la plataforma.</p>
      <form @submit.prevent="entrar"><label>Correo<input v-model.trim="correo" type="email" autocomplete="username" required /></label><label>Contraseña<input v-model="clave" type="password" autocomplete="current-password" required /></label><button class="boton-principal" :disabled="cargando">{{ cargando ? 'Ingresando…' : 'Ingresar' }}</button></form>
      <p v-if="error" class="error" role="alert">{{ error }}</p><RouterLink to="/hoy">Volver a Obu Familia</RouterLink>
    </section>
  </main>

  <main v-else-if="!autorizado" class="acceso acceso--admin"><section class="panel-acceso panel-acceso--denegado"><p class="sobretitulo">Acceso restringido</p><h1>Esta cuenta no administra la plataforma</h1><p>Puedes continuar usando únicamente las familias a las que perteneces.</p><RouterLink class="boton-principal enlace-boton" to="/hoy">Ir a Obu Familia</RouterLink><button type="button" class="boton-secundario" @click="salir">Cerrar sesión</button></section></main>

  <main v-else class="admin-plataforma">
    <header class="admin-cabecera"><div><p class="sobretitulo">Administración de plataforma</p><h1>Familias</h1><p>Registro y acceso de los grupos familiares de OBU System.</p></div><div class="admin-cabecera__acciones"><button type="button" class="boton-principal" @click="abrir(dialogoFamilia, $event)">Nueva familia</button><button type="button" class="boton-admin-salir" @click="salir">Cerrar sesión</button></div></header>

    <section class="admin-registro" aria-labelledby="titulo-registro-familias">
      <div class="admin-registro__titulo"><div><span class="etiqueta etiqueta--verde">Registro activo</span><h2 id="titulo-registro-familias">{{ familias.length }} {{ familias.length === 1 ? 'familia' : 'familias' }}</h2></div><RouterLink to="/hoy">Abrir Obu Familia</RouterLink></div>
      <div v-if="familias.length" class="admin-familias">
        <button v-for="familia in familias" :key="familia.id" type="button" class="admin-familia" :class="{ 'admin-familia--activa': familiaActiva?.id === familia.id }" :aria-pressed="familiaActiva?.id === familia.id" @click="seleccionarFamilia(familia)"><span class="admin-familia__marca" aria-hidden="true">F</span><span class="admin-familia__datos"><strong>{{ familia.nombre }}</strong><span>{{ familia.zonaHoraria }}</span><small>Creada {{ fecha(familia.creadaEn) }}</small></span><span class="estado">{{ familiaActiva?.id === familia.id ? 'Seleccionada' : 'Gestionar' }}</span></button>
      </div>
      <p v-else class="estado-vacio">Todavía no hay familias registradas.</p>
      <p v-if="error" class="error" role="alert">{{ error }}</p>

      <section v-if="familiaActiva" id="miembros-familia" class="admin-miembros" aria-labelledby="titulo-miembros">
        <div class="admin-contexto-familia"><span class="admin-familia__marca" aria-hidden="true">F</span><div><small>Administrando familia</small><strong>{{ familiaActiva.nombre }}</strong></div></div>
        <div class="admin-miembros__titulo"><div><h2 id="titulo-miembros">Miembros con acceso</h2><p>Los cambios de esta sección se aplican solo a {{ familiaActiva.nombre }}.</p></div><button type="button" class="boton-principal" @click="abrirNuevoMiembro">Añadir miembro</button></div>
        <p class="ayuda-admin">Los dependientes sin cuenta se añaden desde Familia en la aplicación. Aquí se administran únicamente personas que ingresarán con correo y contraseña.</p>
        <div v-if="miembros.length" class="admin-miembros__lista">
          <article v-for="miembro in miembros" :key="miembro.perfilId" class="admin-miembro">
            <div><h3>{{ miembro.nombre }}</h3><p>{{ cuenta(miembro)?.correo ?? invitacion(miembro)?.correo ?? 'Sin correo activado' }}</p><small>{{ miembro.permiso === 'ADMINISTRADOR_FAMILIAR' ? 'Administrador familiar' : 'Adulto' }}</small></div>
            <span v-if="!miembro.activo" class="estado estado--baja">Sin acceso</span><span v-else-if="cuenta(miembro)" class="estado estado--activo">Acceso activo</span><span v-else-if="invitacion(miembro)" class="estado">{{ invitacion(miembro)?.estado.toLowerCase() }}</span><span v-else class="estado">Sin invitación</span>
            <div class="admin-miembro__acciones"><button type="button" class="boton-secundario" :disabled="cargando" @click="abrirGestion(miembro, $event)">Gestionar</button><button v-if="miembro.activo && cuenta(miembro)" type="button" class="boton-secundario" :disabled="cargando" @click="restablecer(miembro, $event)">Restablecer acceso</button><button v-else-if="miembro.activo && invitacion(miembro)?.estado === 'PENDIENTE'" type="button" class="boton-secundario" :disabled="cargando" @click="revocar(invitacion(miembro)!)">Revocar invitación</button><button v-else-if="miembro.activo && !cuenta(miembro)" type="button" class="boton-secundario" :disabled="cargando" @click="abrirInvitacion(miembro, $event)">Generar invitación</button></div>
          </article>
        </div>
        <p v-else class="estado-vacio">Aún no hay miembros con acceso. Añade al primer adulto y comparte su enlace.</p>
      </section>
    </section>

    <dialog ref="dialogoFamilia" class="dialogo dialogo--formulario" aria-labelledby="titulo-nueva-familia" @cancel.prevent="cerrar(dialogoFamilia)"><div class="titulo-seccion dialogo__cabecera"><h2 id="titulo-nueva-familia">Nueva familia</h2><button type="button" class="cerrar" aria-label="Cerrar formulario" @click="cerrar(dialogoFamilia)">×</button></div><form id="formulario-familia-plataforma" @submit.prevent="guardarFamilia"><label>Nombre de la familia<input v-model.trim="nuevaFamilia.nombre" autofocus maxlength="120" required /></label><label>Zona horaria<select v-model="nuevaFamilia.zonaHoraria" required><option value="America/Lima">Lima</option><option value="America/Bogota">Bogotá</option><option value="America/Mexico_City">Ciudad de México</option><option value="America/New_York">Nueva York</option><option value="Europe/Madrid">Madrid</option></select></label></form><footer class="dialogo__acciones"><button type="button" class="boton-secundario" :disabled="cargando" @click="cerrar(dialogoFamilia)">Cancelar</button><button type="submit" form="formulario-familia-plataforma" class="boton-principal" :disabled="cargando">{{ cargando ? 'Creando…' : 'Crear familia' }}</button></footer></dialog>

    <dialog ref="dialogoMiembro" class="dialogo dialogo--formulario" aria-labelledby="titulo-nuevo-miembro" @cancel.prevent="cerrar(dialogoMiembro)"><div class="titulo-seccion dialogo__cabecera"><h2 id="titulo-nuevo-miembro">{{ miembroExistente ? 'Nueva invitación' : 'Añadir miembro' }}</h2><button type="button" class="cerrar" aria-label="Cerrar formulario" @click="cerrar(dialogoMiembro)">×</button></div><p class="dialogo__familia">Se añadirá a <strong>{{ familiaActiva?.nombre }}</strong></p><form id="formulario-miembro-plataforma" @submit.prevent="guardarMiembro"><label>Nombre visible<input v-model.trim="nuevoMiembro.nombre" :disabled="!!miembroExistente" autofocus maxlength="120" required /></label><label>Correo de acceso<input v-model.trim="nuevoMiembro.correo" type="email" autocomplete="off" maxlength="320" required /></label><label v-if="!miembroExistente">Permiso<select v-model="nuevoMiembro.permiso"><option value="ADULTO">Adulto</option><option value="ADMINISTRADOR_FAMILIAR">Administrador familiar</option></select></label><p class="ayuda-admin">Se generará un enlace válido por 48 horas. La persona elegirá su contraseña y el enlace se invalidará al usarlo.</p><p v-if="error" class="error" role="alert">{{ error }}</p></form><footer class="dialogo__acciones"><button type="button" class="boton-secundario" :disabled="cargando" @click="cerrar(dialogoMiembro)">Cancelar</button><button type="submit" form="formulario-miembro-plataforma" class="boton-principal" :disabled="cargando">{{ cargando ? 'Generando…' : 'Generar enlace' }}</button></footer></dialog>

    <dialog ref="dialogoGestion" class="dialogo dialogo--formulario" aria-labelledby="titulo-gestionar-miembro" @cancel.prevent="cerrar(dialogoGestion)"><div class="titulo-seccion dialogo__cabecera"><div><p class="sobretitulo">{{ familiaActiva?.nombre }}</p><h2 id="titulo-gestionar-miembro">Gestionar a {{ miembroGestionado?.nombre }}</h2></div><button type="button" class="cerrar" aria-label="Cerrar gestión" @click="cerrar(dialogoGestion)">×</button></div><form id="formulario-gestion-miembro" @submit.prevent="guardarGestion"><label>Rol en esta familia<select v-model="gestionMiembro.permiso"><option value="ADULTO">Adulto</option><option value="ADMINISTRADOR_FAMILIAR">Administrador familiar</option></select></label><fieldset class="admin-estado-acceso"><legend>Acceso a {{ familiaActiva?.nombre }}</legend><label><input v-model="gestionMiembro.activo" type="radio" :value="true" /> Activo</label><label><input v-model="gestionMiembro.activo" type="radio" :value="false" /> Dar de baja</label></fieldset><p v-if="gestionMiembro.activo" class="ayuda-admin">El rol se aplica solamente dentro de esta familia.</p><p v-else class="aviso-baja">La persona perderá el acceso a esta familia. Su cuenta y el historial familiar no se borrarán.</p><p v-if="error" class="error" role="alert">{{ error }}</p></form><footer class="dialogo__acciones"><button type="button" class="boton-secundario" :disabled="cargando" @click="cerrar(dialogoGestion)">Cancelar</button><button type="submit" form="formulario-gestion-miembro" class="boton-principal" :class="{ 'boton-peligro': !gestionMiembro.activo }" :disabled="cargando">{{ cargando ? 'Guardando…' : gestionMiembro.activo ? 'Guardar cambios' : 'Dar de baja' }}</button></footer></dialog>

    <dialog ref="dialogoEnlace" class="dialogo dialogo--enlace" aria-labelledby="titulo-enlace" @cancel.prevent="cerrar(dialogoEnlace)"><div class="titulo-seccion dialogo__cabecera"><h2 id="titulo-enlace">Enlace de un solo uso</h2><button type="button" class="cerrar" aria-label="Cerrar enlace" @click="cerrar(dialogoEnlace)">×</button></div><p>{{ enlaceGenerado?.tipo === 'INVITACION' ? 'Comparte este enlace con la persona invitada.' : 'Envía este enlace a la persona que recuperará su acceso.' }}</p><label>Enlace<input :value="enlaceCompleto" readonly @focus="($event.target as HTMLInputElement).select()" /></label><p class="ayuda-admin">{{ enlaceGenerado?.tipo === 'INVITACION' ? 'Vence en 48 horas.' : 'Vence en 30 minutos.' }} Solo funciona una vez.</p><footer class="dialogo__acciones"><button type="button" class="boton-secundario" @click="cerrar(dialogoEnlace)">Cerrar</button><button type="button" class="boton-principal" @click="copiarEnlace">{{ copiado ? 'Copiado' : 'Copiar enlace' }}</button></footer></dialog>
  </main>
</template>
