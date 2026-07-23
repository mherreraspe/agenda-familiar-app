const PATRON_FECHA_LOCAL = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})$/

function partesEnZona(fecha: Date, zonaHoraria: string) {
  const partes = new Intl.DateTimeFormat('en-CA', {
    timeZone: zonaHoraria,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hourCycle: 'h23'
  }).formatToParts(fecha)

  return Object.fromEntries(partes.map(parte => [parte.type, parte.value]))
}

function offsetZona(fecha: Date, zonaHoraria: string) {
  const partes = partesEnZona(fecha, zonaHoraria)
  const representacionUtc = Date.UTC(
    Number(partes.year),
    Number(partes.month) - 1,
    Number(partes.day),
    Number(partes.hour),
    Number(partes.minute),
    Number(partes.second)
  )
  return representacionUtc - fecha.getTime()
}

export function instantAFechaFamiliar(fecha: Date | string, zonaHoraria: string) {
  const partes = partesEnZona(typeof fecha === 'string' ? new Date(fecha) : fecha, zonaHoraria)
  return `${partes.year}-${partes.month}-${partes.day}T${partes.hour}:${partes.minute}`
}

export function fechaFamiliarAInstant(valor: string, zonaHoraria: string) {
  const coincidencia = PATRON_FECHA_LOCAL.exec(valor)
  if (!coincidencia) throw new Error('La fecha y hora no tienen un formato válido.')

  const [, anio, mes, dia, hora, minuto] = coincidencia
  const fechaUtc = Date.UTC(Number(anio), Number(mes) - 1, Number(dia), Number(hora), Number(minuto))
  let instante = new Date(fechaUtc)

  // Dos pasadas resuelven correctamente cambios de offset cercanos a transiciones DST.
  instante = new Date(fechaUtc - offsetZona(instante, zonaHoraria))
  instante = new Date(fechaUtc - offsetZona(instante, zonaHoraria))

  if (instantAFechaFamiliar(instante, zonaHoraria) !== valor) {
    throw new Error('La fecha y hora no existen en la zona horaria familiar.')
  }
  return instante.toISOString()
}

export function redondearSiguienteCuarto(ahora: Date, zonaHoraria: string) {
  const cuarto = 15 * 60 * 1000
  const siguiente = new Date((Math.floor(ahora.getTime() / cuarto) + 1) * cuarto)
  return instantAFechaFamiliar(siguiente, zonaHoraria)
}
