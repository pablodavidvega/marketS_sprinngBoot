import { useState, useEffect, useRef } from 'react'
import { getProductos, getProductosPorCategoria, getCategorias } from '../api/productos'
import type { Producto, Categoria } from '../types'
import ProductoCard from '../components/ProductoCard'
import BuscadorAvanzado from '../components/BuscadorAvanzado'
import ProductosRecientesSlider from '../components/ProductosRecientesSlider'
import BotonVolverABuscar from '../components/BotonVolverABuscar'

const PRODUCTOS_POR_PAGINA = 30

export default function HomePage() {
  const [productos, setProductos] = useState<Producto[]>([])
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [categoriaActiva, setCategoriaActiva] = useState<number | null>(null)
  // filtro sólo cambia cuando el usuario confirma la búsqueda (submit / selección)
  const [filtro, setFiltro] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [visibles, setVisibles] = useState(PRODUCTOS_POR_PAGINA)
  const buscadorRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    getCategorias()
      .then(r => setCategorias(r.data))
      .catch(() => {})
  }, [])

  useEffect(() => {
    setLoading(true)
    setError('')
    setVisibles(PRODUCTOS_POR_PAGINA)
    const req = categoriaActiva
      ? getProductosPorCategoria(categoriaActiva)
      : getProductos(filtro || undefined, 200)

    req
      .then(r => setProductos(r.data))
      .catch(() => setError('Error al cargar productos'))
      .finally(() => setLoading(false))
  }, [categoriaActiva, filtro])

  function handleSearch(q: string) {
    setCategoriaActiva(null)
    setFiltro(q)
  }

  function handleCategoria(id: number | null) {
    setCategoriaActiva(id)
    setFiltro('')
  }

  return (
    <>
      {/* Slider de últimos productos agregados — full width */}
      <ProductosRecientesSlider />

    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Buscador avanzado */}
      <div className="mb-6 scroll-mt-24" ref={buscadorRef}>
        <BuscadorAvanzado onSearch={handleSearch} initialValue={filtro} />
      </div>

      {/* Filtros por categoría */}
      <div className="flex flex-wrap gap-2 mb-6">
        <button
          onClick={() => handleCategoria(null)}
          className={`px-3 py-1.5 rounded-full text-sm font-medium transition-colors ${
            categoriaActiva === null && !filtro
              ? 'bg-primary-600 text-white'
              : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
          }`}
        >
          Todos
        </button>
        {categorias.map(cat => (
          <button
            key={cat.id}
            onClick={() => handleCategoria(cat.id)}
            className={`px-3 py-1.5 rounded-full text-sm font-medium transition-colors ${
              categoriaActiva === cat.id
                ? 'bg-primary-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            {cat.nombre}
          </button>
        ))}
      </div>

      {/* Etiqueta de búsqueda activa */}
      {filtro && (
        <div className="flex items-center gap-2 mb-4">
          <span className="text-sm text-gray-500">
            Resultados para: <strong className="text-gray-800">"{filtro}"</strong>
          </span>
          <button
            onClick={() => handleSearch('')}
            className="text-xs text-red-500 hover:text-red-700"
          >
            × Limpiar
          </button>
        </div>
      )}

      {/* Grid de productos */}
      {loading ? (
        <div className="flex justify-center py-16">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
        </div>
      ) : error ? (
        <div className="text-center py-16 text-red-500">{error}</div>
      ) : productos.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          {filtro ? `No se encontraron productos para "${filtro}"` : 'No hay productos disponibles'}
        </div>
      ) : (
        <>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
            {productos.slice(0, visibles).map(p => <ProductoCard key={p.id} producto={p} />)}
          </div>
          {visibles < productos.length && (
            <div className="flex justify-center mt-8">
              <button
                onClick={() => setVisibles(v => v + PRODUCTOS_POR_PAGINA)}
                className="btn-secondary px-6"
              >
                Ver más
              </button>
            </div>
          )}
        </>
      )}
    </div>
    <BotonVolverABuscar targetRef={buscadorRef} />
    </>
  )
}
