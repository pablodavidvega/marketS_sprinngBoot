import { useState, useCallback } from 'react'
import { Link } from 'react-router-dom'
import api from '../../api/axios'
import { buildImageUrl } from '../../utils/image'
import { useDebounce } from '../../hooks/useDebounce'
import { usePaginatedResource } from '../../hooks/usePaginatedResource'
import Paginador from '../../components/Paginador'
import { formatMoneda } from '../../utils/formato'
import type { Producto } from '../../types'

type FiltroEstado = 'todos' | 'activos' | 'inactivos'

interface ProductosModResponse {
  contenido: Producto[]
  paginaActual: number
  totalPaginas: number
  totalElementos: number
  totalActivos: number
  totalInactivos: number
}

export default function ModeradorProductosPage() {
  const [busqueda,  setBusqueda]  = useState('')
  const busquedaDebounced = useDebounce(busqueda, 350)
  const [filtro,    setFiltro]    = useState<FiltroEstado>('todos')
  const [toggling,  setToggling]  = useState<number | null>(null)

  const cargarProductos = useCallback((p: number) => api.get<ProductosModResponse>('/api/moderador/productos', {
    params: {
      page: p,
      size: 30,
      activo: filtro === 'activos' ? true : filtro === 'inactivos' ? false : undefined,
      q: busquedaDebounced || undefined,
    },
  }), [filtro, busquedaDebounced])

  const { data, setData, loading, error, pagina, setPagina } =
    usePaginatedResource<ProductosModResponse>(cargarProductos, 'No se pudieron cargar los productos. Intenta de nuevo.')

  async function toggleActivo(id: number) {
    setToggling(id)
    try {
      const res = await api.patch<{ id: number; activo: boolean }>(`/api/moderador/productos/${id}/toggle`)
      setData(prev => prev ? {
        ...prev,
        contenido: prev.contenido.map(p => p.id === id ? { ...p, activo: res.data.activo } : p),
        totalActivos: prev.totalActivos + (res.data.activo ? 1 : -1),
        totalInactivos: prev.totalInactivos + (res.data.activo ? -1 : 1),
      } : prev)
    } finally {
      setToggling(null)
    }
  }

  const lista = data?.contenido ?? []
  const totalActivos   = data?.totalActivos ?? 0
  const totalInactivos = data?.totalInactivos ?? 0

  const FILTROS: { key: FiltroEstado; label: string; count: number }[] = [
    { key: 'todos',     label: 'Todos',     count: totalActivos + totalInactivos },
    { key: 'activos',   label: 'Activos',   count: totalActivos },
    { key: 'inactivos', label: 'Inactivos', count: totalInactivos },
  ]

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center gap-4 mb-2">
        <Link to="/moderador/dashboard" className="text-gray-400 hover:text-gray-600 text-sm">
          ← Panel moderador
        </Link>
      </div>
      <div className="mb-6 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Productos</h1>
          <p className="text-gray-500 text-sm mt-0.5">Puedes activar o desactivar productos. No puedes editarlos.</p>
        </div>
        <input
          className="input max-w-xs text-sm"
          placeholder="Buscar por nombre, vendedor o categoría..."
          value={busqueda}
          onChange={e => { setBusqueda(e.target.value); if (pagina !== 0) setPagina(0) }}
        />
      </div>

      {/* Filtros */}
      <div className="flex gap-2 mb-5 flex-wrap">
        {FILTROS.map(f => (
          <button
            key={f.key}
            onClick={() => { setFiltro(f.key); if (pagina !== 0) setPagina(0) }}
            className={`flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium border transition-colors
              ${filtro === f.key
                ? 'bg-primary-600 text-white border-primary-600'
                : 'bg-white text-gray-600 border-gray-200 hover:border-gray-300'}`}
          >
            {f.label}
            <span className={`rounded-full px-1.5 text-xs font-semibold
              ${filtro === f.key ? 'bg-white/20 text-white' : 'bg-gray-100 text-gray-500'}`}>
              {f.count}
            </span>
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
      ) : lista.length === 0 ? (
        <p className="text-center text-gray-400 py-16">No hay productos que coincidan.</p>
      ) : (
        <>
          <div className="card p-0 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-gray-50 border-b border-gray-200">
                    <th className="text-left px-4 py-3 font-medium text-gray-600 w-10"></th>
                    <th className="text-left px-4 py-3 font-medium text-gray-600">Producto</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-600">Vendedor</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-600">Categoría</th>
                    <th className="text-right px-4 py-3 font-medium text-gray-600">Precio</th>
                    <th className="text-right px-4 py-3 font-medium text-gray-600">Stock</th>
                    <th className="text-center px-4 py-3 font-medium text-gray-600">Estado</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {lista.map(p => (
                    <tr key={p.id} className={`hover:bg-gray-50 transition-colors ${!p.activo ? 'opacity-60' : ''}`}>
                      <td className="px-4 py-3">
                        {p.imagen
                          ? <img src={buildImageUrl(p.imagen) ?? ''} alt={p.nombre}
                              className="w-10 h-10 rounded-lg object-cover" />
                          : <div className="w-10 h-10 rounded-lg bg-gray-100 flex items-center justify-center text-gray-400 text-xs">—</div>
                        }
                      </td>
                      <td className="px-4 py-3">
                        <p className="font-medium text-gray-800 line-clamp-1">{p.nombre}</p>
                      </td>
                      <td className="px-4 py-3 text-gray-500">
                        {p.vendedorUsername ? `@${p.vendedorUsername}` : '—'}
                      </td>
                      <td className="px-4 py-3 text-gray-500">{p.categoriaNombre ?? '—'}</td>
                      <td className="px-4 py-3 text-right font-medium text-gray-800">
                        {formatMoneda(p.precio)}
                      </td>
                      <td className="px-4 py-3 text-right text-gray-600">{p.stock}</td>
                      <td className="px-4 py-3 text-center">
                        <button
                          onClick={() => toggleActivo(p.id)}
                          disabled={toggling === p.id}
                          title={p.activo ? 'Desactivar producto' : 'Activar producto'}
                          className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border transition-colors
                            ${p.activo
                              ? 'text-green-700 bg-green-50 border-green-200 hover:bg-green-100'
                              : 'text-gray-500 bg-gray-100 border-gray-200 hover:bg-gray-200'
                            } disabled:opacity-50`}
                        >
                          <span className={`w-1.5 h-1.5 rounded-full ${p.activo ? 'bg-green-500' : 'bg-gray-400'}`} />
                          {toggling === p.id ? '...' : p.activo ? 'Activo' : 'Inactivo'}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="flex items-center justify-between mt-3">
            <p className="text-sm text-gray-500">{data?.totalElementos ?? 0} productos en total</p>
            <Paginador paginaActual={pagina} totalPaginas={data?.totalPaginas ?? 0} onCambiarPagina={setPagina} />
          </div>
        </>
      )}
    </div>
  )
}
