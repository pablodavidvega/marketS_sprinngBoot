import api from './axios'
import type { Producto } from '../types'

export const getMisFavoritos = () =>
  api.get<Producto[]>('/api/favoritos')

export const getMisFavoritosIds = () =>
  api.get<number[]>('/api/favoritos/ids')

export const agregarFavorito = (productoId: number) =>
  api.post(`/api/favoritos/${productoId}`)

export const quitarFavorito = (productoId: number) =>
  api.delete(`/api/favoritos/${productoId}`)
