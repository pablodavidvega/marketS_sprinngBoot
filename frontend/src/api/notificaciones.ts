import api from './axios'
import type { NotificacionesResponse, NotificacionesTodasResponse } from '../types'

export const getNotificaciones  = () => api.get<NotificacionesResponse>('/api/notificaciones')
export const marcarLeida        = (id: number) => api.patch(`/api/notificaciones/${id}/leer`)
export const marcarTodasLeidas  = () => api.patch('/api/notificaciones/leer-todas')

export const getNotificacionesTodas = (page: number, soloNoLeidas: boolean) =>
  api.get<NotificacionesTodasResponse>('/api/notificaciones/todas', { params: { page, size: 20, soloNoLeidas } })
