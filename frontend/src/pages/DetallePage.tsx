import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getProducto, getResenas, crearResena } from '../api/productos'
import type { Producto, ResenasResponse } from '../types'
import { useCarrito } from '../context/CarritoContext'
import { useAuth } from '../context/AuthContext'
import { buildImageUrl } from '../utils/image'

// ─── Estrellas ────────────────────────────────────────────────────────────────
function Stars({ value, max = 5, size = 'sm', interactive = false, onChange }: {
  value: number
  max?: number
  size?: 'sm' | 'lg'
  interactive?: boolean
  onChange?: (v: number) => void
}) {
  const [hover, setHover] = useState(0)
  const sz = size === 'lg' ? 'text-2xl' : 'text-base'
  return (
    <span className={`flex gap-0.5 ${sz}`}>
      {Array.from({ length: max }, (_, i) => i + 1).map(i => (
        <span
          key={i}
          className={`${interactive ? 'cursor-pointer' : ''} transition-colors leading-none`}
          style={{ color: i <= (interactive ? hover || value : value) ? '#f59e0b' : '#d1d5db' }}
          onMouseEnter={() => interactive && setHover(i)}
          onMouseLeave={() => interactive && setHover(0)}
          onClick={() => interactive && onChange?.(i)}
        >
          ★
        </span>
      ))}
    </span>
  )
}

// ─── Tipo de ítem en el carrusel de medios ─────────────────────────────────
type MediaItem =
  | { type: 'video'; url: string }
  | { type: 'image'; url: string }

export default function DetallePage() {
  const { id }          = useParams<{ id: string }>()
  const navigate        = useNavigate()
  const { agregar }     = useCarrito()
  const { user }        = useAuth()
  const [producto, setProducto] = useState<Producto | null>(null)
  const [loading, setLoading]   = useState(true)
  const [activoIdx, setActivoIdx] = useState(0)
  const [cantidad, setCantidad]   = useState(1)
  const [agregado, setAgregado]   = useState(false)

  // Reseñas
  const [resenas, setResenas] = useState<ResenasResponse | null>(null)
  const [calificacion, setCalificacion] = useState(0)
  const [comentario, setComentario] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [errorResena, setErrorResena] = useState('')

  useEffect(() => {
    if (!id) return
    getProducto(Number(id))
      .then(r => {
        setProducto(r.data)
        setActivoIdx(0)
      })
      .catch(() => navigate('/'))
      .finally(() => setLoading(false))
  }, [id, navigate])

  useEffect(() => {
    if (!id) return
    setResenas(null)
    getResenas(Number(id)).then(r => setResenas(r.data)).catch(() => setResenas({ promedio: 0, total: 0, puedeResenar: false, yaReseno: false, resenas: [] }))
  }, [id, user?.email])

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

  async function handleEnviarResena(e: React.FormEvent) {
    e.preventDefault()
    if (calificacion === 0) { setErrorResena('Selecciona una calificación'); return }
    setEnviando(true)
    setErrorResena('')
    try {
      const { data } = await crearResena(Number(id), { calificacion, comentario: comentario.trim() || undefined })
      setResenas(prev => prev ? {
        ...prev,
        resenas: [data, ...prev.resenas],
        total: prev.total + 1,
        promedio: Math.round(((prev.promedio * prev.total + calificacion) / (prev.total + 1)) * 10) / 10,
        puedeResenar: false,
        yaReseno: true,
      } : prev)
      setCalificacion(0)
      setComentario('')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error
      setErrorResena(msg || 'Error al enviar la reseña')
    } finally {
      setEnviando(false)
    }
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

          {/* Promedio de calificación */}
          {resenas && (
            <div className="flex items-center gap-2">
              <Stars value={resenas.promedio} />
              <span className="text-sm font-medium text-gray-700">{resenas.promedio > 0 ? resenas.promedio.toFixed(1) : '—'}</span>
              <span className="text-sm text-gray-400">({resenas.total} {resenas.total === 1 ? 'reseña' : 'reseñas'})</span>
            </div>
          )}

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

      {/* ── Sección de reseñas ─────────────────────────────────────────────── */}
      {resenas && (
        <div className="mt-12 border-t pt-8">
          <h2 className="text-xl font-bold text-gray-800 mb-6">
            Reseñas {resenas.total > 0 && <span className="text-gray-400 font-normal text-base">({resenas.total})</span>}
          </h2>

          {/* Formulario para dejar reseña */}
          {resenas.puedeResenar && (
            <form onSubmit={handleEnviarResena} className="card mb-8">
              <h3 className="font-semibold text-gray-700 mb-3">Deja tu calificación</h3>
              <div className="flex items-center gap-3 mb-3">
                <Stars value={calificacion} size="lg" interactive onChange={setCalificacion} />
                {calificacion > 0 && (
                  <span className="text-sm text-gray-500">
                    {['', 'Muy malo', 'Malo', 'Regular', 'Bueno', 'Excelente'][calificacion]}
                  </span>
                )}
              </div>
              <textarea
                value={comentario}
                onChange={e => setComentario(e.target.value)}
                placeholder="Comentario opcional..."
                maxLength={500}
                rows={3}
                className="input w-full resize-none text-sm"
              />
              {errorResena && <p className="text-red-500 text-sm mt-2">{errorResena}</p>}
              <button type="submit" disabled={enviando} className="btn-primary mt-3">
                {enviando ? 'Enviando...' : 'Publicar reseña'}
              </button>
            </form>
          )}

          {resenas.yaReseno && (
            <div className="bg-green-50 border border-green-200 text-green-700 rounded-lg p-3 text-sm mb-6">
              Ya dejaste una reseña para este producto.
            </div>
          )}

          {/* Lista de reseñas */}
          {resenas.resenas.length === 0 ? (
            <p className="text-gray-400 text-sm">Este producto aún no tiene reseñas. ¡Sé el primero en opinar!</p>
          ) : (
            <div className="flex flex-col gap-4">
              {resenas.resenas.map(r => (
                <div key={r.id} className="border-b pb-4 last:border-0">
                  <div className="flex items-center gap-2 mb-1">
                    <Stars value={r.calificacion} />
                    <span className="font-medium text-sm text-gray-700">{r.compradorUsername}</span>
                    <span className="text-xs text-gray-400 ml-auto">
                      {new Date(r.fecha).toLocaleDateString('es-CO', { year: 'numeric', month: 'short', day: 'numeric' })}
                    </span>
                  </div>
                  {r.comentario && <p className="text-sm text-gray-600">{r.comentario}</p>}
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
