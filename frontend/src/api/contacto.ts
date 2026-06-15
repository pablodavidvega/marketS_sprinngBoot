import api from './axios'
import type { ContactoResponse, AdminContactoData } from '../types'

export interface EnviarContactoParams {
  nombre: string
  email: string
  tipoSolicitud: string
  asunto: string
  mensaje: string
}

export const enviarContacto = (params: EnviarContactoParams) => {
  const form = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => form.append(k, v))
  return api.post<ContactoResponse>('/api/contacto', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  })
}

export const getMisSolicitudes = () =>
  api.get<ContactoResponse[]>('/api/contacto/mis-solicitudes')

export const getAdminSolicitudes = () =>
  api.get<AdminContactoData>('/api/contacto/admin')

export const responderSolicitud = (id: number, respuesta: string) => {
  const form = new URLSearchParams({ respuesta })
  return api.patch<ContactoResponse>(`/api/contacto/admin/${id}/responder`, form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  })
}

export const cambiarEstadoSolicitud = (id: number, estado: string) => {
  const form = new URLSearchParams({ estado })
  return api.patch<ContactoResponse>(`/api/contacto/admin/${id}/estado`, form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  })
}
