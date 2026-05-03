import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getProducto } from '../api/productos'
import type { Producto } from '../types'
import { useCarrito } from '../context/CarritoContext'

export default function DetallePage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { agregar } = useCarrito()
  const [producto, setProducto] = useState<Producto | null>(null)
  const [loading, setLoading] = useState(true)
  const [imagenActiva, setImagenActiva] = useState(0)
  const [cantidad, setCantidad] = useState(1)
  const [agregado, setAgregado] = useState(false)

  useEffect(() => {
    if (!id) return
    getProducto(Number(id))
      .then(r => setProducto(r.data))
      .catch(() => navigate('/'))
      .finally(() => setLoading(false))
  }, [id, navigate])

  if (loading) {
    return (
      <div className="flex justify-center py-24">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
      </div>
    )
  }

  if (!producto) return null

  const imagenes = [producto.imagen, producto.imagen2, producto.imagen3]
    .filter(Boolean)
    .map(img => img!.startsWith('http') ? img! : `/uploads/${img}`)

  function handleAgregar() {
    if (!producto) return
    agregar({
      productoId: producto.id,
      nombre: producto.nombre,
      precio: producto.precio,
      imagen: imagenes[0] ?? null,
      cantidad,
    })
    setAgregado(true)
    setTimeout(() => setAgregado(false), 2000)
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <button onClick={() => navigate(-1)} className="text-sm text-gray-500 hover:text-primary-600 mb-4 flex items-center gap-1">
        ← Volver
      </button>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* Images */}
        <div>
          <div className="aspect-square bg-gray-100 rounded-xl overflow-hidden mb-3">
            {imagenes.length > 0 ? (
              <img src={imagenes[imagenActiva]} alt={producto.nombre} className="w-full h-full object-cover" />
            ) : (
              <div className="w-full h-full flex items-center justify-center text-gray-400">Sin imagen</div>
            )}
          </div>
          {imagenes.length > 1 && (
            <div className="flex gap-2">
              {imagenes.map((img, i) => (
                <button
                  key={i}
                  onClick={() => setImagenActiva(i)}
                  className={`w-16 h-16 rounded-lg overflow-hidden border-2 transition-colors ${
                    imagenActiva === i ? 'border-primary-500' : 'border-gray-200'
                  }`}
                >
                  <img src={img} alt="" className="w-full h-full object-cover" />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Details */}
        <div className="flex flex-col gap-4">
          {producto.categoriaNombre && (
            <span className="badge badge-primary w-fit">{producto.categoriaNombre}</span>
          )}
          <h1 className="text-2xl font-bold text-gray-800">{producto.nombre}</h1>
          <p className="text-3xl font-bold text-primary-600">
            ${producto.precio.toLocaleString('es-CO')}
          </p>

          {producto.descripcion && (
            <p className="text-gray-600 leading-relaxed">{producto.descripcion}</p>
          )}

          {producto.tallas && producto.tallas.length > 0 && (
            <div>
              <p className="text-sm font-medium text-gray-700 mb-2">Tallas disponibles:</p>
              <div className="flex flex-wrap gap-2">
                {producto.tallas.map(t => (
                  <span
                    key={t.talla}
                    className={`px-3 py-1 rounded border text-sm ${
                      t.stock > 0
                        ? 'border-gray-300 text-gray-700'
                        : 'border-gray-200 text-gray-400 line-through'
                    }`}
                  >
                    {t.talla} ({t.stock})
                  </span>
                ))}
              </div>
            </div>
          )}

          <div className="flex items-center gap-3 mt-2">
            <label className="text-sm font-medium text-gray-700">Cantidad:</label>
            <div className="flex items-center border border-gray-300 rounded-lg">
              <button
                onClick={() => setCantidad(q => Math.max(1, q - 1))}
                className="px-3 py-1.5 text-gray-600 hover:bg-gray-100 rounded-l-lg"
              >−</button>
              <span className="px-4 py-1.5 text-gray-800 font-medium">{cantidad}</span>
              <button
                onClick={() => setCantidad(q => Math.min(producto.stock, q + 1))}
                className="px-3 py-1.5 text-gray-600 hover:bg-gray-100 rounded-r-lg"
              >+</button>
            </div>
            <span className="text-sm text-gray-500">{producto.stock} disponibles</span>
          </div>

          <button
            onClick={handleAgregar}
            disabled={producto.stock === 0}
            className={`btn-primary mt-2 ${agregado ? 'bg-green-600 hover:bg-green-700' : ''}`}
          >
            {producto.stock === 0 ? 'Sin stock' : agregado ? '¡Agregado!' : 'Agregar al carrito'}
          </button>

          {producto.marcaNombre && (
            <p className="text-sm text-gray-400">Marca: {producto.marcaNombre}</p>
          )}
        </div>
      </div>
    </div>
  )
}
