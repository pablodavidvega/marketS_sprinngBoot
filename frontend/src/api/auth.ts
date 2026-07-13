import api from './axios'
import type { AuthResponse, GoogleLoginResponse, PerfilUsuario } from '../types'

export const login = (email: string, password: string) =>
  api.post<AuthResponse>('/api/auth/login', { email, password })

export const googleLogin = (idToken: string) =>
  api.post<GoogleLoginResponse>('/api/auth/google', { idToken })

export const googleCompletarRegistro = (data: {
  pendingToken: string
  username: string
  tipoUsuario: string
  phone: string
  address: string
  city?: string
  invitacion?: string
  aceptaTerminos: boolean
}) => api.post<AuthResponse>('/api/auth/google/completar', data)

export const logout = () =>
  api.post('/api/auth/logout')

export const registro = (data: {
  username: string
  email: string
  password: string
  confirmPassword: string
  phone: string
  address: string
  tipoUsuario: string
  aceptaTerminos: boolean
  city?: string
  invitacion?: string
}) => api.post<PerfilUsuario>('/api/auth/registro', data)

export const forgotPassword = (email: string) =>
  api.post<{ mensaje: string }>('/api/auth/forgot-password', { email })

export const resetPassword = (token: string, password: string) =>
  api.post<{ mensaje: string }>('/api/auth/reset-password', { token, password })
