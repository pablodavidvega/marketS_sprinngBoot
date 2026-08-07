import { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate, useSearchParams, Link } from 'react-router-dom'
import { getProducto, getResenas, crearResena } from '../api/productos'
import { getPreguntas, crearPregunta, responderPregunta } from '../api/preguntas'
import type { Producto, ResenasResponse, PreguntasResponse } from '../types'
import { useCarrito } from '../context/CarritoContext'
import { useAuth } from '../context/AuthContext'
import { useFavoritos } from '../context/FavoritosContext'
import { buildImageUrl } from '../utils/image'
import Stars from '../components/Stars'
import ReputacionBar from '../components/ReputacionBar'

// ─── Tipo de ítem en el carrusel de medios ─────────────────────────────────
type MediaItem =
  | { type: 'video'; url: string }
  | { type: 'image'; url: string }

export default function DetallePage() {
  const { id }          = useParams<{ id: string }>()
  const [searchParams]  = useSearchParams()
  const navigate        = useNavigate()
  const { agregar }     = useCarrito()
  const preguntasRef    = useRef<HTMLDivElement>(null)
  const { user, isAuthenticated } = useAuth()
  const { esFavorito, toggleFavorito } = useFavoritos()
  const [producto, setProducto] = useState<Producto | null>(null)
  const [loading, setLoading]   = useState(true)
  const [activoIdx, setActivoIdx] = useState(0)
  const [cantidad, setCantidad]   = useState(1)
  const [agregado, setAgregado]   = useState(false)

  // Reseñas
  const [resenas, setResenas] = useState<ResenasResponse | null>(null)
  const [paginaResenas, setPaginaResenas] = useState(0)
  const [cargandoMasResenas, setCargandoMasResenas] = useState(false)
  const [calificacion, setCalificacion] = useState(0)
  const [comentario, setComentario] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [errorResena, setErrorResena] = useState('')

  // Preguntas y respuestas
  const [preguntas, setPreguntas] = useState<PreguntasResponse | null>(null)
  const [paginaPreguntas, setPaginaPreguntas] = useState(0)
  const [cargandoMasPreguntas, setCargandoMasPreguntas] = useState(false)
  const [nuevaPregunta, setNuevaPregunta] = useState('')
  const [enviandoPregunta, setEnviandoPregunta] = useState(false)
  const [errorPregunta, setErrorPregunta] = useState('')
  const [respuestas, setRespuestas] = useState<Record<number, string>>({})
  const [respondiendoId, setRespondiendoId] = useState<number | null>(null)
  const [errorRespuesta, setErrorRespuesta] = useState<Record<number, string>>({})

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
    setPaginaResenas(0)
    getResenas(Number(id))
      .then(r => setResenas(r.data))
      .catch(() => setResenas({ promedio: 0, total: 0, puedeResenar: false, yaReseno: false, resenas: [], paginaActual: 0, totalPaginas: 0 }))
  }, [id, user?.email])

  useEffect(() => {
    if (!id) return
    setPreguntas(null)
    setPaginaPreguntas(0)
    getPreguntas(Number(id))
      .then(r => setPreguntas(r.data))
      .catch(() => setPreguntas({ total: 0, puedeResponder: false, preguntas: [], paginaActual: 0, totalPaginas: 0 }))
  }, [id, user?.email])

  function handleVerMasResenas() {
    if (!id || !resenas || cargandoMasResenas) return
    setCargandoMasResenas(true)
    getResenas(Number(id), paginaResenas + 1)
      .then(r => {
        setResenas(prev => prev ? { ...r.data, resenas: [...prev.resenas, ...r.data.resenas] } : r.data)
        setPaginaResenas(r.data.paginaActual)
      })
      .finally(() => setCargandoMasResenas(false))
  }

  function handleVerMasPreguntas() {
    if (!id || !preguntas || cargandoMasPreguntas) return
    setCargandoMasPreguntas(true)
    getPreguntas(Number(id), paginaPreguntas + 1)
      .then(r => {
        setPreguntas(prev => prev ? { ...r.data, preguntas: [...prev.preguntas, ...r.data.preguntas] } : r.data)
        setPaginaPreguntas(r.data.paginaActual)
      })
      .finally(() => setCargandoMasPreguntas(false))
  }

  // Deep-link desde la campanita de notificaciones (?foco=preguntas): baja hasta la sección
  // de preguntas una vez que ya se cargaron (si no, la página aún no tiene esa altura).
  useEffect(() => {
    if (searchParams.get('foco') !== 'preguntas' || !preguntas) return
    preguntasRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, [searchParams, preguntas])

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
      stock:      producto.stock,
      vendedorId: producto.vendedorId ?? null,
      vendedorUsername: producto.vendedorUsername ?? null,
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

  async function handleEnviarPregunta(e: React.FormEvent) {
    e.preventDefault()
    const contenido = nuevaPregunta.trim()
    if (!contenido) return
    setEnviandoPregunta(true)
    setErrorPregunta('')
    try {
      const { data } = await crearPregunta(Number(id), contenido)
      setPreguntas(prev => prev ? { ...prev, total: prev.total + 1, preguntas: [data, ...prev.preguntas] } : prev)
      setNuevaPregunta('')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error
      setErrorPregunta(msg || 'Error al enviar la pregunta')
    } finally {
      setEnviandoPregunta(false)
    }
  }

  async function handleResponder(preguntaId: number) {
    const respuesta = (respuestas[preguntaId] || '').trim()
    if (!respuesta) return
    setRespondiendoId(preguntaId)
    setErrorRespuesta(prev => ({ ...prev, [preguntaId]: '' }))
    try {
      const { data } = await responderPregunta(Number(id), preguntaId, respuesta)
      setPreguntas(prev => prev ? {
        ...prev,
        preguntas: prev.preguntas.map(p => p.id === preguntaId ? data : p),
      } : prev)
      setRespuestas(prev => ({ ...prev, [preguntaId]: '' }))
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error
      setErrorRespuesta(prev => ({ ...prev, [preguntaId]: msg || 'Error al enviar la respuesta' }))
    } finally {
      setRespondiendoId(null)
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
      stock:      producto.stock,
      vendedorId: producto.vendedorId ?? null,
      vendedorUsername: producto.vendedorUsername ?? null,
    })
    navigate('/carrito')
  }

  function handleToggleFavorito() {
    if (!producto) return
    if (!isAuthenticated) {
      navigate('/login')
      return
    }
    toggleFavorito(producto.id)
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <button
        onClick={() => navigate(-1)}
        className="text-sm text-gray-500 hover:text-primary-600 mb-4 flex items-center gap-1"
      >
        ← Volver
      </button>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
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

          {producto.vendedorId && producto.vendedorUsername && (
            <div className="flex items-center gap-3 flex-wrap">
              <Link
                to={`/vendedores/${producto.vendedorId}`}
                className="text-sm text-primary-600 hover:text-primary-700 hover:underline transition-colors"
              >
                @{producto.vendedorUsername}
              </Link>
              {producto.reputacionVendedor && (
                <ReputacionBar compact reputacion={producto.reputacionVendedor} />
              )}
            </div>
          )}

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

          {producto.totalVendido != null && producto.totalVendido > 0 && (
            <p className="text-sm text-gray-500">
              {producto.totalVendido.toLocaleString('es-CO')} {producto.totalVendido === 1 ? 'unidad vendida' : 'unidades vendidas'}
            </p>
          )}

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
          <button
            onClick={handleToggleFavorito}
            className="btn-secondary mt-2 flex items-center justify-center gap-2"
          >
            <span className={esFavorito(producto.id) ? 'text-red-500' : ''}>
              {esFavorito(producto.id) ? '♥' : '♡'}
            </span>
            {esFavorito(producto.id) ? 'Guardado en favoritos' : 'Guardar en favoritos'}
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
            <>
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
              {paginaResenas < resenas.totalPaginas - 1 && (
                <div className="flex justify-center mt-6">
                  <button
                    onClick={handleVerMasResenas}
                    disabled={cargandoMasResenas}
                    className="btn-secondary px-6"
                  >
                    {cargandoMasResenas ? 'Cargando...' : 'Ver más'}
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      )}

      {/* ── Sección de preguntas y respuestas ───────────────────────────────── */}
      {preguntas && (
        <div ref={preguntasRef} className="mt-12 border-t pt-8">
          <h2 className="text-xl font-bold text-gray-800 mb-6">
            Preguntas y respuestas {preguntas.total > 0 && <span className="text-gray-400 font-normal text-base">({preguntas.total})</span>}
          </h2>

          {isAuthenticated ? (
            <form onSubmit={handleEnviarPregunta} className="card mb-8">
              <h3 className="font-semibold text-gray-700 mb-3">¿Tienes una duda sobre este producto?</h3>
              <textarea
                value={nuevaPregunta}
                onChange={e => setNuevaPregunta(e.target.value)}
                placeholder="Escribe tu pregunta..."
                maxLength={500}
                rows={2}
                className="input w-full resize-none text-sm"
              />
              {errorPregunta && <p className="text-red-500 text-sm mt-2">{errorPregunta}</p>}
              <button type="submit" disabled={enviandoPregunta || !nuevaPregunta.trim()} className="btn-primary mt-3">
                {enviandoPregunta ? 'Enviando...' : 'Preguntar'}
              </button>
            </form>
          ) : (
            <div className="bg-gray-50 border border-gray-200 text-gray-500 rounded-lg p-3 text-sm mb-6">
              <button onClick={() => navigate('/login')} className="text-primary-600 hover:underline font-medium">Inicia sesión</button> para preguntarle al vendedor.
            </div>
          )}

          {preguntas.preguntas.length === 0 ? (
            <p className="text-gray-400 text-sm">Aún no hay preguntas sobre este producto.</p>
          ) : (
            <>
            <div className="flex flex-col gap-5">
              {preguntas.preguntas.map(p => (
                <div key={p.id} className="border-b pb-4 last:border-0">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="font-medium text-sm text-gray-700">{p.usuarioUsername}</span>
                    <span className="text-xs text-gray-400 ml-auto">
                      {new Date(p.fechaPregunta).toLocaleDateString('es-CO', { year: 'numeric', month: 'short', day: 'numeric' })}
                    </span>
                  </div>
                  <p className="text-sm text-gray-600">{p.contenido}</p>

                  {p.respuesta ? (
                    <div className="mt-2 ml-4 pl-3 border-l-2 border-primary-200">
                      <p className="text-xs font-medium text-primary-600 mb-0.5">Respuesta del vendedor</p>
                      <p className="text-sm text-gray-600">{p.respuesta}</p>
                    </div>
                  ) : preguntas.puedeResponder ? (
                    <div className="mt-2 ml-4">
                      <div className="flex gap-2">
                        <input
                          value={respuestas[p.id] || ''}
                          onChange={e => setRespuestas(prev => ({ ...prev, [p.id]: e.target.value }))}
                          placeholder="Responder..."
                          maxLength={500}
                          className="input flex-1 text-sm"
                        />
                        <button
                          onClick={() => handleResponder(p.id)}
                          disabled={respondiendoId === p.id || !(respuestas[p.id] || '').trim()}
                          className="btn-primary text-sm px-3"
                        >
                          {respondiendoId === p.id ? 'Enviando...' : 'Responder'}
                        </button>
                      </div>
                      {errorRespuesta[p.id] && (
                        <p className="text-red-500 text-xs mt-1">{errorRespuesta[p.id]}</p>
                      )}
                    </div>
                  ) : (
                    <p className="text-xs text-gray-400 mt-1">Aún sin respuesta</p>
                  )}
                </div>
              ))}
            </div>
            {paginaPreguntas < preguntas.totalPaginas - 1 && (
              <div className="flex justify-center mt-6">
                <button
                  onClick={handleVerMasPreguntas}
                  disabled={cargandoMasPreguntas}
                  className="btn-secondary px-6"
                >
                  {cargandoMasPreguntas ? 'Cargando...' : 'Ver más'}
                </button>
              </div>
            )}
            </>
          )}
        </div>
      )}
    </div>
  )
}
