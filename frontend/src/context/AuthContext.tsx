import { createContext, useContext, useState, useEffect, type ReactNode } from 'react'
import { logout as logoutApi, refresh as refreshApi } from '../api/auth'

interface AuthUser {
  email: string
  role: string
  // Usuarios creados por carga masiva (ver CargaMasivaUsuariosService) reciben una contraseña
  // elegida por el admin, no por ellos — se les obliga a cambiarla antes de usar el resto de
  // la app (ver CambioPasswordObligatorio).
  debeCambiarPassword: boolean
}

interface AuthContextType {
  user: AuthUser | null
  login: (email: string, role: string, token: string, debeCambiarPassword?: boolean) => void
  logout: () => void
  isAuthenticated: boolean
  hasRole: (...roles: string[]) => boolean
  marcarPasswordCambiada: () => void
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const stored = localStorage.getItem('user')
    return stored ? JSON.parse(stored) : null
  })

  const login = (email: string, role: string, token: string, debeCambiarPassword = false) => {
    // El JWT también viaja en una cookie httpOnly, pero al ser cross-site (frontend en
    // Vercel, backend en Render) Safari/iOS la descarta por ITP. Por eso el transporte real
    // es este token guardado en localStorage y mandado como Authorization: Bearer.
    const nuevoUser = { email, role, debeCambiarPassword }
    localStorage.setItem('user', JSON.stringify(nuevoUser))
    localStorage.setItem('authToken', token)
    localStorage.setItem('lastActive', Date.now().toString())
    setUser(nuevoUser)
  }

  // Se llama al completar el cambio obligatorio de contraseña (ver CambioPasswordObligatorio)
  // para que el modal deje de mostrarse sin tener que volver a loguearse.
  const marcarPasswordCambiada = () => {
    setUser(prev => {
      if (!prev) return prev
      const actualizado = { ...prev, debeCambiarPassword: false }
      localStorage.setItem('user', JSON.stringify(actualizado))
      return actualizado
    })
  }

  const logout = async () => {
    try {
      await logoutApi() // pide al backend que limpie la cookie httpOnly
    } catch {
      // Si el backend no responde, igual limpiamos el estado local
    }
    localStorage.removeItem('user')
    localStorage.removeItem('authToken')
    localStorage.removeItem('lastActive')
    setUser(null)
  }

  // Controlar inactividad global (30 minutos) con sesión deslizante: mientras el usuario
  // esté activo, el JWT (jwt.expiration en el backend) se renueva antes de vencer. Antes el
  // token vencía a los 15 min desde el login sin importar la actividad — esto es lo que
  // cerraba la sesión de golpe aunque el usuario siguiera usando la app (ej. subiendo un video).
  useEffect(() => {
    if (!user) return

    const TIMEOUT_DURATION = 30 * 60 * 1000
    // Debe ser bien menor al jwt.expiration del backend (30 min) para que el token
    // siempre se renueve con margen, incluso si el intervalo se retrasa en background.
    const REFRESH_INTERVAL = 10 * 60 * 1000

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

    let lastRefreshed = Date.now()
    const renewSession = async () => {
      const currentTime = Date.now()
      if (currentTime - lastRefreshed < REFRESH_INTERVAL) return
      lastRefreshed = currentTime
      try {
        const { data } = await refreshApi()
        localStorage.setItem('authToken', data.token)
      } catch {
        // Si el token ya no es válido, el interceptor de axios se encarga del logout
      }
    }

    const checkInactivity = () => {
      const stored = localStorage.getItem('lastActive')
      if (stored && Date.now() - Number(stored) > TIMEOUT_DURATION) {
        logout()
      } else {
        renewSession()
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
    <AuthContext.Provider value={{ user, login, logout, isAuthenticated: !!user, hasRole, marcarPasswordCambiada }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}
