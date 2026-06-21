import api from './axios'
import type { ChatMensaje } from '../types'

export const getChat = (pedidoId: number, vendedorId: number) =>
  api.get<ChatMensaje[]>(`/api/pedidos/${pedidoId}/chat/${vendedorId}`)

export const enviarMensajeChat = (pedidoId: number, vendedorId: number, contenido: string) =>
  api.post<ChatMensaje>(`/api/pedidos/${pedidoId}/chat/${vendedorId}`, { contenido })
