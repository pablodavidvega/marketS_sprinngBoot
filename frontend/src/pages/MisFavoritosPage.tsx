import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { getMisFavoritos } from '../api/favoritos'
import ProductoCard from '../components/ProductoCard'
import { useFavoritos } from '../context/FavoritosContext'
import type { Producto } from '../types'

const FAVORITOS_POR_PAGINA = 30

export default function MisFavoritosPage() {
  const { esFavorito, cargado } = useFavoritos()
  const [productos, setProductos] = useState<Producto[]>([])
  const [pagina, setPagina] = useState(0)
  const [totalPaginas, setTotalPaginas] = useState(0)
  const [loading, setLoading] = useState(true)
  const [cargandoMas, setCargandoMas] = useState(false)

  useEffect(() => {
    getMisFavoritos({ page: 0, size: FAVORITOS_POR_PAGINA })
      .then(r => {
        setProductos(r.data.contenido)
        setPagina(r.data.paginaActual)
        setTotalPaginas(r.data.totalPaginas)
      })
      .finally(() => setLoading(false))
  }, [])

  function verMas() {
    setCargandoMas(true)
    getMisFavoritos({ page: pagina + 1, size: FAVORITOS_POR_PAGINA })
      .then(r => {
        setProductos(prev => [...prev, ...r.data.contenido])
        setPagina(r.data.paginaActual)
        setTotalPaginas(r.data.totalPaginas)
      })
      .finally(() => setCargandoMas(false))
  }

  // Se filtra contra el contexto (una vez cargado) para que quitar un favorito
  // con el corazón lo saque de esta lista al instante, sin esperar a un refetch.
  const visibles = cargado ? productos.filter(p => esFavorito(p.id)) : productos

  if (loading) {
    return (
      <div className="flex justify-center py-16">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
      </div>
    )
  }

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Mis favoritos</h1>

      {visibles.length === 0 ? (
        <div className="card text-center py-12">
          <p className="text-gray-400 text-lg mb-4">Aún no has guardado productos como favoritos</p>
          <Link to="/" className="btn-primary">Explorar productos</Link>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
            {visibles.map(p => (
              <ProductoCard key={p.id} producto={p} />
            ))}
          </div>
          {pagina < totalPaginas - 1 && (
            <div className="flex justify-center mt-8">
              <button
                onClick={verMas}
                disabled={cargandoMas}
                className="btn-secondary px-6 disabled:opacity-50"
              >
                {cargandoMas ? 'Cargando...' : 'Ver más'}
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
