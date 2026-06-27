import api from './axios'
import type { BannerSlide } from '../types'

// Público — usado por el slider del home
export const getBannersActivos = () =>
  api.get<BannerSlide[]>('/api/banners-slider/activos')

// Admin — gestión del CMS de banners
export const getBannersAdmin = () =>
  api.get<BannerSlide[]>('/api/admin/banners-slider')

export const crearBanner = (formData: FormData) =>
  api.post<BannerSlide>('/api/admin/banners-slider', formData)

export const actualizarBanner = (id: number, formData: FormData) =>
  api.put<BannerSlide>(`/api/admin/banners-slider/${id}`, formData)

export const activarBanner = (id: number) =>
  api.patch<BannerSlide>(`/api/admin/banners-slider/${id}/activar`)

export const desactivarBanner = (id: number) =>
  api.patch<BannerSlide>(`/api/admin/banners-slider/${id}/desactivar`)

export const eliminarBanner = (id: number) =>
  api.delete(`/api/admin/banners-slider/${id}`)
