import api from './axios'
import type { ContactoResponse } from '../types'

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

// Respuestas paginadas (30 en 30) — mismo patrón que ModeradorApiController.pedidos() /
// PedidosPage.tsx: la interfaz de la envoltura de paginación vive aquí (capa de API), no en
// el types/index.ts compartido.
export interface ContactoPaginado {
  contenido: ContactoResponse[]
  paginaActual: number
  totalPaginas: number
  totalElementos: number
}

export interface AdminContactoPaginado extends ContactoPaginado {
  pendientes: number
  enProceso: number
  resueltos: number
}

export const getMisSolicitudes = (page = 0, size = 30) =>
  api.get<ContactoPaginado>('/api/contacto/mis-solicitudes', { params: { page, size } })

export const getAdminSolicitudes = (estado?: string, page = 0, size = 30) =>
  api.get<AdminContactoPaginado>('/api/contacto/admin', {
    params: { estado: estado || undefined, page, size },
  })

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
