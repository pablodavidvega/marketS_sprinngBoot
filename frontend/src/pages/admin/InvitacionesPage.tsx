import { useState, useEffect } from 'react'
import {
  getInvitaciones, crearInvitacion, revocarInvitacion,
  reactivarInvitacion, eliminarInvitacion,
  getConfigRegistro, setConfigRegistro,
} from '../../api/invitaciones'
import type { Invitacion, ConfigRegistro } from '../../types'

const MODOS = [
  { value: 'ABIERTO',              label: 'Abierto',                     desc: 'Cualquier persona puede registrarse' },
  { value: 'DOMINIO',              label: 'Solo dominio de correo',       desc: 'Solo correos con el dominio configurado' },
  { value: 'INVITACION',           label: 'Solo invitación',              desc: 'Solo con link de invitación válido' },
  { value: 'DOMINIO_O_INVITACION', label: 'Dominio o invitación',         desc: 'Acepta cualquiera de los dos' },
]

function copiarAlPortapapeles(texto: string) {
  navigator.clipboard.writeText(texto).catch(() => {})
}

function linkDeInvitacion(token: string) {
  return `${window.location.origin}/registro?invitacion=${token}`
}

function formatFecha(s: string) {
  if (!s) return '—'
  return new Date(s).toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' })
}

// ── Modal generar invitación ──────────────────────────────────────────────────
interface ModalProps {
  onClose: () => void
  onCreada: (inv: Invitacion) => void
}

function ModalGenerarInvitacion({ onClose, onCreada }: ModalProps) {
  const [descripcion,      setDescripcion]      = useState('')
  const [maxUsos,          setMaxUsos]          = useState('')
  const [fechaExpiracion,  setFechaExpiracion]  = useState('')
  const [loading,          setLoading]          = useState(false)
  const [error,            setError]            = useState('')

  async function handleCrear() {
    setLoading(true)
    setError('')
    try {
      const res = await crearInvitacion({
        descripcion:     descripcion.trim() || undefined,
        maxUsos:         maxUsos ? parseInt(maxUsos) : null,
        fechaExpiracion: fechaExpiracion ? `${fechaExpiracion}T23:59:59` : null,
      })
      onCreada(res.data)
      onClose()
    } catch {
      setError('Error al generar el link')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 px-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6">
        <h2 className="text-lg font-semibold text-gray-800 mb-4">Generar link de invitación</h2>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Descripción <span className="text-gray-400 font-normal">(opcional)</span></label>
            <input className="input" placeholder="Ej: Onboarding enero 2025"
              value={descripcion} onChange={e => setDescripcion(e.target.value)} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Máx. usos <span className="text-gray-400 font-normal">(opcional)</span></label>
              <input type="number" min={1} className="input" placeholder="Sin límite"
                value={maxUsos} onChange={e => setMaxUsos(e.target.value)} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Expira <span className="text-gray-400 font-normal">(opcional)</span></label>
              <input type="date" className="input"
                value={fechaExpiracion} onChange={e => setFechaExpiracion(e.target.value)} />
            </div>
          </div>
        </div>

        {error && <p className="text-red-600 text-sm mt-3">{error}</p>}

        <div className="flex gap-3 justify-end mt-6">
          <button onClick={onClose} className="btn-secondary">Cancelar</button>
          <button onClick={handleCrear} disabled={loading} className="btn-primary disabled:opacity-50">
            {loading ? 'Generando...' : 'Generar link'}
          </button>
        </div>
      </div>
    </div>
  )
}

// ── Página principal ──────────────────────────────────────────────────────────
export default function InvitacionesPage() {
  const [invitaciones, setInvitaciones] = useState<Invitacion[]>([])
  const [config,       setConfig]       = useState<ConfigRegistro>({ modo: 'ABIERTO', dominio: '' })
  const [loading,      setLoading]      = useState(true)
  const [guardando,    setGuardando]    = useState(false)
  const [copiado,      setCopiado]      = useState<number | null>(null)
  const [modal,        setModal]        = useState(false)
  const [error,        setError]        = useState('')
  const [exito,        setExito]        = useState('')

  useEffect(() => { cargar() }, [])

  async function cargar() {
    setLoading(true)
    try {
      const [invRes, cfgRes] = await Promise.all([getInvitaciones(), getConfigRegistro()])
      setInvitaciones(invRes.data)
      setConfig(cfgRes.data)
    } catch {
      setError('Error al cargar datos')
    } finally {
      setLoading(false)
    }
  }

  async function guardarConfig() {
    setGuardando(true)
    setError('')
    setExito('')
    try {
      await setConfigRegistro(config)
      setExito('Configuración guardada correctamente')
      setTimeout(() => setExito(''), 3000)
    } catch {
      setError('Error al guardar la configuración')
    } finally {
      setGuardando(false)
    }
  }

  function handleCopiar(inv: Invitacion) {
    copiarAlPortapapeles(linkDeInvitacion(inv.token))
    setCopiado(inv.id)
    setTimeout(() => setCopiado(null), 2000)
  }

  async function handleRevocar(id: number) {
    await revocarInvitacion(id)
    setInvitaciones(prev => prev.map(i => i.id === id ? { ...i, activo: false, vigente: false } : i))
  }

  async function handleReactivar(id: number) {
    await reactivarInvitacion(id)
    setInvitaciones(prev => prev.map(i => i.id === id ? { ...i, activo: true, vigente: true } : i))
  }

  async function handleEliminar(id: number) {
    if (!confirm('¿Eliminar este link de invitación?')) return
    await eliminarInvitacion(id)
    setInvitaciones(prev => prev.filter(i => i.id !== id))
  }

  function handleCreada(inv: Invitacion) {
    setInvitaciones(prev => [inv, ...prev])
    handleCopiar(inv)
  }

  const necesitaDominio = config.modo === 'DOMINIO' || config.modo === 'DOMINIO_O_INVITACION'

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">

      <h1 className="text-2xl font-bold text-gray-800">Control de acceso y registro</h1>

      {error  && <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3">{error}</div>}
      {exito  && <div className="bg-green-50 border border-green-200 text-green-700 text-sm rounded-lg p-3">{exito}</div>}

      {/* ── Configuración de modo ── */}
      <div className="card space-y-4">
        <h2 className="text-base font-semibold text-gray-700">Modo de registro</h2>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {MODOS.map(m => (
            <label
              key={m.value}
              className={`flex items-start gap-3 p-3 rounded-xl border-2 cursor-pointer transition-colors
                ${config.modo === m.value
                  ? 'border-primary-500 bg-primary-50'
                  : 'border-gray-200 hover:border-gray-300'}`}
            >
              <input
                type="radio"
                name="modo"
                value={m.value}
                checked={config.modo === m.value}
                onChange={() => setConfig(prev => ({ ...prev, modo: m.value as ConfigRegistro['modo'] }))}
                className="mt-0.5 accent-primary-600"
              />
              <div>
                <p className="text-sm font-medium text-gray-800">{m.label}</p>
                <p className="text-xs text-gray-500">{m.desc}</p>
              </div>
            </label>
          ))}
        </div>

        {necesitaDominio && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Dominio de email permitido
            </label>
            <div className="flex items-center gap-2">
              <span className="text-gray-500 text-sm">@</span>
              <input
                className="input flex-1"
                placeholder="empresa.com"
                value={config.dominio}
                onChange={e => setConfig(prev => ({ ...prev, dominio: e.target.value }))}
              />
            </div>
            <p className="text-xs text-gray-400 mt-1">
              Solo usuarios con correo @{config.dominio || 'empresa.com'} podrán registrarse
            </p>
          </div>
        )}

        <div className="flex justify-end">
          <button onClick={guardarConfig} disabled={guardando} className="btn-primary disabled:opacity-50">
            {guardando ? 'Guardando...' : 'Guardar configuración'}
          </button>
        </div>
      </div>

      {/* ── Links de invitación ── */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-base font-semibold text-gray-700">Links de invitación</h2>
          <button onClick={() => setModal(true)} className="btn-primary text-sm py-1.5 flex items-center gap-1.5">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            Generar link
          </button>
        </div>

        {loading ? (
          <div className="flex justify-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
          </div>
        ) : invitaciones.length === 0 ? (
          <p className="text-center text-gray-400 text-sm py-8">No hay links de invitación generados</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-200">
                  <th className="text-left px-3 py-2 font-medium text-gray-600">Descripción</th>
                  <th className="text-left px-3 py-2 font-medium text-gray-600">Usos</th>
                  <th className="text-left px-3 py-2 font-medium text-gray-600">Expira</th>
                  <th className="text-left px-3 py-2 font-medium text-gray-600">Estado</th>
                  <th className="text-right px-3 py-2 font-medium text-gray-600">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {invitaciones.map(inv => (
                  <tr key={inv.id} className={`hover:bg-gray-50 ${!inv.vigente ? 'opacity-50' : ''}`}>
                    <td className="px-3 py-3 text-gray-800">
                      {inv.descripcion || <span className="text-gray-400 italic">Sin descripción</span>}
                    </td>
                    <td className="px-3 py-3 text-gray-600">
                      {inv.usosActuales}{inv.maxUsos !== '' ? `/${inv.maxUsos}` : ''}
                    </td>
                    <td className="px-3 py-3 text-gray-500">{formatFecha(inv.fechaExpiracion)}</td>
                    <td className="px-3 py-3">
                      {inv.vigente
                        ? <span className="text-xs font-medium text-green-700 bg-green-50 border border-green-200 px-2 py-0.5 rounded-full">Activo</span>
                        : <span className="text-xs font-medium text-gray-500 bg-gray-100 border border-gray-200 px-2 py-0.5 rounded-full">Inactivo</span>
                      }
                    </td>
                    <td className="px-3 py-3">
                      <div className="flex items-center gap-2 justify-end">
                        <button
                          onClick={() => handleCopiar(inv)}
                          title="Copiar link"
                          className="text-xs text-primary-600 hover:text-primary-700 font-medium flex items-center gap-1"
                        >
                          {copiado === inv.id ? (
                            <span className="text-green-600">¡Copiado!</span>
                          ) : (
                            <>
                              <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                                  d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                              </svg>
                              Copiar
                            </>
                          )}
                        </button>
                        {inv.activo
                          ? <button onClick={() => handleRevocar(inv.id)} className="text-xs text-amber-600 hover:text-amber-700 font-medium">Revocar</button>
                          : <button onClick={() => handleReactivar(inv.id)} className="text-xs text-green-600 hover:text-green-700 font-medium">Activar</button>
                        }
                        <button onClick={() => handleEliminar(inv.id)} className="text-xs text-red-500 hover:text-red-600 font-medium">Eliminar</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {modal && <ModalGenerarInvitacion onClose={() => setModal(false)} onCreada={handleCreada} />}
    </div>
  )
}
