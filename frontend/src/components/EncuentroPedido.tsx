import { useState, useEffect } from 'react'
import { getEncuentro, guardarEncuentro, cancelarEncuentro, confirmarEncuentro } from '../api/encuentro'
import type { Encuentro } from '../types'

const MINUTOS = ['00', '15', '30', '45']

// hora24: 0-23 → { hora12: 1-12, ampm }
function hora24aHora12(hora24: number): { hora12: number; ampm: 'AM' | 'PM' } {
  const ampm = hora24 >= 12 ? 'PM' : 'AM'
  const hora12 = hora24 % 12 === 0 ? 12 : hora24 % 12
  return { hora12, ampm }
}

function hora12aHora24(hora12: number, ampm: 'AM' | 'PM'): number {
  const h = hora12 % 12
  return ampm === 'PM' ? h + 12 : h
}

export default function EncuentroPedido({ pedidoId, vendedorId }: { pedidoId: number; vendedorId: number }) {
  const [encuentro, setEncuentro] = useState<Partial<Encuentro> | null>(null)
  const [cargando, setCargando] = useState(true)
  const [editando, setEditando] = useState(false)
  const [lugar, setLugar] = useState('')
  const [fechaInput, setFechaInput] = useState('')
  const [hora12, setHora12] = useState(12)
  const [minuto, setMinuto] = useState('00')
  const [ampm, setAmpm] = useState<'AM' | 'PM'>('PM')
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
    if (encuentro?.fechaHora) {
      const [f, h] = encuentro.fechaHora.slice(0, 16).split('T')
      const [hh, mm] = h.split(':').map(Number)
      const { hora12, ampm } = hora24aHora12(hh)
      setFechaInput(f)
      setHora12(hora12)
      setMinuto(MINUTOS.includes(String(mm).padStart(2, '0')) ? String(mm).padStart(2, '0') : MINUTOS[0])
      setAmpm(ampm)
    } else {
      setFechaInput('')
      setHora12(12)
      setMinuto('00')
      setAmpm('PM')
    }
    setNota(encuentro?.nota ?? '')
    setError('')
    setEditando(true)
  }

  async function handleGuardar(e: React.FormEvent) {
    e.preventDefault()
    if (!lugar.trim() || !fechaInput) {
      setError('El lugar y la fecha/hora son obligatorios')
      return
    }
    const hora24 = hora12aHora24(hora12, ampm)
    const fechaHora = `${fechaInput}T${String(hora24).padStart(2, '0')}:${minuto}`
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
          <div className="flex flex-wrap gap-2">
            <input
              type="date"
              value={fechaInput}
              onChange={e => setFechaInput(e.target.value)}
              className="input text-sm flex-1 min-w-[140px]"
            />
            <select
              value={hora12}
              onChange={e => setHora12(Number(e.target.value))}
              className="input text-sm w-[70px]"
            >
              {Array.from({ length: 12 }, (_, i) => i + 1).map(h => (
                <option key={h} value={h}>{h}</option>
              ))}
            </select>
            <select
              value={minuto}
              onChange={e => setMinuto(e.target.value)}
              className="input text-sm w-[70px]"
            >
              {MINUTOS.map(m => (
                <option key={m} value={m}>{m}</option>
              ))}
            </select>
            <select
              value={ampm}
              onChange={e => setAmpm(e.target.value as 'AM' | 'PM')}
              className="input text-sm w-[75px]"
            >
              <option value="AM">AM</option>
              <option value="PM">PM</option>
            </select>
          </div>
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
