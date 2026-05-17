import { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useCarrito } from '../context/CarritoContext'

export default function Navbar() {
  const { isAuthenticated, hasRole, logout, user } = useAuth()
  const { cantidadTotal } = useCarrito()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <nav className="bg-white shadow-sm border-b border-gray-200 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link to="/" className="text-2xl font-bold text-primary-600">
            MarketS
          </Link>

          {/* Desktop links */}
          <div className="hidden md:flex items-center gap-6">
            <NavLink to="/" end className={({ isActive }) => isActive ? 'text-primary-600 font-medium' : 'text-gray-600 hover:text-primary-600'}>
              Inicio
            </NavLink>

            {isAuthenticated && hasRole('CLIENTE') && (
              <NavLink to="/cliente/dashboard" className={({ isActive }) => isActive ? 'text-primary-600 font-medium' : 'text-gray-600 hover:text-primary-600'}>
                Mi cuenta
              </NavLink>
            )}

            {isAuthenticated && hasRole('VENDEDOR') && (
              <NavLink to="/vendedor/productos" className={({ isActive }) => isActive ? 'text-primary-600 font-medium' : 'text-gray-600 hover:text-primary-600'}>
                Mis productos
              </NavLink>
            )}

            {isAuthenticated && hasRole('ADMIN') && (
              <>
                <NavLink to="/admin/dashboard" className={({ isActive }) => isActive ? 'text-primary-600 font-medium' : 'text-gray-600 hover:text-primary-600'}>
                  Panel admin
                </NavLink>
                <NavLink to="/admin/usuarios" className={({ isActive }) => isActive ? 'text-primary-600 font-medium' : 'text-gray-600 hover:text-primary-600'}>
                  Usuarios
                </NavLink>
              </>
            )}
          </div>

          {/* Right side */}
          <div className="flex items-center gap-3">
            {/* Cart */}
            <Link to="/carrito" className="relative p-2 text-gray-600 hover:text-primary-600">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13l-1.5 6h13M7 13L5.4 5M17 19a2 2 0 100 4 2 2 0 000-4zm-8 0a2 2 0 100 4 2 2 0 000-4z" />
              </svg>
              {cantidadTotal > 0 && (
                <span className="absolute -top-1 -right-1 bg-primary-600 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">
                  {cantidadTotal > 99 ? '99+' : cantidadTotal}
                </span>
              )}
            </Link>

            {isAuthenticated ? (
              <div className="hidden md:flex items-center gap-2">
                <span className="text-sm text-gray-500 max-w-[120px] truncate">{user?.email}</span>
                <button onClick={handleLogout} className="btn-secondary text-sm py-1.5">
                  Salir
                </button>
              </div>
            ) : (
              <div className="hidden md:flex items-center gap-2">
                <Link to="/login" className="btn-secondary text-sm py-1.5">Ingresar</Link>
                <Link to="/registro" className="btn-primary text-sm py-1.5">Registrarse</Link>
              </div>
            )}

            {/* Mobile hamburger */}
            <button
              onClick={() => setOpen(!open)}
              className="md:hidden p-2 text-gray-600"
            >
              <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                {open
                  ? <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  : <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                }
              </svg>
            </button>
          </div>
        </div>

        {/* Mobile menu */}
        {open && (
          <div className="md:hidden border-t border-gray-100 py-3 flex flex-col gap-3">
            <Link to="/" onClick={() => setOpen(false)} className="text-gray-700 py-1">Inicio</Link>
            {isAuthenticated && hasRole('CLIENTE') && (
              <Link to="/cliente/dashboard" onClick={() => setOpen(false)} className="text-gray-700 py-1">Mi cuenta</Link>
            )}
            {isAuthenticated && hasRole('VENDEDOR') && (
              <Link to="/vendedor/productos" onClick={() => setOpen(false)} className="text-gray-700 py-1">Mis productos</Link>
            )}
            {isAuthenticated && hasRole('ADMIN') && (
              <>
                <Link to="/admin/dashboard" onClick={() => setOpen(false)} className="text-gray-700 py-1">Panel admin</Link>
                <Link to="/admin/usuarios" onClick={() => setOpen(false)} className="text-gray-700 py-1">Usuarios</Link>
              </>
            )}
            {isAuthenticated ? (
              <button onClick={handleLogout} className="btn-danger text-sm text-left">Cerrar sesión</button>
            ) : (
              <>
                <Link to="/login" onClick={() => setOpen(false)} className="btn-secondary text-sm">Ingresar</Link>
                <Link to="/registro" onClick={() => setOpen(false)} className="btn-primary text-sm">Registrarse</Link>
              </>
            )}
          </div>
        )}
      </div>
    </nav>
  )
}
