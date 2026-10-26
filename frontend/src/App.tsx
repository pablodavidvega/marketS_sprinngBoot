import { lazy, Suspense } from 'react'
import { Routes, Route } from 'react-router-dom'
import Navbar from './components/Navbar'
import Footer from './components/Footer'
import ScrollToTop from './components/ScrollToTop'
import CambioPasswordObligatorio from './components/CambioPasswordObligatorio'
import { ToastProvider } from './components/Toast'
import { VendedorHoverProvider } from './context/VendedorHoverContext'
import ProtectedRoute from './components/ProtectedRoute'
// HomePage se importa eager (no lazy): es la ruta de entrada de cualquier visitante anónimo,
// así que tiene que renderizar con el bundle inicial sin un round-trip extra a buscar su chunk.
// El resto de las páginas se cargan bajo demanda (code-splitting por ruta) para no obligar a
// todo el mundo a bajar, por ejemplo, recharts/jspdf (reportes) o el flujo de carga masiva solo
// por visitar el catálogo público.
import HomePage from './pages/HomePage'

const LoginPage = lazy(() => import('./pages/LoginPage'))
const RegistroPage = lazy(() => import('./pages/RegistroPage'))
const CompletarRegistroGooglePage = lazy(() => import('./pages/CompletarRegistroGooglePage'))
const DetallePage = lazy(() => import('./pages/DetallePage'))
const CarritoPage = lazy(() => import('./pages/CarritoPage'))
const ConfirmacionPage = lazy(() => import('./pages/pedidos/ConfirmacionPage'))
const PagoPendientePage = lazy(() => import('./pages/pedidos/PagoPendientePage'))
const PagoFallidoPage = lazy(() => import('./pages/pedidos/PagoFallidoPage'))
const ClienteDashboardPage = lazy(() => import('./pages/cliente/DashboardPage'))
const VendedorDashboardPage = lazy(() => import('./pages/vendedor/DashboardPage'))
const VendedorProductosPage = lazy(() => import('./pages/vendedor/ProductosPage'))
const VendedorPedidosPage = lazy(() => import('./pages/vendedor/PedidosPage'))
const VendedorReportesPage = lazy(() => import('./pages/vendedor/ReportesPage'))
const CargaMasivaPage = lazy(() => import('./pages/vendedor/CargaMasivaPage'))
const VendedorMercadoPagoPage = lazy(() => import('./pages/vendedor/MercadoPagoPage'))
const EditarPerfilPage = lazy(() => import('./pages/EditarPerfilPage'))
const VenderPage = lazy(() => import('./pages/VenderPage'))
const AdminDashboardPage = lazy(() => import('./pages/admin/DashboardPage'))
const EstadisticasComunidadPage = lazy(() => import('./pages/admin/EstadisticasComunidadPage'))
const UsuariosPage = lazy(() => import('./pages/admin/UsuariosPage'))
const SoporteAdminPage = lazy(() => import('./pages/admin/SoportePage'))
const ReportesVendedoresPage = lazy(() => import('./pages/admin/ReportesVendedoresPage'))
const ReportesCatalogoPage = lazy(() => import('./pages/admin/ReportesCatalogoPage'))
const CargaMasivaUsuariosPage = lazy(() => import('./pages/admin/CargaMasivaUsuariosPage'))
const EnviarCorreosPage = lazy(() => import('./pages/admin/EnviarCorreosPage'))
const InvitacionesPage = lazy(() => import('./pages/admin/InvitacionesPage'))
const ConfiguracionAppPage = lazy(() => import('./pages/admin/ConfiguracionAppPage'))
const BannerSliderPage = lazy(() => import('./pages/admin/BannerSliderPage'))
const CategoriasPage = lazy(() => import('./pages/admin/CategoriasPage'))
const UltimosProductosPage = lazy(() => import('./pages/UltimosProductosPage'))
const ModeradorDashboardPage = lazy(() => import('./pages/moderador/DashboardPage'))
const ModeradorProductosPage = lazy(() => import('./pages/moderador/ProductosPage'))
const ModeradorPedidosPage = lazy(() => import('./pages/moderador/PedidosPage'))
const ServicioClientePage = lazy(() => import('./pages/ServicioClientePage'))
const TerminosPage = lazy(() => import('./pages/TerminosPage'))
const PlanesPage = lazy(() => import('./pages/PlanesPage'))
const MisFavoritosPage = lazy(() => import('./pages/MisFavoritosPage'))
const VendedorPerfilPage = lazy(() => import('./pages/VendedorPerfilPage'))
const ForgotPasswordPage = lazy(() => import('./pages/ForgotPasswordPage'))
const ResetPasswordPage = lazy(() => import('./pages/ResetPasswordPage'))
const NotificacionesPage = lazy(() => import('./pages/NotificacionesPage'))
const NovedadesBajaPage = lazy(() => import('./pages/NovedadesBajaPage'))

function CargandoPagina() {
  return (
    <div className="flex justify-center items-center py-24">
      <span className="h-8 w-8 animate-spin rounded-full border-2 border-gray-300 border-t-primary-600" />
    </div>
  )
}

export default function App() {
  return (
    <ToastProvider>
    <VendedorHoverProvider>
    <ScrollToTop />
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Navbar />
      <main className="flex-1">
        <Suspense fallback={<CargandoPagina />}>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/registro" element={<RegistroPage />} />
          <Route path="/completar-registro-google" element={<CompletarRegistroGooglePage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/productos/recientes" element={<UltimosProductosPage />} />
          <Route path="/productos/:id" element={<DetallePage />} />
          <Route path="/carrito" element={<CarritoPage />} />
          <Route path="/pedidos/confirmacion" element={<ConfirmacionPage />} />
          <Route path="/pagos/pendiente" element={<PagoPendientePage />} />
          <Route path="/pagos/fallo" element={<PagoFallidoPage />} />
          <Route path="/soporte" element={<ServicioClientePage />} />
          <Route path="/terminos" element={<TerminosPage />} />
          <Route path="/planes" element={<PlanesPage />} />
          <Route path="/novedades/baja" element={<NovedadesBajaPage />} />
          <Route
            path="/favoritos"
            element={
              <ProtectedRoute>
                <MisFavoritosPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/notificaciones"
            element={
              <ProtectedRoute>
                <NotificacionesPage />
              </ProtectedRoute>
            }
          />
          <Route path="/vendedores/:id" element={<VendedorPerfilPage />} />

          <Route
            path="/cliente/dashboard"
            element={
              <ProtectedRoute roles={['CLIENTE', 'VENDEDOR']}>
                <ClienteDashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/cliente/editar-perfil"
            element={
              <ProtectedRoute roles={['CLIENTE', 'VENDEDOR']}>
                <EditarPerfilPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/mi-perfil"
            element={
              <ProtectedRoute>
                <EditarPerfilPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/vender"
            element={
              <ProtectedRoute roles={['CLIENTE']}>
                <VenderPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/vendedor"
            element={
              <ProtectedRoute roles={['VENDEDOR', 'ADMIN']}>
                <VendedorDashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/vendedor/productos"
            element={
              <ProtectedRoute roles={['VENDEDOR', 'ADMIN']}>
                <VendedorProductosPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/vendedor/pedidos"
            element={
              <ProtectedRoute roles={['VENDEDOR', 'ADMIN']}>
                <VendedorPedidosPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/vendedor/reportes"
            element={
              <ProtectedRoute roles={['VENDEDOR', 'ADMIN']}>
                <VendedorReportesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/vendedor/carga-masiva"
            element={
              <ProtectedRoute roles={['VENDEDOR']}>
                <CargaMasivaPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/vendedor/pagos"
            element={
              <ProtectedRoute roles={['VENDEDOR']}>
                <VendedorMercadoPagoPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/admin/dashboard"
            element={
              <ProtectedRoute roles={['ADMIN']}>
                <AdminDashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/estadisticas-comunidad"
            element={
              <ProtectedRoute roles={['ADMIN']}>
                <EstadisticasComunidadPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/usuarios"
            element={
              <ProtectedRoute roles={['ADMIN']}>
                <UsuariosPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/soporte"
            element={
              <ProtectedRoute roles={['ADMIN']}>
                <SoporteAdminPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/reportes/vendedores"
            element={
              <ProtectedRoute roles={['ADMIN']}>
                <ReportesVendedoresPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/reportes/catalogo"
            element={
              <ProtectedRoute roles={['ADMIN']}>
                <ReportesCatalogoPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/carga-usuarios"
            element={
              <ProtectedRoute roles={['ADMIN']}>
                <CargaMasivaUsuariosPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/correos"
            element={
              <ProtectedRoute roles={['ADMIN']}>
                <EnviarCorreosPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/invitaciones"
            element={
              <ProtectedRoute roles={['ADMIN']}>
                <InvitacionesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/configuracion"
            element={
              <ProtectedRoute roles={['ADMIN']}>
                <ConfiguracionAppPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/banners-slider"
            element={
              <ProtectedRoute roles={['ADMIN']}>
                <BannerSliderPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/categorias"
            element={
              <ProtectedRoute roles={['ADMIN']}>
                <CategoriasPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/moderador/dashboard"
            element={
              <ProtectedRoute roles={['MODERADOR', 'ADMIN']}>
                <ModeradorDashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/moderador/productos"
            element={
              <ProtectedRoute roles={['MODERADOR', 'ADMIN']}>
                <ModeradorProductosPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/moderador/pedidos"
            element={
              <ProtectedRoute roles={['MODERADOR', 'ADMIN']}>
                <ModeradorPedidosPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/moderador/reportes"
            element={
              <ProtectedRoute roles={['MODERADOR', 'ADMIN']}>
                <ReportesVendedoresPage />
              </ProtectedRoute>
            }
          />

          <Route path="*" element={<HomePage />} />
        </Routes>
        </Suspense>
      </main>
      <Footer />
      <CambioPasswordObligatorio />
    </div>
    </VendedorHoverProvider>
    </ToastProvider>
  )
}
