const MAXIMO_BYTES = 12 * 1024 * 1024
const MAXIMO_LADO = 1600

export function validarImagenReceta(archivo: File) {
  if (!['image/jpeg', 'image/png'].includes(archivo.type)) throw new Error('Selecciona una imagen JPEG o PNG.')
  if (!archivo.size || archivo.size > MAXIMO_BYTES) throw new Error('La imagen debe pesar menos de 12 MiB.')
}

export async function reducirImagenReceta(archivo: File): Promise<Blob> {
  validarImagenReceta(archivo)
  const imagen = await createImageBitmap(archivo)
  try {
    const factor = Math.min(1, MAXIMO_LADO / Math.max(imagen.width, imagen.height))
    const ancho = Math.max(1, Math.round(imagen.width * factor))
    const alto = Math.max(1, Math.round(imagen.height * factor))
    const lienzo = document.createElement('canvas')
    lienzo.width = ancho
    lienzo.height = alto
    const contexto = lienzo.getContext('2d')
    if (!contexto) throw new Error('Este navegador no puede preparar la fotografía.')
    contexto.fillStyle = '#ffffff'
    contexto.fillRect(0, 0, ancho, alto)
    contexto.drawImage(imagen, 0, 0, ancho, alto)
    return await new Promise<Blob>((resolver, rechazar) => lienzo.toBlob(
      blob => blob ? resolver(blob) : rechazar(new Error('No se pudo reducir la fotografía.')),
      'image/jpeg', .86
    ))
  } finally {
    imagen.close()
  }
}
