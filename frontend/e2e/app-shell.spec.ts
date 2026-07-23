import { expect, test, type Page } from '@playwright/test'
import AxeBuilder from '@axe-core/playwright'

async function prepararApi(page: Page) {
  const perfil = { id: 'perfil-1', nombre: 'Mamá', tipo: 'ADULTO', color: '#315b4c', relacion: 'Mamá' }
  const tratamientos = Array.from({ length: 6 }, (_, indice) => ({
    id: `tratamiento-${indice}`, perfilId: perfil.id, persona: 'Mamá', medicamento: `Tratamiento ${indice + 1}`,
    responsable: 'Mamá', horarios: ['08:00:00'], estado: 'ACTIVO', recetaId: indice === 0 ? 'receta-1' : undefined
  }))
  const medicamentos = Array.from({ length: 6 }, (_, indice) => ({
    id: `medicamento-${indice}`, loteId: `lote-${indice}`, nombre: `Medicamento ${indice + 1}`,
    presentacion: 'Tabletas', concentracion: '500 mg', cantidad: 10, unidad: 'tabletas', estado: 'DISPONIBLE'
  }))
  const historialTomas = Array.from({ length: 20 }, (_, indice) => ({
    id: `toma-${indice}`, perfilId: perfil.id, persona: 'Mamá', tratamiento: `Tratamiento ${indice + 1}`,
    estado: 'TOMADA', programadaEn: `2026-07-22T${String(indice % 20).padStart(2, '0')}:00:00Z`,
    resueltaEn: `2026-07-22T${String(indice % 20).padStart(2, '0')}:04:00Z`, resueltaPorNombre: 'Mamá'
  }))
  const objetos = [{
    id: 'objeto-1', nombre: 'Pasaporte de Lucía', categoria: 'Documentos', notas: 'Funda azul',
    ruta: ['Habitación principal', 'Ropero', 'Caja de documentos'], actualizadoEn: '2026-07-23T18:00:00Z', version: 0
  }]
  await page.route('**/api/v1/**', async route => {
    const ruta = new URL(route.request().url()).pathname
    const responder = (datos: unknown) => route.fulfill({ contentType: 'application/json', body: JSON.stringify(datos) })
    if (ruta.endsWith('/autenticacion/renovar')) return responder({ accessToken: 'token-e2e', expiraEn: '2099-01-01T00:00:00Z', usuarioId: 'usuario-1', correo: 'mama@familia.test' })
    if (ruta.endsWith('/hoy')) return responder({ familiaId: 'familia-1', familia: 'Familia Herrera', zonaHoraria: 'America/Lima', perfiles: [perfil], tareas: [] })
    if (ruta.endsWith('/catalogo')) return responder({ medicamentos, tratamientos, eventos: [], lugares: [] })
    if (ruta.endsWith('/ocurrencias')) return responder({ ocurrencias: historialTomas, revisar: [] })
    if (ruta.endsWith('/auditoria')) return responder({ entradas: [] })
    if (ruta.endsWith('/configuracion')) return responder({ puedeAdministrar: true, perfiles: [{ ...perfil, activo: true, permiso: 'ADMINISTRADOR_FAMILIAR' }] })
    if (ruta.endsWith('/archivos/cuota')) return responder({ cuotaBytes: 1_000_000, usadosBytes: 0, disponiblesBytes: 1_000_000, porcentaje: 0, nivel: 'NORMAL' })
    if (ruta.endsWith('/archivos/receta-1')) return route.fulfill({ status: 200, contentType: 'image/jpeg', body: '' })
    if (ruta.endsWith('/objetos') && route.request().method() === 'GET') return responder({ objetos, ubicaciones: [{ ruta: objetos[0].ruta, cantidad: 1 }] })
    if (ruta.endsWith('/objetos') && route.request().method() === 'POST') return responder({ id: 'objeto-2' })
    if (/\/objetos\/[^/]+$/.test(ruta) && route.request().method() === 'PATCH') return route.fulfill({ status: 204 })
    return route.fulfill({ status: 404, contentType: 'application/json', body: '{}' })
  })
}

test.beforeEach(async ({ page }) => prepararApi(page))

test('navega por destinos reales y muestra solo el dominio activo', async ({ page }) => {
  await page.goto('/hoy')
  await expect(page.getByRole('link', { name: /Hoy/ })).toHaveAttribute('aria-current', 'page')
  await expect(page.getByRole('heading', { name: 'Todo está al día' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Por resolver' })).toHaveCount(0)
  await expect(page.getByText('Ver historial', { exact: true })).toHaveCount(0)
  await expect(page.getByRole('heading', { name: 'Medicamentos' })).toHaveCount(0)

  await page.getByRole('link', { name: 'Agenda' }).click()
  await expect(page).toHaveURL(/\/agenda$/)
  await expect(page.getByRole('link', { name: 'Agenda' })).toHaveAttribute('aria-current', 'page')
  await expect(page.getByRole('heading', { name: 'Próximos eventos' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Tomas', exact: true })).toHaveCount(0)

  await page.getByRole('link', { name: 'Salud' }).click()
  await expect(page).toHaveURL(/\/salud$/)
  await expect(page.getByRole('heading', { name: 'Tomas', exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Tratamientos' })).toHaveCount(0)
  await expect(page.getByRole('heading', { name: 'Próximos eventos' })).toHaveCount(0)
})

test('Salud muestra una sola subsección y conserva la selección en la URL', async ({ page }) => {
  await page.goto('/salud')
  await expect(page.getByRole('navigation', { name: 'Secciones de Salud' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Tomas', exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Medicamentos' })).toHaveCount(0)

  await page.getByRole('link', { name: 'Tratamientos', exact: true }).click()
  await expect(page).toHaveURL(/\/salud\?seccion=tratamientos$/)
  await expect(page.getByRole('heading', { name: 'Tratamientos' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Tomas', exact: true })).toHaveCount(0)
  await expect(page.locator('#tratamientos article.tarjeta')).toHaveCount(5)
  await page.getByRole('button', { name: 'Ver 1 más' }).click()
  await expect(page.locator('#tratamientos article.tarjeta')).toHaveCount(6)

  await page.getByRole('link', { name: 'Botiquín', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Medicamentos' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Tratamientos' })).toHaveCount(0)
  await expect(page.locator('#botiquin article.tarjeta')).toHaveCount(5)

  await page.getByRole('link', { name: 'Recetas', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Recetas' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Medicamentos' })).toHaveCount(0)
  await expect(page.getByText(/Espacio usado/)).toHaveCount(0)
})

test('abre el historial de Tomas como vista paginada y no dentro de Hoy', async ({ page }) => {
  await page.goto('/salud')
  await page.getByRole('link', { name: 'Ver historial', exact: true }).click()
  await expect(page).toHaveURL(/vista=historial/)
  await expect(page.getByRole('heading', { name: 'Historial de tomas' })).toBeVisible()
  await expect(page.locator('.historial-tomas article.tarjeta')).toHaveCount(10)
  await page.getByRole('button', { name: 'Ver 10 más' }).click()
  await expect(page.locator('.historial-tomas article.tarjeta')).toHaveCount(20)
})

test('los menús de cabecera son mutuamente exclusivos', async ({ page }) => {
  await page.goto('/salud')
  await expect(page.getByRole('heading', { name: 'Tomas', exact: true })).toBeVisible()
  const botonAnadir = page.locator('button.boton-anadir')
  const botonAvatar = page.locator('button[aria-label="Abrir menú de familia"]')

  await botonAnadir.click()
  await expect(botonAnadir).toHaveAttribute('aria-expanded', 'true')
  await botonAvatar.click()
  await expect(botonAnadir).toHaveAttribute('aria-expanded', 'false')
  await expect(botonAvatar).toHaveAttribute('aria-expanded', 'true')
  await expect(page.locator('.menu-desplegable__panel')).toHaveCount(1)

  await page.keyboard.press('Escape')
  await expect(botonAvatar).toHaveAttribute('aria-expanded', 'false')
})

test('Tratamiento cabe en pantalla y bloquea cualquier segunda alta', async ({ page }, testInfo) => {
  await page.goto('/salud?seccion=tratamientos')
  await page.getByRole('button', { name: 'Tratamiento', exact: true }).click()

  const dialogo = page.getByRole('dialog', { name: 'Nuevo tratamiento' })
  await expect(dialogo).toBeVisible()
  await expect(dialogo.locator('form')).toHaveCSS('overflow-y', 'auto')
  await expect(dialogo.locator('.dialogo__acciones')).toBeVisible()
  await expect(dialogo.getByRole('button', { name: 'Guardar tratamiento' })).toBeInViewport()
  expect(await page.locator('dialog:modal').count()).toBe(1)
  expect(await page.evaluate(() => document.activeElement?.closest('dialog')?.matches(':modal'))).toBe(true)

  const caja = await dialogo.boundingBox()
  expect(caja).not.toBeNull()
  expect(caja!.y).toBeGreaterThanOrEqual(0)
  expect(caja!.y + caja!.height).toBeLessThanOrEqual(testInfo.project.use.viewport!.height)

  let fondoBloqueado = false
  try {
    await page.locator('button[aria-label="Abrir menú de familia"]').click({ timeout: 500 })
  } catch {
    fondoBloqueado = true
  }
  expect(fondoBloqueado).toBe(true)
  expect(await page.locator('dialog:modal').count()).toBe(1)

  await page.keyboard.press('Escape')
  await expect(dialogo).toBeHidden()
  await expect(page.locator('button[aria-label="Abrir menú de familia"]')).toBeEnabled()
})

test('todas las altas generales son modales, exclusivas y restauran el foco', async ({ page }) => {
  const casos = [
    { ruta: '/objetos', boton: 'Objeto', dialogo: 'Nuevo objeto' },
    { ruta: '/salud?seccion=botiquin', boton: 'Medicamento', dialogo: 'Nuevo medicamento' },
    { ruta: '/ajustes/familia', boton: 'Agregar', dialogo: 'Nuevo perfil' }
  ]

  for (const caso of casos) {
    await page.goto(caso.ruta)
    const activador = page.getByRole('button', { name: caso.boton, exact: true })
    await activador.click()
    await expect(page.getByRole('dialog', { name: caso.dialogo })).toBeVisible()
    expect(await page.locator('dialog:modal').count()).toBe(1)
    await page.keyboard.press('Escape')
    await expect(page.getByRole('dialog', { name: caso.dialogo })).toBeHidden()
    await expect(activador).toBeFocused()
  }

  await page.goto('/hoy')
  const anadir = page.locator('button.boton-anadir')
  await anadir.click()
  await page.getByRole('button', { name: 'Tarea o recordatorio', exact: true }).click()
  await expect(page.getByRole('dialog', { name: 'Nueva tarea' })).toBeVisible()
  expect(await page.locator('dialog:modal').count()).toBe(1)
  await page.keyboard.press('Escape')
  await expect(anadir).toBeFocused()
})

test('solo permite un menú Más abierto y lo cierra al pulsar fuera', async ({ page }) => {
  await page.goto('/salud?seccion=tratamientos')
  const activadores = page.locator('.menu-mas > summary')
  expect(await activadores.count()).toBeGreaterThan(1)
  await activadores.nth(0).click()
  await expect(page.locator('.menu-mas[open]')).toHaveCount(1)
  await page.getByRole('heading', { name: 'Tratamientos' }).click()
  await expect(page.locator('.menu-mas[open]')).toHaveCount(0)
  await activadores.nth(1).click()
  await expect(page.locator('.menu-mas[open]')).toHaveCount(1)
  await page.keyboard.press('Escape')
  await expect(page.locator('.menu-mas[open]')).toHaveCount(0)
})

test('la receta privada también usa una única capa modal', async ({ page }) => {
  await page.goto('/salud?seccion=tratamientos')
  const tratamientoConReceta = page
    .locator('#tratamientos article.tarjeta')
    .filter({ has: page.getByText('Tratamiento 1', { exact: true }) })
  await tratamientoConReceta.getByText('Ver detalles', { exact: true }).click()
  await tratamientoConReceta.getByRole('button', { name: 'Ver receta', exact: true }).click()
  const dialogo = page.getByRole('dialog', { name: 'Receta privada' })
  await expect(dialogo).toBeVisible()
  expect(await page.locator('dialog:modal').count()).toBe(1)
  await page.keyboard.press('Escape')
  await expect(dialogo).toBeHidden()
})

test('mantiene Familia y Actividad fuera de la navegación principal', async ({ page }) => {
  await page.goto('/salud')
  await expect(page.getByRole('heading', { name: 'Tomas', exact: true })).toBeVisible()
  await page.locator('button[aria-label="Abrir menú de familia"]').click()
  await page.getByRole('link', { name: 'Familia y permisos' }).click()
  await expect(page).toHaveURL(/\/ajustes\/familia$/)
  await expect(page.getByRole('heading', { name: 'Perfiles y permisos' })).toBeVisible()
  await expect(page.getByRole('navigation', { name: 'Navegación principal' }).getByText('Objetos')).toBeVisible()
})

test('busca y guarda Objetos con una ubicación jerárquica persistente', async ({ page }) => {
  await page.goto('/objetos')
  await expect(page.getByRole('heading', { name: 'Objetos de la familia' })).toBeVisible()
  await expect(page.getByRole('region', { name: 'Objetos de la familia' }).getByText('Habitación principal › Ropero › Caja de documentos', { exact: true })).toBeVisible()
  const medidas = await page.evaluate(() => ({ ancho: document.documentElement.scrollWidth, viewport: window.innerWidth }))
  expect(medidas.ancho).toBeLessThanOrEqual(medidas.viewport)

  const consulta = page.waitForRequest(request => new URL(request.url()).searchParams.get('q') === 'pasaporte')
  await page.getByRole('searchbox', { name: '¿Qué estás buscando?' }).fill('pasaporte')
  await consulta
  await expect(page.getByRole('heading', { name: 'Objetos encontrados' })).toBeVisible()

  await page.getByRole('button', { name: 'Objeto', exact: true }).click()
  const dialogo = page.getByRole('dialog')
  await dialogo.getByLabel('Nombre').fill('Llaves de repuesto')
  await dialogo.getByLabel('Categoría').fill('Llaves')
  await dialogo.getByLabel('Ruta de ubicación').fill('Entrada › Cajón')
  const alta = page.waitForRequest(request => request.method() === 'POST' && new URL(request.url()).pathname.endsWith('/objetos'))
  await dialogo.getByRole('button', { name: 'Guardar objeto' }).click()
  const solicitud = await alta
  expect(solicitud.postDataJSON()).toMatchObject({ nombre: 'Llaves de repuesto', categoria: 'Llaves', ruta: ['Entrada', 'Cajón'] })
  await page.goto('about:blank')
})

test('redirige rutas antiguas y evita desbordamiento horizontal', async ({ page }) => {
  await page.goto('/botiquin')
  await expect(page).toHaveURL(/\/salud\?seccion=botiquin$/)
  const medidas = await page.evaluate(() => ({ ancho: document.documentElement.scrollWidth, viewport: window.innerWidth }))
  expect(medidas.ancho).toBeLessThanOrEqual(medidas.viewport)
})

test('carga solo los recursos requeridos por cada dominio', async ({ page }) => {
  const solicitudes: string[] = []
  page.on('request', request => {
    const ruta = new URL(request.url()).pathname
    if (ruta.startsWith('/api/v1/')) solicitudes.push(ruta)
  })

  await page.goto('/agenda')
  await expect(page.getByRole('heading', { name: 'Próximos eventos' })).toBeVisible()
  expect(solicitudes.some(ruta => ruta.endsWith('/hoy'))).toBe(true)
  expect(solicitudes.some(ruta => ruta.endsWith('/catalogo'))).toBe(true)
  expect(solicitudes.some(ruta => ruta.endsWith('/ocurrencias'))).toBe(false)
  expect(solicitudes.some(ruta => ruta.endsWith('/auditoria'))).toBe(false)
  expect(solicitudes.some(ruta => ruta.endsWith('/configuracion'))).toBe(false)
  expect(solicitudes.some(ruta => ruta.endsWith('/archivos/cuota'))).toBe(false)

  solicitudes.length = 0
  await page.getByRole('link', { name: 'Salud' }).click()
  await expect(page.getByRole('heading', { name: 'Tomas', exact: true })).toBeVisible()
  expect(solicitudes.some(ruta => ruta.endsWith('/ocurrencias'))).toBe(true)
  expect(solicitudes.some(ruta => ruta.endsWith('/archivos/cuota'))).toBe(true)
  expect(solicitudes.some(ruta => ruta.endsWith('/auditoria'))).toBe(false)
})

test('invalida Hoy al recibir un cambio familiar y descarta repeticiones', async ({ page }) => {
  let consultasHoy = 0
  page.on('request', request => {
    if (new URL(request.url()).pathname.endsWith('/hoy')) consultasHoy += 1
  })
  await page.route('**/familias/*/eventos', route => route.fulfill({
    contentType: 'text/event-stream',
    body: [
      'id: inicial', 'event: sincronizar', 'data: {"id":"inicial","recursos":["HOY","AGENDA","SALUD"]}', '',
      'id: cambio-1', 'event: cambio', 'data: {"id":"cambio-1","recursos":["HOY"]}', '',
      'id: cambio-1', 'event: cambio', 'data: {"id":"cambio-1","recursos":["HOY"]}', '', ''
    ].join('\n')
  }))

  await page.goto('/hoy')
  await expect.poll(() => consultasHoy).toBeGreaterThanOrEqual(2)
})

test('refleja cambios de Objetos entre dispositivos sin recargar la página', async ({ page }) => {
  let consultasObjetos = 0
  page.on('request', request => {
    if (new URL(request.url()).pathname.endsWith('/objetos')) consultasObjetos += 1
  })
  await page.route('**/familias/*/eventos', route => route.fulfill({
    contentType: 'text/event-stream',
    body: [
      'id: inicial', 'event: sincronizar', 'data: {"id":"inicial","recursos":["HOY","AGENDA","SALUD","OBJETOS"]}', '',
      'id: objeto-1', 'event: cambio', 'data: {"id":"objeto-1","recursos":["OBJETOS"]}', '', ''
    ].join('\n')
  }))

  await page.goto('/objetos')
  await expect.poll(() => consultasObjetos).toBeGreaterThanOrEqual(2)
})

test('avisa cuando no hay conexión y mantiene las altas solo en línea', async ({ page, context }) => {
  await page.goto('/hoy')
  await expect(page.getByRole('heading', { name: 'Todo está al día' })).toBeVisible()
  await context.setOffline(true)

  await expect(page.getByText(/Sin conexión: puedes consultar lo ya cargado/)).toBeVisible()
  await page.locator('button.boton-anadir').click()
  await page.getByRole('button', { name: 'Evento', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('Vuelve a estar en línea')
  await expect(page.locator('.formulario-adaptativo')).not.toBeVisible()
  await context.setOffline(false)
})

test('no presenta violaciones críticas o serias de accesibilidad', async ({ page }) => {
  for (const ruta of ['/hoy', '/agenda', '/salud', '/objetos', '/ajustes/familia', '/actividad']) {
    await page.goto(ruta)
    await expect(page.locator('.estado-carga')).toHaveCount(0)
    const resultado = await new AxeBuilder({ page }).analyze()
    const graves = resultado.violations.filter(violacion => violacion.impact === 'critical' || violacion.impact === 'serious')
    expect(graves, `${ruta}: ${graves.map(violacion => violacion.id).join(', ')}`).toEqual([])
  }
})
