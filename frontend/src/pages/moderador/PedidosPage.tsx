import { useEffect, useState, useMemo } from 'react'
import api from '../../api/axios'

interface PedidoMod {
  id: number
  comprador: string
  fecha: string
  estado: string
  total: number
}

const ESTADO_CLASES: Record<string, string> = {
  PAGADO:    'text-green-700 bg-green-50 border-green-200',
  PENDIENTE: 'text-amber-700 bg-amber-50 border-amber-200',
  CANCELADO: 'text-red-600 bg-red-50 border-red-200',
  ENVIADO:   'text-blue-700 bg-blue-50 border-blue-200',
}

function estadoClase(estado: string) {
  return ESTADO_CLASES[estado] ?? 'text-gray-600 bg-gray-100 border-gray-200'
}

function formatFecha(s: string) {
  return new Date(s).toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

export default function ModeradorPedidosPage() {
  const [pedidos,  setPedidos]  = useState<PedidoMod[]>([])
  const [loading,  setLoading]  = useState(true)
  const [busqueda, setBusqueda] = useState('')
  const [filtroEstado, setFiltroEstado] = useState('TODOS')

  useEffect(() => {
    api.get<PedidoMod[]>('/api/moderador/pedidos')
      .then(res => setPedidos(res.data))
      .finally(() => setLoading(false))
  }, [])

  const estados = useMemo(() => {
    const set = new Set(pedidos.map(p => p.estado))
    return ['TODOS', ...Array.from(set)]
  }, [pedidos])

  const lista = useMemo(() => {
    let res = pedidos
    if (filtroEstado !== 'TODOS') res = res.filter(p => p.estado === filtroEstado)
    if (busqueda.trim()) {
      const q = busqueda.toLowerCase()
      res = res.filter(p =>
        p.comprador.toLowerCase().includes(q) ||
        String(p.id).includes(q)
      )
    }
    return res
  }, [pedidos, filtroEstado, busqueda])

  const totalMostrado = lista.reduce((s, p) => s + p.total, 0)

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-6 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Pedidos</h1>
          <p className="text-gray-500 text-sm mt-0.5">Vista de solo lectura de todos los pedidos de la plataforma.</p>
        </div>
        <input
          className="input max-w-xs text-sm"
          placeholder="Buscar por comprador o ID..."
          value={busqueda}
          onChange={e => setBusqueda(e.target.value)}
        />
      </div>

      {/* Filtros de estado */}
      <div className="flex gap-2 mb-5 flex-wrap">
        {estados.map(e => (
          <button
            key={e}
            onClick={() => setFiltroEstado(e)}
            className={`px-3 py-1 rounded-full text-xs font-medium border transition-colors
              ${filtroEstado === e
                ? 'bg-primary-600 text-white border-primary-600'
                : 'bg-white text-gray-600 border-gray-200 hover:border-gray-300'}`}
          >
            {e === 'TODOS' ? `Todos (${pedidos.length})` : `${e} (${pedidos.filter(p => p.estado === e).length})`}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="flex justify-center py-16">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
        </div>
      ) : lista.length === 0 ? (
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
                  {lista.map(p => (
                    <tr key={p.id} className="hover:bg-gray-50 transition-colors">
                      <td className="px-4 py-3 text-gray-400 font-mono text-xs">#{p.id}</td>
                      <td className="px-4 py-3 font-medium text-gray-800">@{p.comprador}</td>
                      <td className="px-4 py-3 text-gray-500 whitespace-nowrap">{formatFecha(p.fecha)}</td>
                      <td className="px-4 py-3">
                        <span className={`text-xs font-medium border px-2 py-0.5 rounded-full ${estadoClase(p.estado)}`}>
                          {p.estado}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right font-semibold text-gray-800">
                        ${p.total.toLocaleString('es-CO', { maximumFractionDigits: 0 })}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
          <p className="text-right text-sm text-gray-500 mt-3">
            {lista.length} pedidos · Total:{' '}
            <span className="font-semibold text-gray-800">
              ${totalMostrado.toLocaleString('es-CO', { maximumFractionDigits: 0 })}
            </span>
          </p>
        </>
      )}
    </div>
  )
}
