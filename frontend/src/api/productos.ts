import api from './axios'
import type { Producto, Categoria, SugerenciaProducto, VendedorProductosResponse, ResenasResponse, Resena } from '../types'

export const getProductos = (q?: string, page = 0, size = 30, signal?: AbortSignal) =>
  api.get<VendedorProductosResponse>('/api/productos', { params: { q, page, size }, signal })

export const getProductosRecientes = (limit = 10) =>
  api.get<Producto[]>('/api/productos/recientes', { params: { limit } })

// limit=0 activa el modo paginado real en el backend (usado por la vista "Ver todos los
// últimos productos" con su botón "Ver más").
export const getProductosRecientesPaginado = (page = 0, size = 30) =>
  api.get<VendedorProductosResponse>('/api/productos/recientes', { params: { limit: 0, page, size } })

export const getSugerencias = (q: string) =>
  api.get<SugerenciaProducto[]>('/api/productos/sugerencias', { params: { q } })

export const getProducto = (id: number, signal?: AbortSignal) =>
  api.get<Producto>(`/api/productos/${id}`, { signal })

export const getProductosPorCategoria = (categoriaId: number, page = 0, size = 30, signal?: AbortSignal) =>
  api.get<VendedorProductosResponse>(`/api/productos/categoria/${categoriaId}`, { params: { page, size }, signal })

export const getCategorias = () =>
  api.get<Categoria[]>('/api/categorias')

export const getMarcas = () =>
  api.get<{ id: number; nombre: string }[]>('/api/marcas')

// Vendedor
export const getVendedorProductos = (params?: { keyword?: string; page?: number; size?: number }) =>
  api.get<VendedorProductosResponse>('/api/vendedor/productos', { params })

export const activarProducto = (id: number) =>
  api.patch<Producto>(`/api/vendedor/productos/${id}/activar`)

export const desactivarProducto = (id: number) =>
  api.patch<Producto>(`/api/vendedor/productos/${id}/desactivar`)

export const crearProducto = (
  formData: FormData,
  onUploadProgress?: (pct: number) => void,
) =>
  api.post<Producto>('/api/vendedor/productos', formData, {
    onUploadProgress: onUploadProgress
      ? e => onUploadProgress(Math.round((e.loaded * 100) / (e.total ?? 1)))
      : undefined,
  })

export const actualizarProducto = (
  id: number,
  formData: FormData,
  onUploadProgress?: (pct: number) => void,
) =>
  api.put<Producto>(`/api/vendedor/productos/${id}`, formData, {
    onUploadProgress: onUploadProgress
      ? e => onUploadProgress(Math.round((e.loaded * 100) / (e.total ?? 1)))
      : undefined,
  })

// Reseñas
export const getResenas = (productoId: number, page = 0, size = 30, signal?: AbortSignal) =>
  api.get<ResenasResponse>(`/api/productos/${productoId}/resenas`, { params: { page, size }, signal })

export const crearResena = (productoId: number, data: { calificacion: number; comentario?: string }) =>
  api.post<Resena>(`/api/productos/${productoId}/resenas`, data)
