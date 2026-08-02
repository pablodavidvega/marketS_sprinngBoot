import api from './axios'
import type { PedidoResponse, PedidoVendedorResponse } from '../types'

export interface CrearPedidoItem {
  productoId: number
  cantidad: number
}

// MERCADOPAGO (default) o un acuerdo externo: EFECTIVO, TRANSFERENCIA, NEQUI, OTRO
export type MetodoPago = 'MERCADOPAGO' | 'EFECTIVO' | 'TRANSFERENCIA' | 'NEQUI' | 'OTRO'

// Envoltorio de paginación de /api/pedidos/mis-pedidos y /api/vendedor/pedidos — mismo formato
// que el resto de listados paginados del backend (ver ModeradorApiController.pedidos).
export interface MisPedidosResponse {
  contenido: PedidoResponse[]
  paginaActual: number
  totalPaginas: number
  totalElementos: number
}

export interface PedidosVendedorResponse {
  contenido: PedidoVendedorResponse[]
  paginaActual: number
  totalPaginas: number
  totalElementos: number
}

export const crearPedido = (items: CrearPedidoItem[], metodoPago?: MetodoPago) =>
  api.post<PedidoResponse>('/api/pedidos', { items, metodoPago })

export const getPedido = (id: number) =>
  api.get<PedidoResponse>(`/api/pedidos/${id}`)

export const getMisPedidos = (page = 0, size = 30) =>
  api.get<MisPedidosResponse>('/api/pedidos/mis-pedidos', { params: { page, size } })

export const simularPago = (id: number) =>
  api.post<{ mensaje: string; estado: string }>(`/api/pedidos/${id}/simular-pago`)

export const getPedidosVendedor = (page = 0, size = 30) =>
  api.get<PedidosVendedorResponse>('/api/vendedor/pedidos', { params: { page, size } })

export const confirmarRecepcion = (id: number) =>
  api.patch<{ mensaje: string; estado: string }>(`/api/pedidos/${id}/confirmar-recepcion`)

export const marcarCompletado = (id: number) =>
  api.patch<{ mensaje: string; estado: string }>(`/api/pedidos/${id}/marcar-completado`)

export const marcarRechazado = (id: number) =>
  api.patch<{ mensaje: string; estado: string }>(`/api/pedidos/${id}/marcar-rechazado`)

export const calificarComprador = (id: number, data: { calificacion: number; comentario?: string }) =>
  api.patch<{ mensaje: string }>(`/api/pedidos/${id}/calificar-comprador`, data)
