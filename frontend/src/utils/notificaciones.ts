import type { Notificacion } from '../types'

// Construye la URL de destino con los query params que cada página de pedidos usa para
// auto-expandir el pedido exacto y, si aplica, abrir el hilo de chat/encuentro correcto
// (un pedido puede tener varios vendedores, por eso a veces hace falta también "vendedor").
function destinoPedido(esVendedor: boolean, pedidoId: number, vendedorId: number | '', foco?: 'chat' | 'encuentro') {
  const base = esVendedor ? '/vendedor/pedidos' : '/cliente/mis-pedidos'
  const params = new URLSearchParams({ pedido: String(pedidoId) })
  if (foco) params.set('foco', foco)
  if (vendedorId !== '') params.set('vendedor', String(vendedorId))
  return `${base}?${params.toString()}`
}

// Tipos cuyo destino (comprador o vendedor) es fijo por definición, sin importar el rol global
// del usuario que los recibe: se generan siempre para el mismo lado del pedido. Esto es clave
// para un usuario VENDEDOR que también compra — su rol global ya no alcanza para decidir a
// dónde va cada notificación puntual.
const TIPOS_SIEMPRE_VENDEDOR = new Set([
  'NUEVO_PEDIDO', 'PEDIDO_RECIBIDO', 'VENTA_PAGO_CONFIRMADO', 'RECORDATORIO_COMPRA_EXTERNA_SIN_CONFIRMAR',
])
const TIPOS_SIEMPRE_COMPRADOR = new Set([
  'PEDIDO_CREADO', 'PEDIDO_PAGADO', 'PEDIDO_PAGO_PENDIENTE', 'PEDIDO_PAGO_RECHAZADO',
  'PEDIDO_COMPLETADO_EXTERNO', 'PEDIDO_RECHAZADO_EXTERNO', 'RECORDATORIO_CONFIRMAR_RECEPCION',
])
// Chat y encuentro sí pueden ir a cualquiera de las dos partes de un mismo hilo según quién
// actuó último — para esos, el backend manda `rolDestino` explícito en la propia notificación
// (ver Notificacion.rolDestino). Si falta (notificaciones viejas, previas a ese campo), se cae
// al rol global del usuario como antes.
const TIPOS_HILO = new Set([
  'NUEVO_MENSAJE_CHAT', 'RECORDATORIO_CHAT_SIN_RESPONDER',
  'ENCUENTRO_PROPUESTO', 'ENCUENTRO_ACTUALIZADO', 'ENCUENTRO_CANCELADO', 'ENCUENTRO_CONFIRMADO',
])

export function destinoDeNotificacion(n: Notificacion, esVendedor: boolean): string {
  if (TIPOS_SIEMPRE_VENDEDOR.has(n.tipo)) {
    return n.pedidoId !== '' ? destinoPedido(true, n.pedidoId, '') : '/vendedor/pedidos'
  }
  if (TIPOS_SIEMPRE_COMPRADOR.has(n.tipo)) {
    return n.pedidoId !== '' ? destinoPedido(false, n.pedidoId, '') : '/cliente/mis-pedidos'
  }
  if (TIPOS_HILO.has(n.tipo)) {
    const esVendedorDestino = n.rolDestino !== '' ? n.rolDestino === 'VENDEDOR' : esVendedor
    const foco = n.tipo.startsWith('ENCUENTRO_') ? 'encuentro' : 'chat'
    return n.pedidoId !== ''
      ? destinoPedido(esVendedorDestino, n.pedidoId, n.vendedorId, foco)
      : (esVendedorDestino ? '/vendedor/pedidos' : '/cliente/mis-pedidos')
  }
  switch (n.tipo) {
    case 'NUEVA_PREGUNTA':
    case 'PREGUNTA_RESPONDIDA':
    case 'RECORDATORIO_PREGUNTA_SIN_RESPONDER':
      return n.productoId !== '' ? `/productos/${n.productoId}?foco=preguntas` : '/'
    default:
      return esVendedor ? '/vendedor/pedidos' : '/cliente/mis-pedidos'
  }
}

export function formatTiempoNotificacion(fechaStr: string) {
  const diff = Date.now() - new Date(fechaStr).getTime()
  const min  = Math.floor(diff / 60_000)
  const h    = Math.floor(diff / 3_600_000)
  const d    = Math.floor(diff / 86_400_000)
  if (min < 1)  return 'ahora'
  if (min < 60) return `hace ${min} min`
  if (h < 24)   return `hace ${h} h`
  return `hace ${d} d`
}
