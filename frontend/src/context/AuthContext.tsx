import { createContext, useContext, useState, useEffect, type ReactNode } from 'react'
import { logout as logoutApi } from '../api/auth'

interface AuthUser {
  email: string
  role: string
}

interface AuthContextType {
  user: AuthUser | null
  login: (email: string, role: string) => void
  logout: () => void
  isAuthenticated: boolean
  hasRole: (...roles: string[]) => boolean
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const stored = localStorage.getItem('user')
    return stored ? JSON.parse(stored) : null
  })

  const login = (email: string, role: string) => {
    // El JWT viaja en la cookie httpOnly que el backend ya configuró.
    // Aquí solo guardamos la info no sensible para mostrar en la UI.
    localStorage.setItem('user', JSON.stringify({ email, role }))
    localStorage.setItem('lastActive', Date.now().toString())
    setUser({ email, role })
  }

  const logout = async () => {
    try {
      await logoutApi() // pide al backend que limpie la cookie httpOnly
    } catch {
      // Si el backend no responde, igual limpiamos el estado local
    }
    localStorage.removeItem('user')
    localStorage.removeItem('lastActive')
    setUser(null)
  }

  // Controlar inactividad global (15 minutos)
  useEffect(() => {
    if (!user) return

    const TIMEOUT_DURATION = 15 * 60 * 1000

    const now = Date.now()
    const storedLastActive = localStorage.getItem('lastActive')
    if (storedLastActive) {
      if (now - Number(storedLastActive) > TIMEOUT_DURATION) {
        logout()
        return
      }
    } else {
      localStorage.setItem('lastActive', now.toString())
    }

    let lastUpdated = Date.now()
    const updateActivity = () => {
      const currentTime = Date.now()
      if (currentTime - lastUpdated > 5000) {
        lastUpdated = currentTime
        localStorage.setItem('lastActive', currentTime.toString())
      }
    }

    const events = ['mousemove', 'keydown', 'click', 'scroll', 'touchstart']
    events.forEach((event) => window.addEventListener(event, updateActivity))

    const checkInactivity = () => {
      const stored = localStorage.getItem('lastActive')
      if (stored && Date.now() - Number(stored) > TIMEOUT_DURATION) {
        logout()
      }
    }
    const intervalId = setInterval(checkInactivity, 10000)

    return () => {
      clearInterval(intervalId)
      events.forEach((event) => window.removeEventListener(event, updateActivity))
    }
  }, [user])

  const hasRole = (...roles: string[]) =>
    user ? roles.includes(user.role) : false

  return (
    <AuthContext.Provider value={{ user, login, logout, isAuthenticated: !!user, hasRole }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}
