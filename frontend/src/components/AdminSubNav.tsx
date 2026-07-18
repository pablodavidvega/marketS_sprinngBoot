import { NavLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const linkClass = ({ isActive }: { isActive: boolean }) =>
  isActive ? 'text-primary-700 font-semibold' : 'text-primary-900/70 hover:text-primary-700'

const ADMIN_LINKS: { to: string; label: string; end?: boolean }[] = [
  { to: '/admin/estadisticas-comunidad', label: 'Estadísticas' },
  { to: '/admin/usuarios', label: 'Usuarios' },
  { to: '/admin/categorias', label: 'Categorías' },
  { to: '/admin/correos', label: 'Enviar correos' },
  { to: '/admin/soporte', label: 'Soporte' },
  { to: '/admin/reportes/vendedores', label: 'Reportes de vendedores' },
  { to: '/admin/reportes/catalogo', label: 'Reporte del catálogo' },
  { to: '/admin/carga-usuarios', label: 'Carga masiva' },
  { to: '/admin/invitaciones', label: 'Invitaciones' },
  { to: '/admin/banners-slider', label: 'Banners slider' },
  { to: '/admin/configuracion', label: 'Configuración' },
]

export default function AdminSubNav() {
  const { isAuthenticated, hasRole } = useAuth()

  if (!isAuthenticated || !hasRole('ADMIN')) return null

  return (
    <div className="hidden md:block bg-primary-50 border-t border-primary-100">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-wrap items-center gap-x-5 gap-y-1.5 py-2 text-sm">
          {ADMIN_LINKS.map(({ to, label, end }) => (
            <NavLink key={to} to={to} end={end} className={linkClass}>
              {label}
            </NavLink>
          ))}
        </div>
      </div>
    </div>
  )
}
