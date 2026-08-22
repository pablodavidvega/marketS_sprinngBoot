import { useState, useEffect, useCallback } from 'react'
import { extraerMensajeError } from '../utils/errores'

// Encapsula el patrón repetido en los listados paginados del panel admin/vendedor/moderador:
// cargar(pagina) + estado de loading/error + recarga al cambiar de página o de filtros.
// `fetcher` debe venir memoizado (useCallback) por la página cuando depende de filtros propios
// (búsqueda, estado, etc.) — su identidad es lo que dispara la recarga, junto con `pagina`.
export function usePaginatedResource<R>(
  fetcher: (pagina: number) => Promise<{ data: R }>,
  mensajeError = 'Error al cargar los datos',
) {
  const [data, setData] = useState<R | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [pagina, setPagina] = useState(0)

  const cargar = useCallback(() => {
    setLoading(true)
    setError('')
    fetcher(pagina)
      .then(res => setData(res.data))
      .catch(err => setError(extraerMensajeError(err, mensajeError)))
      .finally(() => setLoading(false))
  }, [fetcher, pagina, mensajeError])

  useEffect(() => { cargar() }, [cargar])

  return { data, setData, loading, error, setError, pagina, setPagina, recargar: cargar }
}
