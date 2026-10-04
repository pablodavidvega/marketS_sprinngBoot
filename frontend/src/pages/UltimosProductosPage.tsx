import { useCallback } from 'react'
import { Link } from 'react-router-dom'
import { getProductosRecientesPaginado } from '../api/productos'
import type { VendedorProductosResponse } from '../types'
import ProductoCard from '../components/ProductoCard'
import Paginador from '../components/Paginador'
import { usePaginatedResource } from '../hooks/usePaginatedResource'

// Múltiplo de 2, 3 y 4 (columnas de la grilla según breakpoint) para no dejar filas a medias.
// Menos cards por página = menos imágenes pedidas a Supabase Storage por vista.
const PRODUCTOS_POR_PAGINA = 24

export default function UltimosProductosPage() {
  const cargarProductos = useCallback(
    (p: number) => getProductosRecientesPaginado(p, PRODUCTOS_POR_PAGINA),
    [],
  )
  const { data, loading, error, pagina, setPagina } =
    usePaginatedResource<VendedorProductosResponse>(cargarProductos, 'Error al cargar los productos')

  const productos = data?.contenido ?? []

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center gap-4 mb-2">
        <Link to="/" className="inline-flex items-center gap-1 -ml-2 pl-2 pr-3 py-1 rounded-md text-sm font-medium text-gray-600 hover:text-primary-600 hover:bg-gray-100 transition-colors">
          ← Volver al inicio
        </Link>
      </div>
      <h1 className="text-2xl font-bold text-gray-800 mb-1">Últimos productos agregados</h1>
      <p className="text-gray-500 text-sm mb-6">
        Todos los productos del catálogo, ordenados del más reciente al más antiguo.
      </p>

      {loading ? (
        <div className="flex justify-center py-16">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
        </div>
      ) : error ? (
        <div className="text-center py-16 text-red-500">{error}</div>
      ) : productos.length === 0 ? (
        <div className="text-center py-16 text-gray-400">No hay productos disponibles</div>
      ) : (
        <>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
            {productos.map((p, i) => <ProductoCard key={p.id} producto={p} prioridad={i < 4} />)}
          </div>
          <div className="flex justify-center mt-8">
            <Paginador paginaActual={pagina} totalPaginas={data?.totalPaginas ?? 0} onCambiarPagina={setPagina} />
          </div>
        </>
      )}
    </div>
  )
}
