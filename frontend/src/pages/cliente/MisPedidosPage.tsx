import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { getMisPedidos, confirmarRecepcion } from '../../api/pedidos'
import ChatPedido from '../../components/ChatPedido'
import type { PedidoResponse } from '../../types'

const ESTADO_COLOR: Record<string, string> = {
  PAGADO: 'bg-green-100 text-green-700',
  RECIBIDO: 'bg-emerald-100 text-emerald-700',
  PENDIENTE: 'bg-yellow-100 text-yellow-700',
  PENDIENTE_PAGO: 'bg-yellow-100 text-yellow-700',
  RECHAZADO: 'bg-red-100 text-red-700',
  CANCELADO: 'bg-red-100 text-red-700',
  PROCESANDO_PAGO: 'bg-blue-100 text-blue-700',
}

export default function MisPedidosPage() {
  const [pedidos, setPedidos] = useState<PedidoResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [expandido, setExpandido] = useState<number | null>(null)
  const [confirmando, setConfirmando] = useState<number | null>(null)
  const [chatAbierto, setChatAbierto] = useState<string | null>(null)

  useEffect(() => {
    getMisPedidos()
      .then(r => setPedidos(r.data))
      .finally(() => setLoading(false))
  }, [])

  async function handleConfirmarRecepcion(pedidoId: number) {
    setConfirmando(pedidoId)
    try {
      await confirmarRecepcion(pedidoId)
      setPedidos(prev =>
        prev.map(p => p.pedidoId === pedidoId ? { ...p, estado: 'RECIBIDO' } : p)
      )
    } finally {
      setConfirmando(null)
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center py-16">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center gap-4 mb-6">
        <Link to="/cliente/dashboard" className="text-gray-400 hover:text-gray-600 text-sm">
          ← Mi cuenta
        </Link>
        <h1 className="text-2xl font-bold text-gray-800">Mis pedidos</h1>
      </div>

      {pedidos.length === 0 ? (
        <div className="card text-center py-12">
          <p className="text-gray-400 text-lg mb-4">No tienes pedidos aún</p>
          <Link to="/" className="btn-primary">Explorar productos</Link>
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          {pedidos.map(pedido => (
            <div key={pedido.pedidoId} className="card">
              {/* Cabecera del pedido */}
              <div
                className="flex items-center justify-between cursor-pointer"
                onClick={() => setExpandido(expandido === pedido.pedidoId ? null : pedido.pedidoId)}
              >
                <div className="flex items-center gap-4">
                  <div>
                    <p className="font-semibold text-gray-800">Pedido #{pedido.pedidoId}</p>
                    <p className="text-sm text-gray-500">
                      {new Date(pedido.fecha).toLocaleDateString('es-CO', {
                        year: 'numeric', month: 'long', day: 'numeric',
                      })}
                      {' · '}{pedido.detalles.length} ítem(s)
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <span className={`badge ${ESTADO_COLOR[pedido.estado] ?? 'bg-gray-100 text-gray-700'}`}>
                    {pedido.estado.replace(/_/g, ' ')}
                  </span>
                  <span className="font-bold text-gray-800 text-lg">
                    ${pedido.total.toLocaleString('es-CO')}
                  </span>
                  <span className="text-gray-400 text-sm">
                    {expandido === pedido.pedidoId ? '▲' : '▼'}
                  </span>
                </div>
              </div>

              {/* Detalle expandible */}
              {expandido === pedido.pedidoId && (
                <div className="mt-4 pt-4 border-t border-gray-100">
                  <div className="flex flex-col gap-3">
                    {pedido.detalles.map((item, i) => (
                      <div key={i} className="flex items-center gap-3">
                        {item.productoImagen ? (
                          <img
                            src={item.productoImagen}
                            alt={item.productoNombre}
                            className="w-14 h-14 object-cover rounded-lg flex-shrink-0"
                          />
                        ) : (
                          <div className="w-14 h-14 bg-gray-100 rounded-lg flex-shrink-0 flex items-center justify-center text-gray-300 text-xl">
                            📦
                          </div>
                        )}
                        <div className="flex-1 min-w-0">
                          <p className="font-medium text-gray-800 truncate">{item.productoNombre}</p>
                          <p className="text-sm text-gray-500">
                            {item.cantidad} × ${item.precioUnitario.toLocaleString('es-CO')}
                          </p>
                        </div>
                        <span className="font-semibold text-gray-700 flex-shrink-0">
                          ${item.subtotal.toLocaleString('es-CO')}
                        </span>
                      </div>
                    ))}
                  </div>

                  {/* Chat con cada vendedor del pedido (un pedido puede tener varios) */}
                  {Array.from(
                    new Map(
                      pedido.detalles
                        .filter((d): d is typeof d & { vendedorId: number; vendedorUsername: string } => d.vendedorId != null)
                        .map(d => [d.vendedorId, d.vendedorUsername])
                    )
                  ).map(([vendedorId, vendedorUsername]) => {
                    const key = `${pedido.pedidoId}-${vendedorId}`
                    return (
                      <div key={key} className="mt-4 pt-4 border-t border-gray-100">
                        <button
                          onClick={() => setChatAbierto(chatAbierto === key ? null : key)}
                          className="text-sm text-primary-600 hover:text-primary-700 font-medium"
                        >
                          {chatAbierto === key ? '▲' : '💬'} Chat con {vendedorUsername}
                        </button>
                        {chatAbierto === key && (
                          <div className="mt-3">
                            <ChatPedido pedidoId={pedido.pedidoId} vendedorId={vendedorId} />
                          </div>
                        )}
                      </div>
                    )
                  })}

                  {pedido.paymentUrl && pedido.estado === 'PENDIENTE' && (
                    <div className="mt-4 pt-4 border-t border-gray-100">
                      <a
                        href={pedido.paymentUrl}
                        className="btn-primary w-full text-center block"
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        Pagar con MercadoPago
                      </a>
                    </div>
                  )}

                  {pedido.estado === 'PAGADO' && (
                    <div className="mt-4 pt-4 border-t border-gray-100">
                      <p className="text-sm text-gray-500 mb-3">
                        ¿Ya recibiste tu pedido? Confirma la recepción para cerrar la compra.
                      </p>
                      <button
                        onClick={() => handleConfirmarRecepcion(pedido.pedidoId)}
                        disabled={confirmando === pedido.pedidoId}
                        className="btn-primary w-full"
                      >
                        {confirmando === pedido.pedidoId ? 'Confirmando...' : 'Confirmar recepción'}
                      </button>
                    </div>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
