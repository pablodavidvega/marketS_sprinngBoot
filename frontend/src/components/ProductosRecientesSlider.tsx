import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getProductosRecientes } from '../api/productos'
import { getBannersActivos } from '../api/banners'
import type { Producto, BannerSlide } from '../types'
import { buildImageUrl } from '../utils/image'
import { formatMoneda } from '../utils/formato'

const DURACION_MIN = 3000
const DURACION_MAX = 5000

// Paleta fija del showcase de "últimos productos" — un color por posición de columna,
// independiente del producto y del color de marca configurable (ver ConfiguracionAppPage).
const PALETA_SHOWCASE = ['#F2A900', '#22252B', '#7C3AED', '#F0466E', '#0EA5B7']

type SlideItem =
  | { tipo: 'resumen'; key: 'resumen'; productos: Producto[] }
  | { tipo: 'banner'; key: string; banner: BannerSlide }

// Resuelve un enlace (ruta relativa o URL completa) a una navegación interna
// (misma app, sin recarga) o externa (otro dominio).
function resolverDestino(enlace: string): { interno: boolean; ruta: string } {
  try {
    const url = new URL(enlace, window.location.origin)
    if (url.origin === window.location.origin) {
      return { interno: true, ruta: `${url.pathname}${url.search}${url.hash}` }
    }
    return { interno: false, ruta: url.href }
  } catch {
    return { interno: true, ruta: enlace }
  }
}

export default function ProductosRecientesSlider() {
  const navigate = useNavigate()
  const [productos, setProductos] = useState<Producto[]>([])
  const [banners, setBanners] = useState<BannerSlide[]>([])
  const [cargado, setCargado] = useState(false)
  const [index, setIndex] = useState(1)
  const [transicion, setTransicion] = useState(true)
  const [reproduciendo, setReproduciendo] = useState(true)

  useEffect(() => {
    Promise.all([
      getProductosRecientes(5).then(r => setProductos(r.data)).catch(() => {}),
      getBannersActivos().then(r => setBanners(r.data)).catch(() => {}),
    ]).finally(() => setCargado(true))
  }, [])

  // Diapositiva 1: resumen con los 10 productos más nuevos. Luego, los banners del CMS.
  const items: SlideItem[] = useMemo(() => [
    ...(productos.length > 0 ? [{ tipo: 'resumen' as const, key: 'resumen' as const, productos }] : []),
    ...banners.map(b => ({ tipo: 'banner' as const, key: `b-${b.id}`, banner: b })),
  ], [productos, banners])

  const n = items.length

  // Cada diapositiva tiene su propia duración aleatoria entre 3 y 5 segundos
  const duraciones = useMemo(
    () => items.map(() => DURACION_MIN + Math.random() * (DURACION_MAX - DURACION_MIN)),
    [items],
  )

  // Clones al inicio/final para lograr un loop infinito y fluido
  const slides = useMemo(() => (n > 1 ? [items[n - 1], ...items, items[0]] : items), [items, n])

  const realIndex = n > 0 ? ((index - 1) + n) % n : 0

  // Reactiva la transición justo después de un salto instantáneo del loop
  useEffect(() => {
    if (!transicion) {
      const id = requestAnimationFrame(() => setTransicion(true))
      return () => cancelAnimationFrame(id)
    }
  }, [transicion])

  function handleTransitionEnd() {
    if (n < 2) return
    if (index === slides.length - 1) {
      setTransicion(false)
      setIndex(1)
    } else if (index === 0) {
      setTransicion(false)
      setIndex(n)
    }
  }

  function irA(real: number) {
    setTransicion(true)
    setIndex(real + 1)
  }

  function siguiente() {
    setTransicion(true)
    setIndex(i => i + 1)
  }

  function anterior() {
    setTransicion(true)
    setIndex(i => i - 1)
  }

  function togglePlay(e: React.MouseEvent) {
    e.stopPropagation()
    setReproduciendo(p => !p)
  }

  function detener(e: React.MouseEvent) {
    e.stopPropagation()
    setReproduciendo(false)
    setTransicion(false)
    setIndex(1)
  }

  function irAlBanner(banner: BannerSlide) {
    const { interno, ruta } = resolverDestino(banner.enlace)
    if (interno) navigate(ruta)
    else window.location.href = ruta
  }

  if (!cargado || n === 0) return null

  return (
    <div className="mb-8">
      <div className="flex items-center justify-between mb-3 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 className="text-lg font-bold text-gray-800 flex items-center gap-2">
          <span className="text-primary-600">✦</span> Últimos productos agregados
        </h2>
        <Link to="/productos/recientes" className="text-sm font-medium text-primary-600 hover:text-primary-700 whitespace-nowrap">
          Ver todos →
        </Link>
      </div>
      <div className="relative w-full h-[440px] sm:h-[350px] overflow-hidden shadow-lg select-none" style={{ backgroundColor: '#000510' }}>
      <div
        className="flex h-full"
        style={{
          transform: `translateX(-${index * 100}%)`,
          transition: transicion ? 'transform 0.6s ease-in-out' : 'none',
        }}
        onTransitionEnd={handleTransitionEnd}
      >
        {slides.map((item, i) => {
          const esActiva = n < 2 ? true : i === index
          const duracion = duraciones[(i - 1 + n) % n] ?? 4000

          if (item.tipo === 'resumen') {
            return (
              <div
                key={`${item.key}-${i}`}
                className="relative w-full h-full flex-shrink-0 overflow-hidden"
              >
                <div className="w-full h-full flex sm:grid sm:grid-cols-5 overflow-x-auto snap-x snap-mandatory">
                  {item.productos.map((p, colIdx) => {
                    const imagenUrl = buildImageUrl(p.imagen)
                    const color = PALETA_SHOWCASE[colIdx % PALETA_SHOWCASE.length]
                    return (
                      <div
                        key={p.id}
                        role="button"
                        tabIndex={0}
                        onClick={() => navigate(`/productos/${p.id}`)}
                        onKeyDown={e => { if (e.key === 'Enter') navigate(`/productos/${p.id}`) }}
                        className="group relative flex-shrink-0 w-[75%] sm:w-auto h-full flex flex-col justify-end cursor-pointer snap-center px-3 pb-4 pt-6"
                        style={{ backgroundColor: color }}
                      >
                        <div className="absolute inset-0 opacity-0 group-hover:opacity-10 bg-white transition-opacity pointer-events-none" />

                        {/* "Plato" claro que contiene la imagen tal cual venga (sin estirar ni recortar) —
                            las fotos las suben los vendedores con tamaños y fondos dispares */}
                        <div className="relative w-full flex-1 min-h-0 mb-3 p-2">
                          <div className="w-full h-full rounded-2xl bg-white/90 shadow-lg flex items-center justify-center overflow-hidden p-3">
                            {imagenUrl ? (
                              <img
                                src={imagenUrl}
                                alt={p.nombre}
                                className="max-w-full max-h-full object-contain group-hover:scale-105 transition-transform duration-300"
                              />
                            ) : (
                              <span className="text-gray-300 text-xs">Sin imagen</span>
                            )}
                          </div>
                        </div>

                        <div className="relative w-full text-center">
                          <p className="text-white font-bold text-sm line-clamp-2 drop-shadow">{p.nombre}</p>
                          <p className="text-white/90 font-bold text-base mt-0.5">{formatMoneda(p.precio)}</p>
                        </div>

                        <span className="absolute bottom-3 right-3 text-white/80 text-lg leading-none">→</span>
                      </div>
                    )
                  })}
                </div>
              </div>
            )
          }

          const imagenUrl = buildImageUrl(item.banner.imagen)
          return (
            <div
              key={`${item.key}-${i}`}
              role="button"
              tabIndex={0}
              onClick={() => irAlBanner(item.banner)}
              onKeyDown={e => { if (e.key === 'Enter') irAlBanner(item.banner) }}
              className="relative w-full h-full flex-shrink-0 cursor-pointer overflow-hidden"
            >
              {imagenUrl ? (
                <img
                  src={imagenUrl}
                  alt={item.banner.texto}
                  className="absolute inset-0 w-full h-full object-cover"
                  style={{
                    transform: esActiva ? 'scale(1.06)' : 'scale(1)',
                    transitionProperty: 'transform',
                    transitionDuration: `${duracion}ms`,
                    transitionTimingFunction: 'linear',
                  }}
                />
              ) : (
                <div className="absolute inset-0 bg-gradient-to-br from-gray-700 to-gray-900" />
              )}

              <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/10 to-transparent" />

              <span className="absolute top-3 left-3 badge bg-amber-500 text-white text-[10px] font-semibold shadow">
                ★ Promocionado
              </span>

              <span className="absolute top-3 right-3 hidden sm:inline-flex items-center gap-1 text-xs font-medium bg-white/15 backdrop-blur px-3 py-1.5 rounded-full border border-white/30 whitespace-nowrap text-white">
                Ver más →
              </span>

              <div className="absolute bottom-3 left-4 right-4 text-white">
                <h3 className="font-bold text-lg line-clamp-2 drop-shadow">{item.banner.texto}</h3>
              </div>
            </div>
          )
        })}
      </div>

      {/* Barra de progreso del autoplay */}
      {n > 1 && (
        <div className="absolute top-0 left-0 right-0 h-1 bg-white/20 z-10">
          <div
            key={`progress-${index}`}
            className="h-full bg-white"
            style={{
              animationName: 'slider-progress',
              animationDuration: `${duraciones[realIndex] ?? 4000}ms`,
              animationTimingFunction: 'linear',
              animationFillMode: 'forwards',
              animationPlayState: reproduciendo ? 'running' : 'paused',
            }}
            onAnimationEnd={() => {
              if (reproduciendo) siguiente()
            }}
          />
        </div>
      )}

      {/* Flechas de navegación */}
      {n > 1 && (
        <>
          <button
            onClick={e => {
              e.stopPropagation()
              anterior()
            }}
            aria-label="Diapositiva anterior"
            className="absolute left-2 top-1/2 -translate-y-1/2 w-9 h-9 flex items-center justify-center rounded-full bg-black/40 hover:bg-black/60 text-white backdrop-blur-sm transition-colors text-xl leading-none z-10"
          >
            ‹
          </button>
          <button
            onClick={e => {
              e.stopPropagation()
              siguiente()
            }}
            aria-label="Diapositiva siguiente"
            className="absolute right-2 top-1/2 -translate-y-1/2 w-9 h-9 flex items-center justify-center rounded-full bg-black/40 hover:bg-black/60 text-white backdrop-blur-sm transition-colors text-xl leading-none z-10"
          >
            ›
          </button>
        </>
      )}

      {/* Indicadores + controles de reproducción */}
      <div className="absolute bottom-2 right-3 flex items-center gap-2 z-10">
        {n > 1 && (
          <div className="hidden md:flex items-center gap-1 mr-2">
            {items.map((item, i) => (
              <button
                key={item.key}
                onClick={e => {
                  e.stopPropagation()
                  irA(i)
                }}
                aria-label={`Ir a la diapositiva ${i + 1}`}
                className={`h-1.5 rounded-full transition-all ${
                  i === realIndex ? 'bg-white w-4' : 'bg-white/40 w-1.5'
                }`}
              />
            ))}
          </div>
        )}
        <button
          onClick={togglePlay}
          aria-label={reproduciendo ? 'Pausar slider' : 'Reanudar slider'}
          className="w-7 h-7 flex items-center justify-center rounded-full bg-black/40 hover:bg-black/60 text-white backdrop-blur-sm text-[10px]"
        >
          {reproduciendo ? '❙❙' : '▶'}
        </button>
        <button
          onClick={detener}
          aria-label="Detener slider"
          className="w-7 h-7 flex items-center justify-center rounded-full bg-black/40 hover:bg-black/60 text-white backdrop-blur-sm text-[10px]"
        >
          ■
        </button>
      </div>
      </div>
    </div>
  )
}
