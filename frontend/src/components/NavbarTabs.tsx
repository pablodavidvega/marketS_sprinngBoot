import { NavLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `nav-tab-link ${isActive ? 'active-tab text-primary-600 font-medium' : 'text-gray-600 hover:text-primary-600'}`

export default function NavbarTabs() {
  const { isAuthenticated, hasRole } = useAuth()

  return (
    <div className="hidden md:block border-t border-gray-100">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-wrap items-center gap-x-6 gap-y-2 py-2.5">
          <NavLink to="/" end className={linkClass}>
            Inicio
          </NavLink>

          {isAuthenticated && hasRole('VENDEDOR') && (
            <NavLink to="/vendedor" end className={linkClass}>
              Dashboard Vendedor
            </NavLink>
          )}

          {isAuthenticated && hasRole('CLIENTE') && (
            <NavLink to="/vender" className="nav-tab-link text-primary-600 font-medium hover:text-primary-700">
              Vender
            </NavLink>
          )}

          {isAuthenticated && (hasRole('CLIENTE') || hasRole('VENDEDOR')) && (
            <NavLink to="/cliente/dashboard" className={linkClass}>
              Mi cuenta Comprador
            </NavLink>
          )}

          <NavLink to="/soporte" className={linkClass}>
            Soporte
          </NavLink>

          {isAuthenticated && hasRole('MODERADOR') && !hasRole('ADMIN') && (
            <>
              <NavLink to="/moderador/dashboard" className={linkClass}>
                Panel mod
              </NavLink>
              <NavLink to="/moderador/productos" className={linkClass}>
                Productos
              </NavLink>
              <NavLink to="/moderador/pedidos" className={linkClass}>
                Pedidos
              </NavLink>
              <NavLink to="/moderador/reportes" className={linkClass}>
                Reportes
              </NavLink>
            </>
          )}

          {isAuthenticated && hasRole('ADMIN') && (
            <NavLink to="/admin/dashboard" end className={linkClass}>
              Panel admin
            </NavLink>
          )}
        </div>
      </div>
    </div>
  )
}
