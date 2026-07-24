import { expect, test, type Page } from '@playwright/test'
import AxeBuilder from '@axe-core/playwright'

async function prepararApi(page: Page) {
  const perfil = { id: 'perfil-1', nombre: 'Mamá', tipo: 'ADULTO', color: '#315b4c', relacion: 'Mamá' }
  const perfilHijo = { id: 'perfil-2', nombre: 'Alessio', tipo: 'DEPENDIENTE', color: '#b57b35', relacion: 'Hijo' }
  const manana = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString()
  const tareas = [{ id: 'tarea-1', perfilId: perfil.id, titulo: 'Comprar ingredientes', descripcion: 'Para la cena',
    responsable: 'Mamá', fechaLimite: manana, estado: 'PENDIENTE', recurrente: false }]
  const eventos = [
    { id: 'evento-cita', perfilId: perfil.id, persona: 'Mamá', titulo: 'Control dental', tipo: 'CITA',
      lugar: 'Clínica familiar', inicioEn: manana, estado: 'PROGRAMADO', recurrente: false },
    { id: 'evento-salida', perfilId: perfilHijo.id, persona: 'Alessio', titulo: 'Visita al museo', tipo: 'SALIDA',
      lugar: 'Museo de Arte', inicioEn: manana, estado: 'PROGRAMADO', recurrente: false }
  ]
  const tratamientos = Array.from({ length: 6 }, (_, indice) => ({
    id: `tratamiento-${indice}`, grupoId: `grupo-${indice}`, perfilId: perfil.id, persona: 'Mamá', medicamento: `Tratamiento ${indice + 1}`,
    responsablePerfilId: perfil.id, responsable: 'Mamá', horarios: ['08:00:00'], fechaInicio: '2026-07-23',
    estado: 'ACTIVO', recetaId: indice === 0 ? 'receta-1' : undefined
  }))
  const medicamentos = Array.from({ length: 6 }, (_, indice) => ({
    id: `medicamento-${indice}`, loteId: `lote-${indice}`, nombre: `Medicamento ${indice + 1}`,
    presentacion: 'Tabletas', concentracion: '500 mg', cantidad: 10, unidad: 'tabletas', estadoEnvase: 'SIN_ABRIR',
    avisarVencimiento: true, anticipacionVencimientoDias: 7, avisarApertura: true, anticipacionAperturaDias: 3,
    estado: 'DISPONIBLE', requiereAtencion: false, version: 0
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
  const familiasAdmin = [{ id: 'familia-1', nombre: 'Familia Herrera', zonaHoraria: 'America/Lima', creadaEn: '2026-07-23T18:00:00Z' }]
  const miembrosAdmin: Array<{ perfilId: string; usuarioId: string; nombre: string; permiso: string; activo: boolean }> = []
  const enlacesAdmin: Array<{ id: string; tipo: string; usuarioId: string; correo: string; estado: string; expiraEn: string; creadoEn: string }> = []
  await page.route('**/api/v1/**', async route => {
    const ruta = new URL(route.request().url()).pathname
    const responder = (datos: unknown) => route.fulfill({ contentType: 'application/json', body: JSON.stringify(datos) })
    if (ruta.endsWith('/autenticacion/renovar')) {
      const pagina = new URL(page.url())
      const tipoAcceso = pagina.searchParams.get('acceso')
      const administrador = tipoAcceso === 'admin' || (pagina.pathname === '/admin' && tipoAcceso !== 'familia')
      return responder({ accessToken: 'token-e2e', expiraEn: '2099-01-01T00:00:00Z', usuarioId: 'usuario-1',
        correo: administrador ? 'propietario@example.com' : 'papa@familia.test',
        rolPlataforma: administrador ? 'ADMINISTRADOR_PLATAFORMA' : 'USUARIO' })
    }
    if (ruta.endsWith('/administracion/familias') && route.request().method() === 'GET') return responder({ familias: familiasAdmin })
    if (ruta.endsWith('/administracion/familias') && route.request().method() === 'POST') {
      const datos = route.request().postDataJSON() as { nombre: string; zonaHoraria: string }
      familiasAdmin.unshift({ id: `familia-${familiasAdmin.length + 1}`, ...datos, creadaEn: '2026-07-23T21:00:00Z' })
      return route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(familiasAdmin[0]) })
    }
    if (/\/administracion\/familias\/[^/]+\/miembros$/.test(ruta) && route.request().method() === 'GET') return responder({ miembros: miembrosAdmin })
    if (/\/administracion\/familias\/[^/]+\/miembros$/.test(ruta) && route.request().method() === 'POST') {
      const datos = route.request().postDataJSON() as { usuarioId: string; nombre: string; permiso: string }
      const miembro = { perfilId: `perfil-admin-${miembrosAdmin.length + 1}`, ...datos, activo: true }
      miembrosAdmin.push(miembro)
      return route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(miembro) })
    }
    if (/\/administracion\/familias\/[^/]+\/miembros\/[^/]+$/.test(ruta) && route.request().method() === 'PATCH') {
      const perfilId = ruta.split('/').at(-1)
      const datos = route.request().postDataJSON() as { permiso: string; activo: boolean }
      const miembro = miembrosAdmin.find(item => item.perfilId === perfilId)
      if (!miembro) return route.fulfill({ status: 404, contentType: 'application/json', body: '{}' })
      Object.assign(miembro, datos)
      return responder(miembro)
    }
    if (ruta.endsWith('/autenticacion/administracion/enlaces') && route.request().method() === 'GET') return responder({ enlaces: enlacesAdmin })
    if (ruta.endsWith('/autenticacion/administracion/usuarios') && route.request().method() === 'GET') return responder({ cuentas: [] })
    if (ruta.endsWith('/autenticacion/administracion/invitaciones') && route.request().method() === 'POST') {
      const datos = route.request().postDataJSON() as { usuarioId: string; correo: string }
      const enlace = { id: `enlace-${enlacesAdmin.length + 1}`, tipo: 'INVITACION', usuarioId: datos.usuarioId,
        correo: datos.correo, estado: 'PENDIENTE', expiraEn: '2026-07-25T18:00:00Z', creadoEn: '2026-07-23T18:00:00Z' }
      enlacesAdmin.unshift(enlace)
      return route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify({ ...enlace, enlace: '/activar#token=token-e2e' }) })
    }
    if (/\/autenticacion\/administracion\/usuarios\/[^/]+\/restablecimientos$/.test(ruta)) return route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify({ id: 'reset-1', tipo: 'RESTABLECIMIENTO', usuarioId: 'usuario-1', enlace: '/activar#token=reset-e2e', expiraEn: '2026-07-23T19:00:00Z' }) })
    if (/\/autenticacion\/administracion\/enlaces\/[^/]+$/.test(ruta) && route.request().method() === 'DELETE') {
      const id = ruta.split('/').at(-1)
      const enlace = enlacesAdmin.find(item => item.id === id)
      if (enlace) enlace.estado = 'REVOCADO'
      return route.fulfill({ status: 204, body: '' })
    }
    if (ruta.endsWith('/autenticacion/enlaces/consultar')) return responder({ tipo: 'INVITACION', correo: 'p***@example.com', familia: 'Familia Herrera', expiraEn: '2099-01-01T00:00:00Z' })
    if (ruta.endsWith('/autenticacion/enlaces/consumir')) return route.fulfill({ status: 204, body: '' })
    if (ruta === '/api/v1/familias') return responder({ familias: [{ id: 'familia-1', nombre: 'Familia Herrera', zonaHoraria: 'America/Lima', rol: 'ADMINISTRADOR_FAMILIAR' }] })
    if (ruta.endsWith('/hoy')) return responder({ familiaId: 'familia-1', familia: 'Familia Herrera', zonaHoraria: 'America/Lima', perfiles: [perfil, perfilHijo], tareas })
    if (ruta.endsWith('/catalogo')) return responder({ medicamentos, tratamientos, eventos, lugares: [] })
    if (ruta.endsWith('/ocurrencias')) return responder({ ocurrencias: historialTomas, revisar: [] })
    if (ruta.endsWith('/auditoria')) return responder({ entradas: [] })
    if (ruta.endsWith('/configuracion')) return responder({ puedeAdministrar: true, perfiles: [{ ...perfil, activo: true, permiso: 'ADMINISTRADOR_FAMILIAR' }] })
    if (ruta.endsWith('/archivos/cuota')) return responder({ cuotaBytes: 1_000_000, usadosBytes: 0, disponiblesBytes: 1_000_000, porcentaje: 0, nivel: 'NORMAL' })
    if (ruta.endsWith('/archivos/receta-1')) return route.fulfill({ status: 200, contentType: 'image/jpeg', body: '' })
    if (ruta.endsWith('/objetos') && route.request().method() === 'GET') return responder({ objetos, ubicaciones: [{ ruta: objetos[0].ruta, cantidad: 1 }] })
    if (ruta.endsWith('/objetos') && route.request().method() === 'POST') return responder({ id: 'objeto-2' })
    if (ruta.endsWith('/medicamentos') && route.request().method() === 'POST') return route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify({ id: 'medicamento-nuevo', loteId: 'lote-nuevo' }) })
    if (ruta.endsWith('/tratamientos/grupos') && route.request().method() === 'POST') return route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify({ grupoId: 'grupo-nuevo', ids: ['tratamiento-nuevo'] }) })
    if (/\/tratamientos\/grupos\/[^/]+$/.test(ruta) && route.request().method() === 'PATCH') {
      const grupoId = ruta.split('/').at(-1)
      const datos = route.request().postDataJSON() as { nombre: string; nombreMedicamento?: string; dosis?: string; aplicacion?: string; indicacion?: string; frecuencia?: string; horarios: string[]; intervaloHoras?: number; fechaInicio: string; fechaFin?: string }
      tratamientos.filter(item => item.grupoId === grupoId).forEach(item => Object.assign(item, {
        medicamento: datos.nombre, nombreMedicamento: datos.nombreMedicamento, dosisIndicada: datos.dosis,
        aplicacion: datos.aplicacion, indicacion: datos.indicacion, frecuencia: datos.frecuencia,
        horarios: datos.horarios.map(hora => `${hora}:00`), intervaloHoras: datos.intervaloHoras,
        fechaInicio: datos.fechaInicio, fechaFin: datos.fechaFin
      }))
      return route.fulfill({ status: 204, body: '' })
    }
    if (/\/medicamentos\/lotes\/[^/]+$/.test(ruta) && route.request().method() === 'PATCH') return route.fulfill({ status: 204, body: '' })
    if (/\/objetos\/[^/]+$/.test(ruta) && route.request().method() === 'PATCH') return route.fulfill({ status: 204 })
    return route.fulfill({ status: 404, contentType: 'application/json', body: '{}' })
  })
}

test.beforeEach(async ({ page }) => prepararApi(page))

test('deriva una sesión administrativa al panel global', async ({ page }) => {
  await page.goto('/hoy?acceso=admin')
  await expect(page).toHaveURL(/\/admin$/)
  await expect(page.getByRole('heading', { name: 'Familias', exact: true })).toBeVisible()
})

test('devuelve una sesión familiar desde administración a su agenda', async ({ page }) => {
  await page.goto('/admin?acceso=familia')
  await expect(page).toHaveURL(/\/hoy$/)
  await expect(page.getByRole('heading', { name: 'Todo está al día' })).toBeVisible()
})

test('administración lista y crea familias en una única capa modal', async ({ page }) => {
  await page.goto('/admin')
  await expect(page.getByRole('heading', { name: 'Familias', exact: true })).toBeVisible()
  await expect(page.getByText('Familia Herrera', { exact: true })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(page.viewportSize()!.width)

  const activador = page.getByRole('button', { name: 'Nueva familia' })
  await activador.click()
  const dialogo = page.getByRole('dialog', { name: 'Nueva familia' })
  await expect(dialogo).toBeVisible()
  expect(await page.locator('dialog:modal').count()).toBe(1)
  await dialogo.getByLabel('Nombre de la familia').fill('Familia Rivera')
  await dialogo.getByRole('button', { name: 'Crear familia' }).click()
  await expect(dialogo).toBeHidden()
  await expect(activador).toBeFocused()
  await expect(page.getByRole('button', { name: /Familia Rivera/ })).toBeVisible()
})

test('administra miembros con invitación de un solo uso sin apilar capas', async ({ page }) => {
  await page.goto('/admin')
  await page.getByRole('button', { name: /Familia Herrera/ }).click()
  await expect(page.getByRole('heading', { name: 'Miembros con acceso' })).toBeVisible()
  await page.getByRole('button', { name: 'Añadir miembro' }).click()
  const formulario = page.getByRole('dialog', { name: 'Añadir miembro' })
  await formulario.getByLabel('Nombre visible').fill('Ana Rivera')
  await formulario.getByLabel('Correo de acceso').fill('ana@example.com')
  await formulario.getByLabel('Permiso').selectOption('ADMINISTRADOR_FAMILIAR')
  await formulario.getByRole('button', { name: 'Generar enlace' }).click()

  const enlace = page.getByRole('dialog', { name: 'Enlace de un solo uso' })
  await expect(enlace).toBeVisible()
  expect(await page.locator('dialog:modal').count()).toBe(1)
  await expect(enlace.getByRole('textbox', { name: 'Enlace' })).toHaveValue(/\/activar#token=token-e2e$/)
  await enlace.getByRole('button', { name: 'Cerrar', exact: true }).click()
  await expect(page.getByText('Ana Rivera', { exact: true })).toBeVisible()
  await expect(page.getByText('pendiente', { exact: true })).toBeVisible()
  const tarjetaAna = page.locator('.admin-miembro').filter({ hasText: 'Ana Rivera' })
  await tarjetaAna.getByRole('button', { name: 'Gestionar' }).click()
  const gestion = page.getByRole('dialog', { name: 'Gestionar a Ana Rivera' })
  await expect(gestion.getByText('Familia Herrera', { exact: true })).toBeVisible()
  await gestion.getByLabel('Rol en esta familia').selectOption('ADULTO')
  await gestion.getByRole('radio', { name: 'Dar de baja' }).check()
  expect(await page.locator('dialog:modal').count()).toBe(1)
  await gestion.getByRole('button', { name: 'Dar de baja' }).click()
  await expect(gestion).toBeHidden()
  await expect(tarjetaAna.getByText('Sin acceso')).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(page.viewportSize()!.width)
})

test('consume una invitación y elimina el token de la dirección visible', async ({ page }) => {
  await page.goto('/activar#token=token-e2e')
  await expect(page.getByRole('heading', { name: 'Activa tu acceso' })).toBeVisible()
  await expect(page).toHaveURL(/\/activar$/)
  await page.getByLabel('Nueva contraseña').fill('ClaveNuevaSegura2026!')
  await page.getByLabel('Repetir contraseña').fill('ClaveNuevaSegura2026!')
  await page.getByRole('button', { name: 'Guardar contraseña' }).click()
  await expect(page.getByRole('heading', { name: 'Contraseña guardada' })).toBeVisible()
})

test('navega por destinos reales y muestra solo el dominio activo', async ({ page }) => {
  await page.goto('/hoy')
  await expect(page.getByRole('navigation', { name: 'Navegación principal' }).getByRole('link', { name: 'Hoy', exact: true })).toHaveAttribute('aria-current', 'page')
  await expect(page.getByRole('heading', { name: 'Todo está al día' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Por resolver' })).toHaveCount(0)
  await expect(page.getByText('Ver historial', { exact: true })).toHaveCount(0)
  await expect(page.getByRole('heading', { name: 'Medicamentos' })).toHaveCount(0)

  await page.getByRole('link', { name: 'Agenda' }).click()
  await expect(page).toHaveURL(/\/agenda$/)
  await expect(page.getByRole('link', { name: 'Agenda' })).toHaveAttribute('aria-current', 'page')
  await expect(page.getByRole('heading', { name: 'Próximos eventos' })).toBeVisible()
  await expect(page.locator('.tipo-entrada--tarea')).toContainText('Tarea')
  await expect(page.locator('.tipo-entrada--cita')).toContainText('Cita')
  await expect(page.locator('.tipo-entrada--salida')).toContainText('Salida')
  const anadirAgenda = page.getByRole('button', { name: 'Añadir', exact: true })
  await anadirAgenda.click()
  await expect(page.getByRole('button', { name: 'Tarea', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Evento, cita o salida', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Objeto', exact: true })).toHaveCount(0)
  await page.keyboard.press('Escape')
  await expect(page.getByRole('heading', { name: 'Tomas', exact: true })).toHaveCount(0)

  await page.getByRole('link', { name: 'Salud' }).click()
  await expect(page).toHaveURL(/\/salud$/)
  await expect(page.getByRole('heading', { name: 'Tomas', exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Tratamientos' })).toHaveCount(0)
  await expect(page.getByRole('heading', { name: 'Próximos eventos' })).toHaveCount(0)
})

test('Agenda diferencia tareas, citas y salidas y permite crear ambos dominios', async ({ page }) => {
  await page.goto('/agenda')
  await expect(page.locator('.tipo-entrada--tarea')).toContainText('Tarea')
  await expect(page.locator('.tipo-entrada--cita')).toContainText('Cita')
  await expect(page.locator('.tipo-entrada--salida')).toContainText('Salida')

  const anadir = page.getByRole('button', { name: 'Añadir', exact: true })
  await anadir.click()
  await expect(page.getByRole('button', { name: 'Tarea', exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Evento, cita o salida', exact: true }).click()

  const dialogoEvento = page.getByRole('dialog', { name: 'Nuevo evento' })
  await expect(dialogoEvento).toBeVisible()
  await expect(dialogoEvento.getByLabel('Tipo o categoría')).toBeVisible()
  await dialogoEvento.getByLabel('Tipo o categoría').selectOption('CITA')
  await expect(dialogoEvento.getByLabel('Tipo o categoría')).toHaveValue('CITA')
  const accesibilidad = await new AxeBuilder({ page }).include('dialog').analyze()
  expect(accesibilidad.violations.filter(item => item.impact === 'critical' || item.impact === 'serious')).toEqual([])
  await page.keyboard.press('Escape')
  await page.getByRole('dialog', { name: '¿Descartar los cambios?' }).getByRole('button', { name: 'Descartar' }).click()
  await expect(dialogoEvento).toBeHidden()

  await anadir.click()
  await page.getByRole('button', { name: 'Tarea', exact: true }).click()
  await expect(page.getByRole('dialog', { name: 'Nueva tarea' })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(page.viewportSize()!.width)
})

test('mantiene Por resolver como vista separada de Hoy', async ({ page }) => {
  await page.goto('/hoy')
  await expect(page.getByRole('navigation', { name: 'Vistas de Hoy' })).toBeVisible()
  await page.getByRole('link', { name: 'Por resolver', exact: true }).click()
  await expect(page).toHaveURL(/vista=resolver/)
  await expect(page.getByRole('heading', { name: 'Nada por resolver' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Tareas de hoy' })).toHaveCount(0)
  await expect(page.getByRole('heading', { name: 'Tomas del día' })).toHaveCount(0)
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

test('permite corregir un tratamiento activo sin cambiar sus personas', async ({ page }) => {
  await page.goto('/salud?seccion=tratamientos')
  const tarjeta = page.locator('#tratamientos article.tarjeta').filter({ has: page.getByRole('heading', { name: 'Tratamiento 1' }) })
  await tarjeta.getByLabel('Más acciones para Tratamiento 1').click()
  await tarjeta.getByRole('button', { name: 'Editar tratamiento' }).click()

  const dialogo = page.getByRole('dialog', { name: 'Editar tratamiento' })
  await expect(dialogo.getByText(/tomas ya confirmadas conservarán su historial/)).toBeVisible()
  await expect(dialogo.locator('.chips-personas input[value="perfil-1"]')).toBeDisabled()
  await dialogo.getByLabel('Nombre corto del tratamiento').fill('Gotas corregidas')
  await dialogo.getByLabel('Horario 1').fill('10:00')
  await dialogo.getByRole('button', { name: 'Guardar cambios' }).click()

  await expect(dialogo).toBeHidden()
  await expect(page.getByRole('heading', { name: 'Gotas corregidas' })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(page.viewportSize()!.width)
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

test('registra un tratamiento práctico para varias personas con horarios explícitos', async ({ page }) => {
  await page.goto('/salud?seccion=tratamientos')
  await page.getByRole('button', { name: 'Tratamiento', exact: true }).click()
  const dialogo = page.getByRole('dialog', { name: 'Nuevo tratamiento' })

  await dialogo.getByLabel('Alessio', { exact: true }).check()
  await dialogo.getByLabel('Nombre corto del tratamiento').fill('Gotas para los ojos')
  await dialogo.getByLabel('Medicamento opcional').fill('Lágrimas artificiales')
  await dialogo.getByLabel('Dosis opcional').fill('2 gotas')
  await dialogo.getByLabel('Dónde o cómo se aplica (opcional)').fill('En ambos ojos')
  await dialogo.getByLabel('Horario 1', { exact: true }).fill('08:00')
  await dialogo.getByRole('button', { name: '+ Añadir otro horario' }).click()
  await dialogo.getByLabel('Horario 2', { exact: true }).fill('14:00')
  await dialogo.getByRole('button', { name: '+ Añadir otro horario' }).click()
  await dialogo.getByLabel('Horario 3', { exact: true }).fill('20:00')
  await expect(dialogo.getByText('2 persona(s) · 3')).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(page.viewportSize()!.width)
  const accesibilidad = await new AxeBuilder({ page }).include('dialog').analyze()
  expect(accesibilidad.violations.filter(item => item.impact === 'critical' || item.impact === 'serious')).toEqual([])

  const alta = page.waitForRequest(request => request.method() === 'POST' && new URL(request.url()).pathname.endsWith('/tratamientos/grupos'))
  await dialogo.getByRole('button', { name: 'Guardar tratamiento' }).click()
  const solicitud = await alta
  expect(solicitud.headers()['idempotency-key']).toBeTruthy()
  expect(solicitud.postDataJSON()).toMatchObject({
    perfilIds: ['perfil-1', 'perfil-2'], nombre: 'Gotas para los ojos', nombreMedicamento: 'Lágrimas artificiales',
    dosis: '2 gotas', aplicacion: 'En ambos ojos', horarios: ['08:00', '14:00', '20:00']
  })
  await expect(dialogo).toBeHidden()
})

test('registra cada envase y actualiza su apertura de forma independiente', async ({ page }) => {
  await page.goto('/salud?seccion=botiquin')
  await page.getByRole('button', { name: 'Medicamento', exact: true }).click()
  const dialogo = page.getByRole('dialog', { name: 'Nuevo medicamento' })
  await dialogo.getByLabel('Nombre').fill('Gotas oculares')
  await dialogo.getByLabel('Abierto', { exact: true }).check()
  await dialogo.getByLabel('Se abrió el').fill('2026-07-23')
  await dialogo.getByLabel('Usar durante (días)').fill('30')
  const alta = page.waitForRequest(request => request.method() === 'POST' && new URL(request.url()).pathname.endsWith('/medicamentos'))
  await dialogo.getByRole('button', { name: 'Guardar medicamento' }).click()
  const solicitudAlta = await alta
  expect(solicitudAlta.postDataJSON()).toMatchObject({ estadoEnvase: 'ABIERTO', abiertoEn: '2026-07-23', duracionAbiertoDias: 30 })

  const tarjeta = page.locator('#botiquin article.tarjeta').filter({ has: page.getByText('Medicamento 1', { exact: true }) })
  const apertura = page.waitForRequest(request => request.method() === 'PATCH' && new URL(request.url()).pathname.endsWith('/medicamentos/lotes/lote-0'))
  await tarjeta.getByRole('button', { name: 'Marcar abierto' }).click()
  const solicitudApertura = await apertura
  expect(solicitudApertura.headers()['idempotency-key']).toBeTruthy()
  expect(solicitudApertura.postDataJSON()).toMatchObject({ estadoEnvase: 'ABIERTO', estadoInventario: 'DISPONIBLE', version: 0 })
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
  await page.getByRole('button', { name: 'Tarea', exact: true }).click()
  await expect(page.getByRole('dialog', { name: 'Nueva tarea' })).toBeVisible()
  expect(await page.locator('dialog:modal').count()).toBe(1)
  await page.keyboard.press('Escape')
  await expect(anadir).toBeFocused()
})

test('solo permite un menú Más abierto y lo cierra al pulsar fuera', async ({ page }) => {
  await page.goto('/salud?seccion=tratamientos')
  const activadores = page.locator('.menu-mas > summary')
  await expect(activadores).toHaveCount(5)
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
  await page.getByRole('button', { name: 'Evento, cita o salida', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('Vuelve a estar en línea')
  await expect(page.locator('.formulario-adaptativo')).not.toBeVisible()
  await context.setOffline(false)
})

test('no presenta violaciones críticas o serias de accesibilidad', async ({ page }) => {
  for (const ruta of ['/hoy', '/agenda', '/salud', '/objetos', '/ajustes/familia', '/actividad', '/admin', '/activar']) {
    await page.goto(ruta)
    await expect(page.locator('.estado-carga')).toHaveCount(0)
    const resultado = await new AxeBuilder({ page }).analyze()
    const graves = resultado.violations.filter(violacion => violacion.impact === 'critical' || violacion.impact === 'serious')
    expect(graves, `${ruta}: ${graves.map(violacion => violacion.id).join(', ')}`).toEqual([])
  }
})
