import api from './axios'
import type { Invitacion, ConfigRegistro, EstadoRegistro, ValidacionInvitacion } from '../types'

// ── Públicos (sin auth) ───────────────────────────────────────────────────────

export const getEstadoRegistro = () =>
  api.get<EstadoRegistro>('/api/invitaciones/estado')

export const validarToken = (token: string) =>
  api.get<ValidacionInvitacion>('/api/invitaciones/validar', { params: { token } })

// ── Admin ─────────────────────────────────────────────────────────────────────

// El tipo de la respuesta paginada (contenido/paginaActual/totalPaginas/...) se declara
// localmente en InvitacionesPage.tsx, igual que en ModeradorProductosPage/ModeradorPedidosPage.
export const getInvitaciones = <T = unknown>(params: { estado?: string; page?: number; size?: number }) =>
  api.get<T>('/api/admin/invitaciones', { params })

export const crearInvitacion = (data: {
  descripcion?: string
  maxUsos?: number | null
  fechaExpiracion?: string | null
}) => api.post<Invitacion>('/api/admin/invitaciones', data)

export const revocarInvitacion = (id: number) =>
  api.patch(`/api/admin/invitaciones/${id}/revocar`)

export const reactivarInvitacion = (id: number) =>
  api.patch(`/api/admin/invitaciones/${id}/reactivar`)

export const eliminarInvitacion = (id: number) =>
  api.delete(`/api/admin/invitaciones/${id}`)

export const getConfigRegistro = () =>
  api.get<ConfigRegistro>('/api/admin/invitaciones/config')

export const setConfigRegistro = (data: ConfigRegistro) =>
  api.put('/api/admin/invitaciones/config', data)
