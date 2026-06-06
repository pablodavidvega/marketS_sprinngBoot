import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getProducto } from '../api/productos'
import type { Producto } from '../types'
import { useCarrito } from '../context/CarritoContext'
import { buildImageUrl } from '../utils/image'

// ─── Tipo de ítem en el carrusel de medios ─────────────────────────────────
type MediaItem =
  | { type: 'video'; url: string }
  | { type: 'image'; url: string }

export default function DetallePage() {
  const { id }          = useParams<{ id: string }>()
  const navigate        = useNavigate()
  const { agregar }     = useCarrito()
  const [producto, setProducto] = useState<Producto | null>(null)
  const [loading, setLoading]   = useState(true)
  const [activoIdx, setActivoIdx] = useState(0)
  const [cantidad, setCantidad]   = useState(1)
  const [agregado, setAgregado]   = useState(false)

  useEffect(() => {
    if (!id) return
    getProducto(Number(id))
      .then(r => {
        setProducto(r.data)
        setActivoIdx(0) // el primer ítem (video si existe) se activa automáticamente
      })
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

  // ── Construir lista de medios: video primero, luego imágenes ─────────────
  const mediaItems: MediaItem[] = []

  const videoUrl = buildImageUrl(producto.video)
  if (videoUrl) mediaItems.push({ type: 'video', url: videoUrl })

  ;[producto.imagen, producto.imagen2, producto.imagen3]
    .filter((img): img is string => !!img)
    .forEach(img => {
      const url = buildImageUrl(img)
      if (url) mediaItems.push({ type: 'image', url })
    })

  const itemActivo = mediaItems[activoIdx] ?? null

  // ── Carrito ──────────────────────────────────────────────────────────────
  function handleAgregar() {
    if (!producto) return
    const primeraImagen = mediaItems.find(m => m.type === 'image')?.url ?? null
    agregar({
      productoId: producto.id,
      nombre:     producto.nombre,
      precio:     producto.precio,
      imagen:     primeraImagen,
      cantidad,
    })
    setAgregado(true)
    setTimeout(() => setAgregado(false), 2000)
  }

  function handlePagar() {
    if (!producto) return
    const primeraImagen = mediaItems.find(m => m.type === 'image')?.url ?? null
    agregar({
      productoId: producto.id,
      nombre:     producto.nombre,
      precio:     producto.precio,
      imagen:     primeraImagen,
      cantidad,
    })
    navigate('/carrito')
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <button
        onClick={() => navigate(-1)}
        className="text-sm text-gray-500 hover:text-primary-600 mb-4 flex items-center gap-1"
      >
        ← Volver
      </button>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* ── Columna de medios ──────────────────────────────────────────── */}
        <div>
          {/* Visor principal */}
          <div className="aspect-square bg-gray-100 rounded-xl overflow-hidden mb-3">
            {itemActivo ? (
              itemActivo.type === 'video' ? (
                /* Video con reproducción automática al entrar a la página */
                <video
                  key={activoIdx}           // remonta el elemento al cambiar de ítem
                  src={itemActivo.url}
                  autoPlay
                  muted
                  loop
                  playsInline
                  controls
                  className="w-full h-full object-cover"
                />
              ) : (
                <img
                  src={itemActivo.url}
                  alt={producto.nombre}
                  className="w-full h-full object-cover"
                />
              )
            ) : (
              <div className="w-full h-full flex items-center justify-center text-gray-400">
                Sin imagen
              </div>
            )}
          </div>

          {/* Miniaturas (video + imágenes) */}
          {mediaItems.length > 1 && (
            <div className="flex gap-2 flex-wrap">
              {mediaItems.map((item, i) => (
                <button
                  key={i}
                  onClick={() => setActivoIdx(i)}
                  className={`w-16 h-16 rounded-lg overflow-hidden border-2 transition-colors flex-shrink-0 ${
                    activoIdx === i
                      ? 'border-primary-500'
                      : 'border-gray-200 hover:border-gray-400'
                  }`}
                >
                  {item.type === 'video' ? (
                    /* Miniatura del video: fondo oscuro con ícono ▶ */
                    <div className="w-full h-full bg-gray-900 flex flex-col items-center justify-center gap-0.5">
                      <span className="text-white text-lg leading-none">▶</span>
                      <span className="text-white text-[9px] font-medium">Video</span>
                    </div>
                  ) : (
                    <img
                      src={item.url}
                      alt=""
                      className="w-full h-full object-cover"
                    />
                  )}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* ── Columna de información ─────────────────────────────────────── */}
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

          {/* Selector de cantidad */}
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
          <button
            onClick={handlePagar}
            disabled={producto.stock === 0}
            className="btn-success mt-2"
          >
            {producto.stock === 0 ? 'Sin stock' : 'Comprar ahora'}
          </button>

          {producto.marcaNombre && (
            <p className="text-sm text-gray-400">Marca: {producto.marcaNombre}</p>
          )}
        </div>
      </div>
    </div>
  )
}
