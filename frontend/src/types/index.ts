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
  promedioCalificacion?: number | null
  totalResenas?: number | null
  totalVendido?: number | null
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

export interface DashboardVendedor {
  gananciaTotal: number
  comisionTotal: number
  totalVentas: number
  totalProductos: number
}

export interface Resena {
  id: number
  calificacion: number
  comentario: string | null
  compradorUsername: string
  fecha: string
}

export interface ResenasResponse {
  promedio: number
  total: number
  puedeResenar: boolean
  yaReseno: boolean
  resenas: Resena[]
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

export interface ItemPedidoVendedor {
  productoId: number
  productoNombre: string
  productoImagen: string | null
  cantidad: number
  precioUnitario: number
  gananciaVendedor: number
}

export interface ContactoResponse {
  id: number
  nombre: string
  email: string
  tipoSolicitud: string
  asunto: string
  mensaje: string
  estado: 'PENDIENTE' | 'EN_PROCESO' | 'RESUELTO'
  respuesta: string | null
  fechaCreacion: string
  fechaRespuesta: string | null
  usuarioUsername: string | null
}

export interface AdminContactoData {
  solicitudes: ContactoResponse[]
  total: number
  pendientes: number
  enProceso: number
  resueltos: number
}

export interface PedidoVendedorResponse {
  pedidoId: number
  fecha: string
  estado: string
  compradorUsername: string
  compradorEmail: string
  compradorPhone: string
  compradorAddress: string
  items: ItemPedidoVendedor[]
}
