import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getPerfilVendedor, getProductosVendedor } from '../api/vendedores'
import ProductoCard from '../components/ProductoCard'
import ReputacionBar from '../components/ReputacionBar'
import { iconoRedSocial } from '../utils/redSocial'
import type { PerfilVendedor, Producto } from '../types'

function AvatarLetra({ username }: { username: string }) {
  return (
    <div className="w-20 h-20 rounded-full bg-primary-100 flex items-center justify-center flex-shrink-0">
      <span className="text-3xl font-bold text-primary-600">
        {username.charAt(0).toUpperCase()}
      </span>
    </div>
  )
}

function formatFecha(s: string) {
  return new Date(s).toLocaleDateString('es-CO', { month: 'long', year: 'numeric' })
}

const PRODUCTOS_POR_PAGINA = 30

export default function VendedorPerfilPage() {
  const { id } = useParams<{ id: string }>()
  const vendedorId = Number(id)

  const [perfil,    setPerfil]    = useState<PerfilVendedor | null>(null)
  const [productos, setProductos] = useState<Producto[]>([])
  const [pagina,       setPagina]       = useState(0)
  const [totalPaginas, setTotalPaginas] = useState(0)
  const [cargandoMas,  setCargandoMas]  = useState(false)
  const [loading,   setLoading]   = useState(true)
  const [noEncontrado, setNoEncontrado] = useState(false)
  const [busqueda,  setBusqueda]  = useState('')

  useEffect(() => {
    if (!vendedorId) return
    setLoading(true)
    Promise.all([
      getPerfilVendedor(vendedorId),
      getProductosVendedor(vendedorId, 0, PRODUCTOS_POR_PAGINA),
    ])
      .then(([perfilRes, prodRes]) => {
        setPerfil(perfilRes.data)
        setProductos(prodRes.data.contenido)
        setPagina(prodRes.data.paginaActual)
        setTotalPaginas(prodRes.data.totalPaginas)
      })
      .catch(() => setNoEncontrado(true))
      .finally(() => setLoading(false))
  }, [vendedorId])

  function handleVerMas() {
    if (cargandoMas) return
    setCargandoMas(true)
    getProductosVendedor(vendedorId, pagina + 1, PRODUCTOS_POR_PAGINA)
      .then(r => {
        setProductos(prev => [...prev, ...r.data.contenido])
        setPagina(r.data.paginaActual)
        setTotalPaginas(r.data.totalPaginas)
      })
      .finally(() => setCargandoMas(false))
  }

  const productosFiltrados = busqueda.trim()
    ? productos.filter(p =>
        p.nombre.toLowerCase().includes(busqueda.toLowerCase()) ||
        p.categoriaNombre?.toLowerCase().includes(busqueda.toLowerCase())
      )
    : productos

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
      </div>
    )
  }

  if (noEncontrado || !perfil) {
    return (
      <div className="max-w-lg mx-auto px-4 py-24 text-center">
        <p className="text-5xl mb-4">🔍</p>
        <h2 className="text-xl font-bold text-gray-800 mb-2">Vendedor no encontrado</h2>
        <p className="text-gray-500 text-sm mb-6">
          El perfil que buscas no existe o ya no está disponible.
        </p>
        <Link to="/" className="btn-primary text-sm">Volver al inicio</Link>
      </div>
    )
  }

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

      {/* ── Header del vendedor ── */}
      <div className="card mb-8">
        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-5">
          <AvatarLetra username={perfil.username} />

          <div className="flex-1">
            <h1 className="text-2xl font-bold text-gray-900">@{perfil.username}</h1>

            <div className="flex flex-wrap items-center gap-x-4 gap-y-1 mt-1.5 text-sm text-gray-500">
              {perfil.city && (
                <span className="flex items-center gap-1">
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                      d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                      d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  {perfil.city}
                </span>
              )}
              <span className="flex items-center gap-1">
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                    d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
                Miembro desde {formatFecha(perfil.miembroDesde)}
              </span>
            </div>

            {perfil.enlaces.length > 0 && (
              <div className="flex flex-wrap gap-2 mt-3">
                {perfil.enlaces.map((en, i) => (
                  <a
                    key={i}
                    href={en.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center gap-1.5 text-xs font-medium bg-gray-100 hover:bg-gray-200 text-gray-700 px-3 py-1.5 rounded-full transition-colors"
                  >
                    <span>{iconoRedSocial(en.etiqueta)}</span> {en.etiqueta}
                  </a>
                ))}
              </div>
            )}
          </div>

          {/* Stats: productos, ventas, unidades vendidas */}
          <div className="flex gap-3 flex-wrap">
            <div className="text-center px-6 py-3 bg-primary-50 rounded-xl border border-primary-100">
              <p className="text-2xl font-bold text-primary-600">{perfil.totalProductos}</p>
              <p className="text-xs text-gray-500 mt-0.5">
                {perfil.totalProductos === 1 ? 'Producto' : 'Productos'}
              </p>
            </div>
            <div className="text-center px-6 py-3 bg-primary-50 rounded-xl border border-primary-100">
              <p className="text-2xl font-bold text-primary-600">{perfil.totalVentas}</p>
              <p className="text-xs text-gray-500 mt-0.5">
                {perfil.totalVentas === 1 ? 'Venta' : 'Ventas'}
              </p>
            </div>
            <div className="text-center px-6 py-3 bg-primary-50 rounded-xl border border-primary-100">
              <p className="text-2xl font-bold text-primary-600">{perfil.unidadesVendidas}</p>
              <p className="text-xs text-gray-500 mt-0.5">
                {perfil.unidadesVendidas === 1 ? 'Unidad vendida' : 'Unidades vendidas'}
              </p>
            </div>
          </div>
        </div>

        <div className="mt-5 pt-5 border-t border-gray-100 max-w-sm">
          <ReputacionBar reputacion={perfil.reputacion} label="Reputación como vendedor" />
        </div>
      </div>

      {/* ── Productos ── */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-5">
        <h2 className="text-lg font-semibold text-gray-800">
          Productos de @{perfil.username}
        </h2>

        {productos.length > 6 && (
          <input
            className="input max-w-xs text-sm"
            placeholder="Buscar en esta tienda..."
            value={busqueda}
            onChange={e => setBusqueda(e.target.value)}
          />
        )}
      </div>

      {productosFiltrados.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          {busqueda
            ? <p>No hay productos que coincidan con <strong>"{busqueda}"</strong></p>
            : <p>Este vendedor aún no tiene productos publicados.</p>
          }
        </div>
      ) : (
        <>
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
            {productosFiltrados.map(p => (
              <ProductoCard key={p.id} producto={p} />
            ))}
          </div>
          {!busqueda.trim() && pagina < totalPaginas - 1 && (
            <div className="flex justify-center mt-8">
              <button
                onClick={handleVerMas}
                disabled={cargandoMas}
                className="btn-secondary px-6"
              >
                {cargandoMas ? 'Cargando...' : 'Ver más'}
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
