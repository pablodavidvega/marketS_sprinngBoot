import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../../api/axios'

interface Stats {
  totalProductos: number
  productosActivos: number
  totalPedidos: number
  totalVendedores: number
  totalUsuarios: number
}

function StatCard({ label, valor, sub, color }: { label: string; valor: number; sub?: string; color: string }) {
  return (
    <div className="card py-5">
      <p className={`text-3xl font-bold ${color}`}>{valor.toLocaleString('es-CO')}</p>
      <p className="text-sm font-medium text-gray-700 mt-1">{label}</p>
      {sub && <p className="text-xs text-gray-400 mt-0.5">{sub}</p>}
    </div>
  )
}

export default function ModeradorDashboardPage() {
  const [stats,   setStats]   = useState<Stats | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get<Stats>('/api/moderador/dashboard')
      .then(res => setStats(res.data))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
    </div>
  )

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Panel de moderación</h1>
        <p className="text-gray-500 text-sm mt-1">
          Tienes acceso de lectura y moderación de contenido. No puedes gestionar usuarios ni configurar la plataforma.
        </p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
        <StatCard label="Productos totales"  valor={stats?.totalProductos   ?? 0} color="text-gray-800" />
        <StatCard label="Activos"            valor={stats?.productosActivos  ?? 0} color="text-green-600"
          sub={`${stats ? Math.round((stats.productosActivos / Math.max(stats.totalProductos, 1)) * 100) : 0}% del total`} />
        <StatCard label="Pedidos"            valor={stats?.totalPedidos      ?? 0} color="text-primary-600" />
        <StatCard label="Vendedores"         valor={stats?.totalVendedores   ?? 0} color="text-purple-600" />
        <StatCard label="Usuarios"           valor={stats?.totalUsuarios     ?? 0} color="text-gray-600" />
      </div>

      {/* Accesos rápidos */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Link to="/moderador/productos" className="card hover:shadow-md transition-shadow group">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-blue-50 rounded-lg flex items-center justify-center">
              <svg className="w-5 h-5 text-primary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
              </svg>
            </div>
            <h2 className="font-semibold text-gray-800 group-hover:text-primary-600 transition-colors">Productos</h2>
          </div>
          <p className="text-sm text-gray-500">Ver y activar/desactivar productos de todos los vendedores.</p>
        </Link>

        <Link to="/moderador/pedidos" className="card hover:shadow-md transition-shadow group">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-green-50 rounded-lg flex items-center justify-center">
              <svg className="w-5 h-5 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
              </svg>
            </div>
            <h2 className="font-semibold text-gray-800 group-hover:text-primary-600 transition-colors">Pedidos</h2>
          </div>
          <p className="text-sm text-gray-500">Ver todos los pedidos de la plataforma (solo lectura).</p>
        </Link>

        <Link to="/moderador/reportes" className="card hover:shadow-md transition-shadow group">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-purple-50 rounded-lg flex items-center justify-center">
              <svg className="w-5 h-5 text-purple-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
              </svg>
            </div>
            <h2 className="font-semibold text-gray-800 group-hover:text-primary-600 transition-colors">Reportes</h2>
          </div>
          <p className="text-sm text-gray-500">Estadísticas de ventas y rendimiento por vendedor.</p>
        </Link>
      </div>

      {/* Info de permisos */}
      <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
        <p className="font-medium mb-1">Tu rol: Moderador</p>
        <p className="text-amber-700">
          Puedes ver productos, pedidos y reportes. Para gestionar usuarios, invitaciones o configuración de la plataforma, contacta al administrador.
        </p>
      </div>
    </div>
  )
}
