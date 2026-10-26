import { useState, useEffect, useRef } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getPedidosVendedor, marcarCompletado, marcarRechazado, calificarComprador, type PedidosVendedorResponse } from '../../api/pedidos'
import ChatPedido from '../../components/ChatPedido'
import EncuentroPedido from '../../components/EncuentroPedido'
import ReputacionBar from '../../components/ReputacionBar'
import Stars from '../../components/Stars'
import Paginador from '../../components/Paginador'
import { ESTADO_COLOR, ESTADO_LABEL, ESTADO_LABEL_INFO, ESTADO_LEYENDA } from '../../config/estadosPedido'
import { usePaginatedResource } from '../../hooks/usePaginatedResource'
import { formatMoneda } from '../../utils/formato'

export default function PedidosVendedorPage() {
  const [searchParams] = useSearchParams()
  const { data, setData, loading, error, pagina, setPagina } =
    usePaginatedResource<PedidosVendedorResponse>(getPedidosVendedor, 'No se pudieron cargar los pedidos. Intenta de nuevo.')
  // Son solo 7 estados agrupados (ver ESTADO_LEYENDA) — se puede empezar desplegado sin abrumar.
  const [mostrarLeyenda, setMostrarLeyenda] = useState(true)
  const [expandido, setExpandido] = useState<number | null>(null)
  const [chatAbierto, setChatAbierto] = useState<number | null>(null)
  const [resolviendo, setResolviendo] = useState<number | null>(null)
  const [calificacionValor, setCalificacionValor] = useState(0)
  const [comentarioValor, setComentarioValor] = useState('')
  const [enviandoCalificacion, setEnviandoCalificacion] = useState<number | null>(null)
  const [errorCalificacion, setErrorCalificacion] = useState('')
  const cardRefs = useRef<Record<number, HTMLDivElement | null>>({})
  // Guarda el último query string ya aplicado (no un simple booleano) para poder reaccionar
  // de nuevo cuando se hace clic en otra notificación estando ya en esta vista: como la ruta
  // no cambia, React Router no remonta el componente, solo actualiza los searchParams.
  const deepLinkAplicado = useRef<string | null>(null)

  const pedidos = data?.contenido ?? []

  // Deep-link desde la campanita de notificaciones: ?pedido=123&foco=chat|encuentro
  // expande el pedido exacto y, si aplica, abre el chat (cada fila ya es por-vendedor).
  useEffect(() => {
    if (loading) return
    const pedidoParam = searchParams.get('pedido')
    if (!pedidoParam) return
    const key = searchParams.toString()
    if (deepLinkAplicado.current === key) return
    const pedidoId = Number(pedidoParam)
    if (!pedidos.some(p => p.pedidoId === pedidoId)) return

    deepLinkAplicado.current = key
    setExpandido(pedidoId)
    if (searchParams.get('foco') === 'chat') {
      setChatAbierto(pedidoId)
    }

    requestAnimationFrame(() => {
      cardRefs.current[pedidoId]?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    })
  }, [loading, pedidos, searchParams])

  async function handleResolver(pedidoId: number, completado: boolean) {
    setResolviendo(pedidoId)
    try {
      const { data: resultado } = completado ? await marcarCompletado(pedidoId) : await marcarRechazado(pedidoId)
      setData(prev => prev ? {
        ...prev,
        contenido: prev.contenido.map(p => p.pedidoId === pedidoId ? { ...p, estado: resultado.estado, yaResolviMiParte: true } : p),
      } : prev)
    } finally {
      setResolviendo(null)
    }
  }

  function toggleExpandido(pedidoId: number) {
    setExpandido(expandido === pedidoId ? null : pedidoId)
    setCalificacionValor(0)
    setComentarioValor('')
    setErrorCalificacion('')
  }

  async function handleCalificarComprador(e: React.FormEvent, pedidoId: number) {
    e.preventDefault()
    if (calificacionValor === 0) { setErrorCalificacion('Selecciona una calificación'); return }
    setEnviandoCalificacion(pedidoId)
    setErrorCalificacion('')
    try {
      await calificarComprador(pedidoId, { calificacion: calificacionValor, comentario: comentarioValor.trim() || undefined })
      setData(prev => prev ? {
        ...prev,
        contenido: prev.contenido.map(p => p.pedidoId === pedidoId ? { ...p, yaCalifiqueComprador: true, puedeCalificarComprador: false } : p),
      } : prev)
      setCalificacionValor(0)
      setComentarioValor('')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error
      setErrorCalificacion(msg || 'Error al enviar la calificación')
    } finally {
      setEnviandoCalificacion(null)
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center py-16">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center gap-4 mb-2">
        <Link to="/vendedor" className="inline-flex items-center gap-1 -ml-2 pl-2 pr-3 py-1 rounded-md text-sm font-medium text-gray-600 hover:text-primary-600 hover:bg-gray-100 transition-colors">
          ← Panel vendedor
        </Link>
        <h1 className="text-2xl font-bold text-gray-800">Pedidos recibidos</h1>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3 mb-5">
          {error}
        </div>
      )}

      <button
        type="button"
        onClick={() => setMostrarLeyenda(v => !v)}
        className="text-sm text-primary-600 hover:text-primary-700 font-medium inline-flex items-center gap-1 mb-6"
      >
        <span>ℹ️</span> ¿Qué significa cada estado? {mostrarLeyenda ? '▲' : '▼'}
      </button>

      {mostrarLeyenda && (
        <div className="bg-blue-50 border border-blue-100 rounded-lg p-4 mb-6">
          <div className="grid grid-cols-1 sm:grid-cols-[150px_1fr] gap-x-4 gap-y-3">
            {ESTADO_LEYENDA.map(({ label, color, texto }) => (
              <div key={label} className="contents">
                <span className={`badge ${color} self-start w-fit`}>
                  {label}
                </span>
                <span className="text-xs text-gray-600 leading-relaxed">{texto}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {pedidos.length === 0 ? (
        <div className="card text-center py-12">
          <p className="text-gray-400 text-lg">Aún no has recibido pedidos</p>
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          {pedidos.map(pedido => (
            <div key={pedido.pedidoId} ref={el => { cardRefs.current[pedido.pedidoId] = el }} className="card">
              {/* Cabecera */}
              <div
                className="flex flex-wrap items-center justify-between gap-x-3 gap-y-2 cursor-pointer"
                onClick={() => toggleExpandido(pedido.pedidoId)}
              >
                <div className="min-w-0">
                  <p className="font-semibold text-gray-800 truncate">Pedido #{pedido.pedidoId}</p>
                  <p className="text-sm text-gray-500 truncate">
                    {new Date(pedido.fecha).toLocaleDateString('es-CO', {
                      year: 'numeric', month: 'long', day: 'numeric',
                    })}
                    {' · '}{pedido.items.length} ítem(s)
                  </p>
                </div>
                <div className="flex items-center gap-3 shrink-0">
                  <span
                    className={`badge ${ESTADO_COLOR[pedido.estado] ?? 'bg-gray-100 text-gray-700'} cursor-help`}
                    title={ESTADO_LABEL_INFO[pedido.estado] ?? ''}
                  >
                    {ESTADO_LABEL[pedido.estado] ?? pedido.estado.replace(/_/g, ' ')}
                  </span>
                  <span className="text-gray-400 text-sm">
                    {expandido === pedido.pedidoId ? '▲' : '▼'}
                  </span>
                </div>
              </div>

              {/* Detalle expandible */}
              {expandido === pedido.pedidoId && (
                <div className="mt-4 pt-4 border-t border-gray-100 flex flex-col gap-5">
                  {/* Datos del comprador */}
                  <div className="bg-gray-50 rounded-lg p-4">
                    <div className="flex items-center justify-between gap-3 mb-2">
                      <p className="text-xs font-semibold text-gray-400 uppercase tracking-wide">
                        Datos del comprador
                      </p>
                      <ReputacionBar compact reputacion={pedido.compradorReputacion} />
                    </div>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-sm">
                      <div>
                        <span className="text-gray-500">Usuario: </span>
                        <span className="font-medium text-gray-800">{pedido.compradorUsername}</span>
                      </div>
                      <div>
                        <span className="text-gray-500">Email: </span>
                        <span className="font-medium text-gray-800">{pedido.compradorEmail}</span>
                      </div>
                      <div>
                        <span className="text-gray-500">Teléfono: </span>
                        <span className="font-medium text-gray-800">{pedido.compradorPhone || '—'}</span>
                      </div>
                      <div>
                        <span className="text-gray-500">Dirección: </span>
                        <span className="font-medium text-gray-800">{pedido.compradorAddress || '—'}</span>
                      </div>
                    </div>
                  </div>

                  {/* Productos */}
                  <div>
                    <p className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-2">
                      Productos
                    </p>
                    <div className="flex flex-col gap-3">
                      {pedido.items.map((item, i) => (
                        <div key={i} className="flex items-center gap-3">
                          <Link to={`/productos/${item.productoId}`} className="flex-shrink-0">
                            {item.productoImagen ? (
                              <img
                                src={item.productoImagen}
                                loading="lazy"
                                alt={item.productoNombre}
                                className="w-14 h-14 object-contain rounded-lg bg-gray-100"
                              />
                            ) : (
                              <div className="w-14 h-14 bg-gray-100 rounded-lg flex items-center justify-center text-gray-300 text-xl">
                                📦
                              </div>
                            )}
                          </Link>
                          <div className="flex-1 min-w-0">
                            <Link to={`/productos/${item.productoId}`} className="font-medium text-gray-800 truncate hover:text-primary-600 hover:underline block">
                              {item.productoNombre}
                            </Link>
                            <p className="text-sm text-gray-500">
                              {item.cantidad} × {formatMoneda(item.precioUnitario)}
                            </p>
                          </div>
                          <div className="text-right flex-shrink-0">
                            <p className="text-xs text-gray-400">Tu ganancia</p>
                            <p className="font-semibold text-green-600">
                              {formatMoneda(item.gananciaVendedor ?? item.precioUnitario * item.cantidad)}
                            </p>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Chat con el comprador sobre la entrega */}
                  <div className="border-t border-gray-100 pt-4">
                    <button
                      onClick={() => setChatAbierto(chatAbierto === pedido.pedidoId ? null : pedido.pedidoId)}
                      className="text-sm text-primary-600 hover:text-primary-700 font-medium"
                    >
                      {chatAbierto === pedido.pedidoId ? '▲' : '💬'} Chat con {pedido.compradorUsername}
                    </button>
                    {chatAbierto === pedido.pedidoId && (
                      <div className="mt-3">
                        <ChatPedido pedidoId={pedido.pedidoId} vendedorId={pedido.vendedorId} />
                      </div>
                    )}
                  </div>

                  {/* Agendar encuentro para la entrega en persona */}
                  <div className="border-t border-gray-100 pt-4">
                    <EncuentroPedido pedidoId={pedido.pedidoId} vendedorId={pedido.vendedorId} />
                  </div>

                  {/* Venta acordada por fuera de la app: el vendedor confirma si se concretó */}
                  {pedido.estado === 'ACORDADO_EXTERNO' && !pedido.yaResolviMiParte && (
                    <div className="border-t border-gray-100 pt-4">
                      <p className="text-sm text-gray-500 mb-3">
                        Este pedido se acordó por fuera de la app ({pedido.metodoPago?.toLowerCase()}). Confirma si la venta se realizó.
                      </p>
                      <div className="flex gap-3">
                        <button
                          onClick={() => handleResolver(pedido.pedidoId, true)}
                          disabled={resolviendo === pedido.pedidoId}
                          className="btn-primary flex-1"
                        >
                          {resolviendo === pedido.pedidoId ? 'Guardando...' : 'Compra realizada'}
                        </button>
                        <button
                          onClick={() => handleResolver(pedido.pedidoId, false)}
                          disabled={resolviendo === pedido.pedidoId}
                          className="btn-secondary flex-1"
                        >
                          {resolviendo === pedido.pedidoId ? 'Guardando...' : 'No se realizó'}
                        </button>
                      </div>
                    </div>
                  )}

                  {/* Ya resolví mi parte, pero el pedido tiene otro vendedor que todavía no
                      resolvió la suya — el pedido sigue en ACORDADO_EXTERNO hasta que todos actúen */}
                  {pedido.estado === 'ACORDADO_EXTERNO' && pedido.yaResolviMiParte && (
                    <div className="border-t border-gray-100 pt-4">
                      <p className="text-sm text-gray-400">
                        Ya enviaste tu confirmación para este pedido. Falta que otro vendedor de este mismo pedido resuelva su parte.
                      </p>
                    </div>
                  )}

                  {/* Calificar al comprador — solo cuando la transacción quedó completada */}
                  {pedido.puedeCalificarComprador && (
                    <form onSubmit={e => handleCalificarComprador(e, pedido.pedidoId)} className="border-t border-gray-100 pt-4">
                      <h3 className="font-semibold text-gray-700 mb-3">Califica a {pedido.compradorUsername}</h3>
                      <div className="flex items-center gap-3 mb-3">
                        <Stars value={calificacionValor} size="lg" interactive onChange={setCalificacionValor} />
                        {calificacionValor > 0 && (
                          <span className="text-sm text-gray-500">
                            {['', 'Muy malo', 'Malo', 'Regular', 'Bueno', 'Excelente'][calificacionValor]}
                          </span>
                        )}
                      </div>
                      <textarea
                        value={comentarioValor}
                        onChange={e => setComentarioValor(e.target.value)}
                        placeholder="Comentario opcional..."
                        maxLength={500}
                        rows={2}
                        className="input w-full resize-none text-sm"
                      />
                      {errorCalificacion && <p className="text-red-500 text-sm mt-2">{errorCalificacion}</p>}
                      <button type="submit" disabled={enviandoCalificacion === pedido.pedidoId} className="btn-primary mt-3">
                        {enviandoCalificacion === pedido.pedidoId ? 'Enviando...' : 'Calificar comprador'}
                      </button>
                    </form>
                  )}

                  {pedido.yaCalifiqueComprador && (
                    <div className="bg-green-50 border border-green-200 text-green-700 rounded-lg p-3 text-sm">
                      Ya calificaste al comprador de este pedido.
                    </div>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {data && data.totalPaginas > 1 && (
        <div className="flex items-center justify-between mt-6">
          <p className="text-sm text-gray-500">
            {data.totalElementos} pedido(s) en total
          </p>
          <Paginador paginaActual={pagina} totalPaginas={data.totalPaginas} onCambiarPagina={setPagina} />
        </div>
      )}
    </div>
  )
}
