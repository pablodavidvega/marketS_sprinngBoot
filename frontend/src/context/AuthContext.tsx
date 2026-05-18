import { createContext, useContext, useState, useEffect, type ReactNode } from 'react'

interface AuthUser {
  email: string
  role: string
}

interface AuthContextType {
  user: AuthUser | null
  token: string | null
  login: (token: string, email: string, role: string) => void
  logout: () => void
  isAuthenticated: boolean
  hasRole: (...roles: string[]) => boolean
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('token'))
  const [user, setUser] = useState<AuthUser | null>(() => {
    const stored = localStorage.getItem('user')
    return stored ? JSON.parse(stored) : null
  })

  const login = (newToken: string, email: string, role: string) => {
    localStorage.setItem('token', newToken)
    localStorage.setItem('user', JSON.stringify({ email, role }))
    localStorage.setItem('lastActive', Date.now().toString())
    setToken(newToken)
    setUser({ email, role })
  }

  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    localStorage.removeItem('lastActive')
    setToken(null)
    setUser(null)
  }

  // Controlar inactividad global (15 minutos)
  useEffect(() => {
    if (!token) return

    const TIMEOUT_DURATION = 15 * 60 * 1000 // 15 minutos en milisegundos

    // 1. Verificar inactividad al montar el componente
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

    // 2. Escuchar la actividad del usuario para actualizar el timestamp (con throttle de 5s para rendimiento)
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

    // 3. Revisar periódicamente si el tiempo ha expirado (cada 10 segundos)
    const checkInactivity = () => {
      const stored = localStorage.getItem('lastActive')
      if (stored) {
        const elapsed = Date.now() - Number(stored)
        if (elapsed > TIMEOUT_DURATION) {
          logout()
        }
      }
    }
    const intervalId = setInterval(checkInactivity, 10000)

    // Limpieza al desmontar o desautenticarse
    return () => {
      clearInterval(intervalId)
      events.forEach((event) => window.removeEventListener(event, updateActivity))
    }
  }, [token])

  const hasRole = (...roles: string[]) =>
    user ? roles.includes(user.role) : false

  return (
    <AuthContext.Provider value={{ user, token, login, logout, isAuthenticated: !!token, hasRole }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}

