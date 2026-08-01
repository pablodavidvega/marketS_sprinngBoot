import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { getNotificacionesTodas, marcarLeida, marcarTodasLeidas } from '../api/notificaciones'
import { useAuth } from '../context/AuthContext'
import { destinoDeNotificacion, formatTiempoNotificacion } from '../utils/notificaciones'
import type { Notificacion, NotificacionesTodasResponse } from '../types'

export default function NotificacionesPage() {
  const { hasRole } = useAuth()
  const esVendedor = hasRole('VENDEDOR') || hasRole('ADMIN')
  const navigate = useNavigate()

  const [data, setData] = useState<NotificacionesTodasResponse | null>(null)
  const [pagina, setPagina] = useState(0)
  const [soloNoLeidas, setSoloNoLeidas] = useState(false)
  const [loading, setLoading] = useState(true)

  const cargar = useCallback(async () => {
    setLoading(true)
    try {
      const { data } = await getNotificacionesTodas(pagina, soloNoLeidas)
      setData(data)
    } finally {
      setLoading(false)
    }
  }, [pagina, soloNoLeidas])

  useEffect(() => { cargar() }, [cargar])

  async function handleClick(n: Notificacion) {
    if (!n.leida) {
      await marcarLeida(n.id)
      setData(d => d && { ...d, contenido: d.contenido.map(x => x.id === n.id ? { ...x, leida: true } : x) })
    }
    navigate(destinoDeNotificacion(n, esVendedor))
  }

  async function handleMarcarTodas() {
    await marcarTodasLeidas()
    setData(d => d && { ...d, contenido: d.contenido.map(x => ({ ...x, leida: true })) })
  }

  const hayNoLeidas = !!data?.contenido.some(n => !n.leida)

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center justify-between flex-wrap gap-3 mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Notificaciones</h1>
        <div className="flex items-center gap-4">
          <label className="flex items-center gap-2 text-sm text-gray-600 cursor-pointer">
            <input
              type="checkbox"
              checked={soloNoLeidas}
              onChange={e => { setSoloNoLeidas(e.target.checked); setPagina(0) }}
              className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
            />
            Solo no leídas
          </label>
          {hayNoLeidas && (
            <button
              onClick={handleMarcarTodas}
              className="text-sm text-primary-600 hover:text-primary-700 font-medium"
            >
              Marcar todas como leídas
            </button>
          )}
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-16">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
        </div>
      ) : !data || data.contenido.length === 0 ? (
        <div className="card text-center py-12">
          <p className="text-gray-400 text-lg">
            {soloNoLeidas ? 'No tienes notificaciones sin leer' : 'Aún no tienes notificaciones'}
          </p>
        </div>
      ) : (
        <>
          <div className="card divide-y divide-gray-100 overflow-hidden">
            {data.contenido.map(n => (
              <button
                key={n.id}
                onClick={() => handleClick(n)}
                className={`w-full text-left px-4 py-3.5 hover:bg-gray-50 transition-colors flex gap-3 items-start
                  ${n.leida ? 'opacity-60' : ''}`}
              >
                <div className="mt-1.5 flex-shrink-0">
                  {n.leida
                    ? <div className="w-2 h-2 rounded-full bg-gray-200" />
                    : <div className="w-2 h-2 rounded-full bg-primary-600" />
                  }
                </div>
                <div className="flex-1 min-w-0">
                  <p className={`text-sm leading-snug ${n.leida ? 'text-gray-500' : 'text-gray-800 font-medium'}`}>
                    {n.mensaje}
                  </p>
                  <p className="text-xs text-gray-400 mt-0.5">{formatTiempoNotificacion(n.fechaCreacion)}</p>
                </div>
              </button>
            ))}
          </div>

          {data.totalPaginas > 1 && (
            <div className="flex items-center justify-center gap-2 mt-4">
              <button
                onClick={() => setPagina(p => p - 1)}
                disabled={pagina === 0}
                className="btn-secondary text-sm py-1.5 px-3 disabled:opacity-40"
              >
                Anterior
              </button>
              <span className="text-sm text-gray-500">
                Página {data.paginaActual + 1} de {data.totalPaginas}
              </span>
              <button
                onClick={() => setPagina(p => p + 1)}
                disabled={pagina >= data.totalPaginas - 1}
                className="btn-secondary text-sm py-1.5 px-3 disabled:opacity-40"
              >
                Siguiente
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
