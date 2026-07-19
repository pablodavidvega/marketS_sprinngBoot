import { NavLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { VENDEDOR_LINKS } from '../config/vendedorLinks'

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `nav-tab-link ${isActive ? 'active-tab text-primary-700 font-semibold' : 'text-primary-900/70 hover:text-primary-700'}`

export default function VendedorSubNav() {
  const { isAuthenticated, hasRole } = useAuth()

  if (!isAuthenticated || !hasRole('VENDEDOR')) return null

  return (
    <div className="hidden md:block bg-primary-50 border-t border-primary-100">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-wrap items-center gap-x-5 gap-y-1.5 py-2 text-sm">
          {VENDEDOR_LINKS.map(({ to, label }) => (
            <NavLink key={to} to={to} className={linkClass}>
              {label}
            </NavLink>
          ))}
        </div>
      </div>
    </div>
  )
}
