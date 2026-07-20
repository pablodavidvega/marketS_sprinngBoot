import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import type { ReactNode } from 'react'

interface Props {
  children: ReactNode
  roles?: string[]
}

export default function ProtectedRoute({ children, roles }: Props) {
  const { isAuthenticated, hasRole } = useAuth()
  const location = useLocation()

  // Guarda a dónde iba (path + query, ej. deep-link de un correo de notificación) para que
  // LoginPage pueda mandarlo ahí de vuelta apenas inicie sesión, en vez del home por rol.
  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  }
  if (roles && !hasRole(...roles)) return <Navigate to="/" replace />

  return <>{children}</>
}
