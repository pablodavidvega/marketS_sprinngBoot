import { Link } from 'react-router-dom'
import { useAppConfig } from '../context/AppConfigContext'

export default function Footer() {
  const { nombreApp, logoUrl, descripcion } = useAppConfig()
  const anio = new Date().getFullYear()

  return (
    <footer className="mt-12 text-gray-300" style={{ backgroundColor: '#000510' }}>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 grid grid-cols-1 sm:grid-cols-3 gap-8">
        <div>
          {logoUrl
            ? <img src={logoUrl} alt={nombreApp} className="h-8 object-contain mb-2" />
            : <span className="text-xl font-bold text-primary-500">{nombreApp}</span>
          }
          <p className="text-sm text-gray-400 mt-2">{descripcion}</p>
        </div>

        <div>
          <h3 className="text-sm font-semibold text-white mb-3">Enlaces</h3>
          <ul className="space-y-2 text-sm">
            <li><Link to="/" className="text-gray-400 hover:text-primary-500">Inicio</Link></li>
            <li><Link to="/productos/recientes" className="text-gray-400 hover:text-primary-500">Últimos productos</Link></li>
            <li><Link to="/planes" className="text-gray-400 hover:text-primary-500">Planes</Link></li>
            <li><Link to="/soporte" className="text-gray-400 hover:text-primary-500">Soporte</Link></li>
          </ul>
        </div>

        <div>
          <h3 className="text-sm font-semibold text-white mb-3">Cuenta</h3>
          <ul className="space-y-2 text-sm">
            <li><Link to="/login" className="text-gray-400 hover:text-primary-500">Ingresar</Link></li>
            <li><Link to="/registro" className="text-gray-400 hover:text-primary-500">Registrarse</Link></li>
          </ul>
        </div>
      </div>

      <div className="border-t border-white/10 py-4">
        <p className="text-center text-xs text-gray-500">
          © {anio} {nombreApp}. Todos los derechos reservados.
        </p>
      </div>
    </footer>
  )
}
