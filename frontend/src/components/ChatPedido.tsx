import { useState, useEffect, useCallback, useRef } from 'react'
import { getChat, enviarMensajeChat } from '../api/chat'
import type { ChatMensaje } from '../types'

const INTERVALO_MS = 10_000

// Combina los mensajes ya cargados con una tanda recibida del backend (que puede ser la
// página más reciente del polling o una tanda de mensajes antiguos pedida por el usuario).
// Se deduplica por id (así el polling solo agrega mensajes nuevos y actualiza el estado
// "leido" de los existentes, sin duplicar los que ya vinieron por "cargar más antiguos") y
// se reordena por fecha para garantizar el orden cronológico sin importar de dónde vino cada tanda.
function fusionarMensajes(previos: ChatMensaje[], tanda: ChatMensaje[]): ChatMensaje[] {
  const mapa = new Map(previos.map(m => [m.id, m]))
  for (const m of tanda) mapa.set(m.id, m)
  return Array.from(mapa.values()).sort((a, b) => a.fechaEnvio.localeCompare(b.fechaEnvio))
}

interface Props {
  pedidoId: number
  vendedorId: number
  // Resalta el input de mensaje (borde ámbar) cuando el comprador llega acá recién después de
  // "Acordar pago con el vendedor" — el aviso ya no basta si no se ve dónde escribir. Se apaga
  // apenas el input recibe foco, para que no quede marcado después de que ya lo usó.
  resaltarInput?: boolean
  onInputEnfocado?: () => void
}

export default function ChatPedido({ pedidoId, vendedorId, resaltarInput, onInputEnfocado }: Props) {
  const [mensajes, setMensajes] = useState<ChatMensaje[]>([])
  const [texto, setTexto] = useState('')
  const [cargando, setCargando] = useState(true)
  const [enviando, setEnviando] = useState(false)
  const [hayMasAntiguos, setHayMasAntiguos] = useState(false)
  const [cargandoAntiguos, setCargandoAntiguos] = useState(false)
  const listaRef = useRef<HTMLDivElement>(null)
  // Solo autoscrolleamos si el usuario ya estaba cerca del final; si scrolleó hacia
  // arriba a leer mensajes viejos, el polling (cada 10s) no debe interrumpirlo.
  const pegadoAlFinalRef = useRef(true)
  // Próxima página "antigua" a pedir si el usuario hace click en "cargar mensajes anteriores".
  // Mientras siga en 1 (no ha cargado historial extra), el polling de la página 0 sigue
  // siendo la fuente de verdad de "hayMasAntiguos"; en cuanto el usuario carga historial,
  // ese valor solo se actualiza desde la respuesta de esa carga (la página 0 del polling
  // siempre reporta "hay más" mientras el hilo tenga más de 30 mensajes en total).
  const paginaAntiguaSiguienteRef = useRef(1)

  const handleScroll = useCallback(() => {
    const el = listaRef.current
    if (!el) return
    pegadoAlFinalRef.current = el.scrollHeight - el.scrollTop - el.clientHeight < 60
  }, [])

  const cargar = useCallback(async () => {
    try {
      const { data } = await getChat(pedidoId, vendedorId, 0)
      setMensajes(prev => fusionarMensajes(prev, data.mensajes))
      if (paginaAntiguaSiguienteRef.current === 1) {
        setHayMasAntiguos(data.hayMasAntiguos)
      }
    } catch { /* silent */ } finally {
      setCargando(false)
    }
  }, [pedidoId, vendedorId])

  useEffect(() => {
    cargar()
    const id = setInterval(cargar, INTERVALO_MS)
    return () => clearInterval(id)
  }, [cargar])

  useEffect(() => {
    if (!pegadoAlFinalRef.current) return
    listaRef.current?.scrollTo({ top: listaRef.current.scrollHeight })
  }, [mensajes])

  const cargarAntiguos = useCallback(async () => {
    if (cargandoAntiguos || !hayMasAntiguos) return
    setCargandoAntiguos(true)
    const el = listaRef.current
    const scrollHeightAntes = el?.scrollHeight ?? 0
    try {
      const { data } = await getChat(pedidoId, vendedorId, paginaAntiguaSiguienteRef.current)
      setMensajes(prev => fusionarMensajes(prev, data.mensajes))
      setHayMasAntiguos(data.hayMasAntiguos)
      paginaAntiguaSiguienteRef.current += 1
      // Al anteponer mensajes viejos arriba, el navegador recalcula scrollHeight; ajustamos
      // scrollTop por la diferencia para que la vista no salte al tope de la conversación.
      requestAnimationFrame(() => {
        if (el) el.scrollTop += el.scrollHeight - scrollHeightAntes
      })
    } catch { /* silent */ } finally {
      setCargandoAntiguos(false)
    }
  }, [pedidoId, vendedorId, cargandoAntiguos, hayMasAntiguos])

  async function handleEnviar(e: React.FormEvent) {
    e.preventDefault()
    const contenido = texto.trim()
    if (!contenido) return
    setEnviando(true)
    try {
      await enviarMensajeChat(pedidoId, vendedorId, contenido)
      setTexto('')
      pegadoAlFinalRef.current = true
      await cargar()
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="border border-gray-200 rounded-lg overflow-hidden">
      <div ref={listaRef} onScroll={handleScroll} className="max-h-64 overflow-y-auto bg-gray-50 p-3 flex flex-col gap-2">
        {cargando ? (
          <p className="text-center text-sm text-gray-400 py-4">Cargando...</p>
        ) : mensajes.length === 0 ? (
          <p className="text-center text-sm text-gray-400 py-4">
            Sin mensajes aún. Escribe para coordinar la entrega.
          </p>
        ) : (
          <>
            {hayMasAntiguos && (
              <div className="flex justify-center pb-1">
                <button
                  type="button"
                  onClick={cargarAntiguos}
                  disabled={cargandoAntiguos}
                  className="text-xs text-primary-700 hover:underline disabled:opacity-50"
                >
                  {cargandoAntiguos ? 'Cargando...' : 'Cargar mensajes anteriores'}
                </button>
              </div>
            )}
            {mensajes.map(m => {
              const esMio = m.propio
              return (
                <div key={m.id} className={`flex ${esMio ? 'justify-end' : 'justify-start'}`}>
                  <div
                    className={`max-w-[75%] rounded-lg px-3 py-2 text-sm ${
                      esMio ? 'bg-primary-600 text-white' : 'bg-emerald-50 border border-emerald-100 text-emerald-950'
                    }`}
                  >
                    {!esMio && <p className="text-xs font-medium opacity-70 mb-0.5">{m.autorUsername}</p>}
                    <p className="whitespace-pre-wrap break-words">{m.contenido}</p>
                    <p className={`text-[10px] mt-1 flex items-center gap-1 ${esMio ? 'text-white/70 justify-end' : 'text-emerald-700/70'}`}>
                      {new Date(m.fechaEnvio).toLocaleString('es-CO', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}
                      {esMio && <span>{m.leido ? '✓✓ Visto' : '✓ Enviado'}</span>}
                    </p>
                  </div>
                </div>
              )
            })}
          </>
        )}
      </div>
      <form onSubmit={handleEnviar} className="flex gap-2 p-2 border-t border-gray-200 bg-white">
        <input
          value={texto}
          onChange={e => setTexto(e.target.value)}
          onFocus={onInputEnfocado}
          placeholder="Escribe un mensaje sobre la entrega..."
          maxLength={500}
          autoFocus={resaltarInput}
          className={`input flex-1 text-sm ${
            resaltarInput ? 'ring-2 ring-amber-400 ring-offset-2 ring-offset-white' : ''
          }`}
        />
        <button type="submit" disabled={enviando || !texto.trim()} className="btn-primary text-sm px-4">
          Enviar
        </button>
      </form>
    </div>
  )
}
