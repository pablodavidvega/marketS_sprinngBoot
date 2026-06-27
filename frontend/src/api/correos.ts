import api from './axios'

export function enviarCorreoIndividual(para: string, asunto: string, mensaje: string) {
  return api.get<string>('/api/correos', { params: { para, asunto, mensaje } })
}

export function enviarCorreoVarios(data: {
  destinatarios?: string
  archivo?: File | null
  asunto: string
  mensaje: string
}) {
  const form = new FormData()
  if (data.destinatarios) form.append('destinatarios', data.destinatarios)
  if (data.archivo) form.append('archivo', data.archivo)
  form.append('asunto', data.asunto)
  form.append('mensaje', data.mensaje)
  return api.post<string>('/api/correos/varios', form)
}

export function enviarCorreoMasivo(archivo: File, asunto: string, mensaje: string) {
  const form = new FormData()
  form.append('archivo', archivo)
  form.append('asunto', asunto)
  form.append('mensaje', mensaje)
  return api.post<string>('/api/correos/masivo', form)
}
