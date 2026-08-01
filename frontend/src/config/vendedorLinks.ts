// Única fuente de verdad para las secciones del panel de vendedor: de acá salen a la vez
// el tab de la segunda fila del navbar (VendedorSubNav) y la card de "Acciones" del
// dashboard (vendedor/DashboardPage). Agregar una vista nueva acá la crea en los dos lugares.
export interface VendedorLink {
  to: string
  label: string
  icon: string
  description: string
  iconClass: string
}

export const VENDEDOR_LINKS: VendedorLink[] = [
  {
    to: '/vendedor/productos',
    label: 'Mis productos',
    icon: '📦',
    description: 'Ver, crear y gestionar productos',
    iconClass: 'bg-primary-100 text-primary-600 group-hover:bg-primary-200',
  },
  {
    to: '/vendedor/carga-masiva',
    label: 'Carga masiva de productos',
    icon: '📤',
    description: 'Agrega varios productos a la vez en una tabla editable',
    iconClass: 'bg-orange-100 text-orange-600 group-hover:bg-orange-200',
  },
  {
    to: '/vendedor/pedidos',
    label: 'Pedidos recibidos',
    icon: '🛍️',
    description: 'Ver compras de tus productos y datos del comprador',
    iconClass: 'bg-green-100 text-green-600 group-hover:bg-green-200',
  },
  {
    to: '/vendedor/reportes',
    label: 'Mis reportes',
    icon: '📈',
    description: 'Análisis de ventas con gráficas y descarga CSV',
    iconClass: 'bg-blue-100 text-blue-600 group-hover:bg-blue-200',
  },
]
