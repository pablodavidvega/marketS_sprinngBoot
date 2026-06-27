import api from './axios'
import type { PerfilVendedor, Producto } from '../types'

export const getPerfilVendedor = (id: number) =>
  api.get<PerfilVendedor>(`/api/vendedores/${id}/perfil`)

export const getProductosVendedor = (id: number) =>
  api.get<Producto[]>(`/api/vendedores/${id}/productos`)
