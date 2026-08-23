import { useState, useCallback } from 'react'
import { Link } from 'react-router-dom'
import api from '../../api/axios'
import { ESTADO_COLOR, ESTADO_INFO } from '../../config/estadosPedido'
import { useDebounce } from '../../hooks/useDebounce'
import { usePaginatedResource } from '../../hooks/usePaginatedResource'
import Paginador from '../../components/Paginador'
import { formatMonedaEntera } from '../../utils/formato'

interface PedidoMod {
  id: number
  comprador: string
  fecha: string
  estado: string
  total: number
}

interface PedidosModResponse {
  contenido: PedidoMod[]
  paginaActual: number
  totalPaginas: number
  totalElementos: number
}

function formatFecha(s: string) {
  return new Date(s).toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

export default function ModeradorPedidosPage() {
  const [busqueda, setBusqueda] = useState('')
  const busquedaDebounced = useDebounce(busqueda, 350)
  const [filtroEstado, setFiltroEstado] = useState('')

  const cargarPedidos = useCallback((p: number) => api.get<PedidosModResponse>('/api/moderador/pedidos', {
    params: { page: p, size: 30, estado: filtroEstado || undefined, q: busquedaDebounced || undefined },
  }), [filtroEstado, busquedaDebounced])

  const { data, loading, error, pagina, setPagina } =
    usePaginatedResource<PedidosModResponse>(cargarPedidos, 'No se pudieron cargar los pedidos. Intenta de nuevo.')

  const pedidos = data?.contenido ?? []
  const totalMostrado = pedidos.reduce((s, p) => s + p.total, 0)

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center gap-4 mb-2">
        <Link to="/moderador/dashboard" className="inline-flex items-center gap-1 -ml-2 pl-2 pr-3 py-1 rounded-md text-sm font-medium text-gray-600 hover:text-primary-600 hover:bg-gray-100 transition-colors">
          ← Panel moderador
        </Link>
      </div>
      <div className="mb-6 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Pedidos</h1>
          <p className="text-gray-500 text-sm mt-0.5">Vista de solo lectura de todos los pedidos de la plataforma.</p>
        </div>
        <input
          className="input max-w-xs text-sm"
          placeholder="Buscar por comprador o ID..."
          value={busqueda}
          onChange={e => { setBusqueda(e.target.value); if (pagina !== 0) setPagina(0) }}
        />
      </div>

      {/* Filtros de estado */}
      <div className="flex gap-2 mb-5 flex-wrap">
        <button
          onClick={() => { setFiltroEstado(''); if (pagina !== 0) setPagina(0) }}
          className={`px-3 py-1 rounded-full text-xs font-medium border transition-colors
            ${filtroEstado === '' ? 'bg-primary-600 text-white border-primary-600' : 'bg-white text-gray-600 border-gray-200 hover:border-gray-300'}`}
        >
          Todos
        </button>
        {Object.keys(ESTADO_COLOR).map(e => (
          <button
            key={e}
            onClick={() => { setFiltroEstado(e); if (pagina !== 0) setPagina(0) }}
            title={ESTADO_INFO[e]}
            className={`px-3 py-1 rounded-full text-xs font-medium border transition-colors
              ${filtroEstado === e ? 'bg-primary-600 text-white border-primary-600' : 'bg-white text-gray-600 border-gray-200 hover:border-gray-300'}`}
          >
            {e.replace(/_/g, ' ')}
          </button>
        ))}
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3 mb-4">
          {error}
        </div>
      )}

      {loading ? (
        <div className="flex justify-center py-16">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
        </div>
      ) : pedidos.length === 0 ? (
        <p className="text-center text-gray-400 py-16">No hay pedidos que coincidan.</p>
      ) : (
        <>
          <div className="card p-0 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-gray-50 border-b border-gray-200">
                    <th className="text-left px-4 py-3 font-medium text-gray-600">#</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-600">Comprador</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-600">Fecha</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-600">Estado</th>
                    <th className="text-right px-4 py-3 font-medium text-gray-600">Total</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {pedidos.map(p => (
                    <tr key={p.id} className="hover:bg-gray-50 transition-colors">
                      <td className="px-4 py-3 text-gray-400 font-mono text-xs">#{p.id}</td>
                      <td className="px-4 py-3 font-medium text-gray-800">@{p.comprador}</td>
                      <td className="px-4 py-3 text-gray-500 whitespace-nowrap">{formatFecha(p.fecha)}</td>
                      <td className="px-4 py-3">
                        <span
                          className={`text-xs font-medium border px-2 py-0.5 rounded-full cursor-help ${ESTADO_COLOR[p.estado] ?? 'bg-gray-100 text-gray-600 border-gray-200'}`}
                          title={ESTADO_INFO[p.estado] ?? ''}
                        >
                          {p.estado.replace(/_/g, ' ')}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right font-semibold text-gray-800">
                        {formatMonedaEntera(p.total)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="flex items-center justify-between mt-3">
            <p className="text-sm text-gray-500">
              {data?.totalElementos ?? 0} pedidos en total · Suma de esta página:{' '}
              <span className="font-semibold text-gray-800">
                {formatMonedaEntera(totalMostrado)}
              </span>
            </p>
            <Paginador paginaActual={pagina} totalPaginas={data?.totalPaginas ?? 0} onCambiarPagina={setPagina} />
          </div>
        </>
      )}
    </div>
  )
}
