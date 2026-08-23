// Estados posibles de un pedido: color del badge y explicación para el usuario.
// Compartido entre las vistas de pedidos de vendedor y cliente para no duplicar
// la lista ni arriesgarse a que las explicaciones queden desincronizadas.
//
// CANCELADO no está acá: existe como valor legal en el modelo pero el backend nunca lo asigna.
//
// PENDIENTE, PROCESANDO_PAGO y PENDIENTE_PAGO son los 3 momentos de "esperando que se resuelva
// el pago" (ver PedidoExpiracionService, backend): si el comprador no completa el pago se
// cancelan solos en 1h/24h/72h respectivamente, así que comparten color y — en las vistas de
// comprador/vendedor — una sola etiqueta amigable (ver ESTADO_LABEL/ESTADO_LEYENDA más abajo).
// El panel de moderador sí necesita distinguirlos (ESTADO_COLOR/ESTADO_INFO), para poder
// filtrar y diagnosticar un pago puntual que quedó atascado.
export const ESTADO_COLOR: Record<string, string> = {
  PENDIENTE: 'bg-yellow-100 text-yellow-700',
  PROCESANDO_PAGO: 'bg-yellow-100 text-yellow-700',
  PENDIENTE_PAGO: 'bg-yellow-100 text-yellow-700',
  PAGADO: 'bg-green-100 text-green-700',
  RECIBIDO: 'bg-emerald-100 text-emerald-700',
  RECHAZADO: 'bg-red-100 text-red-700',
  ACORDADO_EXTERNO: 'bg-blue-100 text-blue-700',
  COMPLETADO_EXTERNO: 'bg-green-100 text-green-700',
  RECHAZADO_EXTERNO: 'bg-red-100 text-red-700',
}

// Texto técnico por estado real — usado tal cual en el panel de moderador.
export const ESTADO_INFO: Record<string, string> = {
  PENDIENTE: 'El pedido se creó, pero todavía no se inició el pago. Se cancela solo si sigue así 1 hora.',
  PROCESANDO_PAGO: 'Se generó el link de pago; falta que el comprador lo complete en MercadoPago. Se cancela solo si no se completa en 24 h.',
  PENDIENTE_PAGO: 'MercadoPago dejó el pago pendiente de confirmar de su lado (efectivo, transferencia, etc.). Se cancela solo si no se resuelve en 72 h.',
  PAGADO: 'El pago fue aprobado y confirmado por MercadoPago.',
  RECIBIDO: 'El comprador confirmó que ya recibió el producto.',
  RECHAZADO: 'MercadoPago rechazó el pago, no se pudo iniciar, o se venció el plazo sin completarlo.',
  ACORDADO_EXTERNO: 'El pago se acordó por fuera de la app (efectivo, transferencia, Nequi, etc.). Falta que el vendedor confirme si la venta se concretó o no.',
  COMPLETADO_EXTERNO: 'El vendedor confirmó que la venta acordada por fuera de la app sí se realizó.',
  RECHAZADO_EXTERNO: 'El vendedor indicó que la venta acordada por fuera de la app no se realizó.',
}

// Etiqueta amigable para las vistas de comprador/vendedor: agrupa los 3 estados "en proceso de
// pago" bajo un mismo texto — al usuario no le aporta distinguirlos, y se resuelven solos.
export const ESTADO_LABEL: Record<string, string> = {
  PENDIENTE: 'Pago en proceso',
  PROCESANDO_PAGO: 'Pago en proceso',
  PENDIENTE_PAGO: 'Pago en proceso',
  PAGADO: 'Aceptado por MercadoPago',
  RECIBIDO: 'Recibido',
  RECHAZADO: 'Rechazado por MercadoPago',
  ACORDADO_EXTERNO: 'Acordado por fuera de la app',
  COMPLETADO_EXTERNO: 'Venta externa confirmada',
  RECHAZADO_EXTERNO: 'Venta externa no realizada',
}

// Explicación corta por grupo, para el tooltip del badge en las mismas vistas.
export const ESTADO_LABEL_INFO: Record<string, string> = {
  PENDIENTE: 'Se está confirmando el pago; si no se completa, el pedido se cancela solo en unas horas.',
  PROCESANDO_PAGO: 'Se está confirmando el pago; si no se completa, el pedido se cancela solo en unas horas.',
  PENDIENTE_PAGO: 'Se está confirmando el pago; si no se completa, el pedido se cancela solo en unas horas.',
  PAGADO: 'El pago fue aprobado por MercadoPago.',
  RECIBIDO: 'Confirmaste que ya recibiste el producto.',
  RECHAZADO: 'MercadoPago rechazó el pago, o no se completó a tiempo. Podés intentar de nuevo desde el carrito.',
  ACORDADO_EXTERNO: 'Acordaron el pago por fuera de la app; falta que el vendedor confirme si la venta se concretó.',
  COMPLETADO_EXTERNO: 'El vendedor confirmó que la venta se realizó.',
  RECHAZADO_EXTERNO: 'El vendedor indicó que la venta no se realizó.',
}

// Filas del desplegable "¿Qué significa cada estado?" en comprador/vendedor: una por grupo
// visible (no por estado técnico), en el orden en que conviene leerlas.
export const ESTADO_LEYENDA: { label: string; color: string; texto: string }[] = [
  { label: 'Pago en proceso', color: ESTADO_COLOR.PROCESANDO_PAGO, texto: ESTADO_LABEL_INFO.PROCESANDO_PAGO },
  { label: 'Aceptado por MercadoPago', color: ESTADO_COLOR.PAGADO, texto: ESTADO_LABEL_INFO.PAGADO },
  { label: 'Recibido', color: ESTADO_COLOR.RECIBIDO, texto: ESTADO_LABEL_INFO.RECIBIDO },
  { label: 'Rechazado por MercadoPago', color: ESTADO_COLOR.RECHAZADO, texto: ESTADO_LABEL_INFO.RECHAZADO },
  { label: 'Acordado por fuera de la app', color: ESTADO_COLOR.ACORDADO_EXTERNO, texto: ESTADO_LABEL_INFO.ACORDADO_EXTERNO },
  { label: 'Venta externa confirmada', color: ESTADO_COLOR.COMPLETADO_EXTERNO, texto: ESTADO_LABEL_INFO.COMPLETADO_EXTERNO },
  { label: 'Venta externa no realizada', color: ESTADO_COLOR.RECHAZADO_EXTERNO, texto: ESTADO_LABEL_INFO.RECHAZADO_EXTERNO },
]
