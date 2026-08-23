import { NavLink, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useVendedorHover } from '../context/VendedorHoverContext'
import { VENDEDOR_LINKS } from '../config/vendedorLinks'

export default function VendedorSubNav() {
  const { isAuthenticated, hasRole } = useAuth()
  const { hoverActivo, setHoverActivo } = useVendedorHover()
  const location = useLocation()
  // Esta fila es del perfil de vendedor: si el usuario está viendo "Mi cuenta Comprador"
  // (cualquier ruta /cliente/*), estos tabs no le pertenecen a esa sección — se atenúan en vez
  // de mostrarse con el mismo peso visual (ver NavbarTabs, que atenúa la fila de arriba igual).
  const modoComprador = location.pathname.startsWith('/cliente')

  if (!isAuthenticated || !hasRole('VENDEDOR')) return null

  return (
    <div className="hidden md:block bg-primary-50 border-t border-primary-100">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-wrap items-center gap-x-5 gap-y-1.5 py-2 text-sm">
          {VENDEDOR_LINKS.map(({ to, label }) => (
            <NavLink
              key={to}
              to={to}
              onMouseEnter={() => setHoverActivo(to)}
              onMouseLeave={() => setHoverActivo(prev => prev === to ? null : prev)}
              className={({ isActive }) =>
                `nav-tab-link ${hoverActivo === to ? 'hover-forzado' : ''} ${
                  isActive
                    ? 'active-tab text-primary-700 font-semibold'
                    : modoComprador
                      ? 'text-primary-300 hover:text-primary-400'
                      : 'text-primary-900/70 hover:text-primary-700'
                }`
              }
            >
              {label}
            </NavLink>
          ))}
        </div>
      </div>
    </div>
  )
}
