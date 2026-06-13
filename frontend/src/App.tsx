import { Routes, Route } from 'react-router-dom'
import Navbar from './components/Navbar'
import ProtectedRoute from './components/ProtectedRoute'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import RegistroPage from './pages/RegistroPage'
import DetallePage from './pages/DetallePage'
import CarritoPage from './pages/CarritoPage'
import ConfirmacionPage from './pages/pedidos/ConfirmacionPage'
import ClienteDashboardPage from './pages/cliente/DashboardPage'
import VendedorDashboardPage from './pages/vendedor/DashboardPage'
import VendedorProductosPage from './pages/vendedor/ProductosPage'
import VendedorPedidosPage from './pages/vendedor/PedidosPage'
import MisPedidosPage from './pages/cliente/MisPedidosPage'
import AdminDashboardPage from './pages/admin/DashboardPage'
import UsuariosPage from './pages/admin/UsuariosPage'
import SoporteAdminPage from './pages/admin/SoportePage'
import ServicioClientePage from './pages/ServicioClientePage'
import ForgotPasswordPage from './pages/ForgotPasswordPage'
import ResetPasswordPage from './pages/ResetPasswordPage'

export default function App() {
  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <main>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/registro" element={<RegistroPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/productos/:id" element={<DetallePage />} />
          <Route path="/carrito" element={<CarritoPage />} />
          <Route path="/pedidos/confirmacion" element={<ConfirmacionPage />} />
          <Route path="/soporte" element={<ServicioClientePage />} />

          <Route
            path="/cliente/dashboard"
            element={
              <ProtectedRoute roles={['CLIENTE']}>
                <ClienteDashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/cliente/mis-pedidos"
            element={
              <ProtectedRoute roles={['CLIENTE']}>
                <MisPedidosPage />
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
            path="/admin/dashboard"
            element={
              <ProtectedRoute roles={['ADMIN']}>
                <AdminDashboardPage />
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

          <Route path="*" element={<HomePage />} />
        </Routes>
      </main>
    </div>
  )
}
