import api from './axios'
import type { ChatMensaje } from '../types'

export interface ChatMensajesPagina {
  mensajes: ChatMensaje[]
  page: number
  hayMasAntiguos: boolean
}

// page=0 trae los 30 mensajes más recientes del hilo; páginas siguientes traen tandas
// de 30 mensajes más antiguos, para poder implementar "cargar mensajes anteriores" en
// negociaciones largas sin tener que traer todo el historial en cada refresco.
export const getChat = (pedidoId: number, vendedorId: number, page = 0) =>
  api.get<ChatMensajesPagina>(`/api/pedidos/${pedidoId}/chat/${vendedorId}`, { params: { page } })

export const enviarMensajeChat = (pedidoId: number, vendedorId: number, contenido: string) =>
  api.post<ChatMensaje>(`/api/pedidos/${pedidoId}/chat/${vendedorId}`, { contenido })
