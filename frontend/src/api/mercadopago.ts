import api from './axios'

export interface EstadoConexionMercadoPago {
  conectado: boolean
  mpEmail: string | null
}

export const getEstadoConexionMercadoPago = () =>
  api.get<EstadoConexionMercadoPago>('/api/vendedor/mercadopago/estado')

export const iniciarConexionMercadoPago = () =>
  api.post<{ authUrl: string }>('/api/vendedor/mercadopago/conectar')

export const desconectarMercadoPago = () =>
  api.post<void>('/api/vendedor/mercadopago/desconectar')
