import { useState, useEffect } from 'react'
import { getEncuentro, guardarEncuentro, cancelarEncuentro, confirmarEncuentro } from '../api/encuentro'
import type { Encuentro } from '../types'

export default function EncuentroPedido({ pedidoId, vendedorId }: { pedidoId: number; vendedorId: number }) {
  const [encuentro, setEncuentro] = useState<Partial<Encuentro> | null>(null)
  const [cargando, setCargando] = useState(true)
  const [editando, setEditando] = useState(false)
  const [lugar, setLugar] = useState('')
  const [fechaHora, setFechaHora] = useState('')
  const [nota, setNota] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [cancelando, setCancelando] = useState(false)
  const [confirmando, setConfirmando] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    getEncuentro(pedidoId, vendedorId)
      .then(r => setEncuentro(r.data.id ? r.data : null))
      .catch(() => setEncuentro(null))
      .finally(() => setCargando(false))
  }, [pedidoId, vendedorId])

  function abrirEdicion() {
    setLugar(encuentro?.lugar ?? '')
    setFechaHora(encuentro?.fechaHora ? encuentro.fechaHora.slice(0, 16) : '')
    setNota(encuentro?.nota ?? '')
    setError('')
    setEditando(true)
  }

  async function handleGuardar(e: React.FormEvent) {
    e.preventDefault()
    if (!lugar.trim() || !fechaHora) {
      setError('El lugar y la fecha/hora son obligatorios')
      return
    }
    setGuardando(true)
    setError('')
    try {
      const { data } = await guardarEncuentro(pedidoId, vendedorId, {
        lugar: lugar.trim(),
        fechaHora,
        nota: nota.trim() || undefined,
      })
      setEncuentro(data)
      setEditando(false)
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error
      setError(msg || 'No se pudo guardar el encuentro')
    } finally {
      setGuardando(false)
    }
  }

  async function handleCancelar() {
    setCancelando(true)
    try {
      await cancelarEncuentro(pedidoId, vendedorId)
      setEncuentro(null)
      setEditando(false)
    } finally {
      setCancelando(false)
    }
  }

  async function handleConfirmar() {
    setConfirmando(true)
    try {
      const { data } = await confirmarEncuentro(pedidoId, vendedorId)
      setEncuentro(data)
    } finally {
      setConfirmando(false)
    }
  }

  if (cargando) return null

  if (editando) {
    return (
      <form onSubmit={handleGuardar} className="border border-gray-200 rounded-lg p-3 flex flex-col gap-3 bg-gray-50">
        <div>
          <label className="block text-xs font-medium text-gray-600 mb-1">Lugar</label>
          <input
            value={lugar}
            onChange={e => setLugar(e.target.value)}
            placeholder="Ej: Centro comercial, entrada principal"
            maxLength={200}
            className="input w-full text-sm"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-gray-600 mb-1">Fecha y hora</label>
          <input
            type="datetime-local"
            value={fechaHora}
            onChange={e => setFechaHora(e.target.value)}
            className="input w-full text-sm"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-gray-600 mb-1">Nota (opcional)</label>
          <input
            value={nota}
            onChange={e => setNota(e.target.value)}
            placeholder="Ej: llevar el efectivo exacto"
            maxLength={300}
            className="input w-full text-sm"
          />
        </div>
        {error && <p className="text-red-500 text-xs">{error}</p>}
        <div className="flex gap-2">
          <button type="submit" disabled={guardando} className="btn-primary text-sm px-4 py-1.5">
            {guardando ? 'Guardando...' : encuentro ? 'Guardar cambios' : 'Proponer encuentro'}
          </button>
          <button type="button" onClick={() => setEditando(false)} className="btn-secondary text-sm px-4 py-1.5">
            Cancelar
          </button>
        </div>
      </form>
    )
  }

  if (!encuentro) {
    return (
      <button onClick={abrirEdicion} className="text-sm text-primary-600 hover:text-primary-700 font-medium">
        📍 Agendar encuentro
      </button>
    )
  }

  const fecha = encuentro.fechaHora ? new Date(encuentro.fechaHora) : null
  const caducado = fecha ? fecha.getTime() < Date.now() : false

  return (
    <div className="border border-gray-200 rounded-lg p-3 bg-gray-50">
      <div className="flex items-center gap-2">
        <p className="text-sm font-medium text-gray-800">📍 {encuentro.lugar}</p>
        {encuentro.ventaExternaConfirmada ? (
          <span className="text-[10px] font-medium text-green-700 bg-green-100 rounded-full px-2 py-0.5">
            Venta realizada
          </span>
        ) : caducado ? (
          <span className="text-[10px] font-medium text-gray-600 bg-gray-200 rounded-full px-2 py-0.5">
            Fecha caducada
          </span>
        ) : encuentro.estado === 'CONFIRMADO' ? (
          <span className="text-[10px] font-medium text-green-700 bg-green-100 rounded-full px-2 py-0.5">
            Confirmado
          </span>
        ) : (
          <span className="text-[10px] font-medium text-amber-700 bg-amber-100 rounded-full px-2 py-0.5">
            Pendiente de confirmación
          </span>
        )}
      </div>
      {fecha && (
        <p className="text-sm text-gray-600">
          {fecha.toLocaleDateString('es-CO', { day: 'numeric', month: 'long', year: 'numeric' })}
          {' · '}
          {fecha.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' })}
        </p>
      )}
      {encuentro.nota && <p className="text-xs text-gray-500 mt-1">{encuentro.nota}</p>}
      <p className="text-xs text-gray-400 mt-1">Propuesto por {encuentro.propuestoPorUsername}</p>
      {caducado && !encuentro.ventaExternaConfirmada && (
        <p className="text-xs text-gray-500 mt-1">
          La fecha de este encuentro ya pasó. Si necesitás coordinar uno nuevo, editalo con otra fecha.
        </p>
      )}
      <div className="flex gap-3 mt-2 items-center">
        {encuentro.puedeConfirmar && !caducado && (
          <button onClick={handleConfirmar} disabled={confirmando} className="text-xs font-medium text-green-700 hover:underline">
            {confirmando ? 'Confirmando...' : '✓ Aceptar encuentro'}
          </button>
        )}
        <button onClick={abrirEdicion} className="text-xs text-primary-600 hover:underline">
          Editar
        </button>
        <button onClick={handleCancelar} disabled={cancelando} className="text-xs text-red-500 hover:underline">
          {cancelando ? 'Cancelando...' : 'Cancelar encuentro'}
        </button>
      </div>
    </div>
  )
}
