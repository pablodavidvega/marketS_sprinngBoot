export interface AuthResponse {
  email: string
  role: string
}

export interface PerfilUsuario {
  id: number
  username: string
  email: string
  phone: string
  address: string
  rol: string
  city?: string
  createdAt: string
}

export interface TallaStock {
  talla: string
  stock: number
}

export interface Producto {
  id: number
  nombre: string
  precio: number
  descripcion: string
  imagen: string | null
  imagen2: string | null
  imagen3: string | null
  video: string | null
  stock: number
  estadoProducto: string
  activo: boolean
  fechaAgregacion: string
  categoriaId: number | null
  categoriaNombre: string | null
  marcaId: number | null
  marcaNombre: string | null
  tallas: TallaStock[]
}

export interface Categoria {
  id: number
  nombre: string
}

export interface CarritoItem {
  productoId: number
  nombre: string
  precio: number
  imagen: string | null
  cantidad: number
}

export interface DetallePedido {
  productoId: number
  productoNombre: string
  productoImagen: string | null
  precioUnitario: number
  cantidad: number
  subtotal: number
}

export interface PedidoResponse {
  pedidoId: number
  estado: string
  total: number
  fecha: string
  paymentUrl?: string
  preferenceId?: string
  detalles: DetallePedido[]
}

export interface DashboardCliente {
  perfil: PerfilUsuario
  pedidosRecientes: PedidoResponse[]
  totalPedidos: number
  pedidosPagados: number
  gastoTotal: number
}

export interface DashboardAdmin {
  totalUsuarios: number
  totalClientes: number
  totalVendedores: number
  totalAdmins: number
}

export interface SugerenciaProducto {
  id: number
  nombre: string
  precio: number
  imagen: string | null
  categoriaNombre: string | null
}

export interface VendedorProductosResponse {
  contenido: Producto[]
  paginaActual: number
  totalPaginas: number
  totalElementos: number
}
