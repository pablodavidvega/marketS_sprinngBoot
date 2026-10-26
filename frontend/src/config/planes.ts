// Planes de la fase SaaS (una suscripción por comunidad). Por ahora solo alimentan la página
// pública /planes: todavía no hay cobro ni límites aplicados en el backend. Los precios son
// una estimación inicial en COP, a validar con las primeras comunidades.

export type PlanId = 'gratis' | 'basico' | 'premium'

export interface Plan {
  id: PlanId
  nombre: string
  descripcion: string
  precioMensual: number
  // Pago anual: 10 mensualidades (2 meses gratis)
  precioAnual: number
  destacado?: boolean
  beneficios: string[]
}

export const PLANES: Plan[] = [
  {
    id: 'gratis',
    nombre: 'Gratis',
    descripcion: 'Para conocer la dinámica de la app con una comunidad pequeña.',
    precioMensual: 0,
    precioAnual: 0,
    beneficios: [
      'Hasta 50 miembros',
      'Hasta 100 productos publicados',
      '1 foto por producto',
      'Registro por dominio de correo o por invitación',
      'Pagos con Mercado Pago',
      'Resumen diario de novedades por correo',
      '1 banner en el home',
      'Subdominio propio (tucomunidad.markets.co)',
    ],
  },
  {
    id: 'basico',
    nombre: 'Básico',
    descripcion: 'Para comunidades activas que quieren su propia identidad.',
    precioMensual: 49900,
    precioAnual: 499000,
    destacado: true,
    beneficios: [
      'Hasta 500 miembros',
      'Hasta 2.000 productos publicados',
      '3 fotos por producto',
      'Registro abierto, por dominio o por invitación',
      'Logo y colores propios',
      'Hasta 3 banners en el home',
      'Reportes completos',
      'Hasta 3 moderadores',
    ],
  },
  {
    id: 'premium',
    nombre: 'Premium',
    descripcion: 'Para organizaciones grandes que necesitan todo, sin límites.',
    precioMensual: 129900,
    precioAnual: 1299000,
    beneficios: [
      'Miembros y productos ilimitados',
      '3 fotos y video por producto',
      'Dominio propio (tienda.tuempresa.com)',
      'Sin la marca "Hecho con MarketS"',
      'Banners ilimitados',
      'Reportes exportables a CSV',
      'Moderadores ilimitados',
      'Soporte prioritario',
    ],
  },
]

type Valor = boolean | string

// Tabla comparativa: [característica, gratis, básico, premium]
export const COMPARATIVA: [string, Valor, Valor, Valor][] = [
  ['Miembros', 'Hasta 50', 'Hasta 500', 'Ilimitados'],
  ['Productos publicados', 'Hasta 100', 'Hasta 2.000', 'Ilimitados'],
  ['Fotos por producto', '1', '3', '3'],
  ['Video por producto', false, false, true],
  ['Registro por dominio de correo o invitación', true, true, true],
  ['Registro abierto', false, true, true],
  ['Pagos con Mercado Pago', true, true, true],
  ['Resumen diario por correo', true, true, true],
  ['Banners en el home', '1', 'Hasta 3', 'Ilimitados'],
  ['Logo y colores propios', 'Solo logo', true, true],
  ['Dirección', 'Subdominio', 'Subdominio', 'Subdominio o dominio propio'],
  ['Sin la marca "Hecho con MarketS"', false, false, true],
  ['Reportes', 'Básicos', 'Completos', 'Completos + CSV'],
  ['Moderadores', '1', 'Hasta 3', 'Ilimitados'],
  ['Soporte', 'Correo', 'Correo', 'Prioritario'],
]
