import api from './axios'
import type { Producto, Categoria, SugerenciaProducto, VendedorProductosResponse, ResenasResponse, Resena, ResultadoCarga } from '../types'

export const getProductos = (q?: string, limit = 30) =>
  api.get<Producto[]>('/api/productos', { params: { q, limit } })

export const getSugerencias = (q: string) =>
  api.get<SugerenciaProducto[]>('/api/productos/sugerencias', { params: { q } })

export const getProducto = (id: number) =>
  api.get<Producto>(`/api/productos/${id}`)

export const getProductosPorCategoria = (categoriaId: number) =>
  api.get<Producto[]>(`/api/productos/categoria/${categoriaId}`)

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

export const cargarProductosMasivo = (
  archivo: File,
  zip?: File | null,
  onUploadProgress?: (pct: number) => void,
) => {
  const formData = new FormData()
  formData.append('archivo', archivo)
  if (zip) formData.append('zip', zip)
  return api.post<ResultadoCarga>('/api/vendedor/carga-masiva', formData, {
    onUploadProgress: onUploadProgress
      ? e => onUploadProgress(Math.round((e.loaded * 100) / (e.total ?? 1)))
      : undefined,
  })
}

// Reseñas
export const getResenas = (productoId: number) =>
  api.get<ResenasResponse>(`/api/productos/${productoId}/resenas`)

export const crearResena = (productoId: number, data: { calificacion: number; comentario?: string }) =>
  api.post<Resena>(`/api/productos/${productoId}/resenas`, data)
