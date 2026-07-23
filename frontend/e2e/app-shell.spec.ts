import { expect, test, type Page } from '@playwright/test'
import AxeBuilder from '@axe-core/playwright'

async function prepararApi(page: Page) {
  const perfil = { id: 'perfil-1', nombre: 'Mamá', tipo: 'ADULTO', color: '#315b4c', relacion: 'Mamá' }
  const tratamientos = Array.from({ length: 6 }, (_, indice) => ({
    id: `tratamiento-${indice}`, perfilId: perfil.id, persona: 'Mamá', medicamento: `Tratamiento ${indice + 1}`,
    responsable: 'Mamá', horarios: ['08:00:00'], estado: 'ACTIVO'
  }))
  const medicamentos = Array.from({ length: 6 }, (_, indice) => ({
    id: `medicamento-${indice}`, loteId: `lote-${indice}`, nombre: `Medicamento ${indice + 1}`,
    presentacion: 'Tabletas', concentracion: '500 mg', cantidad: 10, unidad: 'tabletas', estado: 'DISPONIBLE'
  }))
  await page.route('**/api/v1/**', async route => {
    const ruta = new URL(route.request().url()).pathname
    const responder = (datos: unknown) => route.fulfill({ contentType: 'application/json', body: JSON.stringify(datos) })
    if (ruta.endsWith('/autenticacion/renovar')) return responder({ accessToken: 'token-e2e', expiraEn: '2099-01-01T00:00:00Z', usuarioId: 'usuario-1', correo: 'mama@familia.test' })
    if (ruta.endsWith('/hoy')) return responder({ familiaId: 'familia-1', familia: 'Familia Herrera', zonaHoraria: 'America/Lima', perfiles: [perfil], tareas: [] })
    if (ruta.endsWith('/catalogo')) return responder({ medicamentos, tratamientos, eventos: [], lugares: [] })
    if (ruta.endsWith('/ocurrencias')) return responder({ ocurrencias: [], revisar: [] })
    if (ruta.endsWith('/auditoria')) return responder({ entradas: [] })
    if (ruta.endsWith('/configuracion')) return responder({ puedeAdministrar: true, perfiles: [{ ...perfil, activo: true, permiso: 'ADMINISTRADOR_FAMILIAR' }] })
    if (ruta.endsWith('/archivos/cuota')) return responder({ cuotaBytes: 1_000_000, usadosBytes: 0, disponiblesBytes: 1_000_000, porcentaje: 0, nivel: 'NORMAL' })
    return route.fulfill({ status: 404, contentType: 'application/json', body: '{}' })
  })
}

test.beforeEach(async ({ page }) => prepararApi(page))

test('navega por destinos reales y muestra solo el dominio activo', async ({ page }) => {
  await page.goto('/hoy')
  await expect(page.getByRole('link', { name: /Hoy/ })).toHaveAttribute('aria-current', 'page')
  await expect(page.getByRole('heading', { name: 'Tareas de hoy' })).toBeVisible()
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

test('mantiene Familia y Actividad fuera de la navegación principal', async ({ page }) => {
  await page.goto('/salud')
  await expect(page.getByRole('heading', { name: 'Tomas', exact: true })).toBeVisible()
  await page.locator('button[aria-label="Abrir menú de familia"]').click()
  await page.getByRole('link', { name: 'Familia y permisos' }).click()
  await expect(page).toHaveURL(/\/ajustes\/familia$/)
  await expect(page.getByRole('heading', { name: 'Perfiles y permisos' })).toBeVisible()
  await expect(page.getByRole('navigation', { name: 'Navegación principal' }).getByText('Objetos')).toHaveCount(0)
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

test('no presenta violaciones críticas o serias de accesibilidad', async ({ page }) => {
  for (const ruta of ['/hoy', '/agenda', '/salud', '/ajustes/familia', '/actividad']) {
    await page.goto(ruta)
    await expect(page.locator('.estado-carga')).toHaveCount(0)
    const resultado = await new AxeBuilder({ page }).analyze()
    const graves = resultado.violations.filter(violacion => violacion.impact === 'critical' || violacion.impact === 'serious')
    expect(graves, `${ruta}: ${graves.map(violacion => violacion.id).join(', ')}`).toEqual([])
  }
})
