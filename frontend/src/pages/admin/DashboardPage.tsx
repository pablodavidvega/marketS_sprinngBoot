import { useState, useEffect } from 'react'
import { getDashboardAdmin } from '../../api/usuario'
import type { DashboardAdmin } from '../../types'

export default function AdminDashboardPage() {
  const [data, setData] = useState<DashboardAdmin | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getDashboardAdmin()
      .then(r => setData(r.data))
      .finally(() => setLoading(false))
  }, [])

  if (loading) {
    return (
      <div className="flex justify-center py-16">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
      </div>
    )
  }

  if (!data) return <div className="text-center py-16 text-gray-500">Error al cargar datos</div>

  const stats = [
    { label: 'Total usuarios', value: data.totalUsuarios, color: 'text-primary-600' },
    { label: 'Clientes', value: data.totalClientes, color: 'text-blue-600' },
    { label: 'Vendedores', value: data.totalVendedores, color: 'text-green-600' },
    { label: 'Admins', value: data.totalAdmins, color: 'text-purple-600' },
  ]

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Panel de administración</h1>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {stats.map(s => (
          <div key={s.label} className="card text-center">
            <p className={`text-4xl font-bold ${s.color}`}>{s.value}</p>
            <p className="text-sm text-gray-500 mt-1">{s.label}</p>
          </div>
        ))}
      </div>
    </div>
  )
}
