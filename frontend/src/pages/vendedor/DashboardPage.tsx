import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

export default function VendedorDashboardPage() {
  const { user } = useAuth()

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-800 mb-2">Panel de vendedor</h1>
      <p className="text-gray-500 mb-8">Bienvenido, {user?.email}</p>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
        <Link to="/vendedor/productos" className="card hover:shadow-md transition-shadow group">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center text-primary-600 text-xl group-hover:bg-primary-200 transition-colors">
              📦
            </div>
            <div>
              <h2 className="font-semibold text-gray-800">Mis productos</h2>
              <p className="text-sm text-gray-500">Ver, crear y gestionar productos</p>
            </div>
          </div>
        </Link>
      </div>
    </div>
  )
}
