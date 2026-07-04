import { NavLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const linkClass = ({ isActive }: { isActive: boolean }) =>
  isActive ? 'text-primary-600 font-medium' : 'text-gray-600 hover:text-primary-600'

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
              Dashboard
            </NavLink>
          )}

          <NavLink to="/soporte" className={linkClass}>
            Soporte
          </NavLink>

          {isAuthenticated && hasRole('CLIENTE') && (
            <NavLink to="/cliente/dashboard" className={linkClass}>
              Mi cuenta
            </NavLink>
          )}

          {isAuthenticated && hasRole('VENDEDOR') && (
            <>
              <NavLink to="/vendedor/productos" className={linkClass}>
                Mis productos
              </NavLink>
              <NavLink to="/vendedor/carga-masiva" className={linkClass}>
                Carga masiva de productos
              </NavLink>
            </>
          )}

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
            <>
              <NavLink to="/admin/dashboard" className={linkClass}>
                Panel admin
              </NavLink>
              <NavLink to="/admin/usuarios" className={linkClass}>
                Usuarios
              </NavLink>
              <NavLink to="/admin/categorias" className={linkClass}>
                Categorías
              </NavLink>
              <NavLink to="/admin/carga-usuarios" className={linkClass}>
                Carga masiva
              </NavLink>
              <NavLink to="/admin/invitaciones" className={linkClass}>
                Invitaciones
              </NavLink>
              <NavLink to="/admin/configuracion" className={linkClass}>
                Configuración
              </NavLink>
              <NavLink to="/admin/banners-slider" className={linkClass}>
                Banners slider
              </NavLink>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
