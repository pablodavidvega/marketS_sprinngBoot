import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { getSugerencias } from '../api/productos'
import type { SugerenciaProducto } from '../types'
import { buildImageUrl } from '../utils/image'

interface Props {
  onSearch: (q: string) => void
  initialValue?: string
}

export default function BuscadorAvanzado({ onSearch, initialValue = '' }: Props) {
  const [query, setQuery] = useState(initialValue)
  const [sugerencias, setSugerencias] = useState<SugerenciaProducto[]>([])
  const [abierto, setAbierto] = useState(false)
  const [indice, setIndice] = useState(-1)
  const [cargando, setCargando] = useState(false)
  const navigate = useNavigate()
  const wrapperRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  // Debounce de 300ms: lanza sugerencias cuando el usuario deja de escribir
  useEffect(() => {
    if (query.length < 2) {
      setSugerencias([])
      setAbierto(false)
      return
    }
    const timer = setTimeout(() => {
      setCargando(true)
      getSugerencias(query)
        .then(r => {
          setSugerencias(r.data)
          setAbierto(r.data.length > 0)
          setIndice(-1)
        })
        .catch(() => setSugerencias([]))
        .finally(() => setCargando(false))
    }, 300)

    return () => clearTimeout(timer)
  }, [query])

  // Cerrar dropdown al hacer clic fuera
  useEffect(() => {
    function onClick(e: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) {
        setAbierto(false)
      }
    }
    document.addEventListener('mousedown', onClick)
    return () => document.removeEventListener('mousedown', onClick)
  }, [])

  function seleccionar(s: SugerenciaProducto) {
    setQuery(s.nombre)
    setAbierto(false)
    setSugerencias([])
    navigate(`/productos/${s.id}`)
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (!abierto || sugerencias.length === 0) return

    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setIndice(i => (i < sugerencias.length - 1 ? i + 1 : i))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setIndice(i => (i > 0 ? i - 1 : 0))
    } else if (e.key === 'Enter') {
      if (indice >= 0) {
        e.preventDefault()
        seleccionar(sugerencias[indice])
      } else {
        setAbierto(false)
      }
    } else if (e.key === 'Escape') {
      setAbierto(false)
    }
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setAbierto(false)
    onSearch(query.trim())
  }

  function limpiar() {
    setQuery('')
    setSugerencias([])
    setAbierto(false)
    onSearch('')
    inputRef.current?.focus()
  }

  function resaltar(texto: string) {
    const idx = texto.toLowerCase().indexOf(query.toLowerCase())
    if (idx === -1) return <span>{texto}</span>
    return (
      <>
        {texto.slice(0, idx)}
        <strong className="text-primary-700">{texto.slice(idx, idx + query.length)}</strong>
        {texto.slice(idx + query.length)}
      </>
    )
  }

  return (
    <div ref={wrapperRef} className="relative w-full max-w-xl">
      <form onSubmit={handleSubmit} className="flex gap-2">
        <div className="relative flex-1">
          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={e => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
            onFocus={() => query.length >= 2 && sugerencias.length > 0 && setAbierto(true)}
            placeholder="Buscar productos..."
            autoComplete="off"
            className="input w-full pr-8"
          />

          {/* Spinner o botón limpiar */}
          <div className="absolute right-2.5 top-1/2 -translate-y-1/2">
            {cargando ? (
              <div className="w-4 h-4 border-2 border-primary-400 border-t-transparent rounded-full animate-spin" />
            ) : query ? (
              <button
                type="button"
                onClick={limpiar}
                className="text-gray-400 hover:text-gray-600 text-lg leading-none"
                aria-label="Limpiar búsqueda"
              >
                ×
              </button>
            ) : (
              <svg className="w-4 h-4 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            )}
          </div>
        </div>

        <button type="submit" className="btn-primary px-5 whitespace-nowrap">
          Buscar
        </button>
      </form>

      {/* Dropdown de sugerencias */}
      {abierto && sugerencias.length > 0 && (
        <ul className="absolute top-full left-0 right-16 mt-1 bg-white border border-gray-200 rounded-xl shadow-xl z-50 overflow-hidden">
          {sugerencias.map((s, i) => {
            const imgSrc = buildImageUrl(s.imagen)

            return (
              <li key={s.id}>
                <button
                  type="button"
                  onMouseDown={() => seleccionar(s)}
                  onMouseEnter={() => setIndice(i)}
                  className={`w-full flex items-center gap-3 px-4 py-2.5 text-left transition-colors ${
                    i === indice ? 'bg-primary-50' : 'hover:bg-gray-50'
                  }`}
                >
                  {/* Miniatura */}
                  <div className="w-10 h-10 rounded-lg bg-gray-100 overflow-hidden flex-shrink-0">
                    {imgSrc ? (
                      <img src={imgSrc} alt="" className="w-full h-full object-cover" />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-gray-300 text-sm">📦</div>
                    )}
                  </div>

                  {/* Nombre + categoría */}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm text-gray-800 truncate">{resaltar(s.nombre)}</p>
                    {s.categoriaNombre && (
                      <p className="text-xs text-gray-400 truncate">{s.categoriaNombre}</p>
                    )}
                  </div>

                  {/* Precio */}
                  <span className="text-sm font-semibold text-primary-600 flex-shrink-0">
                    ${s.precio.toLocaleString('es-CO')}
                  </span>
                </button>
              </li>
            )
          })}

          {/* Footer: ver todos los resultados */}
          <li className="border-t border-gray-100">
            <button
              type="button"
              onMouseDown={handleSubmit as unknown as React.MouseEventHandler}
              className="w-full px-4 py-2 text-sm text-primary-600 hover:bg-primary-50 text-center font-medium"
            >
              Ver todos los resultados para "{query}"
            </button>
          </li>
        </ul>
      )}
    </div>
  )
}
