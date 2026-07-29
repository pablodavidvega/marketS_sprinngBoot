import api from './axios'
import type { Encuentro } from '../types'

export const getEncuentro = (pedidoId: number, vendedorId: number) =>
  api.get<Partial<Encuentro>>(`/api/pedidos/${pedidoId}/encuentro/${vendedorId}`)

export const guardarEncuentro = (
  pedidoId: number,
  vendedorId: number,
  data: { lugar: string; fechaHora: string; nota?: string }
) => api.put<Encuentro>(`/api/pedidos/${pedidoId}/encuentro/${vendedorId}`, data)

export const cancelarEncuentro = (pedidoId: number, vendedorId: number) =>
  api.delete(`/api/pedidos/${pedidoId}/encuentro/${vendedorId}`)

export const confirmarEncuentro = (pedidoId: number, vendedorId: number) =>
  api.post<Encuentro>(`/api/pedidos/${pedidoId}/encuentro/${vendedorId}/confirmar`)
