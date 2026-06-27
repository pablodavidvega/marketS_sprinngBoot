import { useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import type { Producto } from '../types'
import { useCarrito } from '../context/CarritoContext'
import { buildImageUrl } from '../utils/image'

interface Props {
  producto: Producto
}

export default function ProductoCard({ producto }: Props) {
  const { agregar } = useCarrito()
  const navigate    = useNavigate()
  const videoRef = useRef<HTMLVideoElement>(null)
  const [videoActivo,    setVideoActivo]    = useState(false)
  const [videoCargando,  setVideoCargando]  = useState(false)

  const imagenUrl = buildImageUrl(producto.imagen)
  const videoUrl  = buildImageUrl(producto.video)

  // ── Hover: reproducir / pausar video ──────────────────────────────────────
  function handleMouseEnter() {
    if (!videoUrl || !videoRef.current) return
    setVideoActivo(true)
    setVideoCargando(true)
    videoRef.current.play().catch(() => {})
  }

  function handleMouseLeave() {
    if (!videoRef.current) return
    videoRef.current.pause()
    videoRef.current.currentTime = 0
    setVideoActivo(false)
    setVideoCargando(false)
  }

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

  function handlePagar(e: React.MouseEvent) {
    e.preventDefault()
    agregar({
      productoId: producto.id,
      nombre: producto.nombre,
      precio: producto.precio,
      imagen: imagenUrl,
      cantidad: 1,
    })
    navigate('/carrito')
  }

  return (
    <div
      className="card group flex flex-col"
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
    >
      <Link to={`/productos/${producto.id}`} className="block flex-1">
        {/* ── Miniatura (imagen + video overlay) ──────────────────────── */}
        <div className="aspect-square bg-gray-100 rounded-lg overflow-hidden mb-3 relative">
          {/* Imagen de fondo */}
          {imagenUrl ? (
            <img
              src={imagenUrl}
              alt={producto.nombre}
              className={`absolute inset-0 w-full h-full object-cover transition-all duration-300 ${
                videoActivo ? 'opacity-0' : 'opacity-100 group-hover:scale-105'
              }`}
            />
          ) : (
            <div className={`absolute inset-0 flex items-center justify-center text-gray-400 text-sm transition-opacity duration-300 ${videoActivo ? 'opacity-0' : 'opacity-100'}`}>
              Sin imagen
            </div>
          )}

          {/* Video (se muestra sobre la imagen al hacer hover) */}
          {videoUrl && (
            <video
              ref={videoRef}
              src={videoUrl}
              muted
              loop
              playsInline
              preload="none"
              onWaiting={() => setVideoCargando(true)}
              onCanPlay={() => setVideoCargando(false)}
              onPlaying={() => setVideoCargando(false)}
              className={`absolute inset-0 w-full h-full object-cover transition-opacity duration-300 ${
                videoActivo ? 'opacity-100' : 'opacity-0'
              }`}
            />
          )}

          {/* Spinner mientras el video bufferea */}
          {videoActivo && videoCargando && (
            <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
              <div className="w-8 h-8 rounded-full border-2 border-white/40 border-t-white animate-spin" />
            </div>
          )}

          {/* Badge "▶ Video" — visible sólo cuando no está reproduciendo */}
          {videoUrl && !videoActivo && (
            <div className="absolute bottom-2 right-2 bg-black/60 text-white text-[10px] px-2 py-0.5 rounded-full flex items-center gap-1 pointer-events-none">
              <span>▶</span> Video
            </div>
          )}
        </div>

        {/* ── Info del producto ─────────────────────────────────────────── */}
        {producto.categoriaNombre && (
          <span className="badge badge-primary text-xs mb-1 inline-block">
            {producto.categoriaNombre}
          </span>
        )}
        <h3 className="font-semibold text-gray-800 line-clamp-2 mb-0.5">
          {producto.nombre}
        </h3>
        {producto.vendedorId && producto.vendedorUsername && (
          <Link
            to={`/vendedores/${producto.vendedorId}`}
            onClick={e => e.stopPropagation()}
            className="text-xs text-gray-400 hover:text-primary-600 transition-colors mb-1 inline-block"
          >
            @{producto.vendedorUsername}
          </Link>
        )}
        {producto.promedioCalificacion != null && producto.totalResenas != null && producto.totalResenas > 0 && (
          <div className="flex items-center gap-1 mb-1">
            <span className="text-yellow-400 text-sm leading-none">
              {'★'.repeat(Math.round(producto.promedioCalificacion))}
              {'☆'.repeat(5 - Math.round(producto.promedioCalificacion))}
            </span>
            <span className="text-xs text-gray-400">({producto.totalResenas})</span>
          </div>
        )}
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
      <button
        onClick={handlePagar}
        disabled={producto.stock === 0}
        className="btn-success w-full mt-2 text-sm"
      >
        {producto.stock === 0 ? 'Sin stock' : 'Comprar ahora'}
      </button>
    </div>
  )
}
