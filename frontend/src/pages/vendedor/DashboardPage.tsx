import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { getDashboardVendedor } from '../../api/usuario'
import ReputacionBar from '../../components/ReputacionBar'
import { VENDEDOR_LINKS } from '../../config/vendedorLinks'
import type { DashboardVendedor } from '../../types'

function StatCard({
  icon,
  label,
  value,
  color = 'text-gray-800',
  borderColor = '',
}: {
  icon: string
  label: string
  value: string
  color?: string
  borderColor?: string
}) {
  return (
    <div className={`card flex items-center gap-4 ${borderColor ? `border-l-4 ${borderColor}` : ''}`}>
      <div className="text-3xl">{icon}</div>
      <div>
        <p className="text-sm text-gray-500">{label}</p>
        <p className={`text-xl font-bold ${color}`}>{value}</p>
      </div>
    </div>
  )
}

function fmt(n: number) {
  return '$' + n.toLocaleString('es-CO', { minimumFractionDigits: 0, maximumFractionDigits: 0 })
}

export default function VendedorDashboardPage() {
  const { user } = useAuth()
  const [stats, setStats] = useState<DashboardVendedor | null>(null)
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    getDashboardVendedor()
      .then(({ data }) => setStats(data))
      .finally(() => setCargando(false))
  }, [])

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-800 mb-1">Panel de vendedor</h1>
      <p className="text-gray-500 mb-8">Bienvenido, {user?.email}</p>

      {/* Stats */}
      {cargando ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="card animate-pulse h-20 bg-gray-100" />
          ))}
        </div>
      ) : stats && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-4">
            <StatCard
              icon="🛒"
              label="Unidades vendidas"
              value={stats.totalVentas.toString()}
            />
            <StatCard
              icon="📦"
              label="Productos publicados"
              value={stats.totalProductos.toString()}
            />
            <StatCard
              icon="💰"
              label="Ganancias totales"
              value={fmt(stats.gananciaTotal)}
              color="text-green-600"
              borderColor="border-green-500"
            />
          </div>
          <div className="card mb-8">
            <ReputacionBar reputacion={stats.reputacion} label="Tu reputación como vendedor" />
          </div>
        </>
      )}

      {/* Acciones */}
      <h2 className="text-lg font-semibold text-gray-700 mb-4">Acciones</h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
        {VENDEDOR_LINKS.map(({ to, label, icon, description, iconClass }) => (
          <Link key={to} to={to} className="card hover:shadow-md transition-shadow group">
            <div className="flex items-center gap-4">
              <div className={`w-12 h-12 rounded-lg flex items-center justify-center text-xl transition-colors ${iconClass}`}>
                {icon}
              </div>
              <div>
                <h2 className="font-semibold text-gray-800">{label}</h2>
                <p className="text-sm text-gray-500">{description}</p>
              </div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  )
}
