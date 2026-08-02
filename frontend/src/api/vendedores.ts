import api from './axios'
import type { PerfilVendedor, VendedorProductosResponse } from '../types'

export const getPerfilVendedor = (id: number) =>
  api.get<PerfilVendedor>(`/api/vendedores/${id}/perfil`)

// Paginado real (botón "Ver más" en VendedorPerfilPage).
export const getProductosVendedor = (id: number, page = 0, size = 30) =>
  api.get<VendedorProductosResponse>(`/api/vendedores/${id}/productos`, { params: { page, size } })
