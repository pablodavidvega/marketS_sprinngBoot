import api from './axios'
import type { PerfilUsuario, DashboardCliente, DashboardAdmin, DashboardVendedor } from '../types'

export const getMiPerfil = () =>
  api.get<PerfilUsuario>('/api/me')

export const actualizarPerfil = (data: {
  username: string
  email: string
  phone: string
  address: string
}) => api.put<PerfilUsuario>('/api/me', data)

export const cambiarPassword = (data: {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}) => api.put<{ mensaje: string }>('/api/me/password', data)

export const getDashboardCliente = () =>
  api.get<DashboardCliente>('/api/cliente/dashboard')

export const getDashboardAdmin = () =>
  api.get<DashboardAdmin>('/api/admin/dashboard')

export const getDashboardVendedor = () =>
  api.get<DashboardVendedor>('/api/vendedor/dashboard')

export const getUsuarios = (tipo?: string, valor?: string) =>
  api.get<PerfilUsuario[]>('/api/admin/usuarios', { params: { tipo, valor } })

export const cambiarRolUsuario = (id: number, rolId: number) =>
  api.put<PerfilUsuario>(`/api/admin/usuarios/${id}/rol`, { rolId })

export const eliminarUsuario = (id: number) =>
  api.delete(`/api/admin/usuarios/${id}`)

export const getRoles = () =>
  api.get<{ id: number; nombre: string }[]>('/api/admin/roles')
