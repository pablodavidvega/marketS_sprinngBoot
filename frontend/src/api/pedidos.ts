import api from './axios'
import type { PedidoResponse } from '../types'

export interface CrearPedidoItem {
  productoId: number
  cantidad: number
}

export const crearPedido = (items: CrearPedidoItem[]) =>
  api.post<PedidoResponse>('/api/pedidos', { items })

export const getPedido = (id: number) =>
  api.get<PedidoResponse>(`/api/pedidos/${id}`)

export const getMisPedidos = () =>
  api.get<PedidoResponse[]>('/api/pedidos/mis-pedidos')

export const simularPago = (id: number) =>
  api.post<{ mensaje: string; estado: string }>(`/api/pedidos/${id}/simular-pago`)
