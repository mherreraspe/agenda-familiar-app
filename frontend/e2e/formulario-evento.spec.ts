import { expect, test, type Page } from '@playwright/test'

const perfil = { id: 'perfil-1', nombre: 'Mamá', tipo: 'ADULTO', color: '#315b4c', relacion: 'Mamá' }

async function prepararApi(page: Page, opciones: { errorEvento?: boolean } = {}) {
  const solicitudes: Array<Record<string, unknown>> = []
  await page.route('**/api/v1/**', async route => {
    const request = route.request()
    const url = new URL(request.url())
    const ruta = url.pathname
    const json = (datos: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(datos) })

    if (ruta.endsWith('/autenticacion/renovar')) return json({ accessToken: 'token-e2e', expiraEn: '2099-01-01T00:00:00Z', usuarioId: 'usuario-1', correo: 'mama@familia.test' })
    if (ruta.endsWith('/hoy')) return json({ familiaId: 'familia-1', familia: 'Familia Herrera', zonaHoraria: 'America/Lima', perfiles: [perfil], tareas: [] })
    if (ruta.endsWith('/catalogo')) return json({ medicamentos: [], tratamientos: [], eventos: [], lugares: [] })
    if (ruta.endsWith('/ocurrencias')) return json({ ocurrencias: [], revisar: [] })
    if (ruta.endsWith('/auditoria')) return json({ entradas: [] })
    if (ruta.endsWith('/configuracion')) return json({ puedeAdministrar: true, perfiles: [{ ...perfil, activo: true, permiso: 'ADMINISTRADOR_FAMILIAR' }] })
    if (ruta.endsWith('/archivos/cuota')) return json({ cuotaBytes: 1_000_000, usadosBytes: 0, disponiblesBytes: 1_000_000, porcentaje: 0, nivel: 'NORMAL' })
    if (ruta.endsWith('/sugerencias')) {
      const consulta = url.searchParams.get('q') ?? ''
      const tipo = consulta.toLocaleLowerCase('es-PE').startsWith('cl') ? 'LUGAR' : 'EVENTO'
      return json({ sugerencias: Array.from({ length: 5 }, (_, indice) => ({
        tipo, entidadId: String(indice), titulo: tipo === 'EVENTO' ? `Control ${indice}` : `Clínica ${indice}`,
        lugar: tipo === 'LUGAR' ? `Clínica ${indice}` : undefined, direccion: `Dirección ${indice}`
      })) })
    }
    if (ruta.endsWith('/eventos') && request.method() === 'POST') {
      solicitudes.push(request.postDataJSON())
      if (opciones.errorEvento) return json({ detail: 'El evento no cumple el contrato' }, 400)
      return json({ id: 'evento-1' }, 201)
    }
    return json({ detail: `Ruta E2E no simulada: ${ruta}` }, 404)
  })
  return solicitudes
}

async function abrirDirecto(page: Page) {
  await page.goto('/?crear=evento')
  await expect(page.getByRole('dialog', { name: 'Nuevo evento' })).toBeVisible()
}

test('mantiene acciones visibles, scroll interno y cero overflow horizontal', async ({ page }, testInfo) => {
  await prepararApi(page)
  await abrirDirecto(page)
  await page.getByText('Más opciones').click()

  const acciones = page.locator('.formulario-adaptativo__acciones')
  const guardar = page.getByRole('button', { name: 'Guardar evento' })
  await expect(acciones).toBeVisible()
  await expect(guardar).toBeInViewport()
  await expect(page.locator('.formulario-adaptativo__contenido')).toHaveCSS('overflow-y', 'auto')
  const desborde = await page.evaluate(() => ({
    anchoVentana: window.innerWidth,
    anchoDocumento: document.documentElement.scrollWidth,
    elementos: [...document.querySelectorAll<HTMLElement>('body *')]
      .filter(elemento => {
        const caja = elemento.getBoundingClientRect()
        return caja.left < 0 || caja.right > window.innerWidth
      })
      .slice(0, 8)
      .map(elemento => ({ etiqueta: elemento.tagName, clase: elemento.className, caja: elemento.getBoundingClientRect().toJSON() }))
  }))
  expect(desborde.anchoDocumento, JSON.stringify(desborde.elementos)).toBeLessThanOrEqual(desborde.anchoVentana)

  const caja = await page.getByRole('dialog', { name: 'Nuevo evento' }).boundingBox()
  expect(caja).not.toBeNull()
  expect(caja!.x).toBeGreaterThanOrEqual(0)
  expect(caja!.x + caja!.width).toBeLessThanOrEqual(testInfo.project.use.viewport!.width)
})

test('Escape cierra y limpia la query cuando el borrador está limpio', async ({ page }) => {
  await prepararApi(page)
  await abrirDirecto(page)
  await page.keyboard.press('Escape')
  await expect(page.getByRole('dialog', { name: 'Nuevo evento' })).toBeHidden()
  await expect(page).not.toHaveURL(/crear=evento/)
})

test('Atrás y Escape piden confirmación si el borrador cambió', async ({ page }) => {
  await prepararApi(page)
  await page.goto('/agenda')
  await page.locator('#calendario').getByRole('button', { name: 'Agregar' }).click()
  await page.getByLabel('Título').fill('Notaría')

  await page.goBack({ waitUntil: 'commit' }).catch(() => undefined)
  await expect(page.getByRole('dialog', { name: '¿Descartar los cambios?' })).toBeVisible()
  await page.getByRole('button', { name: 'Seguir editando' }).click()
  await expect(page.getByLabel('Título')).toHaveValue('Notaría')

  await page.keyboard.press('Escape')
  await page.getByRole('button', { name: 'Descartar' }).click()
  await expect(page.getByRole('dialog', { name: 'Nuevo evento' })).toBeHidden()
  await expect(page).not.toHaveURL(/crear=evento/)
})

test('envía eventos mínimo y recurrente con la zona familiar', async ({ page }) => {
  const solicitudes = await prepararApi(page)
  await abrirDirecto(page)
  await page.getByLabel('Título').fill('Notaría')
  await page.getByLabel('Fecha y hora').fill('2099-07-19T10:00')
  await page.getByRole('button', { name: 'Guardar evento' }).click()
  await expect.poll(() => solicitudes.length).toBe(1)
  expect(solicitudes[0]).toMatchObject({ titulo: 'Notaría', inicioEn: '2099-07-19T15:00:00.000Z' })

  await page.goto('/?crear=evento')
  await page.getByLabel('Título').fill('Reunión semanal')
  await page.getByLabel('Fecha y hora').fill('2099-07-20T10:00')
  await page.getByText('Más opciones').click()
  await page.getByLabel('Repetir evento').check()
  await page.getByLabel('Repetir hasta').fill('2099-08-20T10:00')
  await page.getByRole('button', { name: 'Guardar evento' }).click()
  await expect.poll(() => solicitudes.length).toBe(2)
  expect(solicitudes[1]).toMatchObject({ recurrencia: { frecuencia: 'SEMANAL', intervalo: 1, hasta: '2099-08-20T15:00:00.000Z' } })
})

test('conserva valores ante error 400 y limita las sugerencias', async ({ page }) => {
  await prepararApi(page, { errorEvento: true })
  await abrirDirecto(page)
  const titulo = page.getByLabel('Título')
  await titulo.fill('Co')
  await expect(page.locator('.sugerencias button')).toHaveCount(3)
  await titulo.fill('Notaría')
  await page.getByLabel('Fecha y hora').fill('2099-07-19T10:00')
  await page.getByRole('button', { name: 'Guardar evento' }).click()

  await expect(page.getByRole('alert')).toContainText('El evento no cumple el contrato')
  await expect(titulo).toHaveValue('Notaría')
})
