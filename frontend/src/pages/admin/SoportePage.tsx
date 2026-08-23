import { useState, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { getAdminSolicitudes, responderSolicitud, cambiarEstadoSolicitud } from '../../api/contacto'
import type { AdminContactoPaginado } from '../../api/contacto'
import type { ContactoResponse } from '../../types'
import { usePaginatedResource } from '../../hooks/usePaginatedResource'
import Paginador from '../../components/Paginador'

const ESTADO_COLOR: Record<string, string> = {
  PENDIENTE:  'bg-yellow-100 text-yellow-700',
  EN_PROCESO: 'bg-blue-100 text-blue-700',
  RESUELTO:   'bg-green-100 text-green-700',
}

const ESTADO_LABEL: Record<string, string> = {
  PENDIENTE:  'Pendiente',
  EN_PROCESO: 'En proceso',
  RESUELTO:   'Resuelto',
}

export default function SoporteAdminPage() {
  const [filtro, setFiltro] = useState<'TODOS' | 'PENDIENTE' | 'EN_PROCESO' | 'RESUELTO'>('TODOS')
  const [expandida, setExpandida] = useState<number | null>(null)
  const [respuestaTexto, setRespuestaTexto] = useState<Record<number, string>>({})
  const [procesando, setProcesando] = useState<number | null>(null)

  const cargarSolicitudes = useCallback(
    (p: number) => getAdminSolicitudes(filtro === 'TODOS' ? undefined : filtro, p, 30),
    [filtro],
  )

  const { data, loading, error, setError, pagina, setPagina, setData } =
    usePaginatedResource<AdminContactoPaginado>(cargarSolicitudes, 'No se pudieron cargar las solicitudes. Intenta de nuevo.')

  function cambiarFiltro(f: 'TODOS' | 'PENDIENTE' | 'EN_PROCESO' | 'RESUELTO') {
    setFiltro(f)
    if (pagina !== 0) setPagina(0)
  }

  // Actualiza la solicitud en la página actual y ajusta los contadores globales (que reflejan
  // el total de la plataforma, no solo esta página) según el cambio de estado.
  function actualizarSolicitud(actualizada: ContactoResponse, anterior: ContactoResponse) {
    setData(prev => {
      if (!prev) return prev
      const contenido = prev.contenido.map(s => s.id === actualizada.id ? actualizada : s)
      const delta = (estado: string) =>
        (anterior.estado === estado ? -1 : 0) + (actualizada.estado === estado ? 1 : 0)
      return {
        ...prev,
        contenido,
        pendientes: prev.pendientes + delta('PENDIENTE'),
        enProceso:  prev.enProceso + delta('EN_PROCESO'),
        resueltos:  prev.resueltos + delta('RESUELTO'),
      }
    })
  }

  async function handleCambiarEstado(id: number, estado: string) {
    const anterior = data?.contenido.find(s => s.id === id)
    if (!anterior) return
    setProcesando(id)
    try {
      const { data: actualizada } = await cambiarEstadoSolicitud(id, estado)
      actualizarSolicitud(actualizada, anterior)
    } finally {
      setProcesando(null)
    }
  }

  async function handleResponder(id: number) {
    const texto = respuestaTexto[id]?.trim()
    if (!texto) return
    const anterior = data?.contenido.find(s => s.id === id)
    if (!anterior) return
    setProcesando(id)
    try {
      const { data: actualizada } = await responderSolicitud(id, texto)
      actualizarSolicitud(actualizada, anterior)
      setRespuestaTexto(prev => ({ ...prev, [id]: '' }))
    } finally {
      setProcesando(null)
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center py-16">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
      </div>
    )
  }

  if (!data) return <div className="text-center py-16 text-gray-500">Error al cargar</div>

  const solicitudes = data.contenido

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center gap-4 mb-6">
        <Link to="/admin/dashboard" className="inline-flex items-center gap-1 -ml-2 pl-2 pr-3 py-1 rounded-md text-sm font-medium text-gray-600 hover:text-primary-600 hover:bg-gray-100 transition-colors">← Admin</Link>
        <h1 className="text-2xl font-bold text-gray-800">Soporte al cliente</h1>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3 mb-5 flex items-start justify-between gap-3">
          <span>{error}</span>
          <button onClick={() => setError('')} className="text-red-400 hover:text-red-600 text-lg leading-none flex-shrink-0">&times;</button>
        </div>
      )}

      {/* Stats */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-8">
        {[
          { label: 'Total',      value: data.pendientes + data.enProceso + data.resueltos, color: 'text-gray-800' },
          { label: 'Pendientes', value: data.pendientes, color: 'text-yellow-600' },
          { label: 'En proceso', value: data.enProceso,  color: 'text-blue-600' },
          { label: 'Resueltos',  value: data.resueltos,  color: 'text-green-600' },
        ].map(s => (
          <div key={s.label} className="card text-center">
            <p className={`text-3xl font-bold ${s.color}`}>{s.value}</p>
            <p className="text-sm text-gray-500 mt-1">{s.label}</p>
          </div>
        ))}
      </div>

      {/* Filtros */}
      <div className="flex gap-2 mb-6 flex-wrap">
        {(['TODOS', 'PENDIENTE', 'EN_PROCESO', 'RESUELTO'] as const).map(f => (
          <button
            key={f}
            onClick={() => cambiarFiltro(f)}
            className={`px-4 py-1.5 rounded-full text-sm font-medium border transition-colors ${
              filtro === f
                ? 'bg-primary-600 text-white border-primary-600'
                : 'bg-white text-gray-600 border-gray-300 hover:border-primary-400'
            }`}
          >
            {f === 'TODOS' ? 'Todos' : ESTADO_LABEL[f]}
          </button>
        ))}
      </div>

      {/* Lista de solicitudes */}
      {solicitudes.length === 0 ? (
        <div className="card text-center py-10 text-gray-400">No hay solicitudes en este estado.</div>
      ) : (
        <>
        <div className="flex flex-col gap-3">
          {solicitudes.map(s => (
            <div key={s.id} className="card">
              {/* Cabecera */}
              <div
                className="flex items-start justify-between cursor-pointer gap-4"
                onClick={() => setExpandida(expandida === s.id ? null : s.id)}
              >
                <div className="min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className={`badge text-xs ${ESTADO_COLOR[s.estado]}`}>
                      {ESTADO_LABEL[s.estado]}
                    </span>
                    <span className="text-xs text-gray-400">{s.tipoSolicitud}</span>
                  </div>
                  <p className="font-semibold text-gray-800 mt-1 truncate">{s.asunto}</p>
                  <p className="text-xs text-gray-400 mt-0.5">
                    {s.usuarioUsername ? `@${s.usuarioUsername} · ` : ''}{s.email} ·{' '}
                    {new Date(s.fechaCreacion).toLocaleDateString('es-CO', { year: 'numeric', month: 'short', day: 'numeric' })}
                  </p>
                </div>
                <span className="text-gray-400 text-sm flex-shrink-0">{expandida === s.id ? '▲' : '▼'}</span>
              </div>

              {/* Detalle expandible */}
              {expandida === s.id && (
                <div className="mt-4 pt-4 border-t border-gray-100 flex flex-col gap-4">

                  {/* Info del remitente */}
                  <div className="grid grid-cols-2 gap-2 text-sm">
                    <div><span className="text-gray-400">Nombre: </span><span className="font-medium">{s.nombre}</span></div>
                    <div><span className="text-gray-400">Email: </span><span className="font-medium">{s.email}</span></div>
                  </div>

                  {/* Mensaje */}
                  <div>
                    <p className="text-xs font-semibold text-gray-400 uppercase mb-1">Mensaje</p>
                    <p className="text-sm text-gray-700 whitespace-pre-wrap bg-gray-50 rounded-lg p-3">{s.mensaje}</p>
                  </div>

                  {/* Respuesta existente */}
                  {s.respuesta && (
                    <div className="bg-blue-50 border border-blue-100 rounded-lg p-3">
                      <p className="text-xs font-semibold text-blue-500 uppercase mb-1">Tu respuesta</p>
                      <p className="text-sm text-gray-700 whitespace-pre-wrap">{s.respuesta}</p>
                    </div>
                  )}

                  {/* Acciones */}
                  <div className="flex flex-col gap-3">
                    {/* Cambiar estado */}
                    {s.estado !== 'RESUELTO' && (
                      <div className="flex gap-2 flex-wrap">
                        {s.estado === 'PENDIENTE' && (
                          <button
                            onClick={() => handleCambiarEstado(s.id, 'EN_PROCESO')}
                            disabled={procesando === s.id}
                            className="px-3 py-1.5 text-sm bg-blue-100 text-blue-700 rounded-lg hover:bg-blue-200 disabled:opacity-50"
                          >
                            Marcar en proceso
                          </button>
                        )}
                        <button
                          onClick={() => handleCambiarEstado(s.id, 'RESUELTO')}
                          disabled={procesando === s.id}
                          className="px-3 py-1.5 text-sm bg-green-100 text-green-700 rounded-lg hover:bg-green-200 disabled:opacity-50"
                        >
                          Marcar resuelto
                        </button>
                      </div>
                    )}

                    {/* Formulario de respuesta */}
                    <div>
                      <p className="text-xs font-semibold text-gray-400 uppercase mb-1">
                        {s.respuesta ? 'Actualizar respuesta' : 'Responder'}
                      </p>
                      <textarea
                        rows={3}
                        className="input w-full resize-none text-sm"
                        placeholder="Escribe tu respuesta al usuario..."
                        value={respuestaTexto[s.id] ?? ''}
                        onChange={e => setRespuestaTexto(prev => ({ ...prev, [s.id]: e.target.value }))}
                      />
                      <button
                        onClick={() => handleResponder(s.id)}
                        disabled={procesando === s.id || !respuestaTexto[s.id]?.trim()}
                        className="btn-primary text-sm mt-2 disabled:opacity-50"
                      >
                        {procesando === s.id ? 'Enviando...' : 'Enviar respuesta'}
                      </button>
                    </div>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>

        <div className="flex items-center justify-center mt-6">
          <Paginador paginaActual={pagina} totalPaginas={data.totalPaginas} onCambiarPagina={setPagina} />
        </div>
        </>
      )}
    </div>
  )
}
