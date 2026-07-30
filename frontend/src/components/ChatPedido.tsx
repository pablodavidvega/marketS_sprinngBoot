import { useState, useEffect, useCallback, useRef } from 'react'
import { getChat, enviarMensajeChat } from '../api/chat'
import { useAuth } from '../context/AuthContext'
import type { ChatMensaje } from '../types'

const INTERVALO_MS = 10_000

export default function ChatPedido({ pedidoId, vendedorId }: { pedidoId: number; vendedorId: number }) {
  const { hasRole } = useAuth()
  const soyVendedor = hasRole('VENDEDOR') || hasRole('ADMIN')
  const [mensajes, setMensajes] = useState<ChatMensaje[]>([])
  const [texto, setTexto] = useState('')
  const [cargando, setCargando] = useState(true)
  const [enviando, setEnviando] = useState(false)
  const listaRef = useRef<HTMLDivElement>(null)

  const cargar = useCallback(async () => {
    try {
      const { data } = await getChat(pedidoId, vendedorId)
      setMensajes(data)
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
    listaRef.current?.scrollTo({ top: listaRef.current.scrollHeight })
  }, [mensajes])

  async function handleEnviar(e: React.FormEvent) {
    e.preventDefault()
    const contenido = texto.trim()
    if (!contenido) return
    setEnviando(true)
    try {
      await enviarMensajeChat(pedidoId, vendedorId, contenido)
      setTexto('')
      await cargar()
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="border border-gray-200 rounded-lg overflow-hidden">
      <div ref={listaRef} className="max-h-64 overflow-y-auto bg-gray-50 p-3 flex flex-col gap-2">
        {cargando ? (
          <p className="text-center text-sm text-gray-400 py-4">Cargando...</p>
        ) : mensajes.length === 0 ? (
          <p className="text-center text-sm text-gray-400 py-4">
            Sin mensajes aún. Escribe para coordinar la entrega.
          </p>
        ) : (
          mensajes.map(m => {
            const esMio = soyVendedor ? m.esVendedor : !m.esVendedor
            return (
              <div key={m.id} className={`flex ${esMio ? 'justify-end' : 'justify-start'}`}>
                <div
                  className={`max-w-[75%] rounded-lg px-3 py-2 text-sm ${
                    esMio ? 'bg-primary-600 text-white' : 'bg-white border border-gray-200 text-gray-700'
                  }`}
                >
                  {!esMio && <p className="text-xs font-medium opacity-70 mb-0.5">{m.autorUsername}</p>}
                  <p className="whitespace-pre-wrap break-words">{m.contenido}</p>
                  <p className={`text-[10px] mt-1 flex items-center gap-1 ${esMio ? 'text-white/70 justify-end' : 'text-gray-400'}`}>
                    {new Date(m.fechaEnvio).toLocaleString('es-CO', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}
                    {esMio && <span>{m.leido ? '✓✓ Visto' : '✓ Enviado'}</span>}
                  </p>
                </div>
              </div>
            )
          })
        )}
      </div>
      <form onSubmit={handleEnviar} className="flex gap-2 p-2 border-t border-gray-200 bg-white">
        <input
          value={texto}
          onChange={e => setTexto(e.target.value)}
          placeholder="Escribe un mensaje sobre la entrega..."
          maxLength={500}
          className="input flex-1 text-sm"
        />
        <button type="submit" disabled={enviando || !texto.trim()} className="btn-primary text-sm px-4">
          Enviar
        </button>
      </form>
    </div>
  )
}
