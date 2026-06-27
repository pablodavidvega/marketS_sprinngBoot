import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getProductosRecientes } from '../api/productos'
import type { Producto } from '../types'
import ProductoCard from '../components/ProductoCard'

export default function UltimosProductosPage() {
  const [productos, setProductos] = useState<Producto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    getProductosRecientes(0) // limit=0 → todos los productos activos, ordenados del más nuevo al más antiguo
      .then(r => setProductos(r.data))
      .catch(() => setError('Error al cargar los productos'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center gap-4 mb-2">
        <Link to="/" className="text-gray-400 hover:text-primary-600 text-sm">
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
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
          {productos.map(p => <ProductoCard key={p.id} producto={p} />)}
        </div>
      )}
    </div>
  )
}
