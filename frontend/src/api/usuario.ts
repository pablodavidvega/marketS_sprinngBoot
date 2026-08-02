import api from './axios'
import type { AuthResponse, PerfilUsuario, DashboardCliente, DashboardAdmin, DashboardVendedor, VendedorEstadisticas, ResultadoCarga, ReporteCatalogo, EmbudoAdmin, EstadisticasComunidad } from '../types'

export const getMiPerfil = () =>
  api.get<PerfilUsuario>('/api/me')

export const convertirseEnVendedor = (city: string) =>
  api.post<AuthResponse>('/api/cliente/convertir-vendedor', { city })

export const actualizarPerfil = (data: {
  username: string
  email: string
  phone: string
  address: string
}) => api.put<PerfilUsuario>('/api/me', data)

export const cambiarPassword = (data: {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}) => api.put<{ mensaje: string }>('/api/me/password', data)

export const getDashboardCliente = () =>
  api.get<DashboardCliente>('/api/cliente/dashboard')

export const getDashboardAdmin = () =>
  api.get<DashboardAdmin>('/api/admin/dashboard')

export const getEmbudoAdmin = () =>
  api.get<EmbudoAdmin>('/api/admin/estadisticas/embudo')

export const getEstadisticasComunidad = () =>
  api.get<EstadisticasComunidad>('/api/admin/estadisticas/comunidad')

export const getDashboardVendedor = () =>
  api.get<DashboardVendedor>('/api/vendedor/dashboard')

export const getUsuarios = (tipo?: string, valor?: string, page = 0, size = 30) =>
  api.get<{
    contenido: PerfilUsuario[]
    paginaActual: number
    totalPaginas: number
    totalElementos: number
  }>('/api/admin/usuarios', { params: { tipo, valor, page, size } })

export const cambiarRolUsuario = (id: number, rolId: number) =>
  api.put<PerfilUsuario>(`/api/admin/usuarios/${id}/rol`, { rolId })

export const eliminarUsuario = (id: number) =>
  api.delete(`/api/admin/usuarios/${id}`)

export const getRoles = () =>
  api.get<{ id: number; nombre: string }[]>('/api/admin/roles')

export const getReporteVendedores = () =>
  api.get<VendedorEstadisticas[]>('/api/admin/reportes/vendedores')

export const getReporteCatalogo = () =>
  api.get<ReporteCatalogo>('/api/admin/reportes/catalogo')

export const descargarExcelCatalogo = () =>
  api.get('/api/admin/reportes/catalogo/excel', { responseType: 'blob' })

export const descargarPdfUsuarios = (tipo?: string, valor?: string) =>
  api.get('/api/admin/reportes/usuarios', {
    params: { tipo, valor },
    responseType: 'blob',
  })

export const cargarUsuariosMasivo = (
  archivo: File,
  onUploadProgress?: (pct: number) => void,
) => {
  const formData = new FormData()
  formData.append('archivo', archivo)
  return api.post<ResultadoCarga>('/api/admin/carga-usuarios', formData, {
    onUploadProgress: onUploadProgress
      ? e => onUploadProgress(Math.round((e.loaded * 100) / (e.total ?? 1)))
      : undefined,
  })
}
