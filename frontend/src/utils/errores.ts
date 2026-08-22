interface ErrorConRespuesta {
  response?: { data?: { mensaje?: string; message?: string; error?: string } }
}

// Los distintos endpoints del backend no son consistentes en qué campo usan para el mensaje
// de error (mensaje/message/error) — este helper prueba los tres en orden y cae a `fallback`.
export function extraerMensajeError(err: unknown, fallback: string): string {
  const data = (err as ErrorConRespuesta)?.response?.data
  return data?.mensaje || data?.message || data?.error || fallback
}
