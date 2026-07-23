import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router'

export function useFormularioRuta(tipo: string) {
  const route = useRoute()
  const router = useRouter()
  const abierto = computed(() => route.query.crear === tipo)
  const abiertoDesdeAplicacion = ref(false)
  const modificado = ref(false)
  let activador: HTMLElement | null = null
  let omitirGuardia = false
  let confirmarDescarte: (() => Promise<boolean>) | null = null

  function registrarConfirmacion(confirmar: () => Promise<boolean>) {
    confirmarDescarte = confirmar
  }

  async function confirmarSiHaceFalta() {
    return !modificado.value || !confirmarDescarte || await confirmarDescarte()
  }

  async function abrir(elemento?: HTMLElement | null) {
    activador = elemento ?? document.activeElement as HTMLElement | null
    abiertoDesdeAplicacion.value = true
    await router.push({ query: { ...route.query, crear: tipo } })
  }

  async function cerrar(forzar = false) {
    if (!abierto.value) return
    if (!forzar && !await confirmarSiHaceFalta()) return
    omitirGuardia = true
    if (abiertoDesdeAplicacion.value) {
      router.back()
    } else {
      const query = { ...route.query }
      delete query.crear
      await router.replace({ query })
    }
  }

  onBeforeRouteUpdate(async (to, from) => {
    if (from.query.crear !== tipo || to.query.crear === tipo) return true
    if (omitirGuardia) {
      omitirGuardia = false
      return true
    }
    return confirmarSiHaceFalta()
  })

  watch(abierto, async estaAbierto => {
    if (!estaAbierto) {
      abiertoDesdeAplicacion.value = false
      modificado.value = false
      await nextTick()
      activador?.focus()
      activador = null
    }
  })

  onBeforeUnmount(() => { confirmarDescarte = null })

  return { abierto, modificado, abrir, cerrar, registrarConfirmacion }
}
