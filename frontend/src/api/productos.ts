import api from './axios'
import type { Producto, Categoria, SugerenciaProducto, VendedorProductosResponse } from '../types'

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

export const crearProducto = (formData: FormData) =>
  api.post<Producto>('/api/vendedor/productos', formData)

export const actualizarProducto = (id: number, formData: FormData) =>
  api.put<Producto>(`/api/vendedor/productos/${id}`, formData)
