import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useInterfazStore = defineStore('interfaz', () => {
  const filtroPerfil = ref('TODOS')

  function seleccionarPerfil(perfilId: string) {
    filtroPerfil.value = perfilId
  }

  return { filtroPerfil, seleccionarPerfil }
})
