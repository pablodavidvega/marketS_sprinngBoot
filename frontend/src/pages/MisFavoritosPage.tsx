import { useCallback } from 'react'
import { Link } from 'react-router-dom'
import { getMisFavoritos } from '../api/favoritos'
import ProductoCard from '../components/ProductoCard'
import Paginador from '../components/Paginador'
import { usePaginatedResource } from '../hooks/usePaginatedResource'
import { useFavoritos } from '../context/FavoritosContext'
import type { VendedorProductosResponse } from '../types'

const FAVORITOS_POR_PAGINA = 30

export default function MisFavoritosPage() {
  const { esFavorito, cargado } = useFavoritos()

  const cargarFavoritos = useCallback(
    (p: number) => getMisFavoritos({ page: p, size: FAVORITOS_POR_PAGINA }),
    [],
  )
  const { data, loading, error, pagina, setPagina } =
    usePaginatedResource<VendedorProductosResponse>(cargarFavoritos, 'No se pudieron cargar tus favoritos. Intenta de nuevo.')

  // Se filtra contra el contexto (una vez cargado) para que quitar un favorito
  // con el corazón lo saque de esta lista al instante, sin esperar a un refetch.
  const productos = data?.contenido ?? []
  const visibles = cargado ? productos.filter(p => esFavorito(p.id)) : productos

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Mis favoritos</h1>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3 mb-4">
          {error}
        </div>
      )}

      {loading ? (
        <div className="flex justify-center py-16">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
        </div>
      ) : visibles.length === 0 ? (
        <div className="card text-center py-12">
          <p className="text-gray-400 text-lg mb-4">Aún no has guardado productos como favoritos</p>
          <Link to="/" className="btn-primary">Explorar productos</Link>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
            {visibles.map((p, i) => (
              <ProductoCard key={p.id} producto={p} prioridad={i < 4} />
            ))}
          </div>
          <div className="flex justify-center mt-8">
            <Paginador paginaActual={pagina} totalPaginas={data?.totalPaginas ?? 0} onCambiarPagina={setPagina} />
          </div>
        </>
      )}
    </div>
  )
}
