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
import AdminDashboardPage from './pages/admin/DashboardPage'
import UsuariosPage from './pages/admin/UsuariosPage'
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage'
import ResetPasswordPage from './pages/auth/ResetPasswordPage'
import EditarPerfilPage from './pages/perfil/EditarPerfilPage'
import NotFoundPage from './pages/NotFoundPage'

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

          <Route
            path="/perfil/editar"
            element={
              <ProtectedRoute roles={['CLIENTE', 'VENDEDOR', 'ADMIN']}>
                <EditarPerfilPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/cliente/dashboard"
            element={
              <ProtectedRoute roles={['CLIENTE']}>
                <ClienteDashboardPage />
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

          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
    </div>
  )
}
