import api from './axios'

// Resumen diario "Novedades del día" (ver NovedadesDiariasService en el backend)

export const darseDeBajaNovedades = (token: string) =>
  api.post<{ mensaje: string }>('/api/novedades/baja', null, { params: { token } })

export const enviarNovedadesAhora = () =>
  api.post<{ productos: number; enviados: number; fallidos: number }>('/api/admin/novedades/enviar')
