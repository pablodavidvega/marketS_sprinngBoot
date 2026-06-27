import api from './axios'
import type { AuthResponse, PerfilUsuario } from '../types'

export const login = (email: string, password: string) =>
  api.post<AuthResponse>('/api/auth/login', { email, password })

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
  city?: string
  invitacion?: string
}) => api.post<PerfilUsuario>('/api/auth/registro', data)

export const forgotPassword = (email: string) =>
  api.post<{ mensaje: string }>('/api/auth/forgot-password', { email })

export const resetPassword = (token: string, password: string) =>
  api.post<{ mensaje: string }>('/api/auth/reset-password', { token, password })
