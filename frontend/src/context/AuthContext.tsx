import { createContext, useContext, useState, type ReactNode } from 'react'

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
    setToken(newToken)
    setUser({ email, role })
  }

  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setToken(null)
    setUser(null)
  }

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
