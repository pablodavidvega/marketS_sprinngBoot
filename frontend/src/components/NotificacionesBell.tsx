import { useState, useRef, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useNotificaciones } from '../hooks/useNotificaciones'
import { useAuth } from '../context/AuthContext'
import type { Notificacion } from '../types'

function formatTiempo(fechaStr: string) {
  const diff = Date.now() - new Date(fechaStr).getTime()
  const min  = Math.floor(diff / 60_000)
  const h    = Math.floor(diff / 3_600_000)
  const d    = Math.floor(diff / 86_400_000)
  if (min < 1)  return 'ahora'
  if (min < 60) return `hace ${min} min`
  if (h < 24)   return `hace ${h} h`
  return `hace ${d} d`
}

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

function destinoDe(n: Notificacion, esVendedor: boolean): string {
  switch (n.tipo) {
    case 'NUEVO_PEDIDO':
    case 'PEDIDO_CREADO':
    case 'PEDIDO_PAGADO':
    case 'PEDIDO_PAGO_PENDIENTE':
    case 'PEDIDO_PAGO_RECHAZADO':
    case 'PEDIDO_RECIBIDO':
    case 'PEDIDO_COMPLETADO_EXTERNO':
    case 'PEDIDO_RECHAZADO_EXTERNO':
    case 'CALIFICACION_COMPRADOR':
      return n.pedidoId !== '' ? destinoPedido(esVendedor, n.pedidoId, '') : (esVendedor ? '/vendedor/pedidos' : '/cliente/mis-pedidos')
    case 'NUEVO_MENSAJE_CHAT':
    case 'RECORDATORIO_CHAT_SIN_RESPONDER':
      return n.pedidoId !== '' ? destinoPedido(esVendedor, n.pedidoId, n.vendedorId, 'chat') : (esVendedor ? '/vendedor/pedidos' : '/cliente/mis-pedidos')
    case 'ENCUENTRO_PROPUESTO':
    case 'ENCUENTRO_ACTUALIZADO':
    case 'ENCUENTRO_CANCELADO':
      return n.pedidoId !== '' ? destinoPedido(esVendedor, n.pedidoId, n.vendedorId, 'encuentro') : (esVendedor ? '/vendedor/pedidos' : '/cliente/mis-pedidos')
    case 'NUEVA_PREGUNTA':
    case 'PREGUNTA_RESPONDIDA':
    case 'RECORDATORIO_PREGUNTA_SIN_RESPONDER':
      return n.productoId !== '' ? `/productos/${n.productoId}?foco=preguntas` : '/'
    default:
      return esVendedor ? '/vendedor/pedidos' : '/cliente/mis-pedidos'
  }
}

export default function NotificacionesBell() {
  const { items, noLeidas, cargar, marcarLeida, marcarTodasLeidas } = useNotificaciones()
  const { hasRole } = useAuth()
  const esVendedor = hasRole('VENDEDOR') || hasRole('ADMIN')
  const [abierto, setAbierto] = useState(false)
  const ref     = useRef<HTMLDivElement>(null)
  const navigate = useNavigate()

  // Cerrar al hacer click fuera
  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setAbierto(false)
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  async function handleClick(n: Notificacion) {
    await marcarLeida(n.id)
    setAbierto(false)
    navigate(destinoDe(n, esVendedor))
  }

  return (
    <div ref={ref} className="relative">
      {/* Botón campanita */}
      <button
        onClick={() => {
          setAbierto(v => !v)
          if (!abierto) cargar()
        }}
        className="relative p-2 text-gray-600 hover:text-primary-600 transition-colors"
        aria-label="Notificaciones"
      >
        <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
            d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
        {noLeidas > 0 && (
          <span className="absolute -top-0.5 -right-0.5 bg-red-500 text-white text-[10px] font-bold rounded-full min-w-[18px] h-[18px] flex items-center justify-center px-1 leading-none">
            {noLeidas > 99 ? '99+' : noLeidas}
          </span>
        )}
      </button>

      {/* Dropdown */}
      {abierto && (
        <div className="absolute right-0 top-full mt-2 w-80 bg-white rounded-xl shadow-lg border border-gray-200 z-50 overflow-hidden">
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
            <span className="text-sm font-semibold text-gray-800">
              Notificaciones
              {noLeidas > 0 && (
                <span className="ml-1.5 text-xs font-medium text-white bg-red-500 rounded-full px-1.5 py-0.5">
                  {noLeidas}
                </span>
              )}
            </span>
            {noLeidas > 0 && (
              <button
                onClick={() => marcarTodasLeidas()}
                className="text-xs text-primary-600 hover:text-primary-700 font-medium"
              >
                Marcar todas como leídas
              </button>
            )}
          </div>

          {/* Lista */}
          <div className="max-h-80 overflow-y-auto divide-y divide-gray-50">
            {items.length === 0 ? (
              <p className="text-center text-sm text-gray-400 py-8">Sin notificaciones</p>
            ) : (
              items.map(n => (
                <button
                  key={n.id}
                  onClick={() => handleClick(n)}
                  className={`w-full text-left px-4 py-3 hover:bg-gray-50 transition-colors flex gap-3 items-start
                    ${n.leida ? 'opacity-60' : ''}`}
                >
                  {/* Indicador no leída */}
                  <div className="mt-1.5 flex-shrink-0">
                    {n.leida
                      ? <div className="w-2 h-2 rounded-full bg-gray-200" />
                      : <div className="w-2 h-2 rounded-full bg-primary-600" />
                    }
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className={`text-sm leading-snug ${n.leida ? 'text-gray-500' : 'text-gray-800 font-medium'}`}>
                      {n.mensaje}
                    </p>
                    <p className="text-xs text-gray-400 mt-0.5">{formatTiempo(n.fechaCreacion)}</p>
                  </div>
                </button>
              ))
            )}
          </div>

          {/* Footer */}
          {items.length > 0 && (
            <div className="border-t border-gray-100 px-4 py-2.5 text-center">
              <button
                onClick={() => { setAbierto(false); navigate(esVendedor ? '/vendedor/pedidos' : '/cliente/mis-pedidos') }}
                className="text-xs text-primary-600 hover:text-primary-700 font-medium"
              >
                Ver todos los pedidos →
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
