import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
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

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        {stats.map(s => (
          <div key={s.label} className="card text-center">
            <p className={`text-4xl font-bold ${s.color}`}>{s.value}</p>
            <p className="text-sm text-gray-500 mt-1">{s.label}</p>
          </div>
        ))}
      </div>

      <h2 className="text-lg font-semibold text-gray-700 mb-4">Acciones</h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <Link to="/admin/usuarios" className="card hover:shadow-md transition-shadow group">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center text-primary-600 text-xl group-hover:bg-primary-200 transition-colors">
              👥
            </div>
            <div>
              <h2 className="font-semibold text-gray-800">Gestión de usuarios</h2>
              <p className="text-sm text-gray-500">Ver, activar y cambiar roles</p>
            </div>
          </div>
        </Link>
        <Link to="/admin/correos" className="card hover:shadow-md transition-shadow group">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-orange-100 rounded-lg flex items-center justify-center text-orange-600 text-xl group-hover:bg-orange-200 transition-colors">
              ✉️
            </div>
            <div>
              <h2 className="font-semibold text-gray-800">Enviar correos</h2>
              <p className="text-sm text-gray-500">Individual, varios destinatarios o masivo por CSV</p>
            </div>
          </div>
        </Link>
        <Link to="/admin/soporte" className="card hover:shadow-md transition-shadow group">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center text-blue-600 text-xl group-hover:bg-blue-200 transition-colors">
              💬
            </div>
            <div>
              <h2 className="font-semibold text-gray-800">Soporte al cliente</h2>
              <p className="text-sm text-gray-500">Ver y responder solicitudes</p>
            </div>
          </div>
        </Link>
        <Link to="/admin/reportes/vendedores" className="card hover:shadow-md transition-shadow group">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center text-green-600 text-xl group-hover:bg-green-200 transition-colors">
              📊
            </div>
            <div>
              <h2 className="font-semibold text-gray-800">Reportes de vendedores</h2>
              <p className="text-sm text-gray-500">Estadísticas, CSV y PDF con gráficas</p>
            </div>
          </div>
        </Link>
      </div>
    </div>
  )
}
