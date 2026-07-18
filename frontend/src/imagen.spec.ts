import { describe, expect, it } from 'vitest'
import { validarImagenReceta } from './imagen'

describe('validarImagenReceta', () => {
  it('acepta JPEG y PNG dentro del límite', () => {
    expect(() => validarImagenReceta(new File(['foto'], 'receta.jpg', { type: 'image/jpeg' }))).not.toThrow()
    expect(() => validarImagenReceta(new File(['foto'], 'receta.png', { type: 'image/png' }))).not.toThrow()
  })

  it('rechaza tipos no permitidos y archivos vacíos', () => {
    expect(() => validarImagenReceta(new File(['x'], 'receta.svg', { type: 'image/svg+xml' }))).toThrow('JPEG o PNG')
    expect(() => validarImagenReceta(new File([], 'receta.jpg', { type: 'image/jpeg' }))).toThrow('12 MiB')
  })
})
