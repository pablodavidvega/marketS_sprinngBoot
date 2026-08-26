export interface AuthResponse {
  email: string
  role: string
  token: string
  debeCambiarPassword?: boolean
}

export interface GoogleLoginResponse {
  needsCompletion: boolean
  email?: string
  role?: string
  token?: string
  nombreSugerido?: string
  pendingToken?: string
  requiereInvitacion?: boolean
}

export interface VendedorEstadisticas {
  id: number
  username: string
  email: string
  totalProductos: number
  totalUnidades: number
  gananciaTotal: number
  comisionTotal: number
  ingresosTotal: number
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
  passwordEstablecida?: boolean
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
  vendedorId?: number | null
  vendedorUsername?: string | null
  tallas: TallaStock[]
  promedioCalificacion?: number | null
  totalResenas?: number | null
  totalVendido?: number | null
  reputacionVendedor?: Reputacion | null
}

export interface EnlaceSocial {
  etiqueta: string
  url: string
}

export interface PerfilVendedor {
  id: number
  username: string
  miembroDesde: string
  totalProductos: number
  city?: string | null
  reputacion: Reputacion
  totalVentas: number
  unidadesVendidas: number
  enlaces: EnlaceSocial[]
}

export interface PuntoSemana {
  semana: string
  total: number
}

export interface EmbudoAdmin {
  totalRegistrados: number
  totalVendedores: number
  totalPublicaron: number
  totalCompraron: number
  pedidosElegibles: number
  pedidosAbandonados: number
  tasaAbandonoCheckout: number | null
  diasPromedioRegistroAPublicacion: number | null
  registrosPorSemana: PuntoSemana[]
}

export interface PuntoSemanaTransaccion {
  semana: string
  total: number
  monto: number
}

export interface EstadisticasComunidad {
  totalRegistrados: number
  totalTransaccionaron: number
  tasaAdopcion: number | null
  totalTransacciones: number
  gmvTotal: number
  totalFavoritos: number
  totalEncuentros: number
  transaccionesPorSemana: PuntoSemanaTransaccion[]
}

export interface Categoria {
  id: number
  nombre: string
}

export interface CategoriaAdmin {
  id: number
  nombreCategoria: string
  totalProductos: number
}

export interface BannerSlide {
  id: number
  imagen: string
  texto: string
  enlace: string
  orden: number
  activo: boolean
  fechaCreacion: string
}

export interface CarritoItem {
  productoId: number
  nombre: string
  precio: number
  imagen: string | null
  cantidad: number
  stock: number
  vendedorId: number | null
  vendedorUsername: string | null
}

export interface DetallePedido {
  productoId: number
  productoNombre: string
  productoImagen: string | null
  precioUnitario: number
  cantidad: number
  subtotal: number
  vendedorId: number | null
  vendedorUsername: string | null
  yaReseno: boolean
}

export interface PedidoResponse {
  pedidoId: number
  estado: string
  metodoPago?: string
  total: number
  fecha: string
  paymentUrl?: string
  preferenceId?: string
  detalles: DetallePedido[]
}

export interface Reputacion {
  estado: 'NUEVO' | 'CALCULADA'
  score: number | null
  nivel: 'BAJA' | 'REGULAR' | 'BUENA' | 'MUY_BUENA' | 'EXCELENTE' | null
  promedioEstrellas: number | null
  totalCalificaciones: number
  tasaExito: number | null
  transaccionesResueltas: number
}

export interface DashboardCliente {
  perfil: PerfilUsuario
  pedidosRecientes: PedidoResponse[]
  totalPedidos: number
  pedidosPagados: number
  gastoTotal: number
  reputacion: Reputacion
}

export interface DashboardAdmin {
  totalUsuarios: number
  totalClientes: number
  totalVendedores: number
  totalModeradores: number
  totalAdmins: number
  totalProductos: number
  productosActivos: number
  totalPedidos: number
  reputacionVendedores: Record<string, number>
  reputacionCompradores: Record<string, number>
  totalFavoritos: number
  // Mapa vacío ({}) cuando todavía nadie ha guardado ningún favorito.
  productoMasGuardado: Partial<{ id: number; nombre: string; totalFavoritos: number }>
}

export interface DashboardVendedor {
  gananciaTotal: number
  comisionTotal: number
  totalVentas: number
  totalProductos: number
  reputacion: Reputacion
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
  paginaActual: number
  totalPaginas: number
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
  // Nula en pedidos de antes de que este campo existiera en la BD (columna agregada sin
  // backfill) — el fallback en el render usa precioUnitario × cantidad (100%, comisión = 0).
  gananciaVendedor: number | null
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
  vendedorId: number
  fecha: string
  estado: string
  metodoPago?: string
  compradorUsername: string
  compradorEmail: string
  compradorPhone: string
  compradorAddress: string
  compradorReputacion: Reputacion
  puedeCalificarComprador: boolean
  yaCalifiqueComprador: boolean
  // Solo relevante si estado === 'ACORDADO_EXTERNO' y el pedido tiene productos de más de un
  // vendedor: si YO ya resolví mi parte pero el pedido sigue en ACORDADO_EXTERNO porque otro
  // vendedor del mismo pedido no resolvió la suya, no hay que mostrar de nuevo los botones.
  yaResolviMiParte: boolean
  items: ItemPedidoVendedor[]
}

export interface ResultadoCarga {
  total: number
  correctos: number
  fallidos: number
  errores: string[]
}

export interface Invitacion {
  id: number
  token: string
  descripcion: string
  activo: boolean
  vigente: boolean
  usosActuales: number
  maxUsos: number | ''
  fechaExpiracion: string
  fechaCreacion: string
}

export interface ConfigRegistro {
  modo: 'ABIERTO' | 'DOMINIO' | 'INVITACION' | 'DOMINIO_O_INVITACION'
  dominio: string
}

export interface EstadoRegistro {
  modo: string
  dominioPermitido: string
  registroAbierto: boolean
}

export interface ValidacionInvitacion {
  valido: boolean
  descripcion?: string
  mensaje: string
}

export interface Notificacion {
  id: number
  mensaje: string
  tipo: string
  leida: boolean
  fechaCreacion: string
  pedidoId: number | ''
  productoId: number | ''
  vendedorId: number | ''
  rolDestino: 'COMPRADOR' | 'VENDEDOR' | ''
}

export interface NotificacionesResponse {
  items: Notificacion[]
  noLeidas: number
}

export interface NotificacionesTodasResponse {
  contenido: Notificacion[]
  paginaActual: number
  totalPaginas: number
  totalElementos: number
}

export interface ChatMensaje {
  id: number
  autorId: number
  autorUsername: string
  esVendedor: boolean
  propio: boolean
  contenido: string
  fechaEnvio: string
  leido: boolean
}

export interface Encuentro {
  id: number
  lugar: string
  fechaHora: string
  nota: string | null
  estado: 'PENDIENTE' | 'CONFIRMADO'
  propuestoPorId: number
  propuestoPorUsername: string
  puedeConfirmar: boolean
  ventaExternaConfirmada: boolean
}

export interface Pregunta {
  id: number
  contenido: string
  usuarioUsername: string
  fechaPregunta: string
  respuesta: string | null
  fechaRespuesta: string | null
}

export interface PreguntasResponse {
  total: number
  puedeResponder: boolean
  preguntas: Pregunta[]
  paginaActual: number
  totalPaginas: number
}

export interface ReporteCatalogo {
  totalProductos: number
  totalCategorias: number
  totalMarcas: number
  promedioPrecio: number
  promedioStock: number
  nombresCategorias: string[]
  totalesCategorias: number[]
}
