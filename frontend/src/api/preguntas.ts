import api from './axios'
import type { Pregunta, PreguntasResponse } from '../types'

export const getPreguntas = (productoId: number, page = 0, size = 30, signal?: AbortSignal) =>
  api.get<PreguntasResponse>(`/api/productos/${productoId}/preguntas`, { params: { page, size }, signal })

export const crearPregunta = (productoId: number, contenido: string) =>
  api.post<Pregunta>(`/api/productos/${productoId}/preguntas`, { contenido })

export const responderPregunta = (productoId: number, preguntaId: number, respuesta: string) =>
  api.patch<Pregunta>(`/api/productos/${productoId}/preguntas/${preguntaId}/responder`, { respuesta })
