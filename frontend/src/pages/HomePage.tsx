import { useState, useEffect, useCallback, useRef } from 'react'
import { getProductos, getProductosPorCategoria, getCategorias } from '../api/productos'
import type { Categoria, VendedorProductosResponse } from '../types'
import ProductoCard from '../components/ProductoCard'
import Paginador from '../components/Paginador'
import BuscadorAvanzado from '../components/BuscadorAvanzado'
import ProductosRecientesSlider from '../components/ProductosRecientesSlider'
import BotonVolverABuscar from '../components/BotonVolverABuscar'
import { usePaginatedResource } from '../hooks/usePaginatedResource'

// Múltiplo de 2, 3 y 4 (columnas de la grilla según breakpoint) para no dejar filas a medias.
// Menos cards por página = menos imágenes pedidas a Supabase Storage por vista.
const PRODUCTOS_POR_PAGINA = 24

export default function HomePage() {
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [categoriaActiva, setCategoriaActiva] = useState<number | null>(null)
  // filtro sólo cambia cuando el usuario confirma la búsqueda (submit / selección)
  const [filtro, setFiltro] = useState('')
  const buscadorRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    getCategorias()
      .then(r => setCategorias(r.data))
      .catch(() => {})
  }, [])

  // Con categoría activa o sin ella (búsqueda general/"Todos"): mismo paginado real
  // server-side (page/size), controlado con <Paginador> igual que el resto de la app.
  const cargarProductos = useCallback(
    (p: number) => categoriaActiva
      ? getProductosPorCategoria(categoriaActiva, p, PRODUCTOS_POR_PAGINA)
      : getProductos(filtro || undefined, p, PRODUCTOS_POR_PAGINA),
    [categoriaActiva, filtro],
  )
  const { data, loading, error, pagina, setPagina } =
    usePaginatedResource<VendedorProductosResponse>(cargarProductos, 'Error al cargar productos')

  const productos = data?.contenido ?? []

  function handleSearch(q: string) {
    setCategoriaActiva(null)
    setFiltro(q)
    setPagina(0)
  }

  function handleCategoria(id: number | null) {
    setCategoriaActiva(id)
    setFiltro('')
    setPagina(0)
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
            {productos.map((p, i) => (
              <ProductoCard key={p.id} producto={p} prioridad={i < 4} />
            ))}
          </div>
          <div className="flex justify-center mt-8">
            <Paginador paginaActual={pagina} totalPaginas={data?.totalPaginas ?? 0} onCambiarPagina={setPagina} />
          </div>
        </>
      )}
    </div>
    <BotonVolverABuscar targetRef={buscadorRef} />
    </>
  )
}
