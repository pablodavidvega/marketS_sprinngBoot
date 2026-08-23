import { useState, useEffect, useRef } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getMisPedidos, confirmarRecepcion, type MisPedidosResponse } from '../../api/pedidos'
import { crearResena } from '../../api/productos'
import ChatPedido from '../../components/ChatPedido'
import EncuentroPedido from '../../components/EncuentroPedido'
import Stars from '../../components/Stars'
import { ESTADO_COLOR, ESTADO_LABEL, ESTADO_LABEL_INFO, ESTADO_LEYENDA } from '../../config/estadosPedido'
import { formatMoneda } from '../../utils/formato'

// Mismo criterio que ResenaApiController/PedidoApiController.calificarComprador: estados que
// representan una venta concretada (por MercadoPago o por fuera de la app).
const ESTADOS_RESENABLES = ['PAGADO', 'RECIBIDO', 'COMPLETADO_EXTERNO']

export default function MisPedidosPage() {
  const [searchParams] = useSearchParams()
  const [data, setData] = useState<MisPedidosResponse | null>(null)
  const [pagina, setPagina] = useState(0)
  const [loading, setLoading] = useState(true)
  const [expandido, setExpandido] = useState<number | null>(null)
  const [confirmando, setConfirmando] = useState<number | null>(null)
  const [chatAbierto, setChatAbierto] = useState<string | null>(null)
  // Son solo 7 estados agrupados (ver ESTADO_LEYENDA) — se puede empezar desplegado sin abrumar.
  const [mostrarLeyenda, setMostrarLeyenda] = useState(true)
  // Clave `${pedidoId}-${vendedorId}` del pedido recién creado con "Acordar pago con el
  // vendedor" (ver CarritoPage) — resalta el bloque de chat para que el comprador escriba
  // antes de esperar a que el vendedor confirme.
  const [resaltarEscribir, setResaltarEscribir] = useState<string | null>(null)

  // Calificación de producto desde la vista de pedidos (misma reseña de DetallePage, ver
  // ResenaApiController). formAbierto/formData usan como clave `${pedidoId}-${productoId}`
  // para poder tener el formulario abierto de forma independiente si el mismo producto
  // aparece en más de un pedido; resenados es global por producto porque la reseña es única
  // por producto+comprador, sin importar en qué pedido se originó.
  const [formAbierto, setFormAbierto] = useState<string | null>(null)
  const [formData, setFormData] = useState<Record<string, { calificacion: number; comentario: string }>>({})
  const [enviandoResena, setEnviandoResena] = useState<string | null>(null)
  const [errorResena, setErrorResena] = useState<Record<string, string>>({})
  const [resenados, setResenados] = useState<Set<number>>(new Set())
  const cardRefs = useRef<Record<number, HTMLDivElement | null>>({})
  // Guarda el último query string ya aplicado (no un simple booleano) para poder reaccionar
  // de nuevo cuando se hace clic en otra notificación estando ya en esta vista: como la ruta
  // no cambia, React Router no remonta el componente, solo actualiza los searchParams.
  const deepLinkAplicado = useRef<string | null>(null)

  const pedidos = data?.contenido ?? []

  useEffect(() => {
    setLoading(true)
    getMisPedidos(pagina)
      .then(r => setData(r.data))
      .finally(() => setLoading(false))
  }, [pagina])

  // Deep-link desde la campanita de notificaciones: ?pedido=123&vendedor=45&foco=chat|encuentro
  // expande el pedido exacto y, si aplica, abre el hilo de chat correcto.
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

    const vendedorParam = searchParams.get('vendedor')
    if (searchParams.get('foco') === 'chat' && vendedorParam) {
      const key = `${pedidoId}-${vendedorParam}`
      setChatAbierto(key)
      if (searchParams.get('nuevo') === '1') {
        setResaltarEscribir(key)
      }
    }

    requestAnimationFrame(() => {
      cardRefs.current[pedidoId]?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    })
  }, [loading, pedidos, searchParams])

  async function handleConfirmarRecepcion(pedidoId: number) {
    setConfirmando(pedidoId)
    try {
      await confirmarRecepcion(pedidoId)
      setData(prev => prev ? {
        ...prev,
        contenido: prev.contenido.map(p => p.pedidoId === pedidoId ? { ...p, estado: 'RECIBIDO' } : p),
      } : prev)
    } finally {
      setConfirmando(null)
    }
  }

  async function handleEnviarResena(key: string, productoId: number, e: React.FormEvent) {
    e.preventDefault()
    const datos = formData[key] ?? { calificacion: 0, comentario: '' }
    if (datos.calificacion === 0) {
      setErrorResena(prev => ({ ...prev, [key]: 'Selecciona una calificación' }))
      return
    }
    setEnviandoResena(key)
    setErrorResena(prev => ({ ...prev, [key]: '' }))
    try {
      await crearResena(productoId, { calificacion: datos.calificacion, comentario: datos.comentario.trim() || undefined })
      setResenados(prev => new Set(prev).add(productoId))
      setFormAbierto(null)
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error
      setErrorResena(prev => ({ ...prev, [key]: msg || 'Error al enviar la reseña' }))
    } finally {
      setEnviandoResena(null)
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
        <Link to="/cliente/dashboard" className="text-gray-400 hover:text-gray-600 text-sm">
          ← Mi cuenta
        </Link>
        <h1 className="text-2xl font-bold text-gray-800">Mis pedidos</h1>
      </div>

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
          <p className="text-gray-400 text-lg mb-4">No tienes pedidos aún</p>
          <Link to="/" className="btn-primary">Explorar productos</Link>
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          {pedidos.map(pedido => (
            <div key={pedido.pedidoId} ref={el => { cardRefs.current[pedido.pedidoId] = el }} className="card">
              {/* Cabecera del pedido */}
              <div
                className="flex flex-wrap items-center justify-between gap-x-3 gap-y-2 cursor-pointer"
                onClick={() => setExpandido(expandido === pedido.pedidoId ? null : pedido.pedidoId)}
              >
                <div className="flex items-center gap-4 min-w-0">
                  <div className="min-w-0">
                    <p className="font-semibold text-gray-800 truncate">Pedido #{pedido.pedidoId}</p>
                    <p className="text-sm text-gray-500 truncate">
                      {new Date(pedido.fecha).toLocaleDateString('es-CO', {
                        year: 'numeric', month: 'long', day: 'numeric',
                      })}
                      {' · '}{pedido.detalles.length} ítem(s)
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3 shrink-0">
                  <span
                    className={`badge ${ESTADO_COLOR[pedido.estado] ?? 'bg-gray-100 text-gray-700'} cursor-help`}
                    title={ESTADO_LABEL_INFO[pedido.estado] ?? ''}
                  >
                    {ESTADO_LABEL[pedido.estado] ?? pedido.estado.replace(/_/g, ' ')}
                  </span>
                  <span className="font-bold text-gray-800 text-lg">
                    {formatMoneda(pedido.total)}
                  </span>
                  <span className="text-gray-400 text-sm">
                    {expandido === pedido.pedidoId ? '▲' : '▼'}
                  </span>
                </div>
              </div>

              {/* Detalle expandible */}
              {expandido === pedido.pedidoId && (
                <div className="mt-4 pt-4 border-t border-gray-100">
                  <div className="flex flex-col gap-3">
                    {pedido.detalles.map((item, i) => {
                      const resenaKey = `${pedido.pedidoId}-${item.productoId}`
                      const yaReseno = item.yaReseno || resenados.has(item.productoId)
                      const puedeResenar = ESTADOS_RESENABLES.includes(pedido.estado)
                      const datosForm = formData[resenaKey] ?? { calificacion: 0, comentario: '' }
                      return (
                      <div key={i} className="flex flex-col gap-2">
                        <div className="flex items-center gap-3">
                          <Link to={`/productos/${item.productoId}`} className="flex-shrink-0">
                            {item.productoImagen ? (
                              <img
                                src={item.productoImagen}
                                alt={item.productoNombre}
                                className="w-14 h-14 object-cover rounded-lg"
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
                          <span className="font-semibold text-gray-700 flex-shrink-0">
                            {formatMoneda(item.subtotal)}
                          </span>
                        </div>

                        {puedeResenar && (
                          <div className="ml-[68px]">
                            {yaReseno ? (
                              <span className="text-xs text-green-600">✓ Ya calificaste este producto</span>
                            ) : formAbierto === resenaKey ? (
                              <form onSubmit={e => handleEnviarResena(resenaKey, item.productoId, e)} className="bg-gray-50 rounded-lg p-3 max-w-sm">
                                <div className="flex items-center gap-2 mb-2">
                                  <Stars
                                    value={datosForm.calificacion}
                                    interactive
                                    onChange={v => setFormData(prev => ({ ...prev, [resenaKey]: { ...datosForm, calificacion: v } }))}
                                  />
                                </div>
                                <textarea
                                  value={datosForm.comentario}
                                  onChange={e => setFormData(prev => ({ ...prev, [resenaKey]: { ...datosForm, comentario: e.target.value } }))}
                                  placeholder="Comentario opcional..."
                                  maxLength={500}
                                  rows={2}
                                  className="input w-full resize-none text-sm"
                                />
                                {errorResena[resenaKey] && (
                                  <p className="text-red-500 text-xs mt-1">{errorResena[resenaKey]}</p>
                                )}
                                <div className="flex gap-2 mt-2">
                                  <button type="submit" disabled={enviandoResena === resenaKey} className="btn-primary text-sm py-1.5 px-3">
                                    {enviandoResena === resenaKey ? 'Enviando...' : 'Publicar'}
                                  </button>
                                  <button type="button" onClick={() => setFormAbierto(null)} className="btn-secondary text-sm py-1.5 px-3">
                                    Cancelar
                                  </button>
                                </div>
                              </form>
                            ) : (
                              <button
                                type="button"
                                onClick={() => setFormAbierto(resenaKey)}
                                className="text-sm text-primary-600 hover:text-primary-700 font-medium"
                              >
                                ⭐ Calificar producto
                              </button>
                            )}
                          </div>
                        )}
                      </div>
                      )
                    })}
                  </div>

                  {/* Chat con cada vendedor del pedido (un pedido puede tener varios) */}
                  {Array.from(
                    new Map(
                      pedido.detalles
                        .filter((d): d is typeof d & { vendedorId: number; vendedorUsername: string } => d.vendedorId != null)
                        .map(d => [d.vendedorId, d.vendedorUsername])
                    )
                  ).map(([vendedorId, vendedorUsername]) => {
                    const key = `${pedido.pedidoId}-${vendedorId}`
                    const destacado = resaltarEscribir === key
                    return (
                      <div key={key} className="mt-4 pt-4 border-t border-gray-100">
                        <div className={destacado ? 'rounded-lg border border-amber-300 bg-amber-50 p-3' : ''}>
                          {destacado && (
                            <p className="text-sm text-amber-800 mb-3">
                              👋 Escríbele al vendedor por chat para coordinar el pago, o acuerden un punto de encuentro.
                            </p>
                          )}
                          <button
                            onClick={() => {
                              setChatAbierto(chatAbierto === key ? null : key)
                              setResaltarEscribir(prev => prev === key ? null : prev)
                            }}
                            className="text-sm text-primary-600 hover:text-primary-700 font-medium"
                          >
                            {chatAbierto === key ? '▲' : '💬'} Chat con {vendedorUsername}
                          </button>
                          {chatAbierto === key && (
                            <div className="mt-3">
                              <ChatPedido pedidoId={pedido.pedidoId} vendedorId={vendedorId} />
                            </div>
                          )}
                          <div className="mt-3">
                            <EncuentroPedido pedidoId={pedido.pedidoId} vendedorId={vendedorId} />
                          </div>
                        </div>
                      </div>
                    )
                  })}

                  {pedido.paymentUrl && pedido.estado === 'PENDIENTE' && (
                    <div className="mt-4 pt-4 border-t border-gray-100">
                      <a
                        href={pedido.paymentUrl}
                        className="btn-primary w-full text-center block"
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        Pagar con MercadoPago
                      </a>
                    </div>
                  )}

                  {pedido.estado === 'PAGADO' && (
                    <div className="mt-4 pt-4 border-t border-gray-100">
                      <p className="text-sm text-gray-500 mb-3">
                        ¿Ya recibiste tu pedido? Confirma la recepción para cerrar la compra.
                      </p>
                      <button
                        onClick={() => handleConfirmarRecepcion(pedido.pedidoId)}
                        disabled={confirmando === pedido.pedidoId}
                        className="btn-primary w-full"
                      >
                        {confirmando === pedido.pedidoId ? 'Confirmando...' : 'Confirmar recepción'}
                      </button>
                    </div>
                  )}

                  {pedido.estado === 'ACORDADO_EXTERNO' && (
                    <div className="mt-4 pt-4 border-t border-gray-100">
                      <p className="text-sm text-gray-500">
                        Acordaste el pago por fuera de la app ({pedido.metodoPago?.toLowerCase()}). Coordina la entrega con el vendedor por chat; él confirmará cuando la venta se concrete.
                      </p>
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
          <div className="flex items-center gap-2">
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
        </div>
      )}
    </div>
  )
}
