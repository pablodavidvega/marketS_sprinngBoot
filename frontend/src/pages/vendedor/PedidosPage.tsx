import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { getPedidosVendedor, marcarCompletado, marcarRechazado } from '../../api/pedidos'
import ChatPedido from '../../components/ChatPedido'
import type { PedidoVendedorResponse } from '../../types'

const ESTADO_COLOR: Record<string, string> = {
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

export default function PedidosVendedorPage() {
  const [pedidos, setPedidos] = useState<PedidoVendedorResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [expandido, setExpandido] = useState<number | null>(null)
  const [chatAbierto, setChatAbierto] = useState<number | null>(null)
  const [resolviendo, setResolviendo] = useState<number | null>(null)

  useEffect(() => {
    getPedidosVendedor()
      .then(r => setPedidos(r.data))
      .finally(() => setLoading(false))
  }, [])

  async function handleResolver(pedidoId: number, completado: boolean) {
    setResolviendo(pedidoId)
    try {
      const { data } = completado ? await marcarCompletado(pedidoId) : await marcarRechazado(pedidoId)
      setPedidos(prev => prev.map(p => p.pedidoId === pedidoId ? { ...p, estado: data.estado } : p))
    } finally {
      setResolviendo(null)
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
        <Link to="/vendedor" className="text-gray-400 hover:text-gray-600 text-sm">
          ← Panel vendedor
        </Link>
        <h1 className="text-2xl font-bold text-gray-800">Pedidos recibidos</h1>
      </div>

      {pedidos.length === 0 ? (
        <div className="card text-center py-12">
          <p className="text-gray-400 text-lg">Aún no has recibido pedidos</p>
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          {pedidos.map(pedido => (
            <div key={pedido.pedidoId} className="card">
              {/* Cabecera */}
              <div
                className="flex items-center justify-between cursor-pointer"
                onClick={() => setExpandido(expandido === pedido.pedidoId ? null : pedido.pedidoId)}
              >
                <div>
                  <p className="font-semibold text-gray-800">Pedido #{pedido.pedidoId}</p>
                  <p className="text-sm text-gray-500">
                    {new Date(pedido.fecha).toLocaleDateString('es-CO', {
                      year: 'numeric', month: 'long', day: 'numeric',
                    })}
                    {' · '}{pedido.items.length} ítem(s)
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <span className={`badge ${ESTADO_COLOR[pedido.estado] ?? 'bg-gray-100 text-gray-700'}`}>
                    {pedido.estado.replace(/_/g, ' ')}
                  </span>
                  <span className="text-gray-400 text-sm">
                    {expandido === pedido.pedidoId ? '▲' : '▼'}
                  </span>
                </div>
              </div>

              {/* Detalle expandible */}
              {expandido === pedido.pedidoId && (
                <div className="mt-4 pt-4 border-t border-gray-100 flex flex-col gap-5">
                  {/* Datos del comprador */}
                  <div className="bg-gray-50 rounded-lg p-4">
                    <p className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-2">
                      Datos del comprador
                    </p>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-sm">
                      <div>
                        <span className="text-gray-500">Usuario: </span>
                        <span className="font-medium text-gray-800">{pedido.compradorUsername}</span>
                      </div>
                      <div>
                        <span className="text-gray-500">Email: </span>
                        <span className="font-medium text-gray-800">{pedido.compradorEmail}</span>
                      </div>
                      <div>
                        <span className="text-gray-500">Teléfono: </span>
                        <span className="font-medium text-gray-800">{pedido.compradorPhone || '—'}</span>
                      </div>
                      <div>
                        <span className="text-gray-500">Dirección: </span>
                        <span className="font-medium text-gray-800">{pedido.compradorAddress || '—'}</span>
                      </div>
                    </div>
                  </div>

                  {/* Productos */}
                  <div>
                    <p className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-2">
                      Productos
                    </p>
                    <div className="flex flex-col gap-3">
                      {pedido.items.map((item, i) => (
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
                          <div className="text-right flex-shrink-0">
                            <p className="text-xs text-gray-400">Tu ganancia</p>
                            <p className="font-semibold text-green-600">
                              ${item.gananciaVendedor.toLocaleString('es-CO')}
                            </p>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Chat con el comprador sobre la entrega */}
                  <div className="border-t border-gray-100 pt-4">
                    <button
                      onClick={() => setChatAbierto(chatAbierto === pedido.pedidoId ? null : pedido.pedidoId)}
                      className="text-sm text-primary-600 hover:text-primary-700 font-medium"
                    >
                      {chatAbierto === pedido.pedidoId ? '▲' : '💬'} Chat con {pedido.compradorUsername}
                    </button>
                    {chatAbierto === pedido.pedidoId && (
                      <div className="mt-3">
                        <ChatPedido pedidoId={pedido.pedidoId} vendedorId={pedido.vendedorId} />
                      </div>
                    )}
                  </div>

                  {/* Venta acordada por fuera de la app: el vendedor confirma si se concretó */}
                  {pedido.estado === 'ACORDADO_EXTERNO' && (
                    <div className="border-t border-gray-100 pt-4">
                      <p className="text-sm text-gray-500 mb-3">
                        Este pedido se acordó por fuera de la app ({pedido.metodoPago?.toLowerCase()}). Confirma si la venta se realizó.
                      </p>
                      <div className="flex gap-3">
                        <button
                          onClick={() => handleResolver(pedido.pedidoId, true)}
                          disabled={resolviendo === pedido.pedidoId}
                          className="btn-primary flex-1"
                        >
                          {resolviendo === pedido.pedidoId ? 'Guardando...' : 'Compra realizada'}
                        </button>
                        <button
                          onClick={() => handleResolver(pedido.pedidoId, false)}
                          disabled={resolviendo === pedido.pedidoId}
                          className="btn-secondary flex-1"
                        >
                          {resolviendo === pedido.pedidoId ? 'Guardando...' : 'No se realizó'}
                        </button>
                      </div>
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
