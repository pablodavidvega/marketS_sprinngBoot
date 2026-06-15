import api from './axios'
import type { Invitacion, ConfigRegistro, EstadoRegistro, ValidacionInvitacion } from '../types'

// ── Públicos (sin auth) ───────────────────────────────────────────────────────

export const getEstadoRegistro = () =>
  api.get<EstadoRegistro>('/api/invitaciones/estado')

export const validarToken = (token: string) =>
  api.get<ValidacionInvitacion>('/api/invitaciones/validar', { params: { token } })

// ── Admin ─────────────────────────────────────────────────────────────────────

export const getInvitaciones = () =>
  api.get<Invitacion[]>('/api/admin/invitaciones')

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
