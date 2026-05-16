import { Link } from 'react-router-dom'
import type { Producto } from '../types'
import { useCarrito } from '../context/CarritoContext'
import { buildImageUrl } from '../utils/image'

interface Props {
  producto: Producto
}

export default function ProductoCard({ producto }: Props) {
  const { agregar } = useCarrito()

  const imagenUrl = buildImageUrl(producto.imagen)

  function handleAgregar(e: React.MouseEvent) {
    e.preventDefault()
    agregar({
      productoId: producto.id,
      nombre: producto.nombre,
      precio: producto.precio,
      imagen: imagenUrl,
      cantidad: 1,
    })
  }

  return (
    <div className="card group flex flex-col">
      <Link to={`/productos/${producto.id}`} className="block flex-1">
        <div className="aspect-square bg-gray-100 rounded-lg overflow-hidden mb-3">
          {imagenUrl ? (
            <img
              src={imagenUrl}
              alt={producto.nombre}
              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-gray-400 text-sm">
              Sin imagen
            </div>
          )}
        </div>
        {producto.categoriaNombre && (
          <span className="badge badge-primary text-xs mb-1 inline-block">
            {producto.categoriaNombre}
          </span>
        )}
        <h3 className="font-semibold text-gray-800 line-clamp-2 mb-1">{producto.nombre}</h3>
        <p className="text-primary-600 font-bold text-lg">
          ${producto.precio.toLocaleString('es-CO')}
        </p>
      </Link>
      <button
        onClick={handleAgregar}
        disabled={producto.stock === 0}
        className="btn-primary w-full mt-3 text-sm disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {producto.stock === 0 ? 'Sin stock' : 'Agregar al carrito'}
      </button>
    </div>
  )
}
