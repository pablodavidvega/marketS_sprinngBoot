import api from './axios'
import type { NotificacionesResponse } from '../types'

export const getNotificaciones  = () => api.get<NotificacionesResponse>('/api/notificaciones')
export const marcarLeida        = (id: number) => api.patch(`/api/notificaciones/${id}/leer`)
export const marcarTodasLeidas  = () => api.patch('/api/notificaciones/leer-todas')
