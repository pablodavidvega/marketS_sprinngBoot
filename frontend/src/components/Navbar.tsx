import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useCarrito } from '../context/CarritoContext'
import { useAppConfig } from '../context/AppConfigContext'
import NotificacionesBell from './NotificacionesBell'
import NavbarTabs from './NavbarTabs'
import AdminSubNav from './AdminSubNav'
import VendedorSubNav from './VendedorSubNav'
import { VENDEDOR_LINKS } from '../config/vendedorLinks'

export default function Navbar() {
  const { isAuthenticated, hasRole, logout, user } = useAuth()
  const { cantidadTotal } = useCarrito()
  const { nombreApp, logoUrl } = useAppConfig()
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
          <Link to="/" className="flex items-center gap-2">
            {logoUrl
              ? <img src={logoUrl} alt={nombreApp} className="h-8 object-contain" />
              : <span className="text-2xl font-bold text-primary-600">{nombreApp}</span>
            }
          </Link>

          {/* Right side */}
          <div className="flex items-center gap-3">
            {/* Campanita */}
            {isAuthenticated && (
              <NotificacionesBell />
            )}

            {/* Favoritos */}
            {isAuthenticated && (
              <Link to="/favoritos" className="p-2 text-gray-600 hover:text-primary-600">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                </svg>
              </Link>
            )}

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
                {hasRole('CLIENTE') && (
                  <Link to="/vender" className="btn-primary text-sm py-1.5">
                    Vender
                  </Link>
                )}
                <span className="text-sm text-gray-500 whitespace-nowrap">{user?.email}</span>
                <Link to="/mi-perfil" className="text-sm text-gray-500 hover:text-primary-600 py-1.5">
                  Mi perfil
                </Link>
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
      </div>

      {/* Desktop tabs row */}
      <NavbarTabs />
      <VendedorSubNav />
      <AdminSubNav />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Mobile menu */}
        {open && (
          <div className="md:hidden border-t border-gray-100 py-3 flex flex-col gap-3">
            <Link to="/" onClick={() => setOpen(false)} className="text-gray-700 py-1">Inicio</Link>
            {isAuthenticated && hasRole('VENDEDOR') && (
              <Link to="/vendedor" onClick={() => setOpen(false)} className="text-gray-700 py-1">Dashboard Vendedor</Link>
            )}
            {isAuthenticated && hasRole('CLIENTE') && (
              <Link to="/vender" onClick={() => setOpen(false)} className="text-primary-600 font-medium py-1">Vender</Link>
            )}
            {isAuthenticated && (hasRole('CLIENTE') || hasRole('VENDEDOR')) && (
              <Link to="/cliente/dashboard" onClick={() => setOpen(false)} className="text-gray-700 py-1">Mi cuenta Comprador</Link>
            )}
            <Link to="/soporte" onClick={() => setOpen(false)} className="text-gray-700 py-1">Soporte</Link>
            {isAuthenticated && (
              <Link to="/mi-perfil" onClick={() => setOpen(false)} className="text-gray-700 py-1">Mi perfil</Link>
            )}
            {isAuthenticated && (
              <Link to="/favoritos" onClick={() => setOpen(false)} className="text-gray-700 py-1">Mis favoritos</Link>
            )}
            {isAuthenticated && hasRole('VENDEDOR') && VENDEDOR_LINKS.map(({ to, label }) => (
              <Link key={to} to={to} onClick={() => setOpen(false)} className="text-gray-700 py-1">{label}</Link>
            ))}
            {isAuthenticated && hasRole('MODERADOR') && !hasRole('ADMIN') && (
              <>
                <Link to="/moderador/dashboard" onClick={() => setOpen(false)} className="text-gray-700 py-1">Panel mod</Link>
                <Link to="/moderador/productos" onClick={() => setOpen(false)} className="text-gray-700 py-1">Productos</Link>
                <Link to="/moderador/pedidos"   onClick={() => setOpen(false)} className="text-gray-700 py-1">Pedidos</Link>
                <Link to="/moderador/reportes"  onClick={() => setOpen(false)} className="text-gray-700 py-1">Reportes</Link>
              </>
            )}
            {isAuthenticated && hasRole('ADMIN') && (
              <>
                <Link to="/admin/dashboard" onClick={() => setOpen(false)} className="text-gray-700 py-1">Panel admin</Link>
                <Link to="/admin/estadisticas-comunidad" onClick={() => setOpen(false)} className="text-gray-700 py-1">Estadísticas</Link>
                <Link to="/admin/usuarios" onClick={() => setOpen(false)} className="text-gray-700 py-1">Usuarios</Link>
                <Link to="/admin/categorias" onClick={() => setOpen(false)} className="text-gray-700 py-1">Categorías</Link>
                <Link to="/admin/correos" onClick={() => setOpen(false)} className="text-gray-700 py-1">Enviar correos</Link>
                <Link to="/admin/soporte" onClick={() => setOpen(false)} className="text-gray-700 py-1">Solicitudes de soporte</Link>
                <Link to="/admin/reportes/vendedores" onClick={() => setOpen(false)} className="text-gray-700 py-1">Reportes de vendedores</Link>
                <Link to="/admin/reportes/catalogo" onClick={() => setOpen(false)} className="text-gray-700 py-1">Reporte del catálogo</Link>
                <Link to="/admin/carga-usuarios" onClick={() => setOpen(false)} className="text-gray-700 py-1">Carga masiva</Link>
                <Link to="/admin/invitaciones" onClick={() => setOpen(false)} className="text-gray-700 py-1">Invitaciones</Link>
                <Link to="/admin/banners-slider" onClick={() => setOpen(false)} className="text-gray-700 py-1">Banners slider</Link>
                <Link to="/admin/configuracion" onClick={() => setOpen(false)} className="text-gray-700 py-1">Configuración</Link>
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
