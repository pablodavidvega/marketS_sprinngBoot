// Estados posibles de un pedido: color del badge y explicación para el usuario.
// Compartido entre las vistas de pedidos de vendedor y cliente para no duplicar
// la lista ni arriesgarse a que las explicaciones queden desincronizadas.
export const ESTADO_COLOR: Record<string, string> = {
  PAGADO: 'bg-green-100 text-green-700',
  RECIBIDO: 'bg-emerald-100 text-emerald-700',
  PENDIENTE: 'bg-yellow-100 text-yellow-700',
  PENDIENTE_PAGO: 'bg-yellow-100 text-yellow-700',
  RECHAZADO: 'bg-red-100 text-red-700',
  CANCELADO: 'bg-red-100 text-red-700',
  PROCESANDO_PAGO: 'bg-blue-100 text-blue-700',
  ACORDADO_EXTERNO: 'bg-blue-100 text-blue-700',
  COMPLETADO_EXTERNO: 'bg-green-100 text-green-700',
  RECHAZADO_EXTERNO: 'bg-red-100 text-red-700',
}

export const ESTADO_INFO: Record<string, string> = {
  PENDIENTE: 'El pedido se creó, pero todavía no se inició el pago.',
  PROCESANDO_PAGO: 'Ya se fue a pagar con MercadoPago; falta confirmar si el pago fue aprobado, quedó pendiente o fue rechazado.',
  PENDIENTE_PAGO: 'MercadoPago dejó el pago pendiente de confirmar de su lado (por ejemplo, pago en efectivo o transferencia que tarda en acreditarse).',
  PAGADO: 'El pago fue aprobado y confirmado por MercadoPago.',
  RECIBIDO: 'El comprador confirmó que ya recibió el producto.',
  RECHAZADO: 'MercadoPago rechazó el pago, o no se pudo iniciar el pago. Podés intentar de nuevo desde tu carrito.',
  CANCELADO: 'El pedido fue cancelado.',
  ACORDADO_EXTERNO: 'El pago se acordó por fuera de la app (efectivo, transferencia, Nequi, etc.). Falta que el vendedor confirme si la venta se concretó o no.',
  COMPLETADO_EXTERNO: 'El vendedor confirmó que la venta acordada por fuera de la app sí se realizó.',
  RECHAZADO_EXTERNO: 'El vendedor indicó que la venta acordada por fuera de la app no se realizó.',
}
