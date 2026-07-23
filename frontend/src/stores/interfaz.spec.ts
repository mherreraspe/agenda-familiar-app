import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { useInterfazStore } from './interfaz'

describe('estado compartido de interfaz', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('conserva el filtro de persona entre vistas', () => {
    const store = useInterfazStore()
    store.seleccionarPerfil('perfil-mama')
    expect(useInterfazStore().filtroPerfil).toBe('perfil-mama')
  })
})
