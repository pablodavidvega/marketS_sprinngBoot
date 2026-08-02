import api from './axios'
import type { VendedorProductosResponse } from '../types'

export const getMisFavoritos = (params?: { page?: number; size?: number }) =>
  api.get<VendedorProductosResponse>('/api/favoritos', { params })

export const getMisFavoritosIds = () =>
  api.get<number[]>('/api/favoritos/ids')

export const agregarFavorito = (productoId: number) =>
  api.post(`/api/favoritos/${productoId}`)

export const quitarFavorito = (productoId: number) =>
  api.delete(`/api/favoritos/${productoId}`)
