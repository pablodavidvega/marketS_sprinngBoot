import api from './axios'
import type { CategoriaAdmin } from '../types'

// Admin — gestión de categorías desde el panel
export const getCategoriasAdmin = () =>
  api.get<CategoriaAdmin[]>('/api/admin/categorias')

export const crearCategoria = (nombreCategoria: string) =>
  api.post<CategoriaAdmin>('/api/admin/categorias', { nombreCategoria })

export const actualizarCategoria = (id: number, nombreCategoria: string) =>
  api.put<CategoriaAdmin>(`/api/admin/categorias/${id}`, { nombreCategoria })

export const eliminarCategoria = (id: number) =>
  api.delete(`/api/admin/categorias/${id}`)
