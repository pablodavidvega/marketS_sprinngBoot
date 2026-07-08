import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { getDashboardCliente } from '../../api/usuario'
import { confirmarRecepcion } from '../../api/pedidos'
import ChatPedido from '../../components/ChatPedido'
import ReputacionBar from '../../components/ReputacionBar'
import type { DashboardCliente } from '../../types'

export default function ClienteDashboardPage() {
  const [data, setData] = useState<DashboardCliente | null>(null)
  const [loading, setLoading] = useState(true)
  const [confirmando, setConfirmando] = useState<number | null>(null)
  const [expandido, setExpandido] = useState<number | null>(null)
  const [chatAbierto, setChatAbierto] = useState<string | null>(null)

  useEffect(() => {
    getDashboardCliente()
      .then(r => setData(r.data))
      .finally(() => setLoading(false))
  }, [])

  async function handleConfirmarRecepcion(pedidoId: number) {
    setConfirmando(pedidoId)
    try {
      await confirmarRecepcion(pedidoId)
      setData(prev => prev ? {
        ...prev,
        pedidosRecientes: prev.pedidosRecientes.map(p =>
          p.pedidoId === pedidoId ? { ...p, estado: 'RECIBIDO' } : p
        ),
      } : prev)
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

  if (!data) return <div className="text-center py-16 text-gray-500">Error al cargar datos</div>

  const estadoColor: Record<string, string> = {
    PAGADO: 'bg-green-100 text-green-700',
    RECIBIDO: 'bg-emerald-100 text-emerald-700',
    PENDIENTE: 'bg-yellow-100 text-yellow-700',
    PENDIENTE_PAGO: 'bg-yellow-100 text-yellow-700',
    CANCELADO: 'bg-red-100 text-red-700',
    RECHAZADO: 'bg-red-100 text-red-700',
    PROCESANDO_PAGO: 'bg-blue-100 text-blue-700',
    ACORDADO_EXTERNO: 'bg-blue-100 text-blue-700',
    COMPLETADO_EXTERNO: 'bg-green-100 text-green-700',
    RECHAZADO_EXTERNO: 'bg-red-100 text-red-700',
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Mi cuenta</h1>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
        <div className="card text-center">
          <p className="text-3xl font-bold text-primary-600">{data.totalPedidos}</p>
          <p className="text-sm text-gray-500 mt-1">Pedidos totales</p>
        </div>
        <div className="card text-center">
          <p className="text-3xl font-bold text-green-600">{data.pedidosPagados}</p>
          <p className="text-sm text-gray-500 mt-1">Pedidos pagados</p>
        </div>
        <div className="card text-center">
          <p className="text-3xl font-bold text-primary-600">${data.gastoTotal.toLocaleString('es-CO')}</p>
          <p className="text-sm text-gray-500 mt-1">Gasto total</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Profile */}
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-800">Mi perfil</h2>
            <Link to="/cliente/editar-perfil" className="text-sm text-primary-600 hover:underline">
              Editar →
            </Link>
          </div>
          <div className="flex flex-col gap-2 text-sm">
            <div>
              <span className="text-gray-500">Usuario:</span>
              <span className="ml-2 font-medium">{data.perfil.username}</span>
            </div>
            <div>
              <span className="text-gray-500">Email:</span>
              <span className="ml-2 font-medium">{data.perfil.email}</span>
            </div>
            <div>
              <span className="text-gray-500">Teléfono:</span>
              <span className="ml-2 font-medium">{data.perfil.phone || '—'}</span>
            </div>
            <div>
              <span className="text-gray-500">Dirección:</span>
              <span className="ml-2 font-medium">{data.perfil.address || '—'}</span>
            </div>
            {data.perfil.city && (
              <div>
                <span className="text-gray-500">Ciudad:</span>
                <span className="ml-2 font-medium">{data.perfil.city}</span>
              </div>
            )}
          </div>
          <div className="mt-4 pt-4 border-t border-gray-100">
            <ReputacionBar reputacion={data.reputacion} label="Tu reputación como comprador" />
          </div>
        </div>

        {/* Recent orders */}
        <div className="card lg:col-span-2">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-800">Pedidos recientes</h2>
            <Link to="/cliente/mis-pedidos" className="text-sm text-primary-600 hover:underline">
              Ver todos →
            </Link>
          </div>
          {data.pedidosRecientes.length === 0 ? (
            <p className="text-gray-400 text-sm">No tienes pedidos aún</p>
          ) : (
            <div className="flex flex-col gap-3">
              {data.pedidosRecientes.map(pedido => (
                <div key={pedido.pedidoId} className="p-3 bg-gray-50 rounded-lg">
                  <div
                    className="flex items-center justify-between cursor-pointer"
                    onClick={() => setExpandido(expandido === pedido.pedidoId ? null : pedido.pedidoId)}
                  >
                    <div>
                      <p className="font-medium text-gray-800">Pedido #{pedido.pedidoId}</p>
                      <p className="text-sm text-gray-500">
                        {new Date(pedido.fecha).toLocaleDateString('es-CO')} · {pedido.detalles.length} ítem(s)
                      </p>
                    </div>
                    <div className="flex items-center gap-3">
                      <span className={`badge ${estadoColor[pedido.estado] ?? 'bg-gray-100 text-gray-700'}`}>
                        {pedido.estado.replace(/_/g, ' ')}
                      </span>
                      <span className="font-semibold text-gray-800">
                        ${pedido.total.toLocaleString('es-CO')}
                      </span>
                      <span className="text-gray-400 text-sm">
                        {expandido === pedido.pedidoId ? '▲' : '▼'}
                      </span>
                    </div>
                  </div>

                  {expandido === pedido.pedidoId && (
                    <div className="mt-3 pt-3 border-t border-gray-200 flex flex-col gap-3">
                      {pedido.detalles.map((item, i) => (
                        <div key={i} className="flex items-center gap-3">
                          {item.productoImagen ? (
                            <img
                              src={item.productoImagen}
                              alt={item.productoNombre}
                              className="w-12 h-12 object-cover rounded-lg flex-shrink-0"
                            />
                          ) : (
                            <div className="w-12 h-12 bg-gray-100 rounded-lg flex-shrink-0 flex items-center justify-center text-gray-300 text-lg">
                              📦
                            </div>
                          )}
                          <div className="flex-1 min-w-0">
                            <p className="font-medium text-gray-800 truncate text-sm">{item.productoNombre}</p>
                            <p className="text-xs text-gray-500">
                              {item.cantidad} × ${item.precioUnitario.toLocaleString('es-CO')}
                            </p>
                          </div>
                          <span className="font-semibold text-gray-700 text-sm flex-shrink-0">
                            ${item.subtotal.toLocaleString('es-CO')}
                          </span>
                        </div>
                      ))}

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
                          <div key={key} className="pt-2 border-t border-gray-200">
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

                      {pedido.estado === 'PAGADO' && (
                        <div className="pt-2 border-t border-gray-200">
                          <button
                            onClick={() => handleConfirmarRecepcion(pedido.pedidoId)}
                            disabled={confirmando === pedido.pedidoId}
                            className="btn-primary text-sm w-full"
                          >
                            {confirmando === pedido.pedidoId ? 'Confirmando...' : 'Confirmar recepción'}
                          </button>
                        </div>
                      )}

                      {pedido.estado === 'ACORDADO_EXTERNO' && (
                        <p className="text-sm text-gray-500 pt-2 border-t border-gray-200">
                          Acordaste el pago por fuera de la app ({pedido.metodoPago?.toLowerCase()}). Coordina la entrega con el vendedor por chat; él confirmará cuando la venta se concrete.
                        </p>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
